import {test, expect} from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-52]: Contracts and Orders - Service Contract', {tag: '@contractsAndOrders'}, () => {
  test.describe('[REG-84]: Service Contract', () => {
    test.describe('[REG-493]: Create Service contract', () => {
      test('[REG-920]: Create Service contract', {tag: '@customer'}, async ({Request, GeneratePayload, Responses, Endpoints}) => {
          await test.step('Create Customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            Responses.customer.push(await customer.json());

            await expect(customer).CheckResponse();
          });

         await test.step('Create Price Component', async () => {
            const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
            await expect(price).CheckResponse();
            const priceResponse = await price.json();
            Responses.priceComponent.push(priceResponse);

          });
          
        await test.step('Create Term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            Responses.terms.push(await term.json())

            await expect(term).CheckResponse();
          });

        await test.step('Create Service', async () =>{
          const servicePayload = GeneratePayload.productAndServices.service();
          const service = await Request.post(Endpoints.service, {data: servicePayload});
          await expect(service).CheckResponse();
          const serviceResponse = await service.json();
          Responses.service.push(serviceResponse);

        })

        await test.step('Create POD', async () => {
          const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
          await expect(podSettlement).CheckResponse();
          const podResponse = await podSettlement.json();
          Responses.pod.push(podResponse);
        })

        await test.step('Create POD 2', async () => {
          const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
          await expect(podSettlement).CheckResponse();
          const podResponse = await podSettlement.json();
          Responses.pod.push(podResponse);
        })

        await test.step('Service contract', async () => {
          const serviceContract = await Request.post(Endpoints.serviceContract, {data: await GeneratePayload.contractsAndOrders.serviceContract()});
          await expect(serviceContract).CheckResponse();
        })

        test.info().attach('[REG-920] response', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json'
        });
      })
    })
  })
});