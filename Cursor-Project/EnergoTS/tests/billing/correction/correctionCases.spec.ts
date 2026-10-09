import {test, expect} from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-566]: Correction', () => {
            test.describe.serial('[REG-633]: Price change flow', ()=>{
                test('clean up stash files in case anything left over from previous runs', async ({clearStashedResponses}) => {
                    clearStashedResponses('correctionCases_spec_ts');
                    clearStashedResponses('correctionCases_spec_ts_part_2');
                })

                test('data preparation for REG-636 | part 1', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                    test.setTimeout(10 * 60 * 1000);

                    await test.step('generate customer', async () => {
                        const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });

                    await test.step('generate price component', async () => {
                        const payload = GeneratePayload.productAndServices.priceSettlement();
                        payload.formulaRequest.expression = '200'

                        const price = await Request.post(Endpoints.priceComponent, {data: payload});
                        await expect(price).CheckResponse();

                        Responses.priceComponent.push(await price.json());
                    });

                    await test.step('generate term', async () => {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    });

                    await test.step('generate pod 1', async () => {
                        const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                        await expect(podSettlement).CheckResponse();
                        Responses.pod.push(await podSettlement.json());
                    });

                    await test.step('generate product', async () => {
                        const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                        await expect(product).CheckResponse();
                        Responses.product.push(await product.json())
                    });

                    await test.step('generate product contract', async () => {
                        const payload = await GeneratePayload.contractsAndOrders.product_contract();
                        const contract = await Request.post(Endpoints.productContract, {data: payload});
                        await expect(contract).CheckResponse();
                        Responses.productContract.push(await contract.json())
                    })

                    await test.step('Activate POD', async () => {
                        const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(0)});
                        await expect(podActivation).CheckResponse();
                    });

                    await test.step('Data by profile for pod 1', async() => {
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

                    await test.step('Billing run', async() => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
                        await expect(billingRun).CheckResponse();
                        Responses.billingRun.push(await billingRun.json());
                    })
                    
                    await test.step('save data for generation', async() => {
                        saveResponsesToFile(Responses, 'correctionCases_spec_ts');
                    })
                })

                test('data preparation for REG-637 | part 1', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                    test.setTimeout(10 * 60 * 1000);

                    await test.step('generate customer', async () => {
                        const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });

                    await test.step('generate price component', async () => {
                        const priceComp =  GeneratePayload.productAndServices.priceSettlement();
                        priceComp.formulaRequest.expression = '200'

                        const price = await Request.post(Endpoints.priceComponent, {data: priceComp});
                        await expect(price).CheckResponse();
                        Responses.priceComponent.push(await price.json());
                    });

                    await test.step('generate term', async () => {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    });

                    await test.step('generate pod 1', async () => {
                        const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                        await expect(podSettlement).CheckResponse();
                        Responses.pod.push(await podSettlement.json());
                    });

                    await test.step('generate product', async () => {
                        const payload = GeneratePayload.productAndServices.product();
                        const postProduct = await Request.post(Endpoints.product, {data: payload});
                        await expect(postProduct).CheckResponse();
                        Responses.product.push(await postProduct.json());
                    });

                    await test.step('generate product contract', async () => {
                        const payload = await GeneratePayload.contractsAndOrders.product_contract();
                        const contract = await Request.post(Endpoints.productContract, {data: payload});
                        await expect(contract).CheckResponse();
                        Responses.productContract.push(await contract.json())
                    })

                    await test.step('Activate POD', async () => {
                        const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(0)});
                        await expect(podActivation).CheckResponse();
                    });

                    await test.step('Data by profile for pod 1', async() => {
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

                    await test.step('Billing run', async() => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
                        await expect(billingRun).CheckResponse();
                        Responses.billingRun.push(await billingRun.json());
                    })
                    
                    await test.step('save data for generation', async() => {
                        saveResponsesToFile(Responses, 'correctionCases_spec_ts');
                    })
                })

                test("run billings", async ({GeneratePayload}) => {
                    test.setTimeout(10 * 60 * 1000);
                    await test.step('start billing generation in parallel', async() => {
                        try {
                            await GeneratePayload.billing.waitForInvoiceGenerationParallel('correctionCases_spec_ts', true)
                        } catch (e) {
                            console.warn('Some billing runs failed, subsequent tests will handle their own failures:', (e as Error).message);
                        }
                    })
                })

                test('data preparation for REG-636 | part 2', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('correctionCases_spec_ts');
                    const originalPrice = testCasesData[0].priceComponent[0];

                    Object.assign(Responses.invoice, testCasesData[0].invoice);
                    Object.assign(Responses.priceComponent, testCasesData[0].priceComponent);

                    await test.step('edit price component amount', async () => {
                        await new Promise(resolve => setTimeout(resolve, 5000));
                        const priceComp = GeneratePayload.productAndServices.priceSettlement();
                        const editPrice = await Request.put(`price-components/${originalPrice}`, {data: {
                            ...priceComp,
                            formulaRequest: {
                                ...priceComp.formulaRequest,
                                expression: '300'
                            }
                        }});
                        await expect(editPrice).CheckResponse();
                    })

                    await test.step('correction billing run', async() => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.correctionBilling(0, true, false)});
                        await expect(billingRun).CheckResponse();
                        const billingRunResponse = await billingRun.json();
                        Responses.billingRun.push({
                            id: billingRunResponse,
                            invoiceNumbers: 2
                        });
                    })

                    await test.step('save data for generation', async() => {
                        saveResponsesToFile(Responses, 'correctionCases_spec_ts_part_2');
                    })
                })

                test('data preparation for REG-637 | part 2', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile, loadResponsesFromFile}) => {
                    test.setTimeout(10 * 60 * 1000);
                    const testCasesData = loadResponsesFromFile('correctionCases_spec_ts');
                    Object.assign(Responses.invoice, testCasesData[1].invoice);
                    Object.assign(Responses.product, testCasesData[1].product);
                    Object.assign(Responses.priceComponent, testCasesData[1].priceComponent);
                    Object.assign(Responses.terms, testCasesData[1].terms);
                    Object.assign(Responses.productContract, testCasesData[1].productContract);

                    await test.step('add new price component with existing slot', async () => {
                        const newPriceComp = GeneratePayload.productAndServices.priceSettlement();
                        newPriceComp.formulaRequest.expression = '500'

                        
                        const postPrice = await Request.post(Endpoints.priceComponent, {data: newPriceComp});
                        await expect(postPrice).CheckResponse();
                        const newPriceCompId = await postPrice.json();

                        const editProduct = await GeneratePayload.productAndServices.edit_Product(GeneratePayload.productAndServices.product())
                        Responses.priceComponent.push(newPriceCompId);
                        editProduct.priceComponentIds.push(newPriceCompId);

                        await new Promise(resolve => setTimeout(resolve, 5000));
                        
                        const postProduct = await Request.put(`products/${Responses.product[0]}`, {data: editProduct});
                        await expect(postProduct).CheckResponse();
                    });

                    await test.step('correction billing run', async() => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.correctionBilling(0, true, false)});
                        await expect(billingRun).CheckResponse();
                        const billingRunResponse = await billingRun.json();
                        Responses.billingRun.push({
                            id: billingRunResponse,
                            invoiceNumbers: 2
                        });
                    })
                    
                    await test.step('save data for generation', async() => {
                        saveResponsesToFile(Responses, 'correctionCases_spec_ts_part_2');
                    })
                })

                test('clean first part responses',async ({clearStashedResponses}) => {
                    clearStashedResponses('forVolumes_spec_ts');
                })

                test("run correction billings", async ({GeneratePayload}) => {
                    test.setTimeout(10 * 60 * 1000);
                    await test.step('start billing generation in parallel', async() => {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('correctionCases_spec_ts_part_2', true)
                    })
                })

                test('[REG-636]: Price change flow happy pass', async ({Request, Responses, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('correctionCases_spec_ts_part_2');
                    Object.assign(Responses, testCasesData[0]);

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    await test.step('check summary data tab', async () => {
                        const invoiceResponse = await Request.get(`invoice/summary-data?page=0&size=25&id=${Responses.invoice[1]}`);
                        const invoiceSummaryData = await invoiceResponse.json();
                        expect(invoiceSummaryData.content[0].unitPrice).toBe('300.000000000000');
                    })

                    test.info().attach('[REG-636] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                })

                test('[REG-637]: Price change flow | add new price with existing slot', async ({Request, Responses, loadResponsesFromFile}) => {
                    const testCasesData = loadResponsesFromFile('correctionCases_spec_ts_part_2');
                    Object.assign(Responses, testCasesData[1]);

                    expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                    await test.step('check summary data tab', async () => {
                        const invoiceResponse = await Request.get(`invoice/summary-data?page=0&size=25&id=${Responses.invoice[1]}`);
                        const invoiceSummaryData = await invoiceResponse.json();

                        expect(invoiceSummaryData.content.length).toBe(2)
                    })

                    test.info().attach('[REG-637] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                })

                test('clean second part responses',async ({clearStashedResponses}) => {
                    clearStashedResponses('correctionCases_spec_ts_part_2');
                })

                // test('[REG-1035]: Price change flow | add new price with different slot', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                //     test.setTimeout(10 * 60 * 1000);
                //     let product: any = null;

                //     await test.step('generate customer', async () => {
                //         const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                //         await expect(customer).CheckResponse();
                //         Responses.customer.push(await customer.json());
                //     });

                //     await test.step('generate price component', async () => {
                //         const priceComp =  GeneratePayload.productAndServices.priceSettlement();
                //         priceComp.formulaRequest.expression = '200'

                //         const price = await Request.post(Endpoints.priceComponent, {data: priceComp});
                //         await expect(price).CheckResponse();
                //         Responses.priceComponent.push(await price.json());
                //     });

                //     await test.step('generate term', async () => {
                //         const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                //         await expect(term).CheckResponse();
                //         Responses.terms.push(await term.json())
                //     });

                //     await test.step('generate pod 1', async () => {
                //         const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                //         await expect(podSettlement).CheckResponse();
                //         Responses.pod.push(await podSettlement.json());
                //     });

                //     await test.step('generate product', async () => {
                //         product = GeneratePayload.productAndServices.product()
                //         const postProduct = await Request.post(Endpoints.product, {data: product});

                //         await expect(postProduct).CheckResponse();
                //         Responses.product.push(await postProduct.json())
                //     });

                //     await test.step('generate product contract', async () => {
                //         const payload = await GeneratePayload.contractsAndOrders.product_contract();
                //         const contract = await Request.post(Endpoints.productContract, {data: payload});
                //         await expect(contract).CheckResponse();
                //         Responses.productContract.push(await contract.json())
                //     })

                //     await test.step('Activate POD', async () => {
                //         const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(0)});
                //         await expect(podActivation).CheckResponse();
                //     });

                //     await test.step('Data by profile for pod 1', async() => {
                //         const payload = await GeneratePayload.energyData.profile1Month();
                //         payload.timeZone = 'CET';

                //         const profiles = await Request.post('billing-by-profile', {data: payload});
                //         await expect(profiles).CheckResponse();
                //         const profileData = await profiles.json();
                //         Responses.dataByProfiles.push({
                //             id: profileData,
                //             periodFrom: payload.periodFrom,
                //             periodTo: payload.periodTo,
                //             periodType: payload.periodType
                //         });
                //     });

                //     await test.step('Billing run', async() => {
                //         const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
                //         await expect(billingRun).CheckResponse();
                //         Responses.billingRun.push(await billingRun.json());
                //     })
                    
                //     await test.step('invoice generation', async() => {
                //         await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1);
                //     })

                //     await test.step('add new price component with different slot', async () => {
                //         const newPriceComp = GeneratePayload.productAndServices.priceSettlement();
                //         newPriceComp.formulaRequest.expression = '500'
                //         newPriceComp.formulaRequest.issuedSeparateInvoice = 'INVOICE_TWO'

                        
                //         const postPrice = await Request.post(Endpoints.priceComponent, {data: newPriceComp});
                //         await expect(postPrice).CheckResponse();
                //         Responses.priceComponent.push(await postPrice.json());


                //         const editProduct = await GeneratePayload.productAndServices.edit_Product(product)
                //         editProduct.priceComponentIds.push(Responses.priceComponent[1]);

                //         await new Promise(resolve => setTimeout(resolve, 2000));
                        
                //         const postProduct = await Request.put(`products/${Responses.product[0]}`, {data: editProduct});
                //         await expect(postProduct).CheckResponse();
                //     });

                //     await test.step('correction billing run', async() => {
                //         const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.correctionBilling(0, true, false)});
                //         await expect(billingRun).CheckResponse();
                //         Responses.billingRun.push(await billingRun.json());
                //     })
                    
                //     await test.step('start invoice correction', async() => {
                //         await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 2, 1);
                //     })

                //     await test.step('check summary data tab', async () => {
                //         const invoiceResponse = await Request.get(`invoice/summary-data?page=0&size=25&id=${Responses.invoice[1]}`);
                //         const invoiceSummaryData = await invoiceResponse.json();
                //         console.log('invoiceSummaryData', invoiceSummaryData);

                //         expect(invoiceSummaryData.content.length).toBe(1)
                //     })

                //     test.info().attach('[REG-637] response', {
                //         body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                //         contentType: 'application/json'
                //     });
                // })
            })
            test.describe('[REG-635]: Volume change flow', ()=>{
                test('[REG-703]: Volume change flow happy pass', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                    test.setTimeout(20 * 60 * 1000);
                    await test.step('generate customer', async () => {
                        const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                        await expect(customer).CheckResponse();
                        Responses.customer.push(await customer.json());
                    });

                    await test.step('generate price component', async () => {
                        const payload =  GeneratePayload.productAndServices.priceSettlement();
                        payload.formulaRequest.expression = '200'

                        const price = await Request.post(Endpoints.priceComponent, {data: payload});
                        await expect(price).CheckResponse();
                        Responses.priceComponent.push(await price.json());
                    });

                    await test.step('generate term', async () => {
                        const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                        await expect(term).CheckResponse();
                        Responses.terms.push(await term.json())
                    });

                    await test.step('generate pod 1', async () => {
                        const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                        await expect(podSettlement).CheckResponse();
                        Responses.pod.push(await podSettlement.json());
                    });

                    await test.step('generate pod 2', async () => {
                        const podSettlement2 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                        await expect(podSettlement2).CheckResponse();
                        Responses.pod.push(await podSettlement2.json());
                    });

                    await test.step('generate product', async () => {
                        const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                        await expect(product).CheckResponse();
                        Responses.product.push(await product.json())
                    });

                    await test.step('generate product contract', async () => {
                        const payload = await GeneratePayload.contractsAndOrders.product_contract();
                        const contract = await Request.post(Endpoints.productContract, {data: payload});
                        await expect(contract).CheckResponse();
                        Responses.productContract.push(await contract.json())
                    })

                    await test.step('Activate POD', async () => {
                        const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(0)});
                        const podActivation2 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1)});
                        await expect(podActivation).CheckResponse();
                        await expect(podActivation2).CheckResponse();
                    });

                    await test.step('Data by profile for pod 1', async() => {
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

                    await test.step('Billing run', async() => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
                        await expect(billingRun).CheckResponse();
                        Responses.billingRun.push(await billingRun.json());
                    })
                    
                    await test.step('invoice generation', async() => {
                        await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1);
                    })

                    await test.step('Data by profile for pod 2', async() => {
                        const payload = await GeneratePayload.energyData.profile1Month(1);
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

                    await test.step('correction billing run', async() => {
                        const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.correctionBilling(0, false, true)});
                        await expect(billingRun).CheckResponse();
                        Responses.billingRun.push(await billingRun.json());
                    })
                    
                    await test.step('start invoice correction', async() => {
                        await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 2, 1);
                    })

                    test.info().attach('[REG-703] response', {
                        body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                        contentType: 'application/json'
                    });
                })
            })
        })
    })
})