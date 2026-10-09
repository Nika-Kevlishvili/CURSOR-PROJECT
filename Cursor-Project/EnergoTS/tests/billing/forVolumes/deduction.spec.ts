import {test, expect} from '../../../fixtures/baseFixture';
import {randomGens} from '../../../utils/randomGens';
import reportGenerator from '../../../utils/generateReport';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-562]: interim and advanced payments', () => {
            test('[REG-718]: Interim deduction (from same period)', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints}) => {
                test.setTimeout(20 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = 1
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
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

                await test.step('generate interim', async () => {
                    const payload =  GeneratePayload.productAndServices.interim();
                    payload.value = 300;
                    const interim = await Request.post(Endpoints.interim, {data: payload});
                    await expect(interim).CheckResponse();
                    Responses.interim.push(await interim.json());
                })

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
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                 
                await test.step('invoice generation', async() => {
                    await GeneratePayload.billing.waitForInvoiceGeneration();
                })

                await test.step('Billing run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                await test.step('invoice generation', async() => {
                    await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 1);
                })

                test.info().attach('[REG-718] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
                
            })
        })
    })
})