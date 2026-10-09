import {test, expect} from '../../../fixtures/baseFixture';
import {randomGens} from '../../../utils/randomGens';
import reportGenerator from '../../../utils/generateReport';
import { envVariables } from '../../../fixtures/envCashed';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe.serial('[REG-558]: For volumes', () => {
            test('clean up stash files in case anything left over from previous runs', async ({clearStashedResponses}) => {
                clearStashedResponses('slp_spec_ts');
            });

            test('data preparation for REG-1008', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, saveResponsesToFile }) => {  
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('POD create', async () => {
                    const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
                    const measurementType = await Nomenclatures.measurement_type('ЕСО ЕАД', grid);
                    
                    const payload = GeneratePayload.pointsOfDelivery.pod_slp();

                    payload.gridOperatorId = await grid
                    payload.measurementTypeId = await measurementType
                    
                    const podSlp = await Request.post(Endpoints.pod, {data: payload});
                    await expect(podSlp).CheckResponse();
                    Responses.pod.push(await podSlp.json());
                })

                await test.step('Meters create', async () => {
                    const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');

                    const podGet = await Request.get(`pod/${(Responses.pod[0].id)}?versionId=1`)
                    const podJsonBody = await podGet.json();
                    const podId = podJsonBody.id
                    const gridM = podJsonBody.gridOperatorId
                    const payloadMeter = GeneratePayload.pointsOfDelivery.meters()


                    const scaleCodes = await Request.get('nomenclature/scales/filter?statuses=ACTIVE&page=0&size=25&prompt=codePlaywright')
                    let scaleCode = null;
                    for(const scale of (await scaleCodes.json()).content){
                        if(scale.name = "codePlaywright"){
                            scaleCode = scale.id
                            console.log('scale code', scale)
                            break;
                        }
                    }
                    
                    payloadMeter.gridOperatorId= await gridM
                    payloadMeter.podId = await podId
                    payloadMeter.meterScales = [scaleCode]

                    
                    const meter = await Request.post(Endpoints.meters, {data: payloadMeter})
                    await expect(meter).CheckResponse();
                    Responses.meters.push(await meter.json())

                })
                    
                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('Price parameter 15 minute', async () => {
                    const measurementName = randomGens.generateCurrentTimeStamp();
                    const payload = await GeneratePayload.productAndServices.priceParameter15minute();
                    const podget = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`)
                    const podJson = await podget.json();
                    payload.name = `${measurementName}`

                    const priceParameter = await Request.post(Endpoints.priceParameters, {data: payload});
                    expect(priceParameter).CheckResponse();
                    const priceParameterJson = await priceParameter.json();
                    Responses.priceParameters.push(priceParameterJson);

                    const measurementPayload = {
                        "name": payload.name,
                        "gridOperatorId": envVariables.grid_operator,
                        "status": "ACTIVE",
                        "defaultSelection": false
                    }

                    await Request.put(`/measurement-type/${envVariables.measurement_type}`, {data: measurementPayload})

                    await GeneratePayload.productAndServices.uploadPriceParameterFile(priceParameterJson, 'MIN');     
                })

                await test.step('Generate price component', async () => {
                    const izmereno = await Nomenclatures.profiles('Измерено количество')
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = izmereno;
                    payload.applicationModelRequest.settlementPeriodsRequest.timeZone = 'EET'

                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })
   
                await test.step('generate product', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                });

                await test.step('generate contract', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    payload.productParameters.contractType = 'COMBINED';
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate POD', async () => {
                    const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation).CheckResponse();
                });

                await test.step('Data by scales', async() => {
                    const payload = await GeneratePayload.energyData.scaleCode();
                    const scales = await Request.post(Endpoints.dataByScales, {data: payload});
                    await expect(scales).CheckResponse();
                    Responses.dataByScales.push(await scales.json());
                })

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'slp_spec_ts');
                })
                
            });

            test('data preparation for REG-1030', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, saveResponsesToFile}) => {
                 await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('POD create', async () => {
                    const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
                    const measurementType = await Nomenclatures.measurement_type('ЕСО ЕАД', grid);
                    
                    const payload = GeneratePayload.pointsOfDelivery.pod_slp();

                    payload.gridOperatorId = await grid
                    payload.measurementTypeId = await measurementType
                    
                    const podSlp = await Request.post(Endpoints.pod, {data: payload});
                    await expect(podSlp).CheckResponse();
                    Responses.pod.push(await podSlp.json());
                })

                await test.step('Meters create', async () => {
                    const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');

                    const podGet = await Request.get(`pod/${(Responses.pod[0].id)}?versionId=1`)
                    const podJsonBody = await podGet.json();
                    const podId = podJsonBody.id
                    const gridM = podJsonBody.gridOperatorId
                    const payloadMeter = GeneratePayload.pointsOfDelivery.meters()


                    const scaleCodes = await Request.get('nomenclature/scales/filter?statuses=ACTIVE&page=0&size=25&prompt=codePlaywright')
                    let scaleCode = null;
                    for(const scale of (await scaleCodes.json()).content){
                        if(scale.name = "codePlaywright"){
                            scaleCode = scale.id
                            break;
                        }
                    }
                    const scalGet = await Request.get(`scales/${scaleCode}`)
                    payloadMeter.gridOperatorId= await gridM
                    payloadMeter.podId = await podId
                    payloadMeter.meterScales = [scaleCode]

                    
                    const meter = await Request.post(Endpoints.meters, {data: payloadMeter})
                    await expect(meter).CheckResponse();
                    Responses.meters.push(await meter.json())

                })
                    
                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('Price parameter 15 minute', async () => {
                    const payload = await GeneratePayload.productAndServices.priceParameter15minute();
                    const podget = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`)
                    const podJson = await podget.json();
                    payload.name = `${podJson.podViewMeasurementType.measurementTypeName}${randomGens.generateCurrentTimeStamp()}`

                    const priceParameter = await Request.post(Endpoints.priceParameters, {data: payload});
                    expect(priceParameter).CheckResponse();
                    const priceParameterJson = await priceParameter.json();
                    Responses.priceParameters.push(priceParameterJson);

                    const measurementPayload = {
                        "name": payload.name,
                        "gridOperatorId": envVariables.grid_operator,
                        "status": "ACTIVE",
                        "defaultSelection": false
                    }

                    await Request.put(`/measurement-type/${envVariables.measurement_type}`, {data: measurementPayload})

                    await GeneratePayload.productAndServices.uploadPriceParameterFile(priceParameterJson, 'MIN');     
                })

                await test.step('Generate price component', async () => {
                    const izmereno = await Nomenclatures.profiles('Измерено количество')
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = izmereno;
                    payload.applicationModelRequest.settlementPeriodsRequest.timeZone = 'EET'
                    payload.formulaRequest.expression = '$PRICE_PROFILE$'

                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })
   
                await test.step('generate product', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                });

              await test.step('generate contract', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    payload.productParameters.contractType = 'COMBINED';
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
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
                    payload.profileId = 3;
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

                await test.step('Data by scales', async() => {
                    const payload = await GeneratePayload.energyData.scaleCode();
                    const scales = await Request.post(Endpoints.dataByScales, {data: payload});
                    await expect(scales).CheckResponse();
                    Responses.dataByScales.push(await scales.json());
                })

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'slp_spec_ts');
                })
            
            })

            test("run billings", async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('start billing generation in parallel', async() => {
                    try {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('slp_spec_ts', true)
                    } catch (e) {
                        console.warn('Some billing runs failed, subsequent tests will handle their own failures:', (e as Error).message);
                    }
                })
            })

            test('[REG-1008]: For volumes - Scale with scale code | Happy pass', async ({Request, validateInvoice, Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('slp_spec_ts');
                Object.assign(Responses, testCasesData[0]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await test.step('Data by profiles Check', async () => {
                    const podGet = await Request.get(`pod/${(Responses.pod[0].id)}?versionId=1`)
                    const podJsonBody = await podGet.json();
                    const podIdentifier = podJsonBody.identifier

                    const payload = {"page":0,"size":25,"prompt":podIdentifier}

                    const profileList = await Request.post('billing-by-profile/filter', {data: payload})

                    const profileListJson = await profileList.json();
                    expect(profileListJson.totalElements).toBeGreaterThan(0);
                })
                await test.step('validate invoice', async() => {
                    validateInvoice.checkInvoiceTabs()
                })
            })

            test('[REG-1030]: SLP with price profile', async ({Request, validateInvoice, Responses, loadResponsesFromFile}) => {
                const testCasesData = loadResponsesFromFile('slp_spec_ts');
                Object.assign(Responses, testCasesData[1]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await test.step('Data by profiles Check', async () => {
                    
                    const podGet = await Request.get(`pod/${(Responses.pod[0].id)}?versionId=1`)
                    const podJsonBody = await podGet.json();
                    const podIdentifier = podJsonBody.identifier

                    const payload = {"page":0,"size":25,"prompt":podIdentifier}

                    const profileList = await Request.post('billing-by-profile/filter', {data: payload})

                    const profileListJson = await profileList.json();
                    expect(profileListJson.totalElements).toBeGreaterThan(1);
                })
                
                await test.step('validate invoice', async() => {
                    validateInvoice.checkInvoiceTabs()
                })
            })
        })
    });
});

