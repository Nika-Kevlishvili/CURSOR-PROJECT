/**
 * PDT-2177 — Supply deactivation (Information of deactivation) must surface billing/invoice
 * restricted-period error instead of "Respective Contract version for this pod is not found!".
 *
 * Environment: Dev2
 *   BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 *
 * Lock: STANDARD FOR_VOLUMES invoice; lock date from InvoiceResponse.meterReadingTo
 * (= DB meter_reading_period_to used by getContractLockDate). Not interim/API date guesses.
 *
 * Reference spec(s):
 * - tests/billing/Restriction/restrictionForVolume(Percenage).spec.ts
 * - tests/cursor/pdt-2854-pod-active-two-contracts.fixtures.ts
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProcessPreviewLink,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  assertRestrictedPeriodErrorInReport,
  induceContractLockViaStandardVolumeInvoice,
  resolvePodIdentifier,
  runSupplyActionDeactivationMassImportFlow,
  OLD_MISLEADING_CONTRACT_VERSION_SNIPPET,
  PENALTY_ONCOMPLETE_SNIPPET,
  RESTRICTED_PERIOD_SNIPPET,
} from './pdt-2177-supply-action-deactivation-restricted-period.fixtures';

test.describe('[PDT-2177]: Supply action deactivation restricted period', {
  tag: ['@massImport', '@supplyActivation', '@pdt-2177', '@dev2'],
}, () => {
  test(
    '[PDT-2177]: Supply deactivation - Information for deactivation - error "Respective Contract version for this pod is not found!"',
    async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      TestRunSummary,
    }) => {
      test.setTimeout(30 * 60 * 1000);

      let podIdentifier = '';
      let processId = 0;
      let contractId = 0;
      let deactivationDateIso = '';
      let lockDateIso = '';
      let lockDateField = '';
      let activationDateIso = '';

      await test.step('Precondition: contract + activated POD + STANDARD volume invoice lock', async () => {
        const lock = await induceContractLockViaStandardVolumeInvoice({
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        });
        contractId = lock.contractId;
        deactivationDateIso = lock.deactivationDateIso;
        lockDateIso = lock.lockDateIso;
        lockDateField = lock.lockDateField;
        activationDateIso = lock.activationDateIso;

        expect(lockDateIso, 'invoice meter reading "to" (DB lock) required').toBeTruthy();
        expect(
          activationDateIso <= deactivationDateIso,
          `activation (${activationDateIso}) <= deactivation (${deactivationDateIso})`,
        ).toBeTruthy();
        expect(
          deactivationDateIso < lockDateIso,
          `deactivation (${deactivationDateIso}) must be before lock (${lockDateIso} via ${lockDateField})`,
        ).toBeTruthy();

        podIdentifier = await resolvePodIdentifier(Request, Endpoints, Responses);
        expect(podIdentifier, 'POD identifier for Information of deactivation MI').toBeTruthy();

        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
        TestRunSummary.registerPayload('invoice', {
          invoiceId: lock.invoiceId,
          billingRunId: lock.billingRunId,
          invoiceType: lock.invoiceSnapshot.invoiceType,
          meterReadingTo: lock.invoiceSnapshot.meterReadingTo,
          lockDateIso,
          lockDateField,
          activationDateIso,
          deactivationDateIso,
        });

        console.log(
          `[PDT-2177] contractId=${contractId} pod=${podIdentifier} ` +
            `activation=${activationDateIso} lock=${lockDateIso} (${lockDateField}) ` +
            `deactivationDate=${deactivationDateIso}`,
        );
      });

      await test.step('Upload SUPPLY_ACTION_DEACTIVATIONS mass import (Information of deactivation)', async () => {
        const flow = await runSupplyActionDeactivationMassImportFlow(
          Request,
          FileUploadRequest,
          Responses,
          podIdentifier,
          deactivationDateIso,
        );
        processId = flow.processId;
        expect(processId, 'supply action deactivation process id').toBeGreaterThan(0);
        expect(flow.processStatus, 'mass import process status').toBe('COMPLETED');

        TestRunSummary.registerPayload('supplyActionDeactivationMassImport', {
          domainType: 'SUPPLY_ACTION_DEACTIVATIONS',
          podIdentifier,
          deactivationDateIso,
          processId,
          uploadStatus: flow.uploadStatus,
          errorReportSummary: flow.reportSnapshot.summary,
        });

        const assertion = assertRestrictedPeriodErrorInReport(flow.reportSnapshot);
        TestRunSummary.recordCheck({
          check: 'Restricted-period error in deactivateWithActionNew (not onComplete penalty)',
          expectedResult: assertion.expectedResult,
          actualResult: assertion.passed
            ? `As expected — ${assertion.actualResult}`
            : `Not as expected — ${assertion.actualResult}`,
          passed: assertion.passed,
        });

        const combined = flow.reportSnapshot.failedRows.map((r) => r.errors ?? '').join('\n');
        expect(combined.toLowerCase()).toContain(RESTRICTED_PERIOD_SNIPPET.toLowerCase());
        expect(combined.toLowerCase()).not.toContain(
          OLD_MISLEADING_CONTRACT_VERSION_SNIPPET.toLowerCase(),
        );
        expect(combined.toLowerCase()).not.toContain(PENALTY_ONCOMPLETE_SNIPPET.toLowerCase());
      });

      await test.step('Attach test run summary', async () => {
        const extraLinks: Record<string, string[]> = {};
        const processUrl = buildProcessPreviewLink(processId);
        if (processUrl) {
          extraLinks.process = [processUrl];
        }
        Object.assign(extraLinks, buildProductContractTabLinks(contractId));

        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-2177',
          relevantEntityKeys: ['customer', 'pod', 'product', 'productContract', 'billingRun', 'invoice'],
          extraLinks: Object.keys(extraLinks).length ? extraLinks : undefined,
          snapshot: {
            contractId,
            podIdentifier,
            processId,
            activationDateIso,
            lockDateIso,
            lockDateField,
            deactivationDateIso,
          },
        });
      });
    },
  );
});
