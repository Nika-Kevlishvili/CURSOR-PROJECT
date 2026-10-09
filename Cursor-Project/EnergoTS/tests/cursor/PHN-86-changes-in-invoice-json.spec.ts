/**
 * PHN-86 — Changes in invoice JSON.
 *
 * Uploads a new invoice DOCUMENT template that prints MostUsedPaymentChannel
 * and Preferences, bills an existing Dev2 customer who already has both, and
 * checks the invoice JSON plus the generated PDF.
 *
 * Reference spec(s):
 * - tests/cursor/dev-volume-invoice-custom-template.spec.ts
 * - tests/cursor/phn-86-changes-in-invoice-json.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PHN_86_KEY,
  PHN_86_TEMPLATE_DOCX_PATH,
  PHN_86_TITLE,
  assertPhn86RunsOnDev2,
  completePhn86BillingRun,
  createPhn86InvoiceTemplate,
  downloadPhn86InvoicePdf,
  fetchPhn86InvoiceDocumentModel,
  resolvePhn86ExistingCustomer,
  runPhn86VolumePrechain,
  sameStringSet,
} from './phn-86-changes-in-invoice-json.fixtures';

test.describe(`[${PHN_86_KEY}]: ${PHN_86_TITLE}`, { tag: '@billing' }, () => {
  test(`[${PHN_86_KEY}]: ${PHN_86_TITLE}`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    assertPhn86RunsOnDev2();
    const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

    const customer = await test.step(
      'Precondition: Dev2 customer that already has payment channel and preferences',
      async () => {
        const resolved = await resolvePhn86ExistingCustomer(Request);
        expect(resolved.paymentChannelName, 'customer paymentChannelName').not.toBe('');
        expect(resolved.preferenceNames.length, 'customer ACTIVE preferences').toBeGreaterThan(0);
        TestRunSummary.registerPayload('existingCustomer', resolved);
        return resolved;
      },
    );

    const template = await test.step(
      'Precondition: upload DOCX and create ACTIVE INVOICE DOCUMENT template',
      async () => {
        const created = await createPhn86InvoiceTemplate(fx);
        expect(created.templateId, 'PHN-86 invoice template id').toBeGreaterThan(0);
        TestRunSummary.registerPayload('invoiceDocumentTemplate', {
          templateId: created.templateId,
          templateName: created.templateName,
          fileId: created.fileId,
          sourcePath: PHN_86_TEMPLATE_DOCX_PATH,
          templateType: 'DOCUMENT',
          templatePurpose: 'INVOICE',
          templateStatus: 'ACTIVE',
          language: 'BULGARIAN',
          outputFileFormat: ['PDF'],
        });
        return created;
      },
    );

    const prechain = await test.step(
      'Precondition: product (custom INVOICE_TEMPLATE) → contract → POD → profile',
      async () => {
        const chain = await runPhn86VolumePrechain(fx, customer, template.templateId);
        TestRunSummary.registerPayload('prechain', {
          ...customer,
          ...chain,
          invoiceTemplateId: template.templateId,
        });
        return chain;
      },
    );

    const billing = await test.step(
      'Billing run FOR_VOLUMES through COMPLETED (invoice + PDF)',
      async () => completePhn86BillingRun(fx),
    );

    const documentModel = await test.step(
      'GET billing-run/generate-invoice-data — MostUsedPaymentChannel and Preferences',
      async () => {
        const model = await fetchPhn86InvoiceDocumentModel(Request, billing.invoiceId);
        const actualChannel = (model.MostUsedPaymentChannel ?? '').trim();
        const actualPreferences = model.Preferences ?? [];
        const preferencesMatch = sameStringSet(actualPreferences, customer.preferenceNames);
        const channelMatch = actualChannel === customer.paymentChannelName;

        expect(actualChannel, 'MostUsedPaymentChannel').toBe(customer.paymentChannelName);
        expect(
          preferencesMatch,
          `Preferences expected ${JSON.stringify(customer.preferenceNames)} actual ${JSON.stringify(actualPreferences)}`,
        ).toBe(true);
        if (customer.customerNumber) {
          expect(String(model.CustomerNumber ?? ''), 'CustomerNumber').toBe(customer.customerNumber);
        }

        TestRunSummary.recordCheck({
          check: 'Invoice JSON MostUsedPaymentChannel and Preferences',
          expectedResult: `MostUsedPaymentChannel="${customer.paymentChannelName}"; Preferences=${JSON.stringify(customer.preferenceNames)}.`,
          actualResult: channelMatch && preferencesMatch
            ? `As expected — channel="${actualChannel}", preferences=${JSON.stringify(actualPreferences)}.`
            : `Not as expected — channel="${actualChannel}", preferences=${JSON.stringify(actualPreferences)}.`,
          passed: channelMatch && preferencesMatch,
        });
        return model;
      },
    );

    const pdf = await test.step('Download invoice PDF and read MostUsedPaymentChannel and Preferences', async () => {
      const downloaded = await downloadPhn86InvoicePdf(Request, billing.invoiceId);
      const channelInPdf = downloaded.pdfText.includes(customer.paymentChannelName);
      const preferencesInPdf = customer.preferenceNames.every((name) => downloaded.pdfText.includes(name));
      const templateMarkers =
        downloaded.pdfText.includes('PHN-86 MostUsedPaymentChannel') &&
        downloaded.pdfText.includes('PHN-86 Preferences');
      const passed = channelInPdf && preferencesInPdf && templateMarkers;

      expect(templateMarkers, 'PDF must be rendered from the PHN-86 template').toBe(true);
      expect(channelInPdf, 'PDF must contain MostUsedPaymentChannel').toBe(true);
      expect(preferencesInPdf, 'PDF must contain every preference name').toBe(true);

      TestRunSummary.recordCheck({
        check: 'Invoice PDF contains payment channel and preferences',
        expectedResult:
          'Generated PDF uses the new template and contains the customer payment channel and each preference name.',
        actualResult: passed
          ? `As expected — extractor=${downloaded.extractor}, documentId=${downloaded.invoiceDocumentId}, pdf=${downloaded.pdfPath}.`
          : `Not as expected — templateMarkers=${templateMarkers}, channelInPdf=${channelInPdf}, preferencesInPdf=${preferencesInPdf}, extractor=${downloaded.extractor}.`,
        passed,
      });
      return downloaded;
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_86_KEY,
        relevantEntityKeys: ['customer', 'pod', 'product', 'productContract', 'billingRun', 'invoice'],
        snapshot: {
          customerId: customer.customerId,
          customerNumber: customer.customerNumber,
          paymentChannelName: customer.paymentChannelName,
          preferenceNames: customer.preferenceNames,
          invoiceTemplateId: template.templateId,
          contractId: prechain.contractId,
          billingRunId: billing.billingRunId,
          invoiceId: billing.invoiceId,
          invoiceDocumentId: pdf.invoiceDocumentId,
          mostUsedPaymentChannel: documentModel.MostUsedPaymentChannel,
          preferences: documentModel.Preferences,
        },
      });
    });
  });
});
