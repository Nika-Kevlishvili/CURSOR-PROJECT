/**
 * PDT-2906 — Valeri video flow: product new version save fails when a newly added price component
 * has an undefined formula variable value and popup apply passes all product detail IDs.
 *
 * Run on Dev:
 *   $env:BASE_URL="http://10.236.20.11:8091"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-2906-video-save-error-price-component.spec.ts --project=main
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2906-product-new-version-contract-update.spec.ts (happy path)
 * - tests/cursor/PDT-2906-valeri-async-apply.spec.ts (Valeri async path)
 * - tests/cursor/pdt-2906-product-new-version-contract-update.fixtures.ts
 * - tests/cursor/PHN-2130-sales-portal-contract-update.spec.ts (unfilled formula PC pattern)
 */

import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import {
  applyFixedParameterProductFields,
  applyFixedParameterTermFields,
  buildPriceComponentWithUndefinedFormulaValuePayload,
  buildProductNewVersionEditPayloadWithAddedPriceComponents,
  createSignedProductContractV1,
  entityId,
  loadProductDetailSnapshot,
  PDT_2906_PRICE_COMPONENT_VALUE_NOT_DEFINED_PATTERN,
  postValidateProductRelatedContractUpdate,
  putProductNewVersionWithContractDetailIds,
} from './pdt-2906-product-new-version-contract-update.fixtures';

const PDT_2906_VIDEO_RELEVANT_ENTITIES = [
  'terms',
  'priceComponent',
  'product',
  'customer',
  'pod',
  'productContract',
] as const;

test.describe(
  '[PDT-2906]: Valeri video flow — product save error on added price component',
  { tag: ['@pdt-2906', '@dev', '@productAndServices'] },
  () => {
    test('[PDT-2906]: Valeri video flow — product save fails when added price component formula value undefined', async ({
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

      await test.step('Precondition: term', async () => {
        const termPayload = GeneratePayload.productAndServices.term();
        applyFixedParameterTermFields(termPayload);
        TestRunSummary.registerPayload('terms', termPayload);
        const term = await Request.post(Endpoints.terms, { data: termPayload });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
      });

      await test.step('Precondition: price component for product v1 create', async () => {
        const pricePayload = GeneratePayload.productAndServices.electricity();
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

      await test.step('Precondition: signed product contract v1 on product v1', async () => {
        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
        TestRunSummary.registerPayload('productContractV1', created.contractPayload);

        const v1Product = await loadProductDetailSnapshot(Request, Endpoints, productId, 1);
        productV1DetailId = v1Product.detailId;
        expect(v1Product.version).toBe(1);

        TestRunSummary.recordCheck({
          check: 'Signed contract v1 exists on product v1',
          expectedResult:
            'Product contract must be SIGNED on logical version 1; product entity remains at version 1 with a valid detailId for popup selection.',
          actualResult: `As expected — contractId=${contractId}; productV1DetailId=${productV1DetailId}.`,
          passed: true,
        });
      });

      await test.step(
        'Precondition: NEW price component with undefined formula variable (added during edit, not at create)',
        async () => {
          const unfilledPcPayload = buildPriceComponentWithUndefinedFormulaValuePayload(GeneratePayload);
          TestRunSummary.registerPayload('priceComponentAddedOnEdit', unfilledPcPayload);
          const price = await Request.post(Endpoints.priceComponent, { data: unfilledPcPayload });
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
        'Build product new version edit payload (updateExistingVersion: false) with v1 + added price components',
        async () => {
          editPayload = await buildProductNewVersionEditPayloadWithAddedPriceComponents(
            GeneratePayload,
            0,
          );
          expect(editPayload.updateExistingVersion).toBe(false);
          expect(editPayload.version).toBe(1);
          expect(editPayload.priceComponentIds.length).toBeGreaterThanOrEqual(2);
          const normalizedPcIds = editPayload.priceComponentIds.map((entry) => entityId(entry));
          expect(normalizedPcIds).toEqual(
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
            check: 'Edit payload adds new price component on top of v1 product PCs',
            expectedResult:
              'priceComponentIds must include the original v1 price component and the newly created unfilled-formula component.',
            actualResult: `As expected — priceComponentIds length=${editPayload.priceComponentIds.length}; initialPc=${initialPriceComponentId}; addedPc=${addedPriceComponentId}.`,
            passed: editPayload.priceComponentIds.length >= 2,
          });
        },
      );

      await test.step(
        'POST validate-product-related-contract-update — expect not eligible (popup would block)',
        async () => {
          const validate = await postValidateProductRelatedContractUpdate(
            Request,
            Endpoints,
            editPayload!,
          );
          expect(validate.status).toBeGreaterThanOrEqual(200);
          expect(validate.status).toBeLessThan(300);
          expect(validate.eligible, 'unfilled formula PC must fail contract-update validation').toBe(
            false,
          );

          TestRunSummary.recordCheck({
            check: 'Product not eligible for contract update popup (validate endpoint)',
            expectedResult:
              'POST validate-product-related-contract-update must return false when added price component formula value is undefined.',
            actualResult: `As expected — eligible=${validate.eligible}; body=${validate.bodyText.slice(0, 200)}.`,
            passed: validate.eligible === false,
          });
        },
      );

      await test.step(
        'PUT product new version with productDetailIdsForUpdatingProductContracts — expect validation error',
        async () => {
          const putResult = await putProductNewVersionWithContractDetailIds(
            Request,
            Endpoints,
            productId,
            editPayload!,
            [productV1DetailId],
          );

          TestRunSummary.registerPayload('productPutExpectValidationError', {
            productId,
            productDetailIdsForUpdatingProductContracts: [productV1DetailId],
            httpStatus: putResult.status,
            bodyPreview: putResult.bodyText.slice(0, 500),
          });

          expect(
            putResult.status,
            `SAVE must fail with HTTP >= 400; body=${putResult.bodyText.slice(0, 400)}`,
          ).toBeGreaterThanOrEqual(400);
          expect(putResult.bodyText).toMatch(PDT_2906_PRICE_COMPONENT_VALUE_NOT_DEFINED_PATTERN);

          TestRunSummary.recordCheck({
            check: 'Product save rejected — price component formula value not defined',
            expectedResult:
              'PUT product with updateExistingVersion=false and productDetailIdsForUpdatingProductContracts must fail (HTTP >= 400) with price component formula value validation text.',
            actualResult: `As expected — HTTP ${putResult.status}; body matches price component value-not-defined pattern.`,
            passed:
              putResult.status >= 400 &&
              PDT_2906_PRICE_COMPONENT_VALUE_NOT_DEFINED_PATTERN.test(putResult.bodyText),
          });
        },
      );

      await test.step('Assert product remains at version 1 after failed save', async () => {
        const snapshot = await loadProductDetailSnapshot(Request, Endpoints, productId, 1);
        expect(snapshot.version).toBe(1);

        await expect
          .poll(
            async () => {
              try {
                await loadProductDetailSnapshot(Request, Endpoints, productId, 2);
                return true;
              } catch {
                return false;
              }
            },
            { message: 'product v2 must not be created after failed PUT', timeout: 5_000 },
          )
          .toBe(false);

        TestRunSummary.recordCheck({
          check: 'Product version unchanged after failed save',
          expectedResult: 'Failed PUT must not leave product at logical version 2.',
          actualResult: `As expected — product ${productId} still at version 1 (detailId=${snapshot.detailId}).`,
          passed: snapshot.version === 1,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-2906',
          relevantEntityKeys: [...PDT_2906_VIDEO_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productV1DetailId,
            initialPriceComponentId,
            addedPriceComponentId,
            flow: 'valeri-video-save-error-price-component',
          },
        });
      });
    });
  },
);
