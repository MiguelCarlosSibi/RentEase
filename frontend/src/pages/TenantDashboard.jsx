import { useEffect, useState } from 'react';
import { api } from '../api.js';

export default function TenantDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/tenant/dashboard')
      .then(setData)
      .catch((err) => setError(err.message));
  }, []);

  if (error) return <p className="alert alert-error">{error}</p>;
  if (!data) return <p className="center-note">Loading dashboard...</p>;

  const since = new Date(data.memberSince).toLocaleDateString('en-PH', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <section>
      <h1>Tenant Dashboard</h1>
      <p className="lead">Welcome, {data.fullName}.</p>

      <div className="stat-grid">
        <div className="card stat">
          <span className="stat-value stat-small">{data.mobileNumber}</span>
          <span className="stat-label">Mobile number for SMS reminders</span>
        </div>
        <div className="card stat">
          <span className="stat-value stat-small">{since}</span>
          <span className="stat-label">Member since</span>
        </div>
      </div>

      <div className="card">
        <h2>What you can do as a Tenant</h2>
        <ul className="plain-list">
          <li>View your monthly bills</li>
          <li>Pay rent online and get receipts</li>
          <li>Receive SMS payment reminders</li>
        </ul>
        <p className="hint">These modules are added in the next increments. Account access is ready now.</p>
      </div>
    </section>
  );
}
