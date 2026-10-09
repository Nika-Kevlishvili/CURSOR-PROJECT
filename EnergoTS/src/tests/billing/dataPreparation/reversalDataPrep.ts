import { test, expect } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';
import { createResponsesContainer } from '../../../backend/fixtures/types/responses';
import type { ResponsesContainer } from '../../../backend/fixtures/types/responses';
import { GeneratePayload as GeneratePayloadClass } from '../../../backend/jsons/payloadGenerators/PayloadGenerator';
import reportGenerator from '../../../backend/utils/generateReport';
import { loadResponsesFromFile } from '../../../backend/fixtures/utils/stashResponses';

type DataPrepFixtures = Pick<baseFixture, 'Request' | 'Endpoints' | 'saveResponsesToFile'>;

export class reversalDataPrep {
    private fixtures: DataPrepFixtures;
    private stashKey: string;
    private partTwoStashKey: string;
    private partThreeStashKey: string;
    private pendingResponses: Map<string, ResponsesContainer> = new Map();

    constructor(fixtures: DataPrepFixtures, stashKey: string) {
        this.fixtures = fixtures;
        this.stashKey = stashKey;
        this.partTwoStashKey = `${stashKey}_part_2`;
        this.partThreeStashKey = `${stashKey}_part_3`;
    }

    /**
     * After waitForInvoiceGenerationParallel writes invoice IDs back to the stash file,
     * the in-memory pendingResponses still have empty invoice arrays. This method reloads
     * invoice IDs from the stash file into pendingResponses so that subsequent Part methods
     * (e.g. reversalBilling / correctionBilling) can look up invoice numbers correctly.
     */
    reloadInvoicesFromStash(fileName: string): void {
        const data = loadResponsesFromFile(fileName);
        const entries = Array.isArray(data) ? data : [data];

        for (const entry of entries) {
            if (!entry?.testCaseId || !Array.isArray(entry.invoice)) continue;

            // Map testCaseId → pendingResponses key (e.g. 'REG-707' → 'REG_707')
            const key = entry.testCaseId.replace(/-/g, '_');
            const pending = this.pendingResponses.get(key);
            if (pending) {
                pending.invoice = entry.invoice;
                console.log(`🔄 Reloaded ${entry.invoice.length} invoice(s) for ${entry.testCaseId} from ${fileName}`);
            }
        }
    }

    // REG-707: Standard invoice reversal
    // Part 1: set up all entities and run the FOR_VOLUMES billing to generate the initial invoice
    async REG_707_Part1(): Promise<void> {
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

        this.pendingResponses.set('REG_707', Responses);
        saveResponsesToFile(Responses, this.stashKey, 'REG-707');

        test.info().attach('[REG-707] Part 1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-707: Standard invoice reversal
    // Part 2: run the reversal billing using the invoice generated in Part 1
    async REG_707_Part2(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = this.pendingResponses.get('REG_707')!;
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('reversal billing run', async () => {
            const payload = await GeneratePayload.billing.reversalBilling();
            const reversal = await Request.post(Endpoints.billingRun, {data: payload});
            await expect(reversal).CheckResponse();
            Responses.billingRun.push(await reversal.json());
        });

        saveResponsesToFile(Responses, this.partTwoStashKey, 'REG-707');

        test.info().attach('[REG-707] Part 2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-714: Standard invoice reversal with correction debit/credit notes
    // Part 1: set up all entities and run the FOR_VOLUMES billing to generate the initial invoice
    async REG_714_Part1(): Promise<void> {
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

        this.pendingResponses.set('REG_714', Responses);
        saveResponsesToFile(Responses, this.stashKey, 'REG-714');

        test.info().attach('[REG-714] Part 1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-714: Standard invoice reversal with correction debit/credit notes
    // Part 2: run the correction billing which produces debit/credit notes
    async REG_714_Part2(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = this.pendingResponses.get('REG_714')!;
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('standard invoice correction', async () => {
            const payload = await GeneratePayload.billing.correctionBilling();
            const correction = await Request.post(Endpoints.billingRun, {data: payload});
            await expect(correction).CheckResponse();
            const correctionId = await correction.json();
            Responses.billingRun.push({id: correctionId, invoiceNumbers: 2});
        });

        saveResponsesToFile(Responses, this.partTwoStashKey, 'REG-714');

        test.info().attach('[REG-714] Part 2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-714: Standard invoice reversal with correction debit/credit notes
    // Part 3: run the reversal billing which uses the invoice generated in Part 2
    async REG_714_Part3(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = this.pendingResponses.get('REG_714')!;
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('Reverse standard invoice', async () => {
            // After reload, invoice array is [original, correction_inv1, correction_inv2].
            // Reverse the first correction invoice at index 1, not the original at index 0.
            const payload = await GeneratePayload.billing.reversalBilling(1);
            const reversal = await Request.post(Endpoints.billingRun, {data: payload});
            await expect(reversal).CheckResponse();
            const reversalId = await reversal.json();
            Responses.billingRun.push({id: reversalId, invoiceNumbers: 3});
        });

        saveResponsesToFile(Responses, this.partThreeStashKey, 'REG-714');

        test.info().attach('[REG-714] Part 3 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-715: Deducted invoice reversal
    // Part 1: set up all entities, run INTERIM billing, then run FOR_VOLUMES billing
    async REG_715_Part1(): Promise<void> {
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
            payload.formulaRequest.expression = 1;
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

        await test.step('generate interim', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.value = 300;
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

        await test.step('Interim Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        this.pendingResponses.set('REG_715', Responses);
        saveResponsesToFile(Responses, this.stashKey, 'REG-715');

        test.info().attach('[REG-715] Part 1 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-715: Deducted invoice reversal
    // Part 2: run FOR_VOLUMES billing which deducts the interim invoice
    async REG_715_Part2(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = this.pendingResponses.get('REG_715')!;
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('Standard Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.partTwoStashKey, 'REG-715');

        test.info().attach('[REG-715] Part 2 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-715: Deducted invoice reversal
    // Part 3: run the reversal billing on the standard invoice
    async REG_715_Part3(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile } = this.fixtures;
        const Responses = this.pendingResponses.get('REG_715')!;
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, Request.raw);

        await test.step('Reversal Billing run', async () => {
            // Reverse the standard (FOR_VOLUMES) invoice at index 1, not the interim at index 0
            const payload = await GeneratePayload.billing.reversalBilling([0, 1]);
            const billingRun = await Request.post(Endpoints.billingRun, {data: payload});
            await expect(billingRun).CheckResponse();
            const billingRunId = await billingRun.json();
            Responses.billingRun.push({id: billingRunId, invoiceNumbers: 2});
        });

        saveResponsesToFile(Responses, this.partThreeStashKey, 'REG-715');

        test.info().attach('[REG-715] Part 3 response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }
}
