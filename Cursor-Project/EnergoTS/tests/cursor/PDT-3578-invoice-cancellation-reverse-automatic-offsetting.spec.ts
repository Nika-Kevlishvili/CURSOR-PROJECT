/**
 * PDT-3578 — [Backend] Invoice Cancellation - Reverse of Automating offseting.
 *
 * Jira: an existing customer receivable, then a negative government compensation,
 * then a real invoice whose total is less than that receivable. Accounting offsets
 * the invoice liability fully and the compensation customer liability only in part
 * against the same receivable. Cancellation must reverse both offsets so the
 * receivable current amount returns to the amount it had before the invoice.
 *
 * Swagger (dev, refreshed this session): Cursor-Project/config/swagger/dev/swagger-spec.json
 * - POST /customer-receivable CustomerReceivableRequest
 *   required: accountingPeriodId, currencyId, customerId, dueDate, initialAmount, occurrenceDate
 * - POST /government-compensations CompensationRequest
 *   required: customerId, date, documentAmount, documentCurrencyId, documentPeriod,
 *   number, podId, price, reason, recipientId, volumes
 * - PATCH /billing-run/start-generating, PATCH /billing-run/start-accounting
 * - POST /invoice-cancellation CreateInvoiceCancellationRequest (required templateId;
 *   invoices, fileId, taxEventDate)
 * - GET /customer-receivable/{id}, GET /customer-liability/{id}, GET /invoice
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3486-standard-billing-compensation-deferred-alo.spec.ts
 * - tests/cursor/pdt-3486-standard-billing-compensation-deferred-alo.fixtures.ts
 * - tests/cursor/PDT-3368-invoice-cancellation-government-compensations.spec.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 * - tests/cursor/pdt-3483-billing-run-compensation-credit-debit-note-final-liability.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { DEV_PORTAL_BASE } from './dev-volume-billing-two-compensations.fixtures';
import {
  extraPortalLinks,
  postCancelByInvoiceNumber,
  resolveOpenTaxEventDate,
  waitInvoiceCancelled,
  waitProcessFinished,
} from './pdt-3368-invoice-cancellation-government-compensations.fixtures';
import {
  PDT_3486_TEST_TIMEOUT_MS,
  assertNegativeCompensationPolarity,
  findAutomaticLiabilityOnInvoice,
  generateAndAccount,
  getCompensation,
  getInvoice,
  getLiability,
  getReceivable,
  nestedId,
  postManualReceivableOnBillingGroup,
  postPdt3486Compensation,
  resolveProductContractBillingGroupId,
  roundMoney,
  startPdt3483DebitNoteDraft,
  uniquePdt3486CompensationNumber,
  type Pdt3486Fx,
} from './pdt-3486-standard-billing-compensation-deferred-alo.fixtures';

const PDT_3578_KEY = 'PDT-3578';
const PDT_3578_TITLE = '[Backend] Invoice Cancellation - Reverse of Automating offseting';

/** Receivable exceeds the invoice by this much, so the main liability can be fully offset. */
const RECEIVABLE_ABOVE_INVOICE = 100;
/** Compensation liability exceeds that leftover, so the same receivable offsets it only in part. */
const COMPENSATION_ABOVE_LEFTOVER = 400;

test.describe(`[${PDT_3578_KEY}]: ${PDT_3578_TITLE}`, {
  tag: ['@billing', '@compensations', '@PDT-3578'],
}, () => {
  test(`[${PDT_3578_KEY}]: ${PDT_3578_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(PDT_3486_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };

    const debit = await startPdt3483DebitNoteDraft(fx);
    const invoiceTotal = roundMoney(debit.T);
    expect(invoiceTotal, 'draft invoice totalAmountIncludingVat').toBeGreaterThan(0);
    TestRunSummary.registerPayload('invoice', {
      draftInvoiceId: debit.debitInvoiceId,
      invoiceTotal,
      billingRunId: debit.volumeBillingRunId,
    });
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });

    const receivableInitial = roundMoney(invoiceTotal + RECEIVABLE_ABOVE_INVOICE);
    const compensationAmount = -roundMoney(invoiceTotal + RECEIVABLE_ABOVE_INVOICE + COMPENSATION_ABOVE_LEFTOVER);
    const compensationLiabilityInitial = roundMoney(Math.abs(compensationAmount));
    expect(receivableInitial, 'receivable must be greater than the invoice').toBeGreaterThan(invoiceTotal);
    expect(
      compensationLiabilityInitial,
      'compensation liability must exceed the receivable leftover after the invoice',
    ).toBeGreaterThan(roundMoney(receivableInitial - invoiceTotal));

    const billingGroupId = await test.step(
      'Precondition: contract billing group for the manual receivable',
      async () => resolveProductContractBillingGroupId(Request, debit.chain.contractId),
    );

    const seeded = await test.step(
      'Precondition: POST manual receivable larger than the invoice, before generate',
      async () =>
        postManualReceivableOnBillingGroup(fx, {
          customerId: debit.chain.customerId,
          billingGroupId,
          initialAmount: receivableInitial,
        }),
    );
    const receivableBeforeInvoice = roundMoney(seeded.currentAmount);
    expect(receivableBeforeInvoice, 'receivable current amount before the invoice').toBe(receivableInitial);
    TestRunSummary.registerPayload('customerReceivable', {
      id: seeded.id,
      initialAmount: receivableInitial,
      currentAmountBeforeInvoice: receivableBeforeInvoice,
    });

    const compensationNumber = uniquePdt3486CompensationNumber('PDT3578');
    const compensation = await test.step(
      'Precondition: POST negative government compensation',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: compensationAmount,
          number: compensationNumber,
          reason: 'PDT-3578-negative-compensation',
        }),
    );
    TestRunSummary.registerPayload('compensation', {
      id: compensation.id,
      number: compensationNumber,
      documentAmount: compensationAmount,
    });

    const invoice = await test.step('Action: generate and account the volume invoice to REAL', async () =>
      generateAndAccount(fx, debit.volumeBillingRunId, debit.debitInvoiceId),
    );
    const invoiceNumber = String(invoice.invoiceNumber ?? '').trim();
    expect(invoiceNumber, 'invoiceNumber for cancellation').toBeTruthy();
    expect(invoice.invoiceStatus, 'invoice REAL').toBe('REAL');

    const compView = await getCompensation(Request, compensation.id);
    assertNegativeCompensationPolarity(compView, 'PDT-3578');
    const compensationLiabilityId = nestedId(compView.liabilityForCustomer) as number;

    const mainLiability = await test.step(
      'Read: main invoice liability is fully offset by the existing receivable',
      async () => {
        const liability = await findAutomaticLiabilityOnInvoice(
          Request,
          debit.debitInvoiceId,
          invoiceTotal,
          [compensationLiabilityId],
        );
        expect(roundMoney(liability.initialAmount), 'main liability initial').toBe(invoiceTotal);
        expect(roundMoney(liability.currentAmount), 'main liability fully offset').toBe(0);
        return liability;
      },
    );

    const compensationLiability = await test.step(
      'Read: compensation liability is only partly offset by the same receivable',
      async () => {
        const liability = await getLiability(Request, compensationLiabilityId);
        expect(roundMoney(liability.initialAmount), 'compensation liability initial').toBe(
          compensationLiabilityInitial,
        );
        const current = roundMoney(liability.currentAmount);
        expect(current, 'compensation liability still open').toBeGreaterThan(0);
        expect(current, 'compensation liability partly offset').toBeLessThan(compensationLiabilityInitial);
        return liability;
      },
    );

    const leftoverOnReceivable = roundMoney(receivableBeforeInvoice - invoiceTotal);
    const compensationCurrentAfterOffset = roundMoney(
      compensationLiabilityInitial - leftoverOnReceivable,
    );
    const receivableAfterInvoice = await getReceivable(Request, seeded.id);
    expect(
      roundMoney(receivableAfterInvoice.currentAmount),
      'existing receivable fully consumed by the invoice liability and the compensation-liability leftover',
    ).toBe(0);
    expect(
      roundMoney(compensationLiability.currentAmount),
      'compensation liability reduced only by the receivable leftover',
    ).toBe(compensationCurrentAfterOffset);
    TestRunSummary.registerPayload('customerLiability', {
      mainLiabilityId: mainLiability.id,
      compensationLiabilityId,
      mainCurrent: mainLiability.currentAmount,
      compensationCurrent: compensationLiability.currentAmount,
    });

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const cancel = await test.step('Action: POST /invoice-cancellation', async () =>
      postCancelByInvoiceNumber(Request, invoiceNumber, taxEventDate),
    );
    expect(cancel.status, `POST /invoice-cancellation HTTP 201, body=${cancel.rawText}`).toBe(201);
    expect(cancel.processId, 'processId').toBeGreaterThan(0);
    Responses.invoiceCancellation.push(cancel.body);
    TestRunSummary.registerPayload('invoiceCancellation', cancel.body);

    await test.step('Poll invoice CANCELLED', async () => {
      await waitProcessFinished(Request, cancel.processId);
      await waitInvoiceCancelled(Request, debit.debitInvoiceId);
    });

    const receivableAfterCancel = await test.step(
      'Read: existing receivable is restored to the amount from before the invoice',
      async () => getReceivable(Request, seeded.id as number),
    );
    const restored = roundMoney(receivableAfterCancel.currentAmount);
    expect(
      restored,
      'receivable current amount after cancellation equals the amount before invoice generation',
    ).toBe(receivableBeforeInvoice);

    TestRunSummary.recordCheck({
      check: 'Cancellation reverses offsets of the pre-existing receivable, including the compensation liability',
      expectedResult:
        `After POST /invoice-cancellation, invoice CANCELLED and manual receivable ${seeded.id} currentAmount returns to ${receivableBeforeInvoice} (the amount before the invoice).`,
      actualResult: `invoice=${invoice.invoiceStatus} then CANCELLED; receivable before=${receivableBeforeInvoice} afterInvoice=${receivableAfterInvoice.currentAmount} afterCancel=${restored}; mainL=${mainLiability.id} compL=${compensationLiabilityId}`,
      passed: restored === receivableBeforeInvoice,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3578_KEY,
        relevantEntityKeys: [
          'customer',
          'productContract',
          'billingRun',
          'invoice',
          'compensation',
          'customerReceivable',
          'customerLiability',
        ],
        extraLinks: {
          ...extraPortalLinks({
            gcIds: [compensation.id],
            invoiceId: debit.debitInvoiceId,
            billingRunId: debit.volumeBillingRunId,
            processId: cancel.processId,
          }),
          customerReceivable: [
            `${DEV_PORTAL_BASE}/customer-receivables/preview?id=${seeded.id}`,
          ],
          customerLiability: [
            `${DEV_PORTAL_BASE}/customer-liabilities/preview?id=${mainLiability.id}`,
            `${DEV_PORTAL_BASE}/customer-liabilities/preview?id=${compensationLiabilityId}`,
          ],
        },
        snapshot: {
          invoiceId: debit.debitInvoiceId,
          receivableId: seeded.id,
          compensationId: compensation.id,
          receivableBeforeInvoice,
          restored,
        },
      });
    });
  });
});
