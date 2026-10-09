import exp from 'constants';
import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-127]: Mass Operation for Blocking', () => {
        test.describe('[REG-905]: Create - Mass Operation for Blocking', () => {
            test('[REG-1015]: Block interest charging for list of customers', async({Request, GeneratePayload, Responses, Endpoints}) => {
                let twoMinute: any;
                twoMinute = 2 * 60 * 1000;
                let MainAmount: any;
                MainAmount = 100;
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

                await test.step('generate interest rate', async() => {
                    const payload = GeneratePayload.receivablesManagement.interest_rate();
                    payload.type = "DAILY";
                    payload.interestRatePeriods[0].amountInPercent = MainAmount;
                    payload.interestRatePeriods[0].applicableInterestRate = 0.01;
                    const interestRate = await Request.post(Endpoints.interestRate, {data: payload});
                    expect(interestRate).CheckResponse();
                    const interestData = await interestRate.json();
                    Responses.interestRate.push(interestData);
                });

                await test.step('create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.initialAmount = MainAmount;
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
                    payload.initialAmount = MainAmount;
                    const payment = await Request.post(Endpoints.payment, {data: payload});
                    expect(payment).CheckResponse();
                    const responseBody = await payment.json();
                    Responses.payment.push(responseBody);
                });

                await test.step('Create mass blocking for interest charging', async() => {
                    const payload = GeneratePayload.receivablesManagement.mass_operation_for_blocking();
                    payload.blockingForSupplyTermination = null;
                    payload.blockingForReminderLetters = null;
                    payload.blockingForPayment = null;
                    payload.blockingForLiabilitiesOffsetting = null;
                    payload.isBlockForCalculation = true;
                    payload.blockingForCalculation!.fromDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');

                    const massPost = await Request.post(Endpoints.massOperationForBlocking, {data: payload});
                    expect(massPost).CheckResponse();
                    Responses.massOperationForBlocking.push(await massPost.json());
                });
                
                await test.step('LPF job', async() => {
                    const job = await expect.poll( async() => {
                        const lpfJob = await Request.post(Endpoints.latePaymentFine + `/job`, {timeout: twoMinute});
                        const responseBody = await lpfJob.json();
                        return responseBody;
                    },
                    {
                        timeout: twoMinute,
                        intervals: [2000, 5000],
                    }).toBe(1);
                });

                await test.step('Check if LPF is generated', async() => {
                    const lpfLising = await Request.get(Endpoints.latePaymentFine + `/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&searchBy=CUSTOMER&direction=DESC&sortingType=NUMBER`);
                    expect(lpfLising).CheckResponse();
                    const responseBody = await lpfLising.json();
                    expect(responseBody.totalElements).toBe(0);
                });

                test.info().attach('[REG-1015] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });

            });
            /** this test fails due to potential bug. LPF is not being generated, cause of MOFB object, object blocks interest calculation for two days not whole period.*/
            test('[REG-1049]: Block for interest charging - Exclude calculation logic', async({Request, GeneratePayload, Responses, Endpoints}) => {
                test.setTimeout(5 * 60 * 1000);
                let MainAmount: any;
                MainAmount = 100;
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

                await test.step('generate interest rate', async() => {
                    const payload = GeneratePayload.receivablesManagement.interest_rate();
                    payload.type = "DAILY";
                    payload.interestRatePeriods[0].amountInPercent = MainAmount;
                    payload.interestRatePeriods[0].applicableInterestRate = 0.01;
                    const interestRate = await Request.post(Endpoints.interestRate, {data: payload});
                    expect(interestRate).CheckResponse();
                    const interestData = await interestRate.json();
                    Responses.interestRate.push(interestData);
                });

                await test.step('create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.initialAmount = MainAmount;
                    payload.occurrenceDate = randomGens.generateOneWeekBeforeDate("dd-mm-yyyy");
                    payload.dueDate = randomGens.generateOneWeekBeforeDate("dd-mm-yyyy");
                    payload.applicableInterestRateId = Responses.interestRate[0];
                    const liability = await Request.post(Endpoints.customerLiability, {data: payload});
                    expect(liability).CheckResponse();
                    const responseBody = await liability.json();
                    Responses.customerLiability.push(responseBody);
                });

                await test.step('create payment', async() => {
                    const payload = await GeneratePayload.receivablesManagement.payment();
                    payload.initialAmount = MainAmount;
                    const payment = await Request.post(Endpoints.payment, {data: payload});
                    expect(payment).CheckResponse();
                    const responseBody = await payment.json();
                    Responses.payment.push(responseBody);
                });

                await test.step('Create mass blocking for interest charging', async() => {
                    const payload = GeneratePayload.receivablesManagement.mass_operation_for_blocking();
                    payload.blockingForSupplyTermination = null;
                    payload.blockingForReminderLetters = null;
                    payload.blockingForPayment = null;
                    payload.blockingForLiabilitiesOffsetting = null;
                    payload.isBlockForCalculation = true;
                    payload.blockingForCalculation!.fromDate = randomGens.generateYesterdaysDate('yyyy-mm-dd');
                    payload.blockingForCalculation!.toDate = randomGens.generateTodaysDate('yyyy-mm-dd');

                    const massPost = await Request.post(Endpoints.massOperationForBlocking, {data: payload});
                    expect(massPost).CheckResponse();
                    Responses.massOperationForBlocking.push(await massPost.json());
                });

                await test.step('LPF job and amount check', async() => {
                    const job = await GeneratePayload.receivablesManagement.waitForLPFGeneration(false);
                    expect(job.content.length).toBeGreaterThan(0)
                    expect(job.content[0].amount).toEqual(MainAmount*5)
                });

                test.info().attach('[REG-1049] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });

            });
        });
    });
});