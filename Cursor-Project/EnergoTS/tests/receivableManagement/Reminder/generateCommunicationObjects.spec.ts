import { test, expect} from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';
import { randomGens } from '../../../utils/randomGens';
import { envVariables } from '../../../fixtures/envCashed';
import { customer_liability } from '../../../jsons/payloads/create/Receivables/customerLiability';


test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-118]: Reminder', () => {
        test.describe('[REG-882]: Reminder - Create', () => {
            test('[REG-1010]: Reminder create - Create reminder + sending SMS + sending Email ', async({Request, GeneratePayload, Responses, Endpoints}) => {
                test.setTimeout(10 * 60 * 1000);
                
                await test.step('Create customer', async () => {
                    const createCustomer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(createCustomer).CheckResponse();
                    Responses.customer.push(await createCustomer.json());
                });

                await test.step('generate price components', async () => {

                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = 100;

                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();

                    Responses.priceComponent.push(await price.json());
                })

                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('generate 1 PODs', async () => {
                    for (let i = 0; i < 1; i++) {
                        const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                        await expect(podSettlement).CheckResponse();
                        Responses.pod.push(await podSettlement.json());
                    }
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

                await test.step('Activate all PODs', async () => {
                    for (let i = 0; i < Responses.pod.length; i++) {
                        const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(i)});
                        await expect(podActivation).CheckResponse();
                    }
                });

                await test.step('Create billing run for manual invoice', async () => {
                    const payload = await GeneratePayload.billing.manualInvoice();
                    payload.commonParameters.sendingAnInvoice = 'PAPER';
                    payload.commonParameters.invoiceDueDate = 'DATE';
                    payload.commonParameters.dueDate = randomGens.generateTodaysDate('yyyy-mm-dd');
                    const createManualInvoice = await Request.post(Endpoints.billingRun, {data: payload});

                    await expect(createManualInvoice).CheckResponse();
                    
                    const responseBody = await createManualInvoice.json();
                    Responses.billingRun.push(responseBody);
                })


                await test.step('make the invoice real', async () => {
                    await GeneratePayload.billing.waitForInvoiceGeneration()
                });

                await test.step('Take liability', async() => {
                    let responseBody: any
                    await expect.poll( async() => {
                        const listing = await Request.get(`${Endpoints.customerLiability}/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`)
                        responseBody = await listing.json();
                        return responseBody.content??[]
                    },
                    {
                        timeout: 1 * 1000 * 60,
                        intervals: [2000, 5000],
                    }).toHaveLength(1)
                    Responses.customerLiability.push(responseBody.content[0].id)
                });

                await test.step('create process periodicity', async () => {
                    const process_periodicity = await Request.post(Endpoints.processPeriodicity, {data: await GeneratePayload.operationsManagement.process_periodicity()});
                    const process_periodicity_json = await process_periodicity.json();

                    await expect(process_periodicity).CheckResponse();
                    Responses.processPeriodicity.push(process_periodicity_json);
                });

                /* this is the first iteration, when reminder has trigger on due date -> liability should have due date = TODAY() */
                await test.step('create reminder with trigger on due date', async() => {
                    const payload = GeneratePayload.receivablesManagement.reminder();
                    payload.communicationChannels = ['EMAIL','SMS'];
                    payload.smsTemplateId = envVariables.reminder_sms_template;

                    const reminderPost = await Request.post(Endpoints.reminder, {data: payload});
                    await expect(reminderPost).CheckResponse();
                    Responses.reminder.push(await reminderPost.json());
                });

                await test.step('run reminder job', async() => {
                    await expect.poll( async() => {
                        const reminderJob = await Request.post(Endpoints.reminder + `/job-test`, {timeout: 2 * 1000 * 60});
                        return reminderJob.status();
                    },
                    {
                        timeout: 2 * 1000 * 60,
                        intervals: [2000, 5000],
                    }).toBe(200);
                });

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
                        const smsListing = await Request.get(Endpoints.sms + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&smsCommunicationSearchBy=CUSTOMER_IDENTIFIER`);
                        await expect(smsListing).CheckResponse();
                        const responseBody = await smsListing.json()
                        return responseBody.content??[]
                    },
                    {
                        timeout: 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toHaveLength(1)
                });

                await test.step('delete reminder', async() => {
                    const deletion = await Request.delete(`${Endpoints.reminder}/${Responses.reminder[0]}`);
                    expect(deletion).CheckResponse();
                });
                
                /* this is the second iteration, when reminder trigger is when overdue, liability due date should be yesterday(when postponoment in days is null) */
                await test.step('make liability overdue', async() => {
                    const changetime = await Request.put(`${Endpoints.customerLiability}/${Responses.customerLiability[0]}/due-date-change?dueDate=${randomGens.generateYesterdaysDate("yyyy-mm-dd")}`);
                    expect(changetime).CheckResponse();
                });

                await test.step('create new reminder with trigger when overdue', async() => {
                    const payload = GeneratePayload.receivablesManagement.reminder();
                    payload.triggerForLiabilities = 'WHEN_OVERDUE'
                    payload.communicationChannels = ['EMAIL','SMS'];
                    payload.smsTemplateId = envVariables.reminder_sms_template;

                    const reminderPost = await Request.post(Endpoints.reminder, {data: payload});
                    await expect(reminderPost).CheckResponse();
                    Responses.reminder.push(await reminderPost.json());
                });

                await test.step('run reminder job', async() => {
                    await expect.poll( async() => {
                        const reminderJob = await Request.post(Endpoints.reminder + `/job-test`, {timeout: 2 * 1000 * 60});
                        return reminderJob.status();
                    },
                    {
                        timeout: 2 * 1000 * 60,
                        intervals: [2000, 5000],
                    }).toBe(200);
                });

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
                        return responseBody.content??[]
                    },
                    {
                        timeout: 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toHaveLength(2);
                })
                
                await test.step('check SMS', async() => {
                    await expect.poll( async() => {
                        const smsListing = await Request.get(Endpoints.sms + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&smsCommunicationSearchBy=CUSTOMER_IDENTIFIER`);
                        await expect(smsListing).CheckResponse();
                        const responseBody = await smsListing.json()
                        return responseBody.content??[]
                    },
                    {
                        timeout: 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toHaveLength(2)
                });

                await test.step('delete reminder', async() => {
                    const deletion = await Request.delete(`${Endpoints.reminder}/${Responses.reminder[1]}`);
                    expect(deletion).CheckResponse();
                });

                /* this is the third iteration, reminder trigger is when invoiced, liability should have invoice as an outgoing document and should be overdue */
                await test.step('create new reminder with trigger when invoiced', async() => {
                    const payload = GeneratePayload.receivablesManagement.reminder();
                    payload.triggerForLiabilities = 'WHEN_INVOICES';
                    payload.communicationChannels = ['EMAIL','SMS'];
                    payload.smsTemplateId = envVariables.reminder_sms_template;

                    const reminderPost = await Request.post(Endpoints.reminder, {data: payload});
                    await expect(reminderPost).CheckResponse();
                    Responses.reminder.push(await reminderPost.json());
                });

                await test.step('run reminder job', async() => {
                    await expect.poll( async() => {
                        const reminerJob = await Request.post(Endpoints.reminder + `/job-test`, {timeout: 2 * 1000 * 60});
                        return reminerJob.status();
                    },
                    {
                        timeout: 2 * 1000 * 60,
                        intervals: [2000, 5000],
                    }).toBe(200);
                });

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
                        return responseBody.content??[]
                    },
                    {
                        timeout: 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toHaveLength(3);
                })

                await test.step('check SMS', async() => {
                    await expect.poll( async() => {
                        const smsListing = await Request.get(Endpoints.sms + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&smsCommunicationSearchBy=CUSTOMER_IDENTIFIER`);
                        await expect(smsListing).CheckResponse();
                        const responseBody = await smsListing.json()
                        return responseBody.content??[]
                    },
                    {
                        timeout: 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toHaveLength(3)
                });

                await test.step('delete reminder', async() => {
                    const deletion = await Request.delete(`${Endpoints.reminder}/${Responses.reminder[2]}`);
                    expect(deletion).CheckResponse();
                });

                test.info().attach('[REG-1010] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });

            });
        });
    });
});
