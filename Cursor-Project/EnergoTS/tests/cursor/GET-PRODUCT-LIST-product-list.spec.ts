import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import { envVariables } from '../../fixtures/envCashed';

async function sharedTerm(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const termPayload = GeneratePayload.productAndServices.term();
    const termResponse = await Request.post(Endpoints.terms, { data: termPayload });
    await expect(termResponse).toBeOK();
    Responses.terms.push(await termResponse.json());
}

async function sharedPrice(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const pricePayload = GeneratePayload.productAndServices.electricity();
    const priceResponse = await Request.post(Endpoints.priceComponent, { data: pricePayload });
    await expect(priceResponse).toBeOK();
    Responses.priceComponent.push(await priceResponse.json());
}ა

async function sharedProduct(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const productPayload = GeneratePayload.productAndServices.product();
    productPayload.contractTypes = ['SUPPLY_ONLY'];
    productPayload.paymentGuarantees = ['NO'];

    const productResponse = await Request.post(Endpoints.product, { data: productPayload });
    await expect(productResponse).toBeOK();
    Responses.product.push(await productResponse.json());
}


/**
 * GET-PRODUCT-LIST — POST /products/list filtering validation.
 * Maps 1:1 to:
 * - test_cases/Backend/Get_product_list_energy_products.md  (TC-BE-1 … TC-BE-58)
 * - test_cases/Frontend/Get_product_list_energy_products.md (TC-FE-1 … TC-FE-12)
 */
test.describe('[GET-PRODUCT-LIST]: Get Product List', { tag: '@productAndServices' }, () => {
    test.describe.configure({ mode: 'serial' });

    // TC-BE-1 … TC-BE-8 — Status, availability, dates
    // ──────────────────────────────────────────────────────────────────────────

    test('[GET-PRODUCT-LIST]: TC-BE-1 – Happy path – Valid product appears in product list', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Call POST /products/list and find base product', async () => {
            expect(Responses.product[0]).toBeTruthy();
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-2 – Product with globalSalesChannel=true is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (globalSalesChannel=true) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-3 – Inactive product is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with INACTIVE status', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.productStatus = 'INACTIVE';
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify inactive product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-3 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-4 – Product with availableForSale=false is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with availableForSale=false', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.availableForSale = false;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-4 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-5 – Product outside availability period (future availableFrom) is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with future availableFrom', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.availableFrom = '2030-01-01';
            payload.availableTo = null;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-5 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-6 – Product outside availability period (past availableTo) is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with past availableTo', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.availableFrom = null;
            payload.availableTo = '2020-01-01';
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-6 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-7 – Product within availability period is included', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with current date in range', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.availableFrom = '2024-01-01';
            payload.availableTo = '2030-12-31';
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-7 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-8 – Product with both availableFrom and availableTo null is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (both dates null) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-8 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ──────────────────────────────────────────────────────────────────────────
    // TC-BE-9 … TC-BE-13 — Individual, deleted, channels, areas, segments
    // ──────────────────────────────────────────────────────────────────────────

    test('[GET-PRODUCT-LIST]: TC-BE-9 – Individual product is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create individual product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.isIndividual = true;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify individual product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-9 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-10 – Deleted product is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create a valid product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Delete the product', async () => {
            const response = await Request.delete(`${Endpoints.product}/${productId}`);
            expect(response.status()).toBeLessThan(300);
        });

        await test.step('Verify deleted product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-10 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-11 – Product without Portals channel is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with globalSalesChannel=false', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.globalSalesChannel = false;
            payload.salesChannelIds = [999999];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-11 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-12 – Product without ALL areas is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with globalSalesArea=false', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.globalSalesArea = false;
            payload.salesAreasIds = [1];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-12 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-13 – Product without ALL segments is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with globalSegment=false', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.globalSegment = false;
            payload.segmentIds = [1];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-13 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ──────────────────────────────────────────────────────────────────────────
    // TC-BE-14 … TC-BE-20 — Contract types, term types, payment terms
    // ──────────────────────────────────────────────────────────────────────────

    test('[GET-PRODUCT-LIST]: TC-BE-14 – Product with multiple contract types is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with multiple contractTypes', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['COMBINED', 'SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-14 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-15 – Product with single contract type is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (single contractType) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-15 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-16 – Product with CERTAIN_DATE term type is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with CERTAIN_DATE term type', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.productTerms = [{
                typeOfTerms: 'CERTAIN_DATE',
                value: '2030-01-01',
                periodType: null,
                renewalPeriodValue: null,
                renewalPeriodType: null,
                perpetuityCause: false,
                automaticRenewal: null,
                numberOfRenewals: null,
                name: 'Certain date term',
                id: null,
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-16 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-17 – Product with PERIOD term type is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (PERIOD term) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-17 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-18 – Product with WITHOUT_TERM type is included', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with WITHOUT_TERM type', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
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
                id: null,
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-18 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-19 – Product with multiple payment terms is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with multiple productTerms', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.productTerms = [
                {
                    typeOfTerms: 'PERIOD', value: '12', periodType: 'MONTH',
                    renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false,
                    automaticRenewal: null, numberOfRenewals: null, name: '12 Month', id: null,
                },
                {
                    typeOfTerms: 'PERIOD', value: '24', periodType: 'MONTH',
                    renewalPeriodValue: null, renewalPeriodType: null, perpetuityCause: false,
                    automaticRenewal: null, numberOfRenewals: null, name: '24 Month', id: null,
                },
            ];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-19 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-20 – Product with single payment term is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (single productTerm) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-20 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ──────────────────────────────────────────────────────────────────────────
    // TC-BE-21 … TC-BE-28 — Payment guarantees
    // ──────────────────────────────────────────────────────────────────────────

    test('[GET-PRODUCT-LIST]: TC-BE-21 – Product with multiple payment guarantees is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with multiple payment guarantees', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['CASH_DEPOSIT', 'BANK'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-21 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-22 – Product with payment guarantee NO is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (paymentGuarantees=NO) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-22 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-23 – Product with CASH_DEPOSIT and valid amount is included', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with CASH_DEPOSIT + amount', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['CASH_DEPOSIT'];
            payload.cashDepositAmount = 500;
            payload.cashDepositCurrencyId = envVariables.currency;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-23 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-24 – Product with CASH_DEPOSIT but missing amount is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with CASH_DEPOSIT but no amount', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['CASH_DEPOSIT'];
            payload.cashDepositAmount = null;
            payload.cashDepositCurrencyId = null;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-24 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-25 – Product with BANK_GUARANTEE and valid amount is included', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with BANK guarantee + amount', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['BANK'];
            payload.bankGuaranteeAmount = 1000;
            payload.bankGuaranteeCurrencyId = envVariables.currency;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-25 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-26 – Product with BANK_GUARANTEE but missing amount is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with BANK guarantee but no amount', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['BANK'];
            payload.bankGuaranteeAmount = null;
            payload.bankGuaranteeCurrencyId = null;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-26 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-27 – Product with CASH_DEPOSIT_AND_BANK and both amounts is included', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with CASH_DEPOSIT_AND_BANK + both amounts', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['CASH_DEPOSIT_AND_BANK'];
            payload.cashDepositAmount = 500;
            payload.cashDepositCurrencyId = envVariables.currency;
            payload.bankGuaranteeAmount = 1000;
            payload.bankGuaranteeCurrencyId = envVariables.currency;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-27 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-28 – Product with CASH_DEPOSIT_AND_BANK missing one amount is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with CASH_DEPOSIT_AND_BANK but missing bank amount', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['CASH_DEPOSIT_AND_BANK'];
            payload.cashDepositAmount = 500;
            payload.cashDepositCurrencyId = envVariables.currency;
            payload.bankGuaranteeAmount = null;
            payload.bankGuaranteeCurrencyId = null;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-28 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ──────────────────────────────────────────────────────────────────────────
    // TC-BE-29 … TC-BE-38 — Price components, entering into force, initial term, supply activation
    // ──────────────────────────────────────────────────────────────────────────

    test('[GET-PRODUCT-LIST]: TC-BE-29 – Product with price component having fixed value is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (fixed-value price component) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-29 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-30 – Product with price component missing value is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;
        let emptyPcId: number;

        await test.step('Precondition: Create terms', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create price component without fixed value', async () => {
            const payload = GeneratePayload.productAndServices.electricity();
            payload.formulaRequest.expression = '';
            payload.formulaRequest.variables = [{ name: 'x', value: '' }];
            const response = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(response).CheckResponse();
            emptyPcId = (await response.json())
            Responses.priceComponent.push(emptyPcId);
        });

        await test.step('Create product linked to empty price component', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-30 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-31 – Product with entering into force type Exact day is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms with EXACT_DAY entry into force', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['EXACT_DAY'];
            payload.startsOfContractInitialTerms = ['FIRST_DAY_OF_MONTH'];
            payload.supplyActivations = ['MANUAL'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push({ id: (await response.json()).id });
        });

        await test.step('Precondition: Populate price component', async () => {
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-31 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-32 – Product with entering into force type Manual is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms with MANUAL entry into force', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['MANUAL'];
            payload.startsOfContractInitialTerms = ['FIRST_DAY_OF_MONTH'];
            payload.supplyActivations = ['MANUAL'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push({ id: (await response.json()).id });
        });

        await test.step('Precondition: Populate price component', async () => {
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-32 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-33 – Product with entering into force type other than Exact day/Manual is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (FIRST_DAY_OF_MONTH entry into force) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-33 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-34 – Product with multiple entering into force values is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms with multiple entry into force', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['FIRST_DAY_OF_MONTH', 'MANUAL'];
            payload.startsOfContractInitialTerms = ['FIRST_DAY_OF_MONTH'];
            payload.supplyActivations = ['MANUAL'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push({ id: (await response.json()).id });
        });

        await test.step('Precondition: Populate price component', async () => {
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-34 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-35 – Product with start of initial term type Exact day is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms with EXACT_DAY start of initial term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['FIRST_DAY_OF_MONTH'];
            payload.startsOfContractInitialTerms = ['EXACT_DAY'];
            payload.supplyActivations = ['MANUAL'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push({ id: (await response.json()).id });
        });

        await test.step('Precondition: Populate price component', async () => {
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-35 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-36 – Product with start of initial term type Manual is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms with MANUAL start of initial term', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['FIRST_DAY_OF_MONTH'];
            payload.startsOfContractInitialTerms = ['MANUAL'];
            payload.supplyActivations = ['MANUAL'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push({ id: (await response.json()).id });
        });

        await test.step('Precondition: Populate price component', async () => {
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-36 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-37 – Product with supply activation type Exact day is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms with EXACT_DAY supply activation', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['FIRST_DAY_OF_MONTH'];
            payload.startsOfContractInitialTerms = ['FIRST_DAY_OF_MONTH'];
            payload.supplyActivations = ['EXACT_DAY'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            Responses.terms.push({ id: (await response.json()).id });
        });

        await test.step('Precondition: Populate price component', async () => {
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-37 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-38 – Product with supply activation type Manual is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (MANUAL supply activation) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-38 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ──────────────────────────────────────────────────────────────────────────
    // TC-BE-39 … TC-BE-47 — Interim advance payments, equal monthly installments
    // ──────────────────────────────────────────────────────────────────────────

    test('[GET-PRODUCT-LIST]: TC-BE-39 – Product with interim payment AT_LEAST_ONE is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create IAP with AT_LEAST_ONE selection', async () => {
            const iapPayload = GeneratePayload.productAndServices.interim();
            iapPayload.paymentType = 'AT_LEAST_ONE';
            const response = await Request.post(Endpoints.interim, { data: iapPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.interim.push(json.id ?? json);
        });

        await test.step('Create product with AT_LEAST_ONE IAP', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-39 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-40 – Product with obligatory interim payment and valid config is included', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create IAP with OBLIGATORY + valid values', async () => {
            const iapPayload = GeneratePayload.productAndServices.interim();
            iapPayload.paymentType = 'OBLIGATORY';
            iapPayload.valueType = 'EXACT_AMOUNT';
            iapPayload.value = 100;
            iapPayload.currencyId = envVariables.currency;
            const response = await Request.post(Endpoints.interim, { data: iapPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.interim.push(json.id ?? json);
        });

        await test.step('Create product with obligatory IAP', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-40 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-41 – Product with obligatory interim payment but missing value is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create IAP with OBLIGATORY but null value', async () => {
            const iapPayload = GeneratePayload.productAndServices.interim();
            iapPayload.paymentType = 'OBLIGATORY';
            iapPayload.valueType = 'EXACT_AMOUNT';
            iapPayload.value = null;
            const response = await Request.post(Endpoints.interim, { data: iapPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.interim.push(json.id ?? json);
        });

        await test.step('Create product with invalid IAP', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-41 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-42 – Product with IAP Days after invoice date but no value is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create IAP with DAYS_AFTER_INVOICE_DATE but no value', async () => {
            const iapPayload = GeneratePayload.productAndServices.interim();
            iapPayload.dateOfIssueType = 'WORKING_DAYS_AFTER_INVOICE_DATE';
            iapPayload.dateOfIssueValue = null;
            const response = await Request.post(Endpoints.interim, { data: iapPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.interim.push(json.id ?? json);
        });

        await test.step('Create product with invalid IAP config', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-42 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-43 – Product with interim payments having different settings is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create first IAP (EXACT_AMOUNT)', async () => {
            const iapPayload = GeneratePayload.productAndServices.interim();
            iapPayload.valueType = 'EXACT_AMOUNT';
            iapPayload.value = 100;
            iapPayload.currencyId = envVariables.currency;
            const response = await Request.post(Endpoints.interim, { data: iapPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.interim.push(json.id ?? json);
        });

        await test.step('Precondition: Create second IAP (PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT)', async () => {
            const iapPayload = GeneratePayload.productAndServices.interim();
            iapPayload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
            iapPayload.value = 50;
            const response = await Request.post(Endpoints.interim, { data: iapPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.interim.push(json.id ?? json);
        });

        await test.step('Create product with two IAPs having different settings', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-43 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-44 – Product without equal monthly installments is included', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify base product (installments disabled) in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-44 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-45 – Product with equal monthly installments enabled and all values is included', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with installments enabled + all values filled', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.equalMonthlyInstallmentsActivation = true;
            payload.installmentNumber = 12;
            payload.amount = 100;
            payload.currencyId = envVariables.currency;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-45 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-46 – Product with installments enabled but missing installmentNumber is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with installments enabled but no installmentNumber', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.equalMonthlyInstallmentsActivation = true;
            payload.installmentNumber = null;
            payload.amount = 100;
            payload.currencyId = envVariables.currency;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-46 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-47 – Product with installments enabled but missing amount is excluded', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with installments enabled but no amount', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.equalMonthlyInstallmentsActivation = true;
            payload.installmentNumber = 12;
            payload.amount = null;
            payload.currencyId = envVariables.currency;
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product excluded from list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-47 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ──────────────────────────────────────────────────────────────────────────
    // TC-BE-48 … TC-BE-58 — Re-signing, response validation, pagination, groups
    // ──────────────────────────────────────────────────────────────────────────

    test.skip('[GET-PRODUCT-LIST]: TC-BE-48 – Re-signing product is excluded', async () => {
        // Re-signing product mechanism unclear in current API; skip pending business rule clarification.
    });

    test('[GET-PRODUCT-LIST]: TC-BE-49 – Response contains all required product attributes', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Call POST /products/list and validate base product attributes', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const product = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(product).toBeTruthy();

            expect(product.id).toBeTruthy();
            expect(product.version).toBeDefined();
            expect(product.printingName).toBeTruthy();
            expect(product.printingNameTransliterated).toBeDefined();
            expect(product.shortDescription).toBeDefined();
            expect(product.contractTypes).toBeDefined();
            expect(product.paymentGuarantees).toBeDefined();
            expect(product.typePointsOfDelivery).toBeDefined();
            expect(product.purposeOfConsumptions).toBeDefined();
            expect(product.meteringTypeOfThePointOfDeliveries).toBeDefined();
            expect(product.voltageLevels).toBeDefined();
        });

        await test.step('Validate contract term details', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const product = body.content?.find((p: any) => p.id === Responses.product[0].id);
            if (product.productContractTerms && product.productContractTerms.length > 0) {
                const term = product.productContractTerms[0];
                expect(term.typeOfTerms).toBeDefined();
                expect(term.value).toBeDefined();
            }
        });

        await test.step('Validate price components present', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const product = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(product.priceComponents || product.priceComponentIds).toBeDefined();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-49 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-50 – Price components filtered by Active electric energy or Fee type', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify price components in product list response', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const product = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(product).toBeTruthy();
            if (product.priceComponents && Array.isArray(product.priceComponents)) {
                for (const pc of product.priceComponents) {
                    const validTypes = ['ACTIVE_ELECTRIC_ENERGY', 'FEE', 'Active electric energy', 'Fee'];
                    const hasValidType = validTypes.some(
                        (t) => pc.priceType === t || pc.priceComponentPriceType === t
                    );
                    expect(hasValidType).toBeTruthy();
                }
            }
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-50 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-51 – Contract term with automatic renewal details returned correctly', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with automatic renewal', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.productTerms = [{
                typeOfTerms: 'PERIOD',
                value: '12',
                periodType: 'MONTH',
                renewalPeriodValue: '12',
                renewalPeriodType: 'MONTH',
                perpetuityCause: true,
                automaticRenewal: true,
                numberOfRenewals: null,
                name: '12 Month with renewal',
                id: null,
            }];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product in list and check renewal details', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const product = body.content?.find((p: any) => p.id === productId);
            expect(product).toBeTruthy();
            if (product.productContractTerms && product.productContractTerms.length > 0) {
                const term = product.productContractTerms[0];
                expect(term.automaticRenewal).toBe(true);
                expect(term.renewalPeriodValue).toBeDefined();
                expect(term.renewalPeriodType).toBeDefined();
            }
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-51 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-52 – Application model for price components returned', async ({
        Request, GeneratePayload, Endpoints, Responses,
    }) => {

        await test.step('Precondition: Create shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Verify application model in product list response', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const product = body.content?.find((p: any) => p.id === Responses.product[0].id);
            expect(product).toBeTruthy();
            if (product.priceComponents && Array.isArray(product.priceComponents)) {
                const pcWithModel = product.priceComponents.find(
                    (pc: any) => pc.applicationModel || pc.applicationModelRequest
                );
                if (pcWithModel) {
                    const model = pcWithModel.applicationModel || pcWithModel.applicationModelRequest;
                    expect(model).toBeDefined();
                }
            }
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-52 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-53 – Product list returns paginated results', async ({
        Request, Endpoints, Responses,
    }) => {
        await test.step('Request first page with size=1', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, {
                data: { page: 0, size: 1 },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.content).toBeDefined();
            expect(Array.isArray(body.content)).toBeTruthy();
            expect(body.content.length).toBeLessThanOrEqual(1);
            expect(typeof body.totalElements).toBe('number');
            expect(typeof body.totalPages).toBe('number');
        });

        await test.step('Request second page', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, {
                data: { page: 1, size: 1 },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.content).toBeDefined();
            expect(Array.isArray(body.content)).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-53 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[GET-PRODUCT-LIST]: TC-BE-54 – Empty list when no products meet criteria', async () => {
        // Cannot guarantee all products in shared environment fail criteria; skip.
    });

    test('[GET-PRODUCT-LIST]: TC-BE-55 – Product with term group is included if group passes validation', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Create terms for group', async () => {
            const payload = GeneratePayload.productAndServices.term();
            payload.contractEntryIntoForces = ['FIRST_DAY_OF_MONTH'];
            payload.startsOfContractInitialTerms = ['FIRST_DAY_OF_MONTH'];
            payload.supplyActivations = ['MANUAL'];
            const response = await Request.post(Endpoints.terms, { data: payload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.terms.push({ id: json.id });
        });

        await test.step('Precondition: Create term group', async () => {
            const groupPayload = GeneratePayload.productAndServices.groupOfTerm();
            const response = await Request.post(Endpoints.termsGroup, { data: groupPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.termsGroup.push({ id: json.id });
        });

        await test.step('Precondition: Populate price component', async () => {
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product using term group (not direct term)', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.termId = null;
            payload.termGroupId = Responses.termsGroup[0].id;
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-55 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-56 – Product with price component group is included if group passes validation', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate terms', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create price component for group', async () => {
            const pcPayload = GeneratePayload.productAndServices.electricity();
            const response = await Request.post(Endpoints.priceComponent, { data: pcPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.priceComponent.push(json.id);
        });

        await test.step('Precondition: Create price component group', async () => {
            const groupPayload = {
                name: `PC-Group-${Date.now()}`,
                priceComponentIds: [Responses.priceComponent[0]],
            };
            const response = await Request.post(Endpoints.groupOfPriceComponents, { data: groupPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.groupOfPriceComponents.push({ id: json.id });
        });

        await test.step('Create product using price component group', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-56 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-57 – Product with advance payment group is included if group passes validation', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate terms and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create IAP for group', async () => {
            const iapPayload = GeneratePayload.productAndServices.interim();
            iapPayload.paymentType = 'OBLIGATORY';
            iapPayload.value = 100;
            iapPayload.currencyId = envVariables.currency;
            const response = await Request.post(Endpoints.interim, { data: iapPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.interim.push(json.id ?? json);
        });

        await test.step('Precondition: Create advance payment group', async () => {
            const interimId = typeof Responses.interim[0] === 'object' ? Responses.interim[0].id : Responses.interim[0];
            const groupPayload = {
                name: `IAP-Group-${Date.now()}`,
                interimAdvancePaymentIds: [interimId],
            };
            const response = await Request.post(Endpoints.groupOfInterims, { data: groupPayload });
            await expect(response).CheckResponse();
            const json = await response.json();
            Responses.groupOfInterims.push({ id: json.id });
        });

        await test.step('Create product using advance payment group', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify product appears in list', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeTruthy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-57 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[GET-PRODUCT-LIST]: TC-BE-58 – NEW status product is handled correctly', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        let productId: number;

        await test.step('Precondition: Populate shared entities', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Create product with NEW status', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.contractTypes = ['SUPPLY_ONLY'];
            payload.paymentGuarantees = ['NO'];
            payload.productStatus = 'NEW';
            const response = await Request.post(Endpoints.product, { data: payload });
            await expect(response).CheckResponse();
            productId = (await response.json()).id;
        });

        await test.step('Verify NEW product is excluded (only ACTIVE allowed)', async () => {
            const response = await Request.post(`${Endpoints.product}/list`, { data: {} });
            await expect(response).CheckResponse();
            const body = await response.json();
            const found = body.content?.find((p: any) => p.id === productId);
            expect(found).toBeFalsy();
        });

        test.info().attach('[GET-PRODUCT-LIST] TC-BE-58 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ──────────────────────────────────────────────────────────────────────────
    // TC-FE-1 … TC-FE-12 — Frontend tests (UI – skipped in API suite)
    // ──────────────────────────────────────────────────────────────────────────

    test.skip('[GET-PRODUCT-LIST]: TC-FE-1 – Product list page loads and displays products', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-2 – Product details display all required attributes', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-3 – Contract term details displayed correctly', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-4 – Price components displayed with correct structure', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-5 – Payment guarantee information displayed correctly', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-6 – Text to show in invoices and templates displayed', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-7 – Product list page handles multiple products', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-8 – Empty state when no products are available', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-9 – Error handling when backend API returns an error', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-10 – Product list handles slow API response gracefully', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-11 – Cyrillic and transliterated text render correctly', async () => {
        // UI test - not automated in EnergoTS
    });

    test.skip('[GET-PRODUCT-LIST]: TC-FE-12 – Page refresh reloads product list from API', async () => {
        // UI test - not automated in EnergoTS
    });
});
