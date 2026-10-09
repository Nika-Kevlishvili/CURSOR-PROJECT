/**
 * EXP-PARITY-04 — PreProd vs Experiment billing-run hard-lock parity.
 *
 * Run twice with ONLY `BASE_URL` changed (PreProd, then Experiment) and diff the
 * `[EXP-PARITY-04] parity snapshot — …` attachments. The spec never branches on
 * environment or hostname.
 *
 * `mode: 'default'` — the shared 15-minute profile Excel (`15min.xlsx`) must not
 * be uploaded concurrently (see exp-parity-01 rationale).
 *
 * Reference spec(s):
 * - tests/cursor/EXP-PARITY-01-pulling-shared-pod-handover.spec.ts
 * - tests/cursor/exp-parity-04-billing-run-hard-lock.fixtures.ts
 */

import { expect, finalizeTestRunSummary, test } from './cursor-test.fixtures';
import { attachParitySnapshot } from './shared/exp-parity-snapshot.fixtures';
import {
  buildExpParity04VolumesPrechain,
  createExpParity04ContractVolumesRun,
  EXP_PARITY_04_CASE_9_ID,
  EXP_PARITY_04_CASE_9_TITLE,
  EXP_PARITY_04_CASE_10_ID,
  EXP_PARITY_04_CASE_10_TITLE,
  EXP_PARITY_04_KEY,
  EXP_PARITY_04_TEST_TIMEOUT_MS,
  parityLockProbeFacts,
  startBillingAndProbeContractLockWindows,
  startBillingProbeLockListAndTerminate,
  type ExpParity04Fx,
  type LockProbeResult,
} from './exp-parity-04-billing-run-hard-lock.fixtures';
import { terminateBillingRunAndWaitCancelled } from './pdt-3087-government-compensation-defer-to-pdf.fixtures';

test.describe.configure({ mode: 'default' });

function summarizeProbes(probes: LockProbeResult[]): Record<string, unknown> {
  const byFamily: Record<string, unknown> = {};
  for (const probe of probes) {
    byFamily[probe.family] = parityLockProbeFacts(probe);
  }
  return byFamily;
}

function rejectedFamilies(probes: LockProbeResult[]): string[] {
  return probes.filter((p) => p.rejected).map((p) => p.family);
}

test.describe(
  `[${EXP_PARITY_04_KEY}]: PreProd vs Experiment billing-run hard-lock parity`,
  { tag: '@billing' },
  () => {
    // ─────────────────────────────────────────────────────────────────────
    // Case 9 — contract edit during data preparation (two windows)
    // ─────────────────────────────────────────────────────────────────────
    test(`[${EXP_PARITY_04_KEY}]: ${EXP_PARITY_04_CASE_9_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(EXP_PARITY_04_TEST_TIMEOUT_MS);
      const fx: ExpParity04Fx = { Request, GeneratePayload, Responses, Endpoints };

      const chain = await buildExpParity04VolumesPrechain(fx, { withDiscountAndIap: false });

      TestRunSummary.registerPayload('prechain', {
        contractId: chain.contractId,
        contractNumber: chain.contractNumber,
        contractVersionId: chain.contractVersionId,
        customerId: chain.customerId,
        podId: chain.podId,
        productId: chain.productId,
        billingByProfileId: chain.billingByProfileId,
        profileWindow: chain.window,
      });

      const billingRunId = await test.step(
        'Action: create CONTRACT-level STANDARD_BILLING / FOR_VOLUMES run',
        async () => createExpParity04ContractVolumesRun(fx, chain.contractNumber),
      );
      TestRunSummary.registerPayload('billingRun', {
        billingRunId,
        billingType: 'STANDARD_BILLING',
        applicationModelType: ['FOR_VOLUMES'],
        billingApplicationLevel: 'CONTRACT',
        listOfCustomersContractsOrPOD: chain.contractNumber,
      });

      const outcome = await startBillingAndProbeContractLockWindows(fx, chain, billingRunId);

      // Window 1 — open parity question (wiki lock-at-start vs Experiment lock-at-end-of-prep).
      // Do NOT hard-fail; record for PreProd baseline diff.
      TestRunSummary.recordCheck({
        check:
          'Window 1 — contract edit before prep FINISHED (open parity: wiki vs Experiment lock timing)',
        expectedResult: 'Same as the PreProd baseline',
        actualResult:
          `window1ProbeCount=${outcome.window1Probes.length}; ` +
          `prepFinishedBeforeAnyWindow1Probe=${outcome.prepFinishedBeforeWindow1}; ` +
          `lastWindow1Status=${outcome.window1Probes.at(-1)?.httpStatus ?? 'none'}; ` +
          `lastWindow1Rejected=${outcome.window1Probes.at(-1)?.rejected ?? 'n/a'}`,
        passed: true,
      });

      // Window 2 — HARD ASSERT: after prep FINISHED (proxy: first rejection while
      // IN_PROGRESS_DRAFT), contract edit MUST be rejected.
      await test.step(
        'Assert: contract edit rejected after data prep FINISHED (still IN_PROGRESS_DRAFT)',
        async () => {
          expect(
            outcome.prepFinishedLockObserved,
            'prep FINISHED proxy — contract PUT must be rejected while status is IN_PROGRESS_DRAFT',
          ).toBe(true);
          const prepProbe = outcome.prepFinishedProbes[0];
          expect(prepProbe, 'prep-finished probe must be recorded').toBeTruthy();
          expect(prepProbe?.rejected, 'contract edit must be rejected after prep FINISHED').toBe(
            true,
          );
          expect(prepProbe?.httpStatus, 'rejection must be non-2xx').toBeGreaterThanOrEqual(400);
        },
      );

      TestRunSummary.recordCheck({
        check: 'Window 2 — contract edit rejected after prep FINISHED',
        expectedResult:
          'PUT /product-contract/{id} returns non-2xx while billing run is IN_PROGRESS_DRAFT ' +
          'and run_main_data_preparation_status has reached FINISHED (behavioural proxy)',
        actualResult:
          `prepFinishedLockObserved=${outcome.prepFinishedLockObserved}; ` +
          `httpStatus=${outcome.prepFinishedProbes[0]?.httpStatus}; ` +
          `errorShape=${JSON.stringify(outcome.prepFinishedProbes[0]?.errorShape)}`,
        passed: outcome.prepFinishedLockObserved && outcome.prepFinishedProbes[0]?.rejected === true,
      });

      // DRAFT moment — contract should remain locked until run terminates.
      const draftProbe = outcome.draftProbes[0];
      await test.step(
        'Assert: contract edit still rejected at DRAFT (per-run lock until REAL/terminate)',
        async () => {
          expect(draftProbe, 'draft contract probe must be recorded').toBeTruthy();
          expect(draftProbe?.rejected, 'contract edit must stay rejected at DRAFT').toBe(true);
          expect(draftProbe?.httpStatus, 'rejection must be non-2xx at DRAFT').toBeGreaterThanOrEqual(400);
        },
      );
      TestRunSummary.recordCheck({
        check: 'DRAFT status — contract edit remains rejected (per-run lock, not per-contract release)',
        expectedResult: 'Contract PUT still non-2xx at DRAFT (lock released per run on REAL/terminate only)',
        actualResult: draftProbe
          ? `httpStatus=${draftProbe.httpStatus}; rejected=${draftProbe.rejected}`
          : 'no draft probe recorded',
        passed: draftProbe?.rejected === true,
      });

      let terminated = false;
      await test.step('Cleanup: terminate billing run to release locks', async () => {
        const termStatus = await terminateBillingRunAndWaitCancelled(Request, billingRunId);
        terminated = termStatus === 'CANCELLED';
        expect(terminated, 'billing run must reach CANCELLED after terminate').toBe(true);
      });

      const snapshot = attachParitySnapshot({
        key: EXP_PARITY_04_KEY,
        caseId: EXP_PARITY_04_CASE_9_ID,
        description:
          'Contract edit probe in two windows during start-billing data preparation — ' +
          'window 1 is parity-record-only; window 2 hard-asserts rejection after prep FINISHED',
        facts: {
          scenario: 'contract-edit-during-data-prep',
          billingRunId,
          billingRunFinalStatus: outcome.finalStatus,
          prepFinishedProxy: {
            fieldNote:
              'run_main_data_preparation_status is DB-only — not on BillingRunResponse in Experiment Swagger',
            observedVia: 'first contract PUT non-2xx while IN_PROGRESS_DRAFT',
            prepFinishedLockObserved: outcome.prepFinishedLockObserved,
            prepFinishedBeforeWindow1: outcome.prepFinishedBeforeWindow1,
          },
          window1: {
            probeCount: outcome.window1Probes.length,
            probes: outcome.window1Probes.map(parityLockProbeFacts),
          },
          window2PrepFinished: summarizeProbes(outcome.prepFinishedProbes),
          draft: summarizeProbes(outcome.draftProbes),
          terminationCleanup: terminated,
          wikiVsExperimentNote:
            'Wiki: lock starts at run start (two stages) but edits apply until lock completes. ' +
            'Experiment: standard_billing_stage_21_lock_objects at END of prep sets FINISHED then locks.',
        },
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: EXP_PARITY_04_KEY,
          relevantEntityKeys: [
            'customer',
            'pod',
            'product',
            'productContract',
            'dataByProfiles',
            'billingRun',
          ],
          snapshot: snapshot as unknown as Record<string, unknown>,
        });
      });
    });

    // ─────────────────────────────────────────────────────────────────────
    // Case 10 — locked object set vs wiki (prep FINISHED + DRAFT)
    // ─────────────────────────────────────────────────────────────────────
    test(`[${EXP_PARITY_04_KEY}]: ${EXP_PARITY_04_CASE_10_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(EXP_PARITY_04_TEST_TIMEOUT_MS);
      const fx: ExpParity04Fx = { Request, GeneratePayload, Responses, Endpoints };

      const chain = await buildExpParity04VolumesPrechain(fx, { withDiscountAndIap: true });

      TestRunSummary.registerPayload('prechain', {
        contractId: chain.contractId,
        contractNumber: chain.contractNumber,
        customerId: chain.customerId,
        podId: chain.podId,
        productId: chain.productId,
        priceComponentId: chain.priceComponentId,
        discountId: chain.discountId,
        iapId: chain.iapId,
      });

      const billingRunId = await test.step(
        'Action: create CONTRACT-level STANDARD_BILLING / FOR_VOLUMES run',
        async () => createExpParity04ContractVolumesRun(fx, chain.contractNumber),
      );
      TestRunSummary.registerPayload('billingRun', {
        billingRunId,
        billingType: 'STANDARD_BILLING',
        applicationModelType: ['FOR_VOLUMES'],
        billingApplicationLevel: 'CONTRACT',
        listOfCustomersContractsOrPOD: chain.contractNumber,
      });

      const lockOutcome = await startBillingProbeLockListAndTerminate(fx, chain, billingRunId);

      const prepContract = lockOutcome.prepFinishedProbes.find((p) => p.family === 'contract');
      const draftContract = lockOutcome.draftProbes.find((p) => p.family === 'contract');

      await test.step('Assert: contract edit rejected at prep FINISHED moment', async () => {
        expect(prepContract, 'prep-finished contract probe required').toBeTruthy();
        expect(prepContract?.rejected).toBe(true);
        expect(prepContract?.httpStatus).toBeGreaterThanOrEqual(400);
      });

      await test.step('Assert: contract edit rejected at DRAFT moment', async () => {
        expect(draftContract, 'draft contract probe required').toBeTruthy();
        expect(draftContract?.rejected).toBe(true);
        expect(draftContract?.httpStatus).toBeGreaterThanOrEqual(400);
      });

      // Wiki lists IAP + discounts locked; Experiment stage_21 EXCLUDES them (adds at DRAFT gen).
      const prepIap = lockOutcome.prepFinishedProbes.find((p) => p.family === 'iap');
      const prepDiscount = lockOutcome.prepFinishedProbes.find((p) => p.family === 'discount');
      const draftIap = lockOutcome.draftProbes.find((p) => p.family === 'iap');
      const draftDiscount = lockOutcome.draftProbes.find((p) => p.family === 'discount');

      TestRunSummary.recordCheck({
        check:
          'UNDECIDED — IAP/discount lock timing (wiki lists them; Experiment stage_21 excludes, DRAFT gen adds)',
        expectedResult: 'Same as the PreProd baseline',
        actualResult:
          `prepFinished: iapRejected=${prepIap?.rejected}, discountRejected=${prepDiscount?.rejected}; ` +
          `draft: iapRejected=${draftIap?.rejected}, discountRejected=${draftDiscount?.rejected}`,
        passed: true,
      });

      TestRunSummary.recordCheck({
        check: 'Contract hard-lock at prep FINISHED and DRAFT',
        expectedResult: 'Contract PUT non-2xx at both moments',
        actualResult:
          `prepFinished contract httpStatus=${prepContract?.httpStatus}; ` +
          `draft contract httpStatus=${draftContract?.httpStatus}`,
        passed: prepContract?.rejected === true && draftContract?.rejected === true,
      });

      TestRunSummary.recordCheck({
        check: 'Termination cleanup releases per-run lock',
        expectedResult: 'PATCH /billing-run/terminate → status CANCELLED',
        actualResult: `terminated=${lockOutcome.terminated}; status=${lockOutcome.terminationStatus}`,
        passed: lockOutcome.terminated,
      });

      const snapshot = attachParitySnapshot({
        key: EXP_PARITY_04_KEY,
        caseId: EXP_PARITY_04_CASE_10_ID,
        description:
          'Lock-list comparison at prep FINISHED vs DRAFT — contract hard-asserted; ' +
          'IAP/discount wiki-vs-runtime recorded for parity diff',
        facts: {
          scenario: 'locked-object-set-vs-wiki',
          billingRunId,
          billingRunFinalStatus: lockOutcome.finalStatus,
          documentedLockList:
            'BBP, BDS, Contract, Customer, Product, Service, POD, PCs, PC group, ' +
            'price parameters, IAP, IAP groups, Discounts, all Currency, all VAT, Compensation',
          experimentRuntimeNotes: {
            stage21:
              'Locks contracts/customers/products/services/PODs/PCs/PC groups/price parameters/' +
              'data-by-profiles/data-by-scales/currencies/vat-rates/compensation; EXCLUDES interim+discounts',
            lockObjectsDraftGeneration:
              'After invoice generation / DRAFT ADDS interim, IAP groups, discount-from-duty-to-society; ' +
              'EXCLUDES currencies/vat-rates',
            lockRelease:
              'delete from lock.locks where billing_id = p_run_id on REAL or terminate — not per contract',
          },
          prepFinished: {
            rejectedFamilies: rejectedFamilies(lockOutcome.prepFinishedProbes),
            probes: lockOutcome.prepFinishedProbes.map(parityLockProbeFacts),
          },
          draft: {
            rejectedFamilies: rejectedFamilies(lockOutcome.draftProbes),
            probes: lockOutcome.draftProbes.map(parityLockProbeFacts),
          },
          wikiRuntimeParityFlags: {
            iapLockedAtPrepFinished: prepIap?.rejected ?? null,
            discountLockedAtPrepFinished: prepDiscount?.rejected ?? null,
            iapLockedAtDraft: draftIap?.rejected ?? null,
            discountLockedAtDraft: draftDiscount?.rejected ?? null,
          },
          terminationCleanup: lockOutcome.terminated,
          swaggerEditableFamiliesProbed: [
            'contract',
            'customer',
            'pod',
            'product',
            'priceComponent',
            'iap',
            'discount',
          ],
        },
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: EXP_PARITY_04_KEY,
          relevantEntityKeys: [
            'customer',
            'pod',
            'product',
            'productContract',
            'priceComponent',
            'interim',
            'discount',
            'dataByProfiles',
            'billingRun',
          ],
          snapshot: snapshot as unknown as Record<string, unknown>,
        });
      });
    });
  },
);
