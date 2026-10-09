/**
 * PHN-362 — Mass import case 14: bug regression prep + Excel
 *
 * B: POD 01.05.2026 – 30.11.2026 (ACTIVE_IN_TERM).
 * F: multi-version import file (versions 1/2/4/3) for mass-import re-sign test.
 */

import { test } from '../../fixtures/baseFixture';
import { applyMassImportRiskListEnv } from './phn-362-mass-import-general.fixtures';
import { runPhn362MassImportPodCasePrepTest } from './phn-362-mass-import-pod-case.fixtures';

test.describe(
  'PHN-362 - Mass import case 14 (version-1243-pod-subranges)',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => applyMassImportRiskListEnv());

    test('[PHN-362] Case 14 — B POD May–Nov + multi-version F Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      await runPhn362MassImportPodCasePrepTest(
        '14',
        '14-version-1243-pod-subranges',
        'Bug regression — B POD May–Nov; F versions 1/2/4/3',
        { Request, GeneratePayload, Responses, Endpoints, FileUploadRequest },
      );
    });
  },
);
