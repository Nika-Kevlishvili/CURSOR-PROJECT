import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

test.describe('[REG-1306]: Get Product List by POD ID', { tag: '@salesPortal' }, () => {
    test.describe.configure({ timeout: 900000 });
    // standard product (fixed parameters)
    const standardProductId = 12533

    //pod present in system - ❌
    //customer id provided - ❌
    //customer present in the system - ❌
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //result - return standard products
    test('[REG-1314]: case 1', async ({Endpoints, SPRequest }) => {
        await test.step('GET product list by POD ID - should return the standard product', async () => {
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=32Xსდფსრგერგგ`);
            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(standardProductId);
        });

    });

    //pod present in system - ✅
    //customer id provided - ❌
    //customer present in the system - ❌
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //result - return standard products and pod specific products
    test('[REG-1315]: case 2 (check response)', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        test.setTimeout(6000000);

        await test.step('Create term', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json())
        });

        await test.step('Create POD', async () => {
          const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
          const podSettlement = await Request.post(Endpoints.pod, {data: payload});
          await expect(podSettlement).CheckResponse();
          Responses.pod.push(await podSettlement.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.globalSegment = true
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json())
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list by POD ID - should return the standard product', async () => {
            const getPodIdentifier  = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);

            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            const foundStandardProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);

            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            console.log('Found standard product object:', JSON.stringify(foundStandardProduct, null, 2));

            expect(productIds).toContain(Responses.product[0]);
            expect(productIds).toContain(standardProductId);
        });
    });

    //pod present in system - ✅
    //customer id provided - ✅
    //customer present in the system - ❌
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //result - return standard products and pod specific products
    test('[REG-1316]: case 3', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create term', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json())
        });

        await test.step('Create POD', async () => {
          const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
          const podSettlement = await Request.post(Endpoints.pod, {data: payload});
          await expect(podSettlement).CheckResponse();
          Responses.pod.push(await podSettlement.json());
        });

        await test.step('Create product', async () => {
          const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
          await expect(product).CheckResponse();
          Responses.product.push(await product.json())
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list by POD ID - should return the standard product', async () => {
            const getPodIdentifier  = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);

            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=7867543342IJNNNJ`);
            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            const foundStandardProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            console.log('Found standard product object:', JSON.stringify(foundStandardProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
            expect(productIds).toContain(standardProductId);
        });
    });

  })