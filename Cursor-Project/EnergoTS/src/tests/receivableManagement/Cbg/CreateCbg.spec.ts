import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe('[REG-56]: Receivables Management - Change of Coordinator Objection', { tag: '@receivableManagement' }, () => {
    test.describe('[REG-130]: Change of Coordinator Objection', () => {
        test.describe.serial('[REG-513]: Create', () => {
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

            test('[REG-1349]: Change of coordinator objection after a claimed penalty', async ({Request, Responses, GeneratePayload, Endpoints}) => {
                test.setTimeout(10 * 60 * 1000);

                await test.step('generate objects', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
        
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
        
                    const penalty = await Request.post(Endpoints.penalty, {data: GeneratePayload.productAndServices.penalty()});
                    await expect(penalty).CheckResponse();
                    Responses.penalty.push(await penalty.json());
        
                    const termination = await Request.post(Endpoints.termination, {data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_NOTICE')});
                    await expect(termination).CheckResponse();
                    Responses.termination.push(await termination.json());
        
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
        
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
        
                    const contractpayload = await GeneratePayload.contractsAndOrders.product_contract();
                    contractpayload.basicParameters.status = 'ENTERED_INTO_FORCE'
                    contractpayload.basicParameters.subStatus = 'AWAITING_ACTIVATION'
                    contractpayload.basicParameters.entryInForceDate = randomGens.generateUtcDate('yyyy-mm-dd');
                    const contract = await Request.post(Endpoints.productContract, {data: contractpayload});
                    
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())

                });

                await test.step('Activate POD', async () => {
                    const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation).CheckResponse();
                });
        
                // await test.step('Create action', async () => {
                //     const action = await Request.post(Endpoints.action, {data: await GeneratePayload.contractsAndOrders.action()});
                //     await expect(action).CheckResponse();
                //     Responses.action.push(await action.json());
                // });

                await test.step('create claimed penalty', async() => {
                    const payload = await GeneratePayload.contractsAndOrders.claimedPenalty();
                    // payload.dueDate = '2026-08-06';
                    // payload.billingGroupId = null
                    const claimedPenalty = await Request.post(Endpoints.claimedPenalty, {data: payload});
                    await expect(claimedPenalty).CheckResponse();
                    Responses.claimedPenalty.push(await claimedPenalty.json());
                });

                await test.step('make liability overdue', async() => {
                    await GeneratePayload.receivablesManagement.makeLiabilityOverdue();
                });

                await test.step('generate change of coordinator objection', async() => {
                    const payload = await GeneratePayload.receivablesManagement.cbgCreate();
                    const changeOfCoordinatorObjection = await Request.post(Endpoints.cbg, {data: payload});
                    await expect(changeOfCoordinatorObjection).CheckResponse();
                    Responses.cbg.push(await changeOfCoordinatorObjection.json());
                });

                console.log(reportGenerator.setLinksToResponses(Responses));
            });
        });
    });
});




