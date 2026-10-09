/**
 * PDT-3421 — Request for disconnection: uncheck highest-consumption POD.
 *
 * Test 1 (GE / Giorgi): same liability customer + contract; uncheck highest and
 * check lower PODs; save DRAFT must not 500 Duplicate key.
 *
 * Test 2 (Kalina, 2026-09-04): customer A has >1 POD; customer B is also on the
 * reminder. Edit DRAFT, uncheck only A's highest (A's other PODs stay unchecked,
 * B not sent as checked). Save DRAFT must not reassign the check to B's POD.
 * Asserts documented product behavior — current deleteAndAddNewPods +
 * saveCheckedPods re-expands remaining highest-per-customer, so this test is
 * expected to FAIL until that auto-check is fixed (same pattern as PDT-3409).
 *
 * Test 3 (sticky flag, 2026-09-04 UI): create DRAFT with highest-consumption
 * filter on (pods=[]). Then PUT only A's non-highest POD in pods[] while the
 * flag stays true and excludePodIds stays empty. Expected: only that POD stays
 * checked. Current backend re-selects remaining highest PODs.
 *
 * Bug-only automation (no TC .md). Creates all data from scratch.
 *
 * Swagger refresh: update-swagger-specs.ps1 this session (all envs OK including dev).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3179-reconnection-invoice-emails.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/pdt-3113-incorrect-interim-generation.fixtures.ts
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 * - tests/cursor/PDT-3409-reconnection-draft-disappearing-pods.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3421_KEY,
  PDT_3421_TITLE,
  PDT_3421_TEST_TIMEOUT_MS,
  addAndActivateExtraPodsOnSameBillingGroup,
  buildAllPodRows,
  buildPodRowForIndex,
  buildPodRowForParty,
  createDraftRfdWithAllPodsChecked,
  createDraftRfdWithCheckedPods,
  createDraftRfdWithHighestFilterOn,
  createExecutedReminder,
  createExecutedReminderForIdentifiers,
  createOverdueManualLiabilityWithBillingGroup,
  createSecondCustomerSupplyAndLiability,
  createSupplyChain,
  customerIdentOf,
  customerIdentifier,
  entityId,
  errorHaystack,
  getCheckedPodIds,
  getRfd,
  pdt3421RelevantKeys,
  putDraftStickyHighestOnlyThesePods,
  putDraftUncheckHighestCheckLower,
  putDraftUncheckHighestLeaveOthersUnchecked,
  type Pdt3421Fx,
} from './pdt-3421-rfd-highest-consumption-uncheck.fixtures';

const JIRA_TITLE = PDT_3421_TITLE;

function titleFor(scenario: string): string {
  return `[${PDT_3421_KEY}]: ${JIRA_TITLE} | ${scenario}`;
}

test.describe(`[${PDT_3421_KEY}]: ${JIRA_TITLE}`, {
  tag: ['@dev', '@receivableManagement', '@pdt-3421'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test(`[${PDT_3421_KEY}]: ${JIRA_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3421_TEST_TIMEOUT_MS);
    const fx: Pdt3421Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: supply chain (customer, product, contract, POD #1)', async () => {
      await createSupplyChain(fx);
    });

    await test.step('Precondition: two extra PODs on the same billing group / contract', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 2);
      expect(Responses.pod.length, 'GE needs ≥3 PODs on one customer+contract').toBeGreaterThanOrEqual(3);
    });

    await test.step('Precondition: overdue billing-group liability (shared across PODs)', async () => {
      await createOverdueManualLiabilityWithBillingGroup(fx);
    });

    await test.step('Precondition: reminder for disconnection (INCLUDED customer, no /job)', async () => {
      await createExecutedReminder(fx);
    });

    const podRows = await test.step('Build RFD POD rows from GET /pod (same customer + contract)', async () => {
      const rows = await buildAllPodRows(fx);
      TestRunSummary.registerPayload('pods', rows.map((r) => ({
        podId: r.podId,
        podIdentifier: r.podIdentifier,
        customerId: r.customerId,
      })));
      return rows;
    });

    const highest = podRows[0];
    const lower = podRows.slice(1);
    expect(lower.length, 'need at least two lower-consumption PODs to check').toBeGreaterThanOrEqual(2);

    const rfdId = await test.step('Create DRAFT RFD with all three PODs checked (filter off)', async () => {
      const id = await createDraftRfdWithAllPodsChecked(fx, podRows);
      TestRunSummary.registerPayload('rfdDraft', { id, customer: customerIdentifier(Responses) });
      expect(id).toBeGreaterThan(0);
      return id;
    });

    const putBody = await test.step(
      'PUT DRAFT: podWithHighestConsumption=true, exclude highest POD, check lower PODs',
      async () => {
        TestRunSummary.registerPayload('putDraft', {
          rfdId,
          podWithHighestConsumption: true,
          excludePodIds: [highest.podId],
          checkedLowerPodIds: lower.map((r) => r.podId),
        });
        return putDraftUncheckHighestCheckLower(fx, rfdId, highest, lower);
      },
    );

    const putHaystack = errorHaystack(putBody.status, putBody.text, putBody.json);
    const isDuplicateKey =
      /Duplicate key/i.test(putHaystack) ||
      /IllegalStateException/i.test(putHaystack) ||
      /APPLICATION_ERROR/i.test(putHaystack);
    const putOk = putBody.status >= 200 && putBody.status < 300 && !isDuplicateKey;

    TestRunSummary.recordCheck({
      check: 'PUT DRAFT uncheck highest / check lower PODs does not 500 Duplicate key',
      expectedResult:
        'HTTP 2xx. System must not return APPLICATION_ERROR / IllegalStateException Duplicate key on podId.',
      actualResult: putOk
        ? `As expected — HTTP ${putBody.status}.`
        : `Not as expected — HTTP ${putBody.status}: ${putHaystack.slice(0, 500)}`,
      passed: putOk,
    });
    expect(
      putOk,
      `GE expected save without error. PUT ${putBody.status}: ${putHaystack.slice(0, 800)}`,
    ).toBe(true);

    await test.step('GET RFD stays DRAFT after save', async () => {
      const rfd = await getRfd(fx, rfdId);
      const status = String(rfd.disconnectionRequestsStatus ?? '');
      TestRunSummary.recordCheck({
        check: 'RFD remains DRAFT after PUT',
        expectedResult: 'disconnectionRequestsStatus = DRAFT',
        actualResult: status === 'DRAFT'
          ? 'As expected — DRAFT.'
          : `Not as expected — ${status}`,
        passed: status === 'DRAFT',
      });
      expect(status, 'GET RFD disconnectionRequestsStatus').toBe('DRAFT');
    });

    await test.step('Checked POD list contains lower PODs and not the excluded highest', async () => {
      const checkedIds = await getCheckedPodIds(fx, rfdId);
      const lowerIds = lower.map((r) => r.podId);
      const hasLower = lowerIds.every((id) => checkedIds.includes(id));
      const highestStillChecked = checkedIds.includes(highest.podId);
      TestRunSummary.recordCheck({
        check: 'Lower PODs stay checked; highest POD is not kept as checked',
        expectedResult: `checked contains ${lowerIds.join(', ')}; excludes ${highest.podId}`,
        actualResult: `checked=[${checkedIds.join(', ')}]`,
        passed: hasLower && !highestStillChecked,
      });
      expect(hasLower, `checked PODs ${checkedIds} must include lower ${lowerIds}`).toBe(true);
      expect(
        highestStillChecked,
        `excluded highest POD ${highest.podId} must not remain in checked list ${checkedIds}`,
      ).toBe(false);
    });

    await test.step('Attach test run summary', async () => {
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      if (Responses.requestForDisconnection.length) {
        TestRunSummary.registerPayload('requestForDisconnection', Responses.requestForDisconnection[0]);
      }
      let extra: Record<string, string[]> | undefined;
      try {
        extra = buildProductContractTabLinks(entityId(Responses.productContract[0]));
      } catch {
        extra = undefined;
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3421_KEY,
        relevantEntityKeys: pdt3421RelevantKeys(),
        extraLinks: extra && Object.keys(extra).length ? extra : undefined,
        snapshot: {
          rfdId,
          customerIdentifier: customerIdentifier(Responses),
          highestPodId: highest.podId,
          lowerPodIds: lower.map((r) => r.podId),
          putStatus: putBody.status,
        },
      });
    });
  });

  test(titleFor('uncheck highest only — next customer POD stays unchecked'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3421_TEST_TIMEOUT_MS);
    const fx: Pdt3421Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: customer A supply chain (1 POD)', async () => {
      await createSupplyChain(fx);
    });

    await test.step('Precondition: customer A extra POD on the same billing group / contract', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'Kalina needs customer A with >1 POD').toBeGreaterThanOrEqual(2);
    });

    await test.step('Precondition: customer A overdue billing-group liability', async () => {
      await createOverdueManualLiabilityWithBillingGroup(fx);
    });

    const customerB = await test.step(
      'Precondition: customer B supply + 1 POD + overdue liability',
      async () => createSecondCustomerSupplyAndLiability(fx),
    );

    const identA = customerIdentOf(Responses.customer[0]);
    const identB = customerB.identifier;
    const listOfCustomer = [identA, identB].filter((id) => id.length > 0).join(',');
    expect(identA.length, 'customer A identifier').toBeGreaterThan(0);
    expect(identB.length, 'customer B identifier').toBeGreaterThan(0);

    await test.step('Precondition: reminder INCLUDED for both customers (no /job)', async () => {
      const reminderId = await createExecutedReminderForIdentifiers(fx, [identA, identB]);
      TestRunSummary.registerPayload('reminderForDisconnection', {
        reminderId,
        customerList: listOfCustomer,
      });
      expect(reminderId).toBeGreaterThan(0);
    });

    const highestA = await test.step('Build customer A highest POD row (first POD)', async () => {
      const row = await buildPodRowForIndex(fx, 0);
      TestRunSummary.registerPayload('customerAHighestPod', {
        podId: row.podId,
        customerId: row.customerId,
        identifier: identA,
      });
      return row;
    });

    const otherAPodId = await test.step('Resolve customer A other (unchecked) POD id', async () => {
      const other = await buildPodRowForIndex(fx, 1);
      TestRunSummary.registerPayload('customerAOtherPod', {
        podId: other.podId,
        customerId: other.customerId,
      });
      return other.podId;
    });

    const podB = await test.step('Build customer B POD row (not sent as checked)', async () => {
      const row = await buildPodRowForParty(fx, {
        podIndex: customerB.podIndex,
        customerIndex: customerB.customerIndex,
        contractIndex: customerB.contractIndex,
        liabilityIndex: customerB.liabilityIndex,
      });
      TestRunSummary.registerPayload('customerBPod', {
        podId: row.podId,
        customerId: row.customerId,
        identifier: identB,
      });
      return row;
    });

    const rfdId = await test.step(
      'Create DRAFT RFD with only customer A highest POD checked (filter off)',
      async () => {
        const id = await createDraftRfdWithCheckedPods(fx, [highestA], listOfCustomer);
        TestRunSummary.registerPayload('rfdDraft', { id, listOfCustomer });
        expect(id).toBeGreaterThan(0);
        return id;
      },
    );

    const putBody = await test.step(
      'PUT DRAFT: highest filter on, exclude A highest, pods=[] (A others + B unchecked)',
      async () => {
        TestRunSummary.registerPayload('putDraft', {
          rfdId,
          podWithHighestConsumption: true,
          excludePodIds: [highestA.podId],
          pods: [],
        });
        return putDraftUncheckHighestLeaveOthersUnchecked(fx, rfdId, highestA, listOfCustomer);
      },
    );

    const putHaystack = errorHaystack(putBody.status, putBody.text, putBody.json);
    const isDuplicateKey =
      /Duplicate key/i.test(putHaystack) ||
      /IllegalStateException/i.test(putHaystack) ||
      /APPLICATION_ERROR/i.test(putHaystack);
    const putOk = putBody.status >= 200 && putBody.status < 300 && !isDuplicateKey;

    TestRunSummary.recordCheck({
      check: 'PUT DRAFT uncheck highest only does not 500 Duplicate key',
      expectedResult:
        'HTTP 2xx. System must not return APPLICATION_ERROR / IllegalStateException Duplicate key on podId.',
      actualResult: putOk
        ? `As expected — HTTP ${putBody.status}.`
        : `Not as expected — HTTP ${putBody.status}: ${putHaystack.slice(0, 500)}`,
      passed: putOk,
    });
    expect(
      putOk,
      `Kalina save must succeed. PUT ${putBody.status}: ${putHaystack.slice(0, 800)}`,
    ).toBe(true);

    await test.step('GET RFD stays DRAFT after save', async () => {
      const rfd = await getRfd(fx, rfdId);
      const status = String(rfd.disconnectionRequestsStatus ?? '');
      TestRunSummary.recordCheck({
        check: 'RFD remains DRAFT after PUT',
        expectedResult: 'disconnectionRequestsStatus = DRAFT',
        actualResult: status === 'DRAFT'
          ? 'As expected — DRAFT.'
          : `Not as expected — ${status}`,
        passed: status === 'DRAFT',
      });
      expect(status, 'GET RFD disconnectionRequestsStatus').toBe('DRAFT');
    });

    await test.step(
      'Checked POD list does not reassign A highest to customer B (or A other PODs)',
      async () => {
        const checkedIds = await getCheckedPodIds(fx, rfdId);
        const aHighestChecked = checkedIds.includes(highestA.podId);
        const aOtherChecked = checkedIds.includes(otherAPodId);
        const bChecked = checkedIds.includes(podB.podId);
        const noneReassigned = !aHighestChecked && !aOtherChecked && !bChecked;

        TestRunSummary.recordCheck({
          check: 'Unchecking A highest does not check the next customer POD',
          expectedResult:
            `checked excludes A highest ${highestA.podId}, A other ${otherAPodId}, and B ${podB.podId}`,
          actualResult: `checked=[${checkedIds.join(', ')}]`,
          passed: noneReassigned,
        });
        expect(
          bChecked,
          `Kalina: next customer POD ${podB.podId} must stay unchecked; checked=${checkedIds.join(', ')}`,
        ).toBe(false);
        expect(
          aHighestChecked,
          `excluded A highest ${highestA.podId} must not remain checked; checked=${checkedIds.join(', ')}`,
        ).toBe(false);
        expect(
          aOtherChecked,
          `A other POD ${otherAPodId} was never checked and must stay unchecked; checked=${checkedIds.join(', ')}`,
        ).toBe(false);
      },
    );

    await test.step('Attach test run summary', async () => {
      TestRunSummary.registerPayload('customerA', Responses.customer[0]);
      TestRunSummary.registerPayload('customerB', Responses.customer[customerB.customerIndex]);
      if (Responses.requestForDisconnection.length) {
        TestRunSummary.registerPayload(
          'requestForDisconnection',
          Responses.requestForDisconnection[Responses.requestForDisconnection.length - 1],
        );
      }
      let extra: Record<string, string[]> | undefined;
      try {
        const linksA = buildProductContractTabLinks(entityId(Responses.productContract[0]));
        const linksB = buildProductContractTabLinks(
          entityId(Responses.productContract[customerB.contractIndex]),
        );
        extra = {
          productContract: [
            ...(linksA.productContract ?? []),
            ...(linksB.productContract ?? []),
          ],
        };
      } catch {
        extra = undefined;
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3421_KEY,
        relevantEntityKeys: pdt3421RelevantKeys(),
        extraLinks: extra && Object.keys(extra).length ? extra : undefined,
        snapshot: {
          rfdId,
          listOfCustomer,
          highestAPodId: highestA.podId,
          otherAPodId,
          customerBPodId: podB.podId,
          putStatus: putBody.status,
        },
      });
    });
  });

  test(titleFor('sticky highest flag — only non-highest POD checked'), async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3421_TEST_TIMEOUT_MS);
    const fx: Pdt3421Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: customer A supply chain (1 POD)', async () => {
      await createSupplyChain(fx);
    });

    await test.step('Precondition: customer A extra POD on the same billing group / contract', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 1);
      expect(Responses.pod.length, 'customer A must have >1 POD').toBeGreaterThanOrEqual(2);
    });

    await test.step('Precondition: customer A overdue billing-group liability', async () => {
      await createOverdueManualLiabilityWithBillingGroup(fx);
    });

    const customerB = await test.step(
      'Precondition: customer B supply + 1 POD + overdue liability',
      async () => createSecondCustomerSupplyAndLiability(fx),
    );

    const identA = customerIdentOf(Responses.customer[0]);
    const identB = customerB.identifier;
    const listOfCustomer = [identA, identB].filter((id) => id.length > 0).join(',');
    expect(identA.length, 'customer A identifier').toBeGreaterThan(0);
    expect(identB.length, 'customer B identifier').toBeGreaterThan(0);

    await test.step('Precondition: reminder INCLUDED for both customers (no /job)', async () => {
      const reminderId = await createExecutedReminderForIdentifiers(fx, [identA, identB]);
      TestRunSummary.registerPayload('reminderForDisconnection', {
        reminderId,
        customerList: listOfCustomer,
      });
      expect(reminderId).toBeGreaterThan(0);
    });

    const rowA0 = await buildPodRowForIndex(fx, 0);
    const rowA1 = await buildPodRowForIndex(fx, 1);
    const rowB = await buildPodRowForParty(fx, {
      podIndex: customerB.podIndex,
      customerIndex: customerB.customerIndex,
      contractIndex: customerB.contractIndex,
      liabilityIndex: customerB.liabilityIndex,
    });
    TestRunSummary.registerPayload('customerAPods', {
      first: rowA0.podId,
      second: rowA1.podId,
      identifier: identA,
    });
    TestRunSummary.registerPayload('customerBPod', {
      podId: rowB.podId,
      identifier: identB,
    });

    const rfdId = await test.step(
      'Create DRAFT with highest-consumption filter on (button), pods=[]',
      async () => {
        const id = await createDraftRfdWithHighestFilterOn(fx, listOfCustomer);
        TestRunSummary.registerPayload('rfdDraft', {
          id,
          listOfCustomer,
          podWithHighestConsumption: true,
          pods: [],
        });
        expect(id).toBeGreaterThan(0);
        return id;
      },
    );

    const onlyChecked = await test.step(
      'Pick customer A POD that is not auto-checked as highest',
      async () => {
        const autoChecked = await getCheckedPodIds(fx, rfdId);
        TestRunSummary.registerPayload('afterHighestButton', { autoChecked });
        const aRows = [rowA0, rowA1];
        const nonHighest = aRows.find((row) => !autoChecked.includes(row.podId));
        expect(
          nonHighest,
          `customer A must have a POD that is not auto-checked as highest; autoChecked=[${autoChecked.join(', ')}] aPods=[${aRows.map((r) => r.podId).join(', ')}]`,
        ).toBeTruthy();
        TestRunSummary.registerPayload('onlyNonHighestPod', {
          podId: nonHighest!.podId,
          autoChecked,
        });
        return nonHighest!;
      },
    );

    const putBody = await test.step(
      'PUT DRAFT: flag stays true, exclude=[], pods=[only that non-highest POD]',
      async () => {
        TestRunSummary.registerPayload('putDraft', {
          rfdId,
          podWithHighestConsumption: true,
          excludePodIds: [],
          checkedPodIds: [onlyChecked.podId],
        });
        return putDraftStickyHighestOnlyThesePods(fx, rfdId, [onlyChecked], listOfCustomer);
      },
    );

    const putHaystack = errorHaystack(putBody.status, putBody.text, putBody.json);
    const isDuplicateKey =
      /Duplicate key/i.test(putHaystack) ||
      /IllegalStateException/i.test(putHaystack) ||
      /APPLICATION_ERROR/i.test(putHaystack);
    const putOk = putBody.status >= 200 && putBody.status < 300 && !isDuplicateKey;

    TestRunSummary.recordCheck({
      check: 'PUT DRAFT sticky highest flag does not 500 Duplicate key',
      expectedResult:
        'HTTP 2xx. System must not return APPLICATION_ERROR / IllegalStateException Duplicate key on podId.',
      actualResult: putOk
        ? `As expected — HTTP ${putBody.status}.`
        : `Not as expected — HTTP ${putBody.status}: ${putHaystack.slice(0, 500)}`,
      passed: putOk,
    });
    expect(
      putOk,
      `Sticky-flag save must succeed. PUT ${putBody.status}: ${putHaystack.slice(0, 800)}`,
    ).toBe(true);

    await test.step('GET RFD stays DRAFT after save', async () => {
      const rfd = await getRfd(fx, rfdId);
      const status = String(rfd.disconnectionRequestsStatus ?? '');
      TestRunSummary.recordCheck({
        check: 'RFD remains DRAFT after PUT',
        expectedResult: 'disconnectionRequestsStatus = DRAFT',
        actualResult: status === 'DRAFT'
          ? 'As expected — DRAFT.'
          : `Not as expected — ${status}`,
        passed: status === 'DRAFT',
      });
      expect(status, 'GET RFD disconnectionRequestsStatus').toBe('DRAFT');
    });

    await test.step(
      'Checked POD list contains only the one POD sent in pods[]',
      async () => {
        const checkedIds = await getCheckedPodIds(fx, rfdId);
        const extras = checkedIds.filter((id) => id !== onlyChecked.podId);
        const onlyThatOne = checkedIds.length === 1 && checkedIds[0] === onlyChecked.podId;
        TestRunSummary.recordCheck({
          check: 'Sticky highest flag does not re-check other highest PODs',
          expectedResult: `checked=[${onlyChecked.podId}] only`,
          actualResult: `checked=[${checkedIds.join(', ')}]`,
          passed: onlyThatOne,
        });
        expect(
          extras,
          `sticky flag: extra PODs ${extras.join(', ')} must stay unchecked; sent pods=[${onlyChecked.podId}]; checked=${checkedIds.join(', ')}`,
        ).toEqual([]);
        expect(checkedIds, `checked must be exactly [${onlyChecked.podId}]`).toEqual([
          onlyChecked.podId,
        ]);
      },
    );

    await test.step('Attach test run summary', async () => {
      TestRunSummary.registerPayload('customerA', Responses.customer[0]);
      TestRunSummary.registerPayload('customerB', Responses.customer[customerB.customerIndex]);
      if (Responses.requestForDisconnection.length) {
        TestRunSummary.registerPayload(
          'requestForDisconnection',
          Responses.requestForDisconnection[Responses.requestForDisconnection.length - 1],
        );
      }
      let extra: Record<string, string[]> | undefined;
      try {
        const linksA = buildProductContractTabLinks(entityId(Responses.productContract[0]));
        const linksB = buildProductContractTabLinks(
          entityId(Responses.productContract[customerB.contractIndex]),
        );
        extra = {
          productContract: [
            ...(linksA.productContract ?? []),
            ...(linksB.productContract ?? []),
          ],
        };
      } catch {
        extra = undefined;
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3421_KEY,
        relevantEntityKeys: pdt3421RelevantKeys(),
        extraLinks: extra && Object.keys(extra).length ? extra : undefined,
        snapshot: {
          rfdId,
          listOfCustomer,
          onlyCheckedPodId: onlyChecked.podId,
          customerBPodId: rowB.podId,
          putStatus: putBody.status,
        },
      });
    });
  });
});
