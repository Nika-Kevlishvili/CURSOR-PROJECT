import {test, expect} from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { interimDataPrep } from '../dataPreparation/interimDataPrep';


test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-562]: Interim and advanced payment', () => {
            test.describe.configure({ mode: 'default' });

            test('data preparation: generate billings and wait for invoices', async ({ clearStashedResponses, GeneratePayload, saveResponsesToFile, Request, Endpoints }) => {
                test.setTimeout(35 * 60 * 1000);
                const fixtures = { saveResponsesToFile, Request, Endpoints };

                clearStashedResponses('interimCases_spec_ts');
                clearStashedResponses('interimCases_spec_ts_part2');

                const prep = new interimDataPrep(fixtures, 'interimCases_spec_ts');

                // Phase 1: single-method cases + Part1 of two-part cases
                await prep.REG_978().catch(e => console.warn('REG-978 prep failed:', (e as Error).message));
                await prep.REG_1047().catch(e => console.warn('REG-1047 prep failed:', (e as Error).message));
                await prep.REG_716().catch(e => console.warn('REG-716 prep failed:', (e as Error).message));
                await prep.REG_975().catch(e => console.warn('REG-975 prep failed:', (e as Error).message));
                await prep.REG_1046_Part1().catch(e => console.warn('REG-1046 Part1 prep failed:', (e as Error).message));
                await prep.REG_1154_Part1().catch(e => console.warn('REG-1154 Part1 prep failed:', (e as Error).message));

                // Wait for Phase 1 billing runs to generate invoices
                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('interimCases_spec_ts', true)
                    .catch(e => console.warn('Some Phase 1 billing runs failed:', (e as Error).message));

                // Phase 2: Part2 of two-part cases (requires first invoice to exist)
                await prep.REG_1046_Part2().catch(e => console.warn('REG-1046 Part2 prep failed:', (e as Error).message));
                await prep.REG_1154_Part2().catch(e => console.warn('REG-1154 Part2 prep failed:', (e as Error).message));

                // Wait for Phase 2 billing runs
                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('interimCases_spec_ts_part2', true)
                    .catch(e => console.warn('Some Phase 2 billing runs failed:', (e as Error).message));
            });

            test('[REG-978]: Interim with From previous invoice but previous invoice missing (product contract)', async ({Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('interimCases_spec_ts', 'REG-978'));
                console.log(reportGenerator.setLinksToResponses(Responses));
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-978] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-1047]: Interim with From previous invoice but previous invoice missing (service contract)', async ({Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('interimCases_spec_ts', 'REG-1047'));
                console.log(reportGenerator.setLinksToResponses(Responses));
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1047] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-716]: Interim invoice exact amount | happy pass', async ({Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('interimCases_spec_ts', 'REG-716'));
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-716] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-975]: interim with price component | happy pass', async ({Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('interimCases_spec_ts', 'REG-975'));
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-975] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-1046]: From previous invoice - separate billing runs', async ({Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('interimCases_spec_ts_part2', 'REG-1046'));
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1046] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-1154]: Interim with From previous invoice but previous invoice found (product contract)', async ({Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('interimCases_spec_ts_part2', 'REG-1154'));
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1154] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});
