/**
 * PDT-2913 — Missing email object for customers from reminder for disconnection (bug-only helpers).
 *
 * Non-INVOICE verification path (Dev2) — CASE branch (1) in
 * ProductContractDetailsRepository.findCustomerCommunicationIdsForBillingByLiabilityIds:
 *   creation_type = 'MANUAL' AND contract_billing_group_id IS NOT NULL
 * → billing-group communication (works without invoice / INVOICE|DEBIT_NOTE branch).
 *
 * Flow:
 * 1. Customer + term + POD + product + contract + POD activation
 * 2. Manual customer liability (POST /customer-liability) with billingGroupId from contract
 *    — creationType MANUAL, outgoingDocumentType typically null (not INVOICE/DEBIT_NOTE), no invoice
 * 3. Create Power Supply Disconnection Reminder (EMAIL) — Dev2 listOfCustomer shape
 * 4. Overdue known liabilityId + set-customer-send-time + POST job → EXECUTED
 *    (does NOT use Responses.invoice / offsetReminderForDisconnectionTime invoice path)
 * 5. Assert email-communication objects exist for the customer (topic RFD)
 *
 * Reference:
 * - tests/receivableManagement/reminderForDisconnection/generateCommunicationObjects.spec.ts (REG-1011)
 * - tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures.ts (createManualLiability)
 * - ReceivablesManagementPayloads.offsetReminderForDisconnectionTime (job poll logic)
 *
 * Swagger (dev2): POST/GET /customer-liability, PUT .../due-date-change,
 * POST/GET /power-supply-disconnection-reminder, PUT .../set-customer-send-time,
 * POST .../job, GET /email-communication/list
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { Endpoints } from '../../fixtures/constants/endpoints';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';

/** Communication topic id used by REG-1011 for RFD email listing. */
export const RFD_EMAIL_COMMUNICATION_TOPIC_ID = 50;

export const PDT_2913_KEY = 'PDT-2913';
export const PDT_2913_TITLE =
  'Missing email object for customers from reminder for disconnection';

export type Pdt2913Fx = {
  Request: baseFixture['Request'];
  GeneratePayload: baseFixture['GeneratePayload'];
  Responses: baseFixture['Responses'];
  Endpoints: typeof Endpoints;
};

export type Pdt2913LiabilitySnapshot = {
  liabilityId: number;
  invoiceId: number | null;
  outgoingDocumentType: string | null;
  creationType: string | null;
  contractBillingGroupId: number | null;
};

export type Pdt2913EmailListItem = {
  massOrIndemailCommunicationId?: number;
  uicOrPersonalNumber?: string;
  communicationTopic?: string;
  status?: string;
};

function reminderId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) {
    return raw;
  }
  if (raw != null && typeof raw === 'object' && 'id' in (raw as object)) {
    const id = Number((raw as { id: number }).id);
    if (Number.isFinite(id)) {
      return id;
    }
  }
  throw new Error(`Cannot resolve reminder for disconnection id from: ${JSON.stringify(raw)}`);
}

/** Customer → term → POD → product → contract → POD activation (REG-1011 order). */
export async function setupPdt2913ContractChain(fx: Pdt2913Fx): Promise<void> {
  const customer = await fx.Request.post(fx.Endpoints.customer, {
    data: fx.GeneratePayload.customers.customer_legal(),
  });
  await expect(customer).CheckResponse();
  fx.Responses.customer.push(await customer.json());

  const term = await fx.Request.post(fx.Endpoints.terms, {
    data: fx.GeneratePayload.productAndServices.term(),
  });
  await expect(term).CheckResponse();
  fx.Responses.terms.push(await term.json());

  const pod = await fx.Request.post(fx.Endpoints.pod, {
    data: fx.GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod).CheckResponse();
  fx.Responses.pod.push(await pod.json());

  const product = await fx.Request.post(fx.Endpoints.product, {
    data: fx.GeneratePayload.productAndServices.product(),
  });
  await expect(product).CheckResponse();
  fx.Responses.product.push(await product.json());

  const contract = await fx.Request.post(fx.Endpoints.productContract, {
    data: await fx.GeneratePayload.contractsAndOrders.product_contract(),
  });
  await expect(contract).CheckResponse();
  fx.Responses.productContract.push(await contract.json());

  const podActivation = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(),
  });
  await expect(podActivation).CheckResponse();
}

/**
 * Resolve contract billing group id from product contract POD rows
 * (same pattern as ReceivablesManagementPayloads.payment / PHN-362).
 */
export async function resolvePdt2913BillingGroupId(fx: Pdt2913Fx): Promise<number> {
  const contractId = Number(fx.Responses.productContract[0].id);
  expect(contractId, 'productContract id required for billing group resolve').toBeGreaterThan(0);

  const contractGet = await fx.Request.get(`${fx.Endpoints.productContract}/${contractId}`);
  await expect(contractGet).CheckResponse();
  const body = await contractGet.json();
  const pods = (body?.contractPodsResponses ?? []) as Array<{ billingGroupId?: number }>;
  const billingGroupId = Number(pods[0]?.billingGroupId);
  expect(
    billingGroupId,
    `Contract ${contractId} must expose contractPodsResponses[0].billingGroupId for MANUAL liability CASE (1)`,
  ).toBeGreaterThan(0);
  return billingGroupId;
}

/**
 * Manual customer liability (not billing-run invoice).
 * Sets billingGroupId so SQL CASE (1) resolves communication without invoice branch.
 * Due date = yesterday so liability is overdue for RFD selection.
 */
export async function createPdt2913ManualLiability(fx: Pdt2913Fx): Promise<number> {
  const billingGroupId = await resolvePdt2913BillingGroupId(fx);
  const payload = fx.GeneratePayload.receivablesManagement.customer_liability() as Record<
    string,
    unknown
  >;
  payload.customerId = fx.Responses.customer[0].id;
  payload.billingGroupId = billingGroupId;
  payload.initialAmount = 100;
  payload.occurrenceDate = randomGens.generateYesterdaysDate('dd-mm-yyyy');
  payload.dueDate = randomGens.generateYesterdaysDate('dd-mm-yyyy');

  const res = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  await expect(res).CheckResponse();
  const liabilityId = Number(await res.json());
  expect(liabilityId, 'POST /customer-liability must return liability id').toBeGreaterThan(0);
  fx.Responses.customerLiability.push(liabilityId);
  return liabilityId;
}

/**
 * Read invoice id from CustomerLiabilityResponse.
 * Phoenix exposes nested InvoiceShortResponse as `invoiceResponse` (not flat invoiceId).
 */
function readLiabilityInvoiceId(body: Record<string, unknown>): number | null {
  const invoiceResponse = body.invoiceResponse as { id?: number } | null | undefined;
  if (invoiceResponse?.id != null) {
    const id = Number(invoiceResponse.id);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  if (body.invoiceId != null && body.invoiceId !== '') {
    const id = Number(body.invoiceId);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  const invoice = body.invoice as { id?: number } | null | undefined;
  if (invoice?.id != null) {
    const id = Number(invoice.id);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  return null;
}

function readBillingGroupId(body: Record<string, unknown>): number | null {
  const billingGroupResponse = body.billingGroupResponse as { id?: number } | null | undefined;
  if (billingGroupResponse?.id != null) {
    const id = Number(billingGroupResponse.id);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  if (body.contractBillingGroupId != null && body.contractBillingGroupId !== '') {
    const id = Number(body.contractBillingGroupId);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  if (body.billingGroupId != null && body.billingGroupId !== '') {
    const id = Number(body.billingGroupId);
    if (Number.isFinite(id) && id > 0) {
      return id;
    }
  }
  return null;
}

function toLiabilitySnapshot(
  liabilityId: number,
  body: Record<string, unknown>,
): Pdt2913LiabilitySnapshot {
  return {
    liabilityId,
    invoiceId: readLiabilityInvoiceId(body),
    outgoingDocumentType:
      body.outgoingDocumentType != null ? String(body.outgoingDocumentType) : null,
    creationType: body.creationType != null ? String(body.creationType) : null,
    contractBillingGroupId: readBillingGroupId(body),
  };
}

/**
 * Assert manual liability is on the non-INVOICE SQL path:
 * creationType MANUAL, no invoice, outgoingDocumentType not INVOICE/DEBIT_NOTE,
 * billing group present (CASE 1).
 */
export async function assertPdt2913ManualNonInvoiceLiability(
  fx: Pdt2913Fx,
  liabilityId: number,
): Promise<Pdt2913LiabilitySnapshot> {
  const liabilityGet = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
  await expect(liabilityGet).CheckResponse();
  const body = (await liabilityGet.json()) as Record<string, unknown>;
  const snapshot = toLiabilitySnapshot(liabilityId, body);

  expect(snapshot.creationType, `Liability ${liabilityId} creationType`).toEqual('MANUAL');
  expect(
    snapshot.invoiceId,
    `Liability ${liabilityId} must have null invoiceResponse (non-invoice path)`,
  ).toBeNull();
  expect(
    snapshot.outgoingDocumentType,
    `Liability ${liabilityId} outgoingDocumentType must not be INVOICE`,
  ).not.toEqual('INVOICE');
  expect(
    snapshot.outgoingDocumentType,
    `Liability ${liabilityId} outgoingDocumentType must not be DEBIT_NOTE`,
  ).not.toEqual('DEBIT_NOTE');
  expect(
    snapshot.contractBillingGroupId,
    `Liability ${liabilityId} must have billing group for CASE (1) communication resolve`,
  ).toBeGreaterThan(0);

  fx.Responses.customerLiability.push(body);
  return snapshot;
}

/**
 * Create RFD with EMAIL channel (Swagger: PowerSupplyDisconnectionReminderBaseRequest).
 *
 * Cursor GeneratePayload still emits legacy `customerList` / `customerFilterType`.
 * Dev2 (and staging) require `listOfCustomer` + `conditionType: LIST_OF_CUSTOMERS`
 * (+ `name`, `confirm`) — reshape in this helper; do not edit jsons/payloads.
 */
export async function createPdt2913ReminderForDisconnection(fx: Pdt2913Fx): Promise<{
  reminderId: number;
  payload: Record<string, unknown>;
}> {
  const base = fx.GeneratePayload.receivablesManagement.reminderForDisconnection() as Record<
    string,
    unknown
  >;

  const listOfCustomer = fx.Responses.customer
    .map((c) => String(c.identifier))
    .filter((id) => id.length > 0)
    .join(',');
  expect(listOfCustomer, 'RFD listOfCustomer requires at least one customer identifier').toBeTruthy();

  // Explicit Dev2/staging shape — omit legacy customerList / customerFilterType.
  const payload: Record<string, unknown> = {
    customerSendToDateAndTime: base.customerSendToDateAndTime,
    liabilityAmountFrom: base.liabilityAmountFrom ?? null,
    liabilityAmountTo: base.liabilityAmountTo ?? null,
    liabilitiesMaxDueDate: base.liabilitiesMaxDueDate,
    listOfCustomer,
    conditionType: 'LIST_OF_CUSTOMERS',
    communicationChannels: ['EMAIL'],
    emailTemplateId: envVariables.reminder_disconnection_email_template,
    smsTemplateId: null,
    documentTemplateId: null,
    disconnectionDate: base.disconnectionDate,
    printedFileName: base.printedFileName ?? '1',
    name:
      typeof base.name === 'string' && base.name.length > 0
        ? base.name
        : `PDT2913-${randomGens.generateRandomString(true, true, 10)}`,
    confirm: true,
  };

  const response = await fx.Request.post(fx.Endpoints.reminderForDisconnection, { data: payload });
  await expect(response).CheckResponse();
  const body = await response.json();
  fx.Responses.reminderForDisconnection.push(body);

  return { reminderId: reminderId(body), payload };
}

/**
 * Make known liability overdue, set RFD customer send time to now, run job until EXECUTED.
 * Does NOT call offsetReminderForDisconnectionTime (that path requires Responses.invoice[0]).
 */
export async function executePdt2913ReminderCommunicationForLiability(
  fx: Pdt2913Fx,
  liabilityId: number,
): Promise<void> {
  expect(liabilityId, 'liabilityId required for RFD overdue + job').toBeGreaterThan(0);

  const reminderRaw = fx.Responses.reminderForDisconnection[0];
  const rfdId = reminderId(reminderRaw);

  const dueDateChange = await fx.Request.put(
    `customer-liability/${liabilityId}/due-date-change?dueDate=${randomGens.generateYesterdaysDate('yyyy-mm-dd')}`,
  );
  await expect(dueDateChange).CheckResponse();

  const now = new Date();
  const georgianTime = new Date(now.toLocaleString('en-US', { timeZone: 'Asia/Tbilisi' }));
  const hours = georgianTime.getHours();
  const minutes = georgianTime.getMinutes();

  const timeOffset = await fx.Request.put(
    `power-supply-disconnection-reminder/set-customer-send-time?reminderId=${rfdId}&hour=${hours}&minute=${minutes}`,
  );
  await expect(timeOffset).CheckResponse();

  const jobPost = await fx.Request.post('power-supply-disconnection-reminder/job');
  await expect(jobPost).CheckResponse();

  const maxDuration = 2 * 60 * 1000;
  const interval = 5 * 1000;
  const maxAttempts = Math.floor(maxDuration / interval);

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const reminderGet = await fx.Request.get(`power-supply-disconnection-reminder/${rfdId}`);
      await expect(reminderGet).CheckResponse();
      const reminderJson = await reminderGet.json();

      if (reminderJson.reminderStatus !== 'EXECUTED') {
        if (attempt % 5 === 0 || attempt === 1) {
          console.log(
            `⏳ Attempt ${attempt}/${maxAttempts}: Waiting for reminder for disconnection to become EXECUTED...`,
          );
        }
        if (attempt < maxAttempts) {
          await new Promise((resolve) => setTimeout(resolve, interval));
        }
        continue;
      }

      console.log(`✅ Reminder for disconnection status is EXECUTED at attempt ${attempt}`);
      return;
    } catch (err) {
      console.error(`❌ Error checking reminder status at attempt ${attempt}:`, err);
      if (attempt === maxAttempts) {
        throw new Error(`Failed to verify reminder status after ${maxAttempts} attempts: ${err}`);
      }
      await new Promise((resolve) => setTimeout(resolve, interval));
    }
  }

  throw new Error('❌ Reminder for disconnection did not reach EXECUTED status within 2 minutes');
}

/** Poll GET /email-communication/list until ≥1 email for the customer (RFD topic). */
export async function assertPdt2913EmailObjectsCreated(
  fx: Pdt2913Fx,
  customerIdentifier: string,
): Promise<Pdt2913EmailListItem[]> {
  let emails: Pdt2913EmailListItem[] = [];

  await expect
    .poll(
      async () => {
        const emailListing = await fx.Request.get(
          `${fx.Endpoints.email}/list?page=0&size=25&prompt=${customerIdentifier}&searchBy=CUSTOMER_IDENTIFIER&communicationTopicId=${RFD_EMAIL_COMMUNICATION_TOPIC_ID}`,
        );
        await expect(emailListing).CheckResponse();
        const responseBody = await emailListing.json();
        emails = (responseBody.content ?? []) as Pdt2913EmailListItem[];
        return emails.length;
      },
      {
        message: `Expected ≥1 RFD email-communication for customer ${customerIdentifier} (PDT-2913)`,
        timeout: 90 * 1000,
        intervals: [2000, 5000],
      },
    )
    .toBeGreaterThan(0);

  for (const email of emails) {
    fx.Responses.email.push(email);
  }

  return emails;
}

export function buildPdt2913AttachmentSummary(input: {
  customerIdentifier: string;
  liability: Pdt2913LiabilitySnapshot;
  reminderId: number;
  emailCount: number;
  emails: Pdt2913EmailListItem[];
}): Record<string, unknown> {
  return {
    jiraKey: PDT_2913_KEY,
    path: 'MANUAL_liability_with_billing_group_non_invoice',
    customerIdentifier: input.customerIdentifier,
    liability: input.liability,
    reminderId: input.reminderId,
    emailCount: input.emailCount,
    emailIds: input.emails.map((e) => e.massOrIndemailCommunicationId).filter(Boolean),
  };
}
