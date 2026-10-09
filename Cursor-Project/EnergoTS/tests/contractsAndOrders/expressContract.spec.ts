import {test, expect} from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-52]: Contracts and Orders - Express Contract', {tag: '@contractsAndOrders'}, () => {
  test.describe('[REG-82]: Create express', () => {
    test.describe('[REG-505]: Product contract from express', () => {
      test('[REG-926]: express', {tag: '@customer'}, async ({Request, GeneratePayload, Responses, Endpoints}) => {
        await test.step('Create POD', async () => {
          const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
          Responses.pod.push(await podSettlement.json());
        });
        
        await test.step('Create Price Component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
          Responses.priceComponent.push(await price.json())
        });

        await test.step('Create Term', async () => {
          const termpayload = structuredClone(GeneratePayload.productAndServices.term())
          
          termpayload.supplyActivations = ['FIRST_DAY_OF_MONTH']
          termpayload.contractEntryIntoForces = ['SIGNING']
          termpayload.startsOfContractInitialTerms = ['SIGNING']
          termpayload.waitForOldContractTermToExpires = ['NO']

          const term = await Request.post(Endpoints.terms, {data: termpayload});
          await expect(term).CheckResponse();
          const termResponse = await term.json();
          Responses.terms.push(termResponse);
          
        });

        await test.step('Create Product', async () => {
          const productpayload = structuredClone(GeneratePayload.productAndServices.product())

          productpayload.contractTypes = ["COMBINED"];
          productpayload.paymentGuarantees = ['NO'];
          productpayload.typePointsOfDelivery = ['CONSUMER'];
          productpayload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
          productpayload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
          productpayload.voltageLevels = ['LOW'];
          
          const product = await Request.post(Endpoints.product, {data: productpayload})
          await expect(product).CheckResponse();
          const productResponse = await product.json();
          Responses.product.push(productResponse);
          
        })

        await test.step('Create Express Contract', async () => {
          const expresspayload = await GeneratePayload.contractsAndOrders.expressContract()
          
        
          const express = await Request.post(Endpoints.expressContract, {data: expresspayload})
          await expect(express).CheckResponse();
          
          const expressResponse = await express.json();
          Responses.expressContract.push(expressResponse);
          
          test.info().attach('express contract response', {
            body: JSON.stringify(expressResponse, null, 2),
            contentType: 'application/json'
          });
        })

        test.info().attach('[REG-926] response', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json'
        });
      })
    })
  })
});