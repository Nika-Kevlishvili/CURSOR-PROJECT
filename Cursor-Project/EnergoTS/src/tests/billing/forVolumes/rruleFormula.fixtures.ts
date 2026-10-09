/**
 * Helpers for price-component application-model RRULE_FORMULA billing cases.
 *
 * Runtime acceptance (Java RRuleUtil): FREQ DAILY/WEEKLY/MONTHLY/YEARLY only;
 * INTERVAL must be 1 (or omitted); BYHOUR/BYMINUTE/BYSECOND rejected on create.
 *
 * Billing (Dev DB billing_run.get_dates_from_application_model_v2):
 * concatenates stored formula with settlement BYHOUR/BYMINUTE + UNTIL.
 */
import { expect, test } from '../../../backend/fixtures/baseFixture';
import type { PrepFixtures, SettlementChainResult, SettlementPeriodConfig } from './settlementPeriodHours.fixtures';
import {
    ALL_MINUTE_RANGES,
    PERIOD_FROM,
    PERIOD_TO,
    PROFILE_AMOUNT_PER_INTERVAL,
    PROFILE_PERCENTAGE,
    FIXED_PRICE_EXPRESSION,
    applySettlementPeriodConfig,
    getInvoiceTotalVolumes,
    getPriceComponentSettlement,
    resolveInvoiceId,
} from './settlementPeriodHours.fixtures';

export type { PrepFixtures } from './settlementPeriodHours.fixtures';
export {
    clearResponses,
    prepareSettlementFifteenMinuteBilling,
    assertVolumeRatio,
} from './settlementPeriodHours.fixtures';

export type RruleCaseConfig = SettlementPeriodConfig & {
    rruleFormula: string;
};

/** Aug 2026: 31 days; Mondays = 3,10,17,24,31 → 5/31. */
export const AUG_2026_DAY_COUNT = 31;
export const AUG_2026_MONDAY_COUNT = 5;
export const WEEKLY_MONDAY_RATIO = AUG_2026_MONDAY_COUNT / AUG_2026_DAY_COUNT;
export const SINGLE_DAY_RATIO = 1 / AUG_2026_DAY_COUNT;

export const RRULE_DAILY = 'FREQ=DAILY';
export const RRULE_WEEKLY_MONDAY = 'FREQ=WEEKLY;BYDAY=MO';
export const RRULE_MONTHLY_DAY_1 = 'FREQ=MONTHLY;BYMONTHDAY=1';
export const RRULE_INVALID_INTERVAL = 'FREQ=DAILY;INTERVAL=2';

export function applyRruleSettlementConfig(payload: any, config: RruleCaseConfig): void {
    applySettlementPeriodConfig(payload, config);
    const spr = payload.applicationModelRequest.settlementPeriodsRequest;
    spr.periodType = 'RRULE_FORMULA';
    spr.formula = config.rruleFormula;
    delete spr.dateOfMonths;
    delete spr.dayOfWeekAndPeriodOfYear;
    spr.yearRound = false;
}

/**
 * Same chain as prepareSettlementFifteenMinuteBilling, but PC uses RRULE_FORMULA.
 */
export async function prepareRruleFifteenMinuteBilling(
    fixtures: PrepFixtures,
    config: RruleCaseConfig
): Promise<SettlementChainResult & { rruleFormula: string }> {
    const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = fixtures;
    const profile15 = await Nomenclatures.profiles('15 MINUTES');

    await test.step('generate customer', async () => {
        const customer = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_legal(),
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    await test.step(`generate price component (RRULE_FORMULA: ${config.rruleFormula})`, async () => {
        const payload = GeneratePayload.productAndServices.priceSettlement();
        applyRruleSettlementConfig(payload, {
            ...config,
            hours: config.hours ?? ['ALL_HOURS'],
            minuteRanges: config.minuteRanges ?? ALL_MINUTE_RANGES,
            timeZone: config.timeZone ?? 'CET',
            profileId: config.profileId ?? profile15,
            percentage: config.percentage ?? PROFILE_PERCENTAGE,
            expression: config.expression ?? FIXED_PRICE_EXPRESSION,
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
        const payload = await GeneratePayload.energyData.profile15minute(0, [
            {
                startDate: PERIOD_FROM,
                endDate: PERIOD_TO,
                amount: PROFILE_AMOUNT_PER_INTERVAL,
            },
        ]);
        payload.profileId = config.profileId ?? profile15;
        payload.timeZone = config.timeZone ?? 'CET';
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
        timeZone: settlement.timeZone as 'CET' | 'EET',
        settlementPeriods: settlement.settlementPeriods,
        rruleFormula: config.rruleFormula,
    };
}

export async function createRrulePriceComponentOnly(
    fixtures: PrepFixtures,
    config: RruleCaseConfig
): Promise<{ response: Awaited<ReturnType<PrepFixtures['Request']['post']>>; id?: number | string }> {
    const { Request, GeneratePayload, Endpoints, Nomenclatures } = fixtures;
    const profile15 = await Nomenclatures.profiles('15 MINUTES');
    const payload = GeneratePayload.productAndServices.priceSettlement();
    applyRruleSettlementConfig(payload, {
        ...config,
        hours: config.hours ?? ['ALL_HOURS'],
        minuteRanges: config.minuteRanges ?? ALL_MINUTE_RANGES,
        timeZone: config.timeZone ?? 'CET',
        profileId: config.profileId ?? profile15,
    });
    const response = await Request.post(Endpoints.priceComponent, { data: payload });
    let id: number | string | undefined;
    if (response.ok()) {
        id = await response.json();
    }
    return { response, id };
}
