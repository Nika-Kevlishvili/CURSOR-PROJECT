/**
 * PDT-3219 — Reverse LPF document missing when liability has no contract billing group (bug-only).
 *
 * Flow (Jira reproduce, API fixtures):
 * 1. Customer + overdue manual liability with billingGroupId = null (no contract billing group)
 * 2. Pay source liability → due-date shift → LPF job generates LATE_PAYMENT_FINE
 * 3. POST /latePaymentFine/reverse/{id}
 * 4. Assert REVERSAL_OF_LATE_PAYMENT_FINE exists AND document/email attachment is present
 *
 * Asserts documented product behavior (Confluence: Generate Late Payment File Templates, page 219119675):
 * reverse must generate late-payment-fine document (PDF / email attachment).
 *
 * Against current Test code this assertion is expected to FAIL until the bug is fixed
 * (sendToMainCustomer looks up liability by reversal id → empty communication → empty email/fileResponse).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2459-payment-reverse-lpf-offset.spec.ts
 * - tests/cursor/pdt-2459-payment-reverse-lpf-offset.fixtures.ts
 * - tests/cursor/pdt-2187-payment-partner-export.fixtures.ts (setupLpfViaPaidManualLiability REG-1049)
 * - tests/receivableManagement/massOperationForBlocking/blockingLPF.spec.ts
 * - tests/cursor/PDT-3177-payment-package-mass-import-auto-lock.spec.ts (bug-only + finalizeTestRunSummary)
 *
 * Run on Test:
 *   $env:BASE_URL="https://testapps.energo-pro.bg/backend/phoenix-epres"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-3219-lpf-reverse-document-no-billing-group.spec.ts --project=main
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PDT_3219_KEY,
  PDT_3219_TITLE,
  assertPdt3219DownloadReversalAttachment,
  assertPdt3219ReversalDocumentGenerated,
  assertPdt3219ReversalTypeAndLink,
  buildPdt3219AttachmentSummary,
  getPdt3219LpfDocumentSnapshot,
  preparePdt3219LpfWithoutBillingGroup,
  reversePdt3219LatePaymentFine,
  setupPdt3219ReceivablesBase,
  type Pdt3219Fx,
} from './pdt-3219-lpf-reverse-document-no-billing-group.fixtures';
import { getPdt2459LpfDetails } from './pdt-2459-payment-reverse-lpf-offset.fixtures';

test.describe(`[${PDT_3219_KEY}]: ${PDT_3219_TITLE}`, {
  tag: ['@receivableManagement', '@latePaymentFine', '@pdt-3219', '@test'],
}, () => {
  test.describe.configure({ mode: 'serial' });

  test(`[${PDT_3219_KEY}]: ${PDT_3219_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(5 * 60 * 1000);

    const fx: Pdt3219Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: customer, collection channel, payment package, daily interest rate', async () => {
      await setupPdt3219ReceivablesBase(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
    });

    const { lpfRecord, overdueLiabilityId } = await test.step(
      'Precondition: paid overdue manual liability without billing group → LPF generated',
      async () => preparePdt3219LpfWithoutBillingGroup(fx),
    );
    const originalLpfId = Number(lpfRecord.id);
    const customerIdentifier = String(Responses.customer[0].identifier);
    Responses.latePaymentFine.push(lpfRecord);

    const { lpfAmount } = await test.step('Precondition: resolve original LPF amount', async () =>
      getPdt2459LpfDetails(fx, originalLpfId),
    );

    const originalDoc = await test.step(
      'Assert: original LPF is on no-billing-group path (contractBillingGroupId null)',
      async () => {
        const { snapshot } = await getPdt3219LpfDocumentSnapshot(fx, originalLpfId);
        expect(
          snapshot.contractBillingGroupId,
          'Original LPF must have null contractBillingGroupId (PDT-3219 failing path)',
        ).toBeNull();

        TestRunSummary.recordCheck({
          check: 'Original LPF has no contract billing group',
          expectedResult: 'contractBillingGroupId is null on LATE_PAYMENT_FINE (manual liability path).',
          actualResult: `As expected — LPF ${originalLpfId} contractBillingGroupId=${snapshot.contractBillingGroupId}.`,
          passed: true,
        });

        TestRunSummary.registerPayload('latePaymentFine', {
          id: originalLpfId,
          amount: lpfAmount,
          overdueLiabilityId,
          contractBillingGroupId: snapshot.contractBillingGroupId,
          document: snapshot,
        });

        return snapshot;
      },
    );

    const reversalLpfId = await test.step(
      'Action: reverse LPF via POST /latePaymentFine/reverse/{id}',
      async () => reversePdt3219LatePaymentFine(fx, originalLpfId),
    );

    await test.step('Assert: REVERSAL_OF_LATE_PAYMENT_FINE created and linked to original', async () => {
      await assertPdt3219ReversalTypeAndLink(fx, originalLpfId, reversalLpfId, lpfAmount);
      TestRunSummary.recordCheck({
        check: 'LPF reverse creates reversal record',
        expectedResult: 'Reverse returns new REVERSAL_OF_LATE_PAYMENT_FINE linked to original LPF.',
        actualResult: `As expected — reversal LPF id=${reversalLpfId}, amount≈${-lpfAmount}.`,
        passed: true,
      });
    });

    const reversalDoc = await test.step(
      'Assert: reversal LPF has generated document/email attachment (product expected — fails while PDT-3219 open)',
      async () => {
        const { snapshot } = await getPdt3219LpfDocumentSnapshot(fx, reversalLpfId);

        let assertionPassed = true;
        let assertionError: string | undefined;
        try {
          assertPdt3219ReversalDocumentGenerated(snapshot, reversalLpfId);
        } catch (err) {
          assertionPassed = false;
          assertionError = err instanceof Error ? err.message : String(err);
          throw err;
        } finally {
          TestRunSummary.recordCheck({
            check: 'Reversal LPF document/email generated',
            expectedResult:
              'GET /latePaymentFine/{reversalId} has non-empty fileResponse and communicationShortResponse (Confluence Late Payment File Templates).',
            actualResult: assertionPassed
              ? `As expected — fileResponseCount=${snapshot.fileResponseCount}, communicationCount=${snapshot.communicationCount}.`
              : `Not as expected — fileResponseCount=${snapshot.fileResponseCount}, communicationCount=${snapshot.communicationCount}, fileUrl=${snapshot.fileUrl}. ${assertionError ?? ''}`.trim(),
            passed: assertionPassed,
          });
        }

        if (snapshot.firstFileId !== null) {
          await assertPdt3219DownloadReversalAttachment(fx, snapshot.firstFileId);
          TestRunSummary.recordCheck({
            check: 'Reversal attachment downloadable',
            expectedResult: 'GET /latePaymentFine/download-file/{fileId} returns non-empty binary.',
            actualResult: `As expected — downloaded file id=${snapshot.firstFileId}.`,
            passed: true,
          });
        }

        return snapshot;
      },
    );

    const result = {
      originalLpfId,
      reversalLpfId,
      overdueLiabilityId,
      lpfAmount,
      customerIdentifier,
      originalDoc,
      reversalDoc,
    };

    test.info().attach('[PDT-3219] LPF reverse document assertion summary', {
      body: JSON.stringify(buildPdt3219AttachmentSummary(result), null, 2),
      contentType: 'application/json',
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3219_KEY,
        relevantEntityKeys: ['customer', 'customerLiability', 'payment', 'latePaymentFine', 'interestRate'],
        snapshot: {
          originalLpfId,
          reversalLpfId,
          overdueLiabilityId,
          lpfAmount,
          customerIdentifier,
          originalFileResponseCount: originalDoc.fileResponseCount,
          reversalFileResponseCount: reversalDoc.fileResponseCount,
          reversalCommunicationCount: reversalDoc.communicationCount,
        },
      });
    });
  });
});
