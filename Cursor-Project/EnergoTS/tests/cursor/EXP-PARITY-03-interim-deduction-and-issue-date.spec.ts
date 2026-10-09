/**
 * EXP-PARITY-03 — MATCH_THE_INVOICE_DATE interim + exactly-once deduction.
 *
 * Case 13: MATCH_THE_INVOICE_DATE IAP on one combined STANDARD_BILLING run
 * (`FOR_VOLUMES` + `INTERIM_AND_ADVANCE_PAYMENT`) with `maxEndDate`.
 * Case 12: REAL a PERIODICAL interim first (no maxEndDate), then one FOR_VOLUMES
 * run that produces two volume invoices (maxEndDate set).
 *
 * NAMING: `EXP-PARITY-03` is a PSEUDO-KEY placeholder. No Jira ticket exists yet;
 * rename files, constants, and titles once a real key is assigned.
 *
 * Run twice (PreProd, Experiment) with only `BASE_URL` changed; diff parity snapshots.
 * Never branch on environment or URL.
 *
 * Reference spec(s):
 * - tests/billing/Interim/interimCases.spec.ts (~line 287 combined model)
 * - tests/billing/forVolumes/deduction.spec.ts (REG-718)
 * - tests/cursor/EXP-PARITY-01-pulling-shared-pod-handover.spec.ts
 * - tests/cursor/SLR-18167-dev2-volumes-billing-replica.spec.ts + cursor-test.fixtures.ts
 */

import { expect, finalizeTestRunSummary, test } from './cursor-test.fixtures';
import { attachParitySnapshot } from './shared/exp-parity-snapshot.fixtures';
import {
  EXP_PARITY_03_CASE_12_ID,
  EXP_PARITY_03_CASE_12_TITLE,
  EXP_PARITY_03_CASE_13_ID,
  EXP_PARITY_03_CASE_13_TITLE,
  EXP_PARITY_03_DEDUCTION_TEST_TIMEOUT_MS,
  EXP_PARITY_03_INTERIM_INVOICE_TYPE,
  EXP_PARITY_03_KEY,
  EXP_PARITY_03_POD_COUNT,
  EXP_PARITY_03_TEST_TIMEOUT_MS,
  buildExpParity03SinglePodPrechain,
  buildInterimRealThenTwoPodVolumesChain,
  createStandardBillingRun,
  observeInterimDeductionAcrossVolumes,
  resolveLastVolumeInvoiceIds,
  startBillingAndObserveDraft,
  type ExpParity03Fx,
} from './exp-parity-03-interim-deduction-and-issue-date.fixtures';

function observationSnapshotFacts(
  observation: Awaited<ReturnType<typeof startBillingAndObserveDraft>>,
): Record<string, unknown> {
  return {
    applicationModelTypes: observation.applicationModelTypes,
    billingRunStatus: observation.billingRunStatus,
    draftInvoiceCount: observation.draftInvoiceIds.length,
    interimDraftCount: observation.interimDraftCount,
    volumeDraftCount: observation.volumeDraftCount,
    errorRowCount: observation.errorRows.length,
    errorShapes: observation.errorShapes,
    errorMessages: observation.errorRows.map((row) => row.errorMessage).filter(Boolean),
  };
}

test.describe(`[${EXP_PARITY_03_KEY}]: Interim parity — issue date and deduction`, {
  tag: '@billing',
}, () => {
  test(`[${EXP_PARITY_03_KEY}]: ${EXP_PARITY_03_CASE_13_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(EXP_PARITY_03_TEST_TIMEOUT_MS);
    const fx: ExpParity03Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await buildExpParity03SinglePodPrechain(fx);

    TestRunSummary.registerPayload('iap', {
      valueType: 'EXACT_AMOUNT',
      dateOfIssueType: 'MATCH_THE_INVOICE_DATE',
      deductionFrom: 'FIRST_INVOICE_FOR_SAME_PERIOD',
      matchesWithTermOfStandardInvoice: true,
    });

    const combinedModels = ['FOR_VOLUMES', 'INTERIM_AND_ADVANCE_PAYMENT'];
    const observation = await test.step(
      'Action: STANDARD_BILLING with FOR_VOLUMES + INTERIM_AND_ADVANCE_PAYMENT and maxEndDate',
      async () => {
        const billingRunId = await createStandardBillingRun(fx, combinedModels, chain.anchor);
        return startBillingAndObserveDraft(Request, billingRunId, combinedModels);
      },
    );

    await test.step('Assert: combined run produces MATCH_THE_INVOICE_DATE interim draft(s)', async () => {
      expect(
        observation.interimDraftCount,
        'combined FOR_VOLUMES + INTERIM run must produce at least one interim draft',
      ).toBeGreaterThanOrEqual(1);
      expect(observation.billingRunStatus, 'combined run must reach DRAFT').toBe('DRAFT');
    });

    TestRunSummary.recordCheck({
      check: 'Combined volumes+interim billing produces INTERIM_AND_ADVANCE_PAYMENT draft(s)',
      expectedResult: 'billingRunStatus=DRAFT; interimDraftCount >= 1; maxEndDate present on create',
      actualResult:
        `status=${observation.billingRunStatus}; interimDrafts=${observation.interimDraftCount}; ` +
        `volumeDrafts=${observation.volumeDraftCount}; draftIds=${observation.draftInvoiceIds.join(',')}`,
      passed: observation.interimDraftCount >= 1 && observation.billingRunStatus === 'DRAFT',
    });

    const snapshot = attachParitySnapshot({
      key: EXP_PARITY_03_KEY,
      caseId: `${EXP_PARITY_03_CASE_13_ID}-combined-volumes-interim`,
      description:
        'IAP MATCH_THE_INVOICE_DATE with applicationModelType=[FOR_VOLUMES, INTERIM_AND_ADVANCE_PAYMENT] and maxEndDate',
      facts: {
        run: 'combined-volumes-interim',
        ...observationSnapshotFacts(observation),
      },
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: EXP_PARITY_03_KEY,
        relevantEntityKeys: [
          'customer',
          'pod',
          'product',
          'productContract',
          'interim',
          'dataByProfiles',
          'billingRun',
        ],
        snapshot: {
          case13: snapshot.facts,
        },
      });
    });
  });

  test(`[${EXP_PARITY_03_KEY}]: ${EXP_PARITY_03_CASE_12_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(EXP_PARITY_03_DEDUCTION_TEST_TIMEOUT_MS);
    const fx: ExpParity03Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await buildInterimRealThenTwoPodVolumesChain(fx);

    TestRunSummary.registerPayload('deductionScenario', {
      interimInvoiceId: chain.interimInvoiceId,
      interimBillingRunId: chain.interimBillingRunId,
      volumeBillingRunId: chain.volumeBillingRunId,
      iap: {
        valueType: 'EXACT_AMOUNT',
        dateOfIssueType: 'PERIODICAL',
        deductionFrom: 'FIRST_INVOICE_FOR_SAME_PERIOD',
      },
      separateInvoicePerPodEnabledAfterInterimReal: true,
      podCount: EXP_PARITY_03_POD_COUNT,
      billingAnchor: chain.anchor,
    });

    const volumeInvoiceIds = resolveLastVolumeInvoiceIds(Responses, EXP_PARITY_03_POD_COUNT);

    await test.step(`Assert: ${EXP_PARITY_03_POD_COUNT} REAL volume invoices from separate-invoice-per-POD run`, async () => {
      for (const invoiceId of volumeInvoiceIds) {
        const res = await Request.get(`invoice?id=${invoiceId}`);
        await expect(res).CheckResponse();
        const body = await res.json();
        expect(body.invoiceStatus, `invoice ${invoiceId} must be REAL`).toBe('REAL');
        expect(body.invoiceType, `invoice ${invoiceId} must be STANDARD volume`).toBe('STANDARD');
      }
    });

    const deduction = await test.step('Read: interim deduction facts across volume invoices', async () =>
      observeInterimDeductionAcrossVolumes(Request, chain.interimInvoiceId, volumeInvoiceIds));

    const referenceAmount =
      deduction.interimAmountExclVat ?? deduction.interimAmountInclVat ?? null;

    await test.step(
      'Assert: INTERIM_DEDUCTION total across volume invoices equals interim amount once (not twice)',
      async () => {
        expect(referenceAmount, 'interim reference amount must be readable from GET invoice').not.toBeNull();
        expect(
          deduction.totalDeductedAcrossVolumes,
          'sum of InvoiceDetailedDataResponse.deducted across volume invoices',
        ).toBeGreaterThan(0);
        expect(
          deduction.totalDeductedAcrossVolumes,
          'deduction must not double-charge the same interim across two volume invoices',
        ).toBeLessThanOrEqual((referenceAmount ?? 0) * 1.05);
        expect(
          deduction.totalDeductedAcrossVolumes,
          'deduction must fully apply the interim once',
        ).toBeGreaterThanOrEqual((referenceAmount ?? 0) * 0.95);
        expect(
          deduction.deductingVolumeInvoiceCount,
          'exactly one volume invoice should carry the interim deduction',
        ).toBe(1);
      },
    );

    await test.step('Assert: interim invoice flagged deducted at most once', async () => {
      if (deduction.interimIsDeducted != null) {
        expect(deduction.interimIsDeducted, 'Invoice entity isDeducted (not in Swagger)').toBe(true);
      }
      if (deduction.interimDeductedForInvoiceId != null) {
        expect(
          volumeInvoiceIds.includes(deduction.interimDeductedForInvoiceId),
          'deductedForInvoiceId must reference one of the volume invoices',
        ).toBe(true);
      }
      if (deduction.connectedVolumeIdsFromInterim.length > 0) {
        expect(
          deduction.connectedVolumeIdsFromInterim.length,
          'Connected invoices on interim must not list multiple volume hosts for one interim',
        ).toBeLessThanOrEqual(1);
      }
    });

    TestRunSummary.recordCheck({
      check: 'Single interim deducted exactly once across two separate-POD volume invoices',
      expectedResult:
        `totalDeducted ≈ interim amount once; deductingVolumeInvoiceCount=1; ` +
        `interim REAL id=${chain.interimInvoiceId}`,
      actualResult:
        `interimIncl=${deduction.interimAmountInclVat} interimExcl=${deduction.interimAmountExclVat}; ` +
        `totalDeducted=${deduction.totalDeductedAcrossVolumes}; ` +
        `deductingCount=${deduction.deductingVolumeInvoiceCount}; ` +
        `perInvoice=${JSON.stringify(deduction.perVolumeInvoiceDeduction)}; ` +
        `isDeducted=${deduction.interimIsDeducted}; connected=${deduction.connectedVolumeIdsFromInterim.join(',')}`,
      passed:
        deduction.deductingVolumeInvoiceCount === 1 &&
        referenceAmount != null &&
        deduction.totalDeductedAcrossVolumes >= referenceAmount * 0.95 &&
        deduction.totalDeductedAcrossVolumes <= referenceAmount * 1.05,
    });

    const snapshot = attachParitySnapshot({
      key: EXP_PARITY_03_KEY,
      caseId: EXP_PARITY_03_CASE_12_ID,
      description:
        'One REAL interim, separateInvoiceForEachPod enabled after REAL, two volume invoices — ' +
        'deduction must apply once (InvoiceRepository 862–904 guard is billingId-scoped)',
      facts: {
        interimInvoiceType: EXP_PARITY_03_INTERIM_INVOICE_TYPE,
        interimAmountInclVat: deduction.interimAmountInclVat,
        interimAmountExclVat: deduction.interimAmountExclVat,
        interimIsDeducted: deduction.interimIsDeducted,
        interimDeductedForInvoiceId: deduction.interimDeductedForInvoiceId,
        volumeInvoiceCount: volumeInvoiceIds.length,
        totalDeductedAcrossVolumes: deduction.totalDeductedAcrossVolumes,
        deductingVolumeInvoiceCount: deduction.deductingVolumeInvoiceCount,
        perVolumeInvoiceDeduction: deduction.perVolumeInvoiceDeduction,
        connectedVolumeIdsFromInterim: deduction.connectedVolumeIdsFromInterim,
        separateInvoicePerPodEnabledAfterInterimReal: true,
        volumeBillingRunId: chain.volumeBillingRunId,
        interimBillingRunId: chain.interimBillingRunId,
      },
      allowKeys: ['interimDeductedForInvoiceId'],
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: EXP_PARITY_03_KEY,
        relevantEntityKeys: [
          'customer',
          'pod',
          'product',
          'productContract',
          'interim',
          'dataByProfiles',
          'billingRun',
          'invoice',
        ],
        snapshot: snapshot as unknown as Record<string, unknown>,
      });
    });
  });
});
