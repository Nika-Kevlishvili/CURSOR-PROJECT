/**
 * PHN-362 — Resign case 14 (API): bug regression — versions 1/2/4/3 POD sub-ranges
 *
 * Repro aligned with Dev2 manual run (EPES2606001905 → EPES2606001907):
 * B: POD 01.05–30.11, term end 30.06, version start 01.05.
 * F: four versions (1/2/4/3), signing 05.06.2026.
 * Assert via GET …/version/{id} — POD must exist on every version with per-version sub-ranges
 * (header contractPodsResponses may show a single aggregated row — not used for assertions).
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './massimportresign/phn-362-mass-import-general.fixtures';
import { runPhn362Case14BugfixResign } from './phn-362-resign-case-14-bugfix.fixtures';

test.describe(
  'PHN-362 - Resign case 14 (version 1-2-4-3 POD sub-ranges)',
  { tag: ['@contractsAndOrders', '@dev2', '@resign'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] API — create B+F, re-sign, assert per-version POD sub-ranges', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      test.setTimeout(35 * 60 * 1000);
      test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
      test.info().annotations.push({ type: 'tc', description: 'TC-BE-51 / TC-BE-54 bugfix' });
      await runPhn362Case14BugfixResign({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      });
    });
  },
);
