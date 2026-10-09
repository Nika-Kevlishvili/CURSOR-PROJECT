
import { test, expect } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';
import { createResponsesContainer } from '../../../backend/fixtures/types/responses';
import type { ResponsesContainer } from '../../../backend/fixtures/types/responses';
import { GeneratePayload as GeneratePayloadClass } from '../../../backend/jsons/payloadGenerators/PayloadGenerator';
import reportGenerator from '../../../backend/utils/generateReport';

type DataPrepFixtures = Pick<baseFixture, 'Request' | 'Endpoints' | 'saveResponsesToFile'>;

export class interimDataPrep {
    private fixtures: DataPrepFixtures;
    private stashKey: string;
    private partTwoStashKey: string;
    private pendingResponses: Map<string, ResponsesContainer> = new Map();

    constructor(fixtures: DataPrepFixtures, stashKey: string) {
        this.fixtures = fixtures;
        this.stashKey = stashKey;
        this.partTwoStashKey = `${stashKey}_part2`;
    }

    // REG-978: Interim with From previous invoice but previous invoice missing (product contract)
    async REG_978(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for volume', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 50;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate price component for interim', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 10;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
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

        await test.step('Interim with previous invoice checkbox checked', async () => {
            const payload = GeneratePayload.productAndServices.interim();
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

        await test.step('Data by profiles', async () => {
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

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-978');

        test.info().attach('[REG-978] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1047: Interim with From previous invoice but previous invoice missing (service contract)
    async REG_1047(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for interim', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 10;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate price component for service', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 10;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
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

        await test.step('Interim with previous invoice checkbox checked', async () => {
            const payload = GeneratePayload.productAndServices.interim();
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
            Responses.service.push(await service.json());
        });

        await test.step('generate service contract', async () => {
            const contract = await Request.post(Endpoints.serviceContract, {data: await GeneratePayload.contractsAndOrders.serviceContract()});
            await expect(contract).CheckResponse();
            Responses.serviceContract.push(await contract.json());
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1047');

        test.info().attach('[REG-1047] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-716: Interim invoice exact amount | happy pass
    async REG_716(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-716');

        test.info().attach('[REG-716] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-975: interim with price component | happy pass
    async REG_975(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('Price Component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('Price Component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('Terms', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('POD', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('Interim', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.valueType = 'PRICE_COMPONENT';
            payload.value = null;
            payload.priceComponentId = Responses.priceComponent[0];
            payload.currencyId = null;
            const interim = await Request.post(Endpoints.interim, {data: payload});
            await expect(interim).CheckResponse();
            Responses.interim.push(await interim.json());
        });

        await test.step('Product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            payload.priceComponentIds = [Responses.priceComponent[1]];
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('Product Contract', async () => {
            const contract = await Request.post(Endpoints.productContract, {data: await GeneratePayload.contractsAndOrders.product_contract()});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-975');

        test.info().attach('[REG-975] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1046: From previous invoice - separate billing runs
    // Part 1: set up all entities and run the FOR_VOLUMES billing to generate the initial invoice
    async REG_1046_Part1(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
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

        await test.step('Interim', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.valueType = 'PERCENT_FROM_PREVIOUS_INVOICE_AMOUNT';
            payload.value = 100;
            const interim = await Request.post(Endpoints.interim, {data: payload});
            await expect(interim).CheckResponse();
            Responses.interim.push(await interim.json());
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

        await test.step('Data by profiles', async () => {
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

        await test.step('Billing run for volumes', async () => {
            const BillingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
            const billingRun = await Request.post(Endpoints.billingRun, {data: BillingPayload});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        this.pendingResponses.set('REG_1046', Responses);
        saveResponsesToFile(Responses, this.stashKey, 'REG-1046');

        test.info().attach('[REG-1046] Part 1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1046: From previous invoice - separate billing runs
    // Part 2: run the INTERIM billing using the invoice generated in Part 1
    async REG_1046_Part2(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = this.pendingResponses.get('REG_1046')!;
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('Billing run interim', async () => {
            const BillingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT']);
            const billingRun = await Request.post(Endpoints.billingRun, {data: BillingPayload});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.partTwoStashKey, 'REG-1046');

        test.info().attach('[REG-1046] Part 2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1154: Interim with From previous invoice but previous invoice found (product contract)
    // Part 1: set up all entities and run the FOR_VOLUMES billing to generate the previous invoice
    async REG_1154_Part1(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for volume', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 50;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate price component for interim', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 10;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
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

        await test.step('Interim with previous invoice checkbox checked', async () => {
            const payload = GeneratePayload.productAndServices.interim();
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

        await test.step('Data by profiles', async () => {
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

        await test.step('Billing run for volumes', async () => {
            const BillingPayload = await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES']);
            const billingRun = await Request.post(Endpoints.billingRun, {data: BillingPayload});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        this.pendingResponses.set('REG_1154', Responses);
        saveResponsesToFile(Responses, this.stashKey, 'REG-1154');

        test.info().attach('[REG-1154] Part 1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1154: Interim with From previous invoice but previous invoice found (product contract)
    // Part 2: run the INTERIM billing which should use the invoice generated in Part 1
    async REG_1154_Part2(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = this.pendingResponses.get('REG_1154')!;
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('Billing run interim', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.partTwoStashKey, 'REG-1154');

        test.info().attach('[REG-1154] Part 2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }
}
