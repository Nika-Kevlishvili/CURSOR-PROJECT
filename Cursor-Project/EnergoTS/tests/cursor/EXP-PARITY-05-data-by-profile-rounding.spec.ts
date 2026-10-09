/**
 * EXP-PARITY-05 — PreProd vs Experiment parity: data-by-profile volume rounding.
 *
 * NAMING: `EXP-PARITY-05` is a PSEUDO-KEY placeholder. No Jira ticket exists
 * for this parity work yet; rename the files, `EXP_PARITY_05_KEY` and the test
 * title once a real key is created.
 *
 * HOW TO USE
 * ----------
 * Run this spec twice, changing ONLY `BASE_URL` (PreProd, then Experiment),
 * then diff the two `[EXP-PARITY-05] parity snapshot — …` attachments. An
 * empty diff means both engines round profile volumes the same way for this case.
 *
 * The spec never inspects, hardcodes or branches on the environment.
 * Rationale, engineered constants, Swagger notes and chain builders live in
 * `./exp-parity-05-data-by-profile-rounding.fixtures`.
 *
 * Describe mode is `default` (not `serial`): there is only one test in this
 * file, so intra-file serialisation would add no isolation. Profile window
 * uploads still run sequentially inside `buildExpParity05RoundingChain` because
 * `priceParameterMP` always overwrites the shared `./15min.xlsx` working file.
 * With `fullyParallel: true` in `playwright.config.ts`, other specs that
 * upload the same filename may still collide if scheduled on the same worker
 * cwd — run parity specs with `--workers=1` when diffing if needed.
 */

import { expect, finalizeTestRunSummary, test } from './cursor-test.fixtures';
import { attachParitySnapshot } from './shared/exp-parity-snapshot.fixtures';
import {
  buildExpParity05RoundingChain,
  createExpParity05ContractVolumesRun,
  describeExpParity05Outcome,
  expParity05ContractRoles,
  loadExpParity05RoundingFacts,
  parityPeriodFacts,
  settleBillingDataPreparationCron,
  startBillingAndReadDrafts,
  EXP_PARITY_05_CASE_ID,
  EXP_PARITY_05_DESIGN,
  EXP_PARITY_05_KEY,
  EXP_PARITY_05_KWH_PER_SLOT,
  EXP_PARITY_05_PERIOD_COUNT,
  EXP_PARITY_05_PERIOD_DAYS,
  EXP_PARITY_05_TEST_TIMEOUT_MS,
  EXP_PARITY_05_TITLE,
  EXP_PARITY_05_UNIT_PRICE,
  type ExpParity05Fx,
} from './exp-parity-05-data-by-profile-rounding.fixtures';

test.describe.configure({ mode: 'default' });

test.describe(
  `[${EXP_PARITY_05_KEY}]: PreProd vs Experiment data-by-profile rounding parity`,
  { tag: ['@billing', '@volumes', '@parity'] },
  () => {
    test(`[${EXP_PARITY_05_KEY}]: ${EXP_PARITY_05_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(EXP_PARITY_05_TEST_TIMEOUT_MS);
      const fx: ExpParity05Fx = { Request, GeneratePayload, Responses, Endpoints };

      expect(
        EXP_PARITY_05_DESIGN.strategiesDiffer,
        'engineered constants must distinguish sum-then-round-once from round-each-period-then-sum',
      ).toBe(true);

      const chain = await buildExpParity05RoundingChain(fx);

      TestRunSummary.registerPayload('design', {
        periodCount: EXP_PARITY_05_DESIGN.periodCount,
        periodDays: EXP_PARITY_05_DESIGN.periodDays,
        kwhPerSlot: EXP_PARITY_05_KWH_PER_SLOT,
        unitPrice: EXP_PARITY_05_UNIT_PRICE,
        kwhPerPeriod: EXP_PARITY_05_DESIGN.kwhPerPeriod,
        exactAmountPerPeriod: EXP_PARITY_05_DESIGN.exactAmountPerPeriod,
        roundedAmountPerPeriod: EXP_PARITY_05_DESIGN.roundedAmountPerPeriod,
        sumThenRoundOnceDesign: EXP_PARITY_05_DESIGN.sumThenRoundOnce,
        roundEachPeriodThenSumDesign: EXP_PARITY_05_DESIGN.roundEachPeriodThenSum,
        strategyGapDesign: EXP_PARITY_05_DESIGN.strategyGap,
      });
      TestRunSummary.registerPayload('chain', {
        contractId: chain.contractId,
        contractNumber: chain.contractNumber,
        billingByProfileIds: chain.billingByProfileIds,
        priceComponentIds: chain.priceComponentIds,
        windowCount: chain.windows.length,
        monthStart: chain.monthStart,
        monthEnd: chain.monthEnd,
        profileCoverageEnd: chain.profileCoverageEnd,
        designedTotalKwh: chain.designedTotalKwh,
      });

      await settleBillingDataPreparationCron();

      const { billingRunId, billingPayload } = await test.step(
        'Action: create CONTRACT-level STANDARD_BILLING / FOR_VOLUMES run',
        async () => createExpParity05ContractVolumesRun(fx),
      );
      TestRunSummary.registerPayload('billingRun', billingPayload);

      const run = await startBillingAndReadDrafts(Request, billingRunId);

      await test.step('Assert: billing run reaches DRAFT', async () => {
        expect(
          run.billingRunStatus,
          'start-billing must finish DRAFT (IN_PROGRESS_DRAFT is polled through)',
        ).toBe('DRAFT');
      });

      await test.step('Assert: at least one draft invoice is produced', async () => {
        expect(
          run.draftInvoiceIds.length,
          'FOR_VOLUMES run with profile data must produce draft invoice(s)',
        ).toBeGreaterThanOrEqual(1);
      });

      for (const invoiceId of run.draftInvoiceIds) {
        Responses.invoice.push(invoiceId);
      }

      const invoiceId = run.draftInvoiceIds[0];
      const contractRoles = expParity05ContractRoles(chain);
      const facts = await test.step('Read: invoice rounding facts from draft invoice', async () =>
        loadExpParity05RoundingFacts(Request, invoiceId, contractRoles));

      await test.step(
        'Assert: computed period count matches the engineered profile windows',
        async () => {
          expect(
            facts.computedPeriodCount,
            `invoice/detailed-data must expose ${EXP_PARITY_05_PERIOD_COUNT} computed period(s)`,
          ).toBe(EXP_PARITY_05_PERIOD_COUNT);
        },
      );

      await test.step(
        'Assert: per-period volumes reconcile between detailed and summary tabs',
        async () => {
          expect(
            facts.volumeReconciliationDelta,
            'detailed-data and summary-data totalVolumes must agree',
          ).toBe(0);
        },
      );

      await test.step(
        'Assert: invoice total equals sum-then-round-once, not round-each-period-then-sum',
        async () => {
          expect(
            facts.strategiesDistinguishable,
            describeExpParity05Outcome(facts),
          ).toBe(true);
          expect(
            facts.invoiceTotalExcludingVat,
            describeExpParity05Outcome(facts),
          ).toBe(facts.sumThenRoundOnce);
          expect(
            facts.matchedStrategy,
            describeExpParity05Outcome(facts),
          ).toBe('sum-then-round-once');
          expect(
            facts.invoiceTotalExcludingVat,
            'invoice total must not equal the sum of per-period roundings when strategies differ',
          ).not.toBe(facts.roundEachPeriodThenSum);
          expect(facts.reconciliationDelta, describeExpParity05Outcome(facts)).toBe(0);
        },
      );

      await test.step(
        'Assert: per-period volumes and amounts reconcile with the invoice total',
        async () => {
          for (const period of facts.periods) {
            expect(
              period.volumeKwh,
              `period ${period.periodIndex} volume must match engineered ${EXP_PARITY_05_DESIGN.kwhPerPeriod} kWh`,
            ).toBe(EXP_PARITY_05_DESIGN.kwhPerPeriod);
            expect(
              period.roundedAmount,
              `period ${period.periodIndex} rounded amount must match design ${EXP_PARITY_05_DESIGN.roundedAmountPerPeriod}`,
            ).toBe(EXP_PARITY_05_DESIGN.roundedAmountPerPeriod);
          }
          expect(
            facts.detailedVolumeKwh,
            'sum of per-period volumes must equal the designed total kWh',
          ).toBe(EXP_PARITY_05_DESIGN.totalKwh);
        },
      );

      const roundingPassed =
        run.billingRunStatus === 'DRAFT' &&
        facts.matchedStrategy === 'sum-then-round-once' &&
        facts.reconciliationDelta === 0 &&
        facts.volumeReconciliationDelta === 0 &&
        facts.computedPeriodCount === EXP_PARITY_05_PERIOD_COUNT;

      TestRunSummary.recordCheck({
        check: 'FIFTEEN_MINUTES profile volumes are rounded once on the total',
        expectedResult:
          `Billing run DRAFT; ${EXP_PARITY_05_PERIOD_COUNT} computed periods; ` +
          `invoice total (excl. VAT) = sum-then-round-once (${EXP_PARITY_05_DESIGN.sumThenRoundOnce}), ` +
          `not round-each-period-then-sum (${EXP_PARITY_05_DESIGN.roundEachPeriodThenSum}); ` +
          'reconciliation delta = 0; detailed/summary volumes agree.',
        actualResult: describeExpParity05Outcome(facts),
        passed: roundingPassed,
      });

      const snapshot = attachParitySnapshot({
        key: EXP_PARITY_05_KEY,
        caseId: EXP_PARITY_05_CASE_ID,
        description:
          'Four dateOfMonths price-component bands on one month-bounded FIFTEEN_MINUTES profile — invoice total must follow ' +
          'sum-then-round-once rounding, not the sum of per-period roundings',
        facts: {
          scenario: 'data-by-profile-rounding',
          billingType: 'STANDARD_BILLING',
          billingApplicationLevel: 'CONTRACT',
          applicationModelTypes: ['FOR_VOLUMES'],
          billingRunStatus: run.billingRunStatus,
          draftInvoiceCount: run.draftInvoiceIds.length,
          design: {
            periodCount: EXP_PARITY_05_DESIGN.periodCount,
            periodDays: EXP_PARITY_05_PERIOD_DAYS,
            kwhPerSlot: EXP_PARITY_05_KWH_PER_SLOT,
            unitPrice: EXP_PARITY_05_UNIT_PRICE,
            kwhPerPeriod: EXP_PARITY_05_DESIGN.kwhPerPeriod,
            exactAmountPerPeriod: EXP_PARITY_05_DESIGN.exactAmountPerPeriod,
            roundedAmountPerPeriod: EXP_PARITY_05_DESIGN.roundedAmountPerPeriod,
            sumThenRoundOnceDesign: EXP_PARITY_05_DESIGN.sumThenRoundOnce,
            roundEachPeriodThenSumDesign: EXP_PARITY_05_DESIGN.roundEachPeriodThenSum,
            strategyGapDesign: EXP_PARITY_05_DESIGN.strategyGap,
          },
          profile: {
            periodType: 'FIFTEEN_MINUTES',
            periodCount: EXP_PARITY_05_PERIOD_COUNT,
            periodDays: EXP_PARITY_05_PERIOD_DAYS,
            kwhPerSlot: EXP_PARITY_05_KWH_PER_SLOT,
            designedTotalKwh: chain.designedTotalKwh,
          },
          rounding: {
            computedPeriodCount: facts.computedPeriodCount,
            periods: facts.periods.map(parityPeriodFacts),
            unitPriceSource: facts.unitPriceSource,
            observedUnitPrices: facts.observedUnitPrices,
            detailedVolumeKwh: facts.detailedVolumeKwh,
            summaryVolumeKwh: facts.summaryVolumeKwh,
            volumeReconciliationDelta: facts.volumeReconciliationDelta,
            exactTotalAmount: facts.exactTotalAmount,
            sumThenRoundOnce: facts.sumThenRoundOnce,
            roundEachPeriodThenSum: facts.roundEachPeriodThenSum,
            roundEachRowThenSum: facts.roundEachRowThenSum,
            invoiceTotalExcludingVat: facts.invoiceTotalExcludingVat,
            reconciliationDelta: facts.reconciliationDelta,
            strategiesDistinguishable: facts.strategiesDistinguishable,
            matchedStrategy: facts.matchedStrategy,
          },
          invoice: {
            contractRole: facts.contractRole,
            invoiceType: facts.invoiceType,
            invoiceStatus: facts.invoiceStatus,
            invoiceDocumentType: facts.invoiceDocumentType,
          },
        },
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: EXP_PARITY_05_KEY,
          relevantEntityKeys: [
            'customer',
            'pod',
            'product',
            'productContract',
            'dataByProfiles',
            'billingRun',
            'invoice',
          ],
          snapshot: snapshot as unknown as Record<string, unknown>,
        });
      });
    });
  },
);
