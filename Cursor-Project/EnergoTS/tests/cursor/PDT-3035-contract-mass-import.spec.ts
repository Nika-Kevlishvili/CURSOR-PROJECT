import { test, expect, finalizeTestRunSummary } from './cursor-test.fixtures';

import {
  PDT_3035_EIF_ERROR,
  PDT_3035_EIF_PATTERN,
  PDT_3035_NPE_PATTERNS,
  PDT_3035_PROD_BASELINE,
  PDT_3035_PROD_PROCESS_ID,
  buildProd2114IdenticalMinimalEdit,
  buildLegacyPdt3035NpeMassImportExcel,
  buildSingleRowMinimalEdit,
  createFreshPdt3035EifContract,
  createFreshPdt3035ManagerIdsContract,
  pdt3035BugExpectedForKind,
  evaluatePdt3035MassImportOutcome,
  resolveLegacyPdt3035NpeContract,
  resolvePdt3035Environment,
  uploadProductContractMassImportAndReadReport,
  type LegacyPdt3035NpeContract,
  type FreshPdt3035Contract,
  type MinimalEditRow,
  type Pdt3035CreateFx,
} from './pdt-3035-contract-mass-import.fixtures';

/**
 * TC-BE-1: legacy null until-term on Test (bug repro) or fresh contract regression (post-fix).
 * TC-BE-2/3: fresh data per run.
 *
 * Test env: assert bug reproduces. Dev env (post-fix): assert import succeeds.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3013-separate-pod-reversal-offset.fixtures.ts
 * - tests/cursor/pdt-2872-minimal-interim-payment.fixtures.ts
 */

test.describe(
  '[PDT-3035]: Contract mass import - problems',
  { tag: ['@pdt-3035', '@massImport', '@dev', '@test'] },
  () => {
    test(
      '[PDT-3035]: Contract mass import - problems — TC-BE-1 NPE until-amount',
      async ({ Request, FileUploadRequest, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(15 * 60 * 1000);

        const fx: Pdt3035CreateFx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };
        const env = resolvePdt3035Environment();

        let legacyContract: LegacyPdt3035NpeContract;
        let minimalRow: MinimalEditRow;
        let excelPath = '';

        await test.step(
          'Precondition: contract for NPE (legacy repro or fresh regression post-fix)',
          async () => {
            legacyContract = await resolveLegacyPdt3035NpeContract(fx);
            TestRunSummary.registerPayload('legacyNpeContract', { ...legacyContract, env });
          },
        );

        await test.step('Precondition: minimal Excel (A,B,C,G,H; +O/MANUAL only when DB EIF empty)', async () => {
          const built = await buildLegacyPdt3035NpeMassImportExcel(fx, legacyContract!);
          excelPath = built.excelPath;
          minimalRow = built.row;
          TestRunSummary.registerPayload('massImportExcel', {
            contractNumber: minimalRow.contractNumber,
            productId: minimalRow.productId,
            productVersion: minimalRow.productVersion,
            legacySource: legacyContract!.legacySource,
            columns:
              legacyContract!.legacySource === 'prod-profile'
                ? 'A,B,C,G,H'
                : 'A,B,C,G,H,O,61(MANUAL)',
          });
        });

        let importResult;
        await test.step('Action: upload single-row mass import', async () => {
          importResult = await uploadProductContractMassImportAndReadReport(fx, excelPath);
        });

        const snapshot = importResult!.snapshot;
        const outcome = evaluatePdt3035MassImportOutcome(snapshot, 'npe', env);

        await test.step('Assert: NPE on Test / success on Dev (post-fix regression)', async () => {
          expect(importResult!.processId).toBeGreaterThan(0);

          const sampleNpeRow = snapshot.failedRows.find((row) =>
            PDT_3035_NPE_PATTERNS.test(row.errorText),
          );

          TestRunSummary.recordCheck({
            check: 'TC-BE-1 — NPE until-amount (Test=repro, Dev=fixed)',
            expectedResult: outcome.expectedResult,
            actualResult: `${outcome.actualResult}; sample: ${(sampleNpeRow?.errorText ?? '').slice(0, 240)}`,
            passed: outcome.passed,
          });

          if (pdt3035BugExpectedForKind('npe', env)) {
            expect(snapshot.npeCount, `Prod baseline npeCount=${PDT_3035_PROD_BASELINE.npeCount}`).toBeGreaterThanOrEqual(1);
            expect(sampleNpeRow?.errorText ?? '').toMatch(PDT_3035_NPE_PATTERNS);
          } else {
            expect(outcome.passed, outcome.actualResult).toBe(true);
          }
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3035',
            relevantEntityKeys: ['customer', 'pod', 'product', 'productContract'],
            snapshot: {
              tc: 'TC-BE-1',
              env,
              processId: importResult!.processId,
              contractNumber: minimalRow!.contractNumber,
              contractId: legacyContract!.contractId,
              npeCount: snapshot.npeCount,
              legacySource: legacyContract!.legacySource,
            },
          });
        });
      },
    );

    test(
      '[PDT-3035]: Contract mass import - problems — TC-BE-2 managerIds LEGAL_ENTITY',
      async ({ Request, FileUploadRequest, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(15 * 60 * 1000);

        const fx: Pdt3035CreateFx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };
        const env = resolvePdt3035Environment();

        let freshContract: FreshPdt3035Contract;
        let excelPath = '';

        await test.step('Precondition: fresh LEGAL_ENTITY contract (no proxy — Prod-like minimal row)', async () => {
          freshContract = await createFreshPdt3035ManagerIdsContract(fx);
          TestRunSummary.registerPayload('managerIdsContract', { ...freshContract, env });
        });

        await test.step('Precondition: minimal Excel (A,B,C,G,H only)', async () => {
          const built = await buildProd2114IdenticalMinimalEdit(fx, freshContract!.contractNumber);
          excelPath = built.excelPath;
          TestRunSummary.registerPayload('massImportExcel', {
            contractNumber: built.row.contractNumber,
            productId: built.row.productId,
            productVersion: built.row.productVersion,
            columns: 'A,B,C,G,H',
          });
        });

        let importResult;
        await test.step('Action: upload single-row mass import', async () => {
          importResult = await uploadProductContractMassImportAndReadReport(fx, excelPath);
        });

        const snapshot = importResult!.snapshot;
        const outcome = evaluatePdt3035MassImportOutcome(snapshot, 'managerIds', env);

        await test.step('Assert: managerIds error on Test / success on Dev (post-fix regression)', async () => {
          expect(importResult!.processId).toBeGreaterThan(0);

          TestRunSummary.recordCheck({
            check: 'TC-BE-2 — managerIds LEGAL_ENTITY (Test=repro, Dev=fixed)',
            expectedResult: outcome.expectedResult,
            actualResult: outcome.actualResult,
            passed: outcome.passed,
          });

          expect(outcome.passed, outcome.actualResult).toBe(true);
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3035',
            relevantEntityKeys: ['customer', 'pod', 'product', 'productContract'],
            snapshot: {
              tc: 'TC-BE-2',
              env,
              processId: importResult!.processId,
              contractNumber: freshContract!.contractNumber,
              contractId: freshContract!.contractId,
              managerIdsCount: snapshot.managerIdsCount,
              failedRowCount: snapshot.failedRowCount,
            },
          });
        });
      },
    );

    test(
      '[PDT-3035]: Contract mass import - problems — TC-BE-3 past EIF validation',
      async ({ Request, FileUploadRequest, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(15 * 60 * 1000);

        const fx: Pdt3035CreateFx = { Request, FileUploadRequest, GeneratePayload, Responses, Endpoints };
        const env = resolvePdt3035Environment();

        let freshContract: FreshPdt3035Contract;
        let minimalRow: MinimalEditRow;
        let excelPath = '';

        await test.step('Precondition: fresh contract with past entryInForceDate in DB', async () => {
          freshContract = await createFreshPdt3035EifContract(fx);
          TestRunSummary.registerPayload('eifContract', { ...freshContract, env });
        });

        await test.step('Precondition: minimal Excel (A,B,C,G,H; I=SIGNED when past EIF on ACTIVE contract)', async () => {
          const built = await buildSingleRowMinimalEdit(fx, freshContract!.contractNumber, {
            forceSignedStatusForPastEif: true,
          });
          excelPath = built.excelPath;
          minimalRow = built.row;
          TestRunSummary.registerPayload('massImportExcel', {
            contractNumber: minimalRow.contractNumber,
            entryInForceDate: minimalRow.entryInForceDate,
            status: minimalRow.status,
            columns: 'A,B,C,G,H',
          });
        });

        let importResult;
        await test.step('Action: upload single-row mass import', async () => {
          importResult = await uploadProductContractMassImportAndReadReport(fx, excelPath);
        });

        const snapshot = importResult!.snapshot;
        const outcome = evaluatePdt3035MassImportOutcome(snapshot, 'eif', env);

        await test.step('Assert: EIF future validation on Test / success on Dev (post-fix regression)', async () => {
          expect(importResult!.processId).toBeGreaterThan(0);

          const sampleEifRow = snapshot.failedRows.find((row) =>
            PDT_3035_EIF_PATTERN.test(row.errorText),
          );

          TestRunSummary.recordCheck({
            check: 'TC-BE-3 — past EIF (Test=repro, Dev=fixed)',
            expectedResult: outcome.expectedResult,
            actualResult: `${outcome.actualResult}; sample: ${sampleEifRow?.errorText ?? ''}`,
            passed: outcome.passed,
          });

          if (env === 'test') {
            expect(snapshot.eifCount).toBeGreaterThanOrEqual(1);
            expect(sampleEifRow?.errorText ?? '').toContain(PDT_3035_EIF_ERROR);
          } else {
            expect(outcome.passed, outcome.actualResult).toBe(true);
          }
        });

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3035',
            relevantEntityKeys: ['customer', 'pod', 'product', 'productContract'],
            snapshot: {
              tc: 'TC-BE-3',
              env,
              processId: importResult!.processId,
              contractNumber: minimalRow!.contractNumber,
              contractId: freshContract!.contractId,
              entryInForceDate: minimalRow!.entryInForceDate,
              eifCount: snapshot.eifCount,
            },
          });
        });
      },
    );
  },
);
