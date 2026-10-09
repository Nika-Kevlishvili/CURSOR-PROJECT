/**
 * PDT-2854 — POD must not be active in two product contracts at once.
 *
 * Reference specs:
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts (dual contract + manual activation)
 * - tests/pointOfDelivery/statusChangeContractLifeSycle.spec.ts (contract lifecycle + TERMINATED)
 * - tests/cursor/pdt-2376-volume-with-electricity.fixtures.ts (portal link attach pattern)
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import { massImportGenerator } from '../../mass-imports/generators/massImportGenerator';

type FixtureRequest = baseFixture['Request'];

/** Valeri QA repro (2026-05-19) — Dev portal contract ids. */
export const PDT_2854_VALERI_CONTRACT_A_ID = Number(process.env.PDT_2854_CONTRACT_A_ID ?? '73460');
export const PDT_2854_VALERI_CONTRACT_B_ID = Number(process.env.PDT_2854_CONTRACT_B_ID ?? '78764');
export const PDT_2854_VALERI_POD_IDENTIFIER = process.env.PDT_2854_POD_IDENTIFIER ?? '32Z4TAXTEST02021';

const DEV_FRONTEND = 'http://10.236.20.11:8080';

export type ContractPodRow = {
  identifier: string;
  podDetailId: number;
  podId: number;
  activationDate: string | null;
  deactivationDate: string | null;
};

export type ContractSnapshot = {
  contractId: number;
  contractNumber?: string;
  contractStatus?: string;
  contractSubStatus?: string;
  contractDetailId: number;
  podRow: ContractPodRow;
};

export function isoToday(): string {
  return new Date().toISOString().split('T')[0];
}

export function portalLinksForPdt2854(args: {
  contractAId: number;
  contractBId: number;
  podIdentifier: string;
  podId?: number;
}): string {
  const lines = [
    'PDT-2854 object links (Dev)',
    '',
    `POD identifier: ${args.podIdentifier}`,
    args.podId != null ? `POD id: ${args.podId}` : '',
    '',
    'Contract A',
    `${DEV_FRONTEND}/energy-product-contracts/preview/basic-parameters?id=${args.contractAId}`,
    `${DEV_FRONTEND}/energy-product-contracts/preview/point-of-delivery?id=${args.contractAId}`,
    '',
    'Contract B',
    `${DEV_FRONTEND}/energy-product-contracts/preview/basic-parameters?id=${args.contractBId}`,
    `${DEV_FRONTEND}/energy-product-contracts/preview/point-of-delivery?id=${args.contractBId}`,
    '',
    'Supply automatic activation (mass)',
    `${DEV_FRONTEND}/supply-activation`,
    '',
  ].filter(Boolean);
  return lines.join('\n');
}

export async function attachPortalLinks(links: string): Promise<void> {
  console.log(`\n========== [PDT-2854] Portal links ==========\n${links}\n============================================\n`);
  await test.info().attach('[PDT-2854] Portal links', {
    body: links,
    contentType: 'text/plain; charset=utf-8',
  });
}

export async function loadContractSnapshot(
  Request: FixtureRequest,
  contractId: number,
  podIdentifier: string,
): Promise<ContractSnapshot> {
  const res = await Request.get(`product-contract/${contractId}`);
  await expect(res).CheckResponse();
  const body = await res.json();
  const podRow = (body.contractPodsResponses ?? []).find(
    (r: { identifier?: string }) => r.identifier === podIdentifier,
  );
  expect(podRow, `POD ${podIdentifier} must exist on contract ${contractId}`).toBeDefined();

  const contractDetailId =
    body.versions?.[0]?.id ?? body.basicParameters?.id ?? body.basicParameters?.contractDetailId;
  expect(contractDetailId, `contract detail id for contract ${contractId}`).toBeTruthy();

  return {
    contractId,
    contractNumber: body.basicParameters?.contractNumber ?? body.contractNumber,
    contractStatus:
      body.basicParameters?.contractStatus ?? body.basicParameters?.status ?? body.contractStatus,
    contractSubStatus:
      body.basicParameters?.contractSubStatus ?? body.basicParameters?.subStatus ?? body.contractSubStatus,
    contractDetailId,
    podRow: {
      identifier: podRow.identifier,
      podDetailId: podRow.podDetailId,
      podId: podRow.podId,
      activationDate: podRow.activationDate ?? null,
      deactivationDate: podRow.deactivationDate ?? null,
    },
  };
}

export function buildManualActivationPayload(
  snapshot: ContractSnapshot,
  activationDate: string,
  deactivationDate: string | null = null,
) {
  return {
    identifier: snapshot.podRow.identifier,
    podDetailId: snapshot.podRow.podDetailId,
    contractDetailId: snapshot.contractDetailId,
    activationDate,
    deactivationDate,
    deactivationPurposeId: deactivationDate ? envVariables.deactivation_reason : null,
  };
}

export async function postManualActivation(Request: FixtureRequest, payload: Record<string, unknown>) {
  return Request.post('/contract-pods/manual', { data: payload });
}

export async function productContractStatusUpdate(
  Request: FixtureRequest,
  contractId: number,
  status: string,
  subStatus: string,
  versionId = 1,
) {
  const payload = {
    contractStatus: status,
    contractSubStatus: subStatus,
    contractVersionStatus: 'SIGNED',
  };
  return Request.put(`product-contract/status-update/${contractId}?versionId=${versionId}`, { data: payload });
}

/** Vertical supply-automatic-activations template (column A = POD identifier). */
export async function runSupplyAutomaticActivationMassImport(
  Request: FixtureRequest,
  FileUploadRequest: baseFixture['FileUploadRequest'],
  Responses: baseFixture['Responses'],
  podIdentifier: string,
  activationDate: string,
): Promise<{ result: { success: boolean; message: string | null }; uploadedFileId: unknown }> {
  const uploadUrl = `mass-import/SUPPLY_AUTOMATIC_ACTIVATIONS/files/upload?date=${activationDate}`;
  const downloadUrl = 'mass-import/SUPPLY_AUTOMATIC_ACTIVATIONS/template/download';
  const mi = new massImportGenerator(Request, FileUploadRequest, Responses, uploadUrl, downloadUrl, true);
  return mi.generateAndUpload([podIdentifier]);
}

export function isPodActiveOnSnapshot(snap: ContractSnapshot): boolean {
  return (
    snap.podRow.activationDate != null &&
    (snap.podRow.deactivationDate == null || snap.podRow.deactivationDate === '')
  );
}

/** Edit PUT payload for a specific contract (GeneratePayload.edit_ProductContract is hard-coded to index 0). */
export async function buildEditProductContractPayload(
  Request: FixtureRequest,
  contractId: number,
  generatedPayload: Record<string, any>,
) {
  const contractget = await Request.get(`product-contract/${contractId}?version=1`);
  await expect(contractget).CheckResponse();
  const contractgetjson = await contractget.json();

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

  const allPods = (contractgetjson.contractPodsResponses ?? []).map((pod: { podDetailId: number; dealNumber: unknown }) => ({
    pointOfDeliveryDetailId: pod.podDetailId,
    dealNumber: pod.dealNumber,
  }));

  const signingDate = contractgetjson.basicParameters?.signingDate ?? isoToday();

  const editPayload = {
    ...generatedPayload,
    basicParameters: {
      ...generatedPayload.basicParameters,
      status: contractgetjson.basicParameters.status,
      subStatus: contractgetjson.basicParameters.subStatus,
      signingDate,
    },
    productContractPointOfDeliveries: allPods,
    savingAsNewVersion: false,
    startDate: contractgetjson.versions[0].startDate,
    podRequests,
  };

  if (editPayload.additionalParameters) {
    editPayload.additionalParameters.riskAssessment = 'PERMIT';
    editPayload.additionalParameters.employeeId = 154;
  }

  return editPayload;
}

export async function assertPodNotActiveOnContract(
  Request: FixtureRequest,
  contractId: number,
  podIdentifier: string,
  context: string,
) {
  const snap = await loadContractSnapshot(Request, contractId, podIdentifier);
  expect(isPodActiveOnSnapshot(snap), `${context}: POD must not be active on contract ${contractId}`).toBeFalsy();
}

export async function assertPodActiveOnContract(
  Request: FixtureRequest,
  contractId: number,
  podIdentifier: string,
  context: string,
) {
  const snap = await loadContractSnapshot(Request, contractId, podIdentifier);
  expect(snap.podRow.activationDate, `${context}: activation_date`).toBeTruthy();
  expect(
    snap.podRow.deactivationDate == null || snap.podRow.deactivationDate === '',
    `${context}: deactivation_date must be empty while active`,
  ).toBeTruthy();
}
