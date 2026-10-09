/**
 * PHN-362 — Mass import case 02: Wait=YES + EXACT_DATE
 *
 * Case 01: PHN-362-mass-import-old-contract.spec.ts (unchanged)
 *
 * Run:
 *   $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix-dev2'
 *   npx playwright test tests/cursor/PHN-362-mass-import-case-02-wait-yes-exact-date.spec.ts --project=main
 */

import { test } from '../../fixtures/baseFixture';
import {
  applyMassImportRiskListEnv,
  runPhn362MassImportCasePrepTest,
} from './phn-362-mass-import-general.fixtures';

test.describe(
  'PHN-362 - Mass import case 02 (wait-yes-exact-date)',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Case 02 — Wait=YES + EXACT_DATE → prep-manifest + local Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      await runPhn362MassImportCasePrepTest(
        '02',
        '02-wait-yes-exact-date',
        'Wait=YES + supply EXACT_DATE (bInitialTermEnd+1)',
        { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest },
      );
    });
  },
);
