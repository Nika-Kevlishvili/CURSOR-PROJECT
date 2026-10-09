import {test} from '../../../backend/fixtures/baseFixture';
import reportGenerator from '../../../backend/utils/generateReport';
import { pullingDataPrep } from '../dataPreparation/pullingDataPrep';
import {
    assertNoInvoicesGenerated,
    assertPulledIntoLatestContract,
    assertCustomerContractPodLevelPulling,
    assertInvoiceForPodsOnly,
    assertTwoPullingChains,
    assertPodVersionsHaveDifferentMeasurementTypes,
    runPullingCheck,
} from './pulling.fixtures';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-964]: Pulling', () => {
            // Run in order in a single worker, overriding fullyParallel. Prep is a leading
            // TEST (not beforeAll): a worker restarts after a test failure and re-runs
            // beforeAll hooks, but a passed test is never re-run — so this generates the
            // billing runs exactly once even when later cases fail.
            test.describe.configure({ mode: 'default' });

            test('data preparation: generate billings and wait for invoices', async ({ clearStashedResponses, GeneratePayload, saveResponsesToFile, Request, Endpoints, Nomenclatures, FileUploadRequest }) => {
                test.setTimeout(35 * 60 * 1000);
                const fixtures = { saveResponsesToFile, Request, Endpoints, Nomenclatures, FileUploadRequest };

                clearStashedResponses('pulling_spec_ts');

                const prep = new pullingDataPrep(fixtures, 'pulling_spec_ts');

                await prep.REG_965().catch(e => console.warn('REG-965 prep failed:', (e as Error).message));
                await prep.REG_966().catch(e => console.warn('REG-966 prep failed:', (e as Error).message));
                await prep.REG_967().catch(e => console.warn('REG-967 prep failed:', (e as Error).message));
                await prep.REG_968().catch(e => console.warn('REG-968 prep failed:', (e as Error).message));
                await prep.REG_969().catch(e => console.warn('REG-969 prep failed:', (e as Error).message));
                await prep.REG_970().catch(e => console.warn('REG-970 prep failed:', (e as Error).message));
                await prep.REG_971().catch(e => console.warn('REG-971 prep failed:', (e as Error).message));
                await prep.REG_972().catch(e => console.warn('REG-972 prep failed:', (e as Error).message));

                await GeneratePayload.billing
                    .waitForInvoiceGenerationParallel('pulling_spec_ts', true)
                    .catch(e => console.warn('Some billing runs failed:', (e as Error).message));
            });

            test('[REG-965]: pod has no gaps', async ({Responses, loadResponseById, Request, Endpoints, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('pulling_spec_ts', 'REG-965'));
                const ctx = { Request, Endpoints, Responses };

                await runPullingCheck('Invoice is on contract 2 with contract 2 price', async () => {
                    await assertPulledIntoLatestContract(ctx);
                });
                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-965] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-966]: customer, contract, pod level', async ({Responses, loadResponseById, Request, Endpoints, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('pulling_spec_ts', 'REG-966'));
                const ctx = { Request, Endpoints, Responses };

                await runPullingCheck('Customer/contract invoice all pulled-to PODs; POD level only the selected POD', async () => {
                    await assertCustomerContractPodLevelPulling(ctx);
                });
                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-966] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-967]: pod has gaps', async ({Responses, loadResponseById, Request, Endpoints}) => {
                Object.assign(Responses, loadResponseById('pulling_spec_ts', 'REG-967'));
                const ctx = { Request, Endpoints, Responses };

                await runPullingCheck('Gap in POD activation → no invoice', async () => {
                    await assertNoInvoicesGenerated(ctx, 'REG-967: pulling must fail when the POD has a 1-day activation gap');
                });

                test.info().attach('[REG-967] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-968]: one pod has gap, second pod does not', async ({Responses, loadResponseById, Request, Endpoints, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('pulling_spec_ts', 'REG-968'));
                const ctx = { Request, Endpoints, Responses };

                await runPullingCheck('Invoice only for the POD without a gap (POD 2)', async () => {
                    await assertInvoiceForPodsOnly(ctx, {
                        expectedInvoiceCount: 1,
                        includedPodIndexes: [1],
                        excludedPodIndexes: [0],
                        contractIndex: 0,
                    });
                });
                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-968] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-969]: pods have different measurement type', async ({Responses, loadResponseById, Request, Endpoints}) => {
                Object.assign(Responses, loadResponseById('pulling_spec_ts', 'REG-969'));
                const ctx = { Request, Endpoints, Responses };

                await runPullingCheck('Same POD: contract 1 = SETTLEMENT_PERIOD v1, contract 2 = SLP v2', async () => {
                    await assertPodVersionsHaveDifferentMeasurementTypes(ctx);
                });
                await runPullingCheck('Different measurement types → pulling fails, no invoice', async () => {
                    await assertNoInvoicesGenerated(
                        ctx,
                        'REG-969: pulling must fail when the same POD has different measurement types across contracts'
                    );
                });

                test.info().attach('[REG-969] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-970]: one chain fails another chain', async ({Responses, loadResponseById, Request, Endpoints}) => {
                Object.assign(Responses, loadResponseById('pulling_spec_ts', 'REG-970'));
                const ctx = { Request, Endpoints, Responses };

                await runPullingCheck('Failed pulling chain fails the whole billing run', async () => {
                    await assertNoInvoicesGenerated(
                        ctx,
                        'REG-970: if one POD pulling chain fails, no invoice may be generated for the other chain'
                    );
                });

                test.info().attach('[REG-970] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-971]: pod has no gap (scale flow)', async ({Responses, loadResponseById, Request, Endpoints, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('pulling_spec_ts', 'REG-971'));
                const ctx = { Request, Endpoints, Responses };

                await runPullingCheck('Scale flow: invoice on contract 2 with contract 2 price', async () => {
                    await assertPulledIntoLatestContract(ctx);
                });
                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-971] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-972]: 3 contracts, 2 pulling chains', async ({Responses, loadResponseById, Request, Endpoints, validateInvoice}) => {
                Object.assign(Responses, loadResponseById('pulling_spec_ts', 'REG-972'));
                const ctx = { Request, Endpoints, Responses };

                await runPullingCheck('POD 1 invoiced on contract 2; POD 2 invoiced on contract 3', async () => {
                    await assertTwoPullingChains(ctx);
                });
                await validateInvoice.checkInvoiceTabs();

                test.info().attach('[REG-972] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})