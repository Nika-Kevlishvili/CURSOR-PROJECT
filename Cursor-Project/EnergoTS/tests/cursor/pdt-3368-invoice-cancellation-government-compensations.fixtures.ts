/**
 * PDT-3368 — Invoice cancellation unwind of invoiced government compensations.
 *
 * Reference spec(s):
 * - tests/cursor/GC-REG-government-compensation-process.spec.ts
 * - tests/cursor/gc-reg-government-compensation-regression.fixtures.ts
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 * - tests/cursor/PHN-3992-invoice-cancel-closed-accounting-period.spec.ts / .fixtures.ts
 * - tests/receivableManagement/Rescheduling/happyPass.spec.ts
 *
 * Swagger: refresh via update-swagger-specs.ps1 this session (dev).
 * CreateInvoiceCancellationRequest: required templateId; invoices, fileId, taxEventDate.
 * InvoiceCancellationResponse: invoiceCancellationId, processId.
 * PATCH /invoice/regenerate-compensations?id=
 * GET /process/{id}, GET /process/{id}/report/download?multiSheetExcelType=MASS_IMPORT_ERROR_REPORT
 *   (Swagger MultiSheetExcelType: MASS_IMPORT_ERROR_REPORT | BILLING_RUN_ERROR_REPORT —
 *    invoice cancellation stores per-invoice failures in ProcessedRecordInfo; MASS_IMPORT_ERROR_REPORT
 *    exports success=false rows — InvoiceCancellationProcessService catch → errorMessage)
 * GET /customer-liability/{id}, GET /customer-receivable/{id}
 *
 * Backend TC: Cursor-Project/test_cases/Backend/Invoice_Cancellation_Government_Compensations.md
 */

import * as ExcelJS from 'exceljs';
import { expect, test } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  createGcExpectingCreated,
  completeForVolumesToAccounted,
  getCompensation,
  getInvoice,
  getCustomerLiability,
  getCustomerReceivable,
  patchRegenerateCompensationsRaw,
  runDevVolCompContractAndBbpPrechain,
  shortId,
  toAmountNumber,
  assertGcUninvoicedEmptyFinancials,
  assertPositiveApplyAttachments,
  assertNegativeApplyAttachments,
  resolveInvoiceMainCustomerLiability,
  extractPartyCustomerId,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  type GcRegFx,
  type CompensationView,
  type DevVolCompPrechainResult,
} from './gc-reg-government-compensation-regression.fixtures';
import { runMultiPodForVolumesPrechain } from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import { postPdt2915BillingByProfile } from './pdt-2915-invoice-correction-deleted-bbp.fixtures';
import {
  cancellationInvoiceToken,
  extractApiErrorMessage,
  listAccountingPeriodsByStatus,
  pickTaxEventDateInPeriod,
  postInvoiceCancellation,
  waitForInvoiceStatus,
} from './phn-3992-invoice-cancel-closed-accounting-period.fixtures';
import { buildProcessPreviewLink } from './shared/manual-verification-links.fixtures';

export const PDT_3368_KEY = 'PDT-3368';
export const PDT_3368_TITLE = 'Invoice Cancellation Process - Government Compensations';

export const UNIQUE_RESULT_FRAGMENT = 'Query did not return a unique result';
export const RESCHEDULING_BLOCK_FRAGMENT =
  'Invoice can not be cancelled because connected liability is used for rescheduling';
export const REGEN_NOT_REAL_FRAGMENT =
  "You can't regenerate compensations for invoice, it must be with status 'REAL'";
export const ALREADY_CANCELLED_FRAGMENT = 'not found or is reversed';

export const BILLING_TIMEOUT_MS = 45 * 60 * 1000;
export const CANCEL_POLL_MS = 15 * 60 * 1000;

export type Pdt3368Fx = GcRegFx;

export type GcLinkSnapshot = {
  gcId: number;
  liabilityForRecipientId: number | null;
  receivableForCustomerId: number | null;
  liabilityForCustomerId: number | null;
  receivableForRecipientId: number | null;
};

export type RealDebitWithGc = {
  pre: DevVolCompPrechainResult;
  invoiceId: number;
  invoiceNumber: string;
  billingRunId: number;
  gcs: Array<{ id: number; payload: Record<string, unknown>; links: GcLinkSnapshot; view: CompensationView }>;
};

export {
  createGcExpectingCreated,
  completeForVolumesToAccounted,
  getCompensation,
  getInvoice,
  getCustomerLiability,
  getCustomerReceivable,
  patchRegenerateCompensationsRaw,
  runDevVolCompContractAndBbpPrechain,
  runMultiPodForVolumesPrechain,
  shortId,
  toAmountNumber,
  assertGcUninvoicedEmptyFinancials,
  assertPositiveApplyAttachments,
  assertNegativeApplyAttachments,
  resolveInvoiceMainCustomerLiability,
  extractPartyCustomerId,
  extractApiErrorMessage,
};

export function snapshotGcLinks(gcId: number, view: CompensationView): GcLinkSnapshot {
  return {
    gcId,
    liabilityForRecipientId: shortId(view, 'liabilityForRecipient'),
    receivableForCustomerId: shortId(view, 'receivableForCustomer'),
    liabilityForCustomerId: shortId(view, 'liabilityForCustomer'),
    receivableForRecipientId: shortId(view, 'receivableForRecipient'),
  };
}

export async function resolveOpenTaxEventDate(Request: Pdt3368Fx['Request']): Promise<string> {
  const open = await listAccountingPeriodsByStatus(Request, 'OPEN', 50);
  expect(open.length, 'at least one OPEN accounting period').toBeGreaterThan(0);
  return pickTaxEventDateInPeriod(open[0]!);
}

export async function postCancelByInvoiceNumber(
  Request: Pdt3368Fx['Request'],
  invoiceNumber: string,
  taxEventDate: string,
): Promise<{ status: number; processId: number; cancellationId: number; body: Record<string, unknown>; rawText: string }> {
  const invoices = cancellationInvoiceToken(invoiceNumber);
  const result = await postInvoiceCancellation(Request, {
    invoices,
    taxEventDate,
    fileId: null,
    templateId: Number(envVariables.invoice_cancellation_template),
  });
  const processId = Number(result.body.processId ?? 0);
  const cancellationId = Number(result.body.invoiceCancellationId ?? result.body.id ?? 0);
  return {
    status: result.status,
    processId,
    cancellationId,
    body: result.body,
    rawText: result.rawText,
  };
}

export async function waitInvoiceCancelled(
  Request: Pdt3368Fx['Request'],
  invoiceId: number,
): Promise<string> {
  return waitForInvoiceStatusWithTimeout(Request, invoiceId, 'CANCELLED', CANCEL_POLL_MS);
}

export async function waitForInvoiceStatusWithTimeout(
  Request: Pdt3368Fx['Request'],
  invoiceId: number,
  status: string,
  timeoutMs: number,
): Promise<string> {
  await expect
    .poll(
      async () => {
        const get = await Request.get(`invoice?id=${invoiceId}`);
        await expect(get).CheckResponse();
        const body = (await get.json()) as { invoiceStatus?: string };
        return body.invoiceStatus;
      },
      {
        message: `Invoice ${invoiceId} status is not ${status}`,
        timeout: timeoutMs,
        intervals: [2_000, 5_000, 10_000],
      },
    )
    .toBe(status);
  const finalRes = await Request.get(`invoice?id=${invoiceId}`);
  await expect(finalRes).CheckResponse();
  return String(((await finalRes.json()) as { invoiceStatus?: string }).invoiceStatus ?? '');
}

export async function getProcessBody(
  Request: Pdt3368Fx['Request'],
  processId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`process/${processId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

/** @deprecated Prefer {@link downloadProcessReportText} for cancellation error assertions. */
export function processErrorText(processBody: Record<string, unknown>): string {
  return JSON.stringify(processBody);
}

function cellText(value: ExcelJS.CellValue | null | undefined): string {
  if (value == null || value === '') {
    return '';
  }
  if (typeof value === 'object' && 'text' in value && value.text != null) {
    return String(value.text).trim();
  }
  if (typeof value === 'object' && 'result' in value && value.result != null) {
    return String(value.result).trim();
  }
  return String(value).trim();
}

/**
 * Download the process error report file (xlsx) and return concatenated sheet text.
 *
 * Invoice cancellation (`PROCESS_INVOICE_CANCELLATION`) writes failed invoices to
 * `ProcessedRecordInfo` (`success=false`, `errorMessage`). Swagger exposes those via
 * `GET /process/{id}/report/download?multiSheetExcelType=MASS_IMPORT_ERROR_REPORT`
 * (same report type as mass-import failures — headers: row_number | identifier | identifier_version | errors).
 *
 * Fails hard if the download is not OK, the buffer is empty, or no worksheet is present.
 */
export async function downloadProcessReportText(
  Request: Pdt3368Fx['Request'],
  processId: number,
): Promise<string> {
  const reportRes = await Request.get(`process/${processId}/report/download`, {
    params: { multiSheetExcelType: 'MASS_IMPORT_ERROR_REPORT' },
  });
  await expect(
    reportRes,
    `GET process/${processId}/report/download?multiSheetExcelType=MASS_IMPORT_ERROR_REPORT`,
  ).CheckResponse();
  const buf = await reportRes.body();
  expect(
    buf.byteLength,
    `process ${processId} MASS_IMPORT_ERROR_REPORT buffer must not be empty`,
  ).toBeGreaterThan(0);

  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buf);
  expect(
    workbook.worksheets.length,
    `process ${processId} MASS_IMPORT_ERROR_REPORT must contain a worksheet`,
  ).toBeGreaterThan(0);

  const parts: string[] = [];
  for (const worksheet of workbook.worksheets) {
    worksheet.eachRow((row) => {
      const cells: string[] = [];
      row.eachCell({ includeEmpty: true }, (cell) => {
        cells.push(cellText(cell.value));
      });
      parts.push(cells.join(' | '));
    });
  }
  const text = parts.join('\n');
  expect(
    text.trim().length,
    `process ${processId} MASS_IMPORT_ERROR_REPORT text must not be empty`,
  ).toBeGreaterThan(0);
  return text;
}

export async function waitProcessFinished(
  Request: Pdt3368Fx['Request'],
  processId: number,
): Promise<Record<string, unknown>> {
  await expect
    .poll(
      async () => {
        const body = await getProcessBody(Request, processId);
        return String(body.status ?? '');
      },
      {
        message: `Process ${processId} did not finish`,
        timeout: CANCEL_POLL_MS,
        intervals: [2_000, 5_000, 10_000],
      },
    )
    .toMatch(/COMPLETED|CANCELED|CANCELLED|FAILED|ERROR|FINISHED/);
  return getProcessBody(Request, processId);
}

export async function assertAmountZero(
  Request: Pdt3368Fx['Request'],
  kind: 'liability' | 'receivable',
  id: number | null,
  label: string,
): Promise<void> {
  expect(id, `${label} id`).toBeTruthy();
  if (!id) return;
  if (kind === 'liability') {
    const view = await getCustomerLiability(Request, id);
    expect(toAmountNumber(view.currentAmount), `${label} currentAmount`).toBe(0);
  } else {
    const view = await getCustomerReceivable(Request, id);
    expect(toAmountNumber(view.currentAmount as number), `${label} currentAmount`).toBe(0);
  }
}

export async function accountGcsOnRealDebit(
  ctx: Pdt3368Fx,
  pre: DevVolCompPrechainResult,
  amounts: Array<{ documentAmount: number; podId: number; reason: string; documentPeriod?: string }>,
): Promise<RealDebitWithGc> {
  const created: RealDebitWithGc['gcs'] = [];
  for (const row of amounts) {
    const gc = await createGcExpectingCreated(ctx, {
      customerId: pre.customerId,
      podId: row.podId,
      recipientId: pre.recipientCustomerId,
      documentPeriod: row.documentPeriod ?? pre.documentPeriod,
      documentAmount: row.documentAmount,
      volumes: 1,
      price: Math.abs(row.documentAmount),
      reason: row.reason,
    });
    created.push({
      id: gc.id,
      payload: gc.payload as unknown as Record<string, unknown>,
      links: snapshotGcLinks(gc.id, await getCompensation(ctx.Request, gc.id)),
      view: await getCompensation(ctx.Request, gc.id),
    });
  }

  const billed = await completeForVolumesToAccounted(ctx);
  const invoice = await getInvoice(ctx.Request, billed.invoiceId);
  expect(invoice.invoiceStatus, 'invoice REAL after accounting').toBe('REAL');
  expect(String(invoice.invoiceNumber ?? '').trim(), 'invoiceNumber').toBeTruthy();
  expect(toAmountNumber(invoice.totalAmountIncludingVat), 'invoice total > 0').toBeGreaterThan(0);

  const gcs: RealDebitWithGc['gcs'] = [];
  for (const row of created) {
    const view = await getCompensation(ctx.Request, row.id);
    expect(view.invoice?.id, `GC ${row.id} invoiced on billed invoice`).toBe(billed.invoiceId);
    gcs.push({ ...row, view, links: snapshotGcLinks(row.id, view) });
  }

  return {
    pre,
    invoiceId: billed.invoiceId,
    invoiceNumber: String(invoice.invoiceNumber),
    billingRunId: billed.billingRunId,
    gcs,
  };
}

export async function addBillingByProfileShiftedMonth(
  ctx: Pdt3368Fx,
  podIndex: number,
  monthsDelta: number,
): Promise<{ periodFrom: string; periodTo: string; documentPeriod: string }> {
  const payload = await ctx.GeneratePayload.energyData.profile15minute(podIndex);
  payload.timeZone = 'CET';
  payload.profileId = envVariables.profiles;
  // API requires full calendar months: periodFrom = 1st, periodTo = last day of shifted month
  // (plain day-preserving month shift turns Sep 30 into Aug 30 and fails validation).
  const from = shiftIsoMonth(String(payload.periodFrom), monthsDelta, 'start');
  const to = shiftIsoMonth(String(payload.periodTo), monthsDelta, 'end');
  payload.periodFrom = from;
  payload.periodTo = to;
  const bbp = await postPdt2915BillingByProfile(
    ctx.Request,
    payload,
    `PDT-3368 BBP shifted ${monthsDelta}m pod${podIndex}`,
  );
  await ctx.GeneratePayload.energyData.uploadDataByProfileFile(bbp.id, 'MIN');
  ctx.Responses.dataByProfiles.push({
    id: bbp.id,
    periodFrom: from,
    periodTo: to,
    periodType: payload.periodType,
  });
  return {
    periodFrom: from,
    periodTo: to,
    documentPeriod: `${from.slice(0, 8)}01`,
  };
}

/**
 * Shift an ISO date (optional time suffix preserved) by `monthsDelta` months, then snap to
 * the first (`start`) or last (`end`) day of the resulting calendar month.
 */
function shiftIsoMonth(value: string, monthsDelta: number, boundary: 'start' | 'end'): string {
  const day = value.includes('T') ? value.split('T')[0]! : value.slice(0, 10);
  const [y, m] = day.split('-').map(Number);
  const dt = new Date(Date.UTC(y!, (m ?? 1) - 1, 1));
  dt.setUTCMonth(dt.getUTCMonth() + monthsDelta);
  const yy = dt.getUTCFullYear();
  const mmNum = dt.getUTCMonth() + 1;
  const mm = String(mmNum).padStart(2, '0');
  const ddNum =
    boundary === 'start' ? 1 : new Date(Date.UTC(yy, mmNum, 0)).getUTCDate();
  const dd = String(ddNum).padStart(2, '0');
  if (value.includes('T')) {
    return `${yy}-${mm}-${dd}${value.slice(10)}`;
  }
  return `${yy}-${mm}-${dd}`;
}

export async function createExecutedReschedulingForLiability(
  ctx: Pdt3368Fx,
  opts: { customerId: number; liabilityId: number },
): Promise<number> {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;

  await test.step('Precondition: yearly interest rate', async () => {
    const payload = GeneratePayload.receivablesManagement.dailyInterestRate();
    payload.type = 'YEARLY';
    const post = await Request.post(Endpoints.interestRate, { data: payload });
    await expect(post).CheckResponse();
    Responses.interestRate.push(await post.json());
  });

  const liabilityBefore = await getCustomerLiability(Request, opts.liabilityId);
  const liabilityCustomerId = extractPartyCustomerId(liabilityBefore as Record<string, unknown>);
  expect(
    liabilityCustomerId,
    `liability ${opts.liabilityId} customer must match rescheduling customer ${opts.customerId}`,
  ).toBe(opts.customerId);
  expect(toAmountNumber(liabilityBefore.currentAmount), 'liability currentAmount > 0 before rescheduling').toBeGreaterThan(
    0,
  );

  const customerGet = await Request.get(`customer/${opts.customerId}`);
  await expect(customerGet).CheckResponse();
  const customerBody = (await customerGet.json()) as {
    customerId?: number;
    lastCustomerDetailId?: number;
    communicationData?: Array<{ id?: number }>;
  };
  const communicationDataId = Number(customerBody.communicationData?.[0]?.id);
  const customerDetailId = Number(customerBody.lastCustomerDetailId);
  expect(customerDetailId, `customer ${opts.customerId} lastCustomerDetailId`).toBeGreaterThan(0);
  expect(communicationDataId, `customer ${opts.customerId} communicationData[0].id`).toBeGreaterThan(0);

  await test.step('Precondition: customer assessment', async () => {
    const payload = GeneratePayload.receivablesManagement.customer_assessment();
    payload.customerId = opts.customerId;
    const post = await Request.post(Endpoints.customerAssessment, { data: payload });
    await expect(post).CheckResponse();
    Responses.customerAssessment.push(await post.json());
  });

  const calculationPayload = GeneratePayload.receivablesManagement.reschedulingCalculation(
    '3',
    'INTEREST_WITH_THE_FIRST_INSTALLMENT',
  );
  calculationPayload.liabilityIds = [opts.liabilityId] as unknown as typeof calculationPayload.liabilityIds;
  const calculation = await Request.post(`${Endpoints.rescheduling}/calculate-rescheduling`, {
    data: calculationPayload,
  });
  await expect(calculation).CheckResponse();
  const calcBody = (await calculation.json()) as {
    lpfs?: Array<{
      interest_default_currency?: number;
      lpf_data?: { results?: unknown[] };
    }>;
    installments?: unknown[];
  };

  const savedCustomer0 = Responses.customer[0];
  const savedLiabilities = [...Responses.customerLiability];
  Responses.customer[0] = { id: opts.customerId };
  Responses.customerLiability.length = 0;
  Responses.customerLiability.push(opts.liabilityId);

  let reschedulingPayload: Awaited<
    ReturnType<Pdt3368Fx['GeneratePayload']['receivablesManagement']['rescheduling']>
  >;
  try {
    reschedulingPayload = await GeneratePayload.receivablesManagement.rescheduling(
      '3',
      'INTEREST_WITH_THE_FIRST_INSTALLMENT',
    );
  } finally {
    Responses.customer[0] = savedCustomer0;
    Responses.customerLiability.length = 0;
    Responses.customerLiability.push(...savedLiabilities);
  }

  reschedulingPayload.customerId = Number(customerBody.customerId ?? opts.customerId);
  reschedulingPayload.customerDetailId = customerDetailId;
  reschedulingPayload.customerCommunicationDataId = communicationDataId;
  reschedulingPayload.customerCommunicationDataIdForContract = communicationDataId;
  reschedulingPayload.liabilityIdsForRescheduling = [opts.liabilityId];
  reschedulingPayload.saveAndExecute = true;
  if (calcBody.installments) {
    reschedulingPayload.installments = calcBody.installments as typeof reschedulingPayload.installments;
  }
  if (calcBody.lpfs?.[0] && reschedulingPayload.reschedulingLpfs?.[0]) {
    reschedulingPayload.reschedulingLpfs[0].id = opts.liabilityId;
    reschedulingPayload.reschedulingLpfs[0].interest_default_currency =
      calcBody.lpfs[0].interest_default_currency ?? 0;
    reschedulingPayload.reschedulingLpfs[0].lpf_data.results = (calcBody.lpfs[0].lpf_data?.results ??
      []) as typeof reschedulingPayload.reschedulingLpfs[0]['lpf_data']['results'];
  }

  const create = await Request.post(Endpoints.rescheduling, { data: reschedulingPayload });
  await expect(create).CheckResponse();
  const createdRaw = await create.json();
  const reschedulingId =
    typeof createdRaw === 'number'
      ? createdRaw
      : Number((createdRaw as { id?: unknown })?.id ?? createdRaw);
  expect(reschedulingId, 'rescheduling id').toBeGreaterThan(0);
  Responses.rescheduling.push(reschedulingId);

  const get = await Request.get(`${Endpoints.rescheduling}/${reschedulingId}`);
  await expect(get).CheckResponse();
  const view = (await get.json()) as {
    reschedulingStatus?: string;
    status?: string;
    reversed?: boolean;
    reschedulingLiabilityResponses?: Array<{ liabilityId?: number }>;
  };
  expect(view.reschedulingStatus, 'rescheduling EXECUTED').toBe('EXECUTED');
  expect(String(view.status ?? 'ACTIVE').toUpperCase(), 'rescheduling ACTIVE').toBe('ACTIVE');
  expect(Boolean(view.reversed), 'not reversed').toBe(false);
  // Original invoice L does not expose reschedulingShortResponse (only RESCHEDULING installments do).
  // Link is on GET /rescheduling/{id}.reschedulingLiabilityResponses[].liabilityId (happyPass pattern).
  const linkedLiabilityIds = (view.reschedulingLiabilityResponses ?? [])
    .map((row) => Number(row.liabilityId))
    .filter((id) => Number.isFinite(id) && id > 0);
  expect(
    linkedLiabilityIds,
    `GET /rescheduling/${reschedulingId} must list liabilityId=${opts.liabilityId}`,
  ).toContain(opts.liabilityId);
  return Number(reschedulingId);
}

/** True when GET /rescheduling/{id} lists the liability (same link cancel gate uses via rescheduling_liabilities). */
export async function reschedulingIncludesLiability(
  Request: Pdt3368Fx['Request'],
  Endpoints: Pdt3368Fx['Endpoints'],
  reschedulingId: number,
  liabilityId: number,
): Promise<boolean> {
  const get = await Request.get(`${Endpoints.rescheduling}/${reschedulingId}`);
  await expect(get).CheckResponse();
  const view = (await get.json()) as {
    reschedulingLiabilityResponses?: Array<{ liabilityId?: number }>;
  };
  return (view.reschedulingLiabilityResponses ?? []).some(
    (row) => Number(row.liabilityId) === liabilityId,
  );
}

export function extraPortalLinks(opts: {
  gcIds?: number[];
  invoiceId?: number;
  billingRunId?: number;
  processId?: number;
}): Record<string, string[]> {
  const extra: Record<string, string[]> = {};
  if (opts.gcIds?.length) {
    extra.compensation = opts.gcIds.map((id) => buildDevCompensationPreviewLink(id));
  }
  if (opts.invoiceId) {
    extra.invoice = [buildDevInvoicePreviewLink(opts.invoiceId)];
  }
  if (opts.billingRunId) {
    extra.billingRun = [buildDevBillingRunPreviewLink(opts.billingRunId)];
  }
  if (opts.processId) {
    const processLink = buildProcessPreviewLink(opts.processId);
    if (processLink) {
      extra.process = [processLink];
    }
  }
  return extra;
}

void randomGens;
