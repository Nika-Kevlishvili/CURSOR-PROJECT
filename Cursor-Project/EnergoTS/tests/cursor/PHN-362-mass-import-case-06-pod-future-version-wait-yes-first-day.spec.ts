/**
 * PHN-362 — Mass import case 06: POD future version + Wait=YES + FIRST_DAY_OF_MONTH
 *
 * Case 01: PHN-362-mass-import-old-contract.spec.ts (unchanged)
 */

import { test } from '../../fixtures/baseFixture';
import {
  applyMassImportRiskListEnv,
  runPhn362MassImportCasePrepTest,
} from './phn-362-mass-import-general.fixtures';

test.describe(
  'PHN-362 - Mass import case 06 (pod-future-version)',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Case 06 — POD future version → prep-manifest + local Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      await runPhn362MassImportCasePrepTest(
        '06',
        '06-pod-future-version-wait-yes-first-day',
        'POD future version + Wait=YES + FIRST_DAY_OF_MONTH',
        { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest },
      );
    });
  },
);
