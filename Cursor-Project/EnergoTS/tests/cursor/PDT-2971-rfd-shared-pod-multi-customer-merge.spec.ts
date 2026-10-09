/**
 * PDT-2971 — AS-IS defect reproduction (shared POD, two customers, RFD POD tab merge).
 *
 * Prod case: POD 32Z4101080010651 — customer 7501091095 (terminated contract + liabilities)
 * and customer 6205111469 (active contract) appear as ONE row on Request POD tab with merged data.
 *
 * AS-IS (bug open): reminder second-tab shows both customers; load/view POD tab collapses to one row
 * with contracts/liabilities from both customers. Test PASSES while defect is open.
 * TO-BE (Confluence 72155868): one row per POD with data scoped to displayed customer only.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 * - tests/cursor/PDT-2861-rfd-pod-reconnection-fk.spec.ts
 * - tests/receivableManagement/requestForDisconnection.spec.ts
 */

import { test, expect } from './cursor-test.fixtures';
import { finalizeTestRunSummary } from './shared/manual-verification-links.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { randomGens } from '../../utils/randomGens';
import {
  assertPdt2971AsIsMergedPodTabRow,
  buildManualInvoicePayloadForContract,
  CONTRACT_A_INDEX,
  CONTRACT_B_INDEX,
  createRequestForDisconnectionDraft,
  CUSTOMER_A_INDEX,
  CUSTOMER_B_INDEX,
  executeReminderForDisconnection,
  fetchLoadCustomersForDps,
  fetchReminderSecondTab,
  fetchViewPodTab,
  isoDate,
  reminderIdFromResponses,
  reminderRowsForCustomerIdentifier,
  resolveContractNumber,
  resolvePodIdentifier,
  rowsForPodIdentifier,
  shiftCustomerLiabilitiesToYesterday,
  type Pdt2971Fx,
} from './pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures';

test.describe(
  '[PDT-2971]: Request for disconnection - a POD with 2 contracts for different customers is listed on 1 row',
  { tag: ['@receivableManagement', '@pdt-2971', '@dev'] },
  () => {
    test(
      '[PDT-2971]: Request for disconnection - a POD with 2 contracts for different customers is listed on 1 row',
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(20 * 60 * 1000);

        const fx: Pdt2971Fx = { Request, GeneratePayload, Responses, Endpoints };
        const YESTERDAY = isoDate(-1);
        const TOMORROW = isoDate(1);
        const TODAY = isoDate(0);

        let podIdentifier = '';
        let contractNumberA = '';
        let contractNumberB = '';
        let requestDraftId = 0;
        let reminderId = 0;

        const dpsQuery = () => ({
          page: 0,
          size: 100,
          conditionType: 'ALL_CUSTOMERS' as const,
          powerSupplyDisconnectionReminderId: reminderId,
          gridOperatorId: envVariables.grid_operator,
        });

        await test.step('Precondition: termination catalogue (auto POD deactivation)', async () => {
          const terminationPayload = GeneratePayload.productAndServices.termination(
            'DEACTIVATION_OF_POINTS_OF_DELIVERY',
          );
          terminationPayload.autoTermination = true;
          terminationPayload.autoTerminationFrom = 'EVENT_DATE';
          const terminationRes = await Request.post(Endpoints.termination, { data: terminationPayload });
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
          const groupRes = await Request.post(Endpoints.groupOfTerminations, { data: groupPayload });
          await expect(groupRes).CheckResponse();
          const groupBody = await groupRes.json();
          const terminationGroupId = typeof groupBody === 'number' ? groupBody : groupBody.id;
          const termRes = await Request.post(Endpoints.terms, {
            data: GeneratePayload.productAndServices.term(),
          });
          await expect(termRes).CheckResponse();
          Responses.terms.push(await termRes.json());
          const productPayload = GeneratePayload.productAndServices.product();
          productPayload.contractTypes = ['SUPPLY_ONLY'];
          productPayload.paymentGuarantees = ['NO'];
          productPayload.terminationIds = [];
          productPayload.terminationGroupIds = [terminationGroupId];
          const productRes = await Request.post(Endpoints.product, { data: productPayload });
          await expect(productRes).CheckResponse();
          Responses.product.push(await productRes.json());
        });

        await test.step('Precondition: Customer A (former — will keep unpaid liabilities on shared POD)', async () => {
          const res = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_private(),
          });
          await expect(res).CheckResponse();
          Responses.customer.push(await res.json());
        });

        await test.step('Precondition: Customer B (current — active contract on same POD)', async () => {
          const res = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_private(),
          });
          await expect(res).CheckResponse();
          Responses.customer.push(await res.json());
        });

        await test.step('Precondition: shared POD', async () => {
          const res = await Request.post(Endpoints.pod, {
            data: GeneratePayload.pointsOfDelivery.pod_settlement(),
          });
          await expect(res).CheckResponse();
          Responses.pod.push(await res.json());
          podIdentifier = await resolvePodIdentifier(fx);
        });

        await test.step('Contract A: create and activate for Customer A on shared POD', async () => {
          const payload = await GeneratePayload.contractsAndOrders.product_contract(
            CUSTOMER_A_INDEX,
            0,
            0,
          );
          payload.basicParameters.customerId = Responses.customer[CUSTOMER_A_INDEX].id;
          payload.basicParameters.status = 'ENTERED_INTO_FORCE';
          payload.basicParameters.subStatus = 'AWAITING_ACTIVATION';
          payload.basicParameters.entryInForceDate = TODAY;
          payload.productParameters.contractType = 'SUPPLY_ONLY';
          const res = await Request.post(Endpoints.productContract, { data: payload });
          await expect(res).CheckResponse();
          Responses.productContract.push(await res.json());
          contractNumberA = await resolveContractNumber(fx, CONTRACT_A_INDEX);
        });

        await test.step('Contract A: POD activation + ACTIVE_IN_PERPETUITY', async () => {
          const activation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(
              0,
              YESTERDAY,
              undefined,
              CONTRACT_A_INDEX,
            ),
          });
          await expect(activation).CheckResponse();
          const contractId = Responses.productContract[CONTRACT_A_INDEX].id;
          const statusUpdate = await Request.put(
            `product-contract/status-update/${contractId}?versionId=1`,
            {
              data: {
                contractStatus: 'ACTIVE_IN_PERPETUITY',
                contractSubStatus: 'DELIVERY',
                contractVersionStatus: 'SIGNED',
              },
            },
          );
          await expect(statusUpdate).CheckResponse();
        });

        await test.step('Contract A: billing → unpaid liability on shared POD', async () => {
          const invoicePayload = await buildManualInvoicePayloadForContract(
            fx,
            CUSTOMER_A_INDEX,
            CONTRACT_A_INDEX,
          );
          const billingRes = await Request.post(Endpoints.billingRun, { data: invoicePayload });
          await expect(billingRes).CheckResponse();
          Responses.billingRun.push(await billingRes.json());
          await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 0);
          await shiftCustomerLiabilitiesToYesterday(fx, CUSTOMER_A_INDEX);
        });

        await test.step('Contract A: deactivate POD and terminate (liabilities remain)', async () => {
          const deactivation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(
              0,
              YESTERDAY,
              YESTERDAY,
              CONTRACT_A_INDEX,
            ),
          });
          await expect(deactivation).CheckResponse();
          const contractId = Responses.productContract[CONTRACT_A_INDEX].id;
          const termRes = await Request.get(`ttest/pod-termination?contractId=${contractId}`);
          if (termRes.status() >= 400) {
            const fallback = await Request.put(`product-contract/status-update/${contractId}?versionId=1`, {
              data: {
                contractStatus: 'TERMINATED',
                contractSubStatus: 'ALL_PODS_ARE_DEACTIVATED',
                contractVersionStatus: 'SIGNED',
              },
            });
            await expect(fallback).CheckResponse();
          }
        });

        await test.step('Contract B: create for Customer B on same shared POD', async () => {
          const payload = await GeneratePayload.contractsAndOrders.product_contract(
            CUSTOMER_B_INDEX,
            0,
            0,
          );
          payload.basicParameters.customerId = Responses.customer[CUSTOMER_B_INDEX].id;
          payload.productParameters.contractType = 'SUPPLY_ONLY';
          const res = await Request.post(Endpoints.productContract, { data: payload });
          await expect(res).CheckResponse();
          Responses.productContract.push(await res.json());
          contractNumberB = await resolveContractNumber(fx, CONTRACT_B_INDEX);
        });

        await test.step('Contract B: POD activation', async () => {
          const activation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(
              0,
              TOMORROW,
              undefined,
              CONTRACT_B_INDEX,
            ),
          });
          await expect(activation).CheckResponse();
        });

        await test.step('Contract B: billing → second unpaid liability on shared POD', async () => {
          const invoicePayload = await buildManualInvoicePayloadForContract(
            fx,
            CUSTOMER_B_INDEX,
            CONTRACT_B_INDEX,
          );
          const billingRes = await Request.post(Endpoints.billingRun, { data: invoicePayload });
          await expect(billingRes).CheckResponse();
          Responses.billingRun.push(await billingRes.json());
          await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 1);
          await shiftCustomerLiabilitiesToYesterday(fx, CUSTOMER_B_INDEX);
        });

        await test.step('Action: reminder for disconnection (both customers)', async () => {
          const reminderPayload = GeneratePayload.receivablesManagement.reminderForDisconnection();
          const reminderRes = await Request.post(Endpoints.reminderForDisconnection, {
            data: reminderPayload,
          });
          await expect(reminderRes).CheckResponse();
          Responses.reminderForDisconnection.push(await reminderRes.json());
          reminderId = reminderIdFromResponses(Responses);
          await executeReminderForDisconnection(fx);
        });

        const customerAIdentifier = String(Responses.customer[CUSTOMER_A_INDEX].identifier);
        const customerBIdentifier = String(Responses.customer[CUSTOMER_B_INDEX].identifier);
        const customerBId = Number(Responses.customer[CUSTOMER_B_INDEX].id);

        TestRunSummary.registerPayload('customerA', Responses.customer[CUSTOMER_A_INDEX]);
        TestRunSummary.registerPayload('customerB', Responses.customer[CUSTOMER_B_INDEX]);
        TestRunSummary.registerPayload('pod', { id: Responses.pod[0].id, identifier: podIdentifier });
        TestRunSummary.registerPayload('reminderId', { reminderId });

        await test.step('Assert: reminder second-tab lists both customers (two rows — prod symptom baseline)', async () => {
          const secondTabRows = await fetchReminderSecondTab(fx, reminderId);
          const rowsA = reminderRowsForCustomerIdentifier(secondTabRows, customerAIdentifier);
          const rowsB = reminderRowsForCustomerIdentifier(secondTabRows, customerBIdentifier);
          expect(rowsA.length, 'Customer A should appear on reminder POD/customer tab').toBeGreaterThanOrEqual(
            1,
          );
          expect(rowsB.length, 'Customer B should appear on reminder POD/customer tab').toBeGreaterThanOrEqual(
            1,
          );
          TestRunSummary.recordCheck({
            check: 'Reminder second-tab — two customers on shared POD scenario',
            expectedResult:
              'Separate reminder rows for Customer A and Customer B (same POD, different customers).',
            actualResult: `Customer A rows=${rowsA.length}, Customer B rows=${rowsB.length}.`,
            passed: rowsA.length >= 1 && rowsB.length >= 1,
          });
        });

        await test.step('Action: create Request for disconnection as DRAFT (ALL_CUSTOMERS)', async () => {
          requestDraftId = await createRequestForDisconnectionDraft(fx);
          Responses.requestForDisconnection.push(requestDraftId);
        });

        await test.step('Assert: load-customer-for-disconnection AS-IS — same single merged POD row as view-pod-tab', async () => {
          const loadRows = await fetchLoadCustomersForDps(fx, dpsQuery());
          const podRows = rowsForPodIdentifier(loadRows, podIdentifier);
          expect(
            podRows.length,
            'Load PODs uses customersForDPS — AS-IS collapses shared POD to one row (same as view-pod-tab)',
          ).toBe(1);
          assertPdt2971AsIsMergedPodTabRow({
            row: podRows[0],
            customerBId,
            customerBIdentifier,
            contractNumberA,
            contractNumberB,
          });
          TestRunSummary.recordCheck({
            check: 'Load PODs tab — merged row matches view-pod-tab defect',
            expectedResult:
              'AS-IS: one row per POD on load-customer API with cross-customer contracts (reminder second-tab still lists both customers separately).',
            actualResult: `podRows=${podRows.length}; contracts=${podRows[0]?.contracts}.`,
            passed: podRows.length === 1,
          });
        });

        await test.step('Assert: view-pod-tab AS-IS — one merged row with cross-customer contracts (PDT-2971)', async () => {
          const viewRows = await fetchViewPodTab(fx, requestDraftId, dpsQuery());
          const podRows = rowsForPodIdentifier(viewRows, podIdentifier);
          expect(
            podRows.length,
            'AS-IS bug: Request POD tab collapses multiple customers on one POD into a single row',
          ).toBe(1);

          const mergedRow = podRows[0];
          assertPdt2971AsIsMergedPodTabRow({
            row: mergedRow,
            customerBId,
            customerBIdentifier,
            contractNumberA,
            contractNumberB,
          });

          const mergePassed =
            podRows.length === 1 &&
            String(mergedRow.contracts ?? '').includes(contractNumberA) &&
            String(mergedRow.contracts ?? '').includes(contractNumberB);
          TestRunSummary.recordCheck({
            check: 'Request POD tab — cross-customer contract merge on single row',
            expectedResult:
              'AS-IS defect: one row per POD with contracts from both Customer A (terminated) and Customer B (active).',
            actualResult: `contracts=${mergedRow.contracts}; customerId=${mergedRow.customerId}.`,
            passed: mergePassed,
          });

          test.info().attach('[PDT-2971] view-pod-tab merged row', {
            body: JSON.stringify(mergedRow, null, 2),
            contentType: 'application/json',
          });
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-2971',
            relevantEntityKeys: [
              'customer',
              'pod',
              'productContract',
              'billingRun',
              'reminderForDisconnection',
              'requestForDisconnection',
            ],
            snapshot: {
              podIdentifier,
              contractNumberA,
              contractNumberB,
              reminderId,
              requestDraftId,
              customerAIdentifier,
              customerBIdentifier,
            },
          });
        });
      },
    );
  },
);
