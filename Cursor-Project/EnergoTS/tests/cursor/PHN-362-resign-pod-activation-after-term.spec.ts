/**
 * PHN-362 / Re-signing TC-BE-21 — old POD activation after new contract term anchor.
 *
 * Scenario (Contract_re_signing_changes.md Step 10.0):
 * - B: initial term end < POD activation on old contract.
 * - B POD activation > F third-tab term anchor (`supplyActivationValue` / EXACT_DATE on new contract).
 * - F: WITHOUT_SUPPLY, Wait=Yes, EXACT_DATE (matches user JSON pattern).
 * - Sign F → re-sign blocked; no resigned links; B POD not Re-signed.
 *
 * Run on Dev2:
 *   $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix-dev2'
 *   npx playwright test tests/cursor/PHN-362-resign-pod-activation-after-term.spec.ts --project=main
 *
 * Ref: `Cursor-Project/test_cases/Backend/Contract_re_signing_changes.md` TC-BE-21
 */

import { test } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  TC21_B_CONTRACT_TERM_END,
  TC21_B_INITIAL_TERM_END,
  TC21_B_POD_ACTIVATION,
  TC21_F_SIGNING,
  TC21_F_TERM_ANCHOR,
  TC21_RUNTIME_TODAY,
  runTc21PodActivationAfterTermScenario,
} from './phn-362-resign-pod-activation-after-term.fixtures';

test.describe('PHN-362 - Re-sign TC-BE-21 POD after term anchor', { tag: ['@contractsAndOrders', '@dev2'] }, () => {
  test('[PHN-362] TC-BE-21 — Old POD activation after new contract term anchor | re-sign blocked', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(25 * 60 * 1000);
    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
    test.info().annotations.push({ type: 'tc', description: 'TC-BE-21' });
    test.info().annotations.push({
      type: 'rule',
      description:
        'Step 10 filter: initial term end < old POD activation; old POD activation > F EXACT_DATE term anchor',
    });

    const result = await runTc21PodActivationAfterTermScenario(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    );

    test.info().attach('[PHN-362] TC-BE-21 — POD activation after term anchor', {
      body: JSON.stringify(
        {
          runtimeToday: TC21_RUNTIME_TODAY,
          contractB: {
            id: result.contractBId,
            initialTermEnd: TC21_B_INITIAL_TERM_END,
            podActivation: TC21_B_POD_ACTIVATION,
            contractTermEndDate: TC21_B_CONTRACT_TERM_END,
            note: 'POD activation > F term anchor; initial term end < POD activation',
          },
          contractF: {
            id: result.contractFId,
            signingDate: TC21_F_SIGNING,
            supplyActivation: 'EXACT_DATE',
            supplyActivationValue: TC21_F_TERM_ANCHOR,
            waitForOldContractTermToExpire: 'YES',
            contractType: 'WITHOUT_SUPPLY',
          },
          podIdentifier: result.podIdentifier,
          expected: 'Re-sign blocked — no resigned links; B POD not Re-signed',
          trace: reportGenerator.setLinksToResponses(Responses),
        },
        null,
        2,
      ),
      contentType: 'application/json',
    });
  });
});
