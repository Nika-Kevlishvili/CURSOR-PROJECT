/**
 * PHN-3924 — CLONE - Templater: Unlicensed Templater version on TEST
 *
 * Bug-only automation (no TC .md): upload the Jira-attached invoice DOCX,
 * create a non-default BULGARIAN ACTIVE INVOICE DOCUMENT template, wire it
 * as product.INVOICE_TEMPLATE, complete a FOR_VOLUMES billing run, download
 * the invoice PDF, and assert [[DocumentType]] expands to Bulgarian
 * ("Фактура") — not English ("INVOICE").
 *
 * Jira attachment (read-only source, do not modify):
 *   Cursor-Project/config/jira/attachments/PHN-3924/Faktura_07.08.2025.docx
 *
 * Bug PDF evidence (Inv_202077922.pdf): header shows
 *   …ЕАД\nINVOICE\n№ 1000001071…
 * while appendix still has static "Към Фактура №" — so assertions MUST use
 * the DocumentType header pattern (line "Фактура"/"INVOICE" immediately
 * before "№ <docNumber>"), not a bare includes("Фактура").
 *
 * Reference spec(s):
 * - tests/cursor/dev-volume-invoice-custom-template.fixtures.ts
 *   (upload + POST /template + product.INVOICE_TEMPLATE prechain)
 * - tests/cursor/pdt-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts
 *   (invoiceTemplateId → INVOICE_TEMPLATE)
 * - tests/cursor/phn-3951-cancelled-interim-invoice-price-component.fixtures.ts
 *   (FileUploadRequest upload, PDF download + extractPdfText)
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts (startAndCompleteBillingRun)
 * - Phoenix TemplateControllerIT (INVOICE DOCUMENT create payload shape)
 *
 * Swagger (dev2):
 *   POST /template/upload-template-file?fileFormats=DOCX — multipart field `file`
 *   POST /template — TemplateCreateRequest
 *   GET  /invoice/download-document?invoiceDocumentId=
 *
 * Target env: Dev2 (BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev)
 */

import * as fs from 'fs';
import * as path from 'path';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import {
  runDevVolumeInvoiceCustomTemplatePrechain,
} from './dev-volume-invoice-custom-template.fixtures';
import {
  downloadInvoiceDocumentBuffer,
  extractPdfText,
  getInvoiceBody,
  pollInvoiceFileRefs,
  savePdfToTestResults,
} from './phn-3951-cancelled-interim-invoice-price-component.fixtures';
import {
  pollInvoiceListing,
  startAndCompleteBillingRun,
} from './rps-pod-invoice-due-date.fixtures';

export const PHN_3924_KEY = 'PHN-3924';
export const PHN_3924_TITLE =
  'CLONE - Templater: Unlicensed Templater version on TEST';

/** Original Jira attachment filename (traceability only). */
export const PHN_3924_TEMPLATE_ORIGINAL_NAME = 'Faktura_07.08.2025.docx';

/**
 * Absolute path to the bug-attached DOCX (workspace config — not modified).
 * Override with env PHN_3924_TEMPLATE_DOCX_PATH when needed.
 */
export const PHN_3924_TEMPLATE_DOCX_PATH =
  process.env.PHN_3924_TEMPLATE_DOCX_PATH?.trim() ||
  path.resolve(
    __dirname,
    '../../../config/jira/attachments/PHN-3924',
    PHN_3924_TEMPLATE_ORIGINAL_NAME,
  );

/** ASCII multipart upload name (avoids encoding issues). */
export const PHN_3924_UPLOAD_FILENAME = 'faktura-07-08-2025.docx';

export type Phn3924Fx = Pick<
  baseFixture,
  'Request' | 'FileUploadRequest' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type Phn3924InvoiceTemplate = {
  fileId: number;
  templateId: number;
  templateName: string;
  sourceAssetPath: string;
  originalFilename: string;
};

export type Phn3924ScenarioResult = {
  template: Phn3924InvoiceTemplate;
  customerId: number;
  podId: number;
  productId: number;
  contractId: number;
  bbpId: number;
  billingRunId: number;
  invoiceId: number;
  invoiceDocumentId: number;
  pdfPath: string;
  pdfText: string;
  pdfExtractor: string;
  documentTypeAnalysis: Phn3924DocumentTypeAnalysis;
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
 * Upload the PHN-3924 DOCX and create a non-default ACTIVE INVOICE DOCUMENT
 * template (language=BULGARIAN). Does NOT set setDefault / default* flags.
 *
 * Swagger TemplateCreateRequest required:
 *   fileId, language, name, templatePurpose, templateStatus, templateType
 */
export async function createPhn3924InvoiceDocumentTemplate(
  fx: Phn3924Fx,
  docxAbsolutePath: string = PHN_3924_TEMPLATE_DOCX_PATH,
): Promise<Phn3924InvoiceTemplate> {
  const { Request, FileUploadRequest, Endpoints, Responses } = fx;

  expect(
    fs.existsSync(docxAbsolutePath),
    `PHN-3924 template DOCX must exist at ${docxAbsolutePath} ` +
      `(Jira attachment "${PHN_3924_TEMPLATE_ORIGINAL_NAME}")`,
  ).toBe(true);

  const fileBuffer = fs.readFileSync(docxAbsolutePath);
  expect(fileBuffer.byteLength, 'template DOCX must be non-empty').toBeGreaterThan(0);

  let fileId = 0;
  await test.step('Upload PHN-3924 invoice DOCX (POST template/upload-template-file)', async () => {
    const uploadRes = await FileUploadRequest.post(
      `${Endpoints.template}/upload-template-file?fileFormats=DOCX`,
      {
        multipart: {
          file: {
            name: PHN_3924_UPLOAD_FILENAME,
            mimeType:
              'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            buffer: fileBuffer,
          },
        },
      },
    );
    expect(
      uploadRes.status(),
      `POST ${Endpoints.template}/upload-template-file must return 2xx (body: ${
        uploadRes.ok() ? '<ok>' : await uploadRes.text()
      })`,
    ).toBeGreaterThanOrEqual(200);
    expect(uploadRes.status(), 'template upload status must be < 300').toBeLessThan(300);

    const uploadBody = (await uploadRes.json()) as { id?: unknown };
    fileId = Number(uploadBody?.id);
    expect(
      fileId,
      `TemplateFileResponse.id must be a positive integer (raw=${JSON.stringify(uploadBody)})`,
    ).toBeGreaterThan(0);
  });

  const uniqueSuffix = `${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
  const templateName = `(PHN-3924) Faktura DocumentType ${uniqueSuffix}`;

  // Derived from invoiceDocumentTemplate() + TemplateControllerIT INVOICE DOCUMENT
  // create — validated against Swagger TemplateCreateRequest (dev2).
  // Intentionally omit setDefault / default* flags so we do not collide with
  // existing default invoice templates.
  const templateBody = {
    name: templateName,
    templateType: 'DOCUMENT' as const,
    templateStatus: 'ACTIVE' as const,
    templatePurpose: 'INVOICE' as const,
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
    ] as const,
    fileNamePrefix: null,
    fileNameSuffix: null,
    fileSignings: ['SIGNING_WITH_SYSTEM_CERTIFICATE'] as const,
    fileId,
  };

  let templateId = 0;
  await test.step('Create ACTIVE INVOICE DOCUMENT template (POST /template)', async () => {
    const createRes = await Request.post(Endpoints.template, { data: templateBody });
    await expect(createRes).CheckResponse();
    const createdRaw = await createRes.json();
    templateId =
      typeof createdRaw === 'number'
        ? createdRaw
        : Number((createdRaw as { id?: unknown } | null)?.id ?? NaN);
    expect(
      templateId,
      `POST ${Endpoints.template} must return a positive integer template id (raw=${JSON.stringify(
        createdRaw,
      )})`,
    ).toBeGreaterThan(0);

    const responsesWithTemplate = Responses as unknown as { template?: unknown[] };
    if (!Array.isArray(responsesWithTemplate.template)) {
      responsesWithTemplate.template = [];
    }
    (responsesWithTemplate.template as unknown[]).push({
      id: templateId,
      name: templateName,
      fileId,
    });
  });

  return {
    fileId,
    templateId,
    templateName,
    sourceAssetPath: docxAbsolutePath,
    originalFilename: PHN_3924_TEMPLATE_ORIGINAL_NAME,
  };
}

/**
 * DocumentType language check for rendered invoice PDF text.
 *
 * Matches the header expansion of [[DocumentType]] immediately before the
 * document number line (bug PDF: "INVOICE\\n№ …"; expected: "Фактура\\n№ …").
 * Ignores static appendix wording like "Към Фактура №".
 */
export type Phn3924DocumentTypeAnalysis = {
  passed: boolean;
  hasBulgarianDocumentTypeHeader: boolean;
  hasEnglishDocumentTypeHeader: boolean;
  hasUnlicensedTemplaterWatermark: boolean;
  extractor: string;
  textLength: number;
  reason: string;
};

export function analyzeDocumentTypeLanguage(
  pdfText: string,
  extractor: string,
): Phn3924DocumentTypeAnalysis {
  const hasEnglishDocumentTypeHeader = /(?:^|\n)\s*INVOICE\s*\n\s*№/m.test(pdfText);
  const hasBulgarianDocumentTypeHeader = /(?:^|\n)\s*Фактура\s*\n\s*№/m.test(pdfText);
  const hasUnlicensedTemplaterWatermark = /Unlicensed version/i.test(pdfText);

  const passed = hasBulgarianDocumentTypeHeader && !hasEnglishDocumentTypeHeader;

  let reason: string;
  if (passed) {
    reason =
      'DocumentType header is Bulgarian ("Фактура" before №) and English "INVOICE" header is absent.';
  } else if (hasEnglishDocumentTypeHeader && !hasBulgarianDocumentTypeHeader) {
    reason =
      'DocumentType header expanded to English "INVOICE" before № (expected Bulgarian "Фактура").';
  } else if (!hasBulgarianDocumentTypeHeader && !hasEnglishDocumentTypeHeader) {
    reason =
      'Neither Bulgarian "Фактура\\n№" nor English "INVOICE\\n№" DocumentType header pattern found in PDF text.';
  } else {
    reason =
      'Both Bulgarian and English DocumentType header patterns detected — unexpected dual expansion.';
  }

  if (hasUnlicensedTemplaterWatermark) {
    reason += ' Also observed Templater "Unlicensed version" watermark.';
  }

  return {
    passed,
    hasBulgarianDocumentTypeHeader,
    hasEnglishDocumentTypeHeader,
    hasUnlicensedTemplaterWatermark,
    extractor,
    textLength: pdfText.length,
    reason,
  };
}

/**
 * Full PHN-3924 scenario:
 * template → volume billable chain (custom INVOICE_TEMPLATE) → FOR_VOLUMES
 * billing run COMPLETED → download invoice PDF → DocumentType language analysis.
 */
export async function runPhn3924DocumentTypeScenario(
  fx: Phn3924Fx,
): Promise<Phn3924ScenarioResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  const template = await test.step(
    'Precondition: upload + create PHN-3924 INVOICE DOCUMENT template (non-default)',
    async () => createPhn3924InvoiceDocumentTemplate(fx),
  );

  expect(
    template.templateId,
    'custom invoice template id must be > 0',
  ).toBeGreaterThan(0);
  expect(
    template.templateId,
    'custom template must not equal envVariables.invoice_document_template',
  ).not.toBe(Number(envVariables.invoice_document_template));

  const prechain = await test.step(
    `Precondition: billable chain with product.INVOICE_TEMPLATE=${template.templateId}`,
    async () => runDevVolumeInvoiceCustomTemplatePrechain(fx, template.templateId),
  );

  let billingRunId = 0;
  await test.step('Create FOR_VOLUMES billing run for custom-template product', async () => {
    const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
    const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
    await expect(billingRun).CheckResponse();
    const billingRaw = await billingRun.json();
    billingRunId = asEntityId(billingRaw);
    Responses.billingRun.push(
      typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
    );
  });

  await test.step(
    'Complete billing run (start-billing → DRAFT → GENERATED → COMPLETED)',
    async () => {
      await startAndCompleteBillingRun(Request, billingRunId);
    },
  );

  const invoiceRows = await test.step('Resolve invoice from billing run listing', async () => {
    const rows = await pollInvoiceListing(Request, billingRunId, 1);
    expect(
      rows.length,
      `billing run ${billingRunId} must produce at least one invoice`,
    ).toBeGreaterThan(0);
    return rows;
  });

  const invoiceId = Number(invoiceRows[0]?.id ?? invoiceRows[0]?.invoiceId ?? NaN);
  expect(invoiceId, 'invoice id from listing').toBeGreaterThan(0);
  if (!Responses.invoice.includes(invoiceId)) {
    Responses.invoice.push(invoiceId);
  }

  const { files } = await test.step(
    'Poll invoice file[] for invoiceDocumentId',
    async () => pollInvoiceFileRefs(Request, invoiceId),
  );
  expect(
    files.length,
    `invoice ${invoiceId} must expose at least one file (invoiceDocumentId)`,
  ).toBeGreaterThan(0);

  const invoiceDocumentId = files[0].id;
  const pdfPath = await test.step(
    `Download invoice PDF (invoiceDocumentId=${invoiceDocumentId})`,
    async () => {
      const buf = await downloadInvoiceDocumentBuffer(Request, invoiceDocumentId);
      return savePdfToTestResults(
        buf,
        `phn-3924-invoice-${invoiceId}-doc-${invoiceDocumentId}.pdf`,
      );
    },
  );

  const { text: pdfText, extractor: pdfExtractor } = await test.step(
    'Extract invoice PDF text (system tools only)',
    async () => extractPdfText(pdfPath),
  );

  const documentTypeAnalysis = analyzeDocumentTypeLanguage(pdfText, pdfExtractor);

  // Sanity: invoice GET still reachable (helps portal / summary).
  await getInvoiceBody(Request, invoiceId);

  return {
    template,
    customerId: prechain.customerId,
    podId: prechain.podId,
    productId: prechain.productId,
    contractId: prechain.contractId,
    bbpId: prechain.bbpId,
    billingRunId,
    invoiceId,
    invoiceDocumentId,
    pdfPath,
    pdfText,
    pdfExtractor,
    documentTypeAnalysis,
  };
}
