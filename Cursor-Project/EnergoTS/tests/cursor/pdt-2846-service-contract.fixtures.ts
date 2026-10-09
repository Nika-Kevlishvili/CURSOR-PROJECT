/**
 * PDT-2846 — Service contract version 1 startDate realignment (backend API tests).
 */
import { test, expect } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import {
  addDaysIso,
  attachResponseLinkerAfterTest,
  buildServiceContractEditPayloadFromDetail,
  pickLatestLogicalVersionId,
  pdt2599MinCalendarIsoForServiceContractPost,
  runPdt2846DraftServiceContractChain,
  type Pdt2599ChainFixtures,
} from './pdt-2599-service-contract.fixtures';

export { runPdt2846DraftServiceContractChain, addDaysIso, pdt2599MinCalendarIsoForServiceContractPost };

export type Pdt2846EditOpts = {
  savingAsNewVersion: boolean;
  startDate?: string;
  contractVersionStatus?: string;
  signInDate?: string | null;
  contractStatus?: string;
  detailsSubStatus?: string;
  entryIntoForceDate?: string | null;
};

export function buildPdt2846EditPayload(
  detail: Record<string, unknown>,
  opts: Pdt2846EditOpts,
): Record<string, unknown> {
  const payload = buildServiceContractEditPayloadFromDetail(detail, {
    savingAsNewVersion: opts.savingAsNewVersion,
    startDate: opts.startDate,
    contractVersionStatus: opts.contractVersionStatus,
    signInDate: opts.signInDate,
  });
  const bp = payload.basicParameters as Record<string, unknown>;
  if (opts.contractStatus != null) bp.contractStatus = opts.contractStatus;
  if (opts.detailsSubStatus != null) bp.detailsSubStatus = opts.detailsSubStatus;
  if (opts.entryIntoForceDate !== undefined) bp.entryIntoForceDate = opts.entryIntoForceDate;
  return payload;
}

type RequestLike = Pdt2599ChainFixtures['Request'];
type EndpointsLike = Pdt2599ChainFixtures['Endpoints'];

export async function loadPdt2846ServiceContractDetail(
  Request: RequestLike,
  Endpoints: EndpointsLike,
  contractId: number,
  versionId?: number,
): Promise<Record<string, unknown>> {
  const res = await Request.get(`${Endpoints.serviceContract}/${contractId}`, {
    params: versionId != null ? { versionId } : undefined,
  });
  await expect(res).CheckResponse();
  return (await res.json()) as Record<string, unknown>;
}

export async function loadPdt2846DetailForEdit(
  Request: RequestLike,
  Endpoints: EndpointsLike,
  contractId: number,
  versionId?: number,
): Promise<Record<string, unknown>> {
  const detail = await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, versionId);
  const sid = (detail.basicParameters as { serviceDetailId?: number } | undefined)?.serviceDetailId;
  if (!sid) return detail;
  const tRes = await Request.get(`${Endpoints.serviceContract}/third-tab-fields`, {
    params: { serviceDetailId: sid },
  });
  await expect(tRes).CheckResponse();
  const tab = (await tRes.json()) as Record<string, unknown>;
  const prev = (detail.thirdPageTabs ?? {}) as Record<string, unknown>;
  return { ...detail, thirdPageTabs: { ...prev, ...tab } };
}

export function readPdt2846VersionStartDate(
  detail: Record<string, unknown>,
  versionId: number,
): string {
  const versions = (detail.versions ?? []) as { versionId?: number; startDate?: string }[];
  const row = versions.find((v) => v.versionId === versionId);
  expect(row, `versions must contain versionId=${versionId}`).toBeTruthy();
  return String(row!.startDate).slice(0, 10);
}

export function readPdt2846BasicDates(detail: Record<string, unknown>): {
  startDate: string;
  signInDate: string | null;
  contractVersionStatus: string;
  contractStatus: string;
} {
  const basic = (detail.basicParameters ?? {}) as Record<string, unknown>;
  const versions = (detail.versions ?? []) as { versionId?: number; startDate?: string }[];
  const v1 = versions.find((v) => v.versionId === 1) ?? versions[0];
  const startDate = String(
    basic.startDate ?? v1?.startDate ?? basic.creationDate ?? '',
  ).slice(0, 10);
  const rawSign =
    basic.signInDate ?? basic.signingDate ?? (detail as { signingDate?: unknown }).signingDate;
  const signInDate =
    rawSign == null || rawSign === '' ? null : String(rawSign).slice(0, 10);
  return {
    startDate,
    signInDate,
    contractVersionStatus: String(basic.contractVersionStatus ?? v1?.status ?? ''),
    contractStatus: String(basic.contractStatus ?? ''),
  };
}

export async function pdt2846PutServiceContract(
  Request: RequestLike,
  Endpoints: EndpointsLike,
  contractId: number,
  versionId: number,
  payload: Record<string, unknown>,
): Promise<Awaited<ReturnType<RequestLike['put']>>> {
  return Request.put(`${Endpoints.serviceContract}/${contractId}`, {
    params: { versionId },
    data: payload,
  });
}

export async function pdt2846AssertDraftV1AtCreate(
  Request: RequestLike,
  Endpoints: EndpointsLike,
  contractId: number,
): Promise<{ today: string; detail: Record<string, unknown> }> {
  const today = pdt2599MinCalendarIsoForServiceContractPost();
  const detail = await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1);
  const dates = readPdt2846BasicDates(detail);
  expect(dates.startDate).toBe(today);
  expect(dates.signInDate).toBeNull();
  expect(dates.contractStatus).toMatch(/DRAFT/i);
  const versions = (detail.versions ?? []) as { versionId?: number }[];
  expect(versions[0]?.versionId).toBe(1);
  return { today, detail };
}

/** Contract header → READY; version 1 stays SIGNED (API forbids READY on first version). */
export async function pdt2846PromoteV1ToReady(
  Request: RequestLike,
  Endpoints: EndpointsLike,
  contractId: number,
): Promise<void> {
  const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
  const putRes = await pdt2846PutServiceContract(
    Request,
    Endpoints,
    contractId,
    1,
    buildPdt2846EditPayload(detail, {
      savingAsNewVersion: false,
      contractStatus: 'READY',
      detailsSubStatus: 'READY',
      contractVersionStatus: 'SIGNED',
      signInDate: null,
    }),
  );
  await expect(putRes).CheckResponse();
}

export async function pdt2846FinalizeV1Signed(
  Request: RequestLike,
  Endpoints: EndpointsLike,
  contractId: number,
  opts: {
    signInDate: string;
    startDate: string;
    entryIntoForceDate?: string | null;
    contractStatus?: string;
  },
): Promise<Record<string, unknown>> {
  const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
  const putRes = await pdt2846PutServiceContract(
    Request,
    Endpoints,
    contractId,
    1,
    buildPdt2846EditPayload(detail, {
      savingAsNewVersion: false,
      contractStatus: opts.contractStatus ?? 'SIGNED',
      detailsSubStatus: 'SIGNED_BY_BOTH_SIDES',
      contractVersionStatus: 'SIGNED',
      signInDate: opts.signInDate,
      startDate: opts.startDate,
      entryIntoForceDate: opts.entryIntoForceDate ?? opts.signInDate,
    }),
  );
  await expect(putRes).CheckResponse();
  return loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1);
}

export async function pdt2846FinalizeV1EnteredIntoForce(
  Request: RequestLike,
  Endpoints: EndpointsLike,
  contractId: number,
  opts: { signInDate: string; startDate: string; entryIntoForceDate: string },
): Promise<Record<string, unknown>> {
  const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId, 1);
  const putRes = await pdt2846PutServiceContract(
    Request,
    Endpoints,
    contractId,
    1,
    buildPdt2846EditPayload(detail, {
      savingAsNewVersion: false,
      contractStatus: 'ENTERED_INTO_FORCE',
      detailsSubStatus: 'AWAITING_ACTIVATION',
      contractVersionStatus: 'SIGNED',
      signInDate: opts.signInDate,
      startDate: opts.startDate,
      entryIntoForceDate: opts.entryIntoForceDate,
    }),
  );
  await expect(putRes).CheckResponse();
  return loadPdt2846ServiceContractDetail(Request, Endpoints, contractId, 1);
}

export async function expectPdt2846Put4xx(
  putRes: Awaited<ReturnType<RequestLike['put']>>,
  messagePattern: RegExp,
): Promise<void> {
  const text = await putRes.text();
  expect(putRes.status(), text).toBeGreaterThanOrEqual(400);
  expect(text.toLowerCase()).toMatch(messagePattern);
}

export function attachPdt2846CreatedEntities(
  Responses: baseFixture['Responses'],
  contractId: number,
  extra?: Record<string, unknown>,
): void {
  attachResponseLinkerAfterTest(Responses, 'PDT-2846');
  const createdIds = {
    customerId: Responses.customer[0]?.id,
    priceComponentId: Responses.priceComponent[0],
    termId: Responses.terms[0]?.id,
    podId: Responses.pod[0]?.id,
    serviceId: Responses.service[0]?.id,
    serviceContractId: contractId,
  };
  test.info().attach('[PDT-2846] Created entity IDs', {
    body: JSON.stringify(
      {
        createdIds,
        note: 'Service contract portal preview URL(s): see attachments above and console.',
        ...extra,
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });
}

export async function pdt2846CreateVersion2Draft(
  Request: RequestLike,
  Endpoints: EndpointsLike,
  contractId: number,
  startDate: string,
): Promise<number> {
  const detail = await loadPdt2846DetailForEdit(Request, Endpoints, contractId);
  const v1 = pickLatestLogicalVersionId(detail.versions as { versionId: number }[]);
  const existingSign = readPdt2846BasicDates(detail).signInDate;
  const signForNewVersion = existingSign ?? startDate;
  const putRes = await pdt2846PutServiceContract(
    Request,
    Endpoints,
    contractId,
    v1,
    buildPdt2846EditPayload(detail, {
      savingAsNewVersion: true,
      startDate,
      contractVersionStatus: 'DRAFT',
      signInDate: signForNewVersion,
    }),
  );
  await expect(putRes).CheckResponse();
  const after = await loadPdt2846ServiceContractDetail(Request, Endpoints, contractId);
  return pickLatestLogicalVersionId(after.versions as { versionId: number }[]);
}
