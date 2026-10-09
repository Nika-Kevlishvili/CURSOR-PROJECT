import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

/**
 * PHN-2115 — GET Sales Portal product contract version detail (happy path only).
 * Backend: GET /sales-portal/product-contract/{id}/version/{versionID}?language=BG
 * (SPRequest base URL already includes /sales-portal/)
 *
 * Run with MAIN API base matching Sales Portal OAuth (fixtures/salesPortalLogin.ts uses phoenix-dev2):
 * `BASE_URL=https://devapps.energo-pro.bg/backend/phoenix-dev2 npx playwright test --project=setup --workers=1`
 * then `npx playwright test tests/cursor/PHN-2115-contract-version-detail.spec.ts --workers=1`
 *
 * Experiments host: `BASE_URL=http://10.236.20.81:8094` — Sales Portal OAuth uses the same host (`salesPortalLogin.ts`).
 */

async function sharedTerm(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const termPayload = GeneratePayload.productAndServices.term();
    const termResponse = await Request.post(Endpoints.terms, { data: termPayload });
    await expect(termResponse).CheckResponse();
    Responses.terms.push(await termResponse.json());
}

async function sharedPrice(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const pricePayload = GeneratePayload.productAndServices.electricity();
    const priceResponse = await Request.post(Endpoints.priceComponent, { data: pricePayload });
    await expect(priceResponse).CheckResponse();
    Responses.priceComponent.push(await priceResponse.json());
}

test.describe('[PHN-2115]: Sales Portal contract version detail', { tag: '@salesPortal' }, () => {
    test('[PHN-2115]: Happy path | GET contract version detail returns composite payload', async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let contractId: number;
        let versionId: number;

        await test.step('Precondition: Create term and price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: Create customer', async () => {
            const customerPayload = GeneratePayload.customers.customer_private();
            const customerResponse = await Request.post(Endpoints.customer, { data: customerPayload });
            await expect(customerResponse).CheckResponse();
            Responses.customer.push(await customerResponse.json());
        });

        await test.step('Precondition: Create POD', async () => {
            const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();
            const podResponse = await Request.post(Endpoints.pod, { data: podPayload });
            await expect(podResponse).CheckResponse();
            Responses.pod.push(await podResponse.json());
        });

        await test.step('Precondition: Create product', async () => {
            const productPayload = GeneratePayload.productAndServices.product();
            productPayload.contractTypes = ['SUPPLY_ONLY'];
            productPayload.paymentGuarantees = ['NO'];
            const productResponse = await Request.post(Endpoints.product, { data: productPayload });
            await expect(productResponse).CheckResponse();
            Responses.product.push(await productResponse.json());
        });

        await test.step('Precondition: Create signed product contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.productParameters.contractType = 'SUPPLY_ONLY';
            const contractResponse = await Request.post(Endpoints.productContract, { data: payload });
            await expect(contractResponse).CheckResponse();
            const body = await contractResponse.json();
            Responses.productContract.push(body);
            if (typeof body === 'number') {
                contractId = body;
                versionId = 1;
            } else {
                contractId = body.id;
                versionId = body.versionId ?? 1;
            }
            expect(contractId).toBeTruthy();
            expect(versionId).toBeTruthy();
        });

        await test.step('Precondition: Activate POD on contract', async () => {
            const activationPayload = await GeneratePayload.pointsOfDelivery.pod_activation();
            const activationResponse = await Request.post('/contract-pods/manual', {
                data: activationPayload,
            });
            await expect(activationResponse).CheckResponse();
        });

        await test.step('Execute: GET Sales Portal contract version detail (BG)', async () => {
            const url = `product-contract/${contractId}/version/${versionId}?language=BG`;
            const detailResponse = await SPRequest.get(url);
            await expect(detailResponse).CheckResponse();
            const detail = await detailResponse.json();

            expect(detail.contractDeliveryMethod).toBeTruthy();
            expect(detail.contractDeliveryMethod).toHaveProperty('method');

            expect(detail.contractPaymentMethod).toBeTruthy();
            expect(typeof detail.contractPaymentMethod.directDebit).toBe('boolean');

            expect(detail.contractProduct).toBeTruthy();
            expect(detail.contractProduct.id).toBeDefined();

            expect(Array.isArray(detail.pods)).toBeTruthy();
            expect(detail.pods.length).toBeGreaterThanOrEqual(1);
            expect(detail.pods[0].podNumber).toBeTruthy();

            expect(detail.customer).toBeTruthy();
            expect(detail.customer.name || detail.customer.uicPn).toBeTruthy();

            expect(detail.customerBusinessContactPerson).toBeTruthy();

            expect(Array.isArray(detail.documents)).toBeTruthy();
        });

        test.info().attach('[PHN-2115] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
});
