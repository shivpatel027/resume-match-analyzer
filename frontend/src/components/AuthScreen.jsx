import { useState } from 'react';
import { api } from '../api.js';

export default function AuthScreen({ onAuthenticated }) {
  const [mode, setMode] = useState('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const isRegister = mode === 'register';

  async function submit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      const res = isRegister ? await api.register(name, email, password) : await api.login(email, password);
      onAuthenticated(res);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth">
      <div className="auth-intro">
        <h1 className="wordmark wordmark-large">Resume Match</h1>
        <p className="lede">
          Upload your resume, paste a job posting, and see how well they line up: which skills you already show,
          which ones are missing, and how to rewrite your bullets for this role.
        </p>
      </div>

      <form className="sheet auth-form" onSubmit={submit} noValidate>
        <h2>{isRegister ? 'Create an account' : 'Log in'}</h2>

        {isRegister && (
          <label className="field">
            <span>Name</span>
            <input value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" required />
          </label>
        )}
        <label className="field">
          <span>Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
        </label>
        <label className="field">
          <span>Password</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            minLength={isRegister ? 8 : undefined}
            required
          />
          {isRegister && <small>At least 8 characters.</small>}
        </label>

        {error && <p className="error" role="alert">{error}</p>}

        <button className="btn btn-primary" disabled={busy}>
          {busy ? (isRegister ? 'Creating account…' : 'Logging in…') : isRegister ? 'Create account' : 'Log in'}
        </button>

        <p className="switch">
          {isRegister ? 'Already have an account?' : 'New here?'}{' '}
          <button type="button" className="linklike" onClick={() => { setMode(isRegister ? 'login' : 'register'); setError(''); }}>
            {isRegister ? 'Log in' : 'Create an account'}
          </button>
        </p>
      </form>
    </div>
  );
}
