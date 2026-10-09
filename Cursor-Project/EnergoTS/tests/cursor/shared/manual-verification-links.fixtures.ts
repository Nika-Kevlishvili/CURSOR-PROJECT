/**
 * Shared portal links + entity snapshot for manual tester verification.
 * Use as the last step in every new tests/cursor/*.spec.ts test().
 *
 * Links: ResponseLinker (Responses buckets) + optional extraLinks.
 * Frontend base: FRONTEND_BASE_URL env, else inferred from generated link URLs.
 *
 * SAVE_OBJECT_LINKS=true — optional extra console log in ResponseLinker only.
 */

import { test } from '../../../fixtures/baseFixture';
import type { baseFixture } from '../../../fixtures/baseFixture';
import type { ResponsesContainer } from '../../../fixtures/types/responses';
import {
  TestRunSummaryCollector,
  finalizeTestRunSummary,
  type FinalizeTestRunSummaryOptions,
  type TestRunSummaryCollector as TestRunSummaryCollectorType,
} from './test-run-summary.fixtures';

export type ManualVerificationLinksOptions = FinalizeTestRunSummaryOptions & {
  testRunSummary?: TestRunSummaryCollectorType;
};

/**
 * Log + attach unified test run summary (portal links, payloads, expected/actual).
 */
export function attachManualVerificationLinks(
  Responses: baseFixture['Responses'],
  options: ManualVerificationLinksOptions,
): Record<string, string[]> {
  const { testRunSummary, payloads, outcomes, ...rest } = options;
  const collector = testRunSummary ?? new TestRunSummaryCollector(test.info().title);
  return finalizeTestRunSummary(collector, Responses, {
    ...rest,
    payloads: { ...collector.getPayloads(), ...payloads },
    outcomes: [...collector.getOutcomes(), ...(outcomes ?? [])],
  });
}

export type { VerificationCheck, FinalizeTestRunSummaryOptions } from './test-run-summary.fixtures';
export {
  TestRunSummaryCollector,
  finalizeTestRunSummary,
  type ExpectedActualOutcome,
} from './test-run-summary.fixtures';
const SNAPSHOT_LABEL_FIELDS = [
  'id',
  'number',
  'identifier',
  'customerIdentifier',
  'customerNumber',
  'contractNumber',
  'productContractNumber',
  'serviceContractNumber',
  'podIdentifier',
  'status',
  'contractStatus',
  'processType',
  'type',
  'name',
  'title',
] as const;

export const DEFAULT_ENTITY_KEY_ORDER = [
  'process',
  'customer',
  'pod',
  'product',
  'productContract',
  'service',
  'serviceContract',
  'billingRun',
  'invoice',
  'payment',
  'penalty',
  'termination',
  'action',
  'email',
  'sms',
  'massEmail',
  'massSms',
  'task',
  'deposit',
  'customerLiability',
  'customerReceivable',
];

/** Optional FRONTEND_BASE_URL override (trailing slash normalized). */
export function resolveFrontendBaseUrlOverride(): string | null {
  const env = process.env.FRONTEND_BASE_URL?.trim();
  if (!env) {
    return null;
  }
  return env.endsWith('/') ? env : `${env}/`;
}

export function lastEntityId(bucket: unknown): number | undefined {
  if (!Array.isArray(bucket) || bucket.length === 0) {
    return undefined;
  }
  const item = bucket[bucket.length - 1];
  if (typeof item === 'number' && Number.isFinite(item)) {
    return item;
  }
  if (item && typeof item === 'object' && 'id' in item) {
    const n = Number((item as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  return undefined;
}

function labelsFromEntity(item: unknown): Record<string, unknown> {
  if (!item || typeof item !== 'object') {
    return {};
  }
  const out: Record<string, unknown> = {};
  for (const field of SNAPSHOT_LABEL_FIELDS) {
    const v = (item as Record<string, unknown>)[field];
    if (v !== undefined && v !== null && v !== '') {
      out[field] = v;
    }
  }
  return out;
}

export function mergePortalLinks(
  fromResponses: Record<string, string[]>,
  extra?: Record<string, string[]>,
): Record<string, string[]> {
  const merged: Record<string, string[]> = { ...fromResponses };
  if (!extra) {
    return merged;
  }
  for (const [key, urls] of Object.entries(extra)) {
    const existing = merged[key] ?? [];
    const seen = new Set(existing);
    for (const url of urls) {
      if (!seen.has(url)) {
        existing.push(url);
        seen.add(url);
      }
    }
    if (existing.length > 0) {
      merged[key] = existing;
    }
  }
  return merged;
}

export function buildResponsesSnapshot(Responses: ResponsesContainer): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(Responses)) {
    if (!Array.isArray(value) || value.length === 0) {
      continue;
    }
    const id = lastEntityId(value);
    if (id != null) {
      out[`${key}Id`] = id;
    }
    const last = value[value.length - 1];
    const labels = labelsFromEntity(last);
    if (Object.keys(labels).length > 0) {
      out[`${key}Labels`] = labels;
    }
    if (value.length > 1) {
      out[`${key}Ids`] = value
        .map((item) => {
          if (typeof item === 'number') {
            return item;
          }
          if (item && typeof item === 'object' && 'id' in item) {
            return (item as { id: unknown }).id;
          }
          return undefined;
        })
        .filter((x) => x != null);
    }
  }
  return out;
}

export function inferFrontendFromLinks(links: Record<string, string[]>): string | null {
  for (const urls of Object.values(links)) {
    for (const url of urls) {
      const m = url.match(/^(https?:\/\/[^/]+\/)/i);
      if (m) {
        return m[1];
      }
    }
  }
  return null;
}

/** Mass import / process runs — open Processes preview in portal. */
export function buildProcessPreviewLink(
  processId: number,
  frontendBase?: string | null,
): string | null {
  if (!processId || processId <= 0) {
    return null;
  }
  const base = frontendBase ?? resolveFrontendBaseUrlOverride();
  if (!base) {
    return null;
  }
  const root = base.endsWith('/') ? base : `${base}/`;
  return `${root}processes/preview?id=${processId}`;
}

/** Common contract preview tabs for manual UI check. */
export function buildProductContractTabLinks(
  contractId: number,
  frontendBase?: string | null,
): Record<string, string[]> {
  if (!contractId || contractId <= 0) {
    return {};
  }
  const base = frontendBase ?? resolveFrontendBaseUrlOverride();
  if (!base) {
    return {};
  }
  const root = base.endsWith('/') ? base : `${base}/`;
  return {
    productContract: [
      `${root}energy-product-contracts/preview/basic-parameters?id=${contractId}`,
      `${root}energy-product-contracts/preview/point-of-delivery?id=${contractId}`,
    ],
  };
}

export function formatPlainPortalLinks(
  jiraKey: string,
  portalLinks: Record<string, string[]>,
  snapshot: Record<string, unknown>,
  entityKeyOrder: string[],
): string {
  const testCase = String(snapshot.testCase ?? 'unknown');
  const lines: string[] = [
    `========== [${jiraKey}] ${testCase} — portal links ==========`,
    '',
    '--- Entity snapshot ---',
    JSON.stringify(snapshot, null, 2),
    '',
    '--- Open in portal ---',
  ];

  if (Object.keys(portalLinks).length === 0) {
    lines.push(
      'not available',
      '(set FRONTEND_BASE_URL or run against a mapped API base)',
      `API base: ${String(snapshot.apiBase ?? '')}`,
    );
  } else {
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
  }

  lines.push(
    '',
    '--- Tips ---',
    'Playwright HTML report → test → Attachments → plain text block.',
    'Console: same block printed below the test run.',
    '================================================================',
  );
  return lines.join('\n');
}