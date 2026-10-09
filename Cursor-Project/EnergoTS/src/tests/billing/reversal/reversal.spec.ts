import {test, expect} from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { randomGens } from '../../../backend/utils/randomGens';
import { reversalDataPrep } from '../dataPreparation/reversalDataPrep';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-565]: Reversal', () => {
            test('[REG-706]: Reversal - Manual invoice reversal | happy pass', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                test.setTimeout(3 * 60 * 1000);
                await test.step('Create customer', async () => {
                    const createCustomer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(createCustomer).CheckResponse();
                    Responses.customer.push(await createCustomer.json());
                })

                await test.step('Create billing run for manual invoice', async () => {
                    const payload = await GeneratePayload.billing.manualInvoice();
                    payload.commonParameters.sendingAnInvoice = 'PAPER';
                    payload.commonParameters.invoiceDueDate = 'DATE'
                    payload.commonParameters.dueDate = randomGens.generateTodaysDate('yyyy-mm-dd');
                    const createManualInvoice = await Request.post(Endpoints.billingRun, {data: payload});

                    await expect(createManualInvoice).CheckResponse();
                    
                    const responseBody = await createManualInvoice.json();
                    Responses.billingRun.push(responseBody);
                })


                await test.step('make the invoice real', async () => {
                    await GeneratePayload.billing.waitForInvoiceGeneration()
                })

                await test.step('Reverse manual invoice', async () => {
                    const payload = await GeneratePayload.billing.reversalBilling()
                    const reversal =  await Request.post(Endpoints.billingRun, {data: payload});
                    Responses.billingRun.push(await reversal.json());
                    await expect(reversal).CheckResponse();
                })

                await test.step('Start reversal billing', async () => {
                    await GeneratePayload.billing.waitForInvoiceGeneration(true, false, 1, 1)
                })

                await test.step('Check reversed invoice', async () => {
                    const creditNote = await Request.get(`invoice?id=${Responses.invoice[1]}`);
                    const creditNoteBody = await creditNote.json();
                    expect(creditNoteBody.invoiceDocumentType).toBe('CREDIT_NOTE');

                })

                test.info().attach('[REG-706] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                })
            })

            test.describe('[REG-565]: Standard invoice reversal flow', () => {
                test.describe.configure({ mode: 'default' });

                test('data preparation: generate billings and wait for invoices', async ({ clearStashedResponses, GeneratePayload, saveResponsesToFile, Request, Endpoints }) => {
                    test.setTimeout(35 * 60 * 1000);
                    const fixtures = { saveResponsesToFile, Request, Endpoints };

                    clearStashedResponses('reversalCases_spec_ts');
                    clearStashedResponses('reversalCases_spec_ts_part_2');
                    clearStashedResponses('reversalCases_spec_ts_part_3');

                    const prep = new reversalDataPrep(fixtures, 'reversalCases_spec_ts');

                    // Phase 1: entity creation + initial billings
                    await prep.REG_707_Part1().catch(e => console.warn('REG-707 Part1 prep failed:', (e as Error).message));
                    await prep.REG_714_Part1().catch(e => console.warn('REG-714 Part1 prep failed:', (e as Error).message));
                    await prep.REG_715_Part1().catch(e => console.warn('REG-715 Part1 prep failed:', (e as Error).message));

                    // Wait for Phase 1 billings
                    await GeneratePayload.billing
                        .waitForInvoiceGenerationParallel('reversalCases_spec_ts', true)
                        .catch(e => console.warn('Some Phase 1 billing runs failed:', (e as Error).message));

                    // Reload invoice IDs from stash file into in-memory pendingResponses
                    // so that Part 2 methods can look up invoice numbers correctly
                    prep.reloadInvoicesFromStash('reversalCases_spec_ts');

                    // Phase 2: reversal/correction/standard billing that depends on Part 1 results
                    await prep.REG_707_Part2().catch(e => console.warn('REG-707 Part2 prep failed:', (e as Error).message));
                    await prep.REG_714_Part2().catch(e => console.warn('REG-714 Part2 prep failed:', (e as Error).message));
                    await prep.REG_715_Part2().catch(e => console.warn('REG-715 Part2 prep failed:', (e as Error).message));

                    // Clean up first part responses before phase 2 billings
                    clearStashedResponses('reversalCases_spec_ts');

                    // Wait for Phase 2 billing runs
                    await GeneratePayload.billing
                        .waitForInvoiceGenerationParallel('reversalCases_spec_ts_part_2', true)
                        .catch(e => console.warn('Some Phase 2 billing runs failed:', (e as Error).message));

                    // Reload invoice IDs from stash file into in-memory pendingResponses
                    // so that Part 3 methods can look up invoice numbers correctly
                    prep.reloadInvoicesFromStash('reversalCases_spec_ts_part_2');

                    // Phase 3: final reversals that depend on Part 2 results
                    await prep.REG_714_Part3().catch(e => console.warn('REG-714 Part3 prep failed:', (e as Error).message));
                    await prep.REG_715_Part3().catch(e => console.warn('REG-715 Part3 prep failed:', (e as Error).message));

                    // Clean up second part responses before phase 3 billings
                    clearStashedResponses('reversalCases_spec_ts_part_2');

                    // Wait for Phase 3 billing runs (parallel=false to ensure all complete)
                    await GeneratePayload.billing
                        .waitForInvoiceGenerationParallel('reversalCases_spec_ts_part_3', false)
                        .catch(e => console.warn('Some Phase 3 billing runs failed:', (e as Error).message));
                });

                test('[REG-707]: Reversal - Standard invoice reversal', async ({Request, Responses, loadResponseById}) => {
                    Object.assign(Responses, loadResponseById('reversalCases_spec_ts_part_2', 'REG-707'));
                    console.log(reportGenerator.setLinksToResponses(Responses));

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    await test.step('Check reversed invoice', async () => {
                        const creditNote = await Request.get(`invoice?id=${Responses.invoice[0]}`);
                        const creditNoteBody = await creditNote.json();
                        expect(creditNoteBody.invoiceDocumentType).toBe('CREDIT_NOTE');
                    });

                    test.info().attach('[REG-707] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                })

                test('[REG-714]: Reversal - Standard invoice reversal with correction debit/credit notes', async ({Responses, loadResponseById}) => {
                    Object.assign(Responses, loadResponseById('reversalCases_spec_ts_part_3', 'REG-714'));
                    console.log(reportGenerator.setLinksToResponses(Responses));

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    test.info().attach('[REG-714] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                })

                test('[REG-715]: deducted invoice reversal', async ({Responses, loadResponseById}) => {
                    Object.assign(Responses, loadResponseById('reversalCases_spec_ts_part_3', 'REG-715'));
                    console.log(reportGenerator.setLinksToResponses(Responses));

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    test.info().attach('[REG-715] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                })

                test('clean third part responses', async ({clearStashedResponses}) => {
                    clearStashedResponses('reversalCases_spec_ts_part_3');
                })
            })
        })
    })
})