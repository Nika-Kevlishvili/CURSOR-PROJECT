/**
 * PDT-3169 helpers — Cancellation CREATE table: FE current-page payload omit.
 *
 * Bug-only (no TC .md). Creates ALL data from scratch. No test.beforeAll.
 * Do not hardcode TEST Request-1329 / UIC 148143678 / Dev2 Request-1825.
 *
 * Product rule (Confluence Cancellation Create page 73990412 + PDT-3042 SQL):
 * auto-check + unableToCheck when remaining == 0 or remaining < liabilityAmountFrom;
 * locked PODs cannot be omitted from POST table (validateTable → can't be unchecked).
 *
 * Jira path: filter UIC so table has one visible row; old FE posts only that row
 * and omits auto-checked PODs on other pages → HTTP 400.
 *
 * Swagger (dev2, http://10.236.20.11:8092/v3/api-docs):
 * - GET .../table-content query DisconnectionOfPowerSupplyGetDraftTableRequest
 *   required page, size, requestForDisconnectionId
 *   searchFields enum ALL | CUSTOMER_IDENTIFIER | CUSTOMER_NUMBER | POD_IDENTIFIER
 *   prompt; response PagePowerSupplyDcnCancellationTableResponse.totalElements + content
 *   PowerSupplyDcnCancellationTableResponse.checked, unableToCheck
 * - POST .../ CancellationOfThePowerSupplyRequest
 *   required requestForDisconnectionOfThePowerSupplyId, saveAs DRAFT|EXECUTED
 *   table[] CancellationPodRequest: customerId, podId,
 *     requestForDisconnectionOfPowerSupplyId, cancellationReasonId
 * - DPSRequestsBaseRequest required conditionType, gridOpRequestRegDate, gridOperatorId,
 *   reasonOfDisconnectionId, reminderForDisconnectionId, supplierType,
 *   validityPeriodFrom, validityPeriodTo; currencyId when liabilityAmountFrom set
 *
 * Reference:
 * - tests/cursor/PDT-3042-cancellation-pod-auto-check.spec.ts
 * - tests/cursor/pdt-3042-cancellation-pod-auto-check.fixtures.ts
 * - tests/cursor/PDT-2971-rfd-shared-pod-multi-customer-merge.spec.ts
 * - tests/cursor/pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts
 * - tests/cursor/pdt-2913-missing-email-object-reminder-for-disconnection.fixtures.ts
 * - tests/receivableManagement/cancelationOfRequestForDisconnection.spec.ts (REG-1161)
 * - tests/receivableManagement/requestForDisconnection.spec.ts (REG-1045)
 */

import { expect } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  CUSTOMER_A_INDEX,
  CUSTOMER_B_INDEX,
  CONTRACT_A_INDEX,
  CONTRACT_B_INDEX,
  buildManualInvoicePayloadForContract,
  type Pdt2971Fx,
} from './pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures';
import { setupPdt2913ContractChain } from './pdt-2913-missing-email-object-reminder-for-disconnection.fixtures';
import {
  PDT_3042_LIABILITY_50,
  PDT_3042_RFD_FROM,
  PDT_3042_RFD_HTTP_TIMEOUT_MS,
  PDT_3042_TEST_TIMEOUT_MS,
  asNumber,
  createPdt3042ReminderForDisconnection,
  entityId,
  errorHaystack,
  executePdt3042ReminderJob,
  payPdt3042Amount,
  pdt3042RelevantKeys,
  postCancellationDraft,
  requireReasonForCancellation,
  type Pdt3042Fx,
  type Pdt3042TableRow,
} from './pdt-3042-cancellation-pod-auto-check.fixtures';
import {
  getLiability,
  nestedId,
  todayYmd,
} from './pdt-3179-reconnection-invoice-emails.fixtures';

export const PDT_3169_KEY = 'PDT-3169';
/** Exact Jira summary — keep ticket spelling. */
export const PDT_3169_TITLE = 'Cancellation of request for disc - Error';

/** Two-customer chain + reminder job HTTP/poll + EXECUTED RFD (same class as PDT-3042). */
export const PDT_3169_TEST_TIMEOUT_MS = Math.max(PDT_3042_TEST_TIMEOUT_MS, 45 * 60 * 1000);
export const PDT_3169_RFD_FROM = PDT_3042_RFD_FROM;
export const PDT_3169_INVOICE_AMOUNT = PDT_3042_LIABILITY_50;

export const POD_A_INDEX = 0;
export const POD_B_INDEX = 1;

export type Pdt3169Fx = Pdt3042Fx;

export type Pdt3169SearchFields =
  | 'ALL'
  | 'CUSTOMER_IDENTIFIER'
  | 'CUSTOMER_NUMBER'
  | 'POD_IDENTIFIER';

export type Pdt3169TableQuery = {
  page: number;
  size: number;
  requestForDisconnectionId: number;
  searchFields?: Pdt3169SearchFields;
  prompt?: string;
};

export type Pdt3169TableContent = {
  status: number;
  rows: Pdt3042TableRow[];
  body: unknown;
  totalElements: number;
};

export type Pdt3169Chain = {
  customerAId: number;
  customerBId: number;
  customerAIdentifier: string;
  customerBIdentifier: string;
  podAId: number;
  podBId: number;
  contractAId: number;
  contractBId: number;
  reminderId: number;
  rfdId: number;
  liabilityIdsA: number[];
  liabilityIdsB: number[];
  invoiceAId: number;
  invoiceBId: number;
};

function asPdt2971Fx(fx: Pdt3169Fx): Pdt2971Fx {
  return fx as unknown as Pdt2971Fx;
}

function customerIdentAt(fx: Pdt3169Fx, customerIndex: number): string {
  const identifier = String(
    (fx.Responses.customer[customerIndex] as { identifier?: unknown } | undefined)?.identifier ?? '',
  );
  expect(
    identifier.length,
    `Responses.customer[${customerIndex}].identifier is required`,
  ).toBeGreaterThan(0);
  return identifier;
}

function lastDayOfMonthYmd(ymd: string): string {
  const [year, month] = ymd.split('-').map(Number);
  const last = new Date(year, month, 0);
  const yyyy = last.getFullYear();
  const mm = String(last.getMonth() + 1).padStart(2, '0');
  const dd = String(last.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

function logElapsed(started: number, message: string): void {
  console.log(`[PDT-3169] ${Date.now() - started}ms ${message}`);
}

function listingRows(body: unknown): Pdt3042TableRow[] {
  if (Array.isArray(body)) return body as Pdt3042TableRow[];
  if (body && typeof body === 'object' && Array.isArray((body as { content?: unknown }).content)) {
    return (body as { content: Pdt3042TableRow[] }).content;
  }
  return [];
}

/** Swagger PagePowerSupplyDcnCancellationTableResponse.totalElements (fallback totalCount). */
export function pageTotalElements(body: unknown, rows: Pdt3042TableRow[]): number {
  if (body && typeof body === 'object') {
    const rec = body as { totalElements?: unknown; totalCount?: unknown };
    const totalElements = asNumber(rec.totalElements);
    if (totalElements > 0) return totalElements;
    const totalCount = asNumber(rec.totalCount);
    if (totalCount > 0) return totalCount;
  }
  return rows.length;
}

function readLiabilityBillingGroupId(body: Record<string, unknown>): number {
  const billingGroupResponse = body.billingGroupResponse as { id?: unknown } | null | undefined;
  return (
    asNumber(billingGroupResponse?.id) ||
    asNumber(body.contractBillingGroupId) ||
    asNumber(body.billingGroupId)
  );
}

function readLiabilityInvoiceId(body: Record<string, unknown>): number | null {
  const invoiceResponse = body.invoiceResponse as { id?: number } | null | undefined;
  if (invoiceResponse?.id != null) {
    const id = Number(invoiceResponse.id);
    if (Number.isFinite(id) && id > 0) return id;
  }
  if (body.invoiceId != null && body.invoiceId !== '') {
    const id = Number(body.invoiceId);
    if (Number.isFinite(id) && id > 0) return id;
  }
  return null;
}

async function liabilityIdFromInvoice(fx: Pdt3169Fx, invoiceId: number): Promise<number> {
  const invoiceGet = await fx.Request.get(`${fx.Endpoints.invoice}?id=${invoiceId}`);
  await expect(invoiceGet).CheckResponse();
  const body = (await invoiceGet.json()) as {
    liabilitiesAndReceivables?: Array<{ id?: number; type?: string }>;
  };
  const rows = body.liabilitiesAndReceivables ?? [];
  const liabilityRow =
    rows.find((row) => String(row.type ?? '').toUpperCase() === 'LIABILITY') ?? rows[0];
  expect(
    liabilityRow,
    `invoice ${invoiceId} must have liabilitiesAndReceivables LIABILITY after accounting`,
  ).toBeTruthy();
  const liabilityId = entityId(liabilityRow!.id);
  expect(liabilityId, `invoice ${invoiceId} liability id`).toBeGreaterThan(0);
  return liabilityId;
}

/**
 * setLiabilityInfo parses last hyphen segment via findAllById (customer_liability.id).
 * When we build pods[] ourselves, last segment must be the DB id.
 */
function liabilityToken(liab: Record<string, unknown>, liabilityId: number): string {
  const amount = asNumber(liab.currentAmount) || asNumber(liab.initialAmount);
  const currencyName = String(
    (liab.currencyResponse as { name?: unknown } | undefined)?.name ?? 'BGN',
  ).replace(/-/g, ' ');
  return `${amount}-${currencyName}-${liabilityId}`;
}

function nestedTableParams(query: Pdt3169TableQuery): Record<string, string | number> {
  const params: Record<string, string | number> = {
    'request.page': query.page,
    'request.size': query.size,
    'request.requestForDisconnectionId': query.requestForDisconnectionId,
  };
  if (query.searchFields) params['request.searchFields'] = query.searchFields;
  if (query.prompt != null && query.prompt.length > 0) params['request.prompt'] = query.prompt;
  return params;
}

function flatTableParams(query: Pdt3169TableQuery): Record<string, string | number> {
  const params: Record<string, string | number> = {
    page: query.page,
    size: query.size,
    requestForDisconnectionId: query.requestForDisconnectionId,
  };
  if (query.searchFields) params.searchFields = query.searchFields;
  if (query.prompt != null && query.prompt.length > 0) params.prompt = query.prompt;
  return params;
}

/**
 * GET table-content with page/size/searchFields/prompt.
 * PDT-3042 getCancellationTableContent is hardcoded size 25 — wrap here, do not change 3042.
 * Tries nested request.* then flat (same fallback as PDT-3042).
 */
export async function getPdt3169CancellationTableContent(
  fx: Pdt3169Fx,
  query: Pdt3169TableQuery,
): Promise<Pdt3169TableContent> {
  const path = `${fx.Endpoints.cancellationOfRequestOfDisconnection}/table-content`;
  let res = await fx.Request.get(path, { params: nestedTableParams(query) });
  if (!res.ok()) {
    res = await fx.Request.get(path, { params: flatTableParams(query) });
  }
  await expect(res).CheckResponse();
  const body = await res.json();
  const rows = listingRows(body);
  return {
    status: res.status(),
    rows,
    body,
    totalElements: pageTotalElements(body, rows),
  };
}

export function findCustomerPodRow(
  rows: Pdt3042TableRow[],
  customerId: number,
  podId: number,
): Pdt3042TableRow | undefined {
  return rows.find(
    (row) => Number(row.customerId) === customerId && Number(row.podId) === podId,
  );
}

export function buildPdt3169CancellationDraftPayload(
  fx: Pdt3169Fx,
  rfdId: number,
  table: Array<Record<string, unknown>>,
): Record<string, unknown> {
  const base = fx.GeneratePayload.receivablesManagement.cancellationOfRequestForDisconnection() as Record<
    string,
    unknown
  >;
  return {
    requestForDisconnectionOfThePowerSupplyId: rfdId,
    saveAs: 'DRAFT',
    fileIds: Array.isArray(base.fileIds) ? base.fileIds : [],
    templateIds: Array.isArray(base.templateIds) ? base.templateIds : [],
    table,
  };
}

export function cancellationPodRowFor(opts: {
  customerId: number;
  podId: number;
  rfdId: number;
  cancellationReasonId: number;
}): Record<string, unknown> {
  return {
    customerId: opts.customerId,
    podId: opts.podId,
    requestForDisconnectionOfPowerSupplyId: opts.rfdId,
    cancellationReasonId: opts.cancellationReasonId,
  };
}

export function restErrorFields(
  status: number,
  text: string,
  json: Record<string, unknown> | null,
): { haystack: string; errorCode: string; exceptionId: string; message: string } {
  const haystack = errorHaystack(status, text, json);
  const errorCode = String(json?.errorCode ?? '');
  const exceptionId = String(json?.exceptionId ?? '');
  const message = String(json?.message ?? '');
  return { haystack, errorCode, exceptionId, message };
}

async function createPdt3169Customer(fx: Pdt3169Fx): Promise<void> {
  const customer = await fx.Request.post(fx.Endpoints.customer, {
    data: fx.GeneratePayload.customers.customer_legal(),
  });
  await expect(customer).CheckResponse();
  fx.Responses.customer.push(await customer.json());
}

async function createPdt3169Pod(fx: Pdt3169Fx): Promise<void> {
  const pod = await fx.Request.post(fx.Endpoints.pod, {
    data: fx.GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod).CheckResponse();
  fx.Responses.pod.push(await pod.json());
}

async function createPdt3169Contract(
  fx: Pdt3169Fx,
  opts: { customerIndex: number; productIndex: number; podIndex: number },
): Promise<number> {
  const expectedCustomerId = entityId(fx.Responses.customer[opts.customerIndex]);
  const payload = await fx.GeneratePayload.contractsAndOrders.product_contract(
    opts.customerIndex,
    opts.productIndex,
    opts.podIndex,
  );
  payload.basicParameters.customerId = expectedCustomerId;
  payload.productParameters.contractType = 'SUPPLY_ONLY';

  const getCustomer = await fx.Request.get(`customer/${expectedCustomerId}?version=1`);
  await expect(getCustomer).CheckResponse();
  const customerBody = (await getCustomer.json()) as {
    communicationData?: Array<{ id?: number }>;
  };
  const commId = customerBody.communicationData?.[0]?.id;
  if (commId) {
    payload.basicParameters.communicationDataBillingId = commId;
    payload.basicParameters.communicationDataContractId = commId;
  }

  const contract = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  await expect(contract).CheckResponse();
  const body = await contract.json();
  fx.Responses.productContract.push(body);
  const contractId = entityId(body);

  const view = await fx.Request.get(`${fx.Endpoints.productContract}/${contractId}?version=1`);
  await expect(view).CheckResponse();
  const viewBody = (await view.json()) as {
    basicParameters?: { customerId?: unknown };
  };
  const gotCustomerId = nestedId(viewBody.basicParameters?.customerId);
  expect(
    gotCustomerId,
    `GET product-contract ${contractId} basicParameters.customerId must be customerIndex=${opts.customerIndex} (id=${expectedCustomerId}); generator still writes customer[0] internally`,
  ).toBe(expectedCustomerId);
  return contractId;
}

async function activatePdt3169Pod(
  fx: Pdt3169Fx,
  opts: { podIndex: number; contractIndex: number },
): Promise<void> {
  const activation = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(
      opts.podIndex,
      undefined,
      undefined,
      opts.contractIndex,
    ),
  });
  await expect(activation).CheckResponse();
}

/**
 * Manual invoice on a specific customer/contract (PDT-2971 buildManualInvoicePayloadForContract).
 * Do not use GeneratePayload.billing.manualInvoice() for customer B — it always binds customer[0]/contract[0].
 */
export async function createPdt3169ManualInvoiceForContract(
  fx: Pdt3169Fx,
  opts: {
    customerIndex: number;
    contractIndex: number;
    summaryValue: number;
    billingIndex: number;
  },
): Promise<{ invoiceId: number; liabilityId: number }> {
  const payload = (await buildManualInvoicePayloadForContract(
    asPdt2971Fx(fx),
    opts.customerIndex,
    opts.contractIndex,
  )) as {
    manualInvoiceParameters: {
      manualInvoiceSummaryDataParameters: { summaryDataRowList: Array<{ value: number }> };
    };
  };
  payload.manualInvoiceParameters.manualInvoiceSummaryDataParameters.summaryDataRowList[0].value =
    opts.summaryValue;

  const create = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(create).CheckResponse();
  fx.Responses.billingRun.push(await create.json());
  await fx.GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, opts.billingIndex);

  const invoiceId = entityId(fx.Responses.invoice[fx.Responses.invoice.length - 1]);
  expect(
    invoiceId,
    `invoice id after billingIndex=${opts.billingIndex} customerIndex=${opts.customerIndex}`,
  ).toBeGreaterThan(0);
  const liabilityId = await liabilityIdFromInvoice(fx, invoiceId);
  const liability = await getLiability(fx, liabilityId);
  const billingGroupId = readLiabilityBillingGroupId(liability);
  expect(
    billingGroupId,
    `PDT-3169: after invoice ${invoiceId}, GET customer-liability/${liabilityId} must have ` +
      `contractBillingGroupId / billingGroupResponse.id > 0 ` +
      `(execute() requires contract_billing_group_id IS NOT NULL unless rescheduling).`,
  ).toBeGreaterThan(0);
  fx.Responses.customerLiability.push(liabilityId);
  return { invoiceId, liabilityId };
}

/** Overdue every OPEN liability for one customer identifier (size=100, PDT-3042 pattern). */
export async function shiftPdt3169CustomerLiabilitiesToYesterday(
  fx: Pdt3169Fx,
  customerIndex: number,
): Promise<number[]> {
  const identifier = customerIdentAt(fx, customerIndex);
  const liabilitiesRes = await fx.Request.get(
    `customer-liability/list?page=0&size=100&columns=ID&direction=DESC&prompt=${identifier}&searchFields=CUSTOMER`,
  );
  await expect(liabilitiesRes).CheckResponse();
  const body = (await liabilitiesRes.json()) as {
    content?: Array<{ id: number; currentAmount?: number }>;
  };
  const rows = body.content ?? [];
  const open = rows.filter((row) => (row.currentAmount ?? 0) > 0);
  expect(open.length, `Expected open liability for customer ${identifier}`).toBeGreaterThan(0);

  const dueDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');
  const overdueIds: number[] = [];
  for (const row of open) {
    const change = await fx.Request.put(
      `customer-liability/${row.id}/due-date-change?dueDate=${dueDate}`,
    );
    await expect(change).CheckResponse();
    overdueIds.push(row.id);
  }
  return overdueIds;
}

export async function shiftPdt3169AllCustomersLiabilitiesToYesterday(
  fx: Pdt3169Fx,
): Promise<void> {
  expect(fx.Responses.customer.length, 'need customer A and customer B').toBeGreaterThanOrEqual(2);
  await shiftPdt3169CustomerLiabilitiesToYesterday(fx, CUSTOMER_A_INDEX);
  await shiftPdt3169CustomerLiabilitiesToYesterday(fx, CUSTOMER_B_INDEX);
}

async function buildPdt3169RfdPodRow(
  fx: Pdt3169Fx,
  opts: {
    customerIndex: number;
    podIndex: number;
    contractIndex: number;
    liabilityIds: number[];
  },
): Promise<Record<string, unknown>> {
  const customer = fx.Responses.customer[opts.customerIndex] as Record<string, unknown>;
  const pod = fx.Responses.pod[opts.podIndex] as Record<string, unknown>;
  const podId = entityId(pod);
  const customerId = entityId(customer);
  const identifier = customerIdentAt(fx, opts.customerIndex);
  const contractId = entityId(fx.Responses.productContract[opts.contractIndex]);

  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}?version=1`);
  await expect(getRes).CheckResponse();
  const body = (await getRes.json()) as Record<string, unknown>;
  const versions = Array.isArray(body.versions) ? (body.versions as Record<string, unknown>[]) : [];

  const contractGet = await fx.Request.get(`${fx.Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const contractPods = Array.isArray(contractBody.contractPodsResponses)
    ? (contractBody.contractPodsResponses as Record<string, unknown>[])
    : [];
  const podDetailId =
    nestedId(pod.podDetailId) ??
    nestedId(body.podDetailId) ??
    nestedId(body.lastPodDetailId) ??
    nestedId(versions[0]?.podDetailId) ??
    nestedId(contractPods[0]?.podDetailId) ??
    nestedId(contractPods[0]?.pointOfDeliveryDetailId);
  expect(podDetailId, `podDetailId required on RFD pods[] for podIndex=${opts.podIndex}`).toBeGreaterThan(
    0,
  );

  const tokens: string[] = [];
  let liabilitySum = 0;
  for (const id of opts.liabilityIds) {
    const liab = await getLiability(fx, id);
    tokens.push(liabilityToken(liab, id));
    liabilitySum += asNumber(liab.currentAmount) || asNumber(liab.initialAmount);
  }
  const tokenCsv = tokens.join(',');

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
    liabilitiesInBillingGroup: tokenCsv,
    liabilitiesInPod: tokenCsv,
    liabilityAmountCustomer: liabilitySum,
  };
}

export async function createPdt3169ExecutedRfd(
  fx: Pdt3169Fx,
  opts: {
    liabilityIdsA: number[];
    liabilityIdsB: number[];
    liabilityAmountFrom: number;
  },
): Promise<number> {
  const started = Date.now();
  const listOfCustomer = [customerIdentAt(fx, CUSTOMER_A_INDEX), customerIdentAt(fx, CUSTOMER_B_INDEX)].join(
    ',',
  );
  const reminderId = entityId(fx.Responses.reminderForDisconnection[0]);
  const podA = await buildPdt3169RfdPodRow(fx, {
    customerIndex: CUSTOMER_A_INDEX,
    podIndex: POD_A_INDEX,
    contractIndex: CONTRACT_A_INDEX,
    liabilityIds: opts.liabilityIdsA,
  });
  const podB = await buildPdt3169RfdPodRow(fx, {
    customerIndex: CUSTOMER_B_INDEX,
    podIndex: POD_B_INDEX,
    contractIndex: CONTRACT_B_INDEX,
    liabilityIds: opts.liabilityIdsB,
  });

  const payload = fx.GeneratePayload.receivablesManagement.requestForDisconnection() as Record<
    string,
    unknown
  >;
  payload.reminderForDisconnectionId = reminderId;
  payload.gridOpRequestRegDate = todayYmd();
  payload.disconnectionRequestsStatus = 'EXECUTED';
  payload.supplierType = 'CURRENT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = listOfCustomer;
  payload.allSelected = false;
  payload.podWithHighestConsumption = false;
  payload.excludePodIds = [];
  payload.pods = [podA, podB];
  payload.templateIds = [];
  payload.files = [];
  payload.liabilityAmountFrom = opts.liabilityAmountFrom;
  payload.liabilityAmountTo = null;
  const currencyId = asNumber(envVariables.currency);
  expect(
    currencyId,
    'envVariables.currency is mandatory when liabilityAmountFrom is set (Dev2 DPSRequestsBaseRequest)',
  ).toBeGreaterThan(0);
  payload.currencyId = currencyId;
  const validityPeriodFrom = todayYmd();
  payload.validityPeriodFrom = validityPeriodFrom;
  payload.validityPeriodTo = lastDayOfMonthYmd(validityPeriodFrom);
  payload.name = `PDT3169-${randomGens.generateRandomString(true, true, 8)}`;

  const res = await fx.Request.post(fx.Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3042_RFD_HTTP_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  fx.Responses.requestForDisconnection.push(requestId);
  logElapsed(started, `POST EXECUTED RFD ${requestId} from=${opts.liabilityAmountFrom} pods=2`);
  return requestId;
}

/**
 * Pay every OPEN liability for Customer A (index 0) so remaining == 0 → auto-check lock.
 * payPdt3042Amount uses the payment generator customer[0] — Customer A must stay index 0.
 * Leave Customer B unpaid.
 */
export async function payPdt3169CustomerALiabilitiesFully(
  fx: Pdt3169Fx,
  liabilityIdsA: number[],
): Promise<{ remaining: number; paymentIds: number[] }> {
  const identifier = customerIdentAt(fx, CUSTOMER_A_INDEX);
  const listRes = await fx.Request.get(
    `customer-liability/list?page=0&size=100&columns=ID&direction=DESC&prompt=${identifier}&searchFields=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const listBody = (await listRes.json()) as {
    content?: Array<{ id: number; currentAmount?: number }>;
  };
  const listedIds = (listBody.content ?? [])
    .filter((row) => (row.currentAmount ?? 0) > 0)
    .map((row) => row.id);
  const ids = Array.from(new Set([...liabilityIdsA, ...listedIds]));
  const paymentIds: number[] = [];
  for (const id of ids) {
    const liab = await getLiability(fx, id);
    const current = asNumber(liab.currentAmount);
    if (current <= 0) continue;
    const invoiceId = readLiabilityInvoiceId(liab);
    paymentIds.push(await payPdt3042Amount(fx, current, invoiceId ?? undefined));
  }

  let remaining = 0;
  for (const id of liabilityIdsA) {
    const liab = await getLiability(fx, id);
    remaining += asNumber(liab.currentAmount);
  }
  expect(
    remaining,
    `Customer A reminder liabilities ${liabilityIdsA.join(',')} remaining after full pay must be 0`,
  ).toBeCloseTo(0, 2);
  return { remaining, paymentIds };
}

export async function setupPdt3169TwoPodCancellationChain(
  fx: Pdt3169Fx,
  opts?: { liabilityAmountFrom?: number },
): Promise<Pdt3169Chain> {
  const from = opts?.liabilityAmountFrom ?? PDT_3169_RFD_FROM;

  await setupPdt2913ContractChain(fx);
  const customerAId = entityId(fx.Responses.customer[CUSTOMER_A_INDEX]);
  const contractAId = entityId(fx.Responses.productContract[CONTRACT_A_INDEX]);
  const contractAView = await fx.Request.get(`${fx.Endpoints.productContract}/${contractAId}?version=1`);
  await expect(contractAView).CheckResponse();
  const contractABody = (await contractAView.json()) as {
    basicParameters?: { customerId?: unknown };
  };
  expect(
    nestedId(contractABody.basicParameters?.customerId),
    `setupPdt2913ContractChain contract A ${contractAId} must belong to customer A ${customerAId} (generator writes customer[0])`,
  ).toBe(customerAId);

  await createPdt3169Customer(fx);
  await createPdt3169Pod(fx);
  await createPdt3169Contract(fx, {
    customerIndex: CUSTOMER_B_INDEX,
    productIndex: 0,
    podIndex: POD_B_INDEX,
  });
  await activatePdt3169Pod(fx, { podIndex: POD_B_INDEX, contractIndex: CONTRACT_B_INDEX });

  const invoiceA = await createPdt3169ManualInvoiceForContract(fx, {
    customerIndex: CUSTOMER_A_INDEX,
    contractIndex: CONTRACT_A_INDEX,
    summaryValue: PDT_3169_INVOICE_AMOUNT,
    billingIndex: 0,
  });
  const invoiceB = await createPdt3169ManualInvoiceForContract(fx, {
    customerIndex: CUSTOMER_B_INDEX,
    contractIndex: CONTRACT_B_INDEX,
    summaryValue: PDT_3169_INVOICE_AMOUNT,
    billingIndex: 1,
  });

  const liabilityIdsA = [invoiceA.liabilityId];
  const liabilityIdsB = [invoiceB.liabilityId];
  const allLiabilityIds = [...liabilityIdsA, ...liabilityIdsB];
  expect(
    allLiabilityIds.length,
    'executePdt3042ReminderJob requires exactly two invoice liability ids (one per customer)',
  ).toBe(2);

  await shiftPdt3169AllCustomersLiabilitiesToYesterday(fx);
  const reminder = await createPdt3042ReminderForDisconnection(fx);
  await shiftPdt3169AllCustomersLiabilitiesToYesterday(fx);
  await executePdt3042ReminderJob(fx, allLiabilityIds);

  const rfdId = await createPdt3169ExecutedRfd(fx, {
    liabilityIdsA,
    liabilityIdsB,
    liabilityAmountFrom: from,
  });

  const podA = fx.Responses.pod[POD_A_INDEX] as Record<string, unknown>;
  const podB = fx.Responses.pod[POD_B_INDEX] as Record<string, unknown>;
  return {
    customerAId: entityId(fx.Responses.customer[CUSTOMER_A_INDEX]),
    customerBId: entityId(fx.Responses.customer[CUSTOMER_B_INDEX]),
    customerAIdentifier: customerIdentAt(fx, CUSTOMER_A_INDEX),
    customerBIdentifier: customerIdentAt(fx, CUSTOMER_B_INDEX),
    podAId: entityId(podA),
    podBId: entityId(podB),
    contractAId: entityId(fx.Responses.productContract[CONTRACT_A_INDEX]),
    contractBId: entityId(fx.Responses.productContract[CONTRACT_B_INDEX]),
    reminderId: reminder.reminderId,
    rfdId,
    liabilityIdsA,
    liabilityIdsB,
    invoiceAId: invoiceA.invoiceId,
    invoiceBId: invoiceB.invoiceId,
  };
}

export function pdt3169RelevantKeys(): string[] {
  return pdt3042RelevantKeys({ includePayment: true, includeCancellation: true });
}

export function rowCancellationReasonId(row: Pdt3042TableRow | undefined, context: string): number {
  const fromRow = asNumber(row?.cancellationReasonId);
  if (fromRow > 0) return fromRow;
  return requireReasonForCancellation(context);
}

export { CUSTOMER_A_INDEX, CUSTOMER_B_INDEX, CONTRACT_A_INDEX, CONTRACT_B_INDEX };
export { entityId, errorHaystack, asNumber, postCancellationDraft, requireReasonForCancellation };
export type { Pdt3042TableRow };
