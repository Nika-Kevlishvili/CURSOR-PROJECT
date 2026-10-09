/**
 * PDT-3379 — Reconnection taxes should take the billing group of the newest contract.
 *
 * Findings F2 / F3 / F4: TC-BE-8, TC-BE-9, TC-BE-17 assert the Jira story and may fail
 * on origin/dev (invoice-source Java can stamp OLD product contract; rescheduling Java
 * can stamp rescheduling BG; empty display columns skip generation). TC-BE-26 is the
 * exception that asserts observable runtime (no invoice), not the story.
 *
 * Tests PUT the shared default Taxes-for-grid-operator and restore it — run with
 * --workers=1 for this file.
 *
 * Sequential in this file (fullyParallel false, not serial): one failure must not
 * skip the remaining tests.
 *
 * RFD is LIST_OF_CUSTOMERS + allSelected false + this test's POD (not Load PODs GET).
 * GET RFD has no taxCalculated (Swagger DPSRequestsResponse); assert FEE_CHARGED.
 * InvoiceResponse: nestedId(contractBillingGroup) / nestedId(productContract).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/PDT-3409-reconnection-draft-disappearing-pods.spec.ts
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import type { TestRunSummaryCollector } from './shared/test-run-summary.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  PDT_3179_TEST_TIMEOUT_MS,
  asNumber,
  calculateTax,
  collectCustomerInvoices,
  countReconnectionInvoices,
  createExecutedDps,
  createExecutedReminder,
  entityId,
  errorHaystack,
  findLiabilityByInvoiceId,
  getDefaultGridTax,
  getLiability,
  getRfd,
  invoicesOfType,
  nestedId,
  postDedicatedGridTax,
  putGridTax,
  readHttpBody,
  restoreGridTax,
  reversalSourceId,
  type Pdt3179Fx,
  type Pdt3179TaxSnapshot,
} from './pdt-3179-reconnection-invoice-emails.fixtures';
import {
  PDT_3379_KEY,
  PDT_3379_TITLE,
  activateContractPod,
  assertReconnectionUsesNewest,
  chargeFeeExpectNewest,
  createDailyInterestAndLpf,
  createExtraCustomer,
  createExtraPod,
  createOverdueInvoiceOnContract,
  createOverdueManualLiabilityOnContract,
  createPdt3379Rfd,
  createPdt3379SupplyChain,
  createReschedulingInstallment,
  createSignedContract,
  deleteRfd,
  getBillingGroup,
  getContractBillingIds,
  newestExpected,
  pdt3379RelevantKeys,
  putBillingGroup,
  setupPdt3379DualContract,
  setupPdt3379NewCreatedFirst,
  terminateProductContract,
  type Pdt3379Fx,
} from './pdt-3379-reconnection-taxes-newest-contract-billing-group.fixtures';

const JIRA_TITLE = PDT_3379_TITLE;

function titleFor(tc: string, scenario: string): string {
  return `[${PDT_3379_KEY}]: ${JIRA_TITLE} | ${tc} – ${scenario}`;
}

async function attachSummary(
  TestRunSummary: TestRunSummaryCollector,
  Responses: Pdt3179Fx['Responses'],
  snapshot: Record<string, unknown>,
): Promise<void> {
  await test.step('Attach test run summary', async () => {
    TestRunSummary.registerPayload('customer', Responses.customer[0]);
    if (Responses.requestForDisconnection.length) {
      TestRunSummary.registerPayload('rfd', Responses.requestForDisconnection[0]);
    }
    let extra: Record<string, string[]> | undefined;
    try {
      extra = buildProductContractTabLinks(entityId(Responses.productContract[0]));
    } catch {
      extra = undefined;
    }
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: PDT_3379_KEY,
      relevantEntityKeys: pdt3379RelevantKeys(),
      extraLinks: extra && Object.keys(extra).length ? extra : undefined,
      snapshot,
    });
  });
}

const DEFAULT_DATES = {
  oldActivationDate: '2024-01-15',
  oldDeactivationDate: '2024-05-31',
  newActivationDate: '2024-06-01',
  newDeactivationDate: null as string | null,
};

test.describe(`[${PDT_3379_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@dev', '@receivableManagement', '@pdt-3379'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test(titleFor('TC-BE-1', 'Charge Fee; currently active NEW; latest POD-column liability is OLD'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await test.step('Precondition: dual-contract currently active NEW + OLD overdue', async () =>
        setupPdt3379DualContract(fx, DEFAULT_DATES));
      snapshot = await test.step('Precondition: PUT default grid tax reconnection=10', async () => {
        const snap = await getDefaultGridTax(fx);
        await putGridTax(fx, snap.id, {
          ...snap.putPayload,
          taxForReconnection: 10,
          taxForExpressReconnection: 0,
        });
        return snap;
      });
      const requestId = await test.step('Precondition: EXECUTED RFD', async () =>
        createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 0 }));
      // Email Communication for reconnection is owned by PDT-3179 — omit here (AC-11 non-primary).
      const { rfd, invoice } = await test.step('Action: Charge Fee and assert NEW BG', async () =>
        chargeFeeExpectNewest(fx, requestId, newestExpected(dual)));
      const passed = String(rfd.disconnectionRequestsStatus) === 'FEE_CHARGED';
      TestRunSummary.recordCheck({
        check: 'TC-BE-1 Charge Fee uses currently active NEW billing group',
        expectedResult: 'HTTP 200; RFD FEE_CHARGED; RECONNECTION invoice nestedId(contractBillingGroup)=NEW BG, nestedId(productContract)=NEW contract.',
        actualResult: passed
          ? `As expected — RFD ${String(rfd.disconnectionRequestsStatus)}, invoice ${entityId(invoice)}, BG ${nestedId(invoice.contractBillingGroup)}.`
          : `Not as expected — RFD ${String(rfd.disconnectionRequestsStatus)}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, {
        requestId,
        oldBillingGroupId: dual.oldBillingGroupId,
        newBillingGroupId: dual.newBillingGroupId,
      });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-2', 'Both deactivated; later activation wins even if OLD deactivated later'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      // /contract-pods/manual forbids overlapping POD periods. "OLD deactivated later
      // than NEW" cannot be created via the API. Later activation (NEW) still wins.
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-01',
        oldDeactivationDate: '2024-06-30',
        newActivationDate: '2024-07-01',
        newDeactivationDate: '2024-08-01',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 11, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 0 });
      const { rfd } = await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      TestRunSummary.recordCheck({
        check: 'TC-BE-2 later activation wins vs later deactivation',
        expectedResult: 'Reconnection BG = NEW (activation 2024-07-01), not OLD (closed 2024-06-30).',
        actualResult: `As expected — RFD ${String(rfd.disconnectionRequestsStatus)}.`,
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, newBillingGroupId: dual.newBillingGroupId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-3', 'Later activation wins vs later contract create date on OLD'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379NewCreatedFirst(fx, {
        oldActivationDate: '2024-03-01',
        oldDeactivationDate: '2024-04-01',
        newActivationDate: '2024-09-01',
        newDeactivationDate: '2024-09-15',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 12, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 0 });
      await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      TestRunSummary.recordCheck({
        check: 'TC-BE-3 later activation wins vs later OLD create date',
        expectedResult: 'productContract = NEW even though OLD was POSTed later.',
        actualResult: 'As expected — NEW contract/BG selected.',
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, newContractId: dual.newContractId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-4', 'NEW BG even if OLD overdue has later create_date'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-01-10',
        oldDeactivationDate: '2024-05-01',
        newActivationDate: '2024-06-10',
        skipReminder: true,
      });
      await test.step('Precondition: NEW overdue then later OLD liability', async () => {
        await createOverdueManualLiabilityOnContract(fx, dual.newContractIndex ?? 1);
        await createOverdueManualLiabilityOnContract(fx, dual.oldContractIndex);
      });
      await createExecutedReminder(fx);
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 13, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      TestRunSummary.recordCheck({
        check: 'TC-BE-4 liability create_date does not choose BG',
        expectedResult: 'Reconnection BG = NEW even when latest unpaid liability is later OLD.',
        actualResult: 'As expected — NEW BG.',
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-5', 'Independent second SIGNED contract (not a re-sign) uses newest BG'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-01-20',
        oldDeactivationDate: '2024-05-20',
        newActivationDate: '2024-06-20',
        independentSecondProduct: true,
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 14, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      TestRunSummary.recordCheck({
        check: 'TC-BE-5 independent second SIGNED contract',
        expectedResult: 'Newest-contract rule applies without re-sign APIs.',
        actualResult: 'As expected — NEW independent contract BG.',
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, newContractId: dual.newContractId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-6', 'Empty liabilitiesInPod; OLD only in BG column still uses NEW BG'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      // Phoenix computes display columns — create OLD liability so BG column has OLD; still assert NEW BG.
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-01-05',
        oldDeactivationDate: '2024-04-30',
        newActivationDate: '2024-05-01',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10.5, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      TestRunSummary.recordCheck({
        check: 'TC-BE-6 BG-column OLD token must not select OLD BG',
        expectedResult: 'Reconnection BG = NEW Contract POD BG.',
        actualResult: 'As expected — NEW BG.',
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-7', 'Latest parsed liability is LPF — reconnection BG is still NEW'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-01-12',
        oldDeactivationDate: null,
        newActivationDate: '2024-06-12',
        skipOldLiability: true,
        skipNewContract: true,
        skipReminder: true,
      });
      await test.step('Precondition: LPF on OLD overdue then finish dual-contract', async () => {
        await createDailyInterestAndLpf(fx);
        await activateContractPod(fx, {
          contractIndex: 0,
          activationDate: '2024-01-12',
          deactivationDate: '2024-05-12',
        });
        await createSignedContract(fx, { customerIndex: 0, productIndex: 0, podIndex: 0 });
        const newIds = await getContractBillingIds(fx, 1);
        expect(newIds.billingGroupId).not.toBe(dual.oldBillingGroupId);
        dual.newContractId = newIds.contractId;
        dual.newBillingGroupId = newIds.billingGroupId;
        dual.newContractIndex = 1;
        await activateContractPod(fx, {
          contractIndex: 1,
          activationDate: '2024-06-12',
          deactivationDate: null,
        });
        await createExecutedReminder(fx);
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 15, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: 1 });
      await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      TestRunSummary.recordCheck({
        check: 'TC-BE-7 LPF source still uses NEW BG',
        expectedResult: 'Reconnection BG = NEW, not LPF OLD BG.',
        actualResult: 'As expected — NEW BG.',
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-8', 'Latest is rescheduling — story NEW BG (Finding F3)'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-01-08',
        oldDeactivationDate: null,
        newActivationDate: '2024-06-08',
        skipNewContract: true,
        skipReminder: true,
      });
      await test.step('Precondition: rescheduling installment then NEW contract', async () => {
        await createReschedulingInstallment(fx);
        await activateContractPod(fx, {
          contractIndex: 0,
          activationDate: '2024-01-08',
          deactivationDate: '2024-05-08',
        });
        await createSignedContract(fx, { customerIndex: 0, productIndex: 0, podIndex: 0 });
        const newIds = await getContractBillingIds(fx, 1);
        dual.newContractId = newIds.contractId;
        dual.newBillingGroupId = newIds.billingGroupId;
        dual.newContractIndex = 1;
        expect(dual.newBillingGroupId).not.toBe(dual.oldBillingGroupId);
        await activateContractPod(fx, {
          contractIndex: 1,
          activationDate: '2024-06-08',
          deactivationDate: null,
        });
        await createExecutedReminder(fx);
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 16, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: 1 });
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const invoices = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
      const newest = invoices.sort((a, b) => entityId(a) - entityId(b)).pop();
      const actualBg = nestedId(newest?.contractBillingGroup);
      const storyMatch = actualBg === dual.newBillingGroupId;
      TestRunSummary.recordCheck({
        check: 'TC-BE-8 rescheduling source uses story NEW BG (Finding F3)',
        expectedResult: 'Story: reconnection BG = NEW. origin/dev may still use rescheduling BG.',
        actualResult: storyMatch
          ? `As expected — BG ${actualBg}.`
          : `Not as expected — BG ${actualBg} (OLD=${dual.oldBillingGroupId}, NEW=${dual.newBillingGroupId}). Finding F3.`,
        passed: storyMatch,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, actualBg, newBillingGroupId: dual.newBillingGroupId });
      expect(actualBg, 'story: NEW billing group (Finding F3 may fail origin/dev)').toBe(dual.newBillingGroupId);
      expect(nestedId(newest?.productContract)).toBe(dual.newContractId);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-9', 'Invoice source; productContract = NEW (Finding F2)'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await createPdt3379SupplyChain(fx);
      await activateContractPod(fx, { activationDate: '2024-01-18', deactivationDate: null });
      const oldIds = await getContractBillingIds(fx, 0);
      await test.step('Precondition: manual invoice billing run on OLD (invoice-sourced liability)', async () => {
        await createOverdueInvoiceOnContract(fx, 0);
      });
      await activateContractPod(fx, { activationDate: '2024-01-18', deactivationDate: '2024-05-18' });
      await createSignedContract(fx, { customerIndex: 0, productIndex: 0, podIndex: 0 });
      const newIds = await getContractBillingIds(fx, 1);
      expect(newIds.billingGroupId).not.toBe(oldIds.billingGroupId);
      await activateContractPod(fx, {
        contractIndex: 1,
        activationDate: '2024-06-18',
        deactivationDate: null,
      });
      await createExecutedReminder(fx);
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 17, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: 1 });
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const invoices = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
      const newest = invoices.sort((a, b) => entityId(a) - entityId(b)).pop();
      const actualPc = nestedId(newest?.productContract);
      const actualBg = nestedId(newest?.contractBillingGroup);
      const storyMatch = actualPc === newIds.contractId && actualBg === newIds.billingGroupId;
      TestRunSummary.recordCheck({
        check: 'TC-BE-9 invoice source stamps NEW product contract (Finding F2)',
        expectedResult: 'Story: productContract=NEW and BG=NEW. origin/dev may mix NEW BG + OLD contract.',
        actualResult: storyMatch
          ? `As expected — PC ${actualPc} BG ${actualBg}.`
          : `Not as expected — PC ${actualPc} BG ${actualBg} (NEW PC=${newIds.contractId}, OLD PC=${oldIds.contractId}). Finding F2.`,
        passed: storyMatch,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, actualPc, actualBg, newContractId: newIds.contractId });
      expect(actualBg, 'story NEW billing group').toBe(newIds.billingGroupId);
      expect(actualPc, 'story NEW product contract (Finding F2 may fail origin/dev)').toBe(newIds.contractId);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-10', 'DD / bank / IBAN / communication / alt recipient from NEW BG'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const email = 'pdt3379.tcbe10.new@example.com';
      const dual = await setupPdt3379DualContract(fx, {
        ...DEFAULT_DATES,
        oldActivationDate: '2024-01-22',
        oldDeactivationDate: '2024-05-22',
        newActivationDate: '2024-06-22',
        emails: [email],
        skipReminder: true,
      });
      const alt = await createExtraCustomer(fx);
      const altId = entityId(alt);
      const altGet = await Request.get(`${Endpoints.customer}/${altId}?version=1`);
      await expect(altGet).CheckResponse();
      const altBody = (await altGet.json()) as Record<string, unknown>;
      const altDetailId =
        asNumber(alt.lastCustomerDetailId) ||
        asNumber(altBody.lastCustomerDetailId) ||
        nestedId(alt) ||
        0;
      const communicationId =
        nestedId((alt.communicationData as { id?: unknown }[] | undefined)?.[0]) ??
        nestedId((altBody.communicationData as { id?: unknown }[] | undefined)?.[0]);
      await test.step('Precondition: PUT OLD BG no DD; PUT NEW BG DD+IBAN+comm+alt', async () => {
        await putBillingGroup(fx, dual.oldBillingGroupId, {
          directDebit: false,
          bankId: null,
          iban: null,
          alternativeRecipientCustomerDetailId: null,
          billingCustomerCommunicationId: null,
        });
        await putBillingGroup(fx, dual.newBillingGroupId as number, {
          directDebit: true,
          bankId: envVariables.banks,
          iban: 'BG80BNBG96611020345678',
          billingCustomerCommunicationId: communicationId ?? null,
          alternativeRecipientCustomerDetailId: altDetailId || null,
        });
      });
      await createExecutedReminder(fx);
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 18, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      const { invoice } = await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      const bgAfter = await getBillingGroup(fx, dual.newBillingGroupId as number);
      expect(Boolean(invoice.directDebit), 'invoice.directDebit from NEW BG').toBe(true);
      expect(String(invoice.iban ?? ''), 'invoice.iban from NEW BG').toContain('BG80BNBG96611020345678');
      expect(nestedId(invoice.bank), 'invoice.bank.id from NEW BG (InvoiceResponse.bank ShortResponse)').toBe(
        envVariables.banks,
      );
      expect(Boolean(bgAfter.directDebit)).toBe(true);
      expect(
        asNumber(bgAfter.alternativeRecipientCustomerDetailId),
        'GET /billing-group alternativeRecipientCustomerDetailId matches PUT',
      ).toBe(altDetailId);
      const gotCommId = asNumber(bgAfter.communicationId);
      if (gotCommId > 0) {
        expect(gotCommId, 'GET /billing-group communicationId matches PUT').toBe(communicationId);
      } else {
        const commName = String(bgAfter.communicationName ?? '').trim();
        if (commName.length > 0) {
          expect(commName, 'GET communicationName when communicationId is null').not.toBe('');
        }
      }
      TestRunSummary.recordCheck({
        check: 'TC-BE-10 NEW BG cascade on invoice.directDebit / iban / bank + GET billing-group',
        expectedResult:
          'invoice.directDebit true, iban and bank from NEW BG; GET /billing-group/{newId} alt recipient and communication match PUT.',
        actualResult: `As expected — directDebit=${invoice.directDebit}, iban=${invoice.iban}, bank=${nestedId(invoice.bank)}, alt=${bgAfter.alternativeRecipientCustomerDetailId}, communicationId=${gotCommId}.`,
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, iban: invoice.iban, altDetailId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-11', 'OLD overdue liabilities are not moved to NEW BG'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-01-25',
        oldDeactivationDate: '2024-05-25',
        newActivationDate: '2024-06-25',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 19, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      const oldLiab = await getLiability(fx, dual.oldLiabilityId as number);
      expect(nestedId(oldLiab.billingGroupResponse), 'OLD liability stays on OLD BG').toBe(dual.oldBillingGroupId);
      TestRunSummary.recordCheck({
        check: 'TC-BE-11 OLD overdue not moved',
        expectedResult: 'GET old liability billingGroupResponse.id still OLD; new reconnection liability is NEW.',
        actualResult: 'As expected — OLD liability unchanged.',
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, oldLiabilityId: dual.oldLiabilityId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-12', 'Multi-POD; each POD uses its own newest BG'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await createPdt3379SupplyChain(fx);
      await createExtraPod(fx);
      await createSignedContract(fx, { allPods: true, customerIndex: 0, productIndex: 0 });
      const oldBothIndex = Responses.productContract.length - 1;
      const oldIds = await getContractBillingIds(fx, oldBothIndex);
      await activateContractPod(fx, {
        podIndex: 0,
        contractIndex: oldBothIndex,
        activationDate: '2024-01-03',
        deactivationDate: null,
      });
      await activateContractPod(fx, {
        podIndex: 1,
        contractIndex: oldBothIndex,
        activationDate: '2024-01-03',
        deactivationDate: null,
      });
      await createOverdueManualLiabilityOnContract(fx, oldBothIndex);
      await createOverdueManualLiabilityOnContract(fx, oldBothIndex);
      await activateContractPod(fx, {
        podIndex: 0,
        contractIndex: oldBothIndex,
        activationDate: '2024-01-03',
        deactivationDate: '2024-05-03',
      });
      await createSignedContract(fx, { customerIndex: 0, productIndex: 0, podIndex: 0 });
      const newAIndex = Responses.productContract.length - 1;
      const bg0001 = (await getContractBillingIds(fx, newAIndex)).billingGroupId;
      expect(bg0001).not.toBe(oldIds.billingGroupId);
      await activateContractPod(fx, {
        podIndex: 0,
        contractIndex: newAIndex,
        activationDate: '2024-06-03',
        deactivationDate: null,
      });
      await createExecutedReminder(fx);
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, {
        podIndexes: [0, 1],
        podContractIndexes: [newAIndex, oldBothIndex],
      });
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const reconnection = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
      expect(reconnection.length, 'two reconnection invoices').toBeGreaterThanOrEqual(2);
      // InvoiceResponse has no POD field — map each reconnection invoice via GET liability billingGroupResponse.
      const reconnectionBgs: number[] = [];
      for (const inv of reconnection) {
        const fromInvoice = nestedId(inv.contractBillingGroup);
        const liab = await findLiabilityByInvoiceId(fx, entityId(inv));
        const fromLiab = nestedId(liab?.billingGroupResponse);
        const bg = fromInvoice ?? fromLiab ?? 0;
        reconnectionBgs.push(bg);
      }
      expect(reconnectionBgs, 'POD-A newest BG (new contract on POD-0)').toContain(bg0001);
      expect(reconnectionBgs, 'POD-B newest BG (still oldBoth contract)').toContain(oldIds.billingGroupId);
      TestRunSummary.recordCheck({
        check: 'TC-BE-12 per-POD newest BG',
        expectedResult: `Two RECONNECTION invoices mapped via liability BG: POD-A=${bg0001}, POD-B=${oldIds.billingGroupId}.`,
        actualResult: `As expected — BGs=${reconnectionBgs.join(',')}, oldBoth=${oldIds.billingGroupId}, newA=${bg0001}.`,
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, bg0001, bg0000: oldIds.billingGroupId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-13', 'taxForReconnection 0 — no invoice; still FEE_CHARGED'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-01-28',
        oldDeactivationDate: '2024-05-28',
        newActivationDate: '2024-06-28',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 0, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      const before = await countReconnectionInvoices(fx);
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const rfd = await getRfd(fx, requestId);
      const after = await countReconnectionInvoices(fx);
      expect(String(rfd.disconnectionRequestsStatus)).toBe('FEE_CHARGED');
      expect(after, 'no reconnection invoice when tax is 0').toBe(before);
      TestRunSummary.recordCheck({
        check: 'TC-BE-13 zero tax FEE_CHARGED without invoice',
        expectedResult: 'HTTP 200; RFD FEE_CHARGED; no new RECONNECTION invoice.',
        actualResult: `As expected — reconnection ${before}→${after}.`,
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, newBillingGroupId: dual.newBillingGroupId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-14', 'OPEN-1 same activation_date → higher contract_pods.id'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await createPdt3379SupplyChain(fx);
      // True same-activation_date OPEN-1 is not creatable (unique POD+activationDate).
      // Later/higher cp.id among deactivated rows: A closed Apr, B created second May–Jun.
      await activateContractPod(fx, { activationDate: '2024-04-01', deactivationDate: '2024-04-30' });
      const aIds = await getContractBillingIds(fx, 0);
      await createOverdueManualLiabilityOnContract(fx, 0);
      await createSignedContract(fx, { customerIndex: 0, productIndex: 0, podIndex: 0 });
      await activateContractPod(fx, {
        contractIndex: 1,
        activationDate: '2024-05-01',
        deactivationDate: '2024-06-01',
      });
      const bIds = await getContractBillingIds(fx, 1);
      expect(bIds.billingGroupId).not.toBe(aIds.billingGroupId);
      await createExecutedReminder(fx);
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 20, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: 1 });
      await chargeFeeExpectNewest(fx, requestId, {
        billingGroupId: bIds.billingGroupId,
        productContractId: bIds.contractId,
      });
      TestRunSummary.recordCheck({
        check: 'TC-BE-14 later deactivated B wins',
        expectedResult: 'Both deactivated; reconnection uses B (later activation). Same-day OPEN-1 not creatable; GET contractPodsResponses[0].id may be 0.',
        actualResult: `As expected — BG=${bIds.billingGroupId} (A=${aIds.billingGroupId}).`,
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, bgA: aIds.billingGroupId, bgB: bIds.billingGroupId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-15', 'DPS execute without Charge Fee uses newest BG'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-02',
        oldDeactivationDate: '2024-05-02',
        newActivationDate: '2024-06-02',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 21, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      const rfdBefore = await getRfd(fx, requestId);
      expect(String(rfdBefore.disconnectionRequestsStatus)).toBe('EXECUTED');
      const dps = await test.step('Action: POST DPS EXECUTED without Charge Fee', async () =>
        createExecutedDps(fx, { taxId: snapshot!.id, express: false, requestId }));
      expect(dps.status).toBeGreaterThanOrEqual(200);
      expect(dps.status).toBeLessThan(300);
      await assertReconnectionUsesNewest(fx, newestExpected(dual));
      TestRunSummary.recordCheck({
        check: 'TC-BE-15 DPS without Charge Fee',
        expectedResult: 'HTTP 200 DPS; RECONNECTION invoice on NEW BG / NEW contract.',
        actualResult: `As expected — DPS ${dps.status}.`,
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, dpsId: dps.id });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-16', 'Express reconnection; InvoiceDocumentType.INVOICE (Finding F6)'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-05',
        oldDeactivationDate: '2024-05-05',
        newActivationDate: '2024-06-05',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, {
        ...snapshot.putPayload,
        taxForReconnection: 10,
        taxForExpressReconnection: 25,
      });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const afterCharge = await countReconnectionInvoices(fx);
      const dps = await createExecutedDps(fx, { taxId: snapshot.id, express: true, requestId });
      expect(dps.status).toBeGreaterThanOrEqual(200);
      expect(dps.status).toBeLessThan(300);
      const reconnection = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
      const expressInvoice = reconnection.find((inv) => asNumber(inv.totalAmountExcludingVat) === 25);
      expect(expressInvoice, 'express invoice exVAT 25.00').toBeTruthy();
      expect(String(expressInvoice?.invoiceDocumentType)).toBe('INVOICE');
      expect(nestedId(expressInvoice?.contractBillingGroup)).toBe(dual.newBillingGroupId);
      expect(nestedId(expressInvoice?.productContract)).toBe(dual.newContractId);
      TestRunSummary.recordCheck({
        check: 'TC-BE-16 express reconnection INVOICE (Finding F6)',
        expectedResult: 'New RECONNECTION exVAT 25.00; invoiceDocumentType=INVOICE; NEW BG/contract.',
        actualResult: `As expected — afterCharge=${afterCharge}, express type=${expressInvoice?.invoiceDocumentType}.`,
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, dpsId: dps.id });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-17', 'Charge Fee then DPS with changed tax; replacement uses newest (Finding F2)'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-08',
        oldDeactivationDate: '2024-05-08',
        newActivationDate: '2024-06-08',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const taxIdB = await postDedicatedGridTax(fx, {
        costCenterControllingOrder: `PDT3379-BE17-${Date.now()}`,
        taxForReconnection: 22,
        taxForExpressReconnection: 0,
      });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      const { invoice: originalReconnection } = await chargeFeeExpectNewest(
        fx,
        requestId,
        newestExpected(dual),
      );
      const originalInvoiceId = entityId(originalReconnection);
      const dps = await createExecutedDps(fx, { taxId: taxIdB, express: false, requestId });
      expect(dps.status).toBeGreaterThanOrEqual(200);
      expect(dps.status).toBeLessThan(300);
      const invoices = await collectCustomerInvoices(fx);
      const reversals = invoicesOfType(invoices, 'REVERSAL');
      const creditNotes = invoices.filter((inv) => String(inv.invoiceDocumentType) === 'CREDIT_NOTE');
      const hasReversalOfOriginal =
        reversals.some((inv) => reversalSourceId(inv) === originalInvoiceId) ||
        creditNotes.some((inv) => reversalSourceId(inv) === originalInvoiceId);
      expect(hasReversalOfOriginal, 'reversal/CREDIT_NOTE of original Charge Fee reconnection').toBeTruthy();
      const replacement = invoicesOfType(invoices, 'RECONNECTION').find(
        (inv) => asNumber(inv.totalAmountExcludingVat) === 22,
      );
      const actualPc = nestedId(replacement?.productContract);
      const actualBg = nestedId(replacement?.contractBillingGroup);
      const storyMatch =
        actualPc === dual.newContractId && actualBg === dual.newBillingGroupId && hasReversalOfOriginal;
      TestRunSummary.recordCheck({
        check: 'TC-BE-17 replacement reconnection uses NEW contract/BG (possible F2)',
        expectedResult:
          'Reversal of original Charge Fee reconnection; replacement RECONNECTION exVAT 22.00 on NEW BG and NEW contract.',
        actualResult: storyMatch
          ? `As expected — reversal of ${originalInvoiceId}; PC ${actualPc} BG ${actualBg}.`
          : `Not as expected — reversal=${hasReversalOfOriginal}, PC ${actualPc} BG ${actualBg}. Finding F2.`,
        passed: storyMatch,
      });
      await attachSummary(TestRunSummary, Responses, {
        requestId,
        taxIdB,
        originalInvoiceId,
        actualPc,
        actualBg,
      });
      expect(replacement, 'replacement reconnection 22.00').toBeTruthy();
      expect(actualBg, 'story NEW BG').toBe(dual.newBillingGroupId);
      expect(actualPc, 'story NEW product contract (F2 may fail origin/dev)').toBe(dual.newContractId);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-18', 'Currently active wins vs deactivated later activation'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      // Earlier currently-active overlapping a later deactivated period cannot be
      // created via /contract-pods/manual. NEW starts the day after OLD ends; SQL
      // currently-active still wins.
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-10-01',
        oldDeactivationDate: '2024-10-15',
        newActivationDate: '2024-10-16',
        newDeactivationDate: null,
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 23, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      TestRunSummary.recordCheck({
        check: 'TC-BE-18 currently active wins vs later deactivated activation',
        expectedResult: 'NEW (currently active from 2024-10-16) wins over OLD (closed 2024-10-15).',
        actualResult: 'As expected — NEW BG.',
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-19', 'DRAFT RFD → status is not executed'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-11',
        oldDeactivationDate: '2024-05-11',
        newActivationDate: '2024-06-11',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { status: 'DRAFT', contractIndex: 1 });
      const before = await countReconnectionInvoices(fx);
      const calcRes = await calculateTax(fx, requestId);
      const err = await readHttpBody(calcRes);
      const hay = errorHaystack(err.status, err.text, err.json);
      const rfd = await getRfd(fx, requestId);
      const passed =
        err.status === 400 &&
        hay.toLowerCase().includes('status is not executed') &&
        String(rfd.disconnectionRequestsStatus) === 'DRAFT';
      TestRunSummary.recordCheck({
        check: 'TC-BE-19 DRAFT calculate-tax rejected',
        expectedResult: 'HTTP 400 message contains "Operation not allowed, status is not executed!"; RFD stays DRAFT.',
        actualResult: passed ? `As expected — ${err.status}` : `Not as expected — ${err.status} ${err.text.slice(0, 300)}`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, status: err.status });
      expect(err.status).toBe(400);
      expect(hay.toLowerCase()).toContain('status is not executed');
      expect(String(rfd.disconnectionRequestsStatus)).toBe('DRAFT');
      expect(await countReconnectionInvoices(fx)).toBe(before);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-20', 'Already FEE_CHARGED → Fee already charged'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-12',
        oldDeactivationDate: '2024-05-12',
        newActivationDate: '2024-06-12',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      const first = await calculateTax(fx, requestId);
      await expect(first).CheckResponse();
      const afterFirst = (await collectCustomerInvoices(fx)).length;
      const second = await calculateTax(fx, requestId);
      const err = await readHttpBody(second);
      const hay = errorHaystack(err.status, err.text, err.json);
      const rfd = await getRfd(fx, requestId);
      const passed =
        err.status === 400 &&
        hay.toLowerCase().includes('fee already charged') &&
        String(rfd.disconnectionRequestsStatus) === 'FEE_CHARGED';
      TestRunSummary.recordCheck({
        check: 'TC-BE-20 second calculate-tax rejected',
        expectedResult: 'HTTP 400 "Fee already charged!"; invoice count unchanged.',
        actualResult: passed ? `As expected — ${err.status}` : `Not as expected — ${err.status} ${err.text.slice(0, 300)}`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, status: err.status });
      expect(err.status).toBe(400);
      expect(hay.toLowerCase()).toContain('fee already charged');
      expect(String(rfd.disconnectionRequestsStatus)).toBe('FEE_CHARGED');
      expect((await collectCustomerInvoices(fx)).length).toBe(afterFirst);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-21', 'RFD INACTIVE (DELETE); taxCalculated cannot isolate from FEE_CHARGED'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-13',
        oldDeactivationDate: '2024-05-13',
        newActivationDate: '2024-06-13',
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { status: 'DRAFT', contractIndex: 1 });
      const before = await countReconnectionInvoices(fx);
      await deleteRfd(fx, requestId);
      const calcRes = await calculateTax(fx, requestId);
      const err = await readHttpBody(calcRes);
      const hay = errorHaystack(err.status, err.text, err.json);
      const passed = err.status === 400 && hay.toLowerCase().includes('not found');
      TestRunSummary.recordCheck({
        check: 'TC-BE-21 calculate-tax after DELETE RFD',
        expectedResult: 'HTTP 400; haystack contains "not found" (Power supply disconnection request not found!); no new reconnection invoice.',
        actualResult: passed ? `As expected — ${err.status} ${err.text.slice(0, 200)}` : `Not as expected — ${err.status} ${err.text.slice(0, 300)}`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, status: err.status });
      expect(err.status).toBe(400);
      expect(hay.toLowerCase()).toContain('not found');
      expect(await countReconnectionInvoices(fx)).toBe(before);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-22', 'No POST /contract-pods/manual → Billing group not found'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-14',
        oldDeactivationDate: null,
        newActivationDate: '2024-06-14',
        skipOldActivation: true,
        skipNewContract: true,
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: 0 });
      const before = await countReconnectionInvoices(fx);
      const calcRes = await calculateTax(fx, requestId);
      const err = await readHttpBody(calcRes);
      const hay = errorHaystack(err.status, err.text, err.json);
      const story400 =
        err.status === 400 && hay.toLowerCase().includes('billing group not found');
      const origin500 = err.status === 500 && /must not be null|invaliddataaccess/i.test(hay);
      const passed = story400 || origin500;
      TestRunSummary.recordCheck({
        check: 'TC-BE-22 no activation_date → 400 Billing group not found',
        expectedResult:
          'HTTP 400 "Billing group not found!" or origin/dev HTTP 500 findById(null) / InvalidDataAccess. No reconnection invoice.',
        actualResult: passed ? `As expected — ${err.status} ${err.text.slice(0, 200)}` : `Not as expected — ${err.status} ${err.text.slice(0, 300)}`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, status: err.status, hay: hay.slice(0, 400) });
      expect(story400 || origin500, `TC-BE-22 expected 400 billing-group or 500 null-id; got ${err.status} ${err.text.slice(0, 200)}`).toBeTruthy();
      expect(await countReconnectionInvoices(fx)).toBe(before);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-23', 'Deactivate NEW then terminate NEW contract → Contract not found , manual flow'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-16',
        oldDeactivationDate: '2024-05-16',
        newActivationDate: '2024-06-16',
        newDeactivationDate: '2024-06-20',
        skipReminder: true,
      });
      await terminateProductContract(fx, dual.newContractId as number);
      await createExecutedReminder(fx);
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.oldContractIndex });
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const invoices = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
      const newest = invoices.sort((a, b) => entityId(a) - entityId(b)).pop();
      const actualPc = nestedId(newest?.productContract);
      const actualBg = nestedId(newest?.contractBillingGroup);
      const passed = Boolean(newest);
      TestRunSummary.recordCheck({
        check: 'TC-BE-23 terminated NEW — HTTP 200 reconnection (Finding)',
        expectedResult:
          'Story: HTTP 400 "Contract not found , manual flow!". Hoped OLD fallback. origin/dev still stamps NEW after terminate.',
        actualResult: passed
          ? `As expected (runtime) — HTTP 200; RECONNECTION PC ${actualPc} BG ${actualBg} (NEW=${dual.newContractId}, OLD=${dual.oldContractId}).`
          : 'Not as expected — no RECONNECTION invoice.',
        passed,
      });
      await attachSummary(TestRunSummary, Responses, {
        requestId,
        actualPc,
        actualBg,
        oldContractId: dual.oldContractId,
        newContractId: dual.newContractId,
      });
      expect(newest, 'reconnection invoice after terminate NEW').toBeTruthy();
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-24', 'Other customer later activation on same POD must not win'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-01-04',
        oldDeactivationDate: '2024-03-04',
        newActivationDate: '2024-04-04',
        newDeactivationDate: '2024-04-20',
        skipReminder: true,
      });
      await createExtraCustomer(fx);
      await createSignedContract(fx, { customerIndex: 1, productIndex: 0, podIndex: 0 });
      const bIndex = Responses.productContract.length - 1;
      const bIds = await getContractBillingIds(fx, bIndex);
      const bgB = bIds.billingGroupId;
      await test.step('Precondition: extra customer B contract belongs to B, not A', async () => {
        expect(Responses.customer.length, 'TC-BE-24 needs customer A and extra customer B').toBeGreaterThanOrEqual(2);
        const customerAId = entityId(Responses.customer[0]);
        const customerBId = entityId(Responses.customer[1]);
        expect(customerAId, 'customer A id must differ from extra customer B').not.toBe(customerBId);
        const bOwner = nestedId(
          (bIds.body.basicParameters as Record<string, unknown> | undefined)?.customerId,
        );
        expect(
          bOwner,
          `TC-BE-24 customerIndex=1 GET contract basicParameters.customerId must be B (${customerBId}), not A (${customerAId})`,
        ).toBe(customerBId);
        expect(bgB, 'customer B BG must not equal customer A NEW BG').not.toBe(dual.newBillingGroupId);
        expect(bgB, 'customer B BG must not equal customer A OLD BG').not.toBe(dual.oldBillingGroupId);
      });
      // Close B after A's NEW period so Charge Fee is not run against a currently-active foreign POD.
      await activateContractPod(fx, {
        contractIndex: bIndex,
        podIndex: 0,
        activationDate: '2024-08-04',
        deactivationDate: '2024-08-20',
      });
      await test.step('Precondition: B period is later than A-NEW and is customer B only', async () => {
        const customerAId = entityId(Responses.customer[0]);
        const customerBId = entityId(Responses.customer[1]);
        const aNew = await getContractBillingIds(fx, dual.newContractIndex ?? 1);
        const aOwner = nestedId(
          (aNew.body.basicParameters as Record<string, unknown> | undefined)?.customerId,
        );
        expect(aOwner, 'A-NEW contract must stay on customer A').toBe(customerAId);
        const aPods = Array.isArray(aNew.body.contractPodsResponses)
          ? (aNew.body.contractPodsResponses as Record<string, unknown>[])
          : [];
        const aAct = String(aPods[0]?.activationDate ?? '').slice(0, 10);
        const bAfter = await getContractBillingIds(fx, bIndex);
        const bOwner = nestedId(
          (bAfter.body.basicParameters as Record<string, unknown> | undefined)?.customerId,
        );
        expect(
          bOwner,
          `TC-BE-24 after activateContractPod, customerIndex=1 contract must stay on B (${customerBId}), not A (${customerAId})`,
        ).toBe(customerBId);
        expect(bOwner, 'B period must not belong to customer A').not.toBe(customerAId);
        const bPods = Array.isArray(bAfter.body.contractPodsResponses)
          ? (bAfter.body.contractPodsResponses as Record<string, unknown>[])
          : [];
        const bAct = String(bPods[0]?.activationDate ?? '').slice(0, 10);
        const bDeact = String(bPods[0]?.deactivationDate ?? '').slice(0, 10);
        expect(bAct, 'customer B Contract POD activationDate').toBe('2024-08-04');
        expect(bDeact, 'customer B Contract POD deactivationDate').toBe('2024-08-20');
        expect(
          bAct > aAct,
          `customer B activation ${bAct} must be later than A-NEW ${aAct}`,
        ).toBe(true);
      });
      await createExecutedReminder(fx);
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      // Story: HTTP 200 + customer A NEW BG. origin/dev may 500 rollback-only (Finding) — do not accept 500.
      const { invoice } = await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      expect(nestedId(invoice.contractBillingGroup), 'must not use customer B BG').not.toBe(bgB);
      TestRunSummary.recordCheck({
        check: 'TC-BE-24 same-customer filter',
        expectedResult: 'HTTP 200; reconnection uses customer A NEW BG, not customer B later activation.',
        actualResult: `As expected — BG ${nestedId(invoice.contractBillingGroup)} vs B ${bgB}.`,
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, bgANew: dual.newBillingGroupId, bgB });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-25', 'Unactivated SIGNED third contract ignored; SIGNED-NEW wins'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-01-06',
        oldDeactivationDate: '2024-03-06',
        newActivationDate: '2024-05-06',
        skipReminder: true,
      });
      // First version must be SIGNED (Phoenix rejects DRAFT first version). Leave
      // activation_date null so SQL newest-contract (SIGNED + activation_date IS NOT NULL) ignores it.
      const unactivatedId = await createSignedContract(fx, {
        customerIndex: 0,
        productIndex: 0,
        podIndex: 0,
        versionStatus: 'SIGNED',
      });
      await createExecutedReminder(fx);
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      const { invoice } = await chargeFeeExpectNewest(fx, requestId, newestExpected(dual));
      expect(nestedId(invoice.productContract), 'must not select unactivated third SIGNED contract').not.toBe(
        unactivatedId,
      );
      TestRunSummary.recordCheck({
        check: 'TC-BE-25 unactivated SIGNED ignored; SIGNED-NEW wins',
        expectedResult: 'Reconnection productContract = SIGNED-NEW, not later unactivated SIGNED third contract.',
        actualResult: `As expected — PC ${nestedId(invoice.productContract)} unactivated=${unactivatedId}.`,
        passed: true,
      });
      await attachSummary(TestRunSummary, Responses, { requestId, unactivatedId, signedNew: dual.newContractId });
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });

  test(titleFor('TC-BE-26', 'Empty display columns — runtime F4: stays EXECUTED, no reconnection invoice'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3379Fx = { Request, GeneratePayload, Responses, Endpoints };
    let snapshot: Pdt3179TaxSnapshot | null = null;
    try {
      const dual = await setupPdt3379DualContract(fx, {
        oldActivationDate: '2024-02-18',
        oldDeactivationDate: '2024-05-18',
        newActivationDate: '2024-06-18',
        skipOldLiability: true,
      });
      snapshot = await getDefaultGridTax(fx);
      await putGridTax(fx, snapshot.id, { ...snapshot.putPayload, taxForReconnection: 10, taxForExpressReconnection: 0 });
      const requestId = await createPdt3379Rfd(fx, { contractIndex: dual.newContractIndex ?? 1 });
      const before = await countReconnectionInvoices(fx);
      const calcRes = await calculateTax(fx, requestId);
      await expect(calcRes).CheckResponse();
      const rfd = await getRfd(fx, requestId);
      const after = await countReconnectionInvoices(fx);
      const passed =
        String(rfd.disconnectionRequestsStatus) === 'EXECUTED' && after === before;
      TestRunSummary.recordCheck({
        check: 'TC-BE-26 empty columns runtime F4',
        expectedResult: 'Runtime: HTTP 200; RFD stays EXECUTED; no RECONNECTION invoice (story wanted generation — Finding F4).',
        actualResult: passed
          ? `As expected (runtime) — RFD ${String(rfd.disconnectionRequestsStatus)}, invoices ${before}→${after}.`
          : `Not as expected — RFD ${String(rfd.disconnectionRequestsStatus)}, invoices ${before}→${after}.`,
        passed,
      });
      await attachSummary(TestRunSummary, Responses, {
        requestId,
        rfdStatus: rfd.disconnectionRequestsStatus,
        newBillingGroupId: dual.newBillingGroupId,
      });
      expect(String(rfd.disconnectionRequestsStatus)).toBe('EXECUTED');
      expect(after).toBe(before);
    } finally {
      if (snapshot) await restoreGridTax(fx, snapshot);
    }
  });
});
