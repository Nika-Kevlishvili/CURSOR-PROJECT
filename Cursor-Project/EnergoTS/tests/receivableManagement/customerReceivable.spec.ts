import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-56]: Receivables Management - Customer Receivable', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-112]: Customer receivable', () => {
        test.describe('[REG-828]: Create', () => {
            //this case does not exist in Jira anymore, this should be changed or deleted
            test('[REG-869]', async ({ Request, GeneratePayload, Responses, Endpoints }) => {                
                await test.step('create customer', async () => {
                    const customer_legal = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
                    const customerData = await customer_legal.json();

                    await expect(customer_legal).CheckResponse();
                    Responses.customer.push(customerData);
                });

                await test.step('create receivable', async () => {
                    const receivable = await Request.post(Endpoints.customerReceivable, { data: GeneratePayload.receivablesManagement.customer_receivable() });
                    await expect(receivable).CheckResponse();
                    Responses.customerReceivable.push(await receivable.json());
                });

                test.info().attach('[REG-869] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});