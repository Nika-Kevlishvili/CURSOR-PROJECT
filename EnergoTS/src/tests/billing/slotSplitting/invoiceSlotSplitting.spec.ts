import {test, expect} from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { slotSplittingDataPrep } from '../dataPreparation/slotSplittingDataPrep';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-1019]: For volumes', () => {
            // Run in order in a single worker, overriding fullyParallel. Prep is a leading
            // TEST (not beforeAll): a worker restarts after a test failure and re-runs
            // beforeAll hooks, but a passed test is never re-run — so this generates the
            // billing runs exactly once even when later cases fail.
            test.describe.configure({ mode: 'default' });

            test('data preparation: generate billings and wait for invoices', async ({ clearStashedResponses, GeneratePayload, saveResponsesToFile, Request, Endpoints }) => {
                test.setTimeout(35 * 60 * 1000);
                const fixtures = { saveResponsesToFile, Request, Endpoints };

                clearStashedResponses('slotSplitting_spec_ts');

                const prep = new slotSplittingDataPrep(fixtures, 'slotSplitting_spec_ts');

                await prep.REG_1020().catch(e => console.warn('REG-1020 prep failed:', (e as Error).message));
                await prep.REG_1021().catch(e => console.warn('REG-1021 prep failed:', (e as Error).message));
                await prep.REG_1022().catch(e => console.warn('REG-1022 prep failed:', (e as Error).message));

                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('slotSplitting_spec_ts', true)
                    .catch(e => console.warn('Some billing runs failed:', (e as Error).message));
            });

            test('[REG-1020]: invoice slot splitting - split by price component', async ({Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('slotSplitting_spec_ts', 'REG-1020'));

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1020] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1021]: invoice slot splitting - split by billing group', async ({Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('slotSplitting_spec_ts', 'REG-1021'));

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1021] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1022]: invoice slot splitting - split by seperate checkbox', async ({Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('slotSplitting_spec_ts', 'REG-1022'));

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1022] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})