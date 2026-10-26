export type AttemptStatus = 'passed' | 'failed' | 'timedOut' | 'skipped' | 'interrupted' | 'unknown';

export type NormalizedAttachment = {
  name: string;
  contentType: string | null;
  path: string | null;
  text: string | null;
};

export type NormalizedAttempt = {
  retryIndex: number | null;
  status: AttemptStatus;
  durationMs: number | null;
  startTime: string | null;
  workerIndex: number | null;
  errorMessage: string | null;
  errorStack: string | null;
  attachments: NormalizedAttachment[];
};

export type NormalizedResult = {
  title: string;
  fullTitle: string;
  suitePath: string[];
  filePath: string | null;
  line: number | null;
  column: number | null;
  projectName: string | null;
  tags: string[];
  annotations: { type: string; description: string | null }[];
  expectedStatus: string | null;
  outcome: string | null;
  repeatEachIndex: number | null;
  attempts: NormalizedAttempt[];
  sourceRef: string;
};

export type ParsedReport = {
  parser: 'html-embedded' | 'json-reporter' | 'html-fallback';
  playwrightVersion: string | null;
  startedAt: string | null;
  durationMs: number | null;
  warnings: string[];
  tests: NormalizedResult[];
  sourceReportPath: string;
};

export function asRecord(value: unknown): Record<string, unknown> | null {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

export function asString(value: unknown): string | null {
  return typeof value === 'string' && value.length > 0 ? value : null;
}

export function asNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

export function asArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : [];
}

export function attemptStatus(value: unknown): AttemptStatus {
  if (value === 'passed' || value === 'failed' || value === 'timedOut' || value === 'skipped' || value === 'interrupted') {
    return value;
  }
  return 'unknown';
}

export function posixPath(value: string | null): string | null {
  if (!value) return null;
  const normalized = value.replace(/\\/g, '/').replace(/^\.\//, '');
  return normalized.length > 0 ? normalized : null;
}

export function fullTitle(suitePath: string[], title: string): string {
  return [...suitePath, title].filter((part) => part.trim().length > 0).join(' > ');
}

export function isFileSuiteTitle(title: string, filePath: string | null): boolean {
  if (!filePath) return /\.(spec|test)\.[cm]?[jt]sx?$/.test(title);
  const base = filePath.split('/').pop();
  return title === filePath || title === base;
}
