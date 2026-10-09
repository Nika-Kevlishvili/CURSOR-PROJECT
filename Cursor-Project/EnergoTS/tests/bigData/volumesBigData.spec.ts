import {test, expect} from '../../fixtures/baseFixture';

test('for volumes big data', {tag: '@bigData'}, () => {
        test.skip('For volumes - simple settlement many pods', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                test.setTimeout(10 * 60 * 1000);
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

                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('generate 20 PODs', async () => {
                    for (let i = 0; i < 20; i++) {
                        const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                        await expect(podSettlement).CheckResponse();
                        Responses.pod.push(await podSettlement.json());
                    }
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

                await test.step('Activate all PODs', async () => {
                    for (let i = 0; i < Responses.pod.length; i++) {
                        const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(i)});
                        await expect(podActivation).CheckResponse();
                    }
                });

                await test.step('Data by profiles for all PODs', async() => {
                    for (let i = 0; i < Responses.pod.length; i++) {
                        const payload = await GeneratePayload.energyData.profile1Month(i);
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
                    }
                });

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                 
                await test.step('wait for invoice generation', async () => {
                    await GeneratePayload.billing.waitForInvoiceGeneration()
                })
    })
})
        