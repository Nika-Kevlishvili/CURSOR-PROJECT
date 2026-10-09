import { expect } from '../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../backend/fixtures/baseFixture';

export const DEV_GLOBAL_VAT_PERCENT = 20;

type PositiveDecimal = {
    digits: string;
    scale: number;
};

function parsePositiveDecimal(value: string | number): PositiveDecimal {
    let s = String(value).trim().replace(/^\+/, '');
    if (s.startsWith('-')) {
        throw new Error(`Expected non-negative decimal: ${value}`);
    }
    const [intPart = '0', fracPart = ''] = s.split('.');
    const intDigits = intPart.replace(/^0+(?=\d)/, '') || '0';
    return { digits: intDigits + fracPart, scale: fracPart.length };
}

function padDigitsToScale(digits: string, fromScale: number, toScale: number): bigint {
    if (toScale < fromScale) {
        throw new Error('padDigitsToScale only supports expanding scale');
    }
    return BigInt(digits + '0'.repeat(toScale - fromScale));
}

function formatAtScale(value: bigint, scale: number): string {
    if (scale <= 0) {
        return value.toString();
    }
    const s = value.toString().padStart(scale + 1, '0');
    const intPart = s.slice(0, -scale) || '0';
    const fracPart = s.slice(-scale);
    return `${intPart}.${fracPart}`;
}

function roundDigitsHalfUp(digits: string, fromScale: number, toScale: number): bigint {
    if (toScale > fromScale) {
        return padDigitsToScale(digits, fromScale, toScale);
    }

    const drop = fromScale - toScale;
    if (drop === 0) {
        return BigInt(digits || '0');
    }

    const keep = digits.slice(0, digits.length - drop) || '0';
    const roundChar = digits[digits.length - drop] ?? '0';
    let bi = BigInt(keep);
    if (roundChar >= '5') {
        bi += 1n;
    }
    return bi;
}

function multiplyDecimals(a: string | number, b: string | number): PositiveDecimal {
    const pa = parsePositiveDecimal(a);
    const pb = parsePositiveDecimal(b);
    return {
        digits: (BigInt(pa.digits) * BigInt(pb.digits)).toString(),
        scale: pa.scale + pb.scale,
    };
}

/** HALF_UP to `scale` decimal places using string/BigInt math (no float). */
export function halfUp(value: string | number, scale: number): string {
    const parts = parsePositiveDecimal(value);
    const rounded = roundDigitsHalfUp(parts.digits, parts.scale, scale);
    return formatAtScale(rounded, scale);
}

export function addDecimal(a: string | number, b: string | number): string {
    const pa = parsePositiveDecimal(a);
    const pb = parsePositiveDecimal(b);
    const maxScale = Math.max(pa.scale, pb.scale);
    const sum =
        padDigitsToScale(pa.digits, pa.scale, maxScale) + padDigitsToScale(pb.digits, pb.scale, maxScale);
    return formatAtScale(sum, maxScale);
}

/** SafeDivision-style HALF_UP divide (e.g. `divHalfUp('1', '3', 12)` → `0.333333333333`). */
export function divHalfUp(numerator: string | number, denominator: string | number, scale: number): string {
    const na = parsePositiveDecimal(numerator);
    const nb = parsePositiveDecimal(denominator);
    const numeratorScaled = BigInt(na.digits) * 10n ** BigInt(scale + nb.scale);
    const denominatorScaled = BigInt(nb.digits) * 10n ** BigInt(na.scale);
    const quotient = numeratorScaled / denominatorScaled;
    const remainder = numeratorScaled % denominatorScaled;
    let result = quotient;
    if (remainder * 2n >= denominatorScaled) {
        result += 1n;
    }
    return formatAtScale(result, scale);
}

export function mulHalfUp(a: string | number, b: string | number, scale: number): string {
    const product = multiplyDecimals(a, b);
    const rounded = roundDigitsHalfUp(product.digits, product.scale, scale);
    return formatAtScale(rounded, scale);
}

/**
 * nets12: working line nets still at high precision (or already 12dp strings).
 * Matches invoice path: sum per VAT group → 2 HALF_UP → VAT from rounded net → gross.
 */
export function expectedInvoiceMoney(
    nets12: Array<string | number>,
    vatRatePercent: number
): {
    totalAmountExcludingVat: string;
    valueOfVat: string;
    totalAmountIncludingVat: string;
    vatRatePercent: string;
} {
    const sum = nets12.reduce((acc, n) => addDecimal(acc, String(n)), '0');
    const totalAmountExcludingVat = halfUp(sum, 2);
    const vatProduct = multiplyDecimals(totalAmountExcludingVat, String(vatRatePercent));
    const valueOfVat = divHalfUp(
        formatAtScale(BigInt(vatProduct.digits), vatProduct.scale),
        '100',
        2
    );
    const totalAmountIncludingVat = halfUp(addDecimal(totalAmountExcludingVat, valueOfVat), 2);

    return {
        totalAmountExcludingVat,
        valueOfVat,
        totalAmountIncludingVat,
        vatRatePercent: halfUp(vatRatePercent, 2),
    };
}

/** For RND-04: proves group-then-round ≠ round-each-then-sum */
export function naiveRoundEachThenSum(nets12: Array<string | number>): string {
    return nets12.map((n) => halfUp(n, 2)).reduce((acc, n) => addDecimal(acc, n), '0');
}

function normalizeMoney(value: unknown): string {
    if (value === null || value === undefined) {
        throw new Error('Missing money value on invoice response');
    }
    return halfUp(String(value), 2);
}

type InvoiceVatBucket = {
    vatRatePercent?: number | string;
    amountExcludingVat?: number | string;
    valueOfVat?: number | string;
};

type InvoiceJson = {
    totalAmountExcludingVat?: number | string;
    totalAmountIncludingVat?: number | string;
    invoiceVatRateResponses?: InvoiceVatBucket[];
};

type SummaryDataRow = {
    priceComponent?: string;
    priceComponentName?: string;
    name?: string;
    displayName?: string;
    unitPrice?: number | string;
    totalVolumes?: number | string;
    value?: number | string;
};

type ExpectedSummaryLine = {
    /** Summary tab label — API field is usually `priceComponent` (e.g. BY_SETTLEMENT_PERIODS). */
    priceComponent?: string;
    /** Legacy / alternate matchers checked against row label fields. */
    priceComponentName?: string;
    unitPrice?: string;
    totalVolumes?: string;
    value?: string;
};

function summaryRowLabels(row: SummaryDataRow): string[] {
    return [row.priceComponent, row.priceComponentName, row.name, row.displayName].filter(
        (label): label is string => typeof label === 'string' && label.length > 0
    );
}

function findSummaryLine(rows: SummaryDataRow[], expectedLine: ExpectedSummaryLine): SummaryDataRow | undefined {
    const wantedLabel = expectedLine.priceComponent ?? expectedLine.priceComponentName;
    if (wantedLabel) {
        return rows.find((row) => summaryRowLabels(row).includes(wantedLabel));
    }
    if (rows.length === 1) {
        return rows[0];
    }
    return undefined;
}

export async function assertInvoiceRounding(
    Request: baseFixture['Request'],
    invoiceId: string | number,
    expectation: {
        totalAmountExcludingVat: string;
        totalAmountIncludingVat: string;
        vatBuckets?: Array<{
            vatRatePercent: string;
            amountExcludingVat: string;
            valueOfVat: string;
        }>;
        lines?: ExpectedSummaryLine[];
    }
): Promise<void> {
    const invoiceRes = await Request.get(`invoice?id=${invoiceId}`);
    await expect(invoiceRes).CheckResponse();
    const invoiceJson = (await invoiceRes.json()) as InvoiceJson;

    expect(normalizeMoney(invoiceJson.totalAmountExcludingVat)).toBe(
        normalizeMoney(expectation.totalAmountExcludingVat)
    );
    expect(normalizeMoney(invoiceJson.totalAmountIncludingVat)).toBe(
        normalizeMoney(expectation.totalAmountIncludingVat)
    );

    if (expectation.vatBuckets?.length) {
        const buckets = invoiceJson.invoiceVatRateResponses ?? [];
        for (const expectedBucket of expectation.vatBuckets) {
            const match = buckets.find(
                (bucket) => normalizeMoney(bucket.vatRatePercent ?? '') === normalizeMoney(expectedBucket.vatRatePercent)
            );
            expect(match, `VAT bucket ${expectedBucket.vatRatePercent}% not found on invoice`).toBeTruthy();
            expect(normalizeMoney(match?.amountExcludingVat)).toBe(normalizeMoney(expectedBucket.amountExcludingVat));
            expect(normalizeMoney(match?.valueOfVat)).toBe(normalizeMoney(expectedBucket.valueOfVat));
        }
    }

    if (expectation.lines?.length) {
        const summaryRes = await Request.get(`invoice/summary-data?id=${invoiceId}&page=0&size=25`);
        await expect(summaryRes).CheckResponse();
        const summaryJson = (await summaryRes.json()) as { content?: SummaryDataRow[] };
        const rows = summaryJson.content ?? [];

        for (const expectedLine of expectation.lines) {
            const match = findSummaryLine(rows, expectedLine);
            const labelHint = expectedLine.priceComponent ?? expectedLine.priceComponentName ?? 'row';
            expect(match, `Summary line for ${labelHint} not found`).toBeTruthy();
            if (expectedLine.unitPrice !== undefined) {
                expect(halfUp(String(match?.unitPrice), 12)).toBe(halfUp(expectedLine.unitPrice, 12));
            }
            if (expectedLine.totalVolumes !== undefined) {
                expect(halfUp(String(match?.totalVolumes), 8)).toBe(halfUp(expectedLine.totalVolumes, 8));
            }
            if (expectedLine.value !== undefined) {
                expect(normalizeMoney(match?.value)).toBe(normalizeMoney(expectedLine.value));
            }
        }
    }
}
