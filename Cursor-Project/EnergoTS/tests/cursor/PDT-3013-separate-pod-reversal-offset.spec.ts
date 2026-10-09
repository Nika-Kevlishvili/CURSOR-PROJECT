import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  assertPdt3013AutomaticReceivableLiabilityOffset,
  assertPdt3013OtherLiabilityUnchanged,
  buildPdt3013AttachmentSummary,
  collectPdt3013ReversalOffsetSnapshot,
  liabilityIdFromInvoice,
  PDT3013_POD_COUNT,
  readLiabilityCurrentAmount,
  runPdt3013ContractPrep,
  runPdt3013InvoiceReversal,
  runPdt3013VolumeBillingAndRealize,
} from './pdt-3013-separate-pod-reversal-offset.fixtures';

/**
 * PDT-3013 — automatic receivable↔liability offset after invoice reversal with separate invoice per POD.
 *
 * Run on Dev:
 *   $env:BASE_URL="http://10.236.20.11:8091"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-3013-separate-pod-reversal-offset.spec.ts --project=main
 *
 * Run on Test:
 *   $env:BASE_URL="https://testapps.energo-pro.bg/backend/phoenix-epres"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-3013-separate-pod-reversal-offset.spec.ts --project=main
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts
 * - tests/billing/reversal/reversal.spec.ts
 */
test.describe(
  '[PDT-3013]: No automatic offsetting when separate invoice per POD is enabled',
  { tag: ['@billing', '@receivableManagement', '@pdt-3013'] },
  () => {
    test.describe.configure({ mode: 'parallel' });

    test(
      '[PDT-3013] TC-BE-1: Reversal auto-offsets credit-note receivable and original liability — first POD invoice',
      async ({ Request, GeneratePayload, Responses, Endpoints }) => {
        test.setTimeout(30 * 60 * 1000);
        const fx = { Request, GeneratePayload, Responses, Endpoints };

        const prep = await test.step(
          `Precondition: contract with ${PDT3013_POD_COUNT} PODs and separate invoice per POD`,
          async () =>
            runPdt3013ContractPrep(fx, {
              workerIndex: test.info().workerIndex,
              iteration: test.info().repeatEachIndex,
            }),
        );

        const volumeInvoiceIds = await test.step(
          `Precondition: FOR_VOLUMES billing — expect ${PDT3013_POD_COUNT} REAL invoices`,
          async () => runPdt3013VolumeBillingAndRealize(fx, prep.anchor, PDT3013_POD_COUNT),
        );

        const originalInvoiceId = volumeInvoiceIds[0];

        const { creditNoteInvoiceId } = await test.step(
          'Action: reverse first POD volume invoice (credit note)',
          async () => runPdt3013InvoiceReversal(fx, 0),
        );

        const snapshot = await test.step('Collect offset snapshot', async () =>
          collectPdt3013ReversalOffsetSnapshot(Request, Endpoints, originalInvoiceId, creditNoteInvoiceId),
        );

        await test.step('Expected: automatic receivable↔liability offset (same POD)', async () =>
          assertPdt3013AutomaticReceivableLiabilityOffset(snapshot),
        );

        test.info().attach('[PDT-3013] TC-BE-1 snapshot', {
          body: JSON.stringify(
            buildPdt3013AttachmentSummary(prep, snapshot, reportGenerator.setLinksToResponses(Responses)),
            null,
            2,
          ),
          contentType: 'application/json',
        });
      },
    );

    test(
      '[PDT-3013] TC-BE-2: Reversal auto-offset — second POD volume invoice only',
      async ({ Request, GeneratePayload, Responses, Endpoints }) => {
        test.setTimeout(30 * 60 * 1000);
        const fx = { Request, GeneratePayload, Responses, Endpoints };

        const prep = await test.step('Precondition: 2 PODs + separate invoice per POD', async () =>
          runPdt3013ContractPrep(fx, {
            workerIndex: test.info().workerIndex,
            iteration: test.info().repeatEachIndex + 1000,
          }),
        );

        const volumeInvoiceIds = await test.step('Precondition: 2 REAL volume invoices', async () =>
          runPdt3013VolumeBillingAndRealize(fx, prep.anchor, PDT3013_POD_COUNT),
        );

        const invoiceIdA = volumeInvoiceIds[0];
        const invoiceIdB = volumeInvoiceIds[1];

        const invARes = await Request.get(`invoice?id=${invoiceIdA}`);
        await expect(invARes).CheckResponse();
        const liabilityIdA = liabilityIdFromInvoice(await invARes.json());

        const invBRes = await Request.get(`invoice?id=${invoiceIdB}`);
        await expect(invBRes).CheckResponse();
        const liabilityIdB = liabilityIdFromInvoice(await invBRes.json());
        const amountABefore = await readLiabilityCurrentAmount(Request, Endpoints, liabilityIdA);
        const amountBBefore = await readLiabilityCurrentAmount(Request, Endpoints, liabilityIdB);

        const { creditNoteInvoiceId } = await test.step('Action: reverse second POD invoice', async () =>
          runPdt3013InvoiceReversal(fx, 1),
        );

        const snapshot = await test.step('Collect offset snapshot for POD-B', async () =>
          collectPdt3013ReversalOffsetSnapshot(Request, Endpoints, invoiceIdB, creditNoteInvoiceId),
        );

        await test.step('Expected: offset on POD-B liability only', async () => {
          await assertPdt3013AutomaticReceivableLiabilityOffset(snapshot);
          await assertPdt3013OtherLiabilityUnchanged(Request, Endpoints, liabilityIdA, amountABefore);
        });

        test.info().attach('[PDT-3013] TC-BE-2 snapshot', {
          body: JSON.stringify(
            { ...buildPdt3013AttachmentSummary(prep, snapshot, reportGenerator.setLinksToResponses(Responses)), liabilityIdA, liabilityIdB, amountBBefore },
            null,
            2,
          ),
          contentType: 'application/json',
        });
      },
    );

    test(
      '[PDT-3013] TC-BE-6: Baseline — separateInvoiceForEachPod=false yields one combined invoice; reversal offset succeeds',
      async ({ Request, GeneratePayload, Responses, Endpoints }) => {
        test.setTimeout(30 * 60 * 1000);
        const fx = { Request, GeneratePayload, Responses, Endpoints };

        const prep = await test.step('Precondition: 2 PODs without separate invoice per POD', async () =>
          runPdt3013ContractPrep(fx, {
            workerIndex: test.info().workerIndex,
            iteration: test.info().repeatEachIndex + 2000,
            separateInvoicePerPod: false,
          }),
        );

        const volumeInvoiceIds = await test.step('Precondition: 1 combined REAL invoice', async () =>
          runPdt3013VolumeBillingAndRealize(fx, prep.anchor, 1),
        );

        const combinedInvoiceId = volumeInvoiceIds[0];

        const { creditNoteInvoiceId } = await test.step('Action: reverse combined invoice', async () =>
          runPdt3013InvoiceReversal(fx, 0),
        );

        const snapshot = await test.step('Collect offset snapshot', async () =>
          collectPdt3013ReversalOffsetSnapshot(Request, Endpoints, combinedInvoiceId, creditNoteInvoiceId),
        );

        await test.step('Expected: baseline automatic offset on combined invoice', async () =>
          assertPdt3013AutomaticReceivableLiabilityOffset(snapshot),
        );

        test.info().attach('[PDT-3013] TC-BE-6 baseline snapshot', {
          body: JSON.stringify(
            buildPdt3013AttachmentSummary(prep, snapshot, reportGenerator.setLinksToResponses(Responses)),
            null,
            2,
          ),
          contentType: 'application/json',
        });
      },
    );
  },
);
