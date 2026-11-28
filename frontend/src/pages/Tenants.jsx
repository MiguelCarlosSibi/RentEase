import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { EMAIL_PATTERN, MOBILE_PATTERN, MOBILE_RULE } from '../validators.js';

const EMPTY = { fullName: '', email: '', mobileNumber: '', emergencyContactName: '', emergencyContactNumber: '' };

export default function Tenants() {
  const [tenants, setTenants] = useState(null);
  const [loadError, setLoadError] = useState('');
  const [search, setSearch] = useState('');

  const [form, setForm] = useState(null); // null means the form is closed
  const [editingId, setEditingId] = useState(null);
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [result, setResult] = useState(null); // result of adding a tenant (shows the temporary password if needed)
  const [notice, setNotice] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(() => {
    const params = new URLSearchParams();
    if (search.trim()) params.set('search', search.trim());
    return api(`/owner/tenants?${params.toString()}`)
      .then((data) => {
        setTenants(data);
        setLoadError('');
      })
      .catch((err) => setLoadError(err.message));
  }, [search]);

  useEffect(() => {
    load();
  }, [load]);

  const openAdd = () => {
    setEditingId(null);
    setForm(EMPTY);
    setErrors({});
    setMessage('');
    setNotice('');
    setResult(null);
  };

  const openEdit = (tenant) => {
    setEditingId(tenant.id);
    setForm({
      fullName: tenant.fullName,
      email: tenant.email,
      mobileNumber: tenant.mobileNumber,
      emergencyContactName: tenant.emergencyContactName,
      emergencyContactNumber: tenant.emergencyContactNumber,
    });
    setErrors({});
    setMessage('');
    setNotice('');
    setResult(null);
  };

  const closeForm = () => {
    setForm(null);
    setEditingId(null);
    setErrors({});
    setMessage('');
  };

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const validate = () => {
    const found = {};
    if (!form.fullName.trim()) found.fullName = 'Full name is required';
    if (!editingId) {
      if (!form.email.trim()) found.email = 'Email is required';
      else if (!EMAIL_PATTERN.test(form.email.trim())) found.email = 'Enter a valid email address';
    }
    if (!form.mobileNumber.trim()) found.mobileNumber = 'Mobile number is required';
    else if (!MOBILE_PATTERN.test(form.mobileNumber.trim())) found.mobileNumber = MOBILE_RULE;
    if (!form.emergencyContactName.trim()) found.emergencyContactName = 'Emergency contact name is required';
    if (!form.emergencyContactNumber.trim()) found.emergencyContactNumber = 'Emergency contact number is required';
    else if (!MOBILE_PATTERN.test(form.emergencyContactNumber.trim())) found.emergencyContactNumber = MOBILE_RULE;
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
        fullName: form.fullName.trim(),
        mobileNumber: form.mobileNumber.trim(),
        emergencyContactName: form.emergencyContactName.trim(),
        emergencyContactNumber: form.emergencyContactNumber.trim(),
      };
      if (editingId) {
        await api(`/owner/tenants/${editingId}`, { method: 'PUT', body });
        setNotice(`${body.fullName}'s record was updated.`);
        closeForm();
      } else {
        const created = await api('/owner/tenants', { method: 'POST', body: { ...body, email: form.email.trim() } });
        setResult(created);
        closeForm();
      }
      await load();
    } catch (err) {
      setErrors(err.errors || {});
      setMessage(err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const toggleActive = async (tenant) => {
    const action = tenant.active ? 'deactivate' : 'activate';
    const question = tenant.active
      ? `Deactivate ${tenant.fullName}? They will not be able to log in, but their records are kept.`
      : `Activate ${tenant.fullName}?`;
    if (!window.confirm(question)) return;
    setNotice('');
    setResult(null);
    try {
      await api(`/owner/tenants/${tenant.id}/${action}`, { method: 'POST' });
      setNotice(`${tenant.fullName} was ${tenant.active ? 'deactivated' : 'activated'}.`);
      await load();
    } catch (err) {
      setLoadError(err.message);
    }
  };

  return (
    <section>
      <div className="page-head">
        <h1>Tenants</h1>
        <button type="button" className="btn btn-primary btn-inline" onClick={openAdd}>Add tenant</button>
      </div>

      {notice && <p className="alert alert-success" role="status">{notice}</p>}
      {loadError && <p className="alert alert-error" role="alert">{loadError}</p>}

      {result && (
        <div className={`card ${result.credentialsSentBySms ? '' : 'card-warn'}`} role="status">
          <h2>{result.tenant.fullName} was added</h2>
          <p>{result.message}</p>
          {result.temporaryPassword && (
            <p>
              Login email: <strong>{result.tenant.email}</strong><br />
              Temporary password: <strong className="mono">{result.temporaryPassword}</strong>
              <br />
              <small className="hint">This is shown only once. The tenant must change it at first login.</small>
            </p>
          )}
          <button type="button" className="btn btn-outline" onClick={() => setResult(null)}>Done</button>
        </div>
      )}

      {form && (
        <div className="card">
          <h2>{editingId ? 'Edit tenant' : 'Add a tenant'}</h2>
          <form className="form-grid" onSubmit={handleSubmit} noValidate>
            {message && <p className="alert alert-error" role="alert">{message}</p>}

            <label htmlFor="fullName">Full name</label>
            <input id="fullName" name="fullName" value={form.fullName} onChange={handleChange} />
            {errors.fullName && <span className="field-error">{errors.fullName}</span>}

            <label htmlFor="email">Email (login ID)</label>
            <input id="email" name="email" type="email" value={form.email} onChange={handleChange}
              readOnly={Boolean(editingId)} className={editingId ? 'readonly' : ''} />
            {errors.email && <span className="field-error">{errors.email}</span>}

            <label htmlFor="mobileNumber">Mobile number</label>
            <input id="mobileNumber" name="mobileNumber" type="tel" placeholder="09XXXXXXXXX" value={form.mobileNumber} onChange={handleChange} />
            {errors.mobileNumber && <span className="field-error">{errors.mobileNumber}</span>}
            {!editingId && <small className="hint">The login details are sent to this number by SMS.</small>}

            <label htmlFor="emergencyContactName">Emergency contact name</label>
            <input id="emergencyContactName" name="emergencyContactName" value={form.emergencyContactName} onChange={handleChange} />
            {errors.emergencyContactName && <span className="field-error">{errors.emergencyContactName}</span>}

            <label htmlFor="emergencyContactNumber">Emergency contact number</label>
            <input id="emergencyContactNumber" name="emergencyContactNumber" type="tel" placeholder="09XXXXXXXXX" value={form.emergencyContactNumber} onChange={handleChange} />
            {errors.emergencyContactNumber && <span className="field-error">{errors.emergencyContactNumber}</span>}

            <div className="button-row">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Saving...' : editingId ? 'Save changes' : 'Add tenant'}
              </button>
              <button type="button" className="btn btn-outline" onClick={closeForm}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      <div className="toolbar">
        <input
          type="search"
          placeholder="Search name, email, or mobile number"
          aria-label="Search tenants"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {!tenants && !loadError && <p className="center-note">Loading tenants...</p>}
      {tenants && tenants.length === 0 && <p className="card hint">No tenants found. Use "Add tenant" to create a tenant account.</p>}
      {tenants && tenants.length > 0 && (
        <div className="table-wrap card">
          <table className="data">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Mobile</th>
                <th>Emergency contact</th>
                <th>Leases</th>
                <th>Account</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {tenants.map((t) => (
                <tr key={t.id}>
                  <td><strong>{t.fullName}</strong></td>
                  <td>{t.email}</td>
                  <td>{t.mobileNumber}</td>
                  <td>{t.emergencyContactName}<br /><small className="hint">{t.emergencyContactNumber}</small></td>
                  <td>{t.activeLeases}</td>
                  <td><span className={`pill ${t.active ? 'pill-available' : 'pill-archived'}`}>{t.active ? 'Active' : 'Inactive'}</span></td>
                  <td className="row-actions">
                    <button type="button" className="btn btn-small" onClick={() => openEdit(t)}>Edit</button>
                    <button type="button" className={`btn btn-small ${t.active ? 'btn-danger' : ''}`} onClick={() => toggleActive(t)}>
                      {t.active ? 'Deactivate' : 'Activate'}
                    </button>
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
