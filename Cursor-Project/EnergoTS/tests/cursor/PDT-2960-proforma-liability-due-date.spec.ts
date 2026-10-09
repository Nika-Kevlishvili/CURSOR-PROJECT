/**
 * PDT-2960 — AS-IS defect reproduction (goods order proforma path).
 *
 * Flow: proforma → start-generating → start-accounting (REAL proforma + auto liability).
 * Full payment → liability current_amount = 0 → GET goods-order/test-paid-order-invoices.
 *
 * AS-IS (bug open): paid-proforma batch sets invoice payment_deadline = current_date and syncs
 * liability due_date from invoice — due date CHANGES after payment/batch.
 * TO-BE: liability due date must NOT change after payment.
 *
 * This test PASSES while the defect is open. When fixed, flip assertions in
 * assertPdt2960AsIsDueDateChangedAfterPaidProformaBatch to:
 *   expect(dueDateAfter).toBe(dueDateBefore);
 *
 * Reference spec(s):
 * - tests/contractsAndOrders/goodsOrder.spec.ts
 * - tests/cursor/PDT-2815-customer-version-bug-repro.spec.ts
 * - tests/cursor/PDT-2459-payment-reverse-lpf-offset.spec.ts
 * - tests/cursor/pdt-2459-payment-reverse-lpf-offset.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  assertPdt2960AsIsDueDateChangedAfterPaidProformaBatch,
  buildPdt2960AttachmentSummary,
  createPdt2960Goods,
  createPdt2960GoodsOrder,
  getPdt2960LiabilityDueDate,
  payPdt2960LiabilityFullAmount,
  pollPdt2960LiabilityFullyPaid,
  pollPdt2960LiabilityForInvoice,
  runPdt2960ProformaAccountingChain,
  setupPdt2960ReceivablesBase,
  triggerPdt2960PaidProformaBatch,
  type Pdt2960ProformaChainResult,
} from './pdt-2960-proforma-liability-due-date.fixtures';

test.describe(
  '[PDT-2960]: Issued proforma invoice - liability due date after payment',
  { tag: ['@contractsAndOrders', '@receivableManagement', '@pdt-2960', '@dev'] },
  () => {
    test(
      '[PDT-2960]: Issued proforma invoice - After payment the liability\'s due date is changed',
      async ({ Request, GeneratePayload, Responses, Endpoints, receivableValidations, TestRunSummary }) => {
        test.setTimeout(20 * 60 * 1000);

        const fx = { Request, GeneratePayload, Responses, Endpoints, receivableValidations };

        await test.step('Precondition: customer, collection channel, payment package', async () => {
          await setupPdt2960ReceivablesBase(fx);
        });

        await test.step('Precondition: goods catalog item', async () => {
          await createPdt2960Goods(fx);
        });

        const goodsOrderId = await test.step('Precondition: goods order', async () =>
          createPdt2960GoodsOrder(fx),
        );

        const customerIdentifier = String(Responses.customer[0].identifier);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);

        const proformaInvoiceId = await test.step(
          'Action: issue proforma → start-generating → start-accounting (no issue-invoice)',
          async () => runPdt2960ProformaAccountingChain(fx, goodsOrderId),
        );

        const { liabilityId, dueDate: dueDateBefore, currentAmount, initialAmount } =
          await test.step('Assert: resolve proforma liability and snapshot dueDate before payment', async () => {
            const resolved = await pollPdt2960LiabilityForInvoice(
              fx,
              customerIdentifier,
              proformaInvoiceId,
            );
            expect(resolved.currentAmount, 'liability must have open balance before payment').toBeGreaterThan(
              0,
            );

            TestRunSummary.recordCheck({
              check: 'Liability due date before payment',
              expectedResult:
                'Proforma liability may have empty dueDate before paid-proforma batch (payment term deadline not synced yet).',
              actualResult: `As expected — liability ${resolved.liabilityId} dueDate=${resolved.dueDate || '(empty)'}, currentAmount=${resolved.currentAmount}.`,
              passed: true,
            });

            return resolved;
          });

        const liabilityAmount = currentAmount > 0 ? currentAmount : initialAmount;

        const { paymentId, paymentDate } = await test.step(
          'Action: full payment with paymentDate on/after issue date',
          async () => payPdt2960LiabilityFullAmount(fx, liabilityAmount, proformaInvoiceId),
        );

        await test.step('Assert: liability fully paid (currentAmount = 0)', async () => {
          await pollPdt2960LiabilityFullyPaid(fx, liabilityId);
          TestRunSummary.recordCheck({
            check: 'Liability fully offset after payment',
            expectedResult: 'currentAmount = 0 after full payment.',
            actualResult: `As expected — liability ${liabilityId} currentAmount=0 after payment ${paymentId}.`,
            passed: true,
          });
        });

        const batchInvoiceNumbers = await test.step(
          'Action: trigger paid proforma batch (GET goods-order/test-paid-order-invoices)',
          async () => triggerPdt2960PaidProformaBatch(fx),
        );

        const dueDateAfter = await test.step(
          'Assert AS-IS defect: liability due date changes after paid-proforma batch',
          async () => {
            const after = await getPdt2960LiabilityDueDate(fx, liabilityId);
            assertPdt2960AsIsDueDateChangedAfterPaidProformaBatch(
              dueDateBefore,
              after,
              paymentDate,
            );

            TestRunSummary.recordCheck({
              check: 'AS-IS — liability due date after paid-proforma batch',
              expectedResult:
                'TO-BE: dueDate unchanged. AS-IS (bug open): dueDate changes to payment/processing date.',
              actualResult: `AS-IS observed — dueDate before=${dueDateBefore}, after=${after}, paymentDate=${paymentDate}.`,
              passed: true,
            });

            return after;
          },
        );

        const snapshot: Pdt2960ProformaChainResult & {
          paymentId: number;
          paymentDate: string;
          dueDateAfter: string;
          batchInvoiceNumbers: string[];
        } = {
          goodsOrderId,
          proformaInvoiceId,
          customerLiabilityId: liabilityId,
          liabilityAmount,
          dueDateBefore,
          customerIdentifier,
          paymentId,
          paymentDate,
          dueDateAfter,
          batchInvoiceNumbers,
        };

        test.info().attach('[PDT-2960] proforma liability due-date repro summary', {
          body: JSON.stringify(buildPdt2960AttachmentSummary(snapshot), null, 2),
          contentType: 'application/json',
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-2960',
            relevantEntityKeys: [
              'customer',
              'goodsOrder',
              'invoice',
              'customerLiability',
              'payment',
            ],
            snapshot: {
              goodsOrderId,
              proformaInvoiceId,
              customerLiabilityId: liabilityId,
              paymentId,
              dueDateBefore,
              dueDateAfter,
              paymentDate,
              customerIdentifier,
            },
          });
        });
      },
    );
  },
);
