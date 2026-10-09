import { resolve } from 'path';
import { test, expect } from '../../../backend/fixtures/baseFixture';

test.describe('[REG-56]: Receivables Management - Deposits', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-113]: Deposits', () => {
        test.describe.serial('[REG-520]: Create', () => {
            test.describe('[REG-724]: deposit with minimal data', () => {
                test('[REG-724]: should create a deposit with minimal data', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                    await test.step('create customer', async() => { 
                        const payload =  GeneratePayload.customers.customer_legal();                  
                        const customer = await Request.post("/customer", {data: payload});

                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    })
                    await test.step('create deposit with minimal data', async() => {
                        const depositPayload = GeneratePayload.receivablesManagement.deposit()
                        const deposit = await Request.post("/deposit", {data: depositPayload});
                        await expect(deposit).CheckResponse();
                    })
                    
                });
            });
            test.describe('[REG-723]: deposit with maximal data', () => {
                test('[REG-723]: should create deposit with maximal data', async({Request, GeneratePayload, Responses, Endpoints}) => {
                    await test.step('create customer', async() => {
                        const payload = GeneratePayload.customers.customer_legal();
                        const customer = await Request.post("/customer", {data: payload});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });
                    
                    await test.step('create deposit with maximal data', async() => {
                        const depositPayload = GeneratePayload.receivablesManagement.deposit_max();
                        depositPayload.customerId = Responses.customer[0].id;
                        const deposit_max = await Request.post("/deposit", {data: depositPayload});
                        await expect(deposit_max).CheckResponse();
                    });
                });
            });
        });
    });
});