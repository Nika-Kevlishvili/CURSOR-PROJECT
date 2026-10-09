import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-56]: Receivables Management - Legacy Entry Point', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-127]: Mass operation for blocking', () => {
        test.describe('[REG-905]: Create - Mass operation for blocking', () => {
            test('[REG-905]', async ({Request, GeneratePayload, Nomenclatures, Responses, Endpoints}) => {

                await test.step('create customer', async () => {
                    const customer_legal = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    const customer_legal_json = await customer_legal.json();
                    
                    await expect(customer_legal).CheckResponse();
                    Responses.customer.push(customer_legal_json);
                })

                await test.step('create Mass operation for blocking', async () => {
                    const mofbPayload = GeneratePayload.receivablesManagement.mass_operation_for_blocking();
                    mofbPayload.blockingForCalculation = null
                    mofbPayload.blockingForPayment = null
                    mofbPayload.blockingForReminderLetters = null
                    mofbPayload.blockingForLiabilitiesOffsetting = null
                    mofbPayload.blockingForSupplyTermination = null

                    const mass_operation_for_blocking = await Request.post(Endpoints.massOperationForBlocking, {data: mofbPayload});
                    await expect(mass_operation_for_blocking).CheckResponse();
                    Responses.massOperationForBlocking.push(await mass_operation_for_blocking.json());
                })

                test.info().attach('[REG-905] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});