/**
 * PDT-2529 — single-iteration contract prep (same chain as happy-path spec).
 */
import { test } from '../../fixtures/baseFixture';

import { runPdt2529ContractPrep } from './PDT-2529-rfd-data-model-happy-path.fixtures';

test.describe('[PDT-2529]: RFD data seed (no billing)', { tag: '@receivableManagement' }, () => {
  test('[PDT-2529]: Prep contract data for volume RFD — payment term 1d', async ({
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
