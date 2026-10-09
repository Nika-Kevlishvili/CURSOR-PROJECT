/**
 * [EXP-PARITY-02]  -- Price component date coverage vs the billing run calculated period.
 *
 * NOTE ON THE KEY: `EXP-PARITY-02` is a PLACEHOLDER pseudo-key. No Jira ticket exists
 * for this parity work yet. Replace the key and the titles once a real Jira key is
 * assigned; nothing else in these files depends on the key.
 *
 * PURPOSE  -- Experiment vs PreProd parity
 * --------------------------------------
 * Phoenix standard billing was rewritten on the Experiment environment: it now runs
 * asynchronously through `billing_run.standard_billing_preparation` and
 * `standard_billing_stage_01..22`, where stages 05 / 09 / 12 execute as parallel
 * workers that commit independently. PreProd still runs the old synchronous engine
 * (`billing_run.generate_run_volume`).
 *
 * The three tests in the companion spec run UNCHANGED against both environments  --
 * only `process.env.BASE_URL` differs (see `playwright.config.ts`, `use.baseURL`).
 * There is deliberately NO branch on environment, hostname, or env var anywhere in
 * this file or the spec. Differences between the two engines must surface as data in
 * the `attachParitySnapshot` attachment, never as divergent code paths.
 *
 * THE ONE LEVER: price component date coverage
 * --------------------------------------------
 * All three cases move exactly one thing  -- whether a price component's date coverage
 * includes the billing run's calculated period. Everything else (customer, POD,
 * contract, profile volumes, settlement periods, profile percentage, formula) is held
 * identical so any diff is attributable to the lever.
 *
 * Coverage lives in `applicationModelRequest.settlementPeriodsRequest.dateOfMonths`.
 * Verified against the refreshed Swagger contract
 * (`Cursor-Project/config/swagger/experiment/swagger-spec.json`):
 *   EditDateOfMonthRequest: required [month, monthNumbers]
 *     month        = JANUARY..DECEMBER
 *     monthNumbers = ONE..THIRTYONE | ALL_DAYS   (uniqueItems)
 * The stock generator payload lists all twelve months with `ALL_DAYS`; restricting the
 * list is therefore the minimal, contract-legal way to make a component miss a period.
 *
 * `VolumesBySettlementPeriodsRequest.yearRound` is a Java primitive `boolean`, so an
 * absent JSON property deserializes to `false` and `dateOfMonths` is the effective
 * coverage whitelist. These payloads omit it exactly like the stock generator does.
 *
 * Confluence "Price component conditions" (local extract
 * `Cursor-Project/config/confluence/tmp-pc-conditions-150241300.txt`) documents
 * `calculated period from` / `calculated period to` as price component condition
 * fields, which is the documented counterpart of this lever.
 *
 * SWAGGER-FORCED DEVIATION (read this before changing the slot test)
 * -----------------------------------------------------------------
 * The brief asks Case 2 for "a scale with four scale codes, and all four scale codes
 * must match every price component" while also keeping `dateOfMonths` as the single
 * lever. Swagger says those two cannot coexist on one price component:
 *   - `dateOfMonths` exists ONLY on `VolumesBySettlementPeriodsRequest`
 *     (applicationType `BY_SETTLEMENT_PERIODS`).
 *   - `VolumesByScaleRequest` (applicationType `BY_SCALES`) carries `scaleIds` but has
 *     NO `dateOfMonths`; its only date lever is `periodsOfYear`
 *     (`APPeriodOfYearRequest`, required `startDate` / `endDate` as plain strings  --
 *     Phoenix parses them as recurring `dd.MM` day-of-year markers, not calendar dates).
 * The brief's own governing rule wins: every slot component stays
 * `BY_SETTLEMENT_PERIODS` and differs ONLY in `dateOfMonths`. The "four scale codes"
 * precondition is still created for real on the POD meter (see
 * `ensureExpParityNomenclatures` / `createSlpPodWithMeter`) so the scale dimension is
 * materially present and provably identical for every slot  -- it just cannot be
 * expressed as `scaleIds` on a settlement-period component.
 *
 * OBSERVABILITY (all API, no DB)
 * ------------------------------
 *   GET   /billing-run/{id}                                  -> commonParameters.status
 *   GET   /billing-run/draft-invoices?id=&page=&size=         -> produced invoices
 *   GET   /invoice/detailed-data?id=&page=&size=              -> InvoiceDetailedDataResponse
 *                                                                (priceComponent, periodFrom,
 *                                                                 periodTo, unitPrice, value,
 *                                                                 totalVolumes) = the
 *                                                                 settlement_period rows
 *   PATCH /billing-run/download-error-report/{id}?protocol=BILLING
 *                                                             -> xlsx, columns
 *                                                                `invoice_number` / `error_message`
 *
 * Mandatory reading applied: `.cursor/skills/energo-ts-test/SKILL.md`, the playwright
 * instructions pack under `Cursor-Project/config/playwright_generation/playwright instructions/`.
 *
 * Reference spec(s) read end-to-end:
 * - tests/cursor/SLR-18167-dev2-volumes-billing-replica.spec.ts + .fixtures.ts
 *   (precondition chain, DRAFT poller, FOR_VOLUMES billing run shape)
 * - tests/billing/forVolumes/forVolumes.spec.ts       (REG-962  -- 4 slots on profiles)
 * - tests/billing/slotSplitting/invoiceSlotSplitting.spec.ts (REG-1020  -- 2 slots, none fail)
 * - tests/billing/forVolumes/SLP.spec.ts              (15-minute handling)
 * Neither existing suite exercises a slot that FAILS on date coverage  -- that is the
 * new ground covered here.
 *
 * Rule 0.8.1: this file lives under `Cursor-Project/EnergoTS/tests/` and is the only
 * place these payload shapes may be assembled. Payload generators outside `tests/`
 * are consumed as-is and never modified.
 */

import * as ExcelJS from 'exceljs';
import { expect, test } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  applyExpParityCombinedProduct,
  postExpParitySignedProductContract,
} from './shared/exp-parity-contract.fixtures';
import { parityAmount } from './shared/exp-parity-snapshot.fixtures';
import {
  applyExpParityLocalAddress,
  postExpParityLegalCustomer,
  type ExpParityLocalAddressIds,
} from './shared/exp-parity-customer.fixtures';

export const EXP_PARITY_02_KEY = 'EXP-PARITY-02';
export const EXP_PARITY_02_CASE_7_TITLE =
  'Fifteen-minute profile split into three periods by price component dates bills only the covered periods';
export const EXP_PARITY_02_CASE_2_TITLE =
  'Invoice slot whose price component dates miss the period fails without stopping the other slots';
export const EXP_PARITY_02_CASE_6_TITLE =
  'Billing run reaches DRAFT when invoice generation fails after successful data preparation';

/**
 * Which invoice slots get price component dates that do NOT cover the calculated period.
 *
 * The Slack source reads "some slot must fail, other than the first and the last",
 * while an earlier walkthrough noted slots one and two  -- flipping this constant
 * switches between the readings, and the behaviour under test (a failing slot must not
 * stop the others) is identical either way. Case 2 is driven entirely off this
 * constant; no slot name is hardcoded anywhere else.
 */
export const SLOTS_WITH_NON_COVERING_PC_DATES = ['INVOICE_TWO', 'INVOICE_THREE'] as const;

/** All four invoice slots, in `PriceComponentFormulaRequest.issuedSeparateInvoice` order. */
export const ALL_INVOICE_SLOTS = [
  'INVOICE_ONE',
  'INVOICE_TWO',
  'INVOICE_THREE',
  'INVOICE_FOUR',
] as const;

export type InvoiceSlot = (typeof ALL_INVOICE_SLOTS)[number];

export const BILLING_RUN_ROOT = 'billing-run';
/** Grid operator prompt reused from the SLR-18167 chain (get-or-create nomenclature). */
export const EXP_PARITY_GRID_OPERATOR = 'GIO';

/** Case 2 precondition: four scale codes on one meter  -- deliberately NOT the failure lever. */
export const EXP_PARITY_SCALE_CODE_PROMPTS = [
  'expParityScaleCodeA',
  'expParityScaleCodeB',
  'expParityScaleCodeC',
  'expParityScaleCodeD',
] as const;

/**
 * The billing pipeline is asynchronous on Experiment (a cron picks the run up, then
 * `standard_billing_stage_01..22` run through, with parallel workers on 05 / 09 / 12).
 * Budget the cron pickup plus the whole stage chain, and poll patiently.
 */
const DRAFT_POLL_MS = 15_000;
const DRAFT_MAX_MS = 18 * 60 * 1000;

/** Statuses that mean "still working" while polling for a terminal DRAFT. */
const IN_PROGRESS_STATUSES = new Set([
  '',
  'INITIAL',
  'NEW',
  'CREATED',
  'PROCESSING',
  'IN_PROGRESS',
  'IN_PROGRESS_DRAFT',
]);

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

export type ExpParity02Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'Nomenclatures'
>;

export type DateCoverage = {
  month: (typeof MONTH_ENUMS)[number];
  monthNumbers: string[];
};

export type MonthWindow = {
  /** `yyyy-mm-dd` first day of the month. */
  firstDay: string;
  /** `yyyy-mm-dd` last day of the month. */
  lastDay: string;
  /** `EditDateOfMonthRequest.month` enum for this window. */
  monthEnum: (typeof MONTH_ENUMS)[number];
  /** Number of days in the month. */
  dayCount: number;
};
export type GridNomenclatures = {
  gridId: number;
  measurementTypeId: number;
  scaleCodeIds: number[];
  scaleTariffId: number;
};

export type CustomerPodContract = {
  customerId: number;
  customerIdentifier: string;
  podId: number;
  podIdentifier: string;
  productId: number;
  contractId: number;
  contractNumber: string;
  scaleCodeIds: number[];
};

export type SlotErrorRow = {
  invoiceNumber: string;
  errorMessage: string;
};

export type InvoiceDetailRow = {
  priceComponent: string;
  periodFrom: string;
  periodTo: string;
  totalVolumes: number | null;
  unitPrice: number | null;
  value: number | null;
};

/* -------------------------------------------------------------------------- */
/* Small utilities                                                            */
/* -------------------------------------------------------------------------- */

export function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  throw new Error(`Expected numeric entity id, got: ${JSON.stringify(raw)}`);
}

function pad2(value: number): string {
  return String(value).padStart(2, '0');
}

/**
 * Month window relative to today. Relative (not hardcoded) months keep the spec valid
 * whenever it runs; because the Experiment and PreProd runs are launched close in time
 * they resolve to the same months on both sides, so the parity diff stays meaningful.
 *
 * The 15-minute Excel generator outside `tests/` is not DST-aware, so a window that
 * contains a DST switch (last Sunday of March / October) yields a slightly different
 * slot count. That affects BOTH environments identically and therefore does not
 * disturb the parity comparison.
 */
export function monthWindow(monthOffset: number): MonthWindow {
  const today = new Date();
  const first = new Date(today.getFullYear(), today.getMonth() + monthOffset, 1);
  const last = new Date(today.getFullYear(), today.getMonth() + monthOffset + 1, 0);
  return {
    firstDay: `${first.getFullYear()}-${pad2(first.getMonth() + 1)}-01`,
    lastDay: `${last.getFullYear()}-${pad2(last.getMonth() + 1)}-${pad2(last.getDate())}`,
    monthEnum: MONTH_ENUMS[first.getMonth()],
    dayCount: last.getDate(),
  };
}

/** `yyyy-mm-dd` for a given day inside a month window. */
export function dayInWindow(window: MonthWindow, day: number): string {
  return `${window.firstDay.slice(0, 8)}${pad2(day)}`;
}

/**
 * `EditDateOfMonthRequest` covering an inclusive day band of one month.
 * Listing only the wanted days makes every other day of that month uncovered, and
 * omitting a month entirely makes the whole month uncovered.
 */
export function dateCoverage(
  window: MonthWindow,
  firstDay: number,
  lastDay: number,
): DateCoverage {
  const monthNumbers: string[] = [];
  for (let day = firstDay; day <= lastDay; day++) {
    const label = DAY_OF_MONTH_ENUMS[day - 1];
    if (!label) {
      throw new Error(`Day ${day} is outside the monthNumbers enum (ONE..THIRTYONE).`);
    }
    monthNumbers.push(label);
  }
  return { month: window.monthEnum, monthNumbers };
}

/**
 * Strip run-specific digits (invoice numbers, entity ids, POD identifiers) so the same
 * logical error compares equal between Experiment and PreProd. The message SHAPE is
 * what the parity diff is about; the ids are volatile by construction.
 */
export function normalizeErrorMessage(raw: string): string {
  return String(raw ?? '')
    .replace(/\d+/g, '<n>')
    .replace(/\s+/g, ' ')
    .trim();
}

/* -------------------------------------------------------------------------- */
/* Preconditions                                                              */
/* -------------------------------------------------------------------------- */

/**
 * Nomenclatures shared by all three cases. `scaleCodeCount` drives how many distinct
 * scale codes are created  -- Case 2 asks for four; the other cases need one so the
 * meter is valid.
 */
export async function ensureExpParityNomenclatures(
  ctx: ExpParity02Fx,
  scaleCodeCount = 1,
): Promise<GridNomenclatures> {
  const { Nomenclatures } = ctx;
  await Nomenclatures.currency('BGN');
  await Nomenclatures.vat_rate();
  const gridId = await Nomenclatures.grid_operator(EXP_PARITY_GRID_OPERATOR);
  const measurementTypeId = await Nomenclatures.measurement_type(EXP_PARITY_GRID_OPERATOR, gridId);
  const scaleCodeIds: number[] = [];
  for (let index = 0; index < scaleCodeCount; index++) {
    const prompt = EXP_PARITY_SCALE_CODE_PROMPTS[index];
    if (!prompt) {
      throw new Error(`No scale code prompt configured for index ${index}.`);
    }
    scaleCodeIds.push(await Nomenclatures.scales_code(prompt));
  }

  const scaleTariffId = await Nomenclatures.scales_tariff(EXP_PARITY_GRID_OPERATOR);
  return { gridId, measurementTypeId, scaleCodeIds, scaleTariffId };
}

async function createLegalCustomer(
  ctx: ExpParity02Fx,
): Promise<{ id: number; identifier: string; addressIds: ExpParityLocalAddressIds }> {
  const created = await postExpParityLegalCustomer(ctx);
  return { id: created.id, identifier: created.identifier, addressIds: created.addressIds };
}

/**
 * SLP POD plus a meter carrying every scale code. Fifteen-minute billing data by
 * profile is uploaded against this POD; the scale codes stay inert (no
 * `billing-by-scales` volumes are posted) so they cannot contribute an error of their
 * own and cannot become the failure lever.
 */
async function createSlpPodWithMeter(
  ctx: ExpParity02Fx,
  grid: GridNomenclatures,
  addressIds: ExpParityLocalAddressIds,
): Promise<{ podId: number; podIdentifier: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const podPayload = GeneratePayload.pointsOfDelivery.pod_slp() as Record<string, unknown> & {
    identifier: string;
    gridOperatorId: number;
    measurementTypeId: number;
    type?: string;
    estimatedMonthlyAvgConsumption?: string | number;
  };
  applyExpParityLocalAddress(podPayload, addressIds);
  podPayload.gridOperatorId = grid.gridId;
  podPayload.measurementTypeId = grid.measurementTypeId;
  const podRes = await Request.post(Endpoints.pod, { data: podPayload });
  await expect(podRes).CheckResponse();
  const created = (await podRes.json()) as { id?: number; podDetailId?: number };
  const podId = asEntityId(created);
  const podIdentifier = String(podPayload.identifier ?? '').trim();
  expect(podIdentifier, 'POD identifier must be present on the create payload').toBeTruthy();
  Responses.pod.push({
    ...created,
    id: podId,
    identifier: podIdentifier,
    gridOperatorId: grid.gridId,
    estimatedMonthlyAvgConsumption: Number(podPayload.estimatedMonthlyAvgConsumption ?? 0),
    type: podPayload.type ?? 'CONSUMER',
  });

  const meterPayload = GeneratePayload.pointsOfDelivery.meters() as unknown as {
    gridOperatorId: number | null;
    podId: number | null;
    meterScales: number[];
  };
  meterPayload.gridOperatorId = grid.gridId;
  meterPayload.podId = podId;
  meterPayload.meterScales = [...grid.scaleCodeIds, grid.scaleTariffId];
  const meterRes = await Request.post(Endpoints.meters, { data: meterPayload });
  await expect(meterRes).CheckResponse();
  Responses.meters.push(asEntityId(await meterRes.json()));

  return { podId, podIdentifier };
}

/**
 * One `BY_SETTLEMENT_PERIODS` price component whose ONLY distinguishing feature is
 * `dateOfMonths`. Everything else is pinned so it cannot vary between components or
 * between environments:
 *   - `profiles`: the seeded profile at 100% (the stock generator randomises the
 *     percentage between 1 and 99, which would make amounts non-deterministic).
 *   - `formulaRequest.expression`: a fixed price (the stock generator randomises it,
 *     and can even produce 0).
 *   - `settlementPeriods`: the stock four minute ranges x ALL_HOURS, so every
 *     fifteen-minute slot of every covered day matches.
 */
export async function createDateCoveragePriceComponent(
  ctx: ExpParity02Fx,
  options: {
    displayName: string;
    expression: string;
    slot: InvoiceSlot;
    coverage: DateCoverage[];
  },
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = GeneratePayload.productAndServices.priceSettlement();

  payload.name = options.displayName;
  payload.displayName = options.displayName;
  payload.formulaRequest.expression = options.expression;
  payload.formulaRequest.issuedSeparateInvoice = options.slot;

  const settlement = payload.applicationModelRequest.settlementPeriodsRequest;
  settlement.profiles = [{ profileId: envVariables.profiles, percentage: 100 }];
  settlement.timeZone = 'CET';
  settlement.dateOfMonths = options.coverage;

  const res = await Request.post(Endpoints.priceComponent, { data: payload });
  await expect(res, `POST price component ${options.displayName}`).CheckResponse();
  const id = asEntityId(await res.json());
  Responses.priceComponent.push(id);
  return id;
}

export async function createProductWithPriceComponents(
  ctx: ExpParity02Fx,
  priceComponentIds: number[],
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const term = await Request.post(Endpoints.terms, {
    data: GeneratePayload.productAndServices.term(),
  });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  const productPayload = applyExpParityCombinedProduct(GeneratePayload.productAndServices.product());
  productPayload.priceComponentIds = priceComponentIds;
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  const productId = asEntityId(await product.json());
  Responses.product.push(productId);
  return productId;
}

export async function createContractAndActivatePod(
  ctx: ExpParity02Fx,
  activationDate: string,
): Promise<{ contractId: number; contractNumber: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
  contractPayload.basicParameters.contractNumber = null;

  const body = await postExpParitySignedProductContract(Request, contractPayload);
  const contractId = asEntityId(body);
  Responses.productContract.push(
    body && typeof body === 'object' ? { ...(body as object), id: contractId } : { id: contractId },
  );

  const contractGet = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractJson = (await contractGet.json()) as {
    basicParameters?: { contractNumber?: string };
  };
  const contractNumber = String(contractJson.basicParameters?.contractNumber ?? '');
  expect(contractNumber, 'API must assign a contract number').toBeTruthy();

  const podActivation = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(0, activationDate),
  });
  await expect(podActivation).CheckResponse();

  return { contractId, contractNumber };
}

/**
 * Fifteen-minute billing data by profile, present and complete across the WHOLE span.
 * The profile is deliberately never the missing thing in any of the three cases  -- only
 * the price component dates move.
 *
 * `profileId` is the seeded `envVariables.profiles`, the same profile referenced by
 * every price component built above, so profile matching always succeeds.
 */
export async function postFifteenMinuteProfile(
  ctx: ExpParity02Fx,
  options: {
    podIdentifier: string;
    periodFrom: string;
    periodTo: string;
    totalKwh: number;
  },
): Promise<{ bbpId: number; slotCount: number; perSlotKwh: number }> {
  const { Request, GeneratePayload, Responses } = ctx;

  const fromMs = Date.parse(`${options.periodFrom}T00:00:00Z`);
  const toMs = Date.parse(`${options.periodTo}T00:00:00Z`);
  const dayCount = Math.round((toMs - fromMs) / 86_400_000) + 1;
  const slotCount = dayCount * 24 * 4;
  const perSlotKwh = Number((options.totalKwh / slotCount).toFixed(10));

  const payload = (await GeneratePayload.energyData.profile15minute(0, [
    {
      startDate: `${options.periodFrom}T00:00:00`,
      endDate: `${options.periodTo}T23:45:00`,
      amount: perSlotKwh,
    },
  ])) as Record<string, unknown>;

  payload.identifier = options.podIdentifier;
  payload.timeZone = 'CET';
  payload.profileId = envVariables.profiles;
  payload.periodType = 'FIFTEEN_MINUTES';
  payload.periodFrom = `${options.periodFrom}T00:00:00.000Z`;
  payload.periodTo = `${options.periodTo}T23:45:00.000Z`;
  payload.warningAcceptedByUser = true;

  const res = await Request.post('billing-by-profile', { data: payload });
  await expect(res, 'POST billing-by-profile (FIFTEEN_MINUTES)').CheckResponse();
  const bbpId = Number(await res.json());
  await GeneratePayload.energyData.uploadDataByProfileFile(bbpId, 'MIN');
  Responses.dataByProfiles.push({
    id: bbpId,
    periodFrom: String(payload.periodFrom),
    periodTo: String(payload.periodTo),
    periodType: payload.periodType,
  });

  return { bbpId, slotCount, perSlotKwh };
}

/**
 * STANDARD_BILLING / FOR_VOLUMES / CONTRACT run over one contract number.
 * `maxEndDate` bounds the calculated period so each case bills exactly the span it set
 * up. Invoice / tax / accounting-period values stay on the generator defaults, which
 * point at the environment's seeded OPEN accounting period.
 */
export async function createForVolumesBillingRun(
  ctx: ExpParity02Fx,
  contractNumber: string,
  maxEndDate: string,
): Promise<{ billingRunId: number; billingPayload: Record<string, unknown> }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billingPayload = (await GeneratePayload.billing.billingRun('CONTRACT', [
    'FOR_VOLUMES',
  ])) as Record<string, unknown> & {
    billingType: string;
    basicParameters: Record<string, unknown>;
    commonParameters: Record<string, unknown>;
  };
  billingPayload.billingType = 'STANDARD_BILLING';
  billingPayload.basicParameters.applicationModelType = ['FOR_VOLUMES'];
  billingPayload.basicParameters.billingCriteria = 'LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS';
  billingPayload.basicParameters.billingApplicationLevel = 'CONTRACT';
  billingPayload.basicParameters.listOfCustomersContractsOrPOD = contractNumber;
  billingPayload.basicParameters.maxEndDate = maxEndDate;
  billingPayload.commonParameters.periodicity = 'STANDARD';
  billingPayload.commonParameters.executionType = 'MANUAL';
  billingPayload.commonParameters.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );
  return { billingRunId, billingPayload };
}

/* -------------------------------------------------------------------------- */
/* Observation                                                                */
/* -------------------------------------------------------------------------- */

export async function getBillingRunStatus(
  Request: ExpParity02Fx['Request'],
  billingRunId: number,
): Promise<string> {
  const res = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as { commonParameters?: { status?: string } };
  return String(body.commonParameters?.status ?? '');
}

export type DraftPollResult = {
  /** Last status observed. */
  status: string;
  /** True when the run settled on DRAFT within the budget. */
  reachedDraft: boolean;
  /** True when the budget expired while the run was still working. */
  stillInProgress: boolean;
  /** How long polling actually took, in whole seconds. */
  waitedSeconds: number;
};

/**
 * Start the run and poll `commonParameters.status` until it settles.
 *
 * Deliberately does NOT throw when the run is still in progress at the deadline  -- the
 * caller asserts on `reachedDraft` / `stillInProgress` so the failure message states
 * plainly that the run was stuck rather than surfacing as a generic timeout. A status
 * outside the known in-progress set and not DRAFT is a genuine surprise and does throw.
 */
export async function startBillingAndPollDraft(
  Request: ExpParity02Fx['Request'],
  billingRunId: number,
): Promise<DraftPollResult> {
  const startRes = await Request.patch(
    `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
  );
  await expect(startRes).CheckResponse();

  const startedAt = Date.now();
  const maxAttempts = Math.max(1, Math.ceil(DRAFT_MAX_MS / DRAFT_POLL_MS));
  let status = '';

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    status = await getBillingRunStatus(Request, billingRunId);
    if (status === 'DRAFT') {
      return {
        status,
        reachedDraft: true,
        stillInProgress: false,
        waitedSeconds: Math.round((Date.now() - startedAt) / 1000),
      };
    }
    if (!IN_PROGRESS_STATUSES.has(status)) {
      throw new Error(
        `[${EXP_PARITY_02_KEY}] billing run ${billingRunId} left start-billing with unexpected status=${status} (expected DRAFT).`,
      );
    }
    if (attempt < maxAttempts) {
      await new Promise((resolve) => setTimeout(resolve, DRAFT_POLL_MS));
    }
  }

  return {
    status,
    reachedDraft: false,
    stillInProgress: true,
    waitedSeconds: Math.round((Date.now() - startedAt) / 1000),
  };
}

export async function getDraftInvoiceIds(
  Request: ExpParity02Fx['Request'],
  billingRunId: number,
): Promise<number[]> {
  const res = await Request.get(
    `${BILLING_RUN_ROOT}/draft-invoices?id=${billingRunId}&page=0&size=50`,
  );
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Array<{ id?: number }> };
  return (body.content ?? [])
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

/**
 * `GET /invoice/detailed-data` returns one row per priced settlement period
 * (`InvoiceDetailedDataResponse`: priceComponent, periodFrom, periodTo, totalVolumes,
 * unitPrice, value). These rows ARE the `settlement_period` split as the API exposes it.
 */
export async function getInvoiceDetailRows(
  Request: ExpParity02Fx['Request'],
  invoiceId: number,
): Promise<InvoiceDetailRow[]> {
  const res = await Request.get(`invoice/detailed-data?id=${invoiceId}&page=0&size=100`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    content?: Array<Record<string, unknown>>;
  };
  return (body.content ?? []).map((row) => ({
    priceComponent: String(row.priceComponent ?? ''),
    periodFrom: String(row.periodFrom ?? '').slice(0, 10),
    periodTo: String(row.periodTo ?? '').slice(0, 10),
    totalVolumes: parityAmount(row.totalVolumes, 4),
    unitPrice: parityAmount(row.unitPrice, 4),
    value: parityAmount(row.value, 2),
  }));
}

/**
 * Billing error protocol as xlsx.
 *
 * `PATCH /billing-run/download-error-report/{id}?protocol=BILLING` (required `protocol`
 * query parameter, enum BILLING | ACCOUNTING). Phoenix writes the header row
 * `invoice_number` / `error_message` on row 1 and data from row 2 onward, and always
 * produces the workbook even when there are no error rows.
 *
 * The report is the only documented API surface for slot-level billing errors, so it is
 * how all three cases observe "the slot reported an error and the run carried on".
 */
export async function downloadBillingErrorRows(
  Request: ExpParity02Fx['Request'],
  billingRunId: number,
): Promise<SlotErrorRow[]> {
  const res = await Request.patch(
    `${BILLING_RUN_ROOT}/download-error-report/${billingRunId}?protocol=BILLING`,
  );
  await expect(res, 'PATCH billing-run/download-error-report?protocol=BILLING').CheckResponse();

  const buffer = await res.body();
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buffer as unknown as ExcelJS.Buffer);

  const sheet = workbook.worksheets[0];
  if (!sheet) {
    return [];
  }

  const rows: SlotErrorRow[] = [];
  sheet.eachRow((row, rowNumber) => {
    if (rowNumber === 1) {
      return;
    }
    const invoiceNumber = String(row.getCell(1).value ?? '').trim();
    const errorMessage = String(row.getCell(2).value ?? '').trim();
    if (invoiceNumber || errorMessage) {
      rows.push({ invoiceNumber, errorMessage });
    }
  });
  return rows;
}

/** Normalized, de-duplicated, sorted error shapes for the parity snapshot. */
export function normalizedErrorShapes(rows: SlotErrorRow[]): string[] {
  return [...new Set(rows.map((row) => normalizeErrorMessage(row.errorMessage)).filter(Boolean))].sort();
}

/**
 * Sum the `value` column of the detail rows belonging to one price component.
 * Returns null when the component produced no priced row at all, which is exactly the
 * observable signature of "this period / slot was not covered".
 */
export function amountForPriceComponent(
  rows: InvoiceDetailRow[],
  priceComponentName: string,
): number | null {
  const matching = rows.filter((row) => row.priceComponent === priceComponentName);
  if (!matching.length) {
    return null;
  }
  const total = matching.reduce((sum, row) => sum + (row.value ?? 0), 0);
  return parityAmount(total, 2);
}

/** Count of priced detail rows for one price component. */
export function rowCountForPriceComponent(
  rows: InvoiceDetailRow[],
  priceComponentName: string,
): number {
  return rows.filter((row) => row.priceComponent === priceComponentName).length;
}

/**
 * Collect the detail rows of every produced invoice into one flat list, so a case can
 * reason about "all priced settlement periods of this run" regardless of how the
 * engine distributed them across invoices.
 */
export async function collectAllDetailRows(
  Request: ExpParity02Fx['Request'],
  invoiceIds: number[],
): Promise<InvoiceDetailRow[]> {
  const all: InvoiceDetailRow[] = [];
  for (const invoiceId of invoiceIds) {
    all.push(...(await getInvoiceDetailRows(Request, invoiceId)));
  }
  return all;
}

/**
 * Shared precondition chain: nomenclatures, customer, POD + meter, price components,
 * product, contract, POD activation, fifteen-minute profile volumes.
 *
 * Called from inside a `test.step` in each test  -- there is deliberately no
 * `beforeAll` / `test.beforeAll` anywhere (Rule 40), so every test owns its own data.
 */
export async function runExpParityPrechain(
  ctx: ExpParity02Fx,
  options: {
    scaleCodeCount: number;
    priceComponents: Array<{
      displayName: string;
      expression: string;
      slot: InvoiceSlot;
      coverage: DateCoverage[];
    }>;
    podActivationDate: string;
    profileFrom: string;
    profileTo: string;
    profileTotalKwh: number;
  },
): Promise<CustomerPodContract & { priceComponentIds: number[]; bbpId: number; slotCount: number }> {
  const grid = await test.step('Precondition: nomenclatures (grid operator, measurement type, scale codes, tariff, VAT, currency)', async () =>
    ensureExpParityNomenclatures(ctx, options.scaleCodeCount),
  );

  const customer = await test.step('Precondition: legal customer', async () => createLegalCustomer(ctx));

  const priceComponentIds: number[] = [];
  await test.step('Precondition: price components differing only in dateOfMonths coverage', async () => {
    for (const component of options.priceComponents) {
      priceComponentIds.push(await createDateCoveragePriceComponent(ctx, component));
    }
  });

  const productId = await test.step('Precondition: COMBINED product carrying the price components', async () =>
    createProductWithPriceComponents(ctx, priceComponentIds),
  );

  const pod = await test.step('Precondition: SLP POD with meter carrying every scale code', async () =>
    createSlpPodWithMeter(ctx, grid, customer.addressIds),
  );

  const contract = await test.step('Precondition: product contract and POD activation', async () =>
    createContractAndActivatePod(ctx, options.podActivationDate),
  );

  const profile = await test.step('Precondition: complete FIFTEEN_MINUTES profile volumes across the whole span', async () =>
    postFifteenMinuteProfile(ctx, {
      podIdentifier: pod.podIdentifier,
      periodFrom: options.profileFrom,
      periodTo: options.profileTo,
      totalKwh: options.profileTotalKwh,
    }),
  );

  return {
    customerId: customer.id,
    customerIdentifier: customer.identifier,
    podId: pod.podId,
    podIdentifier: pod.podIdentifier,
    productId,
    contractId: contract.contractId,
    contractNumber: contract.contractNumber,
    scaleCodeIds: grid.scaleCodeIds,
    priceComponentIds,
    bbpId: profile.bbpId,
    slotCount: profile.slotCount,
  };
}
