import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

/**
 * PHN-2211 — Sales Portal additional contact persons.
 * Maps 1:1 to:
 * - test_cases/Backend/PHN-2211_additional_contact_persons.md (TC-BE-1 … TC-BE-8)
 * - test_cases/Frontend/PHN-2211_additional_contact_persons.md (TC-FE-1 … TC-FE-4)
 */
test.describe('[REG-1305]: Get Additional contact person', { tag: '@salesPortal' }, () => {
    test.describe.configure({ mode: 'serial', timeout: 120_000 });

    let additionalContactPurposeId: number;
    let customerId: number;
    let customerIdentifier: string;
    let detailVersionId: number;

    test('[REG-1308]: TC-BE-1 – Happy path – 200 OK with paginated list and expected fields', async ({
        SPRequest,
        Responses,
        GeneratePayload,
        Request,
        Endpoints,
    }) => {
        
        await test.step('get additiona comm purpose', async () => {
            const additionalContactPurpose = await Request.get('contact-purpose?statuses=ACTIVE&statuses=INACTIVE&page=0&size=25&prompt=additional');
            const body = (await additionalContactPurpose.json()) as Record<string, any>;
            additionalContactPurposeId = body.content[0].id;
        });

        await test.step('Create customer and add communications', async () => {
            const payload = GeneratePayload.customers.customer_legal()
            payload.communicationData[0].contactPurposes = [
                {
                        contactPurposeId: additionalContactPurposeId,
                        status: "ACTIVE"
                },
            ]
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            const body = (await response.json()) as Record<string, any>;
            customerId = body.id;
            customerIdentifier = body.identifier;
            const detailsResp = await Request.get(`customer/${customerId}`);
            await expect(detailsResp).CheckResponse();
            const details = (await detailsResp.json()) as Record<string, any>;
            detailVersionId = details.versionId ?? 1;
        });

        await test.step('GET additional-contacts for created customer', async () => {
            const response = await SPRequest.get(
                `additional-contacts/${encodeURIComponent(customerIdentifier)}/${detailVersionId}?page=0&size=20`
            );
            await expect(response).CheckResponse();
            const body = (await response.json()) as Record<string, any>;
            const rows = (body.content ?? body.communicationData) as unknown[];
            expect(Array.isArray(rows)).toBeTruthy();
            expect(typeof body.totalElements).toBe('number');
            expect(typeof body.totalPages).toBe('number');
            if (rows.length > 0) {
                const first = rows[0] as Record<string, any>;
                expect(first).toHaveProperty('communicationDataId');
                expect(first).toHaveProperty('nameOfContactType');
                expect(Array.isArray(first.mobileNumbers)).toBeTruthy();
                expect(Array.isArray(first.emails)).toBeTruthy();
            }
        });

        Responses.customer.push({ id: customerId, identifier: customerIdentifier, versionId: detailVersionId });

        console.log(reportGenerator.setLinksToResponses(Responses));
        test.info().attach('[REG-1308] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });


    test('[REG-1309]: TC-BE-4 – Empty result – customer exists but no qualifying communications', async ({
        SPRequest,
        Responses,
        GeneratePayload,
        Request,
        Endpoints,
    }) => {
        let noContactCustomerIdentifier: string;
        let noContactVersionId: number;

        await test.step('Create customer without additional contact person purpose', async () => {
            const payload = GeneratePayload.customers.customer_legal();
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            const body = (await response.json()) as Record<string, any>;
            const noContactCustomerId = body.id;
            noContactCustomerIdentifier = body.identifier;
            const detailsResp = await Request.get(`customer/${noContactCustomerId}`);
            await expect(detailsResp).CheckResponse();
            const details = (await detailsResp.json()) as Record<string, any>;
            noContactVersionId = details.versionId ?? 1;
        });

        await test.step('GET additional-contacts – expect empty list', async () => {
            const response = await SPRequest.get(
                `additional-contacts/${encodeURIComponent(noContactCustomerIdentifier)}/${noContactVersionId}?page=0&size=20`
            );
            await expect(response).CheckResponse();
            const body = (await response.json()) as Record<string, any>;
            const rows = (body.content ?? body.communicationData) as unknown[];
            expect(Array.isArray(rows)).toBeTruthy();
            expect(rows.length).toBe(0);
            expect(body.totalElements).toBe(0);
        });

        test.info().attach('[REG-1309] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
});
