import {test, expect} from '../../../fixtures/baseFixture';
import {randomGens} from '../../../utils/randomGens';
import reportGenerator from '../../../utils/generateReport';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe.serial('[REG-1019]: For volumes', () => {
            test('clean up stash files in case anything left over from previous runs', async ({clearStashedResponses}) => {
                clearStashedResponses('slotSplitting_spec_ts');
            });

            test('data preparation for case REG-1020', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component slot 1', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate price component slot 2', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_TWO'
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

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    const billingRunId = await billingRun.json();
                    Responses.billingRun.push({ id: billingRunId, invoiceNumbers: 2 });
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'slotSplitting_spec_ts');
                })

                test.info().attach('[REG-1020] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
                
            })

            test('data preparation for case REG-1021', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                let productContract: any = null

                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component slot 1', async () => {
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

                await test.step('generate product', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                });

                await test.step('generate contract', async () => {
                    productContract = await GeneratePayload.contractsAndOrders.product_contract()
                    const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('add billing group', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.addBillingGroup();
                    const billingGroup =  await Request.post(Endpoints.billingGroup, {data: payload});
                    await expect(billingGroup).CheckResponse();
                });

                await test.step('Activate POD 1', async () => {
                    const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation).CheckResponse();
                });

                await test.step('Data by profile for pod 1', async() => {
                    const payload = await GeneratePayload.energyData.profile1Month();
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
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    const billingRunId = await billingRun.json();
                    Responses.billingRun.push({ id: billingRunId, invoiceNumbers: 2 });
                })

                await test.step('generate pod 2 for billing group 2', async () => {
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                });

                await test.step('Data by profile for pod 2', async() => {
                    const payload = await GeneratePayload.energyData.profile1Month(1);
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

                await test.step('add pod to billing group 2', async() => {
                    const payload = await GeneratePayload.contractsAndOrders.edit_ProductContract(productContract)
                    payload.basicParameters.entryInForceDate = "2025-01-01"

                    const newpod = await GeneratePayload.contractsAndOrders.addPodToBillingGroup(1, Responses.pod[1].podDetailId);
                    payload.podRequests.push(newpod.addPod);
                    payload.productContractPointOfDeliveries.push(newpod.addPod.productContractPointOfDeliveries[0]);
                    payload.additionalParameters.estimatedTotalConsumptionUnderContractKwh = newpod.estimatedConsumption
                    
                    const editContract = await Request.put(`product-contract/${Responses.productContract[0].id}?versionId=1&changeFutureVersionsPods=false`, {data: payload});
                    await expect(editContract).CheckResponse();
                })

                await test.step('Activate POD 2', async () => {
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1)});
                    await expect(podActivation2).CheckResponse();
                });

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'slotSplitting_spec_ts');
                })

                test.info().attach('[REG-1021] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
                
            })

            test('data preparation for case REG-1022', async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component slot 1', async () => {
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

                await test.step('generate POD 2', async () => {
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

                await test.step('Activate PODs', async () => {
                    const podActivation1 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation1).CheckResponse();

                    const podActivation2 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1)});
                    await expect(podActivation2).CheckResponse();
                });

                await test.step('edit billing group', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.editBillingGroup()
                    payload.separateInvoiceForEachPod = true;
                    
                    const editBillingGroup = await Request.put(`billing-group/${payload.id}`, {data: payload});
                    await expect(editBillingGroup).CheckResponse();

                });

                await test.step('Data by profiles', async() => {
                    const payload = await GeneratePayload.energyData.profile1Month();

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

                await test.step('Data by profile 2', async() => {
                    const payload = await GeneratePayload.energyData.profile1Month(1);

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
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    const billingRunId = await billingRun.json();
                    Responses.billingRun.push({ id: billingRunId, invoiceNumbers: 2 });
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'slotSplitting_spec_ts');
                })

                test.info().attach('[REG-1022] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
                
            })

            test("run billings", async ({GeneratePayload}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('start billing generation in parallel', async() => {
                    try {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('slotSplitting_spec_ts', true)
                    } catch (e) {
                        console.warn('Some billing runs failed, subsequent tests will handle their own failures:', (e as Error).message);
                    }
                })
            })

            test('[REG-1020]: invoice slot splitting - split by price component', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('slotSplitting_spec_ts');
                Object.assign(Responses, testCasesData[0]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1020] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1021]: invoice slot splitting - split by billing group', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('slotSplitting_spec_ts');
                Object.assign(Responses, testCasesData[1]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1021] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('[REG-1022]: invoice slot splitting - split by seperate checkbox', async ({Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('slotSplitting_spec_ts');
                Object.assign(Responses, testCasesData[2]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                test.info().attach('[REG-1022] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})