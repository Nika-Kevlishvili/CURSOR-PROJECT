/**
 * PDT-3205 helpers — Virtual POS exact-copy of EasyPay online payment protocol.
 *
 * Reference patterns:
 * - tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures.ts (init/confirm, VALIDTO, proforma)
 * - tests/cursor/pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures.ts
 *   (channel lock, combineLiabilities, AP/DATE helpers,
 *    ensureEasyPayOnlinePaymentPackageForDate → mirrored as ensureVirtualPosOnlinePaymentPackageForDate)
 * - tests/receivableManagement/Payment/onlinePayment.spec.ts (REG-1036 EasyPay flow)
 *
 * /virtualpos/* and /epay/* are NOT in Phoenix core swagger — contract from payment-api + TC.
 * Virtual POS merchant (probe via resolveVirtualPosMerchantId):
 *   classic Dev payment-api (:9091) → 7000006;
 *   phoenix2 payment-api2 (:9092) → currently 7000005.
 * EasyPay: resolveEasyPayMerchantId (7000005 / Merchant_ID by env).
 * Channel names: VirtualPos / EasyPay.
 */

import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { expect } from '@playwright/test';
import { randomGens } from '../../utils/randomGens';
import {
  calculateConfirmChecksum as calculateEasyPayConfirmChecksum,
  calculateInitChecksum as calculateEasyPayInitChecksum,
  callConfirmPay as callEasyPayConfirmPay,
  callInitPay as callEasyPayInitPay,
  createLegalCustomer,
  createManualLiability,
  findEasyPayOnlineChannel,
  formatEasyPayStatusDetail,
  getLiabilityDetail,
  listOpenLiabilitiesForCustomer,
  listPaymentsByCustomerIdentifier,
  PROFORMA_VALID_TO_DAYS,
  resolveEasyPayMerchantId,
  setEasyPayCombineLiabilities,
  setupProformaLiabilityChain,
  issueInvoiceAndReadLiabilityDueDate,
  toCoinAmount,
  yyyyMmDdFromLocalDate,
  dueDateToYyyyMmDd,
  assertValidToNotZero,
  type Pdt3171Fx,
} from './pdt-3171-easypay-proforma-due-date-validto.fixtures';
import {
  extractAccountPeriodId,
  findLatestOnlinePaymentForCustomer,
  getPaymentDetail,
  isoToEasyPayDate,
  isEasyPayConfirmEnvBlockerFailure,
  isPaymentPackageNotApplicableFailure,
  resolveEnvAccountingPeriodContext,
  withEasyPayChannelLock,
  type ConfirmPayLpfResult,
  type Pdt3214Fx,
} from './pdt-3214-easypay-liabilities-receivables-accounting-period-rules.fixtures';

export type Pdt3205Fx = Pdt3171Fx;

export {
  createLegalCustomer,
  createManualLiability,
  findEasyPayOnlineChannel,
  formatEasyPayStatusDetail,
  getLiabilityDetail,
  listOpenLiabilitiesForCustomer,
  listPaymentsByCustomerIdentifier,
  PROFORMA_VALID_TO_DAYS,
  resolveEasyPayMerchantId,
  setEasyPayCombineLiabilities,
  setupProformaLiabilityChain,
  issueInvoiceAndReadLiabilityDueDate,
  toCoinAmount,
  yyyyMmDdFromLocalDate,
  dueDateToYyyyMmDd,
  assertValidToNotZero,
  calculateEasyPayConfirmChecksum,
  calculateEasyPayInitChecksum,
  callEasyPayConfirmPay,
  callEasyPayInitPay,
  extractAccountPeriodId,
  findLatestOnlinePaymentForCustomer,
  getPaymentDetail,
  isoToEasyPayDate,
  isEasyPayConfirmEnvBlockerFailure,
  isPaymentPackageNotApplicableFailure,
  resolveEnvAccountingPeriodContext,
  withEasyPayChannelLock,
};

/**
 * Default / docs: classic Dev Virtual POS merchant (`virtualpos.merchant.id`).
 * Prefer `resolveVirtualPosMerchantId` at runtime (9091→7000006, 9092→7000005).
 */
export const VIRTUAL_POS_MERCHANT_ID = '7000006';
/** Default / docs: classic Dev EasyPay merchant. Prefer `resolveEasyPayMerchantId`. */
export const EASY_PAY_MERCHANT_ID = '7000005';
export const VIRTUAL_POS_CHANNEL_NAME = 'VirtualPos';
export const EASY_PAY_CHANNEL_NAME = 'EasyPay';

/** Fake IDN for merchant acceptance probes (unknown subscriber → STATUS 14 when merchant OK). */
const VPOS_MERCHANT_PROBE_IDN = '1234567890';
/** STATUS values that mean the merchant is accepted by payment-api (not 96/93). */
const VPOS_MERCHANT_ACCEPTED_STATUSES = new Set(['14', '62', '00']);

let cachedVirtualPosMerchantId: string | null = null;

const VPOS_CHANNEL_LOCK_PATH = path.join(os.tmpdir(), 'pdt-3205-virtualpos-channel.lock');
const VPOS_CHANNEL_LOCK_STALE_MS = 10 * 60 * 1000;
const VPOS_CHANNEL_LOCK_WAIT_MS = 3 * 60 * 1000;

/**
 * Resolve Virtual POS MERCHANTID for /virtualpos init/confirm.
 * Probes candidates with TYPE=CHECK + fake IDN; caches the first accepted merchant.
 *
 * Candidate order: system-config `virtualPosMerchantId` (if present), then
 * `7000006`, `7000005`, `Merchant_ID`.
 * Accepted STATUS: 14 / 62 / 00 (rejected: 96 wrong merchant, 93 bad checksum).
 *
 * Documented env mapping (when probe falls through):
 * - classic Dev `:9091` → 7000006
 * - phoenix2 `:9092` → 7000005
 */
export async function resolveVirtualPosMerchantId(fx: Pdt3205Fx): Promise<string> {
  if (cachedVirtualPosMerchantId) return cachedVirtualPosMerchantId;

  const candidates: string[] = [];
  const res = await fx.Request.get(`${fx.Endpoints.systemConfiguration}`);
  if (res.status() === 200) {
    const body = await res.json();
    const fromCfg = String(
      body.virtualPosMerchantId ?? body.virtualPOSMerchantId ?? body.virtualposMerchantId ?? '',
    ).trim();
    if (fromCfg) candidates.push(fromCfg);
  }
  for (const id of [VIRTUAL_POS_MERCHANT_ID, EASY_PAY_MERCHANT_ID, 'Merchant_ID']) {
    if (!candidates.includes(id)) candidates.push(id);
  }

  for (const merchantId of candidates) {
    try {
      const checksum = await calculateVirtualPosInitChecksum(fx, {
        customerNumber: VPOS_MERCHANT_PROBE_IDN,
        merchantId,
        type: 'CHECK',
      });
      const body = await callVirtualPosInitPay(fx, {
        customerNumber: VPOS_MERCHANT_PROBE_IDN,
        merchantId,
        type: 'CHECK',
        checksum,
      });
      const status = String(body?.STATUS ?? '');
      if (VPOS_MERCHANT_ACCEPTED_STATUSES.has(status)) {
        cachedVirtualPosMerchantId = merchantId;
        return merchantId;
      }
    } catch {
      /* checksum/init failed for this candidate — try next */
    }
  }

  const paymentUrl = String(fx.OnlinePaymentUrl ?? '');
  const fallback = paymentUrl.includes('9092') ? EASY_PAY_MERCHANT_ID : VIRTUAL_POS_MERCHANT_ID;
  cachedVirtualPosMerchantId = fallback;
  return fallback;
}

/**
 * Serialize VirtualPos ONLINE channel combineLiabilities mutations across workers
 * (same pattern as PDT-3214 withEasyPayChannelLock).
 */
export async function withVirtualPosChannelLock<T>(fn: () => Promise<T>): Promise<T> {
  const started = Date.now();
  for (;;) {
    try {
      const fd = fs.openSync(VPOS_CHANNEL_LOCK_PATH, 'wx');
      try {
        fs.writeFileSync(fd, `${process.pid}:${Date.now()}\n`, 'utf8');
      } finally {
        fs.closeSync(fd);
      }
      break;
    } catch (err: unknown) {
      const code = (err as NodeJS.ErrnoException)?.code;
      if (code !== 'EEXIST') throw err;
      try {
        const st = fs.statSync(VPOS_CHANNEL_LOCK_PATH);
        if (Date.now() - st.mtimeMs > VPOS_CHANNEL_LOCK_STALE_MS) {
          fs.unlinkSync(VPOS_CHANNEL_LOCK_PATH);
          continue;
        }
      } catch {
        /* lock vanished — retry */
      }
      if (Date.now() - started > VPOS_CHANNEL_LOCK_WAIT_MS) {
        throw new Error(
          `VirtualPos channel lock timeout after ${VPOS_CHANNEL_LOCK_WAIT_MS}ms ` +
            `(${VPOS_CHANNEL_LOCK_PATH}). Prefer --workers=1 for PDT-3205.`,
        );
      }
      await new Promise((r) => setTimeout(r, 150 + Math.floor(Math.random() * 200)));
    }
  }
  try {
    return await fn();
  } finally {
    try {
      fs.unlinkSync(VPOS_CHANNEL_LOCK_PATH);
    } catch {
      /* ignore */
    }
  }
}

export function formatVirtualPosStatusDetail(body: any): string {
  return (
    `STATUS=${body?.STATUS} AMOUNT=${body?.AMOUNT ?? 'n/a'} ` +
    `ADDITIONALINFO=${body?.ADDITIONALINFO ?? 'n/a'} ` +
    `DESCRIPTION=${body?.DESCRIPTION ?? 'n/a'} ` +
    `SHORTDESC=${body?.SHORTDESC ?? 'n/a'} ` +
    `LONGDESC=${body?.LONGDESC ?? 'n/a'} ` +
    `VALIDTO=${body?.VALIDTO ?? 'n/a'} ` +
    `body=${JSON.stringify(body)}`
  );
}

/** Resolve ShortResponse / nested `{ id }` / bare id. */
export function shortId(value: unknown): number | null {
  if (value == null) return null;
  if (typeof value === 'number') return value;
  if (typeof value === 'object' && value !== null && 'id' in value) {
    const id = (value as { id?: unknown }).id;
    return typeof id === 'number' ? id : id != null ? Number(id) : null;
  }
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

function collectionChannelPutFromGet(
  current: Record<string, any>,
  combineLiabilities: boolean,
): Record<string, unknown> {
  const employee = current.employee ?? null;
  const performerId = shortId(current.performerId) ?? shortId(employee);
  const performerType = current.performerType ?? employee?.performerType ?? null;

  const payload: Record<string, unknown> = {
    name: current.name,
    type: current.type,
    collectionPartnerId: shortId(current.collectionPartnerId),
    numberOfIncomeAccount: current.numberOfIncomeAccount,
    currencyId: shortId(current.currencyId),
    customerConditionType: current.customerConditionType ?? current.conditionType,
    condition: current.condition ?? null,
    listOfCustomers: current.listOfCustomers ?? null,
    excludeLiabilitiesByPrefix: current.excludeLiabilitiesByPrefix ?? [],
    excludeLiabilitiesByAmount: {
      lessThan: current.excludeLiabilitiesByAmount?.lessThan ?? current.lessThan ?? null,
      greaterThan:
        current.excludeLiabilitiesByAmount?.greaterThan ?? current.greaterThan ?? null,
    },
    priorityLiabilitiesByPrefix: current.priorityLiabilitiesByPrefix ?? [],
    typeOfFile: current.typeOfFile ?? null,
    bankIds: current.bankIds ?? [],
    globalBank: current.globalBank ?? current.isGlobalBank ?? null,
    dataSendingSchedule: current.dataSendingSchedule ?? null,
    dataReceivingSchedule: current.dataReceivingSchedule ?? null,
    numberOfWorkingDays: current.numberOfWorkingDays ?? null,
    calendarId: shortId(current.calendarId),
    waitingPeriodToleranceInHours: current.waitingPeriodToleranceInHours ?? null,
    folderForFileReceiving: current.folderForFileReceiving ?? null,
    folderForFileSending: current.folderForFileSending ?? null,
    emailForFileSending: current.emailForFileSending ?? null,
    combineLiabilities,
  };

  if (performerId != null) {
    payload.performerId = performerId;
    if (performerType != null) payload.performerType = performerType;
    if (employee != null) payload.employee = employee;
  }

  return payload;
}

/**
 * Find ONLINE channel by exact name (VirtualPos / EasyPay).
 * Search mirrors REG-1036 / PDT-3171 listing query.
 */
export async function findOnlineChannelByName(
  fx: Pdt3205Fx,
  channelName: string,
): Promise<{ id: number; name: string; combineLiabilities: boolean } | null> {
  const listRes = await fx.Request.get(
    `${fx.Endpoints.collectionChannel}?page=0&size=25&searchBy=NAME&prompt=${encodeURIComponent(channelName)}&collectionChannelType=ONLINE`,
  );
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  const rows = (body.content ?? []) as Array<Record<string, any>>;
  const hit = rows.find(
    (r) => String(r.name) === channelName && String(r.type).toUpperCase() === 'ONLINE',
  );
  if (!hit) return null;
  return {
    id: Number(hit.id),
    name: String(hit.name),
    combineLiabilities: Boolean(hit.combineLiabilities),
  };
}

/** Create ONLINE named channel when missing (Swagger CollectionChannelBaseRequest). */
export async function createNamedOnlineChannel(
  fx: Pdt3205Fx,
  opts: { name: string; combineLiabilities: boolean },
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.collection_channel();
  payload.name = opts.name;
  payload.type = 'ONLINE';
  payload.combineLiabilities = opts.combineLiabilities;
  payload.customerConditionType = 'ALL_CUSTOMERS';
  payload.typeOfFile = null;
  payload.bankIds = null;
  const res = await fx.Request.post(fx.Endpoints.collectionChannel, { data: payload });
  await expect(res).CheckResponse();
  const created = await res.json();
  const id = typeof created === 'number' ? created : Number(created?.id);
  expect(id, `${opts.name} channel id after create`).toBeGreaterThan(0);
  return id;
}

export async function setOnlineChannelCombineLiabilities(
  fx: Pdt3205Fx,
  channelId: number,
  combineLiabilities: boolean,
): Promise<void> {
  const getRes = await fx.Request.get(`${fx.Endpoints.collectionChannel}/${channelId}`);
  await expect(getRes).CheckResponse();
  const current = await getRes.json();
  if (Boolean(current.combineLiabilities) === combineLiabilities) return;

  const putRes = await fx.Request.put(`${fx.Endpoints.collectionChannel}/${channelId}`, {
    data: collectionChannelPutFromGet(current, combineLiabilities),
  });
  await expect(putRes).CheckResponse();
}

/**
 * Resolve VirtualPos ONLINE channel; create if missing; PUT combineLiabilities as needed.
 * Call inside withVirtualPosChannelLock when mutating combine.
 */
export async function ensureVirtualPosChannel(
  fx: Pdt3205Fx,
  combineLiabilities: boolean,
): Promise<number> {
  let found = await findOnlineChannelByName(fx, VIRTUAL_POS_CHANNEL_NAME);
  let channelId: number;
  if (!found) {
    channelId = await createNamedOnlineChannel(fx, {
      name: VIRTUAL_POS_CHANNEL_NAME,
      combineLiabilities,
    });
  } else {
    channelId = found.id;
    await setOnlineChannelCombineLiabilities(fx, channelId, combineLiabilities);
  }

  const getRes = await fx.Request.get(`${fx.Endpoints.collectionChannel}/${channelId}`);
  await expect(getRes).CheckResponse();
  const channelBody = await getRes.json();
  expect(String(channelBody.name)).toBe(VIRTUAL_POS_CHANNEL_NAME);
  expect(Boolean(channelBody.combineLiabilities)).toBe(combineLiabilities);
  fx.Responses.collectionChannel.push(channelId);
  return channelId;
}

export type VirtualPosOnlinePackageEnsureResult = {
  channelId: number;
  paymentPackageId: number | null;
  /** true when POST /payment-package created a new row */
  created: boolean;
  /** Present when POST did not succeed (package may already exist for date+channel). */
  detail?: string;
};

/**
 * Ensure an ONLINE UNLOCKED payment package exists on the VirtualPos ONLINE channel for the
 * confirm calendar day + OPEN accountingPeriodId (Swagger PaymentPackageCreateRequest).
 *
 * Why: PaymentCreationTransactionalService.createOrFetchPaymentPackage looks up by
 * paymentDate + channelId + UNLOCKED. Env-stale packages (e.g. Test id 4402 bound to a
 * different date/AP) cause HTTP 400
 * "paymentPackageId … not applicable for selected payment date and collection channel".
 * Pre-creating type=ONLINE (same pattern as PDT-3214 ensureEasyPayOnlinePaymentPackageForDate /
 * PDT-3215 createOnlinePackageOnChannel) makes createOrFetch find an applicable package.
 *
 * Do NOT hardcode env-specific package ids. Call inside withVirtualPosChannelLock before confirm.
 * Idempotent: if POST fails (duplicate date+channel), returns created=false — createOrFetch may
 * still reuse an existing UNLOCKED package for that day.
 *
 * @param paymentDateIso yyyy-mm-dd — must equal Virtual POS confirm calendar day
 * @param accountingPeriodId OPEN period containing paymentDateIso (prior OPEN for TC-BE-5)
 * @param channelId optional; defaults to ensureVirtualPosChannel(fx, true) id resolution via name
 */
export async function ensureVirtualPosOnlinePaymentPackageForDate(
  fx: Pdt3205Fx,
  opts: {
    paymentDateIso: string;
    accountingPeriodId: number;
    channelId?: number;
  },
): Promise<VirtualPosOnlinePackageEnsureResult> {
  expect(
    opts.paymentDateIso,
    'paymentDateIso for VirtualPos ONLINE payment package',
  ).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  expect(
    opts.accountingPeriodId,
    'OPEN accountingPeriodId for VirtualPos ONLINE payment package',
  ).toBeGreaterThan(0);

  let channelId = opts.channelId;
  if (channelId == null || channelId <= 0) {
    const found = await findOnlineChannelByName(fx, VIRTUAL_POS_CHANNEL_NAME);
    expect(found, 'VirtualPos ONLINE channel must exist before package ensure').toBeTruthy();
    channelId = found!.id;
  }
  expect(channelId, 'VirtualPos ONLINE channel id').toBeGreaterThan(0);
  fx.Responses.collectionChannel.push(channelId);

  // Plain Swagger PaymentPackageCreateRequest — do not use GeneratePayload.payment_package()
  // (it stamps Responses.collectionChannel[0] / env accounting_period and may omit type).
  const packagePayload = {
    channelId,
    paymentDate: opts.paymentDateIso,
    accountingPeriodId: opts.accountingPeriodId,
    lockStatus: 'UNLOCKED' as const,
    type: 'ONLINE' as const,
  };

  const packageRes = await fx.Request.post(fx.Endpoints.paymentPackage, {
    data: packagePayload,
  });

  if (packageRes.ok()) {
    const paymentPackageId = (await packageRes.json()) as number;
    expect(paymentPackageId, 'VirtualPos ONLINE payment package id').toBeGreaterThan(0);
    fx.Responses.paymentPackage.push(paymentPackageId);
    return { channelId, paymentPackageId, created: true };
  }

  const detail =
    `POST /payment-package ONLINE paymentDate=${opts.paymentDateIso} ` +
    `channelId=${channelId} accountingPeriodId=${opts.accountingPeriodId} ` +
    `HTTP=${packageRes.status()} body=${(await packageRes.text()).slice(0, 500)} ` +
    `(may already exist — createOrFetch will reuse if UNLOCKED)`;
  return { channelId, paymentPackageId: null, created: false, detail };
}

/**
 * Resolve paymentDateIso from Virtual POS / EasyPay confirm DATE (yyyyMMdd or yyyyMMddHHmmss)
 * and ensure an ONLINE package on VirtualPos for that day.
 *
 * When accountingPeriodId is omitted, uses today OPEN period from resolveEnvAccountingPeriodContext
 * (suitable for generateOnlinePaymentDate(4) confirms). For prior-OPEN DATE tests (TC-BE-5),
 * pass priorOpenPeriod.id + paymentDateIso=priorOpenMidIso explicitly via
 * ensureVirtualPosOnlinePaymentPackageForDate instead.
 */
export async function ensureVirtualPosOnlinePaymentPackageForConfirmDate(
  fx: Pdt3205Fx,
  opts: {
    confirmDate: string;
    accountingPeriodId?: number;
    channelId?: number;
  },
): Promise<VirtualPosOnlinePackageEnsureResult> {
  const paymentDateIso = calendarDayIso(opts.confirmDate);
  expect(
    paymentDateIso,
    `confirmDate must yield yyyy-mm-dd (got confirmDate=${opts.confirmDate})`,
  ).toMatch(/^\d{4}-\d{2}-\d{2}$/);

  let accountingPeriodId = opts.accountingPeriodId;
  if (accountingPeriodId == null || accountingPeriodId <= 0) {
    const ctx = await resolveEnvAccountingPeriodContext(fx);
    accountingPeriodId = ctx.todayOpenPeriod.id;
  }

  return ensureVirtualPosOnlinePaymentPackageForDate(fx, {
    paymentDateIso,
    accountingPeriodId,
    channelId: opts.channelId,
  });
}

/**
 * Resolve EasyPay ONLINE channel; create if missing; PUT combine as needed.
 * Call inside withEasyPayChannelLock when mutating combine.
 */
export async function ensureEasyPayChannel(
  fx: Pdt3205Fx,
  combineLiabilities: boolean,
): Promise<number> {
  let found = await findOnlineChannelByName(fx, EASY_PAY_CHANNEL_NAME);
  let channelId: number;
  if (!found) {
    channelId = await createNamedOnlineChannel(fx, {
      name: EASY_PAY_CHANNEL_NAME,
      combineLiabilities,
    });
  } else {
    channelId = found.id;
    await setEasyPayCombineLiabilities(fx, combineLiabilities, channelId);
  }
  const getRes = await fx.Request.get(`${fx.Endpoints.collectionChannel}/${channelId}`);
  await expect(getRes).CheckResponse();
  const channelBody = await getRes.json();
  expect(String(channelBody.name)).toBe(EASY_PAY_CHANNEL_NAME);
  expect(Boolean(channelBody.combineLiabilities)).toBe(combineLiabilities);
  fx.Responses.collectionChannel.push(channelId);
  return channelId;
}

export async function getCollectionChannelDetail(
  fx: Pdt3205Fx,
  channelId: number,
): Promise<any> {
  const res = await fx.Request.get(`${fx.Endpoints.collectionChannel}/${channelId}`);
  await expect(res).CheckResponse();
  return res.json();
}

export async function getPaymentPackageDetail(
  fx: Pdt3205Fx,
  packageId: number,
): Promise<any> {
  const res = await fx.Request.get(`${fx.Endpoints.paymentPackage}/${packageId}`);
  await expect(res).CheckResponse();
  return res.json();
}

/** yyyy-MM-dd calendar day from paymentDate / package paymentDate. */
export function calendarDayIso(value: unknown): string {
  const raw = String(value ?? '').trim();
  if (!raw) return '';
  if (/^\d{4}-\d{2}-\d{2}/.test(raw)) return raw.slice(0, 10);
  if (/^\d{8}/.test(raw)) {
    return `${raw.slice(0, 4)}-${raw.slice(4, 6)}-${raw.slice(6, 8)}`;
  }
  return raw.split('T')[0];
}

/** Build Virtual POS checksum-init URL (CHECK omits TID). */
export async function calculateVirtualPosInitChecksum(
  fx: Pdt3205Fx,
  opts: {
    customerNumber: string;
    merchantId: string;
    type: 'CHECK' | 'BILLING';
    tid?: string;
  },
): Promise<string> {
  let url =
    `${fx.OnlinePaymentUrl}virtualpos/calculate-check-sum-init` +
    `?IDN=${opts.customerNumber}&MERCHANTID=${opts.merchantId}&TYPE=${opts.type}`;
  if (opts.type === 'BILLING') {
    expect(opts.tid, 'TID required for BILLING checksum-init').toBeTruthy();
    url += `&TID=${opts.tid}`;
  }
  const res = await fx.Request.get(url);
  await expect(res).CheckResponse();
  return (await res.text()).trim();
}

export async function callVirtualPosInitPay(
  fx: Pdt3205Fx,
  opts: {
    customerNumber: string;
    merchantId: string;
    type: 'CHECK' | 'BILLING';
    checksum: string;
    tid?: string;
  },
): Promise<any> {
  let url =
    `${fx.OnlinePaymentUrl}virtualpos/init-pay` +
    `?IDN=${opts.customerNumber}&MERCHANTID=${opts.merchantId}` +
    `&TYPE=${opts.type}&CHECKSUM=${opts.checksum}`;
  if (opts.type === 'BILLING') {
    expect(opts.tid, 'TID required for BILLING init-pay').toBeTruthy();
    url += `&TID=${opts.tid}`;
  }
  const res = await fx.Request.get(url);
  await expect(res).CheckResponse();
  return res.json();
}

export async function calculateVirtualPosConfirmChecksum(
  fx: Pdt3205Fx,
  opts: {
    date: string;
    customerNumber: string;
    tid: string;
    merchantId: string;
    total: string | number;
    invoices?: string;
  },
): Promise<string> {
  let url =
    `${fx.OnlinePaymentUrl}virtualpos/calculate-check-sum-confirm` +
    `?DATE=${opts.date}&TYPE=BILLING&MERCHANTID=${opts.merchantId}` +
    `&IDN=${opts.customerNumber}&TOTAL=${opts.total}&TID=${opts.tid}`;
  if (opts.invoices) {
    url += `&INVOICES=${encodeURIComponent(opts.invoices)}`;
  }
  const res = await fx.Request.get(url);
  await expect(res).CheckResponse();
  return (await res.text()).trim();
}

export async function callVirtualPosConfirmPay(
  fx: Pdt3205Fx,
  opts: {
    date: string;
    customerNumber: string;
    tid: string;
    merchantId: string;
    total: string | number;
    checksum: string;
    invoices?: string;
  },
): Promise<any> {
  let url =
    `${fx.OnlinePaymentUrl}virtualpos/confirm-pay` +
    `?DATE=${opts.date}&TYPE=BILLING&MERCHANTID=${opts.merchantId}` +
    `&IDN=${opts.customerNumber}&TOTAL=${opts.total}&TID=${opts.tid}&CHECKSUM=${opts.checksum}`;
  if (opts.invoices) {
    url += `&INVOICES=${encodeURIComponent(opts.invoices)}`;
  }
  const res = await fx.Request.get(url);
  await expect(res).CheckResponse();
  return res.json();
}

/**
 * Virtual POS confirm wrapper (PDT-3214 callConfirmPayForLpf pattern): catches HTTP 400
 * package-not-applicable / paymentDate-outside after ONLINE package ensure, for soft-skip.
 * Does not soft-classify amount-incorrect.
 */
export async function callVirtualPosConfirmPayAllowingEnvBlockers(
  fx: Pdt3205Fx,
  opts: {
    date: string;
    customerNumber: string;
    tid: string;
    merchantId: string;
    total: string | number;
    checksum: string;
    invoices?: string;
  },
): Promise<ConfirmPayLpfResult> {
  let body: any;
  try {
    body = await callVirtualPosConfirmPay(fx, opts);
  } catch (err) {
    const detail = String((err as Error)?.message ?? err ?? '');
    if (isEasyPayConfirmEnvBlockerFailure(detail)) {
      const httpMatch = detail.match(/Status:\s*(\d+)/i);
      return {
        kind: 'payment_date_outside',
        detail,
        httpStatus: httpMatch ? Number(httpMatch[1]) : undefined,
      };
    }
    throw err;
  }

  const status = String(body?.STATUS ?? '');
  if (status === '00') {
    return { kind: 'ok', body };
  }

  const additionalInfo = String(body?.ADDITIONALINFO ?? '');
  const description = String(body?.DESCRIPTION ?? '');
  const detail =
    `STATUS=${body?.STATUS} ADDITIONALINFO=${additionalInfo || 'n/a'} ` +
    `DESCRIPTION=${description || 'n/a'} body=${JSON.stringify(body)}`;
  const combined = `${additionalInfo} ${description} ${JSON.stringify(body)}`;

  if (status === '96' && isEasyPayConfirmEnvBlockerFailure(combined)) {
    return { kind: 'payment_date_outside', detail, status };
  }

  return { kind: 'other_status', body, detail };
}

/** Combined-channel BILLING init + confirm (omit INVOICES). */
export async function virtualPosBillingInitAndConfirm(
  fx: Pdt3205Fx,
  opts: {
    customerNumber: string;
    merchantId?: string;
    confirmDate?: string;
    /** OPEN AP for ONLINE package ensure; defaults to today OPEN via confirm-date helper */
    accountingPeriodId?: number;
    channelId?: number;
    tid: string;
  },
): Promise<{ initBody: any; confirmBody: any; amount: string | number; date: string }> {
  const merchantId = opts.merchantId ?? (await resolveVirtualPosMerchantId(fx));
  const checksumInit = await calculateVirtualPosInitChecksum(fx, {
    customerNumber: opts.customerNumber,
    merchantId,
    type: 'BILLING',
    tid: opts.tid,
  });
  const initBody = await callVirtualPosInitPay(fx, {
    customerNumber: opts.customerNumber,
    merchantId,
    type: 'BILLING',
    tid: opts.tid,
    checksum: checksumInit,
  });
  const amount = initBody.AMOUNT;
  const date = opts.confirmDate ?? randomGens.generateOnlinePaymentDate(4);
  await ensureVirtualPosOnlinePaymentPackageForConfirmDate(fx, {
    confirmDate: date,
    accountingPeriodId: opts.accountingPeriodId,
    channelId: opts.channelId,
  });
  const checksumConfirm = await calculateVirtualPosConfirmChecksum(fx, {
    date,
    customerNumber: opts.customerNumber,
    tid: opts.tid,
    merchantId,
    total: amount,
  });
  const confirmBody = await callVirtualPosConfirmPay(fx, {
    date,
    customerNumber: opts.customerNumber,
    tid: opts.tid,
    merchantId,
    total: amount,
    checksum: checksumConfirm,
  });
  return { initBody, confirmBody, amount, date };
}

/** Corrupt last hex char of HMAC (or use all-zero fallback). */
export function corruptChecksum(checksum: string): string {
  const raw = String(checksum ?? '').trim();
  if (!raw) return '0000000000000000000000000000000000000000';
  const last = raw[raw.length - 1];
  const flip = last === '0' ? '1' : '0';
  return raw.slice(0, -1) + flip;
}

/**
 * LONGDESC invoice fragment helpers (EasyPay EPBStringUtils.formatSingleInvoice).
 * If document number contains `-`, use substring after first `-`.
 */
export function longDescDocFromInvoiceNumber(invoiceDocumentNumber: string): string {
  const n = String(invoiceDocumentNumber ?? '');
  const idx = n.indexOf('-');
  return idx >= 0 ? n.slice(idx + 1) : n;
}

/** Format invoice document date as dd.MM.yyyy for LONGDESC. */
export function formatLongDescDate(isoOrDate: string): string {
  const iso = calendarDayIso(isoOrDate);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    throw new Error(`Cannot format LONGDESC date from: ${isoOrDate}`);
  }
  const [y, m, d] = iso.split('-');
  return `${d}.${m}.${y}`;
}

/**
 * Pick invoice-like object from goods-order GET body (post issue-invoice shapes vary).
 * Prefer real invoice over proforma; supports invoices[], invoice, basicParametersResponse.invoice.
 */
function pickInvoiceFromGoodsOrderBody(body: any): any | null {
  const arrays = [body?.invoices, body?.invoiceList, body?.invoiceResponses];
  for (const arr of arrays) {
    if (!Array.isArray(arr)) continue;
    const hit = arr.find(
      (i: any) =>
        i &&
        (i.invoiceNumber || i.documentNumber || i.number || i.id || i.invoiceId),
    );
    if (hit) return hit;
  }
  if (body?.invoice) return body.invoice;
  const bp = body?.basicParametersResponse;
  if (bp?.invoice) return bp.invoice;
  if (body?.proformaInvoice) return body.proformaInvoice;
  if (bp?.proformaInvoice) return bp.proformaInvoice;
  return null;
}

function invoiceDocNumber(invoice: any): string {
  return String(invoice?.invoiceNumber ?? invoice?.documentNumber ?? invoice?.number ?? '').trim();
}

function invoiceDocDate(invoice: any): string {
  return String(
    invoice?.issueDate ??
      invoice?.documentDate ??
      invoice?.invoiceDate ??
      invoice?.paymentDeadline ??
      '',
  ).trim();
}

/**
 * After proforma + issue-invoice, poll goods-order (and invoice GET) for document number/date.
 * Returns null when invoice meta is still missing after poll (caller may soft-skip LONGDESC).
 */
export async function readGoodsOrderInvoiceMeta(
  fx: Pdt3205Fx,
  goodsOrderId: number,
  opts?: { liabilityId?: number; timeoutMs?: number },
): Promise<{
  invoiceDocumentNumber: string;
  invoiceDocumentDate: string;
  longDescDoc: string;
  expectedLongDescDate: string;
} | null> {
  const timeoutMs = opts?.timeoutMs ?? 90_000;
  const deadline = Date.now() + timeoutMs;

  while (Date.now() < deadline) {
    const res = await fx.Request.get(`${fx.Endpoints.goodsOrder}/${goodsOrderId}?version=1`);
    await expect(res).CheckResponse();
    const lastBody = await res.json();
    let invoice = pickInvoiceFromGoodsOrderBody(lastBody);
    const invoiceId = Number(invoice?.id ?? invoice?.invoiceId ?? 0);

    if (invoiceId > 0 && !invoiceDocNumber(invoice)) {
      const invRes = await fx.Request.get(`${fx.Endpoints.invoice}?id=${invoiceId}`);
      if (invRes.status() === 200) {
        const invBody = await invRes.json();
        invoice = { ...(invoice ?? {}), ...(invBody ?? {}) };
      }
    }

    // Liability may already expose document number after issue-invoice.
    if (opts?.liabilityId && (!invoice || !invoiceDocNumber(invoice) || !invoiceDocDate(invoice))) {
      try {
        const detail = await getLiabilityDetail(fx, opts.liabilityId);
        const fromLiab = {
          invoiceNumber:
            detail.invoiceNumber ??
            detail.documentNumber ??
            detail.invoiceResponse?.invoiceNumber ??
            detail.invoiceShortResponse?.invoiceNumber ??
            invoiceDocNumber(invoice),
          issueDate:
            detail.invoiceIssueDate ??
            detail.documentDate ??
            detail.invoiceResponse?.issueDate ??
            detail.invoiceResponse?.documentDate ??
            invoiceDocDate(invoice),
          id: detail.invoiceId ?? detail.invoiceResponse?.id ?? (invoiceId || undefined),
        };
        if (fromLiab.invoiceNumber || fromLiab.issueDate) {
          invoice = { ...(invoice ?? {}), ...fromLiab };
        }
      } catch {
        /* liability read optional during poll */
      }
    }

    const invoiceDocumentNumber = invoiceDocNumber(invoice);
    const invoiceDocumentDate = invoiceDocDate(invoice);
    if (invoiceDocumentNumber && invoiceDocumentDate) {
      const longDescDoc = longDescDocFromInvoiceNumber(invoiceDocumentNumber);
      const expectedLongDescDate = formatLongDescDate(invoiceDocumentDate);
      return { invoiceDocumentNumber, invoiceDocumentDate, longDescDoc, expectedLongDescDate };
    }

    await new Promise((r) => setTimeout(r, 1500));
  }

  return null;
}

export type { Pdt3214Fx };
