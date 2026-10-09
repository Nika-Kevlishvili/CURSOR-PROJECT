/**
 * PDT-3223 — invoice document JSON `DD[].TablePCScales[].ListPC[]` ordered by
 * price-component **Name** ASC then **Id** ASC.
 *
 * JSON `PC` is `pc.invoice_and_template_text` (Swagger `displayName` on create).
 * Sort key is `pc.name` (not displayName). Id is not in the JSON — discriminate
 * by displayName (+ ScaleCode when unique).
 *
 * Runtime: phoenix-core-lib origin/dev (`97960981d`, `b10206ed1`). Not on origin/dev2.
 *
 * Invoice DOCUMENT template: upload Test Comfort DOCX
 * `1005_Фактура_АП_МП_КИ_ДИ-заенергия_sum_20260805.docx` (POST
 * /template/upload-template-file + POST /template) and bind it as product
 * INVOICE_TEMPLATE so the generated PDF uses the same layout as PDT-3223
 * Valeri's Test billing-run 14392 screenshot.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts
 *   (scale PC / POD / meter without GET /pod; FOR_VOLUMES realize)
 * - tests/cursor/phn-3943-invoiced-month-empty-for-corrections.fixtures.ts
 *   (GET billing-run/generate-invoice-data)
 * - tests/billing/forVolumes/forVolumes.spec.ts (REG-873 data by scales)
 * - tests/cursor/PDT-2937-invoice-detailed-data-same-pc-name.spec.ts (same-name PCs)
 * - tests/cursor/dev-volume-invoice-custom-template.fixtures.ts
 *   (upload DOCX + POST /template + product.INVOICE_TEMPLATE)
 * - tests/cursor/phn-3951-cancelled-interim-invoice-price-component.fixtures.ts
 *   (GET /invoice/download-document + PDF text extract)
 *
 * Swagger (dev2, refreshed via phoenix2-dev /v3/api-docs): GET /billing-run/generate-invoice-data (operationId
 * generateData, deprecated) → BillingRunDocumentModelImpl.DD →
 * BillingRunDocumentDetailedDataModel.TablePCScales →
 * BillingRunDocumentDetailedDataPriceComponentScale.ListPC →
 * BillingRunDocumentDetailedDataPriceComponentModel.PC / ScaleCode.
 * POST /price-components PriceComponentRequest: name, displayName,
 * applicationModelRequest.applicationType = BY_SCALES,
 * formulaRequest.issuedSeparateInvoice = INVOICE_ONE.
 * POST /template/upload-template-file, POST /template TemplateCreateRequest
 * (templateType=DOCUMENT, templatePurpose=INVOICE, language=BULGARIAN),
 * ProductCreateRequest.templateIds TemplateSubObjectRequest.templateType=INVOICE_TEMPLATE,
 * GET /invoice/download-document?invoiceDocumentId=.
 */

import * as fs from 'fs';
import path from 'path';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { scaleCode as scaleCodePayloadTemplate } from '../../jsons/payloads/create/energyData/dataByScales(ScaleCode)';
import { randomGens } from '../../utils/randomGens';
import {
  downloadInvoiceDocumentBuffer,
  extractPdfText,
  pollInvoiceFileRefs,
  savePdfToTestResults,
} from './phn-3951-cancelled-interim-invoice-price-component.fixtures';

export const PDT_3223_KEY = 'PDT-3223';
export const PDT_3223_TITLE =
  'Order price componenst in the invoice json in DD.TablePCScales.ListPC.PC to be ordered by Name ASC and then by Id ASC';

export const PDT_3223_GRID_OPERATOR = 'GIO';
export const PDT_3223_PORTAL_BASE = 'https://devapps.energo-pro.bg/app/phoenix1-dev';

/** Swagger path: GET /billing-run/generate-invoice-data (operationId: generateData, deprecated). */
export const GENERATE_INVOICE_DATA_PATH = 'billing-run/generate-invoice-data';

/**
 * Test Comfort invoice DOCX (same family as template 1005 / Фактура - Енергия).
 * Override with PDT_3223_INVOICE_DOCX_PATH when the file lives elsewhere.
 */
export const PDT_3223_INVOICE_DOCX_PATH =
  process.env.PDT_3223_INVOICE_DOCX_PATH?.trim() ||
  '/Users/lukachrikishvili/Downloads/1005_Фактура_АП_МП_КИ_ДИ-заенергия_sum_20260805.docx';

/**
 * Create order is intentional so Id order ≠ Name order ≠ displayName order.
 * Expected ListPC PC sequence after Name ASC then Id ASC: #2, #3, #1.
 */
export const PDT_3223_PRICE_COMPONENTS = [
  {
    createOrder: 1,
    name: 'Zeta-PDT3223',
    displayName: 'AAA-display-if-sorted-by-shown-name',
    expression: '10',
  },
  {
    createOrder: 2,
    name: 'Alpha-PDT3223',
    displayName: 'ZZZ-display-if-sorted-by-shown-name',
    expression: '20',
  },
  {
    createOrder: 3,
    name: 'Alpha-PDT3223',
    displayName: 'MMM-display-same-name-higher-id',
    expression: '30',
  },
] as const;

export const PDT_3223_EXPECTED_LIST_PC_SEQUENCE = [
  PDT_3223_PRICE_COMPONENTS[1].displayName,
  PDT_3223_PRICE_COMPONENTS[2].displayName,
  PDT_3223_PRICE_COMPONENTS[0].displayName,
] as const;

export type Pdt3223Fx = Pick<
  baseFixture,
  | 'Request'
  | 'FileUploadRequest'
  | 'GeneratePayload'
  | 'Responses'
  | 'Endpoints'
  | 'Nomenclatures'
>;

export type Pdt3223PeriodWindow = {
  periodFrom: string;
  periodTo: string;
};

export type Pdt3223CreatedPriceComponent = {
  createOrder: number;
  id: number;
  name: string;
  displayName: string;
};

export type Pdt3223PrechainResult = {
  period: Pdt3223PeriodWindow;
  podIdentifier: string;
  contractId: number;
  priceComponents: Pdt3223CreatedPriceComponent[];
  scaleCodeValue: string | null;
  invoiceTemplateId: number;
  invoiceTemplateName: string;
  uploadedFileId: number;
};

/** Swagger BillingRunDocumentDetailedDataPriceComponentModel (PC, ScaleCode). */
export type Pdt3223ListPcItem = {
  PC?: string;
  ScaleCode?: string;
};

export type Pdt3223TablePcScales = {
  PeriodFrom?: string;
  PeriodTo?: string;
  Meter?: string;
  Product?: string;
  ListPC?: Pdt3223ListPcItem[];
};

export type Pdt3223DetailedData = {
  PODID?: string;
  TablePCScales?: Pdt3223TablePcScales[];
};

export type Pdt3223ListPcMatch = {
  ddIndex: number;
  tableIndex: number;
  listPc: Pdt3223ListPcItem[];
};

/** POST /price-components returns a raw long in many environments — not always `{ id }`. */
function asPriceComponentId(stored: unknown): number {
  if (typeof stored === 'number' && Number.isFinite(stored)) return stored;
  if (stored !== null && typeof stored === 'object' && 'id' in stored) {
    const n = Number((stored as { id: unknown }).id);
    if (Number.isFinite(n)) return n;
  }
  throw new Error(
    'Could not resolve price component id: expected number or { id } from POST /price-components body.',
  );
}

export function computePdt3223BillingMonth(reference = new Date()): Pdt3223PeriodWindow {
  const year = reference.getUTCFullYear();
  const monthIndex = reference.getUTCMonth();
  const pad = (n: number) => String(n).padStart(2, '0');
  const lastDay = new Date(Date.UTC(year, monthIndex + 1, 0)).getUTCDate();
  return {
    periodFrom: `${year}-${pad(monthIndex + 1)}-01`,
    periodTo: `${year}-${pad(monthIndex + 1)}-${pad(lastDay)}`,
  };
}

export function buildPdt3223BillingRunPdfPreviewLink(billingRunId: number): string {
  const env = process.env.FRONTEND_BASE_URL?.trim();
  const root = (env || PDT_3223_PORTAL_BASE).replace(/\/+$/, '');
  return `${root}/billing-run/pdf-documents?type=STANDARD_BILLING&id=${billingRunId}`;
}

export function asBillingRunId(stored: unknown): number {
  if (typeof stored === 'number' && Number.isFinite(stored) && stored > 0) return stored;
  if (stored !== null && typeof stored === 'object' && 'id' in stored) {
    const n = Number((stored as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) return n;
  }
  throw new Error('Could not resolve billing run id from Responses.billingRun.');
}

function asTemplateId(stored: unknown): number {
  if (typeof stored === 'number' && Number.isFinite(stored) && stored > 0) return stored;
  if (stored !== null && typeof stored === 'object' && 'id' in stored) {
    const n = Number((stored as { id: unknown }).id);
    if (Number.isFinite(n) && n > 0) return n;
  }
  throw new Error('Could not resolve template id from POST /template body.');
}

/**
 * POST /template/upload-template-file?fileFormats=DOCX then POST /template
 * (TemplateCreateRequest: DOCUMENT / INVOICE / ACTIVE / BULGARIAN / PDF).
 */
export async function uploadPdt3223InvoiceDocumentTemplate(
  ctx: Pick<Pdt3223Fx, 'Request' | 'FileUploadRequest' | 'Responses' | 'Endpoints'>,
  docxAbsolutePath: string = PDT_3223_INVOICE_DOCX_PATH,
): Promise<{ templateId: number; templateName: string; fileId: number }> {
  const { Request, FileUploadRequest, Responses, Endpoints } = ctx;

  if (!fs.existsSync(docxAbsolutePath)) {
    throw new Error(`PDT-3223 invoice DOCX not found: ${docxAbsolutePath}`);
  }

  const fileName = path.basename(docxAbsolutePath);
  const fileBuffer = fs.readFileSync(docxAbsolutePath);

  let fileId = 0;
  await test.step('Upload Comfort invoice DOCX (POST template/upload-template-file)', async () => {
    const upload = await FileUploadRequest.post(
      'template/upload-template-file?fileFormats=DOCX',
      {
        multipart: {
          file: {
            name: fileName,
            mimeType:
              'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            buffer: fileBuffer,
          },
        },
      },
    );
    await expect(upload).CheckResponse();
    fileId = asTemplateId(await upload.json());
  });

  const templateName = `PDT-3223 Comfort invoice ${randomGens.generateRandomString(true, true, 8)}`;
  const createPayload = {
    name: templateName,
    templateType: 'DOCUMENT' as const,
    templateStatus: 'ACTIVE' as const,
    templatePurpose: 'INVOICE' as const,
    defaultGoodsOrderDocument: null,
    defaultGoodsOrderEmail: null,
    defaultLatePaymentFineDocument: null,
    defaultLatePaymentFineEmail: null,
    language: 'BULGARIAN' as const,
    subject: null,
    quantity: null,
    customerTypes: null,
    consumptionPurposes: null,
    outputFileFormat: ['PDF'] as const,
    fileNames: [
      'CUSTOMER_IDENTIFIER',
      'CUSTOMER_NAME',
      'CUSTOMER_NUMBER',
      'DOCUMENT_NUMBER',
      'FILE_ID',
      'TIMESTAMP',
    ],
    fileNamePrefix: null,
    fileNameSuffix: null,
    fileSignings: ['SIGNING_WITH_SYSTEM_CERTIFICATE'] as const,
    fileId,
  };

  let templateId = 0;
  await test.step('Create ACTIVE INVOICE DOCUMENT template (POST /template)', async () => {
    const created = await Request.post(Endpoints.template, { data: createPayload });
    await expect(created).CheckResponse();
    const body = await created.json();
    templateId = asTemplateId(body);
    Responses.template.push(
      typeof body === 'object' && body !== null
        ? { ...body, name: templateName, fileId }
        : { id: templateId, name: templateName, fileId },
    );
  });

  return { templateId, templateName, fileId };
}

/** First-occurrence order of unique PC display-name markers in extracted PDF text. */
export function orderOfDisplayNamesInText(text: string, names: readonly string[]): string[] {
  const haystack = text.replace(/\s+/g, ' ');
  return [...names]
    .map((n) => ({ n, i: haystack.indexOf(n) }))
    .filter((x) => x.i >= 0)
    .sort((a, b) => a.i - b.i)
    .map((x) => x.n);
}

export async function downloadPdt3223InvoicePdfAndReadPrintedPcOrder(
  Request: Pdt3223Fx['Request'],
  invoiceId: number,
): Promise<{
  pdfPath: string;
  invoiceDocumentId: number;
  extractor: string;
  printedPcSequence: string[];
  pdfTextSnippet: string;
}> {
  const { files } = await pollInvoiceFileRefs(Request, invoiceId);
  expect(
    files.length,
    `invoice ${invoiceId} must have ≥1 file[] document after generation`,
  ).toBeGreaterThan(0);
  const invoiceDocumentId = files[0].id;
  const buffer = await downloadInvoiceDocumentBuffer(Request, invoiceDocumentId);
  const pdfPath = savePdfToTestResults(buffer, `PDT-3223-invoice-${invoiceId}.pdf`);
  const { text, extractor } = extractPdfText(pdfPath);
  const printedPcSequence = orderOfDisplayNamesInText(text, PDT_3223_EXPECTED_LIST_PC_SEQUENCE);
  return {
    pdfPath,
    invoiceDocumentId,
    extractor,
    printedPcSequence,
    pdfTextSnippet: text.slice(0, 2000),
  };
}

/**
 * GET billing-run/generate-invoice-data?invoiceId= → BillingRunDocumentModelImpl
 * (same helper shape as PHN-3943; local copy to avoid coupling).
 */
export async function fetchInvoiceDocumentModel(
  Request: Pdt3223Fx['Request'],
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${GENERATE_INVOICE_DATA_PATH}?invoiceId=${invoiceId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function findListPcContainingDisplayNames(
  doc: Record<string, unknown>,
  displayNames: readonly string[],
): Pdt3223ListPcMatch {
  const dd = doc.DD;
  expect(Array.isArray(dd) && (dd as unknown[]).length > 0, 'DD must be a non-empty array').toBe(
    true,
  );
  const rows = dd as Pdt3223DetailedData[];
  for (let i = 0; i < rows.length; i++) {
    const tables = rows[i].TablePCScales ?? [];
    for (let j = 0; j < tables.length; j++) {
      const list = tables[j].ListPC ?? [];
      const pcs = list.map((x) => String(x.PC ?? ''));
      if (displayNames.every((n) => pcs.includes(n))) {
        return { ddIndex: i, tableIndex: j, listPc: list };
      }
    }
  }
  throw new Error(
    `No TablePCScales.ListPC contained all expected PC display names ${JSON.stringify(displayNames)}. ` +
      `DD=${JSON.stringify(dd)}`,
  );
}

async function createPdt3223ScalePriceComponent(
  ctx: Pdt3223Fx,
  def: (typeof PDT_3223_PRICE_COMPONENTS)[number],
  scaleId: number,
): Promise<Pdt3223CreatedPriceComponent> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = GeneratePayload.productAndServices.scaleComponent();
  payload.name = def.name;
  payload.displayName = def.displayName;
  payload.formulaRequest.expression = def.expression;
  payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_ONE';
  payload.applicationModelRequest.applicationType = 'BY_SCALES';
  payload.applicationModelRequest.volumesByScaleRequest.scaleIds = [scaleId];

  const res = await Request.post(Endpoints.priceComponent, { data: payload });
  await expect(res).CheckResponse();
  const id = asPriceComponentId(await res.json());
  Responses.priceComponent.push(id);
  return {
    createOrder: def.createOrder,
    id,
    name: def.name,
    displayName: def.displayName,
  };
}

/**
 * Scale POD + meter without GET /pod (token 403 ACCESS_DENIED) — PDT-3072 pattern.
 */
async function createPdt3223ScalePodWithMeter(ctx: Pdt3223Fx): Promise<string> {
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
  Responses.meters.push(await meterRes.json());
  return identifier;
}

type ScaleJson = {
  id?: number;
  scaleType?: string | null;
  scaleCode?: string | null;
  tariffOrScale?: string | null;
};

async function loadMeterScaleCode(
  Request: Pdt3223Fx['Request'],
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
    await expect(scaleGet, `PDT-3223 GET scales/${scaleId}`).CheckResponse();
    scaleDetails.push((await scaleGet.json()) as ScaleJson);
  }

  const scaleCodeJson =
    scaleDetails.find((s) => Boolean(s.scaleCode) && !s.tariffOrScale) ??
    scaleDetails.find((s) => Boolean(s.scaleCode));
  if (!scaleCodeJson?.scaleCode) {
    throw new Error(
      `PDT-3223: meter ${meterId} has no code scale with scaleCode ` +
        `(scales=${JSON.stringify(scaleDetails)})`,
    );
  }

  const meterNumber = String(meterJson.number ?? '');
  expect(meterNumber, 'meter number from GET /meters/{id}').toBeTruthy();
  return { meterNumber, scaleCode: scaleCodeJson };
}

/**
 * Energy data by scales without GET /pod — identifier from create (PDT-3072).
 * One full-month scaleCode row (REG-873 / GeneratePayload.energyData.scaleCode shape).
 */
async function postPdt3223EnergyDataByScales(
  ctx: Pdt3223Fx,
  period: Pdt3223PeriodWindow,
): Promise<{ scaleDataId: number; scaleCodeValue: string }> {
  const { Request, Responses, Endpoints } = ctx;
  const meterId = Responses.meters[0] as number;
  const { meterNumber, scaleCode } = await loadMeterScaleCode(Request, meterId);

  const podEntry = Responses.pod[0] as { identifier?: string };
  const identifier = String(podEntry.identifier ?? '').trim();
  expect(
    identifier,
    'PDT-3223: Responses.pod[0] must carry identifier from create (POD GET is 403)',
  ).toBeTruthy();

  const payload = scaleCodePayloadTemplate() as Record<string, unknown>;
  payload.identifier = identifier;
  payload.dateFrom = period.periodFrom;
  payload.dateTo = period.periodTo;
  payload.invoiceDate = `${period.periodFrom}T00:00:00.000Z`;
  payload.invoiceNumber = '1';
  payload.correction = false;
  payload.invoiceCorrection = null;

  const rows = payload.billingByScalesTableCreateRequests as Array<Record<string, unknown>>;
  rows[0].periodFrom = period.periodFrom;
  rows[0].periodTo = period.periodTo;
  rows[0].meterNumber = meterNumber;
  rows[0].scaleCode = scaleCode.scaleCode;
  rows[0].scaleType = scaleCode.scaleType;

  const res = await Request.post(Endpoints.dataByScales, { data: payload });
  await expect(res, 'PDT-3223 POST billing-by-scales').CheckResponse();
  const scaleDataId = Number(await res.json());
  Responses.dataByScales.push(scaleDataId);
  return { scaleDataId, scaleCodeValue: String(scaleCode.scaleCode) };
}

export async function runPdt3223ScaleInvoiceJsonPrechain(
  ctx: Pdt3223Fx,
  invoiceTemplateId: number,
): Promise<Pdt3223PrechainResult> {
  const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = ctx;
  const period = computePdt3223BillingMonth();
  expect(invoiceTemplateId, 'invoiceTemplateId must be a positive id').toBeGreaterThan(0);

  const customer = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(customer).CheckResponse();
  Responses.customer.push(await customer.json());

  const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
  await expect(term).CheckResponse();
  Responses.terms.push(await term.json());

  await Nomenclatures.grid_operator(PDT_3223_GRID_OPERATOR);
  const scaleCodeId = await Nomenclatures.scales_code('scaleCode');

  const priceComponents: Pdt3223CreatedPriceComponent[] = [];
  for (const def of PDT_3223_PRICE_COMPONENTS) {
    priceComponents.push(await createPdt3223ScalePriceComponent(ctx, def, scaleCodeId));
  }
  expect(priceComponents[0].id, 'PC #1 Id must be lower than PC #2').toBeLessThan(
    priceComponents[1].id,
  );
  expect(priceComponents[1].id, 'PC #2 Id must be lower than PC #3 (same Name, Id ASC)').toBeLessThan(
    priceComponents[2].id,
  );

  const productPayload = GeneratePayload.productAndServices.product();
  productPayload.priceComponentIds = priceComponents.map((pc) => pc.id);
  productPayload.templateIds = [
    {
      templateId: envVariables.invoice_email_template,
      templateType: 'EMAIL_TEMPLATE',
    },
    {
      templateId: invoiceTemplateId,
      templateType: 'INVOICE_TEMPLATE',
    },
    {
      templateId: envVariables.product_contract_template,
      templateType: 'CONTRACT_TEMPLATE',
    },
  ];
  const product = await Request.post(Endpoints.product, { data: productPayload });
  await expect(product).CheckResponse();
  Responses.product.push(await product.json());
  const requestInvoiceTpl = productPayload.templateIds.find(
    (t: { templateType?: string }) => t.templateType === 'INVOICE_TEMPLATE',
  );
  expect(
    Number(requestInvoiceTpl?.templateId),
    'product payload INVOICE_TEMPLATE must be the uploaded Comfort template',
  ).toBe(invoiceTemplateId);

  const podIdentifier = await createPdt3223ScalePodWithMeter(ctx);

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
  expect(contractId).toBeGreaterThan(0);

  const podActivation = await Request.post('/contract-pods/manual', {
    data: await GeneratePayload.pointsOfDelivery.pod_activation(0, period.periodFrom),
  });
  await expect(podActivation).CheckResponse();

  const energy = await postPdt3223EnergyDataByScales(ctx, period);

  const templateRow = Responses.template[Responses.template.length - 1] as
    | { name?: string; fileId?: number }
    | undefined;

  return {
    period,
    podIdentifier,
    contractId,
    priceComponents,
    scaleCodeValue: energy.scaleCodeValue,
    invoiceTemplateId,
    invoiceTemplateName: String(templateRow?.name ?? ''),
    uploadedFileId: Number(templateRow?.fileId ?? 0),
  };
}

export async function runPdt3223StandardBillingAndRealize(ctx: Pdt3223Fx): Promise<{
  billingRunId: number;
  invoiceId: number;
}> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  Responses.billingRun.push(await billingRun.json());
  const billingRunId = asBillingRunId(Responses.billingRun[Responses.billingRun.length - 1]);

  await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);

  const invoiceId = Responses.invoice[Responses.invoice.length - 1] as number;
  expect(invoiceId, 'realized invoice id').toBeGreaterThan(0);

  return { billingRunId, invoiceId };
}

export function assertPdt3223ListPcOrder(match: Pdt3223ListPcMatch): string[] {
  const actual = match.listPc.map((row) => String(row.PC ?? ''));
  expect(
    match.listPc.length,
    `ListPC length must be >= 3 (created PCs). actual=${JSON.stringify(match.listPc)}`,
  ).toBeGreaterThanOrEqual(3);
  expect(
    actual,
    `ListPC.PC must be Name ASC then Id ASC (JSON PC = displayName). ` +
      `Wrong displayName sort would be AAA, MMM, ZZZ; insert order AAA, ZZZ, MMM. ` +
      `actual=${JSON.stringify(actual)} table=${match.ddIndex}/${match.tableIndex}`,
  ).toEqual([...PDT_3223_EXPECTED_LIST_PC_SEQUENCE]);
  return actual;
}
