/**
 * PDT-3483 — Billing Run Compensation - Credit Note and Debit Note
 * (FinalLiabilityAmount after PDF).
 *
 * Backend TC: Cursor-Project/test_cases/Backend/PDT-3483_billing_run_compensation_credit_debit_note_final_liability.md
 *
 * Swagger: DEV refresh this session (Cursor-Project/config/swagger/dev/swagger-spec.json).
 * GET /billing-run/generate-invoice-data?invoiceId= → BillingRunDocumentModelImpl
 * PascalCase: FinalLiabilityAmount, FinalLiabilityAmountWithWords, TotalInclVat, SDCompensations.
 *
 * Excel Case 2 = -10 (not Word story -40). IAP leftover is not asserted.
 * Compensation AFTER draft (T known) BEFORE start-generating. One signed documentAmount.
 * Scope: Excel Cases 1-6 plus zero-FLA identity (C = ±T); tests run in parallel.
 * Credit Note: IAP deduction larger than volumes. Debit: FOR_VOLUMES without IAP
 * (liability = debit case). No INVOICE_CORRECTION.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3470-standard-credit-note-government-compensation-receivable.spec.ts
 * - tests/cursor/pdt-3470-standard-credit-note-government-compensation-receivable.fixtures.ts
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3483_KEY,
  PDT_3483_RELEVANT_ENTITY_KEYS,
  PDT_3483_TEST_TIMEOUT_MS,
  PDT_3483_TITLE,
  assertFlaEquals,
  assertFlaNotForbidden,
  assertFlaTemplaterExtras,
  assertLinkedInvoiceIsDebitNote,
  buildPdt3470IapRealChain,
  expectedFla,
  generatePdt3470VolumeRun,
  getCompensation,
  getGenerateInvoiceData,
  getInvoice,
  money,
  nestedId,
  pdt3470PortalExtraLinks,
  postPdt3483SignedCompensation,
  scaleC,
  startGeneratingAndWaitGenerated,
  startPdt3483DebitNoteDraft,
  startPdt3483VolumeCreditNoteDraft,
  uniquePdt3483CompensationNumber,
  type Pdt3483Fx,
} from './pdt-3483-billing-run-compensation-credit-debit-note-final-liability.fixtures';

function titleFor(scenario: string): string {
  return `[${PDT_3483_KEY}]: ${PDT_3483_TITLE} | ${scenario}`;
}

function attachFlaSummary(
  TestRunSummary: {
    recordCheck: (row: {
      check: string;
      expectedResult: string;
      actualResult: string;
      passed: boolean;
    }) => void;
  },
  opts: {
    check: string;
    expectedResult: string;
    actualResult: string;
    passed: boolean;
  },
): void {
  TestRunSummary.recordCheck(opts);
}

test.describe(`[${PDT_3483_KEY}]: ${PDT_3483_TITLE}`, {
  tag: ['@billing', '@pdt-3483', '@cursor'],
}, () => {
  test.describe.configure({ fullyParallel: true });

  test(titleFor('TC-BE-1 Credit Note Case 1 — positive customer receivable'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3483_TEST_TIMEOUT_MS);
    const fx: Pdt3483Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniquePdt3483CompensationNumber('TCBE1');

    const chain = await buildPdt3470IapRealChain(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: chain.billedCustomerId,
      billedCustomerIdentifier: chain.billedCustomerIdentifier,
      recipientId: chain.recipientId,
    });

    const volume = await startPdt3483VolumeCreditNoteDraft(fx, chain);
    const C = scaleC(volume.T, 40);
    const expected = expectedFla('CREDIT_NOTE', volume.T, C);

    const compensation = await test.step(
      'Precondition: POST government-compensations after draft (positive C, billed POD)',
      async () =>
        postPdt3483SignedCompensation(fx, {
          customerId: chain.billedCustomerId,
          podId: chain.billedPodId,
          recipientId: chain.recipientId,
          documentPeriod: chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3483-TC-BE-1',
        }),
    );
    TestRunSummary.registerPayload('compensation', {
      id: compensation.id,
      number: compensation.number,
      documentAmount: C,
    });

    await test.step('Action: PATCH start-generating (PDF) on volume Credit Note run', async () => {
      await generatePdt3470VolumeRun(fx, volume.volumeBillingRunId);
    });

    const linked = await test.step('Read: compensation index and invoice.id', async () => {
      const view = await getCompensation(Request, compensation.id);
      expect(view.index, 'compensation index after PDF').toBe(0);
      expect(nestedId(view.invoice), 'compensation.invoice.id is the Credit Note').toBe(
        volume.creditNoteInvoiceId,
      );
      return view;
    });

    await test.step('Read: GET /invoice Credit Note still STANDARD + CREDIT_NOTE', async () => {
      const invoice = await getInvoice(Request, volume.creditNoteInvoiceId);
      expect(invoice.invoiceType).toBe('STANDARD');
      expect(invoice.invoiceDocumentType).toBe('CREDIT_NOTE');
      expect(money(invoice.totalAmountIncludingVat)).toBe(volume.T);
    });

    const flaBody = await test.step(
      'Read: GET billing-run/generate-invoice-data FinalLiabilityAmount',
      async () => getGenerateInvoiceData(Request, volume.creditNoteInvoiceId),
    );
    const actualFla = flaBody.FinalLiabilityAmount;
    assertFlaEquals(
      actualFla,
      expected,
      `TC-BE-1 Excel Case 1: FLA=round(-T-C,2) T=${volume.T} C=${C} (T=50 → -90)`,
    );
    assertFlaNotForbidden(actualFla, [10, 50], 'TC-BE-1 wiki template / unlinked compensation', expected);
    const sdMeta = assertFlaTemplaterExtras(flaBody, {
      signedFla: actualFla,
      compensationId: compensation.id,
      compensationNumber: compensation.number,
    });
    const words = flaBody.FinalLiabilityAmountWithWords;

    attachFlaSummary(TestRunSummary, {
      check: 'TC-BE-1 Credit Note Case 1 FLA',
      expectedResult: `FinalLiabilityAmount ${expected} (Excel -90 when T=50 C=40); words non-empty abs; index=0`,
      actualResult: `T=${volume.T} C=${C} FLA=${JSON.stringify(actualFla)} wordsLen=${String(words).trim().length} index=${linked.index} sd=${sdMeta.sdMatchNote}`,
      passed: money(actualFla) === expected,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3483_KEY,
        relevantEntityKeys: [...PDT_3483_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: volume.creditNoteInvoiceId,
          }),
          ...buildProductContractTabLinks(chain.contractId),
        },
        snapshot: {
          billedCustomerId: chain.billedCustomerId,
          compensationId: compensation.id,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
          T: volume.T,
          C,
          expectedFla: expected,
          actualFla,
          sdFirstKeys: sdMeta.sdFirstKeys,
          sdMatchNote: sdMeta.sdMatchNote,
        },
      });
    });
  });

  test(titleFor('TC-BE-2 Credit Note Case 2 — negative customer liability (Excel -10, not story -40)'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3483_TEST_TIMEOUT_MS);
    const fx: Pdt3483Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniquePdt3483CompensationNumber('TCBE2');

    const chain = await buildPdt3470IapRealChain(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: chain.billedCustomerId,
      recipientId: chain.recipientId,
    });

    const volume = await startPdt3483VolumeCreditNoteDraft(fx, chain);
    const C = scaleC(volume.T, -40);
    const expected = expectedFla('CREDIT_NOTE', volume.T, C);

    const compensation = await test.step(
      'Precondition: POST government-compensations negative C after draft',
      async () =>
        postPdt3483SignedCompensation(fx, {
          customerId: chain.billedCustomerId,
          podId: chain.billedPodId,
          recipientId: chain.recipientId,
          documentPeriod: chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3483-TC-BE-2',
        }),
    );
    TestRunSummary.registerPayload('compensation', {
      id: compensation.id,
      number: compensation.number,
      documentAmount: C,
    });

    await test.step('Action: PATCH start-generating (PDF) on volume Credit Note run', async () => {
      await generatePdt3470VolumeRun(fx, volume.volumeBillingRunId);
    });

    await test.step('Read: compensation index and invoice.id', async () => {
      const view = await getCompensation(Request, compensation.id);
      expect(view.index).toBe(0);
      expect(nestedId(view.invoice)).toBe(volume.creditNoteInvoiceId);
      const invoice = await getInvoice(Request, volume.creditNoteInvoiceId);
      expect(invoice.invoiceDocumentType).toBe('CREDIT_NOTE');
    });

    const flaBody = await test.step(
      'Read: GET billing-run/generate-invoice-data FinalLiabilityAmount',
      async () => getGenerateInvoiceData(Request, volume.creditNoteInvoiceId),
    );
    const actualFla = flaBody.FinalLiabilityAmount;
    assertFlaEquals(
      actualFla,
      expected,
      `TC-BE-2 Excel Case 2: FLA=round(-T-C,2) T=${volume.T} C=${C} (T=50 C=-40 → -10, NOT story -40)`,
    );
    assertFlaNotForbidden(actualFla, [-40, 10], 'TC-BE-2 Word AC-4 -40 / missing CN signBase +10', expected);

    attachFlaSummary(TestRunSummary, {
      check: 'TC-BE-2 Credit Note Case 2 Excel -10',
      expectedResult: `FinalLiabilityAmount ${expected} (Excel -10 when T=50 C=-40); fail -40 and +10`,
      actualResult: `T=${volume.T} C=${C} FLA=${JSON.stringify(actualFla)}`,
      passed: money(actualFla) === expected,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3483_KEY,
        relevantEntityKeys: [...PDT_3483_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: volume.creditNoteInvoiceId,
          }),
          ...buildProductContractTabLinks(chain.contractId),
        },
        snapshot: {
          compensationId: compensation.id,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
          T: volume.T,
          C,
          expectedFla: expected,
          actualFla,
        },
      });
    });
  });

  test(titleFor('TC-BE-3 Credit Note Case 3 — customer liability exceeds T'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3483_TEST_TIMEOUT_MS);
    const fx: Pdt3483Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniquePdt3483CompensationNumber('TCBE3');

    const chain = await buildPdt3470IapRealChain(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: chain.billedCustomerId,
      recipientId: chain.recipientId,
    });

    const volume = await startPdt3483VolumeCreditNoteDraft(fx, chain);
    const C = scaleC(volume.T, -90);
    const expected = expectedFla('CREDIT_NOTE', volume.T, C);

    const compensation = await postPdt3483SignedCompensation(fx, {
      customerId: chain.billedCustomerId,
      podId: chain.billedPodId,
      recipientId: chain.recipientId,
      documentPeriod: chain.documentPeriod,
      documentAmount: C,
      number,
      reason: 'PDT-3483-TC-BE-3',
    });
    TestRunSummary.registerPayload('compensation', {
      id: compensation.id,
      number: compensation.number,
      documentAmount: C,
    });

    await test.step('Action: PATCH start-generating (PDF) on volume Credit Note run', async () => {
      await generatePdt3470VolumeRun(fx, volume.volumeBillingRunId);
    });
    await test.step('Read: compensation index and invoice.id', async () => {
      const view = await getCompensation(Request, compensation.id);
      expect(view.index).toBe(0);
      expect(nestedId(view.invoice)).toBe(volume.creditNoteInvoiceId);
      expect((await getInvoice(Request, volume.creditNoteInvoiceId)).invoiceDocumentType).toBe(
        'CREDIT_NOTE',
      );
    });

    const flaBody = await test.step(
      'Read: GET billing-run/generate-invoice-data FinalLiabilityAmount',
      async () => getGenerateInvoiceData(Request, volume.creditNoteInvoiceId),
    );
    const actualFla = flaBody.FinalLiabilityAmount;
    assertFlaEquals(
      actualFla,
      expected,
      `TC-BE-3 Excel Case 3: FLA=round(-T-C,2) T=${volume.T} C=${C} (T=50 C=-90 → +40)`,
    );
    assertFlaNotForbidden(actualFla, [-40, 90], 'TC-BE-3 extra abs/minus or omitted CN signBase', expected);

    attachFlaSummary(TestRunSummary, {
      check: 'TC-BE-3 Credit Note Case 3 FLA +40',
      expectedResult: `FinalLiabilityAmount ${expected} (Excel +40 when T=50 C=-90); fail -40`,
      actualResult: `T=${volume.T} C=${C} FLA=${JSON.stringify(actualFla)}`,
      passed: money(actualFla) === expected,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3483_KEY,
        relevantEntityKeys: [...PDT_3483_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: volume.creditNoteInvoiceId,
          }),
          ...buildProductContractTabLinks(chain.contractId),
        },
        snapshot: {
          compensationId: compensation.id,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
          T: volume.T,
          C,
          expectedFla: expected,
          actualFla,
        },
      });
    });
  });

  test(titleFor('TC-BE-4 Debit Note Case 4 — customer receivable partially offsets debit'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3483_TEST_TIMEOUT_MS);
    const fx: Pdt3483Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniquePdt3483CompensationNumber('TCBE4');

    const draft = await startPdt3483DebitNoteDraft(fx);
    const C = scaleC(draft.T, 40);
    const compensation = await postPdt3483SignedCompensation(fx, {
      customerId: draft.chain.customerId,
      podId: draft.chain.podId,
      recipientId: draft.chain.recipientCustomerId,
      documentPeriod: draft.chain.documentPeriod,
      documentAmount: C,
      number,
      reason: 'PDT-3483-TC-BE-4',
    });
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: draft.chain.customerId,
      recipientId: draft.chain.recipientCustomerId,
    });
    TestRunSummary.registerPayload('compensation', {
      id: compensation.id,
      number: compensation.number,
      documentAmount: C,
    });

    await test.step('Action: PATCH start-generating on FOR_VOLUMES run', async () => {
      await startGeneratingAndWaitGenerated(Request, draft.volumeBillingRunId);
    });

    const linked = await test.step(
      'Read: linked invoice is INVOICE or DEBIT_NOTE (not CREDIT_NOTE)',
      async () => assertLinkedInvoiceIsDebitNote(Request, compensation.id),
    );
    expect(linked.compensation.index).toBe(0);

    const flaBody = await test.step(
      'Read: GET billing-run/generate-invoice-data FinalLiabilityAmount',
      async () => getGenerateInvoiceData(Request, linked.linkedInvoiceId),
    );
    const expected = expectedFla(String(linked.invoice.invoiceDocumentType ?? 'INVOICE'), linked.T, C);
    const actualFla = flaBody.FinalLiabilityAmount;
    assertFlaEquals(
      actualFla,
      expected,
      `TC-BE-4 Excel Case 4: FLA=round(T-C,2) T=${linked.T} C=${C} (T=50 → +10)`,
    );
    assertFlaNotForbidden(actualFla, [-10, 50], 'TC-BE-4 CN signBase wrongly applied / unlinked C', expected);

    attachFlaSummary(TestRunSummary, {
      check: 'TC-BE-4 Debit Note Case 4 FLA',
      expectedResult: `Linked INVOICE or DEBIT_NOTE; FinalLiabilityAmount ${expected}`,
      actualResult: `linked=${linked.linkedInvoiceId} type=${linked.invoice.invoiceDocumentType} T=${linked.T} C=${C} FLA=${JSON.stringify(actualFla)}`,
      passed: money(actualFla) === expected,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3483_KEY,
        relevantEntityKeys: [...PDT_3483_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: linked.linkedInvoiceId,
          }),
          ...buildProductContractTabLinks(draft.chain.contractId),
        },
        snapshot: {
          volumeBillingRunId: draft.volumeBillingRunId,
          compensationId: compensation.id,
          linkedInvoiceId: linked.linkedInvoiceId,
          T: linked.T,
          C,
          expectedFla: expected,
          actualFla,
        },
      });
    });
  });

  test(titleFor('TC-BE-5 Debit Note Case 5 — customer receivable exceeds T'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3483_TEST_TIMEOUT_MS);
    const fx: Pdt3483Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniquePdt3483CompensationNumber('TCBE5');

    const draft = await startPdt3483DebitNoteDraft(fx);
    const C = scaleC(draft.T, 90);
    const compensation = await postPdt3483SignedCompensation(fx, {
      customerId: draft.chain.customerId,
      podId: draft.chain.podId,
      recipientId: draft.chain.recipientCustomerId,
      documentPeriod: draft.chain.documentPeriod,
      documentAmount: C,
      number,
      reason: 'PDT-3483-TC-BE-5',
    });
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: draft.chain.customerId,
      recipientId: draft.chain.recipientCustomerId,
    });
    TestRunSummary.registerPayload('compensation', { id: compensation.id, documentAmount: C });

    await test.step('Action: PATCH start-generating on FOR_VOLUMES run', async () => {
      await startGeneratingAndWaitGenerated(Request, draft.volumeBillingRunId);
    });
    const linked = await test.step(
      'Read: linked invoice is INVOICE or DEBIT_NOTE (not CREDIT_NOTE)',
      async () => assertLinkedInvoiceIsDebitNote(Request, compensation.id),
    );
    expect(linked.compensation.index).toBe(0);
    const flaBody = await test.step(
      'Read: GET billing-run/generate-invoice-data FinalLiabilityAmount',
      async () => getGenerateInvoiceData(Request, linked.linkedInvoiceId),
    );
    const expected = expectedFla(String(linked.invoice.invoiceDocumentType ?? 'INVOICE'), linked.T, C);
    const actualFla = flaBody.FinalLiabilityAmount;
    assertFlaEquals(
      actualFla,
      expected,
      `TC-BE-5 Excel Case 5: FLA=round(T-C,2) T=${linked.T} C=${C} (T=50 C=90 → -40)`,
    );
    assertFlaNotForbidden(actualFla, [40, 50], 'TC-BE-5 absolute-only display / omitted C', expected);

    attachFlaSummary(TestRunSummary, {
      check: 'TC-BE-5 Debit Note Case 5 FLA',
      expectedResult: `Linked INVOICE or DEBIT_NOTE; FinalLiabilityAmount ${expected}`,
      actualResult: `T=${linked.T} C=${C} FLA=${JSON.stringify(actualFla)}`,
      passed: money(actualFla) === expected,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3483_KEY,
        relevantEntityKeys: [...PDT_3483_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: linked.linkedInvoiceId,
          }),
          ...buildProductContractTabLinks(draft.chain.contractId),
        },
        snapshot: {
          compensationId: compensation.id,
          linkedInvoiceId: linked.linkedInvoiceId,
          T: linked.T,
          C,
          expectedFla: expected,
          actualFla,
        },
      });
    });
  });

  test(titleFor('TC-BE-6 Debit Note Case 6 — additional customer liability'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3483_TEST_TIMEOUT_MS);
    const fx: Pdt3483Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniquePdt3483CompensationNumber('TCBE6');

    const draft = await startPdt3483DebitNoteDraft(fx);
    const C = scaleC(draft.T, -40);
    const compensation = await postPdt3483SignedCompensation(fx, {
      customerId: draft.chain.customerId,
      podId: draft.chain.podId,
      recipientId: draft.chain.recipientCustomerId,
      documentPeriod: draft.chain.documentPeriod,
      documentAmount: C,
      number,
      reason: 'PDT-3483-TC-BE-6',
    });
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: draft.chain.customerId,
      recipientId: draft.chain.recipientCustomerId,
    });
    TestRunSummary.registerPayload('compensation', { id: compensation.id, documentAmount: C });

    await test.step('Action: PATCH start-generating on FOR_VOLUMES run', async () => {
      await startGeneratingAndWaitGenerated(Request, draft.volumeBillingRunId);
    });
    const linked = await test.step(
      'Read: linked invoice is INVOICE or DEBIT_NOTE (not CREDIT_NOTE)',
      async () => assertLinkedInvoiceIsDebitNote(Request, compensation.id),
    );
    expect(linked.compensation.index).toBe(0);
    const flaBody = await test.step(
      'Read: GET billing-run/generate-invoice-data FinalLiabilityAmount',
      async () => getGenerateInvoiceData(Request, linked.linkedInvoiceId),
    );
    const expected = expectedFla(String(linked.invoice.invoiceDocumentType ?? 'INVOICE'), linked.T, C);
    const actualFla = flaBody.FinalLiabilityAmount;
    assertFlaEquals(
      actualFla,
      expected,
      `TC-BE-6 Excel Case 6: FLA=round(T-C,2) at PDF (not accounting) T=${linked.T} C=${C} (T=50 → +90)`,
    );
    assertFlaNotForbidden(actualFla, [-90, 50], 'TC-BE-6 CN minus display / omitted C', expected);

    attachFlaSummary(TestRunSummary, {
      check: 'TC-BE-6 Debit Note Case 6 FLA at PDF',
      expectedResult: `Linked INVOICE or DEBIT_NOTE; FinalLiabilityAmount ${expected} before accounting`,
      actualResult: `T=${linked.T} C=${C} FLA=${JSON.stringify(actualFla)}`,
      passed: money(actualFla) === expected,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3483_KEY,
        relevantEntityKeys: [...PDT_3483_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: linked.linkedInvoiceId,
          }),
          ...buildProductContractTabLinks(draft.chain.contractId),
        },
        snapshot: {
          compensationId: compensation.id,
          linkedInvoiceId: linked.linkedInvoiceId,
          T: linked.T,
          C,
          expectedFla: expected,
          actualFla,
        },
      });
    });
  });

  test(titleFor('TC-BE-7 Debit case zero FLA — customer receivable equals T'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3483_TEST_TIMEOUT_MS);
    const fx: Pdt3483Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniquePdt3483CompensationNumber('TCBE7');

    const draft = await startPdt3483DebitNoteDraft(fx);
    const C = scaleC(draft.T, 50);
    const compensation = await postPdt3483SignedCompensation(fx, {
      customerId: draft.chain.customerId,
      podId: draft.chain.podId,
      recipientId: draft.chain.recipientCustomerId,
      documentPeriod: draft.chain.documentPeriod,
      documentAmount: C,
      number,
      reason: 'PDT-3483-TC-BE-7',
    });
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: draft.chain.customerId,
      recipientId: draft.chain.recipientCustomerId,
    });
    TestRunSummary.registerPayload('compensation', { id: compensation.id, documentAmount: C });

    await test.step('Action: PATCH start-generating on FOR_VOLUMES run', async () => {
      await startGeneratingAndWaitGenerated(Request, draft.volumeBillingRunId);
    });
    const linked = await test.step(
      'Read: linked invoice is INVOICE or DEBIT_NOTE (not CREDIT_NOTE)',
      async () => assertLinkedInvoiceIsDebitNote(Request, compensation.id),
    );
    expect(linked.compensation.index).toBe(0);
    expect(money(C), 'C equals T so debit FLA is 0').toBe(money(linked.T));
    const flaBody = await test.step(
      'Read: GET billing-run/generate-invoice-data FinalLiabilityAmount',
      async () => getGenerateInvoiceData(Request, linked.linkedInvoiceId),
    );
    const expected = expectedFla(String(linked.invoice.invoiceDocumentType ?? 'INVOICE'), linked.T, C);
    expect(expected, 'identity T-C with C=T is 0').toBe(0);
    const actualFla = flaBody.FinalLiabilityAmount;
    assertFlaEquals(
      actualFla,
      0,
      `TC-BE-7 zero FLA: round(T-C,2)=0 T=${linked.T} C=${C}`,
    );
    assertFlaNotForbidden(
      actualFla,
      [linked.T, -linked.T, money(-2 * linked.T)],
      'TC-BE-7 unlinked C / CN signBase / double T',
      0,
    );

    attachFlaSummary(TestRunSummary, {
      check: 'TC-BE-7 Debit zero FLA',
      expectedResult: `Linked INVOICE or DEBIT_NOTE; FinalLiabilityAmount 0`,
      actualResult: `type=${linked.invoice.invoiceDocumentType} T=${linked.T} C=${C} FLA=${JSON.stringify(actualFla)}`,
      passed: money(actualFla) === 0,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3483_KEY,
        relevantEntityKeys: [...PDT_3483_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: linked.linkedInvoiceId,
          }),
          ...buildProductContractTabLinks(draft.chain.contractId),
        },
        snapshot: {
          volumeBillingRunId: draft.volumeBillingRunId,
          compensationId: compensation.id,
          linkedInvoiceId: linked.linkedInvoiceId,
          T: linked.T,
          C,
          expectedFla: expected,
          actualFla,
        },
      });
    });
  });

  test(titleFor('TC-BE-8 Credit Note zero FLA — customer liability equals T'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3483_TEST_TIMEOUT_MS);
    const fx: Pdt3483Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniquePdt3483CompensationNumber('TCBE8');

    const chain = await buildPdt3470IapRealChain(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: chain.billedCustomerId,
      billedCustomerIdentifier: chain.billedCustomerIdentifier,
      recipientId: chain.recipientId,
    });

    const volume = await startPdt3483VolumeCreditNoteDraft(fx, chain);
    const C = scaleC(volume.T, -50);
    const expected = expectedFla('CREDIT_NOTE', volume.T, C);
    expect(expected, 'identity -T-C with C=-T is 0').toBe(0);

    const compensation = await test.step(
      'Precondition: POST government-compensations after draft (C = -T)',
      async () =>
        postPdt3483SignedCompensation(fx, {
          customerId: chain.billedCustomerId,
          podId: chain.billedPodId,
          recipientId: chain.recipientId,
          documentPeriod: chain.documentPeriod,
          documentAmount: C,
          number,
          reason: 'PDT-3483-TC-BE-8',
        }),
    );
    TestRunSummary.registerPayload('compensation', {
      id: compensation.id,
      number: compensation.number,
      documentAmount: C,
    });

    await test.step('Action: PATCH start-generating (PDF) on volume Credit Note run', async () => {
      await generatePdt3470VolumeRun(fx, volume.volumeBillingRunId);
    });

    await test.step('Read: compensation index and invoice.id', async () => {
      const view = await getCompensation(Request, compensation.id);
      expect(view.index, 'compensation index after PDF').toBe(0);
      expect(nestedId(view.invoice), 'compensation.invoice.id is the Credit Note').toBe(
        volume.creditNoteInvoiceId,
      );
    });

    const flaBody = await test.step(
      'Read: GET billing-run/generate-invoice-data FinalLiabilityAmount',
      async () => getGenerateInvoiceData(Request, volume.creditNoteInvoiceId),
    );
    const actualFla = flaBody.FinalLiabilityAmount;
    assertFlaEquals(
      actualFla,
      0,
      `TC-BE-8 zero FLA: round(-T-C,2)=0 T=${volume.T} C=${C}`,
    );
    assertFlaNotForbidden(
      actualFla,
      [money(-2 * volume.T), money(2 * volume.T), volume.T],
      'TC-BE-8 unlinked C / missing CN signBase / leftover T',
      0,
    );

    attachFlaSummary(TestRunSummary, {
      check: 'TC-BE-8 Credit Note zero FLA',
      expectedResult: `CREDIT_NOTE; FinalLiabilityAmount 0`,
      actualResult: `T=${volume.T} C=${C} FLA=${JSON.stringify(actualFla)}`,
      passed: money(actualFla) === 0,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3483_KEY,
        relevantEntityKeys: [...PDT_3483_RELEVANT_ENTITY_KEYS],
        extraLinks: {
          ...pdt3470PortalExtraLinks({
            compensationId: compensation.id,
            creditNoteInvoiceId: volume.creditNoteInvoiceId,
          }),
          ...buildProductContractTabLinks(chain.contractId),
        },
        snapshot: {
          compensationId: compensation.id,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
          T: volume.T,
          C,
          expectedFla: expected,
          actualFla,
        },
      });
    });
  });

});
