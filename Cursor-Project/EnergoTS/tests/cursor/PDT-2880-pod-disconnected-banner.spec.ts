/**
 * PDT-2880 — Customer "POD is disconnected" banner (isPodDisconnected on GET /customer/{id}).
 *
 * Core idea (Jira PDT-2880 / GE Description): for a shared disconnected POD, pick the correct
 * customer by contract POD dates and customer status — only that customer gets the flag.
 *
 * GE Description rules (Jira customfield_10103, 2026-06-12) — one TC per rule:
 * - TC-BE-1 / Rule 1: Shared POD on three customers / three contracts with distinct dates.
 *   Contract 2 (Customer B) has POD active today; Contract 1 past period; Contract 3 future POD period.
 * - TC-BE-2 / Rule 2: Shared POD on two customers in different past periods — POD not active
 *   anywhere. After DPS, flag on customer linked to the most recent past contract (latest
 *   POD deactivation). No LOST customers in this TC.
 * - TC-BE-3 / Rule 3: LOST customer must NOT get flag (prod repro — shared POD, two customers;
 *   DPS on new customer B with POD active today; assert B=true and LOST A=false).
 * - TC-BE-4 / Prod repro + Rules: TC-BE-1 layout; billing/RFD on Contract 2 (later LOST);
 *   DPS targets last valid contract customer (C). LOST B must not get flag.
 *
 * Tests assert spec/Jira expected behavior (not current bool_or runtime if it diverges).
 *
 * Reference spec(s):
 * - tests/receivableManagement/disconnectionOfPowerSupply.spec.ts
 * - tests/cursor/PDT-2971-rfd-shared-pod-multi-customer-merge.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import type { TestRunSummaryCollector } from './shared/test-run-summary.fixtures';
import { randomGens } from '../../utils/randomGens';
import { manualInvoice } from '../../jsons/payloads/create/billing/manualInvoice';

type FixtureBundle = {
  Request: any;
  GeneratePayload: any;
  Responses: any;
  Endpoints: any;
};

function isoDate(offsetDays: number): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().split('T')[0];
}

async function attachPdt2880Summary(
  TestRunSummary: TestRunSummaryCollector,
  Responses: FixtureBundle['Responses'],
  options: {
    tcKey: string;
    relevantEntityKeys: string[];
    snapshot?: Record<string, unknown>;
    extraLinks?: Record<string, string[]>;
  },
): Promise<void> {
  await test.step('Attach test run summary', async () => {
    finalizeTestRunSummary(TestRunSummary, Responses, {
      jiraKey: 'PDT-2880',
      relevantEntityKeys: options.relevantEntityKeys,
      snapshot: { tcKey: options.tcKey, ...options.snapshot },
      extraLinks: options.extraLinks,
    });
  });
}

async function buildManualInvoicePayload(
  Request: any,
  Responses: any,
  customerIndex: number,
  contractIndex: number,
) {
  const payload = manualInvoice();
  const customerGet = await Request.get(`customer/${Responses.customer[customerIndex].id}?version=1`);
  await expect(customerGet).CheckResponse();
  const customerJson = await customerGet.json();
  const contractGet = await Request.get(`product-contract/${Responses.productContract[contractIndex].id}?version=1`);
  await expect(contractGet).CheckResponse();
  const contractJson = await contractGet.json();
  const basic = payload.manualInvoiceParameters.manualInvoiceBasicDataParameters;
  basic.customerDetailId = customerJson.customerDetailsId;
  basic.invoiceCommunicationDataId = customerJson.communicationData[0].id;
  basic.billingGroupId = contractJson.contractPodsResponses[0].billingGroupId;
  basic.contractOrderId = contractJson.basicParameters.id;
  basic.prefixType = null;
  basic.contractOrderType = 'PRODUCT_CONTRACT';
  return payload;
}

async function shiftCustomerLiabilitiesToYesterday(fx: FixtureBundle, customerIndex: number): Promise<void> {
  const identifier = String(fx.Responses.customer[customerIndex].identifier);
  const liabilitiesRes = await fx.Request.get(
    `customer-liability/list?page=0&size=10&columns=ID&direction=DESC&prompt=${identifier}&searchFields=CUSTOMER`,
  );
  await expect(liabilitiesRes).CheckResponse();
  const body = await liabilitiesRes.json();
  const rows = (body.content ?? []) as Array<{ id: number; currentAmount?: number }>;
  const open = rows.filter((r) => (r.currentAmount ?? 0) > 0);
  if (open.length === 0) {
    return;
  }
  const dueDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');
  for (const row of open) {
    const change = await fx.Request.put(`customer-liability/${row.id}/due-date-change?dueDate=${dueDate}`);
    await expect(change).CheckResponse();
  }
}

async function createTerminationCatalogue(fx: FixtureBundle): Promise<void> {
  const terminationPayload = fx.GeneratePayload.productAndServices.termination('DEACTIVATION_OF_POINTS_OF_DELIVERY');
  terminationPayload.autoTermination = true;
  terminationPayload.autoTerminationFrom = 'EVENT_DATE';
  const terminationRes = await fx.Request.post(fx.Endpoints.termination, { data: terminationPayload });
  await expect(terminationRes).CheckResponse();
  const terminationData = await terminationRes.json();

  const groupPayload = {
    name: randomGens.generateRandomString(true, false, 10),
    terminationsList: [
      {
        terminationId: terminationData.id,
        terminationName: terminationData.name ?? 'auto',
        terminationFullName: terminationData.name ?? 'auto',
      },
    ],
  };
  const groupRes = await fx.Request.post(fx.Endpoints.groupOfTerminations, { data: groupPayload });
  await expect(groupRes).CheckResponse();
  const groupBody = await groupRes.json();
  const terminationGroupId = typeof groupBody === 'number' ? groupBody : groupBody.id;

  const termRes = await fx.Request.post(fx.Endpoints.terms, {
    data: fx.GeneratePayload.productAndServices.term(),
  });
  await expect(termRes).CheckResponse();
  fx.Responses.terms.push(await termRes.json());

  const productPayload = fx.GeneratePayload.productAndServices.product();
  productPayload.contractTypes = ['SUPPLY_ONLY'];
  productPayload.paymentGuarantees = ['NO'];
  productPayload.terminationIds = [];
  productPayload.terminationGroupIds = [terminationGroupId];
  const productRes = await fx.Request.post(fx.Endpoints.product, { data: productPayload });
  await expect(productRes).CheckResponse();
  fx.Responses.product.push(await productRes.json());
}

async function createPrivateCustomer(fx: FixtureBundle): Promise<number> {
  const res = await fx.Request.post(fx.Endpoints.customer, {
    data: fx.GeneratePayload.customers.customer_private(),
  });
  await expect(res).CheckResponse();
  const body = await res.json();
  fx.Responses.customer.push(body);
  return fx.Responses.customer.length - 1;
}

async function createSharedPod(fx: FixtureBundle): Promise<void> {
  const res = await fx.Request.post(fx.Endpoints.pod, {
    data: fx.GeneratePayload.pointsOfDelivery.pod_settlement(),
  });
  await expect(res).CheckResponse();
  fx.Responses.pod.push(await res.json());
}

async function createSupplyOnlyContract(
  fx: FixtureBundle,
  customerIndex: number,
  options?: { entryInForceDate?: string; status?: string; subStatus?: string },
): Promise<number> {
  const payload = await fx.GeneratePayload.contractsAndOrders.product_contract(customerIndex, 0, 0);
  payload.basicParameters.customerId = fx.Responses.customer[customerIndex].id;
  payload.productParameters.contractType = 'SUPPLY_ONLY';
  if (options?.entryInForceDate) {
    payload.basicParameters.entryInForceDate = options.entryInForceDate;
  }
  if (options?.status) {
    payload.basicParameters.status = options.status;
  }
  if (options?.subStatus) {
    payload.basicParameters.subStatus = options.subStatus;
  }
  const res = await fx.Request.post(fx.Endpoints.productContract, { data: payload });
  await expect(res).CheckResponse();
  fx.Responses.productContract.push(await res.json());
  return fx.Responses.productContract.length - 1;
}

async function activatePodWithDeliveryStatus(
  fx: FixtureBundle,
  contractIndex: number,
  activationDate: string,
  podIndex = 0,
): Promise<void> {
  const activation = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(
      podIndex,
      activationDate,
      undefined,
      contractIndex,
    ),
  });
  await expect(activation).CheckResponse();

  const contractId = fx.Responses.productContract[contractIndex].id;
  const statusUpdate = await fx.Request.put(`product-contract/status-update/${contractId}?versionId=1`, {
    data: {
      contractStatus: 'ACTIVE_IN_PERPETUITY',
      contractSubStatus: 'DELIVERY',
      contractVersionStatus: 'SIGNED',
    },
  });
  await expect(statusUpdate).CheckResponse();
}

async function deactivatePodOnContract(
  fx: FixtureBundle,
  contractIndex: number,
  activationDate: string,
  deactivationDate: string,
  podIndex = 0,
): Promise<void> {
  const deactivation = await fx.Request.post('/contract-pods/manual', {
    data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(
      podIndex,
      activationDate,
      deactivationDate,
      contractIndex,
    ),
  });
  await expect(deactivation).CheckResponse();
}

async function terminateContract(fx: FixtureBundle, contractIndex: number): Promise<void> {
  const contractId = fx.Responses.productContract[contractIndex].id;
  const termRes = await fx.Request.get(`ttest/pod-termination?contractId=${contractId}`);
  console.log(`/ttest/pod-termination response status: ${termRes.status()}`);
  if (termRes.status() >= 400) {
    console.log(`/ttest/pod-termination failed (${termRes.status()}), falling back to status-update`);
    const fallback = await fx.Request.put(`product-contract/status-update/${contractId}?versionId=1`, {
      data: {
        contractStatus: 'TERMINATED',
        contractSubStatus: 'ALL_PODS_ARE_DEACTIVATED',
        contractVersionStatus: 'SIGNED',
      },
    });
    await expect(fallback).CheckResponse();
  } else {
    await expect(termRes).CheckResponse();
  }
}

async function runManualBillingForContract(
  fx: FixtureBundle,
  customerIndex: number,
  contractIndex: number,
  billingRunIndex: number,
): Promise<void> {
  const invoicePayload = await buildManualInvoicePayload(
    fx.Request,
    fx.Responses,
    customerIndex,
    contractIndex,
  );
  const billingRes = await fx.Request.post(fx.Endpoints.billingRun, { data: invoicePayload });
  await expect(billingRes).CheckResponse();
  fx.Responses.billingRun.push(await billingRes.json());
  await fx.GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, billingRunIndex);
  await shiftCustomerLiabilitiesToYesterday(fx, customerIndex);
}

async function executeBillingReminderRfdDpsChain(
  fx: FixtureBundle,
  customerIndex: number,
  contractIndex: number,
  billingRunIndex: number,
): Promise<void> {
  await executeBillingReminderRfdChain(fx, customerIndex, contractIndex, billingRunIndex);
  await executeDpsForCustomerOnLatestRfd(fx, customerIndex);
}

async function executeBillingReminderRfdChain(
  fx: FixtureBundle,
  customerIndex: number,
  contractIndex: number,
  billingRunIndex: number,
): Promise<void> {
  await runManualBillingForContract(fx, customerIndex, contractIndex, billingRunIndex);

  const reminderPayload = fx.GeneratePayload.receivablesManagement.reminderForDisconnection();
  reminderPayload.customerList = fx.Responses.customer[customerIndex].identifier as string;
  const reminderRes = await fx.Request.post(fx.Endpoints.reminderForDisconnection, { data: reminderPayload });
  await expect(reminderRes).CheckResponse();
  fx.Responses.reminderForDisconnection.push(await reminderRes.json());
  await fx.GeneratePayload.receivablesManagement.offsetReminderForDisconnectionTime();

  const rfdPayload = fx.GeneratePayload.receivablesManagement.requestForDisconnection() as Record<string, unknown>;
  rfdPayload.conditionType = 'LIST_OF_CUSTOMERS';
  rfdPayload.listOfCustomer = fx.Responses.customer[customerIndex].identifier as string;
  rfdPayload.allSelected = true;
  rfdPayload.pods = [];
  const rfdRes = await fx.Request.post(fx.Endpoints.requestForDisconnection, { data: rfdPayload });
  await expect(rfdRes).CheckResponse();
  fx.Responses.requestForDisconnection.push(await rfdRes.json());
}

async function executeDpsForCustomerOnLatestRfd(fx: FixtureBundle, customerIndex: number): Promise<void> {
  const rfdId = fx.Responses.requestForDisconnection[fx.Responses.requestForDisconnection.length - 1];
  const calc = await fx.Request.post(`disconnection-of-power-supply-requests/calculate-tax/${rfdId}`);
  await expect(calc).CheckResponse();

  const dpsPayload = fx.GeneratePayload.receivablesManagement.disconnectionOfPowerSupply() as unknown as Record<
    string,
    unknown
  >;
  const disconnected = dpsPayload.disconnectedRequest as Array<Record<string, unknown>>;
  disconnected[0].customerId = fx.Responses.customer[customerIndex].id;
  disconnected[0].podId = fx.Responses.pod[0].id;
  const dpsRes = await fx.Request.post(fx.Endpoints.disconnectionOfPowerSupply, { data: dpsPayload });
  await expect(dpsRes).CheckResponse();
  fx.Responses.disconnectionOfPowerSupply.push(await dpsRes.json());
}

async function fetchCustomer(fx: FixtureBundle, customerIndex: number) {
  const getResp = await fx.Request.get(`${fx.Endpoints.customer}/${fx.Responses.customer[customerIndex].id}`);
  await expect(getResp).CheckResponse();
  return getResp.json();
}

test.describe(
  '[PDT-2880]: Customer - "POD is disconnected" incorrect label',
  { tag: '@receivableManagement' },
  () => {
    test('[PDT-2880] TC-BE-1: Rule 1 — three customers on shared POD; only today-active contract customer gets flag', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(15 * 60 * 1000);

      const fx: FixtureBundle = { Request, GeneratePayload, Responses, Endpoints };
      const CUSTOMER_A = 0;
      const CUSTOMER_B = 1;
      const CUSTOMER_C = 2;
      const CONTRACT_A = 0;
      const CONTRACT_B = 1;
      const CONTRACT_C = 2;
      const FORTY_DAYS_AGO = isoDate(-40);
      const THIRTY_DAYS_AGO = isoDate(-30);
      const TOMORROW = isoDate(1);
      const TWO_DAYS_AHEAD = isoDate(2);
      const ELEVEN_DAYS_AHEAD = isoDate(11);
      const TODAY = isoDate(0);

      await test.step('Precondition: catalogue, three customers, shared POD', async () => {
        await createTerminationCatalogue(fx);
        await createPrivateCustomer(fx);
        await createPrivateCustomer(fx);
        await createPrivateCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_A]);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_B]);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_C]);
        await createSharedPod(fx);
        TestRunSummary.registerPayload('pod', Responses.pod[0]);
      });

      await test.step('Contract 1 (Customer A): oldest past period — POD deactivated ~30 days ago', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_A, {
          entryInForceDate: FORTY_DAYS_AGO,
          status: 'ENTERED_INTO_FORCE',
          subStatus: 'AWAITING_ACTIVATION',
        });
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_A]);
        await activatePodWithDeliveryStatus(fx, CONTRACT_A, FORTY_DAYS_AGO);
        await deactivatePodOnContract(fx, CONTRACT_A, FORTY_DAYS_AGO, THIRTY_DAYS_AGO);
      });

      await test.step('Contract 2 (Customer B): valid for today — contract created (POD activated after C future schedule)', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_B, {
          entryInForceDate: TODAY,
          status: 'ENTERED_INTO_FORCE',
          subStatus: 'AWAITING_ACTIVATION',
        });
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_B]);
      });

      await test.step('Contract 3 (Customer C): future POD period — activation in 2 days, deactivation in 11 days', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_C, {
          entryInForceDate: TODAY,
          status: 'ENTERED_INTO_FORCE',
          subStatus: 'AWAITING_ACTIVATION',
        });
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_C]);
        const activation = await fx.Request.post('/contract-pods/manual', {
          data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(
            0,
            TWO_DAYS_AHEAD,
            ELEVEN_DAYS_AHEAD,
            CONTRACT_C,
          ),
        });
        await expect(activation).CheckResponse();
      });

      await test.step('Contract 2 (Customer B): POD active today until tomorrow (only currently active period)', async () => {
        const activation = await fx.Request.post('/contract-pods/manual', {
          data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(
            0,
            TODAY,
            TOMORROW,
            CONTRACT_B,
          ),
        });
        await expect(activation).CheckResponse();
        const contractBId = fx.Responses.productContract[CONTRACT_B].id;
        const statusUpdate = await fx.Request.put(`product-contract/status-update/${contractBId}?versionId=1`, {
          data: {
            contractStatus: 'ACTIVE_IN_PERPETUITY',
            contractSubStatus: 'DELIVERY',
            contractVersionStatus: 'SIGNED',
          },
        });
        await expect(statusUpdate).CheckResponse();
      });

      await test.step('Billing → reminder → RFD → DPS (Customer B — contract with POD active today)', async () => {
        await executeBillingReminderRfdDpsChain(fx, CUSTOMER_B, CONTRACT_B, 0);
      });

      await test.step('Verify Rule 1: Customer B (POD active today) must have isPodDisconnected=true', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_B);
        const passed = body.isPodDisconnected === true;
        console.log(
          `[PDT-2880] Customer B — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`,
        );
        TestRunSummary.recordCheck({
          check: 'Rule 1 — customer linked to currently active POD contract receives flag',
          expectedResult: 'Customer B isPodDisconnected=true',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed,
        });
        expect(
          body.isPodDisconnected,
          'Rule 1: customer linked to contract where POD is currently active must show disconnect flag after DPS',
        ).toBe(true);
      });

      await test.step('Verify Rule 1: Customer A (older past contract) must NOT receive flag', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_A);
        const passed = body.isPodDisconnected === false;
        console.log(
          `[PDT-2880] Customer A — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`,
        );
        TestRunSummary.recordCheck({
          check: 'Rule 1 — older past contract customer must not receive flag',
          expectedResult: 'Customer A isPodDisconnected=false',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed,
        });
        expect(
          body.isPodDisconnected,
          'Rule 1: customer linked only to older past contract must not receive POD disconnect flag',
        ).toBe(false);
      });

      await test.step('Verify Rule 1: Customer C (future POD period only) must NOT receive flag', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_C);
        const passed = body.isPodDisconnected === false;
        console.log(
          `[PDT-2880] Customer C — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`,
        );
        TestRunSummary.recordCheck({
          check: 'Rule 1 — customer with only future POD activation/deactivation must not receive flag',
          expectedResult: 'Customer C isPodDisconnected=false',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed,
        });
        expect(
          body.isPodDisconnected,
          'Rule 1: customer with only future POD schedule must not receive disconnect flag while POD active on another contract today',
        ).toBe(false);
      });

      const contractAId = Responses.productContract[CONTRACT_A].id;
      const contractBId = Responses.productContract[CONTRACT_B].id;
      const contractCId = Responses.productContract[CONTRACT_C].id;

      await attachPdt2880Summary(TestRunSummary, Responses, {
        tcKey: 'TC-BE-1',
        relevantEntityKeys: [
          'customer',
          'pod',
          'productContract',
          'billingRun',
          'requestForDisconnection',
          'disconnectionOfPowerSupply',
        ],
        snapshot: {
          contractAId,
          contractBId,
          contractCId,
          customerAId: Responses.customer[CUSTOMER_A].id,
          customerBId: Responses.customer[CUSTOMER_B].id,
          customerCId: Responses.customer[CUSTOMER_C].id,
          contractAPodDeactivatedOn: THIRTY_DAYS_AGO,
          contractBPodActiveFrom: TODAY,
          contractBPodDeactivatesOn: TOMORROW,
          contractCPodActivationFrom: TWO_DAYS_AHEAD,
          contractCPodDeactivationOn: ELEVEN_DAYS_AHEAD,
        },
        extraLinks: {
          productContract: [
            ...(buildProductContractTabLinks(contractAId).productContract ?? []),
            ...(buildProductContractTabLinks(contractBId).productContract ?? []),
            ...(buildProductContractTabLinks(contractCId).productContract ?? []),
          ],
        },
      });
    });

    test('[PDT-2880] TC-BE-2: Rule 2 — shared POD on two customers in past periods; flag on most recent past contract customer', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(15 * 60 * 1000);

      const fx: FixtureBundle = { Request, GeneratePayload, Responses, Endpoints };
      const CUSTOMER_A = 0;
      const CUSTOMER_B = 1;
      const CONTRACT_A = 0;
      const CONTRACT_B = 1;
      const FORTY_DAYS_AGO = isoDate(-40);
      const THIRTY_DAYS_AGO = isoDate(-30);
      const FIFTEEN_DAYS_AGO = isoDate(-15);
      const TEN_DAYS_AGO = isoDate(-10);
      const YESTERDAY = isoDate(-1);

      await test.step('Precondition: catalogue, two ACTIVE customers, shared POD', async () => {
        await createTerminationCatalogue(fx);
        await createPrivateCustomer(fx);
        await createPrivateCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_A]);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_B]);
        await createSharedPod(fx);
        TestRunSummary.registerPayload('pod', Responses.pod[0]);
      });

      await test.step('Contract A (Customer A): older past period — POD deactivated ~30 days ago', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_A, {
          entryInForceDate: FORTY_DAYS_AGO,
          status: 'ENTERED_INTO_FORCE',
          subStatus: 'AWAITING_ACTIVATION',
        });
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_A]);
        await activatePodWithDeliveryStatus(fx, CONTRACT_A, FORTY_DAYS_AGO);
        await deactivatePodOnContract(fx, CONTRACT_A, FORTY_DAYS_AGO, THIRTY_DAYS_AGO);
      });

      await test.step('Contract B (Customer B): more recent past period — billing, POD deactivated yesterday (latest deactivation)', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_B, {
          entryInForceDate: FIFTEEN_DAYS_AGO,
          status: 'ENTERED_INTO_FORCE',
          subStatus: 'AWAITING_ACTIVATION',
        });
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_B]);
        await activatePodWithDeliveryStatus(fx, CONTRACT_B, TEN_DAYS_AGO);
        await runManualBillingForContract(fx, CUSTOMER_B, CONTRACT_B, 0);
        if (Responses.billingRun[0]) {
          TestRunSummary.registerPayload('billingRun', Responses.billingRun[0]);
        }
        await deactivatePodOnContract(fx, CONTRACT_B, TEN_DAYS_AGO, YESTERDAY);
      });

      await test.step('Precondition check: both customers ACTIVE (no LOST) — POD not active on any contract', async () => {
        const customerA = await fetchCustomer(fx, CUSTOMER_A);
        const customerB = await fetchCustomer(fx, CUSTOMER_B);
        console.log(
          `[PDT-2880] TC-BE-2 pre-DPS — Customer A: status=${customerA.status}, Customer B: status=${customerB.status}`,
        );
        TestRunSummary.recordCheck({
          check: 'Rule 2 precondition — neither customer is LOST',
          expectedResult: 'Customer A and B status=ACTIVE',
          actualResult: `A=${customerA.status}, B=${customerB.status}`,
          passed: customerA.status === 'ACTIVE' && customerB.status === 'ACTIVE',
        });
        expect(customerA.status, 'Rule 2 TC must not use LOST customers').toBe('ACTIVE');
        expect(customerB.status, 'Rule 2 TC must not use LOST customers').toBe('ACTIVE');
      });

      await test.step('Reminder → RFD → DPS for Customer B (liability on most recent past contract)', async () => {
        const reminderPayload = fx.GeneratePayload.receivablesManagement.reminderForDisconnection();
        reminderPayload.customerList = fx.Responses.customer[CUSTOMER_B].identifier as string;
        const reminderRes = await fx.Request.post(fx.Endpoints.reminderForDisconnection, { data: reminderPayload });
        await expect(reminderRes).CheckResponse();
        fx.Responses.reminderForDisconnection.push(await reminderRes.json());
        await fx.GeneratePayload.receivablesManagement.offsetReminderForDisconnectionTime();

        const rfdPayload = fx.GeneratePayload.receivablesManagement.requestForDisconnection() as Record<
          string,
          unknown
        >;
        rfdPayload.conditionType = 'LIST_OF_CUSTOMERS';
        rfdPayload.listOfCustomer = fx.Responses.customer[CUSTOMER_B].identifier as string;
        rfdPayload.allSelected = true;
        rfdPayload.pods = [];
        const rfdRes = await fx.Request.post(fx.Endpoints.requestForDisconnection, { data: rfdPayload });
        await expect(rfdRes).CheckResponse();
        fx.Responses.requestForDisconnection.push(await rfdRes.json());

        const rfdId = fx.Responses.requestForDisconnection[fx.Responses.requestForDisconnection.length - 1];
        const calc = await fx.Request.post(`disconnection-of-power-supply-requests/calculate-tax/${rfdId}`);
        await expect(calc).CheckResponse();

        const dpsPayload = fx.GeneratePayload.receivablesManagement.disconnectionOfPowerSupply() as unknown as Record<
          string,
          unknown
        >;
        const disconnected = dpsPayload.disconnectedRequest as Array<Record<string, unknown>>;
        disconnected[0].customerId = fx.Responses.customer[CUSTOMER_B].id;
        disconnected[0].podId = fx.Responses.pod[0].id;
        const dpsRes = await fx.Request.post(fx.Endpoints.disconnectionOfPowerSupply, { data: dpsPayload });
        await expect(dpsRes).CheckResponse();
        fx.Responses.disconnectionOfPowerSupply.push(await dpsRes.json());
      });

      await test.step('Verify Rule 2: Customer B (most recent past contract / latest POD deactivation) receives flag', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_B);
        const passed = body.isPodDisconnected === true;
        console.log(
          `[PDT-2880] Customer B — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`,
        );
        TestRunSummary.recordCheck({
          check: 'Rule 2 — customer with most recent past contract receives POD disconnect flag',
          expectedResult: 'Customer B isPodDisconnected=true',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed,
        });
        expect(
          body.isPodDisconnected,
          'Rule 2: when POD is not active anywhere, customer linked to most recent past contract must show disconnect flag after DPS',
        ).toBe(true);
      });

      await test.step('Verify Rule 2: Customer A (older past contract) must NOT receive flag', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_A);
        const passed = body.isPodDisconnected === false;
        console.log(
          `[PDT-2880] Customer A — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`,
        );
        TestRunSummary.recordCheck({
          check: 'Rule 2 — customer with older past contract must not receive flag',
          expectedResult: 'Customer A isPodDisconnected=false',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed,
        });
        expect(
          body.isPodDisconnected,
          'Rule 2: customer linked to older past contract must not receive POD disconnect flag',
        ).toBe(false);
      });

      const contractAId = Responses.productContract[CONTRACT_A].id;
      const contractBId = Responses.productContract[CONTRACT_B].id;

      await attachPdt2880Summary(TestRunSummary, Responses, {
        tcKey: 'TC-BE-2',
        relevantEntityKeys: [
          'customer',
          'pod',
          'productContract',
          'billingRun',
          'requestForDisconnection',
          'disconnectionOfPowerSupply',
        ],
        snapshot: {
          contractAId,
          contractBId,
          customerAId: Responses.customer[CUSTOMER_A].id,
          customerBId: Responses.customer[CUSTOMER_B].id,
          contractAPodDeactivatedOn: THIRTY_DAYS_AGO,
          contractBPodDeactivatedOn: YESTERDAY,
        },
        extraLinks: {
          productContract: [
            ...(buildProductContractTabLinks(contractAId).productContract ?? []),
            ...(buildProductContractTabLinks(contractBId).productContract ?? []),
          ],
        },
      });
    });

    test('[PDT-2880] TC-BE-3: Rule 3 — LOST former customer must not get POD disconnected flag after DPS for new customer (prod repro)', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(15 * 60 * 1000);

      const fx: FixtureBundle = { Request, GeneratePayload, Responses, Endpoints };
      const CUSTOMER_A = 0;
      const CUSTOMER_B = 1;
      const CONTRACT_A = 0;
      const CONTRACT_B = 1;
      const YESTERDAY = isoDate(-1);
      const TODAY = isoDate(0);

      await test.step('Precondition: termination catalogue + product', async () => {
        await createTerminationCatalogue(fx);
      });

      await test.step('Precondition: Customer A (former / will become LOST)', async () => {
        await createPrivateCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_A]);
      });

      await test.step('Precondition: Customer B (current / active)', async () => {
        await createPrivateCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_B]);
      });

      await test.step('Precondition: shared POD (same POD on two contracts — prod repro only)', async () => {
        await createSharedPod(fx);
        TestRunSummary.registerPayload('pod', Responses.pod[0]);
      });

      await test.step('Contract A: create, activate, deactivate, terminate → Customer A LOST', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_A, {
          entryInForceDate: TODAY,
          status: 'ENTERED_INTO_FORCE',
          subStatus: 'AWAITING_ACTIVATION',
        });
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_A]);
        await activatePodWithDeliveryStatus(fx, CONTRACT_A, YESTERDAY);
        await deactivatePodOnContract(fx, CONTRACT_A, YESTERDAY, YESTERDAY);
        await terminateContract(fx, CONTRACT_A);
      });

      await test.step('Verify: Customer A status after termination (expect LOST — Rule 3 precondition)', async () => {
        const maxAttempts = 12;
        let body: { status?: string } = {};
        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
          body = await fetchCustomer(fx, CUSTOMER_A);
          if (body.status === 'LOST') {
            break;
          }
          if (attempt < maxAttempts) {
            await new Promise((r) => setTimeout(r, 5000));
          }
        }
        console.log(`Customer A status: ${body.status} (expected LOST)`);
        TestRunSummary.recordCheck({
          check: 'Rule 3 precondition — Customer A becomes LOST',
          expectedResult: 'status=LOST',
          actualResult: `status=${body.status}`,
          passed: body.status === 'LOST',
        });
        expect(body.status, 'Rule 3 precondition: Customer A must be LOST after historical contract termination').toBe(
          'LOST',
        );
      });

      await test.step('Contract B: create, POD active today + DELIVERY (new customer — prod repro path)', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_B);
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_B]);
        await activatePodWithDeliveryStatus(fx, CONTRACT_B, TODAY);
      });

      await test.step('Billing → reminder → RFD → DPS (Customer B — prod repro path)', async () => {
        await executeBillingReminderRfdDpsChain(fx, CUSTOMER_B, CONTRACT_B, 0);
        if (Responses.billingRun[0]) {
          TestRunSummary.registerPayload('billingRun', Responses.billingRun[0]);
        }
      });

      await test.step('Verify prod repro: Customer B (DPS target) must have isPodDisconnected=true', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_B);
        const passed = body.isPodDisconnected === true;
        console.log(`[PDT-2880] Customer B — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`);
        TestRunSummary.recordCheck({
          check: 'Prod repro — DPS target customer receives POD disconnect flag',
          expectedResult: 'Customer B isPodDisconnected=true',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed,
        });
        expect(
          body.isPodDisconnected,
          'Prod repro: customer targeted by RFD/DPS must show disconnect flag after DPS',
        ).toBe(true);
      });

      await test.step('Verify Rule 3: LOST Customer A must NOT have isPodDisconnected=true', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_A);
        const passed = body.isPodDisconnected === false;
        console.log(`[PDT-2880] Customer A — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`);
        TestRunSummary.recordCheck({
          check: 'Rule 3 — LOST former customer must not inherit POD disconnect flag',
          expectedResult: 'Customer A isPodDisconnected=false',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed,
        });
        expect(
          body.isPodDisconnected,
          'Rule 3 / prod repro: LOST customer linked only via historical contract must not inherit POD disconnect flag',
        ).toBe(false);
      });

      const contractAId = Responses.productContract[CONTRACT_A].id;
      const contractBId = Responses.productContract[CONTRACT_B].id;

      await attachPdt2880Summary(TestRunSummary, Responses, {
        tcKey: 'TC-BE-3',
        relevantEntityKeys: [
          'customer',
          'pod',
          'productContract',
          'billingRun',
          'requestForDisconnection',
          'disconnectionOfPowerSupply',
        ],
        snapshot: {
          contractAId,
          contractBId,
          customerAId: Responses.customer[CUSTOMER_A].id,
          customerBId: Responses.customer[CUSTOMER_B].id,
          contractAPodDeactivatedOn: YESTERDAY,
          contractBPodActivationDate: TODAY,
        },
        extraLinks: {
          productContract: [
            ...(buildProductContractTabLinks(contractAId).productContract ?? []),
            ...(buildProductContractTabLinks(contractBId).productContract ?? []),
          ],
        },
      });
    });

    test('[PDT-2880] TC-BE-4: Prod repro — liability on LOST-path customer; DPS on last valid contract customer', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(15 * 60 * 1000);

      const fx: FixtureBundle = { Request, GeneratePayload, Responses, Endpoints };
      const CUSTOMER_A = 0;
      const CUSTOMER_B = 1;
      const CUSTOMER_C = 2;
      const CONTRACT_A = 0;
      const CONTRACT_B = 1;
      const CONTRACT_C = 2;
      const FORTY_DAYS_AGO = isoDate(-40);
      const THIRTY_DAYS_AGO = isoDate(-30);
      const YESTERDAY = isoDate(-1);
      const TOMORROW = isoDate(1);
      const TWO_DAYS_AHEAD = isoDate(2);
      const ELEVEN_DAYS_AHEAD = isoDate(11);
      const TODAY = isoDate(0);

      await test.step('Precondition: catalogue, three customers, shared POD (same dates as TC-BE-1)', async () => {
        await createTerminationCatalogue(fx);
        await createPrivateCustomer(fx);
        await createPrivateCustomer(fx);
        await createPrivateCustomer(fx);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_A]);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_B]);
        TestRunSummary.registerPayload('customer', Responses.customer[CUSTOMER_C]);
        await createSharedPod(fx);
        TestRunSummary.registerPayload('pod', Responses.pod[0]);
      });

      await test.step('Contract 1 (Customer A): oldest past period — POD deactivated ~30 days ago', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_A, {
          entryInForceDate: FORTY_DAYS_AGO,
          status: 'ENTERED_INTO_FORCE',
          subStatus: 'AWAITING_ACTIVATION',
        });
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_A]);
        await activatePodWithDeliveryStatus(fx, CONTRACT_A, FORTY_DAYS_AGO);
        await deactivatePodOnContract(fx, CONTRACT_A, FORTY_DAYS_AGO, THIRTY_DAYS_AGO);
      });

      await test.step('Contract 2 (Customer B): valid for today — contract created', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_B, {
          entryInForceDate: TODAY,
          status: 'ENTERED_INTO_FORCE',
          subStatus: 'AWAITING_ACTIVATION',
        });
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_B]);
      });

      await test.step('Contract 2 (Customer B): POD activated yesterday + DELIVERY (LOST path — deactivation must be < today)', async () => {
        await activatePodWithDeliveryStatus(fx, CONTRACT_B, YESTERDAY);
      });

      await test.step('Billing → reminder → RFD on Customer B (liability on Contract 2 before LOST — prod repro)', async () => {
        await executeBillingReminderRfdChain(fx, CUSTOMER_B, CONTRACT_B, 0);
        if (Responses.billingRun[0]) {
          TestRunSummary.registerPayload('billingRun', Responses.billingRun[0]);
        }
      });

      await test.step('Contract 2: deactivate POD yesterday and terminate → Customer B LOST', async () => {
        await deactivatePodOnContract(fx, CONTRACT_B, YESTERDAY, YESTERDAY);
        await terminateContract(fx, CONTRACT_B);
      });

      await test.step('Verify: Customer B status after termination (expect LOST)', async () => {
        const maxAttempts = 12;
        let body: { status?: string } = {};
        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
          body = await fetchCustomer(fx, CUSTOMER_B);
          if (body.status === 'LOST') {
            break;
          }
          if (attempt < maxAttempts) {
            await new Promise((r) => setTimeout(r, 5000));
          }
        }
        console.log(`[PDT-2880] TC-BE-4 Customer B status: ${body.status} (expected LOST)`);
        TestRunSummary.recordCheck({
          check: 'TC-BE-4 precondition — Contract 2 customer becomes LOST',
          expectedResult: 'status=LOST',
          actualResult: `status=${body.status}`,
          passed: body.status === 'LOST',
        });
        expect(body.status, 'TC-BE-4: Customer B (Contract 2) must be LOST after POD deactivation + termination').toBe(
          'LOST',
        );
      });

      await test.step('Contract 3 (Customer C): future POD period — activation in 2 days, deactivation in 11 days', async () => {
        await createSupplyOnlyContract(fx, CUSTOMER_C, {
          entryInForceDate: TODAY,
          status: 'ENTERED_INTO_FORCE',
          subStatus: 'AWAITING_ACTIVATION',
        });
        TestRunSummary.registerPayload('productContract', Responses.productContract[CONTRACT_C]);
        const activation = await fx.Request.post('/contract-pods/manual', {
          data: await fx.GeneratePayload.pointsOfDelivery.pod_activation(
            0,
            TWO_DAYS_AHEAD,
            ELEVEN_DAYS_AHEAD,
            CONTRACT_C,
          ),
        });
        await expect(activation).CheckResponse();
      });

      await test.step('DPS for shared POD — target Customer C (RFD from Contract 2 / Customer B; DPS on last valid customer)', async () => {
        TestRunSummary.recordCheck({
          check: 'Prod repro cross-customer DPS — RFD liability on Contract 2, disconnect executed for Contract 3 customer',
          expectedResult: 'RFD from Customer B; DPS payload targets Customer C',
          actualResult: `rfdCustomerId=${Responses.customer[CUSTOMER_B].id}, dpsTargetCustomerId=${Responses.customer[CUSTOMER_C].id}`,
          passed: true,
        });
        await executeDpsForCustomerOnLatestRfd(fx, CUSTOMER_C);
      });

      await test.step('Verify: Customer C (last contract) must have isPodDisconnected=true', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_C);
        const passed = body.isPodDisconnected === true;
        console.log(
          `[PDT-2880] Customer C — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`,
        );
        TestRunSummary.recordCheck({
          check: 'Prod repro — last valid contract customer receives POD disconnect flag after DPS',
          expectedResult: 'Customer C isPodDisconnected=true',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed,
        });
        expect(
          body.isPodDisconnected,
          'PDT-2880: when RFD/billing path started on Contract 2 customer who becomes LOST, flag must go to last valid contract customer (C)',
        ).toBe(true);
      });

      await test.step('Verify Rule 3: LOST Customer B (Contract 2) must NOT receive flag', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_B);
        const passed = body.isPodDisconnected === false;
        console.log(
          `[PDT-2880] Customer B — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`,
        );
        TestRunSummary.recordCheck({
          check: 'Rule 3 — LOST Contract 2 customer must not receive POD disconnect flag',
          expectedResult: 'Customer B status=LOST, isPodDisconnected=false',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed: passed && body.status === 'LOST',
        });
        expect(body.status).toBe('LOST');
        expect(
          body.isPodDisconnected,
          'Rule 3: LOST customer on Contract 2 must not inherit POD disconnect flag',
        ).toBe(false);
      });

      await test.step('Verify: Customer A (older past contract) must NOT receive flag', async () => {
        const body = await fetchCustomer(fx, CUSTOMER_A);
        const passed = body.isPodDisconnected === false;
        console.log(
          `[PDT-2880] Customer A — status: ${body.status}, isPodDisconnected: ${body.isPodDisconnected}`,
        );
        TestRunSummary.recordCheck({
          check: 'Rule 1 — older past contract customer must not receive flag',
          expectedResult: 'Customer A isPodDisconnected=false',
          actualResult: `status=${body.status}, isPodDisconnected=${body.isPodDisconnected}`,
          passed,
        });
        expect(
          body.isPodDisconnected,
          'Customer A linked only to oldest past contract must not receive POD disconnect flag',
        ).toBe(false);
      });

      const contractAId = Responses.productContract[CONTRACT_A].id;
      const contractBId = Responses.productContract[CONTRACT_B].id;
      const contractCId = Responses.productContract[CONTRACT_C].id;

      await attachPdt2880Summary(TestRunSummary, Responses, {
        tcKey: 'TC-BE-4',
        relevantEntityKeys: [
          'customer',
          'pod',
          'productContract',
          'billingRun',
          'requestForDisconnection',
          'disconnectionOfPowerSupply',
        ],
        snapshot: {
          contractAId,
          contractBId,
          contractCId,
          customerAId: Responses.customer[CUSTOMER_A].id,
          customerBId: Responses.customer[CUSTOMER_B].id,
          customerCId: Responses.customer[CUSTOMER_C].id,
          contractAPodDeactivatedOn: THIRTY_DAYS_AGO,
          contractBPodActiveFrom: YESTERDAY,
          contractBPodDeactivatesOn: YESTERDAY,
          contractCPodActivationFrom: TWO_DAYS_AHEAD,
          contractCPodDeactivationOn: ELEVEN_DAYS_AHEAD,
        },
        extraLinks: {
          productContract: [
            ...(buildProductContractTabLinks(contractAId).productContract ?? []),
            ...(buildProductContractTabLinks(contractBId).productContract ?? []),
            ...(buildProductContractTabLinks(contractCId).productContract ?? []),
          ],
        },
      });
    });
  },
);
