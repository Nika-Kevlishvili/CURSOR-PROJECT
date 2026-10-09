/**
 * Combined FOR_VOLUMES helpers: profile vs scale totals.
 *
 * Runtime (BillingRunProcessSettlementService.evaluateSettlement):
 * when scaleValueForCombined != profileTotalForCombined, billed kWh is rescaled
 * toward the scale total; when they are equal, rescale is skipped and billed kWh
 * stays on the profile-priced settlement.
 */
import { test, expect } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';

export const COMBINED_MATCH_STASH_ID = 'REG-1296';
export const COMBINED_MATCH_VOLUME = 100;
export const COMBINED_MATCH_PERCENT = 50;
export const COMBINED_MATCH_PRICE = 10;
export const COMBINED_MATCH_SCALE_DIFFERENCE = 10;
export const COMBINED_MATCH_SCALE_MULTIPLIER = 10;

export const COMBINED_MISMATCH_STASH_ID = 'REG-974';
export const COMBINED_MISMATCH_SCALE_VOLUME = 40;
export const COMBINED_MISMATCH_PROFILE_VOLUME = 80;
export const COMBINED_MISMATCH_PERCENT = 50;
export const COMBINED_MISMATCH_PRICE = 10;
export const COMBINED_MISMATCH_SCALE_DIFFERENCE = 5;
export const COMBINED_MISMATCH_SCALE_MULTIPLIER = 8;

type InvoiceDetailedRow = {
    meter?: string | number | null;
    newMeterReading?: string | number | null;
    oldMeterReading?: string | number | null;
    unitPrice?: string | number | null;
    value?: string | number | null;
    totalVolumes?: string | number | null;
};

function amount(value: string | number | null | undefined): number {
    return Number(value ?? 0);
}

function asId(value: unknown): string | number {
    if (typeof value === 'object' && value !== null && 'id' in value) {
        return (value as { id: string | number }).id;
    }
    return value as string | number;
}

export function expectedCombinedMatchVolume(): number {
    return COMBINED_MATCH_VOLUME * (COMBINED_MATCH_PERCENT / 100);
}

export function expectedCombinedMatchAmount(): number {
    return expectedCombinedMatchVolume() * COMBINED_MATCH_PRICE;
}

export function expectedCombinedMismatchVolume(): number {
    return COMBINED_MISMATCH_SCALE_VOLUME * (COMBINED_MISMATCH_PERCENT / 100);
}

export function expectedCombinedMismatchAmount(): number {
    return expectedCombinedMismatchVolume() * COMBINED_MISMATCH_PRICE;
}

async function assertCombinedInvoiceTotals(
    Request: baseFixture['Request'],
    Endpoints: baseFixture['Endpoints'],
    Responses: baseFixture['Responses'],
    options: {
        expectedVolume: number;
        expectedAmount: number;
        expectedPrice: number;
        volumeMessage: string;
    }
): Promise<void> {
    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);

    const invoiceId = Responses.invoice[0];

    await test.step('Invoice tabs: billed kWh and amount', async () => {
        const invoiceRes = await Request.get(`${Endpoints.invoice}?id=${invoiceId}`);
        await expect(invoiceRes).CheckResponse();
        const invoice = await invoiceRes.json();
        expect(invoice.invoiceStatus).toBe('REAL');
        expect(invoice.invoiceType).toBe('STANDARD');
        expect(amount(invoice.totalAmountExcludingVat)).toBeCloseTo(options.expectedAmount, 2);

        const summaryRes = await Request.get(`${Endpoints.invoice}/summary-data?id=${invoiceId}&page=0&size=25`);
        await expect(summaryRes).CheckResponse();
        const summaryRows: Array<{ totalVolumes?: string | number | null; value?: string | number | null }> =
            (await summaryRes.json()).content ?? [];
        expect(summaryRows.length, 'invoice/summary-data returned no lines').toBeGreaterThan(0);
        const summaryVolume = summaryRows.reduce((sum, row) => sum + amount(row.totalVolumes), 0);
        const summaryValue = summaryRows.reduce((sum, row) => sum + amount(row.value), 0);
        expect(summaryVolume).toBeCloseTo(options.expectedVolume, 4);
        expect(summaryValue).toBeCloseTo(options.expectedAmount, 2);

        const detailedRes = await Request.get(`${Endpoints.invoice}/detailed-data?id=${invoiceId}&page=0&size=100`);
        await expect(detailedRes).CheckResponse();
        const detailedRows: InvoiceDetailedRow[] = (await detailedRes.json()).content ?? [];
        const energyRows = detailedRows.filter(row => amount(row.totalVolumes) !== 0 || amount(row.value) !== 0);
        expect(energyRows.length, 'invoice/detailed-data returned no energy lines').toBeGreaterThan(0);

        const billedVolume = energyRows.reduce((sum, row) => sum + amount(row.totalVolumes), 0);
        const billedValue = energyRows.reduce((sum, row) => sum + amount(row.value), 0);
        expect(billedVolume, options.volumeMessage).toBeCloseTo(options.expectedVolume, 4);
        expect(billedValue).toBeCloseTo(options.expectedAmount, 2);

        for (const row of energyRows) {
            expect(amount(row.unitPrice)).toBeCloseTo(options.expectedPrice, 4);
            expect(amount(row.value)).toBeCloseTo(amount(row.totalVolumes) * options.expectedPrice, 2);
            expect(row.meter, 'Combined settlement line should not copy scale meter number').toBeFalsy();
        }
    });
}

export async function assertCombinedMismatchInvoice(
    Request: baseFixture['Request'],
    Endpoints: baseFixture['Endpoints'],
    Responses: baseFixture['Responses']
): Promise<void> {
    const pcId = asId(Responses.priceComponent[0]);
    const scaleId = asId(Responses.dataByScales[0]);
    const profileHeader = Responses.dataByProfiles[0];
    const expectedVolume = expectedCombinedMismatchVolume();
    const expectedAmount = expectedCombinedMismatchAmount();

    await test.step('Price component formula and profile percent', async () => {
        const pcRes = await Request.get(`${Endpoints.priceComponent}/${pcId}`);
        await expect(pcRes).CheckResponse();
        const pc = await pcRes.json();
        const percent = Number(
            pc.applicationModelResponse?.volumesBySettlementPeriodResponse?.profileResponses?.[0]?.percentage ?? 0
        );
        expect(Number(pc.priceFormula), 'priceFormula must be the mismatch-case constant').toBe(COMBINED_MISMATCH_PRICE);
        expect(percent, 'profile percentage must be the mismatch-case constant').toBe(COMBINED_MISMATCH_PERCENT);
    });

    await test.step('Profile total does not equal scale total', async () => {
        const profileRes = await Request.get(
            `billing-by-profile/${profileHeader.id}?periodFrom=${encodeURIComponent(profileHeader.periodFrom)}&periodTo=${encodeURIComponent(profileHeader.periodTo)}`
        );
        await expect(profileRes).CheckResponse();
        const profileJson = await profileRes.json();
        const profileTotal = (profileJson.entries ?? []).reduce(
            (sum: number, entry: { value?: string | number | null }) => sum + amount(entry.value),
            0
        );

        const scaleRes = await Request.get(`${Endpoints.dataByScales}/${scaleId}`);
        await expect(scaleRes).CheckResponse();
        const scaleJson = await scaleRes.json();
        const scaleTotal = (scaleJson.billingByScalesTableCreateRequests ?? []).reduce(
            (sum: number, row: { totalVolumes?: string | number | null }) => sum + amount(row.totalVolumes),
            0
        );

        expect(profileTotal, 'Profile interval sum must be the mismatch-case profile volume').toBe(COMBINED_MISMATCH_PROFILE_VOLUME);
        expect(scaleTotal, 'Scale totalVolumes must be the mismatch-case scale volume').toBe(COMBINED_MISMATCH_SCALE_VOLUME);
        expect(profileTotal, 'Mismatch case requires profile total != scale total').not.toBe(scaleTotal);
    });

    await assertCombinedInvoiceTotals(Request, Endpoints, Responses, {
        expectedVolume,
        expectedAmount,
        expectedPrice: COMBINED_MISMATCH_PRICE,
        volumeMessage: `When profile and scale totals differ, billed kWh must rescale to scale * percent/100 (${expectedVolume})`,
    });
}

export async function assertCombinedMatchInvoice(
    Request: baseFixture['Request'],
    Endpoints: baseFixture['Endpoints'],
    Responses: baseFixture['Responses']
): Promise<void> {
    const pcId = asId(Responses.priceComponent[0]);
    const scaleId = asId(Responses.dataByScales[0]);
    const profileHeader = Responses.dataByProfiles[0];
    const expectedVolume = expectedCombinedMatchVolume();
    const expectedAmount = expectedCombinedMatchAmount();

    await test.step('Price component formula and profile percent', async () => {
        const pcRes = await Request.get(`${Endpoints.priceComponent}/${pcId}`);
        await expect(pcRes).CheckResponse();
        const pc = await pcRes.json();
        const percent = Number(
            pc.applicationModelResponse?.volumesBySettlementPeriodResponse?.profileResponses?.[0]?.percentage ?? 0
        );
        expect(Number(pc.priceFormula), 'priceFormula must be the match-case constant').toBe(COMBINED_MATCH_PRICE);
        expect(percent, 'profile percentage must be the match-case constant').toBe(COMBINED_MATCH_PERCENT);
    });

    await test.step('Profile total equals scale total', async () => {
        const profileRes = await Request.get(
            `billing-by-profile/${profileHeader.id}?periodFrom=${encodeURIComponent(profileHeader.periodFrom)}&periodTo=${encodeURIComponent(profileHeader.periodTo)}`
        );
        await expect(profileRes).CheckResponse();
        const profileJson = await profileRes.json();
        const profileTotal = (profileJson.entries ?? []).reduce(
            (sum: number, entry: { value?: string | number | null }) => sum + amount(entry.value),
            0
        );

        const scaleRes = await Request.get(`${Endpoints.dataByScales}/${scaleId}`);
        await expect(scaleRes).CheckResponse();
        const scaleJson = await scaleRes.json();
        const scaleTotal = (scaleJson.billingByScalesTableCreateRequests ?? []).reduce(
            (sum: number, row: { totalVolumes?: string | number | null }) => sum + amount(row.totalVolumes),
            0
        );

        expect(profileTotal, 'Profile interval sum must be the match-case volume').toBe(COMBINED_MATCH_VOLUME);
        expect(scaleTotal, 'Scale totalVolumes must equal the profile total').toBe(COMBINED_MATCH_VOLUME);
        expect(profileTotal).toBe(scaleTotal);
    });

    await assertCombinedInvoiceTotals(Request, Endpoints, Responses, {
        expectedVolume,
        expectedAmount,
        expectedPrice: COMBINED_MATCH_PRICE,
        volumeMessage: `When profile and scale totals match, billed kWh must stay on profile (${expectedVolume})`,
    });
}
