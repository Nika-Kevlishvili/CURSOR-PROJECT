/**
 * PDT-2931 — Skip RiskList permission on product contract mass import (Dev).
 *
 * Risk trigger methodology: **excessively high POD consumption**, not block-list UIC hunting.
 * Each test creates a fresh customer + CONSUMER POD with `estimatedMonthlyAvgConsumption` at Phoenix max
 * (`PodBaseRequest` @Range max 99_999_999). Without `skip_risklist` → create/edit/MI must fail risk check;
 * with `skip_risklist` → must succeed. Mass import derives yearly kWh from POD column BY:
 * `sum(monthly CONSUMER pods) * 12 / 1000` (`ProductContractExcelMapper`).
 *
 * **All five Playwright tests** require the default EnergoTS auth user (`baseFixture`) to have
 * to have `skip_risklist` (`bg.energo.phoenix.security.verb.skip_risklist`) on **PRODUCT_CONTRACTS** on Dev.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2906-product-new-version-contract-update.spec.ts (+ fixtures — entity chain)
 * - tests/cursor/pdt-2854-pod-active-two-contracts.fixtures.ts (massImportGenerator pattern)
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (`podMonthlyAvgForYearlyContractKwh` inverse)
 * - mass-imports/generators/massImportGenerator.ts
 * - mass-imports/massImportPayloads/productContractMpPayload.ts (Excel A2/BY2 mapping)
 *
 */

import * as ExcelJS from 'exceljs';
import fs from 'fs';
import path from 'path';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { configuredBaseURL } from '../../fixtures/utils/baseUrl';
import { productContractMassPayload } from '../../mass-imports/massImportPayloads/productContractMpPayload';
import { ResponseLinker } from '../../utils/reporters/ResponseLinker';
import {
  buildEditProductContractPayload,
  productContractStatusUpdate,
} from './pdt-2815-version-validity.fixtures';
import {
  applyFixedParameterProductFields,
  applyFixedParameterTermFields,
  createSignedProductContractV1,
  entityId,
  loadProductDetailSnapshot,
} from './pdt-2906-product-new-version-contract-update.fixtures';

type FixtureRequest = baseFixture['Request'];
type FixtureFileUpload = baseFixture['FileUploadRequest'];

/** Spring `DomainType` path segment (enum name), not kebab-case value. */
export const PDT_2931_UPLOAD_URL = 'mass-import/PRODUCT_CONTRACTS/files/upload';
export const PDT_2931_TEMPLATE_URL = 'mass-import/PRODUCT_CONTRACTS/template/download';

/** Phoenix `PodBaseRequest` max for `estimatedMonthlyAvgConsumption` — triggers Risk List via high consumption. */
export const PDT_2931_MAX_POD_MONTHLY_CONSUMPTION = 99_999_999;

/** Matches Phoenix MI formula: `sum(monthly CONSUMER pods) * 12 / 1000`. */
export function pdt2931YearlyConsumptionKwhFromMonthly(monthly: number): number {
  return (monthly * 12) / 1000;
}

export const RISK_LIST_DECISION_SNIPPET = 'RISK_LIST_DECISION';

export const RISK_ASSESSMENT_RESTRICTION_SNIPPET =
  "Can't conclude a contract because of risk assessment restriction";

/** Shown when mass-import row errors prove Risk List was still invoked despite skip_risklist (PDT-2931 defect). */
export const PDT_2931_BACKEND_DEFECT_RISK_LIST_MSG =
  'PDT-2931 backend defect: skip_risklist not applied to mass import process (Risk List was called)';

/** When high-consumption POD triggers Risk List on direct POST without skip permission. */
export const PDT_2931_SKIP_RISKLIST_REQUIRED_MSG =
  'Positive PDT-2931 tests require the default EnergoTS auth user (User A) to have skip_risklist on PRODUCT_CONTRACTS (Dev). ' +
  'High-consumption POD triggers Risk List; skip permission must bypass the API call.';

export function isRiskListMassImportDefect(errorText: string): boolean {
  const upper = errorText.toUpperCase();
  return (
    upper.includes('RISK_LIST') ||
    upper.includes('RISK LIST API') ||
    errorText.toLowerCase().includes('risk assessment restriction')
  );
}

const PRODUCT_CONTRACT_MI_NOTIFICATION =
  /PRODUCT_CONTRACT_MASS_IMPORT_(COMPLETED|ERROR)/;

export type RiskCustomerRef = {
  customerId: number;
  customerVersionId: number;
  customerNumber: string;
  identifier: string;
};

export type ProductContractChain = {
  riskCustomer: RiskCustomerRef;
  podId: number;
  podIdentifier: string;
  /** Optional second CONSUMER POD for combined mass-import create row while baseline uses `podId`. */
  secondaryPodId?: number;
  secondaryPodIdentifier?: string;
  productId: number;
  productVersion: number;
  interestRateName: string;
};

export type SignedBaselineContract = {
  contractId: number;
  contractNumber: string;
  versionNumber: number;
  /** `basicParameters.type` from GET contract — required in MI column K on edit rows (Dev). */
  contractDetailType: string;
};

export type ProductContractMiRow =
  | {
      kind: 'create';
      customerIdentifier: string;
      customerVersion: number;
      productId: number;
      productVersion: number;
      podIdentifier: string;
      interestRateName: string;
      employeeIdentifier?: string;
    }
  | {
      kind: 'editCurrentVersion';
      contractNumber: string;
      version: number;
      contractDetailType: string;
      customerIdentifier: string;
      customerVersion: number;
      productId: number;
      productVersion: number;
      podIdentifier: string;
      interestRateName: string;
      marginalPrice?: string;
    }
  | {
      kind: 'editNewVersion';
      contractNumber: string;
      version: number;
      startDate: string;
      contractDetailType: string;
      customerIdentifier: string;
      customerVersion: number;
      productId: number;
      productVersion: number;
      podIdentifier: string;
      interestRateName: string;
    };

export type MassImportReportRow = {
  rowNumber: string | number | null;
  identifier: string | null;
  identifierVersion: string | null;
  errors: string | null;
};

/** Phoenix `MassImportErrorReportExcelService` header row (failed rows only). */
export const MASS_IMPORT_ERROR_REPORT_HEADERS = [
  'row_number',
  'identifier',
  'identifier_version',
  'errors',
] as const;

/** Parsed content of `MASS_IMPORT_ERROR_REPORT` — not merely file existence. */
export type MassImportErrorReportSnapshot = {
  headers: string[];
  /** Data rows below the header; Phoenix lists only `success = false` records. */
  failedRows: MassImportReportRow[];
  summary: string;
};

function cellText(value: ExcelJS.CellValue | null | undefined): string {
  if (value == null || value === '') {
    return '';
  }
  if (typeof value === 'object' && 'text' in value && value.text != null) {
    return String(value.text).trim();
  }
  return String(value).trim();
}

export function isoToday(): string {
  return new Date().toISOString().split('T')[0];
}

export function futureStartDateIso(): string {
  const d = new Date();
  d.setUTCMonth(d.getUTCMonth() + 1);
  d.setUTCDate(1);
  return d.toISOString().split('T')[0];
}

/** Align direct POST `additionalParameters` with high-consumption POD (Phoenix validation). */
export function isRiskListRejectionResponse(bodyText: string): boolean {
  return (
    bodyText.includes(RISK_LIST_DECISION_SNIPPET) ||
    bodyText.toLowerCase().includes('risk assessment restriction')
  );
}

/** Assert POST /product-contract succeeded or explain missing skip_risklist on User A. */
export function expectProductContractPostSucceeded(
  status: number,
  bodyText: string,
  context: string,
): void {
  if ([200, 201].includes(status)) {
    return;
  }
  const hint = isRiskListRejectionResponse(bodyText)
    ? `${PDT_2931_SKIP_RISKLIST_REQUIRED_MSG} `
    : '';
  expect(false, `${hint}${context} returned ${status}: ${bodyText.slice(0, 500)}`).toBe(true);
}

export function applyHighConsumptionContractAdditionalParameters(
  contractPayload: Record<string, unknown>,
): void {
  const ap = contractPayload.additionalParameters as Record<string, unknown> | undefined;
  if (!ap) {
    return;
  }
  ap.estimatedTotalConsumptionUnderContractKwh = pdt2931YearlyConsumptionKwhFromMonthly(
    PDT_2931_MAX_POD_MONTHLY_CONSUMPTION,
  );
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export async function resolveActiveInterestRateName(Request: FixtureRequest): Promise<string> {
  const res = await Request.get(`interest-rate/${envVariables.interest_rate}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as { name?: string };
  expect(body.name, 'active interest rate name').toBeTruthy();
  return String(body.name);
}

/** Always creates a new ACTIVE legal customer (unique UIC unless env override). */
export async function createFreshTestCustomer(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<RiskCustomerRef> {
  const payload = GeneratePayload.customers.customer_legal();
  const created = await Request.post(Endpoints.customer, { data: payload });
  await expect(created).CheckResponse();
  const body = await created.json();
  Responses.customer.push(body);
  const customerId = entityId(body);
  const identifier = String(body.identifier ?? payload.customerIdentifier);

  const detailRes = await Request.get(`${Endpoints.customer}/${customerId}?version=1`);
  await expect(detailRes).CheckResponse();
  const detail = (await detailRes.json()) as { versionId?: number; customerNumber?: number | string };
  const customerVersionId = Number(detail.versionId ?? 1);
  expect(customerVersionId, 'customer detail versionId for mass import column F').toBeGreaterThan(0);

  return {
    customerId,
    customerVersionId,
    customerNumber: String(detail.customerNumber ?? body.customerNumber ?? customerId),
    identifier,
  };
}

export async function createConsumerPod(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<{ podId: number; podIdentifier: string }> {
  const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();
  podPayload.type = 'CONSUMER';
  podPayload.consumptionPurpose = 'NON_HOUSEHOLD';
  podPayload.voltageLevel = 'LOW';
  podPayload.estimatedMonthlyAvgConsumption = String(PDT_2931_MAX_POD_MONTHLY_CONSUMPTION);
  const pod = await Request.post(Endpoints.pod, { data: podPayload });
  await expect(pod).CheckResponse();
  const podBody = await pod.json();
  Responses.pod.push(podBody);
  const podId = entityId(podBody);
  let identifier = String(podBody.identifier ?? '');
  if (!identifier) {
    const podGet = await Request.get(`${Endpoints.pod}/${podId}`);
    await expect(podGet).CheckResponse();
    const podDetail = await podGet.json();
    identifier = String(podDetail.identifier);
  }
  return { podId, podIdentifier: identifier };
}

export async function createFixedParameterProductChain(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<{ productId: number; productVersion: number }> {
  const termPayload = GeneratePayload.productAndServices.term();
  applyFixedParameterTermFields(termPayload);
  const term = await Request.post(Endpoints.terms, { data: termPayload });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const price = await Request.post(Endpoints.priceComponent, {
    data: GeneratePayload.productAndServices.electricity(),
  });
  await expect(price).CheckResponse();
  Responses.priceComponent.push(await price.json());

  const productPayload = GeneratePayload.productAndServices.product();
  applyFixedParameterProductFields(productPayload);
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  const productBody = await product.json();
  Responses.product.push(productBody);
  const productId = entityId(productBody);
  const snapshot = await loadProductDetailSnapshot(Request, Endpoints, productId, 1);
  return { productId: snapshot.productId, productVersion: snapshot.version };
}

export async function buildProductContractChain(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<ProductContractChain> {
  const riskCustomer = await createFreshTestCustomer(Request, GeneratePayload, Responses, Endpoints);
  const { podId, podIdentifier } = await createConsumerPod(Request, GeneratePayload, Responses, Endpoints);
  const { productId, productVersion } = await createFixedParameterProductChain(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  );
  const interestRateName = await resolveActiveInterestRateName(Request);
  return {
    riskCustomer,
    podId,
    podIdentifier,
    productId,
    productVersion,
    interestRateName,
  };
}

/** Second CONSUMER POD on the same customer — for combined mass-import create row while baseline keeps the first POD. */
export async function withSecondaryConsumerPod(
  chain: ProductContractChain,
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<ProductContractChain> {
  const { podId, podIdentifier } = await createConsumerPod(Request, GeneratePayload, Responses, Endpoints);
  return { ...chain, secondaryPodId: podId, secondaryPodIdentifier: podIdentifier };
}

/**
 * Signed product contract for the same customer / product / POD already in `Responses` from `buildProductContractChain`.
 */
export async function createSignedBaselineForChain(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  chain: ProductContractChain,
): Promise<SignedBaselineContract> {
  const customerIndex = Responses.customer.findIndex((c) => entityId(c) === chain.riskCustomer.customerId);
  const productIndex = Responses.product.findIndex((p) => entityId(p) === chain.productId);
  const podIndex = Responses.pod.findIndex((p) => entityId(p) === chain.podId);
  expect(customerIndex, 'chain customer in Responses').toBeGreaterThanOrEqual(0);
  expect(productIndex, 'chain product in Responses').toBeGreaterThanOrEqual(0);
  expect(podIndex, 'chain POD in Responses').toBeGreaterThanOrEqual(0);

  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    customerIndex,
    productIndex,
    podIndex,
  )) as Record<string, unknown>;
  applyHighConsumptionContractAdditionalParameters(contractPayload);
  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'COMBINED';
  pp.entryIntoForce = 'SIGNING';
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.supplyActivation = 'FIRST_DAY_OF_MONTH';

  const res = await Request.post(Endpoints.productContract, { data: contractPayload });
  const baselineRaw = await res.text();
  expectProductContractPostSucceeded(
    res.status(),
    baselineRaw,
    'Signed baseline precondition POST /product-contract',
  );
  const body = JSON.parse(baselineRaw);
  Responses.productContract.push(body);
  const contractId = entityId(body);

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
  const futureEntryInForce = futureStartDateIso();
  const editBp = editPayload.basicParameters as Record<string, unknown>;
  editBp.entryInForceDate = futureEntryInForce;
  editBp.startOfInitialTerm = futureEntryInForce;
  const editPp = editPayload.productParameters as Record<string, unknown>;
  editPp.entryIntoForceValue = futureEntryInForce;
  editPp.startOfContractValue = futureEntryInForce;
  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await expect(put).CheckResponse();

  const detail = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
  await expect(detail).CheckResponse();
  const detailBody = await detail.json();
  const contractNumber = String(
    detailBody.basicParameters?.contractNumber ?? detailBody.contractNumber ?? '',
  );
  expect(contractNumber, 'baseline contract number').toBeTruthy();
  const contractDetailType = String(detailBody.basicParameters?.type ?? '');
  expect(contractDetailType, 'baseline contract detail type for MI column K').toBeTruthy();
  return {
    contractId,
    contractNumber,
    versionNumber: Number(detailBody.basicParameters?.version ?? detailBody.versions?.[0]?.version ?? 1),
    contractDetailType,
  };
}

/** @deprecated Prefer `createSignedBaselineForChain` after `buildProductContractChain`. */
export async function createSignedBaselineProductContract(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<SignedBaselineContract> {
  const { contractId } = await createSignedProductContractV1(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  );
  const detail = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
  await expect(detail).CheckResponse();
  const body = await detail.json();
  const contractNumber = String(body.basicParameters?.contractNumber ?? body.contractNumber ?? '');
  expect(contractNumber, 'baseline contract number').toBeTruthy();
  const contractDetailType = String(body.basicParameters?.type ?? '');
  expect(contractDetailType, 'baseline contract detail type for MI column K').toBeTruthy();
  return {
    contractId,
    contractNumber,
    versionNumber: Number(body.basicParameters?.version ?? body.versions?.[0]?.version ?? 1),
    contractDetailType,
  };
}

/** Leave template cells blank so Phoenix uses existing contract values where the mapper falls back to DB. */
const MASS_IMPORT_CLEAR_ON_EDIT = [
  'contract_status',
  'contract_sub_status',
  'contract_version_type',
  'contract_version_status',
  'contract_entering_into_force',
  'contract_start_of_term',
  'contract_supply_activation_after_contract_resigning',
  'contract_entering_into_force_value',
  'contract_start_of_term_value',
  'contract_supply_activation_value',
] as const;

function clearMassImportPayloadFields(
  payload: Record<string, { value: unknown; cellNumber: string }>,
  keys: readonly string[],
): void {
  for (const key of keys) {
    if (payload[key]) {
      payload[key].value = '';
    }
  }
}

function buildRowPayload(row: ProductContractMiRow): Record<string, { value: unknown; cellNumber: string }> {
  const payload = productContractMassPayload() as Record<string, { value: unknown; cellNumber: string }>;

  payload.contract_customer_identifier.value = row.customerIdentifier;
  payload.contract_customer_version.value = row.customerVersion;
  payload.contract_product.value = row.productId;
  payload.contract_product_version.value = row.productVersion;
  payload.contract_pods.value = row.podIdentifier;
  payload.contract_applicable_interest_rate.value = row.interestRateName;

  if (row.kind === 'create') {
    payload.contract_number.value = '';
    payload.contract_version.value = '';
    payload.contract_create_edit_c_or_e.value = '';
    payload.contract_start_date.value = '';
    payload.contract_status.value = 'DRAFT';
    payload.contract_sub_status.value = 'DRAFT';
    payload.contract_type.value = 'CONTRACT';
    // First version must be SIGNED (ValidProductContractStatusOnCreate); DRAFT contract_status
    // keeps signing date empty — DRAFT status does not require a signing date (ProductContractDateService).
    payload.contract_version_status.value = 'SIGNED';
    payload.contract_signing_date.value = '';
    payload.contract_entering_into_force.value = 'SIGNING';
    payload.contract_start_of_term.value = 'SIGNING';
    payload.contract_supply_activation_after_contract_resigning.value = 'FIRST_DAY_OF_MONTH';
    clearMassImportPayloadFields(payload, [
      'contract_entering_into_force_value',
      'contract_start_of_term_value',
      'contract_supply_activation_after_contract_resigning_value',
      'contract_entry_into_force_date',
      'contract_term_start_date',
    ]);
    if (row.employeeIdentifier) {
      payload.contract_employee_identifier.value = row.employeeIdentifier;
    }
    return payload;
  }

  payload.contract_number.value = row.contractNumber;
  payload.contract_version.value = row.version;
  clearMassImportPayloadFields(payload, MASS_IMPORT_CLEAR_ON_EDIT);
  payload.contract_type.value = row.contractDetailType;
  // Do not re-send POD identifiers on edit — mapper would duplicate PODs on the contract.
  payload.contract_pods.value = '';

  if (row.kind === 'editCurrentVersion') {
    payload.contract_create_edit_c_or_e.value = 'E';
    payload.contract_start_date.value = '';
    if (row.marginalPrice != null) {
      payload.contract_marginal_price.value = Number(row.marginalPrice);
    }
    payload.contract_marginal_price_validity.value = '';
    return payload;
  }

  payload.contract_create_edit_c_or_e.value = 'C';
  payload.contract_start_date.value = row.startDate;
  payload.contract_version_status.value = 'SIGNED';
  return payload;
}

/** Mass-import routing columns must be blank for CREATE (Phoenix treats null A+B+C as create). */
const MI_ROUTING_COLUMNS = ['A', 'B', 'C', 'D'] as const;

/** Worksheet date/manual-value columns nulled on create rows before writing payload (stale template data). */
const CREATE_WORKSHEET_DATE_CLEAR_KEYS = [
  'contract_signing_date',
  'contract_entry_into_force_date',
  'contract_term_start_date',
  'contract_term_end_date',
  'contract_termination_date',
  'contract_prepetuity_date',
  'contract_entering_into_force_value',
  'contract_start_of_term_value',
  'contract_supply_activation_after_contract_resigning_value',
] as const;

const CREATE_COMBINED_FILE_TEMPLATE_CLEAR_KEYS = [
  'contract_entering_into_force',
  'contract_start_of_term',
  'contract_supply_activation_after_contract_resigning',
  ...CREATE_WORKSHEET_DATE_CLEAR_KEYS,
] as const;

/** Edit rows: wipe template MANUAL/date defaults so Phoenix falls back to existing contract values. */
const EDIT_WORKSHEET_TEMPLATE_CLEAR_KEYS = [
  ...MASS_IMPORT_CLEAR_ON_EDIT,
  ...CREATE_WORKSHEET_DATE_CLEAR_KEYS,
  'contract_marginal_price_validity',
  'contract_equal_monthly_installments_number',
  'contract_equal_monthly_installments_amount',
] as const;

function clearPayloadWorksheetCells(
  worksheet: ExcelJS.Worksheet,
  payload: Record<string, { value: unknown; cellNumber: string }>,
  excelRow: number,
  keys: readonly string[],
): void {
  for (const key of keys) {
    const field = payload[key];
    if (!field?.cellNumber) {
      continue;
    }
    const col = field.cellNumber.replace(/\d+$/, '');
    worksheet.getCell(`${col}${excelRow}`).value = null;
  }
}

function applyPayloadToWorksheetRow(
  worksheet: ExcelJS.Worksheet,
  payload: Record<string, { value: unknown; cellNumber: string }>,
  excelRow: number,
  options?: {
    clearRoutingColumns?: boolean;
    clearCreateDateCells?: boolean;
    clearCreateEnumCells?: boolean;
    clearEditTemplateCells?: boolean;
  },
): void {
  if (options?.clearRoutingColumns) {
    clearWorksheetCells(worksheet, excelRow, MI_ROUTING_COLUMNS);
  }
  if (options?.clearCreateDateCells) {
    clearPayloadWorksheetCells(worksheet, payload, excelRow, CREATE_WORKSHEET_DATE_CLEAR_KEYS);
  }
  if (options?.clearCreateEnumCells) {
    clearPayloadWorksheetCells(worksheet, payload, excelRow, CREATE_COMBINED_FILE_TEMPLATE_CLEAR_KEYS);
  }
  if (options?.clearEditTemplateCells) {
    clearPayloadWorksheetCells(worksheet, payload, excelRow, EDIT_WORKSHEET_TEMPLATE_CLEAR_KEYS);
  }
  const numericColumns = new Set(['F', 'G', 'H', 'B', 'BS', 'BR', 'BQ', 'BT']);
  const dateColumns = new Set(['D', 'N', 'O', 'P', 'Q', 'R', 'S', 'BK', 'BM']);
  for (const field of Object.values(payload)) {
    if (!field?.cellNumber) {
      continue;
    }
    const col = field.cellNumber.replace(/\d+$/, '');
    const value = field.value;
    if (value === undefined || value === null || value === '') {
      continue;
    }
    const cell = worksheet.getCell(`${col}${excelRow}`);
    if (typeof value === 'number') {
      cell.value = value;
      continue;
    }
    const asString = String(value);
    if (dateColumns.has(col) && /^\d{4}-\d{2}-\d{2}$/.test(asString)) {
      cell.value = new Date(`${asString}T12:00:00.000Z`);
    } else if (numericColumns.has(col) && /^-?\d+(\.\d+)?$/.test(asString)) {
      cell.value = Number(asString);
    } else {
      cell.value = value as ExcelJS.CellValue;
    }
  }
}

function clearWorksheetCells(worksheet: ExcelJS.Worksheet, excelRow: number, columns: readonly string[]): void {
  for (const col of columns) {
    worksheet.getCell(`${col}${excelRow}`).value = null;
  }
}

export async function buildProductContractMassImportWorkbook(
  Request: FixtureRequest,
  rows: ProductContractMiRow[],
): Promise<Buffer> {
  const templateRes = await Request.get(PDT_2931_TEMPLATE_URL);
  await expect(templateRes).CheckResponse();
  const templateBuffer = await templateRes.body();

  const outputDir = path.resolve(__dirname, '../../mass-imports/output');
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filePath = path.join(outputDir, `pdt-2931-pc-mi-${stamp}.xlsx`);
  fs.writeFileSync(filePath, templateBuffer);

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(filePath);
  const worksheet = workbook.getWorksheet(1);
  if (!worksheet) {
    throw new Error('Product contract mass import template has no worksheet');
  }

  rows.forEach((row, index) => {
    const excelRow = index + 2;
    const payload = buildRowPayload(row);
    applyPayloadToWorksheetRow(worksheet, payload, excelRow, {
      clearRoutingColumns: row.kind === 'create',
      clearCreateDateCells: row.kind === 'create',
      clearCreateEnumCells: row.kind === 'create',
      clearEditTemplateCells: row.kind !== 'create',
    });
  });

  await workbook.xlsx.writeFile(filePath);
  const buffer = fs.readFileSync(filePath);

  if (process.env.PDT_2931_KEEP_XLSX === '1') {
    const keepPath = path.join(outputDir, 'pdt-2931-last-upload.xlsx');
    fs.copyFileSync(filePath, keepPath);
  }

  try {
    fs.unlinkSync(filePath);
  } catch {
    /* ignore cleanup */
  }
  return buffer;
}

export async function uploadProductContractMassImportFile(
  FileUploadRequest: FixtureFileUpload,
  fileBuffer: Buffer,
  fileName = 'pdt-2931-product-contract-mass-import.xlsx',
): Promise<{ status: number; bodyText: string }> {
  const upload = await FileUploadRequest.post(PDT_2931_UPLOAD_URL, {
    multipart: {
      file: {
        name: fileName,
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: fileBuffer,
      },
    },
  });
  const bodyText = await upload.text();
  return { status: upload.status(), bodyText };
}

export async function listProductContractMassImportProcessIds(Request: FixtureRequest): Promise<number[]> {
  const res = await Request.get('process', {
    params: {
      page: 0,
      size: 50,
      sortBy: 'ID',
      sortDirection: 'DESC',
    },
  });
  if (!res.ok()) {
    return [];
  }
  const body = (await res.json()) as {
    content?: Array<{ id?: number; name?: string }>;
  };
  return (body.content ?? [])
    .filter((process) => String(process.name ?? '').startsWith('PRODUCT_CONTRACT_MASS_IMPORT'))
    .map((process) => Number(process.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

export async function resolveProductContractMassImportProcessId(
  Request: FixtureRequest,
  processIdsBeforeUpload: ReadonlySet<number>,
  maxAttempts = 60,
  delayMs = 2000,
): Promise<number> {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const processIdsNow = await listProductContractMassImportProcessIds(Request);
    const newProcessIds = processIdsNow.filter((id) => !processIdsBeforeUpload.has(id));
    if (newProcessIds.length > 0) {
      return Math.max(...newProcessIds);
    }

    const notification = await Request.get('notifications?size=30&page=0');
    if (notification.ok()) {
      const body = await notification.json();
      const notifications = Array.isArray(body?.content) ? body.content : [body];
      const notificationIds = notifications
        .filter(
          (n: { notificationType?: string; entityId?: number }) =>
            n?.notificationType && PRODUCT_CONTRACT_MI_NOTIFICATION.test(String(n.notificationType)),
        )
        .map((n: { entityId?: number }) => Number(n.entityId))
        .filter((id) => Number.isFinite(id) && id > 0 && !processIdsBeforeUpload.has(id));
      if (notificationIds.length > 0) {
        return Math.max(...notificationIds);
      }
    }

    if (attempt < maxAttempts - 1) {
      await sleep(delayMs);
    }
  }
  throw new Error('No new PRODUCT_CONTRACT_MASS_IMPORT process found after upload');
}

export async function pollProcessUntilCompleted(
  Request: FixtureRequest,
  processId: number,
  timeoutMs = 15 * 60 * 1000,
  intervalMs = 3000,
): Promise<{ status: string; processType?: string }> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const res = await Request.get(`process/${processId}`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as { status?: string; processType?: string };
    if (body.status === 'COMPLETED') {
      return { status: body.status, processType: body.processType };
    }
    if (body.status === 'CANCELED') {
      throw new Error(`Process ${processId} was CANCELED`);
    }
    await sleep(intervalMs);
  }
  throw new Error(`Process ${processId} did not reach COMPLETED within ${timeoutMs}ms`);
}

export async function downloadMassImportErrorReport(
  Request: FixtureRequest,
  processId: number,
): Promise<Buffer> {
  const reportRes = await Request.get(`process/${processId}/report/download`, {
    params: { multiSheetExcelType: 'MASS_IMPORT_ERROR_REPORT' },
  });
  await expect(reportRes).CheckResponse();
  return reportRes.body();
}

export function formatMassImportErrorReportForAttach(snapshot: MassImportErrorReportSnapshot): string {
  const lines = [
    `Headers: ${snapshot.headers.join(' | ')}`,
    `Failed row count: ${snapshot.failedRows.length}`,
  ];
  if (snapshot.failedRows.length === 0) {
    lines.push('Content: header row only — no failed import rows (all rows succeeded).');
  } else {
    snapshot.failedRows.forEach((row, index) => {
      lines.push(
        `  [${index + 1}] row_number=${row.rowNumber ?? ''} identifier=${row.identifier ?? ''} identifier_version=${row.identifierVersion ?? ''} errors=${row.errors ?? ''}`,
      );
    });
  }
  return lines.join('\n');
}

/**
 * Load and validate MASS_IMPORT_ERROR_REPORT Excel content.
 * Phoenix includes only failed rows (`ProcessedRecordInfo.success = false`), not one row per upload line.
 */
export async function loadMassImportErrorReportSnapshot(buffer: Buffer): Promise<MassImportErrorReportSnapshot> {
  expect(buffer.byteLength, 'mass import error report buffer must not be empty').toBeGreaterThan(0);

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buffer);
  const worksheet = workbook.worksheets[0];
  expect(worksheet, 'mass import error report must contain a worksheet').toBeTruthy();

  const headers = [1, 2, 3, 4].map((col) => cellText(worksheet!.getRow(1).getCell(col).value));
  expect(
    headers,
    'mass import error report header row must match Phoenix MASS_IMPORT_ERROR_REPORT schema',
  ).toEqual([...MASS_IMPORT_ERROR_REPORT_HEADERS]);

  const failedRows: MassImportReportRow[] = [];
  worksheet!.eachRow((row, rowNumber) => {
    if (rowNumber === 1) {
      return;
    }
    const rowNumberValue = row.getCell(1).value;
    const identifierValue = row.getCell(2).value;
    const identifierVersionValue = row.getCell(3).value;
    const errorsValue = row.getCell(4).value;

    const hasAnyContent = [rowNumberValue, identifierValue, identifierVersionValue, errorsValue].some(
      (value) => cellText(value) !== '',
    );
    if (!hasAnyContent) {
      return;
    }

    const errors = cellText(errorsValue);
    const rowNumberText = cellText(rowNumberValue);
    const rowNumberParsed =
      rowNumberText === ''
        ? null
        : Number.isFinite(Number(rowNumberText))
          ? Number(rowNumberText)
          : rowNumberText;
    failedRows.push({
      rowNumber: rowNumberParsed,
      identifier: cellText(identifierValue) || null,
      identifierVersion: cellText(identifierVersionValue) || null,
      errors: errors || null,
    });
  });

  const snapshot: MassImportErrorReportSnapshot = {
    headers,
    failedRows,
    summary:
      failedRows.length === 0
        ? 'MASS_IMPORT_ERROR_REPORT: header only, no failed rows.'
        : `MASS_IMPORT_ERROR_REPORT: ${failedRows.length} failed row(s): ${failedRows
            .map((row) => `[row ${row.rowNumber}] ${row.errors ?? '(empty errors column)'}`)
            .join(' | ')}`,
  };
  return snapshot;
}

/** @deprecated Prefer `loadMassImportErrorReportSnapshot` for content-aware assertions. */
export async function parseMassImportErrorReport(buffer: Buffer): Promise<MassImportReportRow[]> {
  const snapshot = await loadMassImportErrorReportSnapshot(buffer);
  return snapshot.failedRows;
}

export function assertNoRiskListInReport(rows: MassImportReportRow[]): void {
  const combined = rows.map((r) => r.errors ?? '').join('\n');
  expect(combined).not.toContain(RISK_LIST_DECISION_SNIPPET);
  expect(combined.toLowerCase()).not.toContain('risk assessment restriction');
}

export function assertRiskListFailureInReport(rows: MassImportReportRow[]): void {
  expect(rows.length, 'expected at least one failed import row in error report content').toBeGreaterThan(0);
  for (const row of rows) {
    expect(
      row.errors?.trim(),
      `failed report row ${row.rowNumber} must have non-empty errors column content`,
    ).toBeTruthy();
  }
  const combined = rows.map((r) => r.errors ?? '').join('\n');
  expect(combined, 'error report must contain RISK_LIST_DECISION').toContain(RISK_LIST_DECISION_SNIPPET);
  expect(combined, 'error report must contain risk assessment restriction text').toContain(
    RISK_ASSESSMENT_RESTRICTION_SNIPPET,
  );
}

export function assertPositiveMassImportReport(rows: MassImportReportRow[]): void {
  assertPositiveSkipRiskListMassImportReport(rows);
}

/**
 * Positive mass import: error report must be header-only (no failed rows with error text).
 * Phoenix writes one data row per failed import row — not one row per uploaded Excel line.
 */
export function assertMassImportErrorReportHasNoFailures(snapshot: MassImportErrorReportSnapshot): void {
  expect(
    snapshot.failedRows.length,
    `mass import error report must contain no failed rows; ${snapshot.summary}`,
  ).toBe(0);

  for (const row of snapshot.failedRows) {
    const errorText = row.errors?.trim() ?? '';
    if (!errorText) {
      continue;
    }
    const failureMessage = isRiskListMassImportDefect(errorText)
      ? `${PDT_2931_BACKEND_DEFECT_RISK_LIST_MSG}. Report errors: ${errorText}`
      : `unexpected error report row ${row.rowNumber}: ${errorText}`;
    expect(errorText, failureMessage).toBe('');
  }
  assertNoRiskListInReport(snapshot.failedRows);
}

/** Negative mass import: error report must list failed rows with populated errors column. */
export function assertMassImportErrorReportListsFailures(snapshot: MassImportErrorReportSnapshot): void {
  expect(
    snapshot.failedRows.length,
    `mass import error report must list at least one failed row; ${snapshot.summary}`,
  ).toBeGreaterThan(0);
  for (const row of snapshot.failedRows) {
    expect(
      row.errors?.trim(),
      `failed report row ${row.rowNumber} errors column must contain the backend error message`,
    ).toBeTruthy();
  }
}

/** Positive PDT-2931 rows must succeed and must not show Risk List consultation in the error report. */
export function assertPositiveSkipRiskListMassImportReport(rows: MassImportReportRow[]): void {
  if (rows.length > 0) {
    const combined = rows.map((r) => r.errors ?? '').join('\n');
    if (isRiskListMassImportDefect(combined)) {
      expect(
        rows.length,
        `${PDT_2931_BACKEND_DEFECT_RISK_LIST_MSG}. Report errors: ${combined}`,
      ).toBe(0);
    } else {
      expect(rows.length, `mass import error report must have no failed rows. Errors: ${combined}`).toBe(0);
    }
  }
  assertNoRiskListInReport(rows);
}

export function assertPositiveSkipRiskListMassImportReportSnapshot(snapshot: MassImportErrorReportSnapshot): void {
  assertMassImportErrorReportHasNoFailures(snapshot);
}

export async function countProductContractsForCustomer(
  Request: FixtureRequest,
  customerIdentifier: string,
  customerId?: number,
  customerNumber?: string,
): Promise<number> {
  const prompts = [customerIdentifier, customerNumber ? String(customerNumber) : ''].filter(Boolean);
  const searchModes: Array<{ searchBy?: string }> = [
    { searchBy: 'CUSTOMER_UIC_OR_PERSONAL_NUMBER' },
    { searchBy: 'ALL' },
    {},
  ];

  for (const prompt of prompts) {
    for (const mode of searchModes) {
      const res = await Request.post('product-contract/list', {
        data: { page: 0, size: 100, prompt, ...mode },
      });
      if (!res.ok()) {
        continue;
      }
      const body = (await res.json()) as {
        totalElements?: number;
        content?: { customerId?: number }[];
      };
      if (customerId != null && Array.isArray(body.content)) {
        const matched = body.content.filter((c) => Number(c.customerId) === customerId).length;
        if (matched > 0) {
          return matched;
        }
      }
      if (typeof body.totalElements === 'number' && body.totalElements > 0) {
        return body.totalElements;
      }
    }
  }
  return 0;
}

export async function waitForProductContractCount(
  Request: FixtureRequest,
  customerIdentifier: string,
  customerId: number,
  minCount: number,
  customerNumber?: string,
  timeoutMs = 120_000,
): Promise<number> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const count = await countProductContractsForCustomer(
      Request,
      customerIdentifier,
      customerId,
      customerNumber,
    );
    if (count >= minCount) {
      return count;
    }
    await sleep(2000);
  }
  return countProductContractsForCustomer(Request, customerIdentifier, customerId, customerNumber);
}

export async function runProductContractMassImportFlow(
  Request: FixtureRequest,
  FileUploadRequest: FixtureFileUpload,
  rows: ProductContractMiRow[],
): Promise<{
  processId: number;
  reportRows: MassImportReportRow[];
  reportSnapshot: MassImportErrorReportSnapshot;
  uploadStatus: number;
}> {
  const fileBuffer = await buildProductContractMassImportWorkbook(Request, rows);
  const processIdsBeforeUpload = new Set(await listProductContractMassImportProcessIds(Request));
  const upload = await uploadProductContractMassImportFile(FileUploadRequest, fileBuffer);
  expect(upload.status, 'mass import upload must be accepted').toBe(202);

  const processId = await resolveProductContractMassImportProcessId(Request, processIdsBeforeUpload);
  const process = await pollProcessUntilCompleted(Request, processId);
  expect(process.status).toBe('COMPLETED');

  const reportBuffer = await downloadMassImportErrorReport(Request, processId);
  const reportSnapshot = await loadMassImportErrorReportSnapshot(reportBuffer);
  assertMassImportErrorReportHasNoFailures(reportSnapshot);

  test.info().attach(`[PDT-2931] process ${processId} — MASS_IMPORT_ERROR_REPORT content`, {
    body: formatMassImportErrorReportForAttach(reportSnapshot),
    contentType: 'text/plain',
  });

  return {
    processId,
    reportRows: reportSnapshot.failedRows,
    reportSnapshot,
    uploadStatus: upload.status,
  };
}

export async function loadProductContractVersionCount(
  Request: FixtureRequest,
  contractId: number,
): Promise<number> {
  const res = await Request.get(`product-contract/${contractId}`);
  await expect(res).CheckResponse();
  const body = await res.json();
  return (body.versions ?? []).length;
}

// ─── Portal links (open in Dev UI after each test) ───────────────────────────

export type Pdt2931PortalContext = {
  testCase: string;
  /** Mass import process id — primary object for MI tests */
  processId?: number;
  processType?: string;
  customerId?: number;
  customerIdentifier?: string;
  customerNumber?: string;
  podId?: number;
  podIdentifier?: string;
  productId?: number;
  baselineContractId?: number;
  baselineContractNumber?: string;
  directContractId?: number;
  note?: string;
};

function resolvePdt2931PortalFrontendBaseUrl(): string | null {
  const env = process.env.FRONTEND_BASE_URL?.trim();
  if (env) {
    return env.endsWith('/') ? env : `${env}/`;
  }

  const raw = (configuredBaseURL ?? process.env.BASE_URL ?? 'http://10.236.20.11:8091/').replace(/\/$/, '');

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
    if (raw === api) {
      return fe;
    }
  }
  const lab = raw.match(/^http:\/\/(10\.236\.20\.\d+):8091$/);
  if (lab) {
    return `http://${lab[1]}:8080/`;
  }
  return null;
}

function lastResponseEntityId(bucket: unknown): number | undefined {
  if (!Array.isArray(bucket) || bucket.length === 0) {
    return undefined;
  }
  const item = bucket[bucket.length - 1];
  if (typeof item === 'number' && Number.isFinite(item)) {
    return item;
  }
  if (item && typeof item === 'object' && 'id' in item) {
    const n = Number((item as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  return undefined;
}

export function buildPdt2931PortalLinks(
  Responses: baseFixture['Responses'],
  context: Pdt2931PortalContext,
): Record<string, string[]> {
  const fe = resolvePdt2931PortalFrontendBaseUrl();
  const links: Record<string, string[]> = { ...ResponseLinker.setLinksToResponses(Responses) };

  const push = (key: string, url: string) => {
    if (!links[key]) {
      links[key] = [];
    }
    if (!links[key].includes(url)) {
      links[key].push(url);
    }
  };

  if (fe) {
    const base = fe.endsWith('/') ? fe : `${fe}/`;

    if (context.processId != null && context.processId > 0) {
      push('process', `${base}processes/preview?id=${context.processId}`);
    }

    const customerId = context.customerId ?? lastResponseEntityId(Responses.customer);
    if (customerId) {
      push('customer', `${base}customers/preview/basic?id=${customerId}`);
    }

    const podId = context.podId ?? lastResponseEntityId(Responses.pod);
    if (podId) {
      push('pod', `${base}points-of-delivery/preview?id=${podId}`);
    }

    const productId = context.productId ?? lastResponseEntityId(Responses.product);
    if (productId) {
      push('product', `${base}energy-products/preview?id=${productId}`);
    }

    const baselineId = context.baselineContractId ?? lastResponseEntityId(Responses.productContract);
    if (baselineId) {
      push('productContract', `${base}energy-product-contracts/preview?id=${baselineId}`);
    }

    if (context.directContractId != null && context.directContractId > 0) {
      push(
        'productContractDirect',
        `${base}energy-product-contracts/preview?id=${context.directContractId}`,
      );
    }
  }

  return links;
}

function formatPdt2931PortalLinksPlain(
  links: Record<string, string[]>,
  snapshot: Record<string, unknown>,
): string {
  const lines: string[] = [
    `========== [PDT-2931] ${snapshot.testCase} — portal links ==========`,
    '',
    '--- Entity snapshot ---',
    JSON.stringify(snapshot, null, 2),
    '',
    '--- Open in Dev portal ---',
  ];

  const order = ['process', 'customer', 'pod', 'product', 'productContract', 'productContractDirect'];

  if (Object.keys(links).length === 0) {
    lines.push(
      '(no portal URLs — set FRONTEND_BASE_URL or run against mapped Dev API base)',
      `API base: ${String(snapshot.apiBase ?? '')}`,
    );
  } else {
    for (const key of order) {
      const urls = links[key];
      if (!urls?.length) {
        continue;
      }
      lines.push('', `[${key}]`);
      for (const url of urls) {
        lines.push(url);
      }
    }
    for (const [key, urls] of Object.entries(links)) {
      if (order.includes(key) || !urls.length) {
        continue;
      }
      lines.push('', `[${key}]`);
      for (const url of urls) {
        lines.push(url);
      }
    }
  }

  lines.push(
    '',
    '--- Tips ---',
    'Process link = mass import run (Processes).',
    'Playwright report → test → Attachments → plain text block below.',
    '================================================================',
  );
  return lines.join('\n');
}

/** Log + attach Dev portal URLs after each test (process + created entities). */
export function attachPdt2931PortalLinks(
  Responses: baseFixture['Responses'],
  context: Pdt2931PortalContext,
): Record<string, string[]> {
  const apiBase = configuredBaseURL ?? process.env.BASE_URL ?? 'http://10.236.20.11:8091/';
  const portalLinks = buildPdt2931PortalLinks(Responses, context);

  const snapshot: Record<string, unknown> = {
    testCase: context.testCase,
    apiBase,
    frontendBase: resolvePdt2931PortalFrontendBaseUrl(),
    processId: context.processId,
    processType: context.processType ?? 'PRODUCT_CONTRACT_MASS_IMPORT',
    customerId: context.customerId ?? lastResponseEntityId(Responses.customer),
    customerIdentifier: context.customerIdentifier,
    customerNumber: context.customerNumber,
    podId: context.podId ?? lastResponseEntityId(Responses.pod),
    podIdentifier: context.podIdentifier,
    productId: context.productId ?? lastResponseEntityId(Responses.product),
    baselineContractId: context.baselineContractId ?? lastResponseEntityId(Responses.productContract),
    baselineContractNumber: context.baselineContractNumber,
    directContractId: context.directContractId,
    note: context.note,
  };

  const plain = formatPdt2931PortalLinksPlain(portalLinks, snapshot);
  console.log(`\n${plain}\n`);

  test.info().attach(`[PDT-2931] ${context.testCase} — portal links`, {
    body: plain,
    contentType: 'text/plain; charset=utf-8',
  });
  test.info().attach(`[PDT-2931] ${context.testCase} — portal links (JSON)`, {
    body: JSON.stringify(portalLinks, null, 2),
    contentType: 'application/json',
  });

  return portalLinks;
}

export function portalContextFromChain(
  chain: ProductContractChain,
  extra?: Partial<Pdt2931PortalContext>,
): Partial<Pdt2931PortalContext> {
  return {
    customerId: chain.riskCustomer.customerId,
    customerIdentifier: chain.riskCustomer.identifier,
    customerNumber: chain.riskCustomer.customerNumber,
    podIdentifier: chain.podIdentifier,
    productId: chain.productId,
    ...extra,
  };
}
