/**
 * PHN-362 resign scenarios 07–13 — thin wrappers over contract-resign-new-flow helpers.
 * Case 14: phn-362-resign-case-14-bugfix.fixtures.ts
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import {
  MSG_RESIGNING_FAILED_ALL_PODS,
  MSG_RESIGNING_FAILED_ALL_PODS_FULL,
  MSG_VERSION_NOT_EXIST,
  SIGNING_DATE_IN_WINDOW,
  addDaysIso,
  assertBothPodsResignedOnOld,
  assertPodNotResignedOnOldContract,
  assertPodResignedOnOldContract,
  buildEditProductContractPayload,
  createAndSignBaseContractB,
  createAndSignBaseContractTwoPods,
  createDraftContractF,
  createPastSupplyGapThenReactivateOnBase,
  entityId,
  finalizeResignTest,
  getContractPods,
  refreshResignFlowDates,
  runStandardResignChain,
  runSuccessfulResign,
  setupAc4ResignChain,
  sharedBaseProduct,
  sharedCustomer,
  sharedPod,
  sharedPrice,
  sharedResignProduct,
  sharedSecondPod,
  sharedTermBase,
  sharedTermResign,
  signContractExpectError,
  signContractF,
  type FixtureCtx,
} from './contract-resign-new-flow.fixtures';

export type Phn362ResignCtx = FixtureCtx;

function ctx(
  Request: baseFixture['Request'],
  GeneratePayload: baseFixture['GeneratePayload'],
  Responses: baseFixture['Responses'],
  Endpoints: baseFixture['Endpoints'],
): Phn362ResignCtx {
  return { Request, GeneratePayload, Responses, Endpoints };
}

/** Case 08 / TC-BE-44 — AC4 cancel, no confirmRemoveFutureActivation. */
export async function runPhn362Case08Ac4Cancel(c: Phn362ResignCtx) {
  refreshResignFlowDates();
  const chain = await setupAc4ResignChain(c);
  await signContractExpectError(
    c,
    chain.newContractId,
    { signingDate: SIGNING_DATE_IN_WINDOW, removeFuturePods: false, productIndex: 1 },
    [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL],
  );
  await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
  await finalizeResignTest(c, { label: 'PHN-362 Case 08', chain, expectResignLinks: false });
}

/** Case 09 / TC-BE-48 — past gap only on B; resign succeeds. */
export async function runPhn362Case09PastGapSuccess(c: Phn362ResignCtx) {
  refreshResignFlowDates();
  await sharedCustomer(c);
  await sharedPrice(c);
  await sharedTermBase(c);
  await sharedTermResign(c);
  await sharedPod(c);
  const baseId = await sharedBaseProduct(c, 0);
  const baseContractId = await createAndSignBaseContractB(c);
  const contractIndex = c.Responses.productContract.length - 1;
  await createPastSupplyGapThenReactivateOnBase(c, contractIndex, 0);
  await sharedResignProduct(c, baseId, { termIndex: 1 });
  const newContractId = await createDraftContractF(c, { productIndex: 1 });
  const podId = entityId(c.Responses.pod[0]);
  const body = await signContractF(c, newContractId, {
    signingDate: SIGNING_DATE_IN_WINDOW,
    waitExpire: 'NO',
    supplyActivation: 'FIRST_DAY_OF_MONTH',
    productIndex: 1,
  });
  expect(body.id).toBeTruthy();
  await assertPodResignedOnOldContract(c.Request, baseContractId, podId);
  await finalizeResignTest(c, {
    label: 'PHN-362 Case 09',
    oldContractId: baseContractId,
    newContractId,
    expectResignLinks: true,
  });
}

/** Case 10 / TC-BE-50 — POD not activatable in new contract versions. */
export async function runPhn362Case10PodNotInNewVersion(c: Phn362ResignCtx) {
  refreshResignFlowDates();
  const chain = await runStandardResignChain(c);
  await test.step('Precondition: future F version — POD not activatable at signing', async () => {
    const generated = (await c.GeneratePayload.contractsAndOrders.product_contract(0, 1)) as Record<
      string,
      unknown
    >;
    const editPayload = await buildEditProductContractPayload(c.Request, chain.newContractId, generated, {
      forDraftV1: false,
      preserveSigningDate: false,
    });
    editPayload.savingAsNewVersion = true;
    editPayload.startDate = addDaysIso(SIGNING_DATE_IN_WINDOW, 45);
    const put = await c.Request.put(
      `${c.Endpoints.productContract}/${chain.newContractId}?versionId=1&changeFutureVersionsPods=true`,
      { data: editPayload },
    );
    await expect(put).CheckResponse();
  });
  await signContractExpectError(
    c,
    chain.newContractId,
    { signingDate: SIGNING_DATE_IN_WINDOW, productIndex: 1 },
    [MSG_RESIGNING_FAILED_ALL_PODS, MSG_RESIGNING_FAILED_ALL_PODS_FULL, MSG_VERSION_NOT_EXIST],
  );
  await assertPodNotResignedOnOldContract(c.Request, chain.baseContractId, chain.podId);
  await finalizeResignTest(c, { label: 'PHN-362 Case 10', chain, expectResignLinks: false });
}

/** Case 11 / TC-BE-51 — activation spans multiple new contract versions. */
export async function runPhn362Case11StraddleVersions(c: Phn362ResignCtx) {
  refreshResignFlowDates();
  const chain = await runStandardResignChain(c);
  const generated = (await c.GeneratePayload.contractsAndOrders.product_contract(0, 1)) as Record<string, unknown>;
  const editPayload = await buildEditProductContractPayload(c.Request, chain.newContractId, generated, {
    forDraftV1: false,
    preserveSigningDate: false,
  });
  editPayload.savingAsNewVersion = true;
  editPayload.startDate = addDaysIso(SIGNING_DATE_IN_WINDOW, -5);
  const put = await c.Request.put(
    `${c.Endpoints.productContract}/${chain.newContractId}?versionId=1&changeFutureVersionsPods=true`,
    { data: editPayload },
  );
  await expect(put).CheckResponse();
  const body = await signContractF(c, chain.newContractId, {
    signingDate: SIGNING_DATE_IN_WINDOW,
    waitExpire: 'NO',
    productIndex: 1,
  });
  expect(body.id).toBeTruthy();
  const getRes = await c.Request.get(`product-contract/${chain.newContractId}`);
  await expect(getRes).CheckResponse();
  const versions = ((await getRes.json()) as { versions?: { versionId?: number }[] }).versions ?? [];
  expect(versions.length).toBeGreaterThanOrEqual(2);
  await finalizeResignTest(c, { label: 'PHN-362 Case 11', chain, expectResignLinks: true });
}

/** Case 12 / TC-BE-42 — open-ended old POD (no deactivation on B before resign). */
export async function runPhn362Case12OpenEndedOldPod(c: Phn362ResignCtx) {
  refreshResignFlowDates();
  const chain = await runStandardResignChain(c);
  await runSuccessfulResign(c, chain);
  const newPods = await getContractPods(c.Request, chain.newContractId);
  const newRow = newPods.find((p) => Number(p.podId) === chain.podId);
  expect(newRow?.activationDate).toBeTruthy();
  expect(newRow?.deactivationDate == null || newRow?.deactivationDate === '').toBeTruthy();
  const oldPods = await getContractPods(c.Request, chain.baseContractId);
  const oldRow = oldPods.find((p) => Number(p.podId) === chain.podId);
  expect(oldRow?.deactivationDate).toBe(addDaysIso(String(newRow?.activationDate), -1));
  await finalizeResignTest(c, { label: 'PHN-362 Case 12', chain, expectResignLinks: true });
}

/** Case 13 / TC-BE-57 — two PODs re-signed atomically. */
export async function runPhn362Case13TwoPods(c: Phn362ResignCtx) {
  refreshResignFlowDates();
  await sharedCustomer(c);
  await sharedPrice(c);
  await sharedTermBase(c);
  await sharedTermResign(c);
  await sharedPod(c);
  await sharedSecondPod(c);
  const baseId = await sharedBaseProduct(c, 0);
  await createAndSignBaseContractTwoPods(c);
  await sharedResignProduct(c, baseId, { termIndex: 1 });
  const newId = await createDraftContractF(c, { productIndex: 1, podIndices: [0, 1] });
  const baseContractId = c.Responses.productContract[0] as number;
  const podIds = [entityId(c.Responses.pod[0]), entityId(c.Responses.pod[1])];
  const body = await signContractF(c, newId, {
    signingDate: SIGNING_DATE_IN_WINDOW,
    productIndex: 1,
  });
  expect(body.id).toBeTruthy();
  await assertBothPodsResignedOnOld(c.Request, baseContractId, podIds);
  await finalizeResignTest(c, {
    label: 'PHN-362 Case 13',
    oldContractId: baseContractId,
    newContractId: newId,
    expectResignLinks: true,
  });
}

export { ctx as phn362ResignCtx };
