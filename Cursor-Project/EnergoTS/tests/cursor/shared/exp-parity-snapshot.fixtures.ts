/**
 * Shared parity-snapshot helper for PreProd vs Experiment comparison runs.
 *
 * Background
 * ----------
 * Phoenix standard billing was rewritten on the Experiment environment
 * (`billing_run.standard_billing_preparation` + `standard_billing_stage_01..22`,
 * asynchronous, several stages commit independently). PreProd still runs the
 * previous synchronous engine (`billing_run.generate_run_volume`, one plpgsql
 * session). Both engines must produce the same business outcome.
 *
 * The QA approach is: run the SAME spec twice, changing only `BASE_URL`
 * (`playwright.config.ts` reads `baseURL` from `process.env.BASE_URL`), then
 * diff the two attachments file-to-file. Specs therefore must never branch on
 * environment or hostname — the difference is expected to show up in the data,
 * not in the test code.
 *
 * Intended diff workflow
 * ----------------------
 * 1. `BASE_URL=<preprod> npx playwright test <spec>` → save the
 *    `[<KEY>] parity snapshot — <caseId>` attachment as `preprod.json`.
 * 2. `BASE_URL=<experiment> npx playwright test <spec>` → save the same
 *    attachment as `experiment.json`.
 * 3. `diff preprod.json experiment.json` — an empty diff means the rewritten
 *    engine is behaviourally equivalent for that case. Any hunk is a parity
 *    finding worth investigating.
 *
 * Normalization contract
 * ----------------------
 * A raw API response can never be diffed directly: ids, invoice numbers,
 * generated names and timestamps differ on every run and in every environment.
 * {@link buildParitySnapshot} therefore:
 *
 * - DROPS keys whose last camelCase/snake_case token is volatile
 *   (`id`, `identifier`, `number`, `name`, `date`, `timestamp`, ...). Dropped
 *   key paths are listed under `excludedPaths` so the exclusion itself is
 *   visible and stays identical between the two runs.
 * - REPLACES ISO date and date-time string values (`yyyy-mm-dd`, with or
 *   without a time component) with a placeholder (a run-day dependent value
 *   would otherwise create false diffs). Month-boundary effects, DST shifts
 *   and VAT rounding differences are out of scope for this diff — callers
 *   should assert those separately when they matter.
 * - KEEPS the facts a parity check is actually about: counts, monetary
 *   amounts, unit prices, statuses, application-model types, error/warning
 *   messages and boolean outcomes.
 * - SORTS object keys and array elements, and rounds numbers, so that
 *   serialization is deterministic regardless of API ordering.
 *
 * Callers must pass LOGICAL ROLE LABELS (`contractA`, `contractB`, `productB`,
 * ...) instead of raw entity ids — see {@link parityRole} — so both
 * environments produce identical keys for the same business object.
 */

import { test } from '../../../fixtures/baseFixture';

/** Free-form, caller-built set of comparable facts for one parity case. */
export type ParityFacts = Record<string, unknown>;

export type ParitySnapshotOptions = {
  /** Stable spec key (Jira key or pseudo-key), used in the attachment name. */
  key: string;
  /** Stable case id inside the spec, e.g. `case-01-pulling-shared-pod-handover`. */
  caseId: string;
  /** One-line description of what the case proves. */
  description?: string;
  /** Comparable facts. Volatile keys are removed automatically. */
  facts: ParityFacts;
  /**
   * Keys (bare key name or full dotted path) that look volatile but must be
   * kept, e.g. a status field literally called `invoiceDate` that the case is
   * about. Use sparingly — every kept volatile key risks a false diff.
   */
  allowKeys?: string[];
};

export type ParitySnapshot = {
  parityKey: string;
  parityCase: string;
  description: string;
  /** Sorted key paths removed by the volatility filter. Same in both runs. */
  excludedPaths: string[];
  facts: Record<string, unknown>;
};

/** Role label returned when an id is not present in the caller's role map. */
export const PARITY_UNMAPPED_ROLE = 'unmapped';

/** Placeholder written instead of an ISO date-time value. */
export const PARITY_TIMESTAMP_PLACEHOLDER = '<timestamp-excluded>';

/**
 * Last name token of a key that makes the value run- or environment-specific.
 * Matching is done on the final token only, so `settlementPeriodCount` and
 * `errorMessage` survive while `invoiceNumber` and `contractId` are dropped.
 */
const VOLATILE_LAST_TOKENS = new Set([
  'id',
  'ids',
  'uuid',
  'uuids',
  'identifier',
  'identifiers',
  'number',
  'numbers',
  'name',
  'names',
  'date',
  'dates',
  'datetime',
  'timestamp',
  'timestamps',
  'createdat',
  'updatedat',
]);

const ISO_DATE_TIME = /^\d{4}-\d{2}-\d{2}(?:[T ]\d{2}:\d{2})?/;

/** Number of console lines printed per snapshot before truncating. */
const CONSOLE_LINE_LIMIT = 40;

function lastToken(key: string): string {
  const tokens = key
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_\-.]+/g, ' ')
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  return (tokens[tokens.length - 1] ?? key).toLowerCase();
}

function roundNumber(value: number): number {
  return Math.round(value * 1e6) / 1e6;
}

function stableStringify(value: unknown): string {
  try {
    return JSON.stringify(value) ?? 'null';
  } catch {
    return String(value);
  }
}

function sortNormalizedArray(items: unknown[]): unknown[] {
  return [...items].sort((a, b) => {
    const left = stableStringify(a);
    const right = stableStringify(b);
    return left < right ? -1 : left > right ? 1 : 0;
  });
}

type NormalizeContext = {
  excluded: string[];
  allow: Set<string>;
};

function normalizeValue(value: unknown, path: string, ctx: NormalizeContext): unknown {
  if (value === null || value === undefined) {
    return null;
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? roundNumber(value) : null;
  }
  if (typeof value === 'boolean') {
    return value;
  }
  if (typeof value === 'string') {
    return ISO_DATE_TIME.test(value) ? PARITY_TIMESTAMP_PLACEHOLDER : value;
  }
  if (Array.isArray(value)) {
    const items = value.map((item) => normalizeValue(item, `${path}[]`, ctx));
    return sortNormalizedArray(items);
  }
  if (typeof value === 'object') {
    const source = value as Record<string, unknown>;
    const out: Record<string, unknown> = {};
    for (const key of Object.keys(source).sort()) {
      const childPath = path ? `${path}.${key}` : key;
      const allowed = ctx.allow.has(key) || ctx.allow.has(childPath);
      if (!allowed && VOLATILE_LAST_TOKENS.has(lastToken(key))) {
        ctx.excluded.push(childPath);
        continue;
      }
      out[key] = normalizeValue(source[key], childPath, ctx);
    }
    return out;
  }
  return String(value);
}

/**
 * Build the normalized, deterministically ordered snapshot without attaching
 * it. Useful when the caller wants to assert on normalized facts or feed them
 * into `finalizeTestRunSummary({ snapshot })`.
 */
export function buildParitySnapshot(options: ParitySnapshotOptions): ParitySnapshot {
  const ctx: NormalizeContext = {
    excluded: [],
    allow: new Set(options.allowKeys ?? []),
  };
  const facts = normalizeValue(options.facts, '', ctx) as Record<string, unknown>;
  return {
    parityKey: options.key,
    parityCase: options.caseId,
    description: options.description ?? '',
    excludedPaths: [...ctx.excluded].sort(),
    facts,
  };
}

function flattenForConsole(
  value: unknown,
  path: string,
  out: string[],
): void {
  if (out.length >= CONSOLE_LINE_LIMIT) {
    return;
  }
  if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
    for (const [key, child] of Object.entries(value as Record<string, unknown>)) {
      flattenForConsole(child, path ? `${path}.${key}` : key, out);
    }
    return;
  }
  out.push(`${path} = ${stableStringify(value)}`);
}

/**
 * Build the normalized snapshot, attach it to the Playwright report as JSON
 * and print a compact console summary. Returns the snapshot so the caller can
 * also pass it to `finalizeTestRunSummary`.
 */
export function attachParitySnapshot(options: ParitySnapshotOptions): ParitySnapshot {
  const snapshot = buildParitySnapshot(options);

  test.info().attach(`[${snapshot.parityKey}] parity snapshot — ${snapshot.parityCase}`, {
    body: JSON.stringify(snapshot, null, 2),
    contentType: 'application/json',
  });

  const lines: string[] = [];
  flattenForConsole(snapshot.facts, '', lines);
  const truncated = lines.length >= CONSOLE_LINE_LIMIT;
  console.log(
    [
      `[${snapshot.parityKey}] parity snapshot — ${snapshot.parityCase}`,
      snapshot.description ? `  ${snapshot.description}` : '',
      ...lines.map((line) => `  ${line}`),
      truncated ? '  … (truncated — see the JSON attachment for the full snapshot)' : '',
      `  excludedPaths: ${snapshot.excludedPaths.length}`,
    ]
      .filter(Boolean)
      .join('\n'),
  );

  return snapshot;
}

/**
 * Round a monetary / numeric API value (string or number) for the snapshot.
 * Returns `null` when the value cannot be parsed, so a missing amount and a
 * zero amount stay distinguishable in the diff.
 */
export function parityAmount(raw: unknown, decimals = 2): number | null {
  if (typeof raw === 'number') {
    return Number.isFinite(raw) ? Number(raw.toFixed(decimals)) : null;
  }
  if (typeof raw !== 'string') {
    return null;
  }
  const cleaned = raw.replace(/\s/g, '').replace(',', '.').replace(/[^0-9.\-]/g, '');
  if (!cleaned || cleaned === '-' || cleaned === '.') {
    return null;
  }
  const parsed = Number(cleaned);
  return Number.isFinite(parsed) ? Number(parsed.toFixed(decimals)) : null;
}

/**
 * Translate a raw entity id into the caller's logical role label so both
 * environments produce identical snapshot keys and values.
 *
 * @example parityRole({ contractA: 1234, contractB: 1235 }, invoice.productContract.id)
 */
export function parityRole(
  roleById: Record<string, number | string | null | undefined>,
  id: unknown,
  fallback: string = PARITY_UNMAPPED_ROLE,
): string {
  if (id === null || id === undefined) {
    return fallback;
  }
  const wanted = String(id);
  for (const [role, candidate] of Object.entries(roleById)) {
    if (candidate !== null && candidate !== undefined && String(candidate) === wanted) {
      return role;
    }
  }
  return fallback;
}
