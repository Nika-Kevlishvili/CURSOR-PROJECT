/**
 * PDT-3212 — highest consumption ranking (TC-BE-1..15).
 * Reference: PDT-2529 fixtures, PDT-2861 executed RFD patterns.
 */
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  assertExactlyOneHighestAmong,
  assertHighestConsumption,
  assertPodAbsent,
  assertPodPresent,
  createAndExecuteReminder,
  createCatalogEntities,
  createCustomerWithIdentifier,
  createPodWithIdentifier,
  createProductContractFromPods,
  activateAllPods,
  activatePodAtIndex,
  createRfdDraft,
  updateRfdDraft,
  executeRfd,
  fetchCheckedPods,
  fetchPodRowFromLoadCustomers,
  loadCustomersForDps,
  loadCustomersForDpsQueryBuilder,
  pdt3212RunId,
  postBillingByProfileKwh,
  resolveContractNumber,
  resolvePriorBillingAnchor,
  resolveRfdVolumeBillingAnchor,
  runForVolumesBillingAtAnchor,
  runVolumeReminderChain,
  shiftLiabilitiesToYesterday,
  terminateContractForPreviousSupplier,
  viewPodTab,
  type Pdt3212ChainCtx,
  type Pdt3212Fx,
} from './PDT-3212-rfd-highest-consumption.fixtures';
import { randomGens } from '../../utils/randomGens';

test.describe('[PDT-3212]: RFD highest consumption ranking', { tag: '@receivableManagement' }, () => {
  function attachSummary(
    TestRunSummary: Parameters<typeof finalizeTestRunSummary>[0],
    Responses: Parameters<typeof finalizeTestRunSummary>[1],
    check: string,
    expected: string,
    actual: string,
    passed: boolean,
    snapshot: Record<string, unknown>,
  ): void {
    TestRunSummary.recordCheck({ check, expectedResult: expected, actualResult: actual, passed });
    TestRunSummary.registerPayload('scenario', snapshot);
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: 'PDT-3212',
      relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice', 'reminderForDisconnection', 'requestForDisconnection'],
      snapshot,
    });
  }

  test('[PDT-3212] TC-BE-1: Listed POD with highest summed volume receives YES among three listed PODs', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    let ctx!: Pdt3212ChainCtx;
    let rfdDraftId = 0;

    await test.step('Precondition: volume reminder chain (100/300/200 kWh)', async () => {
      ctx = await runVolumeReminderChain(fx, {
        runId,
        tcSuffix: 'BE1',
        podLabels: ['POD-A', 'POD-B', 'POD-C'],
        podVolumes: [100, 300, 200],
      });
      rfdDraftId = await createRfdDraft(fx);
    });

    const query = loadCustomersForDpsQueryBuilder(ctx);
    let loadRows: Awaited<ReturnType<typeof loadCustomersForDps>>['rows'] = [];
    let viewRows: Awaited<ReturnType<typeof viewPodTab>> = [];

    await test.step('Assert load-customer and view-pod-tab ranking', async () => {
      const podIds = ctx.pods.map((pod) => pod.identifier);
      loadRows = (
        await loadCustomersForDps(fx, query, { waitForPodIdentifiers: podIds })
      ).rows;
      assertExactlyOneHighestAmong(loadRows, [ctx.pods[1].identifier]);
      assertHighestConsumption(assertPodPresent(loadRows, ctx.pods[0].identifier), false, 'POD-A load');
      assertHighestConsumption(assertPodPresent(loadRows, ctx.pods[2].identifier), false, 'POD-C load');

      viewRows = await viewPodTab(fx, rfdDraftId, query);
      assertExactlyOneHighestAmong(viewRows, [ctx.pods[1].identifier]);
    });

    attachSummary(
      TestRunSummary,
      Responses,
      'TC-BE-1 highest consumption among three listed PODs',
      'POD-B (300 kWh) is sole YES row on load-customer and view-pod-tab',
      `POD-B isHighestConsumption=true on both endpoints`,
      true,
      { rfdDraftId, podB: ctx.pods[1].identifier, loadCount: loadRows.length },
    );
  });

  test('[PDT-3212] TC-BE-2: Off-list POD with higher global volume does not steal YES from listed PODs', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    let ctx!: Pdt3212ChainCtx;
    let rfdDraftId = 0;

    await test.step('Precondition: exclude POD-B on draft (300 kWh off-list)', async () => {
      ctx = await runVolumeReminderChain(fx, {
        runId,
        tcSuffix: 'BE2',
        podLabels: ['POD-A', 'POD-B', 'POD-C'],
        podVolumes: [100, 300, 200],
      });
      rfdDraftId = await createRfdDraft(fx, { excludePodIds: [ctx.pods[1].id] });
    });

    const query = loadCustomersForDpsQueryBuilder(ctx);
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, ctx.pods[1].identifier);
    assertExactlyOneHighestAmong(loadRows, [ctx.pods[2].identifier]);

    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertPodAbsent(viewRows, ctx.pods[1].identifier);
    assertExactlyOneHighestAmong(viewRows, [ctx.pods[2].identifier]);

    attachSummary(TestRunSummary, Responses, 'TC-BE-2 off-list POD excluded from ranking pool', 'POD-C wins among listed POD-A and POD-C', 'POD-C isHighestConsumption=true', true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-3: Excluded POD on draft update recalculates YES/NO among remaining listed PODs', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    let ctx!: Pdt3212ChainCtx;
    let rfdDraftId = 0;

    await test.step('Precondition: three-POD chain + draft', async () => {
      ctx = await runVolumeReminderChain(fx, {
        runId,
        tcSuffix: 'BE3',
        podLabels: ['POD-A', 'POD-B', 'POD-C'],
        podVolumes: [100, 300, 200],
      });
      rfdDraftId = await createRfdDraft(fx);
      const baseline = (await loadCustomersForDps(fx, loadCustomersForDpsQueryBuilder(ctx))).rows;
      assertHighestConsumption(assertPodPresent(baseline, ctx.pods[1].identifier), true, 'baseline POD-B');
    });

    await test.step('PUT draft with excludePodIds=[podBId]', async () => {
      await updateRfdDraft(fx, rfdDraftId, { excludePodIds: [ctx.pods[1].id] });
      const getRes = await Request.get(`${Endpoints.requestForDisconnection}/${rfdDraftId}`);
      await expect(getRes).CheckResponse();
      const body = await getRes.json();
      expect(body.excludePodIds ?? []).toContain(ctx.pods[1].id);
    });

    const query = loadCustomersForDpsQueryBuilder(ctx);
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertPodAbsent(viewRows, ctx.pods[1].identifier);
    assertExactlyOneHighestAmong(viewRows, [ctx.pods[2].identifier]);

    attachSummary(TestRunSummary, Responses, 'TC-BE-3 excludePodIds recalculates YES', 'POD-C YES after excluding POD-B', 'POD-C isHighestConsumption=true', true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-4: Equal summed volumes — highest POD id wins tie', async ({
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
      tcSuffix: 'BE4',
      podLabels: ['POD-LOW', 'POD-HIGH-ID'],
      podVolumes: [200, 200],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const rows = (await loadCustomersForDps(fx, query)).rows;
    const highPod = ctx.pods[1].id > ctx.pods[0].id ? ctx.pods[1] : ctx.pods[0];
    const lowPod = highPod === ctx.pods[1] ? ctx.pods[0] : ctx.pods[1];
    assertHighestConsumption(assertPodPresent(rows, highPod.identifier), true, 'higher pod id');
    assertHighestConsumption(assertPodPresent(rows, lowPod.identifier), false, 'lower pod id');
    attachSummary(TestRunSummary, Responses, 'TC-BE-4 tie-break by pod id', 'Higher podId receives YES', `${highPod.identifier} isHighestConsumption=true`, true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-5: No invoice volumes — fallback to highest POD id among listed PODs', async ({
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
      tcSuffix: 'BE5',
      podLabels: ['POD-X', 'POD-Y'],
      podVolumes: [0, 0],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const rows = (await loadCustomersForDps(fx, query)).rows;
    const highPod = ctx.pods[1].id > ctx.pods[0].id ? ctx.pods[1] : ctx.pods[0];
    assertHighestConsumption(assertPodPresent(rows, highPod.identifier), true, 'fallback high id');
    attachSummary(TestRunSummary, Responses, 'TC-BE-5 no-volume fallback', 'Highest pod id YES', `${highPod.identifier} isHighestConsumption=true`, true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-6: Mixed volume and no-volume PODs — volume POD wins', async ({
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
      tcSuffix: 'BE6',
      podLabels: ['POD-VOL', 'POD-NOVOL'],
      podVolumes: [300, null],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const rows = await viewPodTab(fx, rfdDraftId, query);
    assertHighestConsumption(assertPodPresent(rows, ctx.pods[0].identifier), true, 'POD-VOL');
    assertHighestConsumption(assertPodPresent(rows, ctx.pods[1].identifier), false, 'POD-NOVOL');
    attachSummary(TestRunSummary, Responses, 'TC-BE-6 mixed volume', 'POD with volume YES', 'POD-VOL true, POD-NOVOL false', true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-7: Bulk select podWithHighestConsumption selects only YES rows', async ({
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
      tcSuffix: 'BE7',
      podLabels: ['POD-A', 'POD-B', 'POD-C'],
      podVolumes: [100, 300, 200],
    });
    const rfdDraftId = await createRfdDraft(fx);
    await updateRfdDraft(fx, rfdDraftId, { podWithHighestConsumption: true });
    const checked = await fetchCheckedPods(fx, ctx.reminderId, ctx.gridOperatorId, rfdDraftId);
    expect(checked.length).toBe(1);
    expect(checked[0].podId).toBe(ctx.pods[1].id);
    expect(checked[0].isHighestConsumption).toBe(true);
    const viewRows = await viewPodTab(fx, rfdDraftId, loadCustomersForDpsQueryBuilder(ctx));
    assertHighestConsumption(assertPodPresent(viewRows, ctx.pods[1].identifier), true, 'POD-B checked YES');
    expect(assertPodPresent(viewRows, ctx.pods[1].identifier).isChecked).toBe(true);
    expect(assertPodPresent(viewRows, ctx.pods[0].identifier).isChecked).toBe(false);
    expect(assertPodPresent(viewRows, ctx.pods[2].identifier).isChecked).toBe(false);
    attachSummary(TestRunSummary, Responses, 'TC-BE-7 bulk select YES only', 'Only POD-B checked', `get-checked-pods length=1 podId=${ctx.pods[1].id}`, true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-8: Execute RFD persists isHighestConsumption readback', async ({
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
      tcSuffix: 'BE8',
      podLabels: ['POD-A', 'POD-B', 'POD-C'],
      podVolumes: [100, 300, 200],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const draftView = await viewPodTab(fx, rfdDraftId, loadCustomersForDpsQueryBuilder(ctx));
    assertHighestConsumption(assertPodPresent(draftView, ctx.pods[1].identifier), true, 'draft POD-B');
    await executeRfd(fx, rfdDraftId);
    const getRes = await Request.get(`${Endpoints.requestForDisconnection}/${rfdDraftId}`);
    await expect(getRes).CheckResponse();
    expect((await getRes.json()).disconnectionRequestsStatus).toBe('EXECUTED');
    const executedView = await viewPodTab(fx, rfdDraftId, loadCustomersForDpsQueryBuilder(ctx));
    assertHighestConsumption(assertPodPresent(executedView, ctx.pods[1].identifier), true, 'executed POD-B');
    attachSummary(TestRunSummary, Responses, 'TC-BE-8 execute persistence', 'POD-B YES after EXECUTED', 'view-pod-tab matches draft flags', true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-9: Filter isHighestConsumption=true returns only YES rows', async ({
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
      tcSuffix: 'BE9',
      podLabels: ['POD-A', 'POD-B', 'POD-C'],
      podVolumes: [100, 300, 200],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const baseQuery = loadCustomersForDpsQueryBuilder(ctx);
    const yesOnly = (await loadCustomersForDps(fx, { ...baseQuery, isHighestConsumption: true })).rows;
    expect(yesOnly.length).toBe(1);
    expect(yesOnly[0].podId).toBe(ctx.pods[1].id);
    const noOnly = (await loadCustomersForDps(fx, { ...baseQuery, isHighestConsumption: false })).rows;
    expect(noOnly.every((r) => r.isHighestConsumption === false)).toBe(true);
    expect(noOnly.length).toBe(2);
    const viewYes = await viewPodTab(fx, rfdDraftId, { ...baseQuery, isHighestConsumption: true });
    expect(viewYes.length).toBe(1);
    attachSummary(TestRunSummary, Responses, 'TC-BE-9 isHighestConsumption filter', 'Filter true returns POD-B only', `yesOnly.length=1 podId=${ctx.pods[1].id}`, true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-10: Sort POD_WITH_HIGHEST_CONSUMPTION DESC lists YES rows first', async ({
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
      tcSuffix: 'BE10',
      podLabels: ['POD-A', 'POD-B', 'POD-C'],
      podVolumes: [100, 300, 200],
    });
    await createRfdDraft(fx);
    const query = {
      ...loadCustomersForDpsQueryBuilder(ctx),
      sortBy: 'POD_WITH_HIGHEST_CONSUMPTION',
      direction: 'DESC' as const,
      size: 10,
    };
    const rows = (await loadCustomersForDps(fx, query)).rows;
    const idx = (id: string) => rows.findIndex((r) => r.podIdentifier === id);
    const idxB = idx(ctx.pods[1].identifier);
    const idxA = idx(ctx.pods[0].identifier);
    const idxC = idx(ctx.pods[2].identifier);
    expect(idxB).toBeLessThan(idxA);
    expect(idxB).toBeLessThan(idxC);
    for (let i = 0; i < rows.length; i++) {
      for (let j = i + 1; j < rows.length; j++) {
        if (rows[i].isHighestConsumption === true && rows[j].isHighestConsumption === false) {
          expect(i).toBeLessThan(j);
        }
      }
    }
    attachSummary(TestRunSummary, Responses, 'TC-BE-10 sort YES first', 'POD-B index lowest among targets', `idxB=${idxB} idxA=${idxA} idxC=${idxC}`, true, { idxB, idxA, idxC });
  });

  test('[PDT-3212] TC-BE-11: get-checked-pods readback matches view-pod-tab after manual selection', async ({
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
      tcSuffix: 'BE11',
      podLabels: ['POD-A', 'POD-B', 'POD-C'],
      podVolumes: [100, 300, 200],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const rowA = await fetchPodRowFromLoadCustomers(fx, query, ctx.pods[0].identifier, true);
    const rowB = await fetchPodRowFromLoadCustomers(fx, query, ctx.pods[1].identifier, true);
    await updateRfdDraft(fx, rfdDraftId, { pods: [rowA, rowB], allSelected: false });
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertHighestConsumption(assertPodPresent(viewRows, ctx.pods[1].identifier), true, 'POD-B YES');
    const checked = await fetchCheckedPods(fx, ctx.reminderId, ctx.gridOperatorId, rfdDraftId);
    expect(checked.length).toBe(2);
    expect(checked.find((p) => p.podId === ctx.pods[1].id)?.isHighestConsumption).toBe(true);
    expect(checked.find((p) => p.podId === ctx.pods[0].id)?.isHighestConsumption).toBe(false);
    attachSummary(TestRunSummary, Responses, 'TC-BE-11 checked pods readback', 'Two checked pods with matching flags', `checked.length=2`, true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-12: Multi-invoice volume sum within one billing group', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };
    const runId = pdt3212RunId(testInfo);
    const anchor1 = await resolveRfdVolumeBillingAnchor(Request);
    const anchor2 = await resolvePriorBillingAnchor(Request, anchor1);
    const ctx = await runVolumeReminderChain(fx, {
      runId,
      tcSuffix: 'BE12',
      podLabels: ['POD-P', 'POD-Q'],
      skipBillingProfiles: true,
      multiAnchorVolumes: [
        { podIndex: 0, kwh: 100, anchor: anchor2 },
        { podIndex: 0, kwh: 250, anchor: anchor1 },
        { podIndex: 1, kwh: 300, anchor: anchor1 },
      ],
    });
    const rfdDraftId = await createRfdDraft(fx);
    const rows = (await loadCustomersForDps(fx, loadCustomersForDpsQueryBuilder(ctx))).rows;
    assertHighestConsumption(assertPodPresent(rows, ctx.pods[0].identifier), true, 'POD-P summed 350');
    assertHighestConsumption(assertPodPresent(rows, ctx.pods[1].identifier), false, 'POD-Q 300');
    attachSummary(TestRunSummary, Responses, 'TC-BE-12 multi-invoice sum', 'POD-P 350 kWh wins', 'POD-P isHighestConsumption=true', true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-13: supplierType PREVIOUS — listed-POD-only ranking still applies', async ({
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
      tcSuffix: 'BE13',
      podLabels: ['POD-A', 'POD-B', 'POD-C'],
      podVolumes: [100, 300, 200],
    });
    await terminateContractForPreviousSupplier(fx);
    for (let i = 0; i < ctx.pods.length; i++) {
      await activatePodAtIndex(fx, i, randomGens.generateYesterdaysDate('yyyy-mm-dd'));
    }
    await shiftLiabilitiesToYesterday(fx, ctx.customerIdentifier);
    ctx.reminderId = await createAndExecuteReminder(fx);
    const rfdDraftId = await createRfdDraft(fx, {
      supplierType: 'PREVIOUS',
      excludePodIds: [ctx.pods[1].id],
    });
    const query = loadCustomersForDpsQueryBuilder(ctx, { supplierType: 'PREVIOUS' });
    const loadRows = (await loadCustomersForDps(fx, query)).rows;
    assertPodAbsent(loadRows, ctx.pods[1].identifier);
    assertExactlyOneHighestAmong(loadRows, [ctx.pods[2].identifier]);
    const viewRows = await viewPodTab(fx, rfdDraftId, query);
    assertPodAbsent(viewRows, ctx.pods[1].identifier);
    assertExactlyOneHighestAmong(viewRows, [ctx.pods[2].identifier]);

    attachSummary(TestRunSummary, Responses, 'TC-BE-13 PREVIOUS supplier ranking', 'POD-C YES among listed under PREVIOUS', 'POD-C isHighestConsumption=true on load and view', true, { rfdDraftId });
  });

  test('[PDT-3212] TC-BE-14: load-customer rejects missing gridOperatorId', async ({
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
      tcSuffix: 'BE14',
      podLabels: ['POD'],
      podVolumes: [150],
    });
    const { status, body } = await loadCustomersForDps(
      fx,
      {
        page: 0,
        size: 20,
        conditionType: 'ALL_CUSTOMERS',
        supplierType: 'CURRENT',
        powerSupplyDisconnectionReminderId: ctx.reminderId,
      },
      { expectSuccess: false },
    );
    expect(status).toBe(400);
    expect(JSON.stringify(body)).toContain('gridOperatorId-gridOperatorId must not be null');
    attachSummary(TestRunSummary, Responses, 'TC-BE-14 missing gridOperatorId', 'HTTP 400 with validation message', `status=${status}`, true, { reminderId: ctx.reminderId });
  });

  test('[PDT-3212] TC-BE-15: Bulk select must not check NO rows when zero YES rows', async ({
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
      tcSuffix: 'BE15',
      podLabels: ['POD-A', 'POD-B', 'POD-C'],
      podVolumes: [100, 300, 200],
    });
    const rfdDraftId = await createRfdDraft(fx, { excludePodIds: [ctx.pods[1].id] });
    const query = loadCustomersForDpsQueryBuilder(ctx);
    const viewBefore = await viewPodTab(fx, rfdDraftId, query);
    const yesCount = viewBefore.filter((r) => r.isHighestConsumption === true).length;
    await updateRfdDraft(fx, rfdDraftId, {
      podWithHighestConsumption: true,
      excludePodIds: [ctx.pods[1].id],
    });
    const checked = await fetchCheckedPods(fx, ctx.reminderId, ctx.gridOperatorId, rfdDraftId);
    if (yesCount >= 1) {
      expect(checked.every((p) => p.isHighestConsumption === true)).toBe(true);
      expect(checked.some((p) => p.podId === ctx.pods[2].id)).toBe(true);
    } else {
      expect(checked.length).toBe(0);
    }
    attachSummary(TestRunSummary, Responses, 'TC-BE-15 bulk select invariant', 'No NO rows checked; empty when yesCount=0', `yesCount=${yesCount} checked=${checked.length}`, true, { rfdDraftId, yesCount });
  });
});
