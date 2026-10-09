import {test, expect} from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

test.describe('[REG-52]: Contracts and Orders - Product Contract', {tag: '@contractsAndOrders'}, () => {  
    test.describe('[REG-86]: Service order', async () => {
        test.describe('[REG-930]: Service order Create', async () => {
            test(' [REG-932]: Create with POD', {tag: '@customer'}, async ({Request, GeneratePayload, Responses, Endpoints}) =>{
                await test.step('Create Customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('Create Term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    Responses.terms.push(await term.json())

                    await expect(term).CheckResponse();
                });
                    
                await test.step('Create Price Component', async () => {
                    const payload = GeneratePayload.productAndServices.perPiece()
                    payload.name = 'SERVICE_ORDER'
                    const price = await Request.post('/price-components', {data: payload});
                    const priceId = await price.json();
                    Responses.priceComponent.push(priceId);
                    expect((price).ok()).toBeTruthy();
                });
                
                await test.step('Create POD', async () => {
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    Responses.pod.push(await podSettlement.json());
                    await expect(podSettlement).CheckResponse();
                });

                await test.step('Create service for pods', async () => {
                    const servicePayload = GeneratePayload.productAndServices.service();

                    servicePayload.basicSettings.saleMethods = ['ORDER'];
                    servicePayload.contractTerms = [];
                    const service = await Request.post(Endpoints.service, {data: servicePayload})
                    Responses.service.push(await service.json());
                    await expect(service).CheckResponse();
                
                });
                
                await test.step('Create Service order', async () => {
                    const serviceOrder = await Request.post(Endpoints.serviceOrder, {data: await GeneratePayload.contractsAndOrders.serviceOrder()});
                    await expect(serviceOrder).CheckResponse();
                    Responses.serviceOrder.push(await serviceOrder.json());
                });

                await test.step('Create proforma invoice', async () => {
                    const proforma = await Request.post(`service-order/${await Responses.serviceOrder[0]}/issue-proforma-invoice`);    
                    await expect(proforma).CheckResponse();        
                });


                await test.step('Start generating', async () => {
                    const startGenerating = await Request.patch(`service-order/${await Responses.serviceOrder[0]}/start-generating`);   
                    await expect(startGenerating).CheckResponse();        
                });

                await test.step('Start accounting', async () => {
                    const startAccounting = await Request.patch(`service-order/${await Responses.serviceOrder[0]}/start-accounting`);    
                    await expect(startAccounting).CheckResponse();
                })

                await test.step('Start issuing', async () => {
                    const startIssuing = await Request.patch(`service-order/${await Responses.serviceOrder[0]}/issue-invoice`);    
                    await expect(startIssuing).CheckResponse();
                })

                test.info().attach('[REG-932] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});