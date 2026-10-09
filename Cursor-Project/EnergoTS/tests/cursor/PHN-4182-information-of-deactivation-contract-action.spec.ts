/**
 * PHN-4182 — Information of deactivation replaces covered POD termination
 * actions with one contract termination without notice action.
 *
 * Jira: Change in actions for terminations and penalties process
 * Confluence: Change in actions for terminations and penalties process (page 1129086986)
 *
 * Swagger (dev2):
 * - POST /mass-import/{domainType}/files/upload domainType=SUPPLY_ACTION_DEACTIVATIONS
 * - GET  /actions/list searchBy=CUSTOMER_IDENTIFIER
 * - GET  /actions/{id}
 * - POST /penalties
 * - POST /product-contract
 * - POST /contract-pods/manual
 *
 * Reference spec(s):
 * - tests/cursor/PHN-3998-actions-incorrect-action-type.spec.ts
 * - tests/cursor/phn-3998-actions-incorrect-action-type.fixtures.ts
 * - tests/cursor/pdt-2177-supply-action-deactivation-restricted-period.fixtures.ts
 *
 * Target env: Dev2
 *   BASE_URL=https://devapps.energo-pro.bg/backend/phoenix2-dev
 *
 * Swagger refresh note: update-swagger-specs.ps1 is Windows-oriented (curl.exe).
 * Endpoint names below were checked against the cached Dev2 spec and the
 * PHN-3998 tests that already call them.
 */
import { test, expect } from './cursor-test.fixtures';

const PHN_4182_TITLE = 'Change in actions for terminations and penalties process';
import {
  finalizeTestRunSummary,
  buildProcessPreviewLink,
  buildProductContractTabLinks,
  resolveFrontendBaseUrlOverride,
} from './shared/manual-verification-links.fixtures';
import { addDaysIso } from './pdt-2177-supply-action-deactivation-restricted-period.fixtures';
import {
  CONTRACT_ACTION_REPORT_SNIPPET,
  CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
  PHN_4182_KEY,
  POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID,
  POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
  activeOnDate,
  createManualAction,
  createTwoPodContractScenario,
  isActiveRecord,
  runRealSupplyDeactivation,
  runSupplyActionDeactivationImport,
  saveNextVersionWithOnePod,
  summarizeActions,
  waitForCustomerActions,
  type ActionDetailSnapshot,
  type ImportFlowResult,
  type TwoPodContractScenario,
} from './phn-4182-information-of-deactivation-contract-action.fixtures';

test.describe(`[${PHN_4182_KEY}] Information of deactivation contract action`, {
  tag: ['@massImport', '@supplyActivation', '@action', '@phn-4182', '@dev2'],
}, () => {
  test(`[${PHN_4182_KEY}] TC-BE-1: ${'Change in actions for terminations and penalties process'} — both PODs replaced by one contract action`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);

    const scenario = await test.step(
      'Precondition: one signed contract, two activated PODs, POD and contract penalties',
      async () =>
        createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints }),
    );

    TestRunSummary.registerPayload('customer', {
      id: scenario.customerId,
      identifier: scenario.customerIdentifier,
    });
    TestRunSummary.registerPayload('productContract', { id: scenario.contractId });
    TestRunSummary.registerPayload('pod', {
      identifiers: scenario.podIdentifiers,
      activationDateIso: scenario.activationDateIso,
    });
    TestRunSummary.registerPayload('penalty', {
      podPenaltyId: scenario.podPenaltyId,
      contractPenaltyId: scenario.contractPenaltyId,
    });

    const flow = await test.step(
      'Upload Information of deactivation for both PODs with a future deactivation date',
      async () =>
        runSupplyActionDeactivationImport(
          Request,
          FileUploadRequest,
          scenario.podIdentifiers.map((podIdentifier) => ({
            podIdentifier,
            noticeReceivingDateIso: scenario.noticeReceivingDateIso,
          })),
          scenario.deactivationDateIso,
        ),
    );

    expect(flow.processStatus, 'mass import process status').toBe('COMPLETED');
    const hardErrors = flow.reportRows.filter(
      (row) => row.errors && !row.errors.includes(CONTRACT_ACTION_REPORT_SNIPPET),
    );
    const warningPresent = flow.reportText.includes(CONTRACT_ACTION_REPORT_SNIPPET);

    const actions = await test.step('Load actions created for the customer', async () =>
      waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
        loaded.some(
          (action) =>
            action.executionDate === scenario.deactivationDateIso &&
            action.actionTypeId === CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID &&
            isActiveRecord(action),
        ),
      ),
    );
    for (const action of actions) {
      Responses.action.push({ id: action.id });
    }

    const onDeactivationDate = actions.filter(
      (action) => action.executionDate === scenario.deactivationDateIso && isActiveRecord(action),
    );
    const contractActions = onDeactivationDate.filter(
      (action) => action.actionTypeId === CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
    );
    const activePodActions = onDeactivationDate.filter(
      (action) => action.actionTypeId === POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
    );
    const passed =
      flow.processStatus === 'COMPLETED' &&
      hardErrors.length === 0 &&
      warningPresent &&
      contractActions.length === 1 &&
      activePodActions.length === 0;

    TestRunSummary.registerPayload('supplyActionDeactivationMassImport', {
      processId: flow.processId,
      deactivationDateIso: scenario.deactivationDateIso,
      noticeReceivingDateIso: scenario.noticeReceivingDateIso,
      reportText: flow.reportText,
    });
    TestRunSummary.recordCheck({
      check: 'Both PODs are replaced by one contract termination without notice action',
      expectedResult:
        'Process COMPLETED. The report contains the additional-contract-action warning. ' +
        'One active contract termination without notice action exists on the import date. ' +
        'The import POD termination without notice action is no longer active.',
      actualResult: passed
        ? `As expected — process ${flow.processId}; contract actions=${contractActions.length}; active POD actions=${activePodActions.length}; ${summarizeActions(onDeactivationDate)}`
        : `Not as expected — process ${flow.processId} status=${flow.processStatus}; hardErrors=${hardErrors.map((row) => row.errors).join(' | ')}; warning=${warningPresent}; ${summarizeActions(actions)}`,
      passed,
    });

    expect(hardErrors.map((row) => row.errors).join(' | '), 'import row errors other than the replacement warning').toBe('');
    expect(warningPresent, 'process report warning').toBeTruthy();
    expect(contractActions, summarizeActions(actions)).toHaveLength(1);
    expect(activePodActions, summarizeActions(actions)).toHaveLength(0);
    expect(contractActions[0].noticeReceivingDate).toBe(scenario.noticeReceivingDateIso);

    await test.step('Attach test run summary', async () => {
      const frontendBase = resolveFrontendBaseUrlOverride();
      const extraLinks = buildProductContractTabLinks(scenario.contractId, frontendBase);
      const processUrl = buildProcessPreviewLink(flow.processId, frontendBase);
      if (processUrl) {
        extraLinks.process = [processUrl];
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_4182_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'pod', 'penalty', 'action'],
        extraLinks,
        snapshot: {
          contractId: scenario.contractId,
          processId: flow.processId,
          deactivationDateIso: scenario.deactivationDateIso,
        },
      });
    });
  });

  test(`[${PHN_4182_KEY}] TC-BE-2: ${'Change in actions for terminations and penalties process'} — uncovered POD blocks the contract action`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);

    const scenario = await test.step(
      'Precondition: one signed contract, two activated PODs, no pre-existing actions',
      async () =>
        createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints }),
    );

    TestRunSummary.registerPayload('customer', {
      id: scenario.customerId,
      identifier: scenario.customerIdentifier,
    });
    TestRunSummary.registerPayload('productContract', { id: scenario.contractId });
    TestRunSummary.registerPayload('pod', {
      imported: scenario.podIdentifiers[0],
      leftOut: scenario.podIdentifiers[1],
    });

    const flow = await test.step(
      'Upload Information of deactivation for only the first POD',
      async () =>
        runSupplyActionDeactivationImport(
          Request,
          FileUploadRequest,
          [
            {
              podIdentifier: scenario.podIdentifiers[0],
              noticeReceivingDateIso: scenario.noticeReceivingDateIso,
            },
          ],
          scenario.deactivationDateIso,
        ),
    );

    expect(flow.processStatus, 'mass import process status').toBe('COMPLETED');
    const warningPresent = flow.reportText.includes(CONTRACT_ACTION_REPORT_SNIPPET);

    const actions = await test.step('Load actions created for the customer', async () =>
      waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
        loaded.some(
          (action) =>
            action.executionDate === scenario.deactivationDateIso &&
            action.actionTypeId === POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID &&
            isActiveRecord(action),
        ),
      ),
    );
    for (const action of actions) {
      Responses.action.push({ id: action.id });
    }

    const onDeactivationDate = actions.filter(
      (action) => action.executionDate === scenario.deactivationDateIso && isActiveRecord(action),
    );
    const contractActions = onDeactivationDate.filter(
      (action) => action.actionTypeId === CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
    );
    const activePodActions = onDeactivationDate.filter(
      (action) => action.actionTypeId === POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
    );
    const passed =
      flow.processStatus === 'COMPLETED' &&
      !warningPresent &&
      contractActions.length === 0 &&
      activePodActions.length === 1;

    TestRunSummary.recordCheck({
      check: 'A suitable POD without a covering action blocks replacement',
      expectedResult:
        'The imported POD keeps an active POD termination without notice action. ' +
        'No contract termination without notice action is created. The replacement warning is absent.',
      actualResult: passed
        ? `As expected — active POD actions=${activePodActions.length}; contract actions=${contractActions.length}; warning=${warningPresent}`
        : `Not as expected — status=${flow.processStatus}; warning=${warningPresent}; report=${flow.reportText}; ${summarizeActions(actions)}`,
      passed,
    });

    expect(warningPresent, 'replacement warning must be absent').toBeFalsy();
    expect(contractActions, summarizeActions(actions)).toHaveLength(0);
    expect(activePodActions, summarizeActions(actions)).toHaveLength(1);

    await test.step('Attach test run summary', async () => {
      const frontendBase = resolveFrontendBaseUrlOverride();
      const extraLinks = buildProductContractTabLinks(scenario.contractId, frontendBase);
      const processUrl = buildProcessPreviewLink(flow.processId, frontendBase);
      if (processUrl) {
        extraLinks.process = [processUrl];
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_4182_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'pod', 'action'],
        extraLinks,
        snapshot: {
          contractId: scenario.contractId,
          processId: flow.processId,
          importedPod: scenario.podIdentifiers[0],
          leftOutPod: scenario.podIdentifiers[1],
        },
      });
    });
  });

  test(`[${PHN_4182_KEY}] TC-BE-3: ${PHN_4182_TITLE} — claimed import action blocks the contract action`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints });
    const flow = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [0, 1], scenario.noticeReceivingDateIso),
      scenario.noticeReceivingDateIso,
    );
    const actions = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      activeOnDate(loaded, scenario.noticeReceivingDateIso, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID).length > 0,
    );
    rememberActions(Responses, actions);
    const podActions = activeOnDate(actions, scenario.noticeReceivingDateIso, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const contractActions = activeOnDate(actions, scenario.noticeReceivingDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const claimed = podActions.some((action) => action.penaltyGenerated);
    const passed = flow.processStatus === 'COMPLETED' && claimed && contractActions.length === 0 && podActions.length >= 1;
    TestRunSummary.recordCheck({
      check: 'A claimed import action is not replaced',
      expectedResult: 'The POD action is claimed and stays active. No contract termination action is created.',
      actualResult: passed
        ? `As expected — claimed=${claimed}; ${summarizeActions(podActions)}`
        : `Not as expected — claimed=${claimed}; report=${flow.reportText}; ${summarizeActions(actions)}`,
      passed,
    });
    expect(claimed, summarizeActions(actions)).toBeTruthy();
    expect(contractActions, summarizeActions(actions)).toHaveLength(0);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-4: ${PHN_4182_TITLE} — user-created POD action stays`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints });
    const userActionId = await test.step('Precondition: user creates a with-notice action for POD 1', async () =>
      createManualAction(Request, {
        actionTypeId: POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID,
        customerId: scenario.customerId,
        contractId: scenario.contractId,
        executionDateIso: scenario.deactivationDateIso,
        noticeReceivingDateIso: scenario.noticeReceivingDateIso,
        penaltyId: scenario.podPenaltyId,
        podIds: [scenario.podIds[0]],
      }),
    );
    const flow = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [0, 1]),
      scenario.deactivationDateIso,
    );
    const actions = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      activeOnDate(loaded, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID).length === 1,
    );
    rememberActions(Responses, actions);
    const userAction = actions.find((action) => action.id === userActionId);
    const contractActions = activeOnDate(actions, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const activePodWithout = activeOnDate(actions, scenario.deactivationDateIso, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const passed =
      isActiveRecord(userAction ?? { status: 'MISSING' } as ActionDetailSnapshot) &&
      userAction?.actionTypeId === POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID &&
      contractActions.length === 1 &&
      activePodWithout.length === 0 &&
      flow.reportText.includes(CONTRACT_ACTION_REPORT_SNIPPET);
    TestRunSummary.recordCheck({
      check: 'User action stays and the system action on the import date is deleted',
      expectedResult: 'The with-notice user action stays active. One contract action is created. No active without-notice POD action remains on the import date.',
      actualResult: passed
        ? `As expected — user action ${userActionId} status=${userAction?.status}; contract actions=${contractActions.length}`
        : `Not as expected — user=${userActionId} ${summarizeActions(actions)}; report=${flow.reportText}`,
      passed,
    });
    expect(userAction?.status).toBe('ACTIVE');
    expect(contractActions).toHaveLength(1);
    expect(activePodWithout, summarizeActions(actions)).toHaveLength(0);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-5: ${PHN_4182_TITLE} — later execution date does not cover`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints });
    const laterDate = addDaysIso(scenario.deactivationDateIso, 10);
    const laterActionId = await createManualAction(Request, {
      actionTypeId: POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
      customerId: scenario.customerId,
      contractId: scenario.contractId,
      executionDateIso: laterDate,
      noticeReceivingDateIso: scenario.noticeReceivingDateIso,
      penaltyId: scenario.podPenaltyId,
      podIds: [scenario.podIds[1]],
    });
    const flow = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [0]),
      scenario.deactivationDateIso,
    );
    const actions = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      activeOnDate(loaded, scenario.deactivationDateIso, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID).length > 0,
    );
    rememberActions(Responses, actions);
    const later = actions.find((action) => action.id === laterActionId);
    const contractActions = activeOnDate(actions, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const importPodActions = activeOnDate(actions, scenario.deactivationDateIso, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const passed =
      later != null &&
      isActiveRecord(later) &&
      contractActions.length === 0 &&
      importPodActions.length === 1 &&
      !flow.reportText.includes(CONTRACT_ACTION_REPORT_SNIPPET);
    TestRunSummary.recordCheck({
      check: 'An action after the import date does not cover the POD left out of the file',
      expectedResult: 'No contract action. The later action stays active. The imported POD keeps its POD action.',
      actualResult: passed
        ? `As expected — later action ${laterActionId} stays; import POD actions=${importPodActions.length}`
        : `Not as expected — ${summarizeActions(actions)}; report=${flow.reportText}`,
      passed,
    });
    expect(contractActions, summarizeActions(actions)).toHaveLength(0);
    expect(later?.status ?? 'MISSING').not.toBe('DELETED');
    expect(importPodActions).toHaveLength(1);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-6: ${PHN_4182_TITLE} — real deactivation does not replace actions`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints });
    const flow = await runRealSupplyDeactivation(
      Request,
      FileUploadRequest,
      scenario.podIdentifiers,
      scenario.deactivationDateIso,
    );
    const actions = await loadOnce(Request, scenario.customerIdentifier);
    rememberActions(Responses, actions);
    const contractActions = activeOnDate(actions, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const passed = !flow.reportText.includes(CONTRACT_ACTION_REPORT_SNIPPET) && contractActions.length === 0;
    TestRunSummary.recordCheck({
      check: 'Real deactivation does not create the contract action from this story',
      expectedResult: 'No contract termination without notice action and no replacement warning.',
      actualResult: passed
        ? `As expected — process ${flow.processId} status=${flow.processStatus}; contract actions=0`
        : `Not as expected — status=${flow.processStatus}; report=${flow.reportText}; ${summarizeActions(actions)}`,
      passed,
    });
    expect(flow.reportText).not.toContain(CONTRACT_ACTION_REPORT_SNIPPET);
    expect(contractActions, summarizeActions(actions)).toHaveLength(0);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-7: ${PHN_4182_TITLE} — term end date equals the deactivation date`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario(
      { Request, GeneratePayload, Responses, Endpoints },
      { termEndDateIso: addDaysIso(new Date().toISOString().slice(0, 10), 14) },
    );
    const flow = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [0, 1]),
      scenario.deactivationDateIso,
    );
    const actions = await loadOnce(Request, scenario.customerIdentifier);
    rememberActions(Responses, actions);
    const contractActions = activeOnDate(
      actions,
      scenario.deactivationDateIso,
      CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
    );
    const passed =
      flow.reportText.includes('Contract term end date equals provided deactivation date') &&
      contractActions.length === 0;
    TestRunSummary.recordCheck({
      check: 'Term end equal to the deactivation date blocks action creation',
      expectedResult: 'The report says the contract term end date equals the deactivation date. No contract action is created.',
      actualResult: passed
        ? `As expected — ${flow.reportText}`
        : `Not as expected — report=${flow.reportText}; ${summarizeActions(actions)}`,
      passed,
    });
    expect(flow.reportText).toContain('Contract term end date equals provided deactivation date');
    expect(contractActions).toHaveLength(0);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-8: ${PHN_4182_TITLE} — contract termination action already exists`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints });
    const existingId = await createManualAction(Request, {
      actionTypeId: CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
      customerId: scenario.customerId,
      contractId: scenario.contractId,
      executionDateIso: scenario.deactivationDateIso,
      noticeReceivingDateIso: scenario.noticeReceivingDateIso,
      penaltyId: scenario.contractPenaltyId,
      podIds: [],
    });
    const flow = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [0, 1]),
      scenario.deactivationDateIso,
    );
    const actions = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      loaded.some((action) => action.id === existingId),
    );
    rememberActions(Responses, actions);
    const contractActions = activeOnDate(actions, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const passed =
      flow.reportText.includes('Action already exist for contract termination') &&
      contractActions.length === 1 &&
      contractActions[0].id === existingId;
    TestRunSummary.recordCheck({
      check: 'An existing contract termination action blocks the import replacement',
      expectedResult: 'The report says a contract termination action already exists. The pre-existing action stays and no second one is created.',
      actualResult: passed
        ? `As expected — existing action ${existingId} remains the only contract action`
        : `Not as expected — report=${flow.reportText}; ${summarizeActions(actions)}`,
      passed,
    });
    expect(flow.reportText).toContain('Action already exist for contract termination');
    expect(contractActions.map((action) => action.id)).toEqual([existingId]);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-9: ${PHN_4182_TITLE} — earlier system action is not deleted`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints });
    let earlierDate = addDaysIso(scenario.noticeReceivingDateIso, 9);
    if (earlierDate === scenario.termEndDateIso) {
      earlierDate = addDaysIso(scenario.noticeReceivingDateIso, 8);
    }
    const first = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [1]),
      earlierDate,
    );
    expect(first.processStatus).toBe('COMPLETED');
    const seeded = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      activeOnDate(loaded, earlierDate, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID).length === 1,
    );
    const earlierAction = activeOnDate(seeded, earlierDate, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID)[0];
    expect(earlierAction, summarizeActions(seeded)).toBeTruthy();

    const flow = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [0, 1]),
      scenario.deactivationDateIso,
    );
    const actions = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      activeOnDate(loaded, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID).length === 1,
    );
    rememberActions(Responses, actions);
    const earlierNow = actions.find((action) => action.id === earlierAction.id);
    const contractActions = activeOnDate(actions, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const sameDayPodActions = activeOnDate(actions, scenario.deactivationDateIso, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const passed =
      earlierNow != null &&
      isActiveRecord(earlierNow) &&
      contractActions.length === 1 &&
      sameDayPodActions.length === 0 &&
      flow.reportText.includes(CONTRACT_ACTION_REPORT_SNIPPET);
    TestRunSummary.recordCheck({
      check: 'An earlier system POD action stays while the same-day system action is deleted',
      expectedResult: 'The earlier system action stays active. One contract action is created. No active POD action remains on the later import date.',
      actualResult: passed
        ? `As expected — earlier action ${earlierAction.id} status=${earlierNow?.status}; contract actions=${contractActions.length}`
        : `Not as expected — ${summarizeActions(actions)}; report=${flow.reportText}`,
      passed,
    });
    expect(earlierNow?.status).toBe('ACTIVE');
    expect(contractActions).toHaveLength(1);
    expect(sameDayPodActions, summarizeActions(actions)).toHaveLength(0);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-10: ${PHN_4182_TITLE} — user actions stay and a contract action is created`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints });
    const userActionIds = await test.step(
      'Precondition: create one unclaimed POD termination without notice action per POD before supply deactivation',
      async () => {
        const ids: number[] = [];
        for (const podId of scenario.podIds) {
          ids.push(
            await createManualAction(Request, {
              actionTypeId: POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
              customerId: scenario.customerId,
              contractId: scenario.contractId,
              executionDateIso: scenario.deactivationDateIso,
              noticeReceivingDateIso: scenario.noticeReceivingDateIso,
              penaltyId: scenario.podPenaltyId,
              podIds: [podId],
            }),
          );
        }
        const created = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
          ids.every((id) =>
            loaded.some(
              (action) =>
                action.id === id &&
                isActiveRecord(action) &&
                action.actionTypeId === POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID &&
                action.executionDate === scenario.deactivationDateIso &&
                action.actionStatus === 'AWAITING' &&
                !action.penaltyGenerated &&
                action.podCount === 1,
            ),
          ),
        );
        expect(
          ids.filter((id) => created.some((action) => action.id === id)).length,
          `user actions must exist before supply deactivation (${summarizeActions(created)})`,
        ).toBe(scenario.podIds.length);
        return ids;
      },
    );
    const flow = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [0, 1]),
      scenario.deactivationDateIso,
    );
    const actions = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      activeOnDate(loaded, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID).length === 1 &&
      userActionIds.every((id) => loaded.some((action) => action.id === id && isActiveRecord(action))),
    );
    rememberActions(Responses, actions);
    const userActions = actions.filter((action) => userActionIds.includes(action.id));
    const contractActions = activeOnDate(actions, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const passed =
      userActions.length === scenario.podIds.length &&
      userActions.every((action) => isActiveRecord(action)) &&
      contractActions.length === 1 &&
      flow.reportText.includes(CONTRACT_ACTION_REPORT_SNIPPET);
    TestRunSummary.recordCheck({
      check: 'User actions stay and one contract action is created',
      expectedResult:
        'Each POD already has an Awaiting unclaimed without-notice action on the deactivation date. Those user actions stay active. One contract termination without notice action is created.',
      actualResult: passed
        ? `As expected — user actions ${userActionIds.join(', ')} stay; contract action ${contractActions[0].id} created.`
        : `Not as expected — report=${flow.reportText}; ${summarizeActions(actions)}`,
      passed,
    });
    expect(userActions.every((action) => action.status === 'ACTIVE')).toBeTruthy();
    expect(contractActions, summarizeActions(actions)).toHaveLength(1);
    expect(flow.reportText).toContain(CONTRACT_ACTION_REPORT_SNIPPET);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-11: ${PHN_4182_TITLE} — notice receiving date is required`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints });
    const flow = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [0, 1], null),
      scenario.deactivationDateIso,
    );
    const actions = await loadOnce(Request, scenario.customerIdentifier);
    rememberActions(Responses, actions);
    const contractActions = activeOnDate(actions, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const podActions = activeOnDate(actions, scenario.deactivationDateIso, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const passed =
      flow.reportText.includes('Notice receiving date is required') &&
      contractActions.length === 0 &&
      podActions.length === 0;
    TestRunSummary.recordCheck({
      check: 'A file row without a notice receiving date is rejected',
      expectedResult: 'Each row reports that the notice receiving date is required. No POD action and no contract action are created.',
      actualResult: passed
        ? `As expected — ${flow.reportText}`
        : `Not as expected — report=${flow.reportText}; ${summarizeActions(actions)}`,
      passed,
    });
    expect(flow.reportText).toContain('Notice receiving date is required');
    expect(contractActions).toHaveLength(0);
    expect(podActions).toHaveLength(0);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-12: ${PHN_4182_TITLE} — missing contract penalty still creates the contract action`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario(
      { Request, GeneratePayload, Responses, Endpoints },
      { includeContractPenalty: false },
    );
    const flow = await runSupplyActionDeactivationImport(
      Request,
      FileUploadRequest,
      fileRows(scenario, [0, 1]),
      scenario.deactivationDateIso,
    );
    const actions = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      activeOnDate(loaded, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID).length === 1,
    );
    rememberActions(Responses, actions);
    const contractActions = activeOnDate(actions, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const podActions = activeOnDate(actions, scenario.deactivationDateIso, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID);
    const passed =
      contractActions.length === 1 &&
      contractActions[0].withoutPenalty &&
      podActions.length === 0 &&
      flow.reportText.includes(CONTRACT_ACTION_REPORT_SNIPPET) &&
      flow.reportText.includes('No valid penalty is found for Action creation');
    TestRunSummary.recordCheck({
      check: 'The contract action is created without an automatic penalty',
      expectedResult: 'One contract action is created with without automatic penalty. The report contains the replacement warning and the missing-penalty sentence. The system POD action is not active.',
      actualResult: passed
        ? `As expected — action ${contractActions[0].id} withoutPenalty=${contractActions[0].withoutPenalty}`
        : `Not as expected — report=${flow.reportText}; ${summarizeActions(actions)}`,
      passed,
    });
    expect(contractActions).toHaveLength(1);
    expect(contractActions[0].withoutPenalty).toBeTruthy();
    expect(podActions, summarizeActions(actions)).toHaveLength(0);
    expect(flow.reportText).toContain('No valid penalty is found for Action creation');
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });

  test(`[${PHN_4182_KEY}] TC-BE-13: ${PHN_4182_TITLE} — other version POD does not block or get deleted`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);
    const scenario = await createTwoPodContractScenario({ Request, GeneratePayload, Responses, Endpoints });
    const firstImport = await test.step(
      'Precondition: system action for POD 1 only, while both PODs are on version 1',
      async () =>
        runSupplyActionDeactivationImport(
          Request,
          FileUploadRequest,
          fileRows(scenario, [0]),
          scenario.deactivationDateIso,
        ),
    );
    const seeded = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      activeOnDate(loaded, scenario.deactivationDateIso, POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID).length === 1,
    );
    const otherVersionAction = activeOnDate(
      seeded,
      scenario.deactivationDateIso,
      POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
    )[0];
    expect(otherVersionAction, summarizeActions(seeded)).toBeTruthy();
    expect(
      activeOnDate(seeded, scenario.deactivationDateIso, CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID),
      'version 1 with one uncovered POD must not create a contract action yet',
    ).toHaveLength(0);

    const nextVersion = await test.step(
      'Precondition: version 2 keeps only POD 2',
      async () => saveNextVersionWithOnePod(Request, scenario.contractId, scenario.podIds[1], scenario.deactivationDateIso),
    );
    expect(nextVersion.versionId).toBeGreaterThan(1);

    const flow = await test.step(
      'Upload Information of deactivation for POD 2 only',
      async () =>
        runSupplyActionDeactivationImport(
          Request,
          FileUploadRequest,
          fileRows(scenario, [1]),
          scenario.deactivationDateIso,
        ),
    );
    const actions = await waitForCustomerActions(Request, scenario.customerIdentifier, (loaded) =>
      loaded.some((action) => action.id === otherVersionAction.id),
    );
    rememberActions(Responses, actions);
    const kept = actions.find((action) => action.id === otherVersionAction.id);
    const contractActions = activeOnDate(
      actions,
      scenario.deactivationDateIso,
      CONTRACT_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
    );
    const passed =
      kept != null &&
      isActiveRecord(kept) &&
      contractActions.length === 1 &&
      flow.reportText.includes(CONTRACT_ACTION_REPORT_SNIPPET);
    TestRunSummary.recordCheck({
      check: 'A POD that exists only on the other version does not block and its action is not deleted',
      expectedResult:
        'Version 2 contains only POD 2. Importing POD 2 creates one contract action. The system POD action created for POD 1 stays active.',
      actualResult: passed
        ? `As expected — version ${nextVersion.versionId} start ${nextVersion.startDateIso}; POD 1 action ${otherVersionAction.id} status=${kept?.status}; contract action ${contractActions[0]?.id}.`
        : `Not as expected — first import=${firstImport.reportText}; second import=${flow.reportText}; ${summarizeActions(actions)}`,
      passed,
    });
    expect(kept?.status, summarizeActions(actions)).toBe('ACTIVE');
    expect(contractActions, `${flow.reportText} | ${summarizeActions(actions)}`).toHaveLength(1);
    await attachRun(TestRunSummary, Responses, scenario, flow);
  });
});

function fileRows(
  scenario: TwoPodContractScenario,
  podIndexes: number[],
  noticeReceivingDateIso: string | null = scenario.noticeReceivingDateIso,
): Array<{ podIdentifier: string; noticeReceivingDateIso: string | null }> {
  return podIndexes.map((index) => ({
    podIdentifier: scenario.podIdentifiers[index],
    noticeReceivingDateIso,
  }));
}

function rememberActions(
  Responses: { action: Array<{ id: number }> },
  actions: ActionDetailSnapshot[],
): void {
  for (const action of actions) {
    Responses.action.push({ id: action.id });
  }
}

async function loadOnce(
  Request: Parameters<typeof waitForCustomerActions>[0],
  customerIdentifier: string,
): Promise<ActionDetailSnapshot[]> {
  return waitForCustomerActions(Request, customerIdentifier, () => true, 1);
}

async function attachRun(
  TestRunSummary: {
    registerPayload: (key: string, value: unknown) => void;
    recordCheck: (check: unknown) => void;
  },
  Responses: Parameters<typeof finalizeTestRunSummary>[1],
  scenario: TwoPodContractScenario,
  flow: ImportFlowResult,
): Promise<void> {
  TestRunSummary.registerPayload('customer', { id: scenario.customerId, identifier: scenario.customerIdentifier });
  TestRunSummary.registerPayload('productContract', { id: scenario.contractId });
  const frontendBase = resolveFrontendBaseUrlOverride();
  const extraLinks = buildProductContractTabLinks(scenario.contractId, frontendBase);
  const processUrl = buildProcessPreviewLink(flow.processId, frontendBase);
  if (processUrl) {
    extraLinks.process = [processUrl];
  }
  finalizeTestRunSummary(TestRunSummary, Responses, {
    jiraKey: PHN_4182_KEY,
    relevantEntityKeys: ['customer', 'productContract', 'pod', 'action'],
    extraLinks,
    snapshot: { contractId: scenario.contractId, processId: flow.processId },
  });
}
