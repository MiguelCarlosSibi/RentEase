import { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../context/AuthContext.jsx';
import { MOBILE_PATTERN, MOBILE_RULE } from '../validators.js';

export default function Profile() {
  const { user, updateUser } = useAuth();
  const [form, setForm] = useState({ fullName: user.fullName, mobileNumber: user.mobileNumber });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const unchanged = form.fullName.trim() === user.fullName && form.mobileNumber.trim() === user.mobileNumber;

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setSuccess('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    setSuccess('');

    const fieldErrors = {};
    if (!form.fullName.trim()) fieldErrors.fullName = 'Full name is required';
    else if (form.fullName.trim().length > 100) fieldErrors.fullName = 'Full name must be at most 100 characters';
    if (!form.mobileNumber.trim()) fieldErrors.mobileNumber = 'Mobile number is required';
    else if (!MOBILE_PATTERN.test(form.mobileNumber.trim())) fieldErrors.mobileNumber = MOBILE_RULE;
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length) return;

    setSubmitting(true);
    try {
      const updated = await api('/profile', {
        method: 'PUT',
        body: { fullName: form.fullName.trim(), mobileNumber: form.mobileNumber.trim() },
      });
      updateUser(updated);
      setForm({ fullName: updated.fullName, mobileNumber: updated.mobileNumber });
      setSuccess('Profile updated.');
    } catch (err) {
      setErrors(err.errors || {});
      setMessage(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section>
      <h1>My Profile</h1>
      <div className="card">
        <form className="form-grid" onSubmit={handleSubmit} noValidate>
          {success && <p className="alert alert-success" role="status">{success}</p>}
          {message && <p className="alert alert-error" role="alert">{message}</p>}

          <label htmlFor="fullName">Full name</label>
          <input id="fullName" name="fullName" value={form.fullName} onChange={handleChange} autoComplete="name" />
          {errors.fullName && <span className="field-error">{errors.fullName}</span>}

          <label htmlFor="mobileNumber">Mobile number</label>
          <input id="mobileNumber" name="mobileNumber" type="tel" value={form.mobileNumber} onChange={handleChange} placeholder="09XXXXXXXXX" />
          {errors.mobileNumber && <span className="field-error">{errors.mobileNumber}</span>}

          <label htmlFor="email">Email</label>
          <input id="email" className="readonly" value={user.email} readOnly />
          <small className="hint">Your email is your login ID and cannot be changed here.</small>

          <label htmlFor="role">Role</label>
          <input id="role" className="readonly" value={user.role} readOnly />

          <button type="submit" className="btn btn-primary" disabled={submitting || unchanged}>
            {submitting ? 'Saving...' : 'Save changes'}
          </button>
        </form>
      </div>
    </section>
  );
}
