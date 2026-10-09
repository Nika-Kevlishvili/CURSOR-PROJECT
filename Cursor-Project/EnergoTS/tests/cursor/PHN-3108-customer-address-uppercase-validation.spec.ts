import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import { envVariables } from '../../fixtures/envCashed';

/**
 * PHN-3149 — Customer main address and communication data address uppercase/transliteration validation.
 * Maps 1:1 to test_cases/Backend/Customer_Address_Uppercase_Validation.md (TC-BE-1 … TC-BE-33).
 * Endpoints: POST /customer, PUT /customer/{id}
 * Environment: Dev2
 *
 * Reference spec(s):
 * - tests/cursor/PHN-2211-additional-contact-persons.spec.ts
 * - tests/cursor/PHN-2202-retrieve-customer-data-by-uic.spec.ts
 * - tests/cursor/PHN-2865-manager-phones-emails.spec.ts
 */

// ═══ HELPER FUNCTIONS ═══

const UPPER_FOREIGN_TEXT = {
    region: 'SOFIA REGION',
    municipality: 'SOFIA MUNICIPALITY',
    populatedPlace: 'SOFIA',
    zipCode: '1000',
    district: 'CENTER',
    residentialAreaType: 'QUARTER' as const,
    residentialArea: 'MLADOST',
    streetType: 'STREET' as const,
    street: 'VITOSHA',
};

function unregisteredForeignAddressData(countryId: number, overrides: Record<string, unknown> = {}) {
    return {
        countryId,
        ...UPPER_FOREIGN_TEXT,
        ...overrides,
    };
}

function unregisteredMainAddress(countryId: number = envVariables.countries) {
    return {
        foreign: true,
        foreignAddressData: unregisteredForeignAddressData(countryId),
        number: '12',
        block: 'A',
        entrance: 'B',
        floor: '3',
        apartment: '15',
        mailbox: 'MB-1',
        additionalInformation: 'NEAR PARK',
    };
}

function registeredLocalAddressData() {
    return {
        countryId: envVariables.countries,
        regionId: envVariables.regions,
        municipalityId: envVariables.municipalities,
        populatedPlaceId: envVariables.population_places,
        zipCodeId: envVariables.zip_codes,
        districtId: envVariables.districts,
        residentialAreaId: envVariables.residential_areas,
        streetId: envVariables.streets,
        streetType: 'STREET' as const,
        residentialAreaType: 'QUARTER' as const,
    };
}

function registeredMainAddress() {
    return {
        foreign: false,
        localAddressData: registeredLocalAddressData(),
        number: '12',
        block: 'A',
        entrance: 'B',
        floor: '3',
        apartment: '15',
        mailbox: 'MB-1',
        additionalInformation: 'NEAR PARK',
    };
}

function unregisteredCommAddress(countryId: number = envVariables.countries) {
    return unregisteredMainAddress(countryId);
}

const STANDARD_TRANSL_LOCAL = {
    country: 'STANDARD COUNTRY',
    region: 'STANDARD REGION',
    municipality: 'STANDARD MUNICIPALITY',
    populatedPlace: 'STANDARD POPULATED PLACE',
    zipCode: 'STANDARD ZIP CODE',
    district: 'STANDARD DISTRICT',
    residentialArea: 'STANDARD RESIDENTIAL AREA',
    residentialAreaType: 'QUARTER' as const,
    street: 'STANDARD STREET',
    streetType: 'STREET' as const,
};

function pickAddressDetailFields(address: Record<string, any>) {
    return {
        number: address.number ?? '12',
        block: address.block ?? 'A',
        entrance: address.entrance ?? 'B',
        floor: address.floor ?? '3',
        apartment: address.apartment ?? '15',
        mailbox: address.mailbox ?? 'MB-1',
        additionalInformation: address.additionalInformation ?? 'NEAR PARK',
    };
}

/** UI always sends transliterated block alongside address (mandatory paired fields). */
function transliteratedFromAddress(address: Record<string, any>, countryName = 'STANDARD COUNTRY') {
    if (address.foreign) {
        const fd = address.foreignAddressData ?? {};
        return {
            foreign: true,
            foreignAddressData: {
                country: countryName,
                region: fd.region ?? UPPER_FOREIGN_TEXT.region,
                municipality: fd.municipality ?? UPPER_FOREIGN_TEXT.municipality,
                populatedPlace: fd.populatedPlace ?? UPPER_FOREIGN_TEXT.populatedPlace,
                zipCode: fd.zipCode ?? UPPER_FOREIGN_TEXT.zipCode,
                district: fd.district ?? UPPER_FOREIGN_TEXT.district,
                residentialAreaType: fd.residentialAreaType ?? UPPER_FOREIGN_TEXT.residentialAreaType,
                residentialArea: fd.residentialArea ?? UPPER_FOREIGN_TEXT.residentialArea,
                streetType: fd.streetType ?? UPPER_FOREIGN_TEXT.streetType,
                street: fd.street ?? UPPER_FOREIGN_TEXT.street,
            },
            ...pickAddressDetailFields(address),
        };
    }
    return {
        foreign: false,
        localAddressData: { ...STANDARD_TRANSL_LOCAL },
        ...pickAddressDetailFields(address),
    };
}

function assignMainAddress(payload: Record<string, any>, address: Record<string, any>, countryName = 'STANDARD COUNTRY') {
    payload.address = address;
    payload.addressTransl = transliteratedFromAddress(address, countryName);
}

function assignCommAddress(
    payload: Record<string, any>,
    commAddress: Record<string, any>,
    index = 0,
    countryName = 'STANDARD COUNTRY',
) {
    payload.communicationData[index].address = commAddress;
    payload.communicationData[index].addressTransliterated = transliteratedFromAddress(commAddress, countryName);
}

function withCommAddressTransliterated(commEntry: Record<string, any>, countryName = 'STANDARD COUNTRY') {
    if (commEntry.address && commEntry.addressTransliterated === undefined) {
        commEntry.addressTransliterated = transliteratedFromAddress(commEntry.address, countryName);
    }
    return commEntry;
}

function extractCreateContext(body: Record<string, any>) {
    const customerId = body.id ?? body.customerId;
    return { customerId };
}

/** GET /customer/{id} nests editable state under customerDetails[] or activeCustomerDetail. */
function resolveCustomerDetail(view: Record<string, any>): Record<string, any> | null {
    if (view.activeCustomerDetail) return view.activeCustomerDetail;
    if (view.customerDetail) return view.customerDetail;
    if (Array.isArray(view.customerDetails) && view.customerDetails.length > 0) {
        const list = view.customerDetails as Record<string, any>[];
        const active = list.find((x) => x.active === true || x.current === true);
        if (active) return active;
        return list.reduce((a, b) => ((b.versionId ?? 0) > (a.versionId ?? 0) ? b : a));
    }
    if (Array.isArray(view.managers) && view.versionId != null) {
        return view;
    }
    return null;
}

function managersFromCustomerBody(body: Record<string, any>): Record<string, any>[] {
    const root = Array.isArray(body.managers) ? body.managers : [];
    const detail = resolveCustomerDetail(body);
    const nested = Array.isArray(detail?.managers) ? detail.managers : [];
    const maxLen = Math.max(root.length, nested.length);
    if (maxLen === 0) return [];

    return Array.from({ length: maxLen }, (_, i) => ({
        ...(nested[i] ?? {}),
        ...(root[i] ?? {}),
    }));
}

function extractEditContext(created: Record<string, any>, getBody: Record<string, any>) {
    const customerId = created.id ?? created.customerId ?? getBody.id ?? getBody.customerId;
    const detail = resolveCustomerDetail(getBody);
    const detailsVersion = detail?.versionId;
    if (detailsVersion == null) {
        throw new Error(
            `buildEditPayload: missing customerDetailsVersion (versionId) in GET /customer/${customerId}`,
        );
    }

    const commFromGet = getBody.communicationData ?? detail?.communicationData;
    const commDataId = Array.isArray(commFromGet) ? commFromGet[0]?.id : undefined;
    return { customerId, detailsVersion, commDataId, getManagers: managersFromCustomerBody(getBody) };
}

function buildEditPayload(
    createPayload: Record<string, any>,
    created: Record<string, any>,
    getBody: Record<string, any>,
    patches: Record<string, any> = {},
) {
    const { customerId, detailsVersion, commDataId, getManagers } = extractEditContext(created, getBody);
    const editPayload: Record<string, any> = {
        ...createPayload,
        ...patches,
        customerDetailsVersion: detailsVersion,
        updateExistingVersion: true,
        customerIdentifier: getBody.identifier ?? created.identifier ?? createPayload.customerIdentifier,
    };

    if (patches.address && patches.addressTransl === undefined) {
        editPayload.addressTransl = transliteratedFromAddress(patches.address);
    }

    if (Array.isArray(editPayload.managers) && getManagers.length > 0) {
        editPayload.managers = editPayload.managers.map((mgr: Record<string, any>, idx: number) => ({
            ...mgr,
            id: getManagers[idx]?.id ?? mgr.id,
        }));
    }

    if (Array.isArray(editPayload.communicationData)) {
        editPayload.communicationData = editPayload.communicationData.map((comm: any, idx: number) => {
            const merged = withCommAddressTransliterated({ ...comm });
            if (idx === 0 && commDataId != null) {
                merged.id = commDataId;
            }
            return merged;
        });
    }

    return { editPayload, customerId, detailsVersion, commDataId };
}

async function fetchCustomerView(Request: any, Endpoints: any, customerId: number): Promise<Record<string, any>> {
    const response = await Request.get(`${Endpoints.customer}/${customerId}`);
    await checkResponse(response);
    return response.json();
}

// ── Structured customer API logging (PHN-2202 pattern) ─────────────────────

type VerifiedCheck = { field: string; expected: unknown; actual: unknown };

function logSection(title: string): void {
    console.log(`\n${'═'.repeat(78)}\n${title}\n${'─'.repeat(78)}`);
}

function logExpected(tcId: string, stepLabel: string, endpoint: string, expected: unknown): void {
    logSection(`[${tcId}] ${stepLabel}\n▶ EXPECTED (before ${endpoint})`);
    console.log(JSON.stringify(expected, null, 2));
}

function logActual(tcId: string, stepLabel: string, status: number, body: unknown): void {
    logSection(`[${tcId}] ${stepLabel}\n▶ ACTUAL RESPONSE (HTTP ${status})`);
    console.log(JSON.stringify(body, null, 2));
}

function logVerified(tcId: string, stepLabel: string, checks: VerifiedCheck[]): void {
    logSection(`[${tcId}] ${stepLabel}\n▶ VERIFIED — matched expected values`);
    for (const check of checks) {
        const pass = JSON.stringify(check.expected) === JSON.stringify(check.actual);
        console.log(`  ${pass ? '✓' : '✗'} ${check.field}: ${JSON.stringify(check.actual)}`);
    }
    console.log(`${'═'.repeat(78)}\n`);
}

function buildCustomerExpectation(
    scenario: string,
    method: 'POST' | 'PUT' | 'GET',
    path: string,
    requestBody: unknown,
    responseShouldInclude: Record<string, unknown>,
    Responses: any,
    httpStatus?: number,
): Record<string, unknown> {
    return {
        scenario,
        request: { method, path, ...(requestBody != null ? { body: requestBody } : {}) },
        responseShouldInclude,
        createdEntities: reportGenerator.setLinksToResponses(Responses),
        ...(httpStatus != null ? { httpStatus } : {}),
    };
}

async function parseResponseBody(response: any): Promise<any> {
    try {
        return await response.json();
    } catch {
        try {
            return await response.text();
        } catch {
            return null;
        }
    }
}

async function checkResponse(response: any): Promise<void> {
    await (expect(response) as any).CheckResponse();
}

async function performPostCustomer(opts: {
    tcId: string;
    stepLabel: string;
    Request: any;
    Endpoints: any;
    payload: any;
    Responses: any;
    scenario: string;
    responseShouldInclude: Record<string, unknown>;
    expectedStatus?: number;
    errorContains?: string;
    pushToResponses?: boolean;
    verify?: (body: any, response: any) => VerifiedCheck[] | Promise<VerifiedCheck[]>;
}): Promise<{ response: any; body: any }> {
    const expectedStatus = opts.expectedStatus ?? (opts.errorContains ? 400 : 201);
    const expectation = buildCustomerExpectation(
        opts.scenario,
        'POST',
        '/customer',
        opts.payload,
        {
            ...opts.responseShouldInclude,
            ...(opts.errorContains ? { errorBodyShouldContain: opts.errorContains } : {}),
        },
        opts.Responses,
        expectedStatus,
    );
    logExpected(opts.tcId, opts.stepLabel, 'POST /customer', expectation);

    const response = await opts.Request.post(opts.Endpoints.customer, { data: opts.payload });
    const status = response.status();
    const body = await parseResponseBody(response);
    logActual(opts.tcId, opts.stepLabel, status, body);

    const checks: VerifiedCheck[] = [{ field: 'httpStatus', expected: expectedStatus, actual: status }];

    if (opts.errorContains) {
        expect(status).toBe(400);
        const bodyStr = typeof body === 'string' ? body : JSON.stringify(body);
        expect(bodyStr).toContain(opts.errorContains);
        checks.push({ field: 'errorBodyContains', expected: opts.errorContains, actual: opts.errorContains });
    } else {
        await checkResponse(response);
        if (opts.verify) {
            checks.push(...(await opts.verify(body, response)));
        }
        if (opts.pushToResponses !== false) {
            opts.Responses.customer.push(body);
        }
    }

    logVerified(opts.tcId, opts.stepLabel, checks);
    return { response, body };
}

async function performPutCustomer(opts: {
    tcId: string;
    stepLabel: string;
    Request: any;
    Endpoints: any;
    customerId: number;
    editPayload: any;
    Responses: any;
    scenario: string;
    responseShouldInclude: Record<string, unknown>;
    expectedStatus?: number;
    errorContains?: string;
    verify?: (body: any, response: any) => VerifiedCheck[] | Promise<VerifiedCheck[]>;
}): Promise<{ response: any; body: any }> {
    const path = `/customer/${opts.customerId}`;
    const expectedStatus = opts.expectedStatus ?? (opts.errorContains ? 400 : 200);
    const expectation = buildCustomerExpectation(
        opts.scenario,
        'PUT',
        path,
        opts.editPayload,
        {
            ...opts.responseShouldInclude,
            ...(opts.errorContains ? { errorBodyShouldContain: opts.errorContains } : {}),
        },
        opts.Responses,
        expectedStatus,
    );
    logExpected(opts.tcId, opts.stepLabel, `PUT ${path}`, expectation);

    const response = await opts.Request.put(`${opts.Endpoints.customer}/${opts.customerId}`, {
        data: opts.editPayload,
    });
    const status = response.status();
    const body = await parseResponseBody(response);
    logActual(opts.tcId, opts.stepLabel, status, body);

    const checks: VerifiedCheck[] = [{ field: 'httpStatus', expected: expectedStatus, actual: status }];

    if (opts.errorContains) {
        expect(status).toBe(400);
        const bodyStr = typeof body === 'string' ? body : JSON.stringify(body);
        expect(bodyStr).toContain(opts.errorContains);
        checks.push({ field: 'errorBodyContains', expected: opts.errorContains, actual: opts.errorContains });
    } else {
        await checkResponse(response);
        if (opts.verify) {
            checks.push(...(await opts.verify(body, response)));
        }
    }

    logVerified(opts.tcId, opts.stepLabel, checks);
    return { response, body };
}

async function performGetCustomer(opts: {
    tcId: string;
    stepLabel: string;
    Request: any;
    Endpoints: any;
    customerId: number;
    Responses: any;
    scenario: string;
    responseShouldInclude: Record<string, unknown>;
    verify: (view: any, response: any) => VerifiedCheck[] | Promise<VerifiedCheck[]>;
}): Promise<{ response: any; body: any }> {
    const path = `/customer/${opts.customerId}`;
    const expectation = buildCustomerExpectation(
        opts.scenario,
        'GET',
        path,
        null,
        opts.responseShouldInclude,
        opts.Responses,
        200,
    );
    logExpected(opts.tcId, opts.stepLabel, `GET ${path}`, expectation);

    const response = await opts.Request.get(`${opts.Endpoints.customer}/${opts.customerId}`);
    const status = response.status();
    const body = await parseResponseBody(response);
    logActual(opts.tcId, opts.stepLabel, status, body);

    await checkResponse(response);
    const checks: VerifiedCheck[] = [
        { field: 'httpStatus', expected: 200, actual: status },
        ...(await opts.verify(body, response)),
    ];
    logVerified(opts.tcId, opts.stepLabel, checks);
    return { response, body };
}

function attachTcReport(tc: string, Responses: any) {
    const createdEntities = reportGenerator.setLinksToResponses(Responses);
    logSection(`[${tc}] Generated objects`);
    console.log(JSON.stringify(createdEntities, null, 2));
    console.log(`${'═'.repeat(78)}\n`);

    test.info().attach(`[PHN-3149] ${tc} response`, {
        body: JSON.stringify(createdEntities, null, 2),
        contentType: 'application/json',
    });
}

function isFlatCustomerView(view: Record<string, any>): boolean {
    return (
        view.address == null &&
        (view.foreignAddress != null ||
            view.streetId != null ||
            view.streetForeign != null ||
            view.populatedPlaceId != null ||
            view.populatedPlaceForeign != null)
    );
}

/** Dev2 GET /customer/{id} returns flat CustomerViewResponse fields at root (PHN-2202 pattern). */
function mainAddressFromFlatCustomerView(view: Record<string, any>): Record<string, any> {
    const foreign = view.foreignAddress ?? false;
    const detailFields = {
        number: view.streetNumber ?? null,
        block: view.block ?? null,
        entrance: view.entrance ?? null,
        floor: view.floor ?? null,
        apartment: view.apartment ?? null,
        mailbox: view.mailbox ?? null,
        additionalInformation: view.addressAdditionalInfo ?? null,
    };

    if (foreign) {
        return {
            foreign: true,
            foreignAddressData: {
                countryId: view.countryId?.id ?? view.countryId ?? null,
                region: view.regionForeign ?? null,
                municipality: view.municipalityForeign ?? null,
                populatedPlace: view.populatedPlaceForeign ?? null,
                zipCode: view.zipCodeForeign ?? null,
                district: view.districtForeign ?? null,
                residentialAreaType: view.residentialAreaTypeForeign ?? null,
                residentialArea: view.residentialAreaForeign ?? null,
                streetType: view.streetTypeForeign ?? null,
                street: view.streetForeign ?? null,
            },
            ...detailFields,
        };
    }

    return {
        foreign: false,
        localAddressData: {
            countryId: view.countryId?.id ?? view.countryId ?? null,
            regionId: view.localRegion?.id ?? view.localRegion ?? null,
            municipalityId: view.localMunicipality?.id ?? view.localMunicipality ?? null,
            populatedPlaceId: view.populatedPlaceId?.id ?? view.populatedPlaceId ?? null,
            zipCodeId: view.zipCode?.id ?? view.zipCode ?? null,
            districtId: view.districtId?.id ?? view.districtId ?? null,
            residentialAreaId: view.residentialAreaId?.id ?? view.residentialAreaId ?? null,
            streetId: view.streetId?.id ?? view.streetId ?? null,
            streetType: view.streetType ?? null,
            residentialAreaType: view.residentialAreaType ?? null,
        },
        ...detailFields,
    };
}

function resolveMainAddress(view: Record<string, any>): Record<string, any> | undefined {
    if (view.address) return view.address;
    const detail = view.activeCustomerDetail ?? view.customerDetail;
    if (detail?.address) return detail.address;
    if (Array.isArray(view.customerDetails) && view.customerDetails.length > 0) {
        const active = view.customerDetails.find((d: any) => d.active === true || d.current === true);
        const nested = (active ?? view.customerDetails[0])?.address;
        if (nested) return nested;
    }
    if (isFlatCustomerView(view)) {
        return mainAddressFromFlatCustomerView(view);
    }
    return undefined;
}

function resolveCommAddress(view: Record<string, any>): Record<string, any> | undefined {
    const comm =
        view.communicationData ??
        view.activeCustomerDetail?.communicationData ??
        view.customerDetail?.communicationData;
    if (Array.isArray(comm) && comm.length > 0) {
        const nested = comm[0]?.address;
        if (nested) return nested;
    }
    return undefined;
}

function mainAddressStreetValue(addr: Record<string, any> | undefined): unknown {
    if (!addr) return undefined;
    if (addr.foreignAddressData?.street != null) return addr.foreignAddressData.street;
    const streetId = addr.localAddressData?.streetId;
    if (streetId != null && typeof streetId === 'object') {
        return (streetId as Record<string, unknown>).name ?? (streetId as Record<string, unknown>).id;
    }
    return streetId;
}

// ═══ TEST DESCRIBE ═══

test.describe('[PHN-3149]: Customer address uppercase validation', { tag: '@customers' }, () => {
    test.describe.configure({ mode: 'serial' });

    test('[PHN-3149]: TC-BE-1 – Create LEGAL_ENTITY — unregistered main address, all free-text fields uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(payload, unregisteredMainAddress());

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-1',
                stepLabel: 'POST /customer — unregistered uppercase main address',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create LEGAL_ENTITY — unregistered main address, all free-text fields uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    customerType: 'LEGAL_ENTITY',
                    'address.foreign': true,
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    { field: 'address.foreign', expected: true, actual: body.address?.foreign },
                ],
            });
        });

        attachTcReport('TC-BE-1', Responses);
    });

    test('[PHN-3149]: TC-BE-2 – Create LEGAL_ENTITY — registered main address, detail fields uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(payload, registeredMainAddress());

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-2',
                stepLabel: 'POST /customer — registered uppercase detail fields',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create LEGAL_ENTITY — registered main address, detail fields uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    customerType: 'LEGAL_ENTITY',
                    'address.foreign': false,
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    { field: 'address.foreign', expected: false, actual: body.address?.foreign },
                ],
            });
        });

        attachTcReport('TC-BE-2', Responses);
    });

    test('[PHN-3149]: TC-BE-3 – Create LEGAL_ENTITY — transliterated main address uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-3',
                stepLabel: 'POST /customer — transliterated main address uppercase',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create LEGAL_ENTITY — transliterated main address uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    customerType: 'LEGAL_ENTITY',
                    addressTransl: 'present (uppercase)',
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    { field: 'addressTransl', expected: 'present', actual: body.addressTransl ? 'present' : null },
                ],
            });
        });

        attachTcReport('TC-BE-3', Responses);
    });

    test('[PHN-3149]: TC-BE-4 – Create LEGAL_ENTITY — unregistered main address, lowercase in region', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries, { region: 'sofia region' }),
            number: '12',
        });

        await test.step('POST /customer with lowercase region — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-4',
                stepLabel: 'POST /customer with lowercase region — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create LEGAL_ENTITY — unregistered main address, lowercase in region rejected',
                responseShouldInclude: {
                    httpStatus: 400,
                    customerCreated: false,
                },
                errorContains: 'address.foreignAddressData.region-Allowed Symbols',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-4', Responses);
    });

    test('[PHN-3149]: TC-BE-5 – Create LEGAL_ENTITY — registered main address, lowercase in number', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(payload, registeredMainAddress());
        payload.address.number = '12a';

        await test.step('POST /customer with lowercase number — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-5',
                stepLabel: 'POST /customer with lowercase number — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create LEGAL_ENTITY — registered main address, lowercase in number rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'address.number-Allowed Symbols',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-5', Responses);
    });

    test('[PHN-3149]: TC-BE-6 – Create LEGAL_ENTITY — lowercase in transliterated main address', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });
        payload.addressTransl.foreignAddressData.region = 'sofia region';

        await test.step('POST /customer with lowercase addressTransl.region — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-6',
                stepLabel: 'POST /customer with lowercase addressTransl.region — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create LEGAL_ENTITY — lowercase in transliterated main address rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'addressTransl.foreignAddressData.region-Transliteration field must be uppercase',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-6', Responses);
    });

    test('[PHN-3149]: TC-BE-7 – Create PRIVATE_CUSTOMER — unregistered main address, all text fields uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, unregisteredMainAddress());

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-7',
                stepLabel: 'POST /customer — unregistered uppercase main address',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER — unregistered main address, all text fields uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    customerType: 'PRIVATE_CUSTOMER',
                    'address.foreign': true,
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    { field: 'address.foreign', expected: true, actual: body.address?.foreign },
                ],
            });
        });

        attachTcReport('TC-BE-7', Responses);
    });

    test('[PHN-3149]: TC-BE-8 – Create PRIVATE_CUSTOMER — registered main address, detail fields uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, registeredMainAddress());

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-8',
                stepLabel: 'POST /customer — registered uppercase detail fields',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER — registered main address, detail fields uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    customerType: 'PRIVATE_CUSTOMER',
                    'address.foreign': false,
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    { field: 'address.foreign', expected: false, actual: body.address?.foreign },
                ],
            });
        });

        attachTcReport('TC-BE-8', Responses);
    });

    test('[PHN-3149]: TC-BE-9 – Create PRIVATE_CUSTOMER — transliterated main address uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-9',
                stepLabel: 'POST /customer — transliterated main address uppercase',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER — transliterated main address uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    customerType: 'PRIVATE_CUSTOMER',
                    addressTransl: 'present (uppercase)',
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    { field: 'addressTransl', expected: 'present', actual: body.addressTransl ? 'present' : null },
                ],
            });
        });

        attachTcReport('TC-BE-9', Responses);
    });

    test('[PHN-3149]: TC-BE-10 – Create PRIVATE_CUSTOMER — unregistered main address, lowercase in district', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries, { district: 'center' }),
            number: '12',
        });

        await test.step('POST /customer with lowercase district — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-10',
                stepLabel: 'POST /customer with lowercase district — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER — unregistered main address, lowercase in district rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'address.foreignAddressData.district-Allowed Symbols',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-10', Responses);
    });

    test('[PHN-3149]: TC-BE-11 – Create PRIVATE_CUSTOMER — registered main address, lowercase in block', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, registeredMainAddress());
        payload.address.block = 'a';

        await test.step('POST /customer with lowercase block — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-11',
                stepLabel: 'POST /customer with lowercase block — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER — registered main address, lowercase in block rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'address.block-Allowed Symbols',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-11', Responses);
    });

    test('[PHN-3149]: TC-BE-12 – Create PRIVATE_CUSTOMER — lowercase in transliterated main address', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });
        payload.addressTransl.foreignAddressData.country = 'bulgaria';

        await test.step('POST /customer with lowercase addressTransl.country — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-12',
                stepLabel: 'POST /customer with lowercase addressTransl.country — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER — lowercase in transliterated main address rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'addressTransl.foreignAddressData.country-Transliteration field must be uppercase',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-12', Responses);
    });

    test('[PHN-3149]: TC-BE-13 – Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — unregistered main address uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private_business();
        assignMainAddress(payload, unregisteredMainAddress());

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-13',
                stepLabel: 'POST /customer — unregistered uppercase main address',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — unregistered main address uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    customerType: 'PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY',
                    'address.foreign': true,
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    { field: 'address.foreign', expected: true, actual: body.address?.foreign },
                ],
            });
        });

        attachTcReport('TC-BE-13', Responses);
    });

    test('[PHN-3149]: TC-BE-14 – Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — registered main address, detail fields uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private_business();
        assignMainAddress(payload, registeredMainAddress());

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-14',
                stepLabel: 'POST /customer — registered uppercase detail fields',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — registered main address, detail fields uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    customerType: 'PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY',
                    'address.foreign': false,
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    { field: 'address.foreign', expected: false, actual: body.address?.foreign },
                ],
            });
        });

        attachTcReport('TC-BE-14', Responses);
    });

    test('[PHN-3149]: TC-BE-15 – Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — transliterated main address uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private_business();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-15',
                stepLabel: 'POST /customer — transliterated main address uppercase',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — transliterated main address uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    customerType: 'PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY',
                    addressTransl: 'present (uppercase)',
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    { field: 'addressTransl', expected: 'present', actual: body.addressTransl ? 'present' : null },
                ],
            });
        });

        attachTcReport('TC-BE-15', Responses);
    });

    test('[PHN-3149]: TC-BE-16 – Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — unregistered, lowercase in municipality', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private_business();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries, {
                municipality: 'sofia municipality',
            }),
            number: '12',
        });

        await test.step('POST /customer with lowercase municipality — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-16',
                stepLabel: 'POST /customer with lowercase municipality — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — unregistered, lowercase in municipality rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'address.foreignAddressData.municipality-Allowed Symbols',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-16', Responses);
    });

    test('[PHN-3149]: TC-BE-17 – Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — registered, lowercase in entrance', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private_business();
        assignMainAddress(payload, registeredMainAddress());
        payload.address.entrance = 'b';

        await test.step('POST /customer with lowercase entrance — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-17',
                stepLabel: 'POST /customer with lowercase entrance — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — registered, lowercase in entrance rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'address.entrance-Allowed Symbols',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-17', Responses);
    });

    test('[PHN-3149]: TC-BE-18 – Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — lowercase in transliterated address', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private_business();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });
        payload.addressTransl.foreignAddressData.street = 'vitosha';

        await test.step('POST /customer with lowercase addressTransl.street — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-18',
                stepLabel: 'POST /customer with lowercase addressTransl.street — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY — lowercase in transliterated address rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'addressTransl.foreignAddressData.street-Transliteration field must be uppercase',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-18', Responses);
    });

    test('[PHN-3149]: TC-BE-19 – Edit customer main address — unregistered mode, uppercase text fields', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const createPayload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(createPayload, registeredMainAddress());
        createPayload.address.number = '12';

        let created: Record<string, any>;
        await test.step('Precondition: create LEGAL_ENTITY with registered address', async () => {
            const { body } = await performPostCustomer({
                tcId: 'TC-BE-19',
                stepLabel: 'Precondition: create LEGAL_ENTITY with registered address',
                Request,
                Endpoints,
                payload: createPayload,
                Responses,
                scenario: 'Precondition — LEGAL_ENTITY with registered main address for edit test',
                responseShouldInclude: { httpStatus: 201, customerType: 'LEGAL_ENTITY', 'address.foreign': false },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                ],
            });
            created = body;
        });

        await test.step('PUT /customer/{id} — switch to unregistered uppercase address', async () => {
            const customerId = extractCreateContext(created).customerId;
            const getBody = await fetchCustomerView(Request, Endpoints, customerId);
            const { editPayload } = buildEditPayload(createPayload, created, getBody, {
                address: unregisteredMainAddress(),
            });
            await performPutCustomer({
                tcId: 'TC-BE-19',
                stepLabel: 'PUT /customer/{id} — switch to unregistered uppercase address',
                Request,
                Endpoints,
                customerId,
                editPayload,
                Responses,
                scenario: 'Edit customer main address — unregistered mode, uppercase text fields',
                responseShouldInclude: { httpStatus: 200, 'address.foreign': true },
                verify: () => [{ field: 'address.foreign', expected: true, actual: true }],
            });
        });

        attachTcReport('TC-BE-19', Responses);
    });

    test('[PHN-3149]: TC-BE-20 – Edit customer main address — registered mode, uppercase detail fields', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const createPayload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(createPayload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });

        let created: Record<string, any>;
        await test.step('Precondition: create PRIVATE_CUSTOMER with unregistered address', async () => {
            const { body } = await performPostCustomer({
                tcId: 'TC-BE-20',
                stepLabel: 'Precondition: create PRIVATE_CUSTOMER with unregistered address',
                Request,
                Endpoints,
                payload: createPayload,
                Responses,
                scenario: 'Precondition — PRIVATE_CUSTOMER with unregistered main address for edit test',
                responseShouldInclude: { httpStatus: 201, customerType: 'PRIVATE_CUSTOMER', 'address.foreign': true },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                ],
            });
            created = body;
        });

        await test.step('PUT /customer/{id} — switch to registered uppercase detail fields', async () => {
            const customerId = extractCreateContext(created).customerId;
            const getBody = await fetchCustomerView(Request, Endpoints, customerId);
            const { editPayload } = buildEditPayload(createPayload, created, getBody, {
                address: registeredMainAddress(),
            });
            await performPutCustomer({
                tcId: 'TC-BE-20',
                stepLabel: 'PUT /customer/{id} — switch to registered uppercase detail fields',
                Request,
                Endpoints,
                customerId,
                editPayload,
                Responses,
                scenario: 'Edit customer main address — registered mode, uppercase detail fields',
                responseShouldInclude: { httpStatus: 200, 'address.foreign': false },
                verify: () => [{ field: 'address.foreign', expected: false, actual: false }],
            });
        });

        attachTcReport('TC-BE-20', Responses);
    });

    test('[PHN-3149]: TC-BE-21 – Edit customer main address — unregistered mode, lowercase in address field', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const createPayload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(createPayload, registeredMainAddress());
        createPayload.address.number = '12';

        let created: Record<string, any>;
        let customerId: number;
        await test.step('Precondition: create LEGAL_ENTITY with registered address', async () => {
            const { body } = await performPostCustomer({
                tcId: 'TC-BE-21',
                stepLabel: 'Precondition: create LEGAL_ENTITY with registered address',
                Request,
                Endpoints,
                payload: createPayload,
                Responses,
                scenario: 'Precondition — LEGAL_ENTITY with registered main address for rejected edit test',
                responseShouldInclude: { httpStatus: 201, customerType: 'LEGAL_ENTITY' },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                ],
            });
            created = body;
            customerId = extractCreateContext(created).customerId;
        });

        await test.step('PUT /customer/{id} with lowercase street — expect 400', async () => {
            const badAddress = {
                foreign: true,
                foreignAddressData: unregisteredForeignAddressData(envVariables.countries, { street: 'vitosha' }),
                number: '12',
            };
            const getBody = await fetchCustomerView(Request, Endpoints, customerId);
            const { editPayload } = buildEditPayload(createPayload, created, getBody, { address: badAddress });
            await performPutCustomer({
                tcId: 'TC-BE-21',
                stepLabel: 'PUT /customer/{id} with lowercase street — expect 400',
                Request,
                Endpoints,
                customerId,
                editPayload,
                Responses,
                scenario: 'Edit customer main address — unregistered mode, lowercase in address field rejected',
                responseShouldInclude: { httpStatus: 400, addressUnchanged: true },
                errorContains: 'address.foreignAddressData.street-Allowed Symbols',
            });
        });

        await test.step('GET /customer/{id} — main address unchanged', async () => {
            await performGetCustomer({
                tcId: 'TC-BE-21',
                stepLabel: 'GET /customer/{id} — main address unchanged',
                Request,
                Endpoints,
                customerId,
                Responses,
                scenario: 'After rejected PUT — main address must remain unchanged',
                responseShouldInclude: { httpStatus: 200, 'address.foreign': false, streetNot: 'vitosha' },
                verify: (view) => {
                    const addr = resolveMainAddress(view);
                    expect(addr?.foreign).toBe(false);
                    expect(mainAddressStreetValue(addr)).not.toBe('vitosha');
                    return [
                        { field: 'address.foreign', expected: false, actual: addr?.foreign },
                        {
                            field: 'address.street',
                            expected: '≠ vitosha',
                            actual: mainAddressStreetValue(addr),
                        },
                    ];
                },
            });
        });

        attachTcReport('TC-BE-21', Responses);
    });

    test('[PHN-3149]: TC-BE-22 – Edit customer main address — registered mode, lowercase in detail field', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const createPayload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(createPayload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });

        let created: Record<string, any>;
        let customerId: number;
        await test.step('Precondition: create PRIVATE_CUSTOMER with unregistered address', async () => {
            const { body } = await performPostCustomer({
                tcId: 'TC-BE-22',
                stepLabel: 'Precondition: create PRIVATE_CUSTOMER with unregistered address',
                Request,
                Endpoints,
                payload: createPayload,
                Responses,
                scenario: 'Precondition — PRIVATE_CUSTOMER with unregistered main address for rejected edit test',
                responseShouldInclude: { httpStatus: 201, customerType: 'PRIVATE_CUSTOMER' },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                ],
            });
            created = body;
            customerId = extractCreateContext(created).customerId;
        });

        await test.step('PUT /customer/{id} with lowercase floor — expect 400', async () => {
            const badAddress = {
                foreign: false,
                localAddressData: registeredLocalAddressData(),
                number: '12',
                floor: '3rd',
            };
            const getBody = await fetchCustomerView(Request, Endpoints, customerId);
            const { editPayload } = buildEditPayload(createPayload, created, getBody, { address: badAddress });
            await performPutCustomer({
                tcId: 'TC-BE-22',
                stepLabel: 'PUT /customer/{id} with lowercase floor — expect 400',
                Request,
                Endpoints,
                customerId,
                editPayload,
                Responses,
                scenario: 'Edit customer main address — registered mode, lowercase in detail field rejected',
                responseShouldInclude: { httpStatus: 400, floorUnchanged: true },
                errorContains: 'address.floor-Allowed Symbols',
            });
        });

        await test.step('GET /customer/{id} — floor unchanged', async () => {
            await performGetCustomer({
                tcId: 'TC-BE-22',
                stepLabel: 'GET /customer/{id} — floor unchanged',
                Request,
                Endpoints,
                customerId,
                Responses,
                scenario: 'After rejected PUT — floor must remain unchanged',
                responseShouldInclude: { httpStatus: 200, floorNot: '3rd' },
                verify: (view) => {
                    const addr = resolveMainAddress(view);
                    expect(addr?.floor).not.toBe('3rd');
                    return [
                        { field: 'address.floor', expected: '≠ 3rd', actual: addr?.floor },
                    ];
                },
            });
        });

        attachTcReport('TC-BE-22', Responses);
    });

    test('[PHN-3149]: TC-BE-23 – Create customer — communication data address unregistered, all text uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, registeredMainAddress());
        payload.address.number = '12';
        assignCommAddress(payload, unregisteredCommAddress());

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-23',
                stepLabel: 'POST /customer — comm data unregistered uppercase',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create customer — communication data address unregistered, all text uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    'communicationData[0].address.foreign': true,
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    {
                        field: 'communicationData[0].address.foreign',
                        expected: true,
                        actual: body.communicationData?.[0]?.address?.foreign,
                    },
                ],
            });
        });

        attachTcReport('TC-BE-23', Responses);
    });

    test('[PHN-3149]: TC-BE-24 – Create customer — communication data address registered, detail fields uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, registeredMainAddress());
        payload.address.number = '12';
        assignCommAddress(payload, registeredMainAddress());

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-24',
                stepLabel: 'POST /customer — comm data registered uppercase detail fields',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create customer — communication data address registered, detail fields uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    'communicationData[0].address.foreign': false,
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    {
                        field: 'communicationData[0].address.foreign',
                        expected: false,
                        actual: body.communicationData?.[0]?.address?.foreign,
                    },
                ],
            });
        });

        attachTcReport('TC-BE-24', Responses);
    });

    test('[PHN-3149]: TC-BE-25 – Create customer — transliterated communication address uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(payload, registeredMainAddress());
        payload.address.number = '12';
        assignCommAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });

        await test.step('POST /customer — expect 201', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-25',
                stepLabel: 'POST /customer — transliterated comm address uppercase',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create customer — transliterated communication address uppercase',
                responseShouldInclude: {
                    httpStatus: 201,
                    'communicationData[0].addressTransliterated': 'present (uppercase)',
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                    {
                        field: 'communicationData[0].addressTransliterated',
                        expected: 'present',
                        actual: body.communicationData?.[0]?.addressTransliterated ? 'present' : null,
                    },
                ],
            });
        });

        attachTcReport('TC-BE-25', Responses);
    });

    test('[PHN-3149]: TC-BE-26 – Create customer — comm data address unregistered, lowercase in region', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, registeredMainAddress());
        payload.address.number = '12';
        assignCommAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries, { region: 'sofia region' }),
            number: '12',
        });

        await test.step('POST /customer with lowercase comm region — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-26',
                stepLabel: 'POST /customer with lowercase comm region — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create customer — comm data address unregistered, lowercase in region rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'communicationData[0].address.foreignAddressData.region-Allowed Symbols',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-26', Responses);
    });

    test('[PHN-3149]: TC-BE-27 – Create customer — comm data address registered, lowercase in number', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, registeredMainAddress());
        payload.address.number = '12';
        assignCommAddress(payload, registeredMainAddress());
        payload.communicationData[0].address.number = '12a';

        await test.step('POST /customer with lowercase comm number — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-27',
                stepLabel: 'POST /customer with lowercase comm number — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create customer — comm data address registered, lowercase in number rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains: 'communicationData[0].address.number-Allowed Symbols',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-27', Responses);
    });

    test('[PHN-3149]: TC-BE-28 – Create customer — lowercase in transliterated comm address', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_legal();
        assignMainAddress(payload, registeredMainAddress());
        payload.address.number = '12';
        assignCommAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries),
            number: '12',
        });
        payload.communicationData[0].addressTransliterated.foreignAddressData.region = 'sofia region';

        await test.step('POST /customer with lowercase comm addressTransliterated.region — expect 400', async () => {
            await performPostCustomer({
                tcId: 'TC-BE-28',
                stepLabel: 'POST /customer with lowercase comm addressTransliterated.region — expect 400',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Create customer — lowercase in transliterated comm address rejected',
                responseShouldInclude: { httpStatus: 400, customerCreated: false },
                errorContains:
                    'communicationData[0].addressTransliterated.foreignAddressData.region-Transliteration field must be uppercase',
                pushToResponses: false,
            });
        });

        attachTcReport('TC-BE-28', Responses);
    });

    test('[PHN-3149]: TC-BE-29 – Edit customer communication data address — unregistered mode, uppercase', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const createPayload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(createPayload, registeredMainAddress());
        createPayload.address.number = '12';
        assignCommAddress(createPayload, registeredMainAddress());
        createPayload.communicationData[0].address.number = '12';

        let created: Record<string, any>;
        await test.step('Precondition: create customer with registered comm address', async () => {
            const { body } = await performPostCustomer({
                tcId: 'TC-BE-29',
                stepLabel: 'Precondition: create customer with registered comm address',
                Request,
                Endpoints,
                payload: createPayload,
                Responses,
                scenario: 'Precondition — customer with registered comm address for edit test',
                responseShouldInclude: { httpStatus: 201, 'communicationData[0].address.foreign': false },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                ],
            });
            created = body;
        });

        await test.step('PUT /customer/{id} — comm address unregistered uppercase', async () => {
            const customerId = extractCreateContext(created).customerId;
            const getBody = await fetchCustomerView(Request, Endpoints, customerId);
            const commEntry = {
                ...createPayload.communicationData[0],
                address: unregisteredCommAddress(),
            };
            const { editPayload } = buildEditPayload(createPayload, created, getBody, {
                communicationData: [commEntry],
            });
            await performPutCustomer({
                tcId: 'TC-BE-29',
                stepLabel: 'PUT /customer/{id} — comm address unregistered uppercase',
                Request,
                Endpoints,
                customerId,
                editPayload,
                Responses,
                scenario: 'Edit customer communication data address — unregistered mode, uppercase',
                responseShouldInclude: { httpStatus: 200, 'communicationData[0].address.foreign': true },
                verify: () => [
                    { field: 'communicationData[0].address.foreign', expected: true, actual: true },
                ],
            });
        });

        attachTcReport('TC-BE-29', Responses);
    });

    test('[PHN-3149]: TC-BE-30 – Edit customer communication data address — lowercase rejected', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const createPayload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(createPayload, registeredMainAddress());
        createPayload.address.number = '12';
        assignCommAddress(createPayload, registeredMainAddress());
        createPayload.communicationData[0].address.number = '12';

        let created: Record<string, any>;
        let customerId: number;
        await test.step('Precondition: create customer with registered comm address', async () => {
            const { body } = await performPostCustomer({
                tcId: 'TC-BE-30',
                stepLabel: 'Precondition: create customer with registered comm address',
                Request,
                Endpoints,
                payload: createPayload,
                Responses,
                scenario: 'Precondition — customer with registered comm address for rejected edit test',
                responseShouldInclude: { httpStatus: 201 },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                ],
            });
            created = body;
            customerId = extractCreateContext(created).customerId;
        });

        await test.step('PUT /customer/{id} with lowercase comm populatedPlace — expect 400', async () => {
            const commEntry = {
                ...createPayload.communicationData[0],
                address: {
                    foreign: true,
                    foreignAddressData: unregisteredForeignAddressData(envVariables.countries, {
                        populatedPlace: 'sofia',
                    }),
                    number: '12',
                },
            };
            const getBody = await fetchCustomerView(Request, Endpoints, customerId);
            const { editPayload } = buildEditPayload(createPayload, created, getBody, {
                communicationData: [commEntry],
            });
            await performPutCustomer({
                tcId: 'TC-BE-30',
                stepLabel: 'PUT /customer/{id} with lowercase comm populatedPlace — expect 400',
                Request,
                Endpoints,
                customerId,
                editPayload,
                Responses,
                scenario: 'Edit customer communication data address — lowercase rejected',
                responseShouldInclude: { httpStatus: 400, commAddressUnchanged: true },
                errorContains: 'communicationData[0].address.foreignAddressData.populatedPlace-Allowed Symbols',
            });
        });

        await test.step('GET /customer/{id} — comm populatedPlace unchanged', async () => {
            await performGetCustomer({
                tcId: 'TC-BE-30',
                stepLabel: 'GET /customer/{id} — comm populatedPlace unchanged',
                Request,
                Endpoints,
                customerId,
                Responses,
                scenario: 'After rejected PUT — comm populatedPlace must remain unchanged',
                responseShouldInclude: { httpStatus: 200, populatedPlaceNot: 'sofia' },
                verify: (view) => {
                    const commAddr = resolveCommAddress(view);
                    expect(commAddr?.foreignAddressData?.populatedPlace).not.toBe('sofia');
                    return [
                        {
                            field: 'communicationData[0].address.foreignAddressData.populatedPlace',
                            expected: '≠ sofia',
                            actual: commAddr?.foreignAddressData?.populatedPlace,
                        },
                    ];
                },
            });
        });

        attachTcReport('TC-BE-30', Responses);
    });

    test('[PHN-3149]: TC-BE-31 – Unregistered mode — countryId (integer) exempt from uppercase text validation', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_legal();
        const countryId = envVariables.countries;
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(countryId),
            number: '12',
        });

        let customerId: number;
        await test.step('POST /customer — expect 201 (integer countryId accepted)', async () => {
            const { body } = await performPostCustomer({
                tcId: 'TC-BE-31',
                stepLabel: 'POST /customer — integer countryId accepted',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Unregistered mode — countryId (integer) exempt from uppercase text validation',
                responseShouldInclude: {
                    httpStatus: 201,
                    'address.foreignAddressData.countryId': countryId,
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                ],
            });
            customerId = body.id ?? body.customerId;
        });

        await test.step('GET /customer/{id} — countryId persisted', async () => {
            await performGetCustomer({
                tcId: 'TC-BE-31',
                stepLabel: 'GET /customer/{id} — countryId persisted',
                Request,
                Endpoints,
                customerId,
                Responses,
                scenario: 'Confirm integer countryId stored on unregistered main address',
                responseShouldInclude: {
                    httpStatus: 200,
                    'address.foreignAddressData.countryId': countryId,
                },
                verify: (view) => {
                    const addr = resolveMainAddress(view);
                    const storedCountryId = addr?.foreignAddressData?.countryId;
                    expect(storedCountryId).toBe(countryId);
                    return [
                        {
                            field: 'address.foreignAddressData.countryId',
                            expected: countryId,
                            actual: storedCountryId,
                        },
                    ];
                },
            });
        });

        attachTcReport('TC-BE-31', Responses);
    });

    test('[PHN-3149]: TC-BE-32 – Unregistered mode — residentialAreaType enum exempt from uppercase text check', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries, {
                residentialAreaType: 'RESIDENTIAL_AREA',
            }),
            number: '12',
        });

        let customerId: number;
        await test.step('POST /customer — expect 201 (RESIDENTIAL_AREA enum accepted)', async () => {
            const { body } = await performPostCustomer({
                tcId: 'TC-BE-32',
                stepLabel: 'POST /customer — RESIDENTIAL_AREA enum accepted',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Unregistered mode — residentialAreaType enum exempt from uppercase text check',
                responseShouldInclude: {
                    httpStatus: 201,
                    'address.foreignAddressData.residentialAreaType': 'RESIDENTIAL_AREA',
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                ],
            });
            customerId = body.id ?? body.customerId;
        });

        await test.step('GET /customer/{id} — residentialAreaType persisted', async () => {
            await performGetCustomer({
                tcId: 'TC-BE-32',
                stepLabel: 'GET /customer/{id} — residentialAreaType persisted',
                Request,
                Endpoints,
                customerId,
                Responses,
                scenario: 'Confirm RESIDENTIAL_AREA enum stored on unregistered main address',
                responseShouldInclude: {
                    httpStatus: 200,
                    'address.foreignAddressData.residentialAreaType': 'RESIDENTIAL_AREA',
                },
                verify: (view) => {
                    const addr = resolveMainAddress(view);
                    const storedType = addr?.foreignAddressData?.residentialAreaType;
                    expect(storedType).toBe('RESIDENTIAL_AREA');
                    return [
                        {
                            field: 'address.foreignAddressData.residentialAreaType',
                            expected: 'RESIDENTIAL_AREA',
                            actual: storedType,
                        },
                    ];
                },
            });
        });

        attachTcReport('TC-BE-32', Responses);
    });

    test('[PHN-3149]: TC-BE-33 – Unregistered mode — streetType enum exempt from uppercase text check', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const payload: any = GeneratePayload.customers.customer_private();
        assignMainAddress(payload, {
            foreign: true,
            foreignAddressData: unregisteredForeignAddressData(envVariables.countries, {
                streetType: 'BOULEVARD',
            }),
            number: '12',
        });

        let customerId: number;
        await test.step('POST /customer — expect 201 (BOULEVARD enum accepted)', async () => {
            const { body } = await performPostCustomer({
                tcId: 'TC-BE-33',
                stepLabel: 'POST /customer — BOULEVARD enum accepted',
                Request,
                Endpoints,
                payload,
                Responses,
                scenario: 'Unregistered mode — streetType enum exempt from uppercase text check',
                responseShouldInclude: {
                    httpStatus: 201,
                    'address.foreignAddressData.streetType': 'BOULEVARD',
                },
                verify: (body) => [
                    { field: 'customerId', expected: '(assigned on create)', actual: body.id ?? body.customerId },
                ],
            });
            customerId = body.id ?? body.customerId;
        });

        await test.step('GET /customer/{id} — streetType persisted', async () => {
            await performGetCustomer({
                tcId: 'TC-BE-33',
                stepLabel: 'GET /customer/{id} — streetType persisted',
                Request,
                Endpoints,
                customerId,
                Responses,
                scenario: 'Confirm BOULEVARD enum stored on unregistered main address',
                responseShouldInclude: {
                    httpStatus: 200,
                    'address.foreignAddressData.streetType': 'BOULEVARD',
                },
                verify: (view) => {
                    const addr = resolveMainAddress(view);
                    const storedType = addr?.foreignAddressData?.streetType;
                    expect(storedType).toBe('BOULEVARD');
                    return [
                        {
                            field: 'address.foreignAddressData.streetType',
                            expected: 'BOULEVARD',
                            actual: storedType,
                        },
                    ];
                },
            });
        });

        attachTcReport('TC-BE-33', Responses);
    });
});
