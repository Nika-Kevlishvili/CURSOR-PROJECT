/**
 * PHN-362 — Mass import case 07: AC4 old-version warning (prep + Excel)
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './phn-362-mass-import-general.fixtures';
import { runPhn362MassImportPodCasePrepTest } from './phn-362-mass-import-pod-case.fixtures';

test.describe(
  'PHN-362 - Mass import case 07 (ac4-old-version-warning)',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Case 07 — AC4 old version warning → prep-manifest + local Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      await runPhn362MassImportPodCasePrepTest(
        '07',
        '07-ac4-old-version-warning',
        'AC4 — activation outside old contract version; no confirm',
        { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest },
      );
    });
  },
);
