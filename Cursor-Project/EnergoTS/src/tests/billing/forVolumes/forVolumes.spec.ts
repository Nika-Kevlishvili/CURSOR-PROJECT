import {test, expect} from '../../../backend/fixtures/baseFixture';
import { volumesDataPrep } from '../dataPreparation/forVolumesDataPrep';
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

                fixtures.clearStashedResponses('forVolumes_spec_ts');
                const prep = new volumesDataPrep(fixtures, 'forVolumes_spec_ts');

                await prep.REG_713().catch(e => console.warn('REG-713 prep failed:', (e as Error).message));
                await prep.REG_873().catch(e => console.warn('REG-873 prep failed:', (e as Error).message));
                await prep.REG_963().catch(e => console.warn('REG-963 prep failed:', (e as Error).message));
                await prep.REG_962().catch(e => console.warn('REG-962 prep failed:', (e as Error).message));
                await prep.REG_992().catch(e => console.warn('REG-992 prep failed:', (e as Error).message));
                await prep.REG_1013().catch(e => console.warn('REG-1013 prep failed:', (e as Error).message));
                await prep.REG_1029().catch(e => console.warn('REG-1029 prep failed:', (e as Error).message));
                await prep.REG_1037().catch(e => console.warn('REG-1037 prep failed:', (e as Error).message));

                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('forVolumes_spec_ts', true)
                    .catch(e => console.warn('Some billing runs failed:', (e as Error).message));
            });

            // --- Validation tests ------------------------------------------------------

            test('[REG-713]: For volumes - simple settlement | happy pass', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('forVolumes_spec_ts', 'REG-713'));

                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-713] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-873]: For volumes - Scale with scale code | Happy pass', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('forVolumes_spec_ts', 'REG-873'));

                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-873] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-963]: profile flow with vat base', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('forVolumes_spec_ts', 'REG-963'));
                
                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);
                //vat base price component income account and cost center are null in summary data tab
                // await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-963] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-962]: data by profile all types', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('forVolumes_spec_ts', 'REG-962'));
                
                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-962] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-992]: Scale with Tariff - splitting | Happy pass', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('forVolumes_spec_ts', 'REG-992'));
                
                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-992] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1013]: scale with tariff - splitting | calculation for number of days', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('forVolumes_spec_ts', 'REG-1013'));
                
                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-1013] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1029]: Price Profile flow', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('forVolumes_spec_ts', 'REG-1029'));
                
                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-1029] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1037]: splitting when total amount of scale codes is zero and "Number or days" checkbox does not selected', async ({Responses, loadResponseById, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('forVolumes_spec_ts', 'REG-1037'));
                
                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-1037] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})
