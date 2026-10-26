import { useEffect, useState } from 'react';
import { api, type Proposal } from '../api';
import { FixReview } from '../components/FixReview';
import { statusTone } from '../labels';

function idFromHash(): string | null {
  const hash = window.location.hash;
  const query = hash.includes('?') ? hash.slice(hash.indexOf('?') + 1) : '';
  return new URLSearchParams(query).get('id');
}

export function Fixes() {
  const [proposals, setProposals] = useState<Proposal[]>([]);
  const [currentId, setCurrentId] = useState<string | null>(idFromHash());
  const [current, setCurrent] = useState<Proposal | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<'PROPOSED' | 'all'>('PROPOSED');

  function loadList() {
    const path = filter === 'all' ? '/api/proposals' : '/api/proposals?state=PROPOSED';
    api<{ proposals: Proposal[] }>(path)
      .then((payload) => {
        setProposals(payload.proposals);
        setCurrentId((id) => id && payload.proposals.some((item) => item.id === id) ? id : payload.proposals[0]?.id ?? null);
      })
      .catch((reason: Error) => setError(reason.message));
  }

  useEffect(() => { loadList(); }, [filter]);

  useEffect(() => {
    if (!currentId) {
      setCurrent(null);
      return;
    }
    api<Proposal>(`/api/proposals/${currentId}`).then(setCurrent).catch((reason: Error) => setError(reason.message));
  }, [currentId]);

  const waiting = proposals.filter((item) => item.state === 'PROPOSED' && item.kind === 'patch');

  return (
    <section className="workspace">
      <header className="page-head">
        <div>
          <p className="eyebrow">Approval</p>
          <h1>Suggested fixes</h1>
          <p className="muted">Review one suggestion at a time. Approve and apply writes only that diff. Reject and Leave pending do not change tests.</p>
        </div>
      </header>
      <div className="filters">
        <button type="button" className={filter === 'PROPOSED' ? 'primary' : ''} onClick={() => setFilter('PROPOSED')}>Open suggestions ({filter === 'PROPOSED' ? proposals.length : waiting.length})</button>
        <button type="button" className={filter === 'all' ? 'primary' : ''} onClick={() => setFilter('all')}>All suggestions</button>
      </div>
      {error && <p className="error">{error}</p>}
      <div className="workspace-grid">
        <div className="queue">
          {proposals.length === 0 && (
            <div className="empty">
              <h2>Nothing to approve</h2>
              <p>Import a report and open Analyze. Suggestions show up here when a failed test has a concrete edit or a recommendation.</p>
              <a className="button primary" href="#/tests">Analyze failed tests</a>
            </div>
          )}
          {proposals.map((item) => (
            <button key={item.id} type="button" className={currentId === item.id ? 'queue-item selected' : 'queue-item'} onClick={() => setCurrentId(item.id)}>
              <span className={`pill ${statusTone(item.state)}`}>{item.state}</span>
              <strong>{item.summary}</strong>
              <span className="muted">{item.kind === 'patch' ? 'Can change a test file' : 'Recommendation only'} · {item.confidence} confidence</span>
            </button>
          ))}
        </div>
        <div>
          {!current && proposals.length > 0 && <p className="muted">Select a suggestion.</p>}
          {current && <FixReview proposal={current} onChange={(next) => { setCurrent(next); loadList(); }} />}
        </div>
      </div>
    </section>
  );
}
