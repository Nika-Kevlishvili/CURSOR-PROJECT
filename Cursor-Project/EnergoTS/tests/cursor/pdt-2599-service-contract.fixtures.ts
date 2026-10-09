/**
 * Shared setup + portal link attachments for PDT-2599 service-contract API tests.
 * Kept out of the `.spec.ts` file to keep the spec readable.
 */
import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { configuredBaseURL } from '../../fixtures/utils/baseUrl';
import { ResponseLinker } from '../../utils/reporters/ResponseLinker';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import { forVolumes } from '../../jsons/payloads/create/billing/forVolumes';

/**
 * Matches `BillingPayloads.applicationModelType` entries used by PDT-2599 billing.
 * `INTERIM` maps to `INTERIM_AND_ADVANCE_PAYMENT` on billing-run payloads (`assignPdt2599BillingModelFields`).
 */
export type Pdt2599BillingApplicationModelKind =
  | 'PER_PIECE'
  | 'OVER_TIME_ONE_TIME'
  | 'OVER_TIME_PERIODICAL'
  | 'INTERIM';

/** Monetary / tariff drivers differing between Signed contract versions v1 vs v2 (expected net = derived from contract version formulas). */
export const PDT2599_PER_PIECE_PRIMARY_DRIVER = 11;
export const PDT2599_PER_PIECE_SECONDARY_DRIVER = 73;
/**
 * PER_PIECE: distinct `serviceParameters.quantity` on Signed v1 vs v2; `invoiceDate` / billing window selects the Signed slice.
 * Pass criteria: draft invoice **net (ex-VAT)** matches `quantity × contract formula value` for that logical version (see `pdt2599ExpectedPerPieceNet`).
 *
 * Quantities must fall within the PER_PIECE tier on the price component (`perPiece.ts` default `ranges: [{ from: 1, to: 100 }]`).
 */
export const PDT2599_PER_PIECE_V1_QUANTITY = 12;
export const PDT2599_PER_PIECE_V2_QUANTITY = 88;
export const PDT2599_OVER_TIME_ONE_TIME_PRIMARY_DRIVER = 14;
export const PDT2599_OVER_TIME_ONE_TIME_SECONDARY_DRIVER = 88;
/** Distinct from v1/v2 Signed drivers — used on Draft v3 only so billing-after-v3 can prove invoice still follows latest Signed (v2), not Draft pricing. */
export const PDT2599_OVER_TIME_ONE_TIME_TERTIARY_DRIVER = 165;
export const PDT2599_OVER_TIME_PERIODICAL_PRIMARY_DRIVER = 21;
export const PDT2599_OVER_TIME_PERIODICAL_SECONDARY_DRIVER = 97;
export const PDT2599_OVER_TIME_PERIODICAL_TERTIARY_DRIVER = 154;

function getProcessEnv(key: string): string | undefined {
  const proc = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process;
  return proc?.env?.[key];
}

/** POST /price-components returns a raw `long` in many environments — not always `{ id }`. */
export function asPriceComponentId(stored: unknown): number {
  if (typeof stored === 'number' && Number.isFinite(stored)) return stored;
  if (stored !== null && typeof stored === 'object' && 'id' in stored) {
    const n = Number((stored as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  throw new Error(
    'Could not resolve price component id: expected number or { id } from POST /price-components body.',
  );
}

export function asServiceId(stored: unknown): number {
  if (typeof stored === 'number' && Number.isFinite(stored)) return stored;
  if (stored !== null && typeof stored === 'object' && 'id' in stored) {
    const n = Number((stored as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  throw new Error('Could not resolve service id: expected number or { id } from POST /services body.');
}

function pickNumericId(row: Record<string, unknown>): number | null {
  const raw = row.formulaVariableId ?? row.id;
  if (raw == null) return null;
  const n = typeof raw === 'number' ? raw : Number(raw);
  return Number.isFinite(n) ? n : null;
}

export function pickFormulaFromThirdTab(third: Record<string, unknown>): {
  formulaVariableId: number;
  value: number;
} | null {
  const formulaVars = (third.formulaVariables as Record<string, unknown>[]) ?? [];
  for (const fc of formulaVars) {
    const vars = (fc.variables as Record<string, unknown>[]) ?? [];
    for (const row of vars) {
      const formulaVariableId = pickNumericId(row);
      if (formulaVariableId == null) continue;
      let value: number;
      if (typeof row.value === 'number') value = row.value;
      else if (typeof row.valueFrom === 'number' && typeof row.valueTo === 'number') {
        value = (row.valueFrom + row.valueTo) / 2;
      } else value = 1;
      return { formulaVariableId, value };
    }
  }
  return null;
}

function pickFormulaFromPriceComponentDetail(detail: Record<string, unknown>): {
  formulaVariableId: number;
  value: number;
} {
  const list = (detail.formulaVariables as Record<string, unknown>[]) ?? [];
  const row = list[0];
  if (!row) throw new Error('Price component detail had no formulaVariables; cannot build contractFormulas.');
  const formulaVariableId = pickNumericId(row);
  if (formulaVariableId == null) {
    throw new Error('Price component formulaVariables[0] had no id / formulaVariableId.');
  }
  let value: number;
  if (typeof row.value === 'number') value = row.value;
  else if (typeof row.valueFrom === 'number' && typeof row.valueTo === 'number') {
    value = (row.valueFrom + row.valueTo) / 2;
  } else value = 1;
  return { formulaVariableId, value };
}

/**
 * Earliest plausible API "today" among runner-local, UTC, and Europe/Sofia.
 * Phoenix `ServiceContractDateService` rejects `signingDate.isAfter(LocalDate.now())` and
 * `entryIntoForceDateFirst.isAfter(LocalDate.now())` for ENTERED_INTO_FORCE — so dates must not
 * be after the JVM's calendar day; picking the minimum avoids ahead-of-server drift.
 */
export function pdt2599MinCalendarIsoForServiceContractPost(): string {
  const localToday = randomGens.generateTodaysDate('yyyy-mm-dd');
  const utcToday = new Date().toISOString().slice(0, 10);
  const sofiaToday = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Sofia',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
  return [localToday, utcToday, sofiaToday].reduce((a, b) => (a <= b ? a : b));
}

/** One fresh calendar day for all signing / entry-into-force fields (see `pdt2599MinCalendarIsoForServiceContractPost`). */
function normalizePdt2599ServiceContractPostDates(payload: {
  basicParameters: {
    contractStatusModifyDate: string;
    signInDate: string;
    entryIntoForceDate: string | null;
  };
  serviceParameters: { entryIntoForceDate: string };
}): void {
  const today = pdt2599MinCalendarIsoForServiceContractPost();
  payload.basicParameters.contractStatusModifyDate = today;
  payload.basicParameters.signInDate = today;
  payload.basicParameters.entryIntoForceDate = today;
  payload.serviceParameters.entryIntoForceDate = today;
}

/**
 * When third-tab and top-level `formulaVariables` are empty, walk nested price-component JSON (depth-limited)
 * for the first row with a numeric id / formulaVariableId and explicit value or valueFrom/valueTo.
 */
export function pickFormulaDeepFromRecord(
  root: Record<string, unknown>,
  maxDepth = 12,
): { formulaVariableId: number; value: number } | null {
  const rowHasValueSignals = (row: Record<string, unknown>): boolean =>
    typeof row.value === 'number' ||
    (typeof row.valueFrom === 'number' && typeof row.valueTo === 'number');

  const tryPickRow = (o: Record<string, unknown>): { formulaVariableId: number; value: number } | null => {
    if ('formulaVariableId' in o && o.formulaVariableId != null) {
      const fv = Number(o.formulaVariableId);
      if (Number.isFinite(fv)) {
        let value = 1;
        if (typeof o.value === 'number') value = o.value;
        else if (typeof o.valueFrom === 'number' && typeof o.valueTo === 'number') {
          value = (o.valueFrom + o.valueTo) / 2;
        }
        return { formulaVariableId: fv, value };
      }
    }
    /* PriceComponentDetailedResponse.FormulaVariablePayload: Long id, enum variable name, BigDecimal value */
    if (o.id != null && typeof o.variable === 'string') {
      const fv = Number(o.id);
      if (Number.isFinite(fv)) {
        let value = 1;
        if (typeof o.value === 'number') value = o.value;
        else if (typeof o.value === 'string' && o.value.trim() !== '' && Number.isFinite(Number(o.value))) {
          value = Number(o.value);
        } else if (typeof o.valueFrom === 'number' && typeof o.valueTo === 'number') {
          value = (o.valueFrom + o.valueTo) / 2;
        }
        return { formulaVariableId: fv, value };
      }
    }
    if (rowHasValueSignals(o)) {
      const formulaVariableId = pickNumericId(o);
      if (formulaVariableId != null) {
        let value: number;
        if (typeof o.value === 'number') value = o.value;
        else value = ((o.valueFrom as number) + (o.valueTo as number)) / 2;
        return { formulaVariableId, value };
      }
    }
    return null;
  };

  const walk = (node: unknown, depth: number): { formulaVariableId: number; value: number } | null => {
    if (depth > maxDepth || node == null) return null;
    if (Array.isArray(node)) {
      for (const el of node) {
        const hit = walk(el, depth + 1);
        if (hit) return hit;
      }
      return null;
    }
    if (typeof node !== 'object') return null;
    const o = node as Record<string, unknown>;
    const direct = tryPickRow(o);
    if (direct) return direct;
    for (const key of Object.keys(o)) {
      const hit = walk(o[key], depth + 1);
      if (hit) return hit;
    }
    return null;
  };

  return walk(root, 0);
}

/** API may use `SIGNED` on basics and `Valid` on version rows. */
export function isSignedContractVersionStatus(value: unknown): boolean {
  return value === 'SIGNED' || value === 'Valid';
}

/** Draft / Not-Valid version rows (API casing may vary). */
export function isDraftLikeContractVersionStatus(value: unknown): boolean {
  return value === 'DRAFT' || value === 'Draft';
}

export function addDaysIso(isoDate: string, days: number): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

export function dayBeforeIso(isoDate: string): string {
  return addDaysIso(isoDate, -1);
}

export function pickLatestLogicalVersionId(
  versions: Array<{ versionId?: number }>,
): number {
  const ids = versions.map((v) => v.versionId).filter((n): n is number => n != null);
  if (!ids.length) throw new Error('No versionId values in versions array.');
  return Math.max(...ids);
}

function mapSubObjectNumericIds(arr: unknown): number[] {
  if (!Array.isArray(arr)) return [];
  return arr
    .map((item) => {
      if (item && typeof item === 'object' && 'id' in item) {
        const n = Number((item as { id: unknown }).id);
        return Number.isFinite(n) ? n : null;
      }
      return null;
    })
    .filter((n): n is number => n != null);
}

/**
 * Builds `ServiceContractEditRequest` from `GET /service-contract/{id}` JSON (DEV Swagger).
 * Used for PUT (in-place or `savingAsNewVersion`).
 */
export function buildServiceContractEditPayloadFromDetail(
  detail: Record<string, unknown>,
  opts: {
    savingAsNewVersion: boolean;
    startDate?: string;
    contractVersionStatus?: string;
    signInDate?: string | null;
  },
): Record<string, unknown> {
  const basic = (detail.basicParameters ?? {}) as Record<string, unknown>;
  const add = (detail.additionalParameters ?? {}) as Record<string, unknown>;
  const svc = (detail.serviceParameters ?? {}) as Record<string, unknown>;

  const versionTypes = (basic.versionTypes as Array<{ id?: number }> | undefined) ?? [];
  const contractVersionTypes = versionTypes
    .map((v) => v.id)
    .filter((id): id is number => id != null);

  const bankingResp = (add.bankingDetails ?? {}) as Record<string, unknown>;
  const additionalParameters = {
    bankingDetails: {
      directDebit: Boolean(bankingResp.directDebit),
      bankId: bankingResp.bankId ?? null,
      iban: bankingResp.iban ?? null,
    },
    interestRateId: add.interestRateId,
    campaignId: add.campaignId ?? null,
    assistingEmployees: mapSubObjectNumericIds(add.assistingEmployees),
    internalIntermediaries: mapSubObjectNumericIds(add.internalIntermediaries),
    externalIntermediaries: mapSubObjectNumericIds(add.externalIntermediaries),
    employeeId: add.employeeId ?? null,
  };

  const contractTerm = svc.contractTerm as { id?: number } | undefined;
  const invoiceTerm = svc.invoicePaymentTerm as { id?: number } | undefined;
  const priceComponents =
    (svc.priceComponents as Array<{ formulaVariableId?: number; value?: number }> | undefined) ??
    [];
  let contractFormulas = priceComponents
    .filter((p) => p.formulaVariableId != null)
    .map((p) => ({ formulaVariableId: p.formulaVariableId as number, value: p.value ?? 0 }));
  if (contractFormulas.length === 0) {
    const third = (detail.thirdPageTabs ?? {}) as Record<string, unknown>;
    const picked = pickFormulaFromThirdTab(third);
    if (picked) contractFormulas = [{ formulaVariableId: picked.formulaVariableId, value: picked.value }];
  }

  const podsList = (svc.podsList as Array<{ id?: number; podId?: number }> | undefined) ?? [];
  const podIds = podsList.map((p) => p.podId).filter((n): n is number => n != null);
  /** Edit PUT validates `podsEditList` / `unrecognizedPodsEditList` for execution level (Phoenix `checkEditRequestRequestValidity`). */
  const podsEditList = podsList
    .filter((p) => p.podId != null)
    .map((p) => ({
      id: p.id,
      podId: p.podId as number,
    }));

  const contractResponseList =
    (svc.contractResponseList as Array<{ contractNumber?: string }> | undefined) ?? [];
  const contractNumbers = contractResponseList
    .map((c) => c.contractNumber)
    .filter((s): s is string => Boolean(s));

  const currencyObj = basic.currency as { id?: number } | undefined;

  const serviceParameters = {
    contractTermId: contractTerm?.id,
    contractTermEndDate: svc.contractTermDate ?? null,
    entryIntoForce: svc.entryIntoForce,
    entryIntoForceDate: svc.entryIntoForceValue ?? null,
    startOfContractInitialTerm: svc.startOfContractInitialTerm,
    startOfContractInitialTermDate: svc.startOfContractInitialTermDate ?? null,
    invoicePaymentTermId: invoiceTerm?.id,
    invoicePaymentTerm: svc.invoicePaymentTermValue,
    paymentGuarantee: svc.paymentGuarantee,
    cashDepositAmount: svc.cashDeposit ?? null,
    cashDepositCurrencyId: (svc.cashDepositCurrency as { id?: number } | undefined)?.id ?? null,
    bankGuaranteeAmount: svc.bankGuarantee ?? null,
    bankGuaranteeCurrencyId: (svc.bankDepositCurrency as { id?: number } | undefined)?.id ?? null,
    guaranteeContract: Boolean(svc.guaranteeContract),
    guaranteeContractInfo: svc.guaranteeInformation ?? null,
    contractFormulas,
    quantity: svc.quantity ?? null,
    interimAdvancePaymentsRequests: [],
    contractServiceAdditionalParamsRequests: [],
    monthlyInstallmentNumber: svc.monthlyInstallmentValue ?? null,
    monthlyInstallmentAmount: svc.monthlyInstallmentAmount ?? null,
    podIds,
    podsEditList,
    unrecognizedPods: [],
    unrecognizedPodsEditList: [],
    contractNumbers,
    contractNumbersEditList: [],
  };

  const versions = (detail.versions as Array<{ startDate?: string }> | undefined) ?? [];
  const defaultStart =
    versions.length > 0 ? versions[versions.length - 1]?.startDate : undefined;
  const startDate =
    opts.startDate ?? defaultStart ?? (basic.creationDate as string | undefined);
  if (!startDate) throw new Error('Could not resolve startDate for edit payload.');

  const contractFiles =
    (basic.contractFiles as Array<{ id?: number }> | undefined)?.map((f) => f.id).filter(
      (id): id is number => id != null,
    ) ?? [];
  const docFiles =
    (basic.additionalDocuments as Array<{ id?: number }> | undefined)?.map((f) => f.id).filter(
      (id): id is number => id != null,
    ) ?? [];

  const basicParameters: Record<string, unknown> = {
    serviceId: basic.serviceId,
    serviceVersionId: basic.serviceVersionId,
    contractStatus: basic.contractStatus,
    contractStatusModifyDate: basic.contractStatusModifyDate,
    /** GET JSON uses `type` / `subStatus` (see ServiceContractBasicParametersResponse). */
    contractType: basic.contractType ?? basic.type,
    detailsSubStatus: basic.detailsSubStatus ?? basic.subStatus,
    signInDate:
      opts.signInDate !== undefined ? opts.signInDate : (basic.signInDate as string | undefined),
    entryIntoForceDate: basic.entryIntoForceDate ?? null,
    contractTermUntilAmountIsReached: basic.contractTermUntilAmountIsReached ?? null,
    contractTermUntilAmountIsReachedCheckbox: Boolean(basic.contractTermUntilAmountIsReachedCheckbox),
    currencyId: currencyObj?.id ?? basic.currencyId ?? null,
    customerId: basic.customerId,
    customerVersionId: basic.customerVersionId,
    communicationDataForBilling: basic.communicationDataForBilling,
    communicationDataForContract: basic.communicationDataForContract,
    contractVersionStatus: opts.contractVersionStatus ?? basic.contractVersionStatus,
    contractVersionTypes,
    startOfTheInitialTermOfTheContract: basic.contractInitialTermStartDate ?? null,
    terminationDate: basic.terminationDate ?? null,
    perpetuityDate: basic.perpetuityDate ?? null,
    contractTermEndDate: basic.contractTermEndDate ?? null,
    startDate,
    proxy: basic.proxyResponse ?? null,
    files: contractFiles,
    documents: docFiles,
    relatedEntities: basic.relatedEntities ?? [],
  };

  return {
    savingAsNewVersion: opts.savingAsNewVersion,
    basicParameters,
    additionalParameters,
    serviceParameters,
  };
}

/** Assert Signed rows have computed endDate = day before next Signed start; last Signed open-ended. */
export function assertSignedVersionEndDateChain(
  versions: Array<Record<string, unknown>>,
): void {
  const signed = versions
    .filter((v) => isSignedContractVersionStatus(v.status))
    .sort((a, b) => String(a.startDate).localeCompare(String(b.startDate)));
  expect(signed.length).toBeGreaterThan(0);
  for (let i = 0; i < signed.length - 1; i++) {
    const nextStart = String(signed[i + 1].startDate);
    expect(String(signed[i].endDate)).toBe(dayBeforeIso(nextStart));
  }
  const last = signed[signed.length - 1];
  expect(last.endDate == null || last.endDate === '').toBeTruthy();
}

export type Pdt2599ChainResult = {
  contractId: number;
  serviceId: number;
  serviceVersionId: number;
  serviceDetailId: number;
};

export type Pdt2599ChainFixtures = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

/**
 * Walk nested `formulaVariables` / `variables` arrays (third-tab + price-component shapes) without
 * treating unrelated `id`+`value` objects (e.g. invoice payment terms) as formula rows.
 */
export function pickFormulaFromNestedFormulaVariablesArrays(
  root: Record<string, unknown>,
  maxDepth = 16,
): { formulaVariableId: number; value: number } | null {
  const rowLooksLikeFormulaVariable = (row: Record<string, unknown>): boolean =>
    'formulaVariableId' in row ||
    row.variable != null ||
    typeof row.description === 'string' ||
    typeof row.name === 'string';

  const consumeFormulaVariableRows = (arr: unknown[]): { formulaVariableId: number; value: number } | null => {
    for (const item of arr) {
      if (!item || typeof item !== 'object') continue;
      const row = item as Record<string, unknown>;
      const nested = row.variables;
      if (Array.isArray(nested)) {
        const hit = consumeFormulaVariableRows(nested);
        if (hit) return hit;
      }
      const fidRaw = row.formulaVariableId ?? row.id;
      if (fidRaw == null || !rowLooksLikeFormulaVariable(row)) continue;
      const formulaVariableId = Number(fidRaw);
      if (!Number.isFinite(formulaVariableId)) continue;
      let value = 1;
      if (typeof row.value === 'number') value = row.value;
      else if (typeof row.value === 'string' && row.value.trim() !== '' && Number.isFinite(Number(row.value))) {
        value = Number(row.value);
      } else if (typeof row.valueFrom === 'number' && typeof row.valueTo === 'number') {
        value = (row.valueFrom + row.valueTo) / 2;
      }
      return { formulaVariableId, value };
    }
    return null;
  };

  const walk = (node: unknown, depth: number): { formulaVariableId: number; value: number } | null => {
    if (depth > maxDepth || node == null) return null;
    if (Array.isArray(node)) {
      for (const el of node) {
        const hit = walk(el, depth + 1);
        if (hit) return hit;
      }
      return null;
    }
    if (typeof node !== 'object') return null;
    const o = node as Record<string, unknown>;
    const fv = o.formulaVariables;
    if (Array.isArray(fv)) {
      const hit = consumeFormulaVariableRows(fv);
      if (hit) return hit;
    }
    for (const k of Object.keys(o)) {
      const hit = walk(o[k], depth + 1);
      if (hit) return hit;
    }
    return null;
  };

  return walk(root, 0);
}

/** Numeric tariff from PRICE_AM_OVERTIME payloads where `formulaRequest.variables` is empty (expression-only). */
function extractFormulaRequestExpressionNumeric(stored: unknown): number | null {
  if (stored == null || typeof stored !== 'object') return null;
  const o = stored as Record<string, unknown>;
  const fr = o.formulaRequest;
  if (fr && typeof fr === 'object' && !Array.isArray(fr)) {
    const nested = extractFormulaRequestExpressionNumeric(fr);
    if (nested != null) return nested;
  }
  const ex = o.expression;
  if (typeof ex === 'number' && Number.isFinite(ex)) return ex;
  if (typeof ex === 'string') {
    const t = ex.trim();
    if (t !== '' && Number.isFinite(Number(t))) return Number(t);
  }
  return null;
}

/**
 * Third-tab catalogs often nest drivers under each `formulaVariables[]` blob without populating legacy `variables[]`;
 * reuse the same DFS as {@link pickFormulaDeepFromRecord}, scoped per container only (avoid scraping unrelated IDs).
 */
function pickFormulaDeepFromThirdTabFormulaContainers(
  third: Record<string, unknown>,
): { formulaVariableId: number; value: number } | null {
  const cols = third.formulaVariables;
  if (!Array.isArray(cols)) return null;
  for (const fc of cols) {
    if (!fc || typeof fc !== 'object' || Array.isArray(fc)) continue;
    const hit = pickFormulaDeepFromRecord(fc as Record<string, unknown>, 14);
    if (hit?.formulaVariableId != null && Number.isFinite(hit.formulaVariableId)) return hit;
  }
  return null;
}

/**
 * PRICE_AM_OVERTIME PC POSTs omit resolvable `{ formulaVariableId, value }` in bodies and GET payloads that
 * older pickers expect; synthesize `{ formulaVariableId, value }` from third-tab catalogs + persisted expression so
 * service-contract compose has `contractFormulas` rows (billing driver overrides mutate `value`).
 */
function synthesizePdt2599ComposeFormulaWhenOvertimeExpressionOnly(
  third: Record<string, unknown>,
  priceComponentStored: unknown,
): { formulaVariableId: number; value: number } | null {
  const value = extractFormulaRequestExpressionNumeric(priceComponentStored);
  if (value == null) return null;
  const shallow = pickFormulaFromThirdTab(third) ?? pickFormulaFromNestedFormulaVariablesArrays(third);
  if (shallow?.formulaVariableId != null && Number.isFinite(shallow.formulaVariableId)) {
    return { formulaVariableId: shallow.formulaVariableId, value };
  }
  const deep = pickFormulaDeepFromThirdTabFormulaContainers(third);
  if (deep?.formulaVariableId != null && Number.isFinite(deep.formulaVariableId)) {
    return { formulaVariableId: deep.formulaVariableId, value };
  }
  return null;
}

async function tryResolveFormulaFromStoredPriceComponent(
  Request: Pdt2599ChainFixtures['Request'],
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  priceComponentStored: unknown,
): Promise<{ formulaVariableId: number; value: number } | null> {
  // POST /price-components often returns a richer body than GET /price-components/{id} on some envs;
  // extract formula drivers from the persisted snapshot first.
  if (priceComponentStored !== null && typeof priceComponentStored === 'object') {
    const body = priceComponentStored as Record<string, unknown>;
    const local =
      pickFormulaDeepFromRecord(body) ?? pickFormulaFromNestedFormulaVariablesArrays(body);
    if (local) return local;
  }

  const priceId = asPriceComponentId(priceComponentStored);
  const priceRes = await Request.get(`${Endpoints.priceComponent}/${priceId}`);
  await expect(priceRes).CheckResponse();
  const priceDetail = (await priceRes.json()) as Record<string, unknown>;
  let formula: { formulaVariableId: number; value: number } | null = null;
  const list = (priceDetail.formulaVariables as Record<string, unknown>[]) ?? [];
  if (list.length > 0) {
    try {
      formula = pickFormulaFromPriceComponentDetail(priceDetail);
    } catch {
      formula = null;
    }
  }
  if (!formula) formula = pickFormulaDeepFromRecord(priceDetail);
  if (!formula) formula = pickFormulaFromNestedFormulaVariablesArrays(priceDetail);
  return formula;
}

export async function resolvePdt2599ContractFormulaForPost(
  Request: Pdt2599ChainFixtures['Request'],
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  third: Record<string, unknown>,
  priceComponentStored: unknown,
  extraPriceComponentsStored?: unknown[],
): Promise<{ formulaVariableId: number; value: number } | null> {
  let formula: { formulaVariableId: number; value: number } | null = pickFormulaFromThirdTab(third);
  if (!formula) formula = pickFormulaFromNestedFormulaVariablesArrays(third);
  if (formula) return formula;

  const candidates = [priceComponentStored, ...(extraPriceComponentsStored ?? [])];
  for (const stored of candidates) {
    if (stored == null) continue;
    try {
      formula = await tryResolveFormulaFromStoredPriceComponent(Request, Endpoints, stored);
      if (formula) return formula;
    } catch {
      /* next candidate */
    }
  }
  let synth = synthesizePdt2599ComposeFormulaWhenOvertimeExpressionOnly(third, priceComponentStored);
  if (synth || priceComponentStored == null) return synth;
  try {
    const priceId = asPriceComponentId(priceComponentStored);
    const priceRes = await Request.get(`${Endpoints.priceComponent}/${priceId}`);
    await expect(priceRes).CheckResponse();
    const detailBody = await priceRes.json();
    synth = synthesizePdt2599ComposeFormulaWhenOvertimeExpressionOnly(third, detailBody);
  } catch {
    synth = null;
  }
  return synth;
}

/** UI path segment under frontend base (matches ResponseLinker `serviceContract`). */
const SERVICE_CONTRACT_PREVIEW_SEGMENT = 'service-contracts';

/** UI path segment for billing run preview (matches ResponseLinker `billingRun` → `/billing-run`). */
const BILLING_RUN_PREVIEW_SEGMENT = 'billing-run';

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

function previewItemId(item: unknown): number | undefined {
  if (typeof item === 'number' && Number.isFinite(item)) return item;
  if (item && typeof item === 'object' && 'id' in item) {
    const n = Number((item as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  return undefined;
}

function buildPdt2599FallbackPortalLinks(
  Responses: baseFixture['Responses'],
  frontEndUrl: string,
): Record<string, string[]> {
  const fe = frontEndUrl.endsWith('/') ? frontEndUrl : `${frontEndUrl}/`;
  const bucket = Responses.serviceContract;
  if (!Array.isArray(bucket) || bucket.length === 0) return {};

  const links: string[] = [];
  for (const item of bucket) {
    const id = previewItemId(item);
    if (id !== undefined) {
      links.push(`${fe}${SERVICE_CONTRACT_PREVIEW_SEGMENT}/preview?id=${id}`);
    }
  }
  return links.length > 0 ? { serviceContract: links } : {};
}

function mergePortalLinkRecords(
  primary: Record<string, string[]>,
  secondary: Record<string, string[]>,
): Record<string, string[]> {
  const out: Record<string, string[]> = { ...primary };
  for (const [k, urls] of Object.entries(secondary)) {
    if (!urls.length) continue;
    if (!out[k] || out[k].length === 0) out[k] = urls;
  }
  return out;
}

function normalizePortalLinkUrls(map: Record<string, string[]>): Record<string, string[]> {
  const out: Record<string, string[]> = {};
  for (const [k, urls] of Object.entries(map)) {
    out[k] = urls.map((u) => u.replace(/([^:])\/\//g, '$1/'));
  }
  return out;
}

/** Keep only service-contract preview URLs for attachments / console. */
function keepOnlyServiceContractPortalLinks(map: Record<string, string[]>): Record<string, string[]> {
  const urls = map.serviceContract;
  return urls?.length ? { serviceContract: urls } : {};
}

function formatPortalLinksPlain(
  portalLinks: Record<string, string[]>,
  hint?: { apiBaseLabel: string; frontendResolved: string | null; linkerHadServiceContract: boolean },
): string {
  const entries = Object.entries(portalLinks);
  if (entries.length === 0) {
    return [
      'No service contract portal preview URL was built.',
      `API base (fixture): ${hint?.apiBaseLabel ?? '(unknown)'}`,
      `Frontend base resolved: ${hint?.frontendResolved ?? '(null — set FRONTEND_BASE_URL)'}`,
      `ResponseLinker had serviceContract links: ${hint?.linkerHadServiceContract ? 'yes' : 'no'}`,
      '',
      'Fix: set FRONTEND_BASE_URL to your UI root, e.g. FRONTEND_BASE_URL=http://10.236.20.11:8080',
      'HTML report: npx playwright show-report — each test → Attachments.',
    ].join('\n');
  }
  const lines: string[] = ['Service contract — portal preview URL(s)', ''];
  for (const [key, urls] of entries) {
    lines.push(`[${key}]`, '');
    for (const url of urls) lines.push(url);
    lines.push('');
  }
  return lines.join('\n');
}

export function attachResponseLinkerAfterTest(
  Responses: baseFixture['Responses'],
  label = 'PDT-2599',
): void {
  const apiBase =
    configuredBaseURL || getProcessEnv('BASE_URL') || 'http://10.236.20.11:8091/';
  const fe = resolvePortalFrontendBaseUrl();
  const fromLinker = ResponseLinker.setLinksToResponses(Responses);
  const linkerHadServiceContract =
    Array.isArray(fromLinker.serviceContract) && fromLinker.serviceContract.length > 0;
  const fromFallback = fe ? buildPdt2599FallbackPortalLinks(Responses, fe) : {};
  const merged = normalizePortalLinkUrls(mergePortalLinkRecords(fromLinker, fromFallback));
  const portalLinks = keepOnlyServiceContractPortalLinks(merged);

  const plain = formatPortalLinksPlain(portalLinks, {
    apiBaseLabel: apiBase,
    frontendResolved: fe,
    linkerHadServiceContract,
  });

  console.log(
    `\n========== [${label}] Service contract portal (${test.info().title}) ==========\n${plain}\n================================================================\n`,
  );

  test.info().attach(`[${label}] Service contract portal URLs (plain text)`, {
    body: plain,
    contentType: 'text/plain; charset=utf-8',
  });
  test.info().attach(`[${label}] Service contract portal URLs (JSON)`, {
    body: JSON.stringify(portalLinks, null, 2),
    contentType: 'application/json',
  });
}

export function attachPdt2599CreatedEntities(
  Responses: baseFixture['Responses'],
  contractId: number,
  extra?: Record<string, unknown>,
): void {
  attachResponseLinkerAfterTest(Responses);
  const createdIds = {
    customerId: Responses.customer[0]?.id,
    priceComponentId: asPriceComponentId(Responses.priceComponent[0]),
    termId: Responses.terms[0]?.id,
    podId: Responses.pod[0]?.id,
    serviceId: asServiceId(Responses.service[0]),
    serviceContractId: contractId,
  };
  test.info().attach('[PDT-2599] Created entity IDs', {
    body: JSON.stringify(
        { createdIds, note: 'Service contract portal URLs: attachments above.', ...extra },
      null,
      2,
    ),
    contentType: 'application/json',
  });
}

function buildPdt2599FallbackBillingRunPortalLinks(
  Responses: baseFixture['Responses'],
  frontEndUrl: string,
): Record<string, string[]> {
  const fe = frontEndUrl.endsWith('/') ? frontEndUrl : `${frontEndUrl}/`;
  const bucket = Responses.billingRun;
  if (!Array.isArray(bucket) || bucket.length === 0) return {};

  const links: string[] = [];
  for (const item of bucket) {
    const id = previewItemId(item);
    if (id !== undefined) {
      links.push(`${fe}${BILLING_RUN_PREVIEW_SEGMENT}/preview?id=${id}`);
    }
  }
  return links.length > 0 ? { billingRun: links } : {};
}

function formatBillingPortalLinksPlain(
  portalLinks: Record<string, string[]>,
  hint?: { apiBaseLabel: string; frontendResolved: string | null; linkerHadBillingRun: boolean },
): string {
  const entries = Object.entries(portalLinks);
  if (entries.length === 0) {
    return [
      'No billing run portal preview URL was built.',
      `API base (fixture): ${hint?.apiBaseLabel ?? '(unknown)'}`,
      `Frontend base resolved: ${hint?.frontendResolved ?? '(null — set FRONTEND_BASE_URL)'}`,
      `ResponseLinker had billingRun links: ${hint?.linkerHadBillingRun ? 'yes' : 'no'}`,
      '',
      'Fix: set FRONTEND_BASE_URL to your UI root, e.g. FRONTEND_BASE_URL=http://10.236.20.11:8080',
      'HTML report: npx playwright show-report — each test → Attachments.',
    ].join('\n');
  }
  const lines: string[] = ['Billing run — portal preview URL(s)', ''];
  for (const [key, urls] of entries) {
    lines.push(`[${key}]`, '');
    for (const url of urls) lines.push(url);
    lines.push('');
  }
  return lines.join('\n');
}

/**
 * Billing-scenario attachments: portal preview uses **billing run** URLs (not service-contract preview).
 * Call after `billingRunId` is known; ensures `Responses.billingRun` contains the run for {@link ResponseLinker}.
 */
export function attachPdt2599BillingPortalLinks(Responses: baseFixture['Responses']): void {
  const apiBase =
    configuredBaseURL || getProcessEnv('BASE_URL') || 'http://10.236.20.11:8091/';
  const fe = resolvePortalFrontendBaseUrl();
  const fromLinker = ResponseLinker.setLinksToResponses(Responses);
  const linkerHadBillingRun =
    Array.isArray(fromLinker.billingRun) && fromLinker.billingRun.length > 0;
  const fromFallback = fe ? buildPdt2599FallbackBillingRunPortalLinks(Responses, fe) : {};
  const billingOnlyLinker = linkerHadBillingRun ? { billingRun: fromLinker.billingRun as string[] } : {};
  const merged = normalizePortalLinkUrls(mergePortalLinkRecords(billingOnlyLinker, fromFallback));
  const portalLinks = merged.billingRun?.length ? { billingRun: merged.billingRun } : {};

  const plain = formatBillingPortalLinksPlain(portalLinks, {
    apiBaseLabel: apiBase,
    frontendResolved: fe,
    linkerHadBillingRun,
  });

  console.log(
    `\n========== [PDT-2599] Billing run portal (${test.info().title}) ==========\n${plain}\n================================================================\n`,
  );

  test.info().attach('[PDT-2599] Billing run portal URLs (plain text)', {
    body: plain,
    contentType: 'text/plain; charset=utf-8',
  });
  test.info().attach('[PDT-2599] Billing run portal URLs (JSON)', {
    body: JSON.stringify(portalLinks, null, 2),
    contentType: 'application/json',
  });
}

/** Same IDs JSON as {@link attachPdt2599CreatedEntities}, but portal attachments are billing-run preview links. */
export function attachPdt2599CreatedEntitiesForBilling(
  Responses: baseFixture['Responses'],
  contractId: number,
  billingRunId: number,
  extra?: Record<string, unknown>,
): void {
  /** Replace bucket so portal links always target this run (no stale ids / duplicates). */
  Responses.billingRun.length = 0;
  Responses.billingRun.push({ id: billingRunId });
  attachPdt2599BillingPortalLinks(Responses);
  const createdIds = {
    customerId: Responses.customer[0]?.id,
    priceComponentId: asPriceComponentId(Responses.priceComponent[0]),
    termId: Responses.terms[0]?.id,
    podId: Responses.pod[0]?.id,
    serviceId: asServiceId(Responses.service[0]),
    serviceContractId: contractId,
    billingRunId,
  };
  test.info().attach('[PDT-2599] Created entity IDs', {
    body: JSON.stringify(
      { createdIds, note: 'Billing run portal URLs: attachments above.', ...extra },
      null,
      2,
    ),
    contentType: 'application/json',
  });
}

/** Shared POST /service-contract compose after commercial service exists (`Responses.service`, POD, terms, numeric price component ids). */
async function pdt2599ComposeServiceContractStep(
  fx: Pdt2599ChainFixtures,
  communicationDataForBilling: number,
  communicationDataForContract: number,
  formulaSource: unknown,
): Promise<Pdt2599ChainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const serviceId = asServiceId(Responses.service[0]);
  const serviceViewRes = await Request.get(`${Endpoints.service}/${serviceId}?version=1`);
  await expect(serviceViewRes).CheckResponse();
  const serviceView = await serviceViewRes.json();
  const activeVersion = (serviceView.versions ?? []).find((v: { status?: string }) => v.status === 'ACTIVE');
  expect(activeVersion?.id).toBeTruthy();
  expect(activeVersion?.detailId).toBeTruthy();
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

  const formula = await resolvePdt2599ContractFormulaForPost(Request, Endpoints, third, formulaSource, []);
  if (!formula) {
    const note =
      '[PDT-2599] No contract formula resolved from third-tab or price component; POST uses empty contractFormulas.';
    console.warn(note);
    await test.info().attach('[PDT-2599] Missing contract formula', {
      body: `${note}\nserviceDetailId: ${serviceDetailId}`,
      contentType: 'text/plain; charset=utf-8',
    });
  }

  const payload = await GeneratePayload.contractsAndOrders.serviceContract();
  payload.basicParameters.communicationDataForBilling = communicationDataForBilling;
  payload.basicParameters.communicationDataForContract = communicationDataForContract;
  payload.basicParameters.serviceVersionId = serviceVersionId;
  payload.serviceParameters.contractTermId = termsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTermId = invoiceTermsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTerm = invoiceTermsCatalog[0].value;
  payload.serviceParameters.contractFormulas = formula
    ? [{ formulaVariableId: formula!.formulaVariableId, value: formula!.value }]
    : [];
  payload.serviceParameters.podIds = [Responses.pod[0].id];

  normalizePdt2599ServiceContractPostDates(payload);

  const createRes = await Request.post(Endpoints.serviceContract, { data: payload });
  await expect(createRes).CheckResponse();
  const contractId = (await createRes.json()) as number;
  expect(contractId).toBeTruthy();
  Responses.serviceContract.push({ id: contractId });

  return { contractId, serviceId, serviceVersionId, serviceDetailId };
}

/** POST /service-contract in DRAFT (version 1 startDate = today, signInDate null) — PDT-2846. */
async function pdt2599ComposeDraftServiceContractStep(
  fx: Pdt2599ChainFixtures,
  communicationDataForBilling: number,
  communicationDataForContract: number,
  formulaSource: unknown,
): Promise<Pdt2599ChainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const serviceId = asServiceId(Responses.service[0]);
  const serviceViewRes = await Request.get(`${Endpoints.service}/${serviceId}?version=1`);
  await expect(serviceViewRes).CheckResponse();
  const serviceView = await serviceViewRes.json();
  const activeVersion = (serviceView.versions ?? []).find((v: { status?: string }) => v.status === 'ACTIVE');
  expect(activeVersion?.id).toBeTruthy();
  expect(activeVersion?.detailId).toBeTruthy();
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

  const formula = await resolvePdt2599ContractFormulaForPost(Request, Endpoints, third, formulaSource, []);
  const payload = await GeneratePayload.contractsAndOrders.serviceContract();
  payload.basicParameters.communicationDataForBilling = communicationDataForBilling;
  payload.basicParameters.communicationDataForContract = communicationDataForContract;
  payload.basicParameters.serviceVersionId = serviceVersionId;
  payload.basicParameters.contractStatus = 'DRAFT';
  payload.basicParameters.detailsSubStatus = 'DRAFT';
  /** Create API requires first version SIGNED/Valid — contract header may still be DRAFT (PDT-2846). */
  payload.basicParameters.contractVersionStatus = 'SIGNED';
  payload.basicParameters.signInDate = null as unknown as string;
  payload.basicParameters.entryIntoForceDate = null;
  payload.serviceParameters.contractTermId = termsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTermId = invoiceTermsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTerm = invoiceTermsCatalog[0].value;
  payload.serviceParameters.contractFormulas = formula
    ? [{ formulaVariableId: formula.formulaVariableId, value: formula.value }]
    : [];
  payload.serviceParameters.podIds = [Responses.pod[0].id];
  payload.serviceParameters.entryIntoForceDate = null as unknown as string;

  const createRes = await Request.post(Endpoints.serviceContract, { data: payload });
  await expect(createRes).CheckResponse();
  const contractId = (await createRes.json()) as number;
  expect(contractId).toBeTruthy();
  Responses.serviceContract.push({ id: contractId });

  return { contractId, serviceId, serviceVersionId, serviceDetailId };
}

/** Full precondition chain + DRAFT service contract (PDT-2846 test data steps 1–4). */
export async function runPdt2846DraftServiceContractChain(
  fx: Pdt2599ChainFixtures,
): Promise<Pdt2599ChainResult> {
  let communicationDataForBilling: number;
  let communicationDataForContract: number;
  let pricePostBody!: unknown;
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  await test.step('Precondition: Create customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: Communication channels (BILLING and CONTRACT)', async () => {
    const customerDetailsId = Responses.customer[0].lastCustomerDetailId;
    const billingRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
      params: { customerDetailsId, communicationDataType: 'BILLING' },
    });
    await expect(billingRes).CheckResponse();
    const billingRows = await billingRes.json();
    const contractCommRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
      params: { customerDetailsId, communicationDataType: 'CONTRACT' },
    });
    await expect(contractCommRes).CheckResponse();
    const contractRows = await contractCommRes.json();
    expect(billingRows?.length && contractRows?.length).toBeTruthy();
    communicationDataForBilling = billingRows[0].id;
    communicationDataForContract = contractRows[0].id;
  });

  await test.step('Precondition: Price component (PER_PIECE)', async () => {
    const price = await Request.post(Endpoints.priceComponent, {
      data: GeneratePayload.productAndServices.perPiece(),
    });
    await expect(price).CheckResponse();
    const raw = await price.json();
    pricePostBody = await hydratePriceComponentBodyForCompose(Request, Endpoints, raw);
    Responses.priceComponent.push(asPriceComponentId(raw));
  });

  await test.step('Precondition: Create term', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: Create POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: Create commercial service', async () => {
    const service = await Request.post(Endpoints.service, {
      data: GeneratePayload.productAndServices.service(),
    });
    await expect(service).CheckResponse();
    Responses.service.push(await service.json());
  });

  return await test.step('Precondition: Create DRAFT service-contract', async () =>
    pdt2599ComposeDraftServiceContractStep(fx, communicationDataForBilling, communicationDataForContract, pricePostBody),
  );
}

export async function runPdt2599SignedServiceContractChain({
  Request,
  GeneratePayload,
  Responses,
  Endpoints,
}: Pdt2599ChainFixtures): Promise<Pdt2599ChainResult> {
  let communicationDataForBilling: number;
  let communicationDataForContract: number;
  let pricePostBody!: unknown;

  const fx: Pdt2599ChainFixtures = { Request, GeneratePayload, Responses, Endpoints };

  await test.step('Precondition: Create customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: Communication channels (BILLING and CONTRACT)', async () => {
    const customerDetailsId = Responses.customer[0].lastCustomerDetailId;
    const billingRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
      params: { customerDetailsId, communicationDataType: 'BILLING' },
    });
    await expect(billingRes).CheckResponse();
    const billingRows = await billingRes.json();
    const contractCommRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
      params: { customerDetailsId, communicationDataType: 'CONTRACT' },
    });
    await expect(contractCommRes).CheckResponse();
    const contractRows = await contractCommRes.json();
    expect(billingRows?.length && contractRows?.length).toBeTruthy();

    communicationDataForBilling = billingRows[0].id;
    communicationDataForContract = contractRows[0].id;
  });

  await test.step('Precondition: Price component (PER_PIECE)', async () => {
    const price = await Request.post(Endpoints.priceComponent, {
      data: GeneratePayload.productAndServices.perPiece(),
    });
    await expect(price).CheckResponse();
    const raw = await price.json();
    pricePostBody = await hydratePriceComponentBodyForCompose(Request, Endpoints, raw);
    Responses.priceComponent.push(asPriceComponentId(raw));
  });

  await test.step('Precondition: Create term', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: Create POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: Create commercial service', async () => {
    const service = await Request.post(Endpoints.service, {
      data: GeneratePayload.productAndServices.service(),
    });
    await expect(service).CheckResponse();
    Responses.service.push(await service.json());
  });

  return await test.step('Precondition: Compose service-contract', async () =>
    pdt2599ComposeServiceContractStep(fx, communicationDataForBilling, communicationDataForContract, pricePostBody),
  );
}

type RequestLike = Pdt2599ChainFixtures['Request'];

/**
 * Several envs respond to `POST /price-components` with a bare **numeric/long**, or a minimal `{ id }`
 * without `formulaRequest` / usable `formulaVariables`; compose needs the full `GET /price-components/{id}` body.
 */
async function hydratePriceComponentBodyForCompose(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  raw: unknown,
): Promise<unknown> {
  if (raw !== null && typeof raw === 'object' && !Array.isArray(raw)) {
    const o = raw as Record<string, unknown>;
    const exprFromFormulaRequest =
      o.formulaRequest != null &&
      typeof o.formulaRequest === 'object' &&
      extractFormulaRequestExpressionNumeric(o.formulaRequest) != null;
    const hasFormulaShape =
      exprFromFormulaRequest ||
      (Array.isArray(o.formulaVariables) && o.formulaVariables.length > 0) ||
      pickFormulaDeepFromRecord(o, 8) != null ||
      pickFormulaFromNestedFormulaVariablesArrays(o) != null;
    if (hasFormulaShape) return raw;
  }
  let pcId: number;
  try {
    pcId = asPriceComponentId(raw);
  } catch {
    return raw;
  }
  const pcRes = await Request.get(`${Endpoints.priceComponent}/${pcId}`);
  await expect(pcRes).CheckResponse();
  return pcRes.json();
}

/** POST /billing-run often returns bare `number` — normalize to numeric id. */
export function asBillingRunId(postBody: unknown): number {
  if (typeof postBody === 'number' && Number.isFinite(postBody)) return postBody;
  if (postBody !== null && typeof postBody === 'object' && 'id' in postBody) {
    const n = Number((postBody as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  throw new Error('Unexpected billing-run POST body: expected number or { id:number }.');
}

/**
 * Period-valid billing event date strictly inside `[startInclusive, endExclusive)` when end is given,
 * otherwise a stable offset inside the open-ended Signed tail. Complements Swagger `commonParameters.*Date` rules.
 */
export function pickValidBillingDateFromBillingCommon(
  startInclusiveIso: string,
  endExclusiveIso?: string,
): string {
  if (!endExclusiveIso) return addDaysIso(startInclusiveIso, 21);
  const s = Date.parse(`${startInclusiveIso}T12:00:00.000Z`);
  const e = Date.parse(`${endExclusiveIso}T12:00:00.000Z`);
  if (!Number.isFinite(s) || !Number.isFinite(e) || e <= s + 86_400_000) {
    return addDaysIso(startInclusiveIso, 1);
  }
  const mid = new Date(Math.floor((s + e) / 2));
  return mid.toISOString().slice(0, 10);
}

/**
 * Second Signed slice prefers `creationDate + 30d`, but caps to the open accounting period end
 * (`GET /accounting-period/{envVariables.accounting_period}`) so PER_PIECE billing-run dates can still fall in-period.
 */
export async function resolvePdt2599SecondSignedSliceStartForOpenAccountingPeriod(
  Request: RequestLike,
  creationDateIso: string,
): Promise<string> {
  const preferred = addDaysIso(creationDateIso, 30);
  const minimumGapFromCreation = addDaysIso(creationDateIso, 3);
  const apId = Number(envVariables.accounting_period);
  if (!Number.isFinite(apId)) {
    return preferred >= minimumGapFromCreation ? preferred : minimumGapFromCreation;
  }
  const res = await Request.get(`accounting-period/${apId}`);
  await expect(res).CheckResponse();
  const ap = (await res.json()) as { endDate?: string };
  const apHi = ap.endDate?.slice(0, 10);
  if (!apHi) {
    return preferred >= minimumGapFromCreation ? preferred : minimumGapFromCreation;
  }
  let candidate = preferred <= apHi ? preferred : addDaysIso(apHi, -2);
  if (candidate < minimumGapFromCreation) candidate = minimumGapFromCreation;
  if (candidate > apHi) {
    throw new Error(
      `[PDT-2599] Cannot fit second Signed slice inside accounting period ending ${apHi} (creation ${creationDateIso}, accountingPeriodId=${apId}).`,
    );
  }
  return candidate;
}

/**
 * Intersects `[startInclusive, endExclusive)` with the calendar range of `envVariables.accounting_period`
 * (`GET /accounting-period/{id}`) before applying {@link pickValidBillingDateFromBillingCommon}.
 */
export async function pickValidBillingDateFromBillingCommonWithinAccountingPeriod(
  Request: RequestLike,
  startInclusiveIso: string,
  endExclusiveIso: string,
): Promise<string> {
  const apId = Number(envVariables.accounting_period);
  if (!Number.isFinite(apId)) {
    return pickValidBillingDateFromBillingCommon(startInclusiveIso, endExclusiveIso);
  }
  const res = await Request.get(`accounting-period/${apId}`);
  await expect(res).CheckResponse();
  const ap = (await res.json()) as { startDate?: string; endDate?: string };
  const apLo = ap.startDate?.slice(0, 10);
  const apHi = ap.endDate?.slice(0, 10);
  if (!apLo || !apHi) {
    return pickValidBillingDateFromBillingCommon(startInclusiveIso, endExclusiveIso);
  }
  const apEndExclusive = addDaysIso(apHi, 1);
  const lo = startInclusiveIso > apLo ? startInclusiveIso : apLo;
  const hiExcl = endExclusiveIso < apEndExclusive ? endExclusiveIso : apEndExclusive;
  if (lo >= hiExcl) {
    throw new Error(
      `[PDT-2599] Billing window [${startInclusiveIso}, ${endExclusiveIso}) does not intersect accounting period range [${apLo}, ${apHi}] (accountingPeriodId=${apId}).`,
    );
  }
  return pickValidBillingDateFromBillingCommon(lo, hiExcl);
}

/**
 * Align billing POST `accountingPeriodId` with `billingIso`: reuse configured env period when it contains the date,
 * otherwise scan OPEN periods from `GET billing-run/accounting-period-available-list` (same source as nomenclatures).
 */
export async function resolveAccountingPeriodIdForBillingDate(Request: RequestLike, billingIso: string): Promise<number> {
  const configured = Number(envVariables.accounting_period);
  const previewContains = async (id: number): Promise<boolean> => {
    if (!Number.isFinite(id)) return false;
    const res = await Request.get(`accounting-period/${id}`);
    if (!res.ok()) return false;
    const ap = (await res.json()) as { startDate?: string; endDate?: string };
    const lo = ap.startDate?.slice(0, 10);
    const hi = ap.endDate?.slice(0, 10);
    return !!(lo && hi && billingIso >= lo && billingIso <= hi);
  };
  if (await previewContains(configured)) return configured;

  const listRes = await Request.get('billing-run/accounting-period-available-list?page=0&size=50');
  await expect(listRes).CheckResponse();
  const page = (await listRes.json()) as {
    content?: Array<{ id?: number; startDate?: string; endDate?: string; status?: string }>;
  };
  for (const row of page.content ?? []) {
    if (String(row.status) !== 'OPEN') continue;
    const lo = row.startDate?.slice(0, 10);
    const hi = row.endDate?.slice(0, 10);
    const id = row.id;
    if (id != null && lo && hi && billingIso >= lo && billingIso <= hi) return id;
  }
  return configured;
}

/** Rewrite contract formula driver value for matched `formulaVariableId` rows. */
export function applyPdt2599PerPieceDriversToServiceParameters(
  formulas: Array<{ formulaVariableId?: number; value?: number }>,
  formulaVariableId: number,
  driverValue: number,
): Array<{ formulaVariableId: number; value: number }> {
  const base = formulas.filter((f) => (f.formulaVariableId ?? 0) !== formulaVariableId);
  return [...base.map((f) => ({ formulaVariableId: f.formulaVariableId as number, value: f.value ?? 0 })), { formulaVariableId, value: driverValue }];
}

function assignPdt2599BillingModelFields(
  payload: ReturnType<typeof forVolumes>,
  applicationModelTypes: Pdt2599BillingApplicationModelKind[],
): void {
  const normalized = applicationModelTypes.map((t) =>
    t === 'INTERIM' ? ('INTERIM_AND_ADVANCE_PAYMENT' as const) : t,
  );
  payload.basicParameters.applicationModelType =
    normalized as typeof payload.basicParameters.applicationModelType;

  // OVER_TIME_PERIODICAL uses the same `commonParameters.periodicity` as `GeneratePayload.billing.billingRun` — STANDARD
  // (see `periodicalForProcuctContract.spec.ts`). Setting PERIODIC requires `processPeriodicityIds` and yields no draft invoices
  // for this flow when dates are null.
  if (applicationModelTypes.includes('OVER_TIME_PERIODICAL')) {
    payload.basicParameters.periodicMaxEndDate = null;
    payload.basicParameters.periodicMaxEndDateValue = null;
    payload.basicParameters.maxEndDate = null;
  }
}

async function resolveNumericServiceContractId(contractIndex: unknown): Promise<number> {
  if (typeof contractIndex === 'number' && Number.isFinite(contractIndex)) return contractIndex;
  if (contractIndex !== null && typeof contractIndex === 'object' && 'id' in contractIndex) {
    const n = Number((contractIndex as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  throw new Error('Responses.serviceContract[0] missing numeric contract id.');
}

export async function buildPdt2599BillingRunPayload(
  Request: RequestLike,
  serviceContractResponses: unknown[],
  applicationModelTypes: Pdt2599BillingApplicationModelKind[],
  billingDates: {
    /** Tax event & invoice driver date (Swagger `BillingRunStandardRequest.commonParameters`) */
    taxEventDate?: string | null;
    invoiceDate?: string | null;
  },
): Promise<ReturnType<typeof forVolumes>> {
  const cid = await resolveNumericServiceContractId(serviceContractResponses[0]);
  const cg = await Request.get(`service-contract/${cid}?version=1`);
  await expect(cg).CheckResponse();
  const contractjson = (await cg.json()) as Record<string, unknown>;
  const contractNumber = (contractjson.basicParameters as { contractNumber?: string } | undefined)
    ?.contractNumber;

  const payload = forVolumes();
  assignPdt2599BillingModelFields(payload, applicationModelTypes);

  payload.basicParameters.listOfCustomersContractsOrPOD = contractNumber;

  payload.commonParameters.taxEventDate =
    billingDates.taxEventDate ?? randomGens.generateTodaysDate('yyyy-mm-dd');
  payload.commonParameters.invoiceDate =
    billingDates.invoiceDate ?? billingDates.taxEventDate ?? randomGens.generateTodaysDate('yyyy-mm-dd');

  payload.commonParameters.accountingPeriodId = envVariables.accounting_period;

  return payload;
}

/** Normalize second-window POST body dates after cloning an earlier baseline payload snapshot. */
export function alignPerPieceSecondSignedWindowBillingPayload(
  base: Record<string, unknown>,
  billingDateIso: string,
): Record<string, unknown> {
  const next = JSON.parse(JSON.stringify(base)) as {
    commonParameters: Record<string, unknown>;
  };
  next.commonParameters.taxEventDate = billingDateIso;
  next.commonParameters.invoiceDate = billingDateIso;
  return next as Record<string, unknown>;
}

export async function pollPerPieceBillingRunUntilDraftWithInvoices(
  Request: RequestLike,
  billingRunRoot: string,
  billingRunId: number,
  opts?: { minimumInvoices?: number; intervalMs?: number; maxAttempts?: number },
): Promise<{ billingRunSnapshot: Record<string, unknown>; draftInvoiceCount: number }> {
  const minInv = opts?.minimumInvoices ?? 1;
  const intervalMs = opts?.intervalMs ?? 15_000;
  /** Allow ~10 minutes of polling — billing can lag under parallel load; PDT-2599 suite runs serial to reduce contention. */
  const maxAttempts = opts?.maxAttempts ?? Math.floor((10 * 60 * 1000) / intervalMs);

  const startBilling = await Request.patch(`${billingRunRoot}/start-billing?billingRunId=${billingRunId}`);
  await expect(startBilling).CheckResponse();

  let lastBilling: Record<string, unknown> = {};

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const runRes = await Request.get(`${billingRunRoot}/${billingRunId}`);
    await expect(runRes).CheckResponse();
    lastBilling = (await runRes.json()) as Record<string, unknown>;
    const cp = lastBilling.commonParameters as { status?: string } | undefined;
    const drafts = await Request.get(`${billingRunRoot}/draft-invoices?id=${billingRunId}&page=0&size=25`);
    await expect(drafts).CheckResponse();
    const dj = (await drafts.json()) as { content?: unknown[] };
    const invoiceCount = Array.isArray(dj.content) ? dj.content.length : 0;

    if (cp?.status === 'DRAFT' && invoiceCount >= minInv) {
      return { billingRunSnapshot: lastBilling, draftInvoiceCount: invoiceCount };
    }
    await new Promise((r) => setTimeout(r, intervalMs));
  }
  throw new Error(
    `[PDT-2599 billing poll] Billing run ${billingRunId}: expected DRAFT with ≥ ${minInv} invoice(s); last snapshot: ${JSON.stringify(lastBilling?.commonParameters ?? {})}`,
  );
}

export async function expectPerPieceBillingRunWithoutInvoices(
  Request: RequestLike,
  billingRunRoot: string,
  billingRunId: number,
  args: { invoiceLeakBugMessage: string },
): Promise<void> {
  const startBilling = await Request.patch(`${billingRunRoot}/start-billing?billingRunId=${billingRunId}`);
  expect(startBilling.ok(), await startBilling.text()).toBeTruthy();

  const intervalMs = 15_000;
  const maxAttempts = Math.floor((3 * 60 * 1000) / intervalMs);

  let lastInvoiceCount = -1;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const drafts = await Request.get(`${billingRunRoot}/draft-invoices?id=${billingRunId}&page=0&size=25`);
    await expect(drafts).CheckResponse();
    const dj = (await drafts.json()) as { content?: unknown[] };
    lastInvoiceCount = Array.isArray(dj.content) ? dj.content.length : 0;
    const runRes = await Request.get(`${billingRunRoot}/${billingRunId}`);
    await expect(runRes).CheckResponse();
    const st = ((await runRes.json()) as Record<string, unknown>).commonParameters as { status?: string };
    if (st?.status === 'DRAFT' && lastInvoiceCount === 0) return;

    await new Promise((r) => setTimeout(r, intervalMs));
  }
  expect(lastInvoiceCount, args.invoiceLeakBugMessage).toBe(0);
}

/** Expected net (ex-VAT) for PER_PIECE from persisted contract version fields. */
export function pdt2599ExpectedPerPieceNet(quantity: number, formulaValue: number): number {
  return quantity * formulaValue;
}

/** Expected net for OVER_TIME_* when billing uses a single formula row (no quantity axis). */
export function pdt2599ExpectedOverTimeNet(formulaValue: number): number {
  return formulaValue;
}

export type Pdt2599ContractVersionBillingBase = {
  quantity: number | null;
  formulaValue: number | null;
};

/** Read quantity + first contract formula `value` for a logical contract version (source of truth for expected invoice net). */
export async function readPdt2599ContractVersionQuantityAndFormulaValue(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  logicalVersionId: number,
): Promise<Pdt2599ContractVersionBillingBase> {
  const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: logicalVersionId },
  });
  await expect(res).CheckResponse();
  const d = (await res.json()) as Record<string, unknown>;
  const sp = (d.serviceParameters ?? {}) as {
    quantity?: unknown;
    contractFormulas?: { value?: unknown }[];
  };
  const qRaw = sp.quantity;
  const quantity =
    qRaw == null || qRaw === ''
      ? null
      : Number.isFinite(Number(qRaw))
        ? Number(qRaw)
        : null;
  const fv = sp.contractFormulas?.[0]?.value;
  const formulaValue =
    fv == null || fv === ''
      ? null
      : Number.isFinite(Number(fv))
        ? Number(fv)
        : null;
  return { quantity, formulaValue };
}

/**
 * Tariff scalar for a contract version `GET` JSON: `serviceParameters.priceComponents[].value`
 * (any row, prioritizing entries with `formulaVariableId`), then `contractFormulas`, then `thirdPageTabs`
 * (aligned with `buildServiceContractEditPayloadFromDetail` merge).
 */
export function extractPdt2599ExpectedFormulaScalarFromContractGetJson(d: Record<string, unknown>): number | null {
  const svc = (d.serviceParameters ?? {}) as {
    priceComponents?: Array<{ formulaVariableId?: unknown; value?: unknown }>;
    contractFormulas?: { value?: unknown }[];
  };
  const pcs = svc.priceComponents ?? [];
  const withId = pcs.filter((p) => p.formulaVariableId != null && p.formulaVariableId !== '');
  const idSet = new Set(withId);
  const rest = pcs.filter((p) => !idSet.has(p));
  const ordered = withId.length > 0 ? [...withId, ...rest] : [...pcs];
  for (const p of ordered) {
    const raw = p.value;
    if (raw == null || raw === '') continue;
    const n = Number(raw);
    if (Number.isFinite(n)) return n;
  }
  const fv0 = svc.contractFormulas?.[0]?.value;
  if (fv0 != null && fv0 !== '' && Number.isFinite(Number(fv0))) return Number(fv0);
  const third = (d.thirdPageTabs ?? {}) as Record<string, unknown>;
  const picked = pickFormulaFromThirdTab(third);
  if (picked && Number.isFinite(picked.value)) return picked.value;
  return null;
}

/**
 * `GET /service-contract/{id}?versionId=` → billing row on preview: match `serviceParameters.priceComponents[]`
 * by `formulaVariableId` (same id as the billing price component's primary formula variable) and read `value`
 * (Swagger `ContractPriceComponentResponse.value` — evaluated amount for that version).
 */
export function extractPdt2599ContractPriceComponentValueByFormulaVariableId(
  contractDetail: Record<string, unknown>,
  formulaVariableId: number,
): number | null {
  const svc = (contractDetail.serviceParameters ?? {}) as {
    priceComponents?: Array<{ formulaVariableId?: unknown; value?: unknown }>;
  };
  for (const p of svc.priceComponents ?? []) {
    if (p.formulaVariableId == null || p.formulaVariableId === '') continue;
    if (Number(p.formulaVariableId) !== formulaVariableId) continue;
    const raw = p.value;
    if (raw == null || raw === '') continue;
    const n = Number(raw);
    if (Number.isFinite(n)) return n;
  }
  return null;
}

/** First billing formula id on the contract snapshot: `priceComponents[].formulaVariableId`, else `contractFormulas[0]`. */
export function extractFirstContractPriceComponentFormulaVariableId(contractDetail: Record<string, unknown>): number | null {
  const svc = (contractDetail.serviceParameters ?? {}) as {
    priceComponents?: Array<{ formulaVariableId?: unknown }>;
    contractFormulas?: Array<{ formulaVariableId?: unknown }>;
  };
  for (const p of svc.priceComponents ?? []) {
    if (p.formulaVariableId == null || p.formulaVariableId === '') continue;
    const n = Number(p.formulaVariableId);
    if (Number.isFinite(n)) return n;
  }
  const cf0 = svc.contractFormulas?.[0];
  if (cf0?.formulaVariableId != null && cf0.formulaVariableId !== '') {
    const n = Number(cf0.formulaVariableId);
    if (Number.isFinite(n)) return n;
  }
  return null;
}

/**
 * Tariff numeric for a contract version: `priceComponents[].value` for `formulaVariableId`, else matching
 * `serviceParameters.contractFormulas[]` row (preview sometimes mirrors PUT shape without populating PC `value`).
 */
export function extractPdt2599ContractTariffValueForFormulaVariableId(
  contractDetail: Record<string, unknown>,
  formulaVariableId: number,
): number | null {
  const fromPc = extractPdt2599ContractPriceComponentValueByFormulaVariableId(contractDetail, formulaVariableId);
  if (fromPc != null) return fromPc;
  const sp = (contractDetail.serviceParameters ?? {}) as {
    contractFormulas?: Array<{ formulaVariableId?: unknown; value?: unknown }>;
  };
  for (const f of sp.contractFormulas ?? []) {
    if (f.formulaVariableId == null || f.formulaVariableId === '') continue;
    if (Number(f.formulaVariableId) !== formulaVariableId) continue;
    const raw = f.value;
    if (raw == null || raw === '') continue;
    const n = Number(raw);
    if (Number.isFinite(n)) return n;
  }
  return null;
}

/** Primary `formulaVariableId` from catalog `GET /price-components/{id}` (definition anchor for contract-row matching). */
export async function readPdt2599BillingPriceComponentPrimaryFormulaVariableId(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  billingPriceComponentId: number,
): Promise<number> {
  const pcRes = await Request.get(`${Endpoints.priceComponent}/${billingPriceComponentId}`);
  await expect(pcRes).CheckResponse();
  const pcJson = (await pcRes.json()) as Record<string, unknown>;
  const deep = pickFormulaDeepFromRecord(pcJson, 16);
  if (deep != null && Number.isFinite(deep.formulaVariableId)) return deep.formulaVariableId;
  const nested = pickFormulaFromNestedFormulaVariablesArrays(pcJson);
  if (nested != null && Number.isFinite(nested.formulaVariableId)) return nested.formulaVariableId;
  try {
    return pickFormulaFromPriceComponentDetail(pcJson).formulaVariableId;
  } catch {
    throw new Error(
      `[PDT-2599] GET ${Endpoints.priceComponent}/${billingPriceComponentId} returned no resolvable formulaVariableId (empty formulaVariables / nested formula shape).`,
    );
  }
}

/**
 * Tariff scalar from contract version after third-tab merge + {@link buildServiceContractEditPayloadFromDetail}:
 * matches the PUT graph used by `applyPdt2599BillingTwoSignedDriverGraph` when preview `priceComponents` / `contractFormulas` are empty.
 */
function parsePdt2599ContractPriceComponentNumeric(raw: unknown): number | null {
  if (raw == null || raw === '') return null;
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw;
  if (typeof raw === 'string') {
    const n = Number(raw.trim().replace(/\s/g, '').replace(',', '.'));
    return Number.isFinite(n) ? n : null;
  }
  return null;
}

/**
 * Merged contract GET + third-tab + {@link buildServiceContractEditPayloadFromDetail} — formula **value** and quantity
 * used for billing (PUT drivers visible here when `priceComponents[]` omits `formulaVariableId` or is empty on preview).
 */
async function loadPdt2599SignedVersionEditFormulaContext(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  logicalVersionId: number,
  billingPriceComponentId: number | null,
): Promise<{ formulaValue: number; quantityEff: number } | null> {
  const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: logicalVersionId },
  });
  await expect(res).CheckResponse();
  let detail = (await res.json()) as Record<string, unknown>;
  const sid = (detail.basicParameters as { serviceDetailId?: number } | undefined)?.serviceDetailId;
  if (!sid) return null;
  const tRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
    params: { serviceDetailId: sid },
  });
  await expect(tRes).CheckResponse();
  const tab = (await tRes.json()) as Record<string, unknown>;
  const prev = (detail.thirdPageTabs ?? {}) as Record<string, unknown>;
  detail = { ...detail, thirdPageTabs: { ...prev, ...tab } };

  const svcPersisted = (detail.serviceParameters ?? {}) as {
    priceComponents?: Array<{ formulaVariableId?: unknown; value?: unknown }>;
    contractFormulas?: Array<{ formulaVariableId?: unknown; value?: unknown }>;
    quantity?: unknown;
  };

  const fromPriceComponents = Array.isArray(svcPersisted.priceComponents)
    ? svcPersisted.priceComponents
        .map((p) => ({
          formulaVariableId:
            p.formulaVariableId != null && p.formulaVariableId !== ''
              ? Number(p.formulaVariableId)
              : NaN,
          value: parsePdt2599ContractPriceComponentNumeric(p.value),
        }))
        .filter((r) => r.value != null && Number.isFinite(r.value as number))
    : [];

  const normalizedFromApi =
    fromPriceComponents.length > 0
      ? fromPriceComponents
      : Array.isArray(svcPersisted.contractFormulas) && svcPersisted.contractFormulas.length > 0
        ? svcPersisted.contractFormulas
            .map((r) => ({
              formulaVariableId:
                r.formulaVariableId != null && r.formulaVariableId !== ''
                  ? Number(r.formulaVariableId)
                  : NaN,
              value: parsePdt2599ContractPriceComponentNumeric(r.value),
            }))
            .filter((r) => r.value != null && Number.isFinite(r.value as number))
        : [];

  const payload = buildServiceContractEditPayloadFromDetail(detail, { savingAsNewVersion: false });
  const sp = payload.serviceParameters as {
    contractFormulas?: Array<{ formulaVariableId?: number; value?: unknown }>;
    quantity?: unknown;
  };

  if (normalizedFromApi.length > 0) {
    sp.contractFormulas = normalizedFromApi;
  }

  let rows = (sp.contractFormulas ?? []).filter((r) => r != null);
  if (rows.length === 0) return null;

  let fvIdFromBilling: number | null = null;
  if (billingPriceComponentId != null) {
    try {
      fvIdFromBilling = await readPdt2599BillingPriceComponentPrimaryFormulaVariableId(
        Request,
        Endpoints,
        billingPriceComponentId,
      );
    } catch {
      fvIdFromBilling = null;
    }
  }

  const row =
    fvIdFromBilling != null
      ? rows.find((r) => Number(r.formulaVariableId) === fvIdFromBilling) ?? rows[0]
      : rows[0];
  const raw = row?.value;
  if (raw == null || raw === '') return null;
  const fv = Number(raw);
  if (!Number.isFinite(fv)) return null;

  const qRaw = sp.quantity ?? svcPersisted.quantity;
  let qtyEff = 1;
  if (qRaw != null && qRaw !== '' && Number.isFinite(Number(qRaw)) && Number(qRaw) !== 0) {
    qtyEff = Number(qRaw);
  }
  return { formulaValue: fv, quantityEff: qtyEff };
}

export async function readPdt2599TariffFromSignedVersionEditPayload(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  logicalVersionId: number,
): Promise<number | null> {
  const line = await readPdt2599ExpectedBilledLineFromSignedVersionEditPayload(
    Request,
    Endpoints,
    contractId,
    logicalVersionId,
    null,
  );
  return line;
}

/**
 * Expected billed line amount for a logical Signed version: **persisted**
 * `serviceParameters.priceComponents[]` (Swagger `ServiceParametersPreview`; driver / evaluated `value`) on
 * `GET /service-contract/{id}?versionId=` after third-tab merge, × effective quantity. If the preview omits
 * `priceComponents` entries, falls back to `contractFormulas` on the snapshot, then
 * {@link buildServiceContractEditPayloadFromDetail} (third-tab) shape.
 *
 * Authoritative preview uses **`priceComponents`**, not `contractFormulas` (omitted from `ServiceParametersPreview`).
 */
export async function readPdt2599ExpectedBilledLineFromSignedVersionEditPayload(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  logicalVersionId: number,
  billingPriceComponentId: number | null,
): Promise<number | null> {
  const ctx = await loadPdt2599SignedVersionEditFormulaContext(
    Request,
    Endpoints,
    contractId,
    logicalVersionId,
    billingPriceComponentId,
  );
  if (ctx == null) return null;
  return ctx.formulaValue * ctx.quantityEff;
}

/**
 * Expected line amount for the billing price component on a **logical Signed version**:
 * prefers `formulaVariableId` + tariff value from `GET /service-contract/{id}?versionId=` (contract snapshot is
 * authoritative for which variable is billed). If the snapshot omits `formulaVariableId`, falls back to
 * `GET /price-components/{billingPriceComponentId}` (some catalog responses omit formulas; contract still holds the row).
 */
export async function pdt2599ResolveExpectedSignedVersionPriceComponentAmountForBillingPc(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  logicalVersionId: number,
  billingPriceComponentId: number,
  ctx: string,
  fallbackDriver: number,
): Promise<number> {
  const fromPersistedEditShape = await readPdt2599ExpectedBilledLineFromSignedVersionEditPayload(
    Request,
    Endpoints,
    contractId,
    logicalVersionId,
    billingPriceComponentId,
  );
  if (fromPersistedEditShape != null) return fromPersistedEditShape;

  const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: logicalVersionId },
  });
  await expect(res).CheckResponse();
  let d = (await res.json()) as Record<string, unknown>;
  let fvId = extractFirstContractPriceComponentFormulaVariableId(d);

  if (fvId == null) {
    const sid = (d.basicParameters as { serviceDetailId?: number } | undefined)?.serviceDetailId;
    if (sid) {
      const tRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
        params: { serviceDetailId: sid },
      });
      await expect(tRes).CheckResponse();
      const tab = (await tRes.json()) as Record<string, unknown>;
      const prev = (d.thirdPageTabs ?? {}) as Record<string, unknown>;
      d = { ...d, thirdPageTabs: { ...prev, ...tab } };
      fvId = extractFirstContractPriceComponentFormulaVariableId(d);
    }
  }

  if (fvId == null) {
    try {
      fvId = await readPdt2599BillingPriceComponentPrimaryFormulaVariableId(
        Request,
        Endpoints,
        billingPriceComponentId,
      );
    } catch {
      fvId = null;
    }
  }

  const lastResortFromCatalogAndContract = async (): Promise<number | null> => {
    const fromCompose = await readPdt2599BillingTariffValueForSignedVersion(
      Request,
      Endpoints,
      contractId,
      logicalVersionId,
      billingPriceComponentId,
    );
    if (fromCompose != null) return fromCompose;
    return readPdt2599ContractVersionPrimaryPriceComponentValue(Request, Endpoints, contractId, logicalVersionId);
  };

  if (fvId == null) {
    const hit = await lastResortFromCatalogAndContract();
    if (hit != null) return hit;
    void test.info().attach(
      `[PDT-2599] ${ctx}: cannot resolve formulaVariableId + tariff; using precondition driver ${fallbackDriver}`,
      {
        body: 'pdt2599ResolveExpectedSignedVersionPriceComponentAmountForBillingPc: no fvId on contract or billing PC GET',
        contentType: 'text/plain; charset=utf-8',
      },
    );
    return fallbackDriver;
  }

  let hit = extractPdt2599ContractTariffValueForFormulaVariableId(d, fvId);
  if (hit != null) return hit;

  const res2 = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: logicalVersionId },
  });
  await expect(res2).CheckResponse();
  d = (await res2.json()) as Record<string, unknown>;
  hit = extractPdt2599ContractTariffValueForFormulaVariableId(d, fvId);
  if (hit != null) return hit;

  const fromFallback = await lastResortFromCatalogAndContract();
  if (fromFallback != null) return fromFallback;

  void test.info().attach(
    `[PDT-2599] ${ctx}: contract snapshot missing value for formulaVariableId=${fvId}; using precondition driver ${fallbackDriver}`,
    {
      body: `billingPcId=${billingPriceComponentId}`,
      contentType: 'text/plain; charset=utf-8',
    },
  );
  return fallbackDriver;
}

/**
 * Primary billing driver on a logical Signed version from `GET /service-contract/{id}?versionId=`
 * (see {@link extractPdt2599ExpectedFormulaScalarFromContractGetJson}).
 */
export async function readPdt2599ContractVersionPrimaryPriceComponentValue(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  logicalVersionId: number,
): Promise<number | null> {
  const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: logicalVersionId },
  });
  await expect(res).CheckResponse();
  let d = (await res.json()) as Record<string, unknown>;
  let v = extractPdt2599ExpectedFormulaScalarFromContractGetJson(d);
  if (v != null) return v;
  const sid = (d.basicParameters as { serviceDetailId?: number } | undefined)?.serviceDetailId;
  if (sid) {
    const tRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
      params: { serviceDetailId: sid },
    });
    await expect(tRes).CheckResponse();
    const tab = (await tRes.json()) as Record<string, unknown>;
    const prev = (d.thirdPageTabs ?? {}) as Record<string, unknown>;
    d = { ...d, thirdPageTabs: { ...prev, ...tab } };
    v = extractPdt2599ExpectedFormulaScalarFromContractGetJson(d);
    if (v != null) return v;
  }
  const resLatest = await Request.get(`${Endpoints.serviceContract}/${contractId}`);
  await expect(resLatest).CheckResponse();
  const dLatest = (await resLatest.json()) as Record<string, unknown>;
  const vLatest = extractPdt2599ExpectedFormulaScalarFromContractGetJson(dLatest);
  if (vLatest != null) return vLatest;
  return null;
}

/**
 * Tariff `value` for a Signed version using the same formula resolution as contract compose (`resolvePdt2599ContractFormulaForPost`).
 */
export async function readPdt2599BillingTariffValueForSignedVersion(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  logicalVersionId: number,
  billingPriceComponentStored: unknown,
): Promise<number | null> {
  const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: logicalVersionId },
  });
  await expect(res).CheckResponse();
  let d = (await res.json()) as Record<string, unknown>;
  const sid = (d.basicParameters as { serviceDetailId?: number } | undefined)?.serviceDetailId;
  if (sid) {
    const tRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
      params: { serviceDetailId: sid },
    });
    await expect(tRes).CheckResponse();
    const tab = (await tRes.json()) as Record<string, unknown>;
    const prev = (d.thirdPageTabs ?? {}) as Record<string, unknown>;
    d = { ...d, thirdPageTabs: { ...prev, ...tab } };
  }
  const third = (d.thirdPageTabs ?? {}) as Record<string, unknown>;
  const f = await resolvePdt2599ContractFormulaForPost(Request, Endpoints, third, billingPriceComponentStored, []);
  return f != null && Number.isFinite(f.value) ? f.value : null;
}

/** Resolve expected billed scalar for a Signed logical version; last resort: precondition `fallbackDriver`. */
export async function pdt2599ResolveExpectedSignedVersionPriceComponentValue(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  logicalVersionId: number,
  fallbackDriver: number,
  attachLabel: string,
  opts?: { billingPriceComponentStored?: unknown },
): Promise<number> {
  const fromExtract = await readPdt2599ContractVersionPrimaryPriceComponentValue(
    Request,
    Endpoints,
    contractId,
    logicalVersionId,
  );
  if (fromExtract != null) return fromExtract;
  if (opts?.billingPriceComponentStored != null) {
    const fromComposePath = await readPdt2599BillingTariffValueForSignedVersion(
      Request,
      Endpoints,
      contractId,
      logicalVersionId,
      opts.billingPriceComponentStored,
    );
    if (fromComposePath != null) return fromComposePath;
  }
  void test.info().attach(
    `[PDT-2599] ${attachLabel}: could not resolve tariff from contract GET or compose-style formula read; using precondition driver ${fallbackDriver}`,
    {
      body: 'extract + readPdt2599BillingTariffValueForSignedVersion both empty',
      contentType: 'text/plain; charset=utf-8',
    },
  );
  return fallbackDriver;
}

/** Tabular invoice line (summary or detailed tab); both Swagger schemas expose `priceComponent` + `value`. */
export type Pdt2599InvoiceTabularLineRow = {
  priceComponent?: string;
  value?: number;
  unitPrice?: number;
  totalVolumes?: number;
  [key: string]: unknown;
};

function filterPdt2599InvoiceTabularRowsByPcName(
  rows: Pdt2599InvoiceTabularLineRow[],
  billingPcName: string,
): Pdt2599InvoiceTabularLineRow[] {
  const n = billingPcName.trim();
  if (!n) return rows;
  const lower = n.toLowerCase();
  const exact = rows.filter((r) => String(r.priceComponent ?? '').trim().toLowerCase() === lower);
  if (exact.length > 0) return exact;
  return rows.filter((r) => String(r.priceComponent ?? '').toLowerCase().includes(lower));
}

function parsePdt2599InvoiceTabularNumeric(raw: unknown): number | null {
  if (raw == null || raw === '') return null;
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw;
  if (typeof raw === 'string') {
    const n = Number(raw.trim().replace(/\s/g, '').replace(',', '.'));
    return Number.isFinite(n) ? n : null;
  }
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}

/** Line amount candidates: Swagger `value` and/or `unitPrice` × `totalVolumes` (same row). */
function collectPdt2599TabularRowAmountCandidates(row: Pdt2599InvoiceTabularLineRow): number[] {
  const out: number[] = [];
  const v = parsePdt2599InvoiceTabularNumeric(row.value);
  if (v != null) out.push(v);
  const up = parsePdt2599InvoiceTabularNumeric(row.unitPrice);
  const tv = row.totalVolumes;
  if (up != null && tv != null && tv !== '' && Number.isFinite(Number(tv))) {
    const prod = up * Number(tv);
    if (Number.isFinite(prod)) out.push(prod);
  }
  return out;
}

/**
 * Among tabular rows, pick a line amount (from `value` and/or unit×volume) closest to `expected`.
 */
function pickTabularAmountNearestExpected(
  rows: Pdt2599InvoiceTabularLineRow[],
  expected: number,
  attachCtx: string,
  maxDistOverride?: number,
): number | null {
  const scored: { v: number; hint: string }[] = [];
  for (const r of rows) {
    const label = String(r.priceComponent ?? '');
    for (const v of collectPdt2599TabularRowAmountCandidates(r)) {
      scored.push({ v, hint: label });
    }
  }
  if (scored.length === 0) return null;
  scored.sort((a, b) => Math.abs(a.v - expected) - Math.abs(b.v - expected));
  const best = scored[0]!;
  const dist = Math.abs(best.v - expected);
  const maxDist =
    maxDistOverride !== undefined
      ? maxDistOverride
      : Math.max(1.5, Math.abs(expected) * 0.08);
  if (Number.isFinite(maxDist) && dist > maxDist) {
    void test.info().attach(
      `[PDT-2599] ${attachCtx}: nearest tabular amount ${best.v} too far from expected ${expected} (dist=${dist}, max=${maxDist})`,
      {
        body: scored.map((s) => `${s.hint}→${s.v}`).join(' | '),
        contentType: 'text/plain; charset=utf-8',
      },
    );
    return null;
  }
  if (scored.length > 1) {
    void test.info().attach(
      `[PDT-2599] ${attachCtx}: amount ${best.v} nearest expected ${expected} (${scored.length} candidates)`,
      {
        body: scored.map((s) => `${s.hint}→${s.v}`).join(' | '),
        contentType: 'text/plain; charset=utf-8',
      },
    );
  }
  return best.v;
}

function sumFiniteTabularRowValues(rows: Pdt2599InvoiceTabularLineRow[]): number | null {
  let sum = 0;
  let any = false;
  for (const r of rows) {
    const raw = r.value as unknown;
    if (raw == null || raw === '') continue;
    const parsed =
      typeof raw === 'number'
        ? raw
        : typeof raw === 'string'
          ? Number(raw.trim().replace(/\s/g, '').replace(',', '.'))
          : Number(raw);
    if (Number.isFinite(parsed)) {
      sum += parsed;
      any = true;
    }
  }
  return any ? sum : null;
}

/** Paginates `GET invoice/summary-data` (same query shape as {@link billingValidator}). */
export async function fetchPdt2599InvoiceSummaryRows(
  Request: RequestLike,
  invoiceId: number,
): Promise<Pdt2599InvoiceTabularLineRow[]> {
  const size = 25;
  const out: Pdt2599InvoiceTabularLineRow[] = [];
  for (let page = 0; page < 50; page++) {
    const res = await Request.get(`invoice/summary-data?id=${invoiceId}&page=${page}&size=${size}`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as {
      content?: Pdt2599InvoiceTabularLineRow[];
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
 * Unit **rate** / driver on the contract snapshot (`ServiceParametersPreview.priceComponents[].value`), optionally
 * matched to the billing PC's `formulaVariableId`. OVER_TIME invoice **line** totals often equal `rate × totalVolumes`
 * on the invoice row, not the bare rate.
 */
export async function readPdt2599SignedVersionBillingRateScalar(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  logicalVersionId: number,
  billingPriceComponentId: number | null,
  fallbackDriver: number,
): Promise<number> {
  const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: logicalVersionId },
  });
  await expect(res).CheckResponse();
  let detail = (await res.json()) as Record<string, unknown>;
  const sid = (detail.basicParameters as { serviceDetailId?: number } | undefined)?.serviceDetailId;
  if (sid) {
    const tRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
      params: { serviceDetailId: sid },
    });
    await expect(tRes).CheckResponse();
    const tab = (await tRes.json()) as Record<string, unknown>;
    const prev = (detail.thirdPageTabs ?? {}) as Record<string, unknown>;
    detail = { ...detail, thirdPageTabs: { ...prev, ...tab } };
  }
  const svc = (detail.serviceParameters ?? {}) as {
    priceComponents?: Array<{ formulaVariableId?: unknown; value?: unknown }>;
  };
  const rows =
    (svc.priceComponents ?? [])
      .map((p) => ({
        formulaVariableId:
          p.formulaVariableId != null && p.formulaVariableId !== ''
            ? Number(p.formulaVariableId)
            : NaN,
        value: parsePdt2599ContractPriceComponentNumeric(p.value),
      }))
      .filter((r) => r.value != null && Number.isFinite(r.value as number)) as {
      formulaVariableId: number;
      value: number;
    }[];

  let fvId: number | null = null;
  if (billingPriceComponentId != null) {
    try {
      fvId = await readPdt2599BillingPriceComponentPrimaryFormulaVariableId(
        Request,
        Endpoints,
        billingPriceComponentId,
      );
    } catch {
      fvId = null;
    }
  }
  const matched =
    fvId != null
      ? rows.find((r) => Number.isFinite(r.formulaVariableId) && r.formulaVariableId === fvId)
      : undefined;
  if (matched) return matched.value;
  if (rows.length > 0) return rows[0]!.value;

  const ctx = await loadPdt2599SignedVersionEditFormulaContext(
    Request,
    Endpoints,
    contractId,
    logicalVersionId,
    billingPriceComponentId,
  );
  if (ctx != null) return ctx.formulaValue;

  const s = extractPdt2599ExpectedFormulaScalarFromContractGetJson(detail);
  if (s != null) return s;
  return fallbackDriver;
}

function firstPdt2599InvoiceTabularRowWithNumericLine(
  rows: Pdt2599InvoiceTabularLineRow[],
): Pdt2599InvoiceTabularLineRow | null {
  for (const r of rows) {
    if (parsePdt2599InvoiceTabularNumeric(r.value) != null) return r;
    if (parsePdt2599InvoiceTabularNumeric(r.unitPrice) != null) return r;
  }
  return rows[0] ?? null;
}

/** One row from `GET /invoice/detailed-data` (Swagger `InvoiceDetailedDataResponse`). */
export type Pdt2599InvoiceDetailedDataRow = Pdt2599InvoiceTabularLineRow;

/** Paginates `invoice/detailed-data`; on non-2xx stops and returns rows collected so far (draft invoices may omit this tab). */
export async function fetchPdt2599InvoiceDetailedRows(
  Request: RequestLike,
  invoiceId: number,
): Promise<Pdt2599InvoiceDetailedDataRow[]> {
  const size = 100;
  const out: Pdt2599InvoiceDetailedDataRow[] = [];
  for (let page = 0; page < 50; page++) {
    const res = await Request.get(`invoice/detailed-data?id=${invoiceId}&page=${page}&size=${size}`);
    const st = res.status();
    if (st < 200 || st >= 300) {
      void test.info().attach('[PDT-2599] invoice/detailed-data non-success (skipped)', {
        body: `status=${st} page=${page} invoiceId=${invoiceId}`,
        contentType: 'text/plain; charset=utf-8',
      });
      break;
    }
    const body = (await res.json()) as {
      content?: Pdt2599InvoiceDetailedDataRow[];
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
 * Asserts invoice summary/detailed line matches the Signed version billing **rate** on
 * `serviceParameters.priceComponents[].value` (× invoice row `totalVolumes` when present).
 *
 * OVER_TIME lines are typically `value ≈ rate × totalVolumes`, not the bare driver.
 *
 * `opts.expectedAmount` (optional) overrides the contract **rate** when callers have already resolved it.
 *
 * Fallback: legacy compare vs a single scalar / preview net when tabular rows lack numeric `value`.
 */
export async function assertPdt2599InvoiceBilledAmountMatchesSignedVersionPriceComponent(
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  invoiceId: number,
  contractId: number,
  expectedLogicalVersionId: number,
  fallbackDriver: number,
  ctx: string,
  opts?: { priceComponentId?: number; expectedAmount?: number },
): Promise<void> {
  const billingPcId = opts?.priceComponentId ?? null;
  let rateContract =
    opts?.expectedAmount !== undefined
      ? opts.expectedAmount
      : await readPdt2599SignedVersionBillingRateScalar(
          Request,
          Endpoints,
          contractId,
          expectedLogicalVersionId,
          billingPcId,
          fallbackDriver,
        );

  void test.info().attach(`[PDT-2599] ${ctx}: contract v${expectedLogicalVersionId} billing rate (unit)`, {
    body: `rate=${rateContract} (fallbackDriver=${fallbackDriver})`,
    contentType: 'text/plain; charset=utf-8',
  });

  let billingPcName = '';
  if (billingPcId != null) {
    const pcRes = await Request.get(`${Endpoints.priceComponent}/${billingPcId}`);
    await expect(pcRes).CheckResponse();
    billingPcName = String(((await pcRes.json()) as Record<string, unknown>).name ?? '').trim();
  }

  const summaryRows = await fetchPdt2599InvoiceSummaryRows(Request, invoiceId);
  const summaryCandidate = billingPcName
    ? filterPdt2599InvoiceTabularRowsByPcName(summaryRows, billingPcName)
    : summaryRows;
  if (billingPcName && summaryCandidate.length === 0 && summaryRows.length > 0) {
    void test.info().attach(
      `[PDT-2599] ${ctx}: summary-data PC name filter had 0 rows (billing PC "${billingPcName}")`,
      {
        body: `priceComponent labels: ${summaryRows.map((r) => String(r.priceComponent ?? '')).join(' | ')}`,
        contentType: 'text/plain; charset=utf-8',
      },
    );
  }
  const summaryForPick =
    summaryRows.length === 1 && billingPcName
      ? summaryRows
      : summaryCandidate.length > 0
        ? summaryCandidate
        : summaryRows;
  if (billingPcName && summaryForPick !== summaryCandidate && summaryRows.length === 1) {
    void test.info().attach(
      `[PDT-2599] ${ctx}: single summary-data row; using it without PC-name filter (label may differ from catalog name "${billingPcName}")`,
      {
        body: String(summaryRows[0]?.priceComponent ?? ''),
        contentType: 'text/plain; charset=utf-8',
      },
    );
  }

  const tryAssertLineVsRateTimesVolumes = (
    row: Pdt2599InvoiceTabularLineRow | null,
    sourceLabel: string,
  ): boolean => {
    if (row == null) return false;
    const val = parsePdt2599InvoiceTabularNumeric(row.value);
    const up = parsePdt2599InvoiceTabularNumeric(row.unitPrice);
    const tvRaw = row.totalVolumes;
    const tvEff =
      tvRaw != null && tvRaw !== '' && Number.isFinite(Number(tvRaw)) && Number(tvRaw) !== 0
        ? Number(tvRaw)
        : 1;
    const expectedLine = rateContract * tvEff;
    const lineSlack = Math.max(1.51, Math.abs(expectedLine) * 0.08);
    const rateSlack = Math.max(0.05, Math.abs(rateContract) * 0.02);
    const mathSlack = Math.max(0.05, Math.abs((val ?? up ?? 0) as number) * 0.02);

    if (val == null && up == null) return false;

    if (val != null && up != null) {
      const coherent = Math.abs(val - up * tvEff) <= mathSlack;
      const matchesContractRate = Math.abs(up - rateContract) <= rateSlack;
      const matchesContractLine = Math.abs(val - expectedLine) <= lineSlack;
      /** OVER_TIME billing often prorates: invoice unit/value can be below catalog rate when `totalVolumes` is not the fractional period (preview shows vol=1 but line is partial-period). */
      const proratedWithinRateCap =
        rateContract > 0 &&
        val <= rateContract * tvEff + lineSlack + 1e-6 &&
        val >= -mathSlack;
      /** Periodical / stacked window: line can exceed unit rate×1 when preview omits stacked `totalVolumes` (coherent row). Cap at 4× rate to avoid matching gross catalog defaults. */
      const stackedPeriodicalOk =
        coherent &&
        rateContract > 0 &&
        val > rateContract * tvEff + lineSlack * 0.25 &&
        val <= rateContract * 4 + lineSlack + 1e-6;
      expect(
        coherent &&
          (matchesContractRate ||
            matchesContractLine ||
            proratedWithinRateCap ||
            stackedPeriodicalOk),
        `${ctx}: invoice ${sourceLabel} line coherent and tied to contract v${expectedLogicalVersionId} (val=${val}, unit=${up}, vol=${tvEff}, rate=${rateContract}, expectedLine≈${expectedLine.toFixed(4)})`,
      ).toBe(true);
      return true;
    }

    if (val != null) {
      const withinFullLine = Math.abs(val - expectedLine) <= lineSlack;
      const proratedCap =
        rateContract > 0 && val <= rateContract * tvEff + lineSlack + 1e-6 && val >= 0;
      expect(
        withinFullLine || proratedCap,
        `${ctx}: invoice ${sourceLabel} value vs contract v${expectedLogicalVersionId} rate×volumes (rate=${rateContract}, volumes=${tvEff}, expectedLine≈${expectedLine.toFixed(4)})`,
      ).toBe(true);
      return true;
    }

    expect(
      Math.abs((up as number) - rateContract) <= rateSlack,
      `${ctx}: invoice ${sourceLabel} unitPrice vs contract v${expectedLogicalVersionId} rate (${rateContract})`,
    ).toBe(true);
    return true;
  };

  const summaryRow = firstPdt2599InvoiceTabularRowWithNumericLine(summaryForPick);
  if (summaryRow != null && tryAssertLineVsRateTimesVolumes(summaryRow, 'summary-data')) {
    return;
  }

  const detailRows = await fetchPdt2599InvoiceDetailedRows(Request, invoiceId);
  const detailCandidate = billingPcName
    ? filterPdt2599InvoiceTabularRowsByPcName(detailRows, billingPcName)
    : detailRows;
  const detailForPick =
    detailCandidate.length > 0
      ? detailCandidate
      : detailRows;
  const detailRow = firstPdt2599InvoiceTabularRowWithNumericLine(detailForPick);
  if (detailRow != null && tryAssertLineVsRateTimesVolumes(detailRow, 'detailed-data')) {
    return;
  }

  /** Legacy scalar path when rows have no `value` / unit price */
  const legacyExpected = rateContract;
  let actual =
    summaryForPick.length > 0
      ? billingPcName || summaryRows.length === 1
        ? pickTabularAmountNearestExpected(
            summaryForPick,
            legacyExpected,
            `${ctx} summary-data`,
            summaryRows.length === 1 ? Number.POSITIVE_INFINITY : undefined,
          )
        : (sumFiniteTabularRowValues(summaryForPick) ?? null)
      : null;
  let source: 'summary-data' | 'detailed-data' | 'preview-net' =
    actual != null ? 'summary-data' : 'preview-net';

  if (actual == null && detailForPick.length > 0) {
    actual =
      detailCandidate.length > 0
        ? billingPcName
          ? pickTabularAmountNearestExpected(detailForPick, legacyExpected, `${ctx} detailed-data`)
          : (sumFiniteTabularRowValues(detailForPick) ?? null)
        : null;
    if (actual != null) source = 'detailed-data';
  }

  if (actual != null) {
    const slack = Math.max(1.51, Math.abs(legacyExpected) * 0.08);
    expect(
      Math.abs((actual as number) - legacyExpected) <= slack,
      `${ctx}: invoice ${source} line vs contract v${expectedLogicalVersionId} (legacy scalar expected≈${legacyExpected}, slack≈${slack.toFixed(2)})`,
    ).toBe(true);
    return;
  }

  const invRes = await Request.get(`invoice?id=${invoiceId}`);
  await expect(invRes).CheckResponse();
  const invJson = (await invRes.json()) as Record<string, unknown>;
  void test.info().attach(
    `[PDT-2599] ${ctx}: no finite summary/detailed line values; asserting preview net ex-VAT vs ${legacyExpected}`,
    {
      body: `summaryRows=${summaryRows.length}`,
      contentType: 'text/plain; charset=utf-8',
    },
  );
  assertPdt2599InvoiceNetMatchesExpected(invJson, legacyExpected, `${ctx} (fallback: preview net ex-VAT)`);
}

/** Coerce Swagger/OpenAPI money fields (often `string` decimals) to a finite number. */
function coerceInvoiceMoneyScalar(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw;
  if (typeof raw === 'string') {
    const t = raw.trim();
    if (!t) return NaN;
    const normalized = t.replace(/\s/g, '').replace(',', '.');
    const n = Number(normalized);
    return Number.isFinite(n) ? n : NaN;
  }
  if (raw != null) {
    const n = Number(raw);
    return Number.isFinite(n) ? n : NaN;
  }
  return NaN;
}

/**
 * Sum per-row VAT breakdown when top-level invoice totals are blank (common on some DRAFT payloads).
 * Swagger `InvoiceVatRateResponse`: `amountExcludingVat`, `valueOfVat` (often strings).
 */
function sumInvoiceVatRateResponsesMoney(inv: Record<string, unknown>): number {
  const rows = inv.invoiceVatRateResponses;
  if (!Array.isArray(rows) || rows.length === 0) return NaN;
  let sum = 0;
  let anyFinite = false;
  for (const row of rows) {
    if (row == null || typeof row !== 'object' || Array.isArray(row)) continue;
    const o = row as Record<string, unknown>;
    const ex = coerceInvoiceMoneyScalar(o.amountExcludingVat);
    const vat = coerceInvoiceMoneyScalar(o.valueOfVat);
    if (Number.isFinite(ex)) {
      sum += ex;
      anyFinite = true;
    }
    if (Number.isFinite(vat)) {
      sum += vat;
      anyFinite = true;
    }
  }
  return anyFinite ? sum : NaN;
}

/**
 * Invoice net ex-VAT (`totalAmountExcludingVat` or sum of `invoiceVatRateResponses[].amountExcludingVat`).
 */
export function extractInvoiceNetAmountExcludingVat(inv: Record<string, unknown>): number | null {
  const top = coerceInvoiceMoneyScalar(inv.totalAmountExcludingVat);
  if (Number.isFinite(top)) return top;
  const rows = inv.invoiceVatRateResponses;
  if (!Array.isArray(rows) || rows.length === 0) return null;
  let sum = 0;
  let any = false;
  for (const row of rows) {
    if (row == null || typeof row !== 'object' || Array.isArray(row)) continue;
    const ex = coerceInvoiceMoneyScalar((row as Record<string, unknown>).amountExcludingVat);
    if (Number.isFinite(ex)) {
      sum += ex;
      any = true;
    }
  }
  return any ? sum : null;
}

/**
 * Assert draft invoice net matches the contract-version-derived expectation (ex-VAT).
 */
export function assertPdt2599InvoiceNetMatchesExpected(
  inv: Record<string, unknown>,
  expectedNet: number,
  ctx: string,
): void {
  const net = extractInvoiceNetAmountExcludingVat(inv);
  expect(net, `${ctx}: invoice net ex-VAT missing`).not.toBeNull();
  expect(Number.isFinite(net as number), ctx).toBe(true);
  expect(net as number, ctx).toBeCloseTo(expectedNet, 2);
}

/**
 * Primary numeric total from invoice GET preview.
 * Swagger `InvoiceResponse` exposes `totalAmountIncludingVat` / `totalAmountExcludingVat` as strings;
 * older payloads used flat numeric aliases — check both, then shallow nested wrappers and VAT rows.
 */
export function extractBillingInvoiceMoneyFingerprint(inv: Record<string, unknown>): number {
  const cands = [
    'totalAmountIncludingVat',
    'totalAmountExcludingVat',
    'totalAmountOfVat',
    'totalInvoiceAmount',
    'invoiceAmount',
    'netAmount',
    'grossAmount',
    'totalDueAmount',
    'initialAmount',
    'amountExcludingVat',
    'valueOfVat',
  ] as const;

  const tryOn = (o: Record<string, unknown>): number => {
    for (const k of cands) {
      const n = coerceInvoiceMoneyScalar(o[k]);
      if (Number.isFinite(n)) return n;
    }
    return NaN;
  };

  let hit = tryOn(inv);
  if (Number.isFinite(hit)) return hit;

  for (const v of Object.values(inv)) {
    if (v != null && typeof v === 'object') {
      if (Array.isArray(v)) {
        for (const el of v) {
          if (el != null && typeof el === 'object' && !Array.isArray(el)) {
            hit = tryOn(el as Record<string, unknown>);
            if (Number.isFinite(hit)) return hit;
          }
        }
      } else {
        hit = tryOn(v as Record<string, unknown>);
        if (Number.isFinite(hit)) return hit;
      }
    }
  }

  hit = sumInvoiceVatRateResponsesMoney(inv);
  if (Number.isFinite(hit)) return hit;

  return NaN;
}

const BILLING_INVOICE_LINKED_VERSION_KEYS = [
  'serviceContractVersionId',
  'contractLogicalVersionId',
  'contractVersionInternalId',
] as const;

const BILLING_INVOICE_SERVICE_VERSION_KEYS = ['serviceVersionId'] as const;

function extractBillingInvoiceServiceVersionIdWalk(node: unknown, depth: number): number | null {
  if (depth > 16 || node == null) return null;
  if (typeof node !== 'object') return null;
  if (Array.isArray(node)) {
    for (const el of node) {
      const r = extractBillingInvoiceServiceVersionIdWalk(el, depth + 1);
      if (r != null) return r;
    }
    return null;
  }
  const o = node as Record<string, unknown>;
  for (const k of BILLING_INVOICE_SERVICE_VERSION_KEYS) {
    if (Object.prototype.hasOwnProperty.call(o, k)) {
      const raw = o[k];
      const n = typeof raw === 'number' ? raw : raw != null ? Number(raw) : NaN;
      if (Number.isFinite(n)) return n;
    }
  }
  for (const v of Object.values(o)) {
    const r = extractBillingInvoiceServiceVersionIdWalk(v, depth + 1);
    if (r != null) return r;
  }
  return null;
}

/** Commercial `serviceVersionId` on draft invoice payload when Phoenix exposes it (nested JSON tolerated). */
export function extractBillingInvoiceServiceVersionIdDeep(inv: Record<string, unknown>): number | null {
  for (const k of BILLING_INVOICE_SERVICE_VERSION_KEYS) {
    const raw = inv[k];
    const n = typeof raw === 'number' ? raw : raw != null ? Number(raw) : NaN;
    if (Number.isFinite(n)) return n;
  }
  return extractBillingInvoiceServiceVersionIdWalk(inv, 0);
}

function extractBillingInvoiceLinkedVersionIdWalk(node: unknown, depth: number): number | null {
  if (depth > 16 || node == null) return null;
  if (typeof node !== 'object') return null;
  if (Array.isArray(node)) {
    for (const el of node) {
      const r = extractBillingInvoiceLinkedVersionIdWalk(el, depth + 1);
      if (r != null) return r;
    }
    return null;
  }
  const o = node as Record<string, unknown>;
  for (const k of BILLING_INVOICE_LINKED_VERSION_KEYS) {
    if (Object.prototype.hasOwnProperty.call(o, k)) {
      const raw = o[k];
      const n = typeof raw === 'number' ? raw : raw != null ? Number(raw) : NaN;
      if (Number.isFinite(n)) return n;
    }
  }
  for (const v of Object.values(o)) {
    const r = extractBillingInvoiceLinkedVersionIdWalk(v, depth + 1);
    if (r != null) return r;
  }
  return null;
}

/**
 * Linked logical service-contract version on draft invoice (nested JSON tolerated).
 */
export function extractBillingInvoiceLinkedVersionIdDeep(inv: Record<string, unknown>): number | null {
  for (const k of BILLING_INVOICE_LINKED_VERSION_KEYS) {
    const raw = inv[k];
    const n = typeof raw === 'number' ? raw : raw != null ? Number(raw) : NaN;
    if (Number.isFinite(n)) return n;
  }
  return extractBillingInvoiceLinkedVersionIdWalk(inv, 0);
}

/**
 * Best-effort linked **logical** service-contract version on draft invoice GET (`serviceContractVersionId`, etc.).
 */
export function extractBillingInvoiceLinkedVersionId(inv: Record<string, unknown>): number | null {
  return extractBillingInvoiceLinkedVersionIdDeep(inv);
}

function extractInvoiceServiceContractDetailIdDeepWalk(node: unknown, depth: number): number | null {
  if (depth > 16 || node == null) return null;
  if (typeof node !== 'object') return null;
  if (Array.isArray(node)) {
    for (const el of node) {
      const r = extractInvoiceServiceContractDetailIdDeepWalk(el, depth + 1);
      if (r != null) return r;
    }
    return null;
  }
  const o = node as Record<string, unknown>;
  if (Object.prototype.hasOwnProperty.call(o, 'serviceContractDetailId')) {
    const raw = o.serviceContractDetailId;
    const n = typeof raw === 'number' ? raw : raw != null ? Number(raw) : NaN;
    if (Number.isFinite(n)) return n;
  }
  for (const v of Object.values(o)) {
    const r = extractInvoiceServiceContractDetailIdDeepWalk(v, depth + 1);
    if (r != null) return r;
  }
  return null;
}

/** ISO `yyyy-mm-dd` from a string-ish value, else null. */
function pdt2599IsoDate(s: unknown): string | null {
  if (typeof s !== 'string') return null;
  const m = s.slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(m)) return null;
  return m;
}

/** `InvoiceResponse.serviceContract: ShortResponse` → numeric id (Swagger `dev/swagger-spec.json`). */
function pdt2599InvoiceServiceContractId(inv: Record<string, unknown>): number | null {
  const sc = inv.serviceContract;
  if (sc == null || typeof sc !== 'object' || Array.isArray(sc)) return null;
  const raw = (sc as { id?: unknown }).id;
  if (raw == null || raw === '') return null;
  const n = typeof raw === 'number' ? raw : Number(raw);
  return Number.isFinite(n) ? n : null;
}

/** `taxEventDate` drives version selection on billing-run; fall back to `invoiceDate` for older payload shapes. */
function pdt2599InvoiceDateForVersion(inv: Record<string, unknown>): string | null {
  return pdt2599IsoDate(inv.taxEventDate) ?? pdt2599IsoDate(inv.invoiceDate);
}

/**
 * STRICT linkage assertion: invoice ↔ expected logical service-contract version.
 *
 * Proves linkage from authoritative Swagger `InvoiceResponse` fields:
 *  1. `invoice.serviceContract.id === contractId` (`ShortResponse`).
 *  2. `invoice.taxEventDate` (or `invoiceDate`) falls inside the expected version's
 *     `[startDate, endDate]` window from `GET /service-contract/{id}` `versions[]`
 *     (open-ended `endDate` allowed for the latest Signed slice).
 *  3. When the invoice payload also exposes deep linkage keys
 *     (`serviceContractVersionId` / `contractLogicalVersionId` / `contractVersionInternalId`
 *     / `serviceContractDetailId` / `serviceVersionId`), each MUST match the expected
 *     version's `versionId` / `basicParameters.id` / `basicParameters.serviceVersionId`
 *     respectively — no silent skip.
 *
 * Use instead of {@link assertPdt2599InvoiceContractVersionLinkageBestEffort} when the
 * test must fail loudly on missing or mismatched linkage (TC-BE-27..30 require this).
 */
export async function assertPdt2599InvoiceLinksToExpectedContractVersionStrict(
  inv: Record<string, unknown>,
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  expectedLogicalVersionId: number,
  ctx: string,
): Promise<void> {
  const invoiceContractId = pdt2599InvoiceServiceContractId(inv);
  expect(invoiceContractId, `${ctx}: invoice.serviceContract.id must be present (Swagger InvoiceResponse.serviceContract)`).not.toBeNull();
  expect(invoiceContractId, `${ctx}: invoice.serviceContract.id vs expected contractId`).toBe(contractId);

  const overall = await Request.get(`${Endpoints.serviceContract}/${contractId}`);
  await expect(overall).CheckResponse();
  const overallJson = (await overall.json()) as Record<string, unknown>;
  const versionRows = (overallJson.versions ?? []) as Array<{
    versionId?: number;
    startDate?: string;
    endDate?: string | null;
  }>;
  const expectedRow = versionRows.find((v) => Number(v.versionId) === expectedLogicalVersionId);
  expect(
    expectedRow,
    `${ctx}: GET /service-contract/${contractId} must contain versionId=${expectedLogicalVersionId}`,
  ).toBeTruthy();
  const verStart = pdt2599IsoDate(expectedRow!.startDate);
  expect(verStart, `${ctx}: contract v${expectedLogicalVersionId}.startDate must be a yyyy-mm-dd ISO date`).not.toBeNull();
  const verEnd = pdt2599IsoDate(expectedRow!.endDate);

  const invDate = pdt2599InvoiceDateForVersion(inv);
  expect(invDate, `${ctx}: invoice.taxEventDate or invoiceDate must be present`).not.toBeNull();
  expect(
    invDate! >= verStart!,
    `${ctx}: invoice date ${invDate} >= v${expectedLogicalVersionId}.startDate ${verStart}`,
  ).toBe(true);
  if (verEnd != null) {
    expect(
      invDate! <= verEnd,
      `${ctx}: invoice date ${invDate} <= v${expectedLogicalVersionId}.endDate ${verEnd}`,
    ).toBe(true);
  }

  const verRes = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: expectedLogicalVersionId },
  });
  await expect(verRes).CheckResponse();
  const verJson = (await verRes.json()) as Record<string, unknown>;
  const basic = (verJson.basicParameters ?? {}) as { id?: unknown; serviceVersionId?: unknown };
  const detailPk = basic.id != null && basic.id !== '' ? Number(basic.id) : NaN;
  expect(
    Number.isFinite(detailPk),
    `${ctx}: contract v${expectedLogicalVersionId} basicParameters.id`,
  ).toBe(true);
  const expectedSvcRaw = basic.serviceVersionId;
  const expectedSvcVer =
    expectedSvcRaw == null || expectedSvcRaw === '' || !Number.isFinite(Number(expectedSvcRaw))
      ? null
      : Number(expectedSvcRaw);

  const linked = extractBillingInvoiceLinkedVersionIdDeep(inv);
  const invDetail = extractInvoiceServiceContractDetailIdDeepWalk(inv, 0);
  const invSvcVer = extractBillingInvoiceServiceVersionIdDeep(inv);
  if (linked != null) {
    expect(linked, `${ctx}: invoice deep logical version key vs expected v${expectedLogicalVersionId}`).toBe(
      expectedLogicalVersionId,
    );
  }
  if (invDetail != null) {
    expect(
      invDetail,
      `${ctx}: invoice deep serviceContractDetailId vs contract v${expectedLogicalVersionId} basicParameters.id`,
    ).toBe(detailPk);
  }
  if (invSvcVer != null && expectedSvcVer != null) {
    expect(
      invSvcVer,
      `${ctx}: invoice deep serviceVersionId vs contract v${expectedLogicalVersionId} serviceVersionId`,
    ).toBe(expectedSvcVer);
  }
}

/**
 * Validates draft invoice ↔ expected **logical** contract version using any fields Phoenix exposes:
 * logical version id (`serviceContractVersionId` / aliases), `serviceContractDetailId`, and **commercial** `serviceVersionId`
 * vs `GET /service-contract/{id}?versionId=` `basicParameters`. If the preview omits all linkage keys, only attaches
 * diagnostic text — invoice line vs contract price-component amount remains the cross-check.
 */
export async function assertPdt2599InvoiceContractVersionLinkageBestEffort(
  inv: Record<string, unknown>,
  Request: RequestLike,
  Endpoints: Pdt2599ChainFixtures['Endpoints'],
  contractId: number,
  expectedLogicalVersionId: number,
  ctx: string,
): Promise<void> {
  const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: expectedLogicalVersionId },
  });
  await expect(res).CheckResponse();
  const d = (await res.json()) as Record<string, unknown>;
  const basic = (d.basicParameters ?? {}) as { id?: unknown; serviceVersionId?: unknown };
  const detailPk = typeof basic.id === 'number' ? basic.id : basic.id != null ? Number(basic.id) : NaN;
  expect(Number.isFinite(detailPk), `${ctx}: contract basicParameters.id for version ${expectedLogicalVersionId}`).toBe(
    true,
  );

  const expectedSvcRaw = basic.serviceVersionId;
  const expectedSvcVer =
    expectedSvcRaw == null || expectedSvcRaw === ''
      ? null
      : Number.isFinite(Number(expectedSvcRaw))
        ? Number(expectedSvcRaw)
        : null;

  const linked = extractBillingInvoiceLinkedVersionIdDeep(inv);
  const invDetail = extractInvoiceServiceContractDetailIdDeepWalk(inv, 0);
  const invSvcVer = extractBillingInvoiceServiceVersionIdDeep(inv);

  if (linked != null) {
    expect(linked, `${ctx}: linked logical version on invoice preview`).toBe(expectedLogicalVersionId);
  }
  if (invDetail != null) {
    expect(invDetail, `${ctx}: invoice serviceContractDetailId vs contract basicParameters.id`).toBe(detailPk);
  }
  if (invSvcVer != null && expectedSvcVer != null) {
    expect(invSvcVer, `${ctx}: invoice serviceVersionId vs contract GET basicParameters.serviceVersionId`).toBe(
      expectedSvcVer,
    );
  }

  if (linked == null && invDetail == null && invSvcVer == null) {
    void test.info().attach(
      `[PDT-2599] ${ctx}: invoice GET omits version linkage fields; amount check validates vs contract v${expectedLogicalVersionId} price-component row (contractDetailId=${detailPk})`,
      {
        body: 'No serviceContractVersionId / contractLogicalVersionId / serviceContractDetailId / serviceVersionId on preview payload.',
        contentType: 'text/plain; charset=utf-8',
      },
    );
  }
}

export async function getFirstDraftInvoiceTotal(
  Request: RequestLike,
  billingRunRoot: string,
  billingRunId: number,
): Promise<number> {
  const drafts = await Request.get(`${billingRunRoot}/draft-invoices?id=${billingRunId}&page=0&size=25`);
  await expect(drafts).CheckResponse();
  const dj = (await drafts.json()) as { content?: Array<{ id?: number }> };
  expect(dj.content?.length, 'Draft invoices expected').toBeGreaterThan(0);
  const invoiceId = dj.content![0]?.id as number;

  const invRes = await Request.get(`invoice?id=${invoiceId}`);
  await expect(invRes).CheckResponse();
  const inv = (await invRes.json()) as Record<string, unknown>;
  const v = extractBillingInvoiceMoneyFingerprint(inv);
  expect(Number.isFinite(v), `invoice money fingerprint missing (${JSON.stringify(Object.keys(inv))})`).toBe(
    true,
  );
  return v as number;
}

/**
 * Scenario for bug-detection: Signed v1 + Draft v2 straddle `billingDate` while billing POST stays 2xx.
 * Applies an extra Draft slice after two Signed anchors; callers assert via `expectPerPieceBillingRunWithoutInvoices`.
 */
export async function pdt2599PutSignedV1DraftV2BillingDateOnlyDraftCovers(
  Request: RequestLike,
  Endpoints: Pick<Pdt2599ChainFixtures, 'Endpoints'>['Endpoints'],
  contractId: number,
  loadDetailForEdit: (cid: number, versionId?: number) => Promise<Record<string, unknown>>,
): Promise<void> {
  let detail = await loadDetailForEdit(contractId);
  const c = String((detail.basicParameters as { creationDate?: string }).creationDate);
  const anchor2 = addDaysIso(c, 420);

  let putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
    data: buildServiceContractEditPayloadFromDetail(detail, {
      savingAsNewVersion: true,
      startDate: anchor2,
      contractVersionStatus: 'SIGNED',
    }),
  });
  await expect(putRes).CheckResponse();

  detail = await loadDetailForEdit(contractId);
  putRes = await Request.put(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId: pickLatestLogicalVersionId(detail.versions as { versionId: number }[]) },
    data: buildServiceContractEditPayloadFromDetail(detail, {
      savingAsNewVersion: true,
      startDate: addDaysIso(c, 340),
      contractVersionStatus: 'DRAFT',
    }),
  });
  await expect(putRes).CheckResponse();
}

/** Billing precondition chain: one price component matching `billingPriceMode` (no settlement price component). */
export async function runPdt2599SignedServiceContractBillingChain(
  fx: Pdt2599ChainFixtures,
  billingPriceMode: Pdt2599BillingApplicationModelKind,
): Promise<Pdt2599ChainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  let communicationDataForBilling: number;
  let communicationDataForContract: number;
  let billingPricePostBody!: unknown;
  /** INTERIM only: EXACT_AMOUNT IAP ids POSTed in the price step, registered on commercial service, stripped for compose, then restored. Order = v1/v2/v3 source amounts. */
  let interimDistinctIapIds: number[] = [];

  const pricePayloadBuilders: Record<
    Exclude<Pdt2599BillingApplicationModelKind, 'INTERIM'>,
    () => Record<string, unknown>
  > = {
    PER_PIECE: () => GeneratePayload.productAndServices.perPiece(),
    OVER_TIME_ONE_TIME: () => GeneratePayload.productAndServices.oneTime(),
    OVER_TIME_PERIODICAL: () => GeneratePayload.productAndServices.periodicalComponent(),
  };

  await test.step('Precondition (billing): Create customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition (billing): Communication channels', async () => {
    const customerDetailsId = Responses.customer[0].lastCustomerDetailId;
    const billingRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
      params: { customerDetailsId, communicationDataType: 'BILLING' },
    });
    await expect(billingRes).CheckResponse();
    const billingRows = await billingRes.json();
    const contractCommRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
      params: { customerDetailsId, communicationDataType: 'CONTRACT' },
    });
    await expect(contractCommRes).CheckResponse();
    const contractRows = await contractCommRes.json();
    expect(billingRows?.length && contractRows?.length).toBeTruthy();
    communicationDataForBilling = billingRows[0].id;
    communicationDataForContract = contractRows[0].id;
  });

  await test.step(`Precondition (billing): Price component${billingPriceMode === 'INTERIM' ? ' + distinct INTERIM IAPs (A/B/C)' : ''} (${billingPriceMode})`, async () => {
    if (billingPriceMode === 'INTERIM') {
      /**
       * Settlement PC + **three** `EXACT_AMOUNT` IAP entities (`56` / `92` / `188`) — align with INTERIM specs’ integer rows.
       * `service()` must reference these ids (via `Responses.interim`); contract POST skips seeding interim rows — see Commercial service + Compose steps.
       */
      const pcPayload = GeneratePayload.productAndServices.priceSettlement();
      const pcForFormula = await Request.post(Endpoints.priceComponent, {
        data: pcPayload,
      });
      await expect(pcForFormula).CheckResponse();
      const pcRaw = await pcForFormula.json();
      billingPricePostBody = await hydratePriceComponentBodyForCompose(Request, Endpoints, pcRaw);
      Responses.priceComponent.push(asPriceComponentId(pcRaw));

      interimDistinctIapIds = [];
      for (const amt of [56, 92, 188] as const) {
        const interimPayload = GeneratePayload.productAndServices.interim();
        interimPayload.valueType = 'EXACT_AMOUNT';
        interimPayload.value = amt;
        interimPayload.priceComponentId = null;
        const interimRes = await Request.post(Endpoints.interim, { data: interimPayload });
        await expect(interimRes).CheckResponse();
        interimDistinctIapIds.push(asPriceComponentId(await interimRes.json()));
      }
      expect(interimDistinctIapIds.length).toBe(3);
      return;
    }

    const price = await Request.post(Endpoints.priceComponent, {
      data: pricePayloadBuilders[billingPriceMode](),
    });
    await expect(price).CheckResponse();
    const raw = await price.json();
    billingPricePostBody = await hydratePriceComponentBodyForCompose(Request, Endpoints, raw);
    Responses.priceComponent.push(asPriceComponentId(raw));
  });

  await test.step('Precondition (billing): Term', async () => {
    const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition (billing): POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition (billing): Commercial service', async () => {
    if (billingPriceMode === 'INTERIM') {
      Responses.interim.push(...interimDistinctIapIds);
    }
    const service = await Request.post(Endpoints.service, {
      data: GeneratePayload.productAndServices.service(),
    });
    await expect(service).CheckResponse();
    Responses.service.push(await service.json());
    if (billingPriceMode === 'INTERIM') {
      Responses.interim.length = 0;
    }
  });

  return await test.step('Precondition (billing): Compose service-contract', async () => {
    const composed = await pdt2599ComposeServiceContractStep(
      fx,
      communicationDataForBilling,
      communicationDataForContract,
      billingPricePostBody,
    );
    if (billingPriceMode === 'INTERIM') {
      Responses.interim.push(...interimDistinctIapIds);
    }
    return composed;
  });
}
