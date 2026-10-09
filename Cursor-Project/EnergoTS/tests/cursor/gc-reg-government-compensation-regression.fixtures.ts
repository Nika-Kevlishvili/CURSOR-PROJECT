/**
 * GC-REG — Government Compensation process regression (create → apply → regeneracia).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3087-government-compensation-defer-to-pdf.spec.ts / .fixtures.ts
 * - tests/cursor/dev-volume-billing-two-compensations.spec.ts / .fixtures.ts
 * - tests/cursor/PDT-3127-government-compensation-defer-to-pdf-invoice-correction.spec.ts
 *
 * Swagger: update-swagger-specs.ps1 failed this session (hosts unreachable); field names taken from cached config/swagger/dev/swagger-spec.json.
 * Backend TC: Cursor-Project/test_cases/Backend/Government_Compensation_Regression.md
 *
 * Environment: Dev. No primary Jira board key on the TC file (optional PDT-1857 / PDT-1734 / PDT-3087).
 */

import { test, expect } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import { postPdt2915BillingByProfile } from './pdt-2915-invoice-correction-deleted-bbp.fixtures';
import {
  COMP_ROOT,
  createForVolumesDraftBillingRun,
  createGovernmentCompensation,
  buildCompensationPayload,
  getCompensation,
  getInvoice,
  getCustomerLiability,
  startGeneratingAndWaitGenerated,
  startAccountingAndWaitCompleted,
  pollPdfDocumentsPresent,
  getBillingRunStatus,
  BILLING_RUN_ROOT,
  findCompensationByNumber,
  resolveEntityIdentifier,
  resolveCurrencyName,
  buildGovernmentCompensationMassImportBuffer,
  uploadGovernmentCompensationMassImport,
  pollGovernmentCompensationMassImportComplete,
  assertCompensationUnlinked,
  isNullishEntityRef,
  toAmountNumber,
  resolveInvoiceCustomerLiabilityId,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  DEV_PORTAL_BASE,
  type Pdt3087Fx,
  type CompensationView,
  type InvoiceView,
  type CustomerLiabilityView,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import {
  runDevVolCompContractAndBbpPrechain,
  monthStartFromPeriod,
  type DevVolCompPrechainResult,
  type CompensationPayload,
} from './dev-volume-billing-two-compensations.fixtures';

export {
  COMP_ROOT,
  createForVolumesDraftBillingRun,
  createGovernmentCompensation,
  buildCompensationPayload,
  getCompensation,
  getInvoice,
  getCustomerLiability,
  startGeneratingAndWaitGenerated,
  startAccountingAndWaitCompleted,
  findCompensationByNumber,
  resolveEntityIdentifier,
  resolveCurrencyName,
  buildGovernmentCompensationMassImportBuffer,
  uploadGovernmentCompensationMassImport,
  pollGovernmentCompensationMassImportComplete,
  assertCompensationUnlinked,
  isNullishEntityRef,
  toAmountNumber,
  resolveInvoiceCustomerLiabilityId,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  DEV_PORTAL_BASE,
  runDevVolCompContractAndBbpPrechain,
  monthStartFromPeriod,
};

export const GC_REG_KEY = 'GC-REG';
/** Parallel billing on Dev queued past PDT-3087's 7-minute generate/accounting poll. */
const GC_REG_GEN_MAX_MS = 15 * 60 * 1000;
const GC_REG_ACC_MAX_MS = 15 * 60 * 1000;
export const ZERO_AMOUNT_ERROR = 'documentAmount-[documentAmount] must not be zero;';
export const EDIT_LOCKED_ERROR = 'or cannot be updated';
export const REGEN_NOT_REAL_ERROR = "You can't regenerate compensations for invoice, it must be with status 'REAL'";
export const REGEN_NULL_INDEX_ERROR = 'Compensations is not accounted for this invoice';
export const DD_IBAN = 'BG80BNBG96611020345678';
/** Distinct valid Bulgarian IBANs (mod-97) so TC-BE-17 can tell institution vs invoice-customer Direct Debit. */
export const DD_IBAN_INVOICE_CUSTOMER = 'BG80BNBG96611020345678';
export const DD_IBAN_INSTITUTION = 'BG25BNBG96611020345601';
export const DD_IBAN_INVOICE_ONLY = 'BG95BNBG96611020345602';
/** Confluence 255295492 zero-amount messages (TC-BE-20 spec; not the runtime fragment). */
export const ZERO_AMOUNT_WIKI_EN = 'Allowed numbers: Any decimal number except zero.';
export const ZERO_AMOUNT_WIKI_BG = 'Допустими стойности: Всяко десетично число, с изключение на нула.';
export const NON_NUMERIC_DOCUMENT_NUMBER = 'ABC-NOT-NUMERIC';

export type GcRegFx = Pdt3087Fx;
export type { CompensationView, InvoiceView, CustomerLiabilityView, CompensationPayload, DevVolCompPrechainResult };

export type GcCreatePrechain = {
  customerId: number;
  recipientId: number;
  podId: number;
  documentPeriod: string;
  customerIdentifier: string;
  recipientIdentifier: string;
};

export type GcAccountedBilling = {
  billingRunId: number;
  invoiceId: number;
  draftInvoiceIds: number[];
};

function pickCreatedIdentifier(body: unknown, payload?: Record<string, unknown>): string {
  const b =
    body && typeof body === 'object'
      ? (body as {
          identifier?: unknown;
          customerIdentifier?: unknown;
          basicParameters?: { identifier?: unknown };
        })
      : {};
  const fromBody = String(
    b.identifier ?? b.customerIdentifier ?? b.basicParameters?.identifier ?? '',
  ).trim();
  if (fromBody) return fromBody;
  return String(payload?.identifier ?? payload?.customerIdentifier ?? '').trim();
}

function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  const n = Number(raw);
  if (Number.isFinite(n) && n > 0) {
    return n;
  }
  throw new Error(`Expected numeric entity id, got: ${JSON.stringify(raw)}`);
}

/** First day of previous calendar month (TC test data). */
export function previousMonthStartIso(): string {
  const d = new Date();
  d.setUTCDate(1);
  d.setUTCMonth(d.getUTCMonth() - 1);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, '0');
  return `${y}-${m}-01`;
}

export function todayIso(): string {
  return randomGens.generateTodaysDate('yyyy-mm-dd');
}

export async function readErrorText(res: { text: () => Promise<string> }): Promise<string> {
  return res.text();
}

export async function runGcCreatePrechain(ctx: GcRegFx): Promise<GcCreatePrechain> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  let customerPayload: Record<string, unknown> = {};
  let recipientPayload: Record<string, unknown> = {};

  await test.step('Precondition: GC customer (LEGAL)', async () => {
    customerPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
    const customer = await Request.post(Endpoints.customer, { data: customerPayload });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: recipient customer (LEGAL, different)', async () => {
    recipientPayload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
    const recipient = await Request.post(Endpoints.customer, { data: recipientPayload });
    await expect(recipient).CheckResponse();
    Responses.customer.push(await recipient.json());
  });

  await test.step('Precondition: electricity settlement POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  const customerId = asEntityId(Responses.customer[0]);
  const recipientId = asEntityId(Responses.customer[1]);
  expect(recipientId, 'recipient must differ from GC customer').not.toBe(customerId);
  const podId = asEntityId(Responses.pod[0]);
  const documentPeriod = previousMonthStartIso();
  const customerIdentifier = pickCreatedIdentifier(Responses.customer[0], customerPayload);
  const recipientIdentifier = pickCreatedIdentifier(Responses.customer[1], recipientPayload);
  expect(customerIdentifier, 'GC customer identifier from create').toBeTruthy();
  expect(recipientIdentifier, 'recipient identifier from create').toBeTruthy();

  return {
    customerId,
    recipientId,
    podId,
    documentPeriod,
    customerIdentifier,
    recipientIdentifier,
  };
}

function applyDirectDebitToLegalPayload(payload: Record<string, unknown>, iban: string): void {
  const banking = (payload.bankingDetails ?? {}) as Record<string, unknown>;
  banking.directDebit = true;
  banking.bankId = banking.bankId ?? envVariables.banks;
  banking.iban = iban;
  payload.bankingDetails = banking;
}

export type GcDdPrechainOptions = {
  invoiceCustomerIban: string;
  /** `null` = government institution created without Direct Debit. */
  institutionIban: string | null;
};

export type GcDdPrechainResult = DevVolCompPrechainResult & {
  invoiceCustomerIban: string;
  institutionIban: string | null;
};

/** Same FOR_VOLUMES chain as PDT-3087, with Direct Debit on billed customer, recipient, and contract. */
export async function runGcBillingPrechainWithDirectDebit(
  ctx: GcRegFx,
  options?: GcDdPrechainOptions,
): Promise<GcDdPrechainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const invoiceCustomerIban = options?.invoiceCustomerIban ?? DD_IBAN;
  const institutionIban = options === undefined ? DD_IBAN : options.institutionIban;

  await test.step('Precondition: billed customer with Direct Debit', async () => {
    const payload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
    applyDirectDebitToLegalPayload(payload, invoiceCustomerIban);
    const customer = await Request.post(Endpoints.customer, { data: payload });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step(
    institutionIban
      ? 'Precondition: recipient customer with Direct Debit'
      : 'Precondition: recipient customer without Direct Debit',
    async () => {
      const payload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
      if (institutionIban) {
        applyDirectDebitToLegalPayload(payload, institutionIban);
      }
      const recipient = await Request.post(Endpoints.customer, { data: payload });
      await expect(recipient).CheckResponse();
      Responses.customer.push(await recipient.json());
    },
  );

  await test.step('Precondition: price component (priceSettlement)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = '100';
    payload.applicationModelRequest.settlementPeriodsRequest.profiles = [
      { profileId: envVariables.profiles, percentage: 100 },
    ];
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  await test.step('Precondition: terms', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: POD settlement', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: product', async () => {
    const product = await Request.post(Endpoints.product, {
      data: GeneratePayload.productAndServices.product(),
    });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  let contractId = 0;
  await test.step('Precondition: product contract with Direct Debit', async () => {
    const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract()) as Record<
      string,
      unknown
    >;
    const additional = (contractPayload.additionalParameters ?? {}) as Record<string, unknown>;
    const banking = (additional.bankingDetails ?? {}) as Record<string, unknown>;
    banking.directDebit = true;
    banking.bankId = banking.bankId ?? envVariables.banks;
    banking.iban = invoiceCustomerIban;
    additional.bankingDetails = banking;
    contractPayload.additionalParameters = additional;
    const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    Responses.productContract.push(body);
    contractId = asEntityId(body);
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
    });
    await expect(podActivation).CheckResponse();
  });

  let bbpId = 0;
  let periodFrom = '';
  let periodTo = '';
  await test.step('Precondition: billing-by-profile', async () => {
    const payload = await GeneratePayload.energyData.profile15minute(0);
    payload.timeZone = 'CET';
    payload.profileId = envVariables.profiles;
    const bbp = await postPdt2915BillingByProfile(Request, payload, 'GC-REG DD BBP');
    await GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');
    bbpId = bbp.id;
    periodFrom = String(payload.periodFrom);
    periodTo = String(payload.periodTo);
    Responses.dataByProfiles.push({
      id: bbp.id,
      periodFrom,
      periodTo,
      periodType: payload.periodType,
    });
  });

  const customerId = asEntityId(Responses.customer[0]);
  const recipientCustomerId = asEntityId(Responses.customer[1]);
  expect(recipientCustomerId).not.toBe(customerId);
  const podId = asEntityId(Responses.pod[0]);

  return {
    customerId,
    recipientCustomerId,
    podId,
    contractId,
    bbpId,
    periodFrom,
    periodTo,
    documentPeriod: monthStartFromPeriod(periodFrom),
    invoiceCustomerIban,
    institutionIban,
  };
}

export async function runGcBillingPrechainDistinctInstitutionDd(
  ctx: GcRegFx,
): Promise<GcDdPrechainResult> {
  return runGcBillingPrechainWithDirectDebit(ctx, {
    invoiceCustomerIban: DD_IBAN_INVOICE_CUSTOMER,
    institutionIban: DD_IBAN_INSTITUTION,
  });
}

export async function runGcBillingPrechainInvoiceDdInstitutionNone(
  ctx: GcRegFx,
): Promise<GcDdPrechainResult> {
  return runGcBillingPrechainWithDirectDebit(ctx, {
    invoiceCustomerIban: DD_IBAN_INVOICE_ONLY,
    institutionIban: null,
  });
}

export async function createGcExpectingCreated(
  ctx: GcRegFx,
  opts: {
    customerId: number;
    podId: number;
    recipientId: number;
    documentPeriod: string;
    documentAmount: number;
    reason?: string;
    volumes?: number;
    price?: number;
    number?: string;
  },
): Promise<{ id: number; payload: CompensationPayload; status: number }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = buildCompensationPayload(GeneratePayload, {
    customerId: opts.customerId,
    podId: opts.podId,
    recipientId: opts.recipientId,
    documentPeriod: opts.documentPeriod,
    documentAmount: opts.documentAmount,
    reason: opts.reason ?? 'GC-REG',
    volumes: opts.volumes,
    price: opts.price,
  });
  payload.date = todayIso();
  if (opts.number) {
    payload.number = opts.number;
  }
  const res = await Request.post(Endpoints.compensation, { data: payload });
  await expect(res).CheckResponse();
  expect(res.status(), 'POST /government-compensations HTTP 201').toBe(201);
  const raw = await res.json();
  const id = asEntityId(raw);
  Responses.compensation.push({ id, ...payload });
  return { id, payload, status: res.status() };
}

export async function postGcRaw(
  ctx: GcRegFx,
  payload: CompensationPayload,
): Promise<{ status: number; bodyText: string }> {
  const res = await ctx.Request.post(ctx.Endpoints.compensation, { data: payload });
  const bodyText = await res.text();
  return { status: res.status(), bodyText };
}

export async function putGcRaw(
  ctx: GcRegFx,
  gcId: number,
  payload: CompensationPayload,
): Promise<{ status: number; bodyText: string }> {
  const res = await ctx.Request.put(`${COMP_ROOT}/${gcId}`, { data: payload });
  const bodyText = await res.text();
  return { status: res.status(), bodyText };
}

export async function patchRegenerateCompensationsRaw(
  Request: GcRegFx['Request'],
  invoiceId: number,
): Promise<{ status: number; bodyText: string }> {
  const res = await Request.patch(`invoice/regenerate-compensations?id=${invoiceId}`);
  const bodyText = await res.text();
  return { status: res.status(), bodyText };
}

export async function patchRegenerateCompensationsOk(
  Request: GcRegFx['Request'],
  invoiceId: number,
): Promise<void> {
  const res = await Request.patch(`invoice/regenerate-compensations?id=${invoiceId}`);
  await expect(res).CheckResponse();
  expect(res.status(), 'PATCH /invoice/regenerate-compensations HTTP 202').toBe(202);
}

export async function listCompensationsByNumber(
  Request: GcRegFx['Request'],
  number: string,
): Promise<{ status: number; content: Array<{ id?: number; number?: string; compensationStatus?: string }> }> {
  const res = await Request.get(`${COMP_ROOT}/listing`, {
    params: {
      size: 25,
      page: 0,
      searchBy: 'NUMBER',
      prompt: number,
      sortBy: 'CREATE_DATE',
      direction: 'DESC',
    },
  });
  const status = res.status();
  expect([200, 206], `listing status for ${number}`).toContain(status);
  const body = (await res.json()) as {
    content?: Array<{ id?: number; number?: string; compensationStatus?: string }>;
  };
  return { status, content: body.content ?? [] };
}

export async function getCustomerReceivable(
  Request: GcRegFx['Request'],
  receivableId: number,
): Promise<Record<string, unknown> & { id: number }> {
  const res = await Request.get(`customer-receivable/${receivableId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as Record<string, unknown> & { id?: number };
  return { ...body, id: Number(body.id ?? receivableId) };
}

export async function completeForVolumesToAccounted(ctx: GcRegFx): Promise<GcAccountedBilling> {
  const billing = await createForVolumesDraftBillingRun(ctx);
  expect(billing.draftInvoiceIds.length, 'draft invoices').toBeGreaterThanOrEqual(1);
  const invoiceId = billing.draftInvoiceIds[0];

  const genRes = await ctx.Request.patch(
    `${BILLING_RUN_ROOT}/start-generating?billingRunId=${billing.billingRunId}`,
  );
  await expect(genRes).CheckResponse();

  const afterGenerate = await pollBillingRunUntilStatuses(
    ctx.Request,
    billing.billingRunId,
    ['GENERATED', 'COMPLETED'],
    GC_REG_GEN_MAX_MS,
  );

  // Dev can skip GENERATED and land on COMPLETED (auto-accounting). Treat that as done.
  if (afterGenerate !== 'COMPLETED') {
    await pollPdfDocumentsPresent(ctx.Request, billing.billingRunId);
    const accRes = await ctx.Request.patch(
      `${BILLING_RUN_ROOT}/start-accounting?billingRunId=${billing.billingRunId}`,
    );
    await expect(accRes).CheckResponse();
    await pollBillingRunUntilStatuses(
      ctx.Request,
      billing.billingRunId,
      ['COMPLETED'],
      GC_REG_ACC_MAX_MS,
    );
  }

  return {
    billingRunId: billing.billingRunId,
    invoiceId,
    draftInvoiceIds: billing.draftInvoiceIds,
  };
}

async function pollBillingRunUntilStatuses(
  Request: GcRegFx['Request'],
  billingRunId: number,
  accepted: string[],
  timeoutMs: number,
): Promise<string> {
  const pollMs = 5_000;
  const maxAttempts = Math.max(1, Math.ceil(timeoutMs / pollMs));
  let last = '';
  for (let i = 1; i <= maxAttempts; i++) {
    last = await getBillingRunStatus(Request, billingRunId);
    if (accepted.includes(last)) {
      return last;
    }
    if (i < maxAttempts) {
      await new Promise((r) => setTimeout(r, pollMs));
    }
  }
  throw new Error(
    `Billing run ${billingRunId} did not reach ${accepted.join(' | ')} within ${timeoutMs}ms; final status: ${last}`,
  );
}

/**
 * CustomerLiabilityResponse / receivable preview: party is `customerResponse.id`
 * (CustomerDetailsShortResponse), not `customer.id` / `customerId`.
 */
export function extractPartyCustomerId(entity: Record<string, unknown>): number {
  const nested =
    (entity.customerResponse as { id?: unknown } | undefined)?.id ??
    (entity.customer as { id?: unknown } | undefined)?.id ??
    entity.customerId;
  const n = Number(nested);
  expect(Number.isFinite(n) && n > 0, `party customer id, got ${JSON.stringify({
    customerResponse: entity.customerResponse,
    customer: entity.customer,
    customerId: entity.customerId,
  })}`).toBe(true);
  return n;
}

/**
 * Invoice.liabilitiesAndReceivables lists GC recipient liability first after apply.
 * TC-BE-5 wants the invoice customer's main INVOICE liability (outgoingDocumentType=INVOICE).
 */
export async function resolveInvoiceMainCustomerLiability(
  Request: GcRegFx['Request'],
  invoiceId: number,
  invoiceCustomerId: number,
  excludeLiabilityIds: number[] = [],
): Promise<CustomerLiabilityView> {
  const invoice = await getInvoice(Request, invoiceId);
  const rows = invoice.liabilitiesAndReceivables ?? [];
  const exclude = new Set(excludeLiabilityIds.filter((id) => Number.isFinite(id) && id > 0));
  const candidates: CustomerLiabilityView[] = [];
  for (const row of rows) {
    if (String(row.type ?? '').toUpperCase() !== 'LIABILITY') {
      continue;
    }
    const id = Number(row.id);
    if (!Number.isFinite(id) || id <= 0 || exclude.has(id)) {
      continue;
    }
    const detail = await getCustomerLiability(Request, id);
    if (extractPartyCustomerId(detail as Record<string, unknown>) !== invoiceCustomerId) {
      continue;
    }
    candidates.push(detail);
  }
  const invoiced = candidates.filter(
    (row) => String((row as { outgoingDocumentType?: unknown }).outgoingDocumentType ?? '').toUpperCase() === 'INVOICE',
  );
  const pool = invoiced.length > 0 ? invoiced : candidates;
  const matched =
    pool.find((row) => Number(row.invoiceResponse?.id) === invoiceId) ?? pool[0];
  expect(
    matched?.id,
    `invoice ${invoiceId} customer ${invoiceCustomerId} main liability (rows=${JSON.stringify(rows)})`,
  ).toBeTruthy();
  return matched;
}

export function shortId(comp: CompensationView, key: string): number | null {
  const ref = comp[key] as { id?: number | null } | null | undefined;
  if (isNullishEntityRef(ref)) {
    return null;
  }
  const id = Number(ref?.id);
  return Number.isFinite(id) && id > 0 ? id : null;
}

export function assertGcUninvoicedEmptyFinancials(comp: CompensationView, label: string): void {
  assertCompensationUnlinked(comp, label);
  expect(isNullishEntityRef(comp.liabilityForRecipient as { id?: number } | null), `${label}: L recipient empty`).toBe(
    true,
  );
  expect(isNullishEntityRef(comp.receivableForCustomer as { id?: number } | null), `${label}: R customer empty`).toBe(
    true,
  );
  expect(isNullishEntityRef(comp.liabilityForCustomer as { id?: number } | null), `${label}: L customer empty`).toBe(
    true,
  );
  expect(isNullishEntityRef(comp.receivableForRecipient as { id?: number } | null), `${label}: R recipient empty`).toBe(
    true,
  );
}

export function assertPositiveApplyAttachments(
  comp: CompensationView,
  invoiceId: number,
  label: string,
): void {
  expect(comp.compensationStatus, `${label}: INVOICED`).toBe('INVOICED');
  expect(comp.index, `${label}: index 0`).toBe(0);
  expect(comp.invoice?.id, `${label}: invoice attached`).toBe(invoiceId);
  expect(shortId(comp, 'liabilityForRecipient'), `${label}: L recipient`).toBeTruthy();
  expect(shortId(comp, 'receivableForCustomer'), `${label}: R customer`).toBeTruthy();
  expect(shortId(comp, 'liabilityForCustomer'), `${label}: L customer empty on +amount`).toBeNull();
  expect(shortId(comp, 'receivableForRecipient'), `${label}: R recipient empty on +amount`).toBeNull();
}

export function assertNegativeApplyAttachments(
  comp: CompensationView,
  invoiceId: number,
  label: string,
): void {
  expect(comp.compensationStatus, `${label}: INVOICED`).toBe('INVOICED');
  expect(comp.index, `${label}: index 0`).toBe(0);
  expect(comp.invoice?.id, `${label}: invoice attached`).toBe(invoiceId);
  expect(shortId(comp, 'liabilityForCustomer'), `${label}: L customer`).toBeTruthy();
  expect(shortId(comp, 'receivableForRecipient'), `${label}: R recipient`).toBeTruthy();
  expect(shortId(comp, 'liabilityForRecipient'), `${label}: L recipient empty on -amount`).toBeNull();
  expect(shortId(comp, 'receivableForCustomer'), `${label}: R customer empty on -amount`).toBeNull();
}

export function billingGroupIsEmpty(liability: CustomerLiabilityView): boolean {
  const rec = liability as Record<string, unknown>;
  const bg = rec.billingGroupResponse ?? rec.billingGroup ?? rec.contractBillingGroupId;
  if (bg === null || bg === undefined || bg === '') {
    return true;
  }
  if (typeof bg === 'object' && bg !== null && 'id' in bg) {
    const id = (bg as { id?: number | null }).id;
    return id === null || id === undefined;
  }
  return false;
}

export function isDirectDebitOn(value: unknown): boolean {
  return value === true || value === 'true';
}

export async function getCustomerById(
  Request: GcRegFx['Request'],
  customerId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`customer/${customerId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function isEmptyOutgoingExternal(value: unknown): boolean {
  return value === null || value === undefined || String(value).trim() === '';
}

export function calendarDatePart(value: unknown): string {
  const s = String(value ?? '').trim();
  return s.length >= 10 ? s.slice(0, 10) : s;
}

export function pickInvoiceIncomeAccount(invoice: InvoiceView): string {
  const rec = invoice as Record<string, unknown>;
  return String(rec.incomeAccountNumber ?? rec.numberOfIncomeAccount ?? '').trim();
}

export function pickInvoiceCostCenter(invoice: InvoiceView): string {
  return String((invoice as Record<string, unknown>).costCenterControllingOrder ?? '').trim();
}

export function pickLiabilityIban(liability: Record<string, unknown>): string {
  return String(liability.iban ?? '').trim();
}

export function pickReceivableBankAccount(receivable: Record<string, unknown>): string {
  return String(receivable.bankAccount ?? receivable.iban ?? '').trim();
}

export function normalizeMoneyField(value: unknown): string {
  return String(value ?? '').trim();
}
