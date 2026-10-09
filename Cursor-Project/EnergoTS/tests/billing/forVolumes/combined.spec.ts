import {test, expect} from '../../../fixtures/baseFixture';
import {randomGens} from '../../../utils/randomGens';
import reportGenerator from '../../../utils/generateReport';

test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-558]: For volumes', () => {
            test('[REG-974]: For volumes - Combined flow (scale and profile does not match)', async ({Request, GeneratePayload, Responses, Nomenclatures, Endpoints, validateInvoice}) => {  
                test.setTimeout(20 * 60 * 1000);
                await test.step('generate customer', async () => {
                    const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                    await expect(customer).CheckResponse();
                    Responses.customer.push(await customer.json());
                });

                await test.step('settlement pod', async () => {
                    const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                    await expect(podSettlement).CheckResponse();
                    Responses.pod.push(await podSettlement.json());
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
                
                await test.step('Generate price component', async () => {
                    const izmereno = await Nomenclatures.profiles('Измерено количество')
                    const payload =  GeneratePayload.productAndServices.priceSettlement();
                    payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = izmereno;

                    const price = await Request.post(Endpoints.priceComponent, {data: payload});
                    await expect(price).CheckResponse();
                    Responses.priceComponent.push(await price.json());
                })

                await test.step('term', async () => {
                    const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                    await expect(term).CheckResponse();
                    Responses.terms.push(await term.json())
                })

                await test.step('product', async () => {
                    const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                    await expect(product).CheckResponse();
                    Responses.product.push(await product.json())
                })

                await test.step('generate contract', async () => {
                    const payload = await GeneratePayload.contractsAndOrders.product_contract()
                    payload.productParameters.contractType = 'COMBINED'
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

                await test.step('Data by profiles', async() => {
                    const izmereno = await Nomenclatures.profiles('Измерено количество')
                    const payload = await GeneratePayload.energyData.profile1Month();
                    payload.profileId = izmereno

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
                    Responses.billingRun.push(await billingRun.json());
                })
    
                await test.step('invoice generation', async() => {
                    await GeneratePayload.billing.waitForInvoiceGeneration();
                })

                await test.step('validate invoice', async() => {
                    await validateInvoice.checkInvoiceTabs()
                })

                test.info().attach('[REG-974] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });
            })
        })
    })
})