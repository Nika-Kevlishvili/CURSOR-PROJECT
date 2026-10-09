/**
 * PDT-3341 — Additional parameters 1–12 on Government compensations.
 *
 * 3341-specific helpers: additionalParameter payloads, mass-import cols 11–22 (Excel L–W),
 * old-template / wrong-header workbooks, generate-invoice-data parse of TableCompensations /
 * SDCompensations (PascalCase AdditionalParameter1–12).
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 *   (createGovernmentCompensation / getCompensation / runDevVolCompContractAndBbpPrechain /
 *    createForVolumesDraftBillingRun / startGeneratingAndWaitGenerated /
 *    startAccountingAndWaitCompleted / mass import template+upload+poll /
 *    Endpoints.compensation = government-compensations / runMultiPodForVolumesPrechain)
 * - tests/cursor/pdt-3223-invoice-json-price-component-order.fixtures.ts
 *   (GET billing-run/generate-invoice-data?invoiceId=)
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 *   (two-POD / recipient ≠ billed customer chain — via 3087 re-exports)
 *
 * Swagger (dev): Cursor-Project/config/swagger/dev/swagger-spec.json
 * CompensationRequest required: customerId, date, documentAmount, documentCurrencyId,
 * documentPeriod, number, podId, price, reason, recipientId, volumes.
 * Optional additionalParameter1–12: string minLength 0 maxLength 64.
 * Listing CompensationListingResponse: no additionalParameter properties.
 * Invoice JSON: BillingRunDocumentDetailedDataCompensations /
 * BillingRunDocumentSummaryDataCompensations PascalCase AdditionalParameter1–12.
 *
 * Do not modify PDT-3087 spec/fixtures. Payload generator has no additionalParameter
 * fields — they are spread onto the cloned CompensationRequest after generate.
 */

import * as ExcelJS from 'exceljs';
import { expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  COMP_ROOT,
  MASS_IMPORT_GC_TEMPLATE,
  buildGovernmentCompensationMassImportBuffer,
  buildCompensationPayload,
  createForVolumesDraftBillingRun,
  findCompensationByNumber,
  getCompensation,
  getCustomerLiability,
  getInvoice,
  pollGovernmentCompensationMassImportComplete,
  pollPdfDocumentsPresent,
  resolveCurrencyName,
  resolveEntityIdentifier,
  runDevVolCompContractAndBbpPrechain,
  runMultiPodForVolumesPrechain,
  startAccountingAndWaitCompleted,
  startGeneratingAndWaitGenerated,
  toAmountNumber,
  uploadGovernmentCompensationMassImport,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  DEV_PORTAL_BASE,
  type CompensationView,
  type FixtureFileUpload,
  type Pdt3087Fx,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import type { CompensationPayload } from './dev-volume-billing-two-compensations.fixtures';
import { fetchPdt3223InvoiceDocumentModel } from './pdt-3223-invoice-json-price-component-order.fixtures';

export {
  COMP_ROOT,
  MASS_IMPORT_GC_TEMPLATE,
  createForVolumesDraftBillingRun,
  findCompensationByNumber,
  getCompensation,
  getCustomerLiability,
  getInvoice,
  pollGovernmentCompensationMassImportComplete,
  pollPdfDocumentsPresent,
  resolveCurrencyName,
  resolveEntityIdentifier,
  runDevVolCompContractAndBbpPrechain,
  runMultiPodForVolumesPrechain,
  startAccountingAndWaitCompleted,
  startGeneratingAndWaitGenerated,
  toAmountNumber,
  uploadGovernmentCompensationMassImport,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  DEV_PORTAL_BASE,
};

export const PDT_3341_KEY = 'PDT-3341';
export const SIMPLE_TIMEOUT_MS = 5 * 60 * 1000;
export const IMPORT_TIMEOUT_MS = 12 * 60 * 1000;
export const INVOICE_TIMEOUT_MS = 25 * 60 * 1000;

export const PARAM64_A = 'A'.repeat(64);
export const PARAM64_B = 'B'.repeat(64);
export const PARAM65_C = 'C'.repeat(65);
export const PARAM65_D = 'D'.repeat(65);
export const PARAM65_E = 'E'.repeat(65);

export const ADDITIONAL_PARAM_KEYS = Array.from(
  { length: 12 },
  (_, i) => `additionalParameter${i + 1}` as const,
);

export const INVOICE_PARAM_KEYS = Array.from(
  { length: 12 },
  (_, i) => `AdditionalParameter${i + 1}` as const,
);

export const INVALID_FILE_FORMAT = 'Invalid file format';

export type Pdt3341Fx = Pdt3087Fx;
export type AdditionalParameterMap = {
  [K in (typeof ADDITIONAL_PARAM_KEYS)[number]]?: string | null;
};

export type Pdt3341CompensationPayload = CompensationPayload & AdditionalParameterMap;

export type Pdt3341CustomerPodPrechain = {
  customerId: number;
  recipientCustomerId: number;
  podId: number;
  documentPeriod: string;
  documentCurrencyId: number | string;
};

export type InvoiceDocumentModel = {
  DD?: Array<{
    PODID?: string;
    TableCompensations?: Array<Record<string, unknown>>;
    [key: string]: unknown;
  }>;
  SDCompensations?: Array<Record<string, unknown>>;
  FinalLiabilityAmount?: unknown;
  TotalInclVat?: unknown;
  CurrencyAbr?: unknown;
  CurrencyPrintName?: unknown;
  [key: string]: unknown;
};

export type MassImportProcessOutcome = {
  id: number;
  status: string;
  name: string;
  failed: boolean;
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

export function uniqueCompNumber(prefix: string): string {
  return `${prefix}-${randomGens.generateRandomString(true, false, 8)}`;
}

export function dateOnDayOfPeriod(documentPeriod: string, day: number): string {
  const ymd = documentPeriod.includes('T')
    ? documentPeriod.split('T')[0]
    : documentPeriod.slice(0, 10);
  const y = ymd.slice(0, 4);
  const m = ymd.slice(5, 7);
  const d = String(Math.min(Math.max(day, 1), 28)).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

export function sizeFragment(n: number): string {
  return `additionalParameter${n}-[additionalParameter${n}] length must be between 0 and 64;`;
}

export function invoicedLockFragment(compensationId: number): string {
  return `Government Compensation not found with given id - ${compensationId} or cannot be updated`;
}

export function isEmptyOrNull(value: unknown): boolean {
  return value === null || value === undefined || value === '';
}

export function filledParams(prefix: string): AdditionalParameterMap {
  const out: AdditionalParameterMap = {};
  for (let i = 1; i <= 12; i++) {
    out[`additionalParameter${i}` as keyof AdditionalParameterMap] = `${prefix}${i}`;
  }
  return out;
}

export function assertAdditionalKeysPresent(
  body: Record<string, unknown>,
  label: string,
): void {
  for (const key of ADDITIONAL_PARAM_KEYS) {
    expect(key in body, `${label}: key ${key} must be present (not omitted)`).toBe(true);
  }
}

export function assertAdditionalExact(
  body: Record<string, unknown>,
  expected: AdditionalParameterMap,
  label: string,
): void {
  for (const key of ADDITIONAL_PARAM_KEYS) {
    if (Object.prototype.hasOwnProperty.call(expected, key)) {
      const exp = expected[key];
      if (exp === null || exp === '') {
        expect(
          isEmptyOrNull(body[key]),
          `${label}: ${key} expected empty/null, got ${JSON.stringify(body[key])}`,
        ).toBe(true);
      } else {
        expect(body[key], `${label}: ${key}`).toBe(exp);
      }
    } else {
      expect(
        isEmptyOrNull(body[key]),
        `${label}: unused ${key} expected empty/null, got ${JSON.stringify(body[key])}`,
      ).toBe(true);
    }
  }
}

export function assertInvoiceParamsExact(
  row: Record<string, unknown>,
  expectedCamel: AdditionalParameterMap,
  label: string,
): void {
  for (let i = 1; i <= 12; i++) {
    const invoiceKey = `AdditionalParameter${i}`;
    const camelKey = `additionalParameter${i}` as keyof AdditionalParameterMap;
    expect(
      invoiceKey in row,
      `${label}: ${invoiceKey} must be present on invoice JSON`,
    ).toBe(true);
    const exp = Object.prototype.hasOwnProperty.call(expectedCamel, camelKey)
      ? expectedCamel[camelKey]
      : undefined;
    if (exp === undefined || exp === null || exp === '') {
      expect(
        isEmptyOrNull(row[invoiceKey]),
        `${label}: ${invoiceKey} unused expected empty/null, got ${JSON.stringify(row[invoiceKey])}`,
      ).toBe(true);
    } else {
      expect(row[invoiceKey], `${label}: ${invoiceKey}`).toBe(exp);
    }
  }
}

export function listingHasAdditionalParameterKeys(row: Record<string, unknown>): string[] {
  return Object.keys(row).filter((k) => /additionalparameter/i.test(k));
}

export async function readHttpError(res: {
  status: () => number;
  text: () => Promise<string>;
}): Promise<{ status: number; text: string }> {
  const status = res.status();
  const text = await res.text();
  return { status, text };
}

/** Customer + recipient + POD only (no billing). Recipient ≠ billed customer. */
export async function runPdt3341CustomerPodPrechain(
  ctx: Pdt3341Fx,
): Promise<Pdt3341CustomerPodPrechain> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  const billed = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(billed).CheckResponse();
  Responses.customer.push(await billed.json());

  const recipient = await Request.post(Endpoints.customer, {
    data: GeneratePayload.customers.customer_legal(),
  });
  await expect(recipient).CheckResponse();
  Responses.customer.push(await recipient.json());

  const pod = await Request.post(Endpoints.pod, {
    data: GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(pod).CheckResponse();
  Responses.pod.push(await pod.json());

  const customerId = asEntityId(Responses.customer[0]);
  const recipientCustomerId = asEntityId(Responses.customer[1]);
  expect(recipientCustomerId, 'recipient must differ from billed customer').not.toBe(customerId);
  const podId = asEntityId(Responses.pod[0]);
  const documentPeriod = randomGens.generateMonthStartDate('yyyy-mm-dd');
  const documentCurrencyId = envVariables.currency;

  return { customerId, recipientCustomerId, podId, documentPeriod, documentCurrencyId };
}

export function buildPdt3341CompensationPayload(
  GeneratePayload: Pdt3341Fx['GeneratePayload'],
  opts: {
    customerId: number;
    podId: number;
    recipientId: number;
    documentPeriod: string;
    number: string;
    date?: string;
    documentAmount: number;
    volumes: number;
    price: number;
    reason: string;
    additional?: AdditionalParameterMap;
  },
): Pdt3341CompensationPayload {
  const base = buildCompensationPayload(GeneratePayload, {
    customerId: opts.customerId,
    podId: opts.podId,
    recipientId: opts.recipientId,
    documentPeriod: opts.documentPeriod,
    documentAmount: opts.documentAmount,
    volumes: opts.volumes,
    price: opts.price,
    reason: opts.reason,
  }) as Pdt3341CompensationPayload;

  const payload: Pdt3341CompensationPayload = {
    ...base,
    number: opts.number,
    date: opts.date ?? dateOnDayOfPeriod(opts.documentPeriod, 15),
    documentPeriod: opts.documentPeriod,
    documentAmount: opts.documentAmount,
    volumes: opts.volumes,
    price: opts.price,
    reason: opts.reason,
    documentCurrencyId: base.documentCurrencyId ?? envVariables.currency,
  };

  for (const key of ADDITIONAL_PARAM_KEYS) {
    delete payload[key];
  }
  if (opts.additional) {
    for (const key of ADDITIONAL_PARAM_KEYS) {
      if (Object.prototype.hasOwnProperty.call(opts.additional, key)) {
        payload[key] = opts.additional[key];
      }
    }
  }
  return payload;
}

export async function createPdt3341Compensation(
  ctx: Pdt3341Fx,
  opts: Parameters<typeof buildPdt3341CompensationPayload>[1],
): Promise<{ id: number; payload: Pdt3341CompensationPayload }> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const payload = buildPdt3341CompensationPayload(GeneratePayload, opts);
  const res = await Request.post(Endpoints.compensation, { data: payload });
  await expect(res).CheckResponse();
  const raw = await res.json();
  const id = asEntityId(raw);
  Responses.compensation.push({ id, number: payload.number, ...payload });
  return { id, payload };
}

export async function putPdt3341Compensation(
  ctx: Pdt3341Fx,
  compensationId: number,
  payload: Pdt3341CompensationPayload,
): Promise<{ status: number; id?: number }> {
  const res = await ctx.Request.put(`${ctx.Endpoints.compensation}/${compensationId}`, {
    data: payload,
  });
  await expect(res).CheckResponse();
  const raw = await res.json();
  return { status: res.status(), id: asEntityId(raw) };
}

export function clonePayloadWithAdditional(
  payload: Pdt3341CompensationPayload,
  additional: AdditionalParameterMap,
): Pdt3341CompensationPayload {
  const next: Pdt3341CompensationPayload = { ...payload };
  for (const key of ADDITIONAL_PARAM_KEYS) {
    delete next[key];
  }
  for (const key of ADDITIONAL_PARAM_KEYS) {
    if (Object.prototype.hasOwnProperty.call(additional, key)) {
      next[key] = additional[key];
    } else if (Object.prototype.hasOwnProperty.call(payload, key)) {
      next[key] = payload[key];
    }
  }
  return next;
}

export async function getCustomerReceivable(
  Request: Pdt3341Fx['Request'],
  receivableId: number,
): Promise<{ id: number; initialAmount?: number | string; currentAmount?: number | string }> {
  const res = await Request.get(`customer-receivable/${receivableId}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    id?: number;
    initialAmount?: number | string;
    currentAmount?: number | string;
  };
  return { ...body, id: Number(body.id ?? receivableId) };
}

export async function getCompensationListingRow(
  Request: Pdt3341Fx['Request'],
  number: string,
): Promise<{ status: number; row: Record<string, unknown> | null }> {
  const res = await Request.get(`${COMP_ROOT}/listing`, {
    params: {
      size: 50,
      page: 0,
      searchBy: 'NUMBER',
      prompt: number,
      sortBy: 'CREATE_DATE',
      direction: 'DESC',
    },
  });
  const status = res.status();
  expect(
    res.ok() || status === 206,
    `listing HTTP 200/206, got ${status}`,
  ).toBe(true);
  const body = (await res.json()) as { content?: Array<Record<string, unknown>> };
  const needle = number.trim().toLowerCase();
  const row =
    (body.content ?? []).find(
      (r) => String(r.number ?? '').trim().toLowerCase() === needle,
    ) ?? null;
  return { status, row };
}

export async function fetchPdt3341InvoiceDocument(
  Request: Pdt3341Fx['Request'],
  invoiceId: number,
): Promise<InvoiceDocumentModel> {
  // PDT-3223 helper asserts CheckResponse on GET billing-run/generate-invoice-data?invoiceId=
  const partial = await fetchPdt3223InvoiceDocumentModel(Request, invoiceId);
  return partial as InvoiceDocumentModel;
}

export function findTableCompensationRows(
  document: InvoiceDocumentModel,
  documentNumber: string,
): Array<{ podId?: string; row: Record<string, unknown> }> {
  const needle = documentNumber.trim().toLowerCase();
  const out: Array<{ podId?: string; row: Record<string, unknown> }> = [];
  for (const dd of document.DD ?? []) {
    for (const row of dd.TableCompensations ?? []) {
      if (String(row.DocumentNumber ?? '').trim().toLowerCase() === needle) {
        out.push({ podId: dd.PODID != null ? String(dd.PODID) : undefined, row });
      }
    }
  }
  return out;
}

export function findSdCompensationRows(
  document: InvoiceDocumentModel,
  documentNumber: string,
): Array<Record<string, unknown>> {
  const needle = documentNumber.trim().toLowerCase();
  return (document.SDCompensations ?? []).filter(
    (row) => String(row.DocumentNumber ?? '').trim().toLowerCase() === needle,
  );
}

export async function downloadGcTemplateBuffer(
  Request: Pdt3341Fx['Request'],
): Promise<{ status: number; buffer: Buffer }> {
  const res = await Request.get(MASS_IMPORT_GC_TEMPLATE);
  await expect(res).CheckResponse();
  return { status: res.status(), buffer: await res.body() };
}

export async function readExcelHeaderRow(buffer: Buffer): Promise<string[]> {
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buffer);
  const worksheet = workbook.worksheets[0];
  expect(worksheet, 'Excel worksheet').toBeTruthy();
  const row = worksheet!.getRow(1);
  let last = 0;
  row.eachCell({ includeEmpty: false }, (cell, colNumber) => {
    const text = String(cell.value ?? '').trim();
    if (text) {
      last = Math.max(last, colNumber);
    }
  });
  const headers: string[] = [];
  for (let c = 1; c <= last; c++) {
    headers.push(String(row.getCell(c).value ?? '').trim());
  }
  return headers;
}

function toExcelDate(iso: string): Date {
  const day = iso.includes('T') ? iso.split('T')[0] : iso.slice(0, 10);
  const [y, m, d] = day.split('-').map((x) => Number(x));
  expect(y && m && d, `valid ISO date (${iso})`).toBeTruthy();
  return new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
}

function fillCoreMassImportRow(
  dataRow: ExcelJS.Row,
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
): void {
  dataRow.getCell(1).value = row.number;
  dataRow.getCell(2).value = toExcelDate(row.date);
  dataRow.getCell(2).numFmt = 'yyyy-mm-dd';
  dataRow.getCell(3).value = toExcelDate(row.documentPeriod);
  dataRow.getCell(3).numFmt = 'yyyy-mm-dd';
  dataRow.getCell(4).value = Number(row.volumes);
  dataRow.getCell(5).value = Number(row.price);
  dataRow.getCell(6).value = row.reason;
  dataRow.getCell(7).value = Number(row.documentAmount);
  dataRow.getCell(8).value = row.currencyName;
  dataRow.getCell(9).value = row.customerIdentifier;
  dataRow.getCell(10).value = row.podIdentifier;
  dataRow.getCell(11).value = row.recipientIdentifier;
}

/**
 * Copy FTP template (3087 fills A–K), then write additionalParameter1–12 in Excel L–W
 * (cells 12–23 = mapper cols 11–22). `undefined` leaves the cell blank.
 */
export async function buildPdt3341MassImportBuffer(
  Request: Pdt3341Fx['Request'],
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
  additional: Array<string | null | undefined>,
): Promise<Buffer> {
  const base = await buildGovernmentCompensationMassImportBuffer(Request, row);
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(base);
  const worksheet = workbook.worksheets[0];
  expect(worksheet, 'GOVERNMENT_COMPENSATION template worksheet').toBeTruthy();
  const dataRow = worksheet!.getRow(2);
  for (let i = 0; i < 12; i++) {
    const val = additional[i];
    if (val === undefined) {
      continue;
    }
    dataRow.getCell(12 + i).value = val === null ? '' : val;
  }
  const out = await workbook.xlsx.writeBuffer();
  return Buffer.from(out);
}

/** Old pre-PDT-3341 file: only mapper cols 0–10 (no Additional Parameter 1–12 headers). */
export async function buildOldFormatGcMassImportBuffer(
  Request: Pdt3341Fx['Request'],
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
  const { buffer } = await downloadGcTemplateBuffer(Request);
  const headers = await readExcelHeaderRow(buffer);
  const first11 = headers.slice(0, 11);
  expect(first11.length, 'old-format file uses first 11 template headers').toBe(11);

  const workbook = new ExcelJS.Workbook();
  const worksheet = workbook.addWorksheet('Import');
  const headerRow = worksheet.getRow(1);
  first11.forEach((h, i) => {
    headerRow.getCell(i + 1).value = h;
  });
  fillCoreMassImportRow(worksheet.getRow(2), row);
  const out = await workbook.xlsx.writeBuffer();
  return Buffer.from(out);
}

/** 23 columns: cells 0–10 match template; cells 11–22 headers = Extra Param N. */
export async function buildWrongHeaderGcMassImportBuffer(
  Request: Pdt3341Fx['Request'],
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
  col11Value = 'SHOULD-NOT-IMPORT',
): Promise<Buffer> {
  const { buffer } = await downloadGcTemplateBuffer(Request);
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buffer);
  const worksheet = workbook.worksheets[0];
  expect(worksheet, 'template worksheet').toBeTruthy();
  const headerRow = worksheet!.getRow(1);
  for (let i = 1; i <= 12; i++) {
    headerRow.getCell(11 + i).value = `Extra Param ${i}`;
  }
  fillCoreMassImportRow(worksheet!.getRow(2), row);
  worksheet!.getRow(2).getCell(12).value = col11Value;
  const out = await workbook.xlsx.writeBuffer();
  return Buffer.from(out);
}

export async function snapshotGcProcessIds(
  Request: Pdt3341Fx['Request'],
): Promise<Set<number>> {
  const beforeIds = new Set<number>();
  const beforeProcesses = await Request.get('process', {
    params: { page: 0, size: 50, sortBy: 'ID', sortDirection: 'DESC' },
  });
  if (beforeProcesses.ok()) {
    const body = (await beforeProcesses.json()) as { content?: Array<{ id?: number }> };
    for (const row of body.content ?? []) {
      const id = Number(row.id);
      if (Number.isFinite(id) && id > 0) {
        beforeIds.add(id);
      }
    }
  }
  return beforeIds;
}

/**
 * Poll GOVERNMENT_COMPENSATION_MASS_IMPORT until complete or error (does not throw on ERROR).
 */
export async function pollGcMassImportSettled(
  Request: Pdt3341Fx['Request'],
  processIdsBefore: ReadonlySet<number>,
  maxAttempts = 60,
  delayMs = 2000,
): Promise<MassImportProcessOutcome | null> {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const res = await Request.get('process', {
      params: { page: 0, size: 50, sortBy: 'ID', sortDirection: 'DESC' },
    });
    if (res.ok()) {
      const body = (await res.json()) as {
        content?: Array<{ id?: number; name?: string; status?: string }>;
      };
      const candidates = (body.content ?? [])
        .filter((p) => String(p.name ?? '').includes('GOVERNMENT_COMPENSATION_MASS_IMPORT'))
        .map((p) => ({
          id: Number(p.id),
          status: String(p.status ?? ''),
          name: String(p.name ?? ''),
        }))
        .filter((p) => Number.isFinite(p.id) && p.id > 0 && !processIdsBefore.has(p.id));

      for (const p of candidates) {
        const failed = /ERROR|FAILED/i.test(p.status) || /ERROR/i.test(p.name);
        const done = failed || /COMPLETE|COMPLETED|SUCCESS|FINISHED/i.test(p.status);
        if (done) {
          return { ...p, failed };
        }
      }
      if (candidates.length > 0) {
        const newest = Math.max(...candidates.map((c) => c.id));
        const detail = await Request.get(`process/${newest}`);
        if (detail.ok()) {
          const dj = (await detail.json()) as { status?: string; name?: string };
          const st = String(dj.status ?? '');
          const name = String(dj.name ?? '');
          const failed = /ERROR|FAILED/i.test(st) || /ERROR/i.test(name);
          const done = failed || /COMPLETE|COMPLETED|SUCCESS|FINISHED/i.test(st);
          if (done) {
            return { id: newest, status: st, name, failed };
          }
        }
      }
    }
    if (attempt < maxAttempts - 1) {
      await new Promise((r) => setTimeout(r, delayMs));
    }
  }
  return null;
}

export async function collectMassImportErrorBlob(
  Request: Pdt3341Fx['Request'],
  processId: number,
): Promise<string> {
  const parts: string[] = [];
  const detail = await Request.get(`process/${processId}`);
  if (detail.ok() || detail.status() === 206) {
    parts.push(JSON.stringify(await detail.json()));
  } else {
    parts.push(await detail.text());
  }
  const report = await Request.get(`process/${processId}/report/download`, {
    params: { multiSheetExcelType: 'MASS_IMPORT_ERROR_REPORT' },
  });
  if (report.ok()) {
    const buf = await report.body();
    try {
      const workbook = new ExcelJS.Workbook();
      await workbook.xlsx.load(buf);
      const worksheet = workbook.worksheets[0];
      worksheet?.eachRow((row) => {
        const cells: string[] = [];
        row.eachCell({ includeEmpty: true }, (cell) => {
          cells.push(String(cell.value ?? ''));
        });
        parts.push(cells.join(' | '));
      });
    } catch {
      parts.push(`report-bytes=${buf.byteLength}`);
    }
  } else {
    parts.push(await report.text());
  }
  return parts.join('\n');
}

/**
 * AC-7 dual-path: upload HTTP 400 with `Invalid file format`, or 202 + process/report
 * containing the same fragment and no created compensation.
 */
export async function assertInvalidFileFormatDualPath(
  Request: Pdt3341Fx['Request'],
  upload: { status: number; bodyText: string },
  processIdsBefore: ReadonlySet<number>,
  number: string,
): Promise<{ path: 'upload-400' | 'process-error'; blob: string }> {
  if (upload.status === 400) {
    expect(upload.bodyText, 'upload 400 body').toContain(INVALID_FILE_FORMAT);
    const found = await findCompensationByNumber(Request, number, {
      maxAttempts: 3,
      delayMs: 1000,
    });
    expect(found, `no compensation ${number} after invalid upload`).toBeNull();
    return { path: 'upload-400', blob: upload.bodyText };
  }

  expect(
    [202, 200].includes(upload.status),
    `upload accepted as process (202) or 400; got ${upload.status} ${upload.bodyText.slice(0, 400)}`,
  ).toBe(true);

  const outcome = await pollGcMassImportSettled(Request, processIdsBefore);
  const blobParts = [upload.bodyText];
  if (outcome) {
    blobParts.push(`${outcome.status} ${outcome.name}`);
    blobParts.push(await collectMassImportErrorBlob(Request, outcome.id));
  }
  const blob = blobParts.join('\n');
  expect(blob, 'process/report must contain Invalid file format').toContain(INVALID_FILE_FORMAT);
  const found = await findCompensationByNumber(Request, number, {
    maxAttempts: 3,
    delayMs: 1000,
  });
  expect(found, `no compensation ${number} after invalid format process`).toBeNull();
  return { path: 'process-error', blob };
}

export async function resolveMassImportIdentifiers(
  Request: Pdt3341Fx['Request'],
  pre: Pdt3341CustomerPodPrechain,
): Promise<{
  customerIdentifier: string;
  podIdentifier: string;
  recipientIdentifier: string;
  currencyName: string;
}> {
  const customerIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.customerId);
  const podIdentifier = await resolveEntityIdentifier(Request, 'pod', pre.podId);
  const recipientIdentifier = await resolveEntityIdentifier(
    Request,
    'customer',
    pre.recipientCustomerId,
  );
  const currencyName = await resolveCurrencyName(Request);
  return { customerIdentifier, podIdentifier, recipientIdentifier, currencyName };
}
