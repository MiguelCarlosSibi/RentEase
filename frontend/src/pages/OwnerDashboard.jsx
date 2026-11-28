import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { fmtDate } from '../format.js';

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
          <span className="stat-value">{data.totalRooms}</span>
          <span className="stat-label">Rooms ({data.availableRooms} available)</span>
        </div>
        <div className="card stat">
          <span className="stat-value">{data.totalTenants}</span>
          <span className="stat-label">Tenants</span>
        </div>
        <div className="card stat">
          <span className="stat-value">{data.activeLeases}</span>
          <span className="stat-label">Active leases</span>
        </div>
      </div>

      <div className="card">
        <h2>Leases ending within 30 days</h2>
        {data.leasesEndingSoon.length === 0 ? (
          <p className="hint">No active lease ends in the next 30 days.</p>
        ) : (
          <ul className="plain-list">
            {data.leasesEndingSoon.map((l) => (
              <li key={l.id}>
                <strong>{l.tenantName}</strong>, room {l.roomNumber}, ends {fmtDate(l.endDate)}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="card">
        <h2>Quick links</h2>
        <ul className="plain-list">
          <li><Link to="/owner/rooms">Manage rooms</Link></li>
          <li><Link to="/owner/tenants">Manage tenants</Link></li>
          <li><Link to="/owner/leases">Manage leases</Link></li>
        </ul>
        <p className="hint">Billing, payments, and reports are added in the next steps of this increment.</p>
      </div>
    </section>
  );
}
