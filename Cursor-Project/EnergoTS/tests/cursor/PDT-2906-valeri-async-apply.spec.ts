/**
 * PDT-2906 — Valeri async bug path: product popup apply on ACTIVE_IN_TERM contract with past start_date
 * spawns contract v2 via ProductContractCreateNewVersionEvent. Fresh contracts get product v2 on v2;
 * Valeri reference contract 41260 on Test still shows the known bug (product v1 refs on contract v2).
 *
 * Run on Dev:
 *   $env:BASE_URL="http://10.236.20.11:8091"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-2906-valeri-async-apply.spec.ts --project=main
 *
 * Run on Test:
 *   $env:BASE_URL="https://testapps.energo-pro.bg/backend/phoenix-epres"
 *   npx playwright test --project=setup
 *   npx playwright test tests/cursor/PDT-2906-valeri-async-apply.spec.ts --project=main
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2906-product-new-version-contract-update.spec.ts (happy path — manual v2)
 * - tests/cursor/pdt-2906-product-new-version-contract-update.fixtures.ts
 * - tests/cursor/pdt-2960-proforma-liability-due-date.fixtures.ts (poll pattern)
 */

import { configuredBaseURL } from '../../fixtures/utils/baseUrl';
import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import {
  applyFixedParameterProductFields,
  applyFixedParameterTermFields,
  applyProductNewVersionToRelatedContracts,
  contractHasLogicalVersion,
  createSignedProductContractV1WithPastStartDate,
  entityId,
  firstDayOfPreviousCalendarMonthIso,
  loadContractVersionProductRef,
  loadProductContract,
  loadProductDetailSnapshot,
  maxContractLogicalVersion,
  pollUntilContractLogicalVersion,
} from './pdt-2906-product-new-version-contract-update.fixtures';

const PDT_2906_VALERI_REFERENCE_CONTRACT_ID = 41260;
const PDT_2906_VALERI_REFERENCE_PRODUCT_V1_DETAIL_ID = 7556;

const PDT_2906_VALERI_RELEVANT_ENTITIES = [
  'terms',
  'priceComponent',
  'product',
  'customer',
  'pod',
  'productContract',
] as const;

const BUG_REPRO_NOTE =
  'Known bug on Valeri reference contract 41260 (Test) — contract v2 keeps productVersionId=1 and productDetailId=7556.';

function isTestBaseUrl(): boolean {
  const raw = (configuredBaseURL || process.env.BASE_URL || '').toLowerCase();
  return raw.includes('testapps');
}

test.describe(
  '[PDT-2906]: Valeri async apply — contract v2 from product popup stays on product v1',
  { tag: '@productAndServices' },
  () => {
    test('[PDT-2906]: Valeri async apply — contract v2 from product popup stays on product v1', async ({
      Request,
      GeneratePayload,
      Responses,
      Endpoints,
      TestRunSummary,
    }) => {
      test.setTimeout(20 * 60 * 1000);

      const pastStartDate = firstDayOfPreviousCalendarMonthIso();
      let productId = 0;
      let contractId = 0;
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

      await test.step(
        'Precondition: ACTIVE_IN_TERM contract v1 with past start_date and POD activated (no manual v2)',
        async () => {
          const created = await createSignedProductContractV1WithPastStartDate(
            Request,
            GeneratePayload,
            Responses,
            Endpoints,
            pastStartDate,
          );
          contractId = created.contractId;
          productV1DetailId = created.productV1DetailId;
          TestRunSummary.registerPayload('productContractV1PastStart', {
            ...created.contractPayload,
            pastStartDate: created.pastStartDate,
            headerStatus: created.headerStatus,
            productV1DetailId: created.productV1DetailId,
          });

          const contractBody = await loadProductContract(Request, contractId);
          expect(contractHasLogicalVersion(contractBody, 2), 'must not create contract v2 manually').toBe(
            false,
          );
          expect(maxContractLogicalVersion(contractBody)).toBe(1);

          TestRunSummary.recordCheck({
            check: 'Contract v1 ACTIVE_IN_TERM with past start_date and POD activated — only v1 exists',
            expectedResult:
              'Contract header must be ACTIVE_IN_TERM (or ACTIVE_IN_PERPETUITY / ENTERED_INTO_FORCE); v1 start_date in the past; POD activated; logical version 2 must not exist before product apply.',
            actualResult: `As expected — headerStatus=${created.headerStatus}; contract v1 startDate=${pastStartDate}; max logical version is 1; productV1DetailId=${productV1DetailId}.`,
            passed: true,
          });
        },
      );

      await test.step('PUT product with productDetailIdsForUpdatingProductContracts (popup apply)', async () => {
        TestRunSummary.registerPayload('productPutApplyToContracts', {
          productId,
          productDetailIdsForUpdatingProductContracts: [productV1DetailId],
          updateExistingVersion: false,
        });

        const productV2 = await applyProductNewVersionToRelatedContracts(
          Request,
          GeneratePayload,
          0,
          productId,
          productV1DetailId,
          Endpoints,
        );
        expect(productV2.version).toBe(2);
      });

      await test.step('Poll until contract logical version 2 exists (async handler, max 120s)', async () => {
        await pollUntilContractLogicalVersion(Request, contractId, 2);
        TestRunSummary.recordCheck({
          check: 'Async handler created contract logical version 2',
          expectedResult:
            'ProductContractCreateNewVersionEvent must create contract v2 within 120 seconds after product apply.',
          actualResult: 'As expected — contract logical version 2 appeared after polling.',
          passed: true,
        });
      });

      await test.step('Assert Valeri async flow — contract v2 created', async () => {
        const contractBody = await loadProductContract(Request, contractId);
        expect(maxContractLogicalVersion(contractBody), 'fresh contract must reach logical version 2').toBe(2);

        const productV2 = await loadProductDetailSnapshot(Request, Endpoints, productId, 2);
        expect(productV2.version, 'product entity must have logical version 2').toBe(2);
        expect(productV2.detailId, 'product v2 detailId must differ from v1').not.toBe(productV1DetailId);

        const contractV2Ref = await loadContractVersionProductRef(Request, contractId, 2);
        expect(contractV2Ref.productVersionId, 'fresh contract v2 must reference product v2').toBe(2);
        expect(contractV2Ref.productDetailId, 'fresh contract v2 must reference product v2 detail').toBe(
          productV2.detailId,
        );

        TestRunSummary.recordCheck({
          check: 'Valeri async flow — contract v2 created with product v2 refs',
          expectedResult:
            'Fresh contract must reach logical version 2; product entity has v2; contract v2 references productVersionId=2 and matching product v2 detailId.',
          actualResult: `As expected — contract ${contractId} max version 2; product ${productId} v2 detailId=${productV2.detailId}; contract v2 productVersionId=${contractV2Ref.productVersionId}.`,
          passed: true,
        });
      });

      await test.step('Assert Valeri reference contract 41260 — known bug state on Test', async () => {
        if (!isTestBaseUrl()) {
          TestRunSummary.recordCheck({
            check: 'Valeri reference contract 41260 — skipped (not Test)',
            expectedResult: `${BUG_REPRO_NOTE} Asserted only when BASE_URL includes testapps.`,
            actualResult: `Skipped — BASE_URL=${configuredBaseURL || process.env.BASE_URL || '(unset)'} is not Test.`,
            passed: true,
          });
          return;
        }

        const ref = await loadContractVersionProductRef(
          Request,
          PDT_2906_VALERI_REFERENCE_CONTRACT_ID,
          2,
        );
        const bugPresent =
          ref.productVersionId === 1 &&
          ref.productDetailId === PDT_2906_VALERI_REFERENCE_PRODUCT_V1_DETAIL_ID;

        expect(
          ref.productVersionId,
          `reference contract ${PDT_2906_VALERI_REFERENCE_CONTRACT_ID} v2 productVersionId stays on 1`,
        ).toBe(1);
        expect(
          ref.productDetailId,
          `reference contract ${PDT_2906_VALERI_REFERENCE_CONTRACT_ID} v2 productDetailId stays on v1 detail`,
        ).toBe(PDT_2906_VALERI_REFERENCE_PRODUCT_V1_DETAIL_ID);

        TestRunSummary.recordCheck({
          check: 'Valeri reference contract 41260 — known bug state on Test',
          expectedResult: `${BUG_REPRO_NOTE} Expected productVersionId=1 and productDetailId=${PDT_2906_VALERI_REFERENCE_PRODUCT_V1_DETAIL_ID}.`,
          actualResult: bugPresent
            ? `As expected (bug) — contract ${PDT_2906_VALERI_REFERENCE_CONTRACT_ID} v2 has productVersionId=1 and productDetailId=${PDT_2906_VALERI_REFERENCE_PRODUCT_V1_DETAIL_ID}.`
            : `Not as expected — contract ${PDT_2906_VALERI_REFERENCE_CONTRACT_ID} v2 has productVersionId=${ref.productVersionId}, productDetailId=${ref.productDetailId}.`,
          passed: bugPresent,
        });
      });

      await test.step('Attach test run summary', async () => {
        finalizeTestRunSummary(TestRunSummary, Responses, {
          jiraKey: 'PDT-2906',
          relevantEntityKeys: [...PDT_2906_VALERI_RELEVANT_ENTITIES],
          extraLinks: buildProductContractTabLinks(contractId),
          snapshot: {
            productId,
            contractId,
            productV1DetailId,
            pastStartDate,
            valeriReferenceContractId: PDT_2906_VALERI_REFERENCE_CONTRACT_ID,
            valeriReferenceProductV1DetailId: PDT_2906_VALERI_REFERENCE_PRODUCT_V1_DETAIL_ID,
            bugReproNote: BUG_REPRO_NOTE,
            testBaseUrl: isTestBaseUrl(),
          },
        });
      });
    });
  },
);
