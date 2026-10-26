import { describe, expect, it } from 'vitest';
import { classifyHistory, type Observation } from '../analysis/classify.js';
import { normalizeErrorSignature, testIdentity } from '../analysis/signature.js';
import { failureFrequency, retryRecovery } from '../analysis/metrics.js';
import { assessCause } from '../analysis/rootCause.js';
import { assertPatchPolicy } from '../patch/policy.js';
import { buildProposalDraft } from '../analysis/propose.js';
import { sha256 } from '../analysis/signature.js';

const fail = (environment: string, reportId: string): Observation => ({
  environment,
  reportId,
  attempts: [{ retryIndex: 0, status: 'failed' }],
});
const pass = (environment: string, reportId: string): Observation => ({
  environment,
  reportId,
  attempts: [{ retryIndex: 0, status: 'passed' }],
});

describe('failure analysis', () => {
  it('does not call a single failure confirmed flaky', () => {
    const result = classifyHistory([fail('test', 'r1')], true);
    expect(result.classification).toBe('insufficient_evidence');
    expect(result.classification).not.toBe('confirmed_intermittent');
    expect(result.classification).not.toBe('suspected_flaky');
  });

  it('treats a failed attempt followed by a pass as intermittent behavior', () => {
    const result = classifyHistory([{
      environment: 'dev',
      reportId: 'r1',
      attempts: [
        { retryIndex: 0, status: 'timedOut' },
        { retryIndex: 1, status: 'passed' },
      ],
    }], false);
    expect(result.classification).toBe('confirmed_intermittent');
  });

  it('keeps two disagreeing runs as suspected rather than confirmed', () => {
    const result = classifyHistory([pass('dev', 'r1'), fail('dev', 'r2')], false);
    expect(result.classification).toBe('suspected_flaky');
  });

  it('recognizes an environment-specific failure without treating a missing environment as a pass', () => {
    const onlyTest = classifyHistory([fail('test', 'r1'), fail('test', 'r2')], false);
    expect(onlyTest.classification).toBe('consistent_failure');
    const compared = classifyHistory([
      pass('dev', 'r1'),
      fail('test', 'r2'),
      fail('test', 'r3'),
    ], false);
    expect(compared.classification).toBe('environment_specific');
  });

  it('calls repeated assertion failures a probable defect', () => {
    const result = classifyHistory([fail('dev', 'r1'), fail('dev2', 'r2')], true);
    expect(result.classification).toBe('probable_defect');
  });

  it('keeps meaningful assertion values in the signature and drops timestamps', () => {
    const left = normalizeErrorSignature('failed at 2026-03-01T00:00:00.000Z id 1234567890123 expected 200');
    const right = normalizeErrorSignature('failed at 2026-04-02T01:02:03.000Z id 9999999999999 expected 200');
    const other = normalizeErrorSignature('failed at 2026-03-01T00:00:00.000Z id 1234567890123 expected 404');
    expect(left).toBe(right);
    expect(left).not.toBe(other);
  });

  it('matches tests across environments without using the line number', () => {
    const dev = testIdentity({ filePath: 'tests/customer.spec.ts', projectName: 'main', fullTitle: 'Customer > creates', repeatEachIndex: null });
    const testEnv = testIdentity({ filePath: 'tests/customer.spec.ts', projectName: 'main', fullTitle: 'Customer > creates', repeatEachIndex: null });
    const other = testIdentity({ filePath: 'tests/customer.spec.ts', projectName: 'setup', fullTitle: 'Customer > creates', repeatEachIndex: null });
    expect(dev).toBe(testEnv);
    expect(dev).not.toBe(other);
  });

  it('shows sample size and ignores skipped attempts in the failure rate', () => {
    const ratio = failureFrequency([
      { retryIndex: 0, status: 'failed' },
      { retryIndex: 1, status: 'passed' },
      { retryIndex: 0, status: 'skipped' },
    ]);
    expect(ratio).toEqual({ numerator: 1, denominator: 2, rate: 0.5 });
    const recovery = retryRecovery([{
      attempts: [
        { retryIndex: 0, status: 'failed' },
        { retryIndex: 1, status: 'passed' },
      ],
    }]);
    expect(recovery).toEqual({ numerator: 1, denominator: 1, rate: 1 });
  });

  it('does not treat a timeout name as proof the timeout is too small', () => {
    const cause = assessCause({
      classification: 'insufficient_evidence',
      errorText: 'Timeout 30000ms exceeded waiting for locator',
      sourceText: null,
      sourcePath: 'tests/customer.spec.ts',
      environmentsFailed: ['test'],
      environmentsPassed: [],
    });
    expect(cause.confidence).toBe('low');
    expect(cause.contradicts.join(' ')).toMatch(/does not prove the timeout value is too small/i);
    expect(cause.categories).not.toContain('application_defect');
  });

  it('refuses patches that weaken or skip a test', () => {
    expect(() => assertPatchPolicy('expect(value).toBe(1);', 'value;')).toThrow(/assertion/i);
    expect(() => assertPatchPolicy('test("a", () => {});', 'test.skip("a", () => {});')).toThrow(/skip/i);
    expect(() => assertPatchPolicy('retries: 0', 'retries: 2')).toThrow(/retries/i);
  });

  it('builds an await patch without being asked to write a file', () => {
    const source = [
      "import { test, expect } from '@playwright/test';",
      '',
      "test('saves the form', async ({ page }) => {",
      "  expect(page.getByRole('button', { name: 'Save' })).toBeVisible();",
      '});',
    ].join('\n');
    const draft = buildProposalDraft({
      relativePath: 'tests/customer.spec.ts',
      sourceText: source,
      line: 4,
      baseHash: sha256(source),
      errorText: 'Timeout 5000ms exceeded',
      classification: 'insufficient_evidence',
      categories: ['timing_or_synchronization'],
      missing: [],
      nextAction: 'Review the line.',
    });
    expect(draft.kind).toBe('patch');
    expect(draft.diff).toContain('await expect');
    expect(source).not.toContain('await expect');
  });

  it('suggests awaiting a Playwright action on the failing line', () => {
    const source = [
      "import { test, expect } from '@playwright/test';",
      '',
      "test('opens the dialog', async ({ page }) => {",
      "  page.getByRole('button', { name: 'Save' }).click();",
      '});',
    ].join('\n');
    const draft = buildProposalDraft({
      relativePath: 'tests/customer.spec.ts',
      sourceText: source,
      line: 4,
      baseHash: sha256(source),
      errorText: 'Timeout 5000ms exceeded waiting for getByRole',
      classification: 'insufficient_evidence',
      categories: ['timing_or_synchronization'],
      missing: [],
      nextAction: 'Review the line.',
    });
    expect(draft.kind).toBe('patch');
    expect(draft.files[0]?.newText).toContain('await page.getByRole');
    expect(source).toContain(".click();");
    expect(source).not.toContain('await page.getByRole');
  });
});
