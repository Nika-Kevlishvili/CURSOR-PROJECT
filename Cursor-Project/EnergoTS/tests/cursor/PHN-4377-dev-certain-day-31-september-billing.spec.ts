/**
 * [PHN-4377] Reproduce standard billing "Invalid date 'SEPTEMBER 31'" on Dev.
 *
 * Locked to Dev via test.use. Does not follow BASE_URL and does not change the Dev2 spec.
 *
 *   npx playwright test tests/cursor/PHN-4377-dev-certain-day-31-september-billing.spec.ts --project=main
 *
 * Reference spec(s):
 * - tests/cursor/PHN-4377-certain-day-31-september-billing.spec.ts
 *
 * Swagger: Cursor-Project/config/swagger/dev/swagger-spec.json
 * CERTAIN_DAYS, ACCORDING_TO_THE_CONTRACT, start-billing, download-error-report.
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PHN_4377_API_BASE,
  PHN_4377_BUG_MESSAGE,
  PHN_4377_CERTAIN_DAY,
  PHN_4377_INVOICE_DATE,
  PHN_4377_KEY,
  PHN_4377_PORTAL_BASE,
  PHN_4377_TITLE,
  buildPhn4377Links,
  createPhn4377BillingRun,
  readContractPaymentTerm,
  refreshDevReferenceData,
  resolveSeptemberBillingDates,
  runPhn4377Prechain,
  startBillingAndCollectOutcome,
} from './phn-4377-dev-certain-day-31-september-billing.fixtures';

test.use({ baseURL: PHN_4377_API_BASE });

test.describe(`[${PHN_4377_KEY}]: Certain day 31 on a September invoice (Dev)`, {
  tag: ['@billing', '@dev', '@phn-4377'],
}, () => {
  test(`[${PHN_4377_KEY}]: ${PHN_4377_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(40 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

    await test.step('Precondition: rebuild Dev nomenclature and template ids', async () => {
      await refreshDevReferenceData();
    });

    const pre = await test.step(
      'Precondition: customer, COMBINED product, CERTAIN_DAYS 31 term, contract, Feb scale, Mar profile',
      async () => runPhn4377Prechain(fx),
    );

    const paymentTerm = await test.step('Read saved contract payment term', async () =>
      readContractPaymentTerm(Request, pre.contractId),
    );

    expect(paymentTerm.value, 'contract invoicePaymentTermValue').toBe(PHN_4377_CERTAIN_DAY);
    expect(paymentTerm.calendarType, 'term calendarType').toBe('CERTAIN_DAYS');

    const dates = await test.step(
      'Resolve an OPEN accounting period that covers 27-28 September 2026',
      async () => resolveSeptemberBillingDates(Request),
    );

    TestRunSummary.registerPayload('prechain', {
      apiBase: PHN_4377_API_BASE,
      portalBase: PHN_4377_PORTAL_BASE,
      ...pre,
      paymentTerm,
    });
    TestRunSummary.registerPayload('billingDates', dates);

    expect(
      dates.invoiceDate,
      `September 2026 must sit in an OPEN accounting period. Open periods: ${dates.openPeriods.join(' | ') || 'none'}`,
    ).toBe(PHN_4377_INVOICE_DATE);

    const billing = await createPhn4377BillingRun(fx, pre.contractNumber, dates);
    TestRunSummary.registerPayload('billingRun', {
      billingRunId: billing.billingRunId,
      invoiceDate: dates.invoiceDate,
      invoiceDueDate: 'ACCORDING_TO_THE_CONTRACT',
      certainDay: PHN_4377_CERTAIN_DAY,
    });

    const outcome = await test.step(
      'PATCH start-billing and read the billing error report',
      async () => startBillingAndCollectOutcome(Request, billing.billingRunId),
    );

    const bugReproduced = outcome.errorMessages.some((message) => message.includes(PHN_4377_BUG_MESSAGE));
    const links = buildPhn4377Links({
      customerId: pre.customerId,
      podId: pre.podId,
      contractId: pre.contractId,
      billingRunId: billing.billingRunId,
      productId: pre.productId,
    });

    const actualResult = bugReproduced
      ? `Bug reproduced on Dev. Billing run ${billing.billingRunId} status=${outcome.status}. Error: ${outcome.errorMessages.join(' | ')}`
      : `Bug not reproduced. status=${outcome.status}, draftInvoiceIds=${outcome.draftInvoiceIds.join(',') || 'none'}, errors=${outcome.errorMessages.join(' | ') || 'none'}`;

    console.log('\n========== PHN-4377 Dev reproduction ==========');
    console.log(JSON.stringify({ ...links, contractNumber: pre.contractNumber, ...outcome, bugReproduced }, null, 2));

    TestRunSummary.recordCheck({
      check: 'September invoice with certain day 31',
      expectedResult:
        'Same failure as Test 2 ES: billing error report contains Invalid date \'SEPTEMBER 31\' and no invoice is created.',
      actualResult,
      passed: bugReproduced,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_4377_KEY,
        relevantEntityKeys: ['customer', 'pod', 'product', 'productContract', 'billingRun'],
        extraLinks: {
          customer: [links.customer],
          pod: [links.pod],
          product: [links.product],
          productContract: [links.productContract],
          billingRun: [links.billingRun],
        },
        snapshot: {
          contractNumber: pre.contractNumber,
          billingRunId: billing.billingRunId,
          bugReproduced,
          outcome,
          links,
        },
      });
    });

    expect(bugReproduced, actualResult).toBe(true);
    expect(outcome.draftInvoiceIds, 'no invoice should be created when the slot date error fires').toEqual([]);
  });
});
