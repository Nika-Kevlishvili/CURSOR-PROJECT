/**
 * PDT-3500 — Invoice Correction of a REAL standard volume invoice plus a
 * signed negative government compensation must create an AUTOMATIC customer
 * receivable on the CORRECTION CREDIT_NOTE.
 *
 * The test encodes expected product behavior. A missing credit-note receivable
 * (the reported bug) fails the assertion.
 *
 * Swagger (Rule 41 this session, Cursor-Project/config/swagger/dev/swagger-spec.json):
 * - POST /billing-run BillingRunCreateRequest.billingType INVOICE_CORRECTION
 *   InvoiceCorrectionParameters.priceChange / volumeChange / listOfInvoices
 *   (correctionBilling template: priceChange true, volumeChange false unless
 *   the boolean args are true; startCorrectionDraftForInvoice passes the index only)
 * - PATCH /billing-run/start-billing|start-generating|start-accounting ?billingRunId
 * - POST /government-compensations CompensationRequest
 *   (documentAmount number, required; negative is valid)
 * - GET /government-compensations/{id} CompensationResponse
 *   compensationStatus UNINVOICED|INVOICED, documentAmount,
 *   receivableForCustomer, receivableForRecipient, liabilityForCustomer
 * - GET /invoice InvoiceResponse
 *   invoiceStatus REAL, invoiceType CORRECTION|STANDARD,
 *   invoiceDocumentType CREDIT_NOTE|DEBIT_NOTE|INVOICE,
 *   liabilitiesAndReceivables, debitCredits (no parentInvoiceId on the schema;
 *   connected invoices are debitCredits / connectedInvoices)
 * - GET /customer-receivable CustomerReceivableListingRequest.customerReceivableSearchBy CUSTOMER
 * - GET /customer-receivable/{id} creationType AUTOMATIC, invoiceResponse, initialAmount
 * - GET /customer-liability/{id} creationType AUTOMATIC, invoiceResponse
 *
 * Order matches PDT-3486: realize the original invoice with no compensation,
 * open the correction draft, POST compensation, then start-generating.
 * Do not POST a second billing-by-profile and do not POST invoice-cancellation.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3486-standard-billing-compensation-deferred-alo.fixtures.ts
 * - tests/cursor/PDT-3470-standard-credit-note-government-compensation-receivable.spec.ts
 * - tests/cursor/pdt-3470-standard-credit-note-government-compensation-receivable.fixtures.ts
 */

import { expect } from './cursor-test.fixtures';
import { readConnectedInvoiceIds } from './exp-parity-03-interim-deduction-and-issue-date.fixtures';
import {
  getInvoice,
  type InvoiceView,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import {
  getReceivable,
  money,
  nestedId,
  receivablesOnCreditNote,
  uniqueCompensationNumber,
  type Pdt3470Fx,
  type Pdt3470ReceivableView,
} from './pdt-3470-standard-credit-note-government-compensation-receivable.fixtures';

export const PDT_3500_KEY = 'PDT-3500';
export const PDT_3500_TITLE =
  '[Backend] invoice correction does not generate a receivable for credit note invoice.';
export const PDT_3500_TEST_TIMEOUT_MS = 70 * 60 * 1000;
/**
 * Signed compensation is this many times the original invoice total, and negative.
 * 10× is large enough that |compensation| is clearly above the invoice amount.
 */
export const PDT_3500_COMPENSATION_LARGER_THAN_INVOICE_FACTOR = 10;

/** Negative documentAmount whose absolute value is factor × the invoice total. */
export function negativeCompensationMuchLargerThanInvoice(invoiceTotalInclVat: number): number {
  const invoiceAbs = money(Math.abs(invoiceTotalInclVat));
  if (!(invoiceAbs > 0)) {
    throw new Error(`Original invoice total must be positive, got ${invoiceTotalInclVat}`);
  }
  return money(-invoiceAbs * PDT_3500_COMPENSATION_LARGER_THAN_INVOICE_FACTOR);
}

export const PDT_3500_RELEVANT_ENTITY_KEYS = [
  'customer',
  'pod',
  'product',
  'productContract',
  'billingRun',
  'invoice',
  'compensation',
  'customerReceivable',
  'customerLiability',
] as const;

export function uniquePdt3500CompensationNumber(): string {
  return uniqueCompensationNumber('NEG').replace(/^PDT3470-/, 'PDT3500-');
}

export function customerIdentifierById(
  Responses: { customer: Array<{ id?: number; identifier?: string }> },
  customerId: number,
): string {
  const row = Responses.customer.find((customer) => Number(customer.id) === customerId);
  return String(row?.identifier ?? '');
}

/**
 * InvoiceResponse has no parentInvoiceId. Parent is the original when connected
 * invoices (debitCredits, or connectedInvoices when the runtime sends it) include
 * the original id or invoice number.
 */
export function correctionDocumentParentsOriginal(
  invoice: InvoiceView,
  originalInvoiceId: number,
  originalInvoiceNumber: string,
): boolean {
  const body = invoice as Record<string, unknown>;
  if (readConnectedInvoiceIds(body).includes(originalInvoiceId)) {
    return true;
  }
  const rawParent = body.parentInvoiceId ?? body.parentInvoice;
  if (nestedId(rawParent) === originalInvoiceId || Number(rawParent) === originalInvoiceId) {
    return true;
  }
  const needle = originalInvoiceNumber.trim();
  if (!needle) {
    return false;
  }
  const rows = (body.connectedInvoices ?? body.debitCredits) as
    | Array<{ invoiceNumber?: string }>
    | undefined;
  if (!Array.isArray(rows)) {
    return false;
  }
  return rows.some((row) => {
    const number = String(row?.invoiceNumber ?? '').trim();
    return number === needle || number.endsWith(needle);
  });
}

export function receivableAmountMatchesCreditNoteOrCompensation(
  initialAmount: unknown,
  creditNoteTotalInclVat: number,
  compensationAmount: number,
): boolean {
  const amount = money(initialAmount);
  if (!(amount > 0)) {
    return false;
  }
  const accepted = new Set<number>([
    money(Math.abs(creditNoteTotalInclVat)),
    money(Math.abs(compensationAmount)),
  ]);
  return accepted.has(amount);
}

/**
 * AUTOMATIC customer receivables whose invoiceResponse is this invoice.
 * Unions GET invoice liabilitiesAndReceivables (type RECEIVABLE) with
 * GET customer-receivable listings for each customer identifier.
 */
export async function automaticReceivablesLinkedToInvoice(
  Request: Pdt3470Fx['Request'],
  invoiceId: number,
  customerIdentifiers: string[],
): Promise<Pdt3470ReceivableView[]> {
  const byId = new Map<number, Pdt3470ReceivableView>();

  const invoice = await getInvoice(Request, invoiceId);
  for (const row of invoice.liabilitiesAndReceivables ?? []) {
    if (String(row.type ?? '').toUpperCase() !== 'RECEIVABLE') {
      continue;
    }
    const id = Number(row.id);
    if (!Number.isFinite(id) || id <= 0) {
      continue;
    }
    const detail = await getReceivable(Request, id);
    if (detail.creationType !== 'AUTOMATIC') {
      continue;
    }
    if (nestedId(detail.invoiceResponse) !== invoiceId) {
      continue;
    }
    byId.set(detail.id, detail);
  }

  for (const identifier of customerIdentifiers) {
    const prompt = identifier.trim();
    if (!prompt) {
      continue;
    }
    const listed = await receivablesOnCreditNote(Request, prompt, invoiceId);
    for (const detail of listed) {
      if (detail.creationType !== 'AUTOMATIC') {
        continue;
      }
      if (nestedId(detail.invoiceResponse) !== invoiceId) {
        continue;
      }
      byId.set(detail.id, detail);
    }
  }

  return [...byId.values()].filter((row) => money(row.initialAmount) > 0);
}

export function assertCorrectionCreditAndDebitPair(
  classified: {
    correctionCnId: number | null;
    forwardInvoiceId: number | null;
    allDocs: Array<{ id: number; docType: string; invoiceType?: string; total: number }>;
  },
): { creditNoteId: number; debitDocumentId: number } {
  expect(
    classified.correctionCnId,
    `CORRECTION CREDIT_NOTE draft; drafts=${JSON.stringify(classified.allDocs)}`,
  ).toBeGreaterThan(0);
  expect(
    classified.forwardInvoiceId,
    `CORRECTION DEBIT_NOTE or INVOICE draft; drafts=${JSON.stringify(classified.allDocs)}`,
  ).toBeGreaterThan(0);
  expect(classified.correctionCnId).not.toBe(classified.forwardInvoiceId);
  return {
    creditNoteId: classified.correctionCnId as number,
    debitDocumentId: classified.forwardInvoiceId as number,
  };
}
