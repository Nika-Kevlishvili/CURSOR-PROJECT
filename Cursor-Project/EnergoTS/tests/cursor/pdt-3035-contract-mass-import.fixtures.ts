/**
 * PDT-3035 — Contract mass-import defects (NPE, managerIds, EIF).
 *
 * Workflow: create fresh Dev contract per test → minimal Excel (A,B,C,G,H) → mass import → error report assert.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3013-separate-pod-reversal-offset.fixtures.ts (catalog + product contract chain)
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts (preconditionProductContractForManual)
 * - tests/salesPortal/GET-PRODUCT-LIST-product-list.spec.ts (READY contract POST)
 */

import * as ExcelJS from 'exceljs';
import { expect, type APIRequestContext } from '@playwright/test';
import type { baseFixture } from '../../fixtures/baseFixture';
import { configuredBaseURL } from '../../fixtures/utils/baseUrl';
import fs from 'fs';
import os from 'os';
import path from 'path';

export const PDT_3035_PROD_PROCESS_ID = 2114;
export const PDT_3035_PROD_FILE_PATH = path.resolve(
  __dirname,
  '../../../config/playwright/pdt-3035/prod-process-2114-product-contract-mass-import.xlsx',
);

/** Prod process 2114 reference only (error counts) — TC-BE-1 uses legacy env contracts from prod xlsx. */
export const PDT_3035_DEV_MIRROR_CONTRACT_NUMBER = 'EPES2606000350';

/** Prod xlsx row on Test with null until-term (legacy NPE path). */
export const PDT_3035_LEGACY_NPE_CONTRACT_TEST = 'EPES2606000595';

export type LegacyPdt3035NpeContract = FreshPdt3035Contract & {
  /** prod-profile = null until + EIF in DB; prod-null-until-eif-via-excel = legacy row, EIF only in excel (API cannot seed EIF without corrupting until). */
  legacySource: 'prod-profile' | 'prod-null-until-eif-via-excel';
};

export type Pdt3035Environment = 'dev' | 'dev2' | 'test' | 'unknown';

export function resolvePdt3035Environment(): Pdt3035Environment {
  const raw = (configuredBaseURL || process.env.BASE_URL || '').toLowerCase();
  if (raw.includes('testapps') || raw.includes('phoenix-epres')) return 'test';
  if (raw.includes('phoenix-dev2') || raw.includes('dev2')) return 'dev2';
  if (raw.includes('10.236.20.11') || raw.includes(':8091') || raw.includes('devapps')) return 'dev';
  return 'unknown';
}

/** Test env contracts with DB null until-term (prefer SIGNED for O+MANUAL EIF workaround). */
export const PDT_3035_TEST_NULL_UNTIL_NPE_CANDIDATES = [
  'EPES2603000201',
  'EPES2603000149',
  'EPES2603000183',
  'EPES2607000557',
  'EPES2607000466',
] as const;

/** Test = bug still present (assert errors). Dev = post-fix regression (assert import succeeds). */
export function pdt3035BugExpectedOnEnv(env: Pdt3035Environment = resolvePdt3035Environment()): boolean {
  return env === 'test';
}

export type Pdt3035BugKind = 'npe' | 'managerIds' | 'eif';

/** Per-bug deploy state on Test (managerIds + NPE fixes deployed; EIF still open on Test). */
export function pdt3035BugExpectedForKind(
  kind: Pdt3035BugKind,
  env: Pdt3035Environment = resolvePdt3035Environment(),
): boolean {
  if (env !== 'test') return false;
  if (kind === 'managerIds' || kind === 'npe') return false;
  return true;
}

export type Pdt3035OutcomeEvaluation = {
  passed: boolean;
  expectedResult: string;
  actualResult: string;
};

export function evaluatePdt3035MassImportOutcome(
  snapshot: MassImportErrorReportSnapshot,
  kind: Pdt3035BugKind,
  env: Pdt3035Environment = resolvePdt3035Environment(),
): Pdt3035OutcomeEvaluation {
  const bugExpected = pdt3035BugExpectedForKind(kind, env);
  if (kind === 'npe') {
    const hasBug = snapshot.npeCount >= 1;
    return {
      passed: bugExpected ? hasBug : snapshot.npeCount === 0 && snapshot.failedRowCount === 0,
      expectedResult: bugExpected
        ? 'Mass import fails with until-amount/volume NPE (Prod process 2114).'
        : 'Mass import succeeds with no NPE after fix.',
      actualResult: `env=${env} npeCount=${snapshot.npeCount} failed=${snapshot.failedRowCount}`,
    };
  }
  if (kind === 'managerIds') {
    const hasBug = snapshot.managerIdsCount >= 1;
    return {
      passed: bugExpected ? hasBug : snapshot.managerIdsCount === 0 && snapshot.failedRowCount === 0,
      expectedResult: bugExpected
        ? 'Mass import fails with managerIds validation (Prod process 2114).'
        : 'Mass import succeeds; managerIds hydrated from DB after fix.',
      actualResult: `env=${env} managerIdsCount=${snapshot.managerIdsCount} failed=${snapshot.failedRowCount}`,
    };
  }
  const hasBug = snapshot.eifCount >= 1;
  return {
    passed: bugExpected ? hasBug : snapshot.eifCount === 0 && snapshot.failedRowCount === 0,
    expectedResult: bugExpected
      ? 'Mass import fails with entryInForceDate future validation (Prod process 2114).'
      : 'Mass import succeeds with past EIF after fix.',
    actualResult: `env=${env} eifCount=${snapshot.eifCount} failed=${snapshot.failedRowCount}`,
  };
}

export const PDT_3035_PROXY_EGN = '7501011232';
export const PDT_3035_DEFAULT_VERSION_ID = 1;

export const PDT_3035_UPLOAD = 'mass-import/PRODUCT_CONTRACTS/files/upload';
export const PDT_3035_TEMPLATE_DOWNLOAD = 'mass-import/PRODUCT_CONTRACTS/template/download';

export const PDT_3035_NPE_PATTERNS =
  /booleanValue|getContractTermUntilTheAmount|getContractTermUntilTheVolume|contractTermUntilTheAmount|contractTermUntilTheVolume/i;

export const PDT_3035_MANAGER_IDS_PATTERN =
  /managerIds-\[managerIds\]|managerIds should be present/i;

export const PDT_3035_EIF_PATTERN =
  /entryInForceDate|Entry in force should be in future/i;

export const PDT_3035_MANAGER_IDS_ERROR =
  'managerIds-[managerIds] should be present when customer is LEGAL_ENTITY or PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY;';

export const PDT_3035_EIF_ERROR =
  'basicParameters.entryInForceDate-Entry in force should be in future!;';

/** Prod process 2114 baseline (2026-06-26) — reference only. */
export const PDT_3035_PROD_BASELINE = {
  totalRows: 90,
  failedRows: 72,
  npeCount: 45,
  managerIdsCount: 4,
  eifCount: 6,
};

export type Pdt3035Fx = {
  Request: APIRequestContext;
  FileUploadRequest: APIRequestContext;
};

export type Pdt3035CreateFx = Pdt3035Fx &
  Pick<baseFixture, 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export type FreshPdt3035Contract = {
  contractId: number;
  contractNumber: string;
  productId: number;
  productVersionId: number;
  status: string | null;
  entryInForceDate: string | null;
};

function addDaysIso(days: number): string {
  const d = new Date();
  d.setUTCHours(12, 0, 0, 0);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

function omitUntilTermFields(bp: Record<string, unknown>): void {
  delete bp.hasUntilAmount;
  delete bp.hasUntilVolume;
  delete bp.untilAmount;
  delete bp.untilVolume;
  delete bp.untilAmountCurrencyId;
}

async function activatePodOnFreshContract(fx: Pdt3035CreateFx): Promise<void> {
  const podActivation = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(),
  });
  await expect(podActivation).CheckResponse();
}

async function resolveFreshContractMeta(
  fx: Pdt3035CreateFx,
  contractId: number,
): Promise<FreshPdt3035Contract> {
  const getRes = await fx.Request.get(
    `product-contract/${contractId}?versionId=${PDT_3035_DEFAULT_VERSION_ID}`,
  );
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;
  const bp = body.basicParameters as Record<string, unknown>;
  const contractNumber = String(bp.contractNumber ?? '').trim();
  if (!contractNumber) {
    throw new Error(`Fresh contract ${contractId} GET missing basicParameters.contractNumber`);
  }
  return {
    contractId,
    contractNumber,
    productId: Number(bp.productId),
    productVersionId: Number(bp.productVersionId),
    status: (bp.status as string | null) ?? null,
    entryInForceDate: (bp.entryInForceDate as string | null | undefined) ?? null,
  };
}

/** Term → price → product → LEGAL_ENTITY customer → POD (entity order per precondition-data-creation). */
export async function createPdt3035CatalogChain(fx: Pdt3035CreateFx): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, {
    data: GeneratePayload.productAndServices.term(),
  });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const pricePayload = GeneratePayload.productAndServices.priceSettlement();
  const price = await Request.post(Endpoints.priceComponent, { data: pricePayload });
  await expect(price).CheckResponse();
  Responses.priceComponent.push(await price.json());

  const productPayload = GeneratePayload.productAndServices.product();
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const customer = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(customer).CheckResponse();
  Responses.customer.push(await customer.json());

  const pod = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod).CheckResponse();
  Responses.pod.push(await pod.json());
}

async function postFreshReadyContractOmitUntil(fx: Pdt3035CreateFx): Promise<FreshPdt3035Contract> {
  const payload = await fx.GeneratePayload.contractsAndOrders.product_contract();
  const bp = payload.basicParameters as Record<string, unknown>;
  bp.status = 'READY';
  bp.subStatus = 'READY';
  bp.signingDate = null;
  bp.entryInForceDate = addDaysIso(7);
  bp.startOfInitialTerm = null;
  omitUntilTermFields(bp);

  const contractRes = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  await expect(contractRes).CheckResponse();
  const contractJson = (await contractRes.json()) as { id?: number };
  fx.Responses.productContract.push(contractJson);
  const contractId = Number(contractJson.id);
  if (!Number.isFinite(contractId) || contractId <= 0) {
    throw new Error('POST product-contract did not return id');
  }
  return resolveFreshContractMeta(fx, contractId);
}

/**
 * Test/Dev mirror EPES2606000350 may have MANUAL EIF with null date — mass import then fails
 * "Entry in force is mandatory" before the until-amount NPE path. Prod row uses DB EIF only (column O empty).
 */
async function ensureContractEntryInForceDateSet(
  fx: Pick<Pdt3035Fx, 'Request'>,
  contractNumber: string,
): Promise<void> {
  const contractId = await findProductContractIdByNumber(fx.Request, contractNumber);
  const versionId = PDT_3035_DEFAULT_VERSION_ID;

  const getRes = await fx.Request.get(`product-contract/${contractId}?versionId=${versionId}`);
  await expect(getRes).CheckResponse();
  const contract = (await getRes.json()) as Record<string, unknown>;
  const bp = contract.basicParameters as ContractBasicParametersGet;
  const existingEif = (bp.entryInForceDate as string | null | undefined) ?? null;
  if (existingEif && String(existingEif).trim()) {
    console.log(`[PDT-3035] ${contractNumber} entryInForceDate already set: ${existingEif}`);
    return;
  }

  const futureEif = addDaysIso(30);
  const pastEif = addDaysIso(-7);
  const status = String(bp.status ?? '');
  const activeStatuses = new Set(['ACTIVE_IN_TERM', 'ENTERED_INTO_FORCE', 'ACTIVE']);
  const eifToSet = activeStatuses.has(status) ? pastEif : futureEif;

  const putPayload = buildContractUpdatePayloadFromGet(contract, []);
  const putBp = putPayload.basicParameters as Record<string, unknown>;
  const putPp = putPayload.productParameters as Record<string, unknown> | undefined;

  putBp.entryInForceDate = eifToSet;
  if (!putBp.startOfInitialTerm) {
    putBp.startOfInitialTerm = eifToSet;
  }
  if (status === 'READY') {
    putBp.signingDate = null;
  } else {
    const signing = putBp.signingDate as string | null | undefined;
    if (!signing || String(signing).trim() === '') {
      putBp.signingDate = addDaysIso(-14);
    }
  }
  if (putPp) {
    putPp.entryIntoForceValue = eifToSet;
    if (!putPp.startOfContractValue) {
      putPp.startOfContractValue = eifToSet;
    }
  }

  // Do not send until-term flags on EIF-only precondition — PUT would coerce null → false and block NPE path.
  delete putBp.hasUntilAmount;
  delete putBp.hasUntilVolume;
  delete putBp.untilAmount;
  delete putBp.untilVolume;
  delete putBp.untilAmountCurrencyId;

  const putRes = await fx.Request.put(
    `product-contract/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: putPayload },
  );
  await expect(putRes).CheckResponse();
  console.log(
    `[PDT-3035] precondition: set entryInForceDate=${eifToSet} on ${contractNumber} (status=${status}; was null)`,
  );
}

/**
 * TC-BE-1 only: resolve legacy prod-process-2114 row on env (null until-term in DB).
 * Fresh POST cannot store null until-term — use existing contract from prod xlsx.
 */
export async function resolveLegacyPdt3035NpeContract(
  fx: Pdt3035CreateFx,
): Promise<LegacyPdt3035NpeContract> {
  if (!pdt3035BugExpectedForKind('npe')) {
    await createPdt3035CatalogChain(fx);
    const base = await postFreshReadyContractOmitUntil(fx);
    const meta = await resolveFreshContractMeta(fx, base.contractId);
    console.log(
      `[PDT-3035] NPE regression contract ${meta.contractNumber} (id=${meta.contractId}) — fresh READY (post-fix)`,
    );
    return { ...meta, legacySource: 'prod-profile' };
  }

  const fromXlsx = await readProdXlsxContractNumbers();
  const candidates = [
    ...new Set([
      PDT_3035_LEGACY_NPE_CONTRACT_TEST,
      PDT_3035_DEV_MIRROR_CONTRACT_NUMBER,
      'EPES2606002385',
      ...PDT_3035_TEST_NULL_UNTIL_NPE_CANDIDATES,
      ...fromXlsx,
    ]),
  ];

  for (const contractNumber of candidates) {
    try {
      const contractId = await findProductContractIdByNumber(fx.Request, contractNumber);
      const getRes = await fx.Request.get(
        `product-contract/${contractId}?versionId=${PDT_3035_DEFAULT_VERSION_ID}`,
      );
      if (!getRes.ok()) {
        continue;
      }
      const contract = (await getRes.json()) as Record<string, unknown>;
      const bp = contract.basicParameters as ContractBasicParametersGet;
      const pp = (contract.productParameters ?? {}) as Record<string, unknown>;

      if (contractHasCorruptedUntilTermOnGet(bp)) {
        console.log(
          `[PDT-3035] skip legacy NPE candidate ${contractNumber}: until-term coerced to false (not null)`,
        );
        continue;
      }

      if (contractHasProd2114NpeDbProfile(bp, pp)) {
        const meta = await resolveFreshContractMeta(fx, contractId);
        console.log(
          `[PDT-3035] legacy NPE ${contractNumber} (id=${contractId}) — full prod profile (null until + EIF in DB)`,
        );
        return { ...meta, legacySource: 'prod-profile' };
      }

      if (contractHasNullUntilTermOnGet(bp)) {
        const meta = await resolveFreshContractMeta(fx, contractId);
        if (contractHasEntryInForceInDb(bp, pp)) {
          console.log(
            `[PDT-3035] legacy NPE ${contractNumber} (id=${contractId}) — null until + EIF in DB`,
          );
          return { ...meta, legacySource: 'prod-profile' };
        }
        const status = String(bp.status ?? '');
        if (status !== 'SIGNED') {
          console.log(
            `[PDT-3035] skip legacy NPE candidate ${contractNumber}: null until without DB EIF needs SIGNED (got ${status})`,
          );
          continue;
        }
        console.log(
          `[PDT-3035] legacy NPE ${contractNumber} (id=${contractId}) — null until-term; EIF via excel O+MANUAL (DB EIF empty)`,
        );
        return { ...meta, legacySource: 'prod-null-until-eif-via-excel' };
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      console.log(`[PDT-3035] skip legacy NPE candidate ${contractNumber}: ${msg}`);
    }
  }

  throw new Error(
    `No legacy prod-row contract with null until-term on ${resolvePdt3035Environment()}. ` +
      `Need env contract from prod-process-2114 xlsx (e.g. ${PDT_3035_LEGACY_NPE_CONTRACT_TEST}).`,
  );
}

export async function buildLegacyPdt3035NpeMassImportExcel(
  fx: Pick<Pdt3035Fx, 'Request'>,
  legacy: LegacyPdt3035NpeContract,
): Promise<{ excelPath: string; row: MinimalEditRow; statusColumn: string | null }> {
  if (legacy.legacySource === 'prod-profile') {
    return buildProd2114IdenticalMinimalEdit(fx, legacy.contractNumber);
  }
  return buildSingleRowMinimalEdit(fx, legacy.contractNumber, {
    clearTemplateRow: true,
    supplyEntryInForceViaExcel: true,
  });
}

/** @deprecated TC-BE-1 uses resolveLegacyPdt3035NpeContract — fresh POST cannot store null until-term. */
export async function createFreshPdt3035NpeContract(fx: Pdt3035CreateFx): Promise<FreshPdt3035Contract> {
  return resolveLegacyPdt3035NpeContract(fx);
}

/** Fresh LEGAL_ENTITY contract (no proxy on contract) — minimal Excel triggers managerIds path on buggy env. */
export async function createFreshPdt3035ManagerIdsContract(
  fx: Pdt3035CreateFx,
): Promise<FreshPdt3035Contract> {
  await createPdt3035CatalogChain(fx);
  const base = await postFreshReadyContractOmitUntil(fx);
  const meta = await resolveFreshContractMeta(fx, base.contractId);
  console.log(
    `[PDT-3035] fresh managerIds contract ${meta.contractNumber} (id=${meta.contractId}) status=${meta.status}`,
  );
  return meta;
}

function contractHasNullUntilTermOnGet(bp: ContractBasicParametersGet): boolean {
  return bp.hasUntilAmount == null && bp.hasUntilVolume == null;
}

function contractHasCorruptedUntilTermOnGet(bp: ContractBasicParametersGet): boolean {
  return bp.hasUntilAmount === false || bp.hasUntilVolume === false;
}

/** Prod-like NPE rows: EIF in DB; Excel has only A,B,C,G,H (columns I/O empty). */
function contractHasEntryInForceInDb(
  bp: ContractBasicParametersGet,
  productParameters?: Record<string, unknown>,
): boolean {
  const eifDate = bp.entryInForceDate;
  if (eifDate != null && String(eifDate).trim() !== '') {
    return true;
  }
  const eifValue = productParameters?.entryIntoForceValue;
  return eifValue != null && String(eifValue).trim() !== '';
}

function contractHasProd2114NpeDbProfile(
  bp: ContractBasicParametersGet,
  productParameters?: Record<string, unknown>,
): boolean {
  return contractHasNullUntilTermOnGet(bp) && contractHasEntryInForceInDb(bp, productParameters);
}

function excelColumnLetter(zeroBasedIndex: number): string {
  let n = zeroBasedIndex + 1;
  let col = '';
  while (n > 0) {
    const rem = (n - 1) % 26;
    col = String.fromCharCode(65 + rem) + col;
    n = Math.floor((n - 1) / 26);
  }
  return col;
}

const PDT_3035_MASS_IMPORT_DATA_ROW = 2;
/** Mass-import template ships sample proxy/EIF/status values in row 2 — clear through col 61+ (entry into force). */
const PDT_3035_MASS_IMPORT_CLEAR_THROUGH_COLUMN = 80;

function clearMassImportTemplateDataRow(
  worksheet: ExcelJS.Worksheet,
  rowNumber = PDT_3035_MASS_IMPORT_DATA_ROW,
): void {
  const row = worksheet.getRow(rowNumber);
  for (let col = 1; col <= PDT_3035_MASS_IMPORT_CLEAR_THROUGH_COLUMN; col++) {
    row.getCell(col).value = null;
  }
  row.commit?.();
}

/** Fresh contract: POST future EIF → POD activation → PUT backdate past EIF (Test allows past only after activation). */
export async function createFreshPdt3035EifContract(fx: Pdt3035CreateFx): Promise<FreshPdt3035Contract> {
  await createPdt3035CatalogChain(fx);

  const futureEif = addDaysIso(30);
  const signingDate = addDaysIso(-14);

  const payload = await fx.GeneratePayload.contractsAndOrders.product_contract();
  const bp = payload.basicParameters as Record<string, unknown>;
  const pp = payload.productParameters as Record<string, unknown>;
  bp.status = 'SIGNED';
  bp.subStatus = 'SIGNED_BY_BOTH_SIDES';
  bp.versionStatus = 'SIGNED';
  bp.signingDate = signingDate;
  bp.entryInForceDate = futureEif;
  bp.startOfInitialTerm = futureEif;
  omitUntilTermFields(bp);
  pp.entryIntoForceValue = futureEif;
  pp.startOfContractValue = futureEif;

  const contractRes = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  await expect(contractRes).CheckResponse();
  const contractJson = (await contractRes.json()) as { id?: number };
  fx.Responses.productContract.push(contractJson);
  const contractId = Number(contractJson.id);
  if (!Number.isFinite(contractId) || contractId <= 0) {
    throw new Error('POST product-contract did not return id for EIF scenario');
  }

  await activatePodOnFreshContract(fx);

  const pastEif = addDaysIso(-7);
  const getRes = await fx.Request.get(
    `product-contract/${contractId}?versionId=${PDT_3035_DEFAULT_VERSION_ID}`,
  );
  await expect(getRes).CheckResponse();
  const contract = (await getRes.json()) as Record<string, unknown>;
  const putPayload = buildContractUpdatePayloadFromGet(contract, []);
  const putBp = putPayload.basicParameters as Record<string, unknown>;
  const putPp = putPayload.productParameters as Record<string, unknown>;
  putBp.entryInForceDate = pastEif;
  putBp.startOfInitialTerm = pastEif;
  putBp.signingDate = signingDate;
  putPp.entryIntoForceValue = pastEif;
  putPp.startOfContractValue = pastEif;

  const putRes = await fx.Request.put(
    `product-contract/${contractId}?versionId=${PDT_3035_DEFAULT_VERSION_ID}&changeFutureVersionsPods=false`,
    { data: putPayload },
  );
  await expect(putRes).CheckResponse();

  const meta = await resolveFreshContractMeta(fx, contractId);
  console.log(
    `[PDT-3035] fresh EIF contract ${meta.contractNumber} (id=${contractId}) ` +
      `status=${meta.status} eif=${meta.entryInForceDate}`,
  );
  return meta;
}

export type MassImportErrorRow = {
  excelRow: number;
  errorText: string;
};

export type MassImportErrorReportSnapshot = {
  totalRows: number;
  failedRowCount: number;
  npeCount: number;
  managerIdsCount: number;
  eifCount: number;
  failedRows: MassImportErrorRow[];
  reportPath: string | null;
};

export type MassImportRunResult = {
  processId: number;
  snapshot: MassImportErrorReportSnapshot;
};

export type ProdMirrorRow = {
  contractNumber: string;
  version: number | string;
  createOrEdit: string;
  productId: number | string;
  productVersion: number | string;
  prodExcelRow?: number;
};

export type MinimalEditRow = {
  contractNumber: string;
  version: number | string;
  createOrEdit: string;
  productId: number | string;
  productVersion: number | string;
  contractId: number;
  status: string | null;
  entryInForceDate: string | null;
  statusColumn?: string | null;
};

type MassImportNotificationItem = {
  notificationType?: string;
  entityId?: number;
  createDate?: string;
};

type ProcessSnapshot = {
  processType?: string;
  name?: string;
  status?: string;
};

type ContractBasicParametersGet = Record<string, unknown>;
type ContractProxyGet = Record<string, unknown>;

const MASS_IMPORT_NOTIFICATION_TYPES =
  /PRODUCT_CONTRACT_MASS_IMPORT_(COMPLETED|ERROR)$/i;

function cellText(value: ExcelJS.CellValue): string {
  if (value == null || value === '') return '';
  if (typeof value === 'object' && value !== null && 'text' in value) {
    return String((value as { text?: string }).text ?? '');
  }
  if (value instanceof Date) {
    return value.toISOString().slice(0, 10);
  }
  return String(value);
}

function cellPrimitive(value: ExcelJS.CellValue): string | number {
  const text = cellText(value);
  if (text === '') return '';
  const num = Number(text);
  return Number.isFinite(num) && String(num) === text ? num : text;
}

function isPastIsoDate(dateStr: string | null | undefined): boolean {
  if (!dateStr) return false;
  const d = new Date(`${String(dateStr).slice(0, 10)}T12:00:00.000Z`);
  const today = new Date();
  today.setUTCHours(12, 0, 0, 0);
  return d.getTime() < today.getTime();
}

export function assertPdt3035ProdFileExists(): void {
  if (!fs.existsSync(PDT_3035_PROD_FILE_PATH)) {
    throw new Error(
      `Prod mass-import XLSX missing at ${PDT_3035_PROD_FILE_PATH}. ` +
        'Use buildSingleRowMinimalEdit (Dev GET) or download process 2114 file.',
    );
  }
}

export async function readProdMirrorRowFromXlsx(contractNumber: string): Promise<ProdMirrorRow> {
  assertPdt3035ProdFileExists();

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(PDT_3035_PROD_FILE_PATH);
  const worksheet = workbook.getWorksheet(1);
  if (!worksheet) {
    throw new Error(`No worksheet in Prod XLSX: ${PDT_3035_PROD_FILE_PATH}`);
  }

  const target = contractNumber.trim();
  for (let excelRow = 2; excelRow <= worksheet.rowCount; excelRow++) {
    const rowContract = cellText(worksheet.getCell(`A${excelRow}`).value);
    if (rowContract !== target) continue;

    return {
      contractNumber: target,
      version: cellPrimitive(worksheet.getCell(`B${excelRow}`).value) || 1,
      createOrEdit: cellText(worksheet.getCell(`C${excelRow}`).value) || 'E',
      productId: cellPrimitive(worksheet.getCell(`G${excelRow}`).value),
      productVersion: cellPrimitive(worksheet.getCell(`H${excelRow}`).value),
      prodExcelRow: excelRow,
    };
  }

  throw new Error(`Contract ${target} not found in Prod XLSX ${PDT_3035_PROD_FILE_PATH}`);
}

/** Contract numbers column A from prod-process-2114 xlsx (legacy TC-BE-1 scan). */
export async function readProdXlsxContractNumbers(): Promise<string[]> {
  assertPdt3035ProdFileExists();
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(PDT_3035_PROD_FILE_PATH);
  const worksheet = workbook.getWorksheet(1);
  if (!worksheet) {
    throw new Error(`No worksheet in Prod XLSX: ${PDT_3035_PROD_FILE_PATH}`);
  }
  const numbers: string[] = [];
  for (let excelRow = 2; excelRow <= worksheet.rowCount; excelRow++) {
    const rowContract = cellText(worksheet.getCell(`A${excelRow}`).value).trim();
    if (rowContract) {
      numbers.push(rowContract);
    }
  }
  return numbers;
}

export async function findProductContractIdByNumber(
  Request: APIRequestContext,
  contractNumber: string,
): Promise<number> {
  const listRes = await Request.post('product-contract/list', {
    data: {
      page: 0,
      size: 10,
      prompt: contractNumber.trim(),
      excludeOldVersions: true,
      excludeFutureVersions: true,
    },
  });
  await expect(listRes).CheckResponse();
  const body = (await listRes.json()) as {
    content?: Array<{ id?: number; contractNumber?: string }>;
  };
  const target = contractNumber.trim();
  const match =
    body.content?.find((row) => row.contractNumber === target) ??
    body.content?.find((row) => String(row.contractNumber ?? '').includes(target));
  const contractId = Number(match?.id);
  if (!Number.isFinite(contractId) || contractId <= 0) {
    throw new Error(`Contract ${target} not found via product-contract/list on Dev.`);
  }
  return contractId;
}

async function getCustomerManagerId(Request: APIRequestContext, customerId: number): Promise<number> {
  const customerRes = await Request.get(`customer/${customerId}?versionId=${PDT_3035_DEFAULT_VERSION_ID}`);
  await expect(customerRes).CheckResponse();
  const customer = (await customerRes.json()) as {
    managers?: Array<{ id?: number }>;
    accountManagers?: Array<{ accountManagerId?: number; id?: number }>;
  };
  const fromManagers = customer.managers?.[0]?.id;
  if (fromManagers != null && Number.isFinite(Number(fromManagers))) {
    return Number(fromManagers);
  }
  const fromAccountManagers =
    customer.accountManagers?.[0]?.accountManagerId ?? customer.accountManagers?.[0]?.id;
  if (fromAccountManagers != null && Number.isFinite(Number(fromAccountManagers))) {
    return Number(fromAccountManagers);
  }
  throw new Error(`Customer ${customerId} has no account managers — required for TC-BE-2 proxy precondition.`);
}

function mapProxyForPutWithManagerIds(
  proxy: ContractProxyGet,
  managerId: number,
): Record<string, unknown> {
  return {
    id: proxy.id,
    proxyForeignEntityPerson: proxy.proxyForeignEntityPerson ?? false,
    proxyName: proxy.proxyName,
    proxyCustomerIdentifier: proxy.proxyCustomerIdentifier,
    proxyEmail: proxy.proxyEmail,
    proxyPhone: proxy.proxyPhone,
    proxyPowerOfAttorneyNumber: proxy.proxyPowerOfAttorneyNumber,
    proxyData: proxy.proxyData,
    proxyValidTill: proxy.proxyValidTill,
    notaryPublic: proxy.notaryPublic,
    registrationNumber: proxy.registrationNumber,
    areaOfOperation: proxy.areaOfOperation,
    authorizedProxyForeignEntityPerson: proxy.authorizedProxyForeignEntityPerson ?? false,
    proxyAuthorizedByProxy: proxy.proxyAuthorizedByProxy,
    authorizedProxyCustomerIdentifier: proxy.authorizedProxyCustomerIdentifier,
    authorizedProxyEmail: proxy.authorizedProxyEmail,
    authorizedProxyPhone: proxy.authorizedProxyPhone,
    authorizedProxyPowerOfAttorneyNumber: proxy.authorizedProxyPowerOfAttorneyNumber,
    authorizedProxyData: proxy.authorizedProxyData,
    authorizedProxyValidTill: proxy.authorizedProxyValidTill,
    authorizedProxyNotaryPublic: proxy.authorizedProxyNotaryPublic,
    authorizedProxyRegistrationNumber: proxy.authorizedProxyRegistrationNumber,
    authorizedProxyAreaOfOperation: proxy.authorizedProxyAreaOfOperation,
    fileIds: Array.isArray(proxy.fileIds) ? proxy.fileIds : [],
    managerIds: [managerId],
  };
}

function mapProxyForPutOmitManagerIds(proxy: ContractProxyGet): Record<string, unknown> {
  return {
    id: proxy.id,
    proxyForeignEntityPerson: proxy.proxyForeignEntityPerson ?? false,
    proxyName: proxy.proxyName,
    proxyCustomerIdentifier: proxy.proxyCustomerIdentifier,
    proxyEmail: proxy.proxyEmail,
    proxyPhone: proxy.proxyPhone,
    proxyPowerOfAttorneyNumber: proxy.proxyPowerOfAttorneyNumber,
    proxyData: proxy.proxyData,
    proxyValidTill: proxy.proxyValidTill,
    notaryPublic: proxy.notaryPublic,
    registrationNumber: proxy.registrationNumber,
    areaOfOperation: proxy.areaOfOperation,
    authorizedProxyForeignEntityPerson: proxy.authorizedProxyForeignEntityPerson ?? false,
    proxyAuthorizedByProxy: proxy.proxyAuthorizedByProxy,
    authorizedProxyCustomerIdentifier: proxy.authorizedProxyCustomerIdentifier,
    authorizedProxyEmail: proxy.authorizedProxyEmail,
    authorizedProxyPhone: proxy.authorizedProxyPhone,
    authorizedProxyPowerOfAttorneyNumber: proxy.authorizedProxyPowerOfAttorneyNumber,
    authorizedProxyData: proxy.authorizedProxyData,
    authorizedProxyValidTill: proxy.authorizedProxyValidTill,
    authorizedProxyNotaryPublic: proxy.authorizedProxyNotaryPublic,
    authorizedProxyRegistrationNumber: proxy.authorizedProxyRegistrationNumber,
    authorizedProxyAreaOfOperation: proxy.authorizedProxyAreaOfOperation,
    fileIds: Array.isArray(proxy.fileIds) ? proxy.fileIds : [],
    managerIds: [],
  };
}

function buildNewProxyForPut(managerId: number, includeManagerIds = true): Record<string, unknown> {
  const proxy: Record<string, unknown> = {
    proxyForeignEntityPerson: false,
    proxyName: 'PDT3035 MIRROR PROXY',
    proxyCustomerIdentifier: PDT_3035_PROXY_EGN,
    proxyEmail: 'pdt3035-proxy@test.local',
    proxyPhone: '359881234567',
    proxyPowerOfAttorneyNumber: 'POA3035',
    proxyData: '2020-01-01',
    proxyValidTill: '2030-12-31',
    notaryPublic: 'NOTARY',
    registrationNumber: 'REG3035',
    areaOfOperation: 'BG',
    authorizedProxyForeignEntityPerson: false,
  };
  if (includeManagerIds) {
    proxy.managerIds = [managerId];
  } else {
    proxy.managerIds = [];
  }
  return proxy;
}

function mapProxiesFromGetForPut(proxies: ContractProxyGet[]): Array<Record<string, unknown>> {
  return proxies.map((proxy) => {
    const rawIds = proxy.managerIds as unknown;
    const managerIds = Array.isArray(rawIds)
      ? rawIds.map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0)
      : [];
    if (managerIds.length > 0) {
      return mapProxyForPutWithManagerIds(proxy, managerIds[0]);
    }
    return mapProxyForPutOmitManagerIds(proxy);
  });
}


function buildPodRequestsFromContractGet(contract: Record<string, unknown>): Array<Record<string, unknown>> {
  const pods = (contract.contractPodsResponses ?? []) as Array<Record<string, unknown>>;
  const byBillingGroup = new Map<number, Array<Record<string, unknown>>>();

  for (const pod of pods) {
    const billingGroupId = Number(pod.billingGroupId);
    const pointOfDeliveryDetailId = Number(pod.podDetailId);
    if (!Number.isFinite(billingGroupId) || billingGroupId <= 0) continue;
    if (!Number.isFinite(pointOfDeliveryDetailId) || pointOfDeliveryDetailId <= 0) continue;
    if (!byBillingGroup.has(billingGroupId)) {
      byBillingGroup.set(billingGroupId, []);
    }
    byBillingGroup.get(billingGroupId)!.push({
      pointOfDeliveryDetailId,
      dealNumber: pod.dealNumber ?? '',
    });
  }

  return [...byBillingGroup.entries()].map(([billingGroupId, productContractPointOfDeliveries]) => ({
    billingGroupId,
    productContractPointOfDeliveries,
  }));
}

function mapAdditionalParametersForPut(
  additionalParameters: Record<string, unknown>,
): Record<string, unknown> {
  const bankingDetails = (additionalParameters.bankingDetails ?? {}) as Record<string, unknown>;
  const riskAssessment = additionalParameters.riskAssessment;
  const normalizedRisk =
    typeof riskAssessment === 'string' ? riskAssessment.toUpperCase() : riskAssessment;

  return {
    dealNumber: additionalParameters.dealNumber,
    estimatedTotalConsumptionUnderContractKwh:
      additionalParameters.estimatedTotalConsumptionUnderContractKwh,
    interestRateId: additionalParameters.interestRateId,
    employeeId: additionalParameters.employeeId,
    bankingDetails: {
      directDebit: bankingDetails.directDebit ?? false,
      bankId: bankingDetails.bankId ?? null,
      iban: bankingDetails.iban ?? null,
      bic: bankingDetails.bic ?? null,
    },
    riskAssessment: normalizedRisk,
    riskAssessmentAdditionalInformation: additionalParameters.riskAssessmentAdditionalInformation,
  };
}

function mapProductParametersForPut(productParameters: Record<string, unknown>): Record<string, unknown> {
  const contractTerm = productParameters.contractTerm as { id?: number } | undefined;
  const invoicePaymentTerm = productParameters.invoicePaymentTerm as { id?: number } | undefined;

  return {
    contractType: productParameters.contractType,
    productContractTermId: contractTerm?.id ?? productParameters.productContractTermId,
    contractTermEndDate: productParameters.contractTermDate ?? productParameters.contractTermEndDate,
    paymentGuarantee: productParameters.paymentGuarantee,
    cashDeposit: productParameters.cashDeposit,
    cashDepositCurrencyId:
      (productParameters.cashDepositCurrency as { id?: number } | undefined)?.id ??
      productParameters.cashDepositCurrencyId,
    bankGuarantee: productParameters.bankGuarantee,
    bankGuaranteeCurrencyId:
      (productParameters.bankDepositCurrency as { id?: number } | undefined)?.id ??
      productParameters.bankGuaranteeCurrencyId,
    guaranteeInformation: productParameters.guaranteeInformation,
    guaranteeContract: productParameters.guaranteeContract,
    contractFormulas: productParameters.priceComponents ?? productParameters.contractFormulas ?? [],
    invoicePaymentTermId: invoicePaymentTerm?.id ?? productParameters.invoicePaymentTermId,
    invoicePaymentTermValue: productParameters.invoicePaymentTermValue,
    entryIntoForce: productParameters.entryIntoForce,
    entryIntoForceValue: productParameters.entryIntoForceValue,
    startOfContractInitialTerm: productParameters.startOfContractInitialTerm,
    startOfContractValue: productParameters.startOfContractValue,
    supplyActivation: productParameters.supplyActivation,
    supplyActivationValue: productParameters.supplyActivationValue,
    monthlyInstallmentValue: productParameters.monthlyInstallmentValue,
    monthlyInstallmentAmount: productParameters.monthlyInstallmentAmount,
    marginalPrice: productParameters.marginalPrice,
    marginalPriceValidity: productParameters.marginalPriceValidity,
    hourlyLoadProfile: productParameters.hourlyLoadProfile,
    procurementPrice: productParameters.procurementPrice,
    imbalancePriceIncrease: productParameters.imbalancePriceIncrease,
    setMargin: productParameters.setMargin,
    productContractWaitForOldContractTermToExpires:
      productParameters.productContractWaitForOldContractTermToExpires,
    interimAdvancePayments: productParameters.interimAdvancePayments ?? [],
    productContractProductAdditionalParams:
      productParameters.productContractProductAdditionalParamsResponseList ??
      productParameters.productContractProductAdditionalParams ??
      [],
  };
}

function mapBasicParametersForPut(
  bp: ContractBasicParametersGet,
  proxy: Array<Record<string, unknown>>,
): Record<string, unknown> {
  const versionTypes = bp.versionTypesResponse as Array<{ id?: number }> | undefined;
  const versionTypeIds = Array.isArray(versionTypes)
    ? versionTypes.map((item) => Number(item.id)).filter((id) => Number.isFinite(id) && id > 0)
    : [];

  return {
    status: bp.status,
    subStatus: bp.subStatus,
    statusModifyDate: bp.statusModifyDate,
    productId: bp.productId,
    productVersionId: bp.productVersionId,
    type: bp.type ?? 'CONTRACT',
    hasUntilAmount: bp.hasUntilAmount,
    hasUntilVolume: bp.hasUntilVolume,
    procurementLaw: bp.procurementLaw,
    untilAmount: bp.untilAmount,
    untilVolume: bp.untilVolume,
    untilAmountCurrencyId: bp.untilAmountCurrencyId,
    signingDate: bp.signingDate,
    entryInForceDate: bp.entryInForceDate,
    startOfInitialTerm: bp.startOfInitialTerm,
    customerId: bp.customerId,
    customerVersionId: bp.customerVersionId ?? bp.customerDetailId,
    communicationDataBillingId: bp.billingCommunicationData?.id ?? bp.communicationDataBillingId,
    communicationDataContractId: bp.contractCommunicationData?.id ?? bp.communicationDataContractId,
    customerNewDetailsId: bp.customerNewDetailsId,
    files: bp.files,
    documents: bp.documents,
    versionStatus: bp.versionStatus,
    versionTypeIds,
    proxy,
    relatedEntities: bp.relatedEntities,
    terminationDate: bp.terminationDate,
    perpetuityDate: bp.perpetuityDate,
    contractTermEndDate: bp.contractTermEndDate,
  };
}

function buildContractUpdatePayloadFromGet(
  contract: Record<string, unknown>,
  proxy: Array<Record<string, unknown>>,
): Record<string, unknown> {
  const basicParameters = contract.basicParameters as ContractBasicParametersGet;
  const productParameters = contract.productParameters as Record<string, unknown> | undefined;
  const podRequests = buildPodRequestsFromContractGet(contract);
  if (podRequests.length === 0) {
    throw new Error('Cannot build product-contract PUT — contractPodsResponses empty.');
  }

  const versions = contract.versions as Array<{ versionId?: number; startDate?: string }> | undefined;
  const versionId = PDT_3035_DEFAULT_VERSION_ID;
  const startDate =
    versions?.find((v) => Number(v.versionId) === versionId)?.startDate ??
    (basicParameters.statusModifyDate as string | undefined) ??
    null;

  return {
    startDate,
    basicParameters: mapBasicParametersForPut(basicParameters, proxy),
    additionalParameters: mapAdditionalParametersForPut(
      (contract.additionalParameters ?? {}) as Record<string, unknown>,
    ),
    productParameters: productParameters ? mapProductParametersForPut(productParameters) : undefined,
    savingAsNewVersion: false,
    updateDealNumber: false,
    podRequests,
  };
}

export type EnsureProxyResult = {
  contractId: number;
  contractNumber: string;
  proxyAdded: boolean;
  proxyCount: number;
  productId: number;
  productVersionId: number;
};

export async function deleteAllProxiesOnContract(
  fx: Pick<Pdt3035Fx, 'Request'>,
  contractNumber: string,
): Promise<void> {
  const contractId = await findProductContractIdByNumber(fx.Request, contractNumber);
  const versionId = PDT_3035_DEFAULT_VERSION_ID;

  const getRes = await fx.Request.get(`product-contract/${contractId}?versionId=${versionId}`);
  await expect(getRes).CheckResponse();
  const contract = (await getRes.json()) as Record<string, unknown>;
  const putPayload = buildContractUpdatePayloadFromGet(contract, []);

  const putRes = await fx.Request.put(
    `product-contract/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: putPayload },
  );
  await expect(putRes).CheckResponse();

  const verifyRes = await fx.Request.get(`product-contract/${contractId}?versionId=${versionId}`);
  await expect(verifyRes).CheckResponse();
  const verified = (await verifyRes.json()) as Record<string, unknown>;
  const verifiedProxies = ((verified.basicParameters as ContractBasicParametersGet)?.proxy ??
    []) as ContractProxyGet[];
  if (verifiedProxies.length > 0) {
    throw new Error(
      `deleteAllProxiesOnContract: ${contractNumber} still has ${verifiedProxies.length} proxy/proxies after PUT`,
    );
  }
  console.log(`[PDT-3035] deleted all proxies on ${contractNumber} (id=${contractId})`);
}

export async function ensureMirrorContractHasActiveProxy(
  fx: Pick<Pdt3035Fx, 'Request'>,
  contractNumber: string,
  options?: {
    /**
     * TC-BE-2 repro: PUT proxy **without** managerIds so mass-import minimal row
     * (no proxy columns) hits LEGAL_ENTITY managerIds validation on the update path.
     */
    reproManagerIdsMissing?: boolean;
    /** TC-BE-2: refresh existing proxy with managerIds (ACTIVE + managers in DB). */
    refreshExistingProxy?: boolean;
  },
): Promise<EnsureProxyResult> {
  const contractId = await findProductContractIdByNumber(fx.Request, contractNumber);
  const versionId = PDT_3035_DEFAULT_VERSION_ID;

  const getRes = await fx.Request.get(`product-contract/${contractId}?versionId=${versionId}`);
  await expect(getRes).CheckResponse();
  const contract = (await getRes.json()) as Record<string, unknown>;
  const basicParameters = contract.basicParameters as ContractBasicParametersGet;
  const productId = Number(basicParameters.productId);
  const productVersionId = Number(basicParameters.productVersionId);
  const existingProxies = (basicParameters?.proxy ?? []) as ContractProxyGet[];

  const reproManagerIdsMissing = options?.reproManagerIdsMissing === true;
  const refreshExistingProxy = options?.refreshExistingProxy === true;

  if (existingProxies.length > 0 && !reproManagerIdsMissing && !refreshExistingProxy) {
    console.log(
      `[PDT-3035] contract ${contractNumber} (id=${contractId}) already has ${existingProxies.length} proxy/proxies`,
    );
    return {
      contractId,
      contractNumber,
      proxyAdded: false,
      proxyCount: existingProxies.length,
      productId,
      productVersionId,
    };
  }

  const customerId = Number(basicParameters.customerId);
  if (!Number.isFinite(customerId) || customerId <= 0) {
    throw new Error(`Contract ${contractNumber} GET missing basicParameters.customerId`);
  }

  let proxyPayload: Array<Record<string, unknown>>;
  if (reproManagerIdsMissing && existingProxies.length > 0) {
    proxyPayload = existingProxies.map((proxy) => mapProxyForPutOmitManagerIds(proxy));
  } else if (refreshExistingProxy && existingProxies.length > 0) {
    const managerId = await getCustomerManagerId(fx.Request, customerId);
    proxyPayload = existingProxies.map((proxy) => mapProxyForPutWithManagerIds(proxy, managerId));
  } else {
    const managerId = await getCustomerManagerId(fx.Request, customerId);
    proxyPayload = [buildNewProxyForPut(managerId, !reproManagerIdsMissing)];
  }
  const putPayload = buildContractUpdatePayloadFromGet(contract, proxyPayload);

  const putRes = await fx.Request.put(
    `product-contract/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: putPayload },
  );
  await expect(putRes).CheckResponse();

  const verifyRes = await fx.Request.get(`product-contract/${contractId}?versionId=${versionId}`);
  await expect(verifyRes).CheckResponse();
  const verified = (await verifyRes.json()) as Record<string, unknown>;
  const verifiedProxies = ((verified.basicParameters as ContractBasicParametersGet)?.proxy ??
    []) as ContractProxyGet[];

  if (verifiedProxies.length < 1) {
    throw new Error(`PUT product-contract/${contractId} completed but re-GET shows no proxy.`);
  }

  console.log(
    `[PDT-3035] ${reproManagerIdsMissing ? 'repro' : 'added'} proxy on ${contractNumber} (id=${contractId}) ` +
      `managerIdsInPut=${!reproManagerIdsMissing}`,
  );

  return {
    contractId,
    contractNumber,
    proxyAdded: true,
    proxyCount: verifiedProxies.length,
    productId,
    productVersionId,
  };
}

/**
 * Prod process 2114 identical row: clear template row 2, write only A,B,C,G,H from env GET.
 * G/H use env product id/version (not Prod xlsx ids). No column I/O/61 — matches bug upload file shape.
 */
export async function buildProd2114IdenticalMinimalEdit(
  fx: Pick<Pdt3035Fx, 'Request'>,
  contractNumber: string,
): Promise<{ excelPath: string; row: MinimalEditRow; statusColumn: null }> {
  const built = await buildSingleRowMinimalEdit(fx, contractNumber, { clearTemplateRow: true });
  return { ...built, statusColumn: null };
}

/**
 * Build single-row mass-import file from contract GET (A,B,C,G,H; optional I/O for non-Prod paths).
 * Prefer buildProd2114IdenticalMinimalEdit for TC-BE-1 — supplyEntryInForceViaExcel is not Prod reproduction.
 */
export async function buildSingleRowMinimalEdit(
  fx: Pick<Pdt3035Fx, 'Request'>,
  contractNumber: string,
  options?: {
    setStatusColumn?: string;
    forceSignedStatusForPastEif?: boolean;
    /** Clear template row 2 before writing — avoids leftover proxy column values auto-filling managerIds. */
    clearTemplateRow?: boolean;
    /** Override G/H (Dev product id/version from precondition). */
    productId?: number;
    productVersion?: number;
    /** Prod mirror TC-BE-2: only A,B,C — product from existing contract (omit G/H). */
    omitProductColumns?: boolean;
    /** Prod NPE row: column O (index 14) future EIF + column 61 MANUAL — avoids PUT that coerces null until-term to false. */
    supplyEntryInForceViaExcel?: boolean;
    /** Set column 23 (proxy name) — triggers excel proxy path; use existing proxy name from GET. */
    setProxyNameColumn?: boolean;
  },
): Promise<{ excelPath: string; row: MinimalEditRow; statusColumn: string | null }> {
  const contractId = await findProductContractIdByNumber(fx.Request, contractNumber);
  const versionId = PDT_3035_DEFAULT_VERSION_ID;

  const getRes = await fx.Request.get(`product-contract/${contractId}?versionId=${versionId}`);
  await expect(getRes).CheckResponse();
  const contract = (await getRes.json()) as Record<string, unknown>;
  const basicParameters = contract.basicParameters as ContractBasicParametersGet;

  const productId =
    options?.productId != null && Number.isFinite(Number(options.productId))
      ? Number(options.productId)
      : Number(basicParameters.productId);
  const productVersionId =
    options?.productVersion != null && Number.isFinite(Number(options.productVersion))
      ? Number(options.productVersion)
      : Number(basicParameters.productVersionId);
  if (!Number.isFinite(productId) || productId <= 0) {
    throw new Error(`Contract ${contractNumber} GET missing basicParameters.productId`);
  }
  if (!Number.isFinite(productVersionId) || productVersionId <= 0) {
    throw new Error(`Contract ${contractNumber} GET missing basicParameters.productVersionId`);
  }

  const entryInForceDate = (basicParameters.entryInForceDate as string | null | undefined) ?? null;
  const status = (basicParameters.status as string | null | undefined) ?? null;
  const existingProxies = (basicParameters.proxy ?? []) as ContractProxyGet[];
  const proxyNameForColumn =
    options?.setProxyNameColumn && existingProxies[0]?.proxyName
      ? String(existingProxies[0].proxyName)
      : null;

  let statusColumn = options?.setStatusColumn ?? null;
  if (
    options?.forceSignedStatusForPastEif &&
    !statusColumn &&
    status !== 'SIGNED' &&
    isPastIsoDate(entryInForceDate)
  ) {
    statusColumn = 'SIGNED';
  }

  const templateRes = await fx.Request.get(PDT_3035_TEMPLATE_DOWNLOAD);
  if (!templateRes.ok()) {
    const text = await templateRes.text();
    throw new Error(`Failed to download mass-import template: HTTP ${templateRes.status()} ${text.slice(0, 300)}`);
  }

  const outDir = path.join(os.tmpdir(), 'energots-pdt-3035-imports');
  fs.mkdirSync(outDir, { recursive: true });
  const excelPath = path.join(
    outDir,
    `pdt-3035-minimal-edit-${contractNumber.trim()}-${Date.now()}.xlsx`,
  );
  fs.writeFileSync(excelPath, Buffer.from(await templateRes.body()));

  const row: MinimalEditRow = {
    contractNumber: contractNumber.trim(),
    version: versionId,
    createOrEdit: 'E',
    productId,
    productVersion: productVersionId,
    contractId,
    status,
    entryInForceDate,
    statusColumn,
  };

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(excelPath);
  const worksheet = workbook.getWorksheet(1);
  if (!worksheet) {
    throw new Error('No worksheet found in PRODUCT_CONTRACTS mass-import template');
  }

  if (options?.clearTemplateRow) {
    clearMassImportTemplateDataRow(worksheet);
  }

  worksheet.getCell('A2').value = row.contractNumber;
  worksheet.getCell('B2').value = row.version;
  worksheet.getCell('C2').value = row.createOrEdit;
  if (!options?.omitProductColumns) {
    worksheet.getCell('G2').value = row.productId;
    worksheet.getCell('H2').value = row.productVersion;
  }
  if (proxyNameForColumn) {
    worksheet.getCell('X2').value = proxyNameForColumn;
  }
  if (statusColumn) {
    worksheet.getCell('I2').value = statusColumn;
  }
  if (options?.supplyEntryInForceViaExcel) {
    const eifCol = excelColumnLetter(14);
    const entryIntoForceCol = excelColumnLetter(61);
    const futureEif = addDaysIso(30);
    worksheet.getCell(`${eifCol}2`).value = new Date(`${futureEif}T12:00:00.000Z`);
    worksheet.getCell(`${entryIntoForceCol}2`).value = 'MANUAL';
  }

  await workbook.xlsx.writeFile(excelPath);

  console.log(
    `[PDT-3035] built minimal-edit: contract=${row.contractNumber} id=${contractId} ` +
      `status=${status} eif=${entryInForceDate}` +
      (options?.omitProductColumns ? ' (A,B,C only)' : ` G=${row.productId} H=${row.productVersion}`) +
      (statusColumn ? ` I=${statusColumn}` : '') +
      (options?.supplyEntryInForceViaExcel ? ` O=${addDaysIso(30)} entryIntoForce=MANUAL` : ''),
  );

  return { excelPath, row, statusColumn };
}

function isProductContractMassImportProcess(body: ProcessSnapshot): boolean {
  if (body.processType === 'PRODUCT_CONTRACT_MASS_IMPORT') return true;
  return String(body.name ?? '').startsWith('PRODUCT_CONTRACT_MASS_IMPORT');
}

async function listProductContractMassImportProcessIds(Request: APIRequestContext): Promise<number[]> {
  const res = await Request.get('process', {
    params: { page: 0, size: 50, sortBy: 'ID', sortDirection: 'DESC' },
  });
  if (!res.ok()) return [];
  const body = (await res.json()) as { content?: Array<{ id?: number; name?: string; processType?: string }> };
  return (body.content ?? [])
    .filter((process) => isProductContractMassImportProcess(process))
    .map((process) => Number(process.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

async function fetchNotifications(Request: APIRequestContext, size = 50): Promise<MassImportNotificationItem[]> {
  const res = await Request.get(`notifications?size=${size}&page=0`);
  if (!res.ok()) throw new Error(`notifications fetch failed: HTTP ${res.status()}`);
  const body = (await res.json()) as { content?: MassImportNotificationItem[] };
  return Array.isArray(body?.content) ? body.content : [];
}

async function collectKnownMassImportProcessIds(Request: APIRequestContext): Promise<Set<number>> {
  const known = new Set<number>();
  const notifications = await fetchNotifications(Request, 50);
  for (const notification of notifications) {
    if (!notification.notificationType || notification.entityId == null) continue;
    if (MASS_IMPORT_NOTIFICATION_TYPES.test(String(notification.notificationType))) {
      known.add(Number(notification.entityId));
    }
  }
  for (const processId of await listProductContractMassImportProcessIds(Request)) {
    known.add(processId);
  }
  return known;
}

async function verifyMassImportProcessReady(
  Request: APIRequestContext,
  processId: number,
): Promise<'completed' | 'in_progress' | 'invalid'> {
  const processRes = await Request.get(`process/${processId}`);
  if (!processRes.ok()) return 'invalid';
  const processBody = (await processRes.json()) as ProcessSnapshot;
  if (!isProductContractMassImportProcess(processBody)) return 'invalid';
  if (processBody.status === 'COMPLETED') return 'completed';
  if (['IN_PROGRESS', 'NOT_STARTED', 'AWAITING'].includes(String(processBody.status))) {
    return 'in_progress';
  }
  return 'invalid';
}

async function tryResolveNewMassImportProcess(
  Request: APIRequestContext,
  knownProcessIds: Set<number>,
  uploadStartedAt: Date,
): Promise<{ processId: number; notificationType: string } | null> {
  const completedPattern = /PRODUCT_CONTRACT_MASS_IMPORT_COMPLETED/i;
  const errorPattern = /PRODUCT_CONTRACT_MASS_IMPORT_ERROR/i;
  const cutoffMs = uploadStartedAt.getTime() - 2000;

  const processIdsNow = await listProductContractMassImportProcessIds(Request);
  const newProcessIds = processIdsNow.filter((id) => !knownProcessIds.has(id));
  if (newProcessIds.length > 0) {
    const processId = Math.max(...newProcessIds);
    const state = await verifyMassImportProcessReady(Request, processId);
    if (state === 'completed') {
      return { processId, notificationType: 'PRODUCT_CONTRACT_MASS_IMPORT_COMPLETED' };
    }
    if (state === 'in_progress') {
      return { processId, notificationType: 'PRODUCT_CONTRACT_MASS_IMPORT_IN_PROGRESS' };
    }
  }

  const notifications = await fetchNotifications(Request, 50);
  for (const notification of notifications) {
    if (!notification.notificationType) continue;
    const notificationType = String(notification.notificationType);
    if (!completedPattern.test(notificationType) && !errorPattern.test(notificationType)) continue;
    if (notification.entityId == null) continue;
    const processId = Number(notification.entityId);
    if (knownProcessIds.has(processId)) continue;
    if (notification.createDate) {
      const createMs = new Date(notification.createDate).getTime();
      if (createMs < cutoffMs) continue;
    }
    const state = await verifyMassImportProcessReady(Request, processId);
    if (state === 'completed') return { processId, notificationType };
    if (state === 'in_progress') return { processId, notificationType };
  }

  return null;
}

async function waitForMassImportNotification(
  Request: APIRequestContext,
  uploadStartedAt: Date,
  knownProcessIds: Set<number>,
  uploadLabel?: string,
): Promise<{ processId: number; notificationType: string }> {
  let pendingProcessId: number | null = null;
  let pendingNotificationType = 'PRODUCT_CONTRACT_MASS_IMPORT_COMPLETED';

  for (let attempt = 0; attempt < 45; attempt++) {
    if (pendingProcessId != null) {
      const state = await verifyMassImportProcessReady(Request, pendingProcessId);
      if (state === 'completed') {
        return { processId: pendingProcessId, notificationType: pendingNotificationType };
      }
      if (state === 'invalid') {
        pendingProcessId = null;
      } else {
        await new Promise((r) => setTimeout(r, 1000));
        continue;
      }
    }

    const candidate = await tryResolveNewMassImportProcess(Request, knownProcessIds, uploadStartedAt);
    if (candidate) {
      const state = await verifyMassImportProcessReady(Request, candidate.processId);
      if (state === 'completed') return candidate;
      pendingProcessId = candidate.processId;
      pendingNotificationType = candidate.notificationType;
      continue;
    }

    await new Promise((r) => setTimeout(r, 1000));
  }

  throw new Error(
    `PRODUCT_CONTRACT mass import process not detected within 45s` +
      (uploadLabel ? ` (upload: ${uploadLabel})` : ''),
  );
}

async function downloadMassImportErrorReportBuffer(
  Request: APIRequestContext,
  processId: number,
): Promise<Buffer | null> {
  const reportFile = await Request.get(
    `process/${processId}/report/download?multiSheetExcelType=MASS_IMPORT_ERROR_REPORT`,
  );
  if (!reportFile.ok()) return null;
  return Buffer.from(await reportFile.body());
}

function classifyErrorRows(rows: MassImportErrorRow[]): {
  failedRowCount: number;
  npeCount: number;
  managerIdsCount: number;
  eifCount: number;
} {
  let npeCount = 0;
  let managerIdsCount = 0;
  let eifCount = 0;

  for (const row of rows) {
    const text = row.errorText;
    if (!text) continue;
    if (PDT_3035_NPE_PATTERNS.test(text)) npeCount += 1;
    if (PDT_3035_MANAGER_IDS_PATTERN.test(text)) managerIdsCount += 1;
    if (PDT_3035_EIF_PATTERN.test(text)) eifCount += 1;
  }

  return {
    failedRowCount: rows.filter((row) => row.errorText.length > 0).length,
    npeCount,
    managerIdsCount,
    eifCount,
  };
}

export async function loadMassImportErrorReportSnapshot(
  Request: APIRequestContext,
  processId: number,
): Promise<MassImportErrorReportSnapshot> {
  const buffer = await downloadMassImportErrorReportBuffer(Request, processId);
  if (!buffer) {
    return {
      totalRows: 0,
      failedRowCount: 0,
      npeCount: 0,
      managerIdsCount: 0,
      eifCount: 0,
      failedRows: [],
      reportPath: null,
    };
  }

  const outDir = path.join(os.tmpdir(), 'energots-pdt-3035-reports');
  fs.mkdirSync(outDir, { recursive: true });
  const reportPath = path.join(outDir, `mass-import-report-${processId}-${Date.now()}.xlsx`);
  fs.writeFileSync(reportPath, buffer);

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buffer);
  const worksheet = workbook.worksheets[0];
  if (!worksheet) {
    return {
      totalRows: 0,
      failedRowCount: 0,
      npeCount: 0,
      managerIdsCount: 0,
      eifCount: 0,
      failedRows: [],
      reportPath,
    };
  }

  const failedRows: MassImportErrorRow[] = [];
  worksheet.eachRow((row, rowNumber) => {
    if (rowNumber === 1) return;
    const errorText = cellText(row.getCell(4).value);
    const hasAnyContent = [1, 2, 3, 4].some((col) => cellText(row.getCell(col).value) !== '');
    if (!hasAnyContent) return;
    if (errorText) {
      failedRows.push({ excelRow: rowNumber, errorText });
    }
  });

  const counts = classifyErrorRows(failedRows);

  return {
    totalRows: failedRows.length > 0 ? failedRows.length : worksheet.rowCount > 1 ? worksheet.rowCount - 1 : 0,
    ...counts,
    failedRows,
    reportPath,
  };
}

export async function uploadProductContractMassImportAndReadReport(
  fx: Pdt3035Fx,
  excelPath: string,
): Promise<MassImportRunResult> {
  const uploadedBasename = path.basename(excelPath);
  const knownProcessIds = await collectKnownMassImportProcessIds(fx.Request);
  const buffer = fs.readFileSync(excelPath);
  const uploadStartedAt = new Date();

  const upload = await fx.FileUploadRequest.post(PDT_3035_UPLOAD, {
    multipart: {
      file: {
        name: uploadedBasename,
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer,
      },
    },
  });
  if (!upload.ok()) {
    const text = await upload.text();
    throw new Error(`Mass import upload failed ${upload.status()}: ${text.slice(0, 500)}`);
  }

  const { processId } = await waitForMassImportNotification(
    fx.Request,
    uploadStartedAt,
    knownProcessIds,
    uploadedBasename,
  );

  const snapshot = await loadMassImportErrorReportSnapshot(fx.Request, processId);

  console.log(
    `[PDT-3035] processId=${processId} failed=${snapshot.failedRowCount} ` +
      `npe=${snapshot.npeCount} managerIds=${snapshot.managerIdsCount} eif=${snapshot.eifCount}`,
  );

  return { processId, snapshot };
}
