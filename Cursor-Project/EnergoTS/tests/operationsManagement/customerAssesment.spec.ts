import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-57]: Operations Management', {tag: '@operationsManagment'}, () => {
    test.describe('[REG-128]: Customer assessment', () => {
        test.describe('[REG-915]: Create - Customer assessment', {tag: '@customer'}, () => {
            test('[REG-915]', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                await test.step('create Customer', async () => {
                    const customer_legal = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    const customer_legal_json = await customer_legal.json();

                    await expect(customer_legal).CheckResponse();
                    Responses.customer.push(customer_legal_json);
                })

                await test.step('create Customer assessment', async () => {
                    const customer_assessment = await Request.post(Endpoints.customerAssessment, {data: GeneratePayload.receivablesManagement.customer_assessment()});
                    await expect(customer_assessment).CheckResponse();
                    Responses.customerAssessment.push(await customer_assessment.json());
                })

                test.info().attach('[REG-915] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
})