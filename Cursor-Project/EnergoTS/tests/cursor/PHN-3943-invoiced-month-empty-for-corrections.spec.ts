/**
 * PHN-3943 — Templater InvoicedMonth empty for corrections (bug-only automation).
 *
 * Jira reproduce:
 * 1. Create invoice (manual interim — issuing_for_the_month source)
 * 2. Create CREDIT_NOTE correction linked to parent
 * 3. GET /billing-run/generate-invoice-data?invoiceId= for parent + credit note
 * Expected: credit InvoicedMonth equals parent InvoicedMonth (non-empty)
 *
 * Runtime fix (Dev2 phoenix-core-lib InvoiceRepository.getInvoiceDocumentModel):
 * coalesce(ci.meter_reading_period_to, ci.issuing_for_the_month,
 *          oi.meter_reading_period_to, oi.issuing_for_the_month)
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2891-connected-invoices.spec.ts (+ pdt-2891-connected-invoices.fixtures.ts)
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 *
 * Target env: Dev2
 *   BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 */
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PHN_3943_KEY,
  PHN_3943_TITLE,
  createCreditNoteForParent,
  createManualInterimParentInvoice,
  fetchInvoiceDocumentModel,
  readInvoicedMonth,
  type Phn3943Fx,
} from './phn-3943-invoiced-month-empty-for-corrections.fixtures';

test.describe(`[${PHN_3943_KEY}]: ${PHN_3943_TITLE}`, {
  tag: ['@billing', '@phn-3943', '@dev2', '@templater'],
}, () => {
  test(`[${PHN_3943_KEY}]: ${PHN_3943_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(40 * 60 * 1000);

    const fx: Phn3943Fx = { Request, GeneratePayload, Responses, Endpoints };

    const { parentInvoiceId, interimBillingRunId } = await test.step(
      'Precondition: create manual interim parent invoice',
      async () => createManualInterimParentInvoice(fx),
    );

    TestRunSummary.registerPayload('billingRun', {
      interimBillingRunId,
      parentInvoiceId,
      documentType: 'INTERIM_AND_ADVANCE_PAYMENT',
    });

    const { cdNoteId, creditNoteBillingRunId } = await test.step(
      'Precondition: create CREDIT_NOTE linked to parent',
      async () => createCreditNoteForParent(fx, parentInvoiceId),
    );

    TestRunSummary.registerPayload('invoice', {
      parentInvoiceId,
      cdNoteId,
      creditNoteBillingRunId,
      documentType: 'CREDIT_NOTE',
    });

    const parentDoc = await test.step(
      'GET generate-invoice-data for parent interim invoice',
      async () => {
        const doc = await fetchInvoiceDocumentModel(Request, parentInvoiceId);
        const parentMonth = readInvoicedMonth(doc);
        expect(
          parentMonth,
          `Parent invoice ${parentInvoiceId} InvoicedMonth must be non-empty (month source present)`,
        ).toBeTruthy();
        TestRunSummary.registerPayload('parentDocumentModel', {
          invoiceId: parentInvoiceId,
          InvoicedMonth: parentMonth,
        });
        return doc;
      },
    );

    const creditDoc = await test.step(
      'GET generate-invoice-data for CREDIT_NOTE',
      async () => {
        const doc = await fetchInvoiceDocumentModel(Request, cdNoteId);
        TestRunSummary.registerPayload('creditNoteDocumentModel', {
          invoiceId: cdNoteId,
          InvoicedMonth: readInvoicedMonth(doc),
        });
        return doc;
      },
    );

    await test.step(
      'Assert CREDIT_NOTE InvoicedMonth equals parent InvoicedMonth',
      async () => {
        const parentMonth = readInvoicedMonth(parentDoc);
        const creditMonth = readInvoicedMonth(creditDoc);

        let passed = true;
        let assertionError: string | undefined;
        try {
          expect(
            creditMonth,
            `CREDIT_NOTE ${cdNoteId} InvoicedMonth must not be null/empty (PHN-3943)`,
          ).toBeTruthy();
          expect(
            creditMonth,
            `CREDIT_NOTE InvoicedMonth must equal parent InvoicedMonth (normalized YYYY-MM-DD)`,
          ).toBe(parentMonth);
        } catch (err) {
          passed = false;
          assertionError = err instanceof Error ? err.message : String(err);
          throw err;
        } finally {
          TestRunSummary.recordCheck({
            check: 'CREDIT_NOTE InvoicedMonth matches parent',
            expectedResult:
              'GET billing-run/generate-invoice-data?invoiceId=credit → InvoicedMonth non-empty and equal to parent InvoicedMonth (YYYY-MM-DD).',
            actualResult: passed
              ? `As expected — parent=${parentMonth} credit=${creditMonth} (parentId=${parentInvoiceId}, cdId=${cdNoteId}).`
              : `Not as expected — parent=${parentMonth} credit=${creditMonth} (parentId=${parentInvoiceId}, cdId=${cdNoteId}). ${assertionError ?? ''}`.trim(),
            passed,
          });
        }
      },
    );

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_3943_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        snapshot: {
          parentInvoiceId,
          cdNoteId,
          interimBillingRunId,
          creditNoteBillingRunId,
          parentInvoicedMonth: readInvoicedMonth(parentDoc),
          creditInvoicedMonth: readInvoicedMonth(creditDoc),
        },
      });
    });
  });
});
