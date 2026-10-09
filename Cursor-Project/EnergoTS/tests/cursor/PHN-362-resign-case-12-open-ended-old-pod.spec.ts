/**
 * PHN-362 — Resign case 12: open-ended old POD inheritance (mass import + sign)
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './massimportresign/phn-362-mass-import-general.fixtures';
import { runPhn362MassImportCase12Resign } from './massimportresign/phn-362-mass-import-resign.fixtures';

test.describe(
  'PHN-362 - Resign case 12 (open-ended old POD)',
  { tag: ['@contractsAndOrders', '@dev2', '@resign', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] TC-BE-42 — new POD inherits open-ended supply', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      test.setTimeout(30 * 60 * 1000);
      test.info().annotations.push({ type: 'tc', description: 'TC-BE-42' });
      await runPhn362MassImportCase12Resign({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      });
    });
  },
);
