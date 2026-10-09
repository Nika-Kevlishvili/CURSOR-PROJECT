/**
 * PHN-3948 — CLONE - Templater: Invoice document type not showing correctly
 *
 * Bug-only automation (no TC .md on disk; alignment: Jira reproduce steps).
 *
 * Target env: Dev2
 *   BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 *
 * Reproduce (customfield_10103):
 *   1. Create a debit note
 *   2. Generate the file for the invoice
 * Actual: PDF DocumentType showed English/corrupt `DEBIT Без гаранцияТЕ`
 *   (UI Тип документ = Debit Note; template placeholder `[[DocumentType]]`)
 * Expected (template DocumentType, Bulgarian):
 *   Debit note → Дебитно известие
 *   Credit note → Кредитно известие
 *   Invoice → Фактура
 *   Proforma Invoice → Проформа фактура
 *
 * Runtime (Phoenix InvoiceRepository.getInvoiceDocumentModel):
 *   translation.translate_text(text(ci.document_type), text('BULGARIAN')) as DocumentType
 * Dev2 DB: DEBIT_NOTE → Дебитно известие, CREDIT_NOTE → Кредитно известие,
 *   INVOICE → Фактура. PROFORMA_INVOICE → PROFORMA_Фактура (enum key missing
 *   in translation.translations) — this test does NOT generate PROFORMA_INVOICE.
 *
 * swagger_refresh=failed_using_cache_plus_manual_curl
 *   update-swagger-specs.ps1 failed on macOS (`curl.exe` missing). Parent GET
 *   http://10.236.20.11:8092/v3/api-docs (HTTP 200) saved to
 *   Cursor-Project/config/swagger/dev2/swagger-spec.json. Confirmed:
 *   - GET /billing-run/generate-invoice-data?invoiceId= (operationId generateData,
 *     deprecated) → BillingRunDocumentModelImpl.DocumentType (string)
 *   - GET /invoice/download-document?invoiceDocumentId= (int64 required) → binary
 *   - InvoiceResponse.invoiceDocumentType enum:
 *     INVOICE, CREDIT_NOTE, DEBIT_NOTE, PROFORMA_INVOICE
 *   - POST /billing-run BillingRunCreateRequest.billingType includes
 *     MANUAL_CREDIT_OR_DEBIT_NOTE; ManualCreditOrDebitNoteBasicDataParameters
 *     .documentType enum DEBIT_NOTE | CREDIT_NOTE
 *   - BillingRunCommonParameters.templateId integer int64
 *   - GET /billing-run/draft-invoices?id= (operationId listDraftInvoices)
 *     → PageBillingRunInvoiceViewResponse (id int64, invoiceType, invoiceDocumentType)
 *
 * Dev2 invoice/listing may stay empty after COMPLETED (indexing lag). PDT-2872
 * resolves via GET billing-run/draft-invoices first. This file does the same
 * with Number(id ?? invoiceId) coerce — it does not call pollInterimInvoicesPositive
 * (uniqueInterimInvoiceIds still drops string ids).
 *
 * Reference spec(s):
 * - tests/cursor/PHN-3924-templater-document-type-bulgarian.spec.ts
 *   + phn-3924-templater-document-type-bulgarian.fixtures.ts
 *   (PDF extract, header-immediately-before-№ — not bare includes("Фактура"))
 * - tests/cursor/pdt-2891-connected-invoices.fixtures.ts
 *   (buildManualCreditOrDebitNotePayload)
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 *   (preconditionProductContractForManual, listInterimDraftRows)
 * - tests/cursor/phn-3943-invoiced-month-empty-for-corrections.fixtures.ts
 *   (fetchInvoiceDocumentModel)
 * - tests/cursor/phn-3951-cancelled-interim-invoice-price-component.fixtures.ts
 *   (pollInvoiceFileRefs, downloadInvoiceDocumentBuffer, extractPdfText,
 *   savePdfToTestResults)
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 *   (startAndCompleteBillingRun, pollInvoiceListing)
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import {
  createPhn3924InvoiceDocumentTemplate,
  PHN_3924_TEMPLATE_ORIGINAL_NAME,
  type Phn3924Fx,
  type Phn3924InvoiceTemplate,
} from './phn-3924-templater-document-type-bulgarian.fixtures';
import {
  buildManualCreditOrDebitNotePayload,
  type Pdt2891Fx,
} from './pdt-2891-connected-invoices.fixtures';
import {
  buildManualInterimPayload,
  listInterimDraftRows,
  preconditionProductContractForManual,
  pushBillingRunPortalRef,
  resolvePdt2872Amounts,
  type Pdt2872Fx,
} from './pdt-2872-minimal-interim-payment.fixtures';
import {
  downloadInvoiceDocumentBuffer,
  extractPdfText,
  getInvoiceBody,
  pollInvoiceFileRefs,
  savePdfToTestResults,
} from './phn-3951-cancelled-interim-invoice-price-component.fixtures';
import {
  asBillingRunId,
  pollInvoiceListing,
  startAndCompleteBillingRun,
} from './rps-pod-invoice-due-date.fixtures';

export const PHN_3948_KEY = 'PHN-3948';
export const PHN_3948_TITLE =
  'CLONE - Templater: Invoice document type not showing correctly';

/** Swagger GET /billing-run/generate-invoice-data (operationId generateData, deprecated). */
export const GENERATE_INVOICE_DATA_PATH = 'billing-run/generate-invoice-data';

const BILLING_RUN_ROOT = 'billing-run';
/** Match rps-pod-invoice-due-date pollInvoiceListing (~2 minutes). */
const INVOICE_ID_POLL_MS = 15_000;
const INVOICE_ID_POLL_MAX_MS = 2 * 60 * 1000;

/** Ticket mapping: invoiceDocumentType enum → expected BillingRunDocumentModelImpl.DocumentType. */
export const EXPECTED_DOCUMENT_TYPE_BG: Record<
  'INVOICE' | 'DEBIT_NOTE' | 'CREDIT_NOTE',
  string
> = {
  INVOICE: 'Фактура',
  DEBIT_NOTE: 'Дебитно известие',
  CREDIT_NOTE: 'Кредитно известие',
};

const FORBIDDEN_DEBIT_JSON_EXACT = ['DEBIT NOTE', 'Debit Note', 'DEBIT_NOTE'] as const;

export type Phn3948Fx = Pick<
  baseFixture,
  'Request' | 'FileUploadRequest' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type Phn3948PdfAnalysis = {
  passed: boolean;
  hasBulgarianDebitHeader: boolean;
  hasEnglishDebitHeader: boolean;
  hasCorruptDebitWithoutGuarantee: boolean;
  hasEnglishInvoiceHeader: boolean;
  extractor: string;
  textLength: number;
  reason: string;
};

export type Phn3948ParentInvoice = {
  parentInvoiceId: number;
  interimBillingRunId: number;
  invoiceDocumentType: string;
};

export type Phn3948DebitNote = {
  debitNoteId: number;
  debitNoteBillingRunId: number;
};

export type Phn3948PdfArtifact = {
  invoiceDocumentId: number;
  pdfPath: string;
  pdfText: string;
  pdfExtractor: string;
  analysis: Phn3948PdfAnalysis;
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

/** Draft/listing rows may return `id` / `invoiceId` as number or string (int64). */
function coerceInvoiceRowId(row: Record<string, unknown>): number | undefined {
  const n = Number(row.id ?? row.invoiceId);
  return Number.isFinite(n) && n > 0 ? n : undefined;
}

function summarizeInvoiceRows(rows: Record<string, unknown>[]): string {
  return JSON.stringify(
    rows.map((r) => ({
      id: r.id,
      invoiceId: r.invoiceId,
      invoiceType: r.invoiceType,
      invoiceDocumentType: r.invoiceDocumentType ?? r.documentType,
    })),
  );
}

function pickCoercedInvoiceId(
  rows: Record<string, unknown>[],
  prefer?: (row: Record<string, unknown>) => boolean,
): number | undefined {
  const withIds = rows
    .map((row) => ({ row, id: coerceInvoiceRowId(row) }))
    .filter((x): x is { row: Record<string, unknown>; id: number } => x.id != null);
  const preferred = prefer ? withIds.find((x) => prefer(x.row)) : undefined;
  return (preferred ?? withIds[0])?.id;
}

async function sleepMs(ms: number): Promise<void> {
  await new Promise((r) => setTimeout(r, ms));
}

/**
 * GET billing-run/draft-invoices?id= with no invoiceType filter.
 * listInterimDraftRows keeps only INTERIM_AND_ADVANCE_PAYMENT — debit notes must
 * not use that filter.
 */
export async function listAllDraftInvoiceRows(
  Request: Phn3948Fx['Request'],
  billingRunId: number,
): Promise<Record<string, unknown>[]> {
  const res = await Request.get(
    `${BILLING_RUN_ROOT}/draft-invoices?id=${billingRunId}&page=0&size=50`,
  );
  await expect(res).CheckResponse();
  const body = (await res.json()) as { content?: Record<string, unknown>[] };
  return body.content ?? [];
}

async function pollDraftRowsUntilCoercibleId(
  fetchRows: () => Promise<Record<string, unknown>[]>,
  prefer?: (row: Record<string, unknown>) => boolean,
): Promise<{ id?: number; rows: Record<string, unknown>[] }> {
  const maxAttempts = Math.max(1, Math.ceil(INVOICE_ID_POLL_MAX_MS / INVOICE_ID_POLL_MS));
  let rows: Record<string, unknown>[] = [];
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    rows = await fetchRows();
    const id = pickCoercedInvoiceId(rows, prefer);
    if (id != null) {
      return { id, rows };
    }
    if (attempt < maxAttempts) {
      await sleepMs(INVOICE_ID_POLL_MS);
    }
  }
  return { rows };
}

/**
 * Parent invoice after COMPLETED manual interim: drafts first (Dev2 listing lag),
 * then invoice/listing. Coerces Number(id ?? invoiceId). Prefers INTERIM type.
 */
export async function resolveParentInvoiceIdFromDraftsThenListing(
  Request: Phn3948Fx['Request'],
  billingRunId: number,
): Promise<number> {
  const preferInterim = (row: Record<string, unknown>) =>
    row.invoiceType === 'INTERIM_AND_ADVANCE_PAYMENT';

  const drafts = await pollDraftRowsUntilCoercibleId(
    () => listInterimDraftRows(Request, billingRunId),
    preferInterim,
  );
  if (drafts.id != null) {
    return drafts.id;
  }

  const listingRows = await pollInvoiceListing(Request, billingRunId, 1);
  const listingId = pickCoercedInvoiceId(listingRows, preferInterim);
  if (listingId != null) {
    return listingId;
  }

  throw new Error(
    `[PHN-3948] No parent invoice id after COMPLETED billingRunId=${billingRunId}; ` +
      `drafts=${summarizeInvoiceRows(drafts.rows)}; listing=${summarizeInvoiceRows(listingRows)}`,
  );
}

/**
 * DEBIT_NOTE invoice after COMPLETED manual C/D run: unfiltered drafts first
 * (do not use listInterimDraftRows), then listing. Prefer invoiceDocumentType
 * / documentType === DEBIT_NOTE.
 */
export async function resolveDebitNoteInvoiceIdFromDraftsThenListing(
  Request: Phn3948Fx['Request'],
  billingRunId: number,
): Promise<number> {
  const preferDebit = (row: Record<string, unknown>) =>
    String(row.invoiceDocumentType ?? row.documentType ?? '') === 'DEBIT_NOTE';

  const drafts = await pollDraftRowsUntilCoercibleId(
    () => listAllDraftInvoiceRows(Request, billingRunId),
    preferDebit,
  );
  if (drafts.id != null) {
    return drafts.id;
  }

  const listingRows = await pollInvoiceListing(Request, billingRunId, 1);
  const listingId = pickCoercedInvoiceId(listingRows, preferDebit);
  if (listingId != null) {
    return listingId;
  }

  throw new Error(
    `[PHN-3948] No DEBIT_NOTE invoice id after COMPLETED billingRunId=${billingRunId}; ` +
      `drafts=${summarizeInvoiceRows(drafts.rows)}; listing=${summarizeInvoiceRows(listingRows)}`,
  );
}

export function productContractIdFromResponses(Responses: Phn3948Fx['Responses']): number {
  expect(Responses.productContract.length, 'product contract must exist').toBeGreaterThan(0);
  return asEntityId(Responses.productContract[0]);
}

export function optionalEntityId(raw: unknown): number | undefined {
  try {
    return asEntityId(raw);
  } catch {
    return undefined;
  }
}

/**
 * GET billing-run/generate-invoice-data?invoiceId= → BillingRunDocumentModelImpl
 * (thin copy of PHN-3943 fetchInvoiceDocumentModel — PHN-3943 files not modified).
 */
export async function fetchInvoiceDocumentModel(
  Request: Phn3948Fx['Request'],
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${GENERATE_INVOICE_DATA_PATH}?invoiceId=${invoiceId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export function readDocumentType(documentModel: Record<string, unknown>): string {
  return String(documentModel.DocumentType ?? '').trim();
}

export function isForbiddenDebitJsonDocumentType(value: string): boolean {
  if ((FORBIDDEN_DEBIT_JSON_EXACT as readonly string[]).includes(value)) {
    return true;
  }
  return /DEBIT\s*Без/i.test(value);
}

/**
 * Debit-note PDF header check adapted from PHN-3924 analyzeDocumentTypeLanguage.
 * Requires line `Дебитно известие` immediately before `№`.
 * Does NOT treat static appendix "Към Фактура №" as pass or fail.
 */
export function analyzeDebitNoteDocumentTypePdf(
  pdfText: string,
  extractor: string,
): Phn3948PdfAnalysis {
  const hasBulgarianDebitHeader = /(?:^|\n)\s*Дебитно известие\s*\n\s*№/m.test(pdfText);
  const hasEnglishDebitHeader = /(?:^|\n)\s*DEBIT(\s+NOTE)?\s*\n/m.test(pdfText);
  const hasCorruptDebitWithoutGuarantee = /DEBIT\s*Без\s*гаранция/i.test(pdfText);
  const hasEnglishInvoiceHeader = /(?:^|\n)\s*INVOICE\s*\n\s*№/m.test(pdfText);

  const passed =
    hasBulgarianDebitHeader &&
    !hasEnglishDebitHeader &&
    !hasCorruptDebitWithoutGuarantee &&
    !hasEnglishInvoiceHeader;

  let reason: string;
  if (passed) {
    reason =
      'Debit-note DocumentType header is Bulgarian ("Дебитно известие" before №); ' +
      'English/corrupt DEBIT / INVOICE headers are absent.';
  } else if (hasCorruptDebitWithoutGuarantee) {
    reason =
      'PDF contains corrupt DocumentType "DEBIT Без гаранция" (PHN-3948 actual).';
  } else if (hasEnglishDebitHeader && !hasBulgarianDebitHeader) {
    reason =
      'DocumentType header expanded to English DEBIT/DEBIT NOTE (expected "Дебитно известие" before №).';
  } else if (hasEnglishInvoiceHeader && !hasBulgarianDebitHeader) {
    reason =
      'PDF shows English INVOICE header before № on a debit note (expected "Дебитно известие").';
  } else if (!hasBulgarianDebitHeader) {
    reason =
      'Bulgarian debit-note header pattern "Дебитно известие\\n№" not found in PDF text.';
  } else {
    reason =
      'Bulgarian debit header present but English/corrupt DEBIT or INVOICE header also detected.';
  }

  return {
    passed,
    hasBulgarianDebitHeader,
    hasEnglishDebitHeader,
    hasCorruptDebitWithoutGuarantee,
    hasEnglishInvoiceHeader,
    extractor,
    textLength: pdfText.length,
    reason,
  };
}

export async function createPhn3948InvoiceDocumentTemplate(
  fx: Phn3948Fx,
): Promise<Phn3924InvoiceTemplate> {
  return createPhn3924InvoiceDocumentTemplate(fx as Phn3924Fx);
}

async function createManualInterimBillingRunWithTemplate(
  fx: Phn3948Fx,
  amountExcludingVat: number,
  templateId: number,
): Promise<number> {
  const { Request, Responses, Endpoints } = fx;

  const payload = await buildManualInterimPayload(fx as Pdt2872Fx, amountExcludingVat);
  payload.commonParameters.templateId = templateId;

  const createRes = await Request.post(Endpoints.billingRun, { data: payload });
  await expect(createRes).CheckResponse();
  const billingRunId = asBillingRunId(await createRes.json());
  expect(billingRunId, 'manual interim billing run id must be > 0').toBeGreaterThan(0);
  pushBillingRunPortalRef(Responses, billingRunId, 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT');

  const putRes = await Request.put(`${BILLING_RUN_ROOT}/${billingRunId}`, { data: payload });
  await expect(putRes).CheckResponse();

  return billingRunId;
}

/**
 * Product contract chain (pdt-2872) + completed manual interim parent invoice
 * with commonParameters.templateId set to the Bulgarian INVOICE DOCUMENT template
 * (PHN-3924 Faktura DOCX — contains [[DocumentType]]).
 */
export async function createManualInterimParentInvoiceWithTemplate(
  fx: Phn3948Fx,
  templateId: number,
): Promise<Phn3948ParentInvoice> {
  expect(templateId, 'invoice document templateId must be > 0').toBeGreaterThan(0);

  const amounts = await resolvePdt2872Amounts(fx.Request);

  await test.step('Precondition: product contract chain for manual billing', async () => {
    await preconditionProductContractForManual(fx as Pdt2872Fx);
  });

  let interimBillingRunId = 0;
  await test.step(
    `Precondition: create + complete manual interim parent (templateId=${templateId})`,
    async () => {
      interimBillingRunId = await createManualInterimBillingRunWithTemplate(
        fx,
        amounts.exclFor500,
        templateId,
      );
      await startAndCompleteBillingRun(fx.Request, interimBillingRunId);
    },
  );

  const parentInvoiceId = await test.step(
    'Precondition: resolve parent invoice id from draft-invoices then listing',
    async () => resolveParentInvoiceIdFromDraftsThenListing(fx.Request, interimBillingRunId),
  );
  expect(
    Number.isFinite(parentInvoiceId) && parentInvoiceId > 0,
    `manual interim parent invoice id must be a finite number > 0 (got ${JSON.stringify(parentInvoiceId)})`,
  ).toBe(true);
  expect(parentInvoiceId, 'manual interim parent invoice id').toBeGreaterThan(0);
  if (!fx.Responses.invoice.includes(parentInvoiceId)) {
    fx.Responses.invoice.push(parentInvoiceId);
  }

  const parentBody = await getInvoiceBody(fx.Request, parentInvoiceId);
  const invoiceDocumentType = String(parentBody.invoiceDocumentType ?? '');

  return { parentInvoiceId, interimBillingRunId, invoiceDocumentType };
}

/**
 * POST MANUAL_CREDIT_OR_DEBIT_NOTE with documentType=DEBIT_NOTE and
 * commonParameters.templateId forced to the PHN-3924 Bulgarian invoice template.
 */
export async function createManualDebitNoteWithTemplate(
  fx: Phn3948Fx,
  parentInvoiceId: number,
  templateId: number,
): Promise<{ billingRunId: number }> {
  expect(parentInvoiceId, 'parent invoice id for debit note').toBeGreaterThan(0);
  expect(templateId, 'debit-note billing-run templateId').toBeGreaterThan(0);

  const payload = await buildManualCreditOrDebitNotePayload(
    fx as Pdt2891Fx,
    [parentInvoiceId],
    'DEBIT_NOTE',
  );
  const common = (payload.commonParameters ?? {}) as Record<string, unknown>;
  common.templateId = templateId;
  payload.commonParameters = common;

  const basic = (
    payload as {
      manualCreditOrDebitNoteParameters: {
        manualCreditOrDebitNoteBasicDataParameters: { documentType?: string };
      };
    }
  ).manualCreditOrDebitNoteParameters.manualCreditOrDebitNoteBasicDataParameters;
  expect(basic.documentType, 'manual debit payload documentType').toBe('DEBIT_NOTE');
  expect(payload.billingType, 'billingType').toBe('MANUAL_CREDIT_OR_DEBIT_NOTE');

  const res = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
  await expect(res).CheckResponse();
  const billingRunId = asBillingRunId(await res.json());
  expect(billingRunId, 'manual DEBIT_NOTE billingRunId').toBeGreaterThan(0);
  fx.Responses.billingRun.push({ id: billingRunId });
  return { billingRunId };
}

export async function completeManualDebitNote(
  fx: Phn3948Fx,
  billingRunId: number,
): Promise<number> {
  expect(billingRunId, 'manual DEBIT_NOTE billingRunId must be created before start-billing').toBeGreaterThan(0);

  await test.step('Complete manual DEBIT_NOTE billing run', async () => {
    await startAndCompleteBillingRun(fx.Request, billingRunId);
  });

  const debitNoteId = await test.step(
    'Resolve DEBIT_NOTE invoice id from draft-invoices then listing',
    async () => resolveDebitNoteInvoiceIdFromDraftsThenListing(fx.Request, billingRunId),
  );
  expect(
    Number.isFinite(debitNoteId) && debitNoteId > 0,
    `DEBIT_NOTE invoice id must be a finite number > 0 (got ${JSON.stringify(debitNoteId)})`,
  ).toBe(true);
  expect(debitNoteId, 'manual DEBIT_NOTE invoice id').toBeGreaterThan(0);
  if (!fx.Responses.invoice.includes(debitNoteId)) {
    fx.Responses.invoice.push(debitNoteId);
  }
  return debitNoteId;
}

export async function downloadAndAnalyzeDebitNotePdf(
  Request: Phn3948Fx['Request'],
  debitNoteId: number,
): Promise<Phn3948PdfArtifact> {
  const { files } = await pollInvoiceFileRefs(Request, debitNoteId);
  expect(
    files.length,
    `debit note ${debitNoteId} must expose at least one file (invoiceDocumentId)`,
  ).toBeGreaterThan(0);

  const invoiceDocumentId = files[0].id;
  const buf = await downloadInvoiceDocumentBuffer(Request, invoiceDocumentId);
  const pdfPath = savePdfToTestResults(
    buf,
    `phn-3948-debit-note-${debitNoteId}-doc-${invoiceDocumentId}.pdf`,
  );
  const { text: pdfText, extractor: pdfExtractor } = extractPdfText(pdfPath);
  const analysis = analyzeDebitNoteDocumentTypePdf(pdfText, pdfExtractor);

  return { invoiceDocumentId, pdfPath, pdfText, pdfExtractor, analysis };
}

export { PHN_3924_TEMPLATE_ORIGINAL_NAME };
