/**

 * PDT-2815 — Mass Email generated individual email must use customer communication

 * from the contract/mass context (customer v1), not the latest customer version (v3).

 *

 * TO-BE test: contract import v1 → mass draft (import row only) → SEND; system spawns/sends per-customer
 * communication (no manual single-email POST). Explicit customer v1 on SEND after contract v1 import.

 *

 * Reference spec(s):

 * - tests/cursor/PDT-2815-customer-version-bug-repro.spec.ts (AS-IS defect without version)

 * - tests/cursor/PDT-2815-version-validity-three-processes.spec.ts (mass email import patterns)

 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (contract + mass email helpers)

 */



import { test, expect } from '../../fixtures/baseFixture';

import reportGenerator from '../../utils/generateReport';

import {

  assertIndividualEmailSentStatus,

  assertNoManualSingleEmailCommunicationCreated,

  assertSystemSpawnedMassEmailChildCommunication,

  attachPdt2815CreatedDataLinks,

  buildMassEmailIndividualCustomerVersionScenario,

  createMassEmailDraft,

  pdt2815LinksFromProductFixture,

  updateMassEmailWithCustomers,

  uploadMassEmailContractImport,

  resolveMassEmailCustomerPreview,

  waitForEmailCommunicationSent,

  type MassEmailIndividualVersionScenario,

} from './pdt-2815-version-validity.fixtures';



function normalizeEmail(value?: string): string {

  return String(value ?? '')

    .trim()

    .toLowerCase();

}



function assertIndividualEmailUsesExpectedCustomerVersion(

  preview: {

    customerShortResponse?: { customerDetailVersionId?: number };

    customerCommunicationDataShortResponse?: { id?: number };

    customerEmailAddress?: string;

  },

  scenario: MassEmailIndividualVersionScenario,

  context: string,

): void {

  const expected = scenario.customerVersions.v1;

  const latest = scenario.customerVersions.v3;



  const versionId = preview.customerShortResponse?.customerDetailVersionId;

  expect(versionId, `${context}: customerDetailVersionId must be present on email preview`).toBeTruthy();

  expect(

    versionId,

    `${context}: must use customer logical version 1 from contract v1 context, not latest v3`,

  ).toBe(expected.customerDetailVersionId);

  expect(versionId, `${context}: must not resolve to latest customer version 3`).not.toBe(

    latest.customerDetailVersionId,

  );



  const commId = preview.customerCommunicationDataShortResponse?.id;

  if (commId != null && expected.communicationDataId > 0) {

    expect(

      commId,

      `${context}: customer communication data must match customer v1, not v3`,

    ).toBe(expected.communicationDataId);

    expect(commId, `${context}: must not use customer v3 communication id`).not.toBe(

      latest.communicationDataId,

    );

  }



  const previewEmail = normalizeEmail(preview.customerEmailAddress);

  if (previewEmail) {

    expect(

      previewEmail,

      `${context}: recipient email must come from customer v1 communication`,

    ).toContain(normalizeEmail(expected.emailAddress).split('@')[0]);

    expect(

      previewEmail,

      `${context}: must not use latest customer v3 email address`,

    ).not.toContain(normalizeEmail(latest.emailAddress).split('@')[0]);

  }

}



test.describe('[PDT-2815]: Mass Email individual customer version', { tag: '@customerComm' }, () => {

  test('[PDT-2815] TC: Mass Email contract v1 — individual email uses customer v1 communication (not latest v3)', async ({

    Request,

    GeneratePayload,

    Responses,

    Endpoints,

    FileUploadRequest,

  }) => {

    test.setTimeout(20 * 60 * 1000);



    const scenario = await test.step(

      'Precondition: customer v1/v2/v3 + contract v1 SIGNED + contract v2 CANCELLED',

      async () =>

        buildMassEmailIndividualCustomerVersionScenario(

          Request,

          GeneratePayload,

          Responses,

          Endpoints,

        ),

    );



    let massEmailId: number;

    let importRowProductDetailId: number;



    await test.step('Precondition: MASS_IMPORT_OF_CONTRACTS — contract logical version 1 only', async () => {

      const importBody = await uploadMassEmailContractImport(Request, FileUploadRequest, Endpoints, [

        { contractNumber: scenario.contractNumber, version: '1' },

      ]);

      const row = (importBody.results ?? []).find(

        (r) => r.customerIdentifier === scenario.customerIdentifier,

      );

      expect(row, 'contract import must accept explicit version 1 row').toBeTruthy();

      importRowProductDetailId = row!.productContractDetailId!;

      expect(importRowProductDetailId, 'import must resolve SIGNED product contract detail for v1').toBe(

        scenario.signedV1DetailId,

      );

    });



    await test.step('Precondition: create mass email — recipient only from contract v1 import', async () => {

      massEmailId = await createMassEmailDraft(

        Request,

        Endpoints,

        Responses,

        `[PDT-2815] mass email contract v1 ${Date.now()}`,

        'Per-customer email must be spawned by system on mass SEND (no manual single-email API).',

        [

          {

            customerIdentifier: scenario.customerIdentifier,

            productContractDetailId: importRowProductDetailId,

          },

        ],

      );

    });



    await test.step('Action: SEND mass email — customers from contract v1 import only', async () => {

      await updateMassEmailWithCustomers(

        Request,

        Endpoints,

        massEmailId,

        [

          {

            customerIdentifier: scenario.customerIdentifier,

            version: scenario.customerVersions.v1.logicalVersion,

            productContractDetailId: importRowProductDetailId,

          },

        ],

        'SEND',

      );

      assertNoManualSingleEmailCommunicationCreated(Responses);

    });



    await test.step('Assert sent individual email uses customer v1 communication', async () => {

      const { channel, previewId } = await resolveMassEmailCustomerPreview(

        Request,

        Endpoints,

        massEmailId,

        scenario.customerIdentifier,

      );



      assertSystemSpawnedMassEmailChildCommunication(

        massEmailId,

        previewId,

        channel,

        'per-customer communication after mass SEND',

      );



      const preview = await waitForEmailCommunicationSent(

        Request,

        Endpoints,

        previewId,

        channel,

        'system-generated per-customer email after mass SEND',

      );



      assertIndividualEmailSentStatus(

        preview,

        'individual email after mass email contract v1 import and SEND',

      );



      assertIndividualEmailUsesExpectedCustomerVersion(

        preview,

        scenario,

        'sent individual email after mass email contract v1 import',

      );



      await test.info().attach('[PDT-2815] individual email version evidence', {

        body: JSON.stringify(

          {

            massEmailId,

            previewId,

            importRowProductDetailId,

            expectedCustomerV1: scenario.customerVersions.v1,

            latestCustomerV3: scenario.customerVersions.v3,

            initialChannel: channel,

            preview: {

              customerDetailVersionId: preview.customerShortResponse?.customerDetailVersionId,

              communicationDataId: preview.customerCommunicationDataShortResponse?.id,

              customerEmailAddress: preview.customerEmailAddress,

              emailCommunicationStatus: preview.emailCommunicationStatus,

              sentDate: preview.sentDate,

              channel,

            },

          },

          null,

          2,

        ),

        contentType: 'application/json',

      });

    });



    attachPdt2815CreatedDataLinks(Responses, {

      ...pdt2815LinksFromProductFixture('TC-mass-email-individual-customer-version', scenario),

      massEmailId,

      note: `v1Comm=${scenario.customerVersions.v1.communicationDataId}; v3Comm=${scenario.customerVersions.v3.communicationDataId}`,

    });



    test.info().attach('[PDT-2815] response snapshot', {

      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),

      contentType: 'application/json',

    });

  });

});


