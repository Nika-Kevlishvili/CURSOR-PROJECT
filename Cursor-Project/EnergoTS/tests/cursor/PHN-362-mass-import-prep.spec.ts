/**

 * PHN-362 — Mass-import prep (legacy alias).

 *

 * Prefer: PHN-362-mass-import-old-contract.spec.ts (explicit old-contract + case 01 manifest).

 * Old contract B ONLY via Playwright (no mass import for B). No F draft.

 *

 * Run on Dev2:

 *   $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix-dev2'

 *   npx playwright test tests/cursor/PHN-362-mass-import-prep.spec.ts --project=main

 *   node "C:/Users/k.nanuashvili/Desktop/tasks/mass import/phn-362/generate-resign-flow-imports.mjs"

 */



import { expect, test } from '../../fixtures/baseFixture';

import reportGenerator from '../../utils/generateReport';

import {

  MASS_IMPORT_OUTPUT_DIR,

  productIdFromResponses,

  resolveCustomerForManifest,

  writeMassImportPrepManifestStep,

} from './phn-362-mass-import-manifest.fixtures';

import {

  PREP_B_SIGNING_DATE,

  PREP_B_TERM_END,

  PREP_POD_ACTIVATION,

  PREP_RUNTIME_TODAY,

  createContractBPrep,

  runSharedPreconditionsPrep,

} from './phn-362-resign-two-pods-precondition.fixtures';

import { loadProductContract } from './pdt-2815-version-validity.fixtures';



test.describe(

  'PHN-362 - Mass-import prep',

  { tag: ['@contractsAndOrders', '@dev2', '@prep', '@mass-import'] },

  () => {

    test('[PHN-362] Mass-import prep: products + B ACTIVE_IN_TERM → prep-manifest.json', async ({

      Request,

      GeneratePayload,

      Responses,

      Endpoints,

      FileUploadRequest,

    }) => {

      test.setTimeout(25 * 60 * 1000);

      test.info().annotations.push({ type: 'jira', description: 'PHN-362' });

      test.info().annotations.push({ type: 'workflow', description: 'mass-import-prep' });



      let contractBId = 0;

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



      await test.step('Precondition: old contract B — 1 POD, ACTIVE_IN_TERM', async () => {

        const b = await createContractBPrep(Request, GeneratePayload, Responses, Endpoints);

        contractBId = b.contractId;

        podIdentifier = b.podIdentifier;

        podId = b.podId;

        podDetailId = b.podDetailId;



        const bBody = await loadProductContract(Request, contractBId);

        const bp = bBody.basicParameters as { contractNumber?: string; status?: string };

        contractNumber = bp.contractNumber ?? contractNumber;

        expect(bp.status).toBe('ACTIVE_IN_TERM');

      });



      manifestPath = await writeMassImportPrepManifestStep(Request, Endpoints, Responses, {

        contractBId,

        contractNumber,

        podIdentifier,

        podId,

        podDetailId,

      });



      const customerManifest = await resolveCustomerForManifest(Request, Endpoints, Responses);



      test.info().attach('[PHN-362] Mass-import prep — prep-manifest.json summary', {

        body: JSON.stringify(

          {

            runtimeToday: PREP_RUNTIME_TODAY,

            manifestPath,

            outputDir: MASS_IMPORT_OUTPUT_DIR,

            contractB: {

              id: contractBId,

              contractNumber,

              signingDate: PREP_B_SIGNING_DATE,

              contractTermEndDate: PREP_B_TERM_END,

              status: 'ACTIVE_IN_TERM',

            },

            customer: customerManifest,

            products: {

              stdId: productIdFromResponses(Responses.product[0]),

              rsId: productIdFromResponses(Responses.product[1]),

            },

            terms: {

              stdId: Responses.terms[0].id,

              rsId: Responses.terms[1].id,

            },

            pod: {

              identifier: podIdentifier,

              podId,

              podDetailId,

              activationOnB: PREP_POD_ACTIVATION,

            },

            nextStep:

              'node "tasks/mass import/phn-362/generate-resign-flow-imports.mjs" → cases/01…06/',

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


