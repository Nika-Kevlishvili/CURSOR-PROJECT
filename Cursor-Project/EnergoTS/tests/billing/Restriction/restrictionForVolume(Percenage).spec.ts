import { totalmem } from 'os';
import {test, expect} from '../../../fixtures/baseFixture';
import reportGenerator from '../../../utils/generateReport';
import { randomGens } from '../../../utils/randomGens';


test.describe('[REG-55]: Billing ', {tag: '@billing'}, () => {
    test.describe('[REG-106]: Billing run', () => {
        test.describe('[REG-558]: For volumes', () => {
            test('[REG-1009] Restriction of volume with %', async ({Request, GeneratePayload, Responses, Endpoints, validateInvoice}) => {

            test.setTimeout(10 * 60 * 1000);

            await test.step('generate customer', async () => {
                const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                await expect(customer).CheckResponse();
                Responses.customer.push(await customer.json());
            });

            await test.step('generate price component', async () => {
                const payload =  GeneratePayload.productAndServices.priceSettlement();
                payload.applicationModelRequest.settlementPeriodsRequest.hasVolumeRestriction = true;
                payload.applicationModelRequest.settlementPeriodsRequest.kwhRestrictionPercent = randomGens.generatePercentage();
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
              
            await test.step('invoice generation', async() => {
                await GeneratePayload.billing.waitForInvoiceGeneration();
            })

            await test.step('validate invoice', async() => {
                validateInvoice.checkInvoiceTabs()
            })

            await test.step('Check the restrictions in invoice', async () => {
                const priceComponent = await Request.get(`price-components/${Responses.priceComponent[0]}`);
                const priceComponentData = await priceComponent.json();

                const periodFrom = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString();
                const periodTo = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).toISOString();

                const profile = await Request.get(`billing-by-profile/${Responses.dataByProfiles?.[0]?.id}?periodFrom=${encodeURIComponent(periodFrom)}&periodTo=${encodeURIComponent(periodTo)}`);
                const profileJson = await profile.json();
                const profileAmount = Number(profileJson.entries?.[0]?.value ?? 0);

                const restrictionPercent = Number(priceComponentData.applicationModelResponse.volumesBySettlementPeriodResponse.volumeRestrictionPercent ?? 0);
                const profilePercent = Number(priceComponentData.applicationModelResponse.volumesBySettlementPeriodResponse.profileResponses?.[0]?.percentage ?? 0);

                const to8 = (v: number) => Number(v.toFixed(8)); // rounds to 8 and trims trailing zeros
                const expectedVolume = to8(profileAmount * (profilePercent / 100) * (restrictionPercent / 100));
                const getSummaryTab = await Request.get(`invoice/summary-data?page=0&size=25&id=${Responses.invoice}`);
                const summaryData = await getSummaryTab.json();
                const totalVolumes = to8(Number(summaryData.content?.[0]?.totalVolumes ?? 0));

                expect(expectedVolume).toBe(totalVolumes);

                test.info().attach('[REG-713] response', {
                    body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                    contentType: 'application/json'
                });

                })
            })
        })
    })
})
