/**
 * PDT-3490 — Government compensation - Interest rate should not be applied
 * in the liabilities of the Government.
 *
 * Takes PDT-3486 TC-BE-2 (positive Document Amount) and TC-BE-3 (negative)
 * and adds GET /customer-liability/{id} checks that Applicable interest rate
 * is truly empty (null or {id:null} + empty interest dates), even when the
 * origin invoice still has interestRate.
 *
 * Jira: https://oppa-support.atlassian.net/browse/PDT-3490
 * Valeri: negative compensation customer liability must also have empty rate.
 * Swagger: Cursor-Project/config/swagger/dev/swagger-spec.json (refreshed this session).
 * GET /customer-liability/{id} → CustomerLiabilityResponse.applicableInterestRate
 * (InterestRateShortResponse), interestDateFrom, interestDateTo.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3486-standard-billing-compensation-deferred-alo.spec.ts
 * - tests/cursor/pdt-3486-standard-billing-compensation-deferred-alo.fixtures.ts
 * - tests/cursor/PDT-3470-standard-credit-note-government-compensation-receivable.spec.ts
 * - tests/cursor/cursor-test.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3490_KEY,
  PDT_3490_RELEVANT_ENTITY_KEYS,
  PDT_3490_TEST_TIMEOUT_MS,
  PDT_3490_TITLE,
  assertCompensationLiabilityInterestNotCopiedFromOrigin,
  assertLinkedInvoiceIsDebitNote,
  assertNegativeCompensationPolarity,
  assertPositiveCompensationPolarity,
  describeInterestSnapshot,
  findAutomaticLiabilityOnInvoice,
  getCompensation,
  getLiability,
  nestedId,
  pdt3470PortalExtraLinks,
  postPdt3486Compensation,
  realizeBillingRun,
  roundMoney,
  startGeneratingAndWaitGenerated,
  startPdt3490DebitNoteDraft,
  uniquePdt3490CompensationNumber,
  type Pdt3486Fx,
} from './pdt-3490-government-compensation-liability-empty-interest.fixtures';

function titleFor(scenario: string): string {
  return `[${PDT_3490_KEY}]: ${PDT_3490_TITLE} | ${scenario}`;
}

test.describe(`[${PDT_3490_KEY}]: ${PDT_3490_TITLE}`, {
  tag: ['@billing', '@pdt-3490', '@cursor'],
}, () => {
  test.describe.configure({ fullyParallel: true });

  test(titleFor('positive Document Amount — government liability interest empty'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3490_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const C = 40;
    const number = uniquePdt3490CompensationNumber('POS');

    const debit = await startPdt3490DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });
    expect(debit.T).toBeGreaterThan(C);

    const compensation = await test.step(
      'Precondition: POST government-compensations after draft (positive C=40)',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3490-positive',
        }),
    );
    TestRunSummary.registerPayload('compensation', { id: compensation.id, number, documentAmount: C });
    TestRunSummary.registerPayload('invoice', { id: debit.debitInvoiceId, T: debit.T });

    await test.step('Action: PATCH start-generating on FOR_VOLUMES run', async () => {
      await startGeneratingAndWaitGenerated(Request, debit.volumeBillingRunId);
    });
    const linked = await test.step(
      'Read: linked invoice is INVOICE or DEBIT_NOTE (not CREDIT_NOTE)',
      async () => assertLinkedInvoiceIsDebitNote(Request, compensation.id),
    );
    expect(linked.compensation.index).toBe(0);
    expect(linked.linkedInvoiceId).toBeGreaterThan(0);

    const invoice = await test.step('Action: PATCH start-accounting (realize billing)', async () =>
      realizeBillingRun(Request, debit.volumeBillingRunId, linked.linkedInvoiceId),
    );

    const govLiab = await test.step(
      'Read: GET /customer-liability/{liabilityForRecipient} — interest must be empty',
      async () => {
        const comp = await getCompensation(Request, compensation.id);
        expect(comp.compensationStatus).toBe('INVOICED');
        assertPositiveCompensationPolarity(comp, 'PDT-3490 positive');
        const liabilityId = nestedId(comp.liabilityForRecipient);
        expect(liabilityId, 'liabilityForRecipient id').toBeGreaterThan(0);
        const detail = await getLiability(Request, liabilityId as number);
        Responses.customerLiability.push(detail);
        expect(detail.creationType).toBe('AUTOMATIC');
        expect(nestedId(detail.customerResponse)).toBe(debit.chain.recipientCustomerId);
        expect(roundMoney(detail.initialAmount)).toBe(C);

        const originInvoiceLiab = await findAutomaticLiabilityOnInvoice(
          Request,
          linked.linkedInvoiceId,
          linked.T,
        );
        const origin = assertCompensationLiabilityInterestNotCopiedFromOrigin(
          detail,
          invoice,
          originInvoiceLiab,
          'PDT-3490 positive government liability',
        );
        return { detail, origin, originInvoiceLiab };
      },
    );

    const interestNote = describeInterestSnapshot({
      compensationLiability: govLiab.detail,
      originInvoiceInterestRateId: govLiab.origin.originInvoiceInterestRateId,
      originInvoiceLiabilityInterestRateId: govLiab.origin.originInvoiceLiabilityInterestRateId,
    });
    TestRunSummary.recordCheck({
      check: 'Positive GC government liability Applicable interest rate is truly empty',
      expectedResult:
        'GET customer-liability/{liabilityForRecipient}: applicableInterestRate empty (null or {id:null}); interestDateFrom/To empty; must not copy origin invoice.interestRate.',
      actualResult: interestNote,
      passed: true,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3490_KEY,
        relevantEntityKeys: [...PDT_3490_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({ compensationId: compensation.id }),
          ...buildProductContractTabLinks(debit.chain.contractId),
        },
        snapshot: {
          polarity: 'positive',
          invoiceId: linked.linkedInvoiceId,
          compensationId: compensation.id,
          governmentLiabilityId: govLiab.detail.id,
          T: linked.T,
          C,
          originInvoiceInterestRateId: govLiab.origin.originInvoiceInterestRateId,
          originInvoiceLiabilityInterestRateId: govLiab.origin.originInvoiceLiabilityInterestRateId,
          compensationApplicableInterestRate: govLiab.detail.applicableInterestRate ?? null,
        },
      });
    });
  });

  test(titleFor('negative Document Amount — customer liability interest empty'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3490_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const C = -35;
    const absC = 35;
    const number = uniquePdt3490CompensationNumber('NEG');

    const debit = await startPdt3490DebitNoteDraft(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: debit.chain.customerId,
      recipientId: debit.chain.recipientCustomerId,
    });
    expect(debit.T).toBeGreaterThan(absC);

    const compensation = await test.step(
      'Precondition: POST government-compensations after draft (negative C=-35)',
      async () =>
        postPdt3486Compensation(fx, {
          customerId: debit.chain.customerId,
          podId: debit.chain.podId,
          recipientId: debit.chain.recipientCustomerId,
          documentPeriod: debit.chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3490-negative',
        }),
    );
    TestRunSummary.registerPayload('compensation', { id: compensation.id, number, documentAmount: C });
    TestRunSummary.registerPayload('invoice', { id: debit.debitInvoiceId, T: debit.T });

    await test.step('Action: PATCH start-generating on FOR_VOLUMES run', async () => {
      await startGeneratingAndWaitGenerated(Request, debit.volumeBillingRunId);
    });
    const linked = await test.step(
      'Read: linked invoice is INVOICE or DEBIT_NOTE (not CREDIT_NOTE)',
      async () => assertLinkedInvoiceIsDebitNote(Request, compensation.id),
    );
    expect(linked.compensation.index).toBe(0);
    expect(linked.linkedInvoiceId).toBeGreaterThan(0);

    const invoice = await test.step('Action: PATCH start-accounting (realize billing)', async () =>
      realizeBillingRun(Request, debit.volumeBillingRunId, linked.linkedInvoiceId),
    );

    const custLiab = await test.step(
      'Read: GET /customer-liability/{liabilityForCustomer} — interest must be empty',
      async () => {
        const comp = await getCompensation(Request, compensation.id);
        expect(comp.compensationStatus).toBe('INVOICED');
        assertNegativeCompensationPolarity(comp, 'PDT-3490 negative');
        const liabilityId = nestedId(comp.liabilityForCustomer);
        expect(liabilityId, 'liabilityForCustomer id').toBeGreaterThan(0);
        const detail = await getLiability(Request, liabilityId as number);
        Responses.customerLiability.push(detail);
        expect(detail.creationType).toBe('AUTOMATIC');
        expect(nestedId(detail.customerResponse)).toBe(debit.chain.customerId);
        expect(roundMoney(detail.initialAmount)).toBe(absC);

        const originInvoiceLiab = await findAutomaticLiabilityOnInvoice(
          Request,
          linked.linkedInvoiceId,
          linked.T,
        );
        const origin = assertCompensationLiabilityInterestNotCopiedFromOrigin(
          detail,
          invoice,
          originInvoiceLiab,
          'PDT-3490 negative customer liability',
        );
        return { detail, origin, originInvoiceLiab };
      },
    );

    const interestNote = describeInterestSnapshot({
      compensationLiability: custLiab.detail,
      originInvoiceInterestRateId: custLiab.origin.originInvoiceInterestRateId,
      originInvoiceLiabilityInterestRateId: custLiab.origin.originInvoiceLiabilityInterestRateId,
    });
    TestRunSummary.recordCheck({
      check: 'Negative GC customer liability Applicable interest rate is truly empty',
      expectedResult:
        'GET customer-liability/{liabilityForCustomer}: applicableInterestRate empty (null or {id:null}); interestDateFrom/To empty; must not copy origin invoice.interestRate.',
      actualResult: interestNote,
      passed: true,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3490_KEY,
        relevantEntityKeys: [...PDT_3490_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({ compensationId: compensation.id }),
          ...buildProductContractTabLinks(debit.chain.contractId),
        },
        snapshot: {
          polarity: 'negative',
          invoiceId: linked.linkedInvoiceId,
          compensationId: compensation.id,
          customerLiabilityId: custLiab.detail.id,
          T: linked.T,
          C,
          originInvoiceInterestRateId: custLiab.origin.originInvoiceInterestRateId,
          originInvoiceLiabilityInterestRateId: custLiab.origin.originInvoiceLiabilityInterestRateId,
          compensationApplicableInterestRate: custLiab.detail.applicableInterestRate ?? null,
        },
      });
    });
  });
});
