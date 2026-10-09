/**
 * PDT-3179 helpers — reconnection invoice emails (create + in-scope tax-change reverse).
 *
 * RFD scope: createExecutedRfd uses conditionType LIST_OF_CUSTOMERS + listOfCustomer
 * = this test's customer identifier (Swagger DPSRequestsBaseRequest; minLength 1),
 * allSelected false, pods = [this test's POD row built from GET /pod/{id}] (not Load PODs
 * GET — that SQL is as slow as allSelected true). create() mapIntoPodsResult uses podId + isChecked.
 * Do not use allSelected true — Phoenix create() then calls
 * getCustomersIfSelectAllAndPodWithHighestConIsSelectedList (empty prompt / searchBy ALL)
 * and EXECUTED also generateDocuments before HTTP returns. Do not use ALL_CUSTOMERS.
 *
 * GET RFD (Swagger DPSRequestsResponse) has no taxCalculated. Assert
 * disconnectionRequestsStatus === FEE_CHARGED (plus invoice/email outcomes), never a GET field
 * that is not on the DTO. Collect invoices via POST /customer/{customerDetailId}/customer-invoices
 * (CustomerInvoicesResponse.invoiceId) then GET /invoice?id= — reversal CREDIT_NOTE rows have no
 * customer_liability, so liability→invoice misses them. InvoiceResponse has no reversalCreatedFromId
 * (Swagger); match reversal→original via debitCredits[].id (ConnectedInvoiceResponse).
 *
 * Calculate-tax uses the **default** Taxes for the grid operator (GET/PUT
 * envVariables.taxes_for_grid_operator). DPS uses disconnectedRequest.gridOperatorTaxesId
 * (dedicated POST tax with measurement defaults false). Always restore the default tax.
 *
 * Reminder chain uses a **manual overdue liability with billingGroupId** (no billing run /
 * REAL volume invoice). Do not call makeLiabilityOverdue() or offsetReminderForDisconnectionTime()
 * — both GET invoice?id=Responses.invoice[0] and crash without a billing invoice.
 *
 * Do **not** POST /power-supply-disconnection-reminder/job. That job runs execute() SQL
 * (cartesian mass-blocking × all customer_liabilities) and keeps Dev busy for ~8–10 min
 * per test. POST the reminder with this customer (INCLUDED), poll until EXECUTED
 * (scheduler, seconds), then attach RFD to an already-EXECUTED reminder if ours is
 * still DRAFT. calculate-tax uses RFD pods, not the reminder customer set.
 *
 * Dev RFD POST create() calls getRemindersForDPSRequestList("") (~9–10 min). Playwright
 * must wait for that HTTP — a 30s timeout is an elementary fail, not a product fail.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 *   (EXECUTED RFD: LIST_OF_CUSTOMERS + allSelected false + pods=[load-customer row])
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 *   (LIST_OF_CUSTOMERS + listOfCustomer; that spec uses allSelected true — too slow for this suite)
 * - tests/receivableManagement/massOperationForBlocking/blockingForReminderLetter.spec.ts
 *   (GET contract billingGroups[0].id + POST customer-liability with billingGroupId)
 * - tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures.ts
 *   (createManualLiability)
 * - tests/cursor/pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts
 *   (POST reminder INCLUDED customerList; no global /job; RFD uses EXECUTED reminder id)
 * - tests/cursor/PDT-3219-lpf-reverse-document-no-billing-group.spec.ts
 *   (contrast: that ticket wants billingGroupId null)
 */

import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';

export const PDT_3179_KEY = 'PDT-3179';
export const PDT_3179_TITLE = 'E-mails for creation and reversal of reconnection invoices';
export const TAX_GRID_PATH = 'tax-for-the-grid-operator';
export const PDT_3179_MANUAL_LIABILITY_AMOUNT = 100;
/** Dev POST /disconnection-of-power-supply-requests timeTook ~557–590s (ES). */
export const PDT_3179_RFD_POST_TIMEOUT_MS = 13 * 60 * 1000;
/** One test: supply chain + reminder + RFD create (~10 min) + calculate-tax/DPS. */
export const PDT_3179_TEST_TIMEOUT_MS = 18 * 60 * 1000;
/** TC-BE-16: same chain + Invoice Reversal billing run (draft/PDF/accounting poll). */
export const PDT_3179_REVERSAL_RUN_TIMEOUT_MS = 28 * 60 * 1000;

export type Pdt3179Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
> & {
  FileUploadRequest?: baseFixture['FileUploadRequest'];
};

export type Pdt3179TaxPutPayload = {
  disconnectionType: unknown;
  gridOperator: number;
  documentTemplateId: number;
  emailTemplateId: number;
  numberOfIncomeAccount: string;
  basisForIssuing?: unknown;
  supplierType: string;
  costCenterControllingOrder: string;
  priceComponentOrPriceComponentGroupOrItem?: unknown;
  taxForReconnection: number;
  taxForExpressReconnection: number;
  currency: number;
  removeTaxInCancel: boolean;
  defaultForPodWithMeasurementTypeSlp: boolean;
  defaultForPodWithMeasurementTypeBySettlementPeriod: boolean;
  status: string;
  defaultSelection: boolean;
};

export type Pdt3179TaxSnapshot = {
  id: number;
  raw: Record<string, unknown>;
  putPayload: Pdt3179TaxPutPayload;
};

export type Pdt3179SupplyOpts = {
  emails?: string[];
  phoneOnly?: boolean;
};

export type Pdt3179DpsOpts = {
  taxId: number;
  express?: boolean;
  requestId?: number;
};

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

/** CRM email list rows are EmailCommunicationListingResponse (massOrIndemailCommunicationId), not { id }. */
export function emailCommunicationId(row: unknown): number {
  if (typeof row === 'number' && Number.isFinite(row) && row > 0) return row;
  if (typeof row === 'string' && /^\d+$/.test(row)) return Number(row);
  if (row !== null && typeof row === 'object') {
    const rec = row as Record<string, unknown>;
    const candidates = [
      rec.id,
      rec.massOrIndemailCommunicationId,
      rec.emailCommunicationId,
      (rec.basicParameters as { id?: unknown } | undefined)?.id,
    ];
    for (const candidate of candidates) {
      const id = Number(candidate);
      if (Number.isFinite(id) && id > 0) return id;
    }
  }
  throw new Error(`Cannot resolve email communication id from: ${JSON.stringify(row)?.slice(0, 240)}`);
}

export function todayYmd(): string {
  return randomGens.generateTodaysDate('yyyy-mm-dd');
}

export function monthStartYmd(): string {
  return randomGens.generateMonthStartDate('yyyy-mm-dd');
}

export function listingContent(body: unknown): Record<string, unknown>[] {
  if (Array.isArray(body)) return body as Record<string, unknown>[];
  if (body && typeof body === 'object' && Array.isArray((body as { content?: unknown }).content)) {
    return (body as { content: Record<string, unknown>[] }).content;
  }
  return [];
}

export function asNumber(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

export function nestedId(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) return value;
  if (value && typeof value === 'object' && 'id' in value) {
    const id = Number((value as { id: unknown }).id);
    if (Number.isFinite(id) && id > 0) return id;
  }
  return undefined;
}

export function customerIdentifier(Responses: Pdt3179Fx['Responses']): string {
  return String(Responses.customer[0]?.identifier ?? '');
}

export function errorHaystack(status: number, text: string, json: unknown): string {
  return `${status} ${text} ${JSON.stringify(json ?? {})}`;
}

export async function readHttpBody(
  response: { status: () => number; text: () => Promise<string> },
): Promise<{ status: number; text: string; json: Record<string, unknown> | null }> {
  const status = response.status();
  const text = await response.text();
  let json: Record<string, unknown> | null = null;
  try {
    json = JSON.parse(text) as Record<string, unknown>;
  } catch {
    /* empty or non-JSON */
  }
  return { status, text, json };
}

export function toTaxPutPayload(
  taxData: Record<string, unknown>,
  overrides: Partial<Pdt3179TaxPutPayload> = {},
): Pdt3179TaxPutPayload {
  const documentTemplateId =
    nestedId(taxData.documentTemplateId) ??
    nestedId(taxData.documentTemplateResponse) ??
    Number(envVariables.invoice_document_template);
  const emailTemplateId =
    nestedId(taxData.emailTemplateId) ??
    nestedId(taxData.emailTemplateResponse) ??
    Number(envVariables.invoice_email_template);
  const currency = nestedId(taxData.currency) ?? asNumber(taxData.currency);
  const gridOperator = nestedId(taxData.gridOperator) ?? asNumber(taxData.gridOperator);

  return {
    disconnectionType: taxData.disconnectionType,
    gridOperator,
    documentTemplateId,
    emailTemplateId,
    numberOfIncomeAccount: String(taxData.numberOfIncomeAccount ?? ''),
    basisForIssuing: taxData.basisForIssuing,
    supplierType: String(taxData.supplierType ?? 'CURRENT'),
    costCenterControllingOrder: String(taxData.costCenterControllingOrder ?? 'PDT3179-CC'),
    priceComponentOrPriceComponentGroupOrItem: taxData.priceComponentOrPriceComponentGroupOrItem,
    taxForReconnection: asNumber(taxData.taxForReconnection),
    taxForExpressReconnection: asNumber(taxData.taxForExpressReconnection),
    currency,
    removeTaxInCancel: Boolean(taxData.removeTaxInCancel),
    defaultForPodWithMeasurementTypeSlp: Boolean(taxData.defaultForPodWithMeasurementTypeSlp),
    defaultForPodWithMeasurementTypeBySettlementPeriod: Boolean(
      taxData.defaultForPodWithMeasurementTypeBySettlementPeriod,
    ),
    status: String(taxData.status ?? 'ACTIVE'),
    defaultSelection: Boolean(taxData.defaultSelection),
    ...overrides,
  };
}

export async function getDefaultGridTax(fx: Pdt3179Fx): Promise<Pdt3179TaxSnapshot> {
  const { Request } = fx;
  const id = Number(envVariables.taxes_for_grid_operator);
  expect(id, 'envVariables.taxes_for_grid_operator').toBeGreaterThan(0);
  const res = await Request.get(`${TAX_GRID_PATH}/${id}`);
  await expect(res).CheckResponse();
  const raw = (await res.json()) as Record<string, unknown>;
  return { id, raw, putPayload: toTaxPutPayload(raw) };
}

export async function putGridTax(
  fx: Pdt3179Fx,
  id: number,
  payload: Pdt3179TaxPutPayload,
): Promise<void> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      const res = await fx.Request.put(`${TAX_GRID_PATH}/${id}`, { data: payload });
      await expect(res).CheckResponse();
      return;
    } catch (error) {
      lastError = error;
      const hay = String(error);
      const transient = /ECONNABORTED|ECONNRESET|ECONNREFUSED|ETIMEDOUT|socket hang up/i.test(hay);
      if (!transient || attempt === 3) throw error;
      await new Promise((r) => setTimeout(r, 2000 * attempt));
    }
  }
  throw lastError;
}

export async function restoreGridTax(fx: Pdt3179Fx, snapshot: Pdt3179TaxSnapshot): Promise<void> {
  await putGridTax(fx, snapshot.id, snapshot.putPayload);
}

export async function postDedicatedGridTax(
  fx: Pdt3179Fx,
  overrides: Partial<Pdt3179TaxPutPayload> & { costCenterControllingOrder: string },
): Promise<number> {
  const snapshot = await getDefaultGridTax(fx);
  // Uniqueness is on disconnectionType (stored as name). Cloning the default
  // "ЕСО ЕАД" returns 400 "name already exists" (TaxForTheGridOperatorService.add).
  const uniqueDisconnectionType =
    overrides.disconnectionType != null
      ? overrides.disconnectionType
      : `PDT3179-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
  const payload = toTaxPutPayload(snapshot.raw, {
    defaultSelection: false,
    defaultForPodWithMeasurementTypeSlp: false,
    defaultForPodWithMeasurementTypeBySettlementPeriod: false,
    status: 'ACTIVE',
    supplierType: 'CURRENT',
    ...overrides,
    disconnectionType: uniqueDisconnectionType,
  });
  const res = await fx.Request.post(TAX_GRID_PATH, { data: payload });
  await expect(res).CheckResponse();
  return entityId(await res.json());
}

const PDT_3179_PHONE_ONLY_EMAIL = 'pdt3179.phoneonly@example.com';
const PDT_3179_MOBILE_NUMBER = '555125531';

function mobileContact(): Record<string, unknown> {
  return {
    sendSms: true,
    platformId: envVariables.platform,
    status: 'ACTIVE',
    contactType: 'MOBILE_NUMBER',
    contactValue: PDT_3179_MOBILE_NUMBER,
  };
}

function emailContact(contactValue: string): Record<string, unknown> {
  return {
    sendSms: false,
    platformId: envVariables.platform,
    status: 'ACTIVE',
    contactType: 'EMAIL',
    contactValue,
  };
}

function applyCustomerContacts(
  payload: Record<string, unknown>,
  opts: Pdt3179SupplyOpts,
): void {
  const comm = (payload.communicationData as Array<Record<string, unknown>>)?.[0];
  if (!comm) return;
  if (opts.phoneOnly) {
    comm.communicationContacts = [mobileContact(), emailContact(PDT_3179_PHONE_ONLY_EMAIL)];
    return;
  }
  const emails = opts.emails ?? [];
  if (emails.length === 0) return;
  comm.communicationContacts = [mobileContact(), ...emails.map(emailContact)];
}

function contactTypeOf(contact: Record<string, unknown>): string {
  const raw = contact.contactType;
  if (typeof raw === 'string') return raw;
  if (raw && typeof raw === 'object' && 'name' in raw) {
    return String((raw as { name?: unknown }).name ?? '');
  }
  return '';
}

function isEmailLikeContact(contact: Record<string, unknown>): boolean {
  if (contactTypeOf(contact).toUpperCase() === 'EMAIL') return true;
  return String(contact.contactValue ?? '').includes('@');
}

function isActiveContact(contact: Record<string, unknown>): boolean {
  return String(contact.status ?? 'ACTIVE').toUpperCase() === 'ACTIVE';
}

/** Assert only GET communicationData[].communicationContacts — not a recursive DTO walk. */
function communicationDataHasActiveEmail(body: Record<string, unknown>): boolean {
  const rows = Array.isArray(body.communicationData)
    ? (body.communicationData as Record<string, unknown>[])
    : [];
  for (const row of rows) {
    const contacts = Array.isArray(row.communicationContacts)
      ? (row.communicationContacts as Record<string, unknown>[])
      : [];
    if (contacts.some((contact) => isActiveContact(contact) && isEmailLikeContact(contact))) {
      return true;
    }
  }
  return false;
}

function mapContactForCustomerPut(contact: Record<string, unknown>): Record<string, unknown> {
  const mapped: Record<string, unknown> = {
    sendSms: Boolean(contact.sendSms),
    platformId: contact.platformId ?? envVariables.platform,
    status: contact.status,
    contactType: contactTypeOf(contact),
    contactValue: String(contact.contactValue ?? ''),
  };
  const id = nestedId(contact.id) ?? nestedId(contact);
  if (id) mapped.id = id;
  return mapped;
}

function mapManagerForCustomerPut(manager: Record<string, unknown>): Record<string, unknown> {
  const mapped: Record<string, unknown> = {
    titleId: nestedId(manager.titleId) ?? asNumber(manager.titleId),
    name: String(manager.name ?? ''),
    middleName: manager.middleName ?? null,
    surname: String(manager.surname ?? ''),
    personalNumber: manager.personalNumber ?? null,
    jobPosition: String(manager.jobPosition ?? ''),
    positionHeldFrom: manager.positionHeldFrom ?? null,
    positionHeldTo: manager.positionHeldTo ?? null,
    birthDate: manager.birthDate ?? null,
    representationMethodId:
      nestedId(manager.representationMethodId) ?? asNumber(manager.representationMethodId),
    additionalInformation: manager.additionalInformation ?? null,
    status: String(manager.status ?? 'ACTIVE'),
  };
  const id = nestedId(manager.id) ?? nestedId(manager);
  if (id) mapped.id = id;
  return mapped;
}

function mapContactPurposeForCustomerPut(purpose: Record<string, unknown>): Record<string, unknown> {
  const mapped: Record<string, unknown> = {
    contactPurposeId:
      nestedId(purpose.contactPurposeId) ?? nestedId(purpose) ?? asNumber(purpose.contactPurposeId),
    status: purpose.status ?? 'ACTIVE',
  };
  const id = nestedId(purpose.id);
  if (id) mapped.id = id;
  return mapped;
}

/**
 * Contract create requires ACTIVE EMAIL + MOBILE_NUMBER. TC-BE-9 then omits EMAIL from
 * PUT /customer so Phoenix deletes the contact (editContacts removes rows not in the request).
 * Sending status=DELETED on the EMAIL row returns HTTP 200 but leaves the contact ACTIVE.
 * GET /customer/{id} is a view DTO; PUT needs EditCustomerRequest (version + updateExistingVersion).
 */
export async function deactivateCustomerEmailContacts(fx: Pdt3179Fx): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const customerId = entityId(Responses.customer[0]);
  const getRes = await Request.get(`${Endpoints.customer}/${customerId}`);
  await expect(getRes).CheckResponse();
  const getBody = (await getRes.json()) as Record<string, unknown>;

  const versionId = asNumber(getBody.versionId);
  expect(versionId, 'GET /customer/{id} versionId (customerDetailsVersion)').toBeGreaterThan(0);

  const template = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  const created = Responses.customer[0] as Record<string, unknown>;
  const getComm = Array.isArray(getBody.communicationData)
    ? (getBody.communicationData as Record<string, unknown>[])
    : [];

  const communicationData = getComm.map((row) => {
    const rawContacts = Array.isArray(row.communicationContacts)
      ? (row.communicationContacts as Record<string, unknown>[])
      : [];
    const communicationContacts = rawContacts
      .filter((contact) => !isEmailLikeContact(contact))
      .map(mapContactForCustomerPut);
    const contactPurposes = Array.isArray(row.contactPurposes)
      ? (row.contactPurposes as Record<string, unknown>[]).map(mapContactPurposeForCustomerPut)
      : [];
    const mapped: Record<string, unknown> = {
      contactTypeName: String(row.contactTypeName ?? row.name ?? ''),
      status: row.status ?? 'ACTIVE',
      address: row.address && typeof row.address === 'object' ? row.address : { foreign: false },
      contactPurposes,
      communicationContacts,
    };
    const id = nestedId(row.id) ?? nestedId(row);
    if (id) mapped.id = id;
    return mapped;
  });

  const managers = (
    Array.isArray(getBody.managers) ? (getBody.managers as Record<string, unknown>[]) : []
  ).map(mapManagerForCustomerPut);

  const detailStatusRaw = String(
    getBody.status ?? getBody.customerStatus ?? template.customerDetailStatus ?? 'NEW',
  );
  const detailStatus = ['POTENTIAL', 'NEW', 'ACTIVE', 'LOST', 'ENDED'].includes(detailStatusRaw)
    ? detailStatusRaw
    : 'NEW';

  const editPayload = {
    customerDetailsVersion: versionId,
    updateExistingVersion: true,
    customerType: template.customerType,
    customerIdentifier: getBody.identifier ?? created.identifier ?? template.customerIdentifier,
    foreign: template.foreign,
    marketingConsent: template.marketingConsent,
    preferCommunicationInEnglish: template.preferCommunicationInEnglish,
    oldCustomerNumber: template.oldCustomerNumber,
    vatNumber: template.vatNumber,
    customerDetailStatus: detailStatus,
    businessCustomerDetails: template.businessCustomerDetails,
    ownershipFormId: template.ownershipFormId,
    economicBranchId: template.economicBranchId,
    economicBranchNCEAId: template.economicBranchNCEAId,
    mainSubjectOfActivity: template.mainSubjectOfActivity,
    segmentIds: template.segmentIds,
    address: template.address,
    bankingDetails: template.bankingDetails,
    managers,
    relatedCustomers: template.relatedCustomers,
    owner: template.owner,
    communicationData,
    accountManagers: template.accountManagers,
    customerEditContractRequests: template.customerEditContractRequests ?? [],
  };

  const putRes = await Request.put(`${Endpoints.customer}/${customerId}`, { data: editPayload });
  await expect(putRes).CheckResponse();

  const verifyRes = await Request.get(`${Endpoints.customer}/${customerId}`);
  await expect(verifyRes).CheckResponse();
  const verifyBody = (await verifyRes.json()) as Record<string, unknown>;
  expect(
    communicationDataHasActiveEmail(verifyBody),
    'PUT /customer must leave no ACTIVE EMAIL contact before calculate-tax',
  ).toBe(false);
}

export async function createSupplyChain(fx: Pdt3179Fx, opts: Pdt3179SupplyOpts = {}): Promise<{
  customerId: number;
  podId: number;
  contractId: number;
  communicationId: number | undefined;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const electricityPayload = GeneratePayload.productAndServices.electricity();
  const electricity = await Request.post(Endpoints.priceComponent, { data: electricityPayload });
  await expect(electricity).CheckResponse();
  Responses.priceComponent.push(await electricity.json());

  const productPayload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.priceComponentIds = [entityId(Responses.priceComponent[0])];
  productPayload.interimAdvancePayments = [];
  productPayload.interimAdvancePaymentGroups = [];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const customerPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  applyCustomerContacts(customerPayload, opts);
  const customer = await Request.post(Endpoints.customer, { data: customerPayload });
  await expect(customer).CheckResponse();
  const customerBody = await customer.json();
  Responses.customer.push(customerBody);

  const pod = await Request.post(Endpoints.pod, { data: GeneratePayload.pointsOfDelivery.pod_settlement() });
  await expect(pod).CheckResponse();
  const podBody = await pod.json();
  Responses.pod.push(podBody);

  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
  (contractPayload.productParameters as Record<string, unknown>).contractType = 'SUPPLY_ONLY';
  const monthly = asNumber((podBody as { estimatedMonthlyAvgConsumption?: unknown }).estimatedMonthlyAvgConsumption) || 1;
  contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = (monthly * 12) / 1000;
  const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(contract).CheckResponse();
  const contractBody = await contract.json();
  Responses.productContract.push(contractBody);

  const activation = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(0, monthStartYmd()),
  });
  await expect(activation).CheckResponse();

  return {
    customerId: entityId(customerBody),
    podId: entityId(podBody),
    contractId: entityId(contractBody),
    communicationId: nestedId(
      (customerBody as { communicationData?: { id?: unknown }[] }).communicationData?.[0],
    ),
  };
}

/** billingGroups[0].id, else contractPodsResponses[0].billingGroupId (REG-1017 / PDT-2971). */
export function resolveContractBillingGroupId(contractBody: Record<string, unknown>): number {
  const groups = contractBody.billingGroups;
  if (Array.isArray(groups) && groups.length > 0) {
    const id = nestedId(groups[0]);
    if (id) return id;
  }
  const pods = contractBody.contractPodsResponses;
  if (Array.isArray(pods) && pods.length > 0) {
    const row = pods[0] as Record<string, unknown>;
    const id = nestedId(row.billingGroupId);
    if (id) return id;
  }
  throw new Error(
    `Cannot resolve billingGroupId from product contract: ${JSON.stringify(contractBody)?.slice(0, 400)}`,
  );
}

/**
 * Invoice-less unpaid liability for RFD. dueDate = yesterday so reminder
 * liabilitiesMaxDueDate (yesterday) includes it. billingGroupId is required for
 * calculate-tax fetchLiabilities (cl.contract_billing_group_id).
 */
export async function createOverdueManualLiabilityWithBillingGroup(fx: Pdt3179Fx): Promise<{
  liabilityId: number;
  billingGroupId: number;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const contractId = entityId(Responses.productContract[0]);
  const contractGet = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractBody = (await contractGet.json()) as Record<string, unknown>;
  const billingGroupId = resolveContractBillingGroupId(contractBody);
  expect(billingGroupId, 'contract billingGroupId').toBeGreaterThan(0);

  const yesterday = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  const payload = GeneratePayload.receivablesManagement.customer_liability();
  payload.billingGroupId = billingGroupId;
  payload.initialAmount = PDT_3179_MANUAL_LIABILITY_AMOUNT;
  payload.dueDate = yesterday;
  payload.occurrenceDate = yesterday;

  const post = await Request.post(Endpoints.customerLiability, { data: payload });
  await expect(post).CheckResponse();
  const liabilityId = entityId(await post.json());
  Responses.customerLiability.push(liabilityId);

  const getLiab = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
  await expect(getLiab).CheckResponse();
  const liab = (await getLiab.json()) as Record<string, unknown>;
  expect(asNumber(liab.currentAmount), 'manual liability currentAmount > 0').toBeGreaterThan(0);
  const gotBillingGroupId = nestedId(liab.billingGroupResponse);
  expect(gotBillingGroupId, 'GET liability billingGroupResponse.id').toBe(billingGroupId);

  return { liabilityId, billingGroupId };
}

function logElapsed(started: number, message: string): void {
  console.log(`[PDT-3179] ${Date.now() - started}ms ${message}`);
}

async function reminderStatusOf(
  fx: Pdt3179Fx,
  reminderId: number,
): Promise<string | undefined> {
  try {
    const reminderGet = await fx.Request.get(
      `power-supply-disconnection-reminder/${reminderId}`,
      { timeout: 3_000 },
    );
    if (!reminderGet.ok()) return undefined;
    return ((await reminderGet.json()) as { reminderStatus?: string }).reminderStatus;
  } catch {
    return undefined;
  }
}

/**
 * POST reminder with this customer (INCLUDED) + set-customer-send-time.
 * Does not call global /job (Dev-wide execute() SQL, ~8–10 min, saturates GET/RFD).
 */
export async function createExecutedReminder(fx: Pdt3179Fx): Promise<number> {
  const started = Date.now();
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.receivablesManagement.reminderForDisconnection();
  const identifier = customerIdentifier(Responses);
  expect(identifier.length, 'reminder customerList must be this test customer identifier').toBeGreaterThan(0);
  payload.customerList = identifier;
  payload.customerFilterType = 'INCLUDED';
  payload.documentTemplateId = null;
  const res = await Request.post(Endpoints.reminderForDisconnection, { data: payload });
  await expect(res).CheckResponse();
  const reminderId = entityId(await res.json());
  Responses.reminderForDisconnection.push(reminderId);
  logElapsed(started, `POST reminder ${reminderId} customerList=${identifier}`);

  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const hours = georgianTime.getHours();
  const minutes = georgianTime.getMinutes();

  const timeOffset = await Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${reminderId}&hour=${hours}&minute=${minutes}`,
    { timeout: 5_000 },
  );
  await expect(timeOffset).CheckResponse();
  logElapsed(started, 'set-customer-send-time');

  const reminderDeadline = Date.now() + 20_000;
  while (Date.now() < reminderDeadline) {
    const lastStatus = await reminderStatusOf(fx, reminderId);
    if (lastStatus === 'EXECUTED') {
      logElapsed(started, `reminder ${reminderId} already EXECUTED`);
      return reminderId;
    }
    await new Promise((r) => setTimeout(r, 1_000));
  }
  logElapsed(started, `reminder ${reminderId} still not EXECUTED (no /job)`);
  return reminderId;
}

/** RFD requires reminderForDisconnectionId in the EXECUTED list (Swagger @NotNull). */
export async function resolveReminderIdForRfd(fx: Pdt3179Fx): Promise<number> {
  const createdId = entityId(fx.Responses.reminderForDisconnection[0]);
  if ((await reminderStatusOf(fx, createdId)) === 'EXECUTED') return createdId;

  const listRes = await fx.Request.get(
    `${fx.Endpoints.reminderForDisconnection}/list?page=0&size=5&statuses=EXECUTED&direction=DESC`,
    { timeout: 8_000 },
  );
  await expect(listRes).CheckResponse();
  const rows = listingContent(await listRes.json());
  expect(rows.length, 'need at least one EXECUTED reminder for RFD').toBeGreaterThan(0);
  const listedId = entityId(rows[0]);
  console.log(
    `[PDT-3179] RFD uses EXECUTED reminder ${listedId} (created reminder ${createdId} is not EXECUTED)`,
  );
  return listedId;
}

export async function resolvePodIdentifier(fx: Pdt3179Fx): Promise<string> {
  const pod = fx.Responses.pod[0] as Record<string, unknown> | undefined;
  const fromCreate = String(pod?.identifier ?? '');
  if (fromCreate.length > 0) return fromCreate;
  const podId = entityId(pod);
  const getRes = await fx.Request.get(`${fx.Endpoints.pod}/${podId}`);
  await expect(getRes).CheckResponse();
  const identifier = String(((await getRes.json()) as { identifier?: unknown }).identifier ?? '');
  expect(identifier.length, 'POD identifier').toBeGreaterThan(0);
  return identifier;
}

/**
 * Build the RFD pods[] row from this test's customer + POD + liability.
 * fetchLiabilities parses liabilitiesInPod as amount-currency-liabilityNumber
 * (CONCAT in customersForDPS; substring after the second hyphen = liability.number).
 * podDetailId must be pod.pod_details.id, not pod.id — otherwise calculate-tax returns no rows
 * and RFD stays EXECUTED. Skip Load PODs GET (same heavy SQL as allSelected true).
 */
export async function fetchCheckedPodRowForRfd(fx: Pdt3179Fx): Promise<Record<string, unknown>> {
  const podIdentifier = await resolvePodIdentifier(fx);
  const customer = fx.Responses.customer[0] as Record<string, unknown>;
  const pod = fx.Responses.pod[0] as Record<string, unknown>;
  const podId = entityId(pod);
  const customerId = entityId(customer);
  const identifier = String(customer.identifier ?? '');
  const contractId = entityId(fx.Responses.productContract[0]);
  const liabilityId = entityId(fx.Responses.customerLiability[0]);

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
  expect(podDetailId, 'podDetailId from POST /pod or contractPodsResponses').toBeGreaterThan(0);

  const liab = await getLiability(fx, liabilityId);
  const liabilityNumber = String(liab.number ?? '');
  expect(liabilityNumber.length, 'GET /customer-liability number').toBeGreaterThan(0);
  const amount = asNumber(liab.currentAmount) || PDT_3179_MANUAL_LIABILITY_AMOUNT;
  const currencyName = String(
    (liab.currencyResponse as { name?: unknown } | undefined)?.name ?? 'BGN',
  ).replace(/-/g, ' ');
  const liabilityToken = `${amount}-${currencyName}-${liabilityNumber}`;

  const bp = (contractBody.basicParameters ?? {}) as Record<string, unknown>;
  const contractNumber = String(bp.contractNumber ?? contractId);
  const bg = Array.isArray(contractBody.billingGroups)
    ? (contractBody.billingGroups[0] as Record<string, unknown>)
    : undefined;
  const billingGroupLabel = String(bg?.groupNumber ?? bg?.number ?? nestedId(bg) ?? '');

  return {
    podId,
    customerId,
    podIdentifier: String(body.identifier ?? podIdentifier),
    isChecked: true,
    isHighestConsumption: false,
    existingCustomerReceivables: true,
    gridOperatorId: nestedId(body.gridOperatorId) ?? envVariables.grid_operator,
    podDetailId,
    customerNumber: identifier,
    customers: identifier,
    contracts: contractNumber,
    billingGroups: billingGroupLabel || contractNumber,
    liabilitiesInBillingGroup: liabilityToken,
    liabilitiesInPod: liabilityToken,
    liabilityAmountCustomer: amount,
  };
}

export async function createExecutedRfd(
  fx: Pdt3179Fx,
  opts: { status?: 'DRAFT' | 'EXECUTED' } = {},
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const started = Date.now();
  const identifier = customerIdentifier(Responses);
  expect(identifier.length, 'RFD listOfCustomer requires this test customer identifier').toBeGreaterThan(0);
  const podRow = await fetchCheckedPodRowForRfd(fx);
  logElapsed(started, 'POD row for RFD ready');
  const payload = GeneratePayload.receivablesManagement.requestForDisconnection();
  payload.reminderForDisconnectionId = await resolveReminderIdForRfd(fx);
  logElapsed(started, `RFD reminderForDisconnectionId=${payload.reminderForDisconnectionId}`);
  payload.gridOpRequestRegDate = todayYmd();
  payload.disconnectionRequestsStatus = opts.status ?? 'EXECUTED';
  payload.supplierType = 'CURRENT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = identifier;
  payload.allSelected = false;
  payload.pods = [podRow];
  payload.podWithHighestConsumption = false;
  // Empty templates: generateDocuments returns immediately (no RFD PDF). PDT-3179 asserts
  // reconnection invoice emails from calculate-tax, not RFD letters.
  payload.templateIds = [];
  const res = await Request.post(Endpoints.requestForDisconnection, {
    data: payload,
    timeout: PDT_3179_RFD_POST_TIMEOUT_MS,
  });
  await expect(res).CheckResponse();
  const requestId = entityId(await res.json());
  Responses.requestForDisconnection.push(requestId);
  logElapsed(started, `POST RFD ${requestId}`);
  return requestId;
}

export async function calculateTax(fx: Pdt3179Fx, requestId: number) {
  const started = Date.now();
  const res = await fx.Request.post(
    `${fx.Endpoints.requestForDisconnection}/calculate-tax/${requestId}`,
    { timeout: 60_000 },
  );
  logElapsed(started, `calculate-tax ${requestId} HTTP ${res.status()}`);
  return res;
}

export async function getRfd(fx: Pdt3179Fx, requestId: number): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${fx.Endpoints.requestForDisconnection}/${requestId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function createExecutedDps(fx: Pdt3179Fx, opts: Pdt3179DpsOpts): Promise<{
  status: number;
  id: number | null;
  text: string;
  json: Record<string, unknown> | null;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.receivablesManagement.disconnectionOfPowerSupply();
  payload.requestForDisconnectionId = opts.requestId ?? entityId(Responses.requestForDisconnection[0]);
  payload.saveType = 'EXECUTED';
  payload.disconnectedRequest[0].customerId = entityId(Responses.customer[0]);
  payload.disconnectedRequest[0].podId = entityId(Responses.pod[0]);
  payload.disconnectedRequest[0].gridOperatorTaxesId = opts.taxId;
  payload.disconnectedRequest[0].expressReconnection = Boolean(opts.express);
  payload.disconnectedRequest[0].dateOfDisconnection = todayYmd();
  const res = await Request.post(Endpoints.disconnectionOfPowerSupply, { data: payload });
  const status = res.status();
  if (status >= 200 && status < 300) {
    await expect(res).CheckResponse();
    const body = await res.json();
    const id = entityId(body);
    Responses.disconnectionOfPowerSupply.push(id);
    return { status, id, text: '', json: typeof body === 'object' ? body : { id: body } };
  }
  const parsed = await readHttpBody(res);
  return { status: parsed.status, id: null, text: parsed.text, json: parsed.json };
}

export async function putExecutedDps(
  fx: Pdt3179Fx,
  dpsId: number,
  opts: Pdt3179DpsOpts,
): Promise<ReturnType<typeof readHttpBody>> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.receivablesManagement.disconnectionOfPowerSupply();
  payload.requestForDisconnectionId = opts.requestId ?? entityId(Responses.requestForDisconnection[0]);
  payload.saveType = 'EXECUTED';
  payload.disconnectedRequest[0].customerId = entityId(Responses.customer[0]);
  payload.disconnectedRequest[0].podId = entityId(Responses.pod[0]);
  payload.disconnectedRequest[0].gridOperatorTaxesId = opts.taxId;
  payload.disconnectedRequest[0].expressReconnection = Boolean(opts.express);
  payload.disconnectedRequest[0].dateOfDisconnection = todayYmd();
  const res = await Request.put(`${Endpoints.disconnectionOfPowerSupply}/${dpsId}`, { data: payload });
  return readHttpBody(res);
}

export async function getDps(fx: Pdt3179Fx, dpsId: number): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${fx.Endpoints.disconnectionOfPowerSupply}/${dpsId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function getInvoice(fx: Pdt3179Fx, id: number): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`invoice?id=${id}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function listInvoicesByNumber(
  fx: Pdt3179Fx,
  invoiceNumber: string,
): Promise<Record<string, unknown>[]> {
  const res = await fx.Request.post(`${fx.Endpoints.invoice}/listing`, {
    data: {
      page: 0,
      size: 50,
      prompt: invoiceNumber,
      searchBy: 'INVOICE_NUMBER',
    },
  });
  await expect(res).CheckResponse();
  return listingContent(await res.json());
}

export async function listLiabilities(
  fx: Pdt3179Fx,
  identifier: string,
): Promise<Record<string, unknown>[]> {
  const res = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=50&columns=ID&direction=DESC&prompt=${encodeURIComponent(identifier)}&searchFields=CUSTOMER`,
  );
  await expect(res).CheckResponse();
  return listingContent(await res.json());
}

export async function getLiability(fx: Pdt3179Fx, id: number): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${fx.Endpoints.customerLiability}/${id}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

/** Listing DTO has no invoiceId; GET CustomerLiabilityResponse.invoiceResponse.id. */
export async function findLiabilityByInvoiceId(
  fx: Pdt3179Fx,
  invoiceId: number,
): Promise<Record<string, unknown> | undefined> {
  const rows = await listLiabilities(fx, customerIdentifier(fx.Responses));
  for (const row of rows) {
    const liabilityId = nestedId(row.id) ?? nestedId(row);
    if (!liabilityId) continue;
    const full = await getLiability(fx, liabilityId);
    if (nestedId(full.invoiceResponse) === invoiceId) return full;
  }
  return undefined;
}

export async function listEmails(
  fx: Pdt3179Fx,
  identifier: string,
): Promise<Record<string, unknown>[]> {
  const nested = `${fx.Endpoints.email}/list?request.page=0&request.size=50&request.prompt=${encodeURIComponent(identifier)}&request.searchBy=CUSTOMER_IDENTIFIER`;
  let res = await fx.Request.get(nested);
  if (!res.ok()) {
    res = await fx.Request.get(
      `${fx.Endpoints.email}/list?page=0&size=50&prompt=${encodeURIComponent(identifier)}&searchBy=CUSTOMER_IDENTIFIER`,
    );
  }
  await expect(res).CheckResponse();
  return listingContent(await res.json());
}

export async function getEmail(fx: Pdt3179Fx, id: number): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${fx.Endpoints.email}/${id}`, { params: { type: 'EMAIL' } });
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function collectCustomerInvoices(
  fx: Pdt3179Fx,
): Promise<Record<string, unknown>[]> {
  const created = fx.Responses.customer[0] as Record<string, unknown>;
  let detailId = asNumber(created.lastCustomerDetailId);
  if (!detailId) {
    const getRes = await fx.Request.get(`${fx.Endpoints.customer}/${entityId(created)}`);
    await expect(getRes).CheckResponse();
    const body = (await getRes.json()) as Record<string, unknown>;
    detailId = asNumber(body.lastCustomerDetailId);
  }
  expect(detailId, 'customerDetailId for POST /customer/{id}/customer-invoices').toBeGreaterThan(0);
  const res = await fx.Request.post(`${fx.Endpoints.customer}/${detailId}/customer-invoices`, {
    data: { page: 0, size: 50 },
  });
  await expect(res).CheckResponse();
  const invoices: Record<string, unknown>[] = [];
  const seen = new Set<number>();
  for (const row of listingContent(await res.json())) {
    const invoiceId = asNumber(row.invoiceId) || nestedId(row.id) || nestedId(row) || 0;
    if (!invoiceId || seen.has(invoiceId)) continue;
    seen.add(invoiceId);
    invoices.push(await getInvoice(fx, invoiceId));
  }
  return invoices;
}

/** InvoiceResponse has no reversalCreatedFromId; CREDIT_NOTE ↔ original is debitCredits[].id. */
export function reversalSourceId(invoice: Record<string, unknown>): number {
  const direct = asNumber(invoice.reversalCreatedFromId);
  if (direct > 0) return direct;
  const credits = invoice.debitCredits;
  if (!Array.isArray(credits)) return 0;
  for (const row of credits) {
    if (!row || typeof row !== 'object') continue;
    const id = nestedId(row) ?? asNumber((row as { id?: unknown }).id);
    if (id > 0) return id;
  }
  return 0;
}

export function invoicesOfType(
  invoices: Record<string, unknown>[],
  invoiceType: string,
): Record<string, unknown>[] {
  return invoices.filter((inv) => String(inv.invoiceType ?? '') === invoiceType);
}

export async function countReconnectionInvoices(fx: Pdt3179Fx): Promise<number> {
  return invoicesOfType(await collectCustomerInvoices(fx), 'RECONNECTION').length;
}

/**
 * Invoice Reversal billing run for a REAL reconnection invoice (Kalina / PDT-3179).
 * Uses GeneratePayload.billing.reversalBilling + waitForInvoiceGeneration
 * (PATCH start-billing → DRAFT → start-generating → GENERATED → start-accounting → COMPLETED).
 * executionType MANUAL so create() does not also start IMMEDIATELY.
 */
export async function completeReconnectionInvoiceReversalBillingRun(
  fx: Pdt3179Fx,
  invoiceId: number,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  Responses.invoice.push(invoiceId);
  const payload = await GeneratePayload.billing.reversalBilling(Responses.invoice.length - 1);
  payload.commonParameters.executionType = 'MANUAL';
  payload.commonParameters.sendingAnInvoice = 'EMAIL';
  const res = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const billingRunId = Number(await res.json());
  expect(billingRunId, 'INVOICE_REVERSAL billing run id').toBeGreaterThan(0);
  Responses.billingRun.push(billingRunId);
  await GeneratePayload.billing.waitForInvoiceGeneration(
    true,
    true,
    1,
    Responses.billingRun.length - 1,
  );
  return billingRunId;
}

export function invoiceHasPdf(invoice: Record<string, unknown>): boolean {
  const files = invoice.file ?? invoice.files ?? invoice.fileResponse ?? invoice.invoiceDocumentFile;
  if (Array.isArray(files)) return files.length > 0;
  if (files && typeof files === 'object') return true;
  const blob = JSON.stringify(invoice);
  return /fileName|pdf|fileResponse|documentFile/i.test(blob);
}

export function emailBlob(email: Record<string, unknown>): string {
  return JSON.stringify(email);
}

export function emailHasTopicInvoice(email: Record<string, unknown>): boolean {
  const topic =
    email.topicOfCommunicationShortResponse ??
    email.communicationTopic ??
    email.topic ??
    email.topicName ??
    email.communicationTopicName;
  if (typeof topic === 'string') return /invoice/i.test(topic);
  if (topic && typeof topic === 'object') {
    const name = (topic as { name?: unknown; topicName?: unknown; shortName?: unknown }).name
      ?? (topic as { topicName?: unknown }).topicName
      ?? (topic as { shortName?: unknown }).shortName;
    if (name != null) return /invoice/i.test(String(name));
  }
  return /invoice/i.test(emailBlob(email));
}

export function emailRecipientText(email: Record<string, unknown>): string {
  const direct = email.customerEmailAddress ?? email.emailAddress ?? email.customerEmail ?? email.recipient;
  if (direct != null && (typeof direct === 'string' || typeof direct === 'number')) return String(direct);
  const keys = [
    'customerEmailAddress',
    'customerEmail',
    'email',
    'emailAddress',
    'recipient',
    'recipients',
    'communicationData',
    'to',
  ];
  const parts: string[] = [];
  for (const key of keys) {
    if (email[key] != null) parts.push(JSON.stringify(email[key]));
  }
  return `${parts.join(' ')} ${emailBlob(email)}`;
}

export function emailHasAttachment(email: Record<string, unknown>): boolean {
  const att =
    email.attachmentsShortResponse ??
    email.filesShortResponse ??
    email.attachments ??
    email.files;
  if (Array.isArray(att)) return att.length > 0;
  const blob = emailBlob(email).toLowerCase();
  return blob.includes('attachment') || blob.includes('filename') || blob.includes('pdf');
}

export function emailTemplateIdOf(email: Record<string, unknown>): number | undefined {
  return (
    nestedId(email.emailTemplateId) ??
    nestedId(email.emailTemplateResponse) ??
    nestedId(email.templateId)
  );
}

/** Rows in `after` whose emailCommunicationId was not in `before` (action delta, not global newest). */
export function emailsCreatedAfter(
  before: Record<string, unknown>[],
  after: Record<string, unknown>[],
): Record<string, unknown>[] {
  const beforeIds = new Set(before.map((row) => emailCommunicationId(row)));
  return after.filter((row) => !beforeIds.has(emailCommunicationId(row)));
}

function templatePurposeFromView(src: Record<string, unknown>): string {
  const raw = src.purpose ?? src.templatePurpose;
  if (typeof raw === 'string' && raw.length > 0) return raw;
  if (raw && typeof raw === 'object' && 'name' in raw) {
    const name = String((raw as { name?: unknown }).name ?? '');
    if (name.length > 0) return name;
  }
  return 'INVOICE';
}

function templateEditPayload(
  body: Record<string, unknown>,
  fileId: number,
  status: 'ACTIVE' | 'INACTIVE',
  fallbackName: string,
  fallbackSubject: unknown,
): Record<string, unknown> {
  return {
    name: body.name ?? fallbackName,
    templateType: 'EMAIL',
    templateStatus: status,
    language: body.language ?? 'BULGARIAN',
    subject: body.subject ?? fallbackSubject,
    fileId: nestedId(body.fileId) ?? nestedId(body.file) ?? fileId,
    customerTypes: body.customerTypes ?? null,
    consumptionPurposes: body.consumptionPurposes ?? body.purposeOfConsumptions ?? null,
    outputFileFormat: body.outputFileFormat ?? body.outputFileFormats ?? [],
    fileNames: body.fileNames ?? body.fileName ?? [],
    fileNamePrefix: body.fileNamePrefix ?? null,
    fileNameSuffix: body.fileNameSuffix ?? null,
    fileSignings: body.fileSignings ?? body.fileSigning ?? [],
    quantity: body.quantity ?? null,
  };
}

export async function postInactiveEmailTemplateClone(
  fx: Pdt3179Fx,
  sourceTemplateId: number,
): Promise<{ templateId: number; deactivate: () => Promise<void>; restore: () => Promise<void> }> {
  const { Request, FileUploadRequest, Endpoints } = fx;
  if (!FileUploadRequest) {
    throw new Error('FileUploadRequest fixture is required to clone an email template file');
  }
  const getRes = await Request.get(`${Endpoints.template}/${sourceTemplateId}`);
  await expect(getRes).CheckResponse();
  const src = (await getRes.json()) as Record<string, unknown>;
  const sourceFileId = nestedId(src.fileId) ?? nestedId(src.file);
  expect(sourceFileId, 'source email template fileId').toBeTruthy();
  const templatePurpose = templatePurposeFromView(src);

  let download = await Request.get(
    `${Endpoints.template}/${sourceTemplateId}/download-template-file?id=${sourceFileId}`,
  );
  if (!download.ok()) {
    download = await Request.get(`${Endpoints.template}/download-template-file?id=${sourceFileId}`);
  }
  await expect(download).CheckResponse();
  const fileBuffer = Buffer.from(await download.body());
  expect(fileBuffer.length, 'downloaded source template file').toBeGreaterThan(0);

  const upload = await FileUploadRequest.post('template/upload-template-file?fileFormats=DOCX', {
    multipart: {
      file: {
        name: `pdt-3179-email-${Date.now()}.docx`,
        mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        buffer: fileBuffer,
      },
    },
  });
  await expect(upload).CheckResponse();
  const uploaded = (await upload.json()) as { id?: number };
  const fileId = nestedId(uploaded) ?? asNumber(uploaded.id);
  expect(fileId, 'cloned template fileId').toBeGreaterThan(0);

  const createPayload = {
    name: `PDT-3179-EMAIL-${Date.now()}`,
    templateType: 'EMAIL',
    templateStatus: 'ACTIVE',
    templatePurpose,
    language: src.language ?? 'BULGARIAN',
    subject: src.subject ?? 'PDT-3179 reconnection invoice',
    fileId,
    customerTypes: src.customerTypes ?? null,
    consumptionPurposes: src.consumptionPurposes ?? src.purposeOfConsumptions ?? null,
    outputFileFormat: src.outputFileFormat ?? src.outputFileFormats ?? [],
    fileNames: src.fileNames ?? src.fileName ?? [],
    fileNamePrefix: src.fileNamePrefix ?? null,
    fileNameSuffix: src.fileNameSuffix ?? null,
    fileSignings: src.fileSignings ?? src.fileSigning ?? [],
    quantity: src.quantity ?? null,
    defaultGoodsOrderDocument: null,
    defaultGoodsOrderEmail: null,
    defaultLatePaymentFineDocument: null,
    defaultLatePaymentFineEmail: null,
  };
  const postRes = await fx.Request.post(fx.Endpoints.template, { data: createPayload });
  await expect(postRes).CheckResponse();
  const templateId = entityId(await postRes.json());

  const putTemplateStatus = async (
    status: 'ACTIVE' | 'INACTIVE',
    required: boolean,
  ): Promise<void> => {
    const current = await fx.Request.get(`${fx.Endpoints.template}/${templateId}`);
    if (!current.ok()) {
      if (required) await expect(current).CheckResponse();
      return;
    }
    const body = (await current.json()) as Record<string, unknown>;
    const vid = asNumber(body.versionId ?? body.version) || 1;
    const res = await fx.Request.put(`${fx.Endpoints.template}/${templateId}?versionId=${vid}`, {
      data: templateEditPayload(body, fileId, status, createPayload.name, createPayload.subject),
    });
    if (required) await expect(res).CheckResponse();
  };

  return {
    templateId,
    deactivate: async () => {
      await putTemplateStatus('INACTIVE', true);
    },
    restore: async () => {
      await putTemplateStatus('ACTIVE', false);
    },
  };
}

export function pdt3179RelevantKeys(): Array<
  'customer' | 'product' | 'productContract' | 'invoice'
> {
  return ['customer', 'product', 'productContract', 'invoice'];
}

export async function runPdt3179ReceivableChain(
  fx: Pdt3179Fx,
  opts: Pdt3179SupplyOpts = {},
): Promise<void> {
  const started = Date.now();
  await test.step('Precondition: terms, electricity PC, product, customer, POD, contract, activate POD', async () => {
    await createSupplyChain(fx, opts);
    logElapsed(started, 'supply chain done');
  });
  await test.step('Precondition: overdue manual liability with billing group', async () => {
    await createOverdueManualLiabilityWithBillingGroup(fx);
    logElapsed(started, 'manual liability done');
  });
  await test.step('Precondition: reminder for disconnection (this customer, no global /job)', async () => {
    await createExecutedReminder(fx);
    logElapsed(started, 'reminder POST done');
  });
}

export function dpsExpressFlag(dps: Record<string, unknown>): boolean | undefined {
  if (typeof dps.expressReconnection === 'boolean') return dps.expressReconnection;
  const rows = dps.disconnectedRequest ?? dps.disconnectedRequests ?? dps.table ?? dps.content;
  if (Array.isArray(rows) && rows[0] && typeof rows[0] === 'object') {
    const flag = (rows[0] as { expressReconnection?: unknown }).expressReconnection;
    if (typeof flag === 'boolean') return flag;
  }
  return undefined;
}

/** GET /disconnection-of-power-supply/{id} has no expressReconnection (Swagger). Use table/executed. */
export async function getDpsExpressFlag(fx: Pdt3179Fx, dpsId: number): Promise<boolean | undefined> {
  const fromView = dpsExpressFlag(await getDps(fx, dpsId));
  if (typeof fromView === 'boolean') return fromView;
  const nested = `${fx.Endpoints.disconnectionOfPowerSupply}/table/executed?request.disconnectionId=${dpsId}&request.page=0&request.size=25`;
  let res = await fx.Request.get(nested);
  if (!res.ok()) {
    res = await fx.Request.get(
      `${fx.Endpoints.disconnectionOfPowerSupply}/table/executed?disconnectionId=${dpsId}&page=0&size=25`,
    );
  }
  await expect(res).CheckResponse();
  const rows = listingContent(await res.json());
  if (!rows.length) return undefined;
  const flag = rows[0].expressReconnection;
  return typeof flag === 'boolean' ? flag : undefined;
}

export async function postDpsRaw(fx: Pdt3179Fx, opts: Pdt3179DpsOpts): Promise<{
  status: number;
  text: string;
  json: Record<string, unknown> | null;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = GeneratePayload.receivablesManagement.disconnectionOfPowerSupply();
  payload.requestForDisconnectionId = opts.requestId ?? entityId(Responses.requestForDisconnection[0]);
  payload.saveType = 'EXECUTED';
  payload.disconnectedRequest[0].customerId = entityId(Responses.customer[0]);
  payload.disconnectedRequest[0].podId = entityId(Responses.pod[0]);
  payload.disconnectedRequest[0].gridOperatorTaxesId = opts.taxId;
  payload.disconnectedRequest[0].expressReconnection = Boolean(opts.express);
  payload.disconnectedRequest[0].dateOfDisconnection = todayYmd();
  const res = await Request.post(Endpoints.disconnectionOfPowerSupply, { data: payload });
  return readHttpBody(res);
}
