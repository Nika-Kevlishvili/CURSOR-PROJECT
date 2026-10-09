/**
 * PDT-3063 helpers: product-contract billing group alternative recipient +
 * Manual invoice / Manual interim Invoice Communication Data.
 *
 * Reference:
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts (manual interim extras)
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts (ids, signed service contract)
 * - jsons/payloads/create/billing/manualInvoice.ts
 * - jsons/payloads/create/contractOrders/billingGroup.ts
 */
import { test, expect } from './cursor-test.fixtures';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import type { ProductContractPayload } from '../../jsons/payloads/create/contractOrders/productContract';
import {
  asBillingRunId,
  asPriceComponentId,
  asServiceId,
  resolvePdt2599ContractFormulaForPost,
} from './pdt-2599-service-contract.fixtures';

export type Pdt3063Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export type Pdt3063IcdItem = {
  id?: number;
  name?: string;
  customerId?: number;
  customerVersion?: number;
  defaultSelected?: boolean | null;
};

export type Pdt3063Chain = {
  customerCId: number;
  customerCDetailsId: number;
  kommContractId: number;
  customerRId: number | null;
  customerRDetailsId: number | null;
  customerRVersion: number | null;
  komm2Id: number | null;
  komm2bId: number | null;
  customerAId: number | null;
  customerADetailsId: number | null;
  komm2aId: number | null;
  customerBId: number | null;
  customerBDetailsId: number | null;
  komm2bRecipientId: number | null;
  productContractId: number;
  productContractPayload: ProductContractPayload;
  billingGroupId: number;
  pod1Id: number;
  pod1DetailId: number;
};

const ICD_LIST_PATH = 'billing-run/manual-invoice/communication-data/list';

/** Fixture map keys (underscores) vs POST contactTypeName (underscore is invalid on Dev). */
const COMM_API_LABEL: Record<string, string> = {
  KOMM_CONTRACT: 'KOMM-CONTRACT',
  KOMM2: 'KOMM2',
  KOMM2B: 'KOMM2B',
  KOMM2_A: 'KOMM2-A',
  KOMM2_B: 'KOMM2-B',
};

export function pdt3063CommApiLabel(mapKey: string): string {
  return COMM_API_LABEL[mapKey] ?? mapKey.replaceAll('_', '-');
}

function commMatchesLabel(row: { name?: unknown; contactTypeName?: unknown }, apiLabel: string): boolean {
  return row.name === apiLabel || row.contactTypeName === apiLabel;
}

export { asBillingRunId };

export function uniquePdt3063Token(): string {
  return `${Date.now()}${Math.floor(Math.random() * 900 + 100)}`;
}

function asNumericId(body: unknown): number {
  if (typeof body === 'number' && Number.isFinite(body)) return body;
  if (body !== null && typeof body === 'object' && 'id' in body) {
    const n = Number((body as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  throw new Error(`Expected numeric id or { id }, got ${JSON.stringify(body)?.slice(0, 200)}`);
}

export function pushPriceComponentId(Responses: Pdt3063Fx['Responses'], raw: unknown): number {
  const id = asPriceComponentId(raw);
  Responses.priceComponent.push(id);
  return id;
}

export function pushProductContract(Responses: Pdt3063Fx['Responses'], raw: unknown): number {
  const id = asNumericId(raw);
  Responses.productContract.push(typeof raw === 'object' && raw !== null ? raw : { id });
  return id;
}

export function pushBillingRun(
  Responses: Pdt3063Fx['Responses'],
  raw: unknown,
  portalType: 'MANUAL_INVOICE' | 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT' | 'STANDARD_BILLING',
): number {
  const id = asBillingRunId(raw);
  Responses.billingRun.push({ id, portalType });
  return id;
}

function cloneCommTemplate(template: Record<string, unknown>, contactTypeName: string): Record<string, unknown> {
  const cloned = JSON.parse(JSON.stringify(template)) as Record<string, unknown>;
  cloned.status = 'ACTIVE';
  cloned.contactTypeName = contactTypeName;
  const contacts = (cloned.communicationContacts as Record<string, unknown>[] | undefined) ?? [];
  for (const c of contacts) {
    c.status = 'ACTIVE';
    if (c.contactType === 'EMAIL') {
      c.contactValue = `pdt3063.${uniquePdt3063Token()}@example.test`;
    }
    if (c.contactType === 'MOBILE_NUMBER') {
      c.contactValue = `35988${uniquePdt3063Token().slice(-7)}`;
    }
  }
  cloned.communicationContacts = contacts;
  return cloned;
}

export async function getCustomerVersion1(
  fx: Pdt3063Fx,
  customerId: number,
): Promise<Record<string, unknown>> {
  const { Request, Endpoints } = fx;
  const res = await Request.get(`${Endpoints.customer}/${customerId}?version=1`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function listCustomerBillingComms(
  fx: Pdt3063Fx,
  customerDetailsId: number,
): Promise<Pdt3063IcdItem[]> {
  const { Request, Endpoints } = fx;
  const res = await Request.get(`${Endpoints.customer}/communication-data/list`, {
    params: { customerDetailsId, communicationDataType: 'BILLING' },
  });
  await expect(res).CheckResponse();
  const body = await res.json();
  return Array.isArray(body) ? body : [];
}

export function findCommByName(rows: Pdt3063IcdItem[], name: string): Pdt3063IcdItem {
  const found = rows.find((r) => r.name === name);
  expect(found, `billing communication named ${name}`).toBeTruthy();
  return found as Pdt3063IcdItem;
}

export async function listManualInvoiceCommunicationData(
  fx: Pdt3063Fx,
  query: {
    customerDetailsId?: number;
    contractOrderType?: string;
    contractOrderId?: number;
    billingGroupIds?: number[];
    omitCustomerDetailsId?: boolean;
  },
) {
  const { Request } = fx;
  const search = new URLSearchParams();
  if (!query.omitCustomerDetailsId && query.customerDetailsId != null) {
    search.append('customerDetailsId', String(query.customerDetailsId));
  }
  if (query.contractOrderType) search.append('contractOrderType', query.contractOrderType);
  if (query.contractOrderId != null) search.append('contractOrderId', String(query.contractOrderId));
  for (const id of query.billingGroupIds ?? []) {
    search.append('billingGroupIds', String(id));
  }
  const url = `${ICD_LIST_PATH}?${search.toString()}`;
  return Request.get(url);
}

export async function parseIcdListOk(response: Awaited<ReturnType<Pdt3063Fx['Request']['get']>>): Promise<Pdt3063IcdItem[]> {
  await expect(response).CheckResponse();
  const body = await response.json();
  expect(Array.isArray(body), 'ICD list must be a JSON array').toBeTruthy();
  return body as Pdt3063IcdItem[];
}

export async function readErrorBody(response: Awaited<ReturnType<Pdt3063Fx['Request']['get']>>): Promise<string> {
  const text = await response.text();
  try {
    return JSON.stringify(JSON.parse(text));
  } catch {
    return text;
  }
}

export async function expectHttp400WithMessage(
  response: { status: () => number; text: () => Promise<string> },
  substring: string,
): Promise<string> {
  const body = await readErrorBody(response);
  expect(response.status(), body).toBe(400);
  expect(body, `expected substring: ${substring}`).toContain(substring);
  return body;
}

async function postLegalCustomerWithComms(
  fx: Pdt3063Fx,
  commMapKeys: string[],
): Promise<{ id: number; detailsId: number; version: number; commIdsByName: Record<string, number> }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  const biz = (payload.businessCustomerDetails ?? {}) as Record<string, unknown>;
  biz.name = `PDT3063 C ${uniquePdt3063Token()}`;
  biz.nameTranslated = biz.name;
  payload.businessCustomerDetails = biz;

  const template = ((payload.communicationData as unknown[])?.[0] ?? {}) as Record<string, unknown>;
  payload.communicationData = commMapKeys.map((key) =>
    cloneCommTemplate(template, pdt3063CommApiLabel(key)),
  );

  const res = await Request.post(Endpoints.customer, { data: payload });
  await expect(res).CheckResponse();
  const created = await res.json();
  Responses.customer.push(created);
  const customerId = asNumericId(created);
  const view = await getCustomerVersion1(fx, customerId);
  const detailsId = Number(view.customerDetailsId ?? created.lastCustomerDetailId);
  const version = Number(view.versionId ?? view.version ?? 1);
  const fromGet = ((view.communicationData as Record<string, unknown>[]) ?? []).filter(Boolean);
  const billingRows = await listCustomerBillingComms(fx, detailsId);
  const commIdsByName: Record<string, number> = {};
  for (let i = 0; i < commMapKeys.length; i++) {
    const mapKey = commMapKeys[i];
    const apiLabel = pdt3063CommApiLabel(mapKey);
    const fromView = fromGet.find((c) => commMatchesLabel(c, apiLabel)) ?? fromGet[i];
    if (fromView?.id != null) {
      commIdsByName[mapKey] = Number(fromView.id);
      continue;
    }
    const row = billingRows.find((r) => commMatchesLabel(r, apiLabel)) ?? billingRows[i];
    expect(row?.id, `billing communication for ${mapKey} (API label ${apiLabel})`).toBeTruthy();
    commIdsByName[mapKey] = Number(row.id);
  }
  return { id: customerId, detailsId, version, commIdsByName };
}

export async function createPdt3063Catalog(fx: Pdt3063Fx): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const pricePayload = GeneratePayload.productAndServices.priceSettlement();
  pricePayload.formulaRequest.expression = 50;
  const price = await Request.post(Endpoints.priceComponent, { data: pricePayload });
  await expect(price).CheckResponse();
  pushPriceComponentId(Responses, await price.json());

  const productPayload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
  productPayload.productStatus = 'ACTIVE';
  productPayload.isIndividual = false;
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.interimAdvancePayments = [];
  productPayload.interimAdvancePaymentGroups = [];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());
}

export async function putBillingGroupAlt(
  fx: Pdt3063Fx,
  billingGroupId: number,
  fields: {
    contractId: number;
    alternativeRecipientCustomerDetailId: number | null;
    billingCustomerCommunicationId: number | null;
  },
): Promise<Record<string, unknown>> {
  const { Request, GeneratePayload, Endpoints } = fx;
  const getRes = await Request.get(`${Endpoints.billingGroup}/${billingGroupId}`);
  await expect(getRes).CheckResponse();
  const existing = (await getRes.json()) as Record<string, unknown>;
  const payload = await GeneratePayload.contractsAndOrders.editBillingGroup(0);
  payload.id = billingGroupId;
  payload.contractId = fields.contractId as unknown as string;
  payload.groupNumber = (existing.groupNumber as number) ?? payload.groupNumber;
  payload.sendingInvoice = 'EMAIL';
  payload.separateInvoiceForEachPod = Boolean(existing.separateInvoiceForEachPod);
  payload.directDebit = Boolean(existing.directDebit);
  payload.alternativeRecipientCustomerDetailId = fields.alternativeRecipientCustomerDetailId;
  payload.billingCustomerCommunicationId = fields.billingCustomerCommunicationId;
  const putRes = await Request.put(`${Endpoints.billingGroup}/${billingGroupId}`, { data: payload });
  await expect(putRes).CheckResponse();
  const after = await Request.get(`${Endpoints.billingGroup}/${billingGroupId}`);
  await expect(after).CheckResponse();
  return (await after.json()) as Record<string, unknown>;
}

export type Pdt3063SetupOptions = {
  attachAlt?: boolean;
  extraBillingCommOnAlt?: boolean;
  twoAltRecipients?: boolean;
  createUnattachedAlt?: boolean;
};

export async function createPdt3063ProductContractWithAltRecipient(
  fx: Pdt3063Fx,
  options: Pdt3063SetupOptions = {},
): Promise<Pdt3063Chain> {
  const attachAlt = options.attachAlt !== false;
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  await createPdt3063Catalog(fx);

  const c = await postLegalCustomerWithComms(fx, ['KOMM_CONTRACT']);
  // Keep C at Responses.customer[0] for product_contract generator.
  Responses.customer.length = 0;
  Responses.customer.push({ id: c.id, lastCustomerDetailId: c.detailsId });

  let r: Awaited<ReturnType<typeof postLegalCustomerWithComms>> | null = null;
  let a: Awaited<ReturnType<typeof postLegalCustomerWithComms>> | null = null;
  let b: Awaited<ReturnType<typeof postLegalCustomerWithComms>> | null = null;

  if (options.twoAltRecipients) {
    a = await postLegalCustomerWithComms(fx, ['KOMM2_A']);
    b = await postLegalCustomerWithComms(fx, ['KOMM2_B']);
  } else if (attachAlt || options.createUnattachedAlt) {
    const names = options.extraBillingCommOnAlt ? ['KOMM2', 'KOMM2B'] : ['KOMM2'];
    r = await postLegalCustomerWithComms(fx, names);
  }

  const pod = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
  await expect(pod).CheckResponse();
  const podBody = await pod.json();
  Responses.pod.push(podBody);

  const productContractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
  productContractPayload.basicParameters.status = 'SIGNED';
  productContractPayload.basicParameters.subStatus = 'SIGNED_BY_BOTH_SIDES';
  productContractPayload.basicParameters.versionStatus = 'SIGNED';
  productContractPayload.basicParameters.communicationDataBillingId = c.commIdsByName.KOMM_CONTRACT;
  productContractPayload.basicParameters.communicationDataContractId = c.commIdsByName.KOMM_CONTRACT;
  productContractPayload.productParameters.contractType = 'SUPPLY_ONLY';
  const contractRes = await Request.post(Endpoints.productContract, { data: productContractPayload });
  await expect(contractRes).CheckResponse();
  const productContractId = pushProductContract(Responses, await contractRes.json());

  const activate = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
  });
  await expect(activate).CheckResponse();

  const contractViewRes = await Request.get(`${Endpoints.productContract}/${productContractId}?version=1`);
  await expect(contractViewRes).CheckResponse();
  const contractView = (await contractViewRes.json()) as Record<string, unknown>;
  const billingGroups = (contractView.billingGroups as { id: number }[]) ?? [];
  const pods = (contractView.contractPodsResponses as { billingGroupId?: number }[]) ?? [];
  const billingGroupId = Number(billingGroups[0]?.id ?? pods[0]?.billingGroupId);
  expect(billingGroupId, 'billingGroupId from product contract').toBeTruthy();

  const komm2Id = r ? r.commIdsByName.KOMM2 : a ? a.commIdsByName.KOMM2_A : null;
  if (attachAlt && (r || a)) {
    const altDetailsId = Number((r ?? a)!.detailsId);
    await putBillingGroupAlt(fx, billingGroupId, {
      contractId: productContractId,
      alternativeRecipientCustomerDetailId: altDetailsId,
      billingCustomerCommunicationId: Number(komm2Id),
    });
  }

  return {
    customerCId: c.id,
    customerCDetailsId: c.detailsId,
    kommContractId: c.commIdsByName.KOMM_CONTRACT,
    customerRId: r?.id ?? null,
    customerRDetailsId: r?.detailsId ?? null,
    customerRVersion: r?.version ?? null,
    komm2Id: r?.commIdsByName.KOMM2 ?? null,
    komm2bId: r?.commIdsByName.KOMM2B ?? null,
    customerAId: a?.id ?? null,
    customerADetailsId: a?.detailsId ?? null,
    komm2aId: a?.commIdsByName.KOMM2_A ?? null,
    customerBId: b?.id ?? null,
    customerBDetailsId: b?.detailsId ?? null,
    komm2bRecipientId: b?.commIdsByName.KOMM2_B ?? null,
    productContractId,
    productContractPayload,
    billingGroupId,
    pod1Id: asNumericId(podBody),
    pod1DetailId: Number(podBody.podDetailId),
  };
}

export async function addPdt3063SecondBillingGroupWithPod(
  fx: Pdt3063Fx,
  chain: Pdt3063Chain,
): Promise<{ billingGroupId2: number; pod2Id: number; pod2DetailId: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const pod2 = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
  await expect(pod2).CheckResponse();
  const pod2Body = await pod2.json();
  Responses.pod.push(pod2Body);

  const bgPayload = await GeneratePayload.contractsAndOrders.addBillingGroup(0);
  bgPayload.contractId = chain.productContractId as unknown as string;
  bgPayload.sendingInvoice = 'EMAIL';
  bgPayload.separateInvoiceForEachPod = false;
  bgPayload.directDebit = false;
  bgPayload.alternativeRecipientCustomerDetailId = null;
  bgPayload.billingCustomerCommunicationId = null;
  const bgRes = await Request.post(Endpoints.billingGroup, { data: bgPayload });
  await expect(bgRes).CheckResponse();
  const billingGroupId2 = asNumericId(await bgRes.json());

  const editPayload = await GeneratePayload.contractsAndOrders.edit_ProductContract(chain.productContractPayload);
  const addPod = {
    billingGroupId: billingGroupId2,
    productContractPointOfDeliveries: [
      { pointOfDeliveryDetailId: Number(pod2Body.podDetailId), dealNumber: null },
    ],
  };
  editPayload.podRequests.push(addPod);
  editPayload.productContractPointOfDeliveries.push(addPod.productContractPointOfDeliveries[0]);
  const pod2Id = asNumericId(pod2Body);
  const podIds: number[] = [];
  for (const id of [chain.pod1Id, pod2Id]) {
    if (Number.isFinite(id) && !podIds.includes(id)) podIds.push(id);
  }
  for (const row of Responses.pod ?? []) {
    const id = Number((row as { id?: unknown })?.id);
    if (Number.isFinite(id) && !podIds.includes(id)) podIds.push(id);
  }
  let annualMwh = 0;
  for (const podId of podIds) {
    const podGet = await Request.get(`${Endpoints.pod}/${podId}?version=1`);
    await expect(podGet).CheckResponse();
    const podJson = (await podGet.json()) as { type?: string; estimatedMonthlyAvgConsumption?: number };
    if (podJson.type === 'CONSUMER') {
      annualMwh += (Number(podJson.estimatedMonthlyAvgConsumption ?? 0) * 12) / 1000;
    }
  }
  editPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = annualMwh;
  const putContract = await Request.put(
    `${Endpoints.productContract}/${chain.productContractId}?versionId=1&changeFutureVersionsPods=false`,
    { data: editPayload },
  );
  await expect(putContract).CheckResponse();

  const activate2 = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(1),
  });
  await expect(activate2).CheckResponse();

  return {
    billingGroupId2,
    pod2Id,
    pod2DetailId: Number(pod2Body.podDetailId),
  };
}

export async function buildManualInvoicePayload(
  fx: Pdt3063Fx,
  args: {
    customerDetailsId: number;
    productContractId: number;
    billingGroupId: number | null;
    invoiceCommunicationDataId: number | null;
    omitInvoiceCommunicationDataId?: boolean;
    contractOrderType?: 'PRODUCT_CONTRACT' | 'SERVICE_CONTRACT';
    contractOrderId?: number;
  },
) {
  const payload = await fx.GeneratePayload.billing.manualInvoice();
  const basic = payload.manualInvoiceParameters.manualInvoiceBasicDataParameters;
  basic.customerDetailId = args.customerDetailsId;
  basic.contractOrderType = args.contractOrderType ?? 'PRODUCT_CONTRACT';
  basic.contractOrderId = args.contractOrderId ?? args.productContractId;
  basic.billingGroupId = args.billingGroupId;
  basic.prefixType = args.contractOrderType === 'SERVICE_CONTRACT' ? null : null;
  basic.basisForIssuing = 'MANUAL INVOICE';
  if (args.omitInvoiceCommunicationDataId) {
    delete (basic as { invoiceCommunicationDataId?: number | null }).invoiceCommunicationDataId;
  } else {
    basic.invoiceCommunicationDataId = args.invoiceCommunicationDataId;
  }
  payload.manualInvoiceParameters.manualInvoiceSummaryDataParameters.manualInvoiceType = 'STANDARD_INVOICE';
  payload.manualInvoiceParameters.manualInvoiceSummaryDataParameters.summaryDataRowList[0].value = 10;
  payload.manualInvoiceParameters.manualInvoiceSummaryDataParameters.summaryDataRowList[0].valueCurrencyId =
    envVariables.currency;
  return payload;
}

export async function buildManualInterimPayload(
  fx: Pdt3063Fx,
  args: {
    customerDetailsId: number;
    productContractId: number;
    billingGroupIds: number[];
    invoiceCommunicationDataId: number | null;
  },
) {
  const payload = fx.GeneratePayload.billing.Manualinterim() as Record<string, unknown> & {
    interimAndAdvancePaymentParameters: Record<string, unknown>;
  };
  const iap = payload.interimAndAdvancePaymentParameters;
  iap.customerDetailId = args.customerDetailsId;
  iap.invoiceCommunicationDataId = args.invoiceCommunicationDataId;
  iap.currencyId = envVariables.currency;
  iap.amountExcludingVat = 100;
  iap.basisForIssuing = 'INTERIM';
  iap.issuingForTheMonthToCurrent = 'ZERO';
  iap.deductionFrom = 'FIRST_INVOICE_FOR_SAME_PERIOD';
  iap.issuedSeparateInvoices = ['INVOICE_ONE'];
  iap.prefixType = null;
  iap.contractType = 'PRODUCT_CONTRACT';
  iap.contractId = args.productContractId;
  iap.billingGroupIds = args.billingGroupIds;
  iap.vatRateId = null;
  iap.globalVatRate = true;
  iap.applicableInterestRateId = envVariables.interest_rate ?? envVariables.base_interest_rate;
  iap.applicableInterestRateManual = true;
  return payload;
}

export function miIcd(getBody: Record<string, unknown>): Record<string, unknown> | null {
  const mi = getBody.manualInvoiceBillingRunParameters as Record<string, unknown> | undefined;
  const basic = mi?.manualInvoiceBasicDataParameters as Record<string, unknown> | undefined;
  return (basic?.invoiceCommunicationData as Record<string, unknown> | undefined) ?? null;
}

export function interimIcd(getBody: Record<string, unknown>): Record<string, unknown> | null {
  const interim = getBody.manualInterimAndAdvancePaymentParametersResponse as Record<string, unknown> | undefined;
  return (interim?.invoiceCommunicationData as Record<string, unknown> | undefined) ?? null;
}

export async function getBillingRun(fx: Pdt3063Fx, billingRunId: number): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${fx.Endpoints.billingRun}/${billingRunId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

function sofiaIsoDateFrom(at: Date): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Europe/Sofia',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(at);
  const year = parts.find((p) => p.type === 'year')?.value;
  const month = parts.find((p) => p.type === 'month')?.value;
  const day = parts.find((p) => p.type === 'day')?.value;
  const iso = `${year}-${month}-${day}`;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    throw new Error(`sofiaIsoDateFrom produced ${iso}`);
  }
  return iso;
}

function sofiaIsoDateToday(): string {
  return sofiaIsoDateFrom(new Date());
}

function sofiaIsoDateTomorrow(): string {
  return sofiaIsoDateFrom(new Date(Date.now() + 36 * 60 * 60 * 1000));
}

export async function createPdt3063ServiceContractContext(fx: Pdt3063Fx): Promise<{
  customerCDetailsId: number;
  kommServiceId: number;
  serviceContractId: number;
  komm2Id: number;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const customer = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(customer).CheckResponse();
  const createdCustomer = await customer.json();
  Responses.customer.push(createdCustomer);
  const customerId = asNumericId(createdCustomer);
  const view = await getCustomerVersion1(fx, customerId);
  const customerCDetailsId = Number(view.customerDetailsId ?? createdCustomer.lastCustomerDetailId);

  const billingRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
    params: { customerDetailsId: customerCDetailsId, communicationDataType: 'BILLING' },
  });
  await expect(billingRes).CheckResponse();
  const billingRows = (await billingRes.json()) as { id: number }[];
  const contractCommRes = await Request.get(`${Endpoints.customer}/communication-data/list`, {
    params: { customerDetailsId: customerCDetailsId, communicationDataType: 'CONTRACT' },
  });
  await expect(contractCommRes).CheckResponse();
  const contractRows = (await contractCommRes.json()) as { id: number }[];
  expect(billingRows?.length && contractRows?.length).toBeTruthy();
  const kommServiceId = Number(billingRows[0].id);
  const kommContractCommId = Number(contractRows[0].id);

  const price = await Request.post(Endpoints.priceComponent, {
    data: GeneratePayload.productAndServices.perPiece(),
  });
  await expect(price).CheckResponse();
  const priceRaw = await price.json();
  pushPriceComponentId(Responses, priceRaw);
  const pcId = asPriceComponentId(priceRaw);
  const pcGet = await Request.get(`${Endpoints.priceComponent}/${pcId}`);
  await expect(pcGet).CheckResponse();
  const pricePostBody = await pcGet.json();

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const pod = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
  await expect(pod).CheckResponse();
  Responses.pod.push(await pod.json());

  const service = await Request.post(Endpoints.service, {
    data: GeneratePayload.productAndServices.service(),
  });
  await expect(service).CheckResponse();
  Responses.service.push(await service.json());

  const serviceId = asServiceId(Responses.service[0]);
  const serviceViewRes = await Request.get(`${Endpoints.service}/${serviceId}?version=1`);
  await expect(serviceViewRes).CheckResponse();
  const serviceView = await serviceViewRes.json();
  const activeVersion = (serviceView.versions ?? []).find((v: { status?: string }) => v.status === 'ACTIVE');
  expect(activeVersion?.id).toBeTruthy();
  expect(activeVersion?.detailId).toBeTruthy();
  const serviceVersionId = activeVersion.id as number;
  const serviceDetailId = activeVersion.detailId as number;

  const thirdRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
    params: { serviceDetailId },
  });
  await expect(thirdRes).CheckResponse();
  const third = (await thirdRes.json()) as Record<string, unknown>;
  const termsCatalog = third.serviceContractTerms as { id: number }[];
  const invoiceTermsCatalog = third.invoicePaymentTerms as { id: number; value: number }[];
  expect(termsCatalog?.length).toBeGreaterThan(0);
  expect(invoiceTermsCatalog?.length).toBeGreaterThan(0);

  const formula = await resolvePdt2599ContractFormulaForPost(Request, Endpoints, third, pricePostBody, []);
  const payload = await GeneratePayload.contractsAndOrders.serviceContract();
  payload.basicParameters.communicationDataForBilling = kommServiceId;
  payload.basicParameters.communicationDataForContract = kommContractCommId;
  payload.basicParameters.serviceVersionId = serviceVersionId;
  payload.basicParameters.contractStatus = 'SIGNED';
  payload.basicParameters.detailsSubStatus = 'SIGNED_BY_BOTH_SIDES';
  payload.basicParameters.contractVersionStatus = 'SIGNED';
  payload.serviceParameters.contractTermId = termsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTermId = invoiceTermsCatalog[0].id;
  payload.serviceParameters.invoicePaymentTerm = invoiceTermsCatalog[0].value;
  payload.serviceParameters.contractFormulas = formula
    ? [{ formulaVariableId: formula.formulaVariableId, value: formula.value }]
    : [];
  payload.serviceParameters.podIds = [Responses.pod[0].id];

  const isoToday = sofiaIsoDateToday();
  const isoEif = sofiaIsoDateTomorrow();
  payload.basicParameters.contractStatusModifyDate = isoToday;
  payload.basicParameters.signInDate = isoToday;
  payload.basicParameters.entryIntoForceDate = isoEif;
  payload.serviceParameters.entryIntoForceDate = isoEif;
  expect(JSON.stringify(payload), 'service-contract POST must not send US MM/DD/YYYY dates').not.toMatch(
    /\d{2}\/\d{2}\/\d{4}/,
  );

  const createRes = await Request.post(Endpoints.serviceContract, { data: payload });
  await expect(createRes).CheckResponse();
  const serviceContractId = asNumericId(await createRes.json());
  Responses.serviceContract.push({ id: serviceContractId });

  const r = await postLegalCustomerWithComms(fx, ['KOMM2']);
  return {
    customerCDetailsId,
    kommServiceId,
    serviceContractId,
    komm2Id: r.commIdsByName.KOMM2,
  };
}
