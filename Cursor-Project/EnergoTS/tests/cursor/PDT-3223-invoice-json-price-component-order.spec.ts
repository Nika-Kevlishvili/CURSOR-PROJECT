/**
 * PDT-3223 (Dev Done): Order price components in invoice JSON
 * `DD.TablePCScales.ListPC.PC` by Name ASC then Id ASC.
 *
 * Proves sort is Name (not Id-only, not displayName) via four **scale** PCs whose
 * create-order Ids disagree with Name order and two share the same Name.
 * SCALE detail_type fills TablePCScales (settlement would fill TablePCProfiles).
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3223-invoice-json-price-component-order.fixtures.ts
 * - tests/cursor/pdt-3072-invoice-correction-volume-change-scale-pc-group.fixtures.ts
 * - tests/billing/forVolumes/forVolumes.spec.ts
 * - tests/cursor/dev-volume-invoice-custom-template.fixtures.ts
 */

import { test } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3223_EXPECTED_PC_ORDER,
  assertPdt3223ListPcOrderByNameThenId,
  buildPdt3223AttachmentSummary,
  buildPdt3223InvoicePreviewLink,
  resolvePortalBase,
  runPdt3223BillingAndRealizeInvoice,
  runPdt3223ContractPrechain,
  type Pdt3223Fx,
} from './pdt-3223-invoice-json-price-component-order.fixtures';

test.describe('[PDT-3223]: Invoice JSON ListPC price component order', {
  tag: ['@billing', '@invoice'],
}, () => {
  test(
    '[PDT-3223]: Order price componenst in the invoice json in DD.TablePCScales.ListPC.PC to be ordered by Name ASC and then by Id ASC',
    async ({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures, TestRunSummary }) => {
      test.setTimeout(30 * 60 * 1000);
      const fx: Pdt3223Fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      const pre = await test.step(
        'Precondition: customer → 4 scale PCs (Name≠Id order) → terms → SLP POD/meter → product → contract → activate',
        async () => {
          const chain = await runPdt3223ContractPrechain(fx);
          TestRunSummary.registerPayload('priceComponents', {
            isolationToken: chain.isolationToken,
            createdInOrder: chain.priceComponents,
            expectedListPcPcOrder: chain.expectedPcOrder,
            scaleTariffId: chain.scaleTariffId,
            scaleCodeId: chain.scaleCodeId,
            note:
              'SCALE PCs (not settlement). Create order Id ASC ≠ Name ASC; two AAA names prove Id tie-break. ' +
              'ListPC[].PC is displayName; sort key is Name then Id. Expected: DDDD→CCCC→BBBB→AAAA.',
          });
          TestRunSummary.registerPayload('prechain', {
            period: chain.period,
            podId: chain.podId,
            podIdentifier: chain.podIdentifier,
            productId: chain.productId,
            contractId: chain.contractId,
            priceComponentIds: chain.priceComponentIds,
          });
          return chain;
        },
      );

      const invoiceId = await test.step(
        'Precondition: billing-by-scales (whole month) and FOR_VOLUMES realized invoice',
        async () => {
          const id = await runPdt3223BillingAndRealizeInvoice(fx, pre);
          TestRunSummary.registerPayload('invoice', { invoiceId: id });
          return id;
        },
      );

      const { sequences } = await test.step(
        'Assert: GET /billing-run/generate-invoice-data TablePCScales.ListPC[].PC order = Name ASC then Id ASC',
        async () => {
          const result = await assertPdt3223ListPcOrderByNameThenId(
            Request,
            invoiceId,
            pre.expectedPcOrder,
          );

          const observedOrders = result.sequences.map((s) => s.pcNames);
          TestRunSummary.recordCheck({
            check: 'TablePCScales.ListPC[].PC ordered by Name ASC then Id ASC',
            expectedResult:
              `Every TablePCScales.ListPC PC sequence equals [${PDT_3223_EXPECTED_PC_ORDER.join(', ')}] ` +
              '(proves Name+Id sort; not Id-only AAAA,BBBB,DDDD,CCCC; not display ASC AAAA,BBBB,CCCC,DDDD).',
            actualResult: `As expected — ${result.sequences.length} scale period(s): ${JSON.stringify(observedOrders)}`,
            passed: true,
          });

          return result;
        },
      );

      test.info().attach('[PDT-3223] ListPC order assertion summary', {
        body: JSON.stringify(
          buildPdt3223AttachmentSummary({
            invoiceId,
            podIdentifier: pre.podIdentifier,
            priceComponents: pre.priceComponents,
            expectedPcOrder: pre.expectedPcOrder,
            sequences,
            invoicePreviewUrl: buildPdt3223InvoicePreviewLink(invoiceId),
            portalBase: resolvePortalBase(),
          }),
          null,
          2,
        ),
        contentType: 'application/json',
      });

      await test.step('Attach test run summary', async () => {
        const portalBase = resolvePortalBase();
        const extraLinks: Record<string, string[]> = {
          ...buildProductContractTabLinks(pre.contractId, portalBase),
          invoice: [buildPdt3223InvoicePreviewLink(invoiceId)],
        };

        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3223',
          relevantEntityKeys: [
            'customer',
            'priceComponent',
            'product',
            'productContract',
            'billingRun',
            'invoice',
          ],
          extraLinks,
          snapshot: {
            invoiceId,
            podIdentifier: pre.podIdentifier,
            productId: pre.productId,
            contractId: pre.contractId,
            priceComponentIds: pre.priceComponentIds,
            priceComponents: pre.priceComponents,
            expectedPcOrder: pre.expectedPcOrder,
            listPcSequences: sequences,
            portalBase,
          },
        });
      });
    },
  );
});
