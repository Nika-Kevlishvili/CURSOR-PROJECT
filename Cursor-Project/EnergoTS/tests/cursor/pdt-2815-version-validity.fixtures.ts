/**
 * PDT-2815 — Contract version validity (Mass Email import + Penalty gap probe).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2854-pod-active-two-contracts.spec.ts (DRAFT POST → READY → SIGNED → edit PUT → new version)
 * - tests/cursor/pdt-2854-pod-active-two-contracts.fixtures.ts (buildEditProductContractPayload, productContractStatusUpdate)
 */

import * as ExcelJS from 'exceljs';
import fs from 'fs';
import path from 'path';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { configuredBaseURL } from '../../fixtures/utils/baseUrl';
import { envVariables } from '../../fixtures/envCashed';
import { ResponseLinker } from '../../utils/reporters/ResponseLinker';
import {
  asPriceComponentId,
  asServiceId,
  buildServiceContractEditPayloadFromDetail,
  pickLatestLogicalVersionId,
  pdt2599MinCalendarIsoForServiceContractPost,
  resolvePdt2599ContractFormulaForPost,
  runPdt2846DraftServiceContractChain,
  runPdt2599SignedServiceContractChain,
} from './pdt-2599-service-contract.fixtures';

type FixtureRequest = baseFixture['Request'];
type FixtureFileUpload = baseFixture['FileUploadRequest'];

/** Execution / import anchor — always "today" so signingDate and POD activation stay aligned on Dev. */
export function anchorDateIso(): string {
  return new Date().toISOString().split('T')[0];
}

/** @deprecated Prefer anchorDateIso() — kept for imports that expect a const name. */
export const ANCHOR_DATE = anchorDateIso();
export const MASS_EMAIL_CONTRACT_IMPORT_TYPE = 'MASS_IMPORT_OF_CONTRACTS';
export const MASS_SMS_CONTRACT_IMPORT_TYPE = 'MASS_SMS_CONTRACT';

export const ERR_RESPECTIVE_VERSION_NOT_FOUND = 'Respective contract version not found';
export const ERR_NO_VALID_PRICE_COMPONENT =
  'there is not valid price component for used price component tag in calculation';
export const ERR_DUPLICATE_PRICE_COMPONENT =
  'more then 1 price component for used price component tag in calculation';

/** Phoenix `EmailCommunicationService` appends `!` before `;` (see MassCommunicationImportProcessResult). */
export const ERR_EXPLICIT_NOT_SIGNED =
  "doesn't exist or is not in status Valid/Signed by both sides";
export const ERR_NO_SIGNED_VERSION = 'has no version in status Valid/Signed by both sides';

/** Penalty amount = resolved version's estimatedTotalConsumptionUnderContractKwh (AvgAnnual evaluator). */
export const PDT2815_PENALTY_FORMULA_CONSUMPTION = '$AVERAGE_ANNUAL_ESTIMATED_CONSUMPTION_UNDER_CONTRACT$';
/**
 * Yearly kWh on contract detail — must equal sum(pod monthly integers) * 12 / 1000 (Phoenix validation).
 * v3 > v2 so penalty amount increases between versions.
 */
export const PDT2815_CONSUMPTION_KWH_V2 = 240;
export const PDT2815_CONSUMPTION_KWH_V3 = 651;

/** Integer monthly avg on POD so contract yearly field matches Phoenix formula. */
export function podMonthlyAvgForYearlyContractKwh(yearlyKwh: number): number {
  return Math.round((yearlyKwh * 1000) / 12);
}

export type Pdt2815MassEmailFixture = {
  contractId: number;
  contractIndex: number;
  contractNumber: string;
  customerIdentifier: string;
  customerId: number;
  signedV2DetailId: number | null;
  /** Date inside SIGNED v2 window (often v2.startDate when v1 was cancelled). */
  v2ExecutionDate: string;
  contractPayload: Record<string, unknown>;
};

/** Execution date that falls in the SIGNED logical version window (use for penalty/Mass Email evidence). */
export function signedVersionExecutionDate(
  body: Record<string, unknown>,
  logicalVersionId: number,
): string {
  const row = versionRow(body, logicalVersionId);
  return String(row?.startDate ?? ANCHOR_DATE);
}

export type MassCommImportBody = {
  results?: {
    customerIdentifier?: string;
    customerVersionId?: number;
    productContractDetailId?: number;
    serviceContractDetailId?: number;
  }[];
  popupMessage?: string | null;
};

export type MassEmailImportBody = MassCommImportBody;
export type MassSmsImportBody = MassCommImportBody;

export type Pdt2815PenaltyGapFixture = Pdt2815MassEmailFixture & {
  penaltyId: number;
  actionTypeId: number;
  terminationId: number;
  podRespectiveId: number;
  podFutureOnlyId: number;
  respectiveDetailId: number | null;
  futureDetailId: number | null;
  /** Yearly consumption (kWh) stamped on v2 / v3 — penalty formula uses version-specific value. */
  consumptionKwhV2?: number;
  consumptionKwhV3?: number;
};

// ─── Date helpers ───────────────────────────────────────────────────────────

export function addDaysIso(isoDate: string, days: number): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().split('T')[0];
}

function compareIsoDates(a: string, b: string): number {
  return a.localeCompare(b);
}

function ensureStartDateAfterFirstVersion(requestedStart: string, firstVersionStart: string): string {
  if (compareIsoDates(requestedStart, firstVersionStart) > 0) {
    return requestedStart;
  }
  return addDaysIso(firstVersionStart, 1);
}

function firstVersionStartDate(body: Record<string, unknown>): string {
  const versions = (body.versions ?? []) as { versionId?: number; startDate?: string }[];
  const v1 = versions.find((v) => Number(v.versionId) === 1);
  return String(v1?.startDate ?? ANCHOR_DATE);
}

type ProductContractVersionRow = {
  versionId?: number;
  id?: number;
  startDate?: string;
  versionStatus?: string;
  contractVersionStatus?: string;
};

export function versionStatusOf(row: ProductContractVersionRow | undefined): string | undefined {
  return (
    row?.versionStatus ??
    row?.contractVersionStatus ??
    (row as { status?: string } | undefined)?.status
  );
}

/** Dev GET maps DB `SIGNED` to display label `Valid` (see ProductContractDetailsRepository). */
export function isEligibleSignedVersionStatus(status: string | undefined): boolean {
  return status === 'SIGNED' || status === 'Valid';
}

export function isCancelledVersionStatus(status: string | undefined): boolean {
  return status === 'CANCELLED' || status === 'Cancelled';
}

export function isDraftVersionDisplayStatus(status: string | undefined): boolean {
  return status === 'DRAFT' || status === 'Not Valid';
}

export function signingDateFromContractBody(body: Record<string, unknown>): string {
  const bp = (body.basicParameters ?? {}) as { signingDate?: string | null };
  return String(bp.signingDate ?? ANCHOR_DATE);
}

/** POD activation requires signingDate <= activationDate (ProductContractRepository.existsForActivation). */
export function activationDateOnOrAfterSigning(
  requestedActivationDate: string,
  signingDate: string,
): string {
  return compareIsoDates(requestedActivationDate, signingDate) >= 0
    ? requestedActivationDate
    : signingDate;
}

async function ensureProductContractHeaderSignedBothSides(
  Request: FixtureRequest,
  contractId: number,
  logicalVersionId: number,
): Promise<void> {
  const body = await loadProductContract(Request, contractId);
  const bp = (body.basicParameters ?? {}) as { subStatus?: string };
  const headerStatus = headerContractStatus(body);
  if (
    headerStatus === 'SIGNED' &&
    bp.subStatus !== 'SIGNED_BY_BOTH_SIDES' &&
    bp.subStatus !== 'SPECIAL_PROCESSES'
  ) {
    const res = await productContractStatusUpdate(
      Request,
      contractId,
      'SIGNED',
      'SIGNED_BY_BOTH_SIDES',
      logicalVersionId,
    );
    await expect(res).CheckResponse();
  }
}

async function withIsolatedServicePriceComponents<T>(
  Responses: baseFixture['Responses'],
  run: () => Promise<T>,
): Promise<T> {
  const savedPc = [...Responses.priceComponent];
  const savedInterim = [...Responses.interim];
  const savedPenalty = [...Responses.penalty];
  const savedTermination = [...Responses.termination];
  Responses.priceComponent = [];
  Responses.interim = [];
  Responses.penalty = [];
  Responses.termination = [];
  try {
    return await run();
  } finally {
    const chainPc = Responses.priceComponent;
    Responses.priceComponent = [...savedPc, ...chainPc];
    Responses.interim = savedInterim;
    Responses.penalty = savedPenalty;
    Responses.termination = savedTermination;
  }
}

export function headerContractStatus(body: Record<string, unknown>): string | undefined {
  const bp = (body.basicParameters ?? {}) as { status?: string; contractStatus?: string };
  return bp.status ?? bp.contractStatus;
}

/** Header states that must not be demoted to READY when signing a new version row. */
function isHeaderLockedForReadyTransition(headerStatus: string | undefined): boolean {
  return (
    headerStatus === 'SIGNED' ||
    headerStatus === 'ENTERED_INTO_FORCE' ||
    headerStatus === 'ACTIVE_IN_TERM' ||
    headerStatus === 'ACTIVE_IN_PERPETUITY'
  );
}

export function versionRow(
  body: Record<string, unknown>,
  logicalVersionId: number,
): ProductContractVersionRow | undefined {
  const versions = (body.versions ?? []) as ProductContractVersionRow[];
  return versions.find((v) => Number(v.versionId) === logicalVersionId);
}

// ─── Product contract payload / lifecycle (PDT-2854 pattern) ──────────────────

type BuildEditProductContractOpts = {
  savingAsNewVersion?: boolean;
  startDate?: string;
  forDraftV1?: boolean;
  preserveSigningDate?: boolean;
};

type ContractPodResponseRow = {
  podDetailId?: number;
  billingGroupId?: number | null;
  dealNumber?: unknown;
};

type ProductContractPodRequestPayload = {
  billingGroupId: number;
  productContractPointOfDeliveries: { pointOfDeliveryDetailId: number; dealNumber: unknown }[];
};

/** Resolve billing group id from contract pods or contract-level billingGroups (DEV may omit pod.billingGroupId). */
function contractBillingGroupIdFromBody(contractBody: Record<string, unknown>): number | undefined {
  for (const pod of (contractBody.contractPodsResponses ?? []) as ContractPodResponseRow[]) {
    if (pod.billingGroupId != null && Number.isFinite(Number(pod.billingGroupId))) {
      return Number(pod.billingGroupId);
    }
  }
  for (const group of (contractBody.billingGroups ?? []) as { id?: number }[]) {
    if (group.id != null && Number.isFinite(Number(group.id))) {
      return Number(group.id);
    }
  }
  return undefined;
}

function buildPodRequestsFromContractBody(
  contractBody: Record<string, unknown>,
): ProductContractPodRequestPayload[] {
  const fallbackBillingGroupId = contractBillingGroupIdFromBody(contractBody);
  const podsByBillingGroup = new Map<number, { pointOfDeliveryDetailId: number; dealNumber: unknown }[]>();

  for (const pod of (contractBody.contractPodsResponses ?? []) as ContractPodResponseRow[]) {
    const podDetailId = Number(pod.podDetailId);
    if (!Number.isFinite(podDetailId)) continue;

    const rawGroupId = pod.billingGroupId ?? fallbackBillingGroupId;
    if (rawGroupId == null || !Number.isFinite(Number(rawGroupId))) continue;

    const billingGroupId = Number(rawGroupId);
    if (!podsByBillingGroup.has(billingGroupId)) {
      podsByBillingGroup.set(billingGroupId, []);
    }
    podsByBillingGroup.get(billingGroupId)!.push({
      pointOfDeliveryDetailId: podDetailId,
      dealNumber: pod.dealNumber ?? null,
    });
  }

  const allPods = ((contractBody.contractPodsResponses ?? []) as ContractPodResponseRow[])
    .filter((pod) => pod.podDetailId != null)
    .map((pod) => ({
      pointOfDeliveryDetailId: Number(pod.podDetailId),
      dealNumber: pod.dealNumber ?? null,
    }));

  let podRequests = Array.from(podsByBillingGroup.entries()).map(
    ([billingGroupId, productContractPointOfDeliveries]) => ({
      billingGroupId,
      productContractPointOfDeliveries,
    }),
  );

  if (podRequests.length === 0 && allPods.length > 0 && fallbackBillingGroupId != null) {
    podRequests = [
      {
        billingGroupId: fallbackBillingGroupId,
        productContractPointOfDeliveries: allPods,
      },
    ];
  }

  return podRequests;
}

export async function buildEditProductContractPayload(
  Request: FixtureRequest,
  contractId: number,
  generatedPayload: Record<string, unknown>,
  opts?: BuildEditProductContractOpts,
) {
  const contractget = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractget).CheckResponse();
  const contractgetjson = await contractget.json();

  const podRequests = buildPodRequestsFromContractBody(contractgetjson);

  const allPods = (contractgetjson.contractPodsResponses ?? []).map(
    (pod: { podDetailId: number; dealNumber: unknown }) => ({
      pointOfDeliveryDetailId: pod.podDetailId,
      dealNumber: pod.dealNumber,
    }),
  );

  const serverStartDate = contractgetjson.versions[0].startDate as string;
  const startDate = opts?.forDraftV1 ? serverStartDate : (opts?.startDate ?? serverStartDate);

  let signingDate: string | null;
  if (opts?.forDraftV1) {
    signingDate = null;
  } else if (opts?.preserveSigningDate) {
    signingDate = (contractgetjson.basicParameters?.signingDate as string | null) ?? null;
  } else {
    signingDate = (contractgetjson.basicParameters?.signingDate as string | null) ?? ANCHOR_DATE;
  }

  const editPayload: Record<string, unknown> = {
    ...generatedPayload,
    basicParameters: {
      ...(generatedPayload.basicParameters as object),
      status: contractgetjson.basicParameters.status,
      subStatus: contractgetjson.basicParameters.subStatus,
      signingDate,
    },
    productContractPointOfDeliveries: allPods,
    savingAsNewVersion: opts?.savingAsNewVersion ?? false,
    startDate,
    podRequests,
  };

  if (editPayload.additionalParameters) {
    (editPayload.additionalParameters as Record<string, unknown>).riskAssessment = 'PERMIT';
    (editPayload.additionalParameters as Record<string, unknown>).employeeId = 154;
  }

  return editPayload;
}

export async function productContractStatusUpdate(
  Request: FixtureRequest,
  contractId: number,
  status: string,
  subStatus: string,
  versionId = 1,
) {
  const payload = {
    contractStatus: status,
    contractSubStatus: subStatus,
    contractVersionStatus: 'SIGNED',
  };
  return Request.put(`product-contract/status-update/${contractId}?versionId=${versionId}`, {
    data: payload,
  });
}

export async function loadProductContract(
  Request: FixtureRequest,
  contractId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`product-contract/${contractId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function contractNumberFromBody(body: Record<string, unknown>): string {
  const bp = body.basicParameters as { contractNumber?: string } | undefined;
  return String(bp?.contractNumber ?? body.contractNumber ?? '');
}

export function versionDetailId(body: Record<string, unknown>, logicalVersionId: number): number | null {
  const versions = (body.versions ?? []) as { versionId?: number; id?: number }[];
  const row = versions.find((v) => v.versionId === logicalVersionId);
  return row?.id != null ? Number(row.id) : null;
}

export function logicalVersionIdForDetailId(
  body: Record<string, unknown>,
  detailId: number,
): number | null {
  const versions = (body.versions ?? []) as { versionId?: number; id?: number }[];
  const row = versions.find((v) => Number(v.id) === detailId);
  return row?.versionId != null ? Number(row.versionId) : null;
}

type SignedVersionRow = { detailId: number; startDate: string; logicalVersionId: number };

/** Collect SIGNED (or Valid) version rows from GET product-contract body. */
export function signedProductVersionRows(body: Record<string, unknown>): SignedVersionRow[] {
  const versions = (body.versions ?? []) as {
    versionId?: number;
    id?: number;
    startDate?: string;
    versionStatus?: string;
    contractVersionStatus?: string;
    status?: string;
  }[];
  const rows: SignedVersionRow[] = [];
  for (const v of versions) {
    if (v.id == null || !v.startDate || v.versionId == null) continue;
    if (!isEligibleSignedVersionStatus(versionStatusOf(v))) continue;
    rows.push({
      detailId: Number(v.id),
      startDate: String(v.startDate),
      logicalVersionId: Number(v.versionId),
    });
  }
  return rows.sort((a, b) => a.startDate.localeCompare(b.startDate));
}

/** Mirrors `ProductContractDetailsRepository.getContractDetailIdByExecutionDate` (SIGNED + date window). */
export function phoenixProductContractDetailIdByExecutionDate(
  body: Record<string, unknown>,
  executionDate: string,
): number | null {
  const signed = signedProductVersionRows(body);
  for (let i = 0; i < signed.length; i++) {
    const start = signed[i].startDate;
    const end =
      i + 1 < signed.length
        ? addDaysIso(signed[i + 1].startDate, -1)
        : addDaysIso('9999-12-31', -1);
    if (compareIsoDates(executionDate, start) >= 0 && compareIsoDates(executionDate, end) <= 0) {
      return signed[i].detailId;
    }
  }
  return null;
}

/** Latest SIGNED row by max `startDate` (Mass Email import without explicit version). */
export function phoenixLatestSignedProductDetailId(body: Record<string, unknown>): number | null {
  const signed = signedProductVersionRows(body);
  if (signed.length === 0) return null;
  return signed.reduce((best, row) =>
    compareIsoDates(row.startDate, best.startDate) > 0 ? row : best,
  ).detailId;
}

export type ContractDetailResolutionEvidence = {
  contractId: number;
  executionDate: string;
  expectedDetailId: number | null;
  expectedLogicalVersionId: number | null;
  phoenixMirrorDetailId: number | null;
  setupDetailIdFromLogicalVersion: number | null;
  resolvedLogicalVersionId: number | null;
  massEmailResolvedDetailId?: number | null;
  tripleMatch: boolean;
};

/**
 * Hard evidence that the system would use the expected contract_details.id for executionDate.
 * 1) Phoenix SQL mirror on fresh GET product-contract
 * 2) setup detail from logical version on same GET
 * 3) optional Mass Email productContractDetailId must match mirror
 */
export async function assertProductContractDetailResolution(
  Request: FixtureRequest,
  params: {
    contractId: number;
    executionDate: string;
    expectedLogicalVersionId: number | null;
    expectedDetailId: number | null;
    context: string;
    massEmailResolvedDetailId?: number | null;
  },
): Promise<ContractDetailResolutionEvidence> {
  const body = await loadProductContract(Request, params.contractId);
  const phoenixMirrorDetailId = phoenixProductContractDetailIdByExecutionDate(
    body,
    params.executionDate,
  );
  const setupDetailIdFromLogicalVersion =
    params.expectedLogicalVersionId != null
      ? versionDetailId(body, params.expectedLogicalVersionId)
      : null;

  expect(
    phoenixMirrorDetailId,
    `${params.context}: Phoenix mirror detail for ${params.executionDate}`,
  ).toBe(params.expectedDetailId);

  if (params.expectedDetailId != null && setupDetailIdFromLogicalVersion != null) {
    expect(
      setupDetailIdFromLogicalVersion,
      `${params.context}: GET logical version ${params.expectedLogicalVersionId} detail`,
    ).toBe(params.expectedDetailId);
  }

  const resolvedLogicalVersionId =
    phoenixMirrorDetailId != null ? logicalVersionIdForDetailId(body, phoenixMirrorDetailId) : null;

  if (params.expectedLogicalVersionId != null && phoenixMirrorDetailId != null) {
    expect(
      resolvedLogicalVersionId,
      `${params.context}: mirror detail must be logical version ${params.expectedLogicalVersionId}`,
    ).toBe(params.expectedLogicalVersionId);
  }

  if (params.massEmailResolvedDetailId != null) {
    expect(
      params.massEmailResolvedDetailId,
      `${params.context}: Mass Email productContractDetailId must match Phoenix mirror`,
    ).toBe(phoenixMirrorDetailId);
  }

  const tripleMatch =
    params.expectedDetailId != null &&
    phoenixMirrorDetailId === params.expectedDetailId &&
    (params.massEmailResolvedDetailId == null || params.massEmailResolvedDetailId === phoenixMirrorDetailId);

  const evidence: ContractDetailResolutionEvidence = {
    contractId: params.contractId,
    executionDate: params.executionDate,
    expectedDetailId: params.expectedDetailId,
    expectedLogicalVersionId: params.expectedLogicalVersionId,
    phoenixMirrorDetailId,
    setupDetailIdFromLogicalVersion,
    resolvedLogicalVersionId,
    massEmailResolvedDetailId: params.massEmailResolvedDetailId,
    tripleMatch,
  };

  await test.info().attach(`[PDT-2815] ${params.context} — contract detail resolution evidence`, {
    body: JSON.stringify(
      {
        ...evidence,
        citation:
          'Mirror: ProductContractDetailsRepository.getContractDetailIdByExecutionDate (cd.status=SIGNED, executionDate in [start, next_start-1])',
        signedVersions: signedProductVersionRows(body),
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });

  return evidence;
}

/**
 * After v2 is SIGNED, cancel v1 so ANCHOR_DATE cannot resolve the overlapping v1 window
 * (v1/v2 often share today's startDate from create; API forbids retroactive v1 startDate edits).
 */
export async function ensureOnlySignedV2ResolvesAtAnchor(
  Request: FixtureRequest,
  contractId: number,
): Promise<number> {
  await cancelProductContractVersion(Request, contractId, 1);
  const body = await loadProductContract(Request, contractId);
  const v2Detail = versionDetailId(body, 2);
  const v2ExecutionDate = signedVersionExecutionDate(body, 2);
  const mirror = phoenixProductContractDetailIdByExecutionDate(body, v2ExecutionDate);
  expect(v2Detail, 'contract must have SIGNED logical version 2').toBeTruthy();
  expect(
    mirror,
    `executionDate ${v2ExecutionDate} must resolve SIGNED v2 detail (Phoenix mirror); signedRows=${JSON.stringify(signedProductVersionRows(body))}`,
  ).toBe(v2Detail);
  expect(logicalVersionIdForDetailId(body, mirror!), 'mirror detail must be version 2').toBe(2);
  return v2Detail!;
}

export async function linkPenaltyToProductByIndex(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Endpoints: baseFixture['Endpoints'],
  Responses: baseFixture['Responses'],
  productIndex: number,
  penaltyId: number,
): Promise<void> {
  const raw = Responses.product[productIndex] as { id?: number; productId?: number };
  const productId = Number(raw?.id ?? raw?.productId ?? raw);
  const getRes = await Request.get(`${Endpoints.product}/${productId}?version=1`);
  await expect(getRes).CheckResponse();
  const productJson = (await getRes.json()) as {
    penaltyIds?: number[];
    penalties?: { id?: number }[];
  };
  const existing =
    productJson.penaltyIds ??
    (productJson.penalties ?? []).map((p) => Number(p.id)).filter((id) => Number.isFinite(id));
  const ids = new Set([...existing, penaltyId]);

  const basePayload = GeneratePayload.productAndServices.product(productIndex);
  const editPayload = await GeneratePayload.productAndServices.edit_Product(basePayload, productIndex);
  editPayload.penaltyIds = [...ids];
  const putRes = await Request.put(`${Endpoints.product}/${productId}`, { data: editPayload });
  await expect(putRes).CheckResponse();
}

async function signProductContractVersion(
  Request: FixtureRequest,
  contractId: number,
  versionId: number,
) {
  const body = await loadProductContract(Request, contractId);
  const row = versionRow(body, versionId);
  const bp = (body.basicParameters ?? {}) as { status?: string; subStatus?: string; contractStatus?: string };
  const headerStatus = headerContractStatus(body);
  const verStatus = versionStatusOf(row);

  if (
    isEligibleSignedVersionStatus(verStatus) &&
    headerStatus === 'SIGNED' &&
    bp.subStatus === 'SIGNED_BY_BOTH_SIDES'
  ) {
    return;
  }

  if (isEligibleSignedVersionStatus(verStatus) && headerStatus === 'SIGNED') {
    const signedOnly = await productContractStatusUpdate(
      Request,
      contractId,
      'SIGNED',
      'SIGNED_BY_BOTH_SIDES',
      versionId,
    );
    await expect(signedOnly).CheckResponse();
    return;
  }

  // Header already in force / active (v2+ after POD activation): promote version only — never READY or header demotion.
  if (
    isHeaderLockedForReadyTransition(headerStatus) &&
    versionId > 1 &&
    !isEligibleSignedVersionStatus(verStatus)
  ) {
    const bpLocked = (body.basicParameters ?? {}) as {
      subStatus?: string;
      contractSubStatus?: string;
    };
    const headerSub =
      bpLocked.subStatus ?? bpLocked.contractSubStatus ?? 'SIGNED_BY_BOTH_SIDES';
    const signedOnly = await productContractStatusUpdate(
      Request,
      contractId,
      headerStatus!,
      headerSub,
      versionId,
    );
    await expect(signedOnly).CheckResponse();
    return;
  }

  const ready = await productContractStatusUpdate(Request, contractId, 'READY', 'READY', versionId);
  await expect(ready).CheckResponse();
  const signed = await productContractStatusUpdate(
    Request,
    contractId,
    'SIGNED',
    'SIGNED_BY_BOTH_SIDES',
    versionId,
  );
  await expect(signed).CheckResponse();
}

export async function putProductContractNewVersion(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  contractId: number,
  basePayload: Record<string, unknown>,
  fromVersionId: number,
  opts: {
    startDate: string;
    savingAsNewVersion: boolean;
    versionStatus?: 'SIGNED' | 'DRAFT';
    podDetailIds?: number[];
    /** When true, POD rows propagate to future contract versions (multi-version F with same POD on each). */
    changeFutureVersionsPods?: boolean;
    /** Pin contract version to a specific customer detail (customerVersionId on basicParameters). */
    customerVersionId?: number;
    /** Distinct penalty driver per SIGNED version (maps to additionalParameters.estimatedTotalConsumptionUnderContractKwh). */
    estimatedConsumptionKwh?: number;
  },
) {
  const contractBody = await loadProductContract(Request, contractId);
  const v1Start = firstVersionStartDate(contractBody);
  const effectiveStartDate = ensureStartDateAfterFirstVersion(opts.startDate, v1Start);
  const headerSt = headerContractStatus(contractBody);

  const editPayload = await buildEditProductContractPayload(Request, contractId, basePayload, {
    startDate: effectiveStartDate,
    savingAsNewVersion: opts.savingAsNewVersion,
    preserveSigningDate: true,
  });

  const bp = editPayload.basicParameters as Record<string, unknown>;
  const pp = editPayload.productParameters as Record<string, unknown>;

  if (opts.savingAsNewVersion) {
    // New version row is DRAFT; contract header stays SIGNED (API rejects header demotion to DRAFT).
    bp.versionStatus = 'DRAFT';
    if (bp.status === 'SIGNED' && (bp.signingDate == null || bp.signingDate === '')) {
      bp.signingDate = anchorDateIso();
    }
    const headerNeedsEntryInForce =
      headerSt === 'ENTERED_INTO_FORCE' ||
      headerSt === 'ACTIVE_IN_TERM' ||
      headerSt === 'ACTIVE_IN_PERPETUITY';
    if (headerNeedsEntryInForce) {
      const entryInForce = anchorDateIso();
      bp.entryInForceDate = entryInForce;
      bp.startOfInitialTerm = entryInForce;
      pp.entryIntoForceValue = entryInForce;
      pp.startOfContractValue = entryInForce;
    } else {
      bp.entryInForceDate = null;
      bp.startOfInitialTerm = null;
      pp.entryIntoForceValue = null;
      pp.startOfContractValue = null;
    }
  } else {
    bp.status = 'DRAFT';
    bp.subStatus = 'DRAFT';
    bp.versionStatus = 'DRAFT';
    bp.signingDate = null;
    bp.entryInForceDate = null;
    bp.startOfInitialTerm = null;
    pp.entryIntoForceValue = null;
    pp.startOfContractValue = null;
  }

  if (opts.podDetailIds) {
    const productContractPointOfDeliveries = opts.podDetailIds.map((podDetailId) => ({
      pointOfDeliveryDetailId: podDetailId,
      dealNumber: null,
    }));
    editPayload.productContractPointOfDeliveries = productContractPointOfDeliveries;

    const existingPodRequests = (editPayload.podRequests ?? []) as ProductContractPodRequestPayload[];
    const billingGroupId =
      existingPodRequests.find((row) => row.billingGroupId != null)?.billingGroupId ??
      contractBillingGroupIdFromBody(contractBody);

    expect(
      billingGroupId,
      `product contract ${contractId} must expose billingGroupId before PUT with podDetailIds`,
    ).toBeTruthy();

    editPayload.podRequests = [
      {
        billingGroupId: Number(billingGroupId),
        productContractPointOfDeliveries,
      },
    ];
  }

  if (opts.estimatedConsumptionKwh != null) {
    const ap = editPayload.additionalParameters as Record<string, unknown>;
    ap.estimatedTotalConsumptionUnderContractKwh = opts.estimatedConsumptionKwh;
  }

  if (opts.customerVersionId != null) {
    (editPayload.basicParameters as Record<string, unknown>).customerVersionId = opts.customerVersionId;
  }

  const changeFutureVersionsPods = opts.changeFutureVersionsPods ?? false;
  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${fromVersionId}&changeFutureVersionsPods=${changeFutureVersionsPods}`,
    { data: editPayload },
  );
  await expect(put).CheckResponse();

  if (opts.versionStatus === 'SIGNED') {
    const body = await loadProductContract(Request, contractId);
    const versions = (body.versions ?? []) as { versionId?: number }[];
    const latest = Math.max(...versions.map((v) => Number(v.versionId ?? 0)));
    await signProductContractVersion(Request, contractId, latest);
  }
}

async function cancelProductContractVersion(
  Request: FixtureRequest,
  contractId: number,
  logicalVersionId: number,
) {
  const body = await loadProductContract(Request, contractId);
  const bp = (body.basicParameters ?? {}) as {
    status?: string;
    subStatus?: string;
    contractStatus?: string;
    contractSubStatus?: string;
  };
  const put = await Request.put(`product-contract/status-update/${contractId}?versionId=${logicalVersionId}`, {
    data: {
      contractStatus: headerContractStatus(body) ?? bp.status ?? bp.contractStatus ?? 'SIGNED',
      contractSubStatus: bp.subStatus ?? bp.contractSubStatus ?? 'SIGNED_BY_BOTH_SIDES',
      contractVersionStatus: 'CANCELLED',
    },
  });
  await expect(put).CheckResponse();
}

/**
 * PDT-2854 v1 chain: POST DRAFT → READY → SIGNED → PUT v1 (attach PODs).
 * Never POST default SIGNED then attempt READY on an already-signed version.
 */
async function createProductContractV1Signed(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  productIndex: number = Math.max(0, Responses.product.length - 1),
): Promise<{ contractId: number; contractPayload: Record<string, unknown>; contractIndex: number }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    productIndex,
  )) as Record<string, unknown>;
  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';

  const res = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(res).CheckResponse();
  const body = await res.json();
  Responses.productContract.push(body);

  const contractId = body.id as number;
  const contractIndex = Responses.productContract.length - 1;

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
    savingAsNewVersion: false,
    preserveSigningDate: false,
  });
  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await expect(put).CheckResponse();

  return { contractId, contractPayload, contractIndex };
}

async function resolveCustomerFields(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  Responses: baseFixture['Responses'],
): Promise<{ customerIdentifier: string; customerId: number }> {
  const custGet = await Request.get(`${Endpoints.customer}/${Responses.customer[0].id}`);
  await expect(custGet).CheckResponse();
  const customerJson = await custGet.json();
  return {
    customerIdentifier: String(customerJson.identifier),
    customerId: Number(customerJson.customerId ?? Responses.customer[0].id),
  };
}

function massEmailFixtureFromContract(
  contractId: number,
  contractIndex: number,
  contractPayload: Record<string, unknown>,
  body: Record<string, unknown>,
  customerIdentifier: string,
  customerId: number,
  signedV2DetailId: number | null,
  v2ExecutionDate?: string,
): Pdt2815MassEmailFixture {
  return {
    contractId,
    contractIndex,
    contractNumber: contractNumberFromBody(body),
    customerIdentifier,
    customerId,
    signedV2DetailId,
    v2ExecutionDate:
      v2ExecutionDate ??
      (signedV2DetailId != null ? signedVersionExecutionDate(body, 2) : ANCHOR_DATE),
    contractPayload,
  };
}

// ─── Shared preconditions (test data steps 1–6) ─────────────────────────────

export type PreconditionSharedOptions = {
  /** Align POD monthly avg with Phoenix yearly consumption validation on contract PUT. */
  podYearlyConsumptionKwh?: number;
};

export async function preconditionSharedEntities(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  options?: PreconditionSharedOptions,
) {
  const resTerms = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(resTerms).CheckResponse();
  Responses.terms.push(await resTerms.json());

  const resPc = await Request.post(Endpoints.priceComponent, {
    data: GeneratePayload.productAndServices.electricity(),
  });
  await expect(resPc).CheckResponse();
  Responses.priceComponent.push(await resPc.json());

  if (Responses.penalty.length === 0) {
    const penaltyRes = await Request.post(Endpoints.penalty, {
      data: GeneratePayload.productAndServices.penalty(),
    });
    await expect(penaltyRes).CheckResponse();
    Responses.penalty.push(await penaltyRes.json());
  }

  if (Responses.termination.length === 0) {
    const termRes = await Request.post(Endpoints.termination, {
      data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_NOTICE'),
    });
    await expect(termRes).CheckResponse();
    Responses.termination.push(await termRes.json());
  }

  const productPayload = GeneratePayload.productAndServices.product();
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  const resProduct = await Request.post(Endpoints.product, { data: productPayload });
  await expect(resProduct).CheckResponse();
  Responses.product.push(await resProduct.json());

  const resCustomer = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_private(),
  });
  await expect(resCustomer).CheckResponse();
  Responses.customer.push(await resCustomer.json());

  const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();
  if (options?.podYearlyConsumptionKwh != null) {
    podPayload.estimatedMonthlyAvgConsumption = String(
      podMonthlyAvgForYearlyContractKwh(options.podYearlyConsumptionKwh),
    );
  }
  const resPod = await Request.post(Endpoints.pod, { data: podPayload });
  await expect(resPod).CheckResponse();
  Responses.pod.push(await resPod.json());
}

// ─── Canonical happy path + extensions ────────────────────────────────────

/** v1 SIGNED only (no v2). Requires `preconditionSharedEntities` first. */
export async function buildSignedV1Only(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<Pdt2815MassEmailFixture> {
  const { contractId, contractPayload, contractIndex } = await createProductContractV1Signed(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  );
  const body = await loadProductContract(Request, contractId);
  const { customerIdentifier, customerId } = await resolveCustomerFields(Request, Endpoints, Responses);

  return massEmailFixtureFromContract(
    contractId,
    contractIndex,
    contractPayload,
    body,
    customerIdentifier,
    customerId,
    null,
  );
}

/** v1 SIGNED + v2 SIGNED (mass email happy path). Requires `preconditionSharedEntities` first. */
export async function buildHappyPathSignedV1AndV2(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<Pdt2815MassEmailFixture> {
  const { contractId, contractPayload, contractIndex } = await createProductContractV1Signed(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  );

  await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 1, {
    startDate: ANCHOR_DATE,
    savingAsNewVersion: true,
    versionStatus: 'SIGNED',
  });

  const body = await loadProductContract(Request, contractId);
  const { customerIdentifier, customerId } = await resolveCustomerFields(Request, Endpoints, Responses);

  return massEmailFixtureFromContract(
    contractId,
    contractIndex,
    contractPayload,
    body,
    customerIdentifier,
    customerId,
    versionDetailId(body, 2),
  );
}

/** v1 SIGNED + v2 DRAFT (TC-BE-2). */
export async function extendWithDraftV2Only(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<Pdt2815MassEmailFixture> {
  const { contractId, contractPayload, contractIndex } = await createProductContractV1Signed(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  );

  await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 1, {
    startDate: addDaysIso(ANCHOR_DATE, 5),
    savingAsNewVersion: true,
    versionStatus: 'DRAFT',
  });

  const body = await loadProductContract(Request, contractId);
  const { customerIdentifier, customerId } = await resolveCustomerFields(Request, Endpoints, Responses);

  return massEmailFixtureFromContract(
    contractId,
    contractIndex,
    contractPayload,
    body,
    customerIdentifier,
    customerId,
    null,
  );
}

/** Happy path (v1+v2 SIGNED) + v3 DRAFT newer startDate (TC-BE-3). */
export async function extendWithDraftVersion3(
  fixture: Pdt2815MassEmailFixture,
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
): Promise<{ fixture: Pdt2815MassEmailFixture; draftV3DetailId: number | null }> {
  await putProductContractNewVersion(
    Request,
    Endpoints,
    fixture.contractId,
    fixture.contractPayload,
    2,
    {
      startDate: addDaysIso(ANCHOR_DATE, 5),
      savingAsNewVersion: true,
      versionStatus: 'DRAFT',
    },
  );

  const body = await loadProductContract(Request, fixture.contractId);
  const draftV3DetailId = versionDetailId(body, 3);

  return {
    fixture: {
      ...fixture,
      signedV2DetailId: versionDetailId(body, 2),
    },
    draftV3DetailId,
  };
}

/** v1 SIGNED then CANCELLED — no remaining SIGNED versions (TC-BE-4). */
export async function cancelAllSignedVersions(
  fixture: Pdt2815MassEmailFixture,
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
): Promise<Pdt2815MassEmailFixture> {
  await cancelProductContractVersion(Request, fixture.contractId, 1);
  await assertNoSignedProductContractVersions(Request, fixture.contractId);
  return { ...fixture, signedV2DetailId: null };
}

// ─── Mass email import helpers ──────────────────────────────────────────────

export function massImportErrorText(body: MassEmailImportBody): string {
  return body.popupMessage ?? '';
}

/** Mass SMS/email import rejection — popupMessage and/or empty results (SMS parse uses same DTO). */
export function assertMassCommImportRejected(
  importBody: MassCommImportBody,
  expectedFragments: string[],
  context: string,
): void {
  const errorText = massImportErrorText(importBody);
  const results = importBody.results ?? [];
  if (errorText) {
    assertMassImportShowsError(importBody, expectedFragments, context);
    return;
  }
  expect(
    results.length,
    `${context}: expected import rejection (popupMessage or zero results); got ${results.length} row(s)`,
  ).toBe(0);
}

export function assertMassImportShowsError(
  importBody: MassEmailImportBody,
  expectedFragments: string[],
  context: string,
): void {
  const errorText = massImportErrorText(importBody);
  const missing = expectedFragments.filter((frag) => !errorText.includes(frag));
  if (missing.length > 0) {
    test.info().attach(`[PDT-2815] ${context} — full importBody`, {
      body: JSON.stringify(importBody, null, 2),
      contentType: 'application/json',
    });
  }
  for (const frag of expectedFragments) {
    expect(errorText, `${context}: popupMessage must contain "${frag}"`).toContain(frag);
  }
}

export async function assertProductContractVersionStatus(
  Request: FixtureRequest,
  contractId: number,
  logicalVersionId: number,
  expectedStatus: string,
): Promise<void> {
  const body = await loadProductContract(Request, contractId);
  const row = versionRow(body, logicalVersionId);
  const actual = versionStatusOf(row);
  if (expectedStatus === 'SIGNED') {
    expect(
      isEligibleSignedVersionStatus(actual),
      `version ${logicalVersionId} must be Valid/SIGNED (got ${actual})`,
    ).toBe(true);
    return;
  }
  if (expectedStatus === 'DRAFT') {
    expect(
      isDraftVersionDisplayStatus(actual) || !isEligibleSignedVersionStatus(actual),
      `version ${logicalVersionId} must be DRAFT/Not Valid (got ${actual})`,
    ).toBe(true);
    return;
  }
  expect(actual, `version ${logicalVersionId} versionStatus`).toBe(expectedStatus);
}

export async function assertNoSignedProductContractVersions(
  Request: FixtureRequest,
  contractId: number,
): Promise<void> {
  const body = await loadProductContract(Request, contractId);
  const versions = (body.versions ?? []) as { versionStatus?: string }[];
  const signed = versions.filter((v) => isEligibleSignedVersionStatus(versionStatusOf(v)));
  expect(signed, 'contract must have no SIGNED/Valid detail versions').toHaveLength(0);
}

export async function uploadMassEmailContractImport(
  Request: FixtureRequest,
  FileUploadRequest: FixtureFileUpload,
  Endpoints: baseFixture['Endpoints'],
  rows: { contractNumber: string; version?: string }[],
): Promise<MassEmailImportBody> {
  const templateRes = await Request.get(
    `${Endpoints.email}/mass-import-template/${MASS_EMAIL_CONTRACT_IMPORT_TYPE}`,
  );
  await expect(templateRes).CheckResponse();
  const templateBuffer = await templateRes.body();

  const outputDir = path.resolve(__dirname, '../../mass-imports/output');
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filePath = path.join(outputDir, `pdt-2815-email-contract-${stamp}.xlsx`);
  fs.writeFileSync(filePath, templateBuffer);

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(filePath);
  const worksheet = workbook.getWorksheet(1);
  if (!worksheet) {
    throw new Error('Mass email contract template has no worksheet');
  }

  rows.forEach((row, index) => {
    const excelRow = index + 2;
    worksheet.getCell(`A${excelRow}`).value = row.contractNumber;
    if (row.version != null && row.version !== '') {
      worksheet.getCell(`B${excelRow}`).value = row.version;
    }
  });

  await workbook.xlsx.writeFile(filePath);
  const fileBuffer = fs.readFileSync(filePath);

  const upload = await FileUploadRequest.post(
    `${Endpoints.email}/customers-import?massEmailCommunicationImportType=${MASS_EMAIL_CONTRACT_IMPORT_TYPE}`,
    {
      multipart: {
        file: {
          name: path.basename(filePath),
          mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          buffer: fileBuffer,
        },
      },
    },
  );
  await expect(upload).CheckResponse();
  const body = (await upload.json()) as MassEmailImportBody;

  try {
    fs.unlinkSync(filePath);
  } catch {
    /* ignore cleanup */
  }

  return body;
}

// ─── Penalty gap setup (TC-BE-7) ────────────────────────────────────────────

async function resolvePodDetailId(
  Request: FixtureRequest,
  podIndex: number,
  Responses: baseFixture['Responses'],
): Promise<number> {
  const pod = Responses.pod[podIndex];
  const fromRow = pod?.podDetailId ?? pod?.lastPodDetailId;
  if (fromRow != null) return Number(fromRow);
  const podGet = await Request.get(`pod/${pod.id}`);
  await expect(podGet).CheckResponse();
  const body = await podGet.json();
  return Number(body.podDetailId ?? body.lastPodDetailId ?? body.versions?.[0]?.podDetailId);
}

export async function activatePodOnContract(
  Request: FixtureRequest,
  Responses: baseFixture['Responses'],
  contractId: number,
  logicalVersionId: number,
  activationDate: string,
  podIndex = 0,
  deactivationDate: string | null = null,
) {
  const body = await loadProductContract(Request, contractId);
  await ensureProductContractHeaderSignedBothSides(Request, contractId, logicalVersionId);

  const contractDetailId = versionDetailId(body, logicalVersionId);
  expect(
    contractDetailId,
    `contract ${contractId} version ${logicalVersionId} detail id`,
  ).toBeTruthy();

  const signingDate = signingDateFromContractBody(body);
  const versionStart = String(versionRow(body, logicalVersionId)?.startDate ?? signingDate);
  const effectiveActivationDate = activationDateOnOrAfterSigning(
    activationDateOnOrAfterSigning(activationDate, signingDate),
    versionStart,
  );

  const podId = Number(Responses.pod[podIndex].id);
  const podRow = (
    (body.contractPodsResponses ?? []) as {
      podId?: number;
      identifier?: string;
      podDetailId?: number;
    }[]
  ).find((r) => Number(r.podId) === podId);
  expect(podRow, `POD index ${podIndex} must exist on contract ${contractId}`).toBeTruthy();

  const activation = await Request.post('/contract-pods/manual', {
    data: {
      identifier: podRow!.identifier,
      podDetailId: podRow!.podDetailId,
      contractDetailId,
      activationDate: effectiveActivationDate,
      deactivationDate,
      deactivationPurposeId: deactivationDate ? envVariables.deactivation_reason : null,
    },
  });
  await expect(activation).CheckResponse();
}

/** v2 SIGNED (respective POD) + v3 SIGNED future with second POD only — penalty gap probe. */
export async function buildPenaltyGapSignedV2V3(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<Pdt2815PenaltyGapFixture> {
  const base = await buildHappyPathSignedV1AndV2(Request, GeneratePayload, Responses, Endpoints);

  const pod2Res = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod2Res).CheckResponse();
  Responses.pod.push(await pod2Res.json());

  await activatePodOnContract(
    Request,
    Responses,
    base.contractId,
    2,
    addDaysIso(ANCHOR_DATE, -15),
    0,
  );

  const pod2DetailId = await resolvePodDetailId(Request, 1, Responses);
  await putProductContractNewVersion(Request, Endpoints, base.contractId, base.contractPayload, 2, {
    startDate: addDaysIso(ANCHOR_DATE, 30),
    savingAsNewVersion: true,
    versionStatus: 'SIGNED',
    podDetailIds: [pod2DetailId],
  });

  const bodyAfterV3 = await loadProductContract(Request, base.contractId);
  const pod2Row = (
    (bodyAfterV3.contractPodsResponses ?? []) as {
      podId?: number;
      identifier?: string;
      podDetailId?: number;
    }[]
  ).find((r) => Number(r.podId) === Number(Responses.pod[1].id));
  expect(pod2Row, 'POD 2 must appear on contract after v3 save').toBeTruthy();

  const signingDate = signingDateFromContractBody(bodyAfterV3);
  const futureActivationDate = activationDateOnOrAfterSigning(addDaysIso(ANCHOR_DATE, 30), signingDate);
  await activatePodOnContract(
    Request,
    Responses,
    base.contractId,
    3,
    futureActivationDate,
    1,
  );

  const body = await loadProductContract(Request, base.contractId);

  const prereq = await resolvePenaltyPrerequisites(Request, GeneratePayload, Responses, Endpoints);
  const { customerId } = await resolveCustomerFields(Request, Endpoints, Responses);

  return {
    ...base,
    customerId,
    ...prereq,
    podRespectiveId: Responses.pod[0].id,
    podFutureOnlyId: Responses.pod[1].id,
    respectiveDetailId: versionDetailId(body, 2),
    futureDetailId: versionDetailId(body, 3),
  };
}

export type PenaltyCalcJson = {
  amount?: number;
  infoErrorMessages?: string[];
  empty?: boolean;
  notEmpty?: boolean;
};

export async function resolvePenaltyPrerequisites(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<{ penaltyId: number; actionTypeId: number; terminationId: number }> {
  if (Responses.penalty.length === 0) {
    const penaltyRes = await Request.post(Endpoints.penalty, {
      data: GeneratePayload.productAndServices.penalty(),
    });
    await expect(penaltyRes).CheckResponse();
    Responses.penalty.push(await penaltyRes.json());
  }

  if (Responses.termination.length === 0) {
    const termRes = await Request.post(Endpoints.termination, {
      data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_NOTICE'),
    });
    await expect(termRes).CheckResponse();
    Responses.termination.push(await termRes.json());
  }

  const penaltyJson = Responses.penalty[Responses.penalty.length - 1] as {
    id: number;
    actionTypeList?: number[];
  };

  return {
    penaltyId: penaltyJson.id,
    actionTypeId: (penaltyJson.actionTypeList ?? [4])[0],
    terminationId: Responses.termination[Responses.termination.length - 1].id,
  };
}

/** Mass-comm SQL requires `contracts.contract_status` ACTIVE_IN_TERM / ACTIVE_IN_PERPETUITY. */
export async function ensureProductContractActiveInTerm(
  Request: FixtureRequest,
  Responses: baseFixture['Responses'],
  contractId: number,
  logicalVersionId = 2,
) {
  await activatePodOnContract(
    Request,
    Responses,
    contractId,
    logicalVersionId,
    addDaysIso(ANCHOR_DATE, -15),
    0,
  );
  const body = await loadProductContract(Request, contractId);
  const headerSt = headerContractStatus(body);
  expect(
    headerSt === 'ACTIVE_IN_TERM' ||
      headerSt === 'ACTIVE_IN_PERPETUITY' ||
      headerSt === 'ENTERED_INTO_FORCE',
    `product contract ${contractId} should be in force after POD activation (got ${headerSt})`,
  ).toBeTruthy();
}

async function resolveCustomerCommunicationIds(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  customerId: number,
): Promise<{ billing: number; contract: number }> {
  const custGet = await Request.get(`${Endpoints.customer}/${customerId}?version=1`);
  await expect(custGet).CheckResponse();
  const custJson = (await custGet.json()) as {
    lastCustomerDetailId?: number;
    communicationData?: { id: number }[];
  };
  const customerDetailsId = custJson.lastCustomerDetailId;
  expect(customerDetailsId, 'customer detail id for communication lookup').toBeTruthy();

  const billingRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
    params: { customerDetailsId, communicationDataType: 'BILLING' },
  });
  await expect(billingRes).CheckResponse();
  const billingRows = (await billingRes.json()) as { id: number }[];

  const contractCommRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
    params: { customerDetailsId, communicationDataType: 'CONTRACT' },
  });
  await expect(contractCommRes).CheckResponse();
  const contractRows = (await contractCommRes.json()) as { id: number }[];

  const fallbackBilling = custJson.communicationData?.[0]?.id;
  const fallbackContract = custJson.communicationData?.[1]?.id ?? fallbackBilling;
  return {
    billing: billingRows[0]?.id ?? fallbackBilling!,
    contract: contractRows[0]?.id ?? fallbackContract!,
  };
}

/** Signed service contract for a specific customer row (not always `Responses.customer[0]`). */
export async function buildSignedServiceContractForCustomerIndex(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  customerIndex: number,
  podIndex: number,
): Promise<{ contractId: number; customerIdentifier: string }> {
  const customerRow = Responses.customer[customerIndex] as { id: number };
  const podId = Number(Responses.pod[podIndex].id);
  const comm = await resolveCustomerCommunicationIds(Request, Endpoints, customerRow.id);

  let serviceId: number;
  if (Responses.service.length === 0) {
    const servicePayload = GeneratePayload.productAndServices.service();
    servicePayload.penalties = [];
    servicePayload.penaltyGroups = [];
    servicePayload.terminations = [];
    servicePayload.terminationGroups = [];
    const serviceRes = await Request.post(Endpoints.service, { data: servicePayload });
    await expect(serviceRes).CheckResponse();
    Responses.service.push(await serviceRes.json());
  }
  serviceId = asServiceId(Responses.service[0]);

  const serviceViewRes = await Request.get(`${Endpoints.service}/${serviceId}?version=1`);
  await expect(serviceViewRes).CheckResponse();
  const serviceView = await serviceViewRes.json();
  const activeVersion = (serviceView.versions ?? []).find((v: { status?: string }) => v.status === 'ACTIVE');
  expect(activeVersion?.id).toBeTruthy();
  const serviceVersionId = activeVersion!.id as number;
  const serviceDetailId = activeVersion!.detailId as number;

  const thirdRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
    params: { serviceDetailId },
  });
  await expect(thirdRes).CheckResponse();
  const third = (await thirdRes.json()) as Record<string, unknown>;
  const termsCatalog = third.serviceContractTerms as { id: number }[];
  const invoiceTermsCatalog = third.invoicePaymentTerms as { id: number; value: number }[];
  expect(termsCatalog?.length).toBeGreaterThan(0);
  expect(invoiceTermsCatalog?.length).toBeGreaterThan(0);

  let formulaSource: unknown = null;
  if (Responses.priceComponent.length > 0) {
    const pcId = asPriceComponentId(Responses.priceComponent[Responses.priceComponent.length - 1]);
    const pcRes = await Request.get(`${Endpoints.priceComponent}/${pcId}`);
    if (pcRes.ok()) formulaSource = await pcRes.json();
  } else {
    const priceRes = await Request.post(Endpoints.priceComponent, {
      data: GeneratePayload.productAndServices.perPiece(),
    });
    await expect(priceRes).CheckResponse();
    const raw = await priceRes.json();
    Responses.priceComponent.push(asPriceComponentId(raw));
    if (priceRes.ok()) {
      const pcRes = await Request.get(`${Endpoints.priceComponent}/${asPriceComponentId(raw)}`);
      if (pcRes.ok()) formulaSource = await pcRes.json();
    }
  }

  const formula = await resolvePdt2599ContractFormulaForPost(Request, Endpoints, third, formulaSource, []);

  const payload = await GeneratePayload.contractsAndOrders.serviceContract();
  const custGet = await Request.get(`${Endpoints.customer}/${customerRow.id}?version=1`);
  await expect(custGet).CheckResponse();
  const custJson = await custGet.json();

  payload.basicParameters.customerId = custJson.customerId ?? customerRow.id;
  payload.basicParameters.customerVersionId = custJson.customerVersionId ?? custJson.versionId;
  payload.basicParameters.communicationDataForBilling = comm.billing;
  payload.basicParameters.communicationDataForContract = comm.contract;
  payload.basicParameters.serviceVersionId = serviceVersionId;
  payload.basicParameters.contractStatus = 'DRAFT';
  payload.basicParameters.detailsSubStatus = 'DRAFT';
  payload.basicParameters.contractVersionStatus = 'SIGNED';
  payload.basicParameters.signInDate = null as unknown as string;
  payload.basicParameters.entryIntoForceDate = null;
  payload.serviceParameters.contractTermId = termsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTermId = invoiceTermsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTerm = invoiceTermsCatalog[0].value;
  payload.serviceParameters.contractFormulas = formula
    ? [{ formulaVariableId: formula.formulaVariableId, value: formula.value }]
    : [];
  payload.serviceParameters.podIds = [podId];
  payload.serviceParameters.entryIntoForceDate = null as unknown as string;
  payload.serviceParameters.startOfContractInitialTermDate = null as unknown as string;

  const createRes = await Request.post(Endpoints.serviceContract, { data: payload });
  await expect(createRes).CheckResponse();
  const contractId = Number(await createRes.json());
  Responses.serviceContract.push({ id: contractId });

  await ensureServiceContractActiveInTerm(Request, Endpoints, contractId);

  return {
    contractId,
    customerIdentifier: String(custJson.identifier),
  };
}

/** Mass-comm eligibility: service `contract_status` ACTIVE_IN_TERM + SIGNED detail covering today. */
export async function ensureServiceContractActiveInTerm(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  contractId: number,
) {
  const versionId = 1;
  const signedRes = await Request.put(`${Endpoints.serviceContract}/status-update/${contractId}`, {
    params: { versionId },
    data: {
      contractStatus: 'SIGNED',
      contractSubStatus: 'SIGNED_BY_BOTH_SIDES',
      contractVersionStatus: 'SIGNED',
    },
  });
  await expect(signedRes).CheckResponse();

  const activeRes = await Request.put(`${Endpoints.serviceContract}/status-update/${contractId}`, {
    params: { versionId },
    data: {
      contractStatus: 'ACTIVE_IN_TERM',
      contractSubStatus: 'DELIVERY',
      contractVersionStatus: 'SIGNED',
    },
  });
  if (!activeRes.ok()) {
    const eifRes = await Request.put(`${Endpoints.serviceContract}/status-update/${contractId}`, {
      params: { versionId },
      data: {
        contractStatus: 'ENTERED_INTO_FORCE',
        contractSubStatus: 'AWAITING_ACTIVATION',
        contractVersionStatus: 'SIGNED',
      },
    });
    await expect(eifRes).CheckResponse();
    const activeRetry = await Request.put(`${Endpoints.serviceContract}/status-update/${contractId}`, {
      params: { versionId },
      data: {
        contractStatus: 'ACTIVE_IN_TERM',
        contractSubStatus: 'DELIVERY',
        contractVersionStatus: 'SIGNED',
      },
    });
    await expect(activeRetry).CheckResponse();
  }

  const detail = await loadServiceContract(Request, contractId);
  const headerSt = (detail.basicParameters as { contractStatus?: string })?.contractStatus;
  expect(
    headerSt === 'ACTIVE_IN_TERM' || headerSt === 'ACTIVE_IN_PERPETUITY',
    `service contract ${contractId} should reach ACTIVE_IN_TERM (got ${headerSt})`,
  ).toBeTruthy();
}

export async function calculatePenalty(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  query: Parameters<typeof getCalculatePenaltyAmount>[2],
): Promise<{ response: Awaited<ReturnType<FixtureRequest['get']>>; body: PenaltyCalcJson }> {
  const response = await getCalculatePenaltyAmount(Request, Endpoints, query);
  let body: PenaltyCalcJson = {};
  try {
    body = (await response.json()) as PenaltyCalcJson;
  } catch {
    body = {};
  }
  return { response, body };
}

export function assertPenaltyMessagesContain(body: PenaltyCalcJson, fragments: string[], context: string) {
  const text = (body.infoErrorMessages ?? []).join(' ');
  for (const frag of fragments) {
    expect(text, `${context}: infoErrorMessages must contain "${frag}"`).toContain(frag);
  }
}

export function penaltyAmountFingerprint(body: PenaltyCalcJson): string {
  return JSON.stringify({
    amount: body.amount ?? null,
    notEmpty: body.notEmpty ?? null,
    empty: body.empty ?? null,
    errors: body.infoErrorMessages ?? [],
  });
}

export function penaltyNumericAmount(body: PenaltyCalcJson): number | null {
  if (body.amount == null || body.amount === '') return null;
  const n = Number(body.amount);
  return Number.isFinite(n) ? n : null;
}

export const PDT2815_TAG_MISSING = 'PDT2815_MISSING_TAG';
export const PDT2815_TAG_DUPLICATE = 'PDT2815_DUP_TAG';

export async function setPodEstimatedMonthlyForYearlyKwh(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Endpoints: baseFixture['Endpoints'],
  Responses: baseFixture['Responses'],
  podIndex: number,
  yearlyKwh: number,
): Promise<void> {
  const monthly = podMonthlyAvgForYearlyContractKwh(yearlyKwh);
  const podId = Number((Responses.pod[podIndex] as { id?: number }).id);
  const getRes = await Request.get(`${Endpoints.pod}/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const podJson = (await getRes.json()) as {
    identifier?: string;
    name?: string;
    gridOperatorId?: number;
    versionId?: number;
    versions?: { version?: number }[];
  };
  const editPayload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
  editPayload.identifier = String(podJson.identifier);
  editPayload.name = String(podJson.name ?? editPayload.name);
  editPayload.estimatedMonthlyAvgConsumption = String(monthly);
  if (podJson.gridOperatorId != null) {
    editPayload.gridOperatorId = podJson.gridOperatorId;
  }
  editPayload.updateExistingVersion = true;
  editPayload.versionId = podJson.versionId ?? podJson.versions?.[0]?.version ?? 1;
  const putRes = await Request.put(`${Endpoints.pod}/${podId}`, { data: editPayload });
  await expect(putRes).CheckResponse();
  const refresh = await Request.get(`${Endpoints.pod}/${podId}?version=1`);
  await expect(refresh).CheckResponse();
  const refreshed = (await refresh.json()) as Record<string, unknown>;
  Responses.pod[podIndex] = { ...(Responses.pod[podIndex] as object), ...refreshed };
}

export function isPenaltyCalculated(body: PenaltyCalcJson): boolean {
  return body.notEmpty === true || (body.amount != null && Number(body.amount) > 0);
}

export function assertPenaltyNotCalculated(body: PenaltyCalcJson, context: string): void {
  expect(isPenaltyCalculated(body), `${context}: penalty must not calculate`).toBeFalsy();
}

export function assertPenaltyCalculated(body: PenaltyCalcJson, context: string): void {
  expect(isPenaltyCalculated(body), `${context}: penalty must calculate`).toBeTruthy();
}

/** Matches UI / `PenaltyFormulaEvaluator` — `$PRICE_COMPONENTS_TAGS_<tag>$`. */
export function penaltyTagFormulaVariable(tag: string): string {
  return `$PRICE_COMPONENTS_TAGS_${tag}$`;
}

export async function postPerPieceWithContractTag(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  tagForContractTemplate: string,
): Promise<number> {
  const payload = GeneratePayload.productAndServices.perPiece();
  payload.tagForContractTemplate = tagForContractTemplate;
  const res = await Request.post(Endpoints.priceComponent, { data: payload });
  await expect(res).CheckResponse();
  const raw = await res.json();
  const id = asPriceComponentId(raw);
  Responses.priceComponent.push(id);
  return id;
}

export async function postProductWithPriceComponentIds(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  priceComponentIds: number[],
): Promise<number> {
  const productPayload = GeneratePayload.productAndServices.product();
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.priceComponentIds = [...priceComponentIds];
  productPayload.penaltyIds = [];
  productPayload.penaltyGroupIds = [];
  if (Responses.terms.length > 0) {
    productPayload.termId = Responses.terms[0].id;
    productPayload.termGroupId = null;
  }
  const res = await Request.post(Endpoints.product, { data: productPayload });
  await expect(res).CheckResponse();
  Responses.product.push(await res.json());
  return Responses.product.length - 1;
}

export async function createPenaltyWithAmountFormula(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  amountFormula: string,
): Promise<{ penaltyId: number; actionTypeId: number; terminationId: number }> {
  if (Responses.termination.length === 0) {
    const termRes = await Request.post(Endpoints.termination, {
      data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_NOTICE'),
    });
    await expect(termRes).CheckResponse();
    Responses.termination.push(await termRes.json());
  }

  const penaltyPayload = GeneratePayload.productAndServices.penalty();
  penaltyPayload.amountFormula = amountFormula;
  const penaltyRes = await Request.post(Endpoints.penalty, { data: penaltyPayload });
  await expect(penaltyRes).CheckResponse();
  const penaltyJson = (await penaltyRes.json()) as { id: number; actionTypeList?: number[] };
  Responses.penalty.push(penaltyJson);

  return {
    penaltyId: penaltyJson.id,
    actionTypeId: (penaltyJson.actionTypeList ?? [4])[0],
    terminationId: Responses.termination[Responses.termination.length - 1].id,
  };
}

export type Pdt2815PenaltyStableFixture = Pdt2815PenaltyGapFixture;

/** Signed product + penalty using a tag that is not on the product detail (TC-BE-10). */
export async function buildPenaltyMissingTagOnSignedProduct(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<Pdt2815PenaltyStableFixture> {
  const base = await buildPenaltyStableSignedV2(Request, GeneratePayload, Responses, Endpoints);
  const prereq = await createPenaltyWithAmountFormula(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    penaltyTagFormulaVariable(PDT2815_TAG_MISSING),
  );
  await linkPenaltyToProductByIndex(
    Request,
    GeneratePayload,
    Endpoints,
    Responses,
    base.contractIndex,
    prereq.penaltyId,
  );
  return { ...base, ...prereq };
}

/** Two per-piece components share the same contract template tag (TC-BE-11). */
export async function buildPenaltyDuplicateTagOnSignedProduct(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<Pdt2815PenaltyStableFixture> {
  const prereq = await createPenaltyWithAmountFormula(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    penaltyTagFormulaVariable(PDT2815_TAG_DUPLICATE),
  );

  const pc1 = await postPerPieceWithContractTag(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    PDT2815_TAG_DUPLICATE,
  );
  const pc2 = await postPerPieceWithContractTag(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    PDT2815_TAG_DUPLICATE,
  );
  const productIndex = await postProductWithPriceComponentIds(Request, GeneratePayload, Responses, Endpoints, [
    pc1,
    pc2,
  ]);

  const { contractId, contractPayload, contractIndex } = await createProductContractV1Signed(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    productIndex,
  );
  await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 1, {
    startDate: ANCHOR_DATE,
    savingAsNewVersion: true,
    versionStatus: 'SIGNED',
  });
  await signProductContractVersion(Request, contractId, 2);
  const v2DetailId = await ensureOnlySignedV2ResolvesAtAnchor(Request, contractId);
  await activatePodOnContract(Request, Responses, contractId, 2, addDaysIso(ANCHOR_DATE, -15), 0);
  await ensureProductContractActiveInTerm(Request, Responses, contractId, 2);

  const body = await loadProductContract(Request, contractId);
  const { customerIdentifier, customerId } = await resolveCustomerFields(Request, Endpoints, Responses);

  return {
    ...massEmailFixtureFromContract(
      contractId,
      contractIndex,
      contractPayload,
      body,
      customerIdentifier,
      customerId,
      v2DetailId,
      signedVersionExecutionDate(body, 2),
    ),
    ...prereq,
    podRespectiveId: Responses.pod[0].id,
    podFutureOnlyId: Responses.pod[0].id,
    signedV2DetailId: v2DetailId,
    respectiveDetailId: v2DetailId,
    futureDetailId: null,
  };
}

export async function createPersistedProductPenaltyAction(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  params: {
    contractId: number;
    customerId: number;
    penaltyId: number;
    actionTypeId: number;
    executionDate: string;
    podIds?: number[];
    /** Only when action type is contract-termination-related (see ValidActionRequest). */
    terminationId?: number | null;
  },
): Promise<number> {
  const payload = await GeneratePayload.contractsAndOrders.action();
  payload.actionTypeId = params.actionTypeId;
  payload.executionDate = params.executionDate;
  payload.noticeReceivingDate = params.executionDate;
  payload.penaltyId = params.penaltyId;
  payload.penaltyPayer = 'CUSTOMER';
  payload.customerId = params.customerId;
  payload.contractId = params.contractId;
  payload.contractType = 'PRODUCT_CONTRACT';
  payload.pods = params.podIds ?? [];
  payload.withoutPenalty = false;
  // Penalty-only action types (e.g. id 4): termination fields must be absent — ValidActionRequest.java
  if (params.terminationId != null) {
    payload.terminationId = params.terminationId;
    payload.withoutAutomaticTermination = false;
  } else {
    payload.terminationId = null;
    payload.withoutAutomaticTermination = null as unknown as boolean;
  }
  const res = await Request.post(Endpoints.action, { data: payload });
  await expect(res).CheckResponse();
  const raw = await res.json();
  const actionId =
    typeof raw === 'number'
      ? raw
      : Number((raw as { id?: number }).id ?? (raw as { actionId?: number }).actionId);
  expect(Number.isFinite(actionId), 'POST /actions must return numeric action id').toBeTruthy();
  Responses.action.push({ id: actionId });
  return actionId;
}

export async function viewPenaltyAction(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  actionId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${Endpoints.action}/${actionId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function penaltyClaimAmountFromActionView(body: Record<string, unknown>): number | null {
  const raw =
    (body as { penaltyClaimAmount?: number | string }).penaltyClaimAmount ??
    (body as { calculatedPenaltyAmount?: number | string }).calculatedPenaltyAmount ??
    (body as { basicParameters?: { penaltyClaimAmount?: number | string } }).basicParameters
      ?.penaltyClaimAmount;
  if (raw == null || raw === '') return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}

/**
 * v1+v2 SIGNED, POD on v2, penalty amounts differ by version via estimated consumption (TC-BE-9 / TC-BE-16).
 * Requires `preconditionSharedEntities` in the same test (uses product index 0 — no second product POST).
 */
export async function buildPenaltyStableSignedV2(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<Pdt2815PenaltyGapFixture> {
  const prereq = await createPenaltyWithAmountFormula(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    PDT2815_PENALTY_FORMULA_CONSUMPTION,
  );
  await linkPenaltyToProductByIndex(Request, GeneratePayload, Endpoints, Responses, 0, prereq.penaltyId);

  const { contractId, contractPayload, contractIndex } = await createProductContractV1Signed(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    0,
  );

  await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 1, {
    startDate: ANCHOR_DATE,
    savingAsNewVersion: true,
    versionStatus: 'SIGNED',
    estimatedConsumptionKwh: PDT2815_CONSUMPTION_KWH_V2,
  });

  await signProductContractVersion(Request, contractId, 2);
  const v2DetailId = await ensureOnlySignedV2ResolvesAtAnchor(Request, contractId);
  await activatePodOnContract(
    Request,
    Responses,
    contractId,
    2,
    addDaysIso(ANCHOR_DATE, -15),
    0,
  );
  await ensureProductContractActiveInTerm(Request, Responses, contractId, 2);

  const body = await loadProductContract(Request, contractId);
  const { customerIdentifier, customerId } = await resolveCustomerFields(Request, Endpoints, Responses);

  return {
    ...massEmailFixtureFromContract(
      contractId,
      contractIndex,
      contractPayload,
      body,
      customerIdentifier,
      customerId,
      v2DetailId,
      signedVersionExecutionDate(body, 2),
    ),
    customerId,
    ...prereq,
    signedV2DetailId: v2DetailId,
    podRespectiveId: Responses.pod[0].id,
    podFutureOnlyId: Responses.pod[0].id,
    respectiveDetailId: v2DetailId,
    futureDetailId: null,
    consumptionKwhV2: PDT2815_CONSUMPTION_KWH_V2,
    consumptionKwhV3: PDT2815_CONSUMPTION_KWH_V3,
  };
}

/** Adds SIGNED v3 with later startDate (after anchor) — TC-BE-16 step 2 (version resolution only). */
export async function extendWithSignedVersion3AfterAnchor(
  fixture: Pdt2815MassEmailFixture,
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
): Promise<{ fixture: Pdt2815MassEmailFixture; signedV3DetailId: number | null }> {
  await putProductContractNewVersion(Request, Endpoints, fixture.contractId, fixture.contractPayload, 2, {
    startDate: addDaysIso(ANCHOR_DATE, 30),
    savingAsNewVersion: true,
    versionStatus: 'SIGNED',
  });
  const body = await loadProductContract(Request, fixture.contractId);
  return {
    fixture: { ...fixture, signedV2DetailId: versionDetailId(body, 2) },
    signedV3DetailId: versionDetailId(body, 3),
  };
}

export async function uploadMassSmsContractParse(
  Request: FixtureRequest,
  FileUploadRequest: FixtureFileUpload,
  Endpoints: baseFixture['Endpoints'],
  rows: { contractNumber: string; version?: string }[],
): Promise<MassSmsImportBody> {
  const templateRes = await Request.get(`${Endpoints.sms}/template`, {
    params: { importType: MASS_SMS_CONTRACT_IMPORT_TYPE },
  });
  await expect(templateRes).CheckResponse();
  const templateBuffer = await templateRes.body();

  const outputDir = path.resolve(__dirname, '../../mass-imports/output');
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filePath = path.join(outputDir, `pdt-2815-sms-contract-${stamp}.xlsx`);
  fs.writeFileSync(filePath, templateBuffer);

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(filePath);
  const worksheet = workbook.getWorksheet(1);
  if (!worksheet) {
    throw new Error('Mass SMS contract template has no worksheet');
  }

  rows.forEach((row, index) => {
    const excelRow = index + 2;
    worksheet.getCell(`A${excelRow}`).value = row.contractNumber;
    if (row.version != null && row.version !== '') {
      worksheet.getCell(`B${excelRow}`).value = row.version;
    }
  });

  await workbook.xlsx.writeFile(filePath);
  const fileBuffer = fs.readFileSync(filePath);

  const upload = await FileUploadRequest.post(`${Endpoints.sms}/parse`, {
    params: { importType: MASS_SMS_CONTRACT_IMPORT_TYPE },
    multipart: {
      file: {
        name: path.basename(filePath),
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: fileBuffer,
      },
    },
  });
  await expect(upload).CheckResponse();
  const body = (await upload.json()) as MassSmsImportBody;

  try {
    fs.unlinkSync(filePath);
  } catch {
    /* ignore */
  }

  return body;
}

export async function getCalculatePenaltyAmount(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  query: {
    actionTypeId: number;
    executionDate: string;
    penaltyId: number;
    penaltyPayer: 'CUSTOMER' | 'EPRES';
    customerId: number;
    contractId: number;
    contractType: 'PRODUCT_CONTRACT' | 'SERVICE_CONTRACT';
    pods?: number[];
    terminationId?: number;
  },
) {
  return Request.get(`${Endpoints.action}/calculate-penalty-amount`, {
    params: {
      actionTypeId: query.actionTypeId,
      executionDate: query.executionDate,
      penaltyId: query.penaltyId,
      penaltyPayer: query.penaltyPayer,
      customerId: query.customerId,
      contractId: query.contractId,
      contractType: query.contractType,
      terminationId: query.terminationId ?? undefined,
      pods: query.pods,
    },
  });
}

// ─── Service contract (TC-BE-5, TC-BE-6, TC-BE-15) ───────────────────────────

export type Pdt2815ServiceFixture = {
  contractId: number;
  contractNumber: string;
  customerIdentifier: string;
  customerId: number;
  signedV1DetailId: number | null;
};

export async function loadServiceContract(
  Request: FixtureRequest,
  contractId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`service-contract/${contractId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function serviceContractNumberFromBody(body: Record<string, unknown>): string {
  const bp = body.basicParameters as { contractNumber?: string } | undefined;
  return String(bp?.contractNumber ?? '');
}

export function serviceVersionDetailId(body: Record<string, unknown>, logicalVersionId: number): number | null {
  const versions = (body.versions ?? []) as { versionId?: number; id?: number }[];
  const row = versions.find((v) => Number(v.versionId) === logicalVersionId);
  return row?.id != null ? Number(row.id) : null;
}

export function serviceVersionStatus(body: Record<string, unknown>, logicalVersionId: number): string | undefined {
  const versions = (body.versions ?? []) as {
    versionId?: number;
    contractVersionStatus?: string;
    status?: string;
  }[];
  const row = versions.find((v) => Number(v.versionId) === logicalVersionId);
  return row?.contractVersionStatus ?? row?.status;
}

export async function putServiceContractNewVersion(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  contractId: number,
  opts: { startDate: string; contractVersionStatus: 'SIGNED' | 'DRAFT' },
) {
  const detail = await loadServiceContract(Request, contractId);
  const fromVersionId = pickLatestLogicalVersionId(
    (detail.versions ?? []) as { versionId: number }[],
  );
  const putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: fromVersionId },
    data: buildServiceContractEditPayloadFromDetail(detail, {
      savingAsNewVersion: true,
      startDate: opts.startDate,
      contractVersionStatus: opts.contractVersionStatus,
    }),
  });
  await expect(putRes).CheckResponse();
}

/** Full PDT-2846 chain — DRAFT service contract (v1 not finalized to header SIGNED). */
export async function buildServiceContractDraftOnly(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<Pdt2815ServiceFixture> {
  const chain = await withIsolatedServicePriceComponents(Responses, () =>
    runPdt2846DraftServiceContractChain({ Request, GeneratePayload, Responses, Endpoints }),
  );
  const body = await loadServiceContract(Request, chain.contractId);
  const custGet = await Request.get(`${Endpoints.customer}/${Responses.customer[Responses.customer.length - 1].id}`);
  await expect(custGet).CheckResponse();
  const customerJson = await custGet.json();

  return {
    contractId: chain.contractId,
    contractNumber: serviceContractNumberFromBody(body),
    customerIdentifier: String(customerJson.identifier),
    customerId: Number(customerJson.customerId ?? customerJson.id),
    signedV1DetailId: serviceVersionDetailId(body, 1),
  };
}

/** Signed service v1 (PDT-2599 chain) + optional v2 DRAFT. */
export async function buildServiceContractSignedV1WithOptionalDraftV2(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  withDraftV2: boolean,
): Promise<Pdt2815ServiceFixture & { signedV1DetailId: number | null; draftV2DetailId: number | null }> {
  const chain = await withIsolatedServicePriceComponents(Responses, () =>
    runPdt2599SignedServiceContractChain({ Request, GeneratePayload, Responses, Endpoints }),
  );
  if (withDraftV2) {
    await putServiceContractNewVersion(Request, Endpoints, chain.contractId, {
      startDate: addDaysIso(ANCHOR_DATE, 10),
      contractVersionStatus: 'DRAFT',
    });
  }
  const body = await loadServiceContract(Request, chain.contractId);
  const custIdx = Responses.customer.length - 1;
  const custGet = await Request.get(`${Endpoints.customer}/${Responses.customer[custIdx].id}`);
  await expect(custGet).CheckResponse();
  const customerJson = await custGet.json();

  return {
    contractId: chain.contractId,
    contractNumber: serviceContractNumberFromBody(body),
    customerIdentifier: String(customerJson.identifier),
    customerId: Number(customerJson.customerId ?? customerJson.id),
    signedV1DetailId: serviceVersionDetailId(body, 1),
    draftV2DetailId: withDraftV2 ? serviceVersionDetailId(body, 2) : null,
  };
}

// ─── Mass email all-customers (TC-BE-12, TC-BE-17) ───────────────────────────

export type AllCustomersProbeFixture = {
  customerAIdentifier: string;
  customerBIdentifier: string;
  customerAId: number;
  customerBId: number;
  productContractId: number;
  productContractNumber: string;
  serviceDraftContractId: number;
  serviceDraftContractNumber: string;
  serviceSignedContractId: number;
  serviceSignedContractNumber: string;
};

export async function createMassEmailDraftAllCustomers(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  Responses?: baseFixture['Responses'],
): Promise<number> {
  const payload = {
    allCustomersWithActiveContract: true,
    createType: 'DRAFT',
    communicationAsInstitution: false,
    emailBoxId: envVariables.email_mailboxes,
    topicOfCommunicationId: envVariables.topic_of_communication,
    contactPurposeIds: [envVariables.contact_purpose],
    subject: `PDT-2815 all-customers probe ${Date.now()}`,
    emailBody: 'PDT-2815 automated probe',
  };
  const res = await Request.post(`${Endpoints.email}/mass`, { data: payload });
  await expect(res).CheckResponse();
  const massEmailId = Number(await res.json());
  if (Responses) {
    Responses.email.push({ id: massEmailId, kind: 'massEmailDraft' });
  }
  return massEmailId;
}

export async function viewMassEmailCustomerIdentifiers(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
): Promise<string[]> {
  const res = await Request.get(`${Endpoints.email}/mass/${massEmailId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    customersShortResponse?: { name?: string; id?: number }[];
  };
  return (body.customersShortResponse ?? []).map((c) => String(c.name ?? c.id ?? ''));
}

/** Customer A: product signed + service draft; Customer B: service signed only. */
export async function buildAllCustomersProbeCustomers(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<AllCustomersProbeFixture> {
  const productFixture = await buildHappyPathSignedV1AndV2(Request, GeneratePayload, Responses, Endpoints);
  await ensureProductContractActiveInTerm(Request, Responses, productFixture.contractId, 2);
  const customerAIdentifier = productFixture.customerIdentifier;

  const serviceDraft = await buildServiceContractDraftOnly(Request, GeneratePayload, Responses, Endpoints);

  const customerB = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_private(),
  });
  await expect(customerB).CheckResponse();
  Responses.customer.push(await customerB.json());
  const customerBIndex = Responses.customer.length - 1;

  const podB = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(podB).CheckResponse();
  Responses.pod.push(await podB.json());
  const podBIndex = Responses.pod.length - 1;

  const serviceSigned = await withIsolatedServicePriceComponents(Responses, () =>
    buildSignedServiceContractForCustomerIndex(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      customerBIndex,
      podBIndex,
    ),
  );
  const customerBIdentifier = serviceSigned.customerIdentifier;

  await test.info().attach('[PDT-2815] all-customers probe ids', {
    body: JSON.stringify(
      {
        customerAIdentifier,
        customerBIdentifier,
        productContractId: productFixture.contractId,
        serviceDraftContractId: serviceDraft.contractId,
        serviceSignedContractId: serviceSigned.contractId,
        customerBIndex,
        podBIndex,
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });

  const serviceDraftBody = await loadServiceContract(Request, serviceDraft.contractId);
  const serviceSignedBody = await loadServiceContract(Request, serviceSigned.contractId);

  return {
    customerAIdentifier,
    customerBIdentifier,
    customerAId: productFixture.customerId,
    customerBId: Number(Responses.customer[customerBIndex].customerId ?? Responses.customer[customerBIndex].id),
    productContractId: productFixture.contractId,
    productContractNumber: productFixture.contractNumber,
    serviceDraftContractId: serviceDraft.contractId,
    serviceDraftContractNumber: serviceContractNumberFromBody(serviceDraftBody),
    serviceSignedContractId: serviceSigned.contractId,
    serviceSignedContractNumber: serviceContractNumberFromBody(serviceSignedBody),
  };
}

// ─── Portal / created-data links (manual verification) ───────────────────────

export type Pdt2815CreatedContext = {
  testCase?: string;
  customerId?: number;
  customerIdentifier?: string;
  productContractId?: number;
  productContractNumber?: string;
  serviceContractId?: number;
  serviceContractNumber?: string;
  massEmailId?: number;
  massSmsId?: number;
  actionId?: number;
  signedV2DetailId?: number | null;
  respectiveDetailId?: number | null;
  futureDetailId?: number | null;
  note?: string;
};

const PDT2815_PORTAL_KEY_ORDER = [
  'customer',
  'pod',
  'product',
  'productContract',
  'service',
  'serviceContract',
  'penalty',
  'termination',
  'email',
  'sms',
  'action',
] as const;

function getProcessEnv(key: string): string | undefined {
  const proc = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process;
  return proc?.env?.[key];
}

function resolvePortalFrontendBaseUrl(): string | null {
  const env = getProcessEnv('FRONTEND_BASE_URL')?.trim();
  if (env) return env.endsWith('/') ? env : `${env}/`;

  const raw = (
    configuredBaseURL || getProcessEnv('BASE_URL') || 'http://10.236.20.11:8091/'
  ).replace(/\/$/, '');

  const table: [string, string][] = [
    ['http://10.236.20.11:8091', 'http://10.236.20.11:8080/'],
    ['http://10.236.20.31:8091', 'http://10.236.20.31:8080/'],
    ['http://10.236.20.81:8091', 'http://10.236.20.81:8080/'],
    ['http://10.236.20.81:8094', 'http://10.236.20.31:8082/'],
    [
      'https://testapps.energo-pro.bg/backend/phoenix-epres',
      'https://testapps.energo-pro.bg/app/phoenix-epres/',
    ],
    [
      'https://devapps.energo-pro.bg/backend/phoenix-dev2',
      'https://devapps.energo-pro.bg/app/phoenix-dev2/',
    ],
  ];
  for (const [api, fe] of table) {
    if (raw === api) return fe;
  }
  const lab = raw.match(/^http:\/\/(10\.236\.20\.\d+):8091$/);
  if (lab) return `http://${lab[1]}:8080/`;
  return null;
}

function lastEntityId(bucket: unknown): number | undefined {
  if (!Array.isArray(bucket) || bucket.length === 0) return undefined;
  const item = bucket[bucket.length - 1];
  if (typeof item === 'number' && Number.isFinite(item)) return item;
  if (item && typeof item === 'object' && 'id' in item) {
    const n = Number((item as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  return undefined;
}

function buildExtraPortalLinks(
  fe: string | null,
  context?: Pdt2815CreatedContext,
): Record<string, string[]> {
  if (!fe) return {};
  const base = fe.endsWith('/') ? fe : `${fe}/`;
  const extra: Record<string, string[]> = {};

  const push = (key: string, url: string) => {
    if (!extra[key]) extra[key] = [];
    extra[key].push(url);
  };

  if (context?.massEmailId != null) {
    push('massEmail', `${base}mass-email/preview?id=${context.massEmailId}`);
  }

  if (context?.massSmsId != null) {
    push('massSms', `${base}mass-sms/preview?id=${context.massSmsId}`);
  }

  if (context?.actionId != null) {
    push('action', `${base}actions-for-terminations-and-penalties/preview?id=${context.actionId}`);
  }

  return extra;
}

function formatPdt2815LinksPlain(
  portalLinks: Record<string, string[]>,
  snapshot: Record<string, unknown>,
  hint: { apiBase: string; frontend: string | null; testCase: string },
): string {
  const lines: string[] = [
    `========== [PDT-2815] ${hint.testCase} — created data links ==========`,
    '',
    '--- IDs & labels (verify in UI) ---',
    JSON.stringify(snapshot, null, 2),
    '',
    '--- Portal preview URLs (open in browser) ---',
  ];

  if (Object.keys(portalLinks).length === 0) {
    lines.push(
      '(no portal URLs — set FRONTEND_BASE_URL or check BASE_URL → frontend mapping)',
      `API base: ${hint.apiBase}`,
      `Frontend resolved: ${hint.frontend ?? '(null)'}`,
    );
  } else {
    for (const key of PDT2815_PORTAL_KEY_ORDER) {
      const urls = portalLinks[key];
      if (!urls?.length) continue;
      lines.push('', `[${key}]`);
      for (const url of urls) lines.push(url);
    }
    for (const [key, urls] of Object.entries(portalLinks)) {
      if ((PDT2815_PORTAL_KEY_ORDER as readonly string[]).includes(key)) continue;
      if (!urls.length) continue;
      lines.push('', `[${key}]`);
      for (const url of urls) lines.push(url);
    }
  }

  lines.push(
    '',
    '--- Tips ---',
    'Playwright HTML report: npx playwright show-report → test → Attachments → plain text attachment.',
    'Console: same block printed below the test run.',
    '================================================================',
  );
  return lines.join('\n');
}

/** Attach portal preview URLs + entity snapshot for manual verification after each TC. */
export function attachPdt2815CreatedDataLinks(
  Responses: baseFixture['Responses'],
  context?: Pdt2815CreatedContext,
): void {
  const testCase = context?.testCase ?? test.info().title;
  const apiBase =
    configuredBaseURL || getProcessEnv('BASE_URL') || 'http://10.236.20.11:8091/';
  const fe = resolvePortalFrontendBaseUrl();

  const fromLinker = ResponseLinker.setLinksToResponses(Responses);
  const extra = buildExtraPortalLinks(fe, context);
  const portalLinks: Record<string, string[]> = { ...fromLinker };
  for (const [k, urls] of Object.entries(extra)) {
    portalLinks[k] = [...(portalLinks[k] ?? []), ...urls];
  }

  const snapshot: Record<string, unknown> = {
    testCase,
    anchorDate: ANCHOR_DATE,
    apiBase,
    frontendBase: fe,
    customerId: context?.customerId ?? lastEntityId(Responses.customer),
    customerIdentifier: context?.customerIdentifier,
    productContractId: context?.productContractId ?? lastEntityId(Responses.productContract),
    productContractNumber: context?.productContractNumber,
    serviceContractId: context?.serviceContractId ?? lastEntityId(Responses.serviceContract),
    serviceContractNumber: context?.serviceContractNumber,
    podIds: (Responses.pod ?? []).map((p) => p?.id).filter(Boolean),
    productId: lastEntityId(Responses.product),
    serviceId: lastEntityId(Responses.service),
    penaltyId: lastEntityId(Responses.penalty),
    terminationId: lastEntityId(Responses.termination),
    massEmailId: context?.massEmailId ?? lastEntityId(Responses.email),
    massSmsId: context?.massSmsId ?? lastEntityId(Responses.sms),
    actionId: context?.actionId ?? lastEntityId(Responses.action),
    signedV2DetailId: context?.signedV2DetailId,
    respectiveDetailId: context?.respectiveDetailId,
    futureDetailId: context?.futureDetailId,
    note: context?.note,
  };

  const plain = formatPdt2815LinksPlain(portalLinks, snapshot, {
    apiBase,
    frontend: fe,
    testCase,
  });

  console.log(`\n${plain}\n`);

  test.info().attach(`[PDT-2815] ${testCase} — portal URLs (plain text)`, {
    body: plain,
    contentType: 'text/plain; charset=utf-8',
  });
  test.info().attach(`[PDT-2815] ${testCase} — portal URLs (JSON)`, {
    body: JSON.stringify(portalLinks, null, 2),
    contentType: 'application/json',
  });
  test.info().attach(`[PDT-2815] ${testCase} — created entity snapshot (JSON)`, {
    body: JSON.stringify(snapshot, null, 2),
    contentType: 'application/json',
  });
}

/** Map product/mass-email fixture fields into attachPdt2815CreatedDataLinks context. */
export function pdt2815LinksFromProductFixture(
  testCase: string,
  fixture: Pdt2815MassEmailFixture,
  extra?: Partial<Pdt2815CreatedContext>,
): Pdt2815CreatedContext {
  return {
    testCase,
    customerId: fixture.customerId,
    customerIdentifier: fixture.customerIdentifier,
    productContractId: fixture.contractId,
    productContractNumber: fixture.contractNumber,
    signedV2DetailId: fixture.signedV2DetailId,
    ...extra,
  };
}

export function pdt2815LinksFromServiceFixture(
  testCase: string,
  fixture: Pdt2815ServiceFixture,
  extra?: Partial<Pdt2815CreatedContext>,
): Pdt2815CreatedContext {
  return {
    testCase,
    customerId: fixture.customerId,
    customerIdentifier: fixture.customerIdentifier,
    serviceContractId: fixture.contractId,
    serviceContractNumber: fixture.contractNumber,
    ...extra,
  };
}

export function pdt2815LinksFromPenaltyFixture(
  testCase: string,
  fixture: Pdt2815PenaltyGapFixture,
  extra?: Partial<Pdt2815CreatedContext>,
): Pdt2815CreatedContext {
  return {
    testCase,
    customerId: fixture.customerId,
    customerIdentifier: fixture.customerIdentifier,
    productContractId: fixture.contractId,
    productContractNumber: fixture.contractNumber,
    signedV2DetailId: fixture.signedV2DetailId,
    respectiveDetailId: fixture.respectiveDetailId,
    futureDetailId: fixture.futureDetailId,
    ...extra,
  };
}

// ─── Mass email preview / individual customer version (PDT-2815 Kalina scenario) ─

export type CustomerVersionFingerprint = {
  logicalVersion: number;
  customerDetailId: number;
  customerDetailVersionId: number;
  communicationDataId: number;
  emailAddress: string;
  mobileNumber: string;
};

export type MassEmailIndividualVersionScenario = Pdt2815MassEmailFixture & {
  customerVersions: {
    v1: CustomerVersionFingerprint;
    v2: CustomerVersionFingerprint;
    v3: CustomerVersionFingerprint;
  };
  signedV1DetailId: number;
  cancelledV2DetailId: number;
  createCustomerTemplate: Record<string, unknown>;
};

type EmailPreviewChannel = 'EMAIL' | 'MASS_EMAIL';

export type EmailCommunicationPreview = {
  customerShortResponse?: {
    customerId?: number;
    customerDetailId?: number;
    customerDetailVersionId?: number;
    name?: string;
  };
  customerCommunicationDataShortResponse?: {
    id?: number;
    name?: string;
  };
  customerEmailAddress?: string;
  communicationChannelType?: string;
  emailCommunicationStatus?: string;
  sentDate?: string;
  id?: number;
};

function stampEmail(tag: string): string {
  return `pdt2815-${tag}-${Date.now()}@automation.local`;
}

function stampMobile(tag: string): string {
  const suffix = `${Date.now()}${tag.length}`.replace(/\D/g, '').slice(-7);
  return `088${suffix.padStart(7, '0')}`;
}

function withDistinctContactsOnCreateTemplate(
  template: Record<string, unknown>,
  emailAddress: string,
  mobileNumber: string,
): Record<string, unknown> {
  const payload = JSON.parse(JSON.stringify(template)) as Record<string, unknown>;
  const commData = payload.communicationData as Record<string, unknown>[];
  for (const comm of commData ?? []) {
    const contacts = comm.communicationContacts as {
      contactType?: string;
      contactValue?: string;
      sendSms?: boolean;
    }[];
    for (const contact of contacts ?? []) {
      if (contact.contactType === 'EMAIL') {
        contact.contactValue = emailAddress;
      }
      if (contact.contactType === 'MOBILE_NUMBER') {
        contact.contactValue = mobileNumber;
        contact.sendSms = true;
      }
    }
  }
  return payload;
}

/** @deprecated Prefer withDistinctContactsOnCreateTemplate — email-only helper kept for imports. */
function withEmailOnCreateTemplate(
  template: Record<string, unknown>,
  emailAddress: string,
): Record<string, unknown> {
  return withDistinctContactsOnCreateTemplate(template, emailAddress, stampMobile('legacy'));
}

export async function loadCustomerVersionFingerprint(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  customerId: number,
  logicalVersion: number,
): Promise<CustomerVersionFingerprint> {
  const res = await Request.get(`${Endpoints.customer}/${customerId}?version=${logicalVersion}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as Record<string, unknown>;
  const detail =
    (body.customerDetail as Record<string, unknown> | undefined) ??
    (body.activeCustomerDetail as Record<string, unknown> | undefined) ??
    body;
  const comms =
    (detail.customerCommunications as Record<string, unknown>[] | undefined) ??
    (detail.communicationData as Record<string, unknown>[] | undefined) ??
    [];
  const commWithSms =
    comms.find((c) => {
      const contacts =
        (c.communicationContacts as { contactType?: string; sendSms?: boolean }[] | undefined) ??
        (c.contacts as { contactType?: string; sendSms?: boolean }[] | undefined) ??
        [];
      return contacts.some((x) => x.contactType === 'MOBILE_NUMBER' && x.sendSms === true);
    }) ?? comms[0] ?? {};
  const contacts =
    (commWithSms.communicationContacts as { contactType?: string; contactValue?: string }[] | undefined) ??
    (commWithSms.contacts as { contactType?: string; contactValue?: string }[] | undefined) ??
    [];
  const emailContact = contacts.find((c) => c.contactType === 'EMAIL');
  const mobileContact = contacts.find((c) => c.contactType === 'MOBILE_NUMBER');
  return {
    logicalVersion,
    customerDetailId: Number(detail.id ?? body.lastCustomerDetailId),
    customerDetailVersionId: Number(detail.versionId ?? detail.customerVersionId ?? logicalVersion),
    communicationDataId: Number(commWithSms.id),
    emailAddress: String(emailContact?.contactValue ?? ''),
    mobileNumber: String(mobileContact?.contactValue ?? ''),
  };
}

export async function createCustomerNewVersionWithEmail(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  customerId: number,
  fromLogicalVersion: number,
  createTemplate: Record<string, unknown>,
  emailAddress: string,
  mobileNumber: string,
): Promise<CustomerVersionFingerprint> {
  const editPayload = {
    customerDetailsVersion: fromLogicalVersion,
    updateExistingVersion: false,
    customerType: createTemplate.customerType,
    customerIdentifier: createTemplate.customerIdentifier,
    foreign: createTemplate.foreign,
    marketingConsent: createTemplate.marketingConsent,
    customerDetailStatus: 'ACTIVE',
    businessActivity: createTemplate.businessActivity,
    preferCommunicationInEnglish: createTemplate.preferCommunicationInEnglish ?? false,
    privateCustomerDetails: createTemplate.privateCustomerDetails,
    segmentIds: createTemplate.segmentIds,
    address: createTemplate.address,
    bankingDetails: createTemplate.bankingDetails,
    communicationData: (createTemplate.communicationData as Record<string, unknown>[]).map((comm) => ({
      contactTypeName: comm.contactTypeName,
      status: comm.status,
      address: comm.address,
      contactPurposes: (comm.contactPurposes as { contactPurposeId: number; status: string }[]).map((p) => ({
        contactPurposeId: p.contactPurposeId,
        status: p.status,
      })),
      communicationContacts: (comm.communicationContacts as Record<string, unknown>[]).map((c) => ({
        sendSms: c.contactType === 'MOBILE_NUMBER' ? true : c.sendSms,
        platformId: c.platformId,
        status: c.status,
        contactType: c.contactType,
        contactValue:
          c.contactType === 'EMAIL'
            ? emailAddress
            : c.contactType === 'MOBILE_NUMBER'
              ? mobileNumber
              : c.contactValue,
      })),
      contactPersons: [],
    })),
    accountManagers: [],
    managers: [],
    relatedCustomers: [],
    owner: [],
    customerEditContractRequests: [],
  };

  const put = await Request.put(`${Endpoints.customer}/${customerId}`, { data: editPayload });
  await expect(put).CheckResponse();
  return loadCustomerVersionFingerprint(Request, Endpoints, customerId, fromLogicalVersion + 1);
}

/**
 * Customer v1 + v2 + v3 (distinct emails), contract v1 SIGNED (customer v1), v2 SIGNED then CANCELLED (customer v2).
 * Mirrors Kalina QA repro on Dev (PDT-2815 comment 2026-05-26).
 */
export async function buildMassEmailIndividualCustomerVersionScenario(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<MassEmailIndividualVersionScenario> {
  await preconditionSharedEntities(Request, GeneratePayload, Responses, Endpoints);
  // Shared precondition creates a throwaway customer; this scenario uses a dedicated versioned customer.
  Responses.customer.pop();

  const emailV1 = stampEmail('cust-v1');
  const mobileV1 = stampMobile('cust-v1');
  const createTemplate = withDistinctContactsOnCreateTemplate(
    GeneratePayload.customers.customer_private() as Record<string, unknown>,
    emailV1,
    mobileV1,
  );
  const customerRes = await Request.post(Endpoints.customer, { data: createTemplate });
  await expect(customerRes).CheckResponse();
  Responses.customer.push(await customerRes.json());
  const customerRow = Responses.customer[Responses.customer.length - 1] as { id: number; identifier?: string };
  const customerId = Number(customerRow.id);

  const v1 = await loadCustomerVersionFingerprint(Request, Endpoints, customerId, 1);
  const v2 = await createCustomerNewVersionWithEmail(
    Request,
    Endpoints,
    customerId,
    1,
    createTemplate,
    stampEmail('cust-v2'),
    stampMobile('cust-v2'),
  );
  const v3 = await createCustomerNewVersionWithEmail(
    Request,
    Endpoints,
    customerId,
    2,
    createTemplate,
    stampEmail('cust-v3'),
    stampMobile('cust-v3'),
  );

  const { contractId, contractPayload, contractIndex } = await createProductContractV1Signed(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  );
  const signedV1DetailId = Number(versionDetailId(await loadProductContract(Request, contractId), 1));
  expect(signedV1DetailId, 'contract v1 detail id').toBeTruthy();

  await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 1, {
    startDate: addDaysIso(ANCHOR_DATE, 5),
    savingAsNewVersion: true,
    versionStatus: 'SIGNED',
    customerVersionId: v2.customerDetailVersionId,
  });
  await cancelProductContractVersion(Request, contractId, 2);

  const body = await loadProductContract(Request, contractId);
  const cancelledV2DetailId = Number(versionDetailId(body, 2));
  expect(cancelledV2DetailId, 'contract v2 detail id').toBeTruthy();

  await ensureProductContractActiveInTerm(Request, Responses, contractId, 1);
  await activatePodOnContract(Request, Responses, contractId, 1, addDaysIso(ANCHOR_DATE, -15), 0);

  const { customerIdentifier } = await resolveCustomerFields(Request, Endpoints, Responses);

  return {
    contractId,
    contractIndex,
    contractNumber: contractNumberFromBody(body),
    customerIdentifier,
    customerId,
    signedV2DetailId: cancelledV2DetailId,
    v2ExecutionDate: signedVersionExecutionDate(body, 1),
    contractPayload,
    customerVersions: { v1, v2, v3 },
    signedV1DetailId,
    cancelledV2DetailId,
    createCustomerTemplate: createTemplate,
  };
}

export function massEmailDraftPayload(subject: string, emailBody: string) {
  return {
    allCustomersWithActiveContract: false,
    createType: 'DRAFT',
    communicationAsInstitution: false,
    emailBoxId: envVariables.email_mailboxes,
    topicOfCommunicationId: envVariables.topic_of_communication,
    contactPurposeIds: [envVariables.contact_purpose],
    subject,
    emailBody,
  };
}

export async function createMassEmailDraft(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  Responses: baseFixture['Responses'],
  subject: string,
  emailBody: string,
  customers: {
    customerIdentifier: string;
    version?: number;
    productContractDetailId?: number;
  }[],
): Promise<number> {
  expect(
    customers.length,
    'mass email draft with allCustomersWithActiveContract=false requires at least one customer',
  ).toBeGreaterThan(0);

  const res = await Request.post(`${Endpoints.email}/mass`, {
    data: {
      ...massEmailDraftPayload(subject, emailBody),
      customers,
    },
  });
  await expect(res).CheckResponse();
  const massEmailId = Number(await res.json());
  Responses.email.push({ id: massEmailId, kind: 'massEmailDraft' });
  return massEmailId;
}

/** Guard: spec must not POST standalone single email — only mass header + contract import + mass SEND. */
export function assertNoManualSingleEmailCommunicationCreated(
  Responses: baseFixture['Responses'],
): void {
  const manual = (Responses.email ?? []).filter(
    (entry) => entry.kind !== 'massEmailDraft' && entry.kind !== 'massSmsDraft',
  );
  expect(
    manual,
    'test must not create single (non-mass) email communication via API',
  ).toHaveLength(0);
}

/** Per-customer communication spawned by mass SEND (system), not a separate manual email create. */
export function assertSystemSpawnedMassEmailChildCommunication(
  massEmailId: number,
  previewId: number,
  channel: EmailPreviewChannel,
  context: string,
): void {
  expect(previewId, `${context}: system must expose per-customer communication after mass SEND`).toBeTruthy();
  expect(
    previewId,
    `${context}: per-customer id must differ from mass email header id`,
  ).not.toBe(massEmailId);
  expect(
    ['EMAIL', 'MASS_EMAIL'].includes(channel),
    `${context}: channel must be mass-linked EMAIL or MASS_EMAIL customer preview`,
  ).toBeTruthy();
}

export async function updateMassEmailWithCustomers(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
  customers: {
    customerIdentifier: string;
    version?: number;
    productContractDetailId?: number;
  }[],
  createType: 'DRAFT' | 'SEND' = 'DRAFT',
): Promise<number> {
  const payload = {
    ...massEmailDraftPayload(`PDT-2815 mass ${massEmailId}`, 'PDT-2815 individual customer version probe'),
    customers,
    createType,
  };
  const res = await Request.put(`${Endpoints.email}/mass/${massEmailId}`, { data: payload });
  await expect(res).CheckResponse();
  return Number(await res.json());
}

export async function getEmailCommunicationPreview(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  id: number,
  channel: EmailPreviewChannel,
): Promise<EmailCommunicationPreview> {
  const res = await Request.get(`${Endpoints.email}/${id}`, { params: { type: channel } });
  await expect(res).CheckResponse();
  return (await res.json()) as EmailCommunicationPreview;
}

export async function findIndividualEmailListingRow(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
  customerIdentifier: string,
): Promise<{ individualEmailId?: number; emailCommunicationCustomerId?: number }> {
  const listRes = await Request.get(`${Endpoints.email}/list`, {
    params: {
      page: 0,
      size: 50,
      prompt: customerIdentifier,
      searchBy: 'CUSTOMER_IDENTIFIER',
      kindOfCommunication: ['EMAIL', 'MASS_EMAIL'],
      sortColumn: 'ID',
      sortDirection: 'DESC',
    },
  });
  await expect(listRes).CheckResponse();
  const body = (await listRes.json()) as {
    content?: {
      massOrIndemailCommunicationId?: number;
      linkedEmailCommunicationId?: number;
      communicationChannel?: string;
    }[];
  };
  const rows = body.content ?? [];
  const individual = rows.find(
    (r) =>
      r.linkedEmailCommunicationId === massEmailId ||
      (r.communicationChannel === 'EMAIL' && r.linkedEmailCommunicationId != null),
  );
  const massRow = rows.find(
    (r) =>
      r.communicationChannel === 'MASS_EMAIL' &&
      (r.linkedEmailCommunicationId === massEmailId || r.massOrIndemailCommunicationId != null),
  );
  const linkedMassCustomerRow = rows.find(
    (r) => r.communicationChannel === 'MASS_EMAIL' && r.linkedEmailCommunicationId === massEmailId,
  );
  return {
    individualEmailId:
      individual?.communicationChannel === 'EMAIL' && individual.linkedEmailCommunicationId === massEmailId
        ? individual.massOrIndemailCommunicationId
        : undefined,
    emailCommunicationCustomerId:
      linkedMassCustomerRow?.massOrIndemailCommunicationId ??
      massRow?.massOrIndemailCommunicationId ??
      individual?.massOrIndemailCommunicationId,
  };
}

export async function resolveMassEmailCustomerPreview(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
  customerIdentifier: string,
): Promise<{ preview: EmailCommunicationPreview; channel: EmailPreviewChannel; previewId: number }> {
  const listing = await findIndividualEmailListingRow(Request, Endpoints, massEmailId, customerIdentifier);
  if (listing.individualEmailId != null) {
    return {
      previewId: listing.individualEmailId,
      channel: 'EMAIL',
      preview: await getEmailCommunicationPreview(Request, Endpoints, listing.individualEmailId, 'EMAIL'),
    };
  }
  if (listing.emailCommunicationCustomerId != null) {
    return {
      previewId: listing.emailCommunicationCustomerId,
      channel: 'MASS_EMAIL',
      preview: await getEmailCommunicationPreview(
        Request,
        Endpoints,
        listing.emailCommunicationCustomerId,
        'MASS_EMAIL',
      ),
    };
  }
  throw new Error(
    `Could not resolve mass email customer preview for massEmailId=${massEmailId}, customer=${customerIdentifier}`,
  );
}

/** After mass `createType: SEND`, poll listing until per-customer individual EMAIL row exists. */
export async function waitForMassEmailIndividualEmailRow(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massEmailId: number,
  customerIdentifier: string,
  context: string,
  maxAttempts = 24,
): Promise<{ preview: EmailCommunicationPreview; channel: EmailPreviewChannel; previewId: number }> {
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const listing = await findIndividualEmailListingRow(Request, Endpoints, massEmailId, customerIdentifier);
    if (listing.individualEmailId != null) {
      const previewId = listing.individualEmailId;
      return {
        previewId,
        channel: 'EMAIL',
        preview: await getEmailCommunicationPreview(Request, Endpoints, previewId, 'EMAIL'),
      };
    }
    if (attempt < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, 5000));
    }
  }
  throw new Error(
    `${context}: no individual EMAIL row for massEmailId=${massEmailId}, customer=${customerIdentifier} after ${maxAttempts} polls`,
  );
}

/** Poll until mass/individual email leaves DRAFT after `createType: SEND` (Swagger: emailCommunicationStatus). */
export async function waitForEmailCommunicationSent(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  emailId: number,
  channel: EmailPreviewChannel,
  context: string,
  maxAttempts = 24,
): Promise<EmailCommunicationPreview> {
  let preview: EmailCommunicationPreview = {};
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    preview = await getEmailCommunicationPreview(Request, Endpoints, emailId, channel);
    const status = String(preview.emailCommunicationStatus ?? '').toUpperCase();
    if (status !== 'DRAFT') {
      return preview;
    }
    if (attempt < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, 5000));
    }
  }
  throw new Error(
    `${context}: email ${emailId} (${channel}) remained DRAFT after ${maxAttempts} polling attempts`,
  );
}

export function assertIndividualEmailSentStatus(
  preview: EmailCommunicationPreview,
  context: string,
): void {
  const status = String(preview.emailCommunicationStatus ?? '').toUpperCase();
  expect(status, `${context}: individual email must be sent, not DRAFT`).not.toBe('DRAFT');
  expect(
    ['IN_PROGRESS', 'SENT_SUCCESSFULLY', 'SENT'].includes(status),
    `${context}: expected outbound status after SEND`,
  ).toBeTruthy();
}

/** Guard: spec must not POST standalone single SMS — only mass header + contract parse + mass SEND. */
export function assertNoManualSingleSmsCommunicationCreated(
  Responses: baseFixture['Responses'],
): void {
  const manual = (Responses.sms ?? []).filter((entry) => entry.kind !== 'massSmsDraft');
  expect(
    manual,
    'test must not create single (non-mass) SMS communication via API',
  ).toHaveLength(0);
}

/** Per-customer SMS spawned by mass SEND (system), not a separate manual SMS create. */
export function assertSystemSpawnedMassSmsChildCommunication(
  massSmsId: number,
  previewId: number,
  context: string,
): void {
  expect(previewId, `${context}: system must expose individual SMS after mass SEND`).toBeTruthy();
  expect(
    previewId,
    `${context}: individual SMS id must differ from mass SMS header id`,
  ).not.toBe(massSmsId);
}

/** Poll until individual/mass SMS leaves DRAFT after `saveAs: SEND` (Swagger: communicationStatus). */
export async function waitForSmsCommunicationSent(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  smsPreviewId: number,
  context: string,
  maxAttempts = 24,
): Promise<SmsCommunicationPreview> {
  let preview: SmsCommunicationPreview = {};
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    preview = await getSmsCommunicationPreview(Request, Endpoints, smsPreviewId);
    const status = String(preview.communicationStatus ?? '').toUpperCase();
    if (status !== 'DRAFT') {
      return preview;
    }
    if (attempt < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, 5000));
    }
  }
  throw new Error(
    `${context}: SMS ${smsPreviewId} remained DRAFT after ${maxAttempts} polling attempts`,
  );
}

export function assertIndividualSmsSentStatus(
  preview: SmsCommunicationPreview,
  context: string,
): void {
  const status = String(preview.communicationStatus ?? '').toUpperCase();
  expect(status, `${context}: individual SMS must be sent, not DRAFT`).not.toBe('DRAFT');
  expect(
    ['IN_PROGRESS', 'SENT_SUCCESSFULLY', 'SENT', 'SENT_FAILED'].includes(status),
    `${context}: expected outbound status after SEND`,
  ).toBeTruthy();
}

export type SmsCommunicationPreview = {
  customerShortResponse?: {
    customerDetailId?: number;
    customerDetailVersionId?: number;
    customerVersion?: number;
  };
  customerCommunicationDataShortResponse?: {
    id?: number;
  };
  phoneNumber?: string;
  communicationStatus?: string;
};

export function massSmsDraftPayload(smsBody: string) {
  return {
    allCustomersWithActiveContract: false,
    communicationAsInstitution: false,
    topicOfCommunicationId: envVariables.topic_of_communication,
    exchangeCodeId: envVariables.sms_sending_numbers,
    contactPurposeIds: [envVariables.contact_purpose],
    smsBody,
    saveAs: 'DRAFT' as const,
  };
}

export async function createMassSmsDraft(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  Responses: baseFixture['Responses'],
  smsBody: string,
  customers: {
    customerIdentifier: string;
    version?: number;
    productContractDetailId?: number;
    serviceContractDetailId?: number;
  }[],
): Promise<number> {
  expect(customers.length, 'mass SMS draft requires at least one customer').toBeGreaterThan(0);
  const res = await Request.post(`${Endpoints.sms}/mass`, {
    data: {
      ...massSmsDraftPayload(smsBody),
      customers,
    },
  });
  await expect(res).CheckResponse();
  const massSmsId = Number(await res.json());
  Responses.sms = Responses.sms ?? [];
  Responses.sms.push({ id: massSmsId, kind: 'massSmsDraft' });
  return massSmsId;
}

export async function updateMassSmsWithCustomers(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massSmsId: number,
  customers: {
    customerIdentifier: string;
    version?: number;
    productContractDetailId?: number;
    serviceContractDetailId?: number;
  }[],
  saveAs: 'DRAFT' | 'SEND' = 'DRAFT',
): Promise<number> {
  const res = await Request.put(`${Endpoints.sms}/mass`, {
    params: { id: massSmsId },
    data: {
      ...massSmsDraftPayload(`PDT-2815 mass SMS ${massSmsId}`),
      customers,
      saveAs,
    },
  });
  await expect(res).CheckResponse();
  return Number(await res.json());
}

export async function getSmsCommunicationPreview(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  id: number,
): Promise<SmsCommunicationPreview> {
  const res = await Request.get(`${Endpoints.sms}/${id}`);
  await expect(res).CheckResponse();
  return (await res.json()) as SmsCommunicationPreview;
}

/** Individual SMS row in listing — `massOrIndSmsCommunicationId` is `SmsCommunicationCustomers.id`, not `Customer.id`. */
export async function findIndividualSmsListingRow(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massSmsId: number,
  customerIdentifier: string,
): Promise<{ individualSmsId?: number }> {
  const listRes = await Request.get(`${Endpoints.sms}/list`, {
    params: {
      page: 0,
      size: 50,
      prompt: customerIdentifier,
      smsCommunicationSearchBy: 'CUSTOMER_IDENTIFIER',
      kindOfCommunications: ['SMS', 'MASS_SMS'],
    },
  });
  await expect(listRes).CheckResponse();
  const body = (await listRes.json()) as {
    content?: {
      massOrIndSmsCommunicationId?: number;
      linkedMassSmsId?: number;
      communicationChannel?: string;
    }[];
  };
  const rows = body.content ?? [];
  const individual = rows.find(
    (r) => r.communicationChannel === 'Individual' && r.linkedMassSmsId === massSmsId,
  );
  return { individualSmsId: individual?.massOrIndSmsCommunicationId };
}

export async function resolveMassSmsCustomerPreview(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  massSmsId: number,
  customerIdentifier: string,
): Promise<{ preview: SmsCommunicationPreview; previewId: number }> {
  const listing = await findIndividualSmsListingRow(Request, Endpoints, massSmsId, customerIdentifier);
  const previewId = listing.individualSmsId;
  expect(
    previewId,
    `Could not resolve individual SMS preview for massSmsId=${massSmsId}, customer=${customerIdentifier}`,
  ).toBeTruthy();
  return {
    previewId: Number(previewId),
    preview: await getSmsCommunicationPreview(Request, Endpoints, Number(previewId)),
  };
}

/** Documents current AS-IS PDT-2815 defect: latest customer v3 used instead of contract-context v1. */
export function assertPdt2815UsesLatestCustomerVersionBug(
  preview: {
    customerShortResponse?: {
      customerDetailId?: number;
      customerDetailVersionId?: number;
      customerVersion?: number;
    };
    customerCommunicationDataShortResponse?: { id?: number };
    customerEmailAddress?: string;
    phoneNumber?: string;
  },
  scenario: MassEmailIndividualVersionScenario,
  context: string,
): void {
  const expected = scenario.customerVersions.v1;
  const latest = scenario.customerVersions.v3;

  const versionId =
    preview.customerShortResponse?.customerDetailVersionId ??
    preview.customerShortResponse?.customerVersion;
  const detailId = preview.customerShortResponse?.customerDetailId;

  expect(
    versionId ?? detailId,
    `${context}: preview must expose customer version or customerDetailId`,
  ).toBeTruthy();

  if (versionId != null) {
    expect(
      versionId,
      `${context}: AS-IS defect — resolves to latest customer v3, not contract-context v1`,
    ).toBe(latest.customerDetailVersionId);
    expect(
      versionId,
      `${context}: must not use contract-scoped customer v1 while defect is open`,
    ).not.toBe(expected.customerDetailVersionId);
  }

  if (detailId != null) {
    expect(
      detailId,
      `${context}: AS-IS defect — physical customer detail must be latest v3`,
    ).toBe(latest.customerDetailId);
    expect(detailId, `${context}: must not use customer v1 physical detail id`).not.toBe(
      expected.customerDetailId,
    );
  }

  const commId = preview.customerCommunicationDataShortResponse?.id;
  if (commId != null && latest.communicationDataId > 0) {
    expect(
      commId,
      `${context}: communication data must match latest customer v3`,
    ).toBe(latest.communicationDataId);
    expect(commId, `${context}: must not use customer v1 communication id`).not.toBe(
      expected.communicationDataId,
    );
  }

  if (preview.phoneNumber && latest.mobileNumber) {
    expect(
      preview.phoneNumber.replace(/\s/g, ''),
      `${context}: phone must match latest customer v3 mobile`,
    ).toContain(latest.mobileNumber.replace(/\s/g, '').slice(-7));
  }
}
