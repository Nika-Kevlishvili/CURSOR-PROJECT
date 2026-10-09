import { test, expect } from '../../../fixtures/baseFixture';
import { randomGens } from '../../../utils/randomGens';   

test.describe('[REG-55]: Billing', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-563]: with electricity (Product)', () => {
            test('[REG-991]: with electricity POD level - happy pass', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, validateInvoice}) => {
                test.setTimeout(6 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();

                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate electricity price component', async () => {
                    const payload = GeneratePayload.productAndServices.electricity()
                    payload.name = 'WITH_ELECTRICITY_INVOICE'
                    const priceElectricity = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(priceElectricity).CheckResponse();
                    Responses.priceComponent.push(await priceElectricity.json());
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

                await test.step('Data by profiles', async() => {
                    const payload = await GeneratePayload.energyData.profile1Month();
                    payload.timeZone = 'CET';
            
                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();
                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                });

                await test.step('Billing run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES', 'WITH_ELECTRICITY_INVOICE'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                
                await test.step('invoice generation', async() => {
                    await GeneratePayload.billing.waitForInvoiceGeneration();
                })

                await test.step('validate invoice', async() => {
                    validateInvoice.checkInvoiceTabs()
                })
            })
        })
    })
})
