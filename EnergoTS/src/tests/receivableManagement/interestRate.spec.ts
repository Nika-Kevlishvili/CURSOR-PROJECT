import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

test.describe('[REG-56]: Receivables Management - Interest Rate', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-124]: Interest rate', () => {
        test.describe('[REG-877]: Interest rates - Create', () => {
            test('[REG-1355]: Create interest rate', async ({Request, GeneratePayload, Endpoints, Responses}) => {
                await test.step('create Interest rate', async () => {
                    const interest_rate = await Request.post(Endpoints.interestRate, {data: GeneratePayload.receivablesManagement.interest_rate()});
                    await expect(interest_rate).CheckResponse();
                    Responses.interestRate.push(await interest_rate.json());
                })

                test.info().attach('[REG-877] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses({Responses}), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});