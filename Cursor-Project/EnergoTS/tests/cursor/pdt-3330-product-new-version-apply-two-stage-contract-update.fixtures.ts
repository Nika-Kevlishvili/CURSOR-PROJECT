/**
 * PDT-3330 — Two-stage apply when creating a product new version
 * (newContractsVersionsStartDate = D).
 *
 * PDT-3454 supersedes parent AC-7 when Open End startDate < D: CREATE +1.
 * PDT-3330 CREATE tests: after PUT, one immediate GET /product-contract/{id}; no wait/poll.
 * PUT 200 is not CREATE. Remaining AC-7 replace applies only when Open End startDate ≥ D
 * (e.g. TC-BE-17 Start = D). pollUntilContractVersionStartDate stays exported for PDT-3454.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3454-open-end-contract-new-version.spec.ts (CREATE +1)
 * - tests/cursor/pdt-2906-product-new-version-contract-update.fixtures.ts
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts
 * - tests/cursor/PDT-2906-product-new-version-contract-update.spec.ts
 * - tests/cursor/contract-resign-new-flow.fixtures.ts (SIGNED on create → date chaining)
 *
 * Note: TC docs cite GET /product-contract/contract-version/{id}, but Swagger/runtime
 * that path is ContractVersionTypes nomenclature. Version dates/product refs come from
 * GET /product-contract/{id}?versionId={n} (ProductContractController). Products use
 * GET /products/{id}?version={n} — different query name; do not mix them.
 *
 * End-date chaining (SIGNED only): createFollowingContractVersion PUT with
 * basicParameters.versionStatus=SIGNED so CalculateVersionDates runs. Shared
 * pdt-2815 putProductContractNewVersion creates DRAFT then status-updates and does NOT chain.
 *
 * Draft limitations: SIGNED↔DRAFT / Draft→SIGNED for date chaining is not a valid setup
 * path. DRAFT createFollowingContractVersion creates the following row as DRAFT on PUT
 * (no promote/demote, no CalculateVersionDates). End=D / End<D skip for Not Valid is
 * covered by SIGNED Valid TCs (TC-BE-1 / TC-BE-2), not Draft demotion.
 * TC-BE-6 Valid two Open End: following is created DRAFT on PUT so prior stays Open End,
 * then only that row is promoted READY→SIGNED via status-update (does not run
 * CalculateVersionDates). Header and prior stay SIGNED. TC-BE-7 is the Draft equivalent.
 * SIGNED status-update (ProductContractEditStatusRequest) has no signingDate — PUT
 * basicParameters.signingDate after READY→SIGNED (createSignedProductContractOnIndices
 * and TC-BE-6 following) so async CREATE does not fail "Signing date is mandatory!".
 * Known runtime gap: ProductContractNewVersionEventHandler loads ACTIVE/DELETED only —
 * Draft CREATE may not complete; spec still requires CREATE +1 (tests fail until handler includes Draft).
 */

import { expect } from '@playwright/test';
import { expect as baseExpect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import type { ProductPayload } from '../../jsons/payloads/create/productAndServices/product';
import {
  buildEditProductContractPayload,
  headerContractStatus,
  productContractStatusUpdate,
  loadProductContract,
  addDaysIso,
  anchorDateIso,
  isEligibleSignedVersionStatus,
  versionStatusOf,
} from './pdt-2815-version-validity.fixtures';
import {
  applyFixedParameterProductFields,
  applyFixedParameterTermFields,
  createSignedProductContractV1,
  entityId,
  loadProductDetailSnapshot,
  type ContractProductRef,
} from './pdt-2906-product-new-version-contract-update.fixtures';

type FixtureRequest = baseFixture['Request'];

export {
  applyFixedParameterProductFields,
  applyFixedParameterTermFields,
  createSignedProductContractV1,
  entityId,
  loadProductDetailSnapshot,
  loadProductContract,
  addDaysIso,
};

/**
 * Per-version product refs via GET /product-contract/{id}?versionId= (not ?version=).
 * Local export so PDT-3330 never depends on a mistaken product-contract query name.
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

/** Exact ProductService validation fragments (TC-BE-13 / TC-BE-14). */
export const ERR_D_MUST_BE_TODAY_OR_FUTURE =
  'newContractsVersionsStartDate-Date must be today or in the future;';
export const ERR_D_FIELD_REQUIRED = 'newContractsVersionsStartDate-Field is required;';

export type Pdt3330DateAliases = {
  todayDate: string;
  D: string;
  /** Prior Open End close date after PDT-3454 CREATE (D - 1 day). */
  DMinus1: string;
  futureDateA: string;
  futureDateE: string;
  pastDate: string;
  /** Following start that closes prior version with endDate = D (D + 1 day). */
  dayAfterD: string;
};

export function pdt3330DateAliases(reference = new Date()): Pdt3330DateAliases {
  const todayDate = reference.toISOString().split('T')[0];
  const D = addDaysIso(todayDate, 10);
  return {
    todayDate,
    D,
    DMinus1: addDaysIso(D, -1),
    futureDateA: addDaysIso(todayDate, 5),
    futureDateE: addDaysIso(todayDate, 20),
    pastDate: addDaysIso(todayDate, -5),
    dayAfterD: addDaysIso(D, 1),
  };
}

export type ContractVersionRow = {
  versionId: number;
  detailId: number | null;
  startDate: string;
  endDate: string | null;
  status?: string;
};

export function contractVersionRows(body: Record<string, unknown>): ContractVersionRow[] {
  const versions = (body.versions ?? []) as {
    versionId?: number;
    id?: number;
    startDate?: string;
    endDate?: string | null;
    status?: string;
  }[];
  return versions
    .filter((v) => v.versionId != null && v.startDate)
    .map((v) => ({
      versionId: Number(v.versionId),
      detailId: v.id != null ? Number(v.id) : null,
      startDate: String(v.startDate).slice(0, 10),
      endDate: v.endDate != null && v.endDate !== '' ? String(v.endDate).slice(0, 10) : null,
      status: v.status,
    }));
}

export function findVersionByStartDate(
  body: Record<string, unknown>,
  startDate: string,
): ContractVersionRow | undefined {
  const target = startDate.slice(0, 10);
  return contractVersionRows(body).find((r) => r.startDate === target);
}

export function findVersionByEndDate(
  body: Record<string, unknown>,
  endDate: string,
): ContractVersionRow | undefined {
  const target = endDate.slice(0, 10);
  return contractVersionRows(body).find((r) => r.endDate === target);
}

export async function productContractStatusUpdateFull(
  Request: FixtureRequest,
  contractId: number,
  versionId: number,
  body: {
    contractStatus: string;
    contractSubStatus: string;
    contractVersionStatus: string;
  },
) {
  return Request.put(`product-contract/status-update/${contractId}?versionId=${versionId}`, {
    data: body,
  });
}

export type BaseChainResult = {
  productId: number;
  productDetailV1Id: number;
  productIndex: number;
};

/**
 * Dev rejects coordinates on POST /customer without edit-coordinates permission.
 * Same strip as contract-resign-new-flow / pdt-3206: delete latitude/longitude on
 * customer.address and communicationData[].address before create.
 */
function stripAddressCoords(addr: unknown): void {
  if (!addr || typeof addr !== 'object') return;
  const row = addr as Record<string, unknown>;
  delete row.latitude;
  delete row.longitude;
}

function stripCustomerCoords(payload: Record<string, unknown>): void {
  stripAddressCoords(payload.address);
  const comm = payload.communicationData;
  if (!Array.isArray(comm)) return;
  for (const entry of comm) {
    if (entry && typeof entry === 'object') {
      stripAddressCoords((entry as Record<string, unknown>).address);
    }
  }
}

/** Term → price → fixed-parameter product v1 → customer → POD. */
export async function createPdt3330BaseChain(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  TestRunSummary?: { registerPayload: (key: string, payload: unknown) => void },
): Promise<BaseChainResult> {
  const termPayload = GeneratePayload.productAndServices.term();
  applyFixedParameterTermFields(termPayload);
  TestRunSummary?.registerPayload('terms', termPayload);
  const termRes = await Request.post(Endpoints.terms, { data: termPayload });
  await baseExpect(termRes).CheckResponse();
  Responses.terms.push(await termRes.json());

  const pricePayload = GeneratePayload.productAndServices.electricity();
  TestRunSummary?.registerPayload('priceComponent', pricePayload);
  const priceRes = await Request.post(Endpoints.priceComponent, { data: pricePayload });
  await baseExpect(priceRes).CheckResponse();
  Responses.priceComponent.push(await priceRes.json());

  const productPayload = GeneratePayload.productAndServices.product();
  applyFixedParameterProductFields(productPayload);
  TestRunSummary?.registerPayload('product', productPayload);
  const productRes = await Request.post(Endpoints.product, { data: productPayload });
  await baseExpect(productRes).CheckResponse();
  Responses.product.push(await productRes.json());
  const productId = entityId(Responses.product[Responses.product.length - 1]);
  const productIndex = Responses.product.length - 1;

  const snap = await loadProductDetailSnapshot(Request, Endpoints, productId, 1);

  const customerPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  stripCustomerCoords(customerPayload);
  TestRunSummary?.registerPayload('customer', customerPayload);
  const customerRes = await Request.post(Endpoints.customer, { data: customerPayload });
  await baseExpect(customerRes).CheckResponse();
  Responses.customer.push(await customerRes.json());

  const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();
  podPayload.type = 'CONSUMER';
  podPayload.consumptionPurpose = 'NON_HOUSEHOLD';
  podPayload.voltageLevel = 'LOW';
  TestRunSummary?.registerPayload('pod', podPayload);
  const podRes = await Request.post(Endpoints.pod, { data: podPayload });
  await baseExpect(podRes).CheckResponse();
  Responses.pod.push(await podRes.json());

  return { productId, productDetailV1Id: snap.detailId, productIndex };
}

/** Extra customer + POD for multi-contract TCs (customerIndex/podIndex = last pushed). */
export async function createExtraCustomerAndPod(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  TestRunSummary?: { registerPayload: (key: string, payload: unknown) => void },
  label = 'B',
): Promise<{ customerIndex: number; podIndex: number }> {
  const customerPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  stripCustomerCoords(customerPayload);
  TestRunSummary?.registerPayload(`customer${label}`, customerPayload);
  const customerRes = await Request.post(Endpoints.customer, { data: customerPayload });
  await baseExpect(customerRes).CheckResponse();
  Responses.customer.push(await customerRes.json());
  const customerIndex = Responses.customer.length - 1;

  const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();
  podPayload.type = 'CONSUMER';
  podPayload.consumptionPurpose = 'NON_HOUSEHOLD';
  podPayload.voltageLevel = 'LOW';
  TestRunSummary?.registerPayload(`pod${label}`, podPayload);
  const podRes = await Request.post(Endpoints.pod, { data: podPayload });
  await baseExpect(podRes).CheckResponse();
  Responses.pod.push(await podRes.json());
  const podIndex = Responses.pod.length - 1;

  return { customerIndex, podIndex };
}

/**
 * Copy invoice payment term from the product version being contracted (GET /products/{id}?version=).
 * Generator always GETs product ?version=1 then terms[0] invoicePaymentTerms — that id is invalid
 * on product v2+ (POST 400 invoicePaymentTermId-wrong invoice payment term).
 *
 * Swagger: ProductDetailResponse has terms.id (not invoicePaymentTerms). Contract create
 * ProductContractProductParametersCreateRequest.required includes invoicePaymentTermId;
 * optional invoicePaymentTermValue. Runtime product body may still expose invoicePaymentTerms
 * or productParameters.invoicePaymentTerm — prefer those, else GET terms?id= like the generator.
 */
async function applyInvoicePaymentTermFromProductVersion(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  productId: number,
  productVersionId: number,
  contractPayload: Record<string, unknown>,
): Promise<void> {
  const productGet = await Request.get(`${Endpoints.product}/${productId}?version=${productVersionId}`);
  await baseExpect(productGet).CheckResponse();
  const productBody = (await productGet.json()) as {
    invoicePaymentTerms?: Array<{ id?: number; value?: number }>;
    productParameters?: {
      invoicePaymentTerm?: { id?: number; value?: number };
      invoicePaymentTermId?: number;
      invoicePaymentTermValue?: number;
    };
    terms?: { id?: number };
  };

  const pp = contractPayload.productParameters as Record<string, unknown>;
  const fromCatalog = productBody.invoicePaymentTerms?.[0];
  const fromParams = productBody.productParameters?.invoicePaymentTerm;
  let termId: number | undefined;
  let termValue: number | undefined;

  if (fromCatalog?.id != null) {
    termId = Number(fromCatalog.id);
    if (fromCatalog.value != null) termValue = Number(fromCatalog.value);
  } else if (fromParams?.id != null) {
    termId = Number(fromParams.id);
    if (fromParams.value != null) termValue = Number(fromParams.value);
  } else if (productBody.productParameters?.invoicePaymentTermId != null) {
    termId = Number(productBody.productParameters.invoicePaymentTermId);
    if (productBody.productParameters.invoicePaymentTermValue != null) {
      termValue = Number(productBody.productParameters.invoicePaymentTermValue);
    }
  } else if (productBody.terms?.id != null) {
    const termsGet = await Request.get(`${Endpoints.terms}?id=${productBody.terms.id}`);
    await baseExpect(termsGet).CheckResponse();
    const termsBody = (await termsGet.json()) as {
      invoicePaymentTerms?: Array<{ id?: number; value?: number }>;
    };
    const ipt = termsBody.invoicePaymentTerms?.[0];
    if (ipt?.id != null) {
      termId = Number(ipt.id);
      if (ipt.value != null) termValue = Number(ipt.value);
    }
  }

  expect(
    termId != null && Number.isFinite(termId) && termId > 0,
    `invoicePaymentTermId from product ${productId} version ${productVersionId}`,
  ).toBe(true);
  pp.invoicePaymentTermId = termId;
  if (termValue != null && Number.isFinite(termValue)) {
    pp.invoicePaymentTermValue = termValue;
  }
}

function signingDateFromHeaderOrVersion(
  body: Record<string, unknown>,
  versionId: number,
): string | null {
  const header = ((body.basicParameters ?? {}) as { signingDate?: string | null }).signingDate;
  if (header != null && String(header).trim() !== '') {
    return String(header);
  }
  const versions = (body.versions ?? []) as { versionId?: number; signingDate?: string | null }[];
  const row = versions.find((v) => Number(v.versionId) === versionId);
  if (row?.signingDate != null && String(row.signingDate).trim() !== '') {
    return String(row.signingDate);
  }
  return null;
}

/**
 * PUT signingDate after READY→SIGNED. Status-update schema has no signingDate;
 * async CREATE requires it on basicParameters.
 */
async function putSigningDateOnSignedVersion(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  contractId: number,
  contractPayload: Record<string, unknown>,
  versionId: number,
): Promise<void> {
  const current = await loadProductContract(Request, contractId);
  const row = contractVersionRows(current).find((r) => r.versionId === versionId);
  expect(row, `contract ${contractId} version ${versionId} must exist before signingDate PUT`).toBeTruthy();

  const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
    startDate: row!.startDate,
    savingAsNewVersion: false,
    preserveSigningDate: true,
  });
  const bp = editPayload.basicParameters as Record<string, unknown>;
  if (bp.signingDate == null || bp.signingDate === '') {
    bp.signingDate = anchorDateIso();
  }

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await baseExpect(put).CheckResponse();

  const afterGet = await Request.get(`product-contract/${contractId}?versionId=${versionId}`);
  await baseExpect(afterGet).CheckResponse();
  const afterBody = (await afterGet.json()) as Record<string, unknown>;
  expect(
    signingDateFromHeaderOrVersion(afterBody, versionId),
    `contract ${contractId} version ${versionId} basicParameters.signingDate must be non-null after SIGNED PUT`,
  ).toBeTruthy();
}

export async function createSignedProductContractOnIndices(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  customerIndex: number,
  productIndex: number,
  podIndex: number,
  productVersionId = 1,
): Promise<{ contractId: number; contractPayload: Record<string, unknown> }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    customerIndex,
    productIndex,
    podIndex,
  )) as Record<string, unknown>;
  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;

  // Generator hardcodes customer[0] — pin the intended customer / product version.
  const customerId = entityId(Responses.customer[customerIndex]);
  bp.customerId = customerId;
  const customerGet = await Request.get(`customer/${customerId}?version=1`);
  await baseExpect(customerGet).CheckResponse();
  const customerBody = await customerGet.json();
  const commId = customerBody.communicationData?.[0]?.id;
  if (commId != null) {
    bp.communicationDataBillingId = commId;
    bp.communicationDataContractId = commId;
  }

  const productId = entityId(Responses.product[productIndex]);
  const productSnap = await loadProductDetailSnapshot(
    Request,
    Endpoints,
    productId,
    productVersionId,
  );
  bp.productId = productSnap.productId;
  bp.productVersionId = productVersionId;
  bp.productDetailId = productSnap.detailId;
  await applyInvoicePaymentTermFromProductVersion(
    Request,
    Endpoints,
    productId,
    productVersionId,
    contractPayload,
  );

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

  await putSigningDateOnSignedVersion(Request, Endpoints, contractId, contractPayload, 1);

  return { contractId, contractPayload };
}

export async function createDraftProductContractOnIndices(
  Request: FixtureRequest,
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
  customerIndex: number,
  productIndex: number,
  podIndex: number,
): Promise<{ contractId: number; contractPayload: Record<string, unknown> }> {
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    customerIndex,
    productIndex,
    podIndex,
  )) as Record<string, unknown>;
  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;

  const customerId = entityId(Responses.customer[customerIndex]);
  bp.customerId = customerId;
  const customerGet = await Request.get(`customer/${customerId}?version=1`);
  await baseExpect(customerGet).CheckResponse();
  const customerBody = await customerGet.json();
  const commId = customerBody.communicationData?.[0]?.id;
  if (commId != null) {
    bp.communicationDataBillingId = commId;
    bp.communicationDataContractId = commId;
  }

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

  const draftStatus = await productContractStatusUpdateFull(Request, contractId, 1, {
    contractStatus: 'DRAFT',
    contractSubStatus: 'DRAFT',
    contractVersionStatus: 'DRAFT',
  });
  await baseExpect(draftStatus).CheckResponse();

  return { contractId, contractPayload };
}

/**
 * Pin an existing logical version's startDate so it is strictly before `beforeDate`.
 * Needed when generator / FIRST_DAY_OF_MONTH yields v1 start >= following start; otherwise
 * a naive bump of following start would break End=D / dayAfterD scenarios.
 */
export async function ensureVersionStartsBefore(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  contractId: number,
  contractPayload: Record<string, unknown>,
  versionId: number,
  beforeDate: string,
): Promise<string> {
  const body = await loadProductContract(Request, contractId);
  const rows = contractVersionRows(body);
  const row = rows.find((r) => r.versionId === versionId);
  expect(row, `contract ${contractId} version ${versionId} must exist before pin`).toBeTruthy();

  const before = beforeDate.slice(0, 10);
  if (row!.startDate < before) {
    return row!.startDate;
  }

  const today = anchorDateIso();
  const pinnedStart = today < before ? today : addDaysIso(before, -1);
  expect(
    pinnedStart < before,
    `pinned start ${pinnedStart} must be before following start ${before}`,
  ).toBe(true);

  const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
    startDate: pinnedStart,
    savingAsNewVersion: false,
    preserveSigningDate: true,
  });
  const bp = editPayload.basicParameters as Record<string, unknown>;
  if (bp.signingDate == null || bp.signingDate === '') {
    bp.signingDate = pinnedStart;
  }

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await baseExpect(put).CheckResponse();

  const after = await loadProductContract(Request, contractId);
  const pinned = contractVersionRows(after).find((r) => r.versionId === versionId);
  expect(pinned?.startDate, `version ${versionId} start after pin`).toBe(pinnedStart);
  return pinnedStart;
}

/**
 * Force a logical version startDate to an exact ISO date (yyyy-MM-dd).
 * Used when Start must equal D (TC-BE-17: D=todayDate → Start = D replace, not CREATE).
 */
export async function ensureVersionStartEquals(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  contractId: number,
  contractPayload: Record<string, unknown>,
  versionId: number,
  startDate: string,
): Promise<string> {
  const target = startDate.slice(0, 10);
  const body = await loadProductContract(Request, contractId);
  const row = contractVersionRows(body).find((r) => r.versionId === versionId);
  expect(row, `contract ${contractId} version ${versionId} must exist before start pin`).toBeTruthy();
  if (row!.startDate === target) {
    return target;
  }

  const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
    startDate: target,
    savingAsNewVersion: false,
    preserveSigningDate: true,
  });
  const bp = editPayload.basicParameters as Record<string, unknown>;
  if (bp.signingDate == null || bp.signingDate === '') {
    bp.signingDate = target;
  }

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await baseExpect(put).CheckResponse();

  const after = await loadProductContract(Request, contractId);
  const pinned = contractVersionRows(after).find((r) => r.versionId === versionId);
  expect(pinned?.startDate, `version ${versionId} start after exact pin`).toBe(target);
  return target;
}

/**
 * Promote a non-SIGNED version to SIGNED so CalculateVersionDates can see it
 * (repository filters SIGNED only when chaining end dates).
 */
async function ensureVersionSignedForChaining(
  Request: FixtureRequest,
  contractId: number,
  versionId: number,
): Promise<void> {
  const body = await loadProductContract(Request, contractId);
  const versions = (body.versions ?? []) as {
    versionId?: number;
    status?: string;
    versionStatus?: string;
  }[];
  const row = versions.find((v) => Number(v.versionId) === versionId);
  const status = versionStatusOf(row);
  if (status === 'SIGNED' || status === 'Valid') {
    return;
  }

  // DRAFT / Not Valid / READY → SIGNED (READY→SIGNED skips READY hop when already READY).
  if (status !== 'READY') {
    const ready = await productContractStatusUpdate(Request, contractId, 'READY', 'READY', versionId);
    await baseExpect(ready).CheckResponse();
  }
  const signed = await productContractStatusUpdate(
    Request,
    contractId,
    'SIGNED',
    'SIGNED_BY_BOTH_SIDES',
    versionId,
  );
  await baseExpect(signed).CheckResponse();
}

/**
 * Local PUT create-new-version.
 *
 * SIGNED path: sets basicParameters.versionStatus = SIGNED on create so
 * ProductContractBasicParametersService.updateStartAndEndDates runs
 * CalculateVersionDates (prior endDate = followingStart - 1 day).
 * pdt-2815 putProductContractNewVersion always creates DRAFT then status-updates —
 * that path does NOT recalculate end dates. Use SIGNED path for End=D / End before D.
 *
 * DRAFT path: creates the following row with versionStatus=DRAFT on PUT (header stays
 * Draft). No Draft→SIGNED promote, no demote, no CalculateVersionDates expectation.
 * Prior endDate typically stays null (Open End). End=D / End<D shapes for Not Valid
 * cannot be automated without forbidden status transitions — covered by SIGNED TCs.
 */
export async function createFollowingContractVersion(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  contractId: number,
  contractPayload: Record<string, unknown>,
  fromVersionId: number,
  startDate: string,
  versionStatus: 'SIGNED' | 'DRAFT' = 'SIGNED',
): Promise<number> {
  const followingStart = startDate.slice(0, 10);

  await ensureVersionStartsBefore(
    Request,
    Endpoints,
    contractId,
    contractPayload,
    fromVersionId,
    followingStart,
  );

  if (versionStatus === 'SIGNED') {
    await ensureVersionSignedForChaining(Request, contractId, fromVersionId);
  }

  const contractBody = await loadProductContract(Request, contractId);
  const headerSt = headerContractStatus(contractBody);

  const editPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
    startDate: followingStart,
    savingAsNewVersion: true,
    preserveSigningDate: true,
  });

  const bp = editPayload.basicParameters as Record<string, unknown>;
  const pp = editPayload.productParameters as Record<string, unknown>;

  if (versionStatus === 'DRAFT') {
    // Stay Draft on create — no promote/demote; no end-date chaining.
    bp.versionStatus = 'DRAFT';
    bp.status = headerSt === 'DRAFT' || !headerSt ? 'DRAFT' : headerSt;
    if (bp.status === 'DRAFT') {
      bp.subStatus = 'DRAFT';
      bp.signingDate = null;
    }
    bp.entryInForceDate = null;
    bp.startOfInitialTerm = null;
    pp.entryIntoForceValue = null;
    pp.startOfContractValue = null;
  } else {
    // Force SIGNED on create so CalculateVersionDates chains prior.endDate = followingStart - 1.
    bp.versionStatus = 'SIGNED';
    bp.status = headerSt === 'DRAFT' || !headerSt ? 'SIGNED' : headerSt;
    if (bp.status === 'SIGNED') {
      bp.subStatus = 'SIGNED_BY_BOTH_SIDES';
    }
    if (bp.signingDate == null || bp.signingDate === '') {
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
  }

  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=${fromVersionId}&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await baseExpect(put).CheckResponse();

  const bodyAfterCreate = await loadProductContract(Request, contractId);
  const rowsAfterCreate = contractVersionRows(bodyAfterCreate);
  const latest = Math.max(...rowsAfterCreate.map((r) => r.versionId));
  const following = findVersionByStartDate(bodyAfterCreate, followingStart);
  expect(following, `following version startDate=${followingStart}`).toBeTruthy();
  expect(following!.versionId).toBe(latest);
  expect(following!.endDate, 'new following version stays Open End').toBeNull();

  if (versionStatus === 'SIGNED') {
    const expectedPriorEnd = addDaysIso(followingStart, -1);
    const prior = findVersionByEndDate(bodyAfterCreate, expectedPriorEnd);
    expect(
      prior,
      `PDT-3330 chaining: prior version endDate must be ${expectedPriorEnd} after SIGNED create ` +
        `(following start=${followingStart}, contract=${contractId})`,
    ).toBeTruthy();
    expect(prior!.versionId).toBe(fromVersionId);
  }

  return latest;
}

/**
 * TC-BE-6: add a following Valid Open End without closing the prior row.
 * Create following as DRAFT on PUT (CalculateVersionDates does not run), then promote
 * only that row READY → SIGNED via status-update (pdt-2815 path also skips chaining).
 * Header stays SIGNED — never send contractStatus READY.
 */
export async function createFollowingValidOpenEndKeepPriorOpen(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  contractId: number,
  contractPayload: Record<string, unknown>,
  priorVersionId: number,
  dates: Pick<Pdt3330DateAliases, 'D' | 'dayAfterD'>,
): Promise<{ priorVersionId: number; followingVersionId: number }> {
  const followingVersionId = await createFollowingContractVersion(
    Request,
    Endpoints,
    contractId,
    contractPayload,
    priorVersionId,
    dates.dayAfterD,
    'DRAFT',
  );

  const headerBefore = await loadProductContract(Request, contractId);
  expect(headerContractStatus(headerBefore), 'header stays SIGNED while promoting following').toBe(
    'SIGNED',
  );
  const headerSub =
    ((headerBefore.basicParameters ?? {}) as { subStatus?: string }).subStatus ??
    'SIGNED_BY_BOTH_SIDES';

  const ready = await productContractStatusUpdateFull(Request, contractId, followingVersionId, {
    contractStatus: 'SIGNED',
    contractSubStatus: headerSub,
    contractVersionStatus: 'READY',
  });
  await baseExpect(ready).CheckResponse();

  const signed = await productContractStatusUpdateFull(Request, contractId, followingVersionId, {
    contractStatus: 'SIGNED',
    contractSubStatus: headerSub,
    contractVersionStatus: 'SIGNED',
  });
  await baseExpect(signed).CheckResponse();

  const followingAfterStatus = await Request.get(
    `product-contract/${contractId}?versionId=${followingVersionId}`,
  );
  await baseExpect(followingAfterStatus).CheckResponse();
  const followingAfterStatusBody = (await followingAfterStatus.json()) as Record<string, unknown>;
  const followingSigningDate = signingDateFromHeaderOrVersion(
    followingAfterStatusBody,
    followingVersionId,
  );
  if (followingSigningDate == null || followingSigningDate === '') {
    await putSigningDateOnSignedVersion(
      Request,
      Endpoints,
      contractId,
      contractPayload,
      followingVersionId,
    );
  }

  const body = await loadProductContract(Request, contractId);
  const rows = contractVersionRows(body);
  expect(rows.length, 'exactly two versions before apply').toBe(2);
  for (const row of rows) {
    expect(row.endDate, `version ${row.versionId} stays Open End`).toBeNull();
  }

  const prior = rows.find((r) => r.versionId === priorVersionId);
  expect(prior, `prior version ${priorVersionId}`).toBeTruthy();
  expect(
    prior!.startDate < dates.D,
    `prior start ${prior!.startDate} must be before D=${dates.D}`,
  ).toBe(true);

  const following = findVersionByStartDate(body, dates.dayAfterD);
  expect(following, `following startDate=${dates.dayAfterD}`).toBeTruthy();
  expect(following!.versionId).toBe(followingVersionId);

  const rawVersions = (body.versions ?? []) as {
    versionId?: number;
    status?: string;
    versionStatus?: string;
    contractVersionStatus?: string;
  }[];
  for (const raw of rawVersions) {
    expect(
      isEligibleSignedVersionStatus(versionStatusOf(raw)),
      `version ${raw.versionId} must be Valid/SIGNED`,
    ).toBe(true);
  }
  expect(headerContractStatus(body), 'header stays SIGNED').toBe('SIGNED');

  return { priorVersionId, followingVersionId };
}

export type ProductApplyEditOpts = {
  baseVersion: number;
  updateExistingVersion: boolean;
  shortDescription: string;
  productDetailIds?: number[] | null;
  /** Pass null to omit; undefined leaves generator default. */
  newContractsVersionsStartDate?: string | null;
};

/**
 * Build ProductEditRequest for create-new-version / in-place update.
 * Forces commercial change via unique shortDescription (+ name).
 */
export async function buildPdt3330ProductEditPayload(
  GeneratePayload: baseFixture['GeneratePayload'],
  productIndex: number,
  opts: ProductApplyEditOpts,
): Promise<ProductPayload & Record<string, unknown>> {
  const template = GeneratePayload.productAndServices.product(productIndex);
  applyFixedParameterProductFields(template);
  template.shortDescription = opts.shortDescription;

  const editPayload = (await GeneratePayload.productAndServices.edit_Product(
    template,
    productIndex,
  )) as unknown as ProductPayload & Record<string, unknown>;
  applyFixedParameterProductFields(editPayload);
  editPayload.shortDescription = opts.shortDescription;
  editPayload.name = `${String(editPayload.name ?? 'PDT-3330')} ${opts.shortDescription}`;
  editPayload.version = opts.baseVersion;
  editPayload.updateExistingVersion = opts.updateExistingVersion;
  editPayload.priceComponentIds = (editPayload.priceComponentIds ?? []).map((e) => entityId(e));

  if (opts.productDetailIds === null) {
    delete editPayload.productDetailIdsForUpdatingProductContracts;
  } else if (opts.productDetailIds !== undefined) {
    editPayload.productDetailIdsForUpdatingProductContracts = opts.productDetailIds;
  }

  if (opts.newContractsVersionsStartDate === null) {
    delete editPayload.newContractsVersionsStartDate;
  } else if (opts.newContractsVersionsStartDate !== undefined) {
    editPayload.newContractsVersionsStartDate = opts.newContractsVersionsStartDate;
  }

  return editPayload;
}

export async function validateProductRelatedContractUpdate(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  editPayload: Record<string, unknown>,
): Promise<{ status: number; eligible: boolean; bodyText: string }> {
  const res = await Request.post(`${Endpoints.product}/validate-product-related-contract-update`, {
    data: editPayload,
  });
  const bodyText = await res.text();
  let eligible = false;
  try {
    eligible = JSON.parse(bodyText) === true;
  } catch {
    eligible = false;
  }
  return { status: res.status(), eligible, bodyText };
}

export async function putProductEdit(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  productId: number,
  editPayload: Record<string, unknown>,
) {
  return Request.put(`${Endpoints.product}/${productId}`, { data: editPayload });
}

/** Poll until a logical version with startDate appears (async Stage1 create). */
export async function pollUntilContractVersionStartDate(
  Request: FixtureRequest,
  contractId: number,
  startDate: string,
  timeoutMs = 120_000,
): Promise<Record<string, unknown>> {
  const target = startDate.slice(0, 10);
  let lastBody: Record<string, unknown> = {};

  await expect
    .poll(
      async () => {
        lastBody = await loadProductContract(Request, contractId);
        return Boolean(findVersionByStartDate(lastBody, target));
      },
      {
        message: `PDT-3330 — contract ${contractId} version with startDate=${target}`,
        timeout: timeoutMs,
        intervals: [2000, 3000, 5000],
      },
    )
    .toBe(true);

  return lastBody;
}

export async function loadVersionProductDetailId(
  Request: FixtureRequest,
  contractId: number,
  versionId: number,
): Promise<number> {
  const ref = await loadContractVersionProductRef(Request, contractId, versionId);
  return ref.productDetailId;
}

/**
 * PDT-3454 CREATE +1 after immediate GET: count+1, new start=D Open End on v2,
 * prior end=D-1 and product stays v1. PUT 200 is not success — caller must GET first.
 */
export async function assertOpenEndCreatePlusOne(
  Request: FixtureRequest,
  contractId: number,
  afterBody: Record<string, unknown>,
  opts: {
    versionCountBefore: number;
    priorVersionId: number;
    productDetailBefore: number;
    expectedNewProductDetailId: number;
    expectedNewProductVersionId?: number;
    D: string;
    DMinus1: string;
  },
): Promise<{
  versionCountAfter: number;
  created: ContractVersionRow;
  prior: ContractVersionRow;
  newDetail: number;
  priorDetail: number;
}> {
  const versionCountAfter = contractVersionRows(afterBody).length;
  expect(versionCountAfter, 'PDT-3454 CREATE +1 version count').toBe(opts.versionCountBefore + 1);

  const created = findVersionByStartDate(afterBody, opts.D);
  expect(created, `new version startDate=${opts.D}`).toBeTruthy();
  expect(created!.endDate, 'new row stays Open End').toBeNull();

  const prior = contractVersionRows(afterBody).find((r) => r.versionId === opts.priorVersionId);
  expect(prior, `prior version ${opts.priorVersionId}`).toBeTruthy();
  expect(prior!.endDate, `prior endDate = D-1 (${opts.DMinus1})`).toBe(opts.DMinus1);

  const newRef = await loadContractVersionProductRef(Request, contractId, created!.versionId);
  expect(newRef.productDetailId).toBe(opts.expectedNewProductDetailId);
  if (opts.expectedNewProductVersionId != null) {
    expect(newRef.productVersionId).toBe(opts.expectedNewProductVersionId);
  }

  const priorDetail = await loadVersionProductDetailId(Request, contractId, opts.priorVersionId);
  expect(priorDetail, 'prior productDetail stays previous version (not replaced in place)').toBe(
    opts.productDetailBefore,
  );

  return {
    versionCountAfter,
    created: created!,
    prior: prior!,
    newDetail: newRef.productDetailId,
    priorDetail,
  };
}

export function errorBodyContains(bodyText: string, fragment: string): boolean {
  return bodyText.includes(fragment);
}

export function todayMinusOneIso(today = anchorDateIso()): string {
  return addDaysIso(today, -1);
}

export const PDT_3330_RELEVANT_ENTITIES = [
  'terms',
  'priceComponent',
  'product',
  'customer',
  'pod',
  'productContract',
] as const;

export const JIRA_TITLE =
  'Additional logics - Create product new version and apply it to existing contracts';

export function pdt3330TestTitle(tcId: string, scenario: string): string {
  return `[PDT-3330]: ${JIRA_TITLE} | ${tcId} ${scenario}`;
}
