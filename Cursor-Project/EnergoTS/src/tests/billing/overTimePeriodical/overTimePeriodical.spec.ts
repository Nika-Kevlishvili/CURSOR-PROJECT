import {test, expect} from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { overTimePeriodicalDataPrep } from '../dataPreparation/overTimePeriodicalDataPrep';


test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-559]: Over time Periodical', () => {
            test.describe.configure({ mode: 'default' });

            test('data preparation: generate billings and wait for invoices', async ({ clearStashedResponses, GeneratePayload, saveResponsesToFile, Request, Endpoints }) => {
                test.setTimeout(20 * 60 * 1000);
                const fixtures = { saveResponsesToFile, Request, Endpoints };

                clearStashedResponses('periodicalForProcuctContract_spec_ts');

                const prep = new overTimePeriodicalDataPrep(fixtures, 'periodicalForProcuctContract_spec_ts');

                await prep.REG_960().catch(e => console.warn('REG-960 prep failed:', (e as Error).message));
                await prep.REG_961().catch(e => console.warn('REG-961 prep failed:', (e as Error).message));
                await prep.REG_1294().catch(e => console.warn('REG-961 prep failed:', (e as Error).message));

                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('periodicalForProcuctContract_spec_ts', true)
                    .catch(e => console.warn('Some billing runs failed, subsequent tests will handle their own failures:', (e as Error).message));
            });

            test('[REG-960]: Periodical for Product Contract - happy pass', async ({validateInvoice, Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('periodicalForProcuctContract_spec_ts', 'REG-960'));
                console.log(reportGenerator.setLinksToResponses(Responses));

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-960] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-961]: Periodical for Service Contract - happy pass', async ({validateInvoice, Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('periodicalForProcuctContract_spec_ts', 'REG-961'));
                console.log(reportGenerator.setLinksToResponses(Responses));

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-961] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1294] Periodical for Product Contract - pod level', async ({validateInvoice, Responses, loadResponseById}) => {
                Object.assign(Responses, loadResponseById('periodicalForProcuctContract_spec_ts', 'REG-1294'));
                console.log(reportGenerator.setLinksToResponses(Responses));

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()

                test.info().attach('[REG-961] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})
