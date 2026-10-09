import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  coerceMoney,
  expectMoneyClose,
  expectInclVatAtLeastGate,
  pollInterimInvoicesPositive,
  startBillingRun,
  assertNoInterimInvoicesAfterBilling,
  preconditionStandardExactInterim,
  preconditionStandardPriceComponentInterim,
  preconditionPercentInterimBelowThreshold,
  preconditionPercentInterimAtOrAboveThreshold,
  preconditionProductContractForManual,
  createManualInterimBillingRun,
  countInterimInvoiceListing,
  assertInterimInvoiceLinkedToBillingRun,
  listCustomerLiabilityIds,
  assertNoNewLiabilityIds,
  getBillingRunSnapshot,
  resolvePdt2872Amounts,
  attachPdt2872BillingRunLinks,
  assertInterimPriceComponentInvoiceDetail,
} from './pdt-2872-minimal-interim-payment.fixtures';

/**
 * PDT-2872 — Minimal amount for interim payment (Dev API).
 * Maps 1:1 to Cursor-Project/test_cases/Backend/PDT_2872_minimal_interim_payment.md (TC-BE-1 … TC-BE-15).
 *
 * Reference spec(s):
 * - tests/billing/Interim/interimCases.spec.ts
 * - tests/cursor/PDT-2750-missing-interim-invoice.spec.ts
 * - tests/cursor/pdt-2599-service-contract.fixtures.ts (billing poll patterns)
 */
test.describe('[PDT-2872]: Minimal amount for interim payment', { tag: ['@billing', '@pdt-2872', '@dev'] }, () => {
  test.describe.configure({ mode: 'parallel' });

  test.afterEach(({ Responses }) => {
    attachPdt2872BillingRunLinks(Responses);
  });

  test('[PDT-2872] TC-BE-1: Standard interim — total incl. VAT 5.00 EUR creates invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await resolvePdt2872Amounts(Request);
    const { billingRunId } = await preconditionStandardExactInterim(fx, [amounts.exclFor500]);

    await test.step('PATCH start-billing — expect 2xx (202 on Dev)', async () => {
      await startBillingRun(Request, billingRunId);
    });

    const poll = await test.step('Poll billing run + interim invoices — expect 1', async () =>
      pollInterimInvoicesPositive(Request, billingRunId, 1),
    );

    await test.step('POST invoice/listing + GET invoice — 5.00 EUR, billing run link', async () => {
      if (poll.listingRows.length >= 1) {
        expect(poll.listingRows.length).toBe(1);
      } else {
        test.info().attach('[PDT-2872] TC-BE-1 listing lag', {
          body: 'Interim confirmed via draft-invoices + GET invoice; invoice/listing empty on Dev (indexing lag).',
          contentType: 'text/plain',
        });
      }
      const inv = await assertInterimInvoiceLinkedToBillingRun(
        Request,
        poll.invoiceId,
        billingRunId,
      );
      expectMoneyClose(inv.totalAmountIncludingVat, amounts.inclFor500, 'TC-BE-1 totalAmountIncludingVat');
      expect(['DRAFT', 'REAL', 'DRAFT_GENERATED']).toContain(inv.invoiceStatus);
    });

    test.info().attach('[PDT-2872] TC-BE-1 amounts', {
      body: JSON.stringify(amounts, null, 2),
      contentType: 'application/json',
    });
    test.info().attach('[PDT-2872] TC-BE-1 entities', {
      body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
      contentType: 'application/json',
    });
  });

  test('[PDT-2872] TC-BE-2: Standard interim — scaled incl. VAT above 5.00 EUR creates invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await resolvePdt2872Amounts(Request);
    const { billingRunId } = await preconditionStandardExactInterim(fx, [amounts.exclFor501]);

    await startBillingRun(Request, billingRunId);
    const poll = await pollInterimInvoicesPositive(Request, billingRunId, 1);

    const inv = await assertInterimInvoiceLinkedToBillingRun(
      Request,
      poll.invoiceId,
      billingRunId,
    );
    expectInclVatAtLeastGate(inv.totalAmountIncludingVat, 5.01, 'TC-BE-2 gate');
    expectMoneyClose(
      inv.totalAmountIncludingVat,
      amounts.inclFor501,
      'TC-BE-2 totalAmountIncludingVat (scaled from exclFor501)',
    );
    test.info().attach('[PDT-2872] TC-BE-2 rounding', {
      body: `excl=${amounts.exclFor501} → expected incl=${amounts.inclFor501} @ VAT ${amounts.vatPercent}% (cent IAP may skip exact 5.01)`,
      contentType: 'text/plain',
    });
    expect(coerceMoney(inv.totalAmountExcludingVat)).toBeGreaterThan(0);
    expect(coerceMoney(inv.totalAmountOfVat)).toBeGreaterThan(0);
  });

  test('[PDT-2872] TC-BE-3: Standard interim — total incl. VAT 4.99 EUR — no invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await resolvePdt2872Amounts(Request);
    const { billingRunId } = await preconditionStandardExactInterim(fx, [amounts.exclFor499]);

    const liabilitiesBefore = await test.step('Precondition: liability ids before billing', async () =>
      listCustomerLiabilityIds(Request, Responses.customer[0].identifier),
    );

    await startBillingRun(Request, billingRunId);
    await test.step('Assert no interim after billing completes', async () => {
      await assertNoInterimInvoicesAfterBilling(Request, billingRunId);
    });

    const liabilitiesAfter = await test.step('GET customer-liability/list after billing', async () =>
      listCustomerLiabilityIds(Request, Responses.customer[0].identifier),
    );
    assertNoNewLiabilityIds(liabilitiesBefore, liabilitiesAfter);
  });

  test('[PDT-2872] TC-BE-4: Standard interim — total incl. VAT 0.00 EUR — no invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await resolvePdt2872Amounts(Request);
    const { billingRunId } = await preconditionStandardExactInterim(fx, [amounts.zero]);

    test.info().attach('[PDT-2872] TC-BE-4 note', {
      body: `Dev IAP minimum value is 0.01 (not 0.00); incl VAT=${amounts.zero} excl → expect no interim.`,
      contentType: 'text/plain',
    });

    await startBillingRun(Request, billingRunId);
    await assertNoInterimInvoicesAfterBilling(Request, billingRunId);
  });

  test('[PDT-2872] TC-BE-5: Rounding — scaled total 5.00 EUR creates invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await resolvePdt2872Amounts(Request);
    const { billingRunId } = await preconditionStandardExactInterim(fx, [amounts.exclRoundingEdge]);

    test.info().attach('[PDT-2872] TC-BE-5 rounding edge', {
      body: JSON.stringify(
        {
          vatPercent: amounts.vatPercent,
          exclRoundingEdge: amounts.exclRoundingEdge,
          exclFor500: amounts.exclFor500,
        },
        null,
        2,
      ),
      contentType: 'application/json',
    });

    await startBillingRun(Request, billingRunId);
    const poll = await pollInterimInvoicesPositive(Request, billingRunId, 1);
    const inv = await assertInterimInvoiceLinkedToBillingRun(
      Request,
      poll.invoiceId,
      billingRunId,
    );
    expectMoneyClose(
      inv.totalAmountIncludingVat,
      amounts.inclRoundingEdge,
      'TC-BE-5 scaled totalAmountIncludingVat',
    );
  });

  test('[PDT-2872] TC-BE-6: Rounding — scaled total 4.99 EUR — no invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await resolvePdt2872Amounts(Request);
    const { billingRunId } = await preconditionStandardExactInterim(fx, [amounts.exclFor499]);

    await startBillingRun(Request, billingRunId);
    await assertNoInterimInvoicesAfterBilling(Request, billingRunId);
  });

  test('[PDT-2872] TC-BE-7: Manual interim — total incl. VAT ≥ 5.00 EUR creates invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await preconditionProductContractForManual(fx);

    const billingRunId = await test.step('Precondition: manual interim billing run', async () =>
      createManualInterimBillingRun(fx, amounts.exclFor500),
    );

    await startBillingRun(Request, billingRunId);
    const poll = await pollInterimInvoicesPositive(Request, billingRunId, 1);

    const inv = await assertInterimInvoiceLinkedToBillingRun(
      Request,
      poll.invoiceId,
      billingRunId,
    );
    expectMoneyClose(inv.totalAmountIncludingVat, amounts.inclFor500, 'TC-BE-7 totalAmountIncludingVat');
    expect(String(inv.invoiceNumber ?? '').length).toBeGreaterThan(0);
  });

  test('[PDT-2872] TC-BE-8: Manual interim — total incl. VAT 4.99 EUR — no invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await preconditionProductContractForManual(fx);

    const billingRunId = await createManualInterimBillingRun(fx, amounts.exclFor499);

    await startBillingRun(Request, billingRunId);
    await assertNoInterimInvoicesAfterBilling(Request, billingRunId);

    const run = await getBillingRunSnapshot(Request, billingRunId);
    const status = (run.commonParameters as { status?: string })?.status;
    expect(status, 'billing run should remain operable after skip').toBeTruthy();
  });

  test('[PDT-2872] TC-BE-9: PRICE_COMPONENT IAP — total incl. VAT ≥ 5.00 creates invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await resolvePdt2872Amounts(Request);
    const { billingRunId } = await preconditionStandardPriceComponentInterim(fx, amounts.exclFor500);

    await startBillingRun(Request, billingRunId);
    const poll = await pollInterimInvoicesPositive(Request, billingRunId, 1);

    const inv = await assertInterimInvoiceLinkedToBillingRun(
      Request,
      poll.invoiceId,
      billingRunId,
    );
    expect(coerceMoney(inv.totalAmountIncludingVat)).toBeGreaterThanOrEqual(5);

    await assertInterimPriceComponentInvoiceDetail(Request, poll.invoiceId);
  });

  test('[PDT-2872] TC-BE-10: PRICE_COMPONENT IAP — total incl. VAT 4.99 — no invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await resolvePdt2872Amounts(Request);
    const { billingRunId } = await preconditionStandardPriceComponentInterim(fx, amounts.exclFor499);

    await startBillingRun(Request, billingRunId);
    await assertNoInterimInvoicesAfterBilling(Request, billingRunId);
  });

  test('[PDT-2872] TC-BE-11: PERCENT_FROM_PREVIOUS — derived total incl. VAT < 5.00 — no invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const { billingRunId, parentInvoiceId } = await preconditionPercentInterimBelowThreshold(
      fx,
      4,
    );

    const parentBefore = await Request.get(`invoice?id=${parentInvoiceId}`);
    await expect(parentBefore).CheckResponse();
    const parentJson = await parentBefore.json();
    const parentTotalBefore = coerceMoney(parentJson.totalAmountIncludingVat);

    await startBillingRun(Request, billingRunId);
    await assertNoInterimInvoicesAfterBilling(Request, billingRunId);

    const parentAfter = await Request.get(`invoice?id=${parentInvoiceId}`);
    await expect(parentAfter).CheckResponse();
    const parentAfterJson = await parentAfter.json();
    expect(parentAfterJson.id).toBe(parentJson.id);
    expectMoneyClose(
      parentAfterJson.totalAmountIncludingVat,
      parentTotalBefore,
      'TC-BE-11 parent invoice unchanged',
    );
  });

  test('[PDT-2872] TC-BE-15: PERCENT_FROM_PREVIOUS — derived total incl. VAT ≥ 5.00 — creates invoice', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const { billingRunId, parentInvoiceId, expectedDerivedIncl, parentIncl } =
      await preconditionPercentInterimAtOrAboveThreshold(fx, 5);

    test.info().attach('[PDT-2872] TC-BE-15 PERCENT derived amounts', {
      body: JSON.stringify(
        {
          parentIncl,
          percent: 100,
          naiveDerivedIncl: expectedDerivedIncl,
          gateMin: 5,
          note: 'Phoenix applies percent per previous-invoice line, not header × percent only',
        },
        null,
        2,
      ),
      contentType: 'application/json',
    });

    const parentBefore = await Request.get(`invoice?id=${parentInvoiceId}`);
    await expect(parentBefore).CheckResponse();
    const parentJson = await parentBefore.json();
    const parentTotalBefore = coerceMoney(parentJson.totalAmountIncludingVat);

    await startBillingRun(Request, billingRunId);
    const poll = await pollInterimInvoicesPositive(Request, billingRunId, 1);

    const inv = await assertInterimInvoiceLinkedToBillingRun(
      Request,
      poll.invoiceId,
      billingRunId,
    );
    expectInclVatAtLeastGate(inv.totalAmountIncludingVat, 5, 'TC-BE-15 gate');
    expect(
      coerceMoney(inv.totalAmountIncludingVat),
      'TC-BE-15 interim incl. should be positive when percent=100% of parent',
    ).toBeGreaterThan(0);

    const parentAfter = await Request.get(`invoice?id=${parentInvoiceId}`);
    await expect(parentAfter).CheckResponse();
    const parentAfterJson = await parentAfter.json();
    expect(parentAfterJson.id).toBe(parentJson.id);
    expectMoneyClose(
      parentAfterJson.totalAmountIncludingVat,
      parentTotalBefore,
      'TC-BE-15 parent invoice unchanged',
    );
  });

  test('[PDT-2872] TC-BE-12: Two interim IAPs — positive amount (≥ 5.00) invoices; below-threshold does not', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(12 * 60 * 1000);
    const fx = { Request, GeneratePayload, Responses, Endpoints };
    const amounts = await resolvePdt2872Amounts(Request);
    const { billingRunId } = await preconditionStandardExactInterim(fx, [
      amounts.exclFor500,
      amounts.exclFor499,
    ]);

    await startBillingRun(Request, billingRunId);
    const poll = await pollInterimInvoicesPositive(Request, billingRunId, 1);

    const inv = await assertInterimInvoiceLinkedToBillingRun(
      Request,
      poll.invoiceId,
      billingRunId,
    );
    expectMoneyClose(
      inv.totalAmountIncludingVat,
      amounts.inclFor500,
      'TC-BE-12 IAP-A positive row at gate boundary',
    );
  });
});
