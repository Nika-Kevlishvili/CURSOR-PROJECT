/**
 * PDT-3187 — semantic twin: billing group still references a DELETED customer
 * communication header → BillingRunEmailSenderService finds no ACTIVE EMAIL
 * contacts → no CRM email-communication objects after REAL invoice.
 *
 * Expected Result (bug twin): zero CRM email-communication objects for the
 * customer after REAL invoice. Do NOT require invoice.customerCommunicationId
 * === deleted header id (that is not a Missing prerequisite).
 *
 * Portable via BASE_URL (DEV / DEV2 / TEST). Do NOT hardcode Prod IDs or PII.
 *
 * Reference spec(s):
 * - tests/billing/electricity/withElectricity(Product).spec.ts ([REG-991])
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts (contract prep + REAL assert)
 * - tests/cursor/pdt-3054-mass-comm-contract-import-comm-data.fixtures.ts (customer PUT communicationData patterns)
 * - tests/cursor/reg-1172-volume-with-electricity.fixtures.ts (BILLING/CONTRACT comm resolution)
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import {
  resolvePdt2376BillingAnchor,
  type Pdt2376BillingAnchor,
} from './pdt-2376-volume-with-electricity.fixtures';

type FixtureRequest = baseFixture['Request'];
type Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export const PDT_3187_JIRA_KEY = 'PDT-3187';
export const PDT_3187_BILLING_TAG = 'PDT3187-BILLING';
export const PDT_3187_CONTRACT_TAG = 'PDT3187-CONTRACT';

export type Pdt3187CommIds = {
  billingCommId: number;
  contractCommId: number;
};

export type Pdt3187ScenarioContext = {
  customerId: number;
  customerIdentifier: string;
  customerDetailsVersion: number;
  createPayload: Record<string, unknown>;
  commIds: Pdt3187CommIds;
  billingGroupId: number;
  contractId: number;
  contractNumber: string;
  billingAnchor: Pdt2376BillingAnchor;
  billingRunId: number | null;
  invoiceIds: number[];
  realInvoiceAchieved: boolean;
  missingPrerequisite: string | null;
};

type CommContact = {
  id?: number;
  sendSms?: boolean;
  platformId?: number | null;
  status: string;
  contactType: string;
  contactValue: string;
};

function cloneJson<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function buildCommEntry(opts: {
  tag: string;
  purposeId: number;
  email: string;
  mobile: string;
  address: Record<string, unknown>;
  id?: number;
  status?: 'ACTIVE' | 'DELETED';
  contacts?: CommContact[];
}): Record<string, unknown> {
  return {
    ...(opts.id != null ? { id: opts.id } : {}),
    status: opts.status ?? 'ACTIVE',
    contactTypeName: opts.tag,
    contactPurposes: [{ contactPurposeId: opts.purposeId, status: 'ACTIVE' }],
    address: cloneJson(opts.address),
    communicationContacts:
      opts.contacts ??
      ([
        {
          sendSms: true,
          platformId: envVariables.platform ?? null,
          status: 'ACTIVE',
          contactType: 'MOBILE_NUMBER',
          contactValue: opts.mobile,
        },
        {
          sendSms: false,
          platformId: envVariables.platform ?? null,
          status: 'ACTIVE',
          contactType: 'EMAIL',
          contactValue: opts.email,
        },
      ] as CommContact[]),
    contactPersons: [],
  };
}

/** Synthetic legal-entity customer with distinct BILLING + CONTRACT communication headers. */
export function buildPdt3187LegalCustomerPayload(
  GeneratePayload: baseFixture['GeneratePayload'],
): { payload: Record<string, unknown>; billingEmail: string; contractEmail: string } {
  const payload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
  const stamp = `${Date.now()}${Math.floor(Math.random() * 900 + 100)}`;
  const billingEmail = `pdt3187.billing.${stamp}@example.com`;
  const contractEmail = `pdt3187.contract.${stamp}@example.com`;
  const mobileBilling = `555${String(stamp).slice(-6)}`;
  const mobileContract = `556${String(stamp).slice(-6)}`;

  const templateComm = (payload.communicationData as Record<string, unknown>[])?.[0] ?? {};
  const baseAddress =
    (templateComm.address as Record<string, unknown>) ??
    (payload.address as Record<string, unknown>) ??
    {};

  // Prefer unique synthetic UIC from generator; never reuse Prod customer identifiers.
  payload.communicationData = [
    buildCommEntry({
      tag: PDT_3187_BILLING_TAG,
      purposeId: Number(envVariables.billing_purpose),
      email: billingEmail,
      mobile: mobileBilling,
      address: baseAddress,
    }),
    buildCommEntry({
      tag: PDT_3187_CONTRACT_TAG,
      purposeId: Number(envVariables.contact_purpose),
      email: contractEmail,
      mobile: mobileContract,
      address: baseAddress,
    }),
  ];

  return { payload, billingEmail, contractEmail };
}

export async function getCustomerJson(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  customerId: number,
  version = 1,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${Endpoints.customer}/${customerId}?version=${version}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function resolveCommIdsFromCustomer(
  customerJson: Record<string, unknown>,
): Pdt3187CommIds {
  const comms =
    (customerJson.communicationData as { id?: number; contactTypeName?: string; status?: string }[]) ??
    [];
  const billing = comms.find((c) => String(c.contactTypeName ?? '').includes(PDT_3187_BILLING_TAG));
  const contract = comms.find((c) => String(c.contactTypeName ?? '').includes(PDT_3187_CONTRACT_TAG));
  const billingCommId = Number(billing?.id);
  const contractCommId = Number(contract?.id);
  if (!Number.isFinite(billingCommId) || billingCommId <= 0) {
    throw new Error(`[${PDT_3187_JIRA_KEY}] Missing BILLING communication header id after customer create`);
  }
  if (!Number.isFinite(contractCommId) || contractCommId <= 0) {
    throw new Error(`[${PDT_3187_JIRA_KEY}] Missing CONTRACT communication header id after customer create`);
  }
  return { billingCommId, contractCommId };
}

function customerDetailsVersionFromView(view: Record<string, unknown>): number {
  const detail =
    (view.activeCustomerDetail as Record<string, unknown> | undefined) ??
    (view.customerDetail as Record<string, unknown> | undefined);
  const version = Number(
    detail?.versionId ?? view.customerVersionId ?? view.versionId ?? view.lastCustomerDetailVersionId ?? 1,
  );
  if (!Number.isFinite(version) || version <= 0) {
    throw new Error(`[${PDT_3187_JIRA_KEY}] Cannot resolve customerDetailsVersion for edit`);
  }
  return version;
}

/**
 * Resolve ACTIVE customer detail id for BillingGroupRequest.alternativeRecipientCustomerDetailId.
 * Same customer as contract holder (Prod twin pattern).
 */
function resolveCustomerDetailIdFromView(view: Record<string, unknown>): number {
  const details = view.customerDetails as { id?: number }[] | undefined;
  const detail =
    (view.activeCustomerDetail as Record<string, unknown> | undefined) ??
    (view.customerDetail as Record<string, unknown> | undefined) ??
    details?.[0];
  const customerDetailId = Number(
    detail?.id ?? view.lastCustomerDetailId ?? view.customerDetailsId ?? view.customerDetailId,
  );
  if (!Number.isFinite(customerDetailId) || customerDetailId <= 0) {
    throw new Error(
      `[${PDT_3187_JIRA_KEY}] Cannot resolve customerDetailId for billing group alternative recipient`,
    );
  }
  return customerDetailId;
}

/**
 * Soft-delete the BILLING communication header via omit-from-list on customer PUT.
 *
 * Phoenix `CustomerCommunicationsService.editCustomerCommunicationsData`:
 * - Explicit `status=DELETED` on an edit request row is **forbidden** (400:
 *   "Cannot set DELETED status when editing communication data").
 * - Soft-delete path is `deleteRemovedCustomerCommunications`: any ACTIVE
 *   communication id present in DB but **omitted** from the edit request's
 *   `communicationData` list → `deleteCustomerCommunication` sets header status
 *   DELETED (contacts unchanged).
 *
 * Keep CONTRACT (and any other non-billing) headers ACTIVE in the PUT payload.
 * Billing group / invoice FK can still point at the deleted header id (Prod twin).
 *
 * Soft-delete verification (do NOT require CheckResponse on GET by id):
 * - GET `customer-communications/{id}` only loads ACTIVE communication; a soft-deleted
 *   header yields 400 `DomainEntityNotFoundException: Active communication data not found`
 *   — same ACTIVE-only lookup used by BillingRunEmailSenderService — treat as success.
 * - Also accept 200 with `status=DELETED` if the API ever returns deleted rows.
 * - Customer GET must not list the billing header id as ACTIVE.
 * Callers still receive `{ deletedStatus: 'DELETED', customerJson }`.
 */
export async function softDeleteBillingCommunicationHeader(
  fx: Fx,
  opts: {
    customerId: number;
    createPayload: Record<string, unknown>;
    commIds: Pdt3187CommIds;
    customerDetailsVersion: number;
  },
): Promise<{ customerJson: Record<string, unknown>; deletedStatus: string }> {
  const { Request, Endpoints } = fx;
  const current = await getCustomerJson(Request, Endpoints, opts.customerId, opts.customerDetailsVersion);
  const existingComms =
    (current.communicationData as Record<string, unknown>[]) ??
    ((opts.createPayload.communicationData as Record<string, unknown>[]) ?? []);

  // Omit billingCommId — do NOT send status=DELETED (API rejects that on edit).
  const keptComms = existingComms
    .filter((comm) => {
      const id = Number(comm.id);
      const tag = String(comm.contactTypeName ?? '');
      const isBilling =
        id === opts.commIds.billingCommId || tag.includes(PDT_3187_BILLING_TAG);
      return !isBilling;
    })
    .map((comm) => {
      const id = Number(comm.id);
      const contacts = ((comm.communicationContacts as CommContact[]) ?? []).map((c) => ({
        ...c,
        status: c.status ?? 'ACTIVE',
      }));
      return {
        ...comm,
        id,
        status: 'ACTIVE',
        communicationContacts: contacts,
        contactPersons: Array.isArray(comm.contactPersons) ? comm.contactPersons : [],
      };
    });

  // Ensure CONTRACT (and other non-billing) headers stay in the list so only billing is soft-deleted.
  if (!keptComms.some((c) => Number(c.id) === opts.commIds.contractCommId)) {
    const template =
      (opts.createPayload.communicationData as Record<string, unknown>[])?.find((c) =>
        String(c.contactTypeName ?? '').includes(PDT_3187_CONTRACT_TAG),
      ) ?? (opts.createPayload.communicationData as Record<string, unknown>[])?.[1];
    keptComms.push(
      buildCommEntry({
        id: opts.commIds.contractCommId,
        tag: PDT_3187_CONTRACT_TAG,
        purposeId: Number(envVariables.contact_purpose),
        email: 'pdt3187.contract.keep@example.com',
        mobile: '556000001',
        address: (template?.address as Record<string, unknown>) ?? {},
        status: 'ACTIVE',
      }),
    );
  }

  expect(
    keptComms.some((c) => Number(c.id) === opts.commIds.billingCommId),
    `[${PDT_3187_JIRA_KEY}] Soft-delete payload must omit billingCommId=${opts.commIds.billingCommId}`,
  ).toBe(false);

  const editPayload: Record<string, unknown> = {
    ...opts.createPayload,
    customerDetailsVersion: opts.customerDetailsVersion,
    updateExistingVersion: true,
    customerIdentifier:
      current.customerIdentifier ??
      current.identifier ??
      opts.createPayload.customerIdentifier,
    customerDetailStatus: 'ACTIVE',
    communicationData: keptComms,
    customerEditContractRequests: [],
    accountManagers: opts.createPayload.accountManagers ?? [],
    managers: opts.createPayload.managers ?? [],
    relatedCustomers: opts.createPayload.relatedCustomers ?? null,
    owner: opts.createPayload.owner ?? null,
  };

  const put = await Request.put(`${Endpoints.customer}/${opts.customerId}`, { data: editPayload });
  await expect(put).CheckResponse();

  const after = await getCustomerJson(Request, Endpoints, opts.customerId, opts.customerDetailsVersion);

  // Customer GET must not still list the soft-deleted billing header as ACTIVE.
  const afterComms =
    (after.communicationData as { id?: number; status?: string }[]) ?? [];
  const billingStillActive = afterComms.some(
    (c) =>
      Number(c.id) === opts.commIds.billingCommId &&
      String(c.status ?? '').toUpperCase() === 'ACTIVE',
  );
  expect(
    billingStillActive,
    `[${PDT_3187_JIRA_KEY}] Customer GET must not list billingCommId=${opts.commIds.billingCommId} as ACTIVE after omit-from-list soft-delete`,
  ).toBe(false);

  // GET customer-communications/{id} loads ACTIVE rows only (same as
  // BillingRunEmailSenderService.extractContactFromCommunications → findByIdAndStatuses ACTIVE).
  // Soft-deleted headers therefore return 400 DomainEntityNotFoundException
  // "Active communication data not found" — that IS soft-delete evidence, not a failure.
  // Also accept 200 with status=DELETED if the API ever returns deleted rows.
  const viewRes = await Request.get(`customer-communications/${opts.commIds.billingCommId}`);
  const viewStatus = viewRes.status();
  const viewBodyText = await viewRes.text().catch(() => '');
  let deletedStatus = 'DELETED';

  if (viewStatus >= 200 && viewStatus < 300) {
    let viewJson: { status?: string } = {};
    try {
      viewJson = JSON.parse(viewBodyText) as { status?: string };
    } catch {
      viewJson = {};
    }
    deletedStatus = String(viewJson.status ?? '');
    expect(
      deletedStatus.toUpperCase(),
      `[${PDT_3187_JIRA_KEY}] GET customer-communications/${opts.commIds.billingCommId} returned ${viewStatus} but status was not DELETED: ${viewBodyText.slice(0, 500)}`,
    ).toBe('DELETED');
  } else {
    const notFound =
      viewStatus === 400 &&
      /Active communication data not found|communication data not found/i.test(viewBodyText);
    expect(
      notFound,
      `[${PDT_3187_JIRA_KEY}] Soft-delete verify: expected HTTP 400 with "Active communication data not found" ` +
        `(or 200 + status DELETED), got ${viewStatus}: ${viewBodyText.slice(0, 500)}`,
    ).toBe(true);
    deletedStatus = 'DELETED';
  }

  return { customerJson: after, deletedStatus };
}

/**
 * Set BG sendingInvoice=EMAIL and pin billingCustomerCommunicationId to the (soon-deleted) header.
 *
 * BillingGroupService.validateCommunicationId: when billingCustomerCommunicationId is set,
 * alternativeRecipientCustomerDetailId must be a valid ACTIVE customer detail id
 * (existsByDetailIdAndCustomerStatus). Omitting/null still fails that check — pass the
 * contract customer's detail id (Prod twin uses the same customer as alt recipient).
 */
export async function configureBillingGroupEmailWithCommHeader(
  fx: Fx,
  billingCustomerCommunicationId: number,
  alternativeRecipientCustomerDetailId: number,
): Promise<{ billingGroupId: number }> {
  const { Request, GeneratePayload } = fx;
  const payload = await GeneratePayload.contractsAndOrders.editBillingGroup(0);
  payload.sendingInvoice = 'EMAIL';
  payload.billingCustomerCommunicationId = billingCustomerCommunicationId;
  payload.alternativeRecipientCustomerDetailId = alternativeRecipientCustomerDetailId;

  const editRes = await Request.put(`billing-group/${payload.id}`, { data: payload });
  await expect(editRes).CheckResponse();

  const verify = await Request.get(`billing-group/${payload.id}`);
  await expect(verify).CheckResponse();
  // BillingGroupResponse exposes stored billingCustomerCommunicationId as communicationId.
  const bg = (await verify.json()) as {
    id?: number;
    sendingInvoice?: string;
    communicationId?: number;
  };
  expect(bg.sendingInvoice, 'Billing group sendingInvoice must be EMAIL').toBe('EMAIL');
  expect(
    Number(bg.communicationId),
    'Billing group must reference billing communication header id',
  ).toBe(billingCustomerCommunicationId);

  return { billingGroupId: Number(payload.id) };
}

export async function createPdt3187CatalogAndContract(fx: Fx): Promise<{
  customerId: number;
  customerIdentifier: string;
  customerDetailsVersion: number;
  createPayload: Record<string, unknown>;
  commIds: Pdt3187CommIds;
  contractId: number;
  contractNumber: string;
  billingGroupId: number;
  billingAnchor: Pdt2376BillingAnchor;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const term = await Request.post(Endpoints.terms, {
    data: GeneratePayload.productAndServices.term(),
  });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const price = await Request.post(Endpoints.priceComponent, {
    data: GeneratePayload.productAndServices.priceSettlement(),
  });
  await expect(price).CheckResponse();
  Responses.priceComponent.push(await price.json());

  const product = await Request.post(Endpoints.product, {
    data: GeneratePayload.productAndServices.product(),
  });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());

  const built = buildPdt3187LegalCustomerPayload(GeneratePayload);
  const customerRes = await Request.post(Endpoints.customer, { data: built.payload });
  await expect(customerRes).CheckResponse();
  const customerCreated = await customerRes.json();
  Responses.customer.push(customerCreated);

  const customerId = Number(customerCreated.id ?? customerCreated.customerId);
  const customerView = await getCustomerJson(Request, Endpoints, customerId, 1);
  const customerIdentifier = String(
    customerView.customerIdentifier ?? customerView.identifier ?? customerCreated.identifier ?? '',
  );
  const customerDetailsVersion = customerDetailsVersionFromView(customerView);
  const customerDetailId = resolveCustomerDetailIdFromView(customerView);
  const commIds = resolveCommIdsFromCustomer(customerView);

  const pod = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod).CheckResponse();
  Responses.pod.push(await pod.json());

  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
  contractPayload.basicParameters.communicationDataBillingId = commIds.billingCommId;
  contractPayload.basicParameters.communicationDataContractId = commIds.contractCommId;

  const contractRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(contractRes).CheckResponse();
  const contractBody = await contractRes.json();
  Responses.productContract.push(contractBody);

  const contractId = Number(contractBody.id);
  const contractGet = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractJson = await contractGet.json();
  const contractNumber = String(contractJson.basicParameters?.contractNumber ?? '');

  const podActivation = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
  });
  await expect(podActivation).CheckResponse();

  // Same customer as alt recipient (Prod twin); required when billingCustomerCommunicationId is set.
  const { billingGroupId } = await configureBillingGroupEmailWithCommHeader(
    fx,
    commIds.billingCommId,
    customerDetailId,
  );

  const billingAnchor = await resolvePdt2376BillingAnchor(Request);

  return {
    customerId,
    customerIdentifier,
    customerDetailsVersion,
    createPayload: built.payload,
    commIds,
    contractId,
    contractNumber,
    billingGroupId,
    billingAnchor,
  };
}

export async function postPdt3187BillingByProfile(
  fx: Fx,
  anchor: Pdt2376BillingAnchor,
): Promise<void> {
  const { Request, GeneratePayload, Responses } = fx;
  const payload = await GeneratePayload.energyData.profile1Month(0, [
    { startDate: anchor.profileStart, endDate: anchor.profileEnd },
  ]);
  payload.timeZone = 'CET';
  const profiles = await Request.post('billing-by-profile', { data: payload });
  await expect(profiles).CheckResponse();
  const profileData = await profiles.json();
  Responses.dataByProfiles.push({
    id: profileData,
    periodFrom: payload.periodFrom,
    periodTo: payload.periodTo,
    periodType: payload.periodType,
  });
}

export async function createPdt3187StandardBillingRun(
  fx: Fx,
  anchor: Pdt2376BillingAnchor,
): Promise<{ billingRunId: number; payload: Record<string, unknown> }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  billingPayload.commonParameters.sendingAnInvoice = 'ACCORDING_TO_THE_CONTRACT';
  billingPayload.commonParameters.accountingPeriodId = anchor.accountingPeriodId;
  billingPayload.commonParameters.taxEventDate = anchor.taxEventDate;
  billingPayload.commonParameters.invoiceDate = anchor.invoiceDate;
  billingPayload.commonParameters.templateId =
    envVariables.invoice_document_template ?? billingPayload.commonParameters.templateId;
  billingPayload.commonParameters.emailTemplateId =
    envVariables.invoice_email_template ?? billingPayload.commonParameters.emailTemplateId;

  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const body = await billingRun.json();
  Responses.billingRun.push(body);
  const billingRunId = Number(body.id ?? body);
  if (!Number.isFinite(billingRunId) || billingRunId <= 0) {
    throw new Error(`[${PDT_3187_JIRA_KEY}] Billing run id missing after POST /billing-run`);
  }
  return { billingRunId, payload: billingPayload as unknown as Record<string, unknown> };
}

export async function assertRealInvoicesForBillingRun(
  Request: FixtureRequest,
  invoiceIds: number[],
): Promise<Array<{ invoiceId: number; invoiceStatus: string; customerCommunicationId: number | null }>> {
  const results: Array<{
    invoiceId: number;
    invoiceStatus: string;
    customerCommunicationId: number | null;
  }> = [];
  expect(invoiceIds.length, `[${PDT_3187_JIRA_KEY}] Expected ≥1 invoice id after accounting`).toBeGreaterThan(
    0,
  );
  for (const invoiceId of invoiceIds) {
    const invoiceRes = await Request.get(`invoice?id=${invoiceId}`);
    await expect(invoiceRes).CheckResponse();
    const invoice = (await invoiceRes.json()) as {
      invoiceStatus?: string;
      customerCommunicationId?: number | null;
    };
    expect(invoice.invoiceStatus, `Invoice ${invoiceId} must be REAL`).toBe('REAL');
    results.push({
      invoiceId,
      invoiceStatus: String(invoice.invoiceStatus),
      customerCommunicationId:
        invoice.customerCommunicationId != null ? Number(invoice.customerCommunicationId) : null,
    });
  }
  return results;
}

/** Assert CRM email list for the synthetic customer has no objects (deleted-header skip path). */
export async function assertNoEmailCommunicationsForCustomer(
  Request: FixtureRequest,
  Endpoints: baseFixture['Endpoints'],
  customerIdentifier: string,
): Promise<{ totalElements: number }> {
  const listRes = await Request.get(`${Endpoints.email}/list`, {
    params: {
      page: 0,
      size: 25,
      prompt: customerIdentifier,
      searchBy: 'CUSTOMER_IDENTIFIER',
    },
  });
  await expect(listRes).CheckResponse();
  const body = (await listRes.json()) as { totalElements?: number; content?: unknown[] };
  const totalElements = Number(body.totalElements ?? body.content?.length ?? 0);
  expect(
    totalElements,
    `[${PDT_3187_JIRA_KEY}] Expected zero email-communication objects for customer ${customerIdentifier} after REAL invoice with DELETED billing header`,
  ).toBe(0);
  return { totalElements };
}

/**
 * Optional note helper — ES DEV search for BillingRunEmailSender skip is outside Playwright.
 * Do not fail the test when ES MCP is unavailable.
 */
export function pdt3187ElasticsearchSkipNote(): string {
  return (
    `[${PDT_3187_JIRA_KEY}] Optional ES DEV check (manual/MCP): search BillingRunEmailSenderService ` +
    `for "Customer communication not found" / empty contacts around the billing run id. ` +
    `Do not fail Playwright if ES is unavailable.`
  );
}
