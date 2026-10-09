import exp from 'constants';
import { test, expect} from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';
import { envVariables } from '../../../backend/fixtures/envCashed';
import { RequestWrapper } from '../../../backend/utils/RequestWrapper';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-127]: Mass Operation for Blocking', () => {
        test.describe('[REG-905]: Create - Mass Operation for Blocking', () => {
            test('[REG-1049]: Block for supply termination - list of customer', async({Request, GeneratePayload, Responses, Endpoints}) => {
                test.setTimeout(10 * 60 * 1000); // 10 minutes

                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
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
                    const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate POD', async () => {
                    const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation).CheckResponse();
                });

                await test.step('Create billing run for manual invoice', async () => {
                    const payload = await GeneratePayload.billing.manualInvoice();
                    const createManualInvoice = await Request.post(Endpoints.billingRun, {data: payload});

                    await expect(createManualInvoice).CheckResponse();
                    
                    const responseBody = await createManualInvoice.json();
                    Responses.billingRun.push(responseBody);
                })

                await test.step('make the invoice real', async () => {
                    await GeneratePayload.billing.waitForInvoiceGeneration()
                })

                await test.step('create mass blocking object', async() => {
                    const payload = GeneratePayload.receivablesManagement.mass_operation_for_blocking();
                    payload.isBlockForSupplyTermination = true;
                    payload.blockingForSupplyTermination!.fromDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');
                    payload.blockingForCalculation = null;
                    payload.blockingForPayment = null;
                    payload.blockingForLiabilitiesOffsetting = null;
                    payload.blockingForReminderLetters = null;

                    const blockingPost = await Request.post(Endpoints.massOperationForBlocking, {data: payload});
                    expect(blockingPost).CheckResponse();
                    Responses.massOperationForBlocking.push(await blockingPost.json());
                });

                await test.step('Generate reminder for disconnection', async () => {
                    const reminderForDisconnectionPayload = GeneratePayload.receivablesManagement.reminderForDisconnection();
                    const reminderForDisconnectionResponse = await Request.post(Endpoints.reminderForDisconnection, {data: reminderForDisconnectionPayload});
                    await expect(reminderForDisconnectionResponse).CheckResponse();

                    Responses.reminderForDisconnection.push(await reminderForDisconnectionResponse.json());
                    await GeneratePayload.receivablesManagement.offsetReminderForDisconnectionTime();
                })

                await test.step('check if customer is included in reminder for disconnection', async() => {
                    const secondTab = await Request.get(`${Endpoints.reminderForDisconnection}/second-tab?page=0&size=25&powerSupplyDisconnectionReminderId=${Responses.reminderForDisconnection[0]}`);
                    expect(secondTab).CheckResponse();
                    const responseBody = await secondTab.json();
                    expect(responseBody.content??[]).toHaveLength(1);
                });

                await test.step('create request for disconnection', async() => {
                    const post = await Request.post(`${Endpoints.reminderForDisconnection}`, {data: GeneratePayload.receivablesManagement.requestForDisconnection()});
                    const body = await post.json();
                    expect(post.status()).toBeGreaterThanOrEqual(500);
                    expect(post.status()).toBeLessThan(600);
                    expect(JSON.stringify(body)).toContain('APPLICATION_ERROR');
                    expect(JSON.stringify(body)).toContain('ValidationException');
                })

                test.info().attach('[REG-1049] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });

            });
        });
    });
});