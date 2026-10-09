import exp from 'constants';
import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-127]: Mass Operation for Blocking', () => {
        test.describe('[REG-905]: Create - Mass Operation for Blocking', () => {
            test('[REG-1016]: Block for payment for list of customers', async({Request, GeneratePayload, Responses, Endpoints}) => {
                await test.step('Create customer', async() => {
                    const customer_legal = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
                    await expect(customer_legal).CheckResponse();
                    const customerData = await customer_legal.json();
                    Responses.customer.push(customerData);
                });

                await test.step('create collection', async() => {
                    const collectionChannel = await Request.post(Endpoints.collectionChannel, {data: GeneratePayload.receivablesManagement.collection_channel()});
                    await expect(collectionChannel).CheckResponse();
                    const connectionChannelData = await collectionChannel.json();
                    Responses.collectionChannel.push(connectionChannelData);
                });

                await test.step('create payment package', async() => {
                    const paymentPackage = await Request.post(Endpoints.paymentPackage, {data: GeneratePayload.receivablesManagement.payment_package()});
                    await expect(paymentPackage).CheckResponse();
                    const paymentPackageData = await paymentPackage.json();
                    Responses.paymentPackage.push(paymentPackageData);
                });

                await test.step('create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.initialAmount = 100;
                    const liability = await Request.post(Endpoints.customerLiability, {data: payload});
                    await expect(liability).CheckResponse();
                    const liabilityResponse = await liability.json();
                    Responses.customerLiability.push(liabilityResponse);
                });

                await test.step('create mass operation for blocking', async() => {
                    const payload = GeneratePayload.receivablesManagement.mass_operation_for_blocking();
                    payload.blockingForLiabilitiesOffsetting = null;
                    payload.blockingForReminderLetters = null;
                    payload.blockingForSupplyTermination = null;
                    payload.isBlockForPayment = true;
                    payload.blockingForCalculation = null;
                    payload.blockingForPayment!.fromDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');
                    const massPost =  await Request.post(Endpoints.massOperationForBlocking, {data: payload});
                    await expect(massPost).CheckResponse();
                    Responses.massOperationForBlocking.push(await massPost.json());
                })

                await test.step('create payment', async() => {
                    const payload = await GeneratePayload.receivablesManagement.payment();
                    payload.initialAmount = 100;
                    const payment = await Request.post(Endpoints.payment, {data: payload});
                    await expect(payment).CheckResponse();
                    const paymentResponse = await payment.json();
                    Responses.payment.push(paymentResponse);
                });

                await test.step('Verify that payment is blocked', async() => {
                    //check liability first should not be covered by payment 
                    const liabilityDetails = await Request.get(Endpoints.customerLiability + `/${Responses.customerLiability[0]}`);
                    await expect(liabilityDetails).CheckResponse();
                    const liabilityData = await liabilityDetails.json();
                    expect(liabilityData.currentAmount).toBe(100);
                    //check payment should be linked with receivable only
                    const paymentDetails = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                    await expect(paymentDetails).CheckResponse();
                    const paymentData = await paymentDetails.json();
                    const receivableIsGenerated = paymentData.offsettingResponseList.some((item: any) => item.offsettingObjectType === 'RECEIVABLE');
                    const LIABILITYleIsLinked = paymentData.offsettingResponseList.some((item: any) => item.offsettingObjectType === 'LIABILITY');
                    expect(receivableIsGenerated).toBe(true);
                    expect(LIABILITYleIsLinked).toBe(false);                  
                });

                test.info().attach('[REG-101] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
                //WE SHOULD IMPLEMENT ONLINE PAYMENT INTEGRATION LATER
            });
        });
    });
});