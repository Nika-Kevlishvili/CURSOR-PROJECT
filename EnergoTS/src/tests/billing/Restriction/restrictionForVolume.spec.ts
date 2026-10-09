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

                fixtures.clearStashedResponses('restriction_for_volume_spec_ts');
                const prep = new restrictionDataPrep(fixtures, 'restriction_for_volume_spec_ts');

                await prep.REG_1009().catch(e => console.warn('REG-1009 prep failed:', (e as Error).message));
                await prep.REG_1165().catch(e => console.warn('REG-1165 prep failed:', (e as Error).message));
                await prep.REG_1168().catch(e => console.warn('REG-1168 prep failed:', (e as Error).message));
                await prep.REG_1169().catch(e => console.warn('REG-1169 prep failed:', (e as Error).message));

                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('restriction_for_volume_spec_ts', true)
                    .catch(e => console.warn('Some billing runs failed:', (e as Error).message));
            });

            // --- Validation tests ------------------------------------------------------

            test('[REG-1009]: Restriction of volume with %', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('restriction_for_volume_spec_ts', 'REG-1009'));

                expect(Responses.invoice.length, 'No invoices found, billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs();
                await validateInvoice.restrictionValidator();

                test.info().attach('[REG-1009] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1165]: Restriction of volume with kWh ranges less than ranges', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('restriction_for_volume_spec_ts', 'REG-1165'));

                expect(Responses.invoice.length, 'No invoices found, billing run likely failed').toBe(0);
                await validateInvoice.checkInvoiceTabs();
                await validateInvoice.restrictionValidator();

                test.info().attach('[REG-1165] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1168]: Restriction of volume with kWh ranges more than ranges', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('restriction_for_volume_spec_ts', 'REG-1168'));

                expect(Responses.invoice.length, 'No invoices found, billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs();
                await validateInvoice.restrictionValidator();

                test.info().attach('[REG-1168] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1169]: Restriction of volume with kWh ranges between the ranges', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('restriction_for_volume_spec_ts', 'REG-1169'));

                expect(Responses.invoice.length, 'No invoices found, billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs();
                await validateInvoice.restrictionValidator();

                test.info().attach('[REG-1169] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})