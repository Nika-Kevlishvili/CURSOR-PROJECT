/**
 * PHN-362 — Mass import old contract B prep (thin Playwright adapter).
 * Case config + orchestration live in tasks/mass import/phn-362/.
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import {
  IDX_PRODUCT_RS,
  IDX_PRODUCT_STD,
  PREP_B_SIGNING_DATE,
  PREP_RUNTIME_TODAY,
  createContractBPrep,
  runMassImportOldContractEntityPreconditions,
} from './phn-362-resign-two-pods-precondition.fixtures';
import {
  MASS_IMPORT_OUTPUT_DIR,
  MASS_IMPORT_PHN362_DIR,
  finalizeMassImportExcelArtifactStep,
  productIdFromResponses,
  resolveCustomerForManifest,
  type WriteMassImportManifestInput,
} from './phn-362-mass-import-manifest.fixtures';
import { loadProductContract } from './pdt-2815-version-validity.fixtures';

export type MassImportCaseDef = {
  id: string;
  folder: string;
  file: string;
  contractNumberKey: string;
  waitForOld: string;
  supplyAfterResign: string;
};

export type MassImportOldContractPrepResult = {
  contractBId: number;
  contractNumber: string;
  podIdentifier: string;
  podId: number;
  podDetailId: number;
  productStdId: number;
  productRsId: number;
  manifestPath: string;
  excelPath: string;
  fContractNumber: string;
  massImportCase: MassImportCaseDef;
};

type FixtureRequest = baseFixture['Request'];
type FixtureResponses = baseFixture['Responses'];
type FixtureEndpoints = baseFixture['Endpoints'];
type FixtureGeneratePayload = baseFixture['GeneratePayload'];

async function loadTasksSetup() {
  const { pathToFileURL } = await import('url');
  return import(pathToFileURL(`${MASS_IMPORT_PHN362_DIR}/mass-import-setup.mjs`).href) as Promise<{
    getCaseById: (id: string) => MassImportCaseDef;
    resolveCaseManifestDir: (caseId: string) => string;
    DEFAULT_SETUP_CASE_ID: string;
  }>;
}

/** Case 01 → root; cases 02–06 → cases/{folder}/prep-manifest.json */
export async function resolveCaseManifestDir(caseId: string): Promise<string> {
  const setup = await loadTasksSetup();
  return setup.resolveCaseManifestDir(caseId);
}

export function resolveMassImportCaseId(): string {
  return process.env.MASS_IMPORT_CASE?.trim() || '01';
}

export async function loadMassImportCase(caseId?: string): Promise<MassImportCaseDef> {
  const setup = await loadTasksSetup();
  return setup.getCaseById(caseId ?? resolveMassImportCaseId());
}

export type RunMassImportOldContractPrepOptions = {
  /** Mass-import case id from cases-registry.json (01–06). Defaults to MASS_IMPORT_CASE env or 01. */
  caseId?: string;
};

export async function runMassImportOldContractPrep(
  Request: FixtureRequest,
  GeneratePayload: FixtureGeneratePayload,
  Responses: FixtureResponses,
  Endpoints: FixtureEndpoints,
  FileUploadRequest: baseFixture['FileUploadRequest'],
  options?: RunMassImportOldContractPrepOptions,
): Promise<MassImportOldContractPrepResult> {
  const massImportCase = await loadMassImportCase(options?.caseId);

  await runMassImportOldContractEntityPreconditions(
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    FileUploadRequest,
    { caseId: massImportCase.id },
  );

  const b = await test.step('Old contract B — 1 POD, ACTIVE_IN_TERM (Playwright only)', async () => {
    const result = await createContractBPrep(Request, GeneratePayload, Responses, Endpoints);
    const bBody = await loadProductContract(Request, result.contractId);
    const bp = bBody.basicParameters as { contractNumber?: string; status?: string };
    expect(bp.status).toBe('ACTIVE_IN_TERM');
    return { ...result, contractNumber: bp.contractNumber ?? 'EPES3620000001' };
  });

  const manifestPath = await test.step(
    `Write prep-manifest.json for mass-import case ${massImportCase.id}`,
    async () =>
      writeMassImportPrepManifestForCase(Request, Endpoints, Responses, massImportCase, {
        contractBId: b.contractId,
        contractNumber: b.contractNumber,
        podIdentifier: b.podIdentifier,
        podId: b.podId,
        podDetailId: b.podDetailId,
      }),
  );

  const { excelPath, fContractNumber } = await finalizeMassImportExcelArtifactStep(massImportCase.id);

  return {
    contractBId: b.contractId,
    contractNumber: b.contractNumber,
    podIdentifier: b.podIdentifier,
    podId: b.podId,
    podDetailId: b.podDetailId,
    productStdId: productIdFromResponses(Responses.product[IDX_PRODUCT_STD]),
    productRsId: productIdFromResponses(Responses.product[IDX_PRODUCT_RS]),
    manifestPath,
    excelPath,
    fContractNumber,
    massImportCase,
  };
}

export async function writeMassImportPrepManifestForCase(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  Responses: FixtureResponses,
  massImportCase: MassImportCaseDef,
  input: WriteMassImportManifestInput,
): Promise<string> {
  let contractNumber = input.contractNumber;
  if (!contractNumber) {
    const bBody = await loadProductContract(Request, input.contractBId);
    const bp = bBody.basicParameters as { contractNumber?: string };
    contractNumber = bp.contractNumber ?? 'EPES3620000001';
  }

  const customer = await resolveCustomerForManifest(Request, Endpoints, Responses);
  const { pathToFileURL } = await import('url');
  const { buildPrepManifest, writePrepManifest } = await import(
    pathToFileURL(`${MASS_IMPORT_PHN362_DIR}/prep-manifest-writer.mjs`).href
  );

  const manifest = buildPrepManifest({
    runtimeToday: PREP_RUNTIME_TODAY,
    contractB: {
      id: input.contractBId,
      contractNumber,
      signingDate: PREP_B_SIGNING_DATE,
    },
    customer,
    pod: {
      id: input.podId,
      identifier: input.podIdentifier,
      detailId: input.podDetailId,
    },
    products: {
      stdId: productIdFromResponses(Responses.product[IDX_PRODUCT_STD]),
      rsId: productIdFromResponses(Responses.product[IDX_PRODUCT_RS]),
      version: 1,
    },
    terms: {
      stdId: Responses.terms[0].id,
      rsId: Responses.terms[1].id,
    },
    massImportCase,
  });

  const manifestDir = await resolveCaseManifestDir(massImportCase.id);
  return writePrepManifest(manifest, manifestDir);
}

/** Shared Playwright report attachment for mass-import old-contract prep (cases 01–06). */
export function attachMassImportOldContractPrepSummary(
  testInfo: import('@playwright/test').TestInfo,
  prep: MassImportOldContractPrepResult,
  customerManifest: Awaited<ReturnType<typeof resolveCustomerForManifest>>,
  trace: unknown,
  meta?: { caseSlug?: string; caseTitle?: string },
): void {
  const massImportCase = prep.massImportCase;
  testInfo.attach(`[PHN-362] Mass-import case ${massImportCase.id} — prep-manifest.json summary`, {
    body: JSON.stringify(
      {
        runtimeToday: PREP_RUNTIME_TODAY,
        manifestPath: prep.manifestPath,
        outputDir: MASS_IMPORT_OUTPUT_DIR,
        massImportCase,
        caseSlug: meta?.caseSlug,
        caseTitle: meta?.caseTitle,
        contractB: {
          id: prep.contractBId,
          contractNumber: prep.contractNumber,
          signingDate: PREP_B_SIGNING_DATE,
          status: 'ACTIVE_IN_TERM',
          productStdId: prep.productStdId,
        },
        massImportF: {
          productRsId: prep.productRsId,
          contractNumber: prep.fContractNumber,
          waitForOld: massImportCase.waitForOld,
          supplyAfterResign: massImportCase.supplyAfterResign,
          excelFile: massImportCase.file,
          excelPath: prep.excelPath,
        },
        customer: customerManifest,
        pod: {
          identifier: prep.podIdentifier,
          podId: prep.podId,
          podDetailId: prep.podDetailId,
        },
        nextStep: `Upload ${prep.excelPath} (updated on this Playwright run)`,
        trace,
      },
      null,
      2,
    ),
    contentType: 'application/json',
  });
}
