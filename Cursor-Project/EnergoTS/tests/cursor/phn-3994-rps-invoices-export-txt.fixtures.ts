/**
 * PHN-3994 — Export Text File for RPS Invoices (GET /invoice/export-txt).
 *
 * Wraps RPS billing chain helpers from rps-pod-invoice-due-date.fixtures.ts and adds
 * export call/parse helpers aligned to InvoiceTxtExportService + InvoiceRepository.exportInvoicesForTxt.
 *
 * Environment: dev2 | Swagger: Cursor-Project/config/swagger/dev2/swagger-spec.json
 *
 * Findings (header):
 * - OpenAPI (dev2, refreshed 2026-08-13) requires invoiceDateFrom, invoiceDateTo, fileName.
 * - UI generateRpsInvoices() does NOT send fileName to the API (billing-run.service.ts).
 * - UI default file name is HOME_BP${yyyy}${mm}TXT (missing '.' before TXT); expected PO name is HOME_BPyyyymm.TXT.
 */

import { expect, test } from './cursor-test.fixtures';
import type { baseFixture } from './cursor-test.fixtures';
import {
  type RpsDueDateFx,
  asBillingRunId,
  createRpsNomenclature,
  createSimpleRpsChain,
  createTerm,
  createElectricityPriceComponent,
  createProduct,
  createPod,
  createProductContract,
  activatePod,
  createBillingProfile,
  createBillingRun,
  listInvoices,
  pollInvoiceListing,
  runBaseInvoiceForManualNotes,
  startAndCompleteBillingRun,
  startBillingRunToDraft,
  patchStartBilling,
  pollBillingRunForStatus,
  BILLING_RUN_ROOT,
} from './rps-pod-invoice-due-date.fixtures';
import {
  createManualInterimBillingRun,
  type Pdt2872Fx,
} from './pdt-2872-minimal-interim-payment.fixtures';
import {
  setupPdt2960ReceivablesBase,
  createPdt2960Goods,
  createPdt2960GoodsOrder,
  runPdt2960ProformaAccountingChain,
  type Pdt2960Fx,
} from './pdt-2960-proforma-liability-due-date.fixtures';
import envVariables from '../../fixtures/envVariables.json';

export const PHN_3994_KEY = 'PHN-3994';
export const PHN_3994_TITLE = 'Export Text File for RPS Invoices';
export const EXPORT_TXT_PATH = 'invoice/export-txt';

/** Distinct invoice date to reduce collision with other env invoices in the same window. */
export const EXPORT_INVOICE_DATE = '2030-03-15';

export type Phn3994Fx = RpsDueDateFx;

export type ParsedExportTxt = {
  rawText: string;
  hasUtf8Bom: boolean;
  dataRows: string[];
  eofLine: string | null;
  eofRows: number | null;
  eofPrincipal: string | null;
};

export type EligibleInvoiceSetup = {
  rpsId: number;
  billingRunId: number;
  invoiceId: number;
  invoiceNumber: string;
  invoiceDate: string;
  principalAmount: string | null;
};

export function buildHomeBpFileName(now: Date = new Date()): string {
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, '0');
  return `HOME_BP${yyyy}${mm}.TXT`;
}

export function uniqueExportFileName(suffix: string): string {
  const stamp = Date.now().toString(36);
  return `HOME_BP_PHN3994_${suffix}_${stamp}.TXT`;
}

export function buildExportTxtUrl(opts: {
  invoiceDateFrom: string;
  invoiceDateTo: string;
  fileName?: string;
}): string {
  const params = new URLSearchParams();
  params.set('invoiceDateFrom', opts.invoiceDateFrom);
  params.set('invoiceDateTo', opts.invoiceDateTo);
  if (opts.fileName !== undefined) {
    params.set('fileName', opts.fileName);
  }
  return `${EXPORT_TXT_PATH}?${params.toString()}`;
}

export async function getExportTxt(
  Request: Phn3994Fx['Request'],
  opts: { invoiceDateFrom: string; invoiceDateTo: string; fileName?: string },
) {
  return Request.get(buildExportTxtUrl(opts));
}

/** Successful export: 2xx + binary body. Does not use CheckResponse JSON failure path after body read. */
export async function getExportTxtOk(
  Request: Phn3994Fx['Request'],
  opts: { invoiceDateFrom: string; invoiceDateTo: string; fileName: string },
): Promise<{ status: number; headers: Record<string, string>; bytes: Buffer; parsed: ParsedExportTxt }> {
  const res = await getExportTxt(Request, opts);
  await expect(res).CheckResponse();
  const status = res.status();
  const headers = res.headers();
  const bytes = Buffer.from(await res.body());
  const parsed = parseExportTxt(bytes);
  return { status, headers, bytes, parsed };
}

export function parseExportTxt(bytes: Buffer): ParsedExportTxt {
  const hasUtf8Bom =
    bytes.length >= 3 && bytes[0] === 0xef && bytes[1] === 0xbb && bytes[2] === 0xbf;
  const textBytes = hasUtf8Bom ? bytes.subarray(3) : bytes;
  const rawText = textBytes.toString('utf8');
  const lines = rawText
    .split(/\r?\n/)
    .map((l) => l.trimEnd())
    .filter((l, idx, arr) => !(idx === arr.length - 1 && l === ''));

  const eofLine = [...lines].reverse().find((l) => l.startsWith('EOF:')) ?? null;
  const dataRows = lines.filter((l) => l.length > 0 && !l.startsWith('EOF:'));

  let eofRows: number | null = null;
  let eofPrincipal: string | null = null;
  if (eofLine) {
    const parts = eofLine.split(':');
    // EOF:yyyyMMddHHmmss:rows:principal
    if (parts.length >= 4) {
      eofRows = Number(parts[2]);
      eofPrincipal = parts.slice(3).join(':');
    }
  }

  return { rawText, hasUtf8Bom, dataRows, eofLine, eofRows, eofPrincipal };
}

export function findDataRowByDocumentNumber(
  parsed: ParsedExportTxt,
  documentNumber: string,
): string | undefined {
  return parsed.dataRows.find((row) => {
    const cols = row.split('|');
    return cols[6] === documentNumber || row.includes(documentNumber);
  });
}

export function assertFourteenColumns(row: string, label: string): string[] {
  const cols = row.split('|');
  expect(cols.length, `${label}: expected 14 pipe columns, got ${cols.length}`).toBe(14);
  return cols;
}

export function assertCurrencyEurAndEmptyInterestOther(cols: string[], label: string): void {
  expect(cols[9], `${label}: Currency`).toBe('EUR');
  expect(cols[11], `${label}: Interest amount empty`).toBe('');
  expect(cols[12], `${label}: Other amount empty`).toBe('');
}

export function assertEofPattern(eofLine: string | null, label: string): void {
  expect(eofLine, `${label}: EOF line present`).toBeTruthy();
  expect(
    eofLine!,
    `${label}: EOF pattern`,
  ).toMatch(/^EOF:\d{14}:\d+:.+$/);
}

export async function createEligibleRealRpsInvoice(
  fx: Phn3994Fx,
  opts?: { invoiceDate?: string; rpsLabel?: string },
): Promise<EligibleInvoiceSetup> {
  const invoiceDate = opts?.invoiceDate ?? EXPORT_INVOICE_DATE;
  const rpsLabel = opts?.rpsLabel ?? `RPS-3994-${Date.now().toString(36)}`;

  let rpsId = 0;
  await test.step('Precondition: RPS nomenclature', async () => {
    rpsId = await createRpsNomenclature(fx.Request, { rpsNumber: rpsLabel, dayOfMonth: 15 });
  });

  const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
    applicationModelType: ['FOR_VOLUMES'],
    invoiceDate,
    invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
  });

  await test.step('Precondition: complete STANDARD billing run', async () => {
    await startAndCompleteBillingRun(fx.Request, billingRunId);
  });

  const rows = await pollInvoiceListing(fx.Request, billingRunId, 1);
  expect(rows.length, 'eligible REAL invoice must exist').toBeGreaterThan(0);
  const inv = rows[0]!;
  const invoiceId = Number(inv.id);
  const invoiceNumber = String(inv.invoiceNumber ?? '');
  expect(invoiceId, 'invoice id').toBeGreaterThan(0);
  expect(invoiceNumber, 'invoiceNumber').toBeTruthy();

  if (!fx.Responses.invoice.includes(invoiceId)) {
    fx.Responses.invoice.push(invoiceId);
  }

  let principalAmount: string | null = null;
  const detailRes = await fx.Request.get(`invoice?id=${invoiceId}`);
  await expect(detailRes).CheckResponse();
  const detail = (await detailRes.json()) as {
    invoiceStatus?: string;
    invoiceDate?: string;
    totalAmountIncludingVat?: number | string;
  };
  expect(detail.invoiceStatus, 'invoice status REAL').toBe('REAL');
  principalAmount =
    detail.totalAmountIncludingVat != null ? String(detail.totalAmountIncludingVat) : null;

  return {
    rpsId,
    billingRunId,
    invoiceId,
    invoiceNumber,
    invoiceDate: String(detail.invoiceDate ?? invoiceDate).slice(0, 10),
    principalAmount,
  };
}

export async function createRealInvoiceWithoutRps(
  fx: Phn3994Fx,
  invoiceDate: string = EXPORT_INVOICE_DATE,
): Promise<EligibleInvoiceSetup> {
  const { billingRunId } = await createSimpleRpsChain(fx, null, {
    applicationModelType: ['FOR_VOLUMES'],
    invoiceDate,
    invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
  });

  await test.step('Precondition: complete billing (non-RPS POD)', async () => {
    await startAndCompleteBillingRun(fx.Request, billingRunId);
  });

  const rows = await pollInvoiceListing(fx.Request, billingRunId, 1);
  expect(rows.length, 'non-RPS invoice must exist').toBeGreaterThan(0);
  const inv = rows[0]!;
  const invoiceId = Number(inv.id);
  const invoiceNumber = String(inv.invoiceNumber ?? '');
  if (!fx.Responses.invoice.includes(invoiceId)) {
    fx.Responses.invoice.push(invoiceId);
  }

  return {
    rpsId: 0,
    billingRunId,
    invoiceId,
    invoiceNumber,
    invoiceDate,
    principalAmount: null,
  };
}

export async function createDraftInvoiceOnRpsPod(
  fx: Phn3994Fx,
  invoiceDate: string = EXPORT_INVOICE_DATE,
): Promise<{ billingRunId: number; invoiceNumber: string; invoiceDate: string }> {
  const rpsId = await createRpsNomenclature(fx.Request, {
    rpsNumber: `RPS-3994-DRAFT-${Date.now().toString(36)}`,
    dayOfMonth: 15,
  });

  const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
    applicationModelType: ['FOR_VOLUMES'],
    invoiceDate,
    invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
  });

  await test.step('Precondition: start-billing only (leave DRAFT invoices)', async () => {
    const startRes = await patchStartBilling(fx.Request, billingRunId);
    await expect(startRes).CheckResponse();
    await pollBillingRunForStatus(fx.Request, billingRunId, 'DRAFT');
  });

  const rows = await listInvoices(fx.Request, billingRunId);
  const draft =
    rows.find((r) => {
      const st = String(r.invoiceStatus ?? r.status ?? '').toUpperCase();
      return st === 'DRAFT' || st === 'IN_PROGRESS_DRAFT';
    }) ?? rows[0];
  expect(draft, 'DRAFT invoice must exist after start-billing').toBeTruthy();
  const invoiceNumber = String(draft!.invoiceNumber ?? '');
  expect(invoiceNumber, 'draft invoiceNumber').toBeTruthy();

  return { billingRunId, invoiceNumber, invoiceDate };
}

export async function cancelInvoiceByNumber(
  fx: Phn3994Fx,
  invoiceNumber: string,
  taxEventDate: string,
): Promise<void> {
  const payload = {
    invoices: invoiceNumber,
    fileId: null,
    taxEventDate,
    templateId: Number((envVariables as Record<string, unknown>).invoice_cancellation_template),
  };
  const res = await fx.Request.post(fx.Endpoints.invoiceCancellation, { data: payload });
  await expect(res).CheckResponse();
}

export async function createEligibleDebitNote(
  fx: Phn3994Fx,
  invoiceDate: string = EXPORT_INVOICE_DATE,
): Promise<{ debitNoteNumber: string; debitNoteBillingRunId: number; invoiceDate: string }> {
  const rpsId = await createRpsNomenclature(fx.Request, {
    rpsNumber: `RPS-3994-DN-${Date.now().toString(36)}`,
    dayOfMonth: 15,
  });

  const { baseInvoiceId } = await runBaseInvoiceForManualNotes(fx, rpsId, invoiceDate);

  const invRes = await fx.Request.get(`invoice?id=${baseInvoiceId}`);
  await expect(invRes).CheckResponse();
  const invBody = (await invRes.json()) as { invoiceNumber?: string };
  const baseInvoiceNumber = String(invBody.invoiceNumber ?? '');
  expect(baseInvoiceNumber, 'parent invoice number for debit note').toBeTruthy();

  let debitNoteBillingRunId = 0;
  await test.step('Precondition: MANUAL_CREDIT_OR_DEBIT_NOTE (DEBIT_NOTE)', async () => {
    const payload = fx.GeneratePayload.billing.debitNote() as Record<string, unknown>;
    const cp = (payload.commonParameters ?? {}) as Record<string, unknown>;
    cp.invoiceDate = invoiceDate;
    cp.invoiceDueDate = 'ACCORDING_TO_THE_CONTRACT';
    cp.dueDate = null;
    payload.commonParameters = cp;

    const noteBasicParams = (payload as any).manualCreditOrDebitNoteParameters
      .manualCreditOrDebitNoteBasicDataParameters;
    noteBasicParams.billingRunInvoiceInformationList = [
      { invoiceId: baseInvoiceId, invoiceNumber: baseInvoiceNumber },
    ];

    const res = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
    await expect(res).CheckResponse();
    debitNoteBillingRunId = asBillingRunId(await res.json());
    fx.Responses.billingRun.push(debitNoteBillingRunId);
  });

  await test.step('Complete debit note billing run', async () => {
    await startAndCompleteBillingRun(fx.Request, debitNoteBillingRunId);
  });

  const rows = await pollInvoiceListing(fx.Request, debitNoteBillingRunId, 1);
  expect(rows.length, 'debit note invoice row').toBeGreaterThan(0);
  const debitNoteNumber = String(rows[0]!.invoiceNumber ?? '');
  expect(debitNoteNumber, 'debit note document number').toBeTruthy();

  return { debitNoteNumber, debitNoteBillingRunId, invoiceDate };
}

export async function createDraftGeneratedInvoiceOnRpsPod(
  fx: Phn3994Fx,
  invoiceDate: string = EXPORT_INVOICE_DATE,
): Promise<{ billingRunId: number; invoiceNumber: string; invoiceDate: string }> {
  const rpsId = await createRpsNomenclature(fx.Request, {
    rpsNumber: `RPS-3994-DG-${Date.now().toString(36)}`,
    dayOfMonth: 15,
  });

  const { billingRunId } = await createSimpleRpsChain(fx, rpsId, {
    applicationModelType: ['FOR_VOLUMES'],
    invoiceDate,
    invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
  });

  await test.step('Precondition: start-billing + start-generating (leave DRAFT_GENERATED)', async () => {
    await startBillingRunToDraft(fx.Request, billingRunId);
  });

  const rows = await listInvoices(fx.Request, billingRunId);
  const dg =
    rows.find((r) => {
      const st = String(r.invoiceStatus ?? r.status ?? '').toUpperCase();
      return st === 'DRAFT_GENERATED';
    }) ?? rows[0];
  expect(dg, 'DRAFT_GENERATED invoice must exist after start-generating').toBeTruthy();
  const invoiceNumber = String(dg!.invoiceNumber ?? '');
  expect(invoiceNumber, 'draft_generated invoiceNumber').toBeTruthy();

  return { billingRunId, invoiceNumber, invoiceDate };
}

export type PrivateCustomerNames = {
  firstName: string;
  middleName: string;
  lastName: string;
};

export type PrivateEligibleInvoiceSetup = EligibleInvoiceSetup & {
  names: PrivateCustomerNames;
};

export function unwrapReceiptColumn(receipt: string): string {
  return receipt.split('\\n').join('');
}

export async function createPrivateCustomer(fx: Phn3994Fx): Promise<PrivateCustomerNames> {
  const payload = fx.GeneratePayload.customers.customer_private() as {
    privateCustomerDetails: { firstName: string; middleName: string | null; lastName: string };
  };
  const details = payload.privateCustomerDetails;
  const res = await fx.Request.post(fx.Endpoints.customer, { data: payload });
  await expect(res).CheckResponse();
  fx.Responses.customer.push(await res.json());
  return {
    firstName: details.firstName,
    middleName: details.middleName ?? '',
    lastName: details.lastName,
  };
}

/** Eligible REAL STANDARD INVOICE on an RPS POD for a PRIVATE_CUSTOMER (Cases 30 / 49). */
export async function createEligibleRealRpsInvoicePrivateCustomer(
  fx: Phn3994Fx,
  opts?: { invoiceDate?: string; rpsLabel?: string },
): Promise<PrivateEligibleInvoiceSetup> {
  const invoiceDate = opts?.invoiceDate ?? EXPORT_INVOICE_DATE;
  const rpsLabel = opts?.rpsLabel ?? `RPS-3994-PRIV-${Date.now().toString(36)}`;

  let rpsId = 0;
  await test.step('Precondition: RPS nomenclature', async () => {
    rpsId = await createRpsNomenclature(fx.Request, { rpsNumber: rpsLabel, dayOfMonth: 15 });
  });

  let names: PrivateCustomerNames = { firstName: '', middleName: '', lastName: '' };
  await test.step('Precondition: term', async () => createTerm(fx));
  await test.step('Precondition: price component', async () => createElectricityPriceComponent(fx));
  await test.step('Precondition: product', async () => createProduct(fx));
  await test.step('Precondition: private customer', async () => {
    names = await createPrivateCustomer(fx);
  });
  await test.step('Precondition: POD', async () => createPod(fx, rpsId));
  await test.step('Precondition: product contract', async () => createProductContract(fx));
  await test.step('Precondition: activate POD', async () => activatePod(fx));
  await test.step('Precondition: billing profile', async () => createBillingProfile(fx));

  let billingRunId = 0;
  await test.step('Precondition: billing run', async () => {
    billingRunId = await createBillingRun(fx, {
      applicationModelType: ['FOR_VOLUMES'],
      invoiceDate,
      invoiceDueDateType: 'ACCORDING_TO_THE_CONTRACT',
    });
  });

  await test.step('Precondition: complete STANDARD billing run', async () => {
    await startAndCompleteBillingRun(fx.Request, billingRunId);
  });

  const rows = await pollInvoiceListing(fx.Request, billingRunId, 1);
  expect(rows.length, 'eligible REAL private-customer invoice must exist').toBeGreaterThan(0);
  const inv = rows[0]!;
  const invoiceId = Number(inv.id);
  const invoiceNumber = String(inv.invoiceNumber ?? '');
  expect(invoiceId, 'invoice id').toBeGreaterThan(0);
  expect(invoiceNumber, 'invoiceNumber').toBeTruthy();
  if (!fx.Responses.invoice.includes(invoiceId)) {
    fx.Responses.invoice.push(invoiceId);
  }

  const detailRes = await fx.Request.get(`invoice?id=${invoiceId}`);
  await expect(detailRes).CheckResponse();
  const detail = (await detailRes.json()) as { invoiceStatus?: string; invoiceDate?: string };
  expect(detail.invoiceStatus, 'invoice status REAL').toBe('REAL');

  return {
    rpsId,
    billingRunId,
    invoiceId,
    invoiceNumber,
    invoiceDate: String(detail.invoiceDate ?? invoiceDate).slice(0, 10),
    principalAmount: null,
    names,
  };
}

/** Case 16: REAL INTERIM_AND_ADVANCE_PAYMENT on an RPS contract (type skip list). */
export async function createRealInterimInvoiceOnRpsPod(
  fx: Phn3994Fx,
  invoiceDate: string = EXPORT_INVOICE_DATE,
): Promise<{ invoiceId: number; invoiceNumber: string; invoiceDate: string; invoiceType: string }> {
  await createEligibleRealRpsInvoice(fx, {
    invoiceDate,
    rpsLabel: `RPS-3994-INT-${Date.now().toString(36)}`,
  });

  let interimBillingRunId = 0;
  await test.step('Precondition: MANUAL_INTERIM_AND_ADVANCE_PAYMENT billing run', async () => {
    interimBillingRunId = await createManualInterimBillingRun(fx as Pdt2872Fx, 100);
  });

  await test.step('Complete interim billing run', async () => {
    await startAndCompleteBillingRun(fx.Request, interimBillingRunId);
  });

  const rows = await pollInvoiceListing(fx.Request, interimBillingRunId, 1);
  expect(rows.length, 'interim invoice row').toBeGreaterThan(0);
  const invoiceId = Number(rows[0]!.id);
  const invoiceNumber = String(rows[0]!.invoiceNumber ?? '');
  expect(invoiceId, 'interim invoice id').toBeGreaterThan(0);
  if (!fx.Responses.invoice.includes(invoiceId)) {
    fx.Responses.invoice.push(invoiceId);
  }

  const detailRes = await fx.Request.get(`invoice?id=${invoiceId}`);
  await expect(detailRes).CheckResponse();
  const detail = (await detailRes.json()) as {
    invoiceStatus?: string;
    invoiceDate?: string;
    invoiceType?: string;
    type?: string;
  };
  const invoiceType = String(detail.invoiceType ?? detail.type ?? '');
  expect(invoiceType.toUpperCase(), 'interim invoice type').toContain('INTERIM');

  return {
    invoiceId,
    invoiceNumber,
    invoiceDate: String(detail.invoiceDate ?? invoiceDate).slice(0, 10),
    invoiceType,
  };
}

/** Case 21: PROFORMA_INVOICE (document-type skip list). */
export async function createProformaInvoice(
  fx: Phn3994Fx,
): Promise<{ invoiceId: number; invoiceNumber: string; invoiceDate: string; documentType: string }> {
  const pdtFx = fx as unknown as Pdt2960Fx;

  await test.step('Precondition: receivables base + goods + goods order', async () => {
    await setupPdt2960ReceivablesBase(pdtFx);
    await createPdt2960Goods(pdtFx);
  });

  let goodsOrderId = 0;
  await test.step('Precondition: goods order', async () => {
    goodsOrderId = await createPdt2960GoodsOrder(pdtFx);
  });

  let invoiceId = 0;
  await test.step('Precondition: issue-proforma-invoice + generating + accounting', async () => {
    invoiceId = await runPdt2960ProformaAccountingChain(pdtFx, goodsOrderId);
  });

  const detailRes = await fx.Request.get(`invoice?id=${invoiceId}`);
  await expect(detailRes).CheckResponse();
  const detail = (await detailRes.json()) as {
    invoiceNumber?: string;
    invoiceDate?: string;
    documentType?: string;
  };
  const invoiceNumber = String(detail.invoiceNumber ?? '');
  expect(invoiceNumber, 'proforma invoiceNumber').toBeTruthy();

  return {
    invoiceId,
    invoiceNumber,
    invoiceDate: String(detail.invoiceDate ?? '').slice(0, 10),
    documentType: String(detail.documentType ?? ''),
  };
}

export { createRpsNomenclature, startBillingRunToDraft, BILLING_RUN_ROOT, asBillingRunId };
