/**
 * PDT-3612 — Cancellation of invoice with compensations - case2
 *
 * Exact Excel `case2` amounts from attachment
 * `bug-Invoice cancellation COMP 1 (1).xlsx` (invoice 109863 / ЕФП-1000075457
 * is evidence only; this spec creates new data):
 * - compensation R1 / gov L4 = 1200 (Excel lists one compensation; Dev also has
 *   unused GC -100 on POD 355053 which case2 does not include)
 * - manual customer L1 = 200 (smaller than leftover 513.28, so ALO uses it in full)
 * - main invoice L2 = 686.72
 * Actual before the PDT-3573 fix: new customer L = 1000 (1200 − 200), L1 stayed 0.
 * Expected: L1 restored to 200, new L3 = 1200, new government R3 = 1200, R2 686.72.
 *
 * Distinct from PDT-3612-positive-compensation-partial-offset-cancellation.spec.ts
 * (Valeri 110078: seed larger than leftover).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3612-positive-compensation-partial-offset-cancellation.spec.ts
 * - tests/cursor/pdt-3573-leftover-offset-cancellation.fixtures.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 *
 * Swagger (dev, update-swagger-specs.ps1 this session):
 * POST /government-compensations CompensationRequest required documentCurrencyId, documentAmount
 * POST /customer-liability CustomerLiabilityRequest
 * POST /invoice-cancellation CreateInvoiceCancellationRequest required templateId
 * GET /customer-liability/{id}, GET /customer-receivable/{id}
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  BILLING_TIMEOUT_MS,
  assertGcUninvoicedEmptyFinancials,
  completeForVolumesToAccounted,
  createGcExpectingCreated,
  extraPortalLinks,
  getCompensation,
  getCustomerLiability,
  getCustomerReceivable,
  getInvoice,
  postCancelByInvoiceNumber,
  resolveOpenTaxEventDate,
  runMultiPodForVolumesPrechain,
  toAmountNumber,
  waitInvoiceCancelled,
  type Pdt3368Fx,
} from './pdt-3368-invoice-cancellation-government-compensations.fixtures';
import { resolveProductContractBillingGroupId } from './pdt-3486-standard-billing-compensation-deferred-alo.fixtures';
import { resolveEntityIdentifier } from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import {
  PDT_3612_BUG_SETTLEMENT_BGN,
  PDT_3612_COMPENSATION_BGN,
  PDT_3612_INVOICE_INCLUDING_VAT,
  PDT_3612_SEED_LIABILITY_BGN,
  listLiabilityIds,
  listNewDocuments,
  listReceivableIds,
  money,
  postManualLiability,
  resolveMainCurrencyId,
  volumePriceExpressionForInvoiceTotal,
} from './pdt-3573-leftover-offset-cancellation.fixtures';

const PDT_3612_KEY = 'PDT-3612';
const PDT_3612_TITLE = 'Cancellation of invoice with compensations - case2';

test.describe(`[${PDT_3612_KEY}]: ${PDT_3612_TITLE}`, {
  tag: ['@billing', '@compensations', '@PDT-3612', '@dev'],
}, () => {
  test(`[${PDT_3612_KEY}]: ${PDT_3612_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const mainCurrencyId = await test.step('Precondition: resolve the active main currency', async () =>
      resolveMainCurrencyId(Request),
    );

    const pre = await test.step(
      'Precondition: customer, government recipient, POD, contract, price scaled to invoice 686.72',
      async () =>
        runMultiPodForVolumesPrechain(fx, {
          priceExpression: volumePriceExpressionForInvoiceTotal(PDT_3612_INVOICE_INCLUDING_VAT),
        }),
    );

    const billingGroupId = await test.step('Precondition: contract billing group', async () =>
      resolveProductContractBillingGroupId(Request, pre.contractId),
    );

    const seedLiabilityId = await test.step(
      'Precondition: manual customer liability L1 of 200 (Excel case2)',
      async () =>
        postManualLiability(fx, {
          customerId: pre.customerId,
          billingGroupId,
          initialAmount: PDT_3612_SEED_LIABILITY_BGN,
          currencyId: mainCurrencyId,
        }),
    );

    const created = await test.step('Precondition: one positive compensation of 1,200 in the main currency', async () =>
      createGcExpectingCreated(fx, {
        customerId: pre.customerId,
        podId: pre.podAId,
        recipientId: pre.recipientCustomerId,
        documentPeriod: pre.documentPeriod,
        documentAmount: PDT_3612_COMPENSATION_BGN,
        volumes: 1,
        price: PDT_3612_COMPENSATION_BGN,
        reason: 'PDT-3612-case2-1200',
        documentCurrencyId: mainCurrencyId,
      }),
    );
    TestRunSummary.registerPayload('compensation', created.payload);

    const billed = await test.step('Precondition: account the volume invoice with the compensation', async () =>
      completeForVolumesToAccounted(fx),
    );
    const invoice = await getInvoice(Request, billed.invoiceId);
    expect(String(invoice.invoiceStatus), 'invoice REAL').toBe('REAL');
    const invoiceTotal = money(toAmountNumber(invoice.totalAmountIncludingVat));
    expect(invoiceTotal, 'main invoice L2 is Excel 686.72').toBe(PDT_3612_INVOICE_INCLUDING_VAT);
    const leftover = money(PDT_3612_COMPENSATION_BGN - invoiceTotal);
    expect(leftover, 'leftover 513.28 covers L1 200 in full').toBe(
      money(PDT_3612_COMPENSATION_BGN - PDT_3612_INVOICE_INCLUDING_VAT),
    );
    expect(leftover, 'leftover is greater than L1').toBeGreaterThan(PDT_3612_SEED_LIABILITY_BGN);

    const customerIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.customerId);
    const recipientIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.recipientCustomerId);

    const before = await test.step(
      'Compensation leftover closes the seed liability; receivable still has a remainder',
      async () => {
        const comp = await getCompensation(Request, created.id);
        expect(comp.compensationStatus, 'compensation INVOICED').toBe('INVOICED');
        expect(comp.invoice?.id, 'compensation on the billed invoice').toBe(billed.invoiceId);

        const customerReceivableId = Number(comp.receivableForCustomer?.id);
        const governmentLiabilityId = Number(comp.liabilityForRecipient?.id);
        expect(customerReceivableId, 'customer compensation receivable').toBeGreaterThan(0);
        expect(governmentLiabilityId, 'government compensation liability').toBeGreaterThan(0);

        const customerReceivable = await getCustomerReceivable(Request, customerReceivableId);
        const governmentLiability = await getCustomerLiability(Request, governmentLiabilityId);
        const seedLiability = await getCustomerLiability(Request, seedLiabilityId);
        const seedLiabilityCurrent = money(toAmountNumber(seedLiability.currentAmount));
        const receivableCurrent = money(toAmountNumber(customerReceivable.currentAmount));
        const expectedReceivableRemainder = money(leftover - PDT_3612_SEED_LIABILITY_BGN);

        expect(money(toAmountNumber(customerReceivable.initialAmount)), 'customer R1 face is 1200').toBe(
          PDT_3612_COMPENSATION_BGN,
        );
        expect(seedLiabilityCurrent, 'L1 200 fully used by leftover ALO').toBe(0);
        expect(receivableCurrent, 'R1 remainder after L1 200').toBe(expectedReceivableRemainder);
        expect(money(toAmountNumber(governmentLiability.initialAmount)), 'government L4 face is 1200').toBe(
          PDT_3612_COMPENSATION_BGN,
        );
        expect(money(toAmountNumber(governmentLiability.currentAmount)), 'government L4 stays open at 1200').toBe(
          PDT_3612_COMPENSATION_BGN,
        );

        TestRunSummary.registerPayload('invoice', {
          invoiceId: billed.invoiceId,
          invoiceNumber: invoice.invoiceNumber,
          invoiceTotal,
          leftover,
          customerReceivableId,
          governmentLiabilityId,
          seedLiabilityId,
          seedLiabilityAfterInvoice: seedLiabilityCurrent,
          receivableCurrent,
        });

        return {
          customerReceivableId,
          governmentLiabilityId,
          seedLiabilityCurrent,
          receivableCurrent,
          knownCustomerLiabilities: await listLiabilityIds(Request, customerIdentifier),
          knownCustomerReceivables: await listReceivableIds(Request, customerIdentifier),
          knownRecipientReceivables: await listReceivableIds(Request, recipientIdentifier),
        };
      },
    );

    const taxEventDate = await test.step('Precondition: OPEN taxEventDate', async () =>
      resolveOpenTaxEventDate(Request),
    );

    const cancel = await test.step('POST /invoice-cancellation by invoice number', async () => {
      const posted = await postCancelByInvoiceNumber(Request, String(invoice.invoiceNumber), taxEventDate);
      expect(posted.status, `POST /invoice-cancellation HTTP 201, body=${posted.rawText}`).toBe(201);
      expect(posted.processId, 'processId').toBeGreaterThan(0);
      Responses.invoiceCancellation.push(posted.body);
      TestRunSummary.registerPayload('invoiceCancellation', posted.body);
      return posted;
    });

    const after = await test.step(
      'Poll invoice CANCELLED: seed restored, compensation settled for its full amount',
      async () => {
        await waitInvoiceCancelled(Request, billed.invoiceId);
        assertGcUninvoicedEmptyFinancials(await getCompensation(Request, created.id), 'PDT-3612 fully-offset seed');

        const customerReceivable = await getCustomerReceivable(Request, before.customerReceivableId);
        const governmentLiability = await getCustomerLiability(Request, before.governmentLiabilityId);
        const seedLiability = await getCustomerLiability(Request, seedLiabilityId);
        const seedLiabilityCurrent = money(toAmountNumber(seedLiability.currentAmount));

        const newCustomerLiabilities = await listNewDocuments(
          Request,
          'liability',
          customerIdentifier,
          before.knownCustomerLiabilities,
        );
        const newCustomerReceivables = await listNewDocuments(
          Request,
          'receivable',
          customerIdentifier,
          before.knownCustomerReceivables,
        );
        const newRecipientReceivables = await listNewDocuments(
          Request,
          'receivable',
          recipientIdentifier,
          before.knownRecipientReceivables,
        );
        const settlementLiability = newCustomerLiabilities.find((row) => row.initial === PDT_3612_COMPENSATION_BGN);
        const settlementReceivable = newRecipientReceivables.find((row) => row.initial === PDT_3612_COMPENSATION_BGN);
        const bugSettlement = newCustomerLiabilities.find((row) => row.initial === PDT_3612_BUG_SETTLEMENT_BGN);
        const settlementLiabilityText = newCustomerLiabilities.map((r) => `${r.id}:${r.initial}/${r.current}`).join(', ');
        const settlementReceivableText = newRecipientReceivables.map((r) => `${r.id}:${r.initial}/${r.current}`).join(', ');

        const passed =
          seedLiabilityCurrent === PDT_3612_SEED_LIABILITY_BGN &&
          settlementLiability != null &&
          settlementLiability.current === 0 &&
          settlementReceivable != null &&
          settlementReceivable.current === 0 &&
          bugSettlement == null;
        TestRunSummary.recordCheck({
          check: 'Cancellation restores L1 200 and settles compensation at 1200, not 1000',
          expectedResult: `L1 ${seedLiabilityId} returns to ${PDT_3612_SEED_LIABILITY_BGN}. New customer L3 ${PDT_3612_COMPENSATION_BGN} (not ${PDT_3612_BUG_SETTLEMENT_BGN}). New government R3 ${PDT_3612_COMPENSATION_BGN}.`,
          actualResult: `invoice=${billed.invoiceId} total=${invoiceTotal} leftover=${leftover} L1 before=${before.seedLiabilityCurrent} after=${seedLiabilityCurrent}; new customer liabilities=[${settlementLiabilityText}]; new government receivables=[${settlementReceivableText}]`,
          passed,
        });

        expect(money(toAmountNumber(customerReceivable.currentAmount)), 'R1 closed').toBe(0);
        expect(money(toAmountNumber(governmentLiability.currentAmount)), 'L4 closed').toBe(0);
        expect(seedLiabilityCurrent, 'L1 restored to 200').toBe(PDT_3612_SEED_LIABILITY_BGN);
        expect(
          bugSettlement,
          `Excel bug amount ${PDT_3612_BUG_SETTLEMENT_BGN} must not be created; seen=[${settlementLiabilityText}]`,
        ).toBeUndefined();
        expect(
          settlementLiability,
          `new L3 of ${PDT_3612_COMPENSATION_BGN}; seen=[${settlementLiabilityText}]`,
        ).toBeTruthy();
        expect(settlementLiability?.initial, 'L3 equals 1200').toBe(PDT_3612_COMPENSATION_BGN);
        expect(settlementLiability?.current, 'L3 closed').toBe(0);
        expect(
          settlementReceivable,
          `new government R3 of ${PDT_3612_COMPENSATION_BGN}; seen=[${settlementReceivableText}]`,
        ).toBeTruthy();
        expect(settlementReceivable?.initial, 'R3 equals 1200').toBe(PDT_3612_COMPENSATION_BGN);
        expect(settlementReceivable?.current, 'R3 closed').toBe(0);
        const invoiceCancelReceivable = newCustomerReceivables.find(
          (row) => row.initial === PDT_3612_INVOICE_INCLUDING_VAT,
        );
        expect(
          invoiceCancelReceivable,
          `Excel R2 ${PDT_3612_INVOICE_INCLUDING_VAT} for invoice cancellation; seen=[${newCustomerReceivables.map((r) => `${r.id}:${r.initial}/${r.current}`).join(', ')}]`,
        ).toBeTruthy();
        expect(invoiceCancelReceivable?.current, 'R2 closed').toBe(0);

        return {
          settlementLiabilityId: settlementLiability?.id,
          settlementReceivableId: settlementReceivable?.id,
        };
      },
    );

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3612_KEY,
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
          gcIds: [created.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: {
          invoiceId: billed.invoiceId,
          invoiceTotal,
          leftover,
          compensationId: created.id,
          seedLiabilityId,
          seedLiabilityAfterInvoice: before.seedLiabilityCurrent,
          settlementLiabilityId: after.settlementLiabilityId,
          settlementReceivableId: after.settlementReceivableId,
          processId: cancel.processId,
        },
      });
    });
  });
});
