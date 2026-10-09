import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';

// When targeting Dev2, set EnergoTS `.env` `BASE_URL` (and matching auth) to the Dev2 Phoenix API — never commit `.env`.

/**
 * Confluence 838795267 — Put: update customer data — Section II · Private customer (no BA).
 * API: PUT /customer/{id} with EditCustomerRequest (Swagger: `Cursor-Project/config/swagger/dev2/swagger-spec.json`).
 * Preconditions: POST `PRIVATE_CUSTOMER` with `businessActivity: false`; lock via POST `/locks/acquire`.
 *
 * Reference spec(s):
 * - `Cursor-Project/EnergoTS/tests/cursor/PHN-2865-manager-phones-emails.spec.ts` (GET view / versionId, PUT /customer/{id})
 */

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

/** GET /customer/{id} — resolve active detail for versionId + nested fields. */
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
    /** Flattened private customer: versionId + privateCustomerDetails on root (no managers array) */
    if (view.versionId != null && view.privateCustomerDetails != null) {
        return view;
    }
    return null;
}

function communicationRowsFromCustomerBody(body: Record<string, any>): any[] {
    const d = resolveCustomerDetail(body);
    const nested = Array.isArray(d?.communicationData) ? d.communicationData : [];
    const root = Array.isArray(body.communicationData) ? body.communicationData : [];
    return nested.length >= root.length ? nested : root;
}

function coalesceNumericId(val: unknown): number | undefined {
    if (val == null) return undefined;
    if (typeof val === 'number' && !Number.isNaN(val)) return val;
    if (typeof val === 'object' && val !== null && 'id' in (val as object)) {
        const n = Number((val as { id: unknown }).id);
        return Number.isNaN(n) ? undefined : n;
    }
    return undefined;
}

/**
 * Main customer address on GET /customer/{id}: nested `address` when present;
 * else `CustomerViewResponse`-style flattened `populatedPlaceId` / `streetId` / `foreignAddress`.
 */
function mainAddressFromCustomerView(body: Record<string, any>): Record<string, any> | null {
    const d = resolveCustomerDetail(body);
    if (d?.address && typeof d.address === 'object') return d.address;
    if (body.address && typeof body.address === 'object') return body.address;
    const acd = body.activeCustomerDetail;
    if (acd?.address && typeof acd.address === 'object') return acd.address;
    const list = body.customerDetails as Record<string, any>[] | undefined;
    if (Array.isArray(list) && list.length > 0) {
        const active = list.find((x) => x.active === true || x.current === true);
        const pick = active ?? list[0];
        if (pick?.address && typeof pick.address === 'object') return pick.address;
    }

    const ppNum = coalesceNumericId(body.populatedPlaceId);
    const stNum = coalesceNumericId(body.streetId);
    if (ppNum != null && stNum != null) {
        const foreign = body.foreignAddress !== undefined ? Boolean(body.foreignAddress) : undefined;
        return {
            foreign,
            localAddressData: {
                populatedPlaceId: ppNum,
                streetId: stNum,
            },
        };
    }
    return null;
}

/** Alternative valid domestic `localAddressData` ids (nomenclature chain) for address update scenarios. */
async function ensureValidLocalAddressIds(Request: any): Promise<LocalAddressIds> {
    const expectAny = expect as any;
    const unique = `pw-confl-${Date.now()}`;
    const mkActive = (name: string) => ({ name: `${name} ${unique}`, status: 'ACTIVE', defaultSelection: false });

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

    const streetResp = await Request.post('/streets', {
        data: { ...mkActive('Street'), type: 'STREET', populatedPlaceId },
    });
    await expectAny(streetResp).CheckResponse();
    const streetId = (await streetResp.json()).id as number;

    return {
        countryId,
        regionId,
        municipalityId,
        populatedPlaceId,
        zipCodeId,
        districtId,
        residentialAreaId,
        streetId,
    };
}

function applyLocalIds(addr: Record<string, any> | undefined, ids: LocalAddressIds): void {
    if (!addr?.localAddressData) return;
    addr.localAddressData.countryId = ids.countryId;
    addr.localAddressData.regionId = ids.regionId;
    addr.localAddressData.municipalityId = ids.municipalityId;
    addr.localAddressData.populatedPlaceId = ids.populatedPlaceId;
    addr.localAddressData.zipCodeId = ids.zipCodeId;
    addr.localAddressData.districtId = ids.districtId;
    addr.localAddressData.residentialAreaId = ids.residentialAreaId;
    addr.localAddressData.streetId = ids.streetId;
    addr.localAddressData.streetType = 'STREET';
    addr.localAddressData.residentialAreaType = 'QUARTER';
}

/** Align `customer_private()` domestic addresses with a live nomenclature chain before POST /customer. */
function applyLocalIdsToPrivatePostPayload(postPayload: Record<string, any>, ids: LocalAddressIds): void {
    applyLocalIds(postPayload.address, ids);
    const rows = postPayload.communicationData as any[] | undefined;
    if (!Array.isArray(rows)) return;
    for (const row of rows) {
        applyLocalIds(row?.address, ids);
    }
}

/** Plain private customer (no BA): maps POST-shaped customer to `EditCustomerRequest` using GET for version + communication ids. */
function buildPlainPrivateEditPayload(
    postPayload: Record<string, any>,
    getBody: Record<string, any>,
    patch?: (payload: Record<string, any>) => void,
): Record<string, any> {
    const detail = resolveCustomerDetail(getBody);
    if (detail?.versionId == null) throw new Error('Missing customerDetailsVersion (versionId) on GET /customer/{id}');

    const commSource = communicationRowsFromCustomerBody(getBody);
    const postComm = postPayload.communicationData as any[];
    const communicationData = postComm.map((row, i) => ({
        ...row,
        id: commSource[i]?.id ?? row.id,
    }));

    const payload: Record<string, any> = {
        customerDetailsVersion: detail.versionId,
        updateExistingVersion: true,
        customerType: 'PRIVATE_CUSTOMER',
        businessActivity: false,
        customerIdentifier: getBody.identifier ?? postPayload.customerIdentifier,
        foreign: postPayload.foreign,
        marketingConsent: postPayload.marketingConsent,
        preferCommunicationInEnglish: postPayload.preferCommunicationInEnglish,
        oldCustomerNumber: postPayload.oldCustomerNumber,
        customerDetailStatus: postPayload.customerDetailStatus,
        privateCustomerDetails: { ...postPayload.privateCustomerDetails },
        segmentIds: postPayload.segmentIds,
        address: structuredClone(postPayload.address),
        bankingDetails: postPayload.bankingDetails,
        communicationData,
        relatedCustomers: postPayload.relatedCustomers,
        accountManagers: Array.isArray(postPayload.accountManagers) ? postPayload.accountManagers : [],
        customerAdditionalInformation: postPayload.customerAdditionalInformation,
        customerEditContractRequests: Array.isArray(postPayload.customerEditContractRequests)
            ? postPayload.customerEditContractRequests
            : [],
    };

    if (patch) patch(payload);
    return payload;
}

async function acquireCustomerLock(Request: any, customerId: number, versionId?: number): Promise<void> {
    const expectAny = expect as any;
    let path = `/locks/acquire?entityType=customers&entityId=${customerId}`;
    if (versionId != null) path += `&versionId=${versionId}`;
    const lockResp = await Request.post(path);
    await expectAny(lockResp).CheckResponse();
}

async function releaseCustomerLock(Request: any, customerId: number, versionId?: number): Promise<void> {
    const expectAny = expect as any;
    let path = `/locks/release?entityType=customers&entityId=${customerId}`;
    if (versionId != null) path += `&versionId=${versionId}`;
    const rel = await Request.delete(path);
    await expectAny(rel).CheckResponse();
}

test.describe('[CONFLUENCE-838795267]: Put update customer — private (no BA)', { tag: '@customers' }, () => {
    test('[CONFLUENCE-838795267]: TC-BE-17 – Update transliterated names in privateCustomerDetails', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const postPayload = structuredClone(GeneratePayload.customers.customer_private()) as Record<string, any>;
        postPayload.businessActivity = false;

        let customerId: number;
        let versionId: number;

        await test.step('Precondition: POST PRIVATE_CUSTOMER (businessActivity false)', async () => {
            const postIds = await ensureValidLocalAddressIds(Request);
            applyLocalIdsToPrivatePostPayload(postPayload, postIds);
            const postResp = await Request.post(Endpoints.customer, { data: postPayload });
            await expect(postResp).CheckResponse();
            const created = await postResp.json();
            Responses.customer.push(created);
            customerId = created.id as number;
        });

        await test.step('Precondition: GET customer (customerDetailsVersion)', async () => {
            const getResp = await Request.get(`/customer/${customerId}`);
            await expect(getResp).CheckResponse();
            const body = await getResp.json();
            const d = resolveCustomerDetail(body);
            if (d?.versionId == null) throw new Error('Expected versionId on customer detail');
            versionId = d.versionId as number;
        });

        await test.step('Delta: transliterated name fields for PUT (uppercase Latin + digits per Swagger pattern)', async () => {
            const ts = Date.now();
            postPayload.privateCustomerDetails.firstNameTranslated = `AUTOFIRST${ts}`;
            postPayload.privateCustomerDetails.middleNameTranslated = `AUTOMIDDLE${ts}`;
            postPayload.privateCustomerDetails.lastNameTranslated = `AUTOLAST${ts}`;
        });

        let lockHeld = false;
        try {
            await test.step('Acquire lock', async () => {
                await acquireCustomerLock(Request, customerId, versionId);
                lockHeld = true;
            });

            await test.step('PUT /customer/{id} (updated transliterated names)', async () => {
                const getResp = await Request.get(`/customer/${customerId}`);
                await expect(getResp).CheckResponse();
                const getBody = await getResp.json();
                const putPayload = buildPlainPrivateEditPayload(postPayload, getBody);
                const putResp = await Request.put(`/customer/${customerId}`, { data: putPayload });
                await expect(putResp).CheckResponse();
            });

            await test.step('GET — persisted transliterated names', async () => {
                const getResp = await Request.get(`/customer/${customerId}`);
                await expect(getResp).CheckResponse();
                const body = await getResp.json();
                const exp = postPayload.privateCustomerDetails;
                expect(body.nameTransl).toBe(exp.firstNameTranslated);
                expect(body.middleNameTransl).toBe(exp.middleNameTranslated);
                expect(body.lastNameTransl).toBe(exp.lastNameTranslated);
            });
        } finally {
            if (lockHeld) {
                await test.step('Teardown: release lock', async () => {
                    await releaseCustomerLock(Request, customerId, versionId);
                });
            }
        }

        test.info().attach('[CONFLUENCE-838795267] TC-BE-17', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[CONFLUENCE-838795267]: TC-BE-18 – Registered domestic main address (localAddressData ids)', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        const postPayload = structuredClone(GeneratePayload.customers.customer_private()) as Record<string, any>;
        postPayload.businessActivity = false;
        postPayload.foreign = false;

        let customerId: number;
        let versionId: number;
        let altIds: LocalAddressIds;

        await test.step('Precondition: POST PRIVATE_CUSTOMER', async () => {
            const postIds = await ensureValidLocalAddressIds(Request);
            applyLocalIdsToPrivatePostPayload(postPayload, postIds);
            const postResp = await Request.post(Endpoints.customer, { data: postPayload });
            await expect(postResp).CheckResponse();
            const created = await postResp.json();
            Responses.customer.push(created);
            customerId = created.id as number;
        });

        await test.step('Precondition: GET customer (version) + build alternative nomenclature ids', async () => {
            const getResp = await Request.get(`/customer/${customerId}`);
            await expect(getResp).CheckResponse();
            const body = await getResp.json();
            const d = resolveCustomerDetail(body);
            if (d?.versionId == null) throw new Error('Expected versionId');
            versionId = d.versionId as number;
            altIds = await ensureValidLocalAddressIds(Request);
        });

        await test.step('Delta: root address.foreign false; replace localAddressData with new valid ids', async () => {
            applyLocalIds(postPayload.address, altIds);
        });

        let lockHeld = false;
        try {
            await test.step('Acquire lock', async () => {
                await acquireCustomerLock(Request, customerId, versionId);
                lockHeld = true;
            });

            await test.step('PUT /customer/{id} (main address localAddressData)', async () => {
                const getResp = await Request.get(`/customer/${customerId}`);
                await expect(getResp).CheckResponse();
                const putPayload = buildPlainPrivateEditPayload(postPayload, await getResp.json());
                const putResp = await Request.put(`/customer/${customerId}`, { data: putPayload });
                await expect(putResp).CheckResponse();
            });

            await test.step('GET — main address reflects new populatedPlaceId', async () => {
                const getResp = await Request.get(`/customer/${customerId}`);
                await expect(getResp).CheckResponse();
                const body = await getResp.json();
                const addr = mainAddressFromCustomerView(body);
                expect(addr, 'Expected main address on GET for id assertions').not.toBeNull();
                if (addr!.foreign !== undefined) {
                    expect(addr!.foreign).toBe(false);
                }
                expect(addr!.localAddressData?.populatedPlaceId).toBe(altIds.populatedPlaceId);
                expect(addr!.localAddressData?.streetId).toBe(altIds.streetId);
            });
        } finally {
            if (lockHeld) {
                await test.step('Teardown: release lock', async () => {
                    await releaseCustomerLock(Request, customerId, versionId);
                });
            }
        }

        test.info().attach('[CONFLUENCE-838795267] TC-BE-18', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[CONFLUENCE-838795267]: TC-BE-20 (Negative) – Omit privateCustomerDetails for PRIVATE_CUSTOMER', async ({
        Request,
        GeneratePayload,
        Endpoints,
    }) => {
        const postPayload = structuredClone(GeneratePayload.customers.customer_private()) as Record<string, any>;
        postPayload.businessActivity = false;

        let customerId: number;
        let versionId: number;

        await test.step('Precondition: POST PRIVATE_CUSTOMER', async () => {
            const postIds = await ensureValidLocalAddressIds(Request);
            applyLocalIdsToPrivatePostPayload(postPayload, postIds);
            const postResp = await Request.post(Endpoints.customer, { data: postPayload });
            await expect(postResp).CheckResponse();
            customerId = (await postResp.json()).id as number;
        });

        await test.step('Precondition: GET customer (version)', async () => {
            const getResp = await Request.get(`/customer/${customerId}`);
            await expect(getResp).CheckResponse();
            const body = await getResp.json();
            const d = resolveCustomerDetail(body);
            if (d?.versionId == null) throw new Error('Expected versionId');
            versionId = d.versionId as number;
        });

        let lockHeld = false;
        try {
            await test.step('Acquire lock', async () => {
                await acquireCustomerLock(Request, customerId, versionId);
                lockHeld = true;
            });

            await test.step('PUT without privateCustomerDetails — expect 400', async () => {
                const getResp = await Request.get(`/customer/${customerId}`);
                await expect(getResp).CheckResponse();
                const putPayload = buildPlainPrivateEditPayload(postPayload, await getResp.json());
                delete putPayload.privateCustomerDetails;
                const putResp = await Request.put(`/customer/${customerId}`, { data: putPayload });
                expect(putResp.status()).toBe(400);
                const text = await putResp.text();
                expect(text).toContain('privateCustomerDetails-Private Customer Details is required;');
            });
        } finally {
            if (lockHeld) {
                await test.step('Teardown: release lock', async () => {
                    await releaseCustomerLock(Request, customerId, versionId);
                });
            }
        }
    });
});
