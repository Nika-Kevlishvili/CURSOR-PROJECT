/**
 * PHN-3948 — CLONE - Templater: Invoice document type not showing correctly
 *
 * Bug-only automation (no TC .md on disk; alignment: Jira reproduce steps).
 *
 * Target env: Dev2
 *   BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 *
 * Reproduce (customfield_10103):
 *   1. Create a debit note
 *   2. Generate the file for the invoice
 * Actual: PDF DocumentType English/corrupt `DEBIT Без гаранцияТЕ`
 * Expected: [[DocumentType]] → Дебитно известие (debit), Фактура (invoice)
 *
 * swagger_refresh=failed_using_cache_plus_manual_curl
 *   Cached Cursor-Project/config/swagger/dev2/swagger-spec.json after parent
 *   GET http://10.236.20.11:8092/v3/api-docs (HTTP 200). Script curl.exe failed.
 *
 * PROFORMA_INVOICE is not generated here: Dev2 translation.translations is
 * missing that enum key (PROFORMA_Фактура). Do not fail this debit-note test
 * on that known gap.
 *
 * Reference spec(s):
 * - tests/cursor/PHN-3924-templater-document-type-bulgarian.spec.ts
 *   + phn-3924-templater-document-type-bulgarian.fixtures.ts
 * - tests/cursor/pdt-2891-connected-invoices.fixtures.ts
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - tests/cursor/phn-3943-invoiced-month-empty-for-corrections.fixtures.ts
 * - tests/cursor/phn-3951-cancelled-interim-invoice-price-component.fixtures.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PHN_3948_KEY,
  PHN_3948_TITLE,
  PHN_3924_TEMPLATE_ORIGINAL_NAME,
  EXPECTED_DOCUMENT_TYPE_BG,
  analyzeDebitNoteDocumentTypePdf,
  completeManualDebitNote,
  createManualDebitNoteWithTemplate,
  createManualInterimParentInvoiceWithTemplate,
  createPhn3948InvoiceDocumentTemplate,
  downloadAndAnalyzeDebitNotePdf,
  fetchInvoiceDocumentModel,
  isForbiddenDebitJsonDocumentType,
  optionalEntityId,
  productContractIdFromResponses,
  readDocumentType,
  type Phn3948Fx,
} from './phn-3948-templater-invoice-document-type.fixtures';

test.describe(
  `[${PHN_3948_KEY}]: Templater invoice DocumentType (Bulgarian debit note vs English/corrupt)`,
  { tag: ['@billing', '@dev2', '@phn-3948', '@templater'] },
  () => {
    test(`[${PHN_3948_KEY}]: ${PHN_3948_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(45 * 60 * 1000);

      const fx: Phn3948Fx = {
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      };

      const template = await test.step(
        'Precondition: upload + create BULGARIAN INVOICE DOCUMENT template ([[DocumentType]])',
        async () => createPhn3948InvoiceDocumentTemplate(fx),
      );
      expect(template.templateId, 'template id').toBeGreaterThan(0);

      TestRunSummary.registerPayload('template', {
        templateId: template.templateId,
        fileId: template.fileId,
        templateName: template.templateName,
        sourceAttachment: PHN_3924_TEMPLATE_ORIGINAL_NAME,
        sourceAssetPath: template.sourceAssetPath,
      });

      const parent = await test.step(
        'Precondition: product contract + completed manual interim parent invoice',
        async () =>
          createManualInterimParentInvoiceWithTemplate(fx, template.templateId),
      );

      TestRunSummary.registerPayload('customer', {
        customerId: optionalEntityId(Responses.customer[0]),
      });
      TestRunSummary.registerPayload('product', {
        productId: optionalEntityId(Responses.product[0]),
        invoiceTemplateId: template.templateId,
      });
      TestRunSummary.registerPayload('productContract', {
        productContractId: productContractIdFromResponses(Responses),
      });
      TestRunSummary.registerPayload('billingRun', {
        interimBillingRunId: parent.interimBillingRunId,
        parentInvoiceId: parent.parentInvoiceId,
        invoiceDocumentType: parent.invoiceDocumentType,
        templateId: template.templateId,
      });

      const { billingRunId: debitNoteBillingRunId } = await test.step(
        'Create MANUAL_CREDIT_OR_DEBIT_NOTE billing run (documentType=DEBIT_NOTE)',
        async () =>
          createManualDebitNoteWithTemplate(
            fx,
            parent.parentInvoiceId,
            template.templateId,
          ),
      );

      const debitNoteId = await test.step(
        'Complete debit-note billing run and resolve DEBIT_NOTE invoice',
        async () => completeManualDebitNote(fx, debitNoteBillingRunId),
      );
      expect(debitNoteId, 'debit note invoice id').toBeGreaterThan(0);

      TestRunSummary.registerPayload('invoice', {
        parentInvoiceId: parent.parentInvoiceId,
        debitNoteId,
        debitNoteBillingRunId,
        documentType: 'DEBIT_NOTE',
        templateId: template.templateId,
      });

      const parentDocType = await test.step(
        'GET generate-invoice-data for parent invoice — DocumentType Фактура',
        async () => {
          const doc = await fetchInvoiceDocumentModel(Request, parent.parentInvoiceId);
          const documentType = readDocumentType(doc);

          let passed = true;
          let assertionError: string | undefined;
          try {
            expect(
              parent.invoiceDocumentType,
              `Parent invoice ${parent.parentInvoiceId} invoiceDocumentType must be INVOICE ` +
                '(manual interim parent still uses InvoiceResponse.invoiceDocumentType=INVOICE)',
            ).toBe('INVOICE');
            expect(
              documentType,
              `Parent invoice ${parent.parentInvoiceId} DocumentType must be Bulgarian "Фактура" ` +
                `(invoiceDocumentType=${parent.invoiceDocumentType})`,
            ).toBe(EXPECTED_DOCUMENT_TYPE_BG.INVOICE);
          } catch (err) {
            passed = false;
            assertionError = err instanceof Error ? err.message : String(err);
            throw err;
          } finally {
            TestRunSummary.recordCheck({
              check: 'Parent invoice JSON DocumentType (INVOICE → Фактура)',
              expectedResult:
                'GET billing-run/generate-invoice-data?invoiceId=parent → DocumentType ' +
                `"${EXPECTED_DOCUMENT_TYPE_BG.INVOICE}" (invoiceDocumentType INVOICE, including interim).`,
              actualResult: passed
                ? `As expected — invoiceDocumentType=${parent.invoiceDocumentType} DocumentType=${JSON.stringify(documentType)}.`
                : `Not as expected — invoiceDocumentType=${parent.invoiceDocumentType} DocumentType=${JSON.stringify(documentType)}. ${assertionError ?? ''}`.trim(),
              passed,
            });
          }

          return documentType;
        },
      );

      const debitJsonType = await test.step(
        'GET generate-invoice-data for debit note — DocumentType Дебитно известие',
        async () => {
          const doc = await fetchInvoiceDocumentModel(Request, debitNoteId);
          const documentType = readDocumentType(doc);

          let passed = true;
          let assertionError: string | undefined;
          try {
            expect(
              isForbiddenDebitJsonDocumentType(documentType),
              `Debit note ${debitNoteId} DocumentType must not be English/corrupt ` +
                `(got ${JSON.stringify(documentType)})`,
            ).toBe(false);
            expect(
              documentType,
              `Debit note ${debitNoteId} DocumentType must be exact "${EXPECTED_DOCUMENT_TYPE_BG.DEBIT_NOTE}"`,
            ).toBe(EXPECTED_DOCUMENT_TYPE_BG.DEBIT_NOTE);
          } catch (err) {
            passed = false;
            assertionError = err instanceof Error ? err.message : String(err);
            throw err;
          } finally {
            TestRunSummary.recordCheck({
              check: 'Debit note JSON DocumentType (DEBIT_NOTE → Дебитно известие)',
              expectedResult:
                'GET billing-run/generate-invoice-data?invoiceId=debitNote → DocumentType ' +
                `exact "${EXPECTED_DOCUMENT_TYPE_BG.DEBIT_NOTE}". Must not be "DEBIT NOTE", ` +
                '"Debit Note", "DEBIT_NOTE", or contain "DEBIT Без".',
              actualResult: passed
                ? `As expected — DocumentType=${JSON.stringify(documentType)} (debitNoteId=${debitNoteId}).`
                : `Not as expected — DocumentType=${JSON.stringify(documentType)} (debitNoteId=${debitNoteId}). ${assertionError ?? ''}`.trim(),
              passed,
            });
          }

          return documentType;
        },
      );

      const pdf = await test.step(
        'Download debit-note PDF and extract text (system tools only)',
        async () => downloadAndAnalyzeDebitNotePdf(Request, debitNoteId),
      );

      await test.step('Attach generated debit-note PDF + extracted text', async () => {
        test.info().attach(`[${PHN_3948_KEY}] debit note PDF`, {
          path: pdf.pdfPath,
          contentType: 'application/pdf',
        });
        test.info().attach(
          `[${PHN_3948_KEY}] debit note PDF text (${pdf.pdfExtractor})`,
          {
            body: pdf.pdfText,
            contentType: 'text/plain; charset=utf-8',
          },
        );
      });

      await test.step(
        'Assert debit-note PDF header is Дебитно известие immediately before №',
        async () => {
          const analysis = analyzeDebitNoteDocumentTypePdf(
            pdf.pdfText,
            pdf.pdfExtractor,
          );

          TestRunSummary.recordCheck({
            check: 'Debit note PDF DocumentType header language',
            expectedResult:
              'Header line "Дебитно известие" immediately before №. English/corrupt ' +
              'DEBIT / DEBIT NOTE / "DEBIT Без гаранция" / INVOICE\\n№ must be absent. ' +
              'Static appendix "Към Фактура №" is ignored.',
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
            analysis.hasEnglishDebitHeader,
            'Debit-note PDF must NOT show English DEBIT/DEBIT NOTE header',
          ).toBe(false);
          expect(
            analysis.hasCorruptDebitWithoutGuarantee,
            'Debit-note PDF must NOT contain corrupt "DEBIT Без гаранция"',
          ).toBe(false);
          expect(
            analysis.hasEnglishInvoiceHeader,
            'Debit-note PDF must NOT show English INVOICE header before №',
          ).toBe(false);
          expect(
            analysis.hasBulgarianDebitHeader,
            'Debit-note PDF must show "Дебитно известие" immediately before №',
          ).toBe(true);
          expect(analysis.passed, analysis.reason).toBe(true);
        },
      );

      await test.step('Attach test run summary', async () => {
        const contractId = productContractIdFromResponses(Responses);
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3948_KEY,
          relevantEntityKeys: [
            'customer',
            'product',
            'productContract',
            'billingRun',
            'invoice',
          ],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            templateId: template.templateId,
            fileId: template.fileId,
            parentInvoiceId: parent.parentInvoiceId,
            parentInvoiceDocumentType: parent.invoiceDocumentType,
            parentJsonDocumentType: parentDocType,
            debitNoteId,
            debitNoteBillingRunId,
            debitJsonDocumentType: debitJsonType,
            invoiceDocumentId: pdf.invoiceDocumentId,
            pdfPath: pdf.pdfPath,
            pdfExtractor: pdf.pdfExtractor,
            debitPdfPassed: pdf.analysis.passed,
            hasBulgarianDebitHeader: pdf.analysis.hasBulgarianDebitHeader,
            hasEnglishDebitHeader: pdf.analysis.hasEnglishDebitHeader,
            hasCorruptDebitWithoutGuarantee:
              pdf.analysis.hasCorruptDebitWithoutGuarantee,
          },
        });
      });
    });
  },
);
