/**
 * PDT-3368 — Invoice Cancellation Process - Government Compensations.
 *
 * Reference spec(s):
 * - tests/cursor/GC-REG-government-compensation-process.spec.ts
 * - tests/cursor/gc-reg-government-compensation-regression.fixtures.ts
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 * - tests/cursor/dev-volume-billing-two-compensations.fixtures.ts
 * - tests/cursor/PHN-3992-invoice-cancel-closed-accounting-period.spec.ts / .fixtures.ts
 * - tests/receivableManagement/Rescheduling/happyPass.spec.ts
 *
 * Swagger (dev, refreshed this session): POST /invoice-cancellation
 * (CreateInvoiceCancellationRequest required templateId; invoices, fileId, taxEventDate);
 * InvoiceCancellationResponse: invoiceCancellationId, processId;
 * PATCH /invoice/regenerate-compensations?id=; GET /process/{id};
 * GET /government-compensations/{id}; GET /customer-liability/{id}; GET /customer-receivable/{id}.
 *
 * Backend TC: Cursor-Project/test_cases/Backend/Invoice_Cancellation_Government_Compensations.md
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  PDT_3368_KEY,
  PDT_3368_TITLE,
  UNIQUE_RESULT_FRAGMENT,
  RESCHEDULING_BLOCK_FRAGMENT,
  REGEN_NOT_REAL_FRAGMENT,
  ALREADY_CANCELLED_FRAGMENT,
  BILLING_TIMEOUT_MS,
  accountGcsOnRealDebit,
  addBillingByProfileShiftedMonth,
  assertAmountZero,
  assertGcUninvoicedEmptyFinancials,
  assertNegativeApplyAttachments,
  assertPositiveApplyAttachments,
  completeForVolumesToAccounted,
  createExecutedReschedulingForLiability,
  extractApiErrorMessage,
  downloadProcessReportText,
  extractPartyCustomerId,
  extraPortalLinks,
  getCompensation,
  getCustomerLiability,
  getCustomerReceivable,
  getInvoice,
  getProcessBody,
  patchRegenerateCompensationsRaw,
  postCancelByInvoiceNumber,
  processErrorText,
  reschedulingIncludesLiability,
  resolveInvoiceMainCustomerLiability,
  resolveOpenTaxEventDate,
  runDevVolCompContractAndBbpPrechain,
  runMultiPodForVolumesPrechain,
  snapshotGcLinks,
  toAmountNumber,
  waitInvoiceCancelled,
  waitProcessFinished,
  type Pdt3368Fx,
  type RealDebitWithGc,
} from './pdt-3368-invoice-cancellation-government-compensations.fixtures';

function assertRowActive(view: Record<string, unknown>, label: string): void {
  expect(view.status, `${label} status present`).toBeTruthy();
  expect(String(view.status), `${label} still ACTIVE`).toBe('ACTIVE');
}

async function assertStoredZeroAndActive(
  Request: Pdt3368Fx['Request'],
  kind: 'liability' | 'receivable',
  id: number | null,
  label: string,
): Promise<void> {
  await assertAmountZero(Request, kind, id, label);
  if (!id) return;
  const view =
    kind === 'liability'
      ? await getCustomerLiability(Request, id)
      : await getCustomerReceivable(Request, id);
  assertRowActive(view as Record<string, unknown>, label);
}

function assertNoUniqueResult(processBody: Record<string, unknown>): void {
  expect(processErrorText(processBody), 'no Hibernate unique-result error').not.toContain(
    UNIQUE_RESULT_FRAGMENT,
  );
}

async function cancelRealExpect201(
  Request: Pdt3368Fx['Request'],
  billed: Pick<RealDebitWithGc, 'invoiceId' | 'invoiceNumber'>,
  taxEventDate: string,
): Promise<{ processId: number; cancellationId: number; body: Record<string, unknown> }> {
  const cancel = await postCancelByInvoiceNumber(Request, billed.invoiceNumber, taxEventDate);
  expect(cancel.status, `POST /invoice-cancellation HTTP 201, body=${cancel.rawText}`).toBe(201);
  expect(cancel.processId, 'processId').toBeGreaterThan(0);
  return {
    processId: cancel.processId,
    cancellationId: cancel.cancellationId,
    body: cancel.body,
  };
}

test.describe(`[${PDT_3368_KEY}]: ${PDT_3368_TITLE}`, {
  tag: ['@billing', '@compensations', '@PDT-3368'],
}, () => {
  test(`[${PDT_3368_KEY}]: TC-BE-1 – Cancel Real debit note with one positive government compensation — both sides to 0, GC UNINVOICED`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: billed customer + recipient + POD + FOR_VOLUMES contract/BBP', async () =>
      runDevVolCompContractAndBbpPrechain(fx),
    );

    const billed = await test.step('Precondition: +20 GC then Standard Billing through accounting', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: 20, podId: pre.podId, reason: 'PDT-3368-BE-1-positive' },
      ]),
    );
    TestRunSummary.registerPayload('compensation', billed.gcs[0]!.payload);

    const gc = billed.gcs[0]!;
    const main = await test.step('Precondition: positive apply attachments + main L reduced by 20', async () => {
      assertPositiveApplyAttachments(gc.view, billed.invoiceId, 'TC-BE-1');
      const invoice = await getInvoice(Request, billed.invoiceId);
      expect(toAmountNumber(invoice.totalAmountIncludingVat), 'invoice total > 20').toBeGreaterThan(20);
      const resolved = await resolveInvoiceMainCustomerLiability(Request, billed.invoiceId, pre.customerId, [
        gc.links.liabilityForRecipientId ?? 0,
      ]);
      expect(toAmountNumber(resolved.currentAmount), 'main L current = initial − 20').toBe(
        toAmountNumber(resolved.initialAmount) - 20,
      );
      TestRunSummary.registerPayload('invoice', {
        invoiceId: billed.invoiceId,
        invoiceNumber: billed.invoiceNumber,
        mainLiabilityId: resolved.id,
      });
      return resolved;
    });

    const taxEventDate = await test.step('Precondition: OPEN taxEventDate', async () =>
      resolveOpenTaxEventDate(Request),
    );

    const cancel = await test.step('POST /invoice-cancellation by invoice number', async () =>
      cancelRealExpect201(Request, billed, taxEventDate),
    );
    Responses.invoiceCancellation.push(cancel.body);
    TestRunSummary.registerPayload('invoiceCancellation', cancel.body);

    await test.step('Poll invoice CANCELLED', async () => {
      await waitInvoiceCancelled(Request, billed.invoiceId);
    });

    await test.step('Unwind: GC UNINVOICED, history L/R at 0, no unique-result', async () => {
      const process = await getProcessBody(Request, cancel.processId);
      assertNoUniqueResult(process);
      const unwound = await getCompensation(Request, gc.id);
      assertGcUninvoicedEmptyFinancials(unwound, 'TC-BE-1');
      const mainAfter = await getCustomerLiability(Request, main.id);
      await assertAmountZero(Request, 'liability', main.id, 'main invoice L');
      assertRowActive(mainAfter as Record<string, unknown>, 'main invoice L');
      await assertStoredZeroAndActive(Request, 'liability', gc.links.liabilityForRecipientId, 'recipient L');
      await assertStoredZeroAndActive(Request, 'receivable', gc.links.receivableForCustomerId, 'customer R');
      const invoiceAfter = await getInvoice(Request, billed.invoiceId);
      const settlementReceivables = (invoiceAfter.liabilitiesAndReceivables ?? []).filter(
        (row) => String(row.type ?? '').toUpperCase() === 'RECEIVABLE',
      );
      expect(
        settlementReceivables.length,
        'new cancellation R and government settlement R exist on the cancelled invoice',
      ).toBeGreaterThanOrEqual(2);
      for (const row of settlementReceivables) {
        await assertAmountZero(Request, 'receivable', Number(row.id), `settlement R ${row.id}`);
      }
      TestRunSummary.recordCheck({
        check: 'TC-BE-1 positive GC cancel unwind',
        expectedResult:
          'HTTP 201; invoice CANCELLED; GC UNINVOICED with empty invoice/L/R; main L, recipient L and customer R currentAmount 0 and still ACTIVE; no unique-result.',
        actualResult: `As expected — processId=${cancel.processId} invoice=${billed.invoiceId} gcId=${gc.id}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [gc.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: { tc: 'TC-BE-1', invoiceId: billed.invoiceId, gcId: gc.id, processId: cancel.processId },
      });
    });
  });

  test(`[${PDT_3368_KEY}]: TC-BE-2 – Cancel Real debit note with one negative government compensation — no unique-result, extra customer L cleared`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: billed customer + recipient + POD + FOR_VOLUMES contract/BBP', async () =>
      runDevVolCompContractAndBbpPrechain(fx),
    );

    const billed = await test.step('Precondition: -20 GC then Standard Billing through accounting', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: -20, podId: pre.podId, reason: 'PDT-3368-BE-2-negative' },
      ]),
    );
    const gc = billed.gcs[0]!;
    TestRunSummary.registerPayload('compensation', gc.payload);

    const main = await test.step('Precondition: negative apply — two customer L, main not offset by 20', async () => {
      assertNegativeApplyAttachments(gc.view, billed.invoiceId, 'TC-BE-2');
      const resolved = await resolveInvoiceMainCustomerLiability(Request, billed.invoiceId, pre.customerId, [
        gc.links.liabilityForCustomerId ?? 0,
      ]);
      expect(toAmountNumber(resolved.currentAmount), 'main L not reduced by 20 at invoicing').toBe(
        toAmountNumber(resolved.initialAmount),
      );
      TestRunSummary.registerPayload('invoice', {
        invoiceId: billed.invoiceId,
        invoiceNumber: billed.invoiceNumber,
        mainLiabilityId: resolved.id,
        compensationLiabilityId: gc.links.liabilityForCustomerId,
      });
      return resolved;
    });

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const cancel = await test.step('POST /invoice-cancellation', async () =>
      cancelRealExpect201(Request, billed, taxEventDate),
    );
    Responses.invoiceCancellation.push(cancel.body);
    TestRunSummary.registerPayload('invoiceCancellation', cancel.body);

    await test.step('Poll invoice CANCELLED', async () => {
      await waitInvoiceCancelled(Request, billed.invoiceId);
    });

    await test.step('Unwind: no unique-result 2; both customer L and recipient R at 0', async () => {
      assertNoUniqueResult(await getProcessBody(Request, cancel.processId));
      assertGcUninvoicedEmptyFinancials(await getCompensation(Request, gc.id), 'TC-BE-2');
      await assertStoredZeroAndActive(Request, 'liability', main.id, 'main invoice L');
      await assertStoredZeroAndActive(
        Request,
        'liability',
        gc.links.liabilityForCustomerId,
        'compensation customer L',
      );
      await assertStoredZeroAndActive(Request, 'receivable', gc.links.receivableForRecipientId, 'recipient R');
      TestRunSummary.recordCheck({
        check: 'TC-BE-2 negative GC cancel (PDT-3181 unique-result 2 path)',
        expectedResult:
          'HTTP 201; CANCELLED; no unique-result; GC UNINVOICED; main L, compensation L and recipient R currentAmount 0.',
        actualResult: `As expected — processId=${cancel.processId} gcId=${gc.id} mainL=${main.id}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [gc.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: { tc: 'TC-BE-2', invoiceId: billed.invoiceId, gcId: gc.id, processId: cancel.processId },
      });
    });
  });

  test(`[${PDT_3368_KEY}]: TC-BE-3 – Cancel Real debit note with mixed-sign compensations on the same invoice`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: billed customer + recipient + POD + FOR_VOLUMES contract/BBP', async () =>
      runDevVolCompContractAndBbpPrechain(fx),
    );

    const billed = await test.step('Precondition: +20 and -10 GCs then accounting', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: 20, podId: pre.podId, reason: 'PDT-3368-BE-3-pos' },
        { documentAmount: -10, podId: pre.podId, reason: 'PDT-3368-BE-3-neg' },
      ]),
    );
    const pos = billed.gcs[0]!;
    const neg = billed.gcs[1]!;
    TestRunSummary.registerPayload('compensation', { positive: pos.payload, negative: neg.payload });

    await test.step('Precondition: both GCs INVOICED on the same invoice', async () => {
      expect(pos.view.compensationStatus, 'positive GC INVOICED').toBe('INVOICED');
      expect(neg.view.compensationStatus, 'negative GC INVOICED').toBe('INVOICED');
      expect(pos.view.invoice?.id, 'positive on billed invoice').toBe(billed.invoiceId);
      expect(neg.view.invoice?.id, 'negative on billed invoice').toBe(billed.invoiceId);
      assertPositiveApplyAttachments(pos.view, billed.invoiceId, 'TC-BE-3 pos');
      assertNegativeApplyAttachments(neg.view, billed.invoiceId, 'TC-BE-3 neg');
    });

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const cancel = await test.step('POST /invoice-cancellation and poll CANCELLED', async () => {
      const posted = await cancelRealExpect201(Request, billed, taxEventDate);
      Responses.invoiceCancellation.push(posted.body);
      TestRunSummary.registerPayload('invoiceCancellation', posted.body);
      await waitInvoiceCancelled(Request, billed.invoiceId);
      return posted;
    });

    await test.step('Unwind both GCs independently', async () => {
      assertNoUniqueResult(await getProcessBody(Request, cancel.processId));
      assertGcUninvoicedEmptyFinancials(await getCompensation(Request, pos.id), 'TC-BE-3 pos');
      assertGcUninvoicedEmptyFinancials(await getCompensation(Request, neg.id), 'TC-BE-3 neg');
      await assertStoredZeroAndActive(Request, 'liability', pos.links.liabilityForRecipientId, 'pos recipient L');
      await assertStoredZeroAndActive(Request, 'receivable', pos.links.receivableForCustomerId, 'pos customer R');
      await assertStoredZeroAndActive(Request, 'liability', neg.links.liabilityForCustomerId, 'neg customer L');
      await assertStoredZeroAndActive(Request, 'receivable', neg.links.receivableForRecipientId, 'neg recipient R');
      TestRunSummary.recordCheck({
        check: 'TC-BE-3 mixed-sign cancel',
        expectedResult: 'Both GCs UNINVOICED; all four compensation L/R currentAmount 0; no unique-result.',
        actualResult: `As expected — pos=${pos.id} neg=${neg.id} invoice=${billed.invoiceId}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [pos.id, neg.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: { tc: 'TC-BE-3', invoiceId: billed.invoiceId, gcPositiveId: pos.id, gcNegativeId: neg.id },
      });
    });
  });

  test(`[${PDT_3368_KEY}]: TC-BE-4 – Cancel Real debit note with two positive GCs on two PODs — no unique-result 3`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: one contract, POD A + POD B, BBP both', async () =>
      runMultiPodForVolumesPrechain(fx),
    );

    const billed = await test.step('Precondition: +20 GC per POD, one CONTRACT billing run', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: 20, podId: pre.podAId, reason: 'PDT-3368-BE-4-podA' },
        { documentAmount: 20, podId: pre.podBId, reason: 'PDT-3368-BE-4-podB' },
      ]),
    );
    const gcA = billed.gcs[0]!;
    const gcB = billed.gcs[1]!;
    TestRunSummary.registerPayload('compensation', { podA: gcA.payload, podB: gcB.payload });

    await test.step('Precondition: both GCs INVOICED on the same invoice', async () => {
      assertPositiveApplyAttachments(gcA.view, billed.invoiceId, 'TC-BE-4 A');
      assertPositiveApplyAttachments(gcB.view, billed.invoiceId, 'TC-BE-4 B');
      expect(gcA.view.invoice?.id, 'GC-A same invoice').toBe(billed.invoiceId);
      expect(gcB.view.invoice?.id, 'GC-B same invoice').toBe(billed.invoiceId);
    });

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const cancel = await test.step('POST /invoice-cancellation and poll CANCELLED', async () => {
      const posted = await cancelRealExpect201(Request, billed, taxEventDate);
      Responses.invoiceCancellation.push(posted.body);
      TestRunSummary.registerPayload('invoiceCancellation', posted.body);
      await waitInvoiceCancelled(Request, billed.invoiceId);
      return posted;
    });

    await test.step('Unwind both POD GCs without unique-result 3', async () => {
      assertNoUniqueResult(await getProcessBody(Request, cancel.processId));
      assertGcUninvoicedEmptyFinancials(await getCompensation(Request, gcA.id), 'TC-BE-4 A');
      assertGcUninvoicedEmptyFinancials(await getCompensation(Request, gcB.id), 'TC-BE-4 B');
      await assertStoredZeroAndActive(Request, 'liability', gcA.links.liabilityForRecipientId, 'A recipient L');
      await assertStoredZeroAndActive(Request, 'liability', gcB.links.liabilityForRecipientId, 'B recipient L');
      await assertStoredZeroAndActive(Request, 'receivable', gcA.links.receivableForCustomerId, 'A customer R');
      await assertStoredZeroAndActive(Request, 'receivable', gcB.links.receivableForCustomerId, 'B customer R');
      TestRunSummary.recordCheck({
        check: 'TC-BE-4 two-POD cancel (PDT-3455 unique-result 3 path)',
        expectedResult: 'HTTP 201; CANCELLED; no unique-result; both GCs UNINVOICED; all stored L/R currentAmount 0.',
        actualResult: `As expected — gcA=${gcA.id} gcB=${gcB.id} invoice=${billed.invoiceId}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [gcA.id, gcB.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: { tc: 'TC-BE-4', invoiceId: billed.invoiceId, gcAId: gcA.id, gcBId: gcB.id },
      });
    });
  });

  test(`[${PDT_3368_KEY}]: TC-BE-5 – Cancel Real debit note with two positive GCs for two months on the same POD`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: billed customer + recipient + POD + BBP M-1', async () =>
      runDevVolCompContractAndBbpPrechain(fx),
    );

    const m2 = await test.step('Precondition: BBP for calendar month M-2 on the same POD', async () =>
      addBillingByProfileShiftedMonth(fx, 0, -1),
    );

    const billed = await test.step('Precondition: GC M-2 + GC M-1 then one FOR_VOLUMES run (maxEndDate 2060)', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: 20, podId: pre.podId, reason: 'PDT-3368-BE-5-m2', documentPeriod: m2.documentPeriod },
        { documentAmount: 20, podId: pre.podId, reason: 'PDT-3368-BE-5-m1', documentPeriod: pre.documentPeriod },
      ]),
    );
    const gcM2 = billed.gcs[0]!;
    const gcM1 = billed.gcs[1]!;
    TestRunSummary.registerPayload('compensation', { m2: gcM2.payload, m1: gcM1.payload });

    await test.step('Precondition: both GCs INVOICED on the same Real debit note', async () => {
      assertPositiveApplyAttachments(gcM2.view, billed.invoiceId, 'TC-BE-5 M-2');
      assertPositiveApplyAttachments(gcM1.view, billed.invoiceId, 'TC-BE-5 M-1');
      expect(gcM2.view.invoice?.id).toBe(billed.invoiceId);
      expect(gcM1.view.invoice?.id).toBe(billed.invoiceId);
    });

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const cancel = await test.step('POST /invoice-cancellation and poll CANCELLED', async () => {
      const posted = await cancelRealExpect201(Request, billed, taxEventDate);
      Responses.invoiceCancellation.push(posted.body);
      TestRunSummary.registerPayload('invoiceCancellation', posted.body);
      await waitInvoiceCancelled(Request, billed.invoiceId);
      return posted;
    });

    await test.step('Unwind both month GCs without unique-result', async () => {
      assertNoUniqueResult(await getProcessBody(Request, cancel.processId));
      assertGcUninvoicedEmptyFinancials(await getCompensation(Request, gcM2.id), 'TC-BE-5 M-2');
      assertGcUninvoicedEmptyFinancials(await getCompensation(Request, gcM1.id), 'TC-BE-5 M-1');
      await assertStoredZeroAndActive(Request, 'liability', gcM2.links.liabilityForRecipientId, 'M-2 recipient L');
      await assertStoredZeroAndActive(Request, 'liability', gcM1.links.liabilityForRecipientId, 'M-1 recipient L');
      await assertStoredZeroAndActive(Request, 'receivable', gcM2.links.receivableForCustomerId, 'M-2 customer R');
      await assertStoredZeroAndActive(Request, 'receivable', gcM1.links.receivableForCustomerId, 'M-1 customer R');
      TestRunSummary.recordCheck({
        check: 'TC-BE-5 two-month cancel',
        expectedResult: 'HTTP 201; CANCELLED; no unique-result; both GCs UNINVOICED; stored L/R currentAmount 0.',
        actualResult: `As expected — gcM2=${gcM2.id} gcM1=${gcM1.id} invoice=${billed.invoiceId}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [gcM2.id, gcM1.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: {
          tc: 'TC-BE-5',
          invoiceId: billed.invoiceId,
          gcM2Id: gcM2.id,
          gcM1Id: gcM1.id,
          documentPeriodM2: m2.documentPeriod,
          documentPeriodM1: pre.documentPeriod,
        },
      });
    });
  });

  test(`[${PDT_3368_KEY}]: TC-BE-6 – Same GC reused on a later Real invoice after cancellation`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: billed customer + recipient + POD + FOR_VOLUMES contract/BBP', async () =>
      runDevVolCompContractAndBbpPrechain(fx),
    );

    const billed1 = await test.step('Precondition: +20 GC then first REAL debit note', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: 20, podId: pre.podId, reason: 'PDT-3368-BE-6-reuse' },
      ]),
    );
    const gc = billed1.gcs[0]!;
    const oldLinks = snapshotGcLinks(gc.id, gc.view);
    TestRunSummary.registerPayload('compensation', gc.payload);

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const cancel = await test.step('POST /invoice-cancellation for invoice 1 and poll CANCELLED', async () => {
      const posted = await cancelRealExpect201(Request, billed1, taxEventDate);
      Responses.invoiceCancellation.push(posted.body);
      await waitInvoiceCancelled(Request, billed1.invoiceId);
      return posted;
    });
    assertGcUninvoicedEmptyFinancials(await getCompensation(Request, gc.id), 'TC-BE-6 after first cancel');

    await test.step('Precondition: later-period BBP + new Standard Billing through accounting', async () => {
      await addBillingByProfileShiftedMonth(fx, 0, 1);
    });

    const billed2 = await test.step('New FOR_VOLUMES run attaching the same UNINVOICED GC', async () =>
      completeForVolumesToAccounted(fx),
    );

    let invoiceId2 = 0;
    await test.step('Same gcId INVOICED on invoice 2 with a new L/R pair', async () => {
      const reused = await getCompensation(Request, gc.id);
      expect(reused.id, 'same GC row').toBe(gc.id);
      expect(reused.compensationStatus, 'GC INVOICED on second invoice').toBe('INVOICED');
      invoiceId2 = Number(reused.invoice?.id);
      expect(invoiceId2, 'GC attached to a new invoice').toBeGreaterThan(0);
      expect(invoiceId2, 'second invoice differs from cancelled invoice').not.toBe(billed1.invoiceId);
      const invoice2 = await getInvoice(Request, invoiceId2);
      expect(invoice2.invoiceStatus, 'invoice 2 REAL').toBe('REAL');
      const invoice1 = await getInvoice(Request, billed1.invoiceId);
      expect(invoice1.invoiceStatus, 'invoice 1 still CANCELLED').toBe('CANCELLED');
      const newLinks = snapshotGcLinks(gc.id, reused);
      expect(newLinks.liabilityForRecipientId, 'new recipient L').not.toBe(oldLinks.liabilityForRecipientId);
      expect(newLinks.receivableForCustomerId, 'new customer R').not.toBe(oldLinks.receivableForCustomerId);
      await assertAmountZero(Request, 'liability', oldLinks.liabilityForRecipientId, 'old recipient L');
      await assertAmountZero(Request, 'receivable', oldLinks.receivableForCustomerId, 'old customer R');
      TestRunSummary.recordCheck({
        check: 'TC-BE-6 GC reuse after cancel',
        expectedResult:
          'Same gcId INVOICED on invoice 2; new L/R ids; old L/R remain at 0; invoice 1 CANCELLED.',
        actualResult: `As expected — gcId=${gc.id} invoice1=${billed1.invoiceId} invoice2=${invoiceId2} newL=${newLinks.liabilityForRecipientId}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [gc.id],
          invoiceId: invoiceId2 || billed2.invoiceId,
          billingRunId: billed2.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: {
          tc: 'TC-BE-6',
          gcId: gc.id,
          invoiceId1: billed1.invoiceId,
          invoiceId2,
          oldLiabilityForRecipientId: oldLinks.liabilityForRecipientId,
        },
      });
    });
  });

  test(`[${PDT_3368_KEY}]: TC-BE-7 – PATCH regenerate-compensations on a cancelled invoice is refused`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: billed customer + recipient + POD + FOR_VOLUMES contract/BBP', async () =>
      runDevVolCompContractAndBbpPrechain(fx),
    );

    const billed = await test.step('Precondition: +20 GC then REAL debit note', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: 20, podId: pre.podId, reason: 'PDT-3368-BE-7-regen' },
      ]),
    );
    const gc = billed.gcs[0]!;
    TestRunSummary.registerPayload('compensation', gc.payload);

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const cancel = await cancelRealExpect201(Request, billed, taxEventDate);
    Responses.invoiceCancellation.push(cancel.body);
    await waitInvoiceCancelled(Request, billed.invoiceId);
    assertGcUninvoicedEmptyFinancials(await getCompensation(Request, gc.id), 'TC-BE-7 after cancel');

    await test.step('PATCH /invoice/regenerate-compensations refused for CANCELLED invoice', async () => {
      const regen = await patchRegenerateCompensationsRaw(Request, billed.invoiceId);
      expect(regen.status, 'must not be HTTP 202').not.toBe(202);
      expect(regen.status, `HTTP 400, body=${regen.bodyText}`).toBe(400);
      expect(regen.bodyText, 'REAL-status message').toContain(REGEN_NOT_REAL_FRAGMENT);

      const invoice = await getInvoice(Request, billed.invoiceId);
      expect(invoice.invoiceStatus, 'invoice remains CANCELLED').toBe('CANCELLED');
      const after = await getCompensation(Request, gc.id);
      assertGcUninvoicedEmptyFinancials(after, 'TC-BE-7 after refused regen');
      expect(snapshotGcLinks(gc.id, after).liabilityForRecipientId, 'no new L on cancelled invoice').toBeNull();
      TestRunSummary.recordCheck({
        check: 'TC-BE-7 regenerate refused after cancel',
        expectedResult: `PATCH HTTP 400 containing "${REGEN_NOT_REAL_FRAGMENT}"; invoice CANCELLED; GC UNINVOICED.`,
        actualResult: `As expected — HTTP ${regen.status} invoice=${invoice.invoiceStatus} gc=${after.compensationStatus}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [gc.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: { tc: 'TC-BE-7', invoiceId: billed.invoiceId, gcId: gc.id },
      });
    });
  });

  test(`[${PDT_3368_KEY}]: TC-BE-8 – Cancel is refused when the main invoice liability is in an active executed rescheduling`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: billed customer + recipient + POD + FOR_VOLUMES contract/BBP', async () =>
      runDevVolCompContractAndBbpPrechain(fx),
    );

    const billed = await test.step('Precondition: +20 GC then REAL debit note', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: 20, podId: pre.podId, reason: 'PDT-3368-BE-8-resched-main' },
      ]),
    );
    const gc = billed.gcs[0]!;
    TestRunSummary.registerPayload('compensation', gc.payload);
    assertPositiveApplyAttachments(gc.view, billed.invoiceId, 'TC-BE-8');

    const main = await resolveInvoiceMainCustomerLiability(Request, billed.invoiceId, pre.customerId, [
      gc.links.liabilityForRecipientId ?? 0,
    ]);
    expect(toAmountNumber(main.currentAmount), 'main L currentAmount > 0').toBeGreaterThan(0);

    const reschedulingId = await test.step('Precondition: EXECUTED ACTIVE rescheduling on main invoice L', async () =>
      createExecutedReschedulingForLiability(fx, { customerId: pre.customerId, liabilityId: main.id }),
    );
    TestRunSummary.registerPayload('rescheduling', { reschedulingId, liabilityId: main.id });

    const afterResched = await getCustomerLiability(Request, main.id);
    const amountAfterResched = toAmountNumber(afterResched.currentAmount);
    // Invoice L does not expose reschedulingShortResponse (only RESCHEDULING installment L does).
    // Verify link via GET /rescheduling/{id}.reschedulingLiabilityResponses (happyPass).
    expect(
      await reschedulingIncludesLiability(Request, Endpoints, reschedulingId, main.id),
      `GET /rescheduling/${reschedulingId} lists main liability ${main.id}`,
    ).toBe(true);

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const cancel = await test.step('POST /invoice-cancellation (async create may still be 201)', async () => {
      const posted = await postCancelByInvoiceNumber(Request, billed.invoiceNumber, taxEventDate);
      expect(posted.processId, 'processId').toBeGreaterThan(0);
      Responses.invoiceCancellation.push(posted.body);
      TestRunSummary.registerPayload('invoiceCancellation', posted.body);
      return posted;
    });

    await test.step('Process finishes with rescheduling block in report file; invoice stays REAL', async () => {
      await waitProcessFinished(Request, cancel.processId);
      const reportText = await downloadProcessReportText(Request, cancel.processId);
      expect(
        reportText,
        `MASS_IMPORT_ERROR_REPORT for process ${cancel.processId} must contain rescheduling block`,
      ).toContain(RESCHEDULING_BLOCK_FRAGMENT);
      expect(reportText, 'no unique-result in process report').not.toContain(UNIQUE_RESULT_FRAGMENT);

      const invoice = await getInvoice(Request, billed.invoiceId);
      expect(invoice.invoiceStatus, 'invoice remains REAL (not CANCELLED)').toBe('REAL');
      const stillInvoiced = await getCompensation(Request, gc.id);
      expect(stillInvoiced.compensationStatus, 'GC remains INVOICED').toBe('INVOICED');
      expect(stillInvoiced.invoice?.id, 'GC still on original invoice').toBe(billed.invoiceId);
      expect(snapshotGcLinks(gc.id, stillInvoiced).liabilityForRecipientId, 'L/R still populated').toBe(
        gc.links.liabilityForRecipientId,
      );
      const mainAfter = await getCustomerLiability(Request, main.id);
      expect(toAmountNumber(mainAfter.currentAmount), 'main L currentAmount unchanged').toBe(amountAfterResched);
      TestRunSummary.recordCheck({
        check: 'TC-BE-8 rescheduling on main L blocks cancel',
        expectedResult: `Invoice REAL; GC INVOICED; process report file contains "${RESCHEDULING_BLOCK_FRAGMENT}".`,
        actualResult: `As expected — invoice=${invoice.invoiceStatus} gc=${stillInvoiced.compensationStatus} reschedulingId=${reschedulingId} reportHasFragment=true`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [gc.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: { tc: 'TC-BE-8', invoiceId: billed.invoiceId, gcId: gc.id, mainLiabilityId: main.id, reschedulingId },
      });
    });
  });

  test(`[${PDT_3368_KEY}]: TC-BE-9 – Cancel is refused when the compensation recipient liability is in an active executed rescheduling`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: billed customer + recipient + POD + FOR_VOLUMES contract/BBP', async () =>
      runDevVolCompContractAndBbpPrechain(fx),
    );

    const billed = await test.step('Precondition: +20 GC then REAL debit note', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: 20, podId: pre.podId, reason: 'PDT-3368-BE-9-resched-gc-l' },
      ]),
    );
    const gc = billed.gcs[0]!;
    TestRunSummary.registerPayload('compensation', gc.payload);
    assertPositiveApplyAttachments(gc.view, billed.invoiceId, 'TC-BE-9');
    const recipientLId = gc.links.liabilityForRecipientId!;
    expect(recipientLId, 'recipient compensation L').toBeTruthy();

    const recipientL = await getCustomerLiability(Request, recipientLId);
    expect(toAmountNumber(recipientL.currentAmount), 'recipient L abs 20').toBe(20);
    expect(
      extractPartyCustomerId(recipientL as Record<string, unknown>),
      'recipient L customer = government recipient',
    ).toBe(pre.recipientCustomerId);

    const reschedulingId = await test.step('Precondition: EXECUTED ACTIVE rescheduling on recipient L', async () =>
      createExecutedReschedulingForLiability(fx, {
        customerId: pre.recipientCustomerId,
        liabilityId: recipientLId,
      }),
    );
    TestRunSummary.registerPayload('rescheduling', { reschedulingId, liabilityId: recipientLId });
    expect(
      await reschedulingIncludesLiability(Request, Endpoints, reschedulingId, recipientLId),
      `GET /rescheduling/${reschedulingId} lists recipient liability ${recipientLId}`,
    ).toBe(true);
    const amountAfterResched = toAmountNumber((await getCustomerLiability(Request, recipientLId)).currentAmount);

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const cancel = await test.step('POST /invoice-cancellation (async create may still be 201)', async () => {
      const posted = await postCancelByInvoiceNumber(Request, billed.invoiceNumber, taxEventDate);
      expect(posted.processId, 'processId').toBeGreaterThan(0);
      Responses.invoiceCancellation.push(posted.body);
      TestRunSummary.registerPayload('invoiceCancellation', posted.body);
      return posted;
    });

    await test.step('Process finishes with rescheduling block in report file; no partial settle', async () => {
      await waitProcessFinished(Request, cancel.processId);
      const reportText = await downloadProcessReportText(Request, cancel.processId);
      expect(
        reportText,
        `MASS_IMPORT_ERROR_REPORT for process ${cancel.processId} must contain rescheduling block`,
      ).toContain(RESCHEDULING_BLOCK_FRAGMENT);

      const invoice = await getInvoice(Request, billed.invoiceId);
      expect(invoice.invoiceStatus, 'invoice remains REAL (not CANCELLED)').toBe('REAL');
      const stillInvoiced = await getCompensation(Request, gc.id);
      expect(stillInvoiced.compensationStatus, 'GC remains INVOICED').toBe('INVOICED');
      expect(snapshotGcLinks(gc.id, stillInvoiced).liabilityForRecipientId).toBe(recipientLId);
      const recipientAfter = await getCustomerLiability(Request, recipientLId);
      expect(toAmountNumber(recipientAfter.currentAmount), 'recipient L unchanged').toBe(amountAfterResched);

      const main = await resolveInvoiceMainCustomerLiability(Request, billed.invoiceId, pre.customerId, [
        recipientLId,
      ]);
      expect(
        toAmountNumber(main.currentAmount),
        'main L not left at 0 while GC stays INVOICED',
      ).not.toBe(0);
      TestRunSummary.recordCheck({
        check: 'TC-BE-9 rescheduling on recipient GC L blocks cancel',
        expectedResult:
          'Invoice REAL; GC INVOICED; recipient L unchanged; main L not 0; process report file contains rescheduling error.',
        actualResult: `As expected — invoice=${invoice.invoiceStatus} gc=${stillInvoiced.compensationStatus} mainL=${toAmountNumber(main.currentAmount)} reportHasFragment=true`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [gc.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: cancel.processId,
        }),
        snapshot: {
          tc: 'TC-BE-9',
          invoiceId: billed.invoiceId,
          gcId: gc.id,
          liabilityForRecipientId: recipientLId,
          reschedulingId,
        },
      });
    });
  });

  test(`[${PDT_3368_KEY}]: TC-BE-10 – Cancel of an already cancelled invoice number is rejected and does not change the UNINVOICED GC`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(BILLING_TIMEOUT_MS);
    const fx: Pdt3368Fx = { Request, GeneratePayload, Responses, Endpoints };

    const pre = await test.step('Precondition: billed customer + recipient + POD + FOR_VOLUMES contract/BBP', async () =>
      runDevVolCompContractAndBbpPrechain(fx),
    );

    const billed = await test.step('Precondition: +20 GC then REAL debit note', async () =>
      accountGcsOnRealDebit(fx, pre, [
        { documentAmount: 20, podId: pre.podId, reason: 'PDT-3368-BE-10-already-cancelled' },
      ]),
    );
    const gc = billed.gcs[0]!;
    TestRunSummary.registerPayload('compensation', gc.payload);

    const taxEventDate = await resolveOpenTaxEventDate(Request);
    const first = await cancelRealExpect201(Request, billed, taxEventDate);
    Responses.invoiceCancellation.push(first.body);
    await waitInvoiceCancelled(Request, billed.invoiceId);
    const afterFirst = await getCompensation(Request, gc.id);
    assertGcUninvoicedEmptyFinancials(afterFirst, 'TC-BE-10 after first cancel');

    await test.step('Second POST /invoice-cancellation for the same invoice number is rejected', async () => {
      const second = await postCancelByInvoiceNumber(Request, billed.invoiceNumber, taxEventDate);
      const combined = `${extractApiErrorMessage(second.body, second.rawText)} ${second.rawText}`;
      expect(second.status, `HTTP 400, body=${combined}`).toBe(400);
      expect(combined, 'already reversed / not found fragment').toContain(ALREADY_CANCELLED_FRAGMENT);

      const invoice = await getInvoice(Request, billed.invoiceId);
      expect(invoice.invoiceStatus, 'invoice remains CANCELLED').toBe('CANCELLED');
      const afterSecond = await getCompensation(Request, gc.id);
      assertGcUninvoicedEmptyFinancials(afterSecond, 'TC-BE-10 after second reject');
      expect(afterSecond.invoice?.id ?? null, 'GC invoice still empty').toBeFalsy();
      TestRunSummary.recordCheck({
        check: 'TC-BE-10 second cancel rejected',
        expectedResult: `HTTP 400 containing "${ALREADY_CANCELLED_FRAGMENT}"; invoice CANCELLED; GC UNINVOICED.`,
        actualResult: `As expected — HTTP ${second.status} invoice=${invoice.invoiceStatus} gc=${afterSecond.compensationStatus}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3368_KEY,
        relevantEntityKeys: ['customer', 'pod', 'productContract', 'billingRun', 'invoice'],
        extraLinks: extraPortalLinks({
          gcIds: [gc.id],
          invoiceId: billed.invoiceId,
          billingRunId: billed.billingRunId,
          processId: first.processId,
        }),
        snapshot: { tc: 'TC-BE-10', invoiceId: billed.invoiceId, gcId: gc.id, firstProcessId: first.processId },
      });
    });
  });
});
