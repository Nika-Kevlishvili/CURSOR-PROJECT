import { test, expect } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';
import { createResponsesContainer } from '../../../backend/fixtures/types/responses';
import { GeneratePayload as GeneratePayloadClass } from '../../../backend/jsons/payloadGenerators/PayloadGenerator';
import reportGenerator from '../../../backend/utils/generateReport';

type DataPrepFixtures = Pick<baseFixture, 'Request' | 'Endpoints' | 'saveResponsesToFile'>;

export class overTimePeriodicalDataPrep {
    private fixtures: DataPrepFixtures;
    private stashKey: string;

    constructor(fixtures: DataPrepFixtures, stashKey: string) {
        this.fixtures = fixtures;
        this.stashKey = stashKey;
    }

    // REG-960: Periodical for Product Contract - happy pass
    async REG_960(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

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
            Responses.terms.push(await term.json());
        });

        await test.step('generate POD', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract', async () => {
            const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['OVER_TIME_PERIODICAL'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-960');

        test.info().attach('[REG-960] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-961: Periodical for Service Contract - happy pass
    async REG_961(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

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
            Responses.terms.push(await term.json());
        });

        await test.step('generate POD', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate service', async () => {
            const service = await Request.post(Endpoints.service, {data: GeneratePayload.productAndServices.service()});
            await expect(service).CheckResponse();
            Responses.service.push(await service.json());
        });

        await test.step('generate contract', async () => {
            const contract = await Request.post(Endpoints.serviceContract, {data: await GeneratePayload.contractsAndOrders.serviceContract()});
            await expect(contract).CheckResponse();
            Responses.serviceContract.push(await contract.json());
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['OVER_TIME_PERIODICAL'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-961');

        test.info().attach('[REG-961] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1294: Periodical for Product Contract - pod level
    async REG_1294(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.periodicalComponent();
            payload.applicationModelRequest.applicationLevel = 'POD'
            const price = await Request.post(Endpoints.priceComponent, {data: GeneratePayload.productAndServices.periodicalComponent()});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
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
            Responses.product.push(await product.json());
        });

        await test.step('generate contract', async () => {
            const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Activate POD 2', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1)});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['OVER_TIME_PERIODICAL'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1294');

        test.info().attach('[REG-960] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }
}
