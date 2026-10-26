import type { Classification, Confidence, RootCause } from '../../shared/types.js';

export type CauseAssessment = {
  categories: RootCause[];
  confidence: Confidence;
  evidence: string[];
  missing: string[];
  contradicts: string[];
  nextAction: string;
};

function includesAny(text: string, patterns: RegExp[]): boolean {
  return patterns.some((pattern) => pattern.test(text));
}

export function looksLikeProductDefect(text: string): boolean {
  const assertion = /expect\(|toBe|toEqual|toHave|CheckResponse|expected[: ].*received|status code \d{3}|\bassert(?:ion)?\b/i.test(text);
  const timeout = /timeout|timed out|locator\.|waiting for/i.test(text);
  return assertion && !timeout;
}

export function assessCause(input: {
  classification: Classification;
  errorText: string;
  sourceText: string | null;
  sourcePath: string | null;
  environmentsFailed: string[];
  environmentsPassed: string[];
}): CauseAssessment {
  const text = `${input.errorText}\n${input.sourceText ?? ''}`;
  const evidence: string[] = [];
  const missing: string[] = [];
  const contradicts: string[] = [];
  const categories: RootCause[] = [];

  if (!input.errorText.trim()) {
    missing.push('The report did not include an error message or stack trace.');
  }
  if (!input.sourcePath) {
    missing.push('The report did not include a test file path, so the failure is not mapped to source.');
  }
  if (input.classification === 'insufficient_evidence') {
    missing.push('Only one failing execution is stored. Another execution of the same test is needed before calling it flaky or consistent.');
  }
  if (input.environmentsPassed.length === 0 && input.environmentsFailed.length <= 1) {
    missing.push('There is no passing result in another environment. A missing environment is not a pass.');
  }

  if (includesAny(text, [/401\b/, /403\b/, /unauthorized/i, /jwt/i, /token expired/i, /auth endpoint/i])) {
    categories.push('authentication');
    evidence.push('The error text mentions an authentication status, token, or auth endpoint.');
  }
  if (includesAny(text, [/ECONNRESET/i, /ECONNREFUSED/i, /socket hang up/i, /net::ERR_/i, /\b50[234]\b/, /ETIMEDOUT/i])) {
    categories.push('api_or_network');
    evidence.push('The error text matches a transport or HTTP 5xx failure.');
    contradicts.push('A failed request does not by itself mean the test should retry.');
  }
  const brittle = input.sourceText ? /nth-child|xpath=|>>|locator\(['"][^'"]*(?:div|span|css=)/i.test(input.sourceText) : false;
  const locatorTimeout = /timeout|timed out/i.test(input.errorText) && /locator|selector|waiting for|getBy/i.test(input.errorText);
  if (locatorTimeout && brittle) {
    categories.push('unstable_locator');
    evidence.push('The timeout names a locator, and the mapped source uses a structural selector.');
    missing.push('The accessible name of the target is not in the report, so a replacement locator was not invented.');
  } else if (locatorTimeout) {
    categories.push('timing_or_synchronization');
    evidence.push('The error is a locator or wait timeout.');
    contradicts.push('A timeout does not prove the timeout value is too small. It can also be an application regression, wrong locator, or unexpected state.');
  }
  if (looksLikeProductDefect(input.errorText)) {
    categories.push(input.classification === 'probable_defect' || input.classification === 'consistent_failure' ? 'application_defect' : 'incorrect_assertion');
    evidence.push('The error is an assertion or response mismatch rather than a timeout.');
    if (input.classification === 'confirmed_intermittent' || input.classification === 'suspected_flaky') {
      contradicts.push('The assertion mismatch is intermittent, so it is not treated as a confirmed product defect.');
    }
  }
  if (/beforeEach|afterEach|beforeAll|afterAll|fixture/i.test(input.errorText)) {
    categories.push('fixture_or_cleanup');
    evidence.push('The stack or error names a hook or fixture.');
  }
  if (/worker|browser has been closed|target closed|page crashed/i.test(input.errorText)) {
    categories.push('resource_contention');
    evidence.push('The error names a browser, target, or worker crash.');
  }
  if (input.classification === 'environment_specific') {
    categories.push('environment_or_infrastructure');
    evidence.push(`Failures were observed only in ${input.environmentsFailed.join(', ') || 'one environment'}, with passes elsewhere.`);
  }
  if (/test\.describe\.serial|depends on previous|mode:\s*['"]serial['"]/i.test(input.errorText)) {
    categories.push('test_order_dependency');
    evidence.push('The source or error mentions ordered or shared test execution.');
  }
  if (categories.includes('timing_or_synchronization') && (input.classification === 'confirmed_intermittent' || input.classification === 'suspected_flaky')) {
    categories.push('race_condition');
    evidence.push('The timeout is paired with pass/fail variation, which is consistent with a race but does not identify the contended state.');
  }
  if (categories.length === 0) categories.push('unknown');

  let confidence: Confidence = 'low';
  if (categories.includes('unknown')) confidence = 'low';
  else if (input.classification === 'insufficient_evidence') confidence = 'low';
  else if (evidence.length >= 2 && missing.length <= 2) confidence = 'medium';
  else confidence = 'medium';
  if (categories.includes('unknown')) confidence = 'low';
  if (input.classification === 'insufficient_evidence') confidence = 'low';

  const primary = categories[0] ?? 'unknown';
  const nextAction = nextActionFor(primary, input.classification, input.sourcePath);
  return { categories: unique(categories), confidence, evidence, missing, contradicts, nextAction };
}

function unique(values: RootCause[]): RootCause[] {
  return [...new Set(values)];
}

function nextActionFor(cause: RootCause, classification: Classification, sourcePath: string | null): string {
  if (!sourcePath) return 'Open the imported report and locate the test manually. The archive did not include a usable file path.';
  if (classification === 'probable_defect' || cause === 'application_defect') {
    return 'Investigate the application response against the assertion. Do not change the test to accept the failing behavior until the intended behavior is confirmed.';
  }
  if (classification === 'environment_specific') {
    return 'Compare the failing environment with an environment that passed before changing the test.';
  }
  if (cause === 'unstable_locator') return 'Inspect the rendered element and replace the structural selector only when a stable accessible locator is visible in the page or source.';
  if (cause === 'authentication') return 'Check whether the token or session expired during the test. Do not log or store the credential.';
  if (cause === 'api_or_network') return 'Check whether the service returned a transport error only on some attempts. Keep the assertion that defines success.';
  if (cause === 'incorrect_assertion') {
    return 'The assertion failed. Do not change the expected value to match this run. Check the data or response the test created, then compare another execution before calling the test flaky.';
  }
  if (classification === 'insufficient_evidence') return 'Import another execution of the same test from the same environment before proposing a behavior change.';
  if (cause === 'timing_or_synchronization' || cause === 'race_condition') {
    return 'Find the application state or response the test should wait for. Do not increase the global timeout from this evidence alone.';
  }
  return 'Collect another failing trace with the error and the surrounding test code before changing the test.';
}
