/**
 * PDT-3059 — Save product contract as new version with individual product + additional params.
 *
 * Bug: cloned individual product gets new additional-param IDs; PUT still sends stale IDs from GET.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3035-contract-mass-import.fixtures.ts (GET→PUT payload mapping)
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (contract version PUT)
 * - Cursor-Project/examples/repro_pdt_3059_dev.mjs (repro flow)
 */

import { expect, type APIRequestContext } from '@playwright/test';
import type { baseFixture } from '../../fixtures/baseFixture';
import { configuredBaseURL } from '../../fixtures/utils/baseUrl';

export const PDT_3059_KNOWN_CONTRACT_NUMBER = 'EPES2607000492';
export const PDT_3059_KNOWN_PRODUCT_ID = 7459;
export const PDT_3059_KNOWN_ADDITIONAL_PARAM_ID = 79334;

/** Dev known individual contract — DB has labeled additional params (product 28993, param 336837). */
export const PDT_3059_DEV_KNOWN_CONTRACT_NUMBER = 'EPES2601002934';
/** Legacy fallback (no labeled params on product GET) — kept for reference only. */
export const PDT_3059_DEV_FALLBACK_CONTRACT_NUMBER = 'EPES2509052558';

export type Pdt3059DevContractRegistryEntry = {
  contractNumber: string;
  productId: number;
  /** Additional-param ids when contract/product GET omit ids (Dev individual-product quirk). */
  additionalParams: Array<{ id: number; value: string }>;
};

export const PDT_3059_DEV_CONTRACT_REGISTRY: Pdt3059DevContractRegistryEntry[] = [
  {
    contractNumber: PDT_3059_DEV_KNOWN_CONTRACT_NUMBER,
    productId: 28993,
    additionalParams: [{ id: 336837, value: '124' }],
  },
];

export const PDT_3059_ADDITIONAL_PARAM_NOT_FOUND_PATTERN =
  /additional params with id .* not found/i;

export type Pdt3059Environment = 'dev' | 'dev2' | 'test' | 'unknown';

export type Pdt3059CreateFx = {
  Request: APIRequestContext;
  GeneratePayload: baseFixture['GeneratePayload'];
  Responses: baseFixture['Responses'];
  Endpoints: baseFixture['Endpoints'];
};

export type Pdt3059ContractContext = {
  contractId: number;
  contractNumber: string;
  productId: number;
  latestVersionId: number;
  additionalParamIds: number[];
  /** Values paired with additionalParamIds for Dev PUT injection when GET omits the list. */
  additionalParamValues?: string[];
  source: 'known-test-contract' | 'known-dev-contract' | 'dev-seeded-contract' | 'fresh-api-chain';
};

export type Pdt3059NewVersionPutResult = {
  status: number;
  bodyText: string;
  hasAdditionalParamNotFoundError: boolean;
  putVersionId: number;
  startDate: string;
  staleAdditionalParamIds: number[];
  /** Logical versionId (max of versions[]) before PUT. */
  versionIdBefore: number;
  /** Logical versionId after reload; same as before when HTTP failed or version was not created. */
  versionIdAfter: number;
  versionCountBefore: number;
  versionCountAfter: number;
  /** True when max versionId increased by 1 or versions.length increased by 1. */
  versionActuallyCreated: boolean;
};

export type Pdt3059OutcomeEvaluation = {
  passed: boolean;
  expectedResult: string;
  actualResult: string;
};

export function countContractVersions(contract: Record<string, unknown>): number {
  const versions = (contract.versions ?? []) as unknown[];
  return Array.isArray(versions) ? versions.length : 0;
}

/**
 * New version created when max logical versionId increased by 1, or versions array grew by 1.
 * Prefer versionId delta; length is a fallback when max id stays stable but a row was appended.
 */
export function didCreateNewContractVersion(before: {
  versionId: number;
  versionCount: number;
}, after: {
  versionId: number;
  versionCount: number;
}): boolean {
  const versionIdIncreasedByOne = after.versionId === before.versionId + 1;
  const versionCountIncreasedByOne = after.versionCount === before.versionCount + 1;
  return versionIdIncreasedByOne || versionCountIncreasedByOne;
}

export function resolvePdt3059Environment(): Pdt3059Environment {
  const raw = (configuredBaseURL || process.env.BASE_URL || '').toLowerCase();
  if (raw.includes('testapps') || raw.includes('phoenix-epres') || raw.includes('10.236.20.31')) {
    return 'test';
  }
  if (raw.includes('phoenix-dev2') || raw.includes('dev2') || raw.includes(':8092')) {
    return 'dev2';
  }
  if (raw.includes('10.236.20.11') || raw.includes(':8091') || raw.includes('devapps')) {
    return 'dev';
  }
  return 'unknown';
}

/** Test = bug still present (assert stale-id error). Dev = post-fix regression (assert success). */
export function pdt3059BugExpectedOnEnv(env: Pdt3059Environment = resolvePdt3059Environment()): boolean {
  return env === 'test';
}

export function evaluatePdt3059NewVersionOutcome(
  result: Pdt3059NewVersionPutResult,
  env: Pdt3059Environment = resolvePdt3059Environment(),
): Pdt3059OutcomeEvaluation {
  const bugExpected = pdt3059BugExpectedOnEnv(env);
  const versionSummary =
    `versionId ${result.versionIdBefore}→${result.versionIdAfter} ` +
    `count ${result.versionCountBefore}→${result.versionCountAfter} ` +
    `created=${result.versionActuallyCreated}`;

  if (bugExpected) {
    const hasBug = result.status >= 400 && result.hasAdditionalParamNotFoundError;
    return {
      passed: hasBug,
      expectedResult:
        'PUT product-contract with savingAsNewVersion:true fails — additional param IDs from GET are stale after individual product clone.',
      actualResult:
        `env=${env} status=${result.status} staleIds=${result.staleAdditionalParamIds.join(',')} ` +
        `error=${result.hasAdditionalParamNotFoundError} ${versionSummary}`,
    };
  }

  const httpOk = result.status >= 200 && result.status < 300;
  const passed =
    httpOk && result.versionActuallyCreated && !result.hasAdditionalParamNotFoundError;

  if (httpOk && !result.versionActuallyCreated) {
    console.log(
      `[PDT-3059] FALSE POSITIVE RISK: PUT returned ${result.status} but new contract version was NOT created. ` +
        `${versionSummary}. body=${result.bodyText.slice(0, 400)}`,
    );
  }

  return {
    passed,
    expectedResult:
      'PUT product-contract with savingAsNewVersion:true succeeds and creates a new version ' +
      '(latestLogicalVersionId +1 or versions.length +1; no stale additional-param ID error).',
    actualResult:
      `env=${env} status=${result.status} additionalParamNotFound=${result.hasAdditionalParamNotFoundError} ` +
      `${versionSummary}`,
  };
}

function addDaysIso(days: number): string {
  const d = new Date();
  d.setUTCHours(12, 0, 0, 0);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

function parseVersionStartDate(startDate?: string): Date | null {
  if (!startDate) return null;
  const trimmed = String(startDate).trim();
  if (/^\d{2}-\d{2}-\d{4}$/.test(trimmed)) {
    const [dd, mm, yyyy] = trimmed.split('-');
    return new Date(`${yyyy}-${mm}-${dd}T12:00:00.000Z`);
  }
  if (/^\d{4}-\d{2}-\d{2}/.test(trimmed)) {
    return new Date(`${trimmed.slice(0, 10)}T12:00:00.000Z`);
  }
  return null;
}

export function futureStartDateFromContract(contract: Record<string, unknown>): string {
  const versions = (contract.versions ?? []) as Array<{ startDate?: string }>;
  const latest = versions.reduce<Date | null>((max, version) => {
    const parsed = parseVersionStartDate(version.startDate);
    return parsed && (!max || parsed > max) ? parsed : max;
  }, null);
  const base = latest ?? new Date();
  const next = new Date(base);
  next.setUTCDate(next.getUTCDate() + 30);
  return next.toISOString().slice(0, 10);
}

export function latestLogicalVersionId(contract: Record<string, unknown>): number {
  const versions = (contract.versions ?? [{ versionId: 1 }]) as Array<{ versionId?: number }>;
  return versions.reduce((max, version) => Math.max(max, Number(version.versionId ?? 0)), 1);
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
    throw new Error(`Contract ${target} not found via product-contract/list.`);
  }
  return contractId;
}

function extractAdditionalParamIdsFromContractGet(contract: Record<string, unknown>): number[] {
  const productParameters = (contract.productParameters ?? {}) as Record<string, unknown>;
  const additionalList = (productParameters.productContractProductAdditionalParamsResponseList ??
    productParameters.productContractProductAdditionalParams ??
    productParameters.productAdditionalParams ??
    []) as Array<Record<string, unknown>>;

  return additionalList
    .map((param) => Number(param.id ?? param.productAdditionalParamsId))
    .filter((id) => Number.isFinite(id) && id > 0);
}

function buildPodRequestsFromContractGet(contract: Record<string, unknown>): Array<Record<string, unknown>> {
  const pods = (contract.contractPodsResponses ?? contract.contractPods ?? []) as Array<
    Record<string, unknown>
  >;
  const byBillingGroup = new Map<number, Array<Record<string, unknown>>>();

  for (const pod of pods) {
    const billingGroupId = Number(pod.billingGroupId);
    const pointOfDeliveryDetailId = Number(pod.podDetailId ?? pod.pointOfDeliveryDetailId);
    if (!Number.isFinite(billingGroupId) || billingGroupId <= 0) continue;
    if (!Number.isFinite(pointOfDeliveryDetailId) || pointOfDeliveryDetailId <= 0) continue;
    if (!byBillingGroup.has(billingGroupId)) {
      byBillingGroup.set(billingGroupId, []);
    }
    byBillingGroup.get(billingGroupId)!.push({
      pointOfDeliveryDetailId,
      dealNumber: pod.podType !== 'CONSUMER' ? pod.dealNumber ?? null : null,
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
    riskAssessment == null || riskAssessment === ''
      ? null
      : typeof riskAssessment === 'string'
        ? riskAssessment.toUpperCase()
        : riskAssessment;

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
    ...(normalizedRisk != null ? { riskAssessment: normalizedRisk } : {}),
    riskAssessmentAdditionalConditions:
      additionalParameters.riskAssessmentAdditionalConditions ??
      additionalParameters.riskAssessmentAdditionalInformation ??
      null,
  };
}

/** Swagger ProductContractProductParametersCreateRequest.productAdditionalParams — { id, value }. */
export function mapProductParametersForPut(
  productParameters: Record<string, unknown>,
): Record<string, unknown> {
  const contractTerm = productParameters.contractTerm as { id?: number } | undefined;
  const invoicePaymentTerm = productParameters.invoicePaymentTerm as { id?: number } | undefined;
  const additionalList = (productParameters.productContractProductAdditionalParamsResponseList ??
    productParameters.productContractProductAdditionalParams ??
    productParameters.productAdditionalParams ??
    []) as Array<Record<string, unknown>>;

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
    productAdditionalParams: additionalList.map((param) => ({
      id: Number(param.id ?? param.productAdditionalParamsId),
      value: String(param.value ?? param.contractValue ?? ''),
    })),
  };
}

function mapBasicParametersForPut(
  bp: Record<string, unknown>,
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
    untilAmountCurrencyId:
      (bp.untilAmountCurrency as { id?: number } | undefined)?.id ?? bp.untilAmountCurrencyId,
    untilVolumeCurrencyId:
      (bp.untilVolumeCurrency as { id?: number } | undefined)?.id ?? bp.untilVolumeCurrencyId,
    entryIntoForceDate: bp.entryIntoForceDate,
    signingDate: bp.signingDate,
    startDate: bp.startDate,
    endDate: bp.endDate,
    versionTypeIds,
    customerId: bp.customerId,
    customerVersionId: bp.customerVersionId ?? bp.customerDetailId,
    communicationDataBillingId: bp.billingCommunicationData?.id ?? bp.communicationDataBillingId,
    communicationDataContractId: bp.contractCommunicationData?.id ?? bp.communicationDataContractId,
    customerNewDetailsId: bp.customerNewDetailsId,
    files: bp.files,
    documents: bp.documents,
    versionStatus: bp.versionStatus,
    proxy,
    relatedEntities: bp.relatedEntities,
    terminationDate: bp.terminationDate,
    perpetuityDate: bp.perpetuityDate,
    contractTermEndDate: bp.contractTermEndDate,
  };
}

export function buildContractNewVersionPayloadFromGet(
  contract: Record<string, unknown>,
  startDate: string,
): { payload: Record<string, unknown>; versionId: number } {
  const basicParameters = (contract.basicParameters ?? {}) as Record<string, unknown>;
  const productParameters = contract.productParameters as Record<string, unknown> | undefined;
  const versionId = latestLogicalVersionId(contract);
  const proxy = (basicParameters.proxy ?? []) as Array<Record<string, unknown>>;
  const podRequests = buildPodRequestsFromContractGet(contract);
  if (podRequests.length === 0) {
    throw new Error('Cannot build product-contract PUT — contractPodsResponses empty.');
  }

  return {
    versionId,
    payload: {
      startDate,
      basicParameters: mapBasicParametersForPut(basicParameters, proxy),
      additionalParameters: mapAdditionalParametersForPut(
        (contract.additionalParameters ?? {}) as Record<string, unknown>,
      ),
      productParameters: productParameters ? mapProductParametersForPut(productParameters) : undefined,
      savingAsNewVersion: true,
      updateDealNumber: false,
      podRequests,
    },
  };
}

export async function loadContractAtLatestVersion(
  Request: APIRequestContext,
  contractId: number,
): Promise<Record<string, unknown>> {
  const listRes = await Request.get(`product-contract/${contractId}`);
  await expect(listRes).CheckResponse();
  const listBody = (await listRes.json()) as Record<string, unknown>;
  const versionId = latestLogicalVersionId(listBody);

  const getRes = await Request.get(`product-contract/${contractId}?versionId=${versionId}`);
  await expect(getRes).CheckResponse();
  return (await getRes.json()) as Record<string, unknown>;
}

async function assertIndividualProductWithAdditionalParams(
  fx: Pdt3059CreateFx,
  contract: Record<string, unknown>,
): Promise<{ productId: number; additionalParamIds: number[] }> {
  const basicParameters = (contract.basicParameters ?? {}) as Record<string, unknown>;
  const productId = Number(basicParameters.productId);
  if (!Number.isFinite(productId) || productId <= 0) {
    throw new Error('Contract GET missing basicParameters.productId');
  }

  const additionalParamIds = extractAdditionalParamIdsFromContractGet(contract);
  if (additionalParamIds.length === 0) {
    throw new Error(`Contract productId=${productId} has no additional params on GET`);
  }

  const productRes = await fx.Request.get(`${fx.Endpoints.product}/${productId}?version=1`);
  await expect(productRes).CheckResponse();
  const productBody = (await productRes.json()) as { isIndividual?: boolean };
  if (productBody.isIndividual !== true) {
    throw new Error(`Product ${productId} is not individual (isIndividual=${productBody.isIndividual})`);
  }

  return { productId, additionalParamIds };
}

async function loadKnownPdt3059Contract(
  fx: Pdt3059CreateFx,
  contractNumber: string,
  source: Pdt3059ContractContext['source'] = 'known-test-contract',
): Promise<Pdt3059ContractContext> {
  const contractId = await findProductContractIdByNumber(fx.Request, contractNumber);
  const contract = await loadContractAtLatestVersion(fx.Request, contractId);
  const { productId, additionalParamIds } = await assertIndividualProductWithAdditionalParams(
    fx,
    contract,
  );
  const bp = (contract.basicParameters ?? {}) as Record<string, unknown>;
  const resolvedNumber = String(bp.contractNumber ?? contractNumber).trim();

  console.log(
    `[PDT-3059] known contract ${resolvedNumber} (id=${contractId}) productId=${productId} ` +
      `additionalParamIds=${additionalParamIds.join(',')}`,
  );

  return {
    contractId,
    contractNumber: resolvedNumber,
    productId,
    latestVersionId: latestLogicalVersionId(contract),
    additionalParamIds,
    source,
  };
}

async function createPdt3059CatalogChain(
  fx: Pdt3059CreateFx,
): Promise<{ customerIdentifier: string }> {
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

  const customerPayload = GeneratePayload.customers.customer_private();
  const customer = await Request.post(Endpoints.customer, { data: customerPayload });
  await expect(customer).CheckResponse();
  Responses.customer.push(await customer.json());

  const pod = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod).CheckResponse();
  Responses.pod.push(await pod.json());

  return {
    customerIdentifier: String(customerPayload.customerIdentifier ?? ''),
  };
}

/** Label/value seeded on product create so contract PUT can attach the param by id. */
export const PDT_3059_SEED_ADDITIONAL_PARAM = {
  orderingId: 0,
  label: 'pdt-3059-param',
  value: '124',
} as const;

function applyPdt3059IndividualProductPayload(
  productPayload: Record<string, unknown>,
  customerIdentifier: string,
): void {
  // IndividualProductValidator: for isIndividual=true, globalSalesChannel/Area/Segment must be
  // absent/null AND salesChannelIds/salesAreasIds/segmentIds must be empty — any non-null global*
  // (true or false) fails with "Sales Channels/Area/Segments is disabled for individual product".
  productPayload.isIndividual = true;
  productPayload.customerIdentifier = customerIdentifier;
  productPayload.availableForSale = null;
  productPayload.availableFrom = null;
  productPayload.availableTo = null;
  delete productPayload.globalSalesChannel;
  delete productPayload.globalSalesArea;
  delete productPayload.globalSegment;
  delete productPayload.salesChannelIds;
  delete productPayload.salesAreasIds;
  delete productPayload.segmentIds;
  productPayload.contractTypes = ['COMBINED'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.typePointsOfDelivery = ['CONSUMER'];
  productPayload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
  productPayload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
  productPayload.voltageLevels = ['LOW'];
  productPayload.capacityLimitAmount = 5000;
  productPayload.capacityLimitType = 'TO';
  productPayload.equalMonthlyInstallmentsActivation = false;
  productPayload.productTerms = [
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
  // ProductsAdditionalParamsRequest — create with filled param so third-tab-fields returns id+value.
  productPayload.productAdditionalParams = [
    {
      orderingId: PDT_3059_SEED_ADDITIONAL_PARAM.orderingId,
      label: PDT_3059_SEED_ADDITIONAL_PARAM.label,
      value: PDT_3059_SEED_ADDITIONAL_PARAM.value,
    },
  ];
}

function resolveCustomerIdentifier(
  customerBody: Record<string, unknown>,
  postedIdentifier?: string | number | null,
): string {
  const identifier = String(
    postedIdentifier ??
      customerBody.customerIdentifier ??
      customerBody.identifier ??
      customerBody.uic ??
      customerBody.legalEntityIdentifier ??
      '',
  ).trim();
  if (!identifier) {
    throw new Error('Customer POST/GET did not expose identifier for individual product');
  }
  return identifier.toUpperCase();
}

async function putProductContractRegularUpdate(
  Request: APIRequestContext,
  contractId: number,
  overrides: Partial<Record<string, unknown>> = {},
): Promise<Record<string, unknown>> {
  const contract = await loadContractAtLatestVersion(Request, contractId);
  const versionId = latestLogicalVersionId(contract);
  const versions = (contract.versions ?? []) as Array<{ startDate?: string }>;
  const currentStartDate = versions[versions.length - 1]?.startDate ?? futureStartDateFromContract(contract);
  const { payload } = buildContractNewVersionPayloadFromGet(contract, currentStartDate);
  const baseBasic = (payload.basicParameters ?? {}) as Record<string, unknown>;
  const updatePayload = {
    ...payload,
    savingAsNewVersion: false,
    startDate: currentStartDate,
    basicParameters: {
      ...baseBasic,
      perpetuityDate: null,
    },
    ...overrides,
  };

  const putRes = await Request.put(
    `product-contract/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: updatePayload },
  );
  await expect(putRes).CheckResponse();
  return loadContractAtLatestVersion(Request, contractId);
}

/**
 * Product GET (ProductAdditionalParamsResponse) omits param id — only orderingId/label/value.
 * Contract third-tab-fields returns ProductContractAdditionalParamsResponse with id.
 */
async function resolveFilledProductAdditionalParams(
  fx: Pdt3059CreateFx,
  productId: number,
  productVersionId: number,
  productDetailIdHint?: number,
): Promise<Array<{ id: number; value: string }>> {
  const productRes = await fx.Request.get(
    `${fx.Endpoints.product}/${productId}?version=${productVersionId}`,
  );
  await expect(productRes).CheckResponse();
  const productBody = (await productRes.json()) as {
    detailsId?: number;
    id?: number;
    productAdditionalParams?: Array<Record<string, unknown>>;
  };

  // Prefer contract/product detail id — ProductDetailResponse.id is the product id, not detail.
  const productDetailId = Number(
    productDetailIdHint ?? productBody.detailsId,
  );
  if (!Number.isFinite(productDetailId) || productDetailId <= 0) {
    throw new Error(
      `Cannot resolve productDetailId for product ${productId} ` +
        `(hint=${productDetailIdHint ?? 'n/a'} detailsId=${productBody.detailsId ?? 'n/a'})`,
    );
  }

  const thirdTabRes = await fx.Request.get(
    `product-contract/third-tab-fields?productDetailId=${productDetailId}`,
  );
  await expect(thirdTabRes).CheckResponse();
  const thirdTab = (await thirdTabRes.json()) as {
    productAdditionalParams?: Array<Record<string, unknown>>;
  };

  const fromThirdTab = (thirdTab.productAdditionalParams ?? [])
    .map((param) => ({
      id: Number(param.id),
      value: String(param.value ?? ''),
    }))
    .filter((param) => Number.isFinite(param.id) && param.id > 0 && param.value.length > 0);

  if (fromThirdTab.length > 0) {
    console.log(
      `[PDT-3059] third-tab-fields productDetailId=${productDetailId} filled params=` +
        fromThirdTab.map((p) => `${p.id}=${p.value}`).join(','),
    );
    return fromThirdTab;
  }

  const fromProductGet = (productBody.productAdditionalParams ?? [])
    .map((param) => ({
      id: Number(param.id),
      value: String(param.value ?? PDT_3059_SEED_ADDITIONAL_PARAM.value),
    }))
    .filter((param) => Number.isFinite(param.id) && param.id > 0);

  if (fromProductGet.length > 0) {
    return fromProductGet;
  }

  throw new Error(
    `Cannot seed additional params — product ${productId} (detail=${productDetailId}) ` +
      `has no filled productAdditionalParams with ids (third-tab-fields empty)`,
  );
}

async function seedAdditionalParamsOnContractIfMissing(
  fx: Pdt3059CreateFx,
  contractId: number,
  source: Pdt3059ContractContext['source'] = 'fresh-api-chain',
): Promise<Pdt3059ContractContext> {
  let contract = await loadContractAtLatestVersion(fx.Request, contractId);
  let additionalParamIds = extractAdditionalParamIdsFromContractGet(contract);
  let additionalParamValues: string[] | undefined;

  if (additionalParamIds.length === 0) {
    const basicParameters = (contract.basicParameters ?? {}) as Record<string, unknown>;
    const productId = Number(basicParameters.productId);
    const productVersionId = Number(basicParameters.productVersionId ?? 1);
    const productDetailIdFromContract = Number(basicParameters.productDetailId);
    const filledParams = await resolveFilledProductAdditionalParams(
      fx,
      productId,
      productVersionId,
      Number.isFinite(productDetailIdFromContract) && productDetailIdFromContract > 0
        ? productDetailIdFromContract
        : undefined,
    );
    // Seed with existing product values — Dev rejects "value is already filled and should not be changed".
    const seed = filledParams.slice(0, 1);

    const mapped = buildContractNewVersionPayloadFromGet(contract, futureStartDateFromContract(contract));
    const productParameters = (mapped.payload.productParameters ?? {}) as Record<string, unknown>;
    productParameters.productAdditionalParams = seed.map((param) => ({
      id: param.id,
      value: param.value,
    }));

    contract = await putProductContractRegularUpdate(fx.Request, contractId, {
      productParameters,
    });
    additionalParamIds = extractAdditionalParamIdsFromContractGet(contract);
    if (additionalParamIds.length === 0) {
      additionalParamIds = seed.map((param) => param.id);
      additionalParamValues = seed.map((param) => param.value);
      console.log(
        `[PDT-3059] Contract GET still omits additional params after seed — ` +
          `keeping injected ids ${additionalParamIds.join(',')} for new-version PUT`,
      );
    }
  }

  const basicParameters = (contract.basicParameters ?? {}) as Record<string, unknown>;
  const productId = Number(basicParameters.productId);
  if (!Number.isFinite(productId) || productId <= 0) {
    throw new Error(`Contract ${contractId} GET missing basicParameters.productId`);
  }

  const productRes = await fx.Request.get(`${fx.Endpoints.product}/${productId}?version=1`);
  await expect(productRes).CheckResponse();
  const productBody = (await productRes.json()) as { isIndividual?: boolean };
  if (productBody.isIndividual !== true) {
    throw new Error(`Product ${productId} is not individual (isIndividual=${productBody.isIndividual})`);
  }

  if (additionalParamIds.length === 0) {
    throw new Error(`Contract productId=${productId} has no additional params after seed`);
  }

  const bp = (contract.basicParameters ?? {}) as Record<string, unknown>;
  const contractNumber = String(bp.contractNumber ?? '').trim();
  if (!contractNumber) {
    throw new Error(`Seeded contract ${contractId} GET missing contractNumber`);
  }

  return {
    contractId,
    contractNumber,
    productId,
    latestVersionId: latestLogicalVersionId(contract),
    additionalParamIds,
    additionalParamValues,
    source,
  };
}

async function loadDevPdt3059KnownContract(fx: Pdt3059CreateFx): Promise<Pdt3059ContractContext> {
  const entry = PDT_3059_DEV_CONTRACT_REGISTRY[0];
  const contractId = await findProductContractIdByNumber(fx.Request, entry.contractNumber);
  const contract = await loadContractAtLatestVersion(fx.Request, contractId);
  const basicParameters = (contract.basicParameters ?? {}) as Record<string, unknown>;
  const productId = Number(basicParameters.productId);
  if (!Number.isFinite(productId) || productId <= 0) {
    throw new Error(`Dev contract ${entry.contractNumber} GET missing productId`);
  }

  const productRes = await fx.Request.get(`${fx.Endpoints.product}/${productId}?version=1`);
  await expect(productRes).CheckResponse();
  const productBody = (await productRes.json()) as { isIndividual?: boolean };
  if (productBody.isIndividual !== true) {
    throw new Error(`Dev product ${productId} is not individual`);
  }

  let additionalParamIds = extractAdditionalParamIdsFromContractGet(contract);
  let additionalParamValues: string[] | undefined;
  if (additionalParamIds.length === 0) {
    additionalParamIds = entry.additionalParams.map((p) => p.id);
    additionalParamValues = entry.additionalParams.map((p) => p.value);
    console.log(
      `[PDT-3059] Dev contract ${entry.contractNumber} GET has no additional params — ` +
        `will inject registry params on new-version PUT: ${additionalParamIds.join(',')}`,
    );
  }

  const contractNumber = String(basicParameters.contractNumber ?? entry.contractNumber).trim();
  return {
    contractId,
    contractNumber,
    productId,
    latestVersionId: latestLogicalVersionId(contract),
    additionalParamIds,
    additionalParamValues,
    source: 'known-dev-contract',
  };
}

async function loadDevFallbackPdt3059Contract(fx: Pdt3059CreateFx): Promise<Pdt3059ContractContext> {
  const contractId = await findProductContractIdByNumber(
    fx.Request,
    PDT_3059_DEV_FALLBACK_CONTRACT_NUMBER,
  );
  return seedAdditionalParamsOnContractIfMissing(fx, contractId, 'dev-seeded-contract');
}

async function createFreshPdt3059IndividualProductContract(
  fx: Pdt3059CreateFx,
): Promise<Pdt3059ContractContext> {
  const { customerIdentifier: postedCustomerIdentifier } = await createPdt3059CatalogChain(fx);

  const customerId = Number(fx.Responses.customer[0]?.id);
  const customerGet = await fx.Request.get(`customer/${customerId}?version=1`);
  await expect(customerGet).CheckResponse();
  const customerBody = (await customerGet.json()) as Record<string, unknown>;
  const customerIdentifier = resolveCustomerIdentifier(customerBody, postedCustomerIdentifier);

  const productPayload = fx.GeneratePayload.productAndServices.product();
  applyPdt3059IndividualProductPayload(
    productPayload as Record<string, unknown>,
    customerIdentifier,
  );
  const productBody = JSON.parse(JSON.stringify(productPayload)) as Record<string, unknown>;

  console.log(
    `[PDT-3059] POST /products individual payload keys=` +
      `isIndividual=${productBody.isIndividual} customerIdentifier=${productBody.customerIdentifier} ` +
      `hasGlobalSalesChannel=${'globalSalesChannel' in productBody} ` +
      `hasSalesChannelIds=${'salesChannelIds' in productBody} ` +
      `additionalParams=${JSON.stringify(productBody.productAdditionalParams)}`,
  );

  const productRes = await fx.Request.post(fx.Endpoints.product, { data: productBody });
  await expect(productRes).CheckResponse();
  const productId = Number(await productRes.json());
  fx.Responses.product.push(productId);

  const contractPayload = await fx.GeneratePayload.contractsAndOrders.product_contract();
  // Default generator uses WITHOUT_SUPPLY; individual product only allows COMBINED.
  (contractPayload as { productParameters?: { contractType?: string } }).productParameters =
    (contractPayload as { productParameters?: { contractType?: string } }).productParameters ?? {};
  (contractPayload as { productParameters: { contractType: string } }).productParameters.contractType =
    'COMBINED';

  const contractRes = await fx.Request.post(fx.Endpoints.productContract, { data: contractPayload });
  await expect(contractRes).CheckResponse();
  const contractJson = (await contractRes.json()) as { id?: number };
  fx.Responses.productContract.push(contractJson);
  const contractId = Number(contractJson.id);
  if (!Number.isFinite(contractId) || contractId <= 0) {
    throw new Error('POST product-contract did not return id');
  }

  const seeded = await seedAdditionalParamsOnContractIfMissing(fx, contractId, 'fresh-api-chain');
  console.log(
    `[PDT-3059] fresh contract ${seeded.contractNumber} (id=${seeded.contractId}) productId=${seeded.productId} ` +
      `additionalParamIds=${seeded.additionalParamIds.join(',')} source=${seeded.source}`,
  );
  return seeded;
}

/**
 * Prefer fresh API chain every run. Known env contracts are fallback only.
 */
export async function resolvePdt3059ContractContext(fx: Pdt3059CreateFx): Promise<Pdt3059ContractContext> {
  const env = resolvePdt3059Environment();

  try {
    return await createFreshPdt3059IndividualProductContract(fx);
  } catch (freshErr) {
    const freshMsg = freshErr instanceof Error ? freshErr.message : String(freshErr);
    console.log(`[PDT-3059] fresh-api-chain failed on ${env}: ${freshMsg}`);
  }

  if (env === 'dev') {
    try {
      return await loadDevPdt3059KnownContract(fx);
    } catch (knownErr) {
      const knownMsg = knownErr instanceof Error ? knownErr.message : String(knownErr);
      console.log(
        `[PDT-3059] known Dev contract ${PDT_3059_DEV_KNOWN_CONTRACT_NUMBER} unavailable (${knownMsg})`,
      );
      try {
        return await loadDevFallbackPdt3059Contract(fx);
      } catch (fallbackErr) {
        const fallbackMsg = fallbackErr instanceof Error ? fallbackErr.message : String(fallbackErr);
        throw new Error(
          `[PDT-3059] Dev precondition failed — fresh chain and known contracts unavailable. ` +
            `fresh/known/fallback errors; last=${fallbackMsg}`,
        );
      }
    }
  }

  if (env === 'test') {
    try {
      return await loadKnownPdt3059Contract(
        fx,
        PDT_3059_KNOWN_CONTRACT_NUMBER,
        'known-test-contract',
      );
    } catch (knownErr) {
      const knownMsg = knownErr instanceof Error ? knownErr.message : String(knownErr);
      throw new Error(
        `[PDT-3059] Test precondition failed — fresh chain and known contract ` +
          `${PDT_3059_KNOWN_CONTRACT_NUMBER} unavailable (${knownMsg})`,
      );
    }
  }

  throw new Error(
    `[PDT-3059] Cannot resolve contract context for env=${env} — fresh-api-chain failed and no known fallback.`,
  );
}

export async function putProductContractSavingAsNewVersion(
  Request: APIRequestContext,
  contractId: number,
  options?: {
    staleAdditionalParamIds?: number[];
    additionalParamValues?: string[];
  },
): Promise<Pdt3059NewVersionPutResult> {
  const contract = await loadContractAtLatestVersion(Request, contractId);
  const versionIdBefore = latestLogicalVersionId(contract);
  const versionCountBefore = countContractVersions(contract);
  const startDate = futureStartDateFromContract(contract);
  const { payload, versionId } = buildContractNewVersionPayloadFromGet(contract, startDate);
  let staleAdditionalParamIds = extractAdditionalParamIdsFromContractGet(contract);

  if (staleAdditionalParamIds.length === 0 && options?.staleAdditionalParamIds?.length) {
    staleAdditionalParamIds = options.staleAdditionalParamIds;
    const productParameters = (payload.productParameters ?? {}) as Record<string, unknown>;
    productParameters.productAdditionalParams = staleAdditionalParamIds.map((id, index) => ({
      id,
      // Keep existing values — Dev rejects value mutations on additional params.
      value: options.additionalParamValues?.[index] ?? '124',
    }));
    payload.productParameters = productParameters;
    console.log(
      `[PDT-3059] Injecting additional param ids into new-version PUT: ${staleAdditionalParamIds.join(',')}`,
    );
  }

  console.log(
    `[PDT-3059] Before new-version PUT: contractId=${contractId} ` +
      `versionId=${versionIdBefore} versionCount=${versionCountBefore}`,
  );

  const putRes = await Request.put(
    `product-contract/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`,
    { data: payload },
  );

  const bodyText = await putRes.text();
  const status = putRes.status();
  console.log(`[PDT-3059] new-version PUT status=${status} body=${bodyText.slice(0, 800)}`);

  let versionIdAfter = versionIdBefore;
  let versionCountAfter = versionCountBefore;
  const httpOk = status >= 200 && status < 300;

  if (httpOk) {
    try {
      const afterContract = await loadContractAtLatestVersion(Request, contractId);
      versionIdAfter = latestLogicalVersionId(afterContract);
      versionCountAfter = countContractVersions(afterContract);
    } catch (reloadErr) {
      const reloadMsg = reloadErr instanceof Error ? reloadErr.message : String(reloadErr);
      console.log(
        `[PDT-3059] Failed to reload contract versions after PUT ${status}: ${reloadMsg}`,
      );
    }
  }

  const versionActuallyCreated = didCreateNewContractVersion(
    { versionId: versionIdBefore, versionCount: versionCountBefore },
    { versionId: versionIdAfter, versionCount: versionCountAfter },
  );

  console.log(
    `[PDT-3059] After new-version PUT: versionId ${versionIdBefore}→${versionIdAfter} ` +
      `count ${versionCountBefore}→${versionCountAfter} versionActuallyCreated=${versionActuallyCreated}`,
  );

  if (httpOk && !versionActuallyCreated) {
    console.log(
      `[PDT-3059] PUT returned ${status} but contract version did not increase. ` +
        `body=${bodyText.slice(0, 400)}`,
    );
  }

  return {
    status,
    bodyText,
    hasAdditionalParamNotFoundError: PDT_3059_ADDITIONAL_PARAM_NOT_FOUND_PATTERN.test(bodyText),
    putVersionId: versionId,
    startDate,
    staleAdditionalParamIds,
    versionIdBefore,
    versionIdAfter,
    versionCountBefore,
    versionCountAfter,
    versionActuallyCreated,
  };
}
