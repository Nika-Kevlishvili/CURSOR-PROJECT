/**
 * PDT-3500 — After Invoice Correction accounting, the CORRECTION CREDIT_NOTE
 * must have an AUTOMATIC customer receivable. The debit document keeps its
 * invoice liability.
 *
 * Bug-only automation (no Backend TC markdown). Jira title is used verbatim.
 *
 * Swagger refresh: update-swagger-specs.ps1 this session (dev spec).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3486-standard-billing-compensation-deferred-alo.spec.ts
 * - tests/cursor/pdt-3486-standard-billing-compensation-deferred-alo.fixtures.ts
 * - tests/cursor/PDT-3470-standard-credit-note-government-compensation-receivable.spec.ts
 * - tests/cursor/pdt-3470-standard-credit-note-government-compensation-receivable.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  buildDevInvoicePreviewLink,
  getInvoice,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import {
  getCompensation,
  money,
  nestedId,
  pdt3470PortalExtraLinks,
} from './pdt-3470-standard-credit-note-government-compensation-receivable.fixtures';
import {
  classifyCorrectionDrafts,
  findAutomaticLiabilityOnInvoice,
  generateAndAccount,
  postPdt3486Compensation,
  startCorrectionDraftForInvoice,
  startAccountingAndWaitCompleted,
  startGeneratingAndWaitGenerated,
  startPdt3483DebitNoteDraft,
  type Pdt3486Fx,
} from './pdt-3486-standard-billing-compensation-deferred-alo.fixtures';
import {
  PDT_3500_COMPENSATION_LARGER_THAN_INVOICE_FACTOR,
  PDT_3500_KEY,
  PDT_3500_RELEVANT_ENTITY_KEYS,
  PDT_3500_TEST_TIMEOUT_MS,
  PDT_3500_TITLE,
  assertCorrectionCreditAndDebitPair,
  automaticReceivablesLinkedToInvoice,
  correctionDocumentParentsOriginal,
  customerIdentifierById,
  negativeCompensationMuchLargerThanInvoice,
  receivableAmountMatchesCreditNoteOrCompensation,
  uniquePdt3500CompensationNumber,
} from './pdt-3500-invoice-correction-credit-note-receivable.fixtures';

test.describe(`[${PDT_3500_KEY}]: ${PDT_3500_TITLE}`, {
  tag: ['@billing', '@pdt-3500', '@cursor'],
}, () => {
  test(`[${PDT_3500_KEY}]: ${PDT_3500_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3500_TEST_TIMEOUT_MS);
    const fx: Pdt3486Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniquePdt3500CompensationNumber();

    const original = await test.step(
      'Precondition: FOR_VOLUMES standard billing draft (customer, contract, POD from scratch)',
      async () => startPdt3483DebitNoteDraft(fx),
    );
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: original.chain.customerId,
      recipientId: original.chain.recipientCustomerId,
      podId: original.chain.podId,
    });
    const billedIdentifier = customerIdentifierById(Responses, original.chain.customerId);
    const recipientIdentifier = customerIdentifierById(
      Responses,
      original.chain.recipientCustomerId,
    );

    const originalInvoice = await test.step(
      'Precondition: account the standard volume invoice to REAL (no compensation, no volume or price edits)',
      async () => {
        const invoice = await generateAndAccount(
          fx,
          original.volumeBillingRunId,
          original.debitInvoiceId,
        );
        expect(invoice.invoiceType, 'original invoiceType').toBe('STANDARD');
        expect(invoice.invoiceStatus, 'original invoiceStatus').toBe('REAL');
        expect(
          String(invoice.invoiceDocumentType),
          'original document is a standard invoice liability, not a credit note',
        ).not.toBe('CREDIT_NOTE');
        expect(
          ['INVOICE', 'DEBIT_NOTE'].includes(String(invoice.invoiceDocumentType)),
          `original invoiceDocumentType, got ${invoice.invoiceDocumentType}`,
        ).toBe(true);
        return invoice;
      },
    );
    const originalInvoiceNumber = String(originalInvoice.invoiceNumber ?? '');
    const originalInvoiceTotal = money(originalInvoice.totalAmountIncludingVat);
    const compensationAmount = negativeCompensationMuchLargerThanInvoice(originalInvoiceTotal);
    expect(
      Math.abs(compensationAmount),
      `negative compensation must be ${PDT_3500_COMPENSATION_LARGER_THAN_INVOICE_FACTOR}× the invoice total`,
    ).toBe(money(originalInvoiceTotal * PDT_3500_COMPENSATION_LARGER_THAN_INVOICE_FACTOR));
    expect(Math.abs(compensationAmount)).toBeGreaterThan(originalInvoiceTotal);
    TestRunSummary.registerPayload('invoice', {
      originalInvoiceId: original.debitInvoiceId,
      originalInvoiceNumber,
      invoiceType: originalInvoice.invoiceType,
      invoiceDocumentType: originalInvoice.invoiceDocumentType,
      totalAmountIncludingVat: originalInvoiceTotal,
    });

    const correction = await test.step(
      'Precondition: INVOICE_CORRECTION draft via correctionBilling (no extra BBP, no invoice-cancellation)',
      async () => startCorrectionDraftForInvoice(fx, original.debitInvoiceId),
    );
    TestRunSummary.registerPayload('billingRun', {
      originalBillingRunId: original.volumeBillingRunId,
      correctionBillingRunId: correction.correctionBillingRunId,
      billingType: 'INVOICE_CORRECTION',
    });

    const pair = await test.step(
      'Read: correction drafts are CORRECTION CREDIT_NOTE and CORRECTION DEBIT_NOTE or INVOICE',
      async () => {
        const classified = await classifyCorrectionDrafts(Request, correction.draftInvoiceIds);
        const ids = assertCorrectionCreditAndDebitPair(classified);
        const credit = await getInvoice(Request, ids.creditNoteId);
        const debit = await getInvoice(Request, ids.debitDocumentId);
        expect(credit.invoiceType, 'credit note invoiceType').toBe('CORRECTION');
        expect(credit.invoiceDocumentType, 'credit note invoiceDocumentType').toBe('CREDIT_NOTE');
        expect(debit.invoiceType, 'debit document invoiceType').toBe('CORRECTION');
        expect(
          ['DEBIT_NOTE', 'INVOICE'].includes(String(debit.invoiceDocumentType)),
          `debit document invoiceDocumentType, got ${debit.invoiceDocumentType}`,
        ).toBe(true);
        expect.soft(
          correctionDocumentParentsOriginal(credit, original.debitInvoiceId, originalInvoiceNumber),
          `credit note ${ids.creditNoteId} parent is original invoice ${original.debitInvoiceId}`,
        ).toBe(true);
        expect.soft(
          correctionDocumentParentsOriginal(debit, original.debitInvoiceId, originalInvoiceNumber),
          `debit document ${ids.debitDocumentId} parent is original invoice ${original.debitInvoiceId}`,
        ).toBe(true);
        return ids;
      },
    );

    const compensation = await test.step(
      'Precondition: POST negative compensation 10× larger than the original invoice, before start-generating',
      async () => {
        const created = await postPdt3486Compensation(fx, {
          customerId: original.chain.customerId,
          podId: original.chain.podId,
          recipientId: original.chain.recipientCustomerId,
          documentPeriod: original.chain.documentPeriod,
          documentAmount: compensationAmount,
          number,
          reason: 'PDT-3500-negative-correction',
        });
        const view = await getCompensation(Request, created.id);
        expect(view.compensationStatus, 'compensation is uninvoiced before generate').toBe(
          'UNINVOICED',
        );
        expect(
          money(view.documentAmount),
          'CompensationRequest.documentAmount stays negative',
        ).toBe(money(compensationAmount));
        expect(money(view.documentAmount)).toBeLessThan(0);
        return created;
      },
    );
    TestRunSummary.registerPayload('compensation', {
      id: compensation.id,
      number,
      documentAmount: compensationAmount,
    });

    await test.step(
      'Action: start-generating then start-accounting on the correction run',
      async () => {
        await startGeneratingAndWaitGenerated(Request, correction.correctionBillingRunId);
        await startAccountingAndWaitCompleted(Request, correction.correctionBillingRunId);
      },
    );

    const realized = await test.step(
      'Read: both correction documents are REAL',
      async () => {
        const credit = await getInvoice(Request, pair.creditNoteId);
        const debit = await getInvoice(Request, pair.debitDocumentId);
        expect(credit.invoiceStatus, 'credit note after accounting').toBe('REAL');
        expect(debit.invoiceStatus, 'debit document after accounting').toBe('REAL');
        expect(credit.invoiceType).toBe('CORRECTION');
        expect(credit.invoiceDocumentType).toBe('CREDIT_NOTE');
        return {
          credit,
          debit,
          creditTotal: money(credit.totalAmountIncludingVat),
          debitTotal: money(debit.totalAmountIncludingVat),
        };
      },
    );

    const creditReceivables = await test.step(
      'Read: AUTOMATIC customer receivable linked to the CORRECTION CREDIT_NOTE',
      async () =>
        automaticReceivablesLinkedToInvoice(Request, pair.creditNoteId, [
          billedIdentifier,
          recipientIdentifier,
        ]),
    );
    for (const row of creditReceivables) {
      Responses.customerReceivable.push(row);
    }

    const matchingReceivable = creditReceivables.find((row) =>
      receivableAmountMatchesCreditNoteOrCompensation(
        row.initialAmount,
        realized.creditTotal,
        compensationAmount,
      ),
    );
    const creditPassed = matchingReceivable != null;
    const creditAmounts = creditReceivables
      .map((row) => `${row.id}:${money(row.initialAmount)}`)
      .join(', ');
    TestRunSummary.recordCheck({
      check: 'CORRECTION CREDIT_NOTE has an AUTOMATIC customer receivable',
      expectedResult:
        `Invoice ${pair.creditNoteId} (CORRECTION CREDIT_NOTE, REAL) has at least one AUTOMATIC customer receivable with initialAmount equal to the credit-note total ${money(Math.abs(realized.creditTotal))} or the compensation absolute amount ${money(Math.abs(compensationAmount))}.`,
      actualResult: creditPassed
        ? `As expected — receivable ${matchingReceivable?.id} initialAmount ${money(matchingReceivable?.initialAmount)} on credit note ${pair.creditNoteId}.`
        : `Not as expected — AUTOMATIC receivables on credit note ${pair.creditNoteId}: ${creditAmounts || 'none'}.`,
      passed: creditPassed,
    });
    expect.soft(
      creditReceivables.length,
      `CORRECTION CREDIT_NOTE ${pair.creditNoteId} must have an AUTOMATIC customer receivable`,
    ).toBeGreaterThan(0);
    expect.soft(
      creditPassed,
      `receivable initialAmount must equal credit-note total ${money(Math.abs(realized.creditTotal))} or |compensation| ${money(Math.abs(compensationAmount))}; saw ${creditAmounts || 'none'}`,
    ).toBe(true);
    if (matchingReceivable) {
      expect(matchingReceivable.creationType).toBe('AUTOMATIC');
      expect(nestedId(matchingReceivable.invoiceResponse)).toBe(pair.creditNoteId);
      expect(money(matchingReceivable.initialAmount)).toBeGreaterThan(0);
    }

    const debitLiability = await test.step(
      'Read: AUTOMATIC invoice liability on the CORRECTION debit document',
      async () =>
        findAutomaticLiabilityOnInvoice(
          Request,
          pair.debitDocumentId,
          realized.debitTotal,
        ),
    );
    Responses.customerLiability.push(debitLiability);
    const debitLinkedInvoice = nestedId(debitLiability.invoiceResponse);
    const debitPassed =
      debitLiability.creationType === 'AUTOMATIC' &&
      money(debitLiability.initialAmount) > 0 &&
      (debitLinkedInvoice == null || debitLinkedInvoice === pair.debitDocumentId);
    TestRunSummary.recordCheck({
      check: 'CORRECTION debit document still has its invoice liability',
      expectedResult:
        `Invoice ${pair.debitDocumentId} (${realized.debit.invoiceDocumentType}) has an AUTOMATIC customer liability with a positive initialAmount.`,
      actualResult: debitPassed
        ? `As expected — liability ${debitLiability.id} creationType ${debitLiability.creationType} initialAmount ${money(debitLiability.initialAmount)}.`
        : `Not as expected — liability ${debitLiability.id} creationType ${debitLiability.creationType} initialAmount ${debitLiability.initialAmount} invoice ${debitLinkedInvoice}.`,
      passed: debitPassed,
    });
    expect(debitLiability.creationType, 'debit invoice liability creationType').toBe('AUTOMATIC');
    expect(money(debitLiability.initialAmount), 'debit invoice liability initialAmount').toBeGreaterThan(
      0,
    );
    if (debitLinkedInvoice != null) {
      expect(debitLinkedInvoice, 'debit liability invoiceResponse').toBe(pair.debitDocumentId);
    }

    await test.step('Attach test run summary', async () => {
      const debitPreview = buildDevInvoicePreviewLink(pair.debitDocumentId);
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3500_KEY,
        relevantEntityKeys: [...PDT_3500_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: pair.creditNoteId,
          }),
          ...(debitPreview ? { debitNotePreview: [debitPreview] } : {}),
          ...buildProductContractTabLinks(original.chain.contractId),
        },
        snapshot: {
          originalInvoiceId: original.debitInvoiceId,
          creditNoteId: pair.creditNoteId,
          debitDocumentId: pair.debitDocumentId,
          correctionBillingRunId: correction.correctionBillingRunId,
          compensationId: compensation.id,
          compensationAmount,
          creditNoteTotal: realized.creditTotal,
          creditReceivableId: matchingReceivable?.id ?? null,
          debitLiabilityId: debitLiability.id,
        },
      });
    });
  });
});
