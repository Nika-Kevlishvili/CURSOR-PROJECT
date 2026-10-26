import { useEffect, useState } from 'react';
import { api, type DashboardResponse } from '../api';

const empty = {
  environment: '',
  from: '',
  to: '',
  status: '',
  rootCause: '',
  fixStatus: '',
};

export function Dashboard() {
  const [filters, setFilters] = useState(empty);
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load(next = filters) {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(next)) if (value) params.set(key, value);
    api<DashboardResponse>(`/api/dashboard?${params.toString()}`)
      .then((payload) => { setData(payload); setError(null); })
      .catch((reason: Error) => setError(reason.message));
  }

  useEffect(() => { load(); }, []);

  const counts = data?.counts;
  const cards: [string, number][] = counts ? [
    ['Imported reports', counts.importedReports],
    ['Tests analyzed', counts.testsAnalyzed],
    ['Failed tests', counts.failedTests],
    ['Suspected flaky', counts.suspectedFlaky],
    ['Confirmed intermittent', counts.confirmedIntermittent],
    ['Environment-specific', counts.environmentSpecific],
    ['Probable defects', counts.probableDefects],
    ['Insufficient evidence', counts.insufficientEvidence],
    ['Fixes awaiting approval', counts.fixesAwaitingApproval],
    ['Approved, not validated', counts.approvedAwaitingValidation],
    ['Failed again after a fix', counts.regressedAfterFix],
    ['Diagnostic recommendations', counts.diagnosticRecommendations],
  ] : [];

  return (
    <section>
      <h1>What needs attention</h1>
      <p className="muted">Start with failed tests. Open a suggestion only when you are ready to approve or reject that specific change.</p>
      <div className="hero-actions">
        <a className="button primary" href="#/tests">Analyze failed tests</a>
        <a className="button" href="#/fixes">Approve suggested fixes{counts ? ` (${counts.fixesAwaitingApproval})` : ''}</a>
        <a className="button" href="#/import">Import a report</a>
      </div>
      <form className="filters" onSubmit={(event) => { event.preventDefault(); load(); }}>
        <label>Environment
          <select value={filters.environment} onChange={(event) => setFilters({ ...filters, environment: event.target.value })}>
            <option value="">All</option>
            <option value="dev">Dev</option>
            <option value="dev2">Dev2</option>
            <option value="test">Test</option>
          </select>
        </label>
        <label>From <input type="date" value={filters.from} onChange={(event) => setFilters({ ...filters, from: event.target.value })} /></label>
        <label>To <input type="date" value={filters.to} onChange={(event) => setFilters({ ...filters, to: event.target.value })} /></label>
        <label>Status
          <select value={filters.status} onChange={(event) => setFilters({ ...filters, status: event.target.value })}>
            <option value="">All</option>
            {['failed', 'timedOut', 'flaky', 'passed', 'skipped', 'interrupted'].map((status) => <option key={status}>{status}</option>)}
          </select>
        </label>
        <label>Root cause <input value={filters.rootCause} onChange={(event) => setFilters({ ...filters, rootCause: event.target.value })} placeholder="timing_or_synchronization" /></label>
        <label>Fix status
          <select value={filters.fixStatus} onChange={(event) => setFilters({ ...filters, fixStatus: event.target.value })}>
            <option value="">All</option>
            {['PROPOSED', 'APPROVED', 'APPLIED', 'VALIDATED', 'REJECTED', 'BLOCKED', 'VALIDATION_FAILED', 'STALE'].map((status) => <option key={status}>{status}</option>)}
          </select>
        </label>
        <button type="submit">Apply filters</button>
      </form>
      {error && <p className="error">{error}</p>}
      <div className="grid">
        {cards.map(([label, value]) => (
          <article className="card" key={label}><strong>{value}</strong><span>{label}</span></article>
        ))}
      </div>
      {data && (
        <>
          <h2>Rates with sample size</h2>
          <p>Failure frequency: {data.failureFrequencyLabel}</p>
          <p>Retry recovery: {data.retryRecoveryLabel}</p>
          <p className="muted">Failure frequency uses failed, timed-out, and interrupted attempts over observed pass/fail attempts. Skipped attempts are excluded. Retry recovery is initial failures that later passed, divided by initial failures that had another attempt.</p>
        </>
      )}
    </section>
  );
}
