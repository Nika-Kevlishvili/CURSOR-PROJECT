import { test, expect } from '../../../backend/fixtures/baseFixture';
import { volumesDataPrep } from '../dataPreparation/forVolumesDataPrep';
import reportGenerator from '../../../backend/utils/generateReport';
import {
    COMBINED_MATCH_STASH_ID,
    COMBINED_MISMATCH_STASH_ID,
    assertCombinedMatchInvoice,
    assertCombinedMismatchInvoice,
} from './combined.fixtures';

test.describe('[REG-55]: Billing ', { tag: '@billing' }, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-558]: For volumes', () => {
            // Run in order in a single worker, overriding fullyParallel. Prep is a leading
            // TEST (not beforeAll): a worker restarts after a test failure and re-runs
            // beforeAll hooks, but a passed test is never re-run — so this generates the
            // billing runs exactly once even when later cases fail.
            test.describe.configure({ mode: 'default' });

            test('data preparation: generate billings and wait for invoices', async ({ clearStashedResponses, GeneratePayload, Nomenclatures, saveResponsesToFile, Request, Responses, Endpoints, FileUploadRequest }) => {
                test.setTimeout(30 * 60 * 1000);
                const fixtures = { clearStashedResponses, GeneratePayload, Nomenclatures, saveResponsesToFile, Request, Responses, Endpoints, FileUploadRequest };

                fixtures.clearStashedResponses('combined_spec_ts');
                const prep = new volumesDataPrep(fixtures, 'combined_spec_ts');
                await prep.REG_974().catch(e => console.warn('REG-974 prep failed:', (e as Error).message));
                await prep.REG_1296().catch(e => console.warn('REG-1296 prep failed:', (e as Error).message));
                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('combined_spec_ts', true)
                    .catch(e => console.warn('Billing generation failed:', (e as Error).message));
            });

            test('[REG-974]: For volumes - Combined flow (scale and profile does not match)', async ({ Request, Endpoints, Responses, loadResponseById, validateInvoice }) => {
                Object.assign(Responses, loadResponseById('combined_spec_ts', COMBINED_MISMATCH_STASH_ID));
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs();
                await assertCombinedMismatchInvoice(Request, Endpoints, Responses);

                test.info().attach('[REG-974] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-1296]: For volumes - combined flow (profile and scale matches)', async ({ Request, Endpoints, Responses, loadResponseById, validateInvoice }) => {
                Object.assign(Responses, loadResponseById('combined_spec_ts', COMBINED_MATCH_STASH_ID));
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs();
                await assertCombinedMatchInvoice(Request, Endpoints, Responses);

                test.info().attach('[REG-1296] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});
