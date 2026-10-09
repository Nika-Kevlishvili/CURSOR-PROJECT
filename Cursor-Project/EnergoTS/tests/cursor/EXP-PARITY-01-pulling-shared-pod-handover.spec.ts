/**
 * EXP-PARITY-01 — PreProd vs Experiment standard-billing parity.
 *
 * NAMING: `EXP-PARITY-01` is a PSEUDO-KEY placeholder. No Jira ticket exists
 * for this parity work yet; rename the files, `EXP_PARITY_01_KEY` and the test
 * titles once a real key is created.
 *
 * HOW TO USE
 * ----------
 * Run this spec twice, changing ONLY `BASE_URL` (PreProd, then Experiment),
 * then diff the two `[EXP-PARITY-01] parity snapshot — …` attachments. An
 * empty diff means the rewritten Experiment billing engine
 * (`standard_billing_preparation` + `standard_billing_stage_01..22`) behaves
 * like the PreProd engine (`billing_run.generate_run_volume`) for these cases.
 *
 * The spec never inspects, hardcodes or branches on the environment.
 * Rationale, Swagger validation notes and the pulling rules are documented in
 * `./exp-parity-01-pulling-shared-pod-handover.fixtures`.
 *
 * Default describe mode (not serial): the 15-minute Excel used for the profile
 * upload is written to a fixed `15min.xlsx` in the working directory by
 * `priceParameterMP` (outside `tests/`, not modifiable), so the two cases must
 * not upload concurrently. `mode: 'default'` opts this describe out of
 * `fullyParallel` without skip-on-failure — a Case 1 failure must not skip
 * Case 11. Each case still builds its own complete precondition chain and does
 * not depend on the other having run (no `beforeAll` — Rule 40).
 */

import { expect, finalizeTestRunSummary, test } from './cursor-test.fixtures';
import { attachParitySnapshot } from './shared/exp-parity-snapshot.fixtures';
import {
  buildExpParitySharedPodHandoverChain,
  createContractLevelVolumesPlusInterimRun,
  createCustomerLevelVolumesRun,
  expParityContractRoles,
  expParityProductRoles,
  loadAllExpParityInvoiceFacts,
  parityInvoiceFacts,
  settleBillingDataPreparationCron,
  startBillingAndReadDrafts,
  EXP_PARITY_01_CASE_1_ID,
  EXP_PARITY_01_CASE_1_TITLE,
  EXP_PARITY_01_CASE_11_ID,
  EXP_PARITY_01_CASE_11_TITLE,
  EXP_PARITY_01_DRAFT_INVOICE_STATUS,
  EXP_PARITY_01_HANDOVER_DAY,
  EXP_PARITY_01_INTERIM_AMOUNT,
  EXP_PARITY_01_KEY,
  EXP_PARITY_01_KWH_PER_SLOT,
  EXP_PARITY_01_PRICE_A,
  EXP_PARITY_01_PRICE_B,
  EXP_PARITY_01_TEST_TIMEOUT_MS,
  EXP_PARITY_01_VOLUMES_INVOICE_TYPE,
  parityUnitPricesExclude,
  parityUnitPricesInclude,
  type ExpParityChain,
  type ExpParityFx,
  type ExpParityInvoiceFacts,
} from './exp-parity-01-pulling-shared-pod-handover.fixtures';

const INTERIM_INVOICE_TYPE = 'INTERIM_AND_ADVANCE_PAYMENT';

function countByContractRole(invoices: ExpParityInvoiceFacts[]): Record<string, number> {
  const counts: Record<string, number> = {};
  for (const invoice of invoices) {
    counts[invoice.contractRole] = (counts[invoice.contractRole] ?? 0) + 1;
  }
  return counts;
}

function countByInvoiceType(invoices: ExpParityInvoiceFacts[]): Record<string, number> {
  const counts: Record<string, number> = {};
  for (const invoice of invoices) {
    counts[invoice.invoiceType] = (counts[invoice.invoiceType] ?? 0) + 1;
  }
  return counts;
}

function profileFacts(chain: ExpParityChain): Record<string, unknown> {
  return {
    periodType: 'FIFTEEN_MINUTES',
    slotCount: chain.fifteenMinuteSlotCount,
    kwhPerSlot: EXP_PARITY_01_KWH_PER_SLOT,
    totalKwh: chain.totalKwh,
  };
}

function handoverFacts(): Record<string, unknown> {
  return {
    sharedPodCount: 1,
    contractCount: 2,
    handoverDayOfMonth: EXP_PARITY_01_HANDOVER_DAY,
    gapDays: 0,
    overlapDays: 0,
    coversWholePreviousMonth: true,
  };
}

test.describe.configure({ mode: 'default' });

test.describe(
  `[${EXP_PARITY_01_KEY}]: PreProd vs Experiment standard billing parity`,
  { tag: '@billing' },
  () => {
    // ─────────────────────────────────────────────────────────────────────
    // Case 1
    // ─────────────────────────────────────────────────────────────────────
    test(`[${EXP_PARITY_01_KEY}]: ${EXP_PARITY_01_CASE_1_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(EXP_PARITY_01_TEST_TIMEOUT_MS);
      const fx: ExpParityFx = { Request, GeneratePayload, Responses, Endpoints };

      const chain = await buildExpParitySharedPodHandoverChain(fx, {
        withInterimOnContractB: false,
      });

      TestRunSummary.registerPayload('handover', {
        sharedPod: chain.podId,
        contractA: { id: chain.contractAId, unitPrice: EXP_PARITY_01_PRICE_A },
        contractB: { id: chain.contractBId, unitPrice: EXP_PARITY_01_PRICE_B },
        window: chain.window,
      });
      TestRunSummary.registerPayload('profile', profileFacts(chain));

      await settleBillingDataPreparationCron();

      const billingRunId = await test.step(
        'Action: create CUSTOMER-level STANDARD_BILLING / FOR_VOLUMES run',
        async () => createCustomerLevelVolumesRun(fx),
      );
      TestRunSummary.registerPayload('billingRun', {
        billingRunId,
        billingType: 'STANDARD_BILLING',
        applicationModelType: ['FOR_VOLUMES'],
        billingApplicationLevel: 'CUSTOMER',
        listOfCustomersContractsOrPOD: chain.customerIdentifier,
      });

      const run = await startBillingAndReadDrafts(Request, billingRunId);

      const invoices = await test.step('Read: draft invoice facts', async () =>
        loadAllExpParityInvoiceFacts(
          Request,
          run.draftInvoiceIds,
          expParityContractRoles(chain),
          expParityProductRoles(chain),
        ));

      // Documented rule — Confluence "Rules for pulling" (page 256049175):
      // "In all other cases invoice in latest contract all billing data from
      // all old contracts." REG-965 confirms the second contract's price
      // component, product and term are used.
      await test.step('Assert: exactly one customer-level invoice is produced', async () => {
        expect(
          run.draftInvoiceIds,
          'Pulled volumes must land on a single invoice for the latest contract',
        ).toHaveLength(1);
      });

      await test.step('Assert: the invoice belongs to contract B (latest contract)', async () => {
        expect(invoices[0].contractRole, 'volume must be invoiced in the latest contract').toBe(
          'contractB',
        );
        expect(invoices[0].productRole, 'invoice must use the latest contract product').toBe(
          'productB',
        );
      });

      await test.step(
        `Assert: volumes invoice type is ${EXP_PARITY_01_VOLUMES_INVOICE_TYPE} and status is ${EXP_PARITY_01_DRAFT_INVOICE_STATUS}`,
        async () => {
          expect(
            invoices[0].invoiceType,
            'Swagger InvoiceResponse.invoiceType for volumes',
          ).toBe(EXP_PARITY_01_VOLUMES_INVOICE_TYPE);
          expect(
            invoices[0].invoiceStatus,
            'Swagger InvoiceResponse.invoiceStatus for draft invoices',
          ).toBe(EXP_PARITY_01_DRAFT_INVOICE_STATUS);
        },
      );

      await test.step('Assert: contract A has no invoice for the same kWh', async () => {
        expect(
          invoices.filter((invoice) => invoice.contractRole === 'contractA'),
          'the old contract must not be invoiced separately',
        ).toHaveLength(0);
      });

      await test.step(
        `Assert: priced with contract B price component (${EXP_PARITY_01_PRICE_B}, not ${EXP_PARITY_01_PRICE_A})`,
        async () => {
          expect(
            invoices[0].summaryRowCount,
            'invoice/summary-data must expose priced lines',
          ).toBeGreaterThan(0);
          expect(
            parityUnitPricesInclude(invoices[0].unitPrices, EXP_PARITY_01_PRICE_B),
            'contract B unit price must be used',
          ).toBe(true);
          expect(
            parityUnitPricesExclude(invoices[0].unitPrices, EXP_PARITY_01_PRICE_A),
            'contract A unit price must not appear',
          ).toBe(true);
        },
      );

      TestRunSummary.recordCheck({
        check: 'Volumes on a shared POD with adjacent handover are pulled into the latest contract',
        expectedResult:
          'Exactly 1 invoice, owned by contract B / product B, priced with contract B ' +
          `unit price ${EXP_PARITY_01_PRICE_B}; contract A has no invoice`,
        actualResult:
          `${run.draftInvoiceIds.length} invoice(s); owner=${invoices[0]?.contractRole}; ` +
          `product=${invoices[0]?.productRole}; unitPrices=${JSON.stringify(invoices[0]?.unitPrices)}`,
        passed:
          run.draftInvoiceIds.length === 1 &&
          invoices[0]?.contractRole === 'contractB' &&
          invoices[0]?.productRole === 'productB' &&
          parityUnitPricesInclude(invoices[0]?.unitPrices ?? [], EXP_PARITY_01_PRICE_B) &&
          parityUnitPricesExclude(invoices[0]?.unitPrices ?? [], EXP_PARITY_01_PRICE_A),
      });

      const snapshot = attachParitySnapshot({
        key: EXP_PARITY_01_KEY,
        caseId: EXP_PARITY_01_CASE_1_ID,
        description:
          'Shared POD, adjacent sequential handover, CUSTOMER-level FOR_VOLUMES run — ' +
          'volume must be invoiced in the latest contract',
        facts: {
          scenario: 'shared-pod-sequential-handover',
          billingType: 'STANDARD_BILLING',
          billingApplicationLevel: 'CUSTOMER',
          applicationModelTypes: ['FOR_VOLUMES'],
          billingRunStatus: run.billingRunStatus,
          draftInvoiceCount: run.draftInvoiceIds.length,
          invoicesByContractRole: countByContractRole(invoices),
          invoicesByType: countByInvoiceType(invoices),
          invoices: invoices.map(parityInvoiceFacts),
          pricing: {
            contractAUnitPrice: EXP_PARITY_01_PRICE_A,
            contractBUnitPrice: EXP_PARITY_01_PRICE_B,
          },
          handover: handoverFacts(),
          profile: profileFacts(chain),
        },
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: EXP_PARITY_01_KEY,
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
    // Case 11
    // ─────────────────────────────────────────────────────────────────────
    test(`[${EXP_PARITY_01_KEY}]: ${EXP_PARITY_01_CASE_11_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(EXP_PARITY_01_TEST_TIMEOUT_MS);
      const fx: ExpParityFx = { Request, GeneratePayload, Responses, Endpoints };

      const chain = await buildExpParitySharedPodHandoverChain(fx, {
        withInterimOnContractB: true,
      });

      TestRunSummary.registerPayload('handover', {
        sharedPod: chain.podId,
        contractA: { id: chain.contractAId, unitPrice: EXP_PARITY_01_PRICE_A, interim: null },
        contractB: {
          id: chain.contractBId,
          unitPrice: EXP_PARITY_01_PRICE_B,
          interim: chain.interimId,
        },
        window: chain.window,
      });
      TestRunSummary.registerPayload('interim', {
        interimId: chain.interimId,
        valueType: 'EXACT_AMOUNT',
        value: EXP_PARITY_01_INTERIM_AMOUNT,
        attachedTo: 'contractB product only',
      });
      TestRunSummary.registerPayload('profile', profileFacts(chain));

      await settleBillingDataPreparationCron();

      const billingRunId = await test.step(
        'Action: create CONTRACT-level STANDARD_BILLING run for FOR_VOLUMES + ' +
          'INTERIM_AND_ADVANCE_PAYMENT, listing ONLY contract A',
        async () => createContractLevelVolumesPlusInterimRun(fx, 0),
      );
      TestRunSummary.registerPayload('billingRun', {
        billingRunId,
        billingType: 'STANDARD_BILLING',
        applicationModelType: ['FOR_VOLUMES', 'INTERIM_AND_ADVANCE_PAYMENT'],
        billingApplicationLevel: 'CONTRACT',
        listOfCustomersContractsOrPOD: chain.contractANumber,
      });

      const run = await startBillingAndReadDrafts(Request, billingRunId);

      const invoices = await test.step('Read: draft invoice facts', async () =>
        loadAllExpParityInvoiceFacts(
          Request,
          run.draftInvoiceIds,
          expParityContractRoles(chain),
          expParityProductRoles(chain),
        ));

      const interimInvoices = invoices.filter(
        (invoice) => invoice.invoiceType === INTERIM_INVOICE_TYPE,
      );
      const volumeInvoices = invoices.filter(
        (invoice) => invoice.invoiceType !== INTERIM_INVOICE_TYPE,
      );
      const interimForContractA = interimInvoices.filter(
        (invoice) => invoice.contractRole === 'contractA',
      );
      const interimForContractB = interimInvoices.some(
        (invoice) => invoice.contractRole === 'contractB',
      );

      // HARD ASSERT — contract A carries no IAP at all, so no interim invoice
      // may be issued for it regardless of how pulling is implemented.
      await test.step(
        'Assert: contract A (no interim attached) gets no INTERIM_AND_ADVANCE_PAYMENT invoice',
        async () => {
          expect(
            interimForContractA,
            'contract A has no IAP on its product — it must not be invoiced for interim',
          ).toHaveLength(0);
        },
      );

      // HARD ASSERT — the volumes side must still obey the Case 1 pulling rule
      // even though the run lists only contract A.
      await test.step(
        'Assert: volumes still invoiced once, in contract B, with contract B pricing',
        async () => {
          expect(volumeInvoices, 'exactly one volumes invoice is expected').toHaveLength(1);
          expect(volumeInvoices[0].contractRole, 'volumes belong to the latest contract').toBe(
            'contractB',
          );
          expect(volumeInvoices[0].productRole, 'volumes use the latest contract product').toBe(
            'productB',
          );
          expect(
            volumeInvoices[0].invoiceType,
            'Swagger InvoiceResponse.invoiceType for volumes',
          ).toBe(EXP_PARITY_01_VOLUMES_INVOICE_TYPE);
          expect(
            volumeInvoices[0].invoiceStatus,
            'Swagger InvoiceResponse.invoiceStatus for draft invoices',
          ).toBe(EXP_PARITY_01_DRAFT_INVOICE_STATUS);
          expect(
            volumeInvoices[0].summaryRowCount,
            'invoice/summary-data must expose priced lines',
          ).toBeGreaterThan(0);
          expect(
            parityUnitPricesInclude(volumeInvoices[0].unitPrices, EXP_PARITY_01_PRICE_B),
            'contract B unit price must be used',
          ).toBe(true);
          expect(
            parityUnitPricesExclude(volumeInvoices[0].unitPrices, EXP_PARITY_01_PRICE_A),
            'contract A unit price must not appear',
          ).toBe(true);
        },
      );

      // NOT ASSERTED — open product question, deliberately not a pass/fail gate.
      //
      // Confluence "Rules for pulling" only states "Interim deduction/generation
      // should discuss", so there is no documented expected behaviour for an
      // interim attached to a pulled contract. Runtime evidence suggests the two
      // engines can legitimately disagree here:
      //   * `billing_run.generate_run_interim` fills `run_interim_contracts` in
      //     two steps. Step 1 reads only `billing_run.run_criteria_data`, which
      //     for CONTRACT level + list = contract A yields contract A alone.
      //     Step 2 ("Start importing pulled contracts") is guarded by
      //     `with_standard_invoice = true` — set by
      //     `execute_interim_data_preparation_job_task` when the run has more
      //     than one application model type — and imports pulled contracts from
      //     `billing_run.run_info` where `parent_id is not null`, joined to
      //     already-existing `invoice.invoices` for the same `billing_id`.
      //   * On Experiment none of the `standard_billing_stage_*` procedures
      //     INSERT into `billing_run.run_info` (stage 14 writes `run_contracts`,
      //     `bdbs_splits_io` and `settlement_period`), so that import step can
      //     find nothing there.
      //
      // Until the product decision exists, the expected value is simply
      // "whatever PreProd produced" — the parity diff is the deliverable.
      TestRunSummary.recordCheck({
        check:
          'UNDECIDED — interim invoice for the pulled contract B when the run lists only contract A',
        expectedResult:
          'Same as the PreProd baseline (no documented product rule; Confluence "Rules for ' +
          'pulling" says only "Interim deduction/generation should discuss")',
        actualResult: `INTERIM_AND_ADVANCE_PAYMENT invoice for contract B present = ${interimForContractB}`,
        passed: true,
      });

      TestRunSummary.recordCheck({
        check: 'Contract A without interim receives no INTERIM_AND_ADVANCE_PAYMENT invoice',
        expectedResult: '0 interim invoices for contract A',
        actualResult: `${interimForContractA.length} interim invoice(s) for contract A`,
        passed: interimForContractA.length === 0,
      });

      TestRunSummary.recordCheck({
        check: 'Pulling rule still applies when the run lists only the old contract',
        expectedResult:
          `1 volumes invoice, owned by contract B, priced with unit price ${EXP_PARITY_01_PRICE_B}`,
        actualResult:
          `${volumeInvoices.length} volumes invoice(s); owner=${volumeInvoices[0]?.contractRole}; ` +
          `unitPrices=${JSON.stringify(volumeInvoices[0]?.unitPrices)}`,
        passed:
          volumeInvoices.length === 1 &&
          volumeInvoices[0]?.contractRole === 'contractB' &&
          parityUnitPricesInclude(volumeInvoices[0]?.unitPrices ?? [], EXP_PARITY_01_PRICE_B),
      });

      const snapshot = attachParitySnapshot({
        key: EXP_PARITY_01_KEY,
        caseId: EXP_PARITY_01_CASE_11_ID,
        description:
          'Interim on the pulled contract while the CONTRACT-level run lists only contract A — ' +
          'the interim outcome is an open product question, exposed via this diff',
        facts: {
          scenario: 'interim-on-pulled-contract',
          billingType: 'STANDARD_BILLING',
          billingApplicationLevel: 'CONTRACT',
          applicationModelTypes: ['FOR_VOLUMES', INTERIM_INVOICE_TYPE],
          billingRunListedContractRole: 'contractA',
          billingRunStatus: run.billingRunStatus,
          draftInvoiceCount: run.draftInvoiceIds.length,
          invoicesByContractRole: countByContractRole(invoices),
          invoicesByType: countByInvoiceType(invoices),
          invoices: invoices.map(parityInvoiceFacts),
          /** Hard-asserted: must stay false in both environments. */
          interimInvoiceForContractA: interimForContractA.length > 0,
          /** UNDECIDED: whatever this is on PreProd is the parity baseline. */
          interimInvoiceForContractB: interimForContractB,
          interim: {
            valueType: 'EXACT_AMOUNT',
            amount: EXP_PARITY_01_INTERIM_AMOUNT,
            attachedToContractRole: 'contractB',
          },
          pricing: {
            contractAUnitPrice: EXP_PARITY_01_PRICE_A,
            contractBUnitPrice: EXP_PARITY_01_PRICE_B,
          },
          handover: handoverFacts(),
          profile: profileFacts(chain),
        },
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: EXP_PARITY_01_KEY,
          relevantEntityKeys: [
            'customer',
            'pod',
            'product',
            'productContract',
            'interim',
            'dataByProfiles',
            'billingRun',
          ],
          snapshot: snapshot as unknown as Record<string, unknown>,
        });
      });
    });
  },
);
