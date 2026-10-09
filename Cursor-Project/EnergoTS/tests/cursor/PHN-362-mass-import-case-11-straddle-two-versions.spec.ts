/**
 * PHN-362 — Mass import case 11: straddle two new versions (prep + Excel)
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './phn-362-mass-import-general.fixtures';
import { runPhn362MassImportPodCasePrepTest } from './phn-362-mass-import-pod-case.fixtures';

test.describe(
  'PHN-362 - Mass import case 11 (straddle-two-versions)',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Case 11 — straddle two versions → prep-manifest + local Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      await runPhn362MassImportPodCasePrepTest(
        '11',
        '11-straddle-two-versions',
        'Supply straddles two new contract versions (TC-BE-51)',
        { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest },
      );
    });
  },
);
