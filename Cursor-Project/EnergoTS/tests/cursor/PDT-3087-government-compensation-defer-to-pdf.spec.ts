/**
 * PDT-3087 — Defer Government Compensation to PDF Generation in Standard Billing (FOR_VOLUMES only)
 *
 * Correction flow tests (TC-BE-9, 18, 19, 20) moved to PDT-3127 spec.
 *
 * Reference spec(s):
 * - tests/cursor/dev-volume-billing-two-compensations.spec.ts / .fixtures.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - tests/billing/overTimeOneTime/overTimeOneTime.spec.ts (TC-BE-15 out-of-scope negative)
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts (TC-BE-3 mass import)
 *
 * Swagger refresh: already executed this session via update-swagger-specs.ps1 (dev/test/dev2/experiment/prod).
 * Backend TC: Cursor-Project/test_cases/Backend/Government_Compensation_Defer_To_PDF_Standard_Billing.md
 *
 * Product scope: government compensation PDF linking = FOR_VOLUMES standard billing only.
 * TC-BE-15 is a negative (non-FOR_VOLUMES must stay unlinked).
 * Permission TCs omitted by design (not in Backend TC file).
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import { randomGens } from '../../utils/randomGens';
import {
  PDT_3087_KEY,
  DEV_PORTAL_BASE,
  runDevVolCompContractAndBbpPrechain,
  createForVolumesDraftBillingRun,
  createStandardDraftBillingRun,
  createGovernmentCompensation,
  getCompensation,
  getInvoice,
  assertCompensationUnlinked,
  isNullishEntityRef,
  assertCompensationLinkedAtPdf,
  assertInvoiceCompensationApplied,
  assertInvoiceNoCompensationApplied,
  startGeneratingAndWaitGenerated,
  startAccountingAndWaitCompleted,
  terminateBillingRunAndWaitCancelled,
  pollPdfDocumentsPresent,
  exportInvoiceWorkbookOk,
  resolveEntityIdentifier,
  resolveCurrencyName,
  buildGovernmentCompensationMassImportBuffer,
  uploadGovernmentCompensationMassImport,
  pollGovernmentCompensationMassImportComplete,
  findCompensationByNumber,
  runOverTimeOneTimeContractPrechain,
  runMultiPodForVolumesPrechain,
  runDualCustomerDualContractPrechain,
  createForVolumesDraftBillingRunForContractList,
  createManualCustomerLiability,
  getCustomerLiability,
  resolveInvoiceCustomerLiabilityId,
  toAmountNumber,
  buildDevBillingRunPreviewLink,
  buildDevCompensationPreviewLink,
  buildDevInvoicePreviewLink,
  getBillingRunStatus,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';

const TEST_TIMEOUT_MS = 25 * 60 * 1000;

test.describe(
  '[PDT-3087]: Defer Government Compensation to PDF Generation in Standard Billing',
  { tag: ['@billing', '@compensations', '@PDT-3087'] },
  () => {
    test('[PDT-3087]: TC-BE-1 – Draft generation does not link government compensations', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step(
        'Precondition: FOR_VOLUMES contract + BBP chain (no compensations)',
        async () => runDevVolCompContractAndBbpPrechain(fx),
      );

      const billing = await test.step(
        'Precondition: FOR_VOLUMES billing run → start-billing → DRAFT',
        async () => createForVolumesDraftBillingRun(fx),
      );

      await test.step('Assert: draft has no compensation application', async () => {
        expect(billing.billingRunStatus).toBe('DRAFT');
        expect(billing.draftInvoiceIds.length).toBeGreaterThanOrEqual(1);
        const invoiceId = billing.draftInvoiceIds[0];
        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceNoCompensationApplied(invoice, 'TC-BE-1 draft invoice');

        const exportMeta = await exportInvoiceWorkbookOk(Request, billing.billingRunId);
        expect(exportMeta.byteLength, 'draft export must return a workbook').toBeGreaterThan(0);

        TestRunSummary.recordCheck({
          check: 'TC-BE-1 draft does not link compensations',
          expectedResult:
            'Billing DRAFT; invoice compensationIndex null/absent; no compensation applied; export OK.',
          actualResult: `As expected — status=${billing.billingRunStatus}, invoiceId=${invoiceId}, exportBytes=${exportMeta.byteLength}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice'],
          extraLinks: {
            ...buildProductContractTabLinks(pre.contractId, DEV_PORTAL_BASE),
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            invoice: billing.draftInvoiceIds.map(buildDevInvoicePreviewLink),
          },
          snapshot: {
            tc: 'TC-BE-1',
            customerId: pre.customerId,
            podId: pre.podId,
            billingRunId: billing.billingRunId,
            draftInvoiceIds: billing.draftInvoiceIds,
          },
        });
      });
    });

    test('[PDT-3087]: TC-BE-2 – Manual create of uninvoiced compensations after draft', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );

      const created = await test.step(
        'Create uninvoiced government compensation after draft',
        async () => {
          const statusBefore = await getBillingRunStatus(Request, billing.billingRunId);
          expect(statusBefore).toBe('DRAFT');
          return createGovernmentCompensation(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 50,
            reason: 'PDT-3087-TC-BE-2',
          });
        },
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      await test.step('Assert: compensation UNINVOICED; run stays DRAFT; draft unlinked', async () => {
        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-2 after create');
        const statusAfter = await getBillingRunStatus(Request, billing.billingRunId);
        expect(statusAfter, 'billing run must remain DRAFT').toBe('DRAFT');
        const invoice = await getInvoice(Request, billing.draftInvoiceIds[0]);
        assertInvoiceNoCompensationApplied(invoice, 'TC-BE-2 draft after create');

        TestRunSummary.recordCheck({
          check: 'TC-BE-2 upload after draft without cancel',
          expectedResult: 'POST compensation UNINVOICED/unlinked; billing remains DRAFT.',
          actualResult: `As expected — comp=${created.id} status=${comp.compensationStatus} run=${statusAfter}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: billing.draftInvoiceIds.map(buildDevInvoicePreviewLink),
          },
          snapshot: { tc: 'TC-BE-2', compensationId: created.id, billingRunId: billing.billingRunId },
        });
      });
    });

    test('[PDT-3087]: TC-BE-3 – Mass import GOVERNMENT_COMPENSATION after draft', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );

      const importNumber = `PDT3087-MI-${randomGens.generateRandomString(true, false, 8)}`;
      let compensationId = 0;

      await test.step('Mass import GOVERNMENT_COMPENSATION after draft', async () => {
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
          reason: 'PDT-3087-TC-BE-3',
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
        assertCompensationUnlinked(found!, 'TC-BE-3 imported');

        const statusAfter = await getBillingRunStatus(Request, billing.billingRunId);
        expect(statusAfter, 'billing run remains draft after mass import').toBe('DRAFT');

        TestRunSummary.recordCheck({
          check: 'TC-BE-3 mass import after draft',
          expectedResult: 'Upload 202; process success; compensation UNINVOICED; run DRAFT.',
          actualResult: `As expected — upload=${upload.status} comp=${compensationId} run=${statusAfter}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: compensationId
              ? [buildDevCompensationPreviewLink(compensationId)]
              : [],
          },
          snapshot: { tc: 'TC-BE-3', importNumber, compensationId, billingRunId: billing.billingRunId },
        });
      });
    });

    test('[PDT-3087]: TC-BE-4 – PDF start links matching compensations and sets index 0', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];
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
          reason: 'PDT-3087-TC-BE-4',
        }),
      );
      TestRunSummary.registerPayload('compensation', created.payload);

      await test.step('start-generating → assert link index 0', async () => {
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const pdfCount = await pollPdfDocumentsPresent(Request, billing.billingRunId);
        expect(pdfCount, 'pdf-documents after generating').toBeGreaterThan(0);

        const comp = await getCompensation(Request, created.id);
        assertCompensationLinkedAtPdf(comp, invoiceId, 'TC-BE-4');
        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceCompensationApplied(invoice, 'TC-BE-4');

        TestRunSummary.recordCheck({
          check: 'TC-BE-4 PDF links compensation index 0',
          expectedResult: 'Compensation linked to invoice; index=0; status still UNINVOICED.',
          actualResult: `As expected — invoice=${comp.invoice?.id} index=${comp.index} status=${comp.compensationStatus} pdfs=${pdfCount}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
          },
          snapshot: { tc: 'TC-BE-4', compensationId: created.id, invoiceId, billingRunId: billing.billingRunId },
        });
      });
    });

    test('[PDT-3087]: TC-BE-5 – Pre-uploaded compensations stay unlinked at draft and link at PDF', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );

      const created = await test.step(
        'Precondition: create compensation BEFORE billing run',
        async () =>
          createGovernmentCompensation(fx, {
            customerId: pre.customerId,
            podId: pre.podId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 40,
            reason: 'PDT-3087-TC-BE-5',
          }),
      );

      const billing = await test.step('Precondition: draft billing after compensation exists', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      await test.step('Assert: still unlinked after draft', async () => {
        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-5 after draft');
        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceNoCompensationApplied(invoice, 'TC-BE-5 draft');
        await exportInvoiceWorkbookOk(Request, billing.billingRunId);
      });

      await test.step('start-generating → linked index 0', async () => {
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const comp = await getCompensation(Request, created.id);
        assertCompensationLinkedAtPdf(comp, invoiceId, 'TC-BE-5 after PDF');
        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceCompensationApplied(invoice, 'TC-BE-5 after PDF');

        TestRunSummary.recordCheck({
          check: 'TC-BE-5 pre-upload: unlinked at draft, linked at PDF',
          expectedResult: 'After draft unlinked; after PDF index=0 linked.',
          actualResult: `As expected — index=${comp.index} invoice=${comp.invoice?.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-5', compensationId: created.id, invoiceId },
        });
      });
    });

    test('[PDT-3087]: TC-BE-6 – PDF succeeds with empty compensation when no matching rows', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: contract + BBP (no compensations)', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      await test.step('start-generating with empty match set', async () => {
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const pdfCount = await pollPdfDocumentsPresent(Request, billing.billingRunId);
        expect(pdfCount, 'PDF must exist without compensations').toBeGreaterThan(0);
        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceNoCompensationApplied(invoice, 'TC-BE-6 after PDF');

        TestRunSummary.recordCheck({
          check: 'TC-BE-6 empty compensation PDF path',
          expectedResult: 'PDF completes; compensationIndex null; no linked compensations.',
          actualResult: `As expected — pdfs=${pdfCount} isCompensationGenerated=${String(invoice.isCompensationGenerated)}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
          },
          snapshot: { tc: 'TC-BE-6', customerId: pre.customerId, billingRunId: billing.billingRunId },
        });
      });
    });

    test('[PDT-3087]: TC-BE-8 – Accounting marks compensations INVOICED with liability/receivable', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      const created = await test.step('Precondition: compensation + PDF link', async () => {
        const c = await createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 30,
          reason: 'PDT-3087-TC-BE-8',
        });
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const linked = await getCompensation(Request, c.id);
        assertCompensationLinkedAtPdf(linked, invoiceId, 'TC-BE-8 pre-accounting');
        return c;
      });

      await test.step('start-accounting → INVOICED + L/R populated', async () => {
        await startAccountingAndWaitCompleted(Request, billing.billingRunId);
        const comp = await getCompensation(Request, created.id);
        expect(comp.compensationStatus, 'TC-BE-8 INVOICED').toBe('INVOICED');
        expect(comp.liabilityForRecipient?.id, 'liabilityForRecipient').toBeTruthy();
        expect(comp.receivableForCustomer?.id, 'receivableForCustomer').toBeTruthy();
        expect(comp.invoice?.id, 'invoice remains attached').toBe(invoiceId);

        TestRunSummary.recordCheck({
          check: 'TC-BE-8 accounting first-time flow',
          expectedResult: 'INVOICED with recipient liability + customer receivable.',
          actualResult: `As expected — status=${comp.compensationStatus} L=${comp.liabilityForRecipient?.id} R=${comp.receivableForCustomer?.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-8', compensationId: created.id, invoiceId },
        });
      });
    });

    test('[PDT-3087]: TC-BE-10 – Compensation for unrelated POD is not linked at PDF', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: billed POD contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );

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

      const billing = await test.step('Precondition: draft billing (billed POD only)', async () =>
        createForVolumesDraftBillingRun(fx),
      );

      const created = await test.step(
        'Create compensation for unrelated POD',
        async () =>
          createGovernmentCompensation(fx, {
            customerId: pre.customerId,
            podId: unrelatedPodId,
            recipientId: pre.recipientCustomerId,
            documentPeriod: pre.documentPeriod,
            documentAmount: 20,
            reason: 'PDT-3087-TC-BE-10',
          }),
      );

      await test.step('PDF → unrelated compensation stays unlinked', async () => {
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-10 unrelated POD');

        TestRunSummary.recordCheck({
          check: 'TC-BE-10 unrelated POD not linked',
          expectedResult: 'Compensation for POD not on invoice remains UNINVOICED/unlinked.',
          actualResult: `As expected — status=${comp.compensationStatus} invoice=${JSON.stringify(comp.invoice)}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'pod', 'billingRun'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: {
            tc: 'TC-BE-10',
            billedPodId: pre.podId,
            unrelatedPodId,
            compensationId: created.id,
          },
        });
      });
    });

    test('[PDT-3087]: TC-BE-11 – Terminate before PDF keeps compensations UNINVOICED and reusable', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const created = await test.step('Precondition: compensation after draft', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 15,
          reason: 'PDT-3087-TC-BE-11',
        }),
      );

      await test.step('Terminate before PDF → UNINVOICED', async () => {
        await terminateBillingRunAndWaitCancelled(Request, billing.billingRunId);
        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-11 after terminate');
      });

      await test.step('Reuse compensation on new run PDF', async () => {
        const reuse = await createForVolumesDraftBillingRun(fx);
        await startGeneratingAndWaitGenerated(Request, reuse.billingRunId);
        const reuseInvoiceId = reuse.draftInvoiceIds[0];
        const linked = await getCompensation(Request, created.id);
        assertCompensationLinkedAtPdf(linked, reuseInvoiceId, 'TC-BE-11 reuse');

        TestRunSummary.recordCheck({
          check: 'TC-BE-11 terminate before PDF then reuse',
          expectedResult: 'After terminate UNINVOICED; later PDF links index 0.',
          actualResult: `As expected — linked invoice=${linked.invoice?.id} index=${linked.index}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            compensation: [buildDevCompensationPreviewLink(created.id)],
          },
          snapshot: { tc: 'TC-BE-11', compensationId: created.id, cancelledRunId: billing.billingRunId },
        });
      });
    });

    test('[PDT-3087]: TC-BE-12 – Terminate after PDF link before accounting unlinks compensations', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );
      const billing = await test.step('Precondition: draft billing', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];

      const created = await test.step('Precondition: compensation + PDF link (no accounting)', async () => {
        const c = await createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 18,
          reason: 'PDT-3087-TC-BE-12',
        });
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const linked = await getCompensation(Request, c.id);
        assertCompensationLinkedAtPdf(linked, invoiceId, 'TC-BE-12 pre-terminate');
        return c;
      });

      await test.step('Terminate after PDF → UNINVOICED unlinked', async () => {
        await terminateBillingRunAndWaitCancelled(Request, billing.billingRunId);
        const comp = await getCompensation(Request, created.id);
        assertCompensationUnlinked(comp, 'TC-BE-12 after terminate');
        expect(
          isNullishEntityRef(comp.liabilityForRecipient),
          'no recipient liability',
        ).toBe(true);
        expect(
          isNullishEntityRef(comp.receivableForCustomer),
          'no customer receivable',
        ).toBe(true);
      });

      await test.step('Reuse on subsequent PDF', async () => {
        const reuse = await createForVolumesDraftBillingRun(fx);
        await startGeneratingAndWaitGenerated(Request, reuse.billingRunId);
        const linked = await getCompensation(Request, created.id);
        assertCompensationLinkedAtPdf(linked, reuse.draftInvoiceIds[0], 'TC-BE-12 reuse');

        TestRunSummary.recordCheck({
          check: 'TC-BE-12 terminate after PDF unlinks for reuse',
          expectedResult: 'After terminate: UNINVOICED, invoice/index null; later PDF links again.',
          actualResult: `As expected — reuse index=${linked.index} invoice=${linked.invoice?.id}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            compensation: [buildDevCompensationPreviewLink(created.id)],
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
          },
          snapshot: { tc: 'TC-BE-12', compensationId: created.id },
        });
      });
    });

    test('[PDT-3087]: TC-BE-15 – Non-FOR_VOLUMES standard billing does not link compensations at PDF', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step(
        'Precondition: OVER_TIME_ONE_TIME product-contract chain (out of compensation scope)',
        async () => runOverTimeOneTimeContractPrechain(fx),
      );

      const billing = await test.step(
        'Precondition: STANDARD_BILLING draft without FOR_VOLUMES',
        async () => createStandardDraftBillingRun(fx, ['OVER_TIME_ONE_TIME']),
      );
      expect(
        billing.draftInvoiceIds.length,
        'non-FOR_VOLUMES standard run must produce ≥1 draft',
      ).toBeGreaterThanOrEqual(1);
      const invoiceId = billing.draftInvoiceIds[0];

      const created = await test.step('Precondition: matching uninvoiced compensation', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 10,
          reason: 'PDT-3087-TC-BE-15',
        }),
      );

      await test.step('PDF must leave compensation unlinked (volumes + correction only)', async () => {
        const before = await getCompensation(Request, created.id);
        assertCompensationUnlinked(before, 'TC-BE-15 pre-PDF');

        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        await pollPdfDocumentsPresent(Request, billing.billingRunId);

        const after = await getCompensation(Request, created.id);
        assertCompensationUnlinked(after, 'TC-BE-15 after PDF (out of scope)');
        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceNoCompensationApplied(invoice, 'TC-BE-15');

        TestRunSummary.recordCheck({
          check: 'TC-BE-15 non-FOR_VOLUMES must not link',
          expectedResult:
            'OVER_TIME_ONE_TIME standard PDF succeeds; compensation stays UNINVOICED/unlinked (scope = FOR_VOLUMES + correction only).',
          actualResult: `As expected — status=${after.compensationStatus} invoiceId=${after.invoice?.id ?? null} index=${after.index ?? null}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
          },
          snapshot: {
            tc: 'TC-BE-15',
            applicationModelType: pre.applicationModelType,
            compensationId: created.id,
            invoiceId,
            note: 'Product scope: FOR_VOLUMES + INVOICE_CORRECTION only',
          },
        });
      });
    });

    test('[PDT-3087]: TC-BE-7 – Multi-POD invoice applies compensations per POD', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step(
        'Precondition: multi-POD FOR_VOLUMES contract + dual BBP',
        async () => runMultiPodForVolumesPrechain(fx),
      );
      const billing = await test.step('Precondition: FOR_VOLUMES draft (multi-POD)', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      expect(billing.draftInvoiceIds.length, 'multi-POD run must produce ≥1 draft').toBeGreaterThanOrEqual(
        1,
      );
      const invoiceId = billing.draftInvoiceIds[0];

      const compA = await test.step('Create compensation for POD A', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podAId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 10,
          reason: 'PDT-3087-TC-BE-7-A',
        }),
      );
      const compB = await test.step('Create compensation for POD B', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podBId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 20,
          reason: 'PDT-3087-TC-BE-7-B',
        }),
      );

      await test.step('PDF → both POD compensations link to same invoice index 0', async () => {
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const linkedA = await getCompensation(Request, compA.id);
        const linkedB = await getCompensation(Request, compB.id);
        assertCompensationLinkedAtPdf(linkedA, invoiceId, 'TC-BE-7 compA');
        assertCompensationLinkedAtPdf(linkedB, invoiceId, 'TC-BE-7 compB');
        expect(linkedA.invoice?.id, 'both comps same invoice').toBe(linkedB.invoice?.id);

        const invoice = await getInvoice(Request, invoiceId);
        assertInvoiceCompensationApplied(invoice, 'TC-BE-7 multi-POD invoice');

        TestRunSummary.recordCheck({
          check: 'TC-BE-7 multi-POD per-POD compensation link',
          expectedResult:
            'Comp A (POD A) and Comp B (POD B) both linked to the multi-POD invoice with index=0.',
          actualResult: `As expected — invoice=${invoiceId} A.index=${linkedA.index} B.index=${linkedB.index}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'pod', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [
              buildDevCompensationPreviewLink(compA.id),
              buildDevCompensationPreviewLink(compB.id),
            ],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
          },
          snapshot: {
            tc: 'TC-BE-7',
            podAId: pre.podAId,
            podBId: pre.podBId,
            compAId: compA.id,
            compBId: compB.id,
            invoiceId,
          },
        });
      });
    });

    test('[PDT-3087]: TC-BE-16 – Accounting offset invoice liability priority', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };
      const COMP_AMOUNT = 30;
      const OTHER_LIABILITY_AMOUNT = 50;

      const pre = await test.step('Precondition: contract + BBP', async () =>
        runDevVolCompContractAndBbpPrechain(fx),
      );

      const otherLiability = await test.step(
        'Precondition: competing open non-invoice customer liability',
        async () =>
          createManualCustomerLiability(fx, {
            customerId: pre.customerId,
            initialAmount: OTHER_LIABILITY_AMOUNT,
            basisForIssuing: 'PDT-3087-TC-BE-16-other-liability',
          }),
      );
      TestRunSummary.registerPayload('otherLiability', otherLiability.payload);

      const otherBeforeCreate = await getCustomerLiability(Request, otherLiability.id);
      expect(toAmountNumber(otherBeforeCreate.initialAmount)).toBe(OTHER_LIABILITY_AMOUNT);
      expect(toAmountNumber(otherBeforeCreate.currentAmount)).toBe(OTHER_LIABILITY_AMOUNT);
      expect(
        isNullishEntityRef(otherBeforeCreate.invoiceResponse as { id?: number | null } | null),
        'competing liability must not be invoice-sourced',
      ).toBe(true);

      const billing = await test.step('Precondition: FOR_VOLUMES draft', async () =>
        createForVolumesDraftBillingRun(fx),
      );
      const invoiceId = billing.draftInvoiceIds[0];
      const invoiceBefore = await getInvoice(Request, invoiceId);
      const invoiceTotal = toAmountNumber(invoiceBefore.totalAmountIncludingVat);
      expect(invoiceTotal, 'invoice total must exceed compensation amount').toBeGreaterThan(
        COMP_AMOUNT,
      );

      const created = await test.step('Precondition: compensation + PDF link', async () => {
        const c = await createGovernmentCompensation(fx, {
          customerId: pre.customerId,
          podId: pre.podId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: COMP_AMOUNT,
          reason: 'PDT-3087-TC-BE-16',
        });
        await startGeneratingAndWaitGenerated(Request, billing.billingRunId);
        const linked = await getCompensation(Request, c.id);
        assertCompensationLinkedAtPdf(linked, invoiceId, 'TC-BE-16 pre-accounting');
        return c;
      });

      await test.step('Accounting → invoice liability preferred over other liability', async () => {
        const otherBefore = await getCustomerLiability(Request, otherLiability.id);
        const otherCurrentBefore = toAmountNumber(otherBefore.currentAmount);
        expect(otherCurrentBefore).toBe(OTHER_LIABILITY_AMOUNT);

        await startAccountingAndWaitCompleted(Request, billing.billingRunId);

        const comp = await getCompensation(Request, created.id);
        expect(comp.compensationStatus, 'TC-BE-16 INVOICED').toBe('INVOICED');
        expect(comp.receivableForCustomer?.id, 'receivableForCustomer').toBeTruthy();
        expect(comp.liabilityForRecipient?.id, 'liabilityForRecipient').toBeTruthy();

        const invoiceLiabilityId = await resolveInvoiceCustomerLiabilityId(Request, invoiceId);
        const invoiceLiability = await getCustomerLiability(Request, invoiceLiabilityId);
        const invoiceCurrentAfter = toAmountNumber(invoiceLiability.currentAmount);
        const invoiceInitial = toAmountNumber(invoiceLiability.initialAmount);

        const otherAfter = await getCustomerLiability(Request, otherLiability.id);
        const otherCurrentAfter = toAmountNumber(otherAfter.currentAmount);

        expect(
          otherCurrentAfter,
          'other (manual) liability must stay at 50 — not preferentially offset',
        ).toBe(otherCurrentBefore);

        expect(
          invoiceCurrentAfter,
          `invoice liability currentAmount must be reduced by compensation ${COMP_AMOUNT} (initial=${invoiceInitial})`,
        ).toBeCloseTo(invoiceInitial - COMP_AMOUNT, 2);

        TestRunSummary.recordCheck({
          check: 'TC-BE-16 invoice liability priority over other liability',
          expectedResult:
            'After accounting: compensation INVOICED; invoice liability reduced by 30; other liability remains 50.',
          actualResult: `As expected — invoiceL=${invoiceLiabilityId} invoiceCurrent=${invoiceCurrentAfter} otherCurrent=${otherCurrentAfter}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [buildDevCompensationPreviewLink(created.id)],
            invoice: [buildDevInvoicePreviewLink(invoiceId)],
          },
          snapshot: {
            tc: 'TC-BE-16',
            compensationId: created.id,
            otherLiabilityId: otherLiability.id,
            invoiceId,
          },
        });
      });
    });

    test('[PDT-3087]: TC-BE-17 – Multi-invoice terminate before PDF reuse', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(TEST_TIMEOUT_MS);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step(
        'Precondition: dual customer / dual contract / dual BBP',
        async () => runDualCustomerDualContractPrechain(fx),
      );

      const billing = await test.step(
        'Precondition: one FOR_VOLUMES run for both contracts (≥2 drafts)',
        async () =>
          createForVolumesDraftBillingRunForContractList(
            fx,
            [pre.contractANumber, pre.contractBNumber],
            2,
          ),
      );
      expect(billing.draftInvoiceIds.length).toBeGreaterThanOrEqual(2);

      const compA = await test.step('Create compensation A (customer A)', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerAId,
          podId: pre.podAId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 10,
          reason: 'PDT-3087-TC-BE-17-A',
        }),
      );
      const compB = await test.step('Create compensation B (customer B)', async () =>
        createGovernmentCompensation(fx, {
          customerId: pre.customerBId,
          podId: pre.podBId,
          recipientId: pre.recipientCustomerId,
          documentPeriod: pre.documentPeriod,
          documentAmount: 15,
          reason: 'PDT-3087-TC-BE-17-B',
        }),
      );

      await test.step('Terminate before PDF → both comps UNINVOICED', async () => {
        assertCompensationUnlinked(await getCompensation(Request, compA.id), 'TC-BE-17 A pre');
        assertCompensationUnlinked(await getCompensation(Request, compB.id), 'TC-BE-17 B pre');
        await terminateBillingRunAndWaitCancelled(Request, billing.billingRunId);
        assertCompensationUnlinked(await getCompensation(Request, compA.id), 'TC-BE-17 A after term');
        assertCompensationUnlinked(await getCompensation(Request, compB.id), 'TC-BE-17 B after term');
      });

      await test.step('Reuse compensation A on new contract-A-only PDF', async () => {
        const reuse = await createForVolumesDraftBillingRunForContractList(
          fx,
          [pre.contractANumber],
          1,
        );
        await startGeneratingAndWaitGenerated(Request, reuse.billingRunId);
        const linked = await getCompensation(Request, compA.id);
        assertCompensationLinkedAtPdf(linked, reuse.draftInvoiceIds[0], 'TC-BE-17 reuse A');

        TestRunSummary.recordCheck({
          check: 'TC-BE-17 multi-invoice terminate before PDF then reuse',
          expectedResult:
            'After terminate both comps UNINVOICED; later contract-A PDF links comp A index 0.',
          actualResult: `As expected — linked invoice=${linked.invoice?.id} index=${linked.index}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3087_KEY,
          relevantEntityKeys: ['customer', 'billingRun', 'invoice'],
          extraLinks: {
            billingRun: [buildDevBillingRunPreviewLink(billing.billingRunId)],
            compensation: [
              buildDevCompensationPreviewLink(compA.id),
              buildDevCompensationPreviewLink(compB.id),
            ],
          },
          snapshot: {
            tc: 'TC-BE-17',
            contractANumber: pre.contractANumber,
            contractBNumber: pre.contractBNumber,
            cancelledRunId: billing.billingRunId,
            compAId: compA.id,
            compBId: compB.id,
          },
        });
      });
    });

  },
);
