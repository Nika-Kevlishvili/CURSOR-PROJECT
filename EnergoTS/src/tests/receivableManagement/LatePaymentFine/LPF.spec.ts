import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-125]: Late Payment Fine', () => {
        test.describe('[REG-979]: Late Payment Fine - Create', () => {
            test('[REG-980]: Late Payment Fine Create - LPF by using manual liability (without billing group)', async({Request, GeneratePayload, Responses, Endpoints, receivableValidations}) => {
                let liabilityAmount: number;
                let interestPercent: number;
                let exchangeRate: any;
                let initialAmount: number;

                await test.step('Create customer and get customer data', async() => {
                    const customerPost = await Request.post("/customer", {data: GeneratePayload.customers.customer_legal()});

                    await expect(customerPost).CheckResponse();
                    Responses.customer.push(await customerPost.json());
                });

                await test.step('generate interest rate', async() => {
                    const payload = GeneratePayload.receivablesManagement.dailyInterestRate();
                    interestPercent = payload.interestRatePeriods[0].amountInPercent
                    const interestRate = await Request.post(Endpoints.interestRate, {data: payload});
                    expect(interestRate).CheckResponse();
                    Responses.interestRate.push(await interestRate.json());
                })

                await test.step('create liability', async() => {
                    const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability()
                    initialAmount = randomGens.getRandomNumber();
                    liabilityPayload.initialAmount = initialAmount
                    liabilityPayload.applicableInterestRateId = Responses.interestRate[0];
                    liabilityPayload.dueDate = randomGens.generateYesterdaysDate("dd-mm-yyyy")
                    liabilityPayload.occurrenceDate = randomGens.generateOneWeekBeforeDate("dd-mm-yyyy")
                    const liabilityPost = await Request.post(`${Endpoints.customerLiability}`, {data: liabilityPayload});

                    await expect(liabilityPost).CheckResponse();
                    Responses.customerLiability.push(await liabilityPost.json());
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
                    const newPayload = await GeneratePayload.receivablesManagement.payment(false, false);
                    newPayload.initialAmount = initialAmount;
                    const payment = await Request.post(Endpoints.payment, { data: newPayload });
                    await expect(payment).CheckResponse();
                    const paymentData = await payment.json();
                    Responses.payment.push(paymentData);
                })

                await test.step('check if LPFs are generated', async() => {
                    const LPFs = await GeneratePayload.receivablesManagement.waitForLPFGeneration(false);
                    expect(LPFs.content).toBeDefined();
                    expect(LPFs.content??[]).toHaveLength(1);
                    Responses.latePaymentFine.push(LPFs.content[0].id);
                })

                await test.step('check declared currencies and exchange rate', async() => {
                    exchangeRate = await GeneratePayload.receivablesManagement.exchangeRateForMainCurrency();
                })

                await test.step('validate LPF', async() => {
                    const LPFID = await Responses.latePaymentFine[0];
                    await receivableValidations.LatePaymentFineValidation(LPFID);
                })

                const round2 = (value: number): number => Math.round((Number(value) + Number.EPSILON) * 100) / 100;

                await test.step('check generated LPF what validations do not check', async() => {

                    const get = await Request.get(`${Endpoints.latePaymentFine}/${Responses.latePaymentFine[0]}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();

                    expect(responseBody.amount, '❌ Amount is not correct ❌').toBeCloseTo(initialAmount*(interestPercent/100), 2);
                    expect(responseBody.tableResponse[0].latePaidAmount).toBe(initialAmount);
                    expect(responseBody.tableResponse[0].overdueStartDate).toBe(randomGens.generateTodaysDate("dd.mm.yyyy"))
                    expect(responseBody.tableResponse[0].overdueEndDate).toBe(randomGens.generateTodaysDate("dd.mm.yyyy"))
                    expect(responseBody.parentLiabilityShortResponse.id).toBe(Responses.customerLiability[0]);
                    expect(responseBody.amountInOtherCurrency).toBe(round2(responseBody.amount * exchangeRate));
                })

                await test.step('check LPF generated liability', async() => {
                    const liability = await Request.get(`${Endpoints.customerLiability}/list?page=0&size=25&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER&initialAmountTo=${initialAmount*(interestPercent/100)}`);
                    expect(liability).CheckResponse();
                    const responseBody = await liability.json();

                    const liabilityId = responseBody.content[0].id;
                    const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
                    expect(liabilityGet).CheckResponse();
                    const liabilityResponse = await liabilityGet.json();

                    expect(liabilityResponse.initialAmount).toBe(liabilityResponse.currentAmount)
                    expect(liabilityResponse.initialAmount).toBeCloseTo(initialAmount*(interestPercent/100), 2);
                    expect(liabilityResponse.initialAmountInOtherCurrency).toBe(round2(liabilityResponse.initialAmount * exchangeRate));
                    expect(liabilityResponse.outgoingDocumentFromExternalSystem).toMatch(/^ЕЛС-\d{10}$/);
                })

                test.info().attach('[REG-980] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })
            })
        })
    })
})