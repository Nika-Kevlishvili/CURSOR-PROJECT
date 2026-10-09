/**
 * PDT-2529 — contract prep for volume RFD (customer, term 1d, PODs, contract, activation,
 * separate invoice per POD, billing-by-profile per POD). No billing run.
 *
 * Env:
 * - `RFD_HAPPY_POD_COUNT` (default 3)
 * - `PDT2529_BULK_FAST=1` — lean checks, no report attachments (bulk)
 * - `PDT2529_BULK_REUSE_CATALOG=1` — one term/product/price per worker (bulk)
 *
 * Bulk (8000, 8 workers):
 *   $env:PDT2529_BULK_FAST="1"; $env:PDT2529_BULK_REUSE_CATALOG="1"
 *   npx playwright test tests/cursor/PDT-2529-rfd-data-model-happy-path.spec.ts --project=main --repeat-each=8000 --workers=8 --retries=0 --reporter=line
 */
import { test } from '../../fixtures/baseFixture';

import { runPdt2529ContractPrep } from './PDT-2529-rfd-data-model-happy-path.fixtures';

test.describe('[PDT-2529]: RFD data model happy path', { tag: '@receivableManagement' }, () => {
  test('[PDT-2529]: Prep contract data for volume RFD — payment term 1d (billing manual)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(15 * 60 * 1000);

    await runPdt2529ContractPrep(
      { Request, GeneratePayload, Responses, Endpoints },
      { workerIndex: test.info().workerIndex, iteration: test.info().repeatEachIndex },
    );
  });
});
