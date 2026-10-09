/**
 * DEV/TEST-DATA — Create FOR_VOLUMES draft volume invoice using a custom invoice
 * document template uploaded from a fixed Downloads .docx.
 *
 * Does NOT call start-generating or start-accounting (billing run stays DRAFT).
 *
 * Environments (portal via resolvePortalBase from BASE_URL):
 * - Dev:  http://10.236.20.11:8091 → https://devapps.energo-pro.bg/app/phoenix1-dev
 * - Test: https://testapps.energo-pro.bg/backend/phoenix-epres → …/app/phoenix-epres
 * - Dev2: https://devapps.energo-pro.bg/backend/phoenix2-dev → …/app/phoenix-dev2
 *
 * Reference spec(s):
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 * - tests/cursor/dev-volume-invoice-custom-template.fixtures.ts
 * - tests/cursor/phn-362-wait-yes-split-month-anchor.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  DEV_DATA_KEY,
  CUSTOM_INVOICE_DOCX_PATH,
  buildBillingRunPreviewLink,
  buildInvoicePreviewLink,
  createForVolumesDraftBillingRun,
  logDevVolCustomTplCreatedEntities,
  resolvePortalBase,
  runDevVolumeInvoiceCustomTemplatePrechain,
  uploadAndCreateCustomInvoiceDocumentTemplate,
  type DevVolCustomTplScenarioResult,
} from './dev-volume-invoice-custom-template.fixtures';

test.describe('[DEV-DATA]: Volume invoice draft with custom invoice document template', {
  tag: ['@billing', '@dev-data'],
}, () => {
  test(
    '[DEV-DATA]: Volume invoice draft with custom invoice document template',
    async ({
      Request,
      FileUploadRequest,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(30 * 60 * 1000);
      const fx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };

      const template = await test.step(
        'Precondition: upload custom DOCX and create ACTIVE INVOICE DOCUMENT template',
        async () => {
          const created = await uploadAndCreateCustomInvoiceDocumentTemplate(
            fx,
            CUSTOM_INVOICE_DOCX_PATH,
          );
          expect(created.templateId, 'custom invoice template id').toBeGreaterThan(0);
          expect(created.fileId, 'uploaded template file id').toBeGreaterThan(0);
          TestRunSummary.registerPayload('invoiceDocumentTemplate', {
            templateId: created.templateId,
            templateName: created.templateName,
            fileId: created.fileId,
            sourcePath: CUSTOM_INVOICE_DOCX_PATH,
            templateType: 'DOCUMENT',
            templatePurpose: 'INVOICE',
            templateStatus: 'ACTIVE',
            language: 'BULGARIAN',
            outputFileFormat: ['PDF'],
          });
          return created;
        },
      );

      const pre = await test.step(
        'Precondition: customer → PC → terms → POD → product (custom INVOICE_TEMPLATE) → contract → activate → BBP',
        async () => {
          const chain = await runDevVolumeInvoiceCustomTemplatePrechain(
            fx,
            template.templateId,
          );
          TestRunSummary.registerPayload('prechain', {
            customerId: chain.customerId,
            podId: chain.podId,
            productId: chain.productId,
            contractId: chain.contractId,
            bbpId: chain.bbpId,
            invoiceTemplateId: template.templateId,
            documentPeriod: chain.documentPeriod,
          });
          return chain;
        },
      );

      const billing = await test.step(
        'Create FOR_VOLUMES billing run and start-billing only (no generate/accounting)',
        async () => createForVolumesDraftBillingRun(fx),
      );

      await test.step('Assert: billing run DRAFT with ≥1 draft invoice', async () => {
        expect(billing.billingRunStatus, 'billing run must remain DRAFT').toBe('DRAFT');
        expect(
          billing.draftInvoiceIds.length,
          'draft-invoices must return at least one invoice',
        ).toBeGreaterThanOrEqual(1);

        TestRunSummary.recordCheck({
          check: 'FOR_VOLUMES draft volume invoice with custom invoice document template',
          expectedResult:
            'Custom INVOICE DOCUMENT template created from Downloads DOCX; product.INVOICE_TEMPLATE uses that template id; billing run status DRAFT after start-billing only; draft-invoices has ≥1 invoice.',
          actualResult: `As expected — templateId=${template.templateId}, status=${billing.billingRunStatus}, draftInvoices=${billing.draftInvoiceIds.length}, billingRunId=${billing.billingRunId}`,
          passed: true,
        });
      });

      const result: DevVolCustomTplScenarioResult = {
        ...pre,
        invoiceTemplateId: template.templateId,
        invoiceTemplateName: template.templateName,
        uploadedFileId: template.fileId,
        billingRunId: billing.billingRunId,
        draftInvoiceIds: billing.draftInvoiceIds,
        billingRunStatus: billing.billingRunStatus,
      };

      await test.step('Log created entity ids and portal preview links', async () => {
        logDevVolCustomTplCreatedEntities(result);
        test.info().attach('[DEV-DATA] created entities', {
          body: JSON.stringify(result, null, 2),
          contentType: 'application/json',
        });
      });

      await test.step('Attach test run summary', async () => {
        const portalBase = resolvePortalBase();
        const billingRunLink = buildBillingRunPreviewLink(billing.billingRunId, portalBase);
        const invoiceLinks = billing.draftInvoiceIds.map((id) =>
          buildInvoicePreviewLink(id, portalBase),
        );
        const extraLinks: Record<string, string[]> = {
          ...buildProductContractTabLinks(pre.contractId, portalBase),
          billingRun: [billingRunLink],
          invoice: invoiceLinks,
        };

        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: DEV_DATA_KEY,
          relevantEntityKeys: ['customer', 'product', 'productContract', 'billingRun', 'invoice'],
          extraLinks,
          snapshot: {
            invoiceTemplateId: template.templateId,
            invoiceTemplateName: template.templateName,
            uploadedFileId: template.fileId,
            customerId: pre.customerId,
            podId: pre.podId,
            productId: pre.productId,
            contractId: pre.contractId,
            bbpId: pre.bbpId,
            billingRunId: billing.billingRunId,
            billingRunStatus: billing.billingRunStatus,
            draftInvoiceIds: billing.draftInvoiceIds,
            portalBase,
            BILLING_RUN_LINK: billingRunLink,
            INVOICE_LINK: invoiceLinks,
          },
        });
      });
    },
  );
});
