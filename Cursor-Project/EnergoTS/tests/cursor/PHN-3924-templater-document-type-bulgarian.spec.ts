/**
 * PHN-3924 — CLONE - Templater: Unlicensed Templater version on TEST
 *
 * Bug-only automation (no TC .md on disk; alignment: Jira reproduce steps).
 *
 * Target env: Dev2
 *   BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 *
 * Reproduce (customfield_10103):
 *   1. Start a billing run with attached template Faktura_07.08.2025.docx
 *   2. Generate the invoice
 * Actual: [[DocumentType]] displayed in English (INVOICE)
 * Expected: [[DocumentType]] displayed in Bulgarian (Фактура)
 *
 * Flow:
 *   1. Upload DOCX via POST template/upload-template-file (FileUploadRequest)
 *   2. POST /template — ACTIVE DOCUMENT/INVOICE, language=BULGARIAN, unique name
 *      (no setDefault / default flags)
 *   3. Billable chain with product.INVOICE_TEMPLATE = new template id
 *   4. FOR_VOLUMES billing run → COMPLETED
 *   5. Download invoice PDF; assert DocumentType header is "Фактура" not "INVOICE"
 *
 * Reference spec(s):
 * - tests/cursor/dev-volume-invoice-custom-template.fixtures.ts
 * - tests/cursor/pdt-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts
 * - tests/cursor/phn-3951-cancelled-interim-invoice-price-component.fixtures.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - tests/cursor/PDT-2177-supply-action-deactivation-restricted-period.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PHN_3924_KEY,
  PHN_3924_TITLE,
  PHN_3924_TEMPLATE_ORIGINAL_NAME,
  analyzeDocumentTypeLanguage,
  runPhn3924DocumentTypeScenario,
  type Phn3924Fx,
} from './phn-3924-templater-document-type-bulgarian.fixtures';

test.describe(
  `[${PHN_3924_KEY}]: Templater DocumentType language (Bulgarian vs English)`,
  { tag: ['@billing', '@dev2', '@phn-3924', '@templater'] },
  () => {
    test(`[${PHN_3924_KEY}]: ${PHN_3924_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(45 * 60 * 1000);

      const fx: Phn3924Fx = {
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      };

      const scenario = await test.step(
        'Run PHN-3924 DocumentType scenario (template → chain → billing → PDF)',
        async () => runPhn3924DocumentTypeScenario(fx),
      );

      TestRunSummary.registerPayload('template', {
        templateId: scenario.template.templateId,
        fileId: scenario.template.fileId,
        templateName: scenario.template.templateName,
        sourceAttachment: PHN_3924_TEMPLATE_ORIGINAL_NAME,
        sourceAssetPath: scenario.template.sourceAssetPath,
      });
      TestRunSummary.registerPayload('product', {
        productId: scenario.productId,
        invoiceTemplateId: scenario.template.templateId,
      });
      TestRunSummary.registerPayload('billingRun', {
        billingRunId: scenario.billingRunId,
        billingType: 'FOR_VOLUMES',
      });
      TestRunSummary.registerPayload('invoice', {
        invoiceId: scenario.invoiceId,
        invoiceDocumentId: scenario.invoiceDocumentId,
      });

      await test.step('Attach generated invoice PDF + extracted text', async () => {
        test.info().attach(`[${PHN_3924_KEY}] invoice PDF`, {
          path: scenario.pdfPath,
          contentType: 'application/pdf',
        });
        test.info().attach(
          `[${PHN_3924_KEY}] invoice PDF text (${scenario.pdfExtractor})`,
          {
            body: scenario.pdfText,
            contentType: 'text/plain; charset=utf-8',
          },
        );
      });

      await test.step(
        'Assert [[DocumentType]] expands to Bulgarian Фактура (not English INVOICE)',
        async () => {
          // Re-run analyzer so the step owns the assertion clearly.
          const analysis = analyzeDocumentTypeLanguage(
            scenario.pdfText,
            scenario.pdfExtractor,
          );

          TestRunSummary.recordCheck({
            check: 'DocumentType language on generated invoice PDF',
            expectedResult:
              'Header DocumentType is Bulgarian "Фактура" immediately before № ' +
              '(not English "INVOICE"). Static appendix "Към Фактура №" alone is insufficient.',
            actualResult: analysis.passed
              ? `As expected — ${analysis.reason}`
              : `Not as expected — ${analysis.reason}`,
            passed: analysis.passed,
          });

          expect(
            analysis.textLength,
            `PDF text extraction (${analysis.extractor}) must yield non-empty text`,
          ).toBeGreaterThan(0);

          expect(
            analysis.hasEnglishDocumentTypeHeader,
            'Invoice PDF must NOT show English DocumentType header "INVOICE" before № ' +
              `(extractor=${analysis.extractor}; see Inv_202077922.pdf bug signature)`,
          ).toBe(false);

          expect(
            analysis.hasBulgarianDocumentTypeHeader,
            'Invoice PDF must show Bulgarian DocumentType header "Фактура" before № ' +
              `(extractor=${analysis.extractor})`,
          ).toBe(true);

          expect(analysis.passed, analysis.reason).toBe(true);
        },
      );

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3924_KEY,
          relevantEntityKeys: [
            'customer',
            'pod',
            'product',
            'productContract',
            'billingRun',
            'invoice',
          ],
          extraLinks: buildProductContractTabLinks(scenario.contractId),
          snapshot: {
            templateId: scenario.template.templateId,
            fileId: scenario.template.fileId,
            billingRunId: scenario.billingRunId,
            invoiceId: scenario.invoiceId,
            invoiceDocumentId: scenario.invoiceDocumentId,
            pdfPath: scenario.pdfPath,
            documentTypePassed: scenario.documentTypeAnalysis.passed,
            hasBulgarianDocumentTypeHeader:
              scenario.documentTypeAnalysis.hasBulgarianDocumentTypeHeader,
            hasEnglishDocumentTypeHeader:
              scenario.documentTypeAnalysis.hasEnglishDocumentTypeHeader,
            hasUnlicensedTemplaterWatermark:
              scenario.documentTypeAnalysis.hasUnlicensedTemplaterWatermark,
          },
        });
      });
    });
  },
);
