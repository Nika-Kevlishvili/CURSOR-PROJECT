/**
 * PDT-2891 — Connected invoices (Story V0.2) shared Playwright helpers.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts (interim IAP + billing poll)
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts (billing lifecycle + invoice listing)
 * - tests/billing/forVolumes/deduction.spec.ts (REG-718 interim deduction chain)
 */
import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import {
  asBillingRunId,
  pollInvoiceListing,
  startAndCompleteBillingRun,
} from './rps-pod-invoice-due-date.fixtures';
import {
  preconditionStandardExactInterim,
  preconditionProductContractForManual,
  createManualInterimBillingRun,
  resolvePdt2872Amounts,
  listInterimInvoiceListingRows,
  type Pdt2872Fx,
} from './pdt-2872-minimal-interim-payment.fixtures';

import { resolveAccountingPeriodIdForBillingDate } from './pdt-2599-service-contract.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';

export const JIRA_KEY = 'PDT-2891';
export const BILLING_RUN_ROOT = 'billing-run';

export type Pdt2891Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type ConnectedInvoiceRef = { id?: number; invoiceNumber?: string };

/** Story V0.2 / TC convention: `connectedInvoices` post-rename; fallback `debitCredits` on Dev. */
export function readConnectedInvoiceIds(body: Record<string, unknown>): number[] {
  const raw = (body.connectedInvoices ?? body.debitCredits) as ConnectedInvoiceRef[] | undefined;
  if (!Array.isArray(raw)) return [];
  return raw
    .map((row) => row?.id)
    .filter((id): id is number => typeof id === 'number' && Number.isFinite(id));
}

export async function getInvoiceBody(
  Request: Pdt2891Fx['Request'],
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`invoice?id=${invoiceId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function assertConnectedContains(
  body: Record<string, unknown>,
  expectedIds: number[],
  label: string,
): void {
  const ids = new Set(readConnectedInvoiceIds(body));
  for (const expected of expectedIds) {
    expect(ids.has(expected), `${label}: Connected invoices must include id ${expected}`).toBeTruthy();
  }
}

export function assertConnectedExcludes(
  body: Record<string, unknown>,
  excludedIds: number[],
  label: string,
): void {
  const ids = new Set(readConnectedInvoiceIds(body));
  for (const excluded of excludedIds) {
    expect(ids.has(excluded), `${label}: Connected invoices must NOT include id ${excluded}`).toBeFalsy();
  }
}

export function assertConnectedLength(
  body: Record<string, unknown>,
  expectedLength: number,
  label: string,
): void {
  expect(readConnectedInvoiceIds(body).length, label).toBe(expectedLength);
}

export async function assertBidirectionalConnected(
  Request: Pdt2891Fx['Request'],
  idA: number,
  idB: number,
  label: string,
): Promise<void> {
  const bodyA = await getInvoiceBody(Request, idA);
  const bodyB = await getInvoiceBody(Request, idB);
  assertConnectedContains(bodyA, [idB], `${label} A→B`);
  assertConnectedContains(bodyB, [idA], `${label} B→A`);
}

export function filterListingRows(
  rows: Record<string, unknown>[],
  predicate: (row: Record<string, unknown>) => boolean,
): Record<string, unknown>[] {
  return rows.filter(predicate);
}

export function findInvoiceByDocumentType(
  rows: Record<string, unknown>[],
  documentType: string,
): Record<string, unknown> | undefined {
  return rows.find((r) => r.documentType === documentType || r.invoiceDocumentType === documentType);
}

function lastBillingRunIndex(fx: Pdt2891Fx): number {
  return fx.Responses.billingRun.length - 1;
}

function pushBillingRunId(fx: Pdt2891Fx, billingRunId: number, invoiceNumbers?: number): void {
  if (invoiceNumbers != null) {
    fx.Responses.billingRun.push({ id: billingRunId, invoiceNumbers });
  } else {
    fx.Responses.billingRun.push(billingRunId);
  }
}

/** REG-718 / deduction.spec.ts — start-billing → generating → accounting → COMPLETED. */
export async function completeBillingRunViaWait(
  fx: Pdt2891Fx,
  billingRunIndex: number,
  expectedDraftInvoices = 1,
): Promise<void> {
  await fx.GeneratePayload.billing.waitForInvoiceGeneration(
    true,
    true,
    expectedDraftInvoices,
    billingRunIndex,
  );
}

export async function resolveInterimInvoiceIds(
  Request: Pdt2891Fx['Request'],
  interimBillingRunId: number,
  expectedCount = 1,
): Promise<number[]> {
  const listing = await listInterimInvoiceListingRows(Request, interimBillingRunId);
  const fromListing = listing
    .map((r) => r.id as number)
    .filter((id) => typeof id === 'number');
  if (fromListing.length >= expectedCount) {
    return fromListing.slice(0, expectedCount);
  }
  const rows = await pollInvoiceListing(Request, interimBillingRunId, expectedCount);
  const interimRows = rows.filter((r) => r.invoiceType === 'INTERIM_AND_ADVANCE_PAYMENT');
  const ids = (interimRows.length > 0 ? interimRows : rows)
    .map((r) => r.id as number)
    .filter((id) => typeof id === 'number');
  return ids.slice(0, expectedCount);
}

export async function resolveVolumeHostAfterDeduction(
  Request: Pdt2891Fx['Request'],
  volumeBillingRunId: number,
): Promise<{
  volumeHostInvoiceId: number;
  documentType: string;
  listingRows: Record<string, unknown>[];
}> {
  const listingRows = await pollInvoiceListing(Request, volumeBillingRunId, 1);
  const hostRow =
    findInvoiceByDocumentType(listingRows, 'CREDIT_NOTE') ??
    findInvoiceByDocumentType(listingRows, 'DEBIT_NOTE') ??
    findInvoiceByDocumentType(listingRows, 'INVOICE') ??
    listingRows[0];

  const volumeHostInvoiceId = hostRow?.id as number;
  const documentType = String(hostRow?.documentType ?? hostRow?.invoiceDocumentType ?? '');

  expect(volumeHostInvoiceId, 'volume host invoice id after FOR_VOLUMES deduction run').toBeTruthy();
  expect(
    documentType === 'CREDIT_NOTE' || documentType === 'DEBIT_NOTE',
    `interim deduction must set volume host to CREDIT_NOTE or DEBIT_NOTE (got ${documentType})`,
  ).toBeTruthy();

  return { volumeHostInvoiceId, documentType, listingRows };
}

/** @deprecated use resolveVolumeHostAfterDeduction — interim deduction is 2 invoices, not 3 */
export async function resolveVolumeAndDeductionCd(
  Request: Pdt2891Fx['Request'],
  volumeBillingRunId: number,
): Promise<{ volumeInvoiceId: number; cdNoteId: number; listingRows: Record<string, unknown>[] }> {
  const { volumeHostInvoiceId, listingRows } = await resolveVolumeHostAfterDeduction(
    Request,
    volumeBillingRunId,
  );
  return {
    volumeInvoiceId: volumeHostInvoiceId,
    cdNoteId: volumeHostInvoiceId,
    listingRows,
  };
}

function billingMonthAfter(period: BillingMonthRange): BillingMonthRange {
  const y = Number(period.endDate.slice(0, 4));
  const m = Number(period.endDate.slice(5, 7));
  const ref = new Date(Date.UTC(y, m, 1));
  const ny = ref.getUTCFullYear();
  const nm = ref.getUTCMonth() + 1;
  const end = new Date(Date.UTC(ny, nm, 0)).toISOString().slice(0, 10);
  return { startDate: `${end.slice(0, 7)}-01`, endDate: end };
}

/** Two consecutive calendar months relative to today (pulling.spec / REG-966 pattern). */
function twoConsecutiveBillingMonths(): [BillingMonthRange, BillingMonthRange] {
  const earlier = {
    startDate: randomGens.generateMonthStartDate('yyyy-mm-dd', -2),
    endDate: randomGens.generateMonthEndDate('yyyy-mm-dd', -2),
  };
  const later = {
    startDate: randomGens.generateMonthStartDate('yyyy-mm-dd', -1),
    endDate: randomGens.generateMonthEndDate('yyyy-mm-dd', -1),
  };
  return [earlier, later];
}

async function alignBillingRunMaxEndDateOnly(
  payload: Record<string, unknown>,
  period: BillingMonthRange,
): Promise<void> {
  const bp = (payload.basicParameters ?? {}) as Record<string, unknown>;
  bp.maxEndDate = period.endDate;
  payload.basicParameters = bp;
}

async function alignBillingRunPayloadToPeriod(
  fx: Pdt2891Fx,
  payload: Record<string, unknown>,
  period: BillingMonthRange,
): Promise<void> {
  const invoiceDate = period.endDate;
  const accountingPeriodId = await resolveAccountingPeriodIdForBillingDate(
    fx.Request,
    invoiceDate,
  );
  const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
  cp.invoiceDate = invoiceDate;
  cp.taxEventDate = invoiceDate;
  cp.accountingPeriodId = accountingPeriodId;
  payload.commonParameters = cp;

  const bp = (payload.basicParameters ?? {}) as Record<string, unknown>;
  bp.maxEndDate = period.endDate;
  payload.basicParameters = bp;
}

/** Contract + POD chain — same as manual interim preconditions (default Dev-valid dates). */
async function ensureProductContractForStandardBilling(fx: Pdt2891Fx): Promise<void> {
  if (fx.Responses.productContract.length > 0) return;
  await preconditionProductContractForManual(fx as Pdt2872Fx);
}

export type BillingMonthRange = { startDate: string; endDate: string };

/** Distinct months for additional FOR_VOLUMES runs on the same contract (interim / legacy helpers). */
const VOLUME_PROFILE_MONTHS: BillingMonthRange[] = [
  { startDate: '2026-05-01', endDate: '2026-05-31' },
  { startDate: '2026-06-01', endDate: '2026-06-30' },
  { startDate: '2026-07-01', endDate: '2026-07-31' },
  { startDate: '2026-08-01', endDate: '2026-08-31' },
];

function pickNextVolumeMonth(fx: Pdt2891Fx): BillingMonthRange {
  const used = new Set(
    fx.Responses.dataByProfiles.map((p) =>
      String((p as { periodFrom?: string }).periodFrom ?? '').slice(0, 7),
    ),
  );
  let latestUsedYm = '';
  for (const ym of used) {
    if (ym > latestUsedYm) latestUsedYm = ym;
  }
  for (const range of VOLUME_PROFILE_MONTHS) {
    const ym = range.startDate.slice(0, 7);
    if (!used.has(ym) && ym > latestUsedYm) return range;
  }
  return VOLUME_PROFILE_MONTHS[fx.Responses.dataByProfiles.length % VOLUME_PROFILE_MONTHS.length];
}

/** Earliest unused month strictly before the latest posted profile (e.g. May after June on Dev). */
function pickEarlierVolumeMonth(fx: Pdt2891Fx): BillingMonthRange {
  const used = new Set(
    fx.Responses.dataByProfiles.map((p) =>
      String((p as { periodFrom?: string }).periodFrom ?? '').slice(0, 7),
    ),
  );
  let latestUsedYm = '';
  for (const ym of used) {
    if (ym > latestUsedYm) latestUsedYm = ym;
  }
  let candidate: BillingMonthRange | undefined;
  for (const range of VOLUME_PROFILE_MONTHS) {
    const ym = range.startDate.slice(0, 7);
    if (!used.has(ym) && ym < latestUsedYm) {
      if (!candidate || ym > candidate.startDate.slice(0, 7)) candidate = range;
    }
  }
  if (candidate) return candidate;
  for (const range of VOLUME_PROFILE_MONTHS) {
    if (!used.has(range.startDate.slice(0, 7))) return range;
  }
  return VOLUME_PROFILE_MONTHS[0];
}

export async function postEnergyProfileForPeriod(
  fx: Pdt2891Fx,
  period: BillingMonthRange,
): Promise<void> {
  const payload = await fx.GeneratePayload.energyData.profile1Month(0, [period]);
  payload.timeZone = 'CET';
  const profiles = await fx.Request.post('billing-by-profile', { data: payload });
  await expect(profiles).CheckResponse();
  const profileData = await profiles.json();
  fx.Responses.dataByProfiles.push({
    id: profileData,
    periodFrom: payload.periodFrom,
    periodTo: payload.periodTo,
    periodType: payload.periodType,
  });
}

export async function postEnergyProfileMonth(fx: Pdt2891Fx): Promise<void> {
  const isFirstProfile = fx.Responses.dataByProfiles.length === 0;
  const period = isFirstProfile ? undefined : pickNextVolumeMonth(fx);
  if (period) {
    await postEnergyProfileForPeriod(fx, period);
    return;
  }
  const payload = await fx.GeneratePayload.energyData.profile1Month();
  payload.timeZone = 'CET';
  const profiles = await fx.Request.post('billing-by-profile', { data: payload });
  await expect(profiles).CheckResponse();
  const profileData = await profiles.json();
  fx.Responses.dataByProfiles.push({
    id: profileData,
    periodFrom: payload.periodFrom,
    periodTo: payload.periodTo,
    periodType: payload.periodType,
  });
}

/**
 * Story §3.3 / §3.4 — STANDARD_BILLING (FOR_VOLUMES application) → REAL INVOICE.
 * TC .md: energy data for explicit calendar month, then billing run on existing contract.
 */
export async function runStandardBillingInvoice(
  fx: Pdt2891Fx,
  options: {
    period?: BillingMonthRange;
    setupContract?: boolean;
    alignBillingDatesToPeriod?: boolean;
    alignBillingMaxEndDateOnly?: boolean;
    useStartAndCompleteBilling?: boolean;
    billingContractIndex?: number;
    skipEnergyPost?: boolean;
  } = {},
): Promise<number> {
  const setupContract = options.setupContract !== false;
  const period = options.period;
  const alignBillingDatesToPeriod = options.alignBillingDatesToPeriod === true;
  const contractIndex = options.billingContractIndex ?? 0;

  if (setupContract && fx.Responses.productContract.length === 0) {
    await ensureProductContractForStandardBilling(fx);
  }

  await test.step(
    `Precondition: energy data (${period?.startDate ?? 'default month'})`,
    async () => {
      if (options.skipEnergyPost) return;
      if (period) {
        await postEnergyProfileForPeriod(fx, period);
      } else if (options.billingContractIndex == null) {
        await postEnergyProfileMonth(fx);
      }
    },
  );

  let billingRunId = 0;
  await test.step('Precondition: STANDARD_BILLING (FOR_VOLUMES) billing run', async () => {
    const payload = await fx.GeneratePayload.billing.billingRun(
      'CONTRACT',
      ['FOR_VOLUMES'],
      0,
      contractIndex,
    );
    if (period && alignBillingDatesToPeriod) {
      await alignBillingRunPayloadToPeriod(fx, payload as Record<string, unknown>, period);
    } else if (period && options.alignBillingMaxEndDateOnly) {
      alignBillingRunMaxEndDateOnly(payload as Record<string, unknown>, period);
    }
    const billingRun = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
    await expect(billingRun).CheckResponse();
    fx.Responses.billingRun.push(await billingRun.json());
    billingRunId = asBillingRunId(fx.Responses.billingRun[fx.Responses.billingRun.length - 1]);
  });

  await test.step('Complete STANDARD_BILLING run to REAL', async () => {
    if (options.useStartAndCompleteBilling) {
      await startAndCompleteBillingRun(fx.Request, billingRunId);
    } else {
      await completeBillingRunViaWait(fx, lastBillingRunIndex(fx), 1);
    }
  });

  const rows = await pollInvoiceListing(fx.Request, billingRunId, 1);
  const invoiceRow = findInvoiceByDocumentType(rows, 'INVOICE') ?? rows[0];
  expect(
    invoiceRow?.id,
    `STANDARD_BILLING INVOICE for billingRunId=${billingRunId}; listing=${JSON.stringify(rows.map((r) => ({ id: r.id, documentType: r.documentType })))}`,
  ).toBeTruthy();
  const invoiceId = invoiceRow!.id as number;
  fx.Responses.invoice.push(invoiceId);
  return invoiceId;
}

async function completeStandardBillingRunOnly(
  fx: Pdt2891Fx,
  options: { useStartAndCompleteBilling?: boolean } = {},
): Promise<number> {
  let billingRunId = 0;
  await test.step('STANDARD_BILLING (FOR_VOLUMES) on existing energy data', async () => {
    const payload = await fx.GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
    const billingRun = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
    await expect(billingRun).CheckResponse();
    fx.Responses.billingRun.push(await billingRun.json());
    billingRunId = asBillingRunId(fx.Responses.billingRun[fx.Responses.billingRun.length - 1]);
  });

  await test.step('Complete STANDARD_BILLING run to REAL', async () => {
    if (options.useStartAndCompleteBilling) {
      await startAndCompleteBillingRun(fx.Request, billingRunId);
    } else {
      await completeBillingRunViaWait(fx, lastBillingRunIndex(fx), 1);
    }
  });

  const rows = await pollInvoiceListing(fx.Request, billingRunId, 1);
  const invoiceRow = findInvoiceByDocumentType(rows, 'INVOICE') ?? rows[0];
  expect(
    invoiceRow?.id,
    `STANDARD_BILLING INVOICE billingRunId=${billingRunId}; listing=${JSON.stringify(rows.map((r) => ({ id: r.id, documentType: r.documentType })))}`,
  ).toBeTruthy();
  const invoiceId = invoiceRow!.id as number;
  fx.Responses.invoice.push(invoiceId);
  return invoiceId;
}

/** TC-BE-7 — two REAL invoice parents on same contract for multi-parent manual C/D. */
export async function runTwoStandardBillingParents(fx: Pdt2891Fx): Promise<{
  parentInvoiceId1: number;
  parentInvoiceId2: number;
}> {
  const parentInvoiceId1 = await runStandardBillingInvoice(fx, { setupContract: true });

  const secondMonth = pickNextVolumeMonth(fx);
  const parentInvoiceId2 = await runStandardBillingInvoice(fx, {
    setupContract: false,
    period: secondMonth,
  });

  return { parentInvoiceId1, parentInvoiceId2 };
}

/** Standard FOR_VOLUMES without IAP — plain INVOICE, no deduction links. */
export async function runStandardVolumeInvoice(fx: Pdt2891Fx): Promise<number> {
  return runStandardBillingInvoice(fx, { setupContract: true });
}

/** @deprecated Prefer runTwoStandardBillingParents (TC-BE-7) or runStandardBillingInvoice with explicit period. */
export async function runAdditionalVolumeInvoice(fx: Pdt2891Fx): Promise<number> {
  return runStandardBillingInvoice(fx, { setupContract: false });
}

type CorrectionRunArtifact = {
  invoiceId: number;
  invoiceType: string;
  docType: string;
  totalInclVat: number;
  parentInvoiceId?: number;
};

/**
 * §3.4 INVOICE_CORRECTION — both legs use invoiceType=CORRECTION in Phoenix
 * (BillingRunCorrectionService.reverseInvoices). Reversal leg is always CREDIT_NOTE;
 * correction leg is CREDIT_NOTE or DEBIT_NOTE from delta sign.
 */
export async function classifyCorrectionRunArtifacts(
  fx: Pdt2891Fx,
  rows: Record<string, unknown>[],
  originalInvoiceId: number,
): Promise<{ reversalInvoiceId?: number; correctionCdId?: number }> {
  const originalBody = await getInvoiceBody(fx.Request, originalInvoiceId);
  const originalTotal = Math.abs(
    Number(originalBody.totalAmountIncludingVat ?? originalBody.totalAmountExcludingVat ?? 0),
  );

  const artifacts: CorrectionRunArtifact[] = [];
  for (const row of rows) {
    const invoiceId = row.id as number;
    if (!invoiceId) continue;
    const body = await getInvoiceBody(fx.Request, invoiceId);
    const parentId = body.parentInvoiceId as number | undefined;
    const reversalFrom = body.reversalCreatedFromId as number | undefined;
    if (parentId !== originalInvoiceId && reversalFrom !== originalInvoiceId) {
      continue;
    }
    artifacts.push({
      invoiceId,
      invoiceType: String(body.invoiceType ?? row.invoiceType ?? ''),
      docType: String(body.invoiceDocumentType ?? row.documentType ?? ''),
      totalInclVat: Math.abs(
        Number(body.totalAmountIncludingVat ?? body.totalAmountExcludingVat ?? 0),
      ),
      parentInvoiceId: parentId,
    });
  }

  const pool =
    artifacts.length >= 2
      ? artifacts
      : (
          await Promise.all(
            rows.map(async (row) => {
              const invoiceId = row.id as number;
              if (!invoiceId) return null;
              const body = await getInvoiceBody(fx.Request, invoiceId);
              return {
                invoiceId,
                invoiceType: String(body.invoiceType ?? row.invoiceType ?? ''),
                docType: String(body.invoiceDocumentType ?? row.documentType ?? ''),
                totalInclVat: Math.abs(
                  Number(body.totalAmountIncludingVat ?? body.totalAmountExcludingVat ?? 0),
                ),
                parentInvoiceId: body.parentInvoiceId as number | undefined,
              } satisfies CorrectionRunArtifact;
            }),
          )
        ).filter((a): a is CorrectionRunArtifact => a != null);

  const correctionPool = pool.filter(
    (a) =>
      a.invoiceType === 'CORRECTION' ||
      a.docType === 'CREDIT_NOTE' ||
      a.docType === 'DEBIT_NOTE',
  );
  const candidates = correctionPool.length >= 2 ? correctionPool : pool;

  const credits = candidates.filter((a) => a.docType === 'CREDIT_NOTE');
  const debits = candidates.filter((a) => a.docType === 'DEBIT_NOTE');

  let reversalInvoiceId: number | undefined;
  let correctionCdId: number | undefined;

  if (debits.length >= 1 && credits.length >= 1) {
    correctionCdId = debits[0].invoiceId;
    const reversalCredit = [...credits].sort(
      (a, b) =>
        Math.abs(b.totalInclVat - originalTotal) - Math.abs(a.totalInclVat - originalTotal),
    )[0];
    reversalInvoiceId = reversalCredit?.invoiceId;
  } else if (credits.length >= 2) {
    const sorted = [...credits].sort(
      (a, b) =>
        Math.abs(b.totalInclVat - originalTotal) - Math.abs(a.totalInclVat - originalTotal),
    );
    reversalInvoiceId = sorted[0]?.invoiceId;
    correctionCdId = sorted[1]?.invoiceId;
  } else if (credits.length === 1) {
    reversalInvoiceId = credits[0].invoiceId;
    correctionCdId = debits[0]?.invoiceId;
  } else if (candidates.length >= 2) {
    reversalInvoiceId = candidates[0].invoiceId;
    correctionCdId = candidates[1].invoiceId;
  }

  return { reversalInvoiceId, correctionCdId };
}

export type InterimDeductionOutcome = {
  /** First interim id (convenience when count = 1) */
  interimInvoiceId: number;
  /** All interim invoice ids included in the deduction run */
  interimInvoiceIds: number[];
  /** Single volume host invoice (CREDIT_NOTE or DEBIT_NOTE after interim deduction) */
  volumeHostInvoiceId: number;
  volumeDocumentType: string;
  interimBillingRunId: number;
  volumeBillingRunId: number;
};

/** Standard + manual interim both deducted in one FOR_VOLUMES run (TC-BE-17). */
export type MixedInterimDeductionOutcome = InterimDeductionOutcome & {
  standardInterimId: number;
  manualInterimId: number;
  standardInterimBillingRunId: number;
  manualInterimBillingRunId: number;
};

/** Interim ↔ volume host (2 invoices): each interim lists volume host and vice versa. */
export async function assertInterimDeductionConnected(
  Request: Pdt2891Fx['Request'],
  interimIds: number[],
  volumeHostInvoiceId: number,
  label: string,
): Promise<void> {
  const volumeBody = await getInvoiceBody(Request, volumeHostInvoiceId);
  assertConnectedContains(volumeBody, interimIds, `${label} volume host`);

  for (const interimId of interimIds) {
    const interimBody = await getInvoiceBody(Request, interimId);
    assertConnectedContains(interimBody, [volumeHostInvoiceId], `${label} interim ${interimId}`);
  }
}

/**
 * Story §3.1 — standard interim (IAP EXACT_AMOUNT) then FOR_VOLUMES with interim deduction.
 * Runtime: 2 REAL invoices — interim + volume host (document becomes CREDIT_NOTE or DEBIT_NOTE).
 * Reference: REG-718 / deduction.spec.ts
 */
export async function runInterimDeductionLink(
  fx: Pdt2891Fx,
  iapExclValues: number[] = [],
): Promise<InterimDeductionOutcome> {
  const amounts = await resolvePdt2872Amounts(fx.Request);
  const excl = iapExclValues.length > 0 ? iapExclValues : [amounts.exclFor500];
  const expectedInterimCount = excl.length;

  const { billingRunId: interimBillingRunId } = await preconditionStandardExactInterim(
    fx as Pdt2872Fx,
    excl,
  );
  const interimBillingIndex = lastBillingRunIndex(fx);

  await test.step('Energy data before billing (REG-718)', async () => postEnergyProfileMonth(fx));

  await test.step('Complete interim billing run to REAL', async () => {
    await completeBillingRunViaWait(fx, interimBillingIndex, expectedInterimCount);
  });

  const interimIds = await resolveInterimInvoiceIds(
    fx.Request,
    interimBillingRunId,
    expectedInterimCount,
  );
  expect(
    interimIds.length,
    `expected ${expectedInterimCount} interim invoice(s) after REAL interim billing`,
  ).toBeGreaterThanOrEqual(expectedInterimCount);
  const interimInvoiceId = interimIds[0];
  for (const id of interimIds) {
    if (!fx.Responses.invoice.includes(id)) {
      fx.Responses.invoice.push(id);
    }
  }

  let volumeBillingRunId = 0;
  await test.step('Create FOR_VOLUMES billing run (interim deduction)', async () => {
    const billingRun = await fx.Request.post(fx.Endpoints.billingRun, {
      data: await fx.GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']),
    });
    await expect(billingRun).CheckResponse();
    const raw = await billingRun.json();
    fx.Responses.billingRun.push(raw);
    volumeBillingRunId = asBillingRunId(raw);
  });

  const volumeBillingIndex = lastBillingRunIndex(fx);
  await test.step('Complete volume billing with interim deduction', async () => {
    await completeBillingRunViaWait(fx, volumeBillingIndex, 1);
  });

  const { volumeHostInvoiceId, documentType } = await resolveVolumeHostAfterDeduction(
    fx.Request,
    volumeBillingRunId,
  );
  if (!fx.Responses.invoice.includes(volumeHostInvoiceId)) {
    fx.Responses.invoice.push(volumeHostInvoiceId);
  }

  return {
    interimInvoiceId,
    interimInvoiceIds: interimIds,
    volumeHostInvoiceId,
    volumeDocumentType: documentType,
    interimBillingRunId,
    volumeBillingRunId,
  };
}

/** @deprecated alias — use runInterimDeductionLink */
export const runInterimDeductionThreeWayLink = runInterimDeductionLink;

export type ManualNoteOutcome = {
  cdNoteId: number;
  billingRunId: number;
};

export async function buildManualCreditOrDebitNotePayload(
  fx: Pdt2891Fx,
  parentInvoiceIds: number[],
  documentType: 'CREDIT_NOTE' | 'DEBIT_NOTE',
  customerDetailIdOverride?: number,
): Promise<Record<string, unknown>> {
  const invoiceRefs: { invoiceId: number; invoiceNumber: string }[] = [];
  for (const parentId of parentInvoiceIds) {
    const invBody = await getInvoiceBody(fx.Request, parentId);
    invoiceRefs.push({
      invoiceId: parentId,
      invoiceNumber: String(invBody.invoiceNumber),
    });
  }

  const payload =
    documentType === 'CREDIT_NOTE'
      ? (fx.GeneratePayload.billing.creditNote() as Record<string, unknown>)
      : (fx.GeneratePayload.billing.debitNote() as Record<string, unknown>);

  const basicParams = (
    payload as {
      manualCreditOrDebitNoteParameters: {
        manualCreditOrDebitNoteBasicDataParameters: Record<string, unknown>;
      };
    }
  ).manualCreditOrDebitNoteParameters.manualCreditOrDebitNoteBasicDataParameters;

  basicParams.billingRunInvoiceInformationList = invoiceRefs;
  basicParams.documentType = documentType;
  basicParams.external = false;

  if (parentInvoiceIds.length > 1) {
    basicParams.costCenterControllingOrderManual = true;
    basicParams.numberOfIncomeAccountManual = true;
    basicParams.applicableInterestRateManual = true;
    basicParams.vatRateManual = true;
    basicParams.directDebitManual = true;
  }

  if (parentInvoiceIds.length > 0 && customerDetailIdOverride == null) {
    const parentBody = await getInvoiceBody(fx.Request, parentInvoiceIds[0]);
    const customerBlock = parentBody.customer as {
      customerDetailsId?: number;
      lastCustomerDetailId?: number;
    } | null;
    const comm = parentBody.customerCommunications as { id?: number } | null;
    basicParams.customerDetailId =
      customerBlock?.customerDetailsId ?? customerBlock?.lastCustomerDetailId;
    if (comm?.id != null) {
      basicParams.invoiceCommunicationDataId = comm.id;
    }
  }

  if (envVariables.bank) {
    basicParams.bankId = envVariables.bank;
  }
  if (envVariables.interest_rate ?? envVariables.base_interest_rate) {
    basicParams.applicableInterestRateId =
      envVariables.interest_rate ?? envVariables.base_interest_rate;
  }

  if (customerDetailIdOverride != null) {
    basicParams.customerDetailId = customerDetailIdOverride;
  } else if (basicParams.customerDetailId == null && fx.Responses.customer.length > 0) {
    const customerId = (fx.Responses.customer[0] as { id?: number }).id;
    if (customerId) {
      const customerGet = await fx.Request.get(`customer/${customerId}?version=1`);
      await expect(customerGet).CheckResponse();
      const customerJson = await customerGet.json();
      basicParams.customerDetailId = customerJson.customerDetailsId ?? customerJson.lastCustomerDetailId;
      const commId = customerJson.communicationData?.[0]?.id;
      if (commId != null) {
        basicParams.invoiceCommunicationDataId = commId;
      }
    }
  }

  const summaryParams = (
    payload as {
      manualCreditOrDebitNoteParameters: {
        manualCreditOrDebitNoteSummaryDataParameters: {
          summaryDataRowList: { value: number | string }[];
        };
      };
    }
  ).manualCreditOrDebitNoteParameters.manualCreditOrDebitNoteSummaryDataParameters;

  for (const row of summaryParams.summaryDataRowList) {
    if (row.value === '{{InvoiceValue}}') {
      row.value = 10;
    }
  }

  if (parentInvoiceIds.length > 0) {
    const parentBody = await getInvoiceBody(fx.Request, parentInvoiceIds[0]);
    const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
    cp.invoiceDate = parentBody.invoiceDate ?? cp.invoiceDate;
    cp.taxEventDate = parentBody.taxEventDate ?? cp.taxEventDate ?? cp.invoiceDate;
    if (documentType === 'CREDIT_NOTE') {
      cp.invoiceDueDate = null;
      cp.dueDate = null;
    } else {
      const invoiceDateStr = String(cp.invoiceDate ?? '').slice(0, 10);
      const parentDeadline = String(parentBody.paymentDeadline ?? '').slice(0, 10);
      let dueDateStr = parentDeadline;
      if (!dueDateStr || dueDateStr <= invoiceDateStr) {
        const anchor = invoiceDateStr || randomGens.generateTodaysDate('yyyy-mm-dd');
        const d = new Date(`${anchor}T12:00:00Z`);
        d.setUTCDate(d.getUTCDate() + 14);
        dueDateStr = d.toISOString().slice(0, 10);
      }
      cp.invoiceDueDate = 'DATE';
      cp.dueDate = dueDateStr;
    }
    payload.commonParameters = cp;
  }

  return payload;
}

export async function createManualCreditOrDebitNote(
  fx: Pdt2891Fx,
  parentInvoiceIds: number[],
  documentType: 'CREDIT_NOTE' | 'DEBIT_NOTE',
  customerDetailId?: number,
): Promise<{ billingRunId: number; createStatus: number }> {
  const payload = await buildManualCreditOrDebitNotePayload(
    fx,
    parentInvoiceIds,
    documentType,
    customerDetailId,
  );

  const res = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  const createStatus = res.status();
  if (!res.ok()) {
    const errBody = await res.text();
    throw new Error(
      `POST manual ${documentType} billing run failed: HTTP ${createStatus} — ${errBody.slice(0, 800)}`,
    );
  }
  const billingRunId = asBillingRunId(await res.json());
  pushBillingRunId(fx, billingRunId);
  return { billingRunId, createStatus };
}

export async function startManualNoteAndGetCdId(
  fx: Pdt2891Fx,
  billingRunId: number,
  documentType: 'CREDIT_NOTE' | 'DEBIT_NOTE',
): Promise<number> {
  expect(billingRunId, 'manual C/D billingRunId must be created before start-billing').toBeGreaterThan(0);

  await test.step(`Complete manual ${documentType} billing run`, async () => {
    await startAndCompleteBillingRun(fx.Request, billingRunId);
  });

  const rows = await pollInvoiceListing(fx.Request, billingRunId, 1);
  const cdRow =
    findInvoiceByDocumentType(rows, documentType) ??
    rows.find((r) => r.documentType === documentType);
  const cdNoteId = cdRow?.id as number;
  expect(cdNoteId, `manual ${documentType} invoice id`).toBeTruthy();
  fx.Responses.invoice.push(cdNoteId);
  return cdNoteId;
}

export async function runManualInterimThenDeduction(
  fx: Pdt2891Fx,
): Promise<InterimDeductionOutcome> {
  const amounts = await resolvePdt2872Amounts(fx.Request);
  await preconditionProductContractForManual(fx as Pdt2872Fx);

  let manualInterimBillingRunId = 0;
  await test.step('Create manual interim billing run', async () => {
    manualInterimBillingRunId = await createManualInterimBillingRun(
      fx as Pdt2872Fx,
      amounts.exclFor500,
    );
  });

  await test.step('Complete manual interim billing to REAL', async () => {
    await startAndCompleteBillingRun(fx.Request, manualInterimBillingRunId);
  });

  const interimIds = await resolveInterimInvoiceIds(fx.Request, manualInterimBillingRunId, 1);
  const interimInvoiceId = interimIds[0];
  if (!fx.Responses.invoice.includes(interimInvoiceId)) {
    fx.Responses.invoice.push(interimInvoiceId);
  }

  const preBody = await getInvoiceBody(fx.Request, interimInvoiceId);
  assertConnectedLength(preBody, 0, 'manual interim before deduction');

  await test.step('Energy data before volume billing (REG-718)', async () => postEnergyProfileMonth(fx));

  let volumeBillingRunId = 0;
  await test.step('Create FOR_VOLUMES billing run', async () => {
    const billingRun = await fx.Request.post(fx.Endpoints.billingRun, {
      data: await fx.GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']),
    });
    await expect(billingRun).CheckResponse();
    const raw = await billingRun.json();
    fx.Responses.billingRun.push(raw);
    volumeBillingRunId = asBillingRunId(raw);
  });

  const volumeBillingIndex = lastBillingRunIndex(fx);
  await test.step('Complete volume billing with interim deduction', async () => {
    await completeBillingRunViaWait(fx, volumeBillingIndex, 1);
  });

  const { volumeHostInvoiceId, documentType } = await resolveVolumeHostAfterDeduction(
    fx.Request,
    volumeBillingRunId,
  );

  return {
    interimInvoiceId,
    interimInvoiceIds: interimIds,
    volumeHostInvoiceId,
    volumeDocumentType: documentType,
    interimBillingRunId: manualInterimBillingRunId,
    volumeBillingRunId,
  };
}

/**
 * Story §3.1 + §3.2 — one standard interim (IAP) and one manual interim on the same contract,
 * then a single FOR_VOLUMES run deducts both; volume host Connected invoices lists both interims.
 */
export async function runMixedManualAndStandardInterimDeduction(
  fx: Pdt2891Fx,
): Promise<MixedInterimDeductionOutcome> {
  const amounts = await resolvePdt2872Amounts(fx.Request);

  const { billingRunId: standardInterimBillingRunId } = await preconditionStandardExactInterim(
    fx as Pdt2872Fx,
    [amounts.exclFor500],
  );
  const standardBillingIndex = lastBillingRunIndex(fx);

  await test.step('Energy data before billing (REG-718)', async () => postEnergyProfileMonth(fx));

  await test.step('Complete standard interim billing run to REAL', async () => {
    await completeBillingRunViaWait(fx, standardBillingIndex, 1);
  });

  const standardInterimIds = await resolveInterimInvoiceIds(
    fx.Request,
    standardInterimBillingRunId,
    1,
  );
  expect(standardInterimIds.length, 'one standard interim invoice').toBeGreaterThanOrEqual(1);
  const standardInterimId = standardInterimIds[0];
  if (!fx.Responses.invoice.includes(standardInterimId)) {
    fx.Responses.invoice.push(standardInterimId);
  }

  let manualInterimBillingRunId = 0;
  await test.step('Create manual interim billing run on same contract', async () => {
    manualInterimBillingRunId = await createManualInterimBillingRun(
      fx as Pdt2872Fx,
      amounts.exclFor501,
      0,
    );
  });

  await test.step('Complete manual interim billing to REAL', async () => {
    await startAndCompleteBillingRun(fx.Request, manualInterimBillingRunId);
  });

  const manualInterimIds = await resolveInterimInvoiceIds(
    fx.Request,
    manualInterimBillingRunId,
    1,
  );
  expect(manualInterimIds.length, 'one manual interim invoice').toBeGreaterThanOrEqual(1);
  const manualInterimId = manualInterimIds[0];
  if (!fx.Responses.invoice.includes(manualInterimId)) {
    fx.Responses.invoice.push(manualInterimId);
  }

  expect(
    manualInterimId,
    'manual and standard interim must be distinct invoices',
  ).not.toBe(standardInterimId);

  const interimInvoiceIds = [standardInterimId, manualInterimId];

  let volumeBillingRunId = 0;
  await test.step('Create FOR_VOLUMES billing run (mixed interim deduction)', async () => {
    const billingRun = await fx.Request.post(fx.Endpoints.billingRun, {
      data: await fx.GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']),
    });
    await expect(billingRun).CheckResponse();
    const raw = await billingRun.json();
    fx.Responses.billingRun.push(raw);
    volumeBillingRunId = asBillingRunId(raw);
  });

  const volumeBillingIndex = lastBillingRunIndex(fx);
  await test.step('Complete volume billing with mixed interim deduction', async () => {
    await completeBillingRunViaWait(fx, volumeBillingIndex, 1);
  });

  const { volumeHostInvoiceId, documentType } = await resolveVolumeHostAfterDeduction(
    fx.Request,
    volumeBillingRunId,
  );
  if (!fx.Responses.invoice.includes(volumeHostInvoiceId)) {
    fx.Responses.invoice.push(volumeHostInvoiceId);
  }

  return {
    interimInvoiceId: standardInterimId,
    interimInvoiceIds,
    standardInterimId,
    manualInterimId,
    standardInterimBillingRunId,
    manualInterimBillingRunId,
    volumeHostInvoiceId,
    volumeDocumentType: documentType,
    interimBillingRunId: standardInterimBillingRunId,
    volumeBillingRunId,
  };
}

export async function runInvoiceCorrection(
  fx: Pdt2891Fx,
  originalInvoiceId: number,
): Promise<{ correctionBillingRunId: number; reversalInvoiceId?: number; correctionCdId?: number }> {
  if (!fx.Responses.invoice.includes(originalInvoiceId)) {
    fx.Responses.invoice.push(originalInvoiceId);
  }
  const invoiceIndex = fx.Responses.invoice.indexOf(originalInvoiceId);
  const payload = await fx.GeneratePayload.billing.correctionBilling(invoiceIndex);
  const res = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const correctionBillingRunId = asBillingRunId(await res.json());
  pushBillingRunId(fx, correctionBillingRunId, 2);

  const correctionIndex = lastBillingRunIndex(fx);
  await completeBillingRunViaWait(fx, correctionIndex, 2);

  const rows = await pollInvoiceListing(fx.Request, correctionBillingRunId, 2);
  const { reversalInvoiceId, correctionCdId } = await classifyCorrectionRunArtifacts(
    fx,
    rows,
    originalInvoiceId,
  );

  expect(
    rows.length,
    `correction billingRunId=${correctionBillingRunId} must produce 2 invoices`,
  ).toBeGreaterThanOrEqual(2);
  expect(
    reversalInvoiceId,
    `correction reversal leg from run ${correctionBillingRunId} (rows=${rows.map((r) => r.id).join(',')}); expect CORRECTION+CREDIT_NOTE, not InvoiceType.REVERSAL`,
  ).toBeTruthy();

  if (reversalInvoiceId) fx.Responses.invoice.push(reversalInvoiceId);
  if (correctionCdId) fx.Responses.invoice.push(correctionCdId);

  return { correctionBillingRunId, reversalInvoiceId, correctionCdId };
}

export async function runInvoiceReversal(
  fx: Pdt2891Fx,
  originalInvoiceId: number,
): Promise<{ reversalBillingRunId: number; cancellationInvoiceId: number }> {
  if (!fx.Responses.invoice.includes(originalInvoiceId)) {
    fx.Responses.invoice.push(originalInvoiceId);
  }
  const invoiceIndex = fx.Responses.invoice.indexOf(originalInvoiceId);
  const payload = await fx.GeneratePayload.billing.reversalBilling(invoiceIndex);
  const res = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const reversalBillingRunId = asBillingRunId(await res.json());
  pushBillingRunId(fx, reversalBillingRunId, 1);

  const reversalIndex = lastBillingRunIndex(fx);
  await completeBillingRunViaWait(fx, reversalIndex, 1);

  const rows = await pollInvoiceListing(fx.Request, reversalBillingRunId, 1);
  const cancellationRow =
    findInvoiceByDocumentType(rows, 'CREDIT_NOTE') ??
    findInvoiceByDocumentType(rows, 'DEBIT_NOTE') ??
    rows[0];
  expect(
    cancellationRow?.id,
    `cancellation invoice for reversal billingRunId=${reversalBillingRunId}`,
  ).toBeTruthy();
  const cancellationInvoiceId = cancellationRow!.id as number;
  fx.Responses.invoice.push(cancellationInvoiceId);

  return { reversalBillingRunId, cancellationInvoiceId };
}

// ─── PDT-3045 — correction on deducted volume host (regression) ───────────────

export const PDT_3045_JIRA_KEY = 'PDT-3045';

export type DeductedVolumeCorrectionOutcome = {
  interimInvoiceId: number;
  deductedVolumeInvoiceId: number;
  volumeDocumentType: string;
  correctionBillingRunId: number;
  correctionCreditInvoiceId: number;
  correctionDebitInvoiceId: number;
};

/** Map correction run legs to credit/debit by documentType (PDT-3045 wording). */
export async function resolveCorrectionCreditAndDebitIds(
  fx: Pdt2891Fx,
  correctionBillingRunId: number,
  deductedVolumeInvoiceId: number,
): Promise<{ correctionCreditInvoiceId: number; correctionDebitInvoiceId: number }> {
  const rows = await pollInvoiceListing(fx.Request, correctionBillingRunId, 2);
  const { reversalInvoiceId, correctionCdId } = await classifyCorrectionRunArtifacts(
    fx,
    rows,
    deductedVolumeInvoiceId,
  );
  expect(reversalInvoiceId, 'correction credit leg id').toBeTruthy();
  expect(correctionCdId, 'correction debit leg id').toBeTruthy();

  const reversalBody = await getInvoiceBody(fx.Request, reversalInvoiceId!);
  const correctionBody = await getInvoiceBody(fx.Request, correctionCdId!);
  const reversalDoc = String(reversalBody.invoiceDocumentType ?? '');
  const correctionDoc = String(correctionBody.invoiceDocumentType ?? '');

  let correctionCreditInvoiceId = reversalInvoiceId!;
  let correctionDebitInvoiceId = correctionCdId!;
  if (reversalDoc === 'DEBIT_NOTE' && correctionDoc === 'CREDIT_NOTE') {
    correctionCreditInvoiceId = correctionCdId!;
    correctionDebitInvoiceId = reversalInvoiceId!;
  } else {
    expect(reversalDoc, 'correction credit documentType').toBe('CREDIT_NOTE');
    expect(correctionDoc, 'correction debit documentType').toBe('DEBIT_NOTE');
  }

  return { correctionCreditInvoiceId, correctionDebitInvoiceId };
}

/**
 * PDT-3045 expected Connected invoices (Jira customfield_10103):
 * - Interim → Correction Credit, Correction Debit, Deducted volume
 * - Deducted volume → interim
 * - Correction Debit → interim
 * - Correction Credit → interim
 */
export async function assertDeductedVolumeCorrectionConnections(
  Request: Pdt2891Fx['Request'],
  params: {
    interimInvoiceId: number;
    deductedVolumeInvoiceId: number;
    correctionCreditInvoiceId: number;
    correctionDebitInvoiceId: number;
  },
  label: string,
): Promise<void> {
  const interimBody = await getInvoiceBody(Request, params.interimInvoiceId);
  const volumeBody = await getInvoiceBody(Request, params.deductedVolumeInvoiceId);
  const creditBody = await getInvoiceBody(Request, params.correctionCreditInvoiceId);
  const debitBody = await getInvoiceBody(Request, params.correctionDebitInvoiceId);

  assertConnectedContains(
    interimBody,
    [
      params.deductedVolumeInvoiceId,
      params.correctionCreditInvoiceId,
      params.correctionDebitInvoiceId,
    ],
    `${label} interim`,
  );
  assertConnectedContains(volumeBody, [params.interimInvoiceId], `${label} deducted volume`);
  assertConnectedContains(debitBody, [params.interimInvoiceId], `${label} correction debit`);
  assertConnectedContains(creditBody, [params.interimInvoiceId], `${label} correction credit`);
}

/** TC-BE-1 chain + INVOICE_CORRECTION on deducted volume host (PDT-3045). */
export async function runDeductedVolumeCorrectionConnectedFlow(
  fx: Pdt2891Fx,
): Promise<DeductedVolumeCorrectionOutcome> {
  const deduction = await runInterimDeductionLink(fx);
  const deductedVolumeInvoiceId = deduction.volumeHostInvoiceId;

  const { correctionBillingRunId } = await runInvoiceCorrection(fx, deductedVolumeInvoiceId);
  const { correctionCreditInvoiceId, correctionDebitInvoiceId } =
    await resolveCorrectionCreditAndDebitIds(fx, correctionBillingRunId, deductedVolumeInvoiceId);

  return {
    interimInvoiceId: deduction.interimInvoiceId,
    deductedVolumeInvoiceId,
    volumeDocumentType: deduction.volumeDocumentType,
    correctionBillingRunId,
    correctionCreditInvoiceId,
    correctionDebitInvoiceId,
  };
}
