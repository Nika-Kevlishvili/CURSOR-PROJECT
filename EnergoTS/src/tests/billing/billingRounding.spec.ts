import { test, expect } from '../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';
import { randomGens } from '../../backend/utils/randomGens';
import {
    DEV_GLOBAL_VAT_PERCENT,
    assertInvoiceRounding,
    addDecimal,
    divHalfUp,
    expectedInvoiceMoney,
    halfUp,
    mulHalfUp,
    naiveRoundEachThenSum,
} from './billingRounding.fixtures';

type PrepFixtures = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;
type ScalesPrepFixtures = PrepFixtures & Pick<baseFixture, 'Nomenclatures'>;

type ScalesNumberOfDaysPrepOptions = {
    expression?: string;
    displayName?: string;
    invoiceDate?: string;
};

type SettlementPrepOptions = {
    expression: string;
    displayName: string;
    profileValue: number;
    invoiceDate?: string;
    periodFrom?: string;
    periodTo?: string;
};

type DualPcSettlementPrepOptions = {
    pcs: Array<{ expression: string; displayName: string }>;
    profileValue: number;
    invoiceDate?: string;
    periodFrom?: string;
    periodTo?: string;
};

/** Must sit inside the open accounting period (same date the billing-run payload uses). */
const DEFAULT_INVOICE_DATE = randomGens.generateTodaysDate('yyyy-mm-dd');
/** Matches POD activation, which is the current month start. */
const DEFAULT_PERIOD_FROM = randomGens.generateMonthStartDate('yyyy-mm-dd');
const DEFAULT_PERIOD_TO = randomGens.generateMonthEndDate('yyyy-mm-dd');
/** invoice/summary-data row label for settlement price components (see billingValidator). */
const SETTLEMENT_PC_SUMMARY_LABEL = 'BY_SETTLEMENT_PERIODS';
/** invoice/summary-data row label for scale price components (see billingValidator / componentByscales). */
const SCALES_PC_SUMMARY_LABEL = 'BY_SCALES';

const PERIOD_HALF = randomGens.generateMonthHalfDate('yyyy-mm-dd');
const PERIOD_HALF_PLUS_ONE = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd');
const RND07_TARIFF_VOLUME = '100';
const RND07_SCALE_HALF_A = '20';
const RND07_SCALE_HALF_B = '20';
/** Tariff volume plus both half-month scale-code totalVolumes. Day count follows the current month. */
const RND07_EXPECTED_BILLED_VOLUME = addDecimal(
    addDecimal(RND07_TARIFF_VOLUME, RND07_SCALE_HALF_A),
    RND07_SCALE_HALF_B
);

function resolveInvoiceId(Responses: PrepFixtures['Responses']): string | number {
    const invoiceId = Responses.invoice?.[0];
    if (invoiceId === undefined || invoiceId === null) {
        throw new Error('No invoice in Responses — call waitForInvoiceGeneration first');
    }
    return invoiceId;
}

async function postBillingRun(
    fixtures: PrepFixtures,
    invoiceDate: string = DEFAULT_INVOICE_DATE
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const payload = await GeneratePayload.billing.billingRun('CUSTOMER');
    payload.commonParameters.invoiceDate = invoiceDate;
    payload.commonParameters.taxEventDate = invoiceDate;
    const billingRun = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(billingRun).CheckResponse();
    Responses.billingRun.push(await billingRun.json());
}

async function postProfileData(
    fixtures: PrepFixtures,
    profileValue: number,
    periodFrom: string,
    periodTo: string
): Promise<void> {
    const { Request, GeneratePayload, Responses } = fixtures;
    const payload = await GeneratePayload.energyData.profile1Month();
    payload.timeZone = 'CET';
    payload.periodFrom = `${periodFrom}T00:00:00.000Z`;
    payload.periodTo = `${periodTo}T00:00:00.000Z`;
    payload.entries[0].periodFrom = `${periodFrom}T00:00:00.000Z`;
    payload.entries[0].value = profileValue;

    const profiles = await Request.post('billing-by-profile', { data: payload });
    await expect(profiles).CheckResponse();
    const profileData = await profiles.json();
    Responses.dataByProfiles.push({
        id: profileData,
        periodFrom: payload.periodFrom,
        periodTo: payload.periodTo,
        periodType: payload.periodType,
    });
}

async function prepareSettlementBilling(
    fixtures: PrepFixtures,
    options: SettlementPrepOptions
): Promise<void> {
    const {
        expression,
        displayName,
        profileValue,
        invoiceDate = DEFAULT_INVOICE_DATE,
        periodFrom = DEFAULT_PERIOD_FROM,
        periodTo = DEFAULT_PERIOD_TO,
    } = options;
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;

    await test.step('generate customer', async () => {
        const customer = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_legal(),
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    await test.step('generate price component', async () => {
        const payload = GeneratePayload.productAndServices.priceSettlement();
        payload.formulaRequest.expression = expression;
        payload.displayName = displayName;
        // Pin 100% so billed kWh equals profile value (default uses random percentage).
        payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage = 100;
        const price = await Request.post(Endpoints.priceComponent, { data: payload });
        await expect(price).CheckResponse();
        Responses.priceComponent.push(await price.json());
    });

    await test.step('generate POD', async () => {
        const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
        const podSettlement = await Request.post(Endpoints.pod, { data: payload });
        await expect(podSettlement).CheckResponse();
        Responses.pod.push(await podSettlement.json());
    });

    await test.step('generate term', async () => {
        const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
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
            data: await GeneratePayload.pointsOfDelivery.pod_activation(),
        });
        await expect(podActivation).CheckResponse();
    });

    await test.step('Data by profiles', async () => {
        await postProfileData(fixtures, profileValue, periodFrom, periodTo);
    });

    await test.step('Billing run', async () => {
        await postBillingRun(fixtures, invoiceDate);
    });
}

async function prepareDualPcSettlementBilling(
    fixtures: PrepFixtures,
    options: DualPcSettlementPrepOptions
): Promise<void> {
    const {
        pcs,
        profileValue,
        invoiceDate = DEFAULT_INVOICE_DATE,
        periodFrom = DEFAULT_PERIOD_FROM,
        periodTo = DEFAULT_PERIOD_TO,
    } = options;
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;

    await test.step('generate customer', async () => {
        const customer = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_legal(),
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    for (const [index, pc] of pcs.entries()) {
        await test.step(`generate price component ${index + 1}`, async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = pc.expression;
            payload.displayName = pc.displayName;
            // Pin 100% so billed kWh equals profile value (default uses random percentage).
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage = 100;
            const price = await Request.post(Endpoints.priceComponent, { data: payload });
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });
    }

    await test.step('generate POD', async () => {
        const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
        const podSettlement = await Request.post(Endpoints.pod, { data: payload });
        await expect(podSettlement).CheckResponse();
        Responses.pod.push(await podSettlement.json());
    });

    await test.step('generate term', async () => {
        const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
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
            data: await GeneratePayload.pointsOfDelivery.pod_activation(),
        });
        await expect(podActivation).CheckResponse();
    });

    await test.step('Data by profiles', async () => {
        await postProfileData(fixtures, profileValue, periodFrom, periodTo);
    });

    await test.step('Billing run', async () => {
        await postBillingRun(fixtures, invoiceDate);
    });
}

type ScaleTariffPayload = {
    dateFrom: string;
    dateTo: string;
    numberOfDays: number;
    invoiceDate: string;
    billingByScalesTableCreateRequests: Array<{
        periodFrom: string;
        periodTo: string;
        volumes?: string;
        totalVolumes?: string;
    }>;
};

function inclusiveCalendarDays(from: string, to: string): number {
    const start = Date.parse(`${from}T00:00:00Z`);
    const end = Date.parse(`${to}T00:00:00Z`);
    return Math.round((end - start) / 86_400_000) + 1;
}

function pinCurrentMonthScaleTariffPayload(payload: ScaleTariffPayload): void {
    payload.dateFrom = DEFAULT_PERIOD_FROM;
    payload.dateTo = DEFAULT_PERIOD_TO;
    payload.numberOfDays = inclusiveCalendarDays(DEFAULT_PERIOD_FROM, DEFAULT_PERIOD_TO);
    payload.invoiceDate = `${DEFAULT_PERIOD_FROM}T00:00:00.000Z`;

    const [tariffRow, scaleRowA, scaleRowB] = payload.billingByScalesTableCreateRequests;

    tariffRow.periodFrom = DEFAULT_PERIOD_FROM;
    tariffRow.periodTo = DEFAULT_PERIOD_TO;
    tariffRow.volumes = RND07_TARIFF_VOLUME;

    scaleRowA.periodFrom = DEFAULT_PERIOD_FROM;
    scaleRowA.periodTo = PERIOD_HALF;
    scaleRowA.totalVolumes = RND07_SCALE_HALF_A;

    scaleRowB.periodFrom = PERIOD_HALF_PLUS_ONE;
    scaleRowB.periodTo = DEFAULT_PERIOD_TO;
    scaleRowB.totalVolumes = RND07_SCALE_HALF_B;
}

async function prepareScalesNumberOfDaysBilling(
    fixtures: ScalesPrepFixtures,
    options: ScalesNumberOfDaysPrepOptions = {}
): Promise<void> {
    const {
        expression = '1',
        displayName = 'RND_PC_SCALES',
        invoiceDate = DEFAULT_INVOICE_DATE,
    } = options;
    const { Request, GeneratePayload, Responses, Endpoints, Nomenclatures } = fixtures;

    await test.step('generate customer', async () => {
        const customer = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_legal(),
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    await test.step('POD create', async () => {
        const grid = await Nomenclatures.grid_operator('me');
        const measurementType = await Nomenclatures.measurement_type('me', grid);
        const payload = GeneratePayload.pointsOfDelivery.pod_slp();
        payload.gridOperatorId = grid;
        payload.measurementTypeId = measurementType;
        const podSlp = await Request.post(Endpoints.pod, { data: payload });
        await expect(podSlp).CheckResponse();
        Responses.pod.push(await podSlp.json());
    });

    await test.step('Meters create', async () => {
        const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
        const podJsonBody = await podGet.json();
        const payloadMeter = GeneratePayload.pointsOfDelivery.meters();
        const scaleCode = await Nomenclatures.scales_code_numberOfDays('scaleCodenumberOfDays');
        const scalesTariff = await Nomenclatures.scales_tariff_numberOfDays('grid');
        payloadMeter.gridOperatorId = podJsonBody.gridOperatorId;
        payloadMeter.podId = podJsonBody.id;
        payloadMeter.meterScales = [scaleCode, scalesTariff];
        const meter = await Request.post(Endpoints.meters, { data: payloadMeter });
        await expect(meter).CheckResponse();
        Responses.meters.push(await meter.json());
    });

    await test.step('generate term', async () => {
        const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
    });

    await test.step('generate price component with scale code', async () => {
        const scaleTariff = await Nomenclatures.scales_tariff_numberOfDays('grid');
        const scaleCode = await Nomenclatures.scales_code_numberOfDays('scaleCodenumberOfDays');
        const payload = GeneratePayload.productAndServices.scaleComponent();
        payload.formulaRequest.expression = expression;
        payload.displayName = displayName;
        payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleTariff;
        payload.applicationModelRequest.volumesByScaleRequest.scaleIds[1] = scaleCode;
        const priceComp = await Request.post(Endpoints.priceComponent, { data: payload });
        await expect(priceComp).CheckResponse();
        Responses.priceComponent.push(await priceComp.json());
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
            data: await GeneratePayload.pointsOfDelivery.pod_activation(),
        });
        await expect(podActivation).CheckResponse();
    });

    await test.step('Data by scales', async () => {
        const payload = await GeneratePayload.energyData.scaleTariff();
        pinCurrentMonthScaleTariffPayload(payload);
        const scales = await Request.post(Endpoints.dataByScales, { data: payload });
        await expect(scales).CheckResponse();
        Responses.dataByScales.push(await scales.json());
    });

    await test.step('Billing run', async () => {
        await postBillingRun(fixtures, invoiceDate);
    });
}

function attachResponses(testTitle: string, Responses: PrepFixtures['Responses']): void {
    test.info().attach(`${testTitle} response`, {
        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
        contentType: 'application/json',
    });
}

test.describe('[REG-558]: Billing run rounding (REG-1231..REG-1237)', { tag: ['@billing', '@rounding'] }, () => {
    test('[REG-1231]: For volumes - Rounding - invoice 2dp HALF_UP round up (RND-01)', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const unitPrice = '0.10005';
        const volume = 100;

        await prepareSettlementBilling(fixtures, {
            expression: unitPrice,
            displayName: 'RND_PC_UP',
            profileValue: volume,
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('assert rounding', async () => {
            const n12 = mulHalfUp(volume, unitPrice, 12);
            const expected = expectedInvoiceMoney([n12], DEV_GLOBAL_VAT_PERCENT);
            const invoiceId = resolveInvoiceId(Responses);
            await assertInvoiceRounding(Request, invoiceId, {
                totalAmountExcludingVat: expected.totalAmountExcludingVat,
                totalAmountIncludingVat: expected.totalAmountIncludingVat,
                vatBuckets: [
                    {
                        vatRatePercent: expected.vatRatePercent,
                        amountExcludingVat: expected.totalAmountExcludingVat,
                        valueOfVat: expected.valueOfVat,
                    },
                ],
            });
        });

        attachResponses('[REG-1231]', Responses);
    });

    test('[REG-1232]: For volumes - Rounding - invoice 2dp HALF_UP round down (RND-02)', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const unitPrice = '0.10004';
        const volume = 100;

        await prepareSettlementBilling(fixtures, {
            expression: unitPrice,
            displayName: 'RND_PC_DOWN',
            profileValue: volume,
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('assert rounding', async () => {
            const n12 = mulHalfUp(volume, unitPrice, 12);
            const expected = expectedInvoiceMoney([n12], DEV_GLOBAL_VAT_PERCENT);
            const invoiceId = resolveInvoiceId(Responses);
            await assertInvoiceRounding(Request, invoiceId, {
                totalAmountExcludingVat: expected.totalAmountExcludingVat,
                totalAmountIncludingVat: expected.totalAmountIncludingVat,
                vatBuckets: [
                    {
                        vatRatePercent: expected.vatRatePercent,
                        amountExcludingVat: expected.totalAmountExcludingVat,
                        valueOfVat: expected.valueOfVat,
                    },
                ],
            });
        });

        attachResponses('[REG-1232]', Responses);
    });

    test('[REG-1233]: For volumes - Rounding - VAT calculated from rounded net (RND-03)', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const unitPrice = '0.10005';
        const volume = 100;

        await prepareSettlementBilling(fixtures, {
            expression: unitPrice,
            displayName: 'RND_PC_VAT',
            profileValue: volume,
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('assert VAT from rounded net', async () => {
            const n12 = mulHalfUp(volume, unitPrice, 12);
            const expected = expectedInvoiceMoney([n12], DEV_GLOBAL_VAT_PERCENT);
            const invoiceId = resolveInvoiceId(Responses);

            const invoiceRes = await Request.get(`invoice?id=${invoiceId}`);
            await expect(invoiceRes).CheckResponse();
            const invoiceJson = await invoiceRes.json();
            const vatBucket = (invoiceJson.invoiceVatRateResponses ?? [])[0];
            expect(vatBucket).toBeTruthy();

            await assertInvoiceRounding(Request, invoiceId, {
                totalAmountExcludingVat: expected.totalAmountExcludingVat,
                totalAmountIncludingVat: expected.totalAmountIncludingVat,
                vatBuckets: [
                    {
                        vatRatePercent: expected.vatRatePercent,
                        amountExcludingVat: expected.totalAmountExcludingVat,
                        valueOfVat: expected.valueOfVat,
                    },
                ],
            });
        });

        attachResponses('[REG-1233]', Responses);
    });

    test('[REG-1234]: For volumes - Rounding - group-then-round vs round-each-then-sum (RND-04)', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const unitPrice = '0.01004';
        const volume = 100;

        await prepareDualPcSettlementBilling(fixtures, {
            pcs: [
                { expression: unitPrice, displayName: 'RND_PC_A' },
                { expression: unitPrice, displayName: 'RND_PC_B' },
            ],
            profileValue: volume,
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('assert group-then-round totals', async () => {
            const n12a = mulHalfUp(volume, unitPrice, 12);
            const n12b = mulHalfUp(volume, unitPrice, 12);
            const expected = expectedInvoiceMoney([n12a, n12b], DEV_GLOBAL_VAT_PERCENT);
            const naiveTotal = naiveRoundEachThenSum([n12a, n12b]);

            expect(naiveTotal).not.toBe(expected.totalAmountExcludingVat);
            expect(expected.totalAmountExcludingVat).toBe('2.01');

            const invoiceId = resolveInvoiceId(Responses);
            await assertInvoiceRounding(Request, invoiceId, {
                totalAmountExcludingVat: expected.totalAmountExcludingVat,
                totalAmountIncludingVat: expected.totalAmountIncludingVat,
                vatBuckets: [
                    {
                        vatRatePercent: expected.vatRatePercent,
                        amountExcludingVat: expected.totalAmountExcludingVat,
                        valueOfVat: expected.valueOfVat,
                    },
                ],
            });
        });

        attachResponses('[REG-1234]', Responses);
    });

    test('[REG-1235]: For volumes - Rounding - profile consumption scale 8 decimals (RND-05)', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const unitPrice = '1';
        const volume = 100.123456789;
        const volumeAt8 = halfUp(volume, 8);

        await prepareSettlementBilling(fixtures, {
            expression: unitPrice,
            displayName: 'RND_PC_VOL8',
            profileValue: volume,
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('assert volume scale 8 and invoice money', async () => {
            const n12 = mulHalfUp(volumeAt8, unitPrice, 12);
            const expected = expectedInvoiceMoney([n12], DEV_GLOBAL_VAT_PERCENT);
            const invoiceId = resolveInvoiceId(Responses);
            await assertInvoiceRounding(Request, invoiceId, {
                totalAmountExcludingVat: expected.totalAmountExcludingVat,
                totalAmountIncludingVat: expected.totalAmountIncludingVat,
                vatBuckets: [
                    {
                        vatRatePercent: expected.vatRatePercent,
                        amountExcludingVat: expected.totalAmountExcludingVat,
                        valueOfVat: expected.valueOfVat,
                    },
                ],
                lines: [
                    {
                        priceComponent: SETTLEMENT_PC_SUMMARY_LABEL,
                        totalVolumes: volumeAt8,
                        value: halfUp(n12, 2),
                    },
                ],
            });
        });

        attachResponses('[REG-1235]', Responses);
    });

    test('[REG-1236]: For volumes - Rounding - SafeDivision formula 1/3 at 12dp (RND-06)', async ({ Request, GeneratePayload, Responses, Endpoints }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const expression = '1/3';
        const volume = 100;
        const unitPrice12 = divHalfUp('1', '3', 12);

        await prepareSettlementBilling(fixtures, {
            expression,
            displayName: 'RND_PC_DIV',
            profileValue: volume,
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('assert SafeDivision unit price and invoice totals', async () => {
            expect(unitPrice12).toBe('0.333333333333');

            const n12 = mulHalfUp(volume, unitPrice12, 12);
            const expected = expectedInvoiceMoney([n12], DEV_GLOBAL_VAT_PERCENT);
            const invoiceId = resolveInvoiceId(Responses);
            await assertInvoiceRounding(Request, invoiceId, {
                totalAmountExcludingVat: expected.totalAmountExcludingVat,
                totalAmountIncludingVat: expected.totalAmountIncludingVat,
                vatBuckets: [
                    {
                        vatRatePercent: expected.vatRatePercent,
                        amountExcludingVat: expected.totalAmountExcludingVat,
                        valueOfVat: expected.valueOfVat,
                    },
                ],
                lines: [
                    {
                        priceComponent: SETTLEMENT_PC_SUMMARY_LABEL,
                        unitPrice: unitPrice12,
                        value: halfUp(n12, 2),
                    },
                ],
            });
        });

        attachResponses('[REG-1236]', Responses);
    });

    test('[REG-1237]: For volumes - Rounding - scales number-of-days day-split volume reconcile (RND-07)', async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints, Nomenclatures };
        const unitPrice = '1';

        await prepareScalesNumberOfDaysBilling(fixtures, {
            expression: unitPrice,
            displayName: 'RND_PC_SCALES',
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('assert scales volume reconcile and invoice money', async () => {
            const n12 = mulHalfUp(RND07_EXPECTED_BILLED_VOLUME, unitPrice, 12);
            const expected = expectedInvoiceMoney([n12], DEV_GLOBAL_VAT_PERCENT);
            const invoiceId = resolveInvoiceId(Responses);
            await assertInvoiceRounding(Request, invoiceId, {
                totalAmountExcludingVat: expected.totalAmountExcludingVat,
                totalAmountIncludingVat: expected.totalAmountIncludingVat,
                vatBuckets: [
                    {
                        vatRatePercent: expected.vatRatePercent,
                        amountExcludingVat: expected.totalAmountExcludingVat,
                        valueOfVat: expected.valueOfVat,
                    },
                ],
                lines: [
                    {
                        priceComponent: SCALES_PC_SUMMARY_LABEL,
                        unitPrice,
                        totalVolumes: halfUp(RND07_EXPECTED_BILLED_VOLUME, 8),
                        value: halfUp(n12, 2),
                    },
                ],
            });
        });

        attachResponses('[REG-1237]', Responses);
    });
});
