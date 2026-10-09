/**
 * REG-705 — Invoice Correction volume change, mixed half + full reversal.
 *
 * Same as REG-704 on the SLP/scale POD, plus an unchanged settlement POD with BBP.
 * Jira expected: one debit note with 2 summary rows (POD1 600 + POD2 original amount)
 * and one credit note 200 for the reversed SLP POD only.
 *
 * Reference spec(s):
 * - tests/cursor/reg-566-invoice-correction-board-cases.fixtures.ts
 * - tests/cursor/REG-704-invoice-correction-volume-full-reversal.spec.ts
 *
 * Swagger: InvoiceSummaryDataResponse rows, InvoiceResponse.invoiceDocumentType.
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
  REG705_SETTLEMENT_ORIGINAL_EXCL_VAT,
  createCorrectionBillingRun,
  expectMoney,
  fetchInvoice,
  fetchInvoiceDetailedRows,
  fetchInvoiceSummaryRows,
  moneyNumber,
  sumDetailedValueForPod,
  postBillingByScalesTariffRow,
  realizeCorrectionInvoices,
  realizeStandardForVolumesInvoice,
  runReg705MixedPodPrechain,
  splitDebitAndCredit,
  type Reg566Fx,
} from './reg-566-invoice-correction-board-cases.fixtures';

const JIRA_KEY = 'REG-705';
const JIRA_TITLE = 'Invoice Correction - volume Change flow | case 3 (half + full reversal)';

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
      'Precondition: scale SLP POD (BBS 10) + settlement POD (BBP 10 kWh) on one contract',
      async () => runReg705MixedPodPrechain(fx),
    );
    TestRunSummary.registerPayload('pods', {
      slp: pre.slpIdentifier,
      settlement: pre.settlementIdentifier,
    });

    const billed = await test.step(
      'Precondition: STANDARD FOR_VOLUMES billing run (one invoice for both PODs)',
      async () => realizeStandardForVolumesInvoice(fx),
    );
    TestRunSummary.registerPayload('originalInvoice', billed);

    const originalExclVat = await test.step(
      'Assert: one original INVOICE covering both PODs',
      async () => {
        const original = await fetchInvoice(Request, billed.originalInvoiceId);
        expect(original.invoiceDocumentType).toBe('INVOICE');
        const expectedOriginal = REG704_CREDIT_EXCL_VAT + REG705_SETTLEMENT_ORIGINAL_EXCL_VAT;
        expectMoney(original.totalAmountExcludingVat, expectedOriginal, 'original combined excl-VAT');
        const originalRows = await fetchInvoiceSummaryRows(Request, billed.originalInvoiceId);
        expect(originalRows.length, 'original summary should have a row per POD/PC').toBeGreaterThanOrEqual(2);
        TestRunSummary.recordCheck({
          check: 'Original combined invoice',
          expectedResult: `One INVOICE excl-VAT ${expectedOriginal} (scale 200 + settlement 200)`,
          actualResult: `As expected — type=${original.invoiceDocumentType}, exclVat=${original.totalAmountExcludingVat}, summaryRows=${originalRows.length}`,
          passed: true,
        });
        return moneyNumber(original.totalAmountExcludingVat);
      },
    );

    await test.step('Post correction billing-by-scales on SLP POD only (correction=true, total 20)', async () => {
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
    });

    const correctionBillingRunId = await test.step(
      'Create volume-change-only INVOICE_CORRECTION billing run',
      async () => createCorrectionBillingRun(fx, 0, false, true),
    );

    const correctionIds = await test.step(
      'Start correction, generate PDF, complete accounting (2 invoices)',
      async () => realizeCorrectionInvoices(fx, 2, 1),
    );

    const pair = await test.step(
      'Assert: one debit with 2 summary rows + one credit 200',
      async () => {
        const split = await splitDebitAndCredit(Request, correctionIds);
        expect(split.debit.invoiceDocumentType).toBe('DEBIT_NOTE');
        expect(split.credit.invoiceDocumentType).toBe('CREDIT_NOTE');
        expectMoney(split.credit.totalAmountExcludingVat, REG704_CREDIT_EXCL_VAT, 'credit excl-VAT');

        const debitRows = await fetchInvoiceSummaryRows(Request, Number(split.debit.id));
        expect(
          debitRows.length,
          `Debit summary must have 2 rows (SLP full reversal + settlement half reversal). rows=${JSON.stringify(debitRows)}`,
        ).toBe(2);

        const expectedDebitTotal = REG704_DEBIT_EXCL_VAT + REG705_SETTLEMENT_ORIGINAL_EXCL_VAT;
        expectMoney(split.debit.totalAmountExcludingVat, expectedDebitTotal, 'debit combined excl-VAT');

        const debitDetail = await fetchInvoiceDetailedRows(Request, Number(split.debit.id));
        expectMoney(
          sumDetailedValueForPod(debitDetail, pre.slpIdentifier),
          REG704_DEBIT_EXCL_VAT,
          'debit detailed-data SLP POD',
        );
        expectMoney(
          sumDetailedValueForPod(debitDetail, pre.settlementIdentifier),
          REG705_SETTLEMENT_ORIGINAL_EXCL_VAT,
          'debit detailed-data settlement POD',
        );

        TestRunSummary.recordCheck({
          check: 'Mixed half+full reversal documents',
          expectedResult:
            'One DEBIT_NOTE with 2 summary rows (600 + original settlement 200) and matching detailed-data rows by POD; one CREDIT_NOTE 200',
          actualResult:
            `As expected — debit=${split.debit.totalAmountExcludingVat} summaryRows=${debitRows.length}; ` +
            `credit=${split.credit.totalAmountExcludingVat}; originalExclVat=${originalExclVat}; ` +
            `slpDetail=${sumDetailedValueForPod(debitDetail, pre.slpIdentifier)}; ` +
            `settlementDetail=${sumDetailedValueForPod(debitDetail, pre.settlementIdentifier)}`,
          passed: true,
        });
        return split;
      },
    );

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
          'dataByProfiles',
          'billingRun',
          'invoice',
        ],
        extraLinks: buildProductContractTabLinks(pre.contractId),
        snapshot: {
          originalInvoiceId: billed.originalInvoiceId,
          correctionBillingRunId,
          debitInvoiceId: pair.debit.id,
          creditInvoiceId: pair.credit.id,
          slpIdentifier: pre.slpIdentifier,
          settlementIdentifier: pre.settlementIdentifier,
        },
      });
    });
  });
});
