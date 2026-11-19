import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="auth-page">
      <div className="card auth-card">
        <h1>Page not found</h1>
        <Link className="btn btn-primary" to="/">Back to start</Link>
      </div>
    </div>
  );
}
