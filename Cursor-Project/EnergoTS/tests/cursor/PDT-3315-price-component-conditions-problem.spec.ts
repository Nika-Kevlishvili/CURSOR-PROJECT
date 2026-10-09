/**
 * PDT-3315 — Price component conditions problem (bug repro).
 *
 * Asserts **correct** documented product behavior: when months-diff ∈ {6,12,18(,24)}
 * and contract=COMBINED + POD voltage ≠ HIGH, Control / OR / IN volume PCs must all
 * appear on the draft invoice (true condition → include PC).
 *
 * Expected to **FAIL** while PDT-3315 is open: runtime evaluates OR / `in(...)`
 * months conditions as `pc_condition_passed=-3` (replacement_text for months has no
 * trailing space → SQL `=6OR` / `diffin(`). Control `=6` alone still passes.
 *
 * Run on Dev:
 *   $env:BASE_URL="http://10.236.20.11:8091"
 *   npx playwright test --project=setup --workers=1
 *   npx playwright test tests/cursor/PDT-3315-price-component-conditions-problem.spec.ts --project=main
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3113-incorrect-interim-generation.spec.ts
 * - tests/cursor/pdt-3113-incorrect-interim-generation.fixtures.ts
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - jsons/payloads/create/productAndServices/priceSettlement.ts (formulaRequest.condition)
 *
 * Swagger (dev refresh): POST /price-components → PriceComponentFormulaRequest.condition (string).
 * No TC .md on disk — bug-only automation aligned to Jira reproduce steps.
 */

import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3315_CONDITION_CONTROL,
  PDT_3315_CONDITION_IN,
  PDT_3315_CONDITION_OR,
  PDT_3315_EXPR_CONTROL,
  PDT_3315_EXPR_IN,
  PDT_3315_EXPR_OR,
  PDT_3315_JIRA_KEY,
  PDT_3315_JIRA_TITLE,
  PDT_3315_TARGET_MONTHS_DIFF,
  invoiceIncludesPriceComponent,
  runPdt3315ConditionReproScenario,
  summarizePdt3315DetailedRows,
  type Pdt3315Fx,
} from './pdt-3315-price-component-conditions.fixtures';

test.describe(
  `[${PDT_3315_JIRA_KEY}]: Price component conditions problem`,
  { tag: ['@billing', '@pdt-3315', '@dev'] },
  () => {
    test(`[${PDT_3315_JIRA_KEY}]: ${PDT_3315_JIRA_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(25 * 60 * 1000);
      const fx: Pdt3315Fx = { Request, GeneratePayload, Responses, Endpoints };

      const scenario = await test.step(
        'Precondition + Action: COMBINED/LOW chain + 3 conditioned volume PCs + FOR_VOLUMES DRAFT',
        async () => runPdt3315ConditionReproScenario(fx),
      );

      const controlPc = scenario.priceComponents.find((p) => p.role === 'control')!;
      const orPc = scenario.priceComponents.find((p) => p.role === 'or')!;
      const inPc = scenario.priceComponents.find((p) => p.role === 'in')!;

      TestRunSummary.registerPayload('calendar', scenario.calendar);
      TestRunSummary.registerPayload('priceComponents', scenario.priceComponents);
      TestRunSummary.registerPayload('conditions', {
        control: PDT_3315_CONDITION_CONTROL,
        or: PDT_3315_CONDITION_OR,
        in: PDT_3315_CONDITION_IN,
        targetMonthsDiff: PDT_3315_TARGET_MONTHS_DIFF,
        expressions: {
          control: PDT_3315_EXPR_CONTROL,
          or: PDT_3315_EXPR_OR,
          in: PDT_3315_EXPR_IN,
        },
      });
      TestRunSummary.registerPayload('billing', {
        billingRunId: scenario.billingRunId,
        invoiceId: scenario.invoiceId,
        contractId: scenario.contractId,
        productId: scenario.productId,
        podId: scenario.podId,
      });

      const detailedSummary = summarizePdt3315DetailedRows(scenario.detailedRows);
      TestRunSummary.registerPayload('invoiceDetailedData', detailedSummary);

      const controlPresent = invoiceIncludesPriceComponent(scenario.detailedRows, controlPc);
      const orPresent = invoiceIncludesPriceComponent(scenario.detailedRows, orPc);
      const inPresent = invoiceIncludesPriceComponent(scenario.detailedRows, inPc);

      await test.step(
        'Assert: Control PC (months=6 only) present on draft invoice — baseline',
        async () => {
          TestRunSummary.recordCheck({
            check: 'Control PC included when months-diff=6',
            expectedResult:
              'Control volume PC (expression 10, condition months=6 only) appears on draft invoice detailed-data.',
            actualResult: controlPresent
              ? `As expected — found ${controlPc.displayName} on invoice ${scenario.invoiceId}. Rows: ${JSON.stringify(detailedSummary)}`
              : `Not as expected — Control PC missing on invoice ${scenario.invoiceId}. Rows: ${JSON.stringify(detailedSummary)}`,
            passed: controlPresent,
          });
          expect(
            controlPresent,
            `Control PC ${controlPc.displayName} must appear on draft invoice ${scenario.invoiceId} ` +
              `(activation=${scenario.calendar.activationDate}, maxEnd=${scenario.calendar.anchor.invoicePeriodTo}). ` +
              `detailed-data=${JSON.stringify(detailedSummary)}`,
          ).toBe(true);
        },
      );

      await test.step(
        'Assert: OR PC present when months∈{6,12,18} (documented product — expect FAIL while bug open)',
        async () => {
          TestRunSummary.recordCheck({
            check: 'OR months condition PC included',
            expectedResult:
              'OR-conditioned volume PC (expression 1111) appears on draft invoice when months-diff is 6.',
            actualResult: orPresent
              ? `As expected — found ${orPc.displayName} on invoice ${scenario.invoiceId}.`
              : `Not as expected — OR PC missing (PDT-3315: pc_condition_passed=-3 / =6OR). Invoice ${scenario.invoiceId}. Rows: ${JSON.stringify(detailedSummary)}`,
            passed: orPresent,
          });
          expect(
            orPresent,
            `OR PC ${orPc.displayName} must appear on draft invoice ${scenario.invoiceId} ` +
              `(documented: true OR months condition → include PC). ` +
              `While PDT-3315 is open this assertion is expected to FAIL. ` +
              `detailed-data=${JSON.stringify(detailedSummary)}`,
          ).toBe(true);
        },
      );

      await test.step(
        'Assert: IN PC present when months∈{6,12,18,24} (documented product — expect FAIL while bug open)',
        async () => {
          TestRunSummary.recordCheck({
            check: 'IN months condition PC included',
            expectedResult:
              'IN-conditioned volume PC (expression 2222) appears on draft invoice when months-diff is 6.',
            actualResult: inPresent
              ? `As expected — found ${inPc.displayName} on invoice ${scenario.invoiceId}.`
              : `Not as expected — IN PC missing (PDT-3315: pc_condition_passed=-3 / diffin(). Invoice ${scenario.invoiceId}. Rows: ${JSON.stringify(detailedSummary)}`,
            passed: inPresent,
          });
          expect(
            inPresent,
            `IN PC ${inPc.displayName} must appear on draft invoice ${scenario.invoiceId} ` +
              `(documented: true in(...) months condition → include PC). ` +
              `While PDT-3315 is open this assertion is expected to FAIL. ` +
              `detailed-data=${JSON.stringify(detailedSummary)}`,
          ).toBe(true);
        },
      );

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3315_JIRA_KEY,
          relevantEntityKeys: [
            'customer',
            'product',
            'priceComponent',
            'productContract',
            'pod',
            'billingRun',
            'invoice',
          ],
          extraLinks: buildProductContractTabLinks(scenario.contractId),
          snapshot: {
            billingRunId: scenario.billingRunId,
            invoiceId: scenario.invoiceId,
            contractId: scenario.contractId,
            activationDate: scenario.calendar.activationDate,
            openPeriod: scenario.calendar.openPeriod,
            controlPresent,
            orPresent,
            inPresent,
            priceComponentIds: scenario.priceComponents.map((p) => ({
              role: p.role,
              id: p.id,
              displayName: p.displayName,
            })),
          },
        });
      });
    });
  },
);
