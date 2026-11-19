import { Navigate, Route, Routes } from 'react-router-dom';
import { dashboardPath, useAuth } from './context/AuthContext.jsx';
import GuestRoute from './components/GuestRoute.jsx';
import Layout from './components/Layout.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Login from './pages/Login.jsx';
import NotFound from './pages/NotFound.jsx';
import OwnerDashboard from './pages/OwnerDashboard.jsx';
import Profile from './pages/Profile.jsx';
import Register from './pages/Register.jsx';
import TenantDashboard from './pages/TenantDashboard.jsx';
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

      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        <Route
          path="/owner/dashboard"
          element={<ProtectedRoute roles={['OWNER']}><OwnerDashboard /></ProtectedRoute>}
        />
        <Route
          path="/tenant/dashboard"
          element={<ProtectedRoute roles={['TENANT']}><TenantDashboard /></ProtectedRoute>}
        />
        <Route path="/profile" element={<Profile />} />
      </Route>

      <Route path="/unauthorized" element={<Unauthorized />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
