/**
 * PDT-2931 — Skip RiskList permission must be applied to product contract mass imports.
 *
 * Risk trigger: **high POD consumption** (`estimatedMonthlyAvgConsumption` at Phoenix max), not DENY UIC hunting.
 * Fresh customer + max-consumption CONSUMER POD per test. Without `skip_risklist` → risk restriction;
 * with `skip_risklist` → create / edit / mass import must succeed.
 *
 * Backend TC source: Cursor-Project/test_cases/Backend/Skip_Risklist_Product_Contract_Mass_Import_PDT_2931.md
 *
 * Reference spec(s):
 * - tests/cursor/PDT-2906-product-new-version-contract-update.spec.ts
 * - tests/cursor/pdt-2854-pod-active-two-contracts.fixtures.ts
 * - tests/cursor/pdt-2815-version-validity.fixtures.ts (consumption formula)
 * - mass-imports/generators/massImportGenerator.ts
 * - mass-imports/massImportPayloads/productContractMpPayload.ts
 */

import { test, expect } from '../../fixtures/baseFixture';
import {
  applyHighConsumptionContractAdditionalParameters,
  assertPositiveSkipRiskListMassImportReportSnapshot,
  attachPdt2931PortalLinks,
  buildProductContractChain,
  countProductContractsForCustomer,
  waitForProductContractCount,
  createSignedBaselineForChain,
  withSecondaryConsumerPod,
  futureStartDateIso,
  loadProductContractVersionCount,
  portalContextFromChain,
  expectProductContractPostSucceeded,
  PDT_2931_SKIP_RISKLIST_REQUIRED_MSG,
  runProductContractMassImportFlow,
} from './PDT-2931-skip-risklist-product-contract-mass-import.fixtures';

const SUITE = '[PDT-2931] Skip RiskList on product contract mass import';

test.describe(SUITE, { tag: '@massImport' }, () => {
  test(`${SUITE} — mass import creates a new contract (high-consumption customer)`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);
    let chain: Awaited<ReturnType<typeof buildProductContractChain>>;

    await test.step('Precondition: fresh customer, max-consumption CONSUMER POD, product chain', async () => {
      chain = await buildProductContractChain(Request, GeneratePayload, Responses, Endpoints);
    });

    let processId = 0;

    await test.step('POST mass-import/product-contracts/files/upload — expect 202 ACCEPTED', async () => {
      const result = await runProductContractMassImportFlow(Request, FileUploadRequest, [
        {
          kind: 'create',
          customerIdentifier: chain.riskCustomer.identifier,
          customerVersion: chain.riskCustomer.customerVersionId,
          productId: chain.productId,
          productVersion: chain.productVersion,
          podIdentifier: chain.podIdentifier,
          interestRateName: chain.interestRateName,
        },
      ]);
      processId = result.processId;
      expect(result.uploadStatus).toBe(202);
      assertPositiveSkipRiskListMassImportReportSnapshot(result.reportSnapshot);
    });

    await test.step('GET product-contract list — new contract exists for test customer', async () => {
      const count = await waitForProductContractCount(
        Request,
        chain.riskCustomer.identifier,
        chain.riskCustomer.customerId,
        1,
        chain.riskCustomer.customerNumber,
      );
      expect(count, 'customer must have at least one product contract after successful create MI').toBeGreaterThan(0);
    });

    attachPdt2931PortalLinks(Responses, {
      testCase: 'Mass import — create new contract',
      processId,
      ...portalContextFromChain(chain),
    });
  });

  test(`${SUITE} — mass import edits signed contract, same version (Excel column E)`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);
    let chain: Awaited<ReturnType<typeof buildProductContractChain>>;
    let baseline: Awaited<ReturnType<typeof createSignedBaselineForChain>>;

    await test.step('Precondition: fresh customer, max-consumption POD, product, signed baseline', async () => {
      chain = await buildProductContractChain(Request, GeneratePayload, Responses, Endpoints);
      baseline = await createSignedBaselineForChain(Request, GeneratePayload, Responses, Endpoints, chain);
    });

    const versionsBefore = await loadProductContractVersionCount(Request, baseline.contractId);
    let processId = 0;

    await test.step('POST mass-import upload — edit same version (E)', async () => {
      const result = await runProductContractMassImportFlow(Request, FileUploadRequest, [
        {
          kind: 'editCurrentVersion',
          contractNumber: baseline.contractNumber,
          version: baseline.versionNumber,
          contractDetailType: baseline.contractDetailType,
          customerIdentifier: chain.riskCustomer.identifier,
          customerVersion: chain.riskCustomer.customerVersionId,
          productId: chain.productId,
          productVersion: chain.productVersion,
          podIdentifier: chain.podIdentifier,
          interestRateName: chain.interestRateName,
        },
      ]);
      processId = result.processId;
      expect(result.uploadStatus).toBe(202);
      assertPositiveSkipRiskListMassImportReportSnapshot(result.reportSnapshot);
    });

    await test.step('GET product-contract — version count unchanged after same-version edit', async () => {
      const versionsAfter = await loadProductContractVersionCount(Request, baseline.contractId);
      expect(versionsAfter).toBe(versionsBefore);
      expect(versionsAfter).toBeGreaterThanOrEqual(1);
    });

    attachPdt2931PortalLinks(Responses, {
      testCase: 'Mass import — edit same version (E)',
      processId,
      baselineContractId: baseline.contractId,
      baselineContractNumber: baseline.contractNumber,
      ...portalContextFromChain(chain),
    });
  });

  test(`${SUITE} — mass import adds new contract version (Excel column C)`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);
    let chain: Awaited<ReturnType<typeof buildProductContractChain>>;
    let baseline: Awaited<ReturnType<typeof createSignedBaselineForChain>>;
    const newVersionStart = futureStartDateIso();

    await test.step('Precondition: fresh customer, max-consumption POD, product, signed baseline', async () => {
      chain = await buildProductContractChain(Request, GeneratePayload, Responses, Endpoints);
      baseline = await createSignedBaselineForChain(Request, GeneratePayload, Responses, Endpoints, chain);
    });

    const versionsBefore = await loadProductContractVersionCount(Request, baseline.contractId);
    let processId = 0;

    await test.step('POST mass-import upload — edit new version (C)', async () => {
      const result = await runProductContractMassImportFlow(Request, FileUploadRequest, [
        {
          kind: 'editNewVersion',
          contractNumber: baseline.contractNumber,
          version: baseline.versionNumber,
          startDate: newVersionStart,
          contractDetailType: baseline.contractDetailType,
          customerIdentifier: chain.riskCustomer.identifier,
          customerVersion: chain.riskCustomer.customerVersionId,
          productId: chain.productId,
          productVersion: chain.productVersion,
          podIdentifier: chain.podIdentifier,
          interestRateName: chain.interestRateName,
        },
      ]);
      processId = result.processId;
      expect(result.uploadStatus).toBe(202);
      assertPositiveSkipRiskListMassImportReportSnapshot(result.reportSnapshot);
    });

    await test.step('GET product-contract — new version persisted', async () => {
      const versionsAfter = await loadProductContractVersionCount(Request, baseline.contractId);
      expect(versionsAfter).toBeGreaterThan(versionsBefore);
    });

    attachPdt2931PortalLinks(Responses, {
      testCase: 'Mass import — new version (C)',
      processId,
      baselineContractId: baseline.contractId,
      baselineContractNumber: baseline.contractNumber,
      note: `new version startDate column: ${newVersionStart}`,
      ...portalContextFromChain(chain),
    });
  });

  test(`${SUITE} — one Excel file: create + edit E + edit C (three rows)`, async ({
    Request,
    FileUploadRequest,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);
    let chain: Awaited<ReturnType<typeof buildProductContractChain>>;
    let baseline: Awaited<ReturnType<typeof createSignedBaselineForChain>>;
    const newVersionStart = futureStartDateIso();

    await test.step('Precondition: fresh customer, max-consumption PODs, product, signed baseline', async () => {
      chain = await buildProductContractChain(Request, GeneratePayload, Responses, Endpoints);
      chain = await withSecondaryConsumerPod(chain, Request, GeneratePayload, Responses, Endpoints);
      baseline = await createSignedBaselineForChain(Request, GeneratePayload, Responses, Endpoints, chain);
    });

    const versionsBefore = await loadProductContractVersionCount(Request, baseline.contractId);
    const contractCountBefore = await countProductContractsForCustomer(
      Request,
      chain.riskCustomer.identifier,
      chain.riskCustomer.customerId,
    );
    let processId = 0;

    await test.step('POST mass-import upload — three-row file (rows 2–4)', async () => {
      const result = await runProductContractMassImportFlow(Request, FileUploadRequest, [
        {
          kind: 'create',
          customerIdentifier: chain.riskCustomer.identifier,
          customerVersion: chain.riskCustomer.customerVersionId,
          productId: chain.productId,
          productVersion: chain.productVersion,
          podIdentifier: chain.secondaryPodIdentifier ?? chain.podIdentifier,
          interestRateName: chain.interestRateName,
        },
        {
          kind: 'editCurrentVersion',
          contractNumber: baseline.contractNumber,
          version: baseline.versionNumber,
          contractDetailType: baseline.contractDetailType,
          customerIdentifier: chain.riskCustomer.identifier,
          customerVersion: chain.riskCustomer.customerVersionId,
          productId: chain.productId,
          productVersion: chain.productVersion,
          podIdentifier: chain.podIdentifier,
          interestRateName: chain.interestRateName,
          marginalPrice: '15',
        },
        {
          kind: 'editNewVersion',
          contractNumber: baseline.contractNumber,
          version: baseline.versionNumber,
          startDate: newVersionStart,
          contractDetailType: baseline.contractDetailType,
          customerIdentifier: chain.riskCustomer.identifier,
          customerVersion: chain.riskCustomer.customerVersionId,
          productId: chain.productId,
          productVersion: chain.productVersion,
          podIdentifier: chain.podIdentifier,
          interestRateName: chain.interestRateName,
        },
      ]);
      processId = result.processId;
      expect(result.uploadStatus).toBe(202);
      assertPositiveSkipRiskListMassImportReportSnapshot(result.reportSnapshot);
    });

    await test.step('GET product-contract — create row + new version reflected', async () => {
      const contractCountAfter = await waitForProductContractCount(
        Request,
        chain.riskCustomer.identifier,
        chain.riskCustomer.customerId,
        contractCountBefore + 1,
        chain.riskCustomer.customerNumber,
      );
      expect(contractCountAfter).toBeGreaterThan(contractCountBefore);
      const versionsAfter = await loadProductContractVersionCount(Request, baseline.contractId);
      expect(versionsAfter).toBeGreaterThan(versionsBefore);
    });

    attachPdt2931PortalLinks(Responses, {
      testCase: 'Mass import — combined create + edit E + edit C',
      processId,
      baselineContractId: baseline.contractId,
      baselineContractNumber: baseline.contractNumber,
      note: 'Three rows: create + edit E + edit C',
      ...portalContextFromChain(chain),
    });
  });

  test(`${SUITE} — direct POST product-contract bypasses Risk List (API parity)`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
  }) => {
    test.setTimeout(20 * 60 * 1000);

    let chain: Awaited<ReturnType<typeof buildProductContractChain>>;
    let contractId = 0;

    await test.step('Precondition: fresh customer, max-consumption CONSUMER POD, product chain', async () => {
      chain = await buildProductContractChain(Request, GeneratePayload, Responses, Endpoints);
    });

    await test.step('POST /product-contract — expect 200/201 without risk rejection', async () => {
      const customerIndex = Responses.customer.length - 1;
      const productIndex = Responses.product.length - 1;
      const podIndex = Responses.pod.length - 1;
      const contractPayload = (await GeneratePayload.contractsAndOrders.product_contract(
        customerIndex,
        productIndex,
        podIndex,
      )) as Record<string, unknown>;
      applyHighConsumptionContractAdditionalParameters(contractPayload);
      const pp = contractPayload.productParameters as Record<string, unknown>;
      pp.contractType = 'COMBINED';
      pp.entryIntoForce = 'SIGNING';
      pp.startOfContractInitialTerm = 'SIGNING';
      pp.supplyActivation = 'FIRST_DAY_OF_MONTH';
      const res = await Request.post(Endpoints.productContract, { data: contractPayload });
      const raw = await res.text();
      expectProductContractPostSucceeded(res.status(), raw, 'Direct POST /product-contract');
      expect(raw, PDT_2931_SKIP_RISKLIST_REQUIRED_MSG).not.toContain('RISK_LIST_DECISION');
      expect(raw.toLowerCase(), PDT_2931_SKIP_RISKLIST_REQUIRED_MSG).not.toContain('risk assessment restriction');
      const body = JSON.parse(raw);
      Responses.productContract.push(body);
      contractId = Number(body.id);
      expect(contractId).toBeGreaterThan(0);
    });

    await test.step('GET /product-contract/{id} — contract linked to test customer', async () => {
      const detail = await Request.get(`${Endpoints.productContract}/${contractId}`);
      await expect(detail).CheckResponse();
      const body = await detail.json();
      expect(Number(body.basicParameters?.customerId ?? body.customerId)).toBe(chain.riskCustomer.customerId);
    });

    attachPdt2931PortalLinks(Responses, {
      testCase: 'Direct POST — no mass import',
      directContractId: contractId,
      note: 'Direct POST /product-contract (no mass import process)',
      ...portalContextFromChain(chain),
    });
  });
});
