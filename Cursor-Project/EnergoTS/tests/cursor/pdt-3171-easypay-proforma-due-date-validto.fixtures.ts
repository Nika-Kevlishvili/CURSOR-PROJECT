/**
 * PDT-3171 helpers — EasyPay init/confirm VALIDTO for proforma + manual liabilities.
 *
 * Reference patterns:
 * - tests/receivableManagement/Payment/onlinePayment.spec.ts (REG-1036)
 * - tests/cursor/pdt-2960-proforma-liability-due-date.fixtures.ts (proforma chain)
 */

import { expect } from '@playwright/test';
import { randomGens } from '../../utils/randomGens';
import { Endpoints } from '../../fixtures/constants/endpoints';
import {
  createPdt2960Goods,
  createPdt2960GoodsOrder,
  patchGoodsOrderLongRunning,
  pollPdt2960LiabilityForInvoice,
  resolveGoodsOrderId,
  runPdt2960ProformaAccountingChain,
  setupPdt2960ReceivablesBase,
  normalizeDateOnly,
} from './pdt-2960-proforma-liability-due-date.fixtures';

export type Pdt3171Fx = {
  Request: any;
  GeneratePayload: any;
  Responses: any;
  Endpoints: typeof Endpoints;
  OnlinePaymentUrl: string;
  receivableValidations?: { paymentValidation: (paymentId: number) => Promise<void> };
};

export const PROFORMA_VALID_TO_DAYS = 30;
export const YYYYMMDD_RE = /^\d{8}$/;

export {
  createPdt2960Goods,
  createPdt2960GoodsOrder,
  pollPdt2960LiabilityForInvoice,
  runPdt2960ProformaAccountingChain,
  setupPdt2960ReceivablesBase,
  resolveGoodsOrderId,
  normalizeDateOnly,
};

/** Local calendar date + offset → yyyyMMdd (payment-api VALIDTO format). */
export function yyyyMmDdFromLocalDate(base: Date = new Date(), plusDays = 0): string {
  const d = new Date(base.getFullYear(), base.getMonth(), base.getDate());
  d.setDate(d.getDate() + plusDays);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}${mm}${dd}`;
}

/** Convert liability dueDate (ISO or dd-mm-yyyy / dd.mm.yyyy) → yyyyMMdd. */
export function dueDateToYyyyMmDd(dueDate: string): string {
  const raw = String(dueDate ?? '').trim();
  if (!raw) return '';
  if (/^\d{8}$/.test(raw)) return raw;
  const iso = raw.split('T')[0];
  if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    return iso.replace(/-/g, '');
  }
  const m = raw.match(/^(\d{2})[-.](\d{2})[-.](\d{4})$/);
  if (m) return `${m[3]}${m[2]}${m[1]}`;
  throw new Error(`Unsupported dueDate format for VALIDTO: ${dueDate}`);
}

export function toCoinAmount(amount: number): number {
  return Math.round(Number(amount) * 100);
}

export async function createLegalCustomer(fx: Pdt3171Fx): Promise<void> {
  const customerPayload = fx.GeneratePayload.customers.customer_legal();
  const res = await fx.Request.post(fx.Endpoints.customer, { data: customerPayload });
  await expect(res).CheckResponse();
  fx.Responses.customer.push(await res.json());
}

export async function createCollectionChannelAndPaymentPackage(fx: Pdt3171Fx): Promise<void> {
  const channelRes = await fx.Request.post(fx.Endpoints.collectionChannel, {
    data: fx.GeneratePayload.receivablesManagement.collection_channel(),
  });
  await expect(channelRes).CheckResponse();
  fx.Responses.collectionChannel.push(await channelRes.json());

  const packageRes = await fx.Request.post(fx.Endpoints.paymentPackage, {
    data: fx.GeneratePayload.receivablesManagement.payment_package(),
  });
  await expect(packageRes).CheckResponse();
  fx.Responses.paymentPackage.push(await packageRes.json());
}

/**
 * Manual liability with known dueDate.
 * @param dueDateDdMmYyyy e.g. '15-09-2026' (matches existing liability generators)
 * @param applicableInterestRateId optional — pass `null` to clear the template default
 *   (`envVariables.interest_rate`) so EasyPay amount_to_pay stays principal-only (surplus path).
 */
export async function createManualLiability(
  fx: Pdt3171Fx,
  opts: {
    initialAmount: number;
    dueDateDdMmYyyy: string;
    applicableInterestRateId?: number | null;
  },
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.customer_liability();
  payload.initialAmount = opts.initialAmount;
  payload.dueDate = opts.dueDateDdMmYyyy;
  payload.occurrenceDate = randomGens.generateTodaysDate('dd-mm-yyyy');
  if (opts.applicableInterestRateId === null) {
    (payload as { applicableInterestRateId: number | null }).applicableInterestRateId = null;
  } else if (opts.applicableInterestRateId != null) {
    payload.applicableInterestRateId = opts.applicableInterestRateId;
  }
  const res = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  await expect(res).CheckResponse();
  const liabilityId = (await res.json()) as number;
  fx.Responses.customerLiability.push(liabilityId);
  return liabilityId;
}

export async function getLiabilityDetail(
  fx: Pdt3171Fx,
  liabilityId: number,
): Promise<{ dueDate: string; currentAmount: number; initialAmount: number }> {
  const res = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
  await expect(res).CheckResponse();
  const body = await res.json();
  return {
    dueDate: normalizeDateOnly(body.dueDate),
    currentAmount: Number(body.currentAmount ?? 0),
    initialAmount: Number(body.initialAmount ?? 0),
  };
}

/**
 * Resolve EasyPay MERCHANTID for init/confirm.
 * Prefer GET system-configurations.easyPayMerchantId when the API exists.
 * Fallbacks when that route is 403/404/empty (common on Dev / Dev2):
 * - payment-api2 (`OnlinePaymentUrl` :9092, phoenix2-dev) → deployed `easypay.merchant.id=Merchant_ID`
 *   (ES: "Merchant not found for ID: 7000005" on phoenix-payment-api2; STATUS 96 with empty body)
 * - classic Dev/Test payment-api (:9091) → `7000005` (application-*.properties)
 */
export async function resolveEasyPayMerchantId(fx: Pdt3171Fx): Promise<string> {
  const res = await fx.Request.get(`${fx.Endpoints.systemConfiguration}`);
  if (res.status() === 200) {
    const body = await res.json();
    const merchantId = String(body.easyPayMerchantId ?? '').trim();
    if (merchantId) return merchantId;
  }
  const epay = String(fx.OnlinePaymentUrl ?? '');
  if (epay.includes('9092')) {
    return 'Merchant_ID';
  }
  return '7000005';
}

/** Serialize EasyPay init/confirm body fields useful when STATUS !== 00. */
export function formatEasyPayStatusDetail(body: any): string {
  return (
    `STATUS=${body?.STATUS} AMOUNT=${body?.AMOUNT ?? 'n/a'} ` +
    `ADDITIONALINFO=${body?.ADDITIONALINFO ?? 'n/a'} ` +
    `DESCRIPTION=${body?.DESCRIPTION ?? 'n/a'} ` +
    `SHORTDESC=${body?.SHORTDESC ?? 'n/a'} ` +
    `LONGDESC=${body?.LONGDESC ?? 'n/a'} ` +
    `body=${JSON.stringify(body)}`
  );
}

/**
 * Locate EasyPay ONLINE channel (payment-api configured name).
 * Does not push onto Responses.collectionChannel — tests may already hold a
 * created channel for payment-package; pass the returned id into setEasyPayCombineLiabilities.
 */
export async function findEasyPayOnlineChannel(fx: Pdt3171Fx): Promise<number> {
  const listRes = await fx.Request.get(
    `${fx.Endpoints.collectionChannel}?page=0&size=25&searchBy=NAME&prompt=EasyPay&collectionChannelType=ONLINE`,
  );
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  const channelId = Number(body.content?.[0]?.id);
  expect(channelId, 'EasyPay ONLINE collection channel must exist').toBeGreaterThan(0);
  return channelId;
}

/** Resolve ShortResponse / nested `{ id }` / bare id from a GET channel field. */
function shortId(value: unknown): number | null {
  if (value == null) return null;
  if (typeof value === 'number') return value;
  if (typeof value === 'object' && value !== null && 'id' in value) {
    const id = (value as { id?: unknown }).id;
    return typeof id === 'number' ? id : id != null ? Number(id) : null;
  }
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

/**
 * Build CollectionChannelBaseRequest from GET /collection-channel/{id} response.
 * Preserves env-specific partner/currency/name/type — only combineLiabilities is overridden.
 * performerId/employee are included only when present (Test EasyPay has employee=null).
 * (Do not use Dev-hardcoded easyPayCollectionChannel() for PUT.)
 */
function collectionChannelPutFromGet(
  current: Record<string, any>,
  combineLiabilities: boolean,
): Record<string, unknown> {
  const employee = current.employee ?? null;
  const performerId =
    shortId(current.performerId) ?? shortId(employee);
  const performerType =
    current.performerType ?? employee?.performerType ?? null;

  const payload: Record<string, unknown> = {
    name: current.name,
    type: current.type,
    collectionPartnerId: shortId(current.collectionPartnerId),
    numberOfIncomeAccount: current.numberOfIncomeAccount,
    currencyId: shortId(current.currencyId),
    customerConditionType:
      current.customerConditionType ?? current.conditionType,
    condition: current.condition ?? null,
    listOfCustomers: current.listOfCustomers ?? null,
    excludeLiabilitiesByPrefix: current.excludeLiabilitiesByPrefix ?? [],
    excludeLiabilitiesByAmount: {
      lessThan:
        current.excludeLiabilitiesByAmount?.lessThan ?? current.lessThan ?? null,
      greaterThan:
        current.excludeLiabilitiesByAmount?.greaterThan ??
        current.greaterThan ??
        null,
    },
    priorityLiabilitiesByPrefix: current.priorityLiabilitiesByPrefix ?? [],
    typeOfFile: current.typeOfFile ?? null,
    bankIds: current.bankIds ?? [],
    globalBank: current.globalBank ?? current.isGlobalBank ?? null,
    dataSendingSchedule: current.dataSendingSchedule ?? null,
    dataReceivingSchedule: current.dataReceivingSchedule ?? null,
    numberOfWorkingDays: current.numberOfWorkingDays ?? null,
    calendarId: shortId(current.calendarId),
    waitingPeriodToleranceInHours:
      current.waitingPeriodToleranceInHours ?? null,
    folderForFileReceiving: current.folderForFileReceiving ?? null,
    folderForFileSending: current.folderForFileSending ?? null,
    emailForFileSending: current.emailForFileSending ?? null,
    combineLiabilities,
  };

  // Only send performer when the channel actually has one (avoids Dev-only IDs on Test).
  if (performerId != null) {
    payload.performerId = performerId;
    if (performerType != null) payload.performerType = performerType;
    if (employee != null) payload.employee = employee;
  }

  return payload;
}

/** Ensure EasyPay channel combineLiabilities matches the TC (env-safe GET→PUT). */
export async function setEasyPayCombineLiabilities(
  fx: Pdt3171Fx,
  combineLiabilities: boolean,
  easyPayChannelId: number,
): Promise<void> {
  const getRes = await fx.Request.get(
    `${fx.Endpoints.collectionChannel}/${easyPayChannelId}`,
  );
  await expect(getRes).CheckResponse();
  const current = await getRes.json();

  if (Boolean(current.combineLiabilities) === combineLiabilities) {
    return;
  }

  const payload = collectionChannelPutFromGet(current, combineLiabilities);
  const putRes = await fx.Request.put(
    `${fx.Endpoints.collectionChannel}/${easyPayChannelId}`,
    { data: payload },
  );
  await expect(putRes).CheckResponse();
}

export async function calculateInitChecksum(
  fx: Pdt3171Fx,
  opts: { customerNumber: string; tid: string; merchantId: string },
): Promise<string> {
  const url =
    `${fx.OnlinePaymentUrl}epay/calculate-check-sum-init` +
    `?IDN=${opts.customerNumber}&TID=${opts.tid}&MERCHANTID=${opts.merchantId}&TYPE=BILLING`;
  const res = await fx.Request.get(url);
  await expect(res).CheckResponse();
  return (await res.text()).trim();
}

export async function callInitPay(
  fx: Pdt3171Fx,
  opts: { customerNumber: string; tid: string; merchantId: string; checksum: string },
): Promise<any> {
  const url =
    `${fx.OnlinePaymentUrl}epay/init-pay` +
    `?IDN=${opts.customerNumber}&TID=${opts.tid}&MERCHANTID=${opts.merchantId}` +
    `&TYPE=BILLING&CHECKSUM=${opts.checksum}`;
  const res = await fx.Request.get(url);
  await expect(res).CheckResponse();
  return res.json();
}

export async function calculateConfirmChecksum(
  fx: Pdt3171Fx,
  opts: {
    date: string;
    customerNumber: string;
    tid: string;
    merchantId: string;
    total: string | number;
  },
): Promise<string> {
  const url =
    `${fx.OnlinePaymentUrl}epay/calculate-check-sum-confirm` +
    `?DATE=${opts.date}&TYPE=BILLING&MERCHANTID=${opts.merchantId}` +
    `&IDN=${opts.customerNumber}&TOTAL=${opts.total}&TID=${opts.tid}`;
  const res = await fx.Request.get(url);
  await expect(res).CheckResponse();
  return (await res.text()).trim();
}

export async function callConfirmPay(
  fx: Pdt3171Fx,
  opts: {
    date: string;
    customerNumber: string;
    tid: string;
    merchantId: string;
    total: string | number;
    checksum: string;
  },
): Promise<any> {
  const url =
    `${fx.OnlinePaymentUrl}epay/confirm-pay` +
    `?DATE=${opts.date}&TYPE=BILLING&MERCHANTID=${opts.merchantId}` +
    `&IDN=${opts.customerNumber}&TOTAL=${opts.total}&TID=${opts.tid}&CHECKSUM=${opts.checksum}`;
  const res = await fx.Request.get(url);
  await expect(res).CheckResponse();
  return res.json();
}

export async function listPaymentsByCustomerIdentifier(
  fx: Pdt3171Fx,
  identifier: string,
): Promise<{ totalElements: number; content: any[] }> {
  const listRes = await fx.Request.post(`${fx.Endpoints.payment}/list`, {
    data: {
      page: 0,
      size: 25,
      prompt: identifier,
      searchFields: 'CUSTOMER_IDENTIFIER',
    },
  });
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  return { totalElements: Number(body.totalElements ?? 0), content: body.content ?? [] };
}

export async function listOpenLiabilitiesForCustomer(
  fx: Pdt3171Fx,
  identifier: string,
): Promise<any[]> {
  const listRes = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC` +
      `&prompt=${identifier}&searchFields=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  const rows = (body.content ?? []) as Array<{ id?: number; currentAmount?: number }>;
  const open: any[] = [];
  for (const row of rows) {
    const id = Number(row.id);
    if (!Number.isFinite(id)) continue;
    const detail = await getLiabilityDetail(fx, id);
    if (detail.currentAmount > 0) {
      open.push({ id, ...detail });
    }
  }
  return open;
}

/** Proforma → accounting → resolve liability; asserts dueDate empty before issue-invoice. */
export async function setupProformaLiabilityChain(fx: Pdt3171Fx): Promise<{
  goodsOrderId: number;
  proformaInvoiceId: number;
  liabilityId: number;
  dueDate: string;
  currentAmount: number;
}> {
  await setupPdt2960ReceivablesBase(fx as any);
  await createPdt2960Goods(fx as any);
  const goodsOrderId = await createPdt2960GoodsOrder(fx as any);
  const proformaInvoiceId = await runPdt2960ProformaAccountingChain(fx as any, goodsOrderId);
  const customerIdentifier = String(fx.Responses.customer[0].identifier);
  const resolved = await pollPdt2960LiabilityForInvoice(
    fx as any,
    customerIdentifier,
    proformaInvoiceId,
  );
  expect(resolved.currentAmount, 'proforma liability must be open').toBeGreaterThan(0);
  expect(
    resolved.dueDate,
    'proforma liability dueDate must be null/empty before issue-invoice',
  ).toBe('');
  return {
    goodsOrderId,
    proformaInvoiceId,
    liabilityId: resolved.liabilityId,
    dueDate: resolved.dueDate,
    currentAmount: resolved.currentAmount,
  };
}

/** After proforma accounting: PATCH issue-invoice and re-read liability dueDate. */
export async function issueInvoiceAndReadLiabilityDueDate(
  fx: Pdt3171Fx,
  goodsOrderId: number,
  liabilityId: number,
): Promise<{ dueDate: string; currentAmount: number }> {
  await patchGoodsOrderLongRunning(
    fx as any,
    `${fx.Endpoints.goodsOrder}/${goodsOrderId}/issue-invoice`,
  );

  const detail = await getLiabilityDetail(fx, liabilityId);
  expect(
    detail.dueDate,
    'After issue-invoice, liability dueDate must be populated (env/data gap if empty)',
  ).toBeTruthy();
  expect(detail.currentAmount, 'liability must remain open after issue-invoice').toBeGreaterThan(0);
  return detail;
}

export function assertValidToNotZero(validTo: unknown, label: string): string {
  const value = String(validTo ?? '');
  expect(value, `${label} VALIDTO must not be "0"`).not.toBe('0');
  expect(value, `${label} VALIDTO must match yyyyMMdd`).toMatch(YYYYMMDD_RE);
  return value;
}
