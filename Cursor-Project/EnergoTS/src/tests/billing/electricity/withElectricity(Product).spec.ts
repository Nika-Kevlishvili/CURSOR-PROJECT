import { test, expect } from '../../../backend/fixtures/baseFixture';
import { randomGens } from '../../../backend/utils/randomGens';
import reportGenerator from '../../../backend/utils/generateReport';

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

                test.info().attach('[REG-991] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
            test('[REG-1172]: with electricity POD level - deactivated pod before meter reading date should not be included ', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, validateInvoice}) => {
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

                await test.step('generate POD 1', async () => {
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                });

                await test.step('generate POD 2', async () => {
                    const podSettlement2 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement2).CheckResponse();
                    Responses.pod.push(await podSettlement2.json());
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

                await test.step('Activate PODs', async () => {
                    const pod1ActivationDate = randomGens.generateMonthStartDate('yyyy-mm-dd');
                    const pod2DeactivationDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
                    const pod2ActivationDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -2);

                    const podActivation = await Request.post('/contract-pods/manual', {
                        data: await GeneratePayload.pointsOfDelivery.pod_activation(0, pod1ActivationDate),
                    });
                    await expect(podActivation).CheckResponse();
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, pod2ActivationDate, pod2DeactivationDate),});
                    await expect(podActivation2).CheckResponse();
                });

                await test.step('Data by profiles POD 1', async() => {
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
                   await validateInvoice.checkInvoiceTabs()
                })
                await test.step('validate invoice detailed tab — single POD only', async () => {
                    const contract = await Request.get(`product-contract/${Responses.productContract[0].id}?version=1`);
                    await expect(contract).CheckResponse();
                    const pods = ((await contract.json()) as { contractPodsResponses?: Array<{ podId?: number; identifier?: string | number }> }).contractPodsResponses ?? [];
                    const activePod = String(pods.find((p) => p.podId === Responses.pod[0].id)?.identifier ?? Responses.pod[0].id);

                    const res = await Request.get(`invoice/detailed-data?id=${Responses.invoice[0]}&page=0&size=100`);
                    await expect(res).CheckResponse();
                    const rows = ((await res.json()) as { content?: Array<{ pointOfDelivery?: string | null }> }).content ?? [];

                    const uniquePods = [...new Set(rows.map((r) => String(r.pointOfDelivery ?? '').trim()).filter(Boolean))];
                    expect(uniquePods, '[REG-991] detailed tab must list only active POD 1').toEqual([activePod]);
                })

                test.info().attach('[REG-1172] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})
    


