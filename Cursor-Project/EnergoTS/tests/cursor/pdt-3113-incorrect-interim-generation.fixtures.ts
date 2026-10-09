/**
 * PDT-3113 — IAP interim generation must use the POD/billing-group-specific previous standard invoice.
 *
 * Reference:
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts (two PODs / separate billing groups)
 * - tests/cursor/PDT-2750-missing-interim-invoice.spec.ts (resign + PERCENT_FROM_PREVIOUS IAP)
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts (IAP percent + interim poll)
 * - tests/cursor/pdt-3013-separate-pod-reversal-offset.fixtures.ts (volume billing + anchor)
 * - tests/pointOfDelivery/statusChangeContractLifeSycle.spec.ts (terminate A → free PODs for resign)
 *
 * Environment: Dev (OPEN accounting periods resolved at runtime) | Swagger: Cursor-Project/config/swagger/dev/swagger-spec.json
 */

import { test, expect } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import {
  applyVolumeBillingAnchor,
  profileDateRangeFromAnchor,
} from './PDT-2529-rfd-data-model-happy-path.fixtures';
import {
  type Pdt2376BillingAnchor,
} from './pdt-2376-volume-with-electricity.fixtures';
import { asBillingRunId } from './pdt-2599-service-contract.fixtures';

export const PDT_3113_JIRA_KEY = 'PDT-3113';
/** Exact Jira summary (trailing space preserved). */
export const PDT_3113_JIRA_TITLE = 'IAP - Incorrect interim generation ';
export const PDT_3113_IAP_PERCENT = 50;
/** Distinct volumes so the two standard invoices have different totals. */
export const PDT_3113_VOLUME_POD1 = 200;
export const PDT_3113_VOLUME_POD2 = 800;
export const PDT_3113_VOLUME_PC_EXPRESSION = 10;

const BILLING_RUN_ROOT = 'billing-run';
const POLL_MS = 15_000;
const POLL_MAX_MS = 10 * 60 * 1000;

/**
 * Valeri PDT-3113 example on **Test** (ticket links + Test DB contracts 44543/44544).
 * Source: `config/jira/attachments/PDT-3113/issue-rest.json` + billing.account_periods 1010/1011.
 */
export const PDT_3113_VALERI_REFERENCE = {
  contractAId: 44543,
  contractBId: 44544,
  standardInvoiceIds: [90025, 90026] as const,
  interimInvoiceIds: [90029, 90030] as const,
  standardBillingRunId: 12907,
  interimBillingRunId: 12912,
  ticketUrls: {
    contractA:
      'https://testapps.energo-pro.bg/app/phoenix-epres/energy-product-contracts/preview/basic-parameters?id=44543',
    contractB:
      'https://testapps.energo-pro.bg/app/phoenix-epres/energy-product-contracts/preview/basic-parameters?id=44544',
    standard90025:
      'https://testapps.energo-pro.bg/app/phoenix-epres/invoices/preview/basic-data?id=90025',
    standard90026:
      'https://testapps.energo-pro.bg/app/phoenix-epres/invoices/preview/basic-data?id=90026',
    interim90029:
      'https://testapps.energo-pro.bg/app/phoenix-epres/billing-run/invoices/preview/basic-data?id=90029',
    interim90030:
      'https://testapps.energo-pro.bg/app/phoenix-epres/billing-run/invoices/preview/basic-data?id=90030',
  },
} as const;

/** Calendar + statuses copied from Test DB (UTC→local calendar dates as stored in API payloads). */
export const PDT_3113_VALERI_TEMPLATE = {
  contractA: {
    podActivationDate: '2026-01-01',
    signingDate: '2026-01-01',
    terminateSubStatus: 'BY_MUTUAL_AGREEMENT' as const,
  },
  standardBilling: {
    profileStart: '2026-06-01',
    profileEnd: '2026-06-30',
    invoicePeriodTo: '2026-06-30',
    taxEventDate: '2026-06-30',
    invoiceDate: '2026-06-30',
    accountingPeriodId: 1010,
    commentary: 'Valeri billing 12907 — June 2026 FOR_VOLUMES on contract A (account period 1010).',
  },
  contractB: {
    /** Valeri Test DB — contract B signed 2026-06-04; version start must be on/before signing for resign API. */
    valeriSigningDate: '2026-06-04',
    versionStartDate: '2026-06-01',
    podActivationDate: '2026-07-01',
    contractStatus: 'ACTIVE_IN_TERM' as const,
    contractSubStatus: 'DELIVERY' as const,
  },
  interimBilling: {
    taxEventDate: '2026-07-20',
    invoiceDate: '2026-07-20',
    invoicePeriodTo: '2026-07-31',
    profileStart: '2026-07-01',
    profileEnd: '2026-07-31',
    accountingPeriodId: 1011,
    commentary: 'Valeri billing 12912 — July 2026 INTERIM on contract B (account period 1011).',
  },
};

export type Pdt3113OpenPeriod = {
  id: number;
  startDate: string;
  endDate: string;
};

export type Pdt3113ValeriCalendar = {
  standardAnchor: Pdt2376BillingAnchor;
  interimAnchor: Pdt2376BillingAnchor;
  contractA: { podActivationDate: string };
  contractB: { signingDate: string; versionStartDate: string; podActivationDate: string };
  /** All OPEN periods from available-list, oldest→newest. Used by GE latest / 3-month cases. */
  openPeriods: Pdt3113OpenPeriod[];
};

/** GE Description: previous invoices considered for the last 3 months. */
export const PDT_3113_PREVIOUS_INVOICE_MONTHS = 3;

type OpenApRow = {
  id?: number;
  startDate?: string;
  endDate?: string;
  status?: string;
};

function ymd(iso?: string): string {
  return (iso ?? '').slice(0, 10);
}

function addUtcDays(dateYmd: string, days: number): string {
  const d = new Date(`${dateYmd}T00:00:00.000Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

function clampYmd(value: string, lo: string, hi: string): string {
  if (value < lo) return lo;
  if (value > hi) return hi;
  return value;
}

function periodsAreConsecutiveMonths(earlier: OpenApRow, later: OpenApRow): boolean {
  const earlierEnd = ymd(earlier.endDate);
  const laterStart = ymd(later.startDate);
  if (!earlierEnd || !laterStart) return false;
  return laterStart === addUtcDays(earlierEnd, 1);
}

function dayOfMonthInRange(start: string, end: string, day: number): string {
  const candidate = `${start.slice(0, 7)}-${String(day).padStart(2, '0')}`;
  return clampYmd(candidate, start, end);
}

/**
 * Valeri pattern (standard month N, interim month N+1) mapped onto **two consecutive OPEN**
 * accounting periods from `GET /billing-run/accounting-period-available-list`.
 * Hardcoded Test ids 1010/1011 are June/July 2026 — closed on Dev by August, so they cannot be used.
 */
export async function resolvePdt3113BillingCalendar(
  Request: baseFixture['Request'],
): Promise<Pdt3113ValeriCalendar> {
  const listRes = await Request.get('billing-run/accounting-period-available-list?page=0&size=50&direction=DESC');
  await expect(listRes).CheckResponse();
  const page = (await listRes.json()) as { content?: OpenApRow[] };
  const open = (page.content ?? [])
    .filter((r) => String(r.status).toUpperCase() === 'OPEN' && r.id != null && ymd(r.startDate) && ymd(r.endDate))
    .sort((a, b) => ymd(a.startDate).localeCompare(ymd(b.startDate)));

  const pairs: Array<{ standard: OpenApRow; interim: OpenApRow }> = [];
  for (let i = 0; i < open.length - 1; i += 1) {
    if (periodsAreConsecutiveMonths(open[i], open[i + 1])) {
      pairs.push({ standard: open[i], interim: open[i + 1] });
    }
  }
  const pair = pairs.at(-1);
  if (!pair) {
    const listed = open
      .map((r) => `id=${r.id} ${ymd(r.startDate)}→${ymd(r.endDate)}`)
      .join('; ');
    throw new Error(
      `[PDT-3113] Need two consecutive OPEN accounting periods (standard month + next-month interim). ` +
        `OPEN now: ${listed || '(none)'}. Open the previous month on this environment, then rerun.`,
    );
  }

  const standardStart = ymd(pair.standard.startDate);
  const standardEnd = ymd(pair.standard.endDate);
  const interimStart = ymd(pair.interim.startDate);
  const interimEnd = ymd(pair.interim.endDate);
  const today = new Date().toISOString().slice(0, 10);
  const standardId = Number(pair.standard.id);
  const interimId = Number(pair.interim.id);

  const aPodActivation = `${standardStart.slice(0, 4)}-01-01`;
  const contractAPodActivation = aPodActivation <= standardStart ? aPodActivation : standardStart;

  const openPeriods: Pdt3113OpenPeriod[] = open.map((r) => ({
    id: Number(r.id),
    startDate: ymd(r.startDate),
    endDate: ymd(r.endDate),
  }));

  return {
    contractA: { podActivationDate: contractAPodActivation },
    contractB: {
      versionStartDate: standardStart,
      signingDate: dayOfMonthInRange(standardStart, standardEnd, 4),
      podActivationDate: interimStart,
    },
    standardAnchor: {
      invoicePeriodTo: standardEnd,
      taxEventDate: standardEnd,
      invoiceDate: standardEnd,
      accountingPeriodId: standardId,
      profileStart: standardStart,
      profileEnd: standardEnd,
      commentary: `OPEN standard ap=${standardId} ${standardStart}→${standardEnd} (Valeri analogue of Test 1010 June).`,
    },
    interimAnchor: {
      invoicePeriodTo: interimEnd,
      taxEventDate: clampYmd(dayOfMonthInRange(interimStart, interimEnd, 20), interimStart, today < interimEnd ? today : interimEnd),
      invoiceDate: clampYmd(dayOfMonthInRange(interimStart, interimEnd, 20), interimStart, today < interimEnd ? today : interimEnd),
      accountingPeriodId: interimId,
      profileStart: interimStart,
      profileEnd: interimEnd,
      commentary: `OPEN interim ap=${interimId} ${interimStart}→${interimEnd} (Valeri analogue of Test 1011 July).`,
    },
    openPeriods,
  };
}

function addUtcMonths(dateYmd: string, months: number): string {
  const d = new Date(`${dateYmd}T00:00:00.000Z`);
  d.setUTCMonth(d.getUTCMonth() + months);
  return d.toISOString().slice(0, 10);
}

/** True when `invoiceDate` is on/after (asOfDate − 3 calendar months) — GE previous-invoice window. */
export function isWithinPreviousInvoiceWindow(
  invoiceDate: string,
  asOfDate: string,
  months = PDT_3113_PREVIOUS_INVOICE_MONTHS,
): boolean {
  const cutoff = addUtcMonths(asOfDate, -months);
  return ymd(invoiceDate) >= cutoff;
}

/**
 * Longest consecutive OPEN chain (oldest→newest). GE "latest among several previous invoices"
 * needs 3 consecutive OPEN months (older standard, newer standard, interim).
 */
export function consecutiveOpenChain(openPeriods: Pdt3113OpenPeriod[]): Pdt3113OpenPeriod[] {
  if (openPeriods.length === 0) return [];
  let best: Pdt3113OpenPeriod[] = [openPeriods[0]];
  let current: Pdt3113OpenPeriod[] = [openPeriods[0]];
  for (let i = 1; i < openPeriods.length; i += 1) {
    const prev = openPeriods[i - 1];
    const next = openPeriods[i];
    if (ymd(next.startDate) === addUtcDays(ymd(prev.endDate), 1)) {
      current.push(next);
    } else {
      if (current.length > best.length) best = current;
      current = [next];
    }
  }
  if (current.length > best.length) best = current;
  return best;
}

export function anchorFromOpenPeriod(
  period: Pdt3113OpenPeriod,
  commentary: string,
  today = new Date().toISOString().slice(0, 10),
  invoiceDay?: number,
): Pdt2376BillingAnchor {
  const start = period.startDate;
  const end = period.endDate;
  const invoiceDate = invoiceDay
    ? clampYmd(
        dayOfMonthInRange(start, end, invoiceDay),
        start,
        today < end ? today : end,
      )
    : end;
  return {
    invoicePeriodTo: end,
    taxEventDate: invoiceDate,
    invoiceDate,
    accountingPeriodId: period.id,
    profileStart: start,
    profileEnd: end,
    commentary,
  };
}

/** A (older+newer standards) → B (IAP interim) when 3 consecutive OPEN periods exist. */
export function calendarForLatestPreviousInvoice(
  openPeriods: Pdt3113OpenPeriod[],
): {
  calendar: Pdt3113ValeriCalendar;
  olderStandardAnchor: Pdt2376BillingAnchor;
  newerStandardAnchor: Pdt2376BillingAnchor;
} | null {
  const chain = consecutiveOpenChain(openPeriods);
  if (chain.length < 3) return null;
  const older = chain[chain.length - 3];
  const newer = chain[chain.length - 2];
  const interim = chain[chain.length - 1];
  const olderAnchor = anchorFromOpenPeriod(older, `OPEN older standard ap=${older.id}`);
  const newerAnchor = anchorFromOpenPeriod(newer, `OPEN newer standard ap=${newer.id}`);
  const today = new Date().toISOString().slice(0, 10);
  const interimAnchor = anchorFromOpenPeriod(
    interim,
    `OPEN interim ap=${interim.id}`,
    today,
    20,
  );
  const aPodActivationYear = `${older.startDate.slice(0, 4)}-01-01`;
  const calendar: Pdt3113ValeriCalendar = {
    contractA: {
      podActivationDate:
        aPodActivationYear <= older.startDate ? aPodActivationYear : older.startDate,
    },
    contractB: {
      versionStartDate: newer.startDate,
      signingDate: dayOfMonthInRange(newer.startDate, newer.endDate, 4),
      podActivationDate: interim.startDate,
    },
    standardAnchor: olderAnchor,
    interimAnchor,
    openPeriods,
  };
  return { calendar, olderStandardAnchor: olderAnchor, newerStandardAnchor: newerAnchor };
}

type ListedAccountingPeriod = Pdt3113OpenPeriod & { status: string; name: string };

async function listAccountingPeriods(
  Request: baseFixture['Request'],
  status?: 'OPEN' | 'CLOSED',
): Promise<ListedAccountingPeriod[]> {
  const rows: ListedAccountingPeriod[] = [];
  for (let page = 0; page < 10; page += 1) {
    const qs = [
      `page=${page}`,
      'size=50',
      'sortBy=START_DATE',
      'direction=ASC',
    ];
    if (status) qs.push(`status=${status}`);
    const res = await Request.get(`accounting-period?${qs.join('&')}`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as {
      content?: Array<Record<string, unknown>>;
      last?: boolean;
    };
    const content = body.content ?? [];
    for (const row of content) {
      const id = Number(row.accountPeriodId ?? row.id);
      const startDate = ymd(String(row.startDate ?? ''));
      const endDate = ymd(String(row.endDate ?? ''));
      if (!(id > 0) || !startDate || !endDate) continue;
      rows.push({
        id,
        startDate,
        endDate,
        status: String(row.status ?? ''),
        name: String(row.name ?? ''),
      });
    }
    if (body.last === true || content.length < 50) break;
  }
  return rows;
}

/** PUT /accounting-period/{id} — AccountingPeriodRequest (name, startDate, endDate, status, modifyDate). */
export async function ensureAccountingPeriodOpen(
  Request: baseFixture['Request'],
  periodId: number,
): Promise<void> {
  const getRes = await Request.get(`accounting-period/${periodId}`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as {
    name?: string;
    startDate?: string;
    endDate?: string;
    status?: string;
  };
  if (String(body.status).toUpperCase() === 'OPEN') return;
  const putRes = await Request.put(`accounting-period/${periodId}`, {
    data: {
      name: body.name,
      startDate: body.startDate,
      endDate: body.endDate,
      status: 'OPEN',
      modifyDate: new Date().toISOString(),
    },
  });
  await expect(putRes).CheckResponse();
}

/**
 * Last-three GE tests: do not skip. OPEN the extra CLOSED periods this env is missing:
 * 1) month immediately before the current 2 consecutive OPEN pair → 3 consecutive (latest)
 * 2) a period ending before asOf − 3 months → stale REAL (3-month ± negative)
 */
export async function ensurePdt3113ExtendedAccountingPeriodsOpen(
  Request: baseFixture['Request'],
): Promise<Pdt3113ValeriCalendar> {
  let calendar = await resolvePdt3113BillingCalendar(Request);

  for (let attempt = 0; attempt < 4; attempt += 1) {
    const chain = consecutiveOpenChain(calendar.openPeriods);
    if (chain.length >= 3) break;
    const all = await listAccountingPeriods(Request);
    const oldest = chain[0];
    const predecessor = all.find((p) => addUtcDays(p.endDate, 1) === oldest.startDate);
    if (!predecessor) {
      throw new Error(
        `[PDT-3113] Cannot OPEN a 3rd consecutive accounting period: no period ending the day before ` +
          `${oldest.startDate} (ap=${oldest.id}). Listed: ` +
          all.map((p) => `ap=${p.id} ${p.status} ${p.startDate}→${p.endDate}`).join('; '),
      );
    }
    await ensureAccountingPeriodOpen(Request, predecessor.id);
    calendar = await resolvePdt3113BillingCalendar(Request);
  }

  const asOf = calendar.interimAnchor.invoiceDate;
  let stale = calendar.openPeriods.filter(
    (p) => !isWithinPreviousInvoiceWindow(p.endDate, asOf, PDT_3113_PREVIOUS_INVOICE_MONTHS),
  );
  if (stale.length === 0) {
    const all = await listAccountingPeriods(Request);
    const staleCandidates = all
      .filter((p) => !isWithinPreviousInvoiceWindow(p.endDate, asOf, PDT_3113_PREVIOUS_INVOICE_MONTHS))
      .sort((a, b) => b.endDate.localeCompare(a.endDate));
    if (staleCandidates.length === 0) {
      throw new Error(
        `[PDT-3113] Cannot OPEN an out-of-window accounting period: none end before ${asOf} minus ` +
          `${PDT_3113_PREVIOUS_INVOICE_MONTHS} months. Listed: ` +
          all.map((p) => `ap=${p.id} ${p.status} ${p.startDate}→${p.endDate}`).join('; '),
      );
    }
    await ensureAccountingPeriodOpen(Request, staleCandidates[0].id);
    calendar = await resolvePdt3113BillingCalendar(Request);
    stale = calendar.openPeriods.filter(
      (p) => !isWithinPreviousInvoiceWindow(p.endDate, asOf, PDT_3113_PREVIOUS_INVOICE_MONTHS),
    );
  }

  const chain = consecutiveOpenChain(calendar.openPeriods);
  if (chain.length < 3) {
    throw new Error(
      `[PDT-3113] After OPEN precondition, still <3 consecutive OPEN periods: ${chain.length} ` +
        `(${chain.map((p) => `ap=${p.id} ${p.startDate}→${p.endDate}`).join('; ')}).`,
    );
  }
  if (stale.length === 0) {
    throw new Error(
      `[PDT-3113] After OPEN precondition, still no OPEN period outside the 3-month window (asOf=${asOf}).`,
    );
  }
  return calendar;
}

function applyInterimBillingAnchor(
  billingPayload: Awaited<ReturnType<baseFixture['GeneratePayload']['billing']['billingRun']>>,
  anchor: Pdt2376BillingAnchor,
): void {
  billingPayload.commonParameters.accountingPeriodId = anchor.accountingPeriodId;
  billingPayload.commonParameters.taxEventDate = anchor.taxEventDate;
  billingPayload.commonParameters.invoiceDate = anchor.invoiceDate;
  billingPayload.commonParameters.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
}

export type Pdt3113Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export type Pdt3113InvoiceSnapshot = {
  id: number;
  invoiceType: string;
  totalAmountIncludingVat: number;
  billingGroupId: number | null;
  productContractId: number | null;
  interimCalculatedFromInvoiceId: number | null;
  podIdentifiers: string[];
};

export function entityId(entry: unknown): number {
  if (typeof entry === 'number' && Number.isFinite(entry) && entry > 0) return entry;
  if (entry !== null && typeof entry === 'object') {
    if ('id' in entry) {
      const id = Number((entry as { id: unknown }).id);
      if (Number.isFinite(id) && id > 0) return id;
    }
    const bpId = Number((entry as { basicParameters?: { id?: unknown } }).basicParameters?.id);
    if (Number.isFinite(bpId) && bpId > 0) return bpId;
  }
  throw new Error(`Cannot resolve entity id from: ${JSON.stringify(entry).slice(0, 200)}`);
}

export function coerceMoney(value: unknown): number {
  if (value == null || value === '') return NaN;
  const n = typeof value === 'number' ? value : Number(String(value).replace(',', '.'));
  return Number.isFinite(n) ? n : NaN;
}

export function round2(n: number): number {
  return Math.round((n + Number.EPSILON) * 100) / 100;
}

function billingGroupIdFromInvoice(inv: Record<string, unknown>): number | null {
  const bg = inv.contractBillingGroup as Record<string, unknown> | number | undefined;
  if (typeof bg === 'number' && Number.isFinite(bg)) return bg;
  if (bg && typeof bg === 'object') {
    const id = Number(bg.id ?? bg.billingGroupId);
    if (Number.isFinite(id) && id > 0) return id;
  }
  const flat = Number(inv.billingGroupId);
  return Number.isFinite(flat) && flat > 0 ? flat : null;
}

function productContractIdFromInvoice(inv: Record<string, unknown>): number | null {
  const pc = inv.productContract as Record<string, unknown> | number | undefined;
  if (typeof pc === 'number' && Number.isFinite(pc)) return pc;
  if (pc && typeof pc === 'object') {
    const id = Number(pc.id);
    if (Number.isFinite(id) && id > 0) return id;
  }
  return null;
}

function readInterimCalculatedFrom(inv: Record<string, unknown>): number | null {
  const raw =
    inv.interimCalculatedFromInvoiceId ??
    inv.interim_calculated_from_invoice_id ??
    (inv as { interimCalculatedFrom?: { id?: number } }).interimCalculatedFrom?.id;
  const n = Number(raw);
  return Number.isFinite(n) && n > 0 ? n : null;
}

export async function fetchInvoiceSnapshot(
  Request: Pdt3113Fx['Request'],
  invoiceId: number,
): Promise<Pdt3113InvoiceSnapshot> {
  const invRes = await Request.get(`invoice?id=${invoiceId}`);
  await expect(invRes).CheckResponse();
  const inv = (await invRes.json()) as Record<string, unknown>;

  const detailRes = await Request.get(`invoice/detailed-data?page=0&size=100&id=${invoiceId}`);
  await expect(detailRes).CheckResponse();
  const detailBody = (await detailRes.json()) as { content?: Array<Record<string, unknown>> };
  const podIdentifiers = [
    ...new Set(
      (detailBody.content ?? [])
        .map((row) => String(row.pointOfDelivery ?? row.podIdentifier ?? '').trim())
        .filter(Boolean),
    ),
  ];

  return {
    id: invoiceId,
    invoiceType: String(inv.invoiceType ?? ''),
    totalAmountIncludingVat: coerceMoney(inv.totalAmountIncludingVat),
    billingGroupId: billingGroupIdFromInvoice(inv),
    productContractId: productContractIdFromInvoice(inv),
    interimCalculatedFromInvoiceId: readInterimCalculatedFrom(inv),
    podIdentifiers,
  };
}

async function waitUntilBillingRunStatus(
  Request: Pdt3113Fx['Request'],
  billingRunId: number,
  targetStatus: string,
  timeoutMs = POLL_MAX_MS,
): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  let lastStatus: string | undefined;
  let attempt = 0;
  while (Date.now() < deadline) {
    attempt += 1;
    const runRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
    await expect(runRes).CheckResponse();
    const body = (await runRes.json()) as Record<string, unknown>;
    const cp = body.commonParameters as { status?: string } | undefined;
    lastStatus = cp?.status;
    if (lastStatus === targetStatus) return;
    if (attempt === 1 || attempt % 5 === 0) {
      console.log(
        `[PDT-3113] billingRunId=${billingRunId} attempt=${attempt} status=${lastStatus ?? '(missing)'}`,
      );
    }
    await new Promise((r) => setTimeout(r, POLL_MS));
  }
  throw new Error(
    `[PDT-3113] Billing run ${billingRunId}: timed out waiting for "${targetStatus}"; last=${lastStatus ?? '(missing)'}`,
  );
}

export async function postBillingByProfile(
  fx: Pdt3113Fx,
  podIndex: number,
  anchor: Pdt2376BillingAnchor,
  volumeValue: number,
): Promise<void> {
  const { Request, GeneratePayload, Responses } = fx;
  const dateRanges = profileDateRangeFromAnchor(anchor);
  const payload = await GeneratePayload.energyData.profile1Month(podIndex, dateRanges);
  payload.timeZone = 'CET';
  payload.entries[0].value = volumeValue;
  const profiles = await Request.post('billing-by-profile', { data: payload });
  await expect(profiles).CheckResponse();
  const profileData = await profiles.json();
  Responses.dataByProfiles.push({
    id: profileData,
    periodFrom: payload.periodFrom,
    periodTo: payload.periodTo,
    periodType: payload.periodType,
  });
}

/**
 * Predecessor contract A: 2 PODs on different billing groups, volume PC only (no IAP), energy data with distinct volumes.
 */
export async function createPredecessorContractTwoBillingGroups(
  fx: Pdt3113Fx,
): Promise<{
  calendar: Pdt3113ValeriCalendar;
  contractAId: number;
  pod1Id: number;
  pod2Id: number;
  billingGroupIds: number[];
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const calendar = await test.step(
    'Precondition: resolve two consecutive OPEN accounting periods (Valeri month N / N+1)',
    async () => resolvePdt3113BillingCalendar(Request),
  );

  await test.step('Precondition: term (contract A)', async () => {
    const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: volume price component (contract A)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = PDT_3113_VOLUME_PC_EXPRESSION;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  await test.step('Precondition: product A (volume only, no IAP)', async () => {
    const payload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    payload.priceComponentIds = [entityId(Responses.priceComponent[0])];
    payload.interimAdvancePayments = [];
    payload.interimAdvancePaymentGroups = [];
    const product = await Request.post(Endpoints.product, { data: payload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  let pod1Id = 0;
  await test.step('Precondition: POD #1', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
    pod1Id = entityId(Responses.pod[0]);
  });

  await test.step('Precondition: product contract A (POD #1 only → billing group 1)', async () => {
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    // Default generator uses WITHOUT_SUPPLY; product A allows SUPPLY_ONLY only.
    (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
    const contract = await Request.post(Endpoints.productContract, {
      data: contractPayload,
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  await test.step('Precondition: add second billing group on contract A', async () => {
    const bgPayload = await GeneratePayload.contractsAndOrders.addBillingGroup(0);
    const bgRes = await Request.post(Endpoints.billingGroup, { data: bgPayload });
    await expect(bgRes).CheckResponse();
  });

  let pod2Id = 0;
  await test.step('Precondition: POD #2', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
    pod2Id = entityId(Responses.pod[1]);
  });

  await test.step('Precondition: add POD #2 to billing group 2 on contract A', async () => {
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
    const editPayload = await GeneratePayload.contractsAndOrders.edit_ProductContract(
      contractPayload as any,
    );
    (editPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
    const newPodInfo = await GeneratePayload.contractsAndOrders.addPodToBillingGroup(
      1,
      Responses.pod[1].podDetailId,
    );
    (editPayload as any).podRequests.push((newPodInfo as any).addPod);
    (editPayload as any).productContractPointOfDeliveries.push(
      (newPodInfo as any).addPod.productContractPointOfDeliveries[0],
    );
    (editPayload as any).additionalParameters.estimatedTotalConsumptionUnderContractKwh =
      (newPodInfo as any).estimatedConsumption;
    const contractId = entityId(Responses.productContract[0]);
    const editRes = await Request.put(
      `product-contract/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: editPayload },
    );
    await expect(editRes).CheckResponse();
  });

  const { standardAnchor: anchor } = calendar;
  const podActivationA = calendar.contractA.podActivationDate;
  console.log(
    `[PDT-3113] OPEN calendar: A PODs from ${podActivationA}, standard ${anchor.profileStart}→${anchor.profileEnd} (ap=${anchor.accountingPeriodId}); ${anchor.commentary}`,
  );

  await test.step(`Precondition: activate POD #1 on contract A (${podActivationA})`, async () => {
    const act = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, podActivationA, undefined, 0),
    });
    await expect(act).CheckResponse();
  });

  await test.step(`Precondition: activate POD #2 on contract A (${podActivationA})`, async () => {
    const act = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(1, podActivationA, undefined, 0),
    });
    await expect(act).CheckResponse();
  });

  await test.step(`Precondition: billing-by-profile POD #1 volume=${PDT_3113_VOLUME_POD1}`, async () =>
    postBillingByProfile(fx, 0, anchor, PDT_3113_VOLUME_POD1));
  await test.step(`Precondition: billing-by-profile POD #2 volume=${PDT_3113_VOLUME_POD2}`, async () =>
    postBillingByProfile(fx, 1, anchor, PDT_3113_VOLUME_POD2));

  const contractAId = entityId(Responses.productContract[0]);
  const getA = await Request.get(`${Endpoints.productContract}/${contractAId}?versionId=1`);
  await expect(getA).CheckResponse();
  const bodyA = (await getA.json()) as {
    billingGroups?: { id?: number }[];
  };
  const billingGroupIds = (bodyA.billingGroups ?? [])
    .map((g) => Number(g.id))
    .filter((id) => Number.isFinite(id) && id > 0);
  expect(billingGroupIds.length, 'Contract A must have 2 billing groups').toBeGreaterThanOrEqual(2);

  return { calendar, contractAId, pod1Id, pod2Id, billingGroupIds };
}

/**
 * GE: billing group with several PODs — previous invoice is usable when it contains **at least one**
 * of those PODs. Contract A: 2 PODs in **one** billing group; energy data only on POD #1.
 */
export async function createPredecessorSameBillingGroupTwoPodsBillOne(
  fx: Pdt3113Fx,
  opts?: {
    calendar?: Pdt3113ValeriCalendar;
    volume?: number;
    volumeAnchor?: Pdt2376BillingAnchor;
  },
): Promise<{
  calendar: Pdt3113ValeriCalendar;
  contractAId: number;
  pod1Id: number;
  pod2Id: number;
  billingGroupIds: number[];
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const calendar =
    opts?.calendar ??
    (await test.step(
      'Precondition: resolve two consecutive OPEN accounting periods (Valeri month N / N+1)',
      async () => resolvePdt3113BillingCalendar(Request),
    ));
  const volume = opts?.volume ?? PDT_3113_VOLUME_POD2;
  const volumeAnchor = opts?.volumeAnchor ?? calendar.standardAnchor;

  await test.step('Precondition: term (contract A)', async () => {
    const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: volume price component (contract A)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = PDT_3113_VOLUME_PC_EXPRESSION;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  await test.step('Precondition: product A (volume only, no IAP)', async () => {
    const payload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    payload.priceComponentIds = [entityId(Responses.priceComponent[0])];
    payload.interimAdvancePayments = [];
    payload.interimAdvancePaymentGroups = [];
    const product = await Request.post(Endpoints.product, { data: payload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  let pod1Id = 0;
  await test.step('Precondition: POD #1', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
    pod1Id = entityId(Responses.pod[0]);
  });

  let pod2Id = 0;
  await test.step('Precondition: POD #2', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
    pod2Id = entityId(Responses.pod[1]);
  });

  await test.step('Precondition: product contract A (both PODs, one billing group)', async () => {
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
    (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
    const contract = await Request.post(Endpoints.productContract, {
      data: contractPayload,
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  const podActivationA = calendar.contractA.podActivationDate;
  console.log(
    `[PDT-3113] OPEN calendar (same BG, 2 PODs): A PODs from ${podActivationA}, volume ${volumeAnchor.profileStart}→${volumeAnchor.profileEnd} (ap=${volumeAnchor.accountingPeriodId}); ${volumeAnchor.commentary}`,
  );

  await test.step(`Precondition: activate POD #1 on contract A (${podActivationA})`, async () => {
    const act = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, podActivationA, undefined, 0),
    });
    await expect(act).CheckResponse();
  });

  await test.step(`Precondition: activate POD #2 on contract A (${podActivationA})`, async () => {
    const act = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(1, podActivationA, undefined, 0),
    });
    await expect(act).CheckResponse();
  });

  await test.step(
    `Precondition: billing-by-profile POD #1 only volume=${volume} (POD #2 has no energy — GE at-least-one POD)`,
    async () => postBillingByProfile(fx, 0, volumeAnchor, volume),
  );

  const contractAId = entityId(Responses.productContract[0]);
  const getA = await Request.get(`${Endpoints.productContract}/${contractAId}?versionId=1`);
  await expect(getA).CheckResponse();
  const bodyA = (await getA.json()) as {
    billingGroups?: { id?: number }[];
  };
  const billingGroupIds = (bodyA.billingGroups ?? [])
    .map((g) => Number(g.id))
    .filter((id) => Number.isFinite(id) && id > 0);
  expect(billingGroupIds.length, 'Contract A must have exactly 1 billing group').toBe(1);

  return { calendar, contractAId, pod1Id, pod2Id, billingGroupIds };
}

/** FOR_VOLUMES on contract A — expect `expectedCount` REAL standard invoices. */
export async function runStandardVolumeBillingTwoInvoices(
  fx: Pdt3113Fx,
  anchor: Pdt2376BillingAnchor,
  expectedCount = 2,
): Promise<Pdt3113InvoiceSnapshot[]> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'], 0, 0);
  applyVolumeBillingAnchor(billingPayload, anchor);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRunId = asBillingRunId(await billingRun.json());
  Responses.billingRun.push({ id: billingRunId, invoiceNumbers: expectedCount });

  const billingIndex = Responses.billingRun.length - 1;
  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, expectedCount, billingIndex);

  const invoiceIds = (Responses.invoice.slice(-expectedCount) as number[]).map((id) => Number(id));
  expect(invoiceIds.length, `Expected ${expectedCount} standard invoice(s) from volume billing`).toBe(
    expectedCount,
  );

  const snapshots: Pdt3113InvoiceSnapshot[] = [];
  for (const id of invoiceIds) {
    const snap = await fetchInvoiceSnapshot(Request, id);
    expect(snap.invoiceType, `Invoice ${id} type`).toBeTruthy();
    expect(snap.totalAmountIncludingVat, `Invoice ${id} amount`).toBeGreaterThan(0);
    snapshots.push(snap);
  }

  const amounts = snapshots.map((s) => s.totalAmountIncludingVat);
  if (expectedCount >= 2) {
    expect(
      amounts[0],
      `Standard invoices must differ (volumes ${PDT_3113_VOLUME_POD1} vs ${PDT_3113_VOLUME_POD2}); got ${amounts.join(', ')}`,
    ).not.toBeCloseTo(amounts[1], 2);
  }

  return snapshots;
}

/**
 * Successor product B + resigned contract B reusing the same 2 PODs / 2 billing groups,
 * with IAP PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT (50%).
 * Second billing group is added while DRAFT; both PODs are attached on the single SIGNED PUT
 * (no post-sign product-contract edit).
 */
export async function createResignedContractWithIap(
  fx: Pdt3113Fx,
  calendar: Pdt3113ValeriCalendar,
  opts?: { billingGroupMode?: 'separate' | 'same' },
): Promise<{ contractBId: number; iapId: number }> {
  const billingGroupMode = opts?.billingGroupMode ?? 'separate';
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const baseProductId = entityId(Responses.product[0]);
  const bPodActivation = calendar.contractB.podActivationDate;
  const bSigningDate = calendar.contractB.signingDate;
  const bVersionStartDate = calendar.contractB.versionStartDate;

  await test.step('Precondition: second term (product B)', async () => {
    const termPayload = GeneratePayload.productAndServices.term();
    termPayload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
    termPayload.waitForOldContractTermToExpires = ['NO'];
    termPayload.contractEntryIntoForces = ['SIGNING'];
    termPayload.startsOfContractInitialTerms = ['SIGNING'];
    const term = await Request.post(Endpoints.terms, { data: termPayload });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: volume PC for product B', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = PDT_3113_VOLUME_PC_EXPRESSION;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  await test.step('Precondition: interim PC for product B (REG-1154 / PDT-2750)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = 10;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  let iapId = 0;
  await test.step('Precondition: IAP PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT 50%', async () => {
    const payload = GeneratePayload.productAndServices.interim();
    payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
    payload.value = PDT_3113_IAP_PERCENT;
    payload.missingInvoice = true;
    payload.priceComponentId = null;
    const interim = await Request.post(Endpoints.interim, { data: payload });
    await expect(interim).CheckResponse();
    const body = await interim.json();
    Responses.interim.push(body);
    iapId = entityId(body);
  });

  await test.step('Precondition: product B (resign catalog + IAP)', async () => {
    const volB = entityId(Responses.priceComponent[Responses.priceComponent.length - 2]);
    const interimPcB = entityId(Responses.priceComponent[Responses.priceComponent.length - 1]);
    const payload = GeneratePayload.productAndServices.product(1) as Record<string, unknown>;
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.priceComponentIds = [volB, interimPcB];
    payload.interimAdvancePayments = [iapId];
    payload.interimAdvancePaymentGroups = [];
    (payload as any).isResigning = true;
    (payload as any).resignProductTargets = [baseProductId];
    const product = await Request.post(Endpoints.product, { data: payload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  let contractBId = 0;
  await test.step(
    `Precondition: contract B DRAFT→SIGNED (2 PODs / ${billingGroupMode === 'same' ? '1 billing group' : '2 billing groups'})`,
    async () => {
    const allInterims = [...Responses.interim];
    Responses.interim.length = 0;
    Responses.interim.push(allInterims[allInterims.length - 1]);

    try {
      const draftPayload =
        billingGroupMode === 'same'
          ? await GeneratePayload.contractsAndOrders.product_contract(0, 1)
          : await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
      (draftPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
      const signingDate = bSigningDate;
      draftPayload.basicParameters.status = 'DRAFT';
      draftPayload.basicParameters.subStatus = 'DRAFT';
      draftPayload.basicParameters.versionStatus = 'SIGNED';
      draftPayload.basicParameters.signingDate = null;
      draftPayload.productParameters.entryIntoForce = 'SIGNING';
      draftPayload.productParameters.startOfContractInitialTerm = 'SIGNING';
      draftPayload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
      (draftPayload.productParameters as any).productContractWaitForOldContractTermToExpires = 'NO';
      const contract = await Request.post(Endpoints.productContract, { data: draftPayload });
      await expect(contract).CheckResponse();
      contractBId = entityId(await contract.json());

      // Keep Responses.productContract entries as objects with .id (addBillingGroup reads
      // this.responses.productContract[0].id — bare numbers make .id undefined and crash).
      const getB = await Request.get(`${Endpoints.productContract}/${contractBId}?versionId=1`);
      await expect(getB).CheckResponse();
      const contractBDraft = (await getB.json()) as Record<string, unknown>;
      const contractB = { ...contractBDraft, id: entityId(contractBDraft) || contractBId };
      Responses.productContract.push(contractB);

      if (billingGroupMode === 'separate') {
        // While DRAFT: add second billing group (helpers read productContract[0] — scope to B).
        const savedContracts = [...Responses.productContract];
        Responses.productContract.length = 0;
        Responses.productContract.push(contractB);
        try {
          const bgPayload = await GeneratePayload.contractsAndOrders.addBillingGroup(0);
          const bgRes = await Request.post(Endpoints.billingGroup, { data: bgPayload });
          await expect(bgRes).CheckResponse();
        } finally {
          Responses.productContract.length = 0;
          for (const row of savedContracts) Responses.productContract.push(row);
        }
      }

      const getBWithGroups = await Request.get(
        `${Endpoints.productContract}/${contractBId}?versionId=1`,
      );
      await expect(getBWithGroups).CheckResponse();
      const contractBWithGroups = (await getBWithGroups.json()) as {
        billingGroups?: { id?: number }[];
        versions?: Record<string, unknown>[];
      };
      const billingGroups = contractBWithGroups.billingGroups ?? [];
      const minGroups = billingGroupMode === 'same' ? 1 : 2;
      expect(
        billingGroups.length,
        `Contract B ${contractBId} must have ≥${minGroups} billing group(s) while DRAFT before SIGNED`,
      ).toBeGreaterThanOrEqual(minGroups);

      const bg0Id = Number(billingGroups[0].id);
      expect(bg0Id, 'Contract B billing group 0 id').toBeGreaterThan(0);

      const pod0DetailId = Number(Responses.pod[0].podDetailId);
      const pod1DetailId = Number(Responses.pod[1].podDetailId);
      const podRequests =
        billingGroupMode === 'same'
          ? [
              {
                billingGroupId: bg0Id,
                productContractPointOfDeliveries: [
                  { pointOfDeliveryDetailId: pod0DetailId, dealNumber: null },
                  { pointOfDeliveryDetailId: pod1DetailId, dealNumber: null },
                ],
              },
            ]
          : [
              {
                billingGroupId: bg0Id,
                productContractPointOfDeliveries: [
                  { pointOfDeliveryDetailId: pod0DetailId, dealNumber: null },
                ],
              },
              {
                billingGroupId: Number(billingGroups[1].id),
                productContractPointOfDeliveries: [
                  { pointOfDeliveryDetailId: pod1DetailId, dealNumber: null },
                ],
              },
            ];
      if (billingGroupMode === 'separate') {
        expect(Number(billingGroups[1].id), 'Contract B billing group 1 id').toBeGreaterThan(0);
      }

      const signedPayload = (await (billingGroupMode === 'same'
        ? GeneratePayload.contractsAndOrders.product_contract(0, 1)
        : GeneratePayload.contractsAndOrders.product_contract(0, 1, 0))) as any;
      (signedPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
      signedPayload.basicParameters.status = 'SIGNED';
      signedPayload.basicParameters.subStatus = 'SIGNED_BY_BOTH_SIDES';
      signedPayload.basicParameters.versionStatus = 'SIGNED';
      signedPayload.basicParameters.signingDate = signingDate;
      signedPayload.productParameters.entryIntoForce = 'SIGNING';
      signedPayload.productParameters.startOfContractInitialTerm = 'SIGNING';
      signedPayload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
      (signedPayload.productParameters as any).productContractWaitForOldContractTermToExpires = 'NO';
      (signedPayload.additionalParameters as any).employeeId = 154;
      (signedPayload.additionalParameters as any).riskAssessment = 'PERMIT';
      signedPayload.savingAsNewVersion = false;
      signedPayload.startDate = bVersionStartDate;
      signedPayload.podRequests = podRequests;
      signedPayload.productContractPointOfDeliveries = [
        { pointOfDeliveryDetailId: pod0DetailId, dealNumber: null },
        { pointOfDeliveryDetailId: pod1DetailId, dealNumber: null },
      ];

      // Sum annual estimated consumption (MWh) from both selected PODs — required for SIGNED PUT validation.
      let annualMwh = 0;
      for (const i of [0, 1] as const) {
        const podGet = await Request.get(`pod/${Responses.pod[i].id}?version=1`);
        await expect(podGet).CheckResponse();
        const podJson = await podGet.json();
        const monthly =
          podJson.type === 'CONSUMER' ? Number(podJson.estimatedMonthlyAvgConsumption ?? 0) : 0;
        annualMwh += (monthly * 12) / 1000;
      }
      signedPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = annualMwh;

      const putB = await Request.put(
        `${Endpoints.productContract}/${contractBId}?versionId=1&changeFutureVersionsPods=false`,
        { data: signedPayload },
      );
      await expect(putB).CheckResponse();

      const getBSigned = await Request.get(
        `${Endpoints.productContract}/${contractBId}?versionId=1`,
      );
      await expect(getBSigned).CheckResponse();
      const contractBSignedRaw = (await getBSigned.json()) as {
        id?: number;
        billingGroups?: unknown[];
        basicParameters?: { id?: number };
      };
      const contractBSigned = {
        ...contractBSignedRaw,
        id: entityId(contractBSignedRaw) || contractBId,
      };
      expect(
        contractBSigned.billingGroups?.length,
        `Contract B ${contractBId} must have ≥${minGroups} billing group(s) after SIGNED`,
      ).toBeGreaterThanOrEqual(minGroups);
      const bIdx = Responses.productContract.findIndex((c) => entityId(c) === contractBId);
      if (bIdx >= 0) Responses.productContract[bIdx] = contractBSigned;
      else Responses.productContract.push(contractBSigned);
    } finally {
      Responses.interim.length = 0;
      for (const row of allInterims) Responses.interim.push(row);
    }
  });

  await test.step('Precondition: validate resign link between contract A and B', async () => {
    const contractAId = entityId(Responses.productContract[0]);
    const getA = await Request.get(`${Endpoints.productContract}/${contractAId}`);
    await expect(getA).CheckResponse();
    const bodyA = (await getA.json()) as Record<string, unknown>;
    const getB = await Request.get(`${Endpoints.productContract}/${contractBId}`);
    await expect(getB).CheckResponse();
    const bodyB = (await getB.json()) as Record<string, unknown>;
    const basicA = (bodyA.basicParameters ?? {}) as Record<string, unknown>;
    const basicB = (bodyB.basicParameters ?? {}) as Record<string, unknown>;
    const resignedToA = basicA.resignedTo;
    const resignedFromB = basicB.resignedFrom;
    const hasResignLink =
      (Array.isArray(resignedToA) && resignedToA.length > 0) ||
      (Array.isArray(resignedFromB) && resignedFromB.length > 0);
    expect(hasResignLink, 'Resign link must exist between predecessor A and successor B').toBeTruthy();
  });

  await test.step('Precondition: terminate contract A (Valeri: TERMINATED before B POD activation)', async () => {
    const contractAId = entityId(Responses.productContract[0]);
    expect(contractAId, 'Contract A id must differ from contract B').not.toBe(contractBId);
    const terminateA = await Request.put(
      `product-contract/status-update/${contractAId}?versionId=1`,
      {
        data: {
          contractStatus: 'TERMINATED',
          contractSubStatus: PDT_3113_VALERI_TEMPLATE.contractA.terminateSubStatus,
          contractVersionStatus: 'SIGNED',
        },
      },
    );
    await expect(terminateA).CheckResponse();
  });

  await test.step(`Precondition: activate both PODs on B (${bPodActivation})`, async () => {
    const contractBIndex = Responses.productContract.findIndex((c) => entityId(c) === contractBId);
    expect(contractBIndex, 'Contract B index in Responses').toBeGreaterThanOrEqual(0);
    for (const podIndex of [0, 1] as const) {
      const act = await Request.post('/contract-pods/manual', {
        data: await GeneratePayload.pointsOfDelivery.pod_activation(
          podIndex,
          bPodActivation,
          undefined,
          contractBIndex,
        ),
      });
      await expect(act).CheckResponse();
    }
  });

  await test.step('Precondition: verify both PODs active on contract B', async () => {
    const getB = await Request.get(`${Endpoints.productContract}/${contractBId}`);
    await expect(getB).CheckResponse();
    const bodyB = (await getB.json()) as {
      contractPodsResponses?: Array<{
        identifier?: string;
        activationDate?: string | null;
      }>;
    };
    const pods = bodyB.contractPodsResponses ?? [];
    expect(pods.length, 'Contract B POD rows').toBeGreaterThanOrEqual(2);
    for (const pod of pods) {
      expect(pod.activationDate, `POD ${pod.identifier} must be active on B`).toBeTruthy();
      expect(pod.activationDate!.slice(0, 10), `POD ${pod.identifier} activation date`).toBe(
        bPodActivation,
      );
    }
  });

  await test.step(
    `Precondition: contract B ${PDT_3113_VALERI_TEMPLATE.contractB.contractStatus}/${PDT_3113_VALERI_TEMPLATE.contractB.contractSubStatus}`,
    async () => {
      const getB = await Request.get(`${Endpoints.productContract}/${contractBId}`);
      await expect(getB).CheckResponse();
      const contractB = (await getB.json()) as Record<string, unknown>;
      const versions = (contractB.versions as Record<string, unknown>[] | undefined) ?? [];
      const signed =
        versions.find((v) => {
          const bp = v.basicParameters as Record<string, unknown> | undefined;
          return (
            v.contractVersionStatus === 'SIGNED' ||
            bp?.contractVersionStatus === 'SIGNED' ||
            v.status === 'SIGNED'
          );
        }) ?? versions[0];
      const versionId = Number((signed as any)?.versionId ?? (signed as any)?.id ?? 1);
      const setActive = await Request.put(
        `${Endpoints.productContract}/status-update/${contractBId}?versionId=${versionId}`,
        {
          data: {
            contractStatus: PDT_3113_VALERI_TEMPLATE.contractB.contractStatus,
            contractSubStatus: PDT_3113_VALERI_TEMPLATE.contractB.contractSubStatus,
            contractVersionStatus: 'SIGNED',
          },
        },
      );
      await expect(setActive).CheckResponse();
    },
  );

  console.log(
    `[PDT-3113] resign calendar: standard=${calendar.standardAnchor.profileStart} interim=${calendar.interimAnchor.profileStart} B PODs=${bPodActivation} B signing=${bSigningDate} start=${bVersionStartDate}`,
  );

  return { contractBId, iapId };
}

/** INTERIM_AND_ADVANCE_PAYMENT billing on contract B — expect `expectedCount` interim invoices. */
export async function runInterimBillingOnContractB(
  fx: Pdt3113Fx,
  contractBIndex: number,
  interimAnchor: Pdt2376BillingAnchor,
  expectedCount = 2,
): Promise<{ billingRunId: number; interimInvoices: Pdt3113InvoiceSnapshot[] }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const billingPayload = await GeneratePayload.billing.billingRun(
    'CONTRACT',
    ['INTERIM_AND_ADVANCE_PAYMENT'],
    0,
    contractBIndex,
  );
  applyInterimBillingAnchor(billingPayload, interimAnchor);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRunId = asBillingRunId(await billingRun.json());
  Responses.billingRun.push(billingRunId);

  const startInterim = await Request.patch(
    `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
  );
  await expect(startInterim).CheckResponse();
  await waitUntilBillingRunStatus(Request, billingRunId, 'DRAFT');

  const billingIndex = Responses.billingRun.length - 1;
  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, expectedCount, billingIndex);

  const invoiceIds = (Responses.invoice.slice(-expectedCount) as number[]).map((id) => Number(id));
  expect(invoiceIds.length, `Expected ${expectedCount} interim invoice(s)`).toBe(expectedCount);

  const interimInvoices: Pdt3113InvoiceSnapshot[] = [];
  for (const id of invoiceIds) {
    const snap = await fetchInvoiceSnapshot(Request, id);
    expect(
      snap.invoiceType,
      `Invoice ${id} must be INTERIM_AND_ADVANCE_PAYMENT`,
    ).toBe('INTERIM_AND_ADVANCE_PAYMENT');
    interimInvoices.push(snap);
  }

  return { billingRunId, interimInvoices };
}

/**
 * Same INTERIM start as {@link runInterimBillingOnContractB}, but 0 invoices is allowed
 * (GE negative: no in-window previous REAL — generation may produce nothing).
 */
export async function runInterimBillingOnContractBAllowMissing(
  fx: Pdt3113Fx,
  contractBIndex: number,
  interimAnchor: Pdt2376BillingAnchor,
): Promise<{ billingRunId: number; interimInvoices: Pdt3113InvoiceSnapshot[] }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const billingPayload = await GeneratePayload.billing.billingRun(
    'CONTRACT',
    ['INTERIM_AND_ADVANCE_PAYMENT'],
    0,
    contractBIndex,
  );
  applyInterimBillingAnchor(billingPayload, interimAnchor);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRunId = asBillingRunId(await billingRun.json());
  Responses.billingRun.push(billingRunId);

  const startInterim = await Request.patch(
    `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
  );
  await expect(startInterim).CheckResponse();
  await waitUntilBillingRunStatus(Request, billingRunId, 'DRAFT');

  const pollDeadline = Date.now() + 4 * 60 * 1000;
  let foundIds: number[] = [];
  while (Date.now() < pollDeadline) {
    const listRes = await Request.get(
      `${BILLING_RUN_ROOT}/draft-invoices?id=${billingRunId}&page=0&size=50`,
    );
    await expect(listRes).CheckResponse();
    const listBody = (await listRes.json()) as { content?: Array<{ id?: unknown; invoiceType?: unknown }> };
    foundIds = (listBody.content ?? [])
      .filter((row) => {
        const type = String(row.invoiceType ?? '');
        return type === 'INTERIM_AND_ADVANCE_PAYMENT' || type === '';
      })
      .map((row) => Number(row.id))
      .filter((id) => Number.isFinite(id) && id > 0);
    if (foundIds.length > 0) break;
    await new Promise((r) => setTimeout(r, POLL_MS));
  }

  if (foundIds.length === 0) {
    return { billingRunId, interimInvoices: [] };
  }

  const billingIndex = Responses.billingRun.length - 1;
  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, foundIds.length, billingIndex);
  const invoiceIds = (Responses.invoice.slice(-foundIds.length) as number[]).map((id) => Number(id));
  const interimInvoices: Pdt3113InvoiceSnapshot[] = [];
  for (const id of invoiceIds) {
    const snap = await fetchInvoiceSnapshot(Request, id);
    expect(
      snap.invoiceType,
      `Invoice ${id} must be INTERIM_AND_ADVANCE_PAYMENT`,
    ).toBe('INTERIM_AND_ADVANCE_PAYMENT');
    interimInvoices.push(snap);
  }
  return { billingRunId, interimInvoices };
}

function podsOverlap(a: string[], b: string[]): boolean {
  const setB = new Set(b.map((p) => p.trim()).filter(Boolean));
  return a.some((p) => setB.has(p.trim()));
}

function findStandardForInterim(
  standards: Pdt3113InvoiceSnapshot[],
  interim: Pdt3113InvoiceSnapshot,
): Pdt3113InvoiceSnapshot | undefined {
  if (interim.podIdentifiers.length > 0) {
    const byPod = standards.find((s) => podsOverlap(s.podIdentifiers, interim.podIdentifiers));
    if (byPod) return byPod;
  }
  // Fallback: match by amount proximity to percent-of-standard (when POD rows missing on API).
  let best: Pdt3113InvoiceSnapshot | undefined;
  let bestDelta = Number.POSITIVE_INFINITY;
  for (const s of standards) {
    const expected = round2(s.totalAmountIncludingVat * (PDT_3113_IAP_PERCENT / 100));
    const delta = Math.abs(interim.totalAmountIncludingVat - expected);
    if (delta < bestDelta) {
      bestDelta = delta;
      best = s;
    }
  }
  return best;
}

/**
 * Correct product behavior (diagram): each interim for a billing group / POD set must be based on
 * the standard invoice that includes those PODs — not a single shared latest invoice.
 *
 * Match standards (contract A) to interims (contract B) by **POD identifiers**, not billingGroupId
 * (BG ids differ across resigned contracts).
 *
 * This assertion encodes the **correct** expected result so the test **fails on Dev today** until PDT-3113 is fixed.
 */
export function assertInterimUsesBillingGroupSpecificPrevious(
  standards: Pdt3113InvoiceSnapshot[],
  interims: Pdt3113InvoiceSnapshot[],
  percent: number = PDT_3113_IAP_PERCENT,
): {
  passed: boolean;
  expectedResult: string;
  actualResult: string;
  details: Record<string, unknown>;
} {
  expect(standards.length).toBe(2);
  expect(interims.length).toBe(2);

  const fromIds = interims.map((i) => i.interimCalculatedFromInvoiceId);
  const amounts = interims.map((i) => i.totalAmountIncludingVat);

  const perChecks: Array<Record<string, unknown>> = [];
  const matchedStdIds = new Set<number>();
  let allMatch = true;

  for (const interim of interims) {
    const std = findStandardForInterim(standards, interim);
    expect(std, `No matching standard invoice for interim ${interim.id}`).toBeTruthy();
    matchedStdIds.add(std!.id);
    const expectedAmount = round2(std!.totalAmountIncludingVat * (percent / 100));
    const amountOk = Math.abs(interim.totalAmountIncludingVat - expectedAmount) < 0.05;
    const fromOk =
      interim.interimCalculatedFromInvoiceId == null ||
      interim.interimCalculatedFromInvoiceId === std!.id;
    if (!amountOk || !fromOk) allMatch = false;
    perChecks.push({
      interimId: interim.id,
      interimBillingGroupId: interim.billingGroupId,
      interimAmount: interim.totalAmountIncludingVat,
      expectedAmount,
      amountOk,
      interimCalculatedFromInvoiceId: interim.interimCalculatedFromInvoiceId,
      expectedFromInvoiceId: std!.id,
      fromOk,
      standardAmount: std!.totalAmountIncludingVat,
      standardBillingGroupId: std!.billingGroupId,
      interimPods: interim.podIdentifiers,
      standardPods: std!.podIdentifiers,
    });
  }

  // Each interim must map to a different standard when standards differ.
  if (matchedStdIds.size < 2) allMatch = false;

  const sharedFromBug =
    fromIds.every((id) => id != null) &&
    fromIds[0] === fromIds[1] &&
    standards[0].id !== standards[1].id;
  const sharedAmountBug =
    Math.abs(amounts[0] - amounts[1]) < 0.05 &&
    Math.abs(standards[0].totalAmountIncludingVat - standards[1].totalAmountIncludingVat) >= 0.05;

  if (sharedFromBug || sharedAmountBug) allMatch = false;

  const expectedResult =
    `Each interim uses the standard invoice that includes the same POD(s): ` +
    `distinct previous bases / amounts ≈ ${percent}% of each matching standard. ` +
    `Standards: ${standards.map((s) => `#${s.id}=${s.totalAmountIncludingVat} pods=[${s.podIdentifiers.join(',')}]`).join('; ')}.`;

  const actualResult = allMatch
    ? `As expected — POD-specific interim bases and amounts match. details=${JSON.stringify(perChecks)}`
    : `Not as expected (PDT-3113 bug symptom) — sharedFrom=${sharedFromBug} sharedAmount=${sharedAmountBug} ` +
      `matchedStdIds=${[...matchedStdIds].join(',')}; fromIds=${JSON.stringify(fromIds)}; ` +
      `amounts=${JSON.stringify(amounts)}; checks=${JSON.stringify(perChecks)}`;

  return {
    passed: allMatch,
    expectedResult,
    actualResult,
    details: {
      standards: standards.map((s) => ({
        id: s.id,
        billingGroupId: s.billingGroupId,
        amount: s.totalAmountIncludingVat,
        pods: s.podIdentifiers,
      })),
      interims: perChecks,
      sharedFromBug,
      sharedAmountBug,
      matchedStdIds: [...matchedStdIds],
    },
  };
}

/**
 * GE: interim amount is `percent` of the given previous standard (at-least-one POD / single BG).
 * `interimCalculatedFromInvoiceId` null is treated as fromOk (API often omits it on Dev).
 */
export function assertInterimPercentFromPrevious(
  standard: Pdt3113InvoiceSnapshot,
  interim: Pdt3113InvoiceSnapshot,
  percent: number,
): { passed: boolean; expectedResult: string; actualResult: string } {
  const expectedAmt = round2(standard.totalAmountIncludingVat * (percent / 100));
  const amountOk = Math.abs(interim.totalAmountIncludingVat - expectedAmt) < 0.05;
  const fromId = interim.interimCalculatedFromInvoiceId;
  const fromOk = fromId == null || fromId === standard.id;
  const passed = amountOk && fromOk;
  return {
    passed,
    expectedResult:
      `Interim #${interim.id} amount ≈ ${percent}% of standard #${standard.id} ` +
      `(${standard.totalAmountIncludingVat} → ${expectedAmt}); fromId null or ${standard.id}.`,
    actualResult: passed
      ? `As expected — interim amt=${interim.totalAmountIncludingVat} fromId=${fromId}`
      : `Not as expected — interim amt=${interim.totalAmountIncludingVat} expected=${expectedAmt} fromId=${fromId}`,
  };
}

/**
 * GE negative: an out-of-window REAL standard must not be the PERCENT_FROM_PREVIOUS basis.
 * No interim at all is also valid (no in-window previous invoice).
 */
export function assertInterimDoesNotUsePrevious(
  standard: Pdt3113InvoiceSnapshot,
  interim: Pdt3113InvoiceSnapshot | undefined,
  percent: number,
): { passed: boolean; expectedResult: string; actualResult: string } {
  const forbiddenAmt = round2(standard.totalAmountIncludingVat * (percent / 100));
  if (!interim) {
    return {
      passed: true,
      expectedResult:
        `Must not use out-of-window REAL #${standard.id} (${standard.totalAmountIncludingVat} → ${forbiddenAmt} at ${percent}%). ` +
        `No in-window previous invoice — no basis, or a non-stale fallback.`,
      actualResult: 'As expected — no interim invoice generated (stale REAL was not used as basis).',
    };
  }
  const amountUsedStale = Math.abs(interim.totalAmountIncludingVat - forbiddenAmt) < 0.05;
  const fromUsedStale = interim.interimCalculatedFromInvoiceId === standard.id;
  const passed = !amountUsedStale && !fromUsedStale;
  return {
    passed,
    expectedResult:
      `Interim must not be ${percent}% of out-of-window REAL #${standard.id} ` +
      `(forbidden amount ${forbiddenAmt}) and fromId must not be ${standard.id}.`,
    actualResult: passed
      ? `As expected — interim #${interim.id} amt=${interim.totalAmountIncludingVat} fromId=${interim.interimCalculatedFromInvoiceId} (not stale #${standard.id}).`
      : `Not as expected — interim #${interim.id} amt=${interim.totalAmountIncludingVat} fromId=${interim.interimCalculatedFromInvoiceId} matches stale #${standard.id} / ${forbiddenAmt}.`,
  };
}
