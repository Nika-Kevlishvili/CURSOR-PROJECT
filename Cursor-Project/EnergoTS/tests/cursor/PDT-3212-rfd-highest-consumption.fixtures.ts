/**
 * PDT-3212 — RFD highest consumption ranking + POD tab listing eligibility helpers.
 *
 * Reference specs:
 * - PDT-2529-rfd-data-model-happy-path.fixtures.ts (term 1d, billing anchor, FOR_VOLUMES chain)
 * - PDT-2861-rfd-pod-reconnection-fk.spec.ts (executed RFD Stage B block)
 * - pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts (load/view POD tab)
 */

import { expect } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import type { TestInfo } from '@playwright/test';
import { profile1Month as profile1MonthTemplate } from '../../jsons/payloads/create/energyData/profile1Month';
import { randomGens } from '../../utils/randomGens';
import { envVariables } from '../../fixtures/envCashed';
import { resolveAccountingPeriodIdForBillingDate } from './pdt-2599-service-contract.fixtures';
import {
  applyVolumeBillingAnchor,
  enableSeparateInvoiceForEachPod,
  profileDateRangeFromAnchor,
  reminderIdFromResponses,
  RFD_MIN_INVOICE_PAYMENT_TERM_DAYS,
  termPayloadWithMinimalPaymentDays,
  resolveRfdVolumeBillingAnchor,
  type RfdVolumeBillingAnchor,
} from './PDT-2529-rfd-data-model-happy-path.fixtures';

export type Pdt3212Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export type PodTabRow = {
  podIdentifier?: string;
  podId?: number;
  customerId?: number;
  isHighestConsumption?: boolean;
  isChecked?: boolean;
  liabilityAmountCustomer?: number;
  customers?: string;
};

export type CustomersForDpsQuery = {
  page: number;
  size: number;
  conditionType: 'ALL_CUSTOMERS' | 'LIST_OF_CUSTOMERS' | 'CUSTOMERS_UNDER_CONDITIONS';
  supplierType?: 'CURRENT' | 'PREVIOUS';
  powerSupplyDisconnectionReminderId?: number;
  gridOperatorId?: number;
  listOfCustomer?: string;
  searchBy?: string;
  prompt?: string;
  sortBy?: string;
  direction?: 'ASC' | 'DESC';
  isHighestConsumption?: boolean;
  liabilityAmountFrom?: number;
  liabilityAmountTo?: number;
};

export type Pdt3212PodRef = { id: number; identifier: string; index: number };

export type Pdt3212ChainCtx = {
  customerIdentifier: string;
  customerId: number;
  contractNumber: string;
  productContractId: number;
  billingGroupId: number;
  reminderId: number;
  gridOperatorId: number;
  anchor: RfdVolumeBillingAnchor;
  pods: Pdt3212PodRef[];
};

export { RFD_MIN_INVOICE_PAYMENT_TERM_DAYS, reminderIdFromResponses, resolveRfdVolumeBillingAnchor };
export type { RfdVolumeBillingAnchor };

export function pdt3212RunId(testInfo: TestInfo): string {
  return `${testInfo.workerIndex}${Date.now()}`;
}

function pdt3212Alphanumeric(value: string): string {
  return value.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
}

/** POD identifier — 32X prefix, max 33 chars, A-Z 0-9 only (POST /pod validation). */
export function buildPdt3212PodIdentifier(tcSuffix: string, runId: string, podIndex: number): string {
  const suffix = `${pdt3212Alphanumeric(tcSuffix)}${podIndex}${runId}`;
  const core = randomGens.generateUniqueIdentifier();
  return (`32X${core}${suffix}`).slice(0, 33);
}

/** Customer identifier — max 17 chars, A-Z 0-9 only. */
export function buildPdt3212CustomerIdentifier(tcSuffix: string, runId: string): string {
  const suffix = `${pdt3212Alphanumeric(tcSuffix)}${runId}`.slice(-8);
  const core = randomGens.generateUniqueIdentifier();
  return `${core.slice(0, 17 - suffix.length)}${suffix}`;
}

export function isoDate(offsetDays: number): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().split('T')[0];
}

function monthFirstFromYmd(endYmd: string): string {
  return `${endYmd.slice(0, 7)}-01`;
}

function lastDayOfMonth(year: number, month1to12: number): string {
  const d = new Date(Date.UTC(year, month1to12, 0));
  return d.toISOString().slice(0, 10);
}

function monthsBeforeMonthEnd(fromYmd: string, monthsBack: number): string {
  const y = Number(fromYmd.slice(0, 4));
  const m = Number(fromYmd.slice(5, 7));
  const ref = new Date(Date.UTC(y, m - 1, 1));
  ref.setUTCMonth(ref.getUTCMonth() - monthsBack);
  return lastDayOfMonth(ref.getUTCFullYear(), ref.getUTCMonth() + 1);
}

export async function resolvePriorBillingAnchor(
  Request: Pdt3212Fx['Request'],
  baseAnchor: RfdVolumeBillingAnchor,
): Promise<RfdVolumeBillingAnchor> {
  const invoicePeriodTo = monthsBeforeMonthEnd(baseAnchor.invoicePeriodTo, 1);
  const accountingPeriodId = await resolveAccountingPeriodIdForBillingDate(Request, invoicePeriodTo);
  let invoiceDate = invoicePeriodTo;
  const apRes = await Request.get(`accounting-period/${accountingPeriodId}`);
  if (apRes.ok()) {
    const ap = (await apRes.json()) as { startDate?: string };
    const startYmd = String(ap.startDate ?? '').slice(0, 10);
    if (startYmd && invoicePeriodTo <= startYmd) {
      const next = new Date(`${startYmd}T00:00:00Z`);
      next.setUTCDate(next.getUTCDate() + 1);
      invoiceDate = next.toISOString().slice(0, 10);
    }
  }
  return {
    invoicePeriodTo,
    taxEventDate: invoiceDate,
    invoiceDate,
    accountingPeriodId,
    profileStart: monthFirstFromYmd(invoicePeriodTo),
    profileEnd: invoicePeriodTo,
    commentary: `Prior month anchor before ${baseAnchor.invoicePeriodTo}`,
  };
}

export async function createCatalogEntities(fx: Pdt3212Fx): Promise<void> {
  const term = await fx.Request.post(fx.Endpoints.terms, {
    data: termPayloadWithMinimalPaymentDays(fx.GeneratePayload),
  });
  await expect(term).CheckResponse();
  fx.Responses.terms.push(await term.json());

  const pricePayload = fx.GeneratePayload.productAndServices.priceSettlement();
  const price = await fx.Request.post(fx.Endpoints.priceComponent, { data: pricePayload });
  await expect(price).CheckResponse();
  fx.Responses.priceComponent.push(await price.json());

  const product = await fx.Request.post(fx.Endpoints.product, {
    data: fx.GeneratePayload.productAndServices.product(),
  });
  await expect(product).CheckResponse();
  fx.Responses.product.push(await product.json());
}

export async function createCustomerWithIdentifier(fx: Pdt3212Fx, identifier: string): Promise<void> {
  const payload = fx.GeneratePayload.customers.customer_legal();
  payload.customerIdentifier = identifier;
  const customer = await fx.Request.post(fx.Endpoints.customer, { data: payload });
  await expect(customer).CheckResponse();
  fx.Responses.customer.push(await customer.json());
}

export function resolveCustomerIdentifier(fx: Pdt3212Fx, customerIndex = 0): string {
  return String(fx.Responses.customer[customerIndex].identifier);
}

export async function createPodWithIdentifier(
  fx: Pdt3212Fx,
  identifier: string,
  gridOperatorId: number = envVariables.grid_operator,
): Promise<Pdt3212PodRef> {
  const payload = fx.GeneratePayload.pointsOfDelivery.pod_settlement();
  payload.identifier = identifier;
  payload.gridOperatorId = gridOperatorId;
  const podRes = await fx.Request.post(fx.Endpoints.pod, { data: payload });
  await expect(podRes).CheckResponse();
  const podJson = await podRes.json();
  const index = fx.Responses.pod.length;
  fx.Responses.pod.push(podJson);
  const id = Number(podJson.id);
  let resolvedIdentifier = String(podJson.identifier ?? identifier);
  if (!podJson.identifier) {
    const podGet = await fx.Request.get(`${fx.Endpoints.pod}/${id}`);
    await expect(podGet).CheckResponse();
    resolvedIdentifier = String((await podGet.json()).identifier);
  }
  return { id, identifier: resolvedIdentifier, index };
}

export async function createProductContractFromPods(fx: Pdt3212Fx): Promise<void> {
  const contract = await fx.Request.post(fx.Endpoints.productContract, {
    data: await fx.GeneratePayload.contractsAndOrders.product_contract(),
  });
  await expect(contract).CheckResponse();
  fx.Responses.productContract.push(await contract.json());
}

export async function activatePodAtIndex(fx: Pdt3212Fx, podIndex: number, deactivationDate: string | null = null): Promise<void> {
  let activationDate: string | undefined;
  if (deactivationDate != null) {
    const deact = new Date(`${deactivationDate}T00:00:00Z`);
    deact.setUTCDate(deact.getUTCDate() - 28);
    activationDate = deact.toISOString().slice(0, 10);
  }
  const activationPayload = await fx.GeneratePayload.pointsOfDelivery.pod_activation(
    podIndex,
    activationDate,
    deactivationDate ?? undefined,
  );
  const podActivation = await fx.Request.post('/contract-pods/manual', { data: activationPayload });
  await expect(podActivation).CheckResponse();
}

export async function activateAllPods(fx: Pdt3212Fx): Promise<void> {
  for (let i = 0; i < fx.Responses.pod.length; i++) {
    await activatePodAtIndex(fx, i);
  }
}

export async function resolveContractNumber(fx: Pdt3212Fx): Promise<string> {
  const contractId = fx.Responses.productContract[0].id;
  const contractGet = await fx.Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const body = await contractGet.json();
  const number = String(body.basicParameters?.contractNumber ?? '');
  expect(number.length).toBeGreaterThan(0);
  return number;
}

export function buildBillingByProfilePayload(
  podIdentifier: string,
  anchor: RfdVolumeBillingAnchor,
  kwh: number,
): Record<string, unknown> {
  const payload = profile1MonthTemplate() as Record<string, unknown>;
  const dateRanges = profileDateRangeFromAnchor(anchor);
  payload.identifier = podIdentifier;
  payload.timeZone = 'CET';
  payload.periodFrom = `${dateRanges[0].startDate}T00:00:00.000Z`;
  payload.periodTo = `${dateRanges[0].endDate}T00:00:00.000Z`;
  const entries = payload.entries as Array<Record<string, unknown>>;
  if (entries[0]) {
    entries[0].periodFrom = `${dateRanges[0].startDate}T00:00:00.000Z`;
    entries[0].value = kwh;
  }
  return payload;
}

export async function postBillingByProfileKwh(
  fx: Pdt3212Fx,
  podIdentifier: string,
  anchor: RfdVolumeBillingAnchor,
  kwh: number,
): Promise<void> {
  const payload = buildBillingByProfilePayload(podIdentifier, anchor, kwh);
  const profiles = await fx.Request.post('billing-by-profile', { data: payload });
  await expect(profiles).CheckResponse();
}

export async function runForVolumesBillingAtAnchor(
  fx: Pdt3212Fx,
  anchor: RfdVolumeBillingAnchor,
  contractNumber: string,
  expectedInvoices: number = 1,
): Promise<number> {
  const billingPayload = await fx.GeneratePayload.billing.billingRun();
  billingPayload.basicParameters.applicationModelType = ['FOR_VOLUMES'];
  billingPayload.basicParameters.billingApplicationLevel = 'CONTRACT';
  billingPayload.basicParameters.listOfCustomersContractsOrPOD = contractNumber;
  applyVolumeBillingAnchor(billingPayload, anchor);
  const billingRun = await fx.Request.post(fx.Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRunBody = await billingRun.json();
  fx.Responses.billingRun.push(billingRunBody);
  const billingIndex = fx.Responses.billingRun.length - 1;
  await fx.GeneratePayload.billing.waitForInvoiceGeneration(true, true, expectedInvoices, billingIndex);
  const raw = fx.Responses.billingRun[billingIndex];
  return typeof raw === 'object' && raw !== null && 'id' in raw ? Number((raw as { id: number }).id) : Number(raw);
}

async function collectOpenLiabilityIdsFromInvoices(fx: Pdt3212Fx): Promise<number[]> {
  const ids: number[] = [];
  for (const rawInvoiceId of fx.Responses.invoice) {
    const invoiceId =
      typeof rawInvoiceId === 'number'
        ? rawInvoiceId
        : Number((rawInvoiceId as { id?: number }).id);
    if (!Number.isFinite(invoiceId)) continue;
    const invoiceRes = await fx.Request.get(`invoice?id=${invoiceId}`);
    await expect(invoiceRes).CheckResponse();
    const invoice = await invoiceRes.json();
    for (const ref of (invoice.liabilitiesAndReceivables ?? []) as Array<{ id?: number }>) {
      if (ref?.id) ids.push(Number(ref.id));
    }
  }
  return ids;
}

async function collectOpenLiabilityIdsFromCustomerSearch(
  fx: Pdt3212Fx,
  customerIdentifier: string,
): Promise<number[]> {
  const customerId = Number(fx.Responses.customer[0]?.id);
  const prompts = [customerIdentifier, String(customerId)].filter(Boolean);
  for (const prompt of prompts) {
    const liabilitiesRes = await fx.Request.get(
      `customer-liability/list?page=0&size=25&columns=ID&direction=DESC&prompt=${encodeURIComponent(prompt)}&searchFields=CUSTOMER`,
    );
    await expect(liabilitiesRes).CheckResponse();
    const body = await liabilitiesRes.json();
    const rows = (body.content ?? []) as Array<{ id: number; currentAmount?: number }>;
    const open = rows.filter((r) => (r.currentAmount ?? 0) > 0);
    if (open.length > 0) return open.map((r) => r.id);
  }
  return [];
}

export async function shiftLiabilitiesToYesterday(fx: Pdt3212Fx, customerIdentifier?: string): Promise<void> {
  const identifier = customerIdentifier ?? String(fx.Responses.customer[0].identifier);
  let liabilityIds = await collectOpenLiabilityIdsFromInvoices(fx);
  if (liabilityIds.length === 0) {
    liabilityIds = await collectOpenLiabilityIdsFromCustomerSearch(fx, identifier);
  }
  expect(liabilityIds.length, `Expected open liability for customer ${identifier}`).toBeGreaterThan(0);

  const dueDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');
  const todayYmd = new Date().toISOString().slice(0, 10);
  for (const liabilityId of liabilityIds) {
    const liabilityRes = await fx.Request.get(`customer-liability/${liabilityId}`);
    await expect(liabilityRes).CheckResponse();
    const liability = await liabilityRes.json();
    if (Number(liability.currentAmount ?? 0) <= 0) continue;
    const liabilityDueDate = String(liability.dueDate ?? '').slice(0, 10);
    if (liabilityDueDate && liabilityDueDate < todayYmd) continue;
    const change = await fx.Request.put(`customer-liability/${liabilityId}/due-date-change?dueDate=${dueDate}`);
    await expect(change).CheckResponse();
  }
}

export async function createReminderForDisconnection(fx: Pdt3212Fx): Promise<number> {
  const reminderPayload = fx.GeneratePayload.receivablesManagement.reminderForDisconnection();
  const reminderRes = await fx.Request.post(fx.Endpoints.reminderForDisconnection, { data: reminderPayload });
  await expect(reminderRes).CheckResponse();
  const reminderJson = await reminderRes.json();
  fx.Responses.reminderForDisconnection.push(reminderJson);
  return typeof reminderJson === 'number' ? reminderJson : Number(reminderJson.id);
}

export function latestReminderId(Responses: { reminderForDisconnection: unknown[] }): number {
  const r = Responses.reminderForDisconnection[Responses.reminderForDisconnection.length - 1] as
    | number
    | { id: number };
  return typeof r === 'number' ? r : r.id;
}

export async function executeReminderForDisconnection(fx: Pdt3212Fx, reminderId?: number): Promise<void> {
  const id = reminderId ?? latestReminderId(fx.Responses);
  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const timeOffset = await fx.Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${id}&hour=${georgianTime.getHours()}&minute=${georgianTime.getMinutes()}`,
  );
  await expect(timeOffset).CheckResponse();
  await fx.Request.post('power-supply-disconnection-reminder/job');
  for (let attempt = 1; attempt <= 24; attempt++) {
    const reminderGet = await fx.Request.get(`power-supply-disconnection-reminder/${id}`);
    await expect(reminderGet).CheckResponse();
    const reminderJson = await reminderGet.json();
    if (reminderJson.reminderStatus === 'EXECUTED') {
      return;
    }
    if (attempt < 24) {
      await new Promise((r) => setTimeout(r, 5000));
    }
  }
  throw new Error('Reminder for disconnection did not reach EXECUTED status');
}

export async function createAndExecuteReminder(fx: Pdt3212Fx): Promise<number> {
  const reminderPayload = fx.GeneratePayload.receivablesManagement.reminderForDisconnection();
  const reminderRes = await fx.Request.post(fx.Endpoints.reminderForDisconnection, { data: reminderPayload });
  await expect(reminderRes).CheckResponse();
  const reminderJson = await reminderRes.json();
  fx.Responses.reminderForDisconnection.push(reminderJson);
  const id = typeof reminderJson === 'number' ? reminderJson : Number(reminderJson.id);
  await executeReminderForDisconnection(fx, id);
  return id;
}

export function loadCustomersForDpsQueryBuilder(
  ctx: Pick<Pdt3212ChainCtx, 'reminderId' | 'gridOperatorId' | 'customerIdentifier'>,
  overrides: Partial<CustomersForDpsQuery> = {},
): CustomersForDpsQuery {
  return {
    page: 0,
    size: 100,
    conditionType: 'LIST_OF_CUSTOMERS',
    supplierType: 'CURRENT',
    powerSupplyDisconnectionReminderId: ctx.reminderId,
    gridOperatorId: ctx.gridOperatorId,
    listOfCustomer: ctx.customerIdentifier,
    ...overrides,
  };
}

export async function loadCustomersForDps(
  fx: Pdt3212Fx,
  query: CustomersForDpsQuery,
  options: {
    expectSuccess?: boolean;
    waitForPodIdentifiers?: string[];
    maxWaitAttempts?: number;
    waitIntervalMs?: number;
  } = {},
): Promise<{ status: number; rows: PodTabRow[]; body: Record<string, unknown> }> {
  const expectSuccess = options.expectSuccess ?? true;
  const waitForPodIdentifiers = options.waitForPodIdentifiers ?? [];
  const maxAttempts =
    waitForPodIdentifiers.length > 0 ? (options.maxWaitAttempts ?? 12) : 1;
  const waitIntervalMs = options.waitIntervalMs ?? 5000;

  let lastResult = { status: 0, rows: [] as PodTabRow[], body: {} as Record<string, unknown> };
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const res = await fx.Request.get(
      'disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply',
      { params: query as Record<string, string | number | boolean> },
    );
    const body = (await res.json().catch(() => ({}))) as Record<string, unknown>;
    const rows = ((body.content as PodTabRow[]) ?? []) as PodTabRow[];
    lastResult = { status: res.status(), rows, body };
    if (expectSuccess) {
      await expect(res).CheckResponse();
    } else if (attempt === maxAttempts) {
      return lastResult;
    }
    const allPresent =
      waitForPodIdentifiers.length === 0 ||
      waitForPodIdentifiers.every((id) => findPodRow(rows, id));
    if (allPresent) {
      return lastResult;
    }
    if (attempt < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, waitIntervalMs));
    }
  }
  return lastResult;
}

export async function viewPodTab(
  fx: Pdt3212Fx,
  rfdId: number,
  query: CustomersForDpsQuery,
): Promise<PodTabRow[]> {
  const res = await fx.Request.get(`disconnection-of-power-supply-requests/view-pod-tab/${rfdId}`, {
    params: query as Record<string, string | number | boolean>,
  });
  await expect(res).CheckResponse();
  const body = await res.json();
  return (body.content ?? []) as PodTabRow[];
}

export function findPodRow(rows: PodTabRow[], podIdentifier: string): PodTabRow | undefined {
  return rows.find((r) => r.podIdentifier === podIdentifier);
}

export function assertPodAbsent(rows: PodTabRow[], podIdentifier: string): void {
  expect(findPodRow(rows, podIdentifier), `POD ${podIdentifier} should be absent`).toBeFalsy();
}

export function assertPodPresent(rows: PodTabRow[], podIdentifier: string): PodTabRow {
  const row = findPodRow(rows, podIdentifier);
  expect(row, `POD ${podIdentifier} should be present`).toBeTruthy();
  return row!;
}

export function assertHighestConsumption(row: PodTabRow | undefined, expected: boolean, label: string): void {
  expect(row, `${label}: row missing`).toBeTruthy();
  expect(row!.isHighestConsumption, `${label}: isHighestConsumption`).toBe(expected);
}

export function assertExactlyOneHighestAmong(rows: PodTabRow[], podIdentifiers: string[]): PodTabRow {
  const targetRows = podIdentifiers.map((id) => assertPodPresent(rows, id));
  const yesRows = targetRows.filter((r) => r.isHighestConsumption === true);
  expect(yesRows.length, 'Exactly one listed POD should have isHighestConsumption=true').toBe(1);
  return yesRows[0];
}

export function rfdPayloadTemplate(fx: Pdt3212Fx): Record<string, unknown> {
  const payload = fx.GeneratePayload.receivablesManagement.requestForDisconnection() as Record<string, unknown>;
  payload.reminderForDisconnectionId = latestReminderId(fx.Responses);
  payload.gridOperatorId = envVariables.grid_operator;
  payload.reasonOfDisconnectionId = envVariables.reason_for_disconnection;
  return payload;
}

export async function createRfdDraft(
  fx: Pdt3212Fx,
  overrides: Record<string, unknown> = {},
): Promise<number> {
  const payload = rfdPayloadTemplate(fx);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = fx.Responses.customer[0].identifier as string;
  payload.allSelected = true;
  payload.pods = [];
  payload.excludePodIds = [];
  payload.podWithHighestConsumption = false;
  payload.templateIds = [];
  payload.files = [];
  payload.gridOpRequestRegDate = isoDate(0);
  Object.assign(payload, overrides);
  const res = await fx.Request.post(fx.Endpoints.requestForDisconnection, { data: payload });
  await expect(res).CheckResponse();
  const id = Number(await res.json());
  fx.Responses.requestForDisconnection.push(id);
  return id;
}

export async function updateRfdDraft(
  fx: Pdt3212Fx,
  rfdId: number,
  overrides: Record<string, unknown> = {},
): Promise<void> {
  const payload = rfdPayloadTemplate(fx);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = fx.Responses.customer[0].identifier as string;
  payload.allSelected = true;
  payload.pods = [];
  payload.excludePodIds = [];
  payload.podWithHighestConsumption = false;
  payload.templateIds = [];
  payload.files = [];
  payload.gridOpRequestRegDate = isoDate(0);
  Object.assign(payload, overrides);
  const res = await fx.Request.put(`${fx.Endpoints.requestForDisconnection}/${rfdId}`, { data: payload });
  await expect(res).CheckResponse();
}

export async function executeRfd(
  fx: Pdt3212Fx,
  rfdId: number,
  overrides: Record<string, unknown> = {},
): Promise<void> {
  const payload = rfdPayloadTemplate(fx);
  payload.disconnectionRequestsStatus = 'EXECUTED';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = fx.Responses.customer[0].identifier as string;
  payload.allSelected = true;
  payload.pods = [];
  payload.excludePodIds = [];
  payload.podWithHighestConsumption = false;
  payload.templateIds = [envVariables.request_disconnection_document_template];
  payload.files = [];
  payload.gridOpRequestRegDate = isoDate(0);
  payload.customerReminderLetterSentDate = null;
  payload.gridOpDisconnectionFeePayDate = null;
  payload.powerSupplyDisconnectionDate = null;
  Object.assign(payload, overrides);
  const res = await fx.Request.put(`${fx.Endpoints.requestForDisconnection}/${rfdId}`, { data: payload });
  await expect(res).CheckResponse();
}

export async function fetchPodRowFromLoadCustomers(
  fx: Pdt3212Fx,
  query: CustomersForDpsQuery,
  podIdentifier: string,
  checked = false,
): Promise<PodTabRow> {
  const searchQuery = { ...query, searchBy: 'POD_IDENTIFIER', prompt: podIdentifier };
  const { rows } = await loadCustomersForDps(fx, searchQuery);
  const row = findPodRow(rows, podIdentifier);
  expect(row, `Expected POD row for ${podIdentifier}`).toBeTruthy();
  return { ...(row as PodTabRow), isChecked: checked };
}

export async function fetchCheckedPods(
  fx: Pdt3212Fx,
  reminderId: number,
  gridOperatorId: number,
  rfdId: number,
): Promise<PodTabRow[]> {
  const res = await fx.Request.get(
    `disconnection-of-power-supply-requests/get-checked-pods/${reminderId}/${gridOperatorId}/${rfdId}`,
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  return Array.isArray(body) ? (body as PodTabRow[]) : ((body.content as PodTabRow[]) ?? []);
}

export async function createExecutedRfdWithPods(
  fx: Pdt3212Fx,
  pods: PodTabRow[],
  overrides: Record<string, unknown> = {},
): Promise<number> {
  const payload = rfdPayloadTemplate(fx);
  payload.disconnectionRequestsStatus = 'EXECUTED';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = fx.Responses.customer[0].identifier as string;
  payload.allSelected = false;
  payload.pods = pods;
  payload.excludePodIds = [];
  payload.podWithHighestConsumption = false;
  payload.templateIds = [envVariables.request_disconnection_document_template];
  payload.gridOpRequestRegDate = isoDate(0);
  Object.assign(payload, overrides);
  const res = await fx.Request.post(fx.Endpoints.requestForDisconnection, { data: payload });
  await expect(res).CheckResponse();
  const id = Number(await res.json());
  fx.Responses.requestForDisconnection.push(id);
  return id;
}

export async function calculateRfdTax(fx: Pdt3212Fx, rfdId: number): Promise<void> {
  const calc = await fx.Request.post(`disconnection-of-power-supply-requests/calculate-tax/${rfdId}`);
  await expect(calc).CheckResponse();
}

export async function markPodDisconnectedExecuted(
  fx: Pdt3212Fx,
  rfdId: number,
  customerId: number,
  podId: number,
): Promise<void> {
  const payload = {
    requestForDisconnectionId: rfdId,
    saveType: 'EXECUTED',
    disconnectedRequest: [
      {
        customerId,
        podId,
        gridOperatorTaxesId: envVariables.taxes_for_grid_operator,
        expressReconnection: false,
        dateOfDisconnection: isoDate(0),
      },
    ],
  };
  const res = await fx.Request.post('disconnection-of-power-supply', { data: payload });
  await expect(res).CheckResponse();
}

export async function createCancellationExecuted(
  fx: Pdt3212Fx,
  rfdId: number,
  podRow: PodTabRow,
): Promise<number> {
  const podId = Number(podRow.podId);
  const customerId = Number(podRow.customerId);
  expect(Number.isFinite(podId), 'cancellation podId').toBeTruthy();
  expect(Number.isFinite(customerId), 'cancellation customerId').toBeTruthy();
  const payload = fx.GeneratePayload.receivablesManagement.cancellationOfRequestForDisconnection();
  payload.requestForDisconnectionOfThePowerSupplyId = rfdId as never;
  payload.saveAs = 'EXECUTED';
  payload.table[0].podId = podId as never;
  payload.table[0].customerId = customerId as never;
  payload.table[0].requestForDisconnectionOfPowerSupplyId = rfdId as never;
  payload.templateIds = [];
  payload.fileIds = [];
  const res = await fx.Request.post(fx.Endpoints.cancellationOfRequestOfDisconnection, { data: payload });
  await expect(res).CheckResponse();
  const id = Number(await res.json());
  fx.Responses.cancellationOfRequestOfDisconnection.push(id);
  return id;
}

export async function createReconnectionExecuted(
  fx: Pdt3212Fx,
  rfdId: number,
  podRow: PodTabRow,
): Promise<number> {
  const podId = Number(podRow.podId);
  const customerId = Number(podRow.customerId);
  expect(Number.isFinite(podId), 'reconnection podId').toBeTruthy();
  expect(Number.isFinite(customerId), 'reconnection customerId').toBeTruthy();
  const payload = fx.GeneratePayload.receivablesManagement.reconnectionOfPowerSupply();
  payload.gridOperatorId = envVariables.grid_operator;
  payload.saveAs = 'EXECUTED';
  payload.table[0].podId = podId;
  payload.table[0].customerId = customerId;
  payload.table[0].requestForDisconnectionOfPowerSupplyId = rfdId;
  payload.templateIds = [];
  payload.fileIds = [];
  const res = await fx.Request.post(fx.Endpoints.reconnectionOfPowerSupply, { data: payload });
  await expect(res).CheckResponse();
  const id = Number(await res.json());
  fx.Responses.reconnectionOfPowerSupply.push(id);
  return id;
}

export async function terminateContractForPreviousSupplier(fx: Pdt3212Fx): Promise<void> {
  const contractId = fx.Responses.productContract[0].id;
  const res = await fx.Request.put(`product-contract/status-update/${contractId}?versionId=1`, {
    data: {
      contractStatus: 'TERMINATED',
      contractSubStatus: 'ALL_PODS_ARE_DEACTIVATED',
      contractVersionStatus: 'SIGNED',
    },
  });
  await expect(res).CheckResponse();
}

export async function resolveSecondaryGridOperatorId(fx: Pdt3212Fx): Promise<number> {
  const primary = envVariables.grid_operator;
  const res = await fx.Request.get(
    'nomenclature/grid-operators/filter?statuses=ACTIVE&page=0&size=25&prompt=GIO',
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  const content = (body.content ?? []) as Array<{ id: number }>;
  const alt = content.find((r) => r.id !== primary) ?? content[0];
  expect(alt?.id, 'Secondary grid operator required').toBeTruthy();
  return Number(alt!.id);
}

export async function executeMassBlockingForSupplyTermination(
  fx: Pdt3212Fx,
  customerIdentifier: string,
): Promise<void> {
  const payload = fx.GeneratePayload.receivablesManagement.mass_operation_for_blocking();
  payload.requestReceivableBlockingStatus = 'EXECUTED';
  payload.receivableBlockingConditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomers = customerIdentifier as never;
  payload.isBlockForSupplyTermination = true;
  payload.blockingForSupplyTermination = {
    fromDate: randomGens.generateYesterdaysDate('yyyy-mm-dd'),
    toDate: null,
    reasonId: envVariables.blocking_reason,
    additionalInformation: null,
    reasonType: 'BLOCKED_FOR_SUPPLY_TERMINATION',
  };
  payload.isBlockForPayment = false;
  payload.blockingForPayment = null;
  payload.isBlockForReminderLetters = false;
  payload.blockingForReminderLetters = null;
  payload.isBlockForCalculation = false;
  payload.blockingForCalculation = null;
  payload.isBlockForLiabilitiesOffsetting = false;
  payload.blockingForLiabilitiesOffsetting = null;
  const res = await fx.Request.post(fx.Endpoints.massOperationForBlocking, { data: payload });
  await expect(res).CheckResponse();
  fx.Responses.massOperationForBlocking.push(await res.json());
}

export type VolumeReminderChainOptions = {
  runId: string;
  tcSuffix: string;
  podLabels: string[];
  podVolumes?: Array<number | null>;
  supplierType?: 'CURRENT' | 'PREVIOUS';
  skipBillingProfiles?: boolean;
  multiAnchorVolumes?: Array<{ podIndex: number; kwh: number; anchor: RfdVolumeBillingAnchor }>;
};

/** Standard FOR_VOLUMES + reminder chain ending before RFD draft (unless caller creates draft). */
export async function runVolumeReminderChain(
  fx: Pdt3212Fx,
  opts: VolumeReminderChainOptions,
): Promise<Pdt3212ChainCtx> {
  await createCatalogEntities(fx);
  await createCustomerWithIdentifier(fx, buildPdt3212CustomerIdentifier(opts.tcSuffix, opts.runId));
  const customerIdentifier = resolveCustomerIdentifier(fx);

  const pods: Pdt3212PodRef[] = [];
  for (let podIndex = 0; podIndex < opts.podLabels.length; podIndex++) {
    pods.push(await createPodWithIdentifier(fx, buildPdt3212PodIdentifier(opts.tcSuffix, opts.runId, podIndex)));
  }

  await createProductContractFromPods(fx);
  await activateAllPods(fx);
  const { billingGroupId } = await enableSeparateInvoiceForEachPod(fx.Request, fx.GeneratePayload);
  const anchor = await resolveRfdVolumeBillingAnchor(fx.Request);
  const contractNumber = await resolveContractNumber(fx);

  if (opts.multiAnchorVolumes?.length) {
    for (const item of opts.multiAnchorVolumes) {
      const pod = pods[item.podIndex];
      await postBillingByProfileKwh(fx, pod.identifier, item.anchor, item.kwh);
      await runForVolumesBillingAtAnchor(fx, item.anchor, contractNumber, 1);
    }
  } else if (!opts.skipBillingProfiles) {
    const volumes = opts.podVolumes ?? pods.map(() => 150);
    for (let i = 0; i < pods.length; i++) {
      const vol = volumes[i];
      if (vol != null) {
        await postBillingByProfileKwh(fx, pods[i].identifier, anchor, vol);
      }
    }
    if (volumes.some((v) => v != null)) {
      const expectedInvoices = volumes.filter((v) => v != null).length;
      await runForVolumesBillingAtAnchor(fx, anchor, contractNumber, expectedInvoices);
    }
  }

  await shiftLiabilitiesToYesterday(fx, customerIdentifier);
  const reminderId = await createAndExecuteReminder(fx);

  return {
    customerIdentifier,
    customerId: Number(fx.Responses.customer[0].id),
    contractNumber,
    productContractId: Number(fx.Responses.productContract[0].id),
    billingGroupId,
    reminderId,
    gridOperatorId: envVariables.grid_operator,
    anchor,
    pods,
  };
}

/** Billing + reminder on existing Responses (customer, pods, contract already created). */
export async function completeBillingReminderForCurrentContract(
  fx: Pdt3212Fx,
  opts: {
    customerIdentifier: string;
    podVolumes: Array<number | null>;
    skipReminder?: boolean;
  },
): Promise<Pdt3212ChainCtx> {
  const { billingGroupId } = await enableSeparateInvoiceForEachPod(fx.Request, fx.GeneratePayload);
  const anchor = await resolveRfdVolumeBillingAnchor(fx.Request);
  const contractNumber = await resolveContractNumber(fx);
  const pods: Pdt3212PodRef[] = fx.Responses.pod.map((p: { id: number; identifier?: string }, index: number) => ({
    id: Number(p.id),
    identifier: String(p.identifier ?? ''),
    index,
  }));
  for (let i = 0; i < pods.length; i++) {
    const vol = opts.podVolumes[i];
    if (vol != null) {
      let identifier = pods[i].identifier;
      if (!identifier) {
        const podGet = await fx.Request.get(`${fx.Endpoints.pod}/${pods[i].id}`);
        await expect(podGet).CheckResponse();
        identifier = String((await podGet.json()).identifier);
        pods[i].identifier = identifier;
      }
      await postBillingByProfileKwh(fx, identifier, anchor, vol);
    }
  }
  const billedCount = opts.podVolumes.filter((v) => v != null).length || pods.length;
  await runForVolumesBillingAtAnchor(fx, anchor, contractNumber, billedCount);
  await shiftLiabilitiesToYesterday(fx, opts.customerIdentifier);
  const reminderId = opts.skipReminder ? 0 : await createAndExecuteReminder(fx);
  return {
    customerIdentifier: opts.customerIdentifier,
    customerId: Number(fx.Responses.customer[0].id),
    contractNumber,
    productContractId: Number(fx.Responses.productContract[0].id),
    billingGroupId,
    reminderId,
    gridOperatorId: envVariables.grid_operator,
    anchor,
    pods,
  };
}
