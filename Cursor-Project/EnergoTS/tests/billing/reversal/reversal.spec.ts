import {test, expect} from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';
import { randomGens } from '../../../utils/randomGens';

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

            test.describe.serial('[REG-565]: Standard invoice reversal flow', () => {
                test('clean up stash files in case anything left over from previous runs', async ({clearStashedResponses}) => {
                    clearStashedResponses('reversalCases_spec_ts');
                    clearStashedResponses('reversalCases_spec_ts_part_2');
                    clearStashedResponses('reversalCases_spec_ts_part_3');
                })

                test('data preparation for REG-707 | part 1', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                    test.setTimeout(10 * 60 * 1000);

                    await test.step('generate customer', async () => {
                        const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });

                    await test.step('generate price component', async () => {
                        const payload = GeneratePayload.productAndServices.priceSettlement();
                        const price = await Request.post(Endpoints.priceComponent, {data: payload});
                        await expect(price).CheckResponse();
                        Responses.priceComponent.push(await price.json());
                    });

                    await test.step('generate term', async () => {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json());
                    });

                    await test.step('generate POD', async () => {
                        const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                        await expect(podSettlement).CheckResponse();
                        Responses.pod.push(await podSettlement.json());
                    });

                    await test.step('generate product', async () => {
                        const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                        await expect(product).CheckResponse();
                        Responses.product.push(await product.json());
                    });

                    await test.step('generate contract', async () => {
                        const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
                        await expect(contract).CheckResponse();
                        Responses.productContract.push(await contract.json());
                    });

                    await test.step('Activate POD', async () => {
                        const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                        await expect(podActivation).CheckResponse();
                    });

                    await test.step('Data by profiles', async () => {
                        const payload = await GeneratePayload.energyData.profile1Month();
                        payload.timeZone = 'CET';
                        const profiles = await Request.post('billing-by-profile', {data: payload});
                        await expect(profiles).CheckResponse();
                        const profileData = await profiles.json();
                        Responses.dataByProfiles.push({
                            id: profileData,
                            periodFrom: payload.periodFrom,
                            periodTo: payload.periodTo,
                            periodType: payload.periodType
                        });
                    });

                    await test.step('Billing run', async () => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                        await expect(billingRun).CheckResponse();
                        Responses.billingRun.push(await billingRun.json());
                    });

                    await test.step('save data for generation', async () => {
                        saveResponsesToFile(Responses, 'reversalCases_spec_ts');
                    });
                })

                test('data preparation for REG-714 | part 1', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                    test.setTimeout(10 * 60 * 1000);

                    await test.step('generate customer', async () => {
                        const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });

                    await test.step('generate price component', async () => {
                        const payload = GeneratePayload.productAndServices.priceSettlement();
                        const price = await Request.post(Endpoints.priceComponent, {data: payload});
                        await expect(price).CheckResponse();
                        Responses.priceComponent.push(await price.json());
                    });

                    await test.step('generate term', async () => {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json());
                    });

                    await test.step('generate POD', async () => {
                        const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                        await expect(podSettlement).CheckResponse();
                        Responses.pod.push(await podSettlement.json());
                    });

                    await test.step('generate product', async () => {
                        const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                        await expect(product).CheckResponse();
                        Responses.product.push(await product.json());
                    });

                    await test.step('generate contract', async () => {
                        const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
                        await expect(contract).CheckResponse();
                        Responses.productContract.push(await contract.json());
                    });

                    await test.step('Activate POD', async () => {
                        const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                        await expect(podActivation).CheckResponse();
                    });

                    await test.step('Data by profiles', async () => {
                        const payload = await GeneratePayload.energyData.profile1Month();
                        payload.timeZone = 'CET';
                        const profiles = await Request.post('billing-by-profile', {data: payload});
                        await expect(profiles).CheckResponse();
                        const profileData = await profiles.json();
                        Responses.dataByProfiles.push({
                            id: profileData,
                            periodFrom: payload.periodFrom,
                            periodTo: payload.periodTo,
                            periodType: payload.periodType
                        });
                    });

                    await test.step('Billing run', async () => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                        await expect(billingRun).CheckResponse();
                        Responses.billingRun.push(await billingRun.json());
                    });

                    await test.step('save data for generation', async () => {
                        saveResponsesToFile(Responses, 'reversalCases_spec_ts');
                    });
                })

                test('data preparation for REG-715 | part 1', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                    test.setTimeout(10 * 60 * 1000);

                    await test.step('generate customer', async () => {
                        const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });

                    await test.step('generate price component', async () => {
                        const payload = GeneratePayload.productAndServices.priceSettlement();
                        payload.formulaRequest.expression = 1;
                        const price = await Request.post(Endpoints.priceComponent, {data: payload});
                        await expect(price).CheckResponse();
                        Responses.priceComponent.push(await price.json());
                    });

                    await test.step('generate term', async () => {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json());
                    });

                    await test.step('generate POD', async () => {
                        const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                        await expect(podSettlement).CheckResponse();
                        Responses.pod.push(await podSettlement.json());
                    });

                    await test.step('generate interim', async () => {
                        const payload = GeneratePayload.productAndServices.interim();
                        payload.value = 300;
                        const interim = await Request.post(Endpoints.interim, {data: payload});
                        await expect(interim).CheckResponse();
                        Responses.interim.push(await interim.json());
                    });

                    await test.step('generate product', async () => {
                        const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                        await expect(product).CheckResponse();
                        Responses.product.push(await product.json());
                    });

                    await test.step('generate contract', async () => {
                        const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
                        await expect(contract).CheckResponse();
                        Responses.productContract.push(await contract.json());
                    });

                    await test.step('Activate POD', async () => {
                        const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                        await expect(podActivation).CheckResponse();
                    });

                    await test.step('Data by profiles', async () => {
                        const payload = await GeneratePayload.energyData.profile1Month();
                        payload.timeZone = 'CET';
                        const profiles = await Request.post('billing-by-profile', {data: payload});
                        await expect(profiles).CheckResponse();
                        const profileData = await profiles.json();
                        Responses.dataByProfiles.push({
                            id: profileData,
                            periodFrom: payload.periodFrom,
                            periodTo: payload.periodTo,
                            periodType: payload.periodType
                        });
                    });

                    await test.step('Interim Billing run', async () => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
                        await expect(billingRun).CheckResponse();
                        Responses.billingRun.push(await billingRun.json());
                    });

                    await test.step('save data for generation', async () => {
                        saveResponsesToFile(Responses, 'reversalCases_spec_ts');
                    });
                })

                test('run standard billings', async ({GeneratePayload}) => {
                    test.setTimeout(10 * 60 * 1000);
                    await test.step('start billing generation in parallel', async () => {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('reversalCases_spec_ts', true);
                    });
                })

                test('data preparation for REG-707 | part 2', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('reversalCases_spec_ts');
                    Object.assign(Responses, testCasesData[0]);

                    await test.step('reversal billing run', async () => {
                        const payload = await GeneratePayload.billing.reversalBilling();
                        const reversal = await Request.post(Endpoints.billingRun, {data: payload});
                        await expect(reversal).CheckResponse();
                        Responses.billingRun.push(await reversal.json());
                    });

                    await test.step('save data for generation', async () => {
                        saveResponsesToFile(Responses, 'reversalCases_spec_ts_part_2');
                    });
                })

                test('data preparation for REG-714 | part 2', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('reversalCases_spec_ts');
                    Object.assign(Responses, testCasesData[1]);

                    await test.step('standard invoice correction', async () => {
                        const payload = await GeneratePayload.billing.correctionBilling();
                        const correction = await Request.post(Endpoints.billingRun, {data: payload});
                        await expect(correction).CheckResponse();
                        const correctionId = await correction.json();
                        Responses.billingRun.push({id: correctionId, invoiceNumbers: 2});
                    });

                    await test.step('save data for generation', async () => {
                        saveResponsesToFile(Responses, 'reversalCases_spec_ts_part_2');
                    });
                })

                test('data preparation for REG-715 | part 2', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('reversalCases_spec_ts');
                    Object.assign(Responses, testCasesData[2]);

                    await test.step('Standard Billing run', async () => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
                        await expect(billingRun).CheckResponse();
                        Responses.billingRun.push(await billingRun.json());
                    });

                    await test.step('save data for generation', async () => {
                        saveResponsesToFile(Responses, 'reversalCases_spec_ts_part_2');
                    });
                })

                test('clean first part responses', async ({clearStashedResponses}) => {
                    clearStashedResponses('reversalCases_spec_ts');
                })

                test('run phase 2 billings', async ({GeneratePayload}) => {
                    test.setTimeout(10 * 60 * 1000);
                    await test.step('start billing generation in parallel', async () => {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('reversalCases_spec_ts_part_2', true);
                    });
                })

                test('[REG-707]: Reversal - Standard invoice reversal', async ({Request, Responses, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('reversalCases_spec_ts_part_2');
                    Object.assign(Responses, testCasesData[0]);

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    await test.step('Check reversed invoice', async () => {
                        const creditNote = await Request.get(`invoice?id=${Responses.invoice[1]}`);
                        const creditNoteBody = await creditNote.json();
                        expect(creditNoteBody.invoiceDocumentType).toBe('CREDIT_NOTE');
                    });

                    test.info().attach('[REG-707] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                })

                test('data preparation for REG-714 | part 3', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('reversalCases_spec_ts_part_2');
                    Object.assign(Responses, testCasesData[1]);

                    await test.step('Reverse standard invoice', async () => {
                        const payload = await GeneratePayload.billing.reversalBilling();
                        const reversal = await Request.post(Endpoints.billingRun, {data: payload});
                        await expect(reversal).CheckResponse();
                        const reversalId = await reversal.json();
                        Responses.billingRun.push({id: reversalId, invoiceNumbers: 3});
                    });

                    await test.step('save data for generation', async () => {
                        saveResponsesToFile(Responses, 'reversalCases_spec_ts_part_3');
                    });
                })

                test('data preparation for REG-715 | part 3', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('reversalCases_spec_ts_part_2');
                    Object.assign(Responses, testCasesData[2]);

                    await test.step('Reversal Billing run', async () => {
                        const payload = await GeneratePayload.billing.reversalBilling();
                        const getStandardInvoice = await Request.get(`invoice?id=${Responses.invoice[1]}`);
                        const standardInvoice = await getStandardInvoice.json();
                        payload.invoiceReversalParameters.listOfInvoices += standardInvoice.invoiceNumber;
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.reversalBilling()});
                        await expect(billingRun).CheckResponse();
                        const billingRunId = await billingRun.json();
                        Responses.billingRun.push({id: billingRunId, invoiceNumbers: 2});
                    });

                    await test.step('save data for generation', async () => {
                        saveResponsesToFile(Responses, 'reversalCases_spec_ts_part_3');
                    });
                })

                test('clean second part responses', async ({clearStashedResponses}) => {
                    clearStashedResponses('reversalCases_spec_ts_part_2');
                })

                test('run reversal billings', async ({GeneratePayload}) => {
                    test.setTimeout(10 * 60 * 1000);
                    await test.step('start billing generation in parallel', async () => {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('reversalCases_spec_ts_part_3', false);
                    });
                })

                test('[REG-714]: Reversal - Standard invoice reversal with correction debit/credit notes', async ({Responses, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('reversalCases_spec_ts_part_3');
                    Object.assign(Responses, testCasesData[0]);

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    test.info().attach('[REG-714] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                })

                test('[REG-715]: deducted invoice reversal', async ({Responses, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('reversalCases_spec_ts_part_3');
                    Object.assign(Responses, testCasesData[1]);

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