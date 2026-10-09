/**
 * PHN-362 — Resign case 11: supply straddles two new versions (mass import E-row sign)
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './massimportresign/phn-362-mass-import-general.fixtures';
import { runPhn362MassImportCase11Resign } from './massimportresign/phn-362-mass-import-resign.fixtures';

test.describe(
  'PHN-362 - Resign case 11 (straddle two versions)',
  { tag: ['@contractsAndOrders', '@dev2', '@resign', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] TC-BE-51 — POD sub-ranges across two F versions', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      test.setTimeout(30 * 60 * 1000);
      test.info().annotations.push({ type: 'tc', description: 'TC-BE-51' });
      await runPhn362MassImportCase11Resign({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      });
    });
  },
);
