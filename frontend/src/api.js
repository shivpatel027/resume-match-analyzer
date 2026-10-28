const TOKEN_KEY = 'rma.token';

export const session = {
  get token() {
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
  },
  save(token) {
    try { localStorage.setItem(TOKEN_KEY, token); } catch { /* private mode: stay logged in for this tab only */ }
  },
  clear() {
    try { localStorage.removeItem(TOKEN_KEY); } catch { /* ignore */ }
  },
};

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function request(path, { method = 'GET', json, form } = {}) {
  const headers = {};
  const token = session.token;
  if (token) headers.Authorization = `Bearer ${token}`;

  let body;
  if (json) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(json);
  } else if (form) {
    body = form; // the browser sets the multipart boundary
  }

  let res;
  try {
    res = await fetch(path, { method, headers, body });
  } catch {
    throw new ApiError(0, 'Can’t reach the server. Check your connection and try again.');
  }

  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const fallback = res.status === 401 ? 'Your session has ended. Log in again.' : `Request failed (${res.status}).`;
    throw new ApiError(res.status, data?.detail || fallback);
  }
  return data;
}

export const api = {
  register: (name, email, password) => request('/api/auth/register', { method: 'POST', json: { name, email, password } }),
  login: (email, password) => request('/api/auth/login', { method: 'POST', json: { email, password } }),
  me: () => request('/api/auth/me'),
  analyze: (file, jobDescription) => {
    const form = new FormData();
    form.append('resume', file);
    form.append('jobDescription', jobDescription);
    return request('/api/analyses', { method: 'POST', form });
  },
  history: () => request('/api/analyses'),
  get: (id) => request(`/api/analyses/${id}`),
  remove: (id) => request(`/api/analyses/${id}`, { method: 'DELETE' }),
};
