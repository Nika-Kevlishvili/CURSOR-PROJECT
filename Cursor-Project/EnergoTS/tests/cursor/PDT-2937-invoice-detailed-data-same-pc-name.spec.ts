import { test } from './cursor-test.fixtures';
import {
  attachManualVerificationLinks,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_2937_SHARED_PC_NAME,
  assertPdt2937AllSameNamedPcRowsOnDetailedData,
  buildPdt2937AttachmentSummary,
  buildPdt2937InvoiceDetailedDataPreviewLink,
  runPdt2937BillingAndRealizeInvoice,
  runPdt2937ContractPrechain,
  summarizePdt2937DetailedRowsForPod,
} from './pdt-2937-invoice-detailed-data-same-pc-name.fixtures';

/**
 * PDT-2937 (Dev): invoice third tab (detailed-data) must list every price component row even when names match.
 *
 * Jira repro: 3 same-named settlement PCs (INVOICE_ONE each) in a price component group on the product.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2937-invoice-detailed-data-same-pc-name.fixtures.ts
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts
 * - tests/salesPortal/GET-PRODUCT-LIST-product-list.spec.ts (PC group creation)
 */
test.describe('[PDT-2937]: Invoice detailed-data same price component name', { tag: ['@billing', '@invoice'] }, () => {
  test(
    '[PDT-2937]: [Backend] Invoice third tab - Wrong price component names in detail data tab',
    async ({ Request, GeneratePayload, Responses, Endpoints }) => {
      test.setTimeout(25 * 60 * 1000);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step(
        'Precondition: contract chain with same-named PCs in PC group on product',
        async () => runPdt2937ContractPrechain(fx),
      );

      const invoiceId = await test.step('Precondition: billing-by-profile and FOR_VOLUMES realized invoice', async () =>
        runPdt2937BillingAndRealizeInvoice(fx, pre),
      );

      const { rowsForPod } = await test.step(
        'Assert: invoice/detailed-data returns all three same-named price component rows for POD',
        async () => assertPdt2937AllSameNamedPcRowsOnDetailedData(Request, invoiceId, pre.podIdentifier),
      );

      const priceComponentNames = rowsForPod.map((row) => String(row.priceComponent ?? '').trim());

      test.info().attach('[PDT-2937] detailed-data assertion summary', {
        body: JSON.stringify(
          buildPdt2937AttachmentSummary({
            invoiceId,
            podIdentifier: pre.podIdentifier,
            rowCountForPod: rowsForPod.length,
            priceComponentNames,
            sharedPcName: PDT_2937_SHARED_PC_NAME,
            priceComponentIds: pre.priceComponentIds,
            groupId: pre.groupId,
            contractId: pre.contractId,
            detailedRowsForPod: summarizePdt2937DetailedRowsForPod(rowsForPod),
            invoiceDetailedDataPreviewUrl: buildPdt2937InvoiceDetailedDataPreviewLink(invoiceId),
          }),
          null,
          2,
        ),
        contentType: 'application/json',
      });

      await test.step('Attach portal links for manual verification', async () => {
        const extra: Record<string, string[]> = {
          invoiceDetailedData: [buildPdt2937InvoiceDetailedDataPreviewLink(invoiceId)],
          ...buildProductContractTabLinks(pre.contractId),
        };

        attachManualVerificationLinks(Responses, {
          jiraKey: 'PDT-2937',
          snapshot: {
            invoiceId,
            podIdentifier: pre.podIdentifier,
            rowCountForPod: rowsForPod.length,
            priceComponentNames,
            sharedPcName: PDT_2937_SHARED_PC_NAME,
            priceComponentIds: pre.priceComponentIds,
            groupId: pre.groupId,
          },
          extraLinks: extra,
        });
      });
    },
  );
});
