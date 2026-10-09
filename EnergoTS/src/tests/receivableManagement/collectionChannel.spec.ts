import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

test.describe('[REG-56]: Receivables Management - Collection Channel', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-117]: Collection channel', () => {
        test.describe('[REG-870]: Create', () => {
            //this case does not exist in Jira anymore, this should be changed or deleted
            test('[REG-1352]: Create collection channel', async ({ Request, GeneratePayload, Endpoints, Responses }) => {
                await test.step('create collection channel', async () => {
                    const collection_channel = await Request.post(Endpoints.collectionChannel, { data: GeneratePayload.receivablesManagement.collection_channel() });
                    await expect(collection_channel).CheckResponse();
                    Responses.collectionChannel.push(await collection_channel.json());
                });

                test.info().attach('[REG-870] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses({Responses}), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});