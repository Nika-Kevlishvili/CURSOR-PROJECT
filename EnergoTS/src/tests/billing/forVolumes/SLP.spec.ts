import { test, expect } from '../../../backend/fixtures/baseFixture';
import { volumesDataPrep } from '../dataPreparation/forVolumesDataPrep';
import reportGenerator from '../../../backend/utils/generateReport';

test.describe('[REG-55]: Billing ', { tag: '@billing' }, () => {
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

                fixtures.clearStashedResponses('slp_spec_ts');
                const prep = new volumesDataPrep(fixtures, 'slp_spec_ts');

                await prep.REG_1008().catch(e => console.warn('REG-1008 prep failed:', (e as Error).message));
                await prep.REG_1030().catch(e => console.warn('REG-1030 prep failed:', (e as Error).message));
                await prep.REG_1221().catch(e => console.warn('REG-1221 prep failed:', (e as Error).message));

                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('slp_spec_ts', true)
                    .catch(e => console.warn('Some billing runs failed:', (e as Error).message));
            });

            test('[REG-1008]: For volumes - Scale with scale code | Happy pass', async ({ Request, Responses, loadResponseById, validateInvoice }) => {
                Object.assign(Responses, loadResponseById('slp_spec_ts', 'REG-1008'));
                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);

                await test.step('Data by profiles Check', async () => {
                    const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
                    const podJsonBody = await podGet.json();
                    const podIdentifier = podJsonBody.identifier;
                    const payload = { page: 0, size: 25, prompt: podIdentifier };
                    const profileList = await Request.post('billing-by-profile/filter', { data: payload });
                    const profileListJson = await profileList.json();
                    expect(profileListJson.totalElements).toBeGreaterThan(0);
                });

                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-1008] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-1030]: SLP with price profile', async ({ Request, Responses, loadResponseById, validateInvoice }) => {
                Object.assign(Responses, loadResponseById('slp_spec_ts', 'REG-1030'));
                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);

                await test.step('Data by profiles Check', async () => {
                    const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
                    const podJsonBody = await podGet.json();
                    const podIdentifier = podJsonBody.identifier;
                    const payload = { page: 0, size: 25, prompt: podIdentifier };
                    const profileList = await Request.post('billing-by-profile/filter', { data: payload });
                    const profileListJson = await profileList.json();
                    expect(profileListJson.totalElements).toBeGreaterThan(1);
                });

                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-1030] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });

            test('[REG-1221]: shifted hour case', async ({ Request, Responses, loadResponseById, validateInvoice }) => {
                Object.assign(Responses, loadResponseById('slp_spec_ts', 'REG-1221'));
                expect(Responses.invoice.length, 'No invoices found � billing run likely failed').toBeGreaterThan(0);

                await test.step('Data by profiles Check', async () => {
                    const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
                    const podJsonBody = await podGet.json();
                    const podIdentifier = podJsonBody.identifier;
                    const payload = { page: 0, size: 25, prompt: podIdentifier };
                    const profileList = await Request.post('billing-by-profile/filter', { data: payload });
                    const profileListJson = await profileList.json();
                    expect(profileListJson.totalElements).toBe(1);
                });

                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-1221] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });
        });
    });
});