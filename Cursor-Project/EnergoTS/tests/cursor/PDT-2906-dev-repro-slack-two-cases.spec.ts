/**
 * PDT-2906 — Dev bug reproduction for Valeri's TWO Slack cases (July 7, 2026).
 *
 * Test A — Case 8828/42753: contract v2 stays on product v1 after name-only product new version apply.
 * Test B — Case 2780: duplicate key constraint on product new version save with contract apply.
 *
 * Run on Dev:
 *   $env:BASE_URL="http://10.236.20.11:8091"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-2906-dev-repro-slack-two-cases.spec.ts --project=main
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2906-dev-repro-valeri-bug.spec.ts (case A async poll + bug repro assert)
 * - tests/cursor/PDT-2906-valeri-async-apply.spec.ts (past start_date + ACTIVE_IN_TERM path)
 * - tests/cursor/PDT-2906-product-new-version-contract-update.spec.ts (happy path)
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
  buildProductNewVersionEditPayloadSamePriceComponents,
  buildProductNewVersionNameOnlyEditPayload,
  createSignedProductContractV1,
  createSignedProductContractV1WithPastStartDate,
  entityId,
  loadContractVersionProductRef,
  loadProductContract,
  loadProductDetailSnapshot,
  maxContractLogicalVersion,
  PDT_2906_DUPLICATE_KEY_CONSTRAINT_PATTERN,
  pollUntilContractLogicalVersion,
  postValidateProductRelatedContractUpdate,
  putProductNewVersionWithContractDetailIds,
} from './pdt-2906-product-new-version-contract-update.fixtures';

const PDT_2906_SLACK_REPRO_ENTITIES = [
  'terms',
  'priceComponent',
  'product',
  'customer',
  'pod',
  'productContract',
] as const;

test.describe(
  '[PDT-2906]: Dev Slack two-case bug repro (July 7)',
  { tag: ['@pdt-2906', '@dev', '@bug-repro'] },
  () => {
    test('[PDT-2906]: Dev slack case 8828/42753 — wrong product on contract v2 after apply', async ({
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
      let productV2DetailId = 0;
      let pastStartDate = '';

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
            passed: created.headerStatus === 'ACTIVE_IN_TERM',
          });
          expect(created.headerStatus, 'contract must be ACTIVE_IN_TERM for case 8828/42753').toBe(
            'ACTIVE_IN_TERM',
          );
        },
      );

      let editPayload: Awaited<ReturnType<typeof buildProductNewVersionNameOnlyEditPayload>>;

      await test.step(
        'Build product new version edit — change product name only (updateExistingVersion: false)',
        async () => {
          editPayload = await buildProductNewVersionNameOnlyEditPayload(GeneratePayload, 0);
          expect(editPayload.updateExistingVersion).toBe(false);
          expect(editPayload.version).toBe(1);
          expect(editPayload.name, 'name must change for Valeri screenshot repro').toContain(
            'Valeri name edit v2',
          );

          editPayload.productDetailIdsForUpdatingProductContracts = [productV1DetailId];
          TestRunSummary.registerPayload('productNewVersionNameOnlyEdit', {
            productId,
            name: editPayload.name,
            updateExistingVersion: editPayload.updateExistingVersion,
            version: editPayload.version,
            priceComponentIds: editPayload.priceComponentIds,
            productDetailIdsForUpdatingProductContracts:
              editPayload.productDetailIdsForUpdatingProductContracts,
          });
        },
      );

      await test.step('POST validate-product-related-contract-update — expect true', async () => {
        const validate = await postValidateProductRelatedContractUpdate(Request, Endpoints, editPayload!);
        expect(validate.status).toBeGreaterThanOrEqual(200);
        expect(validate.status).toBeLessThan(300);
        expect(validate.eligible, 'name-only edit must pass contract-update validation').toBe(true);

        TestRunSummary.recordCheck({
          check: 'Product eligible for contract update popup (validate endpoint)',
          expectedResult:
            'POST validate-product-related-contract-update must return true for name-only product new version edit.',
          actualResult: `As expected — eligible=${validate.eligible}.`,
          passed: validate.eligible === true,
        });
      });

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

          const bugReproduced =
            v2Ref.productVersionId === 1 && v2Ref.productDetailId === productV1DetailId;

          TestRunSummary.registerPayload('contractV2ProductRefCase8828', {
            contractId,
            productVersionId: v2Ref.productVersionId,
            productDetailId: v2Ref.productDetailId,
            productV1DetailId,
            productV2DetailId,
            bugReproduced,
            slackCase: '8828/42753',
          });

          TestRunSummary.recordCheck({
            check: 'Case 8828/42753 — contract v2 product reference (bug repro on Dev)',
            expectedResult:
              'BUG REPRODUCED when contract v2 has productVersionId=1 and productDetailId=v1 detail. Test FAILS if productVersionId=2 (fix deployed).',
            actualResult: bugReproduced
              ? `Bug reproduced — productVersionId=${v2Ref.productVersionId}; productDetailId=${v2Ref.productDetailId} (v1 detail ${productV1DetailId}); product v2 detail=${productV2DetailId}.`
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
          relevantEntityKeys: [...PDT_2906_SLACK_REPRO_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productV1DetailId,
            productV2DetailId,
            pastStartDate,
            slackCase: '8828/42753',
            flow: 'dev-repro-slack-case-a-wrong-product-on-contract-v2',
            environment: 'dev',
          },
        });
      });
    });

    test('[PDT-2906]: Dev slack case 2780 — duplicate key on product new version save with contract apply', async ({
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
      let secondPriceComponentId = 0;

      await test.step('Precondition: term', async () => {
        const termPayload = GeneratePayload.productAndServices.term();
        applyFixedParameterTermFields(termPayload);
        TestRunSummary.registerPayload('terms', termPayload);
        const term = await Request.post(Endpoints.terms, { data: termPayload });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
      });

      await test.step('Precondition: first price component for product v1', async () => {
        const pricePayload = GeneratePayload.productAndServices.electricity();
        TestRunSummary.registerPayload('priceComponentV1Primary', pricePayload);
        const price = await Request.post(Endpoints.priceComponent, { data: pricePayload });
        await expect(price).CheckResponse();
        Responses.priceComponent.push(await price.json());
        initialPriceComponentId = entityId(Responses.priceComponent[0]);
      });

      await test.step('Precondition: second price component for product v1', async () => {
        const pricePayload = GeneratePayload.productAndServices.priceSettlement();
        TestRunSummary.registerPayload('priceComponentV1Secondary', pricePayload);
        const price = await Request.post(Endpoints.priceComponent, { data: pricePayload });
        await expect(price).CheckResponse();
        Responses.priceComponent.push(await price.json());
        secondPriceComponentId = entityId(Responses.priceComponent[1]);
        expect(secondPriceComponentId).not.toBe(initialPriceComponentId);
      });

      await test.step('Precondition: fixed-parameter product v1 with two price components', async () => {
        const productPayload = GeneratePayload.productAndServices.product();
        applyFixedParameterProductFields(productPayload);
        productPayload.priceComponentIds = [initialPriceComponentId, secondPriceComponentId];
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

      await test.step('Precondition: signed product contract v1 on product v1', async () => {
        const created = await createSignedProductContractV1(
          Request,
          GeneratePayload,
          Responses,
          Endpoints,
        );
        contractId = created.contractId;
        TestRunSummary.registerPayload('productContractV1', created.contractPayload);

        const v1Ref = await loadContractVersionProductRef(Request, contractId, 1);
        expect(v1Ref.productVersionId).toBe(1);
        expect(v1Ref.productDetailId).toBe(productV1DetailId);

        TestRunSummary.recordCheck({
          check: 'Signed contract v1 exists on product v1 with price components',
          expectedResult:
            'Product contract must be SIGNED on logical version 1 referencing product v1 detail.',
          actualResult: `As expected — contractId=${contractId}; productV1DetailId=${productV1DetailId}.`,
          passed: true,
        });
      });

      let editPayload: Awaited<ReturnType<typeof buildProductNewVersionEditPayloadSamePriceComponents>>;

      await test.step(
        'Build product new version edit — same price components + apply to contract',
        async () => {
          editPayload = await buildProductNewVersionEditPayloadSamePriceComponents(GeneratePayload, 0);
          expect(editPayload.updateExistingVersion).toBe(false);
          expect(editPayload.version).toBe(1);
          expect(editPayload.priceComponentIds).toEqual(
            expect.arrayContaining([initialPriceComponentId, secondPriceComponentId]),
          );
          expect(editPayload.priceComponentIds.length).toBe(2);

          editPayload.productDetailIdsForUpdatingProductContracts = [productV1DetailId];
          TestRunSummary.registerPayload('productNewVersionSamePcsEdit', {
            productId,
            updateExistingVersion: editPayload.updateExistingVersion,
            version: editPayload.version,
            priceComponentIds: editPayload.priceComponentIds,
            productDetailIdsForUpdatingProductContracts:
              editPayload.productDetailIdsForUpdatingProductContracts,
            initialPriceComponentId,
            secondPriceComponentId,
          });
        },
      );

      await test.step(
        'PUT product new version with contract apply — expect duplicate key constraint (bug repro)',
        async () => {
          const putResult = await putProductNewVersionWithContractDetailIds(
            Request,
            Endpoints,
            productId,
            editPayload!,
            [productV1DetailId],
          );

          const bugReproduced =
            putResult.status >= 400 &&
            PDT_2906_DUPLICATE_KEY_CONSTRAINT_PATTERN.test(putResult.bodyText);

          TestRunSummary.registerPayload('productPutDuplicateKeyCase2780', {
            productId,
            productDetailIdsForUpdatingProductContracts: [productV1DetailId],
            httpStatus: putResult.status,
            bodyPreview: putResult.bodyText.slice(0, 500),
            bugReproduced,
            slackCase: '2780',
          });

          TestRunSummary.recordCheck({
            check: 'Case 2780 — duplicate key on product save with contract apply (bug repro on Dev)',
            expectedResult:
              'BUG REPRODUCED when PUT returns HTTP >= 400 and body matches idx_product_contract_price_components / duplicate key / already exists / price_component_formula_variable_id. Test FAILS on 2xx success.',
            actualResult: bugReproduced
              ? `Bug reproduced — HTTP ${putResult.status}; body matches duplicate/constraint pattern.`
              : putResult.status < 300
                ? `Bug NOT reproduced — PUT succeeded with HTTP ${putResult.status}; duplicate key bug NOT reproduced on Dev.`
                : `HTTP ${putResult.status} but body did not match duplicate/constraint pattern: ${putResult.bodyText.slice(0, 300)}`,
            passed: bugReproduced,
          });

          if (putResult.status >= 200 && putResult.status < 300) {
            expect.fail('duplicate key bug NOT reproduced on Dev — PUT succeeded with 2xx');
          }

          expect(
            putResult.status,
            `bug repro: PUT must fail with HTTP >= 400; body=${putResult.bodyText.slice(0, 400)}`,
          ).toBeGreaterThanOrEqual(400);
          expect(
            putResult.bodyText,
            'bug repro: response body must match duplicate/constraint pattern',
          ).toMatch(PDT_2906_DUPLICATE_KEY_CONSTRAINT_PATTERN);
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
            { message: 'product v2 must not be created after failed PUT (case 2780)', timeout: 5_000 },
          )
          .toBe(false);

        TestRunSummary.recordCheck({
          check: 'Product version unchanged after duplicate-key failed save',
          expectedResult: 'Failed PUT must not leave product at logical version 2.',
          actualResult: `As expected — product ${productId} still at version 1 (detailId=${snapshot.detailId}).`,
          passed: snapshot.version === 1,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-2906',
          relevantEntityKeys: [...PDT_2906_SLACK_REPRO_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productV1DetailId,
            initialPriceComponentId,
            secondPriceComponentId,
            slackCase: '2780',
            flow: 'dev-repro-slack-case-b-duplicate-key-on-save',
            environment: 'dev',
          },
        });
      });
    });
  },
);
