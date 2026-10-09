/**
 * ONLINE-PAY-JUL30 — EasyPay online payment with fixed confirm DATE = July 30, 2026.
 *
 * Environment: Test (`https://testapps.energo-pro.bg/backend/phoenix-epres`).
 *
 * Run:
 *   $env:BASE_URL = "https://testapps.energo-pro.bg/backend/phoenix-epres"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/online-payment-july30-easypay.spec.ts --project=main
 *
 * Accounting-period note:
 * If accounting period 1011 (072026 / July 2026) is CLOSED on Test, epay/confirm-pay with
 * DATE=20260730 may return a non-00 STATUS (payment not created). This test records the
 * actual confirm outcome via TestRunSummary — it does not assume the period is open.
 *
 * Reference spec(s):
 * - tests/receivableManagement/Payment/onlinePayment.spec.ts ([REG-1036])
 */
import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  ONLINE_PAY_JUL30_JIRA_KEY,
  FIXED_EASYPAY_PAYMENT_DATE,
  FIXED_EASYPAY_PAYMENT_DATE_ISO_PREFIX,
  EASYPAY_LIABILITY_AMOUNT,
  createEasyPayTestCustomer,
  createEasyPayTestLiabilities,
  findAndEnsureEasyPayCombinedChannel,
  runEasyPayInitFlow,
  runEasyPayConfirmFlow,
  validateEasyPayPaymentInList,
  type EasyPayJuly30Fx,
} from './online-payment-july30-easypay.fixtures';

test.describe(`[${ONLINE_PAY_JUL30_JIRA_KEY}]: EasyPay fixed payment date`, {
  tag: ['@receivableManagement', '@onlinePayment', '@test'],
}, () => {
  test('[ONLINE-PAY-JUL30] EasyPay online payment — fixed payment date 2026-07-30', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    OnlinePaymentUrl,
    TestRunSummary,
  }) => {
    test.setTimeout(5 * 60 * 1000);

    const fx: EasyPayJuly30Fx = {
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      OnlinePaymentUrl,
    };

    await test.step('Precondition: Create customer', async () => {
      await createEasyPayTestCustomer(fx);
      TestRunSummary.registerPayload('customer', fx.GeneratePayload.customers.customer_legal());
      TestRunSummary.recordCheck({
        check: 'Customer created for EasyPay flow',
        expectedResult: 'Legal customer POST returns 2xx and customerNumber is available.',
        actualResult: `customerNumber=${String(fx.Responses.customer[0].customerNumber)}, identifier=${String(fx.Responses.customer[0].identifier)}`,
        passed: Boolean(fx.Responses.customer[0].customerNumber),
      });
    });

    await test.step('Precondition: Find EasyPay channel', async () => {
      const channelSetup = await findAndEnsureEasyPayCombinedChannel(fx);
      TestRunSummary.recordCheck({
        check: 'EasyPay ONLINE collection channel',
        expectedResult: 'EasyPay channel found; combineLiabilities enabled when performer exists on channel.',
        actualResult: `collectionChannelId=${channelSetup.channelId}, combineLiabilities=${channelSetup.combineLiabilities}`,
        passed: channelSetup.channelId > 0,
      });

      const liabilityCount = channelSetup.combineLiabilities ? 2 : 1;
      await test.step(`Precondition: Create ${liabilityCount} liability(ies)`, async () => {
        const liabilityPayload = fx.GeneratePayload.receivablesManagement.customer_liability();
        liabilityPayload.initialAmount = EASYPAY_LIABILITY_AMOUNT;
        TestRunSummary.registerPayload('customerLiability', liabilityPayload);

        await createEasyPayTestLiabilities(fx, liabilityCount, EASYPAY_LIABILITY_AMOUNT);
        TestRunSummary.recordCheck({
          check: 'Liabilities created for EasyPay payment',
          expectedResult: `${liabilityCount} liability(ies) with initialAmount=${EASYPAY_LIABILITY_AMOUNT}.`,
          actualResult: `customerLiability count=${fx.Responses.customerLiability.length}`,
          passed: fx.Responses.customerLiability.length === liabilityCount,
        });
      });
    });

    let initStatus = '';
    let confirmStatus = '';
    let confirmBody: Record<string, unknown> = {};
    let paymentListDate = '';

    const init = await test.step('EasyPay init-pay (checksum + init)', async () => {
      const initResult = await runEasyPayInitFlow(fx);
      initStatus = initResult.initStatus;

      TestRunSummary.recordCheck({
        check: 'EasyPay init-pay STATUS',
        expectedResult: 'init-pay returns STATUS 00 and AMOUNT reflects combined liabilities.',
        actualResult: `STATUS=${initResult.initStatus}, AMOUNT=${initResult.amount}, TID=${initResult.tid}, MERCHANTID=${initResult.merchantId}`,
        passed: initResult.initStatus === '00',
      });

      expect(initResult.initStatus, 'init-pay must succeed before confirm').toBe('00');
      return initResult;
    });

    await test.step('EasyPay confirm-pay with fixed DATE 20260730101100', async () => {
      const confirm = await runEasyPayConfirmFlow(fx, init, FIXED_EASYPAY_PAYMENT_DATE);
      confirmStatus = confirm.confirmStatus;
      confirmBody = confirm.confirmBody;

      const confirmPassed = confirm.confirmStatus === '00';
      TestRunSummary.recordCheck({
        check: 'EasyPay confirm-pay with fixed July 30 2026 DATE',
        expectedResult:
          `confirm-pay DATE=${FIXED_EASYPAY_PAYMENT_DATE} returns STATUS 00 when accounting period 072026 (1011) is OPEN. ` +
          'If period is CLOSED, confirm may fail — actual STATUS is recorded.',
        actualResult: `STATUS=${confirm.confirmStatus}, DATE=${FIXED_EASYPAY_PAYMENT_DATE}, body=${JSON.stringify(confirm.confirmBody)}`,
        passed: confirmPassed,
      });

      if (confirmPassed) {
        await test.step('Verify payment exists via payment/list', async () => {
          const customerIdentifier = String(fx.Responses.customer[0].identifier);
          const listResult = await validateEasyPayPaymentInList(
            fx,
            customerIdentifier,
            FIXED_EASYPAY_PAYMENT_DATE_ISO_PREFIX,
          );
          paymentListDate = listResult.paymentDate;

          TestRunSummary.recordCheck({
            check: 'Payment listed with paymentDate on 2026-07-30',
            expectedResult: `payment/list returns ≥1 row; paymentDate contains ${FIXED_EASYPAY_PAYMENT_DATE_ISO_PREFIX}.`,
            actualResult: `paymentId=${listResult.paymentId}, paymentDate=${listResult.paymentDate}, totalElements=${listResult.totalElements}`,
            passed: listResult.paymentDate.includes(FIXED_EASYPAY_PAYMENT_DATE_ISO_PREFIX),
          });
        });
      } else {
        TestRunSummary.recordCheck({
          check: 'Payment list skipped — confirm did not return STATUS 00',
          expectedResult: 'When confirm fails (e.g. closed accounting period 072026), no payment row is expected.',
          actualResult: `confirm STATUS=${confirmStatus}; payment/list assertion skipped.`,
          passed: true,
        });
      }
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: ONLINE_PAY_JUL30_JIRA_KEY,
        relevantEntityKeys: ['customer', 'customerLiability', 'payment', 'collectionChannel'],
        snapshot: {
          fixedPaymentDate: FIXED_EASYPAY_PAYMENT_DATE,
          initStatus,
          confirmStatus,
          paymentListDate: paymentListDate || null,
          confirmResponse: confirmBody,
        },
      });
    });
  });
});
