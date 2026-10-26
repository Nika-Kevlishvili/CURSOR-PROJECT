import { randomUUID } from 'node:crypto';
import type { AppContext } from '../context.js';
import { HttpError } from '../errors.js';
import { sha256 } from '../analysis/signature.js';
import { applyTransaction, assertCurrentSource, replaceOnce, resultingDiff } from '../patch/applyPatch.js';
import { parseFiles, reanalyzeIssue } from './importService.js';
import { getProposal } from './queryService.js';

export function decideProposal(ctx: AppContext, id: string, decision: string, proposalId: string, note?: string) {
  if (proposalId !== id) {
    throw new HttpError(400, 'The proposal id in the request body must match the proposal being decided. No file was changed.', 'PROPOSAL_MISMATCH');
  }
  if (decision === 'reanalyze') {
    const created = reanalyzeIssue(ctx, id);
    if (!created) throw new HttpError(404, 'Reanalysis did not produce a proposal.', 'NOT_FOUND');
    return getProposal(ctx, created);
  }
  if (decision === 'reject') return rejectProposal(ctx, id, note);
  if (decision === 'approve') return markApproved(ctx, id, note);
  if (decision === 'approve_and_apply') {
    markApproved(ctx, id, note);
    return applyApproved(ctx, id);
  }
  if (decision === 'revert') return revertApplied(ctx, id, note);
  throw new HttpError(400, 'Decision must be approve, approve_and_apply, reject, revert, or reanalyze.', 'BAD_DECISION');
}

export function applyApproved(ctx: AppContext, id: string) {
  const row = requireProposal(ctx, id);
  if (row.kind !== 'patch') {
    throw new HttpError(409, 'This item is a diagnostic recommendation and has no source patch to apply.', 'NOT_A_PATCH');
  }
  if (row.state !== 'APPROVED') {
    throw new HttpError(409, `Proposal is ${row.state}. Apply is allowed only after an explicit approval of this diff.`, 'NOT_APPROVED');
  }
  const files = parseFiles(row.files_json);
  if (files.length === 0) throw new HttpError(409, 'The proposal does not contain a file change.', 'NOT_A_PATCH');
  let prepared: { absolute: string; relative: string; before: string; next: string }[];
  try {
    prepared = files.map((file) => {
      const current = assertCurrentSource(ctx.config.automationRoot, file);
      return {
        absolute: current.absolute,
        relative: file.path,
        before: current.content,
        next: replaceOnce(current.content, file.oldText, file.newText),
      };
    });
  } catch (error) {
    if (error instanceof HttpError && (error.code === 'STALE_PATCH' || error.code === 'AMBIGUOUS_PATCH' || error.code === 'GIT_STATUS_FAILED')) {
      ctx.db.run(`UPDATE proposals SET state = 'STALE', block_reason = ? WHERE id = ?`, error.message, id);
    }
    throw error;
  }
  try {
    applyTransaction(prepared.map((entry) => ({ absolute: entry.absolute, next: entry.next })));
  } catch (error) {
    throw new HttpError(500, error instanceof Error ? error.message : 'The patch could not be written and the original files were restored.', 'APPLY_FAILED');
  }
  const appliedAt = new Date().toISOString();
  const diffs = prepared.map((entry) => resultingDiff(entry.relative, entry.before, entry.next));
  try {
    ctx.db.transaction(() => {
      for (const entry of prepared) {
        ctx.db.run(
          `INSERT INTO file_backups (proposal_id, path, content, base_hash, applied_hash)
           VALUES (?, ?, ?, ?, ?)
           ON CONFLICT(proposal_id, path) DO UPDATE SET content = excluded.content, base_hash = excluded.base_hash, applied_hash = excluded.applied_hash`,
          id,
          entry.relative,
          entry.before,
          sha256(entry.before),
          sha256(entry.next),
        );
      }
      ctx.db.run(`UPDATE proposals SET state = 'APPLIED', applied_at = ?, applied_diff = ? WHERE id = ?`, appliedAt, diffs.join('\n'), id);
      ctx.db.run(
        `INSERT INTO approvals (id, proposal_id, action, note, created_at) VALUES (?, ?, 'apply', NULL, ?)`,
        `apr_${randomUUID()}`,
        id,
        appliedAt,
      );
    });
  } catch (error) {
    applyTransaction(prepared.map((entry) => ({ absolute: entry.absolute, next: entry.before })));
    throw error;
  }
  return getProposal(ctx, id);
}

function markApproved(ctx: AppContext, id: string, note?: string) {
  const row = requireProposal(ctx, id);
  if (row.kind !== 'patch') throw new HttpError(409, 'Diagnostic recommendations are not approved as source changes.', 'NOT_A_PATCH');
  if (row.state !== 'PROPOSED') throw new HttpError(409, `Proposal is ${row.state} and cannot be approved again.`, 'BAD_STATE');
  const now = new Date().toISOString();
  ctx.db.transaction(() => {
    ctx.db.run(`UPDATE proposals SET state = 'APPROVED', approved_at = ? WHERE id = ? AND state = 'PROPOSED'`, now, id);
    ctx.db.run(
      `INSERT INTO approvals (id, proposal_id, action, note, created_at) VALUES (?, ?, 'approve', ?, ?)`,
      `apr_${randomUUID()}`,
      id,
      note ?? null,
      now,
    );
  });
  return getProposal(ctx, id);
}

function rejectProposal(ctx: AppContext, id: string, note?: string) {
  const row = requireProposal(ctx, id);
  if (!['PROPOSED', 'APPROVED'].includes(row.state)) {
    throw new HttpError(409, `Proposal is ${row.state}. Reject applies before a patch is written.`, 'BAD_STATE');
  }
  const now = new Date().toISOString();
  ctx.db.transaction(() => {
    ctx.db.run(`UPDATE proposals SET state = 'REJECTED', rejected_at = ? WHERE id = ?`, now, id);
    ctx.db.run(
      `INSERT INTO approvals (id, proposal_id, action, note, created_at) VALUES (?, ?, 'reject', ?, ?)`,
      `apr_${randomUUID()}`,
      id,
      note ?? null,
      now,
    );
  });
  return getProposal(ctx, id);
}

function revertApplied(ctx: AppContext, id: string, note?: string) {
  const row = requireProposal(ctx, id);
  if (!['APPLIED', 'VALIDATION_FAILED', 'BLOCKED'].includes(row.state)) {
    throw new HttpError(409, 'Only an applied patch can be reverted, and only by this explicit action.', 'BAD_STATE');
  }
  const backups = ctx.db.all<{ path: string; content: string; applied_hash: string | null }>(
    `SELECT path, content, applied_hash FROM file_backups WHERE proposal_id = ?`,
    id,
  );
  if (backups.length === 0) throw new HttpError(409, 'No stored original content is available for this proposal.', 'NO_BACKUP');
  const prepared = backups.map((backup) => {
    const current = assertCurrentSource(ctx.config.automationRoot, {
      path: backup.path,
      baseHash: backup.applied_hash ?? '',
      oldText: backup.content,
      newText: 'revert-check',
    });
    if (backup.applied_hash && sha256(current.content) !== backup.applied_hash) {
      throw new HttpError(409, `${backup.path} changed after the patch was applied. It was not reverted.`, 'STALE_PATCH');
    }
    return { absolute: current.absolute, relative: backup.path, before: current.content, next: backup.content };
  });
  applyTransaction(prepared.map((entry) => ({ absolute: entry.absolute, next: entry.next })));
  const now = new Date().toISOString();
  ctx.db.transaction(() => {
    ctx.db.run(`UPDATE proposals SET state = 'REJECTED', rejected_at = ?, block_reason = ? WHERE id = ?`, now, note ?? 'Reverted by explicit action.', id);
    ctx.db.run(
      `INSERT INTO approvals (id, proposal_id, action, note, created_at) VALUES (?, ?, 'revert', ?, ?)`,
      `apr_${randomUUID()}`,
      id,
      note ?? null,
      now,
    );
  });
  return getProposal(ctx, id);
}

function requireProposal(ctx: AppContext, id: string) {
  const row = ctx.db.get<{ id: string; kind: string; state: string; files_json: string }>(
    `SELECT id, kind, state, files_json FROM proposals WHERE id = ?`,
    id,
  );
  if (!row) throw new HttpError(404, 'Proposal was not found.', 'NOT_FOUND');
  return row;
}
