/**
 * PDT-2815 — AS-IS defect reproduction (fresh Dev data, no Kalina fixture IDs).
 *
 * These tests PASS while the backend bug is open: when customer `version` is omitted on
 * mass email/SMS save, Phoenix resolves `findLastCustomerDetail` (latest v3) instead of
 * contract-context customer v1.
 *
 * When the defect is fixed, these tests will FAIL — flip assertions to expected TO-BE behavior.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts
 * - tests/cursor/PDT-2815-mass-email-individual-customer-version.spec.ts (TO-BE with explicit version)
 */

import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  assertPdt2815UsesLatestCustomerVersionBug,
  attachPdt2815CreatedDataLinks,
  buildMassEmailIndividualCustomerVersionScenario,
  createMassEmailDraft,
  createMassSmsDraft,
  pdt2815LinksFromProductFixture,
  resolveMassEmailCustomerPreview,
  resolveMassSmsCustomerPreview,
  updateMassEmailWithCustomers,
  updateMassSmsWithCustomers,
  uploadMassEmailContractImport,
  uploadMassSmsContractParse,
} from './pdt-2815-version-validity.fixtures';

test.describe('[PDT-2815]: Customer version defect reproduction (AS-IS)', { tag: '@customerComm' }, () => {
  test('[PDT-2815] TC-BUG-REPRO-1: Mass Email contract v1 import — omit customer version → individual email uses latest customer v3', async ({
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

    await test.step('Precondition: create mass email draft', async () => {
      massEmailId = await createMassEmailDraft(
        Request,
        Endpoints,
        Responses,
        `[PDT-2815] bug repro mass email ${Date.now()}`,
        'AS-IS defect probe — customer version omitted on save.',
        [{ customerIdentifier: scenario.customerIdentifier, version: scenario.customerVersions.v1.logicalVersion }],
      );
    });

    await test.step('Precondition: import contract with explicit version 1', async () => {
      const importBody = await uploadMassEmailContractImport(Request, FileUploadRequest, Endpoints, [
        { contractNumber: scenario.contractNumber, version: '1' },
      ]);
      const row = (importBody.results ?? []).find(
        (r) => r.customerIdentifier === scenario.customerIdentifier,
      );
      expect(row, 'mass import must accept contract version 1 row').toBeTruthy();
      importRowProductDetailId = row!.productContractDetailId!;
      expect(importRowProductDetailId).toBe(scenario.signedV1DetailId);
    });

    await test.step('Action: SEND mass email WITHOUT customer version (bug path)', async () => {
      await updateMassEmailWithCustomers(
        Request,
        Endpoints,
        massEmailId,
        [
          {
            customerIdentifier: scenario.customerIdentifier,
            productContractDetailId: importRowProductDetailId,
          },
        ],
        'SEND',
      );
    });

    await test.step('Assert AS-IS defect: sent individual email uses latest customer v3 communication', async () => {
      const { preview, channel, previewId } = await resolveMassEmailCustomerPreview(
        Request,
        Endpoints,
        massEmailId,
        scenario.customerIdentifier,
      );

      assertPdt2815UsesLatestCustomerVersionBug(
        preview,
        scenario,
        'mass email after contract v1 import without customer version on save',
      );

      await test.info().attach('[PDT-2815] bug repro mass email evidence', {
        body: JSON.stringify(
          {
            massEmailId,
            previewId,
            channel,
            expectedCustomerV1: scenario.customerVersions.v1,
            latestCustomerV3: scenario.customerVersions.v3,
            preview: {
              customerDetailVersionId: preview.customerShortResponse?.customerDetailVersionId,
              communicationDataId: preview.customerCommunicationDataShortResponse?.id,
              customerEmailAddress: preview.customerEmailAddress,
            },
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, {
      ...pdt2815LinksFromProductFixture('TC-BUG-REPRO-1-mass-email-no-version', scenario),
      massEmailId,
      note: `AS-IS bug repro; v1Comm=${scenario.customerVersions.v1.communicationDataId}; v3Comm=${scenario.customerVersions.v3.communicationDataId}`,
    });

    test.info().attach('[PDT-2815] response snapshot', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  test('[PDT-2815] TC-BUG-REPRO-2: Mass SMS contract import without version — omit customer version → individual SMS uses latest customer v3', async ({
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

    let importRowProductDetailId: number;

    await test.step('Precondition: parse SMS contract import without version column', async () => {
      const importBody = await uploadMassSmsContractParse(Request, FileUploadRequest, Endpoints, [
        { contractNumber: scenario.contractNumber },
      ]);
      const row = (importBody.results ?? []).find(
        (r) => r.customerIdentifier === scenario.customerIdentifier,
      );
      expect(row, 'SMS parse must resolve contract row').toBeTruthy();
      importRowProductDetailId = row!.productContractDetailId!;
      expect(importRowProductDetailId).toBe(scenario.signedV1DetailId);
    });

    let massSmsId: number;

    await test.step('Precondition: create mass SMS draft', async () => {
      massSmsId = await createMassSmsDraft(
        Request,
        Endpoints,
        Responses,
        `PDT-2815 bug repro SMS ${Date.now()}`,
        [{ customerIdentifier: scenario.customerIdentifier }],
      );
    });

    await test.step('Action: save mass SMS WITHOUT customer version (bug path; SEND so individual preview exposes comm data)', async () => {
      await updateMassSmsWithCustomers(
        Request,
        Endpoints,
        massSmsId,
        [
          {
            customerIdentifier: scenario.customerIdentifier,
            productContractDetailId: importRowProductDetailId,
          },
        ],
        'SEND',
      );
    });

    await test.step('Assert AS-IS defect: individual SMS preview uses latest customer v3 communication', async () => {
      const { preview, previewId } = await resolveMassSmsCustomerPreview(
        Request,
        Endpoints,
        massSmsId,
        scenario.customerIdentifier,
      );

      assertPdt2815UsesLatestCustomerVersionBug(
        preview,
        scenario,
        'mass SMS after contract import without customer version on save',
      );

      await test.info().attach('[PDT-2815] bug repro mass SMS evidence', {
        body: JSON.stringify(
          {
            massSmsId,
            previewId,
            expectedCustomerV1: scenario.customerVersions.v1,
            latestCustomerV3: scenario.customerVersions.v3,
            preview: {
              customerDetailVersionId:
                preview.customerShortResponse?.customerDetailVersionId ??
                preview.customerShortResponse?.customerVersion,
              customerDetailId: preview.customerShortResponse?.customerDetailId,
              communicationDataId: preview.customerCommunicationDataShortResponse?.id,
              phoneNumber: preview.phoneNumber,
            },
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, {
      ...pdt2815LinksFromProductFixture('TC-BUG-REPRO-2-mass-sms-no-version', scenario),
      note: `AS-IS bug repro massSms; v1Comm=${scenario.customerVersions.v1.communicationDataId}; v3Comm=${scenario.customerVersions.v3.communicationDataId}`,
    });

    test.info().attach('[PDT-2815] response snapshot', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });
});
