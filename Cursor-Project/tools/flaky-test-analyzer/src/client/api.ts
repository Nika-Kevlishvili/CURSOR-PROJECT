export class ApiError extends Error {
  constructor(message: string, public status: number, public code?: string) {
    super(message);
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (init.method && init.method !== 'GET') headers.set('X-Flaky-Analyzer', '1');
  const response = await fetch(path, { ...init, headers });
  const text = await response.text();
  const payload = text ? JSON.parse(text) as { error?: string; code?: string } : {};
  if (!response.ok) throw new ApiError(payload.error ?? response.statusText, response.status, payload.code);
  return payload as T;
}

export type DashboardResponse = {
  counts: {
    importedReports: number;
    testsAnalyzed: number;
    failedTests: number;
    suspectedFlaky: number;
    confirmedIntermittent: number;
    environmentSpecific: number;
    probableDefects: number;
    insufficientEvidence: number;
    fixesAwaitingApproval: number;
    approvedAwaitingValidation: number;
    regressedAfterFix: number;
    diagnosticRecommendations: number;
  };
  failureFrequencyLabel: string;
  retryRecoveryLabel: string;
};

export type ResultRow = {
  id: string;
  testId: string;
  fullTitle: string;
  title: string;
  filePath: string | null;
  projectName: string | null;
  line: number | null;
  environment: string;
  label: string | null;
  executedAt: string | null;
  status: string;
  durationMs: number | null;
  errorSummary: string | null;
  signature: string | null;
  classification: string;
  confidence: string;
  primaryCause: string | null;
  fixStatus: string;
  fixKind: string | null;
  proposalId: string | null;
  analysisStatus: string;
  regressed: boolean;
  sourceRef: string | null;
  expectedStatus: string | null;
};

export type Proposal = {
  id: string;
  kind: string;
  state: string;
  version: number;
  summary: string;
  rationale: string;
  impact: string;
  risks: string;
  confidence: string;
  validationPlan: string;
  why: string;
  diff: string;
  appliedDiff: string | null;
  files: { path: string; original: string; proposed: string }[];
  rootCause: { classification?: string; cause?: string; signature?: string };
  reportIds: string[];
  tests: { test_id: string; full_title: string; file_path: string | null }[];
  createdAt: string;
  approvedAt: string | null;
  appliedAt: string | null;
  rejectedAt: string | null;
  aiNote: string | null;
  blockReason: string | null;
  approvalRequired: string;
  validations?: ValidationRow[];
};

export type ValidationRow = {
  id: string;
  command: string;
  environment: string;
  started_at: string;
  ended_at: string | null;
  exit_code: number | null;
  passed: number | null;
  failed: number | null;
  skipped: number | null;
  interrupted: number | null;
  flaky: number | null;
  repetitions: number;
  observed_failures: number | null;
  report_path: string | null;
  supports_fix: number;
  status: string;
  log_excerpt: string | null;
  disclaimer: string;
  selected_tests: string;
};
