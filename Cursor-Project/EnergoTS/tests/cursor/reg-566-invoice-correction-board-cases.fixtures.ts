/**
 * REG-566 board cases — invoice correction helpers for REG-694 / REG-704 / REG-705.
 *
 * Jira (oppa-support):
 * - REG-694 combined price+volume: price 100→200, profile 100→200, debit 40_000 / credit 10_000
 * - REG-704 scale volume full reversal: BBS 10 then correction 20, PC 20, debit 600 / credit 200
 * - REG-705 mixed two-POD: REG-704 SLP full reversal + unchanged settlement POD half reversal
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts (BBP kWh, correctionBilling flags)
 * - tests/cursor/pdt-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts (SLP+meter+BBS correction)
 * - tests/billing/correction/correctionCases.spec.ts (FOR_VOLUMES → INVOICE_CORRECTION chain)
 *
 * Swagger (refreshed this session, dev + dev2):
 * - InvoiceCorrectionParameters.priceChange / volumeChange / listOfInvoices
 * - BillingRunCreateRequest.billingType = INVOICE_CORRECTION
 * - InvoiceResponse.invoiceDocumentType, totalAmountExcludingVat
 * - BillingByScalesCreateRequest.correction, invoiceCorrection, invoiceNumber
 * - BillingByProfileCreateRequest.entries[].value, warningAcceptedByUser
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { profile1Month as profile1MonthTemplate } from '../../jsons/payloads/create/energyData/profile1Month';
import { scales as scalesPayloadTemplate } from '../../jsons/payloads/create/energyData/scales';
import { asPriceComponentId } from './pdt-2599-service-contract.fixtures';


export const REG566_GRID_OPERATOR = 'GIO';

export const REG694_PRICE_ORIGINAL = 100;
export const REG694_PRICE_NEW = 200;
export const REG694_VOLUME_ORIGINAL = 100;
export const REG694_VOLUME_NEW = 200;
export const REG694_CREDIT_EXCL_VAT = 10_000;
export const REG694_DEBIT_EXCL_VAT = 40_000;

export const REG704_PRICE = 20;
export const REG704_SCALE_ORIGINAL_TOTAL = 10;
export const REG704_SCALE_CORRECTION_TOTAL = 20;
export const REG704_CREDIT_EXCL_VAT = 200;
export const REG704_DEBIT_EXCL_VAT = 600;

/** Settlement POD volume for REG-705 half-reversal row (PC 20 × 10 kWh = 200). */
export const REG705_SETTLEMENT_VOLUME = 10;
export const REG705_SETTLEMENT_ORIGINAL_EXCL_VAT = REG704_PRICE * REG705_SETTLEMENT_VOLUME;

export type Reg566Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'Nomenclatures'
>;

export type Reg566MonthWindow = {
  periodFrom: string;
  periodTo: string;
};

export type Reg566InvoiceBody = {
  id?: number;
  invoiceNumber?: string;
  invoiceDocumentType?: string;
  invoiceType?: string;
  totalAmountExcludingVat?: string | number;
};

export type Reg566SummaryRow = {
  priceComponent?: string;
  totalVolumes?: number;
  unitPrice?: string;
  value?: string | number;
  type?: string;
};

export type Reg566CorrectionPair = {
  originalInvoiceId: number;
  originalInvoiceNumber: string;
  debit: Reg566InvoiceBody;
  credit: Reg566InvoiceBody;
};

export function currentMonthWindow(reference = new Date()): Reg566MonthWindow {
  const year = reference.getUTCFullYear();
  const monthIndex = reference.getUTCMonth();
  const pad = (n: number) => String(n).padStart(2, '0');
  const lastDay = new Date(Date.UTC(year, monthIndex + 1, 0)).getUTCDate();
  return {
    periodFrom: `${year}-${pad(monthIndex + 1)}-01`,
    periodTo: `${year}-${pad(monthIndex + 1)}-${pad(lastDay)}`,
  };
}

export function moneyNumber(value: unknown): number {
  const n = Number(String(value ?? '').replace(/\s/g, '').replace(',', '.'));
  return Number.isFinite(n) ? n : NaN;
}

export function expectMoney(actual: unknown, expected: number, label: string): void {
  const n = moneyNumber(actual);
  expect(n, `${label} actual=${String(actual)}`).toBeCloseTo(expected, 2);
}

export async function fetchInvoice(
  Request: baseFixture['Request'],
  invoiceId: number,
): Promise<Reg566InvoiceBody> {
  const res = await Request.get(`invoice?id=${invoiceId}`);
  await expect(res, `GET invoice?id=${invoiceId}`).CheckResponse();
  return (await res.json()) as Reg566InvoiceBody;
}

export type Reg566DetailedRow = {
  pointOfDelivery?: string;
  value?: number | string;
  totalVolumes?: number;
};

export async function fetchInvoiceDetailedRows(
  Request: baseFixture['Request'],
  invoiceId: number,
): Promise<Reg566DetailedRow[]> {
  const res = await Request.get(`invoice/detailed-data?id=${invoiceId}&page=0&size=50`);
  await expect(res, `GET invoice/detailed-data id=${invoiceId}`).CheckResponse();
  const body = (await res.json()) as { content?: Reg566DetailedRow[] };
  return body.content ?? [];
}

export function sumDetailedValueForPod(rows: Reg566DetailedRow[], podIdentifier: string): number {
  const needle = podIdentifier.trim();
  return rows
    .filter((row) => String(row.pointOfDelivery ?? '').trim() === needle)
    .reduce((sum, row) => sum + moneyNumber(row.value), 0);
}

export async function fetchInvoiceSummaryRows(
  Request: baseFixture['Request'],
  invoiceId: number,
): Promise<Reg566SummaryRow[]> {
  const res = await Request.get(`invoice/summary-data?page=0&size=25&id=${invoiceId}`);
  await expect(res, `GET invoice/summary-data id=${invoiceId}`).CheckResponse();
  const body = (await res.json()) as { content?: Reg566SummaryRow[] };
  return body.content ?? [];
}

async function pushStoredPod(
  Responses: baseFixture['Responses'],
  created: Record<string, unknown>,
  payload: { identifier?: string; estimatedMonthlyAvgConsumption?: string | number; type?: string },
): Promise<void> {
  const podId = Number(created.id);
  expect(podId, 'POD create must return numeric id').toBeGreaterThan(0);
  const identifier = String(payload.identifier ?? '').trim();
  expect(identifier, 'POD payload must include identifier (avoid GET /pod ACCESS_DENIED)').toBeTruthy();
  Responses.pod.push({
    ...created,
    id: podId,
    identifier,
    estimatedMonthlyAvgConsumption: Number(payload.estimatedMonthlyAvgConsumption ?? 0),
    type: payload.type ?? 'CONSUMER',
  } as (typeof Responses.pod)[number]);
}

export async function createLegalCustomer(ctx: Reg566Fx): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const customer = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(customer).CheckResponse();
  Responses.customer.push(await customer.json());
}

export async function createTerm(ctx: Reg566Fx): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());
}

export async function createSettlementPriceComponent(
  ctx: Reg566Fx,
  expression: string,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = GeneratePayload.productAndServices.priceSettlement();
  payload.formulaRequest.expression = expression;
  payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
  const res = await Request.post(Endpoints.priceComponent, { data: payload });
  await expect(res).CheckResponse();
  const id = asPriceComponentId(await res.json());
  Responses.priceComponent.push(id);
  return id;
}

export async function editSettlementPriceComponentExpression(
  ctx: Reg566Fx,
  priceComponentId: number,
  expression: string,
): Promise<void> {
  const { Request, GeneratePayload } = ctx;
  const payload = GeneratePayload.productAndServices.priceSettlement();
  payload.formulaRequest.expression = expression;
  payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
  const res = await Request.put(`price-components/${priceComponentId}`, { data: payload });
  await expect(res, `PUT price-components/${priceComponentId} expression=${expression}`).CheckResponse();
}

export async function createScalePriceComponent(
  ctx: Reg566Fx,
  expression: string,
  scaleId: number,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = GeneratePayload.productAndServices.scaleComponent();
  payload.formulaRequest.expression = expression;
  payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
  payload.applicationModelRequest.volumesByScaleRequest.scaleIds = [scaleId];
  const res = await Request.post(Endpoints.priceComponent, { data: payload });
  await expect(res).CheckResponse();
  const id = asPriceComponentId(await res.json());
  Responses.priceComponent.push(id);
  return id;
}

export async function createSettlementPod(ctx: Reg566Fx): Promise<string> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
  const res = await Request.post(Endpoints.pod, { data: payload });
  await expect(res).CheckResponse();
  const created = (await res.json()) as Record<string, unknown>;
  await pushStoredPod(Responses, created, payload);
  return String(payload.identifier);
}

export async function createScalePodWithMeter(ctx: Reg566Fx): Promise<string> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = ctx;
  const gridId = await Nomenclatures.grid_operator(REG566_GRID_OPERATOR);
  const measurementTypeId = await Nomenclatures.measurement_type(REG566_GRID_OPERATOR, gridId);
  const scaleCodeId = await Nomenclatures.scales_code('scaleCode');
  const scaleTariffId = await Nomenclatures.scales_tariff(REG566_GRID_OPERATOR);

  const podPayload = GeneratePayload.pointsOfDelivery.pod_slp();
  podPayload.gridOperatorId = gridId;
  podPayload.measurementTypeId = measurementTypeId;
  const podRes = await Request.post(Endpoints.pod, { data: podPayload });
  await expect(podRes).CheckResponse();
  const created = (await podRes.json()) as Record<string, unknown>;
  await pushStoredPod(Responses, created, podPayload);
  const podId = Number(created.id);

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
  Responses.meters.push(asPriceComponentId(await meterRes.json()));
  return String(podPayload.identifier ?? '').trim();
}

export async function createProductFromResponses(ctx: Reg566Fx): Promise<void> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const product = await Request.post(Endpoints.product, {
    data: GeneratePayload.productAndServices.product(),
  });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());
}

export async function createContractAndActivatePods(
  ctx: Reg566Fx,
  period: Reg566MonthWindow,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
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

  const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(contract).CheckResponse();
  const contractBody = await contract.json();
  Responses.productContract.push(contractBody);
  const contractId = Number((contractBody as { id?: number }).id);
  expect(contractId, 'product contract id').toBeGreaterThan(0);

  for (let i = 0; i < Responses.pod.length; i++) {
    const activation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(i, period.periodFrom, period.periodTo),
    });
    await expect(activation, `Activate POD index ${i}`).CheckResponse();
  }
  return contractId;
}

export async function postBillingByProfile(
  ctx: Reg566Fx,
  podIndex: number,
  kwh: number,
): Promise<number> {
  const { Request, Responses } = ctx;
  const pod = Responses.pod[podIndex] as { identifier?: string };
  const identifier = String(pod.identifier ?? '').trim();
  expect(identifier, `Responses.pod[${podIndex}] identifier required for billing-by-profile`).toBeTruthy();

  const payload = profile1MonthTemplate();
  payload.identifier = identifier;
  payload.timeZone = 'CET';
  payload.warningAcceptedByUser = true;
  payload.entries[0].value = kwh;

  const res = await Request.post('billing-by-profile', { data: payload });
  await expect(res, `POST billing-by-profile kWh=${kwh}`).CheckResponse();
  const id = Number(await res.json());
  Responses.dataByProfiles.push({
    id,
    periodFrom: payload.periodFrom,
    periodTo: payload.periodTo,
    periodType: payload.periodType,
  });
  return id;
}

async function tariffScaleFromMeter(
  Request: baseFixture['Request'],
  meterId: number,
): Promise<{ meterNumber: string; scaleType: string | null; tariffScale: string }> {
  const meterGet = await Request.get(`meters/${meterId}`);
  await expect(meterGet).CheckResponse();
  const meterJson = (await meterGet.json()) as {
    number?: string;
    meterScales?: Array<{ id?: number }>;
  };
  type ScaleJson = { tariffOrScale?: string | null; scaleType?: string | null; scaleCode?: string | null };
  const details: ScaleJson[] = [];
  for (const meterScale of meterJson.meterScales ?? []) {
    const scaleId = meterScale?.id;
    if (scaleId == null) continue;
    const scaleGet = await Request.get(`scales/${scaleId}`);
    await expect(scaleGet, `GET scales/${scaleId}`).CheckResponse();
    details.push((await scaleGet.json()) as ScaleJson);
  }
  const tariff =
    details.find((s) => Boolean(s.tariffOrScale) && !s.scaleCode) ??
    details.find((s) => Boolean(s.tariffOrScale));
  if (!tariff?.tariffOrScale) {
    throw new Error(`Meter ${meterId} has no tariff scale (scales=${JSON.stringify(details)})`);
  }
  return {
    meterNumber: String(meterJson.number ?? ''),
    scaleType: tariff.scaleType ?? null,
    tariffScale: String(tariff.tariffOrScale),
  };
}

export async function postBillingByScalesTariffRow(
  ctx: Reg566Fx,
  podIndex: number,
  period: Reg566MonthWindow,
  amounts: { volumes: number; unitPrice: number; totalValue: number },
  options: { correction?: boolean; invoiceNumber?: string } = {},
): Promise<number> {
  const { Request, Responses, Endpoints } = ctx;
  const meterId = Responses.meters[podIndex] as number;
  const tariff = await tariffScaleFromMeter(Request, meterId);
  const identifier = String((Responses.pod[podIndex] as { identifier?: string }).identifier ?? '').trim();
  expect(identifier, `Responses.pod[${podIndex}] identifier required for billing-by-scales`).toBeTruthy();

  const payload = scalesPayloadTemplate() as Record<string, unknown>;
  payload.identifier = identifier;
  payload.dateFrom = period.periodFrom;
  payload.dateTo = period.periodTo;
  payload.invoiceDate = `${period.periodFrom}T00:00:00.000Z`;
  payload.invoiceNumber = options.invoiceNumber ?? '1';
  payload.correction = options.correction ?? false;
  payload.invoiceCorrection = options.correction ? 'VOLUME_CHANGE' : null;
  payload.billingByScalesTableCreateRequests = [
    {
      periodFrom: period.periodFrom,
      periodTo: period.periodTo,
      meterNumber: tariff.meterNumber,
      scaleType: tariff.scaleType,
      tariffScale: tariff.tariffScale,
      volumes: String(amounts.volumes),
      unitPrice: String(amounts.unitPrice),
      totalValue: String(amounts.totalValue),
      index: 0,
    },
  ];

  const res = await Request.post(Endpoints.dataByScales, { data: payload });
  await expect(res, `POST billing-by-scales correction=${Boolean(options.correction)}`).CheckResponse();
  const scaleId = Number(await res.json());
  Responses.dataByScales.push(scaleId);
  return scaleId;
}

export async function realizeStandardForVolumesInvoice(ctx: Reg566Fx): Promise<{
  originalInvoiceId: number;
  originalInvoiceNumber: string;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  Responses.billingRun.push(await billingRun.json());

  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);

  const originalInvoiceId = Responses.invoice[Responses.invoice.length - 1] as number;
  expect(originalInvoiceId, 'realized standard invoice id').toBeGreaterThan(0);
  const body = await fetchInvoice(Request, originalInvoiceId);
  const originalInvoiceNumber = String(body.invoiceNumber ?? '');
  expect(originalInvoiceNumber, 'original invoice number').toBeTruthy();
  return { originalInvoiceId, originalInvoiceNumber };
}

export async function createCorrectionBillingRun(
  ctx: Reg566Fx,
  invoiceIndex: number,
  priceChange: boolean,
  volumeChange: boolean,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = await GeneratePayload.billing.correctionBilling(invoiceIndex, priceChange, volumeChange);
  // Template defaults priceChange=true; always set both flags explicitly.
  payload.invoiceCorrectionParameters.priceChange = priceChange;
  payload.invoiceCorrectionParameters.volumeChange = volumeChange;
  const correction = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(correction, 'POST /billing-run INVOICE_CORRECTION').CheckResponse();
  const correctionId = Number(await correction.json());
  Responses.billingRun.push(correctionId);
  return correctionId;
}

export async function realizeCorrectionInvoices(
  ctx: Reg566Fx,
  expectedCount: number,
  billingIndex: number,
): Promise<number[]> {
  const before = ctx.Responses.invoice.length;
  await ctx.GeneratePayload.billing.waitForInvoiceGeneration(true, true, expectedCount, billingIndex);
  const created = ctx.Responses.invoice.slice(before) as number[];
  expect(created.length, `correction should add ${expectedCount} invoice id(s)`).toBe(expectedCount);
  return created;
}

export async function splitDebitAndCredit(
  Request: baseFixture['Request'],
  invoiceIds: number[],
): Promise<{ debit: Reg566InvoiceBody; credit: Reg566InvoiceBody }> {
  const bodies: Reg566InvoiceBody[] = [];
  for (const id of invoiceIds) {
    bodies.push(await fetchInvoice(Request, id));
  }
  const debit = bodies.find((b) => b.invoiceDocumentType === 'DEBIT_NOTE');
  const credit = bodies.find((b) => b.invoiceDocumentType === 'CREDIT_NOTE');
  expect(debit, `DEBIT_NOTE missing among ${JSON.stringify(bodies)}`).toBeTruthy();
  expect(credit, `CREDIT_NOTE missing among ${JSON.stringify(bodies)}`).toBeTruthy();
  return { debit: debit!, credit: credit! };
}

export async function runReg694SettlementPrechain(ctx: Reg566Fx): Promise<{
  period: Reg566MonthWindow;
  contractId: number;
  priceComponentId: number;
  podIdentifier: string;
}> {
  const period = currentMonthWindow();
  await createLegalCustomer(ctx);
  const priceComponentId = await createSettlementPriceComponent(ctx, String(REG694_PRICE_ORIGINAL));
  await createTerm(ctx);
  const podIdentifier = await createSettlementPod(ctx);
  await createProductFromResponses(ctx);
  const contractId = await createContractAndActivatePods(ctx, period);
  await postBillingByProfile(ctx, 0, REG694_VOLUME_ORIGINAL);
  return { period, contractId, priceComponentId, podIdentifier };
}

export async function runReg704ScalePrechain(ctx: Reg566Fx): Promise<{
  period: Reg566MonthWindow;
  contractId: number;
  podIdentifier: string;
  scaleTariffId: number;
}> {
  const period = currentMonthWindow();
  await createLegalCustomer(ctx);
  await ctx.Nomenclatures.grid_operator(REG566_GRID_OPERATOR);
  const scaleTariffId = await ctx.Nomenclatures.scales_tariff(REG566_GRID_OPERATOR);
  await createScalePriceComponent(ctx, String(REG704_PRICE), scaleTariffId);
  await createTerm(ctx);
  const podIdentifier = await createScalePodWithMeter(ctx);
  await createProductFromResponses(ctx);
  const contractId = await createContractAndActivatePods(ctx, period);
  await postBillingByScalesTariffRow(ctx, 0, period, {
    volumes: REG704_SCALE_ORIGINAL_TOTAL,
    unitPrice: REG704_SCALE_ORIGINAL_TOTAL,
    totalValue: REG704_SCALE_ORIGINAL_TOTAL,
  });
  return { period, contractId, podIdentifier, scaleTariffId };
}

export async function runReg705MixedPodPrechain(ctx: Reg566Fx): Promise<{
  period: Reg566MonthWindow;
  contractId: number;
  slpIdentifier: string;
  settlementIdentifier: string;
}> {
  const period = currentMonthWindow();
  await createLegalCustomer(ctx);
  await ctx.Nomenclatures.grid_operator(REG566_GRID_OPERATOR);
  const scaleTariffId = await ctx.Nomenclatures.scales_tariff(REG566_GRID_OPERATOR);
  await createScalePriceComponent(ctx, String(REG704_PRICE), scaleTariffId);
  await createSettlementPriceComponent(ctx, String(REG704_PRICE));
  await createTerm(ctx);
  const slpIdentifier = await createScalePodWithMeter(ctx);
  const settlementIdentifier = await createSettlementPod(ctx);
  await createProductFromResponses(ctx);
  const contractId = await createContractAndActivatePods(ctx, period);
  await postBillingByScalesTariffRow(ctx, 0, period, {
    volumes: REG704_SCALE_ORIGINAL_TOTAL,
    unitPrice: REG704_SCALE_ORIGINAL_TOTAL,
    totalValue: REG704_SCALE_ORIGINAL_TOTAL,
  });
  await postBillingByProfile(ctx, 1, REG705_SETTLEMENT_VOLUME);
  return { period, contractId, slpIdentifier, settlementIdentifier };
}
