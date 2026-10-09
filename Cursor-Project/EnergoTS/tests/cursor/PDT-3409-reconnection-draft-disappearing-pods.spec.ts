/**
 * PDT-3409 — Reconnection of power supply DRAFT listing: unpaid executed-RFD PODs
 * must stay visible and unchecked (Phase 1 wiki 75923727).
 *
 * Bug-only automation (no TC .md). Creates all data from scratch — no ticket UICs.
 *
 * Two PODs, same grid operator:
 * - Unpaid target: 3179 chain ([0]). Not sent in POST table.
 * - Dummy: last-index supply + liability/reminder/RFD/DPS. Only dummy is mapped into
 *   POST table so CREATE is non-empty and Phoenix does not set reconnection_pod_id
 *   on the unpaid row (isChecked stays false).
 *
 * Reproduction: GET /table and GET /table/view list the unpaid POD with checked === false.
 * GET /table/view-temporary currently drops unchecked rows on origin/test. This spec
 * asserts Phase 1 expected behavior, so the temporary test is expected to FAIL until
 * the hide filter is removed. Do not treat a missing unpaid row as success.
 *
 * Third test: four origin/dev regressions on completely fresh data (no ticket UICs,
 * not Reconnection-1331). expect.soft + separate recordCheck per check so one failure
 * does not skip the others. Wiki expected; Dev may fail (that CONFIRMS the regression).
 *
 * Swagger refresh: update-swagger-specs.ps1 this session (all envs OK including dev).
 * Endpoints.reconnectionOfPowerSupply = reconnection-of-the-power-supply.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import type { TestRunSummaryCollector } from './shared/test-run-summary.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3409_KEY,
  PDT_3409_TITLE,
  PDT_3409_TEST_TIMEOUT_MS,
  executeDummyReconnection,
  findPodTableRow,
  formatPartyProbe,
  getPodDisconnectedField,
  getReconnectionTable,
  listingRows,
  missingUnpaidPodMessage,
  pdt3409RelevantKeys,
  probePartyListing,
  rowChecked,
  runPdt3409DraftReconnectionChain,
  rowMatchesCreatedPodAndRfd,
  tryGetReconnectionTable,
  unpaidPodVisibleUnchecked,
  type Pdt3409DraftChain,
  type Pdt3409Fx,
} from './pdt-3409-reconnection-draft-disappearing-pods.fixtures';
import { entityId } from './pdt-3179-reconnection-invoice-emails.fixtures';

const JIRA_TITLE = PDT_3409_TITLE;

function titleFor(scenario: string): string {
  return `[${PDT_3409_KEY}]: ${JIRA_TITLE} | ${scenario}`;
}

function unpaidSearchQuery(chain: Pdt3409DraftChain): Record<string, string | number> {
  return {
    page: 0,
    pageSize: 50,
    prompt: chain.unpaid.customerIdent,
    searchBy: 'CUSTOMER_IDENTIFIER',
  };
}

function dummySearchQuery(chain: Pdt3409DraftChain): Record<string, string | number> {
  return {
    page: 0,
    pageSize: 50,
    prompt: chain.dummy.customerIdent,
    searchBy: 'CUSTOMER_IDENTIFIER',
  };
}

async function attachSummary(
  TestRunSummary: TestRunSummaryCollector,
  Responses: Pdt3409Fx['Responses'],
  snapshot: Record<string, unknown>,
): Promise<void> {
  await test.step('Attach test run summary', async () => {
    TestRunSummary.registerPayload('unpaidCustomer', Responses.customer[0]);
    if (Responses.customer.length > 1) {
      TestRunSummary.registerPayload('dummyCustomer', Responses.customer[Responses.customer.length - 1]);
    }
    if (Responses.requestForDisconnection.length) {
      TestRunSummary.registerPayload('unpaidRfd', Responses.requestForDisconnection[0]);
      TestRunSummary.registerPayload(
        'dummyRfd',
        Responses.requestForDisconnection[Responses.requestForDisconnection.length - 1],
      );
    }
    if (Responses.reconnectionOfPowerSupply.length) {
      TestRunSummary.registerPayload('reconnection', Responses.reconnectionOfPowerSupply[0]);
    }
    let extra: Record<string, string[]> | undefined;
    try {
      extra = buildProductContractTabLinks(entityId(Responses.productContract[0]));
    } catch {
      extra = undefined;
    }
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: PDT_3409_KEY,
      relevantEntityKeys: pdt3409RelevantKeys(),
      extraLinks: extra && Object.keys(extra).length ? extra : undefined,
      snapshot,
    });
  });
}

test.describe(`[${PDT_3409_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@dev', '@receivableManagement', '@pdt-3409'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test(titleFor('table/view unpaid POD visible'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3409_TEST_TIMEOUT_MS);
    const fx: Pdt3409Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await test.step(
      'Precondition: unpaid 3179 chain, dummy disconnected POD (last ids), POST DRAFT with dummy table only',
      async () => runPdt3409DraftReconnectionChain(fx),
    );
    TestRunSummary.registerPayload('unpaidCustomer', {
      id: chain.unpaid.customerId,
      identifier: chain.unpaid.customerIdent,
      podId: chain.unpaid.podId,
    });
    TestRunSummary.registerPayload('dummyCustomer', {
      id: chain.dummy.customerId,
      identifier: chain.dummy.customerIdent,
      podId: chain.dummy.podId,
    });
    TestRunSummary.registerPayload('reconnection', {
      id: chain.reconnectionId,
      saveAs: 'DRAFT',
      gridOperatorId: chain.gridOperatorId,
      tablePodId: chain.dummy.podId,
    });

    const tablePath = `${Endpoints.reconnectionOfPowerSupply}/table`;
    const viewPath = `${Endpoints.reconnectionOfPowerSupply}/table/view`;
    const unpaidSearch = unpaidSearchQuery(chain);

    const tableBody = await test.step(
      'GET /reconnection-of-the-power-supply/table by unpaid CUSTOMER_IDENTIFIER (no reconnectionId)',
      async () => {
        const { body } = await getReconnectionTable(fx, tablePath, {
          gridOperatorId: chain.gridOperatorId,
          ...unpaidSearch,
        });
        return body;
      },
    );
    const tableRow = findPodTableRow(listingRows(tableBody), chain.unpaid);
    const tablePassed =
      unpaidPodVisibleUnchecked(tableRow) && rowMatchesCreatedPodAndRfd(tableRow, chain.unpaid);

    const viewBody = await test.step(
      'GET /reconnection-of-the-power-supply/table/view DRAFT listing by unpaid CUSTOMER_IDENTIFIER',
      async () => {
        const { body } = await getReconnectionTable(
          fx,
          viewPath,
          unpaidSearch,
          chain.reconnectionId,
        );
        return body;
      },
    );
    const viewRow = findPodTableRow(listingRows(viewBody), chain.unpaid);
    const viewPassed =
      unpaidPodVisibleUnchecked(viewRow) && rowMatchesCreatedPodAndRfd(viewRow, chain.unpaid);

    TestRunSummary.recordCheck({
      check: 'GET /table unpaid executed-RFD POD visible unchecked',
      expectedResult:
        'CreateReconnectionTableResponse row for the unpaid POD with checked === false (not saved on DRAFT).',
      actualResult: tablePassed
        ? `As expected — unpaid POD ${chain.unpaid.podIdentifier} present, checked=${String(rowChecked(tableRow))}.`
        : `Not as expected — row=${tableRow ? JSON.stringify(tableRow).slice(0, 400) : 'missing'}.`,
      passed: tablePassed,
    });
    TestRunSummary.recordCheck({
      check: 'GET /table/view unpaid executed-RFD POD visible unchecked',
      expectedResult:
        'TableViewResponse row for the unpaid POD with checked === false (reconnection_pod_id null because it was not in POST table).',
      actualResult: viewPassed
        ? `As expected — unpaid POD ${chain.unpaid.podIdentifier} present, checked=${String(rowChecked(viewRow))}.`
        : `Not as expected — row=${viewRow ? JSON.stringify(viewRow).slice(0, 400) : 'missing'}.`,
      passed: viewPassed,
    });
    await attachSummary(TestRunSummary, Responses, {
      unpaid: chain.unpaid,
      dummy: chain.dummy,
      reconnectionId: chain.reconnectionId,
      gridOperatorId: chain.gridOperatorId,
      tableChecked: rowChecked(tableRow),
      viewChecked: rowChecked(viewRow),
    });

    expect(
      tableRow,
      missingUnpaidPodMessage(tablePath, chain.unpaid.podIdentifier),
    ).toBeTruthy();
    expect(Number(tableRow!.podId), 'GET /table unpaid podId').toBe(chain.unpaid.podId);
    expect(
      Number(tableRow!.requestForDisconnectionId),
      'GET /table unpaid requestForDisconnectionId',
    ).toBe(chain.unpaid.rfdId);
    expect(rowChecked(tableRow), 'GET /table checked must be false (unpaid, not saved on DRAFT)').toBe(
      false,
    );
    expect(
      viewRow,
      missingUnpaidPodMessage(viewPath, chain.unpaid.podIdentifier),
    ).toBeTruthy();
    expect(Number(viewRow!.podId), 'GET /table/view unpaid podId').toBe(chain.unpaid.podId);
    expect(
      Number(viewRow!.requestForDisconnectionId),
      'GET /table/view unpaid requestForDisconnectionId',
    ).toBe(chain.unpaid.rfdId);
    expect(
      rowChecked(viewRow),
      'GET /table/view checked must be false (reconnection_pod_id null)',
    ).toBe(false);
  });

  test(titleFor('table/view-temporary unpaid POD still visible'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3409_TEST_TIMEOUT_MS);
    const fx: Pdt3409Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await test.step(
      'Precondition: unpaid 3179 chain, dummy disconnected POD (last ids), POST DRAFT with dummy table only',
      async () => runPdt3409DraftReconnectionChain(fx),
    );
    TestRunSummary.registerPayload('unpaidCustomer', {
      id: chain.unpaid.customerId,
      identifier: chain.unpaid.customerIdent,
      podId: chain.unpaid.podId,
    });
    TestRunSummary.registerPayload('dummyCustomer', {
      id: chain.dummy.customerId,
      identifier: chain.dummy.customerIdent,
      podId: chain.dummy.podId,
    });
    TestRunSummary.registerPayload('reconnection', {
      id: chain.reconnectionId,
      saveAs: 'DRAFT',
      gridOperatorId: chain.gridOperatorId,
      tablePodId: chain.dummy.podId,
    });

    const viewPath = `${Endpoints.reconnectionOfPowerSupply}/table/view`;
    const temporaryPath = `${Endpoints.reconnectionOfPowerSupply}/table/view-temporary`;
    const unpaidSearch = unpaidSearchQuery(chain);

    const viewBody = await test.step(
      'Control: GET /table/view by unpaid CUSTOMER_IDENTIFIER (unpaid POD should already be listed)',
      async () => {
        const { body } = await getReconnectionTable(fx, viewPath, unpaidSearch, chain.reconnectionId);
        return body;
      },
    );
    const viewRow = findPodTableRow(listingRows(viewBody), chain.unpaid);
    const viewPassed =
      unpaidPodVisibleUnchecked(viewRow) && rowMatchesCreatedPodAndRfd(viewRow, chain.unpaid);

    const temporaryBody = await test.step(
      'GET /reconnection-of-the-power-supply/table/view-temporary by unpaid CUSTOMER_IDENTIFIER (PDT-3409 reproduction)',
      async () => {
        const { body } = await getReconnectionTable(
          fx,
          temporaryPath,
          unpaidSearch,
          chain.reconnectionId,
        );
        return body;
      },
    );
    const temporaryRows = listingRows(temporaryBody);
    const temporaryRow = findPodTableRow(temporaryRows, chain.unpaid);
    const temporaryPassed =
      unpaidPodVisibleUnchecked(temporaryRow) && rowMatchesCreatedPodAndRfd(temporaryRow, chain.unpaid);

    const unfilteredPageSize = 50;
    const unfilteredBody = await test.step(
      'GET /table/view-temporary without prompt (page=0) — portal empty-grid check',
      async () => {
        const { body } = await getReconnectionTable(
          fx,
          temporaryPath,
          { page: 0, pageSize: unfilteredPageSize },
          chain.reconnectionId,
        );
        return body;
      },
    );
    const unfilteredRows = listingRows(unfilteredBody);
    const unfilteredRow = findPodTableRow(unfilteredRows, chain.unpaid);
    const unfilteredFound = Boolean(unfilteredRow);

    TestRunSummary.recordCheck({
      check: 'Control GET /table/view unpaid POD visible unchecked',
      expectedResult: 'Same unpaid POD as temporary, checked === false (data chain; reconnection_pod_id null).',
      actualResult: viewPassed
        ? `As expected — unpaid POD ${chain.unpaid.podIdentifier} present, checked=${String(rowChecked(viewRow))}.`
        : `Not as expected — control view row missing or checked=${String(rowChecked(viewRow))}.`,
      passed: viewPassed,
    });
    TestRunSummary.recordCheck({
      check: 'GET /table/view-temporary unpaid POD still visible unchecked',
      expectedResult:
        'Phase 1: unpaid executed-RFD POD remains in TableViewResponse[] with checked === false. Current code may drop unchecked rows (PDT-3409).',
      actualResult: temporaryPassed
        ? `As expected — unpaid POD ${chain.unpaid.podIdentifier} present, checked=${String(rowChecked(temporaryRow))}.`
        : `Not as expected — unpaid POD missing or checked=${String(rowChecked(temporaryRow))}. This IS the PDT-3409 reproduction if the row was dropped.`,
      passed: temporaryPassed,
    });
    TestRunSummary.recordCheck({
      check: 'GET /table/view-temporary without prompt (page=0) still lists unpaid POD',
      expectedResult:
        `Unpaid POD present in the unfiltered temporary page (pageSize=${unfilteredPageSize}), matching the empty portal grid report.`,
      actualResult: unfilteredFound
        ? `As expected — unpaid POD ${chain.unpaid.podIdentifier} in unfiltered list (${unfilteredRows.length} rows, pageSize=${unfilteredPageSize}).`
        : `Not in first unfiltered page (${unfilteredRows.length} rows, pageSize=${unfilteredPageSize}). UIC-filtered temporary call is the required PDT-3409 assertion.`,
      passed: unfilteredFound,
    });
    await attachSummary(TestRunSummary, Responses, {
      unpaid: chain.unpaid,
      dummy: chain.dummy,
      reconnectionId: chain.reconnectionId,
      gridOperatorId: chain.gridOperatorId,
      viewChecked: rowChecked(viewRow),
      temporaryChecked: rowChecked(temporaryRow),
      temporaryRowCount: temporaryRows.length,
      unfilteredRowCount: unfilteredRows.length,
      unfilteredPageSize,
      unfilteredFound,
    });

    expect(
      viewRow,
      missingUnpaidPodMessage(viewPath, chain.unpaid.podIdentifier),
    ).toBeTruthy();
    expect(Number(viewRow!.podId), 'control /table/view unpaid podId').toBe(chain.unpaid.podId);
    expect(
      Number(viewRow!.requestForDisconnectionId),
      'control /table/view unpaid requestForDisconnectionId',
    ).toBe(chain.unpaid.rfdId);
    expect(rowChecked(viewRow), 'control /table/view checked must be false').toBe(false);
    expect(
      temporaryRow,
      missingUnpaidPodMessage(temporaryPath, chain.unpaid.podIdentifier),
    ).toBeTruthy();
    expect(Number(temporaryRow!.podId), 'GET /table/view-temporary unpaid podId').toBe(chain.unpaid.podId);
    expect(
      Number(temporaryRow!.requestForDisconnectionId),
      'GET /table/view-temporary unpaid requestForDisconnectionId',
    ).toBe(chain.unpaid.rfdId);
    expect(
      rowChecked(temporaryRow),
      'GET /table/view-temporary checked must be false (unpaid row must stay visible)',
    ).toBe(false);
  });

  test(titleFor('four origin/dev regressions on fresh data'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3409_TEST_TIMEOUT_MS);
    const fx: Pdt3409Fx = { Request, GeneratePayload, Responses, Endpoints };

    const chain = await test.step(
      'Precondition: unpaid 3179 chain [0] not in POST table; dummy last-index chain is the only POST DRAFT row (fresh identifiers)',
      async () => runPdt3409DraftReconnectionChain(fx),
    );
    TestRunSummary.registerPayload('unpaidCustomer', {
      id: chain.unpaid.customerId,
      identifier: chain.unpaid.customerIdent,
      podId: chain.unpaid.podId,
      rfdId: chain.unpaid.rfdId,
      liabilityId: chain.unpaid.liabilityId,
    });
    TestRunSummary.registerPayload('dummyCustomer', {
      id: chain.dummy.customerId,
      identifier: chain.dummy.customerIdent,
      podId: chain.dummy.podId,
      rfdId: chain.dummy.rfdId,
      liabilityId: chain.dummy.liabilityId,
    });
    TestRunSummary.registerPayload('reconnection', {
      id: chain.reconnectionId,
      saveAs: 'DRAFT',
      gridOperatorId: chain.gridOperatorId,
      tablePodId: chain.dummy.podId,
    });

    const tablePath = `${Endpoints.reconnectionOfPowerSupply}/table`;
    const viewPath = `${Endpoints.reconnectionOfPowerSupply}/table/view`;
    const temporaryPath = `${Endpoints.reconnectionOfPowerSupply}/table/view-temporary`;
    const unpaidSearch = unpaidSearchQuery(chain);
    const dummySearch = dummySearchQuery(chain);
    const createUnpaidQuery = { gridOperatorId: chain.gridOperatorId, ...unpaidSearch };
    const createDummyQuery = { gridOperatorId: chain.gridOperatorId, ...dummySearch };

    const viewListing = await test.step(
      'P1: GET /reconnection-of-the-power-supply/table/view unpaid CUSTOMER_IDENTIFIER',
      async () => tryGetReconnectionTable(fx, viewPath, unpaidSearch, chain.reconnectionId),
    );
    const createListing = await test.step(
      'P2: GET /reconnection-of-the-power-supply/table CREATE unpaid CUSTOMER_IDENTIFIER (gridOperatorId required)',
      async () => tryGetReconnectionTable(fx, tablePath, createUnpaidQuery),
    );
    const temporaryListing = await test.step(
      'P3: GET /reconnection-of-the-power-supply/table/view-temporary unpaid CUSTOMER_IDENTIFIER',
      async () => tryGetReconnectionTable(fx, temporaryPath, unpaidSearch, chain.reconnectionId),
    );

    const viewProbe = probePartyListing(viewListing, chain.unpaid);
    const createProbe = probePartyListing(createListing, chain.unpaid);
    const temporaryProbe = probePartyListing(temporaryListing, chain.unpaid);
    const viewHasUnpaid = viewProbe.visibleUnchecked && viewProbe.matches;
    const createHasUnpaid = createProbe.visibleUnchecked && createProbe.matches;
    const temporaryHasUnpaid = temporaryProbe.visibleUnchecked && temporaryProbe.matches;

    const p1Passed = viewHasUnpaid;
    TestRunSummary.recordCheck({
      check: 'P1 unpaid hidden on DRAFT view (original PDT-3409)',
      expectedResult:
        'Wiki 75923727: GET /table/view unpaid executed-RFD POD present with checked===false. Dev hide SQL is reconnection_pod_id is not null OR liability_amount = 0.',
      actualResult: p1Passed
        ? `As expected — ${formatPartyProbe('GET /table/view', viewProbe, chain.unpaid)}`
        : `Not as expected — ${formatPartyProbe('GET /table/view', viewProbe, chain.unpaid)} Dev bug if row missing.`,
      passed: p1Passed,
    });

    let p2Actual: string;
    if (createHasUnpaid && viewHasUnpaid) {
      p2Actual =
        'As expected — both CREATE /table and DRAFT /table/view include unpaid unchecked. ' +
        'Date-gate split is not reproduced on this fresh same-day chain (not the cause). ' +
        `CREATE ${formatPartyProbe('/table', createProbe, chain.unpaid)} VIEW ${formatPartyProbe('/table/view', viewProbe, chain.unpaid)}`;
    } else if (createHasUnpaid && !viewHasUnpaid) {
      p2Actual =
        'CREATE /table shows unpaid unchecked (disconnection_date gate passed); VIEW hides it. ' +
        'Isolates P1 hide SQL vs date. CREATE uses last EXECUTED reconnection_date < last EXECUTED disconnection_date; ' +
        'VIEW uses last EXECUTED reconnection_date < last RFD execution_date. disconnected is not in listing SQL.';
    } else {
      p2Actual =
        'Not as expected — CREATE /table missing unpaid too, so P2 date-gate or disconnected-filter is in play. ' +
        `CREATE ${formatPartyProbe('/table', createProbe, chain.unpaid)} VIEW ${formatPartyProbe('/table/view', viewProbe, chain.unpaid)}`;
    }
    const p2Passed = createHasUnpaid;
    TestRunSummary.recordCheck({
      check: 'P2 date-gate split CREATE /table vs DRAFT /table/view',
      expectedResult:
        'Fresh same-day data: both date filters should pass, so CREATE GET /table still shows unpaid unchecked. ' +
        'CREATE date gate uses disconnection_date; VIEW uses RFD execution_date. If both include unpaid, date-gate is not the cause.',
      actualResult: p2Actual,
      passed: p2Passed,
    });

    const p3Match = viewHasUnpaid === temporaryHasUnpaid;
    const p3Passed = viewHasUnpaid && temporaryHasUnpaid && p3Match;
    TestRunSummary.recordCheck({
      check: 'P3 view vs view-temporary mismatch',
      expectedResult:
        'Wiki: view and view-temporary must match AND both show unpaid unchecked (checked===false). ' +
        'Sep 4 origin/dev: view has hide AND, view-temporary does not.',
      actualResult: p3Passed
        ? `As expected — viewHasUnpaid=${viewHasUnpaid} temporaryHasUnpaid=${temporaryHasUnpaid}. ${formatPartyProbe('temporary', temporaryProbe, chain.unpaid)}`
        : `Not as expected — viewHasUnpaid=${viewHasUnpaid} vs temporaryHasUnpaid=${temporaryHasUnpaid}. Dev bug if they disagree. ${formatPartyProbe('view', viewProbe, chain.unpaid)} ${formatPartyProbe('temporary', temporaryProbe, chain.unpaid)}`,
      passed: p3Passed,
    });

    const dummyCreateBefore = await test.step(
      'P4: GET /table by dummy CUSTOMER_IDENTIFIER to map EXECUTED table (non-empty)',
      async () => tryGetReconnectionTable(fx, tablePath, createDummyQuery),
    );
    const dummyBeforeProbe = probePartyListing(dummyCreateBefore, chain.dummy);

    const executed = await test.step(
      'P4: PUT same DRAFT saveAs=EXECUTED with dummy table; POST new EXECUTED if PUT fails',
      async () => executeDummyReconnection(fx, chain, dummyBeforeProbe.row),
    );
    TestRunSummary.registerPayload('executedReconnection', {
      method: executed.method,
      ok: executed.ok,
      status: executed.status,
      reconnectionId: executed.reconnectionId,
      draftReconnectionId: chain.reconnectionId,
    });

    const podDisc = await test.step('P4: GET /pod/{dummyPodId} record disconnected', async () =>
      getPodDisconnectedField(fx, chain.dummy.podId),
    );

    const dummyCreateAfter = await test.step(
      'P4: GET /table CREATE by dummy CUSTOMER_IDENTIFIER after EXECUTED reconnection',
      async () => tryGetReconnectionTable(fx, tablePath, createDummyQuery),
    );
    const dummyAfterProbe = probePartyListing(dummyCreateAfter, chain.dummy);
    const dummyStillListed = dummyAfterProbe.present;
    const disconnectedFalse = podDisc.disconnected === false;

    let p4Actual: string;
    let p4Passed: boolean;
    if (!executed.ok) {
      p4Passed = false;
      p4Actual =
        `Not as expected — could not execute dummy reconnection (${executed.method} HTTP ${executed.status} ${executed.text}). ` +
        `disconnected=${String(podDisc.disconnected)} via ${podDisc.path}.`;
    } else if (!dummyStillListed) {
      p4Passed = true;
      p4Actual =
        `P4 not reproduced on this fresh path — dummy POD ${chain.dummy.podIdentifier} absent from CREATE /table after ${executed.method} EXECUTED ` +
        `(date filter correctly excluded). disconnected=${String(podDisc.disconnected)} GET ${podDisc.path}.`;
    } else {
      p4Passed = false;
      p4Actual =
        `Not as expected — dummy POD ${chain.dummy.podIdentifier} still listed on CREATE /table after EXECUTED. ` +
        `disconnected=${String(podDisc.disconnected)} (PointOfDeliveryResponse swagger has no disconnected field; runtime value recorded). ` +
        (disconnectedFalse
          ? 'Dev bug: listed while disconnected===false (disconnected filter removed + NULL/1900-01-01 reconnection_date vs RFD execution_date). '
          : '') +
        formatPartyProbe('CREATE /table after execute', dummyAfterProbe, chain.dummy);
    }
    TestRunSummary.recordCheck({
      check: 'P4 disconnected=false still listed after EXECUTED reconnection',
      expectedResult:
        'Wiki: dummy POD already on an executed reconnection must not appear on CREATE GET /table. ' +
        'Dev bug if dummy is still listed while GET /pod disconnected===false.',
      actualResult: p4Actual,
      passed: p4Passed,
    });

    expect.soft(viewProbe.present, missingUnpaidPodMessage(viewPath, chain.unpaid.podIdentifier)).toBe(true);
    expect.soft(viewProbe.checked, 'P1 GET /table/view checked===false').toBe(false);
    expect.soft(viewHasUnpaid, 'P1 wiki: unpaid visible unchecked on DRAFT view').toBe(true);

    expect.soft(createProbe.present, missingUnpaidPodMessage(tablePath, chain.unpaid.podIdentifier)).toBe(true);
    expect.soft(createProbe.checked, 'P2 GET /table CREATE checked===false').toBe(false);
    expect.soft(createHasUnpaid, 'P2 wiki: CREATE /table shows unpaid unchecked on same-day data').toBe(true);

    expect.soft(p3Match, `P3 viewHasUnpaid=${viewHasUnpaid} vs temporaryHasUnpaid=${temporaryHasUnpaid} must match`).toBe(
      true,
    );
    expect.soft(temporaryHasUnpaid, 'P3 wiki: view-temporary unpaid visible unchecked').toBe(true);
    expect.soft(viewHasUnpaid, 'P3 wiki: view unpaid visible unchecked (must match temporary)').toBe(true);

    expect.soft(executed.ok, `P4 execute dummy saveAs=EXECUTED (${executed.method} HTTP ${executed.status})`).toBe(true);
    expect.soft(
      dummyStillListed,
      'Wiki: dummy POD must not appear on CREATE /table after executed reconnection',
    ).toBe(false);

    await attachSummary(TestRunSummary, Responses, {
      unpaid: chain.unpaid,
      dummy: chain.dummy,
      reconnectionId: chain.reconnectionId,
      executedReconnectionId: executed.reconnectionId,
      executeMethod: executed.method,
      gridOperatorId: chain.gridOperatorId,
      p1: { viewHasUnpaid, viewChecked: viewProbe.checked, viewPresent: viewProbe.present },
      p2: { createHasUnpaid, viewHasUnpaid, createChecked: createProbe.checked },
      p3: { viewHasUnpaid, temporaryHasUnpaid },
      p4: {
        executedOk: executed.ok,
        dummyStillListed,
        disconnected: podDisc.disconnected,
        podGetPath: podDisc.path,
      },
    });
  });
});
