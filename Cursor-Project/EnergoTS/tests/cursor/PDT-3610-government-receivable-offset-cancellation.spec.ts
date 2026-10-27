/**
 * PDT-3610 — government side of Nika's example (invoice 110044): a positive compensation whose
 * government liability was partly paid by another government receivable before cancellation.
 *
 * Same shape as PDT-3578 (an existing receivable pays part of a compensation liability), but on
 * the government recipient. Before the PDT-3578 fix, cancellation left the 30 on the government
 * receivable and closed the compensation with a receivable for 80,000,000 − 30. Expected after
 * the fix: the 30 payment is reversed, the government receivable returns to 30, and the
 * government compensation liability is closed by a new receivable for the full 80,000,000.
 *
 * Automatic offsetting does not pick up a government receivable with no billing group
 * (Dev run 7 Oct 2026, receivable 62414 stayed at 30). This spec pays the 30 with
 * POST /manual-liability-offsetting, then cancels the invoice.
 *
 * Swagger (dev, refreshed this session, config/swagger/dev/swagger-spec.json):
 * - POST /government-compensations CompensationRequest required documentCurrencyId, documentAmount
 * - POST /customer-receivable CustomerReceivableRequest
 *   required: accountingPeriodId, currencyId, customerId, dueDate, initialAmount, occurrenceDate
 * - GET /currencies — page, size, statuses ACTIVE, prompt
 * - GET /customer/{id}
 * - GET /template/available — templatePurposes MANUAL_LIABILITY_OFFSET, type DOCUMENT, language BULGARIAN
 * - POST /manual-liability-offsetting/calculate ManualLiabilityOffsettingCalculateRequest
 * - POST /manual-liability-offsetting ManualLiabilityOffsettingCreateRequest
 *   required customerCommunicationDataId, customerDetailId, customerId, date
 * - POST /invoice-cancellation CreateInvoiceCancellationRequest required templateId
 * - GET /customer-receivable (customerReceivableSearchBy CUSTOMER), GET /customer-receivable/{id}
 * - GET /customer-liability/{id}
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3612-positive-compensation-partial-offset-cancellation.spec.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { DEV_PORTAL_BASE } from './dev-volume-billing-two-compensations.fixtures';
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
import { resolveEntityIdentifier } from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import { resolveMainCurrencyId } from './pdt-3573-leftover-offset-cancellation.fixtures';

const PDT_3610_KEY = 'PDT-3610';
const PDT_3610_TITLE = 'Cancellation of invoice with compensation';
/** Same amounts as Nika's example on invoice 110044. */
const COMPENSATION_BGN = 80_000_000;
const GOVERNMENT_RECEIVABLE_BGN = 30;

function money(value: number): number {
  return Math.round(value * 100) / 100;
}

/** Government recipients have no contract, so the receivable has no billing group. */
async function postGovernmentReceivable(
  fx: Pdt3368Fx,
  opts: { customerId: number; initialAmount: number; currencyId: number },
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.customer_receivable() as Record<string, unknown>;
  payload.customerId = opts.customerId;
  payload.initialAmount = opts.initialAmount;
  payload.currencyId = opts.currencyId;
  payload.blockedForOffsetting = false;
  delete payload.billingGroupId;
  const res = await fx.Request.post(fx.Endpoints.customerReceivable, { data: payload });
  await expect(res).CheckResponse();
  const raw = await res.json();
  const id = typeof raw === 'number' ? raw : Number((raw as { id?: unknown })?.id);
  expect(id, `government receivable id, body=${JSON.stringify(raw)}`).toBeGreaterThan(0);
  fx.Responses.customerReceivable.push(id);
  return id;
}

type OffsetLine = { id: number; currency_id: number; current_amount: number };

async function resolveCustomerOffsettingParty(
  Request: Pdt3368Fx['Request'],
  customerId: number,
): Promise<{ customerDetailId: number; customerCommunicationDataId: number }> {
  const res = await Request.get(`customer/${customerId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    lastCustomerDetailId?: number;
    communicationData?: Array<{ id?: number }>;
  };
  const customerDetailId = Number(body.lastCustomerDetailId);
  const customerCommunicationDataId = Number(body.communicationData?.[0]?.id);
  expect(customerDetailId, `customer ${customerId} lastCustomerDetailId`).toBeGreaterThan(0);
  expect(customerCommunicationDataId, `customer ${customerId} communicationData[0].id`).toBeGreaterThan(0);
  return { customerDetailId, customerCommunicationDataId };
}

async function resolveManualOffsetDocumentTemplateId(Request: Pdt3368Fx['Request']): Promise<number> {
  const res = await Request.get('template/available', {
    params: {
      templatePurposes: 'MANUAL_LIABILITY_OFFSET',
      type: 'DOCUMENT',
      language: 'BULGARIAN',
      page: 0,
      size: 20,
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  const id = Number(body.content?.[0]?.id);
  expect(id, 'MANUAL_LIABILITY_OFFSET DOCUMENT template').toBeGreaterThan(0);
  return id;
}

async function postManualLiabilityOffsetting(
  fx: Pdt3368Fx,
  opts: {
    customerId: number;
    date: string;
    currencyId: number;
    liabilityId: number;
    liabilityCurrent: number;
    receivableId: number;
    receivableCurrent: number;
  },
): Promise<number> {
  const party = await resolveCustomerOffsettingParty(fx.Request, opts.customerId);
  const templateId = await resolveManualOffsetDocumentTemplateId(fx.Request);
  const liabilities: OffsetLine[] = [
    { id: opts.liabilityId, currency_id: opts.currencyId, current_amount: opts.liabilityCurrent },
  ];
  const receivables: OffsetLine[] = [
    { id: opts.receivableId, currency_id: opts.currencyId, current_amount: opts.receivableCurrent },
  ];
  const calcRes = await fx.Request.post('manual-liability-offsetting/calculate', {
    data: { liabilities, receivables },
  });
  await expect(calcRes).CheckResponse();
  const calc = (await calcRes.json()) as {
    liabilities?: unknown[];
    receivables?: unknown[];
    deposits?: unknown[];
    payments?: unknown[];
    offsets?: Array<{ l_id?: number; r_id?: number; offset_amount?: number }>;
    message?: string;
  };
  expect(
    calc.offsets?.length,
    `calculate offsets for liability ${opts.liabilityId} and receivable ${opts.receivableId}, body=${JSON.stringify(calc)}`,
  ).toBeGreaterThan(0);

  const createRes = await fx.Request.post('manual-liability-offsetting', {
    data: {
      date: opts.date,
      customerId: opts.customerId,
      customerDetailId: party.customerDetailId,
      customerCommunicationDataId: party.customerCommunicationDataId,
      liabilities,
      receivables,
      receivedLiabilities: (calc.liabilities ?? []).map((row) => {
        const line = row as { id?: number; currency_id?: number; amount?: number; current_amount?: number };
        return {
          ...(row as object),
          current_amount: line.amount ?? line.current_amount,
        };
      }),
      receivedReceivables: (calc.receivables ?? []).map((row) => {
        const line = row as { amount?: number; current_amount?: number };
        return { ...(row as object), current_amount: line.amount ?? line.current_amount };
      }),
      receivedDeposits: calc.deposits ?? [],
      receivedPayments: calc.payments ?? [],
      receivedOffsets: calc.offsets ?? [],
      templateIds: [{ templateId, templateType: 'DOCUMENT' }],
    },
  });
  await expect(createRes).CheckResponse();
  const raw = await createRes.json();
  const id = typeof raw === 'number' ? raw : Number((raw as { id?: unknown })?.id);
  expect(id, `manual-liability-offsetting id, body=${JSON.stringify(raw)}`).toBeGreaterThan(0);
  return id;
}

async function listReceivableIds(Request: Pdt3368Fx['Request'], customerIdentifier: string): Promise<number[]> {
  const res = await Request.get('customer-receivable', {
    params: { page: 0, size: 50, prompt: customerIdentifier, customerReceivableSearchBy: 'CUSTOMER' },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? []).map((row) => Number(row.id)).filter((id) => Number.isFinite(id) && id > 0);
}

async function listNewReceivables(
  Request: Pdt3368Fx['Request'],
  customerIdentifier: string,
  knownIds: number[],
): Promise<Array<{ id: number; initial: number; current: number }>> {
  const rows: Array<{ id: number; initial: number; current: number }> = [];
  for (const id of await listReceivableIds(Request, customerIdentifier)) {
    if (knownIds.includes(id)) {
      continue;
    }
    const view = await getCustomerReceivable(Request, id);
    rows.push({
      id,
      initial: money(toAmountNumber(view.initialAmount)),
      current: money(toAmountNumber(view.currentAmount)),
    });
  }
  return rows;
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

    const mainCurrencyId = await test.step('Precondition: resolve the active main currency', async () =>
      resolveMainCurrencyId(Request),
    );

    const pre = await test.step('Precondition: customer, government recipient, POD, contract', async () =>
      runMultiPodForVolumesPrechain(fx),
    );

    const created = await test.step('Precondition: one positive compensation of 80,000,000 in the main currency', async () =>
      createGcExpectingCreated(fx, {
        customerId: pre.customerId,
        podId: pre.podAId,
        recipientId: pre.recipientCustomerId,
        documentPeriod: pre.documentPeriod,
        documentAmount: COMPENSATION_BGN,
        volumes: 1,
        price: COMPENSATION_BGN,
        reason: 'PDT-3610-government-side',
        documentCurrencyId: mainCurrencyId,
      }),
    );
    TestRunSummary.registerPayload('compensation', created.payload);

    const billed = await test.step('Precondition: account the volume invoice with the compensation', async () =>
      completeForVolumesToAccounted(fx),
    );

    const invoice = await test.step('Read: invoice REAL and government liability open for the full amount', async () => {
      const view = await getInvoice(Request, billed.invoiceId);
      expect(String(view.invoiceStatus), 'invoice REAL').toBe('REAL');
      const comp = await getCompensation(Request, created.id);
      expect(comp.compensationStatus, 'compensation INVOICED').toBe('INVOICED');
      const governmentLiabilityId = Number(comp.liabilityForRecipient?.id);
      expect(governmentLiabilityId, 'government compensation liability').toBeGreaterThan(0);
      const governmentLiabilityAtStart = await getCustomerLiability(Request, governmentLiabilityId);
      expect(money(toAmountNumber(governmentLiabilityAtStart.currentAmount)), 'government liability open').toBe(
        COMPENSATION_BGN,
      );
      const recipientIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.recipientCustomerId);
      return { view, governmentLiabilityId, recipientIdentifier };
    });
    const governmentLiabilityId = invoice.governmentLiabilityId;
    const recipientIdentifier = invoice.recipientIdentifier;

    const seed = await test.step(
      'Precondition: government receivable of 30, still open (no automatic offset)',
      async () => {
        const id = await postGovernmentReceivable(fx, {
          customerId: pre.recipientCustomerId,
          initialAmount: GOVERNMENT_RECEIVABLE_BGN,
          currencyId: mainCurrencyId,
        });
        const receivable = await getCustomerReceivable(Request, id);
        const liability = await getCustomerLiability(Request, governmentLiabilityId);
        const receivableCurrent = money(toAmountNumber(receivable.currentAmount));
        const liabilityCurrent = money(toAmountNumber(liability.currentAmount));
        TestRunSummary.registerPayload('customerReceivable', {
          governmentReceivableId: id,
          initialAmount: GOVERNMENT_RECEIVABLE_BGN,
          currentAfterCreate: receivableCurrent,
          governmentLiabilityCurrent: liabilityCurrent,
        });
        expect(receivableCurrent, 'government receivable still open after create').toBe(GOVERNMENT_RECEIVABLE_BGN);
        expect(liabilityCurrent, 'government liability still open for the full amount').toBe(COMPENSATION_BGN);
        return { id };
      },
    );

    const offsettingId = await test.step(
      'Action: POST /manual-liability-offsetting pays 30 of the government compensation liability',
      async () => {
        const taxEventDate = await resolveOpenTaxEventDate(Request);
        const id = await postManualLiabilityOffsetting(fx, {
          customerId: pre.recipientCustomerId,
          date: taxEventDate,
          currencyId: mainCurrencyId,
          liabilityId: governmentLiabilityId,
          liabilityCurrent: COMPENSATION_BGN,
          receivableId: seed.id,
          receivableCurrent: GOVERNMENT_RECEIVABLE_BGN,
        });
        const receivable = await getCustomerReceivable(Request, seed.id);
        const liability = await getCustomerLiability(Request, governmentLiabilityId);
        const receivableCurrent = money(toAmountNumber(receivable.currentAmount));
        const liabilityCurrent = money(toAmountNumber(liability.currentAmount));
        TestRunSummary.registerPayload('manualLiabilityOffsetting', {
          id,
          receivableCurrent,
          liabilityCurrent,
        });
        expect(receivableCurrent, 'government receivable used in full by manual offsetting').toBe(0);
        expect(liabilityCurrent, 'government liability reduced by 30').toBe(
          money(COMPENSATION_BGN - GOVERNMENT_RECEIVABLE_BGN),
        );
        return id;
      },
    );

    const knownRecipientReceivables = await test.step(
      'Read: government receivables known before cancellation',
      async () => listReceivableIds(Request, recipientIdentifier),
    );

    const cancel = await test.step('Action: POST /invoice-cancellation by invoice number', async () => {
      const taxEventDate = await resolveOpenTaxEventDate(Request);
      const posted = await postCancelByInvoiceNumber(Request, String(invoice.view.invoiceNumber), taxEventDate);
      expect(posted.status, `POST /invoice-cancellation HTTP 201, body=${posted.rawText}`).toBe(201);
      expect(posted.processId, 'processId').toBeGreaterThan(0);
      Responses.invoiceCancellation.push(posted.body);
      TestRunSummary.registerPayload('invoiceCancellation', posted.body);
      return posted;
    });

    const after = await test.step(
      'Read: government receivable restored and the liability closed for the full amount',
      async () => {
        await waitInvoiceCancelled(Request, billed.invoiceId);
        assertGcUninvoicedEmptyFinancials(await getCompensation(Request, created.id), 'PDT-3610 government side');

        const receivable = await getCustomerReceivable(Request, seed.id);
        const liability = await getCustomerLiability(Request, governmentLiabilityId);
        const restored = money(toAmountNumber(receivable.currentAmount));
        const newReceivables = await listNewReceivables(Request, recipientIdentifier, knownRecipientReceivables);
        const settlement = newReceivables[0];
        const newText = newReceivables.map((r) => `${r.id}:${r.initial}/${r.current}`).join(', ');

        TestRunSummary.recordCheck({
          check: 'Cancellation reverses the government receivable payment and closes the liability for the full amount',
          expectedResult: `Government receivable ${seed.id} returns to ${GOVERNMENT_RECEIVABLE_BGN}. One new government receivable of ${COMPENSATION_BGN} closes liability ${governmentLiabilityId}.`,
          actualResult: `invoice=${billed.invoiceId}; government receivable after cancel=${restored}; government liability current=${money(toAmountNumber(liability.currentAmount))}; new government receivables=[${newText}]`,
          passed:
            restored === GOVERNMENT_RECEIVABLE_BGN &&
            newReceivables.length === 1 &&
            settlement?.initial === COMPENSATION_BGN,
        });

        expect(money(toAmountNumber(liability.currentAmount)), 'government liability closed').toBe(0);
        expect(newReceivables, `one new government receivable; seen=[${newText}]`).toHaveLength(1);
        expect(restored, 'government receivable restored to 30').toBe(GOVERNMENT_RECEIVABLE_BGN);
        expect(settlement.initial, 'new government receivable equals the full compensation').toBe(COMPENSATION_BGN);
        expect(settlement.current, 'new government receivable closed').toBe(0);
        return { restored, settlementId: settlement.id };
      },
    );

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3610_KEY,
        relevantEntityKeys: [
          'customer',
          'productContract',
          'billingRun',
          'invoice',
          'compensation',
          'customerReceivable',
          'customerLiability',
          'invoiceCancellation',
          'manualLiabilityOffsetting',
        ],
        extraLinks: {
          ...extraPortalLinks({
            gcIds: [created.id],
            invoiceId: billed.invoiceId,
            billingRunId: billed.billingRunId,
            processId: cancel.processId,
          }),
          customerReceivable: [
            `${DEV_PORTAL_BASE}/customer-receivables/preview?id=${seed.id}`,
            `${DEV_PORTAL_BASE}/customer-receivables/preview?id=${after.settlementId}`,
          ],
          customerLiability: [`${DEV_PORTAL_BASE}/customer-liabilities/preview?id=${governmentLiabilityId}`],
          manualLiabilityOffsetting: [
            `${DEV_PORTAL_BASE}/manual-liability-offsetting/preview?id=${offsettingId}`,
          ],
        },
        snapshot: {
          invoiceId: billed.invoiceId,
          compensationId: created.id,
          governmentLiabilityId,
          governmentReceivableId: seed.id,
          offsettingId,
          restored: after.restored,
          settlementId: after.settlementId,
          processId: cancel.processId,
        },
      });
    });
  });
});
