/**
 * PHN-362 — Mass import case 12: open-ended old POD (prep + Excel)
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './phn-362-mass-import-general.fixtures';
import { runPhn362MassImportPodCasePrepTest } from './phn-362-mass-import-pod-case.fixtures';

test.describe(
  'PHN-362 - Mass import case 12 (open-ended-old-pod)',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Case 12 — open-ended old POD → prep-manifest + local Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      await runPhn362MassImportPodCasePrepTest(
        '12',
        '12-open-ended-old-pod',
        'Open-ended old POD — new inherits open-ended (TC-BE-42)',
        { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest },
      );
    });
  },
);
