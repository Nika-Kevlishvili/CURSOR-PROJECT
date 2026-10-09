/**
 * PDT-2376 — shared helpers for multi-POD volume + WITH_ELECTRICITY_INVOICE (billing-by-profile scoped to POD index 0).
 *
 * Billing-by-profile rule (test contract):
 * - `POST /billing-by-profile` is sent **only** for **POD index 0** (first POD).
 * - POD 0 uses a valid activation date and **no** deactivation (`deactivationDate: null`).
 *
 * Invoice line rule:
 * - Only PODs that have billing profile data can get WITH_ELECTRICITY-style lines from that path — here **only POD index 0**.
 * - Other PODs remain on the activation/deactivation matrix for contract-POD lifecycle coverage but must **not** be expected
 *   to show WITH_ELECTRICITY rows from profile-backed electricity in this scenario.
 *
 * Reference specs:
 * - `tests/billing/electricity/withElectricity(Product).spec.ts` (volume + electricity price components, billing run models)
 * - `tests/billing/pulling/pulling.spec.ts` (billing-by-profile patterns)
 * - `tests/cursor/pdt-2599-service-contract.fixtures.ts` (`resolveAccountingPeriodIdForBillingDate`, `asBillingRunId`)
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';

/** `RequestWrapper` — matches helpers imported from {@link pdt-2599-service-contract.fixtures.ts}. */
type FixtureRequest = baseFixture['Request'];
import {
  asBillingRunId,
  resolveAccountingPeriodIdForBillingDate,
} from './pdt-2599-service-contract.fixtures';

/** Only this contract-POD index receives `POST /billing-by-profile` rows in PDT-2376. */
export const PDT_2376_BILLING_BY_PROFILE_POD_INDEX = 0;

/** Target month-end from screenshot-style scenario; may be replaced if no OPEN period contains it (see {@link resolvePdt2376BillingAnchor}). */
export const PDT_2376_DESIRED_INVOICE_PERIOD_END = '2025-12-31';

export type Pdt2376PodMatrixRow = {
  /** Stable label for reporting */
  label: string;
  activationDate: string;
  deactivationDate: string | null;
  /**
   * Calendar eligibility (deactivation vs invoice month) for POD index 0 when it has profile data.
   * For indices > 0 this scenario does **not** assert profile-backed WITH_ELECTRICITY rows.
   */
  expectedEligible: boolean;
};

export type Pdt2376BillingAnchor = {
  invoicePeriodTo: string;
  taxEventDate: string;
  invoiceDate: string;
  accountingPeriodId: number;
  profileStart: string;
  profileEnd: string;
  /** Human-readable note when dates were adapted to Dev accounting periods */
  commentary: string;
};

type OpenApRow = {
  id?: number;
  startDate?: string;
  endDate?: string;
  status?: string;
};

function yearMonthCalendar(yyyyMmDd: string): string {
  return yyyyMmDd.slice(0, 7);
}

export function computeExpectedWithElectricityEligibility(
  deactivationDate: string | null,
  invoicePeriodTo: string,
): boolean {
  if (deactivationDate == null) return true;
  return yearMonthCalendar(deactivationDate) >= yearMonthCalendar(invoicePeriodTo);
}

/** Last calendar day of `(year, month1to12)`. */
function lastDayOfMonth(year: number, month1to12: number): string {
  const d = new Date(Date.UTC(year, month1to12, 0));
  return d.toISOString().slice(0, 10);
}

function monthFirstFromYmd(endYmd: string): string {
  return `${endYmd.slice(0, 7)}-01`;
}

function prevMonthLastDay(fromYmd: string): string {
  const y = Number(fromYmd.slice(0, 4));
  const m = Number(fromYmd.slice(5, 7));
  return lastDayOfMonth(y, m - 1);
}

function prevMonthFirstDay(fromYmd: string): string {
  const y = Number(fromYmd.slice(0, 4));
  const m = Number(fromYmd.slice(5, 7));
  const dt = new Date(Date.UTC(y, m - 2, 1));
  return dt.toISOString().slice(0, 10);
}

function midMonthDay(fromYmd: string): string {
  const y = Number(fromYmd.slice(0, 4));
  const m = Number(fromYmd.slice(5, 7));
  const dt = new Date(Date.UTC(y, m - 1, 15));
  return dt.toISOString().slice(0, 10);
}

/** Last day of calendar month that is `monthsBack` months before `fromYmd`'s month (same day-of-month clamp not used; always true month end). */
function monthsBeforeMonthEnd(fromYmd: string, monthsBack: number): string {
  const y = Number(fromYmd.slice(0, 4));
  const m = Number(fromYmd.slice(5, 7));
  const ref = new Date(Date.UTC(y, m - 1, 1));
  ref.setUTCMonth(ref.getUTCMonth() - monthsBack);
  const ny = ref.getUTCFullYear();
  const nm = ref.getUTCMonth() + 1;
  return lastDayOfMonth(ny, nm);
}

/**
 * Eight PODs: mix of same-month / prior-month deactivation and null, mirroring spreadsheet-style coverage.
 * Dates are derived from the resolved `invoicePeriodTo` (typically month-end).
 */
export function buildPdt2376PodMatrix(invoicePeriodTo: string): Pdt2376PodMatrixRow[] {
  const monthFirst = monthFirstFromYmd(invoicePeriodTo);
  /**
   * Activation must be on/before every matrix deactivation date. P5–P7 use prior months
   * (including two months back for P7); same-month-only activation (monthFirst) made
   * `deactivation < activation` and API validation failed. Anchor activation at the first day
   * of the month that is three month-ends before invoicePeriodTo so all deactivation dates stay valid.
   * POD index 0 uses `monthFirst` activation (in invoice month); others share `podActivationDate`.
   */
  const podActivationDate = monthFirstFromYmd(monthsBeforeMonthEnd(invoicePeriodTo, 3));
  const rows: Array<Omit<Pdt2376PodMatrixRow, 'expectedEligible'> & { deactivationDate: string | null }> = [
    /** POD index 0: in-period activation, never deactivated — sole recipient of billing-by-profile in this test. */
    { label: 'P1', activationDate: monthFirst, deactivationDate: null },
    { label: 'P2', activationDate: podActivationDate, deactivationDate: invoicePeriodTo },
    { label: 'P3', activationDate: podActivationDate, deactivationDate: midMonthDay(invoicePeriodTo) },
    { label: 'P4', activationDate: podActivationDate, deactivationDate: monthFirst },
    { label: 'P5', activationDate: podActivationDate, deactivationDate: prevMonthLastDay(invoicePeriodTo) },
    { label: 'P6', activationDate: podActivationDate, deactivationDate: prevMonthFirstDay(invoicePeriodTo) },
    { label: 'P7', activationDate: podActivationDate, deactivationDate: monthsBeforeMonthEnd(invoicePeriodTo, 2) },
    {
      label: 'P8',
      activationDate: podActivationDate,
      deactivationDate: null,
    },
  ];

  return rows.map((r) => ({
    ...r,
    expectedEligible: computeExpectedWithElectricityEligibility(r.deactivationDate, invoicePeriodTo),
  }));
}

export async function resolvePdt2376BillingAnchor(Request: FixtureRequest): Promise<Pdt2376BillingAnchor> {
  const desired = PDT_2376_DESIRED_INVOICE_PERIOD_END;
  let commentary = `Preferred invoice period end ${desired} (screenshot / ticket style).`;

  const listRes = await Request.get('billing-run/accounting-period-available-list?page=0&size=50');
  await expect(listRes).CheckResponse();
  const page = (await listRes.json()) as { content?: OpenApRow[] };
  const open = (page.content ?? []).filter((r) => String(r.status).toUpperCase() === 'OPEN');

  let invoicePeriodTo = desired;
  let accountingPeriodId = await resolveAccountingPeriodIdForBillingDate(Request, invoicePeriodTo);

  const hit = open.find((r) => {
    const a = r.startDate?.slice(0, 10);
    const b = r.endDate?.slice(0, 10);
    return a != null && b != null && desired >= a && desired <= b && r.id != null;
  });
  if (!hit) {
    const fallback = open.sort((a, b) => String(b.endDate).localeCompare(String(a.endDate)))[0];
    if (fallback?.id != null && fallback.endDate) {
      invoicePeriodTo = fallback.endDate.slice(0, 10);
      accountingPeriodId = fallback.id;
      commentary += ` No OPEN period contained ${desired}; using latest OPEN period id=${accountingPeriodId} with end ${invoicePeriodTo}.`;
    }
  } else {
    accountingPeriodId = hit.id as number;
    commentary += ` OPEN accounting period id=${accountingPeriodId} contains ${desired}.`;
  }

  const apCheck = await Request.get(`accounting-period/${accountingPeriodId}`);
  await expect(apCheck).CheckResponse();
  const apBody = (await apCheck.json()) as { startDate?: string; endDate?: string };
  const hi = apBody.endDate?.slice(0, 10);
  if (hi && invoicePeriodTo > hi) {
    invoicePeriodTo = hi;
    commentary += ` Clamped invoicePeriodTo to accounting period end ${hi}.`;
  }

  const profileStart = monthFirstFromYmd(invoicePeriodTo);
  const taxEventDate = invoicePeriodTo;
  const invoiceDate = invoicePeriodTo;

  return {
    invoicePeriodTo,
    taxEventDate,
    invoiceDate,
    accountingPeriodId,
    profileStart,
    profileEnd: invoicePeriodTo,
    commentary,
  };
}

export type ContractPodRow = {
  podId?: number;
  identifier?: string | number;
  podDetailId?: number;
};

export async function loadProductContractContext(
  Request: FixtureRequest,
  contractId: number,
): Promise<{ contractDetailId: number; contractPods: ContractPodRow[] }> {
  const cg = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(cg).CheckResponse();
  const contract = (await cg.json()) as Record<string, unknown>;
  const versions = (contract.versions as Record<string, unknown>[] | undefined) ?? [];
  const v0 = versions[0] as { id?: number } | undefined;
  const contractDetailId = Number(v0?.id ?? 0);
  if (!Number.isFinite(contractDetailId) || contractDetailId <= 0) {
    throw new Error(`[PDT-2376] Missing contract version/detail id on product-contract ${contractId}`);
  }
  const contractPods = (contract.contractPodsResponses as ContractPodRow[] | undefined) ?? [];
  return { contractDetailId, contractPods };
}

/** POST `/contract-pods/manual` for each created POD in order, using matrix dates. */
export async function applyPdt2376PodActivationMatrix(args: {
  Request: FixtureRequest;
  contractId: number;
  pods: Array<{ id: number }>;
  matrix: Pdt2376PodMatrixRow[];
  deactivationPurposeId: string | number;
}): Promise<void> {
  const { contractDetailId, contractPods } = await loadProductContractContext(args.Request, args.contractId);
  for (let i = 0; i < args.matrix.length; i++) {
    const podId = args.pods[i]?.id;
    if (podId == null) throw new Error(`[PDT-2376] Missing Responses.pod[${i}]`);
    const cp = contractPods.find((p) => Number(p.podId) === Number(podId));
    if (!cp?.identifier || cp.podDetailId == null) {
      throw new Error(`[PDT-2376] contractPodsResponses row not found for podId=${podId}`);
    }
    const spec = args.matrix[i];
    const body = {
      identifier: String(cp.identifier),
      podDetailId: Number(cp.podDetailId),
      contractDetailId,
      activationDate: spec.activationDate,
      deactivationDate: spec.deactivationDate,
      deactivationPurposeId: args.deactivationPurposeId,
    };
    const act = await args.Request.post('/contract-pods/manual', { data: body });
    await expect(act).CheckResponse();
  }
}

/**
 * One row from `GET /invoice/detailed-data` (see `InvoiceDetailedDataResponse` in Swagger).
 * Runtime payloads may expose extra keys (for example `priceComponentId`) not present in OpenAPI —
 * callers may rely on {@link isElectricityInvoiceDetailedRow} name + optional id fallback.
 */
export type InvoiceDetailedDataRow = {
  priceComponent?: string | null;
  /** Optional at runtime — not in Swagger `InvoiceDetailedDataResponse`; used when backend sends id. */
  priceComponentId?: number | string | null;
  pointOfDelivery?: string | null;
  periodFrom?: string | null;
  periodTo?: string | null;
  /** Stamp when merging multi-invoice aggregates (not from API). */
  invoiceId?: number;
};

const MAX_MATCHED_ROWS_IN_ATTACHMENT = 8;

/** Max `matchedRows` length in detailed-data electricity attachments ({@link summarizeMatchedRowsBrief}). */
export const PDT_2376_ELECTRICITY_MATCH_ROW_CAP = MAX_MATCHED_ROWS_IN_ATTACHMENT;

/** POST `/price-component(s)` bodies often return `{ id }` but some environments return a bare numeric id (see `asPriceComponentId` in `pdt-2599-service-contract.fixtures.ts`). */
function tryParsePriceComponentStoredId(stored: unknown): number | undefined {
  if (typeof stored === 'number' && Number.isFinite(stored)) return stored;
  if (stored !== null && typeof stored === 'object' && 'id' in stored) {
    const n = Number((stored as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  return undefined;
}

/** Collect human-readable strings from a price-component POST response, GET `PriceComponentDetailedResponse`, or compatible JSON. */
function priceComponentCreationLabelCandidates(stored: unknown): string[] {
  if (stored === null || typeof stored !== 'object') return [];
  const o = stored as Record<string, unknown>;
  const push = (v: unknown, acc: string[]): void => {
    if (typeof v === 'string' && v.trim()) acc.push(v.trim());
  };

  const out: string[] = [];
  for (const k of ['displayName', 'name', 'invoiceAndTemplateText'] as const) {
    push(o[k], out);
  }

  const harvestAm = (am: unknown): void => {
    if (am === null || typeof am !== 'object') return;
    const mr = am as Record<string, unknown>;
    for (const k of ['applicationModelType', 'applicationType'] as const) {
      push(mr[k], out);
    }
  };

  harvestAm(o.applicationModelRequest);
  harvestAm(o.applicationModelResponse);

  return out;
}

/**
 * POST `/price-component(s)` often returns only an id. Load `GET /price-components/{id}` when we have no label
 * hints so `invoice/detailed-data` rows (which echo `name` / template text / application type text) can match.
 */
export async function hydratePdt2376ElectricityMatchContextIfBareId(
  Request: FixtureRequest,
  ctx: Pdt2376ElectricityMatchContext,
): Promise<Pdt2376ElectricityMatchContext> {
  if (ctx.labelCandidatesFromCreationResponse.length > 0) return ctx;
  const id = ctx.electricityPriceComponentId;
  if (id == null) return ctx;

  const res = await Request.get(`price-components/${id}`);
  await expect(res).CheckResponse();
  const extra = priceComponentCreationLabelCandidates(await res.json());

  return {
    electricityPriceComponentId: ctx.electricityPriceComponentId,
    labelCandidatesFromCreationResponse: extra.length > 0 ? extra : [],
  };
}

/**
 * Correlate invoice `detailed-data` rows with the electricity PC created for PDT-2376.
 * Electricity payloads often use random `name`/`displayName`, so matchers must reuse those strings
 * (plus `priceComponentId` when the APIs echo ids).
 */
export type Pdt2376ElectricityMatchContext = {
  electricityPriceComponentId?: number;
  labelCandidatesFromCreationResponse: readonly string[];
};

export function buildPdt2376ElectricityMatchContext(
  /** Typically `Responses.priceComponent` after volume (0) + electricity (1). */
  priceComponentResponses: unknown[],
): Pdt2376ElectricityMatchContext {
  const stored = priceComponentResponses[1];
  const id = tryParsePriceComponentStoredId(stored);
  return {
    electricityPriceComponentId: id,
    labelCandidatesFromCreationResponse: priceComponentCreationLabelCandidates(stored),
  };
}

/** Collapse separators so `EL.DKONC.INV` and `EL_DKONC_INV` compare consistently. */
function collapsePriceComponentName(s: string): string {
  return s.toUpperCase().replace(/[\s._\-]+/g, '');
}

/**
 * Application-model enum strings reused as label hints must be **exact** on the invoice line (loose
 * `includes('WITH_ELECTRICITY_INVOICE')` matches boilerplate on volume rows). Random `name` /
 * `displayName` / template text may still substring-match (`includes`) for truncated invoice wording.
 */
const ELECTRICITY_LABEL_EXACT_ONLY_UPPER = new Set<string>([
  'WITH_ELECTRICITY_INVOICE',
  'PRICE_AM_OVERTIME',
  'BY_SCALES',
  'BY_SETTLEMENT_PERIODS',
  'PERIODICALLY',
  'ONE_TIME',
]);

function priceComponentMatchesCreationLabel(priceComponentRaw: string, candidate: string): boolean {
  const raw = priceComponentRaw.trim();
  const c = candidate.trim();
  if (!raw || !c) return false;
  if (raw === c) return true;

  const cu = c.toUpperCase();
  if (ELECTRICITY_LABEL_EXACT_ONLY_UPPER.has(cu)) {
    return raw.toUpperCase() === cu;
  }

  return raw.toUpperCase().includes(c.toUpperCase());
}

/**
 * True when the detailed row is treated as the electricity / WITH_ELECTRICITY price line.
 * Matches (in order): `priceComponentId` when present; POST-response `name`/`displayName` text;
 * then generic UI/BE names (`WITH_ELECTRICITY`, `ELECTRICITY`, `EL.DKONC.INV`, …).
 */
export function isElectricityInvoiceDetailedRow(
  row: InvoiceDetailedDataRow,
  ctx?: Pdt2376ElectricityMatchContext | null,
): boolean {
  const electricityPriceComponentId = ctx?.electricityPriceComponentId;
  const labelCandidates = ctx?.labelCandidatesFromCreationResponse ?? [];

  const idRaw = row.priceComponentId;
  if (
    electricityPriceComponentId != null &&
    idRaw != null &&
    Number.isFinite(Number(idRaw)) &&
    Number.isFinite(Number(electricityPriceComponentId)) &&
    Number(idRaw) === Number(electricityPriceComponentId)
  ) {
    return true;
  }

  const rawInner = (row.priceComponent ?? '').trim();
  if (rawInner && labelCandidates.length > 0) {
    for (const c of labelCandidates) {
      if (!c) continue;
      if (priceComponentMatchesCreationLabel(rawInner, c)) return true;
    }
  }

  const raw = rawInner;
  if (!raw) return false;

  const u = raw.toUpperCase();
  const collapsed = collapsePriceComponentName(raw);

  if (u.includes('WITH_ELECTRICITY')) return true;
  if (collapsed.includes('EL') && collapsed.includes('DKONC')) return true;
  if (collapsed.includes('ELDKONCINV')) return true;
  if (u.includes('DKONC') && u.includes('INV')) return true;

  return false;
}

export function podIdentifierMatchesDetailedRow(
  pointOfDelivery: string | null | undefined,
  expectedPodIdentifier: string,
): boolean {
  if (pointOfDelivery == null) return false;
  return String(pointOfDelivery).trim() === String(expectedPodIdentifier).trim();
}

export function getMatchedElectricityRowsForPod(
  aggregatedRows: InvoiceDetailedDataRow[],
  podIdentifier: string,
  electricityMatchContext?: Pdt2376ElectricityMatchContext | null,
): InvoiceDetailedDataRow[] {
  return getMatchedElectricityRowsForPodCandidates(aggregatedRows, [podIdentifier], electricityMatchContext);
}

/** Use when `pointOfDelivery` may mirror either the POD display identifier or the numeric `podId`. */
export function getMatchedElectricityRowsForPodCandidates(
  aggregatedRows: InvoiceDetailedDataRow[],
  candidatePodIdentifiers: readonly string[],
  electricityMatchContext?: Pdt2376ElectricityMatchContext | null,
): InvoiceDetailedDataRow[] {
  const cleaned = candidatePodIdentifiers.map((s) => String(s).trim()).filter((s) => s.length > 0);
  return aggregatedRows.filter(
    (r) =>
      cleaned.some((c) => podIdentifierMatchesDetailedRow(r.pointOfDelivery, c)) &&
      isElectricityInvoiceDetailedRow(r, electricityMatchContext),
  );
}

export type Pdt2376PodElectricityDetailedAttachmentEntry = {
  podIdentifier: string;
  expected: boolean;
  actual: boolean;
  matchedRows: Array<{
    priceComponent?: string | null;
    priceComponentId?: number | string | null;
    periodFrom?: string | null;
    periodTo?: string | null;
    invoiceId?: number;
  }>;
  periodFrom: string;
  periodTo: string;
  matchedRowsTruncated?: boolean;
};

export function summarizeMatchedRowsBrief(
  rows: InvoiceDetailedDataRow[],
): Pdt2376PodElectricityDetailedAttachmentEntry['matchedRows'] {
  const slice = rows.slice(0, MAX_MATCHED_ROWS_IN_ATTACHMENT);
  return slice.map((r) => ({
    priceComponent: r.priceComponent,
    priceComponentId: r.priceComponentId,
    periodFrom: r.periodFrom,
    periodTo: r.periodTo,
    invoiceId: r.invoiceId,
  }));
}

/**
 * Resolved period window for attachment: electricity row min/max when present, otherwise billing-anchor fallback.
 */
export function resolveAttachmentPeriodBounds(
  matched: InvoiceDetailedDataRow[],
  fallbackFrom: string,
  fallbackTo: string,
): { periodFrom: string; periodTo: string } {
  if (matched.length === 0) {
    return { periodFrom: fallbackFrom, periodTo: fallbackTo };
  }
  const fromDates = matched.map((r) => r.periodFrom).filter(Boolean) as string[];
  const toDates = matched.map((r) => r.periodTo).filter(Boolean) as string[];
  if (fromDates.length === 0 || toDates.length === 0) {
    return { periodFrom: fallbackFrom, periodTo: fallbackTo };
  }
  return {
    periodFrom: fromDates.sort()[0],
    periodTo: toDates.sort()[toDates.length - 1],
  };
}

export async function fetchAllInvoiceDetailedRows(
  Request: FixtureRequest,
  invoiceId: number,
): Promise<InvoiceDetailedDataRow[]> {
  const size = 100;
  const out: InvoiceDetailedDataRow[] = [];
  for (let page = 0; page < 50; page++) {
    const res = await Request.get(`invoice/detailed-data?id=${invoiceId}&page=${page}&size=${size}`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as {
      content?: InvoiceDetailedDataRow[];
      last?: boolean;
    };
    const chunk = body.content ?? [];
    out.push(...chunk);
    if (body.last === true) break;
    if (chunk.length === 0) break;
    if (chunk.length < size) break;
  }
  return out;
}

/** All detailed-data rows for many draft invoices, each row stamped with `invoiceId`. */
export async function fetchAggregatedInvoiceDetailedData(
  Request: FixtureRequest,
  invoiceIds: number[],
): Promise<InvoiceDetailedDataRow[]> {
  const out: InvoiceDetailedDataRow[] = [];
  for (const id of invoiceIds) {
    const rows = await fetchAllInvoiceDetailedRows(Request, id);
    for (const r of rows) {
      out.push({ ...r, invoiceId: id });
    }
  }
  return out;
}

/**
 * For PDT-2376 the electricity PC is created immediately after the volume PC
 * (`GeneratePayload.productAndServices.electricity()`), so index `1` is stable.
 *
 * Prefer {@link buildPdt2376ElectricityMatchContext}; this helper exposes only the numeric id slice.
 */
export function resolvePdt2376ElectricityPriceComponentId(
  priceComponentResponses: unknown[],
): number | undefined {
  return tryParsePriceComponentStoredId(priceComponentResponses[1]);
}

/** Union of POD identifiers (invoice detailed `pointOfDelivery`) that have an electricity line across all invoices. */
export async function resolveWithElectricityPodIdentifiersFromInvoices(
  Request: FixtureRequest,
  invoiceIds: number[],
  electricityMatchContext?: Pdt2376ElectricityMatchContext | null,
): Promise<Set<string>> {
  const set = new Set<string>();
  const agg = await fetchAggregatedInvoiceDetailedData(Request, invoiceIds);
  for (const row of agg) {
    if (!isElectricityInvoiceDetailedRow(row, electricityMatchContext)) continue;
    const pod = row.pointOfDelivery?.trim();
    if (pod) set.add(pod);
  }
  return set;
}

/** Re-fetch draftinvoice ids after billing completes ( Responses.invoice is filled by waitForInvoiceGeneration ). */
export async function listInvoiceIdsForBillingRun(Request: FixtureRequest, billingRunId: number): Promise<number[]> {
  const drafts = await Request.get(`billing-run/draft-invoices?id=${billingRunId}&page=0&size=50`);
  await expect(drafts).CheckResponse();
  const dj = (await drafts.json()) as { content?: Array<{ id?: number }> };
  return (dj.content ?? []).map((x) => Number(x.id)).filter((n) => Number.isFinite(n) && n > 0);
}

export function normalizeBillingRunId(raw: unknown): number {
  return asBillingRunId(raw);
}

/**
 * Nomenclature search prompt for the profile reused everywhere in PDT-2376 volume + electricity flows.
 * Must stay aligned with `Nomenclatures.profiles(prompt)` lookup — same DB row as settlement periods.
 */
export const PDT_2376_PROFILE_NOMENCLATURE_PROMPT = 'Gio';

/**
 * Single profile id for BY_SETTLEMENT_PERIODS (volume PC) and POST `billing-by-profile`.
 *
 * **Previous mismatch:** Volume PC forced `settlementPeriodsRequest.profiles[0].profileId` via
 * `Nomenclatures.profiles('Gio')`, while `GeneratePayload.energyData.profile1Month()` kept
 * `profileId` from the static `profile1Month()` payload default (`envVariables.profiles`). Those two IDs could differ.
 *
 * **Now:** Call this once, assign the return value to volume settlement `profiles[0].profileId`, and
 * pass it into `alignPdt2376BillingByProfilePayloadProfileId` for each billing-by-profile row.
 */
export async function resolvePdt2376SharedProfileId(args: {
  Nomenclatures: { profiles: (prompt?: string) => Promise<number> };
}): Promise<number> {
  return args.Nomenclatures.profiles(PDT_2376_PROFILE_NOMENCLATURE_PROMPT);
}

/** Mutates billing-by-profile payload so `profileId` matches volume settlement (see `resolvePdt2376SharedProfileId`). */
export function alignPdt2376BillingByProfilePayloadProfileId(
  payload: { profileId: string | number },
  profileId: number,
): void {
  payload.profileId = profileId;
}
