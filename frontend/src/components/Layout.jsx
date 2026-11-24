import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { dashboardPath, useAuth } from '../context/AuthContext.jsx';

// Menu items per role. Items marked soon belong to later increments and are not clickable yet.
const MENUS = {
  OWNER: [
    { label: 'Dashboard', to: '/owner/dashboard' },
    { label: 'Profile', to: '/profile' },
    { label: 'Change Password', to: '/change-password' },
    { label: 'Properties and Rooms', soon: true },
    { label: 'Tenants and Leases', soon: true },
    { label: 'Bills and Payments', soon: true },
    { label: 'Reports', soon: true },
  ],
  TENANT: [
    { label: 'Dashboard', to: '/tenant/dashboard' },
    { label: 'Profile', to: '/profile' },
    { label: 'Change Password', to: '/change-password' },
    { label: 'My Bills', soon: true },
    { label: 'Payments and Receipts', soon: true },
  ],
};

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true, state: { loggedOut: true } });
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to={dashboardPath(user.role)} className="brand">
          RentEase
        </NavLink>
        <div className="topbar-user">
          <span className="user-name">{user.fullName}</span>
          <span className={`badge badge-${user.role.toLowerCase()}`}>{user.role}</span>
          <button type="button" className="btn btn-outline" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </header>

      <div className="body-row">
        <nav className="sidebar" aria-label="Main menu">
          {MENUS[user.role].map((item) =>
            item.soon ? (
              <span key={item.label} className="nav-item nav-disabled" title="Available in a later increment">
                {item.label}
                <small>Soon</small>
              </span>
            ) : (
              <NavLink
                key={item.label}
                to={item.to}
                className={({ isActive }) => `nav-item${isActive ? ' nav-active' : ''}`}
              >
                {item.label}
              </NavLink>
            )
          )}
        </nav>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
