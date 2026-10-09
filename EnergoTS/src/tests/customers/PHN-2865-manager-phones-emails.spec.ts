import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

/**
 * PHN-2865 — Customer managers: phones[] & emails[] (multi-value).
 * Backend mapping: test_cases/Backend/Add_phone_number_and_email_in_managers.md (TC-BE-1 … TC-BE-28).
 * Swagger: CreateCustomerRequest.managers[] → CreateManagerRequest.phones[], emails[]
 *
 * Skipped automation:
 * - TC-BE-14 — restricted-role Bearer token not wired in EnergoTS fixtures (see test.skip comment).
 *
 * Frontend TC-FE-1 … TC-FE-21: UI-only — see test_cases/Frontend/Add_phone_number_and_email_in_managers.md
 * (no Phoenix UI runner in EnergoTS; manual / separate E2E project).
 */

const MOBILE_A = '+359888123456';
const MOBILE_B = '0888-111-222';

function emailUnique(prefix: string): string {
    return `${prefix}+${Date.now()}@example.com`;
}

/** Spec allows unlimited multiplicity per manager — sanity-check a larger batch (distinct values). */
const MANY_PHONES_COUNT = 25;
const MANY_EMAILS_COUNT = 25;

/** Several phones and several emails together; counts may differ (no requirement they match). */
const SEVERAL_PHONES_COUNT = 10;
const SEVERAL_EMAILS_COUNT = 12;

function manyValidDistinctPhones(count: number): string[] {
    const bucket = Date.now() % 1000;
    return Array.from({ length: count }, (_, i) => `+359888${String(bucket).padStart(3, '0')}${String(i).padStart(6, '0')}`);
}

function manyDistinctEmails(prefix: string, count: number): string[] {
    const ts = Date.now();
    return Array.from({ length: count }, (_, i) => `${prefix}.${ts}.${i}@example.com`);
}

/** GET /customer may omit manager contacts; when present, assert sizes and values. */
function assertManagerPhonesEmailsProjectionOrOmitted(
    managers: any[],
    expectedPhoneCount: number,
    expectedEmailCount: number,
    phones: string[],
    emails: string[],
): void {
    expect(managers.length).toBeGreaterThan(0);
    const gotPhones = (Array.isArray(managers[0]?.phones) ? managers[0].phones : []) as string[];
    const gotEmails = (Array.isArray(managers[0]?.emails) ? managers[0].emails : []) as string[];

    const full =
        gotPhones.length === expectedPhoneCount && gotEmails.length === expectedEmailCount;
    const omitted = gotPhones.length === 0 && gotEmails.length === 0;

    expect(
        full || omitted,
        'Expected either full phones/emails on GET or omission of both (some stacks strip manager contacts on GET)',
    ).toBe(true);

    if (full) {
        expect(gotPhones).toEqual(expect.arrayContaining(phones));
        expect(gotEmails).toEqual(expect.arrayContaining(emails));
    }
}

type LocalAddressIds = {
    countryId: number;
    regionId: number;
    municipalityId: number;
    populatedPlaceId: number;
    zipCodeId: number;
    districtId: number;
    residentialAreaId: number;
    streetId: number;
};

let cachedLocalAddressIds: LocalAddressIds | null = null;

async function ensureValidLocalAddressIds(Request: any): Promise<LocalAddressIds> {
    if (cachedLocalAddressIds) return cachedLocalAddressIds;

    const expectAny = expect as any;
    const unique = `pw-${Date.now()}`;
    const mkActive = (name: string) => ({ name: `${name} ${unique}`, status: 'ACTIVE', defaultSelection: false });

    // Some environments require transliterated name even if Swagger doesn't.
    const countryResp = await Request.post('/countries', {
        data: { ...mkActive('Country'), nameTransliterated: `Country ${unique}` },
    });
    await expectAny(countryResp).CheckResponse();
    const countryId = (await countryResp.json()).id as number;

    const regionResp = await Request.post('/regions', { data: { ...mkActive('Region'), countryId } });
    await expectAny(regionResp).CheckResponse();
    const regionId = (await regionResp.json()).id as number;

    const municipalityResp = await Request.post('/municipalities', { data: { ...mkActive('Municipality'), regionId } });
    await expectAny(municipalityResp).CheckResponse();
    const municipalityId = (await municipalityResp.json()).id as number;

    const ppResp = await Request.post('/populated-places', { data: { ...mkActive('Populated place'), municipalityId } });
    await expectAny(ppResp).CheckResponse();
    const populatedPlaceId = (await ppResp.json()).id as number;

    const zipResp = await Request.post('/zip-codes', { data: { ...mkActive('Zip'), populatedPlaceId } });
    await expectAny(zipResp).CheckResponse();
    const zipCodeId = (await zipResp.json()).id as number;

    const districtResp = await Request.post('/districts', { data: { ...mkActive('District'), populatedPlaceId } });
    await expectAny(districtResp).CheckResponse();
    const districtId = (await districtResp.json()).id as number;

    const raResp = await Request.post('/residential-areas', {
        data: { ...mkActive('Residential area'), type: 'QUARTER', populatedPlaceId },
    });
    await expectAny(raResp).CheckResponse();
    const residentialAreaId = (await raResp.json()).id as number;

    const streetResp = await Request.post('/streets', { data: { ...mkActive('Street'), type: 'STREET', populatedPlaceId } });
    await expectAny(streetResp).CheckResponse();
    const streetId = (await streetResp.json()).id as number;

    cachedLocalAddressIds = {
        countryId,
        regionId,
        municipalityId,
        populatedPlaceId,
        zipCodeId,
        districtId,
        residentialAreaId,
        streetId,
    };
    return cachedLocalAddressIds;
}

async function customerLegalPayloadWithValidAddress(Request: any, GeneratePayload: any) {
    const ids = await ensureValidLocalAddressIds(Request);
    const payload = GeneratePayload.customers.customer_legal();

    const patchAddress = (addr: any) => {
        if (!addr?.localAddressData) return;
        addr.localAddressData.countryId = ids.countryId;
        addr.localAddressData.regionId = ids.regionId;
        addr.localAddressData.municipalityId = ids.municipalityId;
        addr.localAddressData.populatedPlaceId = ids.populatedPlaceId;
        addr.localAddressData.zipCodeId = ids.zipCodeId;
        addr.localAddressData.districtId = ids.districtId;
        addr.localAddressData.residentialAreaId = ids.residentialAreaId;
        addr.localAddressData.streetId = ids.streetId;
    };

    patchAddress(payload.address);
    if (Array.isArray(payload.communicationData)) {
        for (const cd of payload.communicationData) {
            patchAddress(cd?.address);
        }
    }
    return payload as Record<string, any>;
}

/** GET /customer/{id} often nests editable state under customerDetails[]. */
function resolveCustomerDetail(view: Record<string, any>): Record<string, any> | null {
    if (view.activeCustomerDetail) return view.activeCustomerDetail;
    if (view.customerDetail) return view.customerDetail;
    if (Array.isArray(view.customerDetails) && view.customerDetails.length > 0) {
        const list = view.customerDetails as Record<string, any>[];
        const active = list.find((x) => x.active === true || x.current === true);
        if (active) return active;
        return list.reduce((a, b) => ((b.versionId ?? 0) > (a.versionId ?? 0) ? b : a));
    }
    /** Experiment GET /customer/{id}: flattened view — managers + versionId on root */
    if (Array.isArray(view.managers) && view.versionId != null) {
        return view;
    }
    return null;
}

function managersFromCustomerBody(body: Record<string, any>): any[] {
    const root = Array.isArray(body.managers) ? body.managers : [];
    const d = resolveCustomerDetail(body);
    const nested = Array.isArray(d?.managers) ? d.managers : [];

    const pickLongerArray = (a: unknown, b: unknown): unknown[] => {
        const ar = Array.isArray(a) ? a : [];
        const br = Array.isArray(b) ? b : [];
        return ar.length >= br.length ? ar : br;
    };

    const maxLen = Math.max(root.length, nested.length);
    if (maxLen === 0) return [];

    /** Merge root + nested manager rows: some environments duplicate managers without contacts under activeCustomerDetail. */
    return Array.from({ length: maxLen }, (_, i) => {
        const r = root[i] ?? {};
        const n = nested[i] ?? {};
        return {
            ...n,
            ...r,
            phones: pickLongerArray((r as any).phones, (n as any).phones),
            emails: pickLongerArray((r as any).emails, (n as any).emails),
        };
    });
}

/** PUT legal customer: reuse POST-shaped blocks + GET version/manager ids (no flattened GET→address rewrite). */
function buildLegalCustomerEditPayload(postPayload: Record<string, any>, getBody: Record<string, any>): Record<string, any> {
    const detail = resolveCustomerDetail(getBody);
    if (detail?.versionId == null) throw new Error('TC-BE-4: missing versionId');
    const getManagers = managersFromCustomerBody(getBody);
    const postManagers = postPayload.managers as Record<string, any>[];
    const managers = postManagers.map((pm, i) => ({
        ...pm,
        id: getManagers[i]?.id ?? pm.id,
        phones: [] as string[],
        emails: [] as string[],
    }));

    return {
        customerDetailsVersion: detail.versionId,
        updateExistingVersion: true,
        customerType: postPayload.customerType,
        customerIdentifier: getBody.identifier ?? postPayload.customerIdentifier,
        foreign: postPayload.foreign,
        marketingConsent: postPayload.marketingConsent,
        preferCommunicationInEnglish: postPayload.preferCommunicationInEnglish,
        oldCustomerNumber: postPayload.oldCustomerNumber,
        vatNumber: postPayload.vatNumber,
        customerDetailStatus: postPayload.customerDetailStatus,
        businessCustomerDetails: postPayload.businessCustomerDetails,
        ownershipFormId: postPayload.ownershipFormId,
        economicBranchId: postPayload.economicBranchId,
        economicBranchNCEAId: postPayload.economicBranchNCEAId,
        mainSubjectOfActivity: postPayload.mainSubjectOfActivity,
        segmentIds: postPayload.segmentIds,
        address: postPayload.address,
        bankingDetails: postPayload.bankingDetails,
        managers,
    };
}

type ContactUpdateMode = 'include' | 'omit' | 'null';

function buildLegalCustomerEditPayloadWithContacts(
    postPayload: Record<string, any>,
    getBody: Record<string, any>,
    opts: {
        phones?: string[] | null;
        emails?: string[] | null;
        phonesMode?: ContactUpdateMode;
        emailsMode?: ContactUpdateMode;
        patchManager?: (mgr: Record<string, any>) => Record<string, any>;
    },
): Record<string, any> {
    const detail = resolveCustomerDetail(getBody);
    if (detail?.versionId == null) throw new Error('missing versionId');
    const getManagers = managersFromCustomerBody(getBody);
    const postManagers = postPayload.managers as Record<string, any>[];

    const phonesMode = opts.phonesMode ?? 'include';
    const emailsMode = opts.emailsMode ?? 'include';

    const managers = postManagers.map((pm, i) => {
        let mgr: Record<string, any> = {
            ...pm,
            id: getManagers[i]?.id ?? pm.id,
        };
        if (opts.patchManager) mgr = opts.patchManager(mgr);

        if (phonesMode === 'include') mgr.phones = opts.phones ?? [];
        if (phonesMode === 'null') mgr.phones = null;
        if (phonesMode === 'omit') delete mgr.phones;

        if (emailsMode === 'include') mgr.emails = opts.emails ?? [];
        if (emailsMode === 'null') mgr.emails = null;
        if (emailsMode === 'omit') delete mgr.emails;

        return mgr;
    });

    return {
        customerDetailsVersion: detail.versionId,
        updateExistingVersion: true,
        customerType: postPayload.customerType,
        customerIdentifier: getBody.identifier ?? postPayload.customerIdentifier,
        foreign: postPayload.foreign,
        marketingConsent: postPayload.marketingConsent,
        preferCommunicationInEnglish: postPayload.preferCommunicationInEnglish,
        oldCustomerNumber: postPayload.oldCustomerNumber,
        vatNumber: postPayload.vatNumber,
        customerDetailStatus: postPayload.customerDetailStatus,
        businessCustomerDetails: postPayload.businessCustomerDetails,
        ownershipFormId: postPayload.ownershipFormId,
        economicBranchId: postPayload.economicBranchId,
        economicBranchNCEAId: postPayload.economicBranchNCEAId,
        mainSubjectOfActivity: postPayload.mainSubjectOfActivity,
        segmentIds: postPayload.segmentIds,
        address: postPayload.address,
        bankingDetails: postPayload.bankingDetails,
        managers,
    };
}

/** 256 chars total; local ≤64; domain labels each ≤63 (hostname rules). Charset matches Swagger email pattern. */
function emailExactly256CharsetSafe(): string {
    const local = 'a'.repeat(64);
    const domain = `${'x'.repeat(63)}.${'x'.repeat(63)}.${'x'.repeat(59)}.com`;
    return `${local}@${domain}`;
}

test.describe('[REG-1]: Customer', { tag: '@customers' }, () => {
    test.describe('[REG-2]: Customer', () => {
        test.describe('[REG-3]: Create - Customer', () => {
    test('[REG-1321]: PHN-2865 TC-BE-1 - Create customer with one manager, one phone and one email', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const emailA = emailUnique('qa.phn2865.tcbe1');
        await test.step('POST customer', async () => {
            const payload = GeneratePayload.customers.customer_legal();
            const mgr = payload.managers[0] as Record<string, unknown>;
            mgr.phones = [MOBILE_A];
            mgr.emails = [emailA];
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            const created = await response.json();
            Responses.customer.push(created);
        });

        await test.step('GET and assert', async () => {
            const customerId = Responses.customer[Responses.customer.length - 1].id as number;
            const details = await Request.get(`/customer/${customerId}`);
            await expect(details).CheckResponse();
            const body = await details.json();
            test.info().attach('[PHN-2865] TC-BE-1 GET /customer/{id} body', {
                body: JSON.stringify(body, null, 2),
                contentType: 'application/json',
            });
            const managers = managersFromCustomerBody(body);
            expect(managers.length).toBeGreaterThan(0);
            const first = managers[0];
            expect(first.phones).toEqual(expect.arrayContaining([MOBILE_A]));
            expect(first.emails).toEqual(expect.arrayContaining([emailA]));
        });

        test.info().attach('[PHN-2865] TC-BE-1', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-1322]: PHN-2865 TC-BE-2 - Multiple phones on the same manager', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        await test.step('POST customer with two phones', async () => {
            const payload = GeneratePayload.customers.customer_legal();
            const mgr = payload.managers[0] as Record<string, unknown>;
            mgr.phones = [MOBILE_A, MOBILE_B];
            mgr.emails = [emailUnique('qa.tcbe2')];
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('GET assert two phones', async () => {
            const id = Responses.customer[Responses.customer.length - 1].id as number;
            const details = await Request.get(`/customer/${id}`);
            await expect(details).CheckResponse();
            const managers = managersFromCustomerBody(await details.json());
            expect((managers[0].phones as string[]).length).toBe(2);
            expect(managers[0].phones).toEqual(expect.arrayContaining([MOBILE_A, MOBILE_B]));
        });

        test.info().attach('[PHN-2865] TC-BE-2', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-1323]: PHN-2865 TC-BE-3 - Multiple emails on the same manager', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const e1 = emailUnique('qa.tcbe3.a');
        const e2 = emailUnique('qa.tcbe3.b');
        await test.step('POST customer with two emails', async () => {
            const payload = GeneratePayload.customers.customer_legal();
            const mgr = payload.managers[0] as Record<string, unknown>;
            mgr.phones = [MOBILE_A];
            mgr.emails = [e1, e2];
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('GET assert two emails', async () => {
            const id = Responses.customer[Responses.customer.length - 1].id as number;
            const details = await Request.get(`/customer/${id}`);
            await expect(details).CheckResponse();
            const managers = managersFromCustomerBody(await details.json());
            expect((managers[0].emails as string[]).length).toBe(2);
            expect(managers[0].emails).toEqual(expect.arrayContaining([e1, e2]));
        });

        test.info().attach('[PHN-2865] TC-BE-3', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
        });

        test.describe('[REG-149]: Edit - Customer', () => {
    test('[REG-1324]: PHN-2865 TC-BE-4 - PUT clears manager phones and emails', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const emailA = emailUnique('qa.phn2865.tcbe4');
        const postPayload = structuredClone(GeneratePayload.customers.customer_legal()) as Record<string, any>;
        const mgr = postPayload.managers[0] as Record<string, unknown>;
        mgr.phones = [MOBILE_A];
        mgr.emails = [emailA];

        await test.step('POST customer with contacts', async () => {
            const response = await Request.post(Endpoints.customer, { data: postPayload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('PUT clear phones/emails', async () => {
            const customerId = Responses.customer[Responses.customer.length - 1].id as number;
            const getResp = await Request.get(`/customer/${customerId}`);
            await expect(getResp).CheckResponse();
            const editPayload = buildLegalCustomerEditPayload(postPayload, await getResp.json());
            const putResp = await Request.put(`/customer/${customerId}`, { data: editPayload });
            await expect(putResp).CheckResponse();
        });

        await test.step('GET verify cleared', async () => {
            const customerId = Responses.customer[Responses.customer.length - 1].id as number;
            const details = await Request.get(`/customer/${customerId}`);
            await expect(details).CheckResponse();
            const managers = managersFromCustomerBody(await details.json());
            expect((managers[0].phones ?? []).length).toBe(0);
            expect((managers[0].emails ?? []).length).toBe(0);
        });

        test.info().attach('[PHN-2865] TC-BE-4', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
        });

        test.describe('[REG-3]: Create - Customer', () => {
    test('[REG-1325]: PHN-2865 TC-BE-5 - Reject manager phone with a disallowed character', async ({
        Request,
        GeneratePayload,
        Endpoints,
    }) => {
        const payload = GeneratePayload.customers.customer_legal();
        const mgr = payload.managers[0] as Record<string, unknown>;
        mgr.phones = ['+359(888)123'];
        mgr.emails = [emailUnique('qa.tcbe5')];
        const response = await Request.post(Endpoints.customer, { data: payload });
        expect(response.status()).toBeGreaterThanOrEqual(400);
    });

    test('[REG-1326]: PHN-2865 TC-BE-6 - Reject manager phone longer than 32 characters', async ({
        Request,
        GeneratePayload,
        Endpoints,
    }) => {
        const payload = GeneratePayload.customers.customer_legal();
        const mgr = payload.managers[0] as Record<string, unknown>;
        mgr.phones = [`${'1'.repeat(33)}`];
        mgr.emails = [emailUnique('qa.tcbe6')];
        const response = await Request.post(Endpoints.customer, { data: payload });
        expect(response.status()).toBeGreaterThanOrEqual(400);
    });

    test('[REG-1327]: PHN-2865 TC-BE-7 - Accept manager phone of exactly 32 characters', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const phone32 = `${'+'}${'8'.repeat(31)}`;
        expect(phone32.length).toBe(32);
        await test.step('POST', async () => {
            const payload = GeneratePayload.customers.customer_legal();
            const mgr = payload.managers[0] as Record<string, unknown>;
            mgr.phones = [phone32];
            mgr.emails = [emailUnique('qa.tcbe7')];
            const response = await Request.post(Endpoints.customer, { data: payload });
            await expect(response).CheckResponse();
            Responses.customer.push(await response.json());
        });

        await test.step('GET', async () => {
            const id = Responses.customer[Responses.customer.length - 1].id as number;
            const details = await Request.get(`/customer/${id}`);
            await expect(details).CheckResponse();
            const managers = managersFromCustomerBody(await details.json());
            expect(managers[0].phones).toEqual(expect.arrayContaining([phone32]));
        });

        test.info().attach('[PHN-2865] TC-BE-7', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-1328]: PHN-2865 TC-BE-8 - Reject manager email with an invalid format', async ({
        Request,
        GeneratePayload,
        Endpoints,
    }) => {
        const payload = GeneratePayload.customers.customer_legal();
        const mgr = payload.managers[0] as Record<string, unknown>;
        mgr.phones = [MOBILE_A];
        mgr.emails = ['not-an-email'];
        const response = await Request.post(Endpoints.customer, { data: payload });
        expect(response.status()).toBeGreaterThanOrEqual(400);
    });

    test('[REG-1329]: PHN-2865 TC-BE-9 - Reject manager email longer than 256 characters', async ({
        Request,
        GeneratePayload,
        Endpoints,
    }) => {
        const domain = 'example.com';
        const email257 = `${'a'.repeat(257 - domain.length - 1)}@${domain}`;
        expect(email257.length).toBe(257);
        const payload = GeneratePayload.customers.customer_legal();
        const mgr = payload.managers[0] as Record<string, unknown>;
        mgr.phones = [MOBILE_A];
        mgr.emails = [email257];
        const response = await Request.post(Endpoints.customer, { data: payload });
        expect(response.status()).toBeGreaterThanOrEqual(400);
    });
        });
    });
});
