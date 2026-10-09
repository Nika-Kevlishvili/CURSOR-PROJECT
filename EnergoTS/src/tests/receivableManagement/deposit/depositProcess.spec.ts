
import { test, expect } from '../../../backend/fixtures/baseFixture';
import { envVariables } from '../../../backend/fixtures/envCashed';
import { randomGens } from '../../../backend/utils/randomGens';

test.describe('[REG-56]: Receivables Management - Deposits', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-113]: Deposits', () => {
        test.describe.serial('[REG-523]: Process ', () => {
            test.describe('[REG-731]: Automatic Liability creation', () => {
                test('[REG-731]: creating liability while deposit creation', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                    await test.step('create customer', async() => {
                        const payload = GeneratePayload.customers.customer_legal();
                        const customer = await Request.post("/customer", {data: payload});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    })
                    
                    await test.step('create deposit', async() => {
                        const depositPayload = GeneratePayload.receivablesManagement.deposit();
                        depositPayload.initialAmount = 100;
                        const deposit = await Request.post("/deposit", {data: depositPayload});
                        await expect(deposit).CheckResponse();
                        Responses.deposit.push(await deposit.json());
                    })
                    
                    await test.step('check if liability is created', async() => {
                        const deposit_liability = await Request.get(`/customer-liability/list?page=0&size=1&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                        await expect(deposit_liability).CheckResponse();
                        const depositLiabilityData = await deposit_liability.json();
                        expect(depositLiabilityData.content.length).toBeGreaterThan(0);
                        expect(depositLiabilityData.content[0].initialAmount).toBe(100);
                    })
                })
            });

            test('[REG-1350]: Deposit offset against a liability creates another liability of 100', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                    await test.step('Create customer', async() => {
                        const payload = GeneratePayload.customers.customer_legal();
                        const customer = await Request.post("/customer", {data: payload});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });
                    
                    await test.step('create deposit', async() =>{
                        const depositPayload = GeneratePayload.receivablesManagement.deposit();
                        depositPayload.initialAmount = 100;
                        const deposit = await Request.post("/deposit", {data: depositPayload});
                        await expect(deposit).CheckResponse();
                        Responses.deposit.push(await deposit.json());
                    });
                    
                    await test.step('create receivable', async() => {
                        const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                        receivablePayload.initialAmount = 100;
                        const receivable = await Request.post("/customer-receivable", {data: receivablePayload});
                        await expect(receivable).CheckResponse();
                        Responses.customerReceivable.push(await receivable.json());
                    });
                    
                    await test.step('Get deposit liability', async() => {
                        const deposit_liability = await Request.get(`/customer-liability/list?page=0&size=1&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                        await expect(deposit_liability).CheckResponse();
                        Responses.customerLiability.push((await deposit_liability.json()).content[0]);
                    });
                    
                    await test.step('offset dep liability and receivable', async() => {
                        const offset_payload = await GeneratePayload.receivablesManagement.MLO_L_R();
                        const offset = await Request.post(Endpoints.manualLiabilityOffsetting, {data: offset_payload});
                        await expect(offset).CheckResponse();
                        Responses.manualLiabilityOffsetting.push(await offset.json());
                    });
                    
                    await test.step('start job process', async() =>{
                        const job = await Request.post("/deposit/job")
                        await expect(job).CheckResponse();
                    });
                    
                    await test.step('create liability', async() =>{
                        const liabilityPayload = GeneratePayload.receivablesManagement.customer_liability();
                        liabilityPayload.initialAmount = 100;
                        liabilityPayload.currencyId = Responses.customerLiability[0].currencyId;
                        liabilityPayload.accountingPeriodId = envVariables.accounting_period;
                        liabilityPayload.dueDate = randomGens.generateTodaysDate("dd-mm-yyyy");
                        liabilityPayload.occurrenceDate = randomGens.generateTodaysDate("dd-mm-yyyy");
                        const liability_creation = await Request.post('/customer-liability', {data: liabilityPayload});
                        await expect(liability_creation).CheckResponse();
                        Responses.customerLiability.push(await liability_creation.json());
                    });
                    
                    await test.step('offset deposit and liability in each other', async() => {
                        const offset_payload = await GeneratePayload.receivablesManagement.MLO_D_L();
                        offset_payload.date = randomGens.generateUtcDate('yyyy-mm-dd');
                        const offset = await Request.post(Endpoints.manualLiabilityOffsetting, {data: offset_payload});
                        await expect(offset).CheckResponse(); 
                    });
                    
                    await test.step('check if liability was created', async() => {
                        const new_deposit_liability = await Request.get(`/customer-liability/list?page=0&size=25&columns=ID&direction=DESC&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                        const Liability_list = await new_deposit_liability.json();
                        expect(Liability_list.content.length).toBeGreaterThan(2);
                        expect(Liability_list.content[0].currentAmount).toBe(100);
                    });
                });

            test.describe('[REG-733]: Deposit Refund', () => {
                test('[REG-733]: refund newly created deposit, receivable creation', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                    await test.step('Create customer', async() => {
                        const payload = GeneratePayload.customers.customer_legal();
                        const customer = await Request.post("/customer", {data: payload});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });
                    
                    await test.step('create deposit', async() => {
                        const depositPayload = GeneratePayload.receivablesManagement.deposit();
                        const deposit = await Request.post("/deposit", {data: depositPayload});
                        await expect(deposit).CheckResponse();
                        Responses.deposit.push(await deposit.json());
                    });
                    
                    await test.step('refund deposit', async() => {
                        const refundPayload = GeneratePayload.receivablesManagement.deposit();
                        refundPayload.currentAmount = 0;
                        refundPayload.refundDate = randomGens.generateTodaysDate('dd-mm-yyyy');
                        const deposit_refund = await Request.put(`/deposit/${Responses.deposit[0]}`, {data: refundPayload});
                        await expect(deposit_refund).CheckResponse();
                    });
                    
                    await test.step('Now we should check if refund process has created a receivable', async() => {
                        const deposit_receivable = await Request.get(`/customer-receivable?page=0&size=25&direction=DESC&sortBy=ID&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                        await expect(deposit_receivable).CheckResponse();
                        const depositReceivableData = await deposit_receivable.json();
                        expect(depositReceivableData.content.length).toBeGreaterThan(0);
                        
                        const depositGet = await Request.get(`/deposit/${Responses.deposit[0]}`);
                        const depositData = await depositGet.json();
                        expect(depositReceivableData.content[0].initialAmount).toBe(depositData.initialAmount);
                    });
                });
            });
            test.describe('[REG-734]: Deposit delete process', () => {
                test('[REG-734]:  delete nwelycreated deposit', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                    await test.step('Create customer', async() => {
                        const payload = GeneratePayload.customers.customer_legal();
                        const customer = await Request.post("/customer", {data: payload});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });
                    
                    await test.step('create deposit', async() =>{
                        const depositPayload = GeneratePayload.receivablesManagement.deposit();
                        const deposit = await Request.post("/deposit", {data: depositPayload});
                        await expect(deposit).CheckResponse();
                        Responses.deposit.push(await deposit.json());
                    });
                    
                    await test.step('delete deposit', async() => {
                        const deposit_delete = await Request.delete(`/deposit/${Responses.deposit[0]}`);
                        await expect(deposit_delete).CheckResponse();
                    });
                    
                    await test.step('check if liability is deleted as well', async() => {
                        const deposit_liability = await Request.get(`/customer-liability/list?page=0&size=25&direction=DESC&sortBy=ID&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                        await expect(deposit_liability).CheckResponse();
                        const depositLiabilityData = await deposit_liability.json();
                        expect(depositLiabilityData.content.length).toBeGreaterThan(0);
                        const depositLiabilityId = depositLiabilityData.content[0].id;
                        const liabilityGet = await Request.get(`/customer-liability/${depositLiabilityId}`);
                        await expect(liabilityGet).CheckResponse();
                        const status = (await liabilityGet.json()).status;
                        expect(status).toBe("DELETED");
                    });
                });
            });

            test.describe('[REG-732]: Deposit job process', () => {
                test('[REG-732]: job process amount check', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                    await test.step('create customer', async() => {
                        const payload = GeneratePayload.customers.customer_legal();
                        const customer = await Request.post("/customer", {data: payload});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });
                    
                    await test.step('Create deposit', async() => {
                        const depositPayload = GeneratePayload.receivablesManagement.deposit();
                        depositPayload.initialAmount = 100;
                        const deposit = await Request.post("/deposit", {data: depositPayload});
                        await expect(deposit).CheckResponse();
                        Responses.deposit.push(await deposit.json());
                    });
                    
                    await test.step('create receivable', async() => {
                        const receivablePayload = GeneratePayload.receivablesManagement.customer_receivable();
                        receivablePayload.initialAmount = 100;
                        const receivable = await Request.post("/customer-receivable", {data: receivablePayload});
                        await expect(receivable).CheckResponse();
                        Responses.customerReceivable.push(await receivable.json());
                    });
                    
                    await test.step('get deposit liability', async() => {
                        const deposit_liability = await Request.get(`/customer-liability/list?page=0&size=1&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                        await expect(deposit_liability).CheckResponse();
                        Responses.customerLiability.push((await deposit_liability.json()).content[0]);
                    });
                    
                    await test.step('offset receivable and deposit liability', async() => {
                        const offset_payload = await GeneratePayload.receivablesManagement.MLO_L_R();
                        const offset = await Request.post(Endpoints.manualLiabilityOffsetting, {data: offset_payload});
                        await expect(offset).CheckResponse();
                    });
                    
                    await test.step('Start job and check if deposit is updated', async() => {
                        const job = await Request.post("/deposit/job")
                        await expect(job).CheckResponse();
                        const get_deposit = await Request.get(`/deposit/${Responses.deposit[0]}`);
                        await expect(get_deposit).CheckResponse();
                        const depositData = await get_deposit.json();
                        expect(depositData.currentAmount).toBe(100);
                    });
                });
            });
        });
    });
});