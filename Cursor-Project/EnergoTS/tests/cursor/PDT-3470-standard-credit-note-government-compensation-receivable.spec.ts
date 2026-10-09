/**
 * PDT-3470 — STANDARD Credit Note must create the government compensation
 * customer receivable (and paired government liability) after start-accounting.
 *
 * Backend TC: Cursor-Project/test_cases/Backend/PDT-3470_standard_credit_note_government_compensation_receivable.md
 *
 * Swagger refresh: update-swagger-specs.ps1 this session (all envs including test).
 *
 * Reference spec(s):
 * - tests/cursor/EXP-PARITY-03-interim-deduction-and-issue-date.spec.ts
 * - tests/cursor/exp-parity-03-interim-deduction-and-issue-date.fixtures.ts
 * - tests/cursor/pdt-3087-government-compensation-defer-to-pdf.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 *
 * IAP leftover auto-offset is not a pass criterion (Finding F1).
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  applyExpParityLocalAddress,
  postExpParityLegalCustomer,
} from './shared/exp-parity-customer.fixtures';
import { getInvoice } from './pdt-3087-government-compensation-defer-to-pdf.fixtures';
import {
  PDT_3470_KEY,
  PDT_3470_TEST_TIMEOUT_MS,
  PDT_3470_TITLE,
  PDT_3470_ZERO_AMOUNT_MESSAGE,
  accountPdt3470VolumeRun,
  asEntityId,
  buildPdt3470IapRealChain,
  createPdt3470Compensation,
  findCreditNoteReceivable,
  generatePdt3470VolumeRun,
  getCompensation,
  getLiability,
  getReceivable,
  isNullishEntityRef,
  listCompensationsByNumber,
  listLiabilitiesForCustomer,
  listReceivablesForCustomer,
  money,
  nestedId,
  pdt3470PortalExtraLinks,
  pdt3470RelevantKeys,
  postZeroAmountCompensationExpect400,
  receivablesOnCreditNote,
  startPdt3470VolumeCreditNoteDraft,
  uniqueCompensationNumber,
  type Pdt3470Fx,
} from './pdt-3470-standard-credit-note-government-compensation-receivable.fixtures';

const JIRA_TITLE = PDT_3470_TITLE;

function titleFor(scenario: string): string {
  return `[${PDT_3470_KEY}]: ${JIRA_TITLE} | ${scenario}`;
}

test.describe(`[${PDT_3470_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@billing', '@pdt-3470', '@cursor'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test(`[${PDT_3470_KEY}]: ${JIRA_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3470_TEST_TIMEOUT_MS);
    const fx: Pdt3470Fx = { Request, GeneratePayload, Responses, Endpoints };
    const compensationAmount = 80;
    const number = uniqueCompensationNumber('TCBE1');

    const chain = await buildPdt3470IapRealChain(fx);
    TestRunSummary.registerPayload('customer', {
      billedCustomerId: chain.billedCustomerId,
      billedCustomerIdentifier: chain.billedCustomerIdentifier,
      recipientId: chain.recipientId,
    });

    const compensation = await test.step(
      'Precondition: uninvoiced government compensation 80.00 on billed POD',
      async () =>
        createPdt3470Compensation(fx, {
          customerId: chain.billedCustomerId,
          podId: chain.billedPodId,
          recipientId: chain.recipientId,
          documentPeriod: chain.documentPeriod,
          documentAmount: compensationAmount,
          volumes: 80,
          price: 1,
          number,
          reason: 'PDT-3470-TC-BE-1',
        }),
    );
    TestRunSummary.registerPayload('compensation', { id: compensation.id, number: compensation.number });

    const volume = await startPdt3470VolumeCreditNoteDraft(fx, chain, compensationAmount);

    await test.step('Precondition: start-generating links compensation to Credit Note', async () => {
      await generatePdt3470VolumeRun(fx, volume.volumeBillingRunId);
      const linked = await getCompensation(Request, compensation.id);
      expect(linked.index, 'compensation index after PDF/generate').toBe(0);
      expect(nestedId(linked.invoice), 'compensation invoice is the Credit Note').toBe(
        volume.creditNoteInvoiceId,
      );
      expect(linked.compensationStatus, 'linking does not invoice yet').toBe('UNINVOICED');
    });

    await test.step('Action: start-accounting on the volume Credit Note run', async () => {
      await accountPdt3470VolumeRun(fx, volume.volumeBillingRunId, volume.creditNoteInvoiceId);
    });

    const invoiced = await test.step('Read: compensation after accounting', async () => {
      const view = await getCompensation(Request, compensation.id);
      expect(view.compensationStatus).toBe('INVOICED');
      expect(view.index).toBe(0);
      expect(nestedId(view.invoice)).toBe(volume.creditNoteInvoiceId);
      const receivableId = nestedId(view.receivableForCustomer);
      expect(
        receivableId,
        'PDT-3470: compensation receivableForCustomer must be created on STANDARD CREDIT_NOTE',
      ).toBeGreaterThan(0);
      return { view, receivableId: receivableId as number };
    });

    const compensationReceivable = await test.step(
      'Read: GET customer-receivable/{compensationReceivableId}',
      async () => {
        const detail = await getReceivable(Request, invoiced.receivableId);
        Responses.customerReceivable.push(detail);
        expect(detail.creationType).toBe('AUTOMATIC');
        expect(detail.status).toBe('ACTIVE');
        expect(nestedId(detail.customerResponse)).toBe(chain.billedCustomerId);
        expect(nestedId(detail.invoiceResponse)).toBe(volume.creditNoteInvoiceId);
        expect(money(detail.initialAmount)).toBe(money(compensationAmount));
        return detail;
      },
    );

    const listed = await test.step(
      'Read: GET customer-receivable listing by billed customer',
      async () => listReceivablesForCustomer(Request, chain.billedCustomerIdentifier),
    );
    expect(
      listed.some((row) => row.id === invoiced.receivableId),
      'listing contains compensation receivable id',
    ).toBe(true);

    const cnReceivable = await test.step(
      'Read: Credit Note receivable is a different AUTOMATIC row',
      async () =>
        findCreditNoteReceivable(Request, {
          customerIdentifier: chain.billedCustomerIdentifier,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
          compensationReceivableId: invoiced.receivableId,
          cnTotalInclVat: volume.cnTotalInclVat,
        }),
    );
    expect(cnReceivable.id, 'Credit Note receivable id ≠ compensation receivable id').not.toBe(
      invoiced.receivableId,
    );
    expect(money(cnReceivable.initialAmount)).toBe(money(Math.abs(volume.cnTotalInclVat)));
    expect(money(cnReceivable.initialAmount)).not.toBe(money(compensationAmount));

    TestRunSummary.recordCheck({
      check: 'TC-BE-1 STANDARD CREDIT_NOTE creates compensation receivable 80.00',
      expectedResult:
        'INVOICED compensation with AUTOMATIC customer receivable initialAmount 80.00 on the Credit Note, distinct from the CN receivable',
      actualResult: `compensation ${compensation.id} INVOICED; receivable ${compensationReceivable.id} amount ${money(compensationReceivable.initialAmount)}; CN receivable ${cnReceivable.id} amount ${money(cnReceivable.initialAmount)}`,
      passed: true,
    });

    await test.step('Attach test run summary', async () => {
      const extra = {
        ...pdt3470PortalExtraLinks({
          compensationId: compensation.id,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
        }),
        ...buildProductContractTabLinks(Responses),
      };
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3470_KEY,
        relevantEntityKeys: pdt3470RelevantKeys(),
        extraLinks: extra,
        snapshot: {
          billedCustomerId: chain.billedCustomerId,
          recipientId: chain.recipientId,
          compensationId: compensation.id,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
          compensationReceivableId: invoiced.receivableId,
          cnReceivableId: cnReceivable.id,
          cnTotalInclVat: volume.cnTotalInclVat,
        },
      });
    });
  });

  test(titleFor('TC-BE-2 government recipient liability 95.00'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3470_TEST_TIMEOUT_MS);
    const fx: Pdt3470Fx = { Request, GeneratePayload, Responses, Endpoints };
    const compensationAmount = 95;
    const number = uniqueCompensationNumber('TCBE2');

    const chain = await buildPdt3470IapRealChain(fx);
    const compensation = await test.step(
      'Precondition: uninvoiced government compensation 95.00 on billed POD',
      async () =>
        createPdt3470Compensation(fx, {
          customerId: chain.billedCustomerId,
          podId: chain.billedPodId,
          recipientId: chain.recipientId,
          documentPeriod: chain.documentPeriod,
          documentAmount: compensationAmount,
          volumes: 95,
          price: 1,
          number,
          reason: 'PDT-3470-TC-BE-2',
        }),
    );

    const volume = await startPdt3470VolumeCreditNoteDraft(fx, chain, compensationAmount);

    await test.step('Precondition: start-generating links compensation', async () => {
      await generatePdt3470VolumeRun(fx, volume.volumeBillingRunId);
      const linked = await getCompensation(Request, compensation.id);
      expect(linked.index).toBe(0);
      expect(nestedId(linked.invoice)).toBe(volume.creditNoteInvoiceId);
      expect(linked.compensationStatus).toBe('UNINVOICED');
    });

    await test.step('Action: start-accounting', async () => {
      await accountPdt3470VolumeRun(fx, volume.volumeBillingRunId, volume.creditNoteInvoiceId);
    });

    const invoiced = await test.step('Read: compensation government liability', async () => {
      const view = await getCompensation(Request, compensation.id);
      expect(view.compensationStatus).toBe('INVOICED');
      const liabilityId = nestedId(view.liabilityForRecipient);
      const receivableId = nestedId(view.receivableForCustomer);
      expect(
        liabilityId,
        'PDT-3470: liabilityForRecipient must be created on STANDARD CREDIT_NOTE',
      ).toBeGreaterThan(0);
      expect(receivableId, 'supporting compensation receivable').toBeGreaterThan(0);
      return { liabilityId: liabilityId as number, receivableId: receivableId as number };
    });

    const liability = await test.step('Read: GET customer-liability/{id}', async () => {
      const detail = await getLiability(Request, invoiced.liabilityId);
      Responses.customerLiability.push(detail);
      expect(detail.creationType).toBe('AUTOMATIC');
      expect(detail.status).toBe('ACTIVE');
      expect(nestedId(detail.customerResponse)).toBe(chain.recipientId);
      expect(nestedId(detail.invoiceResponse)).toBe(volume.creditNoteInvoiceId);
      expect(money(detail.initialAmount)).toBe(money(compensationAmount));
      return detail;
    });

    const listed = await listLiabilitiesForCustomer(Request, chain.recipientIdentifier);
    expect(
      listed.some((row) => row.id === invoiced.liabilityId),
      'recipient listing contains government liability',
    ).toBe(true);

    const receivable = await getReceivable(Request, invoiced.receivableId);
    expect(nestedId(receivable.customerResponse)).toBe(chain.billedCustomerId);
    expect(money(receivable.initialAmount)).toBe(money(compensationAmount));

    TestRunSummary.recordCheck({
      check: 'TC-BE-2 STANDARD CREDIT_NOTE creates government liability 95.00',
      expectedResult:
        'INVOICED compensation with AUTOMATIC recipient liability initialAmount 95.00 on the Credit Note',
      actualResult: `liability ${liability.id} amount ${money(liability.initialAmount)} customer ${nestedId(liability.customerResponse)}`,
      passed: true,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3470_KEY,
        relevantEntityKeys: pdt3470RelevantKeys(),
        extraLinks: pdt3470PortalExtraLinks({
          compensationId: compensation.id,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
        }),
        snapshot: {
          recipientId: chain.recipientId,
          liabilityId: invoiced.liabilityId,
          receivableId: invoiced.receivableId,
        },
      });
    });
  });

  test(titleFor('TC-BE-3 unrelated POD compensation stays unlinked'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3470_TEST_TIMEOUT_MS);
    const fx: Pdt3470Fx = { Request, GeneratePayload, Responses, Endpoints };
    const compensationAmount = 70;
    const number = uniqueCompensationNumber('TCBE3');

    const chain = await buildPdt3470IapRealChain(fx, { includeUnrelatedPod: true });
    expect(chain.unrelatedPodId, 'unrelated POD id').toBeGreaterThan(0);

    const compensation = await test.step(
      'Precondition: compensation 70.00 on unrelated POD',
      async () =>
        createPdt3470Compensation(fx, {
          customerId: chain.billedCustomerId,
          podId: chain.unrelatedPodId as number,
          recipientId: chain.recipientId,
          documentPeriod: chain.documentPeriod,
          documentAmount: compensationAmount,
          volumes: 70,
          price: 1,
          number,
          reason: 'PDT-3470-TC-BE-3',
        }),
    );

    const volume = await startPdt3470VolumeCreditNoteDraft(fx, chain, compensationAmount);

    await test.step('Read: compensation still unlinked before PDF', async () => {
      const before = await getCompensation(Request, compensation.id);
      expect(before.index == null).toBe(true);
      expect(isNullishEntityRef(before.invoice)).toBe(true);
    });

    await test.step('Action: start-generating', async () => {
      await generatePdt3470VolumeRun(fx, volume.volumeBillingRunId);
    });

    await test.step('Read: unrelated compensation is not on the Credit Note', async () => {
      const afterPdf = await getCompensation(Request, compensation.id);
      expect(afterPdf.index == null, 'unrelated POD compensation index stays null').toBe(true);
      expect(isNullishEntityRef(afterPdf.invoice)).toBe(true);
      expect(afterPdf.compensationStatus).toBe('UNINVOICED');
      const invoice = await getInvoice(Request, volume.creditNoteInvoiceId);
      const compIds = Array.isArray(invoice.compensations)
        ? invoice.compensations.map((row) => nestedId(row)).filter((id): id is number => id != null)
        : [];
      expect(compIds, 'invoice.compensations does not include unrelated compensation').not.toContain(
        compensation.id,
      );
    });

    await test.step('Action: start-accounting', async () => {
      await accountPdt3470VolumeRun(fx, volume.volumeBillingRunId, volume.creditNoteInvoiceId);
    });

    await test.step('Assert: no compensation receivable 70.00 on this Credit Note', async () => {
      const afterAcc = await getCompensation(Request, compensation.id);
      expect(afterAcc.compensationStatus).toBe('UNINVOICED');
      expect(afterAcc.index == null).toBe(true);
      expect(isNullishEntityRef(afterAcc.invoice)).toBe(true);
      expect(isNullishEntityRef(afterAcc.receivableForCustomer)).toBe(true);
      expect(isNullishEntityRef(afterAcc.liabilityForRecipient)).toBe(true);

      const onInvoice = await receivablesOnCreditNote(
        Request,
        chain.billedCustomerIdentifier,
        volume.creditNoteInvoiceId,
      );
      const unexpected = onInvoice.filter(
        (row) => row.creationType === 'AUTOMATIC' && money(row.initialAmount) === money(compensationAmount),
      );
      expect(
        unexpected.length,
        'no AUTOMATIC receivable of 70.00 on the Credit Note',
      ).toBe(0);
    });

    TestRunSummary.recordCheck({
      check: 'TC-BE-3 unrelated POD compensation does not create L/R',
      expectedResult: 'UNINVOICED, index null, no receivable 70.00 on the Credit Note',
      actualResult: `compensation ${compensation.id} remains unlinked after Credit Note ${volume.creditNoteInvoiceId} accounting`,
      passed: true,
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3470_KEY,
        relevantEntityKeys: pdt3470RelevantKeys(),
        extraLinks: pdt3470PortalExtraLinks({
          compensationId: compensation.id,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
        }),
        snapshot: {
          unrelatedPodId: chain.unrelatedPodId,
          compensationId: compensation.id,
          creditNoteInvoiceId: volume.creditNoteInvoiceId,
        },
      });
    });
  });

  test(titleFor('TC-BE-4 POST documentAmount 0 is rejected'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(10 * 60 * 1000);
    const fx: Pdt3470Fx = { Request, GeneratePayload, Responses, Endpoints };
    const number = uniqueCompensationNumber('TCBE4');

    const billed = await test.step('Precondition: billed legal customer', async () =>
      postExpParityLegalCustomer(fx));
    const recipient = await test.step('Precondition: government recipient', async () =>
      postExpParityLegalCustomer(fx, billed.addressIds));
    expect(recipient.id).not.toBe(billed.id);

    await test.step('Precondition: POD', async () => {
      const payload = GeneratePayload.pointsOfDelivery.pod_settlement() as Record<string, unknown>;
      applyExpParityLocalAddress(payload, billed.addressIds);
      payload.identifier = (`32X${randomGens.generateUniqueIdentifier()}Z`).slice(0, 33);
      const res = await Request.post(Endpoints.pod, { data: payload });
      await expect(res).CheckResponse();
      Responses.pod.push(await res.json());
    });
    const podId = asEntityId(Responses.pod[Responses.pod.length - 1]);

    const zeroPayload = {
      number,
      date: '2026-08-15',
      documentPeriod: '2026-08-01',
      volumes: 1,
      price: 0,
      documentAmount: 0,
      reason: 'PDT-3470-TC-BE-4',
      customerId: billed.id,
      podId,
      recipientId: recipient.id,
      documentCurrencyId: envVariables.currency,
    };
    TestRunSummary.registerPayload('compensation', zeroPayload);

    const rejected = await test.step('Action: POST government-compensations documentAmount 0', async () =>
      postZeroAmountCompensationExpect400(fx, zeroPayload));

    expect(rejected.status, 'zero documentAmount is HTTP 400 (not 401/403/404)').toBe(400);
    expect(rejected.haystack).toContain(PDT_3470_ZERO_AMOUNT_MESSAGE);

    const listing = await listCompensationsByNumber(Request, number);
    expect(listing.length, `no persisted row for ${number}`).toBe(0);

    const receivables = await listReceivablesForCustomer(Request, billed.identifier);
    for (const row of receivables) {
      const detail = await getReceivable(Request, row.id);
      expect(
        !(detail.creationType === 'AUTOMATIC' && money(detail.initialAmount) === 0),
        'no AUTOMATIC receivable of amount 0 for this customer',
      ).toBe(true);
    }

    const liabilities = await listLiabilitiesForCustomer(Request, recipient.identifier);
    for (const row of liabilities) {
      const detail = await getLiability(Request, row.id);
      expect(
        !(detail.creationType === 'AUTOMATIC' && money(detail.initialAmount) === 0),
        'no AUTOMATIC government liability of amount 0 from this POST',
      ).toBe(true);
    }

    TestRunSummary.recordCheck({
      check: 'TC-BE-4 zero documentAmount is rejected',
      expectedResult: `HTTP 400 containing ${PDT_3470_ZERO_AMOUNT_MESSAGE}`,
      actualResult: `HTTP ${rejected.status}; listing rows=${listing.length}`,
      passed: rejected.status === 400 && rejected.haystack.includes(PDT_3470_ZERO_AMOUNT_MESSAGE),
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3470_KEY,
        relevantEntityKeys: ['customer', 'pod', 'compensation'],
        snapshot: { status: rejected.status, number },
      });
    });
  });
});
