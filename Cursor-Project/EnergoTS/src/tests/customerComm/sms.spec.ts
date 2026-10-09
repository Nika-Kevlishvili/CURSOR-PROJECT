import { test, expect } from "../../backend/fixtures/baseFixture";
import reportGenerator from '../../backend/utils/generateReport';

test.describe('[REG-51]: Customer Communication', {tag: '@crm'},() => {
    test.describe('[REG-80]: SMS', () => {
        test.describe('[REG-939]: Create - SMS', () => { 
            test('[REG-985]: SMS creating',async ({Request, GeneratePayload, Endpoints, Responses}) => {
            let customerId: any;
            let sms: any;
            let customerGet: any;

            await test.step('generate customer', async () => {
                const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                await expect(customer).CheckResponse();

                customerId = (await customer.json()).lastCustomerDetailId;
                const customerGetJson = await Request.get(`/customer/${(await customer.json()).id}`);
                customerGet = ((await customerGetJson.json()).communicationData[0].id);
            })

            await test.step('generate sms', async () => {
                sms = GeneratePayload.customerCommunication.sms();
                sms.customerDetailId = customerId;
                sms.customerCommunicationId = customerGet;
                const send_sms = await Request.post(Endpoints.sms, {data: sms});

                await expect(send_sms).CheckResponse();
                Responses.sms.push(await send_sms.json());
            })

            test.info().attach('[REG-939] response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses({Responses}), null, 2),
                contentType: 'application/json'
            });
        })
    })
    })
})