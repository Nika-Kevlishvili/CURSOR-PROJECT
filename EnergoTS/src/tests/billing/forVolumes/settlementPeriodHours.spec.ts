import { test, expect } from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import {
    ALL_MINUTE_RANGES,
    FIRST_HALF_MINUTE_RANGES,
    assertVolumeRatio,
    clearResponses,
    createSettlementPriceComponentOnly,
    prepareSettlementFifteenMinuteBilling,
    type PrepFixtures,
} from './settlementPeriodHours.fixtures';

test.describe('[REG-55]: Billing ', { tag: ['@billing', '@dev'] }, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-558]: For volumes', () => {
                test('[REG-1345]: For volumes - settlement hour TEN bills about 1/24 of ALL_HOURS', async ({
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

                    let baselineVolumes = 0;
                    let tenVolumes = 0;

                    await test.step('Precondition: ALL_HOURS baseline chain (CET, 15-min)', async () => {
                        const baseline = await prepareSettlementFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'CET',
                            displayName: 'SP_HOURS_ALL',
                        });
                        baselineVolumes = baseline.totalVolumes;
                        expect(baseline.settlementPeriods.length).toBe(4);
                        for (const sp of baseline.settlementPeriods) {
                            expect(sp.hours).toEqual(['ALL_HOURS']);
                        }
                        expect(Responses.invoice.length, 'Baseline invoices missing').toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Precondition: TEN-only companion chain (CET, 15-min)', async () => {
                        clearResponses(Responses);
                        const tenOnly = await prepareSettlementFifteenMinuteBilling(fixtures, {
                            hours: ['TEN'],
                            timeZone: 'CET',
                            displayName: 'SP_HOURS_TEN',
                        });
                        tenVolumes = tenOnly.totalVolumes;

                        expect(tenOnly.settlementPeriods.length).toBe(4);
                        for (const sp of tenOnly.settlementPeriods) {
                            expect(sp.hours).toEqual(['TEN']);
                        }
                        expect(Responses.invoice.length, 'TEN-only invoices missing').toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Assert TEN totalVolumes ≈ ALL_HOURS / 24 (±2%)', async () => {
                        assertVolumeRatio(tenVolumes, baselineVolumes, 1 / 24);
                        test.info().attach('[REG-SP-HOURS] volumes', {
                            body: JSON.stringify(
                                {
                                    baselineVolumes,
                                    tenVolumes,
                                    expectedRatio: 1 / 24,
                                    actualRatio: tenVolumes / baselineVolumes,
                                },
                                null,
                                2
                            ),
                            contentType: 'application/json',
                        });
                    });

                    test.info().attach('[REG-SP-HOURS] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json',
                    });
                });

                test('[REG-1346]: For volumes - settlement period stores EET and CET and bills', async ({
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

                    let cetVolumes = 0;
                    let eetVolumes = 0;

                    await test.step('Precondition: CET ALL_HOURS chain', async () => {
                        const cet = await prepareSettlementFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'CET',
                            displayName: 'SP_TZ_CET',
                        });
                        cetVolumes = cet.totalVolumes;
                        expect(cet.timeZone).toBe('CET');
                        expect(Responses.invoice.length).toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Precondition: EET ALL_HOURS chain', async () => {
                        clearResponses(Responses);
                        const eet = await prepareSettlementFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            timeZone: 'EET',
                            displayName: 'SP_TZ_EET',
                        });
                        eetVolumes = eet.totalVolumes;
                        expect(eet.timeZone).toBe('EET');
                        expect(Responses.invoice.length).toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Optional: FOUR_ADDITIONAL stores on EET PC (create + GET)', async () => {
                        // THREE_ADDITIONAL billing filter is a known SQL gap (falls through to ALL_HOURS) —
                        // do not assert THREE_ADDITIONAL filters to hour 3.
                        const created = await createSettlementPriceComponentOnly(fixtures, {
                            hours: ['FOUR_ADDITIONAL'],
                            timeZone: 'EET',
                            displayName: 'SP_TZ_EET_FOUR_ADD',
                        });
                        expect(created.settlement.timeZone).toBe('EET');
                        for (const sp of created.settlement.settlementPeriods) {
                            expect(sp.hours).toEqual(['FOUR_ADDITIONAL']);
                        }
                    });

                    await test.step('Attach EET vs CET summary volumes', async () => {
                        test.info().attach('[REG-SP-TZ] volumes', {
                            body: JSON.stringify({ cetVolumes, eetVolumes }, null, 2),
                            contentType: 'application/json',
                        });
                    });

                    test.info().attach('[REG-SP-TZ] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json',
                    });
                });

                test('[REG-1347]: For volumes - two minute ranges bill about half of four', async ({
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

                    let fourRangeVolumes = 0;
                    let twoRangeVolumes = 0;

                    await test.step('Precondition: four minute ranges ALL_HOURS baseline', async () => {
                        const baseline = await prepareSettlementFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            minuteRanges: ALL_MINUTE_RANGES,
                            timeZone: 'CET',
                            displayName: 'SP_Q_FOUR',
                        });
                        fourRangeVolumes = baseline.totalVolumes;
                        expect(baseline.settlementPeriods.map((s) => s.minuteRange).sort()).toEqual(
                            [...ALL_MINUTE_RANGES].sort()
                        );
                        expect(Responses.invoice.length).toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Precondition: ZERO_FIFTEEN + SIXTEEN_THIRTY only', async () => {
                        clearResponses(Responses);
                        const half = await prepareSettlementFifteenMinuteBilling(fixtures, {
                            hours: ['ALL_HOURS'],
                            minuteRanges: FIRST_HALF_MINUTE_RANGES,
                            timeZone: 'CET',
                            displayName: 'SP_Q_TWO',
                        });
                        twoRangeVolumes = half.totalVolumes;

                        const ranges = half.settlementPeriods.map((s) => s.minuteRange).sort();
                        expect(ranges).toEqual([...FIRST_HALF_MINUTE_RANGES].sort());
                        expect(half.settlementPeriods.length).toBe(2);
                        expect(Responses.invoice.length).toBeGreaterThan(0);
                        await validateInvoice.checkInvoiceTabs();
                    });

                    await test.step('Assert two-range totalVolumes ≈ four-range / 2 (±2%)', async () => {
                        assertVolumeRatio(twoRangeVolumes, fourRangeVolumes, 0.5);
                        test.info().attach('[REG-SP-QUARTERS] volumes', {
                            body: JSON.stringify(
                                {
                                    fourRangeVolumes,
                                    twoRangeVolumes,
                                    expectedRatio: 0.5,
                                    actualRatio: twoRangeVolumes / fourRangeVolumes,
                                },
                                null,
                                2
                            ),
                            contentType: 'application/json',
                        });
                    });

                    test.info().attach('[REG-SP-QUARTERS] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json',
                    });
                });

                test('[REG-1348]: For volumes - identical hours on all four settlement periods still bill', async ({
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
                    const multiplyHours = ['EIGHT', 'NINE'];

                    await test.step('Precondition: PC with EIGHT+NINE copied to all four minute ranges', async () => {
                        const chain = await prepareSettlementFifteenMinuteBilling(fixtures, {
                            hours: multiplyHours,
                            minuteRanges: ALL_MINUTE_RANGES,
                            timeZone: 'CET',
                            displayName: 'SP_MULTIPLY',
                        });

                        expect(chain.settlementPeriods.length).toBe(4);
                        for (const sp of chain.settlementPeriods) {
                            expect(sp.hours).toEqual(multiplyHours);
                        }
                        expect(Responses.invoice.length, 'Multiply-equivalent invoices missing').toBeGreaterThan(
                            0
                        );
                        await validateInvoice.checkInvoiceTabs();
                        expect(chain.totalVolumes).toBeGreaterThan(0);
                    });

                    test.info().attach('[REG-SP-MULTIPLY] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json',
                    });
                });
            });
        });
    });
