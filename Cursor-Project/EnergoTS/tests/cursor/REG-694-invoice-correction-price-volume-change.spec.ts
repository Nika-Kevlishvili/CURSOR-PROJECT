/**
 * REG-694 — Invoice Correction combined price + volume change (happy path).
 *
 * Jira expected: standard excl-VAT 10_000; after price 100→200 and profile 100→200,
 * debit note 40_000 and credit note 10_000.
 *
 * Reference spec(s):
 * - tests/cursor/reg-566-invoice-correction-board-cases.fixtures.ts
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts
 * - tests/billing/correction/correctionCases.spec.ts
 *
 * Swagger: InvoiceCorrectionParameters.priceChange + volumeChange, InvoiceResponse.
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  REG694_CREDIT_EXCL_VAT,
  REG694_DEBIT_EXCL_VAT,
  REG694_PRICE_NEW,
  REG694_VOLUME_NEW,
  createCorrectionBillingRun,
  editSettlementPriceComponentExpression,
  expectMoney,
  fetchInvoice,
  postBillingByProfile,
  realizeCorrectionInvoices,
  realizeStandardForVolumesInvoice,
  runReg694SettlementPrechain,
  splitDebitAndCredit,
  type Reg566Fx,
} from './reg-566-invoice-correction-board-cases.fixtures';

const JIRA_KEY = 'REG-694';
const JIRA_TITLE = 'Invoice Correction - Price + volume Change flow | case 1 (happy pass)';

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
      'Precondition: legal customer → settlement PC 100 → term → settlement POD → product → contract → activate → BBP 100 kWh',
      async () => runReg694SettlementPrechain(fx),
    );
    TestRunSummary.registerPayload('priceComponent', {
      id: pre.priceComponentId,
      originalExpression: '100',
    });
    TestRunSummary.registerPayload('billingByProfileOriginal', { kwh: 100, pod: pre.podIdentifier });

    const billed = await test.step(
      'Precondition: STANDARD FOR_VOLUMES billing run, generate PDF, start accounting',
      async () => realizeStandardForVolumesInvoice(fx),
    );
    TestRunSummary.registerPayload('originalInvoice', billed);

    await test.step('Assert: original invoice is INVOICE excl-VAT 10 000', async () => {
      const original = await fetchInvoice(Request, billed.originalInvoiceId);
      expect(original.invoiceDocumentType).toBe('INVOICE');
      expectMoney(original.totalAmountExcludingVat, REG694_CREDIT_EXCL_VAT, 'original excl-VAT');
      TestRunSummary.recordCheck({
        check: 'Standard invoice excl-VAT',
        expectedResult: 'Document type INVOICE; amount excluding VAT = 100 × 100 = 10 000',
        actualResult: `As expected — type=${original.invoiceDocumentType}, exclVat=${original.totalAmountExcludingVat}, id=${billed.originalInvoiceId}`,
        passed: true,
      });
    });

    await test.step('Edit settlement price component 100 → 200', async () => {
      await editSettlementPriceComponentExpression(fx, pre.priceComponentId, String(REG694_PRICE_NEW));
      TestRunSummary.registerPayload('priceComponentEdited', { expression: String(REG694_PRICE_NEW) });
    });

    await test.step('Add replacement billing-by-profile 200 kWh on the same POD', async () => {
      await postBillingByProfile(fx, 0, REG694_VOLUME_NEW);
      TestRunSummary.registerPayload('billingByProfileCorrection', { kwh: REG694_VOLUME_NEW });
    });

    const correctionBillingRunId = await test.step(
      'Create INVOICE_CORRECTION billing run (priceChange + volumeChange)',
      async () => createCorrectionBillingRun(fx, 0, true, true),
    );

    const correctionIds = await test.step(
      'Start correction, generate PDF, complete accounting (2 invoices)',
      async () => realizeCorrectionInvoices(fx, 2, 1),
    );

    const pair = await test.step('Assert: debit 40 000 and credit 10 000', async () => {
      const split = await splitDebitAndCredit(Request, correctionIds);
      expectMoney(split.debit.totalAmountExcludingVat, REG694_DEBIT_EXCL_VAT, 'debit excl-VAT');
      expectMoney(split.credit.totalAmountExcludingVat, REG694_CREDIT_EXCL_VAT, 'credit excl-VAT');
      TestRunSummary.recordCheck({
        check: 'Correction debit/credit amounts',
        expectedResult:
          'Debit note excl-VAT 200 × 200 = 40 000; credit note excl-VAT 100 × 100 = 10 000',
        actualResult:
          `As expected — debit=${split.debit.totalAmountExcludingVat} (${split.debit.invoiceDocumentType}), ` +
          `credit=${split.credit.totalAmountExcludingVat} (${split.credit.invoiceDocumentType})`,
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
          debitExclVat: pair.debit.totalAmountExcludingVat,
          creditExclVat: pair.credit.totalAmountExcludingVat,
        },
      });
    });
  });
});
