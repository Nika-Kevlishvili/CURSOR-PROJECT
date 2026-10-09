/**
 * Contract Resign New Flow — prep-only data (Excel Backend scenarios).
 *
 * Like PHN-362 TC-BE-39-Prep: creates contract B (+ F draft) without signing F.
 * Attachments contain IDs, dates, and sign hints for manual runs or contract-resign-new-flow.spec.ts.
 *
 * Source: Contract_resign_new_flow.xlsx (Backend sheet)
 * Runtime assertions: tests/cursor/contract-resign-new-flow.spec.ts
 *
 * Run all:  npx playwright test contract-resign-new-flow-precondition.spec.ts --project=dev2
 * Run one:  npx playwright test -g "Prep Standard"
 */

import { test } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import type { FixtureCtx } from './contract-resign-new-flow.fixtures';
import {
  POST_SIGN_PREP_CASES,
  attachResignPrepManifest,
  prepAc4ConflictDraft,
  prepBoundaryTermEndDraft,
  prepDifferentCustomerDraft,
  prepIncompatibleResignDraft,
  prepMissingDeadlineDraft,
  prepNoBaseContractDraft,
  prepNoEligiblePodDraft,
  prepNonResignProductDraft,
  prepPerpetuitySkipDraft,
  prepStandardResignDraft,
  prepTwoPodsDraft,
  runPrepStep,
} from './contract-resign-new-flow-precondition.fixtures';

const TAG = '[CONTRACT-RESIGN-PREP]';

test.describe(`${TAG} Excel Backend preconditions`, { tag: ['@contract-resign', '@dev2', '@prep'] }, () => {
  test(`${TAG} Prep Standard — B ACTIVE_IN_TERM + F DRAFT (Cases 2,5–7,11–14,17–19,24,28–31,33)`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('standard B + F draft (happy / date variants)', () =>
      prepStandardResignDraft(c),
    );
    manifest.extra = { ...(manifest.extra ?? {}), trace: reportGenerator.setLinksToResponses(Responses) };
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Boundary — signing date == B term end (Excel row 9)`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('boundary term end == signing date', () => prepBoundaryTermEndDraft(c));
    manifest.extra = { ...(manifest.extra ?? {}), excelRow: 9, trace: reportGenerator.setLinksToResponses(Responses) };
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Case 1 — non-resign product F draft`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('Case 1 non-resign product', () => prepNonResignProductDraft(c));
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Case 3 — no active predecessor B`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(15 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('Case 3 no base B', () => prepNoBaseContractDraft(c));
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Case 4 — incompatible resign product`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('Case 4 incompatible product', () => prepIncompatibleResignDraft(c));
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Case 8 — no activated POD on B`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('Case 8 no POD activation on B', () => prepNoEligiblePodDraft(c));
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Case 15 — perpetuity before signing`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('Case 15 perpetuity skip', () => prepPerpetuitySkipDraft(c));
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Case 16 — missing resigning deadline on B term`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('Case 16 no deadline', () => prepMissingDeadlineDraft(c));
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Case 26 — two PODs on B and F`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(25 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('Case 26 two PODs', () => prepTwoPodsDraft(c));
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Cases 17–19 — AC4 future activation conflict`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(25 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('Cases 17–19 AC4 setup', () => prepAc4ConflictDraft(c));
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep Case 32 — F draft different customer`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }, testInfo) => {
    test.setTimeout(20 * 60 * 1000);
    const c = { Request, GeneratePayload, Responses, Endpoints } satisfies FixtureCtx;
    const manifest = await runPrepStep('Case 32 different customer', () => prepDifferentCustomerDraft(c));
    await attachResignPrepManifest(testInfo, manifest);
  });

  test(`${TAG} Prep index — cases needing post-sign state (no B+F-only prep)`, async ({}, testInfo) => {
    await attachResignPrepManifest(testInfo, {
      profile: 'post-sign-required-index',
      excelSource: 'Contract_resign_new_flow.xlsx',
      caseNumbers: [...POST_SIGN_PREP_CASES],
      runtimeToday: new Date().toISOString().slice(0, 10),
      contractB: { id: 0, termEndDate: '', expectedStatus: 'N/A' },
      products: { baseProductId: 0, productIndices: [] },
      pod: { podId: 0, podDetailId: 0 },
      signHints: {
        signingDateInWindow: '',
        signingDateBeforeWindow: '',
        signingDateAfterTermEnd: '',
        signingDateAtEarlyDeadline: '',
        signingDateAtTermEnd: '',
        waitYesFirstDayOfNextMonth: '',
        exactWaitYesAfterTermEnd: '',
      },
      nextStep: `Run contract-resign-new-flow.spec.ts for cases ${POST_SIGN_PREP_CASES.join(', ')} (require prior successful re-sign or multi-hop lifecycle).`,
      notes: 'Cases 20–25, 27–28 partial flows also live in contract-resign-new-flow.spec.ts with inline preconditions.',
    });
  });
});
