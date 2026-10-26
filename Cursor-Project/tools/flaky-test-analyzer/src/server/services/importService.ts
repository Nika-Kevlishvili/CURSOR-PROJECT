import fs from 'node:fs';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import type { EnvironmentName } from '../../shared/types.js';
import { ENVIRONMENTS } from '../../shared/types.js';
import type { AppContext } from '../context.js';
import { HttpError } from '../errors.js';
import { clip, redact } from '../redact.js';
import { assertProcessingTime, resolveInside, unzipSafely } from '../zip/safeZip.js';
import { parseArchive } from '../parse/parseArchive.js';
import type { AttemptStatus, NormalizedAttempt, NormalizedResult } from '../parse/model.js';
import { extractEndpoint, extractSelector, normalizeErrorSignature, sha256, testIdentity } from '../analysis/signature.js';
import { classifyHistory, executionVerdict, type Observation } from '../analysis/classify.js';
import { failureFrequency, retryRecovery } from '../analysis/metrics.js';
import { assessCause, looksLikeProductDefect } from '../analysis/rootCause.js';
import { isPatchablePath, lineForFailure, lineFromStack, readSource, resolveExistingSource, safeExcerpt, toRepoRelative } from '../analysis/sourceContext.js';
import { buildProposalDraft, type PatchFile } from '../analysis/propose.js';

export type ImportInput = {
  bytes: Uint8Array;
  filename: string;
  environment: string;
  label: string | null;
  executedAt: string | null;
};

function assertEnvironment(value: string): EnvironmentName {
  if (!(ENVIRONMENTS as readonly string[]).includes(value)) {
    throw new HttpError(400, 'Environment must be dev, dev2, or test.', 'BAD_ENVIRONMENT');
  }
  return value as EnvironmentName;
}

function cleanLabel(label: string | null): string | null {
  if (!label) return null;
  const cleaned = label.replace(/[\u0000-\u001f]/g, '').trim();
  return cleaned ? cleaned.slice(0, 200) : null;
}

function parseExecutedAt(value: string | null, fallback: string | null): string | null {
  if (!value) return fallback;
  const time = Date.parse(value);
  if (Number.isNaN(time)) throw new HttpError(400, 'Execution timestamp is not a valid date.', 'BAD_TIMESTAMP');
  return new Date(time).toISOString();
}

export function deriveStatus(attempts: NormalizedAttempt[], outcome: string | null): string {
  const verdict = executionVerdict(attempts.map((attempt) => ({ retryIndex: attempt.retryIndex, status: attempt.status })));
  if (verdict === 'intermittent' || outcome === 'flaky') return 'flaky';
  if (verdict === 'pass') return 'passed';
  if (verdict === 'unknown') {
    if (attempts.length > 0 && attempts.every((attempt) => attempt.status === 'skipped')) return 'skipped';
    return outcome === 'skipped' ? 'skipped' : 'unknown';
  }
  const failing = attempts.find((attempt) => attempt.status === 'timedOut' || attempt.status === 'interrupted' || attempt.status === 'failed');
  if (failing?.status === 'timedOut') return 'timedOut';
  if (failing?.status === 'interrupted') return 'interrupted';
  return 'failed';
}

function firstError(attempts: NormalizedAttempt[]): { message: string; stack: string } {
  const failed = attempts.find((attempt) => attempt.errorMessage || attempt.errorStack);
  return {
    message: failed?.errorMessage ?? '',
    stack: failed?.errorStack ?? '',
  };
}

function persistKeptFiles(ctx: AppContext, reportId: string, files: { path: string; bytes: Uint8Array }[], sourcePath: string): string {
  const directory = path.join(ctx.config.storageDir, reportId);
  fs.mkdirSync(directory, { recursive: true });
  for (const file of files) {
    const keep = file.path === sourcePath || file.path.includes('/data/') || file.path.startsWith('data/');
    if (!keep) continue;
    const destination = resolveInside(directory, file.path);
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    fs.writeFileSync(destination, file.bytes);
  }
  return directory;
}

export function applyRetention(ctx: AppContext, now = Date.now()): number {
  const cutoff = new Date(now - ctx.config.retentionDays * 24 * 60 * 60 * 1000).toISOString();
  const rows = ctx.db.all<{ id: string; storage_dir: string | null }>(
    `SELECT id, storage_dir FROM reports WHERE imported_at < ? AND attachments_available = 1`,
    cutoff,
  );
  for (const row of rows) {
    if (row.storage_dir && fs.existsSync(row.storage_dir)) fs.rmSync(row.storage_dir, { recursive: true, force: true });
    ctx.db.run(`UPDATE reports SET attachments_available = 0, storage_dir = NULL WHERE id = ?`, row.id);
  }
  return rows.length;
}

export async function importReport(ctx: AppContext, input: ImportInput) {
  const started = Date.now();
  const environment = assertEnvironment(input.environment);
  const label = cleanLabel(input.label);
  if (input.bytes.byteLength === 0) throw new HttpError(400, 'The uploaded archive is empty.', 'EMPTY_ARCHIVE');
  const contentHash = sha256(input.bytes);
  const existing = ctx.db.get<{ id: string }>(`SELECT id FROM reports WHERE content_hash = ?`, contentHash);
  if (existing) {
    const report = reportSummary(ctx, existing.id);
    return { duplicate: true, report };
  }

  const files = unzipSafely(input.bytes, ctx.config.limits, started);
  assertProcessingTime(started, ctx.config.limits);
  const parsed = parseArchive(files, ctx.config.limits, started);
  const reportId = `rpt_${randomUUID()}`;
  const executedAt = parseExecutedAt(input.executedAt, parsed.startedAt);
  const storageDir = persistKeptFiles(ctx, reportId, files, parsed.sourceReportPath);
  const importedAt = new Date().toISOString();
  const failedCount = parsed.tests.filter((test) => {
    const status = deriveStatus(test.attempts, test.outcome);
    return status === 'failed' || status === 'timedOut' || status === 'interrupted';
  }).length;

  const testIds = ctx.db.transaction(() => {
    ctx.db.run(
      `INSERT INTO reports (
        id, content_hash, environment, label, executed_at, imported_at, source_filename, parser,
        playwright_version, archive_bytes, test_count, failed_count, storage_dir, attachments_available,
        warnings_json, report_path
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)`,
      reportId,
      contentHash,
      environment,
      label,
      executedAt,
      importedAt,
      sanitizeFilename(input.filename),
      parsed.parser,
      parsed.playwrightVersion,
      input.bytes.byteLength,
      parsed.tests.length,
      failedCount,
      storageDir,
      JSON.stringify(parsed.warnings),
      parsed.sourceReportPath,
    );
    const ids = new Set<string>();
    for (const test of parsed.tests) ids.add(insertResult(ctx, reportId, test));
    return [...ids];
  });

  refreshTests(ctx, testIds);
  await annotateWithAdvisor(ctx, testIds);
  return { duplicate: false, report: reportSummary(ctx, reportId) };
}

function sanitizeFilename(filename: string): string {
  const base = filename.replace(/\\/g, '/').split('/').pop() ?? 'report.zip';
  return base.replace(/[^\w.\- ()[\]]+/g, '_').slice(0, 180) || 'report.zip';
}

function insertResult(ctx: AppContext, reportId: string, test: NormalizedResult): string {
  const errors = firstError(test.attempts);
  const relative = resolveExistingSource(ctx.config.automationRoot, test.filePath, errors.stack);
  const raw = redact(errors.message) ?? '';
  const signature = raw ? normalizeErrorSignature(raw) : `no-error:${test.fullTitle}`;
  const testId = testIdentity({
    filePath: relative ?? test.filePath,
    projectName: test.projectName,
    fullTitle: test.fullTitle,
    repeatEachIndex: test.repeatEachIndex,
  });
  const now = new Date().toISOString();
  ctx.db.run(
    `INSERT INTO test_cases (
      id, file_path, full_title, title, project_name, tags_json, classification, confidence,
      primary_cause, causes_json, secondary_json, regressed, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, 'insufficient_evidence', 'low', NULL, '[]', '[]', 0, ?)
    ON CONFLICT(id) DO UPDATE SET
      file_path = excluded.file_path,
      full_title = excluded.full_title,
      title = excluded.title,
      project_name = excluded.project_name,
      tags_json = excluded.tags_json,
      updated_at = excluded.updated_at`,
    testId,
    relative ?? test.filePath,
    test.fullTitle,
    test.title,
    test.projectName,
    JSON.stringify(test.tags),
    now,
  );
  const status = deriveStatus(test.attempts, test.outcome);
  const resultId = `res_${randomUUID()}`;
  const combined = `${raw}\n${errors.stack ?? ''}`;
  const regressed = isRegression(ctx, testId, status) ? 1 : 0;
  if (regressed) ctx.db.run(`UPDATE test_cases SET regressed = 1 WHERE id = ?`, testId);
  ctx.db.run(
    `INSERT INTO results (
      id, report_id, test_id, line, column_no, outcome, expected_status, status, duration_ms,
      error_summary, raw_error, signature, selector, endpoint, source_ref, regressed
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    resultId,
    reportId,
    testId,
    test.line,
    test.column,
    test.outcome,
    test.expectedStatus,
    status,
    durationOf(test.attempts),
    clip(raw || null, 500),
    clip(raw || null, 8000),
    signature,
    extractSelector(combined),
    extractEndpoint(combined),
    test.sourceRef,
    regressed,
  );
  for (const attempt of test.attempts) insertAttempt(ctx, resultId, attempt);
  return testId;
}

function isRegression(ctx: AppContext, testId: string, status: string): boolean {
  if (!['failed', 'timedOut', 'interrupted', 'flaky'].includes(status)) return false;
  const validated = ctx.db.get<{ id: string }>(
    `SELECT p.id FROM proposals p
     JOIN proposal_tests pt ON pt.proposal_id = p.id
     WHERE pt.test_id = ? AND p.state = 'VALIDATED' LIMIT 1`,
    testId,
  );
  return Boolean(validated);
}

function durationOf(attempts: NormalizedAttempt[]): number | null {
  const values = attempts.map((attempt) => attempt.durationMs).filter((value): value is number => value !== null);
  if (values.length === 0) return null;
  return values.reduce((sum, value) => sum + value, 0);
}

function insertAttempt(ctx: AppContext, resultId: string, attempt: NormalizedAttempt): void {
  ctx.db.run(
    `INSERT INTO attempts (
      id, result_id, retry_index, status, duration_ms, start_time, worker_index, error_message, error_stack, attachments_json
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    `att_${randomUUID()}`,
    resultId,
    attempt.retryIndex,
    attempt.status,
    attempt.durationMs,
    attempt.startTime,
    attempt.workerIndex,
    clip(attempt.errorMessage, 8000),
    clip(attempt.errorStack, 8000),
    JSON.stringify(attempt.attachments),
  );
}

type ResultJoin = {
  id: string;
  report_id: string;
  test_id: string;
  line: number | null;
  status: string;
  raw_error: string | null;
  signature: string | null;
  environment: string;
  file_path: string | null;
};

export function refreshTests(ctx: AppContext, testIds: string[], createProposals = true): void {
  staleProposals(ctx);
  for (const testId of testIds) refreshOne(ctx, testId);
  const issues = new Set<string>();
  for (const testId of testIds) {
    const issueId = ensureIssue(ctx, testId);
    if (issueId) issues.add(issueId);
  }
  if (createProposals) {
    for (const issueId of issues) ensureProposal(ctx, issueId, false);
  }
}

function refreshOne(ctx: AppContext, testId: string): void {
  const rows = ctx.db.all<ResultJoin>(
    `SELECT res.id, res.report_id, res.test_id, res.line, res.status, res.raw_error, res.signature, r.environment, tc.file_path
     FROM results res
     JOIN reports r ON r.id = res.report_id
     JOIN test_cases tc ON tc.id = res.test_id
     WHERE res.test_id = ?
     ORDER BY res.rowid`,
    testId,
  );
  const observations: Observation[] = rows.map((row) => ({
    environment: row.environment,
    reportId: row.report_id,
    attempts: ctx.db.all<{ retry_index: number | null; status: AttemptStatus }>(
      `SELECT retry_index, status FROM attempts WHERE result_id = ? ORDER BY COALESCE(retry_index, 0)`,
      row.id,
    ).map((attempt) => ({ retryIndex: attempt.retry_index, status: attempt.status })),
  }));
  const errorText = rows.map((row) => row.raw_error ?? '').filter(Boolean).join('\n');
  const stack = ctx.db.get<{ error_stack: string | null }>(
    `SELECT error_stack FROM attempts WHERE result_id = ? AND error_stack IS NOT NULL LIMIT 1`,
    rows[0]?.id ?? '',
  )?.error_stack ?? '';
  const classified = classifyHistory(observations, looksLikeProductDefect(`${errorText}\n${stack}`));
  const filePath = rows.find((row) => row.file_path)?.file_path ?? null;
  const resolved = resolveExistingSource(ctx.config.automationRoot, filePath, stack) ?? toRepoRelative(ctx.config.automationRoot, filePath);
  if (resolved && resolved !== filePath) {
    ctx.db.run(`UPDATE test_cases SET file_path = ? WHERE id = ?`, resolved, testId);
  }
  const source = readSource(ctx.config.automationRoot, resolved);
  const focus = [...rows].reverse().find((row) => row.line && row.status !== 'passed' && row.status !== 'skipped') ?? rows.at(-1);
  const reportedLine = focus?.line ?? lineFromStack(`${errorText}\n${stack}`, resolved);
  const line = lineForFailure(source.text, reportedLine, `${errorText}\n${stack}`);
  if (line) {
    for (const row of rows) ctx.db.run(`UPDATE results SET line = ? WHERE id = ?`, line, row.id);
  }
  const failedEnvs = [...new Set(observations.filter((item) => executionVerdict(item.attempts) !== 'pass' && executionVerdict(item.attempts) !== 'unknown').map((item) => item.environment))];
  const passedEnvs = [...new Set(observations.filter((item) => executionVerdict(item.attempts) === 'pass').map((item) => item.environment))];
  const cause = assessCause({
    classification: classified.classification,
    errorText: `${errorText}\n${stack ?? ''}`,
    sourceText: source.text,
    sourcePath: source.relativePath,
    environmentsFailed: failedEnvs,
    environmentsPassed: passedEnvs,
  });
  const attempts = observations.flatMap((item) => item.attempts);
  const now = new Date().toISOString();
  ctx.db.run(
    `UPDATE test_cases SET classification = ?, confidence = ?, primary_cause = ?, causes_json = ?, secondary_json = ?, updated_at = ? WHERE id = ?`,
    classified.classification,
    cause.confidence,
    cause.categories[0] ?? 'unknown',
    JSON.stringify(cause.categories),
    JSON.stringify(classified.secondary),
    now,
    testId,
  );
  ctx.db.run(
    `INSERT INTO assessments (
      test_id, classification, confidence, categories_json, evidence_json, missing_json, contradicts_json,
      source_files_json, next_action, excerpt, frequency_json, recovery_json, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(test_id) DO UPDATE SET
      classification = excluded.classification,
      confidence = excluded.confidence,
      categories_json = excluded.categories_json,
      evidence_json = excluded.evidence_json,
      missing_json = excluded.missing_json,
      contradicts_json = excluded.contradicts_json,
      source_files_json = excluded.source_files_json,
      next_action = excluded.next_action,
      excerpt = excluded.excerpt,
      frequency_json = excluded.frequency_json,
      recovery_json = excluded.recovery_json,
      updated_at = excluded.updated_at`,
    testId,
    classified.classification,
    cause.confidence,
    JSON.stringify(cause.categories),
    JSON.stringify([...classified.notes, ...cause.evidence]),
    JSON.stringify([...cause.missing, ...(source.missingReason ? [source.missingReason] : [])]),
    JSON.stringify(cause.contradicts),
    JSON.stringify(source.relativePath ? [source.relativePath] : []),
    cause.nextAction,
    safeExcerpt(source.text, line),
    JSON.stringify(failureFrequency(attempts)),
    JSON.stringify(retryRecovery(observations)),
    now,
  );
}

function ensureIssue(ctx: AppContext, testId: string): string | null {
  const test = ctx.db.get<{
    classification: string;
    file_path: string | null;
    primary_cause: string | null;
    confidence: string;
    full_title: string;
  }>(`SELECT classification, file_path, primary_cause, confidence, full_title FROM test_cases WHERE id = ?`, testId);
  const statuses = ctx.db.all<{ status: string }>(`SELECT status FROM results WHERE test_id = ?`, testId);
  if (!test || test.classification === 'stable_pass' || (statuses.length > 0 && statuses.every((row) => row.status === 'skipped'))) {
    ctx.db.run(`DELETE FROM issue_tests WHERE test_id = ?`, testId);
    return null;
  }
  const signature = ctx.db.get<{ signature: string | null }>(
    `SELECT signature FROM results WHERE test_id = ? AND signature IS NOT NULL ORDER BY rowid DESC LIMIT 1`,
    testId,
  )?.signature ?? `no-error:${testId}`;
  const issueId = `iss_${sha256(`${signature}\n${test.file_path ?? ''}\n${test.primary_cause ?? 'unknown'}`).slice(0, 20)}`;
  const now = new Date().toISOString();
  ctx.db.run(
    `INSERT INTO issues (id, signature, file_path, primary_cause, classification, summary, confidence, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?)
     ON CONFLICT(id) DO UPDATE SET
       classification = excluded.classification,
       summary = excluded.summary,
       confidence = excluded.confidence,
       updated_at = excluded.updated_at`,
    issueId,
    signature,
    test.file_path,
    test.primary_cause ?? 'unknown',
    test.classification,
    test.full_title,
    test.confidence,
    now,
  );
  ctx.db.run(`DELETE FROM issue_tests WHERE test_id = ?`, testId);
  ctx.db.run(`INSERT INTO issue_tests (issue_id, test_id) VALUES (?, ?)`, issueId, testId);
  return issueId;
}

export function ensureProposal(ctx: AppContext, issueId: string, force: boolean): string | null {
  const issue = ctx.db.get<{
    id: string;
    signature: string;
    file_path: string | null;
    primary_cause: string;
    classification: string;
    summary: string;
    confidence: string;
  }>(`SELECT * FROM issues WHERE id = ?`, issueId);
  if (!issue) return null;
  const tests = ctx.db.all<{ test_id: string; line: number | null; raw_error: string | null }>(
    `SELECT it.test_id, res.line, res.raw_error
     FROM issue_tests it
     LEFT JOIN results res ON res.test_id = it.test_id
     WHERE it.issue_id = ?
     ORDER BY res.rowid DESC`,
    issueId,
  );
  const testIds = [...new Set(tests.map((test) => test.test_id))];
  if (testIds.length === 0) return null;
  const relative = toRepoRelative(ctx.config.automationRoot, issue.file_path);
  const source = readSource(ctx.config.automationRoot, relative);
  const line = tests.find((test) => test.line)?.line ?? null;
  const errorText = tests.map((test) => test.raw_error ?? '').join('\n');
  const assessment = ctx.db.get<{ missing_json: string; categories_json: string; next_action: string }>(
    `SELECT missing_json, categories_json, next_action FROM assessments WHERE test_id = ?`,
    testIds[0],
  );
  const draft = buildProposalDraft({
    relativePath: source.relativePath && isPatchablePath(source.relativePath) ? source.relativePath : source.relativePath,
    sourceText: source.text,
    line,
    baseHash: source.text ? sha256(source.text) : null,
    errorText,
    classification: issue.classification as never,
    categories: parseStringArray(assessment?.categories_json) as never,
    missing: parseStringArray(assessment?.missing_json),
    nextAction: assessment?.next_action ?? 'Review the stored evidence before changing a test.',
  });
  if (draft.kind === 'patch' && draft.files.some((file) => !isPatchablePath(file.path))) {
    draft.kind = 'diagnostic';
    draft.files = [];
    draft.diff = '';
    draft.summary = 'No source patch. The mapped file is outside tests, fixtures, and helpers.';
  }
  const dedupe = sha256(JSON.stringify({
    issueId,
    kind: draft.kind,
    summary: draft.summary,
    files: draft.files.map((file) => ({ path: file.path, oldText: file.oldText, newText: file.newText })),
  }));
  ctx.db.run(
    `UPDATE proposals SET state = 'STALE' WHERE issue_id = ? AND state IN ('PROPOSED', 'APPROVED') AND dedupe_key <> ?`,
    issueId,
    dedupe,
  );
  if (!force) {
    const open = ctx.db.get<{ id: string }>(
      `SELECT id FROM proposals WHERE dedupe_key = ? AND state IN ('PROPOSED','APPROVED','APPLIED','VALIDATED','BLOCKED','VALIDATION_FAILED') ORDER BY created_at DESC LIMIT 1`,
      dedupe,
    );
    if (open) {
      linkProposal(ctx, open.id, testIds);
      return open.id;
    }
    const rejected = ctx.db.get<{ id: string }>(`SELECT id FROM proposals WHERE dedupe_key = ? AND state = 'REJECTED' LIMIT 1`, dedupe);
    if (rejected) return rejected.id;
  }
  const version = ctx.db.get<{ version: number }>(`SELECT COALESCE(MAX(version), 0) + 1 AS version FROM proposals WHERE issue_id = ?`, issueId)?.version ?? 1;
  const proposalId = `fix_${randomUUID()}`;
  const reportIds = ctx.db.all<{ report_id: string }>(
    `SELECT DISTINCT report_id FROM results WHERE test_id IN (${testIds.map(() => '?').join(',') || "''"})`,
    ...testIds,
  ).map((row) => row.report_id);
  ctx.db.run(
    `INSERT INTO proposals (
      id, issue_id, dedupe_key, version, kind, state, summary, rationale, impact, risks, confidence,
      validation_plan, why, files_json, diff_text, root_cause_json, report_ids_json, created_at
    ) VALUES (?, ?, ?, ?, ?, 'PROPOSED', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
    proposalId,
    issueId,
    dedupe,
    version,
    draft.kind,
    draft.summary,
    draft.rationale,
    draft.impact,
    draft.risks,
    draft.confidence,
    draft.validationPlan,
    draft.why,
    JSON.stringify(draft.files satisfies PatchFile[]),
    draft.diff,
    JSON.stringify({ classification: issue.classification, cause: issue.primary_cause, signature: issue.signature }),
    JSON.stringify(reportIds),
    new Date().toISOString(),
  );
  linkProposal(ctx, proposalId, testIds);
  return proposalId;
}

function linkProposal(ctx: AppContext, proposalId: string, testIds: string[]): void {
  for (const testId of testIds) {
    ctx.db.run(`INSERT INTO proposal_tests (proposal_id, test_id) VALUES (?, ?) ON CONFLICT DO NOTHING`, proposalId, testId);
  }
}

function staleProposals(ctx: AppContext): void {
  const rows = ctx.db.all<{ id: string; files_json: string }>(
    `SELECT id, files_json FROM proposals WHERE state IN ('PROPOSED', 'APPROVED') AND kind = 'patch'`,
  );
  for (const row of rows) {
    const files = parseFiles(row.files_json);
    const stale = files.some((file) => {
      const source = readSource(ctx.config.automationRoot, file.path);
      return !source.text || sha256(source.text) !== file.baseHash;
    });
    if (stale) ctx.db.run(`UPDATE proposals SET state = 'STALE' WHERE id = ?`, row.id);
  }
}

export function reanalyzeIssue(ctx: AppContext, proposalId: string): string | null {
  const proposal = ctx.db.get<{ id: string; issue_id: string | null; state: string }>(`SELECT id, issue_id, state FROM proposals WHERE id = ?`, proposalId);
  if (!proposal?.issue_id) throw new HttpError(404, 'Proposal was not found.', 'NOT_FOUND');
  if (proposal.state === 'PROPOSED' || proposal.state === 'APPROVED') {
    ctx.db.run(`UPDATE proposals SET state = 'STALE' WHERE id = ?`, proposal.id);
  }
  const testIds = ctx.db.all<{ test_id: string }>(`SELECT test_id FROM proposal_tests WHERE proposal_id = ?`, proposal.id).map((row) => row.test_id);
  refreshTests(ctx, testIds, false);
  return ensureProposal(ctx, proposal.issue_id, true);
}

async function annotateWithAdvisor(ctx: AppContext, testIds: string[]): Promise<void> {
  if (!ctx.advisor.enabled || testIds.length === 0) return;
  const proposal = ctx.db.get<{ id: string; summary: string }>(
    `SELECT p.id, p.summary FROM proposals p
     JOIN proposal_tests pt ON pt.proposal_id = p.id
     WHERE pt.test_id = ? ORDER BY p.created_at DESC LIMIT 1`,
    testIds[0],
  );
  if (!proposal) return;
  const assessment = ctx.db.get<{ excerpt: string | null }>(`SELECT excerpt FROM assessments WHERE test_id = ?`, testIds[0]);
  const error = ctx.db.get<{ raw_error: string | null }>(`SELECT raw_error FROM results WHERE test_id = ? ORDER BY rowid DESC LIMIT 1`, testIds[0]);
  const note = await ctx.advisor.explain({
    summary: proposal.summary,
    errorText: error?.raw_error ?? '',
    excerpt: assessment?.excerpt ?? null,
  });
  if (note) ctx.db.run(`UPDATE proposals SET ai_note = ? WHERE id = ?`, clip(note, 2000), proposal.id);
}

export function reparseStoredReports(ctx: AppContext): { reports: number; updated: number } {
  const reports = ctx.db.all<{ id: string; storage_dir: string | null; report_path: string | null }>(
    `SELECT id, storage_dir, report_path FROM reports`,
  );
  const touched = new Set<string>();
  let updated = 0;
  for (const report of reports) {
    if (!report.storage_dir || !report.report_path) continue;
    const htmlPath = path.join(report.storage_dir, report.report_path);
    if (!fs.existsSync(htmlPath)) continue;
    const parsed = parseArchive(
      [{ path: report.report_path, bytes: new Uint8Array(fs.readFileSync(htmlPath)) }],
      ctx.config.limits,
      Date.now(),
    );
    for (const test of parsed.tests) {
      const rows = ctx.db.all<{ id: string; test_id: string }>(
        `SELECT res.id, res.test_id
         FROM results res JOIN test_cases tc ON tc.id = res.test_id
         WHERE res.report_id = ? AND tc.full_title = ?`,
        report.id,
        test.fullTitle,
      );
      const errors = firstError(test.attempts);
      const raw = redact(errors.message) ?? '';
      const signature = raw ? normalizeErrorSignature(raw) : `no-error:${test.fullTitle}`;
      const relative = resolveExistingSource(ctx.config.automationRoot, test.filePath, errors.stack);
      for (const row of rows) {
        ctx.db.run(
          `UPDATE results SET raw_error = ?, error_summary = ?, signature = ? WHERE id = ?`,
          clip(raw || null, 8000),
          clip(raw || null, 500),
          signature,
          row.id,
        );
        ctx.db.run(`DELETE FROM attempts WHERE result_id = ?`, row.id);
        for (const attempt of test.attempts) insertAttempt(ctx, row.id, attempt);
        if (relative) ctx.db.run(`UPDATE test_cases SET file_path = ? WHERE id = ?`, relative, row.test_id);
        touched.add(row.test_id);
        updated += 1;
      }
    }
  }
  refreshTests(ctx, [...touched]);
  return { reports: reports.length, updated };
}

export function reportSummary(ctx: AppContext, reportId: string) {
  const row = ctx.db.get<{
    id: string;
    environment: string;
    label: string | null;
    executed_at: string | null;
    imported_at: string;
    parser: string;
    playwright_version: string | null;
    test_count: number;
    failed_count: number;
    warnings_json: string;
    source_filename: string;
    report_path: string | null;
    attachments_available: number;
    content_hash: string;
  }>(`SELECT * FROM reports WHERE id = ?`, reportId);
  if (!row) throw new HttpError(404, 'Report was not found.', 'NOT_FOUND');
  return {
    id: row.id,
    environment: row.environment,
    label: row.label,
    executedAt: row.executed_at,
    importedAt: row.imported_at,
    parser: row.parser,
    playwrightVersion: row.playwright_version,
    testCount: row.test_count,
    failedCount: row.failed_count,
    warnings: parseStringArray(row.warnings_json),
    sourceFilename: row.source_filename,
    reportPath: row.report_path,
    attachmentsAvailable: row.attachments_available === 1,
    contentHash: row.content_hash,
  };
}

export function parseStringArray(value: string | null | undefined): string[] {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value) as unknown;
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [];
  } catch {
    return [];
  }
}

export function parseFiles(value: string): PatchFile[] {
  try {
    const parsed = JSON.parse(value) as PatchFile[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}
