import {test, expect} from '../../backend/fixtures/baseFixture';


test.describe('customercoordinates', {tag: ['@customer', '@dev2']}, () => {
    test('[REG-1313]: Get customers by coordinates', async ({Request, GeneratePayload, Responses, Endpoints, SPRequest}) => {
        test.setTimeout(120_000);
        const customerPayload = GeneratePayload.customers.customer_legal();
        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: customerPayload});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
            console.log('Created customer with ID:', Responses.customer[0].id);
        });

        await test.step('validate sales portal response', async () => {
            const payload = GeneratePayload.salesPortal.customerCoordinates();

            payload.latitude = customerPayload.communicationData[0].address.latitude;
            payload.longitude = customerPayload.communicationData[0].address.longitude;

            const spResponse = await SPRequest.post(Endpoints.salesPortalEndpoints.podCustomerListByCoordinates, {data: payload});
            const spResponseBody = await spResponse.json();
            await expect(spResponse).CheckResponse();
            console.log('Sales Portal response:', spResponseBody);
            expect(spResponseBody.content.length).toBeGreaterThan(0);

            const createdCustomerId = Responses.customer[0].id;
            const found = spResponseBody.content.find((c: any) => c.customerId === createdCustomerId);
            expect(found).toBeTruthy();

            for (const customer of spResponseBody.content) {
                expect(customer.customerId).toBeTruthy();
                expect(customer.customerName).toBeTruthy();
                expect(customer.type).toBeTruthy();
                expect(customer.latitude).not.toBeUndefined();
                expect(customer.longitude).not.toBeUndefined();
            }
        });
    });
});