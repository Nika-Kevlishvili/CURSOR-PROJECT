/**
 * PHN-3992 — Cancel invoice when its accounting period is CLOSED;
 * reject taxEventDate outside an OPEN accounting period.
 *
 * Jira: CLONE - Remove restriction for invoice cancelation in closed accounting period
 * Confluence: Invoice Cancelation - Create Process (page 27099258)
 *
 * Swagger (dev2):
 * - POST /invoice-cancellation → CreateInvoiceCancellationRequest
 *   (invoices, fileId, taxEventDate, templateId) → 200 InvoiceCancellationResponse
 * - GET /accounting-period/{id}, PUT /accounting-period/{id} → AccountingPeriodRequest
 *   status enum OPEN|CLOSED
 *
 * Reference spec(s):
 * - tests/cursor/PHN-3943-invoiced-month-empty-for-corrections.spec.ts
 * - tests/cursor/phn-3943-invoiced-month-empty-for-corrections.fixtures.ts
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - tests/cursor/pdt-2891-connected-invoices.fixtures.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - tests/billing/InvoiceCancelation/manualInvoiceCancellation.spec.ts
 *
 * Target env: Dev2
 *   BASE_URL typically https://devapps.energo-pro.bg/backend/phoenix2-dev
 *
 * Swagger refresh note: update-swagger-specs.ps1 failed on macOS (curl.exe missing);
 * Dev2 spec refreshed via /usr/bin/curl from environments.json openapi_json.
 */
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import {
  PHN_3992_KEY,
  PHN_3992_TITLE,
  CLOSED_PERIOD_ERROR_SNIPPET,
  TAX_EVENT_OPEN_PERIOD_SNIPPET,
  cancellationInvoiceToken,
  createManualInterimInvoiceInPeriod,
  extractApiErrorMessage,
  findEditablePastClosedPeriod,
  findOpenPeriodContainingDate,
  pickTaxEventDateInPeriod,
  postInvoiceCancellation,
  setAccountingPeriodStatus,
  taxEventDateOutsideOpenPeriods,
  waitForInvoiceStatus,
  type Phn3992Fx,
} from './phn-3992-invoice-cancel-closed-accounting-period.fixtures';

test.describe(`[${PHN_3992_KEY}]: ${PHN_3992_TITLE}`, {
  tag: ['@billing', '@phn-3992', '@dev2'],
}, () => {
  test(`[${PHN_3992_KEY}]: ${PHN_3992_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(50 * 60 * 1000);

    const fx: Phn3992Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pastClosed = await test.step(
      'Precondition: resolve editable past CLOSED accounting period (OPEN probe)',
      async () => findEditablePastClosedPeriod(Request),
    );

    const openForTax = await test.step(
      'Precondition: resolve OPEN accounting period for taxEventDate',
      async () => findOpenPeriodContainingDate(Request, new Date().toISOString().slice(0, 10)),
    );

    const taxEventDate = pickTaxEventDateInPeriod(openForTax);
    expect(
      openForTax.id,
      'taxEventDate OPEN period must differ from invoice period under test',
    ).not.toBe(pastClosed.id);

    TestRunSummary.registerPayload('accountingPeriod', {
      invoicePeriodId: pastClosed.id,
      invoicePeriodName: pastClosed.name,
      invoicePeriodEnd: pastClosed.endDate,
      taxEventPeriodId: openForTax.id,
      taxEventPeriodName: openForTax.name,
      taxEventDate,
    });

    let invoicePeriodMutated = false;
    let invoiceId = 0;
    let billingRunId = 0;
    let invoiceNumber = '';
    let cancelStatus = 0;
    let cancelBody: Record<string, unknown> = {};
    let cancelRaw = '';
    let finalInvoiceStatus = '';

    try {
      await test.step('Precondition: temporarily OPEN past CLOSED period', async () => {
        await setAccountingPeriodStatus(Request, pastClosed, 'OPEN', {
          waitUntilApplied: true,
        });
        invoicePeriodMutated = true;
      });

      const invoicePeriodDate = pastClosed.endDate.slice(0, 10);
      const created = await test.step(
        'Precondition: create REAL manual interim invoice in reopened period',
        async () =>
          createManualInterimInvoiceInPeriod(fx, pastClosed.id, invoicePeriodDate),
      );
      invoiceId = created.invoiceId;
      billingRunId = created.billingRunId;
      invoiceNumber = created.invoiceNumber;

      TestRunSummary.registerPayload('invoice', {
        invoiceId,
        invoiceNumber,
        billingRunId,
        accountingPeriodId: pastClosed.id,
      });

      await test.step('Close invoice accounting period (async CLOSE + poll)', async () => {
        const closed = await setAccountingPeriodStatus(Request, pastClosed, 'CLOSED', {
          waitUntilApplied: true,
        });
        expect(closed.status.toUpperCase(), 'invoice accounting period must be CLOSED').toBe(
          'CLOSED',
        );
      });

      const cancelToken = cancellationInvoiceToken(invoiceNumber);
      const cancelPayload = {
        invoices: cancelToken,
        fileId: null as null,
        taxEventDate,
        templateId: Number(envVariables.invoice_cancellation_template),
      };
      TestRunSummary.registerPayload('invoiceCancellation', cancelPayload);

      await test.step(
        'POST /invoice-cancellation with taxEventDate in OPEN period',
        async () => {
          const result = await postInvoiceCancellation(Request, {
            invoices: cancelToken,
            taxEventDate,
            templateId: cancelPayload.templateId,
            fileId: null,
          });
          cancelStatus = result.status;
          cancelBody = result.body;
          cancelRaw = result.rawText;

          const combined = `${extractApiErrorMessage(cancelBody, cancelRaw)} ${cancelRaw}`;
          expect(
            combined.toLowerCase(),
            'must not reject with closed-invoice-period error',
          ).not.toContain(CLOSED_PERIOD_ERROR_SNIPPET.toLowerCase());

          expect(
            cancelStatus,
            `POST /invoice-cancellation expected 2xx, got ${cancelStatus}: ${combined}`,
          ).toBeGreaterThanOrEqual(200);
          expect(cancelStatus).toBeLessThan(300);

          if (result.body.invoiceCancellationId != null) {
            Responses.invoiceCancellation.push(result.body);
          }
        },
      );

      finalInvoiceStatus = await test.step('Poll invoice until CANCELLED', async () =>
        waitForInvoiceStatus(Request, invoiceId, 'CANCELLED'),
      );

      await test.step('Assert happy-path cancellation outcomes', async () => {
        let passed = true;
        let assertionError: string | undefined;
        try {
          expect(finalInvoiceStatus).toBe('CANCELLED');
          expect(cancelStatus).toBeGreaterThanOrEqual(200);
          expect(cancelStatus).toBeLessThan(300);
        } catch (err) {
          passed = false;
          assertionError = err instanceof Error ? err.message : String(err);
          throw err;
        } finally {
          TestRunSummary.recordCheck({
            check: 'Cancel REAL invoice whose accounting period is CLOSED',
            expectedResult:
              'POST /invoice-cancellation succeeds (2xx); invoice becomes CANCELLED; ' +
              `no error containing "${CLOSED_PERIOD_ERROR_SNIPPET}"; taxEventDate in OPEN period.`,
            actualResult: passed
              ? `As expected — HTTP ${cancelStatus}, invoiceStatus=${finalInvoiceStatus}, ` +
                `invoicePeriod=${pastClosed.id} CLOSED, taxEventDate=${taxEventDate} in OPEN ${openForTax.id}.`
              : `Not as expected — HTTP ${cancelStatus}, invoiceStatus=${finalInvoiceStatus}. ${assertionError ?? ''}`.trim(),
            passed,
          });
        }
      });
    } finally {
      if (invoicePeriodMutated) {
        await test.step('Cleanup: ensure past period remains CLOSED', async () => {
          try {
            await setAccountingPeriodStatus(Request, pastClosed, 'CLOSED', {
              waitUntilApplied: true,
            });
          } catch (err) {
            console.warn(
              `[PHN-3992] Cleanup close of accounting-period ${pastClosed.id} failed:`,
              err,
            );
          }
        });
      }
    }

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_3992_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        snapshot: {
          invoiceId,
          invoiceNumber,
          billingRunId,
          invoiceAccountingPeriodId: pastClosed.id,
          taxEventDate,
          taxEventAccountingPeriodId: openForTax.id,
          cancelHttpStatus: cancelStatus,
          finalInvoiceStatus,
          cancelResponse: cancelBody,
        },
      });
    });
  });

  test(`[${PHN_3992_KEY}]: ${PHN_3992_TITLE} | tax event date not in open period`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(40 * 60 * 1000);

    const fx: Phn3992Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pastClosed = await test.step(
      'Precondition: resolve editable past CLOSED period for invoice (OPEN probe)',
      async () => findEditablePastClosedPeriod(Request),
    );

    let invoicePeriodMutated = false;
    let invoiceId = 0;
    let invoiceNumber = '';
    const badTaxEventDate = taxEventDateOutsideOpenPeriods();

    try {
      await test.step('Precondition: temporarily OPEN past CLOSED period', async () => {
        await setAccountingPeriodStatus(Request, pastClosed, 'OPEN', {
          waitUntilApplied: true,
        });
        invoicePeriodMutated = true;
      });

      const created = await test.step(
        'Precondition: create REAL manual interim invoice',
        async () =>
          createManualInterimInvoiceInPeriod(
            fx,
            pastClosed.id,
            pastClosed.endDate.slice(0, 10),
          ),
      );
      invoiceId = created.invoiceId;
      invoiceNumber = created.invoiceNumber;

      TestRunSummary.registerPayload('invoice', {
        invoiceId,
        invoiceNumber,
        billingRunId: created.billingRunId,
      });

      // Leave period OPEN for this negative — taxEventDate validation is independent.
      // Still restore CLOSED in finally.

      const cancelToken = cancellationInvoiceToken(invoiceNumber);
      const cancelPayload = {
        invoices: cancelToken,
        fileId: null as null,
        taxEventDate: badTaxEventDate,
        templateId: Number(envVariables.invoice_cancellation_template),
      };
      TestRunSummary.registerPayload('invoiceCancellation', cancelPayload);

      await test.step(
        'POST /invoice-cancellation with taxEventDate outside OPEN periods → reject',
        async () => {
          const result = await postInvoiceCancellation(Request, {
            invoices: cancelToken,
            taxEventDate: badTaxEventDate,
            templateId: cancelPayload.templateId,
            fileId: null,
          });

          const message = extractApiErrorMessage(result.body, result.rawText);
          const combined = `${message} ${result.rawText}`;

          let passed = true;
          let assertionError: string | undefined;
          try {
            expect(
              result.status,
              `expected client/business error, got ${result.status}: ${combined}`,
            ).toBeGreaterThanOrEqual(400);
            expect(
              combined.toLowerCase(),
              'error must mention Tax event date',
            ).toContain('tax event date');
            expect(
              combined.toLowerCase(),
              `error must contain "${TAX_EVENT_OPEN_PERIOD_SNIPPET}"`,
            ).toContain(TAX_EVENT_OPEN_PERIOD_SNIPPET.toLowerCase());
          } catch (err) {
            passed = false;
            assertionError = err instanceof Error ? err.message : String(err);
            throw err;
          } finally {
            TestRunSummary.recordCheck({
              check: 'Reject taxEventDate outside any OPEN accounting period',
              expectedResult:
                `HTTP 4xx with message containing "Tax event date" and "${TAX_EVENT_OPEN_PERIOD_SNIPPET}".`,
              actualResult: passed
                ? `As expected — HTTP ${result.status}: ${message}`
                : `Not as expected — HTTP ${result.status}: ${message}. ${assertionError ?? ''}`.trim(),
              passed,
            });
          }
        },
      );
    } finally {
      if (invoicePeriodMutated) {
        await test.step('Cleanup: restore past period to CLOSED', async () => {
          try {
            await setAccountingPeriodStatus(Request, pastClosed, 'CLOSED', {
              waitUntilApplied: true,
            });
          } catch (err) {
            console.warn(
              `[PHN-3992] Cleanup close of accounting-period ${pastClosed.id} failed:`,
              err,
            );
          }
        });
      }
    }

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_3992_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        snapshot: {
          invoiceId,
          invoiceNumber,
          badTaxEventDate,
          scenario: 'negative-tax-event-not-in-open-period',
        },
      });
    });
  });
});
