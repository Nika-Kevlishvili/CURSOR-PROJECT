import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import { randomGens } from '../../utils/randomGens';
import { ExtendedAPIResponse } from '../../utils/RequestWrapper';

// ═══════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS & TYPES
// ═══════════════════════════════════════════════════════════════════════════

const SP_CUSTOMER_ENDPOINT = 'customer';

async function createIndividualCustomer(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.customers.customer_private();
    const response = await Request.post(Endpoints.customer, { data: payload });
    await expect(response).toBeOK();
    const body = await response.json();
    Responses.customer.push(body);
    return body;
}

async function createLegalCustomer(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.customers.customer_legal();
    const response = await Request.post(Endpoints.customer, { data: payload });
    await expect(response).toBeOK();
    const body = await response.json();
    Responses.customer.push(body);
    return body;
}

async function getCustomerDetails(Request: any, customerId: number): Promise<any> {
    const response = await Request.get(`customer/${customerId}`);
    await expect(response).toBeOK();
    return await response.json();
}

async function getCustomerVersionId(Request: any, customerId: number): Promise<number> {
    const details = await getCustomerDetails(Request, customerId);
    return details.versionId ?? 1;
}

async function getCommunicationDataId(Request: any, customerId: number): Promise<number | null> {
    const details = await getCustomerDetails(Request, customerId);
    const commData = details.communicationData ?? [];
    return commData.length > 0 ? commData[0].id : null;
}

async function getManagerId(Request: any, customerId: number): Promise<number | null> {
    const details = await getCustomerDetails(Request, customerId);
    const managers = details.managers ?? [];
    return managers.length > 0 ? managers[0].id : null;
}

async function getOwnerId(Request: any, customerId: number): Promise<number | null> {
    const details = await getCustomerDetails(Request, customerId);
    const owners = details.owner ?? [];
    return owners.length > 0 ? owners[0].id : null;
}

async function findActiveBank(Request: any): Promise<{ id: number; bic: string }> {
    const response = await Request.get('banks?statuses=ACTIVE&page=0&size=1');
    await expect(response).toBeOK();
    const body = await response.json();
    if (body?.content?.length > 0) {
        return { id: body.content[0].id, bic: body.content[0].bic ?? 'BNBGBGSF' };
    }
    const createResp = await Request.post('banks', {
        data: { name: `TestBank_${randomGens.generateRandomString(true, false, 4)}`, bic: 'TSTBBGSF', status: 'ACTIVE' },
    });
    await expect(createResp).toBeOK();
    const created = await createResp.json();
    return { id: created.id, bic: created.bic ?? 'TSTBBGSF' };
}

async function findActiveNomenclature(Request: any, endpoint: string): Promise<number> {
    const response = await Request.get(`${endpoint}?statuses=ACTIVE&page=0&size=1`);
    await expect(response).toBeOK();
    const body = await response.json();
    if (body?.content?.length > 0) return body.content[0].id;
    const createResp = await Request.post(endpoint, {
        data: { name: `Test_${randomGens.generateRandomString(true, false, 4)}`, status: 'ACTIVE', defaultSelection: false },
    });
    await expect(createResp).toBeOK();
    return (await createResp.json()).id;
}

interface IndividualPreconditions {
    customerId: number;
    customerVersionId: number;
    customerPnOrUic: string;
    countryId: number;
    populatedPlaceId: number;
    zipCodeId: number;
}

interface LegalPreconditions {
    customerId: number;
    customerVersionId: number;
    customerPnOrUic: string;
    communicationDataId: number;
    managerId: number;
    ownerId: number;
    titleId: number;
    representationMethodId: number;
    belongingCapitalOwnerId: number;
    bankId: number;
    bankBic: string;
    ownerCustomerIdentifier: string;
}

async function setupIndividual(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any
): Promise<IndividualPreconditions> {
    const customer = await createIndividualCustomer(Request, GeneratePayload, Responses, Endpoints);
    const versionId = await getCustomerVersionId(Request, customer.id);
    const countryId = await findActiveNomenclature(Request, 'countries');
    const populatedPlaceId = await findActiveNomenclature(Request, 'populated-places');
    const zipCodeId = await findActiveNomenclature(Request, 'zip-codes');
    return {
        customerId: customer.id,
        customerVersionId: versionId,
        customerPnOrUic: customer.identifier,
        countryId,
        populatedPlaceId,
        zipCodeId,
    };
}

async function setupLegal(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any
): Promise<LegalPreconditions> {
    const titleId = await findActiveNomenclature(Request, 'titles');
    const representationMethodId = await findActiveNomenclature(Request, 'representation-methods');
    const belongingCapitalOwnerId = await findActiveNomenclature(Request, 'belonging-capital-owners');
    const bank = await findActiveBank(Request);

    // Create an individual customer to use as owner (not tracked in Responses)
    const ownerPayload = GeneratePayload.customers.customer_private();
    const ownerResp = await Request.post(Endpoints.customer, { data: ownerPayload });
    await expect(ownerResp).toBeOK();
    const ownerCustomer = await ownerResp.json();

    // Include an owner in the creation payload so the customer has an owner ID to reference
    const payload = GeneratePayload.customers.customer_legal();
    payload.owner = [{ personalNumber: ownerCustomer.identifier, belongingOwnerCapitalId: belongingCapitalOwnerId }];
    const customerResp = await Request.post(Endpoints.customer, { data: payload });
    await expect(customerResp).toBeOK();
    const customer = await customerResp.json();
    Responses.customer.push(customer);

    const versionId = await getCustomerVersionId(Request, customer.id);
    const commId = await getCommunicationDataId(Request, customer.id);
    const managerId = await getManagerId(Request, customer.id);
    const ownerId = await getOwnerId(Request, customer.id);
    return {
        customerId: customer.id,
        customerVersionId: versionId,
        customerPnOrUic: customer.identifier,
        communicationDataId: commId ?? 0,
        managerId: managerId ?? 0,
        ownerId: ownerId ?? 0,
        titleId,
        representationMethodId,
        belongingCapitalOwnerId,
        bankId: bank.id,
        bankBic: bank.bic,
        ownerCustomerIdentifier: ownerCustomer.identifier,
    };
}

// ═══════════════════════════════════════════════════════════════════════════
// PRIVATE CUSTOMER WITH BUSINESS ACTIVITY (businessActivity = true)
// ═══════════════════════════════════════════════════════════════════════════
//
// A PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY is created with both individual
// details AND business details (communicationData, managers, owners, banking,
// economic branches). However, PUT /sales-portal/customer routes it through
// processIndividual() — only name, surname, address, KYC are processed.
// Business-specific fields are SILENTLY IGNORED (logged as cross-type).

async function createPrivateBusinessCustomer(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any
) {
    const payload = GeneratePayload.customers.customer_private_business();
    const response = await Request.post(Endpoints.customer, { data: payload });
    await expect(response).toBeOK();
    const body = await response.json();
    Responses.customer.push(body);
    return body;
}

interface PrivateBusinessPreconditions {
    customerId: number;
    customerVersionId: number;
    customerPnOrUic: string;
    countryId: number;
    populatedPlaceId: number;
    zipCodeId: number;
}

async function setupPrivateBusiness(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any
): Promise<PrivateBusinessPreconditions> {
    const customer = await createPrivateBusinessCustomer(Request, GeneratePayload, Responses, Endpoints);
    const versionId = await getCustomerVersionId(Request, customer.id);
    const countryId = await findActiveNomenclature(Request, 'countries');
    const populatedPlaceId = await findActiveNomenclature(Request, 'populated-places');
    const zipCodeId = await findActiveNomenclature(Request, 'zip-codes');
    return {
        customerId: customer.id,
        customerVersionId: versionId,
        customerPnOrUic: customer.identifier,
        countryId,
        populatedPlaceId,
        zipCodeId,
    };
}

// ═══════════════════════════════════════════════════════════════════════════
// FULL-ADDRESS HELPERS (for TC-BE-58 / TC-BE-59)
// ═══════════════════════════════════════════════════════════════════════════
//
// Expectation: optional address fields (districtId, residentialAreaId, streetId,
// streetNumber, block, entrance, floor, apartment) should NOT be cleared
// when they are absent/null in the PUT payload.

async function createIndividualWithFullAddress(
    Request: any, SPRequest: any, GeneratePayload: any, Responses: any, Endpoints: any
): Promise<{ customerId: number; customerVersionId: number; customerPnOrUic: string }> {
    const customer = await createIndividualCustomer(Request, GeneratePayload, Responses, Endpoints);
    const versionId = await getCustomerVersionId(Request, customer.id);
    const countryId = await findActiveNomenclature(Request, 'countries');
    const populatedPlaceId = await findActiveNomenclature(Request, 'populated-places');
    const zipCodeId = await findActiveNomenclature(Request, 'zip-codes');

    const fullAddressPayload = {
        customerId: customer.id,
        customerVersionId: versionId,
        customerPnOrUic: customer.identifier,
        name: 'ИВАН',
        surname: 'ПЕТРОВ',
        registered: true,
        address: {
            countryId,
            populatedPlaceId,
            zipCodeId,
            streetNumber: '15A',
            block: '7',
            entrance: 'B',
            floor: '3',
            apartment: '12',
        },
    };
    const initResp: ExtendedAPIResponse = await SPRequest.put(SP_CUSTOMER_ENDPOINT, { data: fullAddressPayload });
    await expect(initResp).CheckResponse();

    const freshVersion = await getCustomerVersionId(Request, customer.id);
    return { customerId: customer.id, customerVersionId: freshVersion, customerPnOrUic: customer.identifier };
}

async function createPrivateBusinessWithFullAddress(
    Request: any, SPRequest: any, GeneratePayload: any, Responses: any, Endpoints: any
): Promise<{ customerId: number; customerVersionId: number; customerPnOrUic: string }> {
    const customer = await createPrivateBusinessCustomer(Request, GeneratePayload, Responses, Endpoints);
    const versionId = await getCustomerVersionId(Request, customer.id);
    const countryId = await findActiveNomenclature(Request, 'countries');
    const populatedPlaceId = await findActiveNomenclature(Request, 'populated-places');
    const zipCodeId = await findActiveNomenclature(Request, 'zip-codes');

    const fullAddressPayload = {
        customerId: customer.id,
        customerVersionId: versionId,
        customerPnOrUic: customer.identifier,
        name: 'ГЕОРГИ',
        surname: 'СТОЯНОВ',
        registered: true,
        address: {
            countryId,
            populatedPlaceId,
            zipCodeId,
            streetNumber: '22',
            block: '3',
            entrance: 'A',
            floor: '5',
            apartment: '8',
        },
    };
    const initResp: ExtendedAPIResponse = await SPRequest.put(SP_CUSTOMER_ENDPOINT, { data: fullAddressPayload });
    await expect(initResp).CheckResponse();

    const freshVersion = await getCustomerVersionId(Request, customer.id);
    return { customerId: customer.id, customerVersionId: freshVersion, customerPnOrUic: customer.identifier };
}

// ═══════════════════════════════════════════════════════════════════════════
// LEGAL ENTITY WITH MULTIPLE MANAGERS, OWNERS, COMMUNICATION DATA
// Partial update via sales portal – untouched entries must be preserved
// ═══════════════════════════════════════════════════════════════════════════

interface LegalMultiplePreconditions {
    customerId: number;
    customerVersionId: number;
    customerPnOrUic: string;
    communicationDataId1: number;
    communicationDataId2: number;
    managerId1: number;
    managerId2: number;
    ownerId1: number;
    ownerId2: number;
    ownerCustomerIdentifier1: string;
    ownerCustomerIdentifier2: string;
    titleId: number;
    representationMethodId: number;
    belongingCapitalOwnerId: number;
}

async function setupLegalWithMultiple(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any
): Promise<LegalMultiplePreconditions> {
    const titleId = await findActiveNomenclature(Request, 'titles');
    const representationMethodId = await findActiveNomenclature(Request, 'representation-methods');
    const belongingCapitalOwnerId = await findActiveNomenclature(Request, 'belonging-capital-owners');

    // Create two individual customers to serve as owners
    const owner1Payload = GeneratePayload.customers.customer_private();
    const owner1Resp = await Request.post(Endpoints.customer, { data: owner1Payload });
    await expect(owner1Resp).toBeOK();
    const owner1 = await owner1Resp.json();

    const owner2Payload = GeneratePayload.customers.customer_private();
    const owner2Resp = await Request.post(Endpoints.customer, { data: owner2Payload });
    await expect(owner2Resp).toBeOK();
    const owner2 = await owner2Resp.json();

    // Build legal entity payload with 2 managers, 2 communication data entries, and 2 owners
    const payload = GeneratePayload.customers.customer_legal();

    payload.managers = [
        {
            ...payload.managers[0],
            id: null,
            name: 'ИВАН',
            surname: 'ПЕТРОВ',
            jobPosition: 'CEO',
            titleId,
            representationMethodId,
        },
        {
            ...payload.managers[0],
            id: null,
            name: 'ГЕОРГИ',
            surname: 'СТОЯНОВ',
            jobPosition: 'CFO',
            titleId,
            representationMethodId,
        },
    ];

    payload.communicationData = [
        {
            ...payload.communicationData[0],
            communicationContacts: [
                {
                    sendSms: true,
                    platformId: null,
                    status: 'ACTIVE',
                    contactType: 'MOBILE_NUMBER',
                    contactValue: '555000001',
                },
            ],
        },
        {
            ...payload.communicationData[0],
            communicationContacts: [
                {
                    sendSms: true,
                    platformId: null,
                    status: 'ACTIVE',
                    contactType: 'MOBILE_NUMBER',
                    contactValue: '555000002',
                },
            ],
        },
    ];

    payload.owner = [
        { personalNumber: owner1.identifier, belongingOwnerCapitalId: belongingCapitalOwnerId },
        { personalNumber: owner2.identifier, belongingOwnerCapitalId: belongingCapitalOwnerId },
    ];

    const customerResp = await Request.post(Endpoints.customer, { data: payload });
    await expect(customerResp).toBeOK();
    const customer = await customerResp.json();
    Responses.customer.push(customer);

    const details = await getCustomerDetails(Request, customer.id);
    const commData = details.communicationData ?? [];
    const managers = details.managers ?? [];
    const owners = details.owner ?? [];

    return {
        customerId: customer.id,
        customerVersionId: details.versionId ?? 1,
        customerPnOrUic: customer.identifier,
        communicationDataId1: commData[0]?.id ?? 0,
        communicationDataId2: commData[1]?.id ?? 0,
        managerId1: managers[0]?.id ?? 0,
        managerId2: managers[1]?.id ?? 0,
        ownerId1: owners[0]?.id ?? 0,
        ownerId2: owners[1]?.id ?? 0,
        ownerCustomerIdentifier1: owner1.identifier,
        ownerCustomerIdentifier2: owner2.identifier,
        titleId,
        representationMethodId,
        belongingCapitalOwnerId,
    };
}

// ═══════════════════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════════════════

test.describe('[PHN-2208]: Put update customer data', { tag: '@salesPortal' }, () => {
    test.describe.configure({ mode: 'serial' });

    // ══════════════ POSITIVE TESTS ══════════════

    test('[PHN-2208]: TC-BE-1 – Successfully update individual customer with registered address', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer + nomenclatures', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT /sales-portal/customer – individual, registered address', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'ИВАН',
                    middleName: 'ПЕТРОВ',
                    surname: 'ГЕОРГИЕВ',
                    registered: true,
                    address: {
                        countryId: pre!.countryId,
                        populatedPlaceId: pre!.populatedPlaceId,
                        zipCodeId: pre!.zipCodeId,
                    },
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-2 – Successfully update individual with unregistered (foreign) address', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT /sales-portal/customer – unregistered address', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'JOHN',
                    surname: 'DOE',
                    registered: false,
                    address: {
                        countryId: pre!.countryId,
                        region: 'Sofia Region',
                        municipality: 'Sofia Municipality',
                        populatedPlace: 'Sofia',
                        zipCode: '1000',
                        street: 'Vitosha Blvd',
                        streetNumber: '15',
                    },
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-3 – Successfully update legal entity (full payload)', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints, Nomenclatures,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer with dependencies', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('PUT /sales-portal/customer – legal entity full update', async () => {
            const [countryId, populatedPlaceId, zipCodeId, economicBranchCiId, economicBranchNceaId] = await Promise.all([
                findActiveNomenclature(Request, 'countries'),
                findActiveNomenclature(Request, 'populated-places'),
                findActiveNomenclature(Request, 'zip-codes'),
                Nomenclatures.Economic_branch_based_on_commercial_information(),
                Nomenclatures.economic_branch_according_to_national_classification(),
            ]);

            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    economicBranchCiId,
                    economicBranchNceaId,
                    mainSubjectOfActivity: 'Energy distribution',
                    communicationData: [{
                        communicationDataId: pre!.communicationDataId,
                        registered: true,
                        address: { countryId, populatedPlaceId, zipCodeId },
                        phones: ['+359888123456'],
                        emails: ['test@example.com'],
                    }],
                    managers: [{
                        managerId: pre!.managerId,
                        name: 'ПЕТЪР',
                        surname: 'ИВАНОВ',
                        jobPosition: 'CEO',
                        representationMethodId: pre!.representationMethodId,
                        titleId: pre!.titleId,
                        mobileNumbers: ['+359899000000'],
                        emails: ['manager@company.bg'],
                    }],
                    owners: [{
                        ownerId: pre!.ownerId,
                        ownerIdentifier: pre!.ownerCustomerIdentifier,
                        ownerName: 'Owner Company Ltd',
                        belongingCapitalOwnerId: pre!.belongingCapitalOwnerId,
                    }],
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-3 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-4 – Direct debit set to true with valid bank/bic/iban', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – directDebit true', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    directDebit: true,
                    bankId: pre!.bankId,
                    bic: pre!.bankBic,
                    iban: 'BG80BNBG96611020345678',
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'Owner', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-4 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-7 – Cross-type fields silently ignored for individual', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – individual with legal entity fields (should be ignored)', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'МАРИЯ',
                    surname: 'ПЕТРОВА',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                    additionalComment: 'should be ignored',
                    economicBranchCiId: 999,
                    directDebit: true,
                    ownershipFormId: 10000,
                    economicBranchId: 54635,
                    economicBranchNCEAId: 12345,
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-7 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-11 – KYC updated for individual with valid expiration', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – kycPassed=true with future date', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'ИВАН',
                    surname: 'ГЕОРГИЕВ',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                    kycPassed: true,
                    kycExpirationDate: '2027-12-31',
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-11 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ══════════════ NEGATIVE TESTS ══════════════

    test('[PHN-2208]: TC-BE-13 – Missing customerId (null)', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – missing customerId → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: null,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'TEST',
                    surname: 'TEST',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                },
            });
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('customerId');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-13 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-15 – Customer not found by ID', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        await test.step('PUT – non-existent customerId → error', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: 99999999,
                    customerVersionId: 1,
                    customerPnOrUic: '0000000000',
                    name: 'TEST',
                    surname: 'TEST',
                    registered: true,
                    address: { countryId: 1 },
                },
            });
            expect(response.status()).toBeGreaterThanOrEqual(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Customer not found');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-15 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-17 – Customer identifier mismatch', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – wrong customerPnOrUic → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: 'WRONG123456',
                    name: 'TEST',
                    surname: 'TEST',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                },
            });
            console.log('Response status:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('does not match');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-17 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-18 – Customer version not found', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – invalid versionId → error', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: 99999,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'TEST',
                    surname: 'TEST',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBeGreaterThanOrEqual(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('version not found');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-18 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-19 – Individual missing name (mandatory)', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – name=null → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: null,
                    surname: 'VALID',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('name');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-19 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-22 – Registered address with string fields → nomenclature error', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – registered=true with string fields → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'ИВАН',
                    surname: 'ИВАНОВ',
                    registered: true,
                    address: {
                        countryId: pre!.countryId,
                        populatedPlaceId: pre!.populatedPlaceId,
                        zipCodeId: pre!.zipCodeId,
                        region: 'Should trigger error',
                        municipality: 'Should trigger error',
                    },
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            const msg = JSON.stringify(body);
            expect(msg).toContain('Nomenclature IDs should be provided');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-22 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-23 – Unregistered address with nomenclature IDs → string error', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – registered=false with nomenclature IDs → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'ИВАН',
                    surname: 'ИВАНОВ',
                    registered: false,
                    address: {
                        countryId: pre!.countryId,
                        populatedPlaceId: 123,
                        zipCodeId: 456,
                    },
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            const msg = JSON.stringify(body);
            expect(msg).toContain('Strings should be provided');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-23 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-24 – Legal entity missing communicationData', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – no communicationData → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'Test', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Communication data is mandatory');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-24 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-25 – Legal entity missing owners', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – no owners → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Owners are mandatory');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-25 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-28 – Legal entity managerId not found', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – invalid managerId → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    managers: [{ managerId: 99999999, name: 'T', surname: 'T', jobPosition: 'T', representationMethodId: pre!.representationMethodId, titleId: pre!.titleId }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Manager not found');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-28 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-35 – Direct debit true without bank/bic/iban', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – directDebit=true, no banking fields → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    directDebit: true,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            const msg = JSON.stringify(body);
            expect(msg).toContain('mandatory when direct debit is true');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-35 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-37 – BIC does not match bank BIC', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – wrong BIC → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    directDebit: true,
                    bankId: pre!.bankId,
                    bic: 'WRONGBIC',
                    iban: 'BG80BNBG96611020345678',
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('BIC does not match');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-37 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-38 – KYC expiration without kycPassed=true', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – kycPassed=false with expirationDate → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'ИВАН',
                    surname: 'ИВАНОВ',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                    kycPassed: false,
                    kycExpirationDate: '2027-01-01',
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('KYC expiration date may only be set when KYC is passed');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-38 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-39 – KYC passed=true without expiration date', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – kycPassed=true, no expiration → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'ИВАН',
                    surname: 'ИВАНОВ',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                    kycPassed: true,
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('KYC expiration date is mandatory when KYC is passed');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-39 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-40 – KYC expiration date in the past', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – past kycExpirationDate → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'ИВАН',
                    surname: 'ИВАНОВ',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                    kycPassed: true,
                    kycExpirationDate: '2020-01-01',
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('must be today or in the future');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-40 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-41 – customerPnOrUic with invalid characters', async ({
        SPRequest, Responses,
    }) => {
        await test.step('PUT – invalid characters in identifier → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: 1,
                    customerVersionId: 1,
                    customerPnOrUic: 'abc!@#',
                    name: 'TEST',
                    surname: 'TEST',
                    registered: true,
                    address: { countryId: 1 },
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('not allowed symbols');
        });

        test.info().attach('[PHN-2208] TC-BE-41 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ══════════════ POSITIVE – REMAINING ══════════════

    test('[PHN-2208]: TC-BE-5 – Direct debit set to false clears banking fields', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity with direct debit enabled', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
            const bank = await findActiveBank(Request);
            await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    directDebit: true,
                    bankId: bank.id,
                    bic: bank.bic,
                    iban: 'BG80BNBG96611020345678',
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
        });

        await test.step('PUT – directDebit=false → clears banking fields', async () => {
            const freshVersion = await getCustomerVersionId(Request, pre!.customerId);
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: freshVersion,
                    customerPnOrUic: pre!.customerPnOrUic,
                    directDebit: false,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-5 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-6 – Direct debit null leaves existing unchanged', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity with direct debit enabled', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
            const bank = await findActiveBank(Request);
            await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    directDebit: true,
                    bankId: bank.id,
                    bic: bank.bic,
                    iban: 'BG80BNBG96611020345678',
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('PUT – directDebit omitted → existing state unchanged', async () => {
        //     const freshVersion = await getCustomerVersionId(Request, pre!.customerId);
        //     const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
        //         data: {
        //             customerId: pre!.customerId,
        //             customerVersionId: freshVersion,
        //             customerPnOrUic: pre!.customerPnOrUic,
        //             communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
        //             owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
        //         },
        //     });
        //     await expect(response).CheckResponse();
        //     const body = await response.json();
        //     expect(body.status).toBe('success');
        });

        // console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-6 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-8 – Cross-type fields silently ignored for legal entity', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – legal entity with individual fields (should be ignored)', async () => {
            const payload = {
                customerId: pre!.customerId,
                customerVersionId: pre!.customerVersionId,
                customerPnOrUic: pre!.customerPnOrUic,
                name: 'CHANGED NAME',
                middleName: 'SHOULD_BEIGNORED',
                surname: 'SHOULD_BEIGNORED',
                registered: true,
                address: { countryId: 1, populatedPlaceId: 1, zipCodeId: 1 },
                kycPassed: true,
                kycExpirationDate: '2028-01-01',
                communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'Owner', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
            };
            console.log('PUT payload:', JSON.stringify(payload, null, 2));
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, { data: payload });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-8 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-9 – Communication data APPEND semantics', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity with existing communication contacts', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });
        

        await test.step('PUT – communicationData phones/emails APPEND', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{
                        communicationDataId: pre!.communicationDataId,
                        registered: false,
                        address: { countryId: 1 },
                        phones: ['+359888222222', '+359888333333'],
                        emails: ['new@test.com'],
                    }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-9 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-10 – Manager contacts REPLACE semantics', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity with manager having contacts', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
            await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre.customerId,
                    customerVersionId: pre.customerVersionId,
                    customerPnOrUic: pre.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre.communicationDataId, registered: false, address: { countryId: 1 } }],
                    managers: [{
                        managerId: pre.managerId,
                        name: 'ПЕТЪР',
                        surname: 'ИВАНОВ',
                        jobPosition: 'CEO',
                        representationMethodId: pre.representationMethodId,
                        titleId: pre.titleId,
                        mobileNumbers: ['+6875463524'],
                        emails: ['initial@company.bg'],
                    }],
                    owners: [{ ownerId: pre.ownerId, ownerIdentifier: pre.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre.belongingCapitalOwnerId }],
                },
            });
            pre.customerVersionId = await getCustomerVersionId(Request, pre.customerId);
        });

        await test.step('PUT – manager mobileNumbers/emails REPLACE old contacts', async () => {
            const payload = {
                customerId: pre!.customerId,
                customerVersionId: pre!.customerVersionId,
                customerPnOrUic: pre!.customerPnOrUic,
                communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                managers: [{
                    managerId: pre!.managerId,
                    name: 'ПЕТЪР',
                    surname: 'ИВАНОВ',
                    jobPosition: 'CEO',
                    representationMethodId: pre!.representationMethodId,
                    titleId: pre!.titleId,
                    mobileNumbers: ['+3578678'],
                    emails: ['replaced@company.bg'],
                }],
                owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
            };
            console.log('PUT payload:', JSON.stringify(payload, null, 2));
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, { data: payload });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-10 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-12 – KYC updated on manager for legal entity', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity with manager', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('PUT – manager kycPassed=true with expiration', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    managers: [{
                        managerId: pre!.managerId,
                        name: 'ИВАН',
                        surname: 'ПЕТРОВ',
                        jobPosition: 'Director',
                        representationMethodId: pre!.representationMethodId,
                        titleId: pre!.titleId,
                        kycPassed: true,
                        kycExpirationDate: '2028-06-15',
                    }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        test.info().attach('[PHN-2208] TC-BE-12 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ══════════════ NEGATIVE – REMAINING ══════════════

    test('[PHN-2208]: TC-BE-14 – customerId is negative', async ({
        SPRequest, Responses,
    }) => {
        await test.step('PUT – customerId:-1 → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: -1,
                    customerVersionId: 1,
                    customerPnOrUic: '1234567890',
                    name: 'TEST',
                    surname: 'TEST',
                    registered: true,
                    address: { countryId: 1 },
                },
            });
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('must be greater than 0');
        });

        test.info().attach('[PHN-2208] TC-BE-14 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-16 – Customer is DELETED', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let customerId: number;
        let customerPnOrUic: string;
        let versionId: number;

        await test.step('Precondition: Create customer and delete it', async () => {
            const customer = await createIndividualCustomer(Request, GeneratePayload, Responses, Endpoints);
            customerId = customer.id;
            customerPnOrUic = customer.identifier;
            versionId = await getCustomerVersionId(Request, customerId);
            const delResponse = await Request.delete(`customer/${customerId}`);
            await expect(delResponse).toBeOK();
        });

        await test.step('PUT – deleted customer → 400', async () => {
            const payload = {
                customerId: customerId!,
                customerVersionId: versionId!,
                customerPnOrUic: customerPnOrUic!,
                name: 'TEST',
                surname: 'TEST',
                registered: true,
                address: { countryId: 1 },
            };
            console.log('PUT payload:', JSON.stringify(payload, null, 2));
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, { data: payload });
            
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain("Can't update deleted customer");
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-16 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-20 – Individual missing surname', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – surname=null → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'VALID',
                    surname: null,
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('surname');
        });

        test.info().attach('[PHN-2208] TC-BE-20 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-21 – Individual missing address and registered flag', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – address=null, registered=null → 400', async () => {
            const payload = {
                customerId: pre!.customerId,
                customerVersionId: pre!.customerVersionId,
                customerPnOrUic: pre!.customerPnOrUic,
                name: 'VALID',
                surname: 'VALID',
                registered: false,
                address: {
                    countryId: pre!.countryId,
                    region: 'Sofia Region',
                    municipality: 'Sofia Municipality',
                    populatedPlace: 'Sofia',
                    zipCode: '1000',
                    street: 'Vitosha Blvd',
                    streetNumber: '15',
                },
            };
            console.log('PUT payload:', JSON.stringify(payload, null, 2));
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, { data: payload });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(200);
            const body = await response.json();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-21 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-26 – Legal entity communicationDataId not found', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – invalid communicationDataId → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: 99999999, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Communication data not found');
        });

        test.info().attach('[PHN-2208] TC-BE-26 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-27 – Legal entity communicationDataId belongs to different customer', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let preA: LegalPreconditions;
        let preB: LegalPreconditions;

        await test.step('Precondition: Create two legal entity customers', async () => {
            preA = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
            preB = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – customer A with communicationDataId from B → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: preA!.customerId,
                    customerVersionId: preA!.customerVersionId,
                    customerPnOrUic: preA!.customerPnOrUic,
                    communicationData: [{ communicationDataId: preB!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: preA!.ownerId, ownerIdentifier: preA!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: preA!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('does not belong to this customer');
        });

        test.info().attach('[PHN-2208] TC-BE-27 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-29 – Legal entity managerId belongs to different customer', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let preA: LegalPreconditions;
        let preB: LegalPreconditions;

        await test.step('Precondition: Create two legal entity customers with managers', async () => {
            preA = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
            preB = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – customer A with managerId from B → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: preA!.customerId,
                    customerVersionId: preA!.customerVersionId,
                    customerPnOrUic: preA!.customerPnOrUic,
                    communicationData: [{ communicationDataId: preA!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    managers: [{ managerId: preB!.managerId, name: 'T', surname: 'T', jobPosition: 'T', representationMethodId: preA!.representationMethodId, titleId: preA!.titleId }],
                    owners: [{ ownerId: preA!.ownerId, ownerIdentifier: preA!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: preA!.belongingCapitalOwnerId }],
                },
            });
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('does not belong to this customer');
        });

        test.info().attach('[PHN-2208] TC-BE-29 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-30 – Legal entity manager missing mandatory fields', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer with manager', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – manager with all mandatory fields null → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    managers: [{ managerId: pre!.managerId, name: null, surname: null, jobPosition: null, representationMethodId: null, titleId: null }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            const msg = JSON.stringify(body);
            expect(msg).toContain('Name is mandatory');
            expect(msg).toContain('Surname is mandatory');
            expect(msg).toContain('Job position is mandatory');
        });

        test.info().attach('[PHN-2208] TC-BE-30 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-31 – Legal entity ownerId not found', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – invalid ownerId → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: 99999999, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Owner not found');
        });

        test.info().attach('[PHN-2208] TC-BE-31 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-32 – Legal entity ownerId belongs to different customer', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let preA: LegalPreconditions;
        let preB: LegalPreconditions;

        await test.step('Precondition: Create two legal entity customers', async () => {
            preA = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
            preB = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – customer A with ownerId from B → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: preA!.customerId,
                    customerVersionId: preA!.customerVersionId,
                    customerPnOrUic: preA!.customerPnOrUic,
                    communicationData: [{ communicationDataId: preA!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: preB!.ownerId, ownerIdentifier: preB!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: preA!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('does not belong to this customer');
        });

        test.info().attach('[PHN-2208] TC-BE-32 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-33 – Legal entity owner missing mandatory fields', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – owner with mandatory fields null → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: null, ownerName: null, belongingCapitalOwnerId: null }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            const msg = JSON.stringify(body);
            expect(msg).toContain('Owner identifier is mandatory');
            expect(msg).toContain('Owner name is mandatory');
            expect(msg).toContain('Belonging capital owner is mandatory');
        });

        test.info().attach('[PHN-2208] TC-BE-33 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-34 – Legal entity ownerIdentifier not found as active customer', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – ownerIdentifier non-existent → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: 'NONEXISTENT999', ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Customer not found by identifier');
        });

        test.info().attach('[PHN-2208] TC-BE-34 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-36 – Direct debit bankId not found', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – directDebit=true, bankId=99999999 → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    directDebit: true,
                    bankId: 99999999,
                    bic: 'TESTBIC1',
                    iban: 'BG00TEST00000000001',
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Bank not found');
        });

        test.info().attach('[PHN-2208] TC-BE-36 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-42 – Name with disallowed characters', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: IndividualPreconditions;

        await test.step('Precondition: Create individual customer', async () => {
            pre = await setupIndividual(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – name with lowercase/invalid symbols → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    name: 'ivan123!@',
                    surname: 'VALID',
                    registered: true,
                    address: { countryId: pre!.countryId, populatedPlaceId: pre!.populatedPlaceId, zipCodeId: pre!.zipCodeId },
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('not allowed symbols');
        });

        test.info().attach('[PHN-2208] TC-BE-42 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-43 – Legal entity representationMethodId not found', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – manager representationMethodId=99999999 → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    managers: [{ managerId: pre!.managerId, name: 'T', surname: 'T', jobPosition: 'T', representationMethodId: 99999999, titleId: pre!.titleId }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Representation method not found');
        });

        test.info().attach('[PHN-2208] TC-BE-43 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-44 – Legal entity titleId not found', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – manager titleId=99999999 → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    managers: [{ managerId: pre!.managerId, name: 'T', surname: 'T', jobPosition: 'T', representationMethodId: pre!.representationMethodId, titleId: 99999999 }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: pre!.belongingCapitalOwnerId }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Title not found');
        });

        test.info().attach('[PHN-2208] TC-BE-44 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-45 – Legal entity belongingCapitalOwnerId not found', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT – owner belongingCapitalOwnerId=99999999 → 400', async () => {
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [{ communicationDataId: pre!.communicationDataId, registered: false, address: { countryId: 1 } }],
                    owners: [{ ownerId: pre!.ownerId, ownerIdentifier: pre!.ownerCustomerIdentifier, ownerName: 'T', belongingCapitalOwnerId: 99999999 }],
                },
            });
            console.log('Response text:', await response.text());
            expect(response.status()).toBe(400);
            const body = await response.json();
            expect(JSON.stringify(body)).toContain('Belonging capital owner not found');
        });

        test.info().attach('[PHN-2208] TC-BE-45 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-57 – Non-mandatory fields cleared when omitted in PUT payload', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity with ALL fields populated', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);

            const bank = await findActiveBank(Request);
            const countryId = await findActiveNomenclature(Request, 'countries');
            const populatedPlaceId = await findActiveNomenclature(Request, 'populated-places');
            const zipCodeId = await findActiveNomenclature(Request, 'zip-codes');

            const fullPayload = {
                customerId: pre!.customerId,
                customerVersionId: pre!.customerVersionId,
                customerPnOrUic: pre!.customerPnOrUic,
                additionalComment: 'This is a full additional comment',
                economicBranchCiId: 1056,
                economicBranchNceaId: 1023,
                mainSubjectOfActivity: 'Energy distribution and supply services',
                directDebit: true,
                bankId: bank.id,
                bic: bank.bic,
                iban: 'BG80BNBG96611020345678',
                "registered": false,
                address: {
                    countryId: 1000,
                    populatedPlaceId: '2',
                    zipCodeId: '2',
                    districtId: '2',
                    residentialAreaId: '2',
                    streetId: '2',
                    region: 'string',
                    municipality: 'string',
                    populatedPlace: 'string',
                    zipCode: 'string',
                    district: 'string',
                    residentialArea: 'string',
                    street: 'string',
                    streetNumber: 'string',
                    block: 'string',
                    entrance: 'string',
                    floor: 'string',
                    apartment: 'string',
                },
                communicationData: [{
                    communicationDataId: pre!.communicationDataId,
                    registered: true,
                    address: { countryId, 
                        populatedPlaceId,
                        zipCodeId,
                        block: 'string',
                        entrance: 'string',
                        floor: 'string',
                        apartment: 'string',},
                    phones: ['+359888111111', '+359888222222'],
                    emails: ['full@company.bg', 'backup@company.bg'],
                }],
                managers: [{
                    managerId: pre!.managerId,
                    name: 'ПЕТЪР',
                    middleName: 'ИВАНОВ',
                    surname: 'ГЕОРГИЕВ',
                    jobPosition: 'Chief Executive Officer',
                    representationMethodId: pre!.representationMethodId,
                    titleId: pre!.titleId,
                    personalNumber: '8901011234',
                    birthDate: '1989-01-01',
                    positionHeldFrom: '2020-03-15',
                    positionHeldTo: '2030-12-31',
                    mobileNumbers: ['+359899111222', '+359899333444'],
                    emails: ['ceo@company.bg', 'peter@company.bg'],
                }],
                owners: [{
                    ownerId: pre!.ownerId,
                    ownerIdentifier: pre!.ownerCustomerIdentifier,
                    ownerName: 'Full Owner Company Name Ltd',
                    belongingCapitalOwnerId: pre!.belongingCapitalOwnerId,
                }],
            };

            console.log('Initial PUT payload with all fields:', JSON.stringify(fullPayload, null, 2));

            const initResponse = await SPRequest.put(SP_CUSTOMER_ENDPOINT, { data: fullPayload });
            await expect(initResponse).CheckResponse();
        });

        await test.step('PUT – only mandatory fields, all optional fields null/absent', async () => {
            const freshVersion = await getCustomerVersionId(Request, pre!.customerId);
            const countryId = await findActiveNomenclature(Request, 'countries');

            const minimalPayload = {
                customerId: pre!.customerId,
                customerVersionId: freshVersion,
                customerPnOrUic: pre!.customerPnOrUic,
                // additionalComment: NOT sent (null) → expected: cleared
                economicBranchCiId: 1056,
                // economicBranchNceaId: NOT sent (null) → expected: cleared
                mainSubjectOfActivity: 'Energy distribution and supply services',
                // directDebit: NOT sent (null) → expected: UNCHANGED (special null handling)
                communicationData: [{
                    communicationDataId: pre!.communicationDataId,
                    registered: false,
                    address: {
                        countryId,
                        region: 'Sofia',
                        municipality: 'Sofia',
                        populatedPlace: 'Sofia',
                        zipCode: '1000',
                    },
                    // phones: NOT sent → expected: nothing appended (APPEND with empty = no-op)
                    // emails: NOT sent → expected: nothing appended
                }],
                managers: [{
                    managerId: pre!.managerId,
                    name: 'ПЕТЪР',
                    surname: 'ГЕОРГИЕВ',
                    jobPosition: 'CEO',
                    representationMethodId: pre!.representationMethodId,
                    titleId: pre!.titleId,
                    // middleName: NOT sent (null) → expected: CLEARED
                    // personalNumber: NOT sent (null) → expected: CLEARED
                    // birthDate: NOT sent (null) → expected: CLEARED
                    // positionHeldFrom: NOT sent (null) → expected: CLEARED
                    // positionHeldTo: NOT sent (null) → expected: CLEARED
                    // mobileNumbers: NOT sent → expected: REPLACE with empty = CLEARS all contacts
                    // emails: NOT sent → expected: REPLACE with empty = CLEARS all contacts
                }],
                owners: [{
                    ownerId: pre!.ownerId,
                    ownerIdentifier: pre!.ownerCustomerIdentifier,
                    ownerName: 'Full Owner Company Name Ltd',
                    belongingCapitalOwnerId: pre!.belongingCapitalOwnerId,
                }],
            };

            console.log('Minimal PUT payload:', JSON.stringify(minimalPayload, null, 2));

            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, { data: minimalPayload });
            const body = await response.json();
            console.log('Minimal PUT response:', JSON.stringify(body, null, 2));
            await expect(response).CheckResponse();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));
    });

    test('[PHN-2208]: TC-BE-58 – Private customer: optional address fields cleared when not sent', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let cust: { customerId: number; customerVersionId: number; customerPnOrUic: string };
        let detailsBefore: any;

        await test.step('Precondition: Create private customer with full address (all optional fields)', async () => {
            cust = await createIndividualWithFullAddress(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('Capture address state BEFORE minimal update', async () => {
            detailsBefore = await getCustomerDetails(Request, cust!.customerId);
        });

        await test.step('PUT – only mandatory address fields, optional fields omitted', async () => {
            const countryId = await findActiveNomenclature(Request, 'countries');
            const populatedPlaceId = await findActiveNomenclature(Request, 'populated-places');
            const zipCodeId = await findActiveNomenclature(Request, 'zip-codes');

            const payload = {
                customerId: cust!.customerId,
                customerVersionId: cust!.customerVersionId,
                customerPnOrUic: cust!.customerPnOrUic,
                name: 'ИВАН',
                surname: 'ПЕТРОВ',
                registered: true,
                address: {
                    countryId: 1001,
                    populatedPlaceId: 1001,
                    zipCodeId: 6613,
                    region: 1000,
                    municipality: 1000,
                    // districtId: NOT sent
                    // residentialAreaId: NOT sent
                    // streetId: NOT sent
                    // streetNumber: NOT sent
                    // block: NOT sent
                    // entrance: NOT sent
                    // floor: NOT sent
                    // apartment: NOT sent
                },
            }

            console.log('PUT payload with only mandatory address fields:', JSON.stringify(payload, null, 2));
            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: payload,
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        await test.step('Verify: optional address fields should be cleared', async () => {
            const detailsAfter = await getCustomerDetails(Request, cust!.customerId);

            const addrBefore = detailsBefore?.activeCustomerDetail ?? detailsBefore;
            const addrAfter = detailsAfter?.activeCustomerDetail ?? detailsAfter;

            expect(addrAfter.streetNumber ?? addrAfter.address?.streetNumber ?? null).toBeNull();
            expect(addrAfter.block ?? addrAfter.address?.block ?? null).toBeNull();
            expect(addrAfter.entrance ?? addrAfter.address?.entrance ?? null).toBeNull();
            expect(addrAfter.floor ?? addrAfter.address?.floor ?? null).toBeNull();
            expect(addrAfter.apartment ?? addrAfter.address?.apartment ?? null).toBeNull();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-58 address comparison', {
            body: JSON.stringify({
                before: detailsBefore,
                expectation: 'streetNumber, block, entrance, floor, apartment should be cleared (null) when omitted in PUT payload',
            }, null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-59 – Private-business customer: optional address fields cleared when not sent', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let cust: { customerId: number; customerVersionId: number; customerPnOrUic: string };
        let detailsBefore: any;

        await test.step('Precondition: Create private-business customer with full address', async () => {
            cust = await createPrivateBusinessWithFullAddress(Request, SPRequest, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Capture address state BEFORE minimal update', async () => {
            detailsBefore = await getCustomerDetails(Request, cust!.customerId);
        });

        await test.step('PUT – only mandatory address fields, optional fields omitted', async () => {
            const countryId = await findActiveNomenclature(Request, 'countries');
            const populatedPlaceId = await findActiveNomenclature(Request, 'populated-places');
            const zipCodeId = await findActiveNomenclature(Request, 'zip-codes');

            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: cust!.customerId,
                    customerVersionId: cust!.customerVersionId,
                    customerPnOrUic: cust!.customerPnOrUic,
                    name: 'ГЕОРГИ',
                    surname: 'СТОЯНОВ',
                    registered: true,
                    address: {
                        countryId,
                        populatedPlaceId,
                        zipCodeId,
                        // districtId: NOT sent
                        // residentialAreaId: NOT sent
                        // streetId: NOT sent
                        // streetNumber: NOT sent
                        // block: NOT sent
                        // entrance: NOT sent
                        // floor: NOT sent
                        // apartment: NOT sent
                    },
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        await test.step('Verify: optional address fields should be cleared', async () => {
            const detailsAfter = await getCustomerDetails(Request, cust!.customerId);

            const addrAfter = detailsAfter?.activeCustomerDetail ?? detailsAfter;

            expect(addrAfter.streetNumber ?? addrAfter.address?.streetNumber ?? null).toBeNull();
            expect(addrAfter.block ?? addrAfter.address?.block ?? null).toBeNull();
            expect(addrAfter.entrance ?? addrAfter.address?.entrance ?? null).toBeNull();
            expect(addrAfter.floor ?? addrAfter.address?.floor ?? null).toBeNull();
            expect(addrAfter.apartment ?? addrAfter.address?.apartment ?? null).toBeNull();
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-59 address comparison', {
            body: JSON.stringify({
                before: detailsBefore,
                expectation: 'streetNumber, block, entrance, floor, apartment should be cleared (null) when omitted in PUT payload',
            }, null, 2),
            contentType: 'application/json',
        });
    });

    test('[PHN-2208]: TC-BE-60 – Update only one manager/owner/commData; second entries remain unchanged', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalMultiplePreconditions;
        let detailsBefore: any;

        await test.step('Precondition: Create legal entity with 2 managers, 2 owners, 2 communication data', async () => {
            pre = await setupLegalWithMultiple(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('Capture customer state BEFORE partial update', async () => {
            detailsBefore = await getCustomerDetails(Request, pre!.customerId);
        });

        await test.step('PUT /sales-portal/customer – update only first manager, owner, communicationData; omit second entries', async () => {
            const countryId = await findActiveNomenclature(Request, 'countries');
            const populatedPlaceId = await findActiveNomenclature(Request, 'populated-places');
            const zipCodeId = await findActiveNomenclature(Request, 'zip-codes');

            const response = await SPRequest.put(SP_CUSTOMER_ENDPOINT, {
                data: {
                    customerId: pre!.customerId,
                    customerVersionId: pre!.customerVersionId,
                    customerPnOrUic: pre!.customerPnOrUic,
                    communicationData: [
                        {
                            communicationDataId: pre!.communicationDataId1,
                            registered: true,
                            address: { countryId, populatedPlaceId, zipCodeId },
                            phones: ['+359888111111'],
                            emails: ['updated-comm1@example.com'],
                        },
                        // second communicationData entry deliberately omitted
                    ],
                    managers: [
                        {
                            managerId: pre!.managerId1,
                            name: 'ОБНОВЕН',
                            surname: 'МЕНИДЖЪР',
                            jobPosition: 'CTO',
                            representationMethodId: pre!.representationMethodId,
                            titleId: pre!.titleId,
                            mobileNumbers: ['+359899111222'],
                            emails: ['updated-manager1@company.bg'],
                        },
                        // second manager entry deliberately omitted
                    ],
                    owners: [
                        {
                            ownerId: pre!.ownerId1,
                            ownerIdentifier: 112233,
                            ownerName: 'UPDATED OWNER ONE LTD',
                            belongingCapitalOwnerId: pre!.belongingCapitalOwnerId,
                        },
                        // second owner entry deliberately omitted
                    ],
                },
            });
            await expect(response).CheckResponse();
            const body = await response.json();
            expect(body.status).toBe('success');
        });

        await test.step('Verify: second manager, owner, communicationData are still present and unchanged', async () => {
            const detailsAfter = await getCustomerDetails(Request, pre!.customerId);

            const commAfter: any[] = detailsAfter.communicationData ?? [];
            const managersAfter: any[] = detailsAfter.managers ?? [];
            const ownersAfter: any[] = detailsAfter.owner ?? [];

            // Both entries must still exist
            expect(commAfter.length).toBeGreaterThanOrEqual(2);
            expect(managersAfter.length).toBeGreaterThanOrEqual(2);
            expect(ownersAfter.length).toBeGreaterThanOrEqual(2);

            // Second communicationData entry is still present by its ID
            const secondComm = commAfter.find((c: any) => c.id === pre!.communicationDataId2);
            expect(secondComm).toBeDefined();

            // Second manager entry is still present by its ID
            const secondManager = managersAfter.find((m: any) => m.id === pre!.managerId2);
            expect(secondManager).toBeDefined();
            expect(secondManager.name).toBe('ГЕОРГИ');
            expect(secondManager.surname).toBe('СТОЯНОВ');
            expect(secondManager.jobPosition).toBe('CFO');

            // Second owner entry is still present by its ID
            const secondOwner = ownersAfter.find((o: any) => o.id === pre!.ownerId2);
            expect(secondOwner).toBeDefined();
            expect(secondOwner.identifier ?? secondOwner.personalNumber ?? secondOwner.ownerIdentifier)
                .toBe(pre!.ownerCustomerIdentifier2);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('[PHN-2208] TC-BE-60 response', {
            body: JSON.stringify({
                before: detailsBefore,
                note: 'Only first manager/owner/communicationData updated. Second entries must remain untouched.',
                responses: reportGenerator.setLinksToResponses(Responses),
            }, null, 2),
            contentType: 'application/json',
        });
    });
});

