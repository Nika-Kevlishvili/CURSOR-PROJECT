/**
 * REG-963 — VAT base on STANDARD_BILLING (FOR_VOLUMES), then correction / reversal.
 *
 * Maps Backend TCs in Cursor-Project/test_cases/Backend/VAT_base_billing_run.md
 * (and the Excel workbook of the same name).
 *
 * Reference spec(s):
 * - tests/billing/forVolumes/forVolumes.spec.ts (REG-963 data prep + profile flow with vat base)
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts (billing-run poll / start-generating / accounting)
 * - tests/cursor/pdt-3223-invoice-json-tablepcscales-listpc-order.fixtures.ts (generate-invoice-data)
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts (INVOICE_CORRECTION volumeChange)
 * - tests/cursor/PHN-3951-cancelled-interim-invoice-price-component.spec.ts (INVOICE_REVERSAL)
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts (service + OVER_TIME_PERIODICAL billing)
 *
 * Swagger (dev, refreshed this session): POST /price-components (PriceComponentRequest:
 * doNotIncludeVatBase, customerDetailId), POST /billing-run, PATCH start-billing /
 * start-generating / start-accounting, GET /billing-run/{id}, GET draft-invoices,
 * GET /billing-run/generate-invoice-data (deprecated), GET /invoice, GET
 * /invoice/detailed-data, GET /invoice/summary-data, POST /invoice/listing,
 * GET /customer-liability/list, GET /customer-liability/{id}.
 */

import { expect } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import {
  asBillingRunId,
  BILLING_RUN_ROOT,
  pollBillingRunForStatus,
} from './rps-pod-invoice-due-date.fixtures';

export const REG_963_KEY = 'REG-963';
export const REG_963_TITLE = 'For volumes - vat base case';

export const GENERATE_INVOICE_DATA_PATH = 'billing-run/generate-invoice-data';

/** TC-BE-5 constraint text (PriceComponentRequest / alternativeRecipientCustomerDetailId). */
export const MSG_VAT_BASE_REQUIRES_ALT_RECIPIENT =
  'Alternative Recipient customer detail id must not be null when doNotIncludeVatBase is checked';

/** TC-BE-10 constraint text (alternateLiability). */
export const MSG_ALT_RECIPIENT_REQUIRES_VAT_BASE =
  'Alternate Liability must be null When Do not include in the VAT base is not selected';

export type Reg963Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type VatBasePcSpec = {
  expression: number;
  /** Index in Responses.customer after invoice customer (0) — 1 = first alt, 2 = second alt. */
  altCustomerIndex: number;
  name?: string;
  displayName?: string;
};

export type VolumesChainOpts = {
  regularExpression: number;
  vatBaseComponents: VatBasePcSpec[];
  profileValue?: number;
};

export type LiabilityListRow = {
  id?: number;
  initialAmount?: number | string;
  currentAmount?: number | string;
  creationType?: string;
  outgoingDocumentType?: string;
  billingGroupResponse?: unknown;
  invoiceResponse?: { id?: number };
  customerResponse?: { identifier?: string; id?: number };
};

function asNumericId(raw: unknown, label: string): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) return raw;
  if (raw !== null && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  throw new Error(`${label}: expected number or { id }, got ${JSON.stringify(raw)}`);
}

export function money(value: unknown): number {
  return Number(value);
}

export function amountsClose(actual: unknown, expected: number, digits = 2): boolean {
  const a = money(actual);
  if (!Number.isFinite(a)) return false;
  const factor = 10 ** digits;
  return Math.round(a * factor) === Math.round(expected * factor);
}

export function vatPercentFromInvoice(invoice: Record<string, unknown>): number {
  const rates = invoice.invoiceVatRateResponses as Array<Record<string, unknown>> | undefined;
  const first = rates?.[0] ?? {};
  const raw =
    first.percent ??
    first.vatPercent ??
    first.vatRatePercent ??
    first.rate ??
    first.value ??
    0;
  return Number(raw);
}

export async function createLegalCustomer(fx: Reg963Fx): Promise<Record<string, unknown>> {
  const res = await fx.Request.post(fx.Endpoints.customer, {
    data: fx.GeneratePayload.customers.customer_legal(),
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as Record<string, unknown>;
  fx.Responses.customer.push(body);
  return body;
}

export async function createTerm(fx: Reg963Fx): Promise<void> {
  const res = await fx.Request.post(fx.Endpoints.terms, {
    data: fx.GeneratePayload.productAndServices.term(),
  });
  await expect(res).CheckResponse();
  fx.Responses.terms.push(await res.json());
}

export async function createSettlementPriceComponent(
  fx: Reg963Fx,
  opts: {
    expression: number;
    doNotIncludeVatBase: boolean;
    customerDetailId: number | null;
    name?: string;
    displayName?: string;
  },
): Promise<number> {
  const payload = fx.GeneratePayload.productAndServices.priceSettlement();
  payload.formulaRequest.expression = opts.expression;
  payload.globalVatRate = true;
  payload.vatRateId = null;
  payload.doNotIncludeVatBase = opts.doNotIncludeVatBase;
  payload.customerDetailId = opts.customerDetailId;
  if (opts.name) payload.name = opts.name;
  if (opts.displayName) payload.displayName = opts.displayName;
  const res = await fx.Request.post(fx.Endpoints.priceComponent, { data: payload });
  await expect(res).CheckResponse();
  const id = asNumericId(await res.json(), 'POST /price-components');
  fx.Responses.priceComponent.push(id);
  return id;
}

export async function createProduct(fx: Reg963Fx): Promise<number> {
  const payload = fx.GeneratePayload.productAndServices.product();
  payload.contractTypes = ['SUPPLY_ONLY'];
  payload.paymentGuarantees = ['NO'];
  payload.availableForSale = true;
  payload.productStatus = 'ACTIVE';
  const res = await fx.Request.post(fx.Endpoints.product, { data: payload });
  await expect(res).CheckResponse();
  const id = asNumericId(await res.json(), 'POST /products');
  fx.Responses.product.push(id);
  return id;
}

export async function createSettlementPod(fx: Reg963Fx): Promise<void> {
  const res = await fx.Request.post(fx.Endpoints.pod, {
    data: fx.GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(res).CheckResponse();
  fx.Responses.pod.push(await res.json());
}

export async function createProductContract(fx: Reg963Fx): Promise<void> {
  const payload = await fx.GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
  // Product is created with contractTypes=['SUPPLY_ONLY']; default payload is WITHOUT_SUPPLY → 400.
  payload.productParameters.contractType = 'SUPPLY_ONLY';
  // Keep entryInForceDate null — API rejects past dates ("Entry in force should be in future").
  const res = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  await expect(res).CheckResponse();
  fx.Responses.productContract.push(await res.json());
}

export async function activatePod(fx: Reg963Fx, podIndex = 0): Promise<void> {
  const res = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(podIndex),
  });
  await expect(res).CheckResponse();
}

export async function createProfileData(
  fx: Reg963Fx,
  opts: { podIndex?: number; value: number; periodFrom?: string; periodTo?: string },
): Promise<Record<string, unknown>> {
  const payload = (await fx.GeneratePayload.energyData.profile1Month(opts.podIndex ?? 0)) as Record<
    string,
    unknown
  >;
  payload.timeZone = 'EET';
  payload.periodType = 'ONE_MONTH';
  if (opts.periodFrom) payload.periodFrom = opts.periodFrom;
  if (opts.periodTo) payload.periodTo = opts.periodTo;
  const entries = payload.entries as Array<Record<string, unknown>> | undefined;
  if (entries?.[0]) entries[0].value = opts.value;
  const res = await fx.Request.post('billing-by-profile', { data: payload });
  await expect(res).CheckResponse();
  const data = await res.json();
  fx.Responses.dataByProfiles.push({
    id: data,
    periodFrom: payload.periodFrom,
    periodTo: payload.periodTo,
    periodType: payload.periodType,
  });
  return payload;
}

export async function createStandardVolumesBillingRun(fx: Reg963Fx): Promise<number> {
  const payload = await fx.GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  payload.commonParameters.executionType = 'MANUAL';
  payload.commonParameters.periodicity = 'STANDARD';
  const res = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const raw = await res.json();
  fx.Responses.billingRun.push(raw);
  return asBillingRunId(raw);
}

export async function listDraftInvoices(
  fx: Reg963Fx,
  billingRunId: number,
): Promise<Record<string, unknown>[]> {
  const res = await fx.Request.get(
    `${BILLING_RUN_ROOT}/draft-invoices?id=${billingRunId}&page=0&size=25`,
  );
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Record<string, unknown>[] };
  return body.content ?? [];
}

/**
 * TC steps: start-billing → DRAFT → draft-invoices → start-generating → GENERATED
 * → (optional) start-accounting → COMPLETED.
 */
export async function runBillingThroughStatus(
  fx: Reg963Fx,
  billingRunId: number,
  stopAfter: 'GENERATED' | 'COMPLETED',
  pickDraft?: (drafts: Record<string, unknown>[]) => Record<string, unknown>,
): Promise<{ invoiceId: number; drafts: Record<string, unknown>[] }> {
  const snapRes = await fx.Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
  await expect(snapRes).CheckResponse();
  const snap = (await snapRes.json()) as { commonParameters?: { status?: string } };
  const statusBefore = String(snap.commonParameters?.status ?? '');
  // Reversal/correction POST can leave the run already in DRAFT — start-billing then returns 400.
  if (statusBefore === 'INITIAL' || statusBefore === '') {
    const startRes = await fx.Request.patch(
      `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
    );
    await expect(startRes).CheckResponse();
  }
  if (statusBefore !== 'DRAFT' && statusBefore !== 'GENERATED' && statusBefore !== 'COMPLETED') {
    await pollBillingRunForStatus(fx.Request, billingRunId, 'DRAFT');
  }

  const drafts = await listDraftInvoices(fx, billingRunId);
  expect(drafts.length, 'draft-invoices must return at least one row').toBeGreaterThanOrEqual(1);
  const chosen = pickDraft ? pickDraft(drafts) : drafts[0];
  const invoiceId = asNumericId(chosen.id ?? chosen, 'draft invoice id');
  fx.Responses.invoice.push(invoiceId);

  const afterDraftRes = await fx.Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
  await expect(afterDraftRes).CheckResponse();
  const afterDraft = (await afterDraftRes.json()) as { commonParameters?: { status?: string } };
  const statusAfterDraft = String(afterDraft.commonParameters?.status ?? '');
  if (statusAfterDraft === 'DRAFT') {
    const genRes = await fx.Request.patch(
      `${BILLING_RUN_ROOT}/start-generating?billingRunId=${billingRunId}`,
    );
    await expect(genRes).CheckResponse();
    await pollBillingRunForStatus(fx.Request, billingRunId, 'GENERATED', 3 * 60 * 1000);
  } else if (statusAfterDraft !== 'GENERATED' && statusAfterDraft !== 'COMPLETED') {
    await pollBillingRunForStatus(fx.Request, billingRunId, 'GENERATED', 3 * 60 * 1000);
  }

  if (stopAfter === 'COMPLETED') {
    const accSnapRes = await fx.Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
    await expect(accSnapRes).CheckResponse();
    const accSnap = (await accSnapRes.json()) as { commonParameters?: { status?: string } };
    if (String(accSnap.commonParameters?.status) === 'GENERATED') {
      const accRes = await fx.Request.patch(
        `${BILLING_RUN_ROOT}/start-accounting?billingRunId=${billingRunId}`,
      );
      await expect(accRes).CheckResponse();
      await pollBillingRunForStatus(fx.Request, billingRunId, 'COMPLETED', 3 * 60 * 1000);
    } else if (String(accSnap.commonParameters?.status) !== 'COMPLETED') {
      await pollBillingRunForStatus(fx.Request, billingRunId, 'COMPLETED', 3 * 60 * 1000);
    }
  }

  return { invoiceId, drafts };
}

export async function startAccountingToCompleted(fx: Reg963Fx, billingRunId: number): Promise<void> {
  const accRes = await fx.Request.patch(
    `${BILLING_RUN_ROOT}/start-accounting?billingRunId=${billingRunId}`,
  );
  await expect(accRes).CheckResponse();
  await pollBillingRunForStatus(fx.Request, billingRunId, 'COMPLETED', 3 * 60 * 1000);
}

export async function getInvoice(
  fx: Reg963Fx,
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${fx.Endpoints.invoice}?id=${invoiceId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function getInvoiceDetailedData(
  fx: Reg963Fx,
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(
    `${fx.Endpoints.invoice}/detailed-data?id=${invoiceId}&page=0&size=25`,
  );
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function getInvoiceSummaryData(
  fx: Reg963Fx,
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(
    `${fx.Endpoints.invoice}/summary-data?page=0&size=25&id=${invoiceId}`,
  );
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function fetchInvoiceDocumentModel(
  fx: Reg963Fx,
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${GENERATE_INVOICE_DATA_PATH}?invoiceId=${invoiceId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function listCustomerLiabilities(
  fx: Reg963Fx,
  customerIdentifier: string,
): Promise<LiabilityListRow[]> {
  const prompt = encodeURIComponent(customerIdentifier);
  const res = await fx.Request.get(
    `${fx.Endpoints.customerLiability}/list?page=0&size=25&columns=ID&direction=DESC&prompt=${prompt}&searchFields=CUSTOMER`,
  );
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: LiabilityListRow[] };
  return body.content ?? [];
}

export async function getCustomerLiability(
  fx: Reg963Fx,
  liabilityId: number,
): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${fx.Endpoints.customerLiability}/${liabilityId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

/**
 * GET /customer-liability/list does not include invoiceResponse. Hydrate each row
 * via GET /customer-liability/{id} and keep those linked to invoiceId.
 */
export async function liabilitiesForInvoice(
  fx: Reg963Fx,
  rows: LiabilityListRow[],
  invoiceId: number,
): Promise<Record<string, unknown>[]> {
  const matched: Record<string, unknown>[] = [];
  for (const row of rows) {
    const id = Number(row.id);
    if (!Number.isFinite(id) || id <= 0) continue;
    const detail = await getCustomerLiability(fx, id);
    const inv = detail.invoiceResponse as { id?: number } | undefined;
    if (Number(inv?.id) === invoiceId) matched.push(detail);
  }
  return matched;
}

/** FOR_VOLUMES bills unitPrice × kWh; TC expressions 100/50 are unit prices, not invoice nets. */
export function billedValueForUnitPrice(
  detailed: Record<string, unknown>,
  unitPrice: number,
): number {
  const content = (detailed.content ?? []) as Array<{ unitPrice?: unknown; value?: unknown }>;
  return content
    .filter((row) => amountsClose(row.unitPrice, unitPrice))
    .reduce((sum, row) => sum + money(row.value), 0);
}

export async function listInvoicesByBillingRun(
  fx: Reg963Fx,
  billingRunId: number,
): Promise<Record<string, unknown>[]> {
  const runRes = await fx.Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
  await expect(runRes).CheckResponse();
  const runBody = (await runRes.json()) as {
    commonParameters?: { billingNumber?: string };
  };
  const billingNumber = runBody.commonParameters?.billingNumber;
  expect(billingNumber, 'billingNumber required for POST /invoice/listing').toBeTruthy();
  const res = await fx.Request.post(`${fx.Endpoints.invoice}/listing`, {
    data: { page: 0, size: 50, searchBy: 'BILLING_RUN', billingRun: billingNumber },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Record<string, unknown>[] };
  return body.content ?? [];
}

export async function createVolumesVatBaseChain(
  fx: Reg963Fx,
  opts: VolumesChainOpts,
): Promise<{
  invoiceCustomer: Record<string, unknown>;
  altCustomers: Record<string, unknown>[];
  billingRunId: number;
  profilePayload: Record<string, unknown>;
}> {
  const invoiceCustomer = await createLegalCustomer(fx);

  const maxAltIndex = Math.max(0, ...opts.vatBaseComponents.map((c) => c.altCustomerIndex));
  const altCustomers: Record<string, unknown>[] = [];
  for (let i = 1; i <= maxAltIndex; i++) {
    altCustomers.push(await createLegalCustomer(fx));
  }

  await createSettlementPod(fx);
  await createTerm(fx);

  await createSettlementPriceComponent(fx, {
    expression: opts.regularExpression,
    doNotIncludeVatBase: false,
    customerDetailId: null,
  });

  for (const vatPc of opts.vatBaseComponents) {
    const alt = fx.Responses.customer[vatPc.altCustomerIndex] as Record<string, unknown>;
    const customerDetailId = Number(alt.lastCustomerDetailId);
    expect(customerDetailId, 'alt lastCustomerDetailId').toBeGreaterThan(0);
    await createSettlementPriceComponent(fx, {
      expression: vatPc.expression,
      doNotIncludeVatBase: true,
      customerDetailId,
      name: vatPc.name,
      displayName: vatPc.displayName,
    });
  }

  await createProduct(fx);
  await createProductContract(fx);
  await activatePod(fx, 0);
  const profilePayload = await createProfileData(fx, {
    podIndex: 0,
    value: opts.profileValue ?? 1000,
  });
  const billingRunId = await createStandardVolumesBillingRun(fx);

  return { invoiceCustomer, altCustomers, billingRunId, profilePayload };
}

export async function createPeriodicalVatBaseServiceChain(fx: Reg963Fx): Promise<{
  invoiceCustomer: Record<string, unknown>;
  altCustomer: Record<string, unknown>;
  billingRunId: number;
}> {
  const invoiceCustomer = await createLegalCustomer(fx);
  const altCustomer = await createLegalCustomer(fx);
  await createTerm(fx);
  // Default service executionLevel is POINT_OF_DELIVERY — Phoenix requires podIds (or
  // unrecognizedPods). Periodical billing still does not need billing-by-profile volumes.
  await createSettlementPod(fx);

  const pcPayload = JSON.parse(
    JSON.stringify(fx.GeneratePayload.productAndServices.periodicalComponent()),
  ) as Record<string, unknown>;
  const formula = pcPayload.formulaRequest as Record<string, unknown>;
  formula.expression = 50;
  pcPayload.globalVatRate = true;
  pcPayload.vatRateId = null;
  pcPayload.doNotIncludeVatBase = true;
  pcPayload.customerDetailId = Number(altCustomer.lastCustomerDetailId);
  const appModel = pcPayload.applicationModelRequest as Record<string, unknown>;
  appModel.applicationModelType = 'PRICE_AM_OVERTIME';
  appModel.applicationType = 'PERIODICALLY';
  appModel.applicationLevel = 'CONTRACT';

  const pcRes = await fx.Request.post(fx.Endpoints.priceComponent, { data: pcPayload });
  await expect(pcRes).CheckResponse();
  fx.Responses.priceComponent.push(asNumericId(await pcRes.json(), 'periodical price component'));

  const servicePayload = fx.GeneratePayload.productAndServices.service() as {
    availableForSale?: boolean;
  };
  servicePayload.availableForSale = true;
  const serviceRes = await fx.Request.post(fx.Endpoints.service, { data: servicePayload });
  await expect(serviceRes).CheckResponse();
  fx.Responses.service.push(asNumericId(await serviceRes.json(), 'POST /services'));

  const scPayload = await fx.GeneratePayload.contractsAndOrders.serviceContract();
  scPayload.basicParameters.contractStatus = 'ENTERED_INTO_FORCE';
  scPayload.serviceParameters.paymentGuarantee = 'NO';
  const scRes = await fx.Request.post(fx.Endpoints.serviceContract, { data: scPayload });
  await expect(scRes).CheckResponse();
  fx.Responses.serviceContract.push(asNumericId(await scRes.json(), 'POST /service-contract'));

  const billingPayload = await fx.GeneratePayload.billing.billingRun('CONTRACT', [
    'OVER_TIME_PERIODICAL',
  ]);
  billingPayload.commonParameters.executionType = 'MANUAL';
  billingPayload.commonParameters.periodicity = 'STANDARD';
  // Empty-string maxEndDate on the FOR_VOLUMES template yields no periodical drafts (PDT-2599).
  billingPayload.basicParameters.maxEndDate = null;
  billingPayload.basicParameters.periodicMaxEndDate = null;
  billingPayload.basicParameters.periodicMaxEndDateValue = null;
  const brRes = await fx.Request.post(fx.Endpoints.billingRun, { data: billingPayload });
  await expect(brRes).CheckResponse();
  const raw = await brRes.json();
  fx.Responses.billingRun.push(raw);
  return {
    invoiceCustomer,
    altCustomer,
    billingRunId: asBillingRunId(raw),
  };
}

export function pickDraftByDocumentType(
  drafts: Record<string, unknown>[],
  documentType: string,
): Record<string, unknown> {
  const match = drafts.find(
    (d) =>
      String(d.invoiceDocumentType) === documentType || String(d.invoiceType) === documentType,
  );
  expect(match, `draft with invoiceDocumentType/invoiceType=${documentType}`).toBeTruthy();
  return match!;
}

export function excludedPcRows(doc: Record<string, unknown>): Array<Record<string, unknown>> {
  const sd = (doc.SDExcludedPC ?? doc.sdExcludedPC ?? []) as Array<Record<string, unknown>>;
  return Array.isArray(sd) ? sd : [];
}

export function tableExcludedPcRows(doc: Record<string, unknown>): Array<Record<string, unknown>> {
  const dd = doc.DD as Array<Record<string, unknown>> | undefined;
  if (!Array.isArray(dd) || dd.length === 0) return [];
  const table = (dd[0].TableExcludedPC ?? dd[0].tableExcludedPC ?? []) as Array<
    Record<string, unknown>
  >;
  return Array.isArray(table) ? table : [];
}

export function excludedRowMatches(
  rows: Array<Record<string, unknown>>,
  expectedValue: number,
  pcNeedle: string,
): boolean {
  return rows.some((row) => {
    // Document model stores unit price in Price and billed net in Value (FOR_VOLUMES).
    const billed = money(row.Value ?? row.value);
    const unitPrice = money(row.Price ?? row.price);
    const pc = String(row.PC ?? row.pc ?? '');
    const amountOk = amountsClose(billed, expectedValue) || amountsClose(unitPrice, expectedValue);
    return amountOk && pc.includes(pcNeedle);
  });
}

export async function postPriceComponentExpecting400(
  fx: Reg963Fx,
  payload: unknown,
): Promise<{ status: number; bodyText: string }> {
  const res = await fx.Request.post(fx.Endpoints.priceComponent, { data: payload });
  const status = res.status();
  const bodyText = await res.text();
  return { status, bodyText };
}

export function customerIdentifier(customer: Record<string, unknown>): string {
  const id = String(customer.identifier ?? '');
  expect(id, 'customer.identifier').toBeTruthy();
  return id;
}

export { pollBillingRunForStatus, BILLING_RUN_ROOT, asBillingRunId };
