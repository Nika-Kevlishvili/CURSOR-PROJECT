/**
 * STANDALONE — Customer Receivables and Liabilities POD listing (Backend TC-BE-1 … TC-BE-16).
 *
 * Reference spec(s):
 * - tests/cursor/PDT-3214-easypay-liabilities-receivables-accounting-period-rules.spec.ts
 * - tests/cursor/pdt-3223-invoice-json-price-component-order.fixtures.ts
 * - tests/cursor/pdt-3013-separate-pod-reversal-offset.fixtures.ts
 * - tests/cursor/PDT-2529-rfd-data-model-happy-path.fixtures.ts
 * - tests/receivableManagement/manualLiabilityOffsetting.spec.ts
 * - tests/cursor/cursor-test.fixtures.ts
 * - tests/cursor/shared/manual-verification-links.fixtures.ts
 *
 * Backend TC: Cursor-Project/test_cases/Backend/Customer_receivables_liabilities_POD_listing.md
 */

import { test, expect } from './cursor-test.fixtures';
import {
  finalizeTestRunSummary,
  buildProductContractTabLinks,
} from './shared/manual-verification-links.fixtures';
import {
  type PodListingFx,
  type ListingRow,
  VOLUME_TIMEOUT_MS,
  createLegalCustomer,
  createLegalCustomerWithDistinctDetailId,
  createManualLiability,
  createManualReceivable,
  createDeposit,
  reduceDepositViaMlo,
  runManualContractChain,
  runVolumeInvoiceChain,
  defaultListingBody,
  postListing,
  postListingOk,
  findRowByIdRaw,
  findRowByOutgoingDocumentId,
  findDepositRow,
  isBlankBillingGroup,
  getInvoiceDetailedPods,
  extractErrorText,
} from './customer-receivables-liabilities-pod-listing.fixtures';

const JIRA_KEY = 'STANDALONE';

function fxFrom(args: {
  Request: PodListingFx['Request'];
  GeneratePayload: PodListingFx['GeneratePayload'];
  Responses: PodListingFx['Responses'];
  Endpoints: PodListingFx['Endpoints'];
  Nomenclatures: PodListingFx['Nomenclatures'];
}): PodListingFx {
  return args;
}

function expectNullPodsAndAddress(row: ListingRow, label: string): void {
  expect(row.pods, `${label} pods must be JSON null`).toBeNull();
  expect(row.address, `${label} address must be JSON null`).toBeNull();
}

test.describe('[STANDALONE]: Customer Receivables and Liabilities POD listing', {
  tag: ['@receivables', '@standalone', '@dev'],
}, () => {
  test('[STANDALONE]: TC-BE-1 – Manual liability without a billing group leaves Point of Delivery empty', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const { customerId } = await test.step('Precondition: legal customer + manual liability without billing group', async () => {
      const created = await createLegalCustomer(fx);
      const liabilityId = await createManualLiability(fx, { billingGroupId: null, initialAmount: 10 });
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      TestRunSummary.registerPayload('customerLiability', { id: liabilityId, billingGroupId: null });
      return { customerId: created.customerId, liabilityId };
    });

    const liabilityId = Responses.customerLiability[0] as number;

    const row = await test.step('Assert: listing row has null pods/address and empty billing group', async () => {
      const page = await postListingOk(fx, customerId);
      const found = findRowByIdRaw(page.content, liabilityId);
      expect(found, `listing must contain liability idRaw=${liabilityId}`).toBeTruthy();
      expect(found!.object).toBe('liability');
      expect(Number(found!.idRaw)).toBe(liabilityId);
      expect(isBlankBillingGroup(found!.billingGroup), 'billingGroup empty/null').toBe(true);
      expectNullPodsAndAddress(found!, 'TC-BE-1');
      TestRunSummary.recordCheck({
        check: 'Manual liability without billing group — POD columns null',
        expectedResult: 'HTTP 206; object=liability; pods=null; address=null; billingGroup empty/null',
        actualResult: `As expected — status 206, object=${found!.object}, pods=${String(found!.pods)}, address=${String(found!.address)}`,
        passed: true,
      });
      return found!;
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'customerLiability'],
        snapshot: { customerId, liabilityId, row },
      });
    });
  });

  test('[STANDALONE]: TC-BE-2 – Manual liability with a one-POD billing group shows that POD', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const chain = await test.step('Precondition: customer + 1-POD contract + liability on that billing group', async () => {
      const result = await runManualContractChain(fx, 1, test.info().workerIndex);
      const liabilityId = await createManualLiability(fx, {
        billingGroupId: result.billingGroupId,
        initialAmount: 10,
      });
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
      TestRunSummary.registerPayload('customerLiability', {
        id: liabilityId,
        billingGroupId: result.billingGroupId,
        podIdentifier: result.pods[0].identifier,
      });
      return { ...result, liabilityId };
    });

    const row = await test.step('Assert: listing pods equals the single POD identifier', async () => {
      const page = await postListingOk(fx, chain.customerId);
      const found = findRowByIdRaw(page.content, chain.liabilityId);
      expect(found, `listing must contain liability idRaw=${chain.liabilityId}`).toBeTruthy();
      expect(found!.object).toBe('liability');
      expect(found!.pods).toBe(chain.pods[0].identifier);
      expect(found!.address, 'address must be non-null').toBeTruthy();
      expect(isBlankBillingGroup(found!.billingGroup), 'billingGroup populated').toBe(false);
      TestRunSummary.recordCheck({
        check: 'Manual liability + 1-POD billing group shows POD (rule d)',
        expectedResult: `pods=${chain.pods[0].identifier}; address non-null; billingGroup populated`,
        actualResult: `As expected — pods=${String(found!.pods)}; address=${String(found!.address)}`,
        passed: true,
      });
      return found!;
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'customerLiability'],
        extraLinks: buildProductContractTabLinks(chain.contractId),
        snapshot: { ...chain, row },
      });
    });
  });

  test('[STANDALONE]: TC-BE-3 – Manual liability with a multi-POD billing group leaves Point of Delivery empty', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const chain = await test.step('Precondition: 2-POD contract, separateInvoice=false, liability on shared BG', async () => {
      const result = await runManualContractChain(fx, 2, test.info().workerIndex, false);
      const liabilityId = await createManualLiability(fx, {
        billingGroupId: result.billingGroupId,
        initialAmount: 10,
      });
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
      TestRunSummary.registerPayload('customerLiability', {
        id: liabilityId,
        billingGroupId: result.billingGroupId,
        identifiers: result.pods.map((p) => p.identifier),
      });
      return { ...result, liabilityId };
    });

    await test.step('Assert: listing pods/address are null; identifiers do not appear', async () => {
      const page = await postListingOk(fx, chain.customerId);
      const found = findRowByIdRaw(page.content, chain.liabilityId);
      expect(found, `listing must contain liability idRaw=${chain.liabilityId}`).toBeTruthy();
      expect(isBlankBillingGroup(found!.billingGroup), 'billingGroup populated').toBe(false);
      expectNullPodsAndAddress(found!, 'TC-BE-3');
      const podsText = String(found!.pods ?? '');
      for (const pod of chain.pods) {
        expect(podsText.includes(pod.identifier), `${pod.identifier} must not appear in pods`).toBe(false);
      }
      TestRunSummary.recordCheck({
        check: 'Manual liability + 2-POD BG leaves POD empty',
        expectedResult: 'HTTP 206; billingGroup populated; pods=null; address=null',
        actualResult: `As expected — pods=${String(found!.pods)}; address=${String(found!.address)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'customerLiability'],
        extraLinks: buildProductContractTabLinks(chain.contractId),
        snapshot: chain,
      });
    });
  });

  test('[STANDALONE]: TC-BE-4 – Manual receivable without a billing group leaves Point of Delivery empty', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const { customerId, receivableId } = await test.step(
      'Precondition: legal customer + manual receivable without billing group',
      async () => {
        const created = await createLegalCustomer(fx);
        const id = await createManualReceivable(fx, { billingGroupId: null, initialAmount: 10 });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('customerReceivable', { id, billingGroupId: null });
        return { customerId: created.customerId, receivableId: id };
      },
    );

    await test.step('Assert: listing row object=receivable with null pods/address', async () => {
      const page = await postListingOk(fx, customerId);
      const found = findRowByIdRaw(page.content, receivableId);
      expect(found, `listing must contain receivable idRaw=${receivableId}`).toBeTruthy();
      expect(found!.object).toBe('receivable');
      expectNullPodsAndAddress(found!, 'TC-BE-4');
      TestRunSummary.recordCheck({
        check: 'Manual receivable without billing group — POD columns null',
        expectedResult: 'HTTP 206; object=receivable; pods=null; address=null',
        actualResult: `As expected — object=${found!.object}; pods=${String(found!.pods)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'customerReceivable'],
        snapshot: { customerId, receivableId },
      });
    });
  });

  test('[STANDALONE]: TC-BE-5 – Manual receivable with a one-POD billing group shows that POD', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const chain = await test.step('Precondition: 1-POD contract + receivable on that billing group', async () => {
      const result = await runManualContractChain(fx, 1, test.info().workerIndex);
      const receivableId = await createManualReceivable(fx, {
        billingGroupId: result.billingGroupId,
        initialAmount: 10,
      });
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
      TestRunSummary.registerPayload('customerReceivable', {
        id: receivableId,
        billingGroupId: result.billingGroupId,
        podIdentifier: result.pods[0].identifier,
      });
      return { ...result, receivableId };
    });

    await test.step('Assert: listing pods equals identifier; address non-null', async () => {
      const page = await postListingOk(fx, chain.customerId);
      const found = findRowByIdRaw(page.content, chain.receivableId);
      expect(found, `listing must contain receivable idRaw=${chain.receivableId}`).toBeTruthy();
      expect(found!.object).toBe('receivable');
      expect(found!.pods).toBe(chain.pods[0].identifier);
      expect(found!.address, 'address must be non-null').toBeTruthy();
      TestRunSummary.recordCheck({
        check: 'Manual receivable + 1-POD billing group shows POD',
        expectedResult: `object=receivable; pods=${chain.pods[0].identifier}; address non-null`,
        actualResult: `As expected — pods=${String(found!.pods)}; address=${String(found!.address)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'customerReceivable'],
        extraLinks: buildProductContractTabLinks(chain.contractId),
        snapshot: chain,
      });
    });
  });

  test('[STANDALONE]: TC-BE-6 – Invoice-generated liability with one third-tab POD shows that POD when Separate Invoice is false', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(VOLUME_TIMEOUT_MS);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const chain = await test.step(
      'Precondition: 1-POD FOR_VOLUMES REAL invoice, separateInvoice=false, then accounting',
      async () => {
        const result = await runVolumeInvoiceChain(fx, {
          podCount: 1,
          separateInvoice: false,
          expectedInvoiceCount: 1,
        });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
        TestRunSummary.registerPayload('billingRun', { id: result.billingRunId, invoiceNumbers: 1 });
        TestRunSummary.registerPayload('invoice', { ids: result.invoiceIds });
        return result;
      },
    );

    await test.step('Assert: listing pods equals the invoice third-tab POD (rule e)', async () => {
      const invoiceId = chain.invoiceIds[0];
      const detailedPods = await getInvoiceDetailedPods(fx, invoiceId);
      expect(detailedPods, 'invoice detailed-data must have exactly one POD').toEqual([
        chain.pods[0].identifier,
      ]);
      const page = await postListingOk(fx, chain.customerId);
      const found = findRowByOutgoingDocumentId(page.content, invoiceId);
      expect(found, `listing must contain outgoingDocumentId=${invoiceId}`).toBeTruthy();
      expect(found!.object).toBe('liability');
      expect(found!.outgoingDocumentType).toBe('INVOICE');
      expect(found!.pods).toBe(chain.pods[0].identifier);
      expect(found!.address, 'address must be non-null').toBeTruthy();
      if (chain.pods[0].addressFragment) {
        expect(String(found!.address)).toContain(chain.pods[0].addressFragment);
      }
      TestRunSummary.recordCheck({
        check: 'Single-POD invoice with Separate Invoice false still shows POD (rule e / Finding F2)',
        expectedResult: `pods=${chain.pods[0].identifier}; outgoingDocumentType=INVOICE`,
        actualResult: `As expected — pods=${String(found!.pods)}; type=${String(found!.outgoingDocumentType)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice', 'customerLiability'],
        extraLinks: buildProductContractTabLinks(chain.contractId),
        snapshot: chain,
      });
    });
  });

  test('[STANDALONE]: TC-BE-7 – Invoice-generated liability with one third-tab POD shows that POD when Separate Invoice is true', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(VOLUME_TIMEOUT_MS);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const chain = await test.step(
      'Precondition: 1-POD FOR_VOLUMES REAL invoice, separateInvoice=true before billing',
      async () => {
        const result = await runVolumeInvoiceChain(fx, {
          podCount: 1,
          separateInvoice: true,
          expectedInvoiceCount: 1,
        });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
        TestRunSummary.registerPayload('billingRun', { id: result.billingRunId, invoiceNumbers: 1 });
        TestRunSummary.registerPayload('invoice', { ids: result.invoiceIds });
        return result;
      },
    );

    await test.step('Assert: listing pods equals the single invoice POD', async () => {
      const invoiceId = chain.invoiceIds[0];
      const page = await postListingOk(fx, chain.customerId);
      const found = findRowByOutgoingDocumentId(page.content, invoiceId);
      expect(found, `listing must contain outgoingDocumentId=${invoiceId}`).toBeTruthy();
      expect(found!.object).toBe('liability');
      expect(found!.outgoingDocumentType).toBe('INVOICE');
      expect(found!.pods).toBe(chain.pods[0].identifier);
      expect(found!.address, 'address must be non-null').toBeTruthy();
      if (chain.pods[0].addressFragment) {
        expect(String(found!.address)).toContain(chain.pods[0].addressFragment);
      }
      TestRunSummary.recordCheck({
        check: 'Single-POD invoice with Separate Invoice true shows POD',
        expectedResult: `pods=${chain.pods[0].identifier}; outgoingDocumentType=INVOICE`,
        actualResult: `As expected — pods=${String(found!.pods)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice', 'customerLiability'],
        extraLinks: buildProductContractTabLinks(chain.contractId),
        snapshot: chain,
      });
    });
  });

  test('[STANDALONE]: TC-BE-8 – Invoice-generated liability with more than one third-tab POD and Separate Invoice false leaves Point of Delivery empty', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(VOLUME_TIMEOUT_MS);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const chain = await test.step(
      'Precondition: 2 PODs, separateInvoice=false, one combined REAL invoice with ≥2 detailed-data PODs',
      async () => {
        const result = await runVolumeInvoiceChain(fx, {
          podCount: 2,
          separateInvoice: false,
          expectedInvoiceCount: 1,
        });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
        TestRunSummary.registerPayload('billingRun', { id: result.billingRunId, invoiceNumbers: 1 });
        TestRunSummary.registerPayload('invoice', { ids: result.invoiceIds });
        return result;
      },
    );

    await test.step('Assert: combined invoice listing pods/address are null', async () => {
      const invoiceId = chain.invoiceIds[0];
      const page = await postListingOk(fx, chain.customerId);
      const found = findRowByOutgoingDocumentId(page.content, invoiceId);
      expect(found, `listing must contain outgoingDocumentId=${invoiceId}`).toBeTruthy();
      expect(found!.object).toBe('liability');
      expect(found!.outgoingDocumentType).toBe('INVOICE');
      expectNullPodsAndAddress(found!, 'TC-BE-8');
      TestRunSummary.recordCheck({
        check: 'Multi-POD combined invoice leaves POD empty',
        expectedResult: 'HTTP 206; object=liability; outgoingDocumentType=INVOICE; pods=null; address=null',
        actualResult: `As expected — pods=${String(found!.pods)}; address=${String(found!.address)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice', 'customerLiability'],
        extraLinks: buildProductContractTabLinks(chain.contractId),
        snapshot: chain,
      });
    });
  });

  test('[STANDALONE]: TC-BE-9 – Two PODs with Separate Invoice true produce two invoices, each listing shows its own single POD', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    test.setTimeout(VOLUME_TIMEOUT_MS);
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const chain = await test.step(
      'Precondition: 2 PODs, separateInvoice=true BEFORE billing → two REAL invoices, one POD each',
      async () => {
        const result = await runVolumeInvoiceChain(fx, {
          podCount: 2,
          separateInvoice: true,
          expectedInvoiceCount: 2,
        });
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
        TestRunSummary.registerPayload('billingRun', { id: result.billingRunId, invoiceNumbers: 2 });
        TestRunSummary.registerPayload('invoice', { ids: result.invoiceIds });
        return result;
      },
    );

    await test.step('Assert: each listing row shows that invoice’s single POD', async () => {
      const page = await postListingOk(fx, chain.customerId);
      const expectedIdentifiers = new Set(chain.pods.map((p) => p.identifier));
      const seenPods: string[] = [];
      for (const invoiceId of chain.invoiceIds) {
        const detailedPods = await getInvoiceDetailedPods(fx, invoiceId);
        expect(
          detailedPods.length,
          `invoice ${invoiceId} must have exactly one detailed-data POD`,
        ).toBe(1);
        const podOnInvoice = detailedPods[0];
        const found = findRowByOutgoingDocumentId(page.content, invoiceId);
        expect(found, `listing must contain outgoingDocumentId=${invoiceId}`).toBeTruthy();
        expect(found!.object).toBe('liability');
        expect(found!.outgoingDocumentType).toBe('INVOICE');
        expect(found!.pods).toBe(podOnInvoice);
        expect(String(found!.pods ?? '')).not.toContain(',');
        expect(found!.address, 'address must be non-null').toBeTruthy();
        const matchingPod = chain.pods.find((p) => p.identifier === podOnInvoice);
        if (matchingPod?.addressFragment) {
          expect(String(found!.address)).toContain(matchingPod.addressFragment);
        }
        seenPods.push(podOnInvoice);
      }
      expect(new Set(seenPods).size, 'the two rows must show different PODs').toBe(2);
      for (const id of seenPods) {
        expect(expectedIdentifiers.has(id), `${id} must be one of the contract PODs`).toBe(true);
      }
      TestRunSummary.recordCheck({
        check: 'Two invoices / two listing rows each show their own POD',
        expectedResult: 'Two liability/INVOICE rows; each pods equals that invoice’s single pointOfDelivery',
        actualResult: `As expected — pods=${JSON.stringify(seenPods)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'billingRun', 'invoice', 'customerLiability'],
        extraLinks: buildProductContractTabLinks(chain.contractId),
        snapshot: chain,
      });
    });
  });

  test('[STANDALONE]: TC-BE-10 – Deposit row on the listing has empty Point of Delivery', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const { customerId, depositId } = await test.step(
      'Precondition: deposit then partial MLO offset so currentAmount < initialAmount',
      async () => {
        const created = await createLegalCustomer(fx);
        const id = await createDeposit(fx, 100);
        await reduceDepositViaMlo(fx, id);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('deposit', { id, initialAmount: 100 });
        TestRunSummary.registerPayload('customerLiability', { id: Responses.customerLiability[0] });
        return { customerId: created.customerId, depositId: id };
      },
    );

    await test.step('Assert: deposit listing row has null pods/address', async () => {
      const page = await postListingOk(fx, customerId, defaultListingBody({ showDeposits: true }));
      const found = findDepositRow(page.content, depositId);
      expect(found, `listing must contain deposit idRaw=${depositId}`).toBeTruthy();
      expect(found!.object).toBe('deposit');
      expectNullPodsAndAddress(found!, 'TC-BE-10');
      TestRunSummary.recordCheck({
        check: 'Deposit row POD columns are null',
        expectedResult: 'HTTP 206; object=deposit; pods=null; address=null',
        actualResult: `As expected — object=${found!.object}; pods=${String(found!.pods)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'deposit', 'customerLiability'],
        snapshot: { customerId, depositId },
      });
    });
  });

  test('[STANDALONE]: TC-BE-11 – Search by POD identifier finds the liability that displays that POD', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const chain = await test.step('Precondition: 1-POD liability whose listing pods equals identifier', async () => {
      const result = await runManualContractChain(fx, 1, test.info().workerIndex);
      const liabilityId = await createManualLiability(fx, {
        billingGroupId: result.billingGroupId,
        initialAmount: 10,
      });
      const unfiltered = await postListingOk(fx, result.customerId);
      const baseline = findRowByIdRaw(unfiltered.content, liabilityId);
      expect(baseline?.pods, 'baseline listing pods must equal POD identifier').toBe(result.pods[0].identifier);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
      TestRunSummary.registerPayload('customerLiability', {
        id: liabilityId,
        podIdentifier: result.pods[0].identifier,
      });
      return { ...result, liabilityId };
    });

    await test.step('Assert: searchFields=POD_IDENTIFIER returns the liability row', async () => {
      const page = await postListingOk(
        fx,
        chain.customerId,
        defaultListingBody({
          searchFields: 'POD_IDENTIFIER',
          prompt: chain.pods[0].identifier,
        }),
      );
      const found = findRowByIdRaw(page.content, chain.liabilityId);
      expect(found, `search must return liability idRaw=${chain.liabilityId}`).toBeTruthy();
      expect(found!.pods).toBe(chain.pods[0].identifier);
      TestRunSummary.recordCheck({
        check: 'Search by POD_IDENTIFIER finds the liability',
        expectedResult: `row present; pods=${chain.pods[0].identifier}`,
        actualResult: `As expected — found idRaw=${found!.idRaw}; pods=${String(found!.pods)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'customerLiability'],
        extraLinks: buildProductContractTabLinks(chain.contractId),
        snapshot: chain,
      });
    });
  });

  test('[STANDALONE]: TC-BE-12 – Search by POD address finds the liability that displays that address', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const chain = await test.step('Precondition: 1-POD liability; store addressFromListing', async () => {
      const result = await runManualContractChain(fx, 1, test.info().workerIndex);
      const liabilityId = await createManualLiability(fx, {
        billingGroupId: result.billingGroupId,
        initialAmount: 10,
      });
      const unfiltered = await postListingOk(fx, result.customerId);
      const baseline = findRowByIdRaw(unfiltered.content, liabilityId);
      expect(baseline, 'baseline listing row').toBeTruthy();
      expect(baseline!.address, 'addressFromListing must be a non-null string').toBeTruthy();
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      TestRunSummary.registerPayload('productContract', Responses.productContract[0]);
      TestRunSummary.registerPayload('customerLiability', {
        id: liabilityId,
        addressFromListing: baseline!.address,
      });
      return { ...result, liabilityId, addressFromListing: String(baseline!.address) };
    });

    await test.step('Assert: searchFields=POD_ADDRESS returns the same address', async () => {
      const page = await postListingOk(
        fx,
        chain.customerId,
        defaultListingBody({
          searchFields: 'POD_ADDRESS',
          prompt: chain.addressFromListing,
        }),
      );
      const found = findRowByIdRaw(page.content, chain.liabilityId);
      expect(found, `search must return liability idRaw=${chain.liabilityId}`).toBeTruthy();
      expect(found!.address).toBe(chain.addressFromListing);
      TestRunSummary.recordCheck({
        check: 'Search by POD_ADDRESS finds the liability',
        expectedResult: `row present; address=${chain.addressFromListing}`,
        actualResult: `As expected — address=${String(found!.address)}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'productContract', 'customerLiability'],
        extraLinks: buildProductContractTabLinks(chain.contractId),
        snapshot: chain,
      });
    });
  });

  test('[STANDALONE]: TC-BE-13 – Listing rejects a body without page', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const { customerId } = await test.step('Precondition: legal customer only (no liability)', async () => {
      const created = await createLegalCustomer(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      return created;
    });

    await test.step('Assert: missing page → HTTP 400 with page-Page must not be null;', async () => {
      const { status, json, rawText } = await postListing(fx, customerId, {
        size: 50,
        showDeposits: false,
        showLiabilitiesAndReceivables: true,
      });
      expect(status, 'must not use CheckResponse on expected 400').toBe(400);
      const message = extractErrorText(json) || rawText;
      expect(message).toContain('page-Page must not be null;');
      expect(Array.isArray(json.content), 'validation error must not return listing content').toBe(false);
      expect(Responses.customerLiability.length, 'no new liability created').toBe(0);
      TestRunSummary.recordCheck({
        check: 'Listing without page is rejected',
        expectedResult: 'HTTP 400; message contains page-Page must not be null;',
        actualResult: `As expected — status=${status}; message=${message}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer'],
        snapshot: { customerId },
      });
    });
  });

  test('[STANDALONE]: TC-BE-14 – Listing rejects a body without size', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const { customerId } = await test.step('Precondition: legal customer only', async () => {
      const created = await createLegalCustomer(fx);
      TestRunSummary.registerPayload('customer', Responses.customer[0]);
      return created;
    });

    await test.step('Assert: missing size → HTTP 400 with size-Size must not be null;', async () => {
      const { status, json, rawText } = await postListing(fx, customerId, {
        page: 0,
        showDeposits: false,
        showLiabilitiesAndReceivables: true,
      });
      expect(status).toBe(400);
      const message = extractErrorText(json) || rawText;
      expect(message).toContain('size-Size must not be null;');
      expect(Array.isArray(json.content), 'validation error must not return listing content').toBe(false);
      expect(Responses.customerLiability.length, 'no new liability created').toBe(0);
      TestRunSummary.recordCheck({
        check: 'Listing without size is rejected',
        expectedResult: 'HTTP 400; message contains size-Size must not be null;',
        actualResult: `As expected — status=${status}; message=${message}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer'],
        snapshot: { customerId },
      });
    });
  });

  test('[STANDALONE]: TC-BE-15 – showDeposits false excludes the deposit row', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const { customerId, depositId } = await test.step(
      'Precondition: deposit + liability + MLO so currentAmount < initialAmount',
      async () => {
        const created = await createLegalCustomer(fx);
        const id = await createDeposit(fx, 100);
        await reduceDepositViaMlo(fx, id);
        TestRunSummary.registerPayload('customer', Responses.customer[0]);
        TestRunSummary.registerPayload('deposit', { id, initialAmount: 100 });
        TestRunSummary.registerPayload('customerLiability', { id: Responses.customerLiability[0] });
        return { customerId: created.customerId, depositId: id };
      },
    );

    await test.step('Assert: deposit visible with showDeposits true, absent with false', async () => {
      const withDeposits = await postListingOk(fx, customerId, defaultListingBody({ showDeposits: true }));
      expect(findDepositRow(withDeposits.content, depositId), 'deposit present when showDeposits=true').toBeTruthy();

      const withoutDeposits = await postListingOk(
        fx,
        customerId,
        defaultListingBody({ showDeposits: false }),
      );
      expect(
        findDepositRow(withoutDeposits.content, depositId),
        'deposit absent when showDeposits=false',
      ).toBeUndefined();
      TestRunSummary.recordCheck({
        check: 'showDeposits flag includes/excludes the deposit row',
        expectedResult: 'Both listings HTTP 206; deposit present only when showDeposits=true',
        actualResult: 'As expected — present with true, absent with false',
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'deposit', 'customerLiability'],
        snapshot: { customerId, depositId },
      });
    });
  });

  test('[STANDALONE]: TC-BE-16 – Listing with a customer-details id instead of customer id returns no rows for the created liability', async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    Nomenclatures,
    TestRunSummary,
  }) => {
    const fx = fxFrom({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures });

    const { customerId, customerDetailsId, liabilityId } = await test.step(
      'Precondition: customer with distinct detail id + manual liability',
      async () => {
        const created = await createLegalCustomerWithDistinctDetailId(fx);
        const id = await createManualLiability(fx, { billingGroupId: null, initialAmount: 10 });
        TestRunSummary.registerPayload('customer', {
          ...Responses.customer[0],
          customerId: created.customerId,
          customerDetailsId: created.customerDetailsId,
        });
        TestRunSummary.registerPayload('customerLiability', { id });
        return { ...created, liabilityId: id };
      },
    );

    await test.step('Assert: wrong path hides the liability; control path with customerId shows it', async () => {
      const wrong = await postListingOk(fx, customerDetailsId);
      expect(
        findRowByIdRaw(wrong.content, liabilityId),
        'path customerDetailsId must not return the liability',
      ).toBeUndefined();

      const control = await postListingOk(fx, customerId);
      const found = findRowByIdRaw(control.content, liabilityId);
      expect(found, 'control path with customerId must return the liability').toBeTruthy();
      TestRunSummary.recordCheck({
        check: 'Listing path must use customer.customers.id, not customerDetailsId',
        expectedResult: 'Both HTTP 206; detail-id path has no row; customerId path has the liability',
        actualResult: `As expected — wrong totalElements=${wrong.totalElements ?? (wrong.content ?? []).length}; control found idRaw=${found!.idRaw}`,
        passed: true,
      });
    });

    await test.step('Attach test run summary', async () => {
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: JIRA_KEY,
        relevantEntityKeys: ['customer', 'customerLiability'],
        snapshot: { customerId, customerDetailsId, liabilityId },
      });
    });
  });
});
