/**
 * PDT-3090 helpers — Request for disconnection highest-consumption fallback
 * when the billing group has no invoice POD total volumes.
 *
 * Dev2 DPSRequestsBaseRequest required (swagger-spec.json this session):
 *   supplierType, gridOperatorId, reasonOfDisconnectionId, gridOpRequestRegDate,
 *   validityPeriodFrom, validityPeriodTo, conditionType, reminderForDisconnectionId
 * Cursor jsons/payloads/create/Receivables/requestForDisconnection.ts does not
 * send validityPeriodFrom/To — set them here (Phase 2 / origin/staging pattern).
 * Never conditionType=ALL_CUSTOMERS or allSelected=true (Load PODs SQL too slow).
 * Dev2 reminder POST requires conditionType; ensureExecutedReminder uses PDT-2913
 * LIST_OF_CUSTOMERS + listOfCustomer (not 3179 createExecutedReminder / customerList).
 *
 * Identity is podId (pod.pod.id), not podIdentifier.
 * CustomersForDPSResponse.isHighestConsumption is boolean.
 * GET load-customer-for-disconnection-power-supply and view-pod-tab: HTTP 206
 * (CheckResponse uses response.ok() — 206 is success).
 *
 * Reference spec(s):
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 * - tests/cursor/PDT-3421-rfd-highest-consumption-uncheck.spec.ts
 * - tests/cursor/pdt-3421-rfd-highest-consumption-uncheck.fixtures.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/pdt-2937-invoice-detailed-data-same-pc-name.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/pdt-3042-cancellation-pod-auto-check.fixtures.ts
 *   (validityPeriodFrom = today, validityPeriodTo = last day of month)
 * - tests/cursor/pdt-2913-missing-email-object-reminder-for-disconnection.fixtures.ts
 *   (Dev2 PowerSupplyDisconnectionReminderBaseRequest: LIST_OF_CUSTOMERS + listOfCustomer)
 * - tests/cursor/pdt-2459-payment-reverse-lpf-offset.fixtures.ts
 *   (LPF job after paid overdue; filter list by this UIC only — never POST reminder /job)
 * - tests/receivableManagement/Rescheduling/happyPass.spec.ts
 * - jsons/payloads/create/Receivables/reschedulingCreate.ts
 * - jsons/payloads/create/Receivables/customerLiability.ts
 * - jsons/payloads/create/contractOrders/action.ts
 * - jsons/payloads/create/energyData/profile1Month.ts
 * - jsons/payloads/create/billing/forVolumes.ts
 * - origin/staging src/backend/jsons/payloads/Receivables/requestForDisconnection.ts
 *   (read-only git show — validityPeriodFrom/To only, not ALL_CUSTOMERS)
 */

import { expect } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import { CalculateRescheduling } from '../../jsons/payloads/create/Receivables/calculateRescheduling';
import { rescheduling as reschedulingPayloadTemplate } from '../../jsons/payloads/create/Receivables/rescheduling';
import {
  asNumber,
  collectCustomerInvoices,
  createOverdueManualLiabilityWithBillingGroup,
  createSupplyChain,
  customerIdentifier,
  entityId,
  findLiabilityByInvoiceId,
  getLiability,
  invoicesOfType,
  listingContent,
  monthStartYmd,
  nestedId,
  PDT_3179_RFD_POST_TIMEOUT_MS,
  PDT_3179_REVERSAL_RUN_TIMEOUT_MS,
  PDT_3179_TEST_TIMEOUT_MS,
  postDedicatedGridTax,
  resolveContractBillingGroupId,
  todayYmd,
  type Pdt3179Fx,
} from './pdt-3179-reconnection-invoice-emails.fixtures';
import {
  addAndActivateExtraPodsOnSameBillingGroup,
  resolvePodDetailId,
} from './pdt-3421-rfd-highest-consumption-uncheck.fixtures';
import {
  findPdt2459LpfLiabilityId,
  waitForPdt2459LpfGeneration,
} from './pdt-2459-payment-reverse-lpf-offset.fixtures';

export const PDT_3090_KEY = 'PDT-3090';
export const PDT_3090_TITLE =
  'Request for disconnection - Point of delivery with highest consumption incorrect value';

export const PDT_3090_BE13_TIMEOUT_MS = PDT_3179_TEST_TIMEOUT_MS;
export const PDT_3090_BE2_TIMEOUT_MS = PDT_3179_REVERSAL_RUN_TIMEOUT_MS;
export const PDT_3090_RFD_POST_TIMEOUT_MS = PDT_3179_RFD_POST_TIMEOUT_MS;
export const PDT_3090_LOAD_PODS_TIMEOUT_MS = 5 * 60 * 1000;
export const PDT_3090_REMINDER_POLL_MS = 2 * 60 * 1000;
export const PDT_3090_PROFILE_LOW_VALUE = 500;
/** TC-BE-2 step 11b — non-zero so HighId stays on Load PODs; must stay < Low. */
export const PDT_3090_PROFILE_HIGH_VALUE = 1;
export const PDT_3090_LPF_SOURCE_AMOUNT = 50;
export const PDT_3090_EQUAL_VOLUME_VALUE = 100;
export const PDT_3090_CLAIMED_PENALTY_AMOUNT = 80;
export const PDT_3090_EXTERNAL_OUTGOING_DOC = 'EXT-PDT3090-1';
/** Two FOR_VOLUMES runs + LPF/rescheduling/tax chains. */
export const PDT_3090_BE11_TIMEOUT_MS = PDT_3090_BE2_TIMEOUT_MS + PDT_3090_BE13_TIMEOUT_MS;
export const PDT_3090_PAYMENT_POLL_MS = 90_000;

export type Pdt3090ProfilePeriod = {
  periodFrom: string;
  periodTo: string;
  startYmd: string;
  endYmd: string;
};

export type Pdt3090ProfileValues = {
  lowValue: number;
  highValue: number;
};

export type Pdt3090Fx = Pdt3179Fx;

export type Pdt3090PodRef = {
  index: number;
  podId: number;
  podIdentifier: string;
};

export type Pdt3090PodPair = {
  low: Pdt3090PodRef;
  high: Pdt3090PodRef;
  all: Pdt3090PodRef[];
};

export type Pdt3090DpsRow = Record<string, unknown> & {
  podId?: unknown;
  isHighestConsumption?: unknown;
  isChecked?: unknown;
  podIdentifier?: unknown;
};

function logElapsed(started: number, message: string): void {
  console.log(`[PDT-3090] ${Date.now() - started}ms ${message}`);
}

export function pdt3090RelevantKeys(opts?: {
  includeInvoice?: boolean;
  includeRfd?: boolean;
}): Array<
  | 'customer'
  | 'pod'
  | 'product'
  | 'productContract'
  | 'customerLiability'
  | 'invoice'
  | 'billingRun'
  | 'reminderForDisconnection'
  | 'requestForDisconnection'
> {
  const keys: Array<
    | 'customer'
    | 'pod'
    | 'product'
    | 'productContract'
    | 'customerLiability'
    | 'invoice'
    | 'billingRun'
    | 'reminderForDisconnection'
    | 'requestForDisconnection'
  > = ['customer', 'pod', 'product', 'productContract', 'customerLiability'];
  if (opts?.includeInvoice) {
    keys.push('invoice', 'billingRun');
  }
  keys.push('reminderForDisconnection');
  if (opts?.includeRfd) keys.push('requestForDisconnection');
  return keys;
}

export function loadCustomersQuery(
  fx: Pdt3090Fx,
  reminderId: number,
): Record<string, string | number> {
  const listOfCustomer = customerIdentifier(fx.Responses);
  expect(listOfCustomer.length, 'CustomersForDPSRequest.listOfCustomer minLength 1').toBeGreaterThan(0);
  return {
    page: 0,
    size: 50,
    conditionType: 'LIST_OF_CUSTOMERS',
    listOfCustomer,
    supplierType: 'CURRENT',
    powerSupplyDisconnectionReminderId: reminderId,
    gridOperatorId: Number(envVariables.grid_operator),
  };
}

export function calendarMonthPeriod(monthOffset = 0): Pdt3090ProfilePeriod {
  const startYmd = randomGens.generateMonthStartDate('yyyy-mm-dd', monthOffset);
  const endYmd = randomGens.generateMonthEndDate('yyyy-mm-dd', monthOffset);
  return {
    startYmd,
    endYmd,
    periodFrom: `${startYmd}T00:00:00.000Z`,
    periodTo: `${endYmd}T00:00:00.000Z`,
  };
}

export function previousMonthStartYmd(): string {
  return randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
}

export function previousMonthEndYmd(): string {
  return randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
}

/** Local calendar date plus `days` (YYYY-MM-DD). PHN-4175 needs ≥6 working days after notice. */
export function plusCalendarDaysYmd(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

/**
 * FOR_VOLUMES invoice 3rd-tab totalVolumes need a BY_SETTLEMENT_PERIODS PC
 * (REG-713 / forVolumes.spec.ts). createSupplyChain uses electricity()
 * (WITH_ELECTRICITY_INVOICE) which cannot produce POD totalVolumes.
 */
export async function createSupplyChainWithSettlementPriceComponent(
  fx: Pdt3090Fx,
  opts?: { activationYmd?: string },
): Promise<{ customerId: number; podId: number; contractId: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const activationYmd = opts?.activationYmd ?? monthStartYmd();

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const settlementPayload = GeneratePayload.productAndServices.priceSettlement() as Record<string, unknown>;
  const am = settlementPayload.applicationModelRequest as {
    settlementPeriodsRequest?: { profiles?: Array<{ profileId: unknown; percentage: number }> };
  };
  if (am?.settlementPeriodsRequest) {
    am.settlementPeriodsRequest.profiles = [{ profileId: envVariables.profiles, percentage: 100 }];
  }
  const settlement = await Request.post(Endpoints.priceComponent, { data: settlementPayload });
  await expect(settlement).CheckResponse();
  Responses.priceComponent.push(await settlement.json());

  const productPayload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.priceComponentIds = [entityId(Responses.priceComponent[0])];
  productPayload.interimAdvancePayments = [];
  productPayload.interimAdvancePaymentGroups = [];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const customer = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(customer).CheckResponse();
  const customerBody = await customer.json();
  Responses.customer.push(customerBody);

  const pod = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod).CheckResponse();
  const podBody = await pod.json();
  Responses.pod.push(podBody);

  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
  (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
  const monthly =
    asNumber((podBody as { estimatedMonthlyAvgConsumption?: unknown }).estimatedMonthlyAvgConsumption) ||
    1;
  contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = (monthly * 12) / 1000;
  const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(contract).CheckResponse();
  const contractBody = await contract.json();
  Responses.productContract.push(contractBody);

  const activation = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(0, activationYmd),
  });
  await expect(activation).CheckResponse();

  return {
    customerId: entityId(customerBody),
    podId: entityId(podBody),
    contractId: entityId(contractBody),
  };
}

export async function resolvePodIdentifier(fx: Pdt3090Fx, podIndex: number): Promise<string> {
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

export async function resolveLowHighPods(fx: Pdt3090Fx): Promise<Pdt3090PodPair> {
  expect(fx.Responses.pod.length, 'need two settlement PODs on the same billing group').toBeGreaterThanOrEqual(
    2,
  );
  const all: Pdt3090PodRef[] = [];
  for (let index = 0; index < fx.Responses.pod.length; index += 1) {
    all.push({
      index,
      podId: entityId(fx.Responses.pod[index]),
      podIdentifier: await resolvePodIdentifier(fx, index),
    });
  }
  const ids = new Set(all.map((row) => row.podId));
  expect(ids.size, 'POD ids must be unique').toBe(all.length);
  const low = all.reduce((a, b) => (a.podId <= b.podId ? a : b));
  const high = all.reduce((a, b) => (a.podId >= b.podId ? a : b));
  expect(high.podId, 'podIdHighId must be greater than podIdLow').toBeGreaterThan(low.podId);
  return { low, high, all };
}

/**
 * Dev2 PowerSupplyDisconnectionReminderBaseRequest (required: communicationChannels,
 * conditionType, customerSendToDateAndTime, disconnectionDate, liabilitiesMaxDueDate).
 * Reshape like PDT-2913: LIST_OF_CUSTOMERS + listOfCustomer; omit legacy customerList /
 * customerFilterType. Do not call createExecutedReminder (3179 still posts that legacy shape).
 * Do not POST /job.
 */
export async function ensureExecutedReminder(fx: Pdt3090Fx): Promise<number> {
  const started = Date.now();
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const base = GeneratePayload.receivablesManagement.reminderForDisconnection() as Record<
    string,
    unknown
  >;
  const listOfCustomer = customerIdentifier(Responses);
  expect(
    listOfCustomer.length,
    'Swagger PowerSupplyDisconnectionReminderBaseRequest.listOfCustomer minLength 1',
  ).toBeGreaterThan(0);

  const emailTemplateId =
    base.emailTemplateId ?? envVariables.reminder_disconnection_email_template;
  const smsTemplateId = base.smsTemplateId ?? envVariables.reminder_disconnection_sms_template;
  const communicationChannels = Array.isArray(base.communicationChannels)
    ? (base.communicationChannels as string[])
    : ['EMAIL'];

  const payload: Record<string, unknown> = {
    customerSendToDateAndTime: base.customerSendToDateAndTime,
    liabilityAmountFrom: base.liabilityAmountFrom ?? null,
    liabilityAmountTo: base.liabilityAmountTo ?? null,
    // Yesterday max-due can drop LPF/instalment rows when due-date-change lands on "today"
    // in the API timezone. Today still includes yesterday dues (cl.due_date <= max).
    liabilitiesMaxDueDate: todayYmd(),
    listOfCustomer,
    conditionType: 'LIST_OF_CUSTOMERS',
    communicationChannels,
    emailTemplateId,
    smsTemplateId: smsTemplateId ?? null,
    documentTemplateId: null,
    disconnectionDate: base.disconnectionDate,
    printedFileName: base.printedFileName ?? '1',
    name: `PDT3090-${randomGens.generateRandomString(true, true, 8)}`,
    confirm: true,
  };
  expect(payload.conditionType, 'never ALL_CUSTOMERS on Dev2').toBe('LIST_OF_CUSTOMERS');

  const res = await Request.post(Endpoints.reminderForDisconnection, { data: payload });
  await expect(res).CheckResponse();
  const reminderId = entityId(await res.json());
  Responses.reminderForDisconnection.push(reminderId);
  logElapsed(started, `POST reminder ${reminderId} listOfCustomer=${listOfCustomer}`);

  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const timeOffset = await Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${reminderId}&hour=${georgianTime.getHours()}&minute=${georgianTime.getMinutes()}`,
    { timeout: 5_000 },
  );
  await expect(timeOffset).CheckResponse();
  logElapsed(started, 'set-customer-send-time');

  const deadline = Date.now() + PDT_3090_REMINDER_POLL_MS;
  while (Date.now() < deadline) {
    const reminderGet = await Request.get(`power-supply-disconnection-reminder/${reminderId}`, {
      timeout: 5_000,
    });
    if (reminderGet.ok()) {
      await expect(reminderGet).CheckResponse();
      const status = ((await reminderGet.json()) as { reminderStatus?: string }).reminderStatus;
      if (status === 'EXECUTED') {
        logElapsed(started, `reminder ${reminderId} EXECUTED`);
        return reminderId;
      }
    }
    await new Promise((r) => setTimeout(r, 2_000));
  }
  const finalGet = await Request.get(`power-supply-disconnection-reminder/${reminderId}`);
  await expect(finalGet).CheckResponse();
  const finalStatus = ((await finalGet.json()) as { reminderStatus?: string }).reminderStatus;
  expect(
    finalStatus,
    `reminder ${reminderId} must be EXECUTED for Load PODs / RFD (no /job)`,
  ).toBe('EXECUTED');
  return reminderId;
}

export function createdReminderId(fx: Pdt3090Fx): number {
  return entityId(fx.Responses.reminderForDisconnection[0]);
}

export async function loadCustomersForDps(
  fx: Pdt3090Fx,
  reminderId: number,
  expectedLiabilityType?: string,
): Promise<{ status: number; rows: Pdt3090DpsRow[] }> {
  const types = await readSelectedDisconnectionTypes(fx);
  console.log(`[PDT-3090] liabilitiesForDisconnection=${types.join(',') || 'empty'}`);
  const params = loadCustomersQuery(fx, reminderId);
  const res = await fx.Request.get(
    `${fx.Endpoints.requestForDisconnection}/load-customer-for-disconnection-power-supply`,
    { params, timeout: PDT_3090_LOAD_PODS_TIMEOUT_MS },
  );
  await expect(res).CheckResponse();
  const status = res.status();
  expect(status, 'Load PODs HTTP 206 PARTIAL_CONTENT').toBe(206);
  const body = await res.json();
  const rows = listingContent(body) as Pdt3090DpsRow[];
  if (
    expectedLiabilityType &&
    rows.length === 0 &&
    !types.includes(expectedLiabilityType)
  ) {
    expect(
      types,
      `Load PODs returned 0 rows. GET /system-configurations.liabilitiesForDisconnection=[${types.join(',') || 'empty'}] does not include ${expectedLiabilityType}. Tests must not POST /system-configurations.`,
    ).toContain(expectedLiabilityType);
  }
  return { status, rows };
}

export async function viewPodTab(
  fx: Pdt3090Fx,
  rfdId: number,
  reminderId: number,
): Promise<{ status: number; rows: Pdt3090DpsRow[] }> {
  const params = loadCustomersQuery(fx, reminderId);
  const res = await fx.Request.get(
    `${fx.Endpoints.requestForDisconnection}/view-pod-tab/${rfdId}`,
    { params, timeout: PDT_3090_LOAD_PODS_TIMEOUT_MS },
  );
  await expect(res).CheckResponse();
  const status = res.status();
  expect(status, 'view-pod-tab HTTP 206 PARTIAL_CONTENT').toBe(206);
  const body = await res.json();
  return { status, rows: listingContent(body) as Pdt3090DpsRow[] };
}

export function rowsForPodIds(rows: Pdt3090DpsRow[], podIds: number[]): Pdt3090DpsRow[] {
  const wanted = new Set(podIds);
  return rows.filter((row) => {
    const id = nestedId(row.podId) ?? asNumber(row.podId);
    return wanted.has(id);
  });
}

export function rowPodId(row: Pdt3090DpsRow): number {
  return nestedId(row.podId) ?? asNumber(row.podId);
}

export function isHighestConsumptionFlag(row: Pdt3090DpsRow | undefined): boolean {
  return row?.isHighestConsumption === true;
}

export function isCheckedFlag(row: Pdt3090DpsRow | undefined): boolean {
  return row?.isChecked === true;
}

/** Phase 2 validity period — copy staging pattern, keep LIST_OF_CUSTOMERS. */
export function rfdBasePayload(
  fx: Pdt3090Fx,
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
  payload.allSelected = false;
  const validityPeriodFrom = todayYmd();
  payload.validityPeriodFrom = validityPeriodFrom;
  payload.validityPeriodTo = randomGens.generateMonthEndDate('yyyy-mm-dd');
  expect(payload.conditionType, 'never ALL_CUSTOMERS on Dev2').toBe('LIST_OF_CUSTOMERS');
  expect(payload.allSelected, 'never allSelected=true on Dev2').toBe(false);
  return payload;
}

export async function createDraftRfdWithHighestFilterOn(
  fx: Pdt3090Fx,
  reminderId: number,
): Promise<number> {
  const started = Date.now();
  const listOfCustomer = customerIdentifier(fx.Responses);
  const payload = rfdBasePayload(fx, reminderId, listOfCustomer);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.allSelected = false;
  payload.podWithHighestConsumption = true;
  payload.excludePodIds = [];
  payload.pods = [];
  const res = await fx.Request.post(fx.Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3090_RFD_POST_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  fx.Responses.requestForDisconnection.push(requestId);
  logElapsed(started, `POST DRAFT RFD ${requestId} highest-filter-on LIST_OF_CUSTOMERS`);
  return requestId;
}

export async function getCheckedPodIds(
  fx: Pdt3090Fx,
  reminderId: number,
  rfdId: number,
): Promise<number[]> {
  const gridOperatorId = Number(envVariables.grid_operator);
  const res = await fx.Request.get(
    `${fx.Endpoints.requestForDisconnection}/get-checked-pods/${reminderId}/${gridOperatorId}/${rfdId}`,
    { timeout: PDT_3090_RFD_POST_TIMEOUT_MS },
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  const rows = Array.isArray(body)
    ? body
    : Array.isArray((body as { content?: unknown }).content)
      ? (body as { content: unknown[] }).content
      : [];
  return rows
    .map((row) => nestedId((row as Record<string, unknown>).podId) ?? nestedId(row))
    .filter((id): id is number => typeof id === 'number' && id > 0);
}

async function postFifteenMinuteProfile(
  fx: Pdt3090Fx,
  pod: Pdt3090PodRef,
  value: number,
  period: Pdt3090ProfilePeriod = calendarMonthPeriod(0),
): Promise<number> {
  const payload = (await fx.GeneratePayload.energyData.profile15minute(pod.index, [
    {
      startDate: period.startYmd,
      endDate: period.endYmd,
      amount: value,
    },
  ])) as Record<string, unknown>;
  payload.timeZone = 'CET';
  payload.profileId = envVariables.profiles;
  payload.identifier = pod.podIdentifier;
  payload.warningAcceptedByUser = true;
  payload.periodFrom = period.periodFrom;
  payload.periodTo = period.periodTo;
  const res = await fx.Request.post(fx.Endpoints.profilesByMonth, { data: payload });
  await expect(res).CheckResponse();
  const bbpId = Number(await res.json());
  expect(bbpId, `FIFTEEN_MINUTES billing-by-profile id for ${pod.podIdentifier}`).toBeGreaterThan(0);
  await fx.GeneratePayload.energyData.uploadDataByProfileFile(bbpId, 'MIN');
  fx.Responses.dataByProfiles.push({
    id: bbpId,
    identifier: pod.podIdentifier,
    periodType: 'FIFTEEN_MINUTES',
    value,
  });
  return bbpId;
}

async function postOneMonthBillingByProfile(
  fx: Pdt3090Fx,
  pod: Pdt3090PodRef,
  value: number,
  period: Pdt3090ProfilePeriod = calendarMonthPeriod(0),
): Promise<{ ok: true; bbpId: number } | { ok: false; status: number }> {
  const payload = (await fx.GeneratePayload.energyData.profile1Month(pod.index)) as Record<
    string,
    unknown
  >;
  payload.timeZone = 'CET';
  payload.profileId = envVariables.profiles;
  payload.identifier = pod.podIdentifier;
  payload.periodType = 'ONE_MONTH';
  payload.warningAcceptedByUser = true;
  payload.periodFrom = period.periodFrom;
  payload.periodTo = period.periodTo;
  const entries = Array.isArray(payload.entries)
    ? (payload.entries as Array<Record<string, unknown>>)
    : [];
  if (entries.length > 0) {
    entries[0].value = value;
    entries[0].shiftedHour = false;
    entries[0].periodFrom = period.periodFrom;
  } else {
    payload.entries = [
      {
        periodFrom: period.periodFrom,
        value,
        shiftedHour: false,
      },
    ];
  }

  const res = await fx.Request.post(fx.Endpoints.profilesByMonth, { data: payload });
  if (!res.ok()) {
    return { ok: false, status: res.status() };
  }
  await expect(res).CheckResponse();
  const bbpId = Number(await res.json());
  expect(bbpId, `ONE_MONTH billing-by-profile id for ${pod.podIdentifier}`).toBeGreaterThan(0);
  fx.Responses.dataByProfiles.push({
    id: bbpId,
    identifier: pod.podIdentifier,
    periodType: 'ONE_MONTH',
    value,
  });
  return { ok: true, bbpId };
}

/**
 * TC-BE-2 default: ONE_MONTH BillingByProfileCreateRequest for POD Low (value=500) and
 * POD HighId (value=1). Parameterized for TC-BE-10 (100/100) and TC-BE-11 (custom month).
 * If ONE_MONTH is not 2xx on settlement PODs, fall back to profile15minute + MIN upload
 * for both (PDT-2937 / REG-962).
 */
export async function postBillingByProfileForPods(
  fx: Pdt3090Fx,
  low: Pdt3090PodRef,
  high: Pdt3090PodRef,
  values: Pdt3090ProfileValues,
  period: Pdt3090ProfilePeriod = calendarMonthPeriod(0),
): Promise<{
  lowBbpId: number;
  highBbpId: number;
  periodType: 'ONE_MONTH' | 'FIFTEEN_MINUTES';
}> {
  const lowMonth = await postOneMonthBillingByProfile(fx, low, values.lowValue, period);
  if (lowMonth.ok) {
    const highMonth = await postOneMonthBillingByProfile(fx, high, values.highValue, period);
    if (highMonth.ok) {
      return {
        lowBbpId: lowMonth.bbpId,
        highBbpId: highMonth.bbpId,
        periodType: 'ONE_MONTH',
      };
    }
    const highStatus = 'status' in highMonth ? highMonth.status : 'ok';
    console.log(
      `[PDT-3090] ONE_MONTH billing-by-profile HTTP ${highStatus} on HighId ${high.podIdentifier}; ` +
        `falling back to FIFTEEN_MINUTES + MIN upload for HighId (value=${values.highValue}).`,
    );
    const highBbpId = await postFifteenMinuteProfile(fx, high, values.highValue, period);
    return { lowBbpId: lowMonth.bbpId, highBbpId, periodType: 'ONE_MONTH' };
  }
  const lowStatus = 'status' in lowMonth ? lowMonth.status : 'ok';
  console.log(
    `[PDT-3090] ONE_MONTH billing-by-profile HTTP ${lowStatus} on settlement POD ${low.podIdentifier}; ` +
      `falling back to FIFTEEN_MINUTES + MIN upload for Low (${values.lowValue}) and HighId (${values.highValue}).`,
  );
  const lowBbpId = await postFifteenMinuteProfile(fx, low, values.lowValue, period);
  const highBbpId = await postFifteenMinuteProfile(fx, high, values.highValue, period);
  return { lowBbpId, highBbpId, periodType: 'FIFTEEN_MINUTES' };
}

export async function postBillingByProfileForLowAndHigh(
  fx: Pdt3090Fx,
  low: Pdt3090PodRef,
  high: Pdt3090PodRef,
): Promise<{
  lowBbpId: number;
  highBbpId: number;
  periodType: 'ONE_MONTH' | 'FIFTEEN_MINUTES';
}> {
  return postBillingByProfileForPods(fx, low, high, {
    lowValue: PDT_3090_PROFILE_LOW_VALUE,
    highValue: PDT_3090_PROFILE_HIGH_VALUE,
  });
}

export async function realizeForVolumesInvoice(
  fx: Pdt3090Fx,
  invoiceDateYmd?: string,
): Promise<number> {
  const billingPayload = await fx.GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  expect(billingPayload.billingType, 'BillingRunCreateRequest.billingType').toBe('STANDARD_BILLING');
  expect(billingPayload.basicParameters.applicationModelType).toEqual(['FOR_VOLUMES']);
  expect(billingPayload.basicParameters.billingCriteria).toBe('LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS');
  expect(billingPayload.basicParameters.billingApplicationLevel).toBe('CONTRACT');
  if (invoiceDateYmd) {
    billingPayload.commonParameters.invoiceDate = invoiceDateYmd;
    billingPayload.commonParameters.taxEventDate = invoiceDateYmd;
  }
  const billingRun = await fx.Request.post(fx.Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  fx.Responses.billingRun.push(await billingRun.json());
  const billingIndex = fx.Responses.billingRun.length - 1;
  await fx.GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, billingIndex);
  const invoiceId = entityId(fx.Responses.invoice[fx.Responses.invoice.length - 1]);
  expect(invoiceId, 'realized FOR_VOLUMES invoice id').toBeGreaterThan(0);
  return invoiceId;
}

/**
 * Realize FOR_VOLUMES. If ONE_MONTH waitForInvoiceGeneration fails on settlement
 * PODs, post FIFTEEN_MINUTES for the same Low/High values and run a second billing run.
 */
export async function realizeForVolumesInvoiceWithSettlementFallback(
  fx: Pdt3090Fx,
  low: Pdt3090PodRef,
  high: Pdt3090PodRef,
  periodType: 'ONE_MONTH' | 'FIFTEEN_MINUTES',
  opts?: Pdt3090ProfileValues & { invoiceDateYmd?: string; period?: Pdt3090ProfilePeriod },
): Promise<{ invoiceId: number; periodType: 'ONE_MONTH' | 'FIFTEEN_MINUTES' }> {
  const lowValue = opts?.lowValue ?? PDT_3090_PROFILE_LOW_VALUE;
  const highValue = opts?.highValue ?? PDT_3090_PROFILE_HIGH_VALUE;
  const period = opts?.period ?? calendarMonthPeriod(0);
  try {
    const invoiceId = await realizeForVolumesInvoice(fx, opts?.invoiceDateYmd);
    return { invoiceId, periodType };
  } catch (err) {
    if (periodType !== 'ONE_MONTH') throw err;
    console.log(
      `[PDT-3090] waitForInvoiceGeneration failed after ONE_MONTH profile (${String(err).slice(0, 240)}). ` +
        `Not posting FIFTEEN_MINUTES for the same POD/period (Dev2 rejects mixed dimensions).`,
    );
    throw err;
  }
}

export async function sumInvoiceTotalVolumesByPod(
  fx: Pdt3090Fx,
  invoiceId: number,
  podIdentifier: string,
): Promise<number> {
  const res = await fx.Request.get(
    `${fx.Endpoints.invoice}/detailed-data?id=${invoiceId}&page=0&size=100`,
  );
  await expect(res).CheckResponse();
  const rows = listingContent(await res.json()) as Array<{
    pointOfDelivery?: unknown;
    totalVolumes?: unknown;
  }>;
  const needle = podIdentifier.trim();
  return rows
    .filter((row) => String(row.pointOfDelivery ?? '').trim() === needle)
    .reduce((sum, row) => sum + asNumber(row.totalVolumes), 0);
}

export async function makeInvoiceLiabilityOverdue(
  fx: Pdt3090Fx,
  invoiceId: number,
): Promise<number> {
  const invRes = await fx.Request.get(`${fx.Endpoints.invoice}?id=${invoiceId}`);
  await expect(invRes).CheckResponse();
  const invoice = (await invRes.json()) as {
    liabilitiesAndReceivables?: Array<{ id?: unknown }>;
  };
  let liabilityId = nestedId(invoice.liabilitiesAndReceivables?.[0]?.id);
  if (!liabilityId) {
    const identifier = customerIdentifier(fx.Responses);
    const listRes = await fx.Request.get(
      `${fx.Endpoints.customerLiability}/list?page=0&size=1&columns=ID&direction=DESC&prompt=${encodeURIComponent(identifier)}&searchFields=CUSTOMER`,
    );
    await expect(listRes).CheckResponse();
    const rows = listingContent(await listRes.json());
    expect(rows.length, 'customer-liability list fallback after invoice GET').toBeGreaterThan(0);
    liabilityId = nestedId(rows[0]) ?? asNumber((rows[0] as { id?: unknown }).id);
  }
  expect(liabilityId, 'invoice liability id for due-date-change').toBeGreaterThan(0);

  const yesterday = randomGens.generateYesterdaysDate('yyyy-mm-dd');
  const change = await fx.Request.put(
    `${fx.Endpoints.customerLiability}/${liabilityId}/due-date-change?dueDate=${yesterday}`,
  );
  await expect(change).CheckResponse();
  fx.Responses.customerLiability.push(liabilityId);

  const liab = await getLiability(fx, liabilityId);
  expect(asNumber(liab.currentAmount), 'overdue invoice liability currentAmount > 0').toBeGreaterThan(0);
  const due = String(liab.dueDate ?? '');
  const dmy = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  expect(
    due.includes(yesterday) || due.includes(dmy),
    `GET customer-liability dueDate should be yesterday (got ${due})`,
  ).toBe(true);
  return liabilityId;
}

export async function addAndActivateExtraPodsOnSameBillingGroupAt(
  fx: Pdt3090Fx,
  extraCount: number,
  activationYmd: string,
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
      basicParameters?: { entryInForceDate?: unknown };
    };
    const existingEif = String(existing.basicParameters?.entryInForceDate ?? '');
    const eifForPut =
      existingEif.length > 0 && existingEif <= todayYmd() ? existingEif : todayYmd();
    (editPayload as { basicParameters: { entryInForceDate: string } }).basicParameters.entryInForceDate =
      eifForPut;

    const editRes = await Request.put(
      `product-contract/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: editPayload },
    );
    await expect(editRes).CheckResponse();

    const act = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(
        podIndex,
        activationYmd,
        undefined,
        0,
      ),
    });
    await expect(act).CheckResponse();
  }
  expect(
    Responses.pod.length,
    `expected ${1 + extraCount} PODs on the contract after extras`,
  ).toBeGreaterThanOrEqual(1 + extraCount);
  return addedIds;
}

export async function resolveProductContractBillingGroupId(fx: Pdt3090Fx): Promise<number> {
  const contractId = entityId(fx.Responses.productContract[0]);
  const contractGet = await fx.Request.get(`${fx.Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const billingGroupId = resolveContractBillingGroupId(
    (await contractGet.json()) as Record<string, unknown>,
  );
  expect(billingGroupId, 'contract billingGroupId').toBeGreaterThan(0);
  return billingGroupId;
}

export async function readSelectedDisconnectionTypes(fx: Pdt3090Fx): Promise<string[]> {
  const res = await fx.Request.get('system-configurations');
  await expect(res).CheckResponse();
  const body = (await res.json()) as { liabilitiesForDisconnection?: unknown };
  return Array.isArray(body.liabilitiesForDisconnection)
    ? body.liabilitiesForDisconnection.map((row) => String(row))
    : [];
}

export async function getCustomerView(fx: Pdt3090Fx): Promise<Record<string, unknown>> {
  const customerId = entityId(fx.Responses.customer[0]);
  const res = await fx.Request.get(`${fx.Endpoints.customer}/${customerId}?version=1`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function customerDetailIdFromView(view: Record<string, unknown>): number {
  const id =
    asNumber(view.lastCustomerDetailId) ||
    nestedId((view.customerDetails as unknown[])?.[0]) ||
    asNumber(view.customerDetailsId);
  expect(id, 'customerDetailId').toBeGreaterThan(0);
  return id;
}

export function customerCommunicationIdFromView(view: Record<string, unknown>): number {
  const rows = Array.isArray(view.communicationData)
    ? (view.communicationData as Record<string, unknown>[])
    : [];
  const email = rows.find((row) => String(row.type ?? row.communicationType ?? '') === 'EMAIL');
  const id = nestedId(email) ?? nestedId(rows[0]);
  expect(id, 'customerCommunicationDataId').toBeGreaterThan(0);
  return id!;
}

export async function createYearlyInterestRate(fx: Pdt3090Fx): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.dailyInterestRate() as Record<
    string,
    unknown
  >;
  payload.type = 'YEARLY';
  payload.charging = 'OVERDUE_LIABILITY';
  payload.isDefault = false;
  payload.grouping = false;
  const res = await fx.Request.post(fx.Endpoints.interestRate, { data: payload });
  await expect(res).CheckResponse();
  const body = await res.json();
  fx.Responses.interestRate.push(body);
  const interestRateId = entityId(body);
  expect(interestRateId, 'POST /interest-rate id').toBeGreaterThan(0);
  return interestRateId;
}

export async function ensurePaymentChannelAndPackage(fx: Pdt3090Fx): Promise<void> {
  if (!fx.Responses.collectionChannel.length) {
    const channelRes = await fx.Request.post(fx.Endpoints.collectionChannel, {
      data: fx.GeneratePayload.receivablesManagement.collection_channel(),
    });
    await expect(channelRes).CheckResponse();
    fx.Responses.collectionChannel.push(await channelRes.json());
  }
  if (!fx.Responses.paymentPackage.length) {
    const packagePayload = fx.GeneratePayload.receivablesManagement.payment_package();
    packagePayload.channelId = entityId(fx.Responses.collectionChannel[0]);
    const packageRes = await fx.Request.post(fx.Endpoints.paymentPackage, {
      data: packagePayload,
    });
    await expect(packageRes).CheckResponse();
    fx.Responses.paymentPackage.push(await packageRes.json());
  }
}

export async function createManualLiabilityOnBillingGroup(
  fx: Pdt3090Fx,
  opts: {
    initialAmount: number;
    dueDateDmy: string;
    occurrenceDateDmy: string;
    billingGroupId: number;
    interestRateId?: number;
    outgoingDocumentFromExternalSystem?: string;
  },
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.customer_liability() as Record<
    string,
    unknown
  >;
  payload.billingGroupId = opts.billingGroupId;
  payload.initialAmount = opts.initialAmount;
  payload.currentAmount = opts.initialAmount;
  payload.dueDate = opts.dueDateDmy;
  payload.occurrenceDate = opts.occurrenceDateDmy;
  if (opts.interestRateId) payload.applicableInterestRateId = opts.interestRateId;
  if (opts.outgoingDocumentFromExternalSystem) {
    payload.outgoingDocumentFromExternalSystem = opts.outgoingDocumentFromExternalSystem;
  }
  const post = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  await expect(post).CheckResponse();
  const liabilityId = entityId(await post.json());
  fx.Responses.customerLiability.push(liabilityId);
  const liab = await getLiability(fx, liabilityId);
  expect(asNumber(liab.currentAmount), 'manual liability currentAmount > 0').toBeGreaterThan(0);
  expect(nestedId(liab.billingGroupResponse), 'GET liability billingGroupResponse.id').toBe(
    opts.billingGroupId,
  );
  return liabilityId;
}

export async function changeLiabilityDueDateYesterday(
  fx: Pdt3090Fx,
  liabilityId: number,
): Promise<void> {
  const yesterday = randomGens.generateYesterdaysDate('yyyy-mm-dd');
  const change = await fx.Request.put(
    `${fx.Endpoints.customerLiability}/${liabilityId}/due-date-change?dueDate=${yesterday}`,
  );
  await expect(change).CheckResponse();
}

export async function payLiabilityAmount(fx: Pdt3090Fx, amount: number): Promise<number> {
  await ensurePaymentChannelAndPackage(fx);
  const payload = await fx.GeneratePayload.receivablesManagement.payment();
  payload.initialAmount = amount;
  payload.customerId = entityId(fx.Responses.customer[0]);
  payload.collectionChannelId = entityId(fx.Responses.collectionChannel[0]);
  payload.paymentPackageId = entityId(fx.Responses.paymentPackage[0]);
  const paymentRes = await fx.Request.post(fx.Endpoints.payment, { data: payload });
  await expect(paymentRes).CheckResponse();
  const paymentId = entityId(await paymentRes.json());
  fx.Responses.payment.push(paymentId);
  return paymentId;
}

export async function pollLiabilityCurrentAmount(
  fx: Pdt3090Fx,
  liabilityId: number,
  expected: number,
  message: string,
): Promise<void> {
  await expect
    .poll(
      async () => {
        const liab = await getLiability(fx, liabilityId);
        return asNumber(liab.currentAmount);
      },
      { message, timeout: PDT_3090_PAYMENT_POLL_MS, intervals: [2000, 5000] },
    )
    .toBe(expected);
}

/**
 * PDT-2459 LPF job after paid overdue, but source liability MUST have billingGroupId
 * (createPdt2459OverdueLiability does not set it). Filter LPF list by this UIC only.
 * Do not POST reminder /job.
 */
export async function preparePdt3090LpfOnlyLiability(fx: Pdt3090Fx): Promise<{
  sourceLiabilityId: number;
  lpfId: number;
  lpfLiabilityId: number;
  billingGroupId: number;
}> {
  const types = await readSelectedDisconnectionTypes(fx);
  console.log(`[PDT-3090] liabilitiesForDisconnection=${types.join(',') || 'empty'}`);
  const billingGroupId = await resolveProductContractBillingGroupId(fx);
  const interestRateId = await createYearlyInterestRate(fx);
  const weekBefore = randomGens.generateOneWeekBeforeDate('dd-mm-yyyy');
  const sourceLiabilityId = await createManualLiabilityOnBillingGroup(fx, {
    initialAmount: PDT_3090_LPF_SOURCE_AMOUNT,
    dueDateDmy: weekBefore,
    occurrenceDateDmy: weekBefore,
    billingGroupId,
    interestRateId,
  });

  await payLiabilityAmount(fx, PDT_3090_LPF_SOURCE_AMOUNT);
  await pollLiabilityCurrentAmount(
    fx,
    sourceLiabilityId,
    0,
    'PDT-3090 source liability not fully offset before LPF job',
  );
  await changeLiabilityDueDateYesterday(fx, sourceLiabilityId);

  const lpfRecord = await waitForPdt2459LpfGeneration(fx as never, sourceLiabilityId);
  const lpfId = entityId(lpfRecord);
  const lpfGet = await fx.Request.get(`${fx.Endpoints.latePaymentFine}/${lpfId}`);
  await expect(lpfGet).CheckResponse();
  const lpfBody = (await lpfGet.json()) as { amount?: unknown };
  expect(asNumber(lpfBody.amount), 'LPF amount > 0').toBeGreaterThan(0);

  const identifier = customerIdentifier(fx.Responses);
  const lpfLiabilityId = await findPdt2459LpfLiabilityId(fx as never, identifier, lpfId);
  expect(lpfLiabilityId, `LPF customer liability for latePaymentFine ${lpfId}`).toBeGreaterThan(0);
  fx.Responses.customerLiability.push(lpfLiabilityId);

  const lpfLiab = await getLiability(fx, lpfLiabilityId);
  expect(asNumber(lpfLiab.currentAmount), 'LPF liability currentAmount > 0').toBeGreaterThan(0);
  expect(
    nestedId(lpfLiab.billingGroupResponse),
    'LPF liability contract_billing_group_id is required for reminder/Load PODs (no LPF parent-BG exception in reminder SQL)',
  ).toBe(billingGroupId);
  const due = String(lpfLiab.dueDate ?? '');
  const yesterdayYmd = randomGens.generateYesterdaysDate('yyyy-mm-dd');
  const yesterdayDmy = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  const dueIsPast = due.includes(yesterdayYmd) || due.includes(yesterdayDmy);
  if (!dueIsPast) {
    await changeLiabilityDueDateYesterday(fx, lpfLiabilityId);
    const after = await getLiability(fx, lpfLiabilityId);
    expect(asNumber(after.currentAmount), 'overdue LPF liability currentAmount > 0').toBeGreaterThan(
      0,
    );
  }

  return { sourceLiabilityId, lpfId, lpfLiabilityId, billingGroupId };
}

export async function createExternalOutgoingDocumentLiability(fx: Pdt3090Fx): Promise<{
  liabilityId: number;
  billingGroupId: number;
}> {
  const billingGroupId = await resolveProductContractBillingGroupId(fx);
  const yesterday = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  const liabilityId = await createManualLiabilityOnBillingGroup(fx, {
    initialAmount: 100,
    dueDateDmy: yesterday,
    occurrenceDateDmy: yesterday,
    billingGroupId,
    outgoingDocumentFromExternalSystem: PDT_3090_EXTERNAL_OUTGOING_DOC,
  });
  const liab = await getLiability(fx, liabilityId);
  expect(String(liab.outgoingDocumentFromExternalSystem)).toBe(PDT_3090_EXTERNAL_OUTGOING_DOC);
  expect(nestedId(liab.invoiceResponse) ?? 0, 'external liability must not have invoiceId').toBe(0);
  return { liabilityId, billingGroupId };
}

export async function createExecutedReschedulingForSourceLiability(
  fx: Pdt3090Fx,
): Promise<{
  sourceLiabilityId: number;
  reschedulingId: number;
  instalmentLiabilityId: number;
  billingGroupId: number;
}> {
  const types = await readSelectedDisconnectionTypes(fx);
  console.log(`[PDT-3090] liabilitiesForDisconnection=${types.join(',') || 'empty'}`);
  const billingGroupId = await resolveProductContractBillingGroupId(fx);
  const interestRateId = await createYearlyInterestRate(fx);
  const weekBefore = randomGens.generateOneWeekBeforeDate('dd-mm-yyyy');
  const sourceLiabilityId = await createManualLiabilityOnBillingGroup(fx, {
    initialAmount: 100,
    dueDateDmy: weekBefore,
    occurrenceDateDmy: weekBefore,
    billingGroupId,
    interestRateId,
  });
  const sourceGet = await getLiability(fx, sourceLiabilityId);
  expect(
    nestedId(sourceGet.billingGroupResponse),
    'source liability billingGroupResponse.id must not be null (Confluence 77136161)',
  ).toBe(billingGroupId);

  const assessment = await fx.Request.post(fx.Endpoints.customerAssessment, {
    data: fx.GeneratePayload.receivablesManagement.customer_assessment(),
  });
  await expect(assessment).CheckResponse();
  const assessmentBody = await assessment.json();
  fx.Responses.customerAssessment.push(assessmentBody);
  const customerAssessmentId = entityId(assessmentBody);

  const calcPayload = CalculateRescheduling();
  calcPayload.reschedulingInterestType = 'INTEREST_WITH_THE_FIRST_INSTALLMENT';
  calcPayload.installmentCount = '2';
  calcPayload.installmentCurrencyId = Number(envVariables.currency);
  calcPayload.installmentDate = todayYmd();
  calcPayload.liabilityIds = [sourceLiabilityId];
  const calcRes = await fx.Request.post(fx.Endpoints.calculateRescheduling, { data: calcPayload });
  await expect(calcRes).CheckResponse();
  const calcBody = (await calcRes.json()) as {
    installments?: unknown[];
    lpfs?: Array<Record<string, unknown>>;
  };

  const customerView = await getCustomerView(fx);
  const payload = reschedulingPayloadTemplate() as Record<string, unknown>;
  payload.currencyId = Number(envVariables.currency);
  payload.customerId = entityId(fx.Responses.customer[0]);
  payload.customerDetailId = customerDetailIdFromView(customerView);
  const communicationId = customerCommunicationIdFromView(customerView);
  payload.customerCommunicationDataId = communicationId;
  payload.customerCommunicationDataIdForContract = communicationId;
  payload.customerAssessmentId = customerAssessmentId;
  payload.interestRateForInstalmentsId = interestRateId;
  payload.liabilityIdsForRescheduling = [sourceLiabilityId];
  payload.reschedulingInterestType = 'INTEREST_WITH_THE_FIRST_INSTALLMENT';
  payload.reschedulingStatus = 'EXECUTED';
  payload.numberOfInstallment = '2';
  payload.installmentDueDayOfTheMonth = Math.min(28, Math.max(1, new Date().getDate()));
  payload.installments = calcBody.installments ?? [];
  const lpfs = Array.isArray(calcBody.lpfs) ? calcBody.lpfs : [];
  payload.reschedulingLpfs = lpfs.length
    ? lpfs.map((row) => ({
        id: sourceLiabilityId,
        interest_default_currency: row.interest_default_currency ?? envVariables.currency,
        lpf_data: row.lpf_data ?? { results: [] },
      }))
    : [];
  payload.templateIds = [envVariables.rescheduling_document_template];
  payload.templateRequests = [
    {
      templateId: envVariables.rescheduling_document_template,
      signings: ['NO'],
      outputFileFormat: ['DOCX', 'PDF'],
    },
  ];
  expect(Array.isArray(payload.installments) && (payload.installments as unknown[]).length > 0).toBe(
    true,
  );

  const post = await fx.Request.post(fx.Endpoints.rescheduling, { data: payload });
  await expect(post).CheckResponse();
  const reschedulingId = entityId(await post.json());
  fx.Responses.rescheduling.push(reschedulingId);

  const getRes = await fx.Request.get(`${fx.Endpoints.rescheduling}/${reschedulingId}`);
  await expect(getRes).CheckResponse();
  const saved = (await getRes.json()) as { reschedulingStatus?: string };
  expect(saved.reschedulingStatus, 'reschedulingStatus').toBe('EXECUTED');

  const identifier = customerIdentifier(fx.Responses);
  const listRes = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=25&prompt=${encodeURIComponent(identifier)}&searchFields=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  const rows = listingContent(await listRes.json());
  let instalmentLiabilityId = 0;
  for (const row of rows) {
    const id = nestedId(row) ?? asNumber((row as { id?: unknown }).id);
    if (!id || id === sourceLiabilityId) continue;
    const full = await getLiability(fx, id);
    const docType = String(full.outgoingDocumentType ?? '');
    if (docType === 'RESCHEDULING' && asNumber(full.currentAmount) > 0) {
      instalmentLiabilityId = id;
      break;
    }
  }
  expect(
    instalmentLiabilityId,
    'rescheduling instalment liability with currentAmount > 0',
  ).toBeGreaterThan(0);
  fx.Responses.customerLiability.push(instalmentLiabilityId);
  await changeLiabilityDueDateYesterday(fx, instalmentLiabilityId);
  const instalment = await getLiability(fx, instalmentLiabilityId);
  expect(asNumber(instalment.currentAmount), 'instalment currentAmount > 0').toBeGreaterThan(0);
  expect(nestedId(instalment.billingGroupResponse), 'instalment billingGroupResponse.id').toBe(
    billingGroupId,
  );

  return { sourceLiabilityId, reschedulingId, instalmentLiabilityId, billingGroupId };
}

export async function createExecutedRfdWithHighestFilterOn(
  fx: Pdt3090Fx,
  reminderId: number,
): Promise<number> {
  const listOfCustomer = customerIdentifier(fx.Responses);
  const payload = rfdBasePayload(fx, reminderId, listOfCustomer);
  payload.disconnectionRequestsStatus = 'EXECUTED';
  payload.allSelected = false;
  payload.podWithHighestConsumption = true;
  payload.excludePodIds = [];
  payload.pods = [];
  const templateId = Number(envVariables.request_disconnection_document_template);
  if (Number.isFinite(templateId) && templateId > 0) {
    payload.templateIds = [templateId];
  }
  const res = await fx.Request.post(fx.Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3090_RFD_POST_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  fx.Responses.requestForDisconnection.push(requestId);
  logElapsed(Date.now(), `POST EXECUTED RFD ${requestId} highest-filter-on LIST_OF_CUSTOMERS`);
  return requestId;
}

export async function listActiveGridOperatorTaxId(fx: Pdt3090Fx): Promise<number> {
  const types = await readSelectedDisconnectionTypes(fx);
  console.log(`[PDT-3090] liabilitiesForDisconnection=${types.join(',') || 'empty'}`);
  const taxId = await postDedicatedGridTax(fx, {
    costCenterControllingOrder: `PDT3090-${Date.now()}`,
  });
  expect(taxId, 'POST /tax-for-the-grid-operator dedicated id').toBeGreaterThan(0);
  return taxId;
}

export async function postExecutedDpsForPod(
  fx: Pdt3090Fx,
  rfdId: number,
  podId: number,
  gridOperatorTaxesId: number,
): Promise<number> {
  const postOnce = async (targetPodId: number) => {
    const payload = fx.GeneratePayload.receivablesManagement.disconnectionOfPowerSupply();
    payload.requestForDisconnectionId = rfdId;
    payload.saveType = 'EXECUTED';
    payload.disconnectedRequest[0].customerId = entityId(fx.Responses.customer[0]);
    payload.disconnectedRequest[0].podId = targetPodId;
    payload.disconnectedRequest[0].gridOperatorTaxesId = gridOperatorTaxesId;
    payload.disconnectedRequest[0].expressReconnection = false;
    payload.disconnectedRequest[0].dateOfDisconnection = todayYmd();
    return fx.Request.post(fx.Endpoints.disconnectionOfPowerSupply, { data: payload });
  };
  let res = await postOnce(podId);
  const firstPodId = entityId(fx.Responses.pod[0]);
  if (!res.ok() && firstPodId > 0 && firstPodId !== podId) {
    console.log(
      `[PDT-3090] DPS HTTP ${res.status()} for podId=${podId} taxId=${gridOperatorTaxesId}; retrying first POD ${firstPodId}`,
    );
    res = await postOnce(firstPodId);
  }
  await expect(res).CheckResponse();
  const dpsId = entityId(await res.json());
  fx.Responses.disconnectionOfPowerSupply.push(dpsId);
  expect(dpsId, 'POST /disconnection-of-power-supply id').toBeGreaterThan(0);
  return dpsId;
}

export async function findReconnectionTaxLiability(
  fx: Pdt3090Fx,
  excludeLiabilityId: number,
): Promise<number> {
  const invoices = invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION');
  expect(
    invoices.length,
    'reconnection-tax invoice never appeared after POST /disconnection-of-power-supply — TC-BE-7 cannot continue (CustomerLiabilitiesOutgoingDocType has no TAX; reconnection invoice is the only verified tax-like liability)',
  ).toBeGreaterThan(0);
  const invoiceId = entityId(invoices[0]);
  const fromInvoice = await findLiabilityByInvoiceId(fx, invoiceId);
  if (fromInvoice) {
    const id = nestedId(fromInvoice) ?? asNumber(fromInvoice.id);
    if (id && id !== excludeLiabilityId) return id;
  }

  const identifier = customerIdentifier(fx.Responses);
  const listRes = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=25&prompt=${encodeURIComponent(identifier)}&searchFields=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  for (const row of listingContent(await listRes.json())) {
    const id = nestedId(row) ?? asNumber((row as { id?: unknown }).id);
    if (!id || id === excludeLiabilityId) continue;
    const full = await getLiability(fx, id);
    const invoiceType = String(
      (full.invoiceResponse as { invoiceType?: unknown } | undefined)?.invoiceType ?? '',
    );
    const docType = String(full.outgoingDocumentType ?? '');
    if (invoiceType === 'RECONNECTION' || docType.includes('RECONNECTION')) {
      return id;
    }
  }
  expect(
    false,
    'reconnection-tax customer liability never appeared after DPS EXECUTED — fail vs PDT-3090 / Create 72155868 (do not skip)',
  ).toBe(true);
  return 0;
}

export async function payOffLiabilityFully(fx: Pdt3090Fx, liabilityId: number): Promise<void> {
  const before = await getLiability(fx, liabilityId);
  const amount = asNumber(before.currentAmount);
  if (amount <= 0) return;
  await payLiabilityAmount(fx, amount);
  await pollLiabilityCurrentAmount(
    fx,
    liabilityId,
    0,
    `liability ${liabilityId} not fully offset`,
  );
}

export async function resolveActionTypeId(fx: Pdt3090Fx): Promise<number> {
  const fromEnv = Number(envVariables.action_type);
  if (Number.isFinite(fromEnv) && fromEnv > 0) return fromEnv;
  const res = await fx.Request.get('action-types?statuses=ACTIVE&page=0&size=25');
  await expect(res).CheckResponse();
  const rows = listingContent(await res.json());
  expect(rows.length, 'ACTIVE action-types').toBeGreaterThan(0);
  return nestedId(rows[0]) ?? asNumber((rows[0] as { id?: unknown }).id);
}

export async function postClaimedPenaltyWithBillingGroup(
  fx: Pdt3090Fx,
  billingGroupId: number,
  amount = PDT_3090_CLAIMED_PENALTY_AMOUNT,
): Promise<number> {
  const view = await getCustomerView(fx);
  const payload: Record<string, unknown> = {
    amount,
    costCenter: String(
      envVariables.cost_center ?? envVariables.controlling_order ?? '1',
    ),
    currencyId: Number(envVariables.currency),
    customerDetailId: customerDetailIdFromView(view),
    dueDate: randomGens.generateYesterdaysDate('yyyy-mm-dd'),
    incomeAccountNumber: String(envVariables.income_account_number ?? '1'),
    billingGroupId,
  };
  const docTemplate = Number(envVariables.penalty_document_template);
  const emailTemplate = Number(envVariables.penalty_email_template);
  if (Number.isFinite(docTemplate) && docTemplate > 0) payload.documentTemplateId = docTemplate;
  if (Number.isFinite(emailTemplate) && emailTemplate > 0) payload.emailTemplateId = emailTemplate;
  const res = await fx.Request.post('claimed-penalty', { data: payload });
  await expect(res).CheckResponse();
  const penaltyId = entityId(await res.json());
  expect(penaltyId, 'POST /claimed-penalty id').toBeGreaterThan(0);
  return penaltyId;
}

export async function findClaimedPenaltyLiabilityId(
  fx: Pdt3090Fx,
  billingGroupId?: number,
): Promise<number> {
  const identifier = customerIdentifier(fx.Responses);
  const listRes = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=25&prompt=${encodeURIComponent(identifier)}&searchFields=CUSTOMER`,
  );
  await expect(listRes).CheckResponse();
  let fallbackId = 0;
  for (const row of listingContent(await listRes.json())) {
    const id = nestedId(row) ?? asNumber((row as { id?: unknown }).id);
    if (!id) continue;
    const full = await getLiability(fx, id);
    const docType = String(full.outgoingDocumentType ?? '');
    if (docType !== 'CLAIMED_PENALTY' && docType !== 'ACTION') continue;
    if (billingGroupId && nestedId(full.billingGroupResponse) === billingGroupId) return id;
    if (!fallbackId) fallbackId = id;
  }
  return fallbackId;
}

export async function postPenaltyActionAndClaim(
  fx: Pdt3090Fx,
  billingGroupId: number,
): Promise<{ actionId: number; penaltyLiabilityId: number }> {
  const types = await readSelectedDisconnectionTypes(fx);
  console.log(`[PDT-3090] liabilitiesForDisconnection=${types.join(',') || 'empty'}`);
  const actionTypeId = await resolveActionTypeId(fx);
  const penaltyPayload = fx.GeneratePayload.productAndServices.penalty() as Record<string, unknown>;
  const existingTypes = Array.isArray(penaltyPayload.actionTypeList)
    ? (penaltyPayload.actionTypeList as number[])
    : [];
  if (!existingTypes.includes(actionTypeId)) {
    penaltyPayload.actionTypeList = [...existingTypes, actionTypeId];
  }
  const penaltyRes = await fx.Request.post(fx.Endpoints.penalty, { data: penaltyPayload });
  await expect(penaltyRes).CheckResponse();
  const penaltyBody = await penaltyRes.json();
  fx.Responses.penalty.push(penaltyBody);
  const penaltyCatalogId = entityId(penaltyBody);

  const podIds = fx.Responses.pod.map((pod) => entityId(pod)).filter((id) => id > 0);
  expect(podIds.length, 'action pods[] int64 ids (Swagger ActionRequest.pods)').toBeGreaterThan(0);

  const noticeReceivingDate = todayYmd();
  const executionDate = plusCalendarDaysYmd(14);
  const actionPayload: Record<string, unknown> = {
    actionTypeId,
    noticeReceivingDate,
    executionDate,
    penaltyClaimAmount: PDT_3090_CLAIMED_PENALTY_AMOUNT,
    penaltyClaimAmountCurrencyId: Number(envVariables.currency),
    penaltyPayer: 'CUSTOMER',
    dontAllowAutomaticPenaltyClaim: false,
    withoutPenalty: false,
    penaltyId: penaltyCatalogId,
    customerId: entityId(fx.Responses.customer[0]),
    contractId: entityId(fx.Responses.productContract[0]),
    contractType: 'PRODUCT_CONTRACT',
    files: [],
    pods: podIds,
    templateId: envVariables.penalty_document_template,
    emailTemplateId: envVariables.penalty_email_template,
  };
  let actionRes = await fx.Request.post(fx.Endpoints.action, { data: actionPayload });
  if (!actionRes.ok()) {
    const text = await actionRes.text();
    if (/Pods are not allowed/i.test(text)) {
      actionPayload.pods = [];
      actionRes = await fx.Request.post(fx.Endpoints.action, { data: actionPayload });
    }
  }
  let actionId = 0;
  if (actionRes.ok()) {
    await expect(actionRes).CheckResponse();
    const actionRaw = await actionRes.json();
    actionId = entityId(actionRaw);
    fx.Responses.action.push(typeof actionRaw === 'object' ? actionRaw : { id: actionId });
    const claimRes = await fx.Request.post(`${fx.Endpoints.action}/${actionId}/penalty/claim`);
    if (claimRes.ok()) {
      await expect(claimRes).CheckResponse();
    } else {
      console.log(
        `[PDT-3090] POST /actions/${actionId}/penalty/claim HTTP ${claimRes.status()}; ` +
          `falling back to POST /claimed-penalty with billingGroupId=${billingGroupId}.`,
      );
      await postClaimedPenaltyWithBillingGroup(fx, billingGroupId);
    }
  } else {
    const text = await actionRes.text();
    console.log(
      `[PDT-3090] POST /actions HTTP ${actionRes.status()} ${text.slice(0, 400)}; ` +
        `falling back to POST /claimed-penalty with billingGroupId=${billingGroupId}.`,
    );
    await postClaimedPenaltyWithBillingGroup(fx, billingGroupId);
  }

  let penaltyLiabilityId = await findClaimedPenaltyLiabilityId(fx, billingGroupId);
  let liab = penaltyLiabilityId ? await getLiability(fx, penaltyLiabilityId) : null;
  if (!penaltyLiabilityId || !nestedId(liab?.billingGroupResponse)) {
    console.log(
      `[PDT-3090] claimed-penalty liability missing or billingGroupId null; POST /claimed-penalty with billingGroupId=${billingGroupId}.`,
    );
    await postClaimedPenaltyWithBillingGroup(fx, billingGroupId);
    penaltyLiabilityId = await findClaimedPenaltyLiabilityId(fx, billingGroupId);
    liab = penaltyLiabilityId ? await getLiability(fx, penaltyLiabilityId) : null;
  }
  expect(
    penaltyLiabilityId,
    'CLAIMED_PENALTY / ACTION customer liability after action claim or POST /claimed-penalty',
  ).toBeGreaterThan(0);
  fx.Responses.customerLiability.push(penaltyLiabilityId);
  expect(nestedId(liab?.billingGroupResponse), 'penalty liability billingGroupResponse.id').toBe(
    billingGroupId,
  );
  if (asNumber(liab?.currentAmount) <= 0) {
    expect(false, `penalty liability ${penaltyLiabilityId} currentAmount must be > 0`).toBe(true);
  }
  const due = String(liab?.dueDate ?? '');
  const yesterdayYmd = randomGens.generateYesterdaysDate('yyyy-mm-dd');
  const yesterdayDmy = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  if (!(due.includes(yesterdayYmd) || due.includes(yesterdayDmy))) {
    await changeLiabilityDueDateYesterday(fx, penaltyLiabilityId);
  }
  const after = await getLiability(fx, penaltyLiabilityId);
  expect(asNumber(after.currentAmount), 'overdue penalty liability currentAmount > 0').toBeGreaterThan(
    0,
  );
  return { actionId, penaltyLiabilityId };
}

export {
  addAndActivateExtraPodsOnSameBillingGroup,
  createOverdueManualLiabilityWithBillingGroup,
  createSupplyChain,
  customerIdentifier,
  entityId,
  monthStartYmd,
  todayYmd,
};
