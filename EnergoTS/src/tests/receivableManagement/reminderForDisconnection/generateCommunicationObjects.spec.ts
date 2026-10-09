import { test, expect /*configuredBaseURL*/ } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';
import { envVariables } from '../../../backend/fixtures/envCashed';
import { customer_liability } from '../../../backend/jsons/payloads/Receivables/customerLiability';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-119]: Reminder for Disconnection', () => {
        test.describe('[REG-910]: Reminder for Disconnection - Create', () => {
            test('[REG-1011]: RFD create + Sending SMS + Sending Email + Generate document', async({Request, GeneratePayload, Responses, Endpoints}) => {
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

                await test.step('Generate reminder for disconnection', async () => {
                    const reminderForDisconnectionPayload = GeneratePayload.receivablesManagement.reminderForDisconnection();
                    reminderForDisconnectionPayload.communicationChannels = ["EMAIL", "SMS", "ON_PAPER"];
                    reminderForDisconnectionPayload.smsTemplateId = envVariables.reminder_disconnection_sms_template;
                    reminderForDisconnectionPayload.documentTemplateId = envVariables.reminder_disconnection_document_template;

                    const reminderForDisconnectionResponse = await Request.post(Endpoints.reminderForDisconnection, {data: reminderForDisconnectionPayload});
                    await expect(reminderForDisconnectionResponse).CheckResponse();

                    Responses.reminderForDisconnection.push(await reminderForDisconnectionResponse.json());
                    await GeneratePayload.receivablesManagement.offsetReminderForDisconnectionTime();
                })

                await test.step('Check generated document', async() => {
                    const get = await Request.get(Endpoints.reminderForDisconnection + `/${Responses.reminderForDisconnection[0]}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();
                    expect(responseBody.subFiles??[]).toHaveLength(1);
                })

                await test.step('Check email', async() => {
                    await expect.poll( async() => {
                        const emailListing = await Request.get(Endpoints.email + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&searchBy=CUSTOMER_IDENTIFIER&communicationTopicId=50`);
                        await expect(emailListing).CheckResponse();
                        const responseBody = await emailListing.json();
                        return responseBody.content??[]
                    },
                    {
                        timeout: 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toHaveLength(1);
                })

                await test.step('check SMS', async() => {
                    await expect.poll( async() => {
                        const smsListing = await Request.get(Endpoints.sms + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&smsCommunicationSearchBy=CUSTOMER_IDENTIFIER&topicOfCommunications=50`);
                        await expect(smsListing).CheckResponse();
                        const responseBody = await smsListing.json()
                        return responseBody.content??[]
                    },
                    {
                        timeout: 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toHaveLength(1)
                })

                test.info().attach('[REG-1011] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })

            })
        })
    })
})