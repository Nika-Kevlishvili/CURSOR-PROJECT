import { test, expect } from '../../../backend/fixtures/reminder.fixtures';
import reportGenerator from '../../../backend/utils/generateReport';

test.describe('[REG-56]: Receivables Management', {tag: '@receivableManagement'}, () => {
    test.describe('[REG-118]: Reminder', () => {
        test.describe('[REG-882]: Reminder - Create', () => {
            test('[REG-1357]: Create reminder', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                await test.step('create process periodicity', async () => {
                    const process_periodicity = await Request.post(Endpoints.processPeriodicity, {data: await GeneratePayload.operationsManagement.process_periodicity()});
                    const process_periodicity_json = await process_periodicity.json();

                    await expect(process_periodicity).CheckResponse();
                    Responses.processPeriodicity.push(process_periodicity_json);
                });

                await test.step('create customer', async () => {
                    const customer_legal = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    const customer_legal_json = await customer_legal.json();

                    await expect(customer_legal).CheckResponse();
                    Responses.customer.push(customer_legal_json);
                });

                await test.step('create reminder', async () => {
                    const reminder = await Request.post(Endpoints.reminder, {data: GeneratePayload.receivablesManagement.reminder()});
                    await expect(reminder).CheckResponse();
                    Responses.reminder.push(await reminder.json());
                });

                test.info().attach('[REG-882] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});