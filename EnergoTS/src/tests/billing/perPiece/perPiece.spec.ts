import {test, expect} from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-561]: Per piece', () => {
            test('[REG-973]: Per Piece - happy pass', async ({Request, GeneratePayload, Responses, Endpoints, validateInvoice}) => {
                test.setTimeout(6 * 60 * 1000);

                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.perPiece()});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('generate POD', async () => {
                    const podSlp = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSlp).CheckResponse();
                    Responses.pod.push(await podSlp.json());
                });

                await test.step('generate service', async () => {
                    const service = await Request.post(Endpoints.service, {data: GeneratePayload.productAndServices.service()});
                    await expect(service).CheckResponse();
                    Responses.service.push(await service.json())
                });

                await test.step('generate contract', async () => {
                    const contract = await Request.post(Endpoints.serviceContract, {data: await GeneratePayload.contractsAndOrders.serviceContract()});
                    await expect(contract).CheckResponse();
                    Responses.serviceContract.push(await contract.json())
                });

                

                await test.step('Biiling run', async() => {
                    const BillingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['PER_PIECE']);
                    const billingRun = await Request.post(Endpoints.billingRun, {data: BillingPayload});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                
                await test.step('invoice generation', async() => {
                    await GeneratePayload.billing.waitForInvoiceGeneration();
                })

                await test.step('validate invoice', async() => {
                    await validateInvoice.checkInvoiceTabs()
                })

                test.info().attach('[REG-973] response', {
                     body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                     contentType: 'application/json'
                 });
            })
        })
    })
})