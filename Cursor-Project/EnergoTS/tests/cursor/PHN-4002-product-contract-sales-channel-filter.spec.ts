/**
 * PHN-4002 — Sales channel filter on product-contract listing uses contract Employee,
 * not the customer's account manager.
 *
 * Bug-only automation (no TC .md). Jira: PHN-4002.
 *
 * Reference spec(s):
 * - tests/contractsAndOrders/productContract.spec.ts
 * - tests/cursor/pdt-3059-product-contract-new-version-individual-product.fixtures.ts
 * - tests/cursor/cursor-test.fixtures.ts
 */

import { test, expect } from './cursor-test.fixtures';
import {
  buildProductContractTabLinks,
  finalizeTestRunSummary,
} from './shared/manual-verification-links.fixtures';
import {
  PHN_4002_JIRA_KEY,
  createCustomerWithAccountManager,
  createPod,
  createProductContractWithEmployee,
  createSharedCatalog,
  discoverSalesChannelEmployees,
  listProductContractById,
  uniquePhn4002Key,
  type Phn4002Fx,
} from './PHN-4002-product-contract-sales-channel-filter.fixtures';

const JIRA_TITLE =
  'CLONE - Product contracts listing - Filter Sales channel should filter by Employee not by Account manager of the customer';

test.describe('[PHN-4002]: Product contract listing sales channel filter', {
  tag: ['@phn-4002', '@productContract', '@contractsAndOrders'],
}, () => {
  test(`[PHN-4002]: ${JIRA_TITLE}`, async ({
    Request,
    GeneratePayload,
    Responses,
    Endpoints,
    TestRunSummary,
  }) => {
    test.setTimeout(15 * 60 * 1000);
    const fx: Phn4002Fx = { Request, GeneratePayload, Responses, Endpoints };

    const discovery = await test.step('Precondition: discover sales-channel tagged vs untagged employees', async () => {
      const result = await discoverSalesChannelEmployees(Request);
      TestRunSummary.registerPayload('salesChannelDiscovery', {
        source: result.source,
        taggedChannelId: result.taggedChannel.id,
        taggedChannelPortalTagId: result.taggedChannel.portalTagId,
        taggedEmployeeId: result.taggedEmployee.id,
        untaggedEmployeeId: result.untaggedEmployee.id,
        channelsWithPortalTag: result.channelsWithPortalTag.map((channel) => ({
          id: channel.id,
          portalTagId: channel.portalTagId,
          name: channel.name,
        })),
      });
      expect(
        result.taggedEmployee.id,
        'Need an ACTIVE account manager whose Login Portal tag maps to a sales channel',
      ).toBeGreaterThan(0);
      expect(
        result.untaggedEmployee.id,
        'Need an ACTIVE account manager that does not carry the selected sales-channel tag',
      ).toBeGreaterThan(0);
      expect(result.taggedEmployee.id, 'Tagged and untagged employees must be different people').not.toBe(
        result.untaggedEmployee.id,
      );
      expect(result.taggedChannel.id, 'Need a sales channel with portalTagResponse').toBeGreaterThan(0);
      return result;
    });

    await test.step('Precondition: Create shared catalog (term, price, product)', async () => {
      const { productId, productPayload } = await createSharedCatalog(fx);
      TestRunSummary.registerPayload('product', productPayload);
      expect(productId).toBeGreaterThan(0);
    });

    const wrongUnique = uniquePhn4002Key('W');
    const rightUnique = uniquePhn4002Key('R');

    await test.step('Precondition: Customer whose AM is the tagged employee (old wrong filter would match)', async () => {
      const { payload } = await createCustomerWithAccountManager(
        fx,
        discovery.taggedEmployee.id,
        discovery.accountManagerTypeId,
      );
      TestRunSummary.registerPayload('customer', payload);
    });

    await test.step('Precondition: Customer whose AM is the untagged employee', async () => {
      const { payload } = await createCustomerWithAccountManager(
        fx,
        discovery.untaggedEmployee.id,
        discovery.accountManagerTypeId,
      );
      TestRunSummary.registerPayload('customerUntaggedAm', payload);
    });

    await test.step('Precondition: Create two PODs', async () => {
      await createPod(fx);
      await createPod(fx);
      expect(Responses.pod.length).toBe(2);
    });

    const wrongContract = await test.step(
      'Precondition: Product contract — customer AM tagged, contract Employee untagged',
      async () => {
        const created = await createProductContractWithEmployee(fx, {
          customerIndex: 0,
          podIndex: 0,
          employeeId: discovery.untaggedEmployee.id,
          customerAccountManagerId: discovery.taggedEmployee.id,
          uniqueKey: wrongUnique,
        });
        TestRunSummary.registerPayload('productContract', {
          id: created.id,
          contractNumber: created.contractNumber,
          employeeId: created.employeeId,
          customerAccountManagerId: created.customerAccountManagerId,
          role: 'customer-am-tagged-employee-untagged',
        });
        expect(created.id).toBeGreaterThan(0);
        expect(created.contractNumber.length).toBeGreaterThan(0);
        expect(
          created.employeeId,
          `Wrong-contract persisted Employee must be untagged ${discovery.untaggedEmployee.id} after employee-update`,
        ).toBe(discovery.untaggedEmployee.id);
        return created;
      },
    );

    const rightContract = await test.step(
      'Precondition: Product contract — customer AM untagged, contract Employee tagged',
      async () => {
        const created = await createProductContractWithEmployee(fx, {
          customerIndex: 1,
          podIndex: 1,
          employeeId: discovery.taggedEmployee.id,
          customerAccountManagerId: discovery.untaggedEmployee.id,
          uniqueKey: rightUnique,
        });
        TestRunSummary.registerPayload('productContractTaggedEmployee', {
          id: created.id,
          contractNumber: created.contractNumber,
          employeeId: created.employeeId,
          customerAccountManagerId: created.customerAccountManagerId,
          role: 'customer-am-untagged-employee-tagged',
        });
        expect(created.id).toBeGreaterThan(0);
        expect(created.contractNumber.length).toBeGreaterThan(0);
        expect(
          created.employeeId,
          `Right-contract persisted Employee must be tagged ${discovery.taggedEmployee.id} after employee-update`,
        ).toBe(discovery.taggedEmployee.id);
        return created;
      },
    );

    const salesChannelIds = [discovery.taggedChannel.id];

    await test.step('Sanity: unfiltered list finds both contracts by CONTRACT_NUMBER', async () => {
      const wrongUnfiltered = await listProductContractById(Request, wrongContract);
      const rightUnfiltered = await listProductContractById(Request, rightContract);
      TestRunSummary.recordCheck({
        check: 'Unfiltered product-contract/list finds the two freshly created contracts',
        expectedResult:
          'POST /product-contract/list with page, size, searchBy=CONTRACT_NUMBER, unique prompt returns HTTP 2xx/206 and includes both contracts.',
        actualResult: `wrong status=${wrongUnfiltered.status} found=${wrongUnfiltered.found} number=${wrongContract.contractNumber}; right status=${rightUnfiltered.status} found=${rightUnfiltered.found} number=${rightContract.contractNumber}`,
        passed: wrongUnfiltered.found && rightUnfiltered.found,
      });
      expect(wrongUnfiltered.found, `Unfiltered list missing untagged-employee contract ${wrongContract.contractNumber}`).toBe(
        true,
      );
      expect(rightUnfiltered.found, `Unfiltered list missing tagged-employee contract ${rightContract.contractNumber}`).toBe(
        true,
      );
    });

    await test.step(
      'Assert: salesChannelIds uses contract Employee, not customer account manager',
      async () => {
        const wrongFiltered = await listProductContractById(Request, wrongContract, salesChannelIds);
        const rightFiltered = await listProductContractById(Request, rightContract, salesChannelIds);

        const wrongPassed = !wrongFiltered.found;
        const rightPassed = rightFiltered.found;

        TestRunSummary.recordCheck({
          check: 'Customer AM tagged + contract Employee untagged is excluded',
          expectedResult: `POST /product-contract/list with salesChannelIds=[${salesChannelIds.join(',')}] must NOT return contract ${wrongContract.contractNumber} (employee ${discovery.untaggedEmployee.id} has no matching sales-channel tag; customer AM ${discovery.taggedEmployee.id} must be ignored).`,
          actualResult: wrongPassed
            ? `As expected — HTTP ${wrongFiltered.status}; contract ${wrongContract.contractNumber} not in listing.`
            : `Not as expected — HTTP ${wrongFiltered.status}; contract still listed (old customer-AM filter).`,
          passed: wrongPassed,
        });

        TestRunSummary.recordCheck({
          check: 'Customer AM untagged + contract Employee tagged is included',
          expectedResult: `POST /product-contract/list with salesChannelIds=[${salesChannelIds.join(',')}] MUST return contract ${rightContract.contractNumber} (employee ${discovery.taggedEmployee.id} maps to channel ${discovery.taggedChannel.id}).`,
          actualResult: rightPassed
            ? `As expected — HTTP ${rightFiltered.status}; listing row id=${rightFiltered.match?.id} contractNumber=${rightFiltered.match?.contractNumber}.`
            : `Not as expected — HTTP ${rightFiltered.status}; tagged-employee contract missing from listing.`,
          passed: rightPassed,
        });

        expect(
          wrongFiltered.found,
          `Untagged-employee contract ${wrongContract.contractNumber} must be hidden when filtering by salesChannelIds=${JSON.stringify(salesChannelIds)}`,
        ).toBe(false);
        expect(
          rightFiltered.found,
          `Tagged-employee contract ${rightContract.contractNumber} must appear when filtering by salesChannelIds=${JSON.stringify(salesChannelIds)}`,
        ).toBe(true);
      },
    );

    await test.step('Attach test run summary', async () => {
      const extraLinks: Record<string, string[]> = {};
      for (const contractId of [wrongContract.id, rightContract.id]) {
        const links = buildProductContractTabLinks(contractId);
        for (const [key, urls] of Object.entries(links)) {
          extraLinks[key] = [...(extraLinks[key] ?? []), ...urls];
        }
      }
      finalizeTestRunSummary(TestRunSummary, Responses, {
        jiraKey: PHN_4002_JIRA_KEY,
        relevantEntityKeys: ['customer', 'product', 'productContract'],
        extraLinks: Object.keys(extraLinks).length ? extraLinks : undefined,
        snapshot: {
          discoverySource: discovery.source,
          taggedChannelId: discovery.taggedChannel.id,
          taggedEmployeeId: discovery.taggedEmployee.id,
          untaggedEmployeeId: discovery.untaggedEmployee.id,
          wrongContractId: wrongContract.id,
          wrongContractNumber: wrongContract.contractNumber,
          wrongContractEmployeeId: wrongContract.employeeId,
          rightContractId: rightContract.id,
          rightContractNumber: rightContract.contractNumber,
          rightContractEmployeeId: rightContract.employeeId,
        },
      });
    });
  });

  test(`[PHN-4002]: ${JIRA_TITLE} | contract Employee not assigned is hidden`, async () => {
    const skipReason =
      'Interactive POST /product-contract stamps the logged-in Account Manager (processEmployeeOnCreate); ' +
      'PUT /product-contract/employee-update/{id} requires employeeId. employeeId=null cannot be persisted, ' +
      'so this Confluence hide-when-unassigned case is skipped rather than asserted falsely.';
    test.info().annotations.push({ type: 'skip-reason', description: skipReason });
    test.skip(true, skipReason);
  });
});
