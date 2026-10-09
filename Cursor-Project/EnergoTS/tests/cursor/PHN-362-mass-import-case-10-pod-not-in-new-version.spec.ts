/**
 * PHN-362 — Mass import case 10: POD not in new contract version (prep + Excel)
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './phn-362-mass-import-general.fixtures';
import { runPhn362MassImportPodCasePrepTest } from './phn-362-mass-import-pod-case.fixtures';

test.describe(
  'PHN-362 - Mass import case 10 (pod-not-in-new-version)',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Case 10 — POD not in new version → prep-manifest + local Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      await runPhn362MassImportPodCasePrepTest(
        '10',
        '10-pod-not-in-new-version',
        'POD not activatable in new contract versions (TC-BE-50)',
        { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest },
      );
    });
  },
);
