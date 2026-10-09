import { test, expect } from '../../../backend/fixtures/baseFixture';
import { customer_liability } from '../../../backend/jsons/payloads/Receivables/customerLiability';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-127]: Mass Operation for Blocking', () => {
        test.describe('[REG-905]: Create - Mass Operation for Blocking', () => {
            test('[REG-1014]: Block for offsetting - list of customers', async({Request, GeneratePayload, Responses, Endpoints}) => {
                let billingGroupId: any;
                
                await test.step('Create customer', async() => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    expect(customer).CheckResponse();
                    const responseBody = await customer.json();
                    Responses.customer.push(responseBody);
                });

                await test.step('Create blocking for liability offsetting', async() => {
                    const payload = GeneratePayload.receivablesManagement.mass_operation_for_blocking();
                    payload.blockingForLiabilitiesOffsetting!.fromDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');
                    payload.isBlockForLiabilitiesOffsetting = true;
                    payload.blockingForSupplyTermination = null;
                    payload.blockingForCalculation = null;
                    payload.blockingForReminderLetters = null;
                    payload.blockingForPayment = null;
                    const blocknig = await Request.post(Endpoints.massOperationForBlocking, {data: payload});
                    expect(blocknig).CheckResponse();
                    const responseBody = await blocknig.json();
                    Responses.massOperationForBlocking.push(responseBody);
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

                await test.step('Get billing group', async() => {
                    const contractGet = await Request.get(Endpoints.productContract+`/${Responses.productContract[0].id}`);
                    expect(contractGet).CheckResponse();
                    const responseBody = await contractGet.json();
                    billingGroupId = responseBody.billingGroups[0].id;
                });

                await test.step('Create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.billingGroupId = billingGroupId;
                    payload.occurrenceDate = randomGens.generateYesterdaysDate('dd-mm-yyyy');
                    payload.initialAmount = 100;

                    const liabilityCreate = await Request.post(Endpoints.customerLiability, {data: payload});
                    expect(liabilityCreate).CheckResponse();
                    const responseBody = await liabilityCreate.json();
                    Responses.customerLiability.push(responseBody);
                });

                await test.step('Create receivable to check automatic offsetting', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_receivable();
                    payload.billingGroupId = billingGroupId;
                    payload.occurrenceDate = randomGens.generateTodaysDate('dd-mm-yyyy');
                    payload.initialAmount = 100;

                    const receivablePost = await Request.post(Endpoints.customerReceivable, {data: payload});
                    expect(receivablePost).CheckResponse();
                    const id = await receivablePost.json();
                    Responses.customerReceivable.push(id);
                    //check offsetting on both object (should not happen);
                    const receivableGet = await Request.get(Endpoints.customerReceivable + `/${id}`);
                    const responseBody = await receivableGet.json();
                    expect(responseBody.currentAmount).toEqual(100);
                    expect(responseBody.customerOffsettingResponseList??[]).toHaveLength(0)
                    //check liability as well
                    const liabilityGet = await Request.get(Endpoints.customerLiability + `/${Responses.customerLiability[0]}`);
                    expect(liabilityGet).CheckResponse();
                    const responseBodyLiability = await liabilityGet.json();
                    expect(responseBodyLiability.currentAmount).toEqual(100);
                    expect(responseBodyLiability.customerOffsettingResponseList??[]).toHaveLength(0);
                });

                await test.step('create collection and payment package', async() => {
                    const collectionChannel = await Request.post(Endpoints.collectionChannel, {data: GeneratePayload.receivablesManagement.collection_channel()});
                    await expect(collectionChannel).CheckResponse();
                    const connectionChannelData = await collectionChannel.json();
                    Responses.collectionChannel.push(connectionChannelData);

                    const paymentPackage = await Request.post(Endpoints.paymentPackage, {data: GeneratePayload.receivablesManagement.payment_package()});
                    await expect(paymentPackage).CheckResponse();
                    const paymentPackageData = await paymentPackage.json();
                    Responses.paymentPackage.push(paymentPackageData);
                });

                await test.step('Create negative payment', async() => {
                    const payload = await GeneratePayload.receivablesManagement.payment();
                    payload.initialAmount = -100;
                    const post = await Request.post("/payment", {data: payload});
                    await expect(post).CheckResponse();
                    Responses.payment.push(await post.json());
                })

                await test.step('Check manual offsetting', async() => {
                    const payload = {
                        "customerDetailId": Responses.customer[0].lastCustomerDetailId,
                        "customerId": Responses.customer[0].id,
                        "date": randomGens.generateUtcDate('yyyy-mm-dd')
                    };
                    const infoRequest = await Request.post(Endpoints.manualLiabilityOffsetting + `/liability-offsetting-info`, {data: payload});
                    expect(infoRequest).CheckResponse();
                    const responseBody = await infoRequest.json();
                    expect(responseBody.customerLiabilities??[]).toHaveLength(0);
                    expect(responseBody.negativePayments??[]).toHaveLength(0);
                    expect(responseBody.receivables??[]).toHaveLength(0);
                });

                await test.step('create payment and check direct offsetting', async() => {
                    const payload = await GeneratePayload.receivablesManagement.payment();
                    payload.initialAmount = 100
                    const payment = await Request.post(Endpoints.payment, {data: payload});
                    await expect(payment).CheckResponse();
                    const id = await payment.json();
                    const paymentGet = await Request.get(Endpoints.payment+`/${id}`);
                    expect(paymentGet).CheckResponse();
                    const paymentResponse = await paymentGet.json();
                    const receivableIsGenerated = paymentResponse.offsettingResponseList.some((item: any) => item.offsettingObjectType === 'RECEIVABLE');
                    expect(receivableIsGenerated).toBe(true);
                    Responses.payment.push(id);
                });

                await test.step('check liability finally', async() => {
                    const get = await Request.get(Endpoints.customerLiability + `/${Responses.customerLiability[0]}`);
                    expect(get).CheckResponse();
                    const responseBodyLiability = await get.json();
                    expect(responseBodyLiability.currentAmount).toEqual(100);
                    expect(responseBodyLiability.customerOffsettingResponseList??[]).toHaveLength(0);
                });
                
                test.info().attach('[REG-1014] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});