/**
 * PHN-3992 — Invoice cancellation allowed when invoice accounting period is CLOSED;
 * taxEventDate must still fall in an OPEN accounting period.
 *
 * Approach (API-safe on Dev2):
 * - Find an editable past CLOSED period via OPEN probe (skips periods blocked by
 *   billing-run status / fileGenerationStatus IN_PROGRESS — AccountingPeriodService.edit),
 * - create a REAL manual interim invoice in that period,
 * - CLOSE the period again (async — poll until CLOSED),
 * - POST /invoice-cancellation with taxEventDate inside a currently OPEN period.
 *
 * Reference:
 * - tests/cursor/phn-3943-invoiced-month-empty-for-corrections.fixtures.ts
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts (startAndCompleteBillingRun)
 * - Swagger CreateInvoiceCancellationRequest / AccountingPeriodRequest (dev2)
 */
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { asBillingRunId } from './pdt-2599-service-contract.fixtures';
import {
  buildManualInterimPayload,
  preconditionProductContractForManual,
  resolvePdt2872Amounts,
  type Pdt2872Fx,
} from './pdt-2872-minimal-interim-payment.fixtures';
import { resolveInterimInvoiceIds } from './pdt-2891-connected-invoices.fixtures';
import { startAndCompleteBillingRun } from './rps-pod-invoice-due-date.fixtures';

export const PHN_3992_KEY = 'PHN-3992';
export const PHN_3992_TITLE =
  'CLONE - Remove restriction for invoice cancelation in closed accounting period';

export const CLOSED_PERIOD_ERROR_SNIPPET = 'Accounting period is closed for this invoice!';
export const TAX_EVENT_OPEN_PERIOD_SNIPPET = 'must be within an open accounting period';

const BILLING_RUN_ROOT = 'billing-run';
const AP_POLL_MS = 5_000;
const AP_CLOSE_MAX_MS = 5 * 60 * 1000;

export type Phn3992Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type AccountingPeriodRow = {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  status: 'OPEN' | 'CLOSED' | string;
  modifyDate?: string;
  /** From GET /accounting-period/{id} (AccountingPeriodsPreviewResponse). */
  fileGenerationStatus?: string;
};

export type InvoiceCancellationCreateResult = {
  status: number;
  body: Record<string, unknown>;
  rawText: string;
};

function toYmd(value: string | undefined | null): string {
  if (!value) return '';
  return String(value).slice(0, 10);
}

function midDateInclusive(startYmd: string, endYmd: string): string {
  const start = Date.parse(`${startYmd}T00:00:00Z`);
  const end = Date.parse(`${endYmd}T00:00:00Z`);
  if (!Number.isFinite(start) || !Number.isFinite(end) || end < start) {
    return endYmd || startYmd;
  }
  const mid = new Date(start + Math.floor((end - start) / 2));
  return mid.toISOString().slice(0, 10);
}

function todayYmd(): string {
  return new Date().toISOString().slice(0, 10);
}

/** Same numeric extraction as BillingPayloads.invoiceCancellation(). */
export function cancellationInvoiceToken(invoiceNumber: string): string {
  const raw = String(invoiceNumber ?? '').trim();
  if (!raw) return raw;
  const parts = raw.split('-');
  return parts.length > 1 ? parts[parts.length - 1]! : raw;
}

export function extractApiErrorMessage(body: unknown, rawText = ''): string {
  if (body && typeof body === 'object') {
    const o = body as Record<string, unknown>;
    const parts = [o.message, o.error, o.detail, o.title]
      .filter((v) => typeof v === 'string' && v.trim())
      .map((v) => String(v));
    if (parts.length) return parts.join(' | ');
  }
  return rawText || JSON.stringify(body ?? {});
}

export async function getAccountingPeriod(
  Request: Phn3992Fx['Request'],
  id: number,
): Promise<AccountingPeriodRow> {
  const res = await Request.get(`accounting-period/${id}`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as Record<string, unknown>;
  return {
    id: Number(body.id ?? id),
    name: String(body.name ?? ''),
    startDate: String(body.startDate ?? ''),
    endDate: String(body.endDate ?? ''),
    status: String(body.status ?? ''),
    modifyDate: body.modifyDate != null ? String(body.modifyDate) : undefined,
    fileGenerationStatus:
      body.fileGenerationStatus != null ? String(body.fileGenerationStatus) : undefined,
  };
}

/**
 * GET /accounting-period?page=&size=&status= (AccountingPeriodsListingRequest).
 * Falls back to billing-run available list for OPEN-only when listing shape differs.
 */
export async function listAccountingPeriodsByStatus(
  Request: Phn3992Fx['Request'],
  status: 'OPEN' | 'CLOSED',
  size = 100,
): Promise<AccountingPeriodRow[]> {
  const res = await Request.get(
    `accounting-period?page=0&size=${size}&status=${status}&sortBy=END_DATE&direction=DESC`,
  );
  if (res.ok()) {
    const page = (await res.json()) as {
      content?: Array<Record<string, unknown>>;
    };
    return (page.content ?? [])
      .map((row) => ({
        id: Number(row.accountPeriodId ?? row.id),
        name: String(row.name ?? ''),
        startDate: String(row.startDate ?? ''),
        endDate: String(row.endDate ?? ''),
        status: String(row.status ?? status),
        modifyDate: row.modifyDate != null ? String(row.modifyDate) : undefined,
      }))
      .filter((r) => Number.isFinite(r.id) && r.id > 0);
  }

  if (status === 'OPEN') {
    const openRes = await Request.get(
      'billing-run/accounting-period-available-list?page=0&size=50',
    );
    await expect(openRes).CheckResponse();
    const page = (await openRes.json()) as { content?: Array<Record<string, unknown>> };
    return (page.content ?? [])
      .map((row) => ({
        id: Number(row.id ?? row.accountPeriodId),
        name: String(row.name ?? ''),
        startDate: String(row.startDate ?? ''),
        endDate: String(row.endDate ?? ''),
        status: String(row.status ?? 'OPEN'),
        modifyDate: row.modifyDate != null ? String(row.modifyDate) : undefined,
      }))
      .filter((r) => Number.isFinite(r.id) && r.id > 0);
  }

  throw new Error(
    `[PHN-3992] GET /accounting-period listing failed for status=${status} (HTTP ${res.status()}). ` +
      `Body: ${await res.text().catch(() => '')}`,
  );
}

const BILLING_RUN_BLOCK_SNIPPET = 'status in billing run';
const FILE_GEN_BLOCK_SNIPPET = 'file generation is in progress';

function isEditableBlockedByBillingOrFileGen(message: string): boolean {
  const lower = message.toLowerCase();
  return (
    lower.includes(BILLING_RUN_BLOCK_SNIPPET) || lower.includes(FILE_GEN_BLOCK_SNIPPET)
  );
}

/**
 * Past CLOSED period we can temporarily reopen (endDate < today), verified by API probe.
 *
 * Phoenix `AccountingPeriodService.edit` blocks status change when:
 * - `fileGenerationStatus == IN_PROGRESS`, or
 * - any billing run on the period is in
 *   IN_PROGRESS_DRAFT | DRAFT | IN_PROGRESS_GENERATION | GENERATED |
 *   IN_PROGRESS_ACCOUNTING | IN_PROGRESS_TERMINATION | PAUSED
 * (see phoenix-core-lib `AccountingPeriodService.java` ~103–129).
 * Also requires endDate < now before open/close.
 *
 * Strategy: list CLOSED, filter past, newest endDate first; skip IN_PROGRESS file gen;
 * PUT OPEN as probe. On 2xx the period is already OPEN (callers may still call
 * `setAccountingPeriodStatus(..., 'OPEN')` — it no-ops when status matches).
 * Skip candidates blocked by billing-run / file-gen errors; fail-fast on other errors.
 *
 * Dev2 evidence: ACCOUNTING202604 (id=1008) had hundreds of blocking BRs;
 * ACCOUNTING202508 (id=1) was editable — prefer probe over hardcoding.
 */
export async function findEditablePastClosedPeriod(
  Request: Phn3992Fx['Request'],
): Promise<AccountingPeriodRow> {
  const today = todayYmd();
  const closed = await listAccountingPeriodsByStatus(Request, 'CLOSED', 100);
  const past = closed
    .filter((r) => toYmd(r.endDate) && toYmd(r.endDate) < today)
    .sort((a, b) => toYmd(b.endDate).localeCompare(toYmd(a.endDate)));

  if (!past.length) {
    throw new Error(
      '[PHN-3992] No CLOSED accounting period with endDate < today. ' +
        'Cannot reopen/close via API (AccountingPeriodService rejects close/open until end date).',
    );
  }

  const tried: string[] = [];
  let lastError = '';

  for (const candidate of past) {
    const label = `${candidate.id}/${candidate.name || '?'}`;
    const preview = await getAccountingPeriod(Request, candidate.id);

    if (String(preview.fileGenerationStatus ?? '').toUpperCase() === 'IN_PROGRESS') {
      tried.push(`${label} (skipped: fileGenerationStatus=IN_PROGRESS)`);
      continue;
    }

    const payload = {
      name: preview.name,
      startDate: preview.startDate,
      endDate: preview.endDate,
      status: 'OPEN' as const,
      modifyDate: preview.modifyDate ?? new Date().toISOString(),
    };

    const put = await Request.put(`accounting-period/${candidate.id}`, { data: payload });
    const httpStatus = put.status();
    const rawText = await put.text().catch(() => '');
    let body: unknown = {};
    try {
      body = rawText ? JSON.parse(rawText) : {};
    } catch {
      body = { raw: rawText };
    }
    const message = extractApiErrorMessage(body, rawText);

    if (httpStatus >= 200 && httpStatus < 300) {
      // Probe succeeded — period is already OPEN for the rest of the test.
      const opened = await getAccountingPeriod(Request, candidate.id);
      console.log(
        `[PHN-3992] Editable past CLOSED period selected via OPEN probe: ` +
          `id=${opened.id} name=${opened.name} (was CLOSED; now ${opened.status}).`,
      );
      return opened;
    }

    if (isEditableBlockedByBillingOrFileGen(message)) {
      tried.push(`${label} (HTTP ${httpStatus}: ${message})`);
      lastError = message;
      continue;
    }

    throw new Error(
      `[PHN-3992] Unexpected error probing OPEN on accounting-period ${label} ` +
        `(HTTP ${httpStatus}): ${message || rawText || '(empty body)'}`,
    );
  }

  throw new Error(
    `[PHN-3992] No editable past CLOSED accounting period found after probing ${tried.length} candidate(s). ` +
      `Tried: ${tried.join(' | ')}. Last skip/error: ${lastError || '(none)'}`,
  );
}

/** @deprecated Prefer {@link findEditablePastClosedPeriod} (API probe for billing-run blocks). */
export async function findPastClosedPeriod(
  Request: Phn3992Fx['Request'],
): Promise<AccountingPeriodRow> {
  return findEditablePastClosedPeriod(Request);
}

/** OPEN period that contains taxEventDate (prefer today). */
export async function findOpenPeriodContainingDate(
  Request: Phn3992Fx['Request'],
  ymd: string,
): Promise<AccountingPeriodRow> {
  const open = await listAccountingPeriodsByStatus(Request, 'OPEN', 100);
  const hit = open.find((r) => {
    const a = toYmd(r.startDate);
    const b = toYmd(r.endDate);
    return a && b && ymd >= a && ymd <= b;
  });
  if (!hit) {
    throw new Error(
      `[PHN-3992] No OPEN accounting period contains taxEventDate=${ymd}. ` +
        `OPEN count=${open.length}.`,
    );
  }
  return hit;
}

/**
 * PUT /accounting-period/{id} with AccountingPeriodRequest.
 * CLOSE is async (file generation) — poll until status matches when waiting.
 */
export async function setAccountingPeriodStatus(
  Request: Phn3992Fx['Request'],
  period: AccountingPeriodRow,
  status: 'OPEN' | 'CLOSED',
  opts?: { waitUntilApplied?: boolean },
): Promise<AccountingPeriodRow> {
  const preview = await getAccountingPeriod(Request, period.id);
  if (String(preview.status).toUpperCase() === status) {
    return preview;
  }

  const payload = {
    name: preview.name,
    startDate: preview.startDate,
    endDate: preview.endDate,
    status,
    modifyDate: preview.modifyDate ?? new Date().toISOString(),
  };

  const put = await Request.put(`accounting-period/${period.id}`, { data: payload });
  await expect(put).CheckResponse();

  if (opts?.waitUntilApplied === false) {
    return getAccountingPeriod(Request, period.id);
  }

  const deadline = Date.now() + AP_CLOSE_MAX_MS;
  while (Date.now() < deadline) {
    const current = await getAccountingPeriod(Request, period.id);
    if (String(current.status).toUpperCase() === status) {
      return current;
    }
    await new Promise((r) => setTimeout(r, AP_POLL_MS));
  }

  const last = await getAccountingPeriod(Request, period.id);
  throw new Error(
    `[PHN-3992] Timed out waiting for accounting-period ${period.id} status=${status} ` +
      `(last=${last.status}, file may still be generating).`,
  );
}

export async function createManualInterimInvoiceInPeriod(
  fx: Phn3992Fx,
  accountingPeriodId: number,
  periodDateYmd: string,
): Promise<{ invoiceId: number; billingRunId: number; invoiceNumber: string }> {
  const amounts = await resolvePdt2872Amounts(fx.Request);

  await test.step('Precondition: product contract chain for manual interim', async () => {
    await preconditionProductContractForManual(fx as Pdt2872Fx);
  });

  let billingRunId = 0;
  await test.step('Precondition: create + complete manual interim in target period', async () => {
    const payload = await buildManualInterimPayload(fx as Pdt2872Fx, amounts.exclFor500);
    payload.commonParameters.accountingPeriodId = accountingPeriodId;
    payload.commonParameters.taxEventDate = periodDateYmd;
    payload.commonParameters.invoiceDate = periodDateYmd;
    payload.commonParameters.dueDate = periodDateYmd;

    const createRes = await fx.Request.post(fx.Endpoints.billingRun, { data: payload });
    await expect(createRes).CheckResponse();
    billingRunId = asBillingRunId(await createRes.json());
    fx.Responses.billingRun.push({
      id: billingRunId,
      portalType: 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT',
    });

    const putRes = await fx.Request.put(`${BILLING_RUN_ROOT}/${billingRunId}`, { data: payload });
    await expect(putRes).CheckResponse();

    await startAndCompleteBillingRun(fx.Request, billingRunId);
  });

  const invoiceIds = await test.step('Precondition: resolve interim invoice id', async () =>
    resolveInterimInvoiceIds(fx.Request, billingRunId, 1),
  );
  const invoiceId = invoiceIds[0]!;
  expect(invoiceId, 'manual interim invoice id').toBeGreaterThan(0);
  if (!fx.Responses.invoice.includes(invoiceId)) {
    fx.Responses.invoice.push(invoiceId);
  }

  const invRes = await fx.Request.get(`invoice?id=${invoiceId}`);
  await expect(invRes).CheckResponse();
  const invBody = (await invRes.json()) as { invoiceNumber?: string; invoiceStatus?: string };
  expect(invBody.invoiceStatus, 'invoice must be REAL before cancel').toBe('REAL');
  const invoiceNumber = String(invBody.invoiceNumber ?? '');
  expect(invoiceNumber, 'invoiceNumber required for cancellation').toBeTruthy();

  return { invoiceId, billingRunId, invoiceNumber };
}

export async function postInvoiceCancellation(
  Request: Phn3992Fx['Request'],
  opts: {
    invoices: string;
    taxEventDate: string;
    templateId?: number;
    fileId?: number | null;
  },
): Promise<InvoiceCancellationCreateResult> {
  const payload = {
    invoices: opts.invoices,
    fileId: opts.fileId ?? null,
    taxEventDate: opts.taxEventDate,
    templateId: opts.templateId ?? Number(envVariables.invoice_cancellation_template),
  };
  const res = await Request.post('invoice-cancellation', { data: payload });
  const status = res.status();
  const rawText = await res.text().catch(() => '');
  let body: Record<string, unknown> = {};
  try {
    body = rawText ? (JSON.parse(rawText) as Record<string, unknown>) : {};
  } catch {
    body = { raw: rawText };
  }
  return { status, body, rawText };
}

export async function waitForInvoiceStatus(
  Request: Phn3992Fx['Request'],
  invoiceId: number,
  status: string,
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
        timeout: 90_000,
        intervals: [2_000, 5_000],
      },
    )
    .toBe(status);

  const finalRes = await Request.get(`invoice?id=${invoiceId}`);
  await expect(finalRes).CheckResponse();
  return String(((await finalRes.json()) as { invoiceStatus?: string }).invoiceStatus ?? '');
}

export function pickTaxEventDateInPeriod(period: AccountingPeriodRow): string {
  const start = toYmd(period.startDate);
  const end = toYmd(period.endDate);
  const today = todayYmd();
  if (today >= start && today <= end) return today;
  return midDateInclusive(start, end);
}

/** Date guaranteed outside any OPEN period (far future). */
export function taxEventDateOutsideOpenPeriods(): string {
  return '2099-06-15';
}
