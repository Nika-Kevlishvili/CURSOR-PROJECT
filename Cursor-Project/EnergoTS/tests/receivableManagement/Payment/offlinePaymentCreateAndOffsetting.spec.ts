import { test, expect } from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';

test.describe('[REG-56]: Receivables Management - Payments', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-115]: Payments', () => {
        test.describe('[REG-874]: Create', () => {
            test('[REG-874]', async ({Request, GeneratePayload, Responses, Endpoints, receivableValidations}) => {
                await test.step('create collection channel', async () => {
                    const collection_channel = await Request.post(Endpoints.collectionChannel, {data: GeneratePayload.receivablesManagement.collection_channel()});
                    await expect(collection_channel).CheckResponse();
                    Responses.collectionChannel.push(await collection_channel.json());
                });

                await test.step('create payment package', async () => {
                    const paymentPackagePayload = GeneratePayload.receivablesManagement.payment_package();
                    const payment_package = await Request.post(Endpoints.paymentPackage, {data: paymentPackagePayload});
                    await expect(payment_package).CheckResponse();
                    Responses.paymentPackage.push(await payment_package.json());
                });
                
                await test.step('create customer', async () => {
                    const customer_legal = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer_legal).CheckResponse();
                    const customerData = await customer_legal.json();
                    Responses.customer.push(customerData);
                });

                await test.step('create payment', async () => {
                    const payment = await Request.post(Endpoints.payment, {data: await GeneratePayload.receivablesManagement.payment()});
                    await expect(payment).CheckResponse();
                    Responses.payment.push(await payment.json());
                });

                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });

                test.info().attach('[REG-874] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
        test.describe('[REG-874]: Payments - Create + offsetting', () => {
            test.describe('[REG-1005]: Payment create + direct offsetting + receivable generation', () =>{
                test('[REG-1005]: Create payment + direct offsetting + receivable generation', async({Request, GeneratePayload, Responses, Endpoints, receivableValidations}) => {
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

                    await test.step('create liability', async() => {;
                        const payload = GeneratePayload.receivablesManagement.customer_liability();
                        payload.initialAmount = 100;
                        const liability = await Request.post(Endpoints.customerLiability, {data: payload});
                        await expect(liability).CheckResponse();
                        const liabilityResponse = await liability.json();
                        Responses.customerLiability.push(liabilityResponse);
                    });

                    await test.step('create payment', async() => {
                        const payload = await GeneratePayload.receivablesManagement.payment();
                        payload.initialAmount = 200;
                        const payment = await Request.post(Endpoints.payment, {data: payload});
                        await expect(payment).CheckResponse();
                        const paymentResponse = await payment.json();
                        Responses.payment.push(paymentResponse);
                    });

                    await test.step('validate payment', async () => {
                        const paymentId = await Responses.payment[0];
                        await receivableValidations.paymentValidation(paymentId);
                    });

                    await test.step('check generated payment and liability amounts (should be 0)', async() => {
                        const paymentId = await Responses.payment[0];
                        const paymentGet = await Request.get(Endpoints.payment + `/${paymentId}`);
                        await expect(paymentGet).CheckResponse();
                        const paymentGetResponse = await paymentGet.json();
                        expect(paymentGetResponse.currentAmount).toEqual(0);

                        const liabilityId = await Responses.customerLiability[0];
                        const liabilityGet = await Request.get(Endpoints.customerLiability + `/${liabilityId}`);
                        await expect(liabilityGet).CheckResponse();
                        const liabilityGetResponse = await liabilityGet.json();
                        expect(liabilityGetResponse.currentAmount).toEqual(0);
                        //check if we have liability in payment response body
                        expect(paymentGetResponse.offsettingResponseList[0].id).toEqual(liabilityGetResponse.id)
                    });

                    await test.step('check generated receivable and its amount', async() => {
                        const paymentId = await Responses.payment[0];
                        const paymentGet = await Request.get(Endpoints.payment + `/${paymentId}`);
                        const responseData = await paymentGet.json();
                        const receivable = responseData.offsettingResponseList.find((item: any) => item.offsettingObjectType === 'RECEIVABLE');
                        expect(receivable).toBeTruthy();
                        expect(receivable.amount).toEqual(-100);
                    });
                    test.info().attach('[REG-1005] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                    });
                });
            });
            
            test('[REG-1031]: Payment create + automatic offsetting + receivable generation', async({Request, GeneratePayload, Responses, Endpoints, receivableValidations}) => {
                  test.setTimeout(3 * 60 * 1000); // 3 minutes
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
                    Responses.customerLiability.push(liabilityId);
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

                await test.step('create payment', async() => {
                    const newPayload = await GeneratePayload.receivablesManagement.payment(true, true);
                    newPayload.initialAmount = 160;
                    const payment = await Request.post(Endpoints.payment, { data: newPayload });
                    await expect(payment).CheckResponse();
                    const paymentData = await payment.json();
                    Responses.payment.push(paymentData);
                });
                
                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });

                await test.step('check amounts, check receivables billing group', async() => {
                    const liabilityId = await Responses.customerLiability[0];
                    const liabilityGet = await Request.get(Endpoints.customerLiability + `/${liabilityId}`);
                    const liabData = await liabilityGet.json();
                    const paymentGet = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                    const paymentGetResponse = await paymentGet.json();
                    expect(paymentGetResponse.currentAmount).toEqual(0);
                    //check if we have both liability and receivable connected to payment
                    const receivable = paymentGetResponse.offsettingResponseList.find((item: any) => item.offsettingObjectType === 'RECEIVABLE');
                    const liability = paymentGetResponse.offsettingResponseList.find((item: any) => item.offsettingObjectType === 'LIABILITY');
                    
                    const receivableId = await receivable.id;
                    const receivableGet = await Request.get(Endpoints.customerReceivable + `/${receivableId}`);
                    const receivableResponse = await receivableGet.json();
                    const receivableCurrentAmount = receivableResponse.currentAmount;

                    expect(receivable).toBeTruthy();
                    expect(liability).toBeTruthy();
                    expect(liabData.currentAmount).toEqual(0);
                    expect(receivableCurrentAmount).toBeCloseTo(paymentGetResponse.initialAmount - liabData.initialAmount, 2);
                    expect(receivableResponse.billingGroupResponse.id).toBe(paymentGetResponse.contractBillingGroupId.id);                    
                });

                test.info().attach('[REG-1031] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
                
            });
        });
     });
 });