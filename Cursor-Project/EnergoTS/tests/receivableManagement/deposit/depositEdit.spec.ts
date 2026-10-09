import { test, expect } from '../../../fixtures/baseFixture';
import { randomGens } from '../../../utils/randomGens';
import { envVariables } from '../../../fixtures/envCashed';

test.describe('[REG-56]: Receivables Management - Deposits', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-113]: Deposits', () => {
        test.describe('[REG-521]: Edit', () => {
            test.describe('[REG-725]: Deposits Edit - Newly created deposit', () => {
                test('[REG-725]: Edit fields of newly created deposit', async ({Request, GeneratePayload, Responses, Endpoints}) =>{

                    await test.step('create customer', async() => {
                        const payload = GeneratePayload.customers.customer_legal();
                        const customer = await Request.post("/customer", {data: payload});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    })
                    await test.step('create deposit', async() => {
                        const payload = GeneratePayload.receivablesManagement.deposit();
                        const deposit = await Request.post("/deposit", {data: payload});

                        await expect(deposit).CheckResponse();
                        Responses.deposit.push(await deposit.json());
                    })

                    await test.step('edit deposit fields that are editable', async() => {
                        const payload = GeneratePayload.receivablesManagement.deposit();

                        payload.currentAmount = 0;
                        payload.paymentDeadlineAfterWithdrawalRequest = {
                            value: 30,
                            calendarId: envVariables.calendar.id,
                            calendarType: "CALENDAR_DAYS",
                            excludeWeekends: false,
                            excludeHolidays: false,
                            dueDateChange: null,
                            name: "30 Calendar Days ( )"
                        }
                        payload.numberOfIncomeAccount = "123ggg";
                        payload.costCentre = "123ggg";
                        payload.templateIds = [
                            { templateId: envVariables.deposit_email_template, templateType: "EMAIL" }
                        ];
                        const edit_positive = await Request.put(`/deposit/${Responses.deposit[0]}`, {data: payload});  
                        await expect(edit_positive).CheckResponse();

                    })
                    await test.step('edit uneditable fields', async() => {
                        const depositId = Responses.deposit[0];

                        const A = await Request.put(`/deposit/${depositId}`, {data: {...GeneratePayload.receivablesManagement.deposit(), id: depositId, currentAmount: 123}});
                        expect(A.ok()).toBeFalsy();
                        const F = await Request.put(`/deposit/${depositId}`, {data: {...GeneratePayload.receivablesManagement.deposit(), id: depositId, currentAmountInOtherCurrency: 123}});
                        expect(F.ok()).toBeFalsy();
                        const B = await Request.put(`/deposit/${depositId}`, {data: {...GeneratePayload.receivablesManagement.deposit(), id: depositId, initialAmount: 123}});
                        expect(B.ok()).toBeFalsy();
                        const G = await Request.put(`/deposit/${depositId}`, {data: {...GeneratePayload.receivablesManagement.deposit(), id: depositId, initialAmountInOtherCurrency: 123}});
                        expect(G.ok()).toBeFalsy();
                        const C = await Request.put(`/deposit/${depositId}`, {data: {...GeneratePayload.receivablesManagement.deposit(), id: depositId, customerId: 6000038}});
                        expect(C.ok()).toBeFalsy();
                        const D = await Request.put(`/deposit/${depositId}`, {data: {...GeneratePayload.receivablesManagement.deposit(), id: depositId, depositNumber: "123"}});
                        expect(D.ok()).toBeFalsy();
                        const H = await Request.put(`/deposit/${depositId}`, {data: {...GeneratePayload.receivablesManagement.deposit(), id: depositId, currencyId: 1002}});
                        expect(H.ok()).toBeFalsy();
                    })
                })
            })
            test.describe('[REG-726]: Deposits Edit - Refunded deposit', () => {
                test('[REG-726]: Edit refunded deposit', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                    await test.step('create customer', async() => {
                        const payload =  GeneratePayload.customers.customer_legal();

                        const customer = await Request.post("/customer", {data: payload});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    })

                    await test.step('create deposit', async() => {
                        const payload = GeneratePayload.receivablesManagement.deposit();
                        const deposit = await Request.post("/deposit", {data: payload});

                        await expect(deposit).CheckResponse();
                        Responses.deposit.push(await deposit.json());
                    })
                    await test.step('refund deposit', async() => {
                        const payload = GeneratePayload.receivablesManagement.deposit();
                        payload.currentAmount = 0;
                        payload.refundDate = randomGens.generateTodaysDate('dd-mm-yyyy');
                        const deposit_refund = await Request.put(`/deposit/${Responses.deposit[0]}`, {data: payload});
                        await expect(deposit_refund).CheckResponse();
                    })
                    await test.step('check if refunded dep is editable', async() => {
                        const edit_refunded = await Request.put(`/deposit/${Responses.deposit[0]}`);
                        expect(edit_refunded.ok()).toBeFalsy();
                    })
                })
            })
            test.describe('[REG-727]: Deposits Edit - When object took part in offsetting process', () => {
                test('[REG-727]: edit deposit that took part in offsetting process', async ({Request, GeneratePayload, Responses, Endpoints}) => {                        
                    await test.step('create customer', async() => {
                        const payload =  GeneratePayload.customers.customer_legal();
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

                    await test.step('create receivable', async() => {
                        const payload = GeneratePayload.receivablesManagement.customer_receivable();
                        payload.initialAmount = 100;
                        const receivable = await Request.post("/customer-receivable", {data: payload});

                        await expect(receivable).CheckResponse();
                        Responses.customerReceivable.push(await receivable.json());
                    })

                    await test.step('Get deposit liability', async() => {
                        const deposit_liability = await Request.get(`/customer-liability/list?page=0&size=1&prompt=${Responses.customer[0].identifier}&searchFields=CUSTOMER`);
                        await expect(deposit_liability).CheckResponse();

                        Responses.customerLiability.push((await deposit_liability.json()).content[0])
                    })

                    await test.step('built offset payload and offset objects', async() => {
                        const offset_payload = await GeneratePayload.receivablesManagement.MLO_L_R();
                        const offset = await Request.post(Endpoints.manualLiabilityOffsetting, {data: offset_payload});
                        await expect(offset).CheckResponse();
                        Responses.manualLiabilityOffsetting.push(await offset.json());
                    })

                    await test.step('Now lets check if fields are editable', async() => {
                        const data = GeneratePayload.receivablesManagement.deposit();
                        data.customerId = Responses.customer[0].id;
                        data.paymentDeadlineAfterWithdrawalRequest = {value: 2, calendarId: envVariables.calendar.id, calendarType: "CALENDAR_DAYS", excludeHolidays: false, excludeWeekends: false, dueDateChange: null, name: "2 Calendar Days ( )"};
                        data.currentAmount = 0;
                        data.initialAmount = 100;
                        data.numberOfIncomeAccount = "dd";
                        data.templateIds = [{templateId: envVariables.deposit_email_template, templateType: "EMAIL"}];
                        data.paymentDeadline = randomGens.generateTodaysDate('dd-mm-yyyy');
                        data.refundDate = null
                        const edit_positive_offseted = Request.put(`/deposit/${Responses.deposit[0]}`, {data: data});
                        expect(edit_positive_offseted).toBeTruthy()
                    })  
                })
            })        
        })
    })
});