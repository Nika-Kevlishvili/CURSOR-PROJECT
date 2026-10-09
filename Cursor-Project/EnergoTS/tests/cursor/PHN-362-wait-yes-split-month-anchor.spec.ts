/**
 * PHN-362 — Wait=Yes split-month term-end anchor (standalone).
 *
 * - ANCHOR-TC-BE-1: First day — Confluence anchors on **initial term end**; Phoenix Dev2 uses **contract term end** (defect).
 * - ANCHOR-TC-BE-2: Exact day — Confluence: **same calendar day next month** from initial term end (parallel to First day, not +1 day);
 *   omit `supplyActivationValue` → engine must auto-derive (today: resign blocked on Dev2).
 * - ANCHOR-TC-DRAFT-NO-EXACT: Wait=No + EXACT_DATE — F DRAFT POST (Step 10.1 path, before sign).
 * - ANCHOR-TC-DRAFT-YES-EXACT: Wait=Yes + EXACT_DATE — F DRAFT POST (Step 10.2 path, before sign).
 *
 * Runtime-relative dates: initial term end = last day of current month; contract term end = last day of next month.
 *
 * Run on Dev2:
 *   $env:BASE_URL='https://devapps.energo-pro.bg/backend/phoenix-dev2'
 *   npx playwright test tests/cursor/PHN-362-wait-yes-split-month-anchor.spec.ts --project=main
 *
 * Ref: `Cursor-Project/test_cases/Backend/PHN-362_WaitYes_term_end_anchor.md`
 */

import { expect, test } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  ANCHOR_B_CONTRACT_TERM_END,
  ANCHOR_B_INITIAL_TERM_END,
  ANCHOR_B_SIGNING,
  ANCHOR_EXPECTED_CONFLUENCE_EXACT,
  ANCHOR_EXPECTED_CONFLUENCE_FIRST_DAY,
  ANCHOR_EXPECTED_PHOENIX_DEV2_EXACT,
  ANCHOR_EXPECTED_PHOENIX_DEV2_FIRST_DAY,
  ANCHOR_F_SIGNING,
  ANCHOR_F_VERSIONS,
  ANCHOR_POD_ACTIVATION,
  ANCHOR_RESIGNING_DEADLINE_DAYS,
  anchorExpectedWaitYesExactFromInitial,
  anchorSameDayNextMonth,
  runAnchorDraftNoFlowExactScenario,
  runAnchorDraftYesFlowExactScenario,
  runAnchorWaitYesSplitMonthExactScenario,
  runAnchorWaitYesSplitMonthScenario,
} from './phn-362-wait-yes-split-month-anchor.fixtures';
import type { AnchorExactDraftResult } from './phn-362-wait-yes-split-month-anchor.fixtures';

type AnchorRunResult = Awaited<ReturnType<typeof runAnchorWaitYesSplitMonthScenario>>;

function attachExactDraftSnapshot(tcId: string, result: AnchorExactDraftResult, Responses: Parameters<typeof reportGenerator.setLinksToResponses>[0]) {
  test.info().attach(`[PHN-362] ${tcId} — exact draft snapshot`, {
    body: JSON.stringify(
      {
        contractB: {
          id: result.contractBId,
          initialTermEnd: ANCHOR_B_INITIAL_TERM_END,
          contractTermEnd: ANCHOR_B_CONTRACT_TERM_END,
          signingDate: ANCHOR_B_SIGNING,
          podActivation: ANCHOR_POD_ACTIVATION,
        },
        contractF: {
          id: result.contractFId,
          status: 'DRAFT',
          thirdTab: result.thirdTab,
        },
        podIdentifier: result.podIdentifier,
        trace: reportGenerator.setLinksToResponses(Responses),
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });
}

function attachSplitMonthAnchorReport(
  tcId: string,
  result: AnchorRunResult,
  Responses: Parameters<typeof reportGenerator.setLinksToResponses>[0],
  extra: Record<string, unknown>,
) {
  test.info().attach(`[PHN-362] ${tcId} — split-month anchor dates`, {
    body: JSON.stringify(
      {
        contractB: {
          initialTermEnd: ANCHOR_B_INITIAL_TERM_END,
          contractTermEnd: ANCHOR_B_CONTRACT_TERM_END,
          signingDate: ANCHOR_B_SIGNING,
          podActivation: ANCHOR_POD_ACTIVATION,
          resigningDeadlineDays: ANCHOR_RESIGNING_DEADLINE_DAYS,
        },
        contractF: {
          signingDate: ANCHOR_F_SIGNING,
          versions: ANCHOR_F_VERSIONS,
          waitForOldContractTermToExpire: 'YES',
          ...extra,
        },
        actualRunExpected: result.expected,
        knownPhoenixDev2: result.phoenixDev2Hint,
        resignBlocked: 'resignBlocked' in result ? result.resignBlocked : false,
        resignStatus: 'resignStatus' in result ? result.resignStatus : undefined,
        contractIds: { B: result.contractBId, F: result.contractFId },
        podIdentifier: result.podIdentifier,
        trace: reportGenerator.setLinksToResponses(Responses),
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });
}

test.describe('PHN-362 - Wait Yes split-month anchor', { tag: ['@contractsAndOrders', '@dev2'] }, () => {
  test.describe.configure({ mode: 'serial' });

  test('[PHN-362] ANCHOR-TC-BE-1 — Wait=Yes | First day | initial vs contract term end different months', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(25 * 60 * 1000);
    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
    test.info().annotations.push({ type: 'tc', description: 'ANCHOR-TC-BE-1' });
    test.info().annotations.push({
      type: 'defect',
      description:
        'Asserts known Phoenix Dev2 dates (contract term end anchor); Confluence expected attached — flip assertExpected when SQL fixed',
    });

    const result = await runAnchorWaitYesSplitMonthScenario(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      {
        assertExpected: ANCHOR_EXPECTED_PHOENIX_DEV2_FIRST_DAY,
        phoenixDev2Hint: ANCHOR_EXPECTED_CONFLUENCE_FIRST_DAY,
      },
    );

    attachSplitMonthAnchorReport('ANCHOR-TC-BE-1', result, Responses, {
      supplyActivation: 'FIRST_DAY_OF_MONTH',
      supplyActivationValueSent: null,
      intendedFormula: 'firstDayOfMonthAfter(B.initialTermEnd)',
      expectedConfluence: ANCHOR_EXPECTED_CONFLUENCE_FIRST_DAY,
      knownPhoenixDev2: ANCHOR_EXPECTED_PHOENIX_DEV2_FIRST_DAY,
      defectNote: 'Pass asserts Phoenix; Confluence dates in expectedConfluence until initial_term_date fix',
    });
  });

  test('[PHN-362] ANCHOR-TC-DRAFT-NO-EXACT — Wait=No | EXACT_DATE | F DRAFT (split-month B)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(25 * 60 * 1000);
    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
    test.info().annotations.push({ type: 'tc', description: 'ANCHOR-TC-DRAFT-NO-EXACT' });
    test.info().annotations.push({
      type: 'precondition',
      description: 'Step 10.1 No-flow snapshot: B signed, F POST DRAFT with EXACT_DATE, no sign/re-sign',
    });

    const result = await runAnchorDraftNoFlowExactScenario(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    );

    attachExactDraftSnapshot('ANCHOR-TC-DRAFT-NO-EXACT', result, Responses);
  });

  test('[PHN-362] ANCHOR-TC-DRAFT-YES-EXACT — Wait=Yes | EXACT_DATE | F DRAFT (split-month B)', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(25 * 60 * 1000);
    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
    test.info().annotations.push({ type: 'tc', description: 'ANCHOR-TC-DRAFT-YES-EXACT' });
    test.info().annotations.push({
      type: 'precondition',
      description: 'Step 10.2 Yes-flow snapshot: B signed, F POST DRAFT with EXACT_DATE, no sign/re-sign',
    });

    const result = await runAnchorDraftYesFlowExactScenario(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
    );

    attachExactDraftSnapshot('ANCHOR-TC-DRAFT-YES-EXACT', result, Responses);
  });

  test('[PHN-362] ANCHOR-TC-BE-2 — Wait=Yes | Exact day | same day next month | omit supplyActivationValue', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
  }) => {
    test.setTimeout(25 * 60 * 1000);
    test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
    test.info().annotations.push({ type: 'tc', description: 'ANCHOR-TC-BE-2' });
    test.info().annotations.push({
      type: 'defect',
      description:
        'Asserts known Phoenix Dev2 dates when resign succeeds; Confluence expected attached — flip assertExpected when engine auto-derives',
    });

    await test.step('Precondition: expected dates from Confluence formula', async () => {
      const fromFormula = anchorExpectedWaitYesExactFromInitial(ANCHOR_B_INITIAL_TERM_END);
      expect(fromFormula.fActivation).toBe(
        anchorSameDayNextMonth(ANCHOR_B_INITIAL_TERM_END),
      );
      expect(fromFormula).toEqual(ANCHOR_EXPECTED_CONFLUENCE_EXACT);
      expect(fromFormula.fActivation).not.toBe(ANCHOR_EXPECTED_CONFLUENCE_FIRST_DAY.fActivation);
    });

    const result = await runAnchorWaitYesSplitMonthExactScenario(
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      FileUploadRequest,
      {
        assertExpected: ANCHOR_EXPECTED_PHOENIX_DEV2_EXACT,
        phoenixDev2Hint: ANCHOR_EXPECTED_CONFLUENCE_EXACT,
      },
    );

    attachSplitMonthAnchorReport('ANCHOR-TC-BE-2', result, Responses, {
      supplyActivation: 'EXACT_DATE',
      supplyActivationValueSent: null,
      intendedFormula: 'anchorSameDayNextMonth(B.initialTermEnd); B.deactivation = F.activation - 1 day',
      anchorInitialTermEnd: ANCHOR_B_INITIAL_TERM_END,
      expectedConfluence: ANCHOR_EXPECTED_CONFLUENCE_EXACT,
      knownPhoenixDev2: ANCHOR_EXPECTED_PHOENIX_DEV2_EXACT,
      defectNote:
        'Pass asserts Phoenix Dev2 when resign succeeds; Confluence in expectedConfluence until auto-derive fix',
      gapNote:
        'resignBlocked=true → Phoenix did not auto-derive (pass until engine implements Step 10.2 exact branch)',
    });

    if ('resignBlocked' in result && result.resignBlocked) {
      test.info().annotations.push({
        type: 'gap-verified',
        description: `Resign blocked HTTP ${result.resignStatus} — engine requires manual supplyActivationValue`,
      });
    } else {
      test.info().annotations.push({
        type: 'fix-verified',
        description: 'Resign succeeded — Phoenix Dev2 exact-day dates asserted',
      });
    }
  });
});
