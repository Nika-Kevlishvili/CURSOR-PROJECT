/**
 * PDT-3072 — volume-change INVOICE_CORRECTION with scale PCs (ZDISTJ/ZDISAH).
 *
 * Jira Acceptance Criteria covered by this suite:
 * - AC1 (+ AC2 same fix): Debit SCALE lines split by PC-group version dates like the main
 *   invoice; volumes/values per component sum to the correct period total (NOT ×2).
 *
 * TC-BE-1 (AC1+AC2): mid-period PC group version with **new clone** scale PC entities
 * (new ids, same ZDISTJ/ZDISAH labels) — do not reuse v1 priceComponentIds.
 * (AC3 / negative both-flags-false helpers dropped with those tests.)
 *
 * Dev-automatable (current calendar month); not PreProd-date locked.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts (correctionBilling, draft poll)
 * - tests/billing/forVolumes/forVolumes.spec.ts (scale POD + meter + billing-by-scales)
 * - tests/billing/correction/correctionCases.spec.ts (INVOICE_CORRECTION chain)
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts (invoice/detailed-data rows)
 *
 * Swagger (dev, refreshed): InvoiceCorrectionParameters (priceChange, volumeChange, listOfInvoices),
 * BillingByScalesCreateRequest (correction, invoiceCorrection, invoiceNumber),
 * PUT price-component-groups/{id}, GET invoice/detailed-data, GET billing-run/draft-invoices.
 */

import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  asPriceComponentId,
  type Pdt2599InvoiceDetailedDataRow,
} from './pdt-2599-service-contract.fixtures';
import {
  BILLING_RUN_ROOT,
} from './pdt-2915-invoice-correction-deleted-bbp.fixtures';
import { scales as scalesPayloadTemplate } from '../../jsons/payloads/create/energyData/scales';

export const PDT_3072_GRID_OPERATOR = 'GIO';
export const PDT_3072_PC_ZDISTJ_LABEL = 'PDT-3072-ZDISTJ';
export const PDT_3072_PC_ZDISAH_LABEL = 'PDT-3072-ZDISAH';
export const PDT_3072_PC_GROUP_NAME = 'PDT-3072-PC-Group';
export const PDT_3072_SCALE_PC_LABELS = [PDT_3072_PC_ZDISTJ_LABEL, PDT_3072_PC_ZDISAH_LABEL] as const;
/** Dev portal base for manual preview links (no trailing slash). */
export const PDT_3072_PORTAL_BASE = 'https://devapps.energo-pro.bg/app/phoenix1-dev';

const DRAFT_POLL_MS = 15_000;
const DRAFT_MAX_MS = 10 * 60 * 1000;

export type Pdt3072Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'Nomenclatures'
>;

export type Pdt3072PeriodWindow = {
  periodFrom: string;
  periodTo: string;
  versionBoundary: string;
};

export type Pdt3072ScenarioContext = {
  period: Pdt3072PeriodWindow;
  podIdentifiers: string[];
  /** Index of the POD that receives scale volume correction (one POD on debit). */
  correctedPodIndex: number;
  contractId: number;
  groupId: number;
  /** PC group scale PC ids used before any mid-period clone version. */
  priceComponentIds: number[];
  /**
   * AC1/AC2: mid-period clone PC ids (distinct from v1).
   */
  priceComponentIdsV2: number[];
  /** True when a mid-period PC group version with clone PCs was created (AC1/AC2). */
  hasMidPeriodPcGroupVersion: boolean;
  originalInvoiceId: number;
  originalInvoiceNumber: string;
};

export type Pdt3072ComponentVolumeTotals = {
  label: string;
  volumeSum: number;
  valueSum: number;
  rowCount: number;
  maxIdenticalPeriodCount: number;
  uniquePeriodVolumeSum: number;
  uniquePeriodValueSum: number;
};

export type Pdt3072DetailedLineSummary = {
  priceComponent?: string;
  pointOfDelivery?: string;
  periodFrom?: string;
  periodTo?: string;
  value?: number;
  totalVolumes?: number;
};

export type Pdt3072ScalePeriodAnalysis = {
  splitRespected: boolean;
  hasDuplicateFullPeriodLines: boolean;
  zdistjFullPeriodRowCount: number;
  zdisahFullPeriodRowCount: number;
  zdistjRowCount: number;
  zdisahRowCount: number;
  periodsByLabel: Record<string, string[]>;
  rows: Pdt3072DetailedLineSummary[];
};

export type Pdt3072CorrectionDraftPoll = {
  correctionBillingRunId: number;
  billingRunSnapshot: Record<string, unknown>;
  draftInvoices: Array<{
    id: number;
    invoiceDocumentType?: string;
    invoiceNumber?: string;
  }>;
  debitNoteInvoiceId: number;
  creditNoteInvoiceId: number | null;
};

/**
 * Billing window in the **current** calendar month (Dev AP / FOR_VOLUMES stay on this month).
 * Dev constraint: CreatePriceComponentGroup starts v1 at LocalDate.now().
 * - Prefer mid-period clone startDate **after today** (unique vs v1).
 * - On the last day of the month (no future day left), place the clone startDate in the
 *   **past** half of the month (unique vs month-start coverage + vs v1@today).
 */
export function computePdt3072PeriodWindow(reference = new Date()): Pdt3072PeriodWindow {
  const year = reference.getUTCFullYear();
  const monthIndex = reference.getUTCMonth();
  const pad = (n: number) => String(n).padStart(2, '0');
  const dayMs = 24 * 60 * 60 * 1000;
  const month1 = monthIndex + 1;
  const lastDay = new Date(Date.UTC(year, monthIndex + 1, 0)).getUTCDate();
  const periodFrom = `${year}-${pad(month1)}-01`;
  const periodTo = `${year}-${pad(month1)}-${pad(lastDay)}`;
  const today = `${year}-${pad(month1)}-${pad(reference.getUTCDate())}`;

  const midDate = (from: string, to: string): string => {
    const a = Date.parse(`${from}T00:00:00.000Z`);
    const b = Date.parse(`${to}T00:00:00.000Z`);
    const spanDays = Math.round((b - a) / dayMs);
    const midOffset = Math.max(1, Math.floor(spanDays / 2));
    let mid = addDays(from, midOffset);
    if (mid > to) mid = to;
    return mid;
  };

  let versionBoundary: string;
  if (addDays(today, 1) <= periodTo) {
    // Enough days left after today → split create-day → monthEnd (preferred for SCALE AC).
    versionBoundary = midDate(addDays(today, 1), periodTo);
  } else if (addDays(periodFrom, 1) < today) {
    // Last day of month: place clone version in the past (after periodFrom, before today).
    versionBoundary = midDate(periodFrom, addDays(today, -1));
    if (versionBoundary === periodFrom) versionBoundary = addDays(periodFrom, 1);
    if (versionBoundary >= today) versionBoundary = addDays(today, -1);
  } else {
    // Degenerate (1st == last): fall back to periodTo; caller may skip colliding PUTs.
    versionBoundary = periodTo;
  }

  if (versionBoundary === today && today > periodFrom) {
    versionBoundary = addDays(today, -1);
  }

  return { periodFrom, periodTo, versionBoundary };
}

export function dateOnly(value: unknown): string {
  return String(value ?? '').slice(0, 10);
}

export function addDays(isoDate: string, days: number): string {
  const d = new Date(`${isoDate}T00:00:00.000Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

export function summarizePdt3072DetailedRows(
  rows: Pdt2599InvoiceDetailedDataRow[],
): Pdt3072DetailedLineSummary[] {
  return rows.map((row) => ({
    priceComponent: row.priceComponent,
    // Pdt2599 tabular row index signature → unknown; coerce for summary typing.
    pointOfDelivery:
      row.pointOfDelivery == null ? undefined : String(row.pointOfDelivery),
    periodFrom: dateOnly(row.periodFrom),
    periodTo: dateOnly(row.periodTo),
    value: row.value,
    totalVolumes: row.totalVolumes,
  }));
}

function rowsMatchingPcLabel(
  rows: Pdt2599InvoiceDetailedDataRow[],
  label: string,
): Pdt2599InvoiceDetailedDataRow[] {
  const needle = label.toLowerCase();
  return rows.filter((row) => String(row.priceComponent ?? '').toLowerCase().includes(needle));
}

function fullPeriodRowCount(
  rows: Pdt2599InvoiceDetailedDataRow[],
  label: string,
  period: Pdt3072PeriodWindow,
): number {
  return rowsMatchingPcLabel(rows, label).filter(
    (row) =>
      dateOnly(row.periodFrom) === period.periodFrom && dateOnly(row.periodTo) === period.periodTo,
  ).length;
}

export function extractPdt3072PeriodsByLabel(
  rows: Pdt2599InvoiceDetailedDataRow[],
  labels: readonly string[],
): Record<string, string[]> {
  const periodsByLabel: Record<string, string[]> = {};
  for (const label of labels) {
    const pcRows = rowsMatchingPcLabel(rows, label);
    periodsByLabel[label] = Array.from(
      new Set(
        pcRows.map(
          (r) => `${dateOnly(r.periodFrom)}|${dateOnly(r.periodTo)}`,
        ),
      ),
    ).sort();
  }
  return periodsByLabel;
}

export function filterPdt3072DetailedRowsForPod(
  rows: Pdt2599InvoiceDetailedDataRow[],
  podIdentifier: string,
): Pdt2599InvoiceDetailedDataRow[] {
  const needle = podIdentifier.trim();
  return rows.filter((row) => String(row.pointOfDelivery ?? '').trim() === needle);
}

function identicalPeriodDuplicateCount(
  rows: Pdt2599InvoiceDetailedDataRow[],
  label: string,
): number {
  const keyCounts = new Map<string, number>();
  for (const row of rowsMatchingPcLabel(rows, label)) {
    const key = `${dateOnly(row.periodFrom)}|${dateOnly(row.periodTo)}`;
    keyCounts.set(key, (keyCounts.get(key) ?? 0) + 1);
  }
  let max = 0;
  for (const c of Array.from(keyCounts.values())) max = Math.max(max, c);
  return max;
}

/**
 * AC1 helper: Analyze SCALE period split for ZDISTJ / ZDISAH against a PC group version boundary.
 *
 * Duplicate identical periods (≥2 rows per label with the same from–to) = AS-IS bug:
 * old + new PC ids (same ZDISTJ/ZDISAH label) each bill the same window. Detailed-data exposes
 * name only (Swagger InvoiceDetailedDataResponse.priceComponent), so label aggregation is intentional.
 */
export function analyzePdt3072ScalePeriodSplit(
  rows: Pdt2599InvoiceDetailedDataRow[],
  period: Pdt3072PeriodWindow,
): Pdt3072ScalePeriodAnalysis {
  const zdistjRows = rowsMatchingPcLabel(rows, PDT_3072_PC_ZDISTJ_LABEL);
  const zdisahRows = rowsMatchingPcLabel(rows, PDT_3072_PC_ZDISAH_LABEL);
  const preBoundaryEnd = addDays(period.versionBoundary, -1);

  const zdistjHasBoundarySplit = zdistjRows.some(
    (row) => dateOnly(row.periodTo) === preBoundaryEnd,
  );
  const zdistjHasPostBoundary = zdistjRows.some(
    (row) => dateOnly(row.periodFrom) === period.versionBoundary,
  );
  const zdisahHasBoundarySplit = zdisahRows.some(
    (row) => dateOnly(row.periodTo) === preBoundaryEnd,
  );
  const zdisahHasPostBoundary = zdisahRows.some(
    (row) => dateOnly(row.periodFrom) === period.versionBoundary,
  );

  // Calendar full-month dups (classic bug) OR any identical from–to repeated ≥2 (old+new ids).
  const zdistjFullPeriodRowCount = fullPeriodRowCount(rows, PDT_3072_PC_ZDISTJ_LABEL, period);
  const zdisahFullPeriodRowCount = fullPeriodRowCount(rows, PDT_3072_PC_ZDISAH_LABEL, period);
  const zdistjIdenticalDup = identicalPeriodDuplicateCount(rows, PDT_3072_PC_ZDISTJ_LABEL);
  const zdisahIdenticalDup = identicalPeriodDuplicateCount(rows, PDT_3072_PC_ZDISAH_LABEL);
  const hasDuplicateFullPeriodLines =
    zdistjFullPeriodRowCount >= 2 ||
    zdisahFullPeriodRowCount >= 2 ||
    zdistjIdenticalDup >= 2 ||
    zdisahIdenticalDup >= 2;

  // Accept exact day-before/day-of boundary OR any pre-boundary segment + post-boundary start
  // (create-day PC group versions can add extra cuts inside the month).
  const zdistjTouchesBoundary =
    (zdistjHasBoundarySplit && zdistjHasPostBoundary) ||
    (zdistjRows.length >= 2 &&
      zdistjRows.some((r) => dateOnly(r.periodFrom) === period.versionBoundary) &&
      zdistjRows.some((r) => dateOnly(r.periodTo) < period.versionBoundary));
  const zdisahTouchesBoundary =
    (zdisahHasBoundarySplit && zdisahHasPostBoundary) ||
    (zdisahRows.length >= 2 &&
      zdisahRows.some((r) => dateOnly(r.periodFrom) === period.versionBoundary) &&
      zdisahRows.some((r) => dateOnly(r.periodTo) < period.versionBoundary));

  const splitRespected =
    zdistjRows.length >= 2 &&
    zdisahRows.length >= 2 &&
    zdistjTouchesBoundary &&
    zdisahTouchesBoundary &&
    zdistjFullPeriodRowCount === 0 &&
    zdisahFullPeriodRowCount === 0 &&
    zdistjIdenticalDup < 2 &&
    zdisahIdenticalDup < 2;

  return {
    splitRespected,
    hasDuplicateFullPeriodLines,
    zdistjFullPeriodRowCount,
    zdisahFullPeriodRowCount,
    zdistjRowCount: zdistjRows.length,
    zdisahRowCount: zdisahRows.length,
    periodsByLabel: extractPdt3072PeriodsByLabel(rows, PDT_3072_SCALE_PC_LABELS),
    rows: summarizePdt3072DetailedRows([...zdistjRows, ...zdisahRows]),
  };
}

function nearNumeric(a: number, b: number, relTol = 0.08): boolean {
  return Math.abs(a - b) <= Math.max(1e-6, relTol * Math.max(Math.abs(b), 1));
}

/** Sum totalVolumes / value for one scale component label (AC2). */
export function sumPdt3072ComponentTotals(
  rows: Pdt2599InvoiceDetailedDataRow[],
  label: string,
): Pdt3072ComponentVolumeTotals {
  const pcRows = rowsMatchingPcLabel(rows, label);
  let volumeSum = 0;
  let valueSum = 0;
  const seenPeriods = new Set<string>();
  let uniquePeriodVolumeSum = 0;
  let uniquePeriodValueSum = 0;

  for (const row of pcRows) {
    const vol = Number(row.totalVolumes ?? 0);
    const val = Number(row.value ?? 0);
    volumeSum += Number.isFinite(vol) ? vol : 0;
    valueSum += Number.isFinite(val) ? val : 0;
    const key = `${dateOnly(row.periodFrom)}|${dateOnly(row.periodTo)}`;
    if (!seenPeriods.has(key)) {
      seenPeriods.add(key);
      uniquePeriodVolumeSum += Number.isFinite(vol) ? vol : 0;
      uniquePeriodValueSum += Number.isFinite(val) ? val : 0;
    }
  }

  return {
    label,
    volumeSum,
    valueSum,
    rowCount: pcRows.length,
    maxIdenticalPeriodCount: identicalPeriodDuplicateCount(rows, label),
    uniquePeriodVolumeSum,
    uniquePeriodValueSum,
  };
}

/**
 * AC1 gate: debit SCALE periods must split at versionBoundary like main (not merged full-span
 * on both old+new PC versions).
 */
export function assertPdt3072DebitScalePeriodsMatchMainSplit(
  mainAnalysis: Pdt3072ScalePeriodAnalysis,
  debitAnalysis: Pdt3072ScalePeriodAnalysis,
  period: Pdt3072PeriodWindow,
): void {
  const ac1Msg =
    `AC1: Debit must split SCALE periods at versionBoundary=${period.versionBoundary} like main. ` +
    `Bug (merged dates): debit bills full period ${period.periodFrom}–${period.periodTo} on BOTH ` +
    `old and new PC group versions → duplicates when a split was needed.`;

  expect(
    mainAnalysis.splitRespected,
    `AC1 precondition: main invoice must already split at ${period.versionBoundary}. ` +
      `main=${JSON.stringify(mainAnalysis.periodsByLabel)} rows=${JSON.stringify(mainAnalysis.rows)}`,
  ).toBe(true);

  expect(
    debitAnalysis.hasDuplicateFullPeriodLines,
    `${ac1Msg} Debit duplicate full-period / identical-period detected: ` +
      `ZDISTJ full=${debitAnalysis.zdistjFullPeriodRowCount} ZDISAH full=${debitAnalysis.zdisahFullPeriodRowCount}; ` +
      `debitPeriods=${JSON.stringify(debitAnalysis.periodsByLabel)}; rows=${JSON.stringify(debitAnalysis.rows)}`,
  ).toBe(false);

  expect(
    debitAnalysis.splitRespected,
    `${ac1Msg} Debit must respect version-boundary split. ` +
      `mainPeriods=${JSON.stringify(mainAnalysis.periodsByLabel)} ` +
      `debitPeriods=${JSON.stringify(debitAnalysis.periodsByLabel)} ` +
      `debitRows=${JSON.stringify(debitAnalysis.rows)}`,
  ).toBe(true);
}

/**
 * AC2 gate: per scale component, debit volume/value sums equal the correct period total
 * (unique-period sum — same as main split segments when no intentional volume delta doubles).
 * FAIL if debitSum ≈ 2 × expectedSum or identical period rows ≥2 with doubled volumes.
 *
 * Intentional volume-change uplift may make debit > main; AC2 only forbids ×2 inflation /
 * duplicate identical periods.
 */
export function assertPdt3072Ac2VolumesSumNoDouble(
  mainRows: Pdt2599InvoiceDetailedDataRow[],
  debitRows: Pdt2599InvoiceDetailedDataRow[],
  labels: readonly string[] = PDT_3072_SCALE_PC_LABELS,
): {
  mainByLabel: Record<string, Pdt3072ComponentVolumeTotals>;
  debitByLabel: Record<string, Pdt3072ComponentVolumeTotals>;
} {
  const mainByLabel: Record<string, Pdt3072ComponentVolumeTotals> = {};
  const debitByLabel: Record<string, Pdt3072ComponentVolumeTotals> = {};

  for (const label of labels) {
    const main = sumPdt3072ComponentTotals(mainRows, label);
    const debit = sumPdt3072ComponentTotals(debitRows, label);
    mainByLabel[label] = main;
    debitByLabel[label] = debit;

    expect(
      debit.maxIdenticalPeriodCount,
      `AC2 (${label}): identical period rows ≥2 (doubled volumes on old+new PC versions). ` +
        `debit=${JSON.stringify(debit)}; main=${JSON.stringify(main)}`,
    ).toBeLessThan(2);

    // Unique-period total is the correct period total; full sum must equal it (no double-count).
    expect(
      nearNumeric(debit.volumeSum, debit.uniquePeriodVolumeSum),
      `AC2 (${label}): debit volumeSum=${debit.volumeSum} must equal unique-period total ` +
        `${debit.uniquePeriodVolumeSum} (not ×2). debit=${JSON.stringify(debit)}`,
    ).toBe(true);
    expect(
      nearNumeric(debit.valueSum, debit.uniquePeriodValueSum),
      `AC2 (${label}): debit valueSum=${debit.valueSum} must equal unique-period total ` +
        `${debit.uniquePeriodValueSum} (not ×2). debit=${JSON.stringify(debit)}`,
    ).toBe(true);

    expect(
      nearNumeric(debit.volumeSum, 2 * debit.uniquePeriodVolumeSum),
      `AC2 (${label}): debit volumeSum=${debit.volumeSum} ≈ 2× uniquePeriodVolumeSum=` +
        `${debit.uniquePeriodVolumeSum} (classic ×2 bug). debit=${JSON.stringify(debit)}`,
    ).toBe(false);

    // Classic bug vs main: debit ≈ 2 × main (same volumes billed twice).
    if (main.volumeSum > 0) {
      expect(
        nearNumeric(debit.volumeSum, 2 * main.volumeSum),
        `AC2 (${label}): debit volumeSum=${debit.volumeSum} ≈ 2× main volumeSum=${main.volumeSum}. ` +
          `debit=${JSON.stringify(debit)} main=${JSON.stringify(main)}`,
      ).toBe(false);
    }
    if (main.valueSum > 0) {
      expect(
        nearNumeric(debit.valueSum, 2 * main.valueSum),
        `AC2 (${label}): debit valueSum=${debit.valueSum} ≈ 2× main valueSum=${main.valueSum}. ` +
          `debit=${JSON.stringify(debit)} main=${JSON.stringify(main)}`,
      ).toBe(false);
    }
  }

  return { mainByLabel, debitByLabel };
}

export function buildPdt3072InvoiceDetailedDataPreviewLink(invoiceId: number): string {
  return `${PDT_3072_PORTAL_BASE}/billing-run/invoices/preview/detailed-data?id=${invoiceId}`;
}

export function buildPdt3072BillingRunPreviewLink(billingRunId: number): string {
  return `${PDT_3072_PORTAL_BASE}/billing-run/preview?id=${billingRunId}`;
}

export function buildPdt3072PeriodComparisonTable(
  mainAnalysis: Pdt3072ScalePeriodAnalysis,
  debitAnalysis: Pdt3072ScalePeriodAnalysis,
): Array<{ side: string; label: string; periods: string[]; fullPeriodDup: boolean }> {
  return [
    {
      side: 'main',
      label: PDT_3072_PC_ZDISTJ_LABEL,
      periods: mainAnalysis.periodsByLabel[PDT_3072_PC_ZDISTJ_LABEL] ?? [],
      fullPeriodDup: mainAnalysis.zdistjFullPeriodRowCount >= 2,
    },
    {
      side: 'main',
      label: PDT_3072_PC_ZDISAH_LABEL,
      periods: mainAnalysis.periodsByLabel[PDT_3072_PC_ZDISAH_LABEL] ?? [],
      fullPeriodDup: mainAnalysis.zdisahFullPeriodRowCount >= 2,
    },
    {
      side: 'debit',
      label: PDT_3072_PC_ZDISTJ_LABEL,
      periods: debitAnalysis.periodsByLabel[PDT_3072_PC_ZDISTJ_LABEL] ?? [],
      fullPeriodDup: debitAnalysis.zdistjFullPeriodRowCount >= 2,
    },
    {
      side: 'debit',
      label: PDT_3072_PC_ZDISAH_LABEL,
      periods: debitAnalysis.periodsByLabel[PDT_3072_PC_ZDISAH_LABEL] ?? [],
      fullPeriodDup: debitAnalysis.zdisahFullPeriodRowCount >= 2,
    },
  ];
}

async function createPdt3072ScalePriceComponent(
  ctx: Pdt3072Fx,
  label: string,
  scaleId: number,
  expression: string,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = GeneratePayload.productAndServices.scaleComponent();
  payload.name = label;
  payload.displayName = label;
  payload.formulaRequest.expression = expression;
  payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
  payload.applicationModelRequest.volumesByScaleRequest.scaleIds = [scaleId];

  const res = await Request.post(Endpoints.priceComponent, { data: payload });
  await expect(res).CheckResponse();
  const id = asPriceComponentId(await res.json());
  Responses.priceComponent.push(id);
  return id;
}

async function createPdt3072ScalePodWithMeter(ctx: Pdt3072Fx, podIndex: number): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = ctx;
  const gridId = await Nomenclatures.grid_operator(PDT_3072_GRID_OPERATOR);
  const measurementTypeId = await Nomenclatures.measurement_type(PDT_3072_GRID_OPERATOR, gridId);
  const scaleCodeId = await Nomenclatures.scales_code('scaleCode');
  const scaleTariffId = await Nomenclatures.scales_tariff(PDT_3072_GRID_OPERATOR);

  const podPayload = GeneratePayload.pointsOfDelivery.pod_slp();
  podPayload.gridOperatorId = gridId;
  podPayload.measurementTypeId = measurementTypeId;
  const podRes = await Request.post(Endpoints.pod, { data: podPayload });
  await expect(podRes).CheckResponse();
  // Swagger PodResponse (create): id + podDetailId + versionId only — no gridOperatorId/identifier.
  // Do NOT GET /pod/{id}: this token returns 403 ACCESS_DENIED (missing POD_VIEW_*).
  const podCreated = (await podRes.json()) as { id?: number; podDetailId?: number };
  const podId = Number(podCreated.id);
  expect(podId, 'POD create must return numeric id').toBeGreaterThan(0);
  const identifier = String(podPayload.identifier ?? '').trim();
  expect(identifier, 'pod_slp payload must include identifier').toBeTruthy();
  Responses.pod.push({
    ...podCreated,
    id: podId,
    identifier,
    gridOperatorId: gridId,
    estimatedMonthlyAvgConsumption: Number(podPayload.estimatedMonthlyAvgConsumption ?? 0),
    type: podPayload.type ?? 'CONSUMER',
  } as (typeof Responses.pod)[number]);

  const meterPayload = GeneratePayload.pointsOfDelivery.meters() as unknown as {
    gridOperatorId: number | null;
    podId: number | null;
    meterScales: number[];
  };
  meterPayload.gridOperatorId = gridId;
  meterPayload.podId = podId;
  meterPayload.meterScales = [scaleCodeId, scaleTariffId];
  const meterRes = await Request.post(Endpoints.meters, { data: meterPayload });
  await expect(meterRes).CheckResponse();
  Responses.meters.push(await meterRes.json());
}

type Pdt3072ScaleBillingOptions = {
  correction?: boolean;
  invoiceNumber?: string;
  invoiceDate?: string;
  /** Higher readings for correction delta on the target POD (settlement volume change). */
  readingBoost?: number;
};

async function buildPdt3072ScaleTariffPayload(
  ctx: Pdt3072Fx,
  podIndex: number,
  period: Pdt3072PeriodWindow,
  options: Pdt3072ScaleBillingOptions = {},
): Promise<Record<string, unknown>> {
  const { Request, Responses } = ctx;
  const meterId = Responses.meters[podIndex] as number;
  // Swagger GET /meters/{id} has no version query; do not pass versionId.
  const meterGet = await Request.get(`meters/${meterId}`);
  await expect(meterGet).CheckResponse();
  const meterJson = (await meterGet.json()) as {
    number?: string;
    meterScales?: Array<{ id?: number }>;
  };

  // Meter GET may reorder meterScales vs POST order — classify by fields, not index.
  type ScaleJson = {
    id?: number;
    scaleType?: string | null;
    scaleCode?: string | null;
    tariffOrScale?: string | null;
  };
  const scaleDetails: ScaleJson[] = [];
  for (const meterScale of meterJson.meterScales ?? []) {
    const scaleId = meterScale?.id;
    if (scaleId == null) continue;
    const scaleGet = await Request.get(`scales/${scaleId}`);
    await expect(scaleGet, `PDT-3072 GET scales/${scaleId}`).CheckResponse();
    scaleDetails.push((await scaleGet.json()) as ScaleJson);
  }
  const scaleTariffJson =
    scaleDetails.find((s) => Boolean(s.tariffOrScale) && !s.scaleCode) ??
    scaleDetails.find((s) => Boolean(s.tariffOrScale));
  const scaleCodeJson =
    scaleDetails.find((s) => Boolean(s.scaleCode) && !s.tariffOrScale) ??
    scaleDetails.find((s) => Boolean(s.scaleCode));
  if (!scaleTariffJson?.tariffOrScale) {
    throw new Error(
      `PDT-3072: meter ${meterId} has no tariff scale with tariffOrScale ` +
        `(scales=${JSON.stringify(scaleDetails)})`,
    );
  }
  if (!scaleCodeJson?.scaleCode) {
    throw new Error(
      `PDT-3072: meter ${meterId} has no code scale with scaleCode ` +
        `(scales=${JSON.stringify(scaleDetails)})`,
    );
  }

  const podEntry = Responses.pod[podIndex] as { id?: number; identifier?: string };
  const identifier = String(podEntry.identifier ?? '').trim();
  expect(
    identifier,
    `PDT-3072: Responses.pod[${podIndex}] must carry identifier from create (POD GET is 403)`,
  ).toBeTruthy();

  const preBoundaryEnd = addDays(period.versionBoundary, -1);
  const boost = options.readingBoost ?? 0;
  const payload = scalesPayloadTemplate() as Record<string, unknown>;
  payload.identifier = identifier;
  payload.dateFrom = period.periodFrom;
  payload.dateTo = period.periodTo;
  payload.invoiceDate = `${period.periodFrom}T00:00:00.000Z`;
  payload.invoiceNumber = options.invoiceNumber ?? '1';
  payload.correction = options.correction ?? false;
  // Swagger: invoiceCorrection is a free string; volume-change path (not PRICE_CHANGE).
  payload.invoiceCorrection = options.correction ? 'VOLUME_CHANGE' : null;

  const rows = payload.billingByScalesTableCreateRequests as Array<Record<string, unknown>>;
  // Settlement / tariff scale row — boost models settlement volume change (1180→1300 shape).
  rows[0].periodFrom = period.periodFrom;
  rows[0].periodTo = period.periodTo;
  rows[0].meterNumber = meterJson.number;
  rows[0].scaleType = scaleTariffJson.scaleType;
  rows[0].tariffScale = scaleTariffJson.tariffOrScale;
  rows[0].volumes = String(1180 + boost);

  rows[1].periodFrom = period.periodFrom;
  rows[1].periodTo = preBoundaryEnd;
  rows[1].meterNumber = meterJson.number;
  rows[1].scaleCode = scaleCodeJson.scaleCode;
  rows[1].scaleNumber = randomGens.getRandomEven().toString();
  rows[1].scaleType = scaleCodeJson.scaleType;
  rows[1].newMeterReading = String(20 + boost);
  rows[1].oldMeterReading = '10';
  rows[1].difference = String(10 + boost);
  rows[1].multiplier = '2';
  rows[1].totalVolumes = String(20 + 2 * boost);

  rows[2].periodFrom = period.versionBoundary;
  rows[2].periodTo = period.periodTo;
  rows[2].meterNumber = meterJson.number;
  rows[2].scaleCode = scaleCodeJson.scaleCode;
  rows[2].scaleNumber = randomGens.getRandomEven().toString();
  rows[2].scaleType = scaleCodeJson.scaleType;
  rows[2].newMeterReading = String(30 + boost);
  rows[2].oldMeterReading = String(20 + boost);
  rows[2].difference = '10';
  rows[2].multiplier = '2';
  rows[2].totalVolumes = '20';

  return payload;
}

async function postPdt3072BillingByScales(
  ctx: Pdt3072Fx,
  payload: Record<string, unknown>,
  label: string,
): Promise<number> {
  const { Request, Responses, Endpoints } = ctx;
  const res = await Request.post(Endpoints.dataByScales, { data: payload });
  await expect(res, label).CheckResponse();
  const scaleId = Number(await res.json());
  Responses.dataByScales.push(scaleId);
  return scaleId;
}

async function runPdt3072PodScaleBeforeBilling(
  ctx: Pdt3072Fx,
  podIndex: number,
  period: Pdt3072PeriodWindow,
): Promise<void> {
  const payload = await buildPdt3072ScaleTariffPayload(ctx, podIndex, period, {
    correction: false,
  });
  await postPdt3072BillingByScales(ctx, payload, `PDT-3072 POD${podIndex} scale before billing`);
}

/**
 * Scale volume correction on a single POD after main invoice realize.
 * Swagger: BillingByScalesCreateRequest.correction=true + invoiceNumber of realized invoice.
 */
export async function runPdt3072SinglePodScaleVolumeCorrectionAfterInvoice(
  ctx: Pdt3072Fx,
  podIndex: number,
  originalInvoiceId: number,
  period: Pdt3072PeriodWindow,
): Promise<number> {
  const { Request } = ctx;
  const invoiceRes = await Request.get(`invoice?id=${originalInvoiceId}`);
  await expect(invoiceRes).CheckResponse();
  const invoiceBody = (await invoiceRes.json()) as { invoiceNumber?: string; invoiceDate?: string };
  const invoiceNumber = String(invoiceBody.invoiceNumber ?? '');
  expect(invoiceNumber, 'realized invoice number for scale volume correction').not.toBe('');

  const payload = await buildPdt3072ScaleTariffPayload(ctx, podIndex, period, {
    correction: true,
    invoiceNumber,
    invoiceDate: invoiceBody.invoiceDate,
    // +120 ≈ settlement volume delta shape (1180→1300)
    readingBoost: 120,
  });
  return postPdt3072BillingByScales(
    ctx,
    payload,
    `PDT-3072 POD${podIndex} scale VOLUME_CHANGE after invoice`,
  );
}

async function createPdt3072PriceComponentGroup(
  ctx: Pdt3072Fx,
  priceComponentIds: number[],
): Promise<number> {
  const { Request, Responses, Endpoints } = ctx;
  const groupPayload = {
    name: PDT_3072_PC_GROUP_NAME,
    priceComponentsList: priceComponentIds.map((priceComponentId) => ({ priceComponentId })),
  };
  const res = await Request.post(Endpoints.groupOfPriceComponents, { data: groupPayload });
  await expect(res).CheckResponse();
  const groupId = asPriceComponentId(await res.json());
  Responses.groupOfPriceComponents.push({ id: groupId });
  return groupId;
}

async function loadPcGroupVersionId(Request: Pdt3072Fx['Request'], groupId: number): Promise<number> {
  const res = await Request.get(`price-component-groups/${groupId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as { versionId?: number; version?: number };
  const versionId = Number(body.versionId ?? body.version ?? 1);
  expect(versionId, 'PC group versionId').toBeGreaterThan(0);
  return versionId;
}

/**
 * CreatePriceComponentGroup starts v1 at LocalDate.now(). Add a NEW version at periodFrom
 * (updateExistingVersion=false) so the billed month is covered — cannot change v1 startDate in-place.
 */
async function createPdt3072PcGroupMonthStartCoverageVersion(
  Request: Pdt3072Fx['Request'],
  groupId: number,
  priceComponentIds: number[],
  periodFrom: string,
): Promise<number> {
  return createPdt3072PcGroupNewVersion(Request, groupId, priceComponentIds, periodFrom);
}

async function createPdt3072PcGroupNewVersion(
  Request: Pdt3072Fx['Request'],
  groupId: number,
  priceComponentIdsForNewVersion: number[],
  versionBoundary: string,
): Promise<number> {
  const versionId = await loadPcGroupVersionId(Request, groupId);
  const putRes = await Request.put(`price-component-groups/${groupId}`, {
    data: {
      name: PDT_3072_PC_GROUP_NAME,
      versionId,
      updateExistingVersion: false,
      startDate: versionBoundary,
      // Must be **new** PC entity ids (not the v1 list) so main/debit can show old+new like PreProd.
      priceComponentsList: priceComponentIdsForNewVersion.map((priceComponentId) => ({
        priceComponentId,
      })),
    },
  });
  await expect(putRes).CheckResponse();
  const newVersionId = Number(await putRes.json());
  expect(newVersionId, 'new PC group version id').toBeGreaterThan(0);
  return newVersionId;
}

/**
 * Clone scale PCs for PC group v2 — new ids, same ZDISTJ/ZDISAH labels (PreProd old+new pattern).
 */
async function createPdt3072CloneScalePcsForNewVersion(
  ctx: Pdt3072Fx,
  scaleTariffId: number,
  scaleCodeId: number,
): Promise<number[]> {
  const pcZdistjV2Id = await createPdt3072ScalePriceComponent(
    ctx,
    PDT_3072_PC_ZDISTJ_LABEL,
    scaleTariffId,
    '100',
  );
  const pcZdisahV2Id = await createPdt3072ScalePriceComponent(
    ctx,
    PDT_3072_PC_ZDISAH_LABEL,
    scaleCodeId,
    '200',
  );
  return [pcZdistjV2Id, pcZdisahV2Id];
}

export type Pdt3072PrechainOptions = {
  /**
   * When set, product.INVOICE_TEMPLATE uses this id (custom uploaded DOCUMENT template)
   * instead of envVariables.invoice_document_template. Used by PDT-3144 repro.
   */
  invoiceTemplateId?: number;
};

/** TC-BE-1 (AC1+AC2) prechain: 2 scale PODs + PC group mid-period clone version + scale energy data. */
export async function runPdt3072VolumeChangeScenarioPrechain(
  ctx: Pdt3072Fx,
  options: Pdt3072PrechainOptions = {},
): Promise<Pdt3072ScenarioContext> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = ctx;
  const period = computePdt3072PeriodWindow();
  const correctedPodIndex = 0;

  const customer = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(customer).CheckResponse();
  Responses.customer.push(await customer.json());

  await Nomenclatures.grid_operator(PDT_3072_GRID_OPERATOR);
  const scaleTariffId = await Nomenclatures.scales_tariff(PDT_3072_GRID_OPERATOR);
  const scaleCodeId = await Nomenclatures.scales_code('scaleCode');

  const pcZdistjId = await createPdt3072ScalePriceComponent(
    ctx,
    PDT_3072_PC_ZDISTJ_LABEL,
    scaleTariffId,
    '100',
  );
  const pcZdisahId = await createPdt3072ScalePriceComponent(
    ctx,
    PDT_3072_PC_ZDISAH_LABEL,
    scaleCodeId,
    '200',
  );
  const priceComponentIds = [pcZdistjId, pcZdisahId];

  const groupId = await createPdt3072PriceComponentGroup(ctx, priceComponentIds);
  // Cover month-start → create-day when periodFrom is not already v1's startDate (today).
  const todayIso = new Date().toISOString().slice(0, 10);
  if (period.periodFrom !== todayIso) {
    await createPdt3072PcGroupMonthStartCoverageVersion(
      Request,
      groupId,
      priceComponentIds,
      period.periodFrom,
    );
  }

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  await createPdt3072ScalePodWithMeter(ctx, 0);
  await createPdt3072ScalePodWithMeter(ctx, 1);

  const productPayload = GeneratePayload.productAndServices.product();
  productPayload.priceComponentIds = [];
  productPayload.priceComponentGroupIds = [groupId];
  if (options.invoiceTemplateId && options.invoiceTemplateId > 0) {
    productPayload.templateIds = [
      {
        templateId: envVariables.invoice_email_template,
        templateType: 'EMAIL_TEMPLATE',
      },
      {
        templateId: options.invoiceTemplateId,
        templateType: 'INVOICE_TEMPLATE',
      },
      {
        templateId: envVariables.product_contract_template,
        templateType: 'CONTRACT_TEMPLATE',
      },
    ];
  }
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  if (options.invoiceTemplateId && options.invoiceTemplateId > 0) {
    const requestInvoiceTpl = productPayload.templateIds?.find(
      (t: { templateType?: string }) => t.templateType === 'INVOICE_TEMPLATE',
    );
    expect(
      Number(requestInvoiceTpl?.templateId),
      'product payload INVOICE_TEMPLATE must be the custom uploaded template',
    ).toBe(options.invoiceTemplateId);
    expect(
      Number(requestInvoiceTpl?.templateId),
      'INVOICE_TEMPLATE must not use envVariables.invoice_document_template',
    ).not.toBe(Number(envVariables.invoice_document_template));
  }

  // Do not set entryInForceDate to periodFrom — API requires it in the future.
  // Default product_contract + POD activation at month start is enough for Dev.
  // product_contract() GETs /pod/{id} for estimatedMonthlyAvgConsumption; this token gets 403,
  // so override from values we stored on create (pod_slp default monthly=1 → annual MWh).
  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
  let monthlySum = 0;
  for (const pod of Responses.pod) {
    const row = pod as { type?: string; estimatedMonthlyAvgConsumption?: number };
    if ((row.type ?? 'CONSUMER') === 'CONSUMER') {
      monthlySum += Number(row.estimatedMonthlyAvgConsumption ?? 0);
    }
  }
  contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh =
    (monthlySum * 12) / 1000;
  const contract = await Request.post(Endpoints.productContract, {
    data: contractPayload,
  });
  await expect(contract).CheckResponse();
  const contractBody = await contract.json();
  Responses.productContract.push(contractBody);
  const contractId = Number((contractBody as { id?: number }).id);
  expect(contractId).toBeGreaterThan(0);

  // Activate from billing window start so main can bill pre-boundary (v1) + post-boundary (v2) halves.
  const podActivation0 = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(0, period.periodFrom),
  });
  await expect(podActivation0).CheckResponse();
  const podActivation1 = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(1, period.periodFrom),
  });
  await expect(podActivation1).CheckResponse();

  // Mid-period version uses **new** scale PC entities (new ids), not the v1 ids reused.
  const priceComponentIdsV2 = await createPdt3072CloneScalePcsForNewVersion(
    ctx,
    scaleTariffId,
    scaleCodeId,
  );
  expect(
    new Set(priceComponentIdsV2).size,
    'v2 clone PCs must be distinct ids',
  ).toBe(priceComponentIdsV2.length);
  for (const v1Id of priceComponentIds) {
    expect(
      priceComponentIdsV2.includes(v1Id),
      `v2 must not reuse v1 priceComponentId=${v1Id}; v2=${JSON.stringify(priceComponentIdsV2)}`,
    ).toBe(false);
  }

  await createPdt3072PcGroupNewVersion(
    Request,
    groupId,
    priceComponentIdsV2,
    period.versionBoundary,
  );

  await runPdt3072PodScaleBeforeBilling(ctx, 0, period);
  await runPdt3072PodScaleBeforeBilling(ctx, 1, period);

  const podIdentifiers = Responses.pod.map((pod, idx) => {
    const identifier = String((pod as { identifier?: string }).identifier ?? '').trim();
    expect(
      identifier,
      `PDT-3072: Responses.pod[${idx}] identifier missing (avoid GET /pod — ACCESS_DENIED)`,
    ).toBeTruthy();
    return identifier;
  });

  return {
    period,
    podIdentifiers,
    correctedPodIndex,
    contractId,
    groupId,
    priceComponentIds,
    priceComponentIdsV2,
    hasMidPeriodPcGroupVersion: true,
    originalInvoiceId: 0,
    originalInvoiceNumber: '',
  };
}

export async function runPdt3072StandardBillingAndRealize(ctx: Pdt3072Fx): Promise<{
  originalInvoiceId: number;
  originalInvoiceNumber: string;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  Responses.billingRun.push(await billingRun.json());

  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);

  const originalInvoiceId = Responses.invoice[Responses.invoice.length - 1] as number;
  expect(originalInvoiceId, 'realized invoice id').toBeGreaterThan(0);

  const invoiceGet = await Request.get(`invoice?id=${originalInvoiceId}`);
  await expect(invoiceGet).CheckResponse();
  const originalInvoiceNumber = String((await invoiceGet.json()).invoiceNumber ?? '');
  expect(originalInvoiceNumber, 'original invoice number required').toBeTruthy();

  return { originalInvoiceId, originalInvoiceNumber };
}

/** Volume-change-only correction — PreProd flags. */
export async function createPdt3072VolumeChangeCorrectionBillingRun(ctx: Pdt3072Fx): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const correctionPayload = await GeneratePayload.billing.correctionBilling(0, false, true);
  correctionPayload.invoiceCorrectionParameters.priceChange = false;
  correctionPayload.invoiceCorrectionParameters.volumeChange = true;
  const correction = await Request.post(Endpoints.billingRun, { data: correctionPayload });
  await expect(correction).CheckResponse();
  const correctionId = Number(await correction.json());
  Responses.billingRun.push(correctionId);
  return correctionId;
}

export async function pollPdt3072CorrectionDraftInvoices(
  Request: baseFixture['Request'],
  correctionBillingRunId: number,
): Promise<Pdt3072CorrectionDraftPoll> {
  const startBilling = await Request.patch(
    `${BILLING_RUN_ROOT}/start-billing?billingRunId=${correctionBillingRunId}`,
  );
  await expect(startBilling).CheckResponse();

  const maxAttempts = Math.floor(DRAFT_MAX_MS / DRAFT_POLL_MS);
  let lastBilling: Record<string, unknown> = {};
  let lastDrafts: Pdt3072CorrectionDraftPoll['draftInvoices'] = [];

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const runRes = await Request.get(`${BILLING_RUN_ROOT}/${correctionBillingRunId}`);
    await expect(runRes).CheckResponse();
    lastBilling = (await runRes.json()) as Record<string, unknown>;

    const drafts = await Request.get(
      `${BILLING_RUN_ROOT}/draft-invoices?id=${correctionBillingRunId}&page=0&size=50`,
    );
    await expect(drafts).CheckResponse();
    const dj = (await drafts.json()) as {
      content?: Array<{ id?: number; invoiceDocumentType?: string; invoiceNumber?: string }>;
    };
    lastDrafts = (dj.content ?? [])
      .map((x) => ({
        id: Number(x.id),
        invoiceDocumentType: x.invoiceDocumentType,
        invoiceNumber: x.invoiceNumber,
      }))
      .filter((x) => Number.isFinite(x.id) && x.id > 0);

    const cp = lastBilling.commonParameters as { status?: string } | undefined;
    if (cp?.status === 'DRAFT' && lastDrafts.length > 0) {
      const debit = lastDrafts.find((d) => d.invoiceDocumentType === 'DEBIT_NOTE');
      const credit = lastDrafts.find((d) => d.invoiceDocumentType === 'CREDIT_NOTE');
      // Prefer both legs (PreProd); proceed with debit once present — credit can lag on volume-change.
      if (debit?.id && (credit?.id || attempt >= Math.min(8, maxAttempts))) {
        expect(debit.id, 'Correction must produce a DEBIT_NOTE draft').toBeTruthy();
        if (!credit?.id) {
          console.warn(
            `[PDT-3072] CREDIT_NOTE draft not present yet after attempt ${attempt}; drafts=${JSON.stringify(lastDrafts)}. Continuing with DEBIT_NOTE-only assert.`,
          );
        }
        return {
          correctionBillingRunId,
          billingRunSnapshot: lastBilling,
          draftInvoices: lastDrafts,
          debitNoteInvoiceId: debit.id,
          creditNoteInvoiceId: credit?.id ?? null,
        };
      }
    }
    await new Promise((r) => setTimeout(r, DRAFT_POLL_MS));
  }

  throw new Error(
    `[PDT-3072] Correction billing run ${correctionBillingRunId} did not reach DRAFT with drafts; last=${JSON.stringify(lastBilling.commonParameters ?? {})} drafts=${JSON.stringify(lastDrafts)}`,
  );
}

export function buildPdt3072AttachmentSummary(args: Record<string, unknown>): Record<string, unknown> {
  return { jiraKey: 'PDT-3072', scenario: 'volume-change-scale-pc-group', ...args };
}

/** One row from `GET /invoice/summary-data` (Swagger InvoiceSummaryDataResponse). */
export type Pdt3072InvoiceSummaryRow = {
  priceComponent?: string;
  totalVolumes?: number;
  value?: string | number;
  type?: 'DIRECT' | 'FROM_PC_GROUP' | 'GROUP' | string;
};

export async function fetchPdt3072InvoiceSummaryRows(
  Request: baseFixture['Request'],
  invoiceId: number,
): Promise<Pdt3072InvoiceSummaryRow[]> {
  const size = 50;
  const out: Pdt3072InvoiceSummaryRow[] = [];
  for (let page = 0; page < 20; page++) {
    const res = await Request.get(`invoice/summary-data?id=${invoiceId}&page=${page}&size=${size}`);
    await expect(res, `PDT-3072/3186 GET invoice/summary-data id=${invoiceId}`).CheckResponse();
    const body = (await res.json()) as {
      content?: Pdt3072InvoiceSummaryRow[];
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

/**
 * PDT-3186: Summary `type=GROUP` rows for a PC group name must appear exactly once
 * (not once per pc_group_detail_id / version).
 */
export function assertPdt3186PcGroupSummaryOnce(
  rows: Pdt3072InvoiceSummaryRow[],
  groupName: string,
): { groupRows: Pdt3072InvoiceSummaryRow[]; count: number } {
  const needle = groupName.trim().toLowerCase();
  const groupRows = rows.filter(
    (r) =>
      String(r.type ?? '').toUpperCase() === 'GROUP' &&
      String(r.priceComponent ?? '').trim().toLowerCase() === needle,
  );
  expect(
    groupRows.length,
    `PDT-3186: Summary must show PC group "${groupName}" exactly once (type=GROUP). ` +
      `Found ${groupRows.length}. All GROUP rows=${JSON.stringify(
        rows.filter((r) => String(r.type ?? '').toUpperCase() === 'GROUP'),
      )}`,
  ).toBe(1);
  return { groupRows, count: groupRows.length };
}
