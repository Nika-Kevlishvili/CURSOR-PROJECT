/**
 * PDT-2906 — Product new version applied to existing contracts (fixed-parameter product).
 *
 * Reference spec(s):
 * - tests/salesPortal/getProductList.spec.ts (~2385 — fixed-parameter product payload)
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (contract v2 + edit helpers)
 * - tests/cursor/PDT-2854-pod-active-two-contracts.spec.ts (contract lifecycle)
 */

import { expect } from '@playwright/test';
import { expect as baseExpect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import type { ProductPayload } from '../../jsons/payloads/create/productAndServices/product';
import {
  ANCHOR_DATE,
  activatePodOnContract,
  anchorDateIso,
  buildEditProductContractPayload,
  headerContractStatus,
  loadProductContract,
  productContractStatusUpdate,
  putProductContractNewVersion,
} from './pdt-2815-version-validity.fixtures';

type FixtureRequest = baseFixture['Request'];

export function entityId(entry: unknown): number {
  if (typeof entry === 'number' && Number.isFinite(entry) && entry > 0) {
    return entry;
  }
  if (entry !== null && typeof entry === 'object' && 'id' in entry) {
    const id = Number((entry as { id: unknown }).id);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  throw new Error(`Cannot resolve entity id from: ${JSON.stringify(entry).slice(0, 200)}`);
}

/** Narrow product to fixed-parameter eligibility (ProductRelatedContractUpdateService). */
export function applyFixedParameterProductFields(payload: ProductPayload): void {
  payload.productStatus = 'ACTIVE';
  payload.availableForSale = true;
  payload.globalSalesChannel = true;
  payload.globalSalesArea = true;
  payload.globalSegment = true;
  payload.isIndividual = false;
  payload.customerIdentifier = null;
  payload.contractTypes = ['COMBINED'];
  payload.paymentGuarantees = ['NO'];
  payload.typePointsOfDelivery = ['CONSUMER'];
  payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
  payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
  payload.voltageLevels = ['LOW'];
  payload.capacityLimitAmount = 5000;
  payload.capacityLimitType = 'TO';
  payload.equalMonthlyInstallmentsActivation = false;
  payload.productTerms = [
    {
      typeOfTerms: 'PERIOD',
      value: '12',
      periodType: 'DAY_DAYS',
      renewalPeriodValue: null,
      renewalPeriodType: null,
      perpetuityCause: false,
      automaticRenewal: null,
      numberOfRenewals: null,
      name: '12 Day/Days Period',
      id: null,
    },
  ];
}

export function applyFixedParameterTermFields(
  termPayload: ReturnType<baseFixture['GeneratePayload']['productAndServices']['term']>,
): void {
  termPayload.contractEntryIntoForces = ['SIGNING'];
  termPayload.startsOfContractInitialTerms = ['SIGNING'];
  termPayload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
  termPayload.waitForOldContractTermToExpires = ['NO'];
}

export type ProductDetailSnapshot = {
  productId: number;
  detailId: number;
  version: number;
};

export async function loadProductDetailSnapshot(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  productId: number,
  version: number,
): Promise<ProductDetailSnapshot> {
  const res = await Request.get(`${Endpoints.product}/${productId}?version=${version}`);
  await baseExpect(res).CheckResponse();
  const body = (await res.json()) as { id?: number; detailsId?: number; version?: number };
  const resolvedProductId = Number(body.id);
  // ProductDetailResponse.id = product id; detailsId = product_details row (contract productDetailId).
  const detailId = Number(body.detailsId ?? body.id);
  const logicalVersion = Number(body.version);
  expect(Number.isFinite(resolvedProductId) && resolvedProductId > 0, 'product id').toBeTruthy();
  expect(Number.isFinite(detailId) && detailId > 0, 'product detailsId').toBeTruthy();
  expect(Number.isFinite(logicalVersion) && logicalVersion > 0, 'product version').toBeTruthy();
  return { productId: resolvedProductId, detailId, version: logicalVersion };
}

export type ContractProductRef = {
  productId: number;
  productDetailId: number;
  productVersionId: number;
};

/**
 * Load product refs for a specific contract version.
 * GET /product-contract/{id} query param is `versionId` (Swagger / ProductContractController).
 * Do not use `?version=` here — Spring ignores it and returns the default/current version.
 * Product GETs remain `GET /products/{id}?version=` (different controller).
 */
export async function loadContractVersionProductRef(
  Request: FixtureRequest,
  contractId: number,
  versionId: number,
): Promise<ContractProductRef> {
  const res = await Request.get(`product-contract/${contractId}?versionId=${versionId}`);
  await baseExpect(res).CheckResponse();
  const body = (await res.json()) as {
    basicParameters?: {
      productId?: number;
      productDetailId?: number;
      productVersionId?: number;
    };
  };
  const bp = body.basicParameters ?? {};
  const productId = Number(bp.productId);
  const productDetailId = Number(bp.productDetailId);
  const productVersionId = Number(bp.productVersionId);
  expect(Number.isFinite(productId) && productId > 0, 'contract productId').toBeTruthy();
  expect(Number.isFinite(productDetailId) && productDetailId > 0, 'contract productDetailId').toBeTruthy();
  expect(Number.isFinite(productVersionId) && productVersionId > 0, 'contract productVersionId').toBeTruthy();
  return { productId, productDetailId, productVersionId };
}

export async function createSignedProductContractV1(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<{ contractId: number; contractPayload: Record<string, unknown> }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract()) as Record<
    string,
    unknown
  >;
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
  await baseExpect(res).CheckResponse();
  const body = await res.json();
  Responses.productContract.push(body);
  const contractId = entityId(body);

  const ready = await productContractStatusUpdate(Request, contractId, 'READY', 'READY', 1);
  await baseExpect(ready).CheckResponse();
  const signed = await productContractStatusUpdate(
    Request,
    contractId,
    'SIGNED',
    'SIGNED_BY_BOTH_SIDES',
    1,
  );
  await baseExpect(signed).CheckResponse();

  const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
    savingAsNewVersion: false,
    preserveSigningDate: false,
  });
  const editBp = editPayload.basicParameters as Record<string, unknown>;
  if (editBp.signingDate == null || editBp.signingDate === '') {
    editBp.signingDate = anchorDateIso();
  }
  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await baseExpect(put).CheckResponse();

  const afterSign = await loadProductContract(Request, contractId);
  const headerSigning = ((afterSign.basicParameters ?? {}) as { signingDate?: string | null })
    .signingDate;
  const v1Signing = (
    (afterSign.versions ?? []) as { versionId?: number; signingDate?: string | null }[]
  ).find((v) => Number(v.versionId) === 1)?.signingDate;
  expect(
    (headerSigning != null && String(headerSigning).trim() !== '') ||
      (v1Signing != null && String(v1Signing).trim() !== ''),
    `contract ${contractId} v1 basicParameters.signingDate must be non-null after SIGNED PUT`,
  ).toBe(true);

  return { contractId, contractPayload };
}

export async function createSignedProductContractV2StillOnProductV1(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  contractId: number,
  contractPayload: Record<string, unknown>,
): Promise<void> {
  await putProductContractNewVersion(Request, Endpoints, contractId, contractPayload, 1, {
    startDate: ANCHOR_DATE,
    savingAsNewVersion: true,
    versionStatus: 'SIGNED',
  });
}

export async function buildProductNewVersionEditPayload(
  GeneratePayload: baseFixture['GeneratePayload'],
  productIndex: number,
): Promise<ProductPayload & { updateExistingVersion: boolean; version: number }> {
  const template = GeneratePayload.productAndServices.product(productIndex);
  applyFixedParameterProductFields(template);
  template.shortDescription = `${template.shortDescription ?? 'PDT-2906'} — v2 edit`;

  const editPayload = await GeneratePayload.productAndServices.edit_Product(template, productIndex);
  applyFixedParameterProductFields(editPayload);
  editPayload.updateExistingVersion = false;
  editPayload.productDetailIdsForUpdatingProductContracts = [];
  return editPayload;
}

/** Price component with expression-only filled formula (41260-style add-PC-before-new-version). */
export function buildPriceComponentWithFilledFormulaValuePayload(
  GeneratePayload: baseFixture['GeneratePayload'],
): ReturnType<baseFixture['GeneratePayload']['productAndServices']['priceSettlement']> {
  const payload = GeneratePayload.productAndServices.priceSettlement();
  payload.displayName = `PDT-2906 filled PC ${payload.displayName}`;
  payload.formulaRequest.expression = '500';
  payload.formulaRequest.variables = [];
  return payload;
}

/** Price component with undefined formula variable (PHN-2130 / video flow). */
export function buildPriceComponentWithUndefinedFormulaValuePayload(
  GeneratePayload: baseFixture['GeneratePayload'],
): ReturnType<baseFixture['GeneratePayload']['productAndServices']['electricity']> {
  const payload = GeneratePayload.productAndServices.electricity();
  payload.formulaRequest.expression = '$X1$';
  payload.formulaRequest.variables = [
    {
      variable: 'X1',
      description: 'PDT-2906 unfilled formula variable',
      value: null,
    } as unknown as { name: string; value: string | number },
  ];
  return payload;
}

export const PDT_2906_PRICE_COMPONENT_VALUE_NOT_DEFINED_PATTERN =
  /price component|formula|value.*not defined|not defined/i;

/** Case 2780 — duplicate key on product new version save with contract apply (Slack July 7). */
export const PDT_2906_DUPLICATE_KEY_CONSTRAINT_PATTERN =
  /idx_product_contract_price_components|duplicate key|already exists|price_component_formula_variable_id/i;

/**
 * Product new-version edit that changes display name only (Valeri screenshot / case 8828-42753).
 * Keeps v1 price components unchanged; does not add PCs.
 */
export async function buildProductNewVersionNameOnlyEditPayload(
  GeneratePayload: baseFixture['GeneratePayload'],
  productIndex: number,
  nameSuffix = ' — Valeri name edit v2',
): Promise<ProductPayload & { updateExistingVersion: boolean; version: number }> {
  const template = GeneratePayload.productAndServices.product(productIndex);
  applyFixedParameterProductFields(template);
  const baseName = String(template.name ?? template.shortDescription ?? 'PDT-2906 product');

  const editPayload = await GeneratePayload.productAndServices.edit_Product(template, productIndex);
  applyFixedParameterProductFields(editPayload);
  editPayload.name = `${baseName}${nameSuffix}`;
  editPayload.shortDescription = editPayload.name;
  editPayload.updateExistingVersion = false;
  editPayload.productDetailIdsForUpdatingProductContracts = [];
  editPayload.priceComponentIds = editPayload.priceComponentIds.map((entry) => entityId(entry));
  return editPayload;
}

/** Product new-version edit keeping the same v1 price component IDs (case 2780 duplicate-key path). */
export async function buildProductNewVersionEditPayloadSamePriceComponents(
  GeneratePayload: baseFixture['GeneratePayload'],
  productIndex: number,
): Promise<ProductPayload & { updateExistingVersion: boolean; version: number }> {
  const editPayload = await buildProductNewVersionEditPayload(GeneratePayload, productIndex);
  editPayload.priceComponentIds = editPayload.priceComponentIds.map((entry) => entityId(entry));
  return editPayload;
}

/**
 * Product new-version edit payload including every price component currently in Responses
 * (original v1 PC + any PCs added after product create).
 */
export async function buildProductNewVersionEditPayloadWithAddedPriceComponents(
  GeneratePayload: baseFixture['GeneratePayload'],
  productIndex: number,
): Promise<ProductPayload & { updateExistingVersion: boolean; version: number }> {
  const editPayload = await buildProductNewVersionEditPayload(GeneratePayload, productIndex);
  editPayload.priceComponentIds = editPayload.priceComponentIds.map((entry) => entityId(entry));
  return editPayload;
}

export type ValidateProductRelatedContractUpdateResult = {
  status: number;
  eligible: boolean;
  bodyText: string;
};

export async function postValidateProductRelatedContractUpdate(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  editPayload: ProductPayload,
): Promise<ValidateProductRelatedContractUpdateResult> {
  const validateRes = await Request.post(
    `${Endpoints.product}/validate-product-related-contract-update`,
    { data: editPayload },
  );
  const bodyText = await validateRes.text();
  let eligible = false;
  try {
    eligible = JSON.parse(bodyText) === true;
  } catch {
    eligible = false;
  }
  return { status: validateRes.status(), eligible, bodyText };
}

export type PutProductNewVersionResult = {
  status: number;
  bodyText: string;
};

export async function putProductNewVersionWithContractDetailIds(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  productId: number,
  editPayload: ProductPayload,
  productDetailIds: number[],
): Promise<PutProductNewVersionResult> {
  editPayload.productDetailIdsForUpdatingProductContracts = productDetailIds;
  const putRes = await Request.put(`${Endpoints.product}/${productId}`, { data: editPayload });
  const bodyText = await putRes.text();
  return { status: putRes.status(), bodyText };
}

/** PUT product new version from a pre-built edit payload (validate + apply). */
export async function applyProductNewVersionFromEditPayload(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  productId: number,
  editPayload: ProductPayload,
  productV1DetailId: number,
): Promise<ProductDetailSnapshot> {
  editPayload.productDetailIdsForUpdatingProductContracts = [productV1DetailId];
  expect(editPayload.updateExistingVersion, 'must create product v2, not overwrite v1').toBe(false);
  expect(editPayload.version, 'edit base must be product logical version 1').toBe(1);

  const validate = await postValidateProductRelatedContractUpdate(Request, Endpoints, editPayload);
  expect(validate.status).toBeGreaterThanOrEqual(200);
  expect(validate.status).toBeLessThan(300);
  expect(validate.eligible, 'fixed-parameter product must be eligible for contract update popup').toBe(
    true,
  );

  const putRes = await Request.put(`${Endpoints.product}/${productId}`, { data: editPayload });
  await baseExpect(putRes).CheckResponse();

  const productV2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);
  expect(productV2.version).toBe(2);
  expect(productV2.detailId, 'product v2 detailId must differ from v1').not.toBe(productV1DetailId);
  return productV2;
}

/** POST validate + PUT product new version with popup apply to selected contract product details. */
export async function applyProductNewVersionToRelatedContracts(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  productIndex: number,
  productId: number,
  productV1DetailId: number,
  Endpoints: baseFixture['Endpoints'],
): Promise<ProductDetailSnapshot> {
  const editPayload = await buildProductNewVersionEditPayload(GeneratePayload, productIndex);
  editPayload.productDetailIdsForUpdatingProductContracts = [productV1DetailId];
  expect(editPayload.updateExistingVersion, 'must create product v2, not overwrite v1').toBe(false);
  expect(editPayload.version, 'edit base must be product logical version 1').toBe(1);

  const validateRes = await Request.post(
    `${Endpoints.product}/validate-product-related-contract-update`,
    { data: editPayload },
  );
  await baseExpect(validateRes).CheckResponse();
  const eligible = (await validateRes.json()) as boolean;
  expect(eligible, 'fixed-parameter product must be eligible for contract update popup').toBe(true);

  const putRes = await Request.put(`${Endpoints.product}/${productId}`, { data: editPayload });
  await baseExpect(putRes).CheckResponse();

  const productV2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);
  expect(productV2.version).toBe(2);
  expect(productV2.detailId, 'product v2 detailId must differ from v1').not.toBe(productV1DetailId);
  return productV2;
}

export async function assertContractVersionProductMatches(
  Request: FixtureRequest,
  contractId: number,
  logicalVersion: number,
  expected: { productDetailId: number; productVersionId: number },
  context: string,
): Promise<void> {
  const ref = await loadContractVersionProductRef(Request, contractId, logicalVersion);
  expect(ref.productDetailId, `${context} productDetailId`).toBe(expected.productDetailId);
  expect(ref.productVersionId, `${context} productVersionId`).toBe(expected.productVersionId);
}

/** First calendar day of the previous month (UTC) — triggers async path when != today. */
export function firstDayOfPreviousCalendarMonthIso(reference = new Date()): string {
  const d = new Date(Date.UTC(reference.getUTCFullYear(), reference.getUTCMonth() - 1, 1));
  return d.toISOString().split('T')[0];
}

export function contractHasLogicalVersion(body: Record<string, unknown>, logicalVersion: number): boolean {
  const versions = (body.versions ?? []) as { versionId?: number }[];
  return versions.some((v) => Number(v.versionId) === logicalVersion);
}

export function maxContractLogicalVersion(body: Record<string, unknown>): number {
  const versions = (body.versions ?? []) as { versionId?: number }[];
  return versions.reduce((max, v) => Math.max(max, Number(v.versionId ?? 0)), 0);
}

export function assertProductContractHeaderInForce(
  contractId: number,
  body: Record<string, unknown>,
): string {
  const headerSt = headerContractStatus(body);
  expect(
    headerSt === 'ACTIVE_IN_TERM' ||
      headerSt === 'ACTIVE_IN_PERPETUITY' ||
      headerSt === 'ENTERED_INTO_FORCE',
    `product contract ${contractId} should be in force after POD activation (got ${headerSt})`,
  ).toBeTruthy();
  return headerSt!;
}

/**
 * Valeri async path — ACTIVE_IN_TERM v1 with start_date in the past (not today) and POD activated.
 * Skips manual contract v2; product apply should spawn v2 via ProductContractCreateNewVersionEvent.
 */
export async function createSignedProductContractV1WithPastStartDate(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  pastStartDate = firstDayOfPreviousCalendarMonthIso(),
): Promise<{
  contractId: number;
  contractPayload: Record<string, unknown>;
  pastStartDate: string;
  productV1DetailId: number;
  headerStatus: string;
}> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract()) as Record<
    string,
    unknown
  >;
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
  await baseExpect(res).CheckResponse();
  const body = await res.json();
  Responses.productContract.push(body);
  const contractId = entityId(body);

  const ready = await productContractStatusUpdate(Request, contractId, 'READY', 'READY', 1);
  await baseExpect(ready).CheckResponse();
  const signed = await productContractStatusUpdate(
    Request,
    contractId,
    'SIGNED',
    'SIGNED_BY_BOTH_SIDES',
    1,
  );
  await baseExpect(signed).CheckResponse();

  const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
    savingAsNewVersion: false,
    startDate: pastStartDate,
    preserveSigningDate: false,
  });
  (editPayload.basicParameters as Record<string, unknown>).signingDate = pastStartDate;

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await baseExpect(put).CheckResponse();

  const contractBody = await loadProductContract(Request, contractId);
  const versions = (contractBody.versions ?? []) as { versionId?: number; startDate?: string }[];
  const v1 = versions.find((v) => Number(v.versionId) === 1);
  expect(String(v1?.startDate ?? '').slice(0, 10), 'contract v1 startDate').toBe(pastStartDate);
  expect(pastStartDate, 'past start must differ from today for async event path').not.toBe(
    new Date().toISOString().split('T')[0],
  );

  await activatePodOnContract(Request, Responses, contractId, 1, pastStartDate, 0);

  const contractBodyAfterActivation = await loadProductContract(Request, contractId);
  const headerStatus = assertProductContractHeaderInForce(contractId, contractBodyAfterActivation);

  const v1ProductRef = await loadContractVersionProductRef(Request, contractId, 1);

  return {
    contractId,
    contractPayload,
    pastStartDate,
    productV1DetailId: v1ProductRef.productDetailId,
    headerStatus,
  };
}

/** Poll until async handler creates the requested logical contract version (max 120s default). */
export async function pollUntilContractLogicalVersion(
  Request: FixtureRequest,
  contractId: number,
  logicalVersion: number,
  timeoutMs = 120_000,
): Promise<Record<string, unknown>> {
  let lastBody: Record<string, unknown> = {};

  await expect
    .poll(
      async () => {
        lastBody = await loadProductContract(Request, contractId);
        return contractHasLogicalVersion(lastBody, logicalVersion);
      },
      {
        message: `PDT-2906 Valeri async — contract ${contractId} logical version ${logicalVersion}`,
        timeout: timeoutMs,
        intervals: [2000, 3000, 5000],
      },
    )
    .toBe(true);

  return lastBody;
}

export { loadProductContract };
