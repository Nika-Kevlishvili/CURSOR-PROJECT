/**
 * Shared helpers for PDT-3612 / PDT-3610 leftover-offset cancellation cases
 * that check whether the PDT-3573 fix also restores pre-existing documents.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3612-positive-compensation-partial-offset-cancellation.spec.ts
 * - tests/cursor/PDT-3610-government-receivable-offset-cancellation.spec.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 *
 * Swagger (dev, update-swagger-specs.ps1 this session):
 * POST /customer-liability CustomerLiabilityRequest required
 *   accountingPeriodId, currencyId, customerId, dueDate, initialAmount, occurrenceDate
 * POST /customer-receivable CustomerReceivableRequest required
 *   accountingPeriodId, currencyId, customerId, dueDate, initialAmount, occurrenceDate
 * GET /customer-liability/list CustomerLiabilityListingRequest required page, size
 * GET /customer-receivable CustomerReceivableListingRequest required page, size
 * GET /template/available templatePurposes MANUAL_LIABILITY_OFFSET
 * POST /manual-liability-offsetting/calculate ManualLiabilityOffsettingCalculateRequest
 * POST /manual-liability-offsetting ManualLiabilityOffsettingCreateRequest required
 *   customerCommunicationDataId, customerDetailId, customerId, date
 */

import { expect } from './cursor-test.fixtures';
import {
  getCustomerLiability,
  getCustomerReceivable,
  toAmountNumber,
  type Pdt3368Fx,
} from './pdt-3368-invoice-cancellation-government-compensations.fixtures';

/**
 * PDT-3612 Excel `case2` / invoice 109863:
 * compensation 1200, manual L1 200, main invoice 686.72.
 * Bug settlement was 1000 (1200 − 200). Expected L3 1200 and L1 restored to 200.
 */
export const PDT_3612_COMPENSATION_BGN = 1200;
export const PDT_3612_SEED_LIABILITY_BGN = 200;
export const PDT_3612_INVOICE_INCLUDING_VAT = 686.72;
export const PDT_3612_BUG_SETTLEMENT_BGN = 1000;

/**
 * PDT-3610 Excel / invoice 109860 (ЕФП-1000075455):
 * two EUR compensations on two PODs, converted to BGN.
 * Documents: customer L 686.72 + 195.58, R 234.70; government L 234.70, R 195.58.
 * Cancel actual: government R 39.12. Excel expected: government L 195.58 and R 234.70.
 */
export const PDT_3610_POSITIVE_EUR = 120;
export const PDT_3610_NEGATIVE_EUR = -100;
export const PDT_3610_POSITIVE_BGN = 234.7;
export const PDT_3610_NEGATIVE_BGN = 195.58;
export const PDT_3610_BUG_REMAINDER_BGN = 39.12;
export const PDT_3610_INVOICE_INCLUDING_VAT = 686.72;

/** Dev FOR_VOLUMES + formula 100, two PODs, MIN profile (observed 110044 / 110162 / 110163). */
export const DEV_VOLUME_INVOICE_AT_PRICE_100 = 71_424_000;

export function volumePriceExpressionForInvoiceTotal(targetInclVat: number): string {
  return String((targetInclVat / DEV_VOLUME_INVOICE_AT_PRICE_100) * 100);
}

export function money(value: number): number {
  return Math.round(value * 100) / 100;
}

export function sameAmount(actual: unknown, expected: number): boolean {
  return Math.abs(toAmountNumber(actual) - expected) < 0.02;
}

export async function resolveEuroCurrencyId(Request: Pdt3368Fx['Request']): Promise<number> {
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

type CurrencyListRow = {
  id?: number;
  mainCurrency?: boolean;
  mainCurrencyStartDate?: string | null;
  status?: string;
};

/**
 * Active main currency from GET /currencies (CurrencyResponse.mainCurrency,
 * mainCurrencyStartDate, status ACTIVE). Same rule as
 * receivable.convert_to_currency: latest mainCurrencyStartDate that is not after today.
 * Dev currently returns leva; Test currently returns euro.
 */
export async function resolveMainCurrencyId(Request: Pdt3368Fx['Request']): Promise<number> {
  const res = await Request.get('currencies', {
    params: { page: 0, size: 50, statuses: 'ACTIVE' },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: CurrencyListRow[] };
  const today = new Date().toISOString().slice(0, 10);
  const mains = (body.content ?? []).filter((row) => {
    if (row.mainCurrency !== true) {
      return false;
    }
    if (row.status && row.status !== 'ACTIVE') {
      return false;
    }
    const start = String(row.mainCurrencyStartDate ?? '').slice(0, 10);
    return start !== '' && start <= today;
  });
  mains.sort((left, right) =>
    String(right.mainCurrencyStartDate).localeCompare(String(left.mainCurrencyStartDate)),
  );
  const id = Number(mains[0]?.id);
  expect(id, `active main currency as of ${today}`).toBeGreaterThan(0);
  return id;
}

export async function resolveLevaCurrencyId(Request: Pdt3368Fx['Request']): Promise<number> {
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

export async function postManualLiability(
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
  const id = typeof raw === 'number' ? raw : Number((raw as { id?: unknown })?.id);
  expect(id, `manual liability id, body=${JSON.stringify(raw)}`).toBeGreaterThan(0);
  fx.Responses.customerLiability.push(id);
  const view = await getCustomerLiability(fx.Request, id);
  expect(String(view['creationType']), 'seed liability is MANUAL').toBe('MANUAL');
  expect(money(toAmountNumber(view.currentAmount)), 'seed liability still open').toBe(opts.initialAmount);
  return id;
}

/** Government recipients have no contract; omit billingGroupId so POST matches Swagger required set. */
export async function postGovernmentReceivable(
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
  const view = await getCustomerReceivable(fx.Request, id);
  expect(money(toAmountNumber(view.currentAmount)), 'government seed receivable still open').toBe(
    opts.initialAmount,
  );
  return id;
}

export async function listLiabilityIds(
  Request: Pdt3368Fx['Request'],
  customerIdentifier: string,
): Promise<number[]> {
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

export async function listReceivableIds(
  Request: Pdt3368Fx['Request'],
  customerIdentifier: string,
): Promise<number[]> {
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

export async function listNewDocuments(
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

export async function postManualLiabilityOffsetting(
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
        const line = row as { amount?: number; current_amount?: number };
        return { ...(row as object), current_amount: line.amount ?? line.current_amount };
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
