import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';
import { envVariables } from '../../../backend/fixtures/envCashed';
import { CustomerPayloads } from '../../../backend/jsons/payloadGenerators/domains/CustomerPayloads';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-129]: Rescheduling', () => {
        test.describe('[REG-485]: interest with every installment calculation', () => {
            test('[REG-1155]: Rescheduling - interest with first installment - happy pass', async({Request, GeneratePayload, Responses, Endpoints}) => {
                let sumOfInterestAmount: number
                let initialAmount: number
                await test.step('Create customer', async() => {
                    const customer_legal = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
                    await expect(customer_legal).CheckResponse();
                    const customerData = await customer_legal.json();
                    Responses.customer.push(customerData);
                })

                await test.step('create yearly interest rate', async() => {
                    const payload = GeneratePayload.receivablesManagement.dailyInterestRate();
                    payload.type = "YEARLY"
                    const post = await Request.post(Endpoints.interestRate, {data: payload});
                    expect(post).CheckResponse();
                    Responses.interestRate.push(await post.json());
                })

                await test.step('create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    initialAmount = 100
                    payload.initialAmount = initialAmount
                    payload.occurrenceDate = randomGens.generateOneWeekBeforeDate("dd-mm-yyyy");
                    payload.dueDate = randomGens.generateOneWeekBeforeDate("dd-mm-yyyy");
                    payload.applicableInterestRateId = Responses.interestRate[0];
                    const liability = await Request.post(Endpoints.customerLiability, {data: payload});
                    expect(liability).CheckResponse();
                    const responseBody = await liability.json();
                    Responses.customerLiability.push(responseBody);
                })

                await test.step('create assessment', async() => {
                    const post = await Request.post(Endpoints.customerAssessment, {data: GeneratePayload.receivablesManagement.customer_assessment()});
                    expect(post).CheckResponse();
                    Responses.customerAssessment.push(await post.json());
                })
                
                await test.step('create rescheduling', async() => {
                    const request = await Request.post(Endpoints.rescheduling, {data: await GeneratePayload.receivablesManagement.rescheduling("3","INTEREST_WITH_THE_FIRST_INSTALLMENT")});
                    expect(request).CheckResponse();
                    const responseBody = await request.json();
                    Responses.rescheduling.push(responseBody);
                })

                await test.step('check rescheduling', async() => {
                    const get = await Request.get(`${Endpoints.rescheduling}/${Responses.rescheduling[0]}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();
                    sumOfInterestAmount = responseBody.sumOfInterestAmount;

                    expect(responseBody.reschedulingStatus).toBe("EXECUTED");
                    expect(responseBody.interestType).toBe("INTEREST_WITH_THE_FIRST_INSTALLMENT");
                    expect(responseBody.reschedulingLiabilityResponses[0].liabilityId).toBe(Responses.customerLiability[0]);
                    expect(responseBody.reschedulingLiabilityResponses[0].liabilityCurrentAmountInLeva).toBe(0);
                    expect(responseBody.reschedulingPlans??[]).toHaveLength(3);
                    expect(responseBody.sumOfInterestAmount).toBeGreaterThan(0);
                })

                await test.step('check if LPF is generated', async() => {
                    await GeneratePayload.receivablesManagement.waitForLPFGeneration(true);
                })

                await test.step('check overall liabilities', async() => {
                    const listingOfLiability = await Request.get(`${Endpoints.customerLiability}/list?page=0&size=25&columns=ID&direction=DESC&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                    expect(listingOfLiability).CheckResponse();
                    const responseBody = await listingOfLiability.json();

                    expect(responseBody.content??[]).toHaveLength(5);
                    const interestLiabilityAmount = responseBody.content.some((x: any) => x.initialAmount == sumOfInterestAmount);
                    expect(interestLiabilityAmount).toBeTruthy()

                    //check overall liability amounts, if calculations are right
                    const totalCurrentAmount = responseBody.content.reduce(
                        (sum: number, item: { currentAmount: number }) => sum + item.currentAmount,
                        0
                    );
                    const initial = Number(totalCurrentAmount.toFixed(2));
                    expect(initial).toBe(initialAmount + sumOfInterestAmount);
                })

                test.info().attach('[REG-1155] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses({Responses}), null, 2),
                    contentType: 'application/json'
                })
            })
        })
    })
})