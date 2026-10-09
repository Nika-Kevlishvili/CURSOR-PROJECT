import { test, expect } from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';

test.describe('[REG-56]: Receivables Management - Change of Coordinator Objection', { tag: '@receivableManagement' }, () => {
    test.describe('[REG-130]: Change of Coordinator Objection', () => {
        test.describe.serial('[REG-513]: Create', () => {
            test.describe('[REG-895]: Change of Coordinator Objection | Full Data | All Fields', () => {
                test('[REG-895]: Change of Coordinator Objection | Full Data | All Fields', async ({Request, GeneratePayload, Responses, Endpoints}) => {
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

                    await test.step('Biling run', async() => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                        await expect(billingRun).CheckResponse();
                        Responses.billingRun.push(await billingRun.json());
                    })
                    
                    await test.step('invoice generation', async() => {
                        await GeneratePayload.billing.waitForInvoiceGeneration();
                    })

                    await test.step('make liability overdue', async() => {
                        await GeneratePayload.receivablesManagement.makeLiabilityOverdue();
                    });

                    await test.step('generate change of coordinator objection', async() => {
                        const payload = await GeneratePayload.receivablesManagement.cbgCreate();
                        const changeOfCoordinatorObjection = await Request.post(Endpoints.cbg, {data: payload});
                        await expect(changeOfCoordinatorObjection).CheckResponse();
                        Responses.cbg.push(await changeOfCoordinatorObjection.json());
                    });
                });
            });
        });
    });
});




