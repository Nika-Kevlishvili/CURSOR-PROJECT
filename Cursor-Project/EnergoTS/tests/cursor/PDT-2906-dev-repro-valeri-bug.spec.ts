/**
 * PDT-2906 — Dev bug reproduction (Valeri async path, 41260-style).
 *
 * Reproduces contract v2 staying on product v1 after product popup apply when:
 * - ACTIVE_IN_TERM contract v1 with past start_date and POD activated
 * - Product new version with added filled-formula price component
 * - productDetailIdsForUpdatingProductContracts via async event path
 *
 * Run on Dev:
 *   $env:BASE_URL="http://10.236.20.11:8091"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-2906-dev-repro-valeri-bug.spec.ts --project=main
 *
 * Reference spec(s):
 * - tests/salesPortal/getProductList.spec.ts (~2385 — fixed-parameter product payload)
 * - tests/cursor/PDT-2906-video-save-error-price-component.spec.ts (added PC before new version)
 * - tests/cursor/pdt-2906-product-new-version-contract-update.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import {
  applyFixedParameterProductFields,
  applyFixedParameterTermFields,
  buildPriceComponentWithFilledFormulaValuePayload,
  buildProductNewVersionEditPayloadWithAddedPriceComponents,
  createSignedProductContractV1WithPastStartDate,
  entityId,
  loadContractVersionProductRef,
  loadProductContract,
  loadProductDetailSnapshot,
  maxContractLogicalVersion,
  pollUntilContractLogicalVersion,
  postValidateProductRelatedContractUpdate,
  putProductNewVersionWithContractDetailIds,
} from './pdt-2906-product-new-version-contract-update.fixtures';

const PDT_2906_DEV_REPRO_ENTITIES = [
  'terms',
  'priceComponent',
  'product',
  'customer',
  'pod',
  'productContract',
] as const;

test.describe(
  '[PDT-2906]: Dev Valeri bug repro — contract v2 stays on product v1',
  { tag: ['@pdt-2906', '@dev', '@bug-repro'] },
  () => {
    test('[PDT-2906]: Create product new version and apply it to existing contracts', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);

      let productId = 0;
      let contractId = 0;
      let productV1DetailId = 0;
      let initialPriceComponentId = 0;
      let addedPriceComponentId = 0;
      let pastStartDate = '';

      await test.step('Precondition: term', async () => {
        const termPayload = GeneratePayload.productAndServices.term();
        applyFixedParameterTermFields(termPayload);
        TestRunSummary.registerPayload('terms', termPayload);
        const term = await Request.post(Endpoints.terms, { data: termPayload });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
      });

      await test.step('Precondition: price component for product v1 create', async () => {
        const pricePayload = GeneratePayload.productAndServices.priceSettlement();
        TestRunSummary.registerPayload('priceComponentV1', pricePayload);
        const price = await Request.post(Endpoints.priceComponent, { data: pricePayload });
        await expect(price).CheckResponse();
        Responses.priceComponent.push(await price.json());
        initialPriceComponentId = entityId(Responses.priceComponent[0]);
      });

      await test.step('Precondition: fixed-parameter product v1', async () => {
        const productPayload = GeneratePayload.productAndServices.product();
        applyFixedParameterProductFields(productPayload);
        TestRunSummary.registerPayload('product', productPayload);
        const product = await Request.post(Endpoints.product, { data: productPayload });
        await expect(product).CheckResponse();
        Responses.product.push(await product.json());
        productId = entityId(Responses.product[0]);

        const v1Product = await loadProductDetailSnapshot(Request, Endpoints, productId, 1);
        productV1DetailId = v1Product.detailId;
        expect(v1Product.version).toBe(1);
      });

      await test.step('Precondition: customer', async () => {
        const customerPayload = GeneratePayload.customers.customer_legal();
        TestRunSummary.registerPayload('customer', customerPayload);
        const customer = await Request.post(Endpoints.customer, { data: customerPayload });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
      });

      await test.step('Precondition: POD', async () => {
        const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();
        podPayload.type = 'CONSUMER';
        podPayload.consumptionPurpose = 'NON_HOUSEHOLD';
        podPayload.voltageLevel = 'LOW';
        TestRunSummary.registerPayload('pod', podPayload);
        const pod = await Request.post(Endpoints.pod, { data: podPayload });
        await expect(pod).CheckResponse();
        Responses.pod.push(await pod.json());
      });

      await test.step(
        'Precondition: signed ACTIVE_IN_TERM contract v1 with past start_date + POD activation',
        async () => {
          const created = await createSignedProductContractV1WithPastStartDate(
            Request,
            GeneratePayload,
            Responses,
            Endpoints,
          );
          contractId = created.contractId;
          pastStartDate = created.pastStartDate;
          productV1DetailId = created.productV1DetailId;
          TestRunSummary.registerPayload('productContractV1', created.contractPayload);

          const contractBody = await loadProductContract(Request, contractId);
          expect(maxContractLogicalVersion(contractBody), 'only contract v1 before product apply').toBe(
            1,
          );

          const v1Ref = await loadContractVersionProductRef(Request, contractId, 1);
          expect(v1Ref.productVersionId).toBe(1);
          expect(v1Ref.productDetailId).toBe(productV1DetailId);

          TestRunSummary.recordCheck({
            check: 'ACTIVE_IN_TERM v1 with past start_date — only v1 exists before apply',
            expectedResult:
              'Contract v1 is in force (ACTIVE_IN_TERM), start_date in the past, POD activated; logical version 1 only.',
            actualResult: `As expected — contractId=${contractId}; header=${created.headerStatus}; startDate=${pastStartDate}; maxVersion=1; productV1DetailId=${productV1DetailId}.`,
            passed: true,
          });
        },
      );

      await test.step(
        'Precondition: second price component with FILLED formula (41260-style add before new version)',
        async () => {
          const filledPcPayload = buildPriceComponentWithFilledFormulaValuePayload(GeneratePayload);
          TestRunSummary.registerPayload('priceComponentAddedOnEdit', filledPcPayload);
          const price = await Request.post(Endpoints.priceComponent, { data: filledPcPayload });
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
          addedPriceComponentId = entityId(Responses.priceComponent[1]);
          expect(addedPriceComponentId, 'added PC id must differ from v1 PC').not.toBe(
            initialPriceComponentId,
          );
        },
      );

      let editPayload: Awaited<ReturnType<typeof buildProductNewVersionEditPayloadWithAddedPriceComponents>>;

      await test.step(
        'Build product new version edit payload — add second PC to product (like UI edit)',
        async () => {
          editPayload = await buildProductNewVersionEditPayloadWithAddedPriceComponents(
            GeneratePayload,
            0,
          );
          expect(editPayload.updateExistingVersion).toBe(false);
          expect(editPayload.version).toBe(1);
          expect(editPayload.priceComponentIds.length).toBeGreaterThanOrEqual(2);
          expect(editPayload.priceComponentIds).toEqual(
            expect.arrayContaining([initialPriceComponentId, addedPriceComponentId]),
          );

          editPayload.productDetailIdsForUpdatingProductContracts = [productV1DetailId];
          TestRunSummary.registerPayload('productNewVersionEditWithAddedPc', {
            productId,
            updateExistingVersion: editPayload.updateExistingVersion,
            version: editPayload.version,
            priceComponentIds: editPayload.priceComponentIds,
            productDetailIdsForUpdatingProductContracts:
              editPayload.productDetailIdsForUpdatingProductContracts,
            initialPriceComponentId,
            addedPriceComponentId,
          });

          TestRunSummary.recordCheck({
            check: 'Edit payload adds filled-formula PC on top of v1 product PCs',
            expectedResult:
              'priceComponentIds must include the original v1 price component and the newly created filled-formula component.',
            actualResult: `As expected — priceComponentIds=${JSON.stringify(editPayload.priceComponentIds)}.`,
            passed: editPayload.priceComponentIds.length >= 2,
          });
        },
      );

      await test.step(
        'POST validate-product-related-contract-update — expect true (popup would open)',
        async () => {
          const validate = await postValidateProductRelatedContractUpdate(
            Request,
            Endpoints,
            editPayload!,
          );
          expect(validate.status).toBeGreaterThanOrEqual(200);
          expect(validate.status).toBeLessThan(300);
          expect(
            validate.eligible,
            'filled-formula added PC must pass contract-update validation',
          ).toBe(true);

          TestRunSummary.recordCheck({
            check: 'Product eligible for contract update popup (validate endpoint)',
            expectedResult:
              'POST validate-product-related-contract-update must return true when added price component has filled formula.',
            actualResult: `As expected — eligible=${validate.eligible}.`,
            passed: validate.eligible === true,
          });
        },
      );

      let productV2DetailId = 0;

      await test.step(
        'PUT product new version with productDetailIdsForUpdatingProductContracts (async apply)',
        async () => {
          const putResult = await putProductNewVersionWithContractDetailIds(
            Request,
            Endpoints,
            productId,
            editPayload!,
            [productV1DetailId],
          );
          expect(putResult.status).toBeGreaterThanOrEqual(200);
          expect(putResult.status).toBeLessThan(300);

          const productV2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);
          productV2DetailId = productV2.detailId;
          expect(productV2.version).toBe(2);
          expect(productV2.detailId, 'product v2 detailId must differ from v1').not.toBe(
            productV1DetailId,
          );

          TestRunSummary.recordCheck({
            check: 'Product v2 created with distinct detailId',
            expectedResult:
              'PUT must create product logical version 2; product v2 detailId differs from v1.',
            actualResult: `As expected — productV2DetailId=${productV2DetailId}; productV1DetailId=${productV1DetailId}.`,
            passed: productV2DetailId !== productV1DetailId,
          });
        },
      );

      await test.step('Poll contract logical version 2 (async handler, 120s)', async () => {
        await pollUntilContractLogicalVersion(Request, contractId, 2, 120_000);
        const contractBody = await loadProductContract(Request, contractId);
        expect(maxContractLogicalVersion(contractBody)).toBeGreaterThanOrEqual(2);
      });

      await test.step(
        'Bug repro assertion — contract v2 must stay on product v1 (PASS = bug reproduced)',
        async () => {
          const v2Ref = await loadContractVersionProductRef(Request, contractId, 2);

          TestRunSummary.registerPayload('contractV2ProductRef', {
            contractId,
            productVersionId: v2Ref.productVersionId,
            productDetailId: v2Ref.productDetailId,
            productV1DetailId,
            productV2DetailId,
            bugReproduced:
              v2Ref.productVersionId === 1 && v2Ref.productDetailId === productV1DetailId,
          });

          const bugReproduced =
            v2Ref.productVersionId === 1 && v2Ref.productDetailId === productV1DetailId;

          TestRunSummary.recordCheck({
            check: 'Contract v2 product reference (41260-style bug on Dev)',
            expectedResult:
              'BUG REPRODUCED when contract v2 has productVersionId=1 and productDetailId=v1 detail (stays on product v1). Test FAILS if productVersionId=2 (fix deployed).',
            actualResult: bugReproduced
              ? `Bug reproduced — productVersionId=${v2Ref.productVersionId}; productDetailId=${v2Ref.productDetailId} (v1 detail ${productV1DetailId}); product v2 detail exists as ${productV2DetailId}.`
              : `Bug NOT reproduced — productVersionId=${v2Ref.productVersionId}; productDetailId=${v2Ref.productDetailId}; expected productVersionId=1 and productDetailId=${productV1DetailId}.`,
            passed: bugReproduced,
          });

          expect(
            v2Ref.productVersionId,
            `bug repro: contract v2 must stay on product v1 (productVersionId=1); got ${v2Ref.productVersionId} — bug NOT reproduced on Dev`,
          ).toBe(1);
          expect(
            v2Ref.productDetailId,
            `bug repro: contract v2 productDetailId must remain v1 detail ${productV1DetailId}; got ${v2Ref.productDetailId}`,
          ).toBe(productV1DetailId);
        },
      );

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-2906',
          relevantEntityKeys: [...PDT_2906_DEV_REPRO_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productV1DetailId,
            productV2DetailId,
            initialPriceComponentId,
            addedPriceComponentId,
            pastStartDate,
            flow: 'dev-repro-valeri-bug-41260-style',
            environment: 'dev',
          },
        });
      });
    });
  },
);
