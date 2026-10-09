/**
 * PHN-362 mass-import — shared Playwright helpers (cases 02–06).
 * Case 01: PHN-362-mass-import-old-contract.spec.ts (unchanged, uses its own flow).
 */

import { test, type baseFixture } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import {
  attachMassImportOldContractPrepSummary,
  runMassImportOldContractPrep,
} from './phn-362-mass-import-old-contract.fixtures';
import { resolveCustomerForManifest } from './phn-362-mass-import-manifest.fixtures';

export const DEV2_RISKLIST_CUSTOMER_ID = process.env.MASS_IMPORT_RISKLIST_CUSTOMER_ID ?? '6025823';
export const DEV2_RISKLIST_UIC = process.env.MASS_IMPORT_RISKLIST_UIC ?? '88888';

export function applyMassImportRiskListEnv(): void {
  process.env.MASS_IMPORT_RISKLIST_CUSTOMER_ID ??= DEV2_RISKLIST_CUSTOMER_ID;
  process.env.MASS_IMPORT_RISKLIST_UIC ??= DEV2_RISKLIST_UIC;
}

export type MassImportCasePrepFixtures = Pick<
  baseFixture,
  'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'FileUploadRequest'
>;

/**
 * Full prep for one mass-import case: entities + contract B + case manifest + local Excel.
 * Each case id uses its own prep-manifest.json under cases/{folder}/ (02–06).
 */
export async function runPhn362MassImportCasePrepTest(
  caseId: string,
  caseSlug: string,
  caseTitle: string,
  fixtures: MassImportCasePrepFixtures,
): Promise<void> {
  test.setTimeout(25 * 60 * 1000);
  test.info().annotations.push({ type: 'jira', description: 'PHN-362' });
  test.info().annotations.push({ type: 'workflow', description: 'mass-import-old-contract' });
  test.info().annotations.push({ type: 'mass-import-case', description: caseId });

  const prep = await runMassImportOldContractPrep(
    fixtures.Request,
    fixtures.GeneratePayload,
    fixtures.Responses,
    fixtures.Endpoints,
    fixtures.FileUploadRequest,
    { caseId },
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
