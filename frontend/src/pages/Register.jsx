import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

const MOBILE_PATTERN = /^(09\d{9}|\+639\d{9})$/;
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// Mirrors the server-side rules so users get instant feedback; the server checks everything again.
function validate(form) {
  const errors = {};
  if (!form.fullName.trim()) errors.fullName = 'Full name is required';
  else if (form.fullName.trim().length > 100) errors.fullName = 'Full name must be at most 100 characters';

  if (!form.email.trim()) errors.email = 'Email is required';
  else if (!EMAIL_PATTERN.test(form.email.trim())) errors.email = 'Enter a valid email address';

  if (!form.mobileNumber.trim()) errors.mobileNumber = 'Mobile number is required';
  else if (!MOBILE_PATTERN.test(form.mobileNumber.trim()))
    errors.mobileNumber = 'Enter a valid Philippine mobile number (09XXXXXXXXX or +639XXXXXXXXX)';

  if (!form.role) errors.role = 'Select a role (Owner or Tenant)';

  if (!form.password) errors.password = 'Password is required';
  else if (!PASSWORD_PATTERN.test(form.password))
    errors.password = 'Password must be at least 8 characters and contain a letter and a number';

  if (!form.confirmPassword) errors.confirmPassword = 'Please confirm your password';
  else if (form.confirmPassword !== form.password) errors.confirmPassword = 'Passwords do not match';
  return errors;
}

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    fullName: '',
    email: '',
    mobileNumber: '',
    role: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    const fieldErrors = validate(form);
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length) return;

    setSubmitting(true);
    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        mobileNumber: form.mobileNumber.trim(),
        role: form.role,
        password: form.password,
        confirmPassword: form.confirmPassword,
      });
      navigate('/login', { replace: true, state: { registered: true } });
    } catch (err) {
      setErrors(err.errors || {});
      setMessage(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const field = (name, label, type = 'text', extra = {}) => (
    <>
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} type={type} value={form[name]} onChange={handleChange} {...extra} />
      {errors[name] && <span className="field-error">{errors[name]}</span>}
    </>
  );

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit} noValidate>
        <h1>Create your RentEase account</h1>
        {message && <p className="alert alert-error" role="alert">{message}</p>}

        {field('fullName', 'Full name', 'text', { autoComplete: 'name' })}
        {field('email', 'Email', 'email', { autoComplete: 'email' })}
        {field('mobileNumber', 'Mobile number', 'tel', { placeholder: '09XXXXXXXXX', autoComplete: 'tel' })}

        <label htmlFor="role">I am a</label>
        <select id="role" name="role" value={form.role} onChange={handleChange}>
          <option value="">Select role</option>
          <option value="OWNER">Owner (I rent out rooms)</option>
          <option value="TENANT">Tenant (I rent a room)</option>
        </select>
        {errors.role && <span className="field-error">{errors.role}</span>}

        {field('password', 'Password', 'password', { autoComplete: 'new-password' })}
        <small className="hint">At least 8 characters with a letter and a number.</small>
        {field('confirmPassword', 'Confirm password', 'password', { autoComplete: 'new-password' })}

        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Creating account...' : 'Create account'}
        </button>
        <p className="auth-switch">
          Already registered? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  );
}
