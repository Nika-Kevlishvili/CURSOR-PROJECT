/**
 * PDT-3214 helpers — EasyPay liabilities/receivables accounting-period rules (GE Description TO-BE).
 *
 * Authoritative expected behavior = Jira PDT-3214 GE Description (not old reproduce text):
 * For each in-scope object created by EasyPay/online payment:
 *   1) Determine driving date
 *   2) Find accounting period for that date
 *   3) If period OPEN at creation → assign that period
 *   4) If CLOSED / missing → assign current accounting period
 *
 * Driving dates:
 *   - Customer Payment → payment date (EasyPay confirm DATE)
 *   - Customer Receivable → occurrence date = online first/check (init) request date
 *   - LPF Customer Liability → occurrence date = check request date
 *
 * Therefore payment AP and receivable AP MAY differ when confirm DATE is in a prior OPEN month
 * but init/occurrence is today — that is correct under GE.
 *
 * Reference patterns:
 * - tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures.ts (EasyPay init/confirm)
 * - tests/receivableManagement/Payment/onlinePayment.spec.ts (REG-1036 / REG-1063 EasyPay+LPF)
 * - tests/cursor/pdt-2459-payment-reverse-lpf-offset.fixtures.ts (LPF → Customer Liability link)
 * - tests/receivableManagement/Payment/offlinePaymentCreateAndOffsetting.spec.ts (REG-1005 surplus)
 *
 * /epay/* is not in Phoenix core swagger — contract from payment-api + REG-1036 helpers.
 *
 * CLOSED periods: GET /accounting-period?status=CLOSED (Swagger AccountingPeriodsListingRequest).
 * OPEN periods: GET /billing-run/accounting-period-available-list + GET /accounting-period/{id}.
 *
 * Surplus strategy (PaymentOffsettingTransactionalService):
 * Reduce open liability between init and confirm (partial offline clear) so confirm amount
 * exceeds remaining liabilities → surplus RECEIVABLE. Prefer remaining > 0 (partial path)
 * over full clear (amount-0 offsetting can leave payment with no linked RECEIVABLE).
 *
 * Test 1 init AMOUNT (CRITICAL — Dev phoenix2-dev 2× quirk):
 * customer_liability() template sets applicableInterestRateId=env interest by default.
 * Surplus path MUST pass applicableInterestRateId=null so amount_to_pay stays principal-only.
 * Assert STATUS 00 + AMOUNT > 0; derive confirm TOTAL + expected surplus from actual init
 * AMOUNT and remaining after offline (do not hard-require AMOUNT === liability×100 — SP
 * amount_to_pay can differ by env). Still hard-fail if customer has ≠1 open liability before
 * init (setup wrong) or init STATUS≠00 / AMOUNT≤0.
 *
 * EasyPay confirm DATE — surplus vs LPF (CRITICAL):
 * - Surplus-only: priorOpenEasyPayDate is OK (no LPF accrual in init AMOUNT).
 * - LPF path: priorOpenEasyPayDate is FORBIDDEN. init-pay has no DATE → AMOUNT is always
 *   principal + LPF(due→now). Confirm with a mid-prior DATE recalculates LPF for that day →
 *   TOTAL mismatch → STATUS 96 "amount is incorrect". Prefer generateOnlinePaymentDate(4)
 *   (REG-1063), optionally todayOpenEasyPayDate; pass TOTAL as raw initBody.AMOUNT.
 *   Soft-skip env blockers via STATUS 96 OR HTTP 400 / APPLICATION_ERROR /
 *   ClientException:
 *     (a) "paymentDate … outside the accounting period"
 *     (b) "payment package … not applicable for selected payment date and collection channel"
 *       — only after ensureEasyPayOnlinePaymentPackageForDate (createOrFetch still rejects)
 *   Test multi-OPEN calendars may bind confirm DATE to a prior OPEN AP while today DATE is
 *   outside it. Never soft-skip "Invoice numbers are missing" / amount incorrect / init≠00.
 *   Shared EasyPay channel: withEasyPayChannelLock + ensureEasyPayCombineLiabilities +
 *   ensureEasyPayOnlinePaymentPackageForDate (ONLINE package for confirm calendar day).
 *
 * Test C (init in prior OPEN period ≠ today): NOT feasible via EasyPay API — init-pay has no DATE;
 * only confirm DATE is client-supplied. Documented as skipped (no DB time travel).
 *
 * CLOSED GE scenario (correct approach):
 * Do NOT EasyPay confirm-pay with DATE inside a CLOSED calendar month — payment-api rejects
 * "paymentDate outside the accounting period" and that is the wrong test (asserts Payment AP remap).
 * Instead: create offline Payment (REG-1005) with paymentDate in a CLOSED period (+ matching
 * accountPeriodId when API allows), overpay a liability → surplus Receivable, then assert
 * Receivable AP = current OPEN (not the closed period). Payment may keep old date / closed AP.
 *
 * OPEN prior GE scenario (offline counterpart of CLOSED):
 * Same REG-1005 offline overpay structure, but paymentDate falls in a prior calendar month that
 * is still OPEN (≠ today OPEN). Assert surplus Receivable AP = that prior OPEN (payment-date
 * period), not today's period — exploratory: RecordChecks show expected vs actual if runtime differs.
 *
 * Offline payment package: createCollectionChannelAndPaymentPackageValidForDate —
 * PaymentPackageCreateRequest.paymentDate must equal the offline paymentDate (exact match +
 * channelId + UNLOCKED). Package accountingPeriodId must be OPEN (create rejects CLOSED AP).
 * Do not reuse default createCollectionChannelAndPaymentPackage (paymentDate=today).
 *
 * EasyPay ONLINE package (surplus Test 1 / LPF Test 2):
 * ensureEasyPayOnlinePaymentPackageForDate — POST /payment-package type=ONLINE on the shared
 * EasyPay ONLINE channel with paymentDate = confirm calendar day + matching OPEN
 * accountingPeriodId + UNLOCKED. Mirrors PDT-3215 createOnlinePackageOnChannel so
 * PaymentCreationTransactionalService.createOrFetchPaymentPackage finds a package that
 * passes findPaymentPackageByIdAndCollectionChannelIdAndPaymentDateAndLockStatusIn.
 */

import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { expect } from '@playwright/test';
import { randomGens } from '../../utils/randomGens';
import { Endpoints } from '../../fixtures/constants/endpoints';
import {
  calculateConfirmChecksum,
  calculateInitChecksum,
  callConfirmPay,
  callInitPay,
  createCollectionChannelAndPaymentPackage,
  createLegalCustomer,
  createManualLiability,
  findEasyPayOnlineChannel,
  formatEasyPayStatusDetail,
  getLiabilityDetail,
  listPaymentsByCustomerIdentifier,
  resolveEasyPayMerchantId,
  setEasyPayCombineLiabilities,
  toCoinAmount,
  type Pdt3171Fx,
} from './pdt-3171-easypay-proforma-due-date-validto.fixtures';

export { formatEasyPayStatusDetail };

export type Pdt3214Fx = Pdt3171Fx;

/**
 * Shared EasyPay ONLINE channel is mutated by multiple PDT-3214 tests (combine true/false).
 * With fullyParallel + workers>1, Test 1 can set combine=false after Test 2 sets true but
 * before Test 2 init → EasyPayPaymentEntity.isCombined=false → confirm STATUS 96
 * "Invoice numbers are missing..." when TOTAL ≠ stored init coins.
 *
 * Prefer --workers=1 for this file; this lock still serializes the critical section when
 * workers>1. Do NOT use Playwright mode:'serial' (skips remaining tests on first failure).
 */
const EASYPAY_CHANNEL_LOCK_PATH = path.join(
  os.tmpdir(),
  'pdt-3214-easypay-channel.lock',
);
const EASYPAY_CHANNEL_LOCK_STALE_MS = 10 * 60 * 1000;
const EASYPAY_CHANNEL_LOCK_WAIT_MS = 3 * 60 * 1000;

export async function withEasyPayChannelLock<T>(fn: () => Promise<T>): Promise<T> {
  const started = Date.now();
  for (;;) {
    try {
      const fd = fs.openSync(EASYPAY_CHANNEL_LOCK_PATH, 'wx');
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
        const st = fs.statSync(EASYPAY_CHANNEL_LOCK_PATH);
        if (Date.now() - st.mtimeMs > EASYPAY_CHANNEL_LOCK_STALE_MS) {
          fs.unlinkSync(EASYPAY_CHANNEL_LOCK_PATH);
          continue;
        }
      } catch {
        /* lock vanished between EEXIST and stat — retry */
      }
      if (Date.now() - started > EASYPAY_CHANNEL_LOCK_WAIT_MS) {
        throw new Error(
          `EasyPay channel lock timeout after ${EASYPAY_CHANNEL_LOCK_WAIT_MS}ms ` +
            `(${EASYPAY_CHANNEL_LOCK_PATH}). Prefer --workers=1 for PDT-3214.`,
        );
      }
      await new Promise((r) => setTimeout(r, 150 + Math.floor(Math.random() * 200)));
    }
  }
  try {
    return await fn();
  } finally {
    try {
      fs.unlinkSync(EASYPAY_CHANNEL_LOCK_PATH);
    } catch {
      /* ignore */
    }
  }
}

/** Serialize unknown errors / EasyPay bodies for paymentDate-outside detection. */
export function errorTextFromUnknown(err: unknown): string {
  if (err instanceof Error) {
    return `${err.name}: ${err.message}${err.stack ? `\n${err.stack}` : ''}`;
  }
  if (typeof err === 'string') return err;
  try {
    return JSON.stringify(err);
  } catch {
    return String(err);
  }
}

/**
 * True only for paymentDate / accounting-period outside failures.
 * Explicitly false for amount-incorrect and invoice-numbers-missing (hard-fail those).
 */
export function isPaymentDateOutsideAccountingPeriodFailure(text: string): boolean {
  const msg = String(text ?? '').toLowerCase();
  if (!msg.trim()) return false;
  if (
    msg.includes('amount is incorrect') ||
    msg.includes('amount incorrect') ||
    msg.includes('invoice numbers are missing')
  ) {
    return false;
  }
  const hasPaymentDate =
    msg.includes('paymentdate') ||
    msg.includes('payment date') ||
    msg.includes('paymentdate-');
  const hasOutsideAp =
    msg.includes('outside the accounting period') ||
    (msg.includes('outside') && msg.includes('accounting period'));
  return hasPaymentDate && hasOutsideAp;
}

/**
 * True when payment-api / PaymentService rejects package applicability for DATE + channel
 * (exact match of package.paymentDate + channelId + UNLOCKED).
 * Soft-skip only AFTER ensureEasyPayOnlinePaymentPackageForDate was attempted.
 * Never soft-skip amount-incorrect / invoice-numbers-missing.
 */
export function isPaymentPackageNotApplicableFailure(text: string): boolean {
  const msg = String(text ?? '').toLowerCase();
  if (!msg.trim()) return false;
  if (
    msg.includes('amount is incorrect') ||
    msg.includes('amount incorrect') ||
    msg.includes('invoice numbers are missing')
  ) {
    return false;
  }
  const hasPackage =
    msg.includes('paymentpackageid') ||
    msg.includes('payment package') ||
    msg.includes('paymentpackage');
  const notApplicable =
    msg.includes('not applicable') &&
    (msg.includes('payment date') ||
      msg.includes('paymentdate') ||
      msg.includes('collection channel'));
  return hasPackage && notApplicable;
}

/**
 * Soft-skippable EasyPay confirm env blockers (after package pre-create when applicable).
 * Hard-fails amount-incorrect / invoice-numbers-missing.
 */
export function isEasyPayConfirmEnvBlockerFailure(text: string): boolean {
  return (
    isPaymentDateOutsideAccountingPeriodFailure(text) ||
    isPaymentPackageNotApplicableFailure(text)
  );
}

export type ConfirmPayLpfResult =
  | { kind: 'ok'; body: any }
  | { kind: 'payment_date_outside'; detail: string; httpStatus?: number; status?: string }
  | { kind: 'other_status'; body: any; detail: string };

/**
 * LPF / surplus EasyPay confirm wrapper: catches HTTP 400 from CheckResponse before STATUS
 * handling, and maps STATUS 96 paymentDate/AP-outside OR package-not-applicable to a
 * soft-skipable result (kind payment_date_outside — shared env-blocker path).
 * Does not soft-classify amount-incorrect / invoice-numbers-missing.
 */
export async function callConfirmPayForLpf(
  fx: Pdt3214Fx,
  opts: {
    date: string;
    customerNumber: string;
    tid: string;
    merchantId: string;
    total: string | number;
    checksum: string;
  },
): Promise<ConfirmPayLpfResult> {
  let body: any;
  try {
    body = await callConfirmPay(fx, opts);
  } catch (err) {
    const detail = errorTextFromUnknown(err);
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

/**
 * REG-1063-style: push EasyPay channel, env-safe GET→PUT for combineLiabilities,
 * then re-GET and assert combineLiabilities === expected.
 * Call inside withEasyPayChannelLock immediately before init-pay.
 *
 * Does NOT call CheckCombinedOnChannel — that helper uses Responses.collectionChannel[0]
 * (often the offline channel from Test 1) and Dev-hardcoded easyPayCollectionChannel()
 * which can corrupt Test/Dev2 EasyPay channels.
 */
export async function ensureEasyPayCombineLiabilities(
  fx: Pdt3214Fx,
  combineLiabilities: boolean,
): Promise<number> {
  const easyPayChannelId = await findEasyPayOnlineChannel(fx);
  fx.Responses.collectionChannel.push(easyPayChannelId);

  // Env-safe GET→PUT (preserves partner/currency/employee; only overrides combine).
  await setEasyPayCombineLiabilities(fx, combineLiabilities, easyPayChannelId);

  const getRes = await fx.Request.get(
    `${fx.Endpoints.collectionChannel}/${easyPayChannelId}`,
  );
  await expect(getRes).CheckResponse();
  const channelBody = await getRes.json();
  expect(
    Boolean(channelBody.combineLiabilities),
    `EasyPay channel ${easyPayChannelId} combineLiabilities must be ${combineLiabilities} before init`,
  ).toBe(combineLiabilities);

  return easyPayChannelId;
}

export type AccountingPeriodInfo = {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
};

export type PriorOpenPeriodPlan = {
  priorPeriod: AccountingPeriodInfo;
  todayPeriod: AccountingPeriodInfo;
  /** EasyPay confirm DATE = YYYYMMDDHHmmss mid-day inside priorPeriod */
  easyPayPaymentDate: string;
  /** yyyy-mm-dd mid date used for period selection (payment driving date) */
  paymentDateIso: string;
  /** yyyy-mm-dd local "today" — EasyPay init / receivable occurrence driving date */
  initDateIso: string;
};

/**
 * CLOSED period before today + current OPEN (today).
 * Used for offline Payment with old paymentDate (not EasyPay CLOSED-DATE confirm).
 */
export type ClosedPeriodPaymentPlan = {
  closedPeriod: AccountingPeriodInfo;
  todayPeriod: AccountingPeriodInfo;
  /** EasyPay DATE format kept for diagnostics only — CLOSED test must not confirm with this. */
  easyPayPaymentDate: string;
  /** yyyy-mm-dd mid date inside CLOSED period (offline paymentDate) */
  paymentDateIso: string;
  initDateIso: string;
};

/**
 * Environment-agnostic accounting-period plan (GET OPEN + CLOSED on target env).
 * Soft nulls for prior/closed when missing — callers test.skip with a clear message.
 *
 * Endpoints:
 * - OPEN: GET billing-run/accounting-period-available-list + GET accounting-period/{id}
 * - CLOSED: GET accounting-period?status=CLOSED
 */
export type EnvAccountingPeriodContext = {
  todayIso: string;
  /** OPEN period containing today (verified GET by id) */
  todayOpenPeriod: AccountingPeriodInfo;
  /** Most recent OPEN with endDate < today, ≠ todayOpenPeriod */
  priorOpenPeriod: AccountingPeriodInfo | null;
  /** Most recent CLOSED with endDate < today */
  closedPeriod: AccountingPeriodInfo | null;
  /** Mid of today OPEN (yyyy-mm-dd) — diagnostic only; may be after todayIso */
  todayOpenMidIso: string;
  /**
   * Safe yyyy-mm-dd for EasyPay "current" confirm: min(todayOpenMidIso, todayIso).
   * Always inside today OPEN and never after today (avoids EasyPay STATUS 96 future DATE).
   */
  todayOpenSafeIso: string;
  /**
   * EasyPay DATE for today-clamped path: isoToEasyPayDate(todayOpenSafeIso, 0, 0, 1).
   * Uses 00:00:01 (not noon) so wall-clock runs before 12:00 do not get STATUS 96.
   */
  todayOpenEasyPayDate: string;
  /** Mid of prior OPEN, or null */
  priorOpenMidIso: string | null;
  /** EasyPay DATE mid prior OPEN — surplus-only confirm (Test 1). Forbidden for LPF (Test 2). */
  priorOpenEasyPayDate: string | null;
  /** Mid of CLOSED period — offline CLOSED test paymentDate */
  closedMidIso: string | null;
};

/** Result of creating offline payment with a historical paymentDate (surplus path). */
export type OfflineHistoricalPaymentResult = {
  paymentId: number;
  paymentDetail: any;
  /** AP written on the Payment entity after create */
  paymentAccountPeriodId: number | null;
  /**
   * preferred_ap = CreatePaymentRequest.accountPeriodId = preferredPeriodId succeeded.
   * fallback_ap = preferred AP rejected; retried with fallbackPeriodId, same paymentDate.
   */
  createMode: 'preferred_ap' | 'fallback_ap';
  createFailureDetail?: string;
};

/** Result of creating offline payment dated in a CLOSED period (surplus path). */
export type ClosedPeriodOfflinePaymentResult = {
  paymentId: number;
  paymentDetail: any;
  paymentAccountPeriodId: number | null;
  /**
   * closed_ap = CreatePaymentRequest.accountPeriodId = CLOSED period succeeded.
   * current_ap_fallback = API rejected closed AP; retried with current OPEN AP,
   * still keeping paymentDate inside the CLOSED period range.
   */
  createMode: 'closed_ap' | 'current_ap_fallback';
  createFailureDetail?: string;
};

export type EasyPayLpfLiabilityHit = {
  lpfId: number;
  lpfAmount: number;
  liabilityId: number;
  liabilityDetail: any;
};

/**
 * GE rule: period for drivingDate if OPEN; else current accounting period.
 * Uses available OPEN list + GET /accounting-period/{id} (no hardcoded ids).
 */
export async function resolveExpectedAccountingPeriodForDrivingDate(
  fx: Pdt3214Fx,
  drivingDateIso: string,
  currentPeriod: AccountingPeriodInfo,
): Promise<{ expectedPeriod: AccountingPeriodInfo; reason: string }> {
  const openPeriods = await listOpenAccountingPeriods(fx);
  const openForDate = openPeriods.find((p) => containsDate(p, drivingDateIso));
  if (openForDate) {
    const verified = await getAccountingPeriodById(fx, openForDate.id);
    expect(String(verified.status)).toBe('OPEN');
    return {
      expectedPeriod: verified,
      reason: `OPEN period containing drivingDate=${drivingDateIso}`,
    };
  }
  return {
    expectedPeriod: currentPeriod,
    reason:
      `No OPEN period contains drivingDate=${drivingDateIso} → current period ` +
      `${currentPeriod.name}#${currentPeriod.id}`,
  };
}

export type SurplusReceivableHit = {
  id: number;
  amount: number;
  source: 'payment_offsetting' | 'customer_receivable_list';
};

export {
  calculateConfirmChecksum,
  calculateInitChecksum,
  callConfirmPay,
  callInitPay,
  createCollectionChannelAndPaymentPackage,
  createLegalCustomer,
  createManualLiability,
  findEasyPayOnlineChannel,
  formatEasyPayStatusDetail,
  getLiabilityDetail,
  listPaymentsByCustomerIdentifier,
  resolveEasyPayMerchantId,
  setEasyPayCombineLiabilities,
  toCoinAmount,
};

/**
 * List open customer liabilities (currentAmount > 0) for surplus Test 1 setup checks.
 * Uses GET /customer-liability/list by CUSTOMER identifier.
 */
export async function listOpenCustomerLiabilitiesByIdentifier(
  fx: Pdt3214Fx,
  customerIdentifier: string,
): Promise<Array<{ id: number; currentAmount: number; initialAmount: number }>> {
  const listRes = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC` +
      `&prompt=${encodeURIComponent(customerIdentifier)}&searchFields=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const listBody = await listRes.json();
  const rows = (listBody.content ?? []) as Array<{ id?: number }>;
  const open: Array<{ id: number; currentAmount: number; initialAmount: number }> = [];
  for (const row of rows) {
    const id = Number(row.id);
    if (!Number.isFinite(id) || id <= 0) continue;
    const detail = await getLiabilityDetail(fx, id);
    if (Number(detail.currentAmount) > 0) {
      open.push({
        id,
        currentAmount: Number(detail.currentAmount),
        initialAmount: Number(detail.initialAmount),
      });
    }
  }
  return open;
}

/**
 * Collection channel + payment package applicable for a specific offline paymentDate.
 *
 * PaymentPackageCreateRequest (Swagger): channelId, accountingPeriodId (must be OPEN),
 * lockStatus, paymentDate. Applicability on POST /payment is exact match of
 * package.paymentDate + package.channelId + UNLOCKED
 * (PaymentService.findPaymentPackageByIdAndCollectionChannelIdAndPaymentDateAndLockStatusIn) —
 * there is no validFrom/validTo window.
 *
 * Use for CLOSED-period offline payments whose paymentDate is far from "today"
 * (default createCollectionChannelAndPaymentPackage only sets paymentDate=today).
 *
 * @param paymentDateIso yyyy-mm-dd — must equal CreatePaymentRequest.paymentDate later
 * @param accountingPeriodId OPEN period id (package create rejects CLOSED APs)
 */
export async function createCollectionChannelAndPaymentPackageValidForDate(
  fx: Pdt3214Fx,
  opts: { paymentDateIso: string; accountingPeriodId: number },
): Promise<{ collectionChannelId: number; paymentPackageId: number }> {
  expect(
    opts.paymentDateIso,
    'paymentDateIso for date-valid payment package',
  ).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  expect(opts.accountingPeriodId, 'OPEN accountingPeriodId for payment package').toBeGreaterThan(0);

  const channelRes = await fx.Request.post(fx.Endpoints.collectionChannel, {
    data: fx.GeneratePayload.receivablesManagement.collection_channel(),
  });
  await expect(channelRes).CheckResponse();
  const collectionChannelId = (await channelRes.json()) as number;
  fx.Responses.collectionChannel.push(collectionChannelId);

  const packagePayload = fx.GeneratePayload.receivablesManagement.payment_package();
  packagePayload.channelId = collectionChannelId;
  packagePayload.paymentDate = opts.paymentDateIso;
  packagePayload.accountingPeriodId = opts.accountingPeriodId;
  packagePayload.lockStatus = 'UNLOCKED';

  const packageRes = await fx.Request.post(fx.Endpoints.paymentPackage, {
    data: packagePayload,
  });
  await expect(packageRes).CheckResponse();
  const paymentPackageId = (await packageRes.json()) as number;
  fx.Responses.paymentPackage.push(paymentPackageId);

  return { collectionChannelId, paymentPackageId };
}

export type EasyPayOnlinePackageEnsureResult = {
  channelId: number;
  paymentPackageId: number | null;
  /** true when POST /payment-package created a new row */
  created: boolean;
  /** Present when POST did not succeed (package may already exist for date+channel). */
  detail?: string;
};

/**
 * Ensure an ONLINE UNLOCKED payment package exists on the EasyPay ONLINE channel for the
 * confirm calendar day + OPEN accountingPeriodId (Swagger PaymentPackageCreateRequest).
 *
 * Why: PaymentCreationTransactionalService.createOrFetchPaymentPackage looks up by
 * paymentDate + channelId + UNLOCKED; if missing it creates one. On some Test calendars
 * an existing/mismatched package causes HTTP 400
 * "paymentPackageId … not applicable for selected payment date and collection channel".
 * Pre-creating ONLINE type=ONLINE (PDT-3215 createOnlinePackageOnChannel pattern) makes
 * createOrFetch find a package that passes
 * findPaymentPackageByIdAndCollectionChannelIdAndPaymentDateAndLockStatusIn.
 *
 * Call inside withEasyPayChannelLock before confirm-pay. Idempotent: if POST fails
 * (duplicate date+channel), returns created=false — createOrFetch may still reuse existing.
 *
 * @param paymentDateIso yyyy-mm-dd — must equal EasyPay confirm calendar day
 * @param accountingPeriodId OPEN period containing paymentDateIso (prior OPEN for Test 1)
 * @param channelId optional; defaults to findEasyPayOnlineChannel
 */
export async function ensureEasyPayOnlinePaymentPackageForDate(
  fx: Pdt3214Fx,
  opts: {
    paymentDateIso: string;
    accountingPeriodId: number;
    channelId?: number;
  },
): Promise<EasyPayOnlinePackageEnsureResult> {
  expect(
    opts.paymentDateIso,
    'paymentDateIso for EasyPay ONLINE payment package',
  ).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  expect(
    opts.accountingPeriodId,
    'OPEN accountingPeriodId for EasyPay ONLINE payment package',
  ).toBeGreaterThan(0);

  const channelId =
    opts.channelId != null && opts.channelId > 0
      ? opts.channelId
      : await findEasyPayOnlineChannel(fx);
  expect(channelId, 'EasyPay ONLINE channel id').toBeGreaterThan(0);
  fx.Responses.collectionChannel.push(channelId);

  const packagePayload = fx.GeneratePayload.receivablesManagement.payment_package();
  packagePayload.channelId = channelId;
  packagePayload.paymentDate = opts.paymentDateIso;
  packagePayload.accountingPeriodId = opts.accountingPeriodId;
  packagePayload.lockStatus = 'UNLOCKED';
  (packagePayload as { type?: string }).type = 'ONLINE';

  const packageRes = await fx.Request.post(fx.Endpoints.paymentPackage, {
    data: packagePayload,
  });

  if (packageRes.ok()) {
    const paymentPackageId = (await packageRes.json()) as number;
    expect(paymentPackageId, 'EasyPay ONLINE payment package id').toBeGreaterThan(0);
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
 * Soft resolve for CLOSED payment-date plan — returns null when env has no usable CLOSED period
 * (caller should test.skip). Prefer this over hard-failing the suite on env data gaps.
 * Builds on resolveEnvAccountingPeriodContext (one GET strategy for all tests).
 */
export async function tryResolveClosedPeriodPaymentPlan(
  fx: Pdt3214Fx,
): Promise<ClosedPeriodPaymentPlan | null> {
  const ctx = await resolveEnvAccountingPeriodContext(fx);
  if (!ctx.closedPeriod || !ctx.closedMidIso) return null;
  return {
    closedPeriod: ctx.closedPeriod,
    todayPeriod: ctx.todayOpenPeriod,
    paymentDateIso: ctx.closedMidIso,
    easyPayPaymentDate: isoToEasyPayDate(ctx.closedMidIso),
    initDateIso: ctx.todayIso,
  };
}

/**
 * REG-1005-style offline Payment with a historical paymentDate + preferred accountPeriodId.
 *
 * Tries preferredAccountPeriodId first; if API rejects, retries with fallbackAccountPeriodId
 * while keeping the same paymentDate. Used by CLOSED and OPEN-prior surplus scenarios.
 */
export async function createOfflinePaymentWithHistoricalPaymentDate(
  fx: Pdt3214Fx,
  opts: {
    paymentDateIso: string;
    preferredAccountPeriodId: number;
    fallbackAccountPeriodId: number;
    initialAmount: number;
  },
): Promise<OfflineHistoricalPaymentResult> {
  const buildPayload = async (accountPeriodId: number) => {
    const payload = await fx.GeneratePayload.receivablesManagement.payment();
    payload.initialAmount = opts.initialAmount;
    payload.paymentDate = opts.paymentDateIso;
    payload.accountPeriodId = accountPeriodId;
    payload.collectionChannelId = channelIdFromResponses(fx);
    payload.paymentPackageId = paymentPackageIdFromResponses(fx);
    payload.customerId = fx.Responses.customer[0].id;
    payload.blockedForOffsetting = false;
    return payload;
  };

  const preferredPayload = await buildPayload(opts.preferredAccountPeriodId);
  let res = await fx.Request.post(fx.Endpoints.payment, { data: preferredPayload });
  let createMode: OfflineHistoricalPaymentResult['createMode'] = 'preferred_ap';
  let createFailureDetail: string | undefined;

  if (!res.ok()) {
    createFailureDetail =
      `preferred accountPeriodId=${opts.preferredAccountPeriodId} rejected HTTP=${res.status()} ` +
      `body=${(await res.text()).slice(0, 500)}`;
    const fallbackPayload = await buildPayload(opts.fallbackAccountPeriodId);
    res = await fx.Request.post(fx.Endpoints.payment, { data: fallbackPayload });
    createMode = 'fallback_ap';
  }

  await expect(res).CheckResponse();
  const paymentId = (await res.json()) as number;
  expect(paymentId, 'offline payment id (historical paymentDate)').toBeGreaterThan(0);
  fx.Responses.payment.push(paymentId);

  const paymentDetail = await getPaymentDetail(fx, paymentId);
  return {
    paymentId,
    paymentDetail,
    paymentAccountPeriodId: extractAccountPeriodId(paymentDetail),
    createMode,
    createFailureDetail,
  };
}

/**
 * REG-1005-style offline Payment with paymentDate inside a CLOSED period.
 *
 * Prefer CreatePaymentRequest.accountPeriodId = closed period (matching historical AP).
 * If API rejects assigning CLOSED AP, retry with current OPEN AP while keeping the old
 * paymentDate — document createMode. Do not use EasyPay confirm DATE in a CLOSED month
 * (that hits paymentDate-outside-AP validation and is the wrong CLOSED GE scenario).
 *
 * Overpay liability (initialAmount > liability) so offsetting creates surplus RECEIVABLE
 * whose AP is the GE assert target (current OPEN when payment-date period is CLOSED).
 */
export async function createOfflinePaymentWithClosedPeriodDate(
  fx: Pdt3214Fx,
  opts: {
    paymentDateIso: string;
    closedPeriodId: number;
    currentOpenPeriodId: number;
    initialAmount: number;
  },
): Promise<ClosedPeriodOfflinePaymentResult> {
  const result = await createOfflinePaymentWithHistoricalPaymentDate(fx, {
    paymentDateIso: opts.paymentDateIso,
    preferredAccountPeriodId: opts.closedPeriodId,
    fallbackAccountPeriodId: opts.currentOpenPeriodId,
    initialAmount: opts.initialAmount,
  });
  return {
    paymentId: result.paymentId,
    paymentDetail: result.paymentDetail,
    paymentAccountPeriodId: result.paymentAccountPeriodId,
    createMode: result.createMode === 'preferred_ap' ? 'closed_ap' : 'current_ap_fallback',
    createFailureDetail: result.createFailureDetail,
  };
}

function toDateOnly(value: string | undefined | null): string {
  return String(value ?? '').slice(0, 10);
}

function channelIdFromResponses(fx: Pdt3214Fx): number {
  const raw = fx.Responses.collectionChannel[0];
  const id = typeof raw === 'number' ? raw : Number(raw?.id);
  expect(id, 'collection channel id').toBeGreaterThan(0);
  return id;
}

function paymentPackageIdFromResponses(fx: Pdt3214Fx): number {
  const raw = fx.Responses.paymentPackage[0];
  const id = typeof raw === 'number' ? raw : Number(raw?.id);
  expect(id, 'payment package id').toBeGreaterThan(0);
  return id;
}

function periodFromRow(row: Record<string, any>): AccountingPeriodInfo | null {
  const id = Number(row.id ?? row.accountPeriodId);
  const startDate = toDateOnly(row.startDate);
  const endDate = toDateOnly(row.endDate);
  if (!Number.isFinite(id) || !startDate || !endDate) return null;
  return {
    id,
    name: String(row.name ?? ''),
    startDate,
    endDate,
    status: String(row.status ?? 'OPEN'),
  };
}

function containsDate(period: AccountingPeriodInfo, isoDate: string): boolean {
  return isoDate >= period.startDate && isoDate <= period.endDate;
}

function midDateIso(period: AccountingPeriodInfo): string {
  const startMs = Date.parse(`${period.startDate}T12:00:00`);
  const endMs = Date.parse(`${period.endDate}T12:00:00`);
  const mid = new Date(Math.floor((startMs + endMs) / 2));
  const yyyy = mid.getFullYear();
  const mm = String(mid.getMonth() + 1).padStart(2, '0');
  const dd = String(mid.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

/** Convert yyyy-mm-dd → EasyPay confirm DATE (YYYYMMDDHHmmss). */
export function isoToEasyPayDate(isoDate: string, hour = 12, minute = 0, second = 0): string {
  const [y, m, d] = isoDate.split('-');
  return `${y}${m}${d}${String(hour).padStart(2, '0')}${String(minute).padStart(2, '0')}${String(second).padStart(2, '0')}`;
}

/** Add (or subtract) calendar days to a yyyy-mm-dd ISO date. */
export function addDaysIso(isoDate: string, days: number): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().split('T')[0];
}

/** yyyy-mm-dd → dd-mm-yyyy (customer liability dueDate / occurrenceDate). */
export function isoToDdMmYyyy(isoDate: string): string {
  const [y, m, d] = isoDate.split('-');
  return `${d}-${m}-${y}`;
}

/**
 * EasyPay confirm DATE (YYYYMMDDHHmmss…) → calendar day yyyy-mm-dd.
 * Uses the first 8 characters only (time is ignored).
 */
export function confirmDayIsoFromEasyPayDate(easyPayDate: string): string {
  const raw = String(easyPayDate ?? '').replace(/\D/g, '');
  expect(raw.length, `EasyPay DATE must start with YYYYMMDD: ${easyPayDate}`).toBeGreaterThanOrEqual(8);
  return `${raw.slice(0, 4)}-${raw.slice(4, 6)}-${raw.slice(6, 8)}`;
}

export function todayIsoLocal(): string {
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  const dd = String(now.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

/** List OPEN accounting periods (API — no hardcoded Dev ids). */
export async function listOpenAccountingPeriods(fx: Pdt3214Fx): Promise<AccountingPeriodInfo[]> {
  const listRes = await fx.Request.get(
    'billing-run/accounting-period-available-list?page=0&size=50',
  );
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  const rows = (body.content ?? []) as Array<Record<string, any>>;
  const periods: AccountingPeriodInfo[] = [];
  for (const row of rows) {
    if (String(row.status) !== 'OPEN') continue;
    const period = periodFromRow(row);
    if (period) periods.push(period);
  }
  periods.sort((a, b) => b.startDate.localeCompare(a.startDate));
  return periods;
}

export async function getAccountingPeriodById(
  fx: Pdt3214Fx,
  periodId: number,
): Promise<AccountingPeriodInfo> {
  const res = await fx.Request.get(`accounting-period/${periodId}`);
  await expect(res).CheckResponse();
  const body = await res.json();
  const period = periodFromRow(body);
  expect(period, `accounting-period/${periodId} must resolve`).toBeTruthy();
  return period!;
}

/**
 * Resolve OPEN + CLOSED accounting periods on the target env (no hardcoded ids / calendar assumptions).
 * Soft nulls for priorOpen / closed when the env has no usable periods — callers test.skip.
 */
export async function resolveEnvAccountingPeriodContext(
  fx: Pdt3214Fx,
): Promise<EnvAccountingPeriodContext> {
  const todayIso = todayIsoLocal();
  const openPeriods = await listOpenAccountingPeriods(fx);
  expect(
    openPeriods.length,
    `Target env must have at least one OPEN accounting period (today=${todayIso})`,
  ).toBeGreaterThan(0);

  const todayHit = openPeriods.find((p) => containsDate(p, todayIso));
  expect(
    todayHit,
    `No OPEN period contains today=${todayIso}. Available OPEN: ` +
      openPeriods.map((p) => `${p.name}#${p.id}[${p.startDate}..${p.endDate}]`).join(', '),
  ).toBeTruthy();

  const todayOpenPeriod = await getAccountingPeriodById(fx, todayHit!.id);
  expect(String(todayOpenPeriod.status)).toBe('OPEN');
  expect(
    containsDate(todayOpenPeriod, todayIso),
    `Verified today OPEN ${todayOpenPeriod.name}#${todayOpenPeriod.id} must contain ${todayIso}`,
  ).toBeTruthy();

  const todayOpenMidIso = midDateIso(todayOpenPeriod);
  expect(
    containsDate(todayOpenPeriod, todayOpenMidIso),
    `todayOpenMidIso ${todayOpenMidIso} must fall in today OPEN ${todayOpenPeriod.name}`,
  ).toBeTruthy();

  // EasyPay rejects future DATE (STATUS 96). Prefer mid of today OPEN when ≤ today; else todayIso.
  // Both candidates are inside today OPEN (todayIso by definition; mid by construction).
  const todayOpenSafeIso =
    todayOpenMidIso <= todayIso ? todayOpenMidIso : todayIso;
  expect(
    containsDate(todayOpenPeriod, todayOpenSafeIso),
    `todayOpenSafeIso ${todayOpenSafeIso} must fall in today OPEN ${todayOpenPeriod.name}`,
  ).toBeTruthy();
  expect(
    todayOpenSafeIso <= todayIso,
    `todayOpenSafeIso ${todayOpenSafeIso} must not exceed todayIso=${todayIso}`,
  ).toBeTruthy();

  // Today path: use 00:00:01 — default noon (12:00:00) is still "future" when run before midday.
  // Prior OPEN mid dates keep default noon (historical calendar days).
  const todayOpenEasyPayDate = isoToEasyPayDate(todayOpenSafeIso, 0, 0, 1);

  const priorCandidates = openPeriods
    .filter((p) => p.id !== todayOpenPeriod.id && p.endDate < todayIso)
    .sort((a, b) => b.endDate.localeCompare(a.endDate));

  let priorOpenPeriod: AccountingPeriodInfo | null = null;
  let priorOpenMidIso: string | null = null;
  let priorOpenEasyPayDate: string | null = null;
  if (priorCandidates.length) {
    const verified = await getAccountingPeriodById(fx, priorCandidates[0].id);
    if (
      String(verified.status) === 'OPEN' &&
      verified.id !== todayOpenPeriod.id &&
      verified.endDate < todayIso
    ) {
      const mid = midDateIso(verified);
      if (containsDate(verified, mid) && !containsDate(todayOpenPeriod, mid)) {
        priorOpenPeriod = verified;
        priorOpenMidIso = mid;
        priorOpenEasyPayDate = isoToEasyPayDate(mid);
      }
    }
  }

  const closedPeriods = await listClosedAccountingPeriods(fx);
  const closedCandidates = closedPeriods
    .filter((p) => p.endDate < todayIso && !containsDate(p, todayIso) && p.id !== todayOpenPeriod.id)
    .sort((a, b) => b.endDate.localeCompare(a.endDate));

  let closedPeriod: AccountingPeriodInfo | null = null;
  let closedMidIso: string | null = null;
  if (closedCandidates.length) {
    const verified = await getAccountingPeriodById(fx, closedCandidates[0].id);
    if (String(verified.status) === 'CLOSED' && verified.id !== todayOpenPeriod.id) {
      const mid = midDateIso(verified);
      if (containsDate(verified, mid) && !containsDate(todayOpenPeriod, mid)) {
        closedPeriod = verified;
        closedMidIso = mid;
      }
    }
  }

  return {
    todayIso,
    todayOpenPeriod,
    priorOpenPeriod,
    closedPeriod,
    todayOpenMidIso,
    todayOpenSafeIso,
    todayOpenEasyPayDate,
    priorOpenMidIso,
    priorOpenEasyPayDate,
    closedMidIso,
  };
}

/**
 * Pick a previous calendar period that is still OPEN, while "today" falls in a later OPEN period.
 * EasyPay payment DATE is placed mid-period so payment AP ≠ now AP.
 * Builds on resolveEnvAccountingPeriodContext.
 */
export async function resolvePriorOpenPeriodPlan(fx: Pdt3214Fx): Promise<PriorOpenPeriodPlan> {
  const ctx = await resolveEnvAccountingPeriodContext(fx);
  expect(
    ctx.priorOpenPeriod && ctx.priorOpenMidIso && ctx.priorOpenEasyPayDate,
    `Need a prior OPEN period before today=${ctx.todayIso} ` +
      `(todayPeriod=${ctx.todayOpenPeriod.name}#${ctx.todayOpenPeriod.id}). ` +
      'If all prior months are CLOSED, this scenario cannot run on this env.',
  ).toBeTruthy();

  return {
    priorPeriod: ctx.priorOpenPeriod!,
    todayPeriod: ctx.todayOpenPeriod,
    paymentDateIso: ctx.priorOpenMidIso!,
    easyPayPaymentDate: ctx.priorOpenEasyPayDate!,
    initDateIso: ctx.todayIso,
  };
}

/**
 * Soft resolve for prior OPEN payment-date plan — returns null when env has no prior OPEN
 * (caller should test.skip). Builds on resolveEnvAccountingPeriodContext.
 */
export async function tryResolvePriorOpenPeriodPlan(
  fx: Pdt3214Fx,
): Promise<PriorOpenPeriodPlan | null> {
  const ctx = await resolveEnvAccountingPeriodContext(fx);
  if (!ctx.priorOpenPeriod || !ctx.priorOpenMidIso || !ctx.priorOpenEasyPayDate) return null;
  return {
    priorPeriod: ctx.priorOpenPeriod,
    todayPeriod: ctx.todayOpenPeriod,
    paymentDateIso: ctx.priorOpenMidIso,
    easyPayPaymentDate: ctx.priorOpenEasyPayDate,
    initDateIso: ctx.todayIso,
  };
}

/**
 * Partially reduce liability so EasyPay confirm (init TOTAL) overpays remaining open amount.
 * Leaves remainingCurrentAmount > 0 so PaymentOffsettingTransactionalService uses the
 * checked>current path (surplus = confirm − remaining), not amount-0 full-clear edge case.
 */
export async function reduceLiabilityWithOfflinePayment(
  fx: Pdt3214Fx,
  opts: {
    liabilityId: number;
    offlineAmount: number;
    expectedRemainingCurrentAmount: number;
  },
): Promise<number> {
  const payload = await fx.GeneratePayload.receivablesManagement.payment();
  payload.initialAmount = opts.offlineAmount;
  payload.collectionChannelId = channelIdFromResponses(fx);
  payload.paymentPackageId = paymentPackageIdFromResponses(fx);
  payload.customerId = fx.Responses.customer[0].id;

  const res = await fx.Request.post(fx.Endpoints.payment, { data: payload });
  await expect(res).CheckResponse();
  const paymentId = (await res.json()) as number;
  fx.Responses.payment.push(paymentId);

  await expect
    .poll(
      async () => {
        const detail = await getLiabilityDetail(fx, opts.liabilityId);
        return Number(detail.currentAmount);
      },
      {
        message:
          `Liability ${opts.liabilityId} must be reduced to ` +
          `${opts.expectedRemainingCurrentAmount} before EasyPay confirm ` +
          `(offlineAmount=${opts.offlineAmount})`,
        timeout: 20_000,
        intervals: [1000],
      },
    )
    .toBe(opts.expectedRemainingCurrentAmount);

  return paymentId;
}

/** @deprecated Prefer reduceLiabilityWithOfflinePayment — full clear can omit RECEIVABLE link. */
export async function clearLiabilityWithOfflinePayment(
  fx: Pdt3214Fx,
  opts: { liabilityId: number; amount: number },
): Promise<number> {
  return reduceLiabilityWithOfflinePayment(fx, {
    liabilityId: opts.liabilityId,
    offlineAmount: opts.amount,
    expectedRemainingCurrentAmount: 0,
  });
}

export async function getPaymentDetail(fx: Pdt3214Fx, paymentId: number): Promise<any> {
  const res = await fx.Request.get(`${fx.Endpoints.payment}/${paymentId}`);
  await expect(res).CheckResponse();
  return res.json();
}

export async function getReceivableDetail(fx: Pdt3214Fx, receivableId: number): Promise<any> {
  const res = await fx.Request.get(`${fx.Endpoints.customerReceivable}/${receivableId}`);
  await expect(res).CheckResponse();
  return res.json();
}

export function extractAccountPeriodId(entity: any): number | null {
  const nested =
    entity?.accountPeriodId ??
    entity?.accountingPeriodResponse ??
    entity?.accountingPeriodId ??
    null;
  if (nested == null) return null;
  if (typeof nested === 'number') return nested;
  const id = Number(nested.id ?? nested.accountPeriodId);
  return Number.isFinite(id) ? id : null;
}

export function extractAccountPeriodName(entity: any): string {
  if (typeof entity?.accountingPeriodName === 'string' && entity.accountingPeriodName) {
    return String(entity.accountingPeriodName);
  }
  const nested =
    entity?.accountPeriodId ??
    entity?.accountingPeriodResponse ??
    entity?.accountingPeriodId ??
    null;
  if (nested == null || typeof nested !== 'object') return '';
  return String(nested.name ?? '');
}

function summarizeOffsettingList(offsets: Array<Record<string, any>>): string {
  if (!offsets.length) return '(empty)';
  return offsets
    .map((o) => {
      const type = String(o.offsettingObjectType ?? o.offsettingObject ?? o.type ?? '?');
      return `${type}#${o.id}@${o.amount}`;
    })
    .join(', ');
}

function pickReceivableFromOffsets(offsets: Array<Record<string, any>>): SurplusReceivableHit | null {
  const receivable = offsets.find((o) => {
    const type = String(o.offsettingObjectType ?? o.offsettingObject ?? o.type ?? '').toUpperCase();
    return type === 'RECEIVABLE';
  });
  if (!receivable) return null;
  const id = Number(receivable.id);
  if (!Number.isFinite(id) || id <= 0) return null;
  return {
    id,
    amount: Number(receivable.amount ?? 0),
    source: 'payment_offsetting',
  };
}

async function listPaymentsByCustomerNumber(
  fx: Pdt3214Fx,
  customerNumber: string,
): Promise<{ totalElements: number; content: any[] }> {
  const listRes = await fx.Request.post(`${fx.Endpoints.payment}/list`, {
    data: {
      page: 0,
      size: 25,
      prompt: customerNumber,
      searchFields: 'CUSTOMER_NUMBER',
    },
  });
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  return { totalElements: Number(body.totalElements ?? 0), content: body.content ?? [] };
}

function amountClose(a: unknown, b: number, eps = 0.01): boolean {
  const n = Number(a);
  return Number.isFinite(n) && Math.abs(n - b) < eps;
}

/**
 * Pick EasyPay confirm payment for the customer (not the offline partial-clear payment).
 * List rows often omit / false `isOnlinePayment` — fall back like PDT-3171 (newest row),
 * excluding known offline payment ids and optionally matching confirm amount.
 */
export function selectEasyPayPaymentFromList(
  rows: any[],
  opts?: {
    excludePaymentIds?: number[];
    preferAmount?: number;
  },
): any | null {
  const exclude = new Set((opts?.excludePaymentIds ?? []).map(Number).filter((id) => id > 0));
  const sorted = [...rows].sort((a, b) => Number(b.id) - Number(a.id));
  if (!sorted.length) return null;

  const online = sorted.find((p) => Boolean(p.isOnlinePayment) && !exclude.has(Number(p.id)));
  if (online) return online;

  const notExcluded = sorted.find((p) => !exclude.has(Number(p.id)));
  if (notExcluded) return notExcluded;

  if (opts?.preferAmount != null) {
    const byAmount = sorted.find((p) => amountClose(p.initialAmount, opts.preferAmount!));
    if (byAmount) return byAmount;
  }

  return sorted[0] ?? null;
}

/**
 * Find payment created by EasyPay confirm-pay.
 * Selection: isOnlinePayment → newest not in excludePaymentIds → newest matching preferAmount.
 * Also retries list by CUSTOMER_NUMBER when identifier search only returns excluded rows.
 */
export async function findLatestOnlinePaymentForCustomer(
  fx: Pdt3214Fx,
  identifier: string,
  opts?: {
    excludePaymentIds?: number[];
    preferAmount?: number;
    customerNumber?: string;
  },
): Promise<any> {
  let chosen: any;
  const excludeIds = opts?.excludePaymentIds ?? [];
  const pickOpts = {
    excludePaymentIds: excludeIds,
    preferAmount: opts?.preferAmount,
  };

  await expect
    .poll(
      async () => {
        const byIdentifier = await listPaymentsByCustomerIdentifier(fx, identifier);
        chosen = selectEasyPayPaymentFromList(byIdentifier.content ?? [], pickOpts);

        // Identifier search may only return the offline partial payment — retry CUSTOMER_NUMBER.
        const chosenExcluded = chosen != null && excludeIds.includes(Number(chosen.id));
        const needNumberFallback =
          opts?.customerNumber &&
          (chosen == null ||
            chosenExcluded ||
            ((byIdentifier.content ?? []).length > 0 &&
              (byIdentifier.content ?? []).every((p: any) => excludeIds.includes(Number(p.id)))));

        if (needNumberFallback) {
          const byNumber = await listPaymentsByCustomerNumber(fx, opts!.customerNumber!);
          const fromNumber = selectEasyPayPaymentFromList(byNumber.content ?? [], pickOpts);
          if (fromNumber && !excludeIds.includes(Number(fromNumber.id))) {
            chosen = fromNumber;
          } else if (fromNumber && (chosen == null || chosenExcluded)) {
            chosen = fromNumber;
          }
        }

        // Accept only when we have a row that is not the offline clear payment (or amount match).
        if (chosen == null) return null;
        if (excludeIds.includes(Number(chosen.id))) {
          if (opts?.preferAmount != null && amountClose(chosen.initialAmount, opts.preferAmount)) {
            return chosen.id;
          }
          return null;
        }
        return chosen.id;
      },
      {
        message:
          `EasyPay payment must appear for customer identifier=${identifier}` +
          (opts?.customerNumber ? ` / customerNumber=${opts.customerNumber}` : '') +
          ` after confirm (excludePaymentIds=[${excludeIds.join(',')}]` +
          (opts?.preferAmount != null ? `, preferAmount=${opts.preferAmount}` : '') +
          `)`,
        timeout: 30_000,
        intervals: [1000],
      },
    )
    .toBeTruthy();
  expect(chosen?.id, 'EasyPay payment id').toBeTruthy();
  return chosen;
}

/**
 * List customer receivables (Swagger GET /customer-receivable listing — same query shape as
 * billing InvoiceCancelation specs).
 */
export async function listCustomerReceivablesByIdentifier(
  fx: Pdt3214Fx,
  identifier: string,
): Promise<any[]> {
  const listRes = await fx.Request.get(
    `${Endpoints.customerReceivable}?page=0&size=50&prompt=${encodeURIComponent(identifier)}` +
      `&customerReceivableSearchBy=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  return (body.content ?? []) as any[];
}

/**
 * Resolve surplus receivable after EasyPay confirm:
 * 1) Poll payment.offsettingResponseList for RECEIVABLE
 * 2) Fallback: customer receivable list matching expected surplus amount
 */
export async function findSurplusReceivableForEasyPayPayment(
  fx: Pdt3214Fx,
  opts: {
    paymentId: number;
    customerIdentifier: string;
    expectedSurplusAmount: number;
  },
): Promise<{ surplus: SurplusReceivableHit; paymentDetail: any }> {
  let paymentDetail: any;
  let surplus: SurplusReceivableHit | null = null;

  await expect
    .poll(
      async () => {
        paymentDetail = await getPaymentDetail(fx, opts.paymentId);
        const offsets = (paymentDetail.offsettingResponseList ?? []) as Array<Record<string, any>>;
        surplus = pickReceivableFromOffsets(offsets);
        if (surplus) return surplus.id;

        const rows = await listCustomerReceivablesByIdentifier(fx, opts.customerIdentifier);
        const match = rows.find((r) => {
          const amount = Math.abs(Number(r.initialAmount ?? r.currentAmount ?? 0));
          const creation = String(r.creationType ?? '').toUpperCase();
          const amountOk =
            Math.abs(amount - opts.expectedSurplusAmount) < 0.01 ||
            Math.abs(amount - Number(paymentDetail.initialAmount ?? 0)) < 0.01;
          // Prefer AUTOMATIC surplus from EasyPay; accept amount match if creationType absent in list
          return amountOk && (creation === 'AUTOMATIC' || creation === '' || creation === 'UNDEFINED');
        });
        if (match?.id) {
          surplus = {
            id: Number(match.id),
            amount: Number(match.initialAmount ?? match.currentAmount ?? 0),
            source: 'customer_receivable_list',
          };
          return surplus.id;
        }
        return null;
      },
      {
        message:
          `EasyPay surplus receivable missing for payment ${opts.paymentId}. ` +
          `Expected surplus≈${opts.expectedSurplusAmount}. ` +
          `Last offsetting=[${summarizeOffsettingList(
            (paymentDetail?.offsettingResponseList ?? []) as Array<Record<string, any>>,
          )}] ` +
          `payment.currentAmount=${paymentDetail?.currentAmount} ` +
          `payment.initialAmount=${paymentDetail?.initialAmount}`,
        timeout: 45_000,
        intervals: [1500],
      },
    )
    .toBeTruthy();

  expect(surplus, 'EasyPay surplus receivable must be detectable').toBeTruthy();
  return { surplus: surplus!, paymentDetail };
}

/** Sync helper kept for callers that already have paymentDetail with RECEIVABLE. */
export function findSurplusReceivableFromPayment(paymentDetail: any): {
  id: number;
  amount: number;
} {
  const offsets = (paymentDetail.offsettingResponseList ?? []) as Array<Record<string, any>>;
  const hit = pickReceivableFromOffsets(offsets);
  expect(
    hit,
    `EasyPay surplus receivable must appear in payment offsetting list; ` +
      `offsetting=[${summarizeOffsettingList(offsets)}] ` +
      `currentAmount=${paymentDetail?.currentAmount} initialAmount=${paymentDetail?.initialAmount}`,
  ).toBeTruthy();
  return { id: hit!.id, amount: hit!.amount };
}

/**
 * List CLOSED accounting periods via Swagger GET /accounting-period
 * (AccountingPeriodsListingRequest.status = CLOSED). No hardcoded Dev period ids.
 */
export async function listClosedAccountingPeriods(
  fx: Pdt3214Fx,
): Promise<AccountingPeriodInfo[]> {
  const listRes = await fx.Request.get(
    'accounting-period?page=0&size=50&status=CLOSED&sortBy=END_DATE&direction=DESC',
  );
  await expect(listRes).CheckResponse();
  const body = await listRes.json();
  const rows = (body.content ?? []) as Array<Record<string, any>>;
  const periods: AccountingPeriodInfo[] = [];
  for (const row of rows) {
    if (String(row.status) !== 'CLOSED') continue;
    const period = periodFromRow({
      ...row,
      id: row.accountPeriodId ?? row.id,
    });
    if (period) periods.push(period);
  }
  periods.sort((a, b) => b.endDate.localeCompare(a.endDate));
  return periods;
}

/**
 * Pick a CLOSED calendar period before today for offline paymentDate.
 * GE CLOSED scenario: Liability/Receivable AP → current OPEN (not payment AP remap via EasyPay).
 * Builds on resolveEnvAccountingPeriodContext.
 */
export async function resolveClosedPeriodPaymentPlan(
  fx: Pdt3214Fx,
): Promise<ClosedPeriodPaymentPlan> {
  const plan = await tryResolveClosedPeriodPaymentPlan(fx);
  expect(
    plan,
    `Need a CLOSED period before today for GE closed→current AP scenario on this env.`,
  ).toBeTruthy();
  return plan!;
}

/** POST daily interest rate (REG-1063 / overdueLiability prerequisite for EasyPay LPF). */
export async function createDailyInterestRate(fx: Pdt3214Fx): Promise<number> {
  const interestRes = await fx.Request.post(fx.Endpoints.interestRate, {
    data: fx.GeneratePayload.receivablesManagement.dailyInterestRate(),
  });
  await expect(interestRes).CheckResponse();
  const interestId = (await interestRes.json()) as number;
  fx.Responses.interestRate.push(interestId);
  return interestId;
}

/**
 * Overdue manual liability with applicable interest — EasyPay confirm creates LPF (REG-1063).
 *
 * Default due/occurrence = wall-clock yesterday (generateYesterdaysDate) — same as
 * receivablesManagement.overdueLiability(). That is correct for LPF when EasyPay confirm
 * DATE is today (todayOpenEasyPayDate / generateOnlinePaymentDate): overdue as of init AND
 * confirm, and init AMOUNT LPF days match confirm recalculation.
 *
 * Do NOT pass dueDate = prior-OPEN mid − 1 when confirm DATE is today — that creates months
 * of LPF in init AMOUNT while still OK if confirm is also today. Conversely, do NOT use
 * priorOpenEasyPayDate as confirm DATE for LPF (init AMOUNT is always "now").
 *
 * Optional dueDateDdMmYyyy / occurrenceDateDdMmYyyy overrides exist for non-LPF callers only.
 */
export async function createOverdueLiabilityWithInterest(
  fx: Pdt3214Fx,
  opts: {
    initialAmount: number;
    interestRateId?: number;
    /** dd-mm-yyyy — must be before EasyPay confirm calendar day */
    dueDateDdMmYyyy?: string;
    /** dd-mm-yyyy — defaults to dueDateDdMmYyyy when omitted */
    occurrenceDateDdMmYyyy?: string;
  },
): Promise<number> {
  const interestRateId =
    opts.interestRateId ??
    Number(fx.Responses.interestRate[fx.Responses.interestRate.length - 1]);
  expect(interestRateId, 'applicableInterestRateId for overdue liability').toBeGreaterThan(0);

  const dueDateDdMmYyyy =
    opts.dueDateDdMmYyyy ?? randomGens.generateYesterdaysDate('dd-mm-yyyy');
  const occurrenceDateDdMmYyyy = opts.occurrenceDateDdMmYyyy ?? dueDateDdMmYyyy;

  const payload = fx.GeneratePayload.receivablesManagement.customer_liability();
  payload.initialAmount = opts.initialAmount;
  payload.occurrenceDate = occurrenceDateDdMmYyyy;
  payload.dueDate = dueDateDdMmYyyy;
  payload.applicableInterestRateId = interestRateId;

  const res = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  await expect(res).CheckResponse();
  const liabilityId = (await res.json()) as number;
  fx.Responses.customerLiability.push(liabilityId);
  return liabilityId;
}

export async function getCustomerLiabilityFullDetail(
  fx: Pdt3214Fx,
  liabilityId: number,
): Promise<any> {
  const res = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
  await expect(res).CheckResponse();
  return res.json();
}

/**
 * Resolve Customer Liability created from LATE_PAYMENT_FINE document
 * (CustomerLiabilityResponse.outgoingDocumentType + latePaymentFineShortResponse).
 * Returns 0 when not found yet (safe for expect.poll).
 */
export async function findLpfCustomerLiabilityId(
  fx: Pdt3214Fx,
  customerIdentifier: string,
  lpfId: number,
): Promise<number> {
  const listRes = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC` +
      `&prompt=${encodeURIComponent(customerIdentifier)}&searchFields=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const listBody = await listRes.json();
  const rows = (listBody.content ?? []) as Array<{ id?: number }>;

  for (const row of rows) {
    const liabilityId = Number(row.id);
    if (!Number.isFinite(liabilityId) || liabilityId <= 0) continue;
    const body = await getCustomerLiabilityFullDetail(fx, liabilityId);
    const docType = String(body.outgoingDocumentType ?? '').toUpperCase();
    const lpfRef = body.latePaymentFineShortResponse as { id?: number } | undefined;
    if (docType === 'LATE_PAYMENT_FINE' && Number(lpfRef?.id) === lpfId) {
      return liabilityId;
    }
  }

  return 0;
}

/**
 * After EasyPay confirm on overdue+interest liability: wait for LPF (no job — REG-1063 path)
 * then resolve the linked Customer Liability for AP assertions.
 *
 * Accept LPF whose parentLiability matches ANY of the source overdue liabilities
 * (combine=true creates one LPF per overdue source — filtering a single id can null forever).
 */
export async function waitForEasyPayLpfCustomerLiability(
  fx: Pdt3214Fx,
  opts: {
    customerIdentifier: string;
    /** Prefer LPF whose parentLiability matches this overdue source liability. */
    sourceLiabilityId?: number;
    /** Accept LPF linked to any of these overdue source liabilities (preferred for REG-1063). */
    sourceLiabilityIds?: number[];
  },
): Promise<EasyPayLpfLiabilityHit> {
  let chosenLpfId: number | undefined;
  let chosenLpfAmount = 0;

  const parentAllowlist = new Set<number>(
    (opts.sourceLiabilityIds ?? [])
      .map((id) => Number(id))
      .filter((id) => Number.isFinite(id) && id > 0),
  );
  if (opts.sourceLiabilityId != null && Number(opts.sourceLiabilityId) > 0) {
    parentAllowlist.add(Number(opts.sourceLiabilityId));
  }
  const hasParentFilter = parentAllowlist.size > 0;
  const parentFilterLabel = hasParentFilter
    ? ` after confirm of overdue liability id(s)=${[...parentAllowlist].join(',')}`
    : ' after EasyPay confirm';

  await expect
    .poll(
      async () => {
        const listRes = await fx.Request.get(
          `${fx.Endpoints.latePaymentFine}/list?page=0&size=50` +
            `&prompt=${encodeURIComponent(opts.customerIdentifier)}` +
            `&type=LATE_PAYMENT_FINE&searchBy=CUSTOMER`,
        );
        await expect(listRes).CheckResponse();
        const listBody = await listRes.json();
        const rows = (listBody.content ?? []) as Array<Record<string, any>>;
        if (!rows.length) return null;

        let fallback: { id: number; amount: number; body: any } | null = null;

        for (const row of rows) {
          const lpfId = Number(row.id);
          if (!Number.isFinite(lpfId) || lpfId <= 0) continue;
          const lpfGet = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/${lpfId}`);
          await expect(lpfGet).CheckResponse();
          const lpfBody = await lpfGet.json();
          if (String(lpfBody.type ?? 'LATE_PAYMENT_FINE').toUpperCase() !== 'LATE_PAYMENT_FINE') {
            continue;
          }

          const parentShort = lpfBody.parentLiabilityShortResponse as { id?: number } | undefined;
          const parentId = Number(lpfBody.parentLiabilityId ?? parentShort?.id ?? -1);
          const amount = Number(lpfBody.amount ?? 0);

          if (fallback == null) {
            fallback = { id: lpfId, amount, body: lpfBody };
          }

          if (hasParentFilter) {
            if (parentAllowlist.has(parentId)) {
              chosenLpfId = lpfId;
              chosenLpfAmount = amount;
              fx.Responses.latePaymentFine.push(lpfBody);
              return lpfId;
            }
            continue;
          }

          chosenLpfId = lpfId;
          chosenLpfAmount = amount;
          fx.Responses.latePaymentFine.push(lpfBody);
          return lpfId;
        }

        // Parent filter set but no match yet — keep polling (do not grab unrelated LPF).
        if (hasParentFilter) return null;

        if (fallback) {
          chosenLpfId = fallback.id;
          chosenLpfAmount = fallback.amount;
          fx.Responses.latePaymentFine.push(fallback.body);
          return fallback.id;
        }
        return null;
      },
      {
        message:
          `EasyPay LPF (LATE_PAYMENT_FINE) must appear for customer=${opts.customerIdentifier}` +
          parentFilterLabel,
        timeout: 120_000,
        intervals: [1500, 2000, 5000],
      },
    )
    .toBeTruthy();

  expect(chosenLpfId, 'EasyPay LPF id').toBeTruthy();

  let liabilityId = 0;
  let liabilityDetail: any;
  await expect
    .poll(
      async () => {
        liabilityId = await findLpfCustomerLiabilityId(
          fx,
          opts.customerIdentifier,
          chosenLpfId!,
        );
        if (!liabilityId) return null;
        liabilityDetail = await getCustomerLiabilityFullDetail(fx, liabilityId);
        return liabilityId;
      },
      {
        message: `LPF Customer Liability must exist for latePaymentFine id=${chosenLpfId}`,
        timeout: 45_000,
        intervals: [1500, 2000, 5000],
      },
    )
    .toBeTruthy();

  expect(liabilityId, 'LPF Customer Liability id').toBeGreaterThan(0);
  fx.Responses.customerLiability.push(liabilityId);
  return {
    lpfId: chosenLpfId!,
    lpfAmount: chosenLpfAmount,
    liabilityId,
    liabilityDetail,
  };
}
