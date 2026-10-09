import { expect } from '@playwright/test';
import type { APIRequestContext } from '@playwright/test';
import { randomGens } from '../../utils/randomGens';
import { Endpoints } from '../../fixtures/constants/endpoints';
import { manualInvoice } from '../../jsons/payloads/create/billing/manualInvoice';

export type Pdt2971Fx = {
  Request: APIRequestContext;
  GeneratePayload: any;
  Responses: any;
  Endpoints: typeof Endpoints;
};

export type PodTabRow = {
  podIdentifier?: string;
  podId?: number;
  customerId?: number;
  customers?: string;
  contracts?: string;
  liabilitiesInPod?: string;
  liabilitiesInBillingGroup?: string;
};

export type ReminderSecondTabRow = {
  customer?: string;
  liabilities?: string;
  sumOfLiabilities?: string;
  id?: number;
};

export type CustomersForDpsQuery = {
  page: number;
  size: number;
  conditionType: 'ALL_CUSTOMERS' | 'LIST_OF_CUSTOMERS';
  powerSupplyDisconnectionReminderId: number;
  gridOperatorId: number;
  listOfCustomer?: string;
  searchBy?: string;
  prompt?: string;
};

export const CUSTOMER_A_INDEX = 0;
export const CUSTOMER_B_INDEX = 1;
export const CONTRACT_A_INDEX = 0;
export const CONTRACT_B_INDEX = 1;

export function isoDate(offsetDays: number): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().split('T')[0];
}

export function reminderIdFromResponses(Responses: { reminderForDisconnection: unknown[] }): number {
  const r = Responses.reminderForDisconnection[0] as number | { id: number };
  return typeof r === 'number' ? r : r.id;
}

export async function buildManualInvoicePayloadForContract(
  fx: Pdt2971Fx,
  customerIndex: number,
  contractIndex: number,
) {
  const payload = manualInvoice();
  const customerGet = await fx.Request.get(
    `customer/${fx.Responses.customer[customerIndex].id}?version=1`,
  );
  await expect(customerGet).CheckResponse();
  const customerJson = await customerGet.json();
  const contractGet = await fx.Request.get(
    `product-contract/${fx.Responses.productContract[contractIndex].id}?version=1`,
  );
  await expect(contractGet).CheckResponse();
  const contractJson = await contractGet.json();
  const basic = payload.manualInvoiceParameters.manualInvoiceBasicDataParameters;
  basic.customerDetailId = customerJson.customerDetailsId;
  basic.invoiceCommunicationDataId = customerJson.communicationData[0].id;
  basic.billingGroupId = contractJson.contractPodsResponses[0].billingGroupId;
  basic.contractOrderId = contractJson.basicParameters.id;
  basic.prefixType = null;
  basic.contractOrderType = 'PRODUCT_CONTRACT';
  return payload;
}

export async function resolvePodIdentifier(fx: Pdt2971Fx): Promise<string> {
  const podGet = await fx.Request.get(`${fx.Endpoints.pod}/${fx.Responses.pod[0].id}`);
  await expect(podGet).CheckResponse();
  const body = await podGet.json();
  const identifier = String(body.identifier ?? '');
  expect(identifier.length).toBeGreaterThan(0);
  return identifier;
}

export async function resolveContractNumber(fx: Pdt2971Fx, contractIndex: number): Promise<string> {
  const contractId = fx.Responses.productContract[contractIndex].id;
  const contractGet = await fx.Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const body = await contractGet.json();
  const number = String(body.basicParameters?.contractNumber ?? body.contractNumber ?? '');
  expect(number.length).toBeGreaterThan(0);
  return number;
}

export async function shiftCustomerLiabilitiesToYesterday(
  fx: Pdt2971Fx,
  customerIndex: number,
): Promise<void> {
  const identifier = String(fx.Responses.customer[customerIndex].identifier);
  const liabilitiesRes = await fx.Request.get(
    `customer-liability/list?page=0&size=10&columns=ID&direction=DESC&prompt=${identifier}&searchFields=CUSTOMER`,
  );
  await expect(liabilitiesRes).CheckResponse();
  const body = await liabilitiesRes.json();
  const rows = (body.content ?? []) as Array<{ id: number; currentAmount?: number }>;
  const open = rows.filter((r) => (r.currentAmount ?? 0) > 0);
  expect(open.length, `Expected open liability for customer ${identifier}`).toBeGreaterThan(0);

  const dueDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');
  for (const row of open) {
    const change = await fx.Request.put(
      `customer-liability/${row.id}/due-date-change?dueDate=${dueDate}`,
    );
    await expect(change).CheckResponse();
  }
}

export async function executeReminderForDisconnection(fx: Pdt2971Fx): Promise<void> {
  const reminderId = reminderIdFromResponses(fx.Responses);
  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const hours = georgianTime.getHours();
  const minutes = georgianTime.getMinutes();

  const timeOffset = await fx.Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${reminderId}&hour=${hours}&minute=${minutes}`,
  );
  await expect(timeOffset).CheckResponse();

  await fx.Request.post('power-supply-disconnection-reminder/job');

  const maxAttempts = 24;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const reminderGet = await fx.Request.get(`power-supply-disconnection-reminder/${reminderId}`);
    await expect(reminderGet).CheckResponse();
    const reminderJson = await reminderGet.json();
    if (reminderJson.reminderStatus === 'EXECUTED') {
      return;
    }
    if (attempt < maxAttempts) {
      await new Promise((r) => setTimeout(r, 5000));
    }
  }
  throw new Error('Reminder for disconnection did not reach EXECUTED status within polling window');
}

export async function fetchReminderSecondTab(
  fx: Pdt2971Fx,
  reminderId: number,
): Promise<ReminderSecondTabRow[]> {
  const res = await fx.Request.get(
    `${fx.Endpoints.reminderForDisconnection}/second-tab?page=0&size=100&powerSupplyDisconnectionReminderId=${reminderId}`,
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  return (body.content ?? []) as ReminderSecondTabRow[];
}

export async function fetchLoadCustomersForDps(
  fx: Pdt2971Fx,
  query: CustomersForDpsQuery,
): Promise<PodTabRow[]> {
  const res = await fx.Request.get(
    'disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply',
    { params: query as Record<string, string | number> },
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  return (body.content ?? []) as PodTabRow[];
}

export async function fetchViewPodTab(
  fx: Pdt2971Fx,
  requestId: number,
  query: CustomersForDpsQuery,
): Promise<PodTabRow[]> {
  const res = await fx.Request.get(
    `disconnection-of-power-supply-requests/view-pod-tab/${requestId}`,
    { params: query as Record<string, string | number> },
  );
  await expect(res).CheckResponse();
  const body = await res.json();
  return (body.content ?? []) as PodTabRow[];
}

export async function createRequestForDisconnectionDraft(
  fx: Pdt2971Fx,
): Promise<number> {
  const payload = fx.GeneratePayload.receivablesManagement.requestForDisconnection() as Record<
    string,
    unknown
  >;
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.reminderForDisconnectionId = reminderIdFromResponses(fx.Responses);
  payload.conditionType = 'ALL_CUSTOMERS';
  payload.listOfCustomer = null;
  payload.allSelected = true;
  payload.pods = [];
  payload.podWithHighestConsumption = false;
  const res = await fx.Request.post(fx.Endpoints.requestForDisconnection, { data: payload });
  await expect(res).CheckResponse();
  return Number(await res.json());
}

export function rowsForPodIdentifier(rows: PodTabRow[], podIdentifier: string): PodTabRow[] {
  return rows.filter((r) => r.podIdentifier === podIdentifier);
}

export function reminderRowsForCustomerIdentifier(
  rows: ReminderSecondTabRow[],
  customerIdentifier: string,
): ReminderSecondTabRow[] {
  return rows.filter((r) => (r.customer ?? '').includes(customerIdentifier));
}

/**
 * AS-IS defect (PDT-2971 open): request POD tab merges contracts/liabilities across customers on one POD.
 * Passes while bug is open. When fixed per Confluence 72155868, flip to per-customer scoping assertions.
 */
export function assertPdt2971AsIsMergedPodTabRow(params: {
  row: PodTabRow;
  customerBId: number;
  customerBIdentifier: string;
  contractNumberA: string;
  contractNumberB: string;
}): void {
  const { row, customerBId, customerBIdentifier, contractNumberA, contractNumberB } = params;
  const contracts = String(row.contracts ?? '');
  const liabilitiesBlob = `${row.liabilitiesInPod ?? ''}|${row.liabilitiesInBillingGroup ?? ''}`;

  expect(row.customerId, 'displayed row should be tied to one customerId').toBe(customerBId);
  expect(String(row.customers ?? '')).toContain(customerBIdentifier);
  expect(contracts, 'AS-IS bug: terminated/other customer contract merged into single POD row').toContain(
    contractNumberA,
  );
  expect(contracts, 'active customer contract should appear on the same merged row').toContain(
    contractNumberB,
  );
  expect(
    contracts.split(',').map((s) => s.trim()).filter(Boolean).length,
    'AS-IS bug: multiple contract numbers concatenated on one POD row',
  ).toBeGreaterThan(1);
  expect(liabilitiesBlob.length, 'liabilities should be present on the merged POD row').toBeGreaterThan(0);
}
