/**
 * PHN-3943 — InvoicedMonth empty on correction document model (thin wrappers).
 *
 * Reuses:
 * - pdt-2872-minimal-interim-payment.fixtures.ts (manual interim parent)
 * - pdt-2891-connected-invoices.fixtures.ts (manual CREDIT_NOTE)
 *
 * Document JSON API (Swagger dev2):
 * GET /billing-run/generate-invoice-data?invoiceId={id}
 * → BillingRunDocumentModelImpl.InvoicedMonth (string, format: date)
 */
import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import {
  createManualCreditOrDebitNote,
  resolveInterimInvoiceIds,
  startManualNoteAndGetCdId,
  type Pdt2891Fx,
} from './pdt-2891-connected-invoices.fixtures';
import {
  createManualInterimBillingRun,
  preconditionProductContractForManual,
  resolvePdt2872Amounts,
  type Pdt2872Fx,
} from './pdt-2872-minimal-interim-payment.fixtures';
import { startAndCompleteBillingRun } from './rps-pod-invoice-due-date.fixtures';

export const PHN_3943_KEY = 'PHN-3943';
export const PHN_3943_TITLE =
  'CLONE - Templater: Invoice - InvoicedMonth empty for corrections';

/** Swagger path: GET /billing-run/generate-invoice-data (operationId: generateData, deprecated). */
export const GENERATE_INVOICE_DATA_PATH = 'billing-run/generate-invoice-data';

export type Phn3943Fx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'
>;

export type ManualInterimParentOutcome = {
  parentInvoiceId: number;
  interimBillingRunId: number;
};

export type CreditNoteOutcome = {
  cdNoteId: number;
  creditNoteBillingRunId: number;
};

/**
 * Normalize InvoicedMonth (Swagger: string format date) to YYYY-MM-DD for comparison.
 */
export function normalizeInvoicedMonth(value: unknown): string | null {
  if (value == null) {
    return null;
  }
  const raw = String(value).trim();
  if (!raw) {
    return null;
  }
  return raw.slice(0, 10);
}

export function readInvoicedMonth(documentModel: Record<string, unknown>): string | null {
  return normalizeInvoicedMonth(documentModel.InvoicedMonth);
}

/**
 * GET billing-run/generate-invoice-data?invoiceId= → BillingRunDocumentModelImpl
 */
export async function fetchInvoiceDocumentModel(
  Request: Phn3943Fx['Request'],
  invoiceId: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${GENERATE_INVOICE_DATA_PATH}?invoiceId=${invoiceId}`);
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

/**
 * Manual interim/advance parent invoice (issuing_for_the_month source for InvoicedMonth).
 */
export async function createManualInterimParentInvoice(
  fx: Phn3943Fx,
): Promise<ManualInterimParentOutcome> {
  const amounts = await resolvePdt2872Amounts(fx.Request);

  await test.step('Precondition: product contract chain for manual interim', async () => {
    await preconditionProductContractForManual(fx as Pdt2872Fx);
  });

  let interimBillingRunId = 0;
  await test.step('Precondition: create + complete manual interim billing run', async () => {
    interimBillingRunId = await createManualInterimBillingRun(
      fx as Pdt2872Fx,
      amounts.exclFor500,
    );
    // Match PDT-2891 runManualInterimThenDeduction: complete before linking CREDIT_NOTE
    await startAndCompleteBillingRun(fx.Request, interimBillingRunId);
  });

  const interimIds = await test.step('Precondition: resolve interim invoice id', async () =>
    resolveInterimInvoiceIds(fx.Request, interimBillingRunId, 1),
  );

  const parentInvoiceId = interimIds[0];
  expect(parentInvoiceId, 'manual interim parent invoice id').toBeGreaterThan(0);
  if (!fx.Responses.invoice.includes(parentInvoiceId)) {
    fx.Responses.invoice.push(parentInvoiceId);
  }

  return { parentInvoiceId, interimBillingRunId };
}

/**
 * Manual CREDIT_NOTE linked to parent (PDT-2891 helpers — do not duplicate).
 */
export async function createCreditNoteForParent(
  fx: Phn3943Fx,
  parentInvoiceId: number,
): Promise<CreditNoteOutcome> {
  const { billingRunId: creditNoteBillingRunId } = await test.step(
    'Precondition: create manual CREDIT_NOTE billing run',
    async () => createManualCreditOrDebitNote(fx as Pdt2891Fx, [parentInvoiceId], 'CREDIT_NOTE'),
  );

  const cdNoteId = await test.step('Precondition: complete CREDIT_NOTE and resolve invoice id', async () =>
    startManualNoteAndGetCdId(fx as Pdt2891Fx, creditNoteBillingRunId, 'CREDIT_NOTE'),
  );

  expect(cdNoteId, 'CREDIT_NOTE invoice id').toBeGreaterThan(0);
  return { cdNoteId, creditNoteBillingRunId };
}
