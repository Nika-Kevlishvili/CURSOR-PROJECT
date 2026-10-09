/**
 * PDT-3202 helpers — RFD Load PODs GET SLO on Dev2 (30-minute wall-clock cap).
 *
 * Timed action: GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply
 * (CustomersForDPSRequest). Do not POST an RFD — the bug is Load PODs before save.
 *
 * Scope bound: CUSTOMERS_UNDER_CONDITIONS + `$CUSTOMER_NUMBER$=$n$` OR-chain
 * (never ALL_CUSTOMERS / allSelected true — PDT-3179/PDT-3421 select-all SQL ~9–10 min).
 * Do not send listOfCustomer (Swagger maxLength 1024; GET @Size min 1 — omit the
 * param entirely, do not send "").
 *
 * Do not use `$PRODUCT$=${productId}`: psd_reminder_liability_condition_eval
 * (schema.sql ~11779-11780) resolves prod_id via invoice.invoices → product.product_details.
 * This test’s overdue liabilities are MANUAL billing-group (no invoice) → prod_id null → 0 rows.
 * `$CUSTOMER_NUMBER$` (schema.sql id 4) maps to cust_c.customer_number (no invoice required).
 * SQL-style `IN (...)` is rejected by validateCondition / RuleEvaluatorService
 * (`customer_under_conditions_is_not_valid`). Valid formula is BillingPayloads glue:
 * `$CUSTOMER_NUMBER$=$n$OR$CUSTOMER_NUMBER$=$n2$` (no spaces; each n wrapped as `$n$`).
 *
 * Ticket example Reminder-2373 had 476 customers (~5 min load). Default N=80 is a
 * 30-minute wall-clock cap (25-customer Dev2 run ~5 min ≈ 12s each → 80 × 12s ≈ 16 min
 * setup + reminder + GET), not a 476-customer clone. Env PDT3202_CUSTOMER_COUNT still
 * overrides; N>80 may exceed 30 min. First customer: createSupplyChain + overdue
 * billing-group liability. Remaining N-1: createAdditionalCustomerOnSharedProduct
 * on product index 0 (no new term/electricity/product). Sequential POSTs only
 * (do not parallelize — Responses races). Extra-customer POSTs retry HTTP 502/503
 * (or network throw) up to 3 times with a 2s delay — Dev2 nginx Bad Gateway at ~76/80.
 *
 * Reminder POST uses Dev2 PowerSupplyDisconnectionReminderBaseRequest (PDT-2913 shape):
 * conditionType CUSTOMERS_UNDER_CONDITIONS + condition; listOfCustomer omitted/null;
 * EMAIL + name + confirm. Do not send legacy customerList / customerFilterType.
 * Do not POST /job. Poll GET until EXECUTED (2 min).
 *
 * Swagger: parent refreshed all env specs 2026-09-17 13:01 UTC. This agent re-ran
 * update-swagger-specs.ps1 — failed on macOS (curl.exe missing). Contract taken from
 * Cursor-Project/config/swagger/dev2/swagger-spec.json (mtime 2026-09-17 13:01 local).
 * Verified:
 * - POST /power-supply-disconnection-reminder
 *   PowerSupplyDisconnectionReminderBaseRequest required:
 *   communicationChannels, conditionType, customerSendToDateAndTime,
 *   disconnectionDate, liabilitiesMaxDueDate
 *   conditionType: ALL_CUSTOMERS | CUSTOMERS_UNDER_CONDITIONS | LIST_OF_CUSTOMERS
 *   condition: string (no maxLength)
 *   listOfCustomer: maxLength 1024, minLength 1 — omit for UNDER_CONDITIONS
 * - GET load-customer-for-disconnection-power-supply
 *   Query schema CustomersForDPSRequest
 *   required: conditionType, page, size, supplierType
 *   Runtime also requires gridOperatorId + powerSupplyDisconnectionReminderId
 *   (DisconnectionPowerSupplyRequestsService 887-907)
 *   CUSTOMERS_UNDER_CONDITIONS: condition required; omit listOfCustomer
 *   supplierType: CURRENT | PREVIOUS
 *   searchBy: ALL | CUSTOMER_IDENTIFIER | CUSTOMER_NUMBER | CONTRACT_NUMBER |
 *     BILLING_GROUP_NUMBER | POD_IDENTIFIER | LIABILITY_NUMBER | OUTGOING_DOCUMENT_NUMBER
 * - Response PageCustomersForDPSResponse (totalElements, content[])
 * - Swagger documents HTTP 200; Phoenix controller returns 206 PARTIAL_CONTENT
 *   (DisconnectionPowerSupplyRequestsController ~252-253).
 *   CheckResponse uses response.ok() (200–299), so 206 is success. Do not assert status === 200.
 *
 * Reminder condition keys: receivable.psd_reminder_condition_replacements / schema.sql id 4
 * `$CUSTOMER_NUMBER$` → customer_number. Formula OR-equals (IN rejected by validateCondition):
 * `$CUSTOMER_NUMBER$=$n1$OR$CUSTOMER_NUMBER$=$n2$` using numeric customerNumber (not UIC).
 * N=80 is the 30-minute wall-clock cap.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2913-missing-email-object-reminder-for-disconnection.fixtures.ts
 * - tests/cursor/PDT-3421-rfd-highest-consumption-uncheck.spec.ts
 * - tests/cursor/pdt-3421-rfd-highest-consumption-uncheck.fixtures.ts
 *   (createSecondCustomerSupplyAndLiability ~443-511 copy source; lastProductIdx = 0)
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts
 */

import { expect } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  asNumber,
  monthStartYmd,
  PDT_3179_MANUAL_LIABILITY_AMOUNT,
  resolveContractBillingGroupId,
} from './pdt-3179-reconnection-invoice-emails.fixtures';
import {
  createOverdueManualLiabilityWithBillingGroup,
  createSupplyChain,
  customerIdentOf,
  entityId,
  type Pdt3421Fx,
} from './pdt-3421-rfd-highest-consumption-uncheck.fixtures';

export const PDT_3202_KEY = 'PDT-3202';
export const PDT_3202_TITLE = 'RFD - Performance issue loading PODs in TEST/DEV env';

/**
 * 30-minute wall-clock default (not ticket 476). 25-customer Dev2 run ~5 min ≈ 12s each.
 * Env PDT3202_CUSTOMER_COUNT still overrides; N>80 may exceed 30 min.
 */
export const PDT_3202_DEFAULT_CUSTOMER_COUNT = 80;
/** PDT-3202 expected: Prod reminders with >3500 customers load PODs in under 4 seconds. */
export const PDT_3202_SLO_MS = 4000;
/** Record a slow Load PODs (ticket actual ~5 min) without blowing the 30 min wall clock. */
export const PDT_3202_LOAD_GET_TIMEOUT_MS = 5 * 60 * 1000;
/** Poll GET reminder until EXECUTED (no /job). */
export const PDT_3202_REMINDER_POLL_MS = 2 * 60 * 1000;
/** Reminder POST + poll budget (~2–3 min). */
export const PDT_3202_REMINDER_BUDGET_MS = 3 * 60 * 1000;
/** 25 customers took ~5 min (~12s each). Sequential POSTs only. */
export const PDT_3202_PER_CUSTOMER_MS = 12 * 1000;
/** Hard cap: whole Dev2 Playwright run must finish in ≤ 30 minutes. */
export const PDT_3202_MIN_TEST_TIMEOUT_MS = 30 * 60 * 1000;
/** Ticket comment used size=25. */
export const PDT_3202_PAGE_SIZE = 25;
/** POST retries on nginx 502/503 (or network throw). Total attempts, not extra retries. */
export const PDT_3202_GATEWAY_RETRY_ATTEMPTS = 3;
export const PDT_3202_GATEWAY_RETRY_DELAY_MS = 2_000;

export const PDT_3202_LOAD_CUSTOMERS_PATH =
  'disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply';

export type Pdt3202Fx = Pdt3421Fx;

export type Pdt3202LoadQuery = {
  page: number;
  size: number;
  conditionType: 'CUSTOMERS_UNDER_CONDITIONS';
  condition: string;
  powerSupplyDisconnectionReminderId: number;
  gridOperatorId: number;
  supplierType: 'CURRENT';
  searchBy: 'ALL';
};

export type PageCustomersForDps = {
  totalElements?: number;
  numberOfElements?: number;
  size?: number;
  content?: unknown;
};

export function pdt3202RelevantKeys(): Array<
  'customer' | 'reminderForDisconnection' | 'product'
> {
  return ['customer', 'reminderForDisconnection', 'product'];
}

/** Always 30 minutes — never exceeds the wall-clock cap (even if PDT3202_CUSTOMER_COUNT > 80). */
export function pdt3202TestTimeoutMs(_customerCount: number): number {
  return PDT_3202_MIN_TEST_TIMEOUT_MS;
}

/** Env PDT3202_CUSTOMER_COUNT. Default 80. Invalid or less than 2 → 80. N>80 may exceed 30 min. */
export function parsePdt3202CustomerCount(raw = process.env.PDT3202_CUSTOMER_COUNT): number {
  if (raw === undefined || raw === '') {
    return PDT_3202_DEFAULT_CUSTOMER_COUNT;
  }
  const n = Number.parseInt(raw, 10);
  if (!Number.isFinite(n) || n < 2) {
    return PDT_3202_DEFAULT_CUSTOMER_COUNT;
  }
  return n;
}

function parseNumericCustomerNumber(row: unknown): number | undefined {
  if (row === null || typeof row !== 'object') return undefined;
  const rec = row as Record<string, unknown>;
  const raw = rec.customerNumber ?? rec.customer_number;
  const n = typeof raw === 'number' ? raw : typeof raw === 'string' && /^\d+$/.test(raw) ? Number(raw) : Number.NaN;
  if (Number.isFinite(n) && n > 0) return n;
  return undefined;
}

/**
 * Numeric customer.customers.customer_number for `$CUSTOMER_NUMBER$` (not UIC/identifier).
 * GET customer/{id} when the POST body omits a finite number > 0.
 */
export async function collectPdt3202CustomerNumbers(fx: Pdt3202Fx): Promise<number[]> {
  const { Request, Responses, Endpoints } = fx;
  expect(Responses.customer.length, 'created customers').toBeGreaterThan(0);
  const numbers: number[] = [];
  for (let i = 0; i < Responses.customer.length; i += 1) {
    const row = Responses.customer[i];
    let n = parseNumericCustomerNumber(row);
    if (n === undefined) {
      const id = entityId(row);
      const getRes = await Request.get(`${Endpoints.customer}/${id}?version=1`);
      await expect(getRes).CheckResponse();
      n = parseNumericCustomerNumber(await getRes.json());
    }
    expect(
      n !== undefined && Number.isFinite(n) && n > 0,
      `customer[${i}] customerNumber must be a finite number > 0 (not UIC/identifier); got ${String(
        (row as { customerNumber?: unknown; identifier?: unknown } | undefined)?.customerNumber ??
          (row as { identifier?: unknown } | undefined)?.identifier,
      )}`,
    ).toBe(true);
    numbers.push(n as number);
  }
  return numbers;
}

/**
 * BillingPayloads-style OR-equals (no spaces). SQL `IN` is not a valid boolean expression
 * (validateCondition → customer_under_conditions_is_not_valid).
 * Example: `$CUSTOMER_NUMBER$=$6099403527$OR$CUSTOMER_NUMBER$=$6099403528$`
 */
export function pdt3202CustomerNumberInCondition(numbers: number[]): string {
  expect(numbers.length, 'customer numbers for OR-equals condition').toBeGreaterThan(0);
  for (const n of numbers) {
    expect(Number.isFinite(n) && n > 0, `customerNumber ${String(n)} must be a finite number > 0`).toBe(true);
  }
  return numbers.map((n) => `$CUSTOMER_NUMBER$=$${n}$`).join('OR');
}

export function gridOperatorIdFromEnv(): number {
  const id = Number(envVariables.grid_operator);
  expect(id, 'envVariables.grid_operator (never hardcode 1026)').toBeGreaterThan(0);
  return id;
}

export function buildPdt3202LoadQuery(opts: {
  condition: string;
  reminderId: number;
}): Pdt3202LoadQuery {
  expect(opts.condition.length, 'condition required for CUSTOMERS_UNDER_CONDITIONS').toBeGreaterThan(0);
  expect(opts.reminderId, 'powerSupplyDisconnectionReminderId').toBeGreaterThan(0);
  return {
    page: 0,
    size: PDT_3202_PAGE_SIZE,
    conditionType: 'CUSTOMERS_UNDER_CONDITIONS',
    condition: opts.condition,
    powerSupplyDisconnectionReminderId: opts.reminderId,
    gridOperatorId: gridOperatorIdFromEnv(),
    supplierType: 'CURRENT',
    searchBy: 'ALL',
  };
}

export function pageContent(body: PageCustomersForDps): unknown[] {
  return Array.isArray(body.content) ? body.content : [];
}

export function pageTotalElements(body: PageCustomersForDps, contentLength: number): number {
  const total = Number(body.totalElements);
  if (Number.isFinite(total)) return total;
  const numberOfElements = Number(body.numberOfElements);
  if (Number.isFinite(numberOfElements)) return numberOfElements;
  return contentLength;
}

export async function timeLoadCustomersForDps(
  fx: Pdt3202Fx,
  query: Pdt3202LoadQuery,
): Promise<{ response: Awaited<ReturnType<Pdt3202Fx['Request']['get']>>; elapsedMs: number }> {
  const started = Date.now();
  const response = await fx.Request.get(PDT_3202_LOAD_CUSTOMERS_PATH, {
    params: query as unknown as Record<string, string | number>,
    timeout: PDT_3202_LOAD_GET_TIMEOUT_MS,
  });
  const elapsedMs = Date.now() - started;
  console.log(
    `[PDT-3202] ${elapsedMs}ms GET ${PDT_3202_LOAD_CUSTOMERS_PATH} status=${response.status()}`,
  );
  return { response, elapsedMs };
}

function logElapsed(started: number, message: string): void {
  console.log(`[PDT-3202] ${Date.now() - started}ms ${message}`);
}

function lastEntry<T>(arr: T[], label: string): T {
  expect(arr.length, `${label} must exist`).toBeGreaterThan(0);
  return arr[arr.length - 1];
}

function isGatewayStatus(status: number): boolean {
  return status === 502 || status === 503;
}

/**
 * POST the same payload up to 3 times. Retry only HTTP 502/503 or a network throw.
 * CheckResponse runs after the last attempt (or earlier on a non-gateway status).
 * Caller must not push to Responses until this resolves.
 */
export async function postWithGatewayRetry(
  Request: Pdt3202Fx['Request'],
  path: string,
  data: unknown,
): Promise<Awaited<ReturnType<Pdt3202Fx['Request']['post']>>> {
  let lastNetworkError: unknown;
  for (let attempt = 1; attempt <= PDT_3202_GATEWAY_RETRY_ATTEMPTS; attempt += 1) {
    let res: Awaited<ReturnType<Pdt3202Fx['Request']['post']>>;
    try {
      res = await Request.post(path, { data });
    } catch (err) {
      lastNetworkError = err;
      if (attempt < PDT_3202_GATEWAY_RETRY_ATTEMPTS) {
        console.log(`[PDT-3202] retry POST ${path} status=network attempt=${attempt}`);
        await new Promise((r) => setTimeout(r, PDT_3202_GATEWAY_RETRY_DELAY_MS));
        continue;
      }
      throw err;
    }
    const status = res.status();
    if (isGatewayStatus(status) && attempt < PDT_3202_GATEWAY_RETRY_ATTEMPTS) {
      console.log(`[PDT-3202] retry POST ${path} status=${status} attempt=${attempt}`);
      await new Promise((r) => setTimeout(r, PDT_3202_GATEWAY_RETRY_DELAY_MS));
      continue;
    }
    await expect(res).CheckResponse();
    return res;
  }
  throw lastNetworkError instanceof Error
    ? lastNetworkError
    : new Error(`[PDT-3202] POST ${path} exhausted ${PDT_3202_GATEWAY_RETRY_ATTEMPTS} attempts`);
}

/**
 * Extra customer on the shared product (index 0). Does not create term / electricity / product.
 * Copied from PDT-3421 createSecondCustomerSupplyAndLiability ~443-511 with lastProductIdx = 0.
 * POSTs go through postWithGatewayRetry (502/503); Responses push only after CheckResponse.
 */
export async function createAdditionalCustomerOnSharedProduct(fx: Pdt3202Fx): Promise<{
  customerIndex: number;
  podIndex: number;
  contractIndex: number;
  liabilityIndex: number;
  customerId: number;
  podId: number;
  identifier: string;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  expect(Responses.product.length, 'shared product (index 0) must exist').toBeGreaterThan(0);

  const customerPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  const customer = await postWithGatewayRetry(Request, Endpoints.customer, customerPayload);
  Responses.customer.push(await customer.json());

  const pod = await postWithGatewayRetry(
    Request,
    Endpoints.pod,
    GeneratePayload.pointsOfDelivery.pod_settlement(),
  );
  const podBody = await pod.json();
  Responses.pod.push(podBody);

  const lastCustomerIdx = Responses.customer.length - 1;
  const lastProductIdx = 0;
  const lastPodIdx = Responses.pod.length - 1;
  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(
    lastCustomerIdx,
    lastProductIdx,
    lastPodIdx,
  );
  (contractPayload.basicParameters as Record<string, unknown>).customerId = entityId(
    lastEntry(Responses.customer, 'additional customer'),
  );
  (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
  const monthly =
    asNumber((podBody as { estimatedMonthlyAvgConsumption?: unknown }).estimatedMonthlyAvgConsumption) ||
    1;
  contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = (monthly * 12) / 1000;
  const contract = await postWithGatewayRetry(Request, Endpoints.productContract, contractPayload);
  Responses.productContract.push(await contract.json());

  const lastContractIdx = Responses.productContract.length - 1;
  await postWithGatewayRetry(
    Request,
    '/contract-pods/manual',
    await GeneratePayload.pointsOfDelivery.pod_activation(
      lastPodIdx,
      monthStartYmd(),
      undefined,
      lastContractIdx,
    ),
  );

  const contractId = entityId(lastEntry(Responses.productContract, 'additional contract'));
  const contractGet = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const billingGroupId = resolveContractBillingGroupId(contractBody);
  const yesterday = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  const liabPayload = GeneratePayload.receivablesManagement.customer_liability();
  liabPayload.customerId = entityId(lastEntry(Responses.customer, 'additional customer'));
  liabPayload.billingGroupId = billingGroupId;
  liabPayload.initialAmount = PDT_3179_MANUAL_LIABILITY_AMOUNT;
  liabPayload.dueDate = yesterday;
  liabPayload.occurrenceDate = yesterday;
  const liabRes = await postWithGatewayRetry(Request, Endpoints.customerLiability, liabPayload);
  Responses.customerLiability.push(entityId(await liabRes.json()));

  const createdCustomer = lastEntry(Responses.customer, 'additional customer');
  return {
    customerIndex: lastCustomerIdx,
    podIndex: lastPodIdx,
    contractIndex: lastContractIdx,
    liabilityIndex: Responses.customerLiability.length - 1,
    customerId: entityId(createdCustomer),
    podId: entityId(podBody),
    identifier: customerIdentOf(createdCustomer),
  };
}

/**
 * POST reminder with Dev2 CUSTOMERS_UNDER_CONDITIONS shape (PDT-2913 EMAIL + name + confirm),
 * then set-customer-send-time and poll EXECUTED. Does not call /job. listOfCustomer omitted.
 */
export async function createPdt3202ExecutedReminderUnderConditions(
  fx: Pdt3202Fx,
  condition: string,
): Promise<{ reminderId: number; payload: Record<string, unknown> }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const started = Date.now();
  expect(condition.length, 'reminder condition required for CUSTOMERS_UNDER_CONDITIONS').toBeGreaterThan(
    0,
  );

  const base = GeneratePayload.receivablesManagement.reminderForDisconnection() as Record<
    string,
    unknown
  >;
  const payload: Record<string, unknown> = {
    customerSendToDateAndTime: base.customerSendToDateAndTime,
    liabilityAmountFrom: base.liabilityAmountFrom ?? null,
    liabilityAmountTo: base.liabilityAmountTo ?? null,
    liabilitiesMaxDueDate: base.liabilitiesMaxDueDate,
    conditionType: 'CUSTOMERS_UNDER_CONDITIONS',
    condition,
    communicationChannels: ['EMAIL'],
    emailTemplateId: envVariables.reminder_disconnection_email_template,
    smsTemplateId: null,
    documentTemplateId: null,
    disconnectionDate: base.disconnectionDate,
    printedFileName: base.printedFileName ?? '1',
    name:
      typeof base.name === 'string' && base.name.length > 0
        ? base.name
        : `PDT3202-${randomGens.generateRandomString(true, true, 10)}`,
    confirm: true,
  };

  const res = await Request.post(Endpoints.reminderForDisconnection, { data: payload });
  await expect(res).CheckResponse();
  const reminderId = entityId(await res.json());
  Responses.reminderForDisconnection.push(reminderId);
  logElapsed(
    started,
    `POST reminder ${reminderId} conditionType=CUSTOMERS_UNDER_CONDITIONS condition=${condition}`,
  );

  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const timeOffset = await Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${reminderId}&hour=${georgianTime.getHours()}&minute=${georgianTime.getMinutes()}`,
    { timeout: 5_000 },
  );
  await expect(timeOffset).CheckResponse();

  const deadline = Date.now() + PDT_3202_REMINDER_POLL_MS;
  while (Date.now() < deadline) {
    try {
      const reminderGet = await Request.get(`power-supply-disconnection-reminder/${reminderId}`, {
        timeout: 3_000,
      });
      if (reminderGet.ok()) {
        const status = ((await reminderGet.json()) as { reminderStatus?: string }).reminderStatus;
        if (status === 'EXECUTED') {
          logElapsed(started, `reminder ${reminderId} already EXECUTED`);
          return { reminderId, payload };
        }
      }
    } catch {
      /* poll */
    }
    await new Promise((r) => setTimeout(r, 5_000));
  }
  logElapsed(started, `reminder ${reminderId} still not EXECUTED (no /job)`);
  return { reminderId, payload };
}

export { createOverdueManualLiabilityWithBillingGroup, createSupplyChain, entityId };
