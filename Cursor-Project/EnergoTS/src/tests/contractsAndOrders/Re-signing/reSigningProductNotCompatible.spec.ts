import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe('[REG-52]: Contracts and Orders - Product Contract', { tag: '@contractsAndOrders' }, () => {
  test.describe('[REG-83]: Energy Product Contract', () => {
    test.describe('[REG-1214]: Re-signing', () => {
      test('[REG-1226]: Create product contract at READY — product not compatible', { tag: '@customer' }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
      }) => {
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_legal(),
          });
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {
            data: GeneratePayload.productAndServices.priceSettlement(),
          });
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term', async () => {
          const termPayload = GeneratePayload.productAndServices.term();
          termPayload.resigningDeadlineValue = 7;
          termPayload.resigningDeadlineType = 'MONTH';
          const term = await Request.post(Endpoints.terms, { data: termPayload });
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json());
        });

        await test.step('Create penalty', async () => {
          const penalty = await Request.post(Endpoints.penalty, {
            data: GeneratePayload.productAndServices.penalty(),
          });
          await expect(penalty).CheckResponse();
          Responses.penalty.push(await penalty.json());
        });

        await test.step('Create termination', async () => {
          const termination = await Request.post(Endpoints.termination, {
            data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_CONTRACT_TERM'),
          });
          await expect(termination).CheckResponse();
          Responses.termination.push(await termination.json());
        });

        await test.step('Create POD', async () => {
          const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
          const podSettlement = await Request.post(Endpoints.pod, { data: payload });
          await expect(podSettlement).CheckResponse();
          Responses.pod.push(await podSettlement.json());
        });

        // product[0] = P-STD
        await test.step('Create product P-STD', async () => {
          const productPayload = GeneratePayload.productAndServices.product();
          productPayload.productTerms = [
            {
              typeOfTerms: 'CERTAIN_DATE',
              value: '',
              periodType: null,
              renewalPeriodValue: null,
              renewalPeriodType: null,
              perpetuityCause: false,
              automaticRenewal: null,
              numberOfRenewals: null,
              name: 'Contract end date term',
              id: null,
            },
          ];
          const product = await Request.post(Endpoints.product, { data: productPayload });
          await expect(product).CheckResponse();
          Responses.product.push(await product.json());
        });

        await test.step('Create old contract (Active in term)', async () => {
          const contractpayload = await GeneratePayload.contractsAndOrders.product_contract();
          contractpayload.basicParameters.status = 'ENTERED_INTO_FORCE';
          contractpayload.basicParameters.subStatus = 'AWAITING_ACTIVATION';
          contractpayload.basicParameters.versionStatus = 'SIGNED';
          contractpayload.basicParameters.signingDate = '2025-01-01';
          contractpayload.basicParameters.entryInForceDate = '2025-01-01';

          const today = randomGens.generateTodaysDate('yyyy-mm-dd');
          const endDate = new Date(today);
          endDate.setMonth(endDate.getMonth() + 3);
          const contractTermEndDate = endDate.toISOString().split('T')[0];

          contractpayload.basicParameters.contractTermEndDate = contractTermEndDate;
          contractpayload.productParameters.contractTermEndDate = contractTermEndDate;

          const contract = await Request.post(Endpoints.productContract, { data: contractpayload });
          await expect(contract).CheckResponse();
          Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
          const podActivation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(0, '2025-01-01'),
          });
          await expect(podActivation).CheckResponse();
        });

        // --- ნაკრები #2 — OTHER პროდუქტი ---
        await test.step('Create price component (for other product)', async () => {
          const price = await Request.post(Endpoints.priceComponent, {
            data: GeneratePayload.productAndServices.priceSettlement(),
          });
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term (for other product)', async () => {
          const termPayload = GeneratePayload.productAndServices.term();
          termPayload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
          termPayload.waitForOldContractTermToExpires = ['YES'];
          const term = await Request.post(Endpoints.terms, { data: termPayload });
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json());
        });

        await test.step('Create penalty (for other product)', async () => {
          const penalty = await Request.post(Endpoints.penalty, {
            data: GeneratePayload.productAndServices.penalty(),
          });
          await expect(penalty).CheckResponse();
          Responses.penalty.push(await penalty.json());
        });

        await test.step('Create termination (for other product)', async () => {
          const termination = await Request.post(Endpoints.termination, {
            data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_CONTRACT_TERM'),
          });
          await expect(termination).CheckResponse();
          Responses.termination.push(await termination.json());
        });

        // product[1] = OTHER
        await test.step('Create other product (not P-STD)', async () => {
          const productPayload = GeneratePayload.productAndServices.product(1);
          productPayload.termId = Responses.terms[1].id;
          productPayload.priceComponentIds = [Responses.priceComponent[1]];
          productPayload.penaltyIds = [Responses.penalty[1].id];
          productPayload.terminationIds = [Responses.termination[1].id];
          const product2 = await Request.post(Endpoints.product, { data: productPayload });
          await expect(product2).CheckResponse();
          Responses.product.push(await product2.json());
        });

        // --- ნაკრები #3 — P-RS ---
        await test.step('Create price component (for resigning product)', async () => {
          const price = await Request.post(Endpoints.priceComponent, {
            data: GeneratePayload.productAndServices.priceSettlement(),
          });
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term (for resigning product)', async () => {
          const termPayload = GeneratePayload.productAndServices.term();
          termPayload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
          termPayload.waitForOldContractTermToExpires = ['YES'];
          const term = await Request.post(Endpoints.terms, { data: termPayload });
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json());
        });

        await test.step('Create penalty (for resigning product)', async () => {
          const penalty = await Request.post(Endpoints.penalty, {
            data: GeneratePayload.productAndServices.penalty(),
          });
          await expect(penalty).CheckResponse();
          Responses.penalty.push(await penalty.json());
        });

        await test.step('Create termination (for resigning product)', async () => {
          const termination = await Request.post(Endpoints.termination, {
            data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_CONTRACT_TERM'),
          });
          await expect(termination).CheckResponse();
          Responses.termination.push(await termination.json());
        });

        // product[2] = P-RS, target = OTHER (არა P-STD)
        await test.step('Create Resigning product P-RS (targets OTHER, not P-STD)', async () => {
          const payload = GeneratePayload.productAndServices.product(2);
          payload.termId = Responses.terms[2].id;
          payload.priceComponentIds = [Responses.priceComponent[2]];
          payload.penaltyIds = [Responses.penalty[2].id];
          payload.terminationIds = [Responses.termination[2].id];
          (payload as any).isResigning = true;
          (payload as any).resignProductTargets = [Responses.product[1]];
          const resignProduct = await Request.post(Endpoints.product, { data: payload });
          await expect(resignProduct).CheckResponse();
          Responses.product.push(await resignProduct.json());
        });

        await test.step('Link P-RS special offers to OTHER product (no P-STD → P-RS mapping)', async () => {
          const res = await Request.put(`products/${Responses.product[2]}/special-offers?version=1`, {
            data: {
              promotionalProduct: false,
              applyToSpecificCustomers: false,
              preferenceIds: [],
              customers: [],
              contracts: [],
              resignable: true,
              reSigningProducts: [{ productId: Responses.product[1], versionIds: [1] }],
            },
          });
          await expect(res).CheckResponse();
        });

        await test.step('Create product contract at READY — product not compatible', async () => {
          const contractpayload = await GeneratePayload.contractsAndOrders.product_contract(0, 2, 0);
          contractpayload.basicParameters.status = 'READY';
          contractpayload.basicParameters.subStatus = 'READY';
          // პირველი ვერსია უნდა იყოს SIGNED, თორემ ადრე იჭრება versionStatus ვალიდაციით
          contractpayload.basicParameters.versionStatus = 'SIGNED';
          contractpayload.basicParameters.signingDate = '';
          contractpayload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
          contractpayload.productParameters.productContractWaitForOldContractTermToExpires = 'YES' as any;

          const contract = await Request.post(Endpoints.productContract, { data: contractpayload });

          expect(contract.status(), 'Save must be rejected').toBeGreaterThanOrEqual(400);

          const body = await contract.json();
          console.log('Create error:', body);

          expect(String(body.message ?? '')).toContain(
            "Contract can't be created/edited. Re-signing unavailable due to product incompatibility;"
          );

          expect(Responses.productContract.length, 'Only old contract should exist').toBe(1);
        });

        test.info().attach('[REG-1226] response', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json',
        });
        console.log(reportGenerator.setLinksToResponses(Responses));
      });
    });
  });
});