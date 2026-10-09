import {test, expect} from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';


test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe.serial('[REG-562]: Interim and advanced payment', () => {
            test('clean up stash files in case anything left over from previous runs', async ({clearStashedResponses}) => {
                clearStashedResponses('interimCases_spec_ts');
            });

            test('[REG-1154]: Interim with From previous invoice but previous invoice found (product contract)', {tag: '@dev2'}, async ({Request, GeneratePayload, Responses, Endpoints}) => {
                test.setTimeout(12 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for volume', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = 50
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate price component for interim', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = 10
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
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

                await test.step('Interim with previous invoice checkbox checked', async () => {
                    const payload =  GeneratePayload.productAndServices.interim();
                    payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
                    payload.value = 50;
                    payload.missingInvoice = true;
                    payload.priceComponentId = Responses.priceComponent[1];

                    const interim = await Request.post(Endpoints.interim, {data: payload});
                    await expect(interim).CheckResponse();
                    Responses.interim.push(await interim.json());
                });

                await test.step('Product', async () => { 
                    const payload = GeneratePayload.productAndServices.product();
                    payload.priceComponentIds = [Responses.priceComponent[0]]; 
                    const product = await Request.post(Endpoints.product, {data: payload});
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

                await test.step('Data by profiles', async() => {
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

                await test.step('Billing run for volumes', async() => {
                    const BillingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);

                    const billingRun = await Request.post(Endpoints.billingRun, {data: BillingPayload});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                await test.step('Billing run interim', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                console.log(reportGenerator.setLinksToResponses(Responses));
                 
                // await test.step('invoice generation', async() => {
                //     await GeneratePayload.billing.waitForInvoiceGeneration();
                // })

                test.info().attach('[REG-978] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1046]: From previous invoice - separate billing runs', async ({Request, GeneratePayload, Responses, Endpoints}) => {
                test.setTimeout(15 * 60 * 1000);

                let invoiceAmount = 0;

                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
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

                await test.step('Interim', async () => {
                    const payload =  GeneratePayload.productAndServices.interim();
                    payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
                    payload.value = 100;
                    const interim = await Request.post(Endpoints.interim, {data: payload});
                    await expect(interim).CheckResponse();
                    Responses.interim.push(await interim.json());
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

                await test.step('Data by profiles', async() => {
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

                await test.step('Billing run for volumes', async() => {
                    const BillingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);

                    const billingRun = await Request.post(Endpoints.billingRun, {data: BillingPayload});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                await test.step('Billing run interim', async() => {
                    const BillingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT']);

                    const billingRun = await Request.post(Endpoints.billingRun, {data: BillingPayload});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                test.info().attach('[REG-976] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            });


            //this case needs fixing
            // test('data preparation for case REG-976', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
            //     test.setTimeout(6 * 60 * 1000);

            //     let invoiceAmount = 0;

            //     await test.step('generate customer', async () => {
            //         const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            //         await expect(customer).CheckResponse();
            //         Responses.customer.push(await customer.json());
            //     });

            //     await test.step('generate price component', async () => {
            //         const payload =  GeneratePayload.productAndServices.priceSettlement();
            //         const price = await Request.post(Endpoints.priceComponent, {data: payload});
            //         await expect(price).CheckResponse();
            //         Responses.priceComponent.push(await price.json());
            //     });

            //     await test.step('generate term', async () => {
            //         const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            //         await expect(term).CheckResponse();
            //         Responses.terms.push(await term.json())
            //     });

            //     await test.step('generate POD', async () => {
            //         const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            //         await expect(podSettlement).CheckResponse();
            //         Responses.pod.push(await podSettlement.json());
            //     });

            //     await test.step('Interim', async () => {
            //         const payload =  GeneratePayload.productAndServices.interim();
            //         payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
            //         payload.value = 100;
            //         const interim = await Request.post(Endpoints.interim, {data: payload});
            //         await expect(interim).CheckResponse();
            //         Responses.interim.push(await interim.json());
            //     });

            //     await test.step('generate product', async () => {
            //         const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            //         await expect(product).CheckResponse();
            //         Responses.product.push(await product.json())
            //     });

            //     await test.step('generate contract', async () => {
            //         const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
            //         await expect(contract).CheckResponse();
            //         Responses.productContract.push(await contract.json())
            //     });

            //     await test.step('Activate POD', async () => {
            //         const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            //         await expect(podActivation).CheckResponse();
            //     });

            //     await test.step('Data by profiles', async() => {
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
            //         const BillingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT', 'FOR_VOLUMES']);

            //         const billingRun = await Request.post(Endpoints.billingRun, {data: BillingPayload});
            //         await expect(billingRun).CheckResponse();
            //         const billingRunData = await billingRun.json();
            //         Responses.billingRun.push({ ...billingRunData, invoiceNumbers: 2 });
            //     })
                
            //     await test.step('save data for generation', async() => {
            //         saveResponsesToFile(Responses, 'interimCases_spec_ts');
            //     })

            //     test.info().attach('[REG-976] response', {
            //         body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            //         contentType: 'application/json'
            //     });
            // });

            test('data preparation for case REG-978', {tag: '@dev2'}, async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(12 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for volume', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = 50
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate price component for interim', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = 10
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
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

                await test.step('Interim with previous invoice checkbox checked', async () => {
                    const payload =  GeneratePayload.productAndServices.interim();
                    payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
                    payload.value = 100;
                    payload.missingInvoice = true;
                    payload.priceComponentId = Responses.priceComponent[1];

                    const interim = await Request.post(Endpoints.interim, {data: payload});
                    await expect(interim).CheckResponse();
                    Responses.interim.push(await interim.json());
                });

                await test.step('Product', async () => { 
                    const payload = GeneratePayload.productAndServices.product();
                    payload.priceComponentIds = [Responses.priceComponent[0]]; 
                    const product = await Request.post(Endpoints.product, {data: payload});
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

                await test.step('Data by profiles', async() => {
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
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                 
                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'interimCases_spec_ts');
                })

                test.info().attach('[REG-978] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
            
            test('data preparation for case REG-1047', {tag: '@dev2'}, async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(12 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component for interim', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = 10
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate price component for service', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = 10
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
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

                await test.step('Interim with previous invoice checkbox checked', async () => {
                    const payload =  GeneratePayload.productAndServices.interim();
                    payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
                    payload.value = 100;
                    payload.missingInvoice = true;
                    payload.priceComponentId = Responses.priceComponent[0];

                    const interim = await Request.post(Endpoints.interim, {data: payload});
                    await expect(interim).CheckResponse();
                    Responses.interim.push(await interim.json());
                });

                await test.step('create service', async () => { 
                    const payload = GeneratePayload.productAndServices.service();
                    payload.priceComponents = [Responses.priceComponent[1]];
                    const service = await Request.post(Endpoints.service, {data: payload});
                    await expect(service).CheckResponse();
                    Responses.service.push(await service.json())
                });

                await test.step('generate service contract', async () => {
                    const contract = await Request.post(Endpoints.serviceContract, {data: await GeneratePayload.contractsAndOrders.serviceContract()});
                    await expect(contract).CheckResponse();
                    Responses.serviceContract.push(await contract.json())
                });

                await test.step('Billing run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                console.log(reportGenerator.setLinksToResponses(Responses));
                 
                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'interimCases_spec_ts');
                })

            })

            test('data preparation for case REG-716', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(6 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();

                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('generate interim', async () => {
                    const interim = await Request.post(Endpoints.interim, {data: GeneratePayload.productAndServices.interim()});
                    await expect(interim).CheckResponse();
                    Responses.interim.push(await interim.json());
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

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                
                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'interimCases_spec_ts');
                })
            })

            test('data preparation for case REG-975', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(6 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('Price Component', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                 await test.step('Price Component', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('Terms', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('POD', async () => {
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                });

                await test.step('Interim', async () => {
                    const payload =  GeneratePayload.productAndServices.interim();
                    payload.valueType = 'PRICE_COMPONENT';
                    payload.value = null;
                    payload.priceComponentId = Responses.priceComponent[0];
                    payload.currencyId = null
                    const interim = await Request.post(Endpoints.interim, {data: payload});
                    await expect(interim).CheckResponse();
                    Responses.interim.push(await interim.json());

                });

                await test.step('Product', async () => {
                    const payload = GeneratePayload.productAndServices.product();
                    payload.priceComponentIds = [Responses.priceComponent[1]];
                    const product = await Request.post(Endpoints.product, {data: payload});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                });

                await test.step('Product Contract', async () => {
                    const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate POD', async () => {
                    const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation).CheckResponse();
                });

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                
                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'interimCases_spec_ts');
                })
            })

            test("run billings", async ({GeneratePayload}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('start billing generation in parallel', async() => {
                    try {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('interimCases_spec_ts', true)
                    } catch (e) {
                        console.warn('Some billing runs failed, subsequent tests will handle their own failures:', (e as Error).message);
                    }
                })
            })

            // test('[REG-976]: From previous invoice - happy pass', async ({Responses, loadResponsesFromFile}) => {
            //     const testCasesData = loadResponsesFromFile('interimCases_spec_ts');
            //     Object.assign(Responses, testCasesData[0]);

            //     test.info().attach('[REG-976] response', {
            //         body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            //         contentType: 'application/json'
            //     });
            // })

            test('[REG-978]: Interim with From previous invoice but previous invoice missing (product contract)', async ({Request, GeneratePayload, Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('interimCases_spec_ts');
                Object.assign(Responses, testCasesData[1]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-978] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1047]: Interim with From previous invoice but previous invoice missing (service contract)', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('interimCases_spec_ts');
                Object.assign(Responses, testCasesData[2]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1047] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-716]: Interim invoice exact amount | happy pass', async ({Request, GeneratePayload, Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('interimCases_spec_ts');
                Object.assign(Responses, testCasesData[3]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                // await test.step('compare amount', async () => {
                //     const getInvoice = await Request.get(`invoice?id=${Responses.invoice[0]}`);
                //     await expect(getInvoice).CheckResponse();
                //     const invoiceJson = await getInvoice.json();
                //     let sumAmount = 0;
                //     sumAmount = await GeneratePayload.billing.interimInvoiceCalculation();
                //     expect(sumAmount).toBe(parseFloat(invoiceJson.totalAmountIncludingVat));
                // })
            })

            test('[REG-975]: interim with price component | happy pass', async ({Request, GeneratePayload, Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('interimCases_spec_ts');
                Object.assign(Responses, testCasesData[4]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                // await test.step('compare amount', async () => {
                //     const invoiceId = (Responses.invoice?.[0] as any)?.id ?? Responses.invoice?.[0];
                //     const getInvoice = await Request.get(`invoice?id=${invoiceId}`);
                //     await expect(getInvoice).CheckResponse();
                //     const invoiceJson = await getInvoice.json();
                //     let sumAmount = 0;
                //     sumAmount = await GeneratePayload.billing.interimInvoiceCalculation();
                //     expect(sumAmount).toBe(parseFloat(invoiceJson.totalAmountIncludingVat));
                // })
            })
        });
    });
});
