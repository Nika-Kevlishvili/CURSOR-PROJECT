import {test, expect} from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';
import { randomGens } from '../../backend/utils/randomGens';

test.describe('[REG-52]: Contracts and Orders - Product Contract', {tag: '@contractsAndOrders'}, () => {
  test.describe('[REG-83]: product Contract', () => {
    test.describe('[REG-471]: Create contract', () => {
      test('[REG-472]: Create contract', {tag: '@customer'}, async ({Request, GeneratePayload, Responses, Endpoints}) => {
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json())
        });

        await test.step('Create penalty', async () => {
          const penalty = await Request.post(Endpoints.penalty, {data: GeneratePayload.productAndServices.penalty()});
          await expect(penalty).CheckResponse();
          Responses.penalty.push(await penalty.json());
        });

        await test.step('Create termination', async () => {
          const termination = await Request.post(Endpoints.termination, {data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_CONTRACT_TERM')});
          await expect(termination).CheckResponse();
          Responses.termination.push(await termination.json());
        });

        await test.step('Create POD', async () => {
          const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
          const podSettlement = await Request.post(Endpoints.pod, {data: payload});
          await expect(podSettlement).CheckResponse();
          Responses.pod.push(await podSettlement.json());
        });

        await test.step('Create product', async () => {
          const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
          await expect(product).CheckResponse();
          Responses.product.push(await product.json())
        });

        await test.step('Create contract', async () => {
          const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
          await expect(contract).CheckResponse();
          Responses.productContract.push(await contract.json())
        });

        await test.step('Activate POD', async () => {
          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
          await expect(podActivation).CheckResponse();
        });

        test.info().attach('[REG-472] response', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json'
        });
      })

      test('[REG-472]: Create contract and supply deactivation mass import', {tag: ['@customer', '@massImport']}, async ({Request, GeneratePayload, Responses, Endpoints, MassImportGenerator}) => {
        test.setTimeout(8 * 60 * 1000);

        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json())
        });

        await test.step('Create penalty', async () => {
          const penalty = await Request.post(Endpoints.penalty, {data: GeneratePayload.productAndServices.penalty()});
          await expect(penalty).CheckResponse();
          Responses.penalty.push(await penalty.json());
        });

        await test.step('Create termination', async () => {
          const termination = await Request.post(Endpoints.termination, {data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_CONTRACT_TERM')});
          await expect(termination).CheckResponse();
          Responses.termination.push(await termination.json());
        });

        await test.step('Create POD', async () => {
          const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
          const podSettlement = await Request.post(Endpoints.pod, {data: payload});
          await expect(podSettlement).CheckResponse();
          Responses.pod.push(await podSettlement.json());
        });

        await test.step('Create product', async () => {
          const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
          await expect(product).CheckResponse();
          Responses.product.push(await product.json())
        });

        await test.step('Create contract', async () => {
          const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
          await expect(contract).CheckResponse();
          Responses.productContract.push(await contract.json())
        });

        await test.step('Activate POD 1', async () => {
          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
          await expect(podActivation).CheckResponse();
        });

        await test.step('Deactivate supply via mass import', async () => {
          const payload = await MassImportGenerator.SupplyActionDeactivationMassImportGenerator.supplyDeactivationMP();
          const res = await MassImportGenerator.generateAndUpload(payload);
          expect(res.result.success, res.result.message ?? undefined).toBe(true);

          const contractId = Responses.productContract[0].id;
          const contractGet = await Request.get(`${Endpoints.productContract}/${contractId}?version=1`);
          await expect(contractGet).CheckResponse();
          const contractBody = await contractGet.json();
          expect(contractBody.basicParameters.subStatus).toBe('IN_TERMINATION_BY_GO_DATA');
        });

        await test.step('get created action', async () => {
          const get = await Request.get(`actions/list?page=0&size=25&searchBy=CUSTOMER_IDENTIFIER&prompt=${Responses.customer[0].identifier}&sortDirection=DESC&sortBy=ID&exactMatch=false`);
          await expect(get).CheckResponse();
          const body = await get.json();
          Responses.action.push(body.content[0]);
        })

        console.log(reportGenerator.setLinksToResponses(Responses))

        test.info().attach('[REG-472] response', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json'
        });
      })

    });

    test.describe('[REG-489]: Edit Energy Product Contract', () => {
      test.skip('[REG-1361]: Edit energy product contract', {tag: '@customer'}, async ({Request, GeneratePayload, Responses, Endpoints, }) => {
        let productContract: any = null
        
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json())
        });

        await test.step('Create POD', async () => {
          const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
          await expect(podSettlement).CheckResponse();
          Responses.pod.push(await podSettlement.json());
        });

        await test.step('Create product', async () => {
          const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
          await expect(product).CheckResponse();
          Responses.product.push(await product.json())
        });

        await test.step('Create contract', async () => {
          productContract = await GeneratePayload.contractsAndOrders.product_contract()
          const contract = await Request.post(Endpoints.productContract, {data: productContract});
          await expect(contract).CheckResponse();
          Responses.productContract.push(await contract.json())
        });

        await test.step('Activate POD', async () => {
          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
          await expect(podActivation).CheckResponse();
        });

        await test.step('Edit product contract', async () => {
          const payload = await GeneratePayload.contractsAndOrders.edit_ProductContract(productContract);
          payload.basicParameters.entryInForceDate = randomGens.generateTodaysDate('yyyy-mm-dd');
          const contractId = Responses.productContract[0].id;
          const versionId = Responses.productContract[0].versionId || 1;
          const editResponse = await Request.put(`${Endpoints.productContract}/${contractId}?versionId=${versionId}&changeFutureVersionsPods=false`, {data: payload});
          await expect(editResponse).CheckResponse();
        });

        test.info().attach('[REG-472] response', {
          body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
          contentType: 'application/json'
        });
      })  
    });
  });
});