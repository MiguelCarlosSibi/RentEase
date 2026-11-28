import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { fmtDate, peso } from '../format.js';

const EMPTY = { roomId: '', tenantId: '', startDate: '', endDate: '', monthlyRent: '', dueDay: '1', depositAmount: '0' };
const DUE_DAYS = Array.from({ length: 28 }, (_, i) => i + 1);

export default function Leases() {
  const [leases, setLeases] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [tenants, setTenants] = useState([]);
  const [statusFilter, setStatusFilter] = useState('ACTIVE');
  const [loadError, setLoadError] = useState('');

  const [form, setForm] = useState(null); // null means the form is closed
  const [editing, setEditing] = useState(null); // the lease being edited, or null when creating
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(() => {
    return api(`/owner/leases?status=${statusFilter}`)
      .then((data) => {
        setLeases(data);
        setLoadError('');
      })
      .catch((err) => setLoadError(err.message));
  }, [statusFilter]);

  useEffect(() => {
    load();
  }, [load]);

  // The room and tenant lists are needed for the "new lease" form.
  const loadChoices = () =>
    Promise.all([api('/owner/rooms'), api('/owner/tenants')])
      .then(([roomList, tenantList]) => {
        setRooms(roomList);
        setTenants(tenantList);
      })
      .catch((err) => setLoadError(err.message));

  const openAdd = async () => {
    setEditing(null);
    setForm(EMPTY);
    setErrors({});
    setMessage('');
    setNotice('');
    await loadChoices();
  };

  const openEdit = (lease) => {
    setEditing(lease);
    setForm({ monthlyRent: String(lease.monthlyRent), dueDay: String(lease.dueDay) });
    setErrors({});
    setMessage('');
    setNotice('');
  };

  const closeForm = () => {
    setForm(null);
    setEditing(null);
    setErrors({});
    setMessage('');
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    const next = { ...form, [name]: value };
    // Picking a room suggests its monthly rate as the lease rent.
    if (name === 'roomId' && !editing) {
      const room = rooms.find((r) => String(r.id) === value);
      if (room) next.monthlyRent = String(room.monthlyRate);
    }
    setForm(next);
  };

  const validate = () => {
    const found = {};
    const money = /^\d{1,8}(\.\d{1,2})?$/;
    if (!editing) {
      if (!form.roomId) found.roomId = 'Select a room';
      if (!form.tenantId) found.tenantId = 'Select a tenant';
      if (!form.startDate) found.startDate = 'Start date is required';
      if (!form.endDate) found.endDate = 'End date is required';
      else if (form.startDate && form.endDate <= form.startDate) found.endDate = 'End date must be after the start date';
      if (form.depositAmount === '' || !money.test(String(form.depositAmount).trim()))
        found.depositAmount = 'Enter the security deposit (0 if none), with at most 2 decimal places';
    }
    if (form.monthlyRent === '' || Number(form.monthlyRent) <= 0) found.monthlyRent = 'Monthly rent must be greater than 0';
    else if (!money.test(String(form.monthlyRent).trim())) found.monthlyRent = 'Monthly rent can have at most 2 decimal places';
    const day = Number(form.dueDay);
    if (!Number.isInteger(day) || day < 1 || day > 28) found.dueDay = 'Due day must be from 1 to 28';
    return found;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    const found = validate();
    setErrors(found);
    if (Object.keys(found).length) return;

    setSubmitting(true);
    try {
      if (editing) {
        await api(`/owner/leases/${editing.id}`, {
          method: 'PUT',
          body: { monthlyRent: Number(form.monthlyRent), dueDay: Number(form.dueDay) },
        });
        setNotice(`The lease of ${editing.tenantName} was updated.`);
      } else {
        await api('/owner/leases', {
          method: 'POST',
          body: {
            roomId: Number(form.roomId),
            tenantId: Number(form.tenantId),
            startDate: form.startDate,
            endDate: form.endDate,
            monthlyRent: Number(form.monthlyRent),
            dueDay: Number(form.dueDay),
            depositAmount: Number(form.depositAmount),
          },
        });
        setNotice('The lease was created.');
      }
      closeForm();
      await load();
    } catch (err) {
      setErrors(err.errors || {});
      setMessage(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleEnd = async (lease) => {
    if (!window.confirm(`End the lease of ${lease.tenantName} in room ${lease.roomNumber}? No more bills will be generated.`)) return;
    setNotice('');
    try {
      await api(`/owner/leases/${lease.id}/end`, { method: 'POST' });
      setNotice(`The lease of ${lease.tenantName} was ended.`);
      await load();
    } catch (err) {
      setLoadError(err.message);
    }
  };

  const openRooms = rooms.filter((r) => r.status === 'AVAILABLE');
  const activeTenants = tenants.filter((t) => t.active);

  return (
    <section>
      <div className="page-head">
        <h1>Leases</h1>
        <button type="button" className="btn btn-primary btn-inline" onClick={openAdd}>New lease</button>
      </div>

      {notice && <p className="alert alert-success" role="status">{notice}</p>}
      {loadError && <p className="alert alert-error" role="alert">{loadError}</p>}

      {form && (
        <div className="card">
          <h2>{editing ? `Edit lease: ${editing.tenantName}, room ${editing.roomNumber}` : 'Create a lease'}</h2>
          <form className="form-grid" onSubmit={handleSubmit} noValidate>
            {message && <p className="alert alert-error" role="alert">{message}</p>}

            {!editing && (
              <>
                <label htmlFor="roomId">Room</label>
                <select id="roomId" name="roomId" value={form.roomId} onChange={handleChange}>
                  <option value="">Select a room</option>
                  {openRooms.map((r) => (
                    <option key={r.id} value={r.id}>
                      Room {r.roomNumber} ({r.activeLeases}/{r.capacity} taken, {peso(r.monthlyRate)})
                    </option>
                  ))}
                </select>
                {errors.roomId && <span className="field-error">{errors.roomId}</span>}
                {openRooms.length === 0 && <small className="hint">No room is available. Add a room, or end a lease to free a place.</small>}

                <label htmlFor="tenantId">Tenant</label>
                <select id="tenantId" name="tenantId" value={form.tenantId} onChange={handleChange}>
                  <option value="">Select a tenant</option>
                  {activeTenants.map((t) => (
                    <option key={t.id} value={t.id}>{t.fullName} ({t.email})</option>
                  ))}
                </select>
                {errors.tenantId && <span className="field-error">{errors.tenantId}</span>}
                {activeTenants.length === 0 && <small className="hint">Add a tenant first on the Tenants page.</small>}

                <label htmlFor="startDate">Start date</label>
                <input id="startDate" name="startDate" type="date" value={form.startDate} onChange={handleChange} />
                {errors.startDate && <span className="field-error">{errors.startDate}</span>}

                <label htmlFor="endDate">End date</label>
                <input id="endDate" name="endDate" type="date" value={form.endDate} onChange={handleChange} />
                {errors.endDate && <span className="field-error">{errors.endDate}</span>}
              </>
            )}

            <label htmlFor="monthlyRent">Monthly rent (PHP)</label>
            <input id="monthlyRent" name="monthlyRent" type="number" min="0" step="0.01" value={form.monthlyRent} onChange={handleChange} />
            {errors.monthlyRent && <span className="field-error">{errors.monthlyRent}</span>}

            <label htmlFor="dueDay">Rent is due on day</label>
            <select id="dueDay" name="dueDay" value={form.dueDay} onChange={handleChange}>
              {DUE_DAYS.map((d) => <option key={d} value={d}>{d}</option>)}
            </select>
            {errors.dueDay && <span className="field-error">{errors.dueDay}</span>}
            <small className="hint">Day 1 to 28 of each month.{editing && ' Changes apply only to bills that are not generated yet.'}</small>

            {!editing && (
              <>
                <label htmlFor="depositAmount">Security deposit (PHP)</label>
                <input id="depositAmount" name="depositAmount" type="number" min="0" step="0.01" value={form.depositAmount} onChange={handleChange} />
                {errors.depositAmount && <span className="field-error">{errors.depositAmount}</span>}
              </>
            )}

            <div className="button-row">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Saving...' : editing ? 'Save changes' : 'Create lease'}
              </button>
              <button type="button" className="btn btn-outline" onClick={closeForm}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      <div className="toolbar">
        <select aria-label="Filter by status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="ACTIVE">Active leases</option>
          <option value="ENDED">Ended leases</option>
          <option value="ALL">All leases</option>
        </select>
      </div>

      {!leases && !loadError && <p className="center-note">Loading leases...</p>}
      {leases && leases.length === 0 && <p className="card hint">No leases found for this filter.</p>}
      {leases && leases.length > 0 && (
        <div className="table-wrap card">
          <table className="data">
            <thead>
              <tr>
                <th>Tenant</th>
                <th>Room</th>
                <th>Period</th>
                <th>Rent</th>
                <th>Due day</th>
                <th>Deposit</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {leases.map((l) => (
                <tr key={l.id}>
                  <td><strong>{l.tenantName}</strong></td>
                  <td>{l.roomNumber}</td>
                  <td>{fmtDate(l.startDate)} to {fmtDate(l.endDate)}</td>
                  <td>{peso(l.monthlyRent)}</td>
                  <td>{l.dueDay}</td>
                  <td>{peso(l.depositAmount)}</td>
                  <td><span className={`pill ${l.status === 'ACTIVE' ? 'pill-available' : 'pill-archived'}`}>{l.status === 'ACTIVE' ? 'Active' : 'Ended'}</span></td>
                  <td className="row-actions">
                    {l.status === 'ACTIVE' && (
                      <>
                        <button type="button" className="btn btn-small" onClick={() => openEdit(l)}>Edit</button>
                        <button type="button" className="btn btn-small btn-danger" onClick={() => handleEnd(l)}>End lease</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
