/**
 * Shared helpers for RPS-DUEDATE — invoice payment deadline calculation for RPS PODs.
 * Reference: Confluence 953581569 (AC1–AC1.5, AC2, AC4, AC9)
 * Environment: dev2 | Swagger: Cursor-Project/config/swagger/dev2/swagger-spec.json
 */

import { test, expect } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';

export const BILLING_RUN_ROOT = 'billing-run';
const POLL_MS = 15_000;
const POLL_MAX_MS = 8 * 60 * 1000;
const STATUS_POLL_MAX_MS = 3 * 60 * 1000;

export type RpsDueDateFx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

// ─── RPS NOMENCLATURE ──────────────────────────────────────────────────────

export async function createRpsNomenclature(
  Request: RpsDueDateFx['Request'],
  opts: { rpsNumber: string; dayOfMonth: number },
): Promise<number> {
  // Swagger RpsNumberRequest: defaultSelection (boolean), status, rpsNumber, dayOfMonth.
  // Suffix keeps reruns unique; dayOfMonth is the value the due-date rule uses.
  const rpsNumber = `${opts.rpsNumber}-${Date.now().toString(36)}-${Math.floor(Math.random() * 1000)}`;
  const res = await Request.post('rps-numbers', {
    data: {
      rpsNumber,
      dayOfMonth: opts.dayOfMonth,
      defaultSelection: false,
      status: 'ACTIVE',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { id: number };
  return body.id;
}

// ─── CALENDAR ──────────────────────────────────────────────────────────────

/**
 * Creates a default calendar (isDefault=true, SAT+SUN weekends).
 * Optionally adds holidays via PUT /calendar/{id} after creation.
 * @param holidays ISO date strings (e.g. ['2025-06-06']) to add as NATIONAL holidays
 */
export async function createDefaultCalendar(
  Request: RpsDueDateFx['Request'],
  holidays?: string[],
): Promise<number> {
  // Swagger CalendarRequest: name, defaultSelection, weekends[] (WeekDay enum), holidays[].holidayDate.
  const res = await Request.post('calendar', {
    data: {
      name: `RPS-CAL-${Date.now().toString(36)}-${Math.floor(Math.random() * 1000)}`,
      defaultSelection: true,
      weekends: ['SATURDAY', 'SUNDAY'],
      status: 'ACTIVE',
      holidays: (holidays ?? []).map((holidayDate) => ({
        holidayDate,
        holidayStatus: 'ACTIVE',
      })),
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { id: number };
  return body.id;
}

/** Creates a non-default calendar (no weekends configured) for TC-BE-21 isolation test. */
export async function createNonDefaultCalendar(
  Request: RpsDueDateFx['Request'],
): Promise<number> {
  const res = await Request.post('calendar', {
    data: {
      name: `RPS-CAL-ND-${Date.now().toString(36)}-${Math.floor(Math.random() * 1000)}`,
      defaultSelection: false,
      weekends: [],
      status: 'ACTIVE',
    },
  });
  await expect(res).CheckResponse();
  return ((await res.json()) as { id: number }).id;
}

// ─── ENTITY CREATORS ───────────────────────────────────────────────────────

export async function createTerm(fx: RpsDueDateFx): Promise<void> {
  const res = await fx.Request.post(fx.Endpoints.terms, {
    data: fx.GeneratePayload.productAndServices.term(),
  });
  await expect(res).CheckResponse();
  fx.Responses.terms.push(await res.json());
}

export async function createElectricityPriceComponent(
  fx: RpsDueDateFx,
  opts?: { condition?: string; applicationModelType?: string[] },
): Promise<void> {
  const models = opts?.applicationModelType ?? ['FOR_VOLUMES'];
  const payloads: Array<{ formulaRequest: { condition: string | null } }> = [];
  if (models.includes('FOR_VOLUMES') || models.includes('WITH_ELECTRICITY_INVOICE')) {
    payloads.push(fx.GeneratePayload.productAndServices.priceSettlement() as { formulaRequest: { condition: string | null } });
  }
  if (models.includes('WITH_ELECTRICITY_INVOICE') || (!models.includes('FOR_VOLUMES') && !models.includes('OVER_TIME_PERIODICAL') && !models.includes('OVER_TIME_ONE_TIME') && !models.includes('INTERIM_AND_ADVANCE_PAYMENT'))) {
    payloads.push(fx.GeneratePayload.productAndServices.electricity() as { formulaRequest: { condition: string | null } });
  }
  if (models.includes('OVER_TIME_PERIODICAL')) {
    const periodical = JSON.parse(JSON.stringify(fx.GeneratePayload.productAndServices.periodicalComponent())) as {
      formulaRequest: { condition: string | null };
      applicationModelRequest: { applicationLevel: string };
    };
    periodical.applicationModelRequest.applicationLevel = 'POD';
    payloads.push(periodical);
  }
  if (models.includes('OVER_TIME_ONE_TIME')) {
    const oneTime = JSON.parse(JSON.stringify(fx.GeneratePayload.productAndServices.oneTime())) as {
      formulaRequest: { condition: string | null };
      applicationModelRequest: { applicationLevel: string };
    };
    oneTime.applicationModelRequest.applicationLevel = 'POD';
    payloads.push(oneTime);
  }
  if (models.includes('INTERIM_AND_ADVANCE_PAYMENT')) {
    payloads.push(fx.GeneratePayload.productAndServices.interim() as { formulaRequest: { condition: string | null } });
  }
  if (payloads.length === 0) {
    payloads.push(fx.GeneratePayload.productAndServices.priceSettlement() as { formulaRequest: { condition: string | null } });
  }
  for (const payload of payloads) {
  // Swagger PriceComponentFormulaRequest.condition — e.g. $POD_VOLTAGE_LEVEL$=$LOW$
    if (opts?.condition && payload.formulaRequest) payload.formulaRequest.condition = opts.condition;
    const res = await fx.Request.post(fx.Endpoints.priceComponent, { data: payload });
    await expect(res).CheckResponse();
    fx.Responses.priceComponent.push(await res.json());
  }
}

export async function createProduct(fx: RpsDueDateFx): Promise<void> {
  const payload = fx.GeneratePayload.productAndServices.product() as Record<string, unknown>;
  payload.contractTypes = ['SUPPLY_ONLY'];
  payload.paymentGuarantees = ['NO'];
  const res = await fx.Request.post(fx.Endpoints.product, { data: payload });
  await expect(res).CheckResponse();
  fx.Responses.product.push(await res.json());
}

const LOCAL_ADDRESS_TRANSLATION = {
  country: 'STANDARD COUNTRY',
  region: 'STANDARD REGION',
  municipality: 'STANDARD MUNICIPALITY',
  populatedPlace: 'STANDARD POPULATED PLACE',
  zipCode: 'STANDARD ZIP CODE',
  district: 'STANDARD DISTRICT',
  residentialArea: 'STANDARD RESIDENTIAL AREA',
  residentialAreaType: 'QUARTER',
  street: 'STANDARD STREET',
  streetType: 'STREET',
};

function transliteratedLocalAddress(sourceLocal: Record<string, unknown> | undefined) {
  const local = sourceLocal ?? {};
  const transl: Record<string, string> = {
    country: LOCAL_ADDRESS_TRANSLATION.country,
    region: LOCAL_ADDRESS_TRANSLATION.region,
    municipality: LOCAL_ADDRESS_TRANSLATION.municipality,
    populatedPlace: LOCAL_ADDRESS_TRANSLATION.populatedPlace,
    zipCode: LOCAL_ADDRESS_TRANSLATION.zipCode,
    street: LOCAL_ADDRESS_TRANSLATION.street,
    streetType: typeof local.streetType === 'string' ? local.streetType : LOCAL_ADDRESS_TRANSLATION.streetType,
  };
  if (local.districtId != null) transl.district = LOCAL_ADDRESS_TRANSLATION.district;
  if (local.residentialAreaId != null) {
    transl.residentialArea = LOCAL_ADDRESS_TRANSLATION.residentialArea;
    transl.residentialAreaType =
      typeof local.residentialAreaType === 'string'
        ? local.residentialAreaType
        : LOCAL_ADDRESS_TRANSLATION.residentialAreaType;
  }
  return transl;
}

/** phoenix2-dev rejects POST /customer when a filled address field has no uppercase transliteration. */
function withAddressTransliteration(payload: Record<string, any>): Record<string, any> {
  const address = payload.address ?? {};
  payload.addressTransl = {
    foreign: false,
    localAddressData: transliteratedLocalAddress(address.localAddressData),
    number: address.number ?? '122',
  };
  if (Array.isArray(payload.communicationData)) {
    for (const entry of payload.communicationData) {
      if (!entry?.address || entry.addressTransliterated != null) continue;
      entry.addressTransliterated = {
        foreign: false,
        localAddressData: transliteratedLocalAddress(entry.address.localAddressData),
        number: entry.address.number ?? '122',
      };
    }
  }
  return payload;
}

export async function createCustomer(fx: RpsDueDateFx): Promise<void> {
  const res = await fx.Request.post(fx.Endpoints.customer, {
    data: withAddressTransliteration(fx.GeneratePayload.customers.customer_legal() as Record<string, any>),
  });
  await expect(res).CheckResponse();
  fx.Responses.customer.push(await res.json());
}

export type PodVoltageLevel = 'LOW' | 'MEDIUM' | 'MEDIUM_DIRECT_CONNECTED' | 'HIGH';

/**
 * Creates a POD. Pass rpsNumberId to make it an RPS POD, or null for non-RPS.
 */
export async function createPod(
  fx: RpsDueDateFx,
  rpsNumberId: number | null,
  opts?: { voltageLevel?: PodVoltageLevel },
): Promise<void> {
  const payload = fx.GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
  payload.rpsNumberId = rpsNumberId;
  if (opts?.voltageLevel) payload.voltageLevel = opts.voltageLevel;
  const res = await fx.Request.post(fx.Endpoints.pod, { data: payload });
  await expect(res).CheckResponse();
  fx.Responses.pod.push(await res.json());
}

export async function createProductContract(fx: RpsDueDateFx, podIndex?: number): Promise<void> {
  const payload = (await fx.GeneratePayload.contractsAndOrders.product_contract(0, 0, podIndex)) as {
    productParameters: { contractType: string; invoicePaymentTermValue: number };
    basicParameters: {
      status: string;
      subStatus: string;
      versionStatus: string;
      signingDate: string | null;
      entryInForceDate: string | null;
      startOfInitialTerm: string | null;
    };
  };
  // Product allows SUPPLY_ONLY only. SIGNED rejects a past entry date.
  // ENTERED_INTO_FORCE accepts an entry date of today or earlier (ProductContractDateService).
  payload.productParameters.contractType = 'SUPPLY_ONLY';
  payload.basicParameters.status = 'ENTERED_INTO_FORCE';
  payload.basicParameters.subStatus = 'AWAITING_ACTIVATION';
  payload.basicParameters.versionStatus = 'SIGNED';
  payload.basicParameters.signingDate = '2025-01-01';
  payload.basicParameters.entryInForceDate = '2025-01-01';
  payload.basicParameters.startOfInitialTerm = '2025-01-01';
  payload.productParameters = {
    ...payload.productParameters,
    invoicePaymentTermValue: 15,
  };
  const res = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  await expect(res).CheckResponse();
  fx.Responses.productContract.push(await res.json());
}

export async function activatePod(fx: RpsDueDateFx, podIndex: number = 0): Promise<void> {
  const res = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(podIndex),
  });
  await expect(res).CheckResponse();
}

export async function createBillingProfile(fx: RpsDueDateFx, podIndex: number = 0): Promise<void> {
  const payload = await fx.GeneratePayload.energyData.profile1Month(podIndex);
  payload.timeZone = 'CET';
  const res = await fx.Request.post('billing-by-profile', { data: payload });
  await expect(res).CheckResponse();
  const data = await res.json();
  fx.Responses.dataByProfiles.push({
    id: data,
    periodFrom: payload.periodFrom,
    periodTo: payload.periodTo,
    periodType: payload.periodType,
  });
}

// ─── BILLING RUN CREATION ──────────────────────────────────────────────────

export type BillingRunOpts = {
  applicationModelType: string[];
  invoiceDate: string;
  /** "ACCORDING_TO_THE_CONTRACT" or "DATE" */
  invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT' | 'DATE';
  /** Required when invoiceDueDateType=DATE */
  dueDate?: string;
  contractIndex?: number;
  billingType?: string;
};

export function asBillingRunId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw;
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const id = (raw as { id: unknown }).id;
    if (typeof id === 'number') return id;
  }
  throw new Error(`[RPS-DUEDATE] Cannot extract billingRunId from: ${JSON.stringify(raw)}`);
}

export async function createBillingRun(
  fx: RpsDueDateFx,
  opts: BillingRunOpts,
): Promise<number> {
  const payload = (await fx.GeneratePayload.billing.billingRun(
    'CONTRACT',
    opts.applicationModelType as any,
    0,
    opts.contractIndex ?? 0,
  )) as Record<string, unknown>;

  const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
  cp.invoiceDate = opts.invoiceDate;
  if (opts.invoiceDueDateType === 'ACCORDING_TO_THE_CONTRACT') {
    cp.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
    cp.dueDate = null;
  } else {
    cp.invoiceDueDate = 'DATE';
    cp.dueDate = opts.dueDate ?? null;
  }
  payload.commonParameters = cp;

  if (opts.billingType) {
    payload.billingType = opts.billingType;
  }

  const res = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const billingRunId = asBillingRunId(await res.json());
  fx.Responses.billingRun.push(billingRunId);
  return billingRunId;
}

// ─── BILLING RUN LIFECYCLE ─────────────────────────────────────────────────

export async function getBillingRunStatus(
  Request: RpsDueDateFx['Request'],
  billingRunId: number,
): Promise<string> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const res = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
      await expect(res).CheckResponse();
      const body = await res.json();
      return (body.commonParameters?.status as string) ?? '';
    } catch (err) {
      lastError = err;
      const message = String(err);
      const transient = /ETIMEDOUT|ECONNRESET|ECONNREFUSED|socket hang up|Timeout/i.test(message);
      if (!transient || attempt === 3) break;
      await new Promise((r) => setTimeout(r, 5_000));
    }
  }
  throw lastError;
}

export async function pollBillingRunForStatus(
  Request: RpsDueDateFx['Request'],
  billingRunId: number,
  targetStatus: string,
  timeoutMs: number = POLL_MAX_MS,
): Promise<void> {
  const maxAttempts = Math.max(1, Math.ceil(timeoutMs / POLL_MS));
  for (let i = 1; i <= maxAttempts; i++) {
    const status = await getBillingRunStatus(Request, billingRunId);
    if (status === targetStatus) return;
    if (i < maxAttempts) await new Promise((r) => setTimeout(r, POLL_MS));
  }
  const final = await getBillingRunStatus(Request, billingRunId);
  throw new Error(
    `[RPS-DUEDATE] Billing run ${billingRunId} did not reach ${targetStatus} within ${timeoutMs}ms; final status: ${final}`,
  );
}

/** PATCH start-billing and returns the raw response (caller checks status for negative tests). */
export async function patchStartBilling(
  Request: RpsDueDateFx['Request'],
  billingRunId: number,
) {
  return Request.patch(`${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`);
}

/** Full start-billing → DRAFT poll → start-generating → GENERATED poll → start-accounting → COMPLETED poll. */
export async function startAndCompleteBillingRun(
  Request: RpsDueDateFx['Request'],
  billingRunId: number,
): Promise<void> {
  const startRes = await patchStartBilling(Request, billingRunId);
  await expect(startRes).CheckResponse();

  await pollBillingRunForStatus(Request, billingRunId, 'DRAFT');

  const genRes = await Request.patch(`${BILLING_RUN_ROOT}/start-generating?billingRunId=${billingRunId}`);
  await expect(genRes).CheckResponse();

  await pollBillingRunForStatus(Request, billingRunId, 'GENERATED', STATUS_POLL_MAX_MS);

  const accRes = await Request.patch(`${BILLING_RUN_ROOT}/start-accounting?billingRunId=${billingRunId}`);
  await expect(accRes).CheckResponse();

  await pollBillingRunForStatus(Request, billingRunId, 'COMPLETED', STATUS_POLL_MAX_MS);
}

/** start-billing → DRAFT poll → start-generating (no accounting for draft-invoice inspection). */
export async function startBillingRunToDraft(
  Request: RpsDueDateFx['Request'],
  billingRunId: number,
): Promise<void> {
  const startRes = await patchStartBilling(Request, billingRunId);
  await expect(startRes).CheckResponse();
  await pollBillingRunForStatus(Request, billingRunId, 'DRAFT');
  const genRes = await Request.patch(`${BILLING_RUN_ROOT}/start-generating?billingRunId=${billingRunId}`);
  await expect(genRes).CheckResponse();
}

// ─── INVOICE LISTING ───────────────────────────────────────────────────────

async function getBillingNumber(Request: RpsDueDateFx['Request'], billingRunId: number): Promise<string> {
  const res = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
  await expect(res).CheckResponse();
  const body = await res.json();
  const bn = body.commonParameters?.billingNumber as string | undefined;
  if (!bn) throw new Error(`[RPS-DUEDATE] billingNumber missing for billingRunId=${billingRunId}`);
  return bn;
}

export async function listInvoices(
  Request: RpsDueDateFx['Request'],
  billingRunId: number,
): Promise<Record<string, unknown>[]> {
  const billingNumber = await getBillingNumber(Request, billingRunId);
  const res = await Request.post('invoice/listing', {
    data: { page: 0, size: 50, searchBy: 'BILLING_RUN', billingRun: billingNumber },
  });
  await expect(res).CheckResponse();
  const body = await res.json();
  const rows = (body.content ?? []) as Record<string, unknown>[];
  // InvoiceListingResponse has no payment deadline. InvoiceResponse.paymentDeadLine does.
  for (const row of rows) {
    if (typeof row.id !== 'number') continue;
    const detailRes = await Request.get(`invoice?id=${row.id}`);
    await expect(detailRes).CheckResponse();
    const detail = (await detailRes.json()) as { paymentDeadLine?: string | null; paymentDeadline?: string | null };
    row.paymentDeadline = detail.paymentDeadLine ?? detail.paymentDeadline ?? null;
  }
  return rows;
}

/** Polls invoice/listing until expectedCount invoices appear (or timeout). */
export async function pollInvoiceListing(
  Request: RpsDueDateFx['Request'],
  billingRunId: number,
  expectedCount: number = 1,
): Promise<Record<string, unknown>[]> {
  const maxAttempts = Math.ceil((2 * 60 * 1000) / POLL_MS);
  for (let i = 1; i <= maxAttempts; i++) {
    const rows = await listInvoices(Request, billingRunId);
    if (rows.length >= expectedCount) return rows;
    if (i < maxAttempts) await new Promise((r) => setTimeout(r, POLL_MS));
  }
  return listInvoices(Request, billingRunId);
}

/** Returns the paymentDeadline string (or null for credit notes) from the first invoice. */
export async function getFirstInvoicePaymentDeadline(
  Request: RpsDueDateFx['Request'],
  billingRunId: number,
): Promise<string | null> {
  const rows = await pollInvoiceListing(Request, billingRunId, 1);
  if (rows.length === 0) throw new Error(`[RPS-DUEDATE] No invoices found for billingRunId=${billingRunId}`);
  return (rows[0].paymentDeadline as string | null) ?? null;
}

// ─── CONVENIENCE: FULL SIMPLE SINGLE-POD CHAIN ─────────────────────────────

export type SimpleChainOpts = BillingRunOpts;

export type SimpleChainResult = {
  billingRunId: number;
};

/**
 * Creates the full entity chain for single-POD tests:
 * term → price component → product → customer → POD (with rpsNumberId) →
 * product contract → POD activation → billing profile → billing run.
 */
export async function createSimpleRpsChain(
  fx: RpsDueDateFx,
  rpsNumberId: number | null,
  opts: SimpleChainOpts,
): Promise<SimpleChainResult> {
  await test.step('Precondition: term', async () => createTerm(fx));
  await test.step('Precondition: price component', async () =>
    createElectricityPriceComponent(fx, { applicationModelType: opts.applicationModelType }));
  await test.step('Precondition: product', async () => createProduct(fx));
  await test.step('Precondition: customer', async () => createCustomer(fx));
  await test.step('Precondition: POD', async () => createPod(fx, rpsNumberId));
  await test.step('Precondition: product contract', async () => createProductContract(fx));
  await test.step('Precondition: activate POD', async () => activatePod(fx));
  await test.step('Precondition: billing profile', async () => createBillingProfile(fx));

  let billingRunId = 0;
  await test.step('Precondition: billing run', async () => {
    billingRunId = await createBillingRun(fx, opts);
  });

  return { billingRunId };
}

// ─── MULTI-POD HELPERS ─────────────────────────────────────────────────────

export type MultiPodResult = {
  billingRunId: number;
  pod1Id: number;
  pod2Id: number;
};

/**
 * Two PODs, SAME billing group (both in same invoice).
 * Uses the default product_contract() which adds all pods to a single billing group.
 */
export type SameBillingGroupOpts = {
  pod1Voltage?: PodVoltageLevel;
  pod2Voltage?: PodVoltageLevel;
  /** PriceComponentFormulaRequest.condition — excludes PODs that do not match. */
  priceCondition?: string;
};

export async function createTwoPodSameBillingGroupChain(
  fx: RpsDueDateFx,
  rpsNumberId1: number | null,
  rpsNumberId2: number | null,
  opts: SimpleChainOpts,
  podOpts?: SameBillingGroupOpts,
): Promise<MultiPodResult> {
  await test.step('Precondition: term', async () => createTerm(fx));
  await test.step('Precondition: price component', async () =>
    createElectricityPriceComponent(fx, {
      condition: podOpts?.priceCondition,
      applicationModelType: opts.applicationModelType,
    }));
  await test.step('Precondition: product', async () => createProduct(fx));
  await test.step('Precondition: customer', async () => createCustomer(fx));

  let pod1Id = 0;
  await test.step('Precondition: POD #1', async () => {
    await createPod(fx, rpsNumberId1, { voltageLevel: podOpts?.pod1Voltage });
    pod1Id = (fx.Responses.pod[0] as any).id;
  });

  let pod2Id = 0;
  await test.step('Precondition: POD #2', async () => {
    await createPod(fx, rpsNumberId2, { voltageLevel: podOpts?.pod2Voltage });
    pod2Id = (fx.Responses.pod[1] as any).id;
  });

  // product_contract() with no podIndex adds ALL pods — both in one billing group
  await test.step('Precondition: product contract (both PODs, one billing group)', async () =>
    createProductContract(fx));

  await test.step('Precondition: activate POD #1', async () => activatePod(fx, 0));
  await test.step('Precondition: activate POD #2', async () => activatePod(fx, 1));
  await test.step('Precondition: billing profile POD #1', async () => createBillingProfile(fx, 0));
  await test.step('Precondition: billing profile POD #2', async () => createBillingProfile(fx, 1));

  let billingRunId = 0;
  await test.step('Precondition: billing run', async () => {
    billingRunId = await createBillingRun(fx, opts);
  });

  return { billingRunId, pod1Id, pod2Id };
}

/**
 * Two PODs, SEPARATE billing groups.
 * POD 1 → contract (billing group 1); second billing group added; POD 2 → billing group 2.
 */
export async function createTwoPodSeparateBillingGroupsChain(
  fx: RpsDueDateFx,
  rpsNumberId1: number | null,
  rpsNumberId2: number | null,
  opts: SimpleChainOpts,
): Promise<MultiPodResult> {
  await test.step('Precondition: term', async () => createTerm(fx));
  await test.step('Precondition: price component', async () =>
    createElectricityPriceComponent(fx, { applicationModelType: opts.applicationModelType }));
  await test.step('Precondition: product', async () => createProduct(fx));
  await test.step('Precondition: customer', async () => createCustomer(fx));

  let pod1Id = 0;
  await test.step('Precondition: POD #1', async () => {
    await createPod(fx, rpsNumberId1);
    pod1Id = (fx.Responses.pod[0] as any).id;
  });

  // Create contract with only POD 1 (podIndex=0 → only first pod)
  await test.step('Precondition: product contract (POD #1 only, billing group 1)', async () =>
    createProductContract(fx, 0));

  // Add a second billing group to the contract
  await test.step('Precondition: add second billing group', async () => {
    const bgPayload = await fx.GeneratePayload.contractsAndOrders.addBillingGroup(0);
    const bgRes = await fx.Request.post(fx.Endpoints.billingGroup, { data: bgPayload });
    await expect(bgRes).CheckResponse();
  });

  await test.step('Precondition: activate POD #1', async () => activatePod(fx, 0));
  await test.step('Precondition: billing profile POD #1', async () => createBillingProfile(fx, 0));

  // Create POD 2 and add to billing group 2
  let pod2Id = 0;
  await test.step('Precondition: POD #2', async () => {
    await createPod(fx, rpsNumberId2);
    pod2Id = (fx.Responses.pod[1] as any).id;
  });

  await test.step('Precondition: billing profile POD #2', async () => createBillingProfile(fx, 1));

  await test.step('Precondition: add POD #2 to billing group 2', async () => {
    const contractPayload = await fx.GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    const editPayload = await fx.GeneratePayload.contractsAndOrders.edit_ProductContract(
      contractPayload as any,
    );
    (editPayload as any).basicParameters.entryInForceDate = '2025-01-01';
    if ((editPayload as any).productParameters) {
      (editPayload as any).productParameters.contractType = 'SUPPLY_ONLY';
    }
    const newPodInfo = await fx.GeneratePayload.contractsAndOrders.addPodToBillingGroup(
      1,
      fx.Responses.pod[1].podDetailId,
    );
    (editPayload as any).podRequests.push((newPodInfo as any).addPod);
    (editPayload as any).productContractPointOfDeliveries.push(
      (newPodInfo as any).addPod.productContractPointOfDeliveries[0],
    );
    (editPayload as any).additionalParameters.estimatedTotalConsumptionUnderContractKwh =
      (newPodInfo as any).estimatedConsumption;
    const contractId = (fx.Responses.productContract[0] as any).id;
    const editRes = await fx.Request.put(
      `product-contract/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: editPayload },
    );
    await expect(editRes).CheckResponse();
  });

  await test.step('Precondition: activate POD #2', async () => activatePod(fx, 1));

  let billingRunId = 0;
  await test.step('Precondition: billing run', async () => {
    billingRunId = await createBillingRun(fx, opts);
  });

  return { billingRunId, pod1Id, pod2Id };
}

/**
 * PHN-3531 TC-BE-25 V2.
 * Billing group 1 holds two PODs with different RPS numbers (conflict invoice).
 * Billing group 2 holds one additional RPS POD (invoice must be generated).
 */
export async function createConflictGroupPlusSeparateRpsPodChain(
  fx: RpsDueDateFx,
  rpsConflict1: number,
  rpsConflict2: number,
  rpsSeparate: number,
  opts: SimpleChainOpts,
): Promise<MultiPodResult & { pod3Id: number }> {
  await test.step('Precondition: term', async () => createTerm(fx));
  await test.step('Precondition: price component', async () =>
    createElectricityPriceComponent(fx, { applicationModelType: opts.applicationModelType }));
  await test.step('Precondition: product', async () => createProduct(fx));
  await test.step('Precondition: customer', async () => createCustomer(fx));

  let pod1Id = 0;
  let pod2Id = 0;
  await test.step('Precondition: POD #1 and POD #2 (conflict pair, billing group 1)', async () => {
    await createPod(fx, rpsConflict1);
    await createPod(fx, rpsConflict2);
    pod1Id = (fx.Responses.pod[0] as { id: number }).id;
    pod2Id = (fx.Responses.pod[1] as { id: number }).id;
  });

  await test.step('Precondition: product contract (both conflict PODs, one billing group)', async () =>
    createProductContract(fx));

  await test.step('Precondition: add second billing group', async () => {
    const bgPayload = await fx.GeneratePayload.contractsAndOrders.addBillingGroup(0);
    const bgRes = await fx.Request.post(fx.Endpoints.billingGroup, { data: bgPayload });
    await expect(bgRes).CheckResponse();
  });

  await test.step('Precondition: activate conflict PODs', async () => {
    await activatePod(fx, 0);
    await activatePod(fx, 1);
  });
  await test.step('Precondition: billing profiles for conflict PODs', async () => {
    await createBillingProfile(fx, 0);
    await createBillingProfile(fx, 1);
  });

  let pod3Id = 0;
  await test.step('Precondition: POD #3 (separate billing group)', async () => {
    await createPod(fx, rpsSeparate);
    pod3Id = (fx.Responses.pod[2] as { id: number }).id;
  });
  await test.step('Precondition: billing profile POD #3', async () => createBillingProfile(fx, 2));

  await test.step('Precondition: add POD #3 to billing group 2', async () => {
    const contractPayload = await fx.GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    const editPayload = await fx.GeneratePayload.contractsAndOrders.edit_ProductContract(
      contractPayload as never,
    );
    (editPayload as { basicParameters: { entryInForceDate: string }; productParameters?: { contractType: string } }).basicParameters.entryInForceDate =
      '2025-01-01';
    if ((editPayload as { productParameters?: { contractType: string } }).productParameters) {
      (editPayload as { productParameters: { contractType: string } }).productParameters.contractType = 'SUPPLY_ONLY';
    }
    const newPodInfo = await fx.GeneratePayload.contractsAndOrders.addPodToBillingGroup(
      1,
      (fx.Responses.pod[2] as { podDetailId: number }).podDetailId,
    );
    const addPod = (newPodInfo as { addPod: { productContractPointOfDeliveries: unknown[] } }).addPod;
    (editPayload as { podRequests: unknown[] }).podRequests.push(addPod);
    (editPayload as { productContractPointOfDeliveries: unknown[] }).productContractPointOfDeliveries.push(
      addPod.productContractPointOfDeliveries[0],
    );
    (editPayload as { additionalParameters: { estimatedTotalConsumptionUnderContractKwh: number } })
      .additionalParameters.estimatedTotalConsumptionUnderContractKwh = (
      newPodInfo as { estimatedConsumption: number }
    ).estimatedConsumption;
    const contractId = (fx.Responses.productContract[0] as { id: number }).id;
    const editRes = await fx.Request.put(
      `product-contract/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: editPayload },
    );
    await expect(editRes).CheckResponse();
  });

  await test.step('Precondition: activate POD #3', async () => activatePod(fx, 2));

  let billingRunId = 0;
  await test.step('Precondition: billing run', async () => {
    billingRunId = await createBillingRun(fx, opts);
  });

  return { billingRunId, pod1Id, pod2Id, pod3Id };
}

/**
 * Two PODs in ONE billing group with separateInvoiceForEachPod=true.
 * Used for TC-BE-9.
 */
export async function createTwoPodSeparateInvoicePerPodChain(
  fx: RpsDueDateFx,
  rpsNumberId1: number | null,
  rpsNumberId2: number | null,
  opts: SimpleChainOpts,
): Promise<MultiPodResult> {
  await test.step('Precondition: term', async () => createTerm(fx));
  await test.step('Precondition: price component', async () =>
    createElectricityPriceComponent(fx, { applicationModelType: opts.applicationModelType }));
  await test.step('Precondition: product', async () => createProduct(fx));
  await test.step('Precondition: customer', async () => createCustomer(fx));

  let pod1Id = 0;
  let pod2Id = 0;
  await test.step('Precondition: POD #1', async () => {
    await createPod(fx, rpsNumberId1);
    pod1Id = (fx.Responses.pod[0] as any).id;
  });
  await test.step('Precondition: POD #2', async () => {
    await createPod(fx, rpsNumberId2);
    pod2Id = (fx.Responses.pod[1] as any).id;
  });

  // Both PODs in one billing group
  await test.step('Precondition: product contract (both PODs, one billing group)', async () =>
    createProductContract(fx));

  // Enable separateInvoiceForEachPod on billing group 1
  await test.step('Precondition: enable separate invoice per POD on billing group 1', async () => {
    const bgPayload = await fx.GeneratePayload.contractsAndOrders.editBillingGroup(0);
    bgPayload.separateInvoiceForEachPod = true;
    const bgRes = await fx.Request.put(`billing-group/${bgPayload.id}`, { data: bgPayload });
    await expect(bgRes).CheckResponse();
  });

  await test.step('Precondition: activate POD #1', async () => activatePod(fx, 0));
  await test.step('Precondition: activate POD #2', async () => activatePod(fx, 1));
  await test.step('Precondition: billing profile POD #1', async () => createBillingProfile(fx, 0));
  await test.step('Precondition: billing profile POD #2', async () => createBillingProfile(fx, 1));

  let billingRunId = 0;
  await test.step('Precondition: billing run', async () => {
    billingRunId = await createBillingRun(fx, opts);
  });

  return { billingRunId, pod1Id, pod2Id };
}

/**
 * Creates a base standard billing run and waits for an invoice (used as prerequisite for
 * debit/credit note and correction tests).
 * Returns the base invoice id.
 */
export async function runBaseInvoiceForManualNotes(
  fx: RpsDueDateFx,
  rpsNumberId: number | null,
  invoiceDate: string,
): Promise<{ baseInvoiceId: number; billingRunId: number }> {
  await createSimpleRpsChain(fx, rpsNumberId, {
    applicationModelType: ['FOR_VOLUMES'],
    invoiceDate,
    invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
  });

  const billingRunId = asBillingRunId(fx.Responses.billingRun[fx.Responses.billingRun.length - 1]);

  await test.step('Start base billing run (prerequisite for manual note)', async () => {
    await startAndCompleteBillingRun(fx.Request, billingRunId);
  });

  const rows = await pollInvoiceListing(fx.Request, billingRunId, 1);
  if (rows.length === 0) throw new Error('[RPS-DUEDATE] Base invoice not found after billing run');
  const baseInvoiceId = rows[0].id as number;
  fx.Responses.invoice.push(baseInvoiceId);

  return { baseInvoiceId, billingRunId };
}

// ─── WORKING-DAY ASSERTION HELPERS ─────────────────────────────────────────

const WEEKEND_DAYS = [0, 6]; // Sunday=0, Saturday=6

/** Returns true if the ISO date string falls on a Saturday or Sunday. */
export function isWeekend(dateStr: string): boolean {
  const d = new Date(`${dateStr}T12:00:00Z`);
  return WEEKEND_DAYS.includes(d.getUTCDay());
}

/** Asserts the date is not a weekend day (minimal working-day check). */
export function assertIsNotWeekend(dateStr: string | null, label: string): void {
  expect(dateStr, `${label}: paymentDeadline must be set`).toBeTruthy();
  expect(
    isWeekend(dateStr!),
    `${label}: paymentDeadline ${dateStr} must not be a weekend day`,
  ).toBe(false);
}
