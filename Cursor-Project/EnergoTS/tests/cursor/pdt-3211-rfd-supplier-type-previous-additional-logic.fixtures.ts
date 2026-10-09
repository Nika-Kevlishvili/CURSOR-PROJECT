/**
 * PDT-3211 helpers — Request for disconnection supplier type PREVIOUS additional logic.
 *
 * Bug-only (no TC .md). Creates all data from scratch. No test.beforeAll.
 *
 * TO-BE (Jira PDT-3211 + Confluence 72155868 / 585697986 AC 19.1.4–6):
 * 1. Existing Previous rule: include POD only when the Reminder customer has NO
 *    active contract POD on the current date.
 * 2. Additional Previous rule: exclude the POD if some other customer (different
 *    UIC / Personal Number = customer.identifier) has an active contract POD on
 *    the current date for that same POD.
 * 3. Supplier type CURRENT is unchanged (do not apply Previous exclusions).
 *
 * Approximate (do not simulate Last Supplier Resort period):
 * Customer A had the POD, then deactivated (no active contract POD today) +
 * unpaid overdue liability; Customer B (different identifier) has the same POD
 * active today.
 *
 * Runtime BASE_URL Dev2: http://10.236.20.11:8092
 *   (also https://devapps.energo-pro.bg/backend/phoenix-dev2)
 * Canonical OpenAPI (parent refreshed this session):
 *   Cursor-Project/config/swagger/dev2/swagger-spec.json
 *
 * Swagger findings (grep of that spec):
 * - POST /disconnection-of-power-supply-requests
 *   DPSRequestsBaseRequest required: conditionType, gridOpRequestRegDate,
 *   gridOperatorId, reasonOfDisconnectionId, reminderForDisconnectionId,
 *   supplierType, validityPeriodFrom, validityPeriodTo
 *   supplierType enum: CURRENT | PREVIOUS
 *   conditionType enum: ALL_CUSTOMERS | CUSTOMERS_UNDER_CONDITIONS | LIST_OF_CUSTOMERS
 *   listOfCustomer minLength 1 (do not send "")
 *   disconnectionRequestsStatus enum: DRAFT | EXECUTED | FEE_CHARGED
 *   Cursor jsons/.../requestForDisconnection.ts omits validityPeriod* — set them
 *   here (today → last day of current month). Never ALL_CUSTOMERS / allSelected true.
 * - GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply
 *   Query schema CustomersForDPSRequest required: conditionType, page, size, supplierType
 *   Runtime also requires gridOperatorId + powerSupplyDisconnectionReminderId
 *   LIST_OF_CUSTOMERS requires listOfCustomer minLength 1
 *   Swagger documents HTTP 200; Phoenix returns 206 PARTIAL_CONTENT.
 *   CheckResponse uses response.ok() (200–299) — 206 is success. Do not assert 200.
 * - GET /disconnection-of-power-supply-requests/view-pod-tab/{id}
 *   path id int64; same CustomersForDPSRequest query
 * - POST /power-supply-disconnection-reminder
 *   PowerSupplyDisconnectionReminderBaseRequest required: communicationChannels,
 *   conditionType, customerSendToDateAndTime, disconnectionDate, liabilitiesMaxDueDate
 *   Dev2 shape: LIST_OF_CUSTOMERS + listOfCustomer; omit customerList/customerFilterType
 *   confirm: true; communicationChannels: ['EMAIL']
 * - PUT /power-supply-disconnection-reminder/set-customer-send-time
 *   query reminderId, hour, minute
 * - POST /power-supply-disconnection-reminder/job
 * - POST /customer-liability  CustomerLiabilityRequest required: accountingPeriodId,
 *   currencyId, customerId, dueDate, initialAmount, occurrenceDate
 * - PUT /customer-liability/{id}/due-date-change
 * - POST /contract-pods/manual  PodManualActivationRequest (activationDate, deactivationDate)
 * - POST /product-contract, POST /pod, POST /customer
 *
 * Do not use PDT-2971 TOMORROW activation (not active on current date).
 * Do not use PDT-3179 resolveReminderIdForRfd (do not steal another EXECUTED reminder).
 * POD listing SQL lives in receivable.get_request_fd_pod_data — assert HTTP listing.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2971-rfd-shared-pod-multi-customer-merge.spec.ts
 * - tests/cursor/pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts
 * - tests/cursor/pdt-2913-missing-email-object-reminder-for-disconnection.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/pdt-3090-rfd-highest-consumption-fallback.fixtures.ts
 *   (validityPeriodFrom/To, LIST_OF_CUSTOMERS load query, HTTP 206)
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 */

import { expect } from './cursor-test.fixtures';
import type { baseFixture } from '../../fixtures/baseFixture';
import { Endpoints } from '../../fixtures/constants/endpoints';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';

export const PDT_3211_KEY = 'PDT-3211';
export const PDT_3211_TITLE =
  'Request for disconnection - Supplier type "Previous" additional logic';

export const PDT_3211_MAIN_TEST_TIMEOUT_MS = 25 * 60 * 1000;
export const PDT_3211_SINGLE_CUSTOMER_TIMEOUT_MS = 20 * 60 * 1000;
export const PDT_3211_RFD_POST_TIMEOUT_MS = 13 * 60 * 1000;
export const PDT_3211_LOAD_GET_TIMEOUT_MS = 5 * 60 * 1000;
export const PDT_3211_REMINDER_POLL_MS = 4 * 60 * 1000;
export const PDT_3211_MANUAL_LIABILITY_AMOUNT = 100;

export const PDT_3211_LOAD_CUSTOMERS_PATH =
  'disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply';

export const CUSTOMER_A_INDEX = 0;
export const CUSTOMER_B_INDEX = 1;
export const CONTRACT_A_INDEX = 0;
export const CONTRACT_B_INDEX = 1;

export type Pdt3211Fx = {
  Request: baseFixture['Request'];
  GeneratePayload: baseFixture['GeneratePayload'];
  Responses: baseFixture['Responses'];
  Endpoints: typeof Endpoints;
};

export type Pdt3211SupplierType = 'CURRENT' | 'PREVIOUS';

export type Pdt3211LoadQuery = {
  page: number;
  size: number;
  conditionType: 'LIST_OF_CUSTOMERS';
  listOfCustomer: string;
  powerSupplyDisconnectionReminderId: number;
  gridOperatorId: number;
  supplierType: Pdt3211SupplierType;
};

export type Pdt3211PodTabRow = {
  podIdentifier?: string;
  podId?: number;
  customerId?: number;
  customers?: string;
  contracts?: string;
};

export type Pdt3211ReminderSecondTabRow = {
  customer?: string;
  id?: number;
};

export function pdt3211RelevantKeys(): Array<
  | 'customer'
  | 'pod'
  | 'productContract'
  | 'reminderForDisconnection'
  | 'requestForDisconnection'
  | 'customerLiability'
> {
  return [
    'customer',
    'pod',
    'productContract',
    'reminderForDisconnection',
    'requestForDisconnection',
    'customerLiability',
  ];
}

export function todayYmd(): string {
  return randomGens.generateTodaysDate('yyyy-mm-dd');
}

export function yesterdayYmd(): string {
  return randomGens.generateYesterdaysDate('yyyy-mm-dd');
}

export function monthEndYmd(): string {
  return randomGens.generateMonthEndDate('yyyy-mm-dd');
}

export function entityId(entry: unknown): number {
  if (typeof entry === 'number' && Number.isFinite(entry) && entry > 0) return entry;
  if (typeof entry === 'string' && /^\d+$/.test(entry)) return Number(entry);
  if (entry !== null && typeof entry === 'object') {
    if ('id' in entry) {
      const id = Number((entry as { id: unknown }).id);
      if (Number.isFinite(id) && id > 0) return id;
    }
    const bpId = Number((entry as { basicParameters?: { id?: unknown } }).basicParameters?.id);
    if (Number.isFinite(bpId) && bpId > 0) return bpId;
  }
  throw new Error(`Cannot resolve entity id from: ${JSON.stringify(entry)?.slice(0, 240)}`);
}

export function customerIdentifierOf(row: unknown): string {
  if (row !== null && typeof row === 'object') {
    const rec = row as { identifier?: unknown; customerIdentifier?: unknown };
    const identifier = String(rec.identifier ?? rec.customerIdentifier ?? '');
    if (identifier.length > 0) return identifier;
  }
  return '';
}

export function reminderIdOf(raw: unknown): number {
  return entityId(raw);
}

function numericId(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) return value;
  if (typeof value === 'string' && /^\d+$/.test(value)) return Number(value);
  if (value !== null && typeof value === 'object' && 'id' in value) {
    const id = Number((value as { id: unknown }).id);
    if (Number.isFinite(id) && id > 0) return id;
  }
  return 0;
}

export function gridOperatorIdFromEnv(): number {
  const id = Number(envVariables.grid_operator);
  expect(id, 'envVariables.grid_operator (never hardcode)').toBeGreaterThan(0);
  return id;
}

export function rowsForPod(
  rows: Pdt3211PodTabRow[],
  opts: { podIdentifier?: string; podId?: number; customerId?: number },
): Pdt3211PodTabRow[] {
  const hasIdentifier = Boolean(opts.podIdentifier);
  const hasPodId = opts.podId != null;
  if (!hasIdentifier && !hasPodId) {
    throw new Error('rowsForPod requires podIdentifier or podId');
  }
  return rows.filter((row) => {
    const podMatch = hasIdentifier
      ? row.podIdentifier === opts.podIdentifier
      : Number(row.podId) === opts.podId;
    if (!podMatch) return false;
    if (opts.customerId != null && Number(row.customerId) !== opts.customerId) return false;
    return true;
  });
}

export async function resolvePodIdentifier(fx: Pdt3211Fx, podIndex = 0): Promise<string> {
  const pod = fx.Responses.pod[podIndex] as Record<string, unknown> | undefined;
  const fromCreate = String(pod?.identifier ?? '');
  if (fromCreate.length > 0) return fromCreate;
  const podId = entityId(pod);
  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const identifier = String(((await getRes.json()) as { identifier?: unknown }).identifier ?? '');
  expect(identifier.length, `POD identifier for index ${podIndex}`).toBeGreaterThan(0);
  return identifier;
}

export async function createPrivateCustomer(fx: Pdt3211Fx): Promise<{
  payload: Record<string, unknown>;
  body: Record<string, unknown>;
}> {
  const payload = fx.GeneratePayload.customers.customer_private() as Record<string, unknown>;
  expect(String(payload.customerIdentifier ?? '').length, 'private customerIdentifier (UIC)').toBeGreaterThan(0);
  const res = await fx.Request.post(fx.Endpoints.customer, { data: payload });
  await expect(res).CheckResponse();
  const body = (await res.json()) as Record<string, unknown>;
  fx.Responses.customer.push(body);
  const identifier = customerIdentifierOf(body) || String(payload.customerIdentifier ?? '');
  expect(identifier.length, 'created private customer identifier').toBeGreaterThan(0);
  if (!body.identifier) body.identifier = identifier;
  return { payload, body };
}

export async function createTermAndSupplyProduct(fx: Pdt3211Fx): Promise<void> {
  const termRes = await fx.Request.post(fx.Endpoints.terms, {
    data: fx.GeneratePayload.productAndServices.term(),
  });
  await expect(termRes).CheckResponse();
  fx.Responses.terms.push(await termRes.json());

  const productPayload = fx.GeneratePayload.productAndServices.product() as Record<string, unknown>;
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  const productRes = await fx.Request.post(fx.Endpoints.product, { data: productPayload });
  await expect(productRes).CheckResponse();
  fx.Responses.product.push(await productRes.json());
}

export async function createSettlementPod(fx: Pdt3211Fx): Promise<Record<string, unknown>> {
  const res = await fx.Request.post(fx.Endpoints.pod, {
    data: fx.GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as Record<string, unknown>;
  fx.Responses.pod.push(body);
  return body;
}

export async function createProductContract(
  fx: Pdt3211Fx,
  customerIndex: number,
  productIndex = 0,
  podIndex = 0,
): Promise<Record<string, unknown>> {
  const payload = await fx.GeneratePayload.contractsAndOrders.product_contract(
    customerIndex,
    productIndex,
    podIndex,
  );
  payload.basicParameters.customerId = fx.Responses.customer[customerIndex].id;
  payload.basicParameters.status = 'ENTERED_INTO_FORCE';
  payload.basicParameters.subStatus = 'AWAITING_ACTIVATION';
  payload.basicParameters.entryInForceDate = todayYmd();
  payload.productParameters.contractType = 'SUPPLY_ONLY';
  const res = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  await expect(res).CheckResponse();
  const body = (await res.json()) as Record<string, unknown>;
  fx.Responses.productContract.push(body);
  return body;
}

export async function activateContractPod(
  fx: Pdt3211Fx,
  opts: {
    podIndex?: number;
    contractIndex: number;
    activationDate: string;
    deactivationDate?: string;
  },
): Promise<void> {
  const activation = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(
      opts.podIndex ?? 0,
      opts.activationDate,
      opts.deactivationDate,
      opts.contractIndex,
    ),
  });
  await expect(activation).CheckResponse();
}

export async function setContractActiveInPerpetuity(
  fx: Pdt3211Fx,
  contractIndex: number,
): Promise<void> {
  const contractId = entityId(fx.Responses.productContract[contractIndex]);
  const statusUpdate = await fx.Request.put(
    `product-contract/status-update/${contractId}?versionId=1`,
    {
      data: {
        contractStatus: 'ACTIVE_IN_PERPETUITY',
        contractSubStatus: 'DELIVERY',
        contractVersionStatus: 'SIGNED',
      },
    },
  );
  await expect(statusUpdate).CheckResponse();
}

export async function terminateContract(fx: Pdt3211Fx, contractIndex: number): Promise<void> {
  const contractId = entityId(fx.Responses.productContract[contractIndex]);
  const termRes = await fx.Request.get(`ttest/pod-termination?contractId=${contractId}`);
  if (termRes.status() >= 400) {
    const fallback = await fx.Request.put(`product-contract/status-update/${contractId}?versionId=1`, {
      data: {
        contractStatus: 'TERMINATED',
        contractSubStatus: 'ALL_PODS_ARE_DEACTIVATED',
        contractVersionStatus: 'SIGNED',
      },
    });
    await expect(fallback).CheckResponse();
  }
}

export async function resolveContractBillingGroupId(
  fx: Pdt3211Fx,
  contractIndex: number,
): Promise<number> {
  const contractId = entityId(fx.Responses.productContract[contractIndex]);
  const contractGet = await fx.Request.get(`${fx.Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const body = (await contractGet.json()) as Record<string, unknown>;
  const groups = body.billingGroups;
  if (Array.isArray(groups) && groups.length > 0) {
    const id = numericId(groups[0]);
    if (id > 0) return id;
  }
  const pods = body.contractPodsResponses;
  if (Array.isArray(pods) && pods.length > 0) {
    const row = pods[0] as { billingGroupId?: unknown };
    const id = numericId(row.billingGroupId);
    if (id > 0) return id;
  }
  throw new Error(
    `Cannot resolve billingGroupId from product contract ${contractId}: ${JSON.stringify(body).slice(0, 400)}`,
  );
}

/**
 * MANUAL customer-liability on the contract billing group + due-date-change yesterday
 * (PDT-2913 / PDT-3179). Prefer this over billing-run for overdue unpaid liability.
 */
export async function createOverdueManualLiability(
  fx: Pdt3211Fx,
  customerIndex: number,
  contractIndex: number,
): Promise<{ liabilityId: number; payload: Record<string, unknown> }> {
  const billingGroupId = await resolveContractBillingGroupId(fx, contractIndex);
  const yesterdayDdMmYyyy = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  const payload = fx.GeneratePayload.receivablesManagement.customer_liability() as Record<
    string,
    unknown
  >;
  payload.customerId = fx.Responses.customer[customerIndex].id;
  payload.billingGroupId = billingGroupId;
  payload.initialAmount = PDT_3211_MANUAL_LIABILITY_AMOUNT;
  payload.occurrenceDate = yesterdayDdMmYyyy;
  payload.dueDate = yesterdayDdMmYyyy;

  const res = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  await expect(res).CheckResponse();
  const liabilityId = entityId(await res.json());
  expect(liabilityId, 'POST /customer-liability must return liability id').toBeGreaterThan(0);
  fx.Responses.customerLiability.push(liabilityId);

  const dueDateChange = await fx.Request.put(
    `customer-liability/${liabilityId}/due-date-change?dueDate=${yesterdayYmd()}`,
  );
  await expect(dueDateChange).CheckResponse();

  return { liabilityId, payload };
}

/**
 * Dev2 PowerSupplyDisconnectionReminderBaseRequest (PDT-2913 shape).
 * LIST_OF_CUSTOMERS + listOfCustomer; omit legacy customerList / customerFilterType.
 */
export async function createDev2ReminderForDisconnection(
  fx: Pdt3211Fx,
  listOfCustomer: string,
): Promise<{ reminderId: number; payload: Record<string, unknown> }> {
  expect(listOfCustomer.length, 'listOfCustomer minLength 1 (do not send empty string)').toBeGreaterThan(0);
  const base = fx.GeneratePayload.receivablesManagement.reminderForDisconnection() as Record<
    string,
    unknown
  >;
  const payload: Record<string, unknown> = {
    customerSendToDateAndTime: base.customerSendToDateAndTime,
    liabilityAmountFrom: base.liabilityAmountFrom ?? null,
    liabilityAmountTo: base.liabilityAmountTo ?? null,
    liabilitiesMaxDueDate: todayYmd(),
    listOfCustomer,
    conditionType: 'LIST_OF_CUSTOMERS',
    communicationChannels: ['EMAIL'],
    emailTemplateId: envVariables.reminder_disconnection_email_template,
    smsTemplateId: null,
    documentTemplateId: null,
    disconnectionDate: base.disconnectionDate,
    printedFileName: base.printedFileName ?? '1',
    name: `PDT3211-${randomGens.generateRandomString(true, true, 10)}`,
    confirm: true,
  };
  expect(payload.conditionType, 'never ALL_CUSTOMERS on Dev2').toBe('LIST_OF_CUSTOMERS');

  const response = await fx.Request.post(fx.Endpoints.reminderForDisconnection, { data: payload });
  await expect(response).CheckResponse();
  const body = await response.json();
  fx.Responses.reminderForDisconnection.push(body);
  return { reminderId: reminderIdOf(body), payload };
}

/**
 * set-customer-send-time + POST /job + poll until EXECUTED.
 * Does not call PDT-3179 resolveReminderIdForRfd.
 */
export async function executeReminderUntilExecuted(fx: Pdt3211Fx, reminderId: number): Promise<void> {
  expect(reminderId, 'reminderId required for execute').toBeGreaterThan(0);
  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const timeOffset = await fx.Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${reminderId}&hour=${georgianTime.getHours()}&minute=${georgianTime.getMinutes()}`,
  );
  await expect(timeOffset).CheckResponse();

  const jobPost = await fx.Request.post('power-supply-disconnection-reminder/job');
  await expect(jobPost).CheckResponse();

  const interval = 5 * 1000;
  const maxAttempts = Math.floor(PDT_3211_REMINDER_POLL_MS / interval);
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const reminderGet = await fx.Request.get(`power-supply-disconnection-reminder/${reminderId}`);
      await expect(reminderGet).CheckResponse();
      const reminderJson = (await reminderGet.json()) as { reminderStatus?: string };
      if (reminderJson.reminderStatus === 'EXECUTED') {
        return;
      }
      if (attempt < maxAttempts) {
        await new Promise((r) => setTimeout(r, interval));
      }
    } catch (err) {
      if (attempt === maxAttempts) {
        throw new Error(`Failed to verify reminder ${reminderId} status after ${maxAttempts} attempts: ${err}`);
      }
      await new Promise((r) => setTimeout(r, interval));
    }
  }
  throw new Error(
    `Reminder for disconnection ${reminderId} did not reach EXECUTED within ${PDT_3211_REMINDER_POLL_MS}ms`,
  );
}

export async function fetchReminderSecondTab(
  fx: Pdt3211Fx,
  reminderId: number,
): Promise<Pdt3211ReminderSecondTabRow[]> {
  const res = await fx.Request.get(
    `${fx.Endpoints.reminderForDisconnection}/second-tab?page=0&size=100&powerSupplyDisconnectionReminderId=${reminderId}`,
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  return ((body as { content?: Pdt3211ReminderSecondTabRow[] }).content ?? []) as Pdt3211ReminderSecondTabRow[];
}

export function buildLoadCustomersQuery(opts: {
  reminderId: number;
  listOfCustomer: string;
  supplierType: Pdt3211SupplierType;
}): Pdt3211LoadQuery {
  expect(opts.listOfCustomer.length, 'CustomersForDPSRequest.listOfCustomer minLength 1').toBeGreaterThan(0);
  expect(opts.reminderId, 'powerSupplyDisconnectionReminderId').toBeGreaterThan(0);
  return {
    page: 0,
    size: 100,
    conditionType: 'LIST_OF_CUSTOMERS',
    listOfCustomer: opts.listOfCustomer,
    powerSupplyDisconnectionReminderId: opts.reminderId,
    gridOperatorId: gridOperatorIdFromEnv(),
    supplierType: opts.supplierType,
  };
}

export async function fetchLoadCustomersForDps(
  fx: Pdt3211Fx,
  query: Pdt3211LoadQuery,
): Promise<{ status: number; rows: Pdt3211PodTabRow[] }> {
  expect(query.conditionType, 'never ALL_CUSTOMERS on Dev2 Load PODs').toBe('LIST_OF_CUSTOMERS');
  const res = await fx.Request.get(PDT_3211_LOAD_CUSTOMERS_PATH, {
    params: query as unknown as Record<string, string | number>,
    timeout: PDT_3211_LOAD_GET_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const status = res.status();
  expect(status, 'Load PODs must be 2xx (Phoenix 206 PARTIAL_CONTENT is success)').toBeGreaterThanOrEqual(200);
  expect(status, 'Load PODs must be 2xx').toBeLessThan(300);
  const body = await res.json();
  return {
    status,
    rows: ((body as { content?: Pdt3211PodTabRow[] }).content ?? []) as Pdt3211PodTabRow[],
  };
}

export async function fetchViewPodTab(
  fx: Pdt3211Fx,
  requestId: number,
  query: Pdt3211LoadQuery,
): Promise<{ status: number; rows: Pdt3211PodTabRow[] }> {
  const res = await fx.Request.get(
    `${fx.Endpoints.requestForDisconnection}/view-pod-tab/${requestId}`,
    {
      params: query as unknown as Record<string, string | number>,
      timeout: PDT_3211_LOAD_GET_TIMEOUT_MS,
    },
  );
  await expect(res).CheckResponse();
  const status = res.status();
  expect(status, 'view-pod-tab must be 2xx (Phoenix 206 PARTIAL_CONTENT is success)').toBeGreaterThanOrEqual(
    200,
  );
  expect(status, 'view-pod-tab must be 2xx').toBeLessThan(300);
  const body = await res.json();
  return {
    status,
    rows: ((body as { content?: Pdt3211PodTabRow[] }).content ?? []) as Pdt3211PodTabRow[],
  };
}

/**
 * POST DRAFT RFD. Always set validityPeriodFrom (today) and validityPeriodTo
 * (last day of current month). Never allSelected true / ALL_CUSTOMERS.
 *
 * DisconnectionPowerSupplyRequestsValidator.java ~88-139 (origin/dev2):
 * empty/null pods AND allSelected=false AND podWithHighestConsumption=false
 * → 400 "pods-pods can not be null". Empty pods is allowed if allSelected OR
 * podWithHighestConsumption is true. Do not use allSelected (Dev2 select-all SQL).
 */
export async function createDraftRfd(fx: Pdt3211Fx, opts: {
  reminderId: number;
  listOfCustomer: string;
  supplierType: Pdt3211SupplierType;
}): Promise<{ requestId: number; payload: Record<string, unknown> }> {
  expect(opts.listOfCustomer.length, 'RFD listOfCustomer minLength 1').toBeGreaterThan(0);
  const payload = fx.GeneratePayload.receivablesManagement.requestForDisconnection() as Record<
    string,
    unknown
  >;
  payload.reminderForDisconnectionId = opts.reminderId;
  payload.gridOpRequestRegDate = todayYmd();
  payload.supplierType = opts.supplierType;
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = opts.listOfCustomer;
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.allSelected = false;
  payload.pods = [];
  payload.excludePodIds = [];
  const podsEmpty = !Array.isArray(payload.pods) || (payload.pods as unknown[]).length === 0;
  payload.podWithHighestConsumption = podsEmpty;
  payload.templateIds = [];
  payload.files = [];
  const validityPeriodFrom = todayYmd();
  payload.validityPeriodFrom = validityPeriodFrom;
  payload.validityPeriodTo = monthEndYmd();
  expect(payload.conditionType, 'never ALL_CUSTOMERS on Dev2').toBe('LIST_OF_CUSTOMERS');
  expect(payload.allSelected, 'never allSelected=true on Dev2').toBe(false);

  const res = await fx.Request.post(fx.Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3211_RFD_POST_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  fx.Responses.requestForDisconnection.push(requestId);
  return { requestId, payload };
}
