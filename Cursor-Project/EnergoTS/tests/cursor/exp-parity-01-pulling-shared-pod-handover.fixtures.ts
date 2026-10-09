/**
 * EXP-PARITY-01 — PreProd vs Experiment parity: volumes pulling on a shared POD
 * with sequential contract handover (+ interim on the pulled contract).
 *
 * NAMING: `EXP-PARITY-01` is a PSEUDO-KEY, not a real Jira issue. No ticket
 * exists for this parity work yet. Rename the files, the `EXP_PARITY_01_KEY`
 * constant and the test titles as soon as a real key is created.
 *
 * WHY THIS EXISTS
 * ---------------
 * Phoenix standard billing was rewritten on the Experiment environment:
 * PreProd still runs the old synchronous engine (`billing_run.generate_run_volume`,
 * a single plpgsql session), Experiment runs the new asynchronous pipeline
 * (`billing_run.standard_billing_preparation` plpython3u +
 * `standard_billing_stage_01..22`, where the parallel worker stages 05/09/12
 * COMMIT independently). The contract-pulling loop moved out of
 * `generate_run_volume` into `standard_billing_stage_03_resolve_data_ownership`.
 *
 * The QA goal is that THE SAME spec runs unchanged against both environments
 * and makes any behavioural difference visible. `playwright.config.ts` takes
 * `baseURL` from `process.env.BASE_URL`, so nothing here hardcodes, detects or
 * branches on environment or URL. The observable difference is emitted as a
 * normalized parity snapshot (see `./shared/exp-parity-snapshot.fixtures`),
 * which is diffed between the two runs.
 *
 * NOT A DUPLICATE OF `tests/billing/pulling/pulling.spec.ts`
 * ---------------------------------------------------------
 * The REG-964/REG-965 family covers the same handover topology but with a
 * ONE_DAY profile, CONTRACT-level runs listing the FIRST contract, and no
 * assertions (it only stashes responses and attaches links). This spec uses a
 * FIFTEEN_MINUTES profile covering the WHOLE previous month, a CUSTOMER-level
 * run, hard assertions on which contract priced the volume, and a second case
 * that adds an interim to the pulled contract.
 *
 * EXPECTED BEHAVIOUR (documented product intent)
 * ----------------------------------------------
 * Confluence "Rules for pulling" (page id 256049175; Phase 2 copy 585730680):
 * "In all other cases invoice in latest contract all billing data from all old
 * contracts". Jira REG-965 confirms the volume is pulled into the SECOND
 * contract and calculated with the second contract's price component, product
 * and term. Contracts are pull candidates only when the handover is adjacent:
 * `deactivation date + 1 day = next activation date` (no gap, no overlap).
 *
 * OPEN PRODUCT QUESTION (Case 11)
 * -------------------------------
 * Confluence "Rules for pulling" only says "Interim deduction/generation
 * should discuss" — there is NO documented expected behaviour for an interim
 * on a pulled contract. See the comment above
 * {@link EXP_PARITY_01_CASE_11_TITLE} usage in the spec: that fact is recorded
 * and snapshotted, never hard-asserted.
 *
 * SWAGGER NOTES (`Cursor-Project/config/swagger/experiment/swagger-spec.json`,
 * refreshed today — Rule 41)
 * -----------------------------------------------------------------------
 * Endpoints and enums used here were validated against the Experiment spec:
 * - `POST /billing-run` → `BillingRunCreateRequest`, returns a bare integer id.
 *   `billingType: 'STANDARD_BILLING'`; `applicationModelType` accepts
 *   `FOR_VOLUMES` and `INTERIM_AND_ADVANCE_PAYMENT`; `billingApplicationLevel`
 *   accepts `CUSTOMER` / `CONTRACT` / `POD`.
 * - `PATCH /billing-run/start-billing?billingRunId=`; `GET /billing-run/{id}`
 *   → `BillingRunResponse.commonParameters.status`, enum
 *   `INITIAL | IN_PROGRESS_DRAFT | DRAFT | IN_PROGRESS_GENERATION | GENERATED |
 *    IN_PROGRESS_ACCOUNTING | COMPLETED | DELETED | PAUSED | CANCELLED |
 *    IN_PROGRESS_TERMINATION`.
 * - `GET /billing-run/draft-invoices?id=&page=&size=` →
 *   `PageBillingRunInvoiceViewResponse`, rows expose `id` and `invoiceType`.
 * - `GET /invoice?id=` → `InvoiceResponse` with `productContract` / `product`
 *   (`ShortResponse`), `invoiceType`, `invoiceStatus`, `totalAmount*` strings.
 * - `GET /billing-by-profile/{id}?periodFrom=&periodTo=` →
 *   `BillingByProfilePreviewResponse.entries[].value`.
 * - `POST /billing-by-profile` → `BillingByProfileCreateRequest`, required
 *   `identifier, periodFrom, periodTo, periodType, profileId`; `periodType`
 *   enum contains `FIFTEEN_MINUTES`.
 *
 * Deviations kept on purpose (framework payload templates outside `tests/`
 * already send these on green specs; Swagger is the authority, so they are
 * reported rather than silently trusted):
 * - `BillingByProfileCreateRequest` has NO `timeZone` property in the
 *   Experiment spec, but `BillingByProfilePreviewResponse.timeZone` exists with
 *   enum `CET | EET` and every existing volumes spec sends `timeZone: 'CET'`.
 * - `BillingRunCommonParameters` has no `filePrintName`; the shared
 *   `forVolumes()` template sends it.
 * - `CreateInterimAdvancePaymentRequest` has no `missingInvoice`; the shared
 *   `interim()` template sends it.
 * - `PodManualActivationRequest` has no `identifier`; `pod_activation()` sends it.
 *
 * DETERMINISM
 * -----------
 * Two framework generators randomize values that would create false parity
 * diffs, so they are pinned here: the settlement-period profile percentage
 * (`randomGens.generatePercentage()`) and the interim exact amount
 * (`randomGens.getRandomEven()`). Generators live outside `tests/` and must
 * not be modified, so the pinning happens on the payload after generation.
 */

import { expect, test } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import { randomGens } from '../../utils/randomGens';
import {
  fetchPdt2599InvoiceDetailedRows,
  fetchPdt2599InvoiceSummaryRows,
  type Pdt2599InvoiceTabularLineRow,
} from './pdt-2599-service-contract.fixtures';
import {
  applyExpParityCombinedProduct,
  postExpParitySignedProductContract,
} from './shared/exp-parity-contract.fixtures';
import { parityAmount, parityRole } from './shared/exp-parity-snapshot.fixtures';
import {
  applyExpParityLocalAddress,
  postExpParityLegalCustomer,
} from './shared/exp-parity-customer.fixtures';

/** Pseudo-key placeholder — replace once a real Jira key exists. */
export const EXP_PARITY_01_KEY = 'EXP-PARITY-01';

export const EXP_PARITY_01_CASE_1_ID = 'case-01-pulling-shared-pod-handover';
export const EXP_PARITY_01_CASE_11_ID = 'case-11-interim-on-pulled-contract';

export const EXP_PARITY_01_CASE_1_TITLE =
  'Volumes pulling on shared POD with sequential handover invoices the latest contract';
export const EXP_PARITY_01_CASE_11_TITLE =
  'Interim on pulled contract when billing run lists only the contract without interim';

/** Price component formula of the OLD contract (A) — must NOT price the volume. */
export const EXP_PARITY_01_PRICE_A = 100;
/** Price component formula of the LATEST contract (B) — must price the volume. */
export const EXP_PARITY_01_PRICE_B = 200;
/** Swagger `InvoiceResponse.invoiceType` for volumes invoices. */
export const EXP_PARITY_01_VOLUMES_INVOICE_TYPE = 'STANDARD';
/** Swagger `InvoiceResponse.invoiceStatus` for draft invoices from a billing run. */
export const EXP_PARITY_01_DRAFT_INVOICE_STATUS = 'DRAFT';
/** Tolerance for comparing unit prices parsed from API strings. */
export const EXP_PARITY_UNIT_PRICE_TOLERANCE = 0.01;
/** Pinned interim amount (generator default is random). */
export const EXP_PARITY_01_INTERIM_AMOUNT = 50;
/** Pinned settlement-period profile share (generator default is random). */
const SETTLEMENT_PROFILE_PERCENTAGE = 100;
/** kWh written into every 15-minute slot of the previous month. */
export const EXP_PARITY_01_KWH_PER_SLOT = 1;
/** Last day the POD belongs to contract A; contract B starts the next day. */
export const EXP_PARITY_01_HANDOVER_DAY = 15;

const BILLING_RUN_ROOT = 'billing-run';
const BILLING_BY_PROFILE_ROOT = 'billing-by-profile';
const POD_MANUAL_ACTIVATION_ROOT = 'contract-pods/manual';

/** Uploaded profile data is materialized by a background job — poll, do not sleep blindly. */
const PROFILE_DATA_POLL_INTERVAL_MS = 20_000;
const PROFILE_DATA_MAX_WAIT_MS = 12 * 60 * 1000;
/**
 * Volumes billing-data preparation runs on a cron with a ~2 minute period.
 * Settle past one full cron cycle after the profile rows are visible, before
 * the billing run is created.
 */
const BILLING_DATA_CRON_SETTLE_MS = 3 * 60 * 1000;
/** The Experiment pipeline is asynchronous and multi-stage — allow a long DRAFT wait. */
const DRAFT_POLL_INTERVAL_MS = 20_000;
const DRAFT_MAX_WAIT_MS = 30 * 60 * 1000;

/** Per-test timeout: preconditions + cron settle + async pipeline. */
export const EXP_PARITY_01_TEST_TIMEOUT_MS = 75 * 60 * 1000;

/** Billing-run statuses that mean "still working". */
const BILLING_RUN_PENDING_STATUSES = new Set(['', 'INITIAL', 'IN_PROGRESS_DRAFT']);
/** Billing-run statuses that mean the run can never reach DRAFT. */
const BILLING_RUN_TERMINAL_FAILURES = new Set(['DELETED', 'CANCELLED', 'IN_PROGRESS_TERMINATION']);

export type ExpParityFx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type ExpParityRequest = ExpParityFx['Request'];

/** Previous-month window with an adjacent handover (no gap, no overlap). */
export type ExpParityMonthWindow = {
  /** First day of the previous month (`yyyy-mm-dd`). */
  start: string;
  /** Last day the POD belongs to contract A. */
  handoverEnd: string;
  /** First day the POD belongs to contract B — always `handoverEnd + 1 day`. */
  handoverNextStart: string;
  /** Last day of the previous month. */
  end: string;
};

export type ExpParityChain = {
  customerId: number;
  customerIdentifier: string;
  podId: number;
  productAId: number;
  productBId: number;
  contractAId: number;
  contractBId: number;
  contractANumber: string;
  contractBNumber: string;
  /** IAP attached to product B only; `null` when the case runs without interim. */
  interimId: number | null;
  billingByProfileId: number;
  window: ExpParityMonthWindow;
  fifteenMinuteSlotCount: number;
  totalKwh: number;
};

/** Comparable facts of one invoice. `invoiceId` is stripped before snapshotting. */
export type ExpParityInvoiceFacts = {
  invoiceId: number;
  contractRole: string;
  productRole: string;
  invoiceType: string;
  invoiceStatus: string;
  invoiceDocumentType: string;
  totalAmountExcludingVat: number | null;
  totalAmountIncludingVat: number | null;
  /** Sorted unique unit prices from `invoice/summary-data` — proves which product priced the volume. */
  unitPrices: number[];
  totalVolumes: number | null;
  summaryRowCount: number;
  /** Distinct `periodFrom`/`periodTo` pairs on `invoice/detailed-data`. */
  settlementPeriodCount: number;
  detailedRowCount: number;
};

/** Snapshot-safe projection: drops the raw invoice id. */
export function parityInvoiceFacts(
  facts: ExpParityInvoiceFacts,
): Omit<ExpParityInvoiceFacts, 'invoiceId'> {
  const { invoiceId: _invoiceId, ...rest } = facts;
  return rest;
}

function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const value = Number((raw as { id?: unknown }).id);
    if (Number.isFinite(value)) {
      return value;
    }
  }
  const coerced = Number(raw);
  if (Number.isFinite(coerced)) {
    return coerced;
  }
  throw new Error(`Unable to read an entity id from ${JSON.stringify(raw)}`);
}

function toNumberOrNull(raw: unknown): number | null {
  return parityAmount(raw, 6);
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function daysBetweenInclusive(fromIsoDate: string, toIsoDate: string): number {
  const from = Date.parse(`${fromIsoDate}T00:00:00Z`);
  const to = Date.parse(`${toIsoDate}T00:00:00Z`);
  return Math.round((to - from) / 86_400_000) + 1;
}

/**
 * Previous-month window with the POD handover on day
 * {@link EXP_PARITY_01_HANDOVER_DAY}. `handoverNextStart` is that day + 1,
 * satisfying the adjacency rule the Experiment pipeline requires to treat the
 * two contracts as a pull chain.
 */
export function expParityPreviousMonthWindow(): ExpParityMonthWindow {
  // `generateMonthHalfDate` hardcodes day 15 outside `tests/` — do not modify it.
  // Both handover dates derive from {@link EXP_PARITY_01_HANDOVER_DAY} instead.
  const handoverEnd = randomGens.generateMonthHalfDatePlusOne(
    'yyyy-mm-dd',
    EXP_PARITY_01_HANDOVER_DAY,
    -1,
  );
  const handoverNextStart = randomGens.generateMonthHalfDatePlusOne(
    'yyyy-mm-dd',
    EXP_PARITY_01_HANDOVER_DAY + 1,
    -1,
  );
  return {
    start: randomGens.generateMonthStartDate('yyyy-mm-dd', -1),
    handoverEnd,
    handoverNextStart,
    end: randomGens.generateMonthEndDate('yyyy-mm-dd', -1),
  };
}

/** True when two unit prices match within {@link EXP_PARITY_UNIT_PRICE_TOLERANCE}. */
export function parityUnitPriceMatches(
  actual: number,
  expected: number,
  tolerance = EXP_PARITY_UNIT_PRICE_TOLERANCE,
): boolean {
  return Math.abs(actual - expected) < tolerance;
}

/** True when at least one observed unit price matches `expected` within tolerance. */
export function parityUnitPricesInclude(
  unitPrices: number[],
  expected: number,
  tolerance = EXP_PARITY_UNIT_PRICE_TOLERANCE,
): boolean {
  return unitPrices.some((price) => parityUnitPriceMatches(price, expected, tolerance));
}

/** True when no observed unit price matches `excluded` within tolerance. */
export function parityUnitPricesExclude(
  unitPrices: number[],
  excluded: number,
  tolerance = EXP_PARITY_UNIT_PRICE_TOLERANCE,
): boolean {
  return !unitPrices.some((price) => parityUnitPriceMatches(price, excluded, tolerance));
}

/**
 * Full precondition chain shared by both cases: one legal customer, two
 * products with different price components and different terms, one shared
 * POD handed over between two contracts, and a 15-minute profile covering the
 * whole previous month.
 *
 * Each case calls this itself — no `beforeAll` (Rule 40), and Case 11 never
 * depends on Case 1 having run.
 */
export async function buildExpParitySharedPodHandoverChain(
  fx: ExpParityFx,
  options: { withInterimOnContractB: boolean },
): Promise<ExpParityChain> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const window = expParityPreviousMonthWindow();

  let customerId = 0;
  let customerIdentifier = '';
  const customer = await test.step('Precondition: legal customer', async () => {
    const created = await postExpParityLegalCustomer(fx);
    customerId = created.id;
    customerIdentifier = created.identifier;
    expect(customerIdentifier, 'customer identifier is required for a CUSTOMER-level run').toBeTruthy();
    return created;
  });

  await test.step(
    `Precondition: price component A (expression ${EXP_PARITY_01_PRICE_A})`,
    async () => {
      const payload = GeneratePayload.productAndServices.priceSettlement();
      payload.formulaRequest.expression = String(EXP_PARITY_01_PRICE_A);
      payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage =
        SETTLEMENT_PROFILE_PERCENTAGE;
      const res = await Request.post(Endpoints.priceComponent, { data: payload });
      await expect(res).CheckResponse();
      Responses.priceComponent.push(await res.json());
    },
  );

  await test.step(
    `Precondition: price component B (expression ${EXP_PARITY_01_PRICE_B})`,
    async () => {
      const payload = GeneratePayload.productAndServices.priceSettlement();
      payload.formulaRequest.expression = String(EXP_PARITY_01_PRICE_B);
      payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage =
        SETTLEMENT_PROFILE_PERCENTAGE;
      const res = await Request.post(Endpoints.priceComponent, { data: payload });
      await expect(res).CheckResponse();
      Responses.priceComponent.push(await res.json());
    },
  );

  await test.step('Precondition: two distinct terms (one per product)', async () => {
    for (let i = 0; i < 2; i++) {
      const res = await Request.post(Endpoints.terms, {
        data: GeneratePayload.productAndServices.term(),
      });
      await expect(res).CheckResponse();
      Responses.terms.push(await res.json());
    }
  });

  let podId = 0;
  await test.step('Precondition: one settlement POD shared by both contracts', async () => {
    const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
    applyExpParityLocalAddress(podPayload, customer.addressIds);
    const res = await Request.post(Endpoints.pod, { data: podPayload });
    await expect(res).CheckResponse();
    const body = (await res.json()) as { id?: number };
    Responses.pod.push(body);
    podId = asEntityId(body);
  });

  let interimId: number | null = null;
  if (options.withInterimOnContractB) {
    await test.step(
      `Precondition: interim EXACT_AMOUNT ${EXP_PARITY_01_INTERIM_AMOUNT} (contract B only)`,
      async () => {
        const payload = GeneratePayload.productAndServices.interim();
        // Pinned: the template uses randomGens.getRandomEven(), which would
        // make the two parity runs differ for a reason unrelated to billing.
        payload.value = EXP_PARITY_01_INTERIM_AMOUNT;
        const res = await Request.post(Endpoints.interim, { data: payload });
        await expect(res).CheckResponse();
        const raw = await res.json();
        Responses.interim.push(raw);
        interimId = asEntityId(raw);
      },
    );
  }

  let productAId = 0;
  await test.step('Precondition: product A (price component A, term 0, no interim)', async () => {
    const payload = applyExpParityCombinedProduct(GeneratePayload.productAndServices.product(0));
    // product() harvests every price component and interim already present in
    // Responses — pin both explicitly so A stays the cheap, interim-free product.
    payload.priceComponentIds = [Responses.priceComponent[0]];
    payload.priceComponentGroupIds = [];
    payload.interimAdvancePayments = [];
    payload.interimAdvancePaymentGroups = [];
    const res = await Request.post(Endpoints.product, { data: payload });
    await expect(res).CheckResponse();
    const raw = await res.json();
    Responses.product.push(raw);
    productAId = asEntityId(raw);
  });

  let productBId = 0;
  await test.step(
    `Precondition: product B (price component B, term 1${
      options.withInterimOnContractB ? ', interim attached' : ''
    })`,
    async () => {
      const payload = applyExpParityCombinedProduct(GeneratePayload.productAndServices.product(1));
      payload.priceComponentIds = [Responses.priceComponent[1]];
      payload.priceComponentGroupIds = [];
      payload.interimAdvancePayments = interimId === null ? [] : [interimId];
      payload.interimAdvancePaymentGroups = [];
      const res = await Request.post(Endpoints.product, { data: payload });
      await expect(res).CheckResponse();
      const raw = await res.json();
      Responses.product.push(raw);
      productBId = asEntityId(raw);
    },
  );

  let contractAId = 0;
  await test.step('Precondition: contract A (older contract, product A)', async () => {
    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    // product_contract() copies every interim in Responses onto the contract.
    // Contract A must have none — its product does not carry the interim.
    payload.productParameters.interimAdvancePayments = [];
    const raw = await postExpParitySignedProductContract(Request, payload);
    Responses.productContract.push(raw);
    contractAId = asEntityId(raw);
  });

  let contractBId = 0;
  await test.step('Precondition: contract B (latest contract, product B)', async () => {
    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
    if (interimId === null) {
      payload.productParameters.interimAdvancePayments = [];
    }
    const raw = await postExpParitySignedProductContract(Request, payload);
    Responses.productContract.push(raw);
    contractBId = asEntityId(raw);
  });

  await test.step(
    `Precondition: shared POD handover — A ${window.start}..${window.handoverEnd}, ` +
      `B ${window.handoverNextStart}..${window.end} (adjacent, no gap/overlap)`,
    async () => {
      const activationA = await GeneratePayload.pointsOfDelivery.pod_activation(
        0,
        window.start,
        window.handoverEnd,
        0,
      );
      const resA = await Request.post(POD_MANUAL_ACTIVATION_ROOT, { data: activationA });
      await expect(resA).CheckResponse();

      const activationB = await GeneratePayload.pointsOfDelivery.pod_activation(
        0,
        window.handoverNextStart,
        window.end,
        1,
      );
      const resB = await Request.post(POD_MANUAL_ACTIVATION_ROOT, { data: activationB });
      await expect(resB).CheckResponse();
    },
  );

  const contractANumber = await readContractNumber(Request, contractAId);
  const contractBNumber = await readContractNumber(Request, contractBId);

  const profile = await postWholeMonthFifteenMinuteProfile(fx, window);

  return {
    customerId,
    customerIdentifier,
    podId,
    productAId,
    productBId,
    contractAId,
    contractBId,
    contractANumber,
    contractBNumber,
    interimId,
    billingByProfileId: profile.billingByProfileId,
    window,
    fifteenMinuteSlotCount: profile.slotCount,
    totalKwh: profile.totalKwh,
  };
}

/** `GET product-contract/{id}?version=1` → `basicParameters.contractNumber`. */
export async function readContractNumber(
  Request: ExpParityRequest,
  contractId: number,
): Promise<string> {
  const res = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    basicParameters?: { contractNumber?: string | number | null };
  };
  const number = body.basicParameters?.contractNumber;
  expect(number, `contract ${contractId} must expose a contract number`).toBeTruthy();
  return String(number);
}

/**
 * FIFTEEN_MINUTES profile covering the WHOLE previous month (day 1 00:00 to
 * the last day 23:45). `profile15minute` writes one Excel row per 15-minute
 * slot with `amount` kWh, then `billing-by-profile/import` ingests the file.
 *
 * The framework has no "whole month 15-minute" generator, but
 * `GeneratePayload.energyData.profile15minute(podIndex, dateRanges)` already
 * accepts an arbitrary range, so no generator change is needed — the month
 * window is simply passed as the single range.
 */
export async function postWholeMonthFifteenMinuteProfile(
  fx: ExpParityFx,
  window: ExpParityMonthWindow,
): Promise<{ billingByProfileId: number; slotCount: number; totalKwh: number }> {
  const { Request, GeneratePayload, Responses } = fx;
  const periodFrom = `${window.start}T00:00:00.000Z`;
  const periodTo = `${window.end}T23:45:00.000Z`;
  const slotCount = daysBetweenInclusive(window.start, window.end) * 96;
  const totalKwh = slotCount * EXP_PARITY_01_KWH_PER_SLOT;

  let billingByProfileId = 0;
  await test.step(
    `Precondition: FIFTEEN_MINUTES profile for the whole previous month ` +
      `(${window.start}..${window.end}, ${slotCount} slots × ${EXP_PARITY_01_KWH_PER_SLOT} kWh)`,
    async () => {
      const payload = (await GeneratePayload.energyData.profile15minute(0, [
        {
          startDate: `${window.start}T00:00:00`,
          endDate: `${window.end}T23:45:00`,
          amount: EXP_PARITY_01_KWH_PER_SLOT,
        },
      ])) as Record<string, unknown>;
      payload.periodType = 'FIFTEEN_MINUTES';
      payload.periodFrom = periodFrom;
      payload.periodTo = periodTo;
      payload.warningAcceptedByUser = true;
      // Not in BillingByProfileCreateRequest, but every green volumes spec
      // sends it and BillingByProfilePreviewResponse.timeZone exists (CET|EET).
      payload.timeZone = 'CET';

      const res = await Request.post(BILLING_BY_PROFILE_ROOT, { data: payload });
      await expect(res).CheckResponse();
      const raw = await res.json();
      billingByProfileId = asEntityId(raw);
      Responses.dataByProfiles.push({
        id: billingByProfileId,
        periodFrom,
        periodTo,
        periodType: 'FIFTEEN_MINUTES',
      });

      await GeneratePayload.energyData.uploadDataByProfileFile(billingByProfileId, 'MIN');
    },
  );

  await test.step('Precondition: wait until the uploaded profile rows are readable', async () => {
    await waitForProfileEntries(Request, billingByProfileId, periodFrom, periodTo);
  });

  return { billingByProfileId, slotCount, totalKwh };
}

/**
 * Polls `GET billing-by-profile/{id}` until the imported entries are visible.
 * Import is processed in the background, so starting the billing run too early
 * would bill an empty period and produce a false parity difference.
 */
export async function waitForProfileEntries(
  Request: ExpParityRequest,
  billingByProfileId: number,
  periodFrom: string,
  periodTo: string,
): Promise<number> {
  const deadline = Date.now() + PROFILE_DATA_MAX_WAIT_MS;
  let lastCount = 0;

  while (Date.now() < deadline) {
    const res = await Request.get(
      `${BILLING_BY_PROFILE_ROOT}/${billingByProfileId}` +
        `?periodFrom=${encodeURIComponent(periodFrom)}&periodTo=${encodeURIComponent(periodTo)}`,
    );
    if (res.ok()) {
      const body = (await res.json()) as { entries?: Array<{ value?: number }> };
      lastCount = body.entries?.length ?? 0;
      if (lastCount > 0) {
        console.log(
          `[${EXP_PARITY_01_KEY}] billing-by-profile ${billingByProfileId}: ${lastCount} entries readable`,
        );
        return lastCount;
      }
    }
    await sleep(PROFILE_DATA_POLL_INTERVAL_MS);
  }

  throw new Error(
    `Profile ${billingByProfileId} still has ${lastCount} entries after ` +
      `${Math.round(PROFILE_DATA_MAX_WAIT_MS / 1000)}s — billing data was never prepared.`,
  );
}

/**
 * Waits out one full volumes billing-data preparation cron cycle before the
 * billing run is created. The cron period is the same on both environments,
 * so this does not make the spec environment-aware.
 */
export async function settleBillingDataPreparationCron(): Promise<void> {
  await test.step(
    `Precondition: settle ${Math.round(BILLING_DATA_CRON_SETTLE_MS / 1000)}s for volumes ` +
      `billing-data preparation (cron period ~2 min)`,
    async () => {
      await sleep(BILLING_DATA_CRON_SETTLE_MS);
    },
  );
}

export type ExpParityBillingRunResult = {
  billingRunId: number;
  billingRunStatus: string;
  draftInvoiceIds: number[];
};

/** Creates a CUSTOMER-level `STANDARD_BILLING` / `FOR_VOLUMES` run for the chain's customer. */
export async function createCustomerLevelVolumesRun(fx: ExpParityFx): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = await GeneratePayload.billing.billingRun('CUSTOMER', ['FOR_VOLUMES'], 0);
  const res = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const billingRunId = asEntityId(await res.json());
  Responses.billingRun.push(billingRunId);
  return billingRunId;
}

/**
 * Creates a CONTRACT-level `STANDARD_BILLING` run for
 * `FOR_VOLUMES + INTERIM_AND_ADVANCE_PAYMENT`, listing ONLY contract A.
 *
 * `BillingRunStandardPreparationStateHandler`
 * (`phoenix-core-lib/.../billingRun/service/BillingRunStandardPreparationStateHandler.java:235-246`)
 * classifies this combination as `STANDARD_WITH_INTERIM`.
 */
export async function createContractLevelVolumesPlusInterimRun(
  fx: ExpParityFx,
  contractIndex: number,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const payload = await GeneratePayload.billing.billingRun(
    'CONTRACT',
    ['FOR_VOLUMES', 'INTERIM_AND_ADVANCE_PAYMENT'],
    0,
    contractIndex,
  );
  const res = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const billingRunId = asEntityId(await res.json());
  Responses.billingRun.push(billingRunId);
  return billingRunId;
}

/**
 * `PATCH billing-run/start-billing` then poll `GET billing-run/{id}` until
 * `commonParameters.status === 'DRAFT'`, and read the draft invoices.
 *
 * On Experiment this covers the whole asynchronous `standard_billing_stage_*`
 * pipeline; on PreProd the synchronous `generate_run_volume` call returns much
 * sooner. Same code path, same waiting contract.
 */
export async function startBillingAndReadDrafts(
  Request: ExpParityRequest,
  billingRunId: number,
): Promise<ExpParityBillingRunResult> {
  let billingRunStatus = '';

  await test.step(`Action: start billing run ${billingRunId} and wait for DRAFT`, async () => {
    const startRes = await Request.patch(
      `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
    );
    await expect(startRes).CheckResponse();

    const deadline = Date.now() + DRAFT_MAX_WAIT_MS;
    while (Date.now() < deadline) {
      const statusRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
      await expect(statusRes).CheckResponse();
      const body = (await statusRes.json()) as {
        commonParameters?: { status?: string };
      };
      billingRunStatus = String(body.commonParameters?.status ?? '');

      if (billingRunStatus === 'DRAFT') {
        return;
      }
      if (BILLING_RUN_TERMINAL_FAILURES.has(billingRunStatus)) {
        throw new Error(
          `Billing run ${billingRunId} reached terminal status ${billingRunStatus} instead of DRAFT.`,
        );
      }
      if (!BILLING_RUN_PENDING_STATUSES.has(billingRunStatus)) {
        console.log(
          `[${EXP_PARITY_01_KEY}] billing run ${billingRunId}: unexpected status ${billingRunStatus}, still waiting`,
        );
      }
      await sleep(DRAFT_POLL_INTERVAL_MS);
    }

    throw new Error(
      `Billing run ${billingRunId} stayed in ${billingRunStatus || 'unknown'} for ` +
        `${Math.round(DRAFT_MAX_WAIT_MS / 1000)}s — never reached DRAFT.`,
    );
  });

  const draftInvoiceIds = await readDraftInvoiceIds(Request, billingRunId);
  return { billingRunId, billingRunStatus, draftInvoiceIds };
}

/** Paginates `GET billing-run/draft-invoices?id=` and returns the invoice ids. */
export async function readDraftInvoiceIds(
  Request: ExpParityRequest,
  billingRunId: number,
): Promise<number[]> {
  const size = 25;
  const ids: number[] = [];
  for (let page = 0; page < 20; page++) {
    const res = await Request.get(
      `${BILLING_RUN_ROOT}/draft-invoices?id=${billingRunId}&page=${page}&size=${size}`,
    );
    await expect(res).CheckResponse();
    const body = (await res.json()) as {
      content?: Array<{ id?: number }>;
      last?: boolean;
    };
    const chunk = body.content ?? [];
    for (const row of chunk) {
      const id = Number(row.id);
      if (Number.isFinite(id) && id > 0) {
        ids.push(id);
      }
    }
    if (body.last === true || chunk.length < size) {
      break;
    }
  }
  return ids;
}

function distinctSettlementPeriodCount(rows: Pdt2599InvoiceTabularLineRow[]): number {
  const seen = new Set<string>();
  for (const row of rows) {
    const from = row.periodFrom;
    const to = row.periodTo;
    if (from == null && to == null) {
      continue;
    }
    seen.add(`${String(from ?? '')}|${String(to ?? '')}`);
  }
  return seen.size;
}

/**
 * Reads the comparable facts of one invoice: which contract and product own
 * it, its type/status, amounts, unit prices and settlement-period count.
 *
 * Raw ids are translated to the caller's logical role labels so both parity
 * runs produce identical snapshot values.
 */
export async function loadExpParityInvoiceFacts(
  Request: ExpParityRequest,
  invoiceId: number,
  contractRoleById: Record<string, number>,
  productRoleById: Record<string, number>,
): Promise<ExpParityInvoiceFacts> {
  const res = await Request.get(`invoice?id=${invoiceId}`);
  await expect(res).CheckResponse();
  const invoice = (await res.json()) as {
    invoiceType?: string;
    invoiceStatus?: string;
    invoiceDocumentType?: string;
    totalAmountExcludingVat?: string;
    totalAmountIncludingVat?: string;
    productContract?: { id?: number } | null;
    product?: { id?: number } | null;
  };

  const summaryRows = await fetchPdt2599InvoiceSummaryRows(Request, invoiceId);
  const detailedRows = await fetchPdt2599InvoiceDetailedRows(Request, invoiceId);

  const unitPrices = summaryRows
    .map((row) => toNumberOrNull(row.unitPrice))
    .filter((value): value is number => value !== null)
    .filter((value, index, all) => all.indexOf(value) === index)
    .sort((a, b) => a - b);

  const volumes = summaryRows
    .map((row) => toNumberOrNull(row.totalVolumes))
    .filter((value): value is number => value !== null);

  return {
    invoiceId,
    contractRole: parityRole(contractRoleById, invoice.productContract?.id),
    productRole: parityRole(productRoleById, invoice.product?.id),
    invoiceType: String(invoice.invoiceType ?? 'unknown'),
    invoiceStatus: String(invoice.invoiceStatus ?? 'unknown'),
    invoiceDocumentType: String(invoice.invoiceDocumentType ?? 'unknown'),
    totalAmountExcludingVat: parityAmount(invoice.totalAmountExcludingVat),
    totalAmountIncludingVat: parityAmount(invoice.totalAmountIncludingVat),
    unitPrices,
    totalVolumes: volumes.length ? Number(volumes.reduce((a, b) => a + b, 0).toFixed(6)) : null,
    summaryRowCount: summaryRows.length,
    settlementPeriodCount: distinctSettlementPeriodCount(detailedRows),
    detailedRowCount: detailedRows.length,
  };
}

/** Loads facts for every draft invoice of a run, ordered deterministically by contract role. */
export async function loadAllExpParityInvoiceFacts(
  Request: ExpParityRequest,
  invoiceIds: number[],
  contractRoleById: Record<string, number>,
  productRoleById: Record<string, number>,
): Promise<ExpParityInvoiceFacts[]> {
  const facts: ExpParityInvoiceFacts[] = [];
  for (const invoiceId of invoiceIds) {
    facts.push(
      await loadExpParityInvoiceFacts(Request, invoiceId, contractRoleById, productRoleById),
    );
  }
  return facts.sort((a, b) =>
    `${a.contractRole}|${a.invoiceType}`.localeCompare(`${b.contractRole}|${b.invoiceType}`),
  );
}

/** Logical role maps — keep raw ids out of the parity snapshot. */
export function expParityContractRoles(chain: ExpParityChain): Record<string, number> {
  return { contractA: chain.contractAId, contractB: chain.contractBId };
}

export function expParityProductRoles(chain: ExpParityChain): Record<string, number> {
  return { productA: chain.productAId, productB: chain.productBId };
}
