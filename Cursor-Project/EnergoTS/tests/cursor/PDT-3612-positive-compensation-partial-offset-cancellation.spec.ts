/**
 * PDT-3612 — one positive government compensation, a pre-existing customer liability, then cancel.
 *
 * Mirrors Dev invoice 110078 reported in #production-fixes (compensation 900, invoice 490.75,
 * manual liability 1000 created before the invoice):
 * - before accounting, the billed customer already has a manual liability larger than the
 *   compensation;
 * - the government recipient has nothing open before the invoice;
 * - accounting offsets the compensation receivable onto the invoice liability first, and the
 *   leftover onto the manual liability, so the compensation receivable ends at 0 and the
 *   manual liability is only partly paid;
 * - the government compensation liability stays open for the full amount.
 * Cancellation must reverse both offsets of the compensation receivable, restore the manual
 * liability to its original amount, and settle the compensation with a new customer liability
 * for the full compensation amount, not for the share that came back from the invoice.
 * The government side is settled with a new receivable for the full amount.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3610-cancellation-of-invoice-with-compensation-bgn.spec.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 * - tests/cursor/pdt-3486-standard-billing-compensation-deferred-alo.fixtures.ts
 *
 * Swagger (dev; update-swagger-specs.ps1 could not reach the hosts this session, cached
 * config/swagger/dev/swagger-spec.json used):
 * POST /government-compensations — CompensationRequest required documentCurrencyId, documentAmount
 * POST /customer-liability — CustomerLiabilityRequest
 * GET /currencies — CurrencyFilterRequest page, size, statuses ACTIVE
 * POST /invoice-cancellation — CreateInvoiceCancellationRequest required templateId
 * GET /customer-liability/{id}, GET /customer-receivable/{id} — initialAmount, currentAmount
 * GET /customer-liability/list — page, size, prompt, searchFields CUSTOMER
 * GET /customer-receivable — page, size, prompt, customerReceivableSearchBy CUSTOMER
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
import { resolveMainCurrencyId } from './pdt-3573-leftover-offset-cancellation.fixtures';

const PDT_3612_KEY = 'PDT-3612';
const PDT_3612_TITLE = 'Cancellation of invoice with compensations - case2';
/** Larger than the volume invoice, so a leftover remains for the manual liability. */
const COMPENSATION_BGN = 80_000_000;
/** Larger than the compensation, so the leftover pays it only in part. */
const CUSTOMER_SEED_LIABILITY_BGN = 90_000_000;

function money(value: number): number {
  return Math.round(value * 100) / 100;
}

async function postManualLiability(
  fx: Pdt3368Fx,
  opts: { customerId: number; billingGroupId: number; initialAmount: number; currencyId: number },
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.customer_liability();
  payload.customerId = opts.customerId;
  payload.billingGroupId = opts.billingGroupId;
  payload.initialAmount = opts.initialAmount;
  payload.currencyId = opts.currencyId;
  Object.assign(payload, { blockedForLiabilitiesOffsetting: false });
  const res = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  await expect(res).CheckResponse();
  const raw = await res.json();
  const id =
    typeof raw === 'number'
      ? raw
      : Number((raw as { id?: unknown })?.id);
  expect(id, `manual liability id, body=${JSON.stringify(raw)}`).toBeGreaterThan(0);
  fx.Responses.customerLiability.push(id);
  const view = await getCustomerLiability(fx.Request, id);
  expect(String(view['creationType']), 'seed liability is MANUAL').toBe('MANUAL');
  expect(money(toAmountNumber(view.currentAmount)), 'seed liability still open').toBe(opts.initialAmount);
  return id;
}

async function listLiabilityIds(Request: Pdt3368Fx['Request'], customerIdentifier: string): Promise<number[]> {
  const res = await Request.get('customer-liability/list', {
    params: {
      page: 0,
      size: 50,
      columns: 'ID',
      direction: 'DESC',
      prompt: customerIdentifier,
      searchFields: 'CUSTOMER',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? []).map((row) => Number(row.id)).filter((id) => Number.isFinite(id) && id > 0);
}

async function listReceivableIds(Request: Pdt3368Fx['Request'], customerIdentifier: string): Promise<number[]> {
  const res = await Request.get('customer-receivable', {
    params: {
      page: 0,
      size: 50,
      prompt: customerIdentifier,
      customerReceivableSearchBy: 'CUSTOMER',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? []).map((row) => Number(row.id)).filter((id) => Number.isFinite(id) && id > 0);
}

/** Documents created by cancellation are the ones not known before it. */
async function listNewDocuments(
  Request: Pdt3368Fx['Request'],
  kind: 'liability' | 'receivable',
  customerIdentifier: string,
  knownIds: number[],
): Promise<Array<{ id: number; initial: number; current: number }>> {
  const ids =
    kind === 'liability'
      ? await listLiabilityIds(Request, customerIdentifier)
      : await listReceivableIds(Request, customerIdentifier);
  const rows: Array<{ id: number; initial: number; current: number }> = [];
  for (const id of ids) {
    if (knownIds.includes(id)) {
      continue;
    }
    const view =
      kind === 'liability' ? await getCustomerLiability(Request, id) : await getCustomerReceivable(Request, id);
    rows.push({
      id,
      initial: money(toAmountNumber(view.initialAmount)),
      current: money(toAmountNumber(view.currentAmount)),
    });
  }
  return rows;
}

test.describe(`[${PDT_3612_KEY}]: ${PDT_3612_TITLE}`, { tag: ['@billing', '@compensations', '@PDT-3612'] }, () => {
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

    const pre = await test.step('Precondition: customer, government recipient, POD, contract', async () =>
      runMultiPodForVolumesPrechain(fx),
    );

    const billingGroupId = await test.step('Precondition: contract billing group', async () =>
      resolveProductContractBillingGroupId(Request, pre.contractId),
    );

    const seedLiabilityId = await test.step(
      'Precondition: manual liability on the customer, larger than the compensation',
      async () =>
        postManualLiability(fx, {
          customerId: pre.customerId,
          billingGroupId,
          initialAmount: CUSTOMER_SEED_LIABILITY_BGN,
          currencyId: mainCurrencyId,
        }),
    );

    const created = await test.step('Precondition: one positive compensation in the main currency', async () =>
      createGcExpectingCreated(fx, {
        customerId: pre.customerId,
        podId: pre.podAId,
        recipientId: pre.recipientCustomerId,
        documentPeriod: pre.documentPeriod,
        documentAmount: COMPENSATION_BGN,
        volumes: 1,
        price: COMPENSATION_BGN,
        reason: 'PDT-3612-positive-bgn',
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
    expect(invoiceTotal, 'invoice total').toBeGreaterThan(0);
    expect(invoiceTotal, 'invoice total is below the compensation').toBeLessThan(COMPENSATION_BGN);
    const leftover = money(COMPENSATION_BGN - invoiceTotal);

    const customerIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.customerId);
    const recipientIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.recipientCustomerId);

    const before = await test.step(
      'Compensation receivable pays the invoice, then part of the manual liability',
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

        expect(money(toAmountNumber(customerReceivable.initialAmount)), 'customer receivable face amount').toBe(
          COMPENSATION_BGN,
        );
        expect(
          money(toAmountNumber(customerReceivable.currentAmount)),
          'customer compensation receivable fully used',
        ).toBe(0);
        expect(
          seedLiabilityCurrent,
          'manual liability reduced by the compensation leftover after the invoice',
        ).toBe(money(CUSTOMER_SEED_LIABILITY_BGN - leftover));
        expect(money(toAmountNumber(governmentLiability.initialAmount)), 'government liability face amount').toBe(
          COMPENSATION_BGN,
        );
        expect(
          money(toAmountNumber(governmentLiability.currentAmount)),
          'government liability stays open for the full amount',
        ).toBe(COMPENSATION_BGN);

        TestRunSummary.registerPayload('invoice', {
          invoiceId: billed.invoiceId,
          invoiceNumber: invoice.invoiceNumber,
          invoiceTotal,
          customerReceivableId,
          governmentLiabilityId,
          seedLiabilityId,
          seedLiabilityAfterInvoice: seedLiabilityCurrent,
        });

        return {
          customerReceivableId,
          governmentLiabilityId,
          seedLiabilityCurrent,
          knownCustomerLiabilities: await listLiabilityIds(Request, customerIdentifier),
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
      'Poll invoice CANCELLED: manual liability restored, compensation settled for its full amount',
      async () => {
        await waitInvoiceCancelled(Request, billed.invoiceId);
        assertGcUninvoicedEmptyFinancials(await getCompensation(Request, created.id), 'PDT-3612 positive');

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
        const newRecipientReceivables = await listNewDocuments(
          Request,
          'receivable',
          recipientIdentifier,
          before.knownRecipientReceivables,
        );
        const settlementLiability = newCustomerLiabilities[0];
        const settlementReceivable = newRecipientReceivables[0];
        const settlementLiabilityText = newCustomerLiabilities.map((r) => `${r.id}:${r.initial}/${r.current}`).join(', ');
        const settlementReceivableText = newRecipientReceivables.map((r) => `${r.id}:${r.initial}/${r.current}`).join(', ');

        const passed =
          seedLiabilityCurrent === CUSTOMER_SEED_LIABILITY_BGN &&
          newCustomerLiabilities.length === 1 &&
          settlementLiability?.initial === COMPENSATION_BGN &&
          newRecipientReceivables.length === 1 &&
          settlementReceivable?.initial === COMPENSATION_BGN;
        TestRunSummary.recordCheck({
          check: 'Cancellation reverses the leftover offset and settles the compensation for its full amount',
          expectedResult: `Manual liability ${seedLiabilityId} returns to ${CUSTOMER_SEED_LIABILITY_BGN}. One new customer liability of ${COMPENSATION_BGN} closes the compensation receivable. One new government receivable of ${COMPENSATION_BGN} closes the government liability.`,
          actualResult: `invoice=${billed.invoiceId} total=${invoiceTotal} leftoverOnManualLiability=${leftover} manualLiability before=${before.seedLiabilityCurrent} after=${seedLiabilityCurrent}; new customer liabilities=[${settlementLiabilityText}]; new government receivables=[${settlementReceivableText}]`,
          passed,
        });

        expect(money(toAmountNumber(customerReceivable.currentAmount)), 'compensation receivable closed').toBe(0);
        expect(money(toAmountNumber(governmentLiability.currentAmount)), 'government liability closed').toBe(0);
        expect(newRecipientReceivables, 'one new government receivable').toHaveLength(1);
        expect(settlementReceivable.initial, 'new government receivable equals the full compensation').toBe(
          COMPENSATION_BGN,
        );
        expect(settlementReceivable.current, 'new government receivable closed').toBe(0);
        expect(newCustomerLiabilities, 'one new customer liability').toHaveLength(1);
        expect(
          seedLiabilityCurrent,
          'manual liability restored to the amount from before the invoice',
        ).toBe(CUSTOMER_SEED_LIABILITY_BGN);
        expect(
          settlementLiability.initial,
          'new customer liability equals the full compensation, not the share returned from the invoice',
        ).toBe(COMPENSATION_BGN);
        expect(settlementLiability.current, 'new customer liability closed').toBe(0);

        return { settlementLiabilityId: settlementLiability.id, settlementReceivableId: settlementReceivable.id };
      },
    );

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3612_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice', 'compensation'],
        extraLinks: extraPortalLinks({
          gcIds: [created.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: {
          invoiceId: billed.invoiceId,
          invoiceTotal,
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
