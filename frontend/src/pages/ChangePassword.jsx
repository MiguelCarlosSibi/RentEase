import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { dashboardPath, useAuth } from '../context/AuthContext.jsx';
import { PASSWORD_PATTERN, PASSWORD_RULE } from '../validators.js';

const EMPTY = { currentPassword: '', newPassword: '', confirmNewPassword: '' };

export default function ChangePassword() {
  const { user, replaceSession } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    setSuccess('');

    const fieldErrors = {};
    if (!form.currentPassword) fieldErrors.currentPassword = 'Current password is required';
    if (!form.newPassword) fieldErrors.newPassword = 'New password is required';
    else if (!PASSWORD_PATTERN.test(form.newPassword)) fieldErrors.newPassword = PASSWORD_RULE;
    else if (form.newPassword === form.currentPassword)
      fieldErrors.newPassword = 'New password must be different from your current password';
    if (!form.confirmNewPassword) fieldErrors.confirmNewPassword = 'Please confirm your new password';
    else if (form.confirmNewPassword !== form.newPassword) fieldErrors.confirmNewPassword = 'Passwords do not match';
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length) return;

    setSubmitting(true);
    try {
      const data = await api('/auth/change-password', { method: 'POST', body: form });
      const wasTemporary = user.mustChangePassword;
      replaceSession(data.token, data.user); // the old token no longer works
      if (wasTemporary) {
        navigate(dashboardPath(data.user.role), { replace: true });
        return;
      }
      setForm(EMPTY);
      setErrors({});
      setSuccess('Your password has been changed. Use the new password the next time you log in.');
    } catch (err) {
      setErrors(err.errors || {});
      setMessage(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <h1>Change Password</h1>
      {user.mustChangePassword && (
        <p className="alert alert-error" role="alert">
          You are using a temporary password. Choose a new password to continue.
        </p>
      )}
      <div className="card">
        <form className="form-grid" onSubmit={handleSubmit} noValidate>
          {success && <p className="alert alert-success" role="status">{success}</p>}
          {message && <p className="alert alert-error" role="alert">{message}</p>}

          <label htmlFor="currentPassword">Current password</label>
          <input id="currentPassword" name="currentPassword" type="password" autoComplete="current-password" value={form.currentPassword} onChange={handleChange} />
          {errors.currentPassword && <span className="field-error">{errors.currentPassword}</span>}

          <label htmlFor="newPassword">New password</label>
          <input id="newPassword" name="newPassword" type="password" autoComplete="new-password" value={form.newPassword} onChange={handleChange} />
          {errors.newPassword && <span className="field-error">{errors.newPassword}</span>}
          <small className="hint">At least 8 characters with a letter and a number.</small>

          <label htmlFor="confirmNewPassword">Confirm new password</label>
          <input id="confirmNewPassword" name="confirmNewPassword" type="password" autoComplete="new-password" value={form.confirmNewPassword} onChange={handleChange} />
          {errors.confirmNewPassword && <span className="field-error">{errors.confirmNewPassword}</span>}

          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving...' : 'Change password'}
          </button>
        </form>
      </div>
    </section>
  );
}
