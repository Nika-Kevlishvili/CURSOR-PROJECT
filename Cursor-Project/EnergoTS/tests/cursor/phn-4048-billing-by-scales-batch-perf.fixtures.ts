/**
 * PHN-4048 — sequential POST /billing-by-scales/batch of pre-generated Dev2 JSON parts.
 *
 * Live Dev2 OpenAPI (refreshed 2026-09-01, GET /v3/api-docs on 10.236.20.11:8092
 * and phoenix2-dev): POST /billing-by-scales/batch
 * request BillingByScalesBatchCreateRequest required `requests`
 * (BillingByScalesCreateRequest[], OpenAPI maxItems 10000).
 * Documented response 200 BillingByScalesBatchCreateResponse
 * { accepted: int32, stateIds: int64[] }.
 * Runtime proven 2026-08-22: HTTP 202 body {"accepted":1,"stateIds":[7]}.
 *
 * Official update-swagger-specs.ps1 failed on this Mac (curl.exe missing).
 * Dev2 spec was refreshed with /usr/bin/curl to the same output path.
 *
 * Runtime/safety for THIS test: send ≤7000 requests per POST.
 * Dev2 heap OOMs at 8000+ items; proven max is 7000.
 * Active JSON parts have max 7000 requests each (last part may be smaller).
 * Original 10k files were renamed (not deleted) to USED-10k-* and must not be POSTed.
 * OpenAPI contract still maxItems 10000; this test must not send more than 7000.
 *
 * After HTTP 202 + accepted/stateIds match, the file is renamed with prefix
 * USED- so reruns skip it (same directory; not deleted). Does not wait for
 * process_management COMPLETED / DB persist.
 *
 * JSON parts are generated outside EnergoTS (do not create them in this test):
 * Cursor-Project/generated_data/500k billing-by-scales/
 * (repo-relative from this file: ../../../generated_data/500k billing-by-scales)
 * Active: billing-by-scales-batch-dev2-part-NNN-of-MMM.json
 * Ignored: names starting with USED- (USED-10k-* originals and USED- after a
 * successful POST). Regex matches active names only; listBatchPartFiles also
 * skips USED- prefix. Missing earlier parts (e.g. 001 already USED-) is expected.
 *
 * Reference spec(s):
 * - tests/cursor/PHN-4048-billing-data-by-scales-create-and-edit.spec.ts
 * - tests/cursor/phn-4048-billing-data-by-scales-create-and-edit.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 * - fixtures/constants/endpoints.ts (Endpoints.dataByScales)
 */

import * as fs from 'fs';
import path from 'path';
import { expect } from '../../fixtures/baseFixture';
import type { TestRunSummaryCollector } from './shared/test-run-summary.fixtures';

export const PHN_4048_BATCH_DIR = path.resolve(
  __dirname,
  '../../../generated_data/500k billing-by-scales',
);

/** Prefix applied after a successful POST so the next run skips the file. */
export const USED_FILE_PREFIX = 'USED-';

/**
 * Active batch parts only. Does not match renamed originals such as
 * USED-10k-billing-by-scales-batch-dev2-part-*-of-050.json, or files renamed
 * USED-{originalName} after HTTP 202 + accepted/stateIds match.
 */
export const PHN_4048_BATCH_FILE_RE =
  /^billing-by-scales-batch-dev2-part-(\d+)-of-(\d+)\.json$/;

/**
 * Dev2 heap-safe ceiling per POST. OpenAPI still allows maxItems 10000.
 * Do not POST more than this even if the contract permits it.
 */
export const BATCH_RUNTIME_MAX_REQUESTS = 7000;

/** One 15–40MB POST can exceed Playwright's default 30s API timeout. */
export const BATCH_POST_TIMEOUT_MS = 20 * 60 * 1000;

/** ~50 sequential large uploads; 1h may be tight if the API is slow. */
export const BATCH_TEST_TIMEOUT_MS = 2 * 60 * 60 * 1000;

/**
 * Already created on Dev2 (single create + proven batch of one).
 * Must not appear in the mass-import JSON parts.
 */
export const EXCLUDED_BATCH_IDENTIFIERS = new Set([
  'G2M000000165J9K26TYA0L9PXQ2OXBGDQ',
  'G2M000000204GZ93GQCRC5FYOMC97L4CU',
]);

export type BatchPartFile = {
  path: string;
  name: string;
  part: number;
  of: number;
};

export type BillingByScalesBatchCreateResponse = {
  accepted?: number;
  stateIds?: unknown;
};

export type LoadedBatchFile = {
  rawBody: string;
  requestCount: number;
  sampleRequest: Record<string, unknown>;
};

export function listBatchPartFiles(dir = PHN_4048_BATCH_DIR): BatchPartFile[] {
  expect(fs.existsSync(dir), `batch JSON directory exists: ${dir}`).toBe(true);
  const files: BatchPartFile[] = [];
  for (const name of fs.readdirSync(dir)) {
    // Belt and suspenders: USED-10k-* originals and USED- after a successful POST must not be POSTed.
    if (name.startsWith(USED_FILE_PREFIX)) {
      continue;
    }
    const match = PHN_4048_BATCH_FILE_RE.exec(name);
    if (!match) {
      continue;
    }
    files.push({
      path: path.join(dir, name),
      name,
      part: Number(match[1]),
      of: Number(match[2]),
    });
  }
  files.sort((a, b) => a.part - b.part || a.name.localeCompare(b.name));
  return files;
}

/**
 * Remaining active parts only. Empty is allowed (all remaining files already USED-).
 * Does not require parts 1..of to all be present: after part 001 is marked USED-,
 * remaining files may be 002-066 with 001 missing.
 * When non-empty: every file shares the same of-MMM; part numbers are unique.
 * listBatchPartFiles already sorts by part.
 */
export function assertBatchFileSetComplete(files: BatchPartFile[]): void {
  if (files.length === 0) {
    return;
  }
  const declaredOf = files[0].of;
  expect(
    files.every((file) => file.of === declaredOf),
    'every filename shares the same of-MMM total',
  ).toBe(true);
  const parts = files.map((file) => file.part);
  expect(new Set(parts).size, 'batch part numbers are unique').toBe(parts.length);
  const sortedByPart = [...parts].sort((a, b) => a - b);
  expect(parts, 'batch parts are sorted by part number').toEqual(sortedByPart);
}

/**
 * Rename a successfully posted batch JSON so the next run skips it.
 * Stays in the same directory. Does not overwrite an existing destination.
 */
export function markBatchFileUsed(filePath: string): string {
  const dir = path.dirname(filePath);
  const originalName = path.basename(filePath);
  if (originalName.startsWith(USED_FILE_PREFIX)) {
    return filePath;
  }
  const destPath = path.join(dir, `${USED_FILE_PREFIX}${originalName}`);
  if (fs.existsSync(destPath)) {
    throw new Error(`Cannot mark batch file used: destination already exists: ${destPath}`);
  }
  fs.renameSync(filePath, destPath);
  return destPath;
}

export function loadBatchFile(filePath: string): LoadedBatchFile {
  const rawBody = fs.readFileSync(filePath, 'utf8');
  let parsed: { requests?: unknown };
  try {
    parsed = JSON.parse(rawBody) as { requests?: unknown };
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    throw new Error(`Invalid JSON in ${filePath}: ${message}`);
  }
  expect(Array.isArray(parsed.requests), `${filePath} has requests[]`).toBe(true);
  const requests = parsed.requests as Record<string, unknown>[];
  expect(requests.length, `${filePath} requests length`).toBeGreaterThan(0);
  expect(
    requests.length,
    `${path.basename(filePath)} runtime max ${BATCH_RUNTIME_MAX_REQUESTS} (OpenAPI maxItems 10000)`,
  ).toBeLessThanOrEqual(BATCH_RUNTIME_MAX_REQUESTS);

  const excludedHit = requests.find((row) =>
    EXCLUDED_BATCH_IDENTIFIERS.has(String(row.identifier ?? '')),
  );
  expect(
    excludedHit,
    `${path.basename(filePath)} must not include already-created identifier ${String(excludedHit?.identifier ?? '')}`,
  ).toBeUndefined();

  return {
    rawBody,
    requestCount: requests.length,
    sampleRequest: requests[0],
  };
}

export function batchEndpoint(dataByScales: string): string {
  return `${dataByScales}/batch`;
}

export function assertBatchAccepted(
  body: BillingByScalesBatchCreateResponse,
  expectedCount: number,
): void {
  expect(body.accepted, 'BillingByScalesBatchCreateResponse.accepted').toBe(expectedCount);
  expect(Array.isArray(body.stateIds), 'BillingByScalesBatchCreateResponse.stateIds is array').toBe(
    true,
  );
  expect(body.stateIds as unknown[], 'stateIds length matches accepted requests').toHaveLength(
    expectedCount,
  );
}

export function recordBatchCheck(
  TestRunSummary: TestRunSummaryCollector,
  check: string,
  expectedResult: string,
  actualOnPass: string,
  fn: () => void,
): void {
  let passed = true;
  let assertionError: string | undefined;
  try {
    fn();
  } catch (err) {
    passed = false;
    assertionError = err instanceof Error ? err.message : String(err);
    throw err;
  } finally {
    TestRunSummary.recordCheck({
      check,
      expectedResult,
      actualResult: passed ? actualOnPass : `Not as expected — ${assertionError ?? ''}`.trim(),
      passed,
    });
  }
}
