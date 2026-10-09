/**
 * PDT-3486 / PDT-3487 — delayed ALO after government compensation on Standard
 * Billing and Invoice Correction accounting.
 *
 * Backend TC: Cursor-Project/test_cases/Backend/PDT-3486_standard_billing_compensation_deferred_ALO.md
 *
 * Swagger (Rule 41 this session, Cursor-Project/config/swagger/dev/swagger-spec.json):
 * - PATCH /billing-run/start-accounting — OpenAPI 200, runtime often 202 (CheckResponse 2xx)
 * - POST /government-compensations CompensationRequest
 * - GET /government-compensations/{id} and /listing (CompensationListingRequest.searchBy CUSTOMER)
 * - GET /customer-receivable + GET /customer-receivable/{id}
 * - PUT /customer-receivable/{id} CustomerReceivableRequest
 *   (blockedForOffsetting, blockedFromDate, blockedToDate, reasonId)
 * - PUT /customer-liability/{id} CustomerLiabilityRequest
 *   (blockedForLiabilitiesOffsetting, blockedForLiabilitiesOffsettingFromDate/ToDate,
 *   blockedForLiabilitiesOffsettingReasonId)
 * - GET /customer-liability/{id}
 *
 * Leftover ALO counterpart items are seeded with POST /customer-receivable and
 * POST /customer-liability (creationType MANUAL) on the same contract billing
 * group — not IAP, Credit Note, or invoice cancellation. The billed document
 * under test is one FOR_VOLUMES draft (PDT-3483 debit path). Invoice Correction
 * reuses that REAL invoice (listOfInvoices; no extra BBP; not invoice cancellation).
 * AUTOMATIC receivable/liability PUT uses block flags only (runtime rejects core fields).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3470-standard-credit-note-government-compensation-receivable.spec.ts
 * - tests/cursor/pdt-3470-standard-credit-note-government-compensation-receivable.fixtures.ts
 * - tests/cursor/PDT-3483-billing-run-compensation-credit-debit-note-final-liability.spec.ts
 * - tests/cursor/pdt-3483-billing-run-compensation-credit-debit-note-final-liability.fixtures.ts
 * - tests/cursor/PDT-3127-government-compensation-defer-to-pdf-invoice-correction.spec.ts
 * - tests/cursor/pdt-3127-government-compensation-defer-to-pdf-invoice-correction.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 */

import { expect } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { type ExpParity03Fx } from './exp-parity-03-interim-deduction-and-issue-date.fixtures';
import {
  COMP_ROOT,
  getCompensation,
  getInvoice,
  isNullishEntityRef,
  resolveInvoiceCustomerLiabilityId,
  startAccountingAndWaitCompleted,
  startGeneratingAndWaitGenerated,
  type CompensationView,
  type InvoiceView,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import {
  asEntityId,
  getLiability,
  getReceivable,
  listCompensationsByNumber,
  listReceivablesForCustomer,
  money,
  nestedId,
  pdt3470PortalExtraLinks,
  uniqueCompensationNumber,
  type Pdt3470Fx,
  type Pdt3470LiabilityView,
  type Pdt3470ReceivableView,
} from './pdt-3470-standard-credit-note-government-compensation-receivable.fixtures';
import {
  assertLinkedInvoiceIsDebitNote,
  postPdt3483SignedCompensation,
  startPdt3483DebitNoteDraft,
  type Pdt3483DebitNoteDraft,
} from './pdt-3483-billing-run-compensation-credit-debit-note-final-liability.fixtures';
import {
  classifyCorrectionDraftInvoices,
  startCorrectionBillingAndWaitDraft,
} from './pdt-3127-government-compensation-defer-to-pdf-invoice-correction.fixtures';

export {
  asEntityId,
  getCompensation,
  getInvoice,
  getLiability,
  getReceivable,
  isNullishEntityRef,
  listCompensationsByNumber,
  listReceivablesForCustomer,
  money,
  nestedId,
  pdt3470PortalExtraLinks,
  resolveInvoiceCustomerLiabilityId,
  startAccountingAndWaitCompleted,
  startGeneratingAndWaitGenerated,
  startPdt3483DebitNoteDraft,
  assertLinkedInvoiceIsDebitNote,
  postPdt3483SignedCompensation,
};

export type Pdt3486Fx = Pdt3470Fx;
export type { CompensationView, InvoiceView, Pdt3470LiabilityView, Pdt3470ReceivableView };

export const PDT_3486_KEY = 'PDT-3486';
export const PDT_3486_TITLE =
  'Running Automatic Offsetting in Standard Billing Run with Compensations';
export const PDT_3486_TEST_TIMEOUT_MS = 70 * 60 * 1000;
/** Open leftover receivable/liability seeded before start-accounting (same billing group). */
export const PDT_3486_MANUAL_RECEIVABLE_AMOUNT = 200;
export const PDT_3486_MANUAL_LIABILITY_AMOUNT = 200;
export const PDT_3486_RELEVANT_ENTITY_KEYS = [
  'customer',
  'pod',
  'product',
  'productContract',
  'billingRun',
  'invoice',
  'compensation',
  'customerReceivable',
  'customerLiability',
] as const;

export function uniquePdt3486CompensationNumber(tcTag: string): string {
  return uniqueCompensationNumber(tcTag).replace(/^PDT3470-/, 'PDT3486-');
}

export function roundMoney(raw: unknown): number {
  return money(raw);
}

export function minMoney(a: number, b: number): number {
  return roundMoney(Math.min(a, b));
}

function nestedNumericId(raw: unknown): number | null {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (typeof raw === 'string' && /^\d+$/.test(raw)) {
    const n = Number(raw);
    return n > 0 ? n : null;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    return nestedNumericId((raw as { id: unknown }).id);
  }
  return null;
}

function phoenixLocalDate(raw: unknown, label: string): string {
  const s = String(raw ?? '');
  const dmy = s.match(/^(\d{2})-(\d{2})-(\d{4})/);
  if (dmy) {
    return `${dmy[1]}-${dmy[2]}-${dmy[3]}`;
  }
  const iso = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (iso) {
    return `${iso[3]}-${iso[2]}-${iso[1]}`;
  }
  throw new Error(`${label}: cannot map ${JSON.stringify(raw)} to dd-MM-yyyy`);
}

function coreManualReceivablePutFields(view: Pdt3470ReceivableView): Record<string, unknown> {
  const accountingPeriodId =
    nestedNumericId(view.accountingPeriodId) ??
    nestedNumericId(view.accountPeriodId) ??
    nestedNumericId(view.accountingPeriod) ??
    nestedNumericId(view.accountingPeriodResponse);
  const currencyId =
    nestedNumericId(view.currencyId) ??
    nestedNumericId(view.currency) ??
    nestedNumericId(view.currencyResponse);
  const customerId =
    nestedNumericId(view.customerId) ??
    nestedNumericId(view.customerResponse) ??
    nestedNumericId(view.customer);
  expect(accountingPeriodId, 'GET receivable accountingPeriodId').toBeGreaterThan(0);
  expect(currencyId, 'GET receivable currencyId').toBeGreaterThan(0);
  expect(customerId, 'GET receivable customerId').toBeGreaterThan(0);
  const fields: Record<string, unknown> = {
    accountingPeriodId,
    currencyId,
    customerId,
    dueDate: phoenixLocalDate(view.dueDate, 'receivable dueDate'),
    occurrenceDate: phoenixLocalDate(view.occurrenceDate, 'receivable occurrenceDate'),
    initialAmount: roundMoney(view.initialAmount),
  };
  const billingGroupId = nestedNumericId(view.billingGroupId) ?? nestedNumericId(view.billingGroup);
  if (billingGroupId != null) {
    fields.billingGroupId = billingGroupId;
  }
  return fields;
}

function coreManualLiabilityPutFields(view: Pdt3470LiabilityView): Record<string, unknown> {
  const accountingPeriodId =
    nestedNumericId(view.accountingPeriodId) ??
    nestedNumericId(view.accountPeriodId) ??
    nestedNumericId(view.accountingPeriod) ??
    nestedNumericId(view.accountingPeriodResponse);
  const currencyId =
    nestedNumericId(view.currencyId) ??
    nestedNumericId(view.currency) ??
    nestedNumericId(view.currencyResponse);
  const customerId =
    nestedNumericId(view.customerId) ??
    nestedNumericId(view.customerResponse) ??
    nestedNumericId(view.customer);
  expect(accountingPeriodId, 'GET liability accountingPeriodId').toBeGreaterThan(0);
  expect(currencyId, 'GET liability currencyId').toBeGreaterThan(0);
  expect(customerId, 'GET liability customerId').toBeGreaterThan(0);
  const fields: Record<string, unknown> = {
    accountingPeriodId,
    currencyId,
    customerId,
    dueDate: phoenixLocalDate(view.dueDate, 'liability dueDate'),
    occurrenceDate: phoenixLocalDate(view.occurrenceDate, 'liability occurrenceDate'),
    initialAmount: roundMoney(view.initialAmount),
  };
  const billingGroupId = nestedNumericId(view.billingGroupId) ?? nestedNumericId(view.billingGroup);
  if (billingGroupId != null) {
    fields.billingGroupId = billingGroupId;
  }
  return fields;
}

export async function resolveProductContractBillingGroupId(
  Request: Pdt3486Fx['Request'],
  contractId: number,
): Promise<number> {
  const res = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    billingGroups?: Array<{ id?: number }>;
    contractPodsResponses?: Array<{ billingGroupId?: number }>;
  };
  const fromGroups = Number(body.billingGroups?.[0]?.id);
  if (Number.isFinite(fromGroups) && fromGroups > 0) {
    return fromGroups;
  }
  const fromPods = Number(body.contractPodsResponses?.[0]?.billingGroupId);
  if (Number.isFinite(fromPods) && fromPods > 0) {
    return fromPods;
  }
  throw new Error(`No billing group on product-contract ${contractId}`);
}

/**
 * POST /customer-receivable CustomerReceivableRequest (dev swagger required:
 * accountingPeriodId, currencyId, customerId, dueDate, initialAmount, occurrenceDate).
 * billingGroupId is the contract billing group so leftover ALO can match the invoice.
 */
export async function postManualReceivableOnBillingGroup(
  fx: Pdt3486Fx,
  opts: { customerId: number; billingGroupId: number; initialAmount: number },
): Promise<Pdt3470ReceivableView> {
  const payload = fx.GeneratePayload.receivablesManagement.customer_receivable();
  payload.customerId = opts.customerId;
  payload.billingGroupId = opts.billingGroupId;
  payload.initialAmount = opts.initialAmount;
  payload.blockedForOffsetting = false;
  const res = await fx.Request.post(fx.Endpoints.customerReceivable, { data: payload });
  await expect(res).CheckResponse();
  const id = asEntityId(await res.json());
  fx.Responses.customerReceivable.push(id);
  const rec = await getReceivable(fx.Request, id);
  expect(rec.creationType, 'seeded receivable creationType').toBe('MANUAL');
  expect(roundMoney(rec.currentAmount), 'seeded receivable currentAmount').toBe(
    roundMoney(opts.initialAmount),
  );
  expect(Boolean(rec.blockedForLiabilitiesOffsetting), 'seeded receivable not blocked').toBe(false);
  return rec;
}

/**
 * POST /customer-liability CustomerLiabilityRequest (dev swagger required:
 * accountingPeriodId, currencyId, customerId, dueDate, initialAmount, occurrenceDate).
 */
export async function postManualLiabilityOnBillingGroup(
  fx: Pdt3486Fx,
  opts: { customerId: number; billingGroupId: number; initialAmount: number },
): Promise<Pdt3470LiabilityView> {
  const payload = fx.GeneratePayload.receivablesManagement.customer_liability();
  payload.customerId = opts.customerId;
  payload.billingGroupId = opts.billingGroupId;
  payload.initialAmount = opts.initialAmount;
  const res = await fx.Request.post(fx.Endpoints.customerLiability, { data: payload });
  await expect(res).CheckResponse();
  const id = asEntityId(await res.json());
  fx.Responses.customerLiability.push(id);
  const liab = await getLiability(fx.Request, id);
  expect(liab.creationType, 'seeded liability creationType').toBe('MANUAL');
  expect(roundMoney(liab.currentAmount), 'seeded liability currentAmount').toBe(
    roundMoney(opts.initialAmount),
  );
  expect(Boolean(liab.blockedForLiabilitiesOffsetting), 'seeded liability not blocked').toBe(false);
  return liab;
}

export async function postPdt3486Compensation(
  fx: Pdt3486Fx,
  opts: {
    customerId: number;
    podId: number;
    recipientId: number;
    documentPeriod: string;
    documentAmount: number;
    number: string;
    reason: string;
  },
): Promise<{ id: number; number: string }> {
  return postPdt3483SignedCompensation(fx, opts);
}

/** PDT-3483 stops at PDF. PDT-3486 realizes the run (`PATCH start-accounting` → REAL). */
export async function realizeBillingRun(
  Request: Pdt3486Fx['Request'],
  billingRunId: number,
  invoiceId: number,
): Promise<InvoiceView> {
  await startAccountingAndWaitCompleted(Request, billingRunId);
  const invoice = await getInvoice(Request, invoiceId);
  expect(invoice.invoiceStatus, `invoice ${invoiceId} after start-accounting`).toBe('REAL');
  return invoice;
}

export async function putReceivableOffsettingBlock(
  fx: Pdt3486Fx,
  receivableId: number,
  blocked: boolean,
): Promise<Pdt3470ReceivableView> {
  const current = await getReceivable(fx.Request, receivableId);
  // MANUAL (never offset, OPEN AP) uses CustomerReceivablePostGroup — core fields
  // required. AUTOMATIC / already-offset / CLOSED AP uses validateForNulls —
  // those same fields must be omitted.
  const payload: Record<string, unknown> =
    current.creationType === 'MANUAL' ? coreManualReceivablePutFields(current) : {};
  payload.blockedForOffsetting = blocked;
  if (blocked) {
    payload.blockedFromDate = '01-01-1990';
    payload.blockedToDate = '31-12-2090';
    payload.reasonId = envVariables.blocking_reason;
  } else {
    payload.blockedFromDate = null;
    payload.blockedToDate = null;
    payload.reasonId = null;
  }

  const res = await fx.Request.put(`customer-receivable/${receivableId}`, { data: payload });
  await expect(res).CheckResponse();
  const after = await getReceivable(fx.Request, receivableId);
  expect(
    Boolean(after.blockedForLiabilitiesOffsetting),
    `GET blockedForLiabilitiesOffsetting after PUT blockedForOffsetting=${blocked}`,
  ).toBe(blocked);
  return after;
}

/**
 * PUT /customer-liability/{id} CustomerLiabilityRequest (dev swagger required:
 * accountingPeriodId, currencyId, customerId, dueDate, initialAmount, occurrenceDate).
 * MANUAL (not yet offset) needs those fields; AUTOMATIC must omit them.
 */
export async function putLiabilityOffsettingBlock(
  fx: Pdt3486Fx,
  liabilityId: number,
  blocked: boolean,
): Promise<Pdt3470LiabilityView> {
  const current = await getLiability(fx.Request, liabilityId);
  const payload: Record<string, unknown> =
    current.creationType === 'MANUAL' ? coreManualLiabilityPutFields(current) : {};
  payload.blockedForLiabilitiesOffsetting = blocked;
  if (blocked) {
    payload.blockedForLiabilitiesOffsettingFromDate = '01-01-1990';
    payload.blockedForLiabilitiesOffsettingToDate = '31-12-2090';
    payload.blockedForLiabilitiesOffsettingReasonId = envVariables.blocking_reason;
  } else {
    payload.blockedForLiabilitiesOffsettingFromDate = null;
    payload.blockedForLiabilitiesOffsettingToDate = null;
    payload.blockedForLiabilitiesOffsettingReasonId = null;
  }

  const res = await fx.Request.put(`customer-liability/${liabilityId}`, { data: payload });
  await expect(res).CheckResponse();
  const after = await getLiability(fx.Request, liabilityId);
  expect(
    Boolean(after.blockedForLiabilitiesOffsetting),
    `blockedForLiabilitiesOffsetting after PUT blocked=${blocked}`,
  ).toBe(blocked);
  return after;
}

export async function assertCompensationLinkedUninvoiced(
  Request: Pdt3486Fx['Request'],
  compensationId: number,
  invoiceId: number,
  label: string,
): Promise<CompensationView> {
  const view = await getCompensation(Request, compensationId);
  expect(view.compensationStatus, `${label}: still UNINVOICED`).toBe('UNINVOICED');
  expect(nestedId(view.invoice), `${label}: linked invoice.id`).toBe(invoiceId);
  expect(view.index, `${label}: index=0`).toBe(0);
  return view;
}

export async function listCompensationsForCustomer(
  Request: Pdt3486Fx['Request'],
  customerIdentifier: string,
): Promise<Array<{ id: number; invoice?: { id?: number } | null; compensationStatus?: string }>> {
  const res = await Request.get(`${COMP_ROOT}/listing`, {
    params: {
      page: 0,
      size: 50,
      prompt: customerIdentifier,
      searchBy: 'CUSTOMER',
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    content?: Array<{ id?: number; invoice?: { id?: number } | null; compensationStatus?: string }>;
  };
  return (body.content ?? []).filter((row) => Number(row.id) > 0) as Array<{
    id: number;
    invoice?: { id?: number } | null;
    compensationStatus?: string;
  }>;
}

export async function findAutomaticLiabilityOnInvoice(
  Request: Pdt3486Fx['Request'],
  invoiceId: number,
  expectedInitial: number,
  excludeIds: number[] = [],
): Promise<Pdt3470LiabilityView> {
  const invoice = await getInvoice(Request, invoiceId);
  const rows = invoice.liabilitiesAndReceivables ?? [];
  const exclude = new Set(excludeIds.filter((id) => Number.isFinite(id) && id > 0));
  const details: Pdt3470LiabilityView[] = [];
  for (const row of rows) {
    if (String(row.type ?? '').toUpperCase() !== 'LIABILITY' || Number(row.id) <= 0) {
      continue;
    }
    if (exclude.has(Number(row.id))) {
      continue;
    }
    const detail = await getLiability(Request, Number(row.id));
    if (detail.creationType && detail.creationType !== 'AUTOMATIC') {
      continue;
    }
    const linkedInvoice = nestedId(detail.invoiceResponse);
    if (linkedInvoice != null && linkedInvoice !== invoiceId) {
      continue;
    }
    details.push(detail);
  }

  if (expectedInitial >= 0) {
    const want = roundMoney(expectedInitial);
    const exact = details.find((row) => roundMoney(row.initialAmount) === want);
    if (exact) {
      return exact;
    }
  } else if (details.length > 0) {
    return details[0];
  }

  // Correction invoices: document T can differ from AUTOMATIC liability initial
  // after DLO/ALO. Use the billed-customer AUTOMATIC row with the largest initial.
  if (details.length > 0) {
    return [...details].sort(
      (a, b) => roundMoney(b.initialAmount) - roundMoney(a.initialAmount),
    )[0];
  }

  const fallbackId = await resolveInvoiceCustomerLiabilityId(Request, invoiceId);
  if (exclude.has(fallbackId)) {
    throw new Error(
      `No billed-customer AUTOMATIC liability on invoice ${invoiceId} (exclude=${[...exclude].join(',')})`,
    );
  }
  return getLiability(Request, fallbackId);
}

export async function findAutomaticReceivableOnInvoice(
  Request: Pdt3486Fx['Request'],
  opts: {
    customerIdentifier: string;
    invoiceId: number;
    expectedInitial: number;
    excludeId?: number;
  },
): Promise<Pdt3470ReceivableView> {
  const listed = await listReceivablesForCustomer(Request, opts.customerIdentifier);
  const abs = roundMoney(Math.abs(opts.expectedInitial));
  const details: Pdt3470ReceivableView[] = [];
  for (const row of listed) {
    if (opts.excludeId != null && row.id === opts.excludeId) {
      continue;
    }
    const detail = await getReceivable(Request, row.id);
    if (nestedId(detail.invoiceResponse) !== opts.invoiceId) {
      continue;
    }
    if (detail.creationType !== 'AUTOMATIC') {
      continue;
    }
    details.push(detail);
  }
  const exact = details.find((row) => roundMoney(row.initialAmount) === abs);
  if (exact) {
    return exact;
  }
  if (details.length > 0) {
    return [...details].sort(
      (a, b) => roundMoney(b.initialAmount) - roundMoney(a.initialAmount),
    )[0];
  }
  throw new Error(
    `No AUTOMATIC receivable on invoice ${opts.invoiceId} (exclude=${opts.excludeId ?? 'none'}; expectedInitial=${abs})`,
  );
}

export function assertPositiveCompensationPolarity(view: CompensationView, label: string): void {
  expect(nestedId(view.receivableForCustomer), `${label}: receivableForCustomer`).toBeTruthy();
  expect(nestedId(view.liabilityForRecipient), `${label}: liabilityForRecipient`).toBeTruthy();
  expect(
    isNullishEntityRef(view.liabilityForCustomer as { id?: number } | null),
    `${label}: liabilityForCustomer is null`,
  ).toBe(true);
  expect(
    isNullishEntityRef(view.receivableForRecipient as { id?: number } | null),
    `${label}: receivableForRecipient is null`,
  ).toBe(true);
}

export function assertNegativeCompensationPolarity(view: CompensationView, label: string): void {
  expect(nestedId(view.liabilityForCustomer), `${label}: liabilityForCustomer`).toBeTruthy();
  expect(nestedId(view.receivableForRecipient), `${label}: receivableForRecipient`).toBeTruthy();
  expect(
    isNullishEntityRef(view.receivableForCustomer as { id?: number } | null),
    `${label}: receivableForCustomer is null`,
  ).toBe(true);
  expect(
    isNullishEntityRef(view.liabilityForRecipient as { id?: number } | null),
    `${label}: liabilityForRecipient is null`,
  ).toBe(true);
}

export async function generateAndAccount(
  fx: Pdt3486Fx,
  billingRunId: number,
  invoiceId: number,
  expectedStatus = 'REAL',
): Promise<InvoiceView> {
  await startGeneratingAndWaitGenerated(fx.Request, billingRunId);
  await startAccountingAndWaitCompleted(fx.Request, billingRunId);
  const invoice = await getInvoice(fx.Request, invoiceId);
  expect(invoice.invoiceStatus, `invoice ${invoiceId} after accounting`).toBe(expectedStatus);
  return invoice;
}

export async function startCorrectionDraftForInvoice(
  fx: Pdt3486Fx,
  originalInvoiceId: number,
): Promise<{ correctionBillingRunId: number; draftInvoiceIds: number[] }> {
  const invoiceIndex = fx.Responses.invoice.indexOf(originalInvoiceId as never);
  expect(invoiceIndex, 'original invoice index in Responses.invoice').toBeGreaterThanOrEqual(0);
  // Invoice Correction of the existing REAL invoice (listOfInvoices). Do not
  // POST extra BBP and do not POST /invoice-cancellation — those paths are out.
  const correctionPayload = await fx.GeneratePayload.billing.correctionBilling(invoiceIndex);
  const correction = await fx.Request.post(fx.Endpoints.billingRun, { data: correctionPayload });
  await expect(correction).CheckResponse();
  const correctionBillingRunId = Number(await correction.json());
  expect(correctionBillingRunId, 'INVOICE_CORRECTION billing run id').toBeGreaterThan(0);
  fx.Responses.billingRun.push(correctionBillingRunId);
  const draft = await startCorrectionBillingAndWaitDraft(
    fx as ExpParity03Fx & Pdt3486Fx,
    correctionBillingRunId,
  );
  return { correctionBillingRunId, draftInvoiceIds: draft.draftInvoiceIds };
}

export async function classifyCorrectionDrafts(
  Request: Pdt3486Fx['Request'],
  draftInvoiceIds: number[],
): Promise<{
  forwardInvoiceId: number | null;
  correctionCnId: number | null;
  allDocs: Array<{ id: number; docType: string; invoiceType?: string; total: number }>;
}> {
  const classified = await classifyCorrectionDraftInvoices(Request, draftInvoiceIds);
  const allDocs: Array<{ id: number; docType: string; invoiceType?: string; total: number }> = [];
  let correctionCnId: number | null = null;
  const forwardIds: number[] = [];
  for (const id of draftInvoiceIds) {
    const inv = await getInvoice(Request, id);
    const docType = String(inv.invoiceDocumentType ?? '');
    const invoiceType = String(inv.invoiceType ?? '');
    allDocs.push({
      id,
      docType,
      invoiceType,
      total: roundMoney(Math.abs(Number(inv.totalAmountIncludingVat ?? 0))),
    });
    if (invoiceType.includes('CANCELLATION') || invoiceType === 'REVERSAL') {
      continue;
    }
    if (invoiceType === 'CORRECTION' && docType === 'CREDIT_NOTE') {
      correctionCnId = correctionCnId == null ? id : Math.min(correctionCnId, id);
    }
    if (
      invoiceType === 'CORRECTION' &&
      (docType === 'INVOICE' || docType === 'DEBIT_NOTE')
    ) {
      forwardIds.push(id);
    }
  }
  return {
    forwardInvoiceId:
      forwardIds.length > 0 ? Math.min(...forwardIds) : classified.forwardInvoiceId ?? null,
    correctionCnId,
    allDocs,
  };
}

export type { Pdt3483DebitNoteDraft };
