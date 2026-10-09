/**
 * PDT-2815 — Contract version validity: Mass Email, Mass SMS, Penalty.
 * Test cases: Cursor-Project/test_cases/Backend/Version_Validity_Three_Processes_PDT_2815.md
 *
 * Fixtures: tests/cursor/pdt-2815-version-validity.fixtures.ts
 */

import { test, expect } from '../../fixtures/baseFixture';
import {
  attachPdt2815CreatedDataLinks,
  pdt2815LinksFromPenaltyFixture,
  pdt2815LinksFromProductFixture,
  pdt2815LinksFromServiceFixture,
  ANCHOR_DATE,
  ERR_DUPLICATE_PRICE_COMPONENT,
  ERR_EXPLICIT_NOT_SIGNED,
  ERR_NO_SIGNED_VERSION,
  ERR_NO_VALID_PRICE_COMPONENT,
  ERR_RESPECTIVE_VERSION_NOT_FOUND,
  addDaysIso,
  assertMassImportShowsError,
  assertMassCommImportRejected,
  assertNoSignedProductContractVersions,
  assertPenaltyCalculated,
  assertPenaltyMessagesContain,
  assertPenaltyNotCalculated,
  assertProductContractDetailResolution,
  assertProductContractVersionStatus,
  ensureOnlySignedV2ResolvesAtAnchor,
  loadProductContract,
  phoenixProductContractDetailIdByExecutionDate,
  signedVersionExecutionDate,
  buildAllCustomersProbeCustomers,
  buildHappyPathSignedV1AndV2,
  buildPenaltyDuplicateTagOnSignedProduct,
  buildPenaltyGapSignedV2V3,
  buildPenaltyMissingTagOnSignedProduct,
  buildPenaltyStableSignedV2,
  buildServiceContractDraftOnly,
  buildServiceContractSignedV1WithOptionalDraftV2,
  buildSignedV1Only,
  calculatePenalty,
  cancelAllSignedVersions,
  createMassEmailDraftAllCustomers,
  extendWithDraftV2Only,
  extendWithDraftVersion3,
  extendWithSignedVersion3AfterAnchor,
  penaltyAmountFingerprint,
  penaltyNumericAmount,
  preconditionSharedEntities,
  PDT2815_CONSUMPTION_KWH_V2,
  resolvePenaltyPrerequisites,
  activatePodOnContract,
  uploadMassEmailContractImport,
  uploadMassSmsContractParse,
  viewMassEmailCustomerIdentifiers,
  type MassEmailImportBody,
  type PenaltyCalcJson,
} from './pdt-2815-version-validity.fixtures';

test.describe('[PDT-2815]: Contract version validity — Mass Email, Mass SMS & Penalty', { tag: '@customerComm' }, () => {
  test('[PDT-2815] TC-BE-1: Mass Email contract import — explicit SIGNED product version is accepted', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(15 * 60 * 1000);

    const fixture = await test.step('Precondition: shared entities + happy path v1/v2 SIGNED', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildHappyPathSignedV1AndV2(Request, GeneratePayload, Responses, Endpoints);
    });

    let importBody: MassEmailImportBody;

    await test.step('POST email-communication/customers-import — explicit version 2', async () => {
      importBody = await uploadMassEmailContractImport(Request, FileUploadRequest, Endpoints, [
        { contractNumber: fixture.contractNumber, version: '2' },
      ]);
    });

    await test.step('Expected: row accepted for SIGNED version 2', async () => {
      const popup = importBody.popupMessage ?? '';
      expect(popup).not.toContain(ERR_EXPLICIT_NOT_SIGNED);
      const results = importBody.results ?? [];
      const match = results.find((r) => r.customerIdentifier === fixture.customerIdentifier);
      expect(match, 'processed row must include customer identifier').toBeTruthy();
      if (fixture.signedV2DetailId != null && match?.productContractDetailId != null) {
        expect(match.productContractDetailId).toBe(fixture.signedV2DetailId);
      }
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromProductFixture('TC-BE-1', fixture));
  });

  test('[PDT-2815] TC-BE-2: Mass Email contract import — explicit DRAFT version 2 is rejected with documented message', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(15 * 60 * 1000);

    const fixture = await test.step('Precondition: v1 SIGNED + v2 DRAFT', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      const f = await extendWithDraftV2Only(Request, GeneratePayload, Responses, Endpoints);
      await assertProductContractVersionStatus(Request, f.contractId, 1, 'SIGNED');
      await assertProductContractVersionStatus(Request, f.contractId, 2, 'DRAFT');
      return f;
    });

    let importBody: MassEmailImportBody;

    await test.step('POST customers-import — explicit version 2 (DRAFT)', async () => {
      importBody = await uploadMassEmailContractImport(Request, FileUploadRequest, Endpoints, [
        { contractNumber: fixture.contractNumber, version: '2' },
      ]);
    });

    await test.step('Expected: business-level rejection (popupMessage)', async () => {
      assertMassImportShowsError(
        importBody,
        [ERR_EXPLICIT_NOT_SIGNED, fixture.contractNumber, 'version 2'],
        'TC-BE-2 explicit DRAFT v2',
      );
      const results = importBody.results ?? [];
      const accepted = results.some((r) => r.customerIdentifier === fixture.customerIdentifier);
      expect(accepted, 'DRAFT v2 must not appear in processed results').toBeFalsy();
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromProductFixture('TC-BE-2', fixture));
  });

  test('[PDT-2815] TC-BE-3: Mass Email contract import — latest fallback selects latest SIGNED, not newer DRAFT', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(15 * 60 * 1000);

    const { fixture, draftV3DetailId } = await test.step(
      'Precondition: v2 SIGNED + v3 DRAFT (newer startDate)',
      async () => {
        await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
        const base = await buildHappyPathSignedV1AndV2(Request, GeneratePayload, Responses, Endpoints);
        return extendWithDraftVersion3(base, Request, Endpoints);
      },
    );

    let importBody: MassEmailImportBody;

    await test.step('POST customers-import — contract number only (no version column)', async () => {
      importBody = await uploadMassEmailContractImport(Request, FileUploadRequest, Endpoints, [
        { contractNumber: fixture.contractNumber },
      ]);
    });

    await test.step('Expected: resolves version 2 SIGNED detail, not version 3 DRAFT', async () => {
      const popup = importBody.popupMessage ?? '';
      expect(popup).not.toContain(ERR_NO_SIGNED_VERSION);
      const results = importBody.results ?? [];
      expect(results.length).toBeGreaterThan(0);
      const detailId = results[0]?.productContractDetailId;
      expect(detailId).toBe(fixture.signedV2DetailId);
      expect(detailId).not.toBe(draftV3DetailId);
    });

    attachPdt2815CreatedDataLinks(
      Responses,
      pdt2815LinksFromProductFixture('TC-BE-3', fixture, { note: `draftV3DetailId=${draftV3DetailId}` }),
    );
  });

  test('[PDT-2815] TC-BE-4: Mass Email contract import — no SIGNED version yields clear failure', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(15 * 60 * 1000);

    const fixture = await test.step('Precondition: v1 SIGNED then version CANCELLED', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      const base = await buildSignedV1Only(Request, GeneratePayload, Responses, Endpoints);
      return cancelAllSignedVersions(base, Request, Endpoints);
    });

    await test.step('Precondition delta: assert no SIGNED versions remain', async () => {
      await assertNoSignedProductContractVersions(Request, fixture.contractId);
    });

    let importBody: MassEmailImportBody;

    await test.step('POST customers-import — no version column', async () => {
      importBody = await uploadMassEmailContractImport(Request, FileUploadRequest, Endpoints, [
        { contractNumber: fixture.contractNumber },
      ]);
    });

    await test.step('Expected: no eligible SIGNED version message', async () => {
      assertMassImportShowsError(
        importBody,
        [ERR_NO_SIGNED_VERSION, fixture.contractNumber],
        'TC-BE-4 no SIGNED version',
      );
      const results = importBody.results ?? [];
      const accepted = results.some((r) => r.customerIdentifier === fixture.customerIdentifier);
      expect(accepted, 'contract without SIGNED version must not appear in processed results').toBeFalsy();
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromProductFixture('TC-BE-4', fixture));
  });

  test('[PDT-2815] TC-BE-5: Mass SMS contract parse — explicit non-SIGNED service version rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(20 * 60 * 1000);

    const fixture = await test.step('Precondition: service v1 SIGNED + v2 DRAFT', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildServiceContractSignedV1WithOptionalDraftV2(
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        true,
      );
    });

    let importBody: MassEmailImportBody;

    await test.step('POST sms-communication/parse — explicit version 2 (DRAFT)', async () => {
      importBody = await uploadMassSmsContractParse(Request, FileUploadRequest, Endpoints, [
        { contractNumber: fixture.contractNumber, version: '2' },
      ]);
    });

    await test.step('Expected: rejection mirrors Mass Email semantics', async () => {
      assertMassCommImportRejected(
        importBody,
        [ERR_EXPLICIT_NOT_SIGNED, fixture.contractNumber, 'version 2'],
        'TC-BE-5 SMS explicit DRAFT v2',
      );
      const results = importBody.results ?? [];
      const accepted = results.some((r) => r.customerIdentifier === fixture.customerIdentifier);
      expect(accepted).toBeFalsy();
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromServiceFixture('TC-BE-5', fixture));
  });

  test('[PDT-2815] TC-BE-6: Mass SMS contract parse — latest SIGNED service version when version omitted', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(25 * 60 * 1000);

    const fixture = await test.step('Precondition: service v1 SIGNED + v2 DRAFT', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildServiceContractSignedV1WithOptionalDraftV2(
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        true,
      );
    });

    let importBody: MassEmailImportBody;

    await test.step('POST sms-communication/parse — contract number only', async () => {
      importBody = await uploadMassSmsContractParse(Request, FileUploadRequest, Endpoints, [
        { contractNumber: fixture.contractNumber },
      ]);
    });

    await test.step('Expected: latest SIGNED v1 detail, not DRAFT v2', async () => {
      const popup = importBody.popupMessage ?? '';
      expect(popup).not.toContain(ERR_NO_SIGNED_VERSION);
      const results = importBody.results ?? [];
      expect(results.length).toBeGreaterThan(0);
      const detailId = results[0]?.serviceContractDetailId;
      expect(detailId).toBe(fixture.signedV1DetailId);
      if (fixture.draftV2DetailId != null) {
        expect(detailId).not.toBe(fixture.draftV2DetailId);
      }
    });

    attachPdt2815CreatedDataLinks(
      Responses,
      pdt2815LinksFromServiceFixture('TC-BE-6', fixture, {
        note: `signedV1DetailId=${fixture.signedV1DetailId}; draftV2DetailId=${fixture.draftV2DetailId}`,
      }),
    );
  });

  test('[PDT-2815] TC-BE-7: Penalty — POD on future contract version (description vs code gap)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);

    const gapFixture = await test.step('Precondition: penalty gap contract (v2/v3 SIGNED, future POD)', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildPenaltyGapSignedV2V3(Request, GeneratePayload, Responses, Endpoints);
    });

    let calcJson: PenaltyCalcJson;

    await test.step('GET actions/calculate-penalty-amount — POD active only on future version', async () => {
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: gapFixture.actionTypeId,
        executionDate: ANCHOR_DATE,
        penaltyId: gapFixture.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: gapFixture.customerId,
        contractId: gapFixture.contractId,
        contractType: 'PRODUCT_CONTRACT',
        terminationId: gapFixture.terminationId,
        pods: [gapFixture.podFutureOnlyId],
      });
      await expect(response).CheckResponse();
      calcJson = body;
    });

    await test.step('Expected (Story 3 TO-BE): future-only POD must not produce penalty amount', async () => {
      const messages = (calcJson.infoErrorMessages ?? []).join(' ');
      await test.info().attach('[PDT-2815] TC-BE-7 gap probe — outcome', {
        body: JSON.stringify(
          {
            executionDate: ANCHOR_DATE,
            respectiveVersionDetailId: gapFixture.respectiveDetailId,
            futureVersionDetailId: gapFixture.futureDetailId,
            podRequested: gapFixture.podFutureOnlyId,
            infoErrorMessages: calcJson.infoErrorMessages ?? [],
            amount: calcJson.amount ?? null,
            calculated: isPenaltyCalculated(calcJson),
            hasFutureVersionWording: /future version/i.test(messages),
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
      assertPenaltyNotCalculated(calcJson, 'TC-BE-7 future-only POD at anchor executionDate');

      await assertProductContractDetailResolution(Request, {
        contractId: gapFixture.contractId,
        executionDate: ANCHOR_DATE,
        expectedLogicalVersionId: 2,
        expectedDetailId: gapFixture.respectiveDetailId,
        context: 'TC-BE-7 anchor uses respective v2 detail (POD scope is separate)',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromPenaltyFixture('TC-BE-7', gapFixture));
  });

  test('[PDT-2815] TC-BE-8: Penalty — no SIGNED version covering execution date returns validation failure', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(15 * 60 * 1000);

    const fixture = await test.step('Precondition: product contract with no SIGNED versions', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      const base = await buildSignedV1Only(Request, GeneratePayload, Responses, Endpoints);
      return cancelAllSignedVersions(base, Request, Endpoints);
    });

    const penaltyPrereq = await test.step('Precondition: penalty prerequisites', async () =>
      resolvePenaltyPrerequisites(Request, GeneratePayload, Responses, Endpoints),
    );

    await test.step('GET calculate-penalty-amount — executionDate with no SIGNED version', async () => {
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: penaltyPrereq.actionTypeId,
        executionDate: ANCHOR_DATE,
        penaltyId: penaltyPrereq.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: fixture.customerId,
        contractId: fixture.contractId,
        contractType: 'PRODUCT_CONTRACT',
        terminationId: penaltyPrereq.terminationId,
      });

      expect(JSON.stringify(body)).toContain(ERR_RESPECTIVE_VERSION_NOT_FOUND);
      const calculated = body.notEmpty === true || (body.amount != null && body.amount > 0);
      expect(calculated, 'penalty must not calculate without SIGNED version').toBeFalsy();
      expect(response.ok(), 'expected business/validation failure when no SIGNED version').toBeFalsy();

      await assertProductContractDetailResolution(Request, {
        contractId: fixture.contractId,
        executionDate: ANCHOR_DATE,
        expectedLogicalVersionId: null,
        expectedDetailId: null,
        context: 'TC-BE-8 no SIGNED version — Phoenix mirror must be null',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromProductFixture('TC-BE-8', fixture));
  });

  test('[PDT-2815] TC-BE-9: Penalty — execution date resolves SIGNED product version (happy path)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);

    const fixture = await test.step('Precondition: v2 SIGNED + POD + penalty prereqs', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints, {
        podYearlyConsumptionKwh: PDT2815_CONSUMPTION_KWH_V2,
      });
      return buildPenaltyStableSignedV2(Request, GeneratePayload, Responses, Endpoints);
    });

    await test.step('GET calculate-penalty-amount — executionDate inside SIGNED v2 window', async () => {
      const execDate = fixture.v2ExecutionDate;
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: fixture.actionTypeId,
        executionDate: execDate,
        penaltyId: fixture.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: fixture.customerId,
        contractId: fixture.contractId,
        contractType: 'PRODUCT_CONTRACT',
        terminationId: fixture.terminationId,
      });
      await expect(response).CheckResponse();
      const errors = (body.infoErrorMessages ?? []).join(' ');
      expect(errors).not.toContain(ERR_RESPECTIVE_VERSION_NOT_FOUND);
      assertPenaltyCalculated(body, 'TC-BE-9 SIGNED v2 at v2ExecutionDate');
      const amountV2 = penaltyNumericAmount(body);
      expect(amountV2, 'v2 penalty amount must be calculated').not.toBeNull();
      expect(amountV2!).toBeGreaterThan(0);

      const resolution = await assertProductContractDetailResolution(Request, {
        contractId: fixture.contractId,
        executionDate: execDate,
        expectedLogicalVersionId: 2,
        expectedDetailId: fixture.respectiveDetailId,
        context: 'TC-BE-9 penalty must resolve SIGNED v2 detail (triple-match evidence)',
      });
      expect(resolution.tripleMatch).toBeTruthy();

      await test.info().attach('[PDT-2815] TC-BE-9 penalty outcome', {
        body: JSON.stringify(
          {
            anchorDate: ANCHOR_DATE,
            executionDate: execDate,
            expectedRespectiveDetailId: fixture.respectiveDetailId,
            consumptionKwhV2: fixture.consumptionKwhV2,
            penaltyAmountV2: amountV2,
            amount: body.amount ?? null,
            notEmpty: body.notEmpty ?? null,
            infoErrorMessages: body.infoErrorMessages ?? [],
            resolution,
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromPenaltyFixture('TC-BE-9', fixture));
  });

  test('[PDT-2815] TC-BE-10: Penalty — zero price components for tag yields documented info error', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);

    const fixture = await test.step('Precondition: signed product without tag used in penalty formula', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildPenaltyMissingTagOnSignedProduct(Request, GeneratePayload, Responses, Endpoints);
    });

    await test.step('GET calculate-penalty-amount — missing tag on product detail', async () => {
      const execDate = fixture.v2ExecutionDate;
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: fixture.actionTypeId,
        executionDate: execDate,
        penaltyId: fixture.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: fixture.customerId,
        contractId: fixture.contractId,
        contractType: 'PRODUCT_CONTRACT',
        terminationId: fixture.terminationId,
        pods: [fixture.podRespectiveId],
      });
      await expect(response).CheckResponse();
      assertPenaltyMessagesContain(body, [ERR_NO_VALID_PRICE_COMPONENT], 'TC-BE-10');
      assertPenaltyNotCalculated(body, 'TC-BE-10 missing tag');

      await assertProductContractDetailResolution(Request, {
        contractId: fixture.contractId,
        executionDate: execDate,
        expectedLogicalVersionId: 2,
        expectedDetailId: fixture.respectiveDetailId,
        context: 'TC-BE-10 tag error still on v2 detail at v2ExecutionDate',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromPenaltyFixture('TC-BE-10', fixture));
  });

  test('[PDT-2815] TC-BE-11: Penalty — duplicate price component tags for same tag', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(25 * 60 * 1000);

    const fixture = await test.step('Precondition: two per-piece PCs with same contract template tag', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildPenaltyDuplicateTagOnSignedProduct(Request, GeneratePayload, Responses, Endpoints);
    });

    await test.step('GET calculate-penalty-amount — duplicate tag must block calculation', async () => {
      const execDate = fixture.v2ExecutionDate;
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: fixture.actionTypeId,
        executionDate: execDate,
        penaltyId: fixture.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: fixture.customerId,
        contractId: fixture.contractId,
        contractType: 'PRODUCT_CONTRACT',
        terminationId: fixture.terminationId,
        pods: [fixture.podRespectiveId],
      });
      await expect(response).CheckResponse();
      assertPenaltyMessagesContain(body, [ERR_DUPLICATE_PRICE_COMPONENT], 'TC-BE-11');
      assertPenaltyNotCalculated(body, 'TC-BE-11 duplicate tag');

      await assertProductContractDetailResolution(Request, {
        contractId: fixture.contractId,
        executionDate: execDate,
        expectedLogicalVersionId: 2,
        expectedDetailId: fixture.respectiveDetailId,
        context: 'TC-BE-11 duplicate tag on v2 detail at v2ExecutionDate',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromPenaltyFixture('TC-BE-11', fixture));
  });

  test('[PDT-2815] TC-BE-12: All-customers mass communication — eligible customers present in draft', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(30 * 60 * 1000);

    const probe = await test.step('Precondition: Customer A (signed product) + Customer B (signed service)', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildAllCustomersProbeCustomers(Request, GeneratePayload, Responses, Endpoints);
    });

    let identifiers: string[];
    let massEmailId: number;

    await test.step('POST email-communication/mass — allCustomersWithActiveContract DRAFT', async () => {
      massEmailId = await createMassEmailDraftAllCustomers(Request, Endpoints, Responses);
      identifiers = await viewMassEmailCustomerIdentifiers(Request, Endpoints, massEmailId);
    });

    await test.step('Expected: Customer B appears in mass-email customer list', async () => {
      expect(
        identifiers.some((id) => id === probe.customerBIdentifier),
        'signed service customer must be eligible for all-customers mode',
      ).toBeTruthy();
      await test.info().attach('[PDT-2815] TC-BE-12 all-customers snapshot', {
        body: JSON.stringify(
          {
            customerAIdentifier: probe.customerAIdentifier,
            customerBIdentifier: probe.customerBIdentifier,
            customerACount: identifiers.filter((id) => id === probe.customerAIdentifier).length,
            customerBCount: identifiers.filter((id) => id === probe.customerBIdentifier).length,
            totalRecipients: identifiers.length,
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, {
      testCase: 'TC-BE-12',
      customerIdentifier: probe.customerAIdentifier,
      customerId: probe.customerAId,
      productContractId: probe.productContractId,
      productContractNumber: probe.productContractNumber,
      serviceContractId: probe.serviceDraftContractId,
      serviceContractNumber: probe.serviceDraftContractNumber,
      massEmailId,
      note: `customerB=${probe.customerBIdentifier}; serviceSignedId=${probe.serviceSignedContractId}`,
    });
  });

  test('[PDT-2815] TC-BE-13: Cross-process consistency — Mass Email and Penalty use same contract detail', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(20 * 60 * 1000);

    let fixture = await test.step('Precondition: v1/v2 SIGNED product contract', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildHappyPathSignedV1AndV2(Request, GeneratePayload, Responses, Endpoints);
    });

    await test.step('Precondition: only SIGNED v2 covers penalty executionDate (Phoenix mirror)', async () => {
      const v2DetailId = await ensureOnlySignedV2ResolvesAtAnchor(Request, fixture.contractId);
      const body = await loadProductContract(Request, fixture.contractId);
      fixture = {
        ...fixture,
        signedV2DetailId: v2DetailId,
        v2ExecutionDate: signedVersionExecutionDate(body, 2),
      };
    });

    const penaltyPrereq = await test.step('Precondition: penalty prerequisites + POD on v2', async () => {
      await activatePodOnContract(
        Request,
        Responses,
        fixture.contractId,
        2,
        addDaysIso(ANCHOR_DATE, -15),
        0,
      );
      return resolvePenaltyPrerequisites(Request, GeneratePayload, Responses, Endpoints);
    });

    let emailDetailId: number | undefined;

    await test.step('Mass Email import without version — record productContractDetailId', async () => {
      const importBody = await uploadMassEmailContractImport(Request, FileUploadRequest, Endpoints, [
        { contractNumber: fixture.contractNumber },
      ]);
      const results = importBody.results ?? [];
      expect(results.length).toBeGreaterThan(0);
      emailDetailId = results[0]?.productContractDetailId;
      expect(emailDetailId).toBe(fixture.signedV2DetailId);

      await assertProductContractDetailResolution(Request, {
        contractId: fixture.contractId,
        executionDate: fixture.v2ExecutionDate,
        expectedLogicalVersionId: 2,
        expectedDetailId: fixture.signedV2DetailId,
        context: 'TC-BE-13 Mass Email import',
        massEmailResolvedDetailId: emailDetailId,
      });
    });

    await test.step('Penalty calculate on same contractId + executionDate', async () => {
      const execDate = fixture.v2ExecutionDate;
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: penaltyPrereq.actionTypeId,
        executionDate: execDate,
        penaltyId: penaltyPrereq.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: fixture.customerId,
        contractId: fixture.contractId,
        contractType: 'PRODUCT_CONTRACT',
        terminationId: penaltyPrereq.terminationId,
        pods: [Responses.pod[0].id],
      });
      await expect(response).CheckResponse();
      const penaltyErrors = (body.infoErrorMessages ?? []).join(' ');
      expect(penaltyErrors).not.toContain(ERR_RESPECTIVE_VERSION_NOT_FOUND);
      assertPenaltyCalculated(body, 'TC-BE-13 penalty on same date as mass email');

      const resolution = await assertProductContractDetailResolution(Request, {
        contractId: fixture.contractId,
        executionDate: execDate,
        expectedLogicalVersionId: 2,
        expectedDetailId: fixture.signedV2DetailId,
        context: 'TC-BE-13 penalty calculate',
        massEmailResolvedDetailId: emailDetailId,
      });
      expect(resolution.tripleMatch).toBeTruthy();

      await test.info().attach('[PDT-2815] TC-BE-13 cross-process', {
        body: JSON.stringify(
          {
            massEmailResolvedDetailId: emailDetailId,
            expectedSignedV2DetailId: fixture.signedV2DetailId,
            phoenixMirrorDetailId: resolution.phoenixMirrorDetailId,
            resolvedLogicalVersionId: resolution.resolvedLogicalVersionId,
            penaltyAmount: body.amount ?? null,
            penaltyErrors: body.infoErrorMessages ?? [],
            penaltyCalculated: isPenaltyCalculated(body),
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, pdt2815LinksFromProductFixture('TC-BE-13', fixture));
  });

  test.skip('[PDT-2815] TC-BE-14: Gap probe — header Signed by both sides vs version SIGNED', async () => {
    test.info().attach('[PDT-2815] TC-BE-14 skip reason', {
      body:
        'Constructing version SIGNED with header not SIGNED_BY_BOTH_SIDES is environment-specific. Run manually per Backend TC-BE-14.',
      contentType: 'text/plain',
    });
  });

  test('[PDT-2815] TC-BE-15: Penalty — service contract with no SIGNED version on execution date is rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(25 * 60 * 1000);

    const serviceFixture = await test.step('Precondition: service contract DRAFT only', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildServiceContractDraftOnly(Request, GeneratePayload, Responses, Endpoints);
    });

    const penaltyPrereq = await test.step('Precondition: penalty definition', async () =>
      resolvePenaltyPrerequisites(Request, GeneratePayload, Responses, Endpoints),
    );

    await test.step('GET calculate-penalty-amount — SERVICE_CONTRACT, DRAFT-only', async () => {
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: penaltyPrereq.actionTypeId,
        executionDate: ANCHOR_DATE,
        penaltyId: penaltyPrereq.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: serviceFixture.customerId,
        contractId: serviceFixture.contractId,
        contractType: 'SERVICE_CONTRACT',
        terminationId: penaltyPrereq.terminationId,
      });

      const msg = JSON.stringify(body);
      const versionRejected =
        msg.includes(ERR_RESPECTIVE_VERSION_NOT_FOUND) ||
        msg.includes('No contracts found') ||
        msg.includes('Customer does not match');
      expect(versionRejected, 'SERVICE_CONTRACT DRAFT-only must be rejected at executionDate').toBe(
        true,
      );
      const calculated = body.notEmpty === true || (body.amount != null && body.amount > 0);
      expect(calculated).toBeFalsy();
      expect(response.ok(), 'SERVICE_CONTRACT DRAFT-only must not return successful penalty').toBeFalsy();
    });

    attachPdt2815CreatedDataLinks(
      Responses,
      pdt2815LinksFromServiceFixture('TC-BE-15', serviceFixture, {
        note: 'SERVICE_CONTRACT penalty — DRAFT-only service versions',
      }),
    );
  });

  test('[PDT-2815] TC-BE-16: Penalty — recalculation unchanged executionDate keeps v2 context after SIGNED v3', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);

    let fixture = await test.step('Precondition: v1/v2 SIGNED product contract (TC test data 1–7)', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildHappyPathSignedV1AndV2(Request, GeneratePayload, Responses, Endpoints);
    });

    const penaltyPrereq = await test.step('Precondition: penalty + POD on v2', async () => {
      const v2DetailId = await ensureOnlySignedV2ResolvesAtAnchor(Request, fixture.contractId);
      const body = await loadProductContract(Request, fixture.contractId);
      fixture = {
        ...fixture,
        signedV2DetailId: v2DetailId,
        v2ExecutionDate: signedVersionExecutionDate(body, 2),
      };
      await activatePodOnContract(
        Request,
        Responses,
        fixture.contractId,
        2,
        addDaysIso(ANCHOR_DATE, -15),
        0,
      );
      return resolvePenaltyPrerequisites(Request, GeneratePayload, Responses, Endpoints);
    });

    let firstFingerprint: string;
    let secondFingerprint: string;
    let signedV3DetailId: number | null = null;
    const historicalDate = fixture.v2ExecutionDate;

    await test.step('Step 1: GET calculate-penalty-amount — executionDate in v2 window', async () => {
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: penaltyPrereq.actionTypeId,
        executionDate: historicalDate,
        penaltyId: penaltyPrereq.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: fixture.customerId,
        contractId: fixture.contractId,
        contractType: 'PRODUCT_CONTRACT',
        terminationId: penaltyPrereq.terminationId,
        pods: [Responses.pod[0].id],
      });
      await expect(response).CheckResponse();
      firstFingerprint = penaltyAmountFingerprint(body);

      await assertProductContractDetailResolution(Request, {
        contractId: fixture.contractId,
        executionDate: historicalDate,
        expectedLogicalVersionId: 2,
        expectedDetailId: fixture.signedV2DetailId,
        context: 'TC-BE-16 step 1 — must resolve SIGNED v2',
      });
    });

    await test.step('Step 2: Add SIGNED v3 with startDate after historical executionDate', async () => {
      const extended = await extendWithSignedVersion3AfterAnchor(fixture, Request, Endpoints);
      signedV3DetailId = extended.signedV3DetailId;
      expect(signedV3DetailId).toBeTruthy();
      expect(signedV3DetailId).not.toBe(fixture.signedV2DetailId);
    });

    await test.step('Step 3: GET calculate-penalty-amount — same executionDate as step 1', async () => {
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: penaltyPrereq.actionTypeId,
        executionDate: historicalDate,
        penaltyId: penaltyPrereq.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: fixture.customerId,
        contractId: fixture.contractId,
        contractType: 'PRODUCT_CONTRACT',
        terminationId: penaltyPrereq.terminationId,
        pods: [Responses.pod[0].id],
      });
      await expect(response).CheckResponse();
      secondFingerprint = penaltyAmountFingerprint(body);
      expect(
        secondFingerprint,
        'TC-BE-16: newer v3 must not change calculate result for original executionDate',
      ).toBe(firstFingerprint);

      await assertProductContractDetailResolution(Request, {
        contractId: fixture.contractId,
        executionDate: historicalDate,
        expectedLogicalVersionId: 2,
        expectedDetailId: fixture.signedV2DetailId,
        context: 'TC-BE-16 step 3 — historical date still v2 after v3',
      });
    });

    await test.step('Step 4: GET calculate-penalty-amount — executionDate in v3 window only', async () => {
      const v3Date = addDaysIso(ANCHOR_DATE, 35);
      const { response, body } = await calculatePenalty(Request, Endpoints, {
        actionTypeId: penaltyPrereq.actionTypeId,
        executionDate: v3Date,
        penaltyId: penaltyPrereq.penaltyId,
        penaltyPayer: 'CUSTOMER',
        customerId: fixture.customerId,
        contractId: fixture.contractId,
        contractType: 'PRODUCT_CONTRACT',
        terminationId: penaltyPrereq.terminationId,
        pods: [Responses.pod[0].id],
      });
      await expect(response).CheckResponse();

      const resolution = await assertProductContractDetailResolution(Request, {
        contractId: fixture.contractId,
        executionDate: v3Date,
        expectedLogicalVersionId: 3,
        expectedDetailId: signedV3DetailId,
        context: 'TC-BE-16 step 4 — date in v3 window resolves v3 (date-driven, not latest signed)',
      });
      expect(resolution.tripleMatch).toBeTruthy();

      await test.info().attach('[PDT-2815] TC-BE-16 recalc evidence', {
        body: JSON.stringify(
          {
            anchorDate: ANCHOR_DATE,
            historicalExecutionDate: historicalDate,
            v3ProbeDate: v3Date,
            signedV2DetailId: fixture.signedV2DetailId,
            signedV3DetailId,
            penaltyId: penaltyPrereq.penaltyId,
            firstFingerprint,
            secondFingerprint,
            thirdFingerprint: penaltyAmountFingerprint(body),
            amountAfterV3DateChange: penaltyNumericAmount(body),
            note: 'TC-BE-16 asserts fingerprint equality steps 1 vs 3 only; amount on step 4 may differ per TC',
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, {
      ...pdt2815LinksFromProductFixture('TC-BE-16', fixture),
      note: `penaltyId=${penaltyPrereq.penaltyId}; historicalDate=${historicalDate}`,
    });
  });

  test('[PDT-2815] TC-BE-17: All-customers mass communication — service DRAFT customer probe', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(35 * 60 * 1000);

    const probe = await test.step('Precondition: probe customers A and B', async () => {
      await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
      return buildAllCustomersProbeCustomers(Request, GeneratePayload, Responses, Endpoints);
    });

    let identifiers: string[];
    let massEmailId: number;

    await test.step('POST email-communication/mass — all customers with active contract', async () => {
      massEmailId = await createMassEmailDraftAllCustomers(Request, Endpoints, Responses);
      identifiers = await viewMassEmailCustomerIdentifiers(Request, Endpoints, massEmailId);
    });

    await test.step('Probe: Customer B included; document Customer A presence (service DRAFT gap)', async () => {
      const hasB = identifiers.some((id) => id === probe.customerBIdentifier);
      const hasA = identifiers.some((id) => id === probe.customerAIdentifier);
      expect(hasB, 'Customer B (signed service) should be eligible').toBeTruthy();

      await test.info().attach('[PDT-2815] TC-BE-17 all-customers gap probe', {
        body: JSON.stringify(
          {
            toBe: 'Customer A excluded or without DRAFT service detail in all-customers resolution',
            actual: {
              customerAInList: hasA,
              customerBInList: hasB,
              note:
                'MassEmailCommunicationResponse exposes customer names only — serviceContractDetailIds not visible via this API; if A is listed while service v1 is DRAFT-only, investigate CustomerRepository service CTE.',
            },
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });
    });

    attachPdt2815CreatedDataLinks(Responses, {
      testCase: 'TC-BE-17',
      customerIdentifier: probe.customerAIdentifier,
      customerId: probe.customerAId,
      productContractId: probe.productContractId,
      productContractNumber: probe.productContractNumber,
      serviceContractId: probe.serviceDraftContractId,
      serviceContractNumber: probe.serviceDraftContractNumber,
      massEmailId,
      note: `customerB=${probe.customerBIdentifier}; serviceSigned=${probe.serviceSignedContractNumber}`,
    });
  });
});
