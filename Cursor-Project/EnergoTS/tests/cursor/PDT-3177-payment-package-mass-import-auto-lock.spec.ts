/**
 * PDT-3177 — Payment Packages from mass import are not automatically locked (bug-only).
 *
 * Asserts documented expected behavior (Confluence Payment package Create): after offline/bank
 * mass-import creates payment(s), the auto-created package `lockStatus` must be LOCKED.
 *
 * Against current Test code this assertion is expected to FAIL until the bug is fixed
 * (warnings mark records unsuccessful → delete path instead of lock → package stays UNLOCKED).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts
 * - tests/receivableManagement/Payment/offlinePaymentCreateAndOffsetting.spec.ts
 * - tests/receivableManagement/paymentPackage.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProcessPreviewLink,
  resolveFrontendBaseUrlOverride,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3177_KEY,
  runOfflineBankPaymentMassImport,
  type Pdt3177Fx,
} from './pdt-3177-payment-package-mass-import-auto-lock.fixtures';

const JIRA_TITLE = 'Payment Packages from mass import are not automatically locked';

test.describe(`[${PDT_3177_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@receivableManagement', '@payment', '@massImport', '@pdt-3177', '@test'],
}, () => {
  test(`[${PDT_3177_KEY}]: ${JIRA_TITLE}`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(15 * 60 * 1000);
    const fx: Pdt3177Fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

    const result = await test.step(
      'Precondition + Action: OFFLINE channel + bank payment mass import (warning-bearing :86:)',
      async () => runOfflineBankPaymentMassImport(fx),
    );

    TestRunSummary.registerPayload('collectionChannel', {
      id: result.collectionChannelId,
      type: 'OFFLINE',
      massImport: 'PAYMENT bank file',
    });
    TestRunSummary.registerPayload('paymentPackage', {
      id: result.paymentPackageId,
      paymentDate: result.paymentDate,
      paymentCount: result.paymentCount,
    });

    await test.step(
      'Assert: payment package lockStatus is LOCKED after import payments are created',
      async () => {
        const locked = result.lockStatus === 'LOCKED';
        TestRunSummary.recordCheck({
          check: 'Auto-lock payment package after mass-import payments',
          expectedResult:
            'After offline/bank payment mass import creates payment(s), package lockStatus must be LOCKED ' +
            '(Confluence Payment package Create). Intentional fail against current Test until PDT-3177 is fixed.',
          actualResult: locked
            ? `As expected — paymentPackage ${result.paymentPackageId} lockStatus=LOCKED ` +
              `(payments=${result.paymentCount}, process=${result.processId}).`
            : `Not as expected — paymentPackage ${result.paymentPackageId} lockStatus=${result.lockStatus} ` +
              `with ${result.paymentCount} payment(s) (process=${result.processId}). ` +
              'Matches PDT-3177: warnings → success=false → lock skipped / delete path → package stays UNLOCKED.',
          passed: locked,
        });

        expect(
          result.lockStatus,
          `Payment package ${result.paymentPackageId} must be LOCKED after mass import created ` +
            `${result.paymentCount} payment(s). Current Test may leave UNLOCKED until PDT-3177 is fixed.`,
        ).toBe('LOCKED');
      },
    );

    await test.step('Attach test run summary', async () => {
      const frontendBase = resolveFrontendBaseUrlOverride();
      const processLink = buildProcessPreviewLink(result.processId, frontendBase);
      const packagePreview =
        frontendBase != null
          ? `${frontendBase.endsWith('/') ? frontendBase : `${frontendBase}/`}payment-package/preview?id=${result.paymentPackageId}`
          : null;

      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3177_KEY,
        relevantEntityKeys: ['collectionChannel', 'paymentPackage'],
        extraLinks: {
          ...(processLink ? { process: [processLink] } : {}),
          ...(packagePreview ? { paymentPackage: [packagePreview] } : {}),
        },
        snapshot: {
          testCase: JIRA_TITLE,
          processId: result.processId,
          collectionChannelId: result.collectionChannelId,
          paymentPackageId: result.paymentPackageId,
          paymentDate: result.paymentDate,
          paymentCount: result.paymentCount,
          lockStatus: result.lockStatus,
          note:
            'Asserts documented LOCKED expectation; may fail on Test until mixed-warning auto-lock bug is fixed.',
        },
      });
    });
  });
});
