# RentEase - User Accounts and Authentication (Increment 1)

RentEase is a boarding house rent management system for Owners and Tenants. This repository contains
**Increment 1: User Accounts and Authentication**, built across the frontend, backend, and database.

| Layer | Technology |
|-------|------------|
| Frontend | ReactJS 18 (Vite), React Router |
| Backend | Spring Boot 3.3 (Java 17), Spring Security, Spring Data JPA |
| Database | PostgreSQL (hosted on Supabase) |
| Security | BCrypt password hashing, signed JWT access tokens, server-side token revocation on logout |

## Features in this increment

1. **User account creation**
   - Registration form with required-field validation (browser and server).
   - Philippine mobile number check (`09XXXXXXXXX` or `+639XXXXXXXXX`), email format check, password rule (8+ characters with a letter and a number), password confirmation.
   - Duplicate emails are rejected (server check and a unique database constraint).
   - Passwords are hashed with BCrypt before they are stored. Plain passwords are never saved.
   - The user chooses a role at registration: `OWNER` or `TENANT`.
2. **Login and logout**
   - Login with email and password; wrong credentials show "Invalid email or password".
   - An account is locked for 15 minutes after 5 failed logins in a row.
   - The server issues a signed token that expires after 60 minutes and is sent in the `Authorization` header.
   - Logout revokes the token on the server (stored in `revoked_token`), so it cannot be reused, and clears it from the browser.
   - Protected pages and endpoints reject missing, invalid, expired, or revoked tokens (HTTP 401).
3. **Role-based dashboards**
   - Owners are sent to `/owner/dashboard`, Tenants to `/tenant/dashboard` after login.
   - The menu changes with the role. Modules planned for later increments are shown as disabled ("Soon").
   - Access is enforced in the frontend (route guards) and in the backend (`/api/owner/**` needs `OWNER`, `/api/tenant/**` needs `TENANT`, otherwise HTTP 403).
   - Each dashboard loads real data from the database through its own protected endpoint.

## Project structure

```
rentease/
  backend/                    Spring Boot application
    src/main/java/com/rentease/
      config/                 SecurityConfig (routes, CORS, BCrypt)
      controller/             AuthController, DashboardController
      dto/                    request and response objects
      entity/                 User (table user_account), RevokedToken, enums
      exception/              ApiException, GlobalExceptionHandler
      repository/             Spring Data JPA repositories
      security/               JwtService, JwtAuthFilter
      service/                AuthService (register, login, lockout, logout)
    src/main/resources/application.properties
  frontend/                   React (Vite) application
    src/
      api.js                  fetch helper and token storage
      context/AuthContext.jsx session state (login, register, logout)
      components/             Layout (role menus), ProtectedRoute, GuestRoute
      pages/                  Login, Register, OwnerDashboard, TenantDashboard, Profile, ...
  docs/schema.sql             reference table definitions
```

## Prerequisites

- JDK 17 or newer
- Maven 3.9+ (or the Maven support built into IntelliJ IDEA)
- Node.js 18 or newer
- A free Supabase project (PostgreSQL database)

## Setup and run

### 1. Database (Supabase)
Create a free project at https://supabase.com and save the database password you set. Click **Connect**,
choose **Session pooler**, and note the host and user. The backend creates the tables (`user_account`,
`revoked_token`) automatically on first start.

Set these environment variables before starting the backend (the password is never stored in the repository):

| Variable | Value |
|----------|-------|
| `DB_URL` | `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require` |
| `DB_USERNAME` | `postgres.<project-ref>` |
| `DB_PASSWORD` | your Supabase database password |

### 2. Backend (port 8080)
Windows PowerShell:
```powershell
cd backend
$env:DB_PASSWORD="your-database-password"
mvn spring-boot:run
```
Or open the `backend` folder in IntelliJ IDEA, add the environment variables under
**Run > Edit Configurations > Environment variables**, and run `RentEaseApplication`.

Optional environment variables:

| Variable | Purpose | Default |
|----------|---------|---------|
| `JWT_SECRET` | token signing key, at least 32 characters | development value in `application.properties` |
| `CORS_ORIGINS` | allowed frontend origin | `http://localhost:5173` |

Use your own `JWT_SECRET` outside of local development.

### 3. Frontend (port 5173)
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. The dev server forwards `/api` calls to the backend on port 8080.

## Try it

1. Open `/register` and create one **Owner** and one **Tenant** account.
2. Log in as the Owner: you land on the Owner Dashboard with the owner menu.
3. Log out, then log in as the Tenant: you land on the Tenant Dashboard with the tenant menu.
4. While logged in as the Tenant, open `/owner/dashboard` in the address bar: you are sent to the Access denied page.
5. Log out and try to open `/owner/dashboard` again: you are sent to the login page.
6. Try 5 wrong passwords in a row: the account is locked for 15 minutes.
7. Check the database (Supabase Table Editor): `user_account.password_hash` holds BCrypt hashes (`$2a$10$...`), not passwords.

## API

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | public | Create an account (201, or 400 / 409 with field errors) |
| POST | `/api/auth/login` | public | Returns `{ token, user }` (401 invalid, 423 locked) |
| POST | `/api/auth/logout` | logged in | Revokes the current token |
| GET | `/api/auth/me` | logged in | Returns the current user |
| GET | `/api/owner/dashboard` | OWNER | Owner dashboard data (403 for other roles) |
| GET | `/api/tenant/dashboard` | TENANT | Tenant dashboard data (403 for other roles) |

## Data model used in this increment

- `user_account` (`user_id`, `full_name`, `email` unique, `mobile_number`, `password_hash`, `role`, `status`,
  `failed_attempts`, `locked_until`, `created_at`) from the RentEase data model.
- `revoked_token` (`token_id`, `expires_at`): added in this increment so logout truly ends a session.

See `docs/schema.sql` for the table definitions.

## Troubleshooting

- **Backend cannot connect to the database:** check `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`, and make sure the Supabase project is not paused (click Restore in the Supabase dashboard).
- **Port 8080 or 5173 is in use:** stop the other program or change `server.port` / the Vite port.
- **"JWT secret must be at least 32 characters":** your `JWT_SECRET` value is too short.
- **Frontend shows "Cannot reach the server":** start the backend first, then reload the page.