/**
 * REG-704 — Invoice Correction volume change, scale BBS full reversal.
 *
 * Jira expected: standard excl-VAT 20 × 10 = 200; after correction BBS totalValue 20
 * with correction=true, debit (10+20)×20 = 600 and credit 200.
 *
 * Reference spec(s):
 * - tests/cursor/reg-566-invoice-correction-board-cases.fixtures.ts
 * - tests/cursor/pdt-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts
 *
 * Swagger: BillingByScalesCreateRequest.correction, InvoiceCorrectionParameters.volumeChange.
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  REG704_CREDIT_EXCL_VAT,
  REG704_DEBIT_EXCL_VAT,
  REG704_SCALE_CORRECTION_TOTAL,
  createCorrectionBillingRun,
  expectMoney,
  fetchInvoice,
  postBillingByScalesTariffRow,
  realizeCorrectionInvoices,
  realizeStandardForVolumesInvoice,
  runReg704ScalePrechain,
  splitDebitAndCredit,
  type Reg566Fx,
} from './reg-566-invoice-correction-board-cases.fixtures';

const JIRA_KEY = 'REG-704';
const JIRA_TITLE = 'Invoice Correction - volume Change flow | case 2 (full reversal)';

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, { tag: ['@billing', '@reg-566'] }, () => {
  test(`[${JIRA_KEY}]: ${JIRA_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const fx: Reg566Fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

    const pre = await test.step(
      'Precondition: legal customer → scale PC 20 → SLP POD+meter → product → contract → BBS volumes/unit/total 10',
      async () => runReg704ScalePrechain(fx),
    );
    TestRunSummary.registerPayload('scaleSetup', {
      pod: pre.podIdentifier,
      price: 20,
      originalBbsTotalValue: 10,
    });

    const billed = await test.step(
      'Precondition: STANDARD FOR_VOLUMES billing run, generate PDF, start accounting',
      async () => realizeStandardForVolumesInvoice(fx),
    );
    TestRunSummary.registerPayload('originalInvoice', billed);

    await test.step('Assert: original invoice is INVOICE excl-VAT 200', async () => {
      const original = await fetchInvoice(Request, billed.originalInvoiceId);
      expect(original.invoiceDocumentType).toBe('INVOICE');
      expectMoney(original.totalAmountExcludingVat, REG704_CREDIT_EXCL_VAT, 'original excl-VAT');
      TestRunSummary.recordCheck({
        check: 'Standard scale invoice excl-VAT',
        expectedResult: 'Document type INVOICE; amount excluding VAT = 20 × 10 = 200',
        actualResult: `As expected — type=${original.invoiceDocumentType}, exclVat=${original.totalAmountExcludingVat}`,
        passed: true,
      });
    });

    await test.step(
      'Post correction billing-by-scales (correction=true, volumes/unit/total 20, invoice number of realized invoice)',
      async () => {
        await postBillingByScalesTariffRow(
          fx,
          0,
          pre.period,
          {
            volumes: REG704_SCALE_CORRECTION_TOTAL,
            unitPrice: REG704_SCALE_CORRECTION_TOTAL,
            totalValue: REG704_SCALE_CORRECTION_TOTAL,
          },
          { correction: true, invoiceNumber: billed.originalInvoiceNumber },
        );
      },
    );

    const correctionBillingRunId = await test.step(
      'Create volume-change-only INVOICE_CORRECTION billing run',
      async () => createCorrectionBillingRun(fx, 0, false, true),
    );

    const correctionIds = await test.step(
      'Start correction, generate PDF, complete accounting (2 invoices)',
      async () => realizeCorrectionInvoices(fx, 2, 1),
    );

    const pair = await test.step('Assert: debit 600 and credit 200', async () => {
      const split = await splitDebitAndCredit(Request, correctionIds);
      expectMoney(split.debit.totalAmountExcludingVat, REG704_DEBIT_EXCL_VAT, 'debit excl-VAT');
      expectMoney(split.credit.totalAmountExcludingVat, REG704_CREDIT_EXCL_VAT, 'credit excl-VAT');
      TestRunSummary.recordCheck({
        check: 'Scale full-reversal debit/credit',
        expectedResult: 'Debit note excl-VAT (10+20)×20 = 600; credit note excl-VAT 20×10 = 200',
        actualResult:
          `As expected — debit=${split.debit.totalAmountExcludingVat}, credit=${split.credit.totalAmountExcludingVat}`,
        passed: true,
      });
      return split;
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: [
          'customer',
          'priceComponent',
          'product',
          'productContract',
          'pod',
          'meters',
          'dataByScales',
          'billingRun',
          'invoice',
        ],
        extraLinks: buildProductContractTabLinks(pre.contractId),
        snapshot: {
          originalInvoiceId: billed.originalInvoiceId,
          correctionBillingRunId,
          debitInvoiceId: pair.debit.id,
          creditInvoiceId: pair.credit.id,
        },
      });
    });
  });
});
