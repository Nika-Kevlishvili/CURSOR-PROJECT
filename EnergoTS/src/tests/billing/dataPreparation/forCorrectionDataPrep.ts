
import { test, expect } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';
import { createResponsesContainer } from '../../../backend/fixtures/types/responses';
import { GeneratePayload as GeneratePayloadClass } from '../../../backend/jsons/payloadGenerators/PayloadGenerator';

type DataPrepFixtures = Pick<baseFixture, 'Request' | 'Endpoints' | 'saveResponsesToFile' | 'loadResponsesFromFile' | 'loadResponseById'>;

export class correctionDataPrep {
    private fixtures: DataPrepFixtures;
    private stashKeyPart1: string;
    private stashKeyPart2: string;

    constructor(fixtures: DataPrepFixtures, stashKeyPart1: string, stashKeyPart2: string) {
        this.fixtures = fixtures;
        this.stashKeyPart1 = stashKeyPart1;
        this.stashKeyPart2 = stashKeyPart2;
    }

    // REG-636: Price change flow - part 1 data preparation
    async REG_636_part1(): Promise<void> {
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
            payload.formulaRequest.expression = '200';

            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate pod 1', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate product contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(0)});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Data by profile for pod 1', async () => {
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKeyPart1, 'REG-636');
    }

    // REG-637: Price change flow | add new price with existing slot - part 1 data preparation
    async REG_637_part1(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const priceComp = GeneratePayload.productAndServices.priceSettlement();
            priceComp.formulaRequest.expression = '200';

            const price = await Request.post(Endpoints.priceComponent, {data: priceComp});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate pod 1', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product', async () => {
            const payload = GeneratePayload.productAndServices.product();
            const postProduct = await Request.post(Endpoints.product, {data: payload});
            await expect(postProduct).CheckResponse();
            Responses.product.push(await postProduct.json());
        });

        await test.step('generate product contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(0)});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Data by profile for pod 1', async () => {
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKeyPart1, 'REG-637');
    }

    // REG-636: Price change flow - part 2 data preparation
    async REG_636_part2(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, loadResponseById } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        const part1Data = loadResponseById(this.stashKeyPart1, 'REG-636');
        const originalPrice = part1Data.priceComponent[0];

        Object.assign(Responses.invoice, part1Data.invoice);
        Object.assign(Responses.priceComponent, part1Data.priceComponent);

        await test.step('edit price component amount', async () => {
            await new Promise(resolve => setTimeout(resolve, 5000));
            const priceComp = GeneratePayload.productAndServices.priceSettlement();
            const editPrice = await Request.put(`price-components/${originalPrice}`, {data: {
                ...priceComp,
                formulaRequest: {
                    ...priceComp.formulaRequest,
                    expression: '300'
                }
            }});
            await expect(editPrice).CheckResponse();
        });

        await test.step('correction billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.correctionBilling(0, true, false)});
            await expect(billingRun).CheckResponse();
            const billingRunResponse = await billingRun.json();
            Responses.billingRun.push({
                id: billingRunResponse,
                invoiceNumbers: 2
            });
        });

        saveResponsesToFile(Responses, this.stashKeyPart2, 'REG-636');
    }

    // REG-703: Volume change flow happy pass
    async REG_703(stashKey: string): Promise<void> {
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
            payload.formulaRequest.expression = '200';

            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate pod 1', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate pod 2', async () => {
            const podSettlement2 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement2).CheckResponse();
            Responses.pod.push(await podSettlement2.json());
        });

        await test.step('generate product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate product contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(0)});
            const podActivation2 = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(1)});
            await expect(podActivation).CheckResponse();
            await expect(podActivation2).CheckResponse();
        });

        await test.step('Data by profile for pod 1', async () => {
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1);
        });

        await test.step('Data by profile for pod 2', async () => {
            const payload = await GeneratePayload.energyData.profile1Month(1);
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

        await test.step('correction billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.correctionBilling(0, false, true)});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        await test.step('start invoice correction', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 2, 1);
        });

        saveResponsesToFile(Responses, stashKey, 'REG-703');
    }

    // REG-1035: Price change flow | add new price with different slot
    async REG_1035(stashKey: string): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);
        let product: any = null;

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const priceComp = GeneratePayload.productAndServices.priceSettlement();
            priceComp.formulaRequest.expression = '200';

            const price = await Request.post(Endpoints.priceComponent, {data: priceComp});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate pod 1', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('generate product', async () => {
            product = GeneratePayload.productAndServices.product();
            const postProduct = await Request.post(Endpoints.product, {data: product});
            await expect(postProduct).CheckResponse();
            Responses.product.push(await postProduct.json());
        });

        await test.step('generate product contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(0)});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Data by profile for pod 1', async () => {
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
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1);
        });

        await test.step('add new price component with different slot', async () => {
            const newPriceComp = GeneratePayload.productAndServices.priceSettlement();
            newPriceComp.formulaRequest.expression = '500';
            newPriceComp.formulaRequest.issuedSeparateInvoice = 'INVOICE_TWO';

            const postPrice = await Request.post(Endpoints.priceComponent, {data: newPriceComp});
            await expect(postPrice).CheckResponse();
            Responses.priceComponent.push(await postPrice.json());

            const editProduct = await GeneratePayload.productAndServices.edit_Product(product);
            editProduct.priceComponentIds.push(Responses.priceComponent[1]);

            await new Promise(resolve => setTimeout(resolve, 2000));

            const postProduct = await Request.put(`products/${Responses.product[0]}`, {data: editProduct});
            await expect(postProduct).CheckResponse();
        });

        await test.step('correction billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.correctionBilling(0, true, false)});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        await test.step('start invoice correction', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 2, 1);
        });

        saveResponsesToFile(Responses, stashKey, 'REG-1035');
    }

    // REG-637: Price change flow | add new price with existing slot - part 2 data preparation
    async REG_637_part2(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, loadResponseById } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        const part1Data = loadResponseById(this.stashKeyPart1, 'REG-637');
        Object.assign(Responses.invoice, part1Data.invoice);
        Object.assign(Responses.product, part1Data.product);
        Object.assign(Responses.priceComponent, part1Data.priceComponent);
        Object.assign(Responses.terms, part1Data.terms);
        Object.assign(Responses.productContract, part1Data.productContract);

        await test.step('add new price component with existing slot', async () => {
            const newPriceComp = GeneratePayload.productAndServices.priceSettlement();
            newPriceComp.formulaRequest.expression = '500';

            const postPrice = await Request.post(Endpoints.priceComponent, {data: newPriceComp});
            await expect(postPrice).CheckResponse();
            const newPriceCompId = await postPrice.json();

            const editProduct = await GeneratePayload.productAndServices.edit_Product(GeneratePayload.productAndServices.product());
            Responses.priceComponent.push(newPriceCompId);
            editProduct.priceComponentIds.push(newPriceCompId);

            await new Promise(resolve => setTimeout(resolve, 5000));

            const postProduct = await Request.put(`products/${Responses.product[0]}`, {data: editProduct});
            await expect(postProduct).CheckResponse();
        });

        await test.step('correction billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.correctionBilling(0, true, false)});
            await expect(billingRun).CheckResponse();
            const billingRunResponse = await billingRun.json();
            Responses.billingRun.push({
                id: billingRunResponse,
                invoiceNumbers: 2
            });
        });

        saveResponsesToFile(Responses, this.stashKeyPart2, 'REG-637');
    }
}
