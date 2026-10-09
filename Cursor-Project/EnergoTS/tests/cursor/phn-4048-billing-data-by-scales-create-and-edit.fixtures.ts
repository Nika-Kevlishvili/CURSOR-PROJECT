/**
 * PHN-4048 — Billing data by scales create + table-only edit (Dev2 runtime).
 *
 * Runtime (phoenix-core BillingByScalesController): POST 201, PUT 200 (lock
 * entityType=data-by-scales), GET /{id} 200, GET /list 206, DELETE 200.
 * There is no /billing-by-scales/import.
 *
 * Swagger (dev2, refreshed 2026-08-20 via environments.json openapi_json):
 * BillingByScalesCreateRequest, BillingByScalesEditRequest,
 * BillingByScalesTableCreateRequest / TableEditRequest, BillingByScalesResponse,
 * BillingByScalesListingRequest (searchBy POD_IDENTIFIER), GridOperatorRequest,
 * UserTypeRequest, CountryRequest, ScalesRequest (tariffOrScale), PodCreateRequest,
 * MeterRequest (meterScales), POST /locks/acquire.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3223-invoice-json-tablepcscales-listpc-order.fixtures.ts
 * - tests/cursor/PHN-4002-product-contract-sales-channel-filter.spec.ts
 * - tests/cursor/PHN-3992-invoice-cancel-closed-accounting-period.spec.ts
 * - tests/billing/forVolumes/forVolumes.spec.ts
 * - tests/cursor/cursor-test.fixtures.ts
 * - fixtures/constants/endpoints.ts
 * - jsons/payloads/create/energyData/dataByScales(ScaleCode).ts
 */

import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { Endpoints } from '../../fixtures/constants/endpoints';
import { randomGens } from '../../utils/randomGens';
import type { TestRunSummaryCollector } from './shared/test-run-summary.fixtures';

export const PHN_4048_KEY = 'PHN-4048';
export const PHN_4048_JIRA_TITLE =
  'Performance improvement - mass import of measurement data in Phoenix';

export const DATE_FROM = '2026-02-01';
export const DATE_TO = '2026-02-28';
export const INVOICE_DATE = '2026-02-15T00:00:00.000Z';
export const METER_INSTALL = '2026-01-01';
export const METER_REMOVE = '2026-12-31';
export const SCALE_CODE = '1';
export const SCALE_TYPE = 'DAY';
export const TARIFF_OR_SCALE = 'TARIFF1';

export const GRID_OPERATORS_PATH = 'grid-operators';
export const USER_TYPES_PATH = 'user-types';
export const COUNTRIES_PATH = 'countries';
export const SCALES_PATH = 'scales';

/** Reuse existing Dev2 GO (POST /grid-operators hits grid_operators_ordering_uk). */
export const PHN_4048_GRID_OPERATOR = 'GIO';

export type Phn4048Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'Nomenclatures'
>;

export type Phn4048Chain = {
  tag: string;
  unique: string;
  gridOperatorId: number;
  scaleCodeId: number;
  tariffScaleId: number;
  scaleCode: string;
  scaleType: string;
  tariffScale: string;
  tariffScaleType: string;
  podId: number;
  identifier: string;
  meterId: number;
  meterNumber: string;
};

export type ScaleCodeRowOverrides = {
  periodFrom?: string;
  periodTo?: string;
  meterNumber?: string;
  scaleCode?: string;
  scaleType?: string;
  scaleNumber?: string;
  newMeterReading?: number;
  oldMeterReading?: number;
  difference?: number;
  multiplier?: number;
  totalVolumes?: number;
  index?: number;
  tariffScale?: string;
  volumes?: number;
  unitPrice?: number;
  totalValue?: number;
};

export type HeaderOverrides = {
  identifier?: string;
  dateFrom?: string;
  dateTo?: string;
  billingPowerInKw?: number;
  invoiceNumber?: string;
  invoiceDate?: string;
  invoiceCorrection?: string | null;
  correction?: boolean;
  override?: boolean;
  saveRecordForIntermediatePeriod?: boolean;
  saveRecordForMeterReadings?: boolean;
  billingByScalesTableCreateRequests?: Record<string, unknown>[];
};

function uniqueDigits(): string {
  return randomGens.generateUniqueIdentifier();
}

export function uniquePodIdentifier(tagDigits: string): string {
  const tail = `${tagDigits}${uniqueDigits()}`.replace(/\D/g, '').slice(-10);
  return `32ZB${tail}`.slice(0, 16);
}

export function uniqueMeterNumber(tag: string): string {
  return `MTR${tag.replace(/[^0-9A-Za-z]/g, '')}${uniqueDigits()}`.slice(0, 24);
}

export function asPositiveId(body: unknown, label: string): number {
  let value: number;
  if (typeof body === 'number') {
    value = body;
  } else if (typeof body === 'string' && /^-?\d+(\.\d+)?$/.test(body.trim())) {
    value = Number(body);
  } else if (body && typeof body === 'object' && 'id' in body) {
    value = Number((body as { id: unknown }).id);
  } else {
    value = Number(body);
  }
  expect(value, label).toBeGreaterThan(0);
  return value;
}

export function ymd(value: unknown): string {
  return String(value ?? '').slice(0, 10);
}

export function asNumber(value: unknown): number {
  return Number(value);
}

export function tableRows(view: Record<string, unknown>): Record<string, unknown>[] {
  const rows = view.billingByScalesTableCreateRequests;
  return Array.isArray(rows) ? (rows as Record<string, unknown>[]) : [];
}

export function extractApiErrorMessage(body: unknown, rawText = ''): string {
  if (typeof body === 'string' && body.trim()) {
    return body;
  }
  if (body && typeof body === 'object') {
    const record = body as Record<string, unknown>;
    const parts = [record.message, record.error, record.detail, record.title]
      .filter((part) => typeof part === 'string' && part.trim())
      .map((part) => String(part));
    if (parts.length) {
      return parts.join(' | ');
    }
    return JSON.stringify(body);
  }
  return rawText || JSON.stringify(body ?? {});
}

export function normalizeApostrophes(text: string): string {
  return text.replace(/[\u2018\u2019\u0060]/g, "'");
}

export function happyHeaderFlags(): Pick<
  HeaderOverrides,
  'correction' | 'override' | 'saveRecordForIntermediatePeriod' | 'saveRecordForMeterReadings'
> {
  return {
    correction: false,
    override: false,
    saveRecordForIntermediatePeriod: true,
    saveRecordForMeterReadings: true,
  };
}

export function scaleCodeRow(
  chain: Phn4048Chain,
  overrides: ScaleCodeRowOverrides = {},
): Record<string, unknown> {
  return {
    periodFrom: overrides.periodFrom ?? DATE_FROM,
    periodTo: overrides.periodTo ?? DATE_TO,
    meterNumber: overrides.meterNumber ?? chain.meterNumber,
    scaleCode: overrides.scaleCode ?? chain.scaleCode,
    scaleType: overrides.scaleType ?? chain.scaleType,
    scaleNumber: overrides.scaleNumber ?? '1',
    newMeterReading: overrides.newMeterReading ?? 100,
    oldMeterReading: overrides.oldMeterReading ?? 0,
    difference: overrides.difference ?? 100,
    multiplier: overrides.multiplier ?? 1,
    totalVolumes: overrides.totalVolumes ?? 100,
    index: overrides.index ?? 0,
  };
}

export function tariffRow(
  chain: Phn4048Chain,
  overrides: ScaleCodeRowOverrides = {},
): Record<string, unknown> {
  const row: Record<string, unknown> = {
    periodFrom: overrides.periodFrom ?? DATE_FROM,
    periodTo: overrides.periodTo ?? DATE_TO,
    meterNumber: overrides.meterNumber ?? chain.meterNumber,
    scaleType: overrides.scaleType ?? chain.tariffScaleType,
    tariffScale: overrides.tariffScale ?? chain.tariffScale,
    volumes: overrides.volumes ?? 100,
    unitPrice: overrides.unitPrice ?? 1,
    totalValue: overrides.totalValue ?? 100,
    index: overrides.index ?? 0,
  };
  if (overrides.scaleCode !== undefined) {
    row.scaleCode = overrides.scaleCode;
  }
  if (overrides.scaleNumber !== undefined) {
    row.scaleNumber = overrides.scaleNumber;
  }
  return row;
}

export function createHeader(
  identifier: string,
  invoiceNumber: string,
  rows: Record<string, unknown>[],
  overrides: HeaderOverrides = {},
): Record<string, unknown> {
  const flags = happyHeaderFlags();
  return {
    identifier: overrides.identifier ?? identifier,
    dateFrom: overrides.dateFrom ?? DATE_FROM,
    dateTo: overrides.dateTo ?? DATE_TO,
    billingPowerInKw: overrides.billingPowerInKw ?? 10,
    invoiceNumber: overrides.invoiceNumber ?? invoiceNumber,
    invoiceDate: overrides.invoiceDate ?? INVOICE_DATE,
    invoiceCorrection: overrides.invoiceCorrection ?? null,
    correction: overrides.correction ?? flags.correction,
    override: overrides.override ?? flags.override,
    saveRecordForIntermediatePeriod:
      overrides.saveRecordForIntermediatePeriod ?? flags.saveRecordForIntermediatePeriod,
    saveRecordForMeterReadings:
      overrides.saveRecordForMeterReadings ?? flags.saveRecordForMeterReadings,
    billingByScalesTableCreateRequests: overrides.billingByScalesTableCreateRequests ?? rows,
  };
}

type ScaleNomenclatureJson = {
  id?: number;
  scaleType?: string | null;
  scaleCode?: string | null;
  tariffOrScale?: string | null;
  tariffScale?: string | null;
};

async function loadScaleNomenclature(
  Request: Phn4048Fx['Request'],
  scaleId: number,
): Promise<ScaleNomenclatureJson> {
  const res = await Request.get(`${SCALES_PATH}/${scaleId}`);
  await expect(res, `GET /scales/${scaleId}`).CheckResponse();
  return (await res.json()) as ScaleNomenclatureJson;
}

/**
 * Unique POD + meter. Reuses existing grid operator / user type / country /
 * measurement type (POST unique GO/user-type hits ordering_id unique constraints
 * on Dev2). Creates XOR scale nomenclatures via Nomenclatures: scaleCode-only
 * and tariffOrScale-only (a single Scales row cannot hold both).
 */
export async function createBillingByScalesPrereqs(
  fx: Phn4048Fx,
  options: {
    tag: string;
    installmentDate?: string;
    removeDate?: string;
  },
): Promise<Phn4048Chain> {
  const { Request, Responses, GeneratePayload, Nomenclatures } = fx;
  const unique = uniqueDigits();
  const tag = options.tag;
  const identifier = uniquePodIdentifier(tag.replace(/\D/g, '') || '0');
  const meterNumber = uniqueMeterNumber(tag);

  const gridOperatorId = Number(await Nomenclatures.grid_operator(PHN_4048_GRID_OPERATOR));
  expect(gridOperatorId, 'Nomenclatures.grid_operator GIO').toBeGreaterThan(0);
  const measurementTypeId = Number(
    await Nomenclatures.measurement_type(PHN_4048_GRID_OPERATOR, gridOperatorId as unknown as string),
  );
  const scaleCodeId = Number(await Nomenclatures.scales_code('scaleCode'));
  const tariffScaleId = Number(await Nomenclatures.scales_tariff(PHN_4048_GRID_OPERATOR));
  expect(scaleCodeId, 'scales_code id').toBeGreaterThan(0);
  expect(tariffScaleId, 'scales_tariff id').toBeGreaterThan(0);

  const scaleCodeJson = await loadScaleNomenclature(Request, scaleCodeId);
  const tariffJson = await loadScaleNomenclature(Request, tariffScaleId);
  const scaleCode = String(scaleCodeJson.scaleCode ?? '').trim();
  const scaleType = String(scaleCodeJson.scaleType ?? '').trim();
  const tariffScale = String(tariffJson.tariffOrScale ?? tariffJson.tariffScale ?? '').trim();
  const tariffScaleType = String(tariffJson.scaleType ?? '').trim();
  expect(scaleCode, 'scaleCode nomenclature scaleCode').toBeTruthy();
  expect(scaleType, 'scaleCode nomenclature scaleType').toBeTruthy();
  expect(tariffScale, 'tariff nomenclature tariffOrScale').toBeTruthy();
  expect(tariffScaleType, 'tariff nomenclature scaleType').toBeTruthy();

  const podPayload = GeneratePayload.pointsOfDelivery.pod_slp();
  podPayload.gridOperatorId = gridOperatorId;
  podPayload.measurementTypeId = measurementTypeId;
  podPayload.identifier = identifier;
  podPayload.name = `BDS POD ${tag}`;
  const podRes = await Request.post(Endpoints.pod, { data: podPayload });
  await expect(podRes, `POST /pod (${tag})`).CheckResponse();
  const podBody = (await podRes.json()) as { id?: number };
  const podId = asPositiveId(podBody, 'podId');
  Responses.pod.push({ ...podBody, id: podId, identifier, gridOperatorId });

  const meterPayload = GeneratePayload.pointsOfDelivery.meters() as unknown as {
    number: string;
    gridOperatorId: number | null;
    podId: number | null;
    installmentDate: string;
    removeDate: string;
    meterScales: number[];
  };
  meterPayload.number = meterNumber;
  meterPayload.gridOperatorId = gridOperatorId;
  meterPayload.podId = podId;
  meterPayload.installmentDate = options.installmentDate ?? METER_INSTALL;
  meterPayload.removeDate = options.removeDate ?? METER_REMOVE;
  meterPayload.meterScales = [scaleCodeId, tariffScaleId];
  const meterRes = await Request.post(Endpoints.meters, { data: meterPayload });
  await expect(meterRes, `POST /meters (${tag})`).CheckResponse();
  const meterId = asPositiveId(await meterRes.json(), 'meterId');
  Responses.meters.push({ id: meterId, number: meterNumber });

  return {
    tag,
    unique,
    gridOperatorId,
    scaleCodeId,
    tariffScaleId,
    scaleCode,
    scaleType,
    tariffScale,
    tariffScaleType,
    podId,
    identifier,
    meterId,
    meterNumber,
  };
}

export async function postBillingByScales(
  fx: Phn4048Fx,
  payload: Record<string, unknown>,
): Promise<number> {
  const res = await fx.Request.post(Endpoints.dataByScales, { data: payload });
  await expect(res, 'POST /billing-by-scales').CheckResponse();
  const id = asPositiveId(await res.json(), 'billingByScaleId');
  fx.Responses.dataByScales.push(id);
  return id;
}

export async function postBillingByScalesExpecting400(
  fx: Phn4048Fx,
  payload: Record<string, unknown>,
  messageSnippet: string,
): Promise<{ status: number; message: string }> {
  const res = await fx.Request.post(Endpoints.dataByScales, { data: payload });
  return expectHttp400Containing(res, messageSnippet);
}

export async function expectHttp400Containing(
  res: { status: () => number; text: () => Promise<string> },
  messageSnippet: string,
): Promise<{ status: number; message: string }> {
  const status = res.status();
  const rawText = await res.text().catch(() => '');
  let body: unknown = rawText;
  try {
    body = rawText ? JSON.parse(rawText) : rawText;
  } catch {
    body = rawText;
  }
  const message = extractApiErrorMessage(body, rawText);
  expect(status, `expected HTTP 400, got ${status}: ${message}`).toBe(400);
  expect(
    normalizeApostrophes(message),
    `$.message should contain "${messageSnippet}": ${message}`,
  ).toContain(normalizeApostrophes(messageSnippet));
  return { status, message };
}

export async function getBillingByScales(
  fx: Phn4048Fx,
  id: number,
): Promise<Record<string, unknown>> {
  const res = await fx.Request.get(`${Endpoints.dataByScales}/${id}`);
  await expect(res, `GET /billing-by-scales/${id}`).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function listByPodIdentifier(
  fx: Phn4048Fx,
  identifier: string,
): Promise<{ status: number; content: Record<string, unknown>[] }> {
  const query = new URLSearchParams({
    page: '0',
    size: '20',
    prompt: identifier,
    searchBy: 'POD_IDENTIFIER',
    exactMatch: 'true',
    direction: 'DESC',
    sortBy: 'ID',
  });
  const res = await fx.Request.get(`${Endpoints.dataByScales}/list?${query.toString()}`);
  await expect(res, 'GET /billing-by-scales/list').CheckResponse();
  const body = (await res.json()) as { content?: Record<string, unknown>[] };
  return { status: res.status(), content: body.content ?? [] };
}

export async function expectNoListRowForIdentifier(
  fx: Phn4048Fx,
  identifier: string,
): Promise<void> {
  const listed = await listByPodIdentifier(fx, identifier);
  const hits = listed.content.filter((row) => String(row.identifier ?? '') === identifier);
  expect(hits, `no persisted billing-by-scales list row for ${identifier}`).toHaveLength(0);
}

export async function acquireDataByScalesLock(fx: Phn4048Fx, id: number): Promise<void> {
  const res = await fx.Request.post(`locks/acquire?entityType=data-by-scales&entityId=${id}`);
  await expect(res, `POST /locks/acquire data-by-scales ${id}`).CheckResponse();
}

export async function putBillingByScales(
  fx: Phn4048Fx,
  payload: Record<string, unknown>,
): Promise<{ status: number; body: unknown; rawText: string }> {
  const res = await fx.Request.put(Endpoints.dataByScales, { data: payload });
  const status = res.status();
  if (status >= 200 && status < 300) {
    await expect(res, 'PUT /billing-by-scales').CheckResponse();
    return { status, body: await res.json(), rawText: '' };
  }
  const rawText = await res.text().catch(() => '');
  let body: unknown = rawText;
  try {
    body = rawText ? JSON.parse(rawText) : rawText;
  } catch {
    body = rawText;
  }
  return { status, body, rawText };
}

export async function deleteBillingByScales(fx: Phn4048Fx, id: number): Promise<void> {
  const res = await fx.Request.delete(`${Endpoints.dataByScales}/${id}`);
  await expect(res, `DELETE /billing-by-scales/${id}`).CheckResponse();
}

export function editTablePayload(
  id: number,
  chain: Phn4048Chain,
  overrides: ScaleCodeRowOverrides = {},
): Record<string, unknown> {
  return {
    id,
    saveRecordForIntermediatePeriod: true,
    saveRecordForMeterReadings: true,
    billingByScalesTableEditRequests: [scaleCodeRow(chain, overrides)],
  };
}

export function assertWithCheck(
  TestRunSummary: TestRunSummaryCollector,
  check: string,
  expectedResult: string,
  actualOnPass: string,
  fn: () => void,
): void {
  let passed = true;
  let assertionError: string | undefined;
  try {
    fn();
  } catch (err) {
    passed = false;
    assertionError = err instanceof Error ? err.message : String(err);
    throw err;
  } finally {
    TestRunSummary.recordCheck({
      check,
      expectedResult,
      actualResult: passed ? actualOnPass : `Not as expected — ${assertionError ?? ''}`.trim(),
      passed,
    });
  }
}
