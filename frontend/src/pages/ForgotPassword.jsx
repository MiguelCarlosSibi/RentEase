import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { EMAIL_PATTERN } from '../validators.js';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    if (!email.trim()) return setError('Email is required');
    if (!EMAIL_PATTERN.test(email.trim())) return setError('Enter a valid email address');

    setSubmitting(true);
    try {
      const data = await api('/auth/forgot-password', { method: 'POST', body: { email: email.trim() }, auth: false });
      setMessage(data.message);
    } catch (err) {
      setError(err.errors?.email || err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit} noValidate>
        <h1>Forgot your password?</h1>
        <p className="hint">Enter your account email. If it is registered, we send a link to reset your password. The link works once and expires in 15 minutes.</p>
        {message && <p className="alert alert-success" role="status">{message}</p>}
        {error && <p className="alert alert-error" role="alert">{error}</p>}

        <label htmlFor="email">Email</label>
        <input id="email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} />

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Sending...' : 'Send reset link'}
        </button>
        <p className="auth-switch">
          <Link to="/login">Back to log in</Link>
        </p>
      </form>
    </div>
  );
}
