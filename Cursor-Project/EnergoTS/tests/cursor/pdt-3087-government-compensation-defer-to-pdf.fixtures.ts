/**
 * PDT-3087 — Government compensation deferred to PDF (standard billing / FOR_VOLUMES only).
 *
 * Correction helpers (runVolumeCorrectionDraftAfterRealize, pickCorrectionCompensationInvoiceId)
 * moved to PDT-3127 scope.
 *
 * Reference spec(s):
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts (FOR_VOLUMES prechain + draft billing)
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts (start-generating / accounting poll)
 * - tests/billing/overTimeOneTime/overTimeOneTime.spec.ts (TC-BE-15 out-of-scope negative)
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts (FileUploadRequest + ExcelJS)
 *
 * Swagger refresh: already executed this session via update-swagger-specs.ps1 (dev/test/dev2/experiment/prod).
 * Backend TC: Cursor-Project/test_cases/Backend/Government_Compensation_Defer_To_PDF_Standard_Billing.md
 *
 * Product scope: FOR_VOLUMES standard billing only.
 * TC-BE-15 negative: non-FOR_VOLUMES must remain unlinked at PDF.
 */

import * as fs from 'fs';
import * as path from 'path';
import * as ExcelJS from 'exceljs';
import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  monthStartFromPeriod,
  runDevVolCompContractAndBbpPrechain,
  type CompensationPayload,
  type DevVolCompFx,
  type DevVolCompPrechainResult,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  DEV_PORTAL_BASE,
} from './dev-volume-billing-two-compensations.fixtures';
import {
  getBillingRunStatus,
  pollBillingRunForStatus,
} from './rps-pod-invoice-due-date.fixtures';
import {
  postPdt2915BillingByProfile,
} from './pdt-2915-invoice-correction-deleted-bbp.fixtures';

export {
  runDevVolCompContractAndBbpPrechain,
  monthStartFromPeriod,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  DEV_PORTAL_BASE,
  getBillingRunStatus,
  pollBillingRunForStatus,
};

export const PDT_3087_KEY = 'PDT-3087';
export const BILLING_RUN_ROOT = 'billing-run';
export const COMP_ROOT = 'government-compensations';
export const MASS_IMPORT_GC_TEMPLATE = 'mass-import/GOVERNMENT_COMPENSATION/template/download';
export const MASS_IMPORT_GC_UPLOAD = 'mass-import/GOVERNMENT_COMPENSATION/files/upload';

const GEN_POLL_MS = 3_000;
const GEN_MAX_MS = 7 * 60 * 1000;
const ACC_MAX_MS = 7 * 60 * 1000;
const TERM_MAX_MS = 5 * 60 * 1000;

export type Pdt3087Fx = DevVolCompFx;
export type FixtureFileUpload = baseFixture['FileUploadRequest'];

export type CompensationView = {
  id: number;
  compensationStatus?: string;
  index?: number | null;
  invoice?: { id?: number } | null;
  liabilityForRecipient?: { id?: number } | null;
  receivableForCustomer?: { id?: number } | null;
  [key: string]: unknown;
};

export type InvoiceView = {
  id: number;
  invoiceNumber?: string;
  invoiceDocumentType?: string;
  invoiceStatus?: string;
  totalAmountIncludingVat?: string | number;
  compensationIndex?: number | null;
  compensations?: Array<{ id?: number }>;
  isCompensationGenerated?: boolean;
  liabilitiesAndReceivables?: Array<{ id?: number; name?: string; type?: string }>;
  [key: string]: unknown;
};

export type CustomerLiabilityView = {
  id: number;
  initialAmount?: number | string;
  currentAmount?: number | string;
  invoiceResponse?: { id?: number | null } | null;
  basisForIssuing?: string;
  [key: string]: unknown;
};

export type MultiPodPrechainResult = DevVolCompPrechainResult & {
  podAId: number;
  podBId: number;
};

export type DualContractPrechainResult = {
  customerAId: number;
  customerBId: number;
  recipientCustomerId: number;
  podAId: number;
  podBId: number;
  contractAId: number;
  contractBId: number;
  contractANumber: string;
  contractBNumber: string;
  documentPeriod: string;
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
  const n = Number(raw);
  if (Number.isFinite(n) && n > 0) {
    return n;
  }
  throw new Error(`Expected numeric entity id, got: ${JSON.stringify(raw)}`);
}

export async function getCompensation(
  Request: Pdt3087Fx['Request'],
  compensationId: number,
): Promise<CompensationView> {
  const res = await Request.get(`${COMP_ROOT}/${compensationId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as CompensationView;
  return { ...body, id: compensationId };
}

export async function getInvoice(
  Request: Pdt3087Fx['Request'],
  invoiceId: number,
): Promise<InvoiceView> {
  const res = await Request.get(`invoice?id=${invoiceId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as InvoiceView;
  return { ...body, id: Number(body.id ?? invoiceId) };
}

export async function getDraftInvoiceIds(
  Request: Pdt3087Fx['Request'],
  billingRunId: number,
): Promise<number[]> {
  const draftRes = await Request.get(
    `${BILLING_RUN_ROOT}/draft-invoices?id=${billingRunId}&page=0&size=50`,
  );
  await expect(draftRes).CheckResponse();
  const draftBody = (await draftRes.json()) as { content?: Array<{ id?: number }> };
  return (draftBody.content ?? [])
    .map((row) => Number(row.id))
    .filter((id) => Number.isFinite(id) && id > 0);
}

/**
 * FOR_VOLUMES draft billing using the **latest** Responses.billingRun index
 * (safe for terminate→reuse in the same test — unlike reference helper hard-coded to index 0).
 */
export async function createForVolumesDraftBillingRun(
  ctx: Pdt3087Fx,
  opts?: { accountingPeriodId?: number; invoiceDate?: string; taxEventDate?: string },
): Promise<{ billingRunId: number; draftInvoiceIds: number[]; billingRunStatus: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  if (opts?.accountingPeriodId && opts.accountingPeriodId > 0) {
    billingPayload.commonParameters.accountingPeriodId = opts.accountingPeriodId;
  }
  if (opts?.invoiceDate) {
    billingPayload.commonParameters.invoiceDate = opts.invoiceDate;
  }
  if (opts?.taxEventDate) {
    billingPayload.commonParameters.taxEventDate = opts.taxEventDate;
  }
  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  const billingIndex = Responses.billingRun.length - 1;
  await GeneratePayload.billing.waitForInvoiceGeneration(true, false, 1, billingIndex);

  const statusRes = await Request.get(`${Endpoints.billingRun}/${billingRunId}`);
  await expect(statusRes).CheckResponse();
  const statusBody = (await statusRes.json()) as {
    commonParameters?: { status?: string };
  };
  const billingRunStatus = String(statusBody.commonParameters?.status ?? '');
  const draftInvoiceIds = await getDraftInvoiceIds(Request, billingRunId);

  for (const id of draftInvoiceIds) {
    if (!Responses.invoice.includes(id as never)) {
      Responses.invoice.push(id);
    }
  }

  return { billingRunId, draftInvoiceIds, billingRunStatus };
}

/** Build a single CompensationRequest payload (clone template — never reuse singleton twice). */
export function buildCompensationPayload(
  GeneratePayload: Pdt3087Fx['GeneratePayload'],
  opts: {
    customerId: number;
    podId: number;
    recipientId: number;
    documentPeriod: string;
    documentAmount?: number;
    reason?: string;
    volumes?: number;
    price?: number;
  },
): CompensationPayload {
  const template = GeneratePayload.energyData.compensation() as CompensationPayload;
  const suffix = randomGens.generateRandomString(true, true, 8);
  const volumes = opts.volumes ?? 100;
  const price = opts.price ?? 0.5;
  const documentAmount = opts.documentAmount ?? volumes * price;
  expect(opts.recipientId, 'recipientId must differ from billed customerId').not.toBe(
    opts.customerId,
  );
  return {
    ...template,
    number: `PDT3087-${suffix}`,
    date: opts.documentPeriod.slice(0, 10),
    documentPeriod: opts.documentPeriod,
    customerId: opts.customerId,
    podId: opts.podId,
    recipientId: opts.recipientId,
    documentCurrencyId: template.documentCurrencyId ?? envVariables.currency,
    volumes,
    price,
    documentAmount,
    reason: opts.reason ?? 'PDT-3087',
  };
}

export async function createGovernmentCompensation(
  ctx: Pdt3087Fx,
  opts: {
    customerId: number;
    podId: number;
    recipientId: number;
    documentPeriod: string;
    documentAmount?: number;
    reason?: string;
    volumes?: number;
    price?: number;
  },
): Promise<{ id: number; payload: CompensationPayload }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = buildCompensationPayload(GeneratePayload, opts);
  const res = await Request.post(Endpoints.compensation, { data: payload });
  await expect(res).CheckResponse();
  const raw = await res.json();
  const id = asEntityId(raw);
  Responses.compensation.push({ id, number: payload.number, ...payload });
  return { id, payload };
}

/** API often returns `{ id: null, … }` instead of JSON `null` for cleared links. */
export function isNullishEntityRef(
  value: { id?: number | null } | null | undefined,
): boolean {
  if (value === null || value === undefined) {
    return true;
  }
  const id = value.id;
  return id === null || id === undefined;
}

export function assertCompensationUnlinked(
  comp: CompensationView,
  label: string,
): void {
  expect(comp.compensationStatus, `${label}: status UNINVOICED`).toBe('UNINVOICED');
  expect(
    isNullishEntityRef(comp.invoice),
    `${label}: invoice must be unlinked (null or { id: null }), got ${JSON.stringify(comp.invoice)}`,
  ).toBe(true);
  const idx = comp.index;
  expect(
    idx === null || idx === undefined,
    `${label}: index must be null/absent (got ${String(idx)})`,
  ).toBe(true);
}

/**
 * Assert compensation linked at PDF (FOR_VOLUMES / correction in-scope paths).
 */
export function assertCompensationLinkedAtPdf(
  comp: CompensationView,
  invoiceId: number,
  label: string,
): void {
  expect(comp.compensationStatus, `${label}: still UNINVOICED until accounting`).toBe(
    'UNINVOICED',
  );
  expect(comp.invoice?.id, `${label}: linked invoice id`).toBe(invoiceId);
  expect(comp.index, `${label}: compensation index must be 0`).toBe(0);
}

export function assertInvoiceCompensationApplied(
  invoice: InvoiceView,
  label: string,
): void {
  const idx = invoice.compensationIndex;
  if (idx !== undefined && idx !== null) {
    expect(idx, `${label}: invoice.compensationIndex`).toBe(0);
  }
  if (typeof invoice.isCompensationGenerated === 'boolean') {
    expect(invoice.isCompensationGenerated, `${label}: isCompensationGenerated`).toBe(true);
  }
  if (Array.isArray(invoice.compensations)) {
    expect(
      invoice.compensations.length,
      `${label}: invoice.compensations must be non-empty after PDF link`,
    ).toBeGreaterThan(0);
  }
}

export function assertInvoiceNoCompensationApplied(
  invoice: InvoiceView,
  label: string,
): void {
  const idx = invoice.compensationIndex;
  expect(
    idx === null || idx === undefined,
    `${label}: compensationIndex must be null/absent (got ${String(idx)})`,
  ).toBe(true);
  if (typeof invoice.isCompensationGenerated === 'boolean') {
    expect(invoice.isCompensationGenerated, `${label}: isCompensationGenerated false`).toBe(
      false,
    );
  }
  if (Array.isArray(invoice.compensations)) {
    expect(invoice.compensations.length, `${label}: compensations empty`).toBe(0);
  }
}

export async function startGeneratingAndWaitGenerated(
  Request: Pdt3087Fx['Request'],
  billingRunId: number,
): Promise<void> {
  const genRes = await Request.patch(
    `${BILLING_RUN_ROOT}/start-generating?billingRunId=${billingRunId}`,
  );
  await expect(genRes).CheckResponse();
  await pollBillingRunForStatus(Request, billingRunId, 'GENERATED', GEN_MAX_MS);
}

export async function startAccountingAndWaitCompleted(
  Request: Pdt3087Fx['Request'],
  billingRunId: number,
): Promise<void> {
  const accRes = await Request.patch(
    `${BILLING_RUN_ROOT}/start-accounting?billingRunId=${billingRunId}`,
  );
  await expect(accRes).CheckResponse();
  await pollBillingRunForStatus(Request, billingRunId, 'COMPLETED', ACC_MAX_MS);
}

export async function terminateBillingRunAndWaitCancelled(
  Request: Pdt3087Fx['Request'],
  billingRunId: number,
): Promise<string> {
  const termRes = await Request.patch(
    `${BILLING_RUN_ROOT}/terminate?billingRunId=${billingRunId}`,
  );
  await expect(termRes).CheckResponse();

  const maxAttempts = Math.max(1, Math.ceil(TERM_MAX_MS / GEN_POLL_MS));
  for (let i = 1; i <= maxAttempts; i++) {
    const status = await getBillingRunStatus(Request, billingRunId);
    if (status === 'CANCELLED') {
      return status;
    }
    if (i < maxAttempts) {
      await new Promise((r) => setTimeout(r, GEN_POLL_MS));
    }
  }
  const final = await getBillingRunStatus(Request, billingRunId);
  expect(final, `billing run ${billingRunId} must reach CANCELLED`).toBe('CANCELLED');
  return final;
}

export async function pollPdfDocumentsPresent(
  Request: Pdt3087Fx['Request'],
  billingRunId: number,
): Promise<number> {
  const maxAttempts = Math.max(1, Math.ceil(GEN_MAX_MS / GEN_POLL_MS));
  for (let i = 1; i <= maxAttempts; i++) {
    const res = await Request.get(
      `${BILLING_RUN_ROOT}/pdf-documents?id=${billingRunId}&page=0&size=50`,
    );
    if (res.ok()) {
      const body = (await res.json()) as { content?: unknown[]; totalElements?: number };
      const count = Array.isArray(body.content)
        ? body.content.length
        : Number(body.totalElements ?? 0);
      if (count > 0) {
        return count;
      }
    }
    if (i < maxAttempts) {
      await new Promise((r) => setTimeout(r, GEN_POLL_MS));
    }
  }
  return 0;
}

export async function exportInvoiceWorkbookOk(
  Request: Pdt3087Fx['Request'],
  billingRunId: number,
): Promise<{ status: number; byteLength: number }> {
  const res = await Request.get(`invoice/export/${billingRunId}`);
  await expect(res).CheckResponse();
  const buf = await res.body();
  return { status: res.status(), byteLength: buf.byteLength };
}

/** Resolve customer / POD / recipient identifier strings for mass-import columns. */
export async function resolveEntityIdentifier(
  Request: Pdt3087Fx['Request'],
  kind: 'customer' | 'pod',
  id: number,
): Promise<string> {
  const pathSeg = kind === 'customer' ? `customer/${id}?version=1` : `pod/${id}?version=1`;
  const maxAttempts = 4;
  let lastError: unknown;
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const res = await Request.get(pathSeg);
      await expect(res).CheckResponse();
      const body = (await res.json()) as {
        identifier?: string;
        customerIdentifier?: string;
        basicParameters?: { identifier?: string };
      };
      const identifier = String(
        body.identifier ?? body.customerIdentifier ?? body.basicParameters?.identifier ?? '',
      ).trim();
      expect(identifier, `${kind} ${id} identifier`).toBeTruthy();
      return identifier;
    } catch (error) {
      lastError = error;
      const message = String((error as Error)?.message ?? error ?? '');
      const retryable = /ECONNRESET|ETIMEDOUT|ECONNREFUSED|socket hang up/i.test(message);
      if (!retryable || attempt === maxAttempts) {
        throw error;
      }
      await new Promise((r) => setTimeout(r, 400 * attempt));
    }
  }
  throw lastError;
}

export async function resolveCurrencyName(
  Request: Pdt3087Fx['Request'],
  currencyId: number | string = envVariables.currency,
): Promise<string> {
  const res = await Request.get(`currencies/${currencyId}`);
  if (res.ok()) {
    const body = (await res.json()) as { name?: string; code?: string };
    const name = String(body.name ?? body.code ?? '');
    if (name) {
      return name;
    }
  }
  const list = await Request.get('currencies?statuses=ACTIVE&page=0&size=50');
  await expect(list).CheckResponse();
  const lj = (await list.json()) as { content?: Array<{ id?: number; name?: string }> };
  const row = (lj.content ?? []).find((c) => Number(c.id) === Number(currencyId));
  const name = String(row?.name ?? 'BGN');
  expect(name, 'currency name for mass import').toBeTruthy();
  return name;
}

export async function buildGovernmentCompensationMassImportBuffer(
  Request: Pdt3087Fx['Request'],
  row: {
    number: string;
    date: string;
    documentPeriod: string;
    volumes: number | string;
    price: number | string;
    reason: string;
    documentAmount: number | string;
    currencyName: string;
    customerIdentifier: string;
    podIdentifier: string;
    recipientIdentifier: string;
  },
): Promise<Buffer> {
  const templateRes = await Request.get(MASS_IMPORT_GC_TEMPLATE);
  await expect(templateRes).CheckResponse();
  const templateBuffer = await templateRes.body();

  const outputDir = path.resolve(__dirname, '../../mass-imports/output');
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }
  const filePath = path.join(
    outputDir,
    `pdt-3087-gc-mi-${Date.now()}-${randomGens.generateRandomString(true, false, 6)}.xlsx`,
  );
  fs.writeFileSync(filePath, templateBuffer);

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.readFile(filePath);
  const worksheet = workbook.worksheets[0];
  expect(worksheet, 'GOVERNMENT_COMPENSATION template worksheet').toBeTruthy();

  // CompensationExcelMapper cols 0–10 → Excel columns A–K on first data row (row 2).
  // EPBExcelUtils.getLocalDateValue requires real Excel date-formatted cells (not ISO strings).
  const dateIso = row.date.includes('T') ? row.date.split('T')[0] : row.date.slice(0, 10);
  const periodIso = row.documentPeriod.includes('T')
    ? row.documentPeriod.split('T')[0]
    : row.documentPeriod.slice(0, 10);
  const toExcelDate = (iso: string): Date => {
    const [y, m, d] = iso.split('-').map((x) => Number(x));
    expect(y && m && d, `valid ISO date for mass import (${iso})`).toBeTruthy();
    return new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
  };

  const dataRow = worksheet.getRow(2);
  dataRow.getCell(1).value = row.number;
  dataRow.getCell(2).value = toExcelDate(dateIso);
  dataRow.getCell(2).numFmt = 'yyyy-mm-dd';
  dataRow.getCell(3).value = toExcelDate(periodIso);
  dataRow.getCell(3).numFmt = 'yyyy-mm-dd';
  dataRow.getCell(4).value = Number(row.volumes);
  dataRow.getCell(5).value = Number(row.price);
  dataRow.getCell(6).value = row.reason;
  dataRow.getCell(7).value = Number(row.documentAmount);
  dataRow.getCell(8).value = row.currencyName;
  dataRow.getCell(9).value = row.customerIdentifier;
  dataRow.getCell(10).value = row.podIdentifier;
  dataRow.getCell(11).value = row.recipientIdentifier;

  await workbook.xlsx.writeFile(filePath);
  const buffer = fs.readFileSync(filePath);
  try {
    fs.unlinkSync(filePath);
  } catch {
    /* ignore */
  }
  return buffer;
}

export async function uploadGovernmentCompensationMassImport(
  FileUploadRequest: FixtureFileUpload,
  fileBuffer: Buffer,
  fileName = 'pdt-3087-government-compensation-mass-import.xlsx',
): Promise<{ status: number; bodyText: string }> {
  const upload = await FileUploadRequest.post(MASS_IMPORT_GC_UPLOAD, {
    multipart: {
      file: {
        name: fileName,
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: fileBuffer,
      },
    },
  });
  const bodyText = await upload.text();
  return { status: upload.status(), bodyText };
}

export async function pollGovernmentCompensationMassImportComplete(
  Request: Pdt3087Fx['Request'],
  processIdsBefore: ReadonlySet<number>,
  maxAttempts = 60,
  delayMs = 2000,
): Promise<number> {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const res = await Request.get('process', {
      params: { page: 0, size: 50, sortBy: 'ID', sortDirection: 'DESC' },
    });
    if (res.ok()) {
      const body = (await res.json()) as {
        content?: Array<{ id?: number; name?: string; status?: string }>;
      };
      const candidates = (body.content ?? [])
        .filter((p) =>
          String(p.name ?? '').includes('GOVERNMENT_COMPENSATION_MASS_IMPORT'),
        )
        .map((p) => ({
          id: Number(p.id),
          status: String(p.status ?? ''),
          name: String(p.name ?? ''),
        }))
        .filter((p) => Number.isFinite(p.id) && p.id > 0 && !processIdsBefore.has(p.id));

      for (const p of candidates) {
        if (/ERROR|FAILED/i.test(p.status) || /ERROR/i.test(p.name)) {
          throw new Error(
            `GOVERNMENT_COMPENSATION mass import failed: id=${p.id} status=${p.status} name=${p.name}`,
          );
        }
        if (/COMPLETE|COMPLETED|SUCCESS|FINISHED/i.test(p.status)) {
          return p.id;
        }
      }
      if (candidates.length > 0) {
        const newest = Math.max(...candidates.map((c) => c.id));
        const detail = await Request.get(`process/${newest}`);
        if (detail.ok()) {
          const dj = (await detail.json()) as { status?: string; name?: string };
          const st = String(dj.status ?? '');
          if (/ERROR|FAILED/i.test(st)) {
            throw new Error(`GOVERNMENT_COMPENSATION mass import process ${newest} error: ${st}`);
          }
          if (/COMPLETE|COMPLETED|SUCCESS|FINISHED/i.test(st)) {
            return newest;
          }
        }
      }
    }

    const notification = await Request.get('notifications?size=30&page=0');
    if (notification.ok()) {
      const body = await notification.json();
      const notifications = Array.isArray(body?.content) ? body.content : [body];
      const hit = notifications.find(
        (n: { notificationType?: string; entityId?: number }) =>
          String(n?.notificationType ?? '').includes('GOVERNMENT_COMPENSATION') &&
          !String(n?.notificationType ?? '').includes('ERROR'),
      );
      if (hit?.entityId && !processIdsBefore.has(Number(hit.entityId))) {
        return Number(hit.entityId);
      }
      const err = notifications.find((n: { notificationType?: string }) =>
        String(n?.notificationType ?? '').includes('GOVERNMENT_COMPENSATION_ERROR'),
      );
      if (err) {
        throw new Error(
          `GOVERNMENT_COMPENSATION mass import error notification: ${JSON.stringify(err)}`,
        );
      }
    }

    if (attempt < maxAttempts - 1) {
      await new Promise((r) => setTimeout(r, delayMs));
    }
  }
  throw new Error('No GOVERNMENT_COMPENSATION_MASS_IMPORT process completed after upload');
}

export async function findCompensationByNumber(
  Request: Pdt3087Fx['Request'],
  number: string,
  opts: { maxAttempts?: number; delayMs?: number } = {},
): Promise<CompensationView | null> {
  const maxAttempts = opts.maxAttempts ?? 15;
  const delayMs = opts.delayMs ?? 2000;
  const needle = number.trim().toLowerCase();

  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const res = await Request.get(COMP_ROOT + '/listing', {
      params: {
        size: 50,
        page: 0,
        searchBy: 'NUMBER',
        prompt: number,
        sortBy: 'CREATE_DATE',
        direction: 'DESC',
      },
    });
    // Listing returns HTTP 206 PARTIAL_CONTENT — still a success body.
    if (res.ok() || res.status() === 206) {
      const body = (await res.json()) as {
        content?: Array<{ id?: number; number?: string }>;
      };
      const row = (body.content ?? []).find(
        (r) => String(r.number ?? '').trim().toLowerCase() === needle,
      );
      if (row?.id) {
        return getCompensation(Request, Number(row.id));
      }
    } else if (attempt === maxAttempts - 1) {
      throw new Error(
        `government-compensations/listing failed status=${res.status()} body=${await res.text()}`,
      );
    }

    if (attempt < maxAttempts - 1) {
      await new Promise((r) => setTimeout(r, delayMs));
    }
  }
  return null;
}

/**
 * TC-BE-15 path: OVER_TIME_ONE_TIME product-contract draft (no FOR_VOLUMES in applicationModelType).
 */
export async function runOverTimeOneTimeContractPrechain(
  ctx: Pdt3087Fx,
): Promise<DevVolCompPrechainResult & { applicationModelType: string[] }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  await test.step('Precondition: billed customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: recipient customer', async () => {
    const recipient = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(recipient).CheckResponse();
    Responses.customer.push(await recipient.json());
  });

  await test.step('Precondition: price component (OVER_TIME_ONE_TIME)', async () => {
    const price = await Request.post(Endpoints.priceComponent, {
      data: GeneratePayload.productAndServices.oneTime(),
    });
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

  await test.step('Precondition: product', async () => {
    const product = await Request.post(Endpoints.product, {
      data: GeneratePayload.productAndServices.product(),
    });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
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

  await test.step('Precondition: activate POD', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0),
    });
    await expect(podActivation).CheckResponse();
  });

  const customerId = asEntityId(Responses.customer[0]);
  const recipientCustomerId = asEntityId(Responses.customer[1]);
  const podId = asEntityId(Responses.pod[0]);
  const documentPeriod = randomGens.generateMonthStartDate('yyyy-mm-dd');

  return {
    customerId,
    recipientCustomerId,
    podId,
    contractId,
    bbpId: 0,
    periodFrom: documentPeriod,
    periodTo: documentPeriod,
    documentPeriod,
    applicationModelType: ['OVER_TIME_ONE_TIME'],
  };
}

export async function createStandardDraftBillingRun(
  ctx: Pdt3087Fx,
  applicationModelType: Array<
    | 'FOR_VOLUMES'
    | 'OVER_TIME_PERIODICAL'
    | 'OVER_TIME_ONE_TIME'
    | 'PER_PIECE'
    | 'INTERIM_AND_ADVANCE_PAYMENT'
    | 'WITH_ELECTRICITY_INVOICE'
  >,
): Promise<{ billingRunId: number; draftInvoiceIds: number[]; billingRunStatus: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billingPayload = await GeneratePayload.billing.billingRun(
    'CONTRACT',
    applicationModelType,
  );
  const postedModels = billingPayload.basicParameters.applicationModelType as string[];
  expect(postedModels, 'billing payload applicationModelType').toEqual(applicationModelType);
  expect(
    postedModels.includes('FOR_VOLUMES'),
    'FOR_VOLUMES presence must match requested applicationModelType (TC-BE-15 gate)',
  ).toBe(applicationModelType.includes('FOR_VOLUMES'));

  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  await GeneratePayload.billing.waitForInvoiceGeneration(true, false, 1, Responses.billingRun.length - 1);

  const statusRes = await Request.get(`${Endpoints.billingRun}/${billingRunId}`);
  await expect(statusRes).CheckResponse();
  const statusBody = (await statusRes.json()) as {
    commonParameters?: { status?: string };
  };
  const billingRunStatus = String(statusBody.commonParameters?.status ?? '');
  const draftInvoiceIds = await getDraftInvoiceIds(Request, billingRunId);
  if (Responses.invoice.length === 0) {
    for (const id of draftInvoiceIds) {
      Responses.invoice.push(id);
    }
  }
  return { billingRunId, draftInvoiceIds, billingRunStatus };
}

/**
 * TC-BE-7: single customer, two PODs on one contract, BBP for both → multi-POD invoice.
 */
export async function runMultiPodForVolumesPrechain(
  ctx: Pdt3087Fx,
): Promise<MultiPodPrechainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  await test.step('Precondition: billed customer', async () => {
    const customer = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(customer).CheckResponse();
    Responses.customer.push(await customer.json());
  });

  await test.step('Precondition: recipient customer', async () => {
    const recipient = await Request.post(Endpoints.customer, {
      data: GeneratePayload.customers.customer_legal(),
    });
    await expect(recipient).CheckResponse();
    Responses.customer.push(await recipient.json());
  });

  await test.step('Precondition: price component (priceSettlement)', async () => {
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

  await test.step('Precondition: POD A + POD B', async () => {
    for (const label of ['A', 'B']) {
      const pod = await Request.post(Endpoints.pod, {
        data: GeneratePayload.pointsOfDelivery.pod_settlement(),
      });
      await expect(pod).CheckResponse();
      Responses.pod.push(await pod.json());
      void label;
    }
  });

  await test.step('Precondition: product', async () => {
    const product = await Request.post(Endpoints.product, {
      data: GeneratePayload.productAndServices.product(),
    });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  let contractId = 0;
  await test.step('Precondition: product contract with both PODs', async () => {
    const contract = await Request.post(Endpoints.productContract, {
      data: await GeneratePayload.contractsAndOrders.product_contract(),
    });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    Responses.productContract.push(body);
    contractId = asEntityId(body);
  });

  await test.step('Precondition: activate POD A and POD B', async () => {
    for (const podIndex of [0, 1]) {
      const podActivation = await Request.post('/contract-pods/manual', {
        data: await GeneratePayload.pointsOfDelivery.pod_activation(podIndex),
      });
      await expect(podActivation).CheckResponse();
    }
  });

  let periodFrom = '';
  let periodTo = '';
  await test.step('Precondition: BBP for POD A and POD B (same period)', async () => {
    for (const podIndex of [0, 1]) {
      const payload = await GeneratePayload.energyData.profile15minute(podIndex);
      payload.timeZone = 'CET';
      payload.profileId = envVariables.profiles;
      const bbp = await postPdt2915BillingByProfile(
        Request,
        payload,
        `PDT-3087 multi-POD BBP pod${podIndex}`,
      );
      await GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');
      periodFrom = String(payload.periodFrom);
      periodTo = String(payload.periodTo);
      Responses.dataByProfiles.push({
        id: bbp.id,
        periodFrom,
        periodTo,
        periodType: payload.periodType,
      });
    }
  });

  const customerId = asEntityId(Responses.customer[0]);
  const recipientCustomerId = asEntityId(Responses.customer[1]);
  const podAId = asEntityId(Responses.pod[0]);
  const podBId = asEntityId(Responses.pod[1]);
  const documentPeriod = monthStartFromPeriod(periodFrom);

  return {
    customerId,
    recipientCustomerId,
    podId: podAId,
    podAId,
    podBId,
    contractId,
    bbpId: asEntityId(Responses.dataByProfiles[0]),
    periodFrom,
    periodTo,
    documentPeriod,
  };
}

/**
 * TC-BE-17: two customers / two contracts / two PODs for a multi-invoice FOR_VOLUMES run.
 */
export async function runDualCustomerDualContractPrechain(
  ctx: Pdt3087Fx,
): Promise<DualContractPrechainResult> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  await test.step('Precondition: customer A + customer B + recipient', async () => {
    for (const label of ['A', 'B', 'recipient']) {
      const customer = await Request.post(Endpoints.customer, {
        data: GeneratePayload.customers.customer_legal(),
      });
      await expect(customer).CheckResponse();
      Responses.customer.push(await customer.json());
      void label;
    }
  });

  await test.step('Precondition: price + terms + product', async () => {
    const payload = GeneratePayload.productAndServices.priceSettlement();
    payload.formulaRequest.expression = '100';
    payload.applicationModelRequest.settlementPeriodsRequest.profiles = [
      { profileId: envVariables.profiles, percentage: 100 },
    ];
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    Responses.priceComponent.push(await price.json());

    const term = await Request.post(Endpoints.terms, {
      data: GeneratePayload.productAndServices.term(),
    });
    await expect(term).CheckResponse();
    Responses.terms.push(await term.json());

    const product = await Request.post(Endpoints.product, {
      data: GeneratePayload.productAndServices.product(),
    });
    await expect(product).CheckResponse();
    Responses.product.push(await product.json());
  });

  await test.step('Precondition: POD A + POD B', async () => {
    for (let i = 0; i < 2; i++) {
      const pod = await Request.post(Endpoints.pod, {
        data: GeneratePayload.pointsOfDelivery.pod_settlement(),
      });
      await expect(pod).CheckResponse();
      Responses.pod.push(await pod.json());
    }
  });

  let contractAId = 0;
  let contractBId = 0;
  let contractANumber = '';
  let contractBNumber = '';

  await test.step('Precondition: contract A (customer A + POD A)', async () => {
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
    contractPayload.basicParameters.customerId = asEntityId(Responses.customer[0]);
    const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    Responses.productContract.push(body);
    contractAId = asEntityId(body);
    const view = await Request.get(`product-contract/${contractAId}?version=1`);
    await expect(view).CheckResponse();
    const viewBody = (await view.json()) as {
      basicParameters?: { contractNumber?: string };
    };
    contractANumber = String(viewBody.basicParameters?.contractNumber ?? '');
    expect(contractANumber, 'contract A number').toBeTruthy();
  });

  await test.step('Precondition: activate POD A on contract A', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(0, undefined, undefined, 0),
    });
    await expect(podActivation).CheckResponse();
  });

  await test.step('Precondition: contract B (customer B + POD B)', async () => {
    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract(1, 0, 1);
    // Generator hard-codes customerId to customer[0]; override to customer B.
    contractPayload.basicParameters.customerId = asEntityId(Responses.customer[1]);
    const getCustomerB = await Request.get(
      `customer/${asEntityId(Responses.customer[1])}?version=1`,
    );
    await expect(getCustomerB).CheckResponse();
    const customerB = (await getCustomerB.json()) as {
      communicationData?: Array<{ id?: number }>;
    };
    const commId = customerB.communicationData?.[0]?.id;
    if (commId) {
      contractPayload.basicParameters.communicationDataBillingId = commId;
      contractPayload.basicParameters.communicationDataContractId = commId;
    }
    const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
    await expect(contract).CheckResponse();
    const body = await contract.json();
    Responses.productContract.push(body);
    contractBId = asEntityId(body);
    const view = await Request.get(`product-contract/${contractBId}?version=1`);
    await expect(view).CheckResponse();
    const viewBody = (await view.json()) as {
      basicParameters?: { contractNumber?: string };
    };
    contractBNumber = String(viewBody.basicParameters?.contractNumber ?? '');
    expect(contractBNumber, 'contract B number').toBeTruthy();
  });

  await test.step('Precondition: activate POD B on contract B', async () => {
    const podActivation = await Request.post('/contract-pods/manual', {
      data: await GeneratePayload.pointsOfDelivery.pod_activation(1, undefined, undefined, 1),
    });
    await expect(podActivation).CheckResponse();
  });

  let documentPeriod = '';
  await test.step('Precondition: BBP for POD A and POD B', async () => {
    for (const podIndex of [0, 1]) {
      const payload = await GeneratePayload.energyData.profile15minute(podIndex);
      payload.timeZone = 'CET';
      payload.profileId = envVariables.profiles;
      const bbp = await postPdt2915BillingByProfile(
        Request,
        payload,
        `PDT-3087 dual-contract BBP pod${podIndex}`,
      );
      await GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');
      documentPeriod = monthStartFromPeriod(String(payload.periodFrom));
      Responses.dataByProfiles.push({
        id: bbp.id,
        periodFrom: String(payload.periodFrom),
        periodTo: String(payload.periodTo),
        periodType: payload.periodType,
      });
    }
  });

  return {
    customerAId: asEntityId(Responses.customer[0]),
    customerBId: asEntityId(Responses.customer[1]),
    recipientCustomerId: asEntityId(Responses.customer[2]),
    podAId: asEntityId(Responses.pod[0]),
    podBId: asEntityId(Responses.pod[1]),
    contractAId,
    contractBId,
    contractANumber,
    contractBNumber,
    documentPeriod,
  };
}

/** Create FOR_VOLUMES draft for an explicit comma-separated contract-number list (no spaces). */
export async function createForVolumesDraftBillingRunForContractList(
  ctx: Pdt3087Fx,
  contractNumbers: string[],
  expectedInvoiceCount = 1,
): Promise<{ billingRunId: number; draftInvoiceIds: number[]; billingRunStatus: string }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  expect(contractNumbers.length, 'at least one contract number').toBeGreaterThanOrEqual(1);
  for (const n of contractNumbers) {
    expect(n, 'contract number must be non-empty').toBeTruthy();
  }

  const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
  // Phoenix BillingRunService splits on "," without trim — join with no spaces.
  billingPayload.basicParameters.listOfCustomersContractsOrPOD = contractNumbers.join(',');

  const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
  await expect(billingRun).CheckResponse();
  const billingRaw = await billingRun.json();
  const billingRunId = asEntityId(billingRaw);
  Responses.billingRun.push(
    typeof billingRaw === 'object' && billingRaw !== null ? billingRaw : { id: billingRunId },
  );

  const billingIndex = Responses.billingRun.length - 1;
  await GeneratePayload.billing.waitForInvoiceGeneration(
    true,
    false,
    expectedInvoiceCount,
    billingIndex,
  );

  const statusRes = await Request.get(`${Endpoints.billingRun}/${billingRunId}`);
  await expect(statusRes).CheckResponse();
  const statusBody = (await statusRes.json()) as {
    commonParameters?: { status?: string };
  };
  const billingRunStatus = String(statusBody.commonParameters?.status ?? '');
  const draftInvoiceIds = await getDraftInvoiceIds(Request, billingRunId);
  expect(
    draftInvoiceIds.length,
    `expected ≥${expectedInvoiceCount} drafts for contracts [${contractNumbers.join(',')}]`,
  ).toBeGreaterThanOrEqual(expectedInvoiceCount);

  for (const id of draftInvoiceIds) {
    if (!Responses.invoice.includes(id as never)) {
      Responses.invoice.push(id);
    }
  }

  return { billingRunId, draftInvoiceIds, billingRunStatus };
}

export async function createManualCustomerLiability(
  ctx: Pdt3087Fx,
  opts: {
    customerId: number;
    initialAmount: number;
    basisForIssuing: string;
  },
): Promise<{ id: number; payload: Record<string, unknown> }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = GeneratePayload.receivablesManagement.customer_liability() as Record<
    string,
    unknown
  >;
  payload.customerId = opts.customerId;
  payload.initialAmount = opts.initialAmount;
  payload.basisForIssuing = opts.basisForIssuing;
  // Swagger CustomerLiabilityRequest: dueDate/occurrenceDate are date (ISO). Generator uses dd-mm-yyyy;
  // keep generator dates (proven in EnergoTS receivable flows) unless empty.
  const res = await Request.post(Endpoints.customerLiability, { data: payload });
  await expect(res).CheckResponse();
  const raw = await res.json();
  const id = asEntityId(raw);
  Responses.customerLiability.push(typeof raw === 'object' && raw !== null ? raw : { id });
  return { id, payload };
}

export async function getCustomerLiability(
  Request: Pdt3087Fx['Request'],
  liabilityId: number,
): Promise<CustomerLiabilityView> {
  const res = await Request.get(`customer-liability/${liabilityId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as CustomerLiabilityView;
  return { ...body, id: Number(body.id ?? liabilityId) };
}

/** Resolve invoice-sourced customer liability id from invoice.liabilitiesAndReceivables (type LIABILITY). */
export async function resolveInvoiceCustomerLiabilityId(
  Request: Pdt3087Fx['Request'],
  invoiceId: number,
): Promise<number> {
  const invoice = await getInvoice(Request, invoiceId);
  const rows = invoice.liabilitiesAndReceivables ?? [];
  const liability = rows.find(
    (row) => String(row.type ?? '').toUpperCase() === 'LIABILITY' && Number(row.id) > 0,
  );
  expect(
    liability?.id,
    `invoice ${invoiceId} must expose LIABILITY in liabilitiesAndReceivables after accounting (got ${JSON.stringify(rows)})`,
  ).toBeTruthy();
  return Number(liability!.id);
}

export function toAmountNumber(value: unknown): number {
  const n = Number(value);
  expect(Number.isFinite(n), `numeric amount, got ${JSON.stringify(value)}`).toBe(true);
  return n;
}
