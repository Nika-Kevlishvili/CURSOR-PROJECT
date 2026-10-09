/**
 * PDT-3042 helpers — Cancellation of disconnection CREATE table auto-check lock.
 *
 * Bug-only (no TC .md). Creates all data from scratch. No test.beforeAll.
 *
 * Product rule (Jira example + Confluence AC, principal remaining only — PDT-3134):
 * remaining = SUM(current_amount of reminder liabilities for that POD)
 * Compare to RFD liabilityAmountFrom:
 *   remaining == 0 → checked + unableToCheck, default reason Customer Paid
 *   remaining > 0 AND from is set AND remaining < from → same auto-check lock
 *   remaining >= from OR from is null → NOT auto-checked
 *
 * Ticket example (primary): two **manual invoices** (summary 50 and 100) so
 * reminder_pods matches getRemindersForDPSRequestList invoice UNION
 * (invoice_standard_detailed_data → pod_details). MANUAL liabilities + /job can
 * mark EXECUTED without reminder_pods → RFD 400 "Reminder … not found".
 * After RFD: pay first liability fully + second down so remaining ≈ 15 < from 20.
 * Do not call calculate-tax / FEE_CHARGED.
 *
 * Reminder create: GeneratePayload.receivablesManagement.reminderForDisconnection()
 * (sets customerList) then overlay Dev2 PowerSupplyDisconnectionReminderBaseRequest:
 * required communicationChannels, conditionType, customerSendToDateAndTime,
 * disconnectionDate, liabilitiesMaxDueDate; listOfCustomer minLength 1; confirm true
 * (job findByStatusAndSendTimePessimisticLock CONFIRMED). customerList = listOfCustomer
 * (export persists listOfCustomer into legacy customer_list). Do not use
 * createPdt2913ReminderForDisconnection (omits customerList).
 *
 * After EXECUTED: GET second-tab (PDT-2971 fetchReminderSecondTab). Fail immediately
 * if content is empty or the two invoice liability ids/numbers are absent — that is
 * documentGenerationJob skip ("No customers found" still sets EXECUTED). Do NOT poll
 * GET reminders-list-for-disconnection-request. RFD POST (13 min) is the eligibility
 * check (checkReminderForDisconnectionId → getRemindersForDPSRequestList).
 *
 * Swagger (dev2, refreshed this session from http://10.236.20.11:8092/v3/api-docs):
 * PowerSupplyDcnCancellationTableResponse: checked, unableToCheck
 *   (Java field isChecked maps to JSON checked; unableToUncheck maps to unableToCheck)
 * GET .../table-content?page&size&requestForDisconnectionId
 * GET .../get-checked-pods/{requestForDisconnectionId}/{cancellationId} (create uses 0)
 * POST .../ CancellationOfThePowerSupplyRequest
 *   required: requestForDisconnectionOfThePowerSupplyId, saveAs (DRAFT|EXECUTED)
 *   table[] CancellationPodRequest: customerId, podId,
 *     requestForDisconnectionOfPowerSupplyId, cancellationReasonId
 * PowerSupplyDisconnectionReminderSecondTabRequest: page, size, powerSupplyDisconnectionReminderId
 *
 * Runtime SQL (CancellationOfDisconnectionOfThePowerSupplyRepository.findForCheck):
 *   remaining == 0 OR (from IS NOT NULL AND remaining < from) → isChecked + unableToUncheck
 *
 * Reference:
 * - tests/receivableManagement/requestForDisconnection.spec.ts (REG-1045)
 * - tests/receivableManagement/cancelationOfRequestForDisconnection.spec.ts (REG-1161)
 * - tests/cursor/PDT-2971-rfd-shared-pod-multi-customer-merge.spec.ts
 * - tests/cursor/pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts
 *   (reminder payload + job + fetchReminderSecondTab + shiftCustomerLiabilitiesToYesterday)
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts (RFD POST 13 min timeout)
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 * - tests/receivableManagement/Payment/offlinePaymentCreateAndOffsetting.spec.ts (REG-1005)
 */

import { expect } from './cursor-test.fixtures';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  resolvePdt2913BillingGroupId,
  setupPdt2913ContractChain,
} from './pdt-2913-missing-email-object-reminder-for-disconnection.fixtures';
import {
  asNumber,
  customerIdentifier,
  entityId,
  errorHaystack,
  getLiability,
  nestedId,
  readHttpBody,
  todayYmd,
} from './pdt-3179-reconnection-invoice-emails.fixtures';

export const PDT_3042_KEY = 'PDT-3042';
/** Exact Jira summary — keep ticket spelling Cancelation / payed. */
export const PDT_3042_TITLE =
  'POD to be included and automatically checked in Cancelation of Power supply disconnection request when liabilities are payed until the amount in Power supply disconnection request';

/** Two invoices + up to 12 min job poll + 13 min job HTTP + 13 min RFD + payments. */
export const PDT_3042_TEST_TIMEOUT_MS = 40 * 60 * 1000;
/** POST RFD and POST reminder /job — execute() cartesian can take 8–10 min. */
export const PDT_3042_RFD_HTTP_TIMEOUT_MS = 13 * 60 * 1000;
export const PDT_3042_REMINDER_JOB_HTTP_TIMEOUT_MS = 13 * 60 * 1000;
/** Poll GET reminder/{id} until EXECUTED after /job (not the eligible-list GET). */
export const PDT_3042_REMINDER_JOB_TIMEOUT_MS = 12 * 60 * 1000;

export const PDT_3042_LIABILITY_50 = 50;
export const PDT_3042_LIABILITY_100 = 100;
export const PDT_3042_RFD_FROM = 20;
export const PDT_3042_PAY_COVER_FIRST = 50;
export const PDT_3042_PAY_PARTIAL_SECOND = 85;
export const PDT_3042_REMAINING_AFTER_PAYMENTS = 15;

export type Pdt3042Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type Pdt3042Chain = {
  customerId: number;
  customerIdentifier: string;
  podId: number;
  podIdentifier: string;
  reminderId: number;
  rfdId: number;
  liabilityIds: [number, number];
  invoiceIds: [number, number];
  billingGroupId: number;
};

export type Pdt3042TableRow = Record<string, unknown>;

type Pdt3042SecondTabRow = {
  liabilities?: string;
  customer?: string;
  sumOfLiabilities?: string;
  id?: number;
};

const PDT_3042_JOB_SKIP_PATH =
  'Phoenix PowerSupplyDisconnectionReminderService.documentGenerationJob: if execute() returns no customers, it logs "No customers found" and STILL sets reminderStatus=EXECUTED. Empty second-tab means reminder_customers was never written, so GET reminders-list-for-disconnection-request will never list this reminder. RFD POST checkReminderForDisconnectionId is the eligibility check — do not poll the GET list.';

export function pdt3042RelevantKeys(opts?: {
  includePayment?: boolean;
  includeCancellation?: boolean;
}): string[] {
  const keys = [
    'customer',
    'pod',
    'productContract',
    'customerLiability',
    'invoice',
    'reminderForDisconnection',
    'requestForDisconnection',
  ];
  if (opts?.includePayment) keys.push('payment');
  if (opts?.includeCancellation) keys.push('cancellationOfRequestOfDisconnection');
  return keys;
}

export function requireReasonForCancellation(context: string): number {
  const id = asNumber(envVariables.reason_for_cancellation);
  expect(
    id,
    `${context}: envVariables.reason_for_cancellation is required for CancellationPodRequest.cancellationReasonId`,
  ).toBeGreaterThan(0);
  return id;
}

function readLiabilityBillingGroupId(body: Record<string, unknown>): number {
  const billingGroupResponse = body.billingGroupResponse as { id?: unknown } | null | undefined;
  return (
    asNumber(billingGroupResponse?.id) ||
    asNumber(body.contractBillingGroupId) ||
    asNumber(body.billingGroupId)
  );
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
  console.log(`[PDT-3042] ${Date.now() - started}ms ${message}`);
}

function reminderIdOf(raw: unknown): number {
  return entityId(raw);
}

function listingRows(body: unknown): Pdt3042TableRow[] {
  if (Array.isArray(body)) return body as Pdt3042TableRow[];
  if (body && typeof body === 'object' && Array.isArray((body as { content?: unknown }).content)) {
    return (body as { content: Pdt3042TableRow[] }).content;
  }
  return [];
}

/** Swagger PowerSupplyDcnCancellationTableResponse.checked (not Java isChecked). */
export function swaggerChecked(row: Pdt3042TableRow | undefined): boolean {
  expect(row, 'cancellation table row').toBeTruthy();
  expect(
    typeof row!.checked,
    'Swagger PowerSupplyDcnCancellationTableResponse.checked must be boolean',
  ).toBe('boolean');
  return row!.checked as boolean;
}

/** Swagger PowerSupplyDcnCancellationTableResponse.unableToCheck (not unableToUncheck). */
export function swaggerUnableToCheck(row: Pdt3042TableRow | undefined): boolean {
  expect(row, 'cancellation table row').toBeTruthy();
  expect(
    typeof row!.unableToCheck,
    'Swagger PowerSupplyDcnCancellationTableResponse.unableToCheck must be boolean',
  ).toBe('boolean');
  return row!.unableToCheck as boolean;
}

export function findPodRow(
  rows: Pdt3042TableRow[],
  podId: number,
): Pdt3042TableRow | undefined {
  return rows.find((row) => Number(row.podId) === podId);
}

/**
 * setLiabilityInfo parses last hyphen segment via findAllById (customer_liability.id).
 * PDT-3179 tokens use liability.number because SQL CONCAT is amount-currency-number;
 * when we build pods[] ourselves, last segment must be the DB id or findAllById is empty.
 */
function liabilityToken(liab: Record<string, unknown>, liabilityId: number): string {
  const amount = asNumber(liab.currentAmount) || asNumber(liab.initialAmount);
  const currencyName = String(
    (liab.currencyResponse as { name?: unknown } | undefined)?.name ?? 'BGN',
  ).replace(/-/g, ' ');
  return `${amount}-${currencyName}-${liabilityId}`;
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

async function liabilityIdFromInvoice(fx: Pdt3042Fx, invoiceId: number): Promise<number> {
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

/** REG-1161 / PDT-2880: manual invoice billing run + waitForInvoiceGeneration. */
export async function createPdt3042ManualInvoice(
  fx: Pdt3042Fx,
  summaryValue: number,
  billingIndex: number,
): Promise<{ invoiceId: number; liabilityId: number }> {
  const payload = (await fx.GeneratePayload.billing.manualInvoice()) as {
    manualInvoiceParameters: {
      manualInvoiceSummaryDataParameters: { summaryDataRowList: Array<{ value: number }> };
    };
  };
  payload.manualInvoiceParameters.manualInvoiceSummaryDataParameters.summaryDataRowList[0].value =
    summaryValue;

  const create = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(create).CheckResponse();
  fx.Responses.billingRun.push(await create.json());
  await fx.GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, billingIndex);

  const invoiceId = entityId(fx.Responses.invoice[fx.Responses.invoice.length - 1]);
  expect(invoiceId, `invoice id after billingIndex=${billingIndex}`).toBeGreaterThan(0);
  const liabilityId = await liabilityIdFromInvoice(fx, invoiceId);
  const liability = await getLiability(fx, liabilityId);
  const billingGroupId = readLiabilityBillingGroupId(liability);
  expect(
    billingGroupId,
    `PDT-3042: after invoice ${invoiceId}, GET customer-liability/${liabilityId} must have ` +
      `contractBillingGroupId / billingGroupResponse.id > 0 ` +
      `(PowerSupplyDisconnectionReminderRepository.execute() requires contract_billing_group_id IS NOT NULL unless rescheduling). ` +
      `billingGroupResponse=${JSON.stringify(liability.billingGroupResponse)} ` +
      `contractBillingGroupId=${String(liability.contractBillingGroupId)}`,
  ).toBeGreaterThan(0);
  fx.Responses.customerLiability.push(liabilityId);
  return { invoiceId, liabilityId };
}

/**
 * PDT-2971 shiftCustomerLiabilitiesToYesterday: overdue every OPEN liability for this
 * customer identifier so invoice VAT / extra lines cannot leave principals non-overdue.
 * Keep the two invoice liability ids separately for payment targeting.
 */
export async function shiftPdt3042CustomerLiabilitiesToYesterday(fx: Pdt3042Fx): Promise<number[]> {
  const identifier = customerIdentifier(fx.Responses);
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

/**
 * Dev2 reminder: GeneratePayload sets customerList; overlay LIST_OF_CUSTOMERS +
 * listOfCustomer + confirm true + PDT3042 name. Persist the same identifiers on
 * customerList (legacy customer_list column used by execute()).
 */
export async function createPdt3042ReminderForDisconnection(fx: Pdt3042Fx): Promise<{
  reminderId: number;
  payload: Record<string, unknown>;
}> {
  const base = fx.GeneratePayload.receivablesManagement.reminderForDisconnection() as Record<
    string,
    unknown
  >;
  const listOfCustomer = fx.Responses.customer
    .map((customer) => String(customer.identifier ?? ''))
    .filter((id) => id.length > 0)
    .join(',');
  expect(
    listOfCustomer.length,
    'Swagger PowerSupplyDisconnectionReminderBaseRequest.listOfCustomer minLength 1',
  ).toBeGreaterThanOrEqual(1);

  const payload: Record<string, unknown> = {
    ...base,
    customerList: listOfCustomer,
    listOfCustomer,
    conditionType: 'LIST_OF_CUSTOMERS',
    communicationChannels: ['EMAIL'],
    emailTemplateId: base.emailTemplateId ?? envVariables.reminder_disconnection_email_template,
    smsTemplateId: null,
    name: `PDT3042-${randomGens.generateRandomString(true, true, 8)}`,
    confirm: true,
  };

  const response = await fx.Request.post(fx.Endpoints.reminderForDisconnection, { data: payload });
  await expect(response).CheckResponse();
  const body = await response.json();
  fx.Responses.reminderForDisconnection.push(body);
  return { reminderId: reminderIdOf(body), payload };
}

export async function fetchPdt3042ReminderSecondTab(
  fx: Pdt3042Fx,
  reminderId: number,
): Promise<Pdt3042SecondTabRow[]> {
  const res = await fx.Request.get(
    `${fx.Endpoints.reminderForDisconnection}/second-tab?page=0&size=100&powerSupplyDisconnectionReminderId=${reminderId}`,
  );
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Pdt3042SecondTabRow[] };
  return body.content ?? [];
}

/** Fail immediately after EXECUTED if job skip path left reminder_customers empty. */
export async function assertPdt3042ReminderSecondTabHasInvoiceLiabilities(
  fx: Pdt3042Fx,
  reminderId: number,
  liabilityIds: number[],
): Promise<void> {
  const rows = await fetchPdt3042ReminderSecondTab(fx, reminderId);
  expect(
    rows.length,
    `PDT-3042: reminder ${reminderId} GET second-tab content is empty after EXECUTED. ${PDT_3042_JOB_SKIP_PATH}`,
  ).toBeGreaterThan(0);

  // Swagger PowerSupplyDisconnectionReminderSecondTabResponse.liabilities =
  // string_agg(customer_liabilities.liability_number). Response.id is the reminder id, not a liability id.
  const liabilityNumbersCsv = rows
    .map((row) => String(row.liabilities ?? ''))
    .join(',')
    .toLowerCase();
  const tokens = liabilityNumbersCsv
    .split(',')
    .map((token) => token.trim())
    .filter((token) => token.length > 0);
  const missing: string[] = [];
  for (const id of liabilityIds) {
    const liab = await getLiability(fx, id);
    const number = String(liab.number ?? '').trim();
    const numberHit =
      number.length > 0 && tokens.some((token) => token === number.toLowerCase());
    const looseHit = number.length > 0 && liabilityNumbersCsv.includes(number.toLowerCase());
    if (!numberHit && !looseHit) {
      missing.push(`id=${id} number=${number || '(none)'}`);
    }
  }
  expect(
    missing,
    `PDT-3042: reminder ${reminderId} second-tab liabilities string_agg is missing invoice liability numbers [${missing.join('; ')}]. ` +
      `content=${JSON.stringify(rows).slice(0, 4000)}. ${PDT_3042_JOB_SKIP_PATH}`,
  ).toEqual([]);
}

/**
 * Overdue all open customer liabilities, set send time, POST reminder /job (13 min HTTP)
 * until EXECUTED, then fail-fast on second-tab (no eligible-list poll).
 */
export async function executePdt3042ReminderJob(
  fx: Pdt3042Fx,
  liabilityIds: number[],
): Promise<void> {
  expect(liabilityIds.length, 'need both ticket invoice liabilities for second-tab / payments').toBe(2);
  await shiftPdt3042CustomerLiabilitiesToYesterday(fx);

  const started = Date.now();
  const rfdReminderId = reminderIdOf(fx.Responses.reminderForDisconnection[0]);
  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const timeOffset = await fx.Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${rfdReminderId}&hour=${georgianTime.getHours()}&minute=${georgianTime.getMinutes()}`,
  );
  await expect(timeOffset).CheckResponse();

  const jobPost = await fx.Request.post('power-supply-disconnection-reminder/job', {
    timeout: PDT_3042_REMINDER_JOB_HTTP_TIMEOUT_MS,
  });
  await expect(jobPost).CheckResponse();

  const interval = 5 * 1000;
  const maxAttempts = Math.floor(PDT_3042_REMINDER_JOB_TIMEOUT_MS / interval);
  let executed = false;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const reminderGet = await fx.Request.get(
      `power-supply-disconnection-reminder/${rfdReminderId}`,
    );
    await expect(reminderGet).CheckResponse();
    const reminderJson = (await reminderGet.json()) as { reminderStatus?: string };
    if (reminderJson.reminderStatus === 'EXECUTED') {
      logElapsed(started, `reminder ${rfdReminderId} EXECUTED at attempt ${attempt}`);
      executed = true;
      break;
    }
    if (attempt < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, interval));
    }
  }
  if (!executed) {
    throw new Error(
      `PDT-3042: reminder ${rfdReminderId} did not reach EXECUTED within ${PDT_3042_REMINDER_JOB_TIMEOUT_MS}ms`,
    );
  }
  await assertPdt3042ReminderSecondTabHasInvoiceLiabilities(fx, rfdReminderId, liabilityIds);
}

async function buildRfdPodRow(fx: Pdt3042Fx, liabilityIds: number[]): Promise<Record<string, unknown>> {
  const customer = fx.Responses.customer[0] as Record<string, unknown>;
  const pod = fx.Responses.pod[0] as Record<string, unknown>;
  const podId = entityId(pod);
  const customerId = entityId(customer);
  const identifier = customerIdentifier(fx.Responses);
  const contractId = entityId(fx.Responses.productContract[0]);

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
  expect(podDetailId, 'podDetailId required on RFD pods[]').toBeGreaterThan(0);

  const tokens: string[] = [];
  let liabilitySum = 0;
  for (const id of liabilityIds) {
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

export async function createPdt3042ExecutedRfd(
  fx: Pdt3042Fx,
  opts: { liabilityIds: number[]; liabilityAmountFrom: number },
): Promise<number> {
  const started = Date.now();
  const identifier = customerIdentifier(fx.Responses);
  expect(identifier.length, 'RFD listOfCustomer').toBeGreaterThan(0);
  const reminderId = reminderIdOf(fx.Responses.reminderForDisconnection[0]);
  const podRow = await buildRfdPodRow(fx, opts.liabilityIds);

  const payload = fx.GeneratePayload.receivablesManagement.requestForDisconnection() as Record<
    string,
    unknown
  >;
  payload.reminderForDisconnectionId = reminderId;
  payload.gridOpRequestRegDate = todayYmd();
  payload.disconnectionRequestsStatus = 'EXECUTED';
  payload.supplierType = 'CURRENT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = identifier;
  payload.allSelected = false;
  payload.podWithHighestConsumption = false;
  payload.excludePodIds = [];
  payload.pods = [podRow];
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
  payload.name = `PDT3042-${randomGens.generateRandomString(true, true, 8)}`;

  const res = await fx.Request.post(fx.Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3042_RFD_HTTP_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  fx.Responses.requestForDisconnection.push(requestId);
  logElapsed(started, `POST EXECUTED RFD ${requestId} from=${opts.liabilityAmountFrom}`);
  return requestId;
}

export async function setupPdt3042TicketChain(
  fx: Pdt3042Fx,
  opts?: { liabilityAmountFrom?: number },
): Promise<Pdt3042Chain> {
  const from = opts?.liabilityAmountFrom ?? PDT_3042_RFD_FROM;
  await setupPdt2913ContractChain(fx);
  const billingGroupId = await resolvePdt2913BillingGroupId(fx);

  const invoice50 = await createPdt3042ManualInvoice(fx, PDT_3042_LIABILITY_50, 0);
  const invoice100 = await createPdt3042ManualInvoice(fx, PDT_3042_LIABILITY_100, 1);
  const liabilityIds: [number, number] = [invoice50.liabilityId, invoice100.liabilityId];
  const invoiceIds: [number, number] = [invoice50.invoiceId, invoice100.invoiceId];

  await shiftPdt3042CustomerLiabilitiesToYesterday(fx);

  const reminder = await createPdt3042ReminderForDisconnection(fx);
  await executePdt3042ReminderJob(fx, liabilityIds);

  const rfdId = await createPdt3042ExecutedRfd(fx, {
    liabilityIds,
    liabilityAmountFrom: from,
  });

  const pod = fx.Responses.pod[0] as Record<string, unknown>;
  return {
    customerId: entityId(fx.Responses.customer[0]),
    customerIdentifier: customerIdentifier(fx.Responses),
    podId: entityId(pod),
    podIdentifier: String(pod.identifier ?? ''),
    reminderId: reminder.reminderId,
    rfdId,
    liabilityIds,
    invoiceIds,
    billingGroupId,
  };
}

export async function sumLiabilityCurrentAmounts(
  fx: Pdt3042Fx,
  liabilityIds: number[],
): Promise<number> {
  let sum = 0;
  for (const id of liabilityIds) {
    const liab = await getLiability(fx, id);
    sum += asNumber(liab.currentAmount);
  }
  return sum;
}

async function ensurePaymentInfra(fx: Pdt3042Fx): Promise<void> {
  if (fx.Responses.collectionChannel.length === 0) {
    const channel = await fx.Request.post(fx.Endpoints.collectionChannel, {
      data: fx.GeneratePayload.receivablesManagement.collection_channel(),
    });
    await expect(channel).CheckResponse();
    fx.Responses.collectionChannel.push(entityId(await channel.json()));
  }
  if (fx.Responses.paymentPackage.length === 0) {
    const pkg = await fx.Request.post(fx.Endpoints.paymentPackage, {
      data: fx.GeneratePayload.receivablesManagement.payment_package(),
    });
    await expect(pkg).CheckResponse();
    fx.Responses.paymentPackage.push(entityId(await pkg.json()));
  }
}

/**
 * POST /payment with billing-group auto-offset (REG-1005 / REG-1031).
 * Optional invoiceId targets INVOICE outgoing document. Do not call calculate-tax.
 */
export async function payPdt3042Amount(
  fx: Pdt3042Fx,
  initialAmount: number,
  invoiceId?: number,
): Promise<number> {
  await ensurePaymentInfra(fx);
  const payload = (await fx.GeneratePayload.receivablesManagement.payment(true)) as Record<
    string,
    unknown
  >;
  payload.initialAmount = initialAmount;
  payload.blockedForOffsetting = false;
  if (invoiceId && invoiceId > 0) {
    payload.invoiceId = invoiceId;
    payload.outgoingDocumentType = 'INVOICE';
  }
  const res = await fx.Request.post(fx.Endpoints.payment, { data: payload });
  await expect(res).CheckResponse();
  const paymentId = entityId(await res.json());
  fx.Responses.payment.push(paymentId);
  return paymentId;
}

/**
 * Pay first liability fully, then second down so remaining ≈ 15 (15 < from 20).
 * Invoice VAT may make amounts ≠ 50/100 — compute from live currentAmount.
 */
export async function payTicketExampleAmounts(
  fx: Pdt3042Fx,
  liabilityIds: number[],
): Promise<{ remaining: number; paymentIds: number[] }> {
  expect(liabilityIds.length, 'need two reminder liabilities').toBe(2);
  const snapshots: Array<{ id: number; current: number; invoiceId: number | null }> = [];
  for (const id of liabilityIds) {
    const liab = await getLiability(fx, id);
    snapshots.push({
      id,
      current: asNumber(liab.currentAmount),
      invoiceId: readLiabilityInvoiceId(liab),
    });
  }
  const total = snapshots.reduce((sum, row) => sum + row.current, 0);
  expect(
    total,
    `unpaid principal ${total} must exceed remaining target ${PDT_3042_REMAINING_AFTER_PAYMENTS}`,
  ).toBeGreaterThan(PDT_3042_REMAINING_AFTER_PAYMENTS);
  expect(
    snapshots[1].current,
    `second liability currentAmount must be > ${PDT_3042_REMAINING_AFTER_PAYMENTS} to leave ~15`,
  ).toBeGreaterThan(PDT_3042_REMAINING_AFTER_PAYMENTS);

  const payFirst = snapshots[0].current;
  const paySecond = snapshots[1].current - PDT_3042_REMAINING_AFTER_PAYMENTS;
  const pay50 = await payPdt3042Amount(fx, payFirst, snapshots[0].invoiceId ?? undefined);
  const pay85 = await payPdt3042Amount(fx, paySecond, snapshots[1].invoiceId ?? undefined);
  const remaining = await sumLiabilityCurrentAmounts(fx, liabilityIds);
  expect(
    remaining,
    `after paying first fully (${payFirst}) then ${paySecond} of second, remaining should be ~${PDT_3042_REMAINING_AFTER_PAYMENTS}`,
  ).toBeCloseTo(PDT_3042_REMAINING_AFTER_PAYMENTS, 2);
  return { remaining, paymentIds: [pay50, pay85] };
}

export async function getCancellationTableContent(
  fx: Pdt3042Fx,
  requestForDisconnectionId: number,
): Promise<{ status: number; rows: Pdt3042TableRow[]; body: unknown }> {
  const path = `${fx.Endpoints.cancellationOfRequestOfDisconnection}/table-content`;
  const nested: Record<string, number> = {
    'request.page': 0,
    'request.size': 25,
    'request.requestForDisconnectionId': requestForDisconnectionId,
  };
  const flat = { page: 0, size: 25, requestForDisconnectionId };
  let res = await fx.Request.get(path, { params: nested });
  if (!res.ok()) {
    res = await fx.Request.get(path, { params: flat });
  }
  await expect(res).CheckResponse();
  const body = await res.json();
  return { status: res.status(), rows: listingRows(body), body };
}

/**
 * GET .../get-checked-pods/{rfdId}/{cancellationId}.
 * Create uses cancellationId=0. Dev2 getCheckedPods then returns [] (no saved
 * cancellation POD rows); UI seeds from table-content. Callers must not treat
 * an empty list as a product failure.
 */
export async function getCancellationCheckedPods(
  fx: Pdt3042Fx,
  requestForDisconnectionId: number,
  cancellationId = 0,
): Promise<{ status: number; rows: Pdt3042TableRow[] }> {
  const res = await fx.Request.get(
    `${fx.Endpoints.cancellationOfRequestOfDisconnection}/get-checked-pods/${requestForDisconnectionId}/${cancellationId}`,
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  return { status: res.status(), rows: listingRows(body) };
}

export function buildCancellationDraftPayload(
  fx: Pdt3042Fx,
  chain: Pdt3042Chain,
  table: Array<Record<string, unknown>>,
): Record<string, unknown> {
  const base = fx.GeneratePayload.receivablesManagement.cancellationOfRequestForDisconnection() as Record<
    string,
    unknown
  >;
  return {
    requestForDisconnectionOfThePowerSupplyId: chain.rfdId,
    saveAs: 'DRAFT',
    fileIds: Array.isArray(base.fileIds) ? base.fileIds : [],
    templateIds: Array.isArray(base.templateIds) ? base.templateIds : [],
    table,
  };
}

export function cancellationPodRow(
  chain: Pdt3042Chain,
  cancellationReasonId: number,
): Record<string, unknown> {
  return {
    customerId: chain.customerId,
    podId: chain.podId,
    requestForDisconnectionOfPowerSupplyId: chain.rfdId,
    cancellationReasonId,
  };
}

/**
 * Non-empty CancellationPodRequest that does NOT equal the locked auto-checked POD.
 * Phoenix CancellationOfPowerSupplyRequestValidator rejects table=[] with
 * "table-table must not be empty." before validateTable. Omitting the locked POD
 * from a non-empty table triggers:
 *   "{Customer id : …,POD id : …, Request for disconnection …} can't be unchecked"
 * (CancellationOfDisconnectionOfThePowerSupplyService.validateTable).
 * Equals uses customerId+podId+requestForDisconnectionOfPowerSupplyId only
 * (CancellationPodQueryBaseResponse) — a different podId is enough.
 */
export function cancellationPodRowOmittingLocked(
  chain: Pdt3042Chain,
  cancellationReasonId: number,
): Record<string, unknown> {
  const placeholderPodId = chain.podId === 9_000_000_001 ? 9_000_000_002 : 9_000_000_001;
  return {
    customerId: chain.customerId,
    podId: placeholderPodId,
    requestForDisconnectionOfPowerSupplyId: chain.rfdId,
    cancellationReasonId,
  };
}

export async function postCancellationDraft(
  fx: Pdt3042Fx,
  payload: Record<string, unknown>,
): Promise<{ status: number; id: number | null; text: string; json: Record<string, unknown> | null }> {
  const res = await fx.Request.post(fx.Endpoints.cancellationOfRequestOfDisconnection, {
    data: payload,
  });
  const status = res.status();
  if (status >= 200 && status < 300) {
    await expect(res).CheckResponse();
    const id = entityId(await res.json());
    fx.Responses.cancellationOfRequestOfDisconnection.push(id);
    return { status, id, text: '', json: { id } };
  }
  const parsed = await readHttpBody(res);
  return { status: parsed.status, id: null, text: parsed.text, json: parsed.json };
}

export { entityId, errorHaystack, customerIdentifier, asNumber };
