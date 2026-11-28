import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { fmtDate, peso } from '../format.js';

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
          <span className="stat-value">{data.leases.length}</span>
          <span className="stat-label">Active lease(s)</span>
        </div>
      </div>

      <div className="card">
        <h2>My room</h2>
        {data.leases.length === 0 ? (
          <p className="hint">You do not have an active lease yet. Your owner will set it up.</p>
        ) : (
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr><th>Room</th><th>Period</th><th>Monthly rent</th><th>Due day</th></tr>
              </thead>
              <tbody>
                {data.leases.map((l) => (
                  <tr key={l.id}>
                    <td><strong>{l.roomNumber}</strong></td>
                    <td>{fmtDate(l.startDate)} to {fmtDate(l.endDate)}</td>
                    <td>{peso(l.monthlyRent)}</td>
                    <td>Day {l.dueDay} of each month</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="hint">Bills, online payment, and receipts are added in the next steps of this increment.</p>
      </div>
    </section>
  );
}
