import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import {
    RRULE_DAILY,
    RRULE_WEEKLY_MONDAY,
    RRULE_MONTHLY_DAY_1,
    RRULE_INVALID_INTERVAL,
    WEEKLY_MONDAY_RATIO,
    SINGLE_DAY_RATIO,
    assertVolumeRatio,
    clearResponses,
    createRrulePriceComponentOnly,
    prepareRruleFifteenMinuteBilling,
    prepareSettlementFifteenMinuteBilling,
    type PrepFixtures,
} from './rruleFormula.fixtures';

test.describe.skip('[REG-55]: Billing ', { tag: ['@billing', '@dev'] }, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-558]: For volumes', () => {
                test('[REG-1341]: For volumes - FREQ=DAILY bills about the same as DAY_OF_MONTH ALL_DAYS', async ({
                    Request,
                    GeneratePayload,
                    Responses,
                    Endpoints,
                    Nomenclatures,
                    FileUploadRequest,
                    validateInvoice,
                }) => {
                    test.setTimeout(35 * 60 * 1000);
                    const fixtures: PrepFixtures = {
                        Request,
                        GeneratePayload,
                        Responses,
                        Endpoints,
                        Nomenclatures,
                        FileUploadRequest,
                    };

                    let dayOfMonthVolumes = 0;
                    let dailyRruleVolumes = 0;

                    await test.step('Precondition: DAY_OF_MONTH ALL_DAYS baseline (CET, 15-min)', async () => {
                        const baseline = await prepareSettlementFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'CET',
                            displayName: 'RRULE_BASE_DOM',
                        });
                        dayOfMonthVolumes = baseline.totalVolumes;
                        expect(Responses.invoice.length, 'DAY_OF_MONTH invoices missing').toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Precondition: RRULE FREQ=DAILY companion chain', async () => {
                        clearResponses(Responses);
                        const daily = await prepareRruleFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'CET',
                            displayName: 'RRULE_DAILY',
                            rruleFormula: RRULE_DAILY,
                        });
                        dailyRruleVolumes = daily.totalVolumes;

                        const pc = await Request.get(`price-components/${daily.priceComponentId}`);
                        await expect(pc).CheckResponse();
                        const body = await pc.json();
                        const vbsp = body.applicationModelResponse?.volumesBySettlementPeriodResponse;
                        expect(vbsp?.periodType).toBe('RRULE_FORMULA');
                        expect(vbsp?.formula).toBe(RRULE_DAILY);

                        expect(Responses.invoice.length, 'RRULE DAILY invoices missing').toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Assert FREQ=DAILY totalVolumes ≈ DAY_OF_MONTH (±2%)', async () => {
                        assertVolumeRatio(dailyRruleVolumes, dayOfMonthVolumes, 1);
                        test.info().attach('[REG-RRULE-DAILY] volumes', {
                            body: JSON.stringify(
                                {
                                    dayOfMonthVolumes,
                                    dailyRruleVolumes,
                                    expectedRatio: 1,
                                    actualRatio: dailyRruleVolumes / dayOfMonthVolumes,
                                    rrule: RRULE_DAILY,
                                },
                                null,
                                2
                            ),
                            contentType: 'application/json',
                        });
                    });

                    test.info().attach('[REG-RRULE-DAILY] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json',
                    });
                });

                test('[REG-1342]: For volumes - FREQ=WEEKLY Monday bills about 5/31 of FREQ=DAILY', async ({
                    Request,
                    GeneratePayload,
                    Responses,
                    Endpoints,
                    Nomenclatures,
                    FileUploadRequest,
                    validateInvoice,
                }) => {
                    test.setTimeout(35 * 60 * 1000);
                    const fixtures: PrepFixtures = {
                        Request,
                        GeneratePayload,
                        Responses,
                        Endpoints,
                        Nomenclatures,
                        FileUploadRequest,
                    };

                    let dailyVolumes = 0;
                    let mondayVolumes = 0;

                    await test.step('Precondition: RRULE FREQ=DAILY baseline', async () => {
                        const daily = await prepareRruleFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'CET',
                            displayName: 'RRULE_WK_DAILY',
                            rruleFormula: RRULE_DAILY,
                        });
                        dailyVolumes = daily.totalVolumes;
                        expect(Responses.invoice.length).toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Precondition: RRULE FREQ=WEEKLY;BYDAY=MO', async () => {
                        clearResponses(Responses);
                        const weekly = await prepareRruleFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'CET',
                            displayName: 'RRULE_WK_MO',
                            rruleFormula: RRULE_WEEKLY_MONDAY,
                        });
                        mondayVolumes = weekly.totalVolumes;

                        const pc = await Request.get(`price-components/${weekly.priceComponentId}`);
                        await expect(pc).CheckResponse();
                        const body = await pc.json();
                        const vbsp = body.applicationModelResponse?.volumesBySettlementPeriodResponse;
                        expect(vbsp?.periodType).toBe('RRULE_FORMULA');
                        expect(vbsp?.formula).toBe(RRULE_WEEKLY_MONDAY);

                        expect(Responses.invoice.length).toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Assert Monday RRULE ≈ DAILY * 5/31 (±2%)', async () => {
                        assertVolumeRatio(mondayVolumes, dailyVolumes, WEEKLY_MONDAY_RATIO);
                        test.info().attach('[REG-RRULE-WEEKLY] volumes', {
                            body: JSON.stringify(
                                {
                                    dailyVolumes,
                                    mondayVolumes,
                                    expectedRatio: WEEKLY_MONDAY_RATIO,
                                    actualRatio: mondayVolumes / dailyVolumes,
                                    rrule: RRULE_WEEKLY_MONDAY,
                                    note: 'Aug 2026 has 5 Mondays / 31 days',
                                },
                                null,
                                2
                            ),
                            contentType: 'application/json',
                        });
                    });

                    test.info().attach('[REG-RRULE-WEEKLY] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json',
                    });
                });

                test('[REG-1343]: For volumes - FREQ=MONTHLY day 1 bills about 1/31 of FREQ=DAILY', async ({
                    Request,
                    GeneratePayload,
                    Responses,
                    Endpoints,
                    Nomenclatures,
                    FileUploadRequest,
                    validateInvoice,
                }) => {
                    test.setTimeout(35 * 60 * 1000);
                    const fixtures: PrepFixtures = {
                        Request,
                        GeneratePayload,
                        Responses,
                        Endpoints,
                        Nomenclatures,
                        FileUploadRequest,
                    };

                    let dailyVolumes = 0;
                    let dayOneVolumes = 0;

                    await test.step('Precondition: RRULE FREQ=DAILY baseline', async () => {
                        const daily = await prepareRruleFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'CET',
                            displayName: 'RRULE_MO_DAILY',
                            rruleFormula: RRULE_DAILY,
                        });
                        dailyVolumes = daily.totalVolumes;
                        expect(Responses.invoice.length).toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Precondition: RRULE FREQ=MONTHLY;BYMONTHDAY=1', async () => {
                        clearResponses(Responses);
                        const monthly = await prepareRruleFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'CET',
                            displayName: 'RRULE_MO_D1',
                            rruleFormula: RRULE_MONTHLY_DAY_1,
                        });
                        dayOneVolumes = monthly.totalVolumes;

                        const pc = await Request.get(`price-components/${monthly.priceComponentId}`);
                        await expect(pc).CheckResponse();
                        const body = await pc.json();
                        const vbsp = body.applicationModelResponse?.volumesBySettlementPeriodResponse;
                        expect(vbsp?.periodType).toBe('RRULE_FORMULA');
                        expect(vbsp?.formula).toBe(RRULE_MONTHLY_DAY_1);

                        expect(Responses.invoice.length).toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Assert day-1 RRULE ≈ DAILY * 1/31 (±2%)', async () => {
                        assertVolumeRatio(dayOneVolumes, dailyVolumes, SINGLE_DAY_RATIO);
                        test.info().attach('[REG-RRULE-MONTHLY] volumes', {
                            body: JSON.stringify(
                                {
                                    dailyVolumes,
                                    dayOneVolumes,
                                    expectedRatio: SINGLE_DAY_RATIO,
                                    actualRatio: dayOneVolumes / dailyVolumes,
                                    rrule: RRULE_MONTHLY_DAY_1,
                                },
                                null,
                                2
                            ),
                            contentType: 'application/json',
                        });
                    });

                    test.info().attach('[REG-RRULE-MONTHLY] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json',
                    });
                });

                test('[REG-1344]: For volumes - RRULE INTERVAL greater than 1 is rejected', async ({
                    Request,
                    GeneratePayload,
                    Responses,
                    Endpoints,
                    Nomenclatures,
                    FileUploadRequest,
                }) => {
                    test.setTimeout(5 * 60 * 1000);
                    const fixtures: PrepFixtures = {
                        Request,
                        GeneratePayload,
                        Responses,
                        Endpoints,
                        Nomenclatures,
                        FileUploadRequest,
                    };

                    await test.step('Create PC with FREQ=DAILY;INTERVAL=2 → expect rejection', async () => {
                        const { response } = await createRrulePriceComponentOnly(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'CET',
                            displayName: 'RRULE_BAD_INTERVAL',
                            rruleFormula: RRULE_INVALID_INTERVAL,
                        });
                        expect(response.ok(), 'INTERVAL=2 must be rejected by RRuleValidator').toBeFalsy();
                        expect(response.status()).toBeGreaterThanOrEqual(400);
                        const errText = await response.text();
                        expect(errText.toLowerCase()).toMatch(/rrule|invalid|formula/);
                        test.info().attach('[REG-RRULE-REJECT] body', {
                            body: errText,
                            contentType: 'application/json',
                        });
                    });
                });
            });
        });
    });
