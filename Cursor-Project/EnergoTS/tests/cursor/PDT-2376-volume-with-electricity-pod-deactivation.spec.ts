import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import { envVariables } from '../../fixtures/envCashed';
import {
  alignPdt2376BillingByProfilePayloadProfileId,
  applyPdt2376PodActivationMatrix,
  buildPdt2376ElectricityMatchContext,
  buildPdt2376PodMatrix,
  fetchAggregatedInvoiceDetailedData,
  getMatchedElectricityRowsForPodCandidates,
  hydratePdt2376ElectricityMatchContextIfBareId,
  listInvoiceIdsForBillingRun,
  normalizeBillingRunId,
  PDT_2376_BILLING_BY_PROFILE_POD_INDEX,
  PDT_2376_ELECTRICITY_MATCH_ROW_CAP,
  resolveAttachmentPeriodBounds,
  resolvePdt2376BillingAnchor,
  resolvePdt2376ElectricityPriceComponentId,
  resolvePdt2376SharedProfileId,
  summarizeMatchedRowsBrief,
  type Pdt2376PodElectricityDetailedAttachmentEntry,
  type Pdt2376PodMatrixRow,
} from './pdt-2376-volume-with-electricity.fixtures';

const POD_COUNT = 8;

function attachPodMatrixVerification(title: string, payload: unknown): void {
  test.info().attach(title, {
    body: JSON.stringify(payload, null, 2),
    contentType: 'application/json',
  });
}

test.describe('[PDT-2376]: Volume + WITH_ELECTRICITY — multi-POD matrix, billing-by-profile on POD index 0 only', { tag: ['@billing', '@pdt-2376'] }, () => {
  test.describe.configure({ mode: 'serial' });

  test('[PDT-2376]: Invoices - The system generates a tax (With electricity invoice) for deactivated POD', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
  }) => {
    test.setTimeout(35 * 60 * 1000);

    let billingAnchor: Awaited<ReturnType<typeof resolvePdt2376BillingAnchor>>;
    let matrix: Pdt2376PodMatrixRow[] = [];
    /** Same profile row as volume settlement `profiles[0].profileId` — overrides generator default `envVariables.profiles` on billing-by-profile. */
    let pdt2376SharedProfileId: number;

    await test.step('Precondition: billing anchor (preferred 2025-12-31 vs OPEN accounting period)', async () => {
      billingAnchor = await resolvePdt2376BillingAnchor(Request);
      matrix = buildPdt2376PodMatrix(billingAnchor.invoicePeriodTo);
      attachPodMatrixVerification('[PDT-2376] billing anchor + matrix (dates)', {
        anchor: billingAnchor,
        matrix,
      });
    });

    await test.step('Precondition: customer', async () => {
      const customer = await Request.post(Endpoints.customer, {
        data: GeneratePayload.customers.customer_legal(),
      });
      await expect(customer).CheckResponse();
      Responses.customer.push(await customer.json());
    });

    await test.step('Precondition: price component (volume)', async () => {
      pdt2376SharedProfileId = await resolvePdt2376SharedProfileId({ Nomenclatures });
      const payload = GeneratePayload.productAndServices.priceSettlement();
      payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = pdt2376SharedProfileId;
      const price = await Request.post(Endpoints.priceComponent, { data: payload });
      await expect(price).CheckResponse();
      Responses.priceComponent.push(await price.json());
    });

    await test.step('Precondition: price component (electricity / WITH_ELECTRICITY_INVOICE)', async () => {
      const priceElectricity = await Request.post(Endpoints.priceComponent, {
        data: GeneratePayload.productAndServices.electricity(),
      });
      await expect(priceElectricity).CheckResponse();
      Responses.priceComponent.push(await priceElectricity.json());
    });

    await test.step('Precondition: terms', async () => {
      const term = await Request.post(Endpoints.terms, {
        data: GeneratePayload.productAndServices.term(),
      });
      await expect(term).CheckResponse();
      Responses.terms.push(await term.json());
    });

    await test.step(`Precondition: create ${POD_COUNT} settlement PODs`, async () => {
      for (let i = 0; i < POD_COUNT; i++) {
        const podSettlement = await Request.post(Endpoints.pod, {
          data: GeneratePayload.pointsOfDelivery.pod_settlement(),
        });
        await expect(podSettlement).CheckResponse();
        Responses.pod.push(await podSettlement.json());
      }
    });

    await test.step('Precondition: product (volume + electricity PCs)', async () => {
      const product = await Request.post(Endpoints.product, {
        data: GeneratePayload.productAndServices.product(),
      });
      await expect(product).CheckResponse();
      Responses.product.push(await product.json());
    });

    await test.step('Precondition: product-contract (single contract, all PODs)', async () => {
      const contract = await Request.post(
        Endpoints.productContract,
        { data: await GeneratePayload.contractsAndOrders.product_contract() },
      );
      await expect(contract).CheckResponse();
      Responses.productContract.push(await contract.json());
    });

    await test.step('Precondition: activate each contract-POD row from matrix (manual contract-pods)', async () => {
      const contractId = Responses.productContract[0].id;
      await applyPdt2376PodActivationMatrix({
        Request,
        contractId,
        pods: Responses.pod.map((p) => ({ id: p.id })),
        matrix,
        deactivationPurposeId: envVariables.deactivation_reason,
      });
    });

    await test.step(
      `Precondition: billing-by-profile monthly energy data for POD index ${PDT_2376_BILLING_BY_PROFILE_POD_INDEX} only (aligned to anchor window)`,
      async () => {
        const pi = PDT_2376_BILLING_BY_PROFILE_POD_INDEX;
        const payload = await GeneratePayload.energyData.profile1Month(pi, [
          {
            startDate: billingAnchor.profileStart,
            endDate: billingAnchor.profileEnd,
          },
        ]);
        alignPdt2376BillingByProfilePayloadProfileId(payload, pdt2376SharedProfileId);
        payload.timeZone = 'CET';
        const profiles = await Request.post('billing-by-profile', { data: payload });
        await expect(profiles).CheckResponse();
        const profileData = await profiles.json();
        Responses.dataByProfiles.push({
          id: profileData,
          periodFrom: payload.periodFrom,
          periodTo: payload.periodTo,
          periodType: payload.periodType,
        });
      },
    );

    await test.step('Action: billing run FOR_VOLUMES + WITH_ELECTRICITY_INVOICE (contract level)', async () => {
      const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT', [
        'FOR_VOLUMES',
        'WITH_ELECTRICITY_INVOICE',
      ]);
      billingPayload.commonParameters.accountingPeriodId = billingAnchor.accountingPeriodId;
      billingPayload.commonParameters.taxEventDate = billingAnchor.taxEventDate;
      billingPayload.commonParameters.invoiceDate = billingAnchor.invoiceDate;

      const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
      await expect(billingRun).CheckResponse();
      Responses.billingRun.push(await billingRun.json());
    });

    await test.step('Action: run billing pipeline to invoices', async () => {
      await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
    });

    await test.step(
      'Assert: invalid end-date PODs are NOT counted with electricity in invoice/detailed-data',
      async () => {
      const brid = normalizeBillingRunId(Responses.billingRun[Responses.billingRun.length - 1]);
      let invoiceIds = await listInvoiceIdsForBillingRun(Request, brid);

      const needFromResponses = Responses.invoice.filter((id) => Number(id) > 0).map(Number);
      if (needFromResponses.length > 0) {
        invoiceIds = needFromResponses;
      }

      expect.soft(invoiceIds.length > 0).toBeTruthy();

      let electricityCtx = buildPdt2376ElectricityMatchContext(Responses.priceComponent);
      electricityCtx = await hydratePdt2376ElectricityMatchContextIfBareId(Request, electricityCtx);
      const aggregatedDetailedRows = await fetchAggregatedInvoiceDetailedData(Request, invoiceIds);

      attachPodMatrixVerification('[PDT-2376] detailed-data aggregation summary', {
        invoiceIdsScanned: invoiceIds,
        electricityPriceComponentId: electricityCtx.electricityPriceComponentId ?? null,
        electricityLabelCandidates: [...electricityCtx.labelCandidatesFromCreationResponse],
        totalDetailedRows: aggregatedDetailedRows.length,
      });

      const rowsOut: Record<string, unknown>[] = [];
      const perPodElectricityAttachment: Pdt2376PodElectricityDetailedAttachmentEntry[] = [];

      const contractId = Responses.productContract[0].id;
      const cg = await Request.get(`product-contract/${contractId}?version=1`);
      await expect(cg).CheckResponse();
      const cj = (await cg.json()) as {
        contractPodsResponses?: Array<{ podId?: number; identifier?: string | number }>;
      };

      const fallbackFrom = billingAnchor.profileStart;
      const fallbackTo = billingAnchor.invoicePeriodTo;
      const electricityPcIdForMessage = resolvePdt2376ElectricityPriceComponentId(Responses.priceComponent);

      for (let i = 0; i < POD_COUNT; i++) {
        const podId = Responses.pod[i]?.id;
        const cpRow = (cj.contractPodsResponses ?? []).find((p) => Number(p.podId) === Number(podId));
        const identifier = cpRow?.identifier != null ? String(cpRow.identifier) : String(podId);
        const podLabel = `${identifier}`;
        const spec = matrix[i];

        // User-focused check: only PODs with invalid end-date month must be excluded from electricity rows.
        const isInvalidEndDatePod = spec.deactivationDate != null && !spec.expectedEligible;
        const shouldHaveProfileElectricity = i === PDT_2376_BILLING_BY_PROFILE_POD_INDEX;
        const expectedWithElectricityLine = shouldHaveProfileElectricity ? spec.expectedEligible : false;

        const matchedElectricityRows = getMatchedElectricityRowsForPodCandidates(
          aggregatedDetailedRows,
          [podLabel, String(podId)],
          electricityCtx,
        );

        /** Per-POD verdict from consolidated `invoice/detailed-data`: any electricity row for this POD. */
        const actualWithElectricityLine = matchedElectricityRows.length > 0;

        const { periodFrom: attFrom, periodTo: attTo } = resolveAttachmentPeriodBounds(
          matchedElectricityRows,
          fallbackFrom,
          fallbackTo,
        );

        perPodElectricityAttachment.push({
          podIdentifier: podLabel,
          expected: expectedWithElectricityLine,
          actual: actualWithElectricityLine,
          matchedRows: summarizeMatchedRowsBrief(matchedElectricityRows),
          matchedRowsTruncated: matchedElectricityRows.length > PDT_2376_ELECTRICITY_MATCH_ROW_CAP,
          periodFrom: attFrom,
          periodTo: attTo,
        });

        rowsOut.push({
          label: spec.label,
          identifier: podLabel,
          activationDate: spec.activationDate,
          deactivationDate: spec.deactivationDate,
          invoicePeriodToUsed: billingAnchor.invoicePeriodTo,
          accountingPeriodAdaptationComment: billingAnchor.commentary,
          billingByProfilePosted: shouldHaveProfileElectricity,
          calendarEligibleIfProfilePresent: spec.expectedEligible,
          isInvalidEndDatePod,
          expectedWithElectricityLine,
          actualWithElectricityLineFromDetailedData: actualWithElectricityLine,
        });

        if (!isInvalidEndDatePod) {
          continue;
        }

        expect(
          actualWithElectricityLine,
          `[${spec.label}] Invalid end-date POD must NOT have electricity price component in invoice detailed-data for POD ${podLabel}` +
            ` (billing-by-profile on this POD: ${shouldHaveProfileElectricity}; electricityPcId=${electricityPcIdForMessage ?? 'n/a'};` +
            ` labelCandidates=${JSON.stringify(electricityCtx.labelCandidatesFromCreationResponse)})`,
        ).toBe(false);
      }

      attachPodMatrixVerification('[PDT-2376] per-POD detailed-data electricity verdicts', perPodElectricityAttachment);

      attachPodMatrixVerification('[PDT-2376] per-POD verdict matrix (summary)', {
        invoicesScanned: invoiceIds,
        rows: rowsOut,
      });

      attachPodMatrixVerification('[PDT-2376] created entities', {
        links: reportGenerator.setLinksToResponses(Responses),
      });
    });
  });
});
