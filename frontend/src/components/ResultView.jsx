function verdict(score) {
  if (score >= 80) return 'Strong match';
  if (score >= 60) return 'Good match with gaps';
  if (score >= 40) return 'Partial match';
  return 'Weak match';
}

export default function ResultView({ analysis, onNew }) {
  const { result, jobTitle, resumeFileName, createdAt } = analysis;
  const fallback = result.source === 'KEYWORD_FALLBACK';

  return (
    <article className="result">
      <header className="result-head">
        <div className="score" aria-label={`Match score ${result.score} out of 100`}>
          <span className="score-number">{result.score}</span>
          <span className="score-scale">/100</span>
        </div>
        <div>
          <h1>{verdict(result.score)}</h1>
          <p className="result-meta">
            {jobTitle}
            <br />
            <span>{resumeFileName}, checked {new Date(createdAt).toLocaleString()}</span>
          </p>
        </div>
      </header>

      {fallback && (
        <p className="notice">
          The AI review was unavailable, so this score comes from keyword matching only. Run the analysis again later
          for a full review with rewrite suggestions.
        </p>
      )}

      {result.summary && <p className="summary">{result.summary}</p>}

      <section className="skills">
        <div>
          <h2>Skills you show</h2>
          {result.matchedSkills.length ? (
            <ul className="marks">
              {result.matchedSkills.map((s) => <li key={s} className="mark mark-hit">{s}</li>)}
            </ul>
          ) : <p className="muted">None of the job’s listed skills were found in your resume.</p>}
        </div>
        <div>
          <h2>Skills the job asks for that are missing</h2>
          {result.missingSkills.length ? (
            <ul className="marks">
              {result.missingSkills.map((s) => <li key={s} className="mark mark-miss">{s}</li>)}
            </ul>
          ) : <p className="muted">Nothing missing. Your resume covers every listed skill.</p>}
        </div>
      </section>

      {result.suggestions.length > 0 && (
        <section className="edits">
          <h2>Suggested edits</h2>
          <ol>
            {result.suggestions.map((s, i) => (
              <li key={i} className="edit">
                <div className="edit-text">
                  {s.original ? <del>{s.original}</del> : <span className="edit-new">New bullet</span>}
                  <ins>{s.improved}</ins>
                </div>
                {s.reason && <p className="edit-note">{s.reason}</p>}
              </li>
            ))}
          </ol>
        </section>
      )}

      <div className="actions">
        <button className="btn btn-primary" onClick={onNew}>Check another job</button>
      </div>
    </article>
  );
}
