import { useState } from 'react';
import { api, type Proposal } from '../api';
import { DiffView } from './DiffView';
import { CAUSE_LABEL, CLASS_LABEL, labelOf, statusTone } from '../labels';

export function FixReview({
  proposal,
  onChange,
}: {
  proposal: Proposal;
  onChange: (next: Proposal) => void;
}) {
  const [confirming, setConfirming] = useState<'approve_and_apply' | 'reject' | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);
  const [environment, setEnvironment] = useState('dev');
  const [repetitions, setRepetitions] = useState(1);
  const [confirmRun, setConfirmRun] = useState(false);
  const canApply = proposal.kind === 'patch' && proposal.state === 'PROPOSED';
  const canReject = proposal.state === 'PROPOSED' || proposal.state === 'APPROVED';

  async function send(decision: string) {
    setBusy(true);
    setError(null);
    try {
      const path = decision === 'apply'
        ? `/api/proposals/${proposal.id}/apply`
        : `/api/proposals/${proposal.id}/decision`;
      const body = decision === 'apply'
        ? { proposalId: proposal.id }
        : { decision, proposalId: proposal.id };
      const next = await api<Proposal>(path, { method: 'POST', body: JSON.stringify(body) });
      setConfirming(null);
      setNote(decision === 'approve_and_apply' || decision === 'apply'
        ? 'Applied. The resulting diff is below. The change is not committed.'
        : `Saved. This suggestion is now ${next.state}.`);
      onChange(next);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'The decision was not saved.');
    } finally {
      setBusy(false);
    }
  }

  async function validate() {
    setBusy(true);
    setError(null);
    try {
      await api(`/api/proposals/${proposal.id}/validate`, {
        method: 'POST',
        body: JSON.stringify({ proposalId: proposal.id, environment, repetitions, confirm: true }),
      });
      const next = await api<Proposal>(`/api/proposals/${proposal.id}`);
      setNote('Validation finished. Use the counts below. One passing run does not prove an intermittent test is fixed.');
      onChange(next);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Validation did not start.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="review">
      <header className="review-head">
        <div>
          <p className="eyebrow">Suggested fix</p>
          <h2>{proposal.summary}</h2>
        </div>
        <span className={`pill ${statusTone(proposal.state)}`}>{proposal.state}</span>
      </header>
      <p className="lede">{proposal.why}</p>
      {proposal.kind !== 'patch' && <p className="banner">Apply is off for this item. The failure is real, and changing the test would hide it. Reject it or leave it pending.</p>}
      <div className="meta-row">
        <span className={`pill ${statusTone(proposal.confidence)}`}>{proposal.confidence} confidence</span>
        <span className="pill neutral">{proposal.kind === 'patch' ? 'Code change' : 'Recommendation only'}</span>
        <span className="pill neutral">{labelOf(CLASS_LABEL, proposal.rootCause.classification)}</span>
        <span className="pill neutral">{labelOf(CAUSE_LABEL, proposal.rootCause.cause)}</span>
      </div>

      <section className="review-grid">
        <div>
          <h3>Why this suggestion</h3>
          <p>{proposal.rationale}</p>
          <h3>Expected effect</h3>
          <p>{proposal.impact}</p>
        </div>
        <div>
          <h3>Risk if you apply it</h3>
          <p>{proposal.risks}</p>
          <h3>How to check it</h3>
          <p>{proposal.validationPlan}</p>
        </div>
      </section>

      <h3>Tests</h3>
      <ul className="plain-list">
        {proposal.tests.map((test) => <li key={test.test_id}>{test.full_title}<span>{test.file_path ?? 'file unavailable'}</span></li>)}
        {proposal.tests.length === 0 && <li>No test is linked.</li>}
      </ul>

      {proposal.files.map((file) => (
        <section key={file.path}>
          <h3>{file.path}</h3>
          <div className="code-pair">
            <div>
              <p className="code-label">Current code</p>
              <pre>{file.original || '(empty)'}</pre>
            </div>
            <div>
              <p className="code-label">Suggested code</p>
              <pre>{file.proposed || '(this part is removed)'}</pre>
            </div>
          </div>
        </section>
      ))}

      <h3>Diff you would approve</h3>
      <DiffView diff={proposal.diff} />
      {proposal.appliedDiff && (
        <>
          <h3>What was actually written</h3>
          <DiffView diff={proposal.appliedDiff} />
        </>
      )}
      {proposal.aiNote && <p className="note"><strong>Extra note:</strong> {proposal.aiNote}</p>}
      {proposal.blockReason && <p className="banner">{proposal.blockReason}</p>}

      {error && <p className="error">{error}</p>}
      {note && <p className="ok">{note}</p>}

      <div className="action-bar">
        <button className="primary" disabled={!canApply || busy} onClick={() => setConfirming('approve_and_apply')}>Approve and apply this fix</button>
        <button disabled={!canReject || busy} onClick={() => setConfirming('reject')}>Reject</button>
        <button disabled={busy} onClick={() => { setConfirming(null); setNote('Left pending. Nothing was written.'); }}>Leave pending</button>
        <button disabled={busy} onClick={() => send('reanalyze')}>Reanalyze</button>
        {proposal.kind === 'patch' && proposal.state === 'APPROVED' && (
          <button className="primary" disabled={busy} onClick={() => setConfirming('approve_and_apply')}>Apply the approved fix</button>
        )}
        {['APPLIED', 'VALIDATION_FAILED', 'BLOCKED'].includes(proposal.state) && (
          <button disabled={busy} onClick={() => send('revert')}>Revert applied patch</button>
        )}
      </div>
      <p className="muted">Opening this suggestion does not approve it. Apply changes only this proposal and only the diff above.</p>

      {confirming === 'approve_and_apply' && (
        <div className="confirm" role="dialog" aria-label="Confirm apply">
          <h3>Apply this fix?</h3>
          <p>This writes the suggested code into {proposal.files.map((file) => file.path).join(', ') || 'the listed file'}. It does not commit or push. Other files stay as they are.</p>
          <div className="action-bar">
            <button className="primary" disabled={busy} onClick={() => send(proposal.state === 'APPROVED' ? 'apply' : 'approve_and_apply')}>{busy ? 'Applying…' : 'Confirm apply'}</button>
            <button disabled={busy} onClick={() => setConfirming(null)}>Cancel</button>
          </div>
        </div>
      )}
      {confirming === 'reject' && (
        <div className="confirm" role="dialog" aria-label="Confirm reject">
          <h3>Reject this suggestion?</h3>
          <p>The test file stays unchanged. The same suggestion will not be offered again unless you reanalyze.</p>
          <div className="action-bar">
            <button className="danger" disabled={busy} onClick={() => send('reject')}>{busy ? 'Saving…' : 'Confirm reject'}</button>
            <button disabled={busy} onClick={() => setConfirming(null)}>Cancel</button>
          </div>
        </div>
      )}

      {proposal.kind === 'patch' && ['APPLIED', 'BLOCKED', 'VALIDATION_FAILED', 'VALIDATED'].includes(proposal.state) && (
        <section className="validate">
          <h3>Run the affected test</h3>
          <div className="form">
            <label>Environment
              <select value={environment} onChange={(event) => setEnvironment(event.target.value)}>
                <option value="dev">Dev</option>
                <option value="dev2">Dev2</option>
                <option value="test">Test</option>
              </select>
            </label>
            <label>Repeats
              <input type="number" min={1} max={5} value={repetitions} onChange={(event) => setRepetitions(Number(event.target.value))} />
            </label>
            <label className="check"><input type="checkbox" checked={confirmRun} onChange={(event) => setConfirmRun(event.target.checked)} /> I want to start this run</label>
            <button disabled={!confirmRun || busy} onClick={validate}>Run validation</button>
          </div>
          {proposal.validations?.map((run) => (
            <div className="run" key={run.id}>
              <p><span className={`pill ${statusTone(run.status)}`}>{run.status}</span> exit {run.exit_code ?? 'unavailable'} · passed {run.passed ?? 'unavailable'} · failed {run.failed ?? 'unavailable'} · flaky {run.flaky ?? 'unavailable'}</p>
              <p>{run.repetitions} run(s). Observed failures: {run.observed_failures ?? 'unavailable'}. Supports the fix: {run.supports_fix === 1 ? 'yes, for this sample' : 'no'}.</p>
              <p className="muted">{run.disclaimer}</p>
              <p className="mono">{run.command}</p>
            </div>
          ))}
        </section>
      )}
    </article>
  );
}
