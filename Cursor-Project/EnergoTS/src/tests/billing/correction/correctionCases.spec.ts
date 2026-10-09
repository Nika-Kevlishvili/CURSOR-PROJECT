import {test, expect} from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { correctionDataPrep } from '../dataPreparation/forCorrectionDataPrep';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-566]: Correction', () => {
            test.describe('[REG-633]: Price change flow', () => {
                // Run in order in a single worker, overriding fullyParallel. Prep is a
                // leading TEST (not beforeAll): a worker restarts after a test failure and
                // re-runs beforeAll hooks, but a passed test is never re-run — so this
                // generates the billing runs exactly once even when later cases fail. The
                // trailing cleanup test still runs last (default mode preserves order).
                test.describe.configure({ mode: 'default' });

                test('data preparation: generate billings and wait for invoices', async ({ clearStashedResponses, GeneratePayload, saveResponsesToFile, loadResponsesFromFile, loadResponseById, Request, Endpoints }) => {
                    test.setTimeout(80 * 60 * 1000);
                    const fixtures = { saveResponsesToFile, loadResponsesFromFile, loadResponseById, Request, Endpoints };

                    clearStashedResponses('correctionCases_spec_ts');
                    clearStashedResponses('correctionCases_spec_ts_part_2');
                    clearStashedResponses('correctionCases_1035_spec_ts');

                    const prep = new correctionDataPrep(fixtures, 'correctionCases_spec_ts', 'correctionCases_spec_ts_part_2');

                    await prep.REG_636_part1().catch(e => console.warn('REG-636 part 1 prep failed:', (e as Error).message));
                    await prep.REG_637_part1().catch(e => console.warn('REG-637 part 1 prep failed:', (e as Error).message));

                    try {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('correctionCases_spec_ts', true);
                    } catch (e) {
                        console.warn('Some billing runs failed, subsequent tests will handle their own failures:', (e as Error).message);
                    }

                    await prep.REG_636_part2().catch(e => console.warn('REG-636 part 2 prep failed:', (e as Error).message));
                    await prep.REG_637_part2().catch(e => console.warn('REG-637 part 2 prep failed:', (e as Error).message));

                    clearStashedResponses('forVolumes_spec_ts');

                    await GeneratePayload.billing.waitForInvoiceGenerationParallel('correctionCases_spec_ts_part_2', true);

                    await prep.REG_1035('correctionCases_1035_spec_ts').catch(e => console.warn('REG-1035 prep failed:', (e as Error).message));
                });

                test('[REG-636]: Price change flow happy pass', async ({Request, Responses, loadResponseById}) => {
                    Object.assign(Responses, loadResponseById('correctionCases_spec_ts_part_2', 'REG-636'));

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    await test.step('check summary data tab', async () => {
                        const invoiceResponse = await Request.get(`invoice/summary-data?page=0&size=25&id=${Responses.invoice[1]}`);
                        const invoiceSummaryData = await invoiceResponse.json();
                        expect(invoiceSummaryData.content[0].unitPrice).toBe('300.000000000000');
                    });

                    test.info().attach('[REG-636] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                });

                test('[REG-637]: Price change flow | add new price with existing slot', async ({Request, Responses, loadResponseById}) => {
                    Object.assign(Responses, loadResponseById('correctionCases_spec_ts_part_2', 'REG-637'));

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    await test.step('check summary data tab', async () => {
                        const invoiceResponse = await Request.get(`invoice/summary-data?page=0&size=25&id=${Responses.invoice[1]}`);
                        const invoiceSummaryData = await invoiceResponse.json();
                        expect(invoiceSummaryData.content.length).toBe(2);
                    });

                    test.info().attach('[REG-637] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                });

                test('[REG-1035]: Price change flow | add new price with different slot', async ({Request, Responses, loadResponseById}) => {
                    Object.assign(Responses, loadResponseById('correctionCases_1035_spec_ts', 'REG-1035'));

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    await test.step('check summary data tab', async () => {
                        const invoiceResponse = await Request.get(`invoice/summary-data?page=0&size=25&id=${Responses.invoice[1]}`);
                        const invoiceSummaryData = await invoiceResponse.json();
                        console.log('invoiceSummaryData', invoiceSummaryData);
                        expect(invoiceSummaryData.content.length).toBe(1);
                    });

                    test.info().attach('[REG-1035] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                });

                test('clean responses', async ({clearStashedResponses}) => {
                    clearStashedResponses('correctionCases_spec_ts_part_2');
                    clearStashedResponses('correctionCases_1035_spec_ts');
                });
            });

            test.describe('[REG-635]: Volume change flow', () => {
                // Prep is a leading TEST (not beforeAll) so a worker restart on failure
                // does not re-run it. Run in order in a single worker.
                test.describe.configure({ mode: 'default' });

                test('data preparation: generate billing and wait for invoices', async ({ clearStashedResponses, saveResponsesToFile, loadResponsesFromFile, loadResponseById, Request, Endpoints }) => {
                    test.setTimeout(25 * 60 * 1000);
                    const fixtures = { saveResponsesToFile, loadResponsesFromFile, loadResponseById, Request, Endpoints };

                    clearStashedResponses('correctionCases_703_spec_ts');

                    const prep = new correctionDataPrep(fixtures, 'correctionCases_spec_ts', 'correctionCases_spec_ts_part_2');
                    await prep.REG_703('correctionCases_703_spec_ts');
                });

                test('[REG-703]: Volume change flow happy pass', async ({Responses, loadResponseById}) => {
                    Object.assign(Responses, loadResponseById('correctionCases_703_spec_ts', 'REG-703'));

                    test.info().attach('[REG-703] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                });
            });
        });
    });
});
