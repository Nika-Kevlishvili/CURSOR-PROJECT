import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe('[REG-52]: Contracts and Orders - Product Contract', { tag: '@contractsAndOrders' }, () => {
  test.describe('[REG-83]: Energy Product Contract', () => {
    test.describe('[REG-1214]: Re-signing', () => {
      test('[REG-1228]: Re-signing Product validation during contract creation', { tag: '@customer' }, async ({
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
          // termPayload.resigningDeadlineValue = 7;
          // termPayload.resigningDeadlineType = 'MONTH';
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

        await test.step('Create product', async () => {
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

        await test.step('Create contract', async () => {
          const contractpayload = await GeneratePayload.contractsAndOrders.product_contract(0, 0, 0);
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


        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {
            data: GeneratePayload.productAndServices.priceSettlement(),
          });
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term', async () => {
          const termPayload = GeneratePayload.productAndServices.term();
          termPayload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
          termPayload.waitForOldContractTermToExpires = ['YES'];
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


        await test.step('Create product for Re-signing', async () => {
            const payload = GeneratePayload.productAndServices.product(1);
            payload.termId = Responses.terms[1].id;
            payload.priceComponentIds = [Responses.priceComponent[1]];
            payload.penaltyIds = [Responses.penalty[1].id];
            payload.terminationIds = [Responses.termination[1].id];

            (payload as any).isResigning = true;
            (payload as any).resignProductTargets = [
                Responses.product[0].id,
            ];




          const product = await Request.post(Endpoints.product, { data: payload });
          await expect(product).CheckResponse();
          Responses.product.push(await product.json());
        });



        await test.step('Create contract', async () => {
            const contractpayload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);

            contractpayload.basicParameters.status = 'SIGNED';
            contractpayload.basicParameters.subStatus = 'SIGNED_BY_BOTH_SIDES';
            contractpayload.basicParameters.versionStatus = 'SIGNED';
            contractpayload.basicParameters.signingDate = randomGens.generateUtcDate('yyyy-mm-dd');

            contractpayload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
            contractpayload.productParameters.productContractWaitForOldContractTermToExpires = 'YES' as any;

            const contract = await Request.post(Endpoints.productContract, { data: contractpayload });
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        test.info().attach('[REG-1224] response', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json',
        });
        console.log(reportGenerator.setLinksToResponses(Responses));
      });
    });
  });
});