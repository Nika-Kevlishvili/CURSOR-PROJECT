/**
 * PDT-3465 — setup-only DRAFT RFD for a manual Dev walk of:
 * uncheck Highest=YES POD, Save as Draft, confirm no other NO POD auto-checks.
 *
 * This spec CREATES DATA and STOPS. It does not PUT uncheck / does not reproduce
 * the save-time reselect. Tester uses portal links from finalizeTestRunSummary.
 *
 * Data (Kalina / ticket):
 * - Customer A: ≥3 PODs on the same billing group + same product contract,
 *   overdue BG liability. Highest-consumption filter selects one YES; others NO.
 * - Customer B: 1 POD + overdue liability, same reminder (leave B YES checked
 *   while unchecking A YES in UI).
 * - Reminder INCLUDED for both identifiers. Do not POST reminder /job
 *   (PDT-3179: select-all SQL is huge).
 * - POST DRAFT with podWithHighestConsumption=true, pods=[], excludePodIds=[]
 *   (same as UI "Select POD's with highest consumption"). Leave DRAFT.
 *
 * Bug-only (no Backend TC .md). Environment: Dev.
 *
 * Swagger (dev, update-swagger-specs.ps1 this session — all envs OK):
 * - POST /disconnection-of-power-supply-requests DPSRequestsBaseRequest
 *   required: conditionType, gridOpRequestRegDate, gridOperatorId,
 *   reasonOfDisconnectionId, reminderForDisconnectionId, supplierType
 *   disconnectionRequestsStatus enum: DRAFT | EXECUTED | FEE_CHARGED
 *   conditionType enum: ALL_CUSTOMERS | CUSTOMERS_UNDER_CONDITIONS | LIST_OF_CUSTOMERS
 *   supplierType enum: CURRENT | PREVIOUS
 *   pods[] = CustomersForDPSResponse (podId, isChecked, isHighestConsumption, …)
 *   excludePodIds integer[], podWithHighestConsumption boolean, allSelected boolean
 * - PUT /disconnection-of-power-supply-requests/{id} same DPSRequestsBaseRequest
 *   — not called here (setup-only).
 * - GET /disconnection-of-power-supply-requests/{id}
 * - GET /disconnection-of-power-supply-requests/get-checked-pods/{reminderId}/{gridOperatorId}/{id}
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3421-rfd-highest-consumption-uncheck.spec.ts
 * - tests/cursor/pdt-3421-rfd-highest-consumption-uncheck.fixtures.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3421_TEST_TIMEOUT_MS,
  addAndActivateExtraPodsOnSameBillingGroup,
  buildPodRowForIndex,
  buildPodRowForParty,
  createDraftRfdWithHighestFilterOn,
  createExecutedReminderForIdentifiers,
  createOverdueManualLiabilityWithBillingGroup,
  createSecondCustomerSupplyAndLiability,
  createSupplyChain,
  customerIdentOf,
  customerIdentifier,
  entityId,
  getCheckedPodIds,
  getRfd,
  pdt3421RelevantKeys,
  type Pdt3421Fx,
} from './pdt-3421-rfd-highest-consumption-uncheck.fixtures';

const PDT_3465_KEY = 'PDT-3465';
const PDT_3465_TITLE =
  '[Backend] - Request for disconnection - Unchecking the highest POD checks another non-highest POD on save';

/** ResponseLinker Dev UI prefix — rewritePortalLinksForDisplay maps this to phoenix1-dev. */
const DEV_UI_PREFIX = 'http://10.236.20.11:8080';

function relevantKeysForManualWalk(): string[] {
  return [
    ...pdt3421RelevantKeys(),
    'reminderForDisconnection',
    'requestForDisconnection',
  ];
}

function extraPortalLinks(opts: {
  rfdId: number;
  reminderId: number;
  customerAId: number;
  customerBId: number;
  podIds: number[];
  contractAId: number;
  contractBId: number;
}): Record<string, string[]> {
  const extra: Record<string, string[]> = {
    requestForDisconnection: [`${DEV_UI_PREFIX}/request-for-disconnection/preview?id=${opts.rfdId}`],
    reminderForDisconnection: [`${DEV_UI_PREFIX}/reminder-for-disconnection/preview?id=${opts.reminderId}`],
    customer: [
      `${DEV_UI_PREFIX}/customers/preview/basic?id=${opts.customerAId}`,
      `${DEV_UI_PREFIX}/customers/preview/basic?id=${opts.customerBId}`,
    ],
    pod: opts.podIds.map((id) => `${DEV_UI_PREFIX}/points-of-delivery/preview?id=${id}`),
  };
  try {
    const linksA = buildProductContractTabLinks(opts.contractAId);
    const linksB = buildProductContractTabLinks(opts.contractBId);
    extra.productContract = [
      ...(linksA.productContract ?? []),
      ...(linksB.productContract ?? []),
    ];
  } catch {
    /* contract tab links need FRONTEND_BASE_URL; RFD/reminder/customer/pod still dump */
  }
  return extra;
}

test.describe(`[${PDT_3465_KEY}]: ${PDT_3465_TITLE}`, {
  tag: ['@dev', '@receivableManagement', '@pdt-3465'],
}, () => {
  test.describe.configure({ fullyParallel: false });

  test(`[${PDT_3465_KEY}]: ${PDT_3465_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3421_TEST_TIMEOUT_MS);
    const fx: Pdt3421Fx = { Request, GeneratePayload, Responses, Endpoints };

    await test.step('Precondition: customer A supply chain (customer, product, contract, POD #1)', async () => {
      await createSupplyChain(fx);
    });

    await test.step('Precondition: two extra PODs on customer A same billing group / contract', async () => {
      await addAndActivateExtraPodsOnSameBillingGroup(fx, 2);
      expect(
        Responses.pod.length,
        'customer A needs ≥3 PODs on one billing group + product contract',
      ).toBeGreaterThanOrEqual(3);
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
    TestRunSummary.registerPayload('customerAIdentifier', identA);
    TestRunSummary.registerPayload('customerBIdentifier', identB);

    const reminderId = await test.step(
      'Precondition: reminder INCLUDED for both customers (no /job)',
      async () => {
        const id = await createExecutedReminderForIdentifiers(fx, [identA, identB]);
        TestRunSummary.registerPayload('reminderForDisconnection', {
          reminderId: id,
          customerList: listOfCustomer,
          customerFilterType: 'INCLUDED',
        });
        expect(id).toBeGreaterThan(0);
        return id;
      },
    );

    const highestA = await test.step('Resolve customer A first/highest POD row', async () => {
      const row = await buildPodRowForIndex(fx, 0);
      TestRunSummary.registerPayload('customerAHighestPod', {
        podId: row.podId,
        customerId: row.customerId,
        identifier: identA,
        expectedHighestYes: true,
      });
      return row;
    });

    const otherAPodIds = await test.step('Resolve customer A other (expected Highest=NO) POD ids', async () => {
      const row1 = await buildPodRowForIndex(fx, 1);
      const row2 = await buildPodRowForIndex(fx, 2);
      const ids = [row1.podId, row2.podId];
      TestRunSummary.registerPayload('customerAOtherPods', {
        podIds: ids,
        expectedHighestYes: false,
      });
      return ids;
    });

    const podB = await test.step('Resolve customer B POD row', async () => {
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

    const allPodIds = [
      highestA.podId,
      ...otherAPodIds,
      podB.podId,
    ];
    TestRunSummary.registerPayload('allPodIds', allPodIds);

    const rfdId = await test.step(
      'Create DRAFT RFD with highest-consumption filter on (pods=[], excludePodIds=[])',
      async () => {
        const id = await createDraftRfdWithHighestFilterOn(fx, listOfCustomer);
        TestRunSummary.registerPayload('requestForDisconnection', {
          id,
          listOfCustomer,
          disconnectionRequestsStatus: 'DRAFT',
          podWithHighestConsumption: true,
          pods: [],
          excludePodIds: [],
          allSelected: false,
        });
        expect(id).toBeGreaterThan(0);
        return id;
      },
    );

    await test.step('GET RFD is DRAFT (setup-only — no PUT uncheck, no execute)', async () => {
      const rfd = await getRfd(fx, rfdId);
      const status = String(rfd.disconnectionRequestsStatus ?? '');
      TestRunSummary.recordCheck({
        check: 'RFD left DRAFT for manual UI walk',
        expectedResult: 'disconnectionRequestsStatus = DRAFT. Spec must not PUT uncheck or execute.',
        actualResult: status === 'DRAFT'
          ? 'As expected — DRAFT; no PUT uncheck in this spec.'
          : `Not as expected — ${status}`,
        passed: status === 'DRAFT',
      });
      expect(status, 'GET RFD disconnectionRequestsStatus').toBe('DRAFT');
    });

    await test.step(
      'Checked PODs include A highest; record B if highest-filter selected it (do not fail if B unchecked)',
      async () => {
        const checkedIds = await getCheckedPodIds(fx, rfdId);
        const aHighestChecked = checkedIds.includes(highestA.podId);
        const bChecked = checkedIds.includes(podB.podId);
        TestRunSummary.registerPayload('checkedPodIdsAfterCreate', checkedIds);

        TestRunSummary.recordCheck({
          check: 'Customer A first/highest POD is checked after highest-consumption filter',
          expectedResult: `checked includes A highest ${highestA.podId} (Highest = YES for A)`,
          actualResult: aHighestChecked
            ? `As expected — checked=[${checkedIds.join(', ')}]`
            : `Not as expected — A highest ${highestA.podId} missing; checked=[${checkedIds.join(', ')}]`,
          passed: aHighestChecked,
        });
        expect(
          aHighestChecked,
          `A first/highest POD ${highestA.podId} must be checked; checked=${checkedIds.join(', ')}`,
        ).toBe(true);

        TestRunSummary.recordCheck({
          check: 'Customer B POD may already be Highest=YES (manual: leave B checked while unchecking A)',
          expectedResult:
            `B POD ${podB.podId} is checked if the highest-consumption filter selected it; still dump links if not.`,
          actualResult: bChecked
            ? `As expected — B ${podB.podId} is checked.`
            : `B ${podB.podId} is not in checked=[${checkedIds.join(', ')}]; data is still ready for manual UI.`,
          passed: true,
        });

        TestRunSummary.recordCheck({
          check: 'Manual walk data is ready (uncheck A YES, Save as Draft, confirm no NO POD auto-check)',
          expectedResult:
            'Open DRAFT RFD POD tab: uncheck customer A Highest=YES, leave B YES if checked, Save as Draft; no other Highest=NO POD should auto-check.',
          actualResult:
            `Ready — rfdId=${rfdId} reminderId=${reminderId} identA=${identA} identB=${identB} ` +
            `highestAPodId=${highestA.podId} otherA=[${otherAPodIds.join(', ')}] podB=${podB.podId} ` +
            `checked=[${checkedIds.join(', ')}]`,
          passed: true,
        });
      },
    );

    await test.step('Attach test run summary', async () => {
      TestRunSummary.registerPayload('customerA', Responses.customer[0]);
      TestRunSummary.registerPayload('customerB', Responses.customer[customerB.customerIndex]);
      TestRunSummary.registerPayload('customerAIdentifierPayload', customerIdentifier(Responses));
      if (Responses.requestForDisconnection.length) {
        TestRunSummary.registerPayload(
          'requestForDisconnectionId',
          Responses.requestForDisconnection[Responses.requestForDisconnection.length - 1],
        );
      }

      const extra = extraPortalLinks({
        rfdId,
        reminderId,
        customerAId: entityId(Responses.customer[0]),
        customerBId: customerB.customerId,
        podIds: allPodIds,
        contractAId: entityId(Responses.productContract[0]),
        contractBId: entityId(Responses.productContract[customerB.contractIndex]),
      });

      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3465_KEY,
        relevantEntityKeys: relevantKeysForManualWalk(),
        extraLinks: extra,
        snapshot: {
          rfdId,
          reminderId,
          identA,
          identB,
          podIds: allPodIds,
          highestAPodId: highestA.podId,
        },
      });
    });
  });
});
