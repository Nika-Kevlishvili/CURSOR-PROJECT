/**
 * PHN-362 mass-import — POD / resign cases 07+ only.
 * Cases 01–06: phn-362-mass-import-general.fixtures.ts (unchanged).
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  attachMassImportOldContractPrepSummary,
  writeMassImportPrepManifestForCase,
  type MassImportCaseDef,
} from './phn-362-mass-import-old-contract.fixtures';
import {
  finalizeMassImportExcelArtifact,
  productIdFromResponses,
  resolveCustomerForManifest,
} from './phn-362-mass-import-manifest.fixtures';
import type { MassImportOldContractPrepResult } from './phn-362-mass-import-old-contract.fixtures';
import { IDX_PRODUCT_RS, IDX_PRODUCT_STD } from './phn-362-resign-two-pods-precondition.fixtures';
import {
  PREP_EXPECTED_AFTER_RESIGN,
  PREP_POD_ACTIVATION,
  PREP_POD_INDEX_FOR_RESIGN,
  createContractBPrep,
  runMassImportOldContractEntityPreconditions,
} from './phn-362-resign-two-pods-precondition.fixtures';
import { envVariables } from '../../fixtures/envCashed';
import { loadProductContract, versionDetailId } from './pdt-2815-version-validity.fixtures';
import { createContractBCase14, loadCase14Anchors } from './phn-362-resign-case-14-bugfix.fixtures';

const POD_CASE_IDS = new Set(['07', '10', '11', '12', '14']);

export function assertPodMassImportCaseId(caseId: string): void {
  if (!POD_CASE_IDS.has(caseId)) {
    throw new Error(`Case ${caseId} is not a POD mass-import case (07, 10, 11, 12, 14)`);
  }
}

async function loadPodCaseById(caseId: string): Promise<MassImportCaseDef> {
  assertPodMassImportCaseId(caseId);
  const { pathToFileURL } = await import('url');
  const setup = await import(
    pathToFileURL('c:/Users/k.nanuashvili/Desktop/tasks/mass import/phn-362/mass-import-setup.mjs').href
  );
  return setup.getPodCaseById(caseId);
}

async function setPodDeactivationOnContractVersion(
  Request: baseFixture['Request'],
  Responses: baseFixture['Responses'],
  contractId: number,
  logicalVersionId: number,
  podIndex: number,
  activationDate: string,
  deactivationDate: string,
): Promise<void> {
  const body = await loadProductContract(Request, contractId);
  const contractDetailId = versionDetailId(body, logicalVersionId);
  expect(contractDetailId, `version ${logicalVersionId} detail id`).toBeTruthy();

  const podId = Number(Responses.pod[podIndex].id);
  const podRow = (
    (body.contractPodsResponses ?? []) as {
      podId?: number;
      identifier?: string;
      podDetailId?: number;
    }[]
  ).find((r) => Number(r.podId) === podId);
  expect(podRow, `POD index ${podIndex} on contract ${contractId}`).toBeTruthy();

  const activation = await Request.post('/contract-pods/manual', {
    data: {
      identifier: podRow!.identifier,
      podDetailId: podRow!.podDetailId,
      contractDetailId,
      activationDate,
      deactivationDate,
      deactivationPurposeId: envVariables.deactivation_reason,
    },
  });
  await expect(activation).CheckResponse();
}

async function applyPodCaseBPostSetup(
  caseId: string,
  Request: baseFixture['Request'],
  Responses: baseFixture['Responses'],
  contractBId: number,
  podIdentifier: string,
): Promise<void> {
  if (caseId === '07') {
    await test.step('Case 07: end POD on B before calculated F activation (AC4 / TC-BE-17)', async () => {
      const bDeactivation = PREP_EXPECTED_AFTER_RESIGN.bDeactivation;
      await setPodDeactivationOnContractVersion(
        Request,
        Responses,
        contractBId,
        1,
        PREP_POD_INDEX_FOR_RESIGN,
        PREP_POD_ACTIVATION,
        bDeactivation,
      );
      const bBody = await loadProductContract(Request, contractBId);
      const row = ((bBody.contractPodsResponses ?? []) as { identifier?: string; deactivationDate?: string }[]).find(
        (r) => r.identifier === podIdentifier,
      );
      expect(row?.deactivationDate).toBe(bDeactivation);
      expect(PREP_EXPECTED_AFTER_RESIGN.fActivation > bDeactivation).toBeTruthy();
    });
  }
}

export type MassImportPodCasePrepFixtures = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'FileUploadRequest'
>;

export async function runMassImportPodCasePrep(
  caseId: string,
  fixtures: MassImportPodCasePrepFixtures,
): Promise<MassImportOldContractPrepResult> {
  assertPodMassImportCaseId(caseId);
  const massImportCase = await loadPodCaseById(caseId);

  await runMassImportOldContractEntityPreconditions(
    fixtures.Request,
    fixtures.GeneratePayload,
    fixtures.Responses,
    fixtures.Endpoints,
    fixtures.FileUploadRequest,
    { caseId },
  );

  let result: Awaited<ReturnType<typeof createContractBPrep>>;
  if (caseId === '14') {
    const anchors = await loadCase14Anchors();
    const case14B = await createContractBCase14(fixtures, anchors);
    result = {
      contractId: case14B.contractId,
      podId: case14B.podId,
      podIdentifier: case14B.podIdentifier,
      podDetailId: await (async () => {
        const podGet = await fixtures.Request.get(`pod/${case14B.podId}`);
        await expect(podGet).CheckResponse();
        const body = await podGet.json();
        return Number(body.podDetailId ?? body.lastPodDetailId);
      })(),
    };
  } else {
    result = await createContractBPrep(
      fixtures.Request,
      fixtures.GeneratePayload,
      fixtures.Responses,
      fixtures.Endpoints,
    );
    await applyPodCaseBPostSetup(caseId, fixtures.Request, fixtures.Responses, result.contractId, result.podIdentifier);
  }

  const bBody = await loadProductContract(fixtures.Request, result.contractId);
  const bp = bBody.basicParameters as { contractNumber?: string; status?: string };
  expect(['ACTIVE_IN_TERM', 'ENTERED_INTO_FORCE', 'ACTIVE_IN_PERPETUITY']).toContain(
    String(bp.status ?? ''),
  );
  const contractNumber = bp.contractNumber ?? 'EPES3620000001';

  const manifestPath = await writeMassImportPrepManifestForCase(
    fixtures.Request,
    fixtures.Endpoints,
    fixtures.Responses,
    massImportCase,
    {
      contractBId: result.contractId,
      contractNumber,
      podIdentifier: result.podIdentifier,
      podId: result.podId,
      podDetailId: result.podDetailId,
    },
  );

  const { excelPath, fContractNumber } = await finalizeMassImportExcelArtifact(caseId);

  return {
    contractBId: result.contractId,
    contractNumber,
    podIdentifier: result.podIdentifier,
    podId: result.podId,
    podDetailId: result.podDetailId,
    productStdId: productIdFromResponses(fixtures.Responses.product[IDX_PRODUCT_STD]),
    productRsId: productIdFromResponses(fixtures.Responses.product[IDX_PRODUCT_RS]),
    manifestPath,
    excelPath,
    fContractNumber,
    massImportCase,
  };
}

export async function runPhn362MassImportPodCasePrepTest(
  caseId: string,
  caseSlug: string,
  caseTitle: string,
  fixtures: MassImportPodCasePrepFixtures,
): Promise<void> {
  test.setTimeout(25 * 60 * 1000);
  test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
  test.info().annotations.push({ type: 'workflow', description: 'mass-import-pod-case' });
  test.info().annotations.push({ type: 'mass-import-case', description: caseId });

  const prep = await test.step('Old contract B — POD case prep + Excel', async () =>
    runMassImportPodCasePrep(caseId, fixtures),
  );

  const customerManifest = await resolveCustomerForManifest(
    fixtures.Request,
    fixtures.Endpoints,
    fixtures.Responses,
  );

  attachMassImportOldContractPrepSummary(
    test.info(),
    prep,
    customerManifest,
    reportGenerator.setLinksToResponses(fixtures.Responses),
    { caseSlug, caseTitle },
  );
}
