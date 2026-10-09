import {test, expect} from '../../../fixtures/baseFixture';


test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-560]: Over time One time', () => {
            test('[REG-977]: Over time one time  - happy pass', async ({Request, GeneratePayload, Responses, Endpoints, validateInvoice}) => {
                test.setTimeout(6 * 60 * 1000);

                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.oneTime()});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('generate POD', async () => {
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                });

                await test.step('generate product', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                });

                await test.step('generate contract', async () => {
                    const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate POD', async () => {
                    const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation).CheckResponse();
                });

                await test.step('Biiling run', async() => {
                    const contractget = await Request.get(`product-contract/${Responses.productContract[0].id}?version=1`);
                    const contractjson = await contractget.json();
                    const BillingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['OVER_TIME_ONE_TIME']);
 

                    BillingPayload.basicParameters.listOfCustomersContractsOrPOD = contractjson.basicParameters.contractNumber;

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
            })
        })
    })
})