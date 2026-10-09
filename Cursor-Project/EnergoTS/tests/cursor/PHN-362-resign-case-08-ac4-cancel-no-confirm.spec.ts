/**
 * PHN-362 — Resign case 08: AC4 cancel, no confirmRemoveFutureActivation (API only)
 */

import { test } from '../../fixtures/baseFixture';
import { phn362ResignCtx, runPhn362Case08Ac4Cancel } from './phn-362-resign-scenarios.fixtures';

test.describe(
  'PHN-362 - Resign case 08 (AC4 cancel)',
  { tag: ['@contractsAndOrders', '@dev2', '@resign'] },
  () => {
    test('[PHN-362] TC-BE-44 — future activation conflict, no confirm', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    }) => {
      test.setTimeout(25 * 60 * 1000);
      test.info().annotations.push({ type: 'tc', description: 'TC-BE-44' });
      await runPhn362Case08Ac4Cancel(phn362ResignCtx(Request, GeneratePayload, Responses, Endpoints));
    });
  },
);
