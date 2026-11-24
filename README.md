# RentEase - User Accounts, Profile, and Password Management (Increments 1 and 2)

RentEase is a boarding house rent management system for Owners and Tenants. This repository contains
**Increment 1: User Accounts and Authentication** and **Increment 2: User Profile and Password Management**,
built across the frontend, backend, and database.

| Layer | Technology |
|-------|------------|
| Frontend | ReactJS 18 (Vite), React Router |
| Backend | Spring Boot 3.3 (Java 17), Spring Security, Spring Data JPA |
| Database | MySQL / MariaDB (XAMPP) |
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

## Increment 2 features: profile and password management

4. **User profile management**
   - Logged-in users open **Profile** to see and edit their full name and mobile number. Email (the login ID) and role are shown but cannot be edited.
   - Inputs are validated in the browser and again on the server, then saved to `user_account`.
   - The profile endpoints (`GET/PUT /api/profile`) never take a user id: they always act on the logged-in user, so nobody can view or change another user's profile.
5. **Forgot and reset password**
   - **Forgot password?** on the login page asks for the account email and sends a reset link by email.
   - The link carries a random 256-bit token. Only its SHA-256 hash is stored (`password_reset_token`), the token is **single-use**, and it **expires after 15 minutes**. Requesting a new link cancels the older one.
   - The answer to "forgot password" is always the same, whether or not the email exists, so it cannot be used to find registered emails. Requests are throttled to one link per minute per account.
   - The new password is validated, hashed with BCrypt, and saved. A reset also ends every existing session and clears an account lock.
6. **Change password**
   - Logged-in users open **Change Password**, enter the current password, a new password, and a confirmation.
   - The current password is verified first; the new password must follow the password rule and differ from the current one.
   - The new hash replaces the old one, so the previous password can no longer log in. Tokens issued before the change are rejected (token version check), and the user receives a fresh token to stay logged in.

All Increment 1 features (registration, login, logout, lockout, role-based dashboards) still work.

### Email for reset links

The reset link is sent with Spring Mail. Without a mail host configured, the backend runs in **DEV MODE** and
prints the reset link in the backend console instead of sending an email, so the flow can still be tested.

To send real emails (example with Gmail):

1. Turn on 2-Step Verification for the Gmail account and create an **App Password** (Google Account > Security > App passwords).
2. Copy `backend/src/main/resources/application-local.properties.example` to `application-local.properties` in the same folder (it is git-ignored, so your password is not committed) and fill in your address and app password.
3. Start the backend with the `local` profile:
   - Maven: `mvn spring-boot:run -Dspring-boot.run.profiles=local`
   - IntelliJ: in the run configuration of `RentEaseApplication`, set **Active profiles** to `local`.

## Test Increment 2

1. Log in and open **Profile**. Change your name or mobile number, save, and refresh: the new values stay. Try an invalid mobile number: an error appears.
2. Open **Change Password**. Enter a wrong current password: "Current password is incorrect". Enter the correct one with a valid new password: success.
3. Log out, then log in with the **old** password: it fails. Log in with the new one: it works.
4. Log out and click **Forgot password?** on the login page. Enter your email. Open the reset link from your email (or from the backend console in DEV MODE).
5. Set a new password on the reset page, then log in with it.
6. Open the same reset link again: "invalid or expired". Wait 15 minutes with a fresh link: it also expires.
7. Request a reset link, then change your password from the Profile area: the old link stops working.

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
      service/                AuthService (register, login, lockout, logout), ProfileService,
                              PasswordService (change / forgot / reset), MailService
    src/main/resources/application.properties
  frontend/                   React (Vite) application
    src/
      api.js                  fetch helper and token storage
      context/AuthContext.jsx session state (login, register, logout)
      components/             Layout (role menus), ProtectedRoute, GuestRoute
      pages/                  Login, Register, ForgotPassword, ResetPassword, ChangePassword,
                              Profile, OwnerDashboard, TenantDashboard, ...
  docs/schema.sql             reference table definitions
```

## Prerequisites

- JDK 17 or newer
- Maven 3.9+ (or the Maven support built into IntelliJ IDEA)
- Node.js 18 or newer
- MySQL or MariaDB, for example **XAMPP** with MySQL started

## Setup and run

### 1. Database
Start MySQL in the XAMPP Control Panel. The backend creates the `rentease` database and its tables
automatically on first start (default user `root`, empty password). To use other credentials set
`DB_USERNAME` and `DB_PASSWORD` (or `DB_URL`) as environment variables.

### 2. Backend (port 8080)
```bash
cd backend
mvn spring-boot:run
```
Or open the `backend` folder in IntelliJ IDEA and run `RentEaseApplication`.

Optional environment variables:

| Variable | Purpose | Default |
|----------|---------|---------|
| `DB_URL` | JDBC URL | `jdbc:mysql://localhost:3306/rentease?createDatabaseIfNotExist=true...` |
| `DB_USERNAME` / `DB_PASSWORD` | database login | `root` / empty |
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
7. Check the database: `user_account.password_hash` holds BCrypt hashes (`$2a$10$...`), not passwords.

## API

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | `/api/auth/register` | public | Create an account (201, or 400 / 409 with field errors) |
| POST | `/api/auth/login` | public | Returns `{ token, user }` (401 invalid, 423 locked) |
| POST | `/api/auth/logout` | logged in | Revokes the current token |
| GET | `/api/auth/me` | logged in | Returns the current user |
| POST | `/api/auth/change-password` | logged in | Verifies the current password, sets a new one, returns a new token |
| POST | `/api/auth/forgot-password` | public | Sends a reset link (same response whether or not the email exists) |
| POST | `/api/auth/reset-password` | public + valid token | Sets a new password with a single-use, expiring token |
| GET | `/api/profile` | logged in | Returns the logged-in user's profile |
| PUT | `/api/profile` | logged in | Updates the logged-in user's name and mobile number |
| GET | `/api/owner/dashboard` | OWNER | Owner dashboard data (403 for other roles) |
| GET | `/api/tenant/dashboard` | TENANT | Tenant dashboard data (403 for other roles) |

## Data model used in this increment

- `user_account` (`user_id`, `full_name`, `email` unique, `mobile_number`, `password_hash`, `role`, `status`,
  `failed_attempts`, `locked_until`, `created_at`) from the RentEase data model.
- `user_account.token_version` (Increment 2): raised on every password change or reset so older tokens stop working.
- `revoked_token` (`token_id`, `expires_at`): added in Increment 1 so logout truly ends a session.
- `password_reset_token` (`token_id`, `user_id` FK, `token_hash` unique, `created_at`, `expires_at`, `used_at`): added in Increment 2 for single-use, expiring reset links.

See `docs/schema.sql` for the table definitions.

## Troubleshooting

- **Backend cannot connect to the database:** make sure MySQL is running in XAMPP and the credentials match.
- **Port 8080 or 5173 is in use:** stop the other program or change `server.port` / the Vite port.
- **"JWT secret must be at least 32 characters":** your `JWT_SECRET` value is too short.
- **Frontend shows "Cannot reach the server":** start the backend first, then reload the page.
- **No reset email arrives:** check the backend console. In DEV MODE the link is printed there. For Gmail, use an App Password, not your normal password.
- **Everything logged me out after updating:** Increment 2 adds a token version check, so sessions from before the update end once. Log in again.
