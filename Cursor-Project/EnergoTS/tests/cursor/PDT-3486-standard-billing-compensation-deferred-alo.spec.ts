/**
 * PDT-3486 — Running Automatic Offsetting in Standard Billing Run with Compensations
 * (delayed ALO after government compensation; PDT-3487 old-receivable order).
 *
 * Backend TC: Cursor-Project/test_cases/Backend/PDT-3486_standard_billing_compensation_deferred_ALO.md
 * One test() per TC-BE-1..8. Backend only. PDT-3483 Excel FLA identities are not cloned.
 *
 * One FOR_VOLUMES draft (PDT-3483 debit path). Leftover ALO items are POST
 * MANUAL receivable/liability on the same contract billing group — not IAP,
 * Credit Note, or invoice cancellation. Correction reuses that REAL invoice
 * (priceChange + listOfInvoices; no extra volume BBP).
 * Order: draft → seed MANUAL L/R → POST compensation → PATCH start-generating.
 * Then realize (`PATCH /billing-run/start-accounting` until invoice REAL).
 *
 * Swagger: Cursor-Project/config/swagger/dev/swagger-spec.json (refreshed this session).
 * - PATCH /billing-run/start-accounting (OpenAPI 200; runtime often 202)
 * - POST /government-compensations CompensationRequest
 * - PUT /customer-receivable/{id} blockedForOffsetting
 * - PUT /customer-liability/{id} blockedForLiabilitiesOffsetting
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3483-billing-run-compensation-credit-debit-note-final-liability.spec.ts
 * - tests/cursor/pdt-3483-billing-run-compensation-credit-debit-note-final-liability.fixtures.ts
 * - tests/cursor/PDT-3470-standard-credit-note-government-compensation-receivable.spec.ts
 * - tests/cursor/PDT-3127-government-compensation-defer-to-pdf-invoice-correction.spec.ts
 * - tests/cursor/pdt-3486-standard-billing-compensation-deferred-alo.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3486_KEY,
  PDT_3486_MANUAL_LIABILITY_AMOUNT,
  PDT_3486_MANUAL_RECEIVABLE_AMOUNT,
  PDT_3486_RELEVANT_ENTITY_KEYS,
  PDT_3486_TEST_TIMEOUT_MS,
  PDT_3486_TITLE,
  assertCompensationLinkedUninvoiced,
  assertLinkedInvoiceIsDebitNote,
  assertNegativeCompensationPolarity,
  assertPositiveCompensationPolarity,
  classifyCorrectionDrafts,
  findAutomaticLiabilityOnInvoice,
  getCompensation,
  getInvoice,
  getLiability,
  getReceivable,
  isNullishEntityRef,
  listCompensationsForCustomer,
  listReceivablesForCustomer,
  minMoney,
  nestedId,
  pdt3470PortalExtraLinks,
  postManualLiabilityOnBillingGroup,
  postManualReceivableOnBillingGroup,
  postPdt3486Compensation,
  putLiabilityOffsettingBlock,
  putReceivableOffsettingBlock,
  realizeBillingRun,
  resolveProductContractBillingGroupId,
  roundMoney,
  startCorrectionDraftForInvoice,
  startGeneratingAndWaitGenerated,
  startPdt3483DebitNoteDraft,
  uniquePdt3486CompensationNumber,
  type InvoiceView,
  type Pdt3486Fx,
} from './pdt-3486-standard-billing-compensation-deferred-alo.fixtures';

function titleFor(scenario: string): string {
  return `[${PDT_3486_KEY}]: ${PDT_3486_TITLE} | ${scenario}`;
}

async function realizeBilling(
  Request: Pdt3486Fx['Request'],
  billingRunId: number,
  invoiceId: number,
): Promise<InvoiceView> {
  return test.step('Action: PATCH start-accounting (realize billing)', async () =>
    realizeBillingRun(Request, billingRunId, invoiceId),
  );
}

async function seedManualReceivableThenRealizeOriginal(
  fx: Pdt3486Fx,
  debit: Awaited<ReturnType<typeof startPdt3483DebitNoteDraft>>,
  blockDuringOriginalAccounting: boolean,
): Promise<{
  billingGroupId: number;
  oldRecId: number;
  oldReceivableCurrentBefore: number;
  originalLiabId: number;
  originalLiabilityCurrentBefore: number;
}> {
  const billingGroupId = await resolveProductContractBillingGroupId(
    fx.Request,
    debit.chain.contractId,
  );
  const oldRec = await test.step(
    'Precondition: POST manual receivable on the contract billing group',
    async () =>
      postManualReceivableOnBillingGroup(fx, {
        customerId: debit.chain.customerId,
        billingGroupId,
        initialAmount: PDT_3486_MANUAL_RECEIVABLE_AMOUNT,
      }),
  );
  if (blockDuringOriginalAccounting) {
    await putReceivableOffsettingBlock(fx, oldRec.id, true);
  }
  await test.step('Action: PATCH start-generating on original volume invoice', async () => {
    await startGeneratingAndWaitGenerated(fx.Request, debit.volumeBillingRunId);
  });
  await realizeBilling(fx.Request, debit.volumeBillingRunId, debit.debitInvoiceId);
  const originalLiab = await findAutomaticLiabilityOnInvoice(
    fx.Request,
    debit.debitInvoiceId,
    debit.T,
  );
  let current = roundMoney((await getReceivable(fx.Request, oldRec.id)).currentAmount);
  if (blockDuringOriginalAccounting) {
    const unblocked = await putReceivableOffsettingBlock(fx, oldRec.id, false);
    current = roundMoney(unblocked.currentAmount);
  }
  expect(current, 'MANUAL receivable still open after original invoice').toBeGreaterThan(0);
  expect(
    roundMoney(originalLiab.currentAmount),
    'original invoice liability stays open while MANUAL receivable was blocked',
  ).toBeGreaterThan(0);
  return {
    billingGroupId,
    oldRecId: oldRec.id,
    oldReceivableCurrentBefore: current,
    originalLiabId: originalLiab.id,
    originalLiabilityCurrentBefore: roundMoney(originalLiab.currentAmount),
  };
}

test.describe(`[${PDT_3486_KEY}]: ${PDT_3486_TITLE}`, {
  tag: ['@billing', '@pdt-3486', '@cursor'],
}, () => {
  test.describe.configure({ fullyParallel: true });

  test(titleFor('TC-BE-1 Positive compensation plus old open receivable'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const C = 50;
    const number = uniquePdt3486CompensationNumber('TCBE1');

    const debit = await startPdt3483DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });
    expect(debit.T, 'invoice totalIncludingVat > 50').toBeGreaterThan(C);

    const billingGroupId = await resolveProductContractBillingGroupId(
      Request,
      debit.chain.contractId,
    );
    const oldRec = await test.step(
      'Precondition: POST manual receivable on the contract billing group',
      async () =>
        postManualReceivableOnBillingGroup(fx, {
          customerId: debit.chain.customerId,
          billingGroupId,
          initialAmount: PDT_3486_MANUAL_RECEIVABLE_AMOUNT,
        }),
    );
    const oldReceivableCurrentBefore = roundMoney(oldRec.currentAmount);

    const compensation = await test.step(
      'Precondition: POST government-compensations after draft (positive C=50)',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3486-TC-BE-1-positive',
        }),
    );
    TestRunSummary.registerPayload('compensation', {
      id: compensation.id,
      number,
      documentAmount: C,
    });
    TestRunSummary.registerPayload('invoice', { id: debit.debitInvoiceId, T: debit.T });
    TestRunSummary.registerPayload('customerReceivable', {
      oldReceivableId: oldRec.id,
      currentBefore: oldReceivableCurrentBefore,
      billingGroupId,
    });

    await test.step('Action: PATCH start-generating (PDF) on volume invoice run', async () => {
      await startGeneratingAndWaitGenerated(Request, debit.volumeBillingRunId);
    });
    await assertCompensationLinkedUninvoiced(Request, compensation.id, debit.debitInvoiceId, 'TC-BE-1 PDF');

    const invoice = await realizeBilling(Request, debit.volumeBillingRunId, debit.debitInvoiceId);
    expect(['INVOICE', 'DEBIT_NOTE'].includes(String(invoice.invoiceDocumentType))).toBe(true);

    const comp = await getCompensation(Request, compensation.id);
    expect(comp.compensationStatus).toBe('INVOICED');
    assertPositiveCompensationPolarity(comp, 'TC-BE-1');

    const compRec = await getReceivable(Request, nestedId(comp.receivableForCustomer) as number);
    const govLiab = await getLiability(Request, nestedId(comp.liabilityForRecipient) as number);
    const invoiceLiab = await findAutomaticLiabilityOnInvoice(Request, debit.debitInvoiceId, debit.T);
    const oldAfter = await getReceivable(Request, oldRec.id);
    const leftover = minMoney(oldReceivableCurrentBefore, roundMoney(debit.T - C));
    const expectedInvoiceCurrent = roundMoney(debit.T - C - leftover);
    const expectedOldCurrent = roundMoney(oldReceivableCurrentBefore - leftover);

    expect(roundMoney(compRec.initialAmount)).toBe(C);
    expect(roundMoney(compRec.currentAmount), 'compensation receivable DLO-settled').toBe(0);
    expect(nestedId(compRec.invoiceResponse)).toBe(debit.debitInvoiceId);
    expect(roundMoney(govLiab.initialAmount)).toBe(C);
    expect(roundMoney(govLiab.currentAmount), 'government liability stays 50 after DLO vs invoice').toBe(C);
    expect(roundMoney(invoiceLiab.currentAmount)).toBe(expectedInvoiceCurrent);
    expect(roundMoney(oldAfter.currentAmount)).toBe(expectedOldCurrent);
    expect(
      !(roundMoney(compRec.currentAmount) === C &&
        roundMoney(oldAfter.currentAmount) < oldReceivableCurrentBefore),
      'PDT-3487 fail pattern: old receivable absorbed the invoice while compensation stayed open',
    ).toBe(true);

    TestRunSummary.recordCheck({
      check: 'TC-BE-1 compensation receivable covers new invoice liability first',
      expectedResult:
        'After start-accounting REAL: positive compensation receivable currentAmount=0; leftover ALO only after DLO vs MANUAL receivable on the same billing group; government liability current=50.',
      actualResult: `status=${invoice.invoiceStatus} T=${debit.T} C=${C} leftover=${leftover} invLiab=${invoiceLiab.currentAmount} oldRec=${oldAfter.currentAmount} compRec=${compRec.currentAmount}`,
      passed:
        invoice.invoiceStatus === 'REAL' &&
        roundMoney(compRec.currentAmount) === 0 &&
        roundMoney(invoiceLiab.currentAmount) === expectedInvoiceCurrent,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3486_KEY,
        relevantEntityKeys: [...PDT_3486_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
          }),
          ...buildProductContractTabLinks(debit.chain.contractId),
        },
        snapshot: {
          tc: 'TC-BE-1',
          invoiceId: debit.debitInvoiceId,
          compensationId: compensation.id,
          oldReceivableId: oldRec.id,
          billingGroupId,
          T: debit.T,
          leftover,
        },
      });
    });
  });

  test(titleFor('TC-BE-2 Positive compensation without an old receivable'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const C = 40;
    const number = uniquePdt3486CompensationNumber('TCBE2');

    const debit = await startPdt3483DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });
    expect(debit.T).toBeGreaterThan(C);

    const billedIdentifier = customerIdentifierById(Responses, debit.chain.customerId);
    const listedBefore = await listReceivablesForCustomer(Request, billedIdentifier);
    for (const row of listedBefore) {
      const detail = await getReceivable(Request, row.id);
      expect(
        !(detail.creationType === 'AUTOMATIC' && roundMoney(detail.currentAmount) > 0),
        'TC-BE-2 billed customer has no open AUTOMATIC receivable',
      ).toBe(true);
    }

    const compensation = await test.step(
      'Precondition: POST government-compensations after draft (positive C=40)',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3486-TC-BE-2-positive',
        }),
    );
    TestRunSummary.registerPayload('compensation', { id: compensation.id, number, documentAmount: C });
    TestRunSummary.registerPayload('invoice', { id: debit.debitInvoiceId, T: debit.T });

    await test.step('Action: PATCH start-generating on FOR_VOLUMES run', async () => {
      await startGeneratingAndWaitGenerated(Request, debit.volumeBillingRunId);
    });
    const linked = await test.step(
      'Read: linked invoice is INVOICE or DEBIT_NOTE (not CREDIT_NOTE)',
      async () => assertLinkedInvoiceIsDebitNote(Request, compensation.id),
    );
    expect(linked.compensation.index).toBe(0);
    expect(linked.compensation.compensationStatus).toBe('UNINVOICED');
    expect(linked.linkedInvoiceId).toBeGreaterThan(0);

    const invoice = await realizeBilling(Request, debit.volumeBillingRunId, linked.linkedInvoiceId);

    const comp = await getCompensation(Request, compensation.id);
    expect(comp.compensationStatus).toBe('INVOICED');
    assertPositiveCompensationPolarity(comp, 'TC-BE-2');
    const compRec = await getReceivable(Request, nestedId(comp.receivableForCustomer) as number);
    const govLiab = await getLiability(Request, nestedId(comp.liabilityForRecipient) as number);
    const invoiceLiab = await findAutomaticLiabilityOnInvoice(Request, linked.linkedInvoiceId, linked.T);

    expect(roundMoney(compRec.initialAmount)).toBe(C);
    expect(roundMoney(compRec.currentAmount)).toBe(0);
    expect(roundMoney(govLiab.currentAmount)).toBe(C);
    expect(roundMoney(invoiceLiab.currentAmount)).toBe(roundMoney(linked.T - C));
    expect(isNullishEntityRef(comp.liabilityForCustomer as { id?: number } | null)).toBe(true);

    TestRunSummary.recordCheck({
      check: 'TC-BE-2 DLO covers compensation receivable against invoice liability',
      expectedResult: 'After start-accounting REAL: compensation receivable current=0; invoice liability current=T-40; government liability current=40.',
      actualResult: `status=${invoice.invoiceStatus} T=${linked.T} invLiab=${invoiceLiab.currentAmount} compRec=${compRec.currentAmount}`,
      passed:
        invoice.invoiceStatus === 'REAL' &&
        roundMoney(compRec.currentAmount) === 0 &&
        roundMoney(invoiceLiab.currentAmount) === roundMoney(linked.T - C),
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3486_KEY,
        relevantEntityKeys: [...PDT_3486_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({ compensationId: compensation.id }),
          ...buildProductContractTabLinks(debit.chain.contractId),
        },
        snapshot: { tc: 'TC-BE-2', invoiceId: linked.linkedInvoiceId, compensationId: compensation.id, T: linked.T },
      });
    });
  });

  test(titleFor('TC-BE-3 Negative compensation creates customer liability and government receivable'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const C = -35;
    const absC = 35;
    const number = uniquePdt3486CompensationNumber('TCBE3');

    const debit = await startPdt3483DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });
    expect(debit.T).toBeGreaterThan(absC);
    const billedIdentifier = customerIdentifierById(Responses, debit.chain.customerId);

    const compensation = await test.step(
      'Precondition: POST government-compensations after draft (negative C=-35)',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3486-TC-BE-3-negative',
        }),
    );
    TestRunSummary.registerPayload('compensation', { id: compensation.id, number, documentAmount: C });
    TestRunSummary.registerPayload('invoice', { id: debit.debitInvoiceId, T: debit.T });

    await test.step('Action: PATCH start-generating on FOR_VOLUMES run', async () => {
      await startGeneratingAndWaitGenerated(Request, debit.volumeBillingRunId);
    });
    const linked = await test.step(
      'Read: linked invoice is INVOICE or DEBIT_NOTE (not CREDIT_NOTE)',
      async () => assertLinkedInvoiceIsDebitNote(Request, compensation.id),
    );
    expect(linked.compensation.index).toBe(0);
    expect(linked.linkedInvoiceId).toBeGreaterThan(0);

    const invoice = await realizeBilling(Request, debit.volumeBillingRunId, linked.linkedInvoiceId);

    const comp = await getCompensation(Request, compensation.id);
    expect(comp.compensationStatus).toBe('INVOICED');
    assertNegativeCompensationPolarity(comp, 'TC-BE-3');

    const custLiab = await getLiability(Request, nestedId(comp.liabilityForCustomer) as number);
    const govRec = await getReceivable(Request, nestedId(comp.receivableForRecipient) as number);
    const invoiceLiab = await findAutomaticLiabilityOnInvoice(Request, linked.linkedInvoiceId, linked.T);

    expect(roundMoney(custLiab.initialAmount)).toBe(absC);
    // PDT-3486/3487 DLO pair is customer compensation receivable vs invoice
    // liability (PDT-3483 positive path). Negative C creates a second billed
    // liability + recipient receivable — ALO cannot offset L vs L, so both
    // stay open (customer pays invoice T and compensation abs(C)).
    expect(roundMoney(custLiab.currentAmount), 'compensation customer liability stays open').toBe(absC);
    expect(nestedId(custLiab.invoiceResponse)).toBe(linked.linkedInvoiceId);
    expect(roundMoney(govRec.initialAmount)).toBe(absC);
    expect(roundMoney(govRec.currentAmount), 'recipient receivable stays open').toBe(absC);
    expect(roundMoney(invoiceLiab.currentAmount)).toBe(roundMoney(linked.T));

    const listed = await listReceivablesForCustomer(Request, billedIdentifier);
    for (const row of listed) {
      const detail = await getReceivable(Request, row.id);
      expect(
        !(
          detail.creationType === 'AUTOMATIC' &&
          nestedId(detail.invoiceResponse) === linked.linkedInvoiceId &&
          roundMoney(detail.initialAmount) === absC &&
          nestedNumericCustomer(detail) === debit.chain.customerId
        ),
        'TC-BE-3 must not create a billed-customer receivable of 35.00 on this invoice',
      ).toBe(true);
    }

    TestRunSummary.recordCheck({
      check: 'TC-BE-3 negative polarity: customer pays invoice T and compensation liability abs(C)',
      expectedResult:
        'After start-accounting REAL: liabilityForCustomer current=35; receivableForRecipient current=35; invoice liability current=T; no billed-customer receivable of 35.',
      actualResult: `status=${invoice.invoiceStatus} T=${linked.T} invLiab=${invoiceLiab.currentAmount} custLiab=${custLiab.currentAmount} govRec=${govRec.currentAmount}`,
      passed:
        invoice.invoiceStatus === 'REAL' &&
        roundMoney(custLiab.currentAmount) === absC &&
        roundMoney(govRec.currentAmount) === absC &&
        roundMoney(invoiceLiab.currentAmount) === roundMoney(linked.T),
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3486_KEY,
        relevantEntityKeys: [...PDT_3486_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({ compensationId: compensation.id }),
          ...buildProductContractTabLinks(debit.chain.contractId),
        },
        snapshot: { tc: 'TC-BE-3', invoiceId: linked.linkedInvoiceId, compensationId: compensation.id, T: linked.T },
      });
    });
  });

  test(titleFor('TC-BE-4 Invoice without compensation still runs ALO once'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };

    const debit = await startPdt3483DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
    });
    expect(debit.T, 'volume invoice totalIncludingVat').toBeGreaterThan(0);
    TestRunSummary.registerPayload('invoice', { id: debit.debitInvoiceId, T: debit.T });
    const billedIdentifier = customerIdentifierById(Responses, debit.chain.customerId);
    const billingGroupId = await resolveProductContractBillingGroupId(
      Request,
      debit.chain.contractId,
    );
    const oldRec = await test.step(
      'Precondition: POST manual receivable on the contract billing group',
      async () =>
        postManualReceivableOnBillingGroup(fx, {
          customerId: debit.chain.customerId,
          billingGroupId,
          initialAmount: PDT_3486_MANUAL_RECEIVABLE_AMOUNT,
        }),
    );
    const oldReceivableCurrentBefore = roundMoney(oldRec.currentAmount);
    TestRunSummary.registerPayload('customerReceivable', {
      oldReceivableId: oldRec.id,
      currentBefore: oldReceivableCurrentBefore,
      billingGroupId,
    });

    await test.step('Action: PATCH start-generating (PDF) — no compensation on this run', async () => {
      await startGeneratingAndWaitGenerated(Request, debit.volumeBillingRunId);
    });

    const listing = await listCompensationsForCustomer(Request, billedIdentifier);
    expect(
      listing.some((row) => nestedId(row.invoice) === debit.debitInvoiceId),
      'no compensation linked to this invoice before accounting',
    ).toBe(false);

    const invoice = await realizeBilling(Request, debit.volumeBillingRunId, debit.debitInvoiceId);

    const invoiceLiab = await findAutomaticLiabilityOnInvoice(Request, debit.debitInvoiceId, debit.T);
    expect(Boolean(invoiceLiab.blockedForLiabilitiesOffsetting)).toBe(false);
    const oldAfter = await getReceivable(Request, oldRec.id);
    const offsetAmount = minMoney(oldReceivableCurrentBefore, debit.T);
    expect(offsetAmount).toBeGreaterThan(0);
    expect(roundMoney(invoiceLiab.currentAmount)).toBe(roundMoney(debit.T - offsetAmount));
    expect(roundMoney(oldAfter.currentAmount)).toBe(
      roundMoney(oldReceivableCurrentBefore - offsetAmount),
    );

    const listingAfter = await listCompensationsForCustomer(Request, billedIdentifier);
    expect(
      listingAfter.some(
        (row) =>
          nestedId(row.invoice) === debit.debitInvoiceId && row.compensationStatus === 'INVOICED',
      ),
      'no INVOICED compensation on this invoice',
    ).toBe(false);

    TestRunSummary.recordCheck({
      check: 'TC-BE-4 ALO still runs once when no compensations apply',
      expectedResult: 'After start-accounting REAL: MANUAL receivable on the same billing group offsets the new invoice liability; no compensation INVOICED on this invoice.',
      actualResult: `status=${invoice.invoiceStatus} offset=${offsetAmount} invLiab=${invoiceLiab.currentAmount} oldRec=${oldAfter.currentAmount}`,
      passed:
        invoice.invoiceStatus === 'REAL' &&
        offsetAmount > 0 &&
        roundMoney(invoiceLiab.currentAmount) === roundMoney(debit.T - offsetAmount),
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3486_KEY,
        relevantEntityKeys: [...PDT_3486_RELEVANT_ENTITY_KEYS],
        extraLinks: buildProductContractTabLinks(debit.chain.contractId),
        snapshot: {
          tc: 'TC-BE-4',
          invoiceId: debit.debitInvoiceId,
          oldReceivableId: oldRec.id,
          billingGroupId,
          offsetAmount,
        },
      });
    });
  });

  test(titleFor('TC-BE-5 STANDARD Credit Note with positive compensation'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const C = 80;
    const number = uniquePdt3486CompensationNumber('TCBE5');

    const debit = await startPdt3483DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });
    expect(debit.T).toBeGreaterThan(C);
    const billedIdentifier = customerIdentifierById(Responses, debit.chain.customerId);
    const billingGroupId = await resolveProductContractBillingGroupId(
      Request,
      debit.chain.contractId,
    );
    const oldLiab = await test.step(
      'Precondition: POST manual liability on the contract billing group',
      async () =>
        postManualLiabilityOnBillingGroup(fx, {
          customerId: debit.chain.customerId,
          billingGroupId,
          initialAmount: PDT_3486_MANUAL_LIABILITY_AMOUNT,
        }),
    );
    await putLiabilityOffsettingBlock(fx, oldLiab.id, true);
    const oldRec = await test.step(
      'Precondition: POST manual receivable on the contract billing group',
      async () =>
        postManualReceivableOnBillingGroup(fx, {
          customerId: debit.chain.customerId,
          billingGroupId,
          initialAmount: PDT_3486_MANUAL_RECEIVABLE_AMOUNT,
        }),
    );
    const oldReceivableCurrentBefore = roundMoney(oldRec.currentAmount);
    const oldLiabilityCurrentBefore = roundMoney(oldLiab.currentAmount);

    const compensation = await test.step(
      'Precondition: POST government-compensations after draft (positive C=80)',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3486-TC-BE-5-positive',
        }),
    );
    TestRunSummary.registerPayload('compensation', { id: compensation.id, number, documentAmount: C });
    TestRunSummary.registerPayload('invoice', { id: debit.debitInvoiceId, T: debit.T });

    await test.step('Action: PATCH start-generating (PDF) on volume invoice run', async () => {
      await startGeneratingAndWaitGenerated(Request, debit.volumeBillingRunId);
    });
    await assertCompensationLinkedUninvoiced(
      Request,
      compensation.id,
      debit.debitInvoiceId,
      'TC-BE-5 PDF',
    );

    const invoice = await realizeBilling(Request, debit.volumeBillingRunId, debit.debitInvoiceId);
    expect(['INVOICE', 'DEBIT_NOTE'].includes(String(invoice.invoiceDocumentType))).toBe(true);

    const comp = await getCompensation(Request, compensation.id);
    expect(comp.compensationStatus).toBe('INVOICED');
    assertPositiveCompensationPolarity(comp, 'TC-BE-5');
    const compRec = await getReceivable(Request, nestedId(comp.receivableForCustomer) as number);
    const govLiab = await getLiability(Request, nestedId(comp.liabilityForRecipient) as number);
    const invoiceLiab = await findAutomaticLiabilityOnInvoice(Request, debit.debitInvoiceId, debit.T);
    const recAfter = await getReceivable(Request, oldRec.id);
    const liabAfter = await getLiability(Request, oldLiab.id);
    const leftover = minMoney(oldReceivableCurrentBefore, roundMoney(debit.T - C));

    expect(compRec.id).not.toBe(oldRec.id);
    expect(roundMoney(compRec.initialAmount)).toBe(C);
    expect(roundMoney(compRec.currentAmount)).toBe(0);
    expect(roundMoney(govLiab.currentAmount)).toBe(C);
    expect(roundMoney(invoiceLiab.currentAmount)).toBe(roundMoney(debit.T - C - leftover));
    expect(roundMoney(recAfter.currentAmount)).toBe(roundMoney(oldReceivableCurrentBefore - leftover));
    expect(
      roundMoney(liabAfter.currentAmount),
      'MANUAL liability is not L-vs-L leftover ALO against the invoice',
    ).toBe(oldLiabilityCurrentBefore);

    TestRunSummary.recordCheck({
      check: 'TC-BE-5 compensation DLO then leftover ALO vs MANUAL receivable; MANUAL liability stays',
      expectedResult:
        'After start-accounting REAL: compensation receivable current=0; leftover ALO uses MANUAL receivable; MANUAL liability current stays 200.',
      actualResult: `status=${invoice.invoiceStatus} T=${debit.T} leftover=${leftover} invLiab=${invoiceLiab.currentAmount} oldRec=${recAfter.currentAmount} oldLiab=${liabAfter.currentAmount} compRec=${compRec.currentAmount}`,
      passed:
        invoice.invoiceStatus === 'REAL' &&
        roundMoney(compRec.currentAmount) === 0 &&
        compRec.id !== oldRec.id,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3486_KEY,
        relevantEntityKeys: [...PDT_3486_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({ compensationId: compensation.id }),
          ...buildProductContractTabLinks(debit.chain.contractId),
        },
        snapshot: {
          tc: 'TC-BE-5',
          invoiceId: debit.debitInvoiceId,
          compensationId: compensation.id,
          oldReceivableId: oldRec.id,
          oldLiabilityId: oldLiab.id,
          billedIdentifier,
        },
      });
    });
  });

  test(titleFor('TC-BE-6 Correction INVOICE/DN with positive compensation plus old receivable'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const C = 50;
    const number = uniquePdt3486CompensationNumber('TCBE6');

    const debit = await startPdt3483DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });
    expect(debit.T).toBeGreaterThan(C);
    const billedIdentifier = customerIdentifierById(Responses, debit.chain.customerId);
    const seeded = await seedManualReceivableThenRealizeOriginal(fx, debit, true);

    const correction = await test.step(
      'Precondition: Invoice Correction draft of the REAL invoice (no extra BBP, no cancellation)',
      async () => startCorrectionDraftForInvoice(fx, debit.debitInvoiceId),
    );
    const classified = await classifyCorrectionDrafts(Request, correction.draftInvoiceIds);
    const forwardInvoiceId = classified.forwardInvoiceId;
    expect(
      forwardInvoiceId,
      `forward CORRECTION INVOICE/DEBIT_NOTE (not cancellation); drafts=${JSON.stringify(classified.allDocs)}`,
    ).toBeGreaterThan(0);
    const forward = await getInvoice(Request, forwardInvoiceId as number);
    expect(forward.invoiceType).toBe('CORRECTION');
    expect(['INVOICE', 'DEBIT_NOTE'].includes(String(forward.invoiceDocumentType))).toBe(true);
    const forwardTotal = roundMoney(forward.totalAmountIncludingVat);
    expect(forwardTotal).toBeGreaterThan(C);

    const compensation = await test.step(
      'Precondition: POST government-compensations after correction draft (positive C=50)',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3486-TC-BE-6-correction-positive',
        }),
    );
    TestRunSummary.registerPayload('compensation', { id: compensation.id, number, documentAmount: C });

    await test.step('Action: PATCH start-generating (PDF) on correction run', async () => {
      await startGeneratingAndWaitGenerated(Request, correction.correctionBillingRunId);
    });
    const linked = await getCompensation(Request, compensation.id);
    const linkedInvoiceId = nestedId(linked.invoice) as number;
    const linkedDoc = await getInvoice(Request, linkedInvoiceId);
    expect(
      ['INVOICE', 'DEBIT_NOTE'].includes(String(linkedDoc.invoiceDocumentType)),
      `TC-BE-6 linked document is INVOICE/DEBIT_NOTE, got ${linkedDoc.invoiceDocumentType}`,
    ).toBe(true);
    expect(String(linkedDoc.invoiceType)).toBe('CORRECTION');
    expect(linkedInvoiceId).toBe(forwardInvoiceId);
    expect(linked.index).toBe(0);
    expect(linked.compensationStatus).toBe('UNINVOICED');

    const realized = await realizeBilling(Request, correction.correctionBillingRunId, forwardInvoiceId as number);
    const comp = await getCompensation(Request, compensation.id);
    expect(comp.compensationStatus).toBe('INVOICED');
    assertPositiveCompensationPolarity(comp, 'TC-BE-6');

    const compRec = await getReceivable(Request, nestedId(comp.receivableForCustomer) as number);
    const govLiab = await getLiability(Request, nestedId(comp.liabilityForRecipient) as number);
    const oldAfter = await getReceivable(Request, seeded.oldRecId);
    const origAfter = await getLiability(Request, seeded.originalLiabId);
    const leftover = minMoney(
      seeded.oldReceivableCurrentBefore,
      seeded.originalLiabilityCurrentBefore,
    );
    expect(roundMoney(compRec.currentAmount)).toBe(0);
    expect(roundMoney(govLiab.currentAmount)).toBe(C);
    expect(
      roundMoney(oldAfter.currentAmount),
      'correction CN/DN pair settles the new liability; MANUAL leftover receivable stays open',
    ).toBe(seeded.oldReceivableCurrentBefore);
    expect(roundMoney(origAfter.currentAmount)).toBe(
      roundMoney(seeded.originalLiabilityCurrentBefore - C),
    );

    TestRunSummary.recordCheck({
      check: 'TC-BE-6 compensation DLO on correction; leftover MANUAL receivable not consumed by CN/DN pair',
      expectedResult:
        'After start-accounting REAL: compensation receivable current=0; MANUAL receivable current stays 200 because Invoice Correction reversal/rebill settles internally.',
      actualResult: `status=${realized.invoiceStatus} billed=${billedIdentifier} leftoverUnused=${leftover} origLiab=${origAfter.currentAmount} oldRec=${oldAfter.currentAmount} compRec=${compRec.currentAmount}`,
      passed:
        realized.invoiceStatus === 'REAL' &&
        roundMoney(compRec.currentAmount) === 0 &&
        roundMoney(oldAfter.currentAmount) === seeded.oldReceivableCurrentBefore,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3486_KEY,
        relevantEntityKeys: [...PDT_3486_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({ compensationId: compensation.id }),
          ...buildProductContractTabLinks(debit.chain.contractId),
        },
        snapshot: {
          tc: 'TC-BE-6',
          forwardInvoiceId,
          originalInvoiceId: debit.debitInvoiceId,
          compensationId: compensation.id,
          oldReceivableId: seeded.oldRecId,
          billingGroupId: seeded.billingGroupId,
        },
      });
    });
  });

  test(titleFor('TC-BE-7 Correction Credit Note with positive compensation'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const C = 50;
    const number = uniquePdt3486CompensationNumber('TCBE7');

    const debit = await startPdt3483DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });
    expect(debit.T).toBeGreaterThan(C);
    const billedIdentifier = customerIdentifierById(Responses, debit.chain.customerId);
    const billingGroupId = await resolveProductContractBillingGroupId(
      Request,
      debit.chain.contractId,
    );
    const oldRec = await test.step(
      'Precondition: POST manual receivable on the contract billing group',
      async () =>
        postManualReceivableOnBillingGroup(fx, {
          customerId: debit.chain.customerId,
          billingGroupId,
          initialAmount: PDT_3486_MANUAL_RECEIVABLE_AMOUNT,
        }),
    );
    await putReceivableOffsettingBlock(fx, oldRec.id, true);
    const oldLiab = await test.step(
      'Precondition: POST manual liability on the contract billing group',
      async () =>
        postManualLiabilityOnBillingGroup(fx, {
          customerId: debit.chain.customerId,
          billingGroupId,
          initialAmount: PDT_3486_MANUAL_LIABILITY_AMOUNT,
        }),
    );
    const oldLiabilityCurrentBefore = roundMoney(oldLiab.currentAmount);
    await test.step('Action: PATCH start-generating on original volume invoice', async () => {
      await startGeneratingAndWaitGenerated(Request, debit.volumeBillingRunId);
    });
    await realizeBilling(Request, debit.volumeBillingRunId, debit.debitInvoiceId);
    const originalLiab = await findAutomaticLiabilityOnInvoice(Request, debit.debitInvoiceId, debit.T);
    const originalLiabilityCurrentBefore = roundMoney(originalLiab.currentAmount);
    expect(roundMoney((await getReceivable(Request, oldRec.id)).currentAmount)).toBe(
      PDT_3486_MANUAL_RECEIVABLE_AMOUNT,
    );

    const correction = await test.step(
      'Precondition: Invoice Correction draft of the REAL invoice (no extra BBP, no cancellation)',
      async () => startCorrectionDraftForInvoice(fx, debit.debitInvoiceId),
    );
    const classified = await classifyCorrectionDrafts(Request, correction.draftInvoiceIds);
    expect(
      classified.correctionCnId,
      `CORRECTION CREDIT_NOTE draft (not invoice cancellation); drafts=${JSON.stringify(classified.allDocs)}`,
    ).toBeGreaterThan(0);
    const cnTotal = classified.allDocs.find((d) => d.id === classified.correctionCnId)?.total ?? 0;
    expect(cnTotal).not.toBe(C);

    const compensation = await test.step(
      'Precondition: POST government-compensations after correction draft (positive C=50)',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3486-TC-BE-7-correction-cn',
        }),
    );
    TestRunSummary.registerPayload('compensation', { id: compensation.id, number, documentAmount: C });

    await test.step('Action: PATCH start-generating (PDF) on correction run', async () => {
      await startGeneratingAndWaitGenerated(Request, correction.correctionBillingRunId);
    });
    const linked = await getCompensation(Request, compensation.id);
    const linkedDoc = await getInvoice(Request, nestedId(linked.invoice) as number);
    expect(String(linkedDoc.invoiceType)).toBe('CORRECTION');
    expect(
      ['INVOICE', 'DEBIT_NOTE', 'CREDIT_NOTE'].includes(String(linkedDoc.invoiceDocumentType)),
      `TC-BE-7 linked CORRECTION document, got ${linkedDoc.invoiceDocumentType}`,
    ).toBe(true);
    await putReceivableOffsettingBlock(fx, oldRec.id, false);

    const realized = await realizeBilling(
      Request,
      correction.correctionBillingRunId,
      nestedId(linked.invoice) as number,
    );
    expect(realized.invoiceType).toBe('CORRECTION');
    const comp = await getCompensation(Request, compensation.id);
    expect(comp.compensationStatus).toBe('INVOICED');
    assertPositiveCompensationPolarity(comp, 'TC-BE-7');

    const compRec = await getReceivable(Request, nestedId(comp.receivableForCustomer) as number);
    const govLiab = await getLiability(Request, nestedId(comp.liabilityForRecipient) as number);
    const oldRecAfter = await getReceivable(Request, oldRec.id);
    const oldLiabAfter = await getLiability(Request, oldLiab.id);
    const origAfter = await getLiability(Request, originalLiab.id);
    const leftover = minMoney(PDT_3486_MANUAL_RECEIVABLE_AMOUNT, originalLiabilityCurrentBefore);

    expect(roundMoney(compRec.initialAmount)).toBe(C);
    expect(roundMoney(compRec.currentAmount)).toBe(0);
    expect(roundMoney(govLiab.currentAmount)).toBe(C);
    expect(roundMoney(oldRecAfter.currentAmount)).toBe(PDT_3486_MANUAL_RECEIVABLE_AMOUNT);
    expect(roundMoney(origAfter.currentAmount)).toBe(
      roundMoney(originalLiabilityCurrentBefore - C),
    );
    expect(
      roundMoney(oldLiabAfter.currentAmount),
      'MANUAL liability is not leftover ALO against two liabilities',
    ).toBe(oldLiabilityCurrentBefore);

    TestRunSummary.recordCheck({
      check: 'TC-BE-7 correction CREDIT_NOTE draft exists; compensation DLO; leftover MANUAL items stay',
      expectedResult:
        'After start-accounting REAL: CORRECTION CREDIT_NOTE draft exists (not invoice cancellation); compensation receivable current=0; MANUAL receivable and liability stay 200.',
      actualResult: `status=${realized.invoiceStatus} linked=${linkedDoc.invoiceDocumentType} leftoverUnused=${leftover} origAfter=${origAfter.currentAmount} oldRec=${oldRecAfter.currentAmount} oldLiab=${oldLiabAfter.currentAmount} compRec=${compRec.currentAmount}`,
      passed:
        realized.invoiceStatus === 'REAL' &&
        roundMoney(compRec.currentAmount) === 0 &&
        realized.invoiceType === 'CORRECTION',
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3486_KEY,
        relevantEntityKeys: [...PDT_3486_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: classified.correctionCnId as number,
          }),
          ...buildProductContractTabLinks(debit.chain.contractId),
        },
        snapshot: {
          tc: 'TC-BE-7',
          correctionCnId: classified.correctionCnId,
          compensationId: compensation.id,
          originalInvoiceId: debit.debitInvoiceId,
          oldLiabilityId: oldLiab.id,
          oldReceivableId: oldRec.id,
          billingGroupId,
        },
      });
    });
  });

  test(titleFor('TC-BE-8 Invoice Correction without compensation still runs ALO once'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };

    const debit = await startPdt3483DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
    });
    const billedIdentifier = customerIdentifierById(Responses, debit.chain.customerId);
    const seeded = await seedManualReceivableThenRealizeOriginal(fx, debit, true);
    TestRunSummary.registerPayload('invoice', { id: debit.debitInvoiceId, T: debit.T });
    TestRunSummary.registerPayload('customerReceivable', {
      oldReceivableId: seeded.oldRecId,
      billingGroupId: seeded.billingGroupId,
    });

    const correction = await test.step(
      'Precondition: Invoice Correction draft of the REAL invoice (no extra BBP, no cancellation)',
      async () => startCorrectionDraftForInvoice(fx, debit.debitInvoiceId),
    );
    const classified = await classifyCorrectionDrafts(Request, correction.draftInvoiceIds);
    expect(
      classified.forwardInvoiceId,
      `forward CORRECTION INVOICE/DEBIT_NOTE (not cancellation); drafts=${JSON.stringify(classified.allDocs)}`,
    ).toBeGreaterThan(0);
    const forward = await getInvoice(Request, classified.forwardInvoiceId as number);
    expect(String(forward.invoiceType)).toBe('CORRECTION');
    const forwardTotal = roundMoney(forward.totalAmountIncludingVat);
    expect(forwardTotal).toBeGreaterThan(0);

    await test.step('Action: PATCH start-generating (PDF) on correction run — no compensation', async () => {
      await startGeneratingAndWaitGenerated(Request, correction.correctionBillingRunId);
    });
    const listing = await listCompensationsForCustomer(Request, billedIdentifier);
    expect(
      listing.some((row) => nestedId(row.invoice) === classified.forwardInvoiceId),
      'no compensation on forward correction document',
    ).toBe(false);

    const realized = await realizeBilling(
      Request,
      correction.correctionBillingRunId,
      classified.forwardInvoiceId as number,
    );
    const oldAfter = await getReceivable(Request, seeded.oldRecId);
    const origAfter = await getLiability(Request, seeded.originalLiabId);
    const offsetAmount = minMoney(
      seeded.oldReceivableCurrentBefore,
      seeded.originalLiabilityCurrentBefore,
    );
    expect(offsetAmount).toBeGreaterThan(0);
    expect(
      roundMoney(oldAfter.currentAmount),
      'correction CN/DN pair settles internally; MANUAL leftover receivable stays open',
    ).toBe(seeded.oldReceivableCurrentBefore);
    expect(roundMoney(origAfter.currentAmount)).toBe(seeded.originalLiabilityCurrentBefore);

    const listingAfter = await listCompensationsForCustomer(Request, billedIdentifier);
    expect(
      listingAfter.some(
        (row) =>
          nestedId(row.invoice) === classified.forwardInvoiceId &&
          row.compensationStatus === 'INVOICED',
      ),
    ).toBe(false);

    TestRunSummary.recordCheck({
      check: 'TC-BE-8 correction ALO once when no compensations apply',
      expectedResult:
        'After start-accounting REAL: Invoice Correction still accounts; MANUAL leftover receivable stays 200; no compensation INVOICED on the forward document.',
      actualResult: `status=${realized.invoiceStatus} offsetUnused=${offsetAmount} origLiab=${origAfter.currentAmount} oldRec=${oldAfter.currentAmount}`,
      passed:
        realized.invoiceStatus === 'REAL' &&
        offsetAmount > 0 &&
        roundMoney(oldAfter.currentAmount) === seeded.oldReceivableCurrentBefore,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3486_KEY,
        relevantEntityKeys: [...PDT_3486_RELEVANT_ENTITY_KEYS],
        extraLinks: buildProductContractTabLinks(debit.chain.contractId),
        snapshot: {
          tc: 'TC-BE-8',
          forwardInvoiceId: classified.forwardInvoiceId,
          originalInvoiceId: debit.debitInvoiceId,
          oldReceivableId: seeded.oldRecId,
          billingGroupId: seeded.billingGroupId,
          offsetAmount,
        },
      });
    });
  });
});

function customerIdentifierById(
  Responses: { customer: Array<{ id?: number; identifier?: string }> },
  customerId: number,
): string {
  const row = Responses.customer.find((c) => Number(c.id) === customerId);
  return String(row?.identifier ?? '');
}

function nestedNumericCustomer(detail: { customerResponse?: { id?: number } | null; customerId?: unknown }): number | null {
  const nested = nestedId(detail.customerResponse);
  if (nested != null) {
    return nested;
  }
  const n = Number(detail.customerId);
  return Number.isFinite(n) && n > 0 ? n : null;
}
