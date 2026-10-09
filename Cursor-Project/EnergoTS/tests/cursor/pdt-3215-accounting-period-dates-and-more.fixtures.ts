/**
 * PDT-3215 helpers — Accounting Period UTC bounds, EasyPay DATE, Payment Package lock.
 *
 * Environment-agnostic: resolve the OPEN period that covers Sofia today via BASE_URL.
 * Do not hardcode ACCOUNTING202608 / 2026-08-01. Story August constants remain as
 * documentation of the original PDF example only.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3171-easypay-proforma-due-date-validto.spec.ts
 * - tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures.ts
 * - tests/cursor/pdt-2960-proforma-liability-due-date.fixtures.ts
 * - tests/cursor/pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures.ts
 *   (OPEN period GET + offline package for a specific paymentDate)
 *
 * /epay/* is not in Phoenix core swagger — contract from payment-api + PDT-3171 helpers.
 *
 * Swagger (all envs, update-swagger-specs.ps1 this session):
 * - GET /payment/validate-payment-date-and-accounting-period → PaymentDateValidationResponse
 * - CreatePaymentRequest required: accountPeriodId, collectionChannelId, currencyId,
 *   customerId, initialAmount, paymentDate (date), paymentPackageId
 * - PaymentPackageCreateRequest required: accountingPeriodId, channelId, lockStatus, paymentDate
 *   lockStatus enum LOCKED|UNLOCKED; type ONLINE|OFFLINE (optional)
 * - POST /payment-package/list → PaymentPackageListingRequest (fromDate/toDate format=date,
 *   lockStatuses, collectionChannelIds)
 * EasyPay ONLINE package: plain PaymentPackageCreateRequest (not GeneratePayload.payment_package).
 * GeneratePayload stamps Responses.collectionChannel[0] from the proforma OFFLINE channel and
 * may omit type=ONLINE — createOrFetch then binds a package that fails
 * findPaymentPackageByIdAndCollectionChannelIdAndPaymentDateAndLockStatusIn (HTTP 400,
 * "not applicable for selected payment date and collection channel"). Same fix as PDT-3205.
 * - AccountingPeriodRequest required: name, startDate, endDate, status, modifyDate
 * - GET /accounting-period (list) → PageAccountingPeriodsListingResponse
 *   content[] = AccountingPeriodsListingResponse.accountPeriodId (not id)
 * - GET /accounting-period/{id} → AccountingPeriodsResponse.id
 * - GET /accounting-period/available-accounting-periods → PageAccountingPeriodsResponse.id
 * - CustomerReceivableRequest required: accountingPeriodId, currencyId, customerId,
 *   dueDate, initialAmount, occurrenceDate (Swagger format=date; runtime JSON dd-MM-yyyy)
 * - CustomerLiabilityResponse: accountPeriodId (Long), accountingPeriodName
 * - PaymentResponse.accountPeriodId is AccountingPeriodsResponse (id, name)
 * - POST /payment-package/job-test
 */

import { expect } from './cursor-test.fixtures';
import { randomGens } from '../../utils/randomGens';
import {
  calculateConfirmChecksum,
  calculateInitChecksum,
  callConfirmPay,
  callInitPay,
  createLegalCustomer,
  findEasyPayOnlineChannel,
  formatEasyPayStatusDetail,
  listPaymentsByCustomerIdentifier,
  resolveEasyPayMerchantId,
  setEasyPayCombineLiabilities,
  setupProformaLiabilityChain,
  toCoinAmount,
  type Pdt3171Fx,
} from './pdt-3171-easypay-proforma-due-date-validto.fixtures';
import { withEasyPayChannelLock } from './pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures';

export type Pdt3215Fx = Pdt3171Fx;

export {
  calculateConfirmChecksum,
  calculateInitChecksum,
  callConfirmPay,
  callInitPay,
  createLegalCustomer,
  findEasyPayOnlineChannel,
  formatEasyPayStatusDetail,
  listPaymentsByCustomerIdentifier,
  resolveEasyPayMerchantId,
  setEasyPayCombineLiabilities,
  setupProformaLiabilityChain,
  toCoinAmount,
  withEasyPayChannelLock,
};

/** Story PDF example only — live tests resolve the OPEN period covering Sofia today. */
export const AUGUST_PERIOD_NAME = 'ACCOUNTING202608';
export const JULY_PERIOD_NAME = 'ACCOUNTING202607';
/** Story UTC of 01.08.2026 00:00 Europe/Sofia (summer). */
export const AUGUST_START_UTC_PREFIX = '2026-07-31T21:00:00';
/** Story UTC of 31.08.2026 23:59:59 Europe/Sofia (`bgEndOfDayAsUtc`). */
export const AUGUST_END_UTC_PREFIX = '2026-08-31T20:59:59';
export const EASYPAY_STORY_DATE = '20260801001500';
export const EASYPAY_FIRST_DAY_HMS = '001500';
export const VALIDATE_OUTSIDE_MESSAGE = 'Payment date is outside of the Accounting period!';
export const CLOSE_TOO_EARLY_FRAGMENT =
  'accountingPeriod-Cannot close the accounting period until its end date;';
export const EASYPAY_FUTURE_ADDITIONALINFO =
  'The provided date and time is in the future. Please provide a valid date.';

export type AccountingPeriodRow = {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
  modifyDate?: string;
};

function asId(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (value && typeof value === 'object' && 'id' in value) {
    return Number((value as { id?: unknown }).id);
  }
  return Number(value);
}

/** List GET /accounting-period uses AccountingPeriodsListingResponse.accountPeriodId. */
function periodIdFromListRow(row: Record<string, unknown>): number {
  return asId(row.accountPeriodId ?? row.id);
}

export function datePart(value: unknown): string {
  return String(value ?? '').trim().split('T')[0];
}

/** Response date → yyyy-mm-dd (ISO or Phoenix dd-MM-yyyy). */
export function toIsoDate(value: unknown): string {
  const raw = String(value ?? '').trim();
  if (!raw) return '';
  const iso = raw.split('T')[0];
  if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) return iso;
  const m = raw.match(/^(\d{2})[-.](\d{2})[-.](\d{4})/);
  if (m) return `${m[3]}-${m[2]}-${m[1]}`;
  return iso;
}

/** Swagger format=date; Phoenix LocalDate JSON is dd-MM-yyyy (payload generators). */
export function isoToDdMmYyyy(isoYmd: string): string {
  const [y, m, d] = isoYmd.split('-');
  return `${d}-${m}-${y}`;
}

export function timePart(value: unknown): string {
  const raw = String(value ?? '');
  const t = raw.includes('T') ? raw.split('T')[1] : '';
  return t.slice(0, 8);
}

export function sofiaYmd(date: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Sofia' }).format(date);
}

export function addSofiaDays(isoYmd: string, days: number): string {
  const [y, m, d] = isoYmd.split('-').map(Number);
  const utc = Date.UTC(y, m - 1, d + days, 12, 0, 0);
  return sofiaYmd(new Date(utc));
}

export function toEasyPayDate(isoYmd: string, hms = '001500'): string {
  return `${isoYmd.replace(/-/g, '')}${hms}`;
}

function sofiaParts(date: Date): { ymd: string; hms: string } {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Europe/Sofia',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  const g = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((p) => p.type === type)?.value ?? '';
  return {
    ymd: `${g('year')}-${g('month')}-${g('day')}`,
    hms: `${g('hour')}:${g('minute')}:${g('second')}`,
  };
}

/** Phoenix stores UTC LocalDateTime without offset (`2026-08-31T21:00:00`). */
export function utcLocalDateTimeToSofiaYmd(utcLocal: string): string {
  const iso = String(utcLocal ?? '').trim().replace(' ', 'T');
  const withOffset =
    /Z$/i.test(iso) || /[+-]\d{2}:\d{2}$/.test(iso) ? iso : `${iso}Z`;
  return sofiaYmd(new Date(withOffset));
}

/** Matches `BulgariaTimeUtils.bgStartOfDayAsUtc` — UTC prefix `yyyy-MM-ddTHH:mm:ss`. */
export function bgStartOfDayAsUtcPrefix(isoYmd: string): string {
  const [y, m, d] = isoYmd.split('-').map(Number);
  for (const offsetH of [3, 2]) {
    const utc = new Date(Date.UTC(y, m - 1, d, -offsetH, 0, 0));
    const parts = sofiaParts(utc);
    if (parts.ymd === isoYmd && parts.hms === '00:00:00') {
      return utc.toISOString().slice(0, 19);
    }
  }
  throw new Error(`Cannot map Sofia start of ${isoYmd} to UTC`);
}

/** Matches `BulgariaTimeUtils.bgEndOfDayAsUtc` — UTC prefix `yyyy-MM-ddTHH:mm:ss`. */
export function bgEndOfDayAsUtcPrefix(isoYmd: string): string {
  const [y, m, d] = isoYmd.split('-').map(Number);
  for (const offsetH of [3, 2]) {
    const utc = new Date(Date.UTC(y, m - 1, d, 23 - offsetH, 59, 59));
    const parts = sofiaParts(utc);
    if (parts.ymd === isoYmd && parts.hms === '23:59:59') {
      return utc.toISOString().slice(0, 19);
    }
  }
  throw new Error(`Cannot map Sofia end of ${isoYmd} to UTC`);
}

export function lastIsoDayOfMonth(firstIso: string): string {
  const [y, m] = firstIso.split('-').map(Number);
  return new Date(Date.UTC(y, m, 0)).toISOString().slice(0, 10);
}

export type PeriodCalendar = {
  period: AccountingPeriodRow;
  firstIso: string;
  lastIso: string;
  dayBeforeIso: string;
  startUtcPrefix: string;
  endUtcPrefix: string;
  easyPayFirstDay0015: string;
};

export function toPeriodCalendar(period: AccountingPeriodRow): PeriodCalendar {
  const named = period.name.match(/ACCOUNTING(\d{4})(\d{2})/);
  const firstIso = named
    ? `${named[1]}-${named[2]}-01`
    : utcLocalDateTimeToSofiaYmd(period.startDate);
  const lastIso = named
    ? lastIsoDayOfMonth(firstIso)
    : utcLocalDateTimeToSofiaYmd(period.endDate);
  return {
    period,
    firstIso,
    lastIso,
    dayBeforeIso: addSofiaDays(firstIso, -1),
    startUtcPrefix: bgStartOfDayAsUtcPrefix(firstIso),
    endUtcPrefix: bgEndOfDayAsUtcPrefix(lastIso),
    easyPayFirstDay0015: toEasyPayDate(firstIso, EASYPAY_FIRST_DAY_HMS),
  };
}

export type EasyPayOnlineConfirmPlan = {
  paymentDateIso: string;
  easyPayDate: string;
  usedFirstDayOfPeriod: boolean;
};

/**
 * Calendar day for EasyPay confirm-pay + ONLINE package on this env.
 *
 * origin/test PaymentService.validateAndSetPaymentPackageFromOnlinePayment looks up
 * the package with LocalDate.now(), not CreatePaymentRequest.paymentDate.
 * createOrFetch still uses DATE.toLocalDate(). Those two days must match or confirm
 * returns HTTP 400 "package … not applicable" (Test: package 4791 payment_date=2026-09-01
 * vs validation param 2026-09-23). origin/dev uses the request paymentDate (PDT-3215).
 * Sofia today 00:15 is inside the live OPEN period and equals LocalDate.now() after 00:15.
 * On the 1st of the month this is the PDF first-BG-day DATE.
 */
export function planEasyPayOnlineConfirm(cal: PeriodCalendar): EasyPayOnlineConfirmPlan {
  const today = sofiaYmd();
  const paymentDateIso =
    today >= cal.firstIso && today <= cal.lastIso ? today : cal.firstIso;
  return {
    paymentDateIso,
    easyPayDate: toEasyPayDate(paymentDateIso, EASYPAY_FIRST_DAY_HMS),
    usedFirstDayOfPeriod: paymentDateIso === cal.firstIso,
  };
}

export function sofiaNowIsBeforeEasyPayDate(easyPayDate: string): boolean {
  const now = new Date();
  const ymd = sofiaYmd(now);
  const parts = sofiaParts(now);
  const nowStamp = `${ymd.replace(/-/g, '')}${parts.hms.replace(/:/g, '')}`;
  return nowStamp < easyPayDate;
}

export async function getAccountingPeriodById(
  fx: Pdt3215Fx,
  periodId: number,
): Promise<AccountingPeriodRow> {
  const res = await fx.Request.get(`accounting-period/${periodId}`);
  await expect(res).CheckResponse();
  const body = await res.json();
  return {
    id: Number(body.id),
    name: String(body.name ?? ''),
    startDate: String(body.startDate ?? ''),
    endDate: String(body.endDate ?? ''),
    status: String(body.status ?? ''),
    modifyDate: body.modifyDate != null ? String(body.modifyDate) : undefined,
  };
}

export async function listAccountingPeriodsByPrompt(
  fx: Pdt3215Fx,
  prompt: string,
): Promise<AccountingPeriodRow[]> {
  const res = await fx.Request.get(
    `accounting-period?page=0&size=20&prompt=${encodeURIComponent(prompt)}`,
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  const rows = (body.content ?? []) as Array<Record<string, unknown>>;
  return rows
    .map((row) => ({
      id: periodIdFromListRow(row),
      name: String(row.name ?? ''),
      startDate: String(row.startDate ?? ''),
      endDate: String(row.endDate ?? ''),
      status: String(row.status ?? ''),
      modifyDate: row.modifyDate != null ? String(row.modifyDate) : undefined,
    }))
    .filter((row) => Number.isFinite(row.id) && row.id > 0);
}

export async function resolveAugustAccountingPeriod(
  fx: Pdt3215Fx,
): Promise<AccountingPeriodRow> {
  const listed = await listAccountingPeriodsByPrompt(fx, AUGUST_PERIOD_NAME);
  const match =
    listed.find((row) => row.name === AUGUST_PERIOD_NAME && row.status === 'OPEN') ??
    listed.find((row) => row.name === AUGUST_PERIOD_NAME);
  expect(
    match,
    `GET /accounting-period prompt=${AUGUST_PERIOD_NAME} must return a row`,
  ).toBeTruthy();
  expect(
    match!.id,
    `AccountingPeriodsListingResponse.accountPeriodId for ${AUGUST_PERIOD_NAME}`,
  ).toBeGreaterThan(0);
  return getAccountingPeriodById(fx, match!.id);
}

export async function resolveOpenPeriodForSofiaDate(
  fx: Pdt3215Fx,
  isoYmd: string,
): Promise<AccountingPeriodRow | null> {
  const yyyyMm = isoYmd.slice(0, 7).replace('-', '');
  const expectedName = `ACCOUNTING${yyyyMm}`;
  const open = await listOpenAccountingPeriods(fx);
  const byName = open.find((p) => p.name === expectedName);
  if (byName) return byName;
  const covering = open.find((p) => {
    const cal = toPeriodCalendar(p);
    return isoYmd >= cal.firstIso && isoYmd <= cal.lastIso;
  });
  return covering ?? null;
}

export async function currentOpenPeriodForNow(fx: Pdt3215Fx): Promise<AccountingPeriodRow> {
  const today = sofiaYmd();
  const period = await resolveOpenPeriodForSofiaDate(fx, today);
  expect(
    period,
    `OPEN accounting period covering Sofia today ${today} must exist`,
  ).toBeTruthy();
  return period!;
}

export async function listOpenAccountingPeriods(fx: Pdt3215Fx): Promise<AccountingPeriodRow[]> {
  const res = await fx.Request.get(
    'accounting-period/available-accounting-periods?page=0&size=20',
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  const rows = (body.content ?? []) as Array<Record<string, unknown>>;
  const periods: AccountingPeriodRow[] = [];
  for (const row of rows) {
    const id = periodIdFromListRow(row);
    if (!Number.isFinite(id) || id <= 0) continue;
    periods.push(await getAccountingPeriodById(fx, id));
  }
  return periods.filter((p) => p.status === 'OPEN');
}

export async function validatePaymentDate(
  fx: Pdt3215Fx,
  accountingPeriodId: number,
  paymentDateIso: string,
): Promise<{ valid: boolean; message: string | null; status: number }> {
  const res = await fx.Request.get(
    `${fx.Endpoints.payment}/validate-payment-date-and-accounting-period` +
      `?accountingPeriodId=${accountingPeriodId}&paymentDate=${paymentDateIso}`,
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  return {
    valid: Boolean(body.valid),
    message: body.message == null ? null : String(body.message),
    status: res.status(),
  };
}

export async function getPaymentDetail(fx: Pdt3215Fx, paymentId: number): Promise<any> {
  const res = await fx.Request.get(`${fx.Endpoints.payment}/${paymentId}`);
  await expect(res).CheckResponse();
  return res.json();
}

export async function getPaymentPackageDetail(fx: Pdt3215Fx, packageId: number): Promise<any> {
  const res = await fx.Request.get(`${fx.Endpoints.paymentPackage}/${packageId}`);
  await expect(res).CheckResponse();
  return res.json();
}

export async function findUnlockedOnlinePackageOnChannel(
  fx: Pdt3215Fx,
  opts: { channelId: number; paymentDateIso: string },
): Promise<number | null> {
  const listRes = await fx.Request.post(`${fx.Endpoints.paymentPackage}/list`, {
    data: {
      page: 0,
      size: 50,
      collectionChannelIds: [opts.channelId],
      allCollectionChannels: false,
      lockStatuses: ['UNLOCKED'],
      fromDate: opts.paymentDateIso,
      toDate: opts.paymentDateIso,
    },
  });
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  const rows = Array.isArray(body?.content) ? body.content : [];
  const match = rows.find(
    (row: { id?: number; paymentDate?: unknown }) =>
      toIsoDate(row.paymentDate) === opts.paymentDateIso && asId(row.id) > 0,
  );
  return match ? asId(match.id) : null;
}

async function assertPackageMatchesConfirmDay(
  fx: Pdt3215Fx,
  packageId: number,
  opts: { channelId: number; paymentDateIso: string },
): Promise<void> {
  const pkg = await getPaymentPackageDetail(fx, packageId);
  expect(toIsoDate(pkg.paymentDate), `package ${packageId} paymentDate`).toBe(opts.paymentDateIso);
  expect(String(pkg.lockStatus), `package ${packageId} lockStatus`).toBe('UNLOCKED');
  const channelId = asId(pkg.collectionChannel?.id ?? pkg.collectionChannel);
  expect(channelId, `package ${packageId} collectionChannel.id`).toBe(opts.channelId);
}

export function extractPaymentPeriodId(payment: any): number | null {
  const nested = payment?.accountPeriodId;
  if (nested == null) return null;
  if (typeof nested === 'number') return nested;
  const id = Number(nested.id);
  return Number.isFinite(id) ? id : null;
}

export function extractPaymentPeriodName(payment: any): string {
  const nested = payment?.accountPeriodId;
  if (nested && typeof nested === 'object' && nested.name) return String(nested.name);
  return String(payment?.accountingPeriodName ?? '');
}

export async function createOfflineChannelAndPackage(
  fx: Pdt3215Fx,
  opts: { paymentDateIso: string; accountingPeriodId: number; type?: 'OFFLINE' | 'ONLINE' },
): Promise<{ collectionChannelId: number; paymentPackageId: number }> {
  const channelRes = await fx.Request.post(fx.Endpoints.collectionChannel, {
    data: fx.GeneratePayload.receivablesManagement.collection_channel(),
  });
  await expect(channelRes).CheckResponse();
  const collectionChannelId = asId(await channelRes.json());
  fx.Responses.collectionChannel.push(collectionChannelId);

  const packagePayload = fx.GeneratePayload.receivablesManagement.payment_package();
  packagePayload.channelId = collectionChannelId;
  packagePayload.paymentDate = opts.paymentDateIso;
  packagePayload.accountingPeriodId = opts.accountingPeriodId;
  packagePayload.lockStatus = 'UNLOCKED';
  (packagePayload as { type?: string }).type = opts.type ?? 'OFFLINE';

  const packageRes = await fx.Request.post(fx.Endpoints.paymentPackage, {
    data: packagePayload,
  });
  await expect(packageRes).CheckResponse();
  const paymentPackageId = asId(await packageRes.json());
  fx.Responses.paymentPackage.push(paymentPackageId);
  return { collectionChannelId, paymentPackageId };
}

export async function createOnlinePackageOnChannel(
  fx: Pdt3215Fx,
  opts: { channelId: number; paymentDateIso: string; accountingPeriodId: number },
): Promise<number> {
  expect(opts.paymentDateIso, 'ONLINE package paymentDateIso').toMatch(/^\d{4}-\d{2}-\d{2}$/);
  expect(opts.channelId, 'ONLINE package channelId').toBeGreaterThan(0);
  expect(opts.accountingPeriodId, 'ONLINE package accountingPeriodId').toBeGreaterThan(0);

  const packagePayload = {
    channelId: opts.channelId,
    paymentDate: opts.paymentDateIso,
    accountingPeriodId: opts.accountingPeriodId,
    lockStatus: 'UNLOCKED' as const,
    type: 'ONLINE' as const,
  };

  const packageRes = await fx.Request.post(fx.Endpoints.paymentPackage, {
    data: packagePayload,
  });

  if (packageRes.ok()) {
    const paymentPackageId = asId(await packageRes.json());
    expect(paymentPackageId, 'ONLINE payment package id').toBeGreaterThan(0);
    fx.Responses.paymentPackage.push(paymentPackageId);
    await assertPackageMatchesConfirmDay(fx, paymentPackageId, opts);
    return paymentPackageId;
  }

  const postDetail = (await packageRes.text()).slice(0, 500);
  const existingId = await findUnlockedOnlinePackageOnChannel(fx, opts);
  expect(
    existingId,
    `POST /payment-package ONLINE paymentDate=${opts.paymentDateIso} ` +
      `channelId=${opts.channelId} accountingPeriodId=${opts.accountingPeriodId} ` +
      `HTTP=${packageRes.status()} body=${postDetail} ` +
      `(no UNLOCKED ONLINE package on this channel+date for createOrFetch)`,
  ).toBeTruthy();
  const paymentPackageId = existingId as number;
  fx.Responses.paymentPackage.push(paymentPackageId);
  await assertPackageMatchesConfirmDay(fx, paymentPackageId, opts);
  return paymentPackageId;
}

export async function createOfflinePayment(
  fx: Pdt3215Fx,
  opts: {
    paymentDateIso: string;
    accountingPeriodId: number;
    collectionChannelId: number;
    paymentPackageId: number;
    initialAmount?: number;
  },
): Promise<number> {
  const payload = await fx.GeneratePayload.receivablesManagement.payment();
  payload.initialAmount = opts.initialAmount ?? 10;
  payload.paymentDate = opts.paymentDateIso;
  payload.accountPeriodId = opts.accountingPeriodId;
  payload.collectionChannelId = opts.collectionChannelId;
  payload.paymentPackageId = opts.paymentPackageId;
  payload.customerId = fx.Responses.customer[0].id;
  payload.blockedForOffsetting = false;

  const res = await fx.Request.post(fx.Endpoints.payment, { data: payload });
  await expect(res).CheckResponse();
  const paymentId = asId(await res.json());
  fx.Responses.payment.push(paymentId);
  return paymentId;
}

export async function createReceivableOnPeriodDate(
  fx: Pdt3215Fx,
  opts: { accountingPeriodId: number; occurrenceIso: string; dueIso: string },
): Promise<{ receivableId: number; body: any }> {
  const payload = fx.GeneratePayload.receivablesManagement.customer_receivable();
  payload.accountingPeriodId = opts.accountingPeriodId;
  payload.initialAmount = 10.0;
  payload.occurrenceDate = isoToDdMmYyyy(opts.occurrenceIso);
  payload.dueDate = isoToDdMmYyyy(opts.dueIso);
  payload.customerId = fx.Responses.customer[0].id;

  const res = await fx.Request.post(fx.Endpoints.customerReceivable, { data: payload });
  await expect(res).CheckResponse();
  const created = await res.json();
  const receivableId = asId(created);
  fx.Responses.customerReceivable.push(receivableId);

  const getRes = await fx.Request.get(`${fx.Endpoints.customerReceivable}/${receivableId}`);
  await expect(getRes).CheckResponse();
  return { receivableId, body: await getRes.json() };
}

export async function createAugustReceivable(
  fx: Pdt3215Fx,
  augustId: number,
): Promise<{ receivableId: number; body: any }> {
  return createReceivableOnPeriodDate(fx, {
    accountingPeriodId: augustId,
    occurrenceIso: '2026-08-01',
    dueIso: '2026-08-15',
  });
}

export async function runPackageLockJob(fx: Pdt3215Fx): Promise<void> {
  const res = await fx.Request.post(`${fx.Endpoints.paymentPackage}/job-test`);
  await expect(res).CheckResponse();
}

export async function getLiabilityAccountingPeriod(
  fx: Pdt3215Fx,
  liabilityId: number,
): Promise<{ accountPeriodId: number | null; accountingPeriodName: string; currentAmount: number }> {
  const res = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
  await expect(res).CheckResponse();
  const body = await res.json();
  const accountPeriodId =
    typeof body.accountPeriodId === 'number'
      ? body.accountPeriodId
      : body.accountPeriodId?.id != null
        ? Number(body.accountPeriodId.id)
        : null;
  return {
    accountPeriodId: Number.isFinite(accountPeriodId as number) ? Number(accountPeriodId) : null,
    accountingPeriodName: String(body.accountingPeriodName ?? ''),
    currentAmount: Number(body.currentAmount ?? 0),
  };
}

export function uniqueTid(): string {
  return randomGens.generateTID();
}
