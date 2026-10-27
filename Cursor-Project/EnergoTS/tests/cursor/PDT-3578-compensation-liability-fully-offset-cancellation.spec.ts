/**
 * PDT-3578 — follow-up from Kalina's comment (compensation 1928, receivable 62056).
 *
 * An existing customer receivable is larger than the invoice plus the negative compensation,
 * so accounting pays the invoice liability and the whole compensation liability from it.
 * Before the fix, cancellation created no customer receivable for the compensation, because
 * the compensation liability had nothing left open. Expected: cancellation reverses both
 * payments, the receivable returns to its amount before the invoice, and the compensation
 * liability is closed by a new customer receivable for the full compensation amount.
 * The government side is closed by a new liability for the full amount.
 *
 * Swagger (dev; update-swagger-specs.ps1 could not reach the hosts this session, cached
 * config/swagger/dev/swagger-spec.json used):
 * - POST /customer-receivable CustomerReceivableRequest
 *   required: accountingPeriodId, currencyId, customerId, dueDate, initialAmount, occurrenceDate
 * - POST /government-compensations CompensationRequest
 * - PATCH /billing-run/start-generating, PATCH /billing-run/start-accounting
 * - POST /invoice-cancellation CreateInvoiceCancellationRequest (required templateId)
 * - GET /customer-receivable (customerReceivableSearchBy CUSTOMER), GET /customer-receivable/{id}
 * - GET /customer-liability/{id}, GET /invoice
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3578-invoice-cancellation-reverse-automatic-offsetting.spec.ts
 * - tests/cursor/pdt-3486-standard-billing-compensation-deferred-alo.fixtures.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
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
import { resolveEntityIdentifier } from './pdt-3087-government-compensation-defer-to-pdf.fixtures';

const PDT_3578_KEY = 'PDT-3578';
const PDT_3578_TITLE = '[Backend] Invoice Cancellation - Reverse of Automating offseting';

/** Same size as Kalina's example (50 leva). */
const COMPENSATION_LIABILITY = 50;
/** Left on the receivable after it pays the invoice and the whole compensation liability. */
const RECEIVABLE_LEFTOVER = 300;

async function listReceivableIds(Request: Pdt3486Fx['Request'], customerIdentifier: string): Promise<number[]> {
  const res = await Request.get('customer-receivable', {
    params: { page: 0, size: 50, prompt: customerIdentifier, customerReceivableSearchBy: 'CUSTOMER' },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? []).map((row) => Number(row.id)).filter((id) => Number.isFinite(id) && id > 0);
}

async function listLiabilityIds(Request: Pdt3486Fx['Request'], customerIdentifier: string): Promise<number[]> {
  const res = await Request.get('customer-liability/list', {
    params: { page: 0, size: 50, columns: 'ID', direction: 'DESC', prompt: customerIdentifier, searchFields: 'CUSTOMER' },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? []).map((row) => Number(row.id)).filter((id) => Number.isFinite(id) && id > 0);
}

async function listNewDocuments(
  Request: Pdt3486Fx['Request'],
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
    const view = kind === 'liability' ? await getLiability(Request, id) : await getReceivable(Request, id);
    rows.push({ id, initial: roundMoney(view.initialAmount), current: roundMoney(view.currentAmount) });
  }
  return rows;
}

function describeRows(rows: Array<{ id: number; initial: number; current: number }>): string {
  return rows.map((r) => `${r.id}:${r.initial}/${r.current}`).join(', ');
}

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

    const debit = await test.step('Precondition: customer, recipient, contract and draft volume billing run', async () =>
      startPdt3483DebitNoteDraft(fx),
    );
    const invoiceTotal = roundMoney(debit.T);
    expect(invoiceTotal, 'draft invoice totalAmountIncludingVat').toBeGreaterThan(0);

    const receivableInitial = roundMoney(invoiceTotal + COMPENSATION_LIABILITY + RECEIVABLE_LEFTOVER);
    const compensationAmount = -COMPENSATION_LIABILITY;

    const billingGroupId = await test.step('Precondition: contract billing group for the manual receivable', async () =>
      resolveProductContractBillingGroupId(Request, debit.chain.contractId),
    );

    const seeded = await test.step(
      'Precondition: manual receivable larger than the invoice plus the compensation, before generate',
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

    const compensationNumber = uniquePdt3486CompensationNumber('PDT3578F');
    const compensation = await test.step('Precondition: negative government compensation of 50', async () =>
      postPdt3486Compensation(fx, {
        customerId: debit.chain.customerId,
        podId: debit.chain.podId,
        recipientId: debit.chain.recipientCustomerId,
        documentPeriod: debit.chain.documentPeriod,
        documentAmount: compensationAmount,
        number: compensationNumber,
        reason: 'PDT-3578-full-offset',
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

    const before = await test.step(
      'Read: the receivable pays the invoice and the whole compensation liability',
      async () => {
        const compView = await getCompensation(Request, compensation.id);
        assertNegativeCompensationPolarity(compView, 'PDT-3578 full offset');
        const compensationLiabilityId = nestedId(compView.liabilityForCustomer) as number;
        const governmentReceivableId = nestedId(compView.receivableForRecipient) as number;
        expect(governmentReceivableId, 'government compensation receivable').toBeGreaterThan(0);

        const mainLiability = await findAutomaticLiabilityOnInvoice(
          Request,
          debit.debitInvoiceId,
          invoiceTotal,
          [compensationLiabilityId],
        );
        const compensationLiability = await getLiability(Request, compensationLiabilityId);
        const receivable = await getReceivable(Request, seeded.id as number);
        const governmentReceivable = await getReceivable(Request, governmentReceivableId);

        expect(roundMoney(mainLiability.initialAmount), 'invoice liability amount').toBe(invoiceTotal);
        expect(roundMoney(mainLiability.currentAmount), 'invoice liability fully paid').toBe(0);
        expect(roundMoney(compensationLiability.initialAmount), 'compensation liability amount').toBe(
          COMPENSATION_LIABILITY,
        );
        expect(roundMoney(compensationLiability.currentAmount), 'compensation liability fully paid').toBe(0);
        expect(roundMoney(receivable.currentAmount), 'receivable keeps only the leftover').toBe(RECEIVABLE_LEFTOVER);
        expect(roundMoney(governmentReceivable.currentAmount), 'government receivable still open').toBe(
          COMPENSATION_LIABILITY,
        );

        const customerIdentifier = await resolveEntityIdentifier(Request, 'customer', debit.chain.customerId);
        const recipientIdentifier = await resolveEntityIdentifier(
          Request,
          'customer',
          debit.chain.recipientCustomerId,
        );
        TestRunSummary.registerPayload('customerLiability', {
          mainLiabilityId: mainLiability.id,
          compensationLiabilityId,
          governmentReceivableId,
        });
        return {
          mainLiabilityId: Number(mainLiability.id),
          compensationLiabilityId,
          governmentReceivableId,
          customerIdentifier,
          recipientIdentifier,
          knownCustomerReceivables: await listReceivableIds(Request, customerIdentifier),
          knownRecipientLiabilities: await listLiabilityIds(Request, recipientIdentifier),
        };
      },
    );

    const cancel = await test.step('Action: POST /invoice-cancellation', async () => {
      const taxEventDate = await resolveOpenTaxEventDate(Request);
      const posted = await postCancelByInvoiceNumber(Request, invoiceNumber, taxEventDate);
      expect(posted.status, `POST /invoice-cancellation HTTP 201, body=${posted.rawText}`).toBe(201);
      expect(posted.processId, 'processId').toBeGreaterThan(0);
      Responses.invoiceCancellation.push(posted.body);
      TestRunSummary.registerPayload('invoiceCancellation', posted.body);
      return posted;
    });

    const after = await test.step(
      'Read: receivable restored and the compensation closed by a new receivable for the full amount',
      async () => {
        await waitProcessFinished(Request, cancel.processId);
        await waitInvoiceCancelled(Request, debit.debitInvoiceId);

        const receivable = await getReceivable(Request, seeded.id as number);
        const compensationLiability = await getLiability(Request, before.compensationLiabilityId);
        const governmentReceivable = await getReceivable(Request, before.governmentReceivableId);
        const restored = roundMoney(receivable.currentAmount);
        const newReceivables = await listNewDocuments(
          Request,
          'receivable',
          before.customerIdentifier,
          before.knownCustomerReceivables,
        );
        const newGovernmentLiabilities = await listNewDocuments(
          Request,
          'liability',
          before.recipientIdentifier,
          before.knownRecipientLiabilities,
        );
        const compensationSettlement = newReceivables.find((r) => r.initial === COMPENSATION_LIABILITY);
        const governmentSettlement = newGovernmentLiabilities.find((r) => r.initial === COMPENSATION_LIABILITY);

        TestRunSummary.recordCheck({
          check: 'Cancellation reverses a full payment of the compensation liability',
          expectedResult: `Receivable ${seeded.id} returns to ${receivableBeforeInvoice}. A new customer receivable of ${COMPENSATION_LIABILITY} closes compensation liability ${before.compensationLiabilityId}. A new government liability of ${COMPENSATION_LIABILITY} closes government receivable ${before.governmentReceivableId}.`,
          actualResult: `receivable before=${receivableBeforeInvoice} afterInvoice=${RECEIVABLE_LEFTOVER} afterCancel=${restored}; new customer receivables=[${describeRows(newReceivables)}]; new government liabilities=[${describeRows(newGovernmentLiabilities)}]; compensation liability current=${roundMoney(compensationLiability.currentAmount)}; government receivable current=${roundMoney(governmentReceivable.currentAmount)}`,
          passed:
            restored === receivableBeforeInvoice &&
            compensationSettlement !== undefined &&
            governmentSettlement !== undefined &&
            roundMoney(governmentReceivable.currentAmount) === 0,
        });

        expect(restored, 'receivable restored to the amount before the invoice').toBe(receivableBeforeInvoice);
        expect(
          compensationSettlement,
          `new customer receivable of ${COMPENSATION_LIABILITY} for the compensation; new receivables=[${describeRows(newReceivables)}]`,
        ).toBeDefined();
        expect(compensationSettlement!.current, 'compensation receivable closed against the liability').toBe(0);
        expect(roundMoney(compensationLiability.currentAmount), 'compensation liability closed').toBe(0);
        expect(
          governmentSettlement,
          `new government liability of ${COMPENSATION_LIABILITY}; new liabilities=[${describeRows(newGovernmentLiabilities)}]`,
        ).toBeDefined();
        expect(governmentSettlement!.current, 'new government liability closed').toBe(0);
        expect(roundMoney(governmentReceivable.currentAmount), 'government receivable closed').toBe(0);
        return {
          restored,
          compensationSettlementId: compensationSettlement!.id,
          governmentSettlementId: governmentSettlement!.id,
        };
      },
    );

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
            `${DEV_PORTAL_BASE}/customer-receivables/preview?id=${after.compensationSettlementId}`,
            `${DEV_PORTAL_BASE}/customer-receivables/preview?id=${before.governmentReceivableId}`,
          ],
          customerLiability: [
            `${DEV_PORTAL_BASE}/customer-liabilities/preview?id=${before.mainLiabilityId}`,
            `${DEV_PORTAL_BASE}/customer-liabilities/preview?id=${before.compensationLiabilityId}`,
            `${DEV_PORTAL_BASE}/customer-liabilities/preview?id=${after.governmentSettlementId}`,
          ],
        },
        snapshot: {
          invoiceId: debit.debitInvoiceId,
          invoiceTotal,
          receivableId: seeded.id,
          receivableBeforeInvoice,
          restored: after.restored,
          compensationId: compensation.id,
          compensationSettlementId: after.compensationSettlementId,
        },
      });
    });
  });
});
