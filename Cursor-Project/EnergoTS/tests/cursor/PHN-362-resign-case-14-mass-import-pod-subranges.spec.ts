/**
 * PHN-362 — Resign case 14 (mass import): bug regression — versions 1/2/4/3 POD sub-ranges
 *
 * Creates B via API, imports F via Excel (valid RS product + four versions), re-signs on E-row sign.
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './massimportresign/phn-362-mass-import-general.fixtures';
import { runPhn362MassImportCase14Resign } from './massimportresign/phn-362-mass-import-resign.fixtures';

test.describe(
  'PHN-362 - Resign case 14 (mass import POD sub-ranges)',
  { tag: ['@contractsAndOrders', '@dev2', '@resign', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Mass import — B+F re-sign, per-version POD activation/deactivation', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      test.setTimeout(35 * 60 * 1000);
      test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
      test.info().annotations.push({ type: 'tc', description: 'Bug regression / TC-BE-51+54' });
      await runPhn362MassImportCase14Resign({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      });
    });
  },
);
