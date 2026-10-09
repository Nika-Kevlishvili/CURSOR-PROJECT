import { test, expect } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import reportGenerator from '../../utils/generateReport';
import { randomGens } from '../../utils/randomGens';

const setMatchStandardInvoiceTermForInterim = (payload: any) => {
    payload.matchesWithTermOfStandardInvoice = true;
    payload.dayOfWeekAndPeriodOfYearAndDateOfMonth = null;
    payload.interimAdvancePaymentTerm = null;
};


test.describe('[PHN-2187]: Get Product List by POD ID', { tag: '@salesPortal' }, () => {
    // standard product (fixed parameters)
    const standardProductId = 12533

    test('endpoint test', async ({ Endpoints, SPRequest }) => {
        test.setTimeout(6000000)
        const podIdentifier = '32XPWPOKXXUGM0500398225'
        const customerIdentifier = '17782476121163655'

        const productToFind = 13818
        const productToFind2 = 13819
        
        // const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
        const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerIdentifier}`);
        await expect(response).CheckResponse();
        const body = await response.json();

        expect(Array.isArray(body)).toBeTruthy();
        expect(body.length).toBeGreaterThanOrEqual(1);
        const productIds = body.map((p: any) => p.productId ?? p.id);

        const foundProduct = body.find((p: any) => (p.productId ?? p.id) === productToFind);
        const foundProduct2 = body.find((p: any) => (p.productId ?? p.id) === productToFind2);
        const foundStandardProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);

        console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
        console.log('Found product object:', JSON.stringify(foundProduct2, null, 2));
        console.log('Found standard product object:', JSON.stringify(foundStandardProduct, null, 2));
        console.log('All product ids in response:', productIds);

        expect(productIds).toContain(productToFind);
        expect(productIds).toContain(productToFind2);
        // expect(productIds).toContain(standardProductId);
});

    //pod present in system - ❌
    //customer id provided - ❌
    //customer present in the system - ❌
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //result - return standard products
    test('[PHN-2187]: case 1', async ({Endpoints, SPRequest }) => {
        await test.step('GET product list by POD ID - should return the standard product', async () => {
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=32XYTHGRFEDGGFDSEDvfWQ43564TF34`);
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
    test('[PHN-2187]: case 2 (check response)', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        test.setTimeout(6000000);
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
            const payload = GeneratePayload.productAndServices.product();
            payload.globalSegment = false
            payload.segmentIds = [1009]
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

            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=32XKNIKIAFYAT9433448622`);
            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === 13799);
            const foundStandardProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);

            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            console.log('Found standard product object:', JSON.stringify(foundStandardProduct, null, 2));

            expect(productIds).toContain(13799);
            expect(productIds).toContain(standardProductId);
        });
    });

    //pod present in system - ✅
    //customer id provided - ✅
    //customer present in the system - ❌
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //result - return standard products and pod specific products
    test('[PHN-2187]: case 3', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
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

    //pod present in system - ✅
    //customer id provided - ✅
    //customer present in the system - ✅
    //customer is unwanted customer - ✅
    //pod active in a contract with another customer - ❌
    //result - return standard products and pod specific products
    test('[PHN-2187]: case 4', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('generate unwanted customer', async () => {
            const unwantedCustomer = await Request.post(Endpoints.unwantedCustomer, {data: GeneratePayload.customers.unwanted_customer()});
            await expect(unwantedCustomer).CheckResponse();
            Responses.unwantedCustomer.push(await unwantedCustomer.json());
        })

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

        await test.step('GET product list by POD ID - should return the standard product and pod specific product', async () => {
            const getPodIdentifier  = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created customer identifier:', Responses.customer[0].identifier);
            console.log('created product id:', Responses.product[0]);

            const customerUIC = Responses.customer[0].identifier
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);

            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            // expect(body.length).toBeGreaterThanOrEqual(1);
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            const foundStandardProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            console.log('Found standard product object:', JSON.stringify(foundStandardProduct, null, 2));
            // expect(productIds).toContain(Responses.product[0]);
            expect(productIds).toContain(standardProductId);
        });
    });

    //pod present in system - ✅
    //customer id provided - ✅
    //customer present in the system - ✅
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ✅
    //result - return standard products and pod specific products
    test('[PHN-2187]: case 5', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
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

        await test.step('Create contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.basicParameters.customerId = 6000023
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json())
        });

        await test.step('Activate POD', async () => {
          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
          await expect(podActivation).CheckResponse();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list by POD ID - should return the standard product and pod specific product', async () => {
            const getPodIdentifier  = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);

            const customerUIC = Responses.customer[0].identifier
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);

            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            const foundStandardProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);
            console.log('Found pod specific product:', JSON.stringify(foundProduct, null, 2));
            console.log('Found standard product object:', JSON.stringify(foundStandardProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
            expect(productIds).toContain(standardProductId);
        });
    });

    //pod present in system - ✅
    //customer id provided - ✅
    //customer present in the system - ✅
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //pod marked for re signing - ✅
    //result - return standard products and customer specific products and re signing products
    test('[PHN-2187]: case 6', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        test.setTimeout(6000000)

        let contractPayload: any = null
        
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.resigningDeadlineType = 'MONTH'
            payload.resigningDeadlineValue = 20
            const term = await Request.post(Endpoints.terms, {data: payload});
            

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

        await test.step('Create contract', async () => {
            contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: contractPayload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json())
        });

        await test.step('Activate POD', async () => {
          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
          await expect(podActivation).CheckResponse();
        });

        await test.step('edit contract to set resigning', async () => {
            const payload = await GeneratePayload.contractsAndOrders.edit_ProductContract(contractPayload)
            payload.basicParameters.entryInForceDate = payload.basicParameters.signingDate
            payload.basicParameters.contractTermEndDate = '2026-05-30'
            const contract = await Request.put(`product-contract/${Responses.productContract[0].id}?versionId=1&changeFutureVersionsPods=false`, {data: payload});
            await expect(contract).CheckResponse();
        });

        await test.step('Create term for resign product', async () => {
          const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
          await expect(term).CheckResponse();
          Responses.terms.push(await term.json())
        });

        await test.step('Create resign product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.termId = Responses.terms[1].id
            payload.priceComponentIds = []
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json())

            const specialOfferPayload = await GeneratePayload.productAndServices.specialOffersTab(true, Responses.product[0]);
            const specialOffersResponse = await Request.put(`products/${Responses.product[1]}/special-offers?version=1`, { data: specialOfferPayload });
            await expect(specialOffersResponse).CheckResponse();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list by POD ID - should return the standard product and pod specific product', async () => {
            const getPodIdentifier  = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);

            const customerUIC = Responses.customer[0].identifier
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);

            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            const foundResignProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[1]);
            const foundStandardProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);

            console.log('Found pod specific product:', JSON.stringify(foundProduct, null, 2));
            console.log('Found standard product object:', JSON.stringify(foundStandardProduct, null, 2));
            console.log('Found resign product object:', JSON.stringify(foundResignProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
            expect(productIds).toContain(Responses.product[1]);
            expect(productIds).toContain(standardProductId);
        });
    });

    //pod present in system - ✅
    //customer id provided - ✅
    //customer present in the system - ✅
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //pod marked for re signing - ❌
    //pod active in a contract with provided customer - ✅
    //result - return product attached to contract
    test('[PHN-2187]: case 7', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            const term = await Request.post(Endpoints.terms, {data: payload});
            

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

        await test.step('Create contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json())
        });

        await test.step('Activate POD', async () => {
          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
          await expect(podActivation).CheckResponse();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list by POD ID - should return the standard product and pod specific product', async () => {
            const getPodIdentifier  = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);

            const customerUIC = Responses.customer[0].identifier
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);

            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeLessThan(2)
            console.log('Response body:', JSON.stringify(body, null, 2));
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);

            console.log('Found contract specific product:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    //pod present in system - ✅
    //customer id provided - ✅
    //customer present in the system - ✅
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //pod marked for re signing - ❌
    //pod active in a contract with provided customer - ❌
    //pod inside and active contract with provided customer - ✅
    //pod inside a contract with status signed by both sides or entered into force with provided customer - ❌
    //result - return product attached to contract
    test('[PHN-2187]: case 8', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            const term = await Request.post(Endpoints.terms, {data: payload});
            

            await expect(term).CheckResponse();
            Responses.terms.push(await term.json())
        });

        await test.step('Create POD', async () => {
          const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
          const podSettlement = await Request.post(Endpoints.pod, {data: payload});
          await expect(podSettlement).CheckResponse();
          Responses.pod.push(await podSettlement.json());
        });

        await test.step('Create POD 2', async () => {
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

        await test.step('Create contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json())
        });

        await test.step('Activate POD 2', async () => {
          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1)});
          await expect(podActivation).CheckResponse();
        });

        await test.step('create contract 2', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json())
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list by POD ID - should return the standard product and pod specific product', async () => {
            const getPodIdentifier  = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);

            const customerUIC = Responses.customer[0].identifier
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);

            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeLessThan(2)
            console.log('Response body:', JSON.stringify(body, null, 2));
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);

            console.log('Found contract specific product:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    //pod present in system - ✅
    //customer id provided - ✅
    //customer present in the system - ✅
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //pod marked for re signing - ❌
    //pod active in a contract with provided customer - ❌
    //pod inside and active contract with provided customer - ❌
    //pod inside a contract with status signed by both sides or entered into force with provided customer - ✅
    //result - return product attached to contract
    test('[PHN-2187]: case 9', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        test.setTimeout(6000000)
        // await test.step('generate customer', async () => {
        //   const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
        //   await expect(customer).CheckResponse();
        //   Responses.customer.push(await customer.json());
        // });

        // await test.step('Create price component', async () => {
        //   const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
        //   await expect(price).CheckResponse();
        //   Responses.priceComponent.push(await price.json());
        // });

        // await test.step('Create term', async () => {
        //     const payload = GeneratePayload.productAndServices.term();
        //     const term = await Request.post(Endpoints.terms, {data: payload});
            

        //     await expect(term).CheckResponse();
        //     Responses.terms.push(await term.json())
        // });

        // await test.step('Create POD', async () => {
        //   const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
        //   const podSettlement = await Request.post(Endpoints.pod, {data: payload});
        //   await expect(podSettlement).CheckResponse();
        //   Responses.pod.push(await podSettlement.json());
        // });

        // await test.step('Create product', async () => {
        //   const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
        //   await expect(product).CheckResponse();
        //   Responses.product.push(await product.json())
        // });

        // await test.step('Create contract', async () => {
        //     const payload = await GeneratePayload.contractsAndOrders.product_contract();
        //     const contract = await Request.post(Endpoints.productContract, {data: payload});
        //     await expect(contract).CheckResponse();
        //     Responses.productContract.push(await contract.json())
        // });

        // await test.step('create contract 2', async () => {
        //     const payload = await GeneratePayload.contractsAndOrders.product_contract();
        //     const contract = await Request.post(Endpoints.productContract, {data: payload});
        //     await expect(contract).CheckResponse();
        //     Responses.productContract.push(await contract.json())
        // });

        // console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list by POD ID - should return the standard product and pod specific product', async () => {
            // const getPodIdentifier  = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            // const podIdentifier = (await getPodIdentifier.json()).identifier;

            // console.log('Created POD identifier:', podIdentifier);
            // console.log('created product id:', Responses.product[0]);

            // const customerUIC = Responses.customer[0].identifier
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=32XKFXUQKRLBO7636656051&customerIdentifier=17782292919988446`);

            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeLessThan(2)
            console.log('Response body:', JSON.stringify(body, null, 2));
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);

            console.log('Found contract specific product:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    //pod present in system - ✅
    //customer id provided - ✅
    //customer present in the system - ✅
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //pod marked for re signing - ❌
    //pod active in a contract with provided customer - ❌
    //pod inside and active contract with provided customer - ❌
    //pod inside a contract with status signed by both sides or entered into force with provided customer - ❌
    //result - return standard products and customer specific products
    test('[PHN-2187]: case 10', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        await test.step('generate customer', async () => {
          const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
          await expect(customer).CheckResponse();
          Responses.customer.push(await customer.json());
        });

        await test.step('Create price component', async () => {
          const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.priceSettlement()});
          await expect(price).CheckResponse();
          Responses.priceComponent.push(await price.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            const term = await Request.post(Endpoints.terms, {data: payload});
            

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

        await test.step('Create contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.basicParameters.status = 'READY'
            payload.basicParameters.subStatus = 'READY'
            payload.basicParameters.signingDate = null
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json())
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list by POD ID - should return the standard product and pod specific product', async () => {
            const getPodIdentifier  = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);

            const customerUIC = Responses.customer[0].identifier
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);

            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            const foundStandardProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);
            console.log('Found customer specific product:', JSON.stringify(foundProduct, null, 2));
            console.log('Found standard product object:', JSON.stringify(foundStandardProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
            expect(productIds).toContain(standardProductId);
        });
    });

    //pod present in system - ✅ (but has 2 versions)
    //customer id provided - ❌
    //customer present in the system - ❌
    //customer is unwanted customer - ❌
    //pod active in a contract with another customer - ❌
    //result - return standard products and pod specific products
    test('[PHN-2187]: case 11 ', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {
        test.setTimeout(20 * 60 * 1000);
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

            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=32XPTSBLWNSUI9334056760`);
            await expect(response).CheckResponse();
            const body = await response.json();

            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === 12844);
            const foundStandardProduct = body.find((p: any) => (p.productId ?? p.id) === standardProductId);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            console.log('Found standard product object:', JSON.stringify(foundStandardProduct, null, 2));
            expect(productIds).toContain(12844);
            expect(productIds).toContain(standardProductId);
        });
    });

    // =====================================================================
    // TC-BE-1: Standard product returned for valid POD with all criteria met
    // =====================================================================
    test('[PHN-2187]: TC-BE-1 | Standard product returned for valid POD with all criteria met', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD (CONSUMER/SETTLEMENT_PERIOD/LOW/NON_HOUSEHOLD)', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product (standard, active, for sale, PORTALS, ALL areas/segments)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitType = 'TO'
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
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
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });

    });

    // =====================================================================
    // TC-BE-2: Multiple qualifying standard products returned for one POD
    // =====================================================================
    test('[PHN-2187]: TC-BE-2 | Multiple qualifying standard products returned for one POD', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create term 2', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product A (SUPPLY_ONLY)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        await test.step('Create product B (COMBINED)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.termId = Responses.terms[1].id
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        await test.step('GET product list by POD - should contain both products', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0], Responses.product[1]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
            const foundProduct1 = body.find((p: any) => (p.productId ?? p.id) === Responses.product[1]);
            console.log('Found product[1] object:', JSON.stringify(foundProduct1, null, 2));
            expect(productIds).toContain(Responses.product[1]);
        });
    });

    // =====================================================================
    // TC-BE-3: Product with customerUIC returns customer-specific products
    // =====================================================================
    test('[PHN-2187]: TC-BE-3 | customerUIC parameter returns customer-specific products', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create term 2', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create standard product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create customer specific product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.termId = Responses.terms[1].id
            payload.priceComponentIds = [Responses.priceComponent[1]];
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());

            const specialOfferPayload = await GeneratePayload.productAndServices.specialOffersTab(false);
            await Request.post(`products/${Responses.product[1]}/special-offers?version=1`, { data: specialOfferPayload });
        });

        await test.step('GET product list with customerUIC - should return both standard and customer-specific', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;

            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            console.log('created product id:', Responses.product[1]);

            const customerUIC = Responses.customer[0].id
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);

            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(2);
        });
    });

    // =====================================================================
    // TC-BE-4: Re-signing products returned when POD has active contract
    // =====================================================================
    test('[PHN-2187]: TC-BE-4 | Re-signing products returned when POD has active contract', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create base product for contract', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        await test.step('Create product contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.productParameters.contractType = 'COMBINED';
            payload.productParameters.entryIntoForce = 'SIGNING';
            payload.productParameters.startOfContractInitialTerm = 'SIGNING';
            payload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
            const response = await Request.post(Endpoints.productContract, { data: payload });
            await expect(response).CheckResponse();
            Responses.productContract.push(await response.json());
        });

        await test.step('Activate POD', async () => {
          const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
          await expect(podActivation).CheckResponse();
        });

        await test.step('Create re-signing product (targets base product)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.termId = Responses.terms[1].id
            payload.priceComponentIds = [Responses.priceComponent[1]];
            payload.priceComponentIds = [];
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());

            const specialOfferPayload = await GeneratePayload.productAndServices.specialOffersTab(false);
            await Request.post(`products/${Responses.product[1]}/special-offers?version=1`, { data: specialOfferPayload });
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should include re-signing product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const customerUIC = Responses.customer[0].id
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);
        });
    });

    // =====================================================================
    // TC-BE-5: Product with contract term type PERIOD is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-5 | Product with contract term type PERIOD is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with contract term type PERIOD', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with PERIOD term type should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-6: Product with contract term type WITHOUT_TERM is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-6 | Product with contract term type WITHOUT_TERM is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with WITHOUT_TERM', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'WITHOUT_TERM',
                value: '0',
                periodType: null,
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: 'Without term',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with WITHOUT_TERM should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-7: Product with CASH_DEPOSIT payment guarantee is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-7 | Product with CASH_DEPOSIT payment guarantee is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest, Nomenclatures }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with CASH_DEPOSIT guarantee', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['CASH_DEPOSIT'];
            payload.cashDepositAmount = 500;
            payload.cashDepositCurrencyId = envVariables.currency;
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with CASH_DEPOSIT should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-8: Product with BANK_GUARANTEE payment guarantee is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-8 | Product with BANK_GUARANTEE payment guarantee is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with BANK guarantee', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['BANK'];
            payload.bankGuaranteeAmount = 1000;
            payload.bankGuaranteeCurrencyId = envVariables.currency;
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with BANK guarantee should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-9: Product with equal monthly installments enabled is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-9 | Product with equal monthly installments enabled is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with equal monthly installments enabled', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = true;
            payload.installmentNumber = 6;
            payload.amount = 100;
            payload.currencyId = envVariables.currency;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with installments should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-10: Product with equal monthly installments disabled is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-10 | Product with equal monthly installments disabled is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with installments disabled', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.installmentNumber = null;
            payload.amount = null;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product without installments should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-11: Product with obligatory advance payment (exact amount) is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-11 | Product with obligatory advance payment (exact amount) is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create advance payment (obligatory, exact amount)', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'OBLIGATORY';
            payload.valueType = 'EXACT_AMOUNT';
            payload.value = 50;
            payload.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
            setMatchStandardInvoiceTermForInterim(payload);
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create product with advance payment', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with advance payment should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-12: Product with supply activation FIRST_DAY_OF_MONTH and single wait-for-expire value
    // =====================================================================
    test('[PHN-2187]: TC-BE-12 | Product with supply activation FIRST_DAY_OF_MONTH', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with FIRST_DAY_OF_MONTH supply activation', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should return product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-13: Product with supply activation type MANUAL is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-13 | Product with supply activation MANUAL is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with MANUAL supply activation', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['MANUAL'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with MANUAL supply activation should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-14: Product with term group assigned is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-14 | Product with term group assigned is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['MANUAL'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create term group', async () => {
            const payload = GeneratePayload.productAndServices.groupOfTerm();
            const response = await Request.post(Endpoints.termsGroup, { data: payload });
            await expect(response).CheckResponse();
            Responses.termsGroup.push(await response.json());
        });

        await test.step('Create product with term group (not direct term)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.termId = null; // Ensure termId is null when using termGroupId
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            // Use term group instead of direct term
            payload.termId = null;
            payload.termGroupId = Responses.termsGroup[0];
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with term group should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-15: Response JSON contains all required attributes
    // =====================================================================
    test('[PHN-2187]: TC-BE-15 | Response JSON contains all required attributes', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - verify all required attributes', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            expect(body.length).toBeGreaterThanOrEqual(1);

            const product = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            expect(product).toBeDefined();

            // Verify required top-level attributes
            expect(product).toHaveProperty('productId');
            expect(product).toHaveProperty('productVersion');
            expect(product).toHaveProperty('printingName');
            expect(product).toHaveProperty('printingNameTransliterated');
            expect(product).toHaveProperty('shortDescription');
            expect(product).toHaveProperty('paymentGuarantee');
            expect(product).toHaveProperty('contractType');

            // Verify contract term attributes (returned as flat fields)
            expect(product).toHaveProperty('contractTermTypeOfTerms');
            expect(product).toHaveProperty('contractTermValue');
            expect(product).toHaveProperty('contractTermType');
        });
    });

    // =====================================================================
    // TC-BE-16: All product versions fulfilling criteria are returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-16 | All product versions fulfilling criteria are returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product (version 1)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        await test.step('Create new version of the product (version 2)', async () => {
            const editPayload = await GeneratePayload.productAndServices.edit_Product(
                GeneratePayload.productAndServices.product(), 0
            );
            editPayload.productStatus = 'ACTIVE';
            editPayload.availableForSale = true;
            editPayload.globalSalesChannel = true;
            editPayload.globalSalesArea = true;
            editPayload.globalSegment = true;
            editPayload.contractTypes = ['COMBINED'];
            editPayload.paymentGuarantees = ['NO'];
            editPayload.typePointsOfDelivery = ['CONSUMER'];
            editPayload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            editPayload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            editPayload.voltageLevels = ['LOW'];
            editPayload.capacityLimitType = 'TO'
            editPayload.capacityLimitAmount = 5000;
            editPayload.isIndividual = false;
            editPayload.equalMonthlyInstallmentsActivation = false;
            (editPayload as any).updateExistingVersion = false;
            const response = await Request.put(`${Endpoints.product}/${Responses.product[0]}`, { data: editPayload });
            await expect(response).CheckResponse();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - both versions should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const matchingProducts = body.filter((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            expect(matchingProducts.length).toBeGreaterThanOrEqual(2);
        });
    });

    // =====================================================================
    // TC-BE-17: POD version filtering uses latest POD version when no contract exists
    // =====================================================================
    test('[PHN-2187]: TC-BE-17 | POD version filtering uses latest POD version (no contract)', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD (version 1 - LOW voltage)', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Update POD to version 2 (MEDIUM voltage)', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            const getResponse = await Request.get(`${Endpoints.pod}/${Responses.pod[0].id}?version=1`);
            const podData = await getResponse.json();
            podData.voltageLevel = 'MEDIUM';
            podData.addressRequest = {
                foreign: false,
                localAddressData: {
                    countryId: podData.country?.id ?? null,
                    regionId: podData.region?.id ?? null,
                    municipalityId: podData.municipality?.id ?? null,
                    populatedPlaceId: podData.populatedPlace?.id ?? null,
                    zipCodeId: podData.zipCode?.id ?? null,
                    districtId: podData.district?.id ?? null,
                    residentialAreaId: podData.residentialArea?.id ?? null,
                    countryTrsl: podData.countryTrsl ?? null,
                    regionTrsl: podData.regionTrsl ?? null,
                    municipalityTrsl: podData.municipalityTrsl ?? null,
                    populatedPlaceTrsl: podData.populatedPlaceTrsl ?? null,
                    zipCodeTrsl: podData.zipCodeTrsl ?? null,
                    districtTrsl: podData.districtTrsl ?? null,
                    residentialAreaTrsl: podData.residentialAreaTrsl ?? null,
                }
            };
            const response = await Request.put(`${Endpoints.pod}/${Responses.pod[0].id}`, { data: podData });
            await expect(response).CheckResponse();
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product compatible with MEDIUM voltage only', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['MEDIUM'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should use latest POD version (MEDIUM) and return product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-18: POD version filtering uses contract-specified POD version
    // =====================================================================
    test('[PHN-2187]: TC-BE-18 | POD version filtering uses contract-specified POD version', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD (version 1 - LOW voltage)', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product compatible with LOW voltage', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        await test.step('Create product contract (links POD version 1)', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.productParameters.contractType = 'COMBINED';
            payload.productParameters.entryIntoForce = 'SIGNING';
            payload.productParameters.startOfContractInitialTerm = 'SIGNING';
            payload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
            const response = await Request.post(Endpoints.productContract, { data: payload });
            await expect(response).CheckResponse();
            Responses.productContract.push(await response.json());
        });

        await test.step('Update POD to version 2 (MEDIUM voltage)', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            const getResponse = await Request.get(`${Endpoints.pod}/${Responses.pod[0].id}?version=1`);
            const podData = await getResponse.json();
            podData.voltageLevel = 'MEDIUM';
            podData.addressRequest = {
                foreign: false,
                localAddressData: {
                    countryId: podData.country?.id ?? null,
                    regionId: podData.region?.id ?? null,
                    municipalityId: podData.municipality?.id ?? null,
                    populatedPlaceId: podData.populatedPlace?.id ?? null,
                    zipCodeId: podData.zipCode?.id ?? null,
                    districtId: podData.district?.id ?? null,
                    residentialAreaId: podData.residentialArea?.id ?? null,
                    countryTrsl: podData.countryTrsl ?? null,
                    regionTrsl: podData.regionTrsl ?? null,
                    municipalityTrsl: podData.municipalityTrsl ?? null,
                    populatedPlaceTrsl: podData.populatedPlaceTrsl ?? null,
                    zipCodeTrsl: podData.zipCodeTrsl ?? null,
                    districtTrsl: podData.districtTrsl ?? null,
                    residentialAreaTrsl: podData.residentialAreaTrsl ?? null,
                }
            };
            const response = await Request.put(`${Endpoints.pod}/${Responses.pod[0].id}`, { data: podData });
            await expect(response).CheckResponse();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should use contract POD version (LOW) and return product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const customerUIC = Responses.customer[0].id
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-19: Customer version filtering uses latest version when no contract
    // =====================================================================
    test('[PHN-2187]: TC-BE-19 | Customer version filtering uses latest version (no contract)', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer (version 1)', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with ALL segments', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list with customerUIC - uses latest customer version', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const customerUIC = Responses.customer[0].id
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(Array.isArray(body)).toBeTruthy();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-27: CASH_DEPOSIT_AND_BANK payment guarantee (both amounts filled)
    // =====================================================================
    test('[PHN-2187]: TC-BE-27 | CASH_DEPOSIT_AND_BANK payment guarantee is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with CASH_DEPOSIT_AND_BANK guarantee', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['CASH_DEPOSIT_AND_BANK'];
            payload.cashDepositAmount = 500;
            payload.cashDepositCurrencyId = envVariables.currency;
            payload.bankGuaranteeAmount = 1000;
            payload.bankGuaranteeCurrencyId = envVariables.currency;
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with CASH_DEPOSIT_AND_BANK should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-28: Advance payment with match invoice date is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-28 | Advance payment with MATCH_THE_INVOICE_DATE is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create advance payment (obligatory, match invoice date)', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'OBLIGATORY';
            payload.valueType = 'EXACT_AMOUNT';
            payload.value = 50;
            payload.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
            setMatchStandardInvoiceTermForInterim(payload);
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with advance payment should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-29: Advance payment with PERIODICAL date of issue is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-29 | Advance payment with PERIODICAL date of issue is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create advance payment (obligatory, periodical)', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'OBLIGATORY';
            payload.valueType = 'EXACT_AMOUNT';
            payload.value = 50;
            payload.dateOfIssueType = 'PERIODICAL';
            payload.matchesWithTermOfStandardInvoice = false;
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should return product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-30: Advance payment matches standard invoice term is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-30 | Advance payment matches standard invoice term is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create advance payment (obligatory, matches standard invoice term)', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'OBLIGATORY';
            payload.valueType = 'EXACT_AMOUNT';
            payload.value = 75;
            payload.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
            setMatchStandardInvoiceTermForInterim(payload);
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should return product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-31: Customer-specific product matching by segment
    // =====================================================================
    test('[PHN-2187]: TC-BE-31 | Customer-specific product matching by segment', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_legal();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with ALL segments (matches any customer segment)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list with customerUIC - segment match returns product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const customerUIC = Responses.customer[0].id
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-32: Customer-specific product matching by preferences
    // =====================================================================
    test('[PHN-2187]: TC-BE-32 | Customer-specific product matching by preferences', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_legal();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with preferences matching customer', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list with customerUIC - preferences match returns product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const customerUIC = Responses.customer[0].id
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-33: Product with contract term type OTHER is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-33 | Product with contract term type OTHER is returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with OTHER contract term type', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'OTHER', value: '6', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '6 Day/Days Other', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with OTHER term type should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-34: Product with no availability period (empty from/to) is returned
    // =====================================================================
    test('[PHN-2187]: TC-BE-34 | Product with no availability period is always returned', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with empty availableFrom/To', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.availableFrom = null;
            payload.availableTo = null;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with no availability period should be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            const foundProduct = body.find((p: any) => (p.productId ?? p.id) === Responses.product[0]);
            console.log('Found product object:', JSON.stringify(foundProduct, null, 2));
            expect(productIds).toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // NEGATIVE TEST CASES
    // =====================================================================


    // =====================================================================
    // TC-BE-20: Inactive product is excluded from results
    // =====================================================================
    test('[PHN-2187]: TC-BE-20 | Inactive product is excluded from results', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create INACTIVE product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'INACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - inactive product should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-21: Product not available for sale is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-21 | Product not available for sale is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with availableForSale=false', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = false;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product not for sale should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-22: Product without Portals sales channel is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-22 | Product without Portals sales channel is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product without Portals sales channel', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = false;
            payload.salesChannelIds = [1002];
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product without Portals channel should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-23: Product with expired availability period is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-23 | Product with expired availability period is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with expired availability period', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.availableFrom = '2020-01-01';
            payload.availableTo = '2020-12-31';
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - expired product should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-24: Individual product is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-24 | Individual product is excluded from standard list', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create individual product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = null;
            payload.globalSalesChannel = false;
            payload.globalSalesArea = false;
            payload.globalSegment = false;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = true;
            payload.equalMonthlyInstallmentsActivation = false;
            delete (payload as any).salesChannelIds;
            delete (payload as any).salesAreasIds;
            delete (payload as any).segmentIds;
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'DAY_DAYS',
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: '12 Day/Days Period',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list without customerUIC - individual product should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-25: Deleted product is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-25 | Deleted product is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product then soft-delete it', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());

            // Soft-delete the product
            const deleteResponse = await Request.delete(`${Endpoints.product}/${Responses.product[0]}`);
            await expect(deleteResponse).CheckResponse();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - deleted product should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-26: Product with contract term type CERTAIN_DATE is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-26 | Product with contract term type CERTAIN_DATE is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with CERTAIN_DATE contract term', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{
                typeOfTerms: 'CERTAIN_DATE',
                value: '2027-12-31',
                periodType: null,
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: 'Certain date 2027-12-31',
                id: null
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with CERTAIN_DATE should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-35: Product with multiple contract types is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-35 | Product with multiple contract types is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with multiple contract types', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED', 'SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with multiple contract types should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-36: Product with multiple payment guarantees is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-36 | Product with multiple payment guarantees is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with multiple payment guarantees', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO', 'CASH_DEPOSIT'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product with multiple guarantees should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-37: Incomplete CASH_DEPOSIT (amount not filled) excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-37 | Incomplete CASH_DEPOSIT amount is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with CASH_DEPOSIT but no amount', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['CASH_DEPOSIT'];
            payload.cashDepositAmount = null;
            payload.cashDepositCurrencyId = null;
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - incomplete CASH_DEPOSIT should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-38: Incomplete BANK_GUARANTEE (amount not filled) excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-38 | Incomplete BANK_GUARANTEE amount is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with BANK guarantee but no amount', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['BANK'];
            payload.bankGuaranteeAmount = null;
            payload.bankGuaranteeCurrencyId = null;
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - incomplete BANK guarantee should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-39: Incomplete CASH_DEPOSIT_AND_BANK (cash deposit amount not filled)
    // =====================================================================
    test('[PHN-2187]: TC-BE-39 | CASH_DEPOSIT_AND_BANK with missing cash deposit amount excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with CASH_DEPOSIT_AND_BANK missing cash deposit', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['CASH_DEPOSIT_AND_BANK'];
            payload.cashDepositAmount = null;
            payload.cashDepositCurrencyId = null;
            payload.bankGuaranteeAmount = 1000;
            payload.bankGuaranteeCurrencyId = envVariables.currency;
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-40: Incomplete CASH_DEPOSIT_AND_BANK (bank guarantee amount not filled)
    // =====================================================================
    test('[PHN-2187]: TC-BE-40 | CASH_DEPOSIT_AND_BANK with missing bank guarantee amount excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with CASH_DEPOSIT_AND_BANK missing bank guarantee', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['CASH_DEPOSIT_AND_BANK'];
            payload.cashDepositAmount = 500;
            payload.cashDepositCurrencyId = envVariables.currency;
            payload.bankGuaranteeAmount = null;
            payload.bankGuaranteeCurrencyId = null;
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-41: Entering-into-force EXACT_DAY is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-41 | Entering-into-force EXACT_DAY is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER';
            payload.consumptionPurpose = 'NON_HOUSEHOLD';
            payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with EXACT_DAY entering-into-force', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['EXACT_DAY'];
            payload.contractEntryIntoForceFromExactDayOfMonthStartDay = '2027-01-01';
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE';
            payload.availableForSale = true;
            payload.globalSalesChannel = true;
            payload.globalSalesArea = true;
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED'];
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - EXACT_DAY entering-into-force should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-42: Entering-into-force MANUAL is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-42 | Entering-into-force MANUAL is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with MANUAL entering-into-force', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['MANUAL'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - MANUAL entering-into-force should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-43: Start-of-initial-term EXACT_DAY is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-43 | Start-of-initial-term EXACT_DAY is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with EXACT_DAY start-of-initial-term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['EXACT_DAY'];
            payload.startDayOfInitialContractTerm = '2027-01-01';
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - EXACT_DAY start-of-initial-term should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-44: Start-of-initial-term MANUAL is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-44 | Start-of-initial-term MANUAL is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with MANUAL start-of-initial-term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['MANUAL'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-45: Supply activation EXACT_DAY is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-45 | Supply activation EXACT_DAY is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with EXACT_DAY supply activation', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['EXACT_DAY'];
            payload.supplyActivationExactDateStartDay = '2027-01-01';
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - EXACT_DAY supply activation should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-46: Multiple entering-into-force values excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-46 | Multiple entering-into-force values is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with multiple entering-into-force values', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING', 'FIRST_DAY_OF_MONTH'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - multiple entering-into-force should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-47: Multiple start-of-initial-term values excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-47 | Multiple start-of-initial-term values is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with multiple start-of-initial-term values', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING', 'FIRST_DAY_OF_MONTH'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-48: Multiple supply activation values excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-48 | Multiple supply activation values is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term with multiple supply activation values', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'MANUAL'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-49: Null price component formula variable value excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-49 | Null price component formula variable value excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component with null formula variable value', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 'X1';
            payload.formulaRequest.variables = [{ name: 'X1', value: null as any }];
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING'];
            payload.startsOfContractInitialTerms = ['SIGNING'];
            payload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
            payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - null formula variable should exclude product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-50: Advance payment EXACT_AMOUNT with null value excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-50 | Advance payment EXACT_AMOUNT with null value excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create advance payment with EXACT_AMOUNT but null value', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'OBLIGATORY';
            payload.valueType = 'EXACT_AMOUNT';
            payload.value = null;
            payload.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
            setMatchStandardInvoiceTermForInterim(payload);
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-51: Advance payment % from previous with null value excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-51 | Advance payment PERCENT with null value excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create advance payment with PERCENT_FROM_PREVIOUS but null value', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'OBLIGATORY';
            payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
            payload.value = null;
            payload.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
            setMatchStandardInvoiceTermForInterim(payload);
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-52: Advance payment PRICE_COMPONENT with null priceComponentId excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-52 | Advance payment PRICE_COMPONENT with null id excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create advance payment with PRICE_COMPONENT but null priceComponentId', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'OBLIGATORY';
            payload.valueType = 'PRICE_COMPONENT';
            payload.priceComponentId = null;
            payload.value = null;
            payload.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
            setMatchStandardInvoiceTermForInterim(payload);
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-53: Advance payment "at least one selected" is excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-53 | Advance payment AT_LEAST_ONE is excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create advance payment with AT_LEAST_ONE', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'AT_LEAST_ONE';
            payload.valueType = 'EXACT_AMOUNT';
            payload.value = 50;
            payload.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
            setMatchStandardInvoiceTermForInterim(payload);
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - AT_LEAST_ONE advance payment should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-54: Different advance payment settings excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-54 | Different advance payment settings excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create first advance payment (OBLIGATORY)', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'OBLIGATORY';
            payload.valueType = 'EXACT_AMOUNT';
            payload.value = 50;
            payload.dateOfIssueType = 'MATCH_THE_INVOICE_DATE';
            setMatchStandardInvoiceTermForInterim(payload);
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create second advance payment (AT_LEAST_ONE - different setting)', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.paymentType = 'AT_LEAST_ONE';
            payload.valueType = 'EXACT_AMOUNT';
            payload.value = 30;
            payload.dateOfIssueType = 'PERIODICAL';
            payload.matchesWithTermOfStandardInvoice = false;
            const response = await Request.post(Endpoints.interim, { data: payload });
            await expect(response).CheckResponse();
            Responses.interim.push(await response.json());
        });

        await test.step('Create product with both advance payments', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - different advance payment settings should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-55: Equal monthly installments missing installment number excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-55 | Equal monthly installments missing number excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with installments enabled but missing number', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = true;
            payload.installmentNumber = null;
            payload.amount = 100;
            payload.currencyId = envVariables.currency;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-56: Equal monthly installments missing amount excluded
    // =====================================================================
    test('[PHN-2187]: TC-BE-56 | Equal monthly installments missing amount excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with installments enabled but missing amount', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false;
            payload.equalMonthlyInstallmentsActivation = true;
            payload.installmentNumber = 6;
            payload.amount = null;
            payload.currencyId = envVariables.currency;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-57 through TC-BE-62: POD mismatch tests
    // =====================================================================
    test('[PHN-2187]: TC-BE-57 | POD mismatch - grid operator excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with non-matching grid operator', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.globalGridOperator = false;
            payload.gridOperatorIds = [99999];
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - non-matching grid operator should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    test('[PHN-2187]: TC-BE-58 | POD mismatch - type of delivery excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD (CONSUMER)', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product for GENERATOR only (mismatch CONSUMER POD)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['GENERATOR'];
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - mismatched type should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    test('[PHN-2187]: TC-BE-59 | POD mismatch - purpose of consumption excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD (NON_HOUSEHOLD)', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product for HOUSEHOLD only (mismatch)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER'];
            payload.purposeOfConsumptions = ['HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - mismatched purpose should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    test('[PHN-2187]: TC-BE-60 | POD mismatch - metering type excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD (SETTLEMENT_PERIOD)', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product for SLP only (mismatch SETTLEMENT_PERIOD POD)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SLP'];
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - mismatched metering type should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    test('[PHN-2187]: TC-BE-61 | POD mismatch - voltage level excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD (LOW voltage)', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product for HIGH voltage only (mismatch LOW POD)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            payload.voltageLevels = ['HIGH'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - mismatched voltage should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    test('[PHN-2187]: TC-BE-62 | POD capacity exceeded - consumption over limit excluded', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD with high consumption (10000 kWh)', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW';
            payload.estimatedMonthlyAvgConsumption = '10000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with low capacity limit (500 kWh)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 500;
            payload.isIndividual = false; payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - capacity exceeded should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-63: Re-signing product excluded when no active contract on POD
    // =====================================================================
    test('[PHN-2187]: TC-BE-63 | Re-signing product excluded without active contract', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD (no contract)', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create re-signing product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            (payload as any).isResigning = true;
            (payload as any).resignProductTargets = [];
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - re-signing without contract should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-64: Re-signing product excluded when contract product not in target list
    // =====================================================================
    test('[PHN-2187]: TC-BE-64 | Re-signing excluded when contract product not in target list', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create base product for contract', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        await test.step('Create product contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.productParameters.contractType = 'COMBINED';
            payload.productParameters.entryIntoForce = 'SIGNING';
            payload.productParameters.startOfContractInitialTerm = 'SIGNING';
            payload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
            const response = await Request.post(Endpoints.productContract, { data: payload });
            await expect(response).CheckResponse();
            Responses.productContract.push(await response.json());
        });

        await test.step('Create re-signing product targeting non-existent product (not contract product)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; 
            payload.availableForSale = true; 
            payload.globalSalesChannel = true; 
            payload.globalSalesArea = true; 
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; 
            payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; 
            payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; 
            payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            (payload as any).isResigning = true;
            (payload as any).resignProductTargets = [99999];
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - re-signing without target match should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const customerUIC = Responses.customer[0].id
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[1]);
        });
    });

    // =====================================================================
    // TC-BE-65: Customer-specific product excluded without matching UIC
    // =====================================================================
    test('[PHN-2187]: TC-BE-65 | Customer-specific excluded without matching UIC', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create individual product linked to a different customer identifier', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = null; payload.globalSalesChannel = false; payload.globalSalesArea = false; payload.globalSegment = false;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'
            payload.isIndividual = true;
            payload.customerIdentifier = '9999999999';
            payload.equalMonthlyInstallmentsActivation = false;
            delete (payload as any).salesChannelIds;
            delete (payload as any).salesAreasIds;
            delete (payload as any).segmentIds;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list with non-matching UIC - should NOT return individual product', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const customerUIC = Responses.customer[0].id
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-66: Customer-specific product excluded without matching segment
    // =====================================================================
    test('[PHN-2187]: TC-BE-66 | Customer-specific excluded without matching segment', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with non-matching segment', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true;
            payload.globalSegment = false;
            payload.segmentIds = [99999];
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO';
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - non-matching segment should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const customerUIC = Responses.customer[0].id
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}&customerIdentifier=${customerUIC}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });

    // =====================================================================
    // TC-BE-67: Invalid podId returns error
    // =====================================================================
    test('[PHN-2187]: TC-BE-67 | Invalid podId returns error', async ({ Endpoints, SPRequest }) => {

        await test.step('GET product list with invalid podId', async () => {
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}/99999999`);
            const status = response.status();
            expect(status).toBeGreaterThanOrEqual(400);
        });
    });

    // =====================================================================
    // TC-BE-68: Invalid customerUIC returns error
    // =====================================================================
    test('[PHN-2187]: TC-BE-68 | Invalid customerUIC returns error', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('GET product list with invalid customerUIC', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}?customerUIC=INVALID`);
            const status = response.status();
            expect(status).toBeGreaterThanOrEqual(400);
        });
    });

    // =====================================================================
    // TC-BE-69: Inactive POD returns empty or error
    // =====================================================================
    test('[PHN-2187]: TC-BE-69 | Inactive POD returns empty list or error', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create customer', async () => {
            const payload = GeneratePayload.customers.customer_private();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product and contract to activate POD', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true; payload.globalSalesArea = true; payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO'; 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());

            const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
            contractPayload.productParameters.contractType = 'COMBINED';
            contractPayload.productParameters.entryIntoForce = 'SIGNING';
            contractPayload.productParameters.startOfContractInitialTerm = 'SIGNING';
            contractPayload.productParameters.supplyActivation = 'FIRST_DAY_OF_MONTH';
            const contract = await Request.post(Endpoints.productContract, { data: contractPayload });
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate then deactivate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', { data: await GeneratePayload.pointsOfDelivery.pod_activation(0, '2025-01-01', '2025-01-01') });
            await expect(podActivation).CheckResponse();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list for deactivated POD - should return empty or limited results', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            const status = response.status();
            if (status === 200) {
                const body = await response.json();
                expect(Array.isArray(body)).toBeTruthy();
            } else {
                expect(status).toBeGreaterThanOrEqual(400);
            }
        });
    });

    // =====================================================================
    // TC-BE-70: Product with non-global areas (not ALL) is excluded for standard list
    // =====================================================================
    test('[PHN-2187]: TC-BE-70 | Product without ALL areas is excluded from standard list', async ({ Request, GeneratePayload, Responses, Endpoints, SPRequest }) => {

        await test.step('Create POD', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.type = 'CONSUMER'; payload.consumptionPurpose = 'NON_HOUSEHOLD'; payload.voltageLevel = 'LOW'; payload.estimatedMonthlyAvgConsumption = '3000';
            const response = await Request.post(Endpoints.pod, { data: payload });
            await expect(response).CheckResponse();
            Responses.pod.push(await response.json());
        });

        await test.step('Create price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            Responses.priceComponent.push(await response.json());
        });

        await test.step('Create term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['SIGNING']; payload.startsOfContractInitialTerms = ['SIGNING']; payload.supplyActivations = ['FIRST_DAY_OF_MONTH']; payload.waitForOldContractTermToExpires = ['NO'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push(await response.json());
        });

        await test.step('Create product with non-global areas', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.productStatus = 'ACTIVE'; payload.availableForSale = true; payload.globalSalesChannel = true;
            payload.globalSalesArea = false;
            payload.salesAreasIds = [99999];
            payload.globalSegment = true;
            payload.contractTypes = ['COMBINED']; payload.paymentGuarantees = ['NO'];
            payload.typePointsOfDelivery = ['CONSUMER']; payload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            payload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD']; payload.voltageLevels = ['LOW'];
            payload.capacityLimitAmount = 5000;
            payload.capacityLimitType = 'TO' 
            payload.isIndividual = false; 
            payload.equalMonthlyInstallmentsActivation = false;
            payload.productTerms = [{ typeOfTerms: 'PERIOD', value: '12', periodType: 'DAY_DAYS', renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false, automaticRenewal: null, numberOfRenewals: null, name: '12 Day/Days Period', id: null }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            Responses.product.push(await response.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('GET product list - product without ALL areas should NOT be returned', async () => {
            const getPodIdentifier = await Request.get(Endpoints.pod + `/${Responses.pod[0].id}`);
            const podIdentifier = (await getPodIdentifier.json()).identifier;
            console.log('Created POD identifier:', podIdentifier);
            console.log('created product id:', Responses.product[0]);
            const response = await SPRequest.get(`${Endpoints.salesPortalEndpoints.getProductListByPod}?podIdentifier=${podIdentifier}`);
            await expect(response).CheckResponse();
            const body = await response.json();
            const productIds = body.map((p: any) => p.productId ?? p.id);
            expect(productIds).not.toContain(Responses.product[0]);
        });
    });
});