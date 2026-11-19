import { Link } from 'react-router-dom';
import { dashboardPath, useAuth } from '../context/AuthContext.jsx';

export default function Unauthorized() {
  const { user } = useAuth();
  return (
    <div className="auth-page">
      <div className="card auth-card">
        <h1>Access denied</h1>
        <p>You do not have permission to open that page.</p>
        <Link className="btn btn-primary" to={user ? dashboardPath(user.role) : '/login'}>
          {user ? 'Go to my dashboard' : 'Go to login'}
        </Link>
      </div>
    </div>
  );
}
