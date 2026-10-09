/**
 * EXP-PARITY-02 — PreProd vs Experiment standard-billing parity (price component
 * date coverage vs the billing run calculated period).
 *
 * NAMING: `EXP-PARITY-02` is a PSEUDO-KEY placeholder. No Jira ticket exists
 * for this parity work yet; rename once a real key is assigned.
 *
 * HOW TO USE
 * ----------
 * Run twice (PreProd, then Experiment), changing ONLY `BASE_URL`, then diff the
 * `[EXP-PARITY-02] parity snapshot — …` attachments. The spec never branches on
 * environment — differences must surface in the snapshot data.
 *
 * Parallelism: `playwright.config.ts` sets `fullyParallel: true`. The
 * fifteen-minute Excel upload uses a fixed `15min.xlsx` in the working directory
 * (same as EXP-PARITY-01), so the three cases must not upload concurrently.
 * `mode: 'default'` (not `serial`) keeps later tests runnable after a failure —
 * serial mode would skip the rest of the file on the first red test.
 *
 * Reference spec(s):
 * - tests/cursor/EXP-PARITY-01-pulling-shared-pod-handover.spec.ts
 * - tests/cursor/SLR-18167-dev2-volumes-billing-replica.spec.ts
 * - tests/cursor/exp-parity-02-price-component-date-coverage.fixtures.ts
 */

import { expect, finalizeTestRunSummary, test } from './cursor-test.fixtures';
import { attachParitySnapshot } from './shared/exp-parity-snapshot.fixtures';
import {
  ALL_INVOICE_SLOTS,
  amountForPriceComponent,
  collectAllDetailRows,
  createForVolumesBillingRun,
  dateCoverage,
  downloadBillingErrorRows,
  EXP_PARITY_02_CASE_2_TITLE,
  EXP_PARITY_02_CASE_6_TITLE,
  EXP_PARITY_02_CASE_7_TITLE,
  EXP_PARITY_02_KEY,
  getDraftInvoiceIds,
  monthWindow,
  normalizedErrorShapes,
  rowCountForPriceComponent,
  runExpParityPrechain,
  SLOTS_WITH_NON_COVERING_PC_DATES,
  startBillingAndPollDraft,
  type DateCoverage,
  type ExpParity02Fx,
  type InvoiceSlot,
  type MonthWindow,
} from './exp-parity-02-price-component-date-coverage.fixtures';

const TEST_TIMEOUT_MS = 22 * 60 * 1000;

/** Full-month `dateOfMonths` coverage for a billing window. */
function fullMonthCoverage(window: MonthWindow): DateCoverage {
  return dateCoverage(window, 1, window.dayCount);
}

/** Coverage band on a month that does not overlap the billing calculated period. */
function nonOverlappingMonthCoverage(offsetFromBillingMonth: number): DateCoverage[] {
  const alien = monthWindow(offsetFromBillingMonth);
  return [fullMonthCoverage(alien)];
}

function slotFailsDateCoverage(slot: InvoiceSlot): boolean {
  return (SLOTS_WITH_NON_COVERING_PC_DATES as readonly string[]).includes(slot);
}

test.describe.configure({ mode: 'default', fullyParallel: false });

test.describe(`[${EXP_PARITY_02_KEY}]: Price component date coverage parity`, { tag: '@billing' }, () => {
  // ─────────────────────────────────────────────────────────────────────────
  // Case 7 — three PCs: two with correct dateOfMonths, one with wrong month
  // ─────────────────────────────────────────────────────────────────────────
  test(`[${EXP_PARITY_02_KEY}]: ${EXP_PARITY_02_CASE_7_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(TEST_TIMEOUT_MS);
    const fx: ExpParity02Fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

    const billingMonth = monthWindow(0);
    const wrongMonth = monthWindow(1); // M+1 — not in billing period
    const pcCovered1 = 'expParity02Case7Covered1';
    const pcCovered2 = 'expParity02Case7Covered2';
    const pcUncovered = 'expParity02Case7Uncovered';
    const unitExpression = '0.12';

    const pre = await test.step(
      'Precondition: chain with FIFTEEN_MINUTES profile (one month) and 3 PCs — 2 correct, 1 wrong dateOfMonths',
      async () =>
        runExpParityPrechain(fx, {
          scaleCodeCount: 1,
          priceComponents: [
            {
              displayName: pcCovered1,
              expression: unitExpression,
              slot: 'INVOICE_ONE',
              coverage: [dateCoverage(billingMonth, 1, 15)], // Covered: M days 1-15
            },
            {
              displayName: pcCovered2,
              expression: unitExpression,
              slot: 'INVOICE_TWO',
              coverage: [dateCoverage(billingMonth, 16, billingMonth.dayCount)], // Covered: M days 16-end
            },
            {
              displayName: pcUncovered,
              expression: unitExpression,
              slot: 'INVOICE_THREE',
              coverage: [dateCoverage(wrongMonth, 1, 15)], // WRONG: M+1 days 1-15 — not in billing period
            },
          ],
          podActivationDate: billingMonth.firstDay,
          profileFrom: billingMonth.firstDay,
          profileTo: billingMonth.lastDay, // Profile within one month — no cross-month
          profileTotalKwh: 1200,
        }),
    );

    TestRunSummary.registerPayload('prechain', {
      contractNumber: pre.contractNumber,
      priceComponents: { covered1: pcCovered1, covered2: pcCovered2, uncovered: pcUncovered },
      profileFrom: billingMonth.firstDay,
      profileTo: billingMonth.lastDay,
      coveredBands: ['M days 1–15', 'M days 16–end'],
      uncoveredBand: 'M+1 days 1–15 (wrong month PC)',
      slotCount: pre.slotCount,
    });

    const billing = await test.step('Action: CONTRACT FOR_VOLUMES billing run for month M', async () =>
      createForVolumesBillingRun(fx, pre.contractNumber, billingMonth.lastDay),
    );
    TestRunSummary.registerPayload('billingRun', billing.billingPayload);

    const poll = await test.step('Action: start-billing and poll until DRAFT or timeout', async () =>
      startBillingAndPollDraft(Request, billing.billingRunId),
    );

    const invoiceIds = await test.step('Read: draft invoices', async () => {
      const ids = await getDraftInvoiceIds(Request, billing.billingRunId);
      for (const id of ids) {
        Responses.invoice.push(id);
      }
      return ids;
    });

    const detailRows = await test.step('Read: invoice detailed-data settlement rows', async () =>
      collectAllDetailRows(Request, invoiceIds),
    );

    const errorRows = await test.step('Read: billing error protocol (xlsx)', async () =>
      downloadBillingErrorRows(Request, billing.billingRunId),
    );

    const amountCovered1 = amountForPriceComponent(detailRows, pcCovered1);
    const amountCovered2 = amountForPriceComponent(detailRows, pcCovered2);
    const rowsCovered1 = rowCountForPriceComponent(detailRows, pcCovered1);
    const rowsCovered2 = rowCountForPriceComponent(detailRows, pcCovered2);
    const rowsUncovered = rowCountForPriceComponent(detailRows, pcUncovered);
    const periodFroms = [...new Set(detailRows.map((row) => row.periodFrom))].sort();
    const periodTos = [...new Set(detailRows.map((row) => row.periodTo))].sort();
    const errorShapes = normalizedErrorShapes(errorRows);
    const hasUncoveredErrors = errorShapes.length > 0;

    await test.step('Assert: billing run reaches DRAFT despite uncovered PC errors', async () => {
      expect(
        poll.reachedDraft,
        `billing run must reach DRAFT (last status=${poll.status}, waited ${poll.waitedSeconds}s)`,
      ).toBe(true);
      expect(poll.status).toBe('DRAFT');
    });

    await test.step('Assert: two covered PCs produce priced settlement rows', async () => {
      expect(rowsCovered1, 'PC with M days 1-15 must produce priced rows').toBeGreaterThanOrEqual(1);
      expect(rowsCovered2, 'PC with M days 16-end must produce priced rows').toBeGreaterThanOrEqual(1);
      expect(amountCovered1 ?? 0, 'PC1 must bill a positive amount').toBeGreaterThan(0);
      expect(amountCovered2 ?? 0, 'PC2 must bill a positive amount').toBeGreaterThan(0);
    });

    await test.step('Assert: uncovered PC (wrong month) produces no rows or errors', async () => {
      // The PC with M+1 dates should either produce 0 rows or surface in error report
      const uncoveredFailed = rowsUncovered === 0 || hasUncoveredErrors;
      expect(uncoveredFailed, 'PC with wrong month dates must either fail or produce 0 rows').toBe(true);
    });

    TestRunSummary.recordCheck({
      check: '3 PCs with different dateOfMonths: 2 correct bill successfully, 1 wrong month fails',
      expectedResult:
        'Status DRAFT; covered PCs produce priced rows; uncovered PC (wrong month) produces 0 rows or errors.',
      actualResult:
        `status=${poll.status}; covered1Rows=${rowsCovered1}; covered2Rows=${rowsCovered2}; ` +
        `uncoveredRows=${rowsUncovered}; errorShapes=${JSON.stringify(errorShapes)}.`,
      passed:
        poll.reachedDraft &&
        rowsCovered1 >= 1 &&
        rowsCovered2 >= 1 &&
        (rowsUncovered === 0 || hasUncoveredErrors),
    });

    const snapshot = attachParitySnapshot({
      key: EXP_PARITY_02_KEY,
      caseId: 'case-7-three-pcs-two-correct-one-wrong',
      description:
        'Profile within month M; 3 PCs: PC1 covers M days 1-15, PC2 covers M days 16-end, PC3 covers M+1 days 1-15 (WRONG)',
      facts: {
        billingRunStatus: poll.status,
        reachedDraft: poll.reachedDraft,
        waitedSeconds: poll.waitedSeconds,
        draftInvoiceCount: invoiceIds.length,
        profile: {
          periodType: 'FIFTEEN_MINUTES',
          from: billingMonth.firstDay,
          to: billingMonth.lastDay,
          slotCount: pre.slotCount,
          totalKwh: 1200,
        },
        priceComponentCoverage: {
          covered1: { name: pcCovered1, dates: 'M days 1–15', rows: rowsCovered1, amount: amountCovered1 },
          covered2: { name: pcCovered2, dates: 'M days 16–end', rows: rowsCovered2, amount: amountCovered2 },
          uncovered: { name: pcUncovered, dates: 'M+1 days 1–15 (WRONG)', rows: rowsUncovered },
        },
        settlementPeriods: {
          periodFromValues: periodFroms,
          periodToValues: periodTos,
          unitExpression,
        },
        uncoveredPeriodErrors: {
          errorRowCount: errorRows.length,
          normalizedShapes: errorShapes,
        },
      },
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: EXP_PARITY_02_KEY,
        relevantEntityKeys: [
          'customer',
          'pod',
          'product',
          'productContract',
          'priceComponent',
          'dataByProfiles',
          'billingRun',
          'invoice',
        ],
        snapshot: snapshot as unknown as Record<string, unknown>,
      });
    });
  });

  // ─────────────────────────────────────────────────────────────────────────
  // Case 2 — four slots; middle two fail on date coverage
  // ─────────────────────────────────────────────────────────────────────────
  test(`[${EXP_PARITY_02_KEY}]: ${EXP_PARITY_02_CASE_2_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(TEST_TIMEOUT_MS);
    const fx: ExpParity02Fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

    const billingMonth = monthWindow(0);
    const alienOffset = -3;

    const slotConfigs = ALL_INVOICE_SLOTS.map((slot, index) => {
      const displayName = `expParity02Slot${index + 1}`;
      const expression = `0.${index + 1}${index + 1}`;
      const coverage = slotFailsDateCoverage(slot)
        ? nonOverlappingMonthCoverage(alienOffset)
        : [fullMonthCoverage(billingMonth)];
      return { displayName, expression, slot, coverage };
    });

    const pre = await test.step(
      'Precondition: four invoice slots, four scale codes, PCs differ only in dateOfMonths',
      async () =>
        runExpParityPrechain(fx, {
          scaleCodeCount: 4,
          priceComponents: slotConfigs,
          podActivationDate: billingMonth.firstDay,
          profileFrom: billingMonth.firstDay,
          profileTo: billingMonth.lastDay,
          profileTotalKwh: 960,
        }),
    );

    TestRunSummary.registerPayload('prechain', {
      contractNumber: pre.contractNumber,
      scaleCodeCount: 4,
      slotsWithNonCoveringPcDates: [...SLOTS_WITH_NON_COVERING_PC_DATES],
      slotConfigs: slotConfigs.map((cfg) => ({
        slot: cfg.slot,
        displayName: cfg.displayName,
        failsCoverage: slotFailsDateCoverage(cfg.slot),
      })),
    });

    const billing = await test.step('Action: CONTRACT FOR_VOLUMES billing run for the billing month', async () =>
      createForVolumesBillingRun(fx, pre.contractNumber, billingMonth.lastDay),
    );
    TestRunSummary.registerPayload('billingRun', billing.billingPayload);

    const poll = await test.step('Action: start-billing and poll until DRAFT or timeout', async () =>
      startBillingAndPollDraft(Request, billing.billingRunId),
    );

    const invoiceIds = await test.step('Read: draft invoices', async () => {
      const ids = await getDraftInvoiceIds(Request, billing.billingRunId);
      for (const id of ids) {
        Responses.invoice.push(id);
      }
      return ids;
    });

    const detailRows = await test.step('Read: invoice detailed-data per slot', async () =>
      collectAllDetailRows(Request, invoiceIds),
    );

    const errorRows = await test.step('Read: billing error protocol (xlsx)', async () =>
      downloadBillingErrorRows(Request, billing.billingRunId),
    );

    const errorShapes = normalizedErrorShapes(errorRows);

    const slotOutcomes: Record<string, Record<string, unknown>> = {};
    for (const cfg of slotConfigs) {
      const amount = amountForPriceComponent(detailRows, cfg.displayName);
      const rowCount = rowCountForPriceComponent(detailRows, cfg.displayName);
      const shouldFail = slotFailsDateCoverage(cfg.slot);
      slotOutcomes[cfg.slot] = {
        displayName: cfg.displayName,
        expectedToFail: shouldFail,
        pricedRowCount: rowCount,
        totalAmount: amount,
        producedInvoice: rowCount > 0 || amount !== null,
      };
    }

    const failingSlots = ALL_INVOICE_SLOTS.filter((slot) => slotFailsDateCoverage(slot));
    const passingSlots = ALL_INVOICE_SLOTS.filter((slot) => !slotFailsDateCoverage(slot));

    await test.step('Assert: billing run reaches DRAFT with partial slot failures', async () => {
      expect(poll.reachedDraft, `billing run must reach DRAFT (status=${poll.status})`).toBe(true);
      expect(poll.status).toBe('DRAFT');
    });

    await test.step('Assert: exactly two invoices for the covering slots', async () => {
      expect(
        invoiceIds.length,
        'only INVOICE_ONE and INVOICE_FOUR (non-failing slots) should produce draft invoices',
      ).toBe(2);
    });

    await test.step('Assert: uncovered slots are skipped; covering slots still bill', async () => {
      for (const slot of passingSlots) {
        const outcome = slotOutcomes[slot];
        expect(outcome?.pricedRowCount, `${slot} must be priced`).toBeGreaterThan(0);
      }
      for (const slot of failingSlots) {
        const outcome = slotOutcomes[slot];
        expect(outcome?.pricedRowCount, `${slot} must be skipped (0 priced rows)`).toBe(0);
        expect(outcome?.totalAmount, `${slot} must have no billed amount`).toBeNull();
      }
    });

    TestRunSummary.recordCheck({
      check: 'Non-covering PC dates skip their slots without stopping the other invoice slots',
      expectedResult:
        `Status DRAFT; exactly 2 draft invoices; slots ${failingSlots.join(', ')} skipped (0 rows); ` +
        `slots ${passingSlots.join(', ')} priced. Error-report rows are recorded, not required.`,
      actualResult:
        `status=${poll.status}; draftInvoices=${invoiceIds.length}; ` +
        `errorShapes=${JSON.stringify(errorShapes)}; slotOutcomes=${JSON.stringify(slotOutcomes)}.`,
      passed:
        poll.reachedDraft &&
        invoiceIds.length === 2 &&
        passingSlots.every((slot) => (slotOutcomes[slot]?.pricedRowCount ?? 0) > 0) &&
        failingSlots.every((slot) => (slotOutcomes[slot]?.pricedRowCount ?? 0) === 0),
    });

    const snapshot = attachParitySnapshot({
      key: EXP_PARITY_02_KEY,
      caseId: 'case-2-slot-date-coverage-partial-fail',
      description:
        'Four invoice slots with four scale codes; middle slots fail on dateOfMonths while others bill',
      facts: {
        billingRunStatus: poll.status,
        reachedDraft: poll.reachedDraft,
        draftInvoiceCount: invoiceIds.length,
        expectedDraftInvoiceCount: 2,
        slotsWithNonCoveringPcDates: [...SLOTS_WITH_NON_COVERING_PC_DATES],
        scaleCodeCount: 4,
        slotOutcomes,
        billingErrors: {
          errorRowCount: errorRows.length,
          normalizedShapes: errorShapes,
        },
      },
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: EXP_PARITY_02_KEY,
        relevantEntityKeys: [
          'customer',
          'pod',
          'product',
          'productContract',
          'priceComponent',
          'dataByProfiles',
          'billingRun',
          'invoice',
        ],
        snapshot: snapshot as unknown as Record<string, unknown>,
      });
    });
  });

  // ─────────────────────────────────────────────────────────────────────────
  // Case 6 — generation fails; run still settles on DRAFT
  // ─────────────────────────────────────────────────────────────────────────
  test(`[${EXP_PARITY_02_KEY}]: ${EXP_PARITY_02_CASE_6_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(TEST_TIMEOUT_MS);
    const fx: ExpParity02Fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

    const billingMonth = monthWindow(0);
    const alienMonth = monthWindow(-4);
    const pcName = 'expParity02Case6AlienMonthPc';

    const pre = await test.step(
      'Precondition: complete profile data but PC dates on a non-overlapping month',
      async () =>
        runExpParityPrechain(fx, {
          scaleCodeCount: 1,
          priceComponents: [
            {
              displayName: pcName,
              expression: '0.99',
              slot: 'INVOICE_ONE',
              coverage: [fullMonthCoverage(alienMonth)],
            },
          ],
          podActivationDate: billingMonth.firstDay,
          profileFrom: billingMonth.firstDay,
          profileTo: billingMonth.lastDay,
          profileTotalKwh: 800,
        }),
    );

    TestRunSummary.registerPayload('prechain', {
      contractNumber: pre.contractNumber,
      billingMonth: billingMonth.firstDay,
      pcCoverageMonth: alienMonth.firstDay,
      priceComponent: pcName,
    });

    const billing = await test.step('Action: CONTRACT FOR_VOLUMES billing run for the billing month', async () =>
      createForVolumesBillingRun(fx, pre.contractNumber, billingMonth.lastDay),
    );
    TestRunSummary.registerPayload('billingRun', billing.billingPayload);

    const poll = await test.step('Action: start-billing and poll until DRAFT or timeout', async () =>
      startBillingAndPollDraft(Request, billing.billingRunId),
    );

    const invoiceIds = await test.step('Read: draft invoices', async () => {
      const ids = await getDraftInvoiceIds(Request, billing.billingRunId);
      for (const id of ids) {
        Responses.invoice.push(id);
      }
      return ids;
    });

    const errorRows = await test.step('Read: billing error protocol (xlsx)', async () =>
      downloadBillingErrorRows(Request, billing.billingRunId),
    );

    const errorShapes = normalizedErrorShapes(errorRows);
    const detailRows =
      invoiceIds.length > 0
        ? await test.step('Read: invoice detailed-data (if any invoices exist)', async () =>
            collectAllDetailRows(Request, invoiceIds),
          )
        : [];

    const pricedAmount = amountForPriceComponent(detailRows, pcName);

    await test.step('Assert: terminal status is DRAFT — fail loudly if stuck IN_PROGRESS_DRAFT', async () => {
      if (poll.stillInProgress && poll.status === 'IN_PROGRESS_DRAFT') {
        throw new Error(
          `[${EXP_PARITY_02_KEY}] billing run ${billing.billingRunId} is still IN_PROGRESS_DRAFT ` +
            `after ${poll.waitedSeconds}s — expected terminal DRAFT after successful data preparation ` +
            'even when invoice generation fails.',
        );
      }
      expect(
        poll.reachedDraft,
        `billing run must reach DRAFT, not remain in progress (status=${poll.status}, waited ${poll.waitedSeconds}s)`,
      ).toBe(true);
      expect(poll.status).toBe('DRAFT');
      expect(poll.stillInProgress).toBe(false);
    });

    await test.step('Assert: no invoices priced when PC dates miss the calculated period', async () => {
      expect(invoiceIds.length, 'no draft invoices expected when the PC period is skipped').toBe(0);
      expect(pricedAmount, 'no priced amount expected').toBeNull();
    });

    TestRunSummary.recordCheck({
      check: 'Billing run reaches DRAFT when PC dates miss the calculated period',
      expectedResult:
        'Terminal status DRAFT (not IN_PROGRESS_DRAFT); 0 draft invoices. Error-report rows are recorded, not required.',
      actualResult:
        `status=${poll.status}; stillInProgress=${poll.stillInProgress}; ` +
        `waitedSeconds=${poll.waitedSeconds}; draftInvoices=${invoiceIds.length}; ` +
        `errorShapes=${JSON.stringify(errorShapes)}.`,
      passed:
        poll.reachedDraft &&
        poll.status === 'DRAFT' &&
        !poll.stillInProgress &&
        invoiceIds.length === 0,
    });

    const snapshot = attachParitySnapshot({
      key: EXP_PARITY_02_KEY,
      caseId: 'case-6-draft-after-generation-fail',
      description:
        'Data preparation succeeds but invoice generation fails because PC dates sit in a different month',
      facts: {
        dataPreparation: {
          profileComplete: true,
          bbpId: pre.bbpId,
          profileSlotCount: pre.slotCount,
        },
        generation: {
          draftInvoiceCount: invoiceIds.length,
          pricedAmount,
          errorRowCount: errorRows.length,
          normalizedErrorShapes: errorShapes,
        },
        terminalStatus: {
          status: poll.status,
          reachedDraft: poll.reachedDraft,
          stillInProgress: poll.stillInProgress,
          waitedSeconds: poll.waitedSeconds,
        },
        pcCoverageMonthOffset: -4,
        billingMonthOffset: 0,
      },
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: EXP_PARITY_02_KEY,
        relevantEntityKeys: [
          'customer',
          'pod',
          'product',
          'productContract',
          'priceComponent',
          'dataByProfiles',
          'billingRun',
        ],
        snapshot: snapshot as unknown as Record<string, unknown>,
      });
    });
  });
});
