import exp from 'constants';
import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';
import { request, setMaxIdleHTTPParsers } from 'http';
import { TIMEOUT } from 'dns';
import { setTimeout } from 'timers/promises';
import { APIRequestContext } from "playwright";

test.describe('[REG-56]: Receivables Management - Payments', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-115]: Payments', () => {
        test.describe('[REG-1032]: Payment - Reverse', () => {
            test('[REG-1033]: Payment reverse - without liability connection (without offsetting)', async({Request, GeneratePayload, Responses, Endpoints, receivableValidations}) => {
                await test.step('Create customer', async() => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    expect(customer).CheckResponse();
                    const responseBody = await customer.json();
                    Responses.customer.push(responseBody);
                });
                
                await test.step('create collection channel', async() => {
                    const collectionChannel = await Request.post(Endpoints.collectionChannel, {data: GeneratePayload.receivablesManagement.collection_channel()});
                    expect(collectionChannel).CheckResponse();
                    const responseBody = await collectionChannel.json();
                    Responses.collectionChannel.push(responseBody);
                });

                await test.step('create payment package', async() => {
                    const paymentPackage = await Request.post(Endpoints.paymentPackage, {data: GeneratePayload.receivablesManagement.payment_package()});
                    expect(paymentPackage).CheckResponse();
                    const responseBody = await paymentPackage.json();
                    Responses.paymentPackage.push(responseBody);
                });

                await test.step('create payment', async() => {
                    const payload = await GeneratePayload.receivablesManagement.payment();
                    payload.initialAmount = 100;
                    const payment = await Request.post(Endpoints.payment, {data: payload});
                    const responseBody = await payment.json();
                    expect(payment).CheckResponse();
                    Responses.payment.push(responseBody);

                });

                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });

                await test.step('check payment offsettings (no liability only receivable)', async() => {
                    const paymentGet = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                    expect(paymentGet).CheckResponse();
                    const responseBody = await paymentGet.json(); 
                    //check if receivable exists and is linked with payment
                    const existsReceivable = responseBody.offsettingResponseList.find((item: any) => item.offsettingObjectType == "RECEIVABLE");
                    const existsLiability = responseBody.offsettingResponseList.find((item: any) => item.offsettingObjectType == "LIABILITY");
                    expect(existsReceivable).toBeTruthy();
                    expect(existsLiability).toBeFalsy();
                    //check receivable and store it
                    expect(responseBody.currentAmount).toEqual(0);
                    const receivableGet = await Request.get(Endpoints.customerReceivable + `/${responseBody.offsettingResponseList[0].id}`);
                    const receivableResponse = await receivableGet.json();
                    expect(receivableResponse.currentAmount).toBe(responseBody.initialAmount);
                    Responses.customerReceivable.push(responseBody.offsettingResponseList[0].id); 
                });

                await test.step('create new customer for reverse', async() => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    expect(customer).CheckResponse();
                    const responseBody = await customer.json();
                    Responses.customer.push(responseBody);
                    expect(Responses.customer.length).toBe(2);
                })

                await test.step('Reverse payment', async() => {
                    const reverse = await Request.post(Endpoints.payment + '/cancel', {data: GeneratePayload.receivablesManagement.paymentReverse()});
                    expect(reverse).CheckResponse();
                    const newPayment = await reverse.json();
                    Responses.payment.push(newPayment);
                });

                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });
                
                await test.step('check receivable and old payment', async() => {
                    const receivable = await Request.get(Endpoints.customerReceivable + `/${Responses.customerReceivable[0]}`);
                    expect(receivable).CheckResponse();
                    const responseBody = await receivable.json();
                    expect(responseBody.currentAmount).toEqual(0);
                    const reversedPayment = responseBody.customerOffsettingResponseList.some((item: any) => item.description.includes('Reversed'));
                    expect(reversedPayment).toBeTruthy();
                });

                await test.step('check negative payment and receivable', async() => {
                    const oldCustomerIdentifier = Responses.customer[0].identifier;
                    const data = {
                        "page": 0,
                        "size": 25,
                        "prompt": `${oldCustomerIdentifier}`,
                        "searchFields": "CUSTOMER_IDENTIFIER",
                        "columns": "PAYMENT_NUMBER",
                        "direction": "DESC"
                    }
                    const listing = await Request.post(Endpoints.payment + `/list`, {data: data})
                    expect(listing).CheckResponse();
                    const listingResponse = await listing.json();
                    const negativePaymentId = listingResponse.content[0].id;
                    //validate
                    await receivableValidations.paymentValidation(negativePaymentId);
                    //negativePayment
                    const negativePaymentGet = await Request.get(Endpoints.payment +`/${negativePaymentId}`);
                    expect(negativePaymentGet).CheckResponse();
                    const responseBody = await negativePaymentGet.json();
                    expect(responseBody.currentAmount).toEqual(0);
                    expect(responseBody.offsettingResponseList.length).toBeGreaterThan(0);
                    // receivable
                    const newReceivableId = responseBody.offsettingResponseList[0].id;
                    const receivableGet = await Request.get(Endpoints.customerReceivable +`/${newReceivableId}`);
                    expect(receivableGet).CheckResponse();
                    const responseBodyReceivable = await receivableGet.json();
                    expect(responseBodyReceivable.currentAmount).toEqual(0);
                });

                await test.step('Check new payment', async() => {
                    const paymentGet = await Request.get(Endpoints.payment + `/${Responses.payment[1]}`);
                    const responseBody = await paymentGet.json();
                    expect(responseBody.currentAmount).toEqual(0);
                    //check receivable
                    const receivableExists = responseBody.offsettingResponseList.some((item: any) => item.offsettingObjectType == 'RECEIVABLE');
                    expect(receivableExists).toBeTruthy();
                    const receivableGet = await Request.get(Endpoints.customerReceivable + `/${responseBody.offsettingResponseList[0].id}`);
                    expect(receivableGet).CheckResponse();
                    const responseBodyReceivable = await receivableGet.json();
                    expect(responseBodyReceivable.currentAmount).toEqual(100);
                    const paymentExists = responseBodyReceivable.customerOffsettingResponseList.some((item:any) => item.offsettingObjectType == 'PAYMENT');
                    const paymentAmount = responseBodyReceivable.customerOffsettingResponseList.some((item:any) => item.amount == 100);
                    expect(paymentExists).toBeTruthy();
                    expect(paymentAmount).toBeTruthy();
                })
                test.info().attach('[REG-1033] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-1034]: Payment reverse - Reverse payment, which took part in offsetting process (direct + manual + overdue liability)', async({Request, GeneratePayload, Responses, Endpoints, receivableValidations}) => {
                let twoMinute: any;
                twoMinute = 1 * 60 * 1000
                test.setTimeout(twoMinute);

                await test.step('Create customer', async() => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    expect(customer).CheckResponse();
                    const responseBody = await customer.json();
                    Responses.customer.push(responseBody);
                });

                await test.step('create collection channel', async() => {
                    const collectionChannel = await Request.post(Endpoints.collectionChannel, {data: GeneratePayload.receivablesManagement.collection_channel()});
                    expect(collectionChannel).CheckResponse();
                    const responseBody = await collectionChannel.json();
                    Responses.collectionChannel.push(responseBody);
                });

                await test.step('create payment package', async() => {
                    const paymentPackage = await Request.post(Endpoints.paymentPackage, {data: GeneratePayload.receivablesManagement.payment_package()});
                    expect(paymentPackage).CheckResponse();
                    const responseBody = await paymentPackage.json();
                    Responses.paymentPackage.push(responseBody);
                });

                await test.step('create daily interest rate', async() => {
                    const post = await Request.post(Endpoints.interestRate, {data: GeneratePayload.receivablesManagement.dailyInterestRate()});
                    expect(post).CheckResponse();
                    Responses.interestRate.push(await post.json());
                })

                await test.step('create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.initialAmount = 50
                    payload.occurrenceDate = randomGens.generateYesterdaysDate("dd-mm-yyyy");
                    payload.dueDate = randomGens.generateYesterdaysDate("dd-mm-yyyy");
                    payload.applicableInterestRateId = Responses.interestRate[0];
                    const liability = await Request.post(Endpoints.customerLiability, {data: payload});
                    expect(liability).CheckResponse();
                    const responseBody = await liability.json();
                    Responses.customerLiability.push(responseBody);
                });

                await test.step('create payment', async() => {
                    const payload = await GeneratePayload.receivablesManagement.payment();
                    payload.initialAmount = 100;
                    const payment = await Request.post(Endpoints.payment, {data: payload});
                    expect(payment).CheckResponse();
                    const responseBody = await payment.json();
                    Responses.payment.push(responseBody);
                });

                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });

                await test.step('check if payment happened, get receivable and liability', async() => {
                    const paymentGet = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                    expect(paymentGet).CheckResponse();
                    const responseBody = await paymentGet.json();
                    const LiabilityIsCovered = responseBody.offsettingResponseList.some((item: any) => item.offsettingObjectType == 'LIABILITY');
                    expect(LiabilityIsCovered).toBeTruthy();
                    const receivableGenerated = responseBody.offsettingResponseList.find((item: any) => item.offsettingObjectType == 'RECEIVABLE');
                    expect(receivableGenerated).toBeTruthy();
                    const receicableId = receivableGenerated!.id;
                    expect(receicableId,'receivableId is not defined').toBeDefined();
                    Responses.customerReceivable.push(receicableId);
                    //check receivable amount and save in array
                    const receivableGet = await Request.get(Endpoints.customerReceivable + `/${receicableId}`);
                    expect(receivableGet).CheckResponse();
                    const responseBodyReceivable = await receivableGet.json();
                    expect(responseBodyReceivable.currentAmount).toEqual(50);
                    //check liability amount
                    const liabilityGet = await Request.get(Endpoints.customerLiability + `/${Responses.customerLiability[0]}`);
                    expect(liabilityGet).CheckResponse;
                    const responseBodyLiability = await liabilityGet.json();
                    expect(responseBodyLiability.currentAmount).toEqual(0);
                });

                await test.step('create new liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.initialAmount = 50
                    payload.occurrenceDate = randomGens.generateYesterdaysDate("dd-mm-yyyy");
                    payload.dueDate = randomGens.generateMonthEndDate("dd-mm-yyyy");
                    const liability = await Request.post(Endpoints.customerLiability, {data: payload});
                    expect(liability).CheckResponse();
                    const responseBody = await liability.json();
                    Responses.customerLiability.push(responseBody);
                });

                await test.step('use generated receivable in MLO', async() => {
                    //modify payload
                    const payload = await GeneratePayload.receivablesManagement.MLO_L_R();
                    payload.liabilities[0].id = Responses.customerLiability[1];
                    payload.liabilities[0].current_amount = 50;
                    payload.receivables[0].current_amount = 50;
                    payload.receivedOffsets[0].offset_amount = 50;
                    payload.receivedLiabilities[0].id = Responses.customerLiability[1];
                    payload.receivedOffsets[0].l_id = Responses.customerLiability[1];
                    //post MLO
                    const MLO = await Request.post(Endpoints.manualLiabilityOffsetting, {data: payload});
                    expect(MLO).CheckResponse();
                    const responseBodyMlo = await MLO.json();
                    Responses.manualLiabilityOffsetting.push(responseBodyMlo);
                    //check receivable amount 
                    const receivable = await Request.get(Endpoints.customerReceivable +`/${Responses.customerReceivable[0]}`);
                    expect(receivable).CheckResponse();
                    const responseBody = await receivable.json();
                    expect(responseBody.currentAmount).toEqual(0);
                });

                //generate lpf            
                await test.step('check if LPFs are generated', async() => {
                    const LPFs = await GeneratePayload.receivablesManagement.waitForLPFGeneration(false);
                    expect(LPFs.content).toBeDefined()
                    expect((LPFs.content??[]).length).toBeGreaterThan(0)
                    Responses.latePaymentFine.push(LPFs.content[0]);
                })

                await test.step('create new customer for payment reverse', async() => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    expect(customer).CheckResponse();
                    const responseBody = await customer.json();
                    Responses.customer.push(responseBody);
                });

                await test.step('Reverse payment', async() => {
                    const reverse = await Request.post(Endpoints.payment + '/cancel', {data: GeneratePayload.receivablesManagement.paymentReverse()});
                    expect(reverse).CheckResponse();
                    const newPayment = await reverse.json();
                    Responses.payment.push(newPayment)
                });

                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });

                await test.step('check if payment is reversed', async() => {
                    const reversedPayment = await Request.get(Endpoints.payment +`/${Responses.payment[0]}`);
                    expect(reversedPayment).CheckResponse();
                    const responseBody = await reversedPayment.json();
                    expect(responseBody.status).toEqual("REVERSED");
                });

                await test.step('check if lpf is reversed and reversal exists', async() => {
                    const lpfGet = await Request.get(Endpoints.latePaymentFine + `/${Responses.latePaymentFine[0].id}`);
                    expect(lpfGet).CheckResponse();
                    const responseBody = await lpfGet.json();
                    expect(responseBody.reversed).toBeTruthy();
                    //reversal exists
                    const reversal = await Request.get(Endpoints.latePaymentFine + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&searchBy=ALL&direction=DESC&sortingType=NUMBER`);
                    expect(reversal).CheckResponse();
                    const responseBodyReversal = await reversal.json();
                    const reversalExists = responseBodyReversal.content.some((item: any) => item.type == 'REVERSAL_OF_LATE_PAYMENT_FINE');
                    expect(reversalExists).toBeTruthy();
                });

                await test.step("check if MLO is reversed", async() => {
                    const mloGet = await Request.get(Endpoints.manualLiabilityOffsetting + `/${Responses.manualLiabilityOffsetting[0]}`);
                    expect(mloGet).CheckResponse();
                    const responseBody = await mloGet.json();
                    expect(responseBody.reversed).toBeTruthy();
                });

                await test.step('check if offsettings are rollbacked', async() => {
                    const liability1 = await Request.get(Endpoints.customerLiability + `/${Responses.customerLiability[0]}`);
                    expect(liability1).CheckResponse();
                    const responseBody1 = await liability1.json();
                    expect(responseBody1.currentAmount).toEqual(50);

                    const liability2 = await Request.get(Endpoints.customerLiability + `/${Responses.customerLiability[1]}`);
                    expect(liability2).CheckResponse();
                    const responseBody2 = await liability2.json();
                    expect(responseBody2.currentAmount).toEqual(50);
                });

                await test.step('check negative payment', async() => {
                    const oldCustomerIdentifier = Responses.customer[0].identifier;
                    const data = {
                        "page": 0,
                        "size": 25,
                        "prompt": `${oldCustomerIdentifier}`,
                        "searchFields": "CUSTOMER_IDENTIFIER",
                        "columns": "PAYMENT_NUMBER",
                        "direction": "DESC"
                    }
                    const listing = await Request.post(Endpoints.payment + `/list`, {data: data})
                    expect(listing).CheckResponse();
                    const listingResponse = await listing.json();
                    const negativePaymentId = listingResponse.content[0].id;
                    //validate
                    await receivableValidations.paymentValidation(negativePaymentId)
                    //negativePayment
                    const negativePaymentGet = await Request.get(Endpoints.payment +`/${negativePaymentId}`);
                    expect(negativePaymentGet).CheckResponse();
                    const responseBody = await negativePaymentGet.json();
                    expect(responseBody.currentAmount).toEqual(0);
                    //check offsetting
                    expect(responseBody.offsettingResponseList.length).toBeGreaterThan(0);
                    const receivableExists = responseBody.offsettingResponseList.find((item: any) => item.offsettingObjectType == 'RECEIVABLE');
                    expect(receivableExists).toBeTruthy();
                });

                await test.step('Check if LPF liability is covered', async() => {
                    const liabilityListing = await Request.get(Endpoints.customerLiability + `/list?page=0&size=25&columns=ID&direction=DESC&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                    expect(liabilityListing).CheckResponse();
                    const responseBody = await liabilityListing.json();
                    expect(responseBody.content[0].currentAmount).toEqual(0);
                });

                await test.step('check new payment and its receivable', async() => {
                    const newPayment = await Request.get(Endpoints.payment + `/${Responses.payment[1]}`);
                    expect(newPayment).CheckResponse();
                    const responseBody = await newPayment.json();
                    expect(responseBody.currentAmount).toEqual(0);
                    const receivableIsCreated = responseBody.offsettingResponseList.find((item: any) => item.offsettingObjectType == 'RECEIVABLE');
                    expect(receivableIsCreated).toBeTruthy();
                    const receivableId = receivableIsCreated!.id;
                    //check reveicable
                    const receivable = await Request.get(Endpoints.customerReceivable + `/${receivableId}`);
                    expect(receivable).CheckResponse();
                    const responseBodyReceivable = await receivable.json();
                    expect(responseBodyReceivable.currentAmount).toEqual(100);
                });

                test.info().attach('[REG-1034] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});