import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';
import { ExtendedAPIResponse } from '../../backend/utils/RequestWrapper';
import { envVariables } from '../../backend/fixtures/envCashed';

// ═══════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS & TYPES
// ═══════════════════════════════════════════════════════════════════════════

const SP_CUSTOMER_ENDPOINT = 'customer';

/** Address ids cached by global setup for this environment. */
function envAddressIds() {
    return {
        countryId: envVariables.countries as number,
        populatedPlaceId: envVariables.population_places as number,
        zipCodeId: envVariables.zip_codes as number,
    };
}

async function createIndividualCustomer(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.customers.customer_private();
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
    countryId: number;
}

async function setupIndividual(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any
): Promise<IndividualPreconditions> {
    const customer = await createIndividualCustomer(Request, GeneratePayload, Responses, Endpoints);
    const versionId = await getCustomerVersionId(Request, customer.id);
    return {
        customerId: customer.id,
        customerVersionId: versionId,
        customerPnOrUic: customer.identifier,
        ...envAddressIds(),
    };
}

async function setupLegal(
    Request: any, GeneratePayload: any, Responses: any, Endpoints: any
): Promise<LegalPreconditions> {
    const titleId = envVariables.title as number;
    const representationMethodId = envVariables.method_of_representation as number;
    const belongingCapitalOwnerId = envVariables.belonging_capital_owners as number;
    const bankResp = await Request.get(`banks/${envVariables.banks}`);
    await expect(bankResp).toBeOK();
    const bank = await bankResp.json();

    // Owner is an individual customer. Address nomenclatures come from envVariables via the payload.
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
        countryId: envVariables.countries as number,
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
    return {
        customerId: customer.id,
        customerVersionId: versionId,
        customerPnOrUic: customer.identifier,
        ...envAddressIds(),
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
    const { countryId, populatedPlaceId, zipCodeId } = envAddressIds();

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
    const { countryId, populatedPlaceId, zipCodeId } = envAddressIds();

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
    const titleId = envVariables.title as number;
    const representationMethodId = envVariables.method_of_representation as number;
    const belongingCapitalOwnerId = envVariables.belonging_capital_owners as number;

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

async function getEconomicBranchCi(Request: any){
    const id = await Request.get('nomenclature/economic-branch-ci/filter?statuses=ACTIVE&statuses=INACTIVE&page=0&size=25')
    const idResponse = await id.json();
    return idResponse.content[0].id;
}

// ═══════════════════════════════════════════════════════════════════════════
// TESTS
// ═══════════════════════════════════════════════════════════════════════════

test.describe('[REG-1305]: Put update customer data', { tag: '@salesPortal' }, () => {

    // ══════════════ POSITIVE TESTS ══════════════

    test('[REG-1310]: TC-BE-1 – Successfully update individual customer with registered address', async ({
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

        test.info().attach('[REG-1310] TC-BE-1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-1312]: TC-BE-2 – Successfully update individual with unregistered (foreign) address', async ({
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

        test.info().attach('[REG-1312] TC-BE-2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-1311]: TC-BE-3 – Successfully update legal entity (full payload)', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        let pre: LegalPreconditions;

        await test.step('Precondition: Create legal entity customer with dependencies', async () => {
            pre = await setupLegal(Request, GeneratePayload, Responses, Endpoints);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('PUT /sales-portal/customer – legal entity full update', async () => {
            const { countryId, populatedPlaceId, zipCodeId } = envAddressIds();
            const economicBranchCiId = envVariables.economic_branch_based_on_commercial_information as number;
            const economicBranchNceaId = envVariables.economic_branch_according_to_national_classification as number;

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

        test.info().attach('[REG-1311] TC-BE-3 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

});

