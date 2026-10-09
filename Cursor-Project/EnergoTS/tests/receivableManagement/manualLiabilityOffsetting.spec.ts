import { test, expect } from '../../fixtures/baseFixture';
import { randomGens } from '../../utils/randomGens';

test.describe('[REG-56]: Receivables Management - Manual Liability Offsetting', {tag: '@receivableManagement'}, () => {
        test.describe('[REG-114]: Manual liability offsetting', () => {
            test.describe('[REG-831]: Create MLO - With liability and receivable offsetting', () => {
                test('[REG-831]: create simple MLO with participant liability and receivable', async({Request, GeneratePayload, Responses}) => {
                    await test.step('Create customer and get customer data', async() => {
                        const customerPost = await Request.post("/customer", {data: GeneratePayload.customers.customer_legal()});

                        await expect(customerPost).CheckResponse();
                        Responses.customer.push(await customerPost.json());
                    });

                    await test.step('create liability', async() => {
                        const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability()
                        liabilityPayload.initialAmount = 100;
                        const liabilityPost = await Request.post('/customer-liability', {data: liabilityPayload});

                        await expect(liabilityPost).CheckResponse();
                        Responses.customerLiability.push(await liabilityPost.json());
                    });

                    await test.step('create recievable', async() => {
                        const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable()
                        receivablePayload.initialAmount = 100;
                        const receivablePost = await Request.post(`/customer-receivable`, {data: receivablePayload});

                        await expect(receivablePost).CheckResponse();
                        Responses.customerReceivable.push(await receivablePost.json());
                    });

                    await test.step('create MLO with rec and liab', async() => {
                        const MLOCreate = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_L_R()});

                        await expect(MLOCreate).CheckResponse();
                        Responses.manualLiabilityOffsetting.push(await MLOCreate.json());
                    });

                    await test.step('check participant objects GETs and their current amounts', async() => {
                        const checkRec = await Request.get(`/customer-receivable/${Responses.customerReceivable[0]}`);
                        await expect(checkRec).CheckResponse();

                        const get = await checkRec.json();
                        expect(await get.currentAmount).toBe(0);

                        const checkLiab = await Request.get(`/customer-liability/${Responses.customerLiability[0]}`);
                        await expect(checkLiab).CheckResponse();

                        const get2 = await checkLiab.json();
                        expect(await get2.currentAmount).toBe(0);
                    });
                });
            });
            test.describe('[REG-832]: Create MLO - With With liability and deposit offsetting', () => {
                test('[REG-832]: Create MLO with deposit and liability', async({Request, GeneratePayload, Responses}) => {
                    await test.step('Create customer and get customer data', async() => {
                        const customerPost = await Request.post("/customer", {data: GeneratePayload.customers.customer_legal()});
                        await expect(customerPost).CheckResponse();
                        Responses.customer.push(await customerPost.json());
                    });

                    await test.step('create deposit and find its liability', async() => {
                        const depositPayload = GeneratePayload.receivablesManagement.deposit();
                        depositPayload.initialAmount = 100;
                        const createDeposit = await Request.post(`/deposit`, {data: depositPayload})
                        await expect(createDeposit).CheckResponse();
                        Responses.deposit.push(await createDeposit.json());
                        
                        const customerData = await Request.get(`/customer/${Responses.customer[0].id}`);
                        const customerIdentifier = (await customerData.json()).identifier;
                        const depositLiability = await Request.get(`/customer-liability/list?page=0&size=25&columns=ID&direction=DESC&prompt=${customerIdentifier}&searchFields=CUSTOMER`)
                        const data = await depositLiability.json();
                        Responses.customerLiability.push(data.content[0].id);
                    });

                    await test.step('create receivable', async() => {
                        const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                        receivablePayload.initialAmount = 100;
                        const receivable = await Request.post("/customer-receivable", {data: receivablePayload});
                        await expect(receivable).CheckResponse();
                        Responses.customerReceivable.push(await receivable.json());
                    });

                    await test.step('Pay deposits liability by using MLO', async() => {
                        const MLOCreate = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_L_R()});
                        await expect(MLOCreate).CheckResponse();
                        Responses.manualLiabilityOffsetting.push(await MLOCreate.json());
                    });

                    await test.step('start job to update deposit current amount', async() => {
                        const job = await Request.post("/deposit/job");
                        await expect(job).CheckResponse();
                        const getDeposit = await Request.get(`/deposit/${Responses.deposit[0]}`)
                        await expect(getDeposit).CheckResponse();
                        const data = await getDeposit.json();
                        expect(data.currentAmount).toEqual(data.initialAmount);
                    });

                    await test.step('create liability', async() => {
                        const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability()
                        liabilityPayload.initialAmount = 100;
                        const post = await Request.post('/customer-liability', {data: liabilityPayload});
                        await expect(post).CheckResponse();
                        Responses.customerLiability.push(await post.json());
                    });

                    await test.step('create MLO use deposit and liability', async() => {
                        const MLOCreate = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_D_L()});
                        await expect(MLOCreate).CheckResponse();
                        Responses.manualLiabilityOffsetting.push(await MLOCreate.json());
                    });

                    await test.step('check if current amounts are 0', async() => {
                        const depGet = await Request.get(`/deposit/${Responses.deposit[0]}`);
                        await expect(depGet).CheckResponse();
                        const data = await depGet.json();
                        expect(data.currentAmount).toBe(0);
                        
                        const liabilityGet = await Request.get(`/customer-liability/${Responses.customerLiability[1]}`);
                        await expect(liabilityGet).CheckResponse();
                        const data2 = await liabilityGet.json();
                        expect(data2.currentAmount).toBe(0);
                    });
                });
            });
            test.describe("[REG-838]: Create MLO - negative payment", () => {
                test('[REG-838]:  Create MLO - negative payment', async({Request, GeneratePayload, Responses}) => {
                    await test.step('Create customer and get customer data', async() => {
                        const customerPost = await Request.post("/customer", {data: GeneratePayload.customers.customer_legal()});
                        await expect(customerPost).CheckResponse();
                        Responses.customer.push(await customerPost.json());
                    });

                    await test.step('create receivable', async() => {
                        const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                        receivablePayload.initialAmount = 100;
                        const post = await Request.post("/customer-receivable", {data: receivablePayload});
                        await expect(post).CheckResponse();
                        Responses.customerReceivable.push(await post.json());
                    });

                    await test.step('create payment package and collection channel for neg payment', async() => {
                        const collection_channel = await Request.post("/collection-channel", {data: GeneratePayload.receivablesManagement.collection_channel()});
                        await expect(collection_channel).CheckResponse();
                        Responses.collectionChannel.push(await collection_channel.json());
                        
                        const packagePayload = GeneratePayload.receivablesManagement.payment_package();
                        const post = await Request.post("/payment-package", {data: packagePayload});
                        await expect(post).CheckResponse();
                        Responses.paymentPackage.push(await post.json());
                    });

                    await test.step('create negative payment', async() => {
                        const payload = await GeneratePayload.receivablesManagement.payment();
                        payload.initialAmount = -100;
                        const post = await Request.post("/payment", {data: payload});
                        await expect(post).CheckResponse();
                        Responses.payment.push(await post.json());
                    })

                    await test.step('MLO create', async() => {
                        const post = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_NP_R()});
                        await expect(post).CheckResponse();
                        Responses.manualLiabilityOffsetting.push(await post.json());
                    });

                    await test.step('check if gets are working and amounts are 0', async() => {
                        const get = await Request.get(`/payment/${Responses.payment[0]}`);
                        await expect(get).CheckResponse();
                        const data = await get.json();
                        expect(data.currentAmount).toBe(0);
                        
                        const get2 = await Request.get(`/customer-receivable/${Responses.customerReceivable[0]}`);
                        await expect(get2).CheckResponse();
                        const data2 = await get2.json();
                        expect(data2.currentAmount).toBe(0);
                    });
                });
            });
            test.describe('[REG-927]: MLO process - Reverse', () => {
                test.describe('[REG-928]: MLO reverse - successful case - reverse MLO, which used liability and receivable/deposit', () => {
                    test('[REG-928]: scenario 1 - use deposit and liability in MLO', async({Request, GeneratePayload, Responses}) => {
                        await test.step('Create customer and get customer data', async() => {
                            const customerPost = await Request.post("/customer", {data: GeneratePayload.customers.customer_legal()});
                            await expect(customerPost).CheckResponse();
                            Responses.customer.push(await customerPost.json());
                        });

                        await test.step('create deposit and find its liability', async() => {
                            const depositPayload = GeneratePayload.receivablesManagement.deposit();
                            depositPayload.initialAmount = 100;
                            const createDeposit = await Request.post(`/deposit`, {data: depositPayload})
                            await expect(createDeposit).CheckResponse();
                            Responses.deposit.push(await createDeposit.json());
                            
                            const customerData = await Request.get(`/customer/${Responses.customer[0].id}`);
                            const customerIdentifier = (await customerData.json()).identifier;
                            const depositLiability = await Request.get(`/customer-liability/list?page=0&size=25&columns=ID&direction=DESC&prompt=${customerIdentifier}&searchFields=CUSTOMER`)
                            const data = await depositLiability.json();
                            Responses.customerLiability.push(data.content[0].id);
                        });

                        await test.step('create receivable', async() => {
                            const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                            receivablePayload.initialAmount = 100;
                            const receivable = await Request.post("/customer-receivable", {data: receivablePayload});
                            await expect(receivable).CheckResponse();
                            Responses.customerReceivable.push(await receivable.json());
                        });

                        await test.step('Pay deposits liability by using MLO', async() => {
                            const MLOCreate = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_L_R()});
                            await expect(MLOCreate).CheckResponse();
                            Responses.manualLiabilityOffsetting.push(await MLOCreate.json());
                        });

                        await test.step('start job to update deposit current amount', async() => {
                            const job = await Request.post("/deposit/job");
                            await expect(job).CheckResponse();
                            const getDeposit = await Request.get(`/deposit/${Responses.deposit[0]}`)
                            await expect(getDeposit).CheckResponse();
                            const data = await getDeposit.json();
                            expect(data.currentAmount).toEqual(data.initialAmount);
                        });

                        await test.step('create liability', async() => {
                            const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability()
                            liabilityPayload.initialAmount = 100;
                            const post = await Request.post('/customer-liability', {data: liabilityPayload});
                            await expect(post).CheckResponse();
                            Responses.customerLiability.push(await post.json());
                        });

                        await test.step('create MLO use deposit and liability', async() => {
                            const MLOCreate = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_D_L()});
                            await expect(MLOCreate).CheckResponse();
                            Responses.manualLiabilityOffsetting.push(await MLOCreate.json());
                        });

                        await test.step('check if current amounts are 0', async() => {
                            const depGet = await Request.get(`/deposit/${Responses.deposit[0]}`);
                            await expect(depGet).CheckResponse();
                            const data = await depGet.json();
                            expect(data.currentAmount).toBe(0);
                            
                            const liabilityGet = await Request.get(`/customer-liability/${Responses.customerLiability[1]}`);
                            await expect(liabilityGet).CheckResponse();
                            const data2 = await liabilityGet.json();
                            expect(data2.currentAmount).toBe(0);
                        });

                        await test.step('reverse previously created MLO', async() => {
                            const reverse = await Request.post(`/manual-liability-offsetting/reverse/${Responses.manualLiabilityOffsetting[1]}`);
                            await expect(reverse).CheckResponse();
                            const get = await Request.get(`/manual-liability-offsetting/${Responses.manualLiabilityOffsetting[1]}`);
                            await expect(get).CheckResponse();
                            const data = await get.json();
                            expect(data.reversed).toBe(true);
                        });
                    });
                    test('[REG-928]: scenatio 2 - use regular rec and liab in MLO', async({Request, GeneratePayload, Responses}) => {
                        await test.step('create customer', async() => {
                            const customerPost = await Request.post("/customer", {data: GeneratePayload.customers.customer_legal()});
                            await expect(customerPost).CheckResponse();
                            Responses.customer.push(await customerPost.json());
                        });

                        await test.step('create liability', async() => {
                            const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability()
                            liabilityPayload.initialAmount = 100;
                            const post = await Request.post('/customer-liability', {data: liabilityPayload});
                            await expect(post).CheckResponse();
                            Responses.customerLiability.push(await post.json());
                        });

                        await test.step('create recevable', async() => {
                            const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                            receivablePayload.initialAmount = 100;
                            const receivable = await Request.post("/customer-receivable", {data: receivablePayload});
                            await expect(receivable).CheckResponse();
                            Responses.customerReceivable.push(await receivable.json());
                        });

                        await test.step('create MLO', async() => {
                            const MLOCreate = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_L_R()});
                            await expect(MLOCreate).CheckResponse();
                            Responses.manualLiabilityOffsetting.push(await MLOCreate.json());
                        });

                        await test.step('Reverse MLO', async() => {
                            const reverse = await Request.post(`/manual-liability-offsetting/reverse/${Responses.manualLiabilityOffsetting[0]}`);
                            await expect(reverse).CheckResponse();
                            const get = await Request.get(`/manual-liability-offsetting/${Responses.manualLiabilityOffsetting[0]}`);
                            await expect(get).CheckResponse();
                            const data = await get.json();
                            expect(data.reversed).toBe(true);
                        });
                    });
                });
                test.describe('[REG-929]: MLO reverse - Negative case - reverse MLO, which used negative payment and receivable', () => {
                    test('[REG-929]: negative case using negative payment', async({Request, GeneratePayload, Responses}) => {
                        await test.step('create customer', async() => {
                            const customerPost = await Request.post("/customer", {data: GeneratePayload.customers.customer_legal()});
                            await expect(customerPost).CheckResponse();
                            Responses.customer.push(await customerPost.json());
                        });

                        await test.step('create recevable', async() => {
                            const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                            receivablePayload.initialAmount = 100;
                            const receivable = await Request.post("/customer-receivable", {data: receivablePayload});
                            await expect(receivable).CheckResponse();
                            Responses.customerReceivable.push(await receivable.json());
                        });

                        await test.step('create payment package and collection channel for neg payment', async() => {
                            const payload = GeneratePayload.receivablesManagement.collection_channel();
                            payload.name = `AUTOMATION${randomGens.generateCurrentTimeStamp()}`;
                            const collection_channel = await Request.post("/collection-channel", {data: payload});
                            await expect(collection_channel).CheckResponse();
                            Responses.collectionChannel.push(await collection_channel.json());
                            
                            const packagePayload = GeneratePayload.receivablesManagement.payment_package();
                            const post = await Request.post("/payment-package", {data: packagePayload});
                            await expect(post).CheckResponse();
                            Responses.paymentPackage.push(await post.json());
                        });

                        await test.step('create negative payment', async() => {
                            const payload = await GeneratePayload.receivablesManagement.payment();
                            payload.initialAmount = -100;
                            const post = await Request.post("/payment", {data: payload});
                            await expect(post).CheckResponse();
                            Responses.payment.push(await post.json());
                        });

                        await test.step('MLO create', async() => {
                            const post = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_NP_R()});
                            await expect(post).CheckResponse();
                            Responses.manualLiabilityOffsetting.push(await post.json());
                        });

                        await test.step('check if gets are working and amounts are 0', async() => {
                            const get = await Request.get(`/payment/${Responses.payment[0]}`);
                            await expect(get).CheckResponse();
                            const data = await get.json();
                            expect(data.currentAmount).toBe(0);
                            
                            const get2 = await Request.get(`/customer-receivable/${Responses.customerReceivable[0]}`);
                            await expect(get2).CheckResponse();
                            const data2 = await get2.json();
                            expect(data2.currentAmount).toBe(0);
                        });

                        await test.step('try to reverse MLO', async() => {
                            const reverse = await Request.post(`/manual-liability-offsetting/reverse/${Responses.manualLiabilityOffsetting[0]}`);
                            expect(reverse.ok()).toBeFalsy();
                            const get = await Request.get(`/manual-liability-offsetting/${Responses.manualLiabilityOffsetting[0]}`);
                            const data = await get.json();
                            expect(data.reversed).toBe(false);
                        });
                    });
                });
            });
            test.describe('[REG-936]: MLO - listing', () => {
                test.describe('[REG-937]: MLO listing - Sorting', () => {
                    test('[REG-937]: sort data with given columns', async({Request,GeneratePayload}) => {
                        await test.step('order data by Id', async() => {
                            const idDesc = await Request.get(`/manual-liability-offsetting/list?sortBy=ID&page=0&size=25&direction=DESC`);
                            await expect(idDesc).CheckResponse();
                            const highId = (await idDesc.json()).content[0].id
                            const idAsc = await Request.get(`/manual-liability-offsetting/list?sortBy=ID&page=0&size=25&direction=ASC`);
                            await expect(idAsc).CheckResponse();
                            const lowId = (await idAsc.json()).content[0].id
                            expect(lowId).toBeLessThan(highId);
                        });
                        await test.step('order data by date', async() => {
                            const dateAsc = await Request.get('/manual-liability-offsetting/list?page=0&size=25&sortBy=DATE&direction=ASC&searchBy=ALL');
                            expect(dateAsc).toBeTruthy();
                            const lowDate = new Date((await dateAsc.json()).content[0].manualLiabilityDate);
                            const dateDesc = await Request.get('/manual-liability-offsetting/list?page=0&size=25&sortBy=DATE&direction=DESC&searchBy=ALL');
                            await expect(dateDesc).CheckResponse();
                            const highDate = new Date((await dateDesc.json()).content[0].manualLiabilityDate);
                            expect(highDate.getTime()).toBeGreaterThan(lowDate.getTime());
                        });
                        // await test.step('order by customer', async() => {
                        //     const cusAsc = await Request.get('/manual-liability-offsetting/list?page=0&size=25&sortBy=CUSTOMER&direction=ASC&searchBy=ALL');
                        //     await expect(cusAsc).CheckResponse();
                        //     const customer1 = (await cusAsc.json()).content[0].customer
                        //     const cusDesc = await Request.get('/manual-liability-offsetting/list?page=0&size=25&sortBy=CUSTOMER&direction=DESC&searchBy=ALL');
                        //     await expect(cusDesc).CheckResponse();
                        //     const customer = (await cusDesc.json()).content[0].customer
                        //     //   ახლა არ მუშაობს ქასთუმერით სორტირება და აუთპუტი არ ვიცი რა აქვს რომ გასწორდება მერე დავასრულებ ამ სტეფს
                        // });
                        await test.step('order data by column - Reversed', async() => {
                            const reversedAsc = await Request.get('/manual-liability-offsetting/list?page=0&size=25&sortBy=REVERSED&direction=ASC');
                            await expect(reversedAsc).CheckResponse();
                            const value1 = (await reversedAsc.json()).content[0].reversed;
                            const reversedDesc = await Request.get('/manual-liability-offsetting/list?page=0&size=25&sortBy=REVERSED&direction=DESC');
                            await expect(reversedDesc).CheckResponse();
                            const value2 = (await reversedDesc.json()).content[0].reversed;
                            expect(value1).not.toBe(value2);
                            expect(value1).toBe("NO");
                            expect(value2).toBe("YES");
                        });
                    });
                });
                test.describe('[REG-938]: MLO listing - Search', () => {
                   test('[REG-938]: search data with diff enums', async({Request, GeneratePayload, Responses}) => {
                    await test.step('Create customer and get customer data', async() => {
                        const customerPost = await Request.post("/customer", {data: GeneratePayload.customers.customer_legal()});
                        await expect(customerPost).CheckResponse();
                        Responses.customer.push(await customerPost.json());
                    });

                    await test.step('create liability', async() => {
                        const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability()
                        liabilityPayload.initialAmount = 100;
                        const liabilityPost = await Request.post('/customer-liability', {data: liabilityPayload});
                        await expect(liabilityPost).CheckResponse();
                        Responses.customerLiability.push(await liabilityPost.json());
                    });

                    await test.step('create recievable', async() => {
                        const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                        receivablePayload.initialAmount = 100;
                        const receivablePost = await Request.post(`/customer-receivable`, {data: receivablePayload});
                        await expect(receivablePost).CheckResponse();
                        Responses.customerReceivable.push(await receivablePost.json());
                    });

                    await test.step('create MLO with rec and liab', async() => {
                        const MLOCreate = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_L_R()});
                        await expect(MLOCreate).CheckResponse();
                        Responses.manualLiabilityOffsetting.push(await MLOCreate.json());
                    });

                    await test.step('search data with "NUMBER"', async() => {
                        const searchNumber = await Request.get(`/manual-liability-offsetting/list?page=0&size=25&sortBy=ID&direction=DESC&prompt=${Responses.manualLiabilityOffsetting[0]}&searchBy=NUMBER`);
                        await expect(searchNumber).CheckResponse();
                        const response = await searchNumber.json();
                        expect(response.content).not.toBeEmpty;
                        expect(response.content[0].id).toEqual(Responses.manualLiabilityOffsetting[0]);
                    });

                    await test.step('search with customer', async() => {
                        const customerData = await Request.get(`/customer/${Responses.customer[0].id}`);
                        const customerIdentifier = (await customerData.json()).identifier;
                        
                        const searchCustomer = await Request.get(`/manual-liability-offsetting/list?page=0&size=25&sortBy=ID&direction=DESC&prompt=${customerIdentifier}&searchBy=CUSTOMER`);
                        await expect(searchCustomer).CheckResponse();
                        const response = await searchCustomer.json();
                        expect(response.content).not.toBeEmpty;
                        const customerString = response.content?.[0]?.customer;
                        expect(customerString).toBeTruthy();
                        const returnedIdentifier = customerString.split(/[ (]/)[0];;
                        expect(returnedIdentifier).toEqual(customerIdentifier);
                    });

                    await test.step('search with All', async() => {
                        const customerData = await Request.get(`/customer/${Responses.customer[0].id}`);
                        const customerIdentifier = (await customerData.json()).identifier;
                        
                        const searchAll = await Request.get(`/manual-liability-offsetting/list?page=0&size=25&sortBy=ID&direction=DESC&prompt=${customerIdentifier}&searchBy=ALL`);
                        await expect(searchAll).CheckResponse();
                        const response = await searchAll.json();
                        expect(response.content).not.toBeEmpty;
                        const customerString = response.content?.[0]?.customer;
                        expect(customerString).toBeTruthy();
                        const returnedIdentifier = customerString.split(/[ (]/)[0];;
                        expect(returnedIdentifier).toEqual(customerIdentifier);
                    });
                   });
                });
                test.describe('[REG-940]: Filters and filters with search', () => {
                    test('[REG-940]: Add filters and search', async({Request, GeneratePayload, Responses}) => {
                        await test.step('Create customer and get customer data', async() => {
                            const customerPost = await Request.post("/customer", {data: GeneratePayload.customers.customer_legal()});
                            await expect(customerPost).CheckResponse();
                            Responses.customer.push(await customerPost.json());
                        });

                        await test.step('create liability', async() => {
                            const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability()
                            liabilityPayload.initialAmount = 100;
                            const liabilityPost = await Request.post('/customer-liability', {data: liabilityPayload});
                            await expect(liabilityPost).CheckResponse();
                            Responses.customerLiability.push(await liabilityPost.json());
                        });

                        await test.step('create recievable', async() => {
                            const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                            receivablePayload.initialAmount = 100;
                            const receivablePost = await Request.post(`/customer-receivable`, {data: receivablePayload});
                            await expect(receivablePost).CheckResponse();
                            Responses.customerReceivable.push(await receivablePost.json());
                        });

                        await test.step('create MLO with rec and liab', async() => {
                            const MLOCreate = await Request.post(`/manual-liability-offsetting`, {data: await GeneratePayload.receivablesManagement.MLO_L_R()});
                            await expect(MLOCreate).CheckResponse();
                            Responses.manualLiabilityOffsetting.push(await MLOCreate.json());
                        });

                        await test.step('date filter', async() => {
                            const todaysDate = randomGens.generateTodaysDate('yyyy-mm-dd');
                            const filterRequest = await Request.get(`/manual-liability-offsetting/list?page=0&size=25&sortBy=ID&direction=DESC&searchBy=ALL&fromDate=${todaysDate}&toDate=${todaysDate}`);
                            await expect(filterRequest).CheckResponse();
                            const responseBody = await filterRequest.json();
                            const firstObjectDate = responseBody.content[0].manualLiabilityDate;
                            expect(firstObjectDate).toEqual(todaysDate);
                        });

                        await test.step('reverse filter', async() => {
                            const request = await Request.get(`/manual-liability-offsetting/list?page=0&size=25&sortBy=ID&direction=DESC&searchBy=ALL&reversed=YES`);
                            await expect(request).CheckResponse();
                            const response = await request.json();
                            const firstObjectsValue = response.content[0].reversed
                            expect(firstObjectsValue).toBe('YES');
                        });

                        await test.step('with both filters and search', async() => {
                            const todaysDate = randomGens.generateTodaysDate('yyyy-mm-dd');
                            const customerData = await Request.get(`/customer/${Responses.customer[0].id}`);
                            const customerIdentifier = (await customerData.json()).identifier;
                            
                            const searchRequest = await Request.get(`/manual-liability-offsetting/list?page=0&size=25&sortBy=ID&direction=DESC&${customerIdentifier}&searchBy=ALL&fromDate=${todaysDate}&toDate=${todaysDate}&reversed=NO`);
                            await expect(searchRequest).CheckResponse();
                            const data = await searchRequest.json();
                            const customerString = data.content?.[0]?.customer;
                            const returnedIdentifier = customerString.split(/[ (]/)[0];
                            expect(returnedIdentifier).toEqual(customerIdentifier);
                            expect(data.content[0].reversed).toBe('NO')
                            expect(data.content[0].manualLiabilityDate).toEqual(todaysDate);
                        });
                    });
                });                
            });
    });
});