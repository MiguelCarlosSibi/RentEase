import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { dashboardPath, useAuth } from '../context/AuthContext.jsx';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const notice = location.state?.registered
    ? 'Account created. You can now log in.'
    : location.state?.loggedOut
      ? 'You have been logged out.'
      : '';

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    const fieldErrors = {};
    if (!form.email.trim()) fieldErrors.email = 'Email is required';
    if (!form.password) fieldErrors.password = 'Password is required';
    setErrors(fieldErrors);
    setMessage('');
    if (Object.keys(fieldErrors).length) return;

    setSubmitting(true);
    try {
      const user = await login(form.email.trim(), form.password);
      navigate(dashboardPath(user.role), { replace: true });
    } catch (err) {
      setErrors(err.errors || {});
      setMessage(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit} noValidate>
        <h1>Log in to RentEase</h1>
        {notice && <p className="alert alert-success">{notice}</p>}
        {message && <p className="alert alert-error" role="alert">{message}</p>}

        <label htmlFor="email">Email</label>
        <input id="email" name="email" type="email" autoComplete="email" value={form.email} onChange={handleChange} />
        {errors.email && <span className="field-error">{errors.email}</span>}

        <label htmlFor="password">Password</label>
        <input id="password" name="password" type="password" autoComplete="current-password" value={form.password} onChange={handleChange} />
        {errors.password && <span className="field-error">{errors.password}</span>}

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Logging in...' : 'Log in'}
        </button>
        <p className="auth-switch">
          No account yet? <Link to="/register">Register</Link>
        </p>
      </form>
    </div>
  );
}
