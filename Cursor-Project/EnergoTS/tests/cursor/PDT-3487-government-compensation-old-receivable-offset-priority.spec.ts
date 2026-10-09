/**
 * PDT-3487 — Government compensation - Incorrect offsetting when the customer has old receivable
 *
 * Jira (customfield_10103): an open older receivable must not absorb the invoice
 * liability before the compensation receivable that belongs to that invoice.
 * Expected: compensation receivable offsets the invoice liability first; the older
 * receivable offsets only the leftover (invoice total minus compensation).
 *
 * Environment: Dev2 (BASE_URL https://devapps.energo-pro.bg/backend/phoenix2-dev).
 * Swagger: Cursor-Project/config/swagger/dev2/swagger-spec.json refreshed this session
 * (update-swagger-specs.ps1 failed: curl.exe missing on macOS; system curl wrote the same files).
 * - POST /government-compensations CompensationRequest
 *   required: customerId, date, documentAmount, documentCurrencyId, documentPeriod,
 *   number, podId, price, reason, recipientId, volumes
 * - POST /customer-receivable CustomerReceivableRequest
 *   required: accountingPeriodId, currencyId, customerId, dueDate, initialAmount, occurrenceDate
 *   blockedForOffsetting: boolean
 * - PATCH /billing-run/start-generating?billingRunId
 * - PATCH /billing-run/start-accounting?billingRunId
 * - GET /customer-receivable/{id} customerOffsettingResponseList
 * - GET /customer-liability/{id} customerLiabilityOffsettingReponseList
 *   CustomerOffsettingResponse.offsettingObject enum:
 *   PAYMENT | RECEIVABLE | LIABILITY | DEPOSIT | RESCHEDULING
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3486-standard-billing-compensation-deferred-alo.spec.ts
 * - tests/cursor/pdt-3486-standard-billing-compensation-deferred-alo.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3486_MANUAL_RECEIVABLE_AMOUNT,
  PDT_3486_RELEVANT_ENTITY_KEYS,
  PDT_3486_TEST_TIMEOUT_MS,
  assertCompensationLinkedUninvoiced,
  assertPositiveCompensationPolarity,
  findAutomaticLiabilityOnInvoice,
  getCompensation,
  getLiability,
  getReceivable,
  minMoney,
  nestedId,
  pdt3470PortalExtraLinks,
  postManualReceivableOnBillingGroup,
  postPdt3486Compensation,
  realizeBillingRun,
  resolveProductContractBillingGroupId,
  roundMoney,
  startGeneratingAndWaitGenerated,
  startPdt3483DebitNoteDraft,
  uniquePdt3486CompensationNumber,
  type Pdt3470LiabilityView,
  type Pdt3470ReceivableView,
  type Pdt3486Fx,
} from './pdt-3486-standard-billing-compensation-deferred-alo.fixtures';

const PDT_3487_KEY = 'PDT-3487';
const PDT_3487_TITLE =
  'Government compensation - Incorrect offsetting when the customer has old receivable';

type OffsetRow = {
  amount?: number | string;
  offsettingObject?: string | null;
  offsettingObjectType?: string | null;
  description?: string;
  date?: string;
};

function offsetRows(view: Pdt3470ReceivableView | Pdt3470LiabilityView, key: string): OffsetRow[] {
  const raw = view[key];
  return Array.isArray(raw) ? (raw as OffsetRow[]) : [];
}

/** Dev2 returns the counterpart kind on offsettingObjectType. Liability-side amounts are negative. */
function offsetSum(rows: OffsetRow[], objectType: string): number {
  return roundMoney(
    rows
      .filter((row) => String(row.offsettingObjectType ?? '').toUpperCase() === objectType)
      .reduce((sum, row) => sum + Math.abs(roundMoney(row.amount)), 0),
  );
}

test.describe(`[${PDT_3487_KEY}]: ${PDT_3487_TITLE}`, {
  tag: ['@billing', '@dev2', '@pdt-3487', '@cursor'],
}, () => {
  test(`[${PDT_3487_KEY}]: ${PDT_3487_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const compensationAmount = 50;
    const number = uniquePdt3486CompensationNumber('PDT3487');

    const debit = await startPdt3483DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });
    expect(debit.T, 'invoice totalIncludingVat must exceed the compensation').toBeGreaterThan(
      compensationAmount,
    );

    const billingGroupId = await resolveProductContractBillingGroupId(
      Request,
      debit.chain.contractId,
    );
    const oldReceivable = await test.step(
      'Precondition: POST an open receivable on the contract billing group before compensation accounting',
      async () =>
        postManualReceivableOnBillingGroup(fx, {
          customerId: debit.chain.customerId,
          billingGroupId,
          initialAmount: PDT_3486_MANUAL_RECEIVABLE_AMOUNT,
        }),
    );
    const oldReceivableCurrentBefore = roundMoney(oldReceivable.currentAmount);
    expect(oldReceivable.creationType).toBe('MANUAL');
    expect(oldReceivableCurrentBefore).toBe(PDT_3486_MANUAL_RECEIVABLE_AMOUNT);

    const compensation = await test.step(
      'Precondition: POST government compensation on the draft invoice (positive amount)',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: compensationAmount,
          number,
          reason: 'PDT-3487-old-receivable-offset-priority',
        }),
    );
    TestRunSummary.registerPayload('compensation', {
      id: compensation.id,
      number,
      documentAmount: compensationAmount,
    });
    TestRunSummary.registerPayload('invoice', { id: debit.debitInvoiceId, T: debit.T });
    TestRunSummary.registerPayload('customerReceivable', {
      oldReceivableId: oldReceivable.id,
      currentBefore: oldReceivableCurrentBefore,
      billingGroupId,
    });

    await test.step('Action: PATCH start-generating so the compensation links to the invoice', async () => {
      await startGeneratingAndWaitGenerated(Request, debit.volumeBillingRunId);
    });
    await assertCompensationLinkedUninvoiced(
      Request,
      compensation.id,
      debit.debitInvoiceId,
      'PDT-3487 PDF',
    );

    const invoice = await test.step(
      'Action: PATCH start-accounting and wait until the invoice is REAL',
      async () => realizeBillingRun(Request, debit.volumeBillingRunId, debit.debitInvoiceId),
    );

    const compensationView = await getCompensation(Request, compensation.id);
    expect(compensationView.compensationStatus).toBe('INVOICED');
    assertPositiveCompensationPolarity(compensationView, 'PDT-3487');

    const compensationReceivableId = nestedId(compensationView.receivableForCustomer) as number;
    const governmentLiabilityId = nestedId(compensationView.liabilityForRecipient) as number;
    const compensationReceivable = await getReceivable(Request, compensationReceivableId);
    const governmentLiability = await getLiability(Request, governmentLiabilityId);
    const invoiceLiability = await findAutomaticLiabilityOnInvoice(
      Request,
      debit.debitInvoiceId,
      debit.T,
    );
    const oldReceivableAfter = await getReceivable(Request, oldReceivable.id);

    const leftover = minMoney(oldReceivableCurrentBefore, roundMoney(debit.T - compensationAmount));
    const expectedInvoiceCurrent = roundMoney(debit.T - compensationAmount - leftover);
    const expectedOldCurrent = roundMoney(oldReceivableCurrentBefore - leftover);
    const compensationOffset = offsetSum(
      offsetRows(compensationReceivable, 'customerOffsettingResponseList'),
      'LIABILITY',
    );
    const oldReceivableOffset = offsetSum(
      offsetRows(oldReceivableAfter, 'customerOffsettingResponseList'),
      'LIABILITY',
    );
    const invoiceReceivableOffsets = offsetSum(
      offsetRows(invoiceLiability, 'customerLiabilityOffsettingReponseList'),
      'RECEIVABLE',
    );

    const compensationCurrent = roundMoney(compensationReceivable.currentAmount);
    const oldCurrent = roundMoney(oldReceivableAfter.currentAmount);
    const invoiceCurrent = roundMoney(invoiceLiability.currentAmount);
    const bugPattern =
      compensationCurrent === compensationAmount && oldCurrent < oldReceivableCurrentBefore;

    expect(roundMoney(compensationReceivable.initialAmount)).toBe(compensationAmount);
    expect(nestedId(compensationReceivable.invoiceResponse)).toBe(debit.debitInvoiceId);
    expect(compensationCurrent, 'compensation receivable offsets the invoice first').toBe(0);
    expect(compensationOffset, 'compensation receivable liability offset equals document amount').toBe(
      compensationAmount,
    );
    expect(roundMoney(governmentLiability.currentAmount), 'government liability is not the customer offset').toBe(
      compensationAmount,
    );
    expect(oldCurrent, 'older receivable is reduced only by the leftover after compensation').toBe(
      expectedOldCurrent,
    );
    expect(oldReceivableOffset, 'older receivable offsets only the leftover').toBe(leftover);
    expect(invoiceCurrent, 'invoice liability is reduced by compensation and then leftover').toBe(
      expectedInvoiceCurrent,
    );
    expect(invoiceReceivableOffsets).toBe(roundMoney(compensationAmount + leftover));
    expect(
      bugPattern,
      'PDT-3487 fail pattern: older receivable absorbed the invoice while the compensation receivable stayed open',
    ).toBe(false);

    const passed =
      invoice.invoiceStatus === 'REAL' &&
      compensationCurrent === 0 &&
      oldCurrent === expectedOldCurrent &&
      invoiceCurrent === expectedInvoiceCurrent &&
      !bugPattern;

    TestRunSummary.recordCheck({
      check: 'Compensation receivable offsets the invoice liability before the older receivable',
      expectedResult:
        'After start-accounting REAL: compensation receivable currentAmount=0 and its liability offset equals the compensation amount; the older receivable current amount falls only by min(old receivable, invoice total - compensation); invoice liability current amount is invoice total - compensation - that leftover. The older receivable must not absorb the invoice while the compensation receivable stays at its full amount.',
      actualResult:
        `status=${invoice.invoiceStatus} T=${debit.T} C=${compensationAmount} leftover=${leftover} ` +
        `invLiab=${invoiceLiability.currentAmount} oldRec=${oldReceivableAfter.currentAmount} ` +
        `compRec=${compensationReceivable.currentAmount} compOffset=${compensationOffset} ` +
        `oldOffset=${oldReceivableOffset} invoiceReceivableOffsets=${invoiceReceivableOffsets} ` +
        `bugPattern=${bugPattern}`,
      passed,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3487_KEY,
        relevantEntityKeys: [...PDT_3486_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({ compensationId: compensation.id }),
          ...buildProductContractTabLinks(debit.chain.contractId),
        },
        snapshot: {
          invoiceId: debit.debitInvoiceId,
          compensationId: compensation.id,
          compensationReceivableId,
          oldReceivableId: oldReceivable.id,
          invoiceLiabilityId: invoiceLiability.id,
          billingGroupId,
          invoiceTotal: debit.T,
          compensationAmount,
          leftover,
          expectedInvoiceCurrent,
          expectedOldCurrent,
        },
      });
    });
  });
});
