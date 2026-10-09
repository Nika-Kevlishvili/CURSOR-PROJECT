import { test, expect } from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';
import { randomGens } from '../../../utils/randomGens';
import { envVariables } from '../../../fixtures/envCashed';
import { customer_liability } from '../../../jsons/payloads/create/Receivables/customerLiability';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-123]: Reconnection of Power Supply', () => {
        test.describe('[REG-1039]: Create', () => {
            test('[REG-1038]: Reconnection create with one pod (express disconnection in the way / charge fee Request for disconnection in the way)', async({Request, GeneratePayload, Responses, Endpoints}) => {
                test.setTimeout(10 * 60 * 1000); // 10 minutes
                /* Added checking */
                let totalAmount: any;

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

                await test.step('Generate reminder for disconnection', async () => {
                    const reminderForDisconnectionPayload = GeneratePayload.receivablesManagement.reminderForDisconnection();
                    const reminderForDisconnectionResponse = await Request.post(Endpoints.reminderForDisconnection, {data: reminderForDisconnectionPayload});
                    await expect(reminderForDisconnectionResponse).CheckResponse();

                    Responses.reminderForDisconnection.push(await reminderForDisconnectionResponse.json());
                    await GeneratePayload.receivablesManagement.offsetReminderForDisconnectionTime()
                })

                await test.step('Generate request for disconnection', async () => {
                    const requestForDisconnectionPayload = GeneratePayload.receivablesManagement.requestForDisconnection();
                    const requestForDisconnectionResponse = await Request.post(Endpoints.requestForDisconnection, {data: requestForDisconnectionPayload});
                    await expect(requestForDisconnectionResponse).CheckResponse();

                    Responses.requestForDisconnection.push(await requestForDisconnectionResponse.json());
                })

                await test.step('charge fee', async () => {
                    await Request.post(`disconnection-of-power-supply-requests/calculate-tax/${Responses.requestForDisconnection[0]}`)
                })

                await test.step('Generate disconnection of power supply', async () => {
                    const disconnectionOfPowerSupplyPayload = GeneratePayload.receivablesManagement.disconnectionOfPowerSupply();
                    disconnectionOfPowerSupplyPayload.disconnectedRequest[0].expressReconnection = true;

                    await GeneratePayload.receivablesManagement.changeTaxForGridOperator(20,20)
                    const disconnectionOfPowerSupplyResponse = await Request.post(Endpoints.disconnectionOfPowerSupply, {data: disconnectionOfPowerSupplyPayload});
                    await expect(disconnectionOfPowerSupplyResponse).CheckResponse();


                    await GeneratePayload.receivablesManagement.changeTaxForGridOperator(1,1)
                    Responses.disconnectionOfPowerSupply.push(await disconnectionOfPowerSupplyResponse.json());
                })

                await test.step('check if liabilities are generated with reconnection type invoice', async() => {
                    const liabilityResponse = await Request.get(Endpoints.customerLiability + `/list?page=0&size=25&columns=ID&direction=DESC&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                    expect(liabilityResponse).CheckResponse();
                    const liabilityResponseBody = await liabilityResponse.json();
                    expect(liabilityResponseBody.totalElements).toBeGreaterThan(2);
                    expect((liabilityResponseBody.content??[]).length).toBeGreaterThan(2);

                    const chargeFeeAmount = liabilityResponseBody.content[1].initialAmount;
                    const expressLiabilityAmount = liabilityResponseBody.content[0].initialAmount;
                    const originalLiability = liabilityResponseBody.content[3].initialAmount;
                    totalAmount = chargeFeeAmount + expressLiabilityAmount + originalLiability
                })

                await test.step('pay liabilities with payment for future reconnection', async() => {
                    const collectionChannel = await Request.post(Endpoints.collectionChannel, {data: GeneratePayload.receivablesManagement.collection_channel()});
                    await expect(collectionChannel).CheckResponse();
                    Responses.collectionChannel.push(await collectionChannel.json());

                    const paymentPackage = await Request.post(Endpoints.paymentPackage, {data: GeneratePayload.receivablesManagement.payment_package()});
                    await expect(paymentPackage).CheckResponse();
                    Responses.paymentPackage.push(await paymentPackage.json());

                    const payload = await GeneratePayload.receivablesManagement.payment();
                    payload.initialAmount = totalAmount;
                    const payment = await Request.post(Endpoints.payment, {data: payload});
                    await expect(payment).CheckResponse();
                    Responses.payment.push(await payment.json());
                })

                await test.step('Reconnection create', async() => {
                    const reconnectionPost = await Request.post(Endpoints.reconnectionOfPowerSupply, {data: GeneratePayload.receivablesManagement.reconnectionOfPowerSupply()});
                    expect(reconnectionPost).CheckResponse();
                    Responses.reconnectionOfPowerSupply.push(await reconnectionPost.json())
                })

                test.info().attach('[REG-1038] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })
            })
        })
    })
})
