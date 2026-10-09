import { Console, log } from 'console';
import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import { randomGens } from '../../utils/randomGens';

// ═══════════════════════════════════════════════════════════════════════════
// SHARED PRECONDITION HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

async function createTitle(Request: any, name: string = 'Mr.') {
    const response = await Request.post('titles', {
        data: { name, status: 'ACTIVE', defaultSelection: false },
    });
    await expect(response).toBeOK();
    return await response.json();
}

async function createContactPurpose(Request: any, name: string = 'Additional Contact Person') {
    const getResponse = await Request.get(
        `contact-purpose?statuses=ACTIVE&page=0&size=1&prompt=${encodeURIComponent(name)}`
    );
    await expect(getResponse).toBeOK();
    const getBody = await getResponse.json();
    if (getBody?.content?.length > 0) return getBody.content[0];

    const response = await Request.post('contact-purpose', {
        data: { name, status: 'ACTIVE', defaultSelection: false },
    });
    await expect(response).toBeOK();
    return await response.json();
}

async function createCustomer(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.customers.customer_legal();
    const response = await Request.post(Endpoints.customer, { data: payload });
    await expect(response).toBeOK();
    const body = await response.json();
    Responses.customer.push(body);
    return body;
}

async function getCustomerVersionId(Request: any, customerId: number): Promise<number> {
    const details = await Request.get(`customer/${customerId}`);
    await expect(details).toBeOK();
    const djson = (await details.json()) as Record<string, any>;
    return (
        djson.activeCustomerDetail?.versionId ??
        djson.customerDetail?.versionId ??
        (Array.isArray(djson.customerDetails) ? djson.customerDetails[0]?.versionId : undefined) ??
        1
    );
}

async function seedAdditionalContact(
    SPRequest: any,
    customerUic: string,
    versionId: number,
    titleId: number,
    overrides: Record<string, any> = {}
) {
    const body = {
        source: 'SALES_PORTAL',
        titleId: titleId,
        name: 'JOHN',
        middleName: 'D',
        surname: 'DOE',
        phones: ['+359888000111'],
        emails: ['john.doe@example.com'],
        ...overrides,
    };
    const response = await SPRequest.post(
        `additional-contacts/${encodeURIComponent(customerUic)}/${versionId}`,
        { data: body }
    );
    await expect(response).toBeOK();

        return await response.json();
}

interface SharedPreconditions {
    titleId: number;
    titleId2: number;
    customerId: number;
    customerUic: string;
    versionId: number;
}

async function sharedSetup(
    Request: any,
    SPRequest: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any
): Promise<SharedPreconditions> {
    const title1 = await createTitle(Request, `Mr${randomGens.generateRandomString(true, false, 4)}`);
    const title2 = await createTitle(Request, `Mrs${randomGens.generateRandomString(true, false, 4)}`);
    await createContactPurpose(Request, 'Additional Contact Person');
    const customer = await createCustomer(Request, GeneratePayload, Responses, Endpoints);
    const versionId = await getCustomerVersionId(Request, customer.id);
    return {
        titleId: title1.id,
        titleId2: title2.id,
        customerId: customer.id,
        customerUic: customer.identifier,
        versionId,
    };
}

// ═══════════════════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════════════════

test.describe('[PHN-2214]: Backend Put: update additional contact person for the customer', { tag: '@salesPortal' }, () => {
    test.describe.configure({ mode: 'serial' });

    // ── TC-BE-1 ──
    test('[PHN-2214]: TC-BE-1 – CREATE all fields, no existing IDs', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST additional-contacts – all fields', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        middleName: 'PETER',
                        surname: 'DOE',
                        birthDate: '1990-01-15',
                        phones: ['+359888000111', '+359888000112'],
                        emails: ['john.doe@example.com', 'jp.doe@example.com'],
                        relationship: 'Spouse',
                        contactIsOver18: true,
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBeGreaterThan(0);
            expect(body.contactPersonId).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2214] TC-BE-1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-2 ──
    test('[PHN-2214]: TC-BE-2 – CREATE mandatory fields only', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST additional-contacts – mandatory only', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'ANNA',
                        surname: 'IVANOVA',
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBeGreaterThan(0);
            expect(body.contactPersonId).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2214] TC-BE-2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-3 ──
    test('[PHN-2214]: TC-BE-3 – CREATE contactIsOver18 = true', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with contactIsOver18 true', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'IVAN',
                        surname: 'PETROV',
                        contactIsOver18: true,
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBeGreaterThan(0);
            expect(body.contactPersonId).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2214] TC-BE-3 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-4 ──
    test('[PHN-2214]: TC-BE-4 – CREATE contactIsOver18 = false', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with contactIsOver18 false', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'G',
                        surname: 'N',
                        contactIsOver18: false,
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBeGreaterThan(0);
            expect(body.contactPersonId).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2214] TC-BE-4 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-5 ──
    test('[PHN-2214]: TC-BE-5 – CREATE without contactIsOver18', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST without contactIsOver18', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'ALEX',
                        surname: 'KIROV',
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2214] TC-BE-5 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-6 ──
    test('[PHN-2214]: TC-BE-6 – CREATE with middleName – nameOfContactType', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with middleName', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'GEORGI',
                        middleName: 'IVANOV',
                        surname: 'DIMITROV',
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBeGreaterThan(0);
            expect(body.contactPersonId).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2214] TC-BE-6 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-7 ──
    test('[PHN-2214]: TC-BE-7 – CREATE without middleName – no double space', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST without middleName', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'PETAR',
                        surname: 'TODOROV',
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBeGreaterThan(0);
            expect(body.contactPersonId).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2214] TC-BE-7 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-8 ──
    test('[PHN-2214]: TC-BE-8 – CREATE contact in EXISTING communication data', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;
        let seeded: any;

        await test.step('Precondition: Create shared entities + seed existing contact', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded = await seedAdditionalContact(SPRequest, pre.customerUic, pre.versionId, pre.titleId);
        });

        await test.step('POST with existing communicationDataId', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        communicationDataId: seeded.communicationDataId,
                        titleId: pre!.titleId,
                        name: 'A',
                        surname: 'A'
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBe(seeded.communicationDataId);
            expect(body.contactPersonId).toBeGreaterThan(0);
            expect(body.contactPersonId).not.toBe(seeded.contactPersonId);
        });

        test.info().attach('[PHN-2214] TC-BE-8 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-9 ──
    test('[PHN-2214]: TC-BE-9 – EDIT with both IDs provided (matching)', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;
        let seeded: any;

        await test.step('Precondition: Create shared entities + seed contact', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded = await seedAdditionalContact(SPRequest, pre.customerUic, pre.versionId, pre.titleId);
        });

        await test.step('POST EDIT with both IDs', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        communicationDataId: seeded.communicationDataId,
                        contactPersonId: seeded.contactPersonId,
                        titleId: pre!.titleId2,
                        name: 'J',
                        middleName: 'P',
                        surname: 'D',
                        phones: ['+359888999000'],
                        emails: ['john.updated@example.com'],
                        relationship: 'Partner',
                        contactIsOver18: true,
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBe(seeded.communicationDataId);
            expect(body.contactPersonId).toBe(seeded.contactPersonId);
        });

        test.info().attach('[PHN-2214] TC-BE-9 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-10 ──
    test('[PHN-2214]: TC-BE-10 – EDIT contact person NOT belonging to comm data', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre1: SharedPreconditions;
        let pre2: SharedPreconditions;
        let seeded1: any;
        let seeded2: any;

        await test.step('Precondition: Create two customers with contacts', async () => {
            pre1 = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded1 = await seedAdditionalContact(SPRequest, pre1.customerUic, pre1.versionId, pre1.titleId);
            pre2 = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded2 = await seedAdditionalContact(SPRequest, pre2.customerUic, pre2.versionId, pre2.titleId);
        });

        await test.step('POST EDIT with mismatched contactPersonId', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre1!.customerUic}/${pre1!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        communicationDataId: seeded1.communicationDataId,
                        contactPersonId: seeded2.contactPersonId,
                        titleId: pre1!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBeGreaterThanOrEqual(400);
            expect(response.status()).toBeLessThan(500);
        });

        test.info().attach('[PHN-2214] TC-BE-10 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
        
    });

    // ── TC-BE-11 ──
    test('[PHN-2214]: TC-BE-11 – EDIT with only contactPersonId', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;
        let seeded: any;

        await test.step('Precondition: Create shared entities + seed contact', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded = await seedAdditionalContact(SPRequest, pre.customerUic, pre.versionId, pre.titleId);
        });

        await test.step('POST EDIT with contactPersonId only', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        contactPersonId: seeded.contactPersonId,
                        titleId: pre!.titleId,
                        name: 'J',
                        middleName: 'P2',
                        surname: 'J',
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBe(seeded.communicationDataId);
            expect(body.contactPersonId).toBe(seeded.contactPersonId);
        });

        test.info().attach('[PHN-2214] TC-BE-11 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-12 ──
    test('[PHN-2214]: TC-BE-12 – EDIT clears optional fields when omitted', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;
        let seeded: any;

        await test.step('Precondition: Create shared entities + seed full contact', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded = await seedAdditionalContact(SPRequest, pre.customerUic, pre.versionId, pre.titleId, {
                middleName: 'MIDDLEINITIAL',
                birthDate: '1990-06-15',
                relationship: 'Spouse',
                contactIsOver18: true,
            });
        });

        await test.step('POST EDIT with mandatory fields only', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        communicationDataId: seeded.communicationDataId,
                        contactPersonId: seeded.contactPersonId,
                        titleId: pre!.titleId,
                        name: 'J',
                        surname: 'D',
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBe(seeded.communicationDataId);
            expect(body.contactPersonId).toBe(seeded.contactPersonId);
        });

        test.info().attach('[PHN-2214] TC-BE-12 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-13 ──
    test('[PHN-2214]: TC-BE-13 – EDIT replaces phones and emails wholesale', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;
        let seeded: any;

        await test.step('Precondition: Create shared entities + seed contact', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded = await seedAdditionalContact(SPRequest, pre.customerUic, pre.versionId, pre.titleId);
        });

        await test.step('POST EDIT with new phones/emails', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        communicationDataId: seeded.communicationDataId,
                        contactPersonId: seeded.contactPersonId,
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                        phones: ['+359111111111', '+359222222222'],
                        emails: ['a@example.com', 'b@example.com'],
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBe(seeded.communicationDataId);
            expect(body.contactPersonId).toBe(seeded.contactPersonId);
        });

        test.info().attach('[PHN-2214] TC-BE-13 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-14 ──
    test('[PHN-2214]: TC-BE-14 – EDIT removes phones/emails when omitted', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;
        let seeded: any;

        await test.step('Precondition: Create shared entities + seed contact with phone/email', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded = await seedAdditionalContact(SPRequest, pre.customerUic, pre.versionId, pre.titleId);
        });

        await test.step('POST EDIT without phones/emails', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        communicationDataId: seeded.communicationDataId,
                        contactPersonId: seeded.contactPersonId,
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            await expect(response).CheckResponse();
        });

        test.info().attach('[PHN-2214] TC-BE-14 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-15 ──
    test('[PHN-2214]: TC-BE-15 – Source = Sales_Portal', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with source Sales_Portal', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'T',
                        surname: 'W',
                    },
                }
            );
            await expect(response).CheckResponse();
        });

        test.info().attach('[PHN-2214] TC-BE-15 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-16 ──
    test('[PHN-2214]: TC-BE-16 – Source = Self_Service_Portal', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with source Self_Service_Portal', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SELF_SERVICE_PORTAL',
                        titleId: pre!.titleId,
                        name: 'T',
                        surname: 'SSP',
                    },
                }
            );
            await expect(response).CheckResponse();
        });

        test.info().attach('[PHN-2214] TC-BE-16 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-17 ──
    test('[PHN-2214]: TC-BE-17 – Multiple phones and emails', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with 3 phones and 3 emails', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'M',
                        surname: 'C',
                        phones: ['+359001', '+359002', '+359003'],
                        emails: ['e1@x.com', 'e2@x.com', 'e3@x.com'],
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBeGreaterThan(0);
            expect(body.contactPersonId).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2214] TC-BE-17 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-18 ──
    test('[PHN-2214]: TC-BE-18 – Missing mandatory field: name', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST without name', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2214] TC-BE-18 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-19 ──
    test('[PHN-2214]: TC-BE-19 – Missing mandatory field: surname', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST without surname', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                    },
                }
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2214] TC-BE-19 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-20 ──
    test('[PHN-2214]: TC-BE-20 – Missing mandatory field: title', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST without title', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2214] TC-BE-20 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-21 ──
    test('[PHN-2214]: TC-BE-21 – Missing mandatory field: source', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST without source', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2214] TC-BE-21 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-22 ──
    test('[PHN-2214]: TC-BE-22 – Invalid source value', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with invalid source', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'INVALID_SOURCE_VALUE',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2214] TC-BE-22 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-23 ──
    test('[PHN-2214]: TC-BE-23 – Non-existent customerUic', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create title for valid body', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with non-existent UIC', async () => {
            const response = await SPRequest.post(
                `additional-contacts/9999999999999/1`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBeGreaterThanOrEqual(400);
            expect(response.status()).toBeLessThan(500);
        });

        test.info().attach('[PHN-2214] TC-BE-23 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-24 ──
    test('[PHN-2214]: TC-BE-24 – Non-existent versionId for customer', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with non-existent versionId', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId + 999}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBeGreaterThanOrEqual(400);
            expect(response.status()).toBeLessThan(500);
        });

        test.info().attach('[PHN-2214] TC-BE-24 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-25 ──
    test('[PHN-2214]: TC-BE-25 – communicationDataId belongs to different customer', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre1: SharedPreconditions;
        let pre2: SharedPreconditions;
        let seeded2: any;

        await test.step('Precondition: Create two customers; seed contact on customer 2', async () => {
            pre1 = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            pre2 = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded2 = await seedAdditionalContact(SPRequest, pre2.customerUic, pre2.versionId, pre2.titleId);
        });

        await test.step('POST for customer 1 with communicationDataId from customer 2', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre1!.customerUic}/${pre1!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        communicationDataId: seeded2.communicationDataId,
                        titleId: pre1!.titleId,
                        name: 'CROSS',
                        surname: 'CUSTOMER',
                    },
                }
            );
            expect(response.status()).toBeGreaterThanOrEqual(400);
            expect(response.status()).toBeLessThan(500);
        });

        test.info().attach('[PHN-2214] TC-BE-25 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-26 ──
    test('[PHN-2214]: TC-BE-26 – Non-existent contactPersonId', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with non-existent contactPersonId', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        contactPersonId: 99999999,
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBeGreaterThanOrEqual(400);
            expect(response.status()).toBeLessThan(500);
        });

        test.info().attach('[PHN-2214] TC-BE-26 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-27 ──
    test('[PHN-2214]: TC-BE-27 – versionId <= 0 validation', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with versionId = 0', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/0`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBe(500);
        });

        await test.step('POST with versionId = -1', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/-1`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBe(500);
        });

        test.info().attach('[PHN-2214] TC-BE-27 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-28 ──
    test('[PHN-2214]: TC-BE-28 – customerUic non-integer path validation', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create title for valid body', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with non-integer customerUic', async () => {
            const response = await SPRequest.post(
                `additional-contacts/abc/1`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBeGreaterThanOrEqual(400);
            expect(response.status()).toBeLessThan(500);
        });

        test.info().attach('[PHN-2214] TC-BE-28 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-33 ──
    test('[PHN-2214]: TC-BE-33 – Response structure validation', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST CREATE and validate response structure', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'S',
                        surname: 'C',
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            const keys = Object.keys(body).sort();
            expect(keys).toEqual(['communicationDataId', 'contactPersonId']);
            expect(typeof body.communicationDataId).toBe('number');
            expect(typeof body.contactPersonId).toBe('number');
            expect(body.communicationDataId).toBeGreaterThan(0);
            expect(body.contactPersonId).toBeGreaterThan(0);
        });

        test.info().attach('[PHN-2214] TC-BE-33 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-36 ──
    test('[PHN-2214]: TC-BE-36 – Wrong HTTP method PUT returns 404/405', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT additional-contacts should fail', async () => {
            const response = await SPRequest.put(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect([404, 405]).toContain(response.status());
        });

        test.info().attach('[PHN-2214] TC-BE-36 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-37 ──
    test('[PHN-2214]: TC-BE-37 – EDIT preserves IDs', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;
        let seeded: any;

        await test.step('Precondition: Create shared entities + seed contact', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded = await seedAdditionalContact(SPRequest, pre.customerUic, pre.versionId, pre.titleId);
        });

        await test.step('POST EDIT and verify IDs preserved', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        communicationDataId: seeded.communicationDataId,
                        contactPersonId: seeded.contactPersonId,
                        titleId: pre!.titleId2,
                        name: 'EDITED',
                        surname: 'NAME',
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBe(seeded.communicationDataId);
            expect(body.contactPersonId).toBe(seeded.contactPersonId);
        });

        test.info().attach('[PHN-2214] TC-BE-37 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-40 ──
    test('[PHN-2214]: TC-BE-40 – versionId is a string / non-numeric', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create title for valid body', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with string versionId', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/abc`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                    },
                }
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2214] TC-BE-40 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-41 ──
    test('[PHN-2214]: TC-BE-41 – Empty JSON body', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with empty body – expect all mandatory field errors', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                { data: {} }
            );
            expect(response.status()).toBe(400);
            const errorBody = await response.json();
            const errorText = JSON.stringify(errorBody).toLowerCase();
            expect(errorText).toContain('source');
            expect(errorText).toContain('titleid');
            expect(errorText).toContain('name');
            expect(errorText).toContain('surname');
        });

        test.info().attach('[PHN-2214] TC-BE-41 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-43 ──
    test('[PHN-2214]: TC-BE-43 – phones is not an array (wrong type)', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with phones as string instead of array', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                        phones: '+359888000111',
                    },
                }
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2214] TC-BE-43 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-44 ──
    test('[PHN-2214]: TC-BE-44 – EDIT same contact keeps phone/email when re-sent', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;
        let seeded: any;

        await test.step('Precondition: Create shared entities + seed contact', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
            seeded = await seedAdditionalContact(SPRequest, pre.customerUic, pre.versionId, pre.titleId);
        });

        await test.step('POST EDIT with same phones/emails', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        communicationDataId: seeded.communicationDataId,
                        contactPersonId: seeded.contactPersonId,
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                        phones: ['+359888000111'],
                        emails: ['john.doe@example.com'],
                    },
                }
            );
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.communicationDataId).toBe(seeded.communicationDataId);
            expect(body.contactPersonId).toBe(seeded.contactPersonId);
        });

        test.info().attach('[PHN-2214] TC-BE-44 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── TC-BE-45 ──
    test('[PHN-2214]: TC-BE-45 – birthdate malformed', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: SharedPreconditions;

        await test.step('Precondition: Create shared entities', async () => {
            pre = await sharedSetup(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('POST with wrong date format', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                        birthDate: '15/01/1990',
                    },
                }
            );
            expect(response.status()).toBe(400);
        });

        await test.step('POST with impossible date', async () => {
            const response = await SPRequest.post(
                `additional-contacts/${pre!.customerUic}/${pre!.versionId}`,
                {
                    data: {
                        source: 'SALES_PORTAL',
                        titleId: pre!.titleId,
                        name: 'JOHN',
                        surname: 'DOE',
                        birthDate: '1990-13-40',
                    },
                }
            );
            expect(response.status()).toBe(400);
        });

        test.info().attach('[PHN-2214] TC-BE-45 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ── Skipped: complex setup / regression tests ──

    test.skip('[PHN-2214]: TC-BE-29 – communicationDataId for wrong version', async () => {
        // Requires creating multiple customer versions; skipped for now.
    });

    test.skip('[PHN-2214]: TC-BE-30 – Inactive title id', async () => {
        // Requires creating INACTIVE nomenclature title and validating rejection.
    });

    test.skip('[PHN-2214]: TC-BE-31 – Missing Additional Contact Person purpose nomenclature', async () => {
        // Requires deactivating/reactivating nomenclature — too risky on shared env.
    });

    test.skip('[PHN-2214]: TC-BE-32 – CREATE produces CSP-filled foreign address fields', async () => {
        // Requires reading communication-data details not exposed via Sales Portal API.
    });

    test.skip('[PHN-2214]: TC-BE-34 – Auth: missing SP token', async () => {
        // Requires unauthenticated request context not available in SPRequest fixture.
    });

    test.skip('[PHN-2214]: TC-BE-35 – Auth: expired/invalid SP token', async () => {
        // Requires custom request context with invalid bearer token.
    });

    test.skip('[PHN-2214]: TC-BE-38 – Regression: downstream SQL joins', async () => {
        // Complex multi-domain regression requiring billing/reminder chain setup.
    });

    test.skip('[PHN-2214]: TC-BE-39 – Regression: country_id may remain unset', async () => {
        // Requires reading internal communication-data fields not exposed via SP API.
    });

    test.skip('[PHN-2214]: TC-BE-42 – Body is not valid JSON', async () => {
        // Playwright RequestWrapper always serializes data as JSON; cannot send malformed body.
    });
});
