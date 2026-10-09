import {test, expect} from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe('[REG-56]: Receivables Management - Payments', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-115]: Payments', () => {
        test.describe('[REG-874]: Payments - Create + offsetting', () => {
            test('[REG-1036]: Online Payment - Create + Direct offsetting (Easy-pay integration), combine liabilities', async({Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, receivableValidations}) => {
               test.setTimeout(5 * 60 * 1000)
                let checksumInit: any;
                let TID: any;
                let AMOUNT: any;
                let checksumConfirm: any;
                let Date: any;
                let merchantId: any;

                await test.step('Create customer', async() => {
                    const customer_legal = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
                    await expect(customer_legal).CheckResponse();
                    const customerData = await customer_legal.json();
                    Responses.customer.push(customerData);
                });

                await test.step('Create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.initialAmount = 100;
                    const post = await Request.post(Endpoints.customerLiability, {data: payload});
                    expect(post).CheckResponse();
                    Responses.customerLiability.push(await post.json())
                });

                await test.step('Create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.initialAmount = 100;
                    const post = await Request.post(Endpoints.customerLiability, {data: payload});
                    expect(post).CheckResponse();
                    Responses.customerLiability.push(await post.json())
                });
                await test.step('Find easypay collection channel', async() => {
                    const channelListingGet = await Request.get(Endpoints.collectionChannel + `?page=0&size=25&searchBy=NAME&prompt=EasyPay&collectionChannelType=ONLINE`)
                    expect(channelListingGet).CheckResponse();
                    const responseBody = await channelListingGet.json();
                    const channelId = responseBody.content[0].id;

                    Responses.collectionChannel.push(channelId)
                })

                await test.step('check if combined is checked, if not edit channel', async() => {
                    await GeneratePayload.receivablesManagement.modifyChannel(Responses.collectionChannel[0],true);
                })

                await test.step('Find what is current merchant in configuration', async() => {
                    const get = await Request.get(`${Endpoints.systemConfiguration}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();
                    merchantId = responseBody.easyPayMerchantId;
                })

                await test.step('generate checkusm for init-pay', async() => {
                    TID = randomGens.generateTID();
                    const request = await Request.get(`${OnlinePaymentUrl}epay/calculate-check-sum-init?IDN=${Responses.customer[0].customerNumber}&TID=${TID}&MERCHANTID=${merchantId}&TYPE=BILLING`);
                    expect(request).CheckResponse();
                    checksumInit = await request.text();
                });

                await test.step('init payment', async() => {
                    const request = await Request.get(`${OnlinePaymentUrl}epay/init-pay?IDN=${Responses.customer[0].customerNumber}&TID=${TID}&MERCHANTID=${merchantId}&TYPE=BILLING&CHECKSUM=${checksumInit}`);
                    expect(request).CheckResponse();
                    const responseBody = await request.json()
                    expect(responseBody.STATUS).toBe('00');
                    AMOUNT = responseBody.AMOUNT
                });

                await test.step('generate checkusm for confirm payment', async() => {
                    Date = randomGens.generateOnlinePaymentDate(4);
                    const request = await Request.get(`${OnlinePaymentUrl}epay/calculate-check-sum-confirm?DATE=${Date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${Responses.customer[0].customerNumber}&TOTAL=${AMOUNT}&TID=${TID}`);
                    expect(request).CheckResponse();
                    checksumConfirm = await request.text();
                });

                await test.step('confirm payment', async() => {
                    const request = await Request.get(`${OnlinePaymentUrl}epay/confirm-pay?DATE=${Date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${Responses.customer[0].customerNumber}&TOTAL=${AMOUNT}&TID=${TID}&CHECKSUM=${checksumConfirm}`);
                    const responseBody = await request.json();
                    expect(responseBody.STATUS).toBe('00');
                });
                
                await test.step('confirm, that payment was generated and is correct', async() => {
                    const payload = {
                        "page": 0,
                        "size": 25,
                        "prompt": `${Responses.customer[0].identifier}`,
                        "searchFields": "CUSTOMER_IDENTIFIER",
                    }
                    const paymentList = await Request.post(Endpoints.payment+`/list`, {data: payload})
                    expect(paymentList).CheckResponse();
                    const responseBody = await paymentList.json();
                    expect(responseBody.totalElements).toBeGreaterThan(0);
                    expect(responseBody.content[0]).toBeDefined();

                    Responses.payment.push(responseBody.content[0].id)
                })

                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });

                await test.step('check if offsetting happened', async() => {
                    await expect.poll(
                        async () => {
                            const response = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                            expect(response).CheckResponse();
                            const body = await response.json();
                            return body.currentAmount
                        },
                        {
                            message: `Failed object -> id=${Responses.payment[0]}`,
                            timeout: 20_000,
                            intervals: [1000], 
                        }
                    ).toBe(0);
                })

                test.info().attach('[REG-1036] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })
            
            });

            test('[REG-1063]: Online Payment - When liabilities are overdue (Create + direct offsetting + LPF creation), when combined liability', async({Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, receivableValidations}) => {
                test.setTimeout(5 * 60 * 1000)

                let checksumInit: any;
                let TID: any;
                let AMOUNT: any;
                let checksumConfirm: any;
                let Date: any;
                let merchantId: string;

                await test.step('Create customer', async() => {
                    const customer_legal = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
                    await expect(customer_legal).CheckResponse();
                    const customerData = await customer_legal.json();
                    Responses.customer.push(customerData);
                })

                await test.step('generate interest rate', async() => {
                    const interestRate = await Request.post(Endpoints.interestRate, {data: GeneratePayload.receivablesManagement.dailyInterestRate()});
                    expect(interestRate).CheckResponse();
                    Responses.interestRate.push(await interestRate.json());
                })

                await test.step('create liability', async() => {
                    const liability = await Request.post(Endpoints.customerLiability, {data: GeneratePayload.receivablesManagement.overdueLiability()});
                    expect(liability).CheckResponse();;
                    Responses.customerLiability.push(await liability.json());
                })

                await test.step('create liability', async() => {
                    const liability = await Request.post(Endpoints.customerLiability, {data: GeneratePayload.receivablesManagement.overdueLiability()});
                    expect(liability).CheckResponse();
                    Responses.customerLiability.push(await liability.json());
                })

                await test.step('Find easypay collection channel', async() => {
                    const channelListingGet = await Request.get(Endpoints.collectionChannel + `?page=0&size=25&searchBy=NAME&prompt=EasyPay&collectionChannelType=ONLINE`)
                    expect(channelListingGet).CheckResponse();
                    const responseBody = await channelListingGet.json();
                    const channelId = responseBody.content[0].id;

                    Responses.collectionChannel.push(channelId)
                })

                await test.step('check if combined is checked, if not edit channel', async() => {
                    await GeneratePayload.receivablesManagement.modifyChannel(Responses.collectionChannel[0],true);
                })

                await test.step('Find what is current merchant in configuration', async() => {
                    const get = await Request.get(`${Endpoints.systemConfiguration}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();
                    merchantId = responseBody.easyPayMerchantId;
                })

                await test.step('generate checkusm for init-pay', async() => {
                    TID = randomGens.generateTID();
                    const request = await Request.get(`${OnlinePaymentUrl}epay/calculate-check-sum-init?IDN=${Responses.customer[0].customerNumber}&TID=${TID}&MERCHANTID=${merchantId}&TYPE=BILLING`);
                    expect(request).CheckResponse();
                    checksumInit = await request.text();
                })

                await test.step('init payment', async() => {
                    const request = await Request.get(`${OnlinePaymentUrl}epay/init-pay?IDN=${Responses.customer[0].customerNumber}&TID=${TID}&MERCHANTID=${merchantId}&TYPE=BILLING&CHECKSUM=${checksumInit}`);
                    expect(request).CheckResponse();
                    const responseBody = await request.json();
                    expect(responseBody.STATUS).toBe('00');
                    AMOUNT = responseBody.AMOUNT
                })

                await test.step('generate checkusm for confirm payment', async() => {
                    Date = randomGens.generateOnlinePaymentDate(4);
                    const request = await Request.get(`${OnlinePaymentUrl}epay/calculate-check-sum-confirm?DATE=${Date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${Responses.customer[0].customerNumber}&TOTAL=${AMOUNT}&TID=${TID}`);
                    expect(request).CheckResponse();
                    checksumConfirm = await request.text();
                })

                await test.step('confirm payment', async() => {
                    const request = await Request.get(`${OnlinePaymentUrl}epay/confirm-pay?DATE=${Date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${Responses.customer[0].customerNumber}&TOTAL=${AMOUNT}&TID=${TID}&CHECKSUM=${checksumConfirm}`);
                    const responseBody = await request.json();
                    expect(responseBody.STATUS).toBe('00');
                })
                
                await test.step('confirm, that payment was generated and is correct', async() => {
                    const payload = {
                        "page": 0,
                        "size": 25,
                        "prompt": `${Responses.customer[0].identifier}`,
                        "searchFields": "CUSTOMER_IDENTIFIER",
                    }
                    const paymentList = await Request.post(Endpoints.payment+`/list`, {data: payload})
                    expect(paymentList).CheckResponse();
                    const responseBody = await paymentList.json();
                    expect(responseBody.totalElements).toBeGreaterThan(0);
                    expect(responseBody.content[0]).toBeDefined();

                    Responses.payment.push(responseBody.content[0].id)
                });

                await test.step('check if LPFs are generated', async() => {
                    const LPFs = await GeneratePayload.receivablesManagement.waitForLPFGeneration(true);
                    expect(LPFs.content).toBeDefined()
                    expect(LPFs.content??[]).toHaveLength(2)
                })

                await test.step('check if offsetting happened', async() => {
                    await expect.poll(
                        async () => {
                            const response = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                            expect(response).CheckResponse();
                            const body = await response.json();
                            return body.currentAmount
                        },
                        {
                            message: `Failed object -> id=${Responses.payment[0]}`,
                            timeout: 20_000,
                            intervals: [1000], 
                        }
                    ).toBe(0);
                })

                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });

                test.info().attach('[REG-1063] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })

            })

            test('[REG-1180]: [Dev 2] Integration new online payment partners - Virtual POS terminal', async({Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, receivableValidations}) => {
                test.setTimeout(5 * 60 * 1000)
                let checksumInit: any;
                let TID: any;
                let AMOUNT: any;
                let checksumConfirm: any;
                let Date: any;
                let merchantId: any;

                await test.step('Create customer', async() => {
                    const customer_legal = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
                    await expect(customer_legal).CheckResponse();
                    const customerData = await customer_legal.json();
                    Responses.customer.push(customerData);
                });

                await test.step('Create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.initialAmount = 100;
                    const post = await Request.post(Endpoints.customerLiability, {data: payload});
                    expect(post).CheckResponse();
                    Responses.customerLiability.push(await post.json())
                });

                await test.step('Create liability', async() => {
                    const payload = GeneratePayload.receivablesManagement.customer_liability();
                    payload.initialAmount = 100;
                    const post = await Request.post(Endpoints.customerLiability, {data: payload});
                    expect(post).CheckResponse();
                    Responses.customerLiability.push(await post.json())
                });

                await test.step('Find virtualPos collection channel', async() => {
                    const channelListingGet = await Request.get(Endpoints.collectionChannel + `?page=0&size=25&searchBy=NAME&prompt=VirtualPos&collectionChannelType=ONLINE`)
                    expect(channelListingGet).CheckResponse();
                    const responseBody = await channelListingGet.json();
                    const channelId = responseBody.content[0].id;

                    Responses.collectionChannel.push(channelId)
                })

                await test.step('check if combined is checked, if not edit channel', async() => {
                    await GeneratePayload.receivablesManagement.modifyChannel(Responses.collectionChannel[0],true);
                })

                await test.step('Find what is current merchant in configuration', async() => {
                    const get = await Request.get(`${Endpoints.systemConfiguration}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();
                    merchantId = responseBody.virtualPosMerchantId;
                })

                await test.step('generate checkusm for init-pay', async() => {
                    TID = randomGens.generateTID();
                    const request = await Request.get(`${OnlinePaymentUrl}virtualpos/calculate-check-sum-init?IDN=${Responses.customer[0].customerNumber}&TID=${TID}&MERCHANTID=${merchantId}&TYPE=BILLING`);
                    expect(request).CheckResponse();
                    checksumInit = await request.text();
                });

                await test.step('init payment', async() => {
                    const request = await Request.get(`${OnlinePaymentUrl}virtualpos/init-pay?IDN=${Responses.customer[0].customerNumber}&TID=${TID}&MERCHANTID=${merchantId}&TYPE=BILLING&CHECKSUM=${checksumInit}`);
                    expect(request).CheckResponse();
                    const responseBody = await request.json()
                    expect(responseBody.STATUS).toBe('00');
                    AMOUNT = responseBody.AMOUNT
                });

                await test.step('generate checkusm for confirm payment', async() => {
                    Date = randomGens.generateOnlinePaymentDate(4);
                    const request = await Request.get(`${OnlinePaymentUrl}virtualpos/calculate-check-sum-confirm?DATE=${Date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${Responses.customer[0].customerNumber}&TOTAL=${AMOUNT}&TID=${TID}`);
                    expect(request).CheckResponse();
                    checksumConfirm = await request.text();
                });

                await test.step('confirm payment', async() => {
                    const request = await Request.get(`${OnlinePaymentUrl}virtualpos/confirm-pay?DATE=${Date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${Responses.customer[0].customerNumber}&TOTAL=${AMOUNT}&TID=${TID}&CHECKSUM=${checksumConfirm}`);
                    const responseBody = await request.json();
                    expect(responseBody.STATUS).toBe('00');
                });

                await test.step('confirm, that payment was generated and is correct', async() => {
                    const payload = {
                        "page": 0,
                        "size": 25,
                        "prompt": `${Responses.customer[0].identifier}`,
                        "searchFields": "CUSTOMER_IDENTIFIER",
                    }
                    const paymentList = await Request.post(Endpoints.payment+`/list`, {data: payload})
                    expect(paymentList).CheckResponse();
                    const responseBody = await paymentList.json();
                    expect(responseBody.totalElements).toBeGreaterThan(0);
                    expect(responseBody.content[0]).toBeDefined();

                    Responses.payment.push(responseBody.content[0].id)
                })

                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });

                await test.step('check if offsetting happened', async() => {
                    await expect.poll(
                        async () => {
                            const response = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                            expect(response).CheckResponse();
                            const body = await response.json();
                            return body.currentAmount
                        },
                        {
                            message: `Failed object -> id=${Responses.payment[0]}`,
                            timeout: 20_000,
                            intervals: [1000],
                        }
                    ).toBe(0);
                })

                test.info().attach('[REG-1180] [Dev 2] Integration new online payment partners - Virtual POS terminal', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })
            });

            test.skip('[REG-1180]: [Dev 2] Integration new online payment partners - Virtual POS terminal, on overdue liabilities', async({Request, GeneratePayload, Responses, Endpoints, OnlinePaymentUrl, receivableValidations}) => {
                test.setTimeout(5 * 60 * 1000)

                let checksumInit: any;
                let TID: any;
                let AMOUNT: any;
                let checksumConfirm: any;
                let Date: any;
                let merchantId: string;

                await test.step('Create customer', async() => {
                    const customer_legal = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
                    await expect(customer_legal).CheckResponse();
                    const customerData = await customer_legal.json();
                    Responses.customer.push(customerData);
                })

                await test.step('generate interest rate', async() => {
                    const interestRate = await Request.post(Endpoints.interestRate, {data: GeneratePayload.receivablesManagement.dailyInterestRate()});
                    expect(interestRate).CheckResponse();
                    Responses.interestRate.push(await interestRate.json());
                })

                await test.step('create liability', async() => {
                    const liability = await Request.post(Endpoints.customerLiability, {data: GeneratePayload.receivablesManagement.overdueLiability()});
                    expect(liability).CheckResponse();;
                    Responses.customerLiability.push(await liability.json());
                })

                await test.step('create liability', async() => {
                    const liability = await Request.post(Endpoints.customerLiability, {data: GeneratePayload.receivablesManagement.overdueLiability()});
                    expect(liability).CheckResponse();
                    Responses.customerLiability.push(await liability.json());
                })

                await test.step('Find VirtualPos collection channel', async() => {
                    const channelListingGet = await Request.get(Endpoints.collectionChannel + `?page=0&size=25&searchBy=NAME&prompt=VirtualPos&collectionChannelType=ONLINE`)
                    expect(channelListingGet).CheckResponse();
                    const responseBody = await channelListingGet.json();
                    const channelId = responseBody.content[0].id;

                    Responses.collectionChannel.push(channelId)
                })

                await test.step('check if combined is checked, if not edit channel', async() => {
                    await GeneratePayload.receivablesManagement.modifyChannel(Responses.collectionChannel[0],true);
                })

                await test.step('Find what is current merchant in configuration', async() => {
                    const get = await Request.get(`${Endpoints.systemConfiguration}`);
                    expect(get).CheckResponse();
                    const responseBody = await get.json();
                    merchantId = responseBody.virtualPosMerchantId;
                })

                await test.step('generate checkusm for init-pay', async() => {
                    TID = randomGens.generateTID();
                    const request = await Request.get(`${OnlinePaymentUrl}virtualpos/calculate-check-sum-init?IDN=${Responses.customer[0].customerNumber}&TID=${TID}&MERCHANTID=${merchantId}&TYPE=BILLING`);
                    expect(request).CheckResponse();
                    checksumInit = await request.text();
                })

                await test.step('init payment', async() => {
                    const request = await Request.get(`${OnlinePaymentUrl}virtualpos/init-pay?IDN=${Responses.customer[0].customerNumber}&TID=${TID}&MERCHANTID=${merchantId}&TYPE=BILLING&CHECKSUM=${checksumInit}`);
                    expect(request).CheckResponse();
                    const responseBody = await request.json();
                    expect(responseBody.STATUS).toBe('00');
                    AMOUNT = responseBody.AMOUNT
                })

                await test.step('generate checkusm for confirm payment', async() => {
                    Date = randomGens.generateOnlinePaymentDate(4);
                    const request = await Request.get(`${OnlinePaymentUrl}virtualpos/calculate-check-sum-confirm?DATE=${Date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${Responses.customer[0].customerNumber}&TOTAL=${AMOUNT}&TID=${TID}`);
                    expect(request).CheckResponse();
                    checksumConfirm = await request.text();
                })

                await test.step('confirm payment', async() => {
                    const request = await Request.get(`${OnlinePaymentUrl}virtualpos/confirm-pay?DATE=${Date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${Responses.customer[0].customerNumber}&TOTAL=${AMOUNT}&TID=${TID}&CHECKSUM=${checksumConfirm}`);
                    const responseBody = await request.json();
                    console.log(`\n${OnlinePaymentUrl}virtualpos/confirm-pay?DATE=${Date}&TYPE=BILLING&MERCHANTID=${merchantId}&IDN=${Responses.customer[0].customerNumber}&TOTAL=${AMOUNT}&TID=${TID}&CHECKSUM=${checksumConfirm}\n`)
                    expect(responseBody.STATUS).toBe('00');
                })

                await test.step('confirm, that payment was generated and is correct', async() => {
                    const payload = {
                        "page": 0,
                        "size": 25,
                        "prompt": `${Responses.customer[0].identifier}`,
                        "searchFields": "CUSTOMER_IDENTIFIER",
                    }
                    const paymentList = await Request.post(Endpoints.payment+`/list`, {data: payload})
                    expect(paymentList).CheckResponse();
                    const responseBody = await paymentList.json();
                    expect(responseBody.totalElements).toBeGreaterThan(0);
                    expect(responseBody.content[0]).toBeDefined();

                    Responses.payment.push(responseBody.content[0].id)
                });

                await test.step('check if LPFs are generated', async() => {
                    const LPFs = await GeneratePayload.receivablesManagement.waitForLPFGeneration(true);
                    expect(LPFs.content).toBeDefined()
                    expect(LPFs.content??[]).toHaveLength(2)
                })

                await test.step('check if offsetting happened', async() => {
                    await expect.poll(
                        async () => {
                            const response = await Request.get(Endpoints.payment + `/${Responses.payment[0]}`);
                            expect(response).CheckResponse();
                            const body = await response.json();
                            return body.currentAmount
                        },
                        {
                            message: `Failed object -> id=${Responses.payment[0]}`,
                            timeout: 20_000,
                            intervals: [1000],
                        }
                    ).toBe(0);
                })

                await test.step('validate payment', async () => {
                    const paymentId = await Responses.payment[0];
                    await receivableValidations.paymentValidation(paymentId);
                });

                test.info().attach('[REG-1180]: [Dev 2] Integration new online payment partners - Virtual POS terminal, on overdue liabilities', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })
            });
        });
    });
});