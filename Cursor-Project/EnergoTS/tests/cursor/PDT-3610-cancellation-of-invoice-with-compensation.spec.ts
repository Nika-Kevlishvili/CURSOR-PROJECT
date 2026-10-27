/**
 * PDT-3610 — Cancellation of invoice with compensation.
 *
 * Exact Excel case from `bug-Invoice cancellation COMP.xlsx` (invoice 109860 /
 * ЕФП-1000075455 is evidence only): +120 EUR and -100 EUR on two PODs, converted
 * to 234.70 / 195.58, main invoice 686.72. Excel actual after cancel is government
 * R 39.12. Excel expected: customer L 234.70, R 686.72, R 195.58; government L 195.58
 * and R 234.70 — not a remainder-only receivable.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3368-invoice-cancellation-government-compensations.spec.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 * - tests/cursor/pdt-3470-standard-credit-note-government-compensation-receivable.fixtures.ts
 *
 * Swagger (dev, refreshed this session):
 * POST /government-compensations — CompensationRequest documentCurrencyId
 * GET /currencies — prompt, statuses ACTIVE; CurrencyResponse abbreviation
 * POST /invoice-cancellation — CreateInvoiceCancellationRequest required templateId
 * GET /customer-liability/list — CustomerLiabilityListingRequest required page, size;
 *   searchFields CUSTOMER; columns ID; direction DESC
 * GET /customer-receivable — CustomerReceivableListingRequest required page, size;
 *   customerReceivableSearchBy CUSTOMER; sortBy ID; direction DESC
 * GET /customer-liability/{id}, GET /customer-receivable/{id} — initialAmount, currentAmount, status
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { resolveEntityIdentifier } from './gc-reg-government-compensation-regression.fixtures';
import {
  BILLING_TIMEOUT_MS,
  UNIQUE_RESULT_FRAGMENT,
  accountGcsOnRealDebit,
  assertAmountZero,
  assertGcUninvoicedEmptyFinancials,
  assertNegativeApplyAttachments,
  assertPositiveApplyAttachments,
  extraPortalLinks,
  extractPartyCustomerId,
  getCompensation,
  getCustomerLiability,
  getCustomerReceivable,
  getInvoice,
  getProcessBody,
  postCancelByInvoiceNumber,
  processErrorText,
  resolveInvoiceMainCustomerLiability,
  resolveOpenTaxEventDate,
  runMultiPodForVolumesPrechain,
  toAmountNumber,
  waitInvoiceCancelled,
  type Pdt3368Fx,
} from './pdt-3368-invoice-cancellation-government-compensations.fixtures';
import {
  PDT_3610_BUG_REMAINDER_BGN,
  PDT_3610_INVOICE_INCLUDING_VAT,
  PDT_3610_NEGATIVE_BGN,
  PDT_3610_NEGATIVE_EUR,
  PDT_3610_POSITIVE_BGN,
  PDT_3610_POSITIVE_EUR,
  volumePriceExpressionForInvoiceTotal,
} from './pdt-3573-leftover-offset-cancellation.fixtures';

const PDT_3610_KEY = 'PDT-3610';
const PDT_3610_TITLE = 'Cancellation of invoice with compensation';
const POSITIVE_EUR = PDT_3610_POSITIVE_EUR;
const NEGATIVE_EUR = PDT_3610_NEGATIVE_EUR;

function requireLink(id: number | null, label: string): number {
  expect(id, label).toBeTruthy();
  return Number(id);
}

function money(value: number): number {
  return Math.round(value * 100) / 100;
}

async function resolveEuroCurrencyId(Request: Pdt3368Fx['Request']): Promise<number> {
  const res = await Request.get('currencies', {
    params: { page: 0, size: 50, statuses: 'ACTIVE', prompt: 'EUR' },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<Record<string, unknown>> };
  const hit = (body.content ?? []).find((row) => {
    const hay = [row.name, row.abbreviation, row.printName, row.fullName]
      .map((part) => String(part ?? '').toUpperCase())
      .join(' ');
    return hay.includes('EUR') || hay.includes('ЕВРО');
  });
  const id = Number(hit?.id);
  expect(Number.isFinite(id) && id > 0, 'active EUR currency').toBe(true);
  return id;
}

function sameAmount(actual: unknown, expected: number): boolean {
  return Math.abs(toAmountNumber(actual) - expected) < 0.02;
}

async function listLiabilityIds(
  Request: Pdt3368Fx['Request'],
  customerIdentifier: string,
): Promise<number[]> {
  const res = await Request.get('customer-liability/list', {
    params: {
      page: 0,
      size: 50,
      prompt: customerIdentifier,
      searchFields: 'CUSTOMER',
      columns: 'ID',
      direction: 'DESC',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? [])
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

async function listReceivableIds(
  Request: Pdt3368Fx['Request'],
  customerIdentifier: string,
): Promise<number[]> {
  const res = await Request.get('customer-receivable', {
    params: {
      page: 0,
      size: 50,
      prompt: customerIdentifier,
      customerReceivableSearchBy: 'CUSTOMER',
      sortBy: 'ID',
      direction: 'DESC',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? [])
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

function newIds(before: number[], after: number[]): number[] {
  const known = new Set(before);
  return after.filter((id) => !known.has(id));
}

test.describe(`[${PDT_3610_KEY}]: ${PDT_3610_TITLE}`, { tag: ['@billing', '@compensations', '@PDT-3610'] }, () => {
  test(`[${PDT_3610_KEY}]: ${PDT_3610_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const euroCurrencyId = await test.step('Precondition: resolve EUR currency', async () =>
      resolveEuroCurrencyId(Request),
    );

    const pre = await test.step(
      'Precondition: billed customer + recipient + two PODs, price scaled to invoice 686.72',
      async () =>
        runMultiPodForVolumesPrechain(fx, {
          priceExpression: volumePriceExpressionForInvoiceTotal(PDT_3610_INVOICE_INCLUDING_VAT),
        }),
    );

    const billed = await test.step('Precondition: 120 EUR and -100 EUR on separate PODs through accounting', async () =>
      accountGcsOnRealDebit(fx, pre, [
        {
          documentAmount: POSITIVE_EUR,
          podId: pre.podAId,
          reason: 'PDT-3610-positive-eur',
          documentCurrencyId: euroCurrencyId,
        },
        {
          documentAmount: NEGATIVE_EUR,
          podId: pre.podBId,
          reason: 'PDT-3610-negative-eur',
          documentCurrencyId: euroCurrencyId,
        },
      ]),
    );
    const positive = billed.gcs[0]!;
    const negative = billed.gcs[1]!;
    TestRunSummary.registerPayload('compensation', {
      positive: positive.payload,
      negative: negative.payload,
    });
    const invoice = await getInvoice(Request, billed.invoiceId);
    expect(money(toAmountNumber(invoice.totalAmountIncludingVat)), 'main invoice is Excel 686.72').toBe(
      PDT_3610_INVOICE_INCLUDING_VAT,
    );

    const before = await test.step('Precondition: government netted to the remainder, customer receivable offset on the main debt', async () => {
      expect(positive.view.compensationStatus, 'positive GC INVOICED').toBe('INVOICED');
      expect(negative.view.compensationStatus, 'negative GC INVOICED').toBe('INVOICED');
      expect(positive.view.invoice?.id, 'positive on billed invoice').toBe(billed.invoiceId);
      expect(negative.view.invoice?.id, 'negative on billed invoice').toBe(billed.invoiceId);
      assertPositiveApplyAttachments(positive.view, billed.invoiceId, 'PDT-3610 positive');
      assertNegativeApplyAttachments(negative.view, billed.invoiceId, 'PDT-3610 negative');

      const governmentLiabilityId = requireLink(
        positive.links.liabilityForRecipientId,
        'positive recipient liability',
      );
      const governmentReceivableId = requireLink(
        negative.links.receivableForRecipientId,
        'negative recipient receivable',
      );
      const customerReceivableId = requireLink(
        positive.links.receivableForCustomerId,
        'positive customer receivable',
      );
      const customerLiabilityId = requireLink(
        negative.links.liabilityForCustomerId,
        'negative customer liability',
      );

      expect(Number(positive.payload.documentCurrencyId), 'positive document currency is EUR').toBe(euroCurrencyId);
      expect(Number(negative.payload.documentCurrencyId), 'negative document currency is EUR').toBe(euroCurrencyId);

      const governmentLiability = await getCustomerLiability(Request, governmentLiabilityId);
      const governmentReceivable = await getCustomerReceivable(Request, governmentReceivableId);
      const customerReceivable = await getCustomerReceivable(Request, customerReceivableId);
      const customerLiability = await getCustomerLiability(Request, customerLiabilityId);
      const main = await resolveInvoiceMainCustomerLiability(Request, billed.invoiceId, pre.customerId, [
        governmentLiabilityId,
        customerLiabilityId,
      ]);

      expect(extractPartyCustomerId(governmentLiability), 'government liability party').toBe(pre.recipientCustomerId);
      expect(extractPartyCustomerId(governmentReceivable), 'government receivable party').toBe(pre.recipientCustomerId);
      const positiveBgn = money(toAmountNumber(governmentLiability.initialAmount));
      const negativeBgn = money(toAmountNumber(governmentReceivable.initialAmount));
      const remainderBgn = money(positiveBgn - negativeBgn);
      expect(positiveBgn, 'positive EUR converted to Excel 234.70').toBe(PDT_3610_POSITIVE_BGN);
      expect(negativeBgn, 'negative EUR converted to Excel 195.58').toBe(PDT_3610_NEGATIVE_BGN);
      expect(remainderBgn, 'Excel remainder 39.12').toBe(PDT_3610_BUG_REMAINDER_BGN);
      expect(toAmountNumber(governmentLiability.currentAmount), 'government liability already netted to remainder').toBe(
        remainderBgn,
      );
      expect(toAmountNumber(governmentReceivable.currentAmount), 'government receivable fully offset').toBe(0);
      expect(toAmountNumber(customerReceivable.initialAmount), 'customer receivable equals converted positive').toBe(
        positiveBgn,
      );
      expect(toAmountNumber(customerReceivable.currentAmount), 'customer receivable offset onto main debt').toBe(0);
      expect(toAmountNumber(customerLiability.initialAmount), 'customer liability equals converted negative').toBe(
        negativeBgn,
      );
      expect(toAmountNumber(customerLiability.currentAmount), 'customer compensation liability still full').toBe(
        negativeBgn,
      );
      expect(money(toAmountNumber(main.initialAmount)), 'main invoice liability is Excel 686.72').toBe(
        PDT_3610_INVOICE_INCLUDING_VAT,
      );
      expect(toAmountNumber(main.currentAmount), 'main debt reduced by the converted positive compensation').toBe(
        money(toAmountNumber(main.initialAmount) - positiveBgn),
      );

      const customerIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.customerId);
      const recipientIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.recipientCustomerId);
      const [recipientLiabilityIds, recipientReceivableIds, customerLiabilityIds, customerReceivableIds] =
        await Promise.all([
          listLiabilityIds(Request, recipientIdentifier),
          listReceivableIds(Request, recipientIdentifier),
          listLiabilityIds(Request, customerIdentifier),
          listReceivableIds(Request, customerIdentifier),
        ]);

      TestRunSummary.registerPayload('invoice', {
        invoiceId: billed.invoiceId,
        invoiceNumber: billed.invoiceNumber,
        mainLiabilityId: main.id,
        governmentLiabilityId,
        governmentReceivableId,
      });

      return {
        governmentLiabilityId,
        governmentReceivableId,
        customerReceivableId,
        customerLiabilityId,
        mainInitialAmount: toAmountNumber(main.initialAmount),
        mainLiabilityId: main.id,
        positiveBgn,
        negativeBgn,
        remainderBgn,
        recipientLiabilityIds,
        recipientReceivableIds,
        customerLiabilityIds,
        customerReceivableIds,
        customerIdentifier,
        recipientIdentifier,
      };
    });

    const taxEventDate = await test.step('Precondition: OPEN taxEventDate', async () =>
      resolveOpenTaxEventDate(Request),
    );

    const cancel = await test.step('POST /invoice-cancellation by invoice number', async () => {
      const posted = await postCancelByInvoiceNumber(Request, billed.invoiceNumber, taxEventDate);
      expect(posted.status, `POST /invoice-cancellation HTTP 201, body=${posted.rawText}`).toBe(201);
      expect(posted.processId, 'processId').toBeGreaterThan(0);
      Responses.invoiceCancellation.push(posted.body);
      TestRunSummary.registerPayload('invoiceCancellation', posted.body);
      return posted;
    });

    await test.step('Poll invoice CANCELLED', async () => {
      await waitInvoiceCancelled(Request, billed.invoiceId);
    });

    await test.step('Customer and government settle Excel full 234.70 / 195.58, not remainder 39.12', async () => {
      const processBody = await getProcessBody(Request, cancel.processId);
      expect(processErrorText(processBody), 'no Hibernate unique-result error').not.toContain(UNIQUE_RESULT_FRAGMENT);

      assertGcUninvoicedEmptyFinancials(await getCompensation(Request, positive.id), 'PDT-3610 positive');
      assertGcUninvoicedEmptyFinancials(await getCompensation(Request, negative.id), 'PDT-3610 negative');

      await assertAmountZero(Request, 'liability', before.governmentLiabilityId, 'government liability');
      await assertAmountZero(Request, 'receivable', before.governmentReceivableId, 'government receivable');
      await assertAmountZero(Request, 'receivable', before.customerReceivableId, 'customer compensation receivable');
      await assertAmountZero(Request, 'liability', before.customerLiabilityId, 'customer compensation liability');
      await assertAmountZero(Request, 'liability', before.mainLiabilityId, 'main invoice liability');

      const [recipientLiabilityIds, recipientReceivableIds, customerLiabilityIds, customerReceivableIds] =
        await Promise.all([
          listLiabilityIds(Request, before.recipientIdentifier),
          listReceivableIds(Request, before.recipientIdentifier),
          listLiabilityIds(Request, before.customerIdentifier),
          listReceivableIds(Request, before.customerIdentifier),
        ]);

      const newRecipientLiabilities = await Promise.all(
        newIds(before.recipientLiabilityIds, recipientLiabilityIds).map((id) => getCustomerLiability(Request, id)),
      );
      const newRecipientReceivables = await Promise.all(
        newIds(before.recipientReceivableIds, recipientReceivableIds).map((id) => getCustomerReceivable(Request, id)),
      );
      const newCustomerLiabilities = await Promise.all(
        newIds(before.customerLiabilityIds, customerLiabilityIds).map((id) => getCustomerLiability(Request, id)),
      );
      const newCustomerReceivables = await Promise.all(
        newIds(before.customerReceivableIds, customerReceivableIds).map((id) => getCustomerReceivable(Request, id)),
      );

      const remainderReceivables = newRecipientReceivables.filter((row) =>
        sameAmount(row.initialAmount, PDT_3610_BUG_REMAINDER_BGN),
      );
      const governmentPositive = newRecipientReceivables.filter((row) =>
        sameAmount(row.initialAmount, PDT_3610_POSITIVE_BGN),
      );
      const governmentNegative = newRecipientLiabilities.filter((row) =>
        sameAmount(row.initialAmount, PDT_3610_NEGATIVE_BGN),
      );
      const customerCounterLiability = newCustomerLiabilities.filter((row) =>
        sameAmount(row.initialAmount, PDT_3610_POSITIVE_BGN),
      );
      const customerCounterReceivable = newCustomerReceivables.filter((row) =>
        sameAmount(row.initialAmount, PDT_3610_NEGATIVE_BGN),
      );
      const mainCounter = newCustomerReceivables.filter((row) =>
        sameAmount(row.initialAmount, PDT_3610_INVOICE_INCLUDING_VAT),
      );
      const govRText = newRecipientReceivables
        .map((row) => `${row.id}:${toAmountNumber(row.initialAmount)}`)
        .join(', ');
      const govLText = newRecipientLiabilities
        .map((row) => `${row.id}:${toAmountNumber(row.initialAmount)}`)
        .join(', ');

      const passed =
        remainderReceivables.length === 0 &&
        governmentPositive.length === 1 &&
        governmentNegative.length === 1 &&
        customerCounterLiability.length === 1 &&
        customerCounterReceivable.length === 1 &&
        mainCounter.length > 0;
      TestRunSummary.recordCheck({
        check: 'Mixed-sign cancellation settles Excel full converted amounts, not remainder 39.12',
        expectedResult:
          'Customer L 234.70, R 686.72, R 195.58. Government L 195.58 and R 234.70. No government receivable of 39.12.',
        actualResult: `invoice=${billed.invoiceId} govL=[${govLText}] govR=[${govRText}] process=${cancel.processId}`,
        passed,
      });

      expect(
        remainderReceivables,
        `Excel bug amount ${PDT_3610_BUG_REMAINDER_BGN} must not be the government settlement; seen=[${govRText}]`,
      ).toHaveLength(0);
      expect(governmentPositive, `Excel government R 234.70; seen=[${govRText}]`).toHaveLength(1);
      expect(toAmountNumber(governmentPositive[0]!.currentAmount), 'government 234.70 closed').toBe(0);
      expect(governmentNegative, `Excel government L 195.58; seen=[${govLText}]`).toHaveLength(1);
      expect(toAmountNumber(governmentNegative[0]!.currentAmount), 'government 195.58 closed').toBe(0);
      expect(customerCounterLiability, 'customer liability for the full converted positive compensation').toHaveLength(
        1,
      );
      expect(toAmountNumber(customerCounterLiability[0]!.currentAmount), 'customer counter liability settled').toBe(0);
      expect(customerCounterReceivable, 'customer receivable for the full negative compensation').toHaveLength(1);
      expect(toAmountNumber(customerCounterReceivable[0]!.currentAmount), 'customer counter receivable settled').toBe(0);
      expect(mainCounter.length, 'customer receivable for the full main debt 686.72').toBeGreaterThan(0);
      for (const row of mainCounter) {
        expect(toAmountNumber(row.currentAmount), `main counter receivable ${row.id} settled`).toBe(0);
      }
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3610_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [positive.id, negative.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: {
          invoiceId: billed.invoiceId,
          positiveGcId: positive.id,
          negativeGcId: negative.id,
          remainder: before.remainderBgn,
          processId: cancel.processId,
        },
      });
    });
  });
});
