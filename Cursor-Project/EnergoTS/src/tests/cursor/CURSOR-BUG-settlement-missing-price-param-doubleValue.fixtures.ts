/**
 * CURSOR-BUG — reproduce settlement evaluation OGNL `target is null for method doubleValue`
 * on Dev FOR_VOLUMES billing (same runtime path as PreProd billing 4057).
 *
 * Isolated 15-minute price parameters are created for this test. Shared master prices
 * (e.g. 1001 / 1064) are never modified.
 *
 * Reference spec(s):
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 * - tests/cursor/dev-volume-invoice-custom-template.fixtures.ts
 * - tests/billing/forVolumes/SLP.spec.ts (15-minute price parameter + settlement PC)
 * - tests/cursor/pdt-2937-invoice-detailed-data-same-pc-name.fixtures.ts (BBP CET + MIN)
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts (start-billing poll)
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts (POST billing-by-profile)
 *
 * Swagger (dev, refreshed this session): POST /prices, POST /prices/import
 * (periodType=FIFTEEN_MINUTES, priceParameterId, priceParameterDetailsVersionId),
 * POST /price-components (PriceComponentFormulaRequest.expression + priceParameterIds),
 * POST /billing-by-profile, POST /billing-by-profile/import, POST /billing-run,
 * PATCH /billing-run/start-billing, PATCH /billing-run/download-error-report/{id}?protocol=BILLING.
 */

import * as fs from 'fs';
import * as ExcelJS from 'exceljs';
import { test, expect } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import { envVariables } from '../../backend/fixtures/envCashed';
import { randomGens } from '../../backend/utils/randomGens';

export const JIRA_KEY = 'CURSOR-BUG';
export const JIRA_TITLE =
  'Settlement evaluation error when a 15-minute price parameter is missing from the formula context';

export const BILLING_TIMEOUT_MS = 20 * 60 * 1000;
export const START_BILLING_POLL_MS = 12 * 60 * 1000;
export const PRICE_PARAMETER_DETAILS_VERSION_ID = 1;
export const FIFTEEN_MINUTES = 'FIFTEEN_MINUTES';
/** Match working FOR_VOLUMES + BBP specs (pdt-2937 / two-compensations): CET, not EET. */
export const TIME_ZONE_CET = 'CET';
export const ERROR_PROTOCOL_BILLING = 'BILLING';
export const BILLING_RUN_ROOT = 'billing-run';
/** Punch the price-parameter gap on day 5 so it sits inside a mid-month billed window (not day 10). */
export const GAP_DAY_OF_MONTH = 5;
export const GAP_HOUR = 12;
export const SETTLEMENT_ERROR_NEEDLE = 'target is null for method doubleValue';
export const SETTLEMENT_ERROR_PREFIX = 'settlement evaluation error';

const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
const PRICE_FILE = '15min.xlsx';

export type CursorBugFx = Pick<
  baseFixture,
  'Request' | 'FileUploadRequest' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type CursorBugScenarioResult = {
  customerId: number;
  podId: number;
  contractId: number;
  productId: number;
  fullPriceParameterId: number;
  gappedPriceParameterId: number;
  priceComponentId: number;
  bbpId: number;
  billingRunId: number;
  billingRunStatus: string;
  errorReportText: string;
};

function parseExcelPeriod(raw: string): Date | null {
  const match = /^(\d{2})\.(\d{2})\.(\d{4}) (\d{2}):(\d{2})$/.exec(raw.trim());
  if (!match) {
    return null;
  }
  return new Date(
    Number(match[3]),
    Number(match[2]) - 1,
    Number(match[1]),
    Number(match[4]),
    Number(match[5]),
    0,
    0,
  );
}

/**
 * Remove four consecutive 15-minute rows (one hour) from the shared `15min.xlsx`
 * produced by excelGens — same timestamp grid as the full price parameter / BBP file.
 */
export async function punchFourIntervalGapInPriceFile(): Promise<void> {
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(PRICE_FILE);
  const sheet = workbook.worksheets[0];
  expect(sheet, '15min.xlsx must contain a worksheet').toBeTruthy();

  let gapRow = 0;
  let firstNoonRow = 0;
  sheet!.eachRow((row, rowNumber) => {
    if (rowNumber === 1) {
      return;
    }
    const cellValue = row.getCell(1).value;
    const stamp =
      cellValue instanceof Date && !Number.isNaN(cellValue.getTime())
        ? cellValue
        : parseExcelPeriod(cellText(cellValue));
    if (!stamp || stamp.getHours() !== GAP_HOUR || stamp.getMinutes() !== 0) {
      return;
    }
    if (firstNoonRow === 0) {
      firstNoonRow = rowNumber;
    }
    if (gapRow === 0 && stamp.getDate() === GAP_DAY_OF_MONTH) {
      gapRow = rowNumber;
    }
  });
  if (gapRow === 0) {
    gapRow = firstNoonRow;
  }
  expect(
    gapRow,
    `15min.xlsx must contain a ${GAP_HOUR}:00 row so the price-parameter gap can be punched`,
  ).toBeGreaterThan(1);

  sheet!.spliceRows(gapRow, 4);
  await workbook.xlsx.writeFile(PRICE_FILE);
}

export function asEntityId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw) && raw > 0) {
    return raw;
  }
  if (typeof raw === 'string' && raw.trim() !== '' && Number.isFinite(Number(raw))) {
    const n = Number(raw);
    if (n > 0) {
      return n;
    }
  }
  if (raw && typeof raw === 'object' && 'id' in raw) {
    const n = Number((raw as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) {
      return n;
    }
  }
  throw new Error(`Expected numeric entity id, got: ${JSON.stringify(raw)}`);
}

export async function postPdt2915BillingByProfile(
  Request: CursorBugFx['Request'],
  payload: Record<string, unknown>,
  name: string,
): Promise<{ id: number }> {
  const data = { ...payload, identifier: payload.identifier ?? name };
  const res = await Request.post('billing-by-profile', { data });
  await expect(res).CheckResponse();
  return { id: asEntityId(await res.json()) };
}

export async function patchStartBilling(
  Request: CursorBugFx['Request'],
  billingRunId: number,
) {
  return Request.patch(`${BILLING_RUN_ROOT}/start-billing?billingRunId=${billingRunId}`);
}

export async function pollBillingRunForStatus(
  Request: CursorBugFx['Request'],
  billingRunId: number,
  status: string,
  timeoutMs: number,
): Promise<void> {
  const interval = 15_000;
  const deadline = Date.now() + timeoutMs;
  let lastStatus = '';
  while (Date.now() < deadline) {
    const res = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as { commonParameters?: { status?: string } };
    lastStatus = String(body.commonParameters?.status ?? '');
    if (lastStatus === status) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, interval));
  }
  throw new Error(
    `Billing run ${billingRunId} did not reach ${status} within ${timeoutMs}ms (last status=${lastStatus})`,
  );
}

function cellText(value: ExcelJS.CellValue): string {
  if (value == null) {
    return '';
  }
  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  if (typeof value === 'object' && 'text' in value && typeof (value as { text: unknown }).text === 'string') {
    return (value as { text: string }).text;
  }
  if (typeof value === 'object' && 'richText' in value && Array.isArray((value as { richText: unknown }).richText)) {
    return ((value as { richText: Array<{ text?: string }> }).richText)
      .map((part) => part.text ?? '')
      .join('');
  }
  return String(value);
}

export async function parseErrorReportText(buffer: Buffer): Promise<string> {
  expect(buffer.byteLength, 'billing error report must not be empty').toBeGreaterThan(0);
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buffer);
  const parts: string[] = [];
  for (const sheet of workbook.worksheets) {
    sheet.eachRow((row) => {
      row.eachCell((cell) => {
        const text = cellText(cell.value).trim();
        if (text) {
          parts.push(text);
        }
      });
    });
  }
  return parts.join('\n');
}

export async function importFifteenMinutePriceFile(
  FileUploadRequest: CursorBugFx['FileUploadRequest'],
  priceParameterId: number,
): Promise<void> {
  const fileBuffer = fs.readFileSync(PRICE_FILE);
  const res = await FileUploadRequest.post(
    `prices/import?periodType=${FIFTEEN_MINUTES}&priceParameterId=${priceParameterId}&priceParameterDetailsVersionId=${PRICE_PARAMETER_DETAILS_VERSION_ID}`,
    {
      multipart: {
        file: {
          name: PRICE_FILE,
          mimeType: XLSX_MIME,
          buffer: fileBuffer,
        },
      },
    },
  );
  await expect(res).CheckResponse();
}

export async function importFifteenMinuteProfileFile(
  FileUploadRequest: CursorBugFx['FileUploadRequest'],
  billingByProfileId: number,
  fileBuffer: Buffer = fs.readFileSync(PRICE_FILE),
): Promise<void> {
  const res = await FileUploadRequest.post(
    `billing-by-profile/import?periodType=${FIFTEEN_MINUTES}&billingByProfileId=${billingByProfileId}`,
    {
      multipart: {
        file: {
          name: PRICE_FILE,
          mimeType: XLSX_MIME,
          buffer: fileBuffer,
        },
      },
    },
  );
  await expect(res).CheckResponse();
}

export async function createFifteenMinutePriceParameter(
  ctx: CursorBugFx,
  name: string,
  punchGap: boolean,
): Promise<number> {
  const { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = await GeneratePayload.productAndServices.priceParameter15minute();
  payload.name = name;
  payload.periodType = FIFTEEN_MINUTES;
  payload.timeZone = TIME_ZONE_CET;

  const res = await Request.post(Endpoints.priceParameters, { data: payload });
  await expect(res).CheckResponse();
  const id = asEntityId(await res.json());
  Responses.priceParameters.push({ id, name, periodType: FIFTEEN_MINUTES, timeZone: TIME_ZONE_CET });
  if (punchGap) {
    await punchFourIntervalGapInPriceFile();
  }
  await importFifteenMinutePriceFile(FileUploadRequest, id);
  return id;
}

export async function createGappedSettlementPriceComponent(
  ctx: CursorBugFx,
  fullPriceParameterId: number,
  gappedPriceParameterId: number,
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = GeneratePayload.productAndServices.priceSettlement();
  const suffix = randomGens.generateRandomString(true, false, 8);
  payload.name = `CURSOR-BUG settlement ${suffix}`;
  payload.displayName = payload.name;
  payload.formulaRequest.expression = `$${fullPriceParameterId}$+$${gappedPriceParameterId}$`;
  payload.formulaRequest.priceParameterIds = [fullPriceParameterId, gappedPriceParameterId];
  payload.applicationModelRequest.applicationModelType = 'PRICE_AM_FOR_VOLUMES';
  payload.applicationModelRequest.applicationType = 'BY_SETTLEMENT_PERIODS';
  payload.applicationModelRequest.settlementPeriodsRequest.timeZone = TIME_ZONE_CET;
  payload.applicationModelRequest.settlementPeriodsRequest.profiles = [
    { profileId: envVariables.profiles, percentage: 100 },
  ];

  const res = await Request.post(Endpoints.priceComponent, { data: payload });
  await expect(res).CheckResponse();
  const id = asEntityId(await res.json());
  Responses.priceComponent.push(id);
  return id;
}

export async function runCursorBugPrechain(ctx: CursorBugFx): Promise<{
  customerId: number;
  podId: number;
  contractId: number;
  productId: number;
  fullPriceParameterId: number;
  gappedPriceParameterId: number;
  priceComponentId: number;
  bbpId: number;
}> {
  const { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints } = ctx;
  const stamp = randomGens.generateCurrentTimeStamp();

  await test.step('Precondition: customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: terms', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  const fullPriceParameterId = await test.step(
    'Precondition: 15-minute price parameter A (full month, default excelGens grid)',
    async () => createFifteenMinutePriceParameter(ctx, `CURSOR-BUG-PP-FULL-${stamp}`, false),
  );

  const gappedPriceParameterId = await test.step(
    'Precondition: 15-minute price parameter B (same grid, 4 intervals removed on day 5)',
    async () => createFifteenMinutePriceParameter(ctx, `CURSOR-BUG-PP-GAP-${stamp}`, true),
  );

  const priceComponentId = await test.step(
    'Precondition: settlement price component formula $A$+$B$',
    async () => createGappedSettlementPriceComponent(ctx, fullPriceParameterId, gappedPriceParameterId),
  );

  let productId = 0;
  await test.step('Precondition: product', async () => {
    const product = await Request.post(Endpoints.product, {
      data: GeneratePayload.productAndServices.product(),
    });
    await expect(product).CheckResponse();
    const body = await product.json();
    Responses.product.push(body);
    productId = asEntityId(body);
  });

  await test.step('Precondition: POD settlement', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  let contractId = 0;
  await test.step('Precondition: product contract', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    Responses.productContract.push(body);
    contractId = asEntityId(body);
  });

  await test.step('Precondition: activate POD on contract', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
    });
    await expect(podActivation).CheckResponse();
  });

  let bbpId = 0;
  await test.step('Precondition: billing-by-profile (full 15-minute month, CET)', async () => {
    const payload = await GeneratePayload.energyData.profile15minute(0);
    payload.timeZone = TIME_ZONE_CET;
    payload.profileId = envVariables.profiles;
    const fullProfileBuffer = fs.readFileSync(PRICE_FILE);
    const bbp = await postPdt2915BillingByProfile(Request, payload, 'CURSOR-BUG BBP');
    await importFifteenMinuteProfileFile(FileUploadRequest, bbp.id, fullProfileBuffer);
    bbpId = bbp.id;
    Responses.dataByProfiles.push({
      id: bbp.id,
      periodFrom: String(payload.periodFrom),
      periodTo: String(payload.periodTo),
      periodType: payload.periodType,
    });

    const periodFrom = encodeURIComponent(String(payload.periodFrom));
    const periodTo = encodeURIComponent(String(payload.periodTo));
    const getRes = await Request.get(
      `billing-by-profile/${bbp.id}?periodFrom=${periodFrom}&periodTo=${periodTo}`,
    );
    await expect(getRes).CheckResponse();
    const bbpBody = (await getRes.json()) as { entries?: unknown[] };
    expect(
      (bbpBody.entries ?? []).length,
      'BBP must have a full 15-minute series (not the gapped price-parameter file)',
    ).toBeGreaterThan(100);
  });

  return {
    customerId: asEntityId(Responses.customer[0]),
    podId: asEntityId(Responses.pod[0]),
    contractId,
    productId,
    fullPriceParameterId,
    gappedPriceParameterId,
    priceComponentId,
    bbpId,
  };
}

export async function createForVolumesBillingRunAndStartDraft(
  ctx: CursorBugFx,
): Promise<{ billingRunId: number; billingRunStatus: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  const startRes = await patchStartBilling(Request, billingRunId);
  await expect(startRes).CheckResponse();
  await pollBillingRunForStatus(Request, billingRunId, 'DRAFT', START_BILLING_POLL_MS);

  const statusRes = await Request.get(`${BILLING_RUN_ROOT}/${billingRunId}`);
  await expect(statusRes).CheckResponse();
  const statusBody = (await statusRes.json()) as { commonParameters?: { status?: string } };
  const billingRunStatus = String(statusBody.commonParameters?.status ?? '');
  return { billingRunId, billingRunStatus };
}

export async function downloadBillingErrorReportText(
  Request: CursorBugFx['Request'],
  billingRunId: number,
): Promise<string> {
  const res = await Request.patch(
    `${BILLING_RUN_ROOT}/download-error-report/${billingRunId}?protocol=${ERROR_PROTOCOL_BILLING}`,
  );
  await expect(res).CheckResponse();
  const buffer = Buffer.from(await res.body());
  return parseErrorReportText(buffer);
}
