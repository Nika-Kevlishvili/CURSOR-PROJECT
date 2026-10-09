import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import {
  PDT_3059_ADDITIONAL_PARAM_NOT_FOUND_PATTERN,
  evaluatePdt3059NewVersionOutcome,
  pdt3059BugExpectedOnEnv,
  putProductContractSavingAsNewVersion,
  resolvePdt3059ContractContext,
  resolvePdt3059Environment,
  type Pdt3059ContractContext,
  type Pdt3059CreateFx,
  type Pdt3059NewVersionPutResult,
} from './pdt-3059-product-contract-new-version-individual-product.fixtures';

/**
 * PDT-3059 — Individual product contract new version fails when additional param IDs are stale.
 *
 * Reference spec(s):
 * - tests/cursor/pdt-3035-contract-mass-import.fixtures.ts (GET→PUT mapping)
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (contract new version PUT)
 * - tests/cursor/PDT-2906-product-new-version-contract-update.spec.ts (product version flows)
 */

test.describe(
  '[PDT-3059]: Product contract - create a new version of a contract with individual product',
  { tag: ['@pdt-3059', '@productContract', '@test', '@dev'] },
  () => {
    test(
      '[PDT-3059]: Product contract - create a new version of a contract with individual product',
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
        test.setTimeout(15 * 60 * 1000);

        const fx: Pdt3059CreateFx = { Request, GeneratePayload, Responses, Endpoints };
        const env = resolvePdt3059Environment();

        let contractCtx: Pdt3059ContractContext;
        await test.step(
          'Precondition: fresh individual product contract with additional params (known contract fallback only)',
          async () => {
            contractCtx = await resolvePdt3059ContractContext(fx);
            TestRunSummary.registerPayload('contractPrecondition', {
              env,
              source: contractCtx.source,
              contractId: contractCtx.contractId,
              contractNumber: contractCtx.contractNumber,
              productId: contractCtx.productId,
              latestVersionId: contractCtx.latestVersionId,
              additionalParamIds: contractCtx.additionalParamIds,
            });
            expect(contractCtx.additionalParamIds.length).toBeGreaterThan(0);
          },
        );

        let putResult: Pdt3059NewVersionPutResult;
        await test.step(
          'Action: PUT product-contract savingAsNewVersion=true (mapped from GET — stale additional param IDs)',
          async () => {
            putResult = await putProductContractSavingAsNewVersion(
              Request,
              contractCtx!.contractId,
              {
                staleAdditionalParamIds: contractCtx!.additionalParamIds,
                additionalParamValues: contractCtx!.additionalParamValues,
              },
            );
            TestRunSummary.registerPayload('newVersionPut', {
              versionId: putResult.putVersionId,
              startDate: putResult.startDate,
              savingAsNewVersion: true,
              staleAdditionalParamIds: putResult.staleAdditionalParamIds,
              httpStatus: putResult.status,
              versionIdBefore: putResult.versionIdBefore,
              versionIdAfter: putResult.versionIdAfter,
              versionCountBefore: putResult.versionCountBefore,
              versionCountAfter: putResult.versionCountAfter,
              versionActuallyCreated: putResult.versionActuallyCreated,
            });
          },
        );

        const outcome = evaluatePdt3059NewVersionOutcome(putResult!, env);

        await test.step(
          'Assert: Test=repro (additional param id not found); Dev=HTTP OK + new version created',
          async () => {
            TestRunSummary.recordCheck({
              check: 'Save contract as new version with individual product additional params',
              expectedResult: outcome.expectedResult,
              actualResult: `${outcome.actualResult}; body=${putResult!.bodyText.slice(0, 400)}`,
              passed: outcome.passed,
            });

            if (!pdt3059BugExpectedOnEnv(env)) {
              TestRunSummary.recordCheck({
                check: 'New contract version created after savingAsNewVersion PUT',
                expectedResult:
                  `latestLogicalVersionId increases by 1 (${putResult!.versionIdBefore}+1) ` +
                  `or versions.length increases by 1 (${putResult!.versionCountBefore}+1)`,
                actualResult:
                  `versionId ${putResult!.versionIdBefore}→${putResult!.versionIdAfter}, ` +
                  `count ${putResult!.versionCountBefore}→${putResult!.versionCountAfter}, ` +
                  `created=${putResult!.versionActuallyCreated}`,
                passed: putResult!.versionActuallyCreated,
              });
            }

            if (pdt3059BugExpectedOnEnv(env)) {
              expect(putResult!.status, 'bug path expects HTTP error').toBeGreaterThanOrEqual(400);
              expect(putResult!.bodyText).toMatch(PDT_3059_ADDITIONAL_PARAM_NOT_FOUND_PATTERN);
              expect(putResult!.bodyText.toLowerCase()).toContain('not found');
            } else {
              expect(
                putResult!.status,
                `Dev success path expects HTTP 2xx; body=${putResult!.bodyText.slice(0, 400)}`,
              ).toBeGreaterThanOrEqual(200);
              expect(putResult!.status).toBeLessThan(300);
              expect(
                putResult!.versionActuallyCreated,
                `PUT ${putResult!.status} but version not created: ` +
                  `versionId ${putResult!.versionIdBefore}→${putResult!.versionIdAfter}, ` +
                  `count ${putResult!.versionCountBefore}→${putResult!.versionCountAfter}; ` +
                  `body=${putResult!.bodyText.slice(0, 400)}`,
              ).toBe(true);
              expect(outcome.passed, outcome.actualResult).toBe(true);
            }
          },
        );

        await test.step('Attach test run summary', async () => {
          finalizeTestRunSummary(TestRunSummary, Responses, {
            jiraKey: 'PDT-3059',
            relevantEntityKeys: ['customer', 'pod', 'product', 'productContract'],
            extraLinks: buildProductContractTabLinks(contractCtx!.contractId),
            snapshot: {
              env,
              contractId: contractCtx!.contractId,
              contractNumber: contractCtx!.contractNumber,
              productId: contractCtx!.productId,
              putStatus: putResult!.status,
              preconditionSource: contractCtx!.source,
              staleAdditionalParamIds: putResult!.staleAdditionalParamIds,
              versionIdBefore: putResult!.versionIdBefore,
              versionIdAfter: putResult!.versionIdAfter,
              versionCountBefore: putResult!.versionCountBefore,
              versionCountAfter: putResult!.versionCountAfter,
              versionActuallyCreated: putResult!.versionActuallyCreated,
            },
          });
        });
      },
    );
  },
);
