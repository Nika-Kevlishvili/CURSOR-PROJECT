/**
 * Helpers for settlement-period hour / minute-range / timeZone billing cases.
 *
 * Known SQL gap (do NOT rely on for positive DST hour filter):
 * THREE_ADDITIONAL has no CASE branch in billing_run.get_dates_from_application_model_v2
 * and falls through to ALL_HOURS. FOUR_ADDITIONAL maps to clock hour 3 (same as FOUR).
 */
import { expect, test } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';
import { createResponsesContainer } from '../../../backend/fixtures/types/responses';

export type PrepFixtures = Pick<
    baseFixture,
    'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints' | 'Nomenclatures' | 'FileUploadRequest'
>;

export type MinuteRange =
    | 'ZERO_FIFTEEN'
    | 'SIXTEEN_THIRTY'
    | 'THIRTYONE_FORTYFIVE'
    | 'FORTYSIX_SIXTY';

export type SettlementTimeZone = 'CET' | 'EET';

export const ALL_MINUTE_RANGES: MinuteRange[] = [
    'ZERO_FIFTEEN',
    'SIXTEEN_THIRTY',
    'THIRTYONE_FORTYFIVE',
    'FORTYSIX_SIXTY',
];

export const FIRST_HALF_MINUTE_RANGES: MinuteRange[] = ['ZERO_FIFTEEN', 'SIXTEEN_THIRTY'];

/** Fixed August 2026 window — matches billingRounding settlement prep style. */
export const PERIOD_FROM = '2026-08-01';
export const PERIOD_TO = '2026-08-31';
export const PROFILE_AMOUNT_PER_INTERVAL = 1;
export const FIXED_PRICE_EXPRESSION = '10';
export const PROFILE_PERCENTAGE = 100;
export const VOLUME_RATIO_TOLERANCE = 0.02;

export type SettlementPeriodConfig = {
    minuteRanges?: MinuteRange[];
    hours: string[];
    timeZone: SettlementTimeZone;
    displayName?: string;
    expression?: string | number;
    profileId?: number;
    percentage?: number;
};

export type SettlementChainResult = {
    priceComponentId: number | string;
    invoiceId: number | string;
    totalVolumes: number;
    timeZone: SettlementTimeZone;
    settlementPeriods: Array<{ minuteRange: string; hours: string[] }>;
};

export function buildSettlementPeriods(
    hours: string[],
    minuteRanges: MinuteRange[] = ALL_MINUTE_RANGES
): Array<{ minuteRange: MinuteRange; hours: string[] }> {
    return minuteRanges.map((minuteRange) => ({
        minuteRange,
        hours: [...hours],
    }));
}

export function applySettlementPeriodConfig(payload: any, config: SettlementPeriodConfig): void {
    const spr = payload.applicationModelRequest.settlementPeriodsRequest;
    spr.timeZone = config.timeZone;
    spr.settlementPeriods = buildSettlementPeriods(
        config.hours,
        config.minuteRanges ?? ALL_MINUTE_RANGES
    );
    spr.profiles[0].percentage = config.percentage ?? PROFILE_PERCENTAGE;
    if (config.profileId !== undefined) {
        spr.profiles[0].profileId = config.profileId;
    }
    payload.formulaRequest.expression = config.expression ?? FIXED_PRICE_EXPRESSION;
    if (config.displayName) {
        payload.displayName = config.displayName;
    }
}

/**
 * billing-by-profile CreateRequest has no timeZone (Swagger) — zone comes from
 * nomenclature profile. "15 MINUTES" is CET; EET PC + CET data → missing profile values.
 */
export async function resolveSettlementProfileId(
    fixtures: Pick<PrepFixtures, 'Request' | 'Nomenclatures'>,
    timeZone: SettlementTimeZone,
    explicitProfileId?: number
): Promise<number> {
    if (explicitProfileId !== undefined) {
        return explicitProfileId;
    }
    if (timeZone === 'CET') {
        return fixtures.Nomenclatures.profiles('15 MINUTES');
    }
    const profileRes = await fixtures.Request.post('profiles', {
        data: {
            name: `SP_EET_${Date.now()}`,
            timeZone: 'EET',
            status: 'ACTIVE',
            defaultSelection: false,
        },
    });
    await expect(profileRes).CheckResponse();
    const body = await profileRes.json();
    return typeof body === 'number' ? body : body.id;
}

export function clearResponses(Responses: PrepFixtures['Responses']): void {
    const fresh = createResponsesContainer();
    for (const key of Object.keys(fresh) as Array<keyof typeof fresh>) {
        const value = (Responses as any)[key];
        if (Array.isArray(value)) {
            value.length = 0;
        } else {
            (Responses as any)[key] = (fresh as any)[key];
        }
    }
}

export function resolveInvoiceId(Responses: PrepFixtures['Responses']): string | number {
    const invoiceId = Responses.invoice?.[0];
    if (invoiceId === undefined || invoiceId === null) {
        throw new Error('No invoice in Responses — call waitForInvoiceGeneration first');
    }
    return invoiceId;
}

export async function getInvoiceTotalVolumes(
    Request: PrepFixtures['Request'],
    invoiceId: string | number
): Promise<number> {
    const summaryRes = await Request.get(`invoice/summary-data?id=${invoiceId}&page=0&size=25`);
    await expect(summaryRes).CheckResponse();
    const summaryJson = await summaryRes.json();
    const rows = summaryJson.content ?? [];
    expect(rows.length, 'invoice/summary-data returned no lines').toBeGreaterThan(0);
    const total = rows.reduce(
        (sum: number, row: { totalVolumes?: string | number | null }) =>
            sum + Number(row.totalVolumes ?? 0),
        0
    );
    return total;
}

export function assertVolumeRatio(
    actual: number,
    baseline: number,
    expectedRatio: number,
    tolerance: number = VOLUME_RATIO_TOLERANCE
): void {
    expect(baseline, 'Baseline totalVolumes must be > 0').toBeGreaterThan(0);
    const expected = baseline * expectedRatio;
    const lower = expected * (1 - tolerance);
    const upper = expected * (1 + tolerance);
    expect(
        actual,
        `totalVolumes ${actual} not within ±${tolerance * 100}% of baseline*${expectedRatio} (${expected})`
    ).toBeGreaterThanOrEqual(lower);
    expect(
        actual,
        `totalVolumes ${actual} not within ±${tolerance * 100}% of baseline*${expectedRatio} (${expected})`
    ).toBeLessThanOrEqual(upper);
}

export async function getPriceComponentSettlement(
    Request: PrepFixtures['Request'],
    priceComponentId: string | number
): Promise<{
    timeZone: string;
    settlementPeriods: Array<{ minuteRange: string; hours: string[] }>;
    raw: any;
}> {
    const res = await Request.get(`price-components/${priceComponentId}`);
    await expect(res).CheckResponse();
    const raw = await res.json();
    const vbsp = raw.applicationModelResponse?.volumesBySettlementPeriodResponse;
    expect(vbsp, 'volumesBySettlementPeriodResponse missing on GET price-components').toBeTruthy();
    return {
        timeZone: vbsp.timeZone,
        settlementPeriods: (vbsp.settlementPeriods ?? []).map((sp: any) => ({
            minuteRange: sp.minuteRange,
            hours: sp.hours ?? [],
        })),
        raw,
    };
}

/**
 * Full self-contained chain: customer → PC (settlement) → POD → term → product →
 * contract → activate → 15-min profile upload (uniform amount) → billing run → invoices.
 */
export async function prepareSettlementFifteenMinuteBilling(
    fixtures: PrepFixtures,
    config: SettlementPeriodConfig
): Promise<SettlementChainResult> {
    const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = fixtures;
    const profileId = await resolveSettlementProfileId(fixtures, config.timeZone, config.profileId);

    await test.step('generate customer', async () => {
        const customer = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_legal(),
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    await test.step('generate price component (BY_SETTLEMENT_PERIODS)', async () => {
        const payload = GeneratePayload.productAndServices.priceSettlement();
        applySettlementPeriodConfig(payload, {
            ...config,
            profileId,
        });
        const price = await Request.post(Endpoints.priceComponent, { data: payload });
        await expect(price).CheckResponse();
        Responses.priceComponent.push(await price.json());
    });

    await test.step('generate settlement POD', async () => {
        const podSettlement = await Request.post(Endpoints.pod, {
            data: GeneratePayload.pointsOfDelivery.pod_settlement(),
        });
        await expect(podSettlement).CheckResponse();
        Responses.pod.push(await podSettlement.json());
    });

    await test.step('generate term', async () => {
        const term = await Request.post(Endpoints.terms, {
            data: GeneratePayload.productAndServices.term(),
        });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
    });

    await test.step('generate product', async () => {
        const product = await Request.post(Endpoints.product, {
            data: GeneratePayload.productAndServices.product(),
        });
        await expect(product).CheckResponse();
        Responses.product.push(await product.json());
    });

    await test.step('generate contract', async () => {
        const contract = await Request.post(Endpoints.productContract, {
            data: await GeneratePayload.contractsAndOrders.product_contract(),
        });
        await expect(contract).CheckResponse();
        Responses.productContract.push(await contract.json());
    });

    await test.step('Activate POD', async () => {
        const podActivation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(0, PERIOD_FROM),
        });
        await expect(podActivation).CheckResponse();
    });

    await test.step('Data by profiles (15 MINUTES, uniform amount)', async () => {
        // Bare endDate yyyy-mm-dd parses as midnight → excel stops at 00:00 on last day
        // (2881 intervals). AM expects full month (2976) → billing_error
        // "missing value from billing data by profile". Use end through 23:45 like REG-962 default.
        const payload = await GeneratePayload.energyData.profile15minute(0, [
            {
                startDate: PERIOD_FROM,
                endDate: `${PERIOD_TO}T23:45:00`,
                amount: PROFILE_AMOUNT_PER_INTERVAL,
            },
        ]);
        payload.profileId = profileId;
        // timeZone on create payload is not in BillingByProfileCreateRequest — ignored by API.
        // dateRanges may overwrite API period fields with non-ISO; restore month bounds.
        payload.periodFrom = `${PERIOD_FROM}T00:00:00.000Z`;
        payload.periodTo = `${PERIOD_TO}T00:00:00.000Z`;
        const profiles = await Request.post('billing-by-profile', { data: payload });
        await expect(profiles).CheckResponse();
        const profileData = await profiles.json();
        Responses.dataByProfiles.push({
            id: profileData,
            periodFrom: payload.periodFrom,
            periodTo: payload.periodTo,
            periodType: payload.periodType,
        });
        await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'MIN');
    });

    await test.step('Billing run', async () => {
        const billingPayload = await GeneratePayload.billing.billingRun('CONTRACT');
        const billingRun = await Request.post(Endpoints.billingRun, { data: billingPayload });
        await expect(billingRun).CheckResponse();
        Responses.billingRun.push(await billingRun.json());
    });

    await test.step('Wait for invoice generation', async () => {
        await GeneratePayload.billing.waitForInvoiceGeneration();
    });

    const invoiceId = resolveInvoiceId(Responses);
    const totalVolumes = await getInvoiceTotalVolumes(Request, invoiceId);
    const pcId = Responses.priceComponent[0];
    const settlement = await getPriceComponentSettlement(Request, pcId);

    return {
        priceComponentId: pcId,
        invoiceId,
        totalVolumes,
        timeZone: settlement.timeZone as SettlementTimeZone,
        settlementPeriods: settlement.settlementPeriods,
    };
}

/**
 * Create-only PC for storage assertions (not wired into a product/contract).
 * Used for FOUR_ADDITIONAL storage on EET — not for THREE_ADDITIONAL billing filter
 * (known SQL fall-through to ALL_HOURS).
 */
export async function createSettlementPriceComponentOnly(
    fixtures: PrepFixtures,
    config: SettlementPeriodConfig
): Promise<{ id: number | string; settlement: Awaited<ReturnType<typeof getPriceComponentSettlement>> }> {
    const { Request, GeneratePayload, Endpoints } = fixtures;
    const profileId = await resolveSettlementProfileId(fixtures, config.timeZone, config.profileId);
    const payload = GeneratePayload.productAndServices.priceSettlement();
    applySettlementPeriodConfig(payload, {
        ...config,
        profileId,
    });
    const price = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(price).CheckResponse();
    const id = await price.json();
    const settlement = await getPriceComponentSettlement(Request, id);
    return { id, settlement };
}
