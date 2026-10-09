/**
 * PHN-3951 — CLONE - Templater: Cancelled Interim invoice - not displaying Price component
 *
 * Reproduces Prod bug (bug-only automation, no TC .md on disk):
 * A reversal (invoiceType=REVERSAL, invoiceDocumentType=CREDIT_NOTE) of a
 * manual INTERIM_AND_ADVANCE_PAYMENT invoice is generated with the exact
 * customer-facing INVOICE document template attached to the bug ticket
 * ("Фактура_АП_МП_КИ_ДИ - за енергия.docx"). The templater fails to
 * substitute the interim price-component tags on the reversal PDF —
 * SD.AdvancePayments, TotalExclVat, TotalVat, TotalInclVat, BasisForIssuing
 * remain raw `[[…]]` placeholders instead of numeric amounts + labels.
 *
 * Prod attachment (bug reporter): CN linked to interim (Prod ids 40773 → 41525),
 * amount excl VAT = 14456.08, incl VAT = 17347.30. INTERIM_AND_ADVANCE_PAYMENT
 * / INVOICE original → REVERSAL / CREDIT_NOTE derivative. NOT an
 * `Invoice_Cancelation` (that is a different template JSON and different flow).
 *
 * Template flow (dev2):
 *   1. `POST /template/upload-template-file?fileFormats=DOCX` (multipart, field
 *      `file`) with the bug-attached DOCX → returns `TemplateFileResponse{id}`.
 *   2. `POST /template` with `TemplateCreateRequest` derived from
 *      `invoiceDocumentTemplate()` (templateType=DOCUMENT, templatePurpose=INVOICE,
 *      templateStatus=ACTIVE, language=BULGARIAN, outputFileFormat=['PDF'],
 *      fileSignings=['SIGNING_WITH_SYSTEM_CERTIFICATE'], fileId from step 1).
 *      Name is unique per run so a fresh template is always created.
 *   3. The returned integer template id is passed as `commonParameters.templateId`
 *      into BOTH the manual interim billing run AND the INVOICE_REVERSAL
 *      billing run — the reversal payload does NOT fall back to
 *      `envVariables.invoice_document_template` any more.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2177-supply-action-deactivation-restricted-period.fixtures.ts
 *   (FileUploadRequest multipart upload pattern)
 * - tests/cursor/PDT-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts
 *   (correction/reversal billing-run POST + Responses.billingRun handling)
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 *   (preconditionProductContractForManual, buildManualInterimPayload,
 *   pushBillingRunPortalRef)
 * - tests/cursor/pdt-2891-connected-invoices.fixtures.ts (resolveInterimInvoiceIds)
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts (startAndCompleteBillingRun)
 * - jsons/payloads/create/nomenclatures/templates.ts (uploadTemplateFile + checkTemplate)
 * - jsons/payloads/create/operationsManagement/invoiceDocumentTemplate.ts
 * - jsons/payloads/create/billing/manualoInterim.ts, reversal.ts
 *
 * Swagger (dev2, cached — refresh script is Windows-only, cached spec used):
 *   POST /template/upload-template-file?fileFormats=DOCX
 *     multipart `file` (binary, required) → 200 TemplateFileResponse{id,name}
 *   POST /template
 *     body TemplateCreateRequest (required: fileId, language, name,
 *     templatePurpose, templateStatus, templateType) → 200 integer (templateId)
 *   GET  /invoice/download-document?invoiceDocumentId=...
 *     query int64 required → 200 binary. Ids come from InvoiceResponse.file[].
 *
 * Target env: Dev2 — BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 * This spec is Dev2-only and REQUIRES the bug-attached DOCX template asset
 * shipped under tests/cursor/assets/phn-3951/.
 */

import * as fs from 'fs';
import * as path from 'path';
import { spawnSync } from 'child_process';
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import {
  preconditionProductContractForManual,
  buildManualInterimPayload,
  pushBillingRunPortalRef,
  type Pdt2872Fx,
} from './pdt-2872-minimal-interim-payment.fixtures';
import { resolveInterimInvoiceIds } from './pdt-2891-connected-invoices.fixtures';
import { startAndCompleteBillingRun } from './rps-pod-invoice-due-date.fixtures';

export const PHN_3951_KEY = 'PHN-3951';
export const PHN_3951_TITLE =
  'CLONE - Templater: Cancelled Interim invoice - not displaying Price component';

/** Interim excl. VAT amount taken from the Prod bug attachment (CN 41525). */
export const PHN_3951_AMOUNT_EXCL_VAT = 14456.08;

export type Phn3951Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'FileUploadRequest'
>;

// ─── Template asset shipped with this spec ────────────────────────────────

/**
 * ASCII-safe destination filename for the customer-facing invoice template
 * attached to the PHN-3951 Jira bug. The original Cyrillic filename is kept
 * in {@link PHN_3951_TEMPLATE_ASSET_ORIGINAL_NAME} for traceability only —
 * it is NOT used on disk to avoid multipart / filesystem encoding issues.
 *
 * Source (workspace, read-only):
 *   Cursor-Project/config/jira/attachments/PHN-3951/
 *     "Фактура_АП_МП_КИ_ДИ - за енергия.docx"
 *
 * Destination (shipped with this spec):
 *   Cursor-Project/EnergoTS/tests/cursor/assets/phn-3951/
 *     faktura-ap-mp-ki-di-za-energiya.docx
 */
export const PHN_3951_TEMPLATE_ASSET_FILENAME = 'faktura-ap-mp-ki-di-za-energiya.docx';
export const PHN_3951_TEMPLATE_ASSET_ORIGINAL_NAME =
  'Фактура_АП_МП_КИ_ДИ - за енергия.docx';
export const PHN_3951_TEMPLATE_ASSET_PATH = path.resolve(
  __dirname,
  'assets',
  'phn-3951',
  PHN_3951_TEMPLATE_ASSET_FILENAME,
);

/** Bulgarian labels expected on a substituted reversal CREDIT_NOTE PDF. */
export const PHN_3951_REQUIRED_LABELS = {
  interimPayment: 'Междинно плащане',
  totalExclVat: 'Общо сума без ДДС',
  taxBase: 'Данъчна основа',
  vat: 'ДДС',
} as const;

/** Unsubstituted templater placeholders that MUST NOT appear on a rendered PDF. */
export const PHN_3951_FORBIDDEN_TAGS = [
  '[[SD.AdvancePayments',
  '[[TotalExclVat',
  '[[TotalVat',
  '[[TotalInclVat',
  '[[BasisForIssuing]]',
] as const;

// ─── Energy invoice template (bug-attached DOCX) ───────────────────────────

const BILLING_RUN_ROOT = 'billing-run';

/**
 * Result of {@link createPhn3951EnergyInvoiceTemplate} — captured so the spec
 * can attach both ids to `TestRunSummary` and register the template id in
 * `Responses` for portal-link discovery.
 */
export type Phn3951EnergyInvoiceTemplate = {
  fileId: number;
  templateId: number;
  templateName: string;
  originalFilename: string;
  sourceAssetPath: string;
};

/**
 * Bulgarian-language, ACTIVE, DOCUMENT/INVOICE template built from the exact
 * Word file attached to the PHN-3951 Jira bug (the customer-facing
 * "Фактура АП/МП/КИ/ДИ - за енергия" invoice template that contains the
 * `SD.AdvancePayments`, `TotalExclVat`, `TotalVat`, `TotalInclVat`,
 * `BasisForIssuing` templater blocks).
 *
 * Fresh template is created on every run (unique name via `Date.now()` +
 * random suffix) so the test is idempotent and never picks up state from a
 * previous run. The returned template id is used as
 * `commonParameters.templateId` for BOTH the manual interim billing run AND
 * the INVOICE_REVERSAL billing run — the previous behaviour of falling back
 * to `envVariables.invoice_document_template` (which produced a PDF that did
 * NOT include the price-component block on Dev2) is removed.
 *
 * Swagger contract (dev2 cached spec):
 *   POST /template/upload-template-file?fileFormats=DOCX
 *     multipart `file` (binary, required)
 *     → 200 TemplateFileResponse { id: int64, name: string }
 *   POST /template
 *     body TemplateCreateRequest (required: fileId, language, name,
 *     templatePurpose, templateStatus, templateType)
 *     → 200 integer (int64, template id)
 *
 * Endpoint uses `Endpoints.template` (= `template`) and the
 * `FileUploadRequest` fixture (bearer-token + no forced JSON content-type)
 * for the multipart upload — same fixture pattern as
 * `pdt-2177-supply-action-deactivation-restricted-period.fixtures.ts`
 * `uploadSupplyActionDeactivationFile`.
 */
export async function createPhn3951EnergyInvoiceTemplate(
  fx: Phn3951Fx,
): Promise<Phn3951EnergyInvoiceTemplate> {
  const { Request, FileUploadRequest, Endpoints, Responses } = fx;

  expect(
    fs.existsSync(PHN_3951_TEMPLATE_ASSET_PATH),
    `PHN-3951 template asset must exist at ${PHN_3951_TEMPLATE_ASSET_PATH} ` +
      `(shipped from Jira attachment "${PHN_3951_TEMPLATE_ASSET_ORIGINAL_NAME}")`,
  ).toBe(true);

  const fileBuffer = fs.readFileSync(PHN_3951_TEMPLATE_ASSET_PATH);
  expect(fileBuffer.byteLength, 'template DOCX must be non-empty').toBeGreaterThan(0);

  const uploadRes = await FileUploadRequest.post(
    `${Endpoints.template}/upload-template-file?fileFormats=DOCX`,
    {
      multipart: {
        file: {
          name: PHN_3951_TEMPLATE_ASSET_FILENAME,
          mimeType:
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
          buffer: fileBuffer,
        },
      },
    },
  );
  expect(
    uploadRes.status(),
    `POST ${Endpoints.template}/upload-template-file must return 2xx (got body: ${
      uploadRes.ok() ? '<ok>' : await uploadRes.text()
    })`,
  ).toBeGreaterThanOrEqual(200);
  expect(uploadRes.status(), 'template upload status must be < 300').toBeLessThan(300);

  const uploadBody = (await uploadRes.json()) as { id?: unknown; name?: unknown };
  const fileId = Number(uploadBody?.id);
  expect(
    fileId,
    `TemplateFileResponse.id must be a positive integer (raw=${JSON.stringify(uploadBody)})`,
  ).toBeGreaterThan(0);

  const uniqueSuffix = `${Date.now()}-${Math.floor(Math.random() * 1e6)}`;
  const templateName = `(PHN-3951) energy invoice АП_МП_КИ_ДИ ${uniqueSuffix}`;

  // Body derived from jsons/payloads/create/operationsManagement/
  // invoiceDocumentTemplate.ts + validated against Swagger dev2
  // TemplateCreateRequest — required: fileId, language, name,
  // templatePurpose, templateStatus, templateType.
  const templateBody = {
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
    ] as const,
    fileNamePrefix: null,
    fileNameSuffix: null,
    fileSignings: ['SIGNING_WITH_SYSTEM_CERTIFICATE'] as const,
    fileId,
  };

  const createRes = await Request.post(Endpoints.template, { data: templateBody });
  await expect(createRes).CheckResponse();
  const createdRaw = await createRes.json();
  const templateId =
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
  (responsesWithTemplate.template as number[]).push(templateId);

  return {
    fileId,
    templateId,
    templateName,
    originalFilename: PHN_3951_TEMPLATE_ASSET_ORIGINAL_NAME,
    sourceAssetPath: PHN_3951_TEMPLATE_ASSET_PATH,
  };
}

// ─── Manual interim parent ─────────────────────────────────────────────────

export type ManualInterimParentOutcome = {
  interimBillingRunId: number;
  interimInvoiceId: number;
};

/**
 * Create a manual INTERIM_AND_ADVANCE_PAYMENT billing run whose
 * `commonParameters.templateId` is forced to the PHN-3951 energy invoice
 * template built by {@link createPhn3951EnergyInvoiceTemplate}. This mirrors
 * `pdt-2872-minimal-interim-payment.fixtures.ts`
 * `createManualInterimBillingRun` (which hard-codes
 * `envVariables.invoice_document_template`) but overrides the templateId
 * BEFORE the POST/PUT so both interim and reversal PDFs render against the
 * bug-attached template.
 */
async function createManualInterimBillingRunWithTemplate(
  fx: Phn3951Fx,
  amountExcludingVat: number,
  templateId: number,
): Promise<number> {
  const { Request, Responses, Endpoints } = fx;

  const payload = await buildManualInterimPayload(fx as Pdt2872Fx, amountExcludingVat);
  payload.commonParameters.templateId = templateId;

  const createRes = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(createRes).CheckResponse();
  const raw = await createRes.json();
  const billingRunId =
    typeof raw === 'number' ? raw : Number((raw as { id?: unknown } | null)?.id ?? NaN);
  expect(billingRunId, 'manual interim billing run id must be > 0').toBeGreaterThan(0);
  pushBillingRunPortalRef(Responses, billingRunId, 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT');

  const putRes = await Request.put(`${BILLING_RUN_ROOT}/${billingRunId}`, { data: payload });
  await expect(putRes).CheckResponse();

  return billingRunId;
}

/**
 * Full precondition chain for the manual INTERIM_AND_ADVANCE_PAYMENT parent
 * invoice: contract → manual interim billing run (forced to
 * `templateId = energyInvoiceTemplateId`) → run through COMPLETED (PDF
 * generated + accounting done) → resolve REAL interim invoice id.
 *
 * Delegates to pdt-2872 helpers (`preconditionProductContractForManual`,
 * `buildManualInterimPayload`, `pushBillingRunPortalRef`) and
 * rps-pod-invoice-due-date's `startAndCompleteBillingRun`.
 */
export async function createManualInterimParentInvoice(
  fx: Phn3951Fx,
  energyInvoiceTemplateId: number,
): Promise<ManualInterimParentOutcome> {
  expect(
    energyInvoiceTemplateId,
    'PHN-3951 requires a freshly-created energy invoice templateId (do NOT fall back to envVariables.invoice_document_template)',
  ).toBeGreaterThan(0);

  await test.step('Precondition: product contract chain for manual interim', async () => {
    await preconditionProductContractForManual(fx as Pdt2872Fx);
  });

  let interimBillingRunId = 0;
  await test.step(
    `Precondition: create manual INTERIM_AND_ADVANCE_PAYMENT billing run ` +
      `(excl VAT ${PHN_3951_AMOUNT_EXCL_VAT}, templateId=${energyInvoiceTemplateId})`,
    async () => {
      interimBillingRunId = await createManualInterimBillingRunWithTemplate(
        fx,
        PHN_3951_AMOUNT_EXCL_VAT,
        energyInvoiceTemplateId,
      );
    },
  );

  await test.step(
    'Precondition: start-billing → DRAFT → GENERATED → COMPLETED for interim (PDF + accounting)',
    async () => {
      await startAndCompleteBillingRun(fx.Request, interimBillingRunId);
    },
  );

  const interimIds = await test.step('Precondition: resolve REAL interim invoice id', async () =>
    resolveInterimInvoiceIds(fx.Request, interimBillingRunId, 1),
  );
  const interimInvoiceId = interimIds[0];
  expect(interimInvoiceId, 'manual interim invoice id must be > 0').toBeGreaterThan(0);
  if (!fx.Responses.invoice.includes(interimInvoiceId)) {
    fx.Responses.invoice.push(interimInvoiceId);
  }

  return { interimBillingRunId, interimInvoiceId };
}

// ─── Reversal (INVOICE_REVERSAL → CREDIT_NOTE) ─────────────────────────────

export type ReversalRunOutcome = {
  reversalBillingRunId: number;
  reversalBillingRunIndex: number;
};

/**
 * POST an INVOICE_REVERSAL billing run for the interim invoice at
 * `Responses.invoice[interimInvoiceIndex]` using the freshly-created PHN-3951
 * energy invoice templateId. The reversal payload (`reversal.ts`) defaults
 * `commonParameters.templateId = null`; we FORCE it to
 * `energyInvoiceTemplateId` (the bug-attached DOCX template containing the
 * `SD.AdvancePayments` block). The previous fallback to
 * `envVariables.invoice_document_template` — which pointed at a generic
 * pre-provisioned template that did NOT contain the price-component block on
 * Dev2 — is intentionally removed so the templater always operates against
 * the exact template in the bug report.
 */
export async function createInterimReversalBillingRun(
  fx: Phn3951Fx,
  interimInvoiceIndex: number,
  energyInvoiceTemplateId: number,
): Promise<ReversalRunOutcome> {
  const { Request, GeneratePayload, Responses, Endpoints } = fx;

  expect(
    energyInvoiceTemplateId,
    'PHN-3951 reversal requires the freshly-created energy invoice templateId ' +
      '(no fallback to envVariables.invoice_document_template)',
  ).toBeGreaterThan(0);

  const reversalPayload = await GeneratePayload.billing.reversalBilling(interimInvoiceIndex);
  reversalPayload.commonParameters.templateId = energyInvoiceTemplateId;

  const reversalRes = await Request.post(Endpoints.billingRun, { data: reversalPayload });
  await expect(reversalRes).CheckResponse();
  const body = await reversalRes.json();
  Responses.billingRun.push(body);
  const reversalBillingRunIndex = Responses.billingRun.length - 1;
  const reversalBillingRunId =
    typeof body === 'number' ? body : Number((body as { id?: unknown }).id ?? NaN);
  expect(reversalBillingRunId, 'INVOICE_REVERSAL billing run id').toBeGreaterThan(0);

  return { reversalBillingRunId, reversalBillingRunIndex };
}

// ─── Invoice + document helpers ────────────────────────────────────────────

export type InvoiceFileRef = { id: number; name?: string };

export async function getInvoiceBody(
  Request: Phn3951Fx['Request'],
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`invoice?id=${invoiceId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

/**
 * Swagger InvoiceResponse.file: array of `{id, name}` — those `id` values are
 * `invoiceDocumentId` for GET /invoice/download-document.
 */
export function readInvoiceFileRefs(invoice: Record<string, unknown>): InvoiceFileRef[] {
  const raw = invoice.file as unknown;
  if (!Array.isArray(raw)) return [];
  return raw
    .map((r) => {
      if (!r || typeof r !== 'object') return null;
      const id = Number((r as { id?: unknown }).id);
      if (!Number.isFinite(id) || id <= 0) return null;
      return { id, name: (r as { name?: string }).name };
    })
    .filter((r): r is InvoiceFileRef => r !== null);
}

/** Some billing runs need a few seconds to materialize the file[] entry after COMPLETED. */
export async function pollInvoiceFileRefs(
  Request: Phn3951Fx['Request'],
  invoiceId: number,
  timeoutMs = 3 * 60 * 1000,
  intervalMs = 5000,
): Promise<{ invoice: Record<string, unknown>; files: InvoiceFileRef[] }> {
  const maxAttempts = Math.max(1, Math.ceil(timeoutMs / intervalMs));
  let invoice: Record<string, unknown> = {};
  let files: InvoiceFileRef[] = [];
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    invoice = await getInvoiceBody(Request, invoiceId);
    files = readInvoiceFileRefs(invoice);
    if (files.length > 0) return { invoice, files };
    if (attempt < maxAttempts) await new Promise((r) => setTimeout(r, intervalMs));
  }
  return { invoice, files };
}

export async function downloadInvoiceDocumentBuffer(
  Request: Phn3951Fx['Request'],
  invoiceDocumentId: number,
): Promise<Buffer> {
  const res = await Request.get(
    `invoice/download-document?invoiceDocumentId=${invoiceDocumentId}`,
  );
  await expect(
    res,
    `GET invoice/download-document?invoiceDocumentId=${invoiceDocumentId}`,
  ).CheckResponse();
  const body = await res.body();
  expect(
    body.byteLength,
    `download-document ${invoiceDocumentId} must return non-empty binary`,
  ).toBeGreaterThan(0);
  return Buffer.from(body);
}

const PDF_OUT_DIR = path.join(process.cwd(), 'test-results');

export function savePdfToTestResults(buffer: Buffer, filename: string): string {
  if (!fs.existsSync(PDF_OUT_DIR)) {
    fs.mkdirSync(PDF_OUT_DIR, { recursive: true });
  }
  const target = path.join(PDF_OUT_DIR, filename);
  fs.writeFileSync(target, buffer);
  return target;
}

/**
 * Extract text from a PDF using system tools only — no new npm dependencies.
 * Tries, in order:
 *   1. `pdftotext -layout -enc UTF-8` (poppler) if on PATH
 *   2. `python3` + `pypdf`
 *   3. `python3` + `PyPDF2`
 * Returns the extracted text and which extractor produced it.
 * Throws if none is available so the test surfaces the environment gap.
 */
export function extractPdfText(pdfPath: string): { text: string; extractor: string } {
  const which = spawnSync('bash', ['-lc', 'command -v pdftotext'], { encoding: 'utf8' });
  if (which.status === 0 && (which.stdout ?? '').trim()) {
    const outPath = `${pdfPath}.txt`;
    const res = spawnSync(
      'pdftotext',
      ['-layout', '-enc', 'UTF-8', pdfPath, outPath],
      { encoding: 'utf8' },
    );
    if (res.status === 0 && fs.existsSync(outPath)) {
      return { text: fs.readFileSync(outPath, 'utf8'), extractor: 'pdftotext' };
    }
  }

  const pypdfScript =
    'import sys\n' +
    'from pypdf import PdfReader\n' +
    'reader = PdfReader(sys.argv[1])\n' +
    'sys.stdout.write("\\n".join((p.extract_text() or "") for p in reader.pages))\n';
  const pyPdf = spawnSync('python3', ['-c', pypdfScript, pdfPath], { encoding: 'utf8' });
  if (pyPdf.status === 0) {
    return { text: pyPdf.stdout ?? '', extractor: 'python3-pypdf' };
  }

  const pypdf2Script =
    'import sys\n' +
    'from PyPDF2 import PdfReader\n' +
    'reader = PdfReader(sys.argv[1])\n' +
    'sys.stdout.write("\\n".join((p.extract_text() or "") for p in reader.pages))\n';
  const pyPdf2 = spawnSync('python3', ['-c', pypdf2Script, pdfPath], { encoding: 'utf8' });
  if (pyPdf2.status === 0) {
    return { text: pyPdf2.stdout ?? '', extractor: 'python3-PyPDF2' };
  }

  const errs =
    `pdftotext=${which.status ?? 'na'}:${(which.stderr ?? '').trim().slice(0, 200)} | ` +
    `pypdf=${pyPdf.status}:${(pyPdf.stderr ?? '').trim().slice(0, 200)} | ` +
    `PyPDF2=${pyPdf2.status}:${(pyPdf2.stderr ?? '').trim().slice(0, 200)}`;
  throw new Error(
    `PHN-3951: no PDF text extractor available on this environment for ${pdfPath} — ${errs}`,
  );
}

// ─── PDF assertion helpers ─────────────────────────────────────────────────

export type PhnPdfBugObserved = {
  forbiddenTagsFound: string[];
  hasInterimPaymentLabel: boolean;
  hasTotalExclVatLabel: boolean;
  hasTaxBaseLabel: boolean;
  hasVatLabel: boolean;
  hasExpectedAmount: boolean;
  extractor: string;
  textLength: number;
};

export type PhnPdfBugOutcome = {
  passed: boolean;
  observed: PhnPdfBugObserved;
  reason: string;
};

function normalizeText(raw: string): string {
  return raw.replace(/\s+/g, ' ').trim();
}

/**
 * Bulgarian PDFs typically render amounts as `14 456,08` (space thousand
 * separator, comma decimal). Accept common variants so the test doesn't
 * false-negative on formatting alone.
 */
export function containsAmount(text: string, amount: number): boolean {
  const normalized = normalizeText(text);
  const fixed = amount.toFixed(2);
  const [intPart, decPart] = fixed.split('.');
  const withSpaceSep = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  const withCommaSep = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const candidates = new Set<string>([
    fixed,
    fixed.replace('.', ','),
    `${withSpaceSep}.${decPart}`,
    `${withSpaceSep},${decPart}`,
    `${withCommaSep}.${decPart}`,
    intPart,
    withSpaceSep,
  ]);
  for (const c of candidates) {
    if (normalized.includes(c)) return true;
  }
  return false;
}

/**
 * Analyze a rendered PDF text for the PHN-3951 templater bug — missing
 * substituted price-component fields on a reversal CREDIT_NOTE PDF.
 *
 * `passed=true` means the fix is in place (labels + amount rendered, no raw
 * `[[…]]` tags). `passed=false` means the bug reproduces and the test should
 * fail with a descriptive message that documents what was actually seen.
 */
export function analyzeReversalPdfText(
  text: string,
  extractor: string,
  amountExclVat: number,
): PhnPdfBugOutcome {
  const normalized = normalizeText(text);
  const forbiddenTagsFound = PHN_3951_FORBIDDEN_TAGS.filter((tag) => normalized.includes(tag));
  const hasInterimPaymentLabel = normalized.includes(PHN_3951_REQUIRED_LABELS.interimPayment);
  const hasTotalExclVatLabel = normalized.includes(PHN_3951_REQUIRED_LABELS.totalExclVat);
  const hasTaxBaseLabel = normalized.includes(PHN_3951_REQUIRED_LABELS.taxBase);
  const hasVatLabel = normalized.includes(PHN_3951_REQUIRED_LABELS.vat);
  const hasExpectedAmount = containsAmount(text, amountExclVat);

  const requiredLabelsOk =
    hasInterimPaymentLabel && hasTotalExclVatLabel && hasTaxBaseLabel && hasVatLabel;
  const noForbiddenTags = forbiddenTagsFound.length === 0;
  const passed = requiredLabelsOk && noForbiddenTags && hasExpectedAmount;

  const reasons: string[] = [];
  if (!requiredLabelsOk) {
    reasons.push(
      `missing labels — interimPayment=${hasInterimPaymentLabel}, ` +
        `totalExclVat=${hasTotalExclVatLabel}, taxBase=${hasTaxBaseLabel}, vat=${hasVatLabel}`,
    );
  }
  if (!noForbiddenTags) {
    reasons.push(`unsubstituted templater tags present: ${forbiddenTagsFound.join(', ')}`);
  }
  if (!hasExpectedAmount) {
    reasons.push(`interim amount ${amountExclVat.toFixed(2)} not found in PDF text`);
  }

  const reason =
    reasons.length === 0
      ? 'All required labels + amount present; no unsubstituted templater tags.'
      : `PHN-3951 bug reproduced — ${reasons.join('; ')}`;

  return {
    passed,
    observed: {
      forbiddenTagsFound,
      hasInterimPaymentLabel,
      hasTotalExclVatLabel,
      hasTaxBaseLabel,
      hasVatLabel,
      hasExpectedAmount,
      extractor,
      textLength: text.length,
    },
    reason,
  };
}
