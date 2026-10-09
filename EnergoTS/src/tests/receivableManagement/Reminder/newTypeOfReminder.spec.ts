import { test, expect } from '../../../backend/fixtures/reminder.fixtures';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';
import { envVariables } from '../../../backend/fixtures/envCashed';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-118]: Reminder', () => {
        test.describe('[REG-882]: Reminder - Create', () => {
            test('[REG-1159]: Reminder - New type of reminder(Trigger is RFD) + communication object generation', async({Request, GeneratePayload, Responses, Endpoints}) => {
                test.setTimeout(10 * 60 * 1000);

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

                await test.step('Generate reminder for disconnection and check if customer is included', async () => {
                    const post = await Request.post(Endpoints.reminderForDisconnection, {data: GeneratePayload.receivablesManagement.reminderForDisconnection()});
                    await expect(post).CheckResponse();

                    Responses.reminderForDisconnection.push(await post.json());
                    await GeneratePayload.receivablesManagement.offsetReminderForDisconnectionTime();

                    const checkCustomer = await Request.get(`${Endpoints.reminderForDisconnection}/second-tab?page=0&size=25&powerSupplyDisconnectionReminderId=${Responses.reminderForDisconnection[0]}`)
                    expect((await checkCustomer.json()).content.length).toBeGreaterThan(0)
                })

                await test.step('create process periodicity', async () => {
                    const process_periodicity = await Request.post(Endpoints.processPeriodicity, {data: await GeneratePayload.operationsManagement.process_periodicity()});
                    const process_periodicity_json = await process_periodicity.json();

                    await expect(process_periodicity).CheckResponse();
                    Responses.processPeriodicity.push(process_periodicity_json);
                });

                await test.step('create reminder with trigger -> RFD', async() => {
                    const payload = GeneratePayload.receivablesManagement.reminder();
                    payload.triggerForLiabilities = "REMINDER_FOR_DISCONNECTION";
                    payload.communicationChannels = ["EMAIL", "SMS"];
                    payload.smsTemplateId = envVariables.reminder_sms_template;
                    
                    const post = await Request.post(Endpoints.reminder, {data: payload});
                    expect(post).CheckResponse();
                    Responses.reminder.push(await post.json())
                })

                await test.step('run reminder job', async() => {
                    await expect.poll( async() => {
                        const reminderJob = await Request.post(Endpoints.reminder + `/job-test`, {timeout: 2 * 1000 * 60});
                        return reminderJob.status();
                    },
                    {
                        timeout: 2 * 1000 * 60,
                        intervals: [2000, 5000],
                    }).toBe(200);
                })

                await test.step('loop processes and check that none of it is in progress', async() => {
                    await expect.poll( async() => {
                        const processesListing = await Request.get(`${Endpoints.process}?status=IN_PROGRESS&status=AWAITING&status=NOT_STARTED&status=PAUSED&createdDateFrom=${randomGens.generateYesterdaysDate('yyyy-mm-dd')}T20%3A00%3A38.488Z&startDateFrom=${randomGens.generateYesterdaysDate('yyyy-mm-dd')}T20%3A00%3A42.256Z&searchBy=ALL&page=0&size=25&sortBy=CREATE_DATE&sortDirection=DESC&prompt=REMINDER`);
                        expect(processesListing).CheckResponse();
                        const responseBody = await processesListing.json();
                        return responseBody.content??[]
                    },
                    {
                        timeout: 1 * 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toHaveLength(0)
                });

                await test.step('Check email', async() => {
                    await expect.poll( async() => {
                        const emailListing = await Request.get(Endpoints.email + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&searchBy=CUSTOMER_IDENTIFIER`);
                        await expect(emailListing).CheckResponse();
                        const responseBody = await emailListing.json();
                        const emailExists = responseBody.content.some((X: any) => X.communicationTopic === "Reminder");

                        return emailExists
                    },
                    {
                        timeout: 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toBeTruthy();
                })

                await test.step('check SMS', async() => {
                    await expect.poll( async() => {
                        const smsListing = await Request.get(Endpoints.sms + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&smsCommunicationSearchBy=CUSTOMER_IDENTIFIER`);
                        await expect(smsListing).CheckResponse();
                        const responseBody = await smsListing.json()
                        const smsExists = responseBody.content.some((X: any) => X.communicationTopic === "Reminder");
                       
                        return smsExists
                    },
                    {
                        timeout: 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toBeTruthy()
                })

                test.info().attach('[REG-1159] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })

            })

            /* !!! this upcoming case is not finished, it needs DB update in the middle, we need to comromise query usage inside
               of the test, or we might use endpoint if development creates onse !!!*/
            test.skip('[REG-1160]: New type of reminder(Trigger is RFD) + communication object generation (list of contract, with postponoment in days value)', async({Request, GeneratePayload, Responses, Endpoints}) => { 
                test.setTimeout(10 * 60 * 1000);
                let contractNumber: any;

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

                await test.step('Create contract and get it', async () => {
                    const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())

                    const get = await Request.get(`${Endpoints.productContract}/${(await contract.json()).id}?`);
                    expect(get).CheckResponse();
                    contractNumber = (await get.json()).basicParameters.contractNumber
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

                await test.step('Generate reminder for disconnection and check if customer is included', async () => {
                    const post = await Request.post(Endpoints.reminderForDisconnection, {data: GeneratePayload.receivablesManagement.reminderForDisconnection()});
                    await expect(post).CheckResponse();

                    Responses.reminderForDisconnection.push(await post.json());
                    await GeneratePayload.receivablesManagement.offsetReminderForDisconnectionTime();

                    const checkCustomer = await Request.get(`${Endpoints.reminderForDisconnection}/second-tab?page=0&size=25&powerSupplyDisconnectionReminderId=${Responses.reminderForDisconnection[0]}`)
                    expect((await checkCustomer.json()).content.length).toBeGreaterThan(0)
                })

                await test.step('change Date and time to send to customer as yesterday', async() => {
                    
                })

                await test.step('create process periodicity', async () => {
                    const process_periodicity = await Request.post(Endpoints.processPeriodicity, {data: await GeneratePayload.operationsManagement.process_periodicity()});
                    const process_periodicity_json = await process_periodicity.json();

                    await expect(process_periodicity).CheckResponse();
                    Responses.processPeriodicity.push(process_periodicity_json);
                })

                await test.step('create reminder with trigger -> RFD', async() => {
                    const payload = GeneratePayload.receivablesManagement.reminder();
                    payload.triggerForLiabilities = "REMINDER_FOR_DISCONNECTION";
                    payload.communicationChannels = ["EMAIL", "SMS"];
                    payload.smsTemplateId = envVariables.reminder_sms_template;
                    payload.listType = 'CONTRACTS'
                    payload.listOfCustomers = contractNumber
                    payload.postponementInDays = 1
                    
                    const post = await Request.post(Endpoints.reminder, {data: payload});
                    expect(post).CheckResponse();
                    Responses.reminder.push(await post.json())
                })

            })
        })
    })
})