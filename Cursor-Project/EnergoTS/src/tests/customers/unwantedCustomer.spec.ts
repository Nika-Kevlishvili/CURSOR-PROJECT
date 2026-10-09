import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

test.describe('[REG-1]: Customer', { tag: '@customer' }, () => {
  test.describe('[REG-28]: Unwanted Customer', () => {
    test('[REG-28]: Create - Unwanted Customer', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
      // Step 1: Create legal customer
      await test.step('Create legal customer', async () => {
        const payload = GeneratePayload.customers.customer_legal();
        const response = await Request.post(Endpoints.customer, { data: payload });
        await expect(response).CheckResponse();
        Responses.customer.push(await response.json());
      });

      // Step 2: Create unwanted customer using the already created legal customer
      await test.step('Create unwanted customer', async () => {
        const legalCustomer = Responses.customer[0];
        const payload = GeneratePayload.customers.unwanted_customer();
        // Map identificationNumber from legal customer if needed
        if (legalCustomer && legalCustomer.identifier) {
          payload.identificationNumber = legalCustomer.identifier;
        }
        const response = await Request.post(Endpoints.unwantedCustomer, { data: payload });
        await expect(response).CheckResponse();
        Responses.unwantedCustomer = Responses.unwantedCustomer || [];
        Responses.unwantedCustomer.push(await response.json());
      });

      // Step 3: Check if unwanted customer object was created successfully
      await test.step('Check if unwanted customer was created successfully', async () => {
        const unwantedCustomerId = Responses.unwantedCustomer[0]?.id;
        const response = await Request.get(`${Endpoints.unwantedCustomer}/${unwantedCustomerId}`);
        await expect(response).CheckResponse();
        const body = await response.json();
        expect(body).toBeDefined();
        expect(body.id).toBe(unwantedCustomerId);
      });

      test.info().attach('[REG-28] response', {
        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
        contentType: 'application/json'
      });
    });
  });
});
