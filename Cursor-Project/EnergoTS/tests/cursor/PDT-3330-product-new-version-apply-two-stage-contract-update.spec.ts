/**
 * PDT-3330 — Additional logics: create product new version and apply to existing contracts
 * (two-stage Stage1 create / Stage2 replace driven by newContractsVersionsStartDate = D).
 *
 * PDT-3454 supersedes parent AC-7 for Open End Start < D: CREATE +1. After PUT,
 * one immediate GET /product-contract/{id}; no wait/poll. PUT HTTP 200 is NOT CREATE success.
 * Remaining AC-7 replace applies only when Open End startDate ≥ D (TC-BE-17 Start = D).
 *
 * Backend TCs: Cursor-Project/test_cases/Backend/Product_new_version_apply_two_stage_contract_update.md
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3454-open-end-contract-new-version.spec.ts (CREATE +1)
 * - tests/cursor/PDT-2906-product-new-version-contract-update.spec.ts
 * - tests/cursor/pdt-2906-product-new-version-contract-update.fixtures.ts
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts
 *
 * Swagger (dev) validated:
 * - PUT /products/{id} (ProductEditRequest: newContractsVersionsStartDate, productDetailIdsForUpdatingProductContracts, updateExistingVersion)
 * - POST /products/validate-product-related-contract-update
 * - GET /products/{id}?version=
 * - POST /product-contract (ProductContractProductParametersCreateRequest.invoicePaymentTermId required)
 * - GET/PUT /product-contract/{id}?versionId=
 * - PUT /product-contract/status-update/{id} (ProductContractEditStatusRequest)
 * - POST /product-contract/list
 *
 * Date aliases: todayDate, D=today+10, DMinus1=D-1, futureDateA=today+5, futureDateE=today+20, pastDate=today-5.
 * For End=D Stage1 setups (SIGNED Valid), following start = D+1 (dayAfterD) so prior endDate chains to D.
 * createFollowingContractVersion SIGNED path creates the new version as SIGNED on PUT so
 * CalculateVersionDates runs. DRAFT path creates following as DRAFT (no promote/demote);
 * End=D / End<D for Not Valid cannot be set up without forbidden SIGNED↔DRAFT — covered by TC-BE-1/2.
 * TC-BE-6: Valid two Open End — DRAFT-on-PUT following then status-update SIGNED (no chaining).
 * TC-BE-7: Draft two Open End — following stays DRAFT. Draft CREATE may not complete
 * (handler loads ACTIVE/DELETED only) — spec still requires +1; missing startDate=D on immediate GET FAILS (no wait).
 */

import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import {
  ERR_D_FIELD_REQUIRED,
  ERR_D_MUST_BE_TODAY_OR_FUTURE,
  PDT_3330_RELEVANT_ENTITIES,
  addDaysIso,
  assertOpenEndCreatePlusOne,
  buildPdt3330ProductEditPayload,
  contractVersionRows,
  createDraftProductContractOnIndices,
  createExtraCustomerAndPod,
  createFollowingContractVersion,
  createFollowingValidOpenEndKeepPriorOpen,
  createPdt3330BaseChain,
  createSignedProductContractOnIndices,
  createSignedProductContractV1,
  ensureVersionStartsBefore,
  ensureVersionStartEquals,
  errorBodyContains,
  findVersionByEndDate,
  findVersionByStartDate,
  loadContractVersionProductRef,
  loadProductContract,
  loadProductDetailSnapshot,
  loadVersionProductDetailId,
  pdt3330DateAliases,
  pdt3330TestTitle,
  productContractStatusUpdateFull,
  putProductEdit,
  validateProductRelatedContractUpdate,
} from './pdt-3330-product-new-version-apply-two-stage-contract-update.fixtures';

test.describe(
  '[PDT-3330]: Additional logics - Create product new version and apply it to existing contracts',
  { tag: '@productAndServices' },
  () => {
    // ─── TC-BE-1 ───────────────────────────────────────────────────────────
    // Setup only: leave contract ready for Stage1 (prior End=D, Start<D).
    // Product new version + apply is MANUAL (UI) — automation does not PUT /products.
    test(pdt3330TestTitle('TC-BE-1', 'Stage1 ready — End equals D (manual product version)'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};
      let priorVersionId = 0;
      let followingVersionId = 0;

      await test.step('Setup: product v1 + signed contract + following start dayAfterD (prior End=D)', async () => {
        const base = await createPdt3330BaseChain(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          TestRunSummary,
        );
        productId = base.productId;
        productDetailV1Id = base.productDetailV1Id;

        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;
        TestRunSummary.registerPayload('productContract', created.contractPayload);

        await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          1,
          dates.dayAfterD,
          'SIGNED',
        );

        const body = await loadProductContract(Request, contractId);
        const rows = contractVersionRows(body);
        expect(rows.length, 'Stage1 setup needs ≥2 contract versions (prior End=D + following)').toBeGreaterThanOrEqual(
          2,
        );

        const prior = findVersionByEndDate(body, dates.D);
        expect(prior, `prior version endDate must be D=${dates.D}`).toBeTruthy();
        expect(prior!.startDate < dates.D, 'prior startDate before D').toBe(true);
        priorVersionId = prior!.versionId;

        const following = findVersionByStartDate(body, dates.dayAfterD);
        expect(following, `following startDate must be dayAfterD=${dates.dayAfterD}`).toBeTruthy();
        followingVersionId = following!.versionId;
        expect(followingVersionId, 'following must be a distinct version from prior').not.toBe(priorVersionId);
      });

      await test.step('Ready for manual product Create New version + apply', async () => {
        TestRunSummary.recordCheck({
          check: 'TC-BE-1 contract ready for Stage1 (manual product version)',
          expectedResult:
            `TWO versions: prior End=${dates.D} Start<D; following start=${dates.dayAfterD} Open End. ` +
            `Manual: Product ${productId} → Create New version → select productDetailV1=${productDetailV1Id} → ` +
            `D=${dates.D} → Create. Expect NEW 3rd contract version startDate=${dates.D}; prior end→${addDaysIso(dates.D, -1)}.`,
          actualResult:
            `Setup OK — productId=${productId}, contractId=${contractId}, ` +
            `priorVersionId=${priorVersionId} (End=${dates.D}), followingVersionId=${followingVersionId} (start=${dates.dayAfterD}), ` +
            `productDetailV1Id=${productDetailV1Id}. Open contract Versions list (not only current header).`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productDetailV1Id,
            priorVersionId,
            followingVersionId,
            versionCount: 2,
            D: dates.D,
            dayAfterD: dates.dayAfterD,
            manualNext:
              'Create New product version; select productDetailV1Id; set D; confirm Create (Stage1 inserts version between prior and following)',
          },
        });
      });
    });

    // ─── TC-BE-2 ───────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-2', 'Stage1/2 skip when End before D'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};
      let endBeforeDVersionId = 0;
      let productDetailBefore = 0;
      let versionCountBefore = 0;

      await test.step('Precondition: signed contract + following start=D (prior end=D-1)', async () => {
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

        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;

        await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          1,
          dates.D,
          'SIGNED',
        );

        const body = await loadProductContract(Request, contractId);
        versionCountBefore = contractVersionRows(body).length;
        const prior = findVersionByEndDate(body, addDaysIso(dates.D, -1));
        expect(prior).toBeTruthy();
        endBeforeDVersionId = prior!.versionId;
        productDetailBefore = await loadVersionProductDetailId(
          Request,
          contractId,
          endBeforeDVersionId,
        );
      });

      let productDetailV2Id = 0;

      await test.step('Apply product v2 with D', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-2-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);

        const validate = await validateProductRelatedContractUpdate(Request, Endpoints, editPayload);
        expect(validate.eligible).toBe(true);
        const putRes = await putProductEdit(Request, Endpoints, productId, editPayload);
        await expect(putRes).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;
      });

      await test.step('Assert End-before-D row unchanged; Start=D row may be replaced', async () => {
        const body = await loadProductContract(Request, contractId);
        const priorDetail = await loadVersionProductDetailId(
          Request,
          contractId,
          endBeforeDVersionId,
        );
        expect(priorDetail, 'End before D row must not be replaced').toBe(productDetailBefore);

        const startD = findVersionByStartDate(body, dates.D);
        expect(startD).toBeTruthy();
        const startDDetail = await loadVersionProductDetailId(Request, contractId, startD!.versionId);
        expect(startDDetail, 'Start=D row Stage2 replace').toBe(productDetailV2Id);

        // No Stage1 clone from End-before-D source (version count may stay same or +0 from that row)
        const versionCountAfter = contractVersionRows(body).length;
        expect(versionCountAfter).toBe(versionCountBefore);

        TestRunSummary.recordCheck({
          check: 'TC-BE-2 End before D skip',
          expectedResult: 'Prior endDate<D unchanged productDetail; Start=D replaced; no Stage1 from prior.',
          actualResult: `As expected — priorDetail=${priorDetail}, startDDetail=${startDDetail}, count ${versionCountBefore}→${versionCountAfter}.`,
          passed:
            priorDetail === productDetailBefore &&
            startDDetail === productDetailV2Id &&
            versionCountAfter === versionCountBefore,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId, productDetailV2Id },
        });
      });
    });

    // ─── TC-BE-3 ───────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-3', 'Start equals D — replace only, no Stage1'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};
      let midVersionId = 0;
      let versionCountBefore = 0;

      await test.step('Precondition: mid start=D then late start=futureDateE', async () => {
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

        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;

        midVersionId = await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          1,
          dates.D,
          'SIGNED',
        );
        await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          midVersionId,
          dates.futureDateE,
          'SIGNED',
        );

        const body = await loadProductContract(Request, contractId);
        versionCountBefore = contractVersionRows(body).length;
        const mid = findVersionByStartDate(body, dates.D);
        expect(mid).toBeTruthy();
        expect(mid!.endDate).toBe(addDaysIso(dates.futureDateE, -1));
      });

      let productDetailV2Id = 0;

      await test.step('Apply product v2', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-3-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);

        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;
      });

      await test.step('Assert version count unchanged; mid Start=D replaced', async () => {
        const body = await loadProductContract(Request, contractId);
        const versionCountAfter = contractVersionRows(body).length;
        expect(versionCountAfter).toBe(versionCountBefore);

        const mid = findVersionByStartDate(body, dates.D)!;
        const midDetail = await loadVersionProductDetailId(Request, contractId, mid.versionId);
        expect(midDetail).toBe(productDetailV2Id);

        TestRunSummary.recordCheck({
          check: 'TC-BE-3 Start=D replace only',
          expectedResult: `versionCount unchanged; mid startDate=${dates.D} productDetailId=v2.`,
          actualResult: `As expected — count ${versionCountBefore}→${versionCountAfter}; midDetail=${midDetail}.`,
          passed: versionCountAfter === versionCountBefore && midDetail === productDetailV2Id,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId, productDetailV2Id, midVersionId },
        });
      });
    });

    // ─── TC-BE-4 ───────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-4', 'Open End Start before D creates +1 (PDT-3454)'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};
      let versionCountBefore = 0;
      let openEndVersionId = 0;
      let productDetailBefore = 0;

      await test.step('Precondition: signed Open End startDate < D', async () => {
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

        const created = await createSignedProductContractOnIndices(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          0,
          productIndex,
          0,
          1,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;
        TestRunSummary.registerPayload('productContract', created.contractPayload);

        const bodyBeforePin = await loadProductContract(Request, contractId);
        openEndVersionId = contractVersionRows(bodyBeforePin)[0].versionId;
        await ensureVersionStartsBefore(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          openEndVersionId,
          dates.D,
        );

        const body = await loadProductContract(Request, contractId);
        const rows = contractVersionRows(body);
        versionCountBefore = rows.length;
        const open = rows.find((r) => r.endDate == null) ?? rows[rows.length - 1];
        expect(open.endDate, 'TC-BE-4 version stays Open End').toBeNull();
        expect(open.startDate < dates.D, `startDate ${open.startDate} must be before D=${dates.D}`).toBe(
          true,
        );
        openEndVersionId = open.versionId;
        productDetailBefore = await loadVersionProductDetailId(Request, contractId, openEndVersionId);
      });

      let productDetailV2Id = 0;
      let afterBody: Record<string, unknown> = {};
      let attachDone = false;
      const attachSummary = (extra: Record<string, unknown> = {}) => {
        if (attachDone) return;
        attachDone = true;
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productDetailV2Id,
            openEndVersionId,
            productDetailBefore,
            D: dates.D,
            DMinus1: dates.DMinus1,
            ...extra,
          },
        });
      };

      await test.step('Apply product v2 with D (PUT 200 is not CREATE)', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-4-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;
      });

      await test.step('GET contract immediately after PUT (no wait)', async () => {
        afterBody = await loadProductContract(Request, contractId);
        const hasCreate = Boolean(findVersionByStartDate(afterBody, dates.D));
        if (!hasCreate) {
          const versionCountAfter = contractVersionRows(afterBody).length;
          TestRunSummary.recordCheck({
            check: 'TC-BE-4 Open End Start before D CREATE +1 (PDT-3454)',
            expectedResult:
              `CREATE +1: versionCount ${versionCountBefore}+1; new startDate=${dates.D} endDate=null ` +
              `productDetailId=v2 productVersionId=2; prior ${openEndVersionId} endDate=${dates.DMinus1} ` +
              `productDetailId stays ${productDetailBefore}. PUT 200 is not CREATE.`,
            actualResult:
              `Not as expected — GET after PUT has no versions[] row with startDate=${dates.D} ` +
              `(count ${versionCountBefore}→${versionCountAfter}).`,
            passed: false,
          });
          attachSummary({ versionCountAfter });
        }
        expect(
          findVersionByStartDate(afterBody, dates.D),
          `expected CREATE row startDate=${dates.D} on immediate GET`,
        ).toBeTruthy();
      });

      await test.step('Assert CREATE +1; prior product stays v1', async () => {
        const result = await assertOpenEndCreatePlusOne(Request, contractId, afterBody, {
          versionCountBefore,
          priorVersionId: openEndVersionId,
          productDetailBefore,
          expectedNewProductDetailId: productDetailV2Id,
          expectedNewProductVersionId: 2,
          D: dates.D,
          DMinus1: dates.DMinus1,
        });

        TestRunSummary.recordCheck({
          check: 'TC-BE-4 Open End Start before D CREATE +1 (PDT-3454)',
          expectedResult:
            `CREATE +1: count ${versionCountBefore}+1; new start=${dates.D} end=null productDetail=v2; ` +
            `prior end=${dates.DMinus1} productDetail stays v1.`,
          actualResult:
            `As expected — count ${versionCountBefore}→${result.versionCountAfter}; ` +
            `newDetail=${result.newDetail}; priorEnd=${result.prior.endDate}; priorDetail=${result.priorDetail}.`,
          passed:
            result.versionCountAfter === versionCountBefore + 1 &&
            result.newDetail === productDetailV2Id &&
            result.prior.endDate === dates.DMinus1 &&
            result.priorDetail === productDetailBefore,
        });
      });

      await test.step('Attach test run summary', async () => {
        attachSummary();
      });
    });

    // ─── TC-BE-5 ───────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-5', 'End after D — Stage2 replace only'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};
      let targetVersionId = 0;
      let versionCountBefore = 0;

      await test.step('Precondition: start=futureDateA, end=futureDateE', async () => {
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

        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;

        targetVersionId = await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          1,
          dates.futureDateA,
          'SIGNED',
        );
        await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          targetVersionId,
          addDaysIso(dates.futureDateE, 1),
          'SIGNED',
        );

        const body = await loadProductContract(Request, contractId);
        versionCountBefore = contractVersionRows(body).length;
        const target = findVersionByStartDate(body, dates.futureDateA);
        expect(target).toBeTruthy();
        expect(target!.endDate).toBe(dates.futureDateE);
        targetVersionId = target!.versionId;
      });

      let productDetailV2Id = 0;

      await test.step('Apply product v2', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-5-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;
      });

      await test.step('Assert target row replaced; count unchanged', async () => {
        const body = await loadProductContract(Request, contractId);
        expect(contractVersionRows(body).length).toBe(versionCountBefore);
        const detail = await loadVersionProductDetailId(Request, contractId, targetVersionId);
        expect(detail).toBe(productDetailV2Id);

        TestRunSummary.recordCheck({
          check: 'TC-BE-5 End after D replace',
          expectedResult: `Row start=${dates.futureDateA} end=${dates.futureDateE} → productDetail v2; count unchanged.`,
          actualResult: `As expected — detail=${detail}, count=${contractVersionRows(body).length}.`,
          passed: detail === productDetailV2Id,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId, productDetailV2Id, targetVersionId },
        });
      });
    });

    // ─── TC-BE-6 ───────────────────────────────────────────────────────────
    test(
      pdt3330TestTitle(
        'TC-BE-6',
        'Valid two Open End; prior Start before D CREATE +1; following Start after D replace',
      ),
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};
      let priorVersionId = 0;
      let followingVersionId = 0;
      let versionCountBefore = 0;
      let productDetailBefore = 0;

      await test.step('Precondition: Valid SIGNED two Open End versions', async () => {
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

        const created = await createSignedProductContractOnIndices(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          0,
          productIndex,
          0,
          1,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;
        TestRunSummary.registerPayload('productContract', created.contractPayload);

        const bodyBeforePin = await loadProductContract(Request, contractId);
        priorVersionId = contractVersionRows(bodyBeforePin)[0].versionId;
        await ensureVersionStartsBefore(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          priorVersionId,
          dates.D,
        );

        const follow = await createFollowingValidOpenEndKeepPriorOpen(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          priorVersionId,
          dates,
        );
        followingVersionId = follow.followingVersionId;
        priorVersionId = follow.priorVersionId;

        const body = await loadProductContract(Request, contractId);
        const rows = contractVersionRows(body);
        expect(rows.length).toBe(2);
        expect(rows.every((r) => r.endDate === null), 'both Valid versions stay Open End').toBe(true);
        versionCountBefore = rows.length;
        productDetailBefore = await loadVersionProductDetailId(Request, contractId, priorVersionId);
        expect((body.basicParameters as { status?: string }).status).toBe('SIGNED');
      });

      let productDetailV2Id = 0;
      let afterBody: Record<string, unknown> = {};
      let attachDone = false;
      const attachSummary = (extra: Record<string, unknown> = {}) => {
        if (attachDone) return;
        attachDone = true;
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productDetailV2Id,
            priorVersionId,
            followingVersionId,
            productDetailBefore,
            D: dates.D,
            DMinus1: dates.DMinus1,
            ...extra,
          },
        });
      };

      await test.step('Apply product v2 (PUT 200 is not CREATE)', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-6-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;
      });

      await test.step('GET contract immediately after PUT (no wait)', async () => {
        afterBody = await loadProductContract(Request, contractId);
        const hasCreate = Boolean(findVersionByStartDate(afterBody, dates.D));
        if (!hasCreate) {
          const versionCountAfter = contractVersionRows(afterBody).length;
          TestRunSummary.recordCheck({
            check: 'TC-BE-6 Valid two Open End; prior CREATE +1; following replace',
            expectedResult:
              `CREATE +1: count ${versionCountBefore}+1; new start=${dates.D} end=null productDetail=v2; ` +
              `prior end=${dates.DMinus1} product stays v1; following productDetail=v2; header SIGNED.`,
            actualResult:
              `Not as expected — GET after PUT has no versions[] row with startDate=${dates.D} ` +
              `(count ${versionCountBefore}→${versionCountAfter}). In-place replace is not a pass.`,
            passed: false,
          });
          attachSummary({ versionCountAfter });
        }
        expect(
          findVersionByStartDate(afterBody, dates.D),
          `expected CREATE row startDate=${dates.D} on immediate GET`,
        ).toBeTruthy();
      });

      await test.step('Assert prior CREATE +1; following Stage2 replace; header SIGNED', async () => {
        const result = await assertOpenEndCreatePlusOne(Request, contractId, afterBody, {
          versionCountBefore,
          priorVersionId,
          productDetailBefore,
          expectedNewProductDetailId: productDetailV2Id,
          D: dates.D,
          DMinus1: dates.DMinus1,
        });
        const followingDetail = await loadVersionProductDetailId(
          Request,
          contractId,
          followingVersionId,
        );
        expect(followingDetail).toBe(productDetailV2Id);
        const bp = afterBody.basicParameters as { status?: string };
        expect(bp.status).toBe('SIGNED');

        TestRunSummary.recordCheck({
          check: 'TC-BE-6 Valid two Open End; prior CREATE +1; following replace',
          expectedResult:
            `CREATE +1; new start=${dates.D} productDetail=v2; prior end=${dates.DMinus1} product stays v1; ` +
            `following productDetail=v2; header SIGNED.`,
          actualResult:
            `As expected — count ${versionCountBefore}→${result.versionCountAfter}; ` +
            `newDetail=${result.newDetail}; priorEnd=${result.prior.endDate}; ` +
            `following=${followingDetail}; header=${bp.status}.`,
          passed:
            result.versionCountAfter === versionCountBefore + 1 &&
            result.newDetail === productDetailV2Id &&
            result.prior.endDate === dates.DMinus1 &&
            result.priorDetail === productDetailBefore &&
            followingDetail === productDetailV2Id &&
            bp.status === 'SIGNED',
        });
      });

      await test.step('Attach test run summary', async () => {
        attachSummary();
      });
    });

    // ─── TC-BE-7 ───────────────────────────────────────────────────────────
    test(
      pdt3330TestTitle('TC-BE-7', 'Draft prior Open End CREATE +1; following Start after D replace'),
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};
      let priorVersionId = 0;
      let followingVersionId = 0;
      let versionCountBefore = 0;
      let productDetailBefore = 0;

      await test.step('Precondition: Draft + following created as DRAFT (start=dayAfterD)', async () => {
        // Create following as DRAFT on PUT — no promote/demote. Prior stays Open End (no chaining).
        // Prior Start < D → CREATE +1; following Start after D Open End → Stage2 replace.
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

        const created = await createDraftProductContractOnIndices(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          0,
          productIndex,
          0,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;

        const beforeFollow = await loadProductContract(Request, contractId);
        priorVersionId = contractVersionRows(beforeFollow)[0].versionId;

        followingVersionId = await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          priorVersionId,
          dates.dayAfterD,
          'DRAFT',
        );

        const body = await loadProductContract(Request, contractId);
        const rows = contractVersionRows(body);
        expect(rows.length).toBe(2);
        expect(rows.every((r) => r.endDate === null), 'both Draft versions stay Open End').toBe(true);
        versionCountBefore = rows.length;
        const prior = rows.find((r) => r.versionId === priorVersionId);
        expect(prior).toBeTruthy();
        expect(prior!.endDate, 'prior stays Open End without SIGNED chaining').toBeNull();
        expect(prior!.startDate < dates.D, `prior start ${prior!.startDate} must be before D=${dates.D}`).toBe(
          true,
        );
        productDetailBefore = await loadVersionProductDetailId(Request, contractId, priorVersionId);
        const following = findVersionByStartDate(body, dates.dayAfterD);
        expect(following).toBeTruthy();
        expect(following!.versionId).toBe(followingVersionId);
        expect(following!.endDate, 'following Draft version stays Open End').toBeNull();
        expect((body.basicParameters as { status?: string }).status).toBe('DRAFT');
      });

      let productDetailV2Id = 0;
      let afterBody: Record<string, unknown> = {};
      let attachDone = false;
      const attachSummary = (extra: Record<string, unknown> = {}) => {
        if (attachDone) return;
        attachDone = true;
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productDetailV2Id,
            priorVersionId,
            followingVersionId,
            productDetailBefore,
            D: dates.D,
            DMinus1: dates.DMinus1,
            ...extra,
          },
        });
      };

      await test.step('Apply product v2 (PUT 200 is not CREATE)', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-7-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;
      });

      await test.step('GET contract immediately after PUT (no wait)', async () => {
        afterBody = await loadProductContract(Request, contractId);
        const hasCreate = Boolean(findVersionByStartDate(afterBody, dates.D));
        if (!hasCreate) {
          const versionCountAfter = contractVersionRows(afterBody).length;
          TestRunSummary.recordCheck({
            check: 'TC-BE-7 Draft prior CREATE +1; following replace',
            expectedResult:
              `count ${versionCountBefore}+1; new start=${dates.D} productDetail=v2; ` +
              `prior end=${dates.DMinus1} product stays v1; following productDetail=v2; header DRAFT.`,
            actualResult:
              `Not as expected — GET after PUT has no versions[] row with startDate=${dates.D} ` +
              `(count ${versionCountBefore}→${versionCountAfter}). In-place replace of both rows is not a pass.`,
            passed: false,
          });
          attachSummary({ versionCountAfter });
        }
        expect(
          findVersionByStartDate(afterBody, dates.D),
          `expected CREATE row startDate=${dates.D} on immediate GET`,
        ).toBeTruthy();
      });

      await test.step('Assert prior CREATE +1; following Stage2 replace; header DRAFT', async () => {
        const result = await assertOpenEndCreatePlusOne(Request, contractId, afterBody, {
          versionCountBefore,
          priorVersionId,
          productDetailBefore,
          expectedNewProductDetailId: productDetailV2Id,
          D: dates.D,
          DMinus1: dates.DMinus1,
        });
        const followingDetail = await loadVersionProductDetailId(
          Request,
          contractId,
          followingVersionId,
        );
        expect(followingDetail).toBe(productDetailV2Id);
        const bp = afterBody.basicParameters as { status?: string };
        expect(bp.status).toBe('DRAFT');

        TestRunSummary.recordCheck({
          check: 'TC-BE-7 Draft prior CREATE +1; following replace',
          expectedResult:
            `count ${versionCountBefore}+1; new start=${dates.D} v2; prior end=${dates.DMinus1} stays v1; ` +
            `following productDetail=v2; header DRAFT.`,
          actualResult:
            `As expected — count ${versionCountBefore}→${result.versionCountAfter}; ` +
            `newDetail=${result.newDetail}; priorEnd=${result.prior.endDate}; ` +
            `priorDetail=${result.priorDetail}; following=${followingDetail}; header=${bp.status}.`,
          passed:
            result.versionCountAfter === versionCountBefore + 1 &&
            result.newDetail === productDetailV2Id &&
            result.prior.endDate === dates.DMinus1 &&
            result.priorDetail === productDetailBefore &&
            followingDetail === productDetailV2Id &&
            bp.status === 'DRAFT',
        });
      });

      await test.step('Attach test run summary', async () => {
        attachSummary();
      });
    });

    // ─── TC-BE-8 ───────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-8', 'Empty selection — product v2 only'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      let productId = 0;
      let productIndex = 0;
      let contractId = 0;
      let versionCountBefore = 0;
      let productDetailBefore = 0;

      await test.step('Precondition: Open End on product v1', async () => {
        const base = await createPdt3330BaseChain(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          TestRunSummary,
        );
        productId = base.productId;
        productIndex = base.productIndex;

        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;

        const body = await loadProductContract(Request, contractId);
        versionCountBefore = contractVersionRows(body).length;
        const open = contractVersionRows(body)[0];
        productDetailBefore = await loadVersionProductDetailId(Request, contractId, open.versionId);
      });

      await test.step('PUT product v2 with empty productDetailIds (omit D)', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-8-${Date.now()}`,
          productDetailIds: [],
          newContractsVersionsStartDate: null,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        const v2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);
        expect(v2.version).toBe(2);
      });

      await test.step('Assert contract unchanged', async () => {
        const body = await loadProductContract(Request, contractId);
        expect(contractVersionRows(body).length).toBe(versionCountBefore);
        const open = contractVersionRows(body)[0];
        const detail = await loadVersionProductDetailId(Request, contractId, open.versionId);
        expect(detail).toBe(productDetailBefore);
        const ref = await loadContractVersionProductRef(Request, contractId, open.versionId);
        expect(ref.productVersionId).toBe(1);

        TestRunSummary.recordCheck({
          check: 'TC-BE-8 empty selection no apply',
          expectedResult: 'Product v2 created; contract stays on product v1 / same detail.',
          actualResult: `As expected — detail=${detail}, productVersionId=${ref.productVersionId}.`,
          passed: detail === productDetailBefore && ref.productVersionId === 1,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId },
        });
      });
    });

    // ─── TC-BE-9 ───────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-9', 'Only selected prior product version in scope'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(25 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productDetailV2Id = 0;
      let productIndex = 0;
      let contractAId = 0;
      let contractAPayload: Record<string, unknown> = {};
      let contractBId = 0;
      let contractBProductDetailIdBefore = 0;
      let contractBVersionCountBefore = 0;
      let contractAVersionCountBefore = 0;
      let contractAPriorVersionId = 0;
      let contractAProductDetailBefore = 0;

      await test.step('Precondition: Contract A on v1; create product v2; Contract B on v2', async () => {
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

        const a = await createSignedProductContractOnIndices(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          0,
          productIndex,
          0,
          1,
        );
        contractAId = a.contractId;
        contractAPayload = a.contractPayload;

        const bodyABeforePin = await loadProductContract(Request, contractAId);
        contractAPriorVersionId = contractVersionRows(bodyABeforePin)[0].versionId;
        await ensureVersionStartsBefore(
          Request,
          Endpoints,
          contractAId,
          contractAPayload,
          contractAPriorVersionId,
          dates.D,
        );

        const bodyA = await loadProductContract(Request, contractAId);
        const rowsA = contractVersionRows(bodyA);
        contractAVersionCountBefore = rowsA.length;
        const openA = rowsA.find((r) => r.endDate == null) ?? rowsA[0];
        expect(openA.endDate).toBeNull();
        expect(openA.startDate < dates.D, `A startDate ${openA.startDate} must be before D=${dates.D}`).toBe(
          true,
        );
        contractAPriorVersionId = openA.versionId;
        contractAProductDetailBefore = await loadVersionProductDetailId(
          Request,
          contractAId,
          contractAPriorVersionId,
        );

        const editV2 = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-9-v2-${Date.now()}`,
          productDetailIds: [],
          newContractsVersionsStartDate: null,
        });
        await expect(await putProductEdit(Request, Endpoints, productId, editV2)).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;

        const extra = await createExtraCustomerAndPod(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          TestRunSummary,
          'B',
        );
        const b = await createSignedProductContractOnIndices(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          extra.customerIndex,
          productIndex,
          extra.podIndex,
          2,
        );
        contractBId = b.contractId;

        const bodyB = await loadProductContract(Request, contractBId);
        contractBVersionCountBefore = contractVersionRows(bodyB).length;
        const openB = contractVersionRows(bodyB).find((r) => r.endDate == null)!;
        contractBProductDetailIdBefore = await loadVersionProductDetailId(
          Request,
          contractBId,
          openB.versionId,
        );
        expect(contractBProductDetailIdBefore).toBe(productDetailV2Id);
      });

      let productDetailV3Id = 0;
      let afterA: Record<string, unknown> = {};
      let attachDone = false;
      const attachSummary = (extra: Record<string, unknown> = {}) => {
        if (attachDone) return;
        attachDone = true;
        const linksA = buildProductContractTabLinks(contractAId);
        const linksB = buildProductContractTabLinks(contractBId);
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: {
            productContract: [
              ...(linksA.productContract ?? []),
              ...(linksB.productContract ?? []),
            ],
          },
          snapshot: {
            productId,
            contractAId,
            contractBId,
            productDetailV1Id,
            productDetailV2Id,
            productDetailV3Id,
            contractAPriorVersionId,
            contractAProductDetailBefore,
            D: dates.D,
            DMinus1: dates.DMinus1,
            ...extra,
          },
        });
      };

      await test.step('Apply product v3 selecting only productDetailV1 (PUT 200 is not CREATE on A)', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 2,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-9-v3-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        productDetailV3Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 3))
          .detailId;
      });

      await test.step('GET contract immediately after PUT (no wait)', async () => {
        afterA = await loadProductContract(Request, contractAId);
        const hasCreate = Boolean(findVersionByStartDate(afterA, dates.D));
        if (!hasCreate) {
          const versionCountAfter = contractVersionRows(afterA).length;
          TestRunSummary.recordCheck({
            check: 'TC-BE-9 selection scope — A CREATE +1, B unchanged',
            expectedResult:
              `A count ${contractAVersionCountBefore}+1; new start=${dates.D} productDetail=v3; ` +
              `prior end=${dates.DMinus1} product stays v1. B count unchanged, productDetail still v2.`,
            actualResult:
              `Not as expected — GET after PUT has no versions[] row with startDate=${dates.D} ` +
              `(count ${contractAVersionCountBefore}→${versionCountAfter}).`,
            passed: false,
          });
          attachSummary({ contractAVersionCountAfter: versionCountAfter });
        }
        expect(
          findVersionByStartDate(afterA, dates.D),
          `expected CREATE row startDate=${dates.D} on immediate GET`,
        ).toBeTruthy();
      });

      await test.step('Assert A CREATE +1 onto v3; B unchanged on v2', async () => {
        const resultA = await assertOpenEndCreatePlusOne(Request, contractAId, afterA, {
          versionCountBefore: contractAVersionCountBefore,
          priorVersionId: contractAPriorVersionId,
          productDetailBefore: contractAProductDetailBefore,
          expectedNewProductDetailId: productDetailV3Id,
          D: dates.D,
          DMinus1: dates.DMinus1,
        });

        const bodyB = await loadProductContract(Request, contractBId);
        expect(contractVersionRows(bodyB).length).toBe(contractBVersionCountBefore);
        expect(findVersionByStartDate(bodyB, dates.D), 'do not CREATE on B').toBeFalsy();
        const openB = contractVersionRows(bodyB).find((r) => r.endDate == null)!;
        const detailB = await loadVersionProductDetailId(Request, contractBId, openB.versionId);
        expect(detailB).toBe(contractBProductDetailIdBefore);

        TestRunSummary.recordCheck({
          check: 'TC-BE-9 selection scope — A CREATE +1, B unchanged',
          expectedResult:
            `A CREATE +1 onto productDetail v3; prior end=${dates.DMinus1} stays v1. B stays on v2, count unchanged.`,
          actualResult:
            `As expected — A count ${contractAVersionCountBefore}→${resultA.versionCountAfter} ` +
            `newDetail=${resultA.newDetail}; B detail=${detailB} count=${contractVersionRows(bodyB).length}.`,
          passed:
            resultA.versionCountAfter === contractAVersionCountBefore + 1 &&
            resultA.newDetail === productDetailV3Id &&
            resultA.priorDetail === contractAProductDetailBefore &&
            detailB === contractBProductDetailIdBefore &&
            contractVersionRows(bodyB).length === contractBVersionCountBefore,
        });
      });

      await test.step('Attach test run summary', async () => {
        attachSummary();
      });
    });

    // ─── TC-BE-10 ──────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-10', 'Multiple versions Stage2 replace'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};
      let midVersionId = 0;
      let lateVersionId = 0;

      await test.step('Precondition: mid start=D + late Open End / after D', async () => {
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

        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;

        midVersionId = await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          1,
          dates.D,
          'SIGNED',
        );
        lateVersionId = await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          midVersionId,
          dates.futureDateE,
          'SIGNED',
        );
      });

      let productDetailV2Id = 0;

      await test.step('Apply product v2', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-10-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;
      });

      await test.step('Assert mid and late both on product v2', async () => {
        const body = await loadProductContract(Request, contractId);
        const mid = findVersionByStartDate(body, dates.D)!;
        const late = findVersionByStartDate(body, dates.futureDateE)!;
        midVersionId = mid.versionId;
        lateVersionId = late.versionId;
        const midDetail = await loadVersionProductDetailId(Request, contractId, midVersionId);
        const lateDetail = await loadVersionProductDetailId(Request, contractId, lateVersionId);
        expect(midDetail).toBe(productDetailV2Id);
        expect(lateDetail).toBe(productDetailV2Id);

        TestRunSummary.recordCheck({
          check: 'TC-BE-10 multi-version Stage2',
          expectedResult: 'Both mid (start=D) and late (start=E) productDetailId=v2.',
          actualResult: `As expected — mid=${midDetail}, late=${lateDetail}.`,
          passed: midDetail === productDetailV2Id && lateDetail === productDetailV2Id,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId, midVersionId, lateVersionId, productDetailV2Id },
        });
      });
    });

    // ─── TC-BE-11 ──────────────────────────────────────────────────────────
    test(
      pdt3330TestTitle('TC-BE-11', 'Two contracts independently — A CREATE +1, B End=D CREATE'),
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(25 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractAId = 0;
      let contractAPayload: Record<string, unknown> = {};
      let contractBId = 0;
      let contractBPayload: Record<string, unknown> = {};
      let contractAVersionCountBefore = 0;
      let contractAPriorVersionId = 0;
      let contractAProductDetailBefore = 0;
      let contractBVersionCountBefore = 0;
      let contractBPriorVersionId = 0;

      await test.step('Precondition: Contract A Open End start < D; Contract B End=D shape', async () => {
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

        const a = await createSignedProductContractOnIndices(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          0,
          productIndex,
          0,
          1,
        );
        contractAId = a.contractId;
        contractAPayload = a.contractPayload;

        const bodyABeforePin = await loadProductContract(Request, contractAId);
        contractAPriorVersionId = contractVersionRows(bodyABeforePin)[0].versionId;
        await ensureVersionStartsBefore(
          Request,
          Endpoints,
          contractAId,
          contractAPayload,
          contractAPriorVersionId,
          dates.D,
        );

        const bodyA = await loadProductContract(Request, contractAId);
        const rowsA = contractVersionRows(bodyA);
        contractAVersionCountBefore = rowsA.length;
        const openA = rowsA.find((r) => r.endDate == null) ?? rowsA[0];
        expect(openA.endDate).toBeNull();
        expect(openA.startDate < dates.D, `A startDate ${openA.startDate} must be before D=${dates.D}`).toBe(
          true,
        );
        contractAPriorVersionId = openA.versionId;
        contractAProductDetailBefore = await loadVersionProductDetailId(
          Request,
          contractAId,
          contractAPriorVersionId,
        );

        const extra = await createExtraCustomerAndPod(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          TestRunSummary,
          'B',
        );
        const b = await createSignedProductContractOnIndices(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          extra.customerIndex,
          productIndex,
          extra.podIndex,
          1,
        );
        contractBId = b.contractId;
        contractBPayload = b.contractPayload;

        const bodyBBeforeFollow = await loadProductContract(Request, contractBId);
        contractBPriorVersionId = contractVersionRows(bodyBBeforeFollow)[0].versionId;
        await createFollowingContractVersion(
          Request,
          Endpoints,
          contractBId,
          contractBPayload,
          contractBPriorVersionId,
          dates.dayAfterD,
          'SIGNED',
        );
        const bodyB = await loadProductContract(Request, contractBId);
        expect(findVersionByEndDate(bodyB, dates.D)).toBeTruthy();
        contractBVersionCountBefore = contractVersionRows(bodyB).length;
        const priorB = findVersionByEndDate(bodyB, dates.D);
        expect(priorB).toBeTruthy();
        contractBPriorVersionId = priorB!.versionId;
      });

      let productDetailV2Id = 0;
      let afterA: Record<string, unknown> = {};
      let afterB: Record<string, unknown> = {};
      let attachDone = false;
      const attachSummary = (extra: Record<string, unknown> = {}) => {
        if (attachDone) return;
        attachDone = true;
        const linksA = buildProductContractTabLinks(contractAId);
        const linksB = buildProductContractTabLinks(contractBId);
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: {
            productContract: [
              ...(linksA.productContract ?? []),
              ...(linksB.productContract ?? []),
            ],
          },
          snapshot: {
            productId,
            contractAId,
            contractBId,
            productDetailV2Id,
            contractAPriorVersionId,
            contractBPriorVersionId,
            D: dates.D,
            DMinus1: dates.DMinus1,
            ...extra,
          },
        });
      };

      await test.step('Apply product v2 selecting v1 (PUT 200 is not CREATE)', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-11-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;
      });

      await test.step('GET contracts A then B immediately after PUT (no wait)', async () => {
        afterA = await loadProductContract(Request, contractAId);
        const hasCreateA = Boolean(findVersionByStartDate(afterA, dates.D));
        if (!hasCreateA) {
          const versionCountAfterA = contractVersionRows(afterA).length;
          TestRunSummary.recordCheck({
            check: 'TC-BE-11 independent contracts — A CREATE +1, B End=D CREATE',
            expectedResult:
              `A count ${contractAVersionCountBefore}+1; prior end=${dates.DMinus1}; new row v2. ` +
              `B count ${contractBVersionCountBefore}+1; prior end=${dates.DMinus1}; new row v2.`,
            actualResult:
              `Not as expected — GET after PUT has no versions[] row with startDate=${dates.D} ` +
              `(count ${contractAVersionCountBefore}→${versionCountAfterA}).`,
            passed: false,
          });
          attachSummary({ contractAVersionCountAfter: versionCountAfterA });
        }
        expect(
          findVersionByStartDate(afterA, dates.D),
          `expected CREATE row startDate=${dates.D} on immediate GET (A)`,
        ).toBeTruthy();

        afterB = await loadProductContract(Request, contractBId);
        const hasCreateB = Boolean(findVersionByStartDate(afterB, dates.D));
        if (!hasCreateB) {
          const versionCountAfterB = contractVersionRows(afterB).length;
          TestRunSummary.recordCheck({
            check: 'TC-BE-11 independent contracts — A CREATE +1, B End=D CREATE',
            expectedResult:
              `A count ${contractAVersionCountBefore}+1; prior end=${dates.DMinus1}; new row v2. ` +
              `B count ${contractBVersionCountBefore}+1; prior end=${dates.DMinus1}; new row v2.`,
            actualResult:
              `Not as expected — GET after PUT has no versions[] row with startDate=${dates.D} ` +
              `(count ${contractBVersionCountBefore}→${versionCountAfterB}).`,
            passed: false,
          });
          attachSummary({ contractBVersionCountAfter: versionCountAfterB });
        }
        expect(
          findVersionByStartDate(afterB, dates.D),
          `expected CREATE row startDate=${dates.D} on immediate GET (B)`,
        ).toBeTruthy();
      });

      await test.step('Assert A CREATE +1 Open End; B End=D CREATE +1', async () => {
        const resultA = await assertOpenEndCreatePlusOne(Request, contractAId, afterA, {
          versionCountBefore: contractAVersionCountBefore,
          priorVersionId: contractAPriorVersionId,
          productDetailBefore: contractAProductDetailBefore,
          expectedNewProductDetailId: productDetailV2Id,
          D: dates.D,
          DMinus1: dates.DMinus1,
        });

        const versionCountBAfter = contractVersionRows(afterB).length;
        expect(versionCountBAfter).toBe(contractBVersionCountBefore + 1);
        const createdB = findVersionByStartDate(afterB, dates.D);
        expect(createdB).toBeTruthy();
        const priorB = contractVersionRows(afterB).find((r) => r.versionId === contractBPriorVersionId);
        expect(priorB?.endDate).toBe(dates.DMinus1);
        const detailB = await loadVersionProductDetailId(Request, contractBId, createdB!.versionId);
        expect(detailB).toBe(productDetailV2Id);

        TestRunSummary.recordCheck({
          check: 'TC-BE-11 independent contracts — A CREATE +1, B End=D CREATE',
          expectedResult:
            `A CREATE +1 Open End onto v2 (prior end=${dates.DMinus1}). ` +
            `B CREATE +1 End=D onto v2 (prior end=${dates.DMinus1}).`,
          actualResult:
            `As expected — A ${contractAVersionCountBefore}→${resultA.versionCountAfter} newDetail=${resultA.newDetail}; ` +
            `B ${contractBVersionCountBefore}→${versionCountBAfter} newDetail=${detailB}; priorBEnd=${priorB?.endDate}.`,
          passed:
            resultA.versionCountAfter === contractAVersionCountBefore + 1 &&
            resultA.newDetail === productDetailV2Id &&
            versionCountBAfter === contractBVersionCountBefore + 1 &&
            detailB === productDetailV2Id &&
            priorB?.endDate === dates.DMinus1,
        });
      });

      await test.step('Attach test run summary', async () => {
        attachSummary();
      });
    });

    // ─── TC-BE-12 ──────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-12', 'Stage1 version gets Stage2 params (paymentGuarantee)'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};

      await test.step('Precondition: signed + following dayAfterD (End=D)', async () => {
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

        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;

        await createFollowingContractVersion(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          1,
          dates.dayAfterD,
          'SIGNED',
        );
      });

      let productDetailV2Id = 0;

      await test.step('Apply product v2', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-12-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        editPayload.paymentGuarantees = ['NO'];
        editPayload.contractTypes = ['COMBINED'];
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        const v2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);
        productDetailV2Id = v2.detailId;
      });

      await test.step('Assert Stage1 version productDetail + paymentGuarantee', async () => {
        const body = await loadProductContract(Request, contractId);
        const hasCreate = Boolean(findVersionByStartDate(body, dates.D));
        if (!hasCreate) {
          const versionCountAfter = contractVersionRows(body).length;
          TestRunSummary.recordCheck({
            check: 'TC-BE-12 Stage1+params AC-17',
            expectedResult: `startDate=${dates.D} productDetail=v2; paymentGuarantee=NO; prior end=D-1.`,
            actualResult:
              `Not as expected — GET after PUT has no versions[] row with startDate=${dates.D} ` +
              `(count ${versionCountAfter}).`,
            passed: false,
          });
        }
        expect(
          findVersionByStartDate(body, dates.D),
          `expected CREATE row startDate=${dates.D} on immediate GET`,
        ).toBeTruthy();
        const created = findVersionByStartDate(body, dates.D)!;
        const detail = await loadVersionProductDetailId(Request, contractId, created.versionId);
        expect(detail).toBe(productDetailV2Id);

        const versionRes = await Request.get(
          `${Endpoints.productContract}/${contractId}?versionId=${created.versionId}`,
        );
        await expect(versionRes).CheckResponse();
        const versionBody = await versionRes.json();
        const pp = (versionBody.productParameters ?? {}) as { paymentGuarantee?: string };
        expect(pp.paymentGuarantee).toBe('NO');
        expect(findVersionByEndDate(body, addDaysIso(dates.D, -1))).toBeTruthy();

        TestRunSummary.recordCheck({
          check: 'TC-BE-12 Stage1+params AC-17',
          expectedResult: `startDate=${dates.D} productDetail=v2; paymentGuarantee=NO; prior end=D-1.`,
          actualResult: `As expected — detail=${detail}, paymentGuarantee=${pp.paymentGuarantee}.`,
          passed: detail === productDetailV2Id && pp.paymentGuarantee === 'NO',
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId, productDetailV2Id },
        });
      });
    });

    // ─── TC-BE-13 ──────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-13', 'Past D rejected'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;

      await test.step('Precondition: signed Open End', async () => {
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
        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
      });

      await test.step('PUT with pastDate — expect 400 + exact fragment', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-13-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.pastDate,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);

        const putRes = await putProductEdit(Request, Endpoints, productId, editPayload);
        const bodyText = await putRes.text();
        expect(putRes.status()).toBe(400);
        expect(errorBodyContains(bodyText, ERR_D_MUST_BE_TODAY_OR_FUTURE)).toBe(true);

        const v2Res = await Request.get(`${Endpoints.product}/${productId}?version=2`);
        expect(v2Res.ok()).toBe(false);

        TestRunSummary.recordCheck({
          check: 'TC-BE-13 past D validation',
          expectedResult: `HTTP 400 with fragment "${ERR_D_MUST_BE_TODAY_OR_FUTURE}".`,
          actualResult: `As expected — status=${putRes.status()}; fragment present=${errorBodyContains(bodyText, ERR_D_MUST_BE_TODAY_OR_FUTURE)}.`,
          passed:
            putRes.status() === 400 && errorBodyContains(bodyText, ERR_D_MUST_BE_TODAY_OR_FUTURE),
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId, pastDate: dates.pastDate },
        });
      });
    });

    // ─── TC-BE-14 ──────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-14', 'Missing D rejected when selection non-empty'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;

      await test.step('Precondition: signed Open End', async () => {
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
        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
      });

      await test.step('PUT without newContractsVersionsStartDate — expect 400', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-14-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: null,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);

        const putRes = await putProductEdit(Request, Endpoints, productId, editPayload);
        const bodyText = await putRes.text();
        expect(putRes.status()).toBe(400);
        expect(errorBodyContains(bodyText, ERR_D_FIELD_REQUIRED)).toBe(true);

        const v2Res = await Request.get(`${Endpoints.product}/${productId}?version=2`);
        expect(v2Res.ok()).toBe(false);

        TestRunSummary.recordCheck({
          check: 'TC-BE-14 missing D validation',
          expectedResult: `HTTP 400 with fragment "${ERR_D_FIELD_REQUIRED}".`,
          actualResult: `As expected — status=${putRes.status()}; fragment present=${errorBodyContains(bodyText, ERR_D_FIELD_REQUIRED)}.`,
          passed: putRes.status() === 400 && errorBodyContains(bodyText, ERR_D_FIELD_REQUIRED),
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId },
        });
      });
    });

    // ─── TC-BE-15 ──────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-15', 'updateExistingVersion true — no apply'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let versionCountBefore = 0;
      const shortDescription = `PDT-3330-TC-BE-15-${Date.now()}`;

      await test.step('Precondition: signed Open End', async () => {
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
        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
        versionCountBefore = contractVersionRows(await loadProductContract(Request, contractId)).length;
      });

      await test.step('PUT updateExistingVersion=true', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: true,
          shortDescription,
          productDetailIds: null,
          newContractsVersionsStartDate: null,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();

        const v1 = await Request.get(`${Endpoints.product}/${productId}?version=1`);
        await expect(v1).CheckResponse();
        const v1Body = await v1.json();
        expect(v1Body.shortDescription).toBe(shortDescription);

        const v2Res = await Request.get(`${Endpoints.product}/${productId}?version=2`);
        expect(v2Res.ok()).toBe(false);

        const body = await loadProductContract(Request, contractId);
        expect(contractVersionRows(body).length).toBe(versionCountBefore);
        const open = contractVersionRows(body)[0];
        const detail = await loadVersionProductDetailId(Request, contractId, open.versionId);
        expect(detail).toBe(productDetailV1Id);

        TestRunSummary.recordCheck({
          check: 'TC-BE-15 updateExistingVersion no apply',
          expectedResult: 'v1 shortDescription updated; no v2; contract detail unchanged.',
          actualResult: `As expected — shortDescription=${v1Body.shortDescription}; detail=${detail}.`,
          passed: v1Body.shortDescription === shortDescription && detail === productDetailV1Id,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId, shortDescription },
        });
      });
    });

    // ─── TC-BE-16 ──────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-16', 'TERMINATED contract excluded'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let versionCountBefore = 0;
      let productDetailIdBefore = 0;

      await test.step('Precondition: signed then TERMINATED', async () => {
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
        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;

        const statusRes = await productContractStatusUpdateFull(Request, contractId, 1, {
          contractStatus: 'TERMINATED',
          contractSubStatus: 'BY_MUTUAL_AGREEMENT',
          contractVersionStatus: 'SIGNED',
        });
        await expect(statusRes).CheckResponse();

        const body = await loadProductContract(Request, contractId);
        const bp = body.basicParameters as { status?: string; subStatus?: string };
        expect(bp.status).toBe('TERMINATED');
        expect(bp.subStatus).toBe('BY_MUTUAL_AGREEMENT');
        versionCountBefore = contractVersionRows(body).length;
        productDetailIdBefore = await loadVersionProductDetailId(
          Request,
          contractId,
          contractVersionRows(body)[0].versionId,
        );
      });

      await test.step('Apply product v2 — TERMINATED unchanged', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-16-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        const v2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);
        expect(v2.version).toBe(2);

        const body = await loadProductContract(Request, contractId);
        expect((body.basicParameters as { status?: string }).status).toBe('TERMINATED');
        expect(contractVersionRows(body).length).toBe(versionCountBefore);
        expect(findVersionByStartDate(body, dates.D)).toBeFalsy();
        for (const row of contractVersionRows(body)) {
          const detail = await loadVersionProductDetailId(Request, contractId, row.versionId);
          expect(detail).toBe(productDetailIdBefore);
        }

        TestRunSummary.recordCheck({
          check: 'TC-BE-16 TERMINATED excluded',
          expectedResult: 'Product v2 created; TERMINATED contract versions unchanged.',
          actualResult: `As expected — product v2 detail=${v2.detailId}; contract details stay ${productDetailIdBefore}.`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId, productDetailIdBefore },
        });
      });
    });

    // ─── TC-BE-17 ──────────────────────────────────────────────────────────
    test(
      pdt3330TestTitle('TC-BE-17', 'D equals todayDate — Open End Start equals D replace (AC-6)'),
      async ({ Request, GeneratePayload, Responses, Endpoints, TestRunSummary }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let contractPayload: Record<string, unknown> = {};
      let openEndVersionId = 0;
      let versionCountBefore = 0;

      await test.step('Precondition: signed Open End startDate === todayDate (Start = D)', async () => {
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
        const created = await createSignedProductContractOnIndices(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          0,
          productIndex,
          0,
          1,
        );
        contractId = created.contractId;
        contractPayload = created.contractPayload;
        const bodyBeforePin = await loadProductContract(Request, contractId);
        openEndVersionId = contractVersionRows(bodyBeforePin).find((r) => r.endDate == null)!.versionId;
        await ensureVersionStartEquals(
          Request,
          Endpoints,
          contractId,
          contractPayload,
          openEndVersionId,
          dates.todayDate,
        );
        const body = await loadProductContract(Request, contractId);
        const open = contractVersionRows(body).find((r) => r.endDate == null)!;
        openEndVersionId = open.versionId;
        expect(open.endDate).toBeNull();
        expect(open.startDate, 'Open End startDate equals todayDate = D').toBe(dates.todayDate);
        versionCountBefore = contractVersionRows(body).length;
      });

      let productDetailV2Id = 0;

      await test.step('Apply with newContractsVersionsStartDate = todayDate (no CREATE)', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-17-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.todayDate,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        expect(editPayload.newContractsVersionsStartDate).toBe(dates.todayDate);
        expect((await validateProductRelatedContractUpdate(Request, Endpoints, editPayload)).eligible).toBe(
          true,
        );
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        productDetailV2Id = (await loadProductDetailSnapshot(Request, Endpoints, productId, 2))
          .detailId;
      });

      await test.step('Assert Start = D Open End Stage2 replace; count unchanged (AC-6)', async () => {
        const body = await loadProductContract(Request, contractId);
        expect(contractVersionRows(body).length).toBe(versionCountBefore);
        const open = contractVersionRows(body).find((r) => r.versionId === openEndVersionId);
        expect(open).toBeTruthy();
        expect(open!.endDate).toBeNull();
        const detail = await loadVersionProductDetailId(Request, contractId, openEndVersionId);
        expect(detail).toBe(productDetailV2Id);
        const ref = await loadContractVersionProductRef(Request, contractId, openEndVersionId);
        expect(ref.productVersionId).toBe(2);

        TestRunSummary.recordCheck({
          check: 'TC-BE-17 D=todayDate Open End Start equals D replace (AC-6)',
          expectedResult:
            `Start = D (${dates.todayDate}) → Stage2 replace, not PDT-3454 CREATE. ` +
            `Count unchanged; same openEndVersionId productDetail=v2; endDate still null.`,
          actualResult:
            `As expected — count=${contractVersionRows(body).length}; detail=${detail}; ` +
            `productVersionId=${ref.productVersionId}; endDate=${open!.endDate}.`,
          passed:
            contractVersionRows(body).length === versionCountBefore &&
            detail === productDetailV2Id &&
            ref.productVersionId === 2 &&
            open!.endDate == null,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productDetailV2Id,
            openEndVersionId,
            D: dates.todayDate,
            versionCountBefore,
          },
        });
      });
    });

    // ─── TC-BE-18 ──────────────────────────────────────────────────────────
    test(pdt3330TestTitle('TC-BE-18', 'CANCELLED contract excluded'), async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);
      const dates = pdt3330DateAliases();
      let productId = 0;
      let productDetailV1Id = 0;
      let productIndex = 0;
      let contractId = 0;
      let versionCountBefore = 0;
      let productDetailIdBefore = 0;

      await test.step('Precondition: Draft then CANCELLED', async () => {
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

        const created = await createDraftProductContractOnIndices(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
          0,
          productIndex,
          0,
        );
        contractId = created.contractId;

        const statusRes = await productContractStatusUpdateFull(Request, contractId, 1, {
          contractStatus: 'CANCELLED',
          contractSubStatus: 'UNSENT_TO_CUSTOMER',
          contractVersionStatus: 'CANCELLED',
        });
        await expect(statusRes).CheckResponse();

        const body = await loadProductContract(Request, contractId);
        const bp = body.basicParameters as { status?: string; subStatus?: string };
        expect(bp.status).toBe('CANCELLED');
        expect(bp.subStatus).toBe('UNSENT_TO_CUSTOMER');
        versionCountBefore = contractVersionRows(body).length;
        productDetailIdBefore = await loadVersionProductDetailId(
          Request,
          contractId,
          contractVersionRows(body)[0].versionId,
        );
      });

      await test.step('Apply product v2 — CANCELLED unchanged', async () => {
        const editPayload = await buildPdt3330ProductEditPayload(GeneratePayload, productIndex, {
          baseVersion: 1,
          updateExistingVersion: false,
          shortDescription: `PDT-3330-TC-BE-18-${Date.now()}`,
          productDetailIds: [productDetailV1Id],
          newContractsVersionsStartDate: dates.D,
        });
        TestRunSummary.registerPayload('productEdit', editPayload);
        await expect(await putProductEdit(Request, Endpoints, productId, editPayload)).CheckResponse();
        const v2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);
        expect(v2.version).toBe(2);

        const body = await loadProductContract(Request, contractId);
        expect((body.basicParameters as { status?: string }).status).toBe('CANCELLED');
        expect(contractVersionRows(body).length).toBe(versionCountBefore);
        expect(findVersionByStartDate(body, dates.D)).toBeFalsy();
        for (const row of contractVersionRows(body)) {
          const detail = await loadVersionProductDetailId(Request, contractId, row.versionId);
          expect(detail).toBe(productDetailIdBefore);
        }

        TestRunSummary.recordCheck({
          check: 'TC-BE-18 CANCELLED excluded',
          expectedResult: 'Product v2 created; CANCELLED contract versions unchanged.',
          actualResult: `As expected — product v2 detail=${v2.detailId}; contract details stay ${productDetailIdBefore}.`,
          passed: true,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-3330',
          relevantEntityKeys: [...PDT_3330_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: { productId, contractId, productDetailIdBefore },
        });
      });
    });
  },
);
