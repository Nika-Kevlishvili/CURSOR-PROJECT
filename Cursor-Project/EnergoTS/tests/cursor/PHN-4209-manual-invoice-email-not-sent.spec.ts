/**
 * PHN-4209 — phase 2 handling kept when PDT-3063 / PDT-3145 is merged to Dev2.
 *
 * If invoice email sending is started from communication data that has no email
 * address, the email object is still created with status NOT_SENT.
 *
 * Spec: Confluence "Handling Missed communication Data" (page 535855105),
 * process "When invoice is created manually".
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3063-manual-invoice-communication-data.spec.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - tests/cursor/pdt-2913-missing-email-object-reminder-for-disconnection.fixtures.ts
 *
 * Swagger (Dev2 live): GET /email-communication/{id}?type=EMAIL
 * emailCommunicationStatus includes NOT_SENT.
 */
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  createManualInvoiceWithCommunication,
  setupAltRecipientBillingGroup,
  type Pdt3063Fx,
} from './pdt-3063-manual-invoice-communication-data.fixtures';
import { startAndCompleteBillingRun } from './rps-pod-invoice-due-date.fixtures';

const TIMEOUT_MS = 8 * 60_000;

test.describe('[PHN-4209]: Manual invoice email without an address stays not sent', {
  tag: ['@billing', '@dev2', '@phn-4209'],
}, () => {
  test('[PHN-4209]: technical task for phase 1 and phase 2 merge (manual invoice email flow)', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx: Pdt3063Fx = { Request, GeneratePayload, Responses, Endpoints };
    const setup = await setupAltRecipientBillingGroup(fx, { selectedWithoutEmail: true });

    const billingRunId = await test.step('Precondition: manual invoice uses KOMM1, which has no email address', async () => {
      return createManualInvoiceWithCommunication(fx, setup, setup.selectedCommunicationId, 'EMAIL');
    });

    await test.step('Action: complete the billing run so invoice email sending starts', async () => {
      await startAndCompleteBillingRun(Request, billingRunId);
    });

    let emailId = 0;
    let status = '';
    await test.step('Assert: email object exists with status NOT_SENT', async () => {
      await expect.poll(async () => {
        const listRes = await Request.get(
          `${Endpoints.email}/list?page=0&size=25&prompt=${encodeURIComponent(setup.altCustomerIdentifier)}&searchBy=CUSTOMER_IDENTIFIER`,
        );
        if (!listRes.ok()) return '';
        const body = (await listRes.json()) as {
          content?: Array<{ id?: number; massOrIndemailCommunicationId?: number; emailCommunicationStatus?: string }>;
        };
        const row = (body.content ?? [])[0];
        emailId = Number(row?.massOrIndemailCommunicationId ?? row?.id ?? 0);
        if (!emailId) return '';
        const preview = await Request.get(`${Endpoints.email}/${emailId}?type=EMAIL`);
        if (!preview.ok()) return '';
        const details = (await preview.json()) as { emailCommunicationStatus?: string };
        status = String(details.emailCommunicationStatus ?? '');
        return status;
      }, {
        timeout: 120_000,
        intervals: [3000, 5000, 8000],
      }).toBe('NOT_SENT');
    });

    TestRunSummary.registerPayload('billingRun', { billingRunId, communicationId: setup.selectedCommunicationId });
    TestRunSummary.recordCheck({
      check: 'Missing email address still creates an email object in NOT_SENT',
      expectedResult:
        'Sending an invoice by email from communication data with no email address creates the email object with status NOT_SENT.',
      actualResult: status === 'NOT_SENT'
        ? `As expected — email ${emailId} status ${status} for customer ${setup.altCustomerIdentifier}.`
        : `Not as expected — email ${emailId || '(none)'} status ${status || '(empty)'} for customer ${setup.altCustomerIdentifier}.`,
      passed: status === 'NOT_SENT',
    });
    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: 'PHN-4209',
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'email'],
        snapshot: { billingRunId, emailId, status, altCustomerIdentifier: setup.altCustomerIdentifier },
      });
    });
  });
});
