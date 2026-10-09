/**
 * PDT-3223 — invoice JSON DD.TablePCScales.ListPC ordered by PC Name ASC then Id ASC.
 *
 * Jira (Customer Feedback, Done): exact title used in describe/test (typo "componenst" is verbatim).
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts
 * - tests/cursor/PHN-3943-invoiced-month-empty-for-corrections.spec.ts
 * - tests/cursor/phn-3943-invoiced-month-empty-for-corrections.fixtures.ts
 * - tests/billing/forVolumes/forVolumes.spec.ts
 * - tests/cursor/PDT-2937-invoice-detailed-data-same-pc-name.spec.ts
 *
 * - tests/cursor/dev-volume-invoice-custom-template.spec.ts
 * - tests/cursor/PHN-3951-cancelled-interim-invoice-price-component.spec.ts
 *
 * Target env: Dev2 (phoenix2-dev). Uploads Comfort invoice DOCX and binds it
 * as product INVOICE_TEMPLATE, then asserts JSON ListPC + records PDF print order.
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3223_EXPECTED_LIST_PC_SEQUENCE,
  PDT_3223_INVOICE_DOCX_PATH,
  PDT_3223_KEY,
  PDT_3223_PRICE_COMPONENTS,
  PDT_3223_TITLE,
  assertPdt3223ListPcOrder,
  buildPdt3223BillingRunPdfPreviewLink,
  downloadPdt3223InvoicePdfAndReadPrintedPcOrder,
  fetchInvoiceDocumentModel,
  findListPcContainingDisplayNames,
  runPdt3223ScaleInvoiceJsonPrechain,
  runPdt3223StandardBillingAndRealize,
  uploadPdt3223InvoiceDocumentTemplate,
  type Pdt3223Fx,
} from './pdt-3223-invoice-json-tablepcscales-listpc-order.fixtures';

test.describe(`[${PDT_3223_KEY}]: ${PDT_3223_TITLE}`, {
  tag: ['@billing', '@invoice', '@pdt-3223'],
}, () => {
  test(`[${PDT_3223_KEY}]: ${PDT_3223_TITLE}`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const fx: Pdt3223Fx = {
      Request,
      FileUploadRequest,
      GeneratePayload,
      Responses,
      Endpoints,
      Nomenclatures,
    };

    const template = await test.step(
      'Precondition: upload Comfort invoice DOCX and create ACTIVE INVOICE DOCUMENT template',
      async () => {
        const created = await uploadPdt3223InvoiceDocumentTemplate(fx, PDT_3223_INVOICE_DOCX_PATH);
        TestRunSummary.registerPayload('invoiceDocumentTemplate', {
          templateId: created.templateId,
          templateName: created.templateName,
          fileId: created.fileId,
          sourcePath: PDT_3223_INVOICE_DOCX_PATH,
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
      'Precondition: legal customer → term → 3 BY_SCALES PCs (Name vs displayName vs Id) → product (custom INVOICE_TEMPLATE) → scale POD/meter → contract → POD activation → energy data by scales',
      async () => {
        const result = await runPdt3223ScaleInvoiceJsonPrechain(fx, template.templateId);
        TestRunSummary.registerPayload('customer', {
          customerId: (Responses.customer[0] as { id?: number })?.id,
        });
        TestRunSummary.registerPayload('product', {
          productId: Responses.product[0],
          priceComponentIds: result.priceComponents.map((pc) => pc.id),
          invoiceTemplateId: result.invoiceTemplateId,
        });
        TestRunSummary.registerPayload('productContract', {
          contractId: result.contractId,
          podIdentifier: result.podIdentifier,
          period: result.period,
        });
        TestRunSummary.registerPayload('priceComponents', {
          createOrder: PDT_3223_PRICE_COMPONENTS,
          created: result.priceComponents,
          expectedListPcPcSequence: [...PDT_3223_EXPECTED_LIST_PC_SEQUENCE],
          note:
            'Sort key is pc.name (not displayName). JSON PC is displayName. ' +
            'All three PCs share the same scaleCode so they land in one ListPC.',
        });
        return result;
      },
    );

    const billed = await test.step(
      'Precondition: STANDARD FOR_VOLUMES billing run and realize invoice',
      async () => {
        const result = await runPdt3223StandardBillingAndRealize(fx);
        TestRunSummary.registerPayload('billingRun', {
          billingRunId: result.billingRunId,
          billingType: 'STANDARD_BILLING',
          applicationModelType: ['FOR_VOLUMES'],
        });
        TestRunSummary.registerPayload('invoice', {
          invoiceId: result.invoiceId,
        });
        return result;
      },
    );

    const doc = await test.step(
      'GET billing-run/generate-invoice-data?invoiceId= realized invoice',
      async () => {
        const body = await fetchInvoiceDocumentModel(Request, billed.invoiceId);
        TestRunSummary.registerPayload('invoiceDocumentModel', {
          invoiceId: billed.invoiceId,
          ddLength: Array.isArray(body.DD) ? (body.DD as unknown[]).length : null,
          documentNumber: body.DocumentNumber,
        });
        return body;
      },
    );

    await test.step(
      'Assert DD.TablePCScales.ListPC.PC ordered by Name ASC then Id ASC',
      async () => {
        const expected = [...PDT_3223_EXPECTED_LIST_PC_SEQUENCE];
        let actual: string[] = [];
        let passed = true;
        let assertionError: string | undefined;
        try {
          const match = findListPcContainingDisplayNames(doc, expected);
          expect(
            match.listPc.length,
            `At least one TablePCScales.ListPC must have length >= 3 (created PCs). ` +
              `ddIndex=${match.ddIndex} tableIndex=${match.tableIndex}`,
          ).toBeGreaterThanOrEqual(3);
          actual = assertPdt3223ListPcOrder(match);
          if (prechain.scaleCodeValue) {
            const scaleCodes = match.listPc.map((row) => String(row.ScaleCode ?? ''));
            TestRunSummary.registerPayload('listPcScaleCodes', {
              expectedScaleCode: prechain.scaleCodeValue,
              actualScaleCodes: scaleCodes,
            });
          }
        } catch (err) {
          passed = false;
          assertionError = err instanceof Error ? err.message : String(err);
          throw err;
        } finally {
          TestRunSummary.recordCheck({
            check: 'Invoice JSON ListPC ordered by Name ASC then Id ASC',
            expectedResult:
              'GET billing-run/generate-invoice-data → DD[].TablePCScales[].ListPC[].PC ' +
              `equals ${JSON.stringify(expected)} (PC #2, #3, #1). ` +
              'Wrong displayName sort: AAA, MMM, ZZZ. Wrong insert order: AAA, ZZZ, MMM.',
            actualResult: passed
              ? `As expected — ListPC.PC=${JSON.stringify(actual)} invoiceId=${billed.invoiceId}.`
              : `Not as expected — invoiceId=${billed.invoiceId}. ${assertionError ?? ''}`.trim(),
            passed,
          });
        }
      },
    );

    await test.step(
      'Download generated invoice PDF and record printed PC display-name order',
      async () => {
        const expected = [...PDT_3223_EXPECTED_LIST_PC_SEQUENCE];
        const pdf = await downloadPdt3223InvoicePdfAndReadPrintedPcOrder(Request, billed.invoiceId);
        TestRunSummary.registerPayload('invoicePdf', {
          invoiceId: billed.invoiceId,
          invoiceDocumentId: pdf.invoiceDocumentId,
          pdfPath: pdf.pdfPath,
          extractor: pdf.extractor,
          printedPcSequence: pdf.printedPcSequence,
          jsonExpectedListPcPcSequence: expected,
        });
        test.info().attach('PDT-3223 invoice PDF', {
          path: pdf.pdfPath,
          contentType: 'application/pdf',
        });
        const passed =
          pdf.printedPcSequence.length === expected.length &&
          expected.every((name, i) => pdf.printedPcSequence[i] === name);
        TestRunSummary.recordCheck({
          check: 'Invoice PDF printed PC order vs Name ASC then Id ASC',
          expectedResult:
            `Comfort template PDF should print ${JSON.stringify(expected)} ` +
            '(same as JSON ListPC). Wrong shown-name sort: AAA, MMM, ZZZ.',
          actualResult: passed
            ? `As expected — PDF PC order=${JSON.stringify(pdf.printedPcSequence)} ` +
              `extractor=${pdf.extractor} path=${pdf.pdfPath}`
            : `Not as expected — PDF PC order=${JSON.stringify(pdf.printedPcSequence)} ` +
              `extractor=${pdf.extractor} path=${pdf.pdfPath}`,
          passed,
        });
      },
    );

    await test.step('Attach test run summary', async () => {
      const extraLinks: Record<string, string[]> = {
        ...buildProductContractTabLinks(prechain.contractId),
        billingRunPdf: [buildPdt3223BillingRunPdfPreviewLink(billed.billingRunId)],
      };

      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3223_KEY,
        relevantEntityKeys: ['customer', 'product', 'productContract', 'billingRun', 'invoice'],
        extraLinks,
        snapshot: {
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          contractId: prechain.contractId,
          podIdentifier: prechain.podIdentifier,
          priceComponentIds: prechain.priceComponents.map((pc) => pc.id),
          invoiceTemplateId: prechain.invoiceTemplateId,
          expectedListPcPcSequence: [...PDT_3223_EXPECTED_LIST_PC_SEQUENCE],
        },
      });
    });
  });
});
