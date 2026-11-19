// Small fetch wrapper. The access token lives in sessionStorage, so it is cleared when the tab closes.
const TOKEN_KEY = 'rentease_token';

export const getToken = () => sessionStorage.getItem(TOKEN_KEY);

export const setToken = (token) => {
  if (token) {
    sessionStorage.setItem(TOKEN_KEY, token);
  } else {
    sessionStorage.removeItem(TOKEN_KEY);
  }
};

function apiError(message, status, errors) {
  const error = new Error(message);
  error.status = status;
  error.errors = errors || {};
  return error;
}

export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (auth && token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw apiError('Cannot reach the server. Make sure the backend is running.', 0);
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    if (response.status === 401 && auth && token) {
      // The session ended or the token is no longer valid.
      setToken(null);
      window.dispatchEvent(new Event('auth:expired'));
    }
    throw apiError(data?.message || 'Request failed', response.status, data?.errors);
  }
  return data;
}
