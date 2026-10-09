/**
 * PDT-2913 — Missing email object for customers from reminder for disconnection (bug-only fix verification).
 *
 * Goal: after Power Supply Disconnection Reminder (RFD) communication generation,
 * email objects MUST exist when liability communication is resolved via the
 * MANUAL + billing-group SQL branch (CASE 1) — i.e. NOT via invoice_id /
 * outgoing_document_type IN ('INVOICE','DEBIT_NOTE').
 *
 * Flow:
 * 1. Precondition: customer/contract/POD + manual liability (creationType MANUAL,
 *    outgoingDocumentType not INVOICE/DEBIT_NOTE, no invoice, billingGroup set)
 * 2. Action: create RFD (EMAIL) + overdue known liability + set-customer-send-time + job → EXECUTED
 * 3. Assert: GET /email-communication/list returns ≥1 email for the customer
 *
 * Reference spec(s):
 * - tests/receivableManagement/reminderForDisconnection/generateCommunicationObjects.spec.ts (REG-1011)
 * - tests/cursor/pdt-3171-easypay-proforma-due-date-validto.fixtures.ts (createManualLiability)
 * - tests/cursor/PDT-3219-lpf-reverse-document-no-billing-group.spec.ts (bug-only + TestRunSummary)
 *
 * Run on Dev2 (parent via energo-ts-run):
 *   $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix-dev2'
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-2913-missing-email-object-reminder-for-disconnection.spec.ts --project=main
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PDT_2913_KEY,
  PDT_2913_TITLE,
  assertPdt2913EmailObjectsCreated,
  assertPdt2913ManualNonInvoiceLiability,
  buildPdt2913AttachmentSummary,
  createPdt2913ManualLiability,
  createPdt2913ReminderForDisconnection,
  executePdt2913ReminderCommunicationForLiability,
  setupPdt2913ContractChain,
  type Pdt2913Fx,
} from './pdt-2913-missing-email-object-reminder-for-disconnection.fixtures';

test.describe(`[${PDT_2913_KEY}]: ${PDT_2913_TITLE}`, {
  tag: ['@receivableManagement', '@reminderForDisconnection', '@pdt-2913', '@dev2'],
}, () => {
  test(`[${PDT_2913_KEY}]: ${PDT_2913_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(12 * 60 * 1000);

    const fx: Pdt2913Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: customer, term, POD, product, contract, POD activation', async () => {
      await setupPdt2913ContractChain(fx);
      TestRunSummary.registerPayload('customer', {
        id: Responses.customer[0].id,
        identifier: Responses.customer[0].identifier,
      });
      TestRunSummary.registerPayload('productContract', {
        id: Responses.productContract[0].id,
      });
    });

    const liabilityId = await test.step(
      'Precondition: create MANUAL customer liability with billing group (non-invoice path)',
      async () => {
        const id = await createPdt2913ManualLiability(fx);
        TestRunSummary.registerPayload('customerLiability', { id, path: 'MANUAL_with_billingGroup' });
        return id;
      },
    );

    const liability = await test.step(
      'Precondition: assert MANUAL non-INVOICE liability (SQL CASE 1 path)',
      async () => {
        const snapshot = await assertPdt2913ManualNonInvoiceLiability(fx, liabilityId);
        TestRunSummary.registerPayload('customerLiability', snapshot);
        TestRunSummary.recordCheck({
          check: 'Manual non-invoice liability ready for RFD',
          expectedResult:
            "creationType MANUAL, invoiceResponse null, outgoingDocumentType not INVOICE/DEBIT_NOTE, " +
            'billingGroup present so RFD resolves communication via CASE (1) without invoice branch.',
          actualResult:
            `As expected — liabilityId=${snapshot.liabilityId}, creationType=${snapshot.creationType}, ` +
            `outgoingDocumentType=${snapshot.outgoingDocumentType}, invoiceResponse.id=${snapshot.invoiceId}, ` +
            `billingGroupResponse.id=${snapshot.contractBillingGroupId}.`,
          passed: true,
        });
        return snapshot;
      },
    );

    const { reminderId, payload: rfdPayload } = await test.step(
      'Action: create Power Supply Disconnection Reminder (EMAIL)',
      async () => {
        const created = await createPdt2913ReminderForDisconnection(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', {
          id: created.reminderId,
          ...created.payload,
        });
        return created;
      },
    );

    await test.step(
      'Action: overdue manual liability + set send time + run RFD job until EXECUTED',
      async () => {
        await executePdt2913ReminderCommunicationForLiability(fx, liabilityId);
        TestRunSummary.recordCheck({
          check: 'RFD job reaches EXECUTED (no invoice dependency)',
          expectedResult:
            'due-date-change on known liabilityId + set-customer-send-time + job → reminderStatus EXECUTED ' +
            '(does not use Responses.invoice / offsetReminderForDisconnectionTime).',
          actualResult: `As expected — reminder ${reminderId} executed for liability ${liabilityId}.`,
          passed: true,
        });
      },
    );

    const emails = await test.step(
      'Assert: email communication object(s) created for RFD customer (fix works)',
      async () => {
        const customerIdentifier = String(Responses.customer[0].identifier);
        let assertionPassed = true;
        let assertionError: string | undefined;
        let found: Awaited<ReturnType<typeof assertPdt2913EmailObjectsCreated>> = [];

        try {
          found = await assertPdt2913EmailObjectsCreated(fx, customerIdentifier);
          expect(
            found.length,
            `Expected ≥1 email for customer ${customerIdentifier} after RFD (PDT-2913)`,
          ).toBeGreaterThan(0);
        } catch (err) {
          assertionPassed = false;
          assertionError = err instanceof Error ? err.message : String(err);
          throw err;
        } finally {
          TestRunSummary.recordCheck({
            check: 'RFD creates email object for MANUAL non-invoice customer',
            expectedResult:
              'GET /email-communication/list (CUSTOMER_IDENTIFIER + RFD topic) returns ≥1 email. ' +
              'Pass = communication resolved via MANUAL + billing group (CASE 1).',
            actualResult: assertionPassed
              ? `As expected — emailCount=${found.length}, firstId=${found[0]?.massOrIndemailCommunicationId}.`
              : `Not as expected — no RFD email for customer ${customerIdentifier}. ${assertionError ?? ''}`.trim(),
            passed: assertionPassed,
          });
        }

        return found;
      },
    );

    const customerIdentifier = String(Responses.customer[0].identifier);
    const summary = buildPdt2913AttachmentSummary({
      customerIdentifier,
      liability,
      reminderId,
      emailCount: emails.length,
      emails,
    });

    test.info().attach('[PDT-2913] RFD email fix-verification summary', {
      body: JSON.stringify({ ...summary, rfdPayload }, null, 2),
      contentType: 'application/json',
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_2913_KEY,
        relevantEntityKeys: [
          'customer',
          'productContract',
          'customerLiability',
          'reminderForDisconnection',
          'email',
        ],
        snapshot: {
          customerIdentifier,
          liabilityId: liability.liabilityId,
          creationType: liability.creationType,
          outgoingDocumentType: liability.outgoingDocumentType,
          invoiceId: liability.invoiceId,
          contractBillingGroupId: liability.contractBillingGroupId,
          reminderId,
          emailCount: emails.length,
          path: 'MANUAL_liability_with_billing_group_non_invoice',
        },
      });
    });
  });
});
