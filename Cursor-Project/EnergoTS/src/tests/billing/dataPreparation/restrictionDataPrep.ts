import { test, expect } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';
import { createResponsesContainer } from '../../../backend/fixtures/types/responses';
import { GeneratePayload as GeneratePayloadClass } from '../../../backend/jsons/payloadGenerators/PayloadGenerator';
import { randomGens } from '../../../backend/utils/randomGens';
import reportGenerator from '../../../backend/utils/generateReport';

type DataPrepFixtures = Pick<baseFixture, 'Request' | 'Endpoints' | 'Nomenclatures' | 'saveResponsesToFile' | 'FileUploadRequest'>;

export class restrictionDataPrep {
    private fixtures: DataPrepFixtures;
    private stashKey: string;

    constructor(fixtures: DataPrepFixtures, stashKey: string) {
        this.fixtures = fixtures;
        this.stashKey = stashKey;
    }

    // REG-1009: Restriction of volume with %
    async REG_1009(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.hasVolumeRestriction = true;
            payload.applicationModelRequest.settlementPeriodsRequest.kwhRestrictionPercent = randomGens.generatePercentage();
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

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1009');

        test.info().attach('[REG-1009] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1165: Restriction of volume with kWh ranges less than ranges
    async REG_1165(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage = 100;
            payload.applicationModelRequest.settlementPeriodsRequest.hasVolumeRestriction = true;
            payload.applicationModelRequest.settlementPeriodsRequest.kwhRestriction = [
                { valueFrom: 50, valueTo: 100 }
            ];
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
            payload.entries[0].value = 10;
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1165');

        test.info().attach('[REG-1165] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1168: Restriction of volume with kWh ranges more than ranges
    async REG_1168(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage = 100;
            payload.applicationModelRequest.settlementPeriodsRequest.hasVolumeRestriction = true;
            payload.applicationModelRequest.settlementPeriodsRequest.kwhRestriction = [
                {valueFrom: 50, valueTo: 100 }
            ];
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
            payload.entries[0].value = 10000;
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1168');

        test.info().attach('[REG-1168] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1169: Restriction of volume with kWh ranges between the ranges
    async REG_1169(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage = 100;
            payload.applicationModelRequest.settlementPeriodsRequest.hasVolumeRestriction = true;
            payload.applicationModelRequest.settlementPeriodsRequest.kwhRestriction = [
                { valueFrom: 1, valueTo: 100 }
            ];
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
            payload.entries[0].value = 67;
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1169');

        test.info().attach('[REG-1169] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1166: Restriction for value valid between ranges
    async REG_1166(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.hasValueRestriction = true;
            payload.applicationModelRequest.settlementPeriodsRequest.ccyRestriction[0].valueFrom = 1;
            payload.applicationModelRequest.settlementPeriodsRequest.ccyRestriction[0].valueTo = 1000;
            payload.formulaRequest.expression = '50';
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage = 100;
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
            payload.entries[0].value = 50;
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1166');

        test.info().attach('[REG-1166] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1167: Restriction for value more than ranges
    async REG_1167(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.hasValueRestriction = true;
            payload.applicationModelRequest.settlementPeriodsRequest.ccyRestriction[0].valueFrom = 1;
            payload.applicationModelRequest.settlementPeriodsRequest.ccyRestriction[0].valueTo = 100;
            payload.formulaRequest.expression = '1000000';
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

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1167');

        test.info().attach('[REG-1167] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1171: Restriction for value not valid less than ranges
    async REG_1171(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.hasValueRestriction = true;
            payload.applicationModelRequest.settlementPeriodsRequest.ccyRestriction[0].valueFrom = 10000;
            payload.applicationModelRequest.settlementPeriodsRequest.ccyRestriction[0].valueTo = 1000000;
            payload.formulaRequest.expression = '10';
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
            payload.entries[0].value = 10;
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1171');

        test.info().attach('[REG-1171] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }
}
