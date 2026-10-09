/**
 * PDT-3319 — Customer indicators - Average payment day for electricity bills.
 *
 * Population tests (TC-BE-1 … TC-BE-21 and TC-BE-22 after-job GET) poll
 * GET /customer-indicators/{customerId} until averagePaymentDay matches the
 * User Story integer, or fail after ~120s. There is no Phoenix POST to run
 * Customer Indicators (Finding 4). Same-day runs fail until the overnight job
 * writes reporting.customer_indicators.average_payment_day for these customers;
 * re-run after the job. D = Europe/Sofia today (approximation of next job date).
 *
 * GET does not recalculate averagePaymentDay. Number(null) is not used — null
 * is not treated as 0 except where a TC allows pre-job null (TC-BE-22 immediate).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3205-copy-easypay-to-virtual-pos.spec.ts
 * - tests/cursor/pdt-3205-copy-easypay-to-virtual-pos.fixtures.ts
 * - tests/cursor/pdt-3215-accounting-period-dates-and-more.fixtures.ts
 * - tests/cursor/PDT-3379-reconnection-taxes-newest-contract-billing-group.spec.ts
 * - tests/cursor/pdt-3379-reconnection-taxes-newest-contract-billing-group.fixtures.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/EXP-PARITY-01-pulling-shared-pod-handover.spec.ts
 * - tests/cursor/EXP-PARITY-03-interim-deduction-and-issue-date.spec.ts
 *
 * Swagger: Cursor-Project/config/swagger/dev/swagger-spec.json
 * GET /customer-indicators/{id} → CustomerIndicatorsResponse.averagePaymentDay (number).
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PDT_3319_BILLING_TIMEOUT_MS,
  PDT_3319_FORMULA_TIMEOUT_MS,
  PDT_3319_KEY,
  PDT_3319_RFD_TIMEOUT_MS,
  PDT_3319_TITLE,
  addSofiaDays,
  addSofiaMonths,
  assertAveragePaymentDay,
  averagesEqual,
  completeCorrectionBilling,
  completeCreditNoteNoLiability,
  completeDepositOnly,
  completeInterimAdvanceBilling,
  completeLpfOnlyOutOfWindowSource,
  completePenaltyAction,
  completeProformaOnly,
  completeReconnectionInvoiceOnly,
  completeReschedulingExclude,
  completeStandardVolumesBilling,
  completeTaxExtraOnly,
  createPaidManualCls,
  createUnpaidManualCl,
  deleteCustomerLiability,
  getCustomerIndicators,
  parseAveragePaymentDay,
  payAndOffsetLiability,
  payLiabilityLagDays,
  pdt3319RelevantKeys,
  prepareManualReceivablesBase,
  sharedAccountingPeriodForDate,
  sharedBgnCurrency,
  sharedCollectionChannel,
  sharedCustomer,
  sharedManualLiability,
  snapshotDateD,
  waitForCustomerIndicatorsSnapshot,
  type Pdt3319Fx,
} from './pdt-3319-customer-indicators-average-payment-days.fixtures';

test.describe.configure({ mode: 'default', fullyParallel: false });

test.describe(`[${PDT_3319_KEY}]: ${PDT_3319_TITLE}`, {
  tag: ['@customers', '@pdt-3319', '@dev'],
}, () => {
  test(
    `[${PDT_3319_KEY}] TC-BE-1: Two fully paid MANUAL CLs average 12 days (AC-21)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;

      await test.step('Precondition: two fully paid MANUAL CLs (9d + 15d)', async () => {
        const setup = await createPaidManualCls(fx, [
          {
            dueIso: addSofiaDays(d, -40),
            payIso: addSofiaDays(d, -31),
            occurrenceIso: addSofiaDays(d, -45),
            amount: 100.0,
          },
          {
            dueIso: addSofiaDays(d, -35),
            payIso: addSofiaDays(d, -20),
            occurrenceIso: addSofiaDays(d, -40),
            amount: 200.0,
          },
        ]);
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 12);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 12);
        TestRunSummary.recordCheck({
          check: 'TC-BE-1 averagePaymentDay after snapshot',
          expectedResult: 'HTTP 200; averagePaymentDay = 12 ((9+15)/2); customerId matches path',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay} customerId=${body.customerId}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, expectedAverage: 12, snapshotDateD: d },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-2: One unpaid overdue MANUAL CL averages 10 days (AC-22)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;

      await test.step('Precondition: one unpaid MANUAL CL due D-10', async () => {
        const setup = await createUnpaidManualCl(fx, {
          dueIso: addSofiaDays(d, -10),
          occurrenceIso: addSofiaDays(d, -15),
          amount: 150.0,
        });
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 10);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 10);
        TestRunSummary.recordCheck({
          check: 'TC-BE-2 unpaid overdue averagePaymentDay',
          expectedResult: 'HTTP 200; averagePaymentDay = 10 (D minus due date)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, expectedAverage: 10, snapshotDateD: d },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-3: Paid 8 days late plus unpaid not overdue averages 4 (AC-23)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;

      await test.step('Precondition: paid 8d late + unpaid future CL', async () => {
        const base = await prepareManualReceivablesBase(fx);
        customerId = base.customerId;
        const paidDue = addSofiaDays(d, -20);
        const payIso = addSofiaDays(d, -12);
        const openDue = addSofiaDays(d, 15);
        const clPaid = await sharedManualLiability(fx, {
          customerId,
          currencyId: base.currencyId,
          accountingPeriodId: await sharedAccountingPeriodForDate(fx, paidDue),
          dueIso: paidDue,
          occurrenceIso: addSofiaDays(d, -25),
          initialAmount: 110.0,
        });
        await sharedManualLiability(fx, {
          customerId,
          currencyId: base.currencyId,
          accountingPeriodId: await sharedAccountingPeriodForDate(fx, openDue),
          dueIso: openDue,
          occurrenceIso: addSofiaDays(d, -1),
          initialAmount: 120.0,
        });
        await payAndOffsetLiability(fx, {
          customerId,
          currencyId: base.currencyId,
          collectionChannelId: base.channelId,
          paymentDateIso: payIso,
          amount: 110.0,
          liabilityId: clPaid,
        });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 4);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 4);
        TestRunSummary.recordCheck({
          check: 'TC-BE-3 mixed paid + not-overdue unpaid',
          expectedResult: 'HTTP 200; averagePaymentDay = 4 ((8+0)/2)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, expectedAverage: 4, snapshotDateD: d },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-4: Paid 5 days late plus unpaid 15 days overdue averages 10 (AC-24)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;

      await test.step('Precondition: paid 5d late + unpaid overdue 15d', async () => {
        const base = await prepareManualReceivablesBase(fx);
        customerId = base.customerId;
        const paidDue = addSofiaDays(d, -25);
        const payIso = addSofiaDays(d, -20);
        const overdueDue = addSofiaDays(d, -15);
        const clPaid = await sharedManualLiability(fx, {
          customerId,
          currencyId: base.currencyId,
          accountingPeriodId: await sharedAccountingPeriodForDate(fx, paidDue),
          dueIso: paidDue,
          occurrenceIso: addSofiaDays(d, -30),
          initialAmount: 130.0,
        });
        await sharedManualLiability(fx, {
          customerId,
          currencyId: base.currencyId,
          accountingPeriodId: await sharedAccountingPeriodForDate(fx, overdueDue),
          dueIso: overdueDue,
          occurrenceIso: addSofiaDays(d, -18),
          initialAmount: 140.0,
        });
        await payAndOffsetLiability(fx, {
          customerId,
          currencyId: base.currencyId,
          collectionChannelId: base.channelId,
          paymentDateIso: payIso,
          amount: 130.0,
          liabilityId: clPaid,
        });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 10);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 10);
        TestRunSummary.recordCheck({
          check: 'TC-BE-4 mixed paid + unpaid overdue',
          expectedResult: 'HTTP 200; averagePaymentDay = 10 ((5+15)/2)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, expectedAverage: 10, snapshotDateD: d },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-5: Paid 3 days before due date floors at 0 not minus 3 (AC-25)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;

      await test.step('Precondition: MANUAL CL paid 3 days before due', async () => {
        const setup = await createPaidManualCls(fx, [
          {
            dueIso: addSofiaDays(d, -10),
            payIso: addSofiaDays(d, -13),
            occurrenceIso: addSofiaDays(d, -20),
            amount: 160.0,
          },
        ]);
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-5 paid-before-due floor at 0',
          expectedResult: 'HTTP 200; averagePaymentDay = 0 (not -3)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, expectedAverage: 0, snapshotDateD: d },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-6: Future unpaid due with paid 8-day CL averages 4 (AC-5, AC-18)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;

      await test.step('Precondition: paid 8d (D-30/D-22) + unpaid D+20', async () => {
        const base = await prepareManualReceivablesBase(fx);
        customerId = base.customerId;
        const paidDue = addSofiaDays(d, -30);
        const payIso = addSofiaDays(d, -22);
        const futureDue = addSofiaDays(d, 20);
        const clPaid = await sharedManualLiability(fx, {
          customerId,
          currencyId: base.currencyId,
          accountingPeriodId: await sharedAccountingPeriodForDate(fx, paidDue),
          dueIso: paidDue,
          occurrenceIso: addSofiaDays(d, -35),
          initialAmount: 170.0,
        });
        await sharedManualLiability(fx, {
          customerId,
          currencyId: base.currencyId,
          accountingPeriodId: await sharedAccountingPeriodForDate(fx, futureDue),
          dueIso: futureDue,
          occurrenceIso: addSofiaDays(d, -1),
          initialAmount: 180.0,
        });
        await payAndOffsetLiability(fx, {
          customerId,
          currencyId: base.currencyId,
          collectionChannelId: base.channelId,
          paymentDateIso: payIso,
          amount: 170.0,
          liabilityId: clPaid,
        });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 4);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 4);
        TestRunSummary.recordCheck({
          check: 'TC-BE-6 future unpaid in denominator',
          expectedResult: 'HTTP 200; averagePaymentDay = 4 ((8+0)/2)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, expectedAverage: 4, snapshotDateD: d },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-7: Customer with no included CLs stores averagePaymentDay 0 after refresh (AC-20)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: customer with no CLs / invoices / payments', async () => {
        const created = await sharedCustomer(fx);
        customerId = created.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        expect(parseAveragePaymentDay(body.averagePaymentDay), 'AC-20 after refresh is 0 not null').toBe(0);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-7 empty included set stores 0 after job',
          expectedResult: 'HTTP 200; averagePaymentDay = 0 (not null) after snapshot',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: ['customer'],
          snapshot: { customerId, expectedAverage: 0 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-8: STANDARD billing-run product-contract invoice liability is included (AC-7)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_BILLING_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: STANDARD_BILLING REAL invoice paid due+6', async () => {
        const billed = await completeStandardVolumesBilling(fx);
        customerId = billed.customerId;
        await payLiabilityLagDays(fx, { customerId, cl: billed.mainCl, lagDays: 6 });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('invoice', billed.invoice);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 6);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 6);
        TestRunSummary.recordCheck({
          check: 'TC-BE-8 STANDARD electricity CL included',
          expectedResult: 'HTTP 200; averagePaymentDay = 6 (tax extras excluded from denominator)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['invoice', 'billingRun', 'productContract']),
          snapshot: { customerId, expectedAverage: 6 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-9: INTERIM_AND_ADVANCE_PAYMENT invoice liability is included (AC-7)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_BILLING_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: MANUAL_INTERIM_AND_ADVANCE_PAYMENT paid due+11', async () => {
        const billed = await completeInterimAdvanceBilling(fx);
        customerId = billed.customerId;
        expect(String(billed.invoice.invoiceType)).toBe('INTERIM_AND_ADVANCE_PAYMENT');
        await payLiabilityLagDays(fx, { customerId, cl: billed.cl, lagDays: 11 });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 11);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 11);
        TestRunSummary.recordCheck({
          check: 'TC-BE-9 interim invoice CL included',
          expectedResult: 'HTTP 200; averagePaymentDay = 11',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['invoice', 'billingRun', 'productContract']),
          snapshot: { customerId, expectedAverage: 11 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-10: INVOICE_CORRECTION liability is included (AC-7)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_BILLING_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: STANDARD paid 0d + CORRECTION paid due+14', async () => {
        const billed = await completeCorrectionBilling(fx);
        customerId = billed.customerId;
        await payLiabilityLagDays(fx, { customerId, cl: billed.sourceCl, lagDays: 0 });
        await payLiabilityLagDays(fx, { customerId, cl: billed.correctionCl, lagDays: 14 });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 7);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 7);
        const actual = parseAveragePaymentDay(body.averagePaymentDay);
        expect(actual, 'do not accept 14 as an alternate pass').toBe(7);
        TestRunSummary.recordCheck({
          check: 'TC-BE-10 correction included with source 0d',
          expectedResult: 'HTTP 200; averagePaymentDay = 7 exactly ((14+0)/2), not 14',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['invoice', 'billingRun', 'productContract']),
          snapshot: { customerId, expectedAverage: 7 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-11: Penalty ACTION liability is included (AC-8)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_BILLING_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: ACTION penalty CL paid due+13', async () => {
        const created = await completePenaltyAction(fx);
        customerId = created.customerId;
        await payLiabilityLagDays(fx, { customerId, cl: created.cl, lagDays: 13 });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 13);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 13);
        TestRunSummary.recordCheck({
          check: 'TC-BE-11 ACTION penalty CL included',
          expectedResult: 'HTTP 200; averagePaymentDay = 13',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['action', 'productContract']),
          snapshot: { customerId, expectedAverage: 13 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-12: MANUAL creationType from POST /customer-liability is included (Kalina 2026-09-08)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;

      await test.step('Precondition: single MANUAL CL paid 7 days late', async () => {
        const setup = await createPaidManualCls(fx, [
          {
            dueIso: addSofiaDays(d, -21),
            payIso: addSofiaDays(d, -14),
            occurrenceIso: addSofiaDays(d, -28),
            amount: 77.0,
          },
        ]);
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 7);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 7);
        TestRunSummary.recordCheck({
          check: 'TC-BE-12 single MANUAL CL included',
          expectedResult: 'HTTP 200; averagePaymentDay = 7',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, expectedAverage: 7, snapshotDateD: d },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-13: Late Payment Fine only in the 12-month window averages 0 (AC-10)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_BILLING_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: source CL D-13 months + LPF in window', async () => {
        const setup = await completeLpfOnlyOutOfWindowSource(fx);
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-13 LPF excluded',
          expectedResult: 'HTTP 200; averagePaymentDay = 0 (LATE_PAYMENT_FINE not included)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['latePaymentFine']),
          snapshot: { customerId, expectedAverage: 0 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-14: Deposit-only averages 0 (AC-11)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: deposit + deposit/job DEPOSIT CL', async () => {
        const setup = await completeDepositOnly(fx);
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-14 deposit excluded',
          expectedResult: 'HTTP 200; averagePaymentDay = 0 (outgoingDocumentType DEPOSIT)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['deposit']),
          snapshot: { customerId, expectedAverage: 0 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-15: Pro forma invoice only averages 0 (AC-12)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_BILLING_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: goods-order issue-proforma-invoice only', async () => {
        const setup = await completeProformaOnly(fx);
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-15 proforma excluded',
          expectedResult: 'HTTP 200; averagePaymentDay = 0 (no issue-invoice)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['goodsOrder']),
          snapshot: { customerId, expectedAverage: 0 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-16: Reconnection invoice only averages 0 (AC-7, AC-13)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_RFD_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: Charge Fee RECONNECTION invoice; source CL D-13 months', async () => {
        const setup = await completeReconnectionInvoiceOnly(fx);
        customerId = setup.customerId;
        expect(String(setup.invoice.invoiceType)).toBe('RECONNECTION');
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('invoice', setup.invoice);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-16 InvoiceType.RECONNECTION excluded',
          expectedResult: 'HTTP 200; averagePaymentDay = 0 (identify via invoiceType RECONNECTION)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys([
            'invoice',
            'requestForDisconnection',
            'productContract',
          ]),
          snapshot: { customerId, expectedAverage: 0 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-17: Tax-only extra government CL averages 0 (AC-13)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_BILLING_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: STANDARD invoice, delete main CL, keep tax extra', async () => {
        const setup = await completeTaxExtraOnly(fx);
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-17 tax extra CL excluded',
          expectedResult: 'HTTP 200; averagePaymentDay = 0 after deleting main electricity CL',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['invoice', 'billingRun']),
          snapshot: { customerId, expectedAverage: 0 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-18: Deleted included CL is not averaged (AC-6)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;

      await test.step('Precondition: MANUAL CL D-8 then DELETE', async () => {
        const setup = await createUnpaidManualCl(fx, {
          dueIso: addSofiaDays(d, -8),
          occurrenceIso: addSofiaDays(d, -12),
          amount: 88.0,
        });
        customerId = setup.customerId;
        await deleteCustomerLiability(fx, setup.clId);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-18 DELETED CL excluded',
          expectedResult: 'HTTP 200; averagePaymentDay = 0 (status DELETED)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, expectedAverage: 0, snapshotDateD: d },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-19: Due date older than 12 months is excluded (AC-5)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;

      await test.step('Precondition: unpaid MANUAL CL due D-13 months', async () => {
        const setup = await createUnpaidManualCl(fx, {
          dueIso: addSofiaMonths(d, -13),
          occurrenceIso: addSofiaDays(addSofiaMonths(d, -13), -3),
          amount: 99.0,
        });
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-19 due older than 12 months excluded',
          expectedResult: 'HTTP 200; averagePaymentDay = 0 (AC-5 window)',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, expectedAverage: 0, snapshotDateD: d },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-20: Credit note that does not create a liability is not averaged (AC-7)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_BILLING_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: MANUAL_CREDIT_OR_DEBIT_NOTE CREDIT_NOTE', async () => {
        const setup = await completeCreditNoteNoLiability(fx);
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-20 credit note without CL excluded',
          expectedResult: 'HTTP 200; averagePaymentDay = 0',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['invoice', 'billingRun']),
          snapshot: { customerId, expectedAverage: 0 },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-21: Rescheduling instalment is excluded (AC-14 interpreted)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_BILLING_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      let customerId = 0;

      await test.step('Precondition: EXECUTED rescheduling of MANUAL CL', async () => {
        const setup = await completeReschedulingExclude(fx);
        customerId = setup.customerId;
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('Wait for overnight snapshot then GET /customer-indicators/{id}', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 0);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 0);
        TestRunSummary.recordCheck({
          check: 'TC-BE-21 rescheduling instalments excluded (Finding 6 interpreted AC-14)',
          expectedResult:
            'HTTP 200; averagePaymentDay = 0. If BA later includes rescheduling, this expected result is invalidated (Finding 6).',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay} (interpreted exclude)`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(['rescheduling']),
          snapshot: { customerId, expectedAverage: 0, finding: 'Finding 6 AC-14 interpreted exclude' },
        });
      });
    },
  );

  test(
    `[${PDT_3319_KEY}] TC-BE-22: GET without snapshot keeps stored averagePaymentDay; after job it updates (AC-3)`,
    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(PDT_3319_FORMULA_TIMEOUT_MS);
      const fx: Pdt3319Fx = { Request, GeneratePayload, Responses, Endpoints };
      const d = snapshotDateD();
      let customerId = 0;
      let storedBefore: number | null = null;

      await test.step('Precondition: GET before CLs then pay 8-day MANUAL (no snapshot wait)', async () => {
        const created = await sharedCustomer(fx);
        customerId = created.customerId;
        const before = await getCustomerIndicators(fx, customerId);
        storedBefore = parseAveragePaymentDay(before.body.averagePaymentDay);
        TestRunSummary.registerPayload('storedBefore', { averagePaymentDay: storedBefore });

        const currencyId = await sharedBgnCurrency(fx);
        const channelId = await sharedCollectionChannel(fx, currencyId);
        const dueIso = addSofiaDays(d, -18);
        const payIso = addSofiaDays(d, -10);
        const clId = await sharedManualLiability(fx, {
          customerId,
          currencyId,
          accountingPeriodId: await sharedAccountingPeriodForDate(fx, dueIso),
          dueIso,
          occurrenceIso: addSofiaDays(d, -25),
          initialAmount: 64.0,
        });
        await payAndOffsetLiability(fx, {
          customerId,
          currencyId,
          collectionChannelId: channelId,
          paymentDateIso: payIso,
          amount: 64.0,
          liabilityId: clId,
        });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
      });

      await test.step('GET immediately after create/pay (no snapshot wait)', async () => {
        const { body } = await getCustomerIndicators(fx, customerId);
        const immediate = parseAveragePaymentDay(body.averagePaymentDay);
        expect(
          averagesEqual(immediate, storedBefore),
          `immediate averagePaymentDay ${String(immediate)} must equal storedBefore ${String(storedBefore)}`,
        ).toBe(true);
        expect(immediate, 'immediate GET must not equal 8 (GET does not recalculate)').not.toBe(8);
        TestRunSummary.recordCheck({
          check: 'TC-BE-22 AC-3 immediate GET stays stale',
          expectedResult: `immediateValue equals storedBefore (${String(storedBefore)}) and is not 8`,
          actualResult: `As expected — immediate=${String(immediate)} storedBefore=${String(storedBefore)}`,
          passed: true,
        });
      });

      await test.step('Wait for overnight snapshot then GET again', async () => {
        await waitForCustomerIndicatorsSnapshot(fx, customerId, 8);
        const { body } = await getCustomerIndicators(fx, customerId);
        await assertAveragePaymentDay(body, customerId, 8);
        TestRunSummary.recordCheck({
          check: 'TC-BE-22 after-job averagePaymentDay',
          expectedResult: 'HTTP 200; afterJobValue = 8',
          actualResult: `As expected — averagePaymentDay=${body.averagePaymentDay}`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3319_KEY,
          relevantEntityKeys: pdt3319RelevantKeys(),
          snapshot: { customerId, storedBefore, expectedAfterJob: 8, snapshotDateD: d },
        });
      });
    },
  );
});
