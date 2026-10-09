/**
 * PDT-2815 — Mass SMS generated individual SMS must use customer communication
 * from the contract/mass context (customer v1), not the latest customer version (v3).
 *
 * TO-BE test: MASS_SMS_CONTRACT parse v1 → mass draft (import row only) → SEND; system spawns/sends
 * individual SMS (no manual single-SMS POST). Explicit customer v1 on SEND after contract v1 parse.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2815-mass-email-individual-customer-version.spec.ts
 * - tests/cursor/PDT-2815-customer-version-bug-repro.spec.ts (AS-IS defect without version)
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts
 */

import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  assertIndividualSmsSentStatus,
  assertNoManualSingleSmsCommunicationCreated,
  assertSystemSpawnedMassSmsChildCommunication,
  attachPdt2815CreatedDataLinks,
  buildMassEmailIndividualCustomerVersionScenario,
  createMassSmsDraft,
  pdt2815LinksFromProductFixture,
  resolveMassSmsCustomerPreview,
  updateMassSmsWithCustomers,
  uploadMassSmsContractParse,
  waitForSmsCommunicationSent,
  type MassEmailIndividualVersionScenario,
} from './pdt-2815-version-validity.fixtures';

function normalizePhone(value?: string): string {
  return String(value ?? '').replace(/\s/g, '');
}

function assertIndividualSmsUsesExpectedCustomerVersion(
  preview: {
    customerShortResponse?: {
      customerDetailVersionId?: number;
      customerVersion?: number;
    };
    customerCommunicationDataShortResponse?: { id?: number };
    phoneNumber?: string;
  },
  scenario: MassEmailIndividualVersionScenario,
  context: string,
): void {
  const expected = scenario.customerVersions.v1;
  const latest = scenario.customerVersions.v3;

  const versionId =
    preview.customerShortResponse?.customerDetailVersionId ??
    preview.customerShortResponse?.customerVersion;
  expect(versionId, `${context}: customer version must be present on SMS preview`).toBeTruthy();
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

  const previewPhone = normalizePhone(preview.phoneNumber);
  if (previewPhone && expected.mobileNumber) {
    expect(
      previewPhone,
      `${context}: recipient phone must come from customer v1 communication`,
    ).toContain(normalizePhone(expected.mobileNumber).slice(-7));
    if (latest.mobileNumber) {
      expect(
        previewPhone,
        `${context}: must not use latest customer v3 mobile number`,
      ).not.toContain(normalizePhone(latest.mobileNumber).slice(-7));
    }
  }
}

test.describe('[PDT-2815]: Mass SMS individual customer version', { tag: '@customerComm' }, () => {
  test('[PDT-2815] TC: Mass SMS contract v1 — individual SMS uses customer v1 communication (not latest v3)', async ({
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

    let massSmsId: number;
    let importRowProductDetailId: number;

    await test.step('Precondition: MASS_SMS_CONTRACT parse — contract logical version 1 only', async () => {
      const importBody = await uploadMassSmsContractParse(Request, FileUploadRequest, Endpoints, [
        { contractNumber: scenario.contractNumber, version: '1' },
      ]);
      const row = (importBody.results ?? []).find(
        (r) => r.customerIdentifier === scenario.customerIdentifier,
      );
      expect(row, 'SMS contract parse must accept explicit version 1 row').toBeTruthy();
      importRowProductDetailId = row!.productContractDetailId!;
      expect(importRowProductDetailId, 'parse must resolve SIGNED product contract detail for v1').toBe(
        scenario.signedV1DetailId,
      );
    });

    await test.step('Precondition: create mass SMS — recipient only from contract v1 parse', async () => {
      massSmsId = await createMassSmsDraft(
        Request,
        Endpoints,
        Responses,
        `PDT-2815 mass SMS contract v1 ${Date.now()} — system spawns individual on SEND`,
        [
          {
            customerIdentifier: scenario.customerIdentifier,
            productContractDetailId: importRowProductDetailId,
          },
        ],
      );
    });

    await test.step('Action: SEND mass SMS — customers from contract v1 parse only', async () => {
      await updateMassSmsWithCustomers(
        Request,
        Endpoints,
        massSmsId,
        [
          {
            customerIdentifier: scenario.customerIdentifier,
            version: scenario.customerVersions.v1.logicalVersion,
            productContractDetailId: importRowProductDetailId,
          },
        ],
        'SEND',
      );
      assertNoManualSingleSmsCommunicationCreated(Responses);
    });

    await test.step('Assert sent individual SMS uses customer v1 communication', async () => {
      const { previewId } = await resolveMassSmsCustomerPreview(
        Request,
        Endpoints,
        massSmsId,
        scenario.customerIdentifier,
      );

      assertSystemSpawnedMassSmsChildCommunication(
        massSmsId,
        previewId,
        'individual SMS after mass SEND',
      );

      const preview = await waitForSmsCommunicationSent(
        Request,
        Endpoints,
        previewId,
        'system-generated individual SMS after mass SEND',
      );

      assertIndividualSmsSentStatus(
        preview,
        'individual SMS after mass SMS contract v1 parse and SEND',
      );

      assertIndividualSmsUsesExpectedCustomerVersion(
        preview,
        scenario,
        'sent individual SMS after mass SMS contract v1 import',
      );

      await test.info().attach('[PDT-2815] individual SMS version evidence', {
        body: JSON.stringify(
          {
            massSmsId,
            previewId,
            importRowProductDetailId,
            expectedCustomerV1: scenario.customerVersions.v1,
            latestCustomerV3: scenario.customerVersions.v3,
            preview: {
              customerDetailVersionId:
                preview.customerShortResponse?.customerDetailVersionId ??
                preview.customerShortResponse?.customerVersion,
              communicationDataId: preview.customerCommunicationDataShortResponse?.id,
              phoneNumber: preview.phoneNumber,
              communicationStatus: preview.communicationStatus,
            },
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, {
      ...pdt2815LinksFromProductFixture('TC-mass-sms-individual-customer-version', scenario),
      massSmsId,
      note: `v1Comm=${scenario.customerVersions.v1.communicationDataId}; v3Comm=${scenario.customerVersions.v3.communicationDataId}`,
    });

    test.info().attach('[PDT-2815] response snapshot', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });
});
