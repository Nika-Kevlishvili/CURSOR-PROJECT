import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

test.describe('[REG-56]: Receivables Management - Payment Package', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-116]: Payment package', () => {
        test.describe('[REG-480]: Create', () => {
            test('[REG-480]', async ({ Request, GeneratePayload, Responses, Endpoints}) => {
                await test.step('create collection channel', async () => {
                    const collection_channel = await Request.post(Endpoints.collectionChannel, { data: GeneratePayload.receivablesManagement.collection_channel() });
                    await expect(collection_channel).CheckResponse();
                    Responses.collectionChannel.push(await collection_channel.json());
                });
                
                await test.step('create payment package', async () => {
                    const payment_package = await Request.post(Endpoints.paymentPackage, { data: GeneratePayload.receivablesManagement.payment_package()});
                    await expect(payment_package).CheckResponse();
                    Responses.paymentPackage.push(await payment_package.json());
                });

                test.info().attach('[REG-480] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});