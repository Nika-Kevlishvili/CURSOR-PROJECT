/**
 * PDT-3445 — Create listing must not preselect a POD that is not disconnected,
 * and must preselect the same POD after it is disconnected and paid.
 *
 * Bug-only automation (no TC .md). Creates every entity in the test.
 * No hardcoded customer, POD, grid operator, or request ids — BASE_URL selects
 * the environment and setup fills envVariables.
 *
 * Product rule: checked and unableToUncheck are true only when the RFD-linked
 * liability current amount is 0 and the POD is disconnected. A paid POD that
 * has only an executed request for disconnection must not be preselected.
 * After an executed disconnection of power supply, pay the tax liability the
 * disconnection adds, then that POD must be checked and locked.
 *
 * Swagger: GET /reconnection-of-the-power-supply/table
 * ReconnectionTableListingRequest (gridOperatorId required, searchBy POD_IDENTIFIER)
 * CreateReconnectionTableResponse.checked / unableToUncheck.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/PDT-3409-reconnection-draft-disappearing-pods.spec.ts
 * - tests/cursor/pdt-3042-cancellation-pod-auto-check.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import type { TestRunSummaryCollector } from './shared/test-run-summary.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { PDT_3179_TEST_TIMEOUT_MS } from './pdt-3179-reconnection-invoice-emails.fixtures';
import {
  PDT_3445_KEY,
  PDT_3445_TITLE,
  createPaidNotDisconnectedRfdPod,
  disconnectedPaidMustBePreselected,
  executeDisconnectionForCreatedPod,
  loadCreateListingRow,
  payOpenCustomerLiabilities,
  notDisconnectedPaidMustNotBePreselected,
  pdt3445RelevantKeys,
  rowSnapshot,
  type Pdt3445Fx,
} from './pdt-3445-reconnection-create-not-disconnected-checked.fixtures';

const JIRA_TITLE = PDT_3445_TITLE;

test.describe(`[${PDT_3445_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@receivableManagement', '@pdt-3445'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test(`[${PDT_3445_KEY}]: ${JIRA_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3179_TEST_TIMEOUT_MS);
    const fx: Pdt3445Fx = { Request, GeneratePayload, Responses, Endpoints };
    const tablePath = `${Endpoints.reconnectionOfPowerSupply}/table`;

    const created = await test.step(
      'Precondition: create customer, POD, liability, executed RFD, pay liability to 0 (no disconnection yet)',
      async () => createPaidNotDisconnectedRfdPod(fx),
    );
    TestRunSummary.registerPayload('createdPod', created);

    const beforeDisconnect = await test.step(
      'GET /reconnection-of-the-power-supply/table for the created POD before disconnection',
      async () => loadCreateListingRow(fx, tablePath, created),
    );
    const notDisconnected = notDisconnectedPaidMustNotBePreselected(beforeDisconnect.row);
    TestRunSummary.registerPayload('createListingBeforeDisconnection', {
      query: beforeDisconnect.query,
      status: beforeDisconnect.status,
      rowCount: beforeDisconnect.rowCount,
      row: rowSnapshot(beforeDisconnect.row),
    });
    TestRunSummary.recordCheck({
      check: 'Paid POD that is not disconnected is not preselected on Create',
      expectedResult:
        'Create listing must not return this POD with checked=true or unableToUncheck=true. ' +
        'A missing row is correct: the listing requires an executed disconnection.',
      actualResult: notDisconnected.actualResult,
      passed: notDisconnected.passed,
    });
    expect
      .soft(notDisconnected.passed, notDisconnected.actualResult)
      .toBe(true);
    expect
      .soft(beforeDisconnect.row?.checked === true, notDisconnected.actualResult)
      .toBe(false);
    expect
      .soft(beforeDisconnect.row?.unableToUncheck === true, notDisconnected.actualResult)
      .toBe(false);

    const disconnected = await test.step(
      'Precondition: execute disconnection of power supply for the same POD',
      async () => executeDisconnectionForCreatedPod(fx, created.rfdId),
    );
    TestRunSummary.registerPayload('disconnectionOfPowerSupply', disconnected);

    const taxLiabilitiesPaid = await test.step(
      'Precondition: pay disconnection-tax liabilities so the POD liability sum is 0',
      async () => payOpenCustomerLiabilities(fx, created.customerIdentifier),
    );
    TestRunSummary.registerPayload('taxLiabilitiesPaid', taxLiabilitiesPaid);

    const afterDisconnect = await test.step(
      'GET /reconnection-of-the-power-supply/table for the same POD after disconnection',
      async () => loadCreateListingRow(fx, tablePath, created),
    );
    const mustBeChecked = disconnectedPaidMustBePreselected(afterDisconnect.row);
    TestRunSummary.registerPayload('createListingAfterDisconnection', {
      query: afterDisconnect.query,
      status: afterDisconnect.status,
      rowCount: afterDisconnect.rowCount,
      row: rowSnapshot(afterDisconnect.row),
    });
    TestRunSummary.recordCheck({
      check: 'Disconnected POD with liability sum 0 is preselected and locked on Create',
      expectedResult:
        'After the disconnection-tax liabilities are paid, the Create listing row has checked=true and unableToUncheck=true.',
      actualResult: mustBeChecked.actualResult,
      passed: mustBeChecked.passed,
    });
    expect.soft(Boolean(afterDisconnect.row), mustBeChecked.actualResult).toBe(true);
    expect.soft(mustBeChecked.checked, mustBeChecked.actualResult).toBe(true);
    expect
      .soft(afterDisconnect.row?.unableToUncheck, 'unableToUncheck must be true for a disconnected paid POD')
      .toBe(true);

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary as TestRunSummaryCollector, Responses, {
        jiraKey: PDT_3445_KEY,
        relevantEntityKeys: pdt3445RelevantKeys(),
        snapshot: {
          podId: created.podId,
          podIdentifier: created.podIdentifier,
          customerIdentifier: created.customerIdentifier,
          gridOperatorId: created.gridOperatorId,
          rfdId: created.rfdId,
          liabilityId: created.liabilityId,
          remainingAfterPay: created.remainingAfterPay,
          beforeDisconnection: rowSnapshot(beforeDisconnect.row),
          afterDisconnection: rowSnapshot(afterDisconnect.row),
          notDisconnectedPassed: notDisconnected.passed,
          disconnectedPaidPassed: mustBeChecked.passed,
        },
      });
    });
  });
});
