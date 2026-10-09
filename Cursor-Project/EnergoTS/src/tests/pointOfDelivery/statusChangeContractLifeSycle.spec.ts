import { addYears } from 'date-fns';
import { test, expect } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';
import { randomGens } from '../../backend/utils/randomGens';
import { TIMEOUT } from 'dns';

test.describe('[REG-4]: Point Of Delivery', () => {
    test.describe('[REG-6]: Point of delivery', () => {
        test('[REG-1156]: Point of delivery status change, in one contract lifecycle', async({Request, GeneratePayload, Responses, Endpoints}) => {
            test.setTimeout(10 * 60 * 1000);
            let podDetailId: any;
            let contractPayload: any;
            let deactivationDate: any;
            let activationDate: any;

            await test.step('generate customer', async () => {
                const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
                await expect(customer).CheckResponse();
                Responses.customer.push(await customer.json());
            });

            await test.step('Create term', async () => {
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json())
            });

            await test.step('Create POD', async () => {
                const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
                await expect(podSettlement).CheckResponse();
                Responses.pod.push(await podSettlement.json());
            });

            await test.step('Create product', async () => {
                const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
                await expect(product).CheckResponse();
                Responses.product.push(await product.json())
            });

            await test.step('Create contract', async () => {
                contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
                const payload = await GeneratePayload.contractsAndOrders.product_contract();
                payload.basicParameters.status = "DRAFT";
                payload.basicParameters.subStatus = "DRAFT";
                payload.basicParameters.signingDate = null;
                const contract = await Request.post(Endpoints.productContract, {data: payload});
                await expect(contract).CheckResponse();
                Responses.productContract.push(await contract.json())
            });

            await test.step('check POD status', async() => {
                await GeneratePayload.pointsOfDelivery.podStatusCheck("POTENTIAL");
            })

            await test.step('Make contract ready', async() => {
                await GeneratePayload.contractsAndOrders.contractStatusChange("READY","READY","SIGNED",1);
            })

            await test.step('check POD status', async() => {
                await GeneratePayload.pointsOfDelivery.podStatusCheck("IN_PROCESS_OF_CONTRACTING");
            })

            await test.step('change contract status to signed', async() => {
                await GeneratePayload.contractsAndOrders.contractStatusChange("SIGNED","SIGNED_BY_BOTH_SIDES","SIGNED",1);
            })

            await test.step('check POD status', async() => {
                await GeneratePayload.pointsOfDelivery.podStatusCheck("NON_ACTIVATED");
            })

            await test.step('edit contract', async() => {
                const payload = await GeneratePayload.contractsAndOrders.edit_ProductContract(contractPayload);
                const put = await Request.put(`${Endpoints.productContract}/${Responses.productContract[0].id}?versionId=1&changeFutureVersionsPods=false`, {data: payload});
                expect(put).CheckResponse()
            })

            await test.step('activate POD with future deactivation', async() => {
                deactivationDate = randomGens.generateMonthEndDate("yyyy-mm-dd",1);
                activationDate = randomGens.generateTodaysDate("yyyy-mm-dd");

                const payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, activationDate, deactivationDate);
                const podActivation = await Request.post('/contract-pods/manual', {data: payload});
                expect(podActivation).CheckResponse();
            })

            await test.step('Check POD status and data on customer', async() => {
                const responseBody = await GeneratePayload.pointsOfDelivery.podStatusCheck("ACTIVE");
                expect(responseBody.activeUntil).toBe(deactivationDate);
            })
            
            await test.step("make POD deactivated", async() => {
                const payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, activationDate, activationDate);
                const podActivation = await Request.post('/contract-pods/manual', {data: payload});
                expect(podActivation).CheckResponse();
            })

            await test.step('check pod status and appropriate data', async() => {
                const responseBody = await GeneratePayload.pointsOfDelivery.podStatusCheck("DEACTIVATED");
                expect(responseBody.deactivatedSince).toBe(activationDate);
            })

            await test.step('make POD future actiated', async() => {
                const payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, deactivationDate, deactivationDate);
                const podActivation = await Request.post('/contract-pods/manual', {data: payload});
                expect(podActivation).CheckResponse();
            })

            await test.step('check pod status and appropriate data', async() => {
                const responseBody = await GeneratePayload.pointsOfDelivery.podStatusCheck("AWAITING_ACTIVATION");
            })

            await test.step('Terminate contract', async() => {
                await GeneratePayload.contractsAndOrders.contractStatusChange("TERMINATED","BY_MUTUAL_AGREEMENT","SIGNED",1);
            })

            await test.step('check POD status', async() => {
                await GeneratePayload.pointsOfDelivery.podStatusCheck("POTENTIAL");
            })

            await test.step('create new POD and attach customer as a sub-object', async() => {
                const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
                payload.customerIdentifier = Responses.customer[0].identifier
                const podSettlement = await Request.post(Endpoints.pod, {data: payload});
                await expect(podSettlement).CheckResponse();
                Responses.pod.push(await podSettlement.json());
            })

            await test.step('check newly added pod', async() => {
                const customerPodGet = await Request.get(`${Endpoints.customer}/pod-tab?page=0&size=25&customerId=${Responses.customer[0].id}`);
                await expect(customerPodGet).CheckResponse();
                const customerPodGetResponse = await customerPodGet.json();  
                expect(customerPodGetResponse.content).toHaveLength(2);
                const potentialPodExists = customerPodGetResponse.content.every((X: any) => X.customerPodStatus === "POTENTIAL");
                expect(potentialPodExists).toBeTruthy();
            })

            test.info().attach('[REG-1156] response', {
                body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
                contentType: 'application/json'
            })

        })
    })
})