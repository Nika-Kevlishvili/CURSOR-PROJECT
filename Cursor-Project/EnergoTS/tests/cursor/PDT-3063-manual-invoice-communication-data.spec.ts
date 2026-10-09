/**
 * PDT-3063 and internal bugs — invoice communication data for manual invoice
 * and manual interim when the billing group has an alternative recipient.
 *
 * Expected behavior on Dev2 after the PDT-3063 merge (origin/dev2, live OpenAPI):
 * the list is the alternative recipient's billing communications that have an
 * email and a mobile number, customerId/customerVersion identify that recipient,
 * and the billing-group communication is defaultSelected.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 * - tests/cursor/reg-1172-volume-with-electricity.fixtures.ts
 */
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  createManualInterimWithCommunication,
  createManualInvoiceWithCommunication,
  listInvoiceCommunicationData,
  readSavedInvoiceCommunicationId,
  setupAltRecipientBillingGroup,
  type Pdt3063Fx,
} from './pdt-3063-manual-invoice-communication-data.fixtures';

const TIMEOUT_MS = 240_000;

test.describe('[PDT-3063]: Manual invoice alternative recipient communication data', {
  tag: ['@billing', '@dev2', '@pdt-3063'],
}, () => {
  test('[PDT-3063]: Billing run - Manual invoice and Manual Interim - Incorrect Invoice communication data', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx: Pdt3063Fx = { Request, GeneratePayload, Responses, Endpoints };
    const setup = await setupAltRecipientBillingGroup(fx);
    const rows = await listInvoiceCommunicationData(fx, setup);
    const ids = rows.map((row) => Number(row.id)).filter((id) => id > 0);
    const containsSelected = ids.includes(setup.selectedCommunicationId);
    TestRunSummary.registerPayload('billingGroup', {
      billingGroupId: setup.billingGroupId,
      alternativeRecipientCustomerDetailId: setup.altCustomerDetailId,
      billingCustomerCommunicationId: setup.selectedCommunicationId,
    });
    TestRunSummary.recordCheck({
      check: 'Invoice communication list contains the communication selected on the alternative recipient',
      expectedResult:
        'When the billing group has an alternative recipient, invoice communication data includes the communication selected on that recipient (KOMM1).',
      actualResult: containsSelected
        ? `As expected — list ids ${ids.join(', ')} include selected ${setup.selectedCommunicationId}.`
        : `Not as expected — list ids ${ids.join(', ') || '(empty)'} do not include selected ${setup.selectedCommunicationId}.`,
      passed: containsSelected,
    });
    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: 'PDT-3063',
        relevantEntityKeys: ['customer', 'productContract', 'billingRun'],
        snapshot: { ...setup, listedIds: ids },
      });
    });
    expect(containsSelected, `listed communication ids: ${ids.join(', ')}`).toBe(true);
  });

  test('[PDT-3170]: [Backend] Billing run - Manual invoice - Incorrect Invoice communication data - It is not able to open Alternative recipient customer', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx: Pdt3063Fx = { Request, GeneratePayload, Responses, Endpoints };
    const setup = await setupAltRecipientBillingGroup(fx);
    const rows = await listInvoiceCommunicationData(fx, setup);
    const withIdentity = rows.filter(
      (row) => Number(row.customerId) === setup.altCustomerId && Number(row.customerVersion) > 0,
    );
    const passed = rows.length > 0 && withIdentity.length === rows.length;
    TestRunSummary.recordCheck({
      check: 'Communication list returns alternative recipient customerId and customerVersion',
      expectedResult:
        'Every invoice communication row identifies the alternative recipient via customerId and customerVersion so the UI can open that customer.',
      actualResult: passed
        ? `As expected — ${withIdentity.length} row(s) have customerId ${setup.altCustomerId}.`
        : `Not as expected — rows ${JSON.stringify(rows.map((row) => ({ id: row.id, customerId: row.customerId, customerVersion: row.customerVersion })))}.`,
      passed,
    });
    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: 'PDT-3170',
        relevantEntityKeys: ['customer', 'productContract'],
        snapshot: { altCustomerId: setup.altCustomerId, rows },
      });
    });
    expect(passed, JSON.stringify(rows)).toBe(true);
  });

  test('[PDT-3207]: [Backend]Billing run - Manual invoice - Incorrect Invoice communication data - Only selected communication data is loaded in Billing run object', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx: Pdt3063Fx = { Request, GeneratePayload, Responses, Endpoints };
    const setup = await setupAltRecipientBillingGroup(fx);
    const rows = await listInvoiceCommunicationData(fx, setup);
    const ids = rows.map((row) => Number(row.id));
    const containsBoth =
      ids.includes(setup.selectedCommunicationId) && ids.includes(setup.otherCommunicationId);
    const selectedMarked = rows.some(
      (row) => Number(row.id) === setup.selectedCommunicationId && row.defaultSelected === true,
    );
    const passed = containsBoth && selectedMarked;
    TestRunSummary.recordCheck({
      check: 'All alternative-recipient billing communications are listed and the billing-group selection is default',
      expectedResult:
        'Both billing communications of the alternative recipient are returned. The communication stored on the billing group is defaultSelected.',
      actualResult: passed
        ? `As expected — ids ${ids.join(', ')}; selected ${setup.selectedCommunicationId} is defaultSelected.`
        : `Not as expected — ids ${ids.join(', ')}; defaultSelected flags ${rows.map((row) => `${row.id}:${row.defaultSelected}`).join(', ')}.`,
      passed,
    });
    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: 'PDT-3207',
        relevantEntityKeys: ['customer', 'productContract'],
        snapshot: { ...setup, rows },
      });
    });
    expect(passed, JSON.stringify(rows)).toBe(true);
  });

  test('[PDT-3329]: [Backend] Billing run - Manual invoice - Incorrect Invoice communication data - After save object is not loaded', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx: Pdt3063Fx = { Request, GeneratePayload, Responses, Endpoints };
    const setup = await setupAltRecipientBillingGroup(fx);
    const billingRunId = await createManualInvoiceWithCommunication(
      fx,
      setup,
      setup.otherCommunicationId,
      'ACCORDING_TO_THE_CONTRACT',
    );
    const savedId = await readSavedInvoiceCommunicationId(fx, billingRunId);
    const passed = savedId === setup.otherCommunicationId;
    TestRunSummary.recordCheck({
      check: 'Manual invoice saves the alternative recipient communication without DomainEntityNotFoundException',
      expectedResult:
        'POST/PUT billing-run succeeds and the saved invoice communication id is the alternative recipient communication, not a contract-customer lookup error.',
      actualResult: passed
        ? `As expected — billing run ${billingRunId} saved communication ${savedId}.`
        : `Not as expected — billing run ${billingRunId} saved communication ${savedId}, wanted ${setup.otherCommunicationId}.`,
      passed,
    });
    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: 'PDT-3329',
        relevantEntityKeys: ['customer', 'productContract', 'billingRun'],
        snapshot: { billingRunId, savedId, wanted: setup.otherCommunicationId },
      });
    });
    expect(savedId, 'saved invoiceCommunicationDataId').toBe(setup.otherCommunicationId);
  });

  test('[PDT-3331]: [Backend] Billing run - Manual invoice - Incorrect Invoice communication data - Invoice communication data is not saved correctly', async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(TIMEOUT_MS);
    const fx: Pdt3063Fx = { Request, GeneratePayload, Responses, Endpoints };
    const setup = await setupAltRecipientBillingGroup(fx);
    const billingRunId = await createManualInterimWithCommunication(fx, setup, setup.otherCommunicationId);
    const savedId = await readSavedInvoiceCommunicationId(fx, billingRunId);
    const passed = savedId === setup.otherCommunicationId;
    TestRunSummary.recordCheck({
      check: 'Manual interim persists the communication chosen in the billing run, not the billing-group default',
      expectedResult:
        'Choosing the second alternative-recipient communication on a manual interim billing run stores that id.',
      actualResult: passed
        ? `As expected — billing run ${billingRunId} saved ${savedId}.`
        : `Not as expected — billing run ${billingRunId} saved ${savedId}, wanted ${setup.otherCommunicationId} (billing group default is ${setup.selectedCommunicationId}).`,
      passed,
    });
    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: 'PDT-3331',
        relevantEntityKeys: ['customer', 'productContract', 'billingRun'],
        snapshot: { billingRunId, savedId, wanted: setup.otherCommunicationId },
      });
    });
    expect(savedId, 'saved interim invoiceCommunicationDataId').toBe(setup.otherCommunicationId);
  });
});
