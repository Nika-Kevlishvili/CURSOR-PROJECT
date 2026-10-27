/**
 * PDT-3610 — Cancellation of invoice with compensation, document currency BGN (leva).
 *
 * Same document shape as the EUR spec: one positive and one negative government
 * compensation, each on its own POD, positive 120 and negative -100. Here the
 * compensation document currency is leva, which is also the invoice currency, so
 * the liability and receivable amounts stay 120 and 100. This run stops after
 * accounting: the invoice stays REAL and is not cancelled. Government automatic
 * offsetting leaves a remainder of 20. The customer receivable is offset onto
 * the main invoice debt.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3610-cancellation-of-invoice-with-compensation.spec.ts
 * - tests/cursor/PDT-3368-invoice-cancellation-government-compensations.spec.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 * - tests/cursor/pdt-3470-standard-credit-note-government-compensation-receivable.fixtures.ts
 *
 * Swagger (dev, refreshed this session):
 * POST /government-compensations — CompensationRequest required documentCurrencyId (int64), documentAmount
 * GET /currencies — listing request; CurrencyResponse abbreviation
 * POST /invoice-cancellation — CreateInvoiceCancellationRequest required templateId
 * GET /customer-liability/list — CustomerLiabilityListingRequest required page, size;
 *   searchFields ALL|ID|CUSTOMER|BILLINGGROUP
 * GET /customer-receivable — CustomerReceivableListingRequest required page, size;
 *   customerReceivableSearchBy ALL|ID|CUSTOMER|BILLINGGROUP
 * GET /customer-liability/{id}, GET /customer-receivable/{id} — initialAmount, currentAmount, status
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

const PDT_3610_KEY = 'PDT-3610';
const PDT_3610_TITLE = 'Cancellation of invoice with compensation';
const POSITIVE_BGN = 120;
const NEGATIVE_BGN = -100;

function requireLink(id: number | null, label: string): number {
  expect(id, label).toBeTruthy();
  return Number(id);
}

function money(value: number): number {
  return Math.round(value * 100) / 100;
}

async function resolveLevaCurrencyId(Request: Pdt3368Fx['Request']): Promise<number> {
  const res = await Request.get('currencies', {
    params: { page: 0, size: 50, statuses: 'ACTIVE', prompt: 'BGN' },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<Record<string, unknown>> };
  const hit = (body.content ?? []).find((row) => {
    const hay = [row.name, row.abbreviation, row.printName, row.fullName]
      .map((part) => String(part ?? '').toUpperCase())
      .join(' ');
    if (hay.includes('EUR') || hay.includes('ЕВРО')) {
      return false;
    }
    return hay.includes('BGN') || hay.includes('ЛЕВ');
  });
  const id = Number(hit?.id);
  expect(Number.isFinite(id) && id > 0, 'active BGN (leva) currency').toBe(true);
  return id;
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

test.describe(`[${PDT_3610_KEY}]: ${PDT_3610_TITLE}`, { tag: ['@billing', '@compensations', '@PDT-3610', '@bgn'] }, () => {
  test(`[${PDT_3610_KEY}]: ${PDT_3610_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const levaCurrencyId = await test.step('Precondition: resolve BGN (leva) currency', async () =>
      resolveLevaCurrencyId(Request),
    );

    const pre = await test.step('Precondition: billed customer + recipient + two PODs + FOR_VOLUMES contract/BBP', async () =>
      runMultiPodForVolumesPrechain(fx),
    );

    const billed = await test.step('Precondition: 120 BGN and -100 BGN on separate PODs through accounting', async () =>
      accountGcsOnRealDebit(fx, pre, [
        {
          documentAmount: POSITIVE_BGN,
          podId: pre.podAId,
          reason: 'PDT-3610-positive-bgn',
          documentCurrencyId: levaCurrencyId,
        },
        {
          documentAmount: NEGATIVE_BGN,
          podId: pre.podBId,
          reason: 'PDT-3610-negative-bgn',
          documentCurrencyId: levaCurrencyId,
        },
      ]),
    );
    const positive = billed.gcs[0]!;
    const negative = billed.gcs[1]!;
    TestRunSummary.registerPayload('compensation', {
      positive: positive.payload,
      negative: negative.payload,
    });

    const before = await test.step('Precondition: government netted to the remainder, customer receivable offset on the main debt', async () => {
      expect(positive.view.compensationStatus, 'positive GC INVOICED').toBe('INVOICED');
      expect(negative.view.compensationStatus, 'negative GC INVOICED').toBe('INVOICED');
      expect(positive.view.invoice?.id, 'positive on billed invoice').toBe(billed.invoiceId);
      expect(negative.view.invoice?.id, 'negative on billed invoice').toBe(billed.invoiceId);
      assertPositiveApplyAttachments(positive.view, billed.invoiceId, 'PDT-3610 positive BGN');
      assertNegativeApplyAttachments(negative.view, billed.invoiceId, 'PDT-3610 negative BGN');

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

      expect(Number(positive.payload.documentCurrencyId), 'positive document currency is BGN').toBe(levaCurrencyId);
      expect(Number(negative.payload.documentCurrencyId), 'negative document currency is BGN').toBe(levaCurrencyId);

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
      const positiveAmount = toAmountNumber(governmentLiability.initialAmount);
      const negativeAmount = toAmountNumber(governmentReceivable.initialAmount);
      const remainder = money(positiveAmount - negativeAmount);
      expect(positiveAmount, 'positive compensation stays 120 BGN').toBe(POSITIVE_BGN);
      expect(negativeAmount, 'negative compensation stays 100 BGN').toBe(Math.abs(NEGATIVE_BGN));
      expect(remainder, 'government remainder is 20 BGN').toBe(money(POSITIVE_BGN - Math.abs(NEGATIVE_BGN)));
      expect(toAmountNumber(governmentLiability.currentAmount), 'government liability already netted to remainder').toBe(
        remainder,
      );
      expect(toAmountNumber(governmentReceivable.currentAmount), 'government receivable fully offset').toBe(0);
      expect(toAmountNumber(customerReceivable.initialAmount), 'customer receivable equals positive compensation').toBe(
        positiveAmount,
      );
      expect(toAmountNumber(customerReceivable.currentAmount), 'customer receivable offset onto main debt').toBe(0);
      expect(toAmountNumber(customerLiability.initialAmount), 'customer liability equals negative compensation').toBe(
        negativeAmount,
      );
      expect(toAmountNumber(customerLiability.currentAmount), 'customer compensation liability still full').toBe(
        negativeAmount,
      );
      expect(toAmountNumber(main.currentAmount), 'main debt reduced by the positive compensation').toBe(
        money(toAmountNumber(main.initialAmount) - positiveAmount),
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
        positiveAmount,
        negativeAmount,
        remainder,
        recipientLiabilityIds,
        recipientReceivableIds,
        customerLiabilityIds,
        customerReceivableIds,
        customerIdentifier,
        recipientIdentifier,
      };
    });

    await test.step('Invoice stays REAL and is not cancelled', async () => {
      const invoice = await getInvoice(Request, billed.invoiceId);
      expect(String(invoice.invoiceStatus), 'invoice remains REAL').toBe('REAL');
      expect(positive.view.compensationStatus, 'positive compensation stays INVOICED').toBe('INVOICED');
      expect(negative.view.compensationStatus, 'negative compensation stays INVOICED').toBe('INVOICED');

      TestRunSummary.recordCheck({
        check: 'Mixed-sign BGN compensations are invoiced and the invoice is left open',
        expectedResult:
          'Document amounts are 120 BGN and -100 BGN on two PODs. Government liability current is the 20 BGN remainder. The invoice stays REAL. No cancellation is posted.',
        actualResult: `As expected — invoice=${billed.invoiceId} status=REAL remainder=${before.remainder} positiveGc=${positive.id} negativeGc=${negative.id}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3610_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice', 'compensation'],
        extraLinks: extraPortalLinks({
          gcIds: [positive.id, negative.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
        }),
        snapshot: {
          invoiceId: billed.invoiceId,
          positiveGcId: positive.id,
          negativeGcId: negative.id,
          remainder: before.remainder,
        },
      });
    });
  });
});
