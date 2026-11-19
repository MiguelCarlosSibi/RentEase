import { useAuth } from '../context/AuthContext.jsx';

export default function Profile() {
  const { user } = useAuth();

  return (
    <section>
      <h1>My Profile</h1>
      <div className="card">
        <dl className="profile-list">
          <dt>Full name</dt>
          <dd>{user.fullName}</dd>
          <dt>Email</dt>
          <dd>{user.email}</dd>
          <dt>Mobile number</dt>
          <dd>{user.mobileNumber}</dd>
          <dt>Role</dt>
          <dd>{user.role}</dd>
        </dl>
      </div>
    </section>
  );
}
