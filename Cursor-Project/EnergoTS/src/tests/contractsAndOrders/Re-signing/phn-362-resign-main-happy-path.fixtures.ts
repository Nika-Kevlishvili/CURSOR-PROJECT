/**
 * PHN-362 main happy-path fixtures — shared helpers for Wait Yes/No × First day / Exact day.
 * Used by PHN-362-resign-main-happy-path.spec.ts only (no mass-import deps).
 */
import { expect, test } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';

type FixtureRequest = baseFixture['Request'];
type FixtureResponses = baseFixture['Responses'];
type FixtureEndpoints = baseFixture['Endpoints'];
type FixtureGeneratePayload = baseFixture['GeneratePayload'];

export const IDX_PRODUCT_STD = 0;
export const IDX_PRODUCT_RS = 1;
export const IDX_POD_A = 0;
// ─── Runtime date helpers (all PHN-362 happy-path tests) ─────────────────────

export function tc39AddMonths(dateStr: string, months: number): string {
  const d = new Date(`${dateStr}T00:00:00Z`);
  d.setUTCMonth(d.getUTCMonth() + months);
  return d.toISOString().slice(0, 10);
}

export function tc39LastDayOfMonth(dateStr: string): string {
  const d = new Date(`${dateStr}T00:00:00Z`);
  d.setUTCMonth(d.getUTCMonth() + 1, 0);
  return d.toISOString().slice(0, 10);
}

export function tc39AddDays(dateStr: string, days: number): string {
  const d = new Date(`${dateStr}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

export function tc39DaysBetween(from: string, to: string): number {
  const a = new Date(`${from}T00:00:00Z`);
  const b = new Date(`${to}T00:00:00Z`);
  return Math.floor((b.getTime() - a.getTime()) / 86_400_000);
}

export function tc39FirstDayOfMonth(dateStr: string): string {
  const d = new Date(`${dateStr}T00:00:00Z`);
  d.setUTCDate(1);
  return d.toISOString().slice(0, 10);
}

// ─── Date / contract helpers (PDT-2815 / PDT-2854 patterns) ─────────────────

export function addDaysIso(isoDate: string, days: number): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().split('T')[0];
}

/** First calendar day of the month after the month containing `isoDate`. */
export function firstDayOfMonthAfter(isoDate: string): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCMonth(d.getUTCMonth() + 1);
  d.setUTCDate(1);
  return d.toISOString().split('T')[0];
}

/** Same calendar day, N months later (Step 10.1 — Wait=No + exact day). */
export function addMonthsSameDay(isoDate: string, months: number): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCMonth(d.getUTCMonth() + months);
  return d.toISOString().split('T')[0];
}

// ─── PHN-362 runtime dates (computed at load — Dev2 validates against today) ─

export const PHN362_TODAY = (() => {
  const d = new Date();
  return d.toISOString().slice(0, 10);
})();

/** Last day of the month after the month containing today (old contract B term end). */
export const PHN362_B_CONTRACT_TERM_END = tc39LastDayOfMonth(tc39AddMonths(PHN362_TODAY, 1));
/** Initial term end — six days before contract term end (Excel 2026-03-25 vs 2026-03-31). */
export const PHN362_B_INITIAL_TERM_END = tc39AddDays(PHN362_B_CONTRACT_TERM_END, -6);
export const PHN362_POD_ACTIVATION = tc39FirstDayOfMonth(PHN362_TODAY);
export const PHN362_B_SIGNING_DATE = tc39AddDays(PHN362_TODAY, -14);
/**
 * Dev2 SIGNED PUT: entryInForceDate must be strictly after today (ProductContractDateService).
 * null + entryIntoForce=SIGNING derives signingDate (past) and fails validateEffectiveEntryInForceDate.
 */
export const PHN362_B_ENTRY_IN_FORCE_DATE = tc39AddDays(PHN362_TODAY, 1);
/** F SIGNED PUT: initial term start must be strictly after entryInForceDate (ProductContractProductParametersService). */
export const PHN362_F_INITIAL_TERM_START = tc39AddDays(PHN362_B_ENTRY_IN_FORCE_DATE, 1);
export const PHN362_V1_START = PHN362_POD_ACTIVATION;
/** V2 starts after V1 so F signing date (today) falls inside V2. */
export const PHN362_V2_START = tc39AddDays(PHN362_POD_ACTIVATION, 1);
export const PHN362_V3_START = tc39AddDays(PHN362_V2_START, 1);
export const PHN362_F_SIGNING_DATE = PHN362_TODAY;
/**
 * Resigning window: F signingDate must be > (termEnd - deadline) and < termEnd.
 * DAY span includes PHN362_TODAY (MONTH/1 would exclude today when term end is next month).
 */
export const PHN362_RESIGNING_DEADLINE_DAYS = tc39DaysBetween(PHN362_TODAY, PHN362_B_CONTRACT_TERM_END) + 1;
/** Dev2 employee (Ketevan Nanuashvili) — contract additionalParameters.employeeId */
export const PHN362_EMPLOYEE_ID = 1283;

/**
 * F signing S for perpetuity-gate suite — after B initial term start, still inside resigning window.
 */
export const PHN362_PERP_SIGNING_S = tc39AddDays(PHN362_POD_ACTIVATION, 2);

export const MSG_RESIGNING_FAILED_ALL_PODS = 'Resigning process failed for all pods';

/** F V1–V3 so perpetuity resign signing S matches the version row (not v1 B_SIGNING_DATE). */
export function perpetuityGateFVersionDates(signingS: string): ContractFVersionDates {
  const v1Start = PHN362_POD_ACTIVATION;
  let v2Start = tc39AddDays(v1Start, 1);
  if (v2Start > signingS) {
    v2Start = signingS;
  }
  if (v2Start <= v1Start) {
    v2Start = tc39AddDays(v1Start, 1);
  }
  return { v1Start, v2Start, v3Start: signingS };
}

/** TC-BE-37: Wait=No → first day of month after F signing date S. */
export const EXPECTED_V37 = {
  fActivation: firstDayOfMonthAfter(PHN362_F_SIGNING_DATE),
  bDeactivation: tc39AddDays(firstDayOfMonthAfter(PHN362_F_SIGNING_DATE), -1),
} as const;

/** TC-BE-38: Wait=Yes + Exact day → initial term end + 1. */
export const EXPECTED_V38 = {
  fActivation: tc39AddDays(PHN362_B_INITIAL_TERM_END, 1),
  bDeactivation: PHN362_B_INITIAL_TERM_END,
} as const;

/** TC-BE-39 / Case 13: Wait=Yes + first day of month after initial term end. */
export const EXPECTED_CASE13 = {
  fActivation: firstDayOfMonthAfter(PHN362_B_INITIAL_TERM_END),
  bDeactivation: tc39AddDays(firstDayOfMonthAfter(PHN362_B_INITIAL_TERM_END), -1),
} as const;

export const PHN362_DYNAMIC_DATES_LOG = {
  PHN362_EMPLOYEE_ID,
  PHN362_TODAY,
  PHN362_POD_ACTIVATION,
  PHN362_B_CONTRACT_TERM_END,
  PHN362_B_INITIAL_TERM_END,
  PHN362_B_SIGNING_DATE,
  PHN362_B_ENTRY_IN_FORCE_DATE,
  PHN362_F_INITIAL_TERM_START,
  PHN362_V1_START,
  PHN362_V2_START,
  PHN362_V3_START,
  PHN362_F_SIGNING_DATE,
  PHN362_RESIGNING_DEADLINE_DAYS,
  EXPECTED_V37,
  EXPECTED_V38,
  EXPECTED_CASE13,
} as const;

/** Fixed dates — `test_cases/Backend/PHN-362_WaitYes_term_end_anchor.md` (split-month defect). */
export const ANCHOR_SPLIT_MONTH_B = {
  initialTermEnd: '2026-02-28',
  contractTermEnd: '2026-03-31',
  bSigning: '2026-01-15',
  podActivation: '2026-03-01',
  fSigning: '2026-03-15',
  fVersions: { v1Start: '2026-01-01', v2Start: '2026-03-01', v3Start: '2026-04-01' },
  resigningDeadlineDays: 30,
} as const;

/** Same calendar month — Confluence and Phoenix both yield 2026-04-01 (control). */
export const ANCHOR_SAME_MONTH_B = {
  initialTermEnd: '2026-03-25',
  contractTermEnd: '2026-03-31',
  bSigning: '2026-01-15',
  podActivation: '2026-03-01',
  fSigning: '2026-03-15',
  fVersions: { v1Start: '2026-01-01', v2Start: '2026-03-01', v3Start: '2026-04-01' },
  resigningDeadlineDays: 30,
} as const;

export function confluenceWaitYesFirstDayFromInitial(initialTermEnd: string): {
  fActivation: string;
  bDeactivation: string;
} {
  const fActivation = firstDayOfMonthAfter(initialTermEnd);
  return { fActivation, bDeactivation: tc39AddDays(fActivation, -1) };
}

export function confluenceWaitYesExactFromInitial(initialTermEnd: string): {
  fActivation: string;
  bDeactivation: string;
  supplyActivationValue: string;
} {
  const fActivation = tc39AddDays(initialTermEnd, 1);
  return { fActivation, bDeactivation: initialTermEnd, supplyActivationValue: fActivation };
}

/** Known Phoenix Dev2 behaviour (contract_term_end anchor) — for attach / debugging only. */
export function phoenixWaitYesFirstDayFromContractTermEnd(contractTermEnd: string): {
  fActivation: string;
  bDeactivation: string;
} {
  const fActivation = firstDayOfMonthAfter(contractTermEnd);
  return { fActivation, bDeactivation: tc39AddDays(fActivation, -1) };
}

/**
 * QA / Excel row — Wait=Yes, supply = exact day (NOT Manual, NOT first day of next month).
 * Maps to Playwright TC-BE-38; aligns with test_cases TC-BE-37 (Wait Yes + exact) in PHN-362_Re-signing.md.
 */
export const PHN362_QA_WAIT_YES_EXACT_CHECKLIST = {
  reSigningProduct: true,
  supplyActivationManual: false,
  customerActiveOtherContract: true,
  productCompatible: true,
  signingDateInResigningWindow: true,
  activePodNotResigned: true,
  futureActivationOtherContractAc4: false,
  podGapFutureVersion: false,
  podActivatableOnNewContract: true,
  waitForOldContractTermExpiresYes: true,
  supplyFirstDayOfNextMonth: false,
  oldContractInitialTermEndSet: true,
  activationWaitYesExactDay: true,
} as const;

/**
 * Excel Case 17 / TC-BE-17 — calculated POD activation outside any old-contract version;
 * AC4 path without user confirmation (removeFuturePods=false).
 */
export const PHN362_QA_CASE17_CHECKLIST = {
  reSigningProduct: true,
  supplyActivationManual: false,
  customerActiveOtherContract: true,
  productCompatible: true,
  signingDateInResigningWindow: true,
  activePodNotResigned: true,
  futureActivationOtherContractAc4: false,
  podGapFutureVersion: false,
  podActivatableOnNewContract: true,
  calculatedActivationInsideOldVersion: false,
  confirmRemoveFutureActivation: false,
} as const;

/** Dev2-safe products — no priceComponents (avoids missing template errors). */
export function productPayloadWithoutPriceComponents(
  GeneratePayload: FixtureGeneratePayload,
  termIndex: number,
): Record<string, unknown> & { priceComponentIds?: unknown[]; priceComponentGroupIds?: unknown[] } {
  const payload = GeneratePayload.productAndServices.product(termIndex) as Record<string, unknown> & {
    priceComponentIds?: unknown[];
    priceComponentGroupIds?: unknown[];
  };
  payload.priceComponentIds = [];
  payload.priceComponentGroupIds = [];
  return payload;
}

export async function loadProductContract(Request: FixtureRequest, contractId: number) {
  const res = await Request.get(`product-contract/${contractId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function versionRow(body: Record<string, unknown>, logicalVersionId: number) {
  const versions = (body.versions ?? []) as { versionId?: number; id?: number; startDate?: string }[];
  return versions.find((v) => Number(v.versionId) === logicalVersionId);
}

export function versionDetailId(body: Record<string, unknown>, logicalVersionId: number): number | null {
  const row = versionRow(body, logicalVersionId);
  return row?.id != null ? Number(row.id) : null;
}

export function headerContractStatus(body: Record<string, unknown>): string | undefined {
  const bp = (body.basicParameters ?? {}) as { status?: string; contractStatus?: string };
  return bp.status ?? bp.contractStatus;
}

export async function productContractStatusUpdate(
  Request: FixtureRequest,
  contractId: number,
  status: string,
  subStatus: string,
  versionId = 1,
) {
  return Request.put(`product-contract/status-update/${contractId}?versionId=${versionId}`, {
    data: {
      contractStatus: status,
      contractSubStatus: subStatus,
      contractVersionStatus: 'SIGNED',
    },
  });
}

export async function buildEditProductContractPayload(
  Request: FixtureRequest,
  contractId: number,
  generatedPayload: Record<string, unknown>,
  opts?: {
    savingAsNewVersion?: boolean;
    startDate?: string;
    preserveSigningDate?: boolean;
    /** When true, omit startDate from PUT (server keeps version start). */
    omitStartDate?: boolean;
    /** Resign sign: force F signing S (do not use GET signingDate from v1 setup). */
    signingDateOverride?: string;
  },
) {
  const contractget = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractget).CheckResponse();
  const contractgetjson = await contractget.json();

  const podsByBillingGroup = new Map<number, { pointOfDeliveryDetailId: number; dealNumber: unknown }[]>();
  for (const pod of contractgetjson.contractPodsResponses ?? []) {
    const billingGroupId = pod.billingGroupId as number;
    if (!podsByBillingGroup.has(billingGroupId)) {
      podsByBillingGroup.set(billingGroupId, []);
    }
    podsByBillingGroup.get(billingGroupId)!.push({
      pointOfDeliveryDetailId: pod.podDetailId,
      dealNumber: pod.dealNumber,
    });
  }

  const podRequests = Array.from(podsByBillingGroup.entries()).map(([billingGroupId, pods]) => ({
    billingGroupId,
    productContractPointOfDeliveries: pods,
  }));

  const allPods = (contractgetjson.contractPodsResponses ?? []).map(
    (pod: { podDetailId: number; dealNumber: unknown }) => ({
      pointOfDeliveryDetailId: pod.podDetailId,
      dealNumber: pod.dealNumber,
    }),
  );

  const serverStartDate = contractgetjson.versions[0].startDate as string;
  const signingDate =
    opts?.signingDateOverride ??
    (opts?.preserveSigningDate
      ? ((contractgetjson.basicParameters?.signingDate as string | null) ?? null)
      : ((contractgetjson.basicParameters?.signingDate as string | null) ?? PHN362_B_SIGNING_DATE));

  const editPayload: Record<string, unknown> = {
    ...generatedPayload,
    basicParameters: {
      ...(generatedPayload.basicParameters as object),
      status: contractgetjson.basicParameters.status,
      subStatus: contractgetjson.basicParameters.subStatus,
      signingDate,
      entryInForceDate: null,
      startOfInitialTerm: null,
    },
    productParameters: {
      ...(generatedPayload.productParameters as object),
      entryIntoForceValue: null,
      startOfContractValue: null,
    },
    productContractPointOfDeliveries: allPods,
    savingAsNewVersion: opts?.savingAsNewVersion ?? false,
    podRequests,
  };

  if (!opts?.omitStartDate) {
    editPayload.startDate = opts?.startDate ?? serverStartDate;
  } else {
    delete editPayload.startDate;
  }

  if (editPayload.additionalParameters) {
    (editPayload.additionalParameters as Record<string, unknown>).riskAssessment = 'PERMIT';
    (editPayload.additionalParameters as Record<string, unknown>).employeeId = PHN362_EMPLOYEE_ID;
  }

  return editPayload;
}

export async function putProductContractNewVersion(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  contractId: number,
  basePayload: Record<string, unknown>,
  fromVersionId: number,
  opts: { startDate?: string; versionStatus?: 'SIGNED' | 'DRAFT' },
) {
  const buildOpts: Parameters<typeof buildEditProductContractPayload>[3] = {
    savingAsNewVersion: true,
    preserveSigningDate: true,
  };
  if (opts.startDate != null) {
    buildOpts.startDate = opts.startDate;
  } else {
    buildOpts.omitStartDate = true;
  }
  const editPayload = await buildEditProductContractPayload(Request, contractId, basePayload, buildOpts);

  const bp = editPayload.basicParameters as Record<string, unknown>;
  bp.versionStatus = opts.versionStatus ?? 'SIGNED';
  if ((opts.versionStatus ?? 'SIGNED') === 'SIGNED') {
    applyFContractSignedPutEntryGuard(editPayload);
  }

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${fromVersionId}&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await expect(put).CheckResponse();

  if (opts.versionStatus !== 'DRAFT') {
    const body = await loadProductContract(Request, contractId);
    const versions = (body.versions ?? []) as { versionId?: number }[];
    const latest = Math.max(...versions.map((v) => Number(v.versionId ?? 0)));
    const signed = await productContractStatusUpdate(
      Request,
      contractId,
      'SIGNED',
      'SIGNED_BY_BOTH_SIDES',
      latest,
    );
    await expect(signed).CheckResponse();
  }
}

export async function resolvePodDetailId(Request: FixtureRequest, Responses: FixtureResponses, podIndex: number) {
  const pod = Responses.pod[podIndex] as { podDetailId?: number; id?: number };
  if (pod.podDetailId != null) return Number(pod.podDetailId);
  const podGet = await Request.get(`pod/${pod.id}`);
  await expect(podGet).CheckResponse();
  const body = await podGet.json();
  return Number(body.podDetailId ?? body.lastPodDetailId ?? body.versions?.[0]?.podDetailId);
}

/** Dev2: additionalParameters.estimatedTotalConsumptionUnderContractKwh = Σ (monthly kWh × 12 / 1000) per CONSUMER POD. */
export async function recomputeEstimatedConsumption(
  Request: FixtureRequest,
  Responses: FixtureResponses,
  contractPayload: Record<string, unknown>,
  podIndexes: number[],
) {
  let annualMwh = 0;
  for (const idx of podIndexes) {
    const podGet = await Request.get(`pod/${Responses.pod[idx].id}?version=1`);
    await expect(podGet).CheckResponse();
    const podJson = await podGet.json();
    if (podJson.type === 'CONSUMER') {
      const monthlyKwh = Number(podJson.estimatedMonthlyAvgConsumption ?? 0);
      annualMwh += (monthlyKwh * 12) / 1000;
    }
  }
  const ap = contractPayload.additionalParameters as Record<string, unknown>;
  ap.estimatedTotalConsumptionUnderContractKwh = annualMwh;
}

export async function activatePodOnContractVersion(
  Request: FixtureRequest,
  Responses: FixtureResponses,
  contractId: number,
  logicalVersionId: number,
  podIndex: number,
  activationDate: string,
) {
  const body = await loadProductContract(Request, contractId);
  const contractDetailId = versionDetailId(body, logicalVersionId);
  expect(contractDetailId, `version ${logicalVersionId} detail id`).toBeTruthy();

  const podId = Number(Responses.pod[podIndex].id);
  const podRow = (
    (body.contractPodsResponses ?? []) as {
      podId?: number;
      identifier?: string;
      podDetailId?: number;
    }[]
  ).find((r) => Number(r.podId) === podId);
  expect(podRow, `POD index ${podIndex} on contract ${contractId}`).toBeTruthy();

  const activation = await Request.post('/contract-pods/manual', {
    data: {
      identifier: podRow!.identifier,
      podDetailId: podRow!.podDetailId,
      contractDetailId,
      activationDate,
      deactivationDate: null,
      deactivationPurposeId: null,
    },
  });
  await expect(activation).CheckResponse();
}

/** Ends supply on B before calculated F activation (Case 17 — activation not inside old version window). */
export async function setPodDeactivationOnContractVersion(
  Request: FixtureRequest,
  Responses: FixtureResponses,
  contractId: number,
  logicalVersionId: number,
  podIndex: number,
  activationDate: string,
  deactivationDate: string,
) {
  const { envVariables } = await import('../../../backend/fixtures/envCashed');
  const body = await loadProductContract(Request, contractId);
  const contractDetailId = versionDetailId(body, logicalVersionId);
  expect(contractDetailId, `version ${logicalVersionId} detail id`).toBeTruthy();

  const podId = Number(Responses.pod[podIndex].id);
  const podRow = (
    (body.contractPodsResponses ?? []) as {
      podId?: number;
      identifier?: string;
      podDetailId?: number;
    }[]
  ).find((r) => Number(r.podId) === podId);
  expect(podRow, `POD index ${podIndex} on contract ${contractId}`).toBeTruthy();

  const activation = await Request.post('/contract-pods/manual', {
    data: {
      identifier: podRow!.identifier,
      podDetailId: podRow!.podDetailId,
      contractDetailId,
      activationDate,
      deactivationDate,
      deactivationPurposeId: envVariables.deactivation_reason,
    },
  });
  await expect(activation).CheckResponse();
}

export function findPodRow(body: Record<string, unknown>, podIdentifier: string) {
  return ((body.contractPodsResponses ?? []) as { identifier?: string }[]).find(
    (r) => r.identifier === podIdentifier,
  );
}

export function initialTermEndFromContractB(body: Record<string, unknown>): string {
  const pp = (body.productParameters ?? {}) as { startOfContractValue?: string };
  const bp = (body.basicParameters ?? {}) as { startOfInitialTerm?: string };
  return (pp.startOfContractValue ?? bp.startOfInitialTerm ?? PHN362_B_INITIAL_TERM_END) as string;
}

/** Mirrors Phoenix `findVersionByContractAndDate` (start_date <= signingDate, latest start). */
export function versionIdForSigningDate(body: Record<string, unknown>, signingDate: string): number {
  const versions = (body.versions ?? []) as { versionId?: number; startDate?: string }[];
  let bestId = 1;
  let bestStart = '';
  for (const v of versions) {
    const start = String(v.startDate ?? '');
    if (start && start <= signingDate && start >= bestStart) {
      bestStart = start;
      bestId = Number(v.versionId ?? bestId);
    }
  }
  return bestId;
}

/** Signing date S must fall in V2+, on or before today (Dev2 rule), and inside B's resigning window. */
export function resolveFSigningDate(contractFBody: Record<string, unknown>): string {
  const v2Start = (versionRow(contractFBody, 2)?.startDate as string | undefined) ?? PHN362_V2_START;
  let s = PHN362_TODAY;
  if (s < v2Start) {
    s = v2Start;
  }
  if (s > PHN362_TODAY) {
    s = PHN362_TODAY;
  }
  const windowStart = tc39AddDays(PHN362_B_CONTRACT_TERM_END, -PHN362_RESIGNING_DEADLINE_DAYS);
  if (s <= windowStart) {
    s = tc39AddDays(windowStart, 1);
  }
  if (s >= PHN362_B_CONTRACT_TERM_END) {
    s = tc39AddDays(PHN362_B_CONTRACT_TERM_END, -1);
  }
  if (s > PHN362_TODAY) {
    s = PHN362_TODAY;
  }
  if (s < v2Start) {
    throw new Error(
      `[PHN-362] signing date ${s} is before V2 start ${v2Start}; adjust version dates or run when today >= V2 start`,
    );
  }
  return s;
}

export function resignedLinkIds(body: Record<string, unknown>, direction: 'from' | 'to'): number[] {
  const bp = (body.basicParameters ?? {}) as {
    resignedFrom?: { id?: number }[];
    resignedTo?: { id?: number }[];
  };
  const list = direction === 'from' ? bp.resignedFrom : bp.resignedTo;
  return (list ?? []).map((x) => Number(x.id)).filter((id) => Number.isFinite(id));
}

// ─── Shared precondition helpers ────────────────────────────────────────────

export async function sharedTermStd(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  resigningDeadlineDays: number = PHN362_RESIGNING_DEADLINE_DAYS,
) {
  const payload = GeneratePayload.productAndServices.term();
  payload.contractEntryIntoForces = ['SIGNING'];
  payload.startsOfContractInitialTerms = ['SIGNING', 'EXACT_DATE'];
  payload.resigningDeadlineType = 'DAY';
  payload.resigningDeadlineValue = resigningDeadlineDays;
  const res = await Request.post(Endpoints.terms, { data: payload });
  await expect(res).CheckResponse();
  Responses.terms.push(await res.json());
}

export async function sharedTermRs(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const payload = GeneratePayload.productAndServices.term();
  payload.contractEntryIntoForces = ['SIGNING'];
  payload.startsOfContractInitialTerms = ['SIGNING', 'EXACT_DATE'];
  payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
  payload.waitForOldContractTermToExpires = ['YES', 'NO'];
  const res = await Request.post(Endpoints.terms, { data: payload });
  await expect(res).CheckResponse();
  Responses.terms.push(await res.json());
}

export async function sharedPrice(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const res = await Request.post(Endpoints.priceComponent, {
    data: GeneratePayload.productAndServices.electricity(),
  });
  await expect(res).CheckResponse();
  Responses.priceComponent.push(await res.json());
}

export async function createProductStd(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const payload = productPayloadWithoutPriceComponents(GeneratePayload, 0);
  payload.termId = Responses.terms[0].id;
  payload.contractTypes = ['SUPPLY_ONLY'];
  payload.paymentGuarantees = ['NO'];
  const res = await Request.post(Endpoints.product, { data: payload });
  await expect(res).CheckResponse();
  const json = await res.json();
  Responses.product.push(typeof json === 'number' ? json : json.id);
}

export async function createCustomerA(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const res = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_private(),
  });
  await expect(res).CheckResponse();
  Responses.customer.push(await res.json());
}

export async function createCustomerLegal_TC39(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const res = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(res).CheckResponse();
  Responses.customer.push(await res.json());
}

export async function createPodsAbc(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  for (let i = 0; i < 3; i++) {
    const res = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(res).CheckResponse();
    Responses.pod.push(await res.json());
  }
}

/**
 * Old contract B (SUPPLY_ONLY, Dev2-safe sign PUT).
 * Runtime dates from PHN362_*; EXACT_DATE initial term on sign PUT + SIGNING entry into force.
 */
export async function createOldContractB(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
): Promise<{ contractId: number; contractPayload: Record<string, unknown>; podIdentifier: string }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_STD,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;
  bp.startOfInitialTerm = null;
  bp.entryInForceDate = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;
  pp.supplyActivation = 'MANUAL';
  pp.supplyActivationValue = null;
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.startOfContractValue = null;

  const podDetailId = await resolvePodDetailId(Request, Responses, IDX_POD_A);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  await recomputeEstimatedConsumption(Request, Responses, contractPayload, [IDX_POD_A]);

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  await test.step('Contract B: sign + initial term end on PUT [TC-BE-10/18]', async () => {
    const signPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
      preserveSigningDate: false,
      startDate: PHN362_POD_ACTIVATION,
    });
    const signBp = signPayload.basicParameters as Record<string, unknown>;
    signBp.status = 'SIGNED';
    signBp.subStatus = 'SIGNED_BY_BOTH_SIDES';
    signBp.versionStatus = 'SIGNED';
    signBp.signingDate = PHN362_B_SIGNING_DATE;
    signBp.entryInForceDate = PHN362_B_ENTRY_IN_FORCE_DATE;
    signBp.startOfInitialTerm = PHN362_B_INITIAL_TERM_END;

    const signPp = signPayload.productParameters as Record<string, unknown>;
    signPp.contractType = 'SUPPLY_ONLY';
    signPp.entryIntoForce = 'SIGNING';
    signPp.entryIntoForceValue = null;
    signPp.startOfContractInitialTerm = 'EXACT_DATE';
    signPp.startOfContractValue = PHN362_B_INITIAL_TERM_END;
    signPp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;
    signPp.supplyActivation = 'MANUAL';
    signPp.supplyActivationValue = null;

    const ap = signPayload.additionalParameters as Record<string, unknown> | undefined;
    if (ap) {
      ap.employeeId = PHN362_EMPLOYEE_ID;
      ap.riskAssessment = 'PERMIT';
    }

    const put = await Request.put(
      `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: signPayload },
    );
    await expect(put).CheckResponse();

    await activatePodOnContractVersion(Request, Responses, contractId, 1, IDX_POD_A, PHN362_POD_ACTIVATION);
  });

  await test.step('Contract B: ACTIVE_IN_TERM [TC-BE-10]', async () => {
    const body = await loadProductContract(Request, contractId);
    const header = headerContractStatus(body);
    expect(
      header === 'ACTIVE_IN_TERM' ||
        header === 'ENTERED_INTO_FORCE' ||
        header === 'ACTIVE_IN_PERPETUITY',
      `contract B header after POD activation (got ${header})`,
    ).toBeTruthy();
  });

  const podGet = await Request.get(`pod/${Responses.pod[IDX_POD_A].id}`);
  await expect(podGet).CheckResponse();
  const podIdentifier = String((await podGet.json()).identifier);

  return { contractId, contractPayload, podIdentifier };
}

export type AnchorContractBDates = {
  initialTermEnd: string;
  contractTermEnd: string;
  bSigningDate: string;
  podActivation: string;
};

/**
 * Old contract B with explicit initial / contract term ends (term-end anchor suite).
 */
export async function createOldContractBWithAnchorDates(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  dates: AnchorContractBDates,
): Promise<{ contractId: number; contractPayload: Record<string, unknown>; podIdentifier: string }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_STD,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.contractTermEndDate = dates.contractTermEnd;
  bp.startOfInitialTerm = null;
  bp.entryInForceDate = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.contractTermEndDate = dates.contractTermEnd;
  pp.supplyActivation = 'MANUAL';
  pp.supplyActivationValue = null;
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.startOfContractValue = null;

  const podDetailId = await resolvePodDetailId(Request, Responses, IDX_POD_A);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  await recomputeEstimatedConsumption(Request, Responses, contractPayload, [IDX_POD_A]);

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  await test.step(
    `Contract B: sign (initial ${dates.initialTermEnd}, term end ${dates.contractTermEnd}) [anchor]`,
    async () => {
      const signPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
        preserveSigningDate: false,
        startDate: dates.podActivation,
      });
      const signBp = signPayload.basicParameters as Record<string, unknown>;
      signBp.status = 'SIGNED';
      signBp.subStatus = 'SIGNED_BY_BOTH_SIDES';
      signBp.versionStatus = 'SIGNED';
      signBp.signingDate = dates.bSigningDate;
      signBp.entryInForceDate = PHN362_B_ENTRY_IN_FORCE_DATE;
      signBp.startOfInitialTerm = dates.initialTermEnd;

      const signPp = signPayload.productParameters as Record<string, unknown>;
      signPp.contractType = 'SUPPLY_ONLY';
      signPp.entryIntoForce = 'SIGNING';
      signPp.entryIntoForceValue = null;
      signPp.startOfContractInitialTerm = 'EXACT_DATE';
      signPp.startOfContractValue = dates.initialTermEnd;
      signPp.contractTermEndDate = dates.contractTermEnd;
      signPp.supplyActivation = 'MANUAL';
      signPp.supplyActivationValue = null;

      const ap = signPayload.additionalParameters as Record<string, unknown> | undefined;
      if (ap) {
        ap.employeeId = PHN362_EMPLOYEE_ID;
        ap.riskAssessment = 'PERMIT';
      }

      const put = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
        { data: signPayload },
      );
      await expect(put).CheckResponse();

      await activatePodOnContractVersion(
        Request,
        Responses,
        contractId,
        1,
        IDX_POD_A,
        dates.podActivation,
      );
    },
  );

  await test.step('Contract B: ACTIVE_IN_TERM [anchor]', async () => {
    const body = await loadProductContract(Request, contractId);
    const header = headerContractStatus(body);
    expect(
      header === 'ACTIVE_IN_TERM' ||
        header === 'ENTERED_INTO_FORCE' ||
        header === 'ACTIVE_IN_PERPETUITY',
      `contract B header after POD activation (got ${header})`,
    ).toBeTruthy();
  });

  const podGet = await Request.get(`pod/${Responses.pod[IDX_POD_A].id}`);
  await expect(podGet).CheckResponse();
  const podIdentifier = String((await podGet.json()).identifier);

  return { contractId, contractPayload, podIdentifier };
}

/**
 * Old contract B (ACTIVE_IN_PERPETUITY + perpetuity date) for diagram 10.2.1.
 * Perpetuity must be after initial term start and on/before today; resign gate compares to F signing S.
 */
export async function createOldContractBInPerpetuity(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  perpetuityDate: string,
): Promise<{ contractId: number; contractPayload: Record<string, unknown>; podIdentifier: string }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_STD,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;
  bp.startOfInitialTerm = null;
  bp.entryInForceDate = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;
  pp.supplyActivation = 'MANUAL';
  pp.supplyActivationValue = null;
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.startOfContractValue = null;

  const podDetailId = await resolvePodDetailId(Request, Responses, IDX_POD_A);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  await recomputeEstimatedConsumption(Request, Responses, contractPayload, [IDX_POD_A]);

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  await test.step('Contract B: sign + initial term end [perpetuity suite]', async () => {
    const signPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
      preserveSigningDate: false,
      startDate: PHN362_POD_ACTIVATION,
    });
    const signBp = signPayload.basicParameters as Record<string, unknown>;
    signBp.status = 'SIGNED';
    signBp.subStatus = 'SIGNED_BY_BOTH_SIDES';
    signBp.versionStatus = 'SIGNED';
    signBp.signingDate = PHN362_B_SIGNING_DATE;
    signBp.entryInForceDate = PHN362_B_ENTRY_IN_FORCE_DATE;
    signBp.startOfInitialTerm = PHN362_POD_ACTIVATION;

    const signPp = signPayload.productParameters as Record<string, unknown>;
    signPp.contractType = 'SUPPLY_ONLY';
    signPp.entryIntoForce = 'SIGNING';
    signPp.entryIntoForceValue = null;
    signPp.startOfContractInitialTerm = 'EXACT_DATE';
    signPp.startOfContractValue = PHN362_POD_ACTIVATION;
    signPp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;
    signPp.supplyActivation = 'MANUAL';
    signPp.supplyActivationValue = null;

    const ap = signPayload.additionalParameters as Record<string, unknown> | undefined;
    if (ap) {
      ap.employeeId = PHN362_EMPLOYEE_ID;
      ap.riskAssessment = 'PERMIT';
    }

    const put = await Request.put(
      `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: signPayload },
    );
    await expect(put).CheckResponse();
    await activatePodOnContractVersion(Request, Responses, contractId, 1, IDX_POD_A, PHN362_POD_ACTIVATION);
  });

  await test.step(
    `Contract B: ACTIVE_IN_PERPETUITY + perpetuity ${perpetuityDate} [diagram 10.2.1]`,
    async () => {
      expect(perpetuityDate > PHN362_B_SIGNING_DATE).toBeTruthy();
      expect(perpetuityDate <= PHN362_TODAY).toBeTruthy();

      const promote = await productContractStatusUpdate(
        Request,
        contractId,
        'ACTIVE_IN_PERPETUITY',
        'DELIVERY',
        1,
      );
      await expect(promote).CheckResponse();

      const loaded = await loadProductContract(Request, contractId);
      const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
        preserveSigningDate: true,
        startDate: PHN362_POD_ACTIVATION,
      });
      const editBp = editPayload.basicParameters as Record<string, unknown>;
      editBp.status = 'ACTIVE_IN_PERPETUITY';
      editBp.subStatus = 'DELIVERY';
      editBp.perpetuityDate = perpetuityDate;
      editBp.signingDate = PHN362_B_SIGNING_DATE;
      editBp.entryInForceDate = PHN362_B_SIGNING_DATE;
      editBp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;

      const editPp = editPayload.productParameters as Record<string, unknown>;
      editPp.entryIntoForce = 'SIGNING';
      editPp.entryIntoForceValue = null;
      editPp.startOfContractInitialTerm = 'EXACT_DATE';
      editPp.startOfContractValue = PHN362_POD_ACTIVATION;
      editPp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;

      const put = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
        { data: editPayload },
      );
      await expect(put).CheckResponse();
      expect(headerContractStatus(await loadProductContract(Request, contractId))).toBe(
        'ACTIVE_IN_PERPETUITY',
      );
    },
  );

  const podGet = await Request.get(`pod/${Responses.pod[IDX_POD_A].id}`);
  await expect(podGet).CheckResponse();
  const podIdentifier = String((await podGet.json()).identifier);

  return { contractId, contractPayload, podIdentifier };
}

/**
 * Diagram 10.2.1 gate: B.perpetuityDate empty OR > F.signingDate (S).
 * Not compared: entryInForceDate, B.signingDate vs perpetuity (unless coincidentally equal to S).
 */
export function perpetuityGatePasses(perpetuityOnB: string | null | undefined, signingDateS: string): boolean {
  return perpetuityOnB == null || perpetuityOnB === '' || perpetuityOnB > signingDateS;
}

/**
 * Before blocked sign: all resign preconditions except diagram perpetuity gate (perp not > S).
 * `availableForResigning` applies to the **old** contract (B), not draft/signed F — see
 * ProductContractDetailsRepository.findAvailableForResigningByProductContractDetailId.
 */
export async function assertResignPreconditionsExceptPerpetuityGate(
  Request: FixtureRequest,
  contractBId: number,
  contractFId: number,
  signingS: string,
  perpetuityOnB: string,
  podIdentifier: string,
) {
  const bBody = await loadProductContract(Request, contractBId);
  const fBody = await loadProductContract(Request, contractFId);
  const bBp = bBody.basicParameters as Record<string, unknown>;

  expect(headerContractStatus(bBody)).toBe('ACTIVE_IN_PERPETUITY');
  expect(bBp.perpetuityDate).toBe(perpetuityOnB);
  expect(perpetuityGatePasses(perpetuityOnB, signingS)).toBe(false);

  const bAvailable = (bBody as { availableForResigning?: boolean }).availableForResigning;
  expect(
    bAvailable,
    'B (old contract) must be availableForResigning — otherwise resign fails before perpetuity gate',
  ).toBe(true);

  const podRow = findPodRow(bBody, podIdentifier) as { activationDate?: string };
  expect(podRow?.activationDate, 'POD on B must be activated for resign preconditions').toBeTruthy();

  const fBp = fBody.basicParameters as { status?: string; subStatus?: string };
  expect(['DRAFT', 'SIGNED', 'READY']).toContain(fBp.status);
  // Wait=YES is applied on F sign PUT via thirdTab (createNewContractFWithVersions defaults to NO on GET).
}

export function perpetuityDateForRelation(
  relation: 'greater_than_signing' | 'equal_to_signing' | 'less_than_signing',
  signingDateS: string,
): string {
  if (relation === 'greater_than_signing') {
    const d = PHN362_TODAY;
    expect(d > signingDateS).toBeTruthy();
    expect(d > PHN362_POD_ACTIVATION).toBeTruthy();
    return d;
  }
  if (relation === 'equal_to_signing') {
    expect(signingDateS > PHN362_POD_ACTIVATION).toBeTruthy();
    expect(signingDateS <= PHN362_TODAY).toBeTruthy();
    return signingDateS;
  }
  const d = tc39AddDays(signingDateS, -1);
  expect(d > PHN362_B_SIGNING_DATE).toBeTruthy();
  expect(d < signingDateS).toBeTruthy();
  return d;
}

export function waitYesExpectedActivation(
  supply: 'FIRST_DAY_OF_MONTH' | 'EXACT_DATE',
  bInitialEnd: string,
  /** Phoenix Wait=Yes + FIRST_DAY uses `contract_term_end_date`, not initial term end. */
  contractTermEndForWaitYesFirstDay?: string,
): { fActivation: string; bDeactivation: string; supplyActivationValue?: string } {
  if (supply === 'FIRST_DAY_OF_MONTH') {
    const anchor = contractTermEndForWaitYesFirstDay ?? bInitialEnd;
    const fActivation = firstDayOfMonthAfter(anchor);
    return { fActivation, bDeactivation: tc39AddDays(fActivation, -1) };
  }
  const fActivation = tc39AddDays(bInitialEnd, 1);
  return { fActivation, bDeactivation: bInitialEnd, supplyActivationValue: fActivation };
}

/**
 * F contract SIGNED PUT: Dev2 requires entryInForceDate strictly after today when status=SIGNED.
 * SIGNING + null derives signingDate (often past) and fails validateEffectiveEntryInForceDate.
 */
export function applyFContractSignedPutEntryGuard(editPayload: Record<string, unknown>) {
  const bp = editPayload.basicParameters as Record<string, unknown>;
  bp.entryInForceDate = PHN362_B_ENTRY_IN_FORCE_DATE;
  bp.startOfInitialTerm = PHN362_F_INITIAL_TERM_START;
  const pp = editPayload.productParameters as Record<string, unknown>;
  if (pp.entryIntoForce !== 'MANUAL') {
    pp.entryIntoForce = 'SIGNING';
    pp.entryIntoForceValue = null;
    pp.startOfContractInitialTerm = 'EXACT_DATE';
    pp.startOfContractValue = PHN362_F_INITIAL_TERM_START;
  }
}

/** SIGNED sign PUT: omit entry fields entirely (null still fails if spread from GET). */
export function omitEntryInForceFieldsOnSignPayload(editPayload: Record<string, unknown>) {
  applyFContractSignedPutEntryGuard(editPayload);
  const bp = editPayload.basicParameters as Record<string, unknown>;
  delete bp.entryInForceDate;
  const pp = editPayload.productParameters as Record<string, unknown>;
  delete pp.entryIntoForceValue;
  delete pp.startOfContractValue;
}

export type SanitizeFResignSignOpts = { blockedPerpetuityGate?: boolean };

/**
 * Last line of defense before F resign sign PUT — future entryInForceDate + SIGNING on third tab.
 * blockedPerpetuityGate: same shape; F version dates must align with signing S (see perpetuityGateFVersionDates).
 */
export function sanitizeFResignSignPutPayload(
  editPayload: Record<string, unknown>,
  signingDate: string,
  _opts?: SanitizeFResignSignOpts,
) {
  const bp = editPayload.basicParameters as Record<string, unknown>;
  const pp = editPayload.productParameters as Record<string, unknown>;

  bp.status = 'SIGNED';
  bp.subStatus = 'SIGNED_BY_BOTH_SIDES';
  bp.versionStatus = 'SIGNED';
  bp.signingDate = signingDate;
  bp.entryInForceDate = PHN362_B_ENTRY_IN_FORCE_DATE;
  bp.startOfInitialTerm = PHN362_F_INITIAL_TERM_START;
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'EXACT_DATE';
  pp.startOfContractValue = PHN362_F_INITIAL_TERM_START;
}

export function applySignFResignPutFields(
  editPayload: Record<string, unknown>,
  thirdTab: ResignThirdTabConfig,
  signingDate: string,
  sanitizeOpts?: SanitizeFResignSignOpts,
) {
  const pp = editPayload.productParameters as Record<string, unknown>;
  pp.supplyActivation = thirdTab.supplyActivation;
  pp.productContractWaitForOldContractTermToExpires = thirdTab.waitForOldContractTermToExpires;
  pp.supplyActivationValue = thirdTab.supplyActivationValue ?? null;
  sanitizeFResignSignPutPayload(editPayload, signingDate, sanitizeOpts);
}

export async function signContractFExpectResignBlocked(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  contractId: number,
  basePayload: Record<string, unknown>,
  thirdTab: ResignThirdTabConfig,
  signOpts: SignContractFOptions = {},
) {
  const fBodyBeforeSign = await loadProductContract(Request, contractId);
  const signingDate = signOpts.signingDate ?? resolveFSigningDate(fBodyBeforeSign);
  const matchedVersion =
    signOpts.matchedVersion ?? versionIdForSigningDate(fBodyBeforeSign, signingDate);

  const editPayload = await buildEditProductContractPayload(Request, contractId, basePayload, {
    preserveSigningDate: true,
    omitStartDate: true,
    signingDateOverride: signingDate,
  });

  const sanitizeOpts: SanitizeFResignSignOpts = {
    blockedPerpetuityGate: signOpts.blockedPerpetuityGate,
  };
  applySignFResignPutFields(editPayload, thirdTab, signingDate, sanitizeOpts);
  editPayload.removeFuturePods = signOpts.removeFuturePods ?? true;
  sanitizeFResignSignPutPayload(editPayload, signingDate, sanitizeOpts);
  if (signOpts.omitEntryInForceOnSign) {
    omitEntryInForceFieldsOnSignPayload(editPayload);
  }
  const putBody = JSON.parse(JSON.stringify(editPayload)) as Record<string, unknown>;
  if (signOpts.omitEntryInForceOnSign) {
    const putBp = putBody.basicParameters as Record<string, unknown> | undefined;
    expect(putBp != null && !('entryInForceDate' in putBp)).toBeTruthy();
  }

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${matchedVersion}&changeFutureVersionsPods=false`,
    { data: putBody },
  );
  const status = put.status();
  const text = await put.text();
  let body: Record<string, unknown> = {};
  try {
    body = JSON.parse(text) as Record<string, unknown>;
  } catch {
    /* non-JSON error body */
  }

  const haystack = `${text} ${JSON.stringify(body)}`.toLowerCase();
  expect(
    haystack.includes('entry in force should be in future'),
    `Sign PUT must not fail on entryInForceDate validation. Response: ${text}`,
  ).toBeFalsy();

  if (signOpts.blockedPerpetuityGate) {
    if (status >= 400) {
      const resignOrGateBlocked =
        haystack.includes(MSG_RESIGNING_FAILED_ALL_PODS.toLowerCase()) ||
        haystack.includes('resigning process failed') ||
        haystack.includes('re-signing') ||
        haystack.includes('only for re-signing');
      expect(resignOrGateBlocked, `Expected perpetuity/resign block, got: ${text}`).toBeTruthy();
    }
    return;
  }

  if (status >= 400) {
    expect(haystack.includes(MSG_RESIGNING_FAILED_ALL_PODS.toLowerCase())).toBeTruthy();
  } else {
    const messages = (body.resigningMessages ?? []) as string[];
    expect(
      messages.some((m) => String(m).includes(MSG_RESIGNING_FAILED_ALL_PODS)),
    ).toBeTruthy();
  }
}

/**
 * Excel Case 17 — sign F without AC4 confirmation; expect block + no resign state.
 * Accepts FUTURE_ACTIVATION_NOT_CONFIRMED when API exposes it; else OPERATION_NOT_ALLOWED / all pods failed.
 */
export async function signContractFExpectCase17Ac4Blocked(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  contractId: number,
  basePayload: Record<string, unknown>,
  thirdTab: ResignThirdTabConfig,
  signOpts: SignContractFOptions = {},
) {
  const fBodyBeforeSign = await loadProductContract(Request, contractId);
  const signingDate = signOpts.signingDate ?? resolveFSigningDate(fBodyBeforeSign);
  const matchedVersion =
    signOpts.matchedVersion ?? versionIdForSigningDate(fBodyBeforeSign, signingDate);

  const editPayload = await buildEditProductContractPayload(Request, contractId, basePayload, {
    preserveSigningDate: true,
    omitStartDate: true,
    signingDateOverride: signingDate,
  });

  applySignFResignPutFields(editPayload, thirdTab, signingDate);
  editPayload.removeFuturePods = false;
  sanitizeFResignSignPutPayload(editPayload, signingDate);
  if (signOpts.omitEntryInForceOnSign !== false) {
    omitEntryInForceFieldsOnSignPayload(editPayload);
  }
  const putBody = JSON.parse(JSON.stringify(editPayload)) as Record<string, unknown>;

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${matchedVersion}&changeFutureVersionsPods=false`,
    { data: putBody },
  );

  await assertCase17SignBlocked(put, { contractId });
}

export async function assertCase17SignBlocked(
  response: { status: () => number; text: () => Promise<string> },
  _ctx: { contractId: number },
) {
  const status = response.status();
  const text = await response.text();
  let body: Record<string, unknown> = {};
  try {
    body = JSON.parse(text) as Record<string, unknown>;
  } catch {
    /* non-JSON */
  }

  const haystack = `${text} ${JSON.stringify(body)}`.toLowerCase();
  expect(
    haystack.includes('entry in force should be in future'),
    `Sign PUT must not fail on entryInForceDate: ${text.slice(0, 500)}`,
  ).toBeFalsy();

  const errorCode = String(body.errorCode ?? '');
  const conflicts = extractAc4ConflictRows(body);
  const messages = (body.resigningMessages ?? []) as string[];

  if (errorCode.includes('FUTURE_ACTIVATION_NOT_CONFIRMED')) {
    expect([409, 422]).toContain(status);
    if (conflicts.length > 0) {
      for (const row of conflicts) {
        expect(row.contractId != null || row.contractNumber != null).toBeTruthy();
        expect(row.podId != null).toBeTruthy();
        expect(row.fromDate != null).toBeTruthy();
      }
    }
    return;
  }

  if (status >= 400) {
    expect([400, 403, 409, 422]).toContain(status);
    const blocked =
      haystack.includes(MSG_RESIGNING_FAILED_ALL_PODS.toLowerCase()) ||
      errorCode.includes('OPERATION_NOT_ALLOWED') ||
      haystack.includes('operation_not_allowed');
    expect(
      blocked,
      `Case 17: expected resign block (409/422 or all pods failed), status=${status} body=${text.slice(0, 700)}`,
    ).toBeTruthy();
  } else {
    expect(
      messages.some((m) => String(m).includes(MSG_RESIGNING_FAILED_ALL_PODS)),
      `Case 17: expected resigningMessages when HTTP 200, body=${text.slice(0, 700)}`,
    ).toBeTruthy();
  }

  test.info().annotations.push({
    type: 'case17-api',
    description:
      conflicts.length > 0
        ? `structured AC4 conflicts (${conflicts.length})`
        : errorCode || 'resign-all-pods (FUTURE_ACTIVATION_NOT_CONFIRMED not in response yet)',
  });
}

export async function assertCase17NoResignState(
  Request: FixtureRequest,
  contractFId: number,
  contractBId: number,
  podIdentifier: string,
  expectedCalculatedActivation: string,
) {
  await test.step('Assert: no resign links F↔B [TC-BE-17]', async () => {
    const fBody = await loadProductContract(Request, contractFId);
    const bBody = await loadProductContract(Request, contractBId);
    const fFrom = resignedLinkIds(fBody, 'from');
    const bTo = resignedLinkIds(bBody, 'to');
    expect(fFrom, 'F must not link resignedFrom to B').not.toContain(contractBId);
    expect(bTo, 'B must not link resignedTo to F').not.toContain(contractFId);
  });

  await test.step('Assert: POD on F not activated at calculated date [TC-BE-17]', async () => {
    const fBody = await loadProductContract(Request, contractFId);
    const fPod = findPodRow(fBody, podIdentifier) as { activationDate?: string | null };
    expect(fPod?.activationDate ?? null).not.toBe(expectedCalculatedActivation);
  });

  await assertPodNotResignedOnB(Request, contractBId, podIdentifier);
}

export async function assertPodNotResignedOnB(
  Request: FixtureRequest,
  contractBId: number,
  podIdentifier: string,
) {
  const bBody = await loadProductContract(Request, contractBId);
  const row = findPodRow(bBody, podIdentifier) as { isResigned?: boolean };
  expect(row?.isResigned).not.toBe(true);
}

export async function createProductRs(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const payload = productPayloadWithoutPriceComponents(GeneratePayload, 1);
  payload.termId = Responses.terms[1].id;
  payload.contractTypes = ['SUPPLY_ONLY'];
  payload.paymentGuarantees = ['NO'];
  (payload as Record<string, unknown>).isResigning = true;
  (payload as Record<string, unknown>).resignProductTargets = [Responses.product[IDX_PRODUCT_STD]];

  const productRes = await Request.post(Endpoints.product, { data: payload });
  await expect(productRes).CheckResponse();
  const productJson = await productRes.json();
  Responses.product.push(typeof productJson === 'number' ? productJson : productJson.id);

  const specialOfferPayload = (await GeneratePayload.productAndServices.specialOffersTab(false)) as Record<
    string,
    unknown
  >;
  specialOfferPayload.applyToSpecificCustomers = false;
  specialOfferPayload.customers = [];
  specialOfferPayload.contracts = [];
  specialOfferPayload.resignable = true;
  specialOfferPayload.resigningProducts = [
    {
      productId: Responses.product[IDX_PRODUCT_STD],
      versionIds: [1],
    },
  ];

  const specialOffersRes = await Request.put(
    `products/${Responses.product[IDX_PRODUCT_RS]}/special-offers?version=1`,
    { data: specialOfferPayload },
  );
  await expect(specialOffersRes).CheckResponse();
}

export type ContractFVersionDates = {
  v1Start: string;
  v2Start: string;
  v3Start: string;
};

export async function createNewContractFWithVersions(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  versionDates: ContractFVersionDates = {
    v1Start: PHN362_V1_START,
    v2Start: PHN362_V2_START,
    v3Start: PHN362_V3_START,
  },
  fV1SigningDate: string = PHN362_B_SIGNING_DATE,
): Promise<{ contractId: number; contractPayload: Record<string, unknown> }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_RS,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.entryInForceDate = null;
  bp.startOfInitialTerm = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.startOfContractValue = null;
  pp.supplyActivation = 'FIRST_DAY_OF_MONTH';
  pp.supplyActivationValue = null;
  pp.productContractWaitForOldContractTermToExpires = 'NO';

  const podDetailId = await resolvePodDetailId(Request, Responses, IDX_POD_A);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  await recomputeEstimatedConsumption(Request, Responses, contractPayload, [IDX_POD_A]);

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  await test.step('Contract F: v1 SIGNED [TC-BE-5]', async () => {
    const ready = await productContractStatusUpdate(Request, contractId, 'READY', 'READY', 1);
    await expect(ready).CheckResponse();
    const signed = await productContractStatusUpdate(
      Request,
      contractId,
      'SIGNED',
      'SIGNED_BY_BOTH_SIDES',
      1,
    );
    await expect(signed).CheckResponse();
    const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
      preserveSigningDate: true,
      startDate: versionDates.v1Start,
    });
    const editBp = editPayload.basicParameters as Record<string, unknown>;
    editBp.signingDate = fV1SigningDate;
    applyFContractSignedPutEntryGuard(editPayload);
    const put = await Request.put(
      `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: editPayload },
    );
    await expect(put).CheckResponse();
  });

  await test.step('Contract F: v2 SIGNED (S falls in V2) [TC-BE-5]', async () => {
    const v1Start = versionDates.v1Start;
    let v2Start = versionDates.v2Start;
    if (v2Start <= v1Start) {
      v2Start = tc39AddDays(v1Start, 1);
    }
    if (v2Start > PHN362_TODAY) {
      v2Start = PHN362_TODAY;
    }
    if (v2Start <= v1Start) {
      v2Start = tc39AddDays(v1Start, 1);
    }
    await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 1, {
      startDate: v2Start,
      versionStatus: 'SIGNED',
    });
  });

  await test.step('Contract F: v3 SIGNED (POD in all versions) [TC-BE-36]', async () => {
    const bodyBeforeV3 = await loadProductContract(Request, contractId);
    const v2Start =
      (versionRow(bodyBeforeV3, 2)?.startDate as string | undefined) ?? versionDates.v2Start;
    const v3Start = tc39AddDays(v2Start, 1);
    await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 2, {
      startDate: v3Start,
      versionStatus: 'SIGNED',
    });
  });

  return { contractId, contractPayload };
}

// ─── TC-BE-39 inline helpers (unused by main happy-path tests; kept for reference) ─

export async function sharedTermStd_TC39(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const payload = GeneratePayload.productAndServices.term();
  payload.contractEntryIntoForces = ['MANUAL', 'SIGNING'];
  payload.startsOfContractInitialTerms = ['MANUAL', 'SIGNING'];
  payload.supplyActivations = ['MANUAL', 'FIRST_DAY_OF_MONTH'];
  payload.resigningDeadlineType = 'DAY';
  payload.resigningDeadlineValue = PHN362_RESIGNING_DEADLINE_DAYS;
  const res = await Request.post(Endpoints.terms, { data: payload });
  await expect(res).CheckResponse();
  Responses.terms.push(await res.json());
}

export async function sharedTermRs_TC39(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const payload = GeneratePayload.productAndServices.term();
  payload.contractEntryIntoForces = ['MANUAL'];
  payload.startsOfContractInitialTerms = ['MANUAL'];
  payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
  payload.waitForOldContractTermToExpires = ['YES'];
  const res = await Request.post(Endpoints.terms, { data: payload });
  await expect(res).CheckResponse();
  Responses.terms.push(await res.json());
}

export async function createProductStd_TC39(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const payload = productPayloadWithoutPriceComponents(GeneratePayload, 0);
  payload.termId = Responses.terms[0].id;
  payload.contractTypes = ['WITHOUT_SUPPLY'];
  payload.paymentGuarantees = ['NO'];
  const res = await Request.post(Endpoints.product, { data: payload });
  await expect(res).CheckResponse();
  const json = await res.json();
  Responses.product.push(typeof json === 'number' ? json : json.id);
}

export async function createProductRs_TC39(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const payload = productPayloadWithoutPriceComponents(GeneratePayload, 1);
  payload.termId = Responses.terms[1].id;
  payload.contractTypes = ['WITHOUT_SUPPLY'];
  payload.paymentGuarantees = ['NO'];
  (payload as Record<string, unknown>).isResigning = true;
  (payload as Record<string, unknown>).resignProductTargets = [Responses.product[IDX_PRODUCT_STD]];

  console.log('[TC-BE-39] P-RS product POST:', {
    priceComponentIds: payload.priceComponentIds,
    termId: payload.termId,
    contractTypes: payload.contractTypes,
    productStdIndex: IDX_PRODUCT_STD,
  });

  const productRes = await Request.post(Endpoints.product, { data: payload });
  await expect(productRes).CheckResponse();
  const productJson = await productRes.json();
  Responses.product.push(typeof productJson === 'number' ? productJson : productJson.id);

  const specialOfferPayload = (await GeneratePayload.productAndServices.specialOffersTab(false)) as Record<
    string,
    unknown
  >;
  specialOfferPayload.applyToSpecificCustomers = false;
  specialOfferPayload.customers = [];
  specialOfferPayload.contracts = [];
  specialOfferPayload.resignable = true;

  delete specialOfferPayload.resigningProducts;
  specialOfferPayload.resigningProducts = [
    {
      productId: Responses.product[IDX_PRODUCT_STD],
      versionIds: [1],
    },
  ];

  const specialOffersRes = await Request.put(
    `products/${Responses.product[IDX_PRODUCT_RS]}/special-offers?version=1`,
    { data: specialOfferPayload },
  );
  await expect(specialOffersRes).CheckResponse();
}

export async function createOldContractBManual_TC39(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
): Promise<{ contractId: number; contractPayload: Record<string, unknown>; podIdentifier: string }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_STD,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;
  bp.startOfInitialTerm = null;
  bp.entryInForceDate = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'WITHOUT_SUPPLY';
  pp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;
  pp.supplyActivation = 'MANUAL';
  pp.supplyActivationValue = null;

  const podDetailId = await resolvePodDetailId(Request, Responses, IDX_POD_A);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const podGet = await Request.get(`pod/${Responses.pod[IDX_POD_A].id}?version=1`);
  await expect(podGet).CheckResponse();
  const podJson = await podGet.json();
  const monthlyKwh = podJson.type === 'CONSUMER' ? Number(podJson.estimatedMonthlyAvgConsumption ?? 0) : 0;
  const annualMwh = (monthlyKwh * 12) / 1000;
  (contractPayload.additionalParameters as Record<string, unknown>).estimatedTotalConsumptionUnderContractKwh =
    annualMwh;
  console.log('[TC-BE-39] contract B POST:', {
    pointOfDeliveryDetailId: podDetailId,
    monthlyKwh,
    annualMwh,
    contractTermEndDate: PHN362_B_CONTRACT_TERM_END,
    ...PHN362_DYNAMIC_DATES_LOG,
  });

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  await test.step('Contract B (TC-BE-39 inline): sign + activate POD a [TC-BE-10/18]', async () => {
    const signPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
      preserveSigningDate: false,
    });
    const signBp = signPayload.basicParameters as Record<string, unknown>;
    signBp.status = 'SIGNED';
    signBp.subStatus = 'SIGNED_BY_BOTH_SIDES';
    signBp.versionStatus = 'SIGNED';
    signBp.signingDate = PHN362_B_SIGNING_DATE;

    const signPp = signPayload.productParameters as Record<string, unknown>;
    signPp.entryIntoForce = 'SIGNING';
    signPp.entryIntoForceValue = null;
    signPp.startOfContractInitialTerm = 'SIGNING';
    signPp.startOfContractValue = null;
    signPp.contractTermEndDate = PHN362_B_CONTRACT_TERM_END;
    signPp.supplyActivation = 'MANUAL';
    signPp.supplyActivationValue = null;

    const ap = signPayload.additionalParameters as Record<string, unknown> | undefined;
    if (ap) {
      ap.employeeId = PHN362_EMPLOYEE_ID;
      ap.riskAssessment = 'PERMIT';
    }

    const put = await Request.put(
      `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: signPayload },
    );
    await expect(put).CheckResponse();

    await activatePodOnContractVersion(Request, Responses, contractId, 1, IDX_POD_A, PHN362_POD_ACTIVATION);
  });

  await test.step('Contract B (TC-BE-39 inline): promote ACTIVE_IN_TERM [TC-BE-10]', async () => {
    const body = await loadProductContract(Request, contractId);
    const header = headerContractStatus(body);
    expect(
      header === 'ACTIVE_IN_TERM' ||
        header === 'ENTERED_INTO_FORCE' ||
        header === 'ACTIVE_IN_PERPETUITY',
      `contract B header after POD activation (got ${header})`,
    ).toBeTruthy();
  });

  const podGetIdentifier = await Request.get(`pod/${Responses.pod[IDX_POD_A].id}`);
  await expect(podGetIdentifier).CheckResponse();
  const podIdentifier = String((await podGetIdentifier.json()).identifier);

  return { contractId, contractPayload, podIdentifier };
}

export async function createNewContractFManual_TC39(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
): Promise<{ contractId: number; contractPayload: Record<string, unknown> }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_RS,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.entryInForceDate = null;
  bp.startOfInitialTerm = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'WITHOUT_SUPPLY';
  pp.entryIntoForce = 'MANUAL';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'MANUAL';
  pp.startOfContractValue = null;
  pp.supplyActivation = 'FIRST_DAY_OF_MONTH';
  pp.supplyActivationValue = null;
  pp.productContractWaitForOldContractTermToExpires = 'YES';

  const podDetailId = await resolvePodDetailId(Request, Responses, IDX_POD_A);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const podGet = await Request.get(`pod/${Responses.pod[IDX_POD_A].id}?version=1`);
  await expect(podGet).CheckResponse();
  const podJson = await podGet.json();
  const monthlyKwh = podJson.type === 'CONSUMER' ? Number(podJson.estimatedMonthlyAvgConsumption ?? 0) : 0;
  const annualMwh = (monthlyKwh * 12) / 1000;
  (contractPayload.additionalParameters as Record<string, unknown>).estimatedTotalConsumptionUnderContractKwh =
    annualMwh;
  console.log('[TC-BE-39] contract F POST:', {
    pointOfDeliveryDetailId: podDetailId,
    monthlyKwh,
    annualMwh,
    entryIntoForce: pp.entryIntoForce,
    entryIntoForceValue: pp.entryIntoForceValue,
    startOfContractInitialTerm: pp.startOfContractInitialTerm,
    startOfContractValue: pp.startOfContractValue,
    supplyActivation: pp.supplyActivation,
    supplyActivationValue: pp.supplyActivationValue,
    productContractWaitForOldContractTermToExpires: pp.productContractWaitForOldContractTermToExpires,
    ...PHN362_DYNAMIC_DATES_LOG,
  });

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  return { contractId, contractPayload };
}

export type ResignThirdTabConfig = {
  supplyActivation: 'FIRST_DAY_OF_MONTH' | 'EXACT_DATE';
  waitForOldContractTermToExpires: 'YES' | 'NO';
  supplyActivationValue?: string;
};

export type SignContractFOptions = {
  signingDate?: string;
  versionStartDate?: string;
  matchedVersion?: number;
  blockedPerpetuityGate?: boolean;
  /** Omit basicParameters.entryInForceDate from sign PUT (SIGNING derives entry from signingDate). */
  omitEntryInForceOnSign?: boolean;
  /** Excel AC4 — false = user has not confirmed removal of future activations (Cases 17/19). */
  removeFuturePods?: boolean;
};

export type Ac4ConflictRow = {
  contractId?: number;
  contractNumber?: string;
  podId?: number;
  fromDate?: string;
  toDate?: string | null;
};

export function extractAc4ConflictRows(body: Record<string, unknown>): Ac4ConflictRow[] {
  const candidates = [
    body.conflicts,
    body.futureActivationConflicts,
    body.resigningConflicts,
    (body.details as Record<string, unknown> | undefined)?.conflicts,
  ];
  for (const c of candidates) {
    if (Array.isArray(c) && c.length > 0) {
      return c as Ac4ConflictRow[];
    }
  }
  return [];
}

export async function signContractFAndTriggerResign(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  contractId: number,
  basePayload: Record<string, unknown>,
  thirdTab: ResignThirdTabConfig,
  signOpts: SignContractFOptions = {},
) {
  const fBodyBeforeSign = await loadProductContract(Request, contractId);
  const signingDate = signOpts.signingDate ?? resolveFSigningDate(fBodyBeforeSign);
  const matchedVersion =
    signOpts.matchedVersion ?? versionIdForSigningDate(fBodyBeforeSign, signingDate);

  const editPayload = await buildEditProductContractPayload(Request, contractId, basePayload, {
    preserveSigningDate: true,
    omitStartDate: true,
    signingDateOverride: signingDate,
  });

  applySignFResignPutFields(editPayload, thirdTab, signingDate);
  editPayload.removeFuturePods = true;
  sanitizeFResignSignPutPayload(editPayload, signingDate);
  if (signOpts.omitEntryInForceOnSign) {
    omitEntryInForceFieldsOnSignPayload(editPayload);
  }
  const putBody = signOpts.omitEntryInForceOnSign
    ? (JSON.parse(JSON.stringify(editPayload)) as Record<string, unknown>)
    : editPayload;

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${matchedVersion}&changeFutureVersionsPods=false`,
    { data: putBody },
  );
  await expect(put).CheckResponse();
}

export async function signContractFAndTriggerResign_TC39(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  contractId: number,
  basePayload: Record<string, unknown>,
  thirdTab: ResignThirdTabConfig,
  contractBId: number,
) {
  const matchedVersion = 1;

  const bBefore = await loadProductContract(Request, contractBId);
  const bBp = (bBefore.basicParameters ?? {}) as {
    status?: string;
    contractTermEndDate?: string;
    availableForResigning?: boolean;
  };
  console.log('[TC-BE-39] contract B before F sign:', {
    contractBId,
    status: bBp.status,
    contractTermEndDate: bBp.contractTermEndDate,
    availableForResigning: bBp.availableForResigning,
    fSigningDate: PHN362_F_SIGNING_DATE,
    resigningDeadlineDays: PHN362_RESIGNING_DEADLINE_DAYS,
  });

  const editPayload = await buildEditProductContractPayload(Request, contractId, basePayload, {
    preserveSigningDate: true,
  });

  const bp = editPayload.basicParameters as Record<string, unknown>;
  bp.status = 'SIGNED';
  bp.subStatus = 'SIGNED_BY_BOTH_SIDES';
  bp.versionStatus = 'SIGNED';
  bp.signingDate = PHN362_F_SIGNING_DATE;
  bp.entryInForceDate = null;
  bp.startOfInitialTerm = null;

  const pp = editPayload.productParameters as Record<string, unknown>;
  pp.entryIntoForce = 'MANUAL';
  pp.entryIntoForceValue = null;
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'MANUAL';
  pp.startOfContractValue = null;
  pp.supplyActivation = thirdTab.supplyActivation;
  pp.productContractWaitForOldContractTermToExpires = thirdTab.waitForOldContractTermToExpires;
  if (thirdTab.supplyActivationValue) {
    pp.supplyActivationValue = thirdTab.supplyActivationValue;
  } else {
    pp.supplyActivationValue = null;
  }

  const ap = editPayload.additionalParameters as Record<string, unknown> | undefined;
  if (ap) {
    ap.employeeId = PHN362_EMPLOYEE_ID;
    ap.riskAssessment = 'PERMIT';
  }

  editPayload.removeFuturePods = true;

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${matchedVersion}&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await expect(put).CheckResponse();
}

export async function assertResignOutcome(
  Request: FixtureRequest,
  contractFId: number,
  contractBId: number,
  podIdentifier: string,
  expected: { fActivation: string; bDeactivation: string },
) {
  await test.step('Step 16: activation on F; B deactivation = activation − 1 [TC-BE-37/38]', async () => {
    const fBody = await loadProductContract(Request, contractFId);
    const fPod = findPodRow(fBody, podIdentifier);
    expect(fPod, 'POD a on contract F').toBeDefined();
    expect((fPod as { activationDate?: string }).activationDate).toBe(expected.fActivation);

    const bBody = await loadProductContract(Request, contractBId);
    const bPod = findPodRow(bBody, podIdentifier);
    expect(bPod, 'POD a on contract B').toBeDefined();
    expect((bPod as { deactivationDate?: string }).deactivationDate).toBe(expected.bDeactivation);
  });

  await test.step('Step 17: resignedFrom / resignedTo on F and B [TC-BE-37/38]', async () => {
    const fBody = await loadProductContract(Request, contractFId);
    const bBody = await loadProductContract(Request, contractBId);

    const fFrom = resignedLinkIds(fBody, 'from');
    const bTo = resignedLinkIds(bBody, 'to');

    expect(fFrom, 'F resignedFrom must reference old contract B').toContain(contractBId);
    expect(bTo, 'B resignedTo must reference new contract F').toContain(contractFId);
  });

  await test.step('Step 18: POD marked isResigned on B [TC-BE-37/38]', async () => {
    const bBody = await loadProductContract(Request, contractBId);
    const bPod = findPodRow(bBody, podIdentifier) as { isResigned?: boolean; versionId?: number };
    expect(bPod?.isResigned, 'B POD a isResigned flag').toBe(true);
    expect(bPod?.versionId, 'B POD row on used version').toBeTruthy();
  });
}

export async function runSharedPreconditions_TC39(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  await test.step('Precondition: term (P-STD chain, MANUAL) [TC-BE-39]', async () => {
    await sharedTermStd_TC39(Request, GeneratePayload, Responses, Endpoints);
  });

  await test.step('Precondition: product P-STD (no price components) [TC-BE-39]', async () => {
    await createProductStd_TC39(Request, GeneratePayload, Responses, Endpoints);
  });

  await test.step('Precondition: term (P-RS chain, Wait=Yes) [TC-BE-39]', async () => {
    await sharedTermRs_TC39(Request, GeneratePayload, Responses, Endpoints);
  });

  await test.step('Precondition: customer A (legal entity) [TC-BE-39]', async () => {
    await createCustomerLegal_TC39(Request, GeneratePayload, Responses, Endpoints);
  });

  await test.step('Precondition: PODs a, b, c', async () => {
    await createPodsAbc(Request, GeneratePayload, Responses, Endpoints);
  });
}

export type PerpetuityGateRelation = 'greater_than_signing' | 'equal_to_signing' | 'less_than_signing';

export async function runPerpetuityGateWaitYesScenario(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  opts: {
    relation: PerpetuityGateRelation;
    supply: 'FIRST_DAY_OF_MONTH' | 'EXACT_DATE';
    expectResignSuccess: boolean;
    tcRef: string;
    /** Blocked perpetuity gate: omit entryInForceDate on F sign PUT (SIGNING only). */
    omitEntryInForceOnSign?: boolean;
  },
) {
  const signingS = PHN362_PERP_SIGNING_S;
  const perpetuityOnB = perpetuityDateForRelation(opts.relation, signingS);
  const fVersionDates = perpetuityGateFVersionDates(signingS);
  const omitEntryInForceOnSign = opts.omitEntryInForceOnSign !== false;

  let contractBId = 0;
  let contractFId = 0;
  let podIdentifier = '';
  let contractFPayload: Record<string, unknown> = {};

  await runSharedPreconditions(Request, GeneratePayload, Responses, Endpoints);

  await test.step(`Precondition: old contract B in perpetuity (perpetuity ${opts.relation} S)`, async () => {
    const b = await createOldContractBInPerpetuity(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      perpetuityOnB,
    );
    contractBId = b.contractId;
    podIdentifier = b.podIdentifier;
  });

  await test.step('Precondition: product P-RS + contract F V1–V3', async () => {
    await createProductRs(Request, GeneratePayload, Responses, Endpoints);
    const f = await createNewContractFWithVersions(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      fVersionDates,
    );
    contractFId = f.contractId;
    contractFPayload = f.contractPayload;
  });

  await test.step('Step 10.2.1: diagram perpetuity empty or > F signing S', async () => {
    const bBody = await loadProductContract(Request, contractBId);
    const perp = (bBody.basicParameters as { perpetuityDate?: string })?.perpetuityDate;
    expect(perp).toBe(perpetuityOnB);
    expect(perpetuityGatePasses(perp, signingS)).toBe(opts.expectResignSuccess);
  });

  const expected = waitYesExpectedActivation(
    opts.supply,
    PHN362_B_INITIAL_TERM_END,
    PHN362_B_CONTRACT_TERM_END,
  );

  const thirdTab: ResignThirdTabConfig = {
    supplyActivation: opts.supply,
    waitForOldContractTermToExpires: 'YES',
    ...(expected.supplyActivationValue
      ? { supplyActivationValue: expected.supplyActivationValue }
      : {}),
  };

  if (opts.expectResignSuccess) {
    await test.step('Step 1: sign F — Wait=Yes (perpetuity gate pass)', async () => {
      await signContractFAndTriggerResign(
        Request,
        Endpoints,
        contractFId,
        contractFPayload,
        thirdTab,
        { signingDate: signingS, omitEntryInForceOnSign },
      );
    });
    await assertResignOutcome(Request, contractFId, contractBId, podIdentifier, expected);
  } else {
    await test.step(
      'Assert: resign preconditions met except diagram gate (B.perpetuity not > F.signing S)',
      async () => {
        await assertResignPreconditionsExceptPerpetuityGate(
          Request,
          contractBId,
          contractFId,
          signingS,
          perpetuityOnB,
          podIdentifier,
        );
      },
    );
    await test.step('Step 1: sign F — B skipped (perpetuity not > S)', async () => {
      await signContractFExpectResignBlocked(
        Request,
        Endpoints,
        contractFId,
        contractFPayload,
        thirdTab,
        {
          signingDate: signingS,
          blockedPerpetuityGate: true,
          omitEntryInForceOnSign,
        },
      );
    });
    await test.step('Assert: POD on B not re-signed', async () => {
      await assertPodNotResignedOnB(Request, contractBId, podIdentifier);
    });
  }

  test.info().attach(`[PHN-362] ${opts.tcRef} perpetuity gate`, {
    body: JSON.stringify(
      {
        signingS,
        perpetuityOnB,
        relation: opts.relation,
        supply: opts.supply,
        expectResignSuccess: opts.expectResignSuccess,
        expected,
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });
}

export type WaitYesTermEndAnchorOpts = {
  tcRef: string;
  bDates: AnchorContractBDates;
  fSigning: string;
  fVersions: ContractFVersionDates;
  resigningDeadlineDays: number;
  supply: 'FIRST_DAY_OF_MONTH' | 'EXACT_DATE';
  expectedConfluence: { fActivation: string; bDeactivation: string };
  supplyActivationValue?: string;
  phoenixActualHint?: { fActivation: string; bDeactivation: string };
};

/**
 * Wait=Yes Step 10.2 — asserts **Confluence** dates (initial term end anchor).
 * Fails on current Phoenix when split-month dates diverge (see `PHN-362_WaitYes_term_end_anchor.md`).
 */
export async function runWaitYesTermEndAnchorScenario(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  opts: WaitYesTermEndAnchorOpts,
) {
  let contractBId = 0;
  let contractFId = 0;
  let podIdentifier = '';
  let contractFPayload: Record<string, unknown> = {};

  await runSharedPreconditions(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    opts.resigningDeadlineDays,
  );

  await test.step(`Precondition: contract B split/same-month anchor [${opts.tcRef}]`, async () => {
    const b = await createOldContractBWithAnchorDates(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      opts.bDates,
    );
    contractBId = b.contractId;
    podIdentifier = b.podIdentifier;
  });

  await test.step('Precondition: product P-RS + mapping [anchor]', async () => {
    await createProductRs(Request, GeneratePayload, Responses, Endpoints);
  });

  await test.step('Precondition: contract F V1–V3 [anchor]', async () => {
    const f = await createNewContractFWithVersions(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      opts.fVersions,
      opts.bDates.bSigningDate,
    );
    contractFId = f.contractId;
    contractFPayload = f.contractPayload;
  });

  await test.step('Precondition: F signing S inside re-sign window on B', async () => {
    expect(opts.fSigning > tc39AddDays(opts.bDates.contractTermEnd, -opts.resigningDeadlineDays)).toBeTruthy();
    expect(opts.fSigning < opts.bDates.contractTermEnd).toBeTruthy();
  });

  const thirdTab: ResignThirdTabConfig = {
    supplyActivation: opts.supply,
    waitForOldContractTermToExpires: 'YES',
    ...(opts.supplyActivationValue ? { supplyActivationValue: opts.supplyActivationValue } : {}),
  };

  await test.step(`Step 1: sign F — Wait=Yes (${opts.tcRef})`, async () => {
    await signContractFAndTriggerResign(Request, Endpoints, contractFId, contractFPayload, thirdTab, {
      signingDate: opts.fSigning,
      omitEntryInForceOnSign: true,
    });
  });

  await assertResignOutcome(Request, contractFId, contractBId, podIdentifier, opts.expectedConfluence);

  test.info().attach(`[PHN-362] ${opts.tcRef} term-end anchor`, {
    body: JSON.stringify(
      {
        bDates: opts.bDates,
        fSigning: opts.fSigning,
        supply: opts.supply,
        expectedConfluence: opts.expectedConfluence,
        phoenixActualHint: opts.phoenixActualHint,
        supplyActivationValueSent: opts.supplyActivationValue ?? null,
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });
}

export async function runSharedPreconditions(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  resigningDeadlineDays: number = PHN362_RESIGNING_DEADLINE_DAYS,
) {
  await test.step('Precondition: term (P-STD chain, no price components) [TC-BE-39]', async () => {
    await sharedTermStd(Request, GeneratePayload, Responses, Endpoints, resigningDeadlineDays);
  });

  await test.step('Precondition: product P-STD (Re-sign = No)', async () => {
    await createProductStd(Request, GeneratePayload, Responses, Endpoints);
  });

  await test.step('Precondition: term (P-RS chain)', async () => {
    await sharedTermRs(Request, GeneratePayload, Responses, Endpoints);
  });

  await test.step('Precondition: customer A', async () => {
    await createCustomerA(Request, GeneratePayload, Responses, Endpoints);
  });

  await test.step('Precondition: PODs a, b, c', async () => {
    await createPodsAbc(Request, GeneratePayload, Responses, Endpoints);
  });
}
