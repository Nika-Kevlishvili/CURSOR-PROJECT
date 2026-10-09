/**
 * CURSOR-BUG — Dev FOR_VOLUMES reproduction of settlement OGNL
 * `target is null for method doubleValue` (PreProd billing 4057 pattern).
 *
 * This test asserts the current defect: billing reaches DRAFT and the BILLING
 * error report contains the OGNL message. Isolated 15-minute price parameters
 * are used; shared master prices 1001/1064 are not changed. BBP is a full
 * 15-minute series (CET); only price parameter B has a 4-interval gap.
 *
 * Reference spec(s):
 * - tests/cursor/CURSOR-BUG-settlement-missing-price-param-doubleValue.fixtures.ts
 * - tests/cursor/dev-volume-billing-two-compensations.spec.ts
 * - tests/billing/forVolumes/SLP.spec.ts
 *
 * Swagger: Cursor-Project/config/swagger/dev/swagger-spec.json (refreshed this session).
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  BILLING_TIMEOUT_MS,
  JIRA_KEY,
  JIRA_TITLE,
  SETTLEMENT_ERROR_NEEDLE,
  SETTLEMENT_ERROR_PREFIX,
  createForVolumesBillingRunAndStartDraft,
  downloadBillingErrorReportText,
  runCursorBugPrechain,
} from './CURSOR-BUG-settlement-missing-price-param-doubleValue.fixtures';

test.describe(`[${JIRA_KEY}]: ${JIRA_TITLE}`, { tag: ['@billing', '@cursor-bug'] }, () => {
  test(`[${JIRA_KEY}]: ${JIRA_TITLE}`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

    const pre = await test.step(
      'Precondition: customer → terms → 15-min PP A/B → settlement PC → product → POD → contract → activate → BBP',
      async () => runCursorBugPrechain(fx),
    );

    TestRunSummary.registerPayload('customer', Responses.customer[0]);
    TestRunSummary.registerPayload('priceParameters', {
      fullPriceParameterId: pre.fullPriceParameterId,
      gappedPriceParameterId: pre.gappedPriceParameterId,
    });
    TestRunSummary.registerPayload('priceComponent', {
      id: pre.priceComponentId,
      expression: `$${pre.fullPriceParameterId}$+$${pre.gappedPriceParameterId}$`,
    });
    TestRunSummary.registerPayload('productContract', Responses.productContract[0]);

    const billing = await test.step(
      'Create FOR_VOLUMES billing run and PATCH start-billing until DRAFT',
      async () => createForVolumesBillingRunAndStartDraft(fx),
    );

    TestRunSummary.registerPayload('billingRun', {
      id: billing.billingRunId,
      status: billing.billingRunStatus,
    });

    const errorReportText = await test.step(
      'PATCH /billing-run/download-error-report/{id}?protocol=BILLING',
      async () => downloadBillingErrorReportText(Request, billing.billingRunId),
    );

    await test.step('Assert: DRAFT run and settlement OGNL doubleValue error', async () => {
      expect(billing.billingRunStatus, 'billing run must reach DRAFT after start-billing').toBe(
        'DRAFT',
      );
      expect(
        errorReportText.toLowerCase(),
        `BILLING error report must contain "${SETTLEMENT_ERROR_PREFIX}"`,
      ).toContain(SETTLEMENT_ERROR_PREFIX);
      expect(
        errorReportText,
        `BILLING error report must contain "${SETTLEMENT_ERROR_NEEDLE}"`,
      ).toContain(SETTLEMENT_ERROR_NEEDLE);

      TestRunSummary.recordCheck({
        check: 'Missing 15-minute price parameter triggers settlement OGNL error',
        expectedResult:
          'FOR_VOLUMES start-billing finishes DRAFT; BILLING error report contains settlement evaluation error and target is null for method doubleValue.',
        actualResult: `As expected — status=${billing.billingRunStatus}, billingRunId=${billing.billingRunId}, errorNeedleFound=${errorReportText.includes(SETTLEMENT_ERROR_NEEDLE)}.`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      const extraLinks: Record<string, string[]> = {
        ...buildProductContractTabLinks(pre.contractId),
      };
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'pod', 'product', 'productContract', 'billingRun'],
        extraLinks: Object.keys(extraLinks).length ? extraLinks : undefined,
        snapshot: {
          customerId: pre.customerId,
          podId: pre.podId,
          contractId: pre.contractId,
          productId: pre.productId,
          fullPriceParameterId: pre.fullPriceParameterId,
          gappedPriceParameterId: pre.gappedPriceParameterId,
          priceComponentId: pre.priceComponentId,
          bbpId: pre.bbpId,
          billingRunId: billing.billingRunId,
          billingRunStatus: billing.billingRunStatus,
        },
      });
    });
  });
});
