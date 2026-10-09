/**
 * Unified console + Playwright report block for cursor specs:
 * test title, relevant payloads, expected vs actual, filtered portal links.
 */

import { test } from '../../../fixtures/baseFixture';
import type { baseFixture } from '../../../fixtures/baseFixture';
import { configuredBaseURL } from '../../../fixtures/utils/baseUrl';
import type { ResponsesContainer } from '../../../fixtures/types/responses';
import { ResponseLinker } from '../../../utils/reporters/ResponseLinker';
import {
  DEFAULT_ENTITY_KEY_ORDER,
  mergePortalLinks,
  resolveFrontendBaseUrlOverride,
} from './manual-verification-links.fixtures';

/** ResponseLinker maps Dev API → this legacy IP UI; rewrite to the real Dev portal. */
const LEGACY_DEV_PORTAL_PREFIX = 'http://10.236.20.11:8080';
/** Default Dev UI root when FRONTEND_BASE_URL is unset (no trailing slash). */
const DEFAULT_DEV_PORTAL_REWRITE_BASE = 'https://devapps.energo-pro.bg/app/phoenix1-dev';

function stripTrailingSlashes(url: string): string {
  return url.replace(/\/+$/, '');
}

/** Collapse accidental `//` in the path (keep `https://` scheme intact). */
function normalizePortalPathSlashes(url: string): string {
  const match = /^(https?:\/\/)(.*)$/i.exec(url);
  if (!match) {
    return url;
  }
  return `${match[1]}${match[2].replace(/\/{2,}/g, '/')}`;
}

/**
 * Rewrite ResponseLinker Dev portal URLs:
 * - Prefer FRONTEND_BASE_URL as the new frontend root when set
 * - Else replace legacy `http://10.236.20.11:8080` (with or without extra `/`) with phoenix1-dev
 * - Leaves Test/other portals unchanged (e.g. testapps …/phoenix-epres)
 * Preserves path + query after the old base; avoids double slashes.
 */
function rewritePortalUrl(url: string, frontendRoot: string): string {
  const legacy = LEGACY_DEV_PORTAL_PREFIX;
  if (!url.startsWith(legacy)) {
    return url;
  }
  // Remainder may be `/customers/...`, `//customers/...`, or empty
  const remainder = url.slice(legacy.length).replace(/^\/+/, '');
  const root = stripTrailingSlashes(frontendRoot);
  const rewritten = remainder ? `${root}/${remainder}` : root;
  return normalizePortalPathSlashes(rewritten);
}

function resolvePortalRewriteRoot(): string {
  const override = resolveFrontendBaseUrlOverride();
  if (override) {
    return stripTrailingSlashes(override);
  }
  return DEFAULT_DEV_PORTAL_REWRITE_BASE;
}

/**
 * Public helper for console / Playwright attach: rewrite Dev legacy IP portal links
 * to FRONTEND_BASE_URL or https://devapps.energo-pro.bg/app/phoenix1-dev.
 * Does not rewrite Test (phoenix-epres) or other non-legacy URLs.
 */
export function rewritePortalLinksForDisplay(
  links: Record<string, string[]>,
): Record<string, string[]> {
  const root = resolvePortalRewriteRoot();
  const out: Record<string, string[]> = {};
  for (const [key, urls] of Object.entries(links)) {
    out[key] = urls.map((u) => rewritePortalUrl(u, root));
  }
  return out;
}

export type VerificationCheck = {
  /** Short name of the scenario being verified. */
  check: string;
  /** What should happen — expected result of this verification. */
  expectedResult: string;
  /** What happened — actual result observed (pass or fail narrative). */
  actualResult: string;
  passed: boolean;
};

/** @deprecated Prefer recordCheck with narrative expected/actual strings. */
export type ExpectedActualOutcome = VerificationCheck;

export type FinalizeTestRunSummaryOptions = {
  jiraKey: string;
  relevantEntityKeys?: string[];
  payloads?: Record<string, unknown>;
  outcomes?: VerificationCheck[];
  snapshot?: Record<string, unknown>;
  extraLinks?: Record<string, string[]>;
  entityKeyOrder?: string[];
};

export class TestRunSummaryCollector {
  readonly testTitle: string;
  private readonly payloads = new Map<string, unknown>();
  private readonly checks: VerificationCheck[] = [];
  private finalized = false;

  constructor(testTitle: string) {
    this.testTitle = testTitle;
  }

  registerPayload(entityKey: string, payload: unknown): void {
    this.payloads.set(entityKey, payload);
  }

  /**
   * Record a verification in plain language:
   * what case was run, what was expected, what was observed.
   */
  recordCheck(input: VerificationCheck): void {
    this.checks.push(input);
  }

  /** Legacy — maps raw values to expectedResult / actualResult narrative. */
  recordOutcome(step: string, expected: unknown, actual: unknown, passed: boolean): void {
    this.recordCheck({
      check: step,
      expectedResult:
        typeof expected === 'string'
          ? expected
          : `The outcome should match: ${formatValue(expected)}`,
      actualResult: passed
        ? `As expected — observed: ${formatValue(actual)}`
        : `Not as expected — wanted ${formatValue(expected)} but observed ${formatValue(actual)}`,
      passed,
    });
  }

  getPayloads(): Record<string, unknown> {
    return Object.fromEntries(this.payloads);
  }

  getOutcomes(): VerificationCheck[] {
    return [...this.checks];
  }

  isFinalized(): boolean {
    return this.finalized;
  }

  markFinalized(): void {
    this.finalized = true;
  }
}

function formatValue(value: unknown): string {
  if (typeof value === 'string') {
    return value;
  }
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

function filterPortalLinks(
  all: Record<string, string[]>,
  relevantKeys?: string[],
): Record<string, string[]> {
  if (!relevantKeys?.length) {
    return all;
  }
  const out: Record<string, string[]> = {};
  for (const key of relevantKeys) {
    const urls = all[key];
    if (urls?.length) {
      out[key] = urls;
    }
  }
  return out;
}

function formatOutcomesSection(checks: VerificationCheck[]): string[] {
  const lines = ['--- Expected result vs Actual result ---'];
  if (!checks.length) {
    lines.push('(none recorded — use TestRunSummary.recordCheck during the test)');
    return lines;
  }
  checks.forEach((row, index) => {
    const verdict = row.passed ? 'PASSED' : 'FAILED';
    lines.push(
      '',
      `${index + 1}. [${verdict}] ${row.check}`,
      `   Expected result: ${row.expectedResult}`,
      `   Actual result:   ${row.actualResult}`,
    );
  });
  return lines;
}

function formatPayloadsSection(payloads: Record<string, unknown>): string[] {
  const lines = ['--- Relevant payloads ---'];
  const keys = Object.keys(payloads);
  if (!keys.length) {
    lines.push('(none — use TestRunSummary.registerPayload for entities that drive this test)');
    return lines;
  }
  for (const key of keys) {
    lines.push('', `[${key}]`, formatValue(payloads[key]));
  }
  return lines;
}

function formatPortalSection(
  portalLinks: Record<string, string[]>,
  entityKeyOrder: string[],
): string[] {
  const lines = ['--- Portal links (relevant entities) ---'];
  if (!Object.keys(portalLinks).length) {
    lines.push(
      'not available',
      '(set FRONTEND_BASE_URL or run against a mapped API base)',
    );
    return lines;
  }
  const ordered = new Set(entityKeyOrder);
  for (const key of entityKeyOrder) {
    const urls = portalLinks[key];
    if (!urls?.length) {
      continue;
    }
    lines.push('', `[${key}]`);
    for (const url of urls) {
      lines.push(url);
    }
  }
  for (const [key, urls] of Object.entries(portalLinks)) {
    if (ordered.has(key) || !urls.length) {
      continue;
    }
    lines.push('', `[${key}]`);
    for (const url of urls) {
      lines.push(url);
    }
  }
  return lines;
}

export function formatTestRunSummaryBlock(input: {
  jiraKey: string;
  testTitle: string;
  apiBase: string;
  payloads: Record<string, unknown>;
  outcomes: VerificationCheck[];
  portalLinks: Record<string, string[]>;
  snapshot?: Record<string, unknown>;
  entityKeyOrder: string[];
}): string {
  const lines: string[] = [
    '================================================================================',
    `TEST: ${input.testTitle}`,
    `JIRA: ${input.jiraKey}`,
    `API:  ${input.apiBase}`,
    '================================================================================',
    '',
    ...formatPayloadsSection(input.payloads),
    '',
    ...formatOutcomesSection(input.outcomes),
    '',
    ...formatPortalSection(input.portalLinks, input.entityKeyOrder),
  ];

  if (input.snapshot && Object.keys(input.snapshot).length) {
    lines.push('', '--- Context snapshot ---', formatValue(input.snapshot));
  }

  lines.push('', '================================================================================');
  return lines.join('\n');
}

export function finalizeTestRunSummary(
  collector: TestRunSummaryCollector,
  Responses: ResponsesContainer,
  options: FinalizeTestRunSummaryOptions,
): Record<string, string[]> {
  const {
    jiraKey,
    relevantEntityKeys,
    payloads: optionPayloads,
    outcomes: optionOutcomes,
    snapshot,
    extraLinks,
    entityKeyOrder,
  } = options;

  const testTitle = collector.testTitle || test.info().title;
  const apiBase = configuredBaseURL || process.env.BASE_URL || 'http://10.236.20.11:8091/';

  const fromLinker = ResponseLinker.setLinksToResponses(Responses);
  const allLinks = rewritePortalLinksForDisplay(mergePortalLinks(fromLinker, extraLinks));
  const portalLinks = filterPortalLinks(allLinks, relevantEntityKeys);

  const payloads = {
    ...collector.getPayloads(),
    ...optionPayloads,
  };
  const outcomes = [...collector.getOutcomes(), ...(optionOutcomes ?? [])];
  const order = entityKeyOrder ?? DEFAULT_ENTITY_KEY_ORDER;

  const plain = formatTestRunSummaryBlock({
    jiraKey,
    testTitle,
    apiBase,
    payloads,
    outcomes,
    portalLinks,
    snapshot,
    entityKeyOrder: order,
  });

  console.log(`\n${plain}\n`);

  test.info().attach(`[${jiraKey}] test run summary`, {
    body: plain,
    contentType: 'text/plain; charset=utf-8',
  });
  test.info().attach(`[${jiraKey}] test run summary (JSON)`, {
    body: JSON.stringify(
      {
        testTitle,
        jiraKey,
        apiBase,
        payloads,
        outcomes,
        portalLinks,
        snapshot: snapshot ?? {},
        allResponseLinks: allLinks,
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });

  collector.markFinalized();
  return portalLinks;
}
