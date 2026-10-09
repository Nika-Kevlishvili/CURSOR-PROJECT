/**
 * [SLR-18167] — Dev2 FOR_VOLUMES replica of Test 2 SLR billing run 18167
 * (`BILLING202608200001`). Start-billing only (DRAFT). Expect ≥1 draft invoice.
 *
 * Run (EnergoTS root; current playwright.config.ts default BASE_URL is already Dev2):
 *   npx playwright test tests/cursor/SLR-18167-dev2-volumes-billing-replica.spec.ts
 *
 * Optional: BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 *
 * Do NOT edit playwright.config.ts.
 *
 * Reference spec(s):
 * - tests/billing/forVolumes/forVolumes.spec.ts
 * - tests/cursor/dev-volume-billing-two-compensations.spec.ts
 * - tests/cursor/pdt-3223-invoice-json-tablepcscales-listpc-order.fixtures.ts
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  SLR_18167_KEY,
  SLR_18167_TITLE,
  SLR_18167_PORTAL_BASE,
  SLR_18167_API_BASE,
  SLR_COLLIDING_CONTRACT_NUMBER,
  SLR_INVOICE_DATE,
  SLR_TAX_EVENT_DATE,
  SLR_MAX_END_DATE,
  buildSlr18167BillingRunPreviewLink,
  buildSlr18167CustomerPreviewLink,
  buildSlr18167InvoicePreviewLink,
  buildSlr18167PodPreviewLink,
  createSlr18167ForVolumesBillingRun,
  getDraftInvoiceIds,
  logSlr18167CreatedEntities,
  resolveSlr18167BillingDates,
  runSlr18167ContractScaleAndBbpPrechain,
  slr18167ZeroDraftInvoicesMessage,
  startBillingAndPollDraft,
  type Slr18167ScenarioResult,
} from './slr-18167-dev2-volumes-billing-replica.fixtures';

test.describe('[SLR-18167]: Dev2 volumes billing replica of SLR run 18167', {
  tag: ['@billing', '@dev-data', '@volumes'],
}, () => {
  test(`[SLR-18167]: ${SLR_18167_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(20 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

    const pre = await test.step(
      'Precondition: customer, SLP POD, COMBINED product+PCs+terms, contract, activate, Feb scale 6 kWh, Mar BBP 6 kWh',
      async () => runSlr18167ContractScaleAndBbpPrechain(fx),
    );

    expect(pre.contractNumber, 'new contract number assigned by API').toBeTruthy();
    expect(pre.contractNumber).not.toBe(SLR_COLLIDING_CONTRACT_NUMBER);
    expect(pre.priceComponentIds.length, '3 settlement + 3 scale PCs').toBe(6);

    TestRunSummary.registerPayload('prechain', {
      apiBase: SLR_18167_API_BASE,
      portalBase: SLR_18167_PORTAL_BASE,
      customerId: pre.customerId,
      customerIdentifier: pre.customerIdentifier,
      customerIdentifierPreferredUsed: pre.customerIdentifierPreferredUsed,
      podId: pre.podId,
      podIdentifier: pre.podIdentifier,
      podIdentifierPreferredUsed: pre.podIdentifierPreferredUsed,
      productId: pre.productId,
      contractId: pre.contractId,
      contractNumber: pre.contractNumber,
      collidingContractNumberAvoided: SLR_COLLIDING_CONTRACT_NUMBER,
      scaleDataId: pre.scaleDataId,
      bbpId: pre.bbpId,
      priceComponentIds: pre.priceComponentIds,
    });

    const dates = await test.step(
      'Resolve OPEN accounting period for July 2026 SLR dates (else keep generator dates)',
      async () => resolveSlr18167BillingDates(Request),
    );

    TestRunSummary.registerPayload('billingDates', dates);
    TestRunSummary.recordCheck({
      check: 'Billing invoice/tax dates vs OPEN accounting period',
      expectedResult:
        'Use SLR invoiceDate 2026-07-24 and taxEventDate 2026-07-23 when an OPEN period covers July 2026; otherwise keep generator dates (do not post into a closed period). maxEndDate stays 2026-04-30.',
      actualResult: dates.datesAdapted
        ? `Dates adapted — no OPEN period covering ${SLR_TAX_EVENT_DATE}..${SLR_INVOICE_DATE}; generator invoice/tax/accountingPeriod kept; maxEndDate=${dates.maxEndDate}.`
        : `As expected — OPEN period ${dates.openPeriodName ?? dates.accountingPeriodId} covers SLR dates; invoiceDate=${dates.invoiceDate}, taxEventDate=${dates.taxEventDate}, maxEndDate=${SLR_MAX_END_DATE}.`,
      passed: true,
    });

    const billing = await test.step(
      'Create STANDARD_BILLING FOR_VOLUMES run listing the new contract number',
      async () => createSlr18167ForVolumesBillingRun(fx, pre.contractNumber, dates),
    );

    TestRunSummary.registerPayload('billingRun', billing.billingPayload);

    const billingRunStatus = await test.step(
      'PATCH start-billing and poll until DRAFT (no generate/accounting)',
      async () => startBillingAndPollDraft(Request, billing.billingRunId),
    );

    expect(billingRunStatus, 'start-billing should finish DRAFT (IN_PROGRESS_DRAFT is polled through)').toBe(
      'DRAFT',
    );

    const draftInvoiceIds = await test.step('GET billing-run/draft-invoices', async () => {
      const ids = await getDraftInvoiceIds(Request, billing.billingRunId);
      for (const id of ids) {
        Responses.invoice.push(id);
      }
      return ids;
    });

    const hasDraft = draftInvoiceIds.length >= 1;
    TestRunSummary.recordCheck({
      check: 'Start-billing produces draft invoices (SLR 18167 had 0 — generation bug)',
      expectedResult: 'Billing run status DRAFT; draft-invoices length ≥ 1.',
      actualResult: hasDraft
        ? `As expected — status=${billingRunStatus}, draftInvoices=${draftInvoiceIds.length} (${draftInvoiceIds.join(',')}).`
        : `Not as expected — ${slr18167ZeroDraftInvoicesMessage()} status=${billingRunStatus}.`,
      passed: hasDraft,
    });

    const result: Slr18167ScenarioResult = {
      ...pre,
      billingRunId: billing.billingRunId,
      billingRunStatus,
      draftInvoiceIds,
      datesAdapted: dates.datesAdapted,
    };

    await test.step('Log created entity ids and portal preview links', async () => {
      logSlr18167CreatedEntities(result);
      test.info().attach('[SLR-18167] created entities', {
        body: JSON.stringify(result, null, 2),
        contentType: 'application/json',
      });
    });

    await test.step('Attach test run summary', async () => {
      const extraLinks: Record<string, string[]> = {
        ...buildProductContractTabLinks(pre.contractId, SLR_18167_PORTAL_BASE),
        customer: [buildSlr18167CustomerPreviewLink(pre.customerId)],
        pod: [buildSlr18167PodPreviewLink(pre.podId)],
        billingRun: [buildSlr18167BillingRunPreviewLink(billing.billingRunId)],
        invoice: draftInvoiceIds.map(buildSlr18167InvoicePreviewLink),
      };

      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: SLR_18167_KEY,
        relevantEntityKeys: ['customer', 'pod', 'product', 'productContract', 'billingRun', 'invoice'],
        extraLinks,
        snapshot: {
          ...result,
          portalBase: SLR_18167_PORTAL_BASE,
          apiBase: SLR_18167_API_BASE,
        },
      });
    });

    expect(draftInvoiceIds.length, slr18167ZeroDraftInvoicesMessage()).toBeGreaterThanOrEqual(1);
  });
});
