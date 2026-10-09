/**
 * PDT-3509 — Save And Execute on a second RFD fails because the POD is already
 * checked on another EXECUTED request, even though that check is stored under a
 * different customer than the contract owner.
 *
 * Asserts today's faulty behavior (HTTP 400), not the desired fix.
 * Bug-only (no Backend TC .md). Environment: dev. No test.beforeAll.
 *
 * Runtime BASE_URL Dev: http://10.236.20.11:8091
 * The Playwright default base URL is http://10.236.20.81:8094 (experiments).
 * Product type 1000 is ACTIVE on Dev and DELETED on experiments, so this spec
 * must be run with BASE_URL set to the Dev API:
 *   $env:BASE_URL='http://10.236.20.11:8091'; npx playwright test tests/cursor/PDT-3509-rfd-pod-blocked-by-other-customer.spec.ts
 *
 * Data (fresh, not the Dev rows from the ticket):
 * - Customer A owns the POD, an active product contract, and an overdue liability.
 * - Customer B is a different customer and has no contract on that POD.
 * - Reminder INCLUDED for Customer A only. No POST .../reminder/job (PDT-3179).
 * - First RFD: explicit pods[] only (allSelected false, podWithHighestConsumption
 *   false) with Customer A's podId, Customer B's customerId, isChecked true.
 *   Create DRAFT, then PUT EXECUTED.
 * - Second RFD: same grid operator, supplierType CURRENT, reminder that includes
 *   Customer A, podWithHighestConsumption true, pods [], PUT EXECUTED.
 * - Expected today: second PUT HTTP 400, OperationNotAllowedException, message
 *   contains Customer A's POD identifier and the first request number.
 * - If the first execute rejects the cross-customer pods payload, stop and
 *   record that in the test run summary. Do not seed the database.
 *
 * Swagger (dev, update-swagger-specs.ps1 this session — all envs OK):
 * - POST /disconnection-of-power-supply-requests DPSRequestsBaseRequest
 * - PUT /disconnection-of-power-supply-requests/{id} same body
 *   required: conditionType, gridOpRequestRegDate, gridOperatorId,
 *   reasonOfDisconnectionId, reminderForDisconnectionId, supplierType
 *   disconnectionRequestsStatus: DRAFT | EXECUTED | FEE_CHARGED
 *   supplierType: CURRENT | PREVIOUS
 *   conditionType: ALL_CUSTOMERS | CUSTOMERS_UNDER_CONDITIONS | LIST_OF_CUSTOMERS
 *   pods[] items CustomersForDPSResponse: podId, customerId, isChecked,
 *   podIdentifier, isHighestConsumption, podDetailId, gridOperatorId
 * - GET /disconnection-of-power-supply-requests/{id} requestNumber
 * - POST /power-supply-disconnection-reminder customerFilterType INCLUDED,
 *   customerList (dev schema; not Dev2 listOfCustomer)
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3465-rfd-highest-uncheck-manual-data.spec.ts
 * - tests/cursor/pdt-3421-rfd-highest-consumption-uncheck.fixtures.ts
 * - tests/cursor/PDT-3211-rfd-supplier-type-previous-additional-logic.spec.ts
 * - tests/cursor/pdt-3179-reconnection-invoice-emails.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  createOverdueManualLiabilityWithBillingGroup,
  createSupplyChain,
  customerIdentifier,
} from './pdt-3179-reconnection-invoice-emails.fixtures';
import {
  PDT_3509_KEY,
  PDT_3509_TEST_TIMEOUT_MS,
  PDT_3509_TITLE,
  buildCrossCustomerCheckedPod,
  buildRfdPayload,
  createCustomerWithoutContract,
  createReminderIncludedForCustomer,
  dpsRowCustomerId,
  dpsRowPodId,
  findLiabilityOnInvoice,
  getRfdView,
  gridOperatorIdFromEnv,
  isHttpSuccess,
  loadDpsRowsForIdentifier,
  pdt3509RelevantKeys,
  postDraftRfd,
  putRfd,
  reminderSecondTabIncludesCustomer,
  waitForCreatedReminderExecuted,
  type Pdt3509Fx,
  type Pdt3509HttpResult,
} from './pdt-3509-rfd-pod-blocked-by-other-customer.fixtures';
import { customerIdentOf } from './pdt-3421-rfd-highest-consumption-uncheck.fixtures';
import {
  DEV_PORTAL_BASE,
  runDevVolCompContractAndBbpPrechain,
} from './dev-volume-billing-two-compensations.fixtures';
import {
  createForVolumesDraftBillingRun,
  startAccountingAndWaitCompleted,
  startGeneratingAndWaitGenerated,
} from './pdt-3087-government-compensation-defer-to-pdf.fixtures';

const DEV_UI_PREFIX = 'http://10.236.20.11:8080';

function describesOperationNotAllowed(result: Pdt3509HttpResult): boolean {
  return (
    result.exceptionId.includes('OperationNotAllowedException') ||
    /OperationNotAllowedException/.test(result.haystack)
  );
}

test.describe(`[${PDT_3509_KEY}]: ${PDT_3509_TITLE}`, {
  tag: ['@dev', '@receivableManagement', '@pdt-3509'],
}, () => {
  test(`[${PDT_3509_KEY}]: ${PDT_3509_TITLE}`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(PDT_3509_TEST_TIMEOUT_MS);
    const fx: Pdt3509Fx = { Request, GeneratePayload, Responses, Endpoints };
    let stopReason: string | null = null;
    let podIdentifier = '';
    let firstRequestNumber = '';
    let firstRequestId = 0;
    let secondRequestId = 0;
    let customerAId = 0;
    let customerBId = 0;
    let contractAId = 0;
    let reminderId = 0;
    let secondPut: Pdt3509HttpResult | null = null;

    await test.step('Precondition: customer A supply chain (customer, product, contract, POD)', async () => {
      const chain = await createSupplyChain(fx);
      customerAId = chain.customerId;
      contractAId = chain.contractId;
      expect(customerAId, 'customer A id').toBeGreaterThan(0);
      expect(chain.podId, 'customer A podId').toBeGreaterThan(0);
      expect(contractAId, 'customer A product contract').toBeGreaterThan(0);
    });

    await test.step('Precondition: customer A overdue billing-group liability', async () => {
      const liability = await createOverdueManualLiabilityWithBillingGroup(fx);
      expect(liability.liabilityId, 'overdue liability').toBeGreaterThan(0);
      expect(liability.billingGroupId, 'billing group on liability').toBeGreaterThan(0);
    });

    const customerB = await test.step(
      'Precondition: customer B (different customer, no contract on customer A POD)',
      async () => createCustomerWithoutContract(fx),
    );
    customerBId = customerB.customerId;

    const identA = customerIdentifier(Responses);
    const identB = customerB.identifier;
    expect(identA.length, 'customer A identifier').toBeGreaterThan(0);
    expect(identA, 'customer A and customer B must be different customers').not.toBe(identB);
    TestRunSummary.registerPayload('customerA', { customerId: customerAId, identifier: identA });
    TestRunSummary.registerPayload('customerB', { customerId: customerBId, identifier: identB });

    reminderId = await test.step(
      'Precondition: reminder INCLUDED for customer A (no /job, no stolen reminder)',
      async () => {
        const id = await createReminderIncludedForCustomer(fx, identA);
        const status = await waitForCreatedReminderExecuted(fx, id);
        TestRunSummary.registerPayload('reminderForDisconnection', {
          reminderId: id,
          customerList: identA,
          customerFilterType: 'INCLUDED',
          reminderStatus: status,
        });
        if (status !== 'EXECUTED') {
          stopReason =
            `Stopped — reminder ${id} for customer A did not reach EXECUTED ` +
            `(status=${status || 'unknown'}). Did not POST /job and did not reuse another reminder.`;
        }
        return id;
      },
    );

    if (!stopReason) {
      const includesA = await test.step(
        'Precondition: reminder second tab includes customer A',
        async () => reminderSecondTabIncludesCustomer(fx, reminderId, identA),
      );
      if (!includesA) {
        stopReason =
          `Stopped — reminder ${reminderId} second tab has no row for customer A ${identA}. ` +
          'Highest-consumption execute cannot select this POD.';
      }
    }

    const crossPod = await test.step(
      'Precondition: pods row uses customer A podId and customer B customerId',
      async () => {
        const row = await buildCrossCustomerCheckedPod(fx, customerBId, identB);
        podIdentifier = row.podIdentifier;
        expect(row.podId, 'CustomersForDPSResponse.podId').toBeGreaterThan(0);
        expect(row.customerId, 'CustomersForDPSResponse.customerId is customer B').toBe(customerBId);
        expect(row.customerId, 'checked customer is not the contract owner').not.toBe(customerAId);
        expect(row.isChecked, 'isChecked').toBe(true);
        expect(podIdentifier.length, 'POD identifier').toBeGreaterThan(0);
        expect(row.gridOperatorId, 'pod grid operator matches env').toBe(gridOperatorIdFromEnv());
        TestRunSummary.registerPayload('crossCustomerPod', {
          podId: row.podId,
          podIdentifier,
          customerId: row.customerId,
          contractOwnerCustomerId: customerAId,
          isChecked: true,
        });
        return row;
      },
    );

    if (!stopReason) {
      await test.step(
        'First RFD: DRAFT then PUT EXECUTED with explicit pods only',
        async () => {
          const draftPayload = buildRfdPayload(fx, {
            reminderId,
            listOfCustomer: identB,
            disconnectionRequestsStatus: 'DRAFT',
            pods: [{ ...crossPod, isChecked: true, isHighestConsumption: false }],
            podWithHighestConsumption: false,
          });
          expect(draftPayload.allSelected, 'pods selection only').toBe(false);
          expect(draftPayload.podWithHighestConsumption, 'do not use highest consumption').toBe(false);
          expect(draftPayload.supplierType).toBe('CURRENT');
          firstRequestId = await postDraftRfd(fx, draftPayload);

          const executePayload = buildRfdPayload(fx, {
            reminderId,
            listOfCustomer: identB,
            disconnectionRequestsStatus: 'EXECUTED',
            pods: [{ ...crossPod, isChecked: true, isHighestConsumption: false }],
            podWithHighestConsumption: false,
          });
          TestRunSummary.registerPayload('firstRfdExecute', executePayload);
          const executed = await putRfd(fx, firstRequestId, executePayload);
          if (!isHttpSuccess(executed.status)) {
            stopReason =
              `Stopped — first EXECUTED save rejected the cross-customer pods payload ` +
              `(customer A podId ${crossPod.podId}, customer B customerId ${customerBId}). ` +
              `HTTP ${executed.status} exceptionId=${executed.exceptionId} ` +
              `message=${executed.message || executed.text.slice(0, 500)}`;
            TestRunSummary.recordCheck({
              check: 'First execute accepts explicit cross-customer pods row',
              expectedResult:
                'PUT EXECUTED returns HTTP 2xx so the POD is checked on an executed request under customer B.',
              actualResult: stopReason,
              passed: false,
            });
            return;
          }

          const view = await getRfdView(fx, firstRequestId);
          const status = String(view.disconnectionRequestsStatus ?? '');
          firstRequestNumber = String(view.requestNumber ?? '');
          const accepted = status === 'EXECUTED' && firstRequestNumber.length > 0;
          TestRunSummary.recordCheck({
            check: 'First RFD is EXECUTED with a request number',
            expectedResult: 'GET disconnectionRequestsStatus EXECUTED and requestNumber is present.',
            actualResult: accepted
              ? `As expected — id=${firstRequestId} status=${status} requestNumber=${firstRequestNumber}.`
              : `Not as expected — id=${firstRequestId} status=${status} requestNumber=${firstRequestNumber}.`,
            passed: accepted,
          });
          if (!accepted) {
            stopReason =
              `Stopped — first RFD ${firstRequestId} is not a usable executed request ` +
              `(status=${status}, requestNumber=${firstRequestNumber}).`;
          }
        },
      );
    }

    if (!stopReason) {
      await test.step(
        'Second RFD: DRAFT then PUT EXECUTED with highest consumption and empty pods',
        async () => {
          const draftPayload = buildRfdPayload(fx, {
            reminderId,
            listOfCustomer: identA,
            disconnectionRequestsStatus: 'DRAFT',
            pods: [],
            podWithHighestConsumption: true,
          });
          expect(draftPayload.supplierType).toBe('CURRENT');
          expect(draftPayload.gridOperatorId).toBe(gridOperatorIdFromEnv());
          expect(draftPayload.allSelected).toBe(false);
          expect(draftPayload.pods).toEqual([]);
          secondRequestId = await postDraftRfd(fx, draftPayload);

          const executePayload = buildRfdPayload(fx, {
            reminderId,
            listOfCustomer: identA,
            disconnectionRequestsStatus: 'EXECUTED',
            pods: [],
            podWithHighestConsumption: true,
          });
          TestRunSummary.registerPayload('secondRfdExecute', executePayload);
          secondPut = await putRfd(fx, secondRequestId, executePayload);

          const operationNotAllowed = describesOperationNotAllowed(secondPut);
          const messageText = secondPut.message || secondPut.haystack;
          const mentionsPod = messageText.includes(podIdentifier);
          const mentionsRequest = messageText.includes(firstRequestNumber);
          const passed =
            secondPut.status === 400 &&
            operationNotAllowed &&
            mentionsPod &&
            mentionsRequest;

          TestRunSummary.recordCheck({
            check: 'Second Save And Execute is blocked by the POD already on the first executed request',
            expectedResult:
              `HTTP 400, exceptionId or body contains OperationNotAllowedException, ` +
              `message contains POD ${podIdentifier} and first request ${firstRequestNumber}.`,
            actualResult: passed
              ? `As expected — HTTP ${secondPut.status} exceptionId=${secondPut.exceptionId} ` +
                `errorCode=${secondPut.errorCode} message=${secondPut.message}`
              : `Not as expected — HTTP ${secondPut.status} exceptionId=${secondPut.exceptionId} ` +
                `errorCode=${secondPut.errorCode} message=${secondPut.message || secondPut.text.slice(0, 500)}`,
            passed,
          });
          if (!passed) {
            stopReason =
              `Second PUT did not reproduce the bug. HTTP ${secondPut.status} ` +
              `exceptionId=${secondPut.exceptionId} message=${secondPut.message}`;
          }
        },
      );
    }

    if (stopReason && !secondPut) {
      TestRunSummary.recordCheck({
        check: 'PDT-3509 stopped before the second execute assertion',
        expectedResult:
          'First executed request exists, then the second Save And Execute returns HTTP 400.',
        actualResult: stopReason,
        passed: false,
      });
    }

    await test.step('Attach test run summary', async () => {
      const extra: Record<string, string[]> = {};
      if (firstRequestId > 0) {
        extra.requestForDisconnection = [
          `${DEV_UI_PREFIX}/request-for-disconnection/preview?id=${firstRequestId}`,
        ];
      }
      if (secondRequestId > 0) {
        extra.requestForDisconnection = [
          ...(extra.requestForDisconnection ?? []),
          `${DEV_UI_PREFIX}/request-for-disconnection/preview?id=${secondRequestId}`,
        ];
      }
      if (reminderId > 0) {
        extra.reminderForDisconnection = [
          `${DEV_UI_PREFIX}/reminder-for-disconnection/preview?id=${reminderId}`,
        ];
      }
      if (customerAId > 0) {
        extra.customer = [`${DEV_UI_PREFIX}/customers/preview/basic?id=${customerAId}`];
      }
      if (customerBId > 0) {
        extra.customer = [
          ...(extra.customer ?? []),
          `${DEV_UI_PREFIX}/customers/preview/basic?id=${customerBId}`,
        ];
      }
      if (crossPod.podId > 0) {
        extra.pod = [`${DEV_UI_PREFIX}/points-of-delivery/preview?id=${crossPod.podId}`];
      }
      if (contractAId > 0) {
        const contractLinks = buildProductContractTabLinks(contractAId).productContract;
        if (Array.isArray(contractLinks) && contractLinks.length > 0) {
          extra.productContract = contractLinks;
        }
      }

      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3509_KEY,
        relevantEntityKeys: pdt3509RelevantKeys(),
        extraLinks: Object.keys(extra).length ? extra : undefined,
        snapshot: {
          customerAId,
          customerBId,
          podId: crossPod.podId,
          podIdentifier,
          firstRequestId,
          firstRequestNumber,
          secondRequestId,
          reminderId,
          stopReason,
        },
      });
    });

    if (secondPut) {
      const messageText = secondPut.message || secondPut.haystack;
      expect(secondPut.status, 'second Save And Execute HTTP status').toBe(400);
      expect(
        describesOperationNotAllowed(secondPut),
        `exceptionId or body must contain OperationNotAllowedException: ${secondPut.haystack.slice(0, 500)}`,
      ).toBe(true);
      expect(messageText, 'error message contains customer A POD identifier').toContain(podIdentifier);
      expect(messageText, 'error message contains the first request number').toContain(firstRequestNumber);
    }
    expect(stopReason, stopReason ?? 'PDT-3509').toBeNull();
  });

  test(`[${PDT_3509_KEY}]: ${PDT_3509_TITLE} | government alt-recipient liability`, async ({
    Request, GeneratePayload, Responses, Endpoints, TestRunSummary,
  }) => {
    test.setTimeout(55 * 60 * 1000);
    const fx: Pdt3509Fx = { Request, GeneratePayload, Responses, Endpoints };
    let stopReason: string | null = null;
    let podIdentifier = '';
    let firstRequestNumber = '';
    let firstRequestId = 0;
    let secondRequestId = 0;
    let billedCustomerId = 0;
    let recipientCustomerId = 0;
    let contractId = 0;
    let podId = 0;
    let invoiceId = 0;
    let billingRunId = 0;
    let recipientReminderId = 0;
    let ownerReminderId = 0;
    let secondPut: Pdt3509HttpResult | null = null;

    const chain = await test.step(
      'Precondition: billed customer, alt recipient, VAT-base price component, contract, POD, profile',
      async () => runDevVolCompContractAndBbpPrechain(
        fx as unknown as Parameters<typeof runDevVolCompContractAndBbpPrechain>[0],
        { governmentAltRecipient: true },
      ),
    );
    billedCustomerId = chain.customerId;
    recipientCustomerId = chain.recipientCustomerId;
    contractId = chain.contractId;
    podId = chain.podId;
    expect(recipientCustomerId, 'alt recipient differs from billed customer').not.toBe(billedCustomerId);

    const billedBody = Responses.customer[0] as Record<string, unknown>;
    const recipientBody = Responses.customer[1] as Record<string, unknown>;
    const billedIdentifier = customerIdentOf(billedBody);
    const recipientIdentifier = customerIdentOf(recipientBody);
    expect(billedIdentifier.length, 'billed customer identifier').toBeGreaterThan(0);
    expect(recipientIdentifier.length, 'alt recipient identifier').toBeGreaterThan(0);

    const accountingPeriodId = await test.step('Precondition: open accounting period', async () => {
      const listed = await Request.get('accounting-period/available-accounting-periods', {
        params: { page: 0, size: 5 },
      });
      await expect(listed).CheckResponse();
      const page = (await listed.json()) as { content?: Array<{ id?: number }> };
      const id = Number(page.content?.[0]?.id ?? 0);
      expect(id, 'open accounting period').toBeGreaterThan(0);
      return id;
    });

    const invoiceDate = `${new Date().toISOString().slice(0, 8)}01`;
    const volume = await test.step('Precondition: FOR_VOLUMES draft invoice', async () =>
      createForVolumesDraftBillingRun(
        fx as unknown as Parameters<typeof createForVolumesDraftBillingRun>[0],
        { accountingPeriodId, invoiceDate },
      ),
    );
    billingRunId = volume.billingRunId;
    expect(volume.draftInvoiceIds.length, 'draft invoice').toBeGreaterThan(0);
    invoiceId = volume.draftInvoiceIds[0];

    await test.step('Precondition: generate and account the invoice (creates both liabilities)', async () => {
      await startGeneratingAndWaitGenerated(Request, billingRunId);
      await startAccountingAndWaitCompleted(Request, billingRunId);
    });

    const recipientLiability = await test.step(
      'Read: alt recipient automatic liability on the billed invoice',
      async () => findLiabilityOnInvoice(fx, recipientIdentifier, invoiceId),
    );
    if (!recipientLiability) {
      stopReason = `No liability for alt recipient ${recipientCustomerId} on invoice ${invoiceId}. VAT-base alt recipient did not create the twin liability.`;
    }

    const ownerLiability = recipientLiability
      ? await test.step('Read: billed customer liability on the same invoice', async () =>
        findLiabilityOnInvoice(fx, billedIdentifier, invoiceId),
      )
      : null;

    if (!stopReason && recipientLiability) {
      const due = String(recipientLiability.dueDate ?? '');
      const dueDay = due.slice(0, 10);
      const today = new Date().toISOString().slice(0, 10);
      if (dueDay > today) {
        stopReason = `Alt-recipient liability dueDate ${dueDay} is still in the future, so Request for Disconnection load will not list it (due_date <= today).`;
      }
    }

    if (!stopReason) {
      recipientReminderId = await test.step(
        'Precondition: reminder INCLUDED for the alt recipient only',
        async () => createReminderIncludedForCustomer(fx, recipientIdentifier, {
          emailTemplateId: 1020,
          smsTemplateId: 1086,
        }),
      );
      const recipientReminderStatus = await waitForCreatedReminderExecuted(fx, recipientReminderId);
      if (recipientReminderStatus !== 'EXECUTED') {
        stopReason = `Alt-recipient reminder ${recipientReminderId} status=${recipientReminderStatus || 'unknown'}`;
      }
    }

    let loadedForRecipient: Record<string, unknown> | undefined;
    if (!stopReason) {
      const rows = await test.step('Load PODs for the alt-recipient reminder', async () =>
        loadDpsRowsForIdentifier(fx, recipientReminderId, recipientIdentifier),
      );
      loadedForRecipient = rows.find((row) => dpsRowPodId(row) === podId);
      if (!loadedForRecipient) {
        const billingGroupId = recipientLiability?.contractBillingGroupId ?? recipientLiability?.billingGroupId ?? null;
        stopReason = `Load PODs for reminder ${recipientReminderId} did not return pod ${podId} (rows=${rows.length}). Alt-recipient liability billing group=${billingGroupId ?? 'null'}. Reminder execution only attaches liabilities that have a contract billing group, so a government VAT-base liability is left off the reminder.`;
      } else if (dpsRowCustomerId(loadedForRecipient) !== recipientCustomerId) {
        stopReason = `Load attached pod ${podId} to customer ${dpsRowCustomerId(loadedForRecipient)}, not alt recipient ${recipientCustomerId}.`;
      }
    }

    if (!stopReason && loadedForRecipient) {
      podIdentifier = String(loadedForRecipient.podIdentifier ?? '');
      const checked = { ...loadedForRecipient, isChecked: true };
      const draftPayload = buildRfdPayload(fx, {
        reminderId: recipientReminderId,
        listOfCustomer: recipientIdentifier,
        disconnectionRequestsStatus: 'DRAFT',
        pods: [checked as never],
        podWithHighestConsumption: false,
      });
      firstRequestId = await test.step('Create draft RFD from the loaded alt-recipient POD row', async () =>
        postDraftRfd(fx, draftPayload),
      );
      const executePayload = { ...draftPayload, disconnectionRequestsStatus: 'EXECUTED' };
      const firstPut = await test.step('Save And Execute the alt-recipient RFD', async () =>
        putRfd(fx, firstRequestId, executePayload),
      );
      if (!isHttpSuccess(firstPut.status)) {
        stopReason = `First execute rejected the loaded row HTTP ${firstPut.status}: ${firstPut.message || firstPut.haystack.slice(0, 400)}`;
      } else {
        const view = await getRfdView(fx, firstRequestId);
        firstRequestNumber = String(view.requestNumber ?? '');
        expect(firstRequestNumber.length, 'first requestNumber').toBeGreaterThan(0);
      }
    }

    if (!stopReason) {
      ownerReminderId = await test.step(
        'Precondition: reminder INCLUDED for the billed customer',
        async () => createReminderIncludedForCustomer(fx, billedIdentifier, {
          emailTemplateId: 1020,
          smsTemplateId: 1086,
        }),
      );
      const ownerStatus = await waitForCreatedReminderExecuted(fx, ownerReminderId);
      if (ownerStatus !== 'EXECUTED') {
        stopReason = `Billed-customer reminder ${ownerReminderId} status=${ownerStatus || 'unknown'}`;
      }
    }

    if (!stopReason) {
      const ownerRows = await test.step('Load PODs for the billed-customer reminder', async () =>
        loadDpsRowsForIdentifier(fx, ownerReminderId, billedIdentifier),
      );
      const ownerRow = ownerRows.find((row) => dpsRowPodId(row) === podId);
      if (!ownerRow) {
        stopReason = `Load PODs for billed customer did not return pod ${podId} (rows=${ownerRows.length}).`;
      } else if (dpsRowCustomerId(ownerRow) !== billedCustomerId) {
        stopReason = `Billed-customer load attached pod ${podId} to customer ${dpsRowCustomerId(ownerRow)}, not ${billedCustomerId}.`;
      } else {
        const secondDraft = buildRfdPayload(fx, {
          reminderId: ownerReminderId,
          listOfCustomer: billedIdentifier,
          disconnectionRequestsStatus: 'DRAFT',
          pods: [],
          podWithHighestConsumption: true,
        });
        secondRequestId = await postDraftRfd(fx, secondDraft);
        secondPut = await putRfd(fx, secondRequestId, {
          ...secondDraft,
          disconnectionRequestsStatus: 'EXECUTED',
        });
      }
    }

    TestRunSummary.recordCheck({
      check: 'Alt-recipient liability makes the POD checked, then the owner execute is blocked',
      expectedResult: 'HTTP 400 OperationNotAllowedException naming this POD and the first request number',
      actualResult: stopReason
        ?? `HTTP ${secondPut?.status} ${secondPut?.message || ''} request=${firstRequestNumber}`,
      passed: !stopReason && secondPut?.status === 400 && describesOperationNotAllowed(secondPut),
    });

    await test.step('Attach test run summary', async () => {
      const extra: Record<string, string[]> = {
        customer: [
          `${DEV_PORTAL_BASE}/customers/preview/basic?id=${billedCustomerId}`,
          `${DEV_PORTAL_BASE}/customers/preview/basic?id=${recipientCustomerId}`,
        ],
        pod: [`${DEV_PORTAL_BASE}/points-of-delivery/preview?id=${podId}`],
      };
      if (contractId > 0) {
        const contractLinks = buildProductContractTabLinks(contractId).productContract;
        if (Array.isArray(contractLinks) && contractLinks.length > 0) {
          extra.productContract = contractLinks;
        }
      }
      if (invoiceId > 0) {
        extra.invoice = [
          `${DEV_PORTAL_BASE}/billing-run/invoices/preview/basic-parameters?id=${invoiceId}`,
        ];
      }
      if (billingRunId > 0) {
        extra.billingRun = [
          `${DEV_PORTAL_BASE}/billing-run/preview/basic-parameters?type=STANDARD_BILLING&id=${billingRunId}`,
        ];
      }
      if (firstRequestId > 0) {
        extra.requestForDisconnection = [
          `${DEV_PORTAL_BASE}/request-for-disconnection/preview?id=${firstRequestId}`,
        ];
      }
      if (secondRequestId > 0) {
        extra.requestForDisconnection = [
          ...(extra.requestForDisconnection ?? []),
          `${DEV_PORTAL_BASE}/request-for-disconnection/preview?id=${secondRequestId}`,
        ];
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PDT_3509_KEY,
        relevantEntityKeys: [
          ...pdt3509RelevantKeys(),
          'billingRun',
          'invoice',
        ],
        extraLinks: extra,
        snapshot: {
          billedCustomerId,
          recipientCustomerId,
          podId,
          podIdentifier,
          invoiceId,
          billingRunId,
          contractId,
          recipientReminderId,
          ownerReminderId,
          firstRequestId,
          firstRequestNumber,
          secondRequestId,
          recipientLiabilityId: recipientLiability?.id ?? null,
          ownerLiabilityId: ownerLiability?.id ?? null,
          stopReason,
        },
      });
    });

    if (secondPut) {
      const messageText = secondPut.message || secondPut.haystack;
      expect(secondPut.status, 'owner Save And Execute HTTP status').toBe(400);
      expect(describesOperationNotAllowed(secondPut), secondPut.haystack.slice(0, 500)).toBe(true);
      if (podIdentifier) {
        expect(messageText, 'error names the POD').toContain(podIdentifier);
      }
      expect(messageText, 'error names the first request').toContain(firstRequestNumber);
    }
    expect(stopReason, stopReason ?? 'PDT-3509 government alt-recipient').toBeNull();
  });
});
