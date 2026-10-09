/**
 * PHN-4048 — sequential mass POST of pre-generated billing-by-scales batch JSON
 * to Dev2. Does not wait for process_management COMPLETED / DB persist.
 *
 * Runtime: POST /billing-by-scales/batch → HTTP 202
 * { accepted, stateIds[] } (OpenAPI documents 200; assert 202).
 * Each POST body has ≤7000 requests (Dev2 heap OOM at 8000+; proven max 7000).
 * OpenAPI BillingByScalesBatchCreateRequest.requests.maxItems is still 10000;
 * this test must not send more than 7000. Names starting with USED- are ignored.
 *
 * After HTTP 202 + accepted/stateIds match, the JSON is renamed USED-{original}
 * in the same directory so the next run skips it. Failed POST/CheckResponse/202
 * /accepted assertions throw before rename.
 *
 * Swagger: Cursor-Project/config/swagger/dev2/swagger-spec.json (refreshed
 * 2026-09-01). Path POST /billing-by-scales/batch is present after refresh.
 *
 * Reference spec(s):
 * - tests/cursor/PHN-4048-billing-data-by-scales-create-and-edit.spec.ts
 * - tests/cursor/phn-4048-billing-data-by-scales-create-and-edit.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 * - fixtures/constants/endpoints.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { PHN_4048_JIRA_TITLE, PHN_4048_KEY } from './phn-4048-billing-data-by-scales-create-and-edit.fixtures';
import {
  BATCH_POST_TIMEOUT_MS,
  BATCH_TEST_TIMEOUT_MS,
  PHN_4048_BATCH_DIR,
  assertBatchAccepted,
  assertBatchFileSetComplete,
  batchEndpoint,
  listBatchPartFiles,
  loadBatchFile,
  markBatchFileUsed,
  recordBatchCheck,
  type BillingByScalesBatchCreateResponse,
} from './phn-4048-billing-by-scales-batch-perf.fixtures';

const TAGS = ['@phn-4048', '@billingByScales', '@batch', '@dev2'];

test.describe(`[${PHN_4048_KEY}]: ${PHN_4048_JIRA_TITLE}`, { tag: TAGS }, () => {
  test(`[${PHN_4048_KEY}]: ${PHN_4048_JIRA_TITLE}`, async ({
    Request,
    Endpoints,
    Responses,
    TestRunSummary,
  }) => {
    test.setTimeout(BATCH_TEST_TIMEOUT_MS);

    const files = await test.step('Precondition: resolve billing-by-scales batch JSON files', async () => {
      const listed = listBatchPartFiles();
      assertBatchFileSetComplete(listed);
      if (listed.length === 0) {
        TestRunSummary.recordCheck({
          check: 'no remaining active batch JSON (all marked USED-)',
          expectedResult:
            'No remaining active billing-by-scales-batch-dev2-part JSON; all remaining files already marked USED-.',
          actualResult: `As expected — 0 active parts under ${PHN_4048_BATCH_DIR}.`,
          passed: true,
        });
      }
      return listed;
    });

    const endpoint = batchEndpoint(Endpoints.dataByScales);
    let sampleRegistered = false;
    let totalRequests = 0;
    let totalAccepted = 0;
    let firstStateId: unknown;
    const markedUsed: string[] = [];
    const partResults: Array<{
      name: string;
      status: number;
      accepted: number;
      stateIdCount: number;
      elapsedMs: number;
      markedUsed?: string;
    }> = [];

    for (const file of files) {
      await test.step(`POST /billing-by-scales/batch ${file.name}`, async () => {
        const loaded = loadBatchFile(file.path);
        if (!sampleRegistered) {
          TestRunSummary.registerPayload('dataByScales', {
            requests: [loaded.sampleRequest],
          });
          sampleRegistered = true;
        }

        const started = Date.now();
        const response = await Request.post(endpoint, {
          data: loaded.rawBody,
          timeout: BATCH_POST_TIMEOUT_MS,
        });
        await expect(response, `POST ${endpoint} ${file.name}`).CheckResponse();
        const status = response.status();
        const body = (await response.json()) as BillingByScalesBatchCreateResponse;
        const elapsedMs = Date.now() - started;
        const stateIds = Array.isArray(body.stateIds) ? body.stateIds : [];

        recordBatchCheck(
          TestRunSummary,
          `POST /billing-by-scales/batch ${file.name}`,
          `HTTP 202 (runtime); accepted === ${loaded.requestCount}; stateIds.length === ${loaded.requestCount}.`,
          `As expected — HTTP ${status}; accepted=${String(body.accepted)}; stateIds=${stateIds.length}; ${elapsedMs}ms.`,
          () => {
            expect(status, `${file.name} runtime HTTP 202`).toBe(202);
            assertBatchAccepted(body, loaded.requestCount);
          },
        );

        const usedPath = markBatchFileUsed(file.path);
        markedUsed.push(usedPath);
        console.log(`[PHN-4048] renamed to ${usedPath}`);

        totalRequests += loaded.requestCount;
        totalAccepted += Number(body.accepted ?? 0);
        if (firstStateId === undefined && stateIds.length) {
          firstStateId = stateIds[0];
        }
        partResults.push({
          name: file.name,
          status,
          accepted: Number(body.accepted ?? 0),
          stateIdCount: stateIds.length,
          elapsedMs,
          markedUsed: usedPath,
        });
        console.log(
          `[PHN-4048] ${file.name} HTTP ${status} accepted=${String(body.accepted)} stateIds=${stateIds.length} ${elapsedMs}ms`,
        );
      });
    }

    recordBatchCheck(
      TestRunSummary,
      'All batch parts accepted',
      files.length > 0
        ? `Every part posted this run returned HTTP 202; sum(accepted) === sum(requests) across ${files.length} files.`
        : 'No remaining active batch JSON; totals 0.',
      `As expected — files=${files.length}; posted=${partResults.length}; totalRequests=${totalRequests}; totalAccepted=${totalAccepted}.`,
      () => {
        expect(partResults, 'posted every resolved part this run').toHaveLength(files.length);
        expect(totalAccepted, 'sum of accepted equals sum of requests').toBe(totalRequests);
      },
    );

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_4048_KEY,
        relevantEntityKeys: ['dataByScales'],
        snapshot: {
          batchDir: PHN_4048_BATCH_DIR,
          endpoint,
          fileCount: files.length,
          totalRequests,
          totalAccepted,
          firstStateId,
          partResults,
          markedUsed,
        },
      });
    });
  });
});
