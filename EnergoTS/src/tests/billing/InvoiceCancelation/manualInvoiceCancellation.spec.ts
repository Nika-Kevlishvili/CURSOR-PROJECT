import { generatePrime } from 'crypto';
import {test, expect} from '../../../backend/fixtures/baseFixture';
import { ReceivablesManagementPayloads } from '../../../backend/jsons/payloadGenerators/domains/ReceivablesManagementPayloads';
import reportGenerator from '../../../backend/utils/generateReport';
import { json } from 'stream/consumers';
import { randomGens } from '../../../backend/utils/randomGens';
import { isGeneratorFunction } from 'util/types';

test.describe('[REG-55]: Billing', () => {
    test.describe('[REG-109]: Invoice Cancellation', () => {
        test.describe('[REG-1059]: Invoice Cancellation Test Cases', () => {
            test('[REG-1068]: Invoice cancellation - Manual invoice(credit note type) -> All reverse process on receivable side (all possible object links)', async({Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl}) => {
                test.setTimeout(10 * 60 * 1000); // 10 minutes
                let billingGroupId: any;
                let totalAmount: any;
                let liabilityAmount: any;

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
                })

                await test.step('Activate POD', async () => {
                    const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation).CheckResponse();
                });

                await test.step('create daily interest rate', async() => {
                    const post = await Request.post(Endpoints.interestRate, {data: GeneratePayload.receivablesManagement.dailyInterestRate()});
                    expect(post).CheckResponse();
                    Responses.interestRate.push(await post.json());
                })

                await test.step('Create billing run for manual invoice', async () => {
                    const payload = await GeneratePayload.billing.manualInvoice();
                    payload.manualInvoiceParameters.manualInvoiceBasicDataParameters.applicableInterestRateId = Responses.interestRate[0];
                    payload.manualInvoiceParameters.manualInvoiceSummaryDataParameters.summaryDataRowList[0].value = 400;
                    const createManualInvoice = await Request.post(Endpoints.billingRun, {data: payload});

                    await expect(createManualInvoice).CheckResponse();
                    
                    const responseBody = await createManualInvoice.json();
                    Responses.billingRun.push(responseBody);
                })

                await test.step('make the invoice real', async () => {
                    await GeneratePayload.billing.waitForInvoiceGeneration()
                })

                await test.step('find liability', async() => {
                    const invoiceId = Responses.invoice[0];
                    const findLiability = await Request.get(Endpoints.customerLiability + `/list?page=0&size=25&columns=ID&direction=DESC&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                    expect(findLiability).CheckResponse();
                    const listOfLiability = await findLiability.json();
                    expect(listOfLiability.content??[]).toHaveLength(1);
            
                    const liabilityId = listOfLiability.content[0].id;
                    const liabilityGet = await Request.get(Endpoints.customerLiability + `/${liabilityId}`);
                    expect(liabilityGet).CheckResponse();
                    const response = await liabilityGet.json();

                    expect(response.invoiceResponse.id).toEqual(invoiceId);
                    expect(response.billingGroupResponse).toBeDefined();

                    billingGroupId = response.billingGroupResponse.id;
                    totalAmount = response.initialAmount;
                    Responses.customerLiability.push(liabilityId);
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

                await test.step('Pay part with payment (direct offsetting)', async() => {
                    const payload = await GeneratePayload.receivablesManagement.payment();
                    payload.initialAmount = 100;
                    const payment = await Request.post(Endpoints.payment, {data: payload});
                    expect(payment).CheckResponse();
                    Responses.payment.push(await payment.json());
                })

                await test.step('check if payment happened, get receivable and liability', async() => {
                    const paymentGet = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                    expect(paymentGet).CheckResponse();
                    const responseBody = await paymentGet.json();
                    expect(responseBody.currentAmount).toBe(0)
                    const LiabilityIsLinked= responseBody.offsettingResponseList.some((item: any) => item.offsettingObjectType == 'LIABILITY');
                    expect(LiabilityIsLinked).toBeTruthy();
                });

                await test.step('Create receivable for MLO', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_receivable();
                    payload.initialAmount = 100;
                    const receivable = await Request.post(Endpoints.customerReceivable, {data: payload });
                    expect(receivable).CheckResponse();
                    Responses.customerReceivable.push(await receivable.json());
                })

                await test.step('offset liability manually with receivable', async() => {
                    //check liability details
                    const get = await Request.get(`${Endpoints.customerLiability}/${Responses.customerLiability[0]}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();
                    const liabilityAmount = responseBody.currentAmount;
                    
                    //create MLO object to offset rec and liab manually
                    const payload = await GeneratePayload.receivablesManagement.MLO_L_R();
                    payload.liabilities[0].current_amount = liabilityAmount;
                    payload.receivedLiabilities[0].current_amount = liabilityAmount - 100;
                    const manualOffsetingPost = await Request.post(Endpoints.manualLiabilityOffsetting, {data: payload});
                    expect(manualOffsetingPost).CheckResponse();
                    Responses.manualLiabilityOffsetting.push(await manualOffsetingPost.json())
                })

                await test.step('Pay with receivable -> automatic offsetting', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_receivable();
                    payload.billingGroupId = billingGroupId;
                    payload.initialAmount = 100
                    const post = await Request.post(Endpoints.customerReceivable, {data: payload});
                    expect(post).CheckResponse();
                    Responses.customerReceivable.push(await post.json())

                    //check that offsetting happened
                    const getResponse = await Request.get(`${Endpoints.customerReceivable}/${Responses.customerReceivable[0]}`);
                    expect(getResponse).CheckResponse()
                    const responseBody = await getResponse.json();
                    expect(responseBody.currentAmount).toEqual(0)
                }) 

                await test.step('Create deposit for future offsetting', async() => {
                    //check liability details
                    const get = await Request.get(`${Endpoints.customerLiability}/${Responses.customerLiability[0]}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();
                    liabilityAmount = responseBody.currentAmount;
                    
                    //Create deposit with liability amount
                    const payload = GeneratePayload.receivablesManagement.deposit();
                    payload.initialAmount = liabilityAmount;
                    const postRequest = await Request.post(Endpoints.deposit, {data: payload });
                    expect(postRequest).CheckResponse();
                    Responses.deposit.push(await postRequest.json())
                })

                await test.step('make deposit ready to use in offsetting', async() => {
                    await GeneratePayload.receivablesManagement.makeDepositReady();
                })

                await test.step('use deposit to cover liability fully with MLO', async() => {
                    //config payload
                    const payload = await GeneratePayload.receivablesManagement.MLO_D_L();
                    payload.date = randomGens.generateUtcDate('yyyy-mm-dd');
                    payload.deposits[0].current_amount = liabilityAmount;
                    payload.liabilities[0].current_amount = liabilityAmount;
                    payload.receivedOffsets[0].offset_amount = liabilityAmount;
                    //post MLO
                    const MLOPost = await Request.post(Endpoints.manualLiabilityOffsetting, {data: payload});
                    expect(MLOPost).CheckResponse();
                    Responses.manualLiabilityOffsetting.push(await MLOPost.json());
                })

                await test.step('chane liability due date for LPF', async() => {
                    const change = await Request.put(`${Endpoints.customerLiability}/${Responses.customerLiability[0]}/due-date-change?dueDate=${randomGens.generateYesterdaysDate('yyyy-mm-dd')}`);
                    expect(change).CheckResponse();
                })

                await test.step('wait for LPF generation', async() => {
                    const data = await GeneratePayload.receivablesManagement.waitForLPFGeneration(false);
                    expect(data.content).toBeDefined()
                    expect((data.content ?? []).length).toBeGreaterThan(0);
                    Responses.latePaymentFine.push(data.content[0].id);
                })

                await test.step('get and cancel invoice', async() => {
                    const payload = await GeneratePayload.billing.invoiceCancellation();
                    const cancelation = await Request.post(Endpoints.invoiceCancellation, {data: payload});
                    expect(cancelation).CheckResponse();
                })

                await test.step('check invoice status', async() => {
                    await GeneratePayload.billing.waitForInvoiceStatus(Responses.invoice[0], 'CANCELLED');                    
                })

                await test.step('check if receivable is generated while cancellation', async() => {
                    const get = await Request.get(`${Endpoints.invoice}?id=${Responses.invoice[0]}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();
                    const receivableExists = responseBody.liabilitiesAndReceivables.some((item: any) => item.type == 'RECEIVABLE');
                    expect(receivableExists, '❌receivable was not generated after cancellation').toBeTruthy();

                    const receivableGet = await Request.get(`${Endpoints.customerReceivable}/${responseBody.liabilitiesAndReceivables[1].id}`);
                    expect(receivableGet).CheckResponse();
                    expect((await receivableGet.json()).initialAmount).toBe(totalAmount);
                    expect((await receivableGet.json()).currentAmount).toBe(0);
                })

                await test.step('check if MLOs were reversed', async() => {
                    const MLO1 = await Request.get(`${Endpoints.manualLiabilityOffsetting}/${Responses.manualLiabilityOffsetting[1]}`);
                    expect(MLO1).CheckResponse();
                    expect((await MLO1.json()).reversed, `❌ MLO1 was not reversed`).toBe(true);
                    
                    const MLO2 = await Request.get(`${Endpoints.manualLiabilityOffsetting}/${Responses.manualLiabilityOffsetting[0]}`);
                    expect(MLO2).CheckResponse();
                    expect((await MLO2.json()).reversed, `❌ MLO2 was not reversed`).toBe(true);
                })

                await test.step('check if payment is reversed, negative is created, new one is generated', async() => {
                    const paymentGet = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                    expect(paymentGet).CheckResponse();
                    expect((await paymentGet.json()).status, `❌ Payment was not reversed`).toBe("REVERSED");
                    const paymentAmount = (await paymentGet.json()).initialAmount

                    const payload = {
                        "page": 0,
                        "size": 25,
                        "prompt": `${Responses.customer[0].identifier}`,
                        "columns": "PAYMENT_NUMBER",
                        "searchFields": "CUSTOMER_IDENTIFIER",
                        "direction": "DESC"
                    }
                    const listOfPayment = await Request.post(`${Endpoints.payment}/list`, {data: payload});
                    expect(listOfPayment).CheckResponse();
                    const listOfPaymentResponse = await listOfPayment.json();
                    const negativeExists = listOfPaymentResponse.content.some((item: any) => item.initialAmount == -paymentAmount);
                    expect(negativeExists, `❌ Negative payment was not created`).toBeTruthy();
                    expect(listOfPaymentResponse.content??[], `❌ some payment is missing`).toHaveLength(3);
                    expect(listOfPaymentResponse.content[0].initialAmount, `❌ New payment does not have right amount`).toBe(paymentAmount);
                })

                await test.step('check if receivable amounts are rollbacked', async() => {
                    const receivable1 = await Request.get(`${Endpoints.customerReceivable}/${Responses.customerReceivable[0]}`);
                    expect(receivable1).CheckResponse()
                    expect((await receivable1.json()).currentAmount, `❌ Receivable amount was not restored`).toEqual((await receivable1.json()).initialAmount);

                    const receivable2 = await Request.get(`${Endpoints.customerReceivable}/${Responses.customerReceivable[1]}`);
                    expect(receivable2).CheckResponse()
                    expect((await receivable2.json()).currentAmount, `❌ Receivable amount was not restored`).toEqual((await receivable2.json()).initialAmount);
                })

                await test.step('check if deposit has its amount rollbacked', async() => {
                    const depositGet = await Request.get(`${Endpoints.deposit}/${Responses.deposit[0]}`);
                    expect(depositGet).CheckResponse();
                    const depositInitialAmount = (await depositGet.json() as { initialAmount: number }).initialAmount;
                    const depostCurrentAmount = (await depositGet.json() as { currentAmount: number }).currentAmount;
                    expect(depostCurrentAmount, `❌ Deposit amount was not restored`).toEqual(depositInitialAmount)
                })

                await test.step('check if receivable amounts are right', async() => {
                    const response = await Request.get(`${Endpoints.customerReceivable}?page=0&size=25&prompt=${Responses.customer[0].identifier}&customerReceivableSearchBy=CUSTOMER`);
                    expect(response).CheckResponse();
                    const body = await response.json();

                    const totalCurrentAmount = body.content.reduce(
                        (sum: number, item: { currentAmount: number }) => sum + item.currentAmount,
                        0
                    );
                    expect(totalCurrentAmount).toBe((totalAmount - liabilityAmount));
                })

                await test.step('check if liability amounts are right', async() => {
                    const response = await Request.get(`${Endpoints.customerLiability}/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                    expect(response).CheckResponse();
                    const body = await response.json();

                    const totalCurrentAmount = body.content.reduce(
                        (sum: number, item: { currentAmount: number }) => sum + item.currentAmount,
                        0
                    );
                    expect(totalCurrentAmount).toBe(0);
                })
                
                test.info().attach('[REG-1068] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })

            })
        })
    })
})