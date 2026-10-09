/**
 * EXP-PARITY-05 — PreProd vs Experiment parity: rounding of data-by-profile
 * volumes must not drift when the billed month is computed as several periods.
 *
 * NAMING: `EXP-PARITY-05` is a PSEUDO-KEY, not a real Jira issue. No ticket
 * exists for this parity work yet. Rename the files, the `EXP_PARITY_05_KEY`
 * constant and the test title as soon as a real key is created.
 *
 * WHY THIS EXISTS
 * ---------------
 * Phoenix standard billing was rewritten on the Experiment environment.
 * PreProd still runs the old synchronous engine (`billing_run.generate_run_volume`,
 * a single plpgsql session); Experiment runs the new asynchronous pipeline
 * (`billing_run.standard_billing_preparation` + `standard_billing_stage_01..22`).
 * Stages 05 (SLP profile generation), 09 (price component conditions) and 12
 * (settlement prices) run as PARALLEL WORKERS, each on its own connection with
 * its own COMMIT.
 *
 * The QA concern automated here: with a NON-MONTHLY profile the volume is
 * computed per period/interval. If parallel workers each round their own slice
 * independently, the rounded slices can sum to a different total than rounding
 * once at the end. The old single-session engine cannot produce that drift. A
 * `ONE_MONTH` profile is uninteresting — there is only one period — so this
 * case deliberately uses `FIFTEEN_MINUTES`.
 *
 * The same spec must run unchanged on both environments via `process.env.BASE_URL`
 * (`playwright.config.ts` reads `baseURL` from it). Nothing here inspects,
 * hardcodes or branches on environment, hostname or env var — the difference is
 * expected to surface in the DATA, emitted as a normalized parity snapshot
 * (`./shared/exp-parity-snapshot.fixtures`) that is diffed between the two runs.
 *
 * NOT A DUPLICATE OF EXISTING COVERAGE
 * ------------------------------------
 * - `tests/billing/forVolumes/SLP.spec.ts` (REG-1008 / REG-1030) builds a
 *   15-minute PRICE PARAMETER chain on an SLP POD with meters and scale codes,
 *   and only checks that profile rows exist and the invoice tabs render. It
 *   never reads invoice amounts and never reasons about rounding.
 * - `tests/billing/forVolumes/forVolumes.spec.ts` (REG-962) creates one POD per
 *   profile granularity (15 min / 1 h / 1 day / 1 month) to prove all four types
 *   bill at all; it asserts nothing beyond `invoice.length > 0`.
 * This spec reuses their chain shape (settlement POD → settlement price
 * component → product → contract → POD activation → `billing-by-profile` +
 * `MIN` Excel upload → `STANDARD_BILLING` / `FOR_VOLUMES` run) but adds the
 * thing neither has: several computed periods inside ONE month on ONE contract,
 * and hard arithmetic on the invoice total.
 * A dedicated `billingRounding.spec.ts` exists on the EnergoTS `staging` branch
 * but NOT on `cursor`; a repository-wide search of `tests/` on `cursor` found no
 * rounding coverage, so there is nothing to narrow against here.
 *
 * SHARED PLUMBING
 * ---------------
 * The environment-agnostic waiting/polling contract is imported from
 * `./exp-parity-01-pulling-shared-pod-handover.fixtures` on purpose: every
 * parity spec must wait for the billing pipeline in exactly the same way, or a
 * timing difference would masquerade as a parity difference. Console lines
 * emitted by those helpers therefore carry the `EXP-PARITY-01` key. If that file
 * is renamed when a real Jira key appears, update the import here too.
 *
 * SWAGGER NOTES (`Cursor-Project/config/swagger/experiment/swagger-spec.json`,
 * refreshed today — Rule 41)
 * -----------------------------------------------------------------------
 * Verified against the Experiment spec:
 * - `POST /billing-run` → `BillingRunCreateRequest` (`billingType` enum contains
 *   `STANDARD_BILLING`) → `StandardBillingParameters`: `applicationModelType`
 *   enum contains `FOR_VOLUMES`, `billingApplicationLevel` enum contains
 *   `CONTRACT`, `billingCriteria` enum contains
 *   `LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS`. Returns a bare integer id.
 * - `PATCH /billing-run/start-billing?billingRunId=`; `GET /billing-run/{id}`
 *   → `commonParameters.status` enum
 *   `INITIAL | IN_PROGRESS_DRAFT | DRAFT | IN_PROGRESS_GENERATION | GENERATED |
 *    IN_PROGRESS_ACCOUNTING | COMPLETED | DELETED | PAUSED | CANCELLED |
 *    IN_PROGRESS_TERMINATION`.
 * - `GET /billing-run/draft-invoices?id=` → rows expose `id`.
 * - `GET /invoice?id=` → `InvoiceResponse`: `invoiceType`, `invoiceStatus`,
 *   `invoiceDocumentType`, `productContract` (`ShortResponse`) and the amount
 *   fields `totalAmountExcludingVat` / `totalAmountIncludingVat`, all three of
 *   which are declared as STRING — hence `parityAmount` everywhere.
 * - `POST /countries` … `POST /streets` (see `./shared/exp-parity-customer.fixtures.ts`)
 *   builds a consistent local address tree before `POST /customer` and `POST /pod`.
 * - `GET /invoice/detailed-data?id=` → `InvoiceDetailedDataResponse` exposes
 *   `periodFrom` / `periodTo` (`date`), `unitPrice` (number), `value` (number)
 *   and `totalVolumes` (number). This is the per-period evidence the case needs.
 * - `GET /invoice/summary-data?id=` → `InvoiceSummaryDataResponse` exposes
 *   `totalVolumes` (number) but `unitPrice` / `value` as STRING.
 * - `POST /billing-by-profile` → `BillingByProfileCreateRequest`, required
 *   `identifier, periodFrom, periodTo, periodType, profileId`; `periodType`
 *   enum is `FIFTEEN_MINUTES | ONE_HOUR | ONE_DAY | ONE_MONTH`.
 *   `periodFrom` must be the first day of the month and `periodTo` the last
 *   (`BillingByProfileCreateRequest.isPeriodFromValid` /
 *   `isPeriodToValid`). Mid-month windows 400.
 * - The four computed periods are four `BY_SETTLEMENT_PERIODS` price components
 *   that differ only in `dateOfMonths` (days 1–5 / 6–10 / 11–15 / 16–20), all
 *   `issuedSeparateInvoice: INVOICE_ONE`, on one month-bounded profile.
 * - `POST /price-components` → `PriceComponentRequest` →
 *   `ApplicationModelRequest.settlementPeriodsRequest` is
 *   `VolumesBySettlementPeriodsRequest` (required `periodType`, `profiles`;
 *   `periodType` enum `DAY_OF_WEEK_AND_PERIOD_OF_YEAR | DAY_OF_MONTH |
 *   RRULE_FORMULA`), with `settlementPeriods[].minuteRange` enum
 *   `ZERO_FIFTEEN | SIXTEEN_THIRTY | THIRTYONE_FORTYFIVE | FORTYSIX_SIXTY`.
 *
 * SWAGGER-FORCED DEVIATION FROM THE SIBLING SPECS
 * -----------------------------------------------
 * `PriceComponentFormulaRequest.expression` is declared `type: string` and is
 * the only REQUIRED property of that schema. REG-962 / REG-963 / EXP-PARITY-01
 * all assign a JS number (`expression = 100`). Swagger wins here, so
 * {@link EXP_PARITY_05_UNIT_PRICE} is sent as a STRING via
 * {@link EXP_PARITY_05_UNIT_PRICE_EXPRESSION}. Backends that already accept the
 * numeric form also accept the string form (Jackson coerces), so this is
 * strictly the safer of the two.
 *
 * Deviations kept on purpose (payload templates live outside `tests/` and must
 * not be modified; they are already green on existing specs):
 * - `BillingByProfileCreateRequest` has NO `timeZone` property, but every green
 *   volumes spec sends `timeZone: 'CET'` and
 *   `BillingByProfilePreviewResponse.timeZone` exists with enum `CET | EET`.
 * - `BillingRunCommonParameters` has no `filePrintName`; the shared
 *   `forVolumes()` template sends it.
 * - `PodManualActivationRequest` has no `identifier`; `pod_activation()` sends it.
 * - `GET /invoice/detailed-data`, `/invoice/summary-data` and
 *   `/billing-run/draft-invoices` document a required `request` query OBJECT
 *   (`Pageable`-style); every existing EnergoTS spec flattens it to
 *   `page` / `size`. Same flattening is used here.
 *
 * DETERMINISM
 * -----------
 * `PriceSettlement()` randomizes the settlement-period profile share
 * (`randomGens.generatePercentage()`) and the formula expression
 * (`randomGens.getRandomEven()`). Both are pinned after generation — a random
 * value would create a parity diff for a reason unrelated to billing.
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
  readContractNumber,
  settleBillingDataPreparationCron,
  startBillingAndReadDrafts,
  waitForProfileEntries,
  type ExpParityBillingRunResult,
} from './exp-parity-01-pulling-shared-pod-handover.fixtures';
import {
  applyExpParityCombinedProduct,
  postExpParitySignedProductContract,
} from './shared/exp-parity-contract.fixtures';
import {
  applyExpParityLocalAddress,
  postExpParityLegalCustomer,
} from './shared/exp-parity-customer.fixtures';
import { parityAmount, parityRole } from './shared/exp-parity-snapshot.fixtures';

/** Pseudo-key placeholder — replace once a real Jira key exists. */
export const EXP_PARITY_05_KEY = 'EXP-PARITY-05';

export const EXP_PARITY_05_CASE_ID = 'case-05-data-by-profile-rounding';

export const EXP_PARITY_05_TITLE =
  'Fifteen-minute profile volumes are rounded once on the total rather than per computed period';

// ═══════════════════════════════════════════════════════════════════════════
// TUNABLE CONSTANT BLOCK — the numbers that make the two rounding strategies
// produce DIFFERENT answers. Read this before changing anything.
//
// Design goal
// -----------
// Every computed period must have an EXACT amount whose third decimal is 5 and
// which has no further decimals (`amount * 200` is an odd integer), while the
// SUM over all periods must land on an exact 2-decimal value. Then:
//
//   sum-then-round-once      = the exact sum          (no rounding decision)
//   round-each-then-sum      = exact sum + 0.01 * n   (n = period count)
//
// so the two strategies always differ, AND the expected value never depends on
// whether the engine uses HALF_UP or HALF_EVEN — the expectation is exact.
//
// Why these specific values
// -------------------------
//   slots per period = PERIOD_DAYS * 96                      = 480
//   kWh   per period = 480 * 1.5625                          = 750      (exact)
//   BGN   per period = 750 * 0.0953                          = 71.475   (.  .5)
//   exact total      = 4 * 71.475                            = 285.90   (2 dp)
//   round-each-then-sum = 4 * 71.48                          = 285.92
//
// 1.5625 kWh/slot is 25/16 — exactly representable in binary, so the volume
// arithmetic carries no float error of its own. 0.0953 BGN/kWh is a realistic
// four-decimal energy tariff. Both values stay inside the 4-decimal precision
// the energy-data Excel and the price component use.
//
// Constraint on PERIOD_DAYS (do not change blindly)
// -------------------------------------------------
// With a 4-decimal kWh/slot and a 4-decimal price, a `.005` residue is only
// reachable when `PERIOD_DAYS` is ODD (the 96 slots/day contribute 2^5, and an
// even day count adds another factor of 2 that cancels the half-cent). 3, 5 and
// 7 all work; 4, 6 and 8 are impossible. Keep PERIOD_COUNT EVEN so the exact
// total lands on 2 decimals and the expectation needs no rounding decision.
//
// Why coverage stops at day 20 instead of the calendar month
// ---------------------------------------------------------------------
// The Excel generator (`mass-imports/generators/domains/priceParameterMP.ts`,
// outside `tests/`, not modifiable) steps 15 minutes at a time over local wall
// clock and is NOT DST-aware. EU DST transitions always fall on days 25-31.
// `BillingByProfileCreateRequest` requires `periodFrom` = day 1 and
// `periodTo` = last day of the month, so the profile itself covers the whole
// month. The four computed periods are dateOfMonths bands on days 1–20 only;
// days 21–end are uploaded but not priced, so DST slot-count noise cannot
// change the engineered 71.475 residue.
//
// `POST /billing-by-profile` with a mid-month `periodTo` (the previous four
// window records) is rejected: "periodTo-Period to must be last day of the month".
// ═══════════════════════════════════════════════════════════════════════════

/** Number of dateOfMonths price components = computed periods. Keep EVEN. */
export const EXP_PARITY_05_PERIOD_COUNT = 4;
/** Length of each period in whole days. Keep ODD; keep COUNT * DAYS <= 24. */
export const EXP_PARITY_05_PERIOD_DAYS = 5;
/** 15-minute slots in a day. Fixed by `FIFTEEN_MINUTES`. */
export const EXP_PARITY_05_SLOTS_PER_DAY = 96;
/** kWh written into every 15-minute slot (25/16 — exact in binary). */
export const EXP_PARITY_05_KWH_PER_SLOT = 1.5625;
/** Price component unit price in BGN/kWh. */
export const EXP_PARITY_05_UNIT_PRICE = 0.0953;
/** Swagger declares `PriceComponentFormulaRequest.expression` as a string. */
export const EXP_PARITY_05_UNIT_PRICE_EXPRESSION = String(EXP_PARITY_05_UNIT_PRICE);
/** Invoice money precision the engines are expected to use. */
export const EXP_PARITY_05_AMOUNT_DECIMALS = 2;
/** Volume precision used when comparing kWh (energy data is 4-decimal). */
export const EXP_PARITY_05_VOLUME_DECIMALS = 4;
/** Pinned settlement-period profile share (generator default is random). */
const SETTLEMENT_PROFILE_PERCENTAGE = 100;

const MONTH_ENUMS = [
  'JANUARY',
  'FEBRUARY',
  'MARCH',
  'APRIL',
  'MAY',
  'JUNE',
  'JULY',
  'AUGUST',
  'SEPTEMBER',
  'OCTOBER',
  'NOVEMBER',
  'DECEMBER',
] as const;

/** `EditDateOfMonthRequest.monthNumbers` enum, index 0 == day 1. */
const DAY_OF_MONTH_ENUMS = [
  'ONE',
  'TWO',
  'THREE',
  'FOUR',
  'FIVE',
  'SIX',
  'SEVEN',
  'EIGHT',
  'NINE',
  'TEN',
  'ELEVEN',
  'TWELVE',
  'THIRTEEN',
  'FOURTEEN',
  'FIFTEEN',
  'SIXTEEN',
  'SEVENTEEN',
  'EIGHTEEN',
  'NINETEEN',
  'TWENTY',
  'TWENTYONE',
  'TWENTYTWO',
  'TWENTYTHREE',
  'TWENTYFOUR',
  'TWENTYFIVE',
  'TWENTYSIX',
  'TWENTYSEVEN',
  'TWENTYEIGHT',
  'TWENTYNINE',
  'THIRTY',
  'THIRTYONE',
] as const;

/** Preconditions + 4 profile uploads + cron settle + async pipeline. */
export const EXP_PARITY_05_TEST_TIMEOUT_MS = 120 * 60 * 1000;

const BILLING_BY_PROFILE_ROOT = 'billing-by-profile';
const POD_MANUAL_ACTIVATION_ROOT = 'contract-pods/manual';

export type ExpParity05Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type ExpParity05Request = ExpParity05Fx['Request'];

export type { ExpParityBillingRunResult };

/**
 * Half-up rounding that first neutralises binary representation error.
 *
 * `750 * 0.0953` is `71.47500000000001` in IEEE-754; a naive
 * `Math.round(v * 100) / 100` would still work here, but the same expression
 * can land just BELOW the half on other inputs and silently round down. The
 * `toFixed(6)` pass snaps the scaled value onto its decimal intent first.
 * Only positive money is rounded here, so `Math.round`'s toward-+Infinity
 * behaviour on exact halves is the intended HALF_UP.
 */
export function roundHalfUp(value: number, decimals: number): number {
  const factor = Math.pow(10, decimals);
  const scaled = Number((value * factor).toFixed(6));
  return Math.round(scaled) / factor;
}

function sum(values: number[]): number {
  return values.reduce((total, value) => total + value, 0);
}

/** The arithmetic the constant block promises. Asserted by the spec. */
export type ExpParity05Design = {
  periodCount: number;
  periodDays: number;
  slotsPerPeriod: number;
  kwhPerSlot: number;
  unitPrice: number;
  kwhPerPeriod: number;
  exactAmountPerPeriod: number;
  roundedAmountPerPeriod: number;
  totalKwh: number;
  exactTotalAmount: number;
  sumThenRoundOnce: number;
  roundEachPeriodThenSum: number;
  strategyGap: number;
  strategiesDiffer: boolean;
};

function buildExpParity05Design(): ExpParity05Design {
  const slotsPerPeriod = EXP_PARITY_05_PERIOD_DAYS * EXP_PARITY_05_SLOTS_PER_DAY;
  const kwhPerPeriod = slotsPerPeriod * EXP_PARITY_05_KWH_PER_SLOT;
  const exactAmountPerPeriod = kwhPerPeriod * EXP_PARITY_05_UNIT_PRICE;
  const roundedAmountPerPeriod = roundHalfUp(exactAmountPerPeriod, EXP_PARITY_05_AMOUNT_DECIMALS);
  const exactTotalAmount = exactAmountPerPeriod * EXP_PARITY_05_PERIOD_COUNT;
  const sumThenRoundOnce = roundHalfUp(exactTotalAmount, EXP_PARITY_05_AMOUNT_DECIMALS);
  const roundEachPeriodThenSum = roundHalfUp(
    roundedAmountPerPeriod * EXP_PARITY_05_PERIOD_COUNT,
    EXP_PARITY_05_AMOUNT_DECIMALS,
  );

  return {
    periodCount: EXP_PARITY_05_PERIOD_COUNT,
    periodDays: EXP_PARITY_05_PERIOD_DAYS,
    slotsPerPeriod,
    kwhPerSlot: EXP_PARITY_05_KWH_PER_SLOT,
    unitPrice: EXP_PARITY_05_UNIT_PRICE,
    kwhPerPeriod,
    exactAmountPerPeriod,
    roundedAmountPerPeriod,
    totalKwh: kwhPerPeriod * EXP_PARITY_05_PERIOD_COUNT,
    exactTotalAmount: roundHalfUp(exactTotalAmount, 6),
    sumThenRoundOnce,
    roundEachPeriodThenSum,
    strategyGap: roundHalfUp(
      roundEachPeriodThenSum - sumThenRoundOnce,
      EXP_PARITY_05_AMOUNT_DECIMALS,
    ),
    strategiesDiffer: sumThenRoundOnce !== roundEachPeriodThenSum,
  };
}

/**
 * Designed (data-independent) arithmetic of the constant block:
 * 480 slots × 1.5625 kWh = 750 kWh per period, × 0.0953 = 71.475 BGN,
 * × 4 periods = 285.90 exact, versus 4 × 71.48 = 285.92 when each period is
 * rounded first.
 */
export const EXP_PARITY_05_DESIGN: ExpParity05Design = buildExpParity05Design();

/** One contiguous slice of the billed month that becomes one computed period. */
export type ExpParity05Window = {
  /** 0-based position of the slice inside the month. */
  periodIndex: number;
  /** First day of the slice (`yyyy-mm-dd`). */
  startDate: string;
  /** Last day of the slice (`yyyy-mm-dd`), inclusive. */
  endDate: string;
  dayCount: number;
  slotCount: number;
  kwhPerSlot: number;
  expectedKwh: number;
};

function addDays(isoDate: string, days: number): string {
  const shifted = new Date(`${isoDate}T00:00:00Z`);
  shifted.setUTCDate(shifted.getUTCDate() + days);
  return shifted.toISOString().slice(0, 10);
}

function lastIsoDayOfMonth(monthStart: string): string {
  const shifted = new Date(`${monthStart}T00:00:00Z`);
  shifted.setUTCMonth(shifted.getUTCMonth() + 1, 0);
  return shifted.toISOString().slice(0, 10);
}

function dateOfMonthsForWindow(
  window: ExpParity05Window,
): Array<{ month: (typeof MONTH_ENUMS)[number]; monthNumbers: string[] }> {
  const monthIndex = new Date(`${window.startDate}T00:00:00Z`).getUTCMonth();
  const fromDay = Number(window.startDate.slice(8, 10));
  const toDay = Number(window.endDate.slice(8, 10));
  return [
    {
      month: MONTH_ENUMS[monthIndex],
      monthNumbers: [...DAY_OF_MONTH_ENUMS.slice(fromDay - 1, toDay)],
    },
  ];
}

/**
 * `EXP_PARITY_05_PERIOD_COUNT` adjacent slices of the PREVIOUS month, each
 * `EXP_PARITY_05_PERIOD_DAYS` days long, starting on day 1 — no gaps, no
 * overlaps, and never reaching the DST-transition days (see constant block).
 */
export function expParity05Windows(): ExpParity05Window[] {
  const monthStart = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
  const windows: ExpParity05Window[] = [];

  for (let periodIndex = 0; periodIndex < EXP_PARITY_05_PERIOD_COUNT; periodIndex++) {
    const startDate = addDays(monthStart, periodIndex * EXP_PARITY_05_PERIOD_DAYS);
    const endDate = addDays(startDate, EXP_PARITY_05_PERIOD_DAYS - 1);
    const slotCount = EXP_PARITY_05_PERIOD_DAYS * EXP_PARITY_05_SLOTS_PER_DAY;
    windows.push({
      periodIndex,
      startDate,
      endDate,
      dayCount: EXP_PARITY_05_PERIOD_DAYS,
      slotCount,
      kwhPerSlot: EXP_PARITY_05_KWH_PER_SLOT,
      expectedKwh: slotCount * EXP_PARITY_05_KWH_PER_SLOT,
    });
  }

  return windows;
}

export type ExpParity05Chain = {
  customerId: number;
  customerIdentifier: string;
  podId: number;
  priceComponentId: number;
  priceComponentIds: number[];
  productId: number;
  contractId: number;
  contractNumber: string;
  billingByProfileIds: number[];
  windows: ExpParity05Window[];
  monthStart: string;
  monthEnd: string;
  profileCoverageEnd: string;
  designedTotalKwh: number;
};

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

/**
 * Full precondition chain: one legal customer, four settlement price components
 * priced at {@link EXP_PARITY_05_UNIT_PRICE} (dateOfMonths bands for the four
 * engineered windows, all INVOICE_ONE), one term, one settlement POD, one
 * product carrying those price components, one contract, POD activation over
 * the whole billed month, and one month-bounded FIFTEEN_MINUTES
 * `billing-by-profile` record (`periodFrom` day 1, `periodTo` last day).
 *
 * Entity order follows `precondition-data-creation.instructions.md`:
 * terms → price components → products → customers → PODs → contracts → energy
 * data → billing run. The customer is created first only because
 * `product_contract()` reads it; no entity is created out of dependency order.
 *
 * Called from inside the test (no `beforeAll` — Rule 40).
 */
export async function buildExpParity05RoundingChain(
  fx: ExpParity05Fx,
): Promise<ExpParity05Chain> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  const windows = expParity05Windows();
  const monthStart = windows[0].startDate;
  const profileCoverageEnd = windows[windows.length - 1].endDate;
  const monthEnd = lastIsoDayOfMonth(monthStart);

  let customerId = 0;
  let customerIdentifier = '';
  const customer = await test.step('Precondition: legal customer', async () => {
    const created = await postExpParityLegalCustomer(fx);
    customerId = created.id;
    customerIdentifier = created.identifier;
    return created;
  });

  await test.step('Precondition: terms', async () => {
    const res = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(res).CheckResponse();
    Responses.terms.push(await res.json());
  });

  const priceComponentIds: number[] = [];
  await test.step(
    `Precondition: ${EXP_PARITY_05_PERIOD_COUNT} settlement price components at ` +
      `${EXP_PARITY_05_UNIT_PRICE} per kWh (dateOfMonths bands, INVOICE_ONE, profile share 100%)`,
    async () => {
      for (const window of windows) {
        const payload = GeneratePayload.productAndServices.priceSettlement();
        payload.formulaRequest.expression = EXP_PARITY_05_UNIT_PRICE_EXPRESSION;
        payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
        payload.name = `EXP-PARITY-05 window ${window.periodIndex + 1}`;
        payload.displayName = payload.name;
        const settlement = payload.applicationModelRequest.settlementPeriodsRequest;
        settlement.dateOfMonths = dateOfMonthsForWindow(window);
        settlement.profiles[0].percentage = SETTLEMENT_PROFILE_PERCENTAGE;
        const res = await Request.post(Endpoints.priceComponent, { data: payload });
        await expect(res).CheckResponse();
        const raw = await res.json();
        Responses.priceComponent.push(raw);
        priceComponentIds.push(asEntityId(raw));
      }
    },
  );
  const priceComponentId = priceComponentIds[0];

  let productId = 0;
  await test.step('Precondition: product carrying only that price component', async () => {
    const payload = applyExpParityCombinedProduct(GeneratePayload.productAndServices.product(0));
    // product() harvests every price component, group, interim and penalty
    // present in Responses — pin the lists so the invoice has exactly one
    // priced line source and the total is attributable to this case alone.
    payload.priceComponentIds = priceComponentIds;
    payload.priceComponentGroupIds = [];
    payload.interimAdvancePayments = [];
    payload.interimAdvancePaymentGroups = [];
    const res = await Request.post(Endpoints.product, { data: payload });
    await expect(res).CheckResponse();
    const raw = await res.json();
    Responses.product.push(raw);
    productId = asEntityId(raw);
  });

  let podId = 0;
  await test.step('Precondition: settlement POD', async () => {
    const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
    applyExpParityLocalAddress(podPayload, customer.addressIds);
    const res = await Request.post(Endpoints.pod, { data: podPayload });
    await expect(res).CheckResponse();
    const body = (await res.json()) as { id?: number };
    Responses.pod.push(body);
    podId = asEntityId(body);
  });

  let contractId = 0;
  await test.step('Precondition: product contract', async () => {
    const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    payload.productParameters.interimAdvancePayments = [];
    const raw = await postExpParitySignedProductContract(Request, payload);
    Responses.productContract.push(raw);
    contractId = asEntityId(raw);
  });

  await test.step(`Precondition: activate POD on the contract from ${monthStart}`, async () => {
    const activation = await GeneratePayload.pointsOfDelivery.pod_activation(0, monthStart);
    const res = await Request.post(POD_MANUAL_ACTIVATION_ROOT, { data: activation });
    await expect(res).CheckResponse();
  });

  const contractNumber = await readContractNumber(Request, contractId);

  const billingByProfileIds: number[] = [
    await postExpParity05MonthProfile(fx, monthStart, monthEnd),
  ];

  return {
    customerId,
    customerIdentifier,
    podId,
    priceComponentId,
    priceComponentIds,
    productId,
    contractId,
    contractNumber,
    billingByProfileIds,
    windows,
    monthStart,
    monthEnd,
    profileCoverageEnd,
    designedTotalKwh: EXP_PARITY_05_DESIGN.totalKwh,
  };
}

/**
 * One month-bounded FIFTEEN_MINUTES `billing-by-profile` record.
 *
 * `BillingByProfileCreateRequest` requires `periodFrom` = first day and
 * `periodTo` = last day of the same month. Volume is written for the whole
 * month at {@link EXP_PARITY_05_KWH_PER_SLOT}; only days 1–20 are priced
 * (dateOfMonths on the four price components).
 */
async function postExpParity05MonthProfile(
  fx: ExpParity05Fx,
  monthStart: string,
  monthEnd: string,
): Promise<number> {
  const { Request, GeneratePayload, Responses } = fx;
  const periodFrom = `${monthStart}T00:00:00.000Z`;
  const periodTo = `${monthEnd}T23:45:00.000Z`;

  let billingByProfileId = 0;
  await test.step(
    `Precondition: FIFTEEN_MINUTES profile for ${monthStart}..${monthEnd} ` +
      `(${EXP_PARITY_05_KWH_PER_SLOT} kWh/slot; priced days 1–${EXP_PARITY_05_PERIOD_COUNT * EXP_PARITY_05_PERIOD_DAYS})`,
    async () => {
      const payload = (await GeneratePayload.energyData.profile15minute(0, [
        {
          startDate: `${monthStart}T00:00:00`,
          endDate: `${monthEnd}T23:45:00`,
          amount: EXP_PARITY_05_KWH_PER_SLOT,
        },
      ])) as Record<string, unknown>;
      payload.periodType = 'FIFTEEN_MINUTES';
      payload.periodFrom = periodFrom;
      payload.periodTo = periodTo;
      payload.warningAcceptedByUser = true;
      payload.timeZone = 'CET';

      const res = await Request.post(BILLING_BY_PROFILE_ROOT, { data: payload });
      await expect(res).CheckResponse();
      billingByProfileId = asEntityId(await res.json());
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

  return billingByProfileId;
}

/** Creates the CONTRACT-level `STANDARD_BILLING` / `FOR_VOLUMES` run. */
export async function createExpParity05ContractVolumesRun(
  fx: ExpParity05Fx,
): Promise<{ billingRunId: number; billingPayload: Record<string, unknown> }> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;
  let billingPayload: Record<string, unknown> | undefined;
  let lastError: unknown;
  for (let attempt = 1; attempt <= 3; attempt++) {
    try {
      billingPayload = (await GeneratePayload.billing.billingRun(
        'CONTRACT',
        ['FOR_VOLUMES'],
        0,
        0,
      )) as Record<string, unknown>;
      lastError = undefined;
      break;
    } catch (error) {
      lastError = error;
      const text = error instanceof Error ? error.message : String(error);
      if (!/ECONNABORTED|ECONNRESET|ETIMEDOUT|socket hang up/i.test(text) || attempt === 3) {
        throw error;
      }
      await new Promise((resolve) => setTimeout(resolve, 5000 * attempt));
    }
  }
  if (!billingPayload) {
    throw lastError instanceof Error ? lastError : new Error('billingRun payload was not built');
  }
  const res = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(res).CheckResponse();
  const billingRunId = asEntityId(await res.json());
  Responses.billingRun.push(billingRunId);
  return { billingRunId, billingPayload };
}

export { settleBillingDataPreparationCron, startBillingAndReadDrafts };

// ═══════════════════════════════════════════════════════════════════════════
// INVOICE ROUNDING ANALYSIS
// ═══════════════════════════════════════════════════════════════════════════

/** Where the unit price used for the exact arithmetic came from. */
export type ExpParity05UnitPriceSource =
  | 'detailed-data'
  | 'summary-data'
  | 'configured-constant'
  | 'none';

/** Which rounding strategy the invoice total actually matches. */
export type ExpParity05MatchedStrategy =
  | 'sum-then-round-once'
  | 'round-each-period-then-sum'
  | 'both-identical'
  | 'neither';

/** Aggregated facts of one computed period (one `periodFrom`/`periodTo` pair). */
export type ExpParity05PeriodFacts = {
  periodIndex: number;
  /** Kept for humans; stripped from the parity snapshot (month-dependent). */
  periodFrom: string;
  periodTo: string;
  rowCount: number;
  volumeKwh: number;
  unitPrice: number | null;
  /** volume × unit price at full precision — before any rounding. */
  exactAmount: number | null;
  /** The exact amount rounded to invoice precision on its own. */
  roundedAmount: number | null;
  /** What the engine actually printed on the period's lines. */
  reportedAmount: number | null;
};

/** Snapshot-safe projection: drops the month-dependent period boundaries. */
export function parityPeriodFacts(
  facts: ExpParity05PeriodFacts,
): Omit<ExpParity05PeriodFacts, 'periodFrom' | 'periodTo'> {
  const { periodFrom: _from, periodTo: _to, ...rest } = facts;
  return rest;
}

export type ExpParity05RoundingFacts = {
  contractRole: string;
  invoiceType: string;
  invoiceStatus: string;
  invoiceDocumentType: string;
  invoiceTotalExcludingVat: number | null;
  invoiceTotalIncludingVat: number | null;
  summaryRowCount: number;
  detailedRowCount: number;
  computedPeriodCount: number;
  periods: ExpParity05PeriodFacts[];
  unitPriceSource: ExpParity05UnitPriceSource;
  observedUnitPrices: number[];
  /** Σ of the per-period volumes reported on `invoice/detailed-data`. */
  detailedVolumeKwh: number;
  /** Σ of the volumes reported on `invoice/summary-data`. */
  summaryVolumeKwh: number | null;
  /** detailed − summary; must be 0 or fractions were lost between the tabs. */
  volumeReconciliationDelta: number | null;
  /** Σ (period volume × unit price), full precision. */
  exactTotalAmount: number | null;
  /** Expected: round the exact total exactly once. */
  sumThenRoundOnce: number | null;
  /** Drift candidate: round every computed period first, then add them up. */
  roundEachPeriodThenSum: number | null;
  /** Finer drift candidate: round every invoice LINE first, then add them up. */
  roundEachRowThenSum: number | null;
  /** invoice total − sum-then-round-once; must be 0. */
  reconciliationDelta: number | null;
  strategiesDistinguishable: boolean;
  matchedStrategy: ExpParity05MatchedStrategy;
};

function toAmount(raw: unknown): number | null {
  return parityAmount(raw, 6);
}

function distinctSorted(values: number[]): number[] {
  return Array.from(new Set(values)).sort((a, b) => a - b);
}

function rowVolume(row: Pdt2599InvoiceTabularLineRow): number | null {
  return toAmount(row.totalVolumes);
}

/**
 * Reads every fact the rounding question needs from one invoice:
 * per-period volumes and amounts from `invoice/detailed-data`, the volume
 * cross-check from `invoice/summary-data`, and the header total from
 * `GET /invoice`.
 *
 * The two candidate expectations are computed from the volumes and unit prices
 * the invoice ITSELF reports, not from the fixture constants, so the comparison
 * stays meaningful even if the ingested volume differs from the design.
 */
export async function loadExpParity05RoundingFacts(
  Request: ExpParity05Request,
  invoiceId: number,
  contractRoleById: Record<string, number>,
): Promise<ExpParity05RoundingFacts> {
  const res = await Request.get(`invoice?id=${invoiceId}`);
  await expect(res).CheckResponse();
  const invoice = (await res.json()) as {
    invoiceType?: string;
    invoiceStatus?: string;
    invoiceDocumentType?: string;
    totalAmountExcludingVat?: string;
    totalAmountIncludingVat?: string;
    productContract?: { id?: number } | null;
  };

  const summaryRows = await fetchPdt2599InvoiceSummaryRows(Request, invoiceId);
  const detailedRows = await fetchPdt2599InvoiceDetailedRows(Request, invoiceId);

  const detailedUnitPrices = distinctSorted(
    detailedRows
      .map((row) => toAmount(row.unitPrice))
      .filter((value): value is number => value !== null),
  );
  const summaryUnitPrices = distinctSorted(
    summaryRows
      .map((row) => toAmount(row.unitPrice))
      .filter((value): value is number => value !== null),
  );

  let unitPriceSource: ExpParity05UnitPriceSource = 'none';
  let fallbackUnitPrice: number | null = null;
  if (detailedUnitPrices.length > 0) {
    unitPriceSource = 'detailed-data';
    fallbackUnitPrice = detailedUnitPrices[0];
  } else if (summaryUnitPrices.length > 0) {
    unitPriceSource = 'summary-data';
    fallbackUnitPrice = summaryUnitPrices[0];
  } else if (detailedRows.length > 0) {
    // Neither tab priced the lines; fall back to the configured tariff so the
    // arithmetic is still evaluated, and make the fallback visible in the diff.
    unitPriceSource = 'configured-constant';
    fallbackUnitPrice = EXP_PARITY_05_UNIT_PRICE;
  }

  type PeriodBucket = {
    periodFrom: string;
    periodTo: string;
    rows: Pdt2599InvoiceTabularLineRow[];
  };
  const buckets = new Map<string, PeriodBucket>();
  for (const row of detailedRows) {
    const periodFrom = String(row.periodFrom ?? '');
    const periodTo = String(row.periodTo ?? '');
    const key = `${periodFrom}|${periodTo}`;
    const bucket = buckets.get(key) ?? { periodFrom, periodTo, rows: [] };
    bucket.rows.push(row);
    buckets.set(key, bucket);
  }

  const orderedBuckets = Array.from(buckets.values()).sort((a, b) =>
    `${a.periodFrom}|${a.periodTo}`.localeCompare(`${b.periodFrom}|${b.periodTo}`),
  );

  const periods: ExpParity05PeriodFacts[] = orderedBuckets.map((bucket, periodIndex) => {
    const volumes = bucket.rows
      .map(rowVolume)
      .filter((value): value is number => value !== null);
    const volumeKwh = roundHalfUp(sum(volumes), EXP_PARITY_05_VOLUME_DECIMALS);

    const prices = distinctSorted(
      bucket.rows
        .map((row) => toAmount(row.unitPrice))
        .filter((value): value is number => value !== null),
    );
    const unitPrice = prices.length > 0 ? prices[0] : fallbackUnitPrice;

    const reported = bucket.rows
      .map((row) => toAmount(row.value))
      .filter((value): value is number => value !== null);

    const exactAmount = unitPrice === null ? null : volumeKwh * unitPrice;

    return {
      periodIndex,
      periodFrom: bucket.periodFrom,
      periodTo: bucket.periodTo,
      rowCount: bucket.rows.length,
      volumeKwh,
      unitPrice,
      exactAmount: exactAmount === null ? null : roundHalfUp(exactAmount, 6),
      roundedAmount:
        exactAmount === null ? null : roundHalfUp(exactAmount, EXP_PARITY_05_AMOUNT_DECIMALS),
      reportedAmount:
        reported.length > 0 ? roundHalfUp(sum(reported), EXP_PARITY_05_AMOUNT_DECIMALS) : null,
    };
  });

  const pricedPeriods = periods.filter((period) => period.exactAmount !== null);
  const allPeriodsPriced = pricedPeriods.length === periods.length && periods.length > 0;

  const exactTotalAmount = allPeriodsPriced
    ? roundHalfUp(sum(pricedPeriods.map((period) => period.exactAmount as number)), 6)
    : null;
  const sumThenRoundOnce =
    exactTotalAmount === null
      ? null
      : roundHalfUp(exactTotalAmount, EXP_PARITY_05_AMOUNT_DECIMALS);
  const roundEachPeriodThenSum = allPeriodsPriced
    ? roundHalfUp(
        sum(pricedPeriods.map((period) => period.roundedAmount as number)),
        EXP_PARITY_05_AMOUNT_DECIMALS,
      )
    : null;

  const roundEachRowThenSum =
    detailedRows.length > 0 && fallbackUnitPrice !== null
      ? roundHalfUp(
          sum(
            detailedRows.map((row) => {
              const volume = rowVolume(row) ?? 0;
              const price = toAmount(row.unitPrice) ?? fallbackUnitPrice;
              return roundHalfUp(volume * (price as number), EXP_PARITY_05_AMOUNT_DECIMALS);
            }),
          ),
          EXP_PARITY_05_AMOUNT_DECIMALS,
        )
      : null;

  const detailedVolumeKwh = roundHalfUp(
    sum(periods.map((period) => period.volumeKwh)),
    EXP_PARITY_05_VOLUME_DECIMALS,
  );
  const summaryVolumes = summaryRows
    .map(rowVolume)
    .filter((value): value is number => value !== null);
  const summaryVolumeKwh =
    summaryVolumes.length > 0
      ? roundHalfUp(sum(summaryVolumes), EXP_PARITY_05_VOLUME_DECIMALS)
      : null;

  const invoiceTotalExcludingVat = parityAmount(
    invoice.totalAmountExcludingVat,
    EXP_PARITY_05_AMOUNT_DECIMALS,
  );

  const reconciliationDelta =
    invoiceTotalExcludingVat === null || sumThenRoundOnce === null
      ? null
      : roundHalfUp(invoiceTotalExcludingVat - sumThenRoundOnce, EXP_PARITY_05_AMOUNT_DECIMALS);

  const strategiesDistinguishable =
    sumThenRoundOnce !== null &&
    roundEachPeriodThenSum !== null &&
    sumThenRoundOnce !== roundEachPeriodThenSum;

  let matchedStrategy: ExpParity05MatchedStrategy = 'neither';
  if (sumThenRoundOnce !== null && sumThenRoundOnce === roundEachPeriodThenSum) {
    matchedStrategy =
      invoiceTotalExcludingVat === sumThenRoundOnce ? 'both-identical' : 'neither';
  } else if (invoiceTotalExcludingVat !== null && invoiceTotalExcludingVat === sumThenRoundOnce) {
    matchedStrategy = 'sum-then-round-once';
  } else if (
    invoiceTotalExcludingVat !== null &&
    invoiceTotalExcludingVat === roundEachPeriodThenSum
  ) {
    matchedStrategy = 'round-each-period-then-sum';
  }

  return {
    contractRole: parityRole(contractRoleById, invoice.productContract?.id),
    invoiceType: String(invoice.invoiceType ?? 'unknown'),
    invoiceStatus: String(invoice.invoiceStatus ?? 'unknown'),
    invoiceDocumentType: String(invoice.invoiceDocumentType ?? 'unknown'),
    invoiceTotalExcludingVat,
    invoiceTotalIncludingVat: parityAmount(
      invoice.totalAmountIncludingVat,
      EXP_PARITY_05_AMOUNT_DECIMALS,
    ),
    summaryRowCount: summaryRows.length,
    detailedRowCount: detailedRows.length,
    computedPeriodCount: periods.length,
    periods,
    unitPriceSource,
    observedUnitPrices:
      detailedUnitPrices.length > 0 ? detailedUnitPrices : summaryUnitPrices,
    detailedVolumeKwh,
    summaryVolumeKwh,
    volumeReconciliationDelta:
      summaryVolumeKwh === null
        ? null
        : roundHalfUp(detailedVolumeKwh - summaryVolumeKwh, EXP_PARITY_05_VOLUME_DECIMALS),
    exactTotalAmount,
    sumThenRoundOnce,
    roundEachPeriodThenSum,
    roundEachRowThenSum,
    reconciliationDelta,
    strategiesDistinguishable,
    matchedStrategy,
  };
}

/**
 * Assertion message that names the behaviour actually observed, so a failure
 * report tells the reader which engine strategy produced the total.
 */
export function describeExpParity05Outcome(facts: ExpParity05RoundingFacts): string {
  const observed =
    facts.matchedStrategy === 'sum-then-round-once'
      ? 'the total was rounded once on the sum (expected)'
      : facts.matchedStrategy === 'round-each-period-then-sum'
        ? 'the total is the sum of per-period roundings (rounding drift)'
        : facts.matchedStrategy === 'both-identical'
          ? 'both strategies give the same value, so this run cannot tell them apart'
          : 'the total matches neither strategy';

  return (
    `${facts.computedPeriodCount} computed period(s); ` +
    `invoice total (excl. VAT) = ${facts.invoiceTotalExcludingVat}; ` +
    `sum-then-round-once = ${facts.sumThenRoundOnce}; ` +
    `round-each-period-then-sum = ${facts.roundEachPeriodThenSum}; ` +
    `round-each-row-then-sum = ${facts.roundEachRowThenSum}; ` +
    `reconciliation delta = ${facts.reconciliationDelta} — ${observed}.`
  );
}

/** Logical role map — keeps raw ids out of the parity snapshot. */
export function expParity05ContractRoles(chain: ExpParity05Chain): Record<string, number> {
  return { contractUnderTest: chain.contractId };
}
