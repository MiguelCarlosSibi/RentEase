import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { peso } from '../format.js';

const EMPTY = { roomNumber: '', description: '', capacity: '1', monthlyRate: '', underMaintenance: false };

const STATUS_LABEL = { AVAILABLE: 'Available', FULL: 'Full', MAINTENANCE: 'Under maintenance', ARCHIVED: 'Archived' };

export default function Rooms() {
  const [rooms, setRooms] = useState(null);
  const [loadError, setLoadError] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [search, setSearch] = useState('');

  const [form, setForm] = useState(null); // null means the form is closed
  const [editingId, setEditingId] = useState(null);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(() => {
    const params = new URLSearchParams();
    if (statusFilter) params.set('status', statusFilter);
    if (search.trim()) params.set('search', search.trim());
    return api(`/owner/rooms?${params.toString()}`)
      .then((data) => {
        setRooms(data);
        setLoadError('');
      })
      .catch((err) => setLoadError(err.message));
  }, [statusFilter, search]);

  useEffect(() => {
    load();
  }, [load]);

  const openAdd = () => {
    setEditingId(null);
    setForm(EMPTY);
    setErrors({});
    setMessage('');
    setNotice('');
  };

  const openEdit = (room) => {
    setEditingId(room.id);
    setForm({
      roomNumber: room.roomNumber,
      description: room.description || '',
      capacity: String(room.capacity),
      monthlyRate: String(room.monthlyRate),
      underMaintenance: room.status === 'MAINTENANCE',
    });
    setErrors({});
    setMessage('');
    setNotice('');
  };

  const closeForm = () => {
    setForm(null);
    setEditingId(null);
    setErrors({});
    setMessage('');
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const validate = () => {
    const found = {};
    if (!form.roomNumber.trim()) found.roomNumber = 'Room number is required';
    else if (form.roomNumber.trim().length > 20) found.roomNumber = 'Room number must be at most 20 characters';
    const capacity = Number(form.capacity);
    if (!Number.isInteger(capacity) || capacity < 1 || capacity > 20) found.capacity = 'Capacity must be a whole number from 1 to 20';
    const rate = Number(form.monthlyRate);
    if (form.monthlyRate === '' || Number.isNaN(rate) || rate <= 0) found.monthlyRate = 'Monthly rate must be greater than 0';
    else if (!/^\d{1,8}(\.\d{1,2})?$/.test(String(form.monthlyRate).trim())) found.monthlyRate = 'Monthly rate can have at most 2 decimal places';
    if (form.description.length > 255) found.description = 'Description must be at most 255 characters';
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
      const body = {
        roomNumber: form.roomNumber.trim(),
        description: form.description.trim(),
        capacity: Number(form.capacity),
        monthlyRate: Number(form.monthlyRate),
        underMaintenance: form.underMaintenance,
      };
      if (editingId) {
        await api(`/owner/rooms/${editingId}`, { method: 'PUT', body });
        setNotice(`Room ${body.roomNumber} was updated.`);
      } else {
        await api('/owner/rooms', { method: 'POST', body });
        setNotice(`Room ${body.roomNumber} was added.`);
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

  const handleArchive = async (room) => {
    if (!window.confirm(`Archive room ${room.roomNumber}? It will be hidden from the room list.`)) return;
    setNotice('');
    try {
      await api(`/owner/rooms/${room.id}/archive`, { method: 'POST' });
      setNotice(`Room ${room.roomNumber} was archived.`);
      await load();
    } catch (err) {
      setLoadError(err.message);
    }
  };

  return (
    <section>
      <div className="page-head">
        <h1>Rooms</h1>
        <button type="button" className="btn btn-primary btn-inline" onClick={openAdd}>Add room</button>
      </div>

      {notice && <p className="alert alert-success" role="status">{notice}</p>}
      {loadError && <p className="alert alert-error" role="alert">{loadError}</p>}

      {form && (
        <div className="card">
          <h2>{editingId ? 'Edit room' : 'Add a room'}</h2>
          <form className="form-grid" onSubmit={handleSubmit} noValidate>
            {message && <p className="alert alert-error" role="alert">{message}</p>}

            <label htmlFor="roomNumber">Room number</label>
            <input id="roomNumber" name="roomNumber" value={form.roomNumber} onChange={handleChange} />
            {errors.roomNumber && <span className="field-error">{errors.roomNumber}</span>}

            <label htmlFor="description">Description (optional)</label>
            <input id="description" name="description" value={form.description} onChange={handleChange} placeholder="For example: second floor, with own bathroom" />
            {errors.description && <span className="field-error">{errors.description}</span>}

            <label htmlFor="capacity">Capacity (number of tenants)</label>
            <input id="capacity" name="capacity" type="number" min="1" max="20" value={form.capacity} onChange={handleChange} />
            {errors.capacity && <span className="field-error">{errors.capacity}</span>}

            <label htmlFor="monthlyRate">Monthly rate (PHP)</label>
            <input id="monthlyRate" name="monthlyRate" type="number" min="0" step="0.01" value={form.monthlyRate} onChange={handleChange} />
            {errors.monthlyRate && <span className="field-error">{errors.monthlyRate}</span>}

            {editingId && (
              <label className="check-row">
                <input type="checkbox" name="underMaintenance" checked={form.underMaintenance} onChange={handleChange} />
                Room is under maintenance (no new leases)
              </label>
            )}

            <div className="button-row">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Saving...' : editingId ? 'Save changes' : 'Add room'}
              </button>
              <button type="button" className="btn btn-outline" onClick={closeForm}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      <div className="toolbar">
        <input
          type="search"
          placeholder="Search room number or description"
          aria-label="Search rooms"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select aria-label="Filter by status" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All active rooms</option>
          <option value="AVAILABLE">Available</option>
          <option value="FULL">Full</option>
          <option value="MAINTENANCE">Under maintenance</option>
          <option value="ARCHIVED">Archived</option>
        </select>
      </div>

      {!rooms && !loadError && <p className="center-note">Loading rooms...</p>}
      {rooms && rooms.length === 0 && <p className="card hint">No rooms found. Use "Add room" to create your first one.</p>}
      {rooms && rooms.length > 0 && (
        <div className="table-wrap card">
          <table className="data">
            <thead>
              <tr>
                <th>Room</th>
                <th>Description</th>
                <th>Occupied</th>
                <th>Monthly rate</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rooms.map((room) => (
                <tr key={room.id}>
                  <td><strong>{room.roomNumber}</strong></td>
                  <td>{room.description || '-'}</td>
                  <td>{room.activeLeases} / {room.capacity}</td>
                  <td>{peso(room.monthlyRate)}</td>
                  <td><span className={`pill pill-${room.status.toLowerCase()}`}>{STATUS_LABEL[room.status]}</span></td>
                  <td className="row-actions">
                    {room.status !== 'ARCHIVED' && (
                      <>
                        <button type="button" className="btn btn-small" onClick={() => openEdit(room)}>Edit</button>
                        <button type="button" className="btn btn-small btn-danger" onClick={() => handleArchive(room)}>Archive</button>
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
