import type { DashboardFilters } from '../../shared/types.js';
import type { AppContext } from '../context.js';
import { HttpError } from '../errors.js';
import { failureFrequency, formatRatio, retryRecovery } from '../analysis/metrics.js';
import type { AttemptStatus } from '../parse/model.js';
import { readSource, safeExcerpt } from '../analysis/sourceContext.js';
import { parseFiles, parseStringArray } from './importService.js';

export type ListFilters = DashboardFilters & {
  q?: string;
  groupBy?: 'signature' | 'file' | 'endpoint' | 'selector' | 'cause';
  limit?: number;
  offset?: number;
  attention?: boolean;
};

function whereFor(filters: ListFilters): { sql: string; params: (string | number)[] } {
  const clauses = ['1 = 1'];
  const params: (string | number)[] = [];
  if (filters.environment) {
    clauses.push('r.environment = ?');
    params.push(filters.environment);
  }
  if (filters.from) {
    clauses.push(`COALESCE(r.executed_at, r.imported_at) >= ?`);
    params.push(filters.from);
  }
  if (filters.to) {
    const to = filters.to.length === 10 ? `${filters.to}T23:59:59.999Z` : filters.to;
    clauses.push(`COALESCE(r.executed_at, r.imported_at) <= ?`);
    params.push(to);
  }
  if (filters.status) {
    clauses.push('res.status = ?');
    params.push(filters.status);
  } else if (filters.attention) {
    clauses.push(`res.status IN ('failed', 'timedOut', 'flaky', 'interrupted')`);
  }
  if (filters.rootCause) {
    clauses.push('tc.primary_cause = ?');
    params.push(filters.rootCause);
  }
  if (filters.fixStatus) {
    clauses.push(`(
      SELECT p.state FROM proposal_tests pt
      JOIN proposals p ON p.id = pt.proposal_id
      WHERE pt.test_id = tc.id
      ORDER BY p.created_at DESC LIMIT 1
    ) = ?`);
    params.push(filters.fixStatus);
  }
  return { sql: clauses.join(' AND '), params };
}

const FROM = `
  FROM results res
  JOIN test_cases tc ON tc.id = res.test_id
  JOIN reports r ON r.id = res.report_id
`;

export function dashboard(ctx: AppContext, filters: DashboardFilters) {
  const { sql, params } = whereFor(filters);
  const count = (extra = '1 = 1') => ctx.db.get<{ c: number }>(`SELECT COUNT(*) AS c ${FROM} WHERE ${sql} AND ${extra}`, ...params)?.c ?? 0;
  const distinct = (expression: string, extra = '1 = 1') =>
    ctx.db.get<{ c: number }>(`SELECT COUNT(DISTINCT ${expression}) AS c ${FROM} WHERE ${sql} AND ${extra}`, ...params)?.c ?? 0;
  const reportWhere = ['1 = 1'];
  const reportParams: string[] = [];
  if (filters.environment) {
    reportWhere.push('environment = ?');
    reportParams.push(filters.environment);
  }
  if (filters.from) {
    reportWhere.push(`COALESCE(executed_at, imported_at) >= ?`);
    reportParams.push(filters.from);
  }
  if (filters.to) {
    const to = filters.to.length === 10 ? `${filters.to}T23:59:59.999Z` : filters.to;
    reportWhere.push(`COALESCE(executed_at, imported_at) <= ?`);
    reportParams.push(to);
  }
  const attempts = ctx.db.all<{ retry_index: number | null; status: AttemptStatus; result_id: string }>(
    `SELECT a.retry_index, a.status, a.result_id
     FROM attempts a
     JOIN results res ON res.id = a.result_id
     JOIN test_cases tc ON tc.id = res.test_id
     JOIN reports r ON r.id = res.report_id
     WHERE ${sql}`,
    ...params,
  );
  const byResult = new Map<string, { retryIndex: number | null; status: AttemptStatus }[]>();
  for (const attempt of attempts) {
    const list = byResult.get(attempt.result_id) ?? [];
    list.push({ retryIndex: attempt.retry_index, status: attempt.status });
    byResult.set(attempt.result_id, list);
  }
  return {
    counts: {
      importedReports: ctx.db.get<{ c: number }>(`SELECT COUNT(*) AS c FROM reports WHERE ${reportWhere.join(' AND ')}`, ...reportParams)?.c ?? 0,
      testsAnalyzed: count(),
      failedTests: count(`res.status IN ('failed', 'timedOut', 'interrupted')`),
      suspectedFlaky: distinct('tc.id', `tc.classification = 'suspected_flaky'`),
      confirmedIntermittent: distinct('tc.id', `tc.classification = 'confirmed_intermittent'`),
      environmentSpecific: distinct('tc.id', `tc.classification = 'environment_specific'`),
      probableDefects: distinct('tc.id', `tc.classification = 'probable_defect'`),
      insufficientEvidence: distinct('tc.id', `tc.classification = 'insufficient_evidence' AND res.status NOT IN ('passed', 'skipped')`),
      fixesAwaitingApproval: ctx.db.get<{ c: number }>(
        `SELECT COUNT(DISTINCT p.id) AS c FROM proposals p
         JOIN proposal_tests pt ON pt.proposal_id = p.id
         JOIN results res ON res.test_id = pt.test_id
         JOIN test_cases tc ON tc.id = res.test_id
         JOIN reports r ON r.id = res.report_id
         WHERE p.state = 'PROPOSED' AND p.kind = 'patch' AND ${sql}`,
        ...params,
      )?.c ?? 0,
      approvedAwaitingValidation: ctx.db.get<{ c: number }>(
        `SELECT COUNT(DISTINCT p.id) AS c FROM proposals p
         JOIN proposal_tests pt ON pt.proposal_id = p.id
         JOIN results res ON res.test_id = pt.test_id
         JOIN test_cases tc ON tc.id = res.test_id
         JOIN reports r ON r.id = res.report_id
         WHERE p.state IN ('APPROVED', 'APPLIED') AND ${sql}`,
        ...params,
      )?.c ?? 0,
      regressedAfterFix: count('res.regressed = 1'),
      diagnosticRecommendations: ctx.db.get<{ c: number }>(
        `SELECT COUNT(DISTINCT p.id) AS c FROM proposals p
         JOIN proposal_tests pt ON pt.proposal_id = p.id
         JOIN results res ON res.test_id = pt.test_id
         JOIN test_cases tc ON tc.id = res.test_id
         JOIN reports r ON r.id = res.report_id
         WHERE p.state = 'PROPOSED' AND p.kind = 'diagnostic' AND ${sql}`,
        ...params,
      )?.c ?? 0,
    },
    failureFrequency: failureFrequency([...byResult.values()].flat()),
    retryRecovery: retryRecovery([...byResult.values()].map((items) => ({ attempts: items }))),
    failureFrequencyLabel: formatRatio(failureFrequency([...byResult.values()].flat())),
    retryRecoveryLabel: formatRatio(retryRecovery([...byResult.values()].map((items) => ({ attempts: items })))),
  };
}

export function listReports(ctx: AppContext) {
  return ctx.db.all<{
    id: string;
    environment: string;
    label: string | null;
    executed_at: string | null;
    imported_at: string;
    parser: string;
    test_count: number;
    failed_count: number;
    source_filename: string;
    attachments_available: number;
    playwright_version: string | null;
  }>(`SELECT id, environment, label, executed_at, imported_at, parser, test_count, failed_count, source_filename, attachments_available, playwright_version
      FROM reports ORDER BY imported_at DESC`).map((row) => ({
    id: row.id,
    environment: row.environment,
    label: row.label,
    executedAt: row.executed_at,
    importedAt: row.imported_at,
    parser: row.parser,
    playwrightVersion: row.playwright_version,
    testCount: row.test_count,
    failedCount: row.failed_count,
    sourceFilename: row.source_filename,
    attachmentsAvailable: row.attachments_available === 1,
    historyNote: row.attachments_available === 1 ? null : 'Attachment files were removed by the retention policy. Result history is still stored and is not a clean run.',
  }));
}

type ResultRow = {
  id: string;
  test_id: string;
  full_title: string;
  title: string;
  file_path: string | null;
  project_name: string | null;
  line: number | null;
  environment: string;
  label: string | null;
  executed_at: string | null;
  status: string;
  outcome: string | null;
  expected_status: string | null;
  duration_ms: number | null;
  error_summary: string | null;
  signature: string | null;
  selector: string | null;
  endpoint: string | null;
  classification: string;
  confidence: string;
  primary_cause: string | null;
  report_id: string;
  source_ref: string | null;
  regressed: number;
  fix_state: string | null;
  fix_kind: string | null;
  proposal_id: string | null;
};

const SELECT_RESULTS = `
  SELECT res.id, res.test_id, tc.full_title, tc.title, tc.file_path, tc.project_name, res.line,
         r.environment, r.label, r.executed_at, res.status, res.outcome, res.expected_status, res.duration_ms,
         res.error_summary, res.signature, res.selector, res.endpoint, tc.classification, tc.confidence,
         tc.primary_cause, res.report_id, res.source_ref, res.regressed,
         (
           SELECT p.state FROM proposal_tests pt JOIN proposals p ON p.id = pt.proposal_id
           WHERE pt.test_id = tc.id ORDER BY p.created_at DESC LIMIT 1
         ) AS fix_state,
         (
           SELECT p.kind FROM proposal_tests pt JOIN proposals p ON p.id = pt.proposal_id
           WHERE pt.test_id = tc.id ORDER BY p.created_at DESC LIMIT 1
         ) AS fix_kind,
         (
           SELECT p.id FROM proposal_tests pt JOIN proposals p ON p.id = pt.proposal_id
           WHERE pt.test_id = tc.id ORDER BY p.created_at DESC LIMIT 1
         ) AS proposal_id
  ${FROM}
`;

function mapResult(row: ResultRow) {
  return {
    id: row.id,
    testId: row.test_id,
    fullTitle: row.full_title,
    title: row.title,
    filePath: row.file_path,
    projectName: row.project_name,
    line: row.line,
    environment: row.environment,
    label: row.label,
    executedAt: row.executed_at,
    status: row.status,
    outcome: row.outcome,
    expectedStatus: row.expected_status,
    durationMs: row.duration_ms,
    errorSummary: row.error_summary,
    signature: row.signature,
    selector: row.selector,
    endpoint: row.endpoint,
    classification: row.classification,
    confidence: row.confidence,
    primaryCause: row.primary_cause,
    reportId: row.report_id,
    sourceRef: row.source_ref,
    regressed: row.regressed === 1,
    fixStatus: row.fix_state ?? 'none',
    fixKind: row.fix_kind,
    proposalId: row.proposal_id,
    analysisStatus: analysisStatus(row),
  };
}

function analysisStatus(row: ResultRow): string {
  if (row.fix_state === 'VALIDATED') return 'validated';
  if (row.fix_state === 'APPLIED') return 'applied';
  if (row.fix_state === 'APPROVED') return 'approved';
  if (row.fix_state === 'PROPOSED' && row.fix_kind === 'patch') return 'proposal_ready';
  if (row.fix_state === 'PROPOSED') return 'diagnostic';
  if (row.fix_state) return row.fix_state.toLowerCase();
  if (row.classification === 'insufficient_evidence') return 'insufficient_evidence';
  return 'analyzed';
}

export function listResults(ctx: AppContext, filters: ListFilters) {
  const { sql, params } = whereFor(filters);
  const searchParams = [...params];
  let searchSql = sql;
  if (filters.q) {
    const term = `%${filters.q.replace(/[\\%_]/g, (char) => `\\${char}`)}%`;
    searchSql += ` AND (tc.full_title LIKE ? ESCAPE '\\' OR IFNULL(tc.file_path, '') LIKE ? ESCAPE '\\' OR IFNULL(res.error_summary, '') LIKE ? ESCAPE '\\')`;
    searchParams.push(term, term, term);
  }
  const limit = Math.min(Math.max(filters.limit ?? 100, 1), 500);
  const offset = Math.max(filters.offset ?? 0, 0);
  const total = ctx.db.get<{ c: number }>(`SELECT COUNT(*) AS c ${FROM} WHERE ${searchSql}`, ...searchParams)?.c ?? 0;
  const rows = ctx.db.all<ResultRow>(`${SELECT_RESULTS} WHERE ${searchSql} ORDER BY r.imported_at DESC LIMIT ? OFFSET ?`, ...searchParams, limit, offset).map(mapResult);
  if (!filters.groupBy) return { mode: 'flat' as const, total, rows };
  const keyOf = (row: ReturnType<typeof mapResult>) => {
    if (filters.groupBy === 'file') return row.filePath ?? '(file unavailable)';
    if (filters.groupBy === 'endpoint') return row.endpoint ?? '(endpoint unavailable)';
    if (filters.groupBy === 'selector') return row.selector ?? '(selector unavailable)';
    if (filters.groupBy === 'cause') return row.primaryCause ?? '(cause unavailable)';
    return row.signature ?? '(signature unavailable)';
  };
  const groups = new Map<string, typeof rows>();
  for (const row of rows) {
    const key = keyOf(row);
    groups.set(key, [...(groups.get(key) ?? []), row]);
  }
  return {
    mode: 'grouped' as const,
    total,
    groups: [...groups.entries()].map(([key, items]) => ({ key, count: items.length, rows: items })),
  };
}

export function resultDetail(ctx: AppContext, resultId: string) {
  const row = ctx.db.get<ResultRow>(`${SELECT_RESULTS} WHERE res.id = ?`, resultId);
  if (!row) throw new HttpError(404, 'Test result was not found.', 'NOT_FOUND');
  const attempts = ctx.db.all<{
    retry_index: number | null;
    status: string;
    duration_ms: number | null;
    start_time: string | null;
    worker_index: number | null;
    error_message: string | null;
    error_stack: string | null;
    attachments_json: string;
  }>(`SELECT retry_index, status, duration_ms, start_time, worker_index, error_message, error_stack, attachments_json FROM attempts WHERE result_id = ? ORDER BY COALESCE(retry_index, 0)`, resultId);
  const assessment = ctx.db.get<{
    classification: string;
    confidence: string;
    categories_json: string;
    evidence_json: string;
    missing_json: string;
    contradicts_json: string;
    source_files_json: string;
    next_action: string;
    excerpt: string | null;
    frequency_json: string;
    recovery_json: string;
  }>(`SELECT * FROM assessments WHERE test_id = ?`, row.test_id);
  const source = readSource(ctx.config.automationRoot, row.file_path);
  const related = row.signature
    ? ctx.db.all<ResultRow>(`${SELECT_RESULTS} WHERE res.signature = ? AND res.id <> ? LIMIT 20`, row.signature, resultId).map(mapResult)
    : [];
  const report = ctx.db.get<{ attachments_available: number }>(`SELECT attachments_available FROM reports WHERE id = ?`, row.report_id);
  return {
    result: mapResult(row),
    attempts: attempts.map((attempt) => ({
      retryIndex: attempt.retry_index,
      status: attempt.status,
      durationMs: attempt.duration_ms,
      startTime: attempt.start_time,
      workerIndex: attempt.worker_index,
      errorMessage: attempt.error_message,
      errorStack: attempt.error_stack,
      attachments: JSON.parse(attempt.attachments_json) as unknown[],
    })),
    assessment: assessment
      ? {
          classification: assessment.classification,
          confidence: assessment.confidence,
          categories: parseStringArray(assessment.categories_json),
          evidence: parseStringArray(assessment.evidence_json),
          missing: parseStringArray(assessment.missing_json),
          contradicts: parseStringArray(assessment.contradicts_json),
          sourceFiles: parseStringArray(assessment.source_files_json),
          nextAction: assessment.next_action,
          storedExcerpt: assessment.excerpt,
          frequency: JSON.parse(assessment.frequency_json) as { numerator: number; denominator: number; rate: number | null },
          recovery: JSON.parse(assessment.recovery_json) as { numerator: number; denominator: number; rate: number | null },
        }
      : null,
    source: {
      path: source.relativePath,
      missingReason: source.missingReason,
      excerpt: safeExcerpt(source.text, row.line),
      available: source.text !== null,
    },
    related,
    attachmentsNote: report?.attachments_available === 1 ? null : 'Attachment files for this report were removed by retention. This is not a clean history.',
  };
}

export function compareTests(ctx: AppContext, testId?: string) {
  const reports = ctx.db.all<{ id: string; environment: string; label: string | null; executed_at: string | null; imported_at: string }>(
    `SELECT id, environment, label, executed_at, imported_at FROM reports ORDER BY COALESCE(executed_at, imported_at)`,
  );
  const tests = testId
    ? ctx.db.all<{ id: string; full_title: string; file_path: string | null; project_name: string | null; classification: string }>(
        `SELECT id, full_title, file_path, project_name, classification FROM test_cases WHERE id = ?`,
        testId,
      )
    : ctx.db.all<{ id: string; full_title: string; file_path: string | null; project_name: string | null; classification: string }>(
        `SELECT id, full_title, file_path, project_name, classification FROM test_cases
         WHERE classification <> 'stable_pass' ORDER BY updated_at DESC LIMIT 200`,
      );
  if (testId && tests.length === 0) throw new HttpError(404, 'Test was not found.', 'NOT_FOUND');
  const ids = tests.map((test) => test.id);
  const results = ids.length === 0
    ? []
    : ctx.db.all<{ test_id: string; report_id: string; status: string }>(
        `SELECT test_id, report_id, status FROM results WHERE test_id IN (${ids.map(() => '?').join(',')})`,
        ...ids,
      );
  return {
    reports: reports.map((report) => ({
      id: report.id,
      environment: report.environment,
      label: report.label,
      executedAt: report.executed_at,
      importedAt: report.imported_at,
    })),
    tests: tests.map((test) => {
      const cells = reports.map((report) => {
        const match = results.find((result) => result.test_id === test.id && result.report_id === report.id);
        return {
          reportId: report.id,
          environment: report.environment,
          label: report.label,
          executedAt: report.executed_at,
          status: match ? match.status : 'missing',
        };
      });
      const observed = cells.filter((cell) => cell.status !== 'missing').map((cell) => cell.status);
      return {
        testId: test.id,
        fullTitle: test.full_title,
        filePath: test.file_path,
        projectName: test.project_name,
        classification: test.classification,
        behavior: observed.length === 0 ? 'no executions' : observed.join(' → '),
        cells,
      };
    }),
  };
}

export function listProposals(ctx: AppContext, state?: string) {
  const rows = state
    ? ctx.db.all<ProposalRow>(`SELECT * FROM proposals WHERE state = ? ORDER BY created_at DESC`, state)
    : ctx.db.all<ProposalRow>(`SELECT * FROM proposals ORDER BY created_at DESC`);
  return rows.map((row) => proposalSummary(ctx, row));
}

type ProposalRow = {
  id: string;
  issue_id: string | null;
  version: number;
  kind: string;
  state: string;
  summary: string;
  rationale: string;
  impact: string;
  risks: string;
  confidence: string;
  validation_plan: string;
  why: string;
  files_json: string;
  diff_text: string;
  root_cause_json: string;
  report_ids_json: string;
  created_at: string;
  approved_at: string | null;
  applied_at: string | null;
  rejected_at: string | null;
  applied_diff: string | null;
  ai_note: string | null;
  block_reason: string | null;
};

function proposalSummary(ctx: AppContext, row: ProposalRow) {
  const tests = ctx.db.all<{ test_id: string; full_title: string; file_path: string | null }>(
    `SELECT tc.id AS test_id, tc.full_title, tc.file_path
     FROM proposal_tests pt JOIN test_cases tc ON tc.id = pt.test_id
     WHERE pt.proposal_id = ?`,
    row.id,
  );
  const files = parseFiles(row.files_json);
  return {
    id: row.id,
    issueId: row.issue_id,
    version: row.version,
    kind: row.kind,
    state: row.state,
    summary: row.summary,
    rationale: row.rationale,
    impact: row.impact,
    risks: row.risks,
    confidence: row.confidence,
    validationPlan: row.validation_plan,
    why: row.why,
    diff: row.diff_text,
    appliedDiff: row.applied_diff,
    files: files.map((file) => ({ path: file.path, original: file.oldText, proposed: file.newText })),
    rootCause: JSON.parse(row.root_cause_json) as unknown,
    reportIds: parseStringArray(row.report_ids_json),
    tests,
    createdAt: row.created_at,
    approvedAt: row.approved_at,
    appliedAt: row.applied_at,
    rejectedAt: row.rejected_at,
    aiNote: row.ai_note,
    blockReason: row.block_reason,
    approvalRequired: 'Choose Approve and apply on this proposal. Opening it does not approve it.',
  };
}

export function getProposal(ctx: AppContext, id: string) {
  const row = ctx.db.get<ProposalRow>(`SELECT * FROM proposals WHERE id = ?`, id);
  if (!row) throw new HttpError(404, 'Proposal was not found.', 'NOT_FOUND');
  const validations = ctx.db.all(`SELECT * FROM validations WHERE proposal_id = ? ORDER BY started_at DESC`, id);
  return { ...proposalSummary(ctx, row), validations };
}
