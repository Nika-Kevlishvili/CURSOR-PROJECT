/**
 * PDT-2846 — Backend API tests: Service Contract version 1 startDate auto-realignment.
 * Maps 1:1 to Cursor-Project/test_cases/Backend/Service_Contract_First_Version_StartDate_PDT_2846.md
 */
import { test, expect } from '../../fixtures/baseFixture';
import {
  addDaysIso,
  attachPdt2846CreatedEntities,
  buildPdt2846EditPayload,
  expectPdt2846Put4xx,
  loadPdt2846DetailForEdit,
  loadPdt2846ServiceContractDetail,
  pdt2846AssertDraftV1AtCreate,
  pdt2846CreateVersion2Draft,
  pdt2846FinalizeV1EnteredIntoForce,
  pdt2846FinalizeV1Signed,
  pdt2846PromoteV1ToReady,
  pdt2846PutServiceContract,
  pdt2599MinCalendarIsoForServiceContractPost,
  readPdt2846BasicDates,
  readPdt2846VersionStartDate,
  runPdt2846DraftServiceContractChain,
} from './pdt-2846-service-contract.fixtures';
import {
  asServiceId,
  pickLatestLogicalVersionId,
  resolvePdt2599ContractFormulaForPost,
} from './pdt-2599-service-contract.fixtures';

test.describe('[PDT-2846] Service contract — first version startDate (backend API)', () => {
  test.setTimeout(15 * 60 * 1000);

  test('[PDT-2846] TC-BE-1: Version 1 — signingDate in the past auto-realigns startDate', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const signInDate = addDaysIso(today, -10);

    await test.step('PUT version 1 with past signingDate', async () => {
      const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
      const putRes = await pdt2846PutServiceContract(
        Request,
        Endpoints,
        contractId,
        1,
        buildPdt2846EditPayload(detail, {
          savingAsNewVersion: false,
          contractStatus: 'ENTERED_INTO_FORCE',
          detailsSubStatus: 'AWAITING_ACTIVATION',
          contractVersionStatus: 'SIGNED',
          signInDate,
          startDate: today,
          entryIntoForceDate: today,
        }),
      );
      await expect(putRes).CheckResponse();
    });

    await test.step('GET version 1 — startDate equals signingDate', async () => {
      const after = await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1);
      const dates = readPdt2846BasicDates(after);
      expect(dates.startDate).toBe(signInDate);
      expect(dates.signInDate).toBe(signInDate);
    });

    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-1' });
  });

  test('[PDT-2846] TC-BE-2: Version 1 — signingDate equals today — no realignment', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);

    await test.step('PUT version 1 with signingDate = today', async () => {
      await pdt2846FinalizeV1Signed(Request, Endpoints, contractId, {
        signInDate: today,
        startDate: today,
        entryIntoForceDate: addDaysIso(today, 30),
      });
    });

    const after = await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1);
    const dates = readPdt2846BasicDates(after);
    expect(dates.startDate).toBe(today);
    expect(dates.signInDate).toBe(today);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-2' });
  });

  test('[PDT-2846] TC-BE-3: Version 1 — signingDate equals startDate — no realignment', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);

    await pdt2846FinalizeV1EnteredIntoForce(Request, Endpoints, contractId, {
      signInDate: today,
      startDate: today,
      entryIntoForceDate: today,
    });

    const after = await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1);
    const dates = readPdt2846BasicDates(after);
    expect(dates.startDate).toBe(today);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-3' });
  });

  test('[PDT-2846] TC-BE-4: Version 1 — signingDate yesterday realigns startDate', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const yesterday = addDaysIso(today, -1);

    await pdt2846FinalizeV1EnteredIntoForce(Request, Endpoints, contractId, {
      signInDate: yesterday,
      startDate: today,
      entryIntoForceDate: yesterday,
    });

    const dates = readPdt2846BasicDates(
      await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1),
    );
    expect(dates.startDate).toBe(yesterday);
    expect(dates.signInDate).toBe(yesterday);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-4' });
  });

  test('[PDT-2846] TC-BE-5: Version 1 — signingDate 30 days ago realigns startDate', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const past = addDaysIso(today, -30);

    await pdt2846FinalizeV1EnteredIntoForce(Request, Endpoints, contractId, {
      signInDate: past,
      startDate: today,
      entryIntoForceDate: today,
    });

    const dates = readPdt2846BasicDates(
      await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1),
    );
    expect(dates.startDate).toBe(past);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-5' });
  });

  test('[PDT-2846] TC-BE-6: Version 1 — null signingDate and changed startDate rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const yesterday = addDaysIso(today, -1);
    const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
    const putRes = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      1,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: false,
        contractStatus: 'DRAFT',
        detailsSubStatus: 'DRAFT',
        contractVersionStatus: 'SIGNED',
        signInDate: null,
        startDate: yesterday,
      }),
    );
    await expectPdt2846Put4xx(putRes, /start date must not be changed/);
    const dates = readPdt2846BasicDates(
      await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1),
    );
    expect(dates.startDate).toBe(today);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-6' });
  });

  test('[PDT-2846] TC-BE-7: Version 1 — signingDate today and changed startDate rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const yesterday = addDaysIso(today, -1);
    const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
    const putRes = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      1,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: false,
        contractStatus: 'SIGNED',
        detailsSubStatus: 'SIGNED_BY_BOTH_SIDES',
        contractVersionStatus: 'SIGNED',
        signInDate: today,
        startDate: yesterday,
        entryIntoForceDate: addDaysIso(today, 30),
      }),
    );
    await expectPdt2846Put4xx(putRes, /start date must not be changed/);
    const dates = readPdt2846BasicDates(
      await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1),
    );
    expect(dates.startDate).toBe(today);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-7' });
  });

  test('[PDT-2846] TC-BE-8: Version 2 — no auto-realign when signingDate before v1 startDate', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const v1Past = addDaysIso(today, -20);

    await pdt2846FinalizeV1EnteredIntoForce(Request, Endpoints, contractId, {
      signInDate: v1Past,
      startDate: today,
      entryIntoForceDate: today,
    });

    const v2Start = addDaysIso(today, 5);
    const v2Id = await pdt2846CreateVersion2Draft(Request, Endpoints, contractId, v2Start);

    const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, v2Id);
    const bp = (detail.basicParameters ?? {}) as Record<string, unknown>;
    const evilSign = addDaysIso(v1Past, -5);
    const putRes = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      v2Id,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: false,
        contractStatus: String(bp.contractStatus ?? 'ACTIVE_IN_TERM'),
        detailsSubStatus: String(bp.detailsSubStatus ?? bp.subStatus ?? 'DELIVERY'),
        contractVersionStatus: 'SIGNED',
        signInDate: evilSign,
        startDate: addDaysIso(v1Past, -1),
        entryIntoForceDate: String(bp.entryIntoForceDate ?? today),
      }),
    );
    await expectPdt2846Put4xx(
      putRes,
      /first version|earlier than the previous|after the start date of the first version|status can not be changed/,
    );
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-8', version2Id: v2Id });
  });

  test('[PDT-2846] TC-BE-9: Version 1 — future signingDate rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const tomorrow = addDaysIso(today, 1);
    const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
    const putRes = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      1,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: false,
        contractStatus: 'SIGNED',
        detailsSubStatus: 'SIGNED_BY_BOTH_SIDES',
        contractVersionStatus: 'SIGNED',
        signInDate: tomorrow,
        startDate: today,
        entryIntoForceDate: addDaysIso(today, 30),
      }),
    );
    await expectPdt2846Put4xx(
      putRes,
      /signing date should be today or in future|signing date must not be in the future/,
    );
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-9' });
  });

  test('[PDT-2846] TC-BE-10: Version 1 — realigned startDate passes uniqueness', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const fiveDaysAgo = addDaysIso(today, -5);

    await pdt2846FinalizeV1EnteredIntoForce(Request, Endpoints, contractId, {
      signInDate: fiveDaysAgo,
      startDate: today,
      entryIntoForceDate: today,
    });

    const dates = readPdt2846BasicDates(
      await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1),
    );
    expect(dates.startDate).toBe(fiveDaysAgo);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-10' });
  });

  test('[PDT-2846] TC-BE-11: Version 1 realign conflicts with version 2 startDate', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const v1Past = addDaysIso(today, -10);

    await pdt2846FinalizeV1EnteredIntoForce(Request, Endpoints, contractId, {
      signInDate: v1Past,
      startDate: today,
      entryIntoForceDate: today,
    });

    const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
    const createV2 = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      1,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: true,
        startDate: v1Past,
        contractVersionStatus: 'SIGNED',
        contractStatus: 'ENTERED_INTO_FORCE',
        detailsSubStatus: 'AWAITING_ACTIVATION',
        signInDate: v1Past,
        entryIntoForceDate: today,
      }),
    );
    await expectPdt2846Put4xx(
      createV2,
      /contract version already has provided start date|already exists|start date must be after the start date of the first version/,
    );
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-11' });
  });

  test('[PDT-2846] TC-BE-12: After v1 realign, version 2 with later startDate succeeds', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const v1Past = addDaysIso(today, -10);

    await pdt2846FinalizeV1EnteredIntoForce(Request, Endpoints, contractId, {
      signInDate: v1Past,
      startDate: today,
      entryIntoForceDate: today,
    });

    const v2Start = addDaysIso(today, -5);
    const v2Id = await pdt2846CreateVersion2Draft(Request, Endpoints, contractId, v2Start);
    const v2Detail = await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId);
    expect(readPdt2846VersionStartDate(v2Detail, v2Id)).toBe(v2Start);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-12', version2Id: v2Id });
  });

  test('[PDT-2846] TC-BE-13: Version 1 DRAFT — non-null signingDate rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    const yesterday = addDaysIso(today, -1);
    const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
    const putRes = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      1,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: false,
        contractStatus: 'DRAFT',
        detailsSubStatus: 'DRAFT',
        contractVersionStatus: 'SIGNED',
        signInDate: yesterday,
        startDate: today,
      }),
    );
    await expectPdt2846Put4xx(
      putRes,
      /signing date should be empty|signing date must not be present/,
    );
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-13' });
  });

  test('[PDT-2846] TC-BE-14: DRAFT to SIGNED with past signingDate — realignment in one update', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    await pdt2846PromoteV1ToReady(Request, Endpoints, contractId);
    const sevenDaysAgo = addDaysIso(today, -7);

    await pdt2846FinalizeV1Signed(Request, Endpoints, contractId, {
      signInDate: sevenDaysAgo,
      startDate: today,
      entryIntoForceDate: addDaysIso(today, 30),
    });

    const dates = readPdt2846BasicDates(
      await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1),
    );
    expect(dates.startDate).toBe(sevenDaysAgo);
    expect(dates.signInDate).toBe(sevenDaysAgo);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-14' });
  });

  test('[PDT-2846] TC-BE-15: ENTERED_INTO_FORCE with past signingDate realigns startDate', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    await pdt2846PromoteV1ToReady(Request, Endpoints, contractId);
    const past = addDaysIso(today, -14);

    await pdt2846FinalizeV1EnteredIntoForce(Request, Endpoints, contractId, {
      signInDate: past,
      startDate: today,
      entryIntoForceDate: today,
    });

    const dates = readPdt2846BasicDates(
      await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1),
    );
    expect(dates.startDate).toBe(past);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-15' });
  });

  test('[PDT-2846] TC-BE-16: Version 1 READY — non-null signingDate rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    await pdt2846PromoteV1ToReady(Request, Endpoints, contractId);
    const today = pdt2599MinCalendarIsoForServiceContractPost();
    const past = addDaysIso(today, -5);
    const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
    const putRes = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      1,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: false,
        contractStatus: 'READY',
        detailsSubStatus: 'READY',
        contractVersionStatus: 'SIGNED',
        signInDate: past,
        startDate: today,
      }),
    );
    await expectPdt2846Put4xx(
      putRes,
      /signing date should be empty|signing date must not be present/,
    );
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-16' });
  });

  test('[PDT-2846] TC-BE-17: Version 2 update — no auto-realign of startDate', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);

    await pdt2846FinalizeV1Signed(Request, Endpoints, contractId, {
      signInDate: today,
      startDate: today,
      entryIntoForceDate: addDaysIso(today, 30),
    });

    const tomorrow = addDaysIso(today, 1);
    let detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
    let createV2 = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      1,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: true,
        startDate: tomorrow,
        contractVersionStatus: 'SIGNED',
        contractStatus: 'SIGNED',
        detailsSubStatus: 'SIGNED_BY_BOTH_SIDES',
        signInDate: today,
        entryIntoForceDate: addDaysIso(today, 30),
      }),
    );
    await expect(createV2).CheckResponse();
    const afterCreate = await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId);
    const v2Id = pickLatestLogicalVersionId(afterCreate.versions as { versionId: number }[]);
    expect(v2Id).toBe(2);

    const threeDaysAgo = addDaysIso(today, -3);
    detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, v2Id);
    const putRes = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      v2Id,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: false,
        contractStatus: 'SIGNED',
        detailsSubStatus: 'SIGNED_BY_BOTH_SIDES',
        contractVersionStatus: 'SIGNED',
        signInDate: threeDaysAgo,
        startDate: tomorrow,
        entryIntoForceDate: addDaysIso(today, 30),
      }),
    );
    await expect(putRes).CheckResponse();
    const v2After = await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId);
    expect(readPdt2846VersionStartDate(v2After, v2Id)).toBe(tomorrow);
    expect(readPdt2846BasicDates(
      await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, v2Id),
    ).signInDate).toBe(threeDaysAgo);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-17', version2Id: v2Id });
  });

  test('[PDT-2846] TC-BE-18: ENTERED_INTO_FORCE — future entryIntoForceDate rejected', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    await pdt2846PromoteV1ToReady(Request, Endpoints, contractId);
    const past = addDaysIso(today, -10);
    const tomorrow = addDaysIso(today, 1);
    const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
    const putRes = await pdt2846PutServiceContract(
      Request,
      Endpoints,
      contractId,
      1,
      buildPdt2846EditPayload(detail, {
        savingAsNewVersion: false,
        contractStatus: 'ENTERED_INTO_FORCE',
        detailsSubStatus: 'AWAITING_ACTIVATION',
        contractVersionStatus: 'SIGNED',
        signInDate: past,
        startDate: today,
        entryIntoForceDate: tomorrow,
      }),
    );
    await expectPdt2846Put4xx(
      putRes,
      /entry in force should be today or past|entry into force date is defined in the future/,
    );
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-18' });
  });

  test('[PDT-2846] TC-BE-19: POST create — past signingDate sets startDate at creation', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const serviceId = asServiceId(Responses.service[0]);
    const serviceViewRes = await Request.get(`${Endpoints.service}/${serviceId}?version=1`);
    await expect(serviceViewRes).CheckResponse();
    const serviceView = await serviceViewRes.json();
    const activeVersion = (serviceView.versions ?? []).find(
      (v: { status?: string }) => v.status === 'ACTIVE',
    );
    const serviceVersionId = activeVersion!.id as number;
    const serviceDetailId = activeVersion!.detailId as number;
    const thirdRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
      params: { serviceDetailId },
    });
    await expect(thirdRes).CheckResponse();
    const third = (await thirdRes.json()) as Record<string, unknown>;
    const termsCatalog = third.serviceContractTerms as { id: number }[];
    const invoiceTermsCatalog = third.invoicePaymentTerms as { id: number; value: number }[];
    const formula = await resolvePdt2599ContractFormulaForPost(
      Request,
      Endpoints,
      third,
      Responses.priceComponent[0],
      [],
    );
    const today = pdt2599MinCalendarIsoForServiceContractPost();
    const sevenDaysAgo = addDaysIso(today, -7);
    const customer = Responses.customer[0];
    const customerDetailsId = customer.lastCustomerDetailId;
    const billingRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
      params: { customerDetailsId, communicationDataType: 'BILLING' },
    });
    await expect(billingRes).CheckResponse();
    const contractCommRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
      params: { customerDetailsId, communicationDataType: 'CONTRACT' },
    });
    await expect(contractCommRes).CheckResponse();

    const payload = await GeneratePayload.contractsAndOrders.serviceContract();
    payload.basicParameters.communicationDataForBilling = (await billingRes.json())[0].id;
    payload.basicParameters.communicationDataForContract = (await contractCommRes.json())[0].id;
    payload.basicParameters.serviceVersionId = serviceVersionId;
    payload.basicParameters.contractStatus = 'ENTERED_INTO_FORCE';
    payload.basicParameters.detailsSubStatus = 'AWAITING_ACTIVATION';
    payload.basicParameters.contractVersionStatus = 'SIGNED';
    payload.basicParameters.signInDate = sevenDaysAgo;
    payload.basicParameters.entryIntoForceDate = today;
    payload.basicParameters.contractStatusModifyDate = today;
    payload.serviceParameters.contractTermId = termsCatalog[0].id;
    payload.serviceParameters.invoicePaymentTermId = invoiceTermsCatalog[0].id;
    payload.serviceParameters.invoicePaymentTerm = invoiceTermsCatalog[0].value;
    payload.serviceParameters.contractFormulas = formula
      ? [{ formulaVariableId: formula.formulaVariableId, value: formula.value }]
      : [];
    payload.serviceParameters.podIds = [Responses.pod[0].id];
    payload.serviceParameters.entryIntoForceDate = today;

    const createRes = await Request.post(Endpoints.serviceContract, { data: payload });
    await expect(createRes).CheckResponse();
    const contractId = (await createRes.json()) as number;
    Responses.serviceContract.push({ id: contractId });

    const dates = readPdt2846BasicDates(
      await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1),
    );
    expect(dates.startDate).toBe(sevenDaysAgo);
    expect(dates.signInDate).toBe(sevenDaysAgo);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-19' });
  });

  test('[PDT-2846] TC-BE-20: POST create DRAFT — startDate today, signingDate null', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    const { contractId } = await runPdt2846DraftServiceContractChain({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
    });
    const { today } = await pdt2846AssertDraftV1AtCreate(Request, Endpoints, contractId);
    attachPdt2846CreatedEntities(Responses, contractId, { tc: 'TC-BE-20' });
  });
});
