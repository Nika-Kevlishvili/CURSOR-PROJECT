/**
 * PHN-362 — Mass import case 04: Wait=NO + EXACT_DATE
 *
 * Case 01: PHN-362-mass-import-old-contract.spec.ts (unchanged)
 */

import { test } from '../../fixtures/baseFixture';
import {
  applyMassImportRiskListEnv,
  runPhn362MassImportCasePrepTest,
} from './phn-362-mass-import-general.fixtures';

test.describe(
  'PHN-362 - Mass import case 04 (wait-no-exact-date)',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Case 04 — Wait=NO + EXACT_DATE → prep-manifest + local Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      await runPhn362MassImportCasePrepTest(
        '04',
        '04-wait-no-exact-date',
        'Wait=NO + supply EXACT_DATE (fSigning+1 month)',
        { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest },
      );
    });
  },
);
