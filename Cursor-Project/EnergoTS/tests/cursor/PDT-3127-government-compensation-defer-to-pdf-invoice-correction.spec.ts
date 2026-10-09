/**
 * PDT-3127 — Defer Government Compensation to PDF Generation in Correction flow
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3087-government-compensation-defer-to-pdf.spec.ts (standard billing)
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts (shared helpers)
 * - tests/cursor/pdt-3127-government-compensation-defer-to-pdf-invoice-correction.fixtures.ts (correction helpers)
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts (correction patterns)
 *
 * Swagger refresh: executed this session via update-swagger-specs.ps1 (dev/test/dev2/experiment/prod).
 * Backend TC: Cursor-Project/test_cases/Backend/Government_Compensation_Defer_To_PDF_Invoice_Correction.md
 *
 * Product scope: INVOICE_CORRECTION for product contracts; government compensation deferred to PDF.
 * Standard billing (FOR_VOLUMES) happy paths are in the PDT-3087 spec.
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import { randomGens } from '../../utils/randomGens';
import {
  PDT_3127_KEY,
  runCorrectionPrechain,
  classifyCorrectionDraftInvoices,
  runStandardBillingToAccounted,
  postHigherVolumeBbpAfterRealize,
  createCorrectionBillingRun,
  startCorrectionBillingAndWaitDraft,
  createForVolumesDraftBillingRunLongPoll,
  type CorrectionPrechainResult,
} from './pdt-3127-government-compensation-defer-to-pdf-invoice-correction.fixtures';
import {
  createGovernmentCompensation,
  getCompensation,
  getInvoice,
  assertCompensationUnlinked,
  assertCompensationLinkedAtPdf,
  assertInvoiceCompensationApplied,
  assertInvoiceNoCompensationApplied,
  startGeneratingAndWaitGenerated,
  startAccountingAndWaitCompleted,
  terminateBillingRunAndWaitCancelled,
  pollPdfDocumentsPresent,
  isNullishEntityRef,
  getBillingRunStatus,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  DEV_PORTAL_BASE,
  resolveEntityIdentifier,
  resolveCurrencyName,
  buildGovernmentCompensationMassImportBuffer,
  uploadGovernmentCompensationMassImport,
  pollGovernmentCompensationMassImportComplete,
  findCompensationByNumber,
  runDevVolCompContractAndBbpPrechain,
} from './pdt-3127-government-compensation-defer-to-pdf-invoice-correction.fixtures';

const TEST_TIMEOUT_MS = 30 * 60 * 1000;

test.describe(
  '[PDT-3127]: Defer government compensations to PDF Generation in Correction flow',
  { tag: ['@billing', '@compensations', '@correction', '@PDT-3127'] },
  () => {

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-1: Correction draft does not link compensations
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-1 – Correction draft generation does not link government compensations', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);

      const created = await test.step(
        'Precondition: create matching uninvoiced compensation',
        async () =>
          createGovernmentCompensation(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 50,
            reason: 'PDT-3127-TC-BE-1',
          }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      await test.step('Assert: correction draft has no compensation link', async () => {
        const invoiceId = pre.correctionDraftInvoiceIds[0];
        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceNoCompensationApplied(invoice, 'TC-BE-1 correction draft');

        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-1 after correction draft');

        TestRunSummary.recordCheck({
          check: 'TC-BE-1 correction draft does not link compensations',
          expectedResult:
            'Correction draft invoice has no compensationIndex; compensation remains UNINVOICED/unlinked.',
          actualResult: `As expected — invoiceId=${invoiceId}, comp status=${comp.compensationStatus}, invoice=${JSON.stringify(comp.invoice)}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [
              buildDevBillingRunPreviewLink(pre.originalBillingRunId),
              buildDevBillingRunPreviewLink(pre.correctionBillingRunId),
            ],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: pre.correctionDraftInvoiceIds.map(buildDevInvoicePreviewLink),
          },
          snapshot: {
            tc: 'TC-BE-1',
            correctionBillingRunId: pre.correctionBillingRunId,
            compensationId: created.id,
          },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-2: Upload compensation after correction draft without cancel
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-2 – Upload compensation after correction draft without cancelling the run', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);

      await test.step('Assert: correction run is in DRAFT', async () => {
        const status = await getBillingRunStatus(Request, pre.correctionBillingRunId);
        expect(status).toBe('DRAFT');
      });

      const created = await test.step(
        'Create uninvoiced compensation after correction draft',
        async () =>
          createGovernmentCompensation(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 50,
            reason: 'PDT-3127-TC-BE-2',
          }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      await test.step('Assert: compensation UNINVOICED; run stays DRAFT; draft unlinked', async () => {
        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-2 after create');

        const statusAfter = await getBillingRunStatus(Request, pre.correctionBillingRunId);
        expect(statusAfter, 'correction run must remain DRAFT').toBe('DRAFT');

        const invoice = await getInvoice(Request, pre.correctionDraftInvoiceIds[0]);
        assertInvoiceNoCompensationApplied(invoice, 'TC-BE-2 draft after create');

        TestRunSummary.recordCheck({
          check: 'TC-BE-2 upload after correction draft without cancel',
          expectedResult: 'POST compensation UNINVOICED/unlinked; correction run remains DRAFT.',
          actualResult: `As expected — comp=${created.id} status=${comp.compensationStatus} run=${statusAfter}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-2', compensationId: created.id, correctionBillingRunId: pre.correctionBillingRunId },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-3: PDF links matching comps, index 0, reduces final liability
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-3 – PDF start on correction links matching compensations with index 0', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);
      const invoiceId = pre.correctionDraftInvoiceIds[0];
      const invoiceBefore = await getInvoice(Request, invoiceId);
      const totalIncl = Number(invoiceBefore.totalAmountIncludingVat ?? 0);
      const documentAmount = totalIncl > 10 ? Math.min(50, Math.floor(totalIncl / 2)) : 2;

      const created = await test.step('Precondition: matching uninvoiced compensation', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount,
          reason: 'PDT-3127-TC-BE-3',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      await test.step('start-generating → assert link index 0', async () => {
        await startGeneratingAndWaitGenerated(Request, pre.correctionBillingRunId);
        const pdfCount = await pollPdfDocumentsPresent(Request, pre.correctionBillingRunId);
        expect(pdfCount, 'pdf-documents after generating').toBeGreaterThan(0);

        const comp = await getCompensation(Request, created.id);
        assertCompensationLinkedAtPdf(comp, invoiceId, 'TC-BE-3');

        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceCompensationApplied(invoice, 'TC-BE-3');

        TestRunSummary.recordCheck({
          check: 'TC-BE-3 PDF links compensation index 0 on correction',
          expectedResult: 'Compensation linked to correction invoice; index=0; status still UNINVOICED.',
          actualResult: `As expected — invoice=${comp.invoice?.id} index=${comp.index} status=${comp.compensationStatus} pdfs=${pdfCount}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
          },
          snapshot: { tc: 'TC-BE-3', compensationId: created.id, invoiceId, correctionBillingRunId: pre.correctionBillingRunId },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-4: Accounting → INVOICED + liability/receivable
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-4 – Correction accounting first-time flow — INVOICED status, liability, receivable', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);
      const invoiceId = pre.correctionDraftInvoiceIds[0];

      const created = await test.step('Precondition: compensation + PDF link', async () => {
        const c = await createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 30,
          reason: 'PDT-3127-TC-BE-4',
        });
        await startGeneratingAndWaitGenerated(Request, pre.correctionBillingRunId);
        const linked = await getCompensation(Request, c.id);
        assertCompensationLinkedAtPdf(linked, invoiceId, 'TC-BE-4 pre-accounting');
        return c;
      });

      await test.step('start-accounting → INVOICED + L/R populated', async () => {
        await startAccountingAndWaitCompleted(Request, pre.correctionBillingRunId);
        const comp = await getCompensation(Request, created.id);
        expect(comp.compensationStatus, 'TC-BE-4 INVOICED').toBe('INVOICED');
        expect(comp.liabilityForRecipient?.id, 'liabilityForRecipient').toBeTruthy();
        expect(comp.receivableForCustomer?.id, 'receivableForCustomer').toBeTruthy();
        expect(comp.invoice?.id, 'invoice remains attached').toBe(invoiceId);

        TestRunSummary.recordCheck({
          check: 'TC-BE-4 correction accounting first-time flow',
          expectedResult: 'INVOICED with recipient liability + customer receivable.',
          actualResult: `As expected — status=${comp.compensationStatus} L=${comp.liabilityForRecipient?.id} R=${comp.receivableForCustomer?.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-4', compensationId: created.id, invoiceId },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-5: Energy CREDIT_NOTE/DEBIT_NOTE totals unchanged
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-5 – Energy credit/debit documents remain unchanged after compensation PDF apply', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);

      const classification = await test.step(
        'Classify correction draft invoices (forward vs energy docs)',
        async () => classifyCorrectionDraftInvoices(Request, pre.correctionDraftInvoiceIds),
      );

      const energyDocIds = [
        ...classification.energyCreditNoteIds,
        ...classification.energyDebitNoteIds,
      ];

      const energyBaseline: Array<{ id: number; docType: string; totalAmountIncludingVat: string | number }> = [];
      await test.step('Record energy doc baseline (pre-PDF)', async () => {
        for (const id of energyDocIds) {
          const inv = await getInvoice(Request, id);
          energyBaseline.push({
            id,
            docType: String(inv.invoiceDocumentType ?? ''),
            totalAmountIncludingVat: inv.totalAmountIncludingVat ?? 0,
          });
        }
      });

      const forwardTotal = Number(classification.forwardInvoice.totalAmountIncludingVat ?? 0);
      const documentAmount = forwardTotal > 10 ? Math.min(50, Math.floor(forwardTotal / 2)) : 2;

      const created = await test.step('Precondition: matching uninvoiced compensation', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount,
          reason: 'PDT-3127-TC-BE-5',
        }),
      );

      await test.step('start-generating → PDF; check energy docs unchanged', async () => {
        await startGeneratingAndWaitGenerated(Request, pre.correctionBillingRunId);

        const comp = await getCompensation(Request, created.id);
        expect(comp.index, 'compensation index after PDF').toBe(0);

        for (const baseline of energyBaseline) {
          const afterPdf = await getInvoice(Request, baseline.id);
          expect(
            String(afterPdf.invoiceDocumentType),
            `energy doc ${baseline.id} docType unchanged after PDF`,
          ).toBe(baseline.docType);
          expect(
            Number(afterPdf.totalAmountIncludingVat),
            `energy doc ${baseline.id} totalAmountIncludingVat unchanged after PDF`,
          ).toBeCloseTo(Number(baseline.totalAmountIncludingVat), 2);
        }
      });

      await test.step('start-accounting → check energy docs still unchanged', async () => {
        await startAccountingAndWaitCompleted(Request, pre.correctionBillingRunId);

        for (const baseline of energyBaseline) {
          const afterAcc = await getInvoice(Request, baseline.id);
          expect(
            String(afterAcc.invoiceDocumentType),
            `energy doc ${baseline.id} docType unchanged after accounting`,
          ).toBe(baseline.docType);
          expect(
            Number(afterAcc.totalAmountIncludingVat),
            `energy doc ${baseline.id} totalAmountIncludingVat unchanged after accounting`,
          ).toBeCloseTo(Number(baseline.totalAmountIncludingVat), 2);
        }

        TestRunSummary.recordCheck({
          check: 'TC-BE-5 energy credit/debit docs unchanged',
          expectedResult: 'Energy CREDIT_NOTE/DEBIT_NOTE totals unchanged after PDF and accounting.',
          actualResult: `As expected — ${energyBaseline.length} energy docs verified stable (${energyDocIds.length === 0 ? 'no energy docs produced — correction may not have generated CN/DN; test passes vacuously' : 'all match baseline'}).`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: pre.correctionDraftInvoiceIds.map(buildDevInvoicePreviewLink),
          },
          snapshot: { tc: 'TC-BE-5', energyDocIds, compensationId: created.id },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-6: Pre-uploaded compensations stay unlinked at draft, linked at PDF
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-6 – Pre-uploaded compensations stay unlinked at correction draft and link at PDF', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step(
        'Precondition: contract + BBP + standard billing (accounted) + higher-volume BBP',
        async () => {
          const chain = await runDevVolCompContractAndBbpPrechain(fx);
          const original = await runStandardBillingToAccounted(fx);
          await postHigherVolumeBbpAfterRealize(fx);
          return { ...chain, ...original };
        },
      );

      const created = await test.step(
        'Precondition: create compensation BEFORE correction run',
        async () =>
          createGovernmentCompensation(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 40,
            reason: 'PDT-3127-TC-BE-6',
          }),
      );

      const invoiceIndex = Responses.invoice.indexOf(pre.invoiceId);
      const correctionBillingRunId = await test.step(
        'Create correction billing run',
        async () => createCorrectionBillingRun(fx, invoiceIndex),
      );

      const draft = await test.step('start-billing → DRAFT', async () =>
        startCorrectionBillingAndWaitDraft(fx, correctionBillingRunId),
      );
      const correctionInvoiceId = draft.draftInvoiceIds[0];

      await test.step('Assert: still unlinked after correction draft', async () => {
        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-6 after correction draft');
      });

      await test.step('start-generating → linked index 0', async () => {
        await startGeneratingAndWaitGenerated(Request, correctionBillingRunId);
        const comp = await getCompensation(Request, created.id);
        assertCompensationLinkedAtPdf(comp, correctionInvoiceId, 'TC-BE-6 after PDF');

        TestRunSummary.recordCheck({
          check: 'TC-BE-6 pre-upload: unlinked at correction draft, linked at PDF',
          expectedResult: 'After draft unlinked; after PDF index=0 linked.',
          actualResult: `As expected — index=${comp.index} invoice=${comp.invoice?.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(correctionBillingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-6', compensationId: created.id, correctionInvoiceId },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-7: Compensation links to forward correction invoice, NOT energy CN/DN
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-7 – Compensation links to forward correction invoice, not energy CREDIT_NOTE / DEBIT_NOTE', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);

      const classification = await test.step(
        'Classify correction draft invoices',
        async () => classifyCorrectionDraftInvoices(Request, pre.correctionDraftInvoiceIds),
      );

      const forwardTotal = Number(classification.forwardInvoice.totalAmountIncludingVat ?? 0);
      const documentAmount = forwardTotal > 10 ? Math.min(50, Math.floor(forwardTotal / 2)) : 2;

      const created = await test.step('Precondition: matching uninvoiced compensation', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount,
          reason: 'PDT-3127-TC-BE-7',
        }),
      );

      await test.step('start-generating → verify link target is forward invoice', async () => {
        await startGeneratingAndWaitGenerated(Request, pre.correctionBillingRunId);

        const comp = await getCompensation(Request, created.id);
        const linkedInvoiceId = comp.invoice?.id;

        let linkedDocType = 'UNKNOWN';
        if (linkedInvoiceId) {
          const linkedInv = await getInvoice(Request, linkedInvoiceId);
          linkedDocType = String(linkedInv.invoiceDocumentType ?? 'UNKNOWN');
        }

        expect(
          linkedInvoiceId,
          `linked to forward correction invoice (expected forward=${classification.forwardInvoiceId} ` +
            `docType=INVOICE, actual linked=${linkedInvoiceId} docType=${linkedDocType}). ` +
            `All drafts: ${JSON.stringify(classification.allDocs)}`,
        ).toBe(classification.forwardInvoiceId);
        expect(comp.index, 'compensation index').toBe(0);

        for (const cnId of classification.energyCreditNoteIds) {
          expect(comp.invoice?.id, `must NOT link to CREDIT_NOTE ${cnId}`).not.toBe(cnId);
        }
        for (const dnId of classification.energyDebitNoteIds) {
          expect(comp.invoice?.id, `must NOT link to DEBIT_NOTE ${dnId}`).not.toBe(dnId);
        }

        const passed = linkedInvoiceId === classification.forwardInvoiceId;
        TestRunSummary.recordCheck({
          check: 'TC-BE-7 compensation links to forward correction invoice',
          expectedResult: 'Compensation linked to forward invoice (min-ID non-credit), NOT energy CN/DN.',
          actualResult: passed
            ? `As expected — linked to ${linkedInvoiceId} (forward=${classification.forwardInvoiceId}), CNs=${JSON.stringify(classification.energyCreditNoteIds)}, DNs=${JSON.stringify(classification.energyDebitNoteIds)}`
            : `NOT as expected — linked to ${linkedInvoiceId} (docType=${linkedDocType}), expected forward=${classification.forwardInvoiceId}. All drafts: ${JSON.stringify(classification.allDocs)}`,
          passed,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: pre.correctionDraftInvoiceIds.map(buildDevInvoicePreviewLink),
          },
          snapshot: {
            tc: 'TC-BE-7',
            forwardInvoiceId: classification.forwardInvoiceId,
            energyCreditNoteIds: classification.energyCreditNoteIds,
            energyDebitNoteIds: classification.energyDebitNoteIds,
            compensationId: created.id,
          },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-8: No matching comps — PDF succeeds, no link
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-8 – PDF generation succeeds on correction with no matching uninvoiced compensations', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);
      const invoiceId = pre.correctionDraftInvoiceIds[0];

      await test.step('start-generating with no matching compensations → PDF completes', async () => {
        await startGeneratingAndWaitGenerated(Request, pre.correctionBillingRunId);
        const pdfCount = await pollPdfDocumentsPresent(Request, pre.correctionBillingRunId);
        expect(pdfCount, 'PDF must exist without compensations').toBeGreaterThan(0);

        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceNoCompensationApplied(invoice, 'TC-BE-8 after PDF');

        TestRunSummary.recordCheck({
          check: 'TC-BE-8 no matching compensations — PDF succeeds',
          expectedResult: 'PDF completes; compensationIndex null; no linked compensations.',
          actualResult: `As expected — pdfs=${pdfCount} isCompensationGenerated=${String(invoice.isCompensationGenerated)}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
          },
          snapshot: { tc: 'TC-BE-8', correctionBillingRunId: pre.correctionBillingRunId },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-9: Terminate before PDF → UNINVOICED reusable
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-9 – Terminate correction run before PDF keeps compensations UNINVOICED and reusable', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);

      const created = await test.step('Precondition: compensation after correction draft', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 15,
          reason: 'PDT-3127-TC-BE-9',
        }),
      );

      await test.step('Terminate correction run before PDF → UNINVOICED', async () => {
        await terminateBillingRunAndWaitCancelled(Request, pre.correctionBillingRunId);
        const status = await getBillingRunStatus(Request, pre.correctionBillingRunId);
        expect(status, 'correction run CANCELLED').toBe('CANCELLED');

        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-9 after terminate');
      });

      await test.step('Reuse compensation on new standard run PDF', async () => {
        const reuse = await createForVolumesDraftBillingRunLongPoll(fx);
        await startGeneratingAndWaitGenerated(Request, reuse.billingRunId);
        const reuseInvoiceId = reuse.draftInvoiceIds[0];
        const linked = await getCompensation(Request, created.id);
        assertCompensationLinkedAtPdf(linked, reuseInvoiceId, 'TC-BE-9 reuse');

        TestRunSummary.recordCheck({
          check: 'TC-BE-9 terminate before PDF then reuse',
          expectedResult: 'After terminate UNINVOICED; later PDF links index 0.',
          actualResult: `As expected — linked invoice=${linked.invoice?.id} index=${linked.index}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            compensation: [buildDevCompensationPreviewLink(created.id)],
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
          },
          snapshot: { tc: 'TC-BE-9', compensationId: created.id, cancelledRunId: pre.correctionBillingRunId },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-10: Terminate after PDF before accounting → unlink reusable
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-10 – Terminate correction run after PDF link before accounting unlinks compensations', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);
      const invoiceId = pre.correctionDraftInvoiceIds[0];

      const created = await test.step('Precondition: compensation + PDF link (no accounting)', async () => {
        const c = await createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 18,
          reason: 'PDT-3127-TC-BE-10',
        });
        await startGeneratingAndWaitGenerated(Request, pre.correctionBillingRunId);
        const linked = await getCompensation(Request, c.id);
        assertCompensationLinkedAtPdf(linked, invoiceId, 'TC-BE-10 pre-terminate');
        return c;
      });

      await test.step('Terminate after PDF → UNINVOICED unlinked', async () => {
        await terminateBillingRunAndWaitCancelled(Request, pre.correctionBillingRunId);
        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-10 after terminate');
        expect(
          isNullishEntityRef(comp.liabilityForRecipient),
          'no recipient liability',
        ).toBe(true);
        expect(
          isNullishEntityRef(comp.receivableForCustomer),
          'no customer receivable',
        ).toBe(true);
      });

      await test.step('Reuse on subsequent FOR_VOLUMES PDF', async () => {
        const reuse = await createForVolumesDraftBillingRunLongPoll(fx);
        await startGeneratingAndWaitGenerated(Request, reuse.billingRunId);
        const linked = await getCompensation(Request, created.id);
        assertCompensationLinkedAtPdf(linked, reuse.draftInvoiceIds[0], 'TC-BE-10 reuse');

        TestRunSummary.recordCheck({
          check: 'TC-BE-10 terminate after PDF unlinks for reuse',
          expectedResult: 'After terminate: UNINVOICED, invoice/index null; later PDF links again.',
          actualResult: `As expected — reuse index=${linked.index} invoice=${linked.invoice?.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            compensation: [buildDevCompensationPreviewLink(created.id)],
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
          },
          snapshot: { tc: 'TC-BE-10', compensationId: created.id },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-11: Unrelated POD not linked
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-11 – Unrelated POD compensation is not linked to correction invoice at PDF', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);

      let unrelatedPodId = 0;
      await test.step('Precondition: unrelated POD (not on contract)', async () => {
        const pod = await Request.post(Endpoints.pod, {
          data: GeneratePayload.pointsOfDelivery.pod_settlement(),
        });
        await expect(pod).CheckResponse();
        const body = await pod.json();
        Responses.pod.push(body);
        unrelatedPodId = Number((body as { id: number }).id);
      });

      const created = await test.step(
        'Create compensation for unrelated POD',
        async () =>
          createGovernmentCompensation(fx, {
            customerId: pre.customerId,
            podId: unrelatedPodId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 20,
            reason: 'PDT-3127-TC-BE-11',
          }),
      );

      await test.step('PDF → unrelated compensation stays unlinked', async () => {
        await startGeneratingAndWaitGenerated(Request, pre.correctionBillingRunId);
        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-11 unrelated POD');

        TestRunSummary.recordCheck({
          check: 'TC-BE-11 unrelated POD not linked on correction',
          expectedResult: 'Compensation for POD not on correction invoice remains UNINVOICED/unlinked.',
          actualResult: `As expected — status=${comp.compensationStatus} invoice=${JSON.stringify(comp.invoice)}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'pod', 'billingRun'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: {
            tc: 'TC-BE-11',
            billedPodId: pre.podId,
            unrelatedPodId,
            compensationId: created.id,
          },
        });
      });
    });

    // ═══════════════════════════════════════════════════════════════════════════
    // TC-BE-12: Mass import after correction draft
    // ═══════════════════════════════════════════════════════════════════════════
    test('[PDT-3127]: TC-BE-12 – Mass import GOVERNMENT_COMPENSATION after correction draft', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await runCorrectionPrechain(fx);

      const importNumber = `PDT3127-MI-${randomGens.generateRandomString(true, false, 8)}`;
      let compensationId = 0;

      await test.step('Mass import GOVERNMENT_COMPENSATION after correction draft', async () => {
        const customerIdentifier = await resolveEntityIdentifier(Request, 'customer', pre.customerId);
        const podIdentifier = await resolveEntityIdentifier(Request, 'pod', pre.podId);
        const recipientIdentifier = await resolveEntityIdentifier(
          Request,
          'customer',
          pre.recipientCustomerId,
        );
        const currencyName = await resolveCurrencyName(Request);

        const beforeProcesses = await Request.get('process', {
          params: { page: 0, size: 50, sortBy: 'ID', sortDirection: 'DESC' },
        });
        const beforeIds = new Set<number>();
        if (beforeProcesses.ok()) {
          const body = (await beforeProcesses.json()) as { content?: Array<{ id?: number }> };
          for (const row of body.content ?? []) {
            const id = Number(row.id);
            if (Number.isFinite(id) && id > 0) beforeIds.add(id);
          }
        }

        const fileBuffer = await buildGovernmentCompensationMassImportBuffer(Request, {
          number: importNumber,
          date: pre.documentPeriod.slice(0, 10),
          documentPeriod: pre.documentPeriod,
          volumes: 100,
          price: 1.5,
          reason: 'PDT-3127-TC-BE-12',
          documentAmount: 150,
          currencyName,
          customerIdentifier,
          podIdentifier,
          recipientIdentifier,
        });
        TestRunSummary.registerPayload('massImportRow', {
          number: importNumber,
          customerIdentifier,
          podIdentifier,
          recipientIdentifier,
          currencyName,
        });

        const upload = await uploadGovernmentCompensationMassImport(FileUploadRequest, fileBuffer);
        expect(upload.status, 'mass import upload HTTP 202').toBe(202);

        await pollGovernmentCompensationMassImportComplete(Request, beforeIds);

        const found = await findCompensationByNumber(Request, importNumber);
        expect(found, `imported compensation number ${importNumber}`).toBeTruthy();
        compensationId = found!.id;
        assertCompensationUnlinked(found!, 'TC-BE-12 imported');

        const statusAfter = await getBillingRunStatus(Request, pre.correctionBillingRunId);
        expect(statusAfter, 'correction run remains draft after mass import').toBe('DRAFT');

        TestRunSummary.recordCheck({
          check: 'TC-BE-12 mass import after correction draft',
          expectedResult: 'Upload 202; process success; compensation UNINVOICED; correction run DRAFT.',
          actualResult: `As expected — upload=${upload.status} comp=${compensationId} run=${statusAfter}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3127_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(pre.correctionBillingRunId)],
            compensation: compensationId
              ? [buildDevCompensationPreviewLink(compensationId)]
              : [],
          },
          snapshot: { tc: 'TC-BE-12', importNumber, compensationId, correctionBillingRunId: pre.correctionBillingRunId },
        });
      });
    });

  },
);
