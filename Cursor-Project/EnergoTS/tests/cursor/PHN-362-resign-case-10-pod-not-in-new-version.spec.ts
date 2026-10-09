/**
 * PHN-362 — Resign case 10: POD not in new contract version (mass import + sign)
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './massimportresign/phn-362-mass-import-general.fixtures';
import { runPhn362MassImportCase10Resign } from './massimportresign/phn-362-mass-import-resign.fixtures';

test.describe(
  'PHN-362 - Resign case 10 (POD not in new version)',
  { tag: ['@contractsAndOrders', '@dev2', '@resign', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] TC-BE-50 — POD cannot be placed in new F version', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      test.setTimeout(30 * 60 * 1000);
      test.info().annotations.push({ type: 'tc', description: 'TC-BE-50' });
      await runPhn362MassImportCase10Resign({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      });
    });
  },
);
