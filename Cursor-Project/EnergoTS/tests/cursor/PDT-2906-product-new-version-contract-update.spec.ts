/**

 * PDT-2906 — Create product new version and apply it to existing contracts.

 * Backend API flow equivalent to ProductVersionsModalComponent popup + apply.

 *

 * Reference spec(s):

 * - tests/cursor/PDT-2854-pod-active-two-contracts.spec.ts

 * - tests/cursor/pdt-2815-version-validity.fixtures.ts

 * - tests/salesPortal/getProductList.spec.ts (~2408)

 */



import { test, expect } from './cursor-test.fixtures';

import {

  buildProductContractTabLinks,

  finalizeTestRunSummary,

} from './shared/manual-verification-links.fixtures';

import {

  applyFixedParameterProductFields,

  applyFixedParameterTermFields,

  assertContractVersionProductMatches,

  buildProductNewVersionEditPayload,

  createSignedProductContractV1,

  createSignedProductContractV2StillOnProductV1,

  entityId,

  loadProductDetailSnapshot,

  loadContractVersionProductRef,

} from './pdt-2906-product-new-version-contract-update.fixtures';



const PDT_2906_RELEVANT_ENTITIES = [

  'terms',

  'priceComponent',

  'product',

  'customer',

  'pod',

  'productContract',

] as const;



test.describe(

  '[PDT-2906]: Create product new version and apply it to existing contracts',

  { tag: '@productAndServices' },

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

      let contractPayload: Record<string, unknown> = {};

      let productV1DetailId = 0;



      await test.step('Precondition: term', async () => {

        const termPayload = GeneratePayload.productAndServices.term();

        applyFixedParameterTermFields(termPayload);

        TestRunSummary.registerPayload('terms', termPayload);

        const term = await Request.post(Endpoints.terms, { data: termPayload });

        await expect(term).CheckResponse();

        Responses.terms.push(await term.json());

      });



      await test.step('Precondition: price component', async () => {

        const pricePayload = GeneratePayload.productAndServices.electricity();

        TestRunSummary.registerPayload('priceComponent', pricePayload);

        const price = await Request.post(Endpoints.priceComponent, { data: pricePayload });

        await expect(price).CheckResponse();

        Responses.priceComponent.push(await price.json());

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



      await test.step('Precondition: product contract v1 → SIGNED', async () => {

        const created = await createSignedProductContractV1(

          Request,

          GeneratePayload,

          Responses,

          Endpoints,

        );

        contractId = created.contractId;

        contractPayload = created.contractPayload;

        TestRunSummary.registerPayload('productContractV1', created.contractPayload);

      });



      await test.step('Precondition: product contract v2 (savingAsNewVersion) on product v1', async () => {

        const v1Product = await loadProductDetailSnapshot(Request, Endpoints, productId, 1);

        productV1DetailId = v1Product.detailId;



        await createSignedProductContractV2StillOnProductV1(

          Request,

          Endpoints,

          contractId,

          contractPayload,

        );



        const contractV2Ref = await loadContractVersionProductRef(Request, contractId, 2);

        expect(contractV2Ref.productVersionId, 'contract v2 must still reference product v1').toBe(1);

        expect(contractV2Ref.productDetailId, 'contract v2 productDetailId must be v1 detail').toBe(

          productV1DetailId,

        );

        TestRunSummary.recordCheck({
          check: 'Contract v2 before product apply — still on product v1',
          expectedResult:
            'After saving the contract as a new version (v2), it must still reference product version 1 until the product new version is applied.',
          actualResult: `As expected — contract v2 references productVersionId=1 and productDetailId=${productV1DetailId} (same as product v1 detail).`,
          passed: true,
        });

      });



      let editPayload: Awaited<ReturnType<typeof buildProductNewVersionEditPayload>>;



      await test.step('GET product v1 — capture detailId', async () => {

        const snapshot = await loadProductDetailSnapshot(Request, Endpoints, productId, 1);

        productV1DetailId = snapshot.detailId;

        expect(snapshot.version).toBe(1);

        TestRunSummary.recordCheck({
          check: 'Product v1 detail captured before apply',
          expectedResult: 'Product must be at version 1 with a valid detailId used for contract update selection.',
          actualResult: `As expected — product version is 1; detailId ${productV1DetailId} will be passed to productDetailIdsForUpdatingProductContracts.`,
          passed: true,
        });

      });



      await test.step('Build edit payload for product new version (updateExistingVersion: false)', async () => {

        editPayload = await buildProductNewVersionEditPayload(GeneratePayload, 0);

        expect(editPayload.updateExistingVersion).toBe(false);

        TestRunSummary.registerPayload('productNewVersionEdit', editPayload);

        TestRunSummary.recordCheck({
          check: 'Product edit creates a new version (not in-place update)',
          expectedResult: 'updateExistingVersion must be false so PUT creates product v2 instead of overwriting v1.',
          actualResult: 'As expected — updateExistingVersion is false in the edit payload.',
          passed: true,
        });

      });



      await test.step('POST /products/validate-product-related-contract-update — expect true', async () => {

        const validateRes = await Request.post(

          `${Endpoints.product}/validate-product-related-contract-update`,

          { data: editPayload },

        );

        await expect(validateRes).CheckResponse();

        const eligible = (await validateRes.json()) as boolean;

        expect(eligible, 'fixed-parameter product must be eligible for contract update popup').toBe(true);

        TestRunSummary.recordCheck({
          check: 'Product eligible for contract update popup (validate endpoint)',
          expectedResult:
            'Fixed-parameter product must be eligible — POST validate-product-related-contract-update returns true.',
          actualResult: 'As expected — API returned true; product can be applied to existing contracts.',
          passed: true,
        });

      });



      await test.step('PUT product with productDetailIdsForUpdatingProductContracts', async () => {

        editPayload.productDetailIdsForUpdatingProductContracts = [productV1DetailId];

        TestRunSummary.registerPayload('productPutApplyToContracts', {

          productId,

          productDetailIdsForUpdatingProductContracts: editPayload.productDetailIdsForUpdatingProductContracts,

          updateExistingVersion: editPayload.updateExistingVersion,

        });

        const putRes = await Request.put(`${Endpoints.product}/${productId}`, { data: editPayload });

        await expect(putRes).CheckResponse();

        TestRunSummary.recordCheck({
          check: 'Apply product new version to selected contract details',
          expectedResult:
            'PUT product with productDetailIdsForUpdatingProductContracts must succeed and create product v2.',
          actualResult: `As expected — PUT returned HTTP ${putRes.status()}; product new version apply accepted.`,
          passed: putRes.ok(),
        });

      });



      await test.step('GET product-contract v2 — product ref must be NEW product version (v2)', async () => {

        const productV2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);

        expect(productV2.version).toBe(2);

        expect(productV2.detailId, 'product v2 detailId must differ from v1').not.toBe(productV1DetailId);



        TestRunSummary.recordCheck({
          check: 'Product v2 exists after apply',
          expectedResult:
            'After PUT, product must have version 2 with a new detailId different from product v1 detail.',
          actualResult: `As expected — product version is ${productV2.version}; detailId changed from ${productV1DetailId} to ${productV2.detailId}.`,
          passed: true,
        });

        await assertContractVersionProductMatches(

          Request,

          contractId,

          2,

          {

            productDetailId: productV2.detailId,

            productVersionId: 2,

          },

          'contract v2 after product apply',

        );



        const contractV2After = await loadContractVersionProductRef(Request, contractId, 2);

        TestRunSummary.recordCheck({
          check: 'Contract v2 updated to product v2 after apply',
          expectedResult:
            'Contract version 2 must reference productVersionId=2 and the new product v2 detailId after product apply.',
          actualResult: `As expected — contract v2 now has productVersionId=${contractV2After.productVersionId} and productDetailId=${contractV2After.productDetailId} (matches product v2).`,
          passed:
            contractV2After.productVersionId === 2 &&
            contractV2After.productDetailId === productV2.detailId,
        });

      });



      await test.step('Attach test run summary', async () => {

        finalizeTestRunSummary(TestRunSummary, Responses, {

          jiraKey: 'PDT-2906',

          relevantEntityKeys: [...PDT_2906_RELEVANT_ENTITIES],

          extraLinks: buildProductContractTabLinks(contractId),

          snapshot: {

            productId,

            contractId,

            productV1DetailId,

            productVersionAfterApply: 2,

          },

        });

      });

    });

  },

);


