/**
 * PHN-2856 — Sales Portal GET payment details: optional `lan` (BULGARIAN | ENGLISH) and bilingual field rules.
 *
 * OpenAPI (experiment): `Cursor-Project/config/swagger/experiment/swagger-spec.json`
 *   GET `/sales-portal/payment/details/customer/{identifier}/payment/{paymentId}` — query `lan` enum.
 *   GET nomenclature by id (examples): `/countries/{id}` (`CountryResponse`), `/regions/{id}`, `/municipalities/{id}`,
 *   `/populated-places/{id}`, `/zip-codes/{id}` (`ZipCodeResponse`: `name` / `nameTransliterated`; no `zipCode` in experiment spec),
 *   `/districts/{id}`, `/residential-areas/{id}`, `/streets/{id}`. Address labels: BG prefers `name`, EN prefers `nameTransliterated` then `name`.
 * Response DTO: `SalesPortalPaymentDetailsResponse` / `SalesPortalPaymentPodResponse` (camelCase JSON).
 *
 * Runtime: set `BASE_URL` to experiments Phoenix host (parent: `http://10.236.20.81:8091`; swagger lists `10.236.20.81:8094` for Experiment — use the host your env targets). `SPRequest` uses `{BASE_URL}/sales-portal/` (see fixtures/baseFixture.ts).
 *
 * Reference spec(s): tests/cursor/PHN-2795-pod-address-sales-portal.spec.ts (manual invoice + offsetting payment + Sales Portal payment details).
 */
import { test, expect } from '../../fixtures/baseFixture';
import { envVariables } from '../../fixtures/envCashed';
import type { ExtendedAPIResponse } from '../../utils/RequestWrapper';
import reportGenerator from '../../utils/generateReport';

const JIRA_TITLE = 'Add bilingual logic in API GET Payment details by customer ID and payment ID';

function testName(tc: string, shortTitle: string): string {
    return `[PHN-2856]: ${JIRA_TITLE} — ${tc}: ${shortTitle}`;
}

type SalesPortalPaymentDetailsJson = {
    pods?: Array<{ podIdentifier?: string; podName?: string; podAddress?: string }>;
    paymentInitialAmount?: number;
    paymentCurrentAmount?: number;
    paymentCurrencyId?: number;
    paymentDate?: string;
    paymentMethodType?: string;
    paymentNumber?: string;
    collectionChannel?: string;
    purposeOfPayment?: string;
};

function customerPathSegment(customer: { identifier?: string; customerIdentifier?: string }): string {
    const raw = customer.identifier ?? customer.customerIdentifier;
    if (raw === undefined || raw === null || String(raw) === '') {
        throw new Error('Customer response missing identifier / customerIdentifier');
    }
    return String(raw);
}

function spPaymentDetailsRelativePath(customerKey: string, paymentId: string | number, query?: Record<string, string>): string {
    const base = `payment/details/customer/${encodeURIComponent(customerKey)}/payment/${paymentId}`;
    if (!query || Object.keys(query).length === 0) {
        return base;
    }
    const qs = new URLSearchParams(query).toString();
    return `${base}?${qs}`;
}

function paymentNumericId(paymentEntry: unknown): number {
    if (typeof paymentEntry === 'object' && paymentEntry !== null && 'id' in paymentEntry) {
        return (paymentEntry as { id: number }).id;
    }
    return paymentEntry as number;
}

function billingRunEntryId(entry: unknown): number {
    if (typeof entry === 'number' && Number.isFinite(entry) && entry > 0) {
        return entry;
    }
    if (entry !== null && typeof entry === 'object') {
        const e = entry as Record<string, unknown>;
        const cp = e.commonParameters as Record<string, unknown> | undefined;
        const id = Number(e.id ?? cp?.id);
        if (Number.isFinite(id) && id > 0) {
            return id;
        }
    }
    throw new Error(`Cannot resolve billing run id from: ${JSON.stringify(entry).slice(0, 240)}`);
}

async function resolveInvoiceIdsFromSortedBillingRunDocs(Request: any, Endpoints: any, billingRunId: number): Promise<number[]> {
    const sortedRes = await Request.get(`${Endpoints.billingRun}/sorted-invoices-for-doc/${billingRunId}`);
    await (expect(sortedRes) as any).CheckResponse();
    const rows = (await sortedRes.json()) as Array<{ invoiceId?: number }>;
    if (!Array.isArray(rows)) {
        throw new Error(`sorted-invoices-for-doc: expected array body, got ${typeof rows}`);
    }
    return rows.map((r) => r.invoiceId).filter((id): id is number => typeof id === 'number' && id > 0);
}

function pickInvoiceIdForBillingRun(invoiceIds: number[]): number {
    if (invoiceIds.length === 0) {
        throw new Error('No invoiceId values returned from sorted-invoices-for-doc for this billing run');
    }
    return Math.max(...invoiceIds);
}

async function storeResolvedInvoiceInResponses(Request: any, Endpoints: any, Responses: any): Promise<number> {
    const billingRunId = billingRunEntryId(Responses.billingRun[Responses.billingRun.length - 1]);
    const invoiceIds = await resolveInvoiceIdsFromSortedBillingRunDocs(Request, Endpoints, billingRunId);
    const invoiceId = pickInvoiceIdForBillingRun(invoiceIds);
    Responses.invoice.length = 0;
    Responses.invoice.push(invoiceId);
    return invoiceId;
}

async function findInvoiceLinkedLiabilityIdForCustomer(
    Request: any,
    Endpoints: any,
    Responses: any,
    invoiceId: number,
): Promise<number> {
    const prompt = Responses.customer[0].identifier;
    const findLiability = await Request.get(
        `${Endpoints.customerLiability}/list?page=0&size=100&columns=ID&direction=DESC&prompt=${prompt}&searchFields=CUSTOMER`,
    );
    await (expect(findLiability) as any).CheckResponse();
    const listOfLiability = await findLiability.json();
    const content = Array.isArray(listOfLiability.content) ? listOfLiability.content : [];

    for (const row of content) {
        const liabilityId = row.id as number;
        const liabilityGet = await Request.get(`${Endpoints.customerLiability}/${liabilityId}`);
        await (expect(liabilityGet) as any).CheckResponse();
        const response = await liabilityGet.json();
        const linkedInvoiceId = response?.invoiceResponse?.id;
        if (linkedInvoiceId === invoiceId) {
            return liabilityId;
        }
    }

    throw new Error(`No customer liability with invoiceResponse.id=${invoiceId} for customer identifier=${prompt}`);
}

/** Nomenclature IDs from global-setup `envVariables.json` — matches `LocalAddressData` (experiment swagger). */
function localCustomerAddressDataFromEnv(): Record<string, string | number> {
    return {
        countryId: envVariables.countries,
        regionId: envVariables.regions,
        municipalityId: envVariables.municipalities,
        populatedPlaceId: envVariables.population_places,
        zipCodeId: envVariables.zip_codes,
        districtId: envVariables.districts,
        residentialAreaId: envVariables.residential_areas,
        streetId: envVariables.streets,
        streetType: 'STREET',
        residentialAreaType: 'QUARTER',
    };
}

/** Comma-separated segments of a Sales Portal `podAddress`, trimmed; empty chunks omitted. */
function commaTokens(addr: string): string[] {
    return String(addr ?? '')
        .split(',')
        .map((t) => t.trim())
        .filter((t) => t.length > 0);
}

/** First comma-separated segment of a Sales Portal `podAddress` (country), trimming whitespace. */
function firstCommaToken(addr: string): string {
    const tokens = commaTokens(addr);
    return tokens.length > 0 ? tokens[0] : '';
}

type NomenclatureLabelBody = Record<string, unknown>;

/**
 * Human-readable label from a nomenclature GET body. When `preferTransliterated` is true (EN `lan=ENGLISH`),
 * prefer `nameTransliterated`, then `name`. When false (BG), prefer `name`, then `nameTransliterated`.
 * Also tries `populatedPlaceName` (PopulatedPlaceResponse), then `zipCode` / `code` / `value` when present.
 */
function pickLabel(body: NomenclatureLabelBody, preferTransliterated: boolean): string {
    const pick = (key: string): string => {
        const v = body[key];
        if (typeof v === 'string' && v.trim().length > 0) {
            return v.trim();
        }
        return '';
    };
    const zipAlts = (): string => pick('zipCode') || pick('code') || pick('value');
    const populatedPlaceAlt = (): string => pick('populatedPlaceName');

    if (preferTransliterated) {
        return pick('nameTransliterated') || pick('name') || populatedPlaceAlt() || zipAlts();
    }
    return pick('name') || pick('nameTransliterated') || populatedPlaceAlt() || zipAlts();
}

async function fetchNomenclatureById(Request: any, pathSegment: string, id: number): Promise<NomenclatureLabelBody> {
    const res = await Request.get(`${pathSegment}/${id}`);
    await (expect(res) as any).CheckResponse();
    return (await res.json()) as NomenclatureLabelBody;
}

/** When `districtId` / `residentialAreaId` / `streetId` are set, Phoenix requires matching `*Trsl` strings (transliterated label). */
async function hydratePodAddressTrslFromNomenclature(Request: any, podPayload: any): Promise<void> {
    const lad = podPayload?.addressRequest?.localAddressData;
    if (!lad || typeof lad !== 'object') return;

    const trslNeedsFill = (trsl: unknown): boolean => !isPodAddressSourceFilled(trsl);

    const setFromNomenclature = async (pathSegment: string, idRaw: unknown, trslKey: string): Promise<void> => {
        if (!isPodAddressSourceFilled(idRaw)) return;
        if (!trslNeedsFill(lad[trslKey])) return;
        const idNum = Number(idRaw);
        if (!Number.isFinite(idNum)) return;
        const body = await fetchNomenclatureById(Request, pathSegment, idNum);
        const label = pickLabel(body, true);
        lad[trslKey] = label || String(idNum);
    };

    await setFromNomenclature('districts', lad.districtId, 'districtTrsl');
    await setFromNomenclature('residential-areas', lad.residentialAreaId, 'residentialAreaTrsl');
    await setFromNomenclature('streets', lad.streetId, 'streetTrsl');
}

/**
 * Legal customer from `customer_legal()` with **local** main + communication addresses (swagger `CustomerAddressRequest`:
 * `foreign: false`, full `localAddressData` from env). Top-level `foreign` stays as required for legal automation payloads.
 */
function customerLegalPayloadWithForeignAddresses(GeneratePayload: any): Record<string, unknown> {
    const payload = GeneratePayload.customers.customer_legal() as Record<string, unknown>;
    payload.foreign = true;

    const localAddressData = localCustomerAddressDataFromEnv();

    const prevMain = payload.address as Record<string, unknown>;
    const mainNumber = prevMain.number != null ? String(prevMain.number) : '122';
    payload.address = {
        foreign: false,
        localAddressData,
        number: mainNumber,
        additionalInformation: prevMain.additionalInformation ?? null,
        block: null,
        entrance: null,
        floor: null,
        apartment: null,
        mailbox: prevMain.mailbox ?? null,
    };

    const comm = payload.communicationData as unknown[] | undefined;
    if (Array.isArray(comm)) {
        for (const entry of comm) {
            if (entry === null || typeof entry !== 'object') continue;
            const e = entry as Record<string, unknown>;
            const prevA = e.address as Record<string, unknown> | undefined;
            if (!prevA) continue;

            const num = prevA.number != null ? String(prevA.number) : '122';
            const nextAddr: Record<string, unknown> = {
                foreign: false,
                localAddressData,
                number: num,
                additionalInformation: prevA.additionalInformation ?? null,
                block: null,
                entrance: null,
                floor: null,
                apartment: null,
                mailbox: prevA.mailbox ?? null,
            };
            if (prevA.latitude !== undefined) nextAddr.latitude = prevA.latitude;
            if (prevA.longitude !== undefined) nextAddr.longitude = prevA.longitude;
            e.address = nextAddr;
        }
    }

    return payload;
}

function isPodAddressSourceFilled(value: unknown): boolean {
    if (value === null || value === undefined) return false;
    if (typeof value === 'string') return value.trim().length > 0;
    if (typeof value === 'number') return Number.isFinite(value);
    return true;
}

function copySourceToPodTrsl(source: unknown): string {
    if (source === null || source === undefined) return '';
    return String(source);
}

/** Experiments stack (swagger host `…:8094`) rejects POD create when `regionId` is set but `regionTrsl` is empty. */
function experimentsPhoenixStrictPodRegionTrsl(): boolean {
    return /\b8094\b/.test(String(process.env.BASE_URL ?? ''));
}

type PodBilingualTrslSyncOptions = {
    /**
     * When true, do not default `regionTrsl` if empty (TC-BE-10/11). On Experiments (`:8094`), POST /pod
     * usually still requires `regionTrsl` when `regionId` is set — those tests are skipped there.
     */
    skipRegionTrslDefault?: boolean;
};

/**
 * Experiments (and strict bilingual validators): when a POD `addressRequest` source field is set,
 * the matching `*Trsl` must be populated or POST `/pod` returns 400 ILLEGAL_ARGUMENTS_PROVIDED.
 */
function syncPodAddressTrslFromSource(podPayload: any, opts?: PodBilingualTrslSyncOptions): void {
    const ar = podPayload?.addressRequest;
    if (!ar || typeof ar !== 'object') return;

    const lad = ar.localAddressData;
    if (lad && typeof lad === 'object') {
        if (
            !opts?.skipRegionTrslDefault &&
            isPodAddressSourceFilled(lad.regionId) &&
            !isPodAddressSourceFilled(lad.regionTrsl)
        ) {
            /** Same default label as `jsons/payloads/create/pod/podSettlement.ts` for `envVariables.regions`. */
            lad.regionTrsl = 'TARGOVISHTE';
        }
        if (isPodAddressSourceFilled(lad.residentialAreaType)) {
            lad.residentialAreaTypeTrsl = copySourceToPodTrsl(lad.residentialAreaType);
        }
        if (isPodAddressSourceFilled(lad.streetType)) {
            lad.streetTypeTrsl = copySourceToPodTrsl(lad.streetType);
        }
    }

    /** `PodAddressRequest.foreign: true` + `foreignAddressData` (PODForeignAddressData): mirror type *Trsl from enums like local path. */
    const fad = ar.foreignAddressData;
    if (ar.foreign === true && fad && typeof fad === 'object') {
        if (isPodAddressSourceFilled(fad.residentialAreaType)) {
            fad.residentialAreaTypeTrsl = copySourceToPodTrsl(fad.residentialAreaType);
        }
        if (isPodAddressSourceFilled(fad.streetType)) {
            fad.streetTypeTrsl = copySourceToPodTrsl(fad.streetType);
        }
    }

    if (isPodAddressSourceFilled(ar.number)) {
        ar.numberTrsl = copySourceToPodTrsl(ar.number);
    }
    if (isPodAddressSourceFilled(ar.block)) {
        ar.blockTrsl = copySourceToPodTrsl(ar.block);
    }
    if (isPodAddressSourceFilled(ar.entrance)) {
        ar.entranceTrsl = copySourceToPodTrsl(ar.entrance);
    }
    if (isPodAddressSourceFilled(ar.floor)) {
        ar.floorTrsl = copySourceToPodTrsl(ar.floor);
    }
    if (isPodAddressSourceFilled(ar.apartment)) {
        ar.apartmentTrsl = copySourceToPodTrsl(ar.apartment);
    }
}

function assertPodAddressTrslParityBeforePost(podPayload: any, opts?: PodBilingualTrslSyncOptions): void {
    const ar = podPayload?.addressRequest;
    if (!ar || typeof ar !== 'object') {
        throw new Error(
            'POD payload sanity gate: missing `addressRequest` — cannot verify bilingual *Trsl fields before POST /pod.',
        );
    }
    const lad = ar.localAddressData && typeof ar.localAddressData === 'object' ? ar.localAddressData : null;

    const violations: string[] = [];
    const check = (path: string, source: unknown, trsl: unknown) => {
        if (!isPodAddressSourceFilled(source)) return;
        if (!isPodAddressSourceFilled(trsl)) {
            violations.push(
                `${path}: source=${JSON.stringify(source)} but Trsl=${JSON.stringify(trsl)} (missing or empty)`,
            );
        }
    };

    check('addressRequest.number → numberTrsl', ar.number, ar.numberTrsl);
    check('addressRequest.block → blockTrsl', ar.block, ar.blockTrsl);
    check('addressRequest.entrance → entranceTrsl', ar.entrance, ar.entranceTrsl);
    check('addressRequest.floor → floorTrsl', ar.floor, ar.floorTrsl);
    check('addressRequest.apartment → apartmentTrsl', ar.apartment, ar.apartmentTrsl);
    if (lad) {
        if (!opts?.skipRegionTrslDefault) {
            check('localAddressData.regionId → regionTrsl', lad.regionId, lad.regionTrsl);
        }
        check('localAddressData.districtId → districtTrsl', lad.districtId, lad.districtTrsl);
        check(
            'localAddressData.residentialAreaId → residentialAreaTrsl',
            lad.residentialAreaId,
            lad.residentialAreaTrsl,
        );
        check('localAddressData.streetId → streetTrsl', lad.streetId, lad.streetTrsl);
        check(
            'localAddressData.residentialAreaType → residentialAreaTypeTrsl',
            lad.residentialAreaType,
            lad.residentialAreaTypeTrsl,
        );
        check('localAddressData.streetType → streetTypeTrsl', lad.streetType, lad.streetTypeTrsl);
    }

    const fad =
        ar.foreign === true && ar.foreignAddressData && typeof ar.foreignAddressData === 'object'
            ? ar.foreignAddressData
            : null;
    if (fad) {
        check('foreignAddressData.region → regionTrsl', fad.region, fad.regionTrsl);
        check('foreignAddressData.municipality → municipalityTrsl', fad.municipality, fad.municipalityTrsl);
        check('foreignAddressData.populatedPlace → populatedPlaceTrsl', fad.populatedPlace, fad.populatedPlaceTrsl);
        check('foreignAddressData.zipCode → zipCodeTrsl', fad.zipCode, fad.zipCodeTrsl);
        if (isPodAddressSourceFilled(fad.district)) {
            check('foreignAddressData.district → districtTrsl', fad.district, fad.districtTrsl);
        }
        if (isPodAddressSourceFilled(fad.residentialArea)) {
            check(
                'foreignAddressData.residentialArea → residentialAreaTrsl',
                fad.residentialArea,
                fad.residentialAreaTrsl,
            );
        }
        if (isPodAddressSourceFilled(fad.street)) {
            check('foreignAddressData.street → streetTrsl', fad.street, fad.streetTrsl);
        }
        check(
            'foreignAddressData.residentialAreaType → residentialAreaTypeTrsl',
            fad.residentialAreaType,
            fad.residentialAreaTypeTrsl,
        );
        check('foreignAddressData.streetType → streetTypeTrsl', fad.streetType, fad.streetTypeTrsl);
        if (isPodAddressSourceFilled(fad.countryId) && !isPodAddressSourceFilled(fad.countryTrsl)) {
            violations.push(
                'foreignAddressData.countryId is set but countryTrsl is empty (bilingual / Sales Portal path)',
            );
        }
    }

    if (violations.length > 0) {
        throw new Error(
            `POD address bilingual sanity gate failed before POST /pod (would cause ILLEGAL_ARGUMENTS_PROVIDED):\n${violations.join(
                '\n',
            )}\nCall syncPodAddressTrslFromSource() after building the POD payload or set *Trsl explicitly.`,
        );
    }
}

/** Manual invoice chain → REAL invoice liability linked to POD (REG-1031 / PHN-2795 style). */
async function preconditionManualInvoiceLiability(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    customizePod?: (podPayload: any) => void,
    trslSyncOptions?: PodBilingualTrslSyncOptions,
): Promise<void> {
    await test.step('Precondition: customer', async () => {
        const customerData = customerLegalPayloadWithForeignAddresses(GeneratePayload);
        const customer = await Request.post(Endpoints.customer, { data: customerData });
        await (expect(customer) as any).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    await test.step('Precondition: term', async () => {
        const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
        await (expect(term) as any).CheckResponse();
        Responses.terms.push(await term.json());
    });

    await test.step('Precondition: POD', async () => {
        const podPayload = GeneratePayload.pointsOfDelivery.pod_settlement();
        if (customizePod) {
            customizePod(podPayload);
        }
        await hydratePodAddressTrslFromNomenclature(Request, podPayload);
        syncPodAddressTrslFromSource(podPayload, trslSyncOptions);
        assertPodAddressTrslParityBeforePost(podPayload, trslSyncOptions);
        const podSettlement = await Request.post(Endpoints.pod, { data: podPayload });
        await (expect(podSettlement) as any).CheckResponse();
        Responses.pod.push(await podSettlement.json());
    });

    await test.step('Precondition: product', async () => {
        const product = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
        await (expect(product) as any).CheckResponse();
        Responses.product.push(await product.json());
    });

    await test.step('Precondition: product contract', async () => {
        const contract = await Request.post(Endpoints.productContract, {
            data: await GeneratePayload.contractsAndOrders.product_contract(),
        });
        await (expect(contract) as any).CheckResponse();
        Responses.productContract.push(await contract.json());
    });

    await test.step('Precondition: activate POD', async () => {
        const podActivation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(),
        });
        await (expect(podActivation) as any).CheckResponse();
    });

    await test.step('Precondition: manual invoice billing run', async () => {
        const payload = await GeneratePayload.billing.manualInvoice();
        const createManualInvoice = await Request.post(Endpoints.billingRun, { data: payload });
        await (expect(createManualInvoice) as any).CheckResponse();
        Responses.billingRun.push(await createManualInvoice.json());
    });

    await test.step('Precondition: wait for invoice', async () => {
        await GeneratePayload.billing.waitForInvoiceGeneration();
    });

    await test.step('Precondition: resolve REAL invoice id (sorted-invoices-for-doc)', async () => {
        await storeResolvedInvoiceInResponses(Request, Endpoints, Responses);
    });

    await test.step('Precondition: resolve invoice liability id', async () => {
        const invoiceId = Responses.invoice[0] as number;
        const liabilityId = await findInvoiceLinkedLiabilityIdForCustomer(Request, Endpoints, Responses, invoiceId);
        Responses.customerLiability.push(liabilityId);
    });
}

async function preconditionOffsettingPayment(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    initialAmount: number,
    receivableValidations: any,
    paymentLink?: { invoiceId: number; contractBillingGroupId?: number },
) {
    await test.step('Precondition: collection channel', async () => {
        const collectionChannel = await Request.post(Endpoints.collectionChannel, {
            data: GeneratePayload.receivablesManagement.collection_channel(),
        });
        await (expect(collectionChannel) as any).CheckResponse();
        Responses.collectionChannel.push(await collectionChannel.json());
    });

    await test.step('Precondition: payment package', async () => {
        const paymentPackage = await Request.post(Endpoints.paymentPackage, {
            data: GeneratePayload.receivablesManagement.payment_package(),
        });
        await (expect(paymentPackage) as any).CheckResponse();
        Responses.paymentPackage.push(await paymentPackage.json());
    });

    await test.step('Precondition: payment with invoice offset', async () => {
        const newPayload = await GeneratePayload.receivablesManagement.payment(true, true);
        newPayload.initialAmount = initialAmount;
        if (paymentLink?.invoiceId != null) {
            newPayload.invoiceId = paymentLink.invoiceId;
            newPayload.outgoingDocumentType = 'INVOICE';
        }
        if (paymentLink?.contractBillingGroupId != null) {
            newPayload.contractBillingGroupId = paymentLink.contractBillingGroupId;
        }
        if (
            newPayload.paymentPurpose == null ||
            String(newPayload.paymentPurpose).trim().length === 0
        ) {
            newPayload.paymentPurpose = 'PHN-2856 purpose test';
        }
        const payment = await Request.post(Endpoints.payment, { data: newPayload });
        await (expect(payment) as any).CheckResponse();
        Responses.payment.push(await payment.json());
    });

    await test.step('Precondition: validate payment', async () => {
        const paymentId = paymentNumericId(Responses.payment[0]);
        await receivableValidations.paymentValidation(paymentId);
    });
}

async function fetchPaymentDetailsOk(
    SPRequest: any,
    customerKey: string,
    paymentId: number,
    query?: Record<string, string>,
): Promise<SalesPortalPaymentDetailsJson> {
    const path = spPaymentDetailsRelativePath(customerKey, paymentId, query);
    const res = await SPRequest.get(path);
    await expect(res as ExtendedAPIResponse).CheckResponse();
    return (await res.json()) as SalesPortalPaymentDetailsJson;
}

function assertBgApartmentPrefixes(address: string) {
    expect(address).toMatch(/бл\./);
    expect(address).toMatch(/вх\./);
    expect(address).toMatch(/ет\./);
    expect(address).toMatch(/ап\./);
}

function assertEnApartmentPrefixes(address: string) {
    expect(address).not.toMatch(/бл\.|вх\.|ет\.|ап\./);
    expect(address).toMatch(/Block\./);
    expect(address).toMatch(/Entrance\./);
    expect(address).toMatch(/Floor\./);
    expect(address).toMatch(/Apartment\./);
}

async function preconditionStandardPaymentScenario(
    Request: any,
    GeneratePayload: any,
    Responses: any,
    Endpoints: any,
    receivableValidations: any,
    customizePod?: (podPayload: any) => void,
    trslSyncOptions?: PodBilingualTrslSyncOptions,
): Promise<{ customerKey: string; paymentId: number }> {
    await preconditionManualInvoiceLiability(
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        (p) => {
            p.addressRequest.block = '12';
            p.addressRequest.entrance = 'A';
            p.addressRequest.floor = '3';
            p.addressRequest.apartment = '55';
            p.addressRequest.localAddressData.residentialAreaType = 'QUARTER';
            p.addressRequest.localAddressData.streetType = 'STREET';
            p.addressRequest.localAddressData.districtId = Number(envVariables.districts);
            p.addressRequest.localAddressData.residentialAreaId = Number(envVariables.residential_areas);
            p.addressRequest.localAddressData.streetId = Number(envVariables.streets);
            p.addressRequest.number = '1';
            if (customizePod) {
                customizePod(p);
            }
        },
        trslSyncOptions,
    );
    await preconditionOffsettingPayment(Request, GeneratePayload, Responses, Endpoints, 5000, receivableValidations, {
        invoiceId: Responses.invoice[0] as number,
    });
    const customerKey = customerPathSegment(Responses.customer[0]);
    const paymentId = paymentNumericId(Responses.payment[0]);
    return { customerKey, paymentId };
}

test.describe(`[PHN-2856]: ${JIRA_TITLE}`, { tag: '@cursor' }, () => {
    test.describe.configure({ timeout: 4 * 60 * 1000 });

    test(testName('TC-BE-1', '`lan` omitted → default Bulgarian formatting'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        await test.step('GET payment details without lan (default BULGARIAN)', async () => {
            const body = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId);
            expect(Array.isArray(body.pods)).toBeTruthy();
            expect(body.pods!.length).toBeGreaterThan(0);
            const pod = body.pods![0];
            expect(typeof pod.podName).toBe('string');
            expect(pod.podName!.length).toBeGreaterThan(0);
            const addr = String(pod.podAddress ?? '');
            expect(addr.length).toBeGreaterThan(10);
            assertBgApartmentPrefixes(addr);
            expect(typeof body.paymentCurrencyId).toBe('number');
            expect(typeof body.paymentMethodType).toBe('string');
            expect(body.collectionChannel).toBeTruthy();
            expect(body.purposeOfPayment).toBeTruthy();
        });

        test.info().attach('PHN-2856 TC-BE-1', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    /* optimal for internal use, REVIEW*/ 
    test(testName('TC-BE-2', 'BG vs EN field mapping snapshot'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        let bg!: SalesPortalPaymentDetailsJson;
        let en!: SalesPortalPaymentDetailsJson;
        await test.step('Capture BULGARIAN (explicit) vs ENGLISH', async () => {
            bg = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'BULGARIAN' });
            en = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'ENGLISH' });
        });

        await test.step('Compare story-shaped fields', async () => {
            const bgPod = bg.pods?.[0];
            const enPod = en.pods?.[0];
            expect(bgPod?.podName).toBe(enPod?.podName);
            expect(String(bgPod?.podAddress ?? '')).toMatch(/бл\./);
            assertEnApartmentPrefixes(String(enPod?.podAddress ?? ''));

            const enAddr = String(enPod?.podAddress ?? '');
            const bgAddr = String(bgPod?.podAddress ?? '');

            const addrIds = localCustomerAddressDataFromEnv();
            const countryId = Number(addrIds.countryId);
            const regionId = Number(addrIds.regionId);
            const municipalityId = Number(addrIds.municipalityId);
            const populatedPlaceId = Number(addrIds.populatedPlaceId);
            const zipCodeId = Number(addrIds.zipCodeId);
            const districtId = Number(addrIds.districtId);
            const residentialAreaId = Number(addrIds.residentialAreaId);
            const streetId = Number(addrIds.streetId);

            const [
                countryBody,
                regionBody,
                municipalityBody,
                populatedPlaceBody,
                zipCodeBody,
                districtBody,
                residentialAreaBody,
                streetBody,
            ] = await Promise.all([
                fetchNomenclatureById(Request, 'countries', countryId),
                fetchNomenclatureById(Request, 'regions', regionId),
                fetchNomenclatureById(Request, 'municipalities', municipalityId),
                fetchNomenclatureById(Request, 'populated-places', populatedPlaceId),
                fetchNomenclatureById(Request, 'zip-codes', zipCodeId),
                fetchNomenclatureById(Request, 'districts', districtId),
                fetchNomenclatureById(Request, 'residential-areas', residentialAreaId),
                fetchNomenclatureById(Request, 'streets', streetId),
            ]);

            const enCountryFromAddr = firstCommaToken(enAddr);
            const bgCountryFromAddr = firstCommaToken(bgAddr);
            expect(enCountryFromAddr.length).toBeGreaterThan(0);
            expect(bgCountryFromAddr.length).toBeGreaterThan(0);

            const expectedEnCountry = pickLabel(countryBody, true);
            const expectedBgCountry = pickLabel(countryBody, false);
            expect(expectedEnCountry.length).toBeGreaterThan(0);
            expect(expectedBgCountry.length).toBeGreaterThan(0);
            expect(enCountryFromAddr).toBe(expectedEnCountry);
            expect(bgCountryFromAddr).toBe(expectedBgCountry);

            const restNomenclatureAssertions: Array<{ label: string; expectedEn: string; expectedBg: string }> = [
                { label: 'region', expectedEn: pickLabel(regionBody, true), expectedBg: pickLabel(regionBody, false) },
                {
                    label: 'municipality',
                    expectedEn: pickLabel(municipalityBody, true),
                    expectedBg: pickLabel(municipalityBody, false),
                },
                {
                    label: 'populatedPlace',
                    expectedEn: pickLabel(populatedPlaceBody, true),
                    expectedBg: pickLabel(populatedPlaceBody, false),
                },
                { label: 'zipCode', expectedEn: pickLabel(zipCodeBody, true), expectedBg: pickLabel(zipCodeBody, false) },
                { label: 'district', expectedEn: pickLabel(districtBody, true), expectedBg: pickLabel(districtBody, false) },
                {
                    label: 'residentialArea',
                    expectedEn: pickLabel(residentialAreaBody, true),
                    expectedBg: pickLabel(residentialAreaBody, false),
                },
                { label: 'street', expectedEn: pickLabel(streetBody, true), expectedBg: pickLabel(streetBody, false) },
            ];

            for (const row of restNomenclatureAssertions) {
                expect(row.expectedEn.length, `TC-BE-2: empty EN nomenclature label (${row.label})`).toBeGreaterThan(0);
                expect(row.expectedBg.length, `TC-BE-2: empty BG nomenclature label (${row.label})`).toBeGreaterThan(0);
                expect(enAddr, `TC-BE-2: EN podAddress should contain ${row.label}`).toContain(row.expectedEn);
                expect(bgAddr, `TC-BE-2: BG podAddress should contain ${row.label}`).toContain(row.expectedBg);
            }

            expect(bg.paymentCurrencyId).toBe(en.paymentCurrencyId);
            expect(bg.collectionChannel).toBe(en.collectionChannel);
            expect(bg.purposeOfPayment).toBe(en.purposeOfPayment);
            if (bg.paymentMethodType !== en.paymentMethodType) {
                test.info().annotations.push({
                    type: 'paymentMethodType',
                    description: `TC-BE-2: BG paymentMethodType differs from EN as expected (${bg.paymentMethodType} vs ${en.paymentMethodType}).`,
                });
            } else {
                test.info().annotations.push({
                    type: 'paymentMethodType',
                    description:
                        'TC-BE-2 note: BG and EN `paymentMethodType` are identical (`collection_channels.type` from SQL is not localized in PaymentRepository.payment_base — product may evolve per story).',
                });
                expect(bg.paymentMethodType?.length ?? 0).toBeGreaterThan(0);
            }
        });

        test.info().attach('PHN-2856 TC-BE-2', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-3', 'BG delimiter and relative order for apartment tokens'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        await test.step('Assert ", " delimiter and marker order', async () => {
            const body = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId);
            const addr = String(body.pods?.[0]?.podAddress ?? '');
            expect(addr).not.toMatch(/,{2}/);
            expect(addr).not.toMatch(/[^\s],[^\s]/);
            const ibl = addr.indexOf('бл.');
            const ivh = addr.indexOf('вх.');
            const iet = addr.indexOf('ет.');
            const iap = addr.indexOf('ап.');
            expect(ibl).toBeGreaterThanOrEqual(0);
            expect(ivh).toBeGreaterThan(ibl);
            expect(iet).toBeGreaterThan(ivh);
            expect(iap).toBeGreaterThan(iet);
        });

        test.info().attach('PHN-2856 TC-BE-3', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-4', '`lan=BULGARIAN` → same as defaults'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        let withoutLan!: SalesPortalPaymentDetailsJson;
        let withBg!: SalesPortalPaymentDetailsJson;
        await test.step('Compare omit lan vs lan=BULGARIAN', async () => {
            withoutLan = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId);
            withBg = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'BULGARIAN' });
        });

        await test.step('Expect equivalent addressing', async () => {
            expect(withBg.pods?.[0]?.podAddress).toBe(withoutLan.pods?.[0]?.podAddress);
            expect(withBg.pods?.[0]?.podName).toBe(withoutLan.pods?.[0]?.podName);
            expect(withBg.paymentMethodType).toBe(withoutLan.paymentMethodType);
        });

        test.info().attach('PHN-2856 TC-BE-4', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-5', 'EN apartment prefixes vs BG Cyrillic'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        await test.step('GET lan=ENGLISH and assert prefixes', async () => {
            const body = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'ENGLISH' });
            assertEnApartmentPrefixes(String(body.pods?.[0]?.podAddress ?? ''));
        });

        test.info().attach('PHN-2856 TC-BE-5', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-6', '`lan=ENGLISH` core expectations'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        const bg = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'BULGARIAN' });
        const en = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'ENGLISH' });

        await test.step('English response shape vs Bulgarian reference', async () => {
            expect(en.pods?.[0]?.podName).toBe(bg.pods?.[0]?.podName);
            expect(en.paymentCurrencyId).toBe(bg.paymentCurrencyId);
            expect(en.collectionChannel).toBe(bg.collectionChannel);
            expect(en.purposeOfPayment).toBe(bg.purposeOfPayment);
            assertEnApartmentPrefixes(String(en.pods?.[0]?.podAddress ?? ''));
            expect(en.paymentMethodType?.length ?? 0).toBeGreaterThan(0);
        });

        test.info().attach('PHN-2856 TC-BE-6', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-7', '`paymentCurrencyId` stable across lan'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        const bg = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId);
        const en = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'ENGLISH' });
        expect(bg.paymentCurrencyId).toBe(en.paymentCurrencyId);

        test.info().attach('PHN-2856 TC-BE-7', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-8', '`collectionChannel` and `purposeOfPayment` stable across lan'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        const bg = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId);
        const en = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'ENGLISH' });
        expect(bg.collectionChannel).toBe(en.collectionChannel);
        expect(bg.purposeOfPayment).toBe(en.purposeOfPayment);

        test.info().attach('PHN-2856 TC-BE-8', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-9', '`podName` stable across lan'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        const bg = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId);
        const en = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'ENGLISH' });
        expect(bg.pods?.[0]?.podName).toBe(en.pods?.[0]?.podName);

        test.info().attach('PHN-2856 TC-BE-9', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    /* optimal for internal use, REVIEW*/ 
    test(testName('TC-BE-12', 'Unregistered address checked — per-parameter EN and BG mapping'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let expectedEnCountryLabel = '';
        let expectedBgCountryLabel = '';
        await test.step('Precondition: EN country label from nomenclature (countries/{id})', async () => {
            const countryBody = await fetchNomenclatureById(Request, 'countries', Number(envVariables.countries));
            const pickedCountry = pickLabel(countryBody, true);
            expect(pickedCountry.length).toBeGreaterThan(0);
            expectedEnCountryLabel = pickedCountry.toUpperCase();
            expectedBgCountryLabel = pickLabel(countryBody, false).toUpperCase();
            expect(expectedEnCountryLabel.length).toBeGreaterThan(0);
            expect(expectedBgCountryLabel.length).toBeGreaterThan(0);
        });

        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain (foreign / unregistered POD address)', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
                (p) => {
                    p.addressRequest.foreign = true;
                    p.addressRequest.foreignAddressData = {
                        countryId: Number(envVariables.countries),
                        region: 'БЪЛГАРСКИ РЕГИОН QA',
                        municipality: 'БЪЛГАРСКА ОБЩИНА QA',
                        populatedPlace: 'БЪЛГАРСКО НАСЕЛЕНО МЯСТО QA',
                        zipCode: 'FX-999',
                        district: 'БЪЛГАРСКИ РАЙОН QA',
                        residentialAreaType: 'QUARTER',
                        residentialArea: 'БЪЛГАРСКА ЖИЛИЩНА ЗОНА QA',
                        streetType: 'STREET',
                        street: 'БЪЛГАРСКА УЛИЦА QA',
                        countryTrsl: 'BULGARIAN COUNTRY QA',
                        regionTrsl: 'BULGARIAN REGION QA',
                        municipalityTrsl: 'BULGARIAN MUNICIPALITY QA',
                        populatedPlaceTrsl: 'BULGARIAN POPULATED PLACE QA',
                        zipCodeTrsl: 'FX-999',
                        districtTrsl: 'BULGARIAN DISTRICT QA',
                        residentialAreaTypeTrsl: 'QUARTER',
                        residentialAreaTrsl: 'BULGARIAN RESIDENTIAL AREA QA',
                        streetTypeTrsl: 'STREET',
                        streetTrsl: 'BULGARIAN STREET QA',
                    };
                },
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        await test.step('GET payment details — default BG vs ENGLISH address fragments', async () => {
            const bgOrDefault = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId);
            const en = await fetchPaymentDetailsOk(SPRequest, customerKey, paymentId, { lan: 'ENGLISH' });

            const bgCyrillicFragments = [
                'БЪЛГАРСКИ РЕГИОН QA',
                'БЪЛГАРСКА ОБЩИНА QA',
                'БЪЛГАРСКО НАСЕЛЕНО МЯСТО QA',
                'БЪЛГАРСКИ РАЙОН QA',
                'БЪЛГАРСКА ЖИЛИЩНА ЗОНА QA',
                'БЪЛГАРСКА УЛИЦА QA',
            ];
            const latinTrslFragments = [
                'BULGARIAN REGION QA',
                'BULGARIAN MUNICIPALITY QA',
                'BULGARIAN POPULATED PLACE QA',
                'BULGARIAN DISTRICT QA',
                'BULGARIAN RESIDENTIAL AREA QA',
                'BULGARIAN STREET QA',
            ];

            const addrBg = String(bgOrDefault.pods?.[0]?.podAddress ?? '');
            const addrEn = String(en.pods?.[0]?.podAddress ?? '');

            expect(addrBg).toContain('FX-999');
            for (const frag of bgCyrillicFragments) {
                expect(addrBg).toContain(frag);
            }
            for (const frag of latinTrslFragments) {
                expect(addrBg).not.toContain(frag);
            }

            expect(addrEn).toContain('FX-999');
            for (const frag of latinTrslFragments) {
                expect(addrEn).toContain(frag);
            }
            for (const frag of bgCyrillicFragments) {
                expect(addrEn).not.toContain(frag);
            }

            assertBgApartmentPrefixes(addrBg);
            assertEnApartmentPrefixes(addrEn);
            expect(firstCommaToken(addrBg)).toBe(expectedBgCountryLabel);
            expect(firstCommaToken(addrEn)).toBe(expectedEnCountryLabel);
        });

        test.info().attach('PHN-2856 TC-BE-12', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-14', 'Invalid lan rejected'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        await test.step('GET with lan=FR → HTTP 400', async () => {
            const path = spPaymentDetailsRelativePath(customerKey, paymentId, { lan: 'FR' });
            const res = await SPRequest.get(path);
            expect(res.status()).toBe(400);
        });

        test.info().attach('PHN-2856 TC-BE-14', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-15', '`lan=` empty-string binding'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        await test.step('GET with lan present but empty', async () => {
            const rawPath = `${spPaymentDetailsRelativePath(customerKey, paymentId)}?lan=`;
            const res = await SPRequest.get(rawPath);
            const status = res.status();
            if (status === 400) {
                expect(status).toBe(400);
                test.info().annotations.push({ type: 'TC-BE-15', description: 'Empty lan rejected with 400 (strict enum binding).' });
            } else {
                await expect(res as ExtendedAPIResponse).CheckResponse();
                const body = (await res.json()) as SalesPortalPaymentDetailsJson;
                expect(body.pods?.[0]?.podAddress).toMatch(/бл\./);
                test.info().annotations.push({
                    type: 'TC-BE-15',
                    description: 'Empty lan accepted — treated as default; see Spring/Jackson binding for `Language lan`.',
                });
            }
        });

        test.info().attach('PHN-2856 TC-BE-15', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test(testName('TC-BE-16', 'Wrong lan casing'), async ({
        Request,
        SPRequest,
        GeneratePayload,
        Responses,
        Endpoints,
        receivableValidations,
    }) => {
        let customerKey = '';
        let paymentId = 0;
        await test.step('Precondition: billing + payment chain', async () => {
            const ids = await preconditionStandardPaymentScenario(
                Request,
                GeneratePayload,
                Responses,
                Endpoints,
                receivableValidations,
            );
            customerKey = ids.customerKey;
            paymentId = ids.paymentId;
        });

        await test.step('lan=english and lan=Bulgarian (invalid casing for enum)', async () => {
            const r1 = await SPRequest.get(spPaymentDetailsRelativePath(customerKey, paymentId, { lan: 'english' }));
            const r2 = await SPRequest.get(spPaymentDetailsRelativePath(customerKey, paymentId, { lan: 'Bulgarian' }));
            const s1 = r1.status();
            const s2 = r2.status();
            /** Spring `Language` enum binding typically rejects wrong casing → 400; note if gateway normalizes. */
            expect(s1).toBe(400);
            expect(s2).toBe(400);
        });

        test.info().attach('PHN-2856 TC-BE-16', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
});
