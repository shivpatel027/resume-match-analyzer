import { useEffect, useState } from 'react';
import { api } from '../api.js';

export default function HistoryList({ onOpen, onAuthError, onNew }) {
  const [items, setItems] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.history()
      .then(setItems)
      .catch((err) => { onAuthError(err); setError(err.message); });
  }, [onAuthError]);

  async function open(id) {
    try {
      onOpen(await api.get(id));
    } catch (err) {
      onAuthError(err);
      setError(err.message);
    }
  }

  async function remove(item) {
    if (!window.confirm(`Delete the analysis for “${item.jobTitle}”?`)) return;
    try {
      await api.remove(item.id);
      setItems((list) => list.filter((x) => x.id !== item.id));
    } catch (err) {
      onAuthError(err);
      setError(err.message);
    }
  }

  return (
    <section className="history">
      <h1>History</h1>
      {error && <p className="error" role="alert">{error}</p>}
      {items && items.length === 0 && (
        <div className="empty">
          <p>Your past analyses will be listed here.</p>
          <button className="btn btn-primary" onClick={onNew}>Run your first analysis</button>
        </div>
      )}
      {items && items.length > 0 && (
        <ul className="history-list">
          {items.map((item) => (
            <li key={item.id} className="history-row">
              <button className="history-open" onClick={() => open(item.id)}>
                <span className="history-score">{item.score}</span>
                <span className="history-text">
                  <span className="history-title">{item.jobTitle}</span>
                  <span className="history-meta">
                    {new Date(item.createdAt).toLocaleDateString()}, {item.resumeFileName}
                    {item.source === 'KEYWORD_FALLBACK' && ' (keyword check only)'}
                  </span>
                </span>
              </button>
              <button className="linklike danger" onClick={() => remove(item)} aria-label={`Delete ${item.jobTitle}`}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
