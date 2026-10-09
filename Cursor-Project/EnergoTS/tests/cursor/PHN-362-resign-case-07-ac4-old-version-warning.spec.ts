/**
 * PHN-362 — Resign case 07: AC4 warning, user not confirmed (mass import + sign)
 *
 * Run on Dev2:
 *   $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix-dev2'
 *   npx playwright test tests/cursor/PHN-362-resign-case-07-ac4-old-version-warning.spec.ts --project=main
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './massimportresign/phn-362-mass-import-general.fixtures';
import { runPhn362MassImportCase07Resign } from './massimportresign/phn-362-mass-import-resign.fixtures';

test.describe(
  'PHN-362 - Resign case 07 (AC4 old-version warning)',
  { tag: ['@contractsAndOrders', '@dev2', '@resign', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] TC-BE-17/41 — AC4 block without confirm (mass import)', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      test.setTimeout(30 * 60 * 1000);
      test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
      test.info().annotations.push({ type: 'tc', description: 'TC-BE-17 / TC-BE-41' });
      await runPhn362MassImportCase07Resign({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      });
    });
  },
);
