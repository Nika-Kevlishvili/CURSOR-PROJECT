import {test, expect} from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

const POD_COUNT = 100;

test.describe('For volumes - simple settlement many pods', {tag: '@bigData'}, () => {
    test.skip('for volumes big data', async ({Request, GeneratePayload, Responses, Endpoints}) => {
        test.setTimeout(60 * 60 * 1000);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        for (let i = 0; i < POD_COUNT; i++) {
            await test.step(`generate entity set ${i + 1}/${POD_COUNT} (price, term, pod, product, contract, activation)`, async () => {
                // Price component
                const pricePayload = GeneratePayload.productAndServices.priceSettlement();
                const price = await Request.post(Endpoints.priceComponent, {data: pricePayload});
                await expect(price).CheckResponse();
                Responses.priceComponent.push(await price.json());

                // Term
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());

                // POD
                const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                await expect(podSettlement).CheckResponse();
                Responses.pod.push(await podSettlement.json());

                // Product - link only this iteration's term and price component
                const productPayload = GeneratePayload.productAndServices.product(i);
                productPayload.productTypeId = 1046;
                productPayload.priceComponentIds = [Responses.priceComponent[i]];
                const product = await Request.post(Endpoints.product, {data: productPayload});
                await expect(product).CheckResponse();
                Responses.product.push(await product.json());

                // Contract - link only this iteration's POD
                const contract = await Request.post(Endpoints.productContract, {
                    data: await GeneratePayload.contractsAndOrders.product_contract(0, i, i)
                });
                await expect(contract).CheckResponse();
                Responses.productContract.push(await contract.json());

                // Activate this POD into its respective contract
                const podActivation = await Request.post('/contract-pods/manual', {
                    data: await GeneratePayload.pointsOfDelivery.pod_activation(i, undefined, undefined, i)
                });
                await expect(podActivation).CheckResponse();
            });
        }

        await test.step('Data by profiles for all PODs', async () => {
            for (let i = 0; i < Responses.pod.length; i++) {
                const payload = await GeneratePayload.energyData.profile1Month(i);
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
            }
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        test.info().attach('response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    });
});
