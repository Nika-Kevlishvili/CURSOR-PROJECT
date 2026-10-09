/**
 * PDT-3211 — Request for disconnection supplier type PREVIOUS additional logic.
 *
 * Bug-only (no TC .md). Creates all data from scratch. No test.beforeAll.
 *
 * Runtime BASE_URL Dev2: http://10.236.20.11:8092
 *   (also https://devapps.energo-pro.bg/backend/phoenix-dev2)
 * Canonical OpenAPI (parent refreshed this session):
 *   Cursor-Project/config/swagger/dev2/swagger-spec.json
 *
 * Swagger findings (grep of that spec — every endpoint this spec calls):
 * - POST /disconnection-of-power-supply-requests → DPSRequestsBaseRequest
 *   required includes supplierType, validityPeriodFrom, validityPeriodTo
 *   (Cursor requestForDisconnection.ts omits validityPeriod* — tests set them)
 * - GET /disconnection-of-power-supply-requests/load-customer-for-disconnection-power-supply
 *   CustomersForDPSRequest required: conditionType, page, size, supplierType
 *   Phoenix HTTP 206; CheckResponse treats 200–299 as success. Do not assert 200.
 * - GET /disconnection-of-power-supply-requests/view-pod-tab/{id}
 * - POST /power-supply-disconnection-reminder → PowerSupplyDisconnectionReminderBaseRequest
 * - POST /customer-liability, POST /contract-pods/manual, POST /product-contract,
 *   POST /pod, POST /customer
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2971-rfd-shared-pod-multi-customer-merge.spec.ts
 * - tests/cursor/pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts
 * - tests/cursor/pdt-2913-missing-email-object-reminder-for-disconnection.fixtures.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/cursor/pdt-3090-rfd-highest-consumption-fallback.fixtures.ts
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import {
  CONTRACT_A_INDEX,
  CONTRACT_B_INDEX,
  CUSTOMER_A_INDEX,
  CUSTOMER_B_INDEX,
  PDT_3211_KEY,
  PDT_3211_MAIN_TEST_TIMEOUT_MS,
  PDT_3211_SINGLE_CUSTOMER_TIMEOUT_MS,
  PDT_3211_TITLE,
  activateContractPod,
  buildLoadCustomersQuery,
  createDev2ReminderForDisconnection,
  createDraftRfd,
  createOverdueManualLiability,
  createPrivateCustomer,
  createProductContract,
  createSettlementPod,
  createTermAndSupplyProduct,
  customerIdentifierOf,
  entityId,
  executeReminderUntilExecuted,
  fetchLoadCustomersForDps,
  fetchReminderSecondTab,
  fetchViewPodTab,
  pdt3211RelevantKeys,
  resolvePodIdentifier,
  rowsForPod,
  setContractActiveInPerpetuity,
  terminateContract,
  todayYmd,
  yesterdayYmd,
  type Pdt3211Fx,
} from './pdt-3211-rfd-supplier-type-previous-additional-logic.fixtures';

test.describe(`[${PDT_3211_KEY}]: ${PDT_3211_TITLE}`, {
  tag: ['@dev2', '@receivableManagement', '@pdt-3211'],
}, () => {
  test(`[${PDT_3211_KEY}]: ${PDT_3211_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(PDT_3211_MAIN_TEST_TIMEOUT_MS);
    const fx: Pdt3211Fx = { Request, GeneratePayload, Responses, Endpoints };
    const YESTERDAY = yesterdayYmd();
    const TODAY = todayYmd();

    let podIdentifier = '';
    let podId = 0;
    let customerAId = 0;
    let customerAIdentifier = '';
    let customerBIdentifier = '';
    let reminderId = 0;
    let requestDraftId = 0;

    await test.step('Precondition: term + SUPPLY_ONLY product', async () => {
      await createTermAndSupplyProduct(fx);
    });

    await test.step('Precondition: Customer A (former — private UIC, will deactivate POD)', async () => {
      const created = await createPrivateCustomer(fx);
      customerAId = entityId(created.body);
      customerAIdentifier = customerIdentifierOf(created.body);
      TestRunSummary.registerPayload('customer', created.payload);
    });

    await test.step('Precondition: Customer B (successor — different private UIC, POD active today)', async () => {
      const created = await createPrivateCustomer(fx);
      customerBIdentifier = customerIdentifierOf(created.body);
      expect(
        customerBIdentifier,
        'Customer B UIC / Personal Number (customer.identifier) must differ from Customer A',
      ).not.toEqual(customerAIdentifier);
      TestRunSummary.registerPayload('customerB', created.payload);
    });

    await test.step('Precondition: shared settlement POD', async () => {
      const podBody = await createSettlementPod(fx);
      podId = entityId(podBody);
      podIdentifier = await resolvePodIdentifier(fx, 0);
      TestRunSummary.registerPayload('pod', { id: podId, identifier: podIdentifier });
    });

    await test.step('Contract A: create for Customer A on shared POD', async () => {
      await createProductContract(fx, CUSTOMER_A_INDEX, 0, 0);
    });

    await test.step('Contract A: activate POD yesterday + ACTIVE_IN_PERPETUITY', async () => {
      await activateContractPod(fx, {
        contractIndex: CONTRACT_A_INDEX,
        activationDate: YESTERDAY,
      });
      await setContractActiveInPerpetuity(fx, CONTRACT_A_INDEX);
    });

    await test.step('Contract A: overdue unpaid MANUAL liability on billing group', async () => {
      const { payload } = await createOverdueManualLiability(fx, CUSTOMER_A_INDEX, CONTRACT_A_INDEX);
      TestRunSummary.registerPayload('customerLiability', payload);
    });

    await test.step('Contract A: deactivate POD yesterday and terminate (no active contract POD today)', async () => {
      await activateContractPod(fx, {
        contractIndex: CONTRACT_A_INDEX,
        activationDate: YESTERDAY,
        deactivationDate: YESTERDAY,
      });
      await terminateContract(fx, CONTRACT_A_INDEX);
    });

    await test.step('Contract B: create for Customer B on SAME shared POD', async () => {
      await createProductContract(fx, CUSTOMER_B_INDEX, 0, 0);
    });

    await test.step('Contract B: activate POD TODAY (not tomorrow) without deactivation', async () => {
      await activateContractPod(fx, {
        contractIndex: CONTRACT_B_INDEX,
        activationDate: TODAY,
      });
    });

    await test.step('Action: reminder LIST_OF_CUSTOMERS for Customer A identifier only + execute job', async () => {
      const reminder = await createDev2ReminderForDisconnection(fx, customerAIdentifier);
      reminderId = reminder.reminderId;
      TestRunSummary.registerPayload('reminderForDisconnection', reminder.payload);
      await executeReminderUntilExecuted(fx, reminderId);
    });

    await test.step('Setup proof: reminder second-tab lists Customer A (liability selected)', async () => {
      const secondTabRows = await fetchReminderSecondTab(fx, reminderId);
      const rowsA = secondTabRows.filter((r) => (r.customer ?? '').includes(customerAIdentifier));
      expect(
        rowsA.length,
        `Reminder ${reminderId} second-tab must list Customer A ${customerAIdentifier} before Previous assertions`,
      ).toBeGreaterThanOrEqual(1);
      TestRunSummary.recordCheck({
        check: 'Reminder second-tab includes Customer A',
        expectedResult: 'Customer A identifier appears on the executed reminder second-tab.',
        actualResult: `Customer A rows=${rowsA.length}.`,
        passed: rowsA.length >= 1,
      });
    });

    const previousQuery = () =>
      buildLoadCustomersQuery({
        reminderId,
        listOfCustomer: customerAIdentifier,
        supplierType: 'PREVIOUS',
      });
    const currentQuery = () =>
      buildLoadCustomersQuery({
        reminderId,
        listOfCustomer: customerAIdentifier,
        supplierType: 'CURRENT',
      });

    await test.step('Assert: Load PODs PREVIOUS excludes POD (successor Customer B has it active today)', async () => {
      const { rows } = await fetchLoadCustomersForDps(fx, previousQuery());
      const podRows = rowsForPod(rows, { podIdentifier, podId, customerId: customerAId });
      expect(
        podRows.length,
        'PREVIOUS must exclude POD when a different UIC has an active contract POD on the current date',
      ).toBe(0);
      TestRunSummary.recordCheck({
        check: 'Load PODs PREVIOUS — successor UIC excludes former customer POD',
        expectedResult:
          'No content row with this podIdentifier (or Customer A id) for supplierType=PREVIOUS.',
        actualResult: `podRows=${podRows.length}; contentLength=${rows.length}.`,
        passed: podRows.length === 0,
      });
    });

    await test.step('Assert: Load PODs CURRENT does not list Customer A (no active POD for reminder customer)', async () => {
      const { rows } = await fetchLoadCustomersForDps(fx, currentQuery());
      const podRows = rowsForPod(rows, { podIdentifier, podId, customerId: customerAId });
      expect(
        podRows.length,
        'CURRENT must not list Customer A — reminder customer has no active contract POD today',
      ).toBe(0);
      TestRunSummary.recordCheck({
        check: 'Load PODs CURRENT — reminder customer without active POD is not listed',
        expectedResult: 'Customer A / shared POD is absent on supplierType=CURRENT.',
        actualResult: `podRows=${podRows.length}; contentLength=${rows.length}.`,
        passed: podRows.length === 0,
      });
    });

    await test.step(
      'Action: POST RFD DRAFT PREVIOUS, LIST_OF_CUSTOMERS, allSelected false, pods=[], podWithHighestConsumption true (empty Previous list — no checked PODs to send; validator allows empty pods only with highest-consumption or allSelected)',
      async () => {
      const draft = await createDraftRfd(fx, {
        reminderId,
        listOfCustomer: customerAIdentifier,
        supplierType: 'PREVIOUS',
      });
      requestDraftId = draft.requestId;
      TestRunSummary.registerPayload('requestForDisconnection', draft.payload);
      TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_A_INDEX]);
    });

    await test.step('Assert: view-pod-tab PREVIOUS still omits the POD after draft save', async () => {
      const { rows } = await fetchViewPodTab(fx, requestDraftId, previousQuery());
      const podRows = rowsForPod(rows, { podIdentifier, podId, customerId: customerAId });
      expect(
        podRows.length,
        'view-pod-tab PREVIOUS must still exclude POD after DRAFT save with empty checked pods',
      ).toBe(0);
      TestRunSummary.recordCheck({
        check: 'view-pod-tab PREVIOUS after DRAFT — POD still absent',
        expectedResult: 'Draft POD tab with supplierType=PREVIOUS does not list the shared POD.',
        actualResult: `podRows=${podRows.length}; requestDraftId=${requestDraftId}.`,
        passed: podRows.length === 0,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3211_KEY,
        relevantEntityKeys: pdt3211RelevantKeys(),
        snapshot: {
          podIdentifier,
          podId,
          customerAIdentifier,
          customerBIdentifier,
          reminderId,
          requestDraftId,
          today: TODAY,
          yesterday: YESTERDAY,
        },
      });
    });
  });

  test(`[${PDT_3211_KEY}]: Previous still includes POD when no successor customer has it active`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(PDT_3211_SINGLE_CUSTOMER_TIMEOUT_MS);
    const fx: Pdt3211Fx = { Request, GeneratePayload, Responses, Endpoints };
    const YESTERDAY = yesterdayYmd();

    let podIdentifier = '';
    let podId = 0;
    let customerAId = 0;
    let customerAIdentifier = '';
    let reminderId = 0;

    await test.step('Precondition: term + SUPPLY_ONLY product', async () => {
      await createTermAndSupplyProduct(fx);
    });

    await test.step('Precondition: Customer A (private UIC) only — no successor customer', async () => {
      const created = await createPrivateCustomer(fx);
      customerAId = entityId(created.body);
      customerAIdentifier = customerIdentifierOf(created.body);
      TestRunSummary.registerPayload('customer', created.payload);
    });

    await test.step('Precondition: settlement POD', async () => {
      const podBody = await createSettlementPod(fx);
      podId = entityId(podBody);
      podIdentifier = await resolvePodIdentifier(fx, 0);
      TestRunSummary.registerPayload('pod', { id: podId, identifier: podIdentifier });
    });

    await test.step('Contract A: create, activate yesterday, overdue liability, deactivate yesterday, terminate', async () => {
      await createProductContract(fx, CUSTOMER_A_INDEX, 0, 0);
      await activateContractPod(fx, {
        contractIndex: CONTRACT_A_INDEX,
        activationDate: YESTERDAY,
      });
      await setContractActiveInPerpetuity(fx, CONTRACT_A_INDEX);
      const { payload } = await createOverdueManualLiability(fx, CUSTOMER_A_INDEX, CONTRACT_A_INDEX);
      TestRunSummary.registerPayload('customerLiability', payload);
      await activateContractPod(fx, {
        contractIndex: CONTRACT_A_INDEX,
        activationDate: YESTERDAY,
        deactivationDate: YESTERDAY,
      });
      await terminateContract(fx, CONTRACT_A_INDEX);
      TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_A_INDEX]);
    });

    await test.step('Action: reminder LIST_OF_CUSTOMERS for Customer A + execute job', async () => {
      const reminder = await createDev2ReminderForDisconnection(fx, customerAIdentifier);
      reminderId = reminder.reminderId;
      TestRunSummary.registerPayload('reminderForDisconnection', reminder.payload);
      await executeReminderUntilExecuted(fx, reminderId);
    });

    await test.step('Setup proof: reminder second-tab lists Customer A', async () => {
      const secondTabRows = await fetchReminderSecondTab(fx, reminderId);
      const rowsA = secondTabRows.filter((r) => (r.customer ?? '').includes(customerAIdentifier));
      expect(rowsA.length, 'Reminder second-tab must list Customer A').toBeGreaterThanOrEqual(1);
    });

    await test.step('Assert: Load PODs PREVIOUS includes POD (existing Previous rule — no active POD today, no successor)', async () => {
      const { rows } = await fetchLoadCustomersForDps(
        fx,
        buildLoadCustomersQuery({
          reminderId,
          listOfCustomer: customerAIdentifier,
          supplierType: 'PREVIOUS',
        }),
      );
      const podRows = rowsForPod(rows, { podIdentifier, podId, customerId: customerAId });
      expect(
        podRows.length,
        'PREVIOUS must still list the POD when the reminder customer has no active contract POD and no other UIC holds it',
      ).toBeGreaterThanOrEqual(1);
      TestRunSummary.recordCheck({
        check: 'Load PODs PREVIOUS — existing include rule without successor',
        expectedResult: 'POD is listed for supplierType=PREVIOUS when only Customer A had it and it is inactive today.',
        actualResult: `podRows=${podRows.length}; podIdentifier=${podIdentifier}.`,
        passed: podRows.length >= 1,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3211_KEY,
        relevantEntityKeys: pdt3211RelevantKeys(),
        snapshot: {
          podIdentifier,
          podId,
          customerAIdentifier,
          reminderId,
        },
      });
    });
  });

  test(`[${PDT_3211_KEY}]: Current supplier type is unchanged for an active reminder-customer POD`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(PDT_3211_SINGLE_CUSTOMER_TIMEOUT_MS);
    const fx: Pdt3211Fx = { Request, GeneratePayload, Responses, Endpoints };
    const YESTERDAY = yesterdayYmd();

    let podIdentifier = '';
    let podId = 0;
    let customerId = 0;
    let customerIdentifier = '';
    let reminderId = 0;

    await test.step('Precondition: term + SUPPLY_ONLY product', async () => {
      await createTermAndSupplyProduct(fx);
    });

    await test.step('Precondition: private customer', async () => {
      const created = await createPrivateCustomer(fx);
      customerId = entityId(created.body);
      customerIdentifier = customerIdentifierOf(created.body);
      TestRunSummary.registerPayload('customer', created.payload);
    });

    await test.step('Precondition: settlement POD', async () => {
      const podBody = await createSettlementPod(fx);
      podId = entityId(podBody);
      podIdentifier = await resolvePodIdentifier(fx, 0);
      TestRunSummary.registerPayload('pod', { id: podId, identifier: podIdentifier });
    });

    await test.step('Contract: activate yesterday WITHOUT deactivation (active on current date)', async () => {
      await createProductContract(fx, CUSTOMER_A_INDEX, 0, 0);
      await activateContractPod(fx, {
        contractIndex: CONTRACT_A_INDEX,
        activationDate: YESTERDAY,
      });
      await setContractActiveInPerpetuity(fx, CONTRACT_A_INDEX);
      const { payload } = await createOverdueManualLiability(fx, CUSTOMER_A_INDEX, CONTRACT_A_INDEX);
      TestRunSummary.registerPayload('customerLiability', payload);
      TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_A_INDEX]);
    });

    await test.step('Action: reminder LIST_OF_CUSTOMERS for this customer + execute job', async () => {
      const reminder = await createDev2ReminderForDisconnection(fx, customerIdentifier);
      reminderId = reminder.reminderId;
      TestRunSummary.registerPayload('reminderForDisconnection', reminder.payload);
      await executeReminderUntilExecuted(fx, reminderId);
    });

    await test.step('Setup proof: reminder second-tab lists the customer', async () => {
      const secondTabRows = await fetchReminderSecondTab(fx, reminderId);
      const rowsForCustomer = secondTabRows.filter((r) => (r.customer ?? '').includes(customerIdentifier));
      expect(rowsForCustomer.length, 'Reminder second-tab must list the customer').toBeGreaterThanOrEqual(1);
    });

    await test.step('Assert: Load PODs CURRENT lists the active POD', async () => {
      const { rows } = await fetchLoadCustomersForDps(
        fx,
        buildLoadCustomersQuery({
          reminderId,
          listOfCustomer: customerIdentifier,
          supplierType: 'CURRENT',
        }),
      );
      const podRows = rowsForPod(rows, { podIdentifier, podId, customerId });
      expect(podRows.length, 'CURRENT must list the reminder customer POD that is active today').toBeGreaterThanOrEqual(
        1,
      );
      TestRunSummary.recordCheck({
        check: 'Load PODs CURRENT — active reminder-customer POD is listed',
        expectedResult: 'POD is listed for supplierType=CURRENT.',
        actualResult: `podRows=${podRows.length}; podIdentifier=${podIdentifier}.`,
        passed: podRows.length >= 1,
      });
    });

    await test.step('Assert: Load PODs PREVIOUS does not list POD (still active for reminder customer)', async () => {
      const { rows } = await fetchLoadCustomersForDps(
        fx,
        buildLoadCustomersQuery({
          reminderId,
          listOfCustomer: customerIdentifier,
          supplierType: 'PREVIOUS',
        }),
      );
      const podRows = rowsForPod(rows, { podIdentifier, podId, customerId });
      expect(
        podRows.length,
        'PREVIOUS must not list a POD that is still active for the reminder customer (existing Previous rule)',
      ).toBe(0);
      TestRunSummary.recordCheck({
        check: 'Load PODs PREVIOUS — currently-active reminder-customer POD is excluded',
        expectedResult: 'POD is absent for supplierType=PREVIOUS while the reminder customer still has it active.',
        actualResult: `podRows=${podRows.length}.`,
        passed: podRows.length === 0,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3211_KEY,
        relevantEntityKeys: pdt3211RelevantKeys(),
        snapshot: {
          podIdentifier,
          podId,
          customerIdentifier,
          reminderId,
        },
      });
    });
  });
});
