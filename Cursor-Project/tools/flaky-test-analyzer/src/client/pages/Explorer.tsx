import { useEffect, useState } from 'react';
import { api, type Proposal, type ResultRow } from '../api';
import { FixReview } from '../components/FixReview';
import { CAUSE_LABEL, CLASS_LABEL, labelOf, statusTone } from '../labels';

type Detail = {
  result: ResultRow;
  attempts: {
    retryIndex: number | null;
    status: string;
    durationMs: number | null;
    startTime: string | null;
    workerIndex: number | null;
    errorMessage: string | null;
  }[];
  assessment: {
    categories: string[];
    evidence: string[];
    missing: string[];
    contradicts: string[];
    nextAction: string;
    frequency: { numerator: number; denominator: number; rate: number | null };
    recovery: { numerator: number; denominator: number; rate: number | null };
  } | null;
  source: { path: string | null; missingReason: string | null; excerpt: string | null; available: boolean };
  related: ResultRow[];
  attachmentsNote: string | null;
};

export function Explorer() {
  const [q, setQ] = useState('');
  const [environment, setEnvironment] = useState('');
  const [onlyFailures, setOnlyFailures] = useState(true);
  const [rows, setRows] = useState<ResultRow[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [detail, setDetail] = useState<Detail | null>(null);
  const [proposal, setProposal] = useState<Proposal | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  function load() {
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (environment) params.set('environment', environment);
    if (onlyFailures) params.set('attention', '1');
    setLoading(true);
    api<{ rows?: ResultRow[] }>(`/api/results?${params}`)
      .then((payload) => {
        const next = payload.rows ?? [];
        setRows(next);
        setError(null);
        setSelected((current) => current && next.some((row) => row.id === current) ? current : next[0]?.id ?? null);
      })
      .catch((reason: Error) => setError(reason.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => { load(); }, []);

  useEffect(() => {
    if (!selected) {
      setDetail(null);
      setProposal(null);
      return;
    }
    api<Detail>(`/api/results/${selected}`)
      .then((payload) => {
        setDetail(payload);
        if (payload.result.proposalId) {
          api<Proposal>(`/api/proposals/${payload.result.proposalId}`).then(setProposal).catch(() => setProposal(null));
        } else {
          setProposal(null);
        }
      })
      .catch((reason: Error) => setError(reason.message));
  }, [selected]);

  return (
    <section className="workspace">
      <header className="page-head">
        <div>
          <p className="eyebrow">Analyzer</p>
          <h1>Failed tests</h1>
          <p className="muted">Each failure is classified from the imported reports and the current test source. A suggestion appears when a specific edit is justified. You approve it before any file changes.</p>
        </div>
      </header>
      <form className="filters" onSubmit={(event) => { event.preventDefault(); load(); }}>
        <label>Search <input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Title, file, or error" /></label>
        <label>Environment
          <select value={environment} onChange={(event) => setEnvironment(event.target.value)}>
            <option value="">All</option>
            <option value="dev">Dev</option>
            <option value="dev2">Dev2</option>
            <option value="test">Test</option>
          </select>
        </label>
        <label className="check"><input type="checkbox" checked={onlyFailures} onChange={(event) => setOnlyFailures(event.target.checked)} /> Failures and flakes only</label>
        <button type="button" onClick={() => {
          setLoading(true);
          setError(null);
          api<{ updated: number }>('/api/reports/reanalyze', { method: 'POST', body: '{}' })
            .then(() => load())
            .catch((reason: Error) => { setError(reason.message); setLoading(false); });
        }}>Re-read saved reports</button>
      </form>
      {error && <p className="error">{error}</p>}
      <div className="workspace-grid">
        <div className="queue">
          {loading && <p className="muted">Loading results…</p>}
          {!loading && rows.length === 0 && (
            <div className="empty">
              <h2>No failed tests stored</h2>
              <p>Import a Playwright report ZIP from Dev, Dev2, or Test. Passing tests are hidden while “Failures and flakes only” is on.</p>
              <a className="button primary" href="#/import">Import a report</a>
            </div>
          )}
          {rows.map((row) => (
            <button key={row.id} type="button" className={selected === row.id ? 'queue-item selected' : 'queue-item'} onClick={() => setSelected(row.id)}>
              <span className={`pill ${statusTone(row.status)}`}>{row.status}</span>
              <strong>{row.title}</strong>
              <span className="muted">{row.environment} · {row.filePath ?? 'file unavailable'}{row.line ? `:${row.line}` : ''}</span>
              <span>{labelOf(CLASS_LABEL, row.classification)} · {labelOf(CAUSE_LABEL, row.primaryCause)}</span>
            </button>
          ))}
        </div>
        <div>
          {!detail && selected && <div className="empty"><h2>Loading analysis…</h2><p>Reading the failure, source, and suggestion.</p></div>}
          {!detail && !loading && !selected && <div className="empty"><h2>Select a test</h2><p>The analysis, source, and suggested fix open here.</p></div>}
          {detail && (
            <div className="analysis">
              <header className="review-head">
                <div>
                  <p className="eyebrow">{detail.result.environment}{detail.result.label ? ` · ${detail.result.label}` : ''}</p>
                  <h2>{detail.result.fullTitle}</h2>
                </div>
                <span className={`pill ${statusTone(detail.result.status)}`}>{detail.result.status}</span>
              </header>
              <div className="meta-row">
                <span className={`pill ${statusTone(detail.result.classification)}`}>{labelOf(CLASS_LABEL, detail.result.classification)}</span>
                <span className="pill neutral">{labelOf(CAUSE_LABEL, detail.result.primaryCause)}</span>
                <span className="pill neutral">{detail.result.confidence} confidence</span>
                <span className="pill neutral">{detail.result.fixStatus === 'none' ? 'No suggestion yet' : detail.result.fixStatus}</span>
              </div>
              <h3>What failed</h3>
              <pre className="error-block">{detail.result.errorSummary ?? 'The report did not include an error message.'}</pre>
              <p className="muted">Expected status: {detail.result.expectedStatus ?? 'unavailable'}. Duration: {detail.result.durationMs ?? 'unavailable'} ms. Source: {detail.result.filePath ?? 'unavailable'}{detail.result.line ? `:${detail.result.line}` : ''}.</p>
              <h3>Attempts</h3>
              <ol className="attempts">
                {detail.attempts.map((attempt, index) => (
                  <li key={`${attempt.retryIndex}-${index}`}>
                    <span className={`pill ${statusTone(attempt.status)}`}>{attempt.status}</span>
                    Attempt {attempt.retryIndex ?? 'unavailable'} · {attempt.durationMs ?? 'duration unavailable'} ms · worker {attempt.workerIndex ?? 'unavailable'}
                    {attempt.errorMessage && <span className="attempt-error">{attempt.errorMessage}</span>}
                  </li>
                ))}
              </ol>
              <h3>Analysis</h3>
              {detail.assessment ? (
                <div className="review-grid">
                  <div>
                    <h3>Evidence</h3>
                    <ul>{detail.assessment.evidence.map((item) => <li key={item}>{item}</li>)}</ul>
                    <p>Failure frequency {detail.assessment.frequency.numerator}/{detail.assessment.frequency.denominator}. Retry recovery {detail.assessment.recovery.numerator}/{detail.assessment.recovery.denominator}.</p>
                  </div>
                  <div>
                    <h3>Still missing</h3>
                    <ul>{detail.assessment.missing.length === 0 ? <li>Nothing else was flagged.</li> : detail.assessment.missing.map((item) => <li key={item}>{item}</li>)}</ul>
                    {detail.assessment.contradicts.length > 0 && (
                      <>
                        <h3>Do not assume</h3>
                        <ul>{detail.assessment.contradicts.map((item) => <li key={item}>{item}</li>)}</ul>
                      </>
                    )}
                  </div>
                </div>
              ) : <p>Analysis is not stored for this result.</p>}
              <p className="note"><strong>Next step:</strong> {detail.assessment?.nextAction ?? 'Import another execution before changing the test.'}</p>
              {detail.attachmentsNote && <p className="banner">{detail.attachmentsNote}</p>}
              <h3>Source around the failure</h3>
              {detail.source.available ? <pre>{detail.source.excerpt}</pre> : <p>{detail.source.missingReason}</p>}
              {detail.related.length > 0 && (
                <>
                  <h3>Same error in other tests</h3>
                  <ul className="plain-list">{detail.related.map((row) => <li key={row.id}>{row.environment}: {row.fullTitle}</li>)}</ul>
                </>
              )}
              {proposal ? (
                <FixReview proposal={proposal} onChange={setProposal} />
              ) : (
                <div className="empty">
                  <h2>No code change suggested</h2>
                  <p>The analyzer did not find a single safe edit. Use the next step above instead of weakening the test.</p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
