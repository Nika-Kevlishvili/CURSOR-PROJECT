/**
 * PHN-362 case 14 — bug regression: B POD May–Nov 2026; F multi-version; per-version POD sub-ranges.
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { pathToFileURL } from 'url';
import { envVariables } from '../../fixtures/envCashed';
import {
  addDaysIso,
  assertPodResignedOnOldContract,
  finalizeResignTest,
  signContractF,
} from './contract-resign-new-flow.fixtures';
import {
  buildEditProductContractPayload,
  loadProductContract,
  putProductContractNewVersion,
  versionDetailId,
} from './pdt-2815-version-validity.fixtures';
import {
  IDX_PRODUCT_RS,
  IDX_PRODUCT_STD,
  PREP_EMPLOYEE_ID,
  runMassImportOldContractEntityPreconditions,
} from './phn-362-resign-two-pods-precondition.fixtures';

type Case14Ctx = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'FileUploadRequest'
>;

type VersionAnchor = { start: string; end: string | null };
type PodExpectation = { activation: string; deactivation: string | null };

type Case14Anchors = {
  bPodActivation: string;
  bPodDeactivation: string;
  bPodDeactivationAfterResign?: string;
  fHeaderActivationAfterResign?: string;
  fSigning: string;
  bSigning: string;
  bVersionStart: string;
  bTermEnd: string;
  versions: {
    v1: VersionAnchor;
    v2: VersionAnchor;
    v4: VersionAnchor;
    v3: VersionAnchor;
  };
  expectedPodByVersionId: Record<string, PodExpectation>;
};

export async function loadCase14Anchors(runtimeToday?: string): Promise<Case14Anchors> {
  const mod = await import(
    pathToFileURL(
      'c:/Users/k.nanuashvili/Desktop/tasks/mass import/phn-362/playwright-data/case-14-bugfix-anchors.mjs',
    ).href
  );
  const today = runtimeToday ?? new Date().toISOString().slice(0, 10);
  return mod.buildCase14Anchors(today) as Case14Anchors;
}

async function resolvePodDetailId(
  Request: baseFixture['Request'],
  Responses: baseFixture['Responses'],
  podIndex: number,
): Promise<number> {
  const pod = Responses.pod[podIndex] as { id?: number; podDetailId?: number };
  const podGet = await Request.get(`pod/${pod.id}?version=1`);
  await expect(podGet).CheckResponse();
  const body = await podGet.json();
  return Number(body.lastPodDetailId ?? body.podDetailId);
}

/** Dev2 SIGNED PUT — entryInForceDate must be in the future (same pattern as createContractBPrep). */
function buildCase14SignPutBody(
  signPayload: Record<string, unknown>,
  signingDate: string,
  termEnd: string,
  initialTermValue: string,
): Record<string, unknown> {
  const runtimeToday = new Date().toISOString().slice(0, 10);
  const putBody = JSON.parse(JSON.stringify(signPayload)) as Record<string, unknown>;
  const bp = putBody.basicParameters as Record<string, unknown>;
  bp.status = 'SIGNED';
  bp.subStatus = 'SIGNED_BY_BOTH_SIDES';
  bp.versionStatus = 'SIGNED';
  bp.signingDate = signingDate;
  bp.contractTermEndDate = termEnd;
  bp.entryInForceDate = addDaysIso(runtimeToday, 1);
  delete bp.startOfInitialTerm;
  delete bp.statusModifyDate;
  const pp = putBody.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.contractTermEndDate = termEnd;
  pp.supplyActivation = 'MANUAL';
  delete pp.supplyActivationValue;
  pp.entryIntoForce = 'SIGNING';
  delete pp.entryIntoForceValue;
  pp.startOfContractInitialTerm = 'EXACT_DATE';
  pp.startOfContractValue = initialTermValue;
  const ap = putBody.additionalParameters as Record<string, unknown> | undefined;
  if (ap) {
    ap.employeeId = PREP_EMPLOYEE_ID;
    ap.riskAssessment = 'PERMIT';
  }
  return putBody;
}

export async function createContractBCase14(ctx: Case14Ctx, anchors: Case14Anchors) {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_STD,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.contractTermEndDate = anchors.bTermEnd;
  bp.startOfInitialTerm = null;
  bp.entryInForceDate = null;
  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.contractTermEndDate = anchors.bTermEnd;
  pp.supplyActivation = 'MANUAL';
  pp.supplyActivationValue = null;
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.startOfContractValue = null;

  const podDetailId = await resolvePodDetailId(Request, Responses, 0);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const contractId = (await createRes.json()).id as number;
  Responses.productContract.push(contractId);

  const signPayload = await buildEditProductContractPayload(Request, contractId, contractPayload, {
    startDate: anchors.bVersionStart,
    signingDate: anchors.bSigning,
  });
  const initialTermValue = addDaysIso(anchors.bTermEnd, -6);
  const put = await Request.put(
    `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
    {
      data: buildCase14SignPutBody(signPayload, anchors.bSigning, anchors.bTermEnd, initialTermValue),
    },
  );
  await expect(put).CheckResponse();

  const bBody = await loadProductContract(Request, contractId);
  const contractDetailId = versionDetailId(bBody, 1);
  const podGet = await Request.get(`pod/${Responses.pod[0].id}`);
  const podJson = await podGet.json();

  const activation = await Request.post('/contract-pods/manual', {
    data: {
      identifier: podJson.identifier,
      podDetailId,
      contractDetailId,
      activationDate: anchors.bPodActivation,
      deactivationDate: anchors.bPodDeactivation,
      deactivationPurposeId: envVariables.deactivation_reason,
    },
  });
  await expect(activation).CheckResponse();

  return {
    contractId,
    contractPayload,
    podId: Number(Responses.pod[0].id),
    podIdentifier: String(podJson.identifier),
  };
}

async function createContractFDraftCase14(ctx: Case14Ctx) {
  const { Request, GeneratePayload, Responses, Endpoints } = ctx;
  const podDetailId = await resolvePodDetailId(Request, Responses, 0);
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_RS,
  )) as Record<string, unknown>;
  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.entryInForceDate = null;
  bp.startOfInitialTerm = null;
  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'WITHOUT_SUPPLY';
  pp.supplyActivation = 'FIRST_DAY_OF_MONTH';
  pp.supplyActivationValue = null;
  pp.entryIntoForce = 'MANUAL';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'MANUAL';
  pp.startOfContractValue = null;
  pp.productContractWaitForOldContractTermToExpires = 'YES';
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const contractId = (await createRes.json()).id as number;
  Responses.productContract.push(contractId);
  return { contractId, contractPayload };
}

/** POD must exist on every logical version of contract F (multi-version re-sign precondition). */
export async function assertPodPresentOnAllFVersions(
  Request: baseFixture['Request'],
  contractFId: number,
  podId: number,
  minVersions = 4,
): Promise<void> {
  const fBody = await loadProductContract(Request, contractFId);
  const versions = (fBody.versions ?? []) as { versionId?: number; startDate?: string }[];
  expect(versions.length, 'F version count').toBeGreaterThanOrEqual(minVersions);

  for (const v of versions) {
    const versionId = Number(v.versionId);
    const res = await Request.get(`product-contract/${contractFId}/version/${versionId}?language=BG`);
    await expect(res).CheckResponse();
    const body = (await res.json()) as {
      contractPodsResponses?: { podId?: number }[];
    };
    const pods = body.contractPodsResponses ?? [];
    const row = pods.find((p) => Number(p.podId) === podId);
    expect(
      row,
      `POD ${podId} must exist on F version ${versionId} (start ${String(v.startDate).slice(0, 10)}); got ${pods.length} pod row(s)`,
    ).toBeTruthy();
  }
}

export async function assertPodOnVersionByStart(
  Request: baseFixture['Request'],
  contractId: number,
  versionId: number,
  podId: number,
  expected: PodExpectation,
): Promise<void> {
  const res = await Request.get(`product-contract/${contractId}/version/${versionId}?language=BG`);
  await expect(res).CheckResponse();
  const body = (await res.json()) as {
    contractPodsResponses?: { podId?: number; activationDate?: string; deactivationDate?: string | null }[];
  };
  const row = (body.contractPodsResponses ?? []).find((p) => Number(p.podId) === podId);
  expect(row, `POD on F version ${versionId}`).toBeTruthy();
  expect(String(row?.activationDate).slice(0, 10)).toBe(expected.activation);
  if (expected.deactivation == null) {
    expect(row?.deactivationDate == null || row?.deactivationDate === '').toBeTruthy();
  } else {
    expect(String(row?.deactivationDate).slice(0, 10)).toBe(expected.deactivation);
  }
}

export async function runPhn362Case14BugfixResign(ctx: Case14Ctx): Promise<void> {
  const anchors = await loadCase14Anchors();
  const versionOrder: VersionAnchor[] = [
    anchors.versions.v1,
    anchors.versions.v2,
    anchors.versions.v4,
    anchors.versions.v3,
  ];

  await runMassImportOldContractEntityPreconditions(
    ctx.Request,
    ctx.GeneratePayload,
    ctx.Responses,
    ctx.Endpoints,
    ctx.FileUploadRequest,
  );

  const b = await test.step('Case 14: contract B POD 01.05–30.11.2026', async () =>
    createContractBCase14(ctx, anchors),
  );
  const f = await test.step('Case 14: draft F (RS)', async () => createContractFDraftCase14(ctx));

  await test.step('Case 14: reshape F v1 then add versions (1→2→4→3) with POD on each', async () => {
    const podOpts = { changeFutureVersionsPods: true as const };
    await putProductContractNewVersion(ctx.Request, ctx.Endpoints, f.contractId, f.contractPayload, 1, {
      startDate: anchors.versions.v1.start,
      savingAsNewVersion: false,
      versionStatus: 'DRAFT',
      ...podOpts,
    });
    let fromVersion = 1;
    for (let i = 1; i < versionOrder.length; i++) {
      const body = await loadProductContract(ctx.Request, f.contractId);
      const versions = (body.versions ?? []) as { versionId?: number }[];
      fromVersion = Math.max(...versions.map((v) => Number(v.versionId ?? 0)));
      await putProductContractNewVersion(ctx.Request, ctx.Endpoints, f.contractId, f.contractPayload, fromVersion, {
        startDate: versionOrder[i].start,
        savingAsNewVersion: true,
        versionStatus: 'DRAFT',
        ...podOpts,
      });
    }
  });

  await test.step('Assert B POD 01.05–30.11 before re-sign', async () => {
    const bBody = await loadProductContract(ctx.Request, b.contractId);
    const bPod = (
      (bBody.contractPodsResponses ?? []) as {
        podId?: number;
        activationDate?: string;
        deactivationDate?: string;
      }[]
    ).find((p) => Number(p.podId) === b.podId);
    expect(String(bPod?.activationDate).slice(0, 10)).toBe(anchors.bPodActivation);
    expect(String(bPod?.deactivationDate).slice(0, 10)).toBe(anchors.bPodDeactivation);
  });

  await test.step('Case 14: POD present on every F version (before sign)', async () => {
    await assertPodPresentOnAllFVersions(ctx.Request, f.contractId, b.podId, 4);
  });

  await test.step(`Case 14: sign F (${anchors.fSigning})`, async () => {
    const body = await signContractF(
      { ...ctx },
      f.contractId,
      {
        signingDate: anchors.fSigning,
        waitExpire: 'YES',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
        productIndex: 1,
      },
    );
    expect(body.id).toBeTruthy();
  });

  const fBody = await loadProductContract(ctx.Request, f.contractId);
  const versions = (fBody.versions ?? []) as { versionId?: number; startDate?: string }[];

  const expectedByStart: Record<string, PodExpectation> = {
    [anchors.versions.v1.start]: anchors.expectedPodByVersionId[1],
    [anchors.versions.v2.start]: anchors.expectedPodByVersionId[2],
    [anchors.versions.v4.start]: anchors.expectedPodByVersionId[4],
    [anchors.versions.v3.start]: anchors.expectedPodByVersionId[3],
  };

  await test.step('Assert POD resigned on B after F sign', async () => {
    await assertPodResignedOnOldContract(ctx.Request, b.contractId, b.podId);
    const bBody = await loadProductContract(ctx.Request, b.contractId);
    const bPod = (
      (bBody.contractPodsResponses ?? []) as { podId?: number; deactivationDate?: string }[]
    ).find((p) => Number(p.podId) === b.podId);
    const expectedBEnd = anchors.bPodDeactivationAfterResign ?? anchors.bPodDeactivation;
    expect(String(bPod?.deactivationDate).slice(0, 10)).toBe(expectedBEnd);
  });

  await test.step('Assert POD on every F version after sign (presence)', async () => {
    await assertPodPresentOnAllFVersions(ctx.Request, f.contractId, b.podId, 4);
  });

  for (const va of versionOrder) {
    const versionRow = versions.find((v) => String(v.startDate).slice(0, 10) === va.start);
    const expected = expectedByStart[va.start];
    expect(versionRow?.versionId, `version starting ${va.start}`).toBeTruthy();
    expect(expected, `expected POD for start ${va.start}`).toBeTruthy();
    await test.step(`Assert POD at version start ${va.start}`, async () => {
      await assertPodOnVersionByStart(ctx.Request, f.contractId, Number(versionRow!.versionId), b.podId, expected);
    });
  }

  await finalizeResignTest(
    { ...ctx },
    {
      label: 'PHN-362 Case 14 (bugfix)',
      oldContractId: b.contractId,
      newContractId: f.contractId,
      expectResignLinks: true,
    },
  );
}

export async function assertCase14PodSubrangesOnF(
  Request: baseFixture['Request'],
  contractFId: number,
  podId: number,
): Promise<void> {
  const anchors = await loadCase14Anchors();
  const versionOrder: VersionAnchor[] = [
    anchors.versions.v1,
    anchors.versions.v2,
    anchors.versions.v4,
    anchors.versions.v3,
  ];
  await test.step('Case 14: POD on every F version', async () => {
    await assertPodPresentOnAllFVersions(Request, contractFId, podId, 4);
  });

  const fBody = await loadProductContract(Request, contractFId);
  const versions = (fBody.versions ?? []) as { versionId?: number; startDate?: string }[];
  const expectedByStart: Record<string, PodExpectation> = {
    [anchors.versions.v1.start]: anchors.expectedPodByVersionId[1],
    [anchors.versions.v2.start]: anchors.expectedPodByVersionId[2],
    [anchors.versions.v4.start]: anchors.expectedPodByVersionId[4],
    [anchors.versions.v3.start]: anchors.expectedPodByVersionId[3],
  };

  for (const va of versionOrder) {
    const versionRow = versions.find((v) => String(v.startDate).slice(0, 10) === va.start);
    const expected = expectedByStart[va.start];
    expect(versionRow?.versionId, `version starting ${va.start}`).toBeTruthy();
    expect(expected, `expected POD for start ${va.start}`).toBeTruthy();
    await test.step(`Assert POD at F version start ${va.start}`, async () => {
      await assertPodOnVersionByStart(Request, contractFId, Number(versionRow!.versionId), podId, expected);
    });
  }
}
