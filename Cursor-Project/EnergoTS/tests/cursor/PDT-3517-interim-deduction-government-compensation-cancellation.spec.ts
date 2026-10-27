/**
 * PDT-3517 — Invoice Cancellation / Government Compensations - Interim Deduction Flow.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3368-invoice-cancellation-government-compensations.spec.ts
 * - tests/cursor/pdt-3368-invoice-cancellation-government-compensations.fixtures.ts
 *   (postCancelByInvoiceNumber, waitProcessFinished, downloadProcessReportText,
 *    createExecutedReschedulingForLiability, getCompensation, assertAmountZero)
 * - tests/cursor/exp-parity-03-interim-deduction-and-issue-date.fixtures.ts
 *   (interim then FOR_VOLUMES, isDeducted)
 * - tests/cursor/pdt-3470-standard-credit-note-government-compensation-receivable.fixtures.ts
 *   (createPdt3470Compensation + FOR_VOLUMES accounting; buildPdt3470IapRealChain)
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 *   (government-compensation create payload, start-generating / start-accounting)
 * - tests/cursor/GC-REG-government-compensation-process.spec.ts
 *
 * Backend TC: Cursor-Project/test_cases/Backend/PDT-3517_interim_deduction_government_compensation_cancellation.md
 * Target env: dev. Swagger: Cursor-Project/config/swagger/dev/swagger-spec.json (refreshed this session).
 * templateId: envVariables.invoice_cancellation_template (same as PDT-3368).
 *
 * Volume data follows PDT-3470 billing-by-profile (expression 10). TC text says billing-by-scales.
 * Contract type is COMBINED. TC text says SUPPLY_ONLY.
 */

import { test, expect } from './cursor-test.fixtures';
import { randomGens } from '../../utils/randomGens';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import type { TestRunSummaryCollector } from './shared/test-run-summary.fixtures';
import { downloadProcessReportText, getCustomerLiability } from './pdt-3368-invoice-cancellation-government-compensations.fixtures';
import { money } from './pdt-3470-standard-credit-note-government-compensation-receivable.fixtures';
import {
  CLOSED_TAX_EVENT_DATE,
  INTERIM_NOT_IN_LIST_FRAGMENT,
  PDT_3517_C_AMOUNT,
  PDT_3517_CREDIT_IAP,
  PDT_3517_CREDIT_KWH,
  PDT_3517_DEBIT_IAP,
  PDT_3517_DEBIT_KWH,
  PDT_3517_KEY,
  PDT_3517_PAYMENT_ON_REMAINING,
  PDT_3517_TEST_TIMEOUT_MS,
  PDT_3517_TITLE,
  PDT_3517_TWO_IAP_AMOUNTS,
  PDT_3517_TWO_KWH,
  RESCHEDULING_BLOCK_FRAGMENT,
  TAX_EVENT_OPEN_PERIOD_FRAGMENT,
  TAX_EVENT_REQUIRED_FRAGMENT,
  assertCancellationRejected,
  assertCompensationUnwind,
  assertDateOutsideOpenPeriods,
  buildPdt3517AccountedChain,
  buildPdt3517EqualDraft,
  executeReschedulingOnGovernmentLiability,
  expectCancellationCreated,
  extraPortalLinks,
  offsetPaymentOntoLiability,
  releaseLiabilityPaymentShields,
  resolveOpenTaxEventDate,
  sameMoney,
  snapshotFinancials,
  type Pdt3517Chain,
  type Pdt3517EqualDraft,
  type Pdt3517Fx,
} from './pdt-3517-interim-deduction-government-compensation-cancellation.fixtures';

function registerChain(TestRunSummary: TestRunSummaryCollector, chain: Pdt3517Chain): void {
  TestRunSummary.registerPayload('customer', {
    billedCustomerId: chain.billedCustomerId,
    recipientId: chain.recipientId,
  });
  TestRunSummary.registerPayload('compensation', {
    id: chain.compensationId,
    number: chain.compensationNumber,
    C: chain.C,
    sign: chain.sign,
  });
  TestRunSummary.registerPayload('invoice', {
    remainingInvoiceId: chain.remainingInvoiceId,
    remainingNumber: chain.remainingNumber,
    remainingDocumentType: chain.remainingDocumentType,
    R: chain.R,
    interims: chain.interims.map((row) => ({
      id: row.id,
      number: row.number,
      initialAmount: row.initialAmount,
    })),
  });
}

function attachSummary(
  TestRunSummary: TestRunSummaryCollector,
  Responses: Parameters<typeof finalizeTestRunSummary>[1],
  opts: {
    chain?: Pdt3517Chain;
    equalDraft?: Pdt3517EqualDraft;
    processId?: number;
    keys?: string[];
    check: string;
    expectedResult: string;
    actualResult: string;
  },
): void {
  TestRunSummary.recordCheck({
    check: opts.check,
    expectedResult: opts.expectedResult,
    actualResult: opts.actualResult,
    passed: true,
  });
  const extra = extraPortalLinks({
    gcIds: [opts.chain?.compensationId ?? opts.equalDraft?.compensationId ?? 0].filter((id) => id > 0),
    invoiceId: opts.chain?.remainingInvoiceId,
    billingRunId: opts.chain?.volumeBillingRunId ?? opts.equalDraft?.volumeBillingRunId,
    processId: opts.processId,
  });
  finalizeTestRunSummary(TestRunSummary, Responses, {
    jiraKey: PDT_3517_KEY,
    relevantEntityKeys: opts.keys ?? [
      'customer',
      'pod',
      'productContract',
      'billingRun',
      'invoice',
      'compensation',
      'customerLiability',
      'customerReceivable',
    ],
    extraLinks: extra,
    snapshot: {
      compensationId: opts.chain?.compensationId ?? opts.equalDraft?.compensationId,
      remainingInvoiceId: opts.chain?.remainingInvoiceId,
      processId: opts.processId,
      R: opts.chain?.R,
      C: opts.chain?.C,
    },
  });
}

test.describe(`[${PDT_3517_KEY}]: ${PDT_3517_TITLE}`, { tag: ['@billing', '@PDT-3517'] }, () => {
  test(`[${PDT_3517_KEY}]: TC-BE-1 – Debit remaining and positive compensation — cancel remaining only`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE1',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining only', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], taxEventDate));
    const actual = await test.step('Assert remaining unwind and interim released', async () =>
      assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [] }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-1 debit positive cancel remaining only',
        expectedResult:
          'HTTP 201, process COMPLETED. Remaining CANCELLED. Compensation offset REVERSED. Cancellation receivable initialAmount = R. New customer liability C closes the compensation receivable. New government receivable C closes the government liability. Compensation UNINVOICED with link fields null. Interim REAL, isDeducted false, liability currentAmount = I.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-2 – Debit remaining and positive compensation — cancel remaining and the interim`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE2',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining and interim', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber, chain.interims[0]!.number], taxEventDate));
    const actual = await test.step('Assert remaining unwind and interim cancelled', async () =>
      assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [0] }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-2 debit positive cancel remaining and interim',
        expectedResult:
          'Both rows succeed. Remaining CANCELLED for full R. Compensation pair closed at C and UNINVOICED. Interim CANCELLED. Cancellation receivable initialAmount = I. Interim liability currentAmount = 0.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-3 – Debit remaining and negative compensation — cancel remaining only`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and negative compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE3',
        side: 'debit',
        sign: 'negative',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining only', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], taxEventDate));
    const actual = await test.step('Assert negative compensation unwind', async () =>
      assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [] }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-3 debit negative cancel remaining only',
        expectedResult:
          'Remaining CANCELLED by a receivable of R. No offset between remaining liability and extra liability. New customer receivable C closes extraLiability. New government liability C closes the government receivable. Compensation UNINVOICED. Interim REAL and not deducted.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-4 – Debit remaining and negative compensation — cancel remaining and the interim`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and negative compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE4',
        side: 'debit',
        sign: 'negative',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining and interim', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber, chain.interims[0]!.number], taxEventDate));
    const actual = await test.step('Assert negative unwind and interim cancelled', async () =>
      assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [0] }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-4 debit negative cancel remaining and interim',
        expectedResult:
          'Remaining closed for R. Extra liability closed by a customer receivable of C. Government receivable closed by a government liability of C. Interim CANCELLED and its liability currentAmount = 0.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-5 – Credit remaining and positive compensation — cancel remaining only`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: credit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE5',
        side: 'credit',
        sign: 'positive',
        iapAmounts: [PDT_3517_CREDIT_IAP],
        volumeKwh: PDT_3517_CREDIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining credit note only', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], taxEventDate));
    const actual = await test.step('Assert credit unwind without a compensation offset', async () =>
      assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [] }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-5 credit positive cancel remaining only',
        expectedResult:
          'Credit note CANCELLED. New customer liability initialAmount = R closes the remaining receivable. Separate customer liability C closes the compensation receivable. Government receivable C closes the government liability. No offset between the credit and the compensation receivable. Interim REAL and not deducted.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-6 – Credit remaining and positive compensation — cancel remaining and the interim`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: credit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE6',
        side: 'credit',
        sign: 'positive',
        iapAmounts: [PDT_3517_CREDIT_IAP],
        volumeKwh: PDT_3517_CREDIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining and interim', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber, chain.interims[0]!.number], taxEventDate));
    const actual = await test.step('Assert credit unwind and interim cancelled', async () =>
      assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [0] }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-6 credit positive cancel remaining and interim',
        expectedResult:
          'Credit note closed for full R. Compensation pair closed at C. Interim CANCELLED. Cancellation receivable initialAmount = I. Interim liability currentAmount = 0.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-7 – Credit remaining and negative compensation — cancel remaining only`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: credit remaining and negative compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE7',
        side: 'credit',
        sign: 'negative',
        iapAmounts: [PDT_3517_CREDIT_IAP],
        volumeKwh: PDT_3517_CREDIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining credit note only', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], taxEventDate));
    const actual = await test.step('Assert reversed credit offset then full close', async () =>
      assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [] }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-7 credit negative cancel remaining only',
        expectedResult:
          'Offset between remaining receivable and extra liability is REVERSED. Cancellation liability initialAmount = R (not R minus C). New customer receivable C closes extraLiability. New government liability C closes the government receivable. Interim REAL and not deducted.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-8 – Credit remaining and negative compensation — cancel remaining and the interim`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: credit remaining and negative compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE8',
        side: 'credit',
        sign: 'negative',
        iapAmounts: [PDT_3517_CREDIT_IAP],
        volumeKwh: PDT_3517_CREDIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining and interim', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber, chain.interims[0]!.number], taxEventDate));
    const actual = await test.step('Assert credit negative unwind and interim cancelled', async () =>
      assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [0] }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-8 credit negative cancel remaining and interim',
        expectedResult:
          'Credit/compensation offsetting REVERSED. Remaining closed for full R. Compensation pair closed at C and UNINVOICED. Interim CANCELLED. Cancellation receivable initialAmount = I.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-9 – Debit and positive compensation — payment on the remaining liability is reversed`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE9',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
        invoiceDate: randomGens.generateTodaysDate('yyyy-mm-dd'),
      }));
    registerChain(TestRunSummary, chain);
    const payment = await test.step('Precondition: offset 10.00 of a payment onto the remaining liability', async () => {
      const paid = await offsetPaymentOntoLiability(fx, {
        customerId: chain.billedCustomerId,
        liabilityId: chain.remainingLiabilityId as number,
        amount: PDT_3517_PAYMENT_ON_REMAINING,
        paymentPurpose: 'PDT-3517-TC-BE-9',
        shieldLiabilityIds: chain.interims.map((row) => row.liabilityId),
        retainShields: true,
      });
      const liability = await getCustomerLiability(Request, chain.remainingLiabilityId as number);
      expect(
        sameMoney(money(liability.currentAmount), money(chain.R - chain.C - PDT_3517_PAYMENT_ON_REMAINING)),
        `remaining current after payment must be (R ${chain.R} - C ${chain.C} - 10)`,
      ).toBe(true);
      TestRunSummary.registerPayload('payment', { id: paid.paymentId, amount: PDT_3517_PAYMENT_ON_REMAINING });
      if (paid.offsetCounterpartyId !== paid.paymentId) {
        TestRunSummary.registerPayload('customerReceivable', { id: paid.offsetCounterpartyId, amount: PDT_3517_PAYMENT_ON_REMAINING });
      }
      return paid;
    });
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining only', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], taxEventDate));
    const actual = await test.step('Assert payment offset and compensation offset are reversed', async () =>
      assertCompensationUnwind(Request, chain, before, {
        cancelInterimIndexes: [],
        paymentId: payment.offsetCounterpartyId,
        paymentLiabilityId: chain.remainingLiabilityId as number,
      }));
    await releaseLiabilityPaymentShields(fx, chain.interims.map((row) => row.liabilityId));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        keys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice', 'compensation', 'customerLiability', 'customerReceivable', 'payment'],
        check: 'TC-BE-9 payment on remaining liability is reversed',
        expectedResult:
          'The payment of 10 is offset onto the remaining liability. That offset description contains "(Reversed)". Compensation offset description contains "(Reversed)". Cancellation receivable initialAmount = R, not R minus C minus 10. Compensation UNINVOICED. Interim REAL and not deducted.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-10 – Debit and negative compensation — payment on the extra customer liability is reversed`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and negative compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE10',
        side: 'debit',
        sign: 'negative',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
        invoiceDate: randomGens.generateTodaysDate('yyyy-mm-dd'),
      }));
    registerChain(TestRunSummary, chain);
    const payment = await test.step('Precondition: pay the extra customer liability in full', async () => {
      const paid = await offsetPaymentOntoLiability(fx, {
        customerId: chain.billedCustomerId,
        liabilityId: chain.extraLiabilityId as number,
        amount: chain.C,
        paymentPurpose: 'PDT-3517-TC-BE-10',
        shieldLiabilityIds: [
          ...chain.interims.map((row) => row.liabilityId),
          chain.remainingLiabilityId as number,
        ],
        manualOnly: true,
      });
      const extra = await getCustomerLiability(Request, chain.extraLiabilityId as number);
      expect(sameMoney(money(extra.currentAmount), 0), 'extra liability currentAmount 0 after payment').toBe(true);
      const remaining = await getCustomerLiability(Request, chain.remainingLiabilityId as number);
      expect(sameMoney(money(remaining.currentAmount), chain.R), 'remaining liability stays R').toBe(true);
      TestRunSummary.registerPayload('payment', { id: paid.paymentId, amount: chain.C });
      return paid;
    });
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining only', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], taxEventDate));
    // Finding F6: origin/dev settleCompensationsOnCancellation does not reverse manual
    // offsetting on a compensation liability. The assertion below is still the change
    // description and is expected to fail until that code is fixed.
    const actual = await test.step('Assert manual offset on the extra liability is reversed', async () =>
      assertCompensationUnwind(Request, chain, before, {
        cancelInterimIndexes: [],
        paymentId: payment.offsetCounterpartyId,
        paymentLiabilityId: chain.extraLiabilityId as number,
      }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        keys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice', 'compensation', 'customerLiability', 'customerReceivable', 'payment'],
        check: 'TC-BE-10 manual offset on extra liability is reversed',
        expectedResult:
          'Payment offset on extraLiability has description "(Reversed)", then a customer receivable of C closes it and a government liability of C closes the government receivable. Remaining closed for R. Interim REAL and not deducted.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-11 – Two deducted interims — cancel remaining and only interim A`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step(
      'Precondition: two IAPs (30 and 20) on one interim run, then debit remaining and positive compensation',
      async () =>
        buildPdt3517AccountedChain(fx, {
          tcTag: 'TCBE11',
          side: 'debit',
          sign: 'positive',
          iapAmounts: [...PDT_3517_TWO_IAP_AMOUNTS],
          volumeKwh: PDT_3517_TWO_KWH,
        }),
    );
    registerChain(TestRunSummary, chain);
    expect(chain.interims.length, 'interim A and interim B').toBe(2);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining and interim A only', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber, chain.interims[0]!.number], taxEventDate));
    const actual = await test.step('Assert interim B stays REAL and not deducted', async () =>
      assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [0] }));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-11 cancel remaining and interim A only',
        expectedResult:
          'Remaining and interim A cancelled. Compensation UNINVOICED. Interim B REAL, isDeducted false, liability currentAmount stays its stored initial (TC example 20.00).',
        actualResult: `As expected — ${actual}; interimB=${chain.interims[1]?.initialAmount}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-12 – Listed interim was paid — the payment offsetting is reversed and the liability is closed`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE12',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
        invoiceDate: randomGens.generateTodaysDate('yyyy-mm-dd'),
      }));
    registerChain(TestRunSummary, chain);
    const interim = chain.interims[0]!;
    const payment = await test.step('Precondition: pay the interim liability in full', async () => {
      const paid = await offsetPaymentOntoLiability(fx, {
        customerId: chain.billedCustomerId,
        liabilityId: interim.liabilityId,
        amount: interim.initialAmount,
        paymentPurpose: 'PDT-3517-TC-BE-12',
        shieldLiabilityIds: [chain.remainingLiabilityId as number],
        retainShields: true,
      });
      const liability = await getCustomerLiability(Request, interim.liabilityId);
      expect(sameMoney(money(liability.currentAmount), 0), 'interim liability currentAmount 0 after payment').toBe(true);
      TestRunSummary.registerPayload('payment', { id: paid.paymentId, amount: interim.initialAmount });
      return paid;
    });
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining and interim', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber, interim.number], taxEventDate));
    const actual = await test.step('Assert interim payment offset is reversed and the liability is closed', async () =>
      assertCompensationUnwind(Request, chain, before, {
        cancelInterimIndexes: [0],
        paymentId: payment.offsetCounterpartyId ?? payment.paymentId,
        paymentLiabilityId: interim.liabilityId,
      }));
    await releaseLiabilityPaymentShields(fx, [chain.remainingLiabilityId as number]);
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        keys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice', 'compensation', 'customerLiability', 'customerReceivable', 'payment'],
        check: 'TC-BE-12 paid interim offset is reversed',
        expectedResult:
          'Remaining compensation unwind at R and C. Payment offset on the interim has description "(Reversed)". Interim CANCELLED. Cancellation receivable initialAmount = I. Interim liability currentAmount = 0.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-13 – Tax event date omitted`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE13',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const { processId } = await test.step('POST /invoice-cancellation with taxEventDate null', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], null));
    const report = await test.step('Download MASS_IMPORT_ERROR_REPORT', async () =>
      downloadProcessReportText(Request, processId));
    const actual = await test.step('Assert the invoice and the deduction stay unchanged', async () =>
      assertCancellationRejected(Request, chain, before, report, [TAX_EVENT_REQUIRED_FRAGMENT]));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-13 tax event date omitted',
        expectedResult:
          'HTTP 201. Process row unsuccessful. Report contains "Tax event date is required for invoice cancellation". Remaining REAL. Compensation INVOICED. Interim REAL and deducted. No cancellation receivable.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-14 – Tax event date outside an open accounting period`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE14',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    await test.step(`Precondition: ${CLOSED_TAX_EVENT_DATE} is outside every OPEN accounting period`, async () =>
      assertDateOutsideOpenPeriods(Request, CLOSED_TAX_EVENT_DATE));
    const before = await snapshotFinancials(Request, chain);
    const { processId } = await test.step('POST /invoice-cancellation with taxEventDate 1991-01-15', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], CLOSED_TAX_EVENT_DATE));
    const report = await test.step('Download MASS_IMPORT_ERROR_REPORT', async () =>
      downloadProcessReportText(Request, processId));
    const actual = await test.step('Assert the closed date is rejected on the process row', async () =>
      assertCancellationRejected(Request, chain, before, report, [
        'Tax event date',
        TAX_EVENT_OPEN_PERIOD_FRAGMENT,
      ]));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-14 tax event date outside an open period',
        expectedResult:
          'HTTP 201. Report contains "must be within an open accounting period". Remaining REAL. Compensation INVOICED. Interim stays deducted. No cancellation receivable.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-15 – Deducted interim listed without the remaining invoice`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE15',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation interim number only', async () =>
      expectCancellationCreated(Request, [chain.interims[0]!.number], taxEventDate));
    const report = await test.step('Download MASS_IMPORT_ERROR_REPORT', async () =>
      downloadProcessReportText(Request, processId));
    const actual = await test.step('Assert both invoices stay REAL and the interim stays deducted', async () =>
      assertCancellationRejected(Request, chain, before, report, [INTERIM_NOT_IN_LIST_FRAGMENT]));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-15 deducted interim without the remaining invoice',
        expectedResult:
          'HTTP 201. Report contains "connected invoice for this interim invoice not in the cancellation list". Interim and remaining stay REAL. Interim stays deducted. Compensation stays INVOICED.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-16 – Rescheduling on the government compensation liability blocks cancellation`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE16',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    const reschedulingId = await test.step(
      'Precondition: EXECUTED rescheduling on the government liability (DRAFT would not block)',
      async () => executeReschedulingOnGovernmentLiability(fx, chain),
    );
    TestRunSummary.registerPayload('rescheduling', { id: reschedulingId, status: 'EXECUTED' });
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining only', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], taxEventDate));
    const report = await test.step('Download MASS_IMPORT_ERROR_REPORT', async () =>
      downloadProcessReportText(Request, processId));
    const actual = await test.step('Assert cancellation is refused and the interim stays deducted', async () =>
      assertCancellationRejected(Request, chain, before, report, [RESCHEDULING_BLOCK_FRAGMENT]));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        keys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice', 'compensation', 'customerLiability', 'customerReceivable', 'rescheduling'],
        check: 'TC-BE-16 executed rescheduling blocks cancellation',
        expectedResult:
          'HTTP 201. Report contains "Invoice can not be cancelled because connected liability is used for rescheduling". Remaining REAL. Compensation INVOICED. Compensation receivable currentAmount stays 0. Interim REAL and isDeducted true.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-17 – Listed interim stays real when remaining unwind is rejected`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE17',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    await test.step('Precondition: EXECUTED rescheduling on the government liability', async () =>
      executeReschedulingOnGovernmentLiability(fx, chain));
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining and interim', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber, chain.interims[0]!.number], taxEventDate));
    const report = await test.step('Download MASS_IMPORT_ERROR_REPORT', async () =>
      downloadProcessReportText(Request, processId));
    // Finding F1: origin/dev may cancel the listed interim in parallel while the remaining
    // invoice is rejected. The change description still requires the interim to stay REAL
    // and deducted. This assertion is expected to fail on origin/dev until that is fixed.
    const actual = await test.step('Assert remaining stays REAL and the listed interim stays deducted', async () =>
      assertCancellationRejected(Request, chain, before, report, [RESCHEDULING_BLOCK_FRAGMENT]));
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        keys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice', 'compensation', 'customerLiability', 'customerReceivable', 'rescheduling'],
        check: 'TC-BE-17 listed interim stays real when remaining unwind is rejected',
        expectedResult:
          'Remaining row unsuccessful because the government liability is in an executed rescheduling. Remaining REAL. Compensation INVOICED. Interim REAL, isDeducted true, liability currentAmount = I. No cancellation receivable.',
        actualResult: `As expected — ${actual}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-18 – Positive compensation receivable belongs to the billed customer`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const chain = await test.step('Precondition: debit remaining and positive compensation', async () =>
      buildPdt3517AccountedChain(fx, {
        tcTag: 'TCBE18',
        side: 'debit',
        sign: 'positive',
        iapAmounts: [PDT_3517_DEBIT_IAP],
        volumeKwh: PDT_3517_DEBIT_KWH,
      }));
    registerChain(TestRunSummary, chain);
    expect(chain.compReceivableId, 'compensation receivable id').toBeGreaterThan(0);
    expect(chain.C, 'compensation amount').toBe(PDT_3517_C_AMOUNT);
    const before = await snapshotFinancials(Request, chain);
    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const { processId } = await test.step('POST /invoice-cancellation remaining only', async () =>
      expectCancellationCreated(Request, [chain.remainingNumber], taxEventDate));
    const actual = await test.step(
      'Assert the closing liability is the billed customer and the closing receivable is the recipient',
      async () => assertCompensationUnwind(Request, chain, before, { cancelInterimIndexes: [] }),
    );
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        chain,
        processId,
        check: 'TC-BE-18 positive receivable belongs to the billed customer',
        expectedResult:
          'Before cancel, receivableForCustomer.customerResponse.id is the billed customer and liabilityForRecipient.customerResponse.id is the recipient. After cancel, the new customer liability of C belongs to the billed customer and the new government receivable of C belongs to the recipient. Page 1077608452 is not the expected result.',
        actualResult: `As expected — ${actual}; billedCustomerId=${chain.billedCustomerId}; recipientId=${chain.recipientId}`,
      });
    });
  });

  test(`[${PDT_3517_KEY}]: TC-BE-19 – Equal interim and period amount has no product result`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3517_TEST_TIMEOUT_MS);
    const fx: Pdt3517Fx = { Request, GeneratePayload, Responses, Endpoints };
    const observed = await test.step(
      'Precondition: period including VAT aimed at the interim total; do not cancel',
      async () => buildPdt3517EqualDraft(fx, 'TCBE19'),
    );
    expect(observed.cancellationCalled, 'POST /invoice-cancellation was not called').toBe(false);
    TestRunSummary.registerPayload('compensation', { id: observed.compensationId });
    TestRunSummary.registerPayload('invoice', {
      interimTotalIncludingVat: observed.interimTotalIncludingVat,
      interimTotalExcludingVat: observed.interimTotalExcludingVat,
      profileKwh: observed.profileKwh,
      drafts: observed.drafts,
    });
    await test.step('Attach test run summary', async () => {
      attachSummary(TestRunSummary, Responses, {
        equalDraft: observed,
        check: 'TC-BE-19 equal interim and period has no cancellation result',
        expectedResult:
          'No product pass or fail. Do not call POST /invoice-cancellation. Record the draft. equalityAchieved is true only when every draft totalAmountIncludingVat is 0. A non-zero draft means compensation and VAT kept the period off the interim; that is recorded and is not a product verdict.',
        actualResult:
          `As expected — cancellationCalled=false equalityAchieved=${observed.equalityAchieved} ` +
          `drafts=${observed.drafts.length} ` +
          `totals=${observed.drafts.map((row) => row.totalIncludingVat).join(',') || 'none'} ` +
          `interimTotalIncludingVat=${observed.interimTotalIncludingVat}`,
      });
    });
  });
});
