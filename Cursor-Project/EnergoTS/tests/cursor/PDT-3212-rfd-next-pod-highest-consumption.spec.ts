/**
 * PDT-3212 — Request for disconnection: next POD with highest consumption.
 *
 * Highest consumption YES is calculated ONLY among PODs listed on THIS RFD for
 * that billing group. Off-list PODs (disconnected, already on executed RFD, etc.)
 * must not enter ranking. Among listed: highest invoice detailed-data
 * total_volumes; tie / no usable volumes → highest pod.id.
 *
 * Tests encode TO-BE (Jira PDT-3212 / PDT-3345 + Confluence 72155868 / 585697986).
 * MUST FAIL if Dev2 still returns all false for remaining listed PODs (AS-IS).
 *
 * Bug-only automation (no TC .md). Creates all data from scratch. No test.beforeAll.
 *
 * Swagger: Dev2 spec refreshed this session via curl to
 * config/swagger/dev2/swagger-spec.json (update-swagger-specs.ps1 failed: curl.exe
 * missing on macOS). Field names/enums taken from that spec.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3090-rfd-highest-consumption-fallback.spec.ts
 * - tests/cursor/pdt-3090-rfd-highest-consumption-fallback.fixtures.ts
 * - tests/cursor/PDT-3421-rfd-highest-consumption-uncheck.spec.ts
 * - tests/cursor/pdt-3421-rfd-highest-consumption-uncheck.fixtures.ts
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3212_BE11_TIMEOUT_MS,
  PDT_3212_BE2_TIMEOUT_MS,
  PDT_3212_CASE1_VOLUMES,
  PDT_3212_EQUAL_REMAINING_VOLUMES,
  PDT_3212_JIRA_VOLUMES,
  PDT_3212_KALINA_VOLUMES,
  PDT_3212_KEY,
  PDT_3212_TITLE,
  assertToBeHighestAmongListed,
  createDraftRfdWithHighestFilterOn,
  createVolumeInvoiceChain,
  customerIdentifier,
  disconnectExactPodViaExecutedRfd,
  ensureExecutedReminder,
  entityId,
  expectedHighestConsumptionPodId,
  isCheckedFlag,
  isHighestConsumptionFlag,
  listedFlagSummary,
  loadCustomersForDps,
  loadCustomersForDpsAfterOffList,
  pdt3212RelevantKeys,
  rowPodId,
  rowsForPodIds,
  viewPodTab,
  type Pdt3212Fx,
} from './pdt-3212-rfd-next-pod-highest-consumption.fixtures';

function scenarioTitle(scenario: string): string {
  return `[${PDT_3212_KEY}]: ${PDT_3212_TITLE.trimEnd()} | ${scenario}`;
}

function attachExtraLinks(Responses: { productContract: unknown[] }): Record<string, string[]> | undefined {
  try {
    const extra = buildProductContractTabLinks(entityId(Responses.productContract[0]));
    return extra && Object.keys(extra).length ? extra : undefined;
  } catch {
    return undefined;
  }
}

test.describe(`[${PDT_3212_KEY}]: ${PDT_3212_TITLE}`, {
  tag: ['@dev2', '@receivableManagement', '@pdt-3212'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test(`[${PDT_3212_KEY}]: ${PDT_3212_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3212_BE2_TIMEOUT_MS);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await test.step(
      'Precondition: 3 PODs same BG, invoice volumes 1500 / 2000 / 4020 (Confluence Case 1)',
      async () => {
        const created = await createVolumeInvoiceChain(fx, 2, [...PDT_3212_CASE1_VOLUMES]);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('invoice', {
          invoiceId: created.invoiceId,
          volumesByAscendingPodId: [...PDT_3212_CASE1_VOLUMES],
          sumsByPodId: created.sumsByPodId,
        });
        return created;
      },
    );

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => {
        const id = await ensureExecutedReminder(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', { reminderId: id });
        return id;
      },
    );

    const load = await test.step(
      'GET load-customer-for-disconnection-power-supply (LIST_OF_CUSTOMERS)',
      async () => loadCustomersForDps(fx, reminderId),
    );

    await test.step('Assert: all 3 listed; exactly one YES = 4020 POD', async () => {
      const podIds = chain.sorted.map((pod) => pod.podId);
      const listed = rowsForPodIds(load.rows, podIds);
      const winnerId = chain.sorted[2].podId;
      const expectedYes = expectedHighestConsumptionPodId(podIds, chain.sumsByPodId);
      const yesRows = listed.filter(isHighestConsumptionFlag);
      const passed =
        listed.length === 3 &&
        yesRows.length === 1 &&
        rowPodId(yesRows[0]) === winnerId &&
        expectedYes === winnerId;

      TestRunSummary.recordCheck({
        check: 'Confluence Case 1 — all listed; 4020 POD isHighestConsumption YES',
        expectedResult:
          'HTTP 206. All 3 PODs listed. Exactly one isHighestConsumption=true and that row is the 4020 POD. Others false.',
        actualResult: passed
          ? `As expected — HTTP ${load.status}; ${listedFlagSummary(load.rows, podIds)}`
          : `Not as expected — HTTP ${load.status}; ${listedFlagSummary(load.rows, podIds)}`,
        passed,
      });

      expect(listed.length, 'Load PODs must list all three PODs').toBe(3);
      expect(expectedYes, 'invoice totalVolumes ranking must pick the 4020 POD').toBe(winnerId);
      expect(yesRows.length, 'exactly one isHighestConsumption===true').toBe(1);
      expect(rowPodId(yesRows[0]), 'YES must be the 4020 POD').toBe(winnerId);
      for (const row of listed) {
        if (rowPodId(row) === winnerId) {
          expect(row.isHighestConsumption).toBe(true);
        } else {
          expect(row.isHighestConsumption).toBe(false);
        }
      }
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3212_KEY,
        relevantEntityKeys: pdt3212RelevantKeys({ includeInvoice: true }),
        extraLinks: attachExtraLinks(Responses),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          invoiceId: chain.invoiceId,
          podIds: chain.sorted.map((pod) => pod.podId),
          loadStatus: load.status,
        },
      });
    });
  });

  test(scenarioTitle('Jira 3-POD example (core TO-BE)'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3212_BE11_TIMEOUT_MS);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await test.step(
      'Precondition: 3 PODs same BG, volumes A=3000 B=2000 C=1000 (ascending pod.id)',
      async () => {
        const created = await createVolumeInvoiceChain(fx, 2, [...PDT_3212_JIRA_VOLUMES]);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('invoice', {
          invoiceId: created.invoiceId,
          volumesByAscendingPodId: [...PDT_3212_JIRA_VOLUMES],
          sumsByPodId: created.sumsByPodId,
        });
        return created;
      },
    );
    const podA = chain.sorted[0];
    const podB = chain.sorted[1];
    const podC = chain.sorted[2];

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => {
        const id = await ensureExecutedReminder(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', { reminderId: id });
        return id;
      },
    );

    await test.step('GET Load PODs sanity: A is YES while all three are listed', async () => {
      const load = await loadCustomersForDps(fx, reminderId);
      const listed = rowsForPodIds(load.rows, [podA.podId, podB.podId, podC.podId]);
      const aRow = listed.find((row) => rowPodId(row) === podA.podId);
      TestRunSummary.registerPayload('loadCustomersBeforeDisconnect', {
        status: load.status,
        flags: listedFlagSummary(load.rows, [podA.podId, podB.podId, podC.podId]),
      });
      expect(listed.length, 'sanity: all three PODs listed before disconnect').toBe(3);
      expect(aRow, `sanity: Load PODs must include A ${podA.podId}`).toBeTruthy();
      expect(aRow?.isHighestConsumption, 'sanity: A (3000) isHighestConsumption before disconnect').toBe(true);
    });

    const disconnected = await test.step(
      'POST EXECUTED RFD (highest-consumption filter) then EXECUTED DPS for A only',
      async () => {
        const result = await disconnectExactPodViaExecutedRfd(fx, reminderId, podA.podId);
        TestRunSummary.registerPayload('requestForDisconnection', {
          rfdId: result.rfdId,
          dpsId: result.dpsId,
          disconnectedPodId: podA.podId,
        });
        return result;
      },
    );

    const after = await test.step(
      'GET Load PODs again (same reminder if listed; else new EXECUTED reminder, unpaid liability)',
      async () => {
        const result = await loadCustomersForDpsAfterOffList(fx, reminderId, [podB.podId, podC.podId]);
        TestRunSummary.registerPayload('loadCustomersAfterDisconnect', {
          reminderId: result.reminderId,
          usedNewReminder: result.usedNewReminder,
          status: result.load.status,
          flags: listedFlagSummary(result.load.rows, [podA.podId, podB.podId, podC.podId]),
        });
        return result;
      },
    );

    await test.step('Assert TO-BE: A off-list; B YES; C NO (fail if B and C both false)', async () => {
      const expectedYes = expectedHighestConsumptionPodId([podB.podId, podC.podId], chain.sumsByPodId);
      const listedRemaining = rowsForPodIds(after.load.rows, [podB.podId, podC.podId]);
      const allFalse =
        listedRemaining.length >= 2 &&
        listedRemaining.every((row) => row.isHighestConsumption === false);
      const passed =
        rowsForPodIds(after.load.rows, [podA.podId]).length === 0 &&
        listedRemaining.length === 2 &&
        !allFalse &&
        expectedYes === podB.podId &&
        isHighestConsumptionFlag(listedRemaining.find((row) => rowPodId(row) === podB.podId)) &&
        listedRemaining.find((row) => rowPodId(row) === podC.podId)?.isHighestConsumption === false;

      TestRunSummary.recordCheck({
        check: 'Jira 3-POD — rank only among listed remaining PODs',
        expectedResult:
          'A not listed. B and C listed. B isHighestConsumption=true. C false. Fail vs spec if B and C are both false (AS-IS).',
        actualResult: passed
          ? `As expected — HTTP ${after.load.status}; ${listedFlagSummary(after.load.rows, [podA.podId, podB.podId, podC.podId])}`
          : `Not as expected — HTTP ${after.load.status}; ${listedFlagSummary(after.load.rows, [podA.podId, podB.podId, podC.podId])}` +
            (allFalse ? ' (AS-IS: remaining listed all false)' : ''),
        passed,
      });

      expect(expectedYes, 'among listed B/C, invoice volumes must pick B').toBe(podB.podId);
      assertToBeHighestAmongListed({
        rows: after.load.rows,
        offListPodIds: [podA.podId],
        listedPodIds: [podB.podId, podC.podId],
        expectedYesPodId: podB.podId,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3212_KEY,
        relevantEntityKeys: pdt3212RelevantKeys({ includeInvoice: true, includeRfd: true }),
        extraLinks: attachExtraLinks(Responses),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          reminderAfterId: after.reminderId,
          rfdId: disconnected.rfdId,
          dpsId: disconnected.dpsId,
          podA: podA.podId,
          podB: podB.podId,
          podC: podC.podId,
          usedNewReminder: after.usedNewReminder,
        },
      });
    });
  });

  test(scenarioTitle('Kalina 2-POD remaining listed YES'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3212_BE11_TIMEOUT_MS);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await test.step(
      'Precondition: 2 PODs same BG, disconnect the highest-volume POD',
      async () => {
        const created = await createVolumeInvoiceChain(fx, 1, [...PDT_3212_KALINA_VOLUMES]);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('invoice', {
          invoiceId: created.invoiceId,
          volumesByAscendingPodId: [...PDT_3212_KALINA_VOLUMES],
          sumsByPodId: created.sumsByPodId,
        });
        return created;
      },
    );
    const highest = chain.sorted[0];
    const remaining = chain.sorted[1];

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => {
        const id = await ensureExecutedReminder(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', { reminderId: id });
        return id;
      },
    );

    await test.step('GET Load PODs sanity: highest-volume POD is YES', async () => {
      const load = await loadCustomersForDps(fx, reminderId);
      const listed = rowsForPodIds(load.rows, [highest.podId, remaining.podId]);
      const highRow = listed.find((row) => rowPodId(row) === highest.podId);
      expect(listed.length, 'Kalina sanity: both PODs listed').toBe(2);
      expect(highRow?.isHighestConsumption, 'Kalina sanity: highest-volume POD YES').toBe(true);
    });

    await test.step('POST EXECUTED RFD + EXECUTED DPS for the highest-volume POD only', async () => {
      const result = await disconnectExactPodViaExecutedRfd(fx, reminderId, highest.podId);
      TestRunSummary.registerPayload('requestForDisconnection', {
        rfdId: result.rfdId,
        dpsId: result.dpsId,
        disconnectedPodId: highest.podId,
      });
    });

    const after = await test.step(
      'GET Load PODs after disconnect (same reminder if listed; else new reminder)',
      async () => loadCustomersForDpsAfterOffList(fx, reminderId, [remaining.podId]),
    );

    await test.step('Assert: disconnected POD not listed; remaining listed POD is YES', async () => {
      const expectedYes = expectedHighestConsumptionPodId([remaining.podId], chain.sumsByPodId);
      const listedRemaining = rowsForPodIds(after.load.rows, [remaining.podId]);
      const remainingFalse =
        listedRemaining.length === 1 && listedRemaining[0].isHighestConsumption === false;
      const passed =
        rowsForPodIds(after.load.rows, [highest.podId]).length === 0 &&
        listedRemaining.length === 1 &&
        isHighestConsumptionFlag(listedRemaining[0]) &&
        expectedYes === remaining.podId;

      TestRunSummary.recordCheck({
        check: 'Kalina 2-POD — remaining listed POD is YES not NO',
        expectedResult:
          'Disconnected POD not listed. The remaining listed POD isHighestConsumption=true (not false).',
        actualResult: passed
          ? `As expected — HTTP ${after.load.status}; ${listedFlagSummary(after.load.rows, [highest.podId, remaining.podId])}`
          : `Not as expected — HTTP ${after.load.status}; ${listedFlagSummary(after.load.rows, [highest.podId, remaining.podId])}` +
            (remainingFalse ? ' (AS-IS: remaining listed NO)' : ''),
        passed,
      });

      expect(expectedYes).toBe(remaining.podId);
      assertToBeHighestAmongListed({
        rows: after.load.rows,
        offListPodIds: [highest.podId],
        listedPodIds: [remaining.podId],
        expectedYesPodId: remaining.podId,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3212_KEY,
        relevantEntityKeys: pdt3212RelevantKeys({ includeInvoice: true, includeRfd: true }),
        extraLinks: attachExtraLinks(Responses),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          reminderAfterId: after.reminderId,
          highestPodId: highest.podId,
          remainingPodId: remaining.podId,
          usedNewReminder: after.usedNewReminder,
        },
      });
    });
  });

  test(scenarioTitle('Select PODs with highest consumption follows YES'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3212_BE11_TIMEOUT_MS);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await test.step(
      'Precondition: 3 PODs, A=3000 B=2000 C=1000, then disconnect A',
      async () => {
        const created = await createVolumeInvoiceChain(fx, 2, [...PDT_3212_JIRA_VOLUMES]);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('invoice', {
          invoiceId: created.invoiceId,
          sumsByPodId: created.sumsByPodId,
        });
        return created;
      },
    );
    const podA = chain.sorted[0];
    const podB = chain.sorted[1];
    const podC = chain.sorted[2];

    const reminderId = await test.step(
      'Precondition: reminder R1 for EXECUTED RFD / DPS',
      async () => {
        const id = await ensureExecutedReminder(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', { reminderId: id });
        return id;
      },
    );

    await test.step('GET Load PODs sanity: A is YES while all three are listed', async () => {
      const load = await loadCustomersForDps(fx, reminderId);
      const listed = rowsForPodIds(load.rows, [podA.podId, podB.podId, podC.podId]);
      const aRow = listed.find((row) => rowPodId(row) === podA.podId);
      expect(listed.length, 'sanity: all three PODs listed before disconnect').toBe(3);
      expect(aRow?.isHighestConsumption, 'sanity: A (3000) isHighestConsumption before disconnect').toBe(
        true,
      );
    });

    await test.step('POST EXECUTED RFD + EXECUTED DPS for A only', async () => {
      const result = await disconnectExactPodViaExecutedRfd(fx, reminderId, podA.podId);
      TestRunSummary.registerPayload('rfdExecuted', result);
    });

    const after = await test.step(
      'New EXECUTED reminder on unpaid liability, then GET Load PODs (B YES)',
      async () => {
        const reminderAfter = await ensureExecutedReminder(fx);
        const result = await loadCustomersForDpsAfterOffList(fx, reminderAfter, [podB.podId, podC.podId]);
        TestRunSummary.registerPayload('reminderAfter', {
          reminderId: result.reminderId,
          usedNewReminder: result.usedNewReminder,
        });
        const listed = rowsForPodIds(result.load.rows, [podB.podId, podC.podId]);
        const bYes = isHighestConsumptionFlag(listed.find((row) => rowPodId(row) === podB.podId));
        TestRunSummary.recordCheck({
          check: 'After A off-list, listed YES is B before select-highest DRAFT',
          expectedResult: 'A not listed. B isHighestConsumption=true. C false.',
          actualResult: bYes
            ? `As expected — HTTP ${result.load.status}; ${listedFlagSummary(result.load.rows, [podA.podId, podB.podId, podC.podId])}`
            : `Not as expected — HTTP ${result.load.status}; ${listedFlagSummary(result.load.rows, [podA.podId, podB.podId, podC.podId])}`,
          passed: bYes && rowsForPodIds(result.load.rows, [podA.podId]).length === 0,
        });
        assertToBeHighestAmongListed({
          rows: result.load.rows,
          offListPodIds: [podA.podId],
          listedPodIds: [podB.podId, podC.podId],
          expectedYesPodId: podB.podId,
        });
        return result;
      },
    );

    const rfdId = await test.step(
      'POST DRAFT RFD podWithHighestConsumption=true, LIST_OF_CUSTOMERS, validityPeriodFrom/To',
      async () => {
        const id = await createDraftRfdWithHighestFilterOn(fx, after.reminderId);
        TestRunSummary.registerPayload('rfdDraft', {
          id,
          podWithHighestConsumption: true,
          allSelected: false,
          conditionType: 'LIST_OF_CUSTOMERS',
          supplierType: 'CURRENT',
        });
        expect(id).toBeGreaterThan(0);
        return id;
      },
    );

    await test.step('GET view-pod-tab — B YES+checked; C both false; A not listed', async () => {
      const tab = await viewPodTab(fx, rfdId, after.reminderId);
      const aListed = rowsForPodIds(tab.rows, [podA.podId]);
      const listed = rowsForPodIds(tab.rows, [podB.podId, podC.podId]);
      const bRow = listed.find((row) => rowPodId(row) === podB.podId);
      const cRow = listed.find((row) => rowPodId(row) === podC.podId);
      const passed =
        aListed.length === 0 &&
        isHighestConsumptionFlag(bRow) &&
        isCheckedFlag(bRow) &&
        cRow?.isHighestConsumption === false &&
        cRow?.isChecked === false;

      TestRunSummary.recordCheck({
        check: 'Select PODs with highest consumption follows listed YES',
        expectedResult:
          'HTTP 206. A not listed. B isHighestConsumption=true and isChecked=true. C both false.',
        actualResult: passed
          ? `As expected — HTTP ${tab.status}; ${listedFlagSummary(tab.rows, [podA.podId, podB.podId, podC.podId])}`
          : `Not as expected — HTTP ${tab.status}; ${listedFlagSummary(tab.rows, [podA.podId, podB.podId, podC.podId])}`,
        passed,
      });

      expect(aListed.length, 'A must not appear on view-pod-tab').toBe(0);
      expect(bRow, `view-pod-tab must include B ${podB.podId}`).toBeTruthy();
      expect(cRow, `view-pod-tab must include C ${podC.podId}`).toBeTruthy();
      expect(bRow?.isHighestConsumption, 'B isHighestConsumption').toBe(true);
      expect(bRow?.isChecked, 'B isChecked').toBe(true);
      expect(cRow?.isHighestConsumption, 'C isHighestConsumption').toBe(false);
      expect(cRow?.isChecked, 'C isChecked').toBe(false);
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3212_KEY,
        relevantEntityKeys: pdt3212RelevantKeys({ includeInvoice: true, includeRfd: true }),
        extraLinks: attachExtraLinks(Responses),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          reminderAfterId: after.reminderId,
          rfdDraftId: rfdId,
          podA: podA.podId,
          podB: podB.podId,
          podC: podC.podId,
        },
      });
    });
  });

  test(scenarioTitle('Equal volumes among remaining listed → highest pod.id'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3212_BE11_TIMEOUT_MS);
    const fx: Pdt3212Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await test.step(
      'Precondition: 3 PODs; A unique high volume; B and C equal lower volumes',
      async () => {
        const created = await createVolumeInvoiceChain(fx, 2, [...PDT_3212_EQUAL_REMAINING_VOLUMES]);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('invoice', {
          invoiceId: created.invoiceId,
          volumesByAscendingPodId: [...PDT_3212_EQUAL_REMAINING_VOLUMES],
          sumsByPodId: created.sumsByPodId,
        });
        return created;
      },
    );
    const podA = chain.sorted[0];
    const podB = chain.sorted[1];
    const podC = chain.sorted[2];
    const tieYesId = Math.max(podB.podId, podC.podId);

    const reminderId = await test.step(
      'Precondition: reminder for disconnection (LIST_OF_CUSTOMERS, no /job)',
      async () => {
        const id = await ensureExecutedReminder(fx);
        TestRunSummary.registerPayload('reminderForDisconnection', { reminderId: id });
        return id;
      },
    );

    await test.step('GET Load PODs sanity: A is YES before disconnect', async () => {
      const load = await loadCustomersForDps(fx, reminderId);
      const aRow = rowsForPodIds(load.rows, [podA.podId])[0];
      expect(aRow, `sanity: A ${podA.podId} listed`).toBeTruthy();
      expect(aRow?.isHighestConsumption, 'sanity: unique high-volume A is YES').toBe(true);
    });

    await test.step('POST EXECUTED RFD + EXECUTED DPS for A only', async () => {
      const result = await disconnectExactPodViaExecutedRfd(fx, reminderId, podA.podId);
      TestRunSummary.registerPayload('requestForDisconnection', {
        rfdId: result.rfdId,
        dpsId: result.dpsId,
        disconnectedPodId: podA.podId,
      });
    });

    const after = await test.step(
      'GET Load PODs after A off-list (same reminder if listed; else new reminder)',
      async () => loadCustomersForDpsAfterOffList(fx, reminderId, [podB.podId, podC.podId]),
    );

    await test.step('Assert: among listed B and C (equal volumes), YES = max(pod.id)', async () => {
      expect(chain.sumsByPodId[podB.podId], 'B and C invoice totalVolumes must be equal').toBe(
        chain.sumsByPodId[podC.podId],
      );
      const expectedYes = expectedHighestConsumptionPodId([podB.podId, podC.podId], chain.sumsByPodId);
      const passed = expectedYes === tieYesId;
      TestRunSummary.recordCheck({
        check: 'Equal remaining volumes — YES = highest pod.id among listed only',
        expectedResult: `A not listed. B and C listed with equal volumes. isHighestConsumption=true on max(pod.id)=${tieYesId}. Other remaining false.`,
        actualResult: passed
          ? `As expected — HTTP ${after.load.status}; ${listedFlagSummary(after.load.rows, [podA.podId, podB.podId, podC.podId])}`
          : `Not as expected — HTTP ${after.load.status}; ${listedFlagSummary(after.load.rows, [podA.podId, podB.podId, podC.podId])}`,
        passed:
          passed &&
          rowsForPodIds(after.load.rows, [podA.podId]).length === 0 &&
          isHighestConsumptionFlag(
            rowsForPodIds(after.load.rows, [tieYesId])[0],
          ),
      });

      expect(expectedYes, 'tie-break among listed must be max pod.id').toBe(tieYesId);
      assertToBeHighestAmongListed({
        rows: after.load.rows,
        offListPodIds: [podA.podId],
        listedPodIds: [podB.podId, podC.podId],
        expectedYesPodId: tieYesId,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3212_KEY,
        relevantEntityKeys: pdt3212RelevantKeys({ includeInvoice: true, includeRfd: true }),
        extraLinks: attachExtraLinks(Responses),
        snapshot: {
          customerIdentifier: customerIdentifier(Responses),
          reminderId,
          reminderAfterId: after.reminderId,
          podA: podA.podId,
          podB: podB.podId,
          podC: podC.podId,
          tieYesId,
          usedNewReminder: after.usedNewReminder,
        },
      });
    });
  });
});
