/**
 * PDT-3072 (Dev API): Volume-change invoice correction with scale PCs + PC group versions.
 *
 * Jira Acceptance Criteria covered by this suite:
 * - AC1 + AC2 (same fix): debit SCALE lines split by PC-group version dates like main;
 *   volumes/values per component sum to the correct period total (NOT ×2).
 *
 * Test mapping:
 * - TC-BE-1 → AC1 + AC2 (mid-period clone PC group version; volume-change correction)
 *   (only test in this suite; AC3 / negative both-flags-false coverage dropped)
 *
 * Dev-automatable (current calendar month); not PreProd-date locked.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-2915-invoice-correction-deleted-bbp.fixtures.ts
 * - tests/billing/forVolumes/forVolumes.spec.ts
 * - tests/billing/correction/correctionCases.spec.ts
 * - tests/cursor/dev-volume-billing-two-compensations.spec.ts (cursor-test.fixtures + finalizeTestRunSummary)
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks
} from './shared/manual-verification-links.fixtures';
import { fetchPdt2599InvoiceDetailedRows } from './pdt-2599-service-contract.fixtures';
import {
  analyzePdt3072ScalePeriodSplit,
  assertPdt3072Ac2VolumesSumNoDouble,
  assertPdt3072DebitScalePeriodsMatchMainSplit,
  assertPdt3186PcGroupSummaryOnce,
  buildPdt3072AttachmentSummary,
  buildPdt3072BillingRunPreviewLink,
  buildPdt3072InvoiceDetailedDataPreviewLink,
  buildPdt3072PeriodComparisonTable,
  createPdt3072VolumeChangeCorrectionBillingRun,
  fetchPdt3072InvoiceSummaryRows,
  filterPdt3072DetailedRowsForPod,
  pollPdt3072CorrectionDraftInvoices,
  PDT_3072_PC_GROUP_NAME,
  runPdt3072SinglePodScaleVolumeCorrectionAfterInvoice,
  runPdt3072StandardBillingAndRealize,
  runPdt3072VolumeChangeScenarioPrechain,
  type Pdt3072Fx,
  type Pdt3072ScenarioContext
} from './pdt-3072-invoice-correction-volume-change-scale-pc-group.fixtures';

test.describe('[PDT-3072]: Invoice correction — volume change with scale PC group version', {
  tag: ['@billing', '@pdt-3072'],
}, () => {
  test(
    '[PDT-3072]: TC-BE-1 (AC1+AC2) – Invoice correction - problem when we have price change (new version of the PC group)',
    async ({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures, TestRunSummary }) => {
      test.setTimeout(30 * 60 * 1000);
      const fx: Pdt3072Fx = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };

      let scenario!: Pdt3072ScenarioContext;
      await test.step(
        'Precondition: customer → scale ZDISTJ/ZDISAH → PC group → 2 PODs/meters → product/contract → mid-period PC group version → scale energy data',
        async () => {
          scenario = await runPdt3072VolumeChangeScenarioPrechain(fx);
          TestRunSummary.registerPayload('precondition', {
            period: scenario.period,
            contractId: scenario.contractId,
            groupId: scenario.groupId,
            priceComponentIds: scenario.priceComponentIds,
            priceComponentIdsV2: scenario.priceComponentIdsV2,
            hasMidPeriodPcGroupVersion: scenario.hasMidPeriodPcGroupVersion,
            podIdentifiers: scenario.podIdentifiers,
            correctedPodIndex: scenario.correctedPodIndex,
            note:
              'TC-BE-1 covers AC1+AC2 (same fix). Jira title still says price change; ' +
              'flags are volumeChange=true / priceChange=false. PC group v2 uses clone scale PC ids.',
          });
        },
      );

      await test.step('Precondition: STANDARD FOR_VOLUMES billing run and realize main invoice (2 PODs)', async () => {
        const billed = await runPdt3072StandardBillingAndRealize(fx);
        scenario.originalInvoiceId = billed.originalInvoiceId;
        scenario.originalInvoiceNumber = billed.originalInvoiceNumber;
        TestRunSummary.registerPayload('mainInvoice', billed);
      });

      const correctedPodIdentifier = scenario.podIdentifiers[scenario.correctedPodIndex]!;

      let mainRowsForCorrectedPod!: Awaited<ReturnType<typeof fetchPdt2599InvoiceDetailedRows>>;
      const mainAnalysis = await test.step(
        'Snapshot: main invoice GET /invoice/detailed-data — SCALE periods split at PC group version (AC1 baseline)',
        async () => {
          const mainRows = await fetchPdt2599InvoiceDetailedRows(Request, scenario.originalInvoiceId);
          mainRowsForCorrectedPod = filterPdt3072DetailedRowsForPod(mainRows, correctedPodIdentifier);
          const analysis = analyzePdt3072ScalePeriodSplit(mainRowsForCorrectedPod, scenario.period);
          TestRunSummary.registerPayload('mainInvoicePeriods', {
            originalInvoiceId: scenario.originalInvoiceId,
            podIdentifier: correctedPodIdentifier,
            periodsByLabel: analysis.periodsByLabel,
            splitRespected: analysis.splitRespected,
            rowCount: mainRowsForCorrectedPod.length,
          });
          expect(
            analysis.splitRespected,
            `Main invoice must split ZDISTJ/ZDISAH at ${scenario.period.versionBoundary}; ` +
              `rows=${JSON.stringify(analysis.rows)}`,
          ).toBe(true);
          return analysis;
        },
      );

      await test.step(
        'PDT-3186: main invoice Summary — PC group type=GROUP appears exactly once',
        async () => {
          const summaryRows = await fetchPdt3072InvoiceSummaryRows(
            Request,
            scenario.originalInvoiceId,
          );
          try {
            const result = assertPdt3186PcGroupSummaryOnce(summaryRows, PDT_3072_PC_GROUP_NAME);
            TestRunSummary.recordCheck({
              check: 'PDT-3186: Main Summary PC group once',
              expectedResult: `Summary type=GROUP for "${PDT_3072_PC_GROUP_NAME}" count === 1`,
              actualResult: `As expected — count=${result.count}, invoiceId=${scenario.originalInvoiceId}`,
              passed: true,
            });
            TestRunSummary.registerPayload('mainInvoiceSummary3186', {
              invoiceId: scenario.originalInvoiceId,
              groupName: PDT_3072_PC_GROUP_NAME,
              groupRowCount: result.count,
              allGroupRows: summaryRows.filter((r) => String(r.type).toUpperCase() === 'GROUP'),
            });
          } catch (err) {
            TestRunSummary.recordCheck({
              check: 'PDT-3186: Main Summary PC group once',
              expectedResult: `Summary type=GROUP for "${PDT_3072_PC_GROUP_NAME}" count === 1`,
              actualResult: `Not as expected — invoiceId=${scenario.originalInvoiceId}, error=${err instanceof Error ? err.message : String(err)}`,
              passed: false,
            });
            throw err;
          }
        },
      );

      await test.step(
        'Precondition: scale VOLUME_CHANGE billing-by-scales on corrected POD only (after main realize)',
        async () => {
          await runPdt3072SinglePodScaleVolumeCorrectionAfterInvoice(
            fx,
            scenario.correctedPodIndex,
            scenario.originalInvoiceId,
            scenario.period,
          );
        },
      );

      const correctionBillingRunId = await test.step(
        'Create volume-change-only INVOICE_CORRECTION billing run (priceChange=false)',
        async () => createPdt3072VolumeChangeCorrectionBillingRun(fx),
      );

      const poll = await test.step('Start correction billing and poll draft invoices', async () =>
        pollPdt3072CorrectionDraftInvoices(Request, correctionBillingRunId),
      );

      expect(poll.debitNoteInvoiceId, 'Correction must produce a DEBIT_NOTE draft').toBeGreaterThan(0);
      if (!poll.creditNoteInvoiceId) {
        console.warn(
          `[PDT-3072] No CREDIT_NOTE draft paired with debit ${poll.debitNoteInvoiceId}; drafts=${JSON.stringify(poll.draftInvoices)}. Debit period assert remains the primary bug gate.`,
        );
      } else {
        expect(
          poll.draftInvoices.find((d) => d.id === poll.creditNoteInvoiceId)?.invoiceDocumentType,
          'Paired draft must be CREDIT_NOTE',
        ).toBe('CREDIT_NOTE');
      }
      expect(
        poll.draftInvoices.find((d) => d.id === poll.debitNoteInvoiceId)?.invoiceDocumentType,
        'Correction debit note must be DEBIT_NOTE',
      ).toBe('DEBIT_NOTE');

      let debitRowsForCorrectedPod!: Awaited<ReturnType<typeof fetchPdt2599InvoiceDetailedRows>>;
      const debitAnalysis = await test.step(
        'AC1: debit SCALE periods split like main at versionBoundary (not merged full-span on both versions)',
        async () => {
          const debitRows = await fetchPdt2599InvoiceDetailedRows(Request, poll.debitNoteInvoiceId);
          debitRowsForCorrectedPod = filterPdt3072DetailedRowsForPod(debitRows, correctedPodIdentifier);
          const analysis = analyzePdt3072ScalePeriodSplit(debitRowsForCorrectedPod, scenario.period);

          const otherPod = scenario.podIdentifiers.find((_, i) => i !== scenario.correctedPodIndex);
          const otherPodRows = otherPod
            ? filterPdt3072DetailedRowsForPod(debitRows, otherPod)
            : [];
          expect(
            otherPodRows.length,
            `Debit detailed-data must be empty for non-corrected POD ${otherPod}. Found ${otherPodRows.length} row(s): ` +
              `${JSON.stringify(otherPodRows.slice(0, 8))}`,
          ).toBe(0);

          try {
            assertPdt3072DebitScalePeriodsMatchMainSplit(mainAnalysis, analysis, scenario.period);
            TestRunSummary.recordCheck({
              check: 'AC1: Debit splits like main',
              expectedResult:
                'Debit ZDISTJ/ZDISAH periods split at PC group versionBoundary like main; ' +
                'no merged full-span lines on both old and new PC versions',
              actualResult:
                `As expected — originalInvoiceId=${scenario.originalInvoiceId}, debitNoteId=${poll.debitNoteInvoiceId}, ` +
                `mainPeriods=${JSON.stringify(mainAnalysis.periodsByLabel)}, ` +
                `debitPeriods=${JSON.stringify(analysis.periodsByLabel)}, ` +
                `debitFullPeriodDup=${analysis.hasDuplicateFullPeriodLines}`,
              passed: true,
            });
          } catch (err) {
            TestRunSummary.recordCheck({
              check: 'AC1: Debit splits like main',
              expectedResult:
                'Debit ZDISTJ/ZDISAH periods split at PC group versionBoundary like main; ' +
                'no merged full-span lines on both old and new PC versions',
              actualResult:
                `Not as expected — originalInvoiceId=${scenario.originalInvoiceId}, ` +
                `debitNoteId=${poll.debitNoteInvoiceId}, ` +
                `mainPeriods=${JSON.stringify(mainAnalysis.periodsByLabel)}, ` +
                `debitPeriods=${JSON.stringify(analysis.periodsByLabel)}, ` +
                `debitFullPeriodDup=${analysis.hasDuplicateFullPeriodLines}, ` +
                `error=${err instanceof Error ? err.message : String(err)}`,
              passed: false,
            });
            throw err;
          }

          return analysis;
        },
      );

      const volumeTotals = await test.step(
        'AC2: volumes/values per component sum to correct period total (NOT ×2)',
        async () => {
          try {
            const totals = assertPdt3072Ac2VolumesSumNoDouble(
              mainRowsForCorrectedPod,
              debitRowsForCorrectedPod,
            );
            TestRunSummary.recordCheck({
              check: 'AC2: Volumes sum no x2',
              expectedResult:
                'For each ZDISTJ/ZDISAH label, debit volume/value sum equals unique-period (correct) total; ' +
                'NOT ≈ 2× that total; no identical period rows ≥2 with doubled volumes. ' +
                'Intentional volume-change delta may raise debit vs main without approaching ×2.',
              actualResult:
                `As expected — mainByLabel=${JSON.stringify(totals.mainByLabel)}, ` +
                `debitByLabel=${JSON.stringify(totals.debitByLabel)}`,
              passed: true,
            });
            return totals;
          } catch (err) {
            TestRunSummary.recordCheck({
              check: 'AC2: Volumes sum no x2',
              expectedResult:
                'For each ZDISTJ/ZDISAH label, debit volume/value sum equals unique-period (correct) total; ' +
                'NOT ≈ 2× that total; no identical period rows ≥2 with doubled volumes.',
              actualResult: `Not as expected — error=${err instanceof Error ? err.message : String(err)}`,
              passed: false,
            });
            throw err;
          }
        },
      );

      await test.step(
        'PDT-3186: debit note Summary — PC group type=GROUP appears exactly once',
        async () => {
          const summaryRows = await fetchPdt3072InvoiceSummaryRows(Request, poll.debitNoteInvoiceId);
          try {
            const result = assertPdt3186PcGroupSummaryOnce(summaryRows, PDT_3072_PC_GROUP_NAME);
            TestRunSummary.recordCheck({
              check: 'PDT-3186: Debit Summary PC group once',
              expectedResult: `Summary type=GROUP for "${PDT_3072_PC_GROUP_NAME}" count === 1`,
              actualResult: `As expected — count=${result.count}, debitNoteId=${poll.debitNoteInvoiceId}`,
              passed: true,
            });
            TestRunSummary.registerPayload('debitInvoiceSummary3186', {
              invoiceId: poll.debitNoteInvoiceId,
              groupName: PDT_3072_PC_GROUP_NAME,
              groupRowCount: result.count,
              allGroupRows: summaryRows.filter((r) => String(r.type).toUpperCase() === 'GROUP'),
            });
          } catch (err) {
            TestRunSummary.recordCheck({
              check: 'PDT-3186: Debit Summary PC group once',
              expectedResult: `Summary type=GROUP for "${PDT_3072_PC_GROUP_NAME}" count === 1`,
              actualResult: `Not as expected — debitNoteId=${poll.debitNoteInvoiceId}, error=${err instanceof Error ? err.message : String(err)}`,
              passed: false,
            });
            throw err;
          }
        },
      );

      const periodTable = buildPdt3072PeriodComparisonTable(mainAnalysis, debitAnalysis);

      test.info().attach('[PDT-3072] TC-BE-1 AC1+AC2 main vs debit comparison', {
        body: JSON.stringify(
          buildPdt3072AttachmentSummary({
            tc: 'TC-BE-1',
            acceptanceCriteria: ['AC1', 'AC2'],
            originalInvoiceId: scenario.originalInvoiceId,
            originalInvoiceNumber: scenario.originalInvoiceNumber,
            correctionBillingRunId,
            debitNoteInvoiceId: poll.debitNoteInvoiceId,
            creditNoteInvoiceId: poll.creditNoteInvoiceId,
            correctedPodIdentifier,
            period: scenario.period,
            groupId: scenario.groupId,
            periodTable,
            volumeTotals,
            mainSplitRespected: mainAnalysis.splitRespected,
            debitSplitRespected: debitAnalysis.splitRespected,
            debitHasDuplicateFullPeriodLines: debitAnalysis.hasDuplicateFullPeriodLines,
            originalInvoiceDetailedDataPreviewUrl: buildPdt3072InvoiceDetailedDataPreviewLink(
              scenario.originalInvoiceId,
            ),
            debitNoteDetailedDataPreviewUrl: buildPdt3072InvoiceDetailedDataPreviewLink(
              poll.debitNoteInvoiceId,
            ),
            billingRunPreviewUrl: buildPdt3072BillingRunPreviewLink(correctionBillingRunId),
          }),
          null,
          2,
        ),
        contentType: 'application/json',
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3072',
          relevantEntityKeys: [
            'customer',
            'priceComponent',
            'groupOfPriceComponents',
            'product',
            'productContract',
            'pod',
            'meters',
            'dataByScales',
            'billingRun',
            'invoice',
          ],
          extraLinks: {
            ...buildProductContractTabLinks(scenario.contractId),
            billingRun: [buildPdt3072BillingRunPreviewLink(correctionBillingRunId)],
            invoiceDetailedData: [
              buildPdt3072InvoiceDetailedDataPreviewLink(scenario.originalInvoiceId),
              buildPdt3072InvoiceDetailedDataPreviewLink(poll.debitNoteInvoiceId),
            ],
          },
          snapshot: {
            tc: 'TC-BE-1',
            acceptanceCriteria: ['AC1', 'AC2'],
            originalInvoiceId: scenario.originalInvoiceId,
            correctionBillingRunId,
            debitNoteInvoiceId: poll.debitNoteInvoiceId,
            period: scenario.period,
            periodTable,
            volumeTotals,
            mainSplitRespected: mainAnalysis.splitRespected,
            debitSplitRespected: debitAnalysis.splitRespected,
            debitHasDuplicateFullPeriodLines: debitAnalysis.hasDuplicateFullPeriodLines,
          },
        });
      });
    },
  );
});

