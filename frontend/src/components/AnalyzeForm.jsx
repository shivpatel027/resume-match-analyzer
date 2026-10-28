import { useRef, useState } from 'react';
import { api } from '../api.js';

const MIN_JD = 50;
const MAX_JD = 15000;
const MAX_FILE = 5 * 1024 * 1024;

export default function AnalyzeForm({ onDone, onAuthError, firstName }) {
  const [file, setFile] = useState(null);
  const [jd, setJd] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [dragging, setDragging] = useState(false);
  const inputRef = useRef(null);

  function pick(f) {
    setError('');
    if (!f) return;
    if (f.type && f.type !== 'application/pdf') return setError('Choose a PDF file. Export your resume from Word or Google Docs as PDF first.');
    if (f.size > MAX_FILE) return setError('That PDF is over 5 MB. Try exporting it without embedded images.');
    setFile(f);
  }

  async function submit(e) {
    e.preventDefault();
    if (!file) return setError('Choose your resume PDF first.');
    if (jd.trim().length < MIN_JD) return setError(`Paste the full job description (at least ${MIN_JD} characters).`);
    setBusy(true);
    setError('');
    try {
      onDone(await api.analyze(file, jd));
    } catch (err) {
      onAuthError(err);
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="analyze" onSubmit={submit}>
      <h1>Hi {firstName}. Which job are you applying for?</h1>

      <div className="analyze-grid">
        <section className="sheet">
          <h2 className="step-title">Your resume</h2>
          <div
            className={`drop ${dragging ? 'is-dragging' : ''} ${file ? 'has-file' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => { e.preventDefault(); setDragging(false); pick(e.dataTransfer.files[0]); }}
          >
            <input
              ref={inputRef}
              id="resume"
              type="file"
              accept="application/pdf,.pdf"
              className="visually-hidden"
              onChange={(e) => pick(e.target.files[0])}
            />
            {file ? (
              <>
                <p className="file-name">{file.name}</p>
                <button type="button" className="linklike" onClick={() => inputRef.current.click()}>Choose a different file</button>
              </>
            ) : (
              <>
                <p>Drop your resume PDF here</p>
                <button type="button" className="btn" onClick={() => inputRef.current.click()}>Choose PDF</button>
                <small>Text-based PDF, up to 5 MB</small>
              </>
            )}
          </div>
        </section>

        <section className="sheet">
          <h2 className="step-title"><label htmlFor="jd">The job description</label></h2>
          <textarea
            id="jd"
            value={jd}
            maxLength={MAX_JD}
            onChange={(e) => setJd(e.target.value)}
            placeholder="Paste the whole posting, including the title, responsibilities and requirements."
          />
          <small className="counter">{jd.length.toLocaleString()} / {MAX_JD.toLocaleString()}</small>
        </section>
      </div>

      {error && <p className="error" role="alert">{error}</p>}

      <div className="actions">
        <button className="btn btn-primary" disabled={busy}>
          {busy ? 'Reviewing your resume…' : 'Check my match'}
        </button>
        {busy && <span className="hint">This usually takes 10 to 20 seconds.</span>}
      </div>
    </form>
  );
}
