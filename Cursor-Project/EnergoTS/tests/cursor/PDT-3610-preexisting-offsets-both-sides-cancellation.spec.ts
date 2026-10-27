/**
 * PDT-3610 — Cancellation of invoice with compensation
 *
 * Exact Excel case from Jira attachment `bug-Invoice cancellation COMP.xlsx`
 * (invoice 109860 / ЕФП-1000075455 is evidence only; this spec creates new data):
 * - two EUR compensations on two PODs: +120 and -100
 * - converted BGN: +234.70 and -195.58 (remainder 39.12)
 * - main invoice L = 686.72
 * Documents before cancel: customer L 686.72 + 195.58, R 234.70;
 * government L 234.70, R 195.58.
 * Excel actual after cancel: government R 39.12.
 * Excel expected after cancel: customer L 234.70, R 686.72, R 195.58;
 * government L 195.58 and R 234.70 — not a remainder-only 39.12 receivable.
 *
 * Cancellation is temporarily omitted so the invoice stays REAL for a manual cancel.
 *
 * Distinct from PDT-3610-government-receivable-offset-cancellation.spec.ts
 * (leftover 30 / 80,000,000). Sibling
 * PDT-3610-cancellation-of-invoice-with-compensation.spec.ts uses the same
 * Excel expected amounts.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3610-cancellation-of-invoice-with-compensation.spec.ts
 * - tests/cursor/PDT-3612-preexisting-liability-fully-offset-cancellation.spec.ts
 * - tests/cursor/pdt-3573-leftover-offset-cancellation.fixtures.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 *
 * Swagger (dev, update-swagger-specs.ps1 this session):
 * POST /government-compensations CompensationRequest required documentCurrencyId, documentAmount
 * GET /currencies — prompt, statuses ACTIVE
 * POST /invoice-cancellation CreateInvoiceCancellationRequest required templateId
 * GET /customer-liability/list, GET /customer-receivable
 * GET /customer-liability/{id}, GET /customer-receivable/{id}
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { resolveEntityIdentifier } from './gc-reg-government-compensation-regression.fixtures';
import {
  BILLING_TIMEOUT_MS,
  accountGcsOnRealDebit,
  assertNegativeApplyAttachments,
  assertPositiveApplyAttachments,
  extraPortalLinks,
  extractPartyCustomerId,
  getCustomerLiability,
  getCustomerReceivable,
  getInvoice,
  resolveInvoiceMainCustomerLiability,
  runMultiPodForVolumesPrechain,
  toAmountNumber,
  type Pdt3368Fx,
} from './pdt-3368-invoice-cancellation-government-compensations.fixtures';
import {
  PDT_3610_BUG_REMAINDER_BGN,
  PDT_3610_INVOICE_INCLUDING_VAT,
  PDT_3610_NEGATIVE_BGN,
  PDT_3610_NEGATIVE_EUR,
  PDT_3610_POSITIVE_BGN,
  PDT_3610_POSITIVE_EUR,
  listLiabilityIds,
  listReceivableIds,
  money,
  resolveEuroCurrencyId,
  volumePriceExpressionForInvoiceTotal,
} from './pdt-3573-leftover-offset-cancellation.fixtures';

const PDT_3610_KEY = 'PDT-3610';
const PDT_3610_TITLE = 'Cancellation of invoice with compensation';

function requireLink(id: number | null, label: string): number {
  expect(id, label).toBeTruthy();
  return Number(id);
}

test.describe(`[${PDT_3610_KEY}]: ${PDT_3610_TITLE}`, {
  tag: ['@billing', '@compensations', '@PDT-3610', '@dev'],
}, () => {
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

    const billed = await test.step(
      'Precondition: 120 EUR and -100 EUR on separate PODs through accounting',
      async () =>
        accountGcsOnRealDebit(fx, pre, [
          {
            documentAmount: PDT_3610_POSITIVE_EUR,
            podId: pre.podAId,
            reason: 'PDT-3610-excel-positive-eur',
            documentCurrencyId: euroCurrencyId,
          },
          {
            documentAmount: PDT_3610_NEGATIVE_EUR,
            podId: pre.podBId,
            reason: 'PDT-3610-excel-negative-eur',
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
    const invoiceTotal = money(toAmountNumber(invoice.totalAmountIncludingVat));
    expect(invoiceTotal, 'main invoice is Excel 686.72').toBe(PDT_3610_INVOICE_INCLUDING_VAT);

    const before = await test.step(
      'Precondition: converted 234.70 / 195.58; government netted to 39.12; customer receivable offset on main debt',
      async () => {
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

        expect(Number(positive.payload.documentCurrencyId), 'positive document currency is EUR').toBe(
          euroCurrencyId,
        );
        expect(Number(negative.payload.documentCurrencyId), 'negative document currency is EUR').toBe(
          euroCurrencyId,
        );

        const governmentLiability = await getCustomerLiability(Request, governmentLiabilityId);
        const governmentReceivable = await getCustomerReceivable(Request, governmentReceivableId);
        const customerReceivable = await getCustomerReceivable(Request, customerReceivableId);
        const customerLiability = await getCustomerLiability(Request, customerLiabilityId);
        const main = await resolveInvoiceMainCustomerLiability(Request, billed.invoiceId, pre.customerId, [
          governmentLiabilityId,
          customerLiabilityId,
        ]);

        expect(extractPartyCustomerId(governmentLiability), 'government liability party').toBe(
          pre.recipientCustomerId,
        );
        expect(extractPartyCustomerId(governmentReceivable), 'government receivable party').toBe(
          pre.recipientCustomerId,
        );

        const positiveBgn = money(toAmountNumber(governmentLiability.initialAmount));
        const negativeBgn = money(toAmountNumber(governmentReceivable.initialAmount));
        expect(positiveBgn, 'positive EUR converted to Excel 234.70').toBe(PDT_3610_POSITIVE_BGN);
        expect(negativeBgn, 'negative EUR converted to Excel 195.58').toBe(PDT_3610_NEGATIVE_BGN);
        expect(money(positiveBgn - negativeBgn), 'Excel remainder 39.12').toBe(PDT_3610_BUG_REMAINDER_BGN);

        expect(
          money(toAmountNumber(governmentLiability.currentAmount)),
          'government liability already netted to remainder 39.12',
        ).toBe(PDT_3610_BUG_REMAINDER_BGN);
        expect(money(toAmountNumber(governmentReceivable.currentAmount)), 'government receivable fully offset').toBe(
          0,
        );
        expect(money(toAmountNumber(customerReceivable.initialAmount)), 'customer R face is 234.70').toBe(
          PDT_3610_POSITIVE_BGN,
        );
        expect(money(toAmountNumber(customerReceivable.currentAmount)), 'customer receivable offset onto main debt').toBe(
          0,
        );
        expect(money(toAmountNumber(customerLiability.initialAmount)), 'customer L face is 195.58').toBe(
          PDT_3610_NEGATIVE_BGN,
        );
        expect(money(toAmountNumber(customerLiability.currentAmount)), 'customer compensation liability still full').toBe(
          PDT_3610_NEGATIVE_BGN,
        );
        expect(money(toAmountNumber(main.initialAmount)), 'main invoice liability is 686.72').toBe(
          PDT_3610_INVOICE_INCLUDING_VAT,
        );
        expect(
          money(toAmountNumber(main.currentAmount)),
          'main debt reduced by converted positive 234.70',
        ).toBe(money(PDT_3610_INVOICE_INCLUDING_VAT - PDT_3610_POSITIVE_BGN));

        const customerIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.customerId);
        const recipientIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.recipientCustomerId);

        TestRunSummary.registerPayload('invoice', {
          invoiceId: billed.invoiceId,
          invoiceNumber: billed.invoiceNumber,
          invoiceTotal,
          mainLiabilityId: main.id,
          governmentLiabilityId,
          governmentReceivableId,
        });

        return {
          governmentLiabilityId,
          governmentReceivableId,
          customerReceivableId,
          customerLiabilityId,
          mainLiabilityId: main.id,
          customerIdentifier,
          recipientIdentifier,
          knownCustomerLiabilities: await listLiabilityIds(Request, customerIdentifier),
          knownCustomerReceivables: await listReceivableIds(Request, customerIdentifier),
          knownRecipientLiabilities: await listLiabilityIds(Request, recipientIdentifier),
          knownRecipientReceivables: await listReceivableIds(Request, recipientIdentifier),
        };
      },
    );

    await test.step('Leave the invoice REAL for a manual cancellation', async () => {
      const stillReal = await getInvoice(Request, billed.invoiceId);
      expect(String(stillReal.invoiceStatus), 'invoice stays REAL').toBe('REAL');
      TestRunSummary.recordCheck({
        check: 'Invoice left REAL so cancellation can be done manually',
        expectedResult:
          'Invoice 686.72 stays REAL. Positive 234.70 and negative 195.58 each have their own liability and receivable. Government liability current is the 39.12 remainder.',
        actualResult: `invoice=${billed.invoiceId} number=${billed.invoiceNumber} customerR=${before.customerReceivableId} customerL=${before.customerLiabilityId} govL=${before.governmentLiabilityId} govR=${before.governmentReceivableId} mainL=${before.mainLiabilityId}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3610_KEY,
        relevantEntityKeys: [
          'customer',
          'pod',
          'productContract',
          'billingRun',
          'invoice',
          'compensation',
          'customerLiability',
          'customerReceivable',
        ],
        extraLinks: extraPortalLinks({
          gcIds: [positive.id, negative.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
        }),
        snapshot: {
          invoiceId: billed.invoiceId,
          invoiceNumber: billed.invoiceNumber,
          invoiceTotal,
          positiveGcId: positive.id,
          negativeGcId: negative.id,
          customerReceivableId: before.customerReceivableId,
          customerLiabilityId: before.customerLiabilityId,
          governmentLiabilityId: before.governmentLiabilityId,
          governmentReceivableId: before.governmentReceivableId,
          mainLiabilityId: before.mainLiabilityId,
          cancellation: 'manual',
        },
      });
    });
  });
});
