/**

 * PDT-3454 — Creating New Version +1 Case (bug trigger only)

 *

 * REAL bug: PERIOD recalc from old initialTermDate during async CREATE.

 * CustomerMapperService does not copy header contractTermEndDate → null →

 * setContractTermEndDate recalculates termEnd = initialTerm + PERIOD − 1.

 * When that is < activationDate, validateContractTermEndDate throws in

 * ProductContractService.edit (~368) before adjustPodActivation — no +1 version.

 *

 * NOT the bug: D > header contractTermEndDate alone (89424 / 89328-style fresh

 * contracts with initialTerm ≈ today recalc termEnd ≥ activation → CREATE works).

 *

 * Setup: PERIOD 12 DAY_DAYS product; version start + signing + startOfInitialTerm

 * = pastInitialTerm (today−400); POD activation = today; header termEnd ≥ activation

 * (long/safe — do NOT force T < D). Apply with newContractsVersionsStartDate = D.

 *

 * Spec still requires Open-end CREATE +1. On current Dev the poll must FAIL (bug).

 * If CREATE unexpectedly succeeds, fail with a clear "bug was not reproduced" message.

 *

 * Backend TC bridge: TC-BE-2 only in

 * Cursor-Project/test_cases/Backend/PDT_3454_Open_End_Contract_New_Version.md

 *

 * Reference spec(s):

 * - tests/cursor/PDT-3330-product-new-version-apply-two-stage-contract-update.spec.ts

 * - tests/cursor/pdt-3330-product-new-version-apply-two-stage-contract-update.fixtures.ts

 * - tests/cursor/pdt-2815-version-validity.fixtures.ts

 * - tests/cursor/pdt-2906-product-new-version-contract-update.fixtures.ts

 *

 * Swagger (dev) validated:

 * - PUT /products/{id} (ProductEditRequest: updateExistingVersion, productDetailIdsForUpdatingProductContracts, newContractsVersionsStartDate)

 * - POST /products/validate-product-related-contract-update

 * - GET /products/{id}?version=

 * - POST /product-contract, GET/PUT /product-contract/{id}?versionId=&changeFutureVersionsPods=

 * - PUT /product-contract/status-update/{id}?versionId= (READY→SIGNED after pin PUT)

 * - POST /contract-pods/manual (PodManualActivationRequest)

 *

 * Date aliases: todayDate, D=today+10, DMinus1=D-1, pastInitialTerm=today−400,

 * expectedPeriodRecalcTermEnd=pastInitialTerm+11.

 * Async CREATE is AFTER_COMMIT — poll GET /product-contract/{id} up to 120s; PUT 200 is not a pass.

 */



import { test, expect } from './cursor-test.fixtures';

import {

  buildProductContractTabLinks,

  finalizeTestRunSummary,

} from './shared/manual-verification-links.fixtures';

import {

  PDT_3454_PERIOD_DAY_DAYS,

  PDT_3454_RELEVANT_ENTITIES,

  addDaysIso,

  applyActivatedPodWithPastInitialTermPeriodBug,

  buildPdt3330ProductEditPayload,

  contractVersionRows,

  createPdt3330BaseChain,

  createSignedProductContractOnIndices,

  entityIdFromResponses,

  findVersionByStartDate,

  justCreatedChainIndices,

  loadProductContract,

  loadProductDetailSnapshot,

  loadVersionProductDetailId,

  pdt3454DateAliases,

  pdt3454TestTitle,

  pollUntilContractVersionStartDate,

  putProductEdit,

  validateProductRelatedContractUpdate,

} from './pdt-3454-open-end-contract-new-version.fixtures';



test.describe('[PDT-3454]: Creating New Version +1 Case', { tag: '@productAndServices' }, () => {

  test(

    pdt3454TestTitle(

      'TC-BE-2',

      'Open-end +1 when PERIOD recalc from past initialTerm < activation (not D>termEnd)',

    ),

    async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {

      test.setTimeout(20 * 60 * 1000);

      const dates = pdt3454DateAliases();

      let productId = 0;

      let productDetailV1Id = 0;

      let productIndex = 0;

      let contractId = 0;

      let contractPayload: Record<string, unknown> = {};

      let openEndVersionId = 0;

      let versionCountBefore = 0;

      let headerTermEnd: string | null = null;

      let headerActivation: string | null = null;

      let effectiveInitialTerm: string | null = null;

      let versionStartDate: string | null = null;



      await test.step(

        'Precondition: signed Open End + past initialTerm + POD activated today (PERIOD recalc bug)',

        async () => {

          const base = await createPdt3330BaseChain(

            Request,

            GeneratePayload,

            Responses,

            Endpoints,

            TestRunSummary,

          );

          productId = base.productId;

          productDetailV1Id = base.productDetailV1Id;

          productIndex = base.productIndex;



          const idx = justCreatedChainIndices(Responses, productIndex);

          const created = await createSignedProductContractOnIndices(

            Request,

            GeneratePayload,

            Responses,

            Endpoints,

            idx.customerIndex,

            idx.productIndex,

            idx.podIndex,

          );

          contractId = created.contractId;

          contractPayload = created.contractPayload;

          TestRunSummary.registerPayload('productContract', created.contractPayload);

          const pinnedBp = created.contractPayload.basicParameters as {

            customerId?: number;

            productId?: number;

            productDetailId?: number;

          };

          expect(Number(pinnedBp.customerId)).toBe(

            entityIdFromResponses(Responses.customer[idx.customerIndex] as { id?: number }, 'customer'),

          );

          expect(Number(pinnedBp.productId)).toBe(productId);

          expect(Number(pinnedBp.productDetailId)).toBe(productDetailV1Id);



          const bodyBeforePin = await loadProductContract(Request, contractId);

          openEndVersionId = contractVersionRows(bodyBeforePin)[0].versionId;



          const header = await applyActivatedPodWithPastInitialTermPeriodBug({

            Request,

            Endpoints,

            Responses,

            contractId,

            contractPayload,

            openEndVersionId,

            todayDate: dates.todayDate,

            D: dates.D,

            pastInitialTerm: dates.pastInitialTerm,

            expectedPeriodRecalcTermEnd: dates.expectedPeriodRecalcTermEnd,

            podIndex: idx.podIndex,

          });

          headerTermEnd = header.contractTermEndDate;

          headerActivation = header.activationDate;

          effectiveInitialTerm = header.effectiveInitialTerm;

          versionStartDate = header.versionStartDate;



          const body = await loadProductContract(Request, contractId);

          const rows = contractVersionRows(body);

          versionCountBefore = rows.length;

          const open = rows.find((r) => r.endDate == null) ?? rows[0];

          openEndVersionId = open.versionId;

          expect(open.endDate, 'TC-BE-2 version stays Open End').toBeNull();

          expect(open.startDate < dates.D, `version startDate ${open.startDate} must be before D=${dates.D}`).toBe(

            true,

          );

          expect(headerActivation, 'POD/header activationDate must be set (activated POD)').toBeTruthy();

          expect(effectiveInitialTerm, 'effective initialTerm (past) must be set').toBe(dates.pastInitialTerm);

          expect(versionStartDate, 'version start must equal pastInitialTerm').toBe(dates.pastInitialTerm);



          const periodRecalc = addDaysIso(effectiveInitialTerm!, PDT_3454_PERIOD_DAY_DAYS - 1);

          expect(

            periodRecalc < headerActivation!,

            `BUG TRIGGER: initialTerm ${effectiveInitialTerm} + ${PDT_3454_PERIOD_DAY_DAYS} − 1 = ${periodRecalc} ` +

              `must be < activation ${headerActivation} (PERIOD recalc would fail validateContractTermEndDate). ` +

              `Do not use fresh initialTerm≈today (89424 happy path).`,

          ).toBe(true);



          if (headerTermEnd != null && headerActivation != null) {

            expect(

              headerTermEnd >= headerActivation,

              'before apply: header term-end must stay ≥ activation (setup PUT path; NOT the bug trigger)',

            ).toBe(true);

          }

        },

      );



      let productDetailV2Id = 0;

      let afterBody: Record<string, unknown> = {};

      let attachDone = false;



      const attachSummary = (extra: Record<string, unknown> = {}) => {

        if (attachDone) return;

        attachDone = true;

        finalizeTestRunSummary(TestRunSummary, Responses, {

          jiraKey: 'PDT-3454',

          relevantEntityKeys: [...PDT_3454_RELEVANT_ENTITIES],

          extraLinks: buildProductContractTabLinks(contractId),

          snapshot: {

            productId,

            contractId,

            productDetailV1Id,

            productDetailV2Id,

            openEndVersionId,

            headerTermEnd,

            headerActivation,

            effectiveInitialTerm,

            versionStartDate,

            pastInitialTerm: dates.pastInitialTerm,

            expectedPeriodRecalcTermEnd: dates.expectedPeriodRecalcTermEnd,

            D: dates.D,

            bugTrigger: 'PERIOD recalc from past initialTerm < activation',

            ...extra,

          },

        });

      };



      await test.step('Validate + PUT product (PUT 200 is not a pass)', async () => {

        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {

          baseVersion: 1,

          updateExistingVersion: false,

          shortDescription: `PDT-3454-TC-BE-2-${Date.now()}`,

          productDetailIds: [productDetailV1Id],

          newContractsVersionsStartDate: dates.D,

        });

        TestRunSummary.registerPayload('productEdit', editPayload);



        const validate = await validateProductRelatedContractUpdate(Request, Endpoints, editPayload);

        expect(validate.status).toBe(200);

        expect(validate.eligible).toBe(true);



        const putRes = await putProductEdit(Request, Endpoints, productId, editPayload);

        await expect(putRes).CheckResponse();



        const v2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);

        expect(v2.version).toBe(2);

        productDetailV2Id = v2.detailId;

      });



      await test.step('Poll GET contract until new version startDate=D (spec CREATE)', async () => {

        try {

          afterBody = await pollUntilContractVersionStartDate(Request, contractId, dates.D);

        } catch (err) {

          afterBody = await loadProductContract(Request, contractId);

          const versionCountAfter = contractVersionRows(afterBody).length;

          TestRunSummary.recordCheck({

            check: 'TC-BE-2 spec CREATE when PERIOD recalc from past initialTerm < activation',

            expectedResult:

              `PDT-3454 spec: Open End + start < D + POD activated; past initialTerm ${effectiveInitialTerm} ` +

              `with PERIOD ${PDT_3454_PERIOD_DAY_DAYS} DAY_DAYS (recalc end ${dates.expectedPeriodRecalcTermEnd} < ` +

              `activation ${headerActivation}) must NOT prevent CREATE. After poll: versionCount ` +

              `${versionCountBefore}+1; new startDate=${dates.D} endDate=null productDetailId=v2; ` +

              `prior endDate=${dates.DMinus1}.`,

            actualResult:

              `Not as expected — PUT 200 and product v2 stored, but no versions[] row with startDate=${dates.D} ` +

              `after 120s (count ${versionCountBefore}→${versionCountAfter}). Current Dev async ` +

              `ProductContractService.edit throws ClientException fragment ` +

              `"basicParameters.contractTermEndDate-Contract term end date should be more or equal to activation date" ` +

              `because mapper drops term-end and PERIOD recalc from past initialTerm lands before activation. ` +

              `Spec still requires +1 version; this test must fail until auto-edit is fixed. ` +

              `contractId=${contractId}; initialTerm=${effectiveInitialTerm}; activation=${headerActivation}; D=${dates.D}.`,

            passed: false,

          });

          attachSummary({ versionCountAfter, pollTimedOut: true, bugReproduced: true });

          throw err;

        }

      });



      await test.step('Assert CREATE outcome (spec +1; unexpected success = bug not reproduced)', async () => {

        const versionCountAfter = contractVersionRows(afterBody).length;

        const created = findVersionByStartDate(afterBody, dates.D);

        const prior = contractVersionRows(afterBody).find((r) => r.versionId === openEndVersionId);

        const createSucceeded =

          versionCountAfter === versionCountBefore + 1 &&

          created != null &&

          created.endDate == null;



        if (createSucceeded) {

          const newDetail =

            created != null

              ? await loadVersionProductDetailId(Request, contractId, created.versionId)

              : null;

          TestRunSummary.recordCheck({

            check: 'TC-BE-2 PERIOD-recalc bug reproduction gate',

            expectedResult:

              `On current Dev (bug unfixed), async CREATE must NOT produce +1 version when ` +

              `PERIOD recalc from past initialTerm ${effectiveInitialTerm} is < activation ${headerActivation}.`,

            actualResult:

              `Not as expected — CREATE +1 SUCCEEDED (count ${versionCountBefore}→${versionCountAfter}, ` +

              `newDetail=${newDetail}, priorEnd=${prior?.endDate}). PDT-3454 bug was NOT reproduced — ` +

              `setup likely used fresh initialTerm≈today (89424-style) or PERIOD recalc ≥ activation. ` +

              `contractId=${contractId}; pastInitialTerm=${dates.pastInitialTerm}; ` +

              `expectedRecalcEnd=${dates.expectedPeriodRecalcTermEnd}; activation=${headerActivation}; D=${dates.D}.`,

            passed: false,

          });

          attachSummary({

            versionCountAfter,

            createSucceeded: true,

            bugReproduced: false,

          });

          expect(

            false,

            `PDT-3454 bug was NOT reproduced: CREATE +1 succeeded (contractId=${contractId}, ` +

              `productId=${productId}). Expected async edit to fail with term-end < activation from ` +

              `PERIOD recalc of past initialTerm ${effectiveInitialTerm} (recalc end ` +

              `${dates.expectedPeriodRecalcTermEnd} < activation ${headerActivation}). ` +

              `Check setup dates — do not treat this as a green product fix without verifying ES/logs.`,

          ).toBe(true);

        }



        // Spec path (reachable after product fix + removal of the reproduction gate above):

        expect(versionCountAfter).toBe(versionCountBefore + 1);

        expect(created).toBeTruthy();

        expect(created!.endDate).toBeNull();

        expect(prior?.endDate).toBe(dates.DMinus1);

        const newDetail = await loadVersionProductDetailId(Request, contractId, created!.versionId);

        expect(newDetail).toBe(productDetailV2Id);



        TestRunSummary.recordCheck({

          check: 'TC-BE-2 spec CREATE when PERIOD recalc from past initialTerm < activation',

          expectedResult:

            `Past initialTerm ${effectiveInitialTerm} + PERIOD recalc < activation must not prevent CREATE. ` +

            `versionCount ${versionCountBefore}+1; new start=${dates.D}; prior end=${dates.DMinus1}; productDetailId=v2.`,

          actualResult:

            `As expected — count ${versionCountBefore}→${versionCountAfter}; newDetail=${newDetail}; priorEnd=${prior?.endDate}.`,

          passed:

            versionCountAfter === versionCountBefore + 1 &&

            created != null &&

            prior?.endDate === dates.DMinus1 &&

            newDetail === productDetailV2Id,

        });

      });



      await test.step('Attach test run summary', async () => {

        attachSummary();

      });

    },

  );

});


