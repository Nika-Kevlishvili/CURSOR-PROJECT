/**
 * PDT-3319 helpers — GET /customer-indicators/{id} averagePaymentDay.
 *
 * There is NO Phoenix POST to run Customer Indicators. waitForCustomerIndicatorsSnapshot
 * polls GET until averagePaymentDay equals the expected integer, or fails after ~120s
 * (Finding 4: overnight job writes reporting.customer_indicators.average_payment_day).
 * Do not grep logs. Do not call a fake /customer-indicators/job.
 *
 * Snapshot date D = Europe/Sofia **today** as an approximation of the next job date.
 *
 * Swagger (dev): GET /customer-indicators/{id} → CustomerIndicatorsResponse.averagePaymentDay
 * (number). POST /customer-liability required accountingPeriodId, currencyId, customerId,
 * dueDate, initialAmount, occurrenceDate (runtime 201). POST /payment required
 * accountPeriodId (not accountingPeriodId), collectionChannelId, currencyId, customerId,
 * initialAmount, paymentDate, paymentPackageId. POST /payment-package lockStatus UNLOCKED.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3205-copy-easypay-to-virtual-pos.fixtures.ts (createLegalCustomer / CL)
 * - tests/cursor/pdt-3215-accounting-period-dates-and-more.fixtures.ts (AP, package, payment)
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts (RFD / Charge Fee)
 * - tests/cursor/pdt-3379-reconnection-taxes-newest-contract-billing-group.fixtures.ts
 * - tests/cursor/pdt-2960-proforma-liability-due-date.fixtures.ts (proforma, no issue-invoice)
 * - tests/cursor/exp-parity-03-interim-deduction-and-issue-date.fixtures.ts (billing wait)
 * - tests/cursor/reg-566-invoice-correction-board-cases.fixtures.ts (scaleComponent.scaleIds)
 * - tests/cursor/pdt-2891-connected-invoices.fixtures.ts (external CREDIT_NOTE fields)
 */

import { expect } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  asNumber,
  calculateTax,
  collectCustomerInvoices,
  createExecutedRfd,
  createSupplyChain,
  customerIdentifier,
  entityId,
  findLiabilityByInvoiceId,
  getLiability,
  invoicesOfType,
  listingContent,
  nestedId,
  resolveContractBillingGroupId,
  type Pdt3179Fx,
} from './pdt-3179-reconnection-invoice-emails.fixtures';
import {
  createReschedulingInstallment,
  waitForThisTestReminderExecuted,
} from './pdt-3379-reconnection-taxes-newest-contract-billing-group.fixtures';
import {
  createPdt2960Goods,
  createPdt2960GoodsOrder,
  runPdt2960ProformaAccountingChain,
} from './pdt-2960-proforma-liability-due-date.fixtures';

export const CUSTOMER_INDICATORS = 'customer-indicators';
export const PDT_3319_KEY = 'PDT-3319';
export const PDT_3319_TITLE =
  'Customer indicators - Average payment day for electricity bills';

/** Poll until the overnight snapshot column is visible on GET, or fail Finding 4. */
export const SNAPSHOT_POLL_INTERVAL_MS = 10_000;
export const SNAPSHOT_TIMEOUT_MS = 120_000;

export const PDT_3319_FORMULA_TIMEOUT_MS = 8 * 60 * 1000;
export const PDT_3319_BILLING_TIMEOUT_MS = 18 * 60 * 1000;
/** Supply + reminder + RFD POST (~13 min on Dev) + Charge Fee. */
export const PDT_3319_RFD_TIMEOUT_MS = 28 * 60 * 1000;

export type Pdt3319Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type CustomerIndicatorsBody = {
  customerId?: number;
  averagePaymentDay?: number | null;
  [key: string]: unknown;
};

export type PaidManualCl = {
  dueIso: string;
  payIso: string;
  occurrenceIso: string;
  amount: number;
};

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export { entityId, listingContent, nestedId, asNumber, getLiability, customerIdentifier };

/** Europe/Sofia calendar date yyyy-MM-dd — approximation of next Customer Indicators job D. */
export function snapshotDateD(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Sofia' }).format(new Date());
}

export function addSofiaDays(isoYmd: string, days: number): string {
  const [y, m, d] = isoYmd.split('-').map(Number);
  const utc = Date.UTC(y, m - 1, d + days, 12, 0, 0);
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Sofia' }).format(new Date(utc));
}

export function addSofiaMonths(isoYmd: string, months: number): string {
  const [y, m, d] = isoYmd.split('-').map(Number);
  const lastDay = new Date(Date.UTC(y, m - 1 + months + 1, 0)).getUTCDate();
  const day = Math.min(d, lastDay);
  const dt = new Date(Date.UTC(y, m - 1 + months, day, 12, 0, 0));
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Sofia' }).format(dt);
}

/** Payload generators (CL) use dd-MM-yyyy; Swagger format=date is yyyy-MM-dd. */
export function isoToDdMmYyyy(isoYmd: string): string {
  const [y, month, d] = isoYmd.split('-');
  return `${d}-${month}-${y}`;
}

export function toIsoDate(value: unknown): string {
  const raw = String(value ?? '').trim();
  if (!raw) return '';
  const iso = raw.split('T')[0];
  if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) return iso;
  const match = raw.match(/^(\d{2})[-.](\d{2})[-.](\d{4})/);
  if (match) return `${match[3]}-${match[2]}-${match[1]}`;
  return iso;
}

/**
 * JSON null must not become 0 via Number(null). TC-BE-7 asserts 0 after snapshot;
 * TC-BE-22 allows pre-job null as storedBefore.
 */
export function parseAveragePaymentDay(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

export function averagesEqual(left: unknown, right: unknown): boolean {
  const a = parseAveragePaymentDay(left);
  const b = parseAveragePaymentDay(right);
  if (a === null && b === null) return true;
  if (a === null || b === null) return false;
  return a === b;
}

export async function sharedCustomer(
  fx: Pdt3319Fx,
): Promise<{ customerId: number; identifier: string }> {
  const payload = fx.GeneratePayload.customers.customer_legal();
  const res = await fx.Request.post(fx.Endpoints.customer, { data: payload });
  await expect(res).CheckResponse();
  const body = await res.json();
  const customerId = entityId(body);
  const identifier = String(
    body?.identifier ?? body?.customerIdentifier ?? payload.customerIdentifier ?? '',
  );
  fx.Responses.customer.push(
    body && typeof body === 'object'
      ? { ...body, id: customerId, identifier, customerIdentifier: identifier }
      : { id: customerId, identifier, customerIdentifier: identifier },
  );
  expect(customerId, 'POST /customer id').toBeGreaterThan(0);
  expect(identifier, 'customer identifier').toBeTruthy();
  return { customerId, identifier };
}

/** asNumber(undefined) is 0; `0 ?? fallback` never takes the fallback. */
function firstPositiveId(...values: unknown[]): number {
  for (const value of values) {
    if (value === null || value === undefined || value === '') continue;
    const nested = nestedId(value);
    if (nested && nested > 0) return nested;
    if (typeof value === 'object') continue;
    const n = asNumber(value);
    if (n > 0) return n;
  }
  return 0;
}

function scaleCodeNomenclatureId(): number {
  const scaleId = firstPositiveId(envVariables.scales_code, envVariables.scalecode);
  expect(scaleId, 'envVariables.scales_code for BY_SCALES').toBeGreaterThan(0);
  return scaleId;
}

export async function sharedBgnCurrency(fx: Pdt3319Fx): Promise<number> {
  const cached = firstPositiveId(envVariables.currency);
  const res = await fx.Request.get(
    `currencies?page=0&size=50&statuses=ACTIVE&prompt=BGN`,
  );
  await expect(res).CheckResponse();
  const rows = listingContent(await res.json());
  const hit =
    rows.find((row) => {
      const hay = [row.name, row.abbreviation, row.printName, row.fullName, row.code]
        .map((part) => String(part ?? '').toUpperCase())
        .join(' ');
      return hay.includes('BGN');
    }) ?? rows.find((row) => row.mainCurrency === true);
  const currencyId = firstPositiveId(hit, hit?.id, cached);
  expect(currencyId, 'BGN currencyId').toBeGreaterThan(0);
  return currencyId;
}

function periodContainsDate(startRaw: unknown, endRaw: unknown, isoDate: string): boolean {
  const start = toIsoDate(startRaw);
  const end = toIsoDate(endRaw);
  if (!start || !end) return false;
  return start <= isoDate && isoDate <= end;
}

export async function sharedAccountingPeriodForDate(
  fx: Pdt3319Fx,
  isoDate: string,
): Promise<number> {
  const name = `ACCOUNTING${isoDate.slice(0, 4)}${isoDate.slice(5, 7)}`;
  const named = await fx.Request.get(
    `accounting-period?page=0&size=20&prompt=${encodeURIComponent(name)}&searchBy=NAME`,
  );
  if (named.ok()) {
    await expect(named).CheckResponse();
    const namedRows = listingContent(await named.json());
    const namedHit = namedRows.find((row) => String(row.name ?? '') === name) ?? namedRows[0];
    const namedId = asNumber(namedHit?.accountPeriodId ?? namedHit?.id);
    if (namedId > 0) {
      const detail = await fx.Request.get(`accounting-period/${namedId}`);
      if (detail.ok()) {
        const period = await detail.json();
        if (periodContainsDate(period.startDate, period.endDate, isoDate)) {
          return namedId;
        }
      } else {
        return namedId;
      }
    }
  }

  const filtered = await fx.Request.get(
    `accounting-period?page=0&size=100&searchBy=ALL` +
      `&startDateFrom=${isoDate}&startDateTo=${isoDate}` +
      `&endDateFrom=${isoDate}&endDateTo=${isoDate}`,
  );
  if (filtered.ok()) {
    await expect(filtered).CheckResponse();
    const filteredRows = listingContent(await filtered.json());
    for (const row of filteredRows) {
      const id = asNumber(row.accountPeriodId ?? row.id);
      if (id <= 0) continue;
      if (periodContainsDate(row.startDate, row.endDate, isoDate)) return id;
    }
  }

  const list = await fx.Request.get(
    `accounting-period?page=0&size=100&searchBy=ALL`,
  );
  await expect(list).CheckResponse();
  const rows = listingContent(await list.json());
  for (const row of rows) {
    const id = asNumber(row.accountPeriodId ?? row.id);
    if (id <= 0) continue;
    if (periodContainsDate(row.startDate, row.endDate, isoDate)) return id;
    const detail = await fx.Request.get(`accounting-period/${id}`);
    if (!detail.ok()) continue;
    const period = await detail.json();
    if (periodContainsDate(period.startDate, period.endDate, isoDate)) return id;
  }

  throw new Error(
    `No accounting period contains ${isoDate} (D approximation Europe/Sofia). ` +
      `GET /accounting-period has no POST in Swagger — open ACCOUNTING${isoDate.slice(0, 4)}${isoDate.slice(5, 7)} on Dev if missing.`,
  );
}

export async function validatePaymentDate(
  fx: Pdt3319Fx,
  accountingPeriodId: number,
  paymentDateIso: string,
): Promise<{ valid: boolean; message: string | null }> {
  const res = await fx.Request.get(
    `${fx.Endpoints.payment}/validate-payment-date-and-accounting-period` +
      `?accountingPeriodId=${accountingPeriodId}&paymentDate=${paymentDateIso}`,
  );
  if (!res.ok()) {
    return { valid: false, message: `HTTP ${res.status()}` };
  }
  await expect(res).CheckResponse();
  const body = await res.json();
  return {
    valid: Boolean(body.valid),
    message: body.message == null ? null : String(body.message),
  };
}

export async function sharedCollectionChannel(fx: Pdt3319Fx, currencyId: number): Promise<number> {
  const list = await fx.Request.get(
    `${fx.Endpoints.collectionChannel}?page=0&size=50&searchBy=ALL`,
  );
  if (list.ok()) {
    await expect(list).CheckResponse();
    const rows = listingContent(await list.json());
    const hit = rows.find((row) => {
      const status = String(row.status ?? 'ACTIVE').toUpperCase();
      const channelCurrency =
        nestedId(row.currencyId) ?? nestedId(row.currency) ?? asNumber(row.currencyId);
      return status === 'ACTIVE' && (channelCurrency === currencyId || channelCurrency === 0);
    });
    const existing = nestedId(hit) ?? asNumber(hit?.id);
    if (existing > 0) {
      fx.Responses.collectionChannel.push(existing);
      return existing;
    }
  }

  const created = await fx.Request.post(fx.Endpoints.collectionChannel, {
    data: fx.GeneratePayload.receivablesManagement.collection_channel(),
  });
  await expect(created).CheckResponse();
  const channelId = entityId(await created.json());
  fx.Responses.collectionChannel.push(channelId);
  return channelId;
}

export async function sharedUnlockedPaymentPackage(
  fx: Pdt3319Fx,
  opts: { accountingPeriodId: number; channelId: number; paymentDateIso: string },
): Promise<number> {
  const payload = {
    accountingPeriodId: opts.accountingPeriodId,
    channelId: opts.channelId,
    lockStatus: 'UNLOCKED' as const,
    paymentDate: opts.paymentDateIso,
    type: 'OFFLINE' as const,
  };
  const res = await fx.Request.post(fx.Endpoints.paymentPackage, { data: payload });
  await expect(res).CheckResponse();
  const paymentPackageId = entityId(await res.json());
  fx.Responses.paymentPackage.push(paymentPackageId);
  return paymentPackageId;
}

export async function sharedManualLiability(
  fx: Pdt3319Fx,
  opts: {
    customerId: number;
    currencyId: number;
    accountingPeriodId: number;
    dueIso: string;
    occurrenceIso: string;
    initialAmount: number;
    billingGroupId?: number;
    applicableInterestRateId?: number | null;
  },
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.customer_liability();
  payload.customerId = opts.customerId;
  payload.currencyId = opts.currencyId;
  payload.accountingPeriodId = opts.accountingPeriodId;
  payload.dueDate = isoToDdMmYyyy(opts.dueIso);
  payload.occurrenceDate = isoToDdMmYyyy(opts.occurrenceIso);
  payload.initialAmount = opts.initialAmount;
  payload.applicableInterestRateId =
    opts.applicableInterestRateId === undefined ? null : opts.applicableInterestRateId;
  if (opts.billingGroupId) payload.billingGroupId = opts.billingGroupId;

  const res = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  const status = res.status();
  expect(
    status === 200 || status === 201 || res.ok(),
    `POST /customer-liability expected 200 or 201, got ${status}`,
  ).toBe(true);
  if (res.ok()) {
    await expect(res).CheckResponse();
  }
  const clId = entityId(await res.json());
  fx.Responses.customerLiability.push(clId);

  const detail = await getLiability(fx, clId);
  expect(String(detail.creationType ?? ''), `CL ${clId} creationType`).toBe('MANUAL');
  expect(String(detail.status ?? '')).toBe('ACTIVE');
  return clId;
}

export async function runPaymentJob(fx: Pdt3319Fx): Promise<void> {
  const res = await fx.Request.post(`${fx.Endpoints.payment}/job`);
  const status = res.status();
  expect(
    status === 200 || status === 206 || res.ok(),
    `POST /payment/job expected 200 or 206, got ${status}`,
  ).toBe(true);
  if (res.ok()) {
    await expect(res).CheckResponse();
  }
}

export async function createPayment(
  fx: Pdt3319Fx,
  opts: {
    customerId: number;
    currencyId: number;
    collectionChannelId: number;
    paymentPackageId: number;
    accountPeriodId: number;
    paymentDateIso: string;
    initialAmount: number;
  },
): Promise<number> {
  const payload = {
    accountPeriodId: opts.accountPeriodId,
    collectionChannelId: opts.collectionChannelId,
    currencyId: opts.currencyId,
    customerId: opts.customerId,
    initialAmount: opts.initialAmount,
    paymentDate: opts.paymentDateIso,
    paymentPackageId: opts.paymentPackageId,
    blockedForOffsetting: false,
  };
  const res = await fx.Request.post(fx.Endpoints.payment, { data: payload });
  await expect(res).CheckResponse();
  const paymentId = entityId(await res.json());
  fx.Responses.payment.push(paymentId);
  return paymentId;
}

export async function waitUntilFullyOffset(
  fx: Pdt3319Fx,
  liabilityId: number,
  expectedOffsetIso: string,
): Promise<Record<string, unknown>> {
  const deadline = Date.now() + 90_000;
  let last: Record<string, unknown> = {};
  while (Date.now() < deadline) {
    last = await getLiability(fx, liabilityId);
    const current = Number(last.currentAmount ?? -1);
    const offsetIso = toIsoDate(last.fullOffsetDate);
    if (current === 0 && offsetIso === expectedOffsetIso) return last;
    await sleep(2000);
  }
  throw new Error(
    `CL ${liabilityId} not fully offset: currentAmount=${last.currentAmount} ` +
      `fullOffsetDate=${last.fullOffsetDate} expected ${expectedOffsetIso}`,
  );
}

export async function payAndOffsetLiability(
  fx: Pdt3319Fx,
  opts: {
    customerId: number;
    currencyId: number;
    collectionChannelId: number;
    paymentDateIso: string;
    amount: number;
    liabilityId: number;
    runJob?: boolean;
  },
): Promise<number> {
  const accountPeriodId = await sharedAccountingPeriodForDate(fx, opts.paymentDateIso);
  const dateCheck = await validatePaymentDate(fx, accountPeriodId, opts.paymentDateIso);
  expect(
    dateCheck.valid,
    `GET /payment/validate-payment-date-and-accounting-period valid=true for ${opts.paymentDateIso} (AP ${accountPeriodId}): ${dateCheck.message ?? ''}`,
  ).toBe(true);
  const paymentPackageId = await sharedUnlockedPaymentPackage(fx, {
    accountingPeriodId: accountPeriodId,
    channelId: opts.collectionChannelId,
    paymentDateIso: opts.paymentDateIso,
  });
  const paymentId = await createPayment(fx, {
    customerId: opts.customerId,
    currencyId: opts.currencyId,
    collectionChannelId: opts.collectionChannelId,
    paymentPackageId,
    accountPeriodId,
    paymentDateIso: opts.paymentDateIso,
    initialAmount: opts.amount,
  });
  if (opts.runJob !== false) {
    await runPaymentJob(fx);
    await waitUntilFullyOffset(fx, opts.liabilityId, opts.paymentDateIso);
  }
  return paymentId;
}

export async function listCustomerLiabilities(
  fx: Pdt3319Fx,
  identifier: string,
): Promise<Record<string, unknown>[]> {
  const res = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC` +
      `&prompt=${encodeURIComponent(identifier)}&searchFields=CUSTOMER`,
  );
  const status = res.status();
  expect(
    status === 200 || status === 206 || res.ok(),
    `GET /customer-liability/list expected 200 or 206, got ${status}`,
  ).toBe(true);
  if (res.ok()) await expect(res).CheckResponse();
  return listingContent(await res.json());
}

export async function getCustomerIndicators(
  fx: Pdt3319Fx,
  customerId: number,
): Promise<{ status: number; body: CustomerIndicatorsBody }> {
  const res = await fx.Request.get(`${CUSTOMER_INDICATORS}/${customerId}`);
  await expect(res).CheckResponse();
  expect(res.status(), 'GET /customer-indicators/{id}').toBe(200);
  const body = (await res.json()) as CustomerIndicatorsBody;
  return { status: res.status(), body };
}

/**
 * Poll GET /customer-indicators/{id} until HTTP 200 and averagePaymentDay equals expected.
 * On timeout, fail with Finding 4 (overnight job has not written the stored column).
 * Null is not treated as 0.
 */
export async function waitForCustomerIndicatorsSnapshot(
  fx: Pdt3319Fx,
  customerId: number,
  expectedAverage: number,
  opts?: { timeoutMs?: number },
): Promise<CustomerIndicatorsBody> {
  const timeoutMs = opts?.timeoutMs ?? SNAPSHOT_TIMEOUT_MS;
  const deadline = Date.now() + timeoutMs;
  let lastStatus = 0;
  let lastAverage: number | null | undefined;
  let lastBody: CustomerIndicatorsBody = {};

  while (Date.now() < deadline) {
    const res = await fx.Request.get(`${CUSTOMER_INDICATORS}/${customerId}`);
    lastStatus = res.status();
    if (lastStatus === 200) {
      lastBody = (await res.json()) as CustomerIndicatorsBody;
      lastAverage = parseAveragePaymentDay(lastBody.averagePaymentDay);
      if (lastAverage !== null && lastAverage === expectedAverage) {
        return lastBody;
      }
    }
    await sleep(SNAPSHOT_POLL_INTERVAL_MS);
  }

  throw new Error(
    `Overnight Customer Indicators snapshot job has not written ` +
      `reporting.customer_indicators.average_payment_day for customerId=${customerId} ` +
      `(Finding 4 — no Phoenix POST trigger). Expected averagePaymentDay=${expectedAverage} ` +
      `within ${timeoutMs / 1000}s (poll ${SNAPSHOT_POLL_INTERVAL_MS / 1000}s). ` +
      `Last HTTP=${lastStatus} averagePaymentDay=${String(lastAverage)}. ` +
      `Population tests fail same-day until the overnight job runs; re-run after the job.`,
  );
}

export async function assertAveragePaymentDay(
  body: CustomerIndicatorsBody,
  customerId: number,
  expected: number,
): Promise<void> {
  expect(Number(body.customerId), 'CustomerIndicatorsResponse.customerId').toBe(customerId);
  const actual = parseAveragePaymentDay(body.averagePaymentDay);
  expect(
    actual,
    `averagePaymentDay must be numeric ${expected}, not null (GET does not recalculate)`,
  ).not.toBeNull();
  expect(actual, 'averagePaymentDay').toBe(expected);
}

export async function prepareManualReceivablesBase(
  fx: Pdt3319Fx,
): Promise<{ customerId: number; identifier: string; currencyId: number; channelId: number; d: string }> {
  const { customerId, identifier } = await sharedCustomer(fx);
  const currencyId = await sharedBgnCurrency(fx);
  const channelId = await sharedCollectionChannel(fx, currencyId);
  return { customerId, identifier, currencyId, channelId, d: snapshotDateD() };
}

export async function createPaidManualCls(
  fx: Pdt3319Fx,
  cls: PaidManualCl[],
): Promise<{ customerId: number; identifier: string; clIds: number[] }> {
  const { customerId, identifier, currencyId, channelId, d } = await prepareManualReceivablesBase(fx);
  void d;
  const clIds: number[] = [];
  for (const row of cls) {
    const apDue = await sharedAccountingPeriodForDate(fx, row.dueIso);
    const clId = await sharedManualLiability(fx, {
      customerId,
      currencyId,
      accountingPeriodId: apDue,
      dueIso: row.dueIso,
      occurrenceIso: row.occurrenceIso,
      initialAmount: row.amount,
    });
    clIds.push(clId);
    await payAndOffsetLiability(fx, {
      customerId,
      currencyId,
      collectionChannelId: channelId,
      paymentDateIso: row.payIso,
      amount: row.amount,
      liabilityId: clId,
      runJob: false,
    });
  }
  await runPaymentJob(fx);
  for (let i = 0; i < cls.length; i++) {
    await waitUntilFullyOffset(fx, clIds[i], cls[i].payIso);
  }
  return { customerId, identifier, clIds };
}

export async function createUnpaidManualCl(
  fx: Pdt3319Fx,
  opts: { dueIso: string; occurrenceIso: string; amount: number },
): Promise<{ customerId: number; identifier: string; clId: number }> {
  const { customerId, identifier, currencyId } = await prepareManualReceivablesBase(fx);
  const apDue = await sharedAccountingPeriodForDate(fx, opts.dueIso);
  const clId = await sharedManualLiability(fx, {
    customerId,
    currencyId,
    accountingPeriodId: apDue,
    dueIso: opts.dueIso,
    occurrenceIso: opts.occurrenceIso,
    initialAmount: opts.amount,
  });
  const detail = await getLiability(fx, clId);
  expect(Number(detail.currentAmount)).toBe(opts.amount);
  expect(detail.fullOffsetDate == null || detail.fullOffsetDate === '').toBe(true);
  const listed = await listCustomerLiabilities(fx, identifier);
  expect(listed.length, 'GET /customer-liability/list (200/206) has the unpaid CL').toBeGreaterThan(0);
  return { customerId, identifier, clId };
}

export async function deleteCustomerLiability(fx: Pdt3319Fx, clId: number): Promise<void> {
  const res = await fx.Request.delete(`${fx.Endpoints.customerLiability}/${clId}`);
  await expect(res).CheckResponse();
  const detail = await getLiability(fx, clId);
  expect(String(detail.status)).toBe('DELETED');
}

export async function changeLiabilityDueDate(
  fx: Pdt3319Fx,
  clId: number,
  dueIso: string,
): Promise<void> {
  const res = await fx.Request.put(
    `${fx.Endpoints.customerLiability}/${clId}/due-date-change?dueDate=${dueIso}`,
  );
  await expect(res).CheckResponse();
}

async function customerDetailAndCommunication(
  fx: Pdt3319Fx,
  customerId: number,
): Promise<{ customerDetailId: number; communicationId: number }> {
  const res = await fx.Request.get(`${fx.Endpoints.customer}/${customerId}`);
  await expect(res).CheckResponse();
  const body = await res.json();
  const customerDetailId = asNumber(body.lastCustomerDetailId ?? body.customerDetailsId);
  const communicationId = nestedId(body.communicationData?.[0]) ?? asNumber(body.communicationData?.[0]?.id);
  expect(customerDetailId, 'customerDetailId').toBeGreaterThan(0);
  expect(communicationId, 'invoiceCommunicationDataId').toBeGreaterThan(0);
  return { customerDetailId, communicationId };
}

export async function createVolumesSupplyChain(fx: Pdt3319Fx): Promise<{
  customerId: number;
  identifier: string;
  contractId: number;
  podId: number;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const scaleId = scaleCodeNomenclatureId();
  const scalePcPayload = GeneratePayload.productAndServices.scaleComponent();
  scalePcPayload.applicationModelRequest.volumesByScaleRequest.scaleIds = [scaleId];
  const scalePc = await Request.post(Endpoints.priceComponent, {
    data: scalePcPayload,
  });
  await expect(scalePc).CheckResponse();
  Responses.priceComponent.push(await scalePc.json());

  const productPayload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.priceComponentIds = [entityId(Responses.priceComponent[0])];
  productPayload.interimAdvancePayments = [];
  productPayload.interimAdvancePaymentGroups = [];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const { customerId, identifier } = await sharedCustomer(fx);

  const pod = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
  await expect(pod).CheckResponse();
  const podBody = await pod.json();
  Responses.pod.push(podBody);
  const podId = entityId(podBody);

  const meterPayload = GeneratePayload.pointsOfDelivery.meters() as Record<string, unknown>;
  meterPayload.podId = podId;
  meterPayload.gridOperatorId = envVariables.grid_operator;
  meterPayload.meterScales = [scaleId];
  const meter = await Request.post(Endpoints.meters, { data: meterPayload });
  await expect(meter).CheckResponse();
  Responses.meters.push(entityId(await meter.json()));

  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
  (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
  const monthly = asNumber(podBody.estimatedMonthlyAvgConsumption) || 1;
  contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = (monthly * 12) / 1000;
  const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(contract).CheckResponse();
  const contractBody = await contract.json();
  Responses.productContract.push(contractBody);
  const contractId = entityId(contractBody);

  const activation = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
  });
  await expect(activation).CheckResponse();

  return { customerId, identifier, contractId, podId };
}

export async function postBillingByScales(
  fx: Pdt3319Fx,
  opts?: { correction?: boolean; invoiceNumber?: string; inflateVolumes?: boolean },
): Promise<void> {
  const payload = await fx.GeneratePayload.energyData.scaleCode(0);
  if (opts?.correction) {
    payload.correction = true;
    if (opts.invoiceNumber) payload.invoiceNumber = opts.invoiceNumber;
    payload.invoiceCorrection = `PDT3319-${opts.invoiceNumber ?? 'CORR'}`;
  }
  if (opts?.inflateVolumes) {
    const row = payload.billingByScalesTableCreateRequests?.[0];
    if (row) {
      const oldNew = Number(row.newMeterReading ?? 5);
      row.newMeterReading = String(oldNew + 20);
      row.difference = String(Number(row.newMeterReading) - Number(row.oldMeterReading ?? 0));
      row.totalVolumes = String(Number(row.difference) * Number(row.multiplier ?? 1));
    }
  }
  const res = await fx.Request.post(fx.Endpoints.dataByScales, { data: payload });
  await expect(res).CheckResponse();
}

export async function runBillingToReal(
  fx: Pdt3319Fx,
  payload: Record<string, unknown>,
  expectedInvoices = 1,
): Promise<number> {
  const res = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const billingRunId = entityId(await res.json());
  fx.Responses.billingRun.push(billingRunId);
  await fx.GeneratePayload.billing.waitForInvoiceGeneration(
    true,
    true,
    expectedInvoices,
    fx.Responses.billingRun.length - 1,
  );
  return billingRunId;
}

export async function completeStandardVolumesBilling(fx: Pdt3319Fx): Promise<{
  customerId: number;
  identifier: string;
  invoiceId: number;
  invoice: Record<string, unknown>;
  mainCl: Record<string, unknown>;
  extraCls: Record<string, unknown>[];
}> {
  const { customerId, identifier } = await createVolumesSupplyChain(fx);
  await postBillingByScales(fx);
  const billingPayload = await fx.GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  billingPayload.commonParameters.sendingAnInvoice = 'EMAIL';
  await runBillingToReal(fx, billingPayload, 1);

  const invoices = await collectCustomerInvoices(fx as unknown as Pdt3179Fx);
  const standard = invoicesOfType(invoices, 'STANDARD');
  expect(standard.length, 'STANDARD invoice from STANDARD_BILLING').toBeGreaterThan(0);
  const invoice = standard[standard.length - 1];
  const invoiceId = entityId(invoice);
  fx.Responses.invoice.push(invoiceId);
  expect(String(invoice.invoiceType)).toBe('STANDARD');
  expect(['INVOICE', 'DEBIT_NOTE']).toContain(String(invoice.invoiceDocumentType));
  expect(String(invoice.invoiceStatus)).toBe('REAL');

  const totalInclVat = Number(invoice.totalAmountIncludingVat);
  const rows = await listCustomerLiabilities(fx, identifier);
  const details: Record<string, unknown>[] = [];
  for (const row of rows) {
    const id = nestedId(row.id) ?? nestedId(row);
    if (!id) continue;
    details.push(await getLiability(fx, id));
  }
  const linked = details.filter((cl) => nestedId(cl.invoiceResponse) === invoiceId);
  const mainCl =
    linked.find((cl) => Math.abs(Number(cl.initialAmount) - totalInclVat) < 0.05) ??
    linked[0];
  expect(mainCl, 'main electricity CL for STANDARD invoice').toBeTruthy();
  const extraCls = linked.filter((cl) => entityId(cl) !== entityId(mainCl!));
  return { customerId, identifier, invoiceId, invoice, mainCl: mainCl!, extraCls };
}

export async function payLiabilityLagDays(
  fx: Pdt3319Fx,
  opts: {
    customerId: number;
    cl: Record<string, unknown>;
    lagDays: number;
  },
): Promise<void> {
  const currencyId = await sharedBgnCurrency(fx);
  const channelId = await sharedCollectionChannel(fx, currencyId);
  const d = snapshotDateD();
  const clId = entityId(opts.cl);
  let dueIso = toIsoDate(opts.cl.dueDate);
  let payIso = addSofiaDays(dueIso, opts.lagDays);
  // Fresh billing due dates are often today or in the future; due+lag must stay <= D.
  if (!dueIso || payIso > d) {
    dueIso = addSofiaDays(d, -opts.lagDays);
    await changeLiabilityDueDate(fx, clId, dueIso);
    payIso = d;
  }
  const refreshed = await getLiability(fx, clId);
  const amount = Number(refreshed.currentAmount ?? refreshed.initialAmount ?? opts.cl.initialAmount);
  await payAndOffsetLiability(fx, {
    customerId: opts.customerId,
    currencyId,
    collectionChannelId: channelId,
    paymentDateIso: payIso,
    amount,
    liabilityId: clId,
  });
}

export async function completeInterimAdvanceBilling(fx: Pdt3319Fx): Promise<{
  customerId: number;
  cl: Record<string, unknown>;
  invoice: Record<string, unknown>;
}> {
  const supply = await createSupplyChain(fx as unknown as Pdt3179Fx);
  const currencyId = await sharedBgnCurrency(fx);
  const { customerDetailId, communicationId } = await customerDetailAndCommunication(
    fx,
    supply.customerId,
  );
  const payload = fx.GeneratePayload.billing.Manualinterim() as Record<string, unknown>;
  const common = (payload.commonParameters ?? {}) as Record<string, unknown>;
  common.sendingAnInvoice = 'EMAIL';
  payload.commonParameters = common;
  const contractGet = await fx.Request.get(
    `${fx.Endpoints.productContract}/${supply.contractId}?version=1`,
  );
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const billingGroupId = resolveContractBillingGroupId(contractBody);
  expect(billingGroupId, 'manual interim billingGroupIds').toBeGreaterThan(0);
  const interim = (payload.interimAndAdvancePaymentParameters ?? {}) as Record<string, unknown>;
  interim.amountExcludingVat = 100.0;
  interim.basisForIssuing = 'PDT-3319-TC-BE-9';
  interim.currencyId = currencyId;
  interim.customerDetailId = customerDetailId;
  interim.invoiceCommunicationDataId = communicationId;
  interim.deductionFrom = 'FIRST_INVOICE_FOR_SAME_PERIOD';
  interim.issuedSeparateInvoices = ['INVOICE_ONE'];
  interim.issuingForTheMonthToCurrent = 'ZERO';
  // PDT-2872: prefixType must be null when contractId is set.
  interim.prefixType = null;
  interim.applicableInterestRateId = firstPositiveId(
    envVariables.interest_rate,
    envVariables.base_interest_rate,
  );
  expect(interim.applicableInterestRateId, 'interim applicableInterestRateId').toBeGreaterThan(0);
  interim.applicableInterestRateManual = true;
  interim.contractType = 'PRODUCT_CONTRACT';
  interim.contractId = supply.contractId;
  interim.billingGroupIds = [billingGroupId];
  payload.interimAndAdvancePaymentParameters = interim;
  payload.billingType = 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT';
  await runBillingToReal(fx, payload, 1);

  const invoices = await collectCustomerInvoices(fx as unknown as Pdt3179Fx);
  const interimInvoices = invoicesOfType(invoices, 'INTERIM_AND_ADVANCE_PAYMENT');
  expect(interimInvoices.length, 'INTERIM_AND_ADVANCE_PAYMENT invoice').toBeGreaterThan(0);
  const invoice = interimInvoices[interimInvoices.length - 1];
  const cl = await findLiabilityByInvoiceId(fx as unknown as Pdt3179Fx, entityId(invoice));
  expect(cl, 'interim invoice CL').toBeTruthy();
  return { customerId: supply.customerId, cl: cl!, invoice };
}

export async function completeCorrectionBilling(fx: Pdt3319Fx): Promise<{
  customerId: number;
  sourceCl: Record<string, unknown>;
  correctionCl: Record<string, unknown>;
}> {
  const billed = await completeStandardVolumesBilling(fx);
  const sourceInvoiceNumber = String(billed.invoice.invoiceNumber ?? '');
  expect(sourceInvoiceNumber, 'source STANDARD invoiceNumber').toBeTruthy();

  await postBillingByScales(fx, {
    correction: true,
    invoiceNumber: sourceInvoiceNumber,
    inflateVolumes: true,
  });
  const correctionPayload = await fx.GeneratePayload.billing.correctionBilling(
    fx.Responses.invoice.length - 1,
    false,
    true,
  );
  correctionPayload.commonParameters.sendingAnInvoice = 'EMAIL';
  correctionPayload.invoiceCorrectionParameters.volumeChange = true;
  correctionPayload.invoiceCorrectionParameters.priceChange = false;
  await runBillingToReal(fx, correctionPayload as Record<string, unknown>, 1);

  const invoices = await collectCustomerInvoices(fx as unknown as Pdt3179Fx);
  const corrections = invoicesOfType(invoices, 'CORRECTION');
  expect(corrections.length, 'CORRECTION invoice').toBeGreaterThan(0);
  const correctionInvoice = corrections[corrections.length - 1];
  const correctionCl = await findLiabilityByInvoiceId(
    fx as unknown as Pdt3179Fx,
    entityId(correctionInvoice),
  );
  expect(correctionCl, 'correction CL').toBeTruthy();
  return {
    customerId: billed.customerId,
    sourceCl: billed.mainCl,
    correctionCl: correctionCl!,
  };
}

export async function completePenaltyAction(fx: Pdt3319Fx): Promise<{
  customerId: number;
  cl: Record<string, unknown>;
}> {
  const supply = await createSupplyChain(fx as unknown as Pdt3179Fx);
  const currencyId = await sharedBgnCurrency(fx);
  const d = snapshotDateD();

  const penaltyRes = await fx.Request.post(fx.Endpoints.penalty, {
    data: fx.GeneratePayload.productAndServices.penalty(),
  });
  await expect(penaltyRes).CheckResponse();
  fx.Responses.penalty.push(await penaltyRes.json());

  const typesRes = await fx.Request.get('action-types?page=0&size=50&statuses=ACTIVE');
  await expect(typesRes).CheckResponse();
  const types = listingContent(await typesRes.json());
  const actionTypeId =
    nestedId(types.find((row) => String(row.status ?? 'ACTIVE') === 'ACTIVE')) ??
    nestedId(types[0]) ??
    asNumber(types[0]?.id);
  expect(actionTypeId, 'actionTypeId').toBeGreaterThan(0);

  const actionPayload = {
    actionTypeId,
    contractId: supply.contractId,
    contractType: 'PRODUCT_CONTRACT',
    customerId: supply.customerId,
    dontAllowAutomaticPenaltyClaim: false,
    executionDate: addSofiaDays(d, -20),
    noticeReceivingDate: addSofiaDays(d, -21),
    penaltyPayer: 'CUSTOMER',
    withoutPenalty: false,
    withoutAutomaticTermination: true,
    penaltyClaimAmount: 50.0,
    penaltyClaimAmountCurrencyId: currencyId,
    penaltyId: entityId(fx.Responses.penalty[0]),
    terminationId: null,
    files: [],
    pods: [],
  };
  const actionRes = await fx.Request.post(fx.Endpoints.action, { data: actionPayload });
  await expect(actionRes).CheckResponse();
  const actionId = entityId(await actionRes.json());
  fx.Responses.action.push(actionId);

  const claim = await fx.Request.post(`${fx.Endpoints.action}/${actionId}/penalty/claim`);
  await expect(claim).CheckResponse();

  const rows = await listCustomerLiabilities(fx, customerIdentifier(fx.Responses));
  let actionCl: Record<string, unknown> | undefined;
  for (const row of rows) {
    const id = nestedId(row.id) ?? nestedId(row);
    if (!id) continue;
    const detail = await getLiability(fx, id);
    if (
      String(detail.outgoingDocumentType) === 'ACTION' &&
      nestedId(detail.actionShortResponse) === actionId
    ) {
      actionCl = detail;
      break;
    }
  }
  expect(actionCl, 'ACTION penalty CL').toBeTruthy();
  expect(String(actionCl!.creationType)).toBe('AUTOMATIC');
  return { customerId: supply.customerId, cl: actionCl! };
}

export async function completeLpfOnlyOutOfWindowSource(fx: Pdt3319Fx): Promise<{ customerId: number }> {
  const { customerId, identifier, currencyId } = await prepareManualReceivablesBase(fx);
  const d = snapshotDateD();
  const oldDue = addSofiaMonths(d, -13);
  const ap = await sharedAccountingPeriodForDate(fx, oldDue);

  const interestRes = await fx.Request.post(fx.Endpoints.interestRate, {
    data: fx.GeneratePayload.receivablesManagement.dailyInterestRate(),
  });
  await expect(interestRes).CheckResponse();
  const interestId = entityId(await interestRes.json());
  fx.Responses.interestRate.push(interestId);

  await sharedManualLiability(fx, {
    customerId,
    currencyId,
    accountingPeriodId: ap,
    dueIso: oldDue,
    occurrenceIso: addSofiaDays(oldDue, -5),
    initialAmount: 90.0,
    applicableInterestRateId: interestId,
  });

  // PDT-2459: unpaid overdue + POST /latePaymentFine/job (calculate/{id} returned 400 on Dev).
  // Source due stays D-13 months so it is outside the 12-month average window.
  const job = await fx.Request.post(`${fx.Endpoints.latePaymentFine}/job`, {
    timeout: 2 * 60 * 1000,
  });
  const jobStatus = job.status();
  expect(
    jobStatus === 200 || jobStatus === 206 || job.ok(),
    `POST /latePaymentFine/job expected 200/206, got ${jobStatus}`,
  ).toBe(true);

  await expect
    .poll(
      async () => {
        const rows = await listCustomerLiabilities(fx, identifier);
        for (const row of rows) {
          const id = nestedId(row.id) ?? nestedId(row);
          if (!id) continue;
          const detail = await getLiability(fx, id);
          if (String(detail.outgoingDocumentType) === 'LATE_PAYMENT_FINE') {
            fx.Responses.latePaymentFine.push(id);
            return true;
          }
        }
        return false;
      },
      { timeout: 90_000, intervals: [3000, 5000], message: 'LPF CL not created' },
    )
    .toBe(true);

  return { customerId };
}

export async function completeDepositOnly(fx: Pdt3319Fx): Promise<{ customerId: number }> {
  const { customerId, currencyId } = await prepareManualReceivablesBase(fx);
  const d = snapshotDateD();
  const payload = fx.GeneratePayload.receivablesManagement.deposit();
  payload.customerId = customerId;
  payload.currencyId = currencyId;
  payload.initialAmount = 500.0;
  payload.paymentDeadline = isoToDdMmYyyy(addSofiaDays(d, 30));
  payload.paymentDeadlineAfterWithdrawalRequest = {
    calendarType: 'CALENDAR_DAYS',
    value: 10,
    calendarId: envVariables.calendar.id,
    excludeWeekends: false,
    excludeHolidays: false,
    dueDateChange: null,
    name: 'PDT-3319-TC-BE-14',
  };
  const res = await fx.Request.post(fx.Endpoints.deposit, { data: payload });
  await expect(res).CheckResponse();
  fx.Responses.deposit.push(await res.json());

  const job = await fx.Request.post(`${fx.Endpoints.deposit}/job`);
  const jobStatus = job.status();
  expect(
    jobStatus === 200 || jobStatus === 206 || job.ok(),
    `POST /deposit/job expected 200/206, got ${jobStatus}`,
  ).toBe(true);

  await expect
    .poll(
      async () => {
        const rows = await listCustomerLiabilities(fx, customerIdentifier(fx.Responses));
        for (const row of rows) {
          const id = nestedId(row.id) ?? nestedId(row);
          if (!id) continue;
          const detail = await getLiability(fx, id);
          if (String(detail.outgoingDocumentType) === 'DEPOSIT') return true;
        }
        return false;
      },
      { timeout: 90_000, intervals: [3000, 5000], message: 'DEPOSIT CL not created' },
    )
    .toBe(true);

  return { customerId };
}

export async function completeProformaOnly(fx: Pdt3319Fx): Promise<{ customerId: number }> {
  const { customerId } = await sharedCustomer(fx);
  await createPdt2960Goods(fx as never);
  const goodsOrderId = await createPdt2960GoodsOrder(fx as never);
  // issue-proforma-invoice + start-generating + start-accounting. Do not issue-invoice.
  await runPdt2960ProformaAccountingChain(fx as never, goodsOrderId);
  return { customerId };
}

export async function completeReconnectionInvoiceOnly(fx: Pdt3319Fx): Promise<{
  customerId: number;
  invoice: Record<string, unknown>;
}> {
  const supply = await createSupplyChain(fx as unknown as Pdt3179Fx);
  const currencyId = await sharedBgnCurrency(fx);
  const d = snapshotDateD();
  const oldDue = addSofiaMonths(d, -13);
  const apOld = await sharedAccountingPeriodForDate(fx, oldDue);

  const contractGet = await fx.Request.get(
    `${fx.Endpoints.productContract}/${supply.contractId}?version=1`,
  );
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const billingGroupId = resolveContractBillingGroupId(contractBody);

  await sharedManualLiability(fx, {
    customerId: supply.customerId,
    currencyId,
    accountingPeriodId: apOld,
    dueIso: oldDue,
    occurrenceIso: oldDue,
    initialAmount: 200.0,
    billingGroupId,
  });

  const reminderPayload = fx.GeneratePayload.receivablesManagement.reminderForDisconnection();
  reminderPayload.customerList = customerIdentifier(fx.Responses);
  reminderPayload.customerFilterType = 'INCLUDED';
  reminderPayload.liabilitiesMaxDueDate = oldDue;
  reminderPayload.disconnectionDate = addSofiaDays(d, 14);
  reminderPayload.documentTemplateId = null;
  const reminderRes = await fx.Request.post(fx.Endpoints.reminderForDisconnection, {
    data: reminderPayload,
  });
  await expect(reminderRes).CheckResponse();
  const reminderId = entityId(await reminderRes.json());
  fx.Responses.reminderForDisconnection.push(reminderId);

  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  await fx.Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${reminderId}` +
      `&hour=${georgianTime.getHours()}&minute=${georgianTime.getMinutes()}`,
    { timeout: 5_000 },
  );
  await waitForThisTestReminderExecuted(fx as never);

  const requestId = await createExecutedRfd(fx as unknown as Pdt3179Fx);
  const calcRes = await calculateTax(fx as unknown as Pdt3179Fx, requestId);
  await expect(calcRes).CheckResponse();

  const invoices = await collectCustomerInvoices(fx as unknown as Pdt3179Fx);
  const reconnection = invoicesOfType(invoices, 'RECONNECTION');
  expect(reconnection.length, 'RECONNECTION invoice after Charge Fee').toBeGreaterThan(0);
  const invoice = reconnection[reconnection.length - 1];
  expect(String(invoice.invoiceType)).toBe('RECONNECTION');
  expect(String(invoice.invoiceDocumentType)).toBe('INVOICE');
  return { customerId: supply.customerId, invoice };
}

export async function completeTaxExtraOnly(fx: Pdt3319Fx): Promise<{ customerId: number }> {
  const billed = await completeStandardVolumesBilling(fx);
  if (!billed.extraCls.length) {
    throw new Error(
      'TC-BE-17 setup failed: no extra government/tax CL was created on the STANDARD invoice ' +
        '(VAT-base additional liabilities missing). Do not fake a tax CL via MANUAL POST ' +
        '(CustomerLiabilityRequest has no isGovernmentLiability).',
    );
  }
  await deleteCustomerLiability(fx, entityId(billed.mainCl));
  const taxCl = await getLiability(fx, entityId(billed.extraCls[0]));
  expect(String(taxCl.status)).toBe('ACTIVE');
  return { customerId: billed.customerId };
}

export async function completeCreditNoteNoLiability(fx: Pdt3319Fx): Promise<{ customerId: number }> {
  const { customerId } = await sharedCustomer(fx);
  const { customerDetailId, communicationId } = await customerDetailAndCommunication(fx, customerId);
  const payload = fx.GeneratePayload.billing.creditNote() as Record<string, unknown>;
  const common = (payload.commonParameters ?? {}) as Record<string, unknown>;
  common.sendingAnInvoice = 'EMAIL';
  common.invoiceDueDate = null;
  common.dueDate = null;
  payload.commonParameters = common;
  const note = (payload.manualCreditOrDebitNoteParameters ?? {}) as Record<string, unknown>;
  const basic = (note.manualCreditOrDebitNoteBasicDataParameters ?? {}) as Record<string, unknown>;
  basic.documentType = 'CREDIT_NOTE';
  basic.basisForIssuing = 'PDT-3319-TC-BE-20';
  basic.external = true;
  basic.billingRunInvoiceInformationList = [];
  basic.applicableInterestRateId = firstPositiveId(
    envVariables.interest_rate,
    envVariables.base_interest_rate,
  );
  expect(basic.applicableInterestRateId, 'applicableInterestRateId').toBeGreaterThan(0);
  basic.billingExternalInvoiceInformation = {
    customerDetailId,
    invoiceCommunicationDataId: communicationId,
    invoiceDate: snapshotDateD(),
    invoiceNumber: `PDT3319-CN-${Date.now()}`,
    prefixType: 'PRODUCT',
  };
  note.manualCreditOrDebitNoteBasicDataParameters = basic;
  const summary = (note.manualCreditOrDebitNoteSummaryDataParameters ?? {}) as Record<string, unknown>;
  const rows = Array.isArray(summary.summaryDataRowList)
    ? (summary.summaryDataRowList as Record<string, unknown>[])
    : [];
  if (rows[0]) rows[0].value = 10;
  summary.summaryDataRowList = rows;
  note.manualCreditOrDebitNoteSummaryDataParameters = summary;
  payload.manualCreditOrDebitNoteParameters = note;
  payload.billingType = 'MANUAL_CREDIT_OR_DEBIT_NOTE';
  await runBillingToReal(fx, payload, 1);
  return { customerId };
}

export async function completeReschedulingExclude(fx: Pdt3319Fx): Promise<{ customerId: number }> {
  const { customerId, currencyId } = await prepareManualReceivablesBase(fx);
  const d = snapshotDateD();
  const dueIso = addSofiaDays(d, -5);
  const ap = await sharedAccountingPeriodForDate(fx, dueIso);
  await sharedManualLiability(fx, {
    customerId,
    currencyId,
    accountingPeriodId: ap,
    dueIso,
    occurrenceIso: addSofiaDays(d, -10),
    initialAmount: 300.0,
  });
  // Finding 6 / interpreted AC-14: instalment CLs are excluded. Fail setup if none exist.
  await createReschedulingInstallment(fx as never);
  return { customerId };
}

export function pdt3319RelevantKeys(extra: string[] = []): string[] {
  return ['customer', 'customerLiability', 'payment', ...extra];
}
