/**
 * PHN-362 — Write prep-manifest.json after Playwright prep (contract B ACTIVE_IN_TERM).
 * Used by resign-precondition and mass-import-prep specs → generate-resign-flow-imports.mjs.
 */

import { expect, test } from '../../fixtures/baseFixture';
import type { baseFixture } from '../../fixtures/baseFixture';
import { PREP_B_SIGNING_DATE, PREP_RUNTIME_TODAY } from './phn-362-resign-two-pods-precondition.fixtures';
import { loadProductContract } from './pdt-2815-version-validity.fixtures';

export const MASS_IMPORT_PHN362_DIR =
  'c:/Users/k.nanuashvili/Desktop/tasks/mass import/phn-362';

export const MASS_IMPORT_OUTPUT_DIR =
  'c:/Users/k.nanuashvili/Desktop/tasks/mass import/phn-362-resign-flows';

type FixtureRequest = baseFixture['Request'];
type FixtureResponses = baseFixture['Responses'];
type FixtureEndpoints = baseFixture['Endpoints'];

export function productIdFromResponses(entry: number | { id?: number }): number {
  return typeof entry === 'number' ? entry : Number(entry.id);
}

export async function resolveCustomerForManifest(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  Responses: FixtureResponses,
): Promise<{ id: number; identifier: string; versionId: number; detailId?: number }> {
  const posted = Responses.customer[0] as {
    id?: number;
    customerId?: number;
    identifier?: string;
    versionId?: number;
    lastCustomerDetailId?: number;
  };

  let id = Number(posted.customerId ?? posted.id);
  let identifier = posted.identifier != null ? String(posted.identifier) : undefined;
  let versionId = posted.versionId;
  let detailId = posted.lastCustomerDetailId;

  if (!identifier || versionId == null) {
    const custGet = await Request.get(`${Endpoints.customer}/${id}`);
    await expect(custGet).CheckResponse();
    const body = (await custGet.json()) as {
      id?: number;
      customerId?: number;
      identifier?: string;
      versionId?: number;
      customerDetailsVersion?: number;
      lastCustomerDetailId?: number;
    };
    id = Number(body.customerId ?? body.id ?? id);
    identifier = String(body.identifier ?? identifier);
    versionId = Number(body.versionId ?? body.customerDetailsVersion ?? versionId ?? 1);
    detailId = body.lastCustomerDetailId ?? detailId;
  }

  return {
    id,
    identifier: String(identifier),
    versionId: Number(versionId ?? 1),
    detailId,
  };
}

export type WriteMassImportManifestInput = {
  contractBId: number;
  contractNumber?: string;
  podIdentifier: string;
  podId: number;
  podDetailId: number;
};

/** Writes prep-manifest.json; returns file path. */
export async function writeMassImportPrepManifest(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  Responses: FixtureResponses,
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
      stdId: productIdFromResponses(Responses.product[0]),
      rsId: productIdFromResponses(Responses.product[1]),
      version: 1,
    },
    terms: {
      stdId: Responses.terms[0].id,
      rsId: Responses.terms[1].id,
    },
  });

  return writePrepManifest(manifest, MASS_IMPORT_OUTPUT_DIR);
}

export async function writeMassImportPrepManifestStep(
  Request: FixtureRequest,
  Endpoints: FixtureEndpoints,
  Responses: FixtureResponses,
  input: WriteMassImportManifestInput,
): Promise<string> {
  return test.step('Write prep-manifest.json for mass-import Excel 01–06', async () =>
    writeMassImportPrepManifest(Request, Endpoints, Responses, input),
  );
}

async function loadTasksMassImportSetup() {
  const { pathToFileURL } = await import('url');
  return import(pathToFileURL(`${MASS_IMPORT_PHN362_DIR}/mass-import-setup.mjs`).href) as Promise<{
    freshFContractNumber: (caseId: string) => string;
    patchPrepManifestFContractNumber: (caseId: string, contractNumber: string) => string;
    getAnyCaseById: (caseId: string) => { contractNumberKey: string };
    resolveCaseManifestDir: (caseId: string) => string;
  }>;
}

async function resolveManifestDirForCase(caseId: string): Promise<string> {
  const setup = await loadTasksMassImportSetup();
  return setup.resolveCaseManifestDir(caseId);
}

/**
 * After prep-manifest.json: assign fresh F contract number (unless kept) and regenerate local .xlsx.
 * Called automatically at the end of every mass-import Playwright prep run.
 */
export async function finalizeMassImportExcelArtifact(caseId: string): Promise<{
  excelPath: string;
  fContractNumber: string;
}> {
  const { pathToFileURL } = await import('url');
  const setup = await loadTasksMassImportSetup();
  const manifestDir = await resolveManifestDirForCase(caseId);
  const keepF = process.env.MASS_IMPORT_KEEP_F_CONTRACT_NUMBER === '1';

  let fContractNumber: string;
  if (process.env.MASS_IMPORT_F_CONTRACT_NUMBER?.trim()) {
    fContractNumber = process.env.MASS_IMPORT_F_CONTRACT_NUMBER.trim();
    setup.patchPrepManifestFContractNumber(caseId, fContractNumber);
  } else if (keepF) {
    const { loadPrepManifest } = await import(
      pathToFileURL(`${MASS_IMPORT_PHN362_DIR}/prep-manifest-writer.mjs`).href
    );
    const manifest = loadPrepManifest(manifestDir) as {
      contractNumbers?: Record<string, string>;
    } | null;
    const key = setup.getAnyCaseById(caseId).contractNumberKey;
    fContractNumber = manifest?.contractNumbers?.[key] ?? 'EPES3620000011';
  } else {
    fContractNumber = setup.freshFContractNumber(caseId);
    setup.patchPrepManifestFContractNumber(caseId, fContractNumber);
  }

  const excelPath = await generateExcelFromPrepManifest(caseId, fContractNumber, manifestDir);
  return { excelPath, fContractNumber };
}

/** Generate .xlsx from prep-manifest.json using the same entities/dates Playwright wrote. */
export async function generateExcelFromPrepManifest(
  caseId: string,
  contractNumberOverride?: string,
  manifestDir: string = MASS_IMPORT_OUTPUT_DIR,
): Promise<string> {
  const { pathToFileURL } = await import('url');
  const { generateFromPrepManifest } = await import(
    pathToFileURL(`${MASS_IMPORT_PHN362_DIR}/generate-resign-flow-imports.mjs`).href
  );

  const result = await generateFromPrepManifest({
    caseId,
    contractNumberOverride: contractNumberOverride ?? process.env.MASS_IMPORT_F_CONTRACT_NUMBER ?? null,
    flowsRoot: MASS_IMPORT_OUTPUT_DIR,
    manifestDir,
  });

  const entry = result.generated.find((g: { id: string }) => g.id === caseId) ?? result.generated[0];
  if (!entry?.xlsxPath) {
    throw new Error(`Excel not generated for case ${caseId}`);
  }
  return entry.xlsxPath as string;
}

export async function generateExcelFromPrepManifestStep(
  caseId: string,
  contractNumberOverride?: string,
): Promise<string> {
  return test.step(`Generate Excel case ${caseId} from prep-manifest.json (Playwright data)`, async () =>
    generateExcelFromPrepManifest(caseId, contractNumberOverride),
  );
}

export async function finalizeMassImportExcelArtifactStep(caseId: string): Promise<{
  excelPath: string;
  fContractNumber: string;
}> {
  return test.step(
    `Update local mass-import Excel case ${caseId} from prep-manifest.json (fresh F number + entities)`,
    async () => finalizeMassImportExcelArtifact(caseId),
  );
}
