/**
 * Per-test run summary: relevant payloads + expected/actual checks.
 * Attached by finalizeTestRunSummary in manual-verification-links.fixtures.ts.
 */

export type TestRunCheck = {
  check: string;
  expectedResult: string;
  actualResult: string;
  passed: boolean;
};

export type TestRunSummary = {
  payloads: Record<string, unknown>;
  checks: TestRunCheck[];
  registerPayload: (key: string, payload: unknown) => void;
  recordCheck: (check: TestRunCheck) => void;
};

export function createTestRunSummary(): TestRunSummary {
  const payloads: Record<string, unknown> = {};
  const checks: TestRunCheck[] = [];
  return {
    payloads,
    checks,
    registerPayload(key: string, payload: unknown) {
      payloads[key] = payload;
    },
    recordCheck(check: TestRunCheck) {
      checks.push(check);
    },
  };
}
