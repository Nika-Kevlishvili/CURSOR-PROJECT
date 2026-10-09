import { test, expect } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';
import { createResponsesContainer } from '../../../backend/fixtures/types/responses';
import type { ResponsesContainer } from '../../../backend/fixtures/types/responses';
import { GeneratePayload as GeneratePayloadClass } from '../../../backend/jsons/payloadGenerators/PayloadGenerator';
import { randomGens } from '../../../backend/utils/randomGens';
import reportGenerator from '../../../backend/utils/generateReport';
import { nomenclatures } from '../../../backend/jsons/payloads/nomenclatures/nomenclatures';

type DataPrepFixtures = Pick<baseFixture, 'Request' | 'Endpoints' | 'saveResponsesToFile' | 'FileUploadRequest'> & { Nomenclatures: nomenclatures };

export class pullingDataPrep {
    private fixtures: DataPrepFixtures;
    private stashKey: string;

    constructor(fixtures: DataPrepFixtures, stashKey: string) {
        this.fixtures = fixtures;
        this.stashKey = stashKey;
    }

    // REG-965: pod has no gaps
    async REG_965(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for product 1', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 100;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate terms for products', async () => {
            for (let i = 0; i < 2; i++) {
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            }
        });

        await test.step('generate settlement pod', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 hour pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product 1 for contract 1', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate price component for product 2', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 200;
            const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price2).CheckResponse();
            Responses.priceComponent.push(await price2.json());
        });

        await test.step('generate product 2 for contract 2', async () => {
            const payload = GeneratePayload.productAndServices.product(1);
            payload.priceComponentIds = [Responses.priceComponent[1]];
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract 1', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('generate contract 2', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
            const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

            let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
            const podActivation1 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation1).CheckResponse();

            payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
            const podActivation2 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation2).CheckResponse();
        });

        await test.step('Data by profiles 1 day', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

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

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-965');

        test.info().attach('[REG-965] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-966: customer, contract, pod level
    async REG_966(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for product 1', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 100;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate terms for products', async () => {
            for (let i = 0; i < 2; i++) {
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            }
        });

        await test.step('generate settlement pod', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 hour pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product 1 for contract 1', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate price component for product 2', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 200;
            const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price2).CheckResponse();
            Responses.priceComponent.push(await price2.json());
        });

        await test.step('generate product 2 for contract 2', async () => {
            const payload = GeneratePayload.productAndServices.product(1);
            payload.priceComponentIds = [Responses.priceComponent[1]];
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract 1', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('generate settlement pod 2 for contract 2', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 month pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate contract 2', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate PODs', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
            const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

            let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
            const podActivation1 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation1).CheckResponse();

            payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
            const podActivation2 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation2).CheckResponse();

            const podActivation3 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, startDate, endDate, 1)});
            await expect(podActivation3).CheckResponse();
        });

        await test.step('Data by profiles 1 day for pod 1', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

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

        await test.step('generate data by profile for pod 2', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Month(1, [{ startDate, endDate }]);
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
            const billingRun1 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun1).CheckResponse();

            const billingRun2 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CUSTOMER')});
            await expect(billingRun2).CheckResponse();

            const billingRun3 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('POD')});
            await expect(billingRun3).CheckResponse();

            Responses.billingRun.push(await billingRun1.json());
            Responses.billingRun.push(await billingRun2.json());
            Responses.billingRun.push(await billingRun3.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-966');

        test.info().attach('[REG-966] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-967: pod has gaps
    async REG_967(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for product 1', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 100;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate terms for products', async () => {
            for (let i = 0; i < 2; i++) {
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            }
        });

        await test.step('generate settlement pod', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 hour pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product 1 for contract 1', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate price component for product 2', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 200;
            const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price2).CheckResponse();
            Responses.priceComponent.push(await price2.json());
        });

        await test.step('generate product 2 for contract 2', async () => {
            const payload = GeneratePayload.productAndServices.product(1);
            payload.priceComponentIds = [Responses.priceComponent[1]];
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract 1', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('generate contract 2', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
            const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 17, -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

            let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
            const podActivation1 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation1).CheckResponse();

            payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
            const podActivation2 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation2).CheckResponse();
        });

        await test.step('Data by profiles 1 day', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

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

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            const billingRunData = await billingRun.json();
            Responses.billingRun.push({ id: billingRunData, invoiceNumbers: 0 });
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-967');

        test.info().attach('[REG-967] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-968: one pod has gap, second pod does not
    async REG_968(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for product 1', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 100;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate terms for products', async () => {
            for (let i = 0; i < 2; i++) {
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            }
        });

        await test.step('generate settlement pod 1 for contract 1', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 hour pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate settlement pod 2 for contract 1', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 month pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product 1 for contract 1', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate price component for product 2', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 200;
            const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price2).CheckResponse();
            Responses.priceComponent.push(await price2.json());
        });

        await test.step('generate product 2 for contract 2', async () => {
            const payload = GeneratePayload.productAndServices.product(1);
            payload.priceComponentIds = [Responses.priceComponent[1]];
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract 1', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('generate contract 2', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 0);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate PODs', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
            const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 17, -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

            let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
            const podActivation1 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation1).CheckResponse();

            payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
            const podActivation2 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation2).CheckResponse();

            const podActivation3 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, startDate, endDate, 0)});
            await expect(podActivation3).CheckResponse();
        });

        await test.step('Data by profiles 1 day for pod 1', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

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

        await test.step('generate data by profile for pod 2', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Month(1, [{ startDate, endDate }]);

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
            const billingRun1 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun1).CheckResponse();
            Responses.billingRun.push(await billingRun1.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-968');

        test.info().attach('[REG-968] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-969: same POD on 2 contracts; contract 2 uses POD version 2 with SLP
    // (contract 1 keeps version 1 SETTLEMENT_PERIOD). Pulling must fail — no invoice.
    async REG_969(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest, Nomenclatures } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for product 1', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 100;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate terms for products', async () => {
            for (let i = 0; i < 2; i++) {
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            }
        });

        await test.step('generate settlement POD version 1', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 hour pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product 1 for contract 1', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate price component for product 2', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 200;
            const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price2).CheckResponse();
            Responses.priceComponent.push(await price2.json());
        });

        await test.step('generate product 2 for contract 2', async () => {
            const payload = GeneratePayload.productAndServices.product(1);
            payload.priceComponentIds = [Responses.priceComponent[1]];
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract 1 with POD version 1 (SETTLEMENT_PERIOD)', async () => {
            const version1 = await Request.get(`${Endpoints.pod}/${Responses.pod[0].id}?versionId=1`);
            await expect(version1).CheckResponse();
            const version1Json = await version1.json();
            expect(version1Json.versionId, 'Contract 1 must attach POD version 1').toBe(1);
            expect(version1Json.measurementType, 'POD version 1 must be SETTLEMENT_PERIOD').toBe('SETTLEMENT_PERIOD');
            expect(version1Json.settlementPeriod).toBe(true);
            expect(version1Json.slp).toBe(false);

            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        // Profile data must be uploaded while the POD is still SETTLEMENT_PERIOD.
        // POST billing-by-profile rejects PODs that already have an SLP version.
        await test.step('Data by profiles 1 day', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

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

        await test.step('create POD version 2 as SLP (same POD id, new podDetailId)', async () => {
            const created = Responses.pod[0];
            const version1Res = await Request.get(`${Endpoints.pod}/${created.id}?versionId=1`);
            await expect(version1Res).CheckResponse();
            const version1 = await version1Res.json();
            const measurementTypeId = await Nomenclatures.measurement_type('ЕСО ЕАД', version1.gridOperatorId);
            const slpAddress = GeneratePayload.pointsOfDelivery.pod_slp().addressRequest;

            const edited = await Request.put(`${Endpoints.pod}/${created.id}`, {
                data: {
                    name: version1.name,
                    additionalIdentifier: version1.additionalIdentifier,
                    balancingGroupCoordinatorId: version1.balancingGroupCoordinatorId,
                    type: version1.type,
                    estimatedMonthlyAvgConsumption: Math.trunc(Number(version1.estimatedMonthlyAvgConsumption)),
                    consumptionPurpose: version1.consumptionPurpose,
                    energySharingModel: version1.energySharingModel,
                    userTypeId: version1.userTypeId,
                    voltageLevel: version1.voltageLevel,
                    customerIdentifierByGridOperator: version1.customerIdentifierByGridOperator,
                    customerNumberByGridOperator: version1.customerNumberByGridOperator,
                    settlementPeriod: false,
                    slp: true,
                    measurementTypeId,
                    providedPower: version1.providedPower,
                    multiplier: version1.multiplier,
                    addressRequest: slpAddress,
                    customerIdentifier: version1.customerIdentifier,
                    impossibleToDisconnect: version1.impossibleToDisconnect,
                    blockedDisconnection: version1.blockedDisconnection,
                    blockedBilling: version1.blockedBilling,
                    blockedBillingRequest: null,
                    blockedDisconnectionRequest: null,
                    podAdditionalParameters: null,
                    updateExistingVersion: false,
                    versionId: 1
                }
            });
            await expect(edited).CheckResponse();
            const editedJson = await edited.json();

            expect(editedJson.id, 'POD id must stay the same across versions').toBe(created.id);
            expect(editedJson.versionId, 'PUT without updateExistingVersion must create version 2').toBe(2);
            expect(editedJson.podDetailId, 'Version 2 must have a new podDetailId').not.toBe(created.podDetailId);

            const version2Res = await Request.get(`${Endpoints.pod}/${created.id}?versionId=2`);
            await expect(version2Res).CheckResponse();
            const version2 = await version2Res.json();
            expect(version2.versionId).toBe(2);
            expect(version2.measurementType, 'POD version 2 must be SLP').toBe('SLP');
            expect(version2.slp).toBe(true);
            expect(version2.settlementPeriod).toBe(false);
            expect(version2.identifier, 'Same POD identifier on version 2').toBe(version1.identifier);

            Responses.pod[0] = {
                ...created,
                version1PodDetailId: created.podDetailId,
                podDetailId: editedJson.podDetailId,
                version2PodDetailId: editedJson.podDetailId,
                versionId: 2
            };
        });

        await test.step('generate contract 2 with same POD version 2 (SLP)', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
            expect(payload.productContractPointOfDeliveries[0].pointOfDeliveryDetailId).toBe(Responses.pod[0].podDetailId);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD on both contracts with no gap', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
            const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

            let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
            const podActivation1 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation1).CheckResponse();

            payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
            const podActivation2 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation2).CheckResponse();
        });

        await test.step('Billing run (expect 0 invoices — measurement type mismatch)', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push({ id: await billingRun.json(), invoiceNumbers: 0 });
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-969');

        test.info().attach('[REG-969] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-970: one chain fails another chain
    async REG_970(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for product 1', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 100;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate terms for products', async () => {
            for (let i = 0; i < 2; i++) {
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            }
        });

        await test.step('generate settlement pod', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 hour pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product 1 for contract 1', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate price component for product 2', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 200;
            const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price2).CheckResponse();
            Responses.priceComponent.push(await price2.json());
        });

        await test.step('generate product 2 for contract 2', async () => {
            const payload = GeneratePayload.productAndServices.product(1);
            payload.priceComponentIds = [Responses.priceComponent[1]];
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract 1', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('generate settlement pod 2 for contract 2', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 month pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate contract 2', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('generate contract 3', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 1);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate PODs', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
            const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

            let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
            const podActivation1 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation1).CheckResponse();

            payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
            const podActivation2 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation2).CheckResponse();

            const podActivation3 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, startDate, halfwayDate1, 1)});
            await expect(podActivation3).CheckResponse();

            const podActivation4 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 17, -1), endDate, 2)});
            await expect(podActivation4).CheckResponse();
        });

        await test.step('Data by profiles 1 day for pod 1', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

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

        await test.step('generate data by profile for pod 2', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Month(1, [{ startDate, endDate }]);

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
            const billingRun1 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun1).CheckResponse();
            const billingRunData1 = await billingRun1.json();
            Responses.billingRun.push({ id: billingRunData1, invoiceNumbers: 0 });
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-970');

        test.info().attach('[REG-970] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-971: pod has no gap (scale flow)
    async REG_971(): Promise<void> {
        const { Request, Endpoints, Nomenclatures, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('POD create', async () => {
            const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
            const measurementType = await Nomenclatures.measurement_type('ЕСО ЕАД', grid);

            const payload = GeneratePayload.pointsOfDelivery.pod_slp();
            payload.gridOperatorId = grid;
            payload.measurementTypeId = measurementType;

            const podSlp = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSlp).CheckResponse();
            Responses.pod.push(await podSlp.json());
        });

        await test.step('Meters create', async () => {
            const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');

            const podGet = await Request.get(`pod/${(Responses.pod[0].id)}?versionId=1`);
            const podJsonBody = await podGet.json();
            const podId = podJsonBody.id;
            const gridM = podJsonBody.gridOperatorId;
            const payloadMeter = GeneratePayload.pointsOfDelivery.meters();

            const scaleCode = await Nomenclatures.scales_code(grid);
            const scalGet = await Request.get(`scales/${scaleCode}`);

            payloadMeter.gridOperatorId = gridM;
            payloadMeter.podId = podId;
            payloadMeter.meterScales = [scaleCode];

            const meter = await Request.post(Endpoints.meters, {data: payloadMeter});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('generate terms for products', async () => {
            for (let i = 0; i < 2; i++) {
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            }
        });

        await test.step('scale price 1', async () => {
            const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
            const scaleCode = await Nomenclatures.scales_code(grid);

            const payload = GeneratePayload.productAndServices.scaleComponent();
            payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleCode;

            const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(priceComp).CheckResponse();
            Responses.priceComponent.push(await priceComp.json());
        });

        await test.step('generate product 1 for contract 1', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('scale price 2', async () => {
            const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
            const scaleCode = await Nomenclatures.scales_code(grid);

            const payload = GeneratePayload.productAndServices.scaleComponent();
            payload.formulaRequest.expression = 200;
            payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleCode;

            const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(priceComp).CheckResponse();
            Responses.priceComponent.push(await priceComp.json());
        });

        await test.step('generate product 2 for contract 2', async () => {
            const payload = GeneratePayload.productAndServices.product(1);
            payload.priceComponentIds = [Responses.priceComponent[1]];
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract 1', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('generate contract 2', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
            const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

            let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
            const podActivation1 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation1).CheckResponse();

            payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
            const podActivation2 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation2).CheckResponse();
        });

        await test.step('Data by scales', async () => {
            const payload = await GeneratePayload.energyData.scaleCode();
            payload.dateFrom = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            payload.dateTo = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            payload.invoiceDate = `${randomGens.generateMonthStartDate('yyyy-mm-dd', -1)}T00:00:00.000Z`;
            payload.billingByScalesTableCreateRequests[0].periodFrom = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            payload.billingByScalesTableCreateRequests[0].periodTo = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const scales = await Request.post(Endpoints.dataByScales, {data: payload});
            await expect(scales).CheckResponse();
            Responses.dataByScales.push(await scales.json());
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-971');

        test.info().attach('[REG-971] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-972: 3 contracts, 2 pulling chains
    async REG_972(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component for product 1', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 100;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate terms for products', async () => {
            for (let i = 0; i < 2; i++) {
                const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
                await expect(term).CheckResponse();
                Responses.terms.push(await term.json());
            }
        });

        await test.step('generate settlement pod', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 hour pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product 1 for contract 1', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate price component for product 2', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 200;
            const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price2).CheckResponse();
            Responses.priceComponent.push(await price2.json());
        });

        await test.step('generate product 2 for contract 2', async () => {
            const payload = GeneratePayload.productAndServices.product(1);
            payload.priceComponentIds = [Responses.priceComponent[1]];
            const product = await Request.post(Endpoints.product, {data: payload});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract 1', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('generate settlement pod 2 for contract 2', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.name = '1 month pod';
            const podSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate contract 2', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('generate contract 3', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract(0, 1, 1);
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate PODs', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const halfwayDate1 = randomGens.generateMonthHalfDate('yyyy-mm-dd', -1);
            const halfwayDate2 = randomGens.generateMonthHalfDatePlusOne('yyyy-mm-dd', 16, -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);

            let payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, startDate, halfwayDate1, 0);
            const podActivation1 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation1).CheckResponse();

            payload = await GeneratePayload.pointsOfDelivery.pod_activation(0, halfwayDate2, endDate, 1);
            const podActivation2 = await Request.post('/contract-pods/manual', {data: payload});
            await expect(podActivation2).CheckResponse();

            const podActivation3 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, startDate, halfwayDate1, 1)});
            await expect(podActivation3).CheckResponse();

            const podActivation4 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1, halfwayDate2, endDate, 2)});
            await expect(podActivation4).CheckResponse();
        });

        await test.step('Data by profiles 1 day for pod 1', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Day(0, [{ startDate: `${startDate}T00:00:00.000Z`, endDate: `${endDate}T00:00:00.000Z`, amount: 100 }]);

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

        await test.step('generate data by profile for pod 2', async () => {
            const startDate = randomGens.generateMonthStartDate('yyyy-mm-dd', -1);
            const endDate = randomGens.generateMonthEndDate('yyyy-mm-dd', -1);
            const payload = await GeneratePayload.energyData.profile1Month(1, [{ startDate, endDate }]);

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
            const billingRun1 = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun1).CheckResponse();
            const billingRunData1 = await billingRun1.json();
            Responses.billingRun.push({ id: billingRunData1, invoiceNumbers: 2 });
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-972');

        test.info().attach('[REG-972] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }
}
