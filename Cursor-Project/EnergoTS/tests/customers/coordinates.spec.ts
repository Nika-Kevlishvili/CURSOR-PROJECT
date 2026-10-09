import {test, expect} from '../../fixtures/baseFixture';
import { request } from '@playwright/test';
import reportGenerator from '../../utils/generateReport';
import { randomGens } from '../../utils/randomGens';

test.describe('customercoordinates', {tag: ['@customer', '@dev2']}, () => {
    test('[REG-XXX]: ', async ({Request, GeneratePayload, Responses, Endpoints, SPRequest}) => {
        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
            console.log('Created customer with ID:', Responses.customer[0].id);
        });

        await test.step('validate sales portal response', async () => {
            const payload = GeneratePayload.salesPortal.customerCoordinates();
            const spResponse = await SPRequest.post(Endpoints.salesPortalEndpoints.podCustomerListByCoordinates, {data: payload});
            const spResponseBody = await spResponse.json();
            await expect(spResponse).CheckResponse();

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