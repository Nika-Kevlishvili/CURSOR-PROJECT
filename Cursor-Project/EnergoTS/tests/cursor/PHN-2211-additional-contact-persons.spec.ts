import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

/**
 * PHN-2211 — Sales Portal additional contact persons.
 * Maps 1:1 to:
 * - test_cases/Backend/PHN-2211_additional_contact_persons.md (TC-BE-1 … TC-BE-8)
 * - test_cases/Frontend/PHN-2211_additional_contact_persons.md (TC-FE-1 … TC-FE-4)
 */
test.describe('[PHN-2211]: Get Additional contact person', { tag: '@salesPortal' }, () => {
    test.describe.configure({ mode: 'serial' });

    let customerIdentifier: string;
    let customerId: number;
    let detailVersionId: number;

    test.beforeAll(async ({ Request, GeneratePayload, Endpoints }) => {
        const create = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
        await expect(create).CheckResponse();
        const created = await create.json();
        customerIdentifier = created.identifier;
        customerId = created.id;
        const details = await Request.get(`/customer/${customerId}`);
        await expect(details).CheckResponse();
        const djson = (await details.json()) as Record<string, any>;
        detailVersionId =
            djson.activeCustomerDetail?.versionId ??
            djson.customerDetail?.versionId ??
            (Array.isArray(djson.customerDetails) ? djson.customerDetails[0]?.versionId : undefined) ??
            1;
    });

    test('[PHN-2211]: TC-BE-1 – Happy path – 200 OK with paginated list and expected fields', async ({
        SPRequest,
        Responses,
    }) => {
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
        test.info().attach('[PHN-2211] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[PHN-2211]: TC-BE-2 – Pagination – second page respects page and size', async () => {
        // Not automated: requires three or more Additional Contact Person–only communication rows.
    });

    test.skip('[PHN-2211]: TC-BE-3 – Sorting – rows ordered by modification date descending', async () => {
        // Not automated: requires controlled modify_date on multiple qualifying rows.
    });

    test('[PHN-2211]: TC-BE-4 – Empty result – customer exists but no qualifying communications', async ({
        SPRequest,
        Responses,
    }) => {
        await test.step('GET additional-contacts (may be empty if no purpose-only rows)', async () => {
            const response = await SPRequest.get(
                `additional-contacts/${encodeURIComponent(customerIdentifier)}/${detailVersionId}?page=0&size=20`
            );
            await expect(response).CheckResponse();
            const body = (await response.json()) as Record<string, any>;
            const rows = (body.content ?? body.communicationData) as unknown[];
            expect(Array.isArray(rows)).toBeTruthy();
            expect(body.totalElements).toBeGreaterThanOrEqual(0);
        });

        test.info().attach('[PHN-2211] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2211]: TC-BE-5 – Not found – unknown UIC or wrong version returns 404', async ({ SPRequest, Responses }) => {
        await test.step('GET with non-existent customer identifier', async () => {
            const response = await SPRequest.get(
                `additional-contacts/${encodeURIComponent('NONEXISTENT_UIC_PHN_2211')}/1?page=0&size=20`
            );
            expect(response.status()).toBe(404);
        });

        test.info().attach('[PHN-2211] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2211]: TC-BE-6 – Validation – versionId less than 1 returns 400', async ({ SPRequest, Responses }) => {
        await test.step('GET with versionId path 0', async () => {
            const response = await SPRequest.get(
                `additional-contacts/${encodeURIComponent(customerIdentifier)}/0?page=0&size=20`
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2211] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2211]: TC-BE-7 – Validation – page below 0 returns 400', async ({ SPRequest, Responses }) => {
        await test.step('GET with page=-1', async () => {
            const response = await SPRequest.get(
                `additional-contacts/${encodeURIComponent(customerIdentifier)}/${detailVersionId}?page=-1&size=20`
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2211] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2211]: TC-BE-8 – Validation – size outside 1..100 returns 400', async ({ SPRequest, Responses }) => {
        await test.step('GET with size=0', async () => {
            const r0 = await SPRequest.get(
                `additional-contacts/${encodeURIComponent(customerIdentifier)}/${detailVersionId}?page=0&size=0`
            );
            expect(r0.status()).toBe(400);
        });
        await test.step('GET with size=101', async () => {
            const r101 = await SPRequest.get(
                `additional-contacts/${encodeURIComponent(customerIdentifier)}/${detailVersionId}?page=0&size=101`
            );
            expect(r101.status()).toBe(400);
        });

        test.info().attach('[PHN-2211] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[PHN-2211]: TC-FE-1 – Customer portal lists additional contacts', async () => {
        // Customer portal UI is not automated in EnergoTS (API-first suite).
    });

    test.skip('[PHN-2211]: TC-FE-2 – Customer portal – no cross-customer contact disclosure', async () => {
        // Portal UI / auth scenario not automated in EnergoTS.
    });

    test.skip('[PHN-2211]: TC-FE-3 – Sales portal – representative sees additional contacts', async () => {
        // Sales portal UI is not automated in EnergoTS (use SPRequest API tests).
    });

    test.skip('[PHN-2211]: TC-FE-4 – Sales portal – wrong version id does not leak data', async () => {
        // Portal UI version handling not automated in EnergoTS.
    });
});
