/**
 * PHN-3951 — CLONE - Templater: Cancelled Interim invoice - not displaying Price component
 *
 * Bug-only automation (no TC .md on disk; alignment: Jira reproduce steps).
 *
 * Target env: Dev2 ONLY (this spec is Dev2-locked)
 *   BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 *   FRONTEND_BASE_URL=https://devapps.energo-pro.bg/app/phoenix2-dev
 *
 * Required assets:
 *   Cursor-Project/EnergoTS/tests/cursor/assets/phn-3951/
 *     faktura-ap-mp-ki-di-za-energiya.docx
 *     (shipped from Jira attachment "Фактура_АП_МП_КИ_ДИ - за енергия.docx")
 *
 * Flow (Dev2):
 *   0. Create INVOICE DOCUMENT template on Dev2 by uploading the bug-attached
 *      DOCX (POST /template/upload-template-file?fileFormats=DOCX → fileId)
 *      and creating a fresh Bulgarian ACTIVE INVOICE template (POST /template
 *      with unique name, templateType=DOCUMENT, templatePurpose=INVOICE,
 *      templateStatus=ACTIVE, language=BULGARIAN, outputFileFormat=['PDF'],
 *      fileSignings=['SIGNING_WITH_SYSTEM_CERTIFICATE']). The returned
 *      templateId (`energyInvoiceTemplateId`) is used for BOTH the manual
 *      interim billing run AND the INVOICE_REVERSAL billing run.
 *   1. Precondition — customer + product contract chain for manual interim.
 *   2. Create manual INTERIM_AND_ADVANCE_PAYMENT billing run using
 *      `buildManualInterimPayload` (pdt-2872 helpers) and force
 *      `commonParameters.templateId = energyInvoiceTemplateId`
 *      (excl VAT = 14456.08 → matches the Prod bug attachment amount).
 *   3. Run interim through DRAFT → GENERATED → COMPLETED so the interim
 *      becomes REAL with its PDF; resolve interim invoice id from
 *      `invoice/listing` (`resolveInterimInvoiceIds`).
 *   4. POST an INVOICE_REVERSAL billing run for that interim
 *      (`GeneratePayload.billing.reversalBilling(0)`). FORCE
 *      `commonParameters.templateId = energyInvoiceTemplateId`
 *      (no fallback to `envVariables.invoice_document_template` — that was
 *      the bug in the previous Dev2 run: a generic env-provisioned template
 *      that did not contain the `SD.AdvancePayments` price-component block).
 *   5. `waitForInvoiceGeneration(true, true, 1, reversalIndex)` — generate PDF
 *      and complete accounting for the CREDIT_NOTE reversal invoice.
 *   6. Assert invoiceType=REVERSAL / invoiceDocumentType=CREDIT_NOTE.
 *   7. Download the reversal CREDIT_NOTE PDF via
 *      GET /invoice/download-document?invoiceDocumentId=… (Swagger dev2)
 *      using `InvoiceResponse.file[].id`, save to `test-results/`, extract
 *      text with system tools only (pdftotext → python3 pypdf → PyPDF2).
 *   8. Assert on rendered PDF: labels "Междинно плащане" (bug ER — MUST be
 *      present, not a generic short-template PDF), "Общо сума без ДДС",
 *      "Данъчна основа", "ДДС", the interim excl-VAT amount, and NO raw
 *      `[[SD.AdvancePayments`, `[[TotalExclVat`, `[[TotalVat`, `[[TotalInclVat`,
 *      `[[BasisForIssuing]]` placeholders. Also downloads + extracts the
 *      interim REAL invoice PDF as reference evidence when available.
 *
 * Bug expectation: today on Dev2 the reversal CREDIT_NOTE PDF is expected to
 * FAIL the "Междинно плащане" / amount / no-`[[` assertions (this documents
 * the templater regression). The `expected vs actual` block captures the
 * exact diff for the manual verifier.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2177-supply-action-deactivation-restricted-period.fixtures.ts
 *   (FileUploadRequest multipart pattern)
 * - tests/cursor/PDT-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - tests/cursor/pdt-2891-connected-invoices.fixtures.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - jsons/payloads/create/nomenclatures/templates.ts (uploadTemplateFile pattern)
 * - jsons/payloads/create/operationsManagement/invoiceDocumentTemplate.ts
 * - jsons/payloads/create/billing/manualoInterim.ts, reversal.ts
 * - jsons/payloadGenerators/domains/BillingPayloads.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PHN_3951_KEY,
  PHN_3951_TITLE,
  PHN_3951_AMOUNT_EXCL_VAT,
  PHN_3951_TEMPLATE_ASSET_ORIGINAL_NAME,
  analyzeReversalPdfText,
  createInterimReversalBillingRun,
  createManualInterimParentInvoice,
  createPhn3951EnergyInvoiceTemplate,
  downloadInvoiceDocumentBuffer,
  extractPdfText,
  getInvoiceBody,
  pollInvoiceFileRefs,
  savePdfToTestResults,
  type Phn3951Fx,
} from './phn-3951-cancelled-interim-invoice-price-component.fixtures';

test.describe(
  `[${PHN_3951_KEY}]: Cancelled/reversed interim invoice PDF fields`,
  { tag: ['@billing', '@dev2', '@phn-3951'] },
  () => {
    test(`[${PHN_3951_KEY}]: ${PHN_3951_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(45 * 60 * 1000);

      const fx: Phn3951Fx = {
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        FileUploadRequest,
      };

      // ── 1. Create energy invoice template from bug-attached DOCX ─────────
      const energyInvoiceTemplate = await test.step(
        'Precondition: create Dev2 INVOICE DOCUMENT template from PHN-3951 bug DOCX',
        async () => createPhn3951EnergyInvoiceTemplate(fx),
      );

      TestRunSummary.registerPayload('template', {
        energyInvoiceTemplateId: energyInvoiceTemplate.templateId,
        energyInvoiceTemplateFileId: energyInvoiceTemplate.fileId,
        energyInvoiceTemplateName: energyInvoiceTemplate.templateName,
        sourceAttachment: PHN_3951_TEMPLATE_ASSET_ORIGINAL_NAME,
      });

      // ── 2. Manual interim parent (COMPLETED, REAL, PDF) ──────────────────
      const { interimBillingRunId, interimInvoiceId } = await createManualInterimParentInvoice(
        fx,
        energyInvoiceTemplate.templateId,
      );

      TestRunSummary.registerPayload('billingRun', {
        interimBillingRunId,
        interimInvoiceId,
        energyInvoiceTemplateId: energyInvoiceTemplate.templateId,
        amountExcludingVat: PHN_3951_AMOUNT_EXCL_VAT,
        billingType: 'MANUAL_INTERIM_AND_ADVANCE_PAYMENT',
      });

      const interimInvoiceIndex = Responses.invoice.indexOf(interimInvoiceId);
      expect(
        interimInvoiceIndex,
        'interim invoice id must be present in Responses.invoice before reversal',
      ).toBeGreaterThanOrEqual(0);

      // ── 3. INVOICE_REVERSAL billing run (forced templateId) ──────────────
      let reversalBillingRunId = 0;
      let reversalBillingRunIndex = -1;
      await test.step(
        `Create INVOICE_REVERSAL billing run for interim invoice ` +
          `(templateId=${energyInvoiceTemplate.templateId}, forced override — ` +
          `NO fallback to envVariables.invoice_document_template)`,
        async () => {
          const created = await createInterimReversalBillingRun(
            fx,
            interimInvoiceIndex,
            energyInvoiceTemplate.templateId,
          );
          reversalBillingRunId = created.reversalBillingRunId;
          reversalBillingRunIndex = created.reversalBillingRunIndex;
        },
      );

      await test.step(
        'Generate + complete reversal billing (start-billing → GENERATED → COMPLETED)',
        async () => {
          await GeneratePayload.billing.waitForInvoiceGeneration(
            true,
            true,
            1,
            reversalBillingRunIndex,
          );
        },
      );

      const reversalInvoiceId = Responses.invoice[Responses.invoice.length - 1] as number;
      expect(
        reversalInvoiceId,
        'reversal invoice id must be pushed to Responses.invoice by waitForInvoiceGeneration',
      ).toBeGreaterThan(0);
      expect(
        reversalInvoiceId,
        `reversal invoice id (${reversalInvoiceId}) must differ from parent interim id (${interimInvoiceId})`,
      ).not.toBe(interimInvoiceId);

      // ── 4. Reversal invoice type sanity ──────────────────────────────────
      const reversalInvoice = await test.step(
        'GET reversal invoice — assert invoiceType=REVERSAL / invoiceDocumentType=CREDIT_NOTE',
        async () => {
          const body = await getInvoiceBody(Request, reversalInvoiceId);
          expect(String(body.invoiceType), 'reversal invoice.invoiceType').toBe('REVERSAL');
          expect(String(body.invoiceDocumentType), 'reversal invoice.invoiceDocumentType').toBe(
            'CREDIT_NOTE',
          );
          return body;
        },
      );

      TestRunSummary.registerPayload('invoice', {
        interimInvoiceId,
        reversalInvoiceId,
        reversalBillingRunId,
        invoiceType: reversalInvoice.invoiceType,
        invoiceDocumentType: reversalInvoice.invoiceDocumentType,
        totalAmountExcludingVat: reversalInvoice.totalAmountExcludingVat,
        totalAmountOfVat: reversalInvoice.totalAmountOfVat,
        totalAmountIncludingVat: reversalInvoice.totalAmountIncludingVat,
      });

      // ── 5. Download + extract reversal CREDIT_NOTE PDF ───────────────────
      const reversalFiles = await test.step(
        'Resolve reversal invoice file[] document ids (Swagger InvoiceResponse.file)',
        async () => {
          const { files } = await pollInvoiceFileRefs(Request, reversalInvoiceId);
          expect(
            files.length,
            `reversal invoice ${reversalInvoiceId} must expose at least one file (invoiceDocumentId)`,
          ).toBeGreaterThan(0);
          return files;
        },
      );

      const reversalPrimaryFile = reversalFiles[0];
      const reversalPdfPath = await test.step(
        `Download reversal CREDIT_NOTE PDF (invoiceDocumentId=${reversalPrimaryFile.id})`,
        async () => {
          const buf = await downloadInvoiceDocumentBuffer(Request, reversalPrimaryFile.id);
          const saved = savePdfToTestResults(
            buf,
            `phn-3951-reversal-invoice-${reversalInvoiceId}-doc-${reversalPrimaryFile.id}.pdf`,
          );
          test.info().attach(
            `[${PHN_3951_KEY}] reversal PDF (${reversalPrimaryFile.name ?? 'invoice.pdf'})`,
            {
              path: saved,
              contentType: 'application/pdf',
            },
          );
          return saved;
        },
      );

      const { text: reversalText, extractor: reversalExtractor } = await test.step(
        'Extract reversal PDF text (system tools only)',
        async () => extractPdfText(reversalPdfPath),
      );
      test.info().attach(`[${PHN_3951_KEY}] reversal PDF text (${reversalExtractor})`, {
        body: reversalText,
        contentType: 'text/plain; charset=utf-8',
      });

      // ── 6. Reference: interim REAL invoice PDF (best-effort) ─────────────
      let interimReferenceInfo:
        | { extractor: string; textLength: number; passed: boolean; reason: string }
        | null = null;
      await test.step(
        'Reference: download + extract interim REAL invoice PDF (skips if file[] empty)',
        async () => {
          const { files: interimFiles } = await pollInvoiceFileRefs(
            Request,
            interimInvoiceId,
            60_000,
            5_000,
          );
          if (interimFiles.length === 0) {
            test.info().attach(`[${PHN_3951_KEY}] interim PDF unavailable`, {
              body: `Interim invoice ${interimInvoiceId} has no file[] entries — skipping reference PDF extraction.`,
              contentType: 'text/plain; charset=utf-8',
            });
            return;
          }
          const primary = interimFiles[0];
          const buf = await downloadInvoiceDocumentBuffer(Request, primary.id);
          const saved = savePdfToTestResults(
            buf,
            `phn-3951-interim-invoice-${interimInvoiceId}-doc-${primary.id}.pdf`,
          );
          test.info().attach(
            `[${PHN_3951_KEY}] interim PDF (${primary.name ?? 'invoice.pdf'})`,
            { path: saved, contentType: 'application/pdf' },
          );
          const { text, extractor } = extractPdfText(saved);
          test.info().attach(`[${PHN_3951_KEY}] interim PDF text (${extractor})`, {
            body: text,
            contentType: 'text/plain; charset=utf-8',
          });
          const analysis = analyzeReversalPdfText(text, extractor, PHN_3951_AMOUNT_EXCL_VAT);
          interimReferenceInfo = {
            extractor,
            textLength: text.length,
            passed: analysis.passed,
            reason: analysis.reason,
          };
        },
      );

      // ── 7. Assert reversal PDF has substituted price-component fields ────
      await test.step(
        'Assert reversal CREDIT_NOTE PDF renders substituted price-component fields (PHN-3951 bug)',
        async () => {
          const outcome = analyzeReversalPdfText(
            reversalText,
            reversalExtractor,
            PHN_3951_AMOUNT_EXCL_VAT,
          );

          let passed = true;
          let assertionError: string | undefined;
          try {
            expect(
              outcome.observed.forbiddenTagsFound,
              `Reversal CREDIT_NOTE PDF must NOT contain unsubstituted templater tags. Found: ${JSON.stringify(outcome.observed.forbiddenTagsFound)}`,
            ).toEqual([]);
            // Bug ER: "Междинно плащане" (interim-payment) row MUST be present.
            // A generic short invoice template WITHOUT the SD.AdvancePayments
            // block would produce a PDF where this label is missing — that is
            // exactly the templater bug this test guards against, so we
            // require the specific label (not just amount fallback).
            expect(
              outcome.observed.hasInterimPaymentLabel,
              'Reversal PDF must contain the interim-payment label "Междинно плащане" ' +
                '(bug ER — this is the SD.AdvancePayments block that must be substituted, ' +
                'not a generic short invoice template output).',
            ).toBe(true);
            expect(
              outcome.observed.hasTotalExclVatLabel,
              'Reversal PDF must contain "Общо сума без ДДС" (TotalExclVat substitution)',
            ).toBe(true);
            expect(
              outcome.observed.hasTaxBaseLabel,
              'Reversal PDF must contain "Данъчна основа" (tax base line)',
            ).toBe(true);
            expect(
              outcome.observed.hasVatLabel,
              'Reversal PDF must contain a VAT amount line ("ДДС")',
            ).toBe(true);
            expect(
              outcome.observed.hasExpectedAmount,
              `Reversal PDF must contain the interim amount ${PHN_3951_AMOUNT_EXCL_VAT} (any Bulgarian/decimal format)`,
            ).toBe(true);
          } catch (err) {
            passed = false;
            assertionError = err instanceof Error ? err.message : String(err);
            throw err;
          } finally {
            TestRunSummary.recordCheck({
              check: 'Reversal CREDIT_NOTE PDF price-component fields substituted',
              expectedResult:
                'Reversal PDF of a cancelled/reversed interim invoice — rendered against the ' +
                `PHN-3951 bug-attached template (id=${energyInvoiceTemplate.templateId}, ` +
                `"${energyInvoiceTemplate.templateName}") — renders substituted templater ` +
                'values: "Междинно плащане" line with the interim amount (bug ER, MUST be present), ' +
                '"Общо сума без ДДС" with a number, "Данъчна основа" with a number, ' +
                '"ДДС" line with amount, interim excl-VAT amount visible; ' +
                'no unsubstituted [[SD.AdvancePayments / [[TotalExclVat / [[TotalVat / [[TotalInclVat / [[BasisForIssuing]] tags.',
              actualResult: passed
                ? `As expected — ${outcome.reason} ` +
                  `(extractor=${reversalExtractor}, textLength=${reversalText.length}, ` +
                  `reversalInvoiceId=${reversalInvoiceId}, invoiceDocumentId=${reversalPrimaryFile.id}, ` +
                  `energyInvoiceTemplateId=${energyInvoiceTemplate.templateId}).`
                : `Not as expected — ${outcome.reason} ` +
                  `(extractor=${reversalExtractor}, textLength=${reversalText.length}, ` +
                  `reversalInvoiceId=${reversalInvoiceId}, invoiceDocumentId=${reversalPrimaryFile.id}, ` +
                  `energyInvoiceTemplateId=${energyInvoiceTemplate.templateId}). ` +
                  `${assertionError ?? ''}`.trim(),
              passed,
            });
          }
        },
      );

      // ── 8. Manual verification summary ───────────────────────────────────
      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PHN_3951_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          snapshot: {
            energyInvoiceTemplateId: energyInvoiceTemplate.templateId,
            energyInvoiceTemplateFileId: energyInvoiceTemplate.fileId,
            energyInvoiceTemplateName: energyInvoiceTemplate.templateName,
            energyInvoiceTemplateSource: PHN_3951_TEMPLATE_ASSET_ORIGINAL_NAME,
            interimBillingRunId,
            interimInvoiceId,
            reversalBillingRunId,
            reversalInvoiceId,
            reversalInvoiceDocumentId: reversalPrimaryFile.id,
            reversalInvoiceDocumentName: reversalPrimaryFile.name ?? null,
            amountExcludingVat: PHN_3951_AMOUNT_EXCL_VAT,
            reversalPdfPath,
            reversalExtractor,
            reversalTextLength: reversalText.length,
            interimReference: interimReferenceInfo,
          },
        });
      });
    });
  },
);
