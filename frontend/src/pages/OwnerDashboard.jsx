import { useEffect, useState } from 'react';
import { api } from '../api.js';

export default function OwnerDashboard() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/owner/dashboard')
      .then(setData)
      .catch((err) => setError(err.message));
  }, []);

  if (error) return <p className="alert alert-error">{error}</p>;
  if (!data) return <p className="center-note">Loading dashboard...</p>;

  return (
    <section>
      <h1>Owner Dashboard</h1>
      <p className="lead">Welcome back, {data.fullName}.</p>

      <div className="stat-grid">
        <div className="card stat">
          <span className="stat-value">{data.registeredTenantAccounts}</span>
          <span className="stat-label">Registered tenant accounts</span>
        </div>
        <div className="card stat">
          <span className="stat-value stat-small">{data.email}</span>
          <span className="stat-label">Signed in as Owner</span>
        </div>
      </div>

      <div className="card">
        <h2>What you can do as an Owner</h2>
        <ul className="plain-list">
          <li>Manage properties, rooms, tenants, and leases</li>
          <li>Review monthly bills, payments, and receipts</li>
          <li>View monthly collection reports</li>
        </ul>
        <p className="hint">These modules are added in the next increments. Account access is ready now.</p>
      </div>
    </section>
  );
}
