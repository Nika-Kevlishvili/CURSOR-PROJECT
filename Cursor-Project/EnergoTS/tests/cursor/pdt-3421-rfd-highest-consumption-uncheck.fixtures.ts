/**
 * PDT-3421 helpers — RFD DRAFT PUT: uncheck highest-consumption POD, check lower
 * PODs on the same liability customer + contract (GE reproduce).
 *
 * Bug-only (no TC .md). Creates all data from scratch.
 *
 * GE (Giorgi Kikriashvili): the error occurs when the request contains different
 * PODs associated with the same liability customer and contract. Select PODs with
 * the highest consumption, then for the same customer deselect the highest POD and
 * select lower-consumption PODs. Save as DRAFT → 500 Duplicate key on podId.
 *
 * Kalina (2026-09-04): customer with >1 POD; edit DRAFT; uncheck only the highest-
 * consumption POD (other PODs of that customer stay unchecked); save as DRAFT.
 * Expected: that check is not reassigned. Actual: the next customer's POD becomes
 * checked — deleteAndAddNewPods re-loads remaining highest-consumption PODs and
 * saveCheckedPods persists all of them.
 *
 * Sticky flag (2026-09-04 UI): Select POD's with highest consumption stays true
 * after the user checks only a non-highest POD. PUT still sends
 * podWithHighestConsumption=true and empty excludePodIds. Backend re-selects
 * remaining highest PODs (including the next customer).
 *
 * Runtime: PUT /disconnection-of-power-supply-requests/{id} with
 * podWithHighestConsumption=true, excludePodIds=[highest], pods=[lower checked]
 * used to merge two CustomersForDPSResponse lists keyed by podId without a merge
 * function (IllegalStateException Duplicate key). origin/dev deleteAndAddNewPods
 * now Collectors.toMap(..., keep checked).
 *
 * Do not use allSelected true or ALL_CUSTOMERS (PDT-3179: select-all SQL ~9–10 min).
 * Do not GET load-customer-for-disconnection-power-supply to build pods[] — build
 * rows from GET /pod/{id} like PDT-3179. Do not POST reminder /job.
 *
 * Swagger (dev, update-swagger-specs.ps1 this session — all envs OK):
 * - PUT /disconnection-of-power-supply-requests/{id} DPSRequestsBaseRequest
 *   required: conditionType, gridOpRequestRegDate, gridOperatorId,
 *   reasonOfDisconnectionId, reminderForDisconnectionId, supplierType
 *   disconnectionRequestsStatus enum: DRAFT | EXECUTED | FEE_CHARGED
 *   conditionType enum: ALL_CUSTOMERS | CUSTOMERS_UNDER_CONDITIONS | LIST_OF_CUSTOMERS
 *   supplierType enum: CURRENT | PREVIOUS
 *   pods[] = CustomersForDPSResponse (podId, isChecked, isHighestConsumption, …)
 * - GET /disconnection-of-power-supply-requests/get-checked-pods/{reminderId}/{gridOperatorId}/{id}
 * - GET /disconnection-of-power-supply-requests/view-pod-tab/{id} CustomersForDPSRequest
 *   required: conditionType, page, size, supplierType
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/pdt-3113-incorrect-interim-generation.fixtures.ts (extra POD on same contract)
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 * - tests/cursor/pdt-3409-reconnection-draft-disappearing-pods.fixtures.ts (second customer chain)
 */

import { expect } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  asNumber,
  createExecutedReminder,
  createOverdueManualLiabilityWithBillingGroup,
  createSupplyChain,
  customerIdentifier,
  entityId,
  errorHaystack,
  getLiability,
  monthStartYmd,
  nestedId,
  PDT_3179_MANUAL_LIABILITY_AMOUNT,
  PDT_3179_RFD_POST_TIMEOUT_MS,
  readHttpBody,
  resolveContractBillingGroupId,
  resolveReminderIdForRfd,
  todayYmd,
  type Pdt3179Fx,
} from './pdt-3179-reconnection-invoice-emails.fixtures';

export const PDT_3421_KEY = 'PDT-3421';
export const PDT_3421_TITLE =
  'Request for disconnection - Error when POD with highest consumption is unchecked and another with lower is checked';

/** Supply + 2 extra PODs + reminder + RFD POST (~13 min) + PUT with highest filter. */
export const PDT_3421_TEST_TIMEOUT_MS = 40 * 60 * 1000;
export const PDT_3421_RFD_HTTP_TIMEOUT_MS = PDT_3179_RFD_POST_TIMEOUT_MS;
export const PDT_3421_POD_COUNT = 3;

export type Pdt3421Fx = Pdt3179Fx;

export type Pdt3421PodRow = Record<string, unknown> & {
  podId: number;
  customerId: number;
  podIdentifier: string;
  isChecked: boolean;
  isHighestConsumption: boolean;
  podDetailId: number;
};

export function pdt3421RelevantKeys(): Array<
  'customer' | 'pod' | 'product' | 'productContract' | 'customerLiability'
> {
  return ['customer', 'pod', 'product', 'productContract', 'customerLiability'];
}

function logElapsed(started: number, message: string): void {
  console.log(`[PDT-3421] ${Date.now() - started}ms ${message}`);
}

export async function resolvePodDetailId(
  fx: Pdt3421Fx,
  podIndex: number,
): Promise<number> {
  const pod = fx.Responses.pod[podIndex] as Record<string, unknown> | undefined;
  expect(pod, `Responses.pod[${podIndex}]`).toBeTruthy();
  const fromCreate =
    nestedId(pod?.podDetailId) ?? nestedId(pod?.lastPodDetailId);
  if (fromCreate) return fromCreate;

  const podId = entityId(pod);
  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;
  const versions = Array.isArray(body.versions)
    ? (body.versions as Record<string, unknown>[])
    : [];
  const contractId = entityId(fx.Responses.productContract[0]);
  const contractGet = await fx.Request.get(
    `${fx.Endpoints.productContract}/${contractId}?version=1`,
  );
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const contractPods = Array.isArray(contractBody.contractPodsResponses)
    ? (contractBody.contractPodsResponses as Record<string, unknown>[])
    : [];
  const match = contractPods.find((row) => nestedId(row.podId) === podId);
  const podDetailId =
    nestedId(body.podDetailId) ??
    nestedId(body.lastPodDetailId) ??
    nestedId(versions[0]?.podDetailId) ??
    nestedId(match?.podDetailId) ??
    nestedId(match?.pointOfDeliveryDetailId);
  expect(podDetailId, `podDetailId for POD index ${podIndex}`).toBeGreaterThan(0);
  return podDetailId as number;
}

/**
 * Add extra settlement PODs onto billing group 0 of the existing product contract
 * (same customer + same contract + same liability group as GE).
 */
export async function addAndActivateExtraPodsOnSameBillingGroup(
  fx: Pdt3421Fx,
  extraCount: number,
): Promise<number[]> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const addedIds: number[] = [];
  for (let n = 0; n < extraCount; n += 1) {
    const podRes = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(podRes).CheckResponse();
    const body = await podRes.json();
    Responses.pod.push(body);
    const podIndex = Responses.pod.length - 1;
    addedIds.push(entityId(body));

    const podDetailId = await resolvePodDetailId(fx, podIndex);
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
    const editPayload = await GeneratePayload.contractsAndOrders.edit_ProductContract(
      contractPayload as never,
    );
    (editPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
    const newPodInfo = await GeneratePayload.contractsAndOrders.addPodToBillingGroup(
      0,
      podDetailId,
    );
    (editPayload as { podRequests: unknown[] }).podRequests.push(newPodInfo.addPod);
    (
      editPayload as { productContractPointOfDeliveries: unknown[] }
    ).productContractPointOfDeliveries.push(
      newPodInfo.addPod.productContractPointOfDeliveries[0],
    );
    (
      editPayload as { additionalParameters: { estimatedTotalConsumptionUnderContractKwh: number } }
    ).additionalParameters.estimatedTotalConsumptionUnderContractKwh =
      newPodInfo.estimatedConsumption;

    const contractId = entityId(Responses.productContract[0]);
    const existingGet = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
    await expect(existingGet).CheckResponse();
    const existing = (await existingGet.json()) as {
      basicParameters?: { entryInForceDate?: unknown; signingDate?: unknown };
    };
    const entryInForce =
      existing.basicParameters?.entryInForceDate ??
      existing.basicParameters?.signingDate ??
      (editPayload as { basicParameters?: { signingDate?: unknown } }).basicParameters?.signingDate;
    if (entryInForce) {
      (editPayload as { basicParameters: { entryInForceDate: unknown } }).basicParameters.entryInForceDate =
        entryInForce;
    }

    const editRes = await Request.put(
      `product-contract/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: editPayload },
    );
    await expect(editRes).CheckResponse();

    const act = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(
        podIndex,
        monthStartYmd(),
        undefined,
        0,
      ),
    });
    await expect(act).CheckResponse();
  }
  expect(
    Responses.pod.length,
    `expected ${1 + extraCount} PODs on customer A contract after extras`,
  ).toBeGreaterThanOrEqual(1 + extraCount);
  return addedIds;
}

export async function buildPodRowForIndex(
  fx: Pdt3421Fx,
  podIndex: number,
): Promise<Pdt3421PodRow> {
  const pod = fx.Responses.pod[podIndex] as Record<string, unknown>;
  const customer = fx.Responses.customer[0] as Record<string, unknown>;
  const podId = entityId(pod);
  const customerId = entityId(customer);
  const identifier = customerIdentifier(fx.Responses);
  const contractId = entityId(fx.Responses.productContract[0]);
  const liabilityId = entityId(fx.Responses.customerLiability[0]);
  const podDetailId = await resolvePodDetailId(fx, podIndex);

  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;

  const contractGet = await fx.Request.get(
    `${fx.Endpoints.productContract}/${contractId}?version=1`,
  );
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;

  const liab = await getLiability(fx, liabilityId);
  const liabilityNumber = String(liab.number ?? '');
  expect(liabilityNumber.length, 'GET /customer-liability number').toBeGreaterThan(0);
  const amount = asNumber(liab.currentAmount) || PDT_3179_MANUAL_LIABILITY_AMOUNT;
  const currencyName = String(
    (liab.currencyResponse as { name?: unknown } | undefined)?.name ?? 'BGN',
  ).replace(/-/g, ' ');
  const liabilityToken = `${amount}-${currencyName}-${liabilityNumber}`;

  const bp = (contractBody.basicParameters ?? {}) as Record<string, unknown>;
  const contractNumber = String(bp.contractNumber ?? contractId);
  const bg = Array.isArray(contractBody.billingGroups)
    ? (contractBody.billingGroups[0] as Record<string, unknown>)
    : undefined;
  const billingGroupLabel = String(bg?.groupNumber ?? bg?.number ?? nestedId(bg) ?? '');

  return {
    podId,
    customerId,
    podIdentifier: String(body.identifier ?? pod.identifier ?? ''),
    isChecked: true,
    isHighestConsumption: false,
    existingCustomerReceivables: true,
    gridOperatorId: nestedId(body.gridOperatorId) ?? envVariables.grid_operator,
    podDetailId,
    customerNumber: identifier,
    customers: identifier,
    contracts: contractNumber,
    billingGroups: billingGroupLabel || contractNumber,
    liabilitiesInBillingGroup: liabilityToken,
    liabilitiesInPod: null,
    liabilityAmountCustomer: amount,
  };
}

export async function buildAllPodRows(fx: Pdt3421Fx): Promise<Pdt3421PodRow[]> {
  const rows: Pdt3421PodRow[] = [];
  for (let i = 0; i < fx.Responses.pod.length; i += 1) {
    rows.push(await buildPodRowForIndex(fx, i));
  }
  expect(rows.length, 'need 3 POD rows for GE scenario').toBe(PDT_3421_POD_COUNT);
  const ids = new Set(rows.map((r) => r.podId));
  expect(ids.size, 'POD ids must be unique').toBe(rows.length);
  return rows;
}

function rfdBasePayload(
  fx: Pdt3421Fx,
  reminderId: number,
  listOfCustomer?: string,
): Record<string, unknown> {
  const payload = fx.GeneratePayload.receivablesManagement.requestForDisconnection() as Record<
    string,
    unknown
  >;
  payload.reminderForDisconnectionId = reminderId;
  payload.gridOpRequestRegDate = todayYmd();
  payload.supplierType = 'CURRENT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = listOfCustomer ?? customerIdentifier(fx.Responses);
  payload.templateIds = [];
  payload.files = [];
  return payload;
}

export async function createDraftRfdWithAllPodsChecked(
  fx: Pdt3421Fx,
  podRows: Pdt3421PodRow[],
): Promise<number> {
  const { Request, Responses, Endpoints } = fx;
  const started = Date.now();
  const reminderId = await resolveReminderIdForRfd(fx);
  const payload = rfdBasePayload(fx, reminderId);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.allSelected = false;
  payload.podWithHighestConsumption = false;
  payload.excludePodIds = [];
  payload.pods = podRows.map((row) => ({ ...row, isChecked: true, isHighestConsumption: false }));
  const res = await Request.post(Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3421_RFD_HTTP_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  Responses.requestForDisconnection.push(requestId);
  logElapsed(started, `POST DRAFT RFD ${requestId} pods=${podRows.length}`);
  return requestId;
}

/**
 * GE save: keep highest-consumption filter on, exclude the highest POD, send the
 * remaining same-customer/contract PODs as checked (isHighestConsumption false).
 */
export async function putDraftUncheckHighestCheckLower(
  fx: Pdt3421Fx,
  rfdId: number,
  highest: Pdt3421PodRow,
  lower: Pdt3421PodRow[],
): Promise<ReturnType<typeof readHttpBody>> {
  const reminderId = await resolveReminderIdForRfd(fx);
  const payload = rfdBasePayload(fx, reminderId);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.allSelected = false;
  payload.podWithHighestConsumption = true;
  payload.excludePodIds = [highest.podId];
  payload.pods = lower.map((row) => ({
    ...row,
    isChecked: true,
    isHighestConsumption: false,
  }));
  const res = await fx.Request.put(`${fx.Endpoints.requestForDisconnection}/${rfdId}`, {
    data: payload,
    timeout: PDT_3421_RFD_HTTP_TIMEOUT_MS,
  });
  return readHttpBody(res);
}

export async function getRfd(
  fx: Pdt3421Fx,
  requestId: number,
): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${fx.Endpoints.requestForDisconnection}/${requestId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function getCheckedPodIds(
  fx: Pdt3421Fx,
  rfdId: number,
): Promise<number[]> {
  const reminderId = await resolveReminderIdForRfd(fx);
  const gridOperatorId = Number(envVariables.grid_operator);
  const res = await fx.Request.get(
    `${fx.Endpoints.requestForDisconnection}/get-checked-pods/${reminderId}/${gridOperatorId}/${rfdId}`,
    { timeout: PDT_3421_RFD_HTTP_TIMEOUT_MS },
  );
  // Controller returns 206 PARTIAL_CONTENT (Playwright ok() = 200–299).
  await expect(res).CheckResponse();
  const body = await res.json();
  const rows = Array.isArray(body)
    ? body
    : Array.isArray((body as { content?: unknown }).content)
      ? ((body as { content: unknown[] }).content)
      : [];
  return rows
    .map((row) => nestedId((row as Record<string, unknown>).podId) ?? nestedId(row))
    .filter((id): id is number => typeof id === 'number' && id > 0);
}

export function customerIdentOf(customer: unknown): string {
  return String((customer as { identifier?: unknown } | undefined)?.identifier ?? '');
}

function lastEntry<T>(arr: T[], label: string): T {
  expect(arr.length, `${label} must exist`).toBeGreaterThan(0);
  return arr[arr.length - 1];
}

/**
 * Second customer + 1 POD + contract + overdue billing-group liability.
 * Mirrors PDT-3409 dummy chain (last indices; override customerId on contract/liability).
 */
export async function createSecondCustomerSupplyAndLiability(fx: Pdt3421Fx): Promise<{
  customerIndex: number;
  podIndex: number;
  contractIndex: number;
  liabilityIndex: number;
  customerId: number;
  podId: number;
  identifier: string;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const electricity = await Request.post(Endpoints.priceComponent, {
    data: GeneratePayload.productAndServices.electricity(),
  });
  await expect(electricity).CheckResponse();
  Responses.priceComponent.push(await electricity.json());

  const productPayload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.termId = entityId(lastEntry(Responses.terms, 'customer B term'));
  productPayload.priceComponentIds = [
    entityId(lastEntry(Responses.priceComponent, 'customer B price component')),
  ];
  productPayload.interimAdvancePayments = [];
  productPayload.interimAdvancePaymentGroups = [];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const customerPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  const customer = await Request.post(Endpoints.customer, { data: customerPayload });
  await expect(customer).CheckResponse();
  Responses.customer.push(await customer.json());

  const pod = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod).CheckResponse();
  const podBody = await pod.json();
  Responses.pod.push(podBody);

  const lastCustomerIdx = Responses.customer.length - 1;
  const lastProductIdx = Responses.product.length - 1;
  const lastPodIdx = Responses.pod.length - 1;
  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(
    lastCustomerIdx,
    lastProductIdx,
    lastPodIdx,
  );
  (contractPayload.basicParameters as Record<string, unknown>).customerId = entityId(
    lastEntry(Responses.customer, 'customer B'),
  );
  (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
  const monthly =
    asNumber((podBody as { estimatedMonthlyAvgConsumption?: unknown }).estimatedMonthlyAvgConsumption) ||
    1;
  contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = (monthly * 12) / 1000;
  const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(contract).CheckResponse();
  Responses.productContract.push(await contract.json());

  const lastContractIdx = Responses.productContract.length - 1;
  const activation = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(
      lastPodIdx,
      monthStartYmd(),
      undefined,
      lastContractIdx,
    ),
  });
  await expect(activation).CheckResponse();

  const contractId = entityId(lastEntry(Responses.productContract, 'customer B contract'));
  const contractGet = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const billingGroupId = resolveContractBillingGroupId(contractBody);
  const yesterday = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  const liabPayload = GeneratePayload.receivablesManagement.customer_liability();
  liabPayload.customerId = entityId(lastEntry(Responses.customer, 'customer B'));
  liabPayload.billingGroupId = billingGroupId;
  liabPayload.initialAmount = PDT_3179_MANUAL_LIABILITY_AMOUNT;
  liabPayload.dueDate = yesterday;
  liabPayload.occurrenceDate = yesterday;
  const liabRes = await Request.post(Endpoints.customerLiability, { data: liabPayload });
  await expect(liabRes).CheckResponse();
  Responses.customerLiability.push(entityId(await liabRes.json()));

  const customerB = lastEntry(Responses.customer, 'customer B');
  return {
    customerIndex: lastCustomerIdx,
    podIndex: lastPodIdx,
    contractIndex: lastContractIdx,
    liabilityIndex: Responses.customerLiability.length - 1,
    customerId: entityId(customerB),
    podId: entityId(podBody),
    identifier: customerIdentOf(customerB),
  };
}

export async function createExecutedReminderForIdentifiers(
  fx: Pdt3421Fx,
  identifiers: string[],
  templates?: { emailTemplateId: number; smsTemplateId: number },
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const started = Date.now();
  const list = identifiers.filter((id) => id.length > 0).join(',');
  expect(list.length, 'reminder customerList').toBeGreaterThan(0);
  const payload = GeneratePayload.receivablesManagement.reminderForDisconnection();
  payload.customerList = list;
  payload.customerFilterType = 'INCLUDED';
  payload.documentTemplateId = null;
  if (templates) {
    payload.emailTemplateId = templates.emailTemplateId;
    payload.smsTemplateId = templates.smsTemplateId;
  }
  const res = await Request.post(Endpoints.reminderForDisconnection, { data: payload });
  await expect(res).CheckResponse();
  const reminderId = entityId(await res.json());
  Responses.reminderForDisconnection.push(reminderId);
  logElapsed(started, `POST reminder ${reminderId} customerList=${list}`);

  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const timeOffset = await Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${reminderId}&hour=${georgianTime.getHours()}&minute=${georgianTime.getMinutes()}`,
    { timeout: 5_000 },
  );
  await expect(timeOffset).CheckResponse();

  const deadline = Date.now() + 20_000;
  while (Date.now() < deadline) {
    try {
      const reminderGet = await Request.get(`power-supply-disconnection-reminder/${reminderId}`, {
        timeout: 3_000,
      });
      if (reminderGet.ok()) {
        const status = ((await reminderGet.json()) as { reminderStatus?: string }).reminderStatus;
        if (status === 'EXECUTED') {
          logElapsed(started, `reminder ${reminderId} already EXECUTED`);
          return reminderId;
        }
      }
    } catch {
      /* poll */
    }
    await new Promise((r) => setTimeout(r, 1_000));
  }
  logElapsed(started, `reminder ${reminderId} still not EXECUTED (no /job)`);
  return reminderId;
}

export async function buildPodRowForParty(
  fx: Pdt3421Fx,
  opts: {
    podIndex: number;
    customerIndex: number;
    contractIndex: number;
    liabilityIndex: number;
  },
): Promise<Pdt3421PodRow> {
  const pod = fx.Responses.pod[opts.podIndex] as Record<string, unknown>;
  const customer = fx.Responses.customer[opts.customerIndex] as Record<string, unknown>;
  const podId = entityId(pod);
  const customerId = entityId(customer);
  const identifier = customerIdentOf(customer);
  const contractId = entityId(fx.Responses.productContract[opts.contractIndex]);
  const liabilityId = entityId(fx.Responses.customerLiability[opts.liabilityIndex]);

  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;

  const contractGet = await fx.Request.get(
    `${fx.Endpoints.productContract}/${contractId}?version=1`,
  );
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const contractPods = Array.isArray(contractBody.contractPodsResponses)
    ? (contractBody.contractPodsResponses as Record<string, unknown>[])
    : [];
  const match = contractPods.find((row) => nestedId(row.podId) === podId);
  const podDetailId =
    nestedId(pod.podDetailId) ??
    nestedId(body.podDetailId) ??
    nestedId(body.lastPodDetailId) ??
    nestedId(match?.podDetailId) ??
    nestedId(match?.pointOfDeliveryDetailId);
  expect(podDetailId, `podDetailId for party podIndex ${opts.podIndex}`).toBeGreaterThan(0);

  const liab = await getLiability(fx, liabilityId);
  const liabilityNumber = String(liab.number ?? '');
  expect(liabilityNumber.length, 'GET /customer-liability number').toBeGreaterThan(0);
  const amount = asNumber(liab.currentAmount) || PDT_3179_MANUAL_LIABILITY_AMOUNT;
  const currencyName = String(
    (liab.currencyResponse as { name?: unknown } | undefined)?.name ?? 'BGN',
  ).replace(/-/g, ' ');
  const liabilityToken = `${amount}-${currencyName}-${liabilityNumber}`;
  const bp = (contractBody.basicParameters ?? {}) as Record<string, unknown>;
  const contractNumber = String(bp.contractNumber ?? contractId);
  const bg = Array.isArray(contractBody.billingGroups)
    ? (contractBody.billingGroups[0] as Record<string, unknown>)
    : undefined;
  const billingGroupLabel = String(bg?.groupNumber ?? bg?.number ?? nestedId(bg) ?? '');

  return {
    podId,
    customerId,
    podIdentifier: String(body.identifier ?? pod.identifier ?? ''),
    isChecked: true,
    isHighestConsumption: false,
    existingCustomerReceivables: true,
    gridOperatorId: nestedId(body.gridOperatorId) ?? envVariables.grid_operator,
    podDetailId: podDetailId as number,
    customerNumber: identifier,
    customers: identifier,
    contracts: contractNumber,
    billingGroups: billingGroupLabel || contractNumber,
    liabilitiesInBillingGroup: liabilityToken,
    liabilitiesInPod: null,
    liabilityAmountCustomer: amount,
  };
}

export async function createDraftRfdWithCheckedPods(
  fx: Pdt3421Fx,
  podRows: Pdt3421PodRow[],
  listOfCustomer: string,
): Promise<number> {
  const { Request, Responses, Endpoints } = fx;
  const started = Date.now();
  const reminderId = await resolveReminderIdForRfd(fx);
  const payload = rfdBasePayload(fx, reminderId, listOfCustomer);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.allSelected = false;
  payload.podWithHighestConsumption = false;
  payload.excludePodIds = [];
  payload.pods = podRows.map((row) => ({ ...row, isChecked: true, isHighestConsumption: false }));
  const res = await Request.post(Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3421_RFD_HTTP_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  Responses.requestForDisconnection.push(requestId);
  logElapsed(started, `POST DRAFT RFD ${requestId} pods=${podRows.length}`);
  return requestId;
}

/** Create DRAFT with highest-consumption filter on and no checked POD rows (button). */
export async function createDraftRfdWithHighestFilterOn(
  fx: Pdt3421Fx,
  listOfCustomer: string,
): Promise<number> {
  const { Request, Responses, Endpoints } = fx;
  const started = Date.now();
  const reminderId = await resolveReminderIdForRfd(fx);
  const payload = rfdBasePayload(fx, reminderId, listOfCustomer);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.allSelected = false;
  payload.podWithHighestConsumption = true;
  payload.excludePodIds = [];
  payload.pods = [];
  const res = await Request.post(Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3421_RFD_HTTP_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  Responses.requestForDisconnection.push(requestId);
  logElapsed(started, `POST DRAFT RFD ${requestId} highest-filter-on pods=0`);
  return requestId;
}

/**
 * Sticky highest-consumption flag: user checked only these POD rows, but the
 * filter flag stays true and excludePodIds stays empty (UI did not turn the
 * button off).
 */
export async function putDraftStickyHighestOnlyThesePods(
  fx: Pdt3421Fx,
  rfdId: number,
  checkedRows: Pdt3421PodRow[],
  listOfCustomer: string,
): Promise<ReturnType<typeof readHttpBody>> {
  const reminderId = await resolveReminderIdForRfd(fx);
  const payload = rfdBasePayload(fx, reminderId, listOfCustomer);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.allSelected = false;
  payload.podWithHighestConsumption = true;
  payload.excludePodIds = [];
  payload.pods = checkedRows.map((row) => ({
    ...row,
    isChecked: true,
    isHighestConsumption: false,
  }));
  const res = await fx.Request.put(`${fx.Endpoints.requestForDisconnection}/${rfdId}`, {
    data: payload,
    timeout: PDT_3421_RFD_HTTP_TIMEOUT_MS,
  });
  return readHttpBody(res);
}

/**
 * Kalina: keep highest-consumption filter on, exclude the unchecked highest POD,
 * send no other checked PODs (same-customer others remain unchecked).
 */
export async function putDraftUncheckHighestLeaveOthersUnchecked(
  fx: Pdt3421Fx,
  rfdId: number,
  highest: Pdt3421PodRow,
  listOfCustomer: string,
): Promise<ReturnType<typeof readHttpBody>> {
  const reminderId = await resolveReminderIdForRfd(fx);
  const payload = rfdBasePayload(fx, reminderId, listOfCustomer);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.allSelected = false;
  payload.podWithHighestConsumption = true;
  payload.excludePodIds = [highest.podId];
  payload.pods = [];
  const res = await fx.Request.put(`${fx.Endpoints.requestForDisconnection}/${rfdId}`, {
    data: payload,
    timeout: PDT_3421_RFD_HTTP_TIMEOUT_MS,
  });
  return readHttpBody(res);
}

export { createExecutedReminder, createOverdueManualLiabilityWithBillingGroup, createSupplyChain, entityId, errorHaystack, customerIdentifier };
