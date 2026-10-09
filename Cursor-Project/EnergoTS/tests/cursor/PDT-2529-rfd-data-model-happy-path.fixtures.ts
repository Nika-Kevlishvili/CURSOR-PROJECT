/**
 * PDT-2529 — RFD data model (multi-POD, single product contract, LIST_OF_CUSTOMERS DRAFT request).
 *
 * **Scale note:** {@link RFD_PODS_PER_CONTRACT} documents the intended upper bound of PODs per
 * product contract in performance seeds (e.g. 25,000 PODs → 100 contracts × 250 PODs). This
 * happy-path spec uses {@link RFD_HAPPY_POD_COUNT} only (default 3).
 *
 * Reference specs:
 * - `tests/receivableManagement/requestForDisconnection.spec.ts` ([REG-1045] billing + reminder + RFD)
 * - `tests/bigData/volumesBigData.spec.ts` (multi-POD loop, single `product_contract()`, per-POD profile + billing-by-profile)
 * - `tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts` (`createRequestCDraftListCustomers` / LIST_OF_CUSTOMERS DRAFT)
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { profile1Month as profile1MonthTemplate } from '../../jsons/payloads/create/energyData/profile1Month';
import { randomGens } from '../../utils/randomGens';
import {
  resolvePdt2376BillingAnchor,
  type Pdt2376BillingAnchor,
} from './pdt-2376-volume-with-electricity.fixtures';

/** Re-export for PDT-2529 volume billing (historical invoice period + OPEN accounting period). */
export { resolvePdt2376BillingAnchor as resolveRfdVolumeBillingAnchor };
export type { Pdt2376BillingAnchor as RfdVolumeBillingAnchor };

/** Minimal invoice payment term so due date does not land on today when invoice date is historical. */
export const RFD_MIN_INVOICE_PAYMENT_TERM_DAYS = 1;

function parseRfdHappyPodCount(): number {
  const raw = process.env.RFD_HAPPY_POD_COUNT;
  const fallback = 3;
  if (raw === undefined || raw === '') {
    return fallback;
  }
  const n = parseInt(raw, 10);
  if (!Number.isFinite(n) || n < 1) {
    return fallback;
  }
  return n;
}

/** PODs created in the happy-path spec (`RFD_HAPPY_POD_COUNT` env; default **3**). */
export const RFD_HAPPY_POD_COUNT = parseRfdHappyPodCount();

/**
 * Documented maximum PODs per single product contract for future large-scale seeds.
 * Example: 25k PODs ≈ 100 contracts × 250 PODs.
 */
export const RFD_PODS_PER_CONTRACT = 250;

/** Terms payload with {@link RFD_MIN_INVOICE_PAYMENT_TERM_DAYS} working days (not default 10). */
export function termPayloadWithMinimalPaymentDays(
  GeneratePayload: baseFixture['GeneratePayload'],
): Record<string, unknown> {
  const payload = GeneratePayload.productAndServices.term() as Record<string, unknown>;
  const terms = payload.invoicePaymentTerms as Array<Record<string, unknown>>;
  if (terms?.[0]) {
    terms[0].value = RFD_MIN_INVOICE_PAYMENT_TERM_DAYS;
    terms[0].name = `${RFD_MIN_INVOICE_PAYMENT_TERM_DAYS} WORKING_DAYS ( )`;
  }
  return payload;
}

export function profileDateRangeFromAnchor(anchor: Pdt2376BillingAnchor): { startDate: string; endDate: string }[] {
  return [{ startDate: anchor.profileStart, endDate: anchor.profileEnd }];
}

/** Billing group flag: separate invoice for each point of delivery (see invoiceSlotSplitting.spec.ts). */
export async function enableSeparateInvoiceForEachPod(
  Request: baseFixture['Request'],
  GeneratePayload: baseFixture['GeneratePayload'],
): Promise<{ billingGroupId: number; separateInvoiceForEachPod: true }> {
  const payload = await GeneratePayload.contractsAndOrders.editBillingGroup(0);
  payload.separateInvoiceForEachPod = true;
  const editRes = await Request.put(`billing-group/${payload.id}`, { data: payload });
  await assertPdt2529Response(editRes, 'billing-group separate invoice per POD');
  return { billingGroupId: payload.id as number, separateInvoiceForEachPod: true };
}

/** IDs and manual-billing hints after seed-only run (no billing run started by automation). */
export async function buildDataSeedSummary(
  Request: baseFixture['Request'],
  Responses: baseFixture['Responses'],
  anchor: Pdt2376BillingAnchor,
  billingGroupId: number,
): Promise<Record<string, unknown>> {
  const contractId = Responses.productContract[0]?.id;
  let contractNumber: string | null = null;
  if (contractId != null) {
    const cg = await Request.get(`product-contract/${contractId}?version=1`);
    if (cg.ok()) {
      const cj = await cg.json();
      contractNumber = cj.basicParameters?.contractNumber ?? null;
    }
  }

  const podIdentifiers: string[] = [];
  for (const p of Responses.pod) {
    const pg = await Request.get(`pod/${p.id}?version=1`);
    if (pg.ok()) {
      const pj = await pg.json();
      if (pj.identifier) podIdentifiers.push(String(pj.identifier));
    }
  }

  return {
    jiraKey: 'PDT-2529',
    scenario: 'rfd-data-seed-only',
    readyForManualBilling: true,
    invoicePaymentTermDays: RFD_MIN_INVOICE_PAYMENT_TERM_DAYS,
    separateInvoiceForEachPod: true,
    billingGroupId,
    customer: Responses.customer[0]
      ? { id: Responses.customer[0].id, identifier: Responses.customer[0].identifier as string | undefined }
      : null,
    productContractId: contractId ?? null,
    contractNumber,
    podCount: Responses.pod.length,
    podIds: Responses.pod.map((p: { id: number }) => p.id),
    podIdentifiers,
    billingAnchor: anchor,
    manualBillingRunHints: {
      applicationModelType: ['FOR_VOLUMES'],
      billingApplicationLevel: 'CONTRACT',
      listOfCustomersContractsOrPOD: contractNumber,
      commonParameters: {
        accountingPeriodId: anchor.accountingPeriodId,
        taxEventDate: anchor.taxEventDate,
        invoiceDate: anchor.invoiceDate,
        invoiceDueDate: 'ACCORDING_TO_THE_CONTRACT',
      },
      basicParameters: { maxEndDate: anchor.invoicePeriodTo },
    },
    dataByProfilesCount: Responses.dataByProfiles?.length ?? 0,
  };
}

/** Contract + POD ids after prep run (includes separate-invoice flag + billing-by-profile; no billing run). */
export async function buildContractPrepSummary(
  Request: baseFixture['Request'],
  Responses: baseFixture['Responses'],
  extras?: { anchor?: Pdt2376BillingAnchor; billingGroupId?: number },
): Promise<Record<string, unknown>> {
  const contractId = Responses.productContract[0]?.id;
  let contractNumber: string | null = null;
  if (contractId != null) {
    const cg = await Request.get(`product-contract/${contractId}?version=1`);
    if (cg.ok()) {
      const cj = await cg.json();
      contractNumber = cj.basicParameters?.contractNumber ?? null;
    }
  }

  const podIdentifiers: string[] = [];
  for (const p of Responses.pod) {
    const pg = await Request.get(`pod/${p.id}?version=1`);
    if (pg.ok()) {
      const pj = await pg.json();
      if (pj.identifier) podIdentifiers.push(String(pj.identifier));
    }
  }

  const anchor = extras?.anchor;
  const billingGroupId = extras?.billingGroupId;

  return {
    jiraKey: 'PDT-2529',
    scenario: 'contract-prep-only',
    readyForManualBilling: true,
    invoicePaymentTermDays: RFD_MIN_INVOICE_PAYMENT_TERM_DAYS,
    separateInvoiceForEachPod: billingGroupId != null,
    billingGroupId: billingGroupId ?? null,
    customer: Responses.customer[0]
      ? { id: Responses.customer[0].id, identifier: Responses.customer[0].identifier as string | undefined }
      : null,
    productContractId: contractId ?? null,
    contractNumber,
    podCount: Responses.pod.length,
    podIds: Responses.pod.map((p: { id: number }) => p.id),
    podIdentifiers,
    dataByProfilesCount: Responses.dataByProfiles?.length ?? 0,
    billingAnchor: anchor ?? null,
    manualBillingRunHints: anchor
      ? {
          applicationModelType: ['FOR_VOLUMES'],
          billingApplicationLevel: 'CONTRACT',
          listOfCustomersContractsOrPOD: contractNumber,
          commonParameters: {
            accountingPeriodId: anchor.accountingPeriodId,
            taxEventDate: anchor.taxEventDate,
            invoiceDate: anchor.invoiceDate,
            invoiceDueDate: 'ACCORDING_TO_THE_CONTRACT',
          },
          basicParameters: { maxEndDate: anchor.invoicePeriodTo },
        }
      : null,
  };
}

export type Pdt2529PrepContext = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

/**
 * Bulk mode: lean HTTP status checks only (no CheckResponse), no report attachments,
 * no extra GETs for profiles when POD identifier is already known.
 */
export function isPdt2529BulkFast(): boolean {
  return process.env.PDT2529_BULK_FAST === '1';
}

type Pdt2529HttpResponse = {
  ok: () => boolean;
  status: () => number;
  text?: () => Promise<string>;
};

async function assertPdt2529Response(res: Pdt2529HttpResponse, label: string): Promise<void> {
  if (isPdt2529BulkFast()) {
    if (!res.ok()) {
      const body = res.text ? await res.text().catch(() => '') : '';
      throw new Error(`${label}: HTTP ${res.status()} ${body}`);
    }
    return;
  }
  await (expect(res) as any).CheckResponse();
}

function buildProfile1MonthPayload(
  podIdentifier: string,
  anchor: Pdt2376BillingAnchor,
): Record<string, unknown> {
  const payload = profile1MonthTemplate() as Record<string, unknown>;
  const dateRanges = profileDateRangeFromAnchor(anchor);
  payload.identifier = podIdentifier;
  payload.timeZone = 'CET';
  payload.periodFrom = `${dateRanges[0].startDate}T00:00:00.000Z`;
  payload.periodTo = `${dateRanges[0].endDate}T00:00:00.000Z`;
  const entries = payload.entries as Array<Record<string, unknown>>;
  if (entries[0]) {
    entries[0].periodFrom = `${dateRanges[0].startDate}T00:00:00.000Z`;
  }
  return payload;
}

/**
 * Reuse one term + price + product per Playwright worker (safe: each iteration still gets its own customer/POD/contract).
 * Cuts ~3 POSTs per iteration when running thousands of repeats.
 */
export function isPdt2529BulkReuseCatalog(): boolean {
  return process.env.PDT2529_BULK_REUSE_CATALOG === '1';
}

type Pdt2529WorkerCatalog = {
  termJson: Record<string, unknown>;
  priceJson: Record<string, unknown>;
  productJson: Record<string, unknown>;
};

const workerCatalogByIndex = new Map<number, Promise<Pdt2529WorkerCatalog>>();
const workerAnchorByIndex = new Map<number, Promise<Pdt2376BillingAnchor>>();

async function ensurePdt2529BillingAnchor(
  Request: baseFixture['Request'],
  workerIndex: number,
): Promise<Pdt2376BillingAnchor> {
  if (isPdt2529BulkReuseCatalog()) {
    let pending = workerAnchorByIndex.get(workerIndex);
    if (!pending) {
      pending = resolvePdt2376BillingAnchor(Request);
      workerAnchorByIndex.set(workerIndex, pending);
    }
    return pending;
  }
  return resolvePdt2376BillingAnchor(Request);
}

/** `separateInvoiceForEachPod` on contract billing group + monthly billing-by-profile per POD (volume anchor). */
export async function postPdt2529BillingByProfileForAllPods(
  ctx: Pdt2529PrepContext,
  anchor: Pdt2376BillingAnchor,
): Promise<void> {
  const { Request, GeneratePayload, Responses } = ctx;
  const dateRanges = profileDateRangeFromAnchor(anchor);

  for (let i = 0; i < RFD_HAPPY_POD_COUNT; i++) {
    const podRecord = Responses.pod[i] as { identifier?: string };
    const payload =
      isPdt2529BulkFast() && podRecord.identifier
        ? buildProfile1MonthPayload(String(podRecord.identifier), anchor)
        : await (async () => {
            const p = await GeneratePayload.energyData.profile1Month(i, dateRanges);
            p.timeZone = 'CET';
            return p;
          })();
    const profiles = await Request.post('billing-by-profile', { data: payload });
    await assertPdt2529Response(profiles, `billing-by-profile POD index ${i}`);
    if (!isPdt2529BulkFast()) {
      const profileData = await profiles.json();
      Responses.dataByProfiles.push({
        id: profileData,
        periodFrom: payload.periodFrom,
        periodTo: payload.periodTo,
        periodType: payload.periodType,
      });
    }
  }
}

async function createPdt2529WorkerCatalog(ctx: Pdt2529PrepContext): Promise<Pdt2529WorkerCatalog> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const termRes = await Request.post(Endpoints.terms, {
    data: termPayloadWithMinimalPaymentDays(GeneratePayload),
  });
  await assertPdt2529Response(termRes, 'terms');
  const termJson = (await termRes.json()) as Record<string, unknown>;

  const pricePayload = GeneratePayload.productAndServices.priceSettlement();
  const priceRes = await Request.post(Endpoints.priceComponent, { data: pricePayload });
  await assertPdt2529Response(priceRes, 'price component');
  const priceJson = (await priceRes.json()) as Record<string, unknown>;

  // product() reads term + priceComponent from Responses — stage temporarily, then remove
  const termsLenBefore = Responses.terms.length;
  const priceLenBefore = Responses.priceComponent.length;
  Responses.terms.push(termJson as (typeof Responses.terms)[number]);
  Responses.priceComponent.push(priceJson as (typeof Responses.priceComponent)[number]);

  const productRes = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
  await assertPdt2529Response(productRes, 'product');
  const productJson = (await productRes.json()) as Record<string, unknown>;

  Responses.terms.splice(termsLenBefore);
  Responses.priceComponent.splice(priceLenBefore);

  return { termJson, priceJson, productJson };
}

async function ensurePdt2529WorkerCatalog(ctx: Pdt2529PrepContext, workerIndex: number): Promise<Pdt2529WorkerCatalog> {
  let pending = workerCatalogByIndex.get(workerIndex);
  if (!pending) {
    pending = createPdt2529WorkerCatalog(ctx);
    workerCatalogByIndex.set(workerIndex, pending);
  }
  return pending;
}

/** POD identifier with worker + iteration suffix (parallel / repeat-each safe). */
export function podSettlementPayloadUnique(
  GeneratePayload: baseFixture['GeneratePayload'],
  workerIndex: number,
  iteration: number,
): Record<string, unknown> {
  const payload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
  const suffix = `${workerIndex}${String(iteration).padStart(5, '0')}`;
  const core = randomGens.generateUniqueIdentifier();
  payload.identifier = (`32X${core}${suffix}`).slice(0, 33);
  return payload;
}

export type Pdt2529ContractPrepResult = {
  billingGroupId: number;
  anchor: Pdt2376BillingAnchor;
};

/**
 * PDT-2529 contract prep chain (customer → PODs → contract → activation → separate invoice per POD → billing-by-profile).
 * Catalog entities optional via {@link isPdt2529BulkReuseCatalog}.
 * No automated billing run — operator runs billing manually.
 */
export async function runPdt2529ContractPrep(
  ctx: Pdt2529PrepContext,
  opts?: { workerIndex?: number; iteration?: number },
): Promise<Pdt2529ContractPrepResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const workerIndex = opts?.workerIndex ?? 0;
  const iteration = opts?.iteration ?? 0;

  if (isPdt2529BulkReuseCatalog()) {
    const catalog = await ensurePdt2529WorkerCatalog(ctx, workerIndex);
    Responses.terms.push(catalog.termJson as (typeof Responses.terms)[number]);
    Responses.priceComponent.push(catalog.priceJson as (typeof Responses.priceComponent)[number]);
    Responses.product.push(catalog.productJson as (typeof Responses.product)[number]);
  } else {
    const term = await Request.post(Endpoints.terms, {
      data: termPayloadWithMinimalPaymentDays(GeneratePayload),
    });
    await assertPdt2529Response(term, 'terms');
    Responses.terms.push(await term.json());

    const pricePayload = GeneratePayload.productAndServices.priceSettlement();
    const price = await Request.post(Endpoints.priceComponent, { data: pricePayload });
    await assertPdt2529Response(price, 'price component');
    Responses.priceComponent.push(await price.json());

    const product = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
    await assertPdt2529Response(product, 'product');
    Responses.product.push(await product.json());
  }

  const customer = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
  await assertPdt2529Response(customer, 'customer');
  Responses.customer.push(await customer.json());

  for (let i = 0; i < RFD_HAPPY_POD_COUNT; i++) {
    const podPayload = podSettlementPayloadUnique(GeneratePayload, workerIndex, iteration * 100 + i);
    const podSettlement = await Request.post(Endpoints.pod, { data: podPayload });
    await assertPdt2529Response(podSettlement, `POD ${i}`);
    const podJson = (await podSettlement.json()) as Record<string, unknown>;
    if (!podJson.identifier) {
      podJson.identifier = podPayload.identifier;
    }
    Responses.pod.push(podJson as (typeof Responses.pod)[number]);
  }

  const contract = await Request.post(Endpoints.productContract, {
    data: await GeneratePayload.contractsAndOrders.product_contract(),
  });
  await assertPdt2529Response(contract, 'product contract');
  Responses.productContract.push(await contract.json());

  for (let i = 0; i < RFD_HAPPY_POD_COUNT; i++) {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(i),
    });
    await assertPdt2529Response(podActivation, `POD activation ${i}`);
  }

  const { billingGroupId } = await enableSeparateInvoiceForEachPod(Request, GeneratePayload);
  const anchor = await ensurePdt2529BillingAnchor(Request, workerIndex);
  await postPdt2529BillingByProfileForAllPods(ctx, anchor);

  return { billingGroupId, anchor };
}

/** Align FOR_VOLUMES billing run to historical period (overdue liability without manual due-date hack). */
function isoDateOnly(value: string | null | undefined): string | null {
  if (value == null || value === '') return null;
  return String(value).slice(0, 10);
}

function todayIsoDateOnly(): string {
  return new Date().toISOString().slice(0, 10);
}

function isStrictlyBeforePastDate(ymd: string | null, todayYmd: string): boolean {
  return ymd != null && ymd < todayYmd;
}

export type RealizedInvoiceLiabilityCheck = {
  invoiceId: number;
  invoiceStatus: string;
  invoiceDate: string;
  taxEventDate: string | null;
  liabilityId: number;
  liabilityDueDate: string;
  liabilityStatus: string;
  liabilityCurrentAmount: number;
};

/**
 * After `waitForInvoiceGeneration(..., completeBillingRun: true)`:
 * - billing run is COMPLETED
 * - each invoice is REAL with invoice/tax dates in the past
 * - each invoice has a liability with due date in the past and amount > 0
 */
export async function assertRealizedVolumeInvoicesWithPastLiabilities(
  Request: baseFixture['Request'],
  Responses: baseFixture['Responses'],
  anchor: Pdt2376BillingAnchor,
): Promise<RealizedInvoiceLiabilityCheck[]> {
  const todayYmd = todayIsoDateOnly();
  const checks: RealizedInvoiceLiabilityCheck[] = [];

  const rawBillingRun = Responses.billingRun[0];
  const billingRunId =
    typeof rawBillingRun === 'object' && rawBillingRun !== null && 'id' in rawBillingRun
      ? (rawBillingRun as { id: number }).id
      : (rawBillingRun as number);

  const billingRunRes = await Request.get(`billing-run/${billingRunId}`);
  await (expect(billingRunRes) as any).CheckResponse();
  const billingRunJson = await billingRunRes.json();
  expect(billingRunJson.commonParameters?.status, 'Billing run must finish accounting (COMPLETED)').toBe('COMPLETED');

  expect(Responses.invoice.length, 'Expected at least one realized invoice id').toBeGreaterThan(0);

  for (const invoiceId of Responses.invoice) {
    const invoiceRes = await Request.get(`invoice?id=${invoiceId}`);
    await (expect(invoiceRes) as any).CheckResponse();
    const invoice = await invoiceRes.json();

    const invoiceDate = isoDateOnly(invoice.invoiceDate);
    const taxEventDate = isoDateOnly(invoice.taxEventDate);

    expect(invoice.invoiceStatus, `Invoice ${invoiceId} must be REAL (accounted)`).toBe('REAL');
    expect(
      isStrictlyBeforePastDate(invoiceDate, todayYmd),
      `Invoice ${invoiceId} invoiceDate ${invoiceDate} must be before today ${todayYmd} (anchor ${anchor.invoiceDate})`,
    ).toBe(true);
    if (taxEventDate) {
      expect(
        isStrictlyBeforePastDate(taxEventDate, todayYmd),
        `Invoice ${invoiceId} taxEventDate ${taxEventDate} must be before today`,
      ).toBe(true);
    }

    const liabilityRef = invoice.liabilitiesAndReceivables?.[0];
    expect(liabilityRef?.id, `Invoice ${invoiceId} must have a customer liability after accounting`).toBeTruthy();

    const liabilityRes = await Request.get(`customer-liability/${liabilityRef.id}`);
    await (expect(liabilityRes) as any).CheckResponse();
    const liability = await liabilityRes.json();

    const liabilityDueDate = isoDateOnly(liability.dueDate);
    expect(liabilityDueDate, `Liability ${liabilityRef.id} must have dueDate`).toBeTruthy();
    expect(
      isStrictlyBeforePastDate(liabilityDueDate, todayYmd),
      `Liability ${liabilityRef.id} dueDate ${liabilityDueDate} must be before today (payment term ${RFD_MIN_INVOICE_PAYMENT_TERM_DAYS}d on historical invoice)`,
    ).toBe(true);
    expect(String(liability.status).toUpperCase()).toBe('ACTIVE');
    expect(Number(liability.currentAmount)).toBeGreaterThan(0);

    checks.push({
      invoiceId: Number(invoiceId),
      invoiceStatus: invoice.invoiceStatus,
      invoiceDate: invoiceDate!,
      taxEventDate,
      liabilityId: liabilityRef.id,
      liabilityDueDate: liabilityDueDate!,
      liabilityStatus: liability.status,
      liabilityCurrentAmount: Number(liability.currentAmount),
    });
  }

  return checks;
}

export function applyVolumeBillingAnchor(
  billingPayload: Awaited<ReturnType<baseFixture['GeneratePayload']['billing']['billingRun']>>,
  anchor: Pdt2376BillingAnchor,
): void {
  billingPayload.commonParameters.accountingPeriodId = anchor.accountingPeriodId;
  billingPayload.commonParameters.taxEventDate = anchor.taxEventDate;
  billingPayload.commonParameters.invoiceDate = anchor.invoiceDate;
  billingPayload.commonParameters.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
  billingPayload.basicParameters.maxEndDate = anchor.invoicePeriodTo;
}

export function reminderIdFromResponses(Responses: { reminderForDisconnection: unknown[] }): number {
  const r = Responses.reminderForDisconnection[0] as number | { id: number };
  return typeof r === 'number' ? r : r.id;
}

function rfdPayloadTemplate(
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
): Record<string, unknown> {
  const payload = GeneratePayload.receivablesManagement.requestForDisconnection() as Record<string, unknown>;
  payload.reminderForDisconnectionId = reminderIdFromResponses(Responses);
  return payload;
}

/**
 * POST DRAFT Request for Disconnection — `conditionType: LIST_OF_CUSTOMERS`, `allSelected: true`, `pods: []`.
 * See Swagger `DPSRequestsBaseRequest` (`disconnectionRequestsStatus` includes `DRAFT`).
 */
export async function createRequestCDraftListCustomers(
  Request: baseFixture['Request'],
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Promise<number> {
  const payload = rfdPayloadTemplate(GeneratePayload, Responses);
  payload.disconnectionRequestsStatus = 'DRAFT';
  payload.conditionType = 'LIST_OF_CUSTOMERS';
  payload.listOfCustomer = Responses.customer[0].identifier as string;
  payload.allSelected = true;
  payload.pods = [];
  payload.podWithHighestConsumption = false;
  const res = await Request.post(Endpoints.requestForDisconnection, { data: payload });
  await (expect(res) as any).CheckResponse();
  return Number(await res.json());
}

/** Compact JSON for `test.info().attach` (ids + counts; no full entity bodies). */
export function buildDataModelSummary(Responses: baseFixture['Responses']): Record<string, unknown> {
  const reminderRaw = Responses.reminderForDisconnection[0];
  const reminderId =
    reminderRaw === undefined ? null : typeof reminderRaw === 'number' ? reminderRaw : (reminderRaw as { id: number }).id;

  return {
    jiraKey: 'PDT-2529',
    scenario: 'rfd-data-model-happy-path',
    rfdHappyPodCount: RFD_HAPPY_POD_COUNT,
    documentedPodsPerContractCap: RFD_PODS_PER_CONTRACT,
    customer: Responses.customer[0]
      ? { id: Responses.customer[0].id, identifier: Responses.customer[0].identifier as string | undefined }
      : null,
    podCount: Responses.pod.length,
    podIds: Responses.pod.map((p: { id: number }) => p.id),
    productContractId: Responses.productContract[0]?.id ?? null,
    billingRunIds: Responses.billingRun.map((b: { id?: number }) => b?.id).filter((id: unknown) => id != null),
    invoiceCount: Responses.invoice.length,
    invoiceIds: [...Responses.invoice],
    reminderForDisconnectionId: reminderId,
    requestForDisconnectionIds: [...Responses.requestForDisconnection],
    dataByProfilesCount: Responses.dataByProfiles?.length ?? 0,
  };
}

export function buildDataModelSummaryWithAnchor(
  Responses: baseFixture['Responses'],
  anchor: Pdt2376BillingAnchor,
): Record<string, unknown> {
  return {
    ...buildDataModelSummary(Responses),
    billingAnchor: anchor,
    invoicePaymentTermDays: RFD_MIN_INVOICE_PAYMENT_TERM_DAYS,
  };
}
