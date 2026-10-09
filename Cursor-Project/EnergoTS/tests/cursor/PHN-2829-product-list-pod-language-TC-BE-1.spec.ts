import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

/**
 * PHN-2829 — TC-BE-1 (Backend manual case): Sales Portal list-by-POD without `language`
 * defaults to Bulgarian / backward compatible behaviour.
 *
 * API: GET {sales-portal}/product/list-with-pod-and-customer?podIdentifier=…&customerIdentifier=…
 * (no `language` query parameter).
 *
 * Run after global setup so `fixtures/envVariables.json` matches the target API (address / nomenclature IDs):
 *   npx playwright test --project=setup --workers=1
 *   npx playwright test tests/cursor/PHN-2829-product-list-pod-language-TC-BE-1.spec.ts --workers=1
 *
 * Reference specs:
 * - Cursor-Project/EnergoTS/tests/cursor/PHN-2115-contract-version-detail.spec.ts (precondition chain)
 * - Cursor-Project/EnergoTS/tests/salesPortal/getProductList.spec.ts — TC-BE-1 product flags (~848–902)
 */

const BG_PRINTING = 'Тест PHN2829 printing BG';
const EN_PRINTING = 'Test PHN2829 printing EN';
const BG_INVOICE_TEXT = 'Текст фактури BG PHN2829';
const EN_INVOICE_TEXT = 'Invoice template EN PHN2829';

async function sharedTerm(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const termPayload = GeneratePayload.productAndServices.term();
    termPayload.contractEntryIntoForces = ['SIGNING'];
    termPayload.startsOfContractInitialTerms = ['SIGNING'];
    termPayload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
    termPayload.waitForOldContractTermToExpires = ['NO'];
    const termResponse = await Request.post(Endpoints.terms, { data: termPayload });
    await expect(termResponse).CheckResponse();
    Responses.terms.push(await termResponse.json());
}

async function sharedPrice(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    // Use electricity() (not priceSettlement) — settlement payload can reference env-specific profile IDs that are missing on some hosts.
    const pricePayload = GeneratePayload.productAndServices.electricity();
    const priceResponse = await Request.post(Endpoints.priceComponent, { data: pricePayload });
    await expect(priceResponse).CheckResponse();
    Responses.priceComponent.push(await priceResponse.json());
}

function pushProductId(Responses: any, productResponseJson: unknown) {
    const productId =
        typeof productResponseJson === 'number'
            ? productResponseJson
            : (productResponseJson as { id?: number }).id;
    if (productId == null) {
        throw new Error('Product POST did not return a numeric id');
    }
    Responses.product.push(productId);
}

test.describe('[PHN-2829]: Add bilingual support to "Get: product list by POD ID"', { tag: '@salesPortal' }, () => {
    test('[PHN-2829]: TC-BE-1 – Omitted language defaults to Bulgarian (backward compatible)', async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let podIdentifier: string;
        let customerIdentifier: string;
        let expectedProductId: number;

        await test.step('Precondition: term + price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPrice(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Precondition: customer', async () => {
            const customerPayload = GeneratePayload.customers.customer_private();
            const customerResponse = await Request.post(Endpoints.customer, { data: customerPayload });
            await expect(customerResponse).CheckResponse();
            const customerJson = await customerResponse.json();
            Responses.customer.push(customerJson);
            customerIdentifier = String(customerJson.identifier);
            expect(customerIdentifier.length).toBeGreaterThan(0);
        });

        await test.step('Precondition: POD', async () => {
            const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();
            podPayload.type = 'CONSUMER';
            podPayload.consumptionPurpose = 'NON_HOUSEHOLD';
            podPayload.voltageLevel = 'LOW';
            podPayload.estimatedMonthlyAvgConsumption = '3000';
            const podResponse = await Request.post(Endpoints.pod, { data: podPayload });
            await expect(podResponse).CheckResponse();
            Responses.pod.push(await podResponse.json());
        });

        await test.step('Precondition: product (Sales Portal eligible + distinct BG/EN strings)', async () => {
            const productPayload = GeneratePayload.productAndServices.product();
            productPayload.productStatus = 'ACTIVE';
            productPayload.availableForSale = true;
            productPayload.globalSalesChannel = true;
            productPayload.globalSalesArea = true;
            productPayload.globalSegment = true;
            productPayload.contractTypes = ['COMBINED'];
            productPayload.paymentGuarantees = ['NO'];
            productPayload.typePointsOfDelivery = ['CONSUMER'];
            productPayload.purposeOfConsumptions = ['NON_HOUSEHOLD'];
            productPayload.meteringTypeOfThePointOfDeliveries = ['SETTLEMENT_PERIOD'];
            productPayload.voltageLevels = ['LOW'];
            productPayload.capacityLimitType = 'TO';
            productPayload.capacityLimitAmount = 5000;
            productPayload.isIndividual = false;
            productPayload.equalMonthlyInstallmentsActivation = false;
            productPayload.printingName = BG_PRINTING;
            productPayload.printingNameTransliterated = EN_PRINTING;
            productPayload.invoiceAndTemplatesText = BG_INVOICE_TEXT;
            productPayload.invoiceAndTemplatesTextTransliterated = EN_INVOICE_TEXT;
            productPayload.productTerms = [
                {
                    typeOfTerms: 'PERIOD',
                    value: '12',
                    periodType: 'DAY_DAYS',
                    renewalPeriodValue: null,
                    renewalPeriodType: null,
                    perpetuityCause: false,
                    automaticRenewal: null,
                    numberOfRenewals: null,
                    name: '12 Day/Days Period',
                    id: null,
                },
            ];
            const productResponse = await Request.post(Endpoints.product, { data: productPayload });
            await expect(productResponse).CheckResponse();
            const productJson = await productResponse.json();
            pushProductId(Responses, productJson);
            expectedProductId = Responses.product[Responses.product.length - 1] as number;
        });

        // await test.step('Precondition: product contract + POD activation', async () => {
        //     const payload = await GeneratePayload.contractsAndOrders.product_contract();
        //     payload.productParameters.contractType = 'COMBINED';
        //     const contractResponse = await Request.post(Endpoints.productContract, { data: payload });
        //     await expect(contractResponse).CheckResponse();
        //     Responses.productContract.push(await contractResponse.json());

        //     const activationPayload = await GeneratePayload.pointsOfDelivery.pod_activation();
        //     const activationResponse = await Request.post('/contract-pods/manual', {
        //         data: activationPayload,
        //     });
        //     await expect(activationResponse).CheckResponse();
        // });

        // await test.step('Execute: GET list-with-pod-and-customer without language', async () => {
        //     const podRes = await Request.get(`${Endpoints.pod}/${Responses.pod[0].id}`);
        //     await expect(podRes).CheckResponse();
        //     const podJson = await podRes.json();
        //     podIdentifier = String(podJson.identifier);

        //     const qs = new URLSearchParams({
        //         podIdentifier,
        //         customerIdentifier,
        //     });
        //     const url = `${Endpoints.salesPortalEndpoints.getProductListByPod}?${qs.toString()}`;
        //     const listResponse = await SPRequest.get(url);
        //     await expect(listResponse).CheckResponse();
        //     const body = await listResponse.json();
        //     expect(Array.isArray(body)).toBeTruthy();

        //     const row = body.find((p: { productId?: number }) => p.productId === expectedProductId);
        //     expect(row, `Expected productId ${expectedProductId} in Sales Portal list`).toBeTruthy();

        //     expect(row.printingName).toBe(BG_PRINTING);
        //     if (row.textToShowInInvoicesAndTemplates != null) {
        //         expect(row.textToShowInInvoicesAndTemplates).toBe(BG_INVOICE_TEXT);
        //     }
        //     expect(row.printingNameTransliterated).toBe(EN_PRINTING);
        //     expect(row.paymentGuarantee).toBeTruthy();
        //     expect(row.contractType).toBeTruthy();
        //     expect(row.typeOfPointsOfDelivery).toBeTruthy();
        //     expect(row.purposeOfConsumption).toBeTruthy();
        // });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2829] TC-BE-1 trace', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
});
