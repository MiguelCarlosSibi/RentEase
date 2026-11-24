import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { PASSWORD_PATTERN, PASSWORD_RULE } from '../validators.js';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get('token') || '';
  const navigate = useNavigate();

  const [form, setForm] = useState({ newPassword: '', confirmNewPassword: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    const fieldErrors = {};
    if (!form.newPassword) fieldErrors.newPassword = 'New password is required';
    else if (!PASSWORD_PATTERN.test(form.newPassword)) fieldErrors.newPassword = PASSWORD_RULE;
    if (!form.confirmNewPassword) fieldErrors.confirmNewPassword = 'Please confirm your new password';
    else if (form.confirmNewPassword !== form.newPassword) fieldErrors.confirmNewPassword = 'Passwords do not match';
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length) return;

    setSubmitting(true);
    try {
      await api('/auth/reset-password', {
        method: 'POST',
        body: { token, newPassword: form.newPassword, confirmNewPassword: form.confirmNewPassword },
        auth: false,
      });
      navigate('/login', { replace: true, state: { passwordReset: true } });
    } catch (err) {
      setErrors(err.errors || {});
      setMessage(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (!token) {
    return (
      <div className="auth-page">
        <div className="card auth-card">
          <h1>Reset link incomplete</h1>
          <p>This page needs the link from your reset email. Request a new link to continue.</p>
          <Link className="btn btn-primary" to="/forgot-password">Request a new link</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit} noValidate>
        <h1>Choose a new password</h1>
        {message && (
          <p className="alert alert-error" role="alert">
            {message} <Link to="/forgot-password">Request a new link</Link>
          </p>
        )}

        <label htmlFor="newPassword">New password</label>
        <input id="newPassword" name="newPassword" type="password" autoComplete="new-password" value={form.newPassword} onChange={handleChange} />
        {errors.newPassword && <span className="field-error">{errors.newPassword}</span>}
        <small className="hint">At least 8 characters with a letter and a number.</small>

        <label htmlFor="confirmNewPassword">Confirm new password</label>
        <input id="confirmNewPassword" name="confirmNewPassword" type="password" autoComplete="new-password" value={form.confirmNewPassword} onChange={handleChange} />
        {errors.confirmNewPassword && <span className="field-error">{errors.confirmNewPassword}</span>}

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving...' : 'Reset password'}
        </button>
      </form>
    </div>
  );
}
