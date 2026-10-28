import { useCallback, useEffect, useState } from 'react';
import { api, session } from './api.js';
import AuthScreen from './components/AuthScreen.jsx';
import AnalyzeForm from './components/AnalyzeForm.jsx';
import ResultView from './components/ResultView.jsx';
import HistoryList from './components/HistoryList.jsx';

export default function App() {
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(Boolean(session.token));
  const [view, setView] = useState('new'); // 'new' | 'result' | 'history'
  const [analysis, setAnalysis] = useState(null);

  const logout = useCallback(() => {
    session.clear();
    setUser(null);
    setAnalysis(null);
    setView('new');
  }, []);

  // Restore the session from a saved token.
  useEffect(() => {
    if (!session.token) return;
    api.me()
      .then(setUser)
      .catch(() => session.clear())
      .finally(() => setChecking(false));
  }, []);

  const onAuthError = useCallback((err) => {
    if (err?.status === 401) logout();
  }, [logout]);

  if (checking) return null;

  if (!user) {
    return (
      <AuthScreen
        onAuthenticated={({ token, user }) => {
          session.save(token);
          setUser(user);
        }}
      />
    );
  }

  const showResult = (a) => {
    setAnalysis(a);
    setView('result');
  };

  return (
    <div className="shell">
      <header className="topbar">
        <span className="wordmark">Resume Match</span>
        <nav aria-label="Main">
          <button className={`navlink ${view === 'new' ? 'is-current' : ''}`} onClick={() => setView('new')}>
            New analysis
          </button>
          <button className={`navlink ${view === 'history' ? 'is-current' : ''}`} onClick={() => setView('history')}>
            History
          </button>
          <button className="navlink" onClick={logout}>Log out</button>
        </nav>
      </header>

      <main>
        {view === 'new' && <AnalyzeForm onDone={showResult} onAuthError={onAuthError} firstName={user.name.split(' ')[0]} />}
        {view === 'result' && analysis && <ResultView analysis={analysis} onNew={() => setView('new')} />}
        {view === 'history' && <HistoryList onOpen={showResult} onAuthError={onAuthError} onNew={() => setView('new')} />}
      </main>
    </div>
  );
}
