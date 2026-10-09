import {test, expect} from '../../../backend/fixtures/baseFixture';
import { restrictionDataPrep } from '../dataPreparation/restrictionDataPrep';
import reportGenerator from '../../../backend/utils/generateReport';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-558]: For volumes', () => {
            // Run in order in a single worker, overriding fullyParallel. Prep is a leading
            // TEST (not beforeAll): a worker restarts after a test failure and re-runs
            // beforeAll hooks, but a passed test is never re-run — so this generates the
            // billing runs exactly once even when later cases fail.
            test.describe.configure({ mode: 'default' });

            test('data preparation: generate billings and wait for invoices', async ({ clearStashedResponses, GeneratePayload, Nomenclatures, saveResponsesToFile, Request, Responses, Endpoints, FileUploadRequest }) => {
                test.setTimeout(35 * 60 * 1000);
                const fixtures = { clearStashedResponses, GeneratePayload, Nomenclatures, saveResponsesToFile, Request, Responses, Endpoints, FileUploadRequest };

                fixtures.clearStashedResponses('restriction_for_value_spec_ts');
                const prep = new restrictionDataPrep(fixtures, 'restriction_for_value_spec_ts');

                await prep.REG_1166().catch(e => console.warn('REG-1166 prep failed:', (e as Error).message));
                await prep.REG_1167().catch(e => console.warn('REG-1167 prep failed:', (e as Error).message));
                await prep.REG_1171().catch(e => console.warn('REG-1171 prep failed:', (e as Error).message));

                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('restriction_for_value_spec_ts', true)
                    .catch(e => console.warn('Some billing runs failed:', (e as Error).message));
            });

            // --- Validation tests ------------------------------------------------------

            test('[REG-1166]: Restriction for value valid between ranges', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('restriction_for_value_spec_ts', 'REG-1166'));

                expect(Responses.invoice.length, 'No invoices found, billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs();
                await validateInvoice.restrictionValidator();

                test.info().attach('[REG-1166] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1167]: Restriction for value more than ranges', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('restriction_for_value_spec_ts', 'REG-1167'));

                expect(Responses.invoice.length, 'No invoices found, billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-1167] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1171]: Restriction for value NOT valid less than ranges', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('restriction_for_value_spec_ts', 'REG-1171'));

                expect(Responses.invoice.length, 'No invoices found, billing run likely failed').toBe(0);
                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-1171] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})