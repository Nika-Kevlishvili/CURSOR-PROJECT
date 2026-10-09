/**
 * PHN-362 — Mass import: old contract B (Playwright adapter).
 *
 * Full setup every run → tasks/mass import/phn-362/setup-case.mjs (or setup-case.ps1)
 *
 * Run alone (prep + local Excel update every time):
 *   $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix-dev2'
 *   npx playwright test tests/cursor/PHN-362-mass-import-old-contract.spec.ts --project=main
 *
 * Writes: tasks/mass import/phn-362-resign-flows/prep-manifest.json
 *         tasks/mass import/phn-362-resign-flows/01-wait-yes-first-day-of-month.xlsx (case 01)
 */

import { test } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  resolveMassImportCaseId,
  runMassImportOldContractPrep,
} from './phn-362-mass-import-old-contract.fixtures';
import { MASS_IMPORT_OUTPUT_DIR, resolveCustomerForManifest } from './phn-362-mass-import-manifest.fixtures';
import {
  PREP_B_SIGNING_DATE,
  PREP_B_TERM_END,
  PREP_POD_ACTIVATION,
  PREP_RUNTIME_TODAY,
} from './phn-362-resign-two-pods-precondition.fixtures';

/** Dev2 Risk List API permits UIC 88888; random customer UIC → mass import fails. */
const DEV2_RISKLIST_CUSTOMER_ID = process.env.MASS_IMPORT_RISKLIST_CUSTOMER_ID ?? '6025823';
const DEV2_RISKLIST_UIC = process.env.MASS_IMPORT_RISKLIST_UIC ?? '88888';

test.describe(
  'PHN-362 - Mass import old contract',
  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },
  () => {
    test.beforeAll(() => {
      process.env.MASS_IMPORT_RISKLIST_CUSTOMER_ID ??= DEV2_RISKLIST_CUSTOMER_ID;
      process.env.MASS_IMPORT_RISKLIST_UIC ??= DEV2_RISKLIST_UIC;
    });

    test('[PHN-362] Mass-import old contract B + products → prep-manifest + local Excel', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    }) => {
      test.setTimeout(25 * 60 * 1000);
      const caseId = resolveMassImportCaseId();
      test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
      test.info().annotations.push({ type: 'workflow', description: 'mass-import-old-contract' });
      test.info().annotations.push({ type: 'mass-import-case', description: caseId });

      const prep = await runMassImportOldContractPrep(
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      );

      const customerManifest = await resolveCustomerForManifest(Request, Endpoints, Responses);
      const massImportCase = prep.massImportCase;

      test.info().attach('[PHN-362] Mass-import old contract — prep-manifest.json summary', {
        body: JSON.stringify(
          {
            runtimeToday: PREP_RUNTIME_TODAY,
            manifestPath: prep.manifestPath,
            outputDir: MASS_IMPORT_OUTPUT_DIR,
            massImportCase,
            contractB: {
              id: prep.contractBId,
              contractNumber: prep.contractNumber,
              signingDate: PREP_B_SIGNING_DATE,
              contractTermEndDate: PREP_B_TERM_END,
              status: 'ACTIVE_IN_TERM',
              productStdId: prep.productStdId,
            },
            massImportF: {
              productRsId: prep.productRsId,
              contractNumber: prep.fContractNumber,
              waitForOld: massImportCase.waitForOld,
              supplyAfterResign: massImportCase.supplyAfterResign,
              excelFile: massImportCase.file,
              excelPath: prep.excelPath,
            },
            customer: customerManifest,
            pod: {
              identifier: prep.podIdentifier,
              podId: prep.podId,
              podDetailId: prep.podDetailId,
              activationOnB: PREP_POD_ACTIVATION,
            },
            nextStep: `Upload ${prep.excelPath} (updated on this Playwright run)`,
            trace: reportGenerator.setLinksToResponses(Responses),
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
    });
  },
);
