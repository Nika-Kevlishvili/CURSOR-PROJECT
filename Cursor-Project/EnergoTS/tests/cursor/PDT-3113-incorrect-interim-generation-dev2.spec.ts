/**
 * PDT-3113 — IAP incorrect interim generation (bug repro).
 *
 * Mirrors Valeri's Test case shape (contracts 44543/44544): 2 PODs / 2 BGs → FOR_VOLUMES
 * on predecessor month → resigned contract B → INTERIM on the next OPEN month.
 * Additional GE cases in this file: (1) at-least-one POD of a multi-POD billing group,
 * (2) latest among several previous invoices, (3) 3-month previous-invoice window,
 * (4) negative: only out-of-window REAL previous invoice (must not be the interim basis),
 * (5) missing-previous-invoice checkbox checked, same POD resigned onto another contract,
 * with a 2-month-old invoice on that POD (must use that invoice, not the price component).
 * Accounting period ids/dates are **not** hardcoded (Test 1010/1011 are June/July and may be CLOSED).
 * Runtime uses two consecutive OPEN periods from GET /billing-run/accounting-period-available-list.
 * GE latest / 3-month tests OPEN extra CLOSED periods via PUT /accounting-period/{id} (no skip).
 *
 * Asserts **correct** product behavior (each interim based on the BG/POD-specific previous standard).
 * Expected to **FAIL** while PDT-3113 is open (both interims share the same previous standard).
 *
 * Dev2 copy of PDT-3113-incorrect-interim-generation.spec.ts.
 * The original file stays the Dev version. This copy includes the Dev2 resign,
 * missing-invoice checkbox, and same-POD previous-invoice cases.
 *
 * Run on Dev2:
 *   BASE_URL="https://devapps.energo-pro.bg/backend/phoenix2-dev"
 *   npx playwright test --project=setup --workers=1
 *   npx playwright test tests/cursor/PDT-3113-incorrect-interim-generation-dev2.spec.ts --project=main --workers=1
 *
 * Reference spec(s):
 * - tests/cursor/rps-pod-invoice-due-date.fixtures.ts
 * - tests/cursor/PDT-2750-missing-interim-invoice.spec.ts
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 * - tests/cursor/pdt-3013-separate-pod-reversal-offset.fixtures.ts
 *
 * Jira source: REST fallback (MCP unavailable or failed after retries).
 */

import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3113_IAP_PERCENT,
  PDT_3113_JIRA_KEY,
  PDT_3113_JIRA_TITLE,
  PDT_3113_PREVIOUS_INVOICE_MONTHS,
  PDT_3113_VALERI_REFERENCE,
  PDT_3113_VOLUME_POD1,
  PDT_3113_VOLUME_POD2,
  anchorFromOpenPeriod,
  assertInterimDoesNotUsePrevious,
  assertInterimPercentFromPrevious,
  assertInterimUsesBillingGroupSpecificPrevious,
  calendarForLatestPreviousInvoice,
  consecutiveOpenChain,
  createPredecessorContractTwoBillingGroups,
  createPredecessorSameBillingGroupTwoPodsBillOne,
  createResignedContractWithIap,
  entityId,
  fetchInvoiceDetailTypes,
  ensurePdt3113ExtendedAccountingPeriodsOpen,
  isWithinPreviousInvoiceWindow,
  postBillingByProfile,
  runInterimBillingOnContractB,
  runInterimBillingOnContractBAllowMissing,
  runStandardVolumeBillingTwoInvoices,
  twoMonthOldOpenPeriod,
  type Pdt3113Fx,
  type Pdt3113InvoiceSnapshot,
} from './pdt-3113-incorrect-interim-generation-dev2.fixtures';

test.describe(
  `[${PDT_3113_JIRA_KEY}]: IAP - Incorrect interim generation`,
  { tag: ['@billing', '@iap', '@pdt-3113', '@dev'] },
  () => {
    test(`[${PDT_3113_JIRA_KEY}]: ${PDT_3113_JIRA_TITLE}`, async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(35 * 60 * 1000);
      const fx: Pdt3113Fx = { Request, GeneratePayload, Responses, Endpoints };

      const prep = await test.step(
        'Precondition: contract A with 2 PODs on different billing groups + distinct volumes (Valeri calendar)',
        async () => createPredecessorContractTwoBillingGroups(fx),
      );
      TestRunSummary.registerPayload('contractA', {
        contractAId: prep.contractAId,
        pod1Id: prep.pod1Id,
        pod2Id: prep.pod2Id,
        billingGroupIds: prep.billingGroupIds,
        volumes: { pod1: PDT_3113_VOLUME_POD1, pod2: PDT_3113_VOLUME_POD2 },
        calendar: prep.calendar,
        valeriReference: PDT_3113_VALERI_REFERENCE,
      });

      let standards: Pdt3113InvoiceSnapshot[] = [];
      await test.step('Action: FOR_VOLUMES billing — expect 2 standard invoices with different amounts', async () => {
        standards = await runStandardVolumeBillingTwoInvoices(fx, prep.calendar.standardAnchor);
        TestRunSummary.registerPayload('standardInvoices', standards);
        TestRunSummary.recordCheck({
          check: 'Two standard invoices (one per billing group) with different amounts',
          expectedResult: `2 invoices, amounts differ (volumes ${PDT_3113_VOLUME_POD1} vs ${PDT_3113_VOLUME_POD2})`,
          actualResult: `As expected — ${standards
            .map((s) => `#${s.id} bg=${s.billingGroupId} amt=${s.totalAmountIncludingVat}`)
            .join('; ')}`,
          passed: true,
        });
      });

      const resign = await test.step(
        'Precondition: resigned contract B (same 2 PODs / 2 BGs) with IAP 50% PERCENT_FROM_PREVIOUS',
        async () => createResignedContractWithIap(fx, prep.calendar),
      );
      TestRunSummary.registerPayload('contractB', {
        contractBId: resign.contractBId,
        iapId: resign.iapId,
        calendar: prep.calendar,
        iapPercent: PDT_3113_IAP_PERCENT,
        valueType: 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT',
      });

      let interimBillingRunId = 0;
      let interims: Pdt3113InvoiceSnapshot[] = [];
      await test.step('Action: INTERIM_AND_ADVANCE_PAYMENT billing on contract B (next OPEN accounting period)', async () => {
        const contractBIndex = Responses.productContract.findIndex(
          (c) => entityId(c) === resign.contractBId,
        );
        expect(contractBIndex, 'Contract B index in Responses').toBeGreaterThanOrEqual(0);
        const result = await runInterimBillingOnContractB(
          fx,
          contractBIndex,
          prep.calendar.interimAnchor,
        );
        interimBillingRunId = result.billingRunId;
        interims = result.interimInvoices;
        TestRunSummary.registerPayload('interimInvoices', {
          billingRunId: interimBillingRunId,
          invoices: interims,
        });
      });

      await test.step(
        'Assert: each interim uses BG/POD-specific previous standard (correct behavior — fails while PDT-3113 open)',
        async () => {
          const verdict = assertInterimUsesBillingGroupSpecificPrevious(
            standards,
            interims,
            PDT_3113_IAP_PERCENT,
          );
          TestRunSummary.recordCheck({
            check:
              'Interim base invoice is POD/billing-group specific (not shared latest standard)',
            expectedResult: verdict.expectedResult,
            actualResult: verdict.actualResult,
            passed: verdict.passed,
          });
          TestRunSummary.registerPayload('pdt3113Assertion', verdict.details);

          expect(
            verdict.passed,
            `PDT-3113 regression repro — correct expected behavior not met.\n` +
              `Expected: ${verdict.expectedResult}\n` +
              `Actual: ${verdict.actualResult}`,
          ).toBe(true);

          const fromIds = interims.map((i) => i.interimCalculatedFromInvoiceId).filter((id) => id != null);
          if (fromIds.length === 2) {
            expect(
              fromIds[0],
              'Both interims must not share the same interimCalculatedFromInvoiceId when standards differ',
            ).not.toBe(fromIds[1]);
          }
        },
      );

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: PDT_3113_JIRA_KEY,
          relevantEntityKeys: [
            'customer',
            'pod',
            'product',
            'productContract',
            'billingRun',
            'invoice',
            'interim',
          ],
          extraLinks: {
            ...buildProductContractTabLinks(prep.contractAId),
            ...buildProductContractTabLinks(resign.contractBId),
          },
          snapshot: {
            contractAId: prep.contractAId,
            contractBId: resign.contractBId,
            interimBillingRunId,
            standardInvoiceIds: standards.map((s) => s.id),
            interimInvoiceIds: interims.map((i) => i.id),
            valeriReference: PDT_3113_VALERI_REFERENCE.ticketUrls,
            note:
              'Valeri pattern on two consecutive OPEN accounting periods. Asserts correct BG-specific previous-invoice selection; expected to FAIL until PDT-3113 is fixed.',
          },
        });
      });
    });

    test(
      `[${PDT_3113_JIRA_KEY}]: ${PDT_3113_JIRA_TITLE}(GE: at least one POD of the billing group)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(35 * 60 * 1000);
        const fx: Pdt3113Fx = { Request, GeneratePayload, Responses, Endpoints };

        const prep = await test.step(
          'Precondition: contract A with 2 PODs in one billing group; energy data only on POD #1',
          async () => createPredecessorSameBillingGroupTwoPodsBillOne(fx),
        );
        TestRunSummary.registerPayload('contractA', {
          contractAId: prep.contractAId,
          pod1Id: prep.pod1Id,
          pod2Id: prep.pod2Id,
          billingGroupIds: prep.billingGroupIds,
          billedPod: { podIndex: 0, volume: PDT_3113_VOLUME_POD2 },
          calendar: prep.calendar,
        });

        let standards: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: FOR_VOLUMES billing — expect 1 standard invoice (one billing group)', async () => {
          standards = await runStandardVolumeBillingTwoInvoices(fx, prep.calendar.standardAnchor, 1);
          TestRunSummary.registerPayload('standardInvoices', standards);
          TestRunSummary.recordCheck({
            check: 'One standard invoice for the shared billing group (only POD #1 had volume)',
            expectedResult: '1 invoice, amount > 0',
            actualResult: `As expected — ${standards
              .map((s) => `#${s.id} bg=${s.billingGroupId} amt=${s.totalAmountIncludingVat}`)
              .join('; ')}`,
            passed: true,
          });
        });

        const resign = await test.step(
          'Precondition: resigned contract B (same 2 PODs / 1 BG) with IAP 50% PERCENT_FROM_PREVIOUS',
          async () => createResignedContractWithIap(fx, prep.calendar, { billingGroupMode: 'same' }),
        );
        TestRunSummary.registerPayload('contractB', {
          contractBId: resign.contractBId,
          iapId: resign.iapId,
          iapPercent: PDT_3113_IAP_PERCENT,
          billingGroupMode: 'same',
        });

        let interimBillingRunId = 0;
        let interims: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: INTERIM_AND_ADVANCE_PAYMENT billing on contract B', async () => {
          const contractBIndex = Responses.productContract.findIndex(
            (c) => entityId(c) === resign.contractBId,
          );
          expect(contractBIndex, 'Contract B index in Responses').toBeGreaterThanOrEqual(0);
          const result = await runInterimBillingOnContractB(
            fx,
            contractBIndex,
            prep.calendar.interimAnchor,
            1,
          );
          interimBillingRunId = result.billingRunId;
          interims = result.interimInvoices;
          TestRunSummary.registerPayload('interimInvoices', {
            billingRunId: interimBillingRunId,
            invoices: interims,
          });
        });

        await test.step(
          'Assert: interim uses previous invoice that contains at least one POD of the current billing group',
          async () => {
            expect(standards.length, 'Need 1 standard').toBe(1);
            expect(interims.length, 'Need 1 interim').toBe(1);
            const verdict = assertInterimPercentFromPrevious(
              standards[0],
              interims[0],
              PDT_3113_IAP_PERCENT,
            );
            TestRunSummary.recordCheck({
              check:
                'GE at-least-one POD: previous invoice covering POD #1 is the interim basis for the 2-POD billing group',
              expectedResult: verdict.expectedResult,
              actualResult: verdict.actualResult,
              passed: verdict.passed,
            });
            expect(verdict.passed, `${verdict.expectedResult}\n${verdict.actualResult}`).toBe(true);
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: PDT_3113_JIRA_KEY,
            relevantEntityKeys: [
              'customer',
              'pod',
              'product',
              'productContract',
              'billingRun',
              'invoice',
              'interim',
            ],
            extraLinks: {
              ...buildProductContractTabLinks(prep.contractAId),
              ...buildProductContractTabLinks(resign.contractBId),
            },
            snapshot: {
              geCase: 'at-least-one-pod',
              contractAId: prep.contractAId,
              contractBId: resign.contractBId,
              interimBillingRunId,
              standardInvoiceIds: standards.map((s) => s.id),
              interimInvoiceIds: interims.map((i) => i.id),
            },
          });
        });
      },
    );

    test(
      `[${PDT_3113_JIRA_KEY}]: ${PDT_3113_JIRA_TITLE}(GE: latest previous invoice)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(45 * 60 * 1000);
        const fx: Pdt3113Fx = { Request, GeneratePayload, Responses, Endpoints };
        const resolved = await test.step(
          'Precondition: OPEN extra accounting periods (3 consecutive + out-of-window)',
          async () => ensurePdt3113ExtendedAccountingPeriodsOpen(Request),
        );
        const latestCal = calendarForLatestPreviousInvoice(resolved.openPeriods);
        const chain = consecutiveOpenChain(resolved.openPeriods);
        const listed =
          chain.map((p) => `ap=${p.id} ${p.startDate}→${p.endDate}`).join('; ') || 'none';
        TestRunSummary.registerPayload('openPeriods', {
          openPeriods: resolved.openPeriods,
          consecutiveChain: chain,
        });
        TestRunSummary.recordCheck({
          check: 'Environment has 3 consecutive OPEN accounting periods for GE latest',
          expectedResult: '≥3 consecutive OPEN (older standard, newer standard, interim)',
          actualResult: `${chain.length} (${listed})`,
          passed: latestCal != null,
        });
        expect(
          latestCal,
          `GE latest needs 3 consecutive OPEN after OPEN precondition. Consecutive now: ${chain.length} (${listed}).`,
        ).toBeTruthy();

        const prep = await test.step(
          'Precondition: contract A (2 PODs / 1 BG) + older-month volume on POD #1',
          async () =>
            createPredecessorSameBillingGroupTwoPodsBillOne(fx, {
              calendar: latestCal!.calendar,
              volume: PDT_3113_VOLUME_POD2,
              volumeAnchor: latestCal!.olderStandardAnchor,
            }),
        );
        let olderStandards: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: FOR_VOLUMES on older OPEN month', async () => {
          olderStandards = await runStandardVolumeBillingTwoInvoices(
            fx,
            latestCal!.olderStandardAnchor,
            1,
          );
        });
        await test.step(
          `Precondition: billing-by-profile POD #1 newer month volume=${PDT_3113_VOLUME_POD1}`,
          async () => postBillingByProfile(fx, 0, latestCal!.newerStandardAnchor, PDT_3113_VOLUME_POD1),
        );
        let newerStandards: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: FOR_VOLUMES on newer OPEN month (must be the interim basis)', async () => {
          newerStandards = await runStandardVolumeBillingTwoInvoices(
            fx,
            latestCal!.newerStandardAnchor,
            1,
          );
        });
        TestRunSummary.registerPayload('standardInvoices', { olderStandards, newerStandards });

        const resign = await test.step(
          'Precondition: resigned contract B with IAP 50% PERCENT_FROM_PREVIOUS',
          async () =>
            createResignedContractWithIap(fx, latestCal!.calendar, { billingGroupMode: 'same' }),
        );

        let interimBillingRunId = 0;
        let interims: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: INTERIM on newest OPEN month', async () => {
          const contractBIndex = Responses.productContract.findIndex(
            (c) => entityId(c) === resign.contractBId,
          );
          expect(contractBIndex, 'Contract B index in Responses').toBeGreaterThanOrEqual(0);
          const result = await runInterimBillingOnContractB(
            fx,
            contractBIndex,
            latestCal!.calendar.interimAnchor,
            1,
          );
          interimBillingRunId = result.billingRunId;
          interims = result.interimInvoices;
        });

        await test.step('Assert: interim uses the latest previous invoice, not the older one', async () => {
          expect(olderStandards.length).toBe(1);
          expect(newerStandards.length).toBe(1);
          expect(interims.length).toBe(1);
          const verdict = assertInterimPercentFromPrevious(
            newerStandards[0],
            interims[0],
            PDT_3113_IAP_PERCENT,
          );
          const usedOlder = assertInterimPercentFromPrevious(
            olderStandards[0],
            interims[0],
            PDT_3113_IAP_PERCENT,
          );
          TestRunSummary.recordCheck({
            check: 'GE latest: interim basis is the newer standard, not the older one',
            expectedResult: verdict.expectedResult,
            actualResult: `${verdict.actualResult}; olderMatch=${usedOlder.passed}`,
            passed: verdict.passed && !usedOlder.passed,
          });
          expect(
            verdict.passed && !usedOlder.passed,
            `Expected latest (newer) previous invoice as basis.\nNewer: ${verdict.actualResult}\nOlder match (must be false): ${usedOlder.passed}`,
          ).toBe(true);
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: PDT_3113_JIRA_KEY,
            relevantEntityKeys: [
              'customer',
              'pod',
              'product',
              'productContract',
              'billingRun',
              'invoice',
              'interim',
            ],
            extraLinks: {
              ...buildProductContractTabLinks(prep.contractAId),
              ...buildProductContractTabLinks(resign.contractBId),
            },
            snapshot: {
              geCase: 'latest-previous-invoice',
              contractAId: prep.contractAId,
              contractBId: resign.contractBId,
              interimBillingRunId,
              olderStandardIds: olderStandards.map((s) => s.id),
              newerStandardIds: newerStandards.map((s) => s.id),
              interimInvoiceIds: interims.map((i) => i.id),
            },
          });
        });
      },
    );

    test(
      `[${PDT_3113_JIRA_KEY}]: ${PDT_3113_JIRA_TITLE}(GE: 3-month previous-invoice window)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(45 * 60 * 1000);
        const fx: Pdt3113Fx = { Request, GeneratePayload, Responses, Endpoints };
        const calendar = await test.step(
          'Precondition: OPEN extra accounting periods (3 consecutive + out-of-window)',
          async () => ensurePdt3113ExtendedAccountingPeriodsOpen(Request),
        );
        const asOf = calendar.interimAnchor.invoiceDate;
        const stale = calendar.openPeriods.filter(
          (p) => !isWithinPreviousInvoiceWindow(p.endDate, asOf, PDT_3113_PREVIOUS_INVOICE_MONTHS),
        );
        const listed = calendar.openPeriods
          .map((p) => `ap=${p.id} ${p.startDate}→${p.endDate}`)
          .join('; ');
        TestRunSummary.registerPayload('previousInvoiceWindow', {
          asOf,
          months: PDT_3113_PREVIOUS_INVOICE_MONTHS,
          openPeriods: calendar.openPeriods,
          staleOpenPeriods: stale,
        });
        TestRunSummary.recordCheck({
          check: 'Environment has an OPEN period outside the 3-month previous-invoice window',
          expectedResult: `≥1 OPEN period ending before ${asOf} minus ${PDT_3113_PREVIOUS_INVOICE_MONTHS} months`,
          actualResult: stale.length === 0 ? `none; OPEN=${listed}` : stale.map((p) => `ap=${p.id}`).join(', '),
          passed: stale.length > 0,
        });
        expect(
          stale.length,
          `GE 3-month window needs an OPEN period ending before ${asOf} minus ${PDT_3113_PREVIOUS_INVOICE_MONTHS} months after OPEN precondition. OPEN now: ${listed}.`,
        ).toBeGreaterThan(0);

        const stalePeriod = stale[0];
        const staleAnchor = anchorFromOpenPeriod(
          stalePeriod,
          `OPEN stale (out of 3-month window) ap=${stalePeriod.id}`,
        );
        const activation =
          stalePeriod.startDate < calendar.contractA.podActivationDate
            ? stalePeriod.startDate
            : calendar.contractA.podActivationDate;
        const prepCalendar = {
          ...calendar,
          contractA: { podActivationDate: activation },
        };

        const prep = await test.step(
          'Precondition: contract A (2 PODs / 1 BG) + stale-month volume on POD #1',
          async () =>
            createPredecessorSameBillingGroupTwoPodsBillOne(fx, {
              calendar: prepCalendar,
              volume: PDT_3113_VOLUME_POD2,
              volumeAnchor: staleAnchor,
            }),
        );
        let staleStandards: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: FOR_VOLUMES on stale (out-of-window) OPEN month', async () => {
          staleStandards = await runStandardVolumeBillingTwoInvoices(fx, staleAnchor, 1);
        });
        await test.step(
          `Precondition: billing-by-profile POD #1 in-window month volume=${PDT_3113_VOLUME_POD1}`,
          async () => postBillingByProfile(fx, 0, calendar.standardAnchor, PDT_3113_VOLUME_POD1),
        );
        let inWindowStandards: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: FOR_VOLUMES on in-window standard month (must be the interim basis)', async () => {
          inWindowStandards = await runStandardVolumeBillingTwoInvoices(fx, calendar.standardAnchor, 1);
        });
        TestRunSummary.registerPayload('standardInvoices', { staleStandards, inWindowStandards });

        const resign = await test.step(
          'Precondition: resigned contract B with IAP 50% PERCENT_FROM_PREVIOUS',
          async () => createResignedContractWithIap(fx, prepCalendar, { billingGroupMode: 'same' }),
        );

        let interimBillingRunId = 0;
        let interims: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: INTERIM on next OPEN month', async () => {
          const contractBIndex = Responses.productContract.findIndex(
            (c) => entityId(c) === resign.contractBId,
          );
          expect(contractBIndex, 'Contract B index in Responses').toBeGreaterThanOrEqual(0);
          const result = await runInterimBillingOnContractB(
            fx,
            contractBIndex,
            calendar.interimAnchor,
            1,
          );
          interimBillingRunId = result.billingRunId;
          interims = result.interimInvoices;
        });

        await test.step('Assert: interim ignores the out-of-window previous invoice', async () => {
          expect(staleStandards.length).toBe(1);
          expect(inWindowStandards.length).toBe(1);
          expect(interims.length).toBe(1);
          const verdict = assertInterimPercentFromPrevious(
            inWindowStandards[0],
            interims[0],
            PDT_3113_IAP_PERCENT,
          );
          const usedStale = assertInterimPercentFromPrevious(
            staleStandards[0],
            interims[0],
            PDT_3113_IAP_PERCENT,
          );
          TestRunSummary.recordCheck({
            check: 'GE 3-month window: interim basis is in-window standard, not stale',
            expectedResult: verdict.expectedResult,
            actualResult: `${verdict.actualResult}; staleMatch=${usedStale.passed}`,
            passed: verdict.passed && !usedStale.passed,
          });
          expect(
            verdict.passed && !usedStale.passed,
            `Expected in-window previous invoice as basis.\nIn-window: ${verdict.actualResult}\nStale match (must be false): ${usedStale.passed}`,
          ).toBe(true);
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: PDT_3113_JIRA_KEY,
            relevantEntityKeys: [
              'customer',
              'pod',
              'product',
              'productContract',
              'billingRun',
              'invoice',
              'interim',
            ],
            extraLinks: {
              ...buildProductContractTabLinks(prep.contractAId),
              ...buildProductContractTabLinks(resign.contractBId),
            },
            snapshot: {
              geCase: 'three-month-window',
              contractAId: prep.contractAId,
              contractBId: resign.contractBId,
              interimBillingRunId,
              staleStandardIds: staleStandards.map((s) => s.id),
              inWindowStandardIds: inWindowStandards.map((s) => s.id),
              interimInvoiceIds: interims.map((i) => i.id),
            },
          });
        });
      },
    );

    test(
      `[${PDT_3113_JIRA_KEY}]: ${PDT_3113_JIRA_TITLE}(GE: 3-month window negative — only out-of-window REAL)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(45 * 60 * 1000);
        const fx: Pdt3113Fx = { Request, GeneratePayload, Responses, Endpoints };
        const calendar = await test.step(
          'Precondition: OPEN extra accounting periods (3 consecutive + out-of-window)',
          async () => ensurePdt3113ExtendedAccountingPeriodsOpen(Request),
        );
        const asOf = calendar.interimAnchor.invoiceDate;
        const stale = calendar.openPeriods.filter(
          (p) => !isWithinPreviousInvoiceWindow(p.endDate, asOf, PDT_3113_PREVIOUS_INVOICE_MONTHS),
        );
        const listed = calendar.openPeriods
          .map((p) => `ap=${p.id} ${p.startDate}→${p.endDate}`)
          .join('; ');
        TestRunSummary.registerPayload('previousInvoiceWindow', {
          asOf,
          months: PDT_3113_PREVIOUS_INVOICE_MONTHS,
          openPeriods: calendar.openPeriods,
          staleOpenPeriods: stale,
          geCase: 'three-month-window-negative-only-stale-real',
        });
        TestRunSummary.recordCheck({
          check: 'Environment has an OPEN period outside the 3-month previous-invoice window',
          expectedResult: `≥1 OPEN period ending before ${asOf} minus ${PDT_3113_PREVIOUS_INVOICE_MONTHS} months`,
          actualResult: stale.length === 0 ? `none; OPEN=${listed}` : stale.map((p) => `ap=${p.id}`).join(', '),
          passed: stale.length > 0,
        });
        expect(
          stale.length,
          `GE 3-month negative needs an OPEN period ending before ${asOf} minus ${PDT_3113_PREVIOUS_INVOICE_MONTHS} months after OPEN precondition. OPEN now: ${listed}.`,
        ).toBeGreaterThan(0);

        const stalePeriod = stale[0];
        const staleAnchor = anchorFromOpenPeriod(
          stalePeriod,
          `OPEN stale (out of 3-month window) ap=${stalePeriod.id}`,
        );
        const activation =
          stalePeriod.startDate < calendar.contractA.podActivationDate
            ? stalePeriod.startDate
            : calendar.contractA.podActivationDate;
        const prepCalendar = {
          ...calendar,
          contractA: { podActivationDate: activation },
        };

        const prep = await test.step(
          'Precondition: contract A (2 PODs / 1 BG) + ONLY stale-month volume on POD #1 (no in-window REAL)',
          async () =>
            createPredecessorSameBillingGroupTwoPodsBillOne(fx, {
              calendar: prepCalendar,
              volume: PDT_3113_VOLUME_POD2,
              volumeAnchor: staleAnchor,
            }),
        );
        let staleStandards: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: FOR_VOLUMES on stale (out-of-window) OPEN month — only REAL previous', async () => {
          staleStandards = await runStandardVolumeBillingTwoInvoices(fx, staleAnchor, 1);
        });
        TestRunSummary.registerPayload('standardInvoices', {
          staleStandards,
          inWindowStandards: [],
        });

        const resign = await test.step(
          'Precondition: resigned contract B with IAP 50% PERCENT_FROM_PREVIOUS',
          async () => createResignedContractWithIap(fx, prepCalendar, { billingGroupMode: 'same' }),
        );

        let interimBillingRunId = 0;
        let interims: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: INTERIM on next OPEN month (no in-window previous REAL)', async () => {
          const contractBIndex = Responses.productContract.findIndex(
            (c) => entityId(c) === resign.contractBId,
          );
          expect(contractBIndex, 'Contract B index in Responses').toBeGreaterThanOrEqual(0);
          const result = await runInterimBillingOnContractBAllowMissing(
            fx,
            contractBIndex,
            calendar.interimAnchor,
          );
          interimBillingRunId = result.billingRunId;
          interims = result.interimInvoices;
        });

        await test.step(
          'Assert: out-of-window REAL must not be the PERCENT_FROM_PREVIOUS basis',
          async () => {
            expect(staleStandards.length, 'Need the out-of-window REAL standard').toBe(1);
            const verdict = assertInterimDoesNotUsePrevious(
              staleStandards[0],
              interims[0],
              PDT_3113_IAP_PERCENT,
            );
            TestRunSummary.recordCheck({
              check:
                'GE 3-month negative: only out-of-window REAL exists — must not be used as interim basis',
              expectedResult: verdict.expectedResult,
              actualResult: verdict.actualResult,
              passed: verdict.passed,
            });
            expect(verdict.passed, `${verdict.expectedResult}\n${verdict.actualResult}`).toBe(true);
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: PDT_3113_JIRA_KEY,
            relevantEntityKeys: [
              'customer',
              'pod',
              'product',
              'productContract',
              'billingRun',
              'invoice',
              'interim',
            ],
            extraLinks: {
              ...buildProductContractTabLinks(prep.contractAId),
              ...buildProductContractTabLinks(resign.contractBId),
            },
            snapshot: {
              geCase: 'three-month-window-negative-only-stale-real',
              contractAId: prep.contractAId,
              contractBId: resign.contractBId,
              interimBillingRunId,
              staleStandardIds: staleStandards.map((s) => s.id),
              inWindowStandardIds: [],
              interimInvoiceIds: interims.map((i) => i.id),
            },
          });
        });
      },
    );

    test(
      `[${PDT_3113_JIRA_KEY}]: ${PDT_3113_JIRA_TITLE}(GE: missing previous invoice checked, same POD on another contract)`,
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(45 * 60 * 1000);
        const fx: Pdt3113Fx = { Request, GeneratePayload, Responses, Endpoints };
        const calendar = await test.step(
          'Precondition: OPEN periods so a 2-month-old month is inside the previous-invoice window',
          async () => ensurePdt3113ExtendedAccountingPeriodsOpen(Request),
        );
        const twoMonth = twoMonthOldOpenPeriod(calendar);
        const twoMonthAnchor = anchorFromOpenPeriod(
          twoMonth,
          `OPEN 2-month-old previous invoice ap=${twoMonth.id} ${twoMonth.startDate}→${twoMonth.endDate}`,
        );
        const activation =
          twoMonth.startDate < calendar.contractA.podActivationDate
            ? twoMonth.startDate
            : calendar.contractA.podActivationDate;
        const prepCalendar = { ...calendar, contractA: { podActivationDate: activation } };
        TestRunSummary.registerPayload('twoMonthPreviousInvoice', {
          period: twoMonth,
          interim: calendar.interimAnchor,
          missingInvoice: true,
          reference: 'staging src/tests/billing/dataPreparation/interimDataPrep.ts REG_978',
        });

        const prep = await test.step(
          'Precondition: contract A (other billing group) + volume in the 2-month-old OPEN month',
          async () =>
            createPredecessorSameBillingGroupTwoPodsBillOne(fx, {
              calendar: prepCalendar,
              volume: PDT_3113_VOLUME_POD2,
              volumeAnchor: twoMonthAnchor,
            }),
        );
        let previousOnOtherContract: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: FOR_VOLUMES on the 2-month-old month (different contract)', async () => {
          previousOnOtherContract = await runStandardVolumeBillingTwoInvoices(fx, twoMonthAnchor, 1);
        });
        expect(previousOnOtherContract.length).toBe(1);

        const second = await test.step(
          'Precondition: resign the same PODs onto contract B; missing-invoice checkbox checked',
          async () =>
            createResignedContractWithIap(fx, prepCalendar, {
              billingGroupMode: 'same',
              missingInvoice: true,
            }),
        );
        expect(second.contractBId).not.toBe(prep.contractAId);
        expect(
          previousOnOtherContract[0].podIdentifiers.length,
          'Previous invoice must name the POD that contract B reuses',
        ).toBeGreaterThan(0);
        TestRunSummary.registerPayload('contracts', {
          contractAId: prep.contractAId,
          contractBId: second.contractBId,
          samePods: previousOnOtherContract[0].podIdentifiers,
          previousInvoiceId: previousOnOtherContract[0].id,
          previousAmount: previousOnOtherContract[0].totalAmountIncludingVat,
          missingInvoice: true,
        });

        let interimBillingRunId = 0;
        let interims: Pdt3113InvoiceSnapshot[] = [];
        await test.step('Action: INTERIM on the next OPEN month', async () => {
          const contractBIndex = Responses.productContract.findIndex(
            (c) => entityId(c) === second.contractBId,
          );
          expect(contractBIndex, 'Second contract index in Responses').toBeGreaterThanOrEqual(0);
          const result = await runInterimBillingOnContractB(
            fx,
            contractBIndex,
            calendar.interimAnchor,
            1,
          );
          interimBillingRunId = result.billingRunId;
          interims = result.interimInvoices;
        });

        await test.step(
          'Assert: same POD on the other contract — percent from that invoice, not the price component',
          async () => {
            expect(interims.length).toBe(1);
            const detailTypes = await fetchInvoiceDetailTypes(Request, interims[0].id);
            const verdict = assertInterimPercentFromPrevious(
              previousOnOtherContract[0],
              interims[0],
              PDT_3113_IAP_PERCENT,
            );
            const usedPriceComponent = detailTypes.some((t) => t.includes('PRICE_COMPONENT'));
            const usedPrevious = detailTypes.some((t) =>
              t.includes('PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT'),
            );
            const passed = verdict.passed && !usedPriceComponent;
            const actualResult = passed
              ? `${verdict.actualResult}; detailTypes=${detailTypes.join(',') || '(none)'}`
              : `Not as expected — did not use the 2-month-old invoice #${previousOnOtherContract[0].id}. ${verdict.actualResult}; detailTypes=${detailTypes.join(',') || '(none)'}; priceComponentFallback=${usedPriceComponent}; percentDetail=${usedPrevious}`;
            TestRunSummary.recordCheck({
              check:
                'Missing-invoice checkbox checked: 2-month invoice on the same POD but the other contract is the percent basis',
              expectedResult: `Interim amount is ${PDT_3113_IAP_PERCENT}% of invoice #${previousOnOtherContract[0].id} (${previousOnOtherContract[0].totalAmountIncludingVat}) on contract ${prep.contractAId}, PODs [${previousOnOtherContract[0].podIdentifiers.join(', ')}]. Amount is not the price-component fallback (10 + VAT).`,
              actualResult,
              passed,
            });
            expect(passed, `${verdict.expectedResult}\n${actualResult}`).toBe(true);
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: PDT_3113_JIRA_KEY,
            relevantEntityKeys: [
              'customer',
              'pod',
              'product',
              'productContract',
              'billingRun',
              'invoice',
              'interim',
            ],
            extraLinks: {
              ...buildProductContractTabLinks(prep.contractAId),
              ...buildProductContractTabLinks(second.contractBId),
            },
            snapshot: {
              geCase: 'missing-invoice-checked-same-pod-other-contract',
              contractAId: prep.contractAId,
              contractBId: second.contractBId,
              interimBillingRunId,
              previousInvoiceId: previousOnOtherContract[0].id,
              interimInvoiceIds: interims.map((i) => i.id),
            },
          });
        });
      },
    );
  },
);
