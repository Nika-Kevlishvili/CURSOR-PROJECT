/**
 * PDT-3212 — RFD POD tab listing eligibility Stage A/B (TC-BE-16..28).
 */
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  activateAllPods,
  activatePodAtIndex,
  assertPodAbsent,
  assertPodPresent,
  buildPdt3212CustomerIdentifier,
  buildPdt3212PodIdentifier,
  calculateRfdTax,
  createAndExecuteReminder,
  createCancellationExecuted,
  createCatalogEntities,
  createCustomerWithIdentifier,
  createExecutedRfdWithPods,
  createPodWithIdentifier,
  createProductContractFromPods,
  createReconnectionExecuted,
  createRfdDraft,
  completeBillingReminderForCurrentContract,
  executeMassBlockingForSupplyTermination,
  fetchPodRowFromLoadCustomers,
  isoDate,
  loadCustomersForDps,
  loadCustomersForDpsQueryBuilder,
  markPodDisconnectedExecuted,
  pdt3212RunId,
  resolveCustomerIdentifier,
  resolveSecondaryGridOperatorId,
  runVolumeReminderChain,
  viewPodTab,
  type Pdt3212Fx,
} from './PDT-3212-rfd-highest-consumption.fixtures';

test.describe('[PDT-3212]: RFD POD tab listing eligibility', { tag: '@receivableManagement' }, () => {
  function finish(
    TestRunSummary: Parameters<typeof finalizeTestRunSummary>[0],
    Responses: Parameters<typeof finalizeTestRunSummary>[1],
    check: string,
    snapshot: Record<string, unknown>,
  ): void {
    TestRunSummary.registerPayload('scenario', snapshot);
    TestRunSummary.recordCheck({
      check,
      expectedResult: 'Listing eligibility matches Stage A/B rules',
      actualResult: 'Observed listing matched expected presence/absence',
      passed: true,
    });
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: 'PDT-3212',
      relevantEntityKeys: ['customer', 'pod', 'productContract', 'reminderForDisconnection', 'requestForDisconnection'],
      snapshot,
    });
  }

  test('[PDT-3212] TC-BE-16: Disconnected POD excluded from load-customer and view-pod-tab', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE16',
      podLabels: ['POD-DISC', 'POD-CTRL'],
      podVolumes: [150, 150],
    });
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const podDiscRow = await fetchPodRowFromLoadCustomers(fx, query, ctx.pods[0].identifier, true);
    const rfdExecutedId = await createExecutedRfdWithPods(fx, [podDiscRow]);
    await markPodDisconnectedExecuted(fx, rfdExecutedId, ctx.customerId, ctx.pods[0].id);
    const rfdDraftBId = await createRfdDraft(fx);
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, ctx.pods[0].identifier);
    assertPodPresent(loadRows, ctx.pods[1].identifier);
    const viewRows = await viewPodTab(fx, rfdDraftBId, query);
    assertPodAbsent(viewRows, ctx.pods[0].identifier);
    assertPodPresent(viewRows, ctx.pods[1].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-16 disconnected POD excluded', { rfdDraftBId, rfdExecutedId });
  });

  test('[PDT-3212] TC-BE-17: Impossible-to-disconnect POD excluded from tab', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(18 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE17',
      podLabels: ['POD-IMP', 'POD-CTRL'],
      podVolumes: [150, 150],
    });
    const podImpId = ctx.pods[0].id;
    const podGet = await Request.get(`pod/${podImpId}?version=1`);
    await expect(podGet).CheckResponse();
    const podBody = await podGet.json();
    const podVersion = podBody.version ?? 1;
    podBody.impossibleToDisconnect = true;
    const putRes = await Request.put(`pod/${podImpId}?version=${podVersion}`, { data: podBody });
    await expect(putRes).CheckResponse();
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, ctx.pods[0].identifier);
    assertPodPresent(loadRows, ctx.pods[1].identifier);
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertPodAbsent(viewRows, ctx.pods[0].identifier);
    assertPodPresent(viewRows, ctx.pods[1].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-17 impossible POD excluded', { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-18: Deleted POD status excluded from tab', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(18 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    await createCatalogEntities(fx);
    await createCustomerWithIdentifier(fx, buildPdt3212CustomerIdentifier('BE18', runId));
    const customerIdentifier = resolveCustomerIdentifier(fx);
    const podDel = await createPodWithIdentifier(fx, buildPdt3212PodIdentifier('BE18', runId, 0));
    const podCtrl = await createPodWithIdentifier(fx, buildPdt3212PodIdentifier('BE18', runId, 1));
    await createProductContractFromPods(fx);
    await activatePodAtIndex(fx, 0);
    await activatePodAtIndex(fx, 1);
    await activatePodAtIndex(fx, 0, randomGens.generateYesterdaysDate('yyyy-mm-dd'));
    await Request.delete(`pod/${podDel.id}`);
    const ctx = await completeBillingReminderForCurrentContract(fx, {
      customerIdentifier,
      podVolumes: [null, 150],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, podDel.identifier);
    assertPodPresent(loadRows, podCtrl.identifier);
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertPodAbsent(viewRows, podDel.identifier);
    assertPodPresent(viewRows, podCtrl.identifier);
    finish(TestRunSummary, Responses, 'TC-BE-18 deleted POD excluded', { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-19: Grid-operator mismatch excludes POD from tab', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(18 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const secondaryGridOperatorId = await resolveSecondaryGridOperatorId(fx);
    await createCatalogEntities(fx);
    await createCustomerWithIdentifier(fx, buildPdt3212CustomerIdentifier('BE19', runId));
    const customerIdentifier = resolveCustomerIdentifier(fx);
    const podMis = await createPodWithIdentifier(fx, buildPdt3212PodIdentifier('BE19', runId, 0), secondaryGridOperatorId);
    const podMatch = await createPodWithIdentifier(fx, buildPdt3212PodIdentifier('BE19', runId, 1), envVariables.grid_operator);
    await createProductContractFromPods(fx);
    await activateAllPods(fx);
    const ctx = await completeBillingReminderForCurrentContract(fx, {
      customerIdentifier,
      podVolumes: [150, 150],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, podMis.identifier);
    assertPodPresent(loadRows, podMatch.identifier);
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertPodAbsent(viewRows, podMis.identifier);
    assertPodPresent(viewRows, podMatch.identifier);
    finish(TestRunSummary, Responses, 'TC-BE-19 grid operator mismatch', { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-20: Blocked-for-disconnection window excludes POD', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(18 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE20',
      podLabels: ['POD-BLOCK', 'POD-CTRL'],
      podVolumes: [150, 150],
    });
    const podBlockId = ctx.pods[0].id;
    const podGet = await Request.get(`pod/${podBlockId}?version=1`);
    await expect(podGet).CheckResponse();
    const podBody = await podGet.json();
    const version = podBody.version ?? 1;
    podBody.blockedDisconnection = true;
    podBody.blockedDisconnectionRequest = {
      from: isoDate(0),
      to: isoDate(7),
      reason: 'QA block',
      additionalInfo: null,
    };
    const putRes = await Request.put(`pod/${podBlockId}?version=${version}`, { data: podBody });
    await expect(putRes).CheckResponse();
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, ctx.pods[0].identifier);
    assertPodPresent(loadRows, ctx.pods[1].identifier);
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertPodAbsent(viewRows, ctx.pods[0].identifier);
    assertPodPresent(viewRows, ctx.pods[1].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-20 blocked window', { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-21: Inactive contract-POD excluded under supplierType CURRENT', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(18 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    await createCatalogEntities(fx);
    await createCustomerWithIdentifier(fx, buildPdt3212CustomerIdentifier('BE21', runId));
    const customerIdentifier = resolveCustomerIdentifier(fx);
    await createPodWithIdentifier(fx, buildPdt3212PodIdentifier('BE21', runId, 0));
    await createPodWithIdentifier(fx, buildPdt3212PodIdentifier('BE21', runId, 1));
    await createProductContractFromPods(fx);
    await activatePodAtIndex(fx, 1);
    const ctx = await completeBillingReminderForCurrentContract(fx, {
      customerIdentifier,
      podVolumes: [null, 150],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, ctx.pods[0].identifier);
    assertPodPresent(loadRows, ctx.pods[1].identifier);
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertPodAbsent(viewRows, ctx.pods[0].identifier);
    assertPodPresent(viewRows, ctx.pods[1].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-21 inactive contract-POD', { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-22: Active current contract excludes POD under supplierType PREVIOUS', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(15 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE22',
      podLabels: ['POD'],
      podVolumes: [150],
    });
    const rfdDraftId = await createRfdDraft(fx, { supplierType: 'PREVIOUS' });
    const query = loadCustomersForDpsQueryBuilder(ctx, { supplierType: 'PREVIOUS' });
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, ctx.pods[0].identifier);
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertPodAbsent(viewRows, ctx.pods[0].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-22 PREVIOUS blocked by active CURRENT', { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-23: Executed RFD with checked POD blocks new draft listing (Stage B)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(18 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE23',
      podLabels: ['POD'],
      podVolumes: [150],
    });
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const podRow = await fetchPodRowFromLoadCustomers(fx, query, ctx.pods[0].identifier, true);
    const rfdDraftBId = await createRfdDraft(fx, { allSelected: true });
    const rfdExecutedAId = await createExecutedRfdWithPods(fx, [podRow]);
    await calculateRfdTax(fx, rfdExecutedAId);
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, ctx.pods[0].identifier);
    const viewRows = await viewPodTab(fx, rfdDraftBId, query);
    assertPodAbsent(viewRows, ctx.pods[0].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-23 Stage B block', { rfdDraftBId, rfdExecutedAId });
  });

  test('[PDT-3212] TC-BE-24: Executed cancellation clears Stage B block — POD reappears', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE24',
      podLabels: ['POD'],
      podVolumes: [150],
    });
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const podRow = await fetchPodRowFromLoadCustomers(fx, query, ctx.pods[0].identifier, true);
    const rfdDraftBId = await createRfdDraft(fx, { allSelected: true });
    const rfdExecutedAId = await createExecutedRfdWithPods(fx, [podRow]);
    await calculateRfdTax(fx, rfdExecutedAId);
    assertPodAbsent(await viewPodTab(fx, rfdDraftBId, query), ctx.pods[0].identifier);
    await createCancellationExecuted(fx, rfdExecutedAId, podRow);
    ctx.reminderId = await createAndExecuteReminder(fx);
    const queryAfter = loadCustomersForDpsQueryBuilder(ctx);
    const rfdDraftCId = await createRfdDraft(fx, { allSelected: true });
    const loadRows = (await loadCustomersForDps(fx, queryAfter)).rows;
    assertPodPresent(loadRows, ctx.pods[0].identifier);
    assertPodPresent(await viewPodTab(fx, rfdDraftCId, queryAfter), ctx.pods[0].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-24 cancellation clears block', { rfdDraftCId });
  });

  test('[PDT-3212] TC-BE-25: Executed reconnection clears Stage B block — POD reappears', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE25',
      podLabels: ['POD'],
      podVolumes: [150],
    });
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const podRow = await fetchPodRowFromLoadCustomers(fx, query, ctx.pods[0].identifier, true);
    const rfdDraftBId = await createRfdDraft(fx, { allSelected: true });
    const rfdExecutedAId = await createExecutedRfdWithPods(fx, [podRow]);
    await calculateRfdTax(fx, rfdExecutedAId);
    assertPodAbsent(await viewPodTab(fx, rfdDraftBId, query), ctx.pods[0].identifier);
    await markPodDisconnectedExecuted(fx, rfdExecutedAId, ctx.customerId, ctx.pods[0].id);
    await createReconnectionExecuted(fx, rfdExecutedAId, podRow);
    ctx.reminderId = await createAndExecuteReminder(fx);
    const queryAfter = loadCustomersForDpsQueryBuilder(ctx);
    const rfdDraftCId = await createRfdDraft(fx, { allSelected: true });
    assertPodPresent((await loadCustomersForDps(fx, queryAfter)).rows, ctx.pods[0].identifier);
    assertPodPresent(await viewPodTab(fx, rfdDraftCId, queryAfter), ctx.pods[0].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-25 reconnection clears block', { rfdDraftCId });
  });

  test('[PDT-3212] TC-BE-26: Customer blocked for supply termination excludes all POD rows', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(18 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    await createCatalogEntities(fx);
    await createCustomerWithIdentifier(fx, buildPdt3212CustomerIdentifier('BE26', runId));
    const customerIdentifier = resolveCustomerIdentifier(fx);
    await createPodWithIdentifier(fx, buildPdt3212PodIdentifier('BE26', runId, 0));
    await createProductContractFromPods(fx);
    await activateAllPods(fx);
    let ctx = await completeBillingReminderForCurrentContract(fx, {
      customerIdentifier,
      podVolumes: [150],
      skipReminder: true,
    });
    await executeMassBlockingForSupplyTermination(fx, customerIdentifier);
    const reminderId = await createAndExecuteReminder(fx);
    ctx = { ...ctx, reminderId };
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    expect(loadRows.filter((r) => r.podIdentifier === ctx.pods[0].identifier).length).toBe(0);
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    expect(viewRows.filter((r) => r.podIdentifier === ctx.pods[0].identifier).length).toBe(0);
    finish(TestRunSummary, Responses, 'TC-BE-26 MOFB supply termination block', { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-27: Liability amount From/To filters exclude out-of-range POD', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(18 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE27',
      podLabels: ['POD-LOW', 'POD-HIGH'],
      podVolumes: [50, 500],
    });
    const baseQuery = loadCustomersForDpsQueryBuilder(ctx);
    const baseline = (await loadCustomersForDps(fx, baseQuery)).rows;
    const lowRow = assertPodPresent(baseline, ctx.pods[0].identifier);
    const amountLow = Number(lowRow.liabilityAmountCustomer);
    const filterQuery = {
      ...baseQuery,
      liabilityAmountFrom: Number((amountLow - 0.01).toFixed(2)),
      liabilityAmountTo: Number((amountLow + 0.01).toFixed(2)),
    };
    const rfdDraftId = await createRfdDraft(fx);
    const filtered = (await loadCustomersForDps(fx, filterQuery)).rows;
    assertPodPresent(filtered, ctx.pods[0].identifier);
    assertPodAbsent(filtered, ctx.pods[1].identifier);
    const viewFiltered = await viewPodTab(fx, rfdDraftId, filterQuery);
    assertPodPresent(viewFiltered, ctx.pods[0].identifier);
    assertPodAbsent(viewFiltered, ctx.pods[1].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-27 liability amount filter', { rfdDraftId, amountLow });
  });

  test('[PDT-3212] TC-BE-28: POD tab search by POD_IDENTIFIER returns matching row only', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(15 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE28',
      podLabels: ['POD-A', 'POD-B'],
      podVolumes: [150, 150],
    });
    const baseQuery = loadCustomersForDpsQueryBuilder(ctx);
    expect((await loadCustomersForDps(fx, baseQuery)).rows.length).toBeGreaterThanOrEqual(2);
    const searchQuery = {
      ...baseQuery,
      searchBy: 'POD_IDENTIFIER',
      prompt: ctx.pods[0].identifier,
    };
    const rfdDraftId = await createRfdDraft(fx);
    const searched = (await loadCustomersForDps(fx, searchQuery)).rows;
    expect(searched.length).toBe(1);
    expect(searched[0].podIdentifier).toBe(ctx.pods[0].identifier);
    const viewSearched = await viewPodTab(fx, rfdDraftId, searchQuery);
    expect(viewSearched.length).toBe(1);
    expect(viewSearched[0].podIdentifier).toBe(ctx.pods[0].identifier);
    finish(TestRunSummary, Responses, 'TC-BE-28 POD_IDENTIFIER search', { rfdDraftId });
  });
});
