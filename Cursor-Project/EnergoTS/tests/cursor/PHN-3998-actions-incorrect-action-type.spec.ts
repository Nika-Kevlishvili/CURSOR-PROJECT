/**
 * PHN-3998 — Information-of-deactivation mass import must create POD-termination
 * actions with type «ПРЕКРАТЯВАНЕ НА ТО БЕЗ ПРЕДИЗВЕСТИЕ» (Dev2 nomenclature id 4)
 * for every contract in the file, including a contract whose product also has a
 * with-notice penalty (id 3). Clone of PDT-2337 / PDT-3299.
 *
 * Jira: CLONE - Actions - Incorrect Action Type
 *
 * Phoenix (origin/dev2): ActionSupplyActivationService.onComplete
 *   findPenaltyIdsForProductWithContractDetailId(..., without-notice only)
 *   createActionRequestForPods.setActionTypeId(podTerminationWithoutNoticeId)
 *
 * Swagger (dev2):
 * - POST /mass-import/{domainType}/files/upload domainType=SUPPLY_ACTION_DEACTIVATIONS
 * - GET  /actions/list ActionListRequest.searchBy=CUSTOMER_IDENTIFIER|POD_IDENTIFIER
 * - GET  /actions/{id} ActionResponse.actionTypeId
 * - POST /penalties PenaltyRequest.actionTypeList, penaltyApplicability=POD
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2177-supply-action-deactivation-restricted-period.spec.ts
 * - tests/cursor/pdt-2177-supply-action-deactivation-restricted-period.fixtures.ts
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts
 *
 * Target env: Dev2
 *   BASE_URL typically https://devapps.energo-pro.bg/backend/phoenix2-dev
 *
 * Swagger refresh note: update-swagger-specs.ps1 failed on macOS (curl.exe missing);
 * Dev2 spec refreshed via /usr/bin/curl from environments.json openapi_json.
 */
import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProcessPreviewLink,
  buildProductContractTabLinks,
  resolveFrontendBaseUrlOverride,
} from './shared/manual-verification-links.fixtures';
import {
  PHN_3998_KEY,
  PHN_3998_TITLE,
  POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
  POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID,
  assertAllActionsWithoutNoticeType,
  buildActionPreviewPath,
  createTwoContractDeactivationScenario,
  runMultiPodSupplyActionDeactivationMassImport,
  waitAndLoadCreatedActions,
} from './phn-3998-actions-incorrect-action-type.fixtures';

test.describe(`[${PHN_3998_KEY}]: ${PHN_3998_TITLE}`, {
  tag: ['@massImport', '@supplyActivation', '@action', '@phn-3998', '@dev2'],
}, () => {
  test(`[${PHN_3998_KEY}]: ${PHN_3998_TITLE}`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(30 * 60 * 1000);

    const scenario = await test.step(
      'Precondition: two signed product contracts, mixed POD-termination penalties, activated PODs',
      async () =>
        createTwoContractDeactivationScenario({
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        }),
    );

    expect(scenario.podIdentifiers.length).toBe(2);
    expect(scenario.activationDateIso <= scenario.deactivationDateIso).toBeTruthy();

    TestRunSummary.registerPayload('customer', {
      id: scenario.customerId,
      identifier: scenario.customerIdentifier,
    });
    TestRunSummary.registerPayload('productContract', {
      contractA: scenario.contractIds[0],
      contractB: scenario.contractIds[1],
    });
    TestRunSummary.registerPayload('penalty', {
      withoutNoticePenaltyIdA: scenario.withoutNoticePenaltyIdA,
      withoutNoticePenaltyIdB: scenario.withoutNoticePenaltyIdB,
      withNoticePenaltyId: scenario.withNoticePenaltyId,
      withoutNoticeActionTypeId: POD_TERMINATION_WITHOUT_NOTICE_ACTION_TYPE_ID,
      withNoticeActionTypeId: POD_TERMINATION_WITH_NOTICE_ACTION_TYPE_ID,
    });
    TestRunSummary.registerPayload('pod', {
      identifiers: scenario.podIdentifiers,
      activationDateIso: scenario.activationDateIso,
      deactivationDateIso: scenario.deactivationDateIso,
    });

    const flow = await test.step(
      'Upload SUPPLY_ACTION_DEACTIVATIONS mass import (Information of deactivation) for both PODs',
      async () =>
        runMultiPodSupplyActionDeactivationMassImport(
          Request,
          FileUploadRequest,
          scenario.podIdentifiers,
          scenario.deactivationDateIso,
        ),
    );

    expect(flow.processId, 'supply action deactivation process id').toBeGreaterThan(0);
    expect(flow.processStatus, 'mass import process status').toBe('COMPLETED');

    TestRunSummary.registerPayload('supplyActionDeactivationMassImport', {
      domainType: 'SUPPLY_ACTION_DEACTIVATIONS',
      podIdentifiers: scenario.podIdentifiers,
      deactivationDateIso: scenario.deactivationDateIso,
      processId: flow.processId,
      uploadStatus: flow.uploadStatus,
      errorReportSummary: flow.errorReportSummary,
      failedRowCount: flow.failedRowCount,
    });

    TestRunSummary.recordCheck({
      check: 'Information of deactivation mass import completes without row errors',
      expectedResult:
        'Process COMPLETED and MASS_IMPORT_ERROR_REPORT has zero failed rows for both POD identifiers.',
      actualResult:
        flow.failedRowCount === 0 && flow.processStatus === 'COMPLETED'
          ? `As expected — process ${flow.processId} COMPLETED, failed rows=${flow.failedRowCount}.`
          : `Not as expected — process ${flow.processId} status=${flow.processStatus}, failed rows=${flow.failedRowCount} (${flow.errorReportSummary}).`,
      passed: flow.failedRowCount === 0 && flow.processStatus === 'COMPLETED',
    });

    const actions = await test.step(
      'Assert: both contracts get action type POD termination without notice (id 4)',
      async () => {
        const created = await waitAndLoadCreatedActions(
          Request,
          scenario.customerIdentifier,
          scenario.podIdentifiers,
          scenario.deactivationDateIso,
          2,
        );
        for (const action of created) {
          Responses.action.push({ id: action.id });
        }
        const assertion = assertAllActionsWithoutNoticeType(
          created,
          scenario.contractIds,
          {
            [scenario.contractIds[0]]: scenario.withoutNoticePenaltyIdA,
            [scenario.contractIds[1]]: scenario.withoutNoticePenaltyIdB,
          },
          scenario.withNoticePenaltyId,
        );
        TestRunSummary.recordCheck({
          check: 'Action type is without notice on both contracts',
          expectedResult: assertion.expectedResult,
          actualResult: assertion.passed
            ? `As expected — ${assertion.actualResult}`
            : `Not as expected — ${assertion.actualResult}`,
          passed: assertion.passed,
        });
        return created;
      },
    );

    await test.step('Attach test run summary', async () => {
      const frontendBase = resolveFrontendBaseUrlOverride();
      const extraLinks: Record<string, string[]> = {};
      const processUrl = buildProcessPreviewLink(flow.processId, frontendBase);
      if (processUrl) {
        extraLinks.process = [processUrl];
      }
      Object.assign(
        extraLinks,
        (() => {
          const a = buildProductContractTabLinks(scenario.contractIds[0], frontendBase);
          const b = buildProductContractTabLinks(scenario.contractIds[1], frontendBase);
          const urls = [...(a.productContract ?? []), ...(b.productContract ?? [])];
          return urls.length ? { productContract: urls } : {};
        })(),
      );
      if (frontendBase) {
        const root = frontendBase.endsWith('/') ? frontendBase : `${frontendBase}/`;
        extraLinks.action = actions.map((a) => `${root}${buildActionPreviewPath(a.id)}`);
      }

      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_3998_KEY,
        relevantEntityKeys: ['customer', 'pod', 'product', 'productContract', 'penalty', 'action'],
        extraLinks: Object.keys(extraLinks).length ? extraLinks : undefined,
        snapshot: {
          testCase: PHN_3998_TITLE,
          customerId: scenario.customerId,
          customerIdentifier: scenario.customerIdentifier,
          contractIds: scenario.contractIds,
          podIdentifiers: scenario.podIdentifiers,
          processId: flow.processId,
          activationDateIso: scenario.activationDateIso,
          deactivationDateIso: scenario.deactivationDateIso,
          withoutNoticePenaltyIdA: scenario.withoutNoticePenaltyIdA,
          withoutNoticePenaltyIdB: scenario.withoutNoticePenaltyIdB,
          withNoticePenaltyId: scenario.withNoticePenaltyId,
          actionIds: actions.map((a) => a.id),
          actionTypeIds: actions.map((a) => a.actionTypeId),
        },
      });
    });
  });
});
