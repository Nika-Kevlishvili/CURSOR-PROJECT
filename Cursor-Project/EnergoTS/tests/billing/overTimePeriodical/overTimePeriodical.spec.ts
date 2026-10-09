import {test, expect} from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';


test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe.serial('[REG-559]: Over time Periodical', () => {
            test('clean up stash files in case anything left over from previous runs', async ({clearStashedResponses}) => {
                clearStashedResponses('periodicalForProcuctContract_spec_ts');
            });

            test('data preparation for case REG-960', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(6 * 60 * 1000);

                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.periodicalComponent()});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('generate POD', async () => {
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                });

                await test.step('generate product', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                });

                await test.step('generate contract', async () => {
                    const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate POD', async () => {
                    const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation).CheckResponse();
                });

                await test.step('Billing run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['OVER_TIME_PERIODICAL'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'periodicalForProcuctContract_spec_ts');
                })

                test.info().attach('[REG-960] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('data preparation for case REG-961', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(6 * 60 * 1000);

                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.periodicalComponent()});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('generate POD', async () => {
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                });

                await test.step('generate service', async () => {
                    const service = await Request.post(Endpoints.service, {data: GeneratePayload.productAndServices.service()});
                    await expect(service).CheckResponse();
                    Responses.service.push(await service.json())
                });

                await test.step('generate contract', async () => {
                    const contract = await Request.post(Endpoints.serviceContract, {data: await GeneratePayload.contractsAndOrders.serviceContract()});
                    await expect(contract).CheckResponse();
                    Responses.serviceContract.push(await contract.json())
                });

                await test.step('Billing run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['OVER_TIME_PERIODICAL'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'periodicalForProcuctContract_spec_ts');
                })

                test.info().attach('[REG-961] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('run billings', async ({GeneratePayload}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('start billing generation in parallel', async() => {
                    try {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('periodicalForProcuctContract_spec_ts', true)
                    } catch (e) {
                        console.warn('Some billing runs failed, subsequent tests will handle their own failures:', (e as Error).message);
                    }
                })
            })

            test('[REG-960]: Periodical for Product Contract - happy pass', async ({validateInvoice, Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('periodicalForProcuctContract_spec_ts');
                Object.assign(Responses, testCasesData[0]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
            })

            test('[REG-961]: Periodical for Service Contract - happy pass', async ({validateInvoice, Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('periodicalForProcuctContract_spec_ts');
                Object.assign(Responses, testCasesData[1]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
            })
        })
    })
})