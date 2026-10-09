import { test, expect, type baseFixture } from '../../../backend/fixtures/baseFixture';
import { volumesDataPrep } from '../dataPreparation/forVolumesDataPrep';
import reportGenerator from '../../../backend/utils/generateReport';

// Exact amounts set in forVolumesDataPrep REG_718 / REG_718_LONGER_PAYMENT (excluding VAT).
const SAME_PERIOD_IAP_AMOUNT = 300;
const LONGER_PAYMENT_IAP_AMOUNT = 100;

type InvoicePreview = {
    invoiceType?: string;
    invoiceStatus?: string;
    invoiceDocumentType?: string;
    paymentDeadLine?: string;
    totalAmountExcludingVat?: string | number;
    basisForIssuing?: string;
};

type InvoiceSummaryRow = {
    priceComponent?: string;
    value?: string | number | null;
};

type DeductionCaseOptions = {
    iapAmount: number;
    deductionFrom: 'FIRST_INVOICE_FOR_SAME_PERIOD' | 'FIRST_INVOICE_WITH_LONGER_PAYMENT_TERM';
    requireEarlierIapDueDate: boolean;
    expectedIapTermValue?: number;
};

function amount(value: string | number | null | undefined): number {
    return Number(value);
}

function isInterimSummaryRow(row: InvoiceSummaryRow): boolean {
    return String(row.priceComponent ?? '').startsWith('INTERIM');
}

async function assertInterimDeductionCase(
    Request: baseFixture['Request'],
    Endpoints: baseFixture['Endpoints'],
    Responses: baseFixture['Responses'],
    options: DeductionCaseOptions
): Promise<void> {
    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThanOrEqual(2);

    const iapId = Responses.interim[0];

    await test.step(`IAP deductionFrom is ${options.deductionFrom}, exact ${options.iapAmount}`, async () => {
        expect(iapId, 'IAP id missing from stash').toBeTruthy();
        const iapRes = await Request.get(`${Endpoints.interim}/${iapId}`);
        await expect(iapRes).CheckResponse();
        const iap = await iapRes.json();

        expect(iap.deductionFrom).toBe(options.deductionFrom);
        expect(iap.matchesWithTermOfStandardInvoice).toBe(false);
        expect(iap.paymentType).toBe('OBLIGATORY');
        expect(iap.valueType).toBe('EXACT_AMOUNT');
        expect(amount(iap.value)).toBe(options.iapAmount);
        expect(iap.issuingForTheMonthToCurrent).toBe('ZERO');
        expect(iap.interimAdvancePaymentTerm?.calendarType).toBe('WORKING_DAYS');
        if (options.expectedIapTermValue !== undefined) {
            expect(iap.interimAdvancePaymentTerm?.value).toBe(options.expectedIapTermValue);
        }
    });

    let iapInvoiceId!: number | string;
    let volumesInvoiceId!: number | string;
    let iapInvoice!: InvoicePreview;
    let volumesInvoice!: InvoicePreview;

    await test.step('Load invoices by type (IAP vs STANDARD volumes)', async () => {
        const previews: Array<{ id: number | string; body: InvoicePreview }> = [];
        for (const id of Responses.invoice) {
            const res = await Request.get(`${Endpoints.invoice}?id=${id}`);
            await expect(res).CheckResponse();
            previews.push({ id, body: await res.json() });
        }

        const iapPreview = previews.find(p => p.body.invoiceType === 'INTERIM_AND_ADVANCE_PAYMENT');
        const volumesPreview = previews.find(p => p.body.invoiceType === 'STANDARD');
        expect(iapPreview, 'No INTERIM_AND_ADVANCE_PAYMENT invoice in stash').toBeTruthy();
        expect(volumesPreview, 'No STANDARD volumes invoice in stash').toBeTruthy();

        iapInvoiceId = iapPreview!.id;
        volumesInvoiceId = volumesPreview!.id;
        iapInvoice = iapPreview!.body;
        volumesInvoice = volumesPreview!.body;
    });

    await test.step(`IAP invoice is REAL INTERIM for ${options.iapAmount} excl. VAT`, async () => {
        expect(iapInvoice.invoiceStatus).toBe('REAL');
        expect(iapInvoice.invoiceDocumentType).toBe('INVOICE');
        expect(amount(iapInvoice.totalAmountExcludingVat)).toBe(options.iapAmount);
        expect(iapInvoice.basisForIssuing).toBe('Междинно/авансово плащане за периода');
        expect(iapInvoice.paymentDeadLine, 'IAP paymentDeadLine missing').toBeTruthy();
    });

    await test.step(`IAP summary row is +${options.iapAmount} INTERIM`, async () => {
        const res = await Request.get(`${Endpoints.invoice}/summary-data?id=${iapInvoiceId}&page=0&size=25`);
        await expect(res).CheckResponse();
        const rows: InvoiceSummaryRow[] = (await res.json()).content ?? [];
        const interimRow = rows.find(isInterimSummaryRow);
        expect(interimRow, 'IAP invoice has no INTERIM summary row').toBeTruthy();
        expect(amount(interimRow!.value)).toBe(options.iapAmount);
    });

    await test.step('Volumes invoice deducts the IAP amount', async () => {
        expect(volumesInvoice.invoiceStatus).toBe('REAL');
        expect(volumesInvoice.paymentDeadLine, 'Volumes paymentDeadLine missing').toBeTruthy();

        if (options.requireEarlierIapDueDate) {
            expect(
                volumesInvoice.paymentDeadLine! > iapInvoice.paymentDeadLine!,
                `IAP due date ${iapInvoice.paymentDeadLine} must be before volumes due date ${volumesInvoice.paymentDeadLine}`
            ).toBe(true);
        }

        const summaryRes = await Request.get(`${Endpoints.invoice}/summary-data?id=${volumesInvoiceId}&page=0&size=25`);
        await expect(summaryRes).CheckResponse();
        const rows: InvoiceSummaryRow[] = (await summaryRes.json()).content ?? [];
        expect(rows.length, 'Volumes invoice/summary-data returned no lines').toBeGreaterThanOrEqual(2);

        const deductionRow = rows.find(isInterimSummaryRow);
        expect(deductionRow, 'Volumes invoice has no INTERIM deduction row').toBeTruthy();
        expect(amount(deductionRow!.value)).toBe(-options.iapAmount);

        const energyExclVat = rows
            .filter(row => !isInterimSummaryRow(row))
            .reduce((sum, row) => sum + amount(row.value), 0);
        expect(energyExclVat, 'Volumes energy summary value must be > 0').toBeGreaterThan(0);

        const netExclVat = energyExclVat - options.iapAmount;
        expect(amount(volumesInvoice.totalAmountExcludingVat)).toBeCloseTo(Math.abs(netExclVat), 2);
        // After interim deduction, Phoenix sets CREDIT_NOTE if net < 0, else DEBIT_NOTE
        // (BillingRunStandardInvoiceGenerationProcessor when isDeducted).
        expect(volumesInvoice.invoiceDocumentType).toBe(netExclVat < 0 ? 'CREDIT_NOTE' : 'DEBIT_NOTE');
    });
}

test.describe('[REG-55]: Billing ', { tag: '@billing' }, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-562]: interim and advanced payments', () => {
            // Run in order in a single worker, overriding fullyParallel. Prep is a leading
            // TEST (not beforeAll): a worker restarts after a test failure and re-runs
            // beforeAll hooks, but a passed test is never re-run — so this generates the
            // billing runs exactly once even when later cases fail.
            test.describe.configure({ mode: 'default' });

            test('data preparation: generate billings and wait for invoices', async ({ clearStashedResponses, GeneratePayload, Nomenclatures, saveResponsesToFile, Request, Responses, Endpoints, FileUploadRequest }) => {
                test.setTimeout(45 * 60 * 1000);
                const fixtures = { clearStashedResponses, GeneratePayload, Nomenclatures, saveResponsesToFile, Request, Responses, Endpoints, FileUploadRequest };

                fixtures.clearStashedResponses('deduction_spec_ts');
                const prep = new volumesDataPrep(fixtures, 'deduction_spec_ts');
                await prep.REG_718().catch(e => console.warn('REG-718 prep failed:', (e as Error).message));
                await prep.REG_718_LONGER_PAYMENT().catch(e => console.warn('REG-718 longer payment prep failed:', (e as Error).message));
            });

            test('[REG-718]: Interim deduction (from same period)', async ({ Request, Endpoints, Responses, loadResponseById }) => {
                Object.assign(Responses, loadResponseById('deduction_spec_ts', 'REG-718'));
                await assertInterimDeductionCase(Request, Endpoints, Responses, {
                    iapAmount: SAME_PERIOD_IAP_AMOUNT,
                    deductionFrom: 'FIRST_INVOICE_FOR_SAME_PERIOD',
                    requireEarlierIapDueDate: false,
                });

                test.info().attach('[REG-718] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-718]: Interim deduction (from longer payment term)', async ({ Request, Endpoints, Responses, loadResponseById }) => {
                Object.assign(Responses, loadResponseById('deduction_spec_ts', 'REG-718-LONGER'));
                await assertInterimDeductionCase(Request, Endpoints, Responses, {
                    iapAmount: LONGER_PAYMENT_IAP_AMOUNT,
                    deductionFrom: 'FIRST_INVOICE_WITH_LONGER_PAYMENT_TERM',
                    requireEarlierIapDueDate: true,
                    expectedIapTermValue: 1,
                });

                test.info().attach('[REG-718 longer payment] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});
