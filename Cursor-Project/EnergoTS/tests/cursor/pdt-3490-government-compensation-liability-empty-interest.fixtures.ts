/**
 * PDT-3490 — Government compensation liabilities must not copy Applicable interest rate.
 *
 * Positive Document Amount → GET /customer-liability/{liabilityForRecipient}.
 * Negative Document Amount → GET /customer-liability/{liabilityForCustomer}.
 * Swagger CustomerLiabilityResponse: applicableInterestRate (InterestRateShortResponse),
 * interestDateFrom, interestDateTo. Empty means null or `{ id: null }` (isNullishEntityRef).
 *
 * Data chain is PDT-3486 TC-BE-2 / TC-BE-3 (FOR_VOLUMES debit draft → compensation →
 * PDF → start-accounting). Origin invoice/product-contract may still carry
 * envVariables.interest_rate; compensation liability must stay empty anyway.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3486-standard-billing-compensation-deferred-alo.spec.ts
 * - tests/cursor/pdt-3486-standard-billing-compensation-deferred-alo.fixtures.ts
 * - tests/cursor/PDT-3470-standard-credit-note-government-compensation-receivable.spec.ts
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 */

import { expect } from './cursor-test.fixtures';
import { randomGens } from '../../utils/randomGens';
import {
  uniqueCompensationNumber,
  type Pdt3470LiabilityView,
} from './pdt-3470-standard-credit-note-government-compensation-receivable.fixtures';
import { startPdt3483DebitNoteDraft } from './pdt-3483-billing-run-compensation-credit-debit-note-final-liability.fixtures';
import {
  isNullishEntityRef,
  type InvoiceView,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import {
  PDT_3486_TEST_TIMEOUT_MS,
  type Pdt3486Fx,
} from './pdt-3486-standard-billing-compensation-deferred-alo.fixtures';

export {
  assertLinkedInvoiceIsDebitNote,
  assertNegativeCompensationPolarity,
  assertPositiveCompensationPolarity,
  findAutomaticLiabilityOnInvoice,
  getCompensation,
  getInvoice,
  getLiability,
  nestedId,
  pdt3470PortalExtraLinks,
  postPdt3486Compensation,
  realizeBillingRun,
  roundMoney,
  startGeneratingAndWaitGenerated,
} from './pdt-3486-standard-billing-compensation-deferred-alo.fixtures';

export type { InvoiceView, Pdt3470LiabilityView };
export type { Pdt3486Fx } from './pdt-3486-standard-billing-compensation-deferred-alo.fixtures';

export const PDT_3490_KEY = 'PDT-3490';
export const PDT_3490_TITLE =
  'Government compensation - Interest rate should not be applied in the liabilities of the Government';
export const PDT_3490_TEST_TIMEOUT_MS = PDT_3486_TEST_TIMEOUT_MS;
export const PDT_3490_RELEVANT_ENTITY_KEYS = [
  'customer',
  'pod',
  'product',
  'productContract',
  'billingRun',
  'invoice',
  'compensation',
  'customerLiability',
] as const;

export function uniquePdt3490CompensationNumber(tcTag: string): string {
  return uniqueCompensationNumber(tcTag).replace(/^PDT3470-/, 'PDT3490-');
}

type OpenAccountingPeriod = { id: number; start: string; end: string };

function dayOf(raw: string): string {
  return raw.slice(0, 10);
}

/**
 * Cached envVariables.accounting_period can lag the current month.
 * POST /billing-run rejects invoiceDate outside that period.
 * Use the OPEN period that contains today (AccountingPeriodsResponse).
 * When none does, bill the latest OPEN period and place POD activation and volumes in that month.
 * Swagger: GET /accounting-period/available-accounting-periods.
 */
export async function resolvePdt3490BillingCalendar(Request: Pdt3486Fx['Request']): Promise<{
  accountingPeriodId?: number;
  invoiceDate?: string;
  taxEventDate?: string;
  podActivationDate?: string;
  profileDateRanges?: Array<{ startDate: string; endDate: string; amount: number }>;
  profilePeriodFrom?: string;
  profilePeriodTo?: string;
}> {
  const listed = await Request.get(
    'accounting-period/available-accounting-periods?page=0&size=50',
  );
  await expect(listed).CheckResponse();
  const page = (await listed.json()) as {
    content?: Array<{ id?: number; status?: string; startDate?: string; endDate?: string }>;
  };
  const open: OpenAccountingPeriod[] = (page.content ?? [])
    .filter((row) => String(row.status) === 'OPEN')
    .map((row) => ({
      id: Number(row.id),
      start: row.startDate ? dayOf(row.startDate) : '',
      end: row.endDate ? dayOf(row.endDate) : '',
    }))
    .filter((row) => Number.isFinite(row.id) && row.id > 0 && row.start !== '' && row.end !== '');

  const today = randomGens.generateTodaysDate('yyyy-mm-dd');
  const containing = open.find((row) => today >= row.start && today <= row.end);
  if (containing) {
    return {
      accountingPeriodId: containing.id,
      invoiceDate: today,
      taxEventDate: today,
    };
  }

  const latest = [...open].sort((a, b) => (a.end < b.end ? 1 : a.end > b.end ? -1 : b.id - a.id))[0];
  if (!latest) {
    throw new Error('[PDT-3490] No OPEN accounting period on this environment.');
  }

  const endLocal = new Date(`${latest.end}T00:00:00`);
  endLocal.setHours(23, 45, 0, 0);

  return {
    accountingPeriodId: latest.id,
    invoiceDate: latest.start,
    taxEventDate: latest.start,
    podActivationDate: latest.start,
    profileDateRanges: [
      {
        startDate: latest.start,
        endDate: endLocal.toISOString(),
        amount: 100,
      },
    ],
    profilePeriodFrom: `${latest.start}T00:00:00.000Z`,
    profilePeriodTo: `${latest.end}T00:00:00.000Z`,
  };
}

export async function startPdt3490DebitNoteDraft(fx: Pdt3486Fx) {
  const calendar = await resolvePdt3490BillingCalendar(fx.Request);
  return startPdt3483DebitNoteDraft(fx, calendar);
}

export function nestedPositiveId(value: unknown): number | null {
  if (value == null) {
    return null;
  }
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) {
    return value;
  }
  if (typeof value === 'object' && value !== null && 'id' in value) {
    const id = Number((value as { id?: unknown }).id);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  return null;
}

function isEmptyDate(raw: unknown): boolean {
  return raw == null || raw === '';
}

/**
 * PDT-3490: compensation-created liability Applicable interest rate is truly empty.
 * GET /customer-liability/{id} CustomerLiabilityResponse.applicableInterestRate may be
 * JSON null or `{ id: null }` — both count as empty (isNullishEntityRef).
 * interestDateFrom / interestDateTo must also be empty.
 */
export function assertApplicableInterestRateTrulyEmpty(
  liability: Pdt3470LiabilityView,
  label: string,
): void {
  const rate = liability.applicableInterestRate as { id?: number | null } | null | undefined;
  expect(
    isNullishEntityRef(rate),
    `${label}: applicableInterestRate must be empty (null or {id:null}), got ${JSON.stringify(rate)}`,
  ).toBe(true);
  expect(
    nestedPositiveId(rate),
    `${label}: applicableInterestRate.id must not be a real interest rate`,
  ).toBeNull();
  expect(
    isEmptyDate(liability.interestDateFrom),
    `${label}: interestDateFrom must be empty, got ${JSON.stringify(liability.interestDateFrom)}`,
  ).toBe(true);
  expect(
    isEmptyDate(liability.interestDateTo),
    `${label}: interestDateTo must be empty, got ${JSON.stringify(liability.interestDateTo)}`,
  ).toBe(true);
}

/**
 * Origin invoice / origin invoice liability may still have Applicable interest rate
 * (product-contract additionalParameters.interestRateId / invoice.interestRate).
 * Compensation liability must not copy that id.
 */
export function assertCompensationLiabilityInterestNotCopiedFromOrigin(
  compensationLiability: Pdt3470LiabilityView,
  originInvoice: InvoiceView,
  originInvoiceLiability: Pdt3470LiabilityView,
  label: string,
): {
  originInvoiceInterestRateId: number | null;
  originInvoiceLiabilityInterestRateId: number | null;
} {
  assertApplicableInterestRateTrulyEmpty(compensationLiability, label);
  const originInvoiceInterestRateId = nestedPositiveId(originInvoice.interestRate);
  const originInvoiceLiabilityInterestRateId = nestedPositiveId(
    originInvoiceLiability.applicableInterestRate,
  );
  const copiedId = nestedPositiveId(compensationLiability.applicableInterestRate);
  expect(
    copiedId,
    `${label}: compensation liability must not copy origin invoice interestRate id=${originInvoiceInterestRateId} / origin liability id=${originInvoiceLiabilityInterestRateId}`,
  ).toBeNull();
  if (originInvoiceInterestRateId != null) {
    expect(copiedId, `${label}: must not equal origin invoice.interestRate.id`).not.toBe(
      originInvoiceInterestRateId,
    );
  }
  if (originInvoiceLiabilityInterestRateId != null) {
    expect(
      copiedId,
      `${label}: must not equal origin invoice liability applicableInterestRate.id`,
    ).not.toBe(originInvoiceLiabilityInterestRateId);
  }
  return { originInvoiceInterestRateId, originInvoiceLiabilityInterestRateId };
}

export function describeInterestSnapshot(opts: {
  compensationLiability: Pdt3470LiabilityView;
  originInvoiceInterestRateId: number | null;
  originInvoiceLiabilityInterestRateId: number | null;
}): string {
  return (
    `compLiab=${opts.compensationLiability.id} ` +
    `compRate=${JSON.stringify(opts.compensationLiability.applicableInterestRate)} ` +
    `compFrom=${JSON.stringify(opts.compensationLiability.interestDateFrom)} ` +
    `compTo=${JSON.stringify(opts.compensationLiability.interestDateTo)} ` +
    `originInvoiceRateId=${opts.originInvoiceInterestRateId} ` +
    `originInvoiceLiabRateId=${opts.originInvoiceLiabilityInterestRateId}`
  );
}
