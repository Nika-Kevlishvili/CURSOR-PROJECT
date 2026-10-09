import {test, expect} from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';
import { json } from 'stream/consumers';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe.serial('[REG-558]: For volumes', () => {
            test('clean up stash files in case anything left over from previous runs', async ({clearStashedResponses}) => {
                clearStashedResponses('forVolumes_spec_ts');
            });

            test('data preparation for case REG-713', async ({Request, GeneratePayload, Responses, saveResponsesToFile, Endpoints}) => {
                test.setTimeout(10 * 60 * 1000);
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
                    Responses.billingRun.push(await billingRun.json());
                })
                 
                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'forVolumes_spec_ts');
                })

                test.info().attach('[REG-713] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
                
            })
            
            test('data preparation for case REG-873', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, saveResponsesToFile}) => {  
                test.setTimeout(10 * 60 * 1000);
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


                    const scaleCode = await Nomenclatures.scales_code(grid)
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

                await test.step('generate penalty', async () => {
                    const penalty = await Request.post(Endpoints.penalty, {data: GeneratePayload.productAndServices.penalty()});
                    await expect(penalty).CheckResponse();
                    Responses.penalty.push(await penalty.json());
                });

                await test.step('generate termination', async () => {
                    const termination = await Request.post(Endpoints.termination, {data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_CONTRACT_TERM')});
                    await expect(termination).CheckResponse();
                    Responses.termination.push(await termination.json());
                });

                await test.step('generate price component with scale code', async () => {
                    const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
                    const scaleCode = await Nomenclatures.scales_code(grid)

                    const payload = GeneratePayload.productAndServices.scaleComponent();
                    payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleCode;

                    const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(priceComp).CheckResponse();
                    Responses.priceComponent.push(await priceComp.json());
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
                    saveResponsesToFile(Responses, 'forVolumes_spec_ts');
                })

                // await test.step('compare amount', async () => {
                //     let sum = 0;

                //     const getInvoice = await Request.get(`invoice?id=${Responses.invoice[0]}`);
                //     const invoiceJson = await getInvoice.json();
                  
                //     sum = await GeneratePayload.billing.amountCalculation();

                //     expect(sum).toBe(parseFloat(await invoiceJson.totalAmountIncludingVat));
                // })
                
                test.info().attach('[REG-873] response', {
                //     body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                //     contentType: 'application/json'
                // });
            })
            })

            test('data preparation for case REG-963', async ({Request, GeneratePayload, Responses, saveResponsesToFile, Endpoints}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate customer for vat base', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    
                    payload.formulaRequest.expression = 100;
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                });

                await test.step('generate vat base price component', async () => {
                    const vatBasePrice = GeneratePayload.productAndServices.priceSettlement();
                    vatBasePrice.doNotIncludeVatBase = true;
                    vatBasePrice.customerDetailId = Responses.customer[1].lastCustomerDetailId;
                    vatBasePrice.formulaRequest.expression = 50;
                    const vatBase = await Request.post(Endpoints.priceComponent, {data: vatBasePrice});

                    await expect(vatBase).CheckResponse();
                    Responses.priceComponent.push(await vatBase.json());
                });

                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });

                await test.step('generate POD 1', async () => {
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
                });

                await test.step('generate POD 2', async () => {
                    const podSettlement2 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement2).CheckResponse();
                    Responses.pod.push(await podSettlement2.json());
                });

                await test.step('generate product', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                });

                await test.step('generate contract', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate POD', async () => {
                    const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                    await expect(podActivation).CheckResponse();
                    const podActivation2 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1)});
                    await expect(podActivation).CheckResponse();
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

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                
                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'forVolumes_spec_ts');
                })

                test.info().attach('[REG-963] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('data preparation for case REG-962', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);

                const profile1 = await Nomenclatures.profiles('15 MINUTES');
                const profile2 = await Nomenclatures.profiles('1 HOUR');
                const profile3 = await Nomenclatures.profiles('1 DAY');
                const profile4 = await Nomenclatures.profiles('1 MONTH');

                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price components', async () => {

                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = 100;

                    payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = profile1
                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();

                    payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_TWO';
                    payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = profile2
                    const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price2).CheckResponse();

                    payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_THREE';
                    payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = profile3
                    const price3 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price3).CheckResponse();

                    payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_FOUR';
                    payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = profile4
                    const price4 = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price4).CheckResponse();

                    Responses.priceComponent.push(await price.json());
                    Responses.priceComponent.push(await price2.json());
                    Responses.priceComponent.push(await price3.json());
                    Responses.priceComponent.push(await price4.json());
                })

                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                })

                await test.step('generate 4 settlement pods', async () => {
                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();

                    payload.name = '15 min pod';
                    const podSettlement1 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});

                    payload.name = '1 hour pod';
                    const podSettlement2 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});

                    payload.name = '1 day pod';
                    const podSettlement3 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});

                    payload.name = '1 month pod';
                    const podSettlement4 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});

                    await expect(podSettlement1).CheckResponse();
                    await expect(podSettlement2).CheckResponse();
                    await expect(podSettlement3).CheckResponse();
                    await expect(podSettlement4).CheckResponse();

                    Responses.pod.push(await podSettlement1.json());
                    Responses.pod.push(await podSettlement2.json());
                    Responses.pod.push(await podSettlement3.json());
                    Responses.pod.push(await podSettlement4.json());

                })

                await test.step('generate product', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract();
                    const contract = await Request.post(Endpoints.productContract, {data: payload});
                    await expect(contract).CheckResponse();
                    Responses.productContract.push(await contract.json())
                });

                await test.step('Activate POD', async () => {
                    for (let i = 0; i < Responses.pod.length; i++) {
                     const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(i)});
                     await expect(podActivation).CheckResponse();
                    }
                });

                await test.step('Data by profiles 1 month', async() => {
                    const payload = await GeneratePayload.energyData.profile1Month(0);
                    payload.profileId = profile4;
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

                await test.step('Data by profiles 1 hour', async() => {
                    const payload = await GeneratePayload.energyData.profile1Hour(1);
                    payload.profileId = profile2;

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();
                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'HOUR');
                });

                await test.step('Data by profiles 1 day', async() => {
                    const payload = await GeneratePayload.energyData.profile1Day(2);
                    payload.profileId = profile3;

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();
                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'DAY');
                });

                await test.step('Data by profiles 15 min', async() => {
                    const payload = await GeneratePayload.energyData.profile15minute(3);
                    payload.profileId = profile1;

                    const profiles = await Request.post('billing-by-profile', {data: payload});
                    await expect(profiles).CheckResponse();
                    const profileData = await profiles.json();
                    Responses.dataByProfiles.push({
                        id: profileData,
                        periodFrom: payload.periodFrom,
                        periodTo: payload.periodTo,
                        periodType: payload.periodType
                    });
                    
                    await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'MIN');
                });

                await test.step('Billing run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    const billingRunResponse = await billingRun.json();
                    Responses.billingRun.push({
                        id: billingRunResponse,
                        invoiceNumbers: 4
                    });
                })
                
                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'forVolumes_spec_ts');
                })
                
                test.info().attach('[REG-962] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        
            test('data preparation for case REG-992', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(6 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('POD create', async () => {
                    const grid = await Nomenclatures.grid_operator('GIO');
                    const measurementType = await Nomenclatures.measurement_type('GIO', grid);
                    
                    const payload = GeneratePayload.pointsOfDelivery.pod_slp();

                    payload.gridOperatorId = await grid
                    payload.measurementTypeId = await measurementType
                    
                    const podSlp = await Request.post(Endpoints.pod, {data: payload});
                    await expect(podSlp).CheckResponse();
                    Responses.pod.push(await podSlp.json());
                })

                await test.step('Meters create', async () => {
                    const grid = await Nomenclatures.grid_operator('GIO');

                    const podGet = await Request.get(`pod/${(Responses.pod[0].id)}?versionId=1`)
                    const podJsonBody = await podGet.json();
                    const podId = podJsonBody.id
                    const gridM = podJsonBody.gridOperatorId
                    const payloadMeter = GeneratePayload.pointsOfDelivery.meters()


                    const scaleCode = await Nomenclatures.scales_code('scaleCode')
                    const scalGetCode = await Request.get(`scales/${scaleCode}`)
                    const scalesTariff = await Nomenclatures.scales_tariff('GIO')
                    const scalGetTariff = await Request.get(`scales/${scalesTariff}`)

                    payloadMeter.gridOperatorId= await gridM
                    payloadMeter.podId = await podId
                    payloadMeter.meterScales = [scaleCode, scalesTariff]

                    
                    const meter = await Request.post(Endpoints.meters, {data: payloadMeter})
                    await expect(meter).CheckResponse();
                    Responses.meters.push(await meter.json())

                })
                    
                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });


                await test.step('generate price component with scale code', async () => {
                    const grid = await Nomenclatures.grid_operator('GIO');
                    const scaleTariff = await Nomenclatures.scales_tariff('GIO')
                    const scaleCode = await Nomenclatures.scales_code('scaleCode')


                    const payload = GeneratePayload.productAndServices.scaleComponent();
                    payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleTariff;
                    payload.applicationModelRequest.volumesByScaleRequest.scaleIds[1] = scaleCode;

                    const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(priceComp).CheckResponse();
                    Responses.priceComponent.push(await priceComp.json());
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

                await test.step('Data by scales', async() => {
                    const payload = await GeneratePayload.energyData.scaleTariff();
              
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
                    saveResponsesToFile(Responses, 'forVolumes_spec_ts');
                })
                
                test.info().attach('[REG-992] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })

            test('data preparation for case REG-1013', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, saveResponsesToFile }) => {
                test.setTimeout(6 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('POD create', async () => {
                    const grid = await Nomenclatures.grid_operator('me');
                    const measurementType = await Nomenclatures.measurement_type('me', grid);
                    
                    const payload = GeneratePayload.pointsOfDelivery.pod_slp();

                    payload.gridOperatorId = await grid
                    payload.measurementTypeId = await measurementType
                    
                    const podSlp = await Request.post(Endpoints.pod, {data: payload});
                    await expect(podSlp).CheckResponse();
                    Responses.pod.push(await podSlp.json());
                })

                await test.step('Meters create', async () => {
                    const grid = await Nomenclatures.grid_operator('me');

                    const podGet = await Request.get(`pod/${(Responses.pod[0].id)}?versionId=1`)
                    const podJsonBody = await podGet.json();
                    const podId = podJsonBody.id
                    const gridM = podJsonBody.gridOperatorId
                    const payloadMeter = GeneratePayload.pointsOfDelivery.meters()


                    const scaleCode = await Nomenclatures.scales_code_numberOfDays('scaleCodenumberOfDays')
                    const scalGetCode = await Request.get(`scales/${scaleCode}`)
                    const scalesTariff = await Nomenclatures.scales_tariff_numberOfDays('grid')
                    const scalGetTariff = await Request.get(`scales/${scalesTariff}`)

                    payloadMeter.gridOperatorId= await gridM
                    payloadMeter.podId = await podId
                    payloadMeter.meterScales = [scaleCode, scalesTariff]

                    
                    const meter = await Request.post(Endpoints.meters, {data: payloadMeter})
                    await expect(meter).CheckResponse();
                    Responses.meters.push(await meter.json())

                })
                    
                await test.step('generate term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                });


                await test.step('generate price component with scale code', async () => {
                    const grid = await Nomenclatures.grid_operator('me');
                    const scaleTariff = await Nomenclatures.scales_tariff_numberOfDays('grid')
                    const scaleCode = await Nomenclatures.scales_code_numberOfDays('scaleCodenumberOfDays')


                    const payload = GeneratePayload.productAndServices.scaleComponent();
                    payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleTariff;
                    payload.applicationModelRequest.volumesByScaleRequest.scaleIds[1] = scaleCode;

                    const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(priceComp).CheckResponse();
                    Responses.priceComponent.push(await priceComp.json());
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

                await test.step('Data by scales', async() => {
                    const payload = await GeneratePayload.energyData.scaleTariff();
              
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
                    saveResponsesToFile(Responses, 'forVolumes_spec_ts');
                })

                
                test.info().attach('[REG-1013] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            
            })

            test('data preparation for case REG-1029', async ({Request, GeneratePayload, Responses, saveResponsesToFile, Endpoints}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('generate price component', async () => {
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.formulaRequest.expression = '$PRICE_PROFILE$'
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

                await test.step('Biiling run', async() => {
                    const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
                    await expect(billingRun).CheckResponse();
                    Responses.billingRun.push(await billingRun.json());
                })
                 
                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'forVolumes_spec_ts');
                })

                test.info().attach('[REG-1029] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
                
            })

            test('data preparation for case REG-1037' , async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                })

                await test.step('create term', async () => {
                    const term = await Request.post(Endpoints.terms, {data:GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json());
                })

                await test.step('create POD', async () => {
                    const grid = await Nomenclatures.grid_operator('splitting');

                    const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                    payload.gridOperatorId = await grid;

                    const PodSettlement = await Request.post(Endpoints.pod, {data:payload});
                    await expect(PodSettlement).CheckResponse();
                    Responses.pod.push(await PodSettlement.json());
                })

                await test.step('create meter', async () => {
                    const  scaleCode = await Nomenclatures.scales_code_withoutCheckbox('Scale splitting');
                    const  tariff = await Nomenclatures.scales_tariff_withoutCheckbox('tariff splitting');

                    const getpod = await Request.get(`pod/${(Responses.pod[0].id)}?versionId=1`);
                    const podJson = await getpod.json();
                    const podId = podJson.id
                    const gridId = podJson.gridOperatorId
                    const Meters = GeneratePayload.pointsOfDelivery.meters();

                    Meters.podId = await podId;
                    Meters.gridOperatorId = await gridId;
                    Meters.meterScales = await [scaleCode, tariff];

                    const meter = await Request.post(Endpoints.meters, {data:Meters});
                    await expect(meter).CheckResponse();
                    Responses.meters.push(await meter.json());
                })

                await test.step('create scale', async () => {
                    const scalePayload = await GeneratePayload.energyData.scaleZero();

                    const scale = await Request.post(Endpoints.dataByScales, {data: scalePayload});
                    await expect(scale).CheckResponse();
                    Responses.dataByScales.push(await scale.json());   
                })

                await test.step('create scale price component', async () => {
                    const  tariff = await Nomenclatures.scales_tariff_withoutCheckbox('tariff splitting');

                    const priceCompPayload = await GeneratePayload.productAndServices.scaleComponent();
                    priceCompPayload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = tariff;

                    const createScale = await Request.post(Endpoints.priceComponent, {data: priceCompPayload});
                    await expect(createScale).CheckResponse();
                    Responses.priceComponent.push(await createScale.json());
                })

                await test.step('create product', async () => {
                    const productPayload = await GeneratePayload.productAndServices.product();

                    const createProduct = await Request.post(Endpoints.product, {data: productPayload});
                    await expect(createProduct).CheckResponse();
                    Responses.product.push(await createProduct.json());
                })

                await test.step('create contract', async () => {
                    const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();

                    const createContract = await Request.post(Endpoints.productContract, {data: contractPayload});
                    await expect(createContract).CheckResponse();
                    Responses.productContract.push(await createContract.json());
                })

                await test.step('pod activation', async () => {
                    await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
                })

                await test.step('create billing', async () => {
                    const createBilling = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()})
                    await expect(createBilling).CheckResponse();
                    Responses.billingRun.push(await createBilling.json());
                })

                await test.step('save data for generation', async() => {
                    saveResponsesToFile(Responses, 'forVolumes_spec_ts');
                })
            })

            test("run billings", async ({Request, GeneratePayload, Responses, Endpoints, saveResponsesToFile}) => {
                test.setTimeout(10 * 60 * 1000);
                await test.step('start billing generation in parallel', async() => {
                    try {
                        await GeneratePayload.billing.waitForInvoiceGenerationParallel('forVolumes_spec_ts', true)
                    } catch (e) {
                        console.warn('Some billing runs failed, subsequent tests will handle their own failures:', (e as Error).message);
                    }
                })
            })

            test('[REG-713]: For volumes - simple settlement | happy pass', async ({Responses, loadResponsesFromFile, validateInvoice}) => {
                const testCasesData = loadResponsesFromFile('forVolumes_spec_ts');
                Object.assign(Responses, testCasesData[0]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
                
            })

            test('[REG-873]: For volumes - Scale with scale code | Happy pass', async ({Responses, loadResponsesFromFile, validateInvoice}) => {
                const testCasesData = loadResponsesFromFile('forVolumes_spec_ts');
                Object.assign(Responses, testCasesData[1]);

                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
            })

            test('[REG-963]: profile flow with vat base', async ({Responses, loadResponsesFromFile, validateInvoice}) => {
                const testCasesData = loadResponsesFromFile('forVolumes_spec_ts');
                Object.assign(Responses, testCasesData[2]);
                
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
            })

            test('[REG-962]: data by profile all types', async ({Responses, loadResponsesFromFile, validateInvoice}) => {
                const testCasesData = loadResponsesFromFile('forVolumes_spec_ts');
                Object.assign(Responses, testCasesData[3]);
                
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
            })

            test('[REG-992]: Scale with Tariff - splitting | Happy pass', async ({Responses, loadResponsesFromFile, validateInvoice}) => {
                const testCasesData = loadResponsesFromFile('forVolumes_spec_ts');
                Object.assign(Responses, testCasesData[4]);
                
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
            })

            test('[REG-1013]: scale with tariff - splitting | calculation for number of days', async ({Responses, loadResponsesFromFile, validateInvoice}) => {
                const testCasesData = loadResponsesFromFile('forVolumes_spec_ts');
                Object.assign(Responses, testCasesData[5]);
                
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
            })

            test('[REG-1029]: Price Profile flow', async ({Responses, loadResponsesFromFile, validateInvoice}) => {
                const testCasesData = loadResponsesFromFile('forVolumes_spec_ts');
                Object.assign(Responses, testCasesData[6]);
                
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
            })

            test('[REG-1037]: splitting when total amount of scale codes is zero and "Number or days" checkbox does not selected', async ({Responses, loadResponsesFromFile, validateInvoice}) => {
                const testCasesData = loadResponsesFromFile('forVolumes_spec_ts');
                Object.assign(Responses, testCasesData[7]);
                
                expect(Responses.invoice.length, 'No invoices found — billing run likely failed').toBeGreaterThan(0);
                await validateInvoice.checkInvoiceTabs()
            })
        })
    })
})