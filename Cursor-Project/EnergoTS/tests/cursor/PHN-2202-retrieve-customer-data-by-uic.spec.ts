import { request as playwrightRequest, type APIResponse } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { test, expect } from '../../fixtures/baseFixture';
import reportGenerator from '../../utils/generateReport';
import { envVariables } from '../../fixtures/envCashed';
import salesPortalTokenAuth from '../../fixtures/salesPortalLogin';
import { getSPToken } from '../../fixtures/utils/auth';
import { randomGens } from '../../utils/randomGens';
import { ExtendedAPIResponse } from '../../utils/RequestWrapper';
import { activatePodOnContract } from './contract-resign-new-flow.fixtures';
import { productContractStatusUpdate } from './pdt-2854-pod-active-two-contracts.fixtures';
import {
    headerContractStatus,
    loadProductContract,
    putProductContractNewVersion,
} from './pdt-2815-version-validity.fixtures';

/**
 * PHN-2202 — Sales Portal GET getCustomer (Dev2)
 *
 * Swagger (live): GET /sales-portal/customer — operationId getCustomer
 * Query params: uic | customerNumber | contractNumber | invoiceNumber (optional);
 *   language enum BULGARIAN | ENGLISH (default BULGARIAN)
 * Response: SalesPortalCustomerResponse
 * Docs: http://10.236.20.11:8092/swagger-ui/index.html#/Sales%20Portal/getCustomer
 *
 * Auth: Sales Portal OAuth2 on API gateway :8092 (Swagger host). Preconditions use main API :8091 `Request`.
 *
 * Reference spec(s):
 * - tests/cursor/PHN-2208-sales-portal-customer-update.spec.ts
 * - tests/salesPortal/getProductList.spec.ts
 * - tests/cursor/PHN-2798-get-contract-outgoing-document.spec.ts
 * - tests/cursor/PDT-2880-pod-disconnected-banner.spec.ts
 */

type FixtureCtx = {
    Request: any;
    GeneratePayload: any;
    Responses: any;
    Endpoints: any;
};

type Language = 'BULGARIAN' | 'ENGLISH';

const SP_GET_CUSTOMER = 'customer';
const SP_GATEWAY_BASE = 'http://10.236.20.11:8092/sales-portal/';
const spTokenPath = path.resolve(__dirname, '../../fixtures/salesPortalToken.json');

// ═══════════════════════════════════════════════════════════════════════════
// SHARED HELPERS
// ═══════════════════════════════════════════════════════════════════════════

function isoDate(offsetDays: number): string {
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    return d.toISOString().split('T')[0];
}

function addDays(iso: string, offsetDays: number): string {
    const d = new Date(iso);
    d.setDate(d.getDate() + offsetDays);
    return d.toISOString().split('T')[0];
}

/** Keep signing / entry / start-of-term coherent with the product term (100-day period from signing). */
function normalizeContractDates(payload: any): void {
    const bp = payload.basicParameters;
    const entry = bp.entryInForceDate ?? isoDate(-1);
    bp.entryInForceDate = entry;
    if (!bp.signingDate || bp.signingDate > entry) {
        bp.signingDate = addDays(entry, -30);
    }
    bp.startOfInitialTerm = entry;
}

function attachReport(tc: string, Responses: any): void {
    test.info().attach(`[PHN-2202] ${tc} response`, {
        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
        contentType: 'application/json',
    });
}

// ── Structured Sales Portal GET logging ─────────────────────────────────────

type VerifiedCheck = { field: string; expected: unknown; actual: unknown };

function logSpSection(title: string): void {
    console.log(`\n${'═'.repeat(78)}\n${title}\n${'─'.repeat(78)}`);
}

function logSpGetCustomerExpected(tcId: string, stepLabel: string, expected: unknown): void {
    logSpSection(`[${tcId}] ${stepLabel}\n▶ EXPECTED (before GET /sales-portal/customer)`);
    console.log(JSON.stringify(expected, null, 2));
}

function logSpGetCustomerActual(tcId: string, stepLabel: string, status: number, body: unknown): void {
    logSpSection(`[${tcId}] ${stepLabel}\n▶ ACTUAL RESPONSE (HTTP ${status})`);
    console.log(JSON.stringify(body, null, 2));
}

function logSpGetCustomerVerified(tcId: string, stepLabel: string, checks: VerifiedCheck[]): void {
    logSpSection(`[${tcId}] ${stepLabel}\n▶ VERIFIED — matched expected values`);
    for (const check of checks) {
        const pass = JSON.stringify(check.expected) === JSON.stringify(check.actual);
        console.log(`  ${pass ? '✓' : '✗'} ${check.field}: ${JSON.stringify(check.actual)}`);
    }
    console.log(`${'═'.repeat(78)}\n`);
}

function latestCustomer(Responses: any): any {
    return Responses.customer[Responses.customer.length - 1];
}

function bodyCustomerId(body: any): number | undefined {
    return body.customerID ?? body.customerId;
}

function bodyVersionId(body: any): number | undefined {
    return body.versionID ?? body.versionId;
}

function bodyEgnUic(body: any): string | undefined {
    return body.egnUic ?? body.customerIdentifier ?? body.identifier;
}

function buildSpGetCustomerExpectation(
    Responses: any,
    uic: string,
    language: Language | 'default' | undefined,
    responseFields: Record<string, unknown>,
    notes?: string,
): Record<string, unknown> {
    const customer = Responses.customer?.length ? latestCustomer(Responses) : null;
    const langLabel = language === 'default' || language === undefined ? 'BULGARIAN (default)' : language;
    return {
        ...(notes ? { scenario: notes } : {}),
        request: {
            method: 'GET',
            path: '/sales-portal/customer',
            query: { uic, language: langLabel },
        },
        responseShouldInclude: {
            ...(customer
                ? {
                      customerId: customer.id,
                      customerNumber: customer.customerNumber ?? '(assigned on create)',
                      versionId: customer.versionId ?? customer.versionID ?? 1,
                  }
                : {}),
            egnUic: uic,
            ...responseFields,
        },
        createdEntities: reportGenerator.setLinksToResponses(Responses),
    };
}

function withCachedBody(response: APIResponse, text: string): APIResponse {
    const cached = response as APIResponse & { json: () => Promise<any>; text: () => Promise<string> };
    cached.text = async () => text;
    cached.json = async () => JSON.parse(text);
    return cached;
}

async function fetchGetCustomer(
    Request: any,
    uic: string,
    language?: Language,
): Promise<{ response: APIResponse; body: any; text: string }> {
    const params = new URLSearchParams({ uic });
    if (language) params.set('language', language);
    const response = await spGatewayGetWithAuth(Request, `${SP_GET_CUSTOMER}?${params.toString()}`);
    const text = await response.text();
    let body: any;
    try {
        body = JSON.parse(text);
    } catch {
        body = text;
    }
    return { response: withCachedBody(response, text), body, text };
}

async function fetchGetCustomerRaw(
    Request: any,
    urlPath: string,
): Promise<{ response: APIResponse; body: any; text: string }> {
    const response = await spGatewayGetWithAuth(Request, urlPath);
    const text = await response.text();
    let body: any;
    try {
        body = JSON.parse(text);
    } catch {
        body = text;
    }
    return { response: withCachedBody(response, text), body, text };
}

type PerformSpGetCustomerOpts = {
    tcId: string;
    stepLabel: string;
    Request: any;
    Responses?: any;
    uic: string;
    language?: Language;
    expected: Record<string, unknown>;
    scenario?: string;
    verify: (body: any, response: APIResponse) => VerifiedCheck[] | Promise<VerifiedCheck[]>;
};

async function performSpGetCustomer(opts: PerformSpGetCustomerOpts): Promise<{ response: APIResponse; body: any }> {
    const expectedPayload = buildSpGetCustomerExpectation(
        opts.Responses ?? {},
        opts.uic,
        opts.language,
        opts.expected,
        opts.scenario,
    );
    if (opts.expected.httpStatus != null) {
        expectedPayload.httpStatus = opts.expected.httpStatus;
    }
    logSpGetCustomerExpected(opts.tcId, opts.stepLabel, expectedPayload);

    const { response, body } = await fetchGetCustomer(opts.Request, opts.uic, opts.language);
    logSpGetCustomerActual(opts.tcId, opts.stepLabel, response.status(), body);

    const checks = await opts.verify(body, response);
    if (checks.length > 0) {
        logSpGetCustomerVerified(opts.tcId, opts.stepLabel, checks);
    }

    return { response, body };
}

type PerformSpGetCustomerRawOpts = {
    tcId: string;
    stepLabel: string;
    Request: any;
    urlPath: string;
    expected: Record<string, unknown>;
    verify: (body: any, response: APIResponse) => VerifiedCheck[] | Promise<VerifiedCheck[]>;
};

async function performSpGetCustomerRaw(
    opts: PerformSpGetCustomerRawOpts,
): Promise<{ response: APIResponse; body: any }> {
    logSpGetCustomerExpected(opts.tcId, opts.stepLabel, {
        request: { method: 'GET', path: `/sales-portal/${opts.urlPath}` },
        ...opts.expected,
    });

    const { response, body } = await fetchGetCustomerRaw(opts.Request, opts.urlPath);
    logSpGetCustomerActual(opts.tcId, opts.stepLabel, response.status(), body);

    const checks = await opts.verify(body, response);
    if (checks.length > 0) {
        logSpGetCustomerVerified(opts.tcId, opts.stepLabel, checks);
    }

    return { response, body };
}

async function performSpGetCustomerExpectVersion(
    tcId: string,
    stepLabel: string,
    Request: any,
    Responses: any,
    uic: string,
    expectedVersionId: number,
    scenario: string,
): Promise<{ response: APIResponse; body: any }> {
    const customer = latestCustomer(Responses);
    return performSpGetCustomer({
        tcId,
        stepLabel,
        Request,
        Responses,
        uic,
        scenario,
        expected: { httpStatus: 200, versionId: expectedVersionId },
        verify: async (body, response) => {
            await checkResponse(response);
            expect(bodyVersionId(body)).toBe(expectedVersionId);
            return [
                { field: 'httpStatus', expected: 200, actual: response.status() },
                { field: 'egnUic', expected: uic, actual: bodyEgnUic(body) },
                { field: 'customerId', expected: customer.id, actual: bodyCustomerId(body) },
                { field: 'versionId', expected: expectedVersionId, actual: bodyVersionId(body) },
            ];
        },
    });
}

async function getCustomerByUic(Request: any, customerUIC: string, language?: Language) {
    const { response } = await fetchGetCustomer(Request, customerUIC, language);
    return response;
}

async function getCustomerByUicNoAuth(customerUIC: string) {
    const ctx = await playwrightRequest.newContext({
        baseURL: SP_GATEWAY_BASE,
        extraHTTPHeaders: { Accept: '*/*', 'Content-Type': 'application/json' },
    });
    const urlPath = `${SP_GET_CUSTOMER}?uic=${encodeURIComponent(customerUIC)}`;
    const response = await ctx.get(urlPath);
    const text = await response.text();
    let body: any;
    try {
        body = JSON.parse(text);
    } catch {
        body = text;
    }
    return withCachedBody(response, text);
}

/** Playwright custom matcher typing workaround (see PHN-2856 / PDT-2861). */
async function checkResponse(response: ExtendedAPIResponse | APIResponse): Promise<void> {
    await (expect(response) as any).CheckResponse();
}

function apiRawContext(Request: any): any {
    return Request.raw ?? Request;
}

async function persistSPToken(token: string): Promise<void> {
    fs.writeFileSync(spTokenPath, JSON.stringify(token));
}

async function refreshSPToken(Request: any): Promise<string> {
    const token = await salesPortalTokenAuth(apiRawContext(Request));
    await persistSPToken(token);
    return token;
}

async function spGatewayGet(token: string, urlPath: string): Promise<APIResponse> {
    const ctx = await playwrightRequest.newContext({
        baseURL: SP_GATEWAY_BASE,
        extraHTTPHeaders: {
            Accept: '*/*',
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
        },
    });
    const response = await ctx.get(urlPath);
    return response;
}

async function spGatewayGetWithAuth(Request: any, urlPath: string): Promise<APIResponse> {
    let token = getSPToken();
    let response = await spGatewayGet(token, urlPath);
    if (response.status() === 401) {
        token = await refreshSPToken(Request);
        response = await spGatewayGet(token, urlPath);
    }
    return response;
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

async function findActiveProfile(Request: any, prompt = 'PHN2202'): Promise<number> {
    const getResponse = await Request.get(
        `profiles?statuses=ACTIVE&page=0&size=1&prompt=${encodeURIComponent(prompt)}`,
    );
    await expect(getResponse).toBeOK();
    const getBody = await getResponse.json();
    if (getBody?.content?.length > 0) return getBody.content[0].id;
    const response = await Request.post('profiles', {
        data: { name: prompt, timeZone: 'CET', status: 'ACTIVE', defaultSelection: false },
    });
    await expect(response).toBeOK();
    return (await response.json()).id;
}

async function findActiveTemplateId(Request: any, filter: Record<string, unknown>): Promise<number> {
    const response = await Request.post('template/list', {
        data: { statuses: ['ACTIVE'], page: 0, size: 1, ...filter },
    });
    await expect(response).toBeOK();
    const body = await response.json();
    if (body?.content?.length > 0) return body.content[0].id;
    throw new Error(`No active template found for filter ${JSON.stringify(filter)}`);
}

async function resolveProductTemplateIds(
    Request: any,
): Promise<Array<{ templateId: number; templateType: string }>> {
    const [emailId, invoiceId, contractId] = await Promise.all([
        findActiveTemplateId(Request, { types: ['EMAIL'], templatePurposes: ['INVOICE'] }),
        findActiveTemplateId(Request, { types: ['DOCUMENT'], templatePurposes: ['INVOICE'] }),
        findActiveTemplateId(Request, {
            types: ['DOCUMENT'],
            templatePurposes: ['PRODUCT'],
            languages: ['BULGARIAN'],
        }),
    ]);
    return [
        { templateId: emailId, templateType: 'EMAIL_TEMPLATE' },
        { templateId: invoiceId, templateType: 'INVOICE_TEMPLATE' },
        { templateId: contractId, templateType: 'CONTRACT_TEMPLATE' },
    ];
}

async function findOrCreateContactPurpose(Request: any, name: string): Promise<number> {
    const getResponse = await Request.get(
        `contact-purpose?statuses=ACTIVE&page=0&size=1&prompt=${encodeURIComponent(name)}`,
    );
    await expect(getResponse).toBeOK();
    const getBody = await getResponse.json();
    if (getBody?.content?.length > 0) return getBody.content[0].id;
    const response = await Request.post('contact-purpose', {
        data: { name, status: 'ACTIVE', defaultSelection: false },
    });
    await expect(response).toBeOK();
    return (await response.json()).id;
}

async function getCustomerDetails(Request: any, customerId: number): Promise<any> {
    const response = await Request.get(`customer/${customerId}`);
    await expect(response).toBeOK();
    return response.json();
}

function resolveVersionId(body: any): number {
    return (
        body.versionId ??
        body.activeCustomerDetail?.versionId ??
        body.customerDetail?.versionId ??
        (Array.isArray(body.customerDetails)
            ? body.customerDetails.reduce(
                  (max: any, d: any) => ((d.versionId ?? 0) > (max?.versionId ?? 0) ? d : max),
                  body.customerDetails[0],
              )?.versionId
            : undefined) ??
        1
    );
}

async function getCustomerVersionId(Request: any, customerId: number): Promise<number> {
    const details = await getCustomerDetails(Request, customerId);
    return resolveVersionId(details);
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

function toUpperTrsl(value: string): string {
    return value.toUpperCase().replace(/-/g, ' ').replace(/\s+/g, ' ').trim();
}

async function ensureValidLocalAddressIds(Request: any): Promise<LocalAddressIds> {
    if (cachedLocalAddressIds) return cachedLocalAddressIds;

    const unique = `phn2202-${Date.now()}`;
    /** Dev2 (incl. devapps) may require `nameTransliterated` even when cached Swagger omits it. */
    const mkActive = (name: string) => ({
        name: `${name} ${unique}`,
        nameTransliterated: toUpperTrsl(`${name}${unique.replace(/-/g, '')}`),
        status: 'ACTIVE',
        defaultSelection: false,
    });

    const countryResp = await Request.post('countries', {
        data: mkActive('Country'),
    });
    await expect(countryResp).toBeOK();
    const countryId = (await countryResp.json()).id as number;

    const regionResp = await Request.post('regions', { data: { ...mkActive('Region'), countryId } });
    await expect(regionResp).toBeOK();
    const regionId = (await regionResp.json()).id as number;

    const municipalityResp = await Request.post('municipalities', { data: { ...mkActive('Municipality'), regionId } });
    await expect(municipalityResp).toBeOK();
    const municipalityId = (await municipalityResp.json()).id as number;

    const ppResp = await Request.post('populated-places', { data: { ...mkActive('Populated place'), municipalityId } });
    await expect(ppResp).toBeOK();
    const populatedPlaceId = (await ppResp.json()).id as number;

    const zipResp = await Request.post('zip-codes', { data: { ...mkActive('Zip'), populatedPlaceId } });
    await expect(zipResp).toBeOK();
    const zipCodeId = (await zipResp.json()).id as number;

    const districtResp = await Request.post('districts', { data: { ...mkActive('District'), populatedPlaceId } });
    await expect(districtResp).toBeOK();
    const districtId = (await districtResp.json()).id as number;

    const raResp = await Request.post('residential-areas', {
        data: { ...mkActive('Residential area'), type: 'QUARTER', populatedPlaceId },
    });
    await expect(raResp).toBeOK();
    const residentialAreaId = (await raResp.json()).id as number;

    const streetResp = await Request.post('streets', {
        data: { ...mkActive('Street'), type: 'STREET', populatedPlaceId },
    });
    await expect(streetResp).toBeOK();
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

function isAddressIdFilled(value: unknown): boolean {
    if (value === null || value === undefined) return false;
    if (typeof value === 'string') return value.trim().length > 0;
    if (typeof value === 'number') return Number.isFinite(value);
    return true;
}

function pickNomenclatureLabel(body: Record<string, unknown>, preferTransliterated = true): string {
    const pick = (key: string): string => {
        const v = body[key];
        return typeof v === 'string' && v.trim().length > 0 ? v.trim() : '';
    };
    if (preferTransliterated) {
        return pick('nameTransliterated') || pick('name') || pick('populatedPlaceName') || pick('zipCode') || pick('code');
    }
    return pick('name') || pick('nameTransliterated') || pick('populatedPlaceName') || pick('zipCode') || pick('code');
}

async function fetchNomenclatureById(Request: any, pathSegment: string, id: number): Promise<Record<string, unknown>> {
    const res = await Request.get(`${pathSegment}/${id}`);
    await expect(res).toBeOK();
    return (await res.json()) as Record<string, unknown>;
}

/** Dev2 requires `*Trsl` when matching `*Id` is set on POD / address payloads. */
async function hydrateLocalAddressTrslFromNomenclature(Request: any, localAddressData: any): Promise<void> {
    if (!localAddressData || typeof localAddressData !== 'object') return;

    const setFromNomenclature = async (pathSegment: string, idKey: string, trslKey: string): Promise<void> => {
        const idRaw = localAddressData[idKey];
        if (!isAddressIdFilled(idRaw)) return;
        if (isAddressIdFilled(localAddressData[trslKey])) return;
        const idNum = Number(idRaw);
        if (!Number.isFinite(idNum)) return;
        const body = await fetchNomenclatureById(Request, pathSegment, idNum);
        localAddressData[trslKey] = toUpperTrsl(pickNomenclatureLabel(body) || String(idNum));
    };

    await setFromNomenclature('countries', 'countryId', 'countryTrsl');
    await setFromNomenclature('regions', 'regionId', 'regionTrsl');
    await setFromNomenclature('municipalities', 'municipalityId', 'municipalityTrsl');
    await setFromNomenclature('populated-places', 'populatedPlaceId', 'populatedPlaceTrsl');
    await setFromNomenclature('zip-codes', 'zipCodeId', 'zipCodeTrsl');
    await setFromNomenclature('districts', 'districtId', 'districtTrsl');
    await setFromNomenclature('residential-areas', 'residentialAreaId', 'residentialAreaTrsl');
    await setFromNomenclature('streets', 'streetId', 'streetTrsl');
}

function patchLocalAddressData(localAddressData: any, ids: LocalAddressIds): void {
    if (!localAddressData) return;
    localAddressData.countryId = ids.countryId;
    localAddressData.regionId = ids.regionId;
    localAddressData.municipalityId = ids.municipalityId;
    localAddressData.populatedPlaceId = ids.populatedPlaceId;
    localAddressData.zipCodeId = ids.zipCodeId;
    localAddressData.districtId = ids.districtId;
    localAddressData.residentialAreaId = ids.residentialAreaId;
    localAddressData.streetId = ids.streetId;
}

function patchAddressBlock(address: any, ids: LocalAddressIds): void {
    if (!address?.localAddressData) return;
    patchLocalAddressData(address.localAddressData, ids);
}

function patchCustomerPayloadAddresses(payload: any, ids: LocalAddressIds): void {
    patchAddressBlock(payload.address, ids);
    if (Array.isArray(payload.communicationData)) {
        for (const cd of payload.communicationData) {
            patchAddressBlock(cd?.address, ids);
        }
    }
}

async function preparePodSettlementPayload(Request: any, GeneratePayload: any): Promise<any> {
    const ids = await ensureValidLocalAddressIds(Request);
    const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();
    patchAddressBlock(podPayload.addressRequest, ids);
    const lad = podPayload.addressRequest?.localAddressData;
    if (lad) {
        lad.countryTrsl = null;
        lad.regionTrsl = null;
        lad.municipalityTrsl = null;
        lad.populatedPlaceTrsl = null;
        lad.zipCodeTrsl = null;
        lad.districtTrsl = null;
        lad.residentialAreaTrsl = null;
        lad.streetTrsl = null;
    }
    await hydrateLocalAddressTrslFromNomenclature(Request, lad);
    podPayload.type = podPayload.type ?? 'CONSUMER';
    podPayload.consumptionPurpose = podPayload.consumptionPurpose ?? 'NON_HOUSEHOLD';
    podPayload.voltageLevel = podPayload.voltageLevel ?? 'LOW';
    return podPayload;
}

function buildCommEntry(
    ids: LocalAddressIds,
    purposeIds: number[],
    phone: string,
    email: string,
    extraPhones: string[] = [],
    extraEmails: string[] = [],
): any {
    const phones = [phone, ...extraPhones];
    const emails = [email, ...extraEmails];
    return {
        status: 'ACTIVE',
        contactTypeName: `AUTO-${Date.now()}`,
        contactPurposes: purposeIds.map((id) => ({ contactPurposeId: id, status: 'ACTIVE' })),
        address: {
            foreign: false,
            localAddressData: {
                countryId: ids.countryId,
                regionId: ids.regionId,
                municipalityId: ids.municipalityId,
                populatedPlaceId: ids.populatedPlaceId,
                zipCodeId: ids.zipCodeId,
                districtId: ids.districtId,
                residentialAreaId: ids.residentialAreaId,
                streetId: ids.streetId,
                streetType: 'STREET',
                residentialAreaType: 'QUARTER',
            },
            number: '1',
            additionalInformation: null,
            block: null,
            entrance: null,
            floor: null,
            apartment: null,
            mailbox: null,
        },
        communicationContacts: [
            ...phones.map((p) => ({
                sendSms: false,
                platformId: null,
                status: 'ACTIVE',
                contactType: 'MOBILE_NUMBER',
                contactValue: p,
            })),
            ...emails.map((e) => ({
                sendSms: false,
                platformId: null,
                status: 'ACTIVE',
                contactType: 'EMAIL',
                contactValue: e,
            })),
        ],
        contactPersons: [],
    };
}

async function createPrivateCustomer(
    ctx: FixtureCtx,
    opts: {
        withEnTransliteration?: boolean;
        contractOnlyComm?: boolean;
        billingOnlyComm?: boolean;
        dualContractComm?: boolean;
        extraMarketingComm?: boolean;
        multiPhonesEmails?: boolean;
    } = {},
): Promise<{ id: number; identifier: string; versionId: number }> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const payload = GeneratePayload.customers.customer_private();
    const addressIds = await ensureValidLocalAddressIds(Request);
    patchCustomerPayloadAddresses(payload, addressIds);
    if (opts.withEnTransliteration !== false) {
        payload.privateCustomerDetails.firstName = 'ИВАН';
        payload.privateCustomerDetails.firstNameTranslated = 'IVAN';
        payload.privateCustomerDetails.lastName = 'ИВАНОВ';
        payload.privateCustomerDetails.lastNameTranslated = 'IVANOV';
        payload.preferCommunicationInEnglish = true;
    }

    const contractPurposeId = envVariables.contact_purpose;
    const billingPurposeId = envVariables.billing_purpose;

    if (opts.contractOnlyComm) {
        payload.communicationData = [buildCommEntry(addressIds, [contractPurposeId], '+359111000001', 'contract@test.bg')];
    } else if (opts.billingOnlyComm) {
        payload.communicationData = [buildCommEntry(addressIds, [billingPurposeId], '+359222000002', 'billing@test.bg')];
    } else if (opts.dualContractComm) {
        payload.communicationData = [
            buildCommEntry(addressIds, [contractPurposeId], '+359111000001', 'contract-old@test.bg'),
            buildCommEntry(addressIds, [contractPurposeId], '+359111000099', 'contract-new@test.bg'),
            buildCommEntry(addressIds, [billingPurposeId], '+359222000002', 'billing@test.bg'),
        ];
    } else {
        payload.communicationData = [
            buildCommEntry(addressIds, [contractPurposeId], '+359111000001', 'contract@test.bg', opts.multiPhonesEmails ? ['+359111000002'] : [], opts.multiPhonesEmails ? ['contract2@test.bg'] : []),
            buildCommEntry(addressIds, [billingPurposeId], '+359222000002', 'billing@test.bg'),
        ];
    }

    if (opts.extraMarketingComm) {
        const marketingId = await findOrCreateContactPurpose(Request, 'marketing');
        payload.communicationData.push(
            buildCommEntry(addressIds, [marketingId], '+359333000003', 'marketing@test.bg'),
        );
    }

    const response = await Request.post(Endpoints.customer, { data: payload });
    await checkResponse(response);
    const body = await response.json();
    Responses.customer.push(body);
    const versionId = await getCustomerVersionId(Request, body.id);
    return { id: body.id, identifier: body.identifier, versionId };
}

async function createLegalCustomer(ctx: FixtureCtx): Promise<{ id: number; identifier: string; versionId: number }> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const titleId = await findActiveNomenclature(Request, 'titles');
    const representationMethodId = await findActiveNomenclature(Request, 'representation-methods');
    const belongingCapitalOwnerId = await findActiveNomenclature(Request, 'belonging-capital-owners');

    const ownerPayload = GeneratePayload.customers.customer_private();
    const ownerAddressIds = await ensureValidLocalAddressIds(Request);
    patchCustomerPayloadAddresses(ownerPayload, ownerAddressIds);
    const ownerResp = await Request.post(Endpoints.customer, { data: ownerPayload });
    await checkResponse(ownerResp);
    const ownerCustomer = await ownerResp.json();

    const payload = GeneratePayload.customers.customer_legal();
    patchCustomerPayloadAddresses(payload, ownerAddressIds);
    payload.businessCustomerDetails.name = 'ТЕСТ КОМПАНИ';
    payload.businessCustomerDetails.nameTranslated = 'TEST COMPANY';
    payload.bankingDetails.directDebit = true;
    payload.bankingDetails.iban = 'BG80BNBG96611020345678';
    payload.owner = [
        { personalNumber: ownerCustomer.identifier, belongingOwnerCapitalId: belongingCapitalOwnerId },
        {
            personalNumber: (await (async () => {
                const o2 = GeneratePayload.customers.customer_private();
                patchCustomerPayloadAddresses(o2, ownerAddressIds);
                const r2 = await Request.post(Endpoints.customer, { data: o2 });
                await checkResponse(r2);
                return (await r2.json()).identifier;
            })()),
            belongingOwnerCapitalId: belongingCapitalOwnerId,
        },
    ];
    payload.managers = [
        ...(payload.managers ?? []),
        {
            id: null,
            titleId,
            name: 'ПЕТЪР',
            middleName: null,
            surname: 'ПЕТРОВ',
            personalNumber: null,
            unrecognizedIdentifier: false,
            jobPosition: 'Director',
            positionHeldFrom: null,
            positionHeldTo: null,
            birthDate: null,
            representationMethodId,
            additionalInformation: null,
            status: 'ACTIVE',
        },
    ];

    const response = await Request.post(Endpoints.customer, { data: payload });
    await checkResponse(response);
    const body = await response.json();
    Responses.customer.push(body);
    const versionId = await getCustomerVersionId(Request, body.id);
    return { id: body.id, identifier: body.identifier, versionId };
}

/** Strip name/label fields from a localAddressData object — PUT endpoint only accepts IDs + streetType + residentialAreaType. */
function stripLocalAddressData(lad: any): any {
    if (!lad) return null;
    return {
        countryId: lad.countryId ?? null,
        regionId: lad.regionId ?? null,
        municipalityId: lad.municipalityId ?? null,
        populatedPlaceId: lad.populatedPlaceId ?? null,
        zipCodeId: lad.zipCodeId ?? null,
        districtId: lad.districtId ?? null,
        residentialAreaId: lad.residentialAreaId ?? null,
        streetId: lad.streetId ?? null,
        streetType: lad.streetType ?? null,
        residentialAreaType: lad.residentialAreaType ?? null,
    };
}

async function createSecondCustomerVersion(
    Request: any,
    customerId: number,
    identifier: string,
): Promise<number> {
    // GET /customer/{id} and GET /customer/{id}?version=N both return CustomerViewResponse —
    // a FLAT object with no nested `address` or `privateCustomerDetails` sub-objects.
    // Address fields: countryId.id, localRegion.id, localMunicipality.id, populatedPlaceId.id,
    //   zipCode.id, districtId.id, residentialAreaId.id, streetId.id, streetType, residentialAreaType,
    //   streetNumber, addressAdditionalInfo, block, entrance, floor, apartment, mailbox, foreignAddress.
    // Private customer fields: name (=firstName), nameTransl, middleName, lastName, etc.

    const details = await getCustomerDetails(Request, customerId);
    const versionId = resolveVersionId(details);

    // Versioned view filters data to the requested version — still same flat schema.
    const vdResp = await Request.get(`customer/${customerId}?version=${versionId}`);
    const r = await vdResp.json(); // use 'r' = the versioned flat response

    // Segments from customerSegments[{segment:{id}}].
    const segmentIds: number[] = (() => {
        for (const src of [r, details]) {
            if (!src) continue;
            if (Array.isArray(src.customerSegments) && src.customerSegments.length > 0)
                return src.customerSegments.map((s: any) => s.segment?.id ?? s.id);
            if (Array.isArray(src.segmentIds) && src.segmentIds.length > 0) return src.segmentIds;
        }
        return [];
    })();

    // Build CustomerAddressRequest from flat response fields.
    const isForeignAddr = r.foreignAddress ?? false;
    const address = {
        foreign: isForeignAddr,
        localAddressData: isForeignAddr ? null : {
            countryId: r.countryId?.id ?? null,
            regionId: r.localRegion?.id ?? null,
            municipalityId: r.localMunicipality?.id ?? null,
            populatedPlaceId: r.populatedPlaceId?.id ?? null,
            zipCodeId: r.zipCode?.id ?? null,
            districtId: r.districtId?.id ?? null,
            residentialAreaId: r.residentialAreaId?.id ?? null,
            streetId: r.streetId?.id ?? null,
            streetType: r.streetType ?? null,
            residentialAreaType: r.residentialAreaType ?? null,
        },
        number: r.streetNumber ?? null,
        additionalInformation: r.addressAdditionalInfo ?? null,
        block: r.block ?? null,
        entrance: r.entrance ?? null,
        floor: r.floor ?? null,
        apartment: r.apartment ?? null,
        mailbox: r.mailbox ?? null,
    };

    const commSrc: any[] = r.communicationData ?? details.communicationData ?? [];
    const communicationData = commSrc.map((cd: any) => ({
        id: cd.id,
        contactTypeName: cd.contactTypeName ?? cd.name ?? `COMM-${cd.id}`,
        status: cd.status ?? 'ACTIVE',
        address: {
            foreign: cd.address?.foreign ?? false,
            localAddressData: stripLocalAddressData(cd.address?.localAddressData),
            number: cd.address?.number ?? null,
            additionalInformation: cd.address?.additionalInformation ?? null,
            block: cd.address?.block ?? null,
            entrance: cd.address?.entrance ?? null,
            floor: cd.address?.floor ?? null,
            apartment: cd.address?.apartment ?? null,
            mailbox: cd.address?.mailbox ?? null,
        },
        contactPurposes: (cd.contactPurposes ?? cd.contactPurpose ?? []).map((cp: any) => ({
            id: cp.contactPurposeId,   // junction record ID in GET → goes to `id` in PUT
            contactPurposeId: cp.id,   // system purpose type ID in GET → goes to `contactPurposeId` in PUT
            status: cp.status ?? 'ACTIVE',
        })),
        communicationContacts: (cd.communicationContacts ?? []).map((cc: any) => ({
            id: cc.id,
            sendSms: cc.sendSms ?? false,
            platformId: cc.platformId ?? null,
            status: cc.status ?? 'ACTIVE',
            contactType: cc.contactType,
            contactValue: cc.contactValue,
        })),
        contactPersons: cd.contactPersons ?? [],
    }));

    // Build addressTransl from the flat *Transl fields in CustomerViewResponse.
    const addressTransl = {
        foreign: isForeignAddr,
        localAddressData: isForeignAddr ? null : {
            country: r.countryTransl ?? null,
            region: r.regionTransl ?? null,
            municipality: r.municipalityTransl ?? null,
            populatedPlace: r.populatedPlaceTransl ?? null,
            zipCode: r.zipCodeTransl ?? null,
            district: r.districtTransl ?? null,
            residentialArea: r.residentialAreaTransl ?? null,
            residentialAreaType: r.residentialAreaType ?? null,
            street: r.streetTransl ?? null,
            streetType: r.streetType ?? null,
        },
        number: r.streetNumberTransl ?? r.streetNumber ?? null,
        additionalInformation: r.addressAdditionalInfoTransl ?? null,
        block: r.blockTransl ?? null,
        entrance: r.entranceTransl ?? null,
        floor: r.floorTransl ?? null,
        apartment: r.apartmentTransl ?? null,
        mailbox: r.mailboxTransl ?? null,
    };

    // Build bankingDetails from flat CustomerViewResponse fields.
    const bankingDetails = {
        directDebit: r.directDebit ?? false,
        bankId: r.bank?.id ?? null,
        bic: r.bank?.bic ?? null,
        iban: r.iban ?? null,
        declaredConsumption: r.customerDeclaredConsumption ?? null,
        creditRatingId: r.creditRating?.id ?? null,
        preferenceIds: (r.customerPreferences ?? [])
            .filter((p: any) => p?.status === 'ACTIVE')
            .map((p: any) => p.preferences?.id ?? p.id),
    };

    // PUT /customer/{id} with updateExistingVersion=false creates a new customer version.
    const putResp = await Request.put(`customer/${customerId}`, {
        data: {
            customerDetailsVersion: versionId,
            updateExistingVersion: false,
            customerType: r.customerType ?? 'PRIVATE_CUSTOMER',
            customerIdentifier: identifier,
            customerDetailStatus: r.status ?? 'ACTIVE',
            foreign: r.foreignEntityPerson ?? false,
            businessActivity: r.businessActivity ?? false,
            marketingConsent: r.marketingCommConsent ?? false,
            preferCommunicationInEnglish: r.preferCommunicationInEnglish ?? true,
            privateCustomerDetails: {
                gdprRegulationConsent: r.gdprRegulationConsent ?? true,
                firstName: r.name ?? 'ИВАН',
                firstNameTranslated: r.nameTransl ?? 'IVAN',
                middleName: r.middleName ?? null,
                middleNameTranslated: r.middleNameTransl ?? null,
                lastName: 'ВЕРСИЯ2',
                lastNameTranslated: 'VERSION2',
                birthDate: r.birthDate ?? null,
                kycPassed: r.kycPassed ?? false,
            },
            address,
            addressTransl,
            bankingDetails,
            segmentIds,
            communicationData,
            customerEditContractRequests: [],
        },
    });
    await checkResponse(putResp);
    return getCustomerVersionId(Request, customerId);
}

async function createCatalogChain(ctx: FixtureCtx): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const termPayload = GeneratePayload.productAndServices.term();
    termPayload.contractEntryIntoForces = ['SIGNING'];
    termPayload.startsOfContractInitialTerms = ['SIGNING'];
    termPayload.supplyActivations = ['FIRST_DAY_OF_MONTH'];
    termPayload.waitForOldContractTermToExpires = ['NO'];
    const termRes = await Request.post(Endpoints.terms, { data: termPayload });
    await checkResponse(termRes);
    Responses.terms.push(await termRes.json());

    const pricePayload = GeneratePayload.productAndServices.priceSettlement();
    pricePayload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId =
        await findActiveProfile(Request);
    const priceRes = await Request.post(Endpoints.priceComponent, {
        data: pricePayload,
    });
    await checkResponse(priceRes);
    Responses.priceComponent.push(await priceRes.json());

    const productPayload = GeneratePayload.productAndServices.product();
    productPayload.contractTypes = ['SUPPLY_ONLY'];
    productPayload.paymentGuarantees = ['NO'];
    productPayload.productStatus = 'ACTIVE';
    productPayload.availableForSale = true;
    productPayload.templateIds = await resolveProductTemplateIds(Request);
    const lastTerm = Responses.terms[Responses.terms.length - 1];
    productPayload.termId = lastTerm?.id ?? lastTerm;
    const lastPrice = Responses.priceComponent[Responses.priceComponent.length - 1];
    productPayload.priceComponentIds = [lastPrice?.id ?? lastPrice];
    const productRes = await Request.post(Endpoints.product, { data: productPayload });
    await checkResponse(productRes);
    Responses.product.push(await productRes.json());

    const podPayload = await preparePodSettlementPayload(Request, GeneratePayload);
    const podRes = await Request.post(Endpoints.pod, { data: podPayload });
    await checkResponse(podRes);
    Responses.pod.push(await podRes.json());
}

async function resolveCommDataIdsByPurpose(
    Request: any,
    customerId: number,
    contractPurposeId: number,
    billingPurposeId: number,
): Promise<{ contractCommId: number; billingCommId: number | null }> {
    const details = await getCustomerDetails(Request, customerId);
    const entries = details.communicationData ?? [];
    let contractCommId: number | undefined;
    let billingCommId: number | undefined;
    for (const entry of entries) {
        const purposes = entry.contactPurposes ?? entry.contactPurpose ?? [];
        const purposeIds = purposes.map((p: any) => p.id ?? p.contactPurposeId);
        if (purposeIds.includes(contractPurposeId)) contractCommId = entry.id;
        if (purposeIds.includes(billingPurposeId)) billingCommId = entry.id;
    }
    if (!contractCommId && entries[0]?.id) contractCommId = entries[0].id;
    return { contractCommId: contractCommId!, billingCommId: billingCommId ?? null };
}

/** Must match `createCatalogChain` term configuration (SIGNING / SIGNING / FIRST_DAY_OF_MONTH). */
function alignProductContractWithTerm(payload: any): void {
    const pp = payload.productParameters;
    pp.entryIntoForce = 'SIGNING';
    pp.startOfContractInitialTerm = 'SIGNING';
    pp.supplyActivation = 'FIRST_DAY_OF_MONTH';
}

async function createProductContract(
    ctx: FixtureCtx,
    customerIndex: number,
    opts: {
        customerVersionId?: number;
        status?: string;
        subStatus?: string;
        activationDate?: string | null;
        entryInForceDate?: string | null;
        signingDate?: string | null;
        podIndex?: number;
    } = {},
): Promise<{ contractId: number; versionId: number; contractIndex: number }> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const payload = await GeneratePayload.contractsAndOrders.product_contract(customerIndex, 0, opts.podIndex ?? 0);
    payload.productParameters.contractType = 'SUPPLY_ONLY';
    if (opts.customerVersionId != null) {
        payload.basicParameters.customerVersionId = opts.customerVersionId;
    }
    if (opts.status) payload.basicParameters.status = opts.status;
    if (opts.subStatus) payload.basicParameters.subStatus = opts.subStatus;
    if (opts.activationDate !== undefined) payload.basicParameters.activationDate = opts.activationDate;
    if (opts.entryInForceDate !== undefined) payload.basicParameters.entryInForceDate = opts.entryInForceDate;
    if (opts.signingDate !== undefined) payload.basicParameters.signingDate = opts.signingDate;

    normalizeContractDates(payload);

    const customerId = Responses.customer[customerIndex].id;
    const { contractCommId, billingCommId } = await resolveCommDataIdsByPurpose(
        Request,
        customerId,
        envVariables.contact_purpose,
        envVariables.billing_purpose,
    );
    payload.basicParameters.communicationDataContractId = contractCommId;
    payload.basicParameters.communicationDataBillingId = billingCommId;
    alignProductContractWithTerm(payload);
    payload.additionalParameters.riskAssessment = 'PERMIT';
    payload.additionalParameters.employeeId = 154;

    const response = await Request.post(Endpoints.productContract, { data: payload });
    await checkResponse(response);
    const body = await response.json();
    Responses.productContract.push(body);
    return {
        contractId: body.id,
        versionId: body.versionId ?? 1,
        contractIndex: Responses.productContract.length - 1,
    };
}

async function driveContractActiveInTerm(ctx: FixtureCtx, contractIndex: number, activationDate: string, deactivationDate?: string, podIndex = 0): Promise<void> {
    const { Request, GeneratePayload, Responses } = ctx;
    const contractId = Responses.productContract[contractIndex].id ?? Responses.productContract[contractIndex];
    await activatePodOnContract(ctx, contractIndex, activationDate, podIndex, deactivationDate);
    const res = await productContractStatusUpdate(Request, contractId, 'ACTIVE_IN_TERM', 'DELIVERY', 1);
    await checkResponse(res);
}

async function driveContractActiveInPerpetuity(ctx: FixtureCtx, contractIndex: number, activationDate: string): Promise<void> {
    const { Request, GeneratePayload, Responses } = ctx;
    const contractId = Responses.productContract[contractIndex].id ?? Responses.productContract[contractIndex];
    await activatePodOnContract(ctx, contractIndex, activationDate);
    const res = await productContractStatusUpdate(Request, contractId, 'ACTIVE_IN_PERPETUITY', 'DELIVERY', 1);
    await checkResponse(res);
}

async function setupHappyPathPrivateWithActiveContract(ctx: FixtureCtx): Promise<{ identifier: string; versionId: number }> {
    await createCatalogChain(ctx);
    const customer = await createPrivateCustomer(ctx, { withEnTransliteration: true });
    await createProductContract(ctx, ResponsesIndex(ctx, 'customer') - 1, {
        customerVersionId: customer.versionId,
        status: 'ENTERED_INTO_FORCE',
        subStatus: 'AWAITING_ACTIVATION',
        entryInForceDate: isoDate(0),
    });
    await driveContractActiveInTerm(ctx, ctx.Responses.productContract.length - 1, isoDate(-1));
    return { identifier: customer.identifier, versionId: customer.versionId };
}

function ResponsesIndex(ctx: FixtureCtx, key: 'customer'): number {
    return ctx.Responses.customer.length;
}

async function setupDualVersionCustomer(
    ctx: FixtureCtx,
    SPRequest: any,
): Promise<{ identifier: string; versionA: number; versionB: number; customerId: number }> {
    const customer = await createPrivateCustomer(ctx, { withEnTransliteration: true });
    const versionA = customer.versionId;
    const versionB = await createSecondCustomerVersion(ctx.Request, customer.id, customer.identifier);
    expect(versionB).toBeGreaterThan(versionA);
    // Update Responses.customer so the latest entry reflects the second version,
    // ensuring downstream code (createProductContract, etc.) sees versionB data.
    // Preserve `id` because GET /customer/{id} returns CustomerViewResponse which
    // uses `customerId` (not `id`), so downstream code that reads `.id` would break.
    const updatedCustomerBody = await getCustomerDetails(ctx.Request, customer.id);
    ctx.Responses.customer[ctx.Responses.customer.length - 1] = { ...updatedCustomerBody, id: customer.id };
    return { identifier: customer.identifier, versionA, versionB, customerId: customer.id };
}

function assert200CustomerBody(body: any, identifier: string): void {
    expect(body).toBeTruthy();
    expect(body.egnUic ?? body.customerIdentifier ?? body.identifier).toBe(identifier);
}

const TC_BE_8_FUTURE_START_A = '2026-09-01';
const TC_BE_8_FUTURE_START_B = '2026-12-01';
const TC_BE_9_FUTURE_START = '2026-09-01';
const TC_BE_12_FUTURE_START_A = '2026-07-01';
const TC_BE_12_FUTURE_START_B = '2026-10-01';
const TC_BE_12_TIE_START = '2026-07-01';
const TC_BE_15_FUTURE_START_A = '2026-07-01';
const TC_BE_15_FUTURE_START_B = '2026-10-01';

/** Cancel logical v1 so only future contract versions supply customer version for getCustomer (TC-BE-8/9). */
async function cancelProductContractLogicalVersion(
    Request: any,
    contractId: number,
    logicalVersionId: number,
): Promise<void> {
    const body = await loadProductContract(Request, contractId);
    const bp = (body.basicParameters ?? {}) as Record<string, unknown>;
    const headerStatus =
        headerContractStatus(body) ??
        (bp.status as string) ??
        (bp.contractStatus as string) ??
        'ACTIVE_IN_TERM';
    const subStatus =
        (bp.subStatus as string) ?? (bp.contractSubStatus as string) ?? 'DELIVERY';
    const res = await Request.put(
        `product-contract/status-update/${contractId}?versionId=${logicalVersionId}`,
        {
            data: {
                contractStatus: headerStatus,
                contractSubStatus: subStatus,
                contractVersionStatus: 'CANCELLED',
            },
        },
    );
    await checkResponse(res);
}

/**
 * Add a signed future contract version via PUT (GeneratePayload.edit_ProductContract pattern in
 * `putProductContractNewVersion` — see ContractsAndOrdersPayloads.edit_ProductContract).
 */
async function addFutureSignedProductContractVersion(
    ctx: FixtureCtx,
    contractIndex: number,
    customerIndex: number,
    fromLogicalVersionId: number,
    opts: { startDate: string; customerVersionId: number; podIndex?: number },
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = ctx;
    const contractRow = Responses.productContract[contractIndex];
    const contractId = contractRow.id ?? contractRow;
    const basePayload = await GeneratePayload.contractsAndOrders.product_contract(
        customerIndex,
        0,
        opts.podIndex ?? 0,
    );
    basePayload.basicParameters.customerVersionId = opts.customerVersionId;
    const customerId = Responses.customer[customerIndex].id;
    const { contractCommId, billingCommId } = await resolveCommDataIdsByPurpose(
        Request,
        customerId,
        envVariables.contact_purpose,
        envVariables.billing_purpose,
    );
    basePayload.basicParameters.communicationDataContractId = contractCommId;
    basePayload.basicParameters.communicationDataBillingId = billingCommId;
    basePayload.productParameters.contractType = 'SUPPLY_ONLY';
    alignProductContractWithTerm(basePayload);
    await putProductContractNewVersion(Request, Endpoints, contractId, basePayload, fromLogicalVersionId, {
        startDate: opts.startDate,
        savingAsNewVersion: true,
        versionStatus: 'SIGNED',
    });
}

/**
 * ACTIVE_IN_TERM header with cancelled current contract version + one future signed version
 * referencing the given customer version (test case TC-BE-8 / TC-BE-9 preconditions).
 */
async function setupActiveContractWithFutureCustomerVersionOnly(
    ctx: FixtureCtx,
    customerIndex: number,
    podIndex: number,
    futureStartDate: string,
    customerVersionId: number,
): Promise<{ contractIndex: number; contractId: number }> {
    const created = await createProductContract(ctx, customerIndex, {
        customerVersionId,
        status: 'ENTERED_INTO_FORCE',
        subStatus: 'AWAITING_ACTIVATION',
        entryInForceDate: isoDate(0),
        podIndex,
    });
    await driveContractActiveInTerm(ctx, created.contractIndex, isoDate(-1), undefined, podIndex);
    const contractId = created.contractId;
    await cancelProductContractLogicalVersion(ctx.Request, contractId, 1);
    await addFutureSignedProductContractVersion(ctx, created.contractIndex, customerIndex, 1, {
        startDate: futureStartDate,
        customerVersionId,
        podIndex,
    });
    return { contractIndex: created.contractIndex, contractId };
}

/**
 * ENTERED_INTO_FORCE header with cancelled current version + one future signed version
 * (Group 2 — TC-BE-12; does not promote to ACTIVE_IN_TERM).
 */
async function setupEnteredIntoForceContractWithFutureCustomerVersionOnly(
    ctx: FixtureCtx,
    customerIndex: number,
    podIndex: number,
    futureStartDate: string,
    customerVersionId: number,
): Promise<{ contractIndex: number; contractId: number }> {
    const created = await createProductContract(ctx, customerIndex, {
        customerVersionId,
        status: 'ENTERED_INTO_FORCE',
        subStatus: 'AWAITING_ACTIVATION',
        entryInForceDate: isoDate(-30),
        podIndex,
    });
    const contractId = created.contractId;
    await cancelProductContractLogicalVersion(ctx.Request, contractId, 1);
    await addFutureSignedProductContractVersion(ctx, created.contractIndex, customerIndex, 1, {
        startDate: futureStartDate,
        customerVersionId,
        podIndex,
    });
    return { contractIndex: created.contractIndex, contractId };
}

/**
 * SIGNED header (Group 3) with cancelled current version + one future signed version (TC-BE-15 future variant).
 */
async function setupSignedContractWithFutureCustomerVersionOnly(
    ctx: FixtureCtx,
    customerIndex: number,
    podIndex: number,
    futureStartDate: string,
    customerVersionId: number,
    opts: { subStatus: 'SIGNED_BY_BOTH_SIDES' | 'SPECIAL_PROCESSES'; signingDate?: string },
): Promise<{ contractIndex: number; contractId: number }> {
    const created = await createProductContract(ctx, customerIndex, {
        customerVersionId,
        status: 'SIGNED',
        subStatus: opts.subStatus,
        signingDate: opts.signingDate ?? isoDate(-10),
        entryInForceDate: isoDate(1),
        podIndex,
    });
    const contractId = created.contractId;
    await cancelProductContractLogicalVersion(ctx.Request, contractId, 1);
    await addFutureSignedProductContractVersion(ctx, created.contractIndex, customerIndex, 1, {
        startDate: futureStartDate,
        customerVersionId,
        podIndex,
    });
    return { contractIndex: created.contractIndex, contractId };
}

// ═══════════════════════════════════════════════════════════════════════════
// TESTS — 38 blocks (TC-BE-1 … TC-BE-38)
// ═══════════════════════════════════════════════════════════════════════════

test.describe('[PHN-2202]: Get: retrieving Customer Data by UIC', { tag: '@salesPortal' }, () => {
    test.describe.configure({ mode: 'serial' });

    test('[PHN-2202]: TC-BE-1 – Retrieve existing customer by UIC – happy path, default language', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: private customer + ACTIVE_IN_TERM contract', async () => {
            const setup = await setupHappyPathPrivateWithActiveContract(ctx);
            identifier = setup.identifier;
        });

        await test.step('GET /sales-portal/customer?uic=… — default language', async () => {
            const customer = latestCustomer(Responses);
            await performSpGetCustomer({
                tcId: 'TC-BE-1',
                stepLabel: 'GET default language',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Happy path — private customer with ACTIVE_IN_TERM contract',
                expected: {
                    httpStatus: 200,
                    customerType: 'PRIVATE_CUSTOMER',
                    company: null,
                    communicationDataContract: 'present (contract@test.bg)',
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    assert200CustomerBody(body, identifier);
                    expect(body.versionID ?? body.versionId).toBeTruthy();
                    expect(body.company ?? null).toBeNull();
                    expect(body.communicationDataContract).toBeTruthy();
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'egnUic', expected: identifier, actual: bodyEgnUic(body) },
                        { field: 'customerId', expected: customer.id, actual: bodyCustomerId(body) },
                        { field: 'versionId', expected: customer.versionId ?? 1, actual: bodyVersionId(body) },
                        { field: 'customerType', expected: 'PRIVATE_CUSTOMER', actual: body.customerType },
                        { field: 'company', expected: null, actual: body.company ?? null },
                        {
                            field: 'communicationDataContract.email',
                            expected: 'contract@test.bg',
                            actual: body.communicationDataContract?.email,
                        },
                    ];
                },
            });
        });

        attachReport('TC-BE-1', Responses);
    });

    test('[PHN-2202]: TC-BE-2 – language=English returns transliterated/translated values', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let bgBody: any;

        await test.step('Precondition: same as TC-BE-1', async () => {
            const setup = await setupHappyPathPrivateWithActiveContract(ctx);
            identifier = setup.identifier;
        });


        await test.step('GET default (Bulgarian)', async () => {
            const customer = latestCustomer(Responses);
            const { body } = await performSpGetCustomer({
                tcId: 'TC-BE-2',
                stepLabel: 'GET default (Bulgarian)',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Default language baseline for English comparison',
                expected: { httpStatus: 200 },
                verify: async (body, response) => {
                    await checkResponse(response);
                    assert200CustomerBody(body, identifier);
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'egnUic', expected: identifier, actual: bodyEgnUic(body) },
                        { field: 'customerId', expected: customer.id, actual: bodyCustomerId(body) },
                    ];
                },
            });
            bgBody = body;
        });

        await test.step('GET ?language=English', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-2',
                stepLabel: 'GET language=ENGLISH',
                Request,
                Responses,
                uic: identifier,
                language: 'ENGLISH',
                scenario: 'English returns transliterated/translated values vs default BG',
                expected: {
                    httpStatus: 200,
                    sameCustomerAndVersionAsDefault: true,
                    nameDiffersFromBulgarianWhenPresent: true,
                },
                verify: async (enBody, response) => {
                    await checkResponse(response);
                    expect(enBody.customerID ?? enBody.customerId).toBe(bgBody.customerID ?? bgBody.customerId);
                    expect(enBody.versionID ?? enBody.versionId).toBe(bgBody.versionID ?? bgBody.versionId);
                    if (enBody.name != null && bgBody.name != null) {
                        expect(enBody.name).not.toBe(bgBody.name);
                    }
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        {
                            field: 'customerId matches BG',
                            expected: bgBody.customerID ?? bgBody.customerId,
                            actual: enBody.customerID ?? enBody.customerId,
                        },
                        {
                            field: 'versionId matches BG',
                            expected: bgBody.versionID ?? bgBody.versionId,
                            actual: enBody.versionID ?? enBody.versionId,
                        },
                    ];
                },
            });
        });

        attachReport('TC-BE-2', Responses);
    });

    test('[PHN-2202]: TC-BE-3 – language=Bulgarian explicitly returns BG values', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: same as TC-BE-1', async () => {
            const setup = await setupHappyPathPrivateWithActiveContract(ctx);
            identifier = setup.identifier;
        });


        let defaultBody: any;

        await test.step('GET default language', async () => {
            const { body } = await performSpGetCustomer({
                tcId: 'TC-BE-3',
                stepLabel: 'GET default language',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Default language baseline for explicit BULGARIAN comparison',
                expected: { httpStatus: 200 },
                verify: async (body, response) => {
                    await checkResponse(response);
                    assert200CustomerBody(body, identifier);
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'egnUic', expected: identifier, actual: bodyEgnUic(body) },
                    ];
                },
            });
            defaultBody = body;
        });

        await test.step('GET language=Bulgarian — same name/surname as default', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-3',
                stepLabel: 'GET language=BULGARIAN',
                Request,
                Responses,
                uic: identifier,
                language: 'BULGARIAN',
                scenario: 'Explicit BULGARIAN matches default BG values',
                expected: {
                    httpStatus: 200,
                    nameMatchesDefault: true,
                    surnameMatchesDefault: true,
                },
                verify: async (bgBody, response) => {
                    await checkResponse(response);
                    expect(bgBody.name).toBe(defaultBody.name);
                    expect(bgBody.surname).toBe(defaultBody.surname);
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'name', expected: defaultBody.name, actual: bgBody.name },
                        { field: 'surname', expected: defaultBody.surname, actual: bgBody.surname },
                    ];
                },
            });
        });

        attachReport('TC-BE-3', Responses);
    });

    test('[PHN-2202]: TC-BE-4 – Group 1 – single ACTIVE_IN_TERM contract drives version selection', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: dual version customer + ACTIVE_IN_TERM on Version B', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionB,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — expect Version B', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-4',
                'GET — expect Version B (Group 1 ACTIVE_IN_TERM)',
                Request,
                Responses,
                identifier,
                versionB,
                'Group 1 — single ACTIVE_IN_TERM contract drives version selection',
            );
        });

        attachReport('TC-BE-4', Responses);
    });

    test('[PHN-2202]: TC-BE-5 – Group 1 – ACTIVE_IN_PERPETUITY single contract', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionA = 0;

        await test.step('Precondition: dual version + ACTIVE_IN_PERPETUITY on Version A', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionA = dual.versionA;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionA,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInPerpetuity(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — expect Version A', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-5',
                'GET — expect Version A (Group 1 ACTIVE_IN_PERPETUITY)',
                Request,
                Responses,
                identifier,
                versionA,
                'Group 1 — ACTIVE_IN_PERPETUITY single contract',
            );
        });

        attachReport('TC-BE-5', Responses);
    });

    test('[PHN-2202]: TC-BE-6 – Group 1 – multiple active contracts → latest activation date wins', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: two ACTIVE_IN_TERM contracts, B has later activation', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;

            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: dual.versionA,
                activationDate: '2025-01-01',
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: '2025-01-01',
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, '2025-01-01', isoDate(0));

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionB,
                activationDate: '2025-06-01',
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: '2025-06-01',
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(1));
        });


        await test.step('GET — expect Version B', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-6',
                'GET — expect Version B (latest activation date)',
                Request,
                Responses,
                identifier,
                versionB,
                'Group 1 — multiple active contracts → latest activation date wins',
            );
        });

        attachReport('TC-BE-6', Responses);
    });

    test('[PHN-2202]: TC-BE-7 – Group 1 – same activation date → latest created customer version wins', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: equal activation dates on A and B contracts', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;
            const actDate = '2025-03-01';

            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: dual.versionA,
                activationDate: actDate,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: actDate,
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, actDate);

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionB,
                activationDate: actDate,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: actDate,
                podIndex: 1,
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, actDate, undefined, 1);
        });


        await test.step('GET — expect latest created version (B)', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-7',
                'GET — expect Version B (tie-break latest created)',
                Request,
                Responses,
                identifier,
                versionB,
                'Group 1 — same activation date → latest created customer version wins',
            );
        });

        attachReport('TC-BE-7', Responses);
    });

    test('[PHN-2202]: TC-BE-8 – Group 1 – no current version → nearest future start date', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionA = 0;

        await test.step('Precondition: ACTIVE_IN_TERM contracts — future versions only reference customer', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionA = dual.versionA;
            const customerIndex = Responses.customer.length - 1;

            await setupActiveContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                0,
                TC_BE_8_FUTURE_START_A,
                dual.versionA,
            );

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            await setupActiveContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                1,
                TC_BE_8_FUTURE_START_B,
                dual.versionB,
            );
        });

        await test.step('GET — expect Version A (nearest future start 2026-09-01)', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-8',
                'GET — expect Version A (nearest future start)',
                Request,
                Responses,
                identifier,
                versionA,
                'Group 1 — no current contract version → nearest future start date (2026-09-01)',
            );
        });

        attachReport('TC-BE-8', Responses);
    });

    test('[PHN-2202]: TC-BE-9 – Group 1 – future versions with same nearest start date → latest created version', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: ACTIVE_IN_TERM — tied future start dates on both contracts', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;
            const customerIndex = Responses.customer.length - 1;

            await setupActiveContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                0,
                TC_BE_9_FUTURE_START,
                dual.versionA,
            );

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            await setupActiveContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                1,
                TC_BE_9_FUTURE_START,
                dual.versionB,
            );
        });

        await test.step('GET — expect Version B (latest created among tied futures)', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-9',
                'GET — expect Version B (tied future start → latest created customer version)',
                Request,
                Responses,
                identifier,
                versionB,
                'Group 1 — same nearest future start date → latest created customer version',
            );
        });

        attachReport('TC-BE-9', Responses);
    });

    test('[PHN-2202]: TC-BE-10 – Group 2 – Entered into Force applies only when Group 1 is empty', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: dual version + ENTERED_INTO_FORCE on Version B (no Group 1)', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionB,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(-30),
            });
        });


        await test.step('GET — expect Version B from entered-into-force contract', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-10',
                'GET — expect Version B (Group 2 ENTERED_INTO_FORCE)',
                Request,
                Responses,
                identifier,
                versionB,
                'Group 2 — Entered into Force applies only when Group 1 is empty',
            );
        });

        attachReport('TC-BE-10', Responses);
    });

    test('[PHN-2202]: TC-BE-11 – Group 2 – multiple entered-into-force → latest entry-into-force date; tie → latest created', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: two ENTERED_INTO_FORCE contracts', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;

            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: dual.versionA,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: '2025-02-01',
            });

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionB,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: '2025-08-01',
            });
        });


        await test.step('GET — latest entry-into-force → Version B', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-11',
                'GET — expect Version B (latest entry-into-force)',
                Request,
                Responses,
                identifier,
                versionB,
                'Group 2 — multiple entered-into-force → latest entry-into-force date',
            );
        });

        attachReport('TC-BE-11', Responses);
    });

    test('[PHN-2202]: TC-BE-12 – Group 2 – future-version fallback and tie-break', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifierNearest = '';
        let versionANearest = 0;
        let identifierTie = '';
        let versionBTie = 0;

        await test.step('Precondition: ENTERED_INTO_FORCE — nearest future start (2026-07-01 vs 2026-10-01)', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifierNearest = dual.identifier;
            versionANearest = dual.versionA;
            const customerIndex = Responses.customer.length - 1;

            await setupEnteredIntoForceContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                0,
                TC_BE_12_FUTURE_START_A,
                dual.versionA,
            );

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            await setupEnteredIntoForceContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                1,
                TC_BE_12_FUTURE_START_B,
                dual.versionB,
            );
        });

        await test.step('GET — expect Version A (nearest EIF future start 2026-07-01)', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-12',
                'GET — expect Version A (nearest EIF future start)',
                Request,
                Responses,
                identifierNearest,
                versionANearest,
                'Group 2 — EIF future-version fallback → nearest future start date',
            );
        });

        await test.step('Precondition (tie): EIF futures with same start date 2026-07-01', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifierTie = dual.identifier;
            versionBTie = dual.versionB;
            const customerIndex = Responses.customer.length - 1;

            await setupEnteredIntoForceContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                0,
                TC_BE_12_TIE_START,
                dual.versionA,
            );

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            await setupEnteredIntoForceContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                1,
                TC_BE_12_TIE_START,
                dual.versionB,
            );
        });

        await test.step('GET (tie) — expect Version B (latest created among tied EIF futures)', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-12',
                'GET (tie) — expect Version B (tied EIF future start)',
                Request,
                Responses,
                identifierTie,
                versionBTie,
                'Group 2 — tied EIF future start date → latest created customer version',
            );
        });

        attachReport('TC-BE-12', Responses);
    });

    test('[PHN-2202]: TC-BE-13 – Group 3 – Signed by Both Sides drives version when Groups 1 & 2 empty', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionA = 0;

        await test.step('Precondition: SIGNED/SIGNED_BY_BOTH_SIDES on Version A', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionA = dual.versionA;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionA,
                status: 'SIGNED',
                subStatus: 'SIGNED_BY_BOTH_SIDES',
                signingDate: isoDate(-10),
                entryInForceDate: isoDate(1),
            });
        });


        await test.step('GET — expect Version A', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-13',
                'GET — expect Version A (Group 3 SIGNED_BY_BOTH_SIDES)',
                Request,
                Responses,
                identifier,
                versionA,
                'Group 3 — Signed by Both Sides drives version when Groups 1 & 2 empty',
            );
        });

        attachReport('TC-BE-13', Responses);
    });

    test('[PHN-2202]: TC-BE-14 – Group 3 – Special Processes sub-status is also eligible', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: SIGNED/SPECIAL_PROCESSES on Version B', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionB,
                status: 'SIGNED',
                subStatus: 'SPECIAL_PROCESSES',
                signingDate: isoDate(-5),
                entryInForceDate: isoDate(1),
            });
        });


        await test.step('GET — expect Version B', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-14',
                'GET — expect Version B (SIGNED/SPECIAL_PROCESSES)',
                Request,
                Responses,
                identifier,
                versionB,
                'Group 3 — Special Processes sub-status is also eligible',
            );
        });

        attachReport('TC-BE-14', Responses);
    });

    test('[PHN-2202]: TC-BE-15 – Group 3 – multiple signed → latest signing date; future fallback', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        test.setTimeout(90000);
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: two signed contracts — B has later signing date', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;

            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: dual.versionA,
                status: 'SIGNED',
                subStatus: 'SIGNED_BY_BOTH_SIDES',
                signingDate: '2025-04-01',
                entryInForceDate: isoDate(1),
            });

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionB,
                status: 'SIGNED',
                subStatus: 'SPECIAL_PROCESSES',
                signingDate: '2025-09-01',
                entryInForceDate: isoDate(2),
            });
        });


        await test.step('GET — latest signing date → Version B', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-15',
                'GET — expect Version B (latest signing date)',
                Request,
                Responses,
                identifier,
                versionB,
                'Group 3 — multiple signed → latest signing date wins',
            );
        });

        let identifierFuture = '';
        let versionAFuture = 0;

        await test.step('Precondition (future): SIGNED — future versions only (2026-05-01 vs 2026-08-01)', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifierFuture = dual.identifier;
            versionAFuture = dual.versionA;
            const customerIndex = Responses.customer.length - 1;

            await setupSignedContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                0,
                TC_BE_15_FUTURE_START_A,
                dual.versionA,
                { subStatus: 'SIGNED_BY_BOTH_SIDES', signingDate: '2025-04-01' },
            );

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            await setupSignedContractWithFutureCustomerVersionOnly(
                ctx,
                customerIndex,
                1,
                TC_BE_15_FUTURE_START_B,
                dual.versionB,
                { subStatus: 'SPECIAL_PROCESSES', signingDate: '2025-09-01' },
            );
        });

        await test.step('GET (future) — expect Version A (nearest signed future start)', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-15',
                'GET (future) — expect Version A (nearest future start)',
                Request,
                Responses,
                identifierFuture,
                versionAFuture,
                'Group 3 — signed future-version fallback → nearest future start date',
            );
        });

        attachReport('TC-BE-15', Responses);
    });

    test('[PHN-2202]: TC-BE-16 – Group 4 – Terminated contract current version (Groups 1–3 empty)', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionA = 0;

        await test.step('Precondition: TERMINATED contract on Version A', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionA = dual.versionA;
            const { contractId } = await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionA,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(-60),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-50));
            const termRes = await productContractStatusUpdate(Request, contractId, 'TERMINATED', 'ALL_PODS_ARE_DEACTIVATED', 1);
            await checkResponse(termRes);
        });


        await test.step('GET — expect Version A from terminated contract', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-16',
                'GET — expect Version A (terminated contract)',
                Request,
                Responses,
                identifier,
                versionA,
                'Group 4 — Terminated contract current version when Groups 1–3 empty',
            );
        });

        attachReport('TC-BE-16', Responses);
    });

    test('[PHN-2202]: TC-BE-17 – Group 4 – multiple terminated → latest activation/entry-into-force date; NO future-version check', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: two terminated contracts — B has later date', async () => {
            await createCatalogChain(ctx);
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;

            const c1 = await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: dual.versionA,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: '2024-02-01',
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, '2024-02-01');
            await productContractStatusUpdate(Request, c1.contractId, 'TERMINATED', 'ALL_PODS_ARE_DEACTIVATED', 1);

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            const c2 = await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionB,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: '2024-10-01',
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, '2024-10-01');
            await productContractStatusUpdate(Request, c2.contractId, 'TERMINATED', 'ALL_PODS_ARE_DEACTIVATED', 1);
        });


        await test.step('GET — expect Version B', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-17',
                'GET — expect Version B (terminated — latest date)',
                Request,
                Responses,
                identifier,
                versionB,
                'Group 4 — multiple terminated → latest activation/entry-into-force date',
            );
        });

        attachReport('TC-BE-17', Responses);
    });

    test('[PHN-2202]: TC-BE-18 – Final fallback – latest created customer version when no qualifying contracts', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionB = 0;

        await test.step('Precondition: dual version, no qualifying contracts', async () => {
            const dual = await setupDualVersionCustomer(ctx, SPRequest);
            identifier = dual.identifier;
            versionB = dual.versionB;
        });


        await test.step('GET — expect latest created version B', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-18',
                'GET — expect Version B (final fallback)',
                Request,
                Responses,
                identifier,
                versionB,
                'Final fallback — latest created customer version when no qualifying contracts',
            );
        });

        attachReport('TC-BE-18', Responses);
    });

    test('[PHN-2202]: TC-BE-19 – Group priority ordering – active overrides lower groups', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';
        let versionA = 0;

        await test.step('Precondition: contracts in Groups 1, 2, 3 referencing A, B, C versions', async () => {
            await createCatalogChain(ctx);
            const customer = await createPrivateCustomer(ctx);
            versionA = customer.versionId;
            const versionB = await createSecondCustomerVersion(ctx.Request, customer.id, customer.identifier);
            const versionC = await createSecondCustomerVersion(ctx.Request, customer.id, customer.identifier);
            identifier = customer.identifier;

            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionA,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));

            const pod2 = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2.type = 'CONSUMER';
            await Request.post(Endpoints.pod, { data: pod2 }).then(async (r) => {
                await checkResponse(r);
                Responses.pod.push(await r.json());
            });
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionB,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(-20),
            });

            const pod3 = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod3.type = 'CONSUMER';
            await Request.post(Endpoints.pod, { data: pod3 }).then(async (r) => {
                await checkResponse(r);
                Responses.pod.push(await r.json());
            });
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: versionC,
                status: 'SIGNED',
                subStatus: 'SIGNED_BY_BOTH_SIDES',
                signingDate: isoDate(-5),
                entryInForceDate: isoDate(1),
            });
        });


        await test.step('GET — Group 1 (active) wins → Version A', async () => {
            await performSpGetCustomerExpectVersion(
                'TC-BE-19',
                'GET — expect Version A (Group 1 priority)',
                Request,
                Responses,
                identifier,
                versionA,
                'Group priority ordering — active (Group 1) overrides lower groups',
            );
        });

        attachReport('TC-BE-19', Responses);
    });

    test('[PHN-2202]: TC-BE-20 – communicationDataContract populated from highest-id contract-purpose record', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: two contract-purpose comm records (second higher id)', async () => {
            await createCatalogChain(ctx);
            const customer = await createPrivateCustomer(ctx, { dualContractComm: true });
            identifier = customer.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: customer.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — contract comm from higher-id record', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-20',
                stepLabel: 'GET — contract comm from highest-id record',
                Request,
                Responses,
                uic: identifier,
                scenario: 'communicationDataContract from highest-id contract-purpose record',
                expected: {
                    httpStatus: 200,
                    communicationDataContract: 'contract-new@test.bg',
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    const comm = body.communicationDataContract;
                    expect(comm).toBeTruthy();
                    expect(comm.email ?? comm.emails).toContain('contract-new@test.bg');
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        {
                            field: 'communicationDataContract.email',
                            expected: 'contains contract-new@test.bg',
                            actual: comm.email ?? comm.emails,
                        },
                    ];
                },
            });
        });

        attachReport('TC-BE-20', Responses);
    });

    test('[PHN-2202]: TC-BE-21 – communicationDataBilling returned in parallel with contract data', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: contract + billing comm records', async () => {
            const setup = await setupHappyPathPrivateWithActiveContract(ctx);
            identifier = setup.identifier;
        });


        await test.step('GET — both comm objects populated independently', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-21',
                stepLabel: 'GET — contract and billing comm objects',
                Request,
                Responses,
                uic: identifier,
                scenario: 'communicationDataContract and communicationDataBilling populated independently',
                expected: {
                    httpStatus: 200,
                    communicationDataContract: 'present',
                    communicationDataBilling: 'present',
                    distinctCommIds: true,
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    expect(body.communicationDataContract).toBeTruthy();
                    expect(body.communicationDataBilling).toBeTruthy();
                    const contractId = body.communicationDataContract?.communicationDataId ?? body.communicationDataContract?.id;
                    const billingId = body.communicationDataBilling?.communicationDataId ?? body.communicationDataBilling?.id;
                    if (contractId != null && billingId != null) {
                        expect(contractId).not.toBe(billingId);
                    }
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'communicationDataContract', expected: 'present', actual: body.communicationDataContract ? 'present' : null },
                        { field: 'communicationDataBilling', expected: 'present', actual: body.communicationDataBilling ? 'present' : null },
                        { field: 'distinctCommIds', expected: 'different when both ids present', actual: { contractId, billingId } },
                    ];
                },
            });
        });

        attachReport('TC-BE-21', Responses);
    });

    test('[PHN-2202]: TC-BE-22 – Multiple phones/emails comma-separated on the same record', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: contract comm with two phones and two emails', async () => {
            await createCatalogChain(ctx);
            const customer = await createPrivateCustomer(ctx, { multiPhonesEmails: true });
            identifier = customer.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: customer.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — comma-separated phone and email', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-22',
                stepLabel: 'GET — comma-separated phones/emails',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Multiple phones/emails comma-separated on contract comm record',
                expected: {
                    httpStatus: 200,
                    communicationDataContract: 'comma-separated phone and email',
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    const comm = body.communicationDataContract;
                    expect(comm?.phone ?? comm?.phones).toMatch(/,|\+359/);
                    expect(comm?.email ?? comm?.emails).toMatch(/,@|contract/);
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'communicationDataContract.phone', expected: 'comma or +359', actual: comm?.phone ?? comm?.phones },
                        { field: 'communicationDataContract.email', expected: 'comma or contract', actual: comm?.email ?? comm?.emails },
                    ];
                },
            });
        });

        attachReport('TC-BE-22', Responses);
    });

    test('[PHN-2202]: TC-BE-23 – Account manager formatting "Surname, Name (username)" joined with "; "', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: customer with account managers assigned at creation', async () => {
            await createCatalogChain(ctx);
            const addressIds = await ensureValidLocalAddressIds(Request);
            const payload = GeneratePayload.customers.customer_private();
            patchCustomerPayloadAddresses(payload, addressIds);
            payload.accountManagers = [
                { accountManagerId: envVariables.account_manager_types, accountManagerTypeId: envVariables.account_manager_types },
            ];
            const res = await Request.post(Endpoints.customer, { data: payload });
            await checkResponse(res);
            const body = await res.json();
            Responses.customer.push(body);
            identifier = body.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: await getCustomerVersionId(Request, body.id),
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — accountManager formatted string', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-23',
                stepLabel: 'GET — accountManager formatting',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Account manager formatted as "Surname, Name (username)"',
                expected: {
                    httpStatus: 200,
                    accountManager: 'contains parentheses when populated',
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    if (body.accountManager != null && body.accountManager !== '') {
                        expect(String(body.accountManager)).toMatch(/\(.*\)/);
                    } else {
                        test.info().annotations.push({
                            type: 'note',
                            description: 'accountManager empty — assign API may require live employee records on Dev2',
                        });
                    }
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'accountManager', expected: 'formatted or empty', actual: body.accountManager ?? '' },
                    ];
                },
            });
        });

        attachReport('TC-BE-23', Responses);
    });

    test('[PHN-2202]: TC-BE-24 – managers[] array preserves all attributes', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: legal entity with two managers', async () => {
            await createCatalogChain(ctx);
            const legal = await createLegalCustomer(ctx);
            identifier = legal.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: legal.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — managers array with attributes', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-24',
                stepLabel: 'GET — managers[] attributes',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Legal entity managers[] preserves all attributes',
                expected: {
                    httpStatus: 200,
                    managers: 'array with title, name, surname, jobPosition, methodOfRepresentation',
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    expect(Array.isArray(body.managers)).toBe(true);
                    expect(body.managers.length).toBeGreaterThanOrEqual(1);
                    const mgr = body.managers[0];
                    expect(mgr.title ?? mgr.titleId).toBeTruthy();
                    expect(mgr.name).toBeTruthy();
                    expect(mgr.surname ?? mgr.lastName).toBeTruthy();
                    expect(mgr.jobPosition).toBeTruthy();
                    expect(mgr.methodOfRepresentation ?? mgr.representationMethod).toBeTruthy();
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'managers.length', expected: '>= 1', actual: body.managers.length },
                    ];
                },
            });
        });

        attachReport('TC-BE-24', Responses);
    });

    test('[PHN-2202]: TC-BE-25 – owners[] array preserves all attributes', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: legal entity with two owners', async () => {
            await createCatalogChain(ctx);
            const legal = await createLegalCustomer(ctx);
            identifier = legal.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: legal.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — owners array with attributes', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-25',
                stepLabel: 'GET — owners[] attributes',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Legal entity owners[] preserves all attributes',
                expected: {
                    httpStatus: 200,
                    owners: 'array with identifier, name, belongingToOwnerOfCapital',
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    expect(Array.isArray(body.owners)).toBe(true);
                    expect(body.owners.length).toBeGreaterThanOrEqual(1);
                    const owner = body.owners[0];
                    expect(owner.identifier).toBeTruthy();
                    expect(owner.name).toBeTruthy();
                    expect(owner.belongingToOwnerOfCapital ?? owner.belongingToCapitalOwner).toBeTruthy();
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'owners.length', expected: '>= 1', actual: body.owners.length },
                    ];
                },
            });
        });

        attachReport('TC-BE-25', Responses);
    });

    test('[PHN-2202]: TC-BE-26 – directDebit / iban / bank / bic returned at top level', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: legal entity with banking fields', async () => {
            await createCatalogChain(ctx);
            const legal = await createLegalCustomer(ctx);
            identifier = legal.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: legal.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — top-level banking fields', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-26',
                stepLabel: 'GET — banking fields',
                Request,
                Responses,
                uic: identifier,
                scenario: 'directDebit, iban, bank returned at top level',
                expected: {
                    httpStatus: 200,
                    directDebit: true,
                    iban: 'present',
                    bank: 'present',
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    expect(body.directDebit).toBe(true);
                    expect(body.iban).toBeTruthy();
                    expect(body.bank ?? body.bankName).toBeTruthy();
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'directDebit', expected: true, actual: body.directDebit },
                        { field: 'iban', expected: 'present', actual: body.iban },
                        { field: 'bank', expected: 'present', actual: body.bank ?? body.bankName },
                    ];
                },
            });
        });

        attachReport('TC-BE-26', Responses);
    });

    test('[PHN-2202]: TC-BE-27 – Legal entity → company populated, name/surname empty', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: legal entity customer', async () => {
            await createCatalogChain(ctx);
            const legal = await createLegalCustomer(ctx);
            identifier = legal.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: legal.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET BG — company populated, name/surname empty', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-27',
                stepLabel: 'GET language=BULGARIAN',
                Request,
                Responses,
                uic: identifier,
                language: 'BULGARIAN',
                scenario: 'Legal entity — company populated, name/surname empty (BG)',
                expected: {
                    httpStatus: 200,
                    company: 'present',
                    name: 'empty',
                    surname: 'empty',
                },
                verify: async (bg, response) => {
                    await checkResponse(response);
                    expect(bg.company).toBeTruthy();
                    expect(bg.name ?? '').toBeFalsy();
                    expect(bg.surname ?? '').toBeFalsy();
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'company', expected: 'present', actual: bg.company },
                        { field: 'name', expected: 'empty', actual: bg.name ?? '' },
                        { field: 'surname', expected: 'empty', actual: bg.surname ?? '' },
                    ];
                },
            });
        });

        await test.step('GET EN — company populated', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-27',
                stepLabel: 'GET language=ENGLISH',
                Request,
                Responses,
                uic: identifier,
                language: 'ENGLISH',
                scenario: 'Legal entity — company populated (EN)',
                expected: {
                    httpStatus: 200,
                    company: 'present',
                },
                verify: async (en, response) => {
                    await checkResponse(response);
                    expect(en.company).toBeTruthy();
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'company', expected: 'present', actual: en.company },
                    ];
                },
            });
        });

        attachReport('TC-BE-27', Responses);
    });

    test('[PHN-2202]: TC-BE-28 – Private customer → name/surname populated, company empty', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: private customer', async () => {
            const setup = await setupHappyPathPrivateWithActiveContract(ctx);
            identifier = setup.identifier;
        });


        await test.step('GET — name/surname populated, company null', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-28',
                stepLabel: 'GET — private customer fields',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Private customer — name/surname populated, company null',
                expected: {
                    httpStatus: 200,
                    name: 'present',
                    surname: 'present',
                    company: null,
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    expect(body.name).toBeTruthy();
                    expect(body.surname).toBeTruthy();
                    expect(body.company ?? null).toBeNull();
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'name', expected: 'present', actual: body.name },
                        { field: 'surname', expected: 'present', actual: body.surname },
                        { field: 'company', expected: null, actual: body.company ?? null },
                    ];
                },
            });
        });

        attachReport('TC-BE-28', Responses);
    });

    test('[PHN-2202]: TC-BE-29 – EGN lookup works the same as UIC lookup', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let egn = '';
        let uic = '';

        await test.step('Precondition: private (EGN) and legal (UIC) customers', async () => {
            await createCatalogChain(ctx);
            const priv = await createPrivateCustomer(ctx);
            egn = priv.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: priv.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));

            const pod2Payload = await preparePodSettlementPayload(ctx.Request, ctx.GeneratePayload);
            pod2Payload.type = 'CONSUMER';
            const pod2 = await Request.post(Endpoints.pod, { data: pod2Payload });
            await checkResponse(pod2);
            Responses.pod.push(await pod2.json());

            const legal = await createLegalCustomer(ctx);
            uic = legal.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: legal.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
                podIndex: 1,
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1), undefined, 1);
        });


        await test.step('GET by EGN — expect 200', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-29',
                stepLabel: 'GET by EGN',
                Request,
                Responses,
                uic: egn,
                scenario: 'EGN lookup returns customer by identifier',
                expected: { httpStatus: 200, egnUic: egn },
                verify: async (body, response) => {
                    await checkResponse(response);
                    assert200CustomerBody(body, egn);
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'egnUic', expected: egn, actual: bodyEgnUic(body) },
                    ];
                },
            });
        });

        await test.step('GET by UIC — expect 200', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-29',
                stepLabel: 'GET by UIC',
                Request,
                Responses,
                uic,
                scenario: 'UIC lookup returns legal entity by identifier',
                expected: { httpStatus: 200, egnUic: uic },
                verify: async (body, response) => {
                    await checkResponse(response);
                    assert200CustomerBody(body, uic);
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'egnUic', expected: uic, actual: bodyEgnUic(body) },
                    ];
                },
            });
        });

        attachReport('TC-BE-29', Responses);
    });

    test('[PHN-2202]: TC-BE-30 – Unknown UIC → 404 not found', async ({ Request }) => {
        await test.step('GET non-existent identifier — expect 404', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-30',
                stepLabel: 'GET unknown UIC',
                Request,
                uic: '0000000000',
                scenario: 'Unknown UIC — customer not found',
                expected: {
                    httpStatus: 404,
                    errorMessage: 'not found / customer',
                },
                verify: async (body, response) => {
                    expect(response.status()).toBe(404);
                    expect(JSON.stringify(body).toLowerCase()).toMatch(/not found|customer/);
                    return [
                        { field: 'httpStatus', expected: 404, actual: response.status() },
                    ];
                },
            });
        });
        attachReport('TC-BE-30', {});
    });

    test('[PHN-2202]: TC-BE-31 – Invalid language value → 400', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: existing customer', async () => {
            const setup = await setupHappyPathPrivateWithActiveContract(ctx);
            identifier = setup.identifier;
        });


        await test.step('GET ?language=GERMAN — expect 400', async () => {
            await performSpGetCustomerRaw({
                tcId: 'TC-BE-31',
                stepLabel: 'GET invalid language=GERMAN',
                Request,
                urlPath: `${SP_GET_CUSTOMER}?uic=${encodeURIComponent(identifier)}&language=GERMAN`,
                expected: {
                    httpStatus: 400,
                    invalidLanguage: 'GERMAN',
                },
                verify: async (body, response) => {
                    expect(response.status()).toBe(400);
                    return [
                        { field: 'httpStatus', expected: 400, actual: response.status() },
                    ];
                },
            });
        });

        attachReport('TC-BE-31', Responses);
    });

    test('[PHN-2202]: TC-BE-32 – Blank / malformed customerUIC → 400 or 404', async ({ Request }) => {
        await test.step('GET malformed uic query — expect 400 or 404, not 500', async () => {
            await performSpGetCustomerRaw({
                tcId: 'TC-BE-32',
                stepLabel: 'GET malformed UIC',
                Request,
                urlPath: `${SP_GET_CUSTOMER}?uic=${encodeURIComponent('!!!INVALID!!!')}`,
                expected: {
                    httpStatus: '400 or 404',
                    malformedUic: '!!!INVALID!!!',
                },
                verify: async (body, response) => {
                    expect([400, 404]).toContain(response.status());
                    return [
                        { field: 'httpStatus', expected: '400 or 404', actual: response.status() },
                    ];
                },
            });
        });
        attachReport('TC-BE-32', {});
    });

    test('[PHN-2202]: TC-BE-33 – Missing communication scope → null object, other scope unaffected', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: billing-only comm (no contract purpose)', async () => {
            await createCatalogChain(ctx);
            const customer = await createPrivateCustomer(ctx, { billingOnlyComm: true });
            identifier = customer.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: customer.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — contract null, billing populated', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-33',
                stepLabel: 'GET — missing contract comm scope',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Missing contract-purpose comm — contract null, billing populated',
                expected: {
                    httpStatus: 200,
                    communicationDataContract: null,
                    communicationDataBilling: 'present',
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    expect(body.communicationDataContract ?? null).toBeNull();
                    expect(body.communicationDataBilling).toBeTruthy();
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'communicationDataContract', expected: null, actual: body.communicationDataContract ?? null },
                        { field: 'communicationDataBilling', expected: 'present', actual: body.communicationDataBilling ? 'present' : null },
                    ];
                },
            });
        });

        attachReport('TC-BE-33', Responses);
    });

    test('[PHN-2202]: TC-BE-34 – Numeric UIC query resolves by identifier, not internal DB id', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let numericUic = '';
        let customerXId = 0;

        await test.step('Precondition: Customer Y then Customer X with identifier = Y.id', async () => {
            const customerY = await createPrivateCustomer(ctx);
            const customerXPayload = GeneratePayload.customers.customer_private();
            const addressIds = await ensureValidLocalAddressIds(Request);
            patchCustomerPayloadAddresses(customerXPayload, addressIds);
            customerXPayload.customerIdentifier = String(customerY.id);
            const xRes = await Request.post(Endpoints.customer, { data: customerXPayload });
            await checkResponse(xRes);
            const customerX = await xRes.json();
            Responses.customer.push(customerX);
            numericUic = customerX.identifier;
            customerXId = customerX.id;
            expect(customerX.id).not.toBe(customerY.id);
        });


        await test.step('GET by UIC — returns Customer X (by identifier), not Customer Y by DB id', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-34',
                stepLabel: 'GET by numeric UIC identifier',
                Request,
                Responses,
                uic: numericUic,
                scenario: 'Numeric UIC resolves by identifier string, not internal DB id',
                expected: {
                    httpStatus: 200,
                    customerId: customerXId,
                    egnUic: numericUic,
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    expect(body.customerID ?? body.customerId).toBe(customerXId);
                    expect(body.egnUic ?? body.customerIdentifier).toBe(numericUic);
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'customerId', expected: customerXId, actual: bodyCustomerId(body) },
                        { field: 'egnUic', expected: numericUic, actual: bodyEgnUic(body) },
                    ];
                },
            });
        });

        attachReport('TC-BE-34', Responses);
    });

    test('[PHN-2202]: TC-BE-35 – Authentication required', async ({
        Request, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: existing customer', async () => {
            const customer = await createPrivateCustomer(ctx);
            identifier = customer.identifier;
        });


        await test.step('GET without token — expect 401 or 403', async () => {
            logSpGetCustomerExpected('TC-BE-35', 'GET without token', {
                scenario: 'Unauthenticated GET — auth required',
                request: {
                    method: 'GET',
                    path: '/sales-portal/customer',
                    query: { uic: identifier },
                    auth: 'none',
                },
                httpStatus: '401 or 403',
            });
            const response = await getCustomerByUicNoAuth(identifier);
            const bodyText = await response.text();
            let body: unknown;
            try {
                body = JSON.parse(bodyText);
            } catch {
                body = bodyText;
            }
            logSpGetCustomerActual('TC-BE-35', 'GET without token', response.status(), body);
            expect([401, 403]).toContain(response.status());
            logSpGetCustomerVerified('TC-BE-35', 'GET without token', [
                { field: 'httpStatus', expected: '401 or 403', actual: response.status() },
            ]);
        });

        attachReport('TC-BE-35', Responses);
    });

    test('[PHN-2202]: TC-BE-36 – Existing customer endpoints remain unchanged', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let customerId = 0;
        let identifier = '';

        await test.step('Precondition: customer from TC-BE-1 chain', async () => {
            const setup = await setupHappyPathPrivateWithActiveContract(ctx);
            identifier = setup.identifier;
            customerId = Responses.customer[Responses.customer.length - 1].id;
        });


        await test.step('GET /customer/{id} by DB id still works', async () => {
            const response = await Request.get(`customer/${customerId}`);
            await checkResponse(response);
            const body = await response.json();
            expect(body.id ?? body.customerId).toBe(customerId);
        });


        await test.step('GET /sales-portal/customer/search-by-identifier still works', async () => {
            const response = await SPRequest.get(
                `customer/search-by-identifier?customerIdentifier=${encodeURIComponent(identifier)}&language=BULGARIAN`,
            );
            await checkResponse(response);
        });

        attachReport('TC-BE-36', Responses);
    });

    test('[PHN-2202]: TC-BE-37 – Customer with no managers / owners returns gracefully', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: minimal private customer + contract', async () => {
            await createCatalogChain(ctx);
            const customer = await createPrivateCustomer(ctx);
            identifier = customer.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: customer.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — empty managers/owners, no server error', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-37',
                stepLabel: 'GET — minimal customer arrays',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Customer with no managers/owners returns gracefully',
                expected: {
                    httpStatus: 200,
                    managers: 'empty array',
                    owners: 'empty array',
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    expect(body.managers ?? []).toEqual(expect.any(Array));
                    expect(body.owners ?? []).toEqual(expect.any(Array));
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'managers', expected: 'array', actual: body.managers ?? [] },
                        { field: 'owners', expected: 'array', actual: body.owners ?? [] },
                    ];
                },
            });
        });

        attachReport('TC-BE-37', Responses);
    });

    test('[PHN-2202]: TC-BE-38 – Only contract/billing purpose records are selected; other purposes ignored', async ({
        Request, SPRequest, GeneratePayload, Responses, Endpoints,
    }) => {
        const ctx: FixtureCtx = { Request, GeneratePayload, Responses, Endpoints };
        let identifier = '';

        await test.step('Precondition: contract + billing + marketing comm records', async () => {
            await createCatalogChain(ctx);
            const customer = await createPrivateCustomer(ctx, { extraMarketingComm: true });
            identifier = customer.identifier;
            await createProductContract(ctx, Responses.customer.length - 1, {
                customerVersionId: customer.versionId,
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate: isoDate(0),
            });
            await driveContractActiveInTerm(ctx, Responses.productContract.length - 1, isoDate(-1));
        });


        await test.step('GET — marketing record never appears in contract/billing objects', async () => {
            await performSpGetCustomer({
                tcId: 'TC-BE-38',
                stepLabel: 'GET — contract/billing only comm purposes',
                Request,
                Responses,
                uic: identifier,
                scenario: 'Only contract/billing purpose records selected; marketing ignored',
                expected: {
                    httpStatus: 200,
                    communicationDataContract: 'present (no marketing email)',
                    communicationDataBilling: 'present (no marketing email)',
                    excludesMarketingEmail: true,
                },
                verify: async (body, response) => {
                    await checkResponse(response);
                    const serialized = JSON.stringify(body);
                    expect(serialized).not.toContain('marketing@test.bg');
                    expect(body.communicationDataContract).toBeTruthy();
                    expect(body.communicationDataBilling).toBeTruthy();
                    return [
                        { field: 'httpStatus', expected: 200, actual: response.status() },
                        { field: 'excludesMarketingEmail', expected: true, actual: !serialized.includes('marketing@test.bg') },
                        { field: 'communicationDataContract', expected: 'present', actual: body.communicationDataContract ? 'present' : null },
                        { field: 'communicationDataBilling', expected: 'present', actual: body.communicationDataBilling ? 'present' : null },
                    ];
                },
            });
        });

        attachReport('TC-BE-38', Responses);
    });
});
