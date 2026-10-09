/**

 * PHN-362 — Pre-re-sign data preparation (1 POD on B + F draft).

 *

 * - B (old): term end 2026-06-30, version/POD activation 2026-05-01, signing ~today-14, initial term on sign PUT = term end - 6d

 * - POD activation 2026-05-01 (first day of month — not end of month)

 * - F: DRAFT, Wait=Yes + FIRST_DAY_OF_MONTH — NOT signed

 *

 * Also writes prep-manifest.json → run generate-resign-flow-imports.mjs for Excel 01–06 (new F via mass import).

 *

 * Re-sign later: use the same POD on F (see attachment `podForResign.identifier`).

 * Expected after sign: F activation 2026-07-01, B deactivation 2026-06-30.

 *

 * Run on Dev2:

 *   $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix-dev2'

 *   npx playwright test tests/cursor/PHN-362-resign-precondition-two-pods.spec.ts --project=main

 *   node "C:/Users/k.nanuashvili/Desktop/tasks/mass import/phn-362/generate-resign-flow-imports.mjs"

 */



import { test } from '../../fixtures/baseFixture';

import reportGenerator from '../../utils/generateReport';

import {

  MASS_IMPORT_OUTPUT_DIR,

  productIdFromResponses,

  resolveCustomerForManifest,

  writeMassImportPrepManifestStep,

} from './massimportresign/phn-362-mass-import-manifest.fixtures';

import {

  PREP_B_SIGNING_DATE,

  PREP_B_TERM_END,

  PREP_EXPECTED_AFTER_RESIGN,

  PREP_POD_ACTIVATION,

  PREP_POD_INDEX_FOR_RESIGN,

  PREP_RESIGNING_DEADLINE_DAYS,

  PREP_RUNTIME_TODAY,

  assertPreResignState,

  createContractBPrep,

  createContractFDraftPrep,

  runSharedPreconditionsPrep,

} from './phn-362-resign-two-pods-precondition.fixtures';

import { loadProductContract } from './pdt-2815-version-validity.fixtures';



test.describe('PHN-362 - Re-sign precondition data', { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] }, () => {

  test('[PHN-362] TC-BE-39-Prep: Data ready for re-sign — 1 POD, F draft (no sign)', async ({

    Request,

    GeneratePayload,

    Responses,

    Endpoints,

    FileUploadRequest,

  }) => {

    test.setTimeout(25 * 60 * 1000);

    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });

    test.info().annotations.push({ type: 'tc', description: 'TC-BE-39-Prep' });



    let contractBId = 0;

    let contractFId = 0;

    let contractNumber = 'EPES3620000001';

    let podIdentifier = '';

    let podId = 0;

    let podDetailId = 0;

    let manifestPath = '';



    await runSharedPreconditionsPrep(

      Request,

      GeneratePayload,

      Responses,

      Endpoints,

      FileUploadRequest,

    );



    await test.step('Precondition: old contract B — 1 POD, ACTIVE_IN_TERM [TC-BE-10/18]', async () => {

      const b = await createContractBPrep(Request, GeneratePayload, Responses, Endpoints);

      contractBId = b.contractId;

      podIdentifier = b.podIdentifier;

      podId = b.podId;

      podDetailId = b.podDetailId;



      const bBody = await loadProductContract(Request, contractBId);

      const bp = bBody.basicParameters as { contractNumber?: string };

      contractNumber = bp.contractNumber ?? contractNumber;

    });



    await test.step('Precondition: new contract F (DRAFT) — same POD, Wait=Yes [TC-BE-5/19]', async () => {

      const f = await createContractFDraftPrep(Request, GeneratePayload, Responses, Endpoints);

      contractFId = f.contractId;

    });



    await assertPreResignState(Request, contractBId, contractFId, podIdentifier);



    manifestPath = await writeMassImportPrepManifestStep(Request, Endpoints, Responses, {

      contractBId,

      contractNumber,

      podIdentifier,

      podId,

      podDetailId,

    });



    const customerManifest = await resolveCustomerForManifest(Request, Endpoints, Responses);



    test.info().attach('[PHN-362] TC-BE-39-Prep — re-sign data (IDs + dates)', {

      body: JSON.stringify(

        {

          runtimeToday: PREP_RUNTIME_TODAY,

          massImport: {

            manifestPath,

            outputDir: MASS_IMPORT_OUTPUT_DIR,

            nextStep:

              'node config/mass-import/phn-362/generate-resign-flow-imports.mjs → import 01–06 only (B exists; F draft via API is optional)',

          },

          contractB: {

            id: contractBId,

            contractNumber,

            signingDate: PREP_B_SIGNING_DATE,

            contractTermEndDate: PREP_B_TERM_END,

            resigningDeadlineDays: PREP_RESIGNING_DEADLINE_DAYS,

          },

          contractF: {

            id: contractFId,

            status: 'DRAFT',

            nextStep: 'Sign F (SIGNED_BY_BOTH_SIDES) with signingDate = today to trigger re-sign',

          },

          customer: customerManifest,

          products: {

            stdId: productIdFromResponses(Responses.product[0]),

            rsId: productIdFromResponses(Responses.product[1]),

          },

          podForResign: {

            useThisPod: 'Same POD on B and F — sign contract F to re-sign',

            responsesPodIndex: PREP_POD_INDEX_FOR_RESIGN,

            identifier: podIdentifier,

            podId,

            podDetailId,

            activationOnB: PREP_POD_ACTIVATION,

            note: 'Activation is first day of month (2026-05-01), not end of month',

          },

          expectedAfterResign: PREP_EXPECTED_AFTER_RESIGN,

          trace: reportGenerator.setLinksToResponses(Responses),

        },

        null,

        2,

      ),

      contentType: 'application/json',

    });

  });

});


