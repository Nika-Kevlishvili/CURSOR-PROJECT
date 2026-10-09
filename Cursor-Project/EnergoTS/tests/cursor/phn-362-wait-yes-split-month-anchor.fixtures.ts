/**
 * PHN-362 — Wait=Yes + First day of month — split-month anchor (ANCHOR-TC-BE-1).
 *
 * Runtime-relative split-month pair: **Initial term end** (current month) vs **Contract term end** (next month).
 * Expected outcomes derived from those anchors; Dev2 sign PUT uses future entry placeholders where required.
 * Confluence expects activation from **initial term end**; Phoenix Dev2 uses **contract term end**.
 *
 * Ref: `test_cases/Backend/PHN-362_WaitYes_term_end_anchor.md` TC-BE-1
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { templates as TemplatesGenerator } from '../../jsons/payloads/create/nomenclatures/templates';
import { addDaysIso } from './contract-resign-new-flow.fixtures';
import { activatePodOnContract, loadProductContract } from './pdt-2815-version-validity.fixtures';
import {
  findPodRow,
  IDX_PRODUCT_RS,
  IDX_PRODUCT_STD,
} from './phn-362-resign-two-pods-precondition.fixtures';

type FixtureRequest = baseFixture['Request'];
type FixtureResponses = baseFixture['Responses'];
type FixtureEndpoints = baseFixture['Endpoints'];
type FixtureGeneratePayload = baseFixture['GeneratePayload'];

export const ANCHOR_POD_INDEX = 0;
export const ANCHOR_EMPLOYEE_ID = 1283;

export const ANCHOR_RUNTIME_TODAY = (() => new Date().toISOString().slice(0, 10))();

/** First calendar day of the month containing `isoDate`. */
function anchorFirstDayOfMonth(isoDate: string): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCDate(1);
  return d.toISOString().split('T')[0];
}

/** Last calendar day of the month containing `isoDate`. */
function anchorLastDayOfMonth(isoDate: string): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCMonth(d.getUTCMonth() + 1);
  d.setUTCDate(0);
  return d.toISOString().split('T')[0];
}

function anchorAddMonths(isoDate: string, months: number): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCMonth(d.getUTCMonth() + months);
  return d.toISOString().split('T')[0];
}

function anchorDaysBetween(from: string, to: string): number {
  const a = new Date(`${from}T12:00:00.000Z`).getTime();
  const b = new Date(`${to}T12:00:00.000Z`).getTime();
  return Math.round((b - a) / 86_400_000);
}

/**
 * Old contract B — **Initial term end** (current month) vs **Contract term end** (next month).
 * Runtime-relative so Dev2 accepts signing/entry validations on any run day.
 */
export const ANCHOR_B_INITIAL_TERM_END = anchorLastDayOfMonth(ANCHOR_RUNTIME_TODAY);
export const ANCHOR_B_CONTRACT_TERM_END = anchorLastDayOfMonth(anchorAddMonths(ANCHOR_RUNTIME_TODAY, 1));
export const ANCHOR_B_SIGNING = addDaysIso(ANCHOR_RUNTIME_TODAY, -45);
export const ANCHOR_POD_ACTIVATION = anchorFirstDayOfMonth(ANCHOR_RUNTIME_TODAY);

/** F resign signing — today (inside computed re-sign window before B term end). */
export const ANCHOR_F_SIGNING = ANCHOR_RUNTIME_TODAY;

const anchorV1Start = anchorFirstDayOfMonth(ANCHOR_RUNTIME_TODAY);
export const ANCHOR_F_VERSIONS = {
  v1Start: anchorV1Start,
  v2Start: addDaysIso(anchorV1Start, 1),
  v3Start: addDaysIso(anchorV1Start, 2),
} as const;

/** Span includes runtime today through B contract term end (happy-path DAY pattern). */
export const ANCHOR_RESIGNING_DEADLINE_DAYS =
  anchorDaysBetween(ANCHOR_RUNTIME_TODAY, ANCHOR_B_CONTRACT_TERM_END) + 1;

/** Dev2 SIGNED PUT — entry strictly in future (placeholder; SIGNING derives real entry on activation). */
export const ANCHOR_B_ENTRY_IN_FORCE_SIGN_PUT = addDaysIso(ANCHOR_RUNTIME_TODAY, 1);

/** F v1 signing — recent past (Dev2 rejects entryInForce when signing is too far in the past). */
export const ANCHOR_F_V1_SIGNING = addDaysIso(ANCHOR_RUNTIME_TODAY, -14);

/** Wait=Yes + First day — anchor = initial term end → 1st of following month; B deact = F act − 1. */
export function anchorExpectedWaitYesFirstDayFromInitial(initialTermEnd: string): AnchorResignOutcome {
  const fActivation = anchorFirstDayOfMonthAfter(initialTermEnd);
  return { fActivation, bDeactivation: addDaysIso(fActivation, -1) };
}

/**
 * Wait=Yes + Exact day — same anchor as First day, but F activation = same calendar day next month.
 * Parallel to Wait=No Step 10.1 exact (`S + 1 month`, same day), anchored on B initial term end.
 */
export function anchorExpectedWaitYesExactFromInitial(initialTermEnd: string): AnchorResignOutcome {
  const fActivation = anchorSameDayNextMonth(initialTermEnd);
  return { fActivation, bDeactivation: addDaysIso(fActivation, -1) };
}

/** Confluence — Wait=Yes + First day (ANCHOR-TC-BE-1). */
export const ANCHOR_EXPECTED_CONFLUENCE_FIRST_DAY = anchorExpectedWaitYesFirstDayFromInitial(
  ANCHOR_B_INITIAL_TERM_END,
);

/**
 * Confluence — Wait=Yes + Exact day (ANCHOR-TC-BE-2).
 * e.g. `2026-06-30` → F `2026-07-30`, B `2026-07-29`; `2026-02-28` → F `2026-03-28`, B `2026-03-27`.
 */
export const ANCHOR_EXPECTED_CONFLUENCE_EXACT = anchorExpectedWaitYesExactFromInitial(
  ANCHOR_B_INITIAL_TERM_END,
);

/** @deprecated alias — use {@link ANCHOR_EXPECTED_CONFLUENCE_FIRST_DAY} */
export const ANCHOR_EXPECTED_CONFLUENCE = ANCHOR_EXPECTED_CONFLUENCE_FIRST_DAY;

/** Phoenix Dev2 — First day uses `contract_term_end_date` in SQL (ANCHOR-TC-BE-1 defect). */
export const ANCHOR_EXPECTED_PHOENIX_DEV2_FIRST_DAY = {
  fActivation: anchorFirstDayOfMonthAfter(ANCHOR_B_CONTRACT_TERM_END),
  bDeactivation: ANCHOR_B_CONTRACT_TERM_END,
} as const;

/**
 * Phoenix Dev2 — if Exact day were auto-derived from wrong anchor (`contract_term_end_date`),
 * same-day-next-month from contract term end; B deact = contract term end (First-day defect pattern).
 */
export const ANCHOR_EXPECTED_PHOENIX_DEV2_EXACT = {
  fActivation: anchorSameDayNextMonth(ANCHOR_B_CONTRACT_TERM_END),
  bDeactivation: addDaysIso(anchorSameDayNextMonth(ANCHOR_B_CONTRACT_TERM_END), -1),
} as const;

/** @deprecated alias — use {@link ANCHOR_EXPECTED_PHOENIX_DEV2_FIRST_DAY} */
export const ANCHOR_EXPECTED_PHOENIX_DEV2 = ANCHOR_EXPECTED_PHOENIX_DEV2_FIRST_DAY;

export type AnchorContractBDates = {
  initialTermEnd: string;
  contractTermEnd: string;
  bSigningDate: string;
  podActivation: string;
};

export type AnchorResignOutcome = {
  fActivation: string;
  bDeactivation: string;
};

export type AnchorResignThirdTab = {
  supplyActivation: 'FIRST_DAY_OF_MONTH' | 'EXACT_DATE';
  /** When false, `supplyActivationValue` is omitted from sign PUT (TC-BE-2 auto-calc gap). */
  sendSupplyActivationValue?: boolean;
  supplyActivationValue?: string | null;
};

type PrepProductTemplateRefs = {
  templateId: number;
  templateType: 'EMAIL_TEMPLATE' | 'INVOICE_TEMPLATE' | 'CONTRACT_TEMPLATE';
}[];

function anchorAddDays(isoDate: string, days: number): string {
  return addDaysIso(isoDate, days);
}

/** First calendar day of the month after the month containing `isoDate`. */
export function anchorFirstDayOfMonthAfter(isoDate: string): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCMonth(d.getUTCMonth() + 1);
  d.setUTCDate(1);
  return d.toISOString().split('T')[0];
}

/**
 * Same calendar day in the month after `isoDate` (Wait=Yes + Exact day).
 * Parallel to {@link anchorFirstDayOfMonthAfter}: same anchor, but keeps the day instead of the 1st.
 */
export function anchorSameDayNextMonth(isoDate: string): string {
  return anchorAddMonths(isoDate, 1);
}

function headerContractStatus(body: Record<string, unknown>): string | undefined {
  const bp = (body.basicParameters ?? {}) as { status?: string; contractStatus?: string };
  return bp.status ?? bp.contractStatus;
}

function versionRow(body: Record<string, unknown>, logicalVersionId: number) {
  const versions = (body.versions ?? []) as { versionId?: number; id?: number; startDate?: string }[];
  return versions.find((v) => Number(v.versionId) === logicalVersionId);
}

function versionDetailId(body: Record<string, unknown>, logicalVersionId: number): number | null {
  const row = versionRow(body, logicalVersionId);
  return row?.id != null ? Number(row.id) : null;
}

function versionIdForSigningDate(body: Record<string, unknown>, signingDate: string): number {
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

function resignedLinkIds(body: Record<string, unknown>, direction: 'from' | 'to'): number[] {
  const bp = (body.basicParameters ?? {}) as {
    resignedFrom?: { id?: number }[];
    resignedTo?: { id?: number }[];
  };
  const list = direction === 'from' ? bp.resignedFrom : bp.resignedTo;
  return (list ?? []).map((x) => Number(x.id)).filter((id) => Number.isFinite(id));
}

function productPayloadWithoutPriceComponents(
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

function prepProductTemplatesFromEnv(): PrepProductTemplateRefs {
  return [
    { templateId: envVariables.invoice_email_template, templateType: 'EMAIL_TEMPLATE' },
    { templateId: envVariables.invoice_document_template, templateType: 'INVOICE_TEMPLATE' },
    { templateId: envVariables.product_contract_template, templateType: 'CONTRACT_TEMPLATE' },
  ];
}

async function ensurePrepProductTemplates(
  FileUploadRequest: baseFixture['FileUploadRequest'],
  Request: FixtureRequest,
): Promise<PrepProductTemplateRefs> {
  const generator = new TemplatesGenerator(FileUploadRequest, Request.raw);
  const ids = await generator.generateEveryTemplate();
  return [
    { templateId: ids.invoice_email_template, templateType: 'EMAIL_TEMPLATE' },
    { templateId: ids.invoice_document_template, templateType: 'INVOICE_TEMPLATE' },
    { templateId: ids.product_contract_template, templateType: 'CONTRACT_TEMPLATE' },
  ];
}

async function postProductWithTemplateFallback(
  Request: FixtureRequest,
  FileUploadRequest: baseFixture['FileUploadRequest'],
  Endpoints: FixtureEndpoints,
  payload: Record<string, unknown>,
) {
  let templateIds = prepProductTemplatesFromEnv();
  payload.templateIds = templateIds;
  let res = await Request.post(Endpoints.product, { data: payload });
  if (!res.ok()) {
    const text = await res.text();
    if (/template.*not found|wrong purpose/i.test(text)) {
      templateIds = await ensurePrepProductTemplates(FileUploadRequest, Request);
      payload.templateIds = templateIds;
      res = await Request.post(Endpoints.product, { data: payload });
    }
  }
  await expect(res).CheckResponse();
  return res;
}

async function resolvePodDetailId(
  Request: FixtureRequest,
  Responses: FixtureResponses,
  podIndex: number,
): Promise<number> {
  const pod = Responses.pod[podIndex] as { podDetailId?: number; id?: number };
  if (pod.podDetailId != null) return Number(pod.podDetailId);
  const podGet = await Request.get(`pod/${pod.id}`);
  await expect(podGet).CheckResponse();
  const body = await podGet.json();
  return Number(body.podDetailId ?? body.lastPodDetailId ?? body.versions?.[0]?.podDetailId);
}

async function annualMwhForPodIndex(
  Request: FixtureRequest,
  Responses: FixtureResponses,
  podIndex: number,
): Promise<number> {
  const podGet = await Request.get(`pod/${Responses.pod[podIndex].id}?version=1`);
  await expect(podGet).CheckResponse();
  const podJson = await podGet.json();
  const monthlyKwh =
    podJson.type === 'CONSUMER' ? Number(podJson.estimatedMonthlyAvgConsumption ?? 0) : 0;
  return (monthlyKwh * 12) / 1000;
}

async function buildEditProductContractPayload(
  Request: FixtureRequest,
  contractId: number,
  generatedPayload: Record<string, unknown>,
  opts?: {
    savingAsNewVersion?: boolean;
    startDate?: string;
    preserveSigningDate?: boolean;
    omitStartDate?: boolean;
    signingDateOverride?: string;
  },
): Promise<Record<string, unknown>> {
  const contractget = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractget).CheckResponse();
  const contractgetjson = await contractget.json();

  const allPods = (contractgetjson.contractPodsResponses ?? []).map(
    (pod: { podDetailId: number; dealNumber: unknown }) => ({
      pointOfDeliveryDetailId: pod.podDetailId,
      dealNumber: pod.dealNumber,
    }),
  );

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

  const serverStartDate = contractgetjson.versions[0].startDate as string;
  const signingDate =
    opts?.signingDateOverride ??
    (opts?.preserveSigningDate
      ? ((contractgetjson.basicParameters?.signingDate as string | null) ?? null)
      : ANCHOR_B_SIGNING);

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
    (editPayload.additionalParameters as Record<string, unknown>).employeeId = ANCHOR_EMPLOYEE_ID;
  }

  return editPayload;
}

/**
 * Dev2-safe B sign PUT — same pattern as `buildPrepSignPutBody` (precondition fixtures).
 */
function buildAnchorBSignPutBody(
  signPayload: Record<string, unknown>,
  dates: AnchorContractBDates,
): Record<string, unknown> {
  const putBody = JSON.parse(JSON.stringify(signPayload)) as Record<string, unknown>;
  const bp = putBody.basicParameters as Record<string, unknown>;
  bp.status = 'SIGNED';
  bp.subStatus = 'SIGNED_BY_BOTH_SIDES';
  bp.versionStatus = 'SIGNED';
  bp.signingDate = dates.bSigningDate;
  bp.entryInForceDate = ANCHOR_B_ENTRY_IN_FORCE_SIGN_PUT;
  bp.contractTermEndDate = dates.contractTermEnd;
  delete bp.startOfInitialTerm;
  delete bp.statusModifyDate;

  const pp = putBody.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.entryIntoForce = 'SIGNING';
  pp.startOfContractInitialTerm = 'EXACT_DATE';
  pp.startOfContractValue = dates.initialTermEnd;
  pp.contractTermEndDate = dates.contractTermEnd;
  pp.supplyActivation = 'MANUAL';
  delete pp.entryIntoForceValue;
  delete pp.supplyActivationValue;

  return putBody;
}

/**
 * Dev2-safe F historical version SIGNED PUT when POD is already active on contract B.
 * Future entry placeholder + initial term strictly after entry (Dev2 cross-field validation).
 */
function buildAnchorFHistoricalSignPutBody(
  signPayload: Record<string, unknown>,
  signingDate: string,
): Record<string, unknown> {
  const putBody = JSON.parse(JSON.stringify(signPayload)) as Record<string, unknown>;
  const bp = putBody.basicParameters as Record<string, unknown>;
  const pp = putBody.productParameters as Record<string, unknown>;
  const entryInForce = ANCHOR_B_ENTRY_IN_FORCE_SIGN_PUT;
  const initialTermStart = addDaysIso(entryInForce, 1);

  bp.signingDate = signingDate;
  bp.entryInForceDate = entryInForce;
  bp.startOfInitialTerm = initialTermStart;
  delete bp.statusModifyDate;

  pp.contractType = 'SUPPLY_ONLY';
  pp.entryIntoForce = 'SIGNING';
  delete pp.entryIntoForceValue;
  pp.startOfContractInitialTerm = 'EXACT_DATE';
  pp.startOfContractValue = initialTermStart;
  pp.supplyActivation = pp.supplyActivation ?? 'FIRST_DAY_OF_MONTH';
  delete pp.supplyActivationValue;

  return putBody;
}

/**
 * Dev2-safe F re-sign SIGNED PUT — signingDate = today, future entry placeholder, initial term after entry.
 */
function buildAnchorFResignSignPutBody(
  signPayload: Record<string, unknown>,
  signingDate: string,
  thirdTab: AnchorResignThirdTab,
): Record<string, unknown> {
  const putBody = JSON.parse(JSON.stringify(signPayload)) as Record<string, unknown>;
  const bp = putBody.basicParameters as Record<string, unknown>;
  const pp = putBody.productParameters as Record<string, unknown>;
  const entryInForce = ANCHOR_B_ENTRY_IN_FORCE_SIGN_PUT;
  const initialTermStart = addDaysIso(entryInForce, 1);

  bp.status = 'SIGNED';
  bp.subStatus = 'SIGNED_BY_BOTH_SIDES';
  bp.versionStatus = 'SIGNED';
  bp.signingDate = signingDate;
  bp.entryInForceDate = entryInForce;
  bp.startOfInitialTerm = initialTermStart;
  delete bp.statusModifyDate;

  pp.contractType = 'SUPPLY_ONLY';
  pp.entryIntoForce = 'SIGNING';
  delete pp.entryIntoForceValue;
  pp.startOfContractInitialTerm = 'EXACT_DATE';
  pp.startOfContractValue = initialTermStart;
  pp.supplyActivation = thirdTab.supplyActivation;
  pp.productContractWaitForOldContractTermToExpires = 'YES';
  delete pp.supplyActivationValue;
  if (thirdTab.sendSupplyActivationValue && thirdTab.supplyActivationValue != null) {
    pp.supplyActivationValue = thirdTab.supplyActivationValue;
  }

  putBody.removeFuturePods = true;
  return putBody;
}

async function productContractStatusUpdate(
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

/** F historical version SIGNED PUT — SIGNING derives entry from signingDate (past signing allowed). */
function applyAnchorFContractSignedPutEntryGuard(editPayload: Record<string, unknown>) {
  const bp = editPayload.basicParameters as Record<string, unknown>;
  bp.entryInForceDate = null;
  bp.startOfInitialTerm = null;
  delete bp.statusModifyDate;
  const pp = editPayload.productParameters as Record<string, unknown>;
  if (pp.entryIntoForce !== 'MANUAL') {
    pp.entryIntoForce = 'SIGNING';
    pp.entryIntoForceValue = null;
    pp.startOfContractInitialTerm = 'SIGNING';
    pp.startOfContractValue = null;
  }
}

function sanitizeFResignSignPutPayload(editPayload: Record<string, unknown>, signingDate: string) {
  const bp = editPayload.basicParameters as Record<string, unknown>;
  const pp = editPayload.productParameters as Record<string, unknown>;
  bp.status = 'SIGNED';
  bp.subStatus = 'SIGNED_BY_BOTH_SIDES';
  bp.versionStatus = 'SIGNED';
  bp.signingDate = signingDate;
  bp.entryInForceDate = null;
  bp.startOfInitialTerm = null;
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.startOfContractValue = null;
}

function omitEntryInForceFieldsOnSignPayload(editPayload: Record<string, unknown>) {
  applyAnchorFContractSignedPutEntryGuard(editPayload);
  const bp = editPayload.basicParameters as Record<string, unknown>;
  delete bp.entryInForceDate;
  const pp = editPayload.productParameters as Record<string, unknown>;
  delete pp.entryIntoForceValue;
  delete pp.startOfContractValue;
}

async function putProductContractNewVersion(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  contractId: number,
  basePayload: Record<string, unknown>,
  fromVersionId: number,
  opts: { startDate: string; versionStatus?: 'SIGNED' | 'DRAFT' },
) {
  const editPayload = await buildEditProductContractPayload(Request, contractId, basePayload, {
    savingAsNewVersion: true,
    preserveSigningDate: true,
    startDate: opts.startDate,
  });

  const bp = editPayload.basicParameters as Record<string, unknown>;
  bp.versionStatus = opts.versionStatus ?? 'SIGNED';

  const putData =
    (opts.versionStatus ?? 'SIGNED') === 'SIGNED'
      ? buildAnchorFHistoricalSignPutBody(
          editPayload,
          String((editPayload.basicParameters as Record<string, unknown>).signingDate ?? ANCHOR_F_V1_SIGNING),
        )
      : editPayload;

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${fromVersionId}&changeFutureVersionsPods=false`,
    { data: putData },
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

async function sharedTermStdAnchor(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const payload = GeneratePayload.productAndServices.term();
  payload.contractEntryIntoForces = ['SIGNING'];
  payload.startsOfContractInitialTerms = ['SIGNING', 'EXACT_DATE'];
  payload.supplyActivations = ['MANUAL', 'FIRST_DAY_OF_MONTH'];
  payload.resigningDeadlineType = 'DAY';
  payload.resigningDeadlineValue = ANCHOR_RESIGNING_DEADLINE_DAYS;
  const res = await Request.post(Endpoints.terms, { data: payload });
  await expect(res).CheckResponse();
  Responses.terms.push(await res.json());
}

async function sharedTermRsAnchor(
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

async function createProductStdAnchor(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
) {
  const payload = productPayloadWithoutPriceComponents(GeneratePayload, 0);
  payload.termId = Responses.terms[0].id;
  payload.contractTypes = ['SUPPLY_ONLY'];
  payload.paymentGuarantees = ['NO'];
  const res = await postProductWithTemplateFallback(Request, FileUploadRequest, Endpoints, payload);
  const json = await res.json();
  Responses.product.push(typeof json === 'number' ? json : json.id);
}

async function createProductRsAnchor(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
) {
  const payload = productPayloadWithoutPriceComponents(GeneratePayload, 1);
  payload.termId = Responses.terms[1].id;
  payload.contractTypes = ['SUPPLY_ONLY'];
  payload.paymentGuarantees = ['NO'];
  (payload as Record<string, unknown>).isResigning = true;
  (payload as Record<string, unknown>).resignProductTargets = [Responses.product[IDX_PRODUCT_STD]];

  const productRes = await postProductWithTemplateFallback(Request, FileUploadRequest, Endpoints, payload);
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
    { productId: Responses.product[IDX_PRODUCT_STD], versionIds: [1] },
  ];

  const specialOffersRes = await Request.put(
    `products/${Responses.product[IDX_PRODUCT_RS]}/special-offers?version=1`,
    { data: specialOfferPayload },
  );
  await expect(specialOffersRes).CheckResponse();
}

export async function runAnchorSharedPreconditions(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
) {
  await test.step('Precondition: term P-STD (30-day re-sign window)', async () => {
    await sharedTermStdAnchor(Request, GeneratePayload, Responses, Endpoints);
  });
  await test.step('Precondition: product P-STD', async () => {
    await createProductStdAnchor(Request, GeneratePayload, Responses, Endpoints, FileUploadRequest);
  });
  await test.step('Precondition: term P-RS (Wait=Yes/No)', async () => {
    await sharedTermRsAnchor(Request, GeneratePayload, Responses, Endpoints);
  });
  await test.step('Precondition: product P-RS + mapping', async () => {
    await createProductRsAnchor(Request, GeneratePayload, Responses, Endpoints, FileUploadRequest);
  });
  await test.step('Precondition: customer (legal entity)', async () => {
    const res = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(res).CheckResponse();
    Responses.customer.push(await res.json());
  });
  await test.step('Precondition: POD a', async () => {
    const res = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(res).CheckResponse();
    Responses.pod.push(await res.json());
  });
}

export async function createAnchorContractB(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  dates: AnchorContractBDates = {
    initialTermEnd: ANCHOR_B_INITIAL_TERM_END,
    contractTermEnd: ANCHOR_B_CONTRACT_TERM_END,
    bSigningDate: ANCHOR_B_SIGNING,
    podActivation: ANCHOR_POD_ACTIVATION,
  },
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

  const podDetailId = await resolvePodDetailId(Request, Responses, ANCHOR_POD_INDEX);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const annualMwh = await annualMwhForPodIndex(Request, Responses, ANCHOR_POD_INDEX);
  (contractPayload.additionalParameters as Record<string, unknown>).estimatedTotalConsumptionUnderContractKwh =
    annualMwh;

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  await test.step(
    `Contract B: sign (initial ${dates.initialTermEnd}, term end ${dates.contractTermEnd})`,
    async () => {
      const signPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
        preserveSigningDate: false,
        startDate: dates.podActivation,
      });
      const ap = signPayload.additionalParameters as Record<string, unknown> | undefined;
      if (ap) {
        ap.employeeId = ANCHOR_EMPLOYEE_ID;
        ap.riskAssessment = 'PERMIT';
      }

      const putBody = buildAnchorBSignPutBody(signPayload, dates);

      const put = await Request.put(
        `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
        { data: putBody },
      );
      await expect(put).CheckResponse();

      await activatePodOnContract(
        Request,
        Responses,
        contractId,
        1,
        dates.podActivation,
        ANCHOR_POD_INDEX,
      );
    },
  );

  await test.step('Contract B: ACTIVE_IN_TERM', async () => {
    const body = await loadProductContract(Request, contractId);
    const header = headerContractStatus(body);
    expect(
      header === 'ACTIVE_IN_TERM' ||
        header === 'ENTERED_INTO_FORCE' ||
        header === 'ACTIVE_IN_PERPETUITY',
      `contract B header after POD activation (got ${header})`,
    ).toBeTruthy();
  });

  const podGet = await Request.get(`pod/${Responses.pod[ANCHOR_POD_INDEX].id}`);
  await expect(podGet).CheckResponse();
  const podIdentifier = String((await podGet.json()).identifier);

  return { contractId, contractPayload, podIdentifier };
}

export async function createAnchorContractFWithVersions(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  versionDates: typeof ANCHOR_F_VERSIONS = ANCHOR_F_VERSIONS,
  fV1SigningDate: string = ANCHOR_F_V1_SIGNING,
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

  const podDetailId = await resolvePodDetailId(Request, Responses, ANCHOR_POD_INDEX);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const annualMwh = await annualMwhForPodIndex(Request, Responses, ANCHOR_POD_INDEX);
  (contractPayload.additionalParameters as Record<string, unknown>).estimatedTotalConsumptionUnderContractKwh =
    annualMwh;

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  await test.step('Contract F: v1 SIGNED', async () => {
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
    const putBody = buildAnchorFHistoricalSignPutBody(editPayload, fV1SigningDate);
    const putBp = putBody.basicParameters as Record<string, unknown>;
    putBp.status = 'SIGNED';
    putBp.subStatus = 'SIGNED_BY_BOTH_SIDES';
    putBp.versionStatus = 'SIGNED';
    const put = await Request.put(
      `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: putBody },
    );
    await expect(put).CheckResponse();
  });

  await test.step('Contract F: v2 SIGNED', async () => {
    await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 1, {
      startDate: versionDates.v2Start,
      versionStatus: 'SIGNED',
    });
  });

  await test.step('Contract F: v3 SIGNED', async () => {
    await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 2, {
      startDate: versionDates.v3Start,
      versionStatus: 'SIGNED',
    });
  });

  return { contractId, contractPayload };
}

export type AnchorFSignResult = {
  ok: boolean;
  status: number;
  responseText: string;
};

export async function signAnchorContractFAndTriggerResign(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  contractId: number,
  basePayload: Record<string, unknown>,
  signingDate: string = ANCHOR_F_SIGNING,
  thirdTab: AnchorResignThirdTab = {
    supplyActivation: 'FIRST_DAY_OF_MONTH',
    sendSupplyActivationValue: false,
  },
  opts: { allowResignFailure?: boolean } = {},
): Promise<AnchorFSignResult> {
  const fBodyBeforeSign = await loadProductContract(Request, contractId);
  const matchedVersion = versionIdForSigningDate(fBodyBeforeSign, signingDate);

  const editPayload = await buildEditProductContractPayload(Request, contractId, basePayload, {
    preserveSigningDate: true,
    omitStartDate: true,
    signingDateOverride: signingDate,
  });

  const putBody = buildAnchorFResignSignPutBody(editPayload, signingDate, thirdTab);
  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${matchedVersion}&changeFutureVersionsPods=false`,
    { data: putBody },
  );
  const status = put.status();
  const responseText = await put.text();

  if (status >= 400) {
    if (opts.allowResignFailure) {
      return { ok: false, status, responseText };
    }
    await expect(put).CheckResponse();
  }

  return { ok: true, status, responseText };
}

export async function assertAnchorResignOutcome(
  Request: FixtureRequest,
  contractFId: number,
  contractBId: number,
  podIdentifier: string,
  expected: AnchorResignOutcome,
) {
  await test.step('Assert: POD activation on F and deactivation on B', async () => {
    const fBody = await loadProductContract(Request, contractFId);
    const fPod = findPodRow(fBody, podIdentifier);
    expect(fPod, 'POD on contract F').toBeDefined();
    expect((fPod as { activationDate?: string }).activationDate).toBe(expected.fActivation);

    const bBody = await loadProductContract(Request, contractBId);
    const bPod = findPodRow(bBody, podIdentifier);
    expect(bPod, 'POD on contract B').toBeDefined();
    expect((bPod as { deactivationDate?: string }).deactivationDate).toBe(expected.bDeactivation);
  });

  await test.step('Assert: resignedFrom / resignedTo links', async () => {
    const fBody = await loadProductContract(Request, contractFId);
    const bBody = await loadProductContract(Request, contractBId);
    expect(resignedLinkIds(fBody, 'from')).toContain(contractBId);
    expect(resignedLinkIds(bBody, 'to')).toContain(contractFId);
  });

  await test.step('Assert: POD isResigned on B', async () => {
    const bBody = await loadProductContract(Request, contractBId);
    const bPod = findPodRow(bBody, podIdentifier) as { isResigned?: boolean };
    expect(bPod?.isResigned).toBe(true);
  });
}

export type RunAnchorSplitMonthOpts = {
  /** Assert Confluence dates (pass after fix) or Phoenix Dev2 defect baseline. */
  assertExpected?: AnchorResignOutcome;
  thirdTab?: AnchorResignThirdTab;
  tcRef?: string;
  phoenixDev2Hint?: AnchorResignOutcome;
};

const ANCHOR_THIRD_TAB_FIRST_DAY: AnchorResignThirdTab = {
  supplyActivation: 'FIRST_DAY_OF_MONTH',
  sendSupplyActivationValue: false,
};

const ANCHOR_THIRD_TAB_EXACT_OMIT_VALUE: AnchorResignThirdTab = {
  supplyActivation: 'EXACT_DATE',
  sendSupplyActivationValue: false,
};

/**
 * Full ANCHOR split-month flow — Wait=Yes + First day (TC-BE-1) or Exact day omit value (TC-BE-2).
 */
export async function runAnchorWaitYesSplitMonthScenario(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
  opts: RunAnchorSplitMonthOpts = {},
) {
  const thirdTab = opts.thirdTab ?? ANCHOR_THIRD_TAB_FIRST_DAY;
  const expected =
    opts.assertExpected ??
    (thirdTab.supplyActivation === 'EXACT_DATE'
      ? ANCHOR_EXPECTED_CONFLUENCE_EXACT
      : ANCHOR_EXPECTED_CONFLUENCE_FIRST_DAY);
  const phoenixHint =
    opts.phoenixDev2Hint ??
    (thirdTab.supplyActivation === 'EXACT_DATE'
      ? ANCHOR_EXPECTED_PHOENIX_DEV2_EXACT
      : ANCHOR_EXPECTED_PHOENIX_DEV2_FIRST_DAY);
  const tcRef = opts.tcRef ?? 'ANCHOR-TC-BE-1';

  let contractBId = 0;
  let contractFId = 0;
  let podIdentifier = '';
  let contractFPayload: Record<string, unknown> = {};

  await runAnchorSharedPreconditions(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  );

  await test.step('Precondition: contract B (split-month anchor)', async () => {
    const b = await createAnchorContractB(Request, GeneratePayload, Responses, Endpoints);
    contractBId = b.contractId;
    podIdentifier = b.podIdentifier;
  });

  await test.step('Precondition: contract F V1–V3', async () => {
    const f = await createAnchorContractFWithVersions(Request, GeneratePayload, Responses, Endpoints);
    contractFId = f.contractId;
    contractFPayload = f.contractPayload;
  });

  await test.step('Precondition: F signing inside B re-sign window', async () => {
    expect(
      ANCHOR_F_SIGNING > anchorAddDays(ANCHOR_B_CONTRACT_TERM_END, -ANCHOR_RESIGNING_DEADLINE_DAYS),
    ).toBeTruthy();
    expect(ANCHOR_F_SIGNING < ANCHOR_B_CONTRACT_TERM_END).toBeTruthy();
  });

  const signStepLabel =
    thirdTab.supplyActivation === 'EXACT_DATE'
      ? `Sign F — Wait=Yes + EXACT_DATE (omit supplyActivationValue) [${tcRef}]`
      : `Sign F — Wait=Yes + FIRST_DAY_OF_MONTH (no supplyActivationValue) [${tcRef}]`;

  const allowResignFailure =
    thirdTab.supplyActivation === 'EXACT_DATE' && !thirdTab.sendSupplyActivationValue;

  let signResult: AnchorFSignResult = { ok: true, status: 200, responseText: '' };

  await test.step(signStepLabel, async () => {
    signResult = await signAnchorContractFAndTriggerResign(
      Request,
      Endpoints,
      contractFId,
      contractFPayload,
      ANCHOR_F_SIGNING,
      thirdTab,
      { allowResignFailure },
    );
  });

  if (!signResult.ok) {
    await test.step(`Assert: resign blocked without supplyActivationValue [${tcRef}]`, async () => {
      expect(signResult.status).toBeGreaterThanOrEqual(400);
      if (thirdTab.supplyActivation === 'EXACT_DATE') {
        expect(
          signResult.responseText,
          'Phoenix should fail when engine cannot derive activation (null supplyActivationDate)',
        ).toMatch(/activationDate|supplyActivation|NullPointerException|Resigning process failed/i);
      }
    });

    return {
      contractBId,
      contractFId,
      podIdentifier,
      expected,
      phoenixDev2Hint: phoenixHint,
      thirdTab,
      tcRef,
      resignBlocked: true as const,
      resignStatus: signResult.status,
      resignResponse: signResult.responseText,
    };
  }

  await assertAnchorResignOutcome(Request, contractFId, contractBId, podIdentifier, expected);

  return {
    contractBId,
    contractFId,
    podIdentifier,
    expected,
    phoenixDev2Hint: phoenixHint,
    thirdTab,
    tcRef,
    resignBlocked: false as const,
  };
}

export type AnchorFDraftThirdTab = {
  waitForOldContractTermToExpire: 'YES' | 'NO';
  supplyActivation: 'EXACT_DATE';
  /** Omit on POST — engine should derive on sign (TC-BE-2 gap when Wait=Yes). */
  supplyActivationValue?: string | null;
};

/**
 * Contract F — DRAFT POST only (no v1–v3 sign, no re-sign PUT).
 */
export async function createAnchorContractFDraft(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  thirdTab: AnchorFDraftThirdTab = {
    waitForOldContractTermToExpire: 'NO',
    supplyActivation: 'EXACT_DATE',
    supplyActivationValue: null,
  },
): Promise<{ contractId: number; contractPayload: Record<string, unknown> }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_RS,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  // v1 POST must keep generator default versionStatus SIGNED (Dev2 rejects DRAFT on create).
  bp.signingDate = null;
  bp.entryInForceDate = null;
  bp.startOfInitialTerm = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.startOfContractValue = null;
  pp.supplyActivation = thirdTab.supplyActivation;
  pp.productContractWaitForOldContractTermToExpires = thirdTab.waitForOldContractTermToExpire;
  if (thirdTab.supplyActivationValue != null) {
    pp.supplyActivationValue = thirdTab.supplyActivationValue;
  } else {
    pp.supplyActivationValue = null;
  }

  const podDetailId = await resolvePodDetailId(Request, Responses, ANCHOR_POD_INDEX);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const annualMwh = await annualMwhForPodIndex(Request, Responses, ANCHOR_POD_INDEX);
  (contractPayload.additionalParameters as Record<string, unknown>).estimatedTotalConsumptionUnderContractKwh =
    annualMwh;

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = (await createRes.json()) as Record<string, unknown>;
  Responses.productContract.push(createJson);

  return { contractId: createJson.id as number, contractPayload };
}

export type AnchorExactDraftResult = {
  contractBId: number;
  contractFId: number;
  podIdentifier: string;
  tcRef: string;
  thirdTab: AnchorFDraftThirdTab;
};

/** @deprecated alias */
export type AnchorPreResignDraftResult = AnchorExactDraftResult;

/** After B sign PUT, initial term end lives on productParameters.startOfContractValue. */
function initialTermEndFromContractB(body: Record<string, unknown>): string {
  const pp = (body.productParameters ?? {}) as { startOfContractValue?: string };
  const bp = (body.basicParameters ?? {}) as { startOfInitialTerm?: string };
  return (pp.startOfContractValue ?? bp.startOfInitialTerm ?? ANCHOR_B_INITIAL_TERM_END) as string;
}

export async function assertAnchorExactDraftState(
  Request: FixtureRequest,
  contractFId: number,
  contractBId: number,
  podIdentifier: string,
  thirdTab: AnchorFDraftThirdTab,
) {
  await test.step(
    `Assert: F DRAFT | Wait=${thirdTab.waitForOldContractTermToExpire} | ${thirdTab.supplyActivation}`,
    async () => {
      const fBody = await loadProductContract(Request, contractFId);
      const fBp = (fBody.basicParameters ?? {}) as {
        status?: string;
        subStatus?: string;
        signingDate?: string | null;
        resignedFrom?: unknown[];
        resignedTo?: unknown[];
      };
      expect(fBp.status, 'F basicParameters.status').toBe('DRAFT');
      expect(fBp.subStatus, 'F basicParameters.subStatus').toBe('DRAFT');
      expect(fBp.signingDate == null || fBp.signingDate === '', 'F signingDate unset').toBeTruthy();
      expect(fBp.resignedFrom ?? [], 'F basicParameters.resignedFrom').toHaveLength(0);
      expect(fBp.resignedTo ?? [], 'F basicParameters.resignedTo').toHaveLength(0);
      expect(resignedLinkIds(fBody, 'from'), 'F resignedFrom links').toEqual([]);
      expect(resignedLinkIds(fBody, 'to'), 'F resignedTo links').toEqual([]);

      const fPods = (fBody.contractPodsResponses ?? []) as { identifier?: string; activationDate?: string | null }[];
      expect(fPods, 'F POD rows').toHaveLength(1);
      expect(fPods[0]?.identifier).toBe(podIdentifier);
      expect(fPods[0]?.activationDate ?? null, 'F POD activation').toBeNull();

      const pp = (fBody.productParameters ?? {}) as {
        supplyActivation?: string;
        productContractWaitForOldContractTermToExpires?: string;
        supplyActivationValue?: string | null;
      };
      expect(pp.supplyActivation, 'F supplyActivation').toBe(thirdTab.supplyActivation);
      expect(pp.productContractWaitForOldContractTermToExpires, 'F wait for old contract').toBe(
        thirdTab.waitForOldContractTermToExpire,
      );
      expect(pp.supplyActivationValue ?? null, 'F supplyActivationValue').toBe(
        thirdTab.supplyActivationValue ?? null,
      );
    },
  );

  await test.step('Assert: contract B split-month anchor dates unchanged', async () => {
    const bBody = await loadProductContract(Request, contractBId);
    const bBp = (bBody.basicParameters ?? {}) as {
      contractTermEndDate?: string;
      status?: string;
    };
    expect(bBp.contractTermEndDate).toBe(ANCHOR_B_CONTRACT_TERM_END);
    expect(initialTermEndFromContractB(bBody)).toBe(ANCHOR_B_INITIAL_TERM_END);
    expect(
      bBp.status === 'ACTIVE_IN_TERM' ||
        bBp.status === 'ENTERED_INTO_FORCE' ||
        bBp.status === 'ACTIVE_IN_PERPETUITY',
      `B header after setup (got ${bBp.status})`,
    ).toBeTruthy();

    const bPod = findPodRow(bBody, podIdentifier) as { activationDate?: string; isResigned?: boolean };
    expect(bPod?.activationDate).toBe(ANCHOR_POD_ACTIVATION);
    expect(bPod?.isResigned ?? false).toBe(false);
    expect(resignedLinkIds(bBody, 'to'), 'B resignedTo').toEqual([]);
  });
}

export const ANCHOR_DRAFT_THIRD_TAB_NO_EXACT: AnchorFDraftThirdTab = {
  waitForOldContractTermToExpire: 'NO',
  supplyActivation: 'EXACT_DATE',
  supplyActivationValue: null,
};

export const ANCHOR_DRAFT_THIRD_TAB_YES_EXACT: AnchorFDraftThirdTab = {
  waitForOldContractTermToExpire: 'YES',
  supplyActivation: 'EXACT_DATE',
  supplyActivationValue: null,
};

/**
 * Split-month B (signed) + F POST DRAFT with **EXACT_DATE** third tab (no sign, no re-sign).
 */
export async function runAnchorSplitMonthExactDraftScenario(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
  thirdTab: AnchorFDraftThirdTab,
  tcRef: string,
): Promise<AnchorExactDraftResult> {
  let contractBId = 0;
  let contractFId = 0;
  let podIdentifier = '';

  await runAnchorSharedPreconditions(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  );

  await test.step('Precondition: contract B (split-month anchor)', async () => {
    const b = await createAnchorContractB(Request, GeneratePayload, Responses, Endpoints);
    contractBId = b.contractId;
    podIdentifier = b.podIdentifier;
  });

  await test.step(
    `Precondition: contract F POST DRAFT | Wait=${thirdTab.waitForOldContractTermToExpire} | EXACT_DATE`,
    async () => {
      const f = await createAnchorContractFDraft(Request, GeneratePayload, Responses, Endpoints, thirdTab);
      contractFId = f.contractId;
    },
  );

  await assertAnchorExactDraftState(Request, contractFId, contractBId, podIdentifier, thirdTab);

  return { contractBId, contractFId, podIdentifier, tcRef, thirdTab };
}

/** Wait=No + EXACT_DATE — F DRAFT snapshot (Step 10.1 path, before sign). */
export async function runAnchorDraftNoFlowExactScenario(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
) {
  return runAnchorSplitMonthExactDraftScenario(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
    ANCHOR_DRAFT_THIRD_TAB_NO_EXACT,
    'ANCHOR-TC-DRAFT-NO-EXACT',
  );
}

/** Wait=Yes + EXACT_DATE — F DRAFT snapshot (Step 10.2 path, before sign / re-sign). */
export async function runAnchorDraftYesFlowExactScenario(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
) {
  return runAnchorSplitMonthExactDraftScenario(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
    ANCHOR_DRAFT_THIRD_TAB_YES_EXACT,
    'ANCHOR-TC-DRAFT-YES-EXACT',
  );
}

/** ANCHOR-TC-BE-2 — Wait=Yes + Exact day, omit `supplyActivationValue` (auto-calculation gap). */
export async function runAnchorWaitYesSplitMonthExactScenario(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
  opts: Omit<RunAnchorSplitMonthOpts, 'thirdTab' | 'tcRef'> = {},
) {
  return runAnchorWaitYesSplitMonthScenario(Request, GeneratePayload, Responses, Endpoints, FileUploadRequest, {
    ...opts,
    thirdTab: ANCHOR_THIRD_TAB_EXACT_OMIT_VALUE,
    tcRef: 'ANCHOR-TC-BE-2',
    assertExpected: opts.assertExpected ?? ANCHOR_EXPECTED_PHOENIX_DEV2_EXACT,
    phoenixDev2Hint: opts.phoenixDev2Hint ?? ANCHOR_EXPECTED_CONFLUENCE_EXACT,
  });
}
