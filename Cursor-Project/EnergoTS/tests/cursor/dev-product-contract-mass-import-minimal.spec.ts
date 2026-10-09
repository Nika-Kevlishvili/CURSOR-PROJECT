/**
 * Purpose: Dev data creation — product contract CREATE via mass import with minimal Excel fields.
 *
 * Confluence: Mass import of product contract (page ID 9568493)
 * https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/9568493/Mass+import+of+product+contract
 * CREATE rule: empty Contract_number + empty Contract_version + empty Contract_create_edit = create new contract.
 *
 * Product used in MI create must have fixed contract term, invoice payment term, price components,
 * and advance payments (existing `createFixedParameterProductChain`). Same validation as UI create.
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.spec.ts
 * - tests/cursor/PDT-2931-skip-risklist-product-contract-mass-import.fixtures.ts
 * - tests/cursor/dev-volume-invoice-custom-template.spec.ts (DEV-DATA naming)
 * - mass-imports/massImportPayloads/productContractMpPayload.ts
 *
 * Run hint:
 *   $env:BASE_URL='http://10.236.20.11:8091'
 *   npx playwright test tests/cursor/dev-product-contract-mass-import-minimal.spec.ts --project=main
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
  buildProcessPreviewLink,
} from './shared/manual-verification-links.fixtures';
import {
  createFreshTestCustomer,
  createFixedParameterProductChain,
  resolveActiveInterestRateName,
  type ProductContractMiRow,
} from './PDT-2931-skip-risklist-product-contract-mass-import.fixtures';
import {
  createMinimalConsumerPod,
  runMinimalProductContractMassImportFlow,
  waitAndLoadCreatedProductContractForCustomer,
} from './dev-product-contract-mass-import-minimal.fixtures';

test.describe('[DEV-DATA]: Product contract mass import with minimal data', {
  tag: ['@massImport', '@dev', '@dev-data'],
}, () => {
  test('[DEV-DATA]: Product contract mass import with minimal data', async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(20 * 60 * 1000);

    const customer = await test.step('Precondition: fresh legal customer', async () => {
      const created = await createFreshTestCustomer(Request, GeneratePayload, Responses, Endpoints);
      TestRunSummary.registerPayload('customer', created);
      return created;
    });

    const pod = await test.step(
      'Precondition: minimal CONSUMER POD (generator default consumption)',
      async () => {
        const created = await createMinimalConsumerPod(Request, GeneratePayload, Responses, Endpoints);
        TestRunSummary.registerPayload('pod', created);
        return created;
      },
    );

    const product = await test.step('Precondition: fixed-parameter product', async () => {
      const created = await createFixedParameterProductChain(
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
      );
      TestRunSummary.registerPayload('product', created);
      return created;
    });

    const interestRateName = await test.step('Precondition: active interest rate name', async () => {
      return resolveActiveInterestRateName(Request);
    });

    const massImportCreateRow: ProductContractMiRow = {
      kind: 'create',
      customerIdentifier: customer.identifier,
      customerVersion: customer.customerVersionId,
      productId: product.productId,
      productVersion: product.productVersion,
      podIdentifier: pod.podIdentifier,
      interestRateName,
    };
    TestRunSummary.registerPayload('massImportCreateRow', massImportCreateRow);

    const miResult = await test.step(
      'POST mass-import/PRODUCT_CONTRACTS/files/upload — expect 202 ACCEPTED',
      async () => {
        const result = await runMinimalProductContractMassImportFlow(Request, FileUploadRequest, [
          massImportCreateRow,
        ]);
        expect(result.uploadStatus, 'mass import upload must be accepted').toBe(202);
        expect(result.reportRows.length, result.reportSnapshot.summary).toBe(0);
        TestRunSummary.registerPayload('processId', { processId: result.processId });
        TestRunSummary.recordCheck({
          check: 'Mass import upload 202',
          expectedResult: 'POST mass-import/PRODUCT_CONTRACTS/files/upload returns 202 ACCEPTED.',
          actualResult: `As expected — upload status ${result.uploadStatus}.`,
          passed: result.uploadStatus === 202,
        });
        TestRunSummary.recordCheck({
          check: 'Mass import process completed with no report errors',
          expectedResult:
            'PRODUCT_CONTRACT_MASS_IMPORT process reaches COMPLETED and MASS_IMPORT_ERROR_REPORT has no failed rows.',
          actualResult: `As expected — processId=${result.processId}, blockingFailures=${result.reportRows.length}; leftover template rows ignored.`,
          passed: result.reportRows.length === 0,
        });
        return result;
      },
    );

    const createdContract = await test.step(
      'POST product-contract/list + GET product-contract/{id} — wait and persist created contract',
      async () => {
        const loaded = await waitAndLoadCreatedProductContractForCustomer(Request, {
          customerId: customer.customerId,
          customerIdentifier: customer.identifier,
          customerNumber: customer.customerNumber,
        });
        expect(loaded.id, 'created contract id').toBeGreaterThan(0);
        expect(loaded.contractNumber, 'created contract number').toBeTruthy();
        Responses.productContract.push(loaded.detail);
        TestRunSummary.registerPayload('createdContract', {
          id: loaded.id,
          contractNumber: loaded.contractNumber,
        });
        TestRunSummary.recordCheck({
          check: 'Created product contract exists for test customer',
          expectedResult:
            'After COMPLETED mass import, product-contract/list returns a row matching customerId and GET detail succeeds.',
          actualResult: `As expected — contractId=${loaded.id}, contractNumber=${loaded.contractNumber}.`,
          passed: true,
        });
        return loaded;
      },
    );

    await test.step('Attach test run summary', async () => {
      const extraLinks: Record<string, string[]> = {
        ...buildProductContractTabLinks(createdContract.id),
      };
      const processUrl = buildProcessPreviewLink(miResult.processId);
      if (processUrl) {
        extraLinks.process = [processUrl];
      }

      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: 'DEV-DATA',
        relevantEntityKeys: ['customer', 'pod', 'product', 'productContract', 'process'],
        extraLinks: Object.keys(extraLinks).length ? extraLinks : undefined,
        snapshot: {
          processId: miResult.processId,
          contractId: createdContract.id,
          contractNumber: createdContract.contractNumber,
          customerIdentifier: customer.identifier,
          podIdentifier: pod.podIdentifier,
          productId: product.productId,
        },
      });
    });
  });
});
