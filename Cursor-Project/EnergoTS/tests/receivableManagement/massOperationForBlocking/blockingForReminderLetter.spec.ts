import exp from 'constants';
import { test, expect/*, configuredBaseURL */} from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';
import { randomGens } from '../../../utils/randomGens';
import { envVariables } from '../../../fixtures/envCashed';
import { RequestWrapper } from '../../../utils/RequestWrapper';
import { Console } from 'console';

test.describe('[REG-56]: Receivables Management', /* {tag: '@receivableManagement'}, */ () => {
    test.describe('[REG-127]: Mass Operation for Blocking', () => {
        test.describe('[REG-905]: Create - Mass Operation for Blocking', () => {
            test('[REG-1017]: Block list of customer for reminder letter', async({Request, GeneratePayload, Responses, Endpoints}) => {
                
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

                await test.step('Get billing group and add overdue liability', async() => {
                    const contractGet = await Request.get(Endpoints.productContract + `/${Responses.productContract[0].id}`);
                    await expect(contractGet).CheckResponse();
                    const responseBody = await contractGet.json();
                    
                    const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability();
                    liabilityPayload.billingGroupId = responseBody.billingGroups[0]!.id;
                    const liabilityPost = await Request.post(Endpoints.customerLiability, {data: liabilityPayload});
                    await expect(liabilityPost).CheckResponse()
                });

                await test.step('create mass blocking for reminder letter', async() => {
                    const payload = GeneratePayload.receivablesManagement.mass_operation_for_blocking();
                    payload.blockingForReminderLetters!.fromDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');
                    payload.isBlockForReminderLetters = true;
                    payload.blockingForSupplyTermination = null;
                    payload.blockingForCalculation = null;
                    payload.blockingForPayment = null;
                    payload.blockingForLiabilitiesOffsetting = null;

                    const blockingPost = await Request.post(Endpoints.massOperationForBlocking, {data: payload});
                    await expect(blockingPost).CheckResponse();
                    Responses.massOperationForBlocking.push(await blockingPost.json());
                });

                await test.step('create periodicity for reminder', async() => {
                    const periodicityPost = await Request.post(Endpoints.processPeriodicity, {data: await GeneratePayload.operationsManagement.process_periodicity()});
                    await expect(periodicityPost).CheckResponse();
                    Responses.processPeriodicity.push(await periodicityPost.json());
                });

                await test.step('create reminder', async() => {
                    const payload = GeneratePayload.receivablesManagement.reminder();
                    payload.communicationChannels = ['EMAIL','ON_PAPER','SMS'];
                    payload.smsTemplateId = envVariables.reminder_sms_template;
                    payload.documentTemplateId = envVariables.reminder_document_template;

                    const reminderPost = await Request.post(Endpoints.reminder, {data: payload});
                    await expect(reminderPost).CheckResponse();
                    Responses.reminder.push(await reminderPost.json())
                });

                await test.step('run reminder job', async() => {
                    const job = await expect.poll( async() => {
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
                        timeout: 2 * 60 * 1000,
                        intervals: [2000, 5000],
                    }
                    ).toHaveLength(0)
                });

                await test.step('wait for communication object generation', async() => {
                    const sleep = (ms: number) => new Promise<void>(resolve => setTimeout(resolve, ms));
                    await sleep(2000);
                })

                await test.step('Check if communication objects have been generated', async() => {
                    //email
                    const emailListing = await Request.get(Endpoints.email + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&searchBy=CUSTOMER_IDENTIFIER`);
                    await expect(emailListing).CheckResponse();
                    const responseBodyEmail = await emailListing.json();
                    expect(responseBodyEmail.content).toHaveLength(0);
                    //sms
                    const smsListing = await Request.get(Endpoints.sms + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&smsCommunicationSearchBy=CUSTOMER_IDENTIFIER`);
                    await expect(smsListing).CheckResponse();
                    const responseBodySms = await smsListing.json();
                    expect(responseBodySms.content).toHaveLength(0);
                });

                await test.step('delete reminder', async() => {
                    const deletion = await Request.delete(`${Endpoints.reminder}/${Responses.reminder[0]}`);
                    expect(deletion).CheckResponse();
                });

                test.info().attach('[REG-1015] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })
            });
        });
    });
});
