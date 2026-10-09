/**
 * PDT-3063 — manual invoice / manual interim invoice communication data
 * when the billing group has an alternative recipient.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 * - tests/cursor/reg-1172-volume-with-electricity.fixtures.ts
 *
 * Swagger (Dev2 live, phoenix2-dev / 10.236.20.11:8092):
 * BillingCommunicationDataListRequest: customerDetailsId, contractOrderType,
 * contractOrderId, objectCreateDate, billingGroupIds.
 * CustomerCommunicationDataResponse: id, name, createDate, concatPurposes,
 * customerId, customerVersion, defaultSelected.
 * Dev2 list query customerCommunicationDataListWithCustomerInfo requires an
 * active EMAIL contact and an active MOBILE_NUMBER contact.
 */
import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { manualInvoice } from '../../jsons/payloads/create/billing/manualInvoice';
import { preconditionProductContractForManual } from './pdt-2872-minimal-interim-payment.fixtures';

export type Pdt3063Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export type AltRecipientSetup = {
  contractCustomerDetailId: number;
  contractOrderId: number;
  billingGroupId: number;
  altCustomerId: number;
  altCustomerIdentifier: string;
  altCustomerDetailId: number;
  selectedCommunicationId: number;
  otherCommunicationId: number;
};

type CommRow = {
  id?: number;
  name?: string;
  customerId?: number;
  customerVersion?: number;
  defaultSelected?: boolean;
};

function asNumber(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

function cloneAddressWithoutCoordinates(template: Record<string, unknown>): Record<string, unknown> {
  const address = JSON.parse(JSON.stringify(template.address ?? {})) as Record<string, unknown>;
  delete address.latitude;
  delete address.longitude;
  return address;
}

function billingComm(
  template: Record<string, unknown>,
  label: string,
  email: string | null,
  mobile: string,
): Record<string, unknown> {
  const communicationContacts: Record<string, unknown>[] = [
    {
      sendSms: true,
      platformId: envVariables.platform,
      status: 'ACTIVE',
      contactType: 'MOBILE_NUMBER',
      contactValue: mobile,
    },
  ];
  if (email) {
    communicationContacts.push({
      sendSms: false,
      platformId: envVariables.platform,
      status: 'ACTIVE',
      contactType: 'EMAIL',
      contactValue: email,
    });
  }
  return {
    status: 'ACTIVE',
    contactTypeName: label,
    contactPurposes: [{ contactPurposeId: envVariables.billing_purpose, status: 'ACTIVE' }],
    address: cloneAddressWithoutCoordinates(template),
    communicationContacts,
    contactPersons: [],
  };
}

function billingRunIdFrom(body: unknown): number {
  if (typeof body === 'number' && Number.isFinite(body) && body > 0) return body;
  if (body && typeof body === 'object') {
    const record = body as Record<string, unknown>;
    const id = asNumber(record.id ?? record.billingRunId);
    if (id > 0) return id;
  }
  throw new Error(`Unexpected billing-run create body: ${JSON.stringify(body).slice(0, 500)}`);
}

export async function setupAltRecipientBillingGroup(
  fx: Pdt3063Fx,
  options?: { selectedWithoutEmail?: boolean },
): Promise<AltRecipientSetup> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  await test.step('Precondition: product contract for the contract customer', async () => {
    await preconditionProductContractForManual(fx);
  });

  let altCustomerId = 0;
  await test.step('Precondition: alternative recipient with two billing communications', async () => {
    const payload = GeneratePayload.customers.customer_legal() as unknown as Record<string, unknown>;
    const rows = payload.communicationData as Record<string, unknown>[];
    const template = rows[0];
    const stamp = Date.now();
    const komm1Email = options?.selectedWithoutEmail ? null : `pdt3063.komm1.${stamp}@example.com`;
    payload.communicationData = [
      billingComm(template, 'KOMM1', komm1Email, `555${String(stamp).slice(-7)}1`),
      billingComm(template, 'KOMM2', `pdt3063.komm2.${stamp}@example.com`, `555${String(stamp).slice(-7)}2`),
    ];
    const created = await Request.post(Endpoints.customer, { data: payload });
    await expect(created).CheckResponse();
    const body = (await created.json()) as { id?: number };
    Responses.customer.push(body);
    altCustomerId = asNumber(body.id);
    expect(altCustomerId, 'alternative recipient customer id').toBeGreaterThan(0);
  });

  const altGet = await Request.get(`${Endpoints.customer}/${altCustomerId}?version=1`);
  await expect(altGet).CheckResponse();
  const altJson = (await altGet.json()) as {
    identifier?: string;
    customerDetailsId?: number;
    lastCustomerDetailId?: number;
    communicationData?: Array<{ id?: number; contactTypeName?: string }>;
  };
  const altCustomerDetailId = asNumber(altJson.customerDetailsId) || asNumber(altJson.lastCustomerDetailId);
  const named = altJson.communicationData ?? [];
  const komm1 = named.find((row) => row.contactTypeName === 'KOMM1');
  const komm2 = named.find((row) => row.contactTypeName === 'KOMM2');
  const selectedCommunicationId = asNumber(komm1?.id);
  const otherCommunicationId = asNumber(komm2?.id);
  expect(selectedCommunicationId, 'KOMM1 communication id').toBeGreaterThan(0);
  expect(otherCommunicationId, 'KOMM2 communication id').toBeGreaterThan(0);

  const contractCustomer = Responses.customer[0] as { id?: number };
  const contractCustomerGet = await Request.get(`${Endpoints.customer}/${contractCustomer.id}?version=1`);
  await expect(contractCustomerGet).CheckResponse();
  const contractCustomerJson = (await contractCustomerGet.json()) as { customerDetailsId?: number };
  const contractCustomerDetailId = asNumber(contractCustomerJson.customerDetailsId);

  const contractGet = await Request.get(`product-contract/${Responses.productContract[0].id}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractJson = (await contractGet.json()) as {
    basicParameters?: { id?: number };
    contractPodsResponses?: Array<{ billingGroupId?: number }>;
    billingGroups?: Array<{ id?: number }>;
  };
  const contractOrderId = asNumber(contractJson.basicParameters?.id);
  const billingGroupId =
    asNumber(contractJson.contractPodsResponses?.[0]?.billingGroupId) ||
    asNumber(contractJson.billingGroups?.[0]?.id);
  expect(contractOrderId, 'product contract version id').toBeGreaterThan(0);
  expect(billingGroupId, 'billing group id').toBeGreaterThan(0);

  await test.step('Precondition: billing group alternative recipient uses KOMM1', async () => {
    const bgPayload = await GeneratePayload.contractsAndOrders.editBillingGroup(0);
    bgPayload.alternativeRecipientCustomerDetailId = altCustomerDetailId;
    bgPayload.billingCustomerCommunicationId = selectedCommunicationId;
    const put = await Request.put(`${Endpoints.billingGroup}/${bgPayload.id}`, { data: bgPayload });
    await expect(put).CheckResponse();
  });

  return {
    contractCustomerDetailId,
    contractOrderId,
    billingGroupId,
    altCustomerId,
    altCustomerIdentifier: String(altJson.identifier ?? ''),
    altCustomerDetailId,
    selectedCommunicationId,
    otherCommunicationId,
  };
}

export async function listInvoiceCommunicationData(
  fx: Pdt3063Fx,
  setup: AltRecipientSetup,
): Promise<CommRow[]> {
  const res = await fx.Request.get(`${fx.Endpoints.billingRun}/manual-invoice/communication-data/list`, {
    params: {
      customerDetailsId: setup.contractCustomerDetailId,
      contractOrderType: 'PRODUCT_CONTRACT',
      contractOrderId: setup.contractOrderId,
      billingGroupIds: setup.billingGroupId,
    },
  });
  await expect(res).CheckResponse();
  const body = await res.json();
  expect(Array.isArray(body), 'communication data list must be an array').toBe(true);
  return body as CommRow[];
}

function findCommunicationId(node: unknown): number {
  if (!node || typeof node !== 'object') return 0;
  if (Array.isArray(node)) {
    for (const item of node) {
      const found = findCommunicationId(item);
      if (found > 0) return found;
    }
    return 0;
  }
  const record = node as Record<string, unknown>;
  if ('invoiceCommunicationDataId' in record) {
    const direct = asNumber(record.invoiceCommunicationDataId);
    if (direct > 0) return direct;
    if (record.invoiceCommunicationDataId && typeof record.invoiceCommunicationDataId === 'object') {
      const nested = asNumber((record.invoiceCommunicationDataId as { id?: number }).id);
      if (nested > 0) return nested;
    }
  }
  if (record.invoiceCommunicationData && typeof record.invoiceCommunicationData === 'object') {
    const nested = asNumber((record.invoiceCommunicationData as { id?: number }).id);
    if (nested > 0) return nested;
  }
  for (const value of Object.values(record)) {
    const found = findCommunicationId(value);
    if (found > 0) return found;
  }
  return 0;
}

export async function createManualInvoiceWithCommunication(
  fx: Pdt3063Fx,
  setup: AltRecipientSetup,
  communicationId: number,
  sendingAnInvoice: 'ACCORDING_TO_THE_CONTRACT' | 'EMAIL',
): Promise<number> {
  const payload = manualInvoice();
  const basic = payload.manualInvoiceParameters.manualInvoiceBasicDataParameters;
  basic.customerDetailId = setup.contractCustomerDetailId;
  basic.invoiceCommunicationDataId = communicationId;
  basic.billingGroupId = setup.billingGroupId;
  basic.contractOrderId = setup.contractOrderId;
  basic.contractOrderType = 'PRODUCT_CONTRACT';
  basic.prefixType = null;
  payload.commonParameters.sendingAnInvoice = sendingAnInvoice;

  const createRes = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(createRes).CheckResponse();
  const created = await createRes.json();
  const billingRunId = billingRunIdFrom(created);
  fx.Responses.billingRun.push(typeof created === 'object' && created ? created : { id: billingRunId });

  const putRes = await fx.Request.put(`${fx.Endpoints.billingRun}/${billingRunId}`, { data: payload });
  await expect(putRes).CheckResponse();
  return billingRunId;
}

export async function createManualInterimWithCommunication(
  fx: Pdt3063Fx,
  setup: AltRecipientSetup,
  communicationId: number,
): Promise<number> {
  const payload = await fx.GeneratePayload.billing.Manualinterim();
  const iap = payload.interimAndAdvancePaymentParameters as Record<string, unknown>;
  iap.customerDetailId = setup.contractCustomerDetailId;
  iap.invoiceCommunicationDataId = communicationId;
  iap.currencyId = envVariables.currency;
  iap.vatRateId = null;
  iap.globalVatRate = true;
  iap.applicableInterestRateId = envVariables.interest_rate ?? envVariables.base_interest_rate;
  iap.applicableInterestRateManual = true;
  iap.prefixType = null;
  iap.contractType = 'PRODUCT_CONTRACT';
  iap.contractId = setup.contractOrderId;
  iap.billingGroupIds = [setup.billingGroupId];
  payload.commonParameters.sendingAnInvoice = 'ACCORDING_TO_THE_CONTRACT';

  const createRes = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(createRes).CheckResponse();
  const created = await createRes.json();
  const billingRunId = billingRunIdFrom(created);
  fx.Responses.billingRun.push(typeof created === 'object' && created ? created : { id: billingRunId });

  const putRes = await fx.Request.put(`${fx.Endpoints.billingRun}/${billingRunId}`, { data: payload });
  await expect(putRes).CheckResponse();
  return billingRunId;
}

export async function readSavedInvoiceCommunicationId(
  fx: Pdt3063Fx,
  billingRunId: number,
): Promise<number> {
  const res = await fx.Request.get(`${fx.Endpoints.billingRun}/${billingRunId}`);
  await expect(res).CheckResponse();
  return findCommunicationId(await res.json());
}
