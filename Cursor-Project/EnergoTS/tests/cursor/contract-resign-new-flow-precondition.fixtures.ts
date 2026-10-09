/**
 * Contract Resign New Flow — prep-only fixtures (Excel Backend scenarios).
 *
 * Creates contract B (+ optional F draft) without signing F.
 * Reference: Contract_resign_new_flow.xlsx, PHN-362-resign-precondition-two-pods.spec.ts
 * Runtime tests: contract-resign-new-flow.spec.ts
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { TestInfo } from '@playwright/test';
import {
  EXACT_WAIT_YES_AFTER_TERM_END,
  SIGNING_DATE_AFTER_TERM_END,
  SIGNING_DATE_AT_EARLY_DEADLINE,
  SIGNING_DATE_BEFORE_WINDOW,
  SIGNING_DATE_IN_WINDOW,
  TERM_END_DATE,
  WAIT_YES_FIRST_DAY_OF_NEXT_MONTH,
  createAndSignBaseContractB,
  createAndSignBaseContractPerpetuity,
  createAndSignBaseContractTwoPods,
  createDraftContractF,
  createDraftContractDifferentCustomer,
  entityId,
  getContractPods,
  runStandardResignChain,
  setupAc4ResignChain,
  sharedBaseProduct,
  sharedCustomer,
  sharedNonResignProduct,
  sharedPod,
  sharedPrice,
  sharedResignProduct,
  sharedTermBase,
  sharedTermResign,
  todayIso,
  type FixtureCtx,
} from './contract-resign-new-flow.fixtures';
import { loadProductContract } from './pdt-2815-version-validity.fixtures';

/** Excel boundary — signing date equals old contract term end (inclusive per QA table). */
export const SIGNING_DATE_AT_TERM_END = TERM_END_DATE;

export type ResignPrepSignHints = {
  signingDateInWindow: string;
  signingDateBeforeWindow: string;
  signingDateAfterTermEnd: string;
  signingDateAtEarlyDeadline: string;
  signingDateAtTermEnd: string;
  waitYesFirstDayOfNextMonth: string;
  exactWaitYesAfterTermEnd: string;
};

export type ResignPrepManifest = {
  profile: string;
  excelSource: string;
  caseNumbers: number[];
  runtimeToday: string;
  contractB: {
    id: number;
    termEndDate: string;
    expectedStatus: string;
    availableForResigning?: boolean;
  };
  contractF?: {
    id: number;
    status: 'DRAFT';
    productIndex: number;
    waitExpireDefault: 'YES' | 'NO';
    supplyActivationDefault: string;
  };
  products: {
    baseProductId: number;
    resignProductId?: number;
    productIndices: number[];
  };
  pod: {
    podId: number;
    podDetailId: number;
    identifier?: string;
    podIndices?: number[];
  };
  signHints: ResignPrepSignHints;
  nextStep: string;
  notes?: string;
  extra?: Record<string, unknown>;
};

export function defaultSignHints(): ResignPrepSignHints {
  return {
    signingDateInWindow: SIGNING_DATE_IN_WINDOW,
    signingDateBeforeWindow: SIGNING_DATE_BEFORE_WINDOW,
    signingDateAfterTermEnd: SIGNING_DATE_AFTER_TERM_END,
    signingDateAtEarlyDeadline: SIGNING_DATE_AT_EARLY_DEADLINE,
    signingDateAtTermEnd: SIGNING_DATE_AT_TERM_END,
    waitYesFirstDayOfNextMonth: WAIT_YES_FIRST_DAY_OF_NEXT_MONTH,
    exactWaitYesAfterTermEnd: EXACT_WAIT_YES_AFTER_TERM_END,
  };
}

export async function attachResignPrepManifest(info: TestInfo, manifest: ResignPrepManifest): Promise<void> {
  await info.attach(`[PREP] ${manifest.profile} — Excel cases ${manifest.caseNumbers.join(',')}`, {
    body: JSON.stringify(manifest, null, 2),
    contentType: 'application/json',
  });
}

async function podMetaFromContract(
  ctx: FixtureCtx,
  contractId: number,
  podIndex = 0,
): Promise<{ podId: number; podDetailId: number; identifier?: string }> {
  const pods = await getContractPods(ctx.Request, contractId);
  const podId = Number(pods[0]?.podId ?? entityId(ctx.Responses.pod[podIndex]));
  const podDetailId = Number(pods[0]?.podDetailId);
  const podGet = await ctx.Request.get(`pod/${entityId(ctx.Responses.pod[podIndex])}`);
  await expect(podGet).CheckResponse();
  const podJson = await podGet.json();
  return {
    podId,
    podDetailId,
    identifier: String(podJson.identifier ?? pods[0]?.identifier ?? ''),
  };
}

export async function assertBaseBReady(
  ctx: FixtureCtx,
  baseContractId: number,
  opts?: { expectAvailableForResigning?: boolean; termEndDate?: string },
): Promise<void> {
  const body = await loadProductContract(ctx.Request, baseContractId);
  const bp = body.basicParameters as {
    status?: string;
    contractTermEndDate?: string;
  };
  expect(['ACTIVE_IN_TERM', 'ACTIVE_IN_PERPETUITY']).toContain(bp.status);
  if (opts?.termEndDate) {
    expect(bp.contractTermEndDate).toBe(opts.termEndDate);
  }
  const available = (body as { availableForResigning?: boolean }).availableForResigning;
  if (opts?.expectAvailableForResigning !== undefined && available !== undefined) {
    expect(available).toBe(opts.expectAvailableForResigning);
  }
}

export async function assertFDraftReady(
  ctx: FixtureCtx,
  contractFId: number,
  podIdentifier: string,
): Promise<void> {
  const body = await loadProductContract(ctx.Request, contractFId);
  const bp = body.basicParameters as { status?: string; subStatus?: string; signingDate?: string | null };
  expect(bp.status).toBe('DRAFT');
  expect(bp.subStatus).toBe('DRAFT');
  expect(bp.signingDate == null || bp.signingDate === '').toBeTruthy();
  const fPods = (body.contractPodsResponses ?? []) as { identifier?: string }[];
  expect(fPods.some((p) => p.identifier === podIdentifier)).toBeTruthy();
}

/** Standard B (ACTIVE_IN_TERM) + F draft — Excel Cases 2,5,6,7,11–14,17–19,24,28,29,30,31,33. */
export async function prepStandardResignDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  const chain = await runStandardResignChain(ctx);
  const pod = await podMetaFromContract(ctx, chain.newContractId);
  const bBody = await loadProductContract(ctx.Request, chain.baseContractId);
  const termEnd = String(
    (bBody.basicParameters as { contractTermEndDate?: string }).contractTermEndDate ?? TERM_END_DATE,
  );
  await assertBaseBReady(ctx, chain.baseContractId, { termEndDate: termEnd, expectAvailableForResigning: true });
  await assertFDraftReady(ctx, chain.newContractId, pod.identifier ?? '');

  return {
    profile: 'standard-b-plus-f-draft',
    excelSource: 'Contract_resign_new_flow.xlsx — Backend sheet',
    caseNumbers: [2, 5, 6, 7, 11, 12, 13, 14, 17, 18, 19, 24, 28, 29, 30, 31, 33],
    runtimeToday: todayIso(),
    contractB: {
      id: chain.baseContractId,
      termEndDate: termEnd,
      expectedStatus: 'ACTIVE_IN_TERM',
      availableForResigning: (bBody as { availableForResigning?: boolean }).availableForResigning,
    },
    contractF: {
      id: chain.newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: {
      baseProductId: chain.baseProductId,
      resignProductId: chain.resignProductId,
      productIndices: [0, 1],
    },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep:
      'Sign F via contract-resign-new-flow.spec.ts or manual PUT — use signHints.signingDateInWindow (happy), signingDateBeforeWindow (Case 5), signingDateAfterTermEnd (Case 6), signingDateAtEarlyDeadline (Case 7).',
  };
}

/** Boundary — sign date == B term end (Excel boundary row). B term end aligned to TERM_END_DATE. */
export async function prepBoundaryTermEndDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  await sharedCustomer(ctx);
  await sharedPrice(ctx);
  await sharedTermBase(ctx);
  await sharedTermResign(ctx);
  await sharedPod(ctx);
  const baseProductId = await sharedBaseProduct(ctx, 0);
  const baseContractId = await createAndSignBaseContractB(ctx, { termEndDate: TERM_END_DATE });
  const resignProductId = await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
  const newContractId = await createDraftContractF(ctx, { productIndex: 1 });
  const pod = await podMetaFromContract(ctx, newContractId);
  await assertBaseBReady(ctx, baseContractId, { termEndDate: TERM_END_DATE });
  await assertFDraftReady(ctx, newContractId, pod.identifier ?? '');

  return {
    profile: 'boundary-sign-equals-term-end',
    excelSource: 'Contract_resign_new_flow.xlsx — boundary signing date == term end (Excel row 9)',
    caseNumbers: [],
    runtimeToday: todayIso(),
    contractB: {
      id: baseContractId,
      termEndDate: TERM_END_DATE,
      expectedStatus: 'ACTIVE_IN_TERM',
    },
    contractF: {
      id: newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'YES',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: { baseProductId, resignProductId, productIndices: [0, 1] },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep: `Sign F with signingDate = signHints.signingDateAtTermEnd (${SIGNING_DATE_AT_TERM_END}). QA expects inclusive boundary (200); backend may reject strict '<' — document actual vs expected.`,
    notes: 'Backend SQL uses signingDate < term_end (strict). Spec expects inclusive equality.',
  };
}

/** Case 1 — non-resign product on F draft. */
export async function prepNonResignProductDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  await sharedCustomer(ctx);
  await sharedPrice(ctx);
  await sharedTermBase(ctx);
  await sharedTermResign(ctx);
  await sharedPod(ctx);
  const baseProductId = await sharedBaseProduct(ctx, 0);
  const baseContractId = await createAndSignBaseContractB(ctx);
  await sharedNonResignProduct(ctx, 1);
  const newContractId = await createDraftContractF(ctx, { productIndex: 1 });
  const pod = await podMetaFromContract(ctx, newContractId);

  return {
    profile: 'non-resign-product-f-draft',
    excelSource: 'Contract_resign_new_flow.xlsx — Case 1',
    caseNumbers: [1],
    runtimeToday: todayIso(),
    contractB: { id: baseContractId, termEndDate: TERM_END_DATE, expectedStatus: 'ACTIVE_IN_TERM' },
    contractF: {
      id: newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: { baseProductId, productIndices: [0, 1] },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep: 'Sign F — expect informational "Product is not for re-signing" (Case 1).',
  };
}

/** Case 3 — resign product F draft, no signed predecessor B. */
export async function prepNoBaseContractDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  await sharedCustomer(ctx);
  await sharedPrice(ctx);
  await sharedTermBase(ctx);
  await sharedTermResign(ctx);
  await sharedPod(ctx);
  const baseProductId = await sharedBaseProduct(ctx, 0);
  await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
  const newContractId = await createDraftContractF(ctx, { productIndex: 1 });
  const pod = await podMetaFromContract(ctx, newContractId);

  return {
    profile: 'no-active-predecessor-b',
    excelSource: 'Contract_resign_new_flow.xlsx — Case 3',
    caseNumbers: [3],
    runtimeToday: todayIso(),
    contractB: { id: 0, termEndDate: '', expectedStatus: 'N/A' },
    contractF: {
      id: newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: { baseProductId, productIndices: [0, 1] },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep: 'Sign F — expect only-for-re-signing error (no active B).',
  };
}

/** Case 4 — incompatible resign target. */
export async function prepIncompatibleResignDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  await sharedCustomer(ctx);
  await sharedPrice(ctx);
  await sharedTermBase(ctx);
  await sharedTermResign(ctx);
  await sharedPod(ctx);
  const baseA = await sharedBaseProduct(ctx, 0);
  const baseContractId = await createAndSignBaseContractB(ctx, { productIndex: 0 });
  const baseB = await sharedBaseProduct(ctx, 0);
  await sharedResignProduct(ctx, baseB, { termIndex: 1, linkTarget: true });
  const newContractId = await createDraftContractF(ctx, { productIndex: 2 });
  const pod = await podMetaFromContract(ctx, newContractId);

  return {
    profile: 'incompatible-resign-product',
    excelSource: 'Contract_resign_new_flow.xlsx — Case 4',
    caseNumbers: [4],
    runtimeToday: todayIso(),
    contractB: { id: baseContractId, termEndDate: TERM_END_DATE, expectedStatus: 'ACTIVE_IN_TERM' },
    contractF: {
      id: newContractId,
      status: 'DRAFT',
      productIndex: 2,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: { baseProductId: baseA, productIndices: [0, 1, 2] },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep: 'Sign F with productIndex 2 — expect product incompatibility.',
    notes: 'B uses product[0]; resign product links to product[1], F draft uses product[2].',
  };
}

/** Case 8 — B signed but POD not activated. */
export async function prepNoEligiblePodDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  await sharedCustomer(ctx);
  await sharedPrice(ctx);
  await sharedTermBase(ctx);
  await sharedTermResign(ctx);
  await sharedPod(ctx);
  const baseProductId = await sharedBaseProduct(ctx, 0);
  const baseContractId = await createAndSignBaseContractB(ctx, { activatePod: false });
  await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
  const newContractId = await createDraftContractF(ctx, { productIndex: 1 });
  const pod = await podMetaFromContract(ctx, newContractId);

  return {
    profile: 'no-activated-pod-on-b',
    excelSource: 'Contract_resign_new_flow.xlsx — Case 8',
    caseNumbers: [8],
    runtimeToday: todayIso(),
    contractB: { id: baseContractId, termEndDate: TERM_END_DATE, expectedStatus: 'ACTIVE_IN_TERM' },
    contractF: {
      id: newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: { baseProductId, productIndices: [0, 1] },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep: 'Sign F — expect no eligible POD / resign failed.',
    notes: 'POD on B exists but activationDate is null.',
  };
}

/** Case 16 — happy-path data; old contract B without term / initial-term dates. */
export async function prepMissingDeadlineDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  const {
    setupExcelCase16ResignChain,
  } = await import('./contract-resign-new-flow.fixtures');
  const chain = await setupExcelCase16ResignChain(ctx);
  const baseContractId = chain.baseContractId;
  const newContractId = chain.newContractId;
  const pod = await podMetaFromContract(ctx, newContractId);

  return {
    profile: 'old-contract-without-term-dates',
    excelSource: 'Contract_resign_new_flow.xlsx — Case 16',
    caseNumbers: [16],
    runtimeToday: todayIso(),
    contractB: { id: baseContractId, termEndDate: null, expectedStatus: 'ACTIVE_IN_TERM' },
    contractF: {
      id: newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: { baseProductId: chain.baseProductId, productIndices: [0, 1] },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep: 'Sign F — expect resigning period cannot be calculated / failed for all pods.',
  };
}

/** Case 15 — perpetuity before signing window. */
export async function prepPerpetuitySkipDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  await sharedCustomer(ctx);
  await sharedPrice(ctx);
  await sharedTermBase(ctx);
  await sharedTermResign(ctx);
  await sharedPod(ctx);
  const baseProductId = await sharedBaseProduct(ctx, 0);
  const baseContractId = await createAndSignBaseContractPerpetuity(ctx, {
    perpetuityDate: '2025-01-01',
    signingDate: SIGNING_DATE_IN_WINDOW,
  });
  await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
  const newContractId = await createDraftContractF(ctx, { productIndex: 1 });
  const pod = await podMetaFromContract(ctx, newContractId);

  return {
    profile: 'perpetuity-before-signing-skips-b',
    excelSource: 'Contract_resign_new_flow.xlsx — Case 15',
    caseNumbers: [15],
    runtimeToday: todayIso(),
    contractB: { id: baseContractId, termEndDate: TERM_END_DATE, expectedStatus: 'ACTIVE_IN_PERPETUITY' },
    contractF: {
      id: newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: { baseProductId, productIndices: [0, 1] },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep: 'Sign F — B skipped (perpetuity date before signing).',
  };
}

/** Case 26 — two PODs on B and F draft. */
export async function prepTwoPodsDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  const baseContractId = await createAndSignBaseContractTwoPods(ctx);
  const baseProductId = entityId(ctx.Responses.product[0]);
  await sharedTermResign(ctx);
  await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
  const newContractId = await createDraftContractF(ctx, { productIndex: 1, podIndices: [0, 1] });
  const pods = await getContractPods(ctx.Request, newContractId);
  const pod0 = entityId(ctx.Responses.pod[0]);
  const pod1 = entityId(ctx.Responses.pod[1]);

  return {
    profile: 'two-pods-b-and-f-draft',
    excelSource: 'Contract_resign_new_flow.xlsx — Case 26',
    caseNumbers: [26],
    runtimeToday: todayIso(),
    contractB: { id: baseContractId, termEndDate: TERM_END_DATE, expectedStatus: 'ACTIVE_IN_TERM' },
    contractF: {
      id: newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: { baseProductId, productIndices: [0, 1] },
    pod: {
      podId: pod0,
      podDetailId: Number(pods[0]?.podDetailId),
      podIndices: [0, 1],
    },
    signHints: defaultSignHints(),
    nextStep: 'Sign F — both PODs should transfer atomically (Case 26).',
    extra: { secondPodId: pod1 },
  };
}

/** Cases 17–19 — AC4 future activation on interference contract (prep stops before sign). */
export async function prepAc4ConflictDraft(ctx: FixtureCtx): Promise<ResignPrepManifest & { interferenceContractId: number }> {
  const chain = await setupAc4ResignChain(ctx);
  const pod = await podMetaFromContract(ctx, chain.newContractId);

  return {
    profile: 'ac4-future-activation-conflict',
    excelSource: 'Contract_resign_new_flow.xlsx — Cases 17–19',
    caseNumbers: [17, 18, 19],
    runtimeToday: todayIso(),
    contractB: { id: chain.baseContractId, termEndDate: TERM_END_DATE, expectedStatus: 'ACTIVE_IN_TERM' },
    contractF: {
      id: chain.newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: {
      baseProductId: chain.baseProductId,
      resignProductId: chain.resignProductId,
      productIndices: [0, 1],
    },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep:
      'Case 17: sign without removeFuturePods. Case 18: sign with removeFuturePods=true. Case 19: cancel confirmation.',
    extra: { interferenceContractId: chain.interferenceContractId },
    interferenceContractId: chain.interferenceContractId,
  };
}

/** Case 32 — F draft for different customer. */
export async function prepDifferentCustomerDraft(ctx: FixtureCtx): Promise<ResignPrepManifest> {
  await sharedCustomer(ctx);
  await sharedPrice(ctx);
  await sharedTermBase(ctx);
  await sharedTermResign(ctx);
  await sharedPod(ctx);
  const baseProductId = await sharedBaseProduct(ctx, 0);
  const baseContractId = await createAndSignBaseContractB(ctx);
  await sharedResignProduct(ctx, baseProductId, { termIndex: 1 });
  const newContractId = await createDraftContractDifferentCustomer(ctx);
  const pod = await podMetaFromContract(ctx, newContractId, 0);

  return {
    profile: 'different-customer-f-draft',
    excelSource: 'Contract_resign_new_flow.xlsx — Case 32',
    caseNumbers: [32],
    runtimeToday: todayIso(),
    contractB: { id: baseContractId, termEndDate: TERM_END_DATE, expectedStatus: 'ACTIVE_IN_TERM' },
    contractF: {
      id: newContractId,
      status: 'DRAFT',
      productIndex: 1,
      waitExpireDefault: 'NO',
      supplyActivationDefault: 'FIRST_DAY_OF_MONTH',
    },
    products: { baseProductId, productIndices: [0, 1] },
    pod: { ...pod, podIndices: [0] },
    signHints: defaultSignHints(),
    nextStep: 'Sign F — re-sign across different customers should fail.',
    notes: 'F uses customer index 1; B uses customer index 0.',
  };
}

/** Cases 9,10,21,27 — require prior successful sign; not pure B+F draft prep. */
export const POST_SIGN_PREP_CASES = [9, 10, 21, 27] as const;

export async function runPrepStep<T>(label: string, fn: () => Promise<T>): Promise<T> {
  return test.step(`Precondition: ${label}`, fn);
}
