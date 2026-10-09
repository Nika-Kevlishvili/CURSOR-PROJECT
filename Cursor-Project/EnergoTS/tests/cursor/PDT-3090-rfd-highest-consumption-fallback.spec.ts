/**
 * PDT-3090 — Request for disconnection: Point of Delivery with Highest Consumption
 * must mark YES on the listed POD with the biggest pod.id when the billing group
 * has no invoice POD total volumes. When invoice volumes exist, that fallback
 * must not apply. Select highest consumption must check the YES POD.
 *
 * Dev2. Creates all data from scratch. No test.beforeAll.
 * Tests encode documented product intent (fail vs spec if Dev2 still returns all false).
 *
 * Reference spec(s):
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 * - tests/cursor/PDT-3421-rfd-highest-consumption-uncheck.spec.ts
 * - tests/cursor/pdt-3421-rfd-highest-consumption-uncheck.fixtures.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/pdt-2937-invoice-detailed-data-same-pc-name.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/pdt-3042-cancellation-pod-auto-check.fixtures.ts
 * - tests/cursor/pdt-2459-payment-reverse-lpf-offset.fixtures.ts
 * - tests/receivableManagement/Rescheduling/happyPass.spec.ts
 * - jsons/payloads/create/Receivables/reschedulingCreate.ts
 * - jsons/payloads/create/contractOrders/action.ts
 * - jsons/payloads/create/Receivables/customerLiability.ts
 * - jsons/payloads/create/energyData/profile1Month.ts
 * - jsons/payloads/create/billing/forVolumes.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3090_BE11_TIMEOUT_MS,
  PDT_3090_BE13_TIMEOUT_MS,
  PDT_3090_BE2_TIMEOUT_MS,
  PDT_3090_EQUAL_VOLUME_VALUE,
  PDT_3090_KEY,
  PDT_3090_TITLE,
  addAndActivateExtraPodsOnSameBillingGroup,
  addAndActivateExtraPodsOnSameBillingGroupAt,
  calendarMonthPeriod,
  changeLiabilityDueDateYesterday,
  createDraftRfdWithHighestFilterOn,
  createExecutedRfdWithHighestFilterOn,
  createExecutedReschedulingForSourceLiability,
  createExternalOutgoingDocumentLiability,
  createOverdueManualLiabilityWithBillingGroup,
  createSupplyChain,
  createSupplyChainWithSettlementPriceComponent,
  createdReminderId,
  customerIdentifier,
  entityId,
  ensureExecutedReminder,
  findReconnectionTaxLiability,
  getCheckedPodIds,
  isCheckedFlag,
  isHighestConsumptionFlag,
  listActiveGridOperatorTaxId,
  loadCustomersForDps,
  makeInvoiceLiabilityOverdue,
  monthStartYmd,
  payOffLiabilityFully,
  pdt3090RelevantKeys,
  postBillingByProfileForLowAndHigh,
  postBillingByProfileForPods,
  postClaimedPenaltyWithBillingGroup,
  postExecutedDpsForPod,
  postPenaltyActionAndClaim,
  preparePdt3090LpfOnlyLiability,
  previousMonthEndYmd,
  previousMonthStartYmd,
  realizeForVolumesInvoiceWithSettlementFallback,
  resolveLowHighPods,
  resolveProductContractBillingGroupId,
  rowPodId,
  rowsForPodIds,
  sumInvoiceTotalVolumesByPod,
  todayYmd,
  viewPodTab,
  type Pdt3090Fx,
} from './pdt-3090-rfd-highest-consumption-fallback.fixtures';

const JIRA_TITLE = PDT_3090_TITLE;

test.describe(`[${PDT_3090_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@dev2', '@receivableManagement', '@pdt-3090'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-1: No invoice POD volumes — biggest pod.id YES', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE13_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain (customer, product, contract, POD #1)', async () => {
      await createSupplyChain(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'TC-BE-1 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Resolve podId Low / HighId (identity is pod.id)', async () => {
      const resolved = await resolveLowHighPods(fx);
      TestRunSummary.registerPayload('pods', {
        lowPodId: resolved.low.podId,
        highPodId: resolved.high.podId,
        lowIdentifier: resolved.low.podIdentifier,
        highIdentifier: resolved.high.podIdentifier,
      });
      return resolved;
    });

    await test.step('Precondition: overdue manual liability with billingGroupId (no billing invoice)', async () => {
      const liability = await createOverdueManualLiabilityWithBillingGroup(fx);
      TestRunSummary.registerPayload('customerLiability', liability);
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (INCLUDED this customer, no /job)',
      async () => {
        const id = await ensureExecutedReminder(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', { reminderId: id });
        return id;
      },
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply (LIST_OF_CUSTOMERS)',
      async () => {
        const result = await loadCustomersForDps(fx, reminderId);
        TestRunSummary.registerPayload('loadCustomers', {
          status: result.status,
          rowCount: result.rows.length,
        });
        return result;
      },
    );

    await test.step('Assert: HTTP 206 and exactly one YES on max pod.id', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const maxPodId = Math.max(...listed.map(rowPodId));
      const yesRows = listed.filter(isHighestConsumptionFlag);
      const yesPodId = yesRows[0] ? rowPodId(yesRows[0]) : 0;
      const noRows = listed.filter((row) => !isHighestConsumptionFlag(row));
      const passed =
        listed.length >= 2 &&
        yesRows.length === 1 &&
        yesPodId === maxPodId &&
        noRows.length === listed.length - 1 &&
        noRows.every((row) => row.isHighestConsumption === false);

      TestRunSummary.recordCheck({
        check: 'No invoice volumes — biggest pod.id isHighestConsumption YES',
        expectedResult:
          'HTTP 206. Exactly one listed row isHighestConsumption=true and that row.podId equals max(podId). Other listed rows false.',
        actualResult: passed
          ? `As expected — HTTP ${load.status}; yesPodId=${yesPodId}; maxPodId=${maxPodId}; listed=${listed.map((r) => `${rowPodId(r)}:${String(r.isHighestConsumption)}`).join(', ')}`
          : `Not as expected — HTTP ${load.status}; listed=${listed.map((r) => `${rowPodId(r)}:${String(r.isHighestConsumption)}`).join(', ')} (fail vs spec if all false)`,
        passed,
      });

      expect(listed.length, 'Load PODs must return both PODs for this customer').toBeGreaterThanOrEqual(2);
      expect(yesRows.length, 'exactly one isHighestConsumption===true among listed PODs').toBe(1);
      expect(yesPodId, 'YES row podId must equal max listed pod.id').toBe(maxPodId);
      for (const row of listed) {
        if (rowPodId(row) === maxPodId) {
          expect(row.isHighestConsumption, `podId ${maxPodId} isHighestConsumption`).toBe(true);
        } else {
          expect(
            row.isHighestConsumption,
            `podId ${rowPodId(row)} must be isHighestConsumption===false`,
          ).toBe(false);
        }
      }
    });

    await test.step('Attach test run summary', async () => {
      let extra: Record<string, string[]> | undefined;
      try {
        extra = buildProductContractTabLinks(entityId(Responses.productContract[0]));
      } catch {
        extra = undefined;
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys(),
        extraLinks: extra && Object.keys(extra).length ? extra : undefined,
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          lowPodId: pair.low.podId,
          highPodId: pair.high.podId,
          loadStatus: load.status,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-2: Invoice volumes present — fallback must not apply', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE2_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain with settlement FOR_VOLUMES price component', async () => {
      await createSupplyChainWithSettlementPriceComponent(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'TC-BE-2 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Identify podLow = min(podId), podHighId = max(podId)', async () => {
      const resolved = await resolveLowHighPods(fx);
      TestRunSummary.registerPayload('pods', {
        lowPodId: resolved.low.podId,
        highPodId: resolved.high.podId,
        lowIdentifier: resolved.low.podIdentifier,
        highIdentifier: resolved.high.podIdentifier,
      });
      return resolved;
    });

    const profile = await test.step(
      'POST billing-by-profile for POD Low value=500 and POD HighId value=1',
      async () => {
        const posted = await postBillingByProfileForLowAndHigh(fx, pair.low, pair.high);
        TestRunSummary.registerPayload('billingByProfile', posted);
        return posted;
      },
    );

    const invoice = await test.step(
      'POST STANDARD_BILLING FOR_VOLUMES and wait for realized invoice',
      async () => {
        const realized = await realizeForVolumesInvoiceWithSettlementFallback(
          fx,
          pair.low,
          pair.high,
          profile.periodType,
        );
        TestRunSummary.registerPayload('invoice', { invoiceId: realized.invoiceId, periodType: realized.periodType });
        return realized;
      },
    );

    await test.step('GET invoice/detailed-data — sum totalVolumes Low > HighId (HighId value=1)', async () => {
      const sumLow = await sumInvoiceTotalVolumesByPod(fx, invoice.invoiceId, pair.low.podIdentifier);
      const sumHigh = await sumInvoiceTotalVolumesByPod(fx, invoice.invoiceId, pair.high.podIdentifier);
      const passed = sumLow > sumHigh && sumHigh > 0;
      TestRunSummary.recordCheck({
        check: 'Invoice 3rd-tab totalVolumes: Low > HighId and HighId listed (value=1)',
        expectedResult: `sum(totalVolumes) for ${pair.low.podIdentifier} > ${pair.high.podIdentifier}; HighId totalVolumes > 0 so Load PODs includes HighId`,
        actualResult: passed
          ? `As expected — sumLow=${sumLow} sumHigh=${sumHigh}`
          : `Not as expected — sumLow=${sumLow} sumHigh=${sumHigh}`,
        passed,
      });
      expect(sumHigh, 'POD HighId invoice totalVolumes must be > 0 so HighId is listed').toBeGreaterThan(0);
      expect(sumLow, 'POD Low invoice totalVolumes').toBeGreaterThan(sumHigh);
    });

    await test.step('PUT customer-liability/{id}/due-date-change?dueDate=yesterday', async () => {
      const liabilityId = await makeInvoiceLiabilityOverdue(fx, invoice.invoiceId);
      TestRunSummary.registerPayload('overdueInvoiceLiability', { liabilityId });
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (INCLUDED this customer, no /job)',
      async () => {
        const id = await ensureExecutedReminder(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', { reminderId: id });
        return id;
      },
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply (LIST_OF_CUSTOMERS)',
      async () => {
        return loadCustomersForDps(fx, reminderId);
      },
    );

    await test.step('Assert: Low YES, HighId NO even though HighId has larger pod.id', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const lowRow = listed.find((row) => rowPodId(row) === pair.low.podId);
      const highRow = listed.find((row) => rowPodId(row) === pair.high.podId);
      const lowYes = isHighestConsumptionFlag(lowRow);
      const highNo = highRow?.isHighestConsumption === false;
      const passed = Boolean(lowRow) && Boolean(highRow) && lowYes && highNo;

      TestRunSummary.recordCheck({
        check: 'Invoice volumes present — biggest-podId fallback must not apply',
        expectedResult:
          'HTTP 206. POD Low isHighestConsumption=true. POD HighId isHighestConsumption=false even though podIdHighId > podIdLow.',
        actualResult: passed
          ? `As expected — HTTP ${load.status}; Low ${pair.low.podId}=${String(lowRow?.isHighestConsumption)}; HighId ${pair.high.podId}=${String(highRow?.isHighestConsumption)}`
          : `Not as expected — HTTP ${load.status}; Low=${String(lowRow?.isHighestConsumption)} HighId=${String(highRow?.isHighestConsumption)}`,
        passed,
      });

      expect(lowRow, `Load PODs must include POD Low ${pair.low.podId}`).toBeTruthy();
      expect(highRow, `Load PODs must include POD HighId ${pair.high.podId}`).toBeTruthy();
      expect(lowRow?.isHighestConsumption, 'POD Low isHighestConsumption').toBe(true);
      expect(highRow?.isHighestConsumption, 'POD HighId isHighestConsumption must stay false').toBe(false);
    });

    await test.step('Attach test run summary', async () => {
      let extra: Record<string, string[]> | undefined;
      try {
        extra = buildProductContractTabLinks(entityId(Responses.productContract[0]));
      } catch {
        extra = undefined;
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys({ includeInvoice: true }),
        extraLinks: extra && Object.keys(extra).length ? extra : undefined,
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          lowPodId: pair.low.podId,
          highPodId: pair.high.podId,
          invoiceId: invoice.invoiceId,
          profilePeriodType: invoice.periodType,
          loadStatus: load.status,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-3: Select highest consumption checks fallback YES POD', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE13_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain (customer, product, contract, POD #1)', async () => {
      await createSupplyChain(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'TC-BE-3 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Resolve maxPodId among this customer PODs', async () => {
      const resolved = await resolveLowHighPods(fx);
      TestRunSummary.registerPayload('pods', {
        maxPodId: resolved.high.podId,
        otherPodId: resolved.low.podId,
      });
      return resolved;
    });
    const maxPodId = pair.high.podId;
    const otherPodId = pair.low.podId;

    await test.step('Precondition: overdue manual liability with billingGroupId (no billing invoice)', async () => {
      await createOverdueManualLiabilityWithBillingGroup(fx);
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (INCLUDED this customer, no /job)',
      async () => {
        const id = await ensureExecutedReminder(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', { reminderId: id });
        return id;
      },
    );

    await test.step('GET Load PODs — spec: maxPodId row is already isHighestConsumption=true', async () => {
      const load = await loadCustomersForDps(fx, reminderId);
      const listed = rowsForPodIds(load.rows, [maxPodId, otherPodId]);
      const maxRow = listed.find((row) => rowPodId(row) === maxPodId);
      TestRunSummary.registerPayload('loadCustomersBeforeRfd', {
        status: load.status,
        flags: listed.map((row) => ({ podId: rowPodId(row), isHighestConsumption: row.isHighestConsumption })),
      });
      expect(maxRow, `Load PODs must include maxPodId ${maxPodId}`).toBeTruthy();
      expect(
        maxRow?.isHighestConsumption,
        'spec: max pod.id isHighestConsumption=true before select-highest (fail vs spec if all false)',
      ).toBe(true);
    });

    const rfdId = await test.step(
      'POST DRAFT RFD podWithHighestConsumption=true, LIST_OF_CUSTOMERS, validityPeriodFrom/To',
      async () => {
        const id = await createDraftRfdWithHighestFilterOn(fx, reminderId);
        TestRunSummary.registerPayload('rfdDraft', {
          id,
          podWithHighestConsumption: true,
          allSelected: false,
          conditionType: 'LIST_OF_CUSTOMERS',
        });
        expect(id).toBeGreaterThan(0);
        return id;
      },
    );

    await test.step('GET get-checked-pods — contains maxPodId only among this customer PODs', async () => {
      const checkedIds = await getCheckedPodIds(fx, reminderId, rfdId);
      const hasMax = checkedIds.includes(maxPodId);
      const hasOther = checkedIds.includes(otherPodId);
      const passed = hasMax && !hasOther;
      TestRunSummary.recordCheck({
        check: 'Select highest consumption checks the fallback YES POD only',
        expectedResult: `checked contains maxPodId ${maxPodId} and does not contain ${otherPodId}`,
        actualResult: passed
          ? `As expected — checked=[${checkedIds.join(', ')}]`
          : `Not as expected — checked=[${checkedIds.join(', ')}] (empty list is fail vs spec / PDT-2868)`,
        passed,
      });
      expect(hasMax, `checked PODs ${checkedIds.join(', ')} must contain maxPodId ${maxPodId}`).toBe(true);
      expect(hasOther, `checked PODs must not contain the other POD ${otherPodId}`).toBe(false);
    });

    await test.step('GET view-pod-tab — maxPodId YES+checked; other POD both false', async () => {
      const tab = await viewPodTab(fx, rfdId, reminderId);
      const listed = rowsForPodIds(tab.rows, [maxPodId, otherPodId]);
      const maxRow = listed.find((row) => rowPodId(row) === maxPodId);
      const otherRow = listed.find((row) => rowPodId(row) === otherPodId);
      const passed =
        isHighestConsumptionFlag(maxRow) &&
        isCheckedFlag(maxRow) &&
        otherRow?.isHighestConsumption === false &&
        otherRow?.isChecked === false;

      TestRunSummary.recordCheck({
        check: 'view-pod-tab persists YES+checked on max pod.id only',
        expectedResult:
          'HTTP 206. maxPodId isHighestConsumption=true and isChecked=true; other POD both false.',
        actualResult: passed
          ? `As expected — HTTP ${tab.status}; max=${String(maxRow?.isHighestConsumption)}/${String(maxRow?.isChecked)}; other=${String(otherRow?.isHighestConsumption)}/${String(otherRow?.isChecked)}`
          : `Not as expected — HTTP ${tab.status}; max=${JSON.stringify({ h: maxRow?.isHighestConsumption, c: maxRow?.isChecked })} other=${JSON.stringify({ h: otherRow?.isHighestConsumption, c: otherRow?.isChecked })}`,
        passed,
      });

      expect(maxRow, `view-pod-tab must include maxPodId ${maxPodId}`).toBeTruthy();
      expect(otherRow, `view-pod-tab must include other POD ${otherPodId}`).toBeTruthy();
      expect(maxRow?.isHighestConsumption, 'maxPodId isHighestConsumption').toBe(true);
      expect(maxRow?.isChecked, 'maxPodId isChecked').toBe(true);
      expect(otherRow?.isHighestConsumption, 'other POD isHighestConsumption').toBe(false);
      expect(otherRow?.isChecked, 'other POD isChecked').toBe(false);
    });

    await test.step('Attach test run summary', async () => {
      TestRunSummary.registerPayload('requestForDisconnection', rfdId);
      let extra: Record<string, string[]> | undefined;
      try {
        extra = buildProductContractTabLinks(entityId(Responses.productContract[0]));
      } catch {
        extra = undefined;
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys({ includeRfd: true }),
        extraLinks: extra && Object.keys(extra).length ? extra : undefined,
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId: createdReminderId(fx),
          rfdId,
          maxPodId,
          otherPodId,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-4: LPF-only — biggest pod.id YES', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE2_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain (customer, product, contract, POD #1)', async () => {
      await createSupplyChain(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'TC-BE-4 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Resolve podId Low / HighId (identity is pod.id)', async () => {
      const resolved = await resolveLowHighPods(fx);
      TestRunSummary.registerPayload('pods', {
        lowPodId: resolved.low.podId,
        highPodId: resolved.high.podId,
      });
      return resolved;
    });

    await test.step('Precondition: LPF from paid overdue source liability with billingGroupId', async () => {
      const lpf = await preparePdt3090LpfOnlyLiability(fx);
      TestRunSummary.registerPayload('latePaymentFine', lpf);
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => {
        const id = await ensureExecutedReminder(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', { reminderId: id });
        return id;
      },
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply (LIST_OF_CUSTOMERS)',
      async () => loadCustomersForDps(fx, reminderId, 'LATE_PAYMENT_FINE'),
    );

    await test.step('Assert: HTTP 206 and exactly one YES on max pod.id (LPF-only)', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const maxPodId = Math.max(pair.low.podId, pair.high.podId);
      const yesRows = listed.filter(isHighestConsumptionFlag);
      const yesPodId = yesRows[0] ? rowPodId(yesRows[0]) : 0;
      const passed =
        listed.length >= 2 && yesRows.length === 1 && yesPodId === maxPodId;
      TestRunSummary.recordCheck({
        check: 'LPF-only — biggest pod.id isHighestConsumption YES',
        expectedResult:
          'HTTP 206. Both PODs listed. maxPodId isHighestConsumption=true; the other POD false. Empty list is fail vs PDT-3090.',
        actualResult: passed
          ? `As expected — HTTP ${load.status}; yesPodId=${yesPodId}; maxPodId=${maxPodId}`
          : `Not as expected — HTTP ${load.status}; listed=${listed.map((r) => `${rowPodId(r)}:${String(r.isHighestConsumption)}`).join(', ') || 'zero rows'}`,
        passed,
      });
      expect(
        listed.length,
        'Load PODs must return both PODs for this LPF customer (empty list is fail vs PDT-3090 / Create 72155868)',
      ).toBeGreaterThanOrEqual(2);
      expect(yesRows.length, 'exactly one isHighestConsumption===true').toBe(1);
      expect(yesPodId, 'YES row podId must equal max listed pod.id').toBe(maxPodId);
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys(),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          lowPodId: pair.low.podId,
          highPodId: pair.high.podId,
          loadStatus: load.status,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-5: External outgoing-document liability — biggest pod.id YES', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE13_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain (customer, product, contract, POD #1)', async () => {
      await createSupplyChain(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'TC-BE-5 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Resolve podId Low / HighId (identity is pod.id)', async () => {
      return resolveLowHighPods(fx);
    });

    await test.step('POST /customer-liability outgoingDocumentFromExternalSystem=EXT-PDT3090-1 + billingGroupId', async () => {
      const created = await createExternalOutgoingDocumentLiability(fx);
      TestRunSummary.registerPayload('customerLiability', created);
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => ensureExecutedReminder(fx),
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply (LIST_OF_CUSTOMERS)',
      async () => loadCustomersForDps(fx, reminderId),
    );

    await test.step('Assert: HTTP 206 and exactly one YES on max pod.id (external doc)', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const maxPodId = Math.max(pair.low.podId, pair.high.podId);
      const yesRows = listed.filter(isHighestConsumptionFlag);
      const yesPodId = yesRows[0] ? rowPodId(yesRows[0]) : 0;
      const passed = listed.length >= 2 && yesRows.length === 1 && yesPodId === maxPodId;
      TestRunSummary.recordCheck({
        check: 'External outgoing-document — biggest pod.id YES',
        expectedResult: 'HTTP 206. maxPodId isHighestConsumption=true; other POD false.',
        actualResult: passed
          ? `As expected — HTTP ${load.status}; yesPodId=${yesPodId}`
          : `Not as expected — HTTP ${load.status}; listed=${listed.map((r) => `${rowPodId(r)}:${String(r.isHighestConsumption)}`).join(', ') || 'zero rows'}`,
        passed,
      });
      expect(
        listed.length,
        'Load PODs must return both PODs (empty list is fail vs Create 72155868)',
      ).toBeGreaterThanOrEqual(2);
      expect(yesPodId).toBe(maxPodId);
      expect(yesRows.length).toBe(1);
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys(),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          lowPodId: pair.low.podId,
          highPodId: pair.high.podId,
          loadStatus: load.status,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-6: Rescheduling instalment-only — biggest pod.id YES', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE2_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain (customer, product, contract, POD #1)', async () => {
      await createSupplyChain(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'TC-BE-6 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Resolve podId Low / HighId (identity is pod.id)', async () => {
      return resolveLowHighPods(fx);
    });

    await test.step('POST customer-assessment, calculate-rescheduling, POST /rescheduling EXECUTED; overdue instalment', async () => {
      const created = await createExecutedReschedulingForSourceLiability(fx);
      TestRunSummary.registerPayload('rescheduling', created);
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => ensureExecutedReminder(fx),
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply (LIST_OF_CUSTOMERS)',
      async () => loadCustomersForDps(fx, reminderId, 'RESCHEDULING'),
    );

    await test.step('Assert: HTTP 206 and exactly one YES on max pod.id (rescheduling)', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const maxPodId = Math.max(pair.low.podId, pair.high.podId);
      const yesRows = listed.filter(isHighestConsumptionFlag);
      const yesPodId = yesRows[0] ? rowPodId(yesRows[0]) : 0;
      const passed = listed.length >= 2 && yesRows.length === 1 && yesPodId === maxPodId;
      TestRunSummary.recordCheck({
        check: 'Rescheduling instalment-only — biggest pod.id YES',
        expectedResult: 'HTTP 206. maxPodId isHighestConsumption=true; other POD false.',
        actualResult: passed
          ? `As expected — HTTP ${load.status}; yesPodId=${yesPodId}`
          : `Not as expected — HTTP ${load.status}; listed=${listed.map((r) => `${rowPodId(r)}:${String(r.isHighestConsumption)}`).join(', ') || 'zero rows (check instalment BG / Confluence 77136161)'}`,
        passed,
      });
      expect(
        listed.length,
        'Load PODs must return both PODs (empty list is fail vs Create 72155868 / Confluence 77136161)',
      ).toBeGreaterThanOrEqual(2);
      expect(yesPodId).toBe(maxPodId);
      expect(yesRows.length).toBe(1);
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys(),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          lowPodId: pair.low.podId,
          highPodId: pair.high.podId,
          loadStatus: load.status,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-7: Reconnection-tax liability — biggest pod.id YES', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE2_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain (customer, product, contract, POD #1)', async () => {
      await createSupplyChain(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'TC-BE-7 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Resolve maxPodId among this customer PODs', async () => {
      return resolveLowHighPods(fx);
    });
    const maxPodId = pair.high.podId;

    const manual = await test.step('Precondition: overdue manual liability (for first RFD only)', async () => {
      return createOverdueManualLiabilityWithBillingGroup(fx);
    });

    const reminderIdR1 = await test.step('Precondition: reminder R1 EXECUTED', async () => {
      return ensureExecutedReminder(fx);
    });

    const taxId = await test.step('GET tax-for-the-grid-operator ACTIVE list', async () => {
      const id = await listActiveGridOperatorTaxId(fx);
      TestRunSummary.registerPayload('gridOperatorTaxesId', { taxId: id });
      return id;
    });

    const rfdId1 = await test.step('POST EXECUTED RFD podWithHighestConsumption=true', async () => {
      const id = await createExecutedRfdWithHighestFilterOn(fx, reminderIdR1);
      TestRunSummary.registerPayload('rfdExecuted', { id });
      expect(id).toBeGreaterThan(0);
      return id;
    });

    await test.step('POST /disconnection-of-power-supply saveType=EXECUTED', async () => {
      const dpsId = await postExecutedDpsForPod(fx, rfdId1, maxPodId, taxId);
      TestRunSummary.registerPayload('disconnectionOfPowerSupply', { dpsId });
    });

    const taxLiabilityId = await test.step('Find reconnection-tax liability (fail if missing)', async () => {
      const id = await findReconnectionTaxLiability(fx, manual.liabilityId);
      TestRunSummary.registerPayload('reconnectionTaxLiability', { taxLiabilityId: id });
      return id;
    });

    await test.step('Pay off original manual liability so R2 is tax-only', async () => {
      await payOffLiabilityFully(fx, manual.liabilityId);
    });

    await test.step('Overdue reconnection-tax liability if needed', async () => {
      await changeLiabilityDueDateYesterday(fx, taxLiabilityId);
    });

    const reminderIdR2 = await test.step('Precondition: reminder R2 EXECUTED (tax-only)', async () => {
      const id = await ensureExecutedReminder(fx);
      TestRunSummary.registerPayload('reminderR2', { reminderId: id });
      return id;
    });

    const load = await test.step(
      'GET Load PODs with reminder R2 (not R1)',
      async () => loadCustomersForDps(fx, reminderIdR2, 'RECONNECTION_TAX'),
    );

    await test.step('Assert: HTTP 206 and exactly one YES on max pod.id (reconnection-tax)', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const yesRows = listed.filter(isHighestConsumptionFlag);
      const yesPodId = yesRows[0] ? rowPodId(yesRows[0]) : 0;
      const passed = listed.length >= 2 && yesRows.length === 1 && yesPodId === maxPodId;
      TestRunSummary.recordCheck({
        check: 'Reconnection-tax only — biggest pod.id YES',
        expectedResult: 'HTTP 206. maxPodId isHighestConsumption=true; other POD false. Uses reconnection-tax, not POST /tax-for-the-grid-operator as a liability.',
        actualResult: passed
          ? `As expected — HTTP ${load.status}; yesPodId=${yesPodId}`
          : `Not as expected — HTTP ${load.status}; listed=${listed.map((r) => `${rowPodId(r)}:${String(r.isHighestConsumption)}`).join(', ') || 'zero rows'}`,
        passed,
      });
      expect(
        listed.length,
        'Load PODs must return both PODs for reconnection-tax R2 (empty list is fail vs Create 72155868)',
      ).toBeGreaterThanOrEqual(2);
      expect(yesPodId).toBe(maxPodId);
      expect(yesRows.length).toBe(1);
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys({ includeRfd: true }),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderIdR1,
          reminderIdR2,
          rfdId1,
          taxLiabilityId,
          maxPodId,
          loadStatus: load.status,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-8: Action / claimed-penalty-only — biggest pod.id YES', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE13_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain (customer, product, contract, POD #1)', async () => {
      await createSupplyChain(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'TC-BE-8 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Resolve podId Low / HighId (identity is pod.id)', async () => {
      return resolveLowHighPods(fx);
    });
    const billingGroupId = await test.step('Resolve billingGroupId', async () => {
      return resolveProductContractBillingGroupId(fx);
    });

    await test.step('POST /actions with penaltyClaimAmount then POST /actions/{id}/penalty/claim', async () => {
      const claimed = await postPenaltyActionAndClaim(fx, billingGroupId);
      TestRunSummary.registerPayload('actionClaimedPenalty', claimed);
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => ensureExecutedReminder(fx),
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply (LIST_OF_CUSTOMERS)',
      async () => loadCustomersForDps(fx, reminderId, 'PENALTY'),
    );

    await test.step('Assert: HTTP 206 and YES on max pod.id (action/claimed-penalty)', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const maxPodId = Math.max(pair.low.podId, pair.high.podId);
      const yesRows = listed.filter(isHighestConsumptionFlag);
      const yesPodId = yesRows[0] ? rowPodId(yesRows[0]) : 0;
      const passed = listed.length >= 2 && yesRows.length === 1 && yesPodId === maxPodId;
      TestRunSummary.recordCheck({
        check: 'Action/claimed-penalty-only — biggest pod.id YES (PDT-3090 / Create 72155868)',
        expectedResult:
          'HTTP 206. Both PODs listed. maxPodId YES. Zero rows is fail vs PDT-3090 (that outcome matches page 393248776 — do not treat as pass).',
        actualResult: passed
          ? `As expected — HTTP ${load.status}; yesPodId=${yesPodId}`
          : `Not as expected — HTTP ${load.status}; listed=${listed.map((r) => `${rowPodId(r)}:${String(r.isHighestConsumption)}`).join(', ') || 'zero rows'}`,
        passed,
      });
      expect(
        listed.length,
        'Load PODs zero rows for claimed-penalty is fail vs PDT-3090 TO-BE (matches wiki 393248776 conflict — do not pass empty list)',
      ).toBeGreaterThanOrEqual(2);
      expect(yesPodId).toBe(maxPodId);
      expect(yesRows.length).toBe(1);
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys(),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          billingGroupId,
          lowPodId: pair.low.podId,
          highPodId: pair.high.podId,
          loadStatus: load.status,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-9: Mixed FOR_VOLUMES invoice + action — volumes win', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE2_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain with settlement FOR_VOLUMES price component', async () => {
      await createSupplyChainWithSettlementPriceComponent(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroupAt(fx, 1, monthStartYmd());
      expect(Responses.pod.length, 'TC-BE-9 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Identify podLow = min(podId), podHighId = max(podId)', async () => {
      return resolveLowHighPods(fx);
    });
    const billingGroupId = await resolveProductContractBillingGroupId(fx);

    const profile = await test.step(
      'POST billing-by-profile for POD Low value=500 and POD HighId value=1',
      async () => postBillingByProfileForLowAndHigh(fx, pair.low, pair.high),
    );

    const invoice = await test.step(
      'POST STANDARD_BILLING FOR_VOLUMES and wait for realized invoice',
      async () => realizeForVolumesInvoiceWithSettlementFallback(fx, pair.low, pair.high, profile.periodType),
    );

    await test.step('GET invoice/detailed-data — sum totalVolumes Low > HighId', async () => {
      const sumLow = await sumInvoiceTotalVolumesByPod(fx, invoice.invoiceId, pair.low.podIdentifier);
      const sumHigh = await sumInvoiceTotalVolumesByPod(fx, invoice.invoiceId, pair.high.podIdentifier);
      const passed = sumLow > sumHigh && sumHigh > 0;
      TestRunSummary.recordCheck({
        check: 'Invoice 3rd-tab totalVolumes: Low > HighId',
        expectedResult: 'sumLow > sumHigh and sumHigh > 0',
        actualResult: passed
          ? `As expected — sumLow=${sumLow} sumHigh=${sumHigh}`
          : `Not as expected — sumLow=${sumLow} sumHigh=${sumHigh}`,
        passed,
      });
      expect(sumHigh).toBeGreaterThan(0);
      expect(sumLow).toBeGreaterThan(sumHigh);
    });

    await test.step('PUT invoice liability due-date-change?dueDate=yesterday', async () => {
      await makeInvoiceLiabilityOverdue(fx, invoice.invoiceId);
    });

    await test.step('POST /claimed-penalty same billingGroupId', async () => {
      const penaltyId = await postClaimedPenaltyWithBillingGroup(fx, billingGroupId);
      TestRunSummary.registerPayload('claimedPenalty', { penaltyId, billingGroupId });
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => ensureExecutedReminder(fx),
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply (LIST_OF_CUSTOMERS)',
      async () => loadCustomersForDps(fx, reminderId, 'PRODUCT_INVOICE_DEBIT_NOTE'),
    );

    await test.step('Assert: Low YES, HighId NO (volumes win over action)', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const lowRow = listed.find((row) => rowPodId(row) === pair.low.podId);
      const highRow = listed.find((row) => rowPodId(row) === pair.high.podId);
      const passed =
        Boolean(lowRow) &&
        Boolean(highRow) &&
        isHighestConsumptionFlag(lowRow) &&
        highRow?.isHighestConsumption === false;
      TestRunSummary.recordCheck({
        check: 'Mixed invoice + action — volumes win; biggest pod.id stays NO',
        expectedResult:
          'HTTP 206. POD Low isHighestConsumption=true. POD HighId false even though podIdHighId > podIdLow and claimed-penalty exists on the same BG.',
        actualResult: passed
          ? `As expected — Low ${pair.low.podId}=${String(lowRow?.isHighestConsumption)}; HighId ${pair.high.podId}=${String(highRow?.isHighestConsumption)}`
          : `Not as expected — Low=${String(lowRow?.isHighestConsumption)} HighId=${String(highRow?.isHighestConsumption)}`,
        passed,
      });
      expect(lowRow, `Load PODs must include POD Low ${pair.low.podId}`).toBeTruthy();
      expect(highRow, `Load PODs must include POD HighId ${pair.high.podId}`).toBeTruthy();
      expect(lowRow?.isHighestConsumption, 'POD Low isHighestConsumption').toBe(true);
      expect(highRow?.isHighestConsumption, 'POD HighId must stay false').toBe(false);
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys({ includeInvoice: true }),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          invoiceId: invoice.invoiceId,
          billingGroupId,
          lowPodId: pair.low.podId,
          highPodId: pair.high.podId,
          loadStatus: load.status,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-10: Equal invoice totalVolumes — biggest pod.id YES', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE2_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain with settlement FOR_VOLUMES price component', async () => {
      await createSupplyChainWithSettlementPriceComponent(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second settlement POD on the same billing group', async () => {
      await addAndActivateExtraPodsOnSameBillingGroupAt(fx, 1, monthStartYmd());
      expect(Responses.pod.length, 'TC-BE-10 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Identify podLow = min(podId), podHighId = max(podId)', async () => {
      return resolveLowHighPods(fx);
    });

    const profile = await test.step(
      'POST billing-by-profile for both PODs value=100',
      async () =>
        postBillingByProfileForPods(fx, pair.low, pair.high, {
          lowValue: PDT_3090_EQUAL_VOLUME_VALUE,
          highValue: PDT_3090_EQUAL_VOLUME_VALUE,
        }),
    );

    const invoice = await test.step(
      'POST STANDARD_BILLING FOR_VOLUMES and wait for realized invoice',
      async () =>
        realizeForVolumesInvoiceWithSettlementFallback(fx, pair.low, pair.high, profile.periodType, {
          lowValue: PDT_3090_EQUAL_VOLUME_VALUE,
          highValue: PDT_3090_EQUAL_VOLUME_VALUE,
        }),
    );

    await test.step('GET invoice/detailed-data — sumLow === sumHigh and both > 0', async () => {
      const sumLow = await sumInvoiceTotalVolumesByPod(fx, invoice.invoiceId, pair.low.podIdentifier);
      const sumHigh = await sumInvoiceTotalVolumesByPod(fx, invoice.invoiceId, pair.high.podIdentifier);
      const passed = sumLow === sumHigh && sumLow > 0 && sumHigh > 0;
      TestRunSummary.recordCheck({
        check: 'Equal invoice totalVolumes sums',
        expectedResult: 'sumLow === sumHigh and both > 0 (example each ≥ 100)',
        actualResult: passed
          ? `As expected — sumLow=${sumLow} sumHigh=${sumHigh}`
          : `Not as expected — sumLow=${sumLow} sumHigh=${sumHigh}`,
        passed,
      });
      expect(sumLow, 'equal-sum Low totalVolumes').toBeGreaterThan(0);
      expect(sumHigh, 'equal-sum HighId totalVolumes').toBe(sumLow);
    });

    await test.step('PUT invoice liability due-date-change?dueDate=yesterday', async () => {
      await makeInvoiceLiabilityOverdue(fx, invoice.invoiceId);
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => ensureExecutedReminder(fx),
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply (LIST_OF_CUSTOMERS)',
      async () => loadCustomersForDps(fx, reminderId, 'PRODUCT_INVOICE_DEBIT_NOTE'),
    );

    await test.step('Assert: HighId YES, Low NO (equal-sum tie-break)', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const lowRow = listed.find((row) => rowPodId(row) === pair.low.podId);
      const highRow = listed.find((row) => rowPodId(row) === pair.high.podId);
      const passed =
        Boolean(lowRow) &&
        Boolean(highRow) &&
        isHighestConsumptionFlag(highRow) &&
        lowRow?.isHighestConsumption === false;
      TestRunSummary.recordCheck({
        check: 'Equal totalVolumes — YES on biggest pod.id',
        expectedResult: 'HTTP 206. POD HighId isHighestConsumption=true. POD Low false.',
        actualResult: passed
          ? `As expected — HighId ${pair.high.podId}=true Low ${pair.low.podId}=false`
          : `Not as expected — HighId=${String(highRow?.isHighestConsumption)} Low=${String(lowRow?.isHighestConsumption)}`,
        passed,
      });
      expect(highRow, `Load PODs must include POD HighId ${pair.high.podId}`).toBeTruthy();
      expect(lowRow, `Load PODs must include POD Low ${pair.low.podId}`).toBeTruthy();
      expect(highRow?.isHighestConsumption, 'POD HighId isHighestConsumption').toBe(true);
      expect(lowRow?.isHighestConsumption, 'POD Low isHighestConsumption must be false').toBe(false);
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys({ includeInvoice: true }),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          invoiceId: invoice.invoiceId,
          lowPodId: pair.low.podId,
          highPodId: pair.high.podId,
          loadStatus: load.status,
        },
      });
    });
  });

  test('[PDT-3090]: Request for disconnection - Point of delivery with highest consumption incorrect value | TC-BE-11: Two FOR_VOLUMES invoices — YES follows summed totalVolumes', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3090_BE11_TIMEOUT_MS);
    const fx: Pdt3090Fx = { Request, GeneratePayload, Responses, Endpoints };
    const prevStart = previousMonthStartYmd();
    const prevPeriod = calendarMonthPeriod(-1);
    const currentPeriod = calendarMonthPeriod(0);

    await test.step('Precondition: supply chain activated on first day of previous month', async () => {
      await createSupplyChainWithSettlementPriceComponent(fx, { activationYmd: prevStart });
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    await test.step('Precondition: second POD on same BG, activated previous month start', async () => {
      await addAndActivateExtraPodsOnSameBillingGroupAt(fx, 1, prevStart);
      expect(Responses.pod.length, 'TC-BE-11 needs two PODs on one billing group').toBeGreaterThanOrEqual(2);
    });

    const pair = await test.step('Identify podLow = min(podId), podHighId = max(podId)', async () => {
      return resolveLowHighPods(fx);
    });

    const profile1 = await test.step(
      'POST billing-by-profile previous month Low=10 High=100',
      async () =>
        postBillingByProfileForPods(
          fx,
          pair.low,
          pair.high,
          { lowValue: 10, highValue: 100 },
          prevPeriod,
        ),
    );

    const invoice1 = await test.step(
      'Realize FOR_VOLUMES invoice 1 (previous month, invoiceDate=last day previous month)',
      async () =>
        realizeForVolumesInvoiceWithSettlementFallback(fx, pair.low, pair.high, profile1.periodType, {
          lowValue: 10,
          highValue: 100,
          invoiceDateYmd: previousMonthEndYmd(),
          period: prevPeriod,
        }),
    );

    const sums1 = await test.step('GET detailed-data invoice 1 — sumHigh1 > sumLow1', async () => {
      const sumLow1 = await sumInvoiceTotalVolumesByPod(fx, invoice1.invoiceId, pair.low.podIdentifier);
      const sumHigh1 = await sumInvoiceTotalVolumesByPod(fx, invoice1.invoiceId, pair.high.podIdentifier);
      TestRunSummary.registerPayload('invoice1Volumes', { sumLow1, sumHigh1, invoiceId: invoice1.invoiceId });
      expect(sumHigh1, 'invoice 1 HighId volume class').toBeGreaterThan(sumLow1);
      return { sumLow1, sumHigh1 };
    });

    const profile2 = await test.step(
      'POST billing-by-profile current month Low=200 High=10',
      async () =>
        postBillingByProfileForPods(
          fx,
          pair.low,
          pair.high,
          { lowValue: 200, highValue: 10 },
          currentPeriod,
        ),
    );

    const invoice2 = await test.step(
      'Realize FOR_VOLUMES invoice 2 (current month, invoiceDate=today)',
      async () =>
        realizeForVolumesInvoiceWithSettlementFallback(fx, pair.low, pair.high, profile2.periodType, {
          lowValue: 200,
          highValue: 10,
          invoiceDateYmd: todayYmd(),
          period: currentPeriod,
        }),
    );

    await test.step('GET detailed-data both invoices — combined Low > High; invoice 1 alone High > Low', async () => {
      const sumLow2 = await sumInvoiceTotalVolumesByPod(fx, invoice2.invoiceId, pair.low.podIdentifier);
      const sumHigh2 = await sumInvoiceTotalVolumesByPod(fx, invoice2.invoiceId, pair.high.podIdentifier);
      const sumLow = sums1.sumLow1 + sumLow2;
      const sumHigh = sums1.sumHigh1 + sumHigh2;
      const passed = sumLow > sumHigh && sums1.sumHigh1 > sums1.sumLow1;
      TestRunSummary.recordCheck({
        check: 'Two invoices — combined volumes Low > High; invoice 1 alone would pick HighId',
        expectedResult: 'sumLow1+sumLow2 > sumHigh1+sumHigh2 (example 210 > 110) and sumHigh1 > sumLow1',
        actualResult: passed
          ? `As expected — combined Low=${sumLow} High=${sumHigh}; inv1 Low=${sums1.sumLow1} High=${sums1.sumHigh1}`
          : `Not as expected — combined Low=${sumLow} High=${sumHigh}; inv1 Low=${sums1.sumLow1} High=${sums1.sumHigh1}`,
        passed,
      });
      expect(sums1.sumHigh1).toBeGreaterThan(sums1.sumLow1);
      expect(sumLow).toBeGreaterThan(sumHigh);
    });

    await test.step('Overdue BOTH invoice liabilities', async () => {
      await makeInvoiceLiabilityOverdue(fx, invoice1.invoiceId);
      await makeInvoiceLiabilityOverdue(fx, invoice2.invoiceId);
    });

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => ensureExecutedReminder(fx),
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply after both invoices',
      async () => loadCustomersForDps(fx, reminderId, 'PRODUCT_INVOICE_DEBIT_NOTE'),
    );

    await test.step('Assert: Low YES, HighId NO (sum of both invoices, not invoice 1 alone)', async () => {
      const listed = rowsForPodIds(load.rows, [pair.low.podId, pair.high.podId]);
      const lowRow = listed.find((row) => rowPodId(row) === pair.low.podId);
      const highRow = listed.find((row) => rowPodId(row) === pair.high.podId);
      const passed =
        Boolean(lowRow) &&
        Boolean(highRow) &&
        isHighestConsumptionFlag(lowRow) &&
        highRow?.isHighestConsumption === false;
      TestRunSummary.recordCheck({
        check: 'Two invoices — YES follows summed totalVolumes',
        expectedResult:
          'HTTP 206. POD Low YES. POD HighId NO even though invoice 1 alone had higher volume on HighId.',
        actualResult: passed
          ? `As expected — Low ${pair.low.podId}=true HighId ${pair.high.podId}=false`
          : `Not as expected — Low=${String(lowRow?.isHighestConsumption)} HighId=${String(highRow?.isHighestConsumption)} (fail if HighId YES — SQL used a single invoice)`,
        passed,
      });
      expect(lowRow, `Load PODs must include POD Low ${pair.low.podId}`).toBeTruthy();
      expect(highRow, `Load PODs must include POD HighId ${pair.high.podId}`).toBeTruthy();
      expect(lowRow?.isHighestConsumption, 'POD Low isHighestConsumption after both invoices').toBe(true);
      expect(highRow?.isHighestConsumption, 'POD HighId must stay false after volume sum').toBe(false);
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3090_KEY,
        relevantEntityKeys: pdt3090RelevantKeys({ includeInvoice: true }),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          invoiceId1: invoice1.invoiceId,
          invoiceId2: invoice2.invoiceId,
          activationYmd: prevStart,
          lowPodId: pair.low.podId,
          highPodId: pair.high.podId,
          loadStatus: load.status,
        },
      });
    });
  });
});
