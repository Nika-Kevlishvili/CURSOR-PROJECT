/**
 * PDT-3223 — invoice JSON `DD[].TablePCScales[].ListPC[].PC` ordered by price component
 * **Name ASC**, then **Id ASC** (not Id-only, not displayName ASC).
 *
 * Data design (create order so Id ASC ≠ Name ASC; two PCs share the same Name to prove Id tie-break):
 * | Create | name              | displayName |
 * |--------|-------------------|-------------|
 * | 1      | ZZZ-PDT3223-{tok} | AAAA-DISP   |
 * | 2      | MMM-PDT3223-{tok} | BBBB-DISP   |
 * | 3      | AAA-PDT3223-{tok} | DDDD-DISP   |
 * | 4      | AAA-PDT3223-{tok} | CCCC-DISP   |
 *
 * Expected ListPC[].PC (displayName) after Name ASC + Id ASC:
 * DDDD-DISP → CCCC-DISP → BBBB-DISP → AAAA-DISP
 *
 * Chain (SCALE — fills TablePCScales, not TablePCProfiles):
 * legal customer → 4 scale PCs (`scaleComponent` + scaleIds) → terms → pod_slp + meter
 * → product (direct `priceComponentIds`, no group) → product contract + POD activation
 * → POST dataByScales (whole current month) → FOR_VOLUMES → GET /billing-run/generate-invoice-data.
 *
 * Reference specs:
 * - tests/cursor/pdt-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts
 * - tests/billing/forVolumes/forVolumes.spec.ts (scale POD + meter + billing-by-scales)
 * - tests/cursor/dev-volume-invoice-custom-template.fixtures.ts (resolvePortalBase)
 *
 * Swagger (dev): GET /billing-run/generate-invoice-data?invoiceId= (deprecated, BillingRunDocumentModelImpl),
 * POST /price-components (scaleComponent / volumesByScaleRequest), POST /billing-by-scales
 * (Endpoints.dataByScales), POST /products, POST /product-contracts, POST /billing-run.
 */

import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { randomGens } from '../../utils/randomGens';
import { scales as scalesPayloadTemplate } from '../../jsons/payloads/create/energyData/scales';
import { asPriceComponentId } from './pdt-2599-service-contract.fixtures';
import {
  buildInvoicePreviewLink,
  resolvePortalBase,
} from './dev-volume-invoice-custom-template.fixtures';

export { resolvePortalBase, buildInvoicePreviewLink };

export const PDT_3223_GRID_OPERATOR = 'GIO';
export const PDT_3223_PC_EXPRESSION = '100';
export const PDT_3223_PC_COUNT = 4;

/** Fixed displayName prefixes — ListPC[].PC asserts these in Name+Id order. */
export const PDT_3223_DISPLAY = {
  create1: 'AAAA-DISP',
  create2: 'BBBB-DISP',
  create3: 'DDDD-DISP',
  create4: 'CCCC-DISP',
} as const;

/** Name ASC then Id ASC → display sequence that differs from Id-only and displayName ASC. */
export const PDT_3223_EXPECTED_PC_ORDER: readonly string[] = [
  PDT_3223_DISPLAY.create3, // AAA name, lower id → DDDD-DISP
  PDT_3223_DISPLAY.create4, // AAA name, higher id → CCCC-DISP
  PDT_3223_DISPLAY.create2, // MMM → BBBB-DISP
  PDT_3223_DISPLAY.create1, // ZZZ → AAAA-DISP
];

export type Pdt3223Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'Nomenclatures'
>;

export type Pdt3223PcSpec = {
  name: string;
  displayName: string;
};

export type Pdt3223CreatedPc = Pdt3223PcSpec & { id: number };

export type Pdt3223PeriodWindow = {
  periodFrom: string;
  periodTo: string;
};

export type Pdt3223ContractPrechainResult = {
  isolationToken: string;
  period: Pdt3223PeriodWindow;
  podId: number;
  podIdentifier: string;
  priceComponents: Pdt3223CreatedPc[];
  priceComponentIds: number[];
  expectedPcOrder: string[];
  contractId: number;
  productId: number;
  scaleTariffId: number;
  scaleCodeId: number;
};

export type Pdt3223ScenarioResult = Pdt3223ContractPrechainResult & {
  invoiceId: number;
};

export type Pdt3223ListPcItem = { PC?: string };
export type Pdt3223TablePcScale = {
  PeriodFrom?: string;
  PeriodTo?: string;
  ListPC?: Pdt3223ListPcItem[];
};
export type Pdt3223DocumentDd = {
  PODID?: string;
  TablePCScales?: Pdt3223TablePcScale[];
};

/** Current calendar month window (Dev AP / FOR_VOLUMES). */
export function computePdt3223PeriodWindow(reference = new Date()): Pdt3223PeriodWindow {
  const year = reference.getUTCFullYear();
  const monthIndex = reference.getUTCMonth();
  const pad = (n: number) => String(n).padStart(2, '0');
  const month1 = monthIndex + 1;
  const lastDay = new Date(Date.UTC(year, monthIndex + 1, 0)).getUTCDate();
  return {
    periodFrom: `${year}-${pad(month1)}-01`,
    periodTo: `${year}-${pad(month1)}-${pad(lastDay)}`,
  };
}

/** Short alphanumeric token shared across all four PC names (isolation without breaking sort). */
export function buildPdt3223IsolationToken(): string {
  return Math.random().toString(36).slice(2, 8).toUpperCase();
}

export function buildPdt3223PcSpecs(isolationToken: string): Pdt3223PcSpec[] {
  const t = isolationToken;
  return [
    { name: `ZZZ-PDT3223-${t}`, displayName: PDT_3223_DISPLAY.create1 },
    { name: `MMM-PDT3223-${t}`, displayName: PDT_3223_DISPLAY.create2 },
    { name: `AAA-PDT3223-${t}`, displayName: PDT_3223_DISPLAY.create3 },
    { name: `AAA-PDT3223-${t}`, displayName: PDT_3223_DISPLAY.create4 },
  ];
}

export function buildPdt3223ScalePriceComponentPayload(
  GeneratePayload: Pdt3223Fx['GeneratePayload'],
  spec: Pdt3223PcSpec,
  scaleId: number,
): ReturnType<Pdt3223Fx['GeneratePayload']['productAndServices']['scaleComponent']> {
  const payload = GeneratePayload.productAndServices.scaleComponent();
  payload.name = spec.name;
  payload.displayName = spec.displayName;
  payload.formulaRequest.expression = PDT_3223_PC_EXPRESSION;
  payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
  payload.applicationModelRequest.volumesByScaleRequest.scaleIds = [scaleId];
  return payload;
}

async function createPdt3223OrderedScalePriceComponents(
  ctx: Pdt3223Fx,
  specs: Pdt3223PcSpec[],
  scaleId: number,
): Promise<Pdt3223CreatedPc[]> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const created: Pdt3223CreatedPc[] = [];

  for (const spec of specs) {
    const payload = buildPdt3223ScalePriceComponentPayload(GeneratePayload, spec, scaleId);
    const res = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(res).CheckResponse();
    const raw = await res.json();
    const id = asPriceComponentId(raw);
    expect(id, `scale price component id for ${spec.name}/${spec.displayName}`).toBeGreaterThan(0);
    created.push({ ...spec, id });
    Responses.priceComponent.push(id);
  }

  expect(created.length, 'expected four scale price components').toBe(PDT_3223_PC_COUNT);
  for (let i = 1; i < created.length; i++) {
    expect(
      created[i].id,
      `Create-order Id must be strictly increasing so Name+Id proof holds (idx ${i - 1}→${i}: ${created[i - 1].id} < ${created[i].id})`,
    ).toBeGreaterThan(created[i - 1].id);
  }
  return created;
}

async function createPdt3223ScalePodWithMeter(ctx: Pdt3223Fx): Promise<{
  podId: number;
  podIdentifier: string;
}> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = ctx;
  const gridId = await Nomenclatures.grid_operator(PDT_3223_GRID_OPERATOR);
  const measurementTypeId = await Nomenclatures.measurement_type(PDT_3223_GRID_OPERATOR, gridId);
  const scaleCodeId = await Nomenclatures.scales_code('scaleCode');
  const scaleTariffId = await Nomenclatures.scales_tariff(PDT_3223_GRID_OPERATOR);

  const podPayload = GeneratePayload.pointsOfDelivery.pod_slp();
  podPayload.gridOperatorId = gridId;
  podPayload.measurementTypeId = measurementTypeId;
  const podRes = await Request.post(Endpoints.pod, { data: podPayload });
  await expect(podRes).CheckResponse();
  // Swagger PodResponse (create): id + podDetailId + versionId only — no identifier on response.
  // Do NOT GET /pod/{id}: this token returns 403 ACCESS_DENIED (missing POD_VIEW_*).
  const podCreated = (await podRes.json()) as { id?: number; podDetailId?: number };
  const podId = Number(podCreated.id);
  expect(podId, 'POD create must return numeric id').toBeGreaterThan(0);
  const identifier = String(podPayload.identifier ?? '').trim();
  expect(identifier, 'pod_slp payload must include identifier').toBeTruthy();
  Responses.pod.push({
    ...podCreated,
    id: podId,
    identifier,
    gridOperatorId: gridId,
    estimatedMonthlyAvgConsumption: Number(podPayload.estimatedMonthlyAvgConsumption ?? 0),
    type: podPayload.type ?? 'CONSUMER',
  } as (typeof Responses.pod)[number]);

  const meterPayload = GeneratePayload.pointsOfDelivery.meters() as unknown as {
    gridOperatorId: number | null;
    podId: number | null;
    meterScales: number[];
  };
  meterPayload.gridOperatorId = gridId;
  meterPayload.podId = podId;
  meterPayload.meterScales = [scaleCodeId, scaleTariffId];
  const meterRes = await Request.post(Endpoints.meters, { data: meterPayload });
  await expect(meterRes).CheckResponse();
  // POST /meters may return a bare id (number) — PDT-3072 uses this with GET meters/{id}.
  Responses.meters.push(await meterRes.json());

  return { podId, podIdentifier: identifier };
}

/**
 * Whole-month scale energy data (no mid-period split) — PDT-3072 meter/scale classify pattern.
 */
async function buildPdt3223ScaleBillingPayload(
  ctx: Pdt3223Fx,
  period: Pdt3223PeriodWindow,
): Promise<Record<string, unknown>> {
  const { Request, Responses } = ctx;
  const meterId = Responses.meters[0] as number;
  const meterGet = await Request.get(`meters/${meterId}`);
  await expect(meterGet).CheckResponse();
  const meterJson = (await meterGet.json()) as {
    number?: string;
    meterScales?: Array<{ id?: number }>;
  };

  type ScaleJson = {
    id?: number;
    scaleType?: string | null;
    scaleCode?: string | null;
    tariffOrScale?: string | null;
  };
  const scaleDetails: ScaleJson[] = [];
  for (const meterScale of meterJson.meterScales ?? []) {
    const scaleId = meterScale?.id;
    if (scaleId == null) continue;
    const scaleGet = await Request.get(`scales/${scaleId}`);
    await expect(scaleGet, `PDT-3223 GET scales/${scaleId}`).CheckResponse();
    scaleDetails.push((await scaleGet.json()) as ScaleJson);
  }
  const scaleTariffJson =
    scaleDetails.find((s) => Boolean(s.tariffOrScale) && !s.scaleCode) ??
    scaleDetails.find((s) => Boolean(s.tariffOrScale));
  const scaleCodeJson =
    scaleDetails.find((s) => Boolean(s.scaleCode) && !s.tariffOrScale) ??
    scaleDetails.find((s) => Boolean(s.scaleCode));
  if (!scaleTariffJson?.tariffOrScale) {
    throw new Error(
      `PDT-3223: meter ${meterId} has no tariff scale with tariffOrScale ` +
        `(scales=${JSON.stringify(scaleDetails)})`,
    );
  }
  if (!scaleCodeJson?.scaleCode) {
    throw new Error(
      `PDT-3223: meter ${meterId} has no code scale with scaleCode ` +
        `(scales=${JSON.stringify(scaleDetails)})`,
    );
  }

  const podEntry = Responses.pod[0] as { id?: number; identifier?: string };
  const identifier = String(podEntry.identifier ?? '').trim();
  expect(
    identifier,
    'PDT-3223: Responses.pod[0] must carry identifier from create (POD GET is 403)',
  ).toBeTruthy();

  const payload = scalesPayloadTemplate() as Record<string, unknown>;
  payload.identifier = identifier;
  payload.dateFrom = period.periodFrom;
  payload.dateTo = period.periodTo;
  payload.invoiceDate = `${period.periodFrom}T00:00:00.000Z`;
  payload.invoiceNumber = '1';
  payload.correction = false;
  payload.invoiceCorrection = null;

  // Whole month only — tariff row + one scale-code reading row (drop mid-period split row).
  const rows = payload.billingByScalesTableCreateRequests as Array<Record<string, unknown>>;
  rows[0].periodFrom = period.periodFrom;
  rows[0].periodTo = period.periodTo;
  rows[0].meterNumber = meterJson.number;
  rows[0].scaleType = scaleTariffJson.scaleType;
  rows[0].tariffScale = scaleTariffJson.tariffOrScale;
  rows[0].volumes = '1180';
  rows[0].index = 0;

  rows[1].periodFrom = period.periodFrom;
  rows[1].periodTo = period.periodTo;
  rows[1].meterNumber = meterJson.number;
  rows[1].scaleCode = scaleCodeJson.scaleCode;
  rows[1].scaleNumber = randomGens.getRandomEven().toString();
  rows[1].scaleType = scaleCodeJson.scaleType;
  rows[1].newMeterReading = '30';
  rows[1].oldMeterReading = '10';
  rows[1].difference = '20';
  rows[1].multiplier = '2';
  rows[1].totalVolumes = '40';
  rows[1].index = 1;

  payload.billingByScalesTableCreateRequests = [rows[0], rows[1]];
  return payload;
}

async function postPdt3223BillingByScales(
  ctx: Pdt3223Fx,
  payload: Record<string, unknown>,
): Promise<number> {
  const { Request, Responses, Endpoints } = ctx;
  const res = await Request.post(Endpoints.dataByScales, { data: payload });
  await expect(res, 'PDT-3223 POST dataByScales').CheckResponse();
  const scaleBillingId = Number(await res.json());
  Responses.dataByScales.push(scaleBillingId);
  return scaleBillingId;
}

/** Contract prechain: four SCALE PCs on product via priceComponentIds (no PC group). */
export async function runPdt3223ContractPrechain(ctx: Pdt3223Fx): Promise<Pdt3223ContractPrechainResult> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = ctx;
  const isolationToken = buildPdt3223IsolationToken();
  const specs = buildPdt3223PcSpecs(isolationToken);
  const period = computePdt3223PeriodWindow();

  await test.step('Precondition: legal customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await Nomenclatures.grid_operator(PDT_3223_GRID_OPERATOR);
  const scaleTariffId = await Nomenclatures.scales_tariff(PDT_3223_GRID_OPERATOR);
  const scaleCodeId = await Nomenclatures.scales_code('scaleCode');

  const priceComponents = await test.step(
    'Precondition: four scale PCs (Name order ≠ Id order; two share Name for Id tie-break)',
    async () => createPdt3223OrderedScalePriceComponents(ctx, specs, scaleTariffId),
  );
  const priceComponentIds = priceComponents.map((pc) => pc.id);

  await test.step('Precondition: terms', async () => {
    const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  const { podId, podIdentifier } = await test.step(
    'Precondition: SLP POD + meter (scaleCode + scaleTariff)',
    async () => createPdt3223ScalePodWithMeter(ctx),
  );

  let productId = 0;
  await test.step('Precondition: product with four priceComponentIds (no group)', async () => {
    const productPayload = GeneratePayload.productAndServices.product();
    productPayload.priceComponentIds = priceComponentIds;
    productPayload.priceComponentGroupIds = [];
    const product = await Request.post(Endpoints.product, { data: productPayload });
    await expect(product).CheckResponse();
    const productBody = await product.json();
    Responses.product.push(productBody);
    productId = asPriceComponentId(productBody);
    expect(productId, 'product id required').toBeGreaterThan(0);
  });

  let contractId = 0;
  await test.step('Precondition: product contract', async () => {
    // product_contract() GETs /pod/{id} for estimatedMonthlyAvgConsumption; this token gets 403,
    // so override from values we stored on create (PDT-3072 pattern).
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
    let monthlySum = 0;
    for (const pod of Responses.pod) {
      const row = pod as { type?: string; estimatedMonthlyAvgConsumption?: number };
      if ((row.type ?? 'CONSUMER') === 'CONSUMER') {
        monthlySum += Number(row.estimatedMonthlyAvgConsumption ?? 0);
      }
    }
    contractPayload.additionalParameters.estimatedTotalConsumptionUnderContractKwh =
      (monthlySum * 12) / 1000;
    const contract = await Request.post(Endpoints.productContract, {
      data: contractPayload,
    });
    await expect(contract).CheckResponse();
    const contractBody = await contract.json();
    Responses.productContract.push(contractBody);
    contractId = asPriceComponentId(contractBody);
    expect(contractId, 'product contract id required for portal links').toBeGreaterThan(0);
  });

  await test.step('Precondition: activate POD on contract from billing window start', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, period.periodFrom),
    });
    await expect(podActivation).CheckResponse();
  });

  return {
    isolationToken,
    period,
    podId,
    podIdentifier,
    priceComponents,
    priceComponentIds,
    expectedPcOrder: [...PDT_3223_EXPECTED_PC_ORDER],
    contractId,
    productId,
    scaleTariffId,
    scaleCodeId,
  };
}

/** Post billing-by-scales (whole month) and realize FOR_VOLUMES invoice (PDT-3072 pattern). */
export async function runPdt3223BillingAndRealizeInvoice(
  ctx: Pdt3223Fx,
  pre: Pdt3223ContractPrechainResult,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  await test.step('Precondition: billing-by-scales for POD (whole current month)', async () => {
    const payload = await buildPdt3223ScaleBillingPayload(ctx, pre.period);
    await postPdt3223BillingByScales(ctx, payload);
  });

  await test.step('Precondition: standard FOR_VOLUMES billing run', async () => {
    const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
    const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(billingRun).CheckResponse();
    Responses.billingRun.push(await billingRun.json());
  });

  await test.step('Precondition: realize standard invoice', async () => {
    await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
  });

  return Responses.invoice[Responses.invoice.length - 1] as number;
}

/** GET /billing-run/generate-invoice-data?invoiceId= (Swagger: deprecated, BillingRunDocumentModelImpl). */
export async function fetchPdt3223InvoiceDocumentModel(
  Request: baseFixture['Request'],
  invoiceId: number,
): Promise<{ DD?: Pdt3223DocumentDd[] }> {
  const res = await Request.get(
    `billing-run/generate-invoice-data?invoiceId=${encodeURIComponent(String(invoiceId))}`,
  );
  await expect(res).CheckResponse();
  return (await res.json()) as { DD?: Pdt3223DocumentDd[] };
}

export function extractPdt3223ListPcSequences(
  document: { DD?: Pdt3223DocumentDd[] },
): Array<{ scaleIndex: number; periodFrom?: string; periodTo?: string; pcNames: string[] }> {
  const sequences: Array<{
    scaleIndex: number;
    periodFrom?: string;
    periodTo?: string;
    pcNames: string[];
  }> = [];
  let scaleIndex = 0;
  for (const dd of document.DD ?? []) {
    for (const scale of dd.TablePCScales ?? []) {
      const list = scale.ListPC ?? [];
      if (list.length === 0) {
        scaleIndex += 1;
        continue;
      }
      sequences.push({
        scaleIndex,
        periodFrom: scale.PeriodFrom,
        periodTo: scale.PeriodTo,
        pcNames: list.map((item) => String(item.PC ?? '').trim()),
      });
      scaleIndex += 1;
    }
  }
  return sequences;
}

/**
 * Assert every non-empty TablePCScales.ListPC PC sequence equals expected displayNames
 * ordered by Name ASC then Id ASC.
 */
export async function assertPdt3223ListPcOrderByNameThenId(
  Request: baseFixture['Request'],
  invoiceId: number,
  expectedPcOrder: readonly string[],
): Promise<{
  document: { DD?: Pdt3223DocumentDd[] };
  sequences: ReturnType<typeof extractPdt3223ListPcSequences>;
}> {
  const document = await fetchPdt3223InvoiceDocumentModel(Request, invoiceId);
  const sequences = extractPdt3223ListPcSequences(document);

  expect(
    sequences.length,
    `PDT-3223: expected ≥1 TablePCScales with ListPC on invoice ${invoiceId}`,
  ).toBeGreaterThanOrEqual(1);

  for (const seq of sequences) {
    expect(
      seq.pcNames,
      `ListPC[].PC order for scale[${seq.scaleIndex}] period ${seq.periodFrom}→${seq.periodTo} ` +
        `must be Name ASC then Id ASC (displayNames). ` +
        `Id-only would be AAAA,BBBB,DDDD,CCCC; display ASC would be AAAA,BBBB,CCCC,DDDD.`,
    ).toEqual([...expectedPcOrder]);
  }

  return { document, sequences };
}

export function buildPdt3223InvoicePreviewLink(invoiceId: number): string {
  return buildInvoicePreviewLink(invoiceId, resolvePortalBase());
}

export function buildPdt3223AttachmentSummary(args: Record<string, unknown>): Record<string, unknown> {
  return { jiraKey: 'PDT-3223', ...args };
}
