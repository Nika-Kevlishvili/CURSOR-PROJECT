/**
 * DEV/TEST-DATA — FOR_VOLUMES (STANDARD_BILLING) draft volume invoice using a custom
 * invoice DOCUMENT template uploaded from a fixed .docx path (Downloads).
 *
 * Flow: upload template file → create ACTIVE INVOICE DOCUMENT template →
 * billed customer → priceSettlement → terms → pod_settlement → product
 * (INVOICE_TEMPLATE = new template id) → product_contract → POD activation →
 * BBP (15-minute + CET + MIN) → billing run FOR_VOLUMES → start-billing only
 * (no start-generating / start-accounting).
 *
 * Portal preview links resolve from `process.env.BASE_URL` (Dev / Test / Dev2).
 *
 * Reference spec(s):
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 * - tests/cursor/phn-362-wait-yes-split-month-anchor.fixtures.ts (product templateIds)
 * - jsons/payloads/create/nomenclatures/templates.ts (upload + POST /template)
 *
 * Swagger (dev): POST /template/upload-template-file, POST /template
 * (TemplateCreateRequest), POST /products (templateIds / TemplateSubObjectRequest),
 * POST /billing-run, GET /billing-run/draft-invoices.
 */

import * as fs from 'fs';
import path from 'path';
import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import { postPdt2915BillingByProfile } from './pdt-2915-invoice-correction-deleted-bbp.fixtures';
import {
  DEV_PORTAL_BASE,
  DEV_DATA_KEY,
  buildDevBillingRunPreviewLink,
  buildDevInvoicePreviewLink,
  createForVolumesDraftBillingRun,
  monthStartFromPeriod,
} from './dev-volume-billing-two-compensations.fixtures';

export {
  DEV_PORTAL_BASE,
  DEV_DATA_KEY,
  buildDevBillingRunPreviewLink,
  buildDevInvoicePreviewLink,
  createForVolumesDraftBillingRun,
  monthStartFromPeriod,
};

/** Portal UI bases (no trailing slash) — aligned with playwright-environments + ResponseLinker. */
export const TEST_PORTAL_BASE = 'https://testapps.energo-pro.bg/app/phoenix-epres';
export const DEV2_PORTAL_BASE = 'https://devapps.energo-pro.bg/app/phoenix-dev2';

function stripTrailingSlash(url: string): string {
  return url.replace(/\/+$/, '');
}

/**
 * Resolve Phoenix portal UI base from API `BASE_URL` (or optional `FRONTEND_BASE_URL`).
 * Defaults to Dev phoenix1-dev when unset / unrecognized (same as DEV_PORTAL_BASE).
 */
export function resolvePortalBase(
  baseUrl: string = process.env.FRONTEND_BASE_URL || process.env.BASE_URL || '',
): string {
  const normalized = stripTrailingSlash((baseUrl || '').trim());
  if (!normalized) {
    return DEV_PORTAL_BASE;
  }

  // Already a known portal root
  if (
    normalized === DEV_PORTAL_BASE ||
    normalized === TEST_PORTAL_BASE ||
    normalized === DEV2_PORTAL_BASE
  ) {
    return normalized;
  }

  // Test
  if (
    normalized === 'https://testapps.energo-pro.bg/backend/phoenix-epres' ||
    normalized.includes('/backend/phoenix-epres') ||
    normalized.includes('/app/phoenix-epres')
  ) {
    return TEST_PORTAL_BASE;
  }

  // Dev2
  if (
    normalized === 'https://devapps.energo-pro.bg/backend/phoenix2-dev' ||
    normalized.includes('/backend/phoenix2-dev') ||
    normalized.includes('/app/phoenix-dev2') ||
    normalized.startsWith('http://10.236.20.31:8091')
  ) {
    return DEV2_PORTAL_BASE;
  }

  // Dev (IP API or phoenix1-dev backend/app)
  if (
    normalized === 'http://10.236.20.11:8091' ||
    normalized.startsWith('http://10.236.20.11:8091') ||
    normalized.includes('/backend/phoenix1-dev') ||
    normalized.includes('/app/phoenix1-dev')
  ) {
    return DEV_PORTAL_BASE;
  }

  return DEV_PORTAL_BASE;
}

/** Billing-run preview link using resolved portal base (optional override). */
export function buildBillingRunPreviewLink(
  billingRunId: number,
  portalBase: string = resolvePortalBase(),
): string {
  const root = stripTrailingSlash(portalBase);
  return `${root}/billing-run/preview/basic-parameters?id=${billingRunId}`;
}

/** Invoice preview link using resolved portal base (optional override). */
export function buildInvoicePreviewLink(
  invoiceId: number,
  portalBase: string = resolvePortalBase(),
): string {
  const root = stripTrailingSlash(portalBase);
  return `${root}/billing-run/invoices/preview/basic-parameters?id=${invoiceId}`;
}

/**
 * Default Downloads .docx (PDT-3144 failing template `(4)`).
 * Override with env CUSTOM_INVOICE_DOCX_PATH when needed.
 */
export const CUSTOM_INVOICE_DOCX_PATH =
  process.env.CUSTOM_INVOICE_DOCX_PATH?.trim() ||
  'c:\\Users\\N.kevlishvili\\Downloads\\1005_Фактура_АП_МП_КИ_ДИ-заенергия_sum_20260720 (4).docx';

export type DevVolCustomTplFx = Pick<
  baseFixture,
  'Request' | 'FileUploadRequest' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type DevVolCustomTplPrechainResult = {
  customerId: number;
  podId: number;
  contractId: number;
  productId: number;
  bbpId: number;
  periodFrom: string;
  periodTo: string;
  documentPeriod: string;
  invoiceTemplateId: number;
  invoiceTemplateName: string;
  uploadedFileId: number;
};

export type DevVolCustomTplScenarioResult = DevVolCustomTplPrechainResult & {
  billingRunId: number;
  draftInvoiceIds: number[];
  billingRunStatus: string;
};

function asEntityId(raw: unknown): number {
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

/**
 * Upload the fixed Downloads .docx then POST /template as ACTIVE DOCUMENT / INVOICE.
 * Returns created template id + name + uploaded file id.
 */
export async function uploadAndCreateCustomInvoiceDocumentTemplate(
  ctx: Pick<DevVolCustomTplFx, 'Request' | 'FileUploadRequest' | 'Responses' | 'Endpoints'>,
  docxAbsolutePath: string = CUSTOM_INVOICE_DOCX_PATH,
): Promise<{ templateId: number; templateName: string; fileId: number }> {
  const { Request, FileUploadRequest, Responses, Endpoints } = ctx;

  if (!fs.existsSync(docxAbsolutePath)) {
    throw new Error(`Custom invoice template file not found: ${docxAbsolutePath}`);
  }

  const fileName = path.basename(docxAbsolutePath);
  const fileBuffer = fs.readFileSync(docxAbsolutePath);

  let fileId = 0;
  await test.step('Upload custom invoice DOCX (POST template/upload-template-file)', async () => {
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
    const uploaded = (await upload.json()) as { id?: number };
    fileId = asEntityId(uploaded);
  });

  const templateName = `(DEV-DATA) volume invoice template ${randomGens.generateRandomString(true, true, 8)}`;
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
    templateId = asEntityId(body);
    Responses.template.push(
      typeof body === 'object' && body !== null
        ? { ...body, name: templateName, fileId }
        : { id: templateId, name: templateName, fileId },
    );
  });

  return { templateId, templateName, fileId };
}

/**
 * Contract + BBP prechain for volume draft billing, with product.INVOICE_TEMPLATE
 * pointing at the newly created custom document template (not envVariables.invoice_document_template).
 * Government compensations / second recipient customer are intentionally omitted.
 */
export async function runDevVolumeInvoiceCustomTemplatePrechain(
  ctx: DevVolCustomTplFx,
  invoiceTemplateId: number,
): Promise<Omit<DevVolCustomTplPrechainResult, 'invoiceTemplateId' | 'invoiceTemplateName' | 'uploadedFileId'>> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  await test.step('Precondition: billed customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: price component (priceSettlement, 15-min profile)', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = '100';
    payload.applicationModelRequest.settlementPeriodsRequest.profiles = [
      { profileId: envVariables.profiles, percentage: 100 },
    ];
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());
  });

  await test.step('Precondition: terms', async () => {
    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());
  });

  await test.step('Precondition: POD settlement', async () => {
    const pod = await Request.post(Endpoints.pod, {
      data: GeneratePayload.pointsOfDelivery.pod_settlement(),
    });
    await expect(pod).CheckResponse();
    Responses.pod.push(await pod.json());
  });

  let productId = 0;
  await test.step('Precondition: product with custom INVOICE_TEMPLATE', async () => {
    if (!invoiceTemplateId || invoiceTemplateId <= 0) {
      throw new Error(`invoiceTemplateId must be a positive id, got: ${invoiceTemplateId}`);
    }
    const productPayload = GeneratePayload.productAndServices.product();
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
    const body = await product.json();
    Responses.product.push(body);
    productId = asEntityId(body);

    const requestInvoiceTpl = productPayload.templateIds?.find(
      (t: { templateType?: string }) => t.templateType === 'INVOICE_TEMPLATE',
    );
    expect(
      Number(requestInvoiceTpl?.templateId),
      'product payload INVOICE_TEMPLATE must be the custom uploaded template',
    ).toBe(invoiceTemplateId);
    expect(
      Number(requestInvoiceTpl?.templateId),
      'INVOICE_TEMPLATE must not use envVariables.invoice_document_template',
    ).not.toBe(Number(envVariables.invoice_document_template));
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
  let periodFrom = '';
  let periodTo = '';
  await test.step('Precondition: billing-by-profile (15-minute + CET + MIN upload)', async () => {
    const payload = await GeneratePayload.energyData.profile15minute(0);
    payload.timeZone = 'CET';
    payload.profileId = envVariables.profiles;
    const bbp = await postPdt2915BillingByProfile(Request, payload, 'DEV-VOL-CUSTOM-TPL BBP');
    await GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');
    bbpId = bbp.id;
    periodFrom = String(payload.periodFrom);
    periodTo = String(payload.periodTo);
    Responses.dataByProfiles.push({
      id: bbp.id,
      periodFrom,
      periodTo,
      periodType: payload.periodType,
    });
  });

  const customerId = asEntityId(Responses.customer[0]);
  const podId = asEntityId(Responses.pod[0]);

  return {
    customerId,
    podId,
    contractId,
    productId,
    bbpId,
    periodFrom,
    periodTo,
    documentPeriod: monthStartFromPeriod(periodFrom),
  };
}

export function logDevVolCustomTplCreatedEntities(result: DevVolCustomTplScenarioResult): void {
  const portalBase = resolvePortalBase();
  const billingRunLink = buildBillingRunPreviewLink(result.billingRunId, portalBase);
  const invoiceLinks = result.draftInvoiceIds.map((id) =>
    buildInvoicePreviewLink(id, portalBase),
  );

  console.log(
    '\n========== DEV/TEST-DATA: volume invoice draft + custom invoice template ==========',
  );
  console.log(
    JSON.stringify(
      {
        portalBase,
        apiBaseUrl: process.env.BASE_URL ?? '',
        invoiceTemplateId: result.invoiceTemplateId,
        invoiceTemplateName: result.invoiceTemplateName,
        uploadedFileId: result.uploadedFileId,
        customerId: result.customerId,
        podId: result.podId,
        productId: result.productId,
        contractId: result.contractId,
        bbpId: result.bbpId,
        billingRunId: result.billingRunId,
        billingRunStatus: result.billingRunStatus,
        draftInvoiceIds: result.draftInvoiceIds,
        portal: {
          billingRun: billingRunLink,
          invoices: invoiceLinks,
          contract: `${portalBase}/energy-product-contracts/preview/basic-parameters?id=${result.contractId}`,
        },
      },
      null,
      2,
    ),
  );

  console.log(`PORTAL_BASE=${portalBase}`);
  console.log(`BILLING_RUN_ID=${result.billingRunId}`);
  console.log(`BILLING_RUN_LINK=${billingRunLink}`);
  console.log(`INVOICE_IDS=${result.draftInvoiceIds.join(',')}`);
  for (let i = 0; i < invoiceLinks.length; i++) {
    console.log(`INVOICE_LINK=${invoiceLinks[i]}`);
    console.log(`INVOICE_ID=${result.draftInvoiceIds[i]}`);
  }
}
