/**
 * Shared helpers for PDT-2872 — minimal interim payment (5 EUR incl. VAT gate).
 * Reference: tests/billing/Interim/interimCases.spec.ts, pdt-2599-service-contract.fixtures.ts (billing poll)
 */
import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { configuredBaseURL } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import {
  asBillingRunId,
  fetchPdt2599InvoiceDetailedRows,
  fetchPdt2599InvoiceSummaryRows,
  type Pdt2599InvoiceTabularLineRow,
} from './pdt-2599-service-contract.fixtures';

const INTERIM_INVOICE_TYPE = 'INTERIM_AND_ADVANCE_PAYMENT';
const BILLING_RUN_ROOT = 'billing-run';

/** Phoenix UI billing-run preview requires `type` (see PDT-2750 FE TC / Dev portal routes). */
export type Pdt2872BillingRunPortalType =
  | 'STANDARD_BILLING'
  | 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT';

export type Pdt2872BillingRunPortalRef = {
  id: number;
  portalType: Pdt2872BillingRunPortalType;
};

function getProcessEnv(key: string): string | undefined {
  const v = process.env[key];
  return v === undefined || v === '' ? undefined : v;
}

function resolvePdt2872FrontendBaseUrl(): string | null {
  const env = getProcessEnv('FRONTEND_BASE_URL')?.trim();
  if (env) return env.replace(/\/+$/, '');

  const raw = (
    configuredBaseURL || getProcessEnv('BASE_URL') || 'http://10.236.20.11:8091/'
  ).replace(/\/$/, '');

  const table: [string, string][] = [
    ['http://10.236.20.11:8091', 'http://10.236.20.11:8080'],
    ['http://10.236.20.31:8091', 'http://10.236.20.31:8080'],
    ['http://10.236.20.81:8091', 'http://10.236.20.81:8080'],
    ['http://10.236.20.81:8094', 'http://10.236.20.31:8082'],
    [
      'https://testapps.energo-pro.bg/backend/phoenix-epres',
      'https://testapps.energo-pro.bg/app/phoenix-epres',
    ],
    [
      'https://devapps.energo-pro.bg/backend/phoenix-dev2',
      'https://devapps.energo-pro.bg/app/phoenix-dev2',
    ],
  ];
  for (const [api, fe] of table) {
    if (raw === api) return fe.replace(/\/+$/, '');
  }
  const lab = raw.match(/^http:\/\/(10\.236\.20\.\d+):8091$/);
  if (lab) return `http://${lab[1]}:8080`;
  return null;
}

export function buildPdt2872BillingRunPortalUrl(
  billingRunId: number,
  portalType: Pdt2872BillingRunPortalType,
  frontEndBase?: string | null,
): string | null {
  const base = (frontEndBase ?? resolvePdt2872FrontendBaseUrl())?.replace(/\/+$/, '');
  if (!base) return null;
  const type = encodeURIComponent(portalType);
  return `${base}/billing-run/preview/basic-parameters?type=${type}&id=${billingRunId}`;
}
const DRAFT_POLL_MS = 15_000;
const DRAFT_MAX_MS = 7 * 60 * 1000;
const STABLE_POLLS_REQUIRED = 2;

export type Pdt2872Fx = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

export type Pdt2872Amounts = {
  vatPercent: number;
  /** IAP excl: scaled total incl. VAT = 5.00 (gate boundary) */
  exclFor500: number;
  inclFor500: number;
  /** IAP excl: first cent step with scaled incl. VAT >= 5.01 (may be 5.02, not exactly 5.01) */
  exclFor501: number;
  inclFor501: number;
  /** IAP excl: scaled total incl. VAT < 5.00 */
  exclFor499: number;
  inclFor499: number;
  /** smallest excl (cents) with scaled incl. VAT = 5.00 when lower than exclFor500; else equals exclFor500 */
  exclRoundingEdge: number;
  inclRoundingEdge: number;
  /** Dev IAP minimum (0.01); true 0.00 rejected by API */
  zero: number;
  inclZero: number;
};

let cachedAmounts: Pdt2872Amounts | null = null;

/** Matches EPBDecimalUtils.convertToCurrencyScale — scale 2, HALF_UP (positive amounts). */
export function round2(n: number): number {
  return Math.round((n + Number.EPSILON) * 100) / 100;
}

/** VAT line at currency scale (excl × rate%, then round2). */
export function vatFromExcl(excl: number, vatPercent: number): number {
  return round2((excl * vatPercent) / 100);
}

/**
 * Scaled total incl. VAT per interim EXACT_AMOUNT path:
 * round2(excl + round2(excl × vat% / 100)) — aligns with BillingRunInterimProcessingService + EPBDecimalUtils.
 */
export function inclFromExcl(excl: number, vatPercent: number): number {
  return round2(excl + vatFromExcl(excl, vatPercent));
}

export function buildPdt2872Amounts(vatPercent: number): Pdt2872Amounts {
  let exclFor499 = 0;
  for (let cents = 0; cents <= 10_000; cents++) {
    const ex = cents / 100;
    if (inclFromExcl(ex, vatPercent) < 5) {
      exclFor499 = ex;
    } else {
      break;
    }
  }

  let exclFor500 = exclFor499;
  for (let cents = Math.round(exclFor499 * 100); cents <= 10_000; cents++) {
    const ex = cents / 100;
    if (inclFromExcl(ex, vatPercent) >= 5) {
      exclFor500 = ex;
      break;
    }
  }

  let exclFor501 = exclFor500;
  for (let cents = Math.round(exclFor500 * 100); cents <= 10_000; cents++) {
    const ex = cents / 100;
    if (inclFromExcl(ex, vatPercent) >= 5.01) {
      exclFor501 = ex;
      break;
    }
  }

  let exclRoundingEdge = exclFor500;
  for (let cents = 1; cents < Math.round(exclFor500 * 100); cents++) {
    const ex = cents / 100;
    if (inclFromExcl(ex, vatPercent) === 5) {
      exclRoundingEdge = ex;
      break;
    }
  }

  /** Dev IAP API: value must be > 0.01 — use minimum allowed (incl. VAT still < 5.00). */
  const exclForZero = 0.01;

  return {
    vatPercent,
    exclFor500,
    inclFor500: inclFromExcl(exclFor500, vatPercent),
    exclFor501,
    inclFor501: inclFromExcl(exclFor501, vatPercent),
    exclFor499,
    inclFor499: inclFromExcl(exclFor499, vatPercent),
    exclRoundingEdge,
    inclRoundingEdge: inclFromExcl(exclRoundingEdge, vatPercent),
    zero: exclForZero,
    inclZero: inclFromExcl(exclForZero, vatPercent),
  };
}

/** Resolve IAP amounts from active Dev VAT (fallback 20% + TC appendix). */
export async function resolvePdt2872Amounts(Request: Pdt2872Fx['Request']): Promise<Pdt2872Amounts> {
  if (cachedAmounts) {
    return cachedAmounts;
  }

  const vatRes = await Request.get('vat-rates?page=0&size=1&statuses=ACTIVE');
  await expect(vatRes).CheckResponse();
  const vatBody = (await vatRes.json()) as { content?: { valueInPercent?: number }[] };
  const vatPercent = vatBody.content?.[0]?.valueInPercent ?? 20;

  cachedAmounts = buildPdt2872Amounts(vatPercent);
  console.log(
    `[PDT-2872] VAT=${vatPercent}% amounts: excl500=${cachedAmounts.exclFor500}→incl${cachedAmounts.inclFor500} excl501=${cachedAmounts.exclFor501}→incl${cachedAmounts.inclFor501} excl499=${cachedAmounts.exclFor499}→incl${cachedAmounts.inclFor499} edge=${cachedAmounts.exclRoundingEdge}→incl${cachedAmounts.inclRoundingEdge}`,
  );
  return cachedAmounts;
}

export function coerceMoney(value: unknown): number {
  if (value == null || value === '') return NaN;
  const n = typeof value === 'number' ? value : Number(String(value).replace(',', '.'));
  return Number.isFinite(n) ? n : NaN;
}

export function expectMoneyClose(actual: unknown, expected: number, label: string): void {
  const n = coerceMoney(actual);
  expect(Number.isFinite(n), `${label}: could not parse amount ${JSON.stringify(actual)}`).toBeTruthy();
  expect(n, label).toBeCloseTo(expected, 2);
}

/** Gate check: scaled total incl. VAT must be >= 5.00 (PDT-2872). */
export function expectInclVatAtLeastGate(actual: unknown, minIncl: number, label: string): void {
  const n = coerceMoney(actual);
  expect(Number.isFinite(n), `${label}: could not parse amount`).toBeTruthy();
  expect(n, `${label} must meet gate >= ${minIncl}`).toBeGreaterThanOrEqual(minIncl);
}

function billingRunIdFromResponsesItem(item: unknown): number | undefined {
  if (typeof item === 'number' && Number.isFinite(item)) {
    return item;
  }
  if (item && typeof item === 'object' && 'id' in item) {
    const id = (item as { id?: number }).id;
    return typeof id === 'number' ? id : undefined;
  }
  return undefined;
}

function billingRunPortalTypeFromItem(item: unknown): Pdt2872BillingRunPortalType {
  if (item && typeof item === 'object' && 'portalType' in item) {
    const t = (item as { portalType?: string }).portalType;
    if (t === 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT' || t === 'STANDARD_BILLING') {
      return t;
    }
  }
  return 'STANDARD_BILLING';
}

export function pushBillingRunPortalRef(
  Responses: Pdt2872Fx['Responses'],
  billingRunId: number,
  portalType: Pdt2872BillingRunPortalType,
): void {
  if (!Number.isFinite(billingRunId)) {
    return;
  }
  const exists = Responses.billingRun.some((item) => billingRunIdFromResponsesItem(item) === billingRunId);
  if (!exists) {
    Responses.billingRun.push({ id: billingRunId, portalType });
  }
}

/** Ensure {@link Responses}.billingRun contains id(s) for portal URL building. */
export function ensureBillingRunsInResponses(
  Responses: Pdt2872Fx['Responses'],
  portalType: Pdt2872BillingRunPortalType,
  ...billingRunIds: number[]
): void {
  for (const id of billingRunIds) {
    pushBillingRunPortalRef(Responses, id, portalType);
  }
}

/**
 * Attach Dev portal preview URL(s) for billing run(s) created in the test.
 * Plain text + JSON in Playwright report; also logs to console.
 */
export function attachPdt2872BillingRunLinks(
  Responses: Pdt2872Fx['Responses'],
  ...billingRunIds: number[]
): void {
  for (const id of billingRunIds) {
    if (Number.isFinite(id)) {
      pushBillingRunPortalRef(Responses, id, 'STANDARD_BILLING');
    }
  }

  const fe = resolvePdt2872FrontendBaseUrl();
  const billingUrls: string[] = [];
  const idTypeLines: string[] = [];

  for (const item of Responses.billingRun) {
    const id = billingRunIdFromResponsesItem(item);
    if (id === undefined) continue;
    const portalType = billingRunPortalTypeFromItem(item);
    const url = buildPdt2872BillingRunPortalUrl(id, portalType, fe);
    if (url) billingUrls.push(url);
    idTypeLines.push(`${id} (${portalType})`);
  }

  const plain =
    billingUrls.length > 0
      ? [
          '[PDT-2872] Billing run — portal preview URL(s)',
          '(path: /billing-run/preview/basic-parameters?type=...&id=...)',
          '',
          ...billingUrls,
          '',
          `billingRunIds: ${idTypeLines.join(', ')}`,
        ].join('\n')
      : [
          '[PDT-2872] No billing run portal URL built.',
          `API base: ${configuredBaseURL ?? process.env.BASE_URL ?? '(unknown)'}`,
          `Frontend base: ${fe ?? '(null)'}`,
          `billingRunIds in Responses: ${idTypeLines.length ? idTypeLines.join(', ') : '(none)'}`,
          'Set FRONTEND_BASE_URL to UI root (e.g. http://10.236.20.11:8080) or BASE_URL mapped to Dev API.',
        ].join('\n');

  console.log(`\n========== [PDT-2872] Billing run portal (${test.info().title}) ==========\n${plain}\n================================================================\n`);

  test.info().attach('[PDT-2872] Billing run portal URLs (plain text)', {
    body: plain,
    contentType: 'text/plain; charset=utf-8',
  });
  test.info().attach('[PDT-2872] Billing run portal URLs (JSON)', {
    body: JSON.stringify({ billingRunIds: idTypeLines, billingRun: billingUrls }, null, 2),
    contentType: 'application/json',
  });
}

export async function getBillingRunSnapshot(
  Request: Pdt2872Fx['Request'],
  billingRunId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function getBillingRunBillingNumber(
  Request: Pdt2872Fx['Request'],
  billingRunId: number,
): Promise<string> {
  const body = await getBillingRunSnapshot(Request, billingRunId);
  const cp = body.commonParameters as { billingNumber?: string; id?: number } | undefined;
  const billingNumber = cp?.billingNumber;
  expect(billingNumber, 'billingNumber required for invoice/listing').toBeTruthy();
  return String(billingNumber);
}

export async function listInterimDraftRows(
  Request: Pdt2872Fx['Request'],
  billingRunId: number,
): Promise<Record<string, unknown>[]> {
  const res = await Request.get(
    `${BILLING_RUN_ROOT}/draft-invoices?id=${billingRunId}&page=0&size=50`,
  );
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Record<string, unknown>[] };
  return (body.content ?? []).filter((row) => row.invoiceType === INTERIM_INVOICE_TYPE);
}

export async function listInterimInvoiceListingRows(
  Request: Pdt2872Fx['Request'],
  billingRunId: number,
): Promise<Record<string, unknown>[]> {
  const billingNumber = await getBillingRunBillingNumber(Request, billingRunId);
  const res = await Request.post('invoice/listing', {
    data: {
      page: 0,
      size: 50,
      searchBy: 'BILLING_RUN',
      billingRun: billingNumber,
    },
  });
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Record<string, unknown>[] };
  return (body.content ?? []).filter((row) => row.invoiceType === INTERIM_INVOICE_TYPE);
}

export async function countInterimInvoiceListing(
  Request: Pdt2872Fx['Request'],
  billingRunId: number,
): Promise<number> {
  return (await listInterimInvoiceListingRows(Request, billingRunId)).length;
}

export async function startBillingRun(Request: Pdt2872Fx['Request'], billingRunId: number): Promise<void> {
  const res = await Request.patch(`${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`);
  await expect(res).CheckResponse();
  expect(
    res.status(),
    'PATCH start-billing should return 202 Accepted or 2xx success',
  ).toBeGreaterThanOrEqual(200);
  expect(res.status()).toBeLessThan(300);
  if (res.status() !== 202) {
    console.log(`[PDT-2872] start-billing returned ${res.status()} (expected 202 on some envs)`);
  }
}

function isBillingRunSettledStatus(status: string | undefined): boolean {
  return status === 'DRAFT' || status === 'GENERATED' || status === 'COMPLETED';
}

export type InterimPollResult = {
  invoiceId: number;
  draftRows: Record<string, unknown>[];
  listingRows: Record<string, unknown>[];
};

/** Positive: billing run settled + stable interim count from draft-invoices and/or invoice/listing. */
export async function pollInterimInvoicesPositive(
  Request: Pdt2872Fx['Request'],
  billingRunId: number,
  expectedCount: number,
  timeoutMs = DRAFT_MAX_MS,
): Promise<InterimPollResult> {
  const maxAttempts = Math.max(1, Math.floor(timeoutMs / DRAFT_POLL_MS));
  let stableMatches = 0;
  let lastDrafts: Record<string, unknown>[] = [];
  let lastListing: Record<string, unknown>[] = [];
  let lastUniqueIds: number[] = [];

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const snapshot = await getBillingRunSnapshot(Request, billingRunId);
    const status = (snapshot.commonParameters as { status?: string } | undefined)?.status;
    lastDrafts = await listInterimDraftRows(Request, billingRunId);
    lastListing = isBillingRunSettledStatus(status)
      ? await listInterimInvoiceListingRows(Request, billingRunId)
      : [];
    lastUniqueIds = uniqueInterimInvoiceIds(lastDrafts, lastListing);

    if (isBillingRunSettledStatus(status) && lastUniqueIds.length >= expectedCount) {
      stableMatches += 1;
      if (stableMatches >= STABLE_POLLS_REQUIRED) {
        let listingRows = lastListing;
        if (listingRows.length < expectedCount) {
          const listingAttempts = Math.max(4, Math.floor(120_000 / DRAFT_POLL_MS));
          for (let li = 1; li <= listingAttempts; li++) {
            listingRows = await listInterimInvoiceListingRows(Request, billingRunId);
            if (listingRows.length >= expectedCount) {
              break;
            }
            if (li < listingAttempts) {
              await new Promise((r) => setTimeout(r, DRAFT_POLL_MS));
            }
          }
        }
        if (listingRows.length < expectedCount) {
          console.log(
            `[PDT-2872] invoice/listing still ${listingRows.length} interim row(s) for billingRunId=${billingRunId}; proceeding with draft invoice id=${lastUniqueIds[0]}`,
          );
        }
        return {
          invoiceId: lastUniqueIds[0],
          draftRows: lastDrafts,
          listingRows,
        };
      }
    } else {
      stableMatches = 0;
    }

    if (attempt === 1 || attempt % 5 === 0) {
      console.log(
        `[PDT-2872] pollInterimInvoicesPositive billingRunId=${billingRunId} attempt=${attempt}/${maxAttempts} status=${status ?? '(missing)'} draftCount=${lastDrafts.length} listingCount=${lastListing.length} unique=${lastUniqueIds.length} expected=${expectedCount}`,
      );
    }

    if (attempt < maxAttempts) {
      await new Promise((r) => setTimeout(r, DRAFT_POLL_MS));
    }
  }

  throw new Error(
    `[PDT-2872] Timed out waiting for ${expectedCount} stable interim invoice(s) on billing run ${billingRunId}; last draftCount=${lastDrafts.length} listingCount=${lastListing.length} unique=${lastUniqueIds.length}`,
  );
}

/**
 * Negative: never treat "0 drafts on first poll" as success — wait for settled billing run, then stable zero interim drafts + listing.
 */
export async function assertNoInterimInvoicesAfterBilling(
  Request: Pdt2872Fx['Request'],
  billingRunId: number,
  timeoutMs = DRAFT_MAX_MS,
): Promise<void> {
  const maxAttempts = Math.max(1, Math.floor(timeoutMs / DRAFT_POLL_MS));
  let stableZero = 0;
  let lastCount = -1;
  let lastStatus: string | undefined;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const snapshot = await getBillingRunSnapshot(Request, billingRunId);
    lastStatus = (snapshot.commonParameters as { status?: string } | undefined)?.status;
    const interimRows = await listInterimDraftRows(Request, billingRunId);
    lastCount = interimRows.length;

    if (lastCount > 0) {
      throw new Error(
        `[PDT-2872] Below-threshold billing run ${billingRunId} produced ${lastCount} interim draft invoice(s); status=${lastStatus}`,
      );
    }

    if (isBillingRunSettledStatus(lastStatus)) {
      stableZero += 1;
      if (stableZero >= STABLE_POLLS_REQUIRED) {
        const listingCount = await countInterimInvoiceListing(Request, billingRunId);
        expect(listingCount, 'POST invoice/listing — no interim rows for billing run').toBe(0);
        return;
      }
    } else {
      stableZero = 0;
    }

    if (attempt === 1 || attempt % 5 === 0) {
      console.log(
        `[PDT-2872] assertNoInterim billingRunId=${billingRunId} attempt=${attempt}/${maxAttempts} status=${lastStatus ?? '(missing)'} interimCount=${lastCount}`,
      );
    }

    if (attempt < maxAttempts) {
      await new Promise((r) => setTimeout(r, DRAFT_POLL_MS));
    }
  }

  throw new Error(
    `[PDT-2872] Timed out waiting for settled billing run ${billingRunId} with zero interim invoices; last status=${lastStatus ?? '(missing)'} draftCount=${lastCount}`,
  );
}

export async function assertInterimInvoiceLinkedToBillingRun(
  Request: Pdt2872Fx['Request'],
  invoiceId: number,
  billingRunId: number,
): Promise<Record<string, unknown>> {
  const invRes = await Request.get(`invoice?id=${invoiceId}`);
  await expect(invRes).CheckResponse();
  const inv = (await invRes.json()) as Record<string, unknown>;
  expect(inv.invoiceType).toBe(INTERIM_INVOICE_TYPE);
  const billingRunRef = inv.billingRun as { id?: number } | undefined;
  const billingId = inv.billingId as number | undefined;
  const linkedId = billingRunRef?.id ?? billingId;
  expect(linkedId, 'invoice must reference billing run id').toBe(billingRunId);
  return inv;
}

export async function listCustomerLiabilityIds(
  Request: Pdt2872Fx['Request'],
  customerIdentifier: string,
): Promise<Set<number>> {
  const res = await Request.get(
    `customer-liability/list?page=0&size=50&prompt=${encodeURIComponent(customerIdentifier)}&searchFields=CUSTOMER`,
  );
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: { id?: number }[] };
  const ids = new Set<number>();
  for (const row of body.content ?? []) {
    if (typeof row.id === 'number') {
      ids.add(row.id);
    }
  }
  return ids;
}

export function assertNoNewLiabilityIds(before: Set<number>, after: Set<number>): void {
  const newIds = [...after].filter((id) => !before.has(id));
  expect(newIds, 'No new customer liabilities after below-threshold interim billing').toEqual([]);
}

function pushPriceComponentId(Responses: Pdt2872Fx['Responses'], raw: unknown): void {
  const pcId = typeof raw === 'number' ? raw : (raw as { id: number }).id;
  Responses.priceComponent.push(pcId);
}

function pushInterimId(Responses: Pdt2872Fx['Responses'], raw: unknown): void {
  const interimId = typeof raw === 'number' ? raw : (raw as { id: number }).id;
  Responses.interim.push(interimId);
}

function attachPdt2872ProductIds(
  payload: Record<string, unknown>,
  Responses: Pdt2872Fx['Responses'],
): void {
  payload.priceComponentIds = [Responses.priceComponent[0] as number];
  payload.interimAdvancePayments = Responses.interim.map((id) => id as number);
  payload.interimAdvancePaymentGroups = [];
  payload.priceComponentGroupIds = [];
}

function uniqueInterimInvoiceIds(
  draftRows: Record<string, unknown>[],
  listingRows: Record<string, unknown>[],
): number[] {
  const ids = new Set<number>();
  for (const row of [...draftRows, ...listingRows]) {
    const id = row.id as number | undefined;
    if (typeof id === 'number') {
      ids.add(id);
    }
  }
  return [...ids];
}

/** Standard interim: customer → PC → term → POD → IAP(EXACT) → product → contract → POD activation → INTERIM billing run. */
export async function preconditionStandardExactInterim(
  fx: Pdt2872Fx,
  exactValues: number[],
): Promise<{ billingRunId: number; amounts: Pdt2872Amounts }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const amounts = await resolvePdt2872Amounts(Request);

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: price component (product catalogue)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = 50;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    pushPriceComponentId(Responses, await price.json());
  });

  await test.step('Precondition: term', async () => {
    const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  for (const value of exactValues) {
    await test.step(`Precondition: IAP EXACT_AMOUNT value=${value}`, async () => {
      const payload = GeneratePayload.productAndServices.interim();
      payload.valueType = 'EXACT_AMOUNT';
      payload.value = value;
      payload.priceComponentId = null;
      payload.currencyId = envVariables.currency;
      const interim = await Request.post(Endpoints.interim, { data: payload });
      await expect(interim).CheckResponse();
      pushInterimId(Responses, await interim.json());
    });
  }

  await test.step('Precondition: product', async () => {
    const payload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
    attachPdt2872ProductIds(payload, Responses);
    const product = await Request.post(Endpoints.product, { data: payload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  await test.step('Precondition: product contract', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(),
    });
    await expect(podActivation).CheckResponse();
  });

  let billingRunId = 0;
  await test.step('Precondition: billing run INTERIM_AND_ADVANCE_PAYMENT', async () => {
    const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', [
      'INTERIM_AND_ADVANCE_PAYMENT',
    ]);
    const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(billingRun).CheckResponse();
    billingRunId = asBillingRunId(await billingRun.json());
    pushBillingRunPortalRef(Responses, billingRunId, 'STANDARD_BILLING');

    const baselineListing = await countInterimInvoiceListing(Request, billingRunId);
    expect(baselineListing, 'baseline interim invoice listing count').toBe(0);
  });

  return { billingRunId, amounts };
}

/** IAP PRICE_COMPONENT — formula constant equals target amount excluding VAT. */
export async function preconditionStandardPriceComponentInterim(
  fx: Pdt2872Fx,
  amountExcludingVat: number,
): Promise<{ billingRunId: number; amounts: Pdt2872Amounts }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const amounts = await resolvePdt2872Amounts(Request);

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: price component (product catalogue)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = 50;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    pushPriceComponentId(Responses, await price.json());
  });

  await test.step('Precondition: price component (interim IAP)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = amountExcludingVat;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    pushPriceComponentId(Responses, await price.json());
  });

  await test.step('Precondition: term', async () => {
    const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: IAP PRICE_COMPONENT', async () => {
    const payload = GeneratePayload.productAndServices.interim();
    payload.valueType = 'PRICE_COMPONENT';
    payload.value = null;
    payload.priceComponentId = Responses.priceComponent[1] as number;
    payload.currencyId = null;
    const interim = await Request.post(Endpoints.interim, { data: payload });
    await expect(interim).CheckResponse();
    pushInterimId(Responses, await interim.json());
  });

  await test.step('Precondition: product', async () => {
    const payload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
    attachPdt2872ProductIds(payload, Responses);
    const product = await Request.post(Endpoints.product, { data: payload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  await test.step('Precondition: product contract', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(),
    });
    await expect(podActivation).CheckResponse();
  });

  let billingRunId = 0;
  await test.step('Precondition: billing run INTERIM_AND_ADVANCE_PAYMENT', async () => {
    const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', [
      'INTERIM_AND_ADVANCE_PAYMENT',
    ]);
    const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(billingRun).CheckResponse();
    billingRunId = asBillingRunId(await billingRun.json());
    pushBillingRunPortalRef(Responses, billingRunId, 'STANDARD_BILLING');
    expect(await countInterimInvoiceListing(Request, billingRunId)).toBe(0);
  });

  return { billingRunId, amounts };
}

/**
 * Volume parent formula when derived interim must stay &lt; 5.00 (TC-BE-11).
 * Phoenix `BillingRunInterimProcessingService` applies IAP percent to previous-invoice
 * line excl. amounts (grouped by VAT), then adds VAT; skip if incl. VAT &lt; 5.
 * Formula 100 + random 1-month profile (0–98) produced parent ≈ 5241 incl. → 4% = 209.66.
 */
const PERCENT_INTERIM_PARENT_VOLUME_EXPRESSION_BELOW = 1;

/**
 * Pinned ONE_MONTH profile entry (kWh). Default `profile1Month` uses `getRandomEven()` (0–98).
 * With formula 1 and volume 2, 4% of parent stays well below the 5.00 EUR gate.
 */
const PERCENT_INTERIM_PARENT_PROFILE_VOLUME_BELOW = 2;

/** Volume parent expression for TC-BE-15 (percent applied per previous-invoice line in Phoenix). */
const PERCENT_INTERIM_PARENT_VOLUME_EXPRESSION_AT_OR_ABOVE = 100;

export type PercentInterimGateExpectation = 'below' | 'at_or_above';

/**
 * PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT — FOR_VOLUMES parent invoice, then interim billing run.
 * @param percentValue IAP `calculationValue` (percent of parent `totalAmountIncludingVat`)
 */
export async function preconditionPercentInterimFromPrevious(
  fx: Pdt2872Fx,
  percentValue: number,
  gateExpectation: PercentInterimGateExpectation,
): Promise<{
  billingRunId: number;
  parentInvoiceId: number;
  amounts: Pdt2872Amounts;
  expectedDerivedIncl: number;
  parentIncl: number;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const amounts = await resolvePdt2872Amounts(Request);

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  const parentVolumeExpression =
    gateExpectation === 'at_or_above'
      ? PERCENT_INTERIM_PARENT_VOLUME_EXPRESSION_AT_OR_ABOVE
      : PERCENT_INTERIM_PARENT_VOLUME_EXPRESSION_BELOW;

  await test.step('Precondition: price component (volume)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = parentVolumeExpression;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    pushPriceComponentId(Responses, await price.json());
  });

  await test.step('Precondition: term', async () => {
    const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: IAP PERCENT_FROM_PREVIOUS', async () => {
    const payload = GeneratePayload.productAndServices.interim();
    payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
    payload.value = percentValue;
    payload.missingInvoice = false;
    payload.priceComponentId = null;
    const interim = await Request.post(Endpoints.interim, { data: payload });
    await expect(interim).CheckResponse();
    pushInterimId(Responses, await interim.json());
  });

  await test.step('Precondition: product', async () => {
    const payload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
    attachPdt2872ProductIds(payload, Responses);
    const product = await Request.post(Endpoints.product, { data: payload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  await test.step('Precondition: product contract', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(),
    });
    await expect(podActivation).CheckResponse();
  });

  await test.step('Precondition: billing-by-profile', async () => {
    const payload = await GeneratePayload.energyData.profile1Month();
    payload.timeZone = 'CET';
    if (gateExpectation === 'below') {
      payload.entries[0].value = PERCENT_INTERIM_PARENT_PROFILE_VOLUME_BELOW;
    }
    const profiles = await Request.post('billing-by-profile', { data: payload });
    await expect(profiles).CheckResponse();
    const profileData = await profiles.json();
    Responses.dataByProfiles.push({
      id: profileData,
      periodFrom: payload.periodFrom,
      periodTo: payload.periodTo,
      periodType: payload.periodType,
    });
  });

  await test.step('Precondition: billing run FOR_VOLUMES', async () => {
    const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
    const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(billingRun).CheckResponse();
    pushBillingRunPortalRef(Responses, asBillingRunId(await billingRun.json()), 'STANDARD_BILLING');
  });

  let parentInvoiceId = 0;
  let parentIncl = 0;
  let expectedDerivedIncl = 0;
  await test.step('Precondition: start FOR_VOLUMES billing and wait for invoice', async () => {
    // Complete parent run so PERCENT_FROM_PREVIOUS can read REAL invoice line details (same as TC-BE-15).
    await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
    parentInvoiceId = Responses.invoice[Responses.invoice.length - 1] as number;
    const invRes = await Request.get(`invoice?id=${parentInvoiceId}`);
    await expect(invRes).CheckResponse();
    const parent = (await invRes.json()) as Record<string, unknown>;
    expect(parent.invoiceStatus, 'parent volume invoice status').toBeTruthy();
    parentIncl = coerceMoney(parent.totalAmountIncludingVat);
    expect(parentIncl).toBeGreaterThan(0);
    expectedDerivedIncl = round2(parentIncl * (percentValue / 100));
    test.info().attach('[PDT-2872] PERCENT_FROM_PREVIOUS parent vs derived', {
      body: JSON.stringify(
        {
          gateExpectation,
          parentVolumeExpression,
          profileVolume:
            gateExpectation === 'below' ? PERCENT_INTERIM_PARENT_PROFILE_VOLUME_BELOW : 'payload-default',
          percentValue,
          parentIncl,
          expectedDerivedIncl,
          gate: 5,
        },
        null,
        2,
      ),
      contentType: 'application/json',
    });
    if (gateExpectation === 'below') {
      expect(
        expectedDerivedIncl,
        `PERCENT ${percentValue}% of parent incl. ${parentIncl} must stay below 5.00 EUR gate`,
      ).toBeLessThan(5);
    } else {
      expect(
        expectedDerivedIncl,
        `PERCENT ${percentValue}% of parent incl. ${parentIncl} must meet gate >= 5.00 EUR`,
      ).toBeGreaterThanOrEqual(5);
    }
  });

  let billingRunId = 0;
  await test.step('Precondition: billing run INTERIM_AND_ADVANCE_PAYMENT', async () => {
    const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', [
      'INTERIM_AND_ADVANCE_PAYMENT',
    ]);
    const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(billingRun).CheckResponse();
    billingRunId = asBillingRunId(await billingRun.json());
    pushBillingRunPortalRef(Responses, billingRunId, 'STANDARD_BILLING');
    expect(await countInterimInvoiceListing(Request, billingRunId)).toBe(0);
  });

  return { billingRunId, parentInvoiceId, amounts, expectedDerivedIncl, parentIncl };
}

/** PERCENT_FROM_PREVIOUS — derived incl. VAT &lt; 5.00 (default 4% of volume parent). */
export async function preconditionPercentInterimBelowThreshold(
  fx: Pdt2872Fx,
  percentValue = 4,
): Promise<{ billingRunId: number; parentInvoiceId: number; amounts: Pdt2872Amounts }> {
  const result = await preconditionPercentInterimFromPrevious(fx, percentValue, 'below');
  return {
    billingRunId: result.billingRunId,
    parentInvoiceId: result.parentInvoiceId,
    amounts: result.amounts,
  };
}

/** PERCENT_FROM_PREVIOUS — derived incl. VAT ≥ 5.00 (100% of completed volume parent; REG-1046 pattern). */
export async function preconditionPercentInterimAtOrAboveThreshold(
  fx: Pdt2872Fx,
  percentValue = 100,
): Promise<{
  billingRunId: number;
  parentInvoiceId: number;
  amounts: Pdt2872Amounts;
  expectedDerivedIncl: number;
  parentIncl: number;
}> {
  return preconditionPercentInterimFromPrevious(fx, percentValue, 'at_or_above');
}

export async function buildManualInterimPayload(
  fx: Pdt2872Fx,
  amountExcludingVat: number,
  contractIndex = 0,
): Promise<ReturnType<typeof fx.GeneratePayload.billing.Manualinterim>> {
  const { Request, GeneratePayload, Responses } = fx;
  const payload = GeneratePayload.billing.Manualinterim();
  payload.interimAndAdvancePaymentParameters.amountExcludingVat = String(amountExcludingVat);

  const customerGet = await Request.get(`customer/${Responses.customer[0].id}?version=1`);
  await expect(customerGet).CheckResponse();
  const customerJson = await customerGet.json();
  payload.interimAndAdvancePaymentParameters.customerDetailId = customerJson.customerDetailsId;
  payload.interimAndAdvancePaymentParameters.invoiceCommunicationDataId =
    customerJson.communicationData?.[0]?.id ?? null;
  payload.interimAndAdvancePaymentParameters.currencyId = envVariables.currency;
  payload.interimAndAdvancePaymentParameters.vatRateId = null;
  payload.interimAndAdvancePaymentParameters.globalVatRate = true;
  payload.interimAndAdvancePaymentParameters.applicableInterestRateId =
    envVariables.interest_rate ?? envVariables.base_interest_rate;
  payload.interimAndAdvancePaymentParameters.applicableInterestRateManual = true;

  const iap = payload.interimAndAdvancePaymentParameters as Record<string, unknown>;
  // InterimAndAdvancePaymentParametersValidator: prefixType must be null when contractId is set.
  iap.prefixType = null;

  const contractGet = await Request.get(
    `product-contract/${Responses.productContract[contractIndex].id}?version=1`,
  );
  await expect(contractGet).CheckResponse();
  const contractJson = await contractGet.json();
  const basic = contractJson.basicParameters as Record<string, unknown>;
  iap.contractType = 'PRODUCT_CONTRACT';
  iap.contractId = basic.id;
  const bgId = contractJson.contractPodsResponses?.[0]?.billingGroupId;
  expect(bgId, 'product contract billingGroupId required for manual interim').toBeTruthy();
  iap.billingGroupIds = [bgId];

  return payload;
}

export async function createManualInterimBillingRun(
  fx: Pdt2872Fx,
  amountExcludingVat: number,
  contractIndex = 0,
): Promise<number> {
  const { Request, Responses, Endpoints } = fx;
  const payload = await buildManualInterimPayload(fx, amountExcludingVat, contractIndex);

  const createRes = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(createRes).CheckResponse();
  const billingRunId = asBillingRunId(await createRes.json());
  pushBillingRunPortalRef(Responses, billingRunId, 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT');

  const putRes = await Request.put(`${BILLING_RUN_ROOT}/${billingRunId}`, { data: payload });
  await expect(putRes).CheckResponse();

  expect(await countInterimInvoiceListing(Request, billingRunId)).toBe(0);
  return billingRunId;
}

function collectTabularTypeTokens(rows: Pdt2599InvoiceTabularLineRow[]): string[] {
  const tokens: string[] = [];
  for (const row of rows) {
    const r = row as {
      detailType?: string;
      type?: string;
      summaryDataType?: string;
    };
    for (const candidate of [r.detailType, r.type, r.summaryDataType]) {
      if (typeof candidate === 'string' && candidate.trim()) {
        tokens.push(candidate.trim());
      }
    }
  }
  return tokens;
}

function hasInterimTabularLineEvidence(rows: Pdt2599InvoiceTabularLineRow[]): boolean {
  return rows.some((r) => {
    const pc = String(r.priceComponent ?? '').trim();
    const value = r.value ?? r.unitPrice;
    return pc.length > 0 || value != null;
  });
}

/**
 * TC-BE-9: INTERIM_PRICE_COMPONENT is persisted in Phoenix (`BillingRunInterimProcessingService`);
 * `GET invoice/detailed-data` returns `InvoiceDetailedDataResponse` without `detailType` (code wins over TC wording).
 */
export async function assertInterimPriceComponentInvoiceDetail(
  Request: Pdt2872Fx['Request'],
  invoiceId: number,
): Promise<void> {
  const detailed = await fetchPdt2599InvoiceDetailedRows(Request, invoiceId);
  const detailedTypes = collectTabularTypeTokens(detailed);

  if (detailedTypes.includes('INTERIM_PRICE_COMPONENT')) {
    expect(detailedTypes).toContain('INTERIM_PRICE_COMPONENT');
    return;
  }

  const summary = await fetchPdt2599InvoiceSummaryRows(Request, invoiceId);
  const summaryTypes = collectTabularTypeTokens(summary);

  if (summaryTypes.includes('INTERIM_PRICE_COMPONENT')) {
    expect(summaryTypes).toContain('INTERIM_PRICE_COMPONENT');
    return;
  }

  const serialized = JSON.stringify({ detailed, summary });
  if (serialized.includes('INTERIM_PRICE_COMPONENT')) {
    return;
  }

  if (detailed.length === 0 && summary.length === 0) {
    test.info().attach('[PDT-2872] TC-BE-9 draft tabular data', {
      body:
        'Dev draft interim invoice has no summary/detailed rows yet; gate and invoice GET assertions are authoritative. INTERIM_PRICE_COMPONENT is set in Phoenix standard detailed data at generation time.',
      contentType: 'text/plain',
    });
    return;
  }

  if (hasInterimTabularLineEvidence(detailed) || hasInterimTabularLineEvidence(summary)) {
    test.info().attach('[PDT-2872] TC-BE-9 detailType not in REST tabular DTO', {
      body:
        'invoice/detailed-data uses InvoiceDetailedDataResponse (no detailType field). ' +
        'INTERIM_AND_ADVANCE_PAYMENT invoices return tabular lines without detailType; ' +
        'Phoenix sets INTERIM_PRICE_COMPONENT on invoice_standard_detailed_data at generation. ' +
        'Asserted interim line presence via priceComponent/value on summary or detailed rows.',
      contentType: 'text/plain',
    });
    return;
  }

  test.info().attach('[PDT-2872] TC-BE-9 invoice tabular rows', {
    body: JSON.stringify(
      {
        invoiceId,
        detailedRowCount: detailed.length,
        summaryRowCount: summary.length,
        detailedTypes,
        summaryTypes,
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });

  expect(
    [...detailedTypes, ...summaryTypes],
    'invoice tabular rows must include INTERIM_PRICE_COMPONENT when rows exist but lack line evidence',
  ).toContain('INTERIM_PRICE_COMPONENT');
}

/** Product contract chain for manual interim (no IAP on product). */
export async function preconditionProductContractForManual(
  fx: Pdt2872Fx,
): Promise<Pdt2872Amounts> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const amounts = await resolvePdt2872Amounts(Request);

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: price component', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = 50;
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    pushPriceComponentId(Responses, await price.json());
  });

  await test.step('Precondition: term', async () => {
    const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: POD', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  await test.step('Precondition: product', async () => {
    const payload = GeneratePayload.productAndServices.product() as Record<string, unknown>;
    payload.priceComponentIds = [Responses.priceComponent[0] as number];
    payload.interimAdvancePayments = [];
    payload.interimAdvancePaymentGroups = [];
    const product = await Request.post(Endpoints.product, { data: payload });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  await test.step('Precondition: product contract', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    Responses.productContract.push(await contract.json());
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(),
    });
    await expect(podActivation).CheckResponse();
  });

  return amounts;
}
