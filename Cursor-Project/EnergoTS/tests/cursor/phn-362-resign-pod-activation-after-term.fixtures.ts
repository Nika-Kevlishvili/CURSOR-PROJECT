/**
 * PHN-362 / Contract re-signing Step 10.0 — TC-BE-21 (Contract_re_signing_changes.md).
 *
 * Old contract B: POD activation is **after** the new contract F term anchor (`supplyActivationValue` / signing).
 * Also: B initial term end < B POD activation (diagram gate: initial term end >= POD activation on old).
 *
 * F profile matches WITHOUT_SUPPLY + Wait=Yes + EXACT_DATE (user JSON pattern).
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { addDaysIso } from './contract-resign-new-flow.fixtures';
import {
  activatePodOnContract,
  buildEditProductContractPayload,
  loadProductContract,
  productContractStatusUpdate,
} from './pdt-2815-version-validity.fixtures';
import {
  IDX_PRODUCT_RS,
  IDX_PRODUCT_STD,
  PREP_EMPLOYEE_ID,
  PREP_POD_INDEX_FOR_RESIGN,
  findPodRow,
  runSharedPreconditionsPrep,
} from './phn-362-resign-two-pods-precondition.fixtures';

type FixtureRequest = baseFixture['Request'];
type FixtureResponses = baseFixture['Responses'];
type FixtureEndpoints = baseFixture['Endpoints'];
type FixtureGeneratePayload = baseFixture['GeneratePayload'];

const MSG_RESIGNING_FAILED_ALL_PODS = 'Resigning process failed for all pods';

export const TC21_RUNTIME_TODAY = (() => new Date().toISOString().slice(0, 10))();

function tc21LastDayOfMonth(isoDate: string): string {
  const d = new Date(`${isoDate}T12:00:00.000Z`);
  d.setUTCMonth(d.getUTCMonth() + 1);
  d.setUTCDate(0);
  return d.toISOString().split('T')[0];
}

/** F signing — recent past, inside P-STD resigning window (user sample used ~today-4). */
export const TC21_F_SIGNING = addDaysIso(TC21_RUNTIME_TODAY, -1);

/** New contract term anchor on third tab — EXACT_DATE value (user JSON `supplyActivationValue`). */
export const TC21_F_TERM_ANCHOR = TC21_F_SIGNING;

export const TC21_B_ENTRY_IN_FORCE_SIGN_PUT = addDaysIso(TC21_RUNTIME_TODAY, 1);

/** B initial term end — after entry in force, before POD activation (TC-BE-21). */
export const TC21_B_INITIAL_TERM_END = addDaysIso(TC21_B_ENTRY_IN_FORCE_SIGN_PUT, +1);

/** B POD activation — after F term anchor and after B initial term end. */
export const TC21_B_POD_ACTIVATION = addDaysIso(TC21_B_INITIAL_TERM_END, +5);

/** Align with prep resigning window (end of current month). */
export const TC21_B_CONTRACT_TERM_END = tc21LastDayOfMonth(TC21_RUNTIME_TODAY);
export const TC21_B_VERSION_START = addDaysIso(TC21_B_POD_ACTIVATION, -14);
export const TC21_B_SIGNING_DATE = addDaysIso(TC21_B_VERSION_START, -14);

function resignedLinkIds(body: Record<string, unknown>, direction: 'from' | 'to'): number[] {
  const bp = (body.basicParameters ?? {}) as {
    resignedFrom?: { contractId?: number }[];
    resignedTo?: { contractId?: number }[];
  };
  const links = direction === 'from' ? bp.resignedFrom ?? [] : bp.resignedTo ?? [];
  return links.map((l) => Number(l.contractId)).filter((id) => !Number.isNaN(id));
}

async function resolvePodDetailId(
  Request: FixtureRequest,
  Responses: FixtureResponses,
  podIndex: number,
): Promise<number> {
  const pod = Responses.pod[podIndex] as { podDetailId?: number; id?: number };
  if (pod.podDetailId != null) return Number(pod.podDetailId);
  const podGet = await Request.get(`pod/${pod.id}`);
  await expect(podGet).CheckResponse();
  const body = await podGet.json();
  return Number(body.podDetailId ?? body.lastPodDetailId ?? body.versions?.[0]?.podDetailId);
}

async function annualMwhForPodIndex(
  Request: FixtureRequest,
  Responses: FixtureResponses,
  podIndex: number,
): Promise<number> {
  const podGet = await Request.get(`pod/${Responses.pod[podIndex].id}?version=1`);
  await expect(podGet).CheckResponse();
  const podJson = await podGet.json();
  const monthlyKwh =
    podJson.type === 'CONSUMER' ? Number(podJson.estimatedMonthlyAvgConsumption ?? 0) : 0;
  return (monthlyKwh * 12) / 1000;
}

function buildTc21BSignPutBody(signPayload: Record<string, unknown>): Record<string, unknown> {
  const putBody = JSON.parse(JSON.stringify(signPayload)) as Record<string, unknown>;
  const bp = putBody.basicParameters as Record<string, unknown>;
  bp.entryInForceDate = TC21_B_ENTRY_IN_FORCE_SIGN_PUT;
  delete bp.startOfInitialTerm;
  delete bp.statusModifyDate;
  bp.signingDate = TC21_B_SIGNING_DATE;
  bp.contractTermEndDate = TC21_B_CONTRACT_TERM_END;
  const pp = putBody.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.entryIntoForce = 'SIGNING';
  delete pp.entryIntoForceValue;
  pp.startOfContractInitialTerm = 'EXACT_DATE';
  pp.startOfContractValue = TC21_B_INITIAL_TERM_END;
  pp.contractTermEndDate = TC21_B_CONTRACT_TERM_END;
  pp.supplyActivation = 'MANUAL';
  delete pp.supplyActivationValue;
  return putBody;
}

async function buildTc21EditProductContractPayload(
  Request: FixtureRequest,
  contractId: number,
  generatedPayload: Record<string, unknown>,
  opts?: { startDate?: string; signingDate?: string },
): Promise<Record<string, unknown>> {
  const contractget = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractget).CheckResponse();
  const contractgetjson = await contractget.json();

  const allPods = (contractgetjson.contractPodsResponses ?? []).map(
    (pod: { podDetailId: number; dealNumber: unknown }) => ({
      pointOfDeliveryDetailId: pod.podDetailId,
      dealNumber: pod.dealNumber,
    }),
  );

  const podsByBillingGroup = new Map<number, { pointOfDeliveryDetailId: number; dealNumber: unknown }[]>();
  for (const pod of contractgetjson.contractPodsResponses ?? []) {
    const billingGroupId = pod.billingGroupId;
    if (!podsByBillingGroup.has(billingGroupId)) {
      podsByBillingGroup.set(billingGroupId, []);
    }
    podsByBillingGroup.get(billingGroupId)!.push({
      pointOfDeliveryDetailId: pod.podDetailId,
      dealNumber: pod.dealNumber,
    });
  }

  const podRequests = Array.from(podsByBillingGroup.entries()).map(([billingGroupId, pods]) => ({
    billingGroupId,
    productContractPointOfDeliveries: pods,
  }));

  const serverStartDate = contractgetjson.versions[0].startDate as string;
  const startDate = opts?.startDate ?? serverStartDate;
  const signingDate = opts?.signingDate ?? TC21_B_SIGNING_DATE;

  const editPayload: Record<string, unknown> = {
    ...generatedPayload,
    basicParameters: {
      ...(generatedPayload.basicParameters as object),
      status: contractgetjson.basicParameters.status,
      subStatus: contractgetjson.basicParameters.subStatus,
      signingDate,
      entryInForceDate: null,
      startOfInitialTerm: null,
    },
    productParameters: {
      ...(generatedPayload.productParameters as object),
      entryIntoForceValue: null,
      startOfContractValue: null,
    },
    productContractPointOfDeliveries: allPods,
    savingAsNewVersion: false,
    startDate,
    podRequests,
  };

  if (editPayload.additionalParameters) {
    (editPayload.additionalParameters as Record<string, unknown>).riskAssessment = 'PERMIT';
    (editPayload.additionalParameters as Record<string, unknown>).employeeId = PREP_EMPLOYEE_ID;
  }

  return editPayload;
}

export async function createContractBTc21(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
): Promise<{ contractId: number; contractPayload: Record<string, unknown>; podIdentifier: string }> {
  const podIndex = PREP_POD_INDEX_FOR_RESIGN;
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_STD,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.signingDate = null;
  bp.contractTermEndDate = TC21_B_CONTRACT_TERM_END;
  bp.startOfInitialTerm = null;
  bp.entryInForceDate = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'SUPPLY_ONLY';
  pp.contractTermEndDate = TC21_B_CONTRACT_TERM_END;
  pp.supplyActivation = 'MANUAL';
  pp.supplyActivationValue = null;
  pp.entryIntoForce = 'SIGNING';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'SIGNING';
  pp.startOfContractValue = null;

  const podDetailId = await resolvePodDetailId(Request, Responses, podIndex);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const annualMwh = await annualMwhForPodIndex(Request, Responses, podIndex);
  (contractPayload.additionalParameters as Record<string, unknown>).estimatedTotalConsumptionUnderContractKwh =
    annualMwh;

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);
  const contractId = createJson.id as number;

  await test.step('Contract B: SIGNED + initial term end before POD activation', async () => {
    const signPayload = await buildTc21EditProductContractPayload(Request, contractId, contractPayload, {
      startDate: TC21_B_VERSION_START,
      signingDate: TC21_B_SIGNING_DATE,
    });
    const signBp = signPayload.basicParameters as Record<string, unknown>;
    signBp.status = 'SIGNED';
    signBp.subStatus = 'SIGNED_BY_BOTH_SIDES';
    signBp.versionStatus = 'SIGNED';

    const put = await Request.put(
      `${Endpoints.productContract}/${contractId}?versionId=1&changeFutureVersionsPods=false`,
      { data: buildTc21BSignPutBody(signPayload) },
    );
    await expect(put).CheckResponse();
  });

  await test.step(`Contract B: activate POD at ${TC21_B_POD_ACTIVATION} (after F term anchor)`, async () => {
    await activatePodOnContract(
      Request,
      Responses,
      contractId,
      1,
      TC21_B_POD_ACTIVATION,
      podIndex,
    );
  });

  const podGet = await Request.get(`pod/${Responses.pod[podIndex].id}`);
  await expect(podGet).CheckResponse();
  const podIdentifier = String((await podGet.json()).identifier);

  return { contractId, contractPayload, podIdentifier };
}

/** F DRAFT — WITHOUT_SUPPLY, Wait=Yes, EXACT_DATE + supplyActivationValue (user JSON). */
export async function createContractFDraftTc21(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
): Promise<{ contractId: number; contractPayload: Record<string, unknown> }> {
  const podIndex = PREP_POD_INDEX_FOR_RESIGN;
  const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
    0,
    IDX_PRODUCT_RS,
  )) as Record<string, unknown>;

  const bp = contractPayload.basicParameters as Record<string, unknown>;
  bp.status = 'DRAFT';
  bp.subStatus = 'DRAFT';
  bp.versionStatus = 'SIGNED';
  bp.signingDate = null;
  bp.entryInForceDate = null;
  bp.startOfInitialTerm = null;
  bp.contractTermEndDate = null;

  const pp = contractPayload.productParameters as Record<string, unknown>;
  pp.contractType = 'WITHOUT_SUPPLY';
  pp.contractTermEndDate = null;
  pp.entryIntoForce = 'MANUAL';
  pp.entryIntoForceValue = null;
  pp.startOfContractInitialTerm = 'MANUAL';
  pp.startOfContractValue = null;
  pp.supplyActivation = 'EXACT_DATE';
  pp.supplyActivationValue = TC21_F_TERM_ANCHOR;
  pp.productContractWaitForOldContractTermToExpires = 'YES';

  const podDetailId = await resolvePodDetailId(Request, Responses, podIndex);
  contractPayload.productContractPointOfDeliveries = [{ pointOfDeliveryDetailId: podDetailId, dealNumber: null }];

  const annualMwh = await annualMwhForPodIndex(Request, Responses, podIndex);
  (contractPayload.additionalParameters as Record<string, unknown>).estimatedTotalConsumptionUnderContractKwh =
    annualMwh;

  const createRes = await Request.post(Endpoints.productContract, { data: contractPayload });
  await expect(createRes).CheckResponse();
  const createJson = await createRes.json();
  Responses.productContract.push(createJson);

  return { contractId: createJson.id as number, contractPayload };
}

export async function assertTc21DatePreconditions(
  Request: FixtureRequest,
  contractBId: number,
  podIdentifier: string,
) {
  await test.step('Assert: B POD activation > F term anchor; B initial term end < POD activation', async () => {
    expect(TC21_B_POD_ACTIVATION > TC21_F_TERM_ANCHOR).toBeTruthy();
    expect(TC21_B_INITIAL_TERM_END < TC21_B_POD_ACTIVATION).toBeTruthy();

    const bBody = await loadProductContract(Request, contractBId);
    const bBp = (bBody.basicParameters ?? {}) as {
      startOfInitialTerm?: string | null;
      contractTermEndDate?: string;
    };
    const bPod = findPodRow(bBody, podIdentifier) as { activationDate?: string };
    expect(bBp.startOfInitialTerm).toBe(TC21_B_INITIAL_TERM_END);
    expect(bPod?.activationDate).toBe(TC21_B_POD_ACTIVATION);
    expect(bBp.contractTermEndDate).toBe(TC21_B_CONTRACT_TERM_END);
  });
}

export async function signContractFTc21ExpectResignBlocked(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  contractFId: number,
  contractFPayload: Record<string, unknown>,
) {
  await test.step(`Sign F at ${TC21_F_SIGNING} — expect re-sign blocked (TC-BE-21)`, async () => {
    const ready = await productContractStatusUpdate(Request, contractFId, 'READY', 'READY', 1);
    await expect(ready).CheckResponse();
    const signedStatus = await productContractStatusUpdate(
      Request,
      contractFId,
      'SIGNED',
      'SIGNED_BY_BOTH_SIDES',
      1,
    );
    await expect(signedStatus).CheckResponse();

    const editPayload = await buildEditProductContractPayload(Request, contractFId, contractFPayload, {
      preserveSigningDate: false,
      startDate: TC21_F_SIGNING,
    });

    const bp = editPayload.basicParameters as Record<string, unknown>;
    bp.status = 'SIGNED';
    bp.subStatus = 'SIGNED_BY_BOTH_SIDES';
    bp.versionStatus = 'SIGNED';
    bp.signingDate = TC21_F_SIGNING;
    bp.entryInForceDate = null;
    bp.startOfInitialTerm = null;
    bp.contractTermEndDate = null;

    const pp = editPayload.productParameters as Record<string, unknown>;
    pp.contractType = 'WITHOUT_SUPPLY';
    pp.contractTermEndDate = null;
    pp.entryIntoForce = 'MANUAL';
    pp.entryIntoForceValue = null;
    pp.startOfContractInitialTerm = 'MANUAL';
    pp.startOfContractValue = null;
    pp.supplyActivation = 'EXACT_DATE';
    pp.supplyActivationValue = TC21_F_TERM_ANCHOR;
    pp.productContractWaitForOldContractTermToExpires = 'YES';

    if (editPayload.additionalParameters) {
      (editPayload.additionalParameters as Record<string, unknown>).employeeId = PREP_EMPLOYEE_ID;
      (editPayload.additionalParameters as Record<string, unknown>).riskAssessment = 'PERMIT';
    }

    const put = await Request.put(
      `${Endpoints.productContract}/${contractFId}?versionId=1&changeFutureVersionsPods=false`,
      { data: editPayload },
    );
    const status = put.status();
    const text = await put.text();
    let body: Record<string, unknown> = {};
    try {
      body = JSON.parse(text) as Record<string, unknown>;
    } catch {
      /* non-JSON */
    }

    const haystack = `${text} ${JSON.stringify(body)}`.toLowerCase();
    const messages = (body.resigningMessages ?? []) as string[];
    const resignBlocked =
      (status >= 400 &&
        (haystack.includes('only for re-signing') ||
          haystack.includes(MSG_RESIGNING_FAILED_ALL_PODS.toLowerCase()) ||
          haystack.includes('resigning process failed'))) ||
      messages.some((m) => String(m).includes(MSG_RESIGNING_FAILED_ALL_PODS));

    expect(
      resignBlocked,
      `Expected resign blocked (TC-BE-21); status=${status} body=${text}`,
    ).toBeTruthy();
  });
}

export async function assertTc21NoResignPersistence(
  Request: FixtureRequest,
  contractBId: number,
  contractFId: number,
  podIdentifier: string,
) {
  await test.step('Assert: no re-sign persistence (no links, B POD not Re-signed, F POD not activated)', async () => {
    const bBody = await loadProductContract(Request, contractBId);
    const fBody = await loadProductContract(Request, contractFId);

    expect(resignedLinkIds(bBody, 'to')).toEqual([]);
    expect(resignedLinkIds(fBody, 'from')).toEqual([]);

    const bPod = findPodRow(bBody, podIdentifier) as { isResigned?: boolean; deactivationDate?: string | null };
    expect(bPod?.isResigned ?? false).toBe(false);
    expect(bPod?.deactivationDate ?? null).toBeNull();

    const fPod = findPodRow(fBody, podIdentifier) as { activationDate?: string | null };
    expect(fPod?.activationDate ?? null).toBeNull();

    const fBp = (fBody.basicParameters ?? {}) as { status?: string; subStatus?: string };
    expect(
      fBp.status === 'SIGNED' || fBp.status === 'DRAFT' || fBp.status === 'READY',
      `F status after blocked resign (got ${fBp.status})`,
    ).toBeTruthy();
    expect(resignedLinkIds(fBody, 'from')).toEqual([]);
  });
}

export async function runTc21PodActivationAfterTermScenario(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
) {
  let contractBId = 0;
  let contractFId = 0;
  let podIdentifier = '';

  await runSharedPreconditionsPrep(Request, GeneratePayload, Responses, Endpoints, FileUploadRequest);

  const b = await createContractBTc21(Request, GeneratePayload, Responses, Endpoints);
  contractBId = b.contractId;
  podIdentifier = b.podIdentifier;

  const f = await createContractFDraftTc21(Request, GeneratePayload, Responses, Endpoints);
  contractFId = f.contractId;

  await assertTc21DatePreconditions(Request, contractBId, podIdentifier);
  await signContractFTc21ExpectResignBlocked(Request, Endpoints, contractFId, f.contractPayload);
  await assertTc21NoResignPersistence(Request, contractBId, contractFId, podIdentifier);

  return { contractBId, contractFId, podIdentifier };
}
