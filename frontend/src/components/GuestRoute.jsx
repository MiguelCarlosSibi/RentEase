import { Navigate } from 'react-router-dom';
import { dashboardPath, useAuth } from '../context/AuthContext.jsx';

/** Login and register pages: logged-in users are sent to their own dashboard instead. */
export default function GuestRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <p className="center-note">Loading...</p>;
  }
  if (user) {
    return <Navigate to={dashboardPath(user.role)} replace />;
  }
  return children;
}
