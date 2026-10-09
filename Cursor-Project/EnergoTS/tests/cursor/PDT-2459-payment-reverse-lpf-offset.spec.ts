import { expect, test } from './cursor-test.fixtures';
import { attachManualVerificationLinks } from './shared/manual-verification-links.fixtures';
import {
  assertPdt2459LiabilityCurrentAmount,
  assertPdt2459LpfLiabilityCoveredAfterReverse,
  assertPdt2459NewReversalLpfCreated,
  assertPdt2459PaymentReversed,
  buildPdt2459AttachmentSummary,
  createPdt2459ReverseCustomer,
  findPdt2459LpfLiabilityId,
  getPdt2459LpfDetails,
  payPdt2459LpfLiability,
  preparePdt2459LpfFromOverdueLiability,
  reversePdt2459Payment,
  setupPdt2459ReceivablesBase,
  type Pdt2459LpfSnapshot,
} from './pdt-2459-payment-reverse-lpf-offset.fixtures';

/**
 * PDT-2459 (Dev): payment reverse must create REVERSAL_OF_LATE_PAYMENT_FINE and offset LPF liability via its receivable.
 *
 * Jira repro: overdue liability → LPF → pay LPF → reverse payment → new reversal LPF + liability covered.
 *
 * Reference spec(s):
 * - tests/receivableManagement/Payment/paymentReverse.spec.ts (REG-1034 LPF reverse assertions)
 * - tests/cursor/pdt-2187-payment-partner-export.fixtures.ts (setupLpfViaPaidManualLiability REG-1049 pattern)
 * - tests/cursor/PDT-2937-invoice-detailed-data-same-pc-name.spec.ts (cursor-test.fixtures + manual links)
 *
 * Run on Test (serial — global LPF job must not overlap between tests in this file):
 *   $env:BASE_URL="https://testapps.energo-pro.bg/backend/phoenix-epres"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-2459-payment-reverse-lpf-offset.spec.ts --project=main
 */
test.describe('[PDT-2459]: Payment reverse LPF offset', { tag: ['@receivableManagement', '@payment'] }, () => {
  test.describe.configure({ mode: 'serial' });

  test(
    '[PDT-2459] TC-BE-1: Payment reverse — LPF reversed and LPF liability covered by reversal receivable',
    async ({ Request, GeneratePayload, Responses, Endpoints, receivableValidations }) => {
      test.setTimeout(5 * 60 * 1000);

      const fx = { Request, GeneratePayload, Responses, Endpoints, receivableValidations };
      let snapshot: Pdt2459LpfSnapshot;

      await test.step('Precondition: customer, collection channel, payment package, daily interest rate', async () => {
        await setupPdt2459ReceivablesBase(fx);
      });

      const { lpfRecord, overdueLiabilityId } = await test.step(
        'Precondition: paid overdue liability, due shifted to yesterday, LPF generated',
        async () => preparePdt2459LpfFromOverdueLiability(fx),
      );
      const lpfId = Number(lpfRecord.id);
      const customerIdentifier = String(Responses.customer[0].identifier);

      const { lpfAmount, lpfLiabilityId } = await test.step(
        'Precondition: resolve LPF amount and linked liability',
        async () => {
          const details = await getPdt2459LpfDetails(fx, lpfId);
          const liabilityId = await findPdt2459LpfLiabilityId(fx, customerIdentifier, lpfId);
          return { ...details, lpfLiabilityId: liabilityId };
        },
      );

      const lpfPaymentId = await test.step(
        'Precondition: pay LPF liability with manual payment (sole Responses.payment entry for reverse)',
        async () => payPdt2459LpfLiability(fx, lpfAmount),
      );

      await test.step('Assert: LPF liability is fully covered before payment reverse', async () => {
        await assertPdt2459LiabilityCurrentAmount(
          fx,
          lpfLiabilityId,
          0,
          'LPF liability currentAmount before reverse',
        );
      });

      const reverseCustomerId = await test.step('Precondition: create second customer for payment reverse', async () =>
        createPdt2459ReverseCustomer(fx),
      );

      await test.step('Assert: Responses.payment[0] is LPF payment only (source payment not tracked)', async () => {
        expect(Responses.payment).toHaveLength(1);
        expect(Responses.payment[0]).toEqual(lpfPaymentId);
      });

      await test.step('Action: reverse LPF payment via POST /payment/cancel', async () => {
        await reversePdt2459Payment(fx);
        await receivableValidations.paymentValidation(lpfPaymentId);
      });

      await test.step('Assert: original payment status is REVERSED', async () => {
        await assertPdt2459PaymentReversed(fx, lpfPaymentId);
      });

      const { reversalLpfId, reversalLpfNumber } = await test.step(
        'Assert: new REVERSAL_OF_LATE_PAYMENT_FINE created and linked to original LPF',
        async () => assertPdt2459NewReversalLpfCreated(fx, lpfId, lpfAmount),
      );

      await test.step(
        'Assert: LPF liability covered via receivable from reversal LPF (currentAmount = 0, offset linked)',
        async () => {
          await assertPdt2459LpfLiabilityCoveredAfterReverse(
            fx,
            lpfId,
            lpfLiabilityId,
            reversalLpfId,
            lpfAmount,
          );
          await assertPdt2459LiabilityCurrentAmount(
            fx,
            lpfLiabilityId,
            0,
            'LPF liability currentAmount after reverse',
          );
        },
      );

      snapshot = {
        lpfId,
        lpfAmount,
        lpfLiabilityId,
        overdueLiabilityId,
        lpfPaymentId,
        reverseCustomerId,
        customerIdentifier,
        reversalLpfId,
        reversalLpfNumber,
      };

      test.info().attach('[PDT-2459] payment reverse LPF assertion summary', {
        body: JSON.stringify(buildPdt2459AttachmentSummary(snapshot), null, 2),
        contentType: 'application/json',
      });

      await test.step('Attach portal links for manual verification', async () => {
        attachManualVerificationLinks(Responses, {
          jiraKey: 'PDT-2459',
          snapshot: {
            paymentId: snapshot.lpfPaymentId,
            lpfId: snapshot.lpfId,
            reversalLpfId: snapshot.reversalLpfId,
            overdueLiabilityId: snapshot.overdueLiabilityId,
            lpfLiabilityId: snapshot.lpfLiabilityId,
            reverseCustomerId: snapshot.reverseCustomerId,
            customerIdentifier: snapshot.customerIdentifier,
            lpfAmount: snapshot.lpfAmount,
          },
        });
      });
    },
  );

  test(
    '[PDT-2459] TC-BE-1-setup: LPF paid — pre-reverse state (no payment cancel)',
    async ({ Request, GeneratePayload, Responses, Endpoints, receivableValidations }) => {
      test.setTimeout(5 * 60 * 1000);

      const fx = { Request, GeneratePayload, Responses, Endpoints, receivableValidations };

      await test.step('Precondition: customer, collection channel, payment package, daily interest rate', async () => {
        await setupPdt2459ReceivablesBase(fx);
      });

      const { lpfRecord, overdueLiabilityId } = await test.step(
        'Precondition: paid overdue liability, due shifted to yesterday, LPF generated',
        async () => preparePdt2459LpfFromOverdueLiability(fx),
      );
      const lpfId = Number(lpfRecord.id);
      const customerIdentifier = String(Responses.customer[0].identifier);

      const { lpfAmount, lpfLiabilityId } = await test.step(
        'Precondition: resolve LPF amount and linked liability',
        async () => {
          const details = await getPdt2459LpfDetails(fx, lpfId);
          const liabilityId = await findPdt2459LpfLiabilityId(fx, customerIdentifier, lpfId);
          return { ...details, lpfLiabilityId: liabilityId };
        },
      );

      const lpfPaymentId = await test.step(
        'Precondition: pay LPF liability with manual payment (sole Responses.payment entry)',
        async () => payPdt2459LpfLiability(fx, lpfAmount),
      );

      await test.step('Assert: LPF liability is fully covered (currentAmount = 0) — pre-reverse state', async () => {
        await assertPdt2459LiabilityCurrentAmount(
          fx,
          lpfLiabilityId,
          0,
          'LPF liability currentAmount after LPF payment (pre-reverse state)',
        );
      });

      await test.step('Assert: Responses.payment[0] is LPF payment only (no reverse performed)', async () => {
        expect(Responses.payment).toHaveLength(1);
        expect(Responses.payment[0]).toEqual(lpfPaymentId);
      });

      test.info().attach('[PDT-2459] LPF paid pre-reverse setup summary', {
        body: JSON.stringify(
          {
            jiraKey: 'PDT-2459',
            scenario: 'TC-BE-1-setup',
            overdueLiabilityId,
            lpfId,
            lpfLiabilityId,
            lpfAmount,
            lpfPaymentId,
            customerIdentifier,
          },
          null,
          2,
        ),
        contentType: 'application/json',
      });

      await test.step('Attach portal links for manual verification', async () => {
        attachManualVerificationLinks(Responses, {
          jiraKey: 'PDT-2459',
          snapshot: {
            paymentId: lpfPaymentId,
            lpfId,
            overdueLiabilityId,
            lpfLiabilityId,
            customerIdentifier,
            lpfAmount,
          },
        });
      });
    },
  );
});
