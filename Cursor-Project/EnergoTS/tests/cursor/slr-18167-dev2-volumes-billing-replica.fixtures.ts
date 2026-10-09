/**
 * [SLR-18167] — Dev2 FOR_VOLUMES replica of Test 2 SLR billing run 18167
 * (`BILLING202608200001`). Shape replica only: new entity IDs, no SQL clone,
 * no SLR runtime read.
 *
 * Environment: Dev2
 * - API BASE_URL (do not edit playwright.config.ts):
 *   https://devapps.energo-pro.bg/backend/phoenix2-dev
 *   Current EnergoTS playwright.config.ts default already uses that URL.
 * - Portal: https://devapps.energo-pro.bg/app/phoenix2-dev
 *
 * Identifier strategy:
 * - Customer 206236269 — TRY on create (foreign=false, uppercase names).
 *   Retry only on HTTP 409, or 400/422 whose body indicates duplicate/already
 *   exists (not Invalid Format). Fallback identifier is a fresh
 *   customer_legal().customerIdentifier (never generateUniqueIdentifier()).
 *   customer_legal() identifiers are valid for foreign=true; retry sets that.
 * - POD 32Z410109903225Z — same collision rule; fallback pod_slp().identifier.
 * - Contract K12608000004 ALREADY EXISTS on Dev2 (id 27144, different POD,
 *   COMPLETED volumes run 21507). NEVER set that number. Leave
 *   product_contract.basicParameters.contractNumber null so the API assigns
 *   a new number.
 *
 * Volume window (SLR 18167):
 * - Scale: 2026-02-01 .. 2026-02-28, 6.0000 kWh
 * - Profile: 2026-03-01 .. 2026-03-31, FIFTEEN_MINUTES SLP, ~6 kWh total
 *   via profile15minute date-range Excel + uploadDataByProfileFile('MIN')
 *
 * Billing (SLR 18167): STANDARD_BILLING, FOR_VOLUMES, LIST of contracts,
 * CONTRACT application level, invoiceDate 2026-07-24, taxEventDate 2026-07-23,
 * maxEndDate 2026-04-30, execution MANUAL, due date ACCORDING_TO_THE_CONTRACT.
 * July 2026 dates are applied only when an OPEN accounting period covers them;
 * otherwise generator dates are kept (no closed-period posts).
 *
 * Start-billing only (no start-generating / start-accounting) — stay DRAFT.
 *
 * Reference spec(s):
 * - tests/billing/forVolumes/forVolumes.spec.ts (REG-713 / REG-873 / REG-962)
 * - tests/cursor/dev-volume-billing-two-compensations.spec.ts + .fixtures.ts
 * - tests/cursor/pdt-3223-invoice-json-tablepcscales-listpc-order.fixtures.ts
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts
 *
 * Swagger (dev2, Cursor-Project/config/swagger/dev2/swagger-spec.json):
 * POST /customer, /pod, /products, /product-contract, /contract-pods/manual,
 * /billing-by-profile, /billing-by-scales (Endpoints.dataByScales),
 * /price-components, /terms, /billing-run, /meters
 * PATCH /billing-run/start-billing?billingRunId=
 * GET /billing-run/{id}, /billing-run/draft-invoices?id=
 *
 * Header notes vs spec:
 * - GET /billing-run/draft-invoices documents a required `request` query
 *   object (BillingRunInvoiceListingRequest). Existing EnergoTS tests use
 *   `id`, `page`, `size` only — same as DEV-DATA.
 * - BillingByProfileCreateRequest has no `timeZone`; REG-962 / DEV-DATA still
 *   set CET on the payload.
 * - PodCreateRequest.systemSource is documented for Sales Portal create;
 *   Phoenix pod_slp() omits it (same as REG-873 / PDT-3223).
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { scaleCode as scaleCodePayloadTemplate } from '../../jsons/payloads/create/energyData/dataByScales(ScaleCode)';
import { postPdt2915BillingByProfile } from './pdt-2915-invoice-correction-deleted-bbp.fixtures';

export const SLR_18167_KEY = 'SLR-18167';
export const SLR_18167_TITLE =
  'Dev2 FOR_VOLUMES replica of Test 2 SLR billing run 18167 (BILLING202608200001) — start-billing draft invoices';

/** Dev2 portal (ResponseLinker maps phoenix2-dev API here). No trailing slash. */
export const SLR_18167_PORTAL_BASE = 'https://devapps.energo-pro.bg/app/phoenix2-dev';
export const SLR_18167_API_BASE = 'https://devapps.energo-pro.bg/backend/phoenix2-dev';

/** SLR source identifiers — TRY on create; never reuse contract K12608000004. */
export const SLR_PREFERRED_CUSTOMER_IDENTIFIER = '206236269';
export const SLR_PREFERRED_POD_IDENTIFIER = '32Z410109903225Z';
/** Phoenix @Pattern on businessCustomerDetails.name / nameTranslated is uppercase-only. */
export const SLR_CUSTOMER_LEGAL_NAME = 'SLR-18167 REPLICA OOD';
/** Documented collision — do not POST this contract number. */
export const SLR_COLLIDING_CONTRACT_NUMBER = 'K12608000004';

export const SLR_INVOICE_DATE = '2026-07-24';
export const SLR_TAX_EVENT_DATE = '2026-07-23';
export const SLR_MAX_END_DATE = '2026-04-30';

export const SLR_SCALE_FROM = '2026-02-01';
export const SLR_SCALE_TO = '2026-02-28';
export const SLR_PROFILE_FROM = '2026-03-01';
export const SLR_PROFILE_TO = '2026-03-31';
export const SLR_TARGET_KWH = 6;
export const SLR_POD_ACTIVATION_DATE = '2026-02-01';

export const SLR_GRID_OPERATOR = 'GIO';
export const BILLING_RUN_ROOT = 'billing-run';

const DRAFT_POLL_MS = 15_000;
const DRAFT_MAX_MS = 10 * 60 * 1000;
const ZERO_DRAFT_INVOICES_MESSAGE =
  'SLR-18167 replica: start-billing finished with 0 draft invoices (same symptom as SLR run 18167).';

/** Naive 15-min slots for March 2026 (generator is not DST-aware). 31 * 96 = 2976. */
const MARCH_15MIN_SLOT_COUNT = 31 * 24 * 4;

export type Slr18167Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'Nomenclatures'
>;

export type Slr18167BillingDates = {
  datesAdapted: boolean;
  invoiceDate: string | null;
  taxEventDate: string | null;
  maxEndDate: string;
  accountingPeriodId: number | null;
  openPeriodName: string | null;
};

export type Slr18167PrechainResult = {
  customerId: number;
  customerIdentifier: string;
  customerIdentifierPreferredUsed: boolean;
  podId: number;
  podIdentifier: string;
  podIdentifierPreferredUsed: boolean;
  productId: number;
  contractId: number;
  contractNumber: string;
  scaleDataId: number;
  bbpId: number;
  priceComponentIds: number[];
};

export type Slr18167ScenarioResult = Slr18167PrechainResult & {
  billingRunId: number;
  billingRunStatus: string;
  draftInvoiceIds: number[];
  datesAdapted: boolean;
};

type ScaleJson = {
  id?: number;
  scaleType?: string | null;
  scaleCode?: string | null;
  tariffOrScale?: string | null;
};

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

function ymd(value: string): string {
  return String(value ?? '').slice(0, 10);
}

function isDuplicateIdentifierMessage(bodyText: string): boolean {
  const lower = bodyText.toLowerCase();
  if (/invalid format/.test(lower)) {
    return false;
  }
  return (
    lower.includes('already exist') ||
    lower.includes('duplicate') ||
    lower.includes('exists with this identifier')
  );
}

/**
 * Retry create only on HTTP 409, or 400/422 whose body is a duplicate identifier
 * (not bean-validation "Invalid Format" / checksum "invalid format or symbols").
 */
async function isIdentifierCollisionResponse(res: {
  status(): number;
  text(): Promise<string>;
}): Promise<boolean> {
  const status = res.status();
  if (status === 409) {
    return true;
  }
  if (status !== 400 && status !== 422) {
    return false;
  }
  const bodyText = await res.text().catch(() => '');
  return isDuplicateIdentifierMessage(bodyText);
}

export function buildSlr18167CustomerPreviewLink(customerId: number): string {
  return `${SLR_18167_PORTAL_BASE}/customers/preview/basic?id=${customerId}`;
}

export function buildSlr18167PodPreviewLink(podId: number): string {
  return `${SLR_18167_PORTAL_BASE}/points-of-delivery/preview?id=${podId}`;
}

export function buildSlr18167BillingRunPreviewLink(billingRunId: number): string {
  return `${SLR_18167_PORTAL_BASE}/billing-run/preview/basic-parameters?id=${billingRunId}`;
}

export function buildSlr18167InvoicePreviewLink(invoiceId: number): string {
  return `${SLR_18167_PORTAL_BASE}/billing-run/invoices/preview/basic-parameters?id=${invoiceId}`;
}

function mapAccountingPeriodRows(
  rows: Array<Record<string, unknown>>,
  fallbackStatus: string,
): Array<{ id: number; name: string; startDate: string; endDate: string; status: string }> {
  return rows
    .map((row) => ({
      id: Number(row.accountPeriodId ?? row.id),
      name: String(row.name ?? ''),
      startDate: ymd(String(row.startDate ?? '')),
      endDate: ymd(String(row.endDate ?? '')),
      status: String(row.status ?? fallbackStatus),
    }))
    .filter((r) => Number.isFinite(r.id) && r.id > 0);
}

/**
 * Prefer an OPEN period covering SLR invoice/tax dates (July 2026).
 * Does not PUT/open closed periods.
 */
export async function resolveSlr18167BillingDates(
  Request: Slr18167Fx['Request'],
): Promise<Slr18167BillingDates> {
  let open: ReturnType<typeof mapAccountingPeriodRows> = [];

  const listed = await Request.get(
    'accounting-period?page=0&size=100&status=OPEN&sortBy=END_DATE&direction=DESC',
  );
  if (listed.status() === 200) {
    await expect(listed).CheckResponse();
    const page = (await listed.json()) as { content?: Array<Record<string, unknown>> };
    open = mapAccountingPeriodRows(page.content ?? [], 'OPEN');
  } else {
    const available = await Request.get('billing-run/accounting-period-available-list?page=0&size=50');
    if (available.status() === 200) {
      await expect(available).CheckResponse();
      const page = (await available.json()) as { content?: Array<Record<string, unknown>> };
      open = mapAccountingPeriodRows(page.content ?? [], 'OPEN');
    }
  }

  const july = open.find(
    (p) => p.startDate <= SLR_TAX_EVENT_DATE && p.endDate >= SLR_INVOICE_DATE,
  );

  if (july) {
    return {
      datesAdapted: false,
      invoiceDate: SLR_INVOICE_DATE,
      taxEventDate: SLR_TAX_EVENT_DATE,
      maxEndDate: SLR_MAX_END_DATE,
      accountingPeriodId: july.id,
      openPeriodName: july.name || `${july.startDate}..${july.endDate}`,
    };
  }

  return {
    datesAdapted: true,
    invoiceDate: null,
    taxEventDate: null,
    maxEndDate: SLR_MAX_END_DATE,
    accountingPeriodId: null,
    openPeriodName: null,
  };
}

async function ensureGridNomenclatures(ctx: Slr18167Fx): Promise<{
  gridId: number;
  measurementTypeId: number;
  scaleCodeId: number;
  scaleTariffId: number;
}> {
  const { Nomenclatures } = ctx;
  await Nomenclatures.currency('BGN');
  await Nomenclatures.vat_rate();
  const gridId = await Nomenclatures.grid_operator(SLR_GRID_OPERATOR);
  const measurementTypeId = await Nomenclatures.measurement_type(SLR_GRID_OPERATOR, gridId);
  const scaleCodeId = await Nomenclatures.scales_code('scaleCode');
  const scaleTariffId = await Nomenclatures.scales_tariff(SLR_GRID_OPERATOR);
  return { gridId, measurementTypeId, scaleCodeId, scaleTariffId };
}

async function postPriceComponent(ctx: Slr18167Fx, payload: object): Promise<number> {
  const { Request, Responses, Endpoints } = ctx;
  const res = await Request.post(Endpoints.priceComponent, { data: payload });
  await expect(res).CheckResponse();
  const id = asEntityId(await res.json());
  Responses.priceComponent.push(id);
  return id;
}

async function createSettlementPriceComponent(
  ctx: Slr18167Fx,
  displayName: string,
  expression: string,
): Promise<number> {
  const payload = ctx.GeneratePayload.productAndServices.priceSettlement();
  payload.name = displayName;
  payload.displayName = displayName;
  payload.formulaRequest.expression = expression;
  payload.applicationModelRequest.settlementPeriodsRequest.profiles = [
    { profileId: envVariables.profiles, percentage: 100 },
  ];
  return postPriceComponent(ctx, payload);
}

async function createScalePriceComponent(
  ctx: Slr18167Fx,
  displayName: string,
  expression: string,
  scaleId: number,
): Promise<number> {
  const payload = ctx.GeneratePayload.productAndServices.scaleComponent();
  payload.name = displayName;
  payload.displayName = displayName;
  payload.formulaRequest.expression = expression;
  payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
  payload.applicationModelRequest.applicationType = 'BY_SCALES';
  payload.applicationModelRequest.volumesByScaleRequest.scaleIds = [scaleId];
  return postPriceComponent(ctx, payload);
}

async function createCustomerTryPreferred(ctx: Slr18167Fx): Promise<{
  customerId: number;
  customerIdentifier: string;
  preferredUsed: boolean;
}> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = ctx;
  const payload = GeneratePayload.customers.customer_legal();
  payload.customerIdentifier = SLR_PREFERRED_CUSTOMER_IDENTIFIER;
  payload.foreign = false;
  payload.businessCustomerDetails.name = SLR_CUSTOMER_LEGAL_NAME;
  payload.businessCustomerDetails.nameTranslated = SLR_CUSTOMER_LEGAL_NAME;
  const legalFormId = await Nomenclatures.legal_forms('ООД');
  if (legalFormId) {
    payload.businessCustomerDetails.legalFormId = legalFormId;
    payload.businessCustomerDetails.legalFormTransId = legalFormId;
  }

  let preferredUsed = true;
  let res = await Request.post(Endpoints.customer, { data: payload });
  if (await isIdentifierCollisionResponse(res)) {
    preferredUsed = false;
    const fallback = GeneratePayload.customers.customer_legal();
    payload.customerIdentifier = fallback.customerIdentifier;
    // customer_legal() identifier is a unique numeric string accepted when foreign=true
    // (LegalEntityValidationTest: non-foreign UIC must be checksummed 9/13).
    payload.foreign = true;
    res = await Request.post(Endpoints.customer, { data: payload });
  }
  await expect(res).CheckResponse();
  const raw = await res.json();
  const customerId = asEntityId(raw);
  const customerIdentifier = String(payload.customerIdentifier);
  Responses.customer.push(
    raw && typeof raw === 'object'
      ? { ...(raw as object), id: customerId, identifier: customerIdentifier, customerIdentifier }
      : { id: customerId, identifier: customerIdentifier, customerIdentifier },
  );
  return { customerId, customerIdentifier, preferredUsed };
}

async function createSlpPodWithMeter(
  ctx: Slr18167Fx,
  grid: { gridId: number; measurementTypeId: number; scaleCodeId: number; scaleTariffId: number },
): Promise<{ podId: number; podIdentifier: string; preferredUsed: boolean }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const podPayload = GeneratePayload.pointsOfDelivery.pod_slp() as Record<string, unknown> & {
    identifier: string;
    gridOperatorId: number;
    measurementTypeId: number;
    type?: string;
    estimatedMonthlyAvgConsumption?: string | number;
  };
  podPayload.identifier = SLR_PREFERRED_POD_IDENTIFIER;
  podPayload.gridOperatorId = grid.gridId;
  podPayload.measurementTypeId = grid.measurementTypeId;

  let preferredUsed = true;
  let podRes = await Request.post(Endpoints.pod, { data: podPayload });
  if (await isIdentifierCollisionResponse(podRes)) {
    preferredUsed = false;
    const fallback = GeneratePayload.pointsOfDelivery.pod_slp();
    podPayload.identifier = String(fallback.identifier);
    podRes = await Request.post(Endpoints.pod, { data: podPayload });
  }
  await expect(podRes).CheckResponse();
  const created = (await podRes.json()) as { id?: number; podDetailId?: number };
  const podId = asEntityId(created);
  const podIdentifier = String(podPayload.identifier ?? '').trim();
  expect(podIdentifier, 'POD identifier must be present on create payload').toBeTruthy();
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
  meterPayload.meterScales = [grid.scaleCodeId, grid.scaleTariffId];
  const meterRes = await Request.post(Endpoints.meters, { data: meterPayload });
  await expect(meterRes).CheckResponse();
  Responses.meters.push(asEntityId(await meterRes.json()));

  return { podId, podIdentifier, preferredUsed };
}

async function loadMeterScaleCode(
  Request: Slr18167Fx['Request'],
  meterId: number,
): Promise<{ meterNumber: string; scaleCode: ScaleJson }> {
  const meterGet = await Request.get(`meters/${meterId}`);
  await expect(meterGet).CheckResponse();
  const meterJson = (await meterGet.json()) as {
    number?: string;
    meterScales?: Array<{ id?: number }>;
  };

  const scaleDetails: ScaleJson[] = [];
  for (const meterScale of meterJson.meterScales ?? []) {
    const scaleId = meterScale?.id;
    if (scaleId == null) continue;
    const scaleGet = await Request.get(`scales/${scaleId}`);
    await expect(scaleGet, `GET scales/${scaleId}`).CheckResponse();
    scaleDetails.push((await scaleGet.json()) as ScaleJson);
  }

  const scaleCodeJson =
    scaleDetails.find((s) => Boolean(s.scaleCode) && !s.tariffOrScale) ??
    scaleDetails.find((s) => Boolean(s.scaleCode));
  if (!scaleCodeJson?.scaleCode) {
    throw new Error(
      `SLR-18167: meter ${meterId} has no code scale with scaleCode (scales=${JSON.stringify(scaleDetails)})`,
    );
  }

  const meterNumber = String(meterJson.number ?? '');
  expect(meterNumber, 'meter number from GET /meters/{id}').toBeTruthy();
  return { meterNumber, scaleCode: scaleCodeJson };
}

async function postFebruaryScale6kWh(
  ctx: Slr18167Fx,
  podIdentifier: string,
): Promise<number> {
  const { Request, Responses, Endpoints } = ctx;
  const meterId = Responses.meters[0] as number;
  const { meterNumber, scaleCode } = await loadMeterScaleCode(Request, meterId);

  const payload = scaleCodePayloadTemplate() as Record<string, unknown>;
  payload.identifier = podIdentifier;
  payload.dateFrom = SLR_SCALE_FROM;
  payload.dateTo = SLR_SCALE_TO;
  payload.invoiceDate = `${SLR_SCALE_FROM}T00:00:00.000Z`;
  payload.invoiceNumber = '1';
  payload.correction = false;
  payload.invoiceCorrection = null;
  payload.numberOfDays = 28;

  const rows = payload.billingByScalesTableCreateRequests as Array<Record<string, unknown>>;
  rows[0].periodFrom = SLR_SCALE_FROM;
  rows[0].periodTo = SLR_SCALE_TO;
  rows[0].meterNumber = meterNumber;
  rows[0].scaleCode = scaleCode.scaleCode;
  rows[0].scaleType = scaleCode.scaleType;
  rows[0].oldMeterReading = '0';
  rows[0].newMeterReading = '6';
  rows[0].difference = '6';
  rows[0].multiplier = '1';
  rows[0].totalVolumes = '6.0000';

  const res = await Request.post(Endpoints.dataByScales, { data: payload });
  await expect(res, 'SLR-18167 POST billing-by-scales (Feb 6 kWh)').CheckResponse();
  const scaleDataId = Number(await res.json());
  Responses.dataByScales.push(scaleDataId);
  return scaleDataId;
}

async function postMarchBbp6kWh(ctx: Slr18167Fx, podIdentifier: string): Promise<number> {
  const { Request, GeneratePayload, Responses } = ctx;
  const perSlot = Number((SLR_TARGET_KWH / MARCH_15MIN_SLOT_COUNT).toFixed(10));
  const payload = (await GeneratePayload.energyData.profile15minute(0, [
    {
      startDate: `${SLR_PROFILE_FROM}T00:00:00`,
      endDate: `${SLR_PROFILE_TO}T23:45:00`,
      amount: perSlot,
    },
  ])) as Record<string, unknown>;
  payload.identifier = podIdentifier;
  payload.timeZone = 'CET';
  payload.profileId = envVariables.profiles;
  payload.periodType = 'FIFTEEN_MINUTES';
  payload.periodFrom = `${SLR_PROFILE_FROM}T00:00:00.000Z`;
  payload.periodTo = `${SLR_PROFILE_TO}T23:45:00.000Z`;
  payload.warningAcceptedByUser = true;

  const bbp = await postPdt2915BillingByProfile(Request, payload, 'SLR-18167 March BBP');
  await GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');
  Responses.dataByProfiles.push({
    id: bbp.id,
    periodFrom: String(payload.periodFrom),
    periodTo: String(payload.periodTo),
    periodType: payload.periodType,
  });
  return bbp.id;
}

/** REG-873-style SLP chain + Feb scale 6 kWh + Mar 15-min BBP ~6 kWh. */
export async function runSlr18167ContractScaleAndBbpPrechain(
  ctx: Slr18167Fx,
): Promise<Slr18167PrechainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const grid = await test.step('Precondition: nomenclatures (grid / scales / VAT / currency)', async () =>
    ensureGridNomenclatures(ctx),
  );

  const customer = await test.step('Precondition: legal customer (try SLR identifier)', async () =>
    createCustomerTryPreferred(ctx),
  );

  await test.step('Precondition: terms', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  const priceComponentIds: number[] = [];
  await test.step('Precondition: settlement PCs (energy, excise, obligation-to-society)', async () => {
    priceComponentIds.push(
      await createSettlementPriceComponent(ctx, 'SLR-18167 Energy', '10'),
    );
    priceComponentIds.push(
      await createSettlementPriceComponent(ctx, 'SLR-18167 Excise', '0.02'),
    );
    priceComponentIds.push(
      await createSettlementPriceComponent(ctx, 'SLR-18167 Obligation to society', '0.037'),
    );
  });

  await test.step('Precondition: three network scale PCs', async () => {
    priceComponentIds.push(
      await createScalePriceComponent(ctx, 'SLR-18167 Network 1', '1', grid.scaleCodeId),
    );
    priceComponentIds.push(
      await createScalePriceComponent(ctx, 'SLR-18167 Network 2', '2', grid.scaleCodeId),
    );
    priceComponentIds.push(
      await createScalePriceComponent(ctx, 'SLR-18167 Network 3', '3', grid.scaleCodeId),
    );
  });

  let productId = 0;
  await test.step('Precondition: COMBINED product', async () => {
    const productPayload = GeneratePayload.productAndServices.product();
    productPayload.contractTypes = ['COMBINED'];
    productPayload.priceComponentIds = priceComponentIds;
    const product = await Request.post(Endpoints.product, { data: productPayload });
    await expect(product).CheckResponse();
    productId = asEntityId(await product.json());
    Responses.product.push(productId);
  });

  const pod = await test.step('Precondition: SLP POD + meter (try SLR identifier)', async () =>
    createSlpPodWithMeter(ctx, grid),
  );

  let contractId = 0;
  let contractNumber = '';
  await test.step('Precondition: product contract (new number — never K12608000004)', async () => {
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
    contractPayload.basicParameters.contractNumber = null;
    contractPayload.productParameters.contractType = 'COMBINED';
    let monthlySum = 0;
    for (const row of Responses.pod) {
      const podRow = row as { type?: string; estimatedMonthlyAvgConsumption?: number };
      if ((podRow.type ?? 'CONSUMER') === 'CONSUMER') {
        monthlySum += Number(podRow.estimatedMonthlyAvgConsumption ?? 0);
      }
    }
    contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh =
      (monthlySum * 12) / 1000;

    const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    contractId = asEntityId(body);
    Responses.productContract.push(
      body && typeof body === 'object' ? { ...(body as object), id: contractId } : { id: contractId },
    );

    const contractGet = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
    await expect(contractGet).CheckResponse();
    const contractJson = (await contractGet.json()) as {
      basicParameters?: { contractNumber?: string };
    };
    contractNumber = String(contractJson.basicParameters?.contractNumber ?? '');
    expect(contractNumber, 'API must assign a new contract number').toBeTruthy();
    expect(
      contractNumber,
      `Must not reuse colliding Dev2 contract ${SLR_COLLIDING_CONTRACT_NUMBER}`,
    ).not.toBe(SLR_COLLIDING_CONTRACT_NUMBER);
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, SLR_POD_ACTIVATION_DATE),
    });
    await expect(podActivation).CheckResponse();
  });

  const scaleDataId = await test.step('Precondition: Feb 2026 scale 6.0000 kWh', async () =>
    postFebruaryScale6kWh(ctx, pod.podIdentifier),
  );

  const bbpId = await test.step(
    'Precondition: Mar 2026 15-minute BBP (~6 kWh via MIN upload)',
    async () => postMarchBbp6kWh(ctx, pod.podIdentifier),
  );

  return {
    customerId: customer.customerId,
    customerIdentifier: customer.customerIdentifier,
    customerIdentifierPreferredUsed: customer.preferredUsed,
    podId: pod.podId,
    podIdentifier: pod.podIdentifier,
    podIdentifierPreferredUsed: pod.preferredUsed,
    productId,
    contractId,
    contractNumber,
    scaleDataId,
    bbpId,
    priceComponentIds,
  };
}

export async function createSlr18167ForVolumesBillingRun(
  ctx: Slr18167Fx,
  contractNumber: string,
  dates: Slr18167BillingDates,
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
  billingPayload.basicParameters.maxEndDate = dates.maxEndDate;
  billingPayload.commonParameters.executionType = 'MANUAL';
  billingPayload.commonParameters.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
  billingPayload.commonParameters.sendingAnInvoice = 'ACCORDING_TO_THE_CONTRACT';
  billingPayload.commonParameters.periodicity = 'STANDARD';
  if (dates.invoiceDate) {
    billingPayload.commonParameters.invoiceDate = dates.invoiceDate;
  }
  if (dates.taxEventDate) {
    billingPayload.commonParameters.taxEventDate = dates.taxEventDate;
  }
  if (dates.accountingPeriodId) {
    billingPayload.commonParameters.accountingPeriodId = dates.accountingPeriodId;
  }

  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );
  return { billingRunId, billingPayload };
}

export async function startBillingAndPollDraft(
  Request: Slr18167Fx['Request'],
  billingRunId: number,
): Promise<string> {
  const startRes = await Request.patch(
    `${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`,
  );
  await expect(startRes).CheckResponse();

  const waiting = new Set(['', 'INITIAL', 'IN_PROGRESS_DRAFT', 'IN_PROGRESS', 'PROCESSING', 'NEW', 'CREATED']);
  const maxAttempts = Math.max(1, Math.ceil(DRAFT_MAX_MS / DRAFT_POLL_MS));
  let status = '';
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    const statusRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
    await expect(statusRes).CheckResponse();
    const body = (await statusRes.json()) as { commonParameters?: { status?: string } };
    status = String(body.commonParameters?.status ?? '');
    if (status === 'DRAFT') {
      return status;
    }
    if (!waiting.has(status) && status !== 'IN_PROGRESS_DRAFT') {
      throw new Error(
        `SLR-18167 replica: billing run ${billingRunId} left start-billing with status=${status} (expected DRAFT).`,
      );
    }
    if (attempt < maxAttempts) {
      await new Promise((r) => setTimeout(r, DRAFT_POLL_MS));
    }
  }
  throw new Error(
    `SLR-18167 replica: billing run ${billingRunId} did not reach DRAFT within ${DRAFT_MAX_MS}ms; final status=${status}`,
  );
}

export async function getDraftInvoiceIds(
  Request: Slr18167Fx['Request'],
  billingRunId: number,
): Promise<number[]> {
  const draftRes = await Request.get(
    `${BILLING_RUN_ROOT}/draft-invoices?id=${billingRunId}&page=0&size=25`,
  );
  await expect(draftRes).CheckResponse();
  const draftBody = (await draftRes.json()) as { content?: Array<{ id?: number }> };
  return (draftBody.content ?? [])
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

export function slr18167ZeroDraftInvoicesMessage(): string {
  return ZERO_DRAFT_INVOICES_MESSAGE;
}

export function logSlr18167CreatedEntities(result: Slr18167ScenarioResult): void {
  console.log('\n========== SLR-18167: Dev2 volumes billing replica (draft only) ==========');
  console.log(
    JSON.stringify(
      {
        customerId: result.customerId,
        customerIdentifier: result.customerIdentifier,
        customerIdentifierPreferredUsed: result.customerIdentifierPreferredUsed,
        podId: result.podId,
        podIdentifier: result.podIdentifier,
        podIdentifierPreferredUsed: result.podIdentifierPreferredUsed,
        productId: result.productId,
        contractId: result.contractId,
        contractNumber: result.contractNumber,
        scaleDataId: result.scaleDataId,
        bbpId: result.bbpId,
        billingRunId: result.billingRunId,
        billingRunStatus: result.billingRunStatus,
        draftInvoiceIds: result.draftInvoiceIds,
        datesAdapted: result.datesAdapted,
        portal: {
          customer: buildSlr18167CustomerPreviewLink(result.customerId),
          pod: buildSlr18167PodPreviewLink(result.podId),
          contract: `${SLR_18167_PORTAL_BASE}/energy-product-contracts/preview/basic-parameters?id=${result.contractId}`,
          billingRun: buildSlr18167BillingRunPreviewLink(result.billingRunId),
          invoices: result.draftInvoiceIds.map(buildSlr18167InvoicePreviewLink),
        },
      },
      null,
      2,
    ),
  );
}
