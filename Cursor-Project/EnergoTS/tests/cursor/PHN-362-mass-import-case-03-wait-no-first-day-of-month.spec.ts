/**
 * PHN-362 — Mass import case 03: Wait=NO + FIRST_DAY_OF_MONTH
 *
 * Case 01: PHN-362-mass-import-old-contract.spec.ts (unchanged)
 */

import { test } from '../../fixtures/baseFixture';
import {
  applyMassImportRiskListEnv,
  runPhn362MassImportCasePrepTest,
} from './phn-362-mass-import-general.fixtures';

test.describe(
  'PHN-362 - Mass import case 03 (wait-no-first-day-of-month)',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Case 03 — Wait=NO + FIRST_DAY_OF_MONTH → prep-manifest + local Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      await runPhn362MassImportCasePrepTest(
        '03',
        '03-wait-no-first-day-of-month',
        'Wait=NO + supply FIRST_DAY_OF_MONTH',
        { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest },
      );
    });
  },
);
