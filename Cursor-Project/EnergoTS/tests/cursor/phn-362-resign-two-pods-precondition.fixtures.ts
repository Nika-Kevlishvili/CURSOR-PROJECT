/**
 * PHN-362 — Pre-re-sign data setup.
 *
 * - **Contract B (old):** signed, POD active, ACTIVE_IN_TERM — source for re-sign.
 * - **Contract F (new):** DRAFT only — same POD; sign F later to trigger re-sign.
 *
 * Fixed anchors: term end, version start, POD activation, signing, expected post-resign dates.
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { templates as TemplatesGenerator } from '../../jsons/payloads/create/nomenclatures/templates';
import { addDaysIso } from './contract-resign-new-flow.fixtures';
import { activatePodOnContract, loadProductContract } from './pdt-2815-version-validity.fixtures';

type PrepProductTemplateRefs = {
  templateId: number;
  templateType: 'EMAIL_TEMPLATE' | 'INVOICE_TEMPLATE' | 'CONTRACT_TEMPLATE';
}[];

type FixtureRequest = baseFixture['Request'];
type FixtureResponses = baseFixture['Responses'];
type FixtureEndpoints = baseFixture['Endpoints'];
type FixtureGeneratePayload = baseFixture['GeneratePayload'];

export const IDX_PRODUCT_STD = 0;
export const IDX_PRODUCT_RS = 1;

/** Single POD used on B and F — index in `Responses.pod[]` after preconditions. */
export const PREP_POD_INDEX_FOR_RESIGN = 0;

/** Fixed calendar anchors on contract B (re-sign Wait=Yes + first day of month). */
export const PREP_B_TERM_END = '2026-06-30';
export const PREP_B_VERSION_START = '2026-01-15';

export const PREP_RUNTIME_TODAY = (() => new Date().toISOString().slice(0, 10))();

/**
 * Signing before {@link PREP_POD_ACTIVATION} so manual POD activation can stay 2026-05-01
 * (`activatePodOnContract` clamps activation to max(signing, versionStart)).
 */
export const PREP_B_SIGNING_DATE = addDaysIso(PREP_B_VERSION_START, -14);

/**
 * Dev2 SIGNED PUT requires `entryInForceDate` strictly in the future when the field is sent;
 * omitting the field still fails on this product/term combo — send tomorrow, then SIGNING derives entry on activation.
 */
export const PREP_B_ENTRY_IN_FORCE_SIGN_PUT = addDaysIso(PREP_RUNTIME_TODAY, 1);

/** Initial term on sign PUT (fixed vs term end); POD activation stays {@link PREP_POD_ACTIVATION}. */
export const PREP_B_INITIAL_TERM_SIGN_VALUE = addDaysIso(PREP_B_TERM_END, -6);

/** POD activation = first day of month (not end of month). Same as version start. */
export const PREP_POD_ACTIVATION = PREP_B_VERSION_START;

/** Expected after future re-sign (Wait=Yes + FIRST_DAY_OF_MONTH). */
export const PREP_EXPECTED_AFTER_RESIGN = {
  fActivation: '2026-07-01',
  bDeactivation: '2026-06-30',
} as const;

/** Dev2 employee (Ketevan Nanuashvili) — contract additionalParameters.employeeId */
export const PREP_EMPLOYEE_ID = 1283;

function prepDaysBetween(from: string, to: string): number {
  const a = new Date(`${from}T00:00:00Z`);
  const b = new Date(`${to}T00:00:00Z`);
  return Math.floor((b.getTime() - a.getTime()) / 86_400_000);
}

/** Signing window for F must include runtime today when re-sign is executed later. */
export const PREP_RESIGNING_DEADLINE_DAYS =
  prepDaysBetween(PREP_RUNTIME_TODAY, PREP_B_TERM_END) + 1;

/**
 * SIGNED PUT: keep entry fields off the wire so Phoenix derives entry from signingDate (past signing allowed).
 * Mirrors PHN-362-resign-happy-path.spec.ts — null alone fails when GET/template spread leaves stale values.
 */
/** Dev2-safe SIGNED PUT — omit entry fields (null from GET/template still fails SIGNED validation). */
function buildPrepSignPutBody(signPayload: Record<string, unknown>): Record<string, unknown> {
  const putBody = JSON.parse(JSON.stringify(signPayload)) as Record<string, unknown>;
  const bp = putBody.basicParameters as Record<string, unknown>;
  bp.entryInForceDate = PREP_B_ENTRY_IN_FORCE_SIGN_PUT;
  delete bp.startOfInitialTerm;
  delete bp.statusModifyDate;
  bp.signingDate = PREP_B_SIGNING_DATE;
  bp.contractTermEndDate = PREP_B_TERM_END;
  const pp = putBody.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.entryIntoForce = 'SIGNING';
  delete pp.entryIntoForceValue;
  pp.startOfContractInitialTerm = 'EXACT_DATE';
  pp.startOfContractValue = PREP_B_INITIAL_TERM_SIGN_VALUE;
  pp.contractTermEndDate = PREP_B_TERM_END;
  pp.supplyActivation = 'MANUAL';
  delete pp.supplyActivationValue;
  return putBody;
}

function prepProductTemplatesFromEnv(): PrepProductTemplateRefs {
  return [
    { templateId: envVariables.invoice_email_template, templateType: 'EMAIL_TEMPLATE' },
    { templateId: envVariables.invoice_document_template, templateType: 'INVOICE_TEMPLATE' },
    { templateId: envVariables.product_contract_template, templateType: 'CONTRACT_TEMPLATE' },
  ];
}

/** When envVariables template IDs are stale for BASE_URL, create fresh templates (slow). */
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

/** Happy-path-style edit payload — always nulls entry fields before SIGNED PUT. */
async function buildPrepEditProductContractPayload(
  Request: FixtureRequest,
  contractId: number,
  generatedPayload: Record<string, unknown>,
  opts?: { startDate?: string; signingDate?: string },
): Promise<Record<string, unknown>> {
  const contractget = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractget).CheckResponse();
  const contractgetjson = await contractget.json();

  const podsByBillingGroup = new Map<number, { pointOfDeliveryDetailId: number; dealNumber: unknown }[]>();
  for (const pod of contractgetjson.contractPodsResponses ?? []) {
    const billingGroupId = pod.billingGroupId;
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
  const startDate = opts?.startDate ?? serverStartDate;
  const signingDate = opts?.signingDate ?? PREP_B_SIGNING_DATE;

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
    savingAsNewVersion: false,
    startDate,
    podRequests,
  };

  if (editPayload.additionalParameters) {
    (editPayload.additionalParameters as Record<string, unknown>).riskAssessment = 'PERMIT';
    (editPayload.additionalParameters as Record<string, unknown>).employeeId = PREP_EMPLOYEE_ID;
  }

  return editPayload;
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

function headerContractStatus(body: Record<string, unknown>): string | undefined {
  const bp = (body.basicParameters ?? {}) as { status?: string; contractStatus?: string };
  return bp.status ?? bp.contractStatus;
}

export function findPodRow(body: Record<string, unknown>, podIdentifier: string) {
  return ((body.contractPodsResponses ?? []) as { identifier?: string }[]).find(
    (r) => r.identifier === podIdentifier,
  );
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

async function sharedTermStdPrep(
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
  payload.resigningDeadlineValue = PREP_RESIGNING_DEADLINE_DAYS;
  const res = await Request.post(Endpoints.terms, { data: payload });
  await expect(res).CheckResponse();
  Responses.terms.push(await res.json());
}

const MASS_IMPORT_PHN362_DIR =
  'c:/Users/k.nanuashvili/Desktop/tasks/mass import/phn-362';

type WaitForOldTermValue = 'YES' | 'NO';

async function resolveRsTermWaitForOld(caseId?: string): Promise<WaitForOldTermValue[]> {
  if (!caseId) {
    return ['YES', 'NO'];
  }
  const { pathToFileURL } = await import('url');
  const { rsTermWaitForOldContractTermToExpires } = (await import(
    pathToFileURL(`${MASS_IMPORT_PHN362_DIR}/playwright-data/case-prep-profiles.mjs`).href
  )) as { rsTermWaitForOldContractTermToExpires: (id: string) => WaitForOldTermValue[] };
  return rsTermWaitForOldContractTermToExpires(caseId);
}

async function sharedTermRsPrep(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  waitForOldContractTermToExpires: WaitForOldTermValue[],
) {
  const payload = GeneratePayload.productAndServices.term();
  payload.contractEntryIntoForces = ['MANUAL'];
  payload.startsOfContractInitialTerms = ['MANUAL'];
  payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
  payload.waitForOldContractTermToExpires = waitForOldContractTermToExpires;
  const res = await Request.post(Endpoints.terms, { data: payload });
  await expect(res).CheckResponse();
  const term = (await res.json()) as {
    id: number;
    waitForOldContractTermToExpires?: WaitForOldTermValue[];
  };
  expect(term.waitForOldContractTermToExpires ?? []).toEqual(
    expect.arrayContaining(waitForOldContractTermToExpires),
  );
  expect((term.waitForOldContractTermToExpires ?? []).length).toBe(
    waitForOldContractTermToExpires.length,
  );
  Responses.terms.push(term);
}

async function createProductStdPrep(
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

async function createProductRsPrep(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
) {
  const payload = productPayloadWithoutPriceComponents(GeneratePayload, 1);
  payload.termId = Responses.terms[1].id;
  payload.contractTypes = ['WITHOUT_SUPPLY'];
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
  // Default payload is promotional + preferenceIds — mass-import customer (88888) often lacks those prefs.
  specialOfferPayload.promotionalProduct = false;
  specialOfferPayload.preferenceIds = [];
  specialOfferPayload.applyToSpecificCustomers = false;
  specialOfferPayload.customers = [];
  specialOfferPayload.contracts = [];
  if (Responses.customer.length > 0) {
    const c = Responses.customer[0] as {
      id?: number;
      customerId?: number;
      identifier?: string;
      versionId?: number;
      lastCustomerDetailId?: number;
    };
    const customerId = Number(c.customerId ?? c.id);
    specialOfferPayload.applyToSpecificCustomers = true;
    specialOfferPayload.customers = [
      {
        customerId,
        customerDetailId: c.lastCustomerDetailId,
        name: String(c.identifier ?? customerId),
        versionId: c.versionId ?? 1,
        versionName: String(c.identifier ?? customerId),
        displayDate: new Date().toISOString().slice(0, 10),
      },
    ];
  }
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

async function createPodForResign(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const res = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(res).CheckResponse();
  Responses.pod.push(await res.json());
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

export type PrepContractBResult = {
  contractId: number;
  contractPayload: Record<string, unknown>;
  /** Use this POD identifier on B and F when signing F to trigger re-sign. */
  podIdentifier: string;
  podId: number;
  podDetailId: number;
};

export async function createContractBPrep(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
): Promise<PrepContractBResult> {
  const podIndex = PREP_POD_INDEX_FOR_RESIGN;
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_STD,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.contractTermEndDate = PREP_B_TERM_END;
  bp.startOfInitialTerm = null;
  bp.entryInForceDate = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.contractTermEndDate = PREP_B_TERM_END;
  pp.supplyActivation = 'MANUAL';
  pp.supplyActivationValue = null;
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.startOfContractValue = null;

  const podDetailId = await resolvePodDetailId(Request, Responses, podIndex);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const annualMwh = await annualMwhForPodIndex(Request, Responses, podIndex);
  (contractPayload.additionalParameters as Record<string, unknown>).estimatedTotalConsumptionUnderContractKwh =
    annualMwh;

  console.log('[PHN-362 Prep] contract B POST (1 POD for re-sign):', {
    podIndex,
    podDetailId,
    podIdentifierHint: 'see Responses.pod[0] after run',
    annualMwh,
    contractTermEndDate: PREP_B_TERM_END,
    signingDate: PREP_B_SIGNING_DATE,
    initialTermOnSignPut: PREP_B_INITIAL_TERM_SIGN_VALUE,
    versionStart: PREP_B_VERSION_START,
    podActivation: PREP_POD_ACTIVATION,
  });

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  await test.step('Contract B (old): DRAFT → SIGNED + version start [TC-BE-10]', async () => {
    const signPayload = await buildPrepEditProductContractPayload(Request, contractId, contractPayload, {
      startDate: PREP_B_VERSION_START,
      signingDate: PREP_B_SIGNING_DATE,
    });
    const signBp = signPayload.basicParameters as Record<string, unknown>;
    signBp.status = 'SIGNED';
    signBp.subStatus = 'SIGNED_BY_BOTH_SIDES';
    signBp.versionStatus = 'SIGNED';
    signBp.signingDate = PREP_B_SIGNING_DATE;
    signBp.entryInForceDate = null;

    const signPp = signPayload.productParameters as Record<string, unknown>;
    signPp.contractType = 'SUPPLY_ONLY';
    signPp.entryIntoForce = 'SIGNING';
    signPp.entryIntoForceValue = null;
    signPp.startOfContractInitialTerm = 'EXACT_DATE';
    signPp.startOfContractValue = PREP_B_INITIAL_TERM_SIGN_VALUE;
    signPp.contractTermEndDate = PREP_B_TERM_END;
    signPp.supplyActivation = 'MANUAL';
    signPp.supplyActivationValue = null;

    const ap = signPayload.additionalParameters as Record<string, unknown> | undefined;
    if (ap) {
      ap.employeeId = PREP_EMPLOYEE_ID;
      ap.riskAssessment = 'PERMIT';
    }

    const put = await Request.put(
      `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: buildPrepSignPutBody(signPayload) },
    );
    await expect(put).CheckResponse();
  });

  await test.step('Contract B (old): activate POD (first day of month) [TC-BE-18]', async () => {
    await activatePodOnContract(
      Request,
      Responses,
      contractId,
      1,
      PREP_POD_ACTIVATION,
      podIndex,
    );
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

  const podGet = await Request.get(`pod/${Responses.pod[podIndex].id}`);
  await expect(podGet).CheckResponse();
  const podJson = await podGet.json();

  return {
    contractId,
    contractPayload,
    podIdentifier: String(podJson.identifier),
    podId: Number(Responses.pod[podIndex].id),
    podDetailId,
  };
}

/** New contract F — DRAFT only (Wait=Yes, same POD as B). Re-sign happens when F is signed later. */
export async function createContractFDraftPrep(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
): Promise<{ contractId: number; contractPayload: Record<string, unknown> }> {
  const podIndex = PREP_POD_INDEX_FOR_RESIGN;
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

  const podDetailId = await resolvePodDetailId(Request, Responses, podIndex);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const annualMwh = await annualMwhForPodIndex(Request, Responses, podIndex);
  (contractPayload.additionalParameters as Record<string, unknown>).estimatedTotalConsumptionUnderContractKwh =
    annualMwh;

  console.log('[PHN-362 Prep] contract F POST (draft, same POD as B):', {
    podIndex,
    podDetailId,
    annualMwh,
    supplyActivation: pp.supplyActivation,
    productContractWaitForOldContractTermToExpires: pp.productContractWaitForOldContractTermToExpires,
  });

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);

  return { contractId: createJson.id as number, contractPayload };
}

/**
 * Mass-import old-contract prep: STD product (old B), RS product with STD linked, customer, POD.
 * Used by PHN-362-mass-import-old-contract.spec.ts — no contract B/F here.
 */
async function loadMassImportCustomerPrep(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
) {
  const riskListCustomerId = process.env.MASS_IMPORT_RISKLIST_CUSTOMER_ID?.trim();
  if (riskListCustomerId) {
    const expectedUic = process.env.MASS_IMPORT_RISKLIST_UIC?.trim() ?? '88888';
    const getRes = await Request.get(`${Endpoints.customer}/${riskListCustomerId}`);
    await expect(getRes).CheckResponse();
    const body = (await getRes.json()) as {
      id?: number;
      customerId?: number;
      identifier?: string;
      versionId?: number;
      lastCustomerDetailId?: number;
    };
    expect(String(body.identifier)).toBe(expectedUic);
    const customerId = Number(body.customerId ?? body.id ?? riskListCustomerId);
    Responses.customer.push({
      // product_contract() reads Responses.customer[n].id — GET body may only expose customerId
      id: customerId,
      customerId,
      identifier: body.identifier,
      versionId: body.versionId,
      lastCustomerDetailId: body.lastCustomerDetailId,
    });
    return;
  }
  const res = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(res).CheckResponse();
  Responses.customer.push(await res.json());
}

export type MassImportEntityPreconditionOptions = {
  /** Mass-import case id (01–06) — drives RS term wait-for-old checkboxes. */
  caseId?: string;
};

export async function runMassImportOldContractEntityPreconditions(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
  options?: MassImportEntityPreconditionOptions,
) {
  const rsTermWait = await resolveRsTermWaitForOld(options?.caseId);
  await test.step('Old-contract product (P-STD): term + product SUPPLY_ONLY', async () => {
    await sharedTermStdPrep(Request, GeneratePayload, Responses, Endpoints);
    await createProductStdPrep(Request, GeneratePayload, Responses, Endpoints, FileUploadRequest);
  });
  await test.step('Customer (legal entity) — before RS special offers', async () => {
    await loadMassImportCustomerPrep(Request, GeneratePayload, Responses, Endpoints);
  });
  await test.step(
    `Re-sign product (P-RS): term wait=[${rsTermWait.join(',')}] + product WITHOUT_SUPPLY`,
    async () => {
      await sharedTermRsPrep(
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        rsTermWait,
      );
      await createProductRsPrep(Request, GeneratePayload, Responses, Endpoints, FileUploadRequest);
    },
  );
  await test.step('POD (single — shared by old B and mass-import F)', async () => {
    await createPodForResign(Request, GeneratePayload, Responses, Endpoints);
  });
}

export async function runSharedPreconditionsPrep(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
) {
  await runMassImportOldContractEntityPreconditions(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  );
}

export async function assertPreResignState(
  Request: FixtureRequest,
  contractBId: number,
  contractFId: number,
  podIdentifier: string,
) {
  await test.step('Assert: contract B ready (pre-re-sign)', async () => {
    const bBody = await loadProductContract(Request, contractBId);
    const bp = bBody.basicParameters as {
      status?: string;
      signingDate?: string;
      entryInForceDate?: string;
      contractTermEndDate?: string;
      activationDate?: string;
      resignedFrom?: unknown[];
      resignedTo?: unknown[];
    };

    expect(bp.status).toBe('ACTIVE_IN_TERM');
    expect(bp.signingDate).toBe(PREP_B_SIGNING_DATE);
    expect(bp.entryInForceDate).toBeTruthy();
    expect(String(bp.entryInForceDate) <= PREP_B_TERM_END).toBeTruthy();
    expect(bp.contractTermEndDate).toBe(PREP_B_TERM_END);
    expect(bp.resignedFrom ?? []).toHaveLength(0);
    expect(bp.resignedTo ?? []).toHaveLength(0);

    if (bp.activationDate) {
      expect(bp.activationDate).toBe(PREP_POD_ACTIVATION);
    }

    const versions = (bBody.versions ?? []) as { versionId?: number; startDate?: string }[];
    const v1 = versions.find((v) => Number(v.versionId) === 1);
    expect(v1?.startDate).toBe(PREP_B_VERSION_START);

    const pods = (bBody.contractPodsResponses ?? []) as unknown[];
    expect(pods).toHaveLength(1);

    const row = findPodRow(bBody, podIdentifier) as {
      activationDate?: string;
      deactivationDate?: string | null;
      isResigned?: boolean | null;
    };
    expect(row?.activationDate).toBe(PREP_POD_ACTIVATION);
    expect(row?.deactivationDate == null || row?.deactivationDate === '').toBeTruthy();
    expect(row?.isResigned).not.toBe(true);

    const available = (bBody as { availableForResigning?: boolean }).availableForResigning;
    if (available !== undefined) {
      expect(available).toBe(true);
    }
  });

  await test.step('Assert: contract F draft ready (NOT signed, no re-sign yet)', async () => {
    const fBody = await loadProductContract(Request, contractFId);
    const bp = fBody.basicParameters as {
      status?: string;
      subStatus?: string;
      signingDate?: string | null;
      resignedFrom?: unknown[];
      resignedTo?: unknown[];
    };

    expect(bp.status).toBe('DRAFT');
    expect(bp.subStatus).toBe('DRAFT');
    expect(bp.signingDate == null || bp.signingDate === '').toBeTruthy();
    expect(bp.resignedFrom ?? []).toHaveLength(0);
    expect(bp.resignedTo ?? []).toHaveLength(0);

    const fPods = (fBody.contractPodsResponses ?? []) as { identifier?: string }[];
    expect(fPods).toHaveLength(1);
    expect(fPods[0]?.identifier).toBe(podIdentifier);

    const pp = (fBody.productParameters ?? {}) as {
      supplyActivation?: string;
      productContractWaitForOldContractTermToExpires?: string;
    };
    expect(pp.supplyActivation).toBe('FIRST_DAY_OF_MONTH');
    expect(pp.productContractWaitForOldContractTermToExpires).toBe('YES');
  });
}
