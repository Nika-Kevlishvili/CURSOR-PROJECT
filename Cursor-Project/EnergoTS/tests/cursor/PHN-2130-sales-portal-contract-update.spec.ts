/**
 * PHN-2130 — Sales Portal PUT update existing product contract (TC-BE-1 … TC-BE-34).
 *
 * OpenAPI: `Cursor-Project/config/swagger/dev2/swagger-spec.json`
 *   PUT  `/sales-portal/product-contract` — `SalesPortalContractAndCustomerUpdateRequest` (nested `contract` + `customer`)
 *   GET  `/sales-portal/product-contract/{id}/version/{versionID}` — `SalesPortalContractVersionDetailResponse`
 *   GET  `/sales-portal/customer/{uic}` — `SalesPortalCustomerResponse`
 *
 * Note: TC .md references `PUT /sales-portal/contract`; dev2 swagger exposes `PUT /sales-portal/product-contract` (deployed).
 * PUT returns HTTP 200 empty body (not `{ "status": "success" }`).
 * PUT payload shape follows working dev2 manual example: contract.name, EXACT_DATE supply activation,
 * mandatory podIdentifiers on every PUT (SalesPortalContractUpdateRequest @NotNull), no entryIntoForceDate,
 * foreign customer address + middleName + KYC fields.
 * Happy-path TC-BE-1…11 use uppercase Latin for names, address text, proxy, and manager fields (dev2 validation).
 *
 * Reference spec(s):
 *   - `tests/cursor/PHN-2124-sales-portal-express-contract-create.spec.ts` (SPRequest, entity chain, portal product)
 *   - `tests/cursor/PHN-2208-sales-portal-customer-update.spec.ts` (private/business customer, manager, bank helpers)
 *   - `tests/cursor/PHN-2202-retrieve-customer-data-by-uic.spec.ts` (product-contract POST, comm-data resolution)
 */
import { test, expect } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import reportGenerator from '../../utils/generateReport';
import { randomGens } from '../../utils/randomGens';

const JIRA_TITLE = 'Put: Update existing contract Part 1 ( Development)';

const SP_PRODUCT_CONTRACT = 'product-contract';
const SP_CUSTOMER = 'customer';

const SIGNING_DATE = '2025-01-01';
const CURRENCY_ID = envVariables.currency;
const IBAN_ENABLE = 'BG80BNBG96611020345678';
const BIC_ENABLE = 'BNBGBGSD';
/** PHN-2124-style low consumption (monthly 100 → yearly 1.2). TC .md cites 1000/5000 kWh but those
 * require POD monthly 83333/416667 and typically fail Risk List on dev2 at contract POST. */
const DEFAULT_YEARLY_CONSUMPTION_KWH = 1.2;
const TC_BE_1_INITIAL_YEARLY_KWH = 1.2;
const TC_BE_1_UPDATED_YEARLY_KWH = 2.4;

function yearlyConsumptionFromPodMonthly(monthly: number): number {
    return (monthly * 12) / 1000;
}

/** Yearly kWh values that map to an integer POD monthly avg under Phoenix formula. */
function isExactYearlyConsumptionKwh(yearlyKwh: number): boolean {
    return Number.isInteger((yearlyKwh * 1000) / 12);
}

function isoDate(offsetDays: number): string {
    const d = new Date();
    d.setUTCDate(d.getUTCDate() + offsetDays);
    return d.toISOString().slice(0, 10);
}

/** Integer monthly avg on POD so contract yearly kWh matches Phoenix: sum(monthly)*12/1000. */
function podMonthlyAvgForYearlyContractKwh(yearlyKwh: number): number {
    return Math.round((yearlyKwh * 1000) / 12);
}

/** Customer name fields must be uppercase Latin (dev2 validation). */
const DEFAULT_CUSTOMER_FIRST = 'IVAN';
const DEFAULT_CUSTOMER_LAST = 'GEORGIEV';
const UPDATED_CUSTOMER_FIRST = 'IVAN UPDATED';
const MARIA_CUSTOMER_FIRST = 'MARIA';
const MARIA_CUSTOMER_LAST = 'DIMITROVA';
const MARIA_UPDATED_FIRST = 'MARIA UPDATED';
const MGR_GEORGI = 'GEORGI';
const MGR_NIKOLOV = 'NIKOLOV';
const MGR_IVAN = 'IVAN';
const MGR_PETROV = 'PETROV';
const MGR_GTIGOL = 'GTIGOL';
const DEFAULT_MIDDLE_NAME = 'MIDDLE NAME';
const MANAGER_JOB_POSITION = 'MANAGER';
/** Valid Bulgarian EGN for proxy personal number (IdentificationNumberChecker.personalNumber). */
const VALID_PROXY_EGN = '7501011232';
/** Second valid EGN for authorized-proxy personal number (ProxyUpdate schema). */
const VALID_AUTH_PROXY_EGN = '6101047500';
const KYC_EXP_2028 = '2028-01-01';
const INVALID_PROXY_ID = 9999999;
const MGR_DIMITAR = 'DIMITAR';
const MGR_STOEV = 'STOEV';
const LEGAL_ENTITY_COMPANY = 'LEGAL ENTITY CORP';
const EXISTING_PROXY_NAME = 'EXISTING PROXY';
const FIRST_PROXY_NAME = 'FIRST PROXY';
const SECOND_PROXY_PERSON = 'SECOND PROXY PERSON';
const SECOND_PROXY_DATE = '2025-06-15';
const SECOND_PROXY_UIC_BUSINESS = '8501014321';
// TC .md used 131234567 but Phoenix requires valid Bulgarian UIC checksum
const SECOND_PROXY_UIC_LEGAL = '131234568';
const TC_BE_28_YEARLY_KWH = 6000;
const TC_BE_27_UPDATED_YEARLY_KWH = 2.4;
const ADDITIONAL_PARAM_LABEL = 'PHN2130 ADDITIONAL PARAM';
const ADDITIONAL_PARAM_INITIAL_VALUE = 'INITIAL VALUE';
const ADDITIONAL_PARAM_UPDATED_VALUE = 'UPDATED VALUE';

/** TC-BE-25 — flat SP PUT third-tab values (excludes IAP, contractFormulas, productAdditionalParams). */
const TC_BE_25_CASH_DEPOSIT_AMOUNT = 1.11;
const TC_BE_25_BANK_GUARANTEE_AMOUNT = 2.22;
const TC_BE_25_SECOND_INVOICE_PAYMENT_TERM_VALUE = 15;
const TC_BE_25_UPDATED_START_OF_INITIAL_TERM_OFFSET_DAYS = 21;

/** Happy-path SP foreign address — uppercase Latin (dev2 rejects mixed/lowercase on name-like fields). */
const SP_FOREIGN_ADDRESS_TEXT = {
    region: 'SOFIA REGION',
    municipality: 'SOFIA MUNICIPALITY',
    populatedPlace: 'SOFIA',
    zipCode: '1000',
    district: 'CENTER',
    residentialAreaType: 'QUARTER',
    residentialArea: 'SAMPLE QUARTER',
    streetType: 'STREET',
    street: 'SAMPLE STREET',
} as const;
const SP_ADDRESS_NUMBER = '122';
const SP_ADDRESS_ADDITIONAL_INFO = 'SALES PORTAL TEST';

const TC_BE_29_ADDRESS_UPDATED = {
    number: '229',
    additionalInformation: 'TC BE 29 ADDRESS UPDATE',
    block: 'B',
    entrance: '2',
    floor: '3',
    apartment: '29',
    mailbox: 'MB-29',
    streetType: 'BOULEVARD',
    residentialAreaType: 'RESIDENTIAL_AREA',
} as const;

type LocalAddressScalars = {
    number: string;
    additionalInformation: string;
    block: string;
    entrance: string;
    floor: string;
    apartment: string;
    mailbox: string;
    streetType: string;
    residentialAreaType: string;
};

function latinUpper(value: string): string {
    return value.toUpperCase();
}

/** Uppercase Latin string fields on SP customer PUT overrides (happy-path defaults). */
function applyCustomerPutUppercaseOverrides(overrides: CustomerPutOverrides): CustomerPutOverrides {
    const upperKeys = [
        'name',
        'nameTransliterated',
        'middleName',
        'middleNameTransliterated',
        'lastName',
        'lastNameTransliterated',
    ];
    const out = { ...overrides };
    for (const key of upperKeys) {
        if (typeof out[key] === 'string') {
            out[key] = latinUpper(out[key] as string);
        }
    }
    if (Array.isArray(out.managers)) {
        out.managers = out.managers.map((mgr: any) => ({
            ...mgr,
            ...(typeof mgr.firstName === 'string' ? { firstName: latinUpper(mgr.firstName) } : {}),
            ...(typeof mgr.middleName === 'string' ? { middleName: latinUpper(mgr.middleName) } : {}),
            ...(typeof mgr.lastName === 'string' ? { lastName: latinUpper(mgr.lastName) } : {}),
            ...(typeof mgr.jobPosition === 'string' ? { jobPosition: latinUpper(mgr.jobPosition) } : {}),
        }));
    }
    return out;
}

async function checkResponse(response: unknown) {
    await (expect(response) as any).CheckResponse();
}

function testName(tc: string, shortTitle: string): string {
    return `[PHN-2130]: ${JIRA_TITLE} — ${tc}: ${shortTitle}`;
}

function attachReport(tc: string, Responses: any) {
    test.info().attach(`PHN-2130 ${tc}`, {
        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
        contentType: 'application/json',
    });
}

/** Logs copy-paste-ready PUT body to console and Playwright report (manual Postman/curl reuse). */
function logSpPutContractPayload(tcLabel: string, body: Record<string, unknown>) {
    const endpoint = `PUT /sales-portal/${SP_PRODUCT_CONTRACT}`;
    const json = JSON.stringify(body, null, 2);
    const plain = [
        `[PHN-2130] ${tcLabel} — ${endpoint}`,
        'Copy the JSON below for manual requests (Sales Portal OAuth token required):',
        json,
    ].join('\n\n');

    console.log(`\n${plain}\n`);

    test.info().attach(`${tcLabel} — SP PUT product-contract payload`, {
        body: plain,
        contentType: 'text/plain; charset=utf-8',
    });
    test.info().attach(`${tcLabel} — SP PUT product-contract payload (JSON)`, {
        body: json,
        contentType: 'application/json',
    });
}

// ─── Types ───────────────────────────────────────────────────────────────────

interface ContractChainContext {
    contractId: number;
    contractVersionId: number;
    contractName: string;
    customerUic: string;
    customerId: number;
    productId: number;
    productVersionId: number;
    productContractTermId: number;
    invoicePaymentTermId: number;
    commContractId: number;
    commBillingId: number;
    podIdentifier: string;
    estimatedTotalConsumption: number;
    signingDate: string;
    startOfInitialTermDate: string;
    supplyActivationAfterContractResigningDate: string;
    managerId?: number;
}

type ContractCreateOpts = {
    estimatedTotalConsumption?: number;
    directDebit?: boolean;
    bankId?: number;
    iban?: string;
    bic?: string;
    proxy?: Record<string, unknown>[];
    customerIndex?: number;
    productIndex?: number;
    podIndex?: number;
    entryInForceDate?: string | null;
    status?: string;
    subStatus?: string;
};

type ContractPutOverrides = Record<string, unknown>;
type CustomerPutOverrides = Record<string, unknown>;
type CustomerPutSectionOpts = {
    /** Omit kycPassed / kycExpirationDate entirely (leave-as-is rule — TC-BE-11). */
    omitKycFields?: boolean;
    /** Do not auto-fill kycExpirationDate when kycPassed is true (TC-BE-20 negative). */
    skipAutoKycExpiration?: boolean;
};

// ─── Low-level helpers ───────────────────────────────────────────────────────

async function getCustomerDetails(Request: any, customerId: number): Promise<any> {
    const response = await Request.get(`customer/${customerId}`);
    await checkResponse(response);
    return response.json();
}

async function getCustomerVersionId(Request: any, customerId: number): Promise<number> {
    const details = await getCustomerDetails(Request, customerId);
    return details.versionId ?? 1;
}

async function getManagerId(Request: any, customerId: number): Promise<number | null> {
    const details = await getCustomerDetails(Request, customerId);
    const managers = details.managers ?? [];
    return managers.length > 0 ? managers[0].id : null;
}

async function findActiveBank(Request: any): Promise<{ id: number; bic: string }> {
    const response = await Request.get('banks?statuses=ACTIVE&page=0&size=1');
    await checkResponse(response);
    const body = await response.json();
    if (body?.content?.length > 0) {
        return { id: body.content[0].id, bic: body.content[0].bic ?? BIC_ENABLE };
    }
    const createResp = await Request.post('banks', {
        data: {
            name: latinUpper(`TESTBANK_${randomGens.generateRandomString(true, false, 4)}`),
            bic: 'TSTBBGSF',
            status: 'ACTIVE',
        },
    });
    await checkResponse(createResp);
    const created = await createResp.json();
    return { id: created.id, bic: created.bic ?? 'TSTBBGSF' };
}

async function findActiveTemplateId(Request: any, filter: Record<string, unknown>): Promise<number> {
    const response = await Request.post('template/list', {
        data: { statuses: ['ACTIVE'], page: 0, size: 1, ...filter },
    });
    await checkResponse(response);
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

/** Align contract productParameters with term configuration (PHN-2202 pattern). */
function alignProductContractWithTerm(payload: any): void {
    const pp = payload.productParameters;
    pp.entryIntoForce = 'SIGNING';
    pp.startOfContractInitialTerm = 'SIGNING';
    pp.supplyActivation = 'FIRST_DAY_OF_MONTH';
    pp.productContractWaitForOldContractTermToExpires = 'NO';
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

function isAddressSourceFilled(value: unknown): boolean {
    if (value === null || value === undefined) return false;
    if (typeof value === 'string') return value.trim().length > 0;
    if (typeof value === 'number') return Number.isFinite(value);
    return true;
}

/** Bilingual rule: *Trsl mirrors the source field value (uppercase Latin). */
function copySourceToTrsl(source: unknown): string {
    if (source === null || source === undefined) return '';
    return toUpperTrsl(String(source));
}

/** POD / customer local address — enum and scalar *Trsl fields. */
function syncLocalAddressDataTrsl(localAddressData: any): void {
    if (!localAddressData || typeof localAddressData !== 'object') return;
    if (isAddressSourceFilled(localAddressData.residentialAreaType)) {
        localAddressData.residentialAreaTypeTrsl = copySourceToTrsl(localAddressData.residentialAreaType);
    }
    if (isAddressSourceFilled(localAddressData.streetType)) {
        localAddressData.streetTypeTrsl = copySourceToTrsl(localAddressData.streetType);
    }
}

/** addressRequest or customer `address` block — local + foreign + number/block/etc. */
function syncAddressRequestTrsl(addressBlock: any): void {
    if (!addressBlock || typeof addressBlock !== 'object') return;
    syncLocalAddressDataTrsl(addressBlock.localAddressData);

    const fad = addressBlock.foreignAddressData;
    if (fad && typeof fad === 'object') {
        if (isAddressSourceFilled(fad.residentialAreaType)) {
            fad.residentialAreaTypeTrsl = copySourceToTrsl(fad.residentialAreaType);
        }
        if (isAddressSourceFilled(fad.streetType)) {
            fad.streetTypeTrsl = copySourceToTrsl(fad.streetType);
        }
        for (const key of [
            'region',
            'municipality',
            'populatedPlace',
            'zipCode',
            'district',
            'residentialArea',
            'street',
        ] as const) {
            const trslKey = `${key}Trsl`;
            if (isAddressSourceFilled(fad[key])) {
                fad[trslKey] = copySourceToTrsl(fad[key]);
            }
        }
    }

    for (const [srcKey, trslKey] of [
        ['number', 'numberTrsl'],
        ['block', 'blockTrsl'],
        ['entrance', 'entranceTrsl'],
        ['floor', 'floorTrsl'],
        ['apartment', 'apartmentTrsl'],
        ['mailbox', 'mailboxTrsl'],
        ['additionalInformation', 'additionalInformationTrsl'],
    ] as const) {
        if (isAddressSourceFilled(addressBlock[srcKey])) {
            addressBlock[trslKey] = copySourceToTrsl(addressBlock[srcKey]);
        }
    }
}

async function ensureValidLocalAddressIds(Request: any): Promise<LocalAddressIds> {
    if (cachedLocalAddressIds) return cachedLocalAddressIds;

    const unique = `phn2130-${Date.now()}`;
    const mkActive = (name: string) => {
        const label = toUpperTrsl(`${name}${unique.replace(/-/g, '')}`);
        return {
            name: label,
            nameTransliterated: label,
            status: 'ACTIVE',
            defaultSelection: false,
        };
    };

    const countryResp = await Request.post('countries', { data: mkActive('Country') });
    await checkResponse(countryResp);
    const countryId = (await countryResp.json()).id as number;

    const regionResp = await Request.post('regions', { data: { ...mkActive('Region'), countryId } });
    await checkResponse(regionResp);
    const regionId = (await regionResp.json()).id as number;

    const municipalityResp = await Request.post('municipalities', { data: { ...mkActive('Municipality'), regionId } });
    await checkResponse(municipalityResp);
    const municipalityId = (await municipalityResp.json()).id as number;

    const ppResp = await Request.post('populated-places', { data: { ...mkActive('Populated place'), municipalityId } });
    await checkResponse(ppResp);
    const populatedPlaceId = (await ppResp.json()).id as number;

    const zipResp = await Request.post('zip-codes', { data: { ...mkActive('Zip'), populatedPlaceId } });
    await checkResponse(zipResp);
    const zipCodeId = (await zipResp.json()).id as number;

    const districtResp = await Request.post('districts', { data: { ...mkActive('District'), populatedPlaceId } });
    await checkResponse(districtResp);
    const districtId = (await districtResp.json()).id as number;

    const raResp = await Request.post('residential-areas', {
        data: { ...mkActive('Residential area'), type: 'QUARTER', populatedPlaceId },
    });
    await checkResponse(raResp);
    const residentialAreaId = (await raResp.json()).id as number;

    const streetResp = await Request.post('streets', {
        data: { ...mkActive('Street'), type: 'STREET', populatedPlaceId },
    });
    await checkResponse(streetResp);
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
    await checkResponse(res);
    return res.json();
}

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
    localAddressData.streetType = localAddressData.streetType ?? 'STREET';
    localAddressData.residentialAreaType = localAddressData.residentialAreaType ?? 'QUARTER';
}

function patchAddressBlock(address: any, ids: LocalAddressIds): void {
    if (!address?.localAddressData) return;
    patchLocalAddressData(address.localAddressData, ids);
    syncAddressRequestTrsl(address);
}

function patchCustomerPayloadAddresses(payload: any, ids: LocalAddressIds): void {
    patchAddressBlock(payload.address, ids);
    if (Array.isArray(payload.communicationData)) {
        for (const cd of payload.communicationData) {
            patchAddressBlock(cd?.address, ids);
        }
    }
}

async function hydrateCustomerPayloadAddressTrsl(Request: any, payload: any): Promise<void> {
    const blocks = [payload.address, ...(payload.communicationData?.map((cd: any) => cd?.address) ?? [])];
    for (const addr of blocks) {
        if (!addr) continue;
        if (addr.localAddressData) {
            await hydrateLocalAddressTrslFromNomenclature(Request, addr.localAddressData);
        }
        syncAddressRequestTrsl(addr);
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
    if (podPayload.addressRequest) {
        podPayload.addressRequest.number = podPayload.addressRequest.number ?? '1';
    }
    podPayload.name = latinUpper(String(podPayload.name ?? 'SETTLEMENT POD'));
    syncAddressRequestTrsl(podPayload.addressRequest);
    return podPayload;
}

async function resolveCommDataIdsByPurpose(
    Request: any,
    customerId: number,
): Promise<{ contractCommId: number; billingCommId: number }> {
    const details = await getCustomerDetails(Request, customerId);
    const entries = details.communicationData ?? [];
    let contractCommId: number | undefined;
    let billingCommId: number | undefined;
    for (const entry of entries) {
        const purposes = entry.contactPurposes ?? entry.contactPurpose ?? [];
        const purposeIds = purposes.map((p: any) => p.id ?? p.contactPurposeId);
        if (purposeIds.includes(envVariables.contact_purpose)) contractCommId = entry.id;
        if (purposeIds.includes(envVariables.billing_purpose)) billingCommId = entry.id;
    }
    if (!contractCommId && entries[0]?.id) contractCommId = entries[0].id;
    if (!billingCommId && entries[1]?.id) billingCommId = entries[1].id;
    if (!billingCommId && contractCommId) billingCommId = contractCommId;
    return { contractCommId: contractCommId!, billingCommId: billingCommId! };
}

function buildCommunicationEntry(ids: LocalAddressIds, purposeId: number, label: string, email: string) {
    const address = {
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
        number: '122',
        additionalInformation: null,
        block: null,
        entrance: null,
        floor: null,
        apartment: null,
        mailbox: null,
    };
    syncAddressRequestTrsl(address);
    return {
        status: 'ACTIVE',
        contactTypeName: label,
        contactPurposes: [{ contactPurposeId: purposeId, status: 'ACTIVE' }],
        address,
        communicationContacts: [
            {
                sendSms: false,
                platformId: null,
                status: 'ACTIVE',
                contactType: 'EMAIL',
                contactValue: email,
            },
            {
                sendSms: false,
                platformId: null,
                status: 'ACTIVE',
                contactType: 'MOBILE_NUMBER',
                contactValue: `35988${randomGens.generateRandomString(false, true, 7)}`,
            },
        ],
        contactPersons: [],
    };
}

function addDaysIso(isoDate: string, days: number): string {
    const d = new Date(`${isoDate}T00:00:00.000Z`);
    d.setUTCDate(d.getUTCDate() + days);
    return d.toISOString().slice(0, 10);
}

async function getInternalProductContract(Request: any, contractId: number, versionId: number) {
    const response = await Request.get(`product-contract/${contractId}?versionId=${versionId}`);
    await checkResponse(response);
    return response.json();
}

/**
 * Sales Portal PUT customer address — foreign free-text shape (matches working manual payload on dev2).
 */
function buildSalesPortalForeignAddressForPut(overrides: Record<string, unknown> = {}) {
    const foreignAddressData = {
        countryId: envVariables.countries,
        ...SP_FOREIGN_ADDRESS_TEXT,
    };
    const address = {
        isForeign: true,
        foreignAddressData,
        number: SP_ADDRESS_NUMBER,
        additionalInformation: SP_ADDRESS_ADDITIONAL_INFO,
        block: 'A',
        entrance: '1',
        floor: '2',
        apartment: '10',
        mailbox: 'MB-1',
        ...overrides,
    };
    syncAddressRequestTrsl(address);
    return address;
}

let cachedDefaultSegmentId: number | undefined;

/** ACTIVE segment with defaultSelection=true (Sales Portal expects nomenclature segment IDs, not CustomerSegment row ids). */
async function getDefaultSegmentId(Request: any): Promise<number> {
    if (cachedDefaultSegmentId !== undefined) {
        return cachedDefaultSegmentId;
    }
    const response = await Request.get('segments?statuses=ACTIVE&page=0&size=100');
    await checkResponse(response);
    const body = (await response.json()) as { content?: Array<{ id: number; defaultSelection?: boolean }> };
    const defaultSeg = (body.content ?? []).find((s) => s.defaultSelection === true);
    if (!defaultSeg?.id) {
        throw new Error('No ACTIVE segment with defaultSelection=true — required for Sales Portal customer PUT.');
    }
    cachedDefaultSegmentId = Number(defaultSeg.id);
    return cachedDefaultSegmentId;
}

/** Nomenclature segment IDs from customer GET — never use CustomerSegment link row `id` (e.g. 38624). */
async function resolveCustomerSegmentIds(Request: any, details: any): Promise<number[]> {
    for (const src of [details]) {
        if (!src) continue;
        if (Array.isArray(src.customerSegments) && src.customerSegments.length > 0) {
            const ids = src.customerSegments
                .map((s: any) => Number(s.segment?.id ?? s.segmentId))
                .filter((id: number) => Number.isFinite(id) && id > 0);
            if (ids.length > 0) {
                return ids;
            }
        }
        if (Array.isArray(src.segmentIds) && src.segmentIds.length > 0) {
            return src.segmentIds.map((id: unknown) => Number(id)).filter((id: number) => Number.isFinite(id));
        }
    }
    return [await getDefaultSegmentId(Request)];
}

async function buildPrivateCustomerPutSection(
    Request: any,
    details: any,
    overrides: CustomerPutOverrides = {},
    opts: CustomerPutSectionOpts = {},
) {
    const pcd = details.privateCustomerDetails ?? {};
    const firstName = latinUpper(String(pcd.firstName ?? DEFAULT_CUSTOMER_FIRST));
    const lastName = latinUpper(String(pcd.lastName ?? DEFAULT_CUSTOMER_LAST));
    const middleName = latinUpper(String(pcd.middleName ?? DEFAULT_MIDDLE_NAME));
    const detailsKycPassed = pcd.kycPassed ?? details.kycPassed ?? false;
    const detailsKycExpirationDate = pcd.kycExpirationDate ?? details.kycExpirationDate ?? null;

    const base: Record<string, unknown> = {
        name: firstName,
        nameTransliterated: latinUpper(String(pcd.firstNameTranslated ?? firstName)),
        middleName,
        middleNameTransliterated: latinUpper(String(pcd.middleNameTranslated ?? middleName)),
        lastName,
        lastNameTransliterated: latinUpper(String(pcd.lastNameTranslated ?? lastName)),
        customerSegmentIds: await resolveCustomerSegmentIds(Request, details),
        address: buildSalesPortalForeignAddressForPut(),
        ...applyCustomerPutUppercaseOverrides(overrides),
    };

    if (!opts.omitKycFields) {
        const effectiveKycPassed =
            overrides.kycPassed !== undefined ? overrides.kycPassed : detailsKycPassed;
        base.kycPassed = effectiveKycPassed;

        const overrideExpiration = overrides.kycExpirationDate;
        if (overrideExpiration != null) {
            base.kycExpirationDate = String(overrideExpiration).slice(0, 10);
        } else if (detailsKycExpirationDate != null && overrides.kycPassed === undefined) {
            base.kycExpirationDate = String(detailsKycExpirationDate).slice(0, 10);
        } else if (effectiveKycPassed && !opts.skipAutoKycExpiration) {
            base.kycExpirationDate = '2026-11-30';
        }
    }

    if (
        details.businessActivity === true
        || details.customerType === 'PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY'
    ) {
        const bcd = details.businessCustomerDetails ?? {};
        const ownershipFormId = details.ownershipFormId?.id ?? details.ownershipFormId;
        const economicBranchId = details.economicBranchId?.id ?? details.economicBranchId;
        const legalFormId = bcd.legalFormId?.id ?? bcd.legalFormId;
        const legalFormTransliteratedId =
            bcd.legalFormTransId?.id ?? bcd.legalFormTransId ?? bcd.legalFormTranslId?.id;
        const mainSubjectOfActivity =
            details.mainSubjectOfActivity ?? details.mainActivitySubject ?? 'MAINSUBJECT';

        Object.assign(base, {
            ownershipFormId: Number(ownershipFormId ?? envVariables.form_of_ownership),
            economicBranchId: Number(
                economicBranchId ?? envVariables.economic_branch_based_on_commercial_information,
            ),
            mainSubjectOfActivity: String(mainSubjectOfActivity),
            legalFormId: Number(legalFormId ?? envVariables.legal_forms),
            legalFormTransliteratedId: Number(
                legalFormTransliteratedId ?? legalFormId ?? envVariables.legal_forms,
            ),
        });
    }

    return base;
}

// ─── Entity chain helpers ──────────────────────────────────────────────────────

async function sharedTerm(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.productAndServices.term();
    payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
    payload.contractEntryIntoForces = ['SIGNING'];
    payload.startsOfContractInitialTerms = ['SIGNING'];
    payload.waitForOldContractTermToExpires = ['NO'];
    const response = await Request.post(Endpoints.terms, { data: payload });
    await checkResponse(response);
    Responses.terms.push(await response.json());
}

/** Term with two invoice payment terms so TC-BE-25 can change invoicePaymentTermId via SP PUT. */
async function sharedTermWithDualInvoicePaymentTerms(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
) {
    const payload = GeneratePayload.productAndServices.term();
    payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
    payload.contractEntryIntoForces = ['SIGNING'];
    payload.startsOfContractInitialTerms = ['SIGNING'];
    payload.waitForOldContractTermToExpires = ['NO'];
    const firstIpt = payload.invoicePaymentTerms[0];
    const secondIpt = {
        ...JSON.parse(JSON.stringify(firstIpt)),
        id: null,
        value: TC_BE_25_SECOND_INVOICE_PAYMENT_TERM_VALUE,
        name: `${TC_BE_25_SECOND_INVOICE_PAYMENT_TERM_VALUE} WORKING_DAYS ( )`,
    };
    payload.invoicePaymentTerms = [firstIpt, secondIpt];
    const response = await Request.post(Endpoints.terms, { data: payload });
    await checkResponse(response);
    Responses.terms.push(await response.json());
}

async function sharedPriceElectricity(Request: any, GeneratePayload: any, Responses: any, Endpoints: any) {
    const payload = GeneratePayload.productAndServices.electricity();
    payload.value = 0.15;
    const response = await Request.post(Endpoints.priceComponent, { data: payload });
    await checkResponse(response);
    Responses.priceComponent.push(await response.json());
}

async function sharedProduct(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    termIndex = 0,
    priceComponentIndex?: number,
) {
    const payload = GeneratePayload.productAndServices.product(termIndex);
    payload.productStatus = 'ACTIVE';
    payload.availableForSale = true;
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    payload.contractEntryIntoForces = ['SIGNING'];
    payload.startsOfContractInitialTerms = ['SIGNING'];
    payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
    payload.waitForOldContractTermToExpires = ['NO'];
    payload.templateIds = await resolveProductTemplateIds(Request);
    if (priceComponentIndex !== undefined) {
        payload.priceComponentIds = [
            resolvePriceComponentId(Responses.priceComponent[priceComponentIndex]),
        ];
    }
    const response = await Request.post(Endpoints.product, { data: payload });
    await checkResponse(response);
    Responses.product.push(await response.json());
}

/** POST /price-component body may be a bare number or `{ id }` — product expects numeric PC ids. */
function resolvePriceComponentId(raw: unknown): number {
    const id = typeof raw === 'number' ? raw : Number((raw as { id?: unknown })?.id);
    if (!Number.isFinite(id) || id <= 0) {
        throw new Error(`Price component missing id: ${JSON.stringify(raw).slice(0, 200)}`);
    }
    return id;
}

/** POST /iap body may be a bare number or `{ id }` — product expects numeric IAP ids in Responses.interim. */
function resolveInterimAdvancePaymentId(raw: unknown): number {
    const id = typeof raw === 'number' ? raw : Number((raw as { id?: unknown })?.id);
    if (!Number.isFinite(id) || id <= 0) {
        throw new Error(`IAP POST response missing id: ${JSON.stringify(raw).slice(0, 200)}`);
    }
    return id;
}

/**
 * Bind catalogue IAP + price component ids on product POST (pdt-2872 pattern).
 * GeneratePayload.product() usually fills these from Responses — explicit for TC-BE-18/27.
 */
function attachProductInterimAndPriceComponents(
    payload: Record<string, unknown>,
    Responses: any,
    interimId: number,
) {
    const priceIds = (Responses.priceComponent ?? [])
        .map((pc: unknown) => (typeof pc === 'number' ? pc : Number((pc as { id?: unknown })?.id)))
        .filter((id: number) => Number.isFinite(id) && id > 0);
    if (priceIds.length) {
        payload.priceComponentIds = priceIds;
    }
    payload.interimAdvancePayments = [interimId];
    payload.interimAdvancePaymentGroups = [];
    payload.priceComponentGroupIds = [];
}

/**
 * Product with non-fixed IAP (TC-BE-18).
 * Phoenix `SalesPortalProductFixedParametersValidator` rejects dynamic IAP ranges
 * (`valueFrom` / `valueTo` with no fixed `value`).
 */
async function sharedProductWithNonFixedIap(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
) {
    const iapPayload = GeneratePayload.productAndServices.interim();
    iapPayload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
    iapPayload.value = null;
    iapPayload.valueFrom = 1;
    iapPayload.valueTo = 10;
    iapPayload.missingInvoice = false;
    iapPayload.priceComponentId = null;
    const iapResponse = await Request.post(Endpoints.interim, { data: iapPayload });
    await checkResponse(iapResponse);
    const interimId = resolveInterimAdvancePaymentId(await iapResponse.json());
    Responses.interim.push(interimId);

    const payload = GeneratePayload.productAndServices.product();
    payload.productStatus = 'ACTIVE';
    payload.availableForSale = true;
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    payload.contractEntryIntoForces = ['SIGNING'];
    payload.startsOfContractInitialTerms = ['SIGNING'];
    payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
    payload.waitForOldContractTermToExpires = ['NO'];
    payload.templateIds = await resolveProductTemplateIds(Request);
    attachProductInterimAndPriceComponents(payload, Responses, interimId);
    const response = await Request.post(Endpoints.product, { data: payload });
    await checkResponse(response);
    Responses.product.push(await response.json());
}

/** Price component without a filled formula variable value (TC-BE-24). */
async function sharedPriceElectricityUnfilled(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
) {
    const payload = GeneratePayload.productAndServices.electricity();
    payload.formulaRequest.expression = '$X1$';
    payload.formulaRequest.variables = [
        {
            variable: 'X1',
            description: 'TC-BE-24 UNFILLED PRICE COMPONENT',
            value: null,
        },
    ];
    const response = await Request.post(Endpoints.priceComponent, { data: payload });
    await checkResponse(response);
    Responses.priceComponent.push(await response.json());
}

/** Product with EXACT_AMOUNT IAP (TC-BE-27 — fixed IAP accepted on SP PUT). */
async function sharedProductWithFixedIap(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
) {
    const iapPayload = GeneratePayload.productAndServices.interim();
    iapPayload.valueType = 'EXACT_AMOUNT';
    iapPayload.value = 50;
    iapPayload.missingInvoice = false;
    iapPayload.priceComponentId = null;
    iapPayload.currencyId = envVariables.currency;
    const iapResponse = await Request.post(Endpoints.interim, { data: iapPayload });
    await checkResponse(iapResponse);
    const interimId = resolveInterimAdvancePaymentId(await iapResponse.json());
    Responses.interim.push(interimId);

    const payload = GeneratePayload.productAndServices.product();
    payload.productStatus = 'ACTIVE';
    payload.availableForSale = true;
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    payload.contractEntryIntoForces = ['SIGNING'];
    payload.startsOfContractInitialTerms = ['SIGNING'];
    payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
    payload.waitForOldContractTermToExpires = ['NO'];
    payload.templateIds = await resolveProductTemplateIds(Request);
    attachProductInterimAndPriceComponents(payload, Responses, interimId);
    const response = await Request.post(Endpoints.product, { data: payload });
    await checkResponse(response);
    Responses.product.push(await response.json());
}

/** Product with one additional parameter on POST (TC-BE-25 / TC-BE-26). */
async function sharedProductWithAdditionalParams(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
) {
    const payload = GeneratePayload.productAndServices.product();
    payload.productStatus = 'ACTIVE';
    payload.availableForSale = true;
    payload.contractTypes = ['SUPPLY_ONLY'];
    payload.paymentGuarantees = ['NO'];
    payload.contractEntryIntoForces = ['SIGNING'];
    payload.startsOfContractInitialTerms = ['SIGNING'];
    payload.supplyActivations = ['FIRST_DAY_OF_MONTH', 'EXACT_DATE'];
    payload.waitForOldContractTermToExpires = ['NO'];
    payload.templateIds = await resolveProductTemplateIds(Request);
    payload.productAdditionalParams = [
        {
            orderingId: 1,
            label: ADDITIONAL_PARAM_LABEL,
            value: ADDITIONAL_PARAM_INITIAL_VALUE,
        },
    ];
    const response = await Request.post(Endpoints.product, { data: payload });
    await checkResponse(response);
    Responses.product.push(await response.json());
}

/** TC-BE-29: second full local-address nomenclature chain (distinct from cached baseline). */
async function createAlternateLocalAddressIds(Request: any): Promise<LocalAddressIds> {
    const unique = `phn2130-alt-${Date.now()}`;
    const mkActive = (name: string) => {
        const label = toUpperTrsl(`${name}${unique.replace(/-/g, '')}`);
        return {
            name: label,
            nameTransliterated: label,
            status: 'ACTIVE',
            defaultSelection: false,
        };
    };

    const countryResp = await Request.post('countries', { data: mkActive('CountryB') });
    await checkResponse(countryResp);
    const countryId = (await countryResp.json()).id as number;

    const regionResp = await Request.post('regions', { data: { ...mkActive('RegionB'), countryId } });
    await checkResponse(regionResp);
    const regionId = (await regionResp.json()).id as number;

    const municipalityResp = await Request.post('municipalities', { data: { ...mkActive('MunicipalityB'), regionId } });
    await checkResponse(municipalityResp);
    const municipalityId = (await municipalityResp.json()).id as number;

    const ppResp = await Request.post('populated-places', { data: { ...mkActive('PopulatedPlaceB'), municipalityId } });
    await checkResponse(ppResp);
    const populatedPlaceId = (await ppResp.json()).id as number;

    const zipResp = await Request.post('zip-codes', { data: { ...mkActive('ZipB'), populatedPlaceId } });
    await checkResponse(zipResp);
    const zipCodeId = (await zipResp.json()).id as number;

    const districtResp = await Request.post('districts', { data: { ...mkActive('DistrictB'), populatedPlaceId } });
    await checkResponse(districtResp);
    const districtId = (await districtResp.json()).id as number;

    const raResp = await Request.post('residential-areas', {
        data: {
            ...mkActive('ResidentialAreaB'),
            type: TC_BE_29_ADDRESS_UPDATED.residentialAreaType,
            populatedPlaceId,
        },
    });
    await checkResponse(raResp);
    const residentialAreaId = (await raResp.json()).id as number;

    const streetResp = await Request.post('streets', {
        data: {
            ...mkActive('StreetB'),
            type: TC_BE_29_ADDRESS_UPDATED.streetType,
            populatedPlaceId,
        },
    });
    await checkResponse(streetResp);
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

function buildSalesPortalLocalAddressForPut(
    ids: LocalAddressIds,
    scalars: Partial<LocalAddressScalars> = {},
) {
    return {
        isForeign: false,
        localAddressData: {
            countryId: ids.countryId,
            regionId: ids.regionId,
            municipalityId: ids.municipalityId,
            populatedPlaceId: ids.populatedPlaceId,
            zipCodeId: ids.zipCodeId,
            districtId: ids.districtId,
            residentialAreaId: ids.residentialAreaId,
            streetId: ids.streetId,
            streetType: scalars.streetType ?? 'STREET',
            residentialAreaType: scalars.residentialAreaType ?? 'QUARTER',
        },
        number: scalars.number ?? SP_ADDRESS_NUMBER,
        additionalInformation: scalars.additionalInformation ?? SP_ADDRESS_ADDITIONAL_INFO,
        block: scalars.block ?? 'A',
        entrance: scalars.entrance ?? '1',
        floor: scalars.floor ?? '2',
        apartment: scalars.apartment ?? '10',
        mailbox: scalars.mailbox ?? 'MB-1',
    };
}

function resolveNomenclatureId(value: unknown): number | null {
    if (value == null) return null;
    if (typeof value === 'number' && Number.isFinite(value)) return value;
    if (typeof value === 'object' && value !== null && 'id' in value) {
        const id = Number((value as { id: unknown }).id);
        return Number.isFinite(id) ? id : null;
    }
    return null;
}

function readInternalCustomerLocalAddress(details: any): {
    foreign: boolean;
    ids: LocalAddressIds;
    scalars: LocalAddressScalars;
} {
    const nested = details.address?.localAddressData;
    if (nested) {
        return {
            foreign: Boolean(details.address?.foreign ?? details.foreignAddress),
            ids: {
                countryId: Number(nested.countryId),
                regionId: Number(nested.regionId),
                municipalityId: Number(nested.municipalityId),
                populatedPlaceId: Number(nested.populatedPlaceId),
                zipCodeId: Number(nested.zipCodeId),
                districtId: Number(nested.districtId),
                residentialAreaId: Number(nested.residentialAreaId),
                streetId: Number(nested.streetId),
            },
            scalars: {
                number: String(details.address?.number ?? ''),
                additionalInformation: String(details.address?.additionalInformation ?? ''),
                block: String(details.address?.block ?? ''),
                entrance: String(details.address?.entrance ?? ''),
                floor: String(details.address?.floor ?? ''),
                apartment: String(details.address?.apartment ?? ''),
                mailbox: String(details.address?.mailbox ?? ''),
                streetType: String(nested.streetType ?? ''),
                residentialAreaType: String(nested.residentialAreaType ?? ''),
            },
        };
    }

    return {
        foreign: Boolean(details.foreignAddress),
        ids: {
            countryId: resolveNomenclatureId(details.countryId) ?? 0,
            regionId: resolveNomenclatureId(details.localRegion) ?? 0,
            municipalityId: resolveNomenclatureId(details.localMunicipality) ?? 0,
            populatedPlaceId: resolveNomenclatureId(details.populatedPlaceId) ?? 0,
            zipCodeId: resolveNomenclatureId(details.zipCode) ?? 0,
            districtId: resolveNomenclatureId(details.districtId) ?? 0,
            residentialAreaId: resolveNomenclatureId(details.residentialAreaId) ?? 0,
            streetId: resolveNomenclatureId(details.streetId) ?? 0,
        },
        scalars: {
            number: String(details.streetNumber ?? ''),
            additionalInformation: String(details.addressAdditionalInfo ?? ''),
            block: String(details.block ?? ''),
            entrance: String(details.entrance ?? ''),
            floor: String(details.floor ?? ''),
            apartment: String(details.apartment ?? ''),
            mailbox: String(details.mailbox ?? ''),
            streetType: String(details.streetType ?? ''),
            residentialAreaType: String(details.residentialAreaType ?? ''),
        },
    };
}

function expectLocalAddressIds(actual: LocalAddressIds, expected: LocalAddressIds): void {
    expect(actual.countryId).toBe(expected.countryId);
    expect(actual.regionId).toBe(expected.regionId);
    expect(actual.municipalityId).toBe(expected.municipalityId);
    expect(actual.populatedPlaceId).toBe(expected.populatedPlaceId);
    expect(actual.zipCodeId).toBe(expected.zipCodeId);
    expect(actual.districtId).toBe(expected.districtId);
    expect(actual.residentialAreaId).toBe(expected.residentialAreaId);
    expect(actual.streetId).toBe(expected.streetId);
}

function expectLocalAddressScalars(actual: LocalAddressScalars, expected: LocalAddressScalars): void {
    expect(actual.number).toBe(expected.number);
    expect(actual.additionalInformation).toBe(expected.additionalInformation);
    expect(actual.block).toBe(expected.block);
    expect(actual.entrance).toBe(expected.entrance);
    expect(actual.floor).toBe(expected.floor);
    expect(actual.apartment).toBe(expected.apartment);
    expect(actual.mailbox).toBe(expected.mailbox);
    expect(actual.streetType).toBe(expected.streetType);
    expect(actual.residentialAreaType).toBe(expected.residentialAreaType);
}

function snapshotSpCommunicationAddress(comm: Record<string, unknown>) {
    return {
        countryId: comm.countryId ?? null,
        regionId: comm.regionId ?? null,
        municipalityId: comm.municipalityId ?? null,
        populatedPlaceId: comm.populatedPlaceId ?? null,
        zipCodeId: comm.zipCodeId ?? null,
        districtId: comm.districtId ?? null,
        residentialAreaId: comm.residentialAreaId ?? null,
        streetId: comm.streetId ?? null,
        buildingNumber: comm.buildingNumber ?? null,
        block: comm.block ?? null,
        entrance: comm.entrance ?? null,
        apartment: comm.apartment ?? null,
        mailbox: comm.mailbox ?? null,
        floor: comm.floor ?? null,
        number: comm.number ?? null,
        listStreetBoulevard: comm.listStreetBoulevard ?? null,
    };
}

/**
 * TC-BE-25: Sales Portal PUT maps flat contract fields (SalesPortalContractUpdateRequest /
 * SalesPortalContractUpdateMapper) — not nested productParameters.
 * Tested: contractType, contractTermId, paymentGuarantee, cash/bank deposit amounts+currencies,
 * invoicePaymentTermId, entryIntoForce, startOfInitialTerm(+Date), supplyActivationAfterContractResigning(+Date),
 * waitForOldContractTermToExpire.
 * Excluded (not on SP PUT): interimAdvancePayments, contractFormulas, productAdditionalParams,
 * invoicePaymentTermValue, guaranteeInformation, guaranteeContract, monthlyInstallment*.
 */
type SpThirdTabInternalExpected = {
    contractType?: string;
    productContractTermId?: number;
    paymentGuarantee?: string;
    cashDeposit?: number;
    cashDepositCurrencyId?: number;
    bankGuarantee?: number;
    bankGuaranteeCurrencyId?: number;
    invoicePaymentTermId?: number;
    invoicePaymentTermValue?: number;
    entryIntoForce?: string;
    entryIntoForceValue?: string | null;
    startOfContractInitialTerm?: string;
    startOfContractValue?: string | null;
    supplyActivation?: string;
    supplyActivationValue?: string | null;
    productContractWaitForOldContractTermToExpires?: string;
};

function buildSpThirdTabPutOverrides(
    ctx: ContractChainContext,
    updates: ContractPutOverrides = {},
): ContractPutOverrides {
    return {
        contractType: 'SUPPLY_ONLY',
        contractTermId: ctx.productContractTermId,
        paymentGuarantee: 'NO',
        cashDepositAmount: TC_BE_25_CASH_DEPOSIT_AMOUNT,
        cashDepositCurrencyId: CURRENCY_ID,
        bankGuaranteeAmount: TC_BE_25_BANK_GUARANTEE_AMOUNT,
        bankGuaranteeCurrencyId: CURRENCY_ID,
        invoicePaymentTermId: ctx.invoicePaymentTermId,
        entryIntoForce: 'SIGNING',
        startOfInitialTerm: 'SIGNING',
        startOfInitialTermDate: ctx.startOfInitialTermDate,
        supplyActivationAfterContractResigning: 'EXACT_DATE',
        supplyActivationAfterContractResigningDate: ctx.supplyActivationAfterContractResigningDate,
        waitForOldContractTermToExpire: 'NO',
        ...updates,
    };
}

function normalizeIsoDate(value: unknown): string | null {
    if (value === null || value === undefined || value === '') {
        return null;
    }
    return String(value).slice(0, 10);
}

function assertInternalThirdTabProductParameters(pp: any, expected: SpThirdTabInternalExpected) {
    if (expected.contractType !== undefined) {
        expect(pp.contractType).toBe(expected.contractType);
    }
    if (expected.productContractTermId !== undefined) {
        const termId = pp.contractTerm?.id ?? pp.productContractTermId;
        expect(Number(termId)).toBe(expected.productContractTermId);
    }
    if (expected.paymentGuarantee !== undefined) {
        expect(pp.paymentGuarantee).toBe(expected.paymentGuarantee);
    }
    if (expected.cashDeposit !== undefined && pp.cashDeposit != null) {
        expect(Number(pp.cashDeposit)).toBeCloseTo(expected.cashDeposit, 2);
    }
    if (expected.cashDepositCurrencyId !== undefined && pp.cashDepositCurrency?.id != null) {
        expect(Number(pp.cashDepositCurrency.id)).toBe(expected.cashDepositCurrencyId);
    }
    if (expected.bankGuarantee !== undefined && pp.bankGuarantee != null) {
        expect(Number(pp.bankGuarantee)).toBeCloseTo(expected.bankGuarantee, 2);
    }
    if (expected.bankGuaranteeCurrencyId !== undefined && pp.bankDepositCurrency?.id != null) {
        expect(Number(pp.bankDepositCurrency.id)).toBe(expected.bankGuaranteeCurrencyId);
    }
    if (expected.invoicePaymentTermId !== undefined) {
        const iptId = pp.invoicePaymentTerm?.id ?? pp.invoicePaymentTermId;
        expect(Number(iptId)).toBe(expected.invoicePaymentTermId);
    }
    if (expected.invoicePaymentTermValue !== undefined) {
        expect(Number(pp.invoicePaymentTermValue)).toBe(expected.invoicePaymentTermValue);
    }
    if (expected.entryIntoForce !== undefined) {
        expect(pp.entryIntoForce).toBe(expected.entryIntoForce);
    }
    if (expected.entryIntoForceValue !== undefined) {
        expect(normalizeIsoDate(pp.entryIntoForceValue)).toBe(expected.entryIntoForceValue);
    }
    if (expected.startOfContractInitialTerm !== undefined) {
        expect(pp.startOfContractInitialTerm).toBe(expected.startOfContractInitialTerm);
    }
    if (expected.startOfContractValue !== undefined) {
        expect(normalizeIsoDate(pp.startOfContractValue)).toBe(expected.startOfContractValue);
    }
    if (expected.supplyActivation !== undefined) {
        expect(pp.supplyActivation).toBe(expected.supplyActivation);
    }
    if (expected.supplyActivationValue !== undefined) {
        expect(normalizeIsoDate(pp.supplyActivationValue)).toBe(expected.supplyActivationValue);
    }
    if (expected.productContractWaitForOldContractTermToExpires !== undefined) {
        expect(pp.productContractWaitForOldContractTermToExpires).toBe(
            expected.productContractWaitForOldContractTermToExpires,
        );
    }
}

function buildContractProductParametersPut(
    ctx: ContractChainContext,
    overrides: Record<string, unknown> = {},
) {
    return {
        contractType: 'SUPPLY_ONLY',
        productContractTermId: ctx.productContractTermId,
        paymentGuarantee: 'NO',
        cashDeposit: null,
        bankGuarantee: null,
        contractFormulas: [],
        invoicePaymentTermId: ctx.invoicePaymentTermId,
        invoicePaymentTermValue: 12,
        entryIntoForce: 'SIGNING',
        startOfContractInitialTerm: 'SIGNING',
        supplyActivation: 'FIRST_DAY_OF_MONTH',
        productContractWaitForOldContractTermToExpires: 'NO',
        monthlyInstallmentValue: null,
        monthlyInstallmentAmount: null,
        interimAdvancePayments: [],
        productAdditionalParams: [],
        ...overrides,
    };
}

async function getProductDetailIdFromContract(
    Request: any,
    contractId: number,
    versionId: number,
): Promise<number> {
    const detail = await getInternalProductContract(Request, contractId, versionId);
    const productDetailId = Number(detail.basicParameters?.productDetailId);
    if (!Number.isFinite(productDetailId) || productDetailId <= 0) {
        throw new Error(`Missing productDetailId on contract ${contractId} v${versionId}`);
    }
    return productDetailId;
}

async function getThirdTabFields(Request: any, productDetailId: number) {
    const response = await Request.get(`product-contract/third-tab-fields?productDetailId=${productDetailId}`);
    await checkResponse(response);
    return response.json();
}

async function getInternalContractProxies(Request: any, contractId: number, versionId: number) {
    const detail = await getInternalProductContract(Request, contractId, versionId);
    return detail.basicParameters?.proxy ?? [];
}

async function createLegalEntityCustomer(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    overrides: {
        managerFirstName?: string;
        managerLastName?: string;
        companyName?: string;
    } = {},
) {
    const addressIds = await ensureValidLocalAddressIds(Request);
    const payload = GeneratePayload.customers.customer_legal();
    patchCustomerPayloadAddresses(payload, addressIds);
    const companyName = latinUpper(overrides.companyName ?? LEGAL_ENTITY_COMPANY);
    payload.businessCustomerDetails.name = companyName;
    payload.businessCustomerDetails.nameTranslated = companyName;
    payload.managers = [
        {
            id: null,
            titleId: envVariables.title,
            name: latinUpper(overrides.managerFirstName ?? MGR_GEORGI),
            middleName: null,
            surname: latinUpper(overrides.managerLastName ?? MGR_NIKOLOV),
            personalNumber: null,
            jobPosition: MANAGER_JOB_POSITION,
            positionHeldFrom: null,
            positionHeldTo: null,
            birthDate: null,
            representationMethodId: envVariables.method_of_representation,
            additionalInformation: null,
            status: 'ACTIVE',
            phones: ['+359888111111'],
            emails: ['mgr1@test.com'],
        },
    ];
    await hydrateCustomerPayloadAddressTrsl(Request, payload);
    const response = await Request.post(Endpoints.customer, { data: payload });
    await checkResponse(response);
    const body = await response.json();
    Responses.customer.push(body);
    return body;
}

async function buildLegalEntityCustomerPutSection(
    Request: any,
    details: any,
    overrides: CustomerPutOverrides = {},
) {
    const bcd = details.businessCustomerDetails ?? {};
    const companyName = latinUpper(String(bcd.name ?? LEGAL_ENTITY_COMPANY));
    const ownershipFormId = details.ownershipFormId?.id ?? details.ownershipFormId;
    const economicBranchId = details.economicBranchId?.id ?? details.economicBranchId;
    const legalFormId = bcd.legalFormId?.id ?? bcd.legalFormId;
    const legalFormTransliteratedId =
        bcd.legalFormTransId?.id ?? bcd.legalFormTransId ?? bcd.legalFormTranslId?.id;

    return {
        name: companyName,
        nameTransliterated: latinUpper(String(bcd.nameTranslated ?? bcd.nameTransl ?? companyName)),
        customerSegmentIds: await resolveCustomerSegmentIds(Request, details),
        ownershipFormId: Number(ownershipFormId ?? envVariables.form_of_ownership),
        economicBranchId: Number(
            economicBranchId ?? envVariables.economic_branch_based_on_commercial_information,
        ),
        mainSubjectOfActivity: String(details.mainSubjectOfActivity ?? details.mainActivitySubject ?? 'MAINSUBJECT'),
        legalFormId: Number(legalFormId ?? envVariables.legal_forms),
        legalFormTransliteratedId: Number(
            legalFormTransliteratedId ?? legalFormId ?? envVariables.legal_forms,
        ),
        address: buildSalesPortalForeignAddressForPut(),
        ...applyCustomerPutUppercaseOverrides(overrides),
    };
}

async function buildCustomerPutSectionAuto(
    Request: any,
    details: any,
    overrides: CustomerPutOverrides = {},
    opts: CustomerPutSectionOpts = {},
) {
    if (details.customerType === 'LEGAL_ENTITY') {
        return buildLegalEntityCustomerPutSection(Request, details, overrides);
    }
    return buildPrivateCustomerPutSection(Request, details, overrides, opts);
}

async function sharedPod(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    yearlyConsumptionKwh: number = DEFAULT_YEARLY_CONSUMPTION_KWH,
) {
    const payload = await preparePodSettlementPayload(Request, GeneratePayload);
    payload.estimatedMonthlyAvgConsumption = String(podMonthlyAvgForYearlyContractKwh(yearlyConsumptionKwh));
    payload.activationDate = isoDate(-1);
    const response = await Request.post(Endpoints.pod, { data: payload });
    await checkResponse(response);
    Responses.pod.push(await response.json());
}

/** SP PUT consumption must match POD monthly sum; update POD before raising contract kWh. */
async function updatePodMonthlyForYearlyConsumption(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    yearlyKwh: number,
    podIndex = 0,
): Promise<void> {
    const monthly = podMonthlyAvgForYearlyContractKwh(yearlyKwh);
    const podId = Number(Responses.pod[podIndex].id);
    const getRes = await Request.get(`${Endpoints.pod}/${podId}?version=1`);
    await checkResponse(getRes);
    const podJson = await getRes.json();
    const editPayload = await preparePodSettlementPayload(Request, GeneratePayload);
    editPayload.identifier = String(podJson.identifier);
    editPayload.name = String(podJson.name ?? editPayload.name);
    editPayload.estimatedMonthlyAvgConsumption = String(monthly);
    if (podJson.gridOperatorId != null) {
        editPayload.gridOperatorId = podJson.gridOperatorId;
    }
    editPayload.updateExistingVersion = true;
    editPayload.versionId = podJson.versionId ?? podJson.versions?.[0]?.version ?? 1;
    const putRes = await Request.put(`${Endpoints.pod}/${podId}`, { data: editPayload });
    await checkResponse(putRes);
    const refresh = await Request.get(`${Endpoints.pod}/${podId}?version=1`);
    await checkResponse(refresh);
    Responses.pod[podIndex] = { ...Responses.pod[podIndex], ...(await refresh.json()) };
}

async function createPrivateCustomer(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    overrides: {
        firstName?: string;
        lastName?: string;
        contractEmail?: string;
        billingEmail?: string;
        kycPassed?: boolean;
        kycExpirationDate?: string;
    } = {},
) {
    const payload = GeneratePayload.customers.customer_private();
    payload.businessActivity = false;
    const addressIds = await ensureValidLocalAddressIds(Request);
    patchCustomerPayloadAddresses(payload, addressIds);
    const firstName = latinUpper(overrides.firstName ?? DEFAULT_CUSTOMER_FIRST);
    const lastName = latinUpper(overrides.lastName ?? DEFAULT_CUSTOMER_LAST);
    const middleName = latinUpper(DEFAULT_MIDDLE_NAME);
    payload.privateCustomerDetails.firstName = firstName;
    payload.privateCustomerDetails.firstNameTranslated = firstName;
    payload.privateCustomerDetails.middleName = middleName;
    payload.privateCustomerDetails.middleNameTranslated = middleName;
    payload.privateCustomerDetails.lastName = lastName;
    payload.privateCustomerDetails.lastNameTranslated = lastName;
    if (overrides.kycPassed !== undefined) {
        payload.privateCustomerDetails.kycPassed = overrides.kycPassed;
    }
    if (overrides.kycExpirationDate !== undefined) {
        payload.privateCustomerDetails.kycExpirationDate = overrides.kycExpirationDate;
    }
    payload.communicationData = [
        buildCommunicationEntry(addressIds, envVariables.contact_purpose, 'CONTRACT COMM', overrides.contractEmail ?? 'contract@test.com'),
        buildCommunicationEntry(addressIds, envVariables.billing_purpose, 'BILLING COMM', overrides.billingEmail ?? 'billing@test.com'),
    ];
    await hydrateCustomerPayloadAddressTrsl(Request, payload);
    const response = await Request.post(Endpoints.customer, { data: payload });
    await checkResponse(response);
    const body = await response.json();
    Responses.customer.push(body);
    return body;
}

async function createPrivateBusinessCustomer(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    overrides: {
        managerFirstName?: string;
        managerLastName?: string;
        managerPhones?: string[];
        managerEmails?: string[];
        customerFirstName?: string;
        customerLastName?: string;
    } = {},
) {
    const payload = GeneratePayload.customers.customer_private_business();
    payload.businessActivity = true;
    const addressIds = await ensureValidLocalAddressIds(Request);
    patchCustomerPayloadAddresses(payload, addressIds);
    const custFirst = latinUpper(overrides.customerFirstName ?? DEFAULT_CUSTOMER_FIRST);
    const custLast = latinUpper(overrides.customerLastName ?? DEFAULT_CUSTOMER_LAST);
    payload.privateCustomerDetails.firstName = custFirst;
    payload.privateCustomerDetails.firstNameTranslated = custFirst;
    payload.privateCustomerDetails.middleName = DEFAULT_MIDDLE_NAME;
    payload.privateCustomerDetails.middleNameTranslated = DEFAULT_MIDDLE_NAME;
    payload.privateCustomerDetails.lastName = custLast;
    payload.privateCustomerDetails.lastNameTranslated = custLast;
    payload.communicationData = [
        buildCommunicationEntry(addressIds, envVariables.contact_purpose, 'CONTRACT COMM', 'contract@test.com'),
        buildCommunicationEntry(addressIds, envVariables.billing_purpose, 'BILLING COMM', 'billing@test.com'),
    ];
    payload.managers = [
        {
            id: null,
            titleId: envVariables.title,
            name: latinUpper(overrides.managerFirstName ?? MGR_GEORGI),
            middleName: null,
            surname: latinUpper(overrides.managerLastName ?? MGR_NIKOLOV),
            personalNumber: null,
            jobPosition: MANAGER_JOB_POSITION,
            positionHeldFrom: null,
            positionHeldTo: null,
            birthDate: null,
            representationMethodId: envVariables.method_of_representation,
            additionalInformation: null,
            status: 'ACTIVE',
            phones: overrides.managerPhones ?? ['+359888111111'],
            emails: overrides.managerEmails ?? ['mgr1@test.com'],
        },
    ];
    await hydrateCustomerPayloadAddressTrsl(Request, payload);
    const response = await Request.post(Endpoints.customer, { data: payload });
    await checkResponse(response);
    const body = await response.json();
    Responses.customer.push(body);
    return body;
}

async function createSignedProductContract(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    customerIndex: number,
    opts: ContractCreateOpts = {},
): Promise<ContractChainContext> {
    const customer = Responses.customer[customerIndex];
    const customerId = customer.id;
    const customerDetails = await getCustomerDetails(Request, customerId);
    const { contractCommId, billingCommId } = await resolveCommDataIdsByPurpose(Request, customerId);

    const productIndex = opts.productIndex ?? 0;
    const payload = await GeneratePayload.contractsAndOrders.product_contract(
        customerIndex,
        productIndex,
        opts.podIndex ?? 0,
    );
    payload.productParameters.contractType = 'SUPPLY_ONLY';
    alignProductContractWithTerm(payload);
    const signingDate = isoDate(-10);
    const defaultEntryInForceDate = isoDate(7);
    payload.basicParameters.status = opts.status ?? 'SIGNED';
    payload.basicParameters.subStatus = opts.subStatus ?? 'SIGNED_BY_BOTH_SIDES';
    payload.basicParameters.signingDate = signingDate;
    if (opts.entryInForceDate === null) {
        payload.basicParameters.entryInForceDate = null;
    } else {
        payload.basicParameters.entryInForceDate = opts.entryInForceDate ?? defaultEntryInForceDate;
    }
    const entryInForceDate = payload.basicParameters.entryInForceDate ?? defaultEntryInForceDate;
    payload.basicParameters.startOfInitialTerm = entryInForceDate ?? signingDate;
    payload.basicParameters.communicationDataContractId = contractCommId;
    payload.basicParameters.communicationDataBillingId = billingCommId;
    payload.basicParameters.customerVersionId = customerDetails.versionId ?? 1;

    const podIndex = opts.podIndex ?? 0;
    const podBody = Responses.pod[podIndex];
    const podGet = await Request.get(`pod/${podBody.id}?version=1`);
    await checkResponse(podGet);
    const podJson = await podGet.json();

    const podMonthly = Number(podJson.estimatedMonthlyAvgConsumption ?? podBody.estimatedMonthlyAvgConsumption ?? 0);
    const alignedEstimated = yearlyConsumptionFromPodMonthly(podMonthly);
    payload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = alignedEstimated;
    payload.additionalParameters.riskAssessment = 'PERMIT';
    payload.additionalParameters.employeeId = 154;

    if (opts.directDebit !== undefined) {
        payload.additionalParameters.bankingDetails.directDebit = opts.directDebit;
        if (opts.bankId) payload.additionalParameters.bankingDetails.bankId = opts.bankId;
        if (opts.iban) payload.additionalParameters.bankingDetails.iban = opts.iban;
        if (opts.bic) payload.additionalParameters.bankingDetails.bic = opts.bic;
    }

    if (opts.proxy?.length) {
        payload.basicParameters.proxy = opts.proxy;
    }

    const response = await Request.post(Endpoints.productContract, { data: payload });
    await checkResponse(response);
    const contractBody = await response.json();
    Responses.productContract.push(contractBody);

    const productId = Responses.product[productIndex].id ?? Responses.product[productIndex];
    const productGet = await Request.get(`products/${productId}?`);
    await checkResponse(productGet);
    const productJson = await productGet.json();

    const managerId = await getManagerId(Request, customerId);

    const versionId = contractBody.versionId ?? 1;
    const contractDetail = await getInternalProductContract(Request, contractBody.id, versionId);
    const bp = contractDetail.basicParameters ?? {};
    const pp = contractDetail.productParameters ?? {};
    const resolvedSigningDate = String(bp.signingDate ?? signingDate).slice(0, 10);
    const contractName = String(bp.contractNumber ?? `CONTRACT-${contractBody.id}`);

    return {
        contractId: contractBody.id,
        contractVersionId: versionId,
        contractName,
        customerUic: String(customer.identifier ?? customer.customerIdentifier),
        customerId,
        productId,
        productVersionId: productJson.version ?? 1,
        productContractTermId: pp.productContractTermId ?? productJson.productContractTerms[0].id,
        invoicePaymentTermId: pp.invoicePaymentTermId ?? Responses.terms[0].invoicePaymentTerms[0].id,
        commContractId: contractCommId,
        commBillingId: billingCommId,
        podIdentifier: String(podJson.identifier ?? podJson.podIdentifier ?? podBody.identifier),
        estimatedTotalConsumption: alignedEstimated,
        signingDate: resolvedSigningDate,
        startOfInitialTermDate: String(bp.startOfInitialTerm ?? addDaysIso(resolvedSigningDate, 9)).slice(0, 10),
        supplyActivationAfterContractResigningDate: addDaysIso(resolvedSigningDate, 37),
        managerId: managerId ?? undefined,
    };
}

async function buildStandardChain(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    opts: ContractCreateOpts & {
        customerKind?: 'private' | 'business';
        customerOverrides?: Record<string, unknown>;
    } = {},
): Promise<ContractChainContext> {
    await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
    await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
    await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
    const yearlyConsumption =
        opts.estimatedTotalConsumption != null && isExactYearlyConsumptionKwh(opts.estimatedTotalConsumption)
            ? opts.estimatedTotalConsumption
            : DEFAULT_YEARLY_CONSUMPTION_KWH;
    await sharedPod(Request, GeneratePayload, Responses, Endpoints, yearlyConsumption);

    if (opts.customerKind === 'business') {
        await createPrivateBusinessCustomer(
            Request,
            GeneratePayload,
            Responses,
            Endpoints,
            opts.customerOverrides as any,
        );
    } else {
        await createPrivateCustomer(
            Request,
            GeneratePayload,
            Responses,
            Endpoints,
            opts.customerOverrides as any,
        );
    }

    return createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, opts);
}

async function buildStandardChainWithNonFixedIapProduct(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    opts: ContractCreateOpts = {},
): Promise<ContractChainContext> {
    await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
    await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
    await sharedProductWithNonFixedIap(Request, GeneratePayload, Responses, Endpoints);
    await sharedPod(Request, GeneratePayload, Responses, Endpoints);
    await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints);
    return createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, opts);
}

function buildContractPutBody(ctx: ContractChainContext, overrides: ContractPutOverrides = {}) {
    const normalizedOverrides = applyContractPutUppercaseOverrides(overrides);
    return {
        name: ctx.contractName,
        contractId: ctx.contractId,
        contractVersionId: ctx.contractVersionId,
        contractStatus: 'SIGNED',
        contractSubStatus: 'SIGNED_BY_BOTH_SIDES',
        signingDate: ctx.signingDate,
        estimatedTotalConsumption: ctx.estimatedTotalConsumption,
        productId: ctx.productId,
        productVersionId: ctx.productVersionId,
        contractType: 'SUPPLY_ONLY',
        contractTermId: ctx.productContractTermId,
        invoicePaymentTermId: ctx.invoicePaymentTermId,
        paymentGuarantee: 'NO',
        cashDepositAmount: 0.01,
        cashDepositCurrencyId: CURRENCY_ID,
        bankGuaranteeAmount: 0.01,
        bankGuaranteeCurrencyId: CURRENCY_ID,
        entryIntoForce: 'SIGNING',
        startOfInitialTerm: 'SIGNING',
        startOfInitialTermDate: ctx.startOfInitialTermDate,
        supplyActivationAfterContractResigning: 'EXACT_DATE',
        supplyActivationAfterContractResigningDate: ctx.supplyActivationAfterContractResigningDate,
        waitForOldContractTermToExpire: 'NO',
        customerUic: ctx.customerUic,
        communicationDataContractId: ctx.commContractId,
        communicationDataBillingId: ctx.commBillingId,
        podIdentifiers: [ctx.podIdentifier],
        ...normalizedOverrides,
    };
}

/** Uppercase Latin on contract PUT name-like fields (happy-path proxy, etc.). */
function applyContractPutUppercaseOverrides(overrides: ContractPutOverrides): ContractPutOverrides {
    const out = { ...overrides };
    const proxy = out.proxy;
    if (proxy && typeof proxy === 'object' && !Array.isArray(proxy)) {
        const p = proxy as Record<string, unknown>;
        if (typeof p.proxyName === 'string') {
            out.proxy = { ...p, proxyName: latinUpper(p.proxyName) };
        }
    }
    return out;
}

async function putSalesPortalContract(
    SPRequest: any,
    ctx: ContractChainContext,
    contractOverrides: ContractPutOverrides = {},
    customerOverrides: CustomerPutOverrides = {},
    Request?: any,
    logLabel = 'SP PUT',
    customerPutOpts: CustomerPutSectionOpts = {},
) {
    const customerDetails = await getCustomerDetails(Request, ctx.customerId);
    const body = {
        contract: buildContractPutBody(ctx, contractOverrides),
        customer: await buildCustomerPutSectionAuto(
            Request,
            customerDetails,
            customerOverrides,
            customerPutOpts,
        ),
    };
    logSpPutContractPayload(logLabel, body);
    const response = await SPRequest.put(SP_PRODUCT_CONTRACT, { data: body });
    await checkResponse(response);
    return response;
}

async function expectBodyContains(response: { text: () => Promise<string> }, fragment: string) {
    const text = await response.text();
    expect(text.toLowerCase()).toContain(fragment.toLowerCase());
}

/** Bean-validation field paths when SP PUT sends managerId only (TC-BE-16). */
const TC_BE_16_MISSING_MANAGER_PARAM_FRAGMENTS = [
    'firstname',
    'lastname',
    'jobposition',
    'representationmethodid',
] as const;

async function expectBodyContainsAll(
    response: { text: () => Promise<string> },
    fragments: readonly string[],
) {
    const text = (await response.text()).toLowerCase();
    for (const fragment of fragments) {
        expect(text).toContain(fragment.toLowerCase());
    }
}

async function buildSpPutBody(
    Request: any,
    ctx: ContractChainContext,
    contractOverrides: ContractPutOverrides = {},
    customerOverrides: CustomerPutOverrides = {},
    customerPutOpts: CustomerPutSectionOpts = {},
) {
    const customerDetails = await getCustomerDetails(Request, ctx.customerId);
    return {
        contract: buildContractPutBody(ctx, contractOverrides),
        customer: await buildCustomerPutSectionAuto(
            Request,
            customerDetails,
            customerOverrides,
            customerPutOpts,
        ),
    };
}

async function putSalesPortalContractExpect(
    SPRequest: any,
    ctx: ContractChainContext,
    contractOverrides: ContractPutOverrides,
    customerOverrides: CustomerPutOverrides,
    Request: any,
    expectedStatus: number,
    errorFragment?: string,
    logLabel = 'SP PUT expect reject',
    customerPutOpts: CustomerPutSectionOpts = {},
) {
    const body = await buildSpPutBody(
        Request,
        ctx,
        contractOverrides,
        customerOverrides,
        customerPutOpts,
    );
    logSpPutContractPayload(logLabel, body);
    const response = await SPRequest.put(SP_PRODUCT_CONTRACT, { data: body });
    expect(response.status()).toBe(expectedStatus);
    if (errorFragment) {
        await expectBodyContains(response, errorFragment);
    }
    return response;
}

async function getInternalContractProxyId(
    Request: any,
    contractId: number,
    versionId: number,
): Promise<number | null> {
    const detail = await getInternalProductContract(Request, contractId, versionId);
    const proxies = detail.basicParameters?.proxy ?? [];
    return proxies.length > 0 ? Number(proxies[0].id) : null;
}

async function getInternalContractStatus(Request: any, contractId: number, versionId: number) {
    const detail = await getInternalProductContract(Request, contractId, versionId);
    const bp = detail.basicParameters ?? {};
    return {
        status: bp.status ?? bp.contractStatus,
        subStatus: bp.subStatus ?? bp.contractSubStatus,
        signingDate: String(bp.signingDate ?? '').slice(0, 10),
        entryInForceDate: String(bp.entryInForceDate ?? '').slice(0, 10),
        directDebit: detail.additionalParameters?.bankingDetails?.directDebit ?? false,
    };
}

async function getSpContractVersion(SPRequest: any, contractId: number, versionId: number) {
    const response = await SPRequest.get(`${SP_PRODUCT_CONTRACT}/${contractId}/version/${versionId}`);
    await checkResponse(response);
    return response.json();
}

async function getSpCustomer(SPRequest: any, customerUic: string) {
    const response = await SPRequest.get(`${SP_CUSTOMER}/${encodeURIComponent(customerUic)}`);
    await checkResponse(response);
    return response.json();
}

async function getInternalEstimatedConsumption(Request: any, contractId: number, versionId: number) {
    const response = await Request.get(`product-contract/${contractId}?versionId=${versionId}`);
    await checkResponse(response);
    const body = await response.json();
    const additional = body.additionalParameters ?? {};
    return Number(
        additional.estimatedTotalConsumptionUnderContractKwh ??
            additional.estimatedTotalConsumption ??
            0,
    );
}

async function getInternalCustomerKyc(Request: any, customerId: number) {
    const details = await getCustomerDetails(Request, customerId);
    const pcd = details.privateCustomerDetails ?? {};
    return {
        kycPassed: pcd.kycPassed ?? details.kycPassed ?? false,
        kycExpirationDate: pcd.kycExpirationDate ?? details.kycExpirationDate ?? null,
    };
}

function buildProxyForCreate(
    proxyName: string,
    proxyDate: string,
    managerIds: number[] = [],
): Record<string, unknown> {
    return {
        proxyForeignEntityPerson: false,
        proxyName: latinUpper(proxyName),
        proxyCustomerIdentifier: VALID_PROXY_EGN,
        proxyEmail: randomGens.generateRandomEMail(),
        proxyPhone: `35988${randomGens.generateRandomString(false, true, 7)}`,
        proxyPowerOfAttorneyNumber: 'POA-001',
        proxyData: proxyDate,
        proxyValidTill: '2030-12-31',
        notaryPublic: 'NOTARY',
        registrationNumber: 'REG-001',
        areaOfOperation: 'BG',
        authorizedProxyForeignEntityPerson: false,
        managerIds,
    };
}

/**
 * Sales Portal PUT `contract.proxy` — full ProxyUpdate schema (Swagger) except optional
 * `proxyId` (omit for new-proxy create; pass via overrides when patching existing).
 * `managerIds` omitted by default for PRIVATE_CUSTOMER; pass via overrides for legal/business customers.
 */
function buildProxyForSalesPortalPut(
    proxyName: string,
    proxyDate: string,
    overrides: Record<string, unknown> = {},
): Record<string, unknown> {
    const proxyEmail = randomGens.generateRandomEMail();
    const proxyMobileNumber = `35988${randomGens.generateRandomString(false, true, 7)}`;
    const authProxyEmail = randomGens.generateRandomEMail();
    const authProxyMobileNumber = `35988${randomGens.generateRandomString(false, true, 7)}`;
    const authProxyDate = '2025-05-01';
    return {
        proxyName: latinUpper(proxyName),
        proxyUicOrPersonalNumber: VALID_PROXY_EGN,
        proxyEmail,
        proxyMobileNumber,
        powerOfAttorneyNumber: 'POA-001',
        proxyDate,
        notaryPublic: 'NOTARY',
        registrationNumber: 'REG-001',
        areaOfOperation: 'BG',
        proxyAuthorizedByProxyName: latinUpper('AUTH PROXY PERSON'),
        proxyAuthorizedByProxyUicOrPersonalNumber: VALID_AUTH_PROXY_EGN,
        proxyAuthorizedByProxyEmail: authProxyEmail,
        proxyAuthorizedByProxyMobileNumber: authProxyMobileNumber,
        proxyAuthorizedByProxyPowerOfAttorneyNumber: 'POA-002',
        proxyAuthorizedByProxyDate: authProxyDate,
        proxyAuthorizedByProxyNotaryPublic: 'AUTH NOTARY',
        proxyAuthorizedByProxyAreaOfOperation: 'BG',
        proxyAuthorizedByProxyRegistrationNumber: 'REG-002',
        ...overrides,
    };
}

// ─── Tests TC-BE-1 … TC-BE-20 ────────────────────────────────────────────────

test.describe(`[PHN-2130]: ${JIRA_TITLE}`, { tag: '@salesPortal' }, () => {
    test.describe.configure({ mode: 'parallel' });
/** CAN BE USED FOR GENERAL PAYLOAD */
    test(testName('TC-BE-1', 'Full happy-path contract update — estimatedTotalConsumption persisted'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: Terms → Price → Product → Customer → POD → Product contract (1.2 kWh)', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                estimatedTotalConsumption: TC_BE_1_INITIAL_YEARLY_KWH,
            });
        });

        await test.step('PUT /sales-portal/product-contract — update estimatedTotalConsumption to 2.4 kWh', async () => {
            await updatePodMonthlyForYearlyConsumption(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                TC_BE_1_UPDATED_YEARLY_KWH,
            );
            ctx.estimatedTotalConsumption = TC_BE_1_UPDATED_YEARLY_KWH;
            await putSalesPortalContract(SPRequest, ctx, {}, {}, Request, 'TC-BE-1');
        });

        await test.step('Verify estimatedTotalConsumption persisted (internal GET — field absent on SP GET)', async () => {
            const consumption = await getInternalEstimatedConsumption(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(consumption).toBe(TC_BE_1_UPDATED_YEARLY_KWH);
        });

        attachReport('TC-BE-1', Responses);
    });

    test(testName('TC-BE-2', 'Enable direct debit with bankId, IBAN, and BIC'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let bankId: number;
        let bankBic: string;

        await test.step('Precondition: chain + bank + contract without direct debit', async () => {
            const bank = await findActiveBank(Request);
            bankId = bank.id;
            bankBic = bank.bic;
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                directDebit: false,
            });
        });

        await test.step('PUT — directDebit true with bank fields', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {
                    directDebit: true,
                    bankId,
                    iban: IBAN_ENABLE,
                    bic: bankBic,
                },
                {},
                Request,
                'TC-BE-2',
            );
        });

        await test.step('GET SP contract version — direct debit and bank fields persisted', async () => {
            const body = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const payment = body.contractPaymentMethod ?? {};
            expect(payment.directDebit).toBe(true);
            expect(payment.iban).toBe(IBAN_ENABLE);
            expect(payment.bicSwift).toBe(bankBic);
        });

        attachReport('TC-BE-2', Responses);
    });

    test(testName('TC-BE-3', 'Disable direct debit — directDebit false unchecks only'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let bankId: number;
        let bankBic: string;

        await test.step('Precondition: contract with direct debit enabled', async () => {
            const bank = await findActiveBank(Request);
            bankId = bank.id;
            bankBic = bank.bic;
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                directDebit: true,
                bankId,
                iban: IBAN_ENABLE,
                bic: bankBic,
            });
        });

        await test.step('PUT — directDebit false (omit bank fields)', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                { directDebit: false },
                {},
                Request,
                'TC-BE-3',
            );
        });

        await test.step('GET SP contract version — direct debit disabled', async () => {
            const body = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const payment = body.contractPaymentMethod ?? {};
            expect(payment.directDebit).toBe(false);
        });

        await test.step('GET internal product contract — bank fields unchanged in persistence', async () => {
            const detail = await getInternalProductContract(Request, ctx!.contractId, ctx!.contractVersionId);
            const banking = detail.additionalParameters?.bankingDetails ?? {};
            expect(banking.directDebit).toBe(false);
            expect(banking.iban).toBe(IBAN_ENABLE);
            expect(banking.bankId).toBe(bankId);
            expect(banking.bic).toBe(bankBic);
        });

        attachReport('TC-BE-3', Responses);
    });

    test(testName('TC-BE-4', 'Direct debit omitted — leave-as-is'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let bankId: number;
        let bankBic: string;

        await test.step('Precondition: contract with direct debit enabled', async () => {
            const bank = await findActiveBank(Request);
            bankId = bank.id;
            bankBic = bank.bic;
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                directDebit: true,
                bankId,
                iban: IBAN_ENABLE,
                bic: bankBic,
            });
        });

        await test.step('PUT — omit directDebit field entirely', async () => {
            const contractBody = buildContractPutBody(ctx!);
            const customerDetails = await getCustomerDetails(Request, ctx!.customerId);
            const body = {
                contract: contractBody,
                customer: await buildPrivateCustomerPutSection(Request, customerDetails),
            };
            logSpPutContractPayload('TC-BE-4', body);
            const response = await SPRequest.put(SP_PRODUCT_CONTRACT, { data: body });
            await checkResponse(response);
        });

        await test.step('GET SP contract version — direct debit unchanged', async () => {
            const body = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const payment = body.contractPaymentMethod ?? {};
            expect(payment.directDebit).toBe(true);
            expect(payment.iban).toBe(IBAN_ENABLE);
            expect(payment.bicSwift).toBe(bankBic);
        });

        attachReport('TC-BE-4', Responses);
    });

    test(testName('TC-BE-5', 'Same customerUic — in-place customer name update'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: signed product contract for private customer', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT — same customerUic with updated name', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {},
                {
                    name: UPDATED_CUSTOMER_FIRST,
                    nameTransliterated: UPDATED_CUSTOMER_FIRST,
                },
                Request,
                'TC-BE-5',
            );
        });

        await test.step('Verify same customerUic on contract and updated name on customer', async () => {
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            expect(spContract.customer?.uicPn ?? spContract.customer?.uic).toBe(ctx!.customerUic);

            const spCustomer = await getSpCustomer(SPRequest, ctx!.customerUic);
            expect(spCustomer.name).toBe(UPDATED_CUSTOMER_FIRST);
        });

        attachReport('TC-BE-5', Responses);
    });

    test(testName('TC-BE-6', 'Different customerUic — customer swap'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let newCustomerUic: string;
        let newCustomerId: number;

        await test.step('Precondition: original + new customer, contract on original', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);

            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints, {
                firstName: DEFAULT_CUSTOMER_FIRST,
                lastName: DEFAULT_CUSTOMER_LAST,
            });
            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints, {
                firstName: MARIA_CUSTOMER_FIRST,
                lastName: MARIA_CUSTOMER_LAST,
            });

            newCustomerUic = String(
                Responses.customer[1].identifier ?? Responses.customer[1].customerIdentifier,
            );
            newCustomerId = Responses.customer[1].id;

            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0);
        });

        await test.step('PUT — swap to new customerUic with updated name', async () => {
            const newDetails = await getCustomerDetails(Request, newCustomerId);
            const { contractCommId: newContractCommId, billingCommId: newBillingCommId } =
                await resolveCommDataIdsByPurpose(Request, newCustomerId);
            const body = {
                contract: buildContractPutBody(
                    { ...ctx!, customerUic: newCustomerUic },
                    {
                        communicationDataContractId: newContractCommId,
                        communicationDataBillingId: newBillingCommId,
                    },
                ),
                customer: await buildPrivateCustomerPutSection(Request, newDetails, {
                    name: MARIA_UPDATED_FIRST,
                    nameTransliterated: MARIA_UPDATED_FIRST,
                    lastName: MARIA_CUSTOMER_LAST,
                    lastNameTransliterated: MARIA_CUSTOMER_LAST,
                }),
            };
            logSpPutContractPayload('TC-BE-6', body);
            const response = await SPRequest.put(SP_PRODUCT_CONTRACT, { data: body });
            await checkResponse(response);
        });

        await test.step('Verify customer swapped and new customer name updated', async () => {
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            expect(spContract.customer?.uicPn ?? spContract.customer?.uic).toBe(newCustomerUic);

            const spCustomer = await getSpCustomer(SPRequest, newCustomerUic);
            expect(spCustomer.name).toBe(MARIA_UPDATED_FIRST);
        });

        attachReport('TC-BE-6', Responses);
    });

    test(testName('TC-BE-7', 'Proxy and manager absent — existing proxy and manager unchanged'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: business customer with manager + contract proxy', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);

            await createPrivateBusinessCustomer(Request, GeneratePayload, Responses, Endpoints, {
                managerFirstName: MGR_GEORGI,
                managerLastName: MGR_NIKOLOV,
                managerPhones: ['+359888111111'],
                managerEmails: ['mgr1@test.com'],
            });

            const managerId = (await getManagerId(Request, Responses.customer[0].id))!;
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, {
                proxy: [buildProxyForCreate('EXISTING PROXY', SIGNING_DATE, [managerId])],
            });
        });

        await test.step('PUT — omit proxy and manager sub-objects', async () => {
            const customerDetails = await getCustomerDetails(Request, ctx!.customerId);
            const body = {
                contract: buildContractPutBody(ctx!),
                customer: await buildPrivateCustomerPutSection(Request, customerDetails),
            };
            logSpPutContractPayload('TC-BE-7', body);
            const response = await SPRequest.put(SP_PRODUCT_CONTRACT, { data: body });
            await checkResponse(response);
        });

        await test.step('Verify proxy and manager unchanged', async () => {
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const proxy = spContract.customerBusinessContactPerson?.proxy ?? {};
            expect(proxy.proxyName).toBe('EXISTING PROXY');
            expect(proxy.date).toBe(SIGNING_DATE);

            const spCustomer = await getSpCustomer(SPRequest, ctx!.customerUic);
            const manager = (spCustomer.managers ?? [])[0] ?? {};
            expect(manager.name).toBe(MGR_GEORGI);
            expect(manager.surname).toBe(MGR_NIKOLOV);

            const internalCustomer = await getCustomerDetails(Request, ctx!.customerId);
            const internalManager = (internalCustomer.managers ?? [])[0] ?? {};
            const phones: string[] = internalManager.phones ?? [];
            const emails: string[] = internalManager.emails ?? [];
            expect(phones).toContain('+359888111111');
            expect(emails).toContain('mgr1@test.com');
        });

        attachReport('TC-BE-7', Responses);
    });
    
    test(testName('TC-BE-8', 'New proxy created when proxy params sent without proxyId'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: signed contract without proxy', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints);
        });

        const proxyPayload = buildProxyForSalesPortalPut('NEW PROXY PERSON', '2025-06-01');

        await test.step('PUT — full ProxyUpdate schema params without proxyId', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                { proxy: proxyPayload },
                {},
                Request,
                'TC-BE-8',
            );
        });

        await test.step('GET SP contract version — new proxy created with full ProxyUpdate data', async () => {
            const body = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const bcp = body.customerBusinessContactPerson ?? {};
            const proxy = bcp.proxy ?? {};
            expect(proxy.proxyName).toBe(proxyPayload.proxyName);
            expect(proxy.date).toBe(proxyPayload.proxyDate);
            expect(proxy.uic).toBe(proxyPayload.proxyUicOrPersonalNumber);
            expect(proxy.email).toBe(proxyPayload.proxyEmail);
            expect(proxy.phone).toBe(proxyPayload.proxyMobileNumber);
            expect(proxy.powerOfAttorneyNumber).toBe(proxyPayload.powerOfAttorneyNumber);
            expect(proxy.notaryPublic).toBe(proxyPayload.notaryPublic);
            expect(proxy.areaOfOperation).toBe(proxyPayload.areaOfOperation);

            const authProxy = bcp.proxyAuthorizedByProxy ?? {};
            expect(authProxy.proxyName).toBe(proxyPayload.proxyAuthorizedByProxyName);
            expect(authProxy.date).toBe(proxyPayload.proxyAuthorizedByProxyDate);
            expect(authProxy.uic).toBe(proxyPayload.proxyAuthorizedByProxyUicOrPersonalNumber);
            expect(authProxy.email).toBe(proxyPayload.proxyAuthorizedByProxyEmail);
            expect(authProxy.phone).toBe(proxyPayload.proxyAuthorizedByProxyMobileNumber);
            expect(authProxy.powerOfAttorneyNumber).toBe(proxyPayload.proxyAuthorizedByProxyPowerOfAttorneyNumber);
            expect(authProxy.notaryPublic).toBe(proxyPayload.proxyAuthorizedByProxyNotaryPublic);
            expect(authProxy.areaOfOperation).toBe(proxyPayload.proxyAuthorizedByProxyAreaOfOperation);
        });

        attachReport('TC-BE-8', Responses);
    });

    test(testName('TC-BE-9', 'Manager sent without email/phone — existing contacts preserved'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let managerId: number;

        await test.step('Precondition: business customer with manager contacts', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                customerKind: 'business',
                customerOverrides: {
                    managerFirstName: MGR_IVAN,
                    managerLastName: MGR_PETROV,
                    managerPhones: ['+359888123456'],
                    managerEmails: ['manager@test.com'],
                },
            });
            managerId = ctx!.managerId!;
        });

        await test.step('PUT — manager params without email/mobileNumbers (contacts leave-as-is)', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {},
                {
                    managers: [
                        {
                            managerId,
                            firstName: MGR_GTIGOL,
                            middleName: DEFAULT_MIDDLE_NAME,
                            lastName: MGR_PETROV,
                            titleId: envVariables.title,
                            jobPosition: MANAGER_JOB_POSITION,
                            representationMethodId: envVariables.method_of_representation,
                        },
                    ],
                },
                Request,
                'TC-BE-9',
            );
        });

        await test.step('Verify manager updated and email/phone preserved (internal GET — SP GET has no contact fields)', async () => {
            const internalCustomer = await getCustomerDetails(Request, ctx!.customerId);
            const manager = (internalCustomer.managers ?? []).find((m: any) => m.id === managerId) ?? {};
            expect(manager.name ?? manager.firstName).toBe(MGR_GTIGOL);
            expect(manager.surname ?? manager.lastName).toBe(MGR_PETROV);
            expect(manager.phones ?? []).toContain('+359888123456');
            expect(manager.emails ?? []).toContain('manager@test.com');
        });

        attachReport('TC-BE-9', Responses);
    });

    test(testName('TC-BE-10', 'KYC pass true with expiration date — KYC fields updated'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: private customer (default kycPassed false)', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT — kycPassed true + kycExpirationDate on customer section', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {},
                {
                    kycPassed: true,
                    kycExpirationDate: '2027-06-01',
                },
                Request,
                'TC-BE-10',
            );
        });

        await test.step('Verify KYC fields updated (internal GET — SP GET excludes KYC per PHN-2202)', async () => {
            const kyc = await getInternalCustomerKyc(Request, ctx!.customerId);
            expect(kyc.kycPassed).toBe(true);
            expect(String(kyc.kycExpirationDate).slice(0, 10)).toBe('2027-06-01');
        });

        attachReport('TC-BE-10', Responses);
    });

    test(testName('TC-BE-11', 'KYC fields omitted — existing KYC status left unchanged'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: private customer with kycPassed true + expiration 2028-01-01', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);
            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints, {
                kycPassed: true,
                kycExpirationDate: KYC_EXP_2028,
            });
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0);
        });

        await test.step('PUT — omit kycPassed and kycExpirationDate (leave-as-is)', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {},
                {},
                Request,
                'TC-BE-11',
                { omitKycFields: true },
            );
        });

        await test.step('Verify KYC unchanged (internal GET)', async () => {
            const kyc = await getInternalCustomerKyc(Request, ctx!.customerId);
            expect(kyc.kycPassed).toBe(true);
            expect(String(kyc.kycExpirationDate).slice(0, 10)).toBe(KYC_EXP_2028);
        });

        attachReport('TC-BE-11', Responses);
    });

    test(testName('TC-BE-12', 'Missing signingDate — validation error, contract unchanged'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: signed product contract', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT — omit signingDate from contract section', async () => {
            const body = await buildSpPutBody(Request, ctx!);
            delete (body.contract as Record<string, unknown>).signingDate;
            logSpPutContractPayload('TC-BE-12', body);
            const response = await SPRequest.put(SP_PRODUCT_CONTRACT, { data: body });
            expect(response.status()).toBe(400);
            await expectBodyContains(response, 'signingDate');
        });

        await test.step('Verify signingDate unchanged', async () => {
            const status = await getInternalContractStatus(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(status.signingDate).toBe(ctx!.signingDate);
        });

        attachReport('TC-BE-12', Responses);
    });

    test(testName('TC-BE-13', 'Proxy ID not linked to contract — rejected'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: signed contract without proxy', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT — proxyId 9999999 with valid proxy params', async () => {
            await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {
                    proxy: {
                        proxyId: INVALID_PROXY_ID,
                        ...buildProxyForSalesPortalPut('TEST PROXY', '2025-06-01'),
                    },
                },
                {},
                Request,
                400,
                'Provided proxy is not added in contract',
                'TC-BE-13',
            );
        });

        await test.step('Verify no proxy on contract', async () => {
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const proxy = spContract.customerBusinessContactPerson?.proxy ?? {};
            expect(proxy.proxyName ?? null).toBeFalsy();
        });

        attachReport('TC-BE-13', Responses);
    });

    test(testName('TC-BE-14', 'Proxy ID only without params — proxy parameters missing'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let existingProxyId: number;

        await test.step('Precondition: contract with existing proxy', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);
            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints);
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, {
                proxy: [buildProxyForCreate('EXISTING PROXY', SIGNING_DATE)],
            });
            existingProxyId = (await getInternalContractProxyId(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            ))!;
            expect(existingProxyId).toBeGreaterThan(0);
        });

        await test.step('PUT — proxyId only, no other proxy fields', async () => {
            await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                { proxy: { proxyId: existingProxyId! } },
                {},
                Request,
                400,
                'Proxy parameters are missing',
                'TC-BE-14',
            );
        });

        await test.step('Verify proxy unchanged', async () => {
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const proxy = spContract.customerBusinessContactPerson?.proxy ?? {};
            expect(proxy.proxyName).toBe('EXISTING PROXY');
        });

        attachReport('TC-BE-14', Responses);
    });

    test(testName('TC-BE-15', 'Manager ID from other customer — not in the customer'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let otherManagerId: number;

        await test.step('Precondition: contract customer + unrelated business customer', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);

            await createPrivateBusinessCustomer(Request, GeneratePayload, Responses, Endpoints, {
                managerFirstName: MGR_GEORGI,
                managerLastName: MGR_NIKOLOV,
            });
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0);

            await createPrivateBusinessCustomer(Request, GeneratePayload, Responses, Endpoints, {
                customerFirstName: MARIA_CUSTOMER_FIRST,
                customerLastName: MARIA_CUSTOMER_LAST,
                managerFirstName: MGR_DIMITAR,
                managerLastName: MGR_STOEV,
            });
            otherManagerId = (await getManagerId(Request, Responses.customer[1].id))!;
        });

        await test.step('PUT — other customer managerId with manager params', async () => {
            await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {},
                {
                    managers: [
                        {
                            managerId: otherManagerId!,
                            firstName: MGR_DIMITAR,
                            lastName: MGR_STOEV,
                            titleId: envVariables.title,
                            jobPosition: MANAGER_JOB_POSITION,
                            representationMethodId: envVariables.method_of_representation,
                        },
                    ],
                },
                Request,
                400,
                'Provided ID is not in the customer',
                'TC-BE-15',
            );
        });

        attachReport('TC-BE-15', Responses);
    });

    test(testName('TC-BE-16', 'Manager ID only without params — manager parameters missing'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let managerId: number;

        await test.step('Precondition: business customer with manager + contract', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                customerKind: 'business',
                customerOverrides: {
                    managerFirstName: MGR_GEORGI,
                    managerLastName: MGR_NIKOLOV,
                },
            });
            managerId = ctx!.managerId!;
        });

        await test.step('PUT — managerId only, no other manager fields', async () => {
            const body = await buildSpPutBody(
                Request,
                ctx!,
                {},
                { managers: [{ managerId: managerId! }] },
            );
            logSpPutContractPayload('TC-BE-16', body);
            const response = await SPRequest.put(SP_PRODUCT_CONTRACT, { data: body });
            expect(response.status()).toBe(400);
            await expectBodyContainsAll(response, TC_BE_16_MISSING_MANAGER_PARAM_FRAGMENTS);
        });

        await test.step('Verify manager unchanged', async () => {
            const spCustomer = await getSpCustomer(SPRequest, ctx!.customerUic);
            const manager = (spCustomer.managers ?? [])[0] ?? {};
            expect(manager.name).toBe(MGR_GEORGI);
            expect(manager.surname).toBe(MGR_NIKOLOV);
        });

        attachReport('TC-BE-16', Responses);
    });

    test(testName('TC-BE-17', 'ACTIVE_IN_TERM without entryIntoForceDate — validation error'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        /** Future date — valid for SIGNED; invalid when transitioning to ACTIVE_IN_TERM without a past/today date. */
        const futureEntryInForceDate = isoDate(14);

        await test.step('Precondition: SIGNED contract with future entryInForceDate', async () => {
            // Do NOT use entryInForceDate: null here — with entryIntoForce SIGNING, Phoenix
            // (ProductContractDateService) auto-fills entryInForceDate from signingDate on POST,
            // so the contract would already have a past date and SP PUT to ACTIVE_IN_TERM could succeed.
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                entryInForceDate: futureEntryInForceDate,
            });
        });

        await test.step('PUT — contractStatus ACTIVE_IN_TERM, omit entryIntoForceDate', async () => {
            const response = await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {
                    contractStatus: 'ACTIVE_IN_TERM',
                    contractSubStatus: 'DELIVERY',
                },
                {},
                Request,
                400,
                undefined,
                'TC-BE-17',
            );
            const text = (await response.text()).toLowerCase();
            expect(
                text.includes('entryinforcedate') ||
                    text.includes('entry in force is mandatory') ||
                    text.includes('entry in force should be today or past'),
            ).toBe(true);
        });

        await test.step('Verify contract status unchanged', async () => {
            const status = await getInternalContractStatus(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(status.status).toBe('SIGNED');
        });

        attachReport('TC-BE-17', Responses);
    });

    test(testName('TC-BE-18', 'Non-fixed IAP product — SP PUT rejected'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let consumptionBefore: number;

        await test.step('Precondition: contract on product with dynamic-range IAP', async () => {
            ctx = await buildStandardChainWithNonFixedIapProduct(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
            );
            consumptionBefore = await getInternalEstimatedConsumption(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
        });

        await test.step('PUT — rejected for non-fixed IAP product', async () => {
            const response = await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {},
                {},
                Request,
                400,
                undefined,
                'TC-BE-18',
            );
            await expectBodyContainsAll(response, [
                'dynamic value range',
                'interim advance payment',
            ]);
        });

        await test.step('Verify contract unchanged', async () => {
            const consumptionAfter = await getInternalEstimatedConsumption(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(consumptionAfter).toBe(consumptionBefore);
        });

        attachReport('TC-BE-18', Responses);
    });

    test(testName('TC-BE-19', 'Direct debit true without bank fields — validation error'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: contract with directDebit false', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                directDebit: false,
            });
        });

        await test.step('PUT — directDebit true, omit bankId/iban/bic', async () => {
            await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                { directDebit: true },
                {},
                Request,
                400,
                'direct debit is true',
                'TC-BE-19',
            );
        });

        await test.step('Verify directDebit unchanged', async () => {
            const status = await getInternalContractStatus(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(status.directDebit).toBe(false);
        });

        attachReport('TC-BE-19', Responses);
    });

    test(testName('TC-BE-20', 'KYC passed true without expiration date — validation error'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: private customer kycPassed false', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints);
        });

        await test.step('PUT — kycPassed true, omit kycExpirationDate', async () => {
            await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {},
                { kycPassed: true },
                Request,
                400,
                'KYC expiration date is mandatory when KYC is passed',
                'TC-BE-20',
                { skipAutoKycExpiration: true },
            );
        });

        await test.step('Verify KYC unchanged', async () => {
            const kyc = await getInternalCustomerKyc(Request, ctx!.customerId);
            expect(kyc.kycPassed).toBe(false);
        });

        attachReport('TC-BE-20', Responses);
    });

    // ─── Tests TC-BE-21 … TC-BE-30 ────────────────────────────────────────────────

    test(testName('TC-BE-21', 'Business customer — second proxy rejected'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let managerId: number;

        await test.step('Precondition: business customer with one contract proxy', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);
            await createPrivateBusinessCustomer(Request, GeneratePayload, Responses, Endpoints, {
                managerFirstName: MGR_GEORGI,
                managerLastName: MGR_NIKOLOV,
            });
            managerId = (await getManagerId(Request, Responses.customer[0].id))!;
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, {
                proxy: [buildProxyForCreate(EXISTING_PROXY_NAME, SIGNING_DATE, [managerId!])],
            });
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const proxy = spContract.customerBusinessContactPerson?.proxy ?? {};
            expect(proxy.proxyName).toBe(EXISTING_PROXY_NAME);
        });

        await test.step('PUT — second proxy params without proxyId', async () => {
            const response = await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {
                    proxy: buildProxyForSalesPortalPut(SECOND_PROXY_PERSON, SECOND_PROXY_DATE, {
                        proxyUicOrPersonalNumber: SECOND_PROXY_UIC_BUSINESS,
                        managerIds: [managerId!],
                    }),
                },
                {},
                Request,
                400,
                undefined,
                'TC-BE-21',
            );
            const text = (await response.text()).toLowerCase();
            expect(
                text.includes('not possible to add more than one proxy') ||
                    text.includes('multiple proxy') ||
                    text.includes('legal entity'),
            ).toBe(true);
        });

        await test.step('Verify exactly one proxy remains', async () => {
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const proxy = spContract.customerBusinessContactPerson?.proxy ?? {};
            expect(proxy.proxyName).toBe(EXISTING_PROXY_NAME);
            const internalProxies = await getInternalContractProxies(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(internalProxies.length).toBe(1);
        });

        attachReport('TC-BE-21', Responses);
    });

    test(testName('TC-BE-22', 'Legal entity — second proxy added'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let managerId: number;
        let existingProxyId: number;

        await test.step('Precondition: legal entity with one contract proxy', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);
            await createLegalEntityCustomer(Request, GeneratePayload, Responses, Endpoints, {
                managerFirstName: MGR_GEORGI,
                managerLastName: MGR_NIKOLOV,
            });
            managerId = (await getManagerId(Request, Responses.customer[0].id))!;
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, {
                proxy: [buildProxyForCreate(FIRST_PROXY_NAME, SIGNING_DATE, [managerId!])],
            });
            existingProxyId = (await getInternalContractProxyId(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            ))!;
        });

        await test.step('PUT — add second proxy without proxyId', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {
                    proxy: buildProxyForSalesPortalPut(SECOND_PROXY_PERSON, SECOND_PROXY_DATE, {
                        proxyUicOrPersonalNumber: SECOND_PROXY_UIC_LEGAL,
                        proxyEmail: 'second.proxy@test.com',
                        proxyMobileNumber: '+359888999888',
                        managerIds: [managerId!],
                    }),
                },
                {},
                Request,
                'TC-BE-22',
            );
        });

        await test.step('Verify two proxies on contract', async () => {
            const proxies = await getInternalContractProxies(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(proxies.length).toBe(2);
            const names = proxies.map((p: any) => latinUpper(String(p.proxyName ?? '')));
            expect(names).toContain(FIRST_PROXY_NAME);
            expect(names).toContain(SECOND_PROXY_PERSON);
            const first = proxies.find((p: any) => Number(p.id) === existingProxyId);
            expect(first).toBeTruthy();
            const second = proxies.find((p: any) => latinUpper(String(p.proxyName ?? '')) === SECOND_PROXY_PERSON);
            expect(second).toBeTruthy();
            expect(Number(second.id)).not.toBe(existingProxyId);
        });

        attachReport('TC-BE-22', Responses);
    });

    test(testName('TC-BE-23', 'Different product sent — product swap on contract'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let productId2: number;
        let productVersionId2: number;
        let product2ContractTermId: number;
        let product2InvoicePaymentTermId: number;
        let originalProductId: number;

        await test.step('Precondition: two products, contract on first product', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints, 0);
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints, 1, 1);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);
            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints);
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, {
                productIndex: 0,
            });
            originalProductId = ctx!.productId;
            const product2Body = Responses.product[1];
            productId2 = Number(product2Body.id ?? product2Body);
            const product2Get = await Request.get(`products/${productId2}?`);
            await checkResponse(product2Get);
            const product2Json = await product2Get.json();
            productVersionId2 = product2Json.version ?? 1;
            product2ContractTermId = product2Json.productContractTerms[0].id;
            product2InvoicePaymentTermId = Responses.terms[1].invoicePaymentTerms[0].id;
        });

        await test.step('PUT — swap to second product', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {
                    productId: productId2,
                    productVersionId: productVersionId2,
                    contractTermId: product2ContractTermId,
                    invoicePaymentTermId: product2InvoicePaymentTermId,
                },
                {},
                Request,
                'TC-BE-23',
            );
        });

        await test.step('Verify contract references second product', async () => {
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            expect(spContract.contractProduct?.id).toBe(productId2);
            expect(spContract.contractProduct?.versionId).toBe(productVersionId2);
            expect(productId2).not.toBe(originalProductId);
        });

        attachReport('TC-BE-23', Responses);
    });

    /**FAILED */
    test(testName('TC-BE-24', 'Unfilled price component — SP PUT rejected'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let consumptionBefore: number;

        await test.step('Precondition: contract on product with unfilled price component', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricityUnfilled(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);
            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints);
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0);
            consumptionBefore = await getInternalEstimatedConsumption(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
        });

        await test.step('PUT — rejected for unfilled price component product', async () => {
            const response = await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {},
                {},
                Request,
                400,
                undefined,
                'TC-BE-24',
            );
            const text = (await response.text()).toLowerCase();
            expect(
                text.includes('price component') ||
                    text.includes('value') ||
                    text.includes('not defined') ||
                    text.includes('formula'),
            ).toBe(true);
        });

        await test.step('Verify contract unchanged', async () => {
            const consumptionAfter = await getInternalEstimatedConsumption(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(consumptionAfter).toBe(consumptionBefore);
        });

        attachReport('TC-BE-24', Responses);
    });

    test(testName('TC-BE-25', 'Third-tab SP-mappable fields update persisted'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let productDetailId: number;
        let paramId: number;
        let baselineIptId: number;
        let secondIptId: number;
        let baselineSupplyActivation: string;

        await test.step('Precondition: dual-IPT term, product with additional param, signed contract', async () => {
            await sharedTermWithDualInvoicePaymentTerms(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProductWithAdditionalParams(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);
            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints);
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0);
            productDetailId = await getProductDetailIdFromContract(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            const termBody = Responses.terms[0];
            const invoiceTerms = termBody.invoicePaymentTerms ?? [];
            expect(invoiceTerms.length).toBeGreaterThanOrEqual(2);
            baselineIptId = Number(ctx!.invoicePaymentTermId);
            secondIptId = Number(invoiceTerms[1].id);
            expect(secondIptId).not.toBe(baselineIptId);

            const thirdTab = await getThirdTabFields(Request, productDetailId);
            const params = thirdTab.productAdditionalParams ?? [];
            expect(params.length).toBeGreaterThan(0);
            paramId = Number(params[0].id);

            const internalBefore = await getInternalProductContract(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            const ppBefore = internalBefore.productParameters ?? {};
            baselineSupplyActivation = String(ppBefore.supplyActivation ?? 'FIRST_DAY_OF_MONTH');
            expect(baselineSupplyActivation).toBeTruthy();
        });

        await test.step('PUT — flat SP third-tab field overrides (not nested productParameters)', async () => {
            const toggledSupplyActivation =
                baselineSupplyActivation === 'EXACT_DATE' ? 'FIRST_DAY_OF_MONTH' : 'EXACT_DATE';
            const supplyActivationDate =
                toggledSupplyActivation === 'EXACT_DATE'
                    ? isoDate(TC_BE_25_UPDATED_START_OF_INITIAL_TERM_OFFSET_DAYS + 7)
                    : ctx!.supplyActivationAfterContractResigningDate;

            await putSalesPortalContract(
                SPRequest,
                ctx!,
                buildSpThirdTabPutOverrides(ctx!, {
                    invoicePaymentTermId: secondIptId,
                    supplyActivationAfterContractResigning: toggledSupplyActivation,
                    supplyActivationAfterContractResigningDate: supplyActivationDate,
                }),
                {},
                Request,
                'TC-BE-25',
            );
        });

        await test.step('Verify internal productParameters and SP product unchanged', async () => {
            const internalAfter = await getInternalProductContract(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            const pp = internalAfter.productParameters ?? {};
            const toggledSupplyActivation =
                baselineSupplyActivation === 'EXACT_DATE' ? 'FIRST_DAY_OF_MONTH' : 'EXACT_DATE';
            const expectedSupplyActivationValue =
                toggledSupplyActivation === 'EXACT_DATE'
                    ? isoDate(TC_BE_25_UPDATED_START_OF_INITIAL_TERM_OFFSET_DAYS + 7)
                    : null;

            assertInternalThirdTabProductParameters(pp, {
                contractType: 'SUPPLY_ONLY',
                productContractTermId: ctx!.productContractTermId,
                paymentGuarantee: 'NO',
                invoicePaymentTermId: secondIptId,
                entryIntoForce: 'SIGNING',
                startOfContractInitialTerm: 'SIGNING',
                // ProductContractDateService: SIGNING startOfInitialTerm → initialTermDate = signingDate.
                startOfContractValue: ctx!.signingDate,
                supplyActivation: toggledSupplyActivation,
                supplyActivationValue: expectedSupplyActivationValue,
                productContractWaitForOldContractTermToExpires: 'NO',
            });

            const thirdTab = await getThirdTabFields(Request, productDetailId!);
            const unchangedParam = (thirdTab.productAdditionalParams ?? []).find(
                (p: any) => Number(p.id) === paramId!,
            );
            expect(unchangedParam?.value).toBe(ADDITIONAL_PARAM_INITIAL_VALUE);

            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            expect(spContract.contractProduct?.id).toBe(ctx!.productId);
            expect(spContract.contractProduct?.versionId).toBe(ctx!.productVersionId);
        });

        attachReport('TC-BE-25', Responses);
    });

    test(testName('TC-BE-27', 'Fixed interim advance payment — update succeeds'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;

        await test.step('Precondition: contract on product with EXACT_AMOUNT IAP', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProductWithFixedIap(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints, TC_BE_1_INITIAL_YEARLY_KWH);
            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints);
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, {
                estimatedTotalConsumption: TC_BE_1_INITIAL_YEARLY_KWH,
            });
        });

        await test.step('PUT — update estimatedTotalConsumption on fixed-IAP product', async () => {
            await updatePodMonthlyForYearlyConsumption(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                TC_BE_27_UPDATED_YEARLY_KWH,
            );
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                { estimatedTotalConsumption: TC_BE_27_UPDATED_YEARLY_KWH },
                {},
                Request,
                'TC-BE-27',
            );
        });

        await test.step('Verify consumption updated', async () => {
            const consumption = await getInternalEstimatedConsumption(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(consumption).toBe(TC_BE_27_UPDATED_YEARLY_KWH);
        });

        attachReport('TC-BE-27', Responses);
    });

    /** FAILED */
    test(testName('TC-BE-28', 'Add second POD and update consumption'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let podIdentifier1: string;
        let podIdentifier2: string;

        await test.step('Precondition: two PODs, contract with first POD only', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints, DEFAULT_YEARLY_CONSUMPTION_KWH);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints, DEFAULT_YEARLY_CONSUMPTION_KWH);
            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints);
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, {
                podIndex: 0,
            });
            podIdentifier1 = ctx!.podIdentifier;
            const pod2 = Responses.pod[1];
            const pod2Get = await Request.get(`pod/${pod2.id}?version=1`);
            await checkResponse(pod2Get);
            const pod2Json = await pod2Get.json();
            podIdentifier2 = String(pod2Json.identifier ?? pod2.identifier);
        });

        await test.step('PUT — add second POD and raise consumption', async () => {
            const monthlyPerPod = podMonthlyAvgForYearlyContractKwh(TC_BE_28_YEARLY_KWH) / 2;
            for (const podIndex of [0, 1]) {
                const podId = Number(Responses.pod[podIndex].id);
                const getRes = await Request.get(`${Endpoints.pod}/${podId}?version=1`);
                await checkResponse(getRes);
                const podJson = await getRes.json();
                const editPayload = await preparePodSettlementPayload(Request, GeneratePayload);
                editPayload.identifier = String(podJson.identifier);
                editPayload.name = String(podJson.name ?? editPayload.name);
                editPayload.estimatedMonthlyAvgConsumption = String(Math.round(monthlyPerPod));
                editPayload.updateExistingVersion = true;
                editPayload.versionId = podJson.versionId ?? 1;
                const putRes = await Request.put(`${Endpoints.pod}/${podId}`, { data: editPayload });
                await checkResponse(putRes);
            }
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {
                    estimatedTotalConsumption: TC_BE_28_YEARLY_KWH,
                    podIdentifiers: [podIdentifier1!, podIdentifier2!],
                },
                {},
                Request,
                'TC-BE-28',
            );
        });

        await test.step('Verify both PODs linked and consumption updated', async () => {
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            const podNumbers = (spContract.pods ?? []).map((p: any) => String(p.podNumber));
            expect(podNumbers).toContain(podIdentifier1);
            expect(podNumbers).toContain(podIdentifier2);
            const consumption = await getInternalEstimatedConsumption(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(consumption).toBe(TC_BE_28_YEARLY_KWH);
        });

        attachReport('TC-BE-28', Responses);
    });

    /** Neeed to run again */
    test(testName('TC-BE-29', 'Update customer local address via PUT'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let addressIdsBaseline: LocalAddressIds;
        let addressIdsUpdated: LocalAddressIds;
        let commContractBefore: ReturnType<typeof snapshotSpCommunicationAddress>;
        const expectedScalars: LocalAddressScalars = { ...TC_BE_29_ADDRESS_UPDATED };

        await test.step('Precondition: chain + alternate local-address nomenclature chain', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints);
            addressIdsBaseline = await ensureValidLocalAddressIds(Request);
            addressIdsUpdated = await createAlternateLocalAddressIds(Request);
            expect(addressIdsUpdated.streetId).not.toBe(addressIdsBaseline.streetId);
            expect(addressIdsUpdated.countryId).not.toBe(addressIdsBaseline.countryId);

            const beforePut = readInternalCustomerLocalAddress(
                await getCustomerDetails(Request, ctx!.customerId),
            );
            expect(beforePut.foreign).toBe(false);
            expectLocalAddressIds(beforePut.ids, addressIdsBaseline);

            const spBefore = await getSpCustomer(SPRequest, ctx!.customerUic);
            commContractBefore = snapshotSpCommunicationAddress(spBefore.communicationDataContract ?? {});
        });

        await test.step('PUT — update every local address nomenclature id and scalar field', async () => {
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {},
                {
                    address: buildSalesPortalLocalAddressForPut(addressIdsUpdated!, expectedScalars),
                },
                Request,
                'TC-BE-29',
            );
        });

        await test.step('Verify full registered local address updated (internal GET)', async () => {
            const spCustomer = await getSpCustomer(SPRequest, ctx!.customerUic);
            expect(String(spCustomer.egnUic ?? spCustomer.customerNumber)).toBe(ctx!.customerUic);

            const afterPut = readInternalCustomerLocalAddress(
                await getCustomerDetails(Request, ctx!.customerId),
            );
            expect(afterPut.foreign).toBe(false);
            expectLocalAddressIds(afterPut.ids, addressIdsUpdated!);
            expectLocalAddressScalars(afterPut.scalars, expectedScalars);
            expect(afterPut.ids.streetId).not.toBe(addressIdsBaseline!.streetId);

            const commAfter = snapshotSpCommunicationAddress(spCustomer.communicationDataContract ?? {});
            expect(commAfter).toEqual(commContractBefore);
        });

        attachReport('TC-BE-29', Responses);
    });

    test(testName('TC-BE-30', 'Foreign address missing mandatory fields rejected'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let streetIdBefore: number | null;

        await test.step('Precondition: signed contract with private customer', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints);
            const details = await getCustomerDetails(Request, ctx!.customerId);
            streetIdBefore = Number(
                details.address?.localAddressData?.streetId ??
                    details.streetId?.id ??
                    details.streetId ??
                    0,
            ) || null;
        });

        await test.step('PUT — foreign address with countryId only', async () => {
            const response = await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {},
                {
                    address: {
                        isForeign: true,
                        foreignAddressData: {
                            countryId: envVariables.countries,
                        },
                        number: SP_ADDRESS_NUMBER,
                    },
                },
                Request,
                400,
                undefined,
                'TC-BE-30',
            );
            const text = (await response.text()).toLowerCase();
            expect(
                text.includes('region') ||
                    text.includes('municipality') ||
                    text.includes('populatedplace') ||
                    text.includes('populated place') ||
                    text.includes('zipcode') ||
                    text.includes('zip code'),
            ).toBe(true);
        });

        await test.step('Verify customer address unchanged', async () => {
            const spCustomer = await getSpCustomer(SPRequest, ctx!.customerUic);
            const comm = spCustomer.communicationDataContract ?? {};
            if (streetIdBefore != null && comm.streetId != null) {
                expect(Number(comm.streetId)).toBe(streetIdBefore);
            }
            const details = await getCustomerDetails(Request, ctx!.customerId);
            const streetAfter = Number(
                details.address?.localAddressData?.streetId ??
                    details.streetId?.id ??
                    details.streetId ??
                    0,
            );
            if (streetIdBefore != null) {
                expect(streetAfter).toBe(streetIdBefore);
            }
        });

        attachReport('TC-BE-30', Responses);
    });

    test(testName('TC-BE-31', 'Product swap with mismatched productVersionId rejected'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        let productId2: number;
        let product2ContractTermId: number;
        let product2InvoicePaymentTermId: number;
        let originalProductId: number;
        let originalProductVersionId: number;
        let mismatchedProductVersionId: number;

        await test.step('Precondition: two products at v1, contract on product1', async () => {
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints, 0);
            await sharedTerm(Request, GeneratePayload, Responses, Endpoints);
            await sharedPriceElectricity(Request, GeneratePayload, Responses, Endpoints);
            await sharedProduct(Request, GeneratePayload, Responses, Endpoints, 1, 1);
            await sharedPod(Request, GeneratePayload, Responses, Endpoints);
            await createPrivateCustomer(Request, GeneratePayload, Responses, Endpoints);
            ctx = await createSignedProductContract(Request, GeneratePayload, Responses, Endpoints, 0, {
                productIndex: 0,
            });
            originalProductId = ctx!.productId;
            originalProductVersionId = ctx!.productVersionId;
            expect(originalProductVersionId).toBe(1);

            productId2 = Number(Responses.product[1].id ?? Responses.product[1]);
            expect(productId2).not.toBe(originalProductId);

            const product2Get = await Request.get(`products/${productId2}?`);
            await checkResponse(product2Get);
            const product2Json = await product2Get.json();
            expect(Number(product2Json.version ?? 1)).toBe(1);
            product2ContractTermId = product2Json.productContractTerms[0].id;
            product2InvoicePaymentTermId = Responses.terms[1].invoicePaymentTerms[0].id;

            // productVersionId is per-product; v1 is valid for both products. Use v2 so
            // findByProductIdAndVersion(product2, 2) fails — no internal product PUT needed.
            mismatchedProductVersionId = Number(product2Json.version ?? 1) + 1;
            expect(mismatchedProductVersionId).toBe(2);
        });

        await test.step('PUT — productId2 with productVersionId not on product2', async () => {
            const response = await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {
                    productId: productId2,
                    productVersionId: mismatchedProductVersionId,
                    contractTermId: product2ContractTermId,
                    invoicePaymentTermId: product2InvoicePaymentTermId,
                },
                {},
                Request,
                400,
                undefined,
                'TC-BE-31',
            );
            const text = (await response.text()).toLowerCase();
            expect(
                text.includes('product version not found') ||
                    text.includes('productversionid') ||
                    text.includes('version not found'),
            ).toBe(true);
        });

        await test.step('Verify contract still references original product', async () => {
            const spContract = await getSpContractVersion(SPRequest, ctx!.contractId, ctx!.contractVersionId);
            expect(spContract.contractProduct?.id).toBe(originalProductId);
            expect(spContract.contractProduct?.versionId).toBe(originalProductVersionId);
        });

        attachReport('TC-BE-31', Responses);
    });

    test(testName('TC-BE-32', 'Contract status SIGNED to ENTERED_INTO_FORCE'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        /** SIGNED + IN_PROCESS POST requires a future entryInForceDate on dev2. */
        const futureEntryInForceDate = isoDate(14);
        /** Transition date when moving to ENTERED_INTO_FORCE (today or past). */
        const entryIntoForceDateOnPut = isoDate(-1);

        await test.step('Precondition: SIGNED contract with future entryInForceDate', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                entryInForceDate: futureEntryInForceDate,
                subStatus: 'IN_PROCESS',
            });
            const before = await getInternalContractStatus(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(before.status).toBe('SIGNED');
            expect(before.entryInForceDate).toBe(futureEntryInForceDate);
        });

        await test.step('PUT — transition to ENTERED_INTO_FORCE with entryIntoForceDate', async () => {
            // Keep entryIntoForce/startOfInitialTerm as SIGNING (product term allows only SIGNING).
            await putSalesPortalContract(
                SPRequest,
                ctx!,
                {
                    contractStatus: 'ENTERED_INTO_FORCE',
                    contractSubStatus: 'AWAITING_ACTIVATION',
                    entryIntoForceDate: entryIntoForceDateOnPut,
                },
                {},
                Request,
                'TC-BE-32',
            );
        });

        await test.step('Verify status and entryInForceDate persisted', async () => {
            const status = await getInternalContractStatus(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(status.status).toBe('ENTERED_INTO_FORCE');
            expect(status.subStatus).toBe('AWAITING_ACTIVATION');
            expect(status.entryInForceDate).toBe(entryIntoForceDateOnPut);
        });

        attachReport('TC-BE-32', Responses);
    });
    // Regression: passes after backend blocks manual EIF → AIT per Confluence 2228419
    test(testName('TC-BE-33', 'Manual EIF to ACTIVE_IN_TERM — validation error'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        let ctx: ContractChainContext;
        const entryInForceDate = isoDate(-5);

        await test.step('Precondition: contract in ENTERED_INTO_FORCE / AWAITING_ACTIVATION', async () => {
            ctx = await buildStandardChain(Request, GeneratePayload, Responses, Endpoints, {
                status: 'ENTERED_INTO_FORCE',
                subStatus: 'AWAITING_ACTIVATION',
                entryInForceDate,
            });
            const before = await getInternalContractStatus(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(before.status).toBe('ENTERED_INTO_FORCE');
            expect(before.subStatus).toBe('AWAITING_ACTIVATION');
        });

        await test.step('PUT — manual EIF to ACTIVE_IN_TERM rejected', async () => {
            const response = await putSalesPortalContractExpect(
                SPRequest,
                ctx!,
                {
                    contractStatus: 'ACTIVE_IN_TERM',
                    contractSubStatus: 'DELIVERY',
                    entryIntoForceDate: entryInForceDate,
                },
                {},
                Request,
                400,
                undefined,
                'TC-BE-33',
            );
            const text = (await response.text()).toLowerCase();
            expect(
                text.includes('active_in_term') ||
                    text.includes('status') ||
                    text.includes('manual') ||
                    text.includes('transition') ||
                    text.includes('cannot') ||
                    text.includes('not allowed'),
            ).toBe(true);
        });

        await test.step('Verify contract status unchanged', async () => {
            const status = await getInternalContractStatus(
                Request,
                ctx!.contractId,
                ctx!.contractVersionId,
            );
            expect(status.status).toBe('ENTERED_INTO_FORCE');
            expect(status.subStatus).toBe('AWAITING_ACTIVATION');
        });

        attachReport('TC-BE-33', Responses);
    });
});
