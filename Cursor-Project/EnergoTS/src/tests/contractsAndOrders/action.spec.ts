import {test, expect} from '../../backend/fixtures/baseFixture';
import { randomGens } from '../../backend/utils/randomGens';
import reportGenerator from '../../backend/utils/generateReport';

test.describe('[REG-52]: Contracts and Orders - Action', {tag: '@contractsAndOrders'}, () => {
  test.describe('[REG-85]: Action', () => {
    test.describe('[REG-914]: Create action', () => {
      test('[REG-921]: Happy path', {tag: '@customer'}, async ({Request, Responses, GeneratePayload, Endpoints}) => {
        await test.step('generate objects', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json())

          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());

          const penalty = await Request.post(Endpoints.penalty, {data: GeneratePayload.productAndServices.penalty()});
          await expect(penalty).CheckResponse();
          Responses.penalty.push(await penalty.json());

          const termination = await Request.post(Endpoints.termination, {data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_NOTICE')});
          await expect(termination).CheckResponse();
          Responses.termination.push(await termination.json());

          const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
          await expect(podSettlement).CheckResponse();
          Responses.pod.push(await podSettlement.json());

          const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
          await expect(product).CheckResponse();
          Responses.product.push(await product.json())

          const contractpayload = await GeneratePayload.contractsAndOrders.product_contract();
          contractpayload.basicParameters.status = 'ENTERED_INTO_FORCE'
          contractpayload.basicParameters.subStatus = 'AWAITING_ACTIVATION'
          contractpayload.basicParameters.entryInForceDate = randomGens.generateUtcDate('yyyy-mm-dd');
          const contract = await Request.post(Endpoints.productContract, {data: contractpayload});
          
          await expect(contract).CheckResponse();
          Responses.productContract.push(await contract.json())
        });

        await test.step('Create action', async () => {
          const action = await Request.post(Endpoints.action, {data: await GeneratePayload.contractsAndOrders.action()});
          await expect(action).CheckResponse();
        });

        test.info().attach('[REG-921] response', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json'
        });
      });
    });
  });
});