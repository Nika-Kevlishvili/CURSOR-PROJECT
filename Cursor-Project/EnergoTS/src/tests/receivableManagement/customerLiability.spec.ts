import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

test.describe('[REG-56]: Receivables Management - Customer Liability', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-111]: Customer liability', () => {
        test.describe('[REG-477]: Create', () => {
            //this case does not exist in Jira anymore, this should be changed or deleted
            test('[REG-827]', async ({ Request, GeneratePayload, Responses, Endpoints}) => {                
                await test.step('create customer', async () => {
                    const customer_legal = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
                    const customerData = await customer_legal.json();

                    await expect(customer_legal).CheckResponse();
                    Responses.customer.push(customerData);
                });

                await test.step('create liability', async () => {
                    const liability = await Request.post(Endpoints.customerLiability, { data: GeneratePayload.receivablesManagement.customer_liability() });
                    await expect(liability).CheckResponse();
                    Responses.customerLiability.push(await liability.json());
                });

                test.info().attach('[REG-827] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});