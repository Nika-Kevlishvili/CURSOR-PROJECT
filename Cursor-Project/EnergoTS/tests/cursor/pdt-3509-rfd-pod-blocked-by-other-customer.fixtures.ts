/**
 * PDT-3509 helpers — executed RFD blocks a POD by pod id even when the checked
 * row belongs to a different customer than the contract owner.
 *
 * Bug-only (no TC .md). Creates all data from scratch. No test.beforeAll.
 * Asserts today's faulty Save And Execute (HTTP 400), not the desired fix.
 *
 * Do not POST power-supply-disconnection-reminder/job (PDT-3179: Dev-wide SQL).
 * Do not call resolveReminderIdForRfd (must not steal another EXECUTED reminder).
 * Do not hardcode Request-1422, Request-2732, reminder 3927, or those customer ids.
 *
 * Swagger (dev, update-swagger-specs.ps1 this session — all envs OK):
 * - POST/PUT /disconnection-of-power-supply-requests DPSRequestsBaseRequest
 *   required: conditionType, gridOpRequestRegDate, gridOperatorId,
 *   reasonOfDisconnectionId, reminderForDisconnectionId, supplierType
 *   disconnectionRequestsStatus enum: DRAFT | EXECUTED | FEE_CHARGED
 *   conditionType enum: ALL_CUSTOMERS | CUSTOMERS_UNDER_CONDITIONS | LIST_OF_CUSTOMERS
 *   supplierType enum: CURRENT | PREVIOUS
 *   pods[] = CustomersForDPSResponse (podId, customerId, isChecked, podIdentifier, …)
 *   allSelected boolean, podWithHighestConsumption boolean, excludePodIds integer[]
 *   listOfCustomer minLength 1 when sent
 * - GET /disconnection-of-power-supply-requests/{id} DPSRequestsResponse.requestNumber
 * - POST /power-supply-disconnection-reminder
 *   PowerSupplyDisconnectionReminderBaseRequest required: communicationChannels,
 *   customerSendToDateAndTime, disconnectionDate, liabilitiesMaxDueDate
 *   customerFilterType enum: NONE | INCLUDED | EXCLUDED
 *   customerList pattern ^[0-9A-Z, /–-]*$
 * - GET .../second-tab PowerSupplyDisconnectionReminderSecondTabRequest
 *   required: page, size, powerSupplyDisconnectionReminderId
 *   searchFields enum includes CUSTOMER_IDENTIFIER
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3465-rfd-highest-uncheck-manual-data.spec.ts
 * - tests/cursor/pdt-3421-rfd-highest-consumption-uncheck.fixtures.ts
 * - tests/cursor/PDT-3211-rfd-supplier-type-previous-additional-logic.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 */

import { expect } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  entityId,
  errorHaystack,
  listingContent,
  readHttpBody,
  todayYmd,
  type Pdt3179Fx,
} from './pdt-3179-reconnection-invoice-emails.fixtures';
import {
  PDT_3421_RFD_HTTP_TIMEOUT_MS,
  buildPodRowForIndex,
  createExecutedReminderForIdentifiers,
  customerIdentOf,
  type Pdt3421PodRow,
} from './pdt-3421-rfd-highest-consumption-uncheck.fixtures';

export const PDT_3509_KEY = 'PDT-3509';
export const PDT_3509_TITLE =
  'DEV Request for disconnection - Error when save as executed';

export const PDT_3509_TEST_TIMEOUT_MS = 40 * 60 * 1000;
export const PDT_3509_RFD_HTTP_TIMEOUT_MS = PDT_3421_RFD_HTTP_TIMEOUT_MS;
export const PDT_3509_REMINDER_POLL_MS = 2 * 60 * 1000;

export type Pdt3509Fx = Pdt3179Fx;

export type Pdt3509HttpResult = {
  status: number;
  text: string;
  json: Record<string, unknown> | null;
  haystack: string;
  errorCode: string;
  exceptionId: string;
  message: string;
};

export function pdt3509RelevantKeys(): Array<
  | 'customer'
  | 'pod'
  | 'product'
  | 'productContract'
  | 'customerLiability'
  | 'reminderForDisconnection'
  | 'requestForDisconnection'
> {
  return [
    'customer',
    'pod',
    'product',
    'productContract',
    'customerLiability',
    'reminderForDisconnection',
    'requestForDisconnection',
  ];
}

export function gridOperatorIdFromEnv(): number {
  const id = Number(envVariables.grid_operator);
  expect(id, 'envVariables.grid_operator').toBeGreaterThan(0);
  return id;
}

function toHttpResult(
  raw: Awaited<ReturnType<typeof readHttpBody>>,
): Pdt3509HttpResult {
  const errorCode = String(raw.json?.errorCode ?? '');
  const exceptionId = String(raw.json?.exceptionId ?? '');
  const message = String(raw.json?.message ?? '');
  return {
    status: raw.status,
    text: raw.text,
    json: raw.json,
    haystack: errorHaystack(raw.status, raw.text, raw.json),
    errorCode,
    exceptionId,
    message,
  };
}

export function isHttpSuccess(status: number): boolean {
  return status >= 200 && status < 300;
}

/**
 * Customer B only. No product contract and no POD — the checked row will point
 * at Customer A's podId with this customerId.
 */
export async function createCustomerWithoutContract(fx: Pdt3509Fx): Promise<{
  customerId: number;
  identifier: string;
}> {
  const payload = fx.GeneratePayload.customers.customer_legal();
  const response = await fx.Request.post(fx.Endpoints.customer, { data: payload });
  await expect(response).CheckResponse();
  const body = await response.json();
  fx.Responses.customer.push(body);
  const customerId = entityId(body);
  const identifier = customerIdentOf(body);
  expect(customerId, 'customer B id').toBeGreaterThan(0);
  expect(identifier.length, 'customer B identifier').toBeGreaterThan(0);
  return { customerId, identifier };
}

/**
 * Poll the reminder created for Customer A. Does not POST /job and does not
 * substitute another EXECUTED reminder.
 */
export async function waitForCreatedReminderExecuted(
  fx: Pdt3509Fx,
  reminderId: number,
): Promise<string> {
  const deadline = Date.now() + PDT_3509_REMINDER_POLL_MS;
  let status = '';
  while (Date.now() < deadline) {
    const reminderGet = await fx.Request.get(
      `${fx.Endpoints.reminderForDisconnection}/${reminderId}`,
      { timeout: 5_000 },
    );
    if (reminderGet.ok()) {
      await expect(reminderGet).CheckResponse();
      status = String(
        ((await reminderGet.json()) as { reminderStatus?: string }).reminderStatus ?? '',
      );
      if (status === 'EXECUTED') return status;
    }
    await new Promise((resolve) => setTimeout(resolve, 2_000));
  }
  return status;
}

export async function createReminderIncludedForCustomer(
  fx: Pdt3509Fx,
  identifier: string,
  templates?: { emailTemplateId: number; smsTemplateId: number },
): Promise<number> {
  expect(identifier.length, 'reminder customerList').toBeGreaterThan(0);
  return createExecutedReminderForIdentifiers(fx, [identifier], templates);
}

export async function reminderSecondTabIncludesCustomer(
  fx: Pdt3509Fx,
  reminderId: number,
  identifier: string,
): Promise<boolean> {
  const response = await fx.Request.get(
    `${fx.Endpoints.reminderForDisconnection}/second-tab`,
    {
      params: {
        page: 0,
        size: 25,
        powerSupplyDisconnectionReminderId: reminderId,
        prompt: identifier,
        searchFields: 'CUSTOMER_IDENTIFIER',
      },
    },
  );
  await expect(response).CheckResponse();
  const needle = identifier.toLowerCase();
  return listingContent(await response.json()).some((row) =>
    String(row.customer ?? '').toLowerCase().includes(needle),
  );
}

/**
 * CustomersForDPSResponse row: Customer A's podId, Customer B's customerId, isChecked true.
 * Liability token last segment is the liability id (setLiabilityInfo → findAllById).
 * Empty/null liability strings NPE inside String.join on the EXECUTED path.
 */
export async function buildCrossCustomerCheckedPod(
  fx: Pdt3509Fx,
  customerBId: number,
  customerBIdentifier: string,
): Promise<Pdt3421PodRow> {
  const row = await buildPodRowForIndex(fx, 0);
  const liabilityId = entityId(fx.Responses.customerLiability[0]);
  const existing = String(row.liabilitiesInBillingGroup ?? '');
  const parts = existing.split('-').filter((part) => part.length > 0);
  const liabilityToken =
    parts.length >= 2
      ? [...parts.slice(0, -1), String(liabilityId)].join('-')
      : `${row.liabilityAmountCustomer ?? 100}-BGN-${liabilityId}`;

  return {
    ...row,
    podId: row.podId,
    customerId: customerBId,
    customers: customerBIdentifier,
    customerNumber: customerBIdentifier,
    isChecked: true,
    isHighestConsumption: false,
    liabilitiesInBillingGroup: liabilityToken,
    liabilitiesInPod: liabilityToken,
  };
}

export function buildRfdPayload(
  fx: Pdt3509Fx,
  opts: {
    reminderId: number;
    listOfCustomer: string;
    disconnectionRequestsStatus: 'DRAFT' | 'EXECUTED';
    pods: Pdt3421PodRow[];
    podWithHighestConsumption: boolean;
  },
): Record<string, unknown> {
  expect(opts.listOfCustomer.length, 'DPSRequestsBaseRequest.listOfCustomer minLength 1').toBeGreaterThan(0);
  expect(opts.reminderId, 'reminderForDisconnectionId').toBeGreaterThan(0);
  const payload = fx.GeneratePayload.receivablesManagement.requestForDisconnection() as Record<
    string,
    unknown
  >;
  payload.supplierType = 'CURRENT';
  payload.disconnectionRequestsStatus = opts.disconnectionRequestsStatus;
  payload.gridOperatorId = gridOperatorIdFromEnv();
  payload.gridOpRequestRegDate = todayYmd();
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.condition = null;
  payload.listOfCustomer = opts.listOfCustomer;
  payload.reminderForDisconnectionId = opts.reminderId;
  payload.allSelected = false;
  payload.podWithHighestConsumption = opts.podWithHighestConsumption;
  payload.excludePodIds = [];
  payload.pods = opts.pods;
  payload.templateIds = [];
  payload.files = [];
  payload.liabilityAmountFrom = null;
  payload.liabilityAmountTo = null;
  return payload;
}

export async function postDraftRfd(
  fx: Pdt3509Fx,
  payload: Record<string, unknown>,
): Promise<number> {
  const response = await fx.Request.post(fx.Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3509_RFD_HTTP_TIMEOUT_MS,
  });
  await expect(response).CheckResponse();
  const requestId = entityId(await response.json());
  fx.Responses.requestForDisconnection.push(requestId);
  expect(requestId, 'POST draft RFD id').toBeGreaterThan(0);
  return requestId;
}

export async function putRfd(
  fx: Pdt3509Fx,
  requestId: number,
  payload: Record<string, unknown>,
): Promise<Pdt3509HttpResult> {
  const response = await fx.Request.put(
    `${fx.Endpoints.requestForDisconnection}/${requestId}`,
    { data: payload, timeout: PDT_3509_RFD_HTTP_TIMEOUT_MS },
  );
  return toHttpResult(await readHttpBody(response));
}

/**
 * Load PODs the way the UI does. listOfCustomer is the identifier under test,
 * not the first customer in Responses.
 * Swagger: GET .../load-customer-for-disconnection-power-supply
 * CustomersForDPSRequest required page, size; conditionType LIST_OF_CUSTOMERS;
 * listOfCustomer minLength 1; supplierType CURRENT | PREVIOUS.
 * Runtime success is HTTP 206.
 */
export async function loadDpsRowsForIdentifier(
  fx: Pdt3509Fx,
  reminderId: number,
  listOfCustomer: string,
): Promise<Record<string, unknown>[]> {
  expect(listOfCustomer.length, 'CustomersForDPSRequest.listOfCustomer').toBeGreaterThan(0);
  const response = await fx.Request.get(
    `${fx.Endpoints.requestForDisconnection}/load-customer-for-disconnection-power-supply`,
    {
      params: {
        page: 0,
        size: 50,
        conditionType: 'LIST_OF_CUSTOMERS',
        listOfCustomer,
        supplierType: 'CURRENT',
        powerSupplyDisconnectionReminderId: reminderId,
        gridOperatorId: gridOperatorIdFromEnv(),
      },
      timeout: PDT_3509_RFD_HTTP_TIMEOUT_MS,
    },
  );
  await expect(response).CheckResponse();
  return listingContent(await response.json()) as Record<string, unknown>[];
}

export function dpsRowCustomerId(row: Record<string, unknown>): number {
  const raw = row.customerId;
  if (typeof raw === 'number') return raw;
  if (raw && typeof raw === 'object' && 'id' in raw) return Number((raw as { id?: unknown }).id ?? 0);
  return Number(raw ?? 0);
}

export function dpsRowPodId(row: Record<string, unknown>): number {
  const raw = row.podId;
  if (typeof raw === 'number') return raw;
  if (raw && typeof raw === 'object' && 'id' in raw) return Number((raw as { id?: unknown }).id ?? 0);
  return Number(raw ?? 0);
}

/** GET customer-liability/list then GET each until invoiceResponse.id matches. */
export async function findLiabilityOnInvoice(
  fx: Pdt3509Fx,
  customerIdentifier: string,
  invoiceId: number,
): Promise<Record<string, unknown> | null> {
  const listed = await fx.Request.get('customer-liability/list', {
    params: {
      page: 0,
      size: 50,
      columns: 'ID',
      direction: 'DESC',
      prompt: customerIdentifier,
      searchFields: 'CUSTOMER',
    },
  });
  await expect(listed).CheckResponse();
  const ids = listingContent(await listed.json())
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0);
  for (const id of ids) {
    const detailRes = await fx.Request.get(`${fx.Endpoints.customerLiability}/${id}`);
    await expect(detailRes).CheckResponse();
    const detail = (await detailRes.json()) as Record<string, unknown>;
    const invoice = detail.invoiceResponse as { id?: unknown } | undefined;
    if (Number(invoice?.id ?? 0) === invoiceId) {
      fx.Responses.customerLiability.push(id);
      return detail;
    }
  }
  return null;
}

export async function getRfdView(
  fx: Pdt3509Fx,
  requestId: number,
): Promise<Record<string, unknown>> {
  const response = await fx.Request.get(
    `${fx.Endpoints.requestForDisconnection}/${requestId}`,
  );
  await expect(response).CheckResponse();
  return (await response.json()) as Record<string, unknown>;
}
