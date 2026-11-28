import { Navigate, Route, Routes } from 'react-router-dom';
import { dashboardPath, useAuth } from './context/AuthContext.jsx';
import GuestRoute from './components/GuestRoute.jsx';
import Layout from './components/Layout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import ChangePassword from './pages/ChangePassword.jsx';
import ForgotPassword from './pages/ForgotPassword.jsx';
import Leases from './pages/Leases.jsx';
import Login from './pages/Login.jsx';
import NotFound from './pages/NotFound.jsx';
import OwnerDashboard from './pages/OwnerDashboard.jsx';
import Profile from './pages/Profile.jsx';
import Register from './pages/Register.jsx';
import Rooms from './pages/Rooms.jsx';
import ResetPassword from './pages/ResetPassword.jsx';
import TenantDashboard from './pages/TenantDashboard.jsx';
import Tenants from './pages/Tenants.jsx';
import Unauthorized from './pages/Unauthorized.jsx';

function Home() {
  const { user, loading } = useAuth();
  if (loading) return <p className="center-note">Loading...</p>;
  return <Navigate to={user ? dashboardPath(user.role) : '/login'} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<GuestRoute><Login /></GuestRoute>} />
      <Route path="/register" element={<GuestRoute><Register /></GuestRoute>} />
      <Route path="/forgot-password" element={<GuestRoute><ForgotPassword /></GuestRoute>} />
      <Route path="/reset-password" element={<ResetPassword />} />

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route
          path="/owner/dashboard"
          element={<ProtectedRoute roles={['OWNER']}><OwnerDashboard /></ProtectedRoute>}
        />
        <Route path="/owner/rooms" element={<ProtectedRoute roles={['OWNER']}><Rooms /></ProtectedRoute>} />
        <Route path="/owner/tenants" element={<ProtectedRoute roles={['OWNER']}><Tenants /></ProtectedRoute>} />
        <Route path="/owner/leases" element={<ProtectedRoute roles={['OWNER']}><Leases /></ProtectedRoute>} />
        <Route
          path="/tenant/dashboard"
          element={<ProtectedRoute roles={['TENANT']}><TenantDashboard /></ProtectedRoute>}
        />
        <Route path="/profile" element={<Profile />} />
        <Route path="/change-password" element={<ChangePassword />} />
      </Route>

      <Route path="/unauthorized" element={<Unauthorized />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
