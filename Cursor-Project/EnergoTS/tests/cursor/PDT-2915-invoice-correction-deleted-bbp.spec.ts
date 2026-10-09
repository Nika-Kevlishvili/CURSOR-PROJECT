import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  assertPdt2915DeleteInvoicedBbpBlocked,
  buildPdt2915AttachmentSummary,
  recordPdt2915NonZeroDeltaCorrectionOutcome,
  createPdt2915VolumeCorrectionBillingRun,
  isPdt2915VolumeOnlyCorrection,
  pollPdt2915CorrectionDraftInvoices,
  resolvePdt2915InvoiceCorrectionParameters,
  runPdt2915ContractPrechain,
  runPdt2915SingleBbpInvoicedPrechain,
  setupPdt2915DeletedBbpBeforeBillingScenario,
} from './pdt-2915-invoice-correction-deleted-bbp.fixtures';

/**
 * PDT-2915 (Dev API): DELETED BBP1 with **800 kWh** vs ACTIVE BBP2 **1000 kWh** (non-zero delta) →
 * volume-only correction — records draft outcome for portal comparison.
 *
 * Main flow: contract → BBP1 (800) + BBP2 (1000) → DELETE BBP1 before billing → realize on BBP2 → correction.
 * Guard test: DELETE after invoice is blocked (HTTP 400) — documents Prod vs Dev data path.
 *
 * Reference spec(s):
 * - tests/billing/correction/correctionCases.spec.ts
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 */
test.describe('[PDT-2915]: Invoice correction — deleted billing-by-profile volume delta', { tag: '@billing' }, () => {
  test(
    '[PDT-2915]: Invoice correction - PODs without correction are included in the credit and debit note',
    async ({ Request, GeneratePayload, Responses, Endpoints }) => {
      test.setTimeout(25 * 60 * 1000);
      const fx = { Request, GeneratePayload, Responses, Endpoints };

      const pre = await test.step('Precondition: contract through POD activation', async () =>
        runPdt2915ContractPrechain(fx),
      );

      const scenario = await test.step(
        'Precondition: BBP1+BBP2, DELETE BBP1 before billing, realize invoice on BBP2',
        async () => setupPdt2915DeletedBbpBeforeBillingScenario(fx, pre),
      );

      const correctionBillingRunId = await test.step(
        'Create volume-only INVOICE_CORRECTION billing run',
        async () => createPdt2915VolumeCorrectionBillingRun(fx),
      );

      const poll = await test.step('Start correction billing and poll draft invoices', async () =>
        pollPdt2915CorrectionDraftInvoices(Request, correctionBillingRunId),
      );

      const outcome = await test.step('Observe: non-zero delta DELETED vs ACTIVE on correction drafts', async () => {
        const icp = resolvePdt2915InvoiceCorrectionParameters(poll.billingRunSnapshot);
        expect(
          isPdt2915VolumeOnlyCorrection(icp),
          `Correction run must be volume-only (invoiceCorrectionBillingRunParametersResponse=${JSON.stringify(icp ?? null)})`,
        ).toBe(true);

        return recordPdt2915NonZeroDeltaCorrectionOutcome(
          Request,
          poll,
          scenario.podIdentifier,
          scenario.deletedTotal,
          scenario.activeTotal,
        );
      });

      test.info().attach('[PDT-2915] scenario summary', {
        body: JSON.stringify(
          buildPdt2915AttachmentSummary({
            podIdentifier: scenario.podIdentifier,
            originalInvoiceId: scenario.originalInvoiceId,
            firstBbpId: scenario.firstBbp.id,
            secondBbpId: scenario.secondBbp.id,
            activeTotal: scenario.activeTotal,
            deletedTotal: scenario.deletedTotal,
            volumeDeltaKwh: outcome.volumeDeltaKwh,
            podLineCountOnDrafts: outcome.podLineCountOnDrafts,
            deleteAttempt: scenario.deleteAttempt,
            correctionBillingRunId,
            draftInvoiceCount: poll.draftInvoiceCount,
            draftInvoiceIds: poll.draftInvoiceIds,
            billingRunStatus: (poll.billingRunSnapshot.commonParameters as { status?: string })?.status,
            invoiceCorrectionParameters: resolvePdt2915InvoiceCorrectionParameters(poll.billingRunSnapshot),
            responsesLinks: reportGenerator.setLinksToResponses(Responses),
          }),
          null,
          2,
        ),
        contentType: 'application/json',
      });
    },
  );

  test('[PDT-2915]: DELETE invoiced billing-by-profile is blocked (API guard)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };

    const { bbpId } = await test.step('Precondition: contract, one BBP, realized invoice', async () =>
      runPdt2915SingleBbpInvoicedPrechain(fx),
    );

    await test.step('DELETE invoiced billing-by-profile must return HTTP 400', async () =>
      assertPdt2915DeleteInvoicedBbpBlocked(Request, bbpId),
    );
  });
});
