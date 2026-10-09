import { test, expect } from '../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';

type PodOverride = Partial<{
    rpsNumberId: number | null;
    voltageLevel: string;
    consumptionPurpose: string | null;
    type: string;
}>;

type PrepFixtures = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

type BillingApplicationModel =
    | 'FOR_VOLUMES'
    | 'OVER_TIME_PERIODICAL'
    | 'OVER_TIME_ONE_TIME'
    | 'PER_PIECE'
    | 'INTERIM_AND_ADVANCE_PAYMENT'
    | 'WITH_ELECTRICITY_INVOICE';

function applyPodOverride(
    payload: { type?: string; consumptionPurpose?: string | null },
    override?: PodOverride
): void {
    Object.assign(payload, override ?? {});
    // API: consumptionPurpose is required for CONSUMER and must be empty for GENERATOR.
    if (payload.type === 'GENERATOR') {
        payload.consumptionPurpose = null;
    }
}

async function prepareBillingData(
    fixtures: PrepFixtures,
    podOverrides?: { pod0?: PodOverride; pod1?: PodOverride; podCount?: 1 | 2 }
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const podCount = podOverrides?.podCount ?? 2;

    await test.step('generate customer', async () => {
        const customer = await Request.post(Endpoints.customer, { data: GeneratePayload.customers.customer_legal() });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    await test.step('generate price component', async () => {
        const payload = GeneratePayload.productAndServices.priceSettlement();
        payload.discount = true;
        const price = await Request.post(Endpoints.priceComponent, { data: payload });
        await expect(price).CheckResponse();
        Responses.priceComponent.push(await price.json());
    });

    await test.step('generate POD 0', async () => {
        const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
        applyPodOverride(payload, podOverrides?.pod0);
        const podSettlement = await Request.post(Endpoints.pod, { data: payload });
        await expect(podSettlement).CheckResponse();
        Responses.pod.push(await podSettlement.json());
    });

    if (podCount > 1) {
        await test.step('generate POD 1', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            applyPodOverride(payload, podOverrides?.pod1);
            const podSettlement = await Request.post(Endpoints.pod, { data: payload });
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });
    }

    await test.step('generate term', async () => {
        const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
    });

    await test.step('generate product', async () => {
        const product = await Request.post(Endpoints.product, { data: GeneratePayload.productAndServices.product() });
        await expect(product).CheckResponse();
        Responses.product.push(await product.json());
    });

    await test.step('generate contract', async () => {
        const contract = await Request.post(Endpoints.productContract, {
            data: await GeneratePayload.contractsAndOrders.product_contract(
                0,
                0,
                podCount === 1 ? 0 : undefined
            ),
        });
        await expect(contract).CheckResponse();
        Responses.productContract.push(await contract.json());
    });

    await test.step('Activate POD 0', async () => {
        const podActivation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(),
        });
        await expect(podActivation).CheckResponse();
    });

    if (podCount > 1) {
        await test.step('Activate POD 1', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {
                data: await GeneratePayload.pointsOfDelivery.pod_activation(1),
            });
            await expect(podActivation).CheckResponse();
        });
    }

    await test.step('Data by profiles POD 0', async () => {
        const payload = await GeneratePayload.energyData.profile1Month();
        payload.timeZone = 'CET';
        const profiles = await Request.post('billing-by-profile', { data: payload });
        await expect(profiles).CheckResponse();
        const profileData = await profiles.json();
        Responses.dataByProfiles.push({
            id: profileData,
            periodFrom: payload.periodFrom,
            periodTo: payload.periodTo,
            periodType: payload.periodType,
        });
    });

    if (podCount > 1) {
        await test.step('Data by profiles POD 1', async () => {
            const payload = await GeneratePayload.energyData.profile1Month(1);
            payload.timeZone = 'CET';
            const profiles = await Request.post('billing-by-profile', { data: payload });
            await expect(profiles).CheckResponse();
            const profileData = await profiles.json();
            Responses.dataByProfiles.push({
                id: profileData,
                periodFrom: payload.periodFrom,
                periodTo: payload.periodTo,
                periodType: payload.periodType,
            });
        });
    }
}

function getEntityId(response: unknown, entityName: string): string | number {
    if (typeof response === 'string' || typeof response === 'number') {
        return response;
    }

    if (typeof response === 'object' && response !== null && 'id' in response) {
        const id = (response as { id?: unknown }).id;
        if (typeof id === 'string' || typeof id === 'number') {
            return id;
        }
    }

    throw new Error(`${entityName} response does not contain an id`);
}

async function createAdditionalPod(fixtures: PrepFixtures): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const podIndex = Responses.pod.length;

    await test.step(`generate POD ${podIndex}`, async () => {
        const pod = await Request.post(Endpoints.pod, {
            data: GeneratePayload.pointsOfDelivery.pod_settlement(),
        });
        await expect(pod).CheckResponse();
        Responses.pod.push(await pod.json());
    });

    return podIndex;
}

async function createProfileData(fixtures: PrepFixtures, podIndex: number): Promise<void> {
    const { Request, GeneratePayload, Responses } = fixtures;

    await test.step(`Data by profiles POD ${podIndex}`, async () => {
        const payload = await GeneratePayload.energyData.profile1Month(podIndex);
        payload.timeZone = 'CET';
        const profiles = await Request.post('billing-by-profile', { data: payload });
        await expect(profiles).CheckResponse();
        const profileData = await profiles.json();
        Responses.dataByProfiles.push({
            id: profileData,
            periodFrom: payload.periodFrom,
            periodTo: payload.periodTo,
            periodType: payload.periodType,
        });
    });
}

async function createAdditionalProduct(fixtures: PrepFixtures, productTypeId?: number): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const productIndex = Responses.product.length;
    const termIndex = Responses.terms.length;
    const priceComponentIndex = Responses.priceComponent.length;

    await test.step(`generate term ${termIndex}`, async () => {
        const term = await Request.post(Endpoints.terms, { data: GeneratePayload.productAndServices.term() });
        await expect(term).CheckResponse();
        Responses.terms.push(await term.json());
    });

    await test.step(`generate price component ${priceComponentIndex}`, async () => {
        const pricePayload = GeneratePayload.productAndServices.priceSettlement();
        pricePayload.discount = true;
        const price = await Request.post(Endpoints.priceComponent, { data: pricePayload });
        await expect(price).CheckResponse();
        Responses.priceComponent.push(await price.json());
    });

    await test.step(`generate product ${productIndex}`, async () => {
        const payload = GeneratePayload.productAndServices.product();
        payload.termId = Responses.terms[termIndex].id;
        payload.priceComponentIds = [Responses.priceComponent[priceComponentIndex]];
        if (productTypeId !== undefined) {
            payload.productTypeId = productTypeId;
        }

        const product = await Request.post(Endpoints.product, { data: payload });
        await expect(product).CheckResponse();
        Responses.product.push(await product.json());
    });

    return productIndex;
}

async function createAdditionalCustomer(
    fixtures: PrepFixtures,
    options: {
        type: 'LEGAL_ENTITY' | 'PRIVATE_CUSTOMER';
        segmentId?: number;
    }
): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;

    await test.step(`generate ${options.type} customer`, async () => {
        const payload =
            options.type === 'PRIVATE_CUSTOMER'
                ? GeneratePayload.customers.customer_private()
                : GeneratePayload.customers.customer_legal();
        if (options.segmentId !== undefined) {
            payload.segmentIds = [options.segmentId];
        }

        const customer = await Request.post(Endpoints.customer, { data: payload });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    return Responses.customer.length - 1;
}

async function createAdditionalProductContract(
    fixtures: PrepFixtures,
    options: { productIndex?: number; contractType?: string; customerIndex?: number } = {}
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const podIndex = await createAdditionalPod(fixtures);
    const contractIndex = Responses.productContract.length;
    const customerIndex = options.customerIndex ?? 0;

    await test.step(`generate product contract ${contractIndex}`, async () => {
        const payload = await GeneratePayload.contractsAndOrders.product_contract(
            customerIndex,
            options.productIndex ?? 0,
            podIndex
        );
        // product_contract() always stamps customer[0]; override when billing a different customer
        payload.basicParameters.customerId = Responses.customer[customerIndex].id;
        if (options.contractType !== undefined) {
            payload.productParameters.contractType = options.contractType;
        }

        const contract = await Request.post(Endpoints.productContract, { data: payload });
        await expect(contract).CheckResponse();
        Responses.productContract.push(await contract.json());
    });

    await test.step(`Activate POD ${podIndex}`, async () => {
        const podActivation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(
                podIndex,
                undefined,
                undefined,
                contractIndex
            ),
        });
        await expect(podActivation).CheckResponse();
    });

    await createProfileData(fixtures, podIndex);
}

async function createAdditionalServiceContract(fixtures: PrepFixtures): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const podIndex = await createAdditionalPod(fixtures);
    const priceComponentIndex = Responses.priceComponent.length;

    await test.step('generate service price component', async () => {
        const priceComponent = await Request.post(Endpoints.priceComponent, {
            data: GeneratePayload.productAndServices.periodicalComponent(),
        });
        await expect(priceComponent).CheckResponse();
        Responses.priceComponent.push(await priceComponent.json());
    });

    await test.step('generate service', async () => {
        const payload = GeneratePayload.productAndServices.service();
        payload.priceComponents = [Responses.priceComponent[priceComponentIndex]];
        const service = await Request.post(Endpoints.service, {
            data: payload,
        });
        await expect(service).CheckResponse();
        Responses.service.push(await service.json());
    });

    const contractIndex = Responses.serviceContract.length;
    await test.step(`generate service contract ${contractIndex}`, async () => {
        const payload = await GeneratePayload.contractsAndOrders.serviceContract();
        payload.serviceParameters.podIds = [Responses.pod[podIndex].id];
        const contract = await Request.post(Endpoints.serviceContract, { data: payload });
        await expect(contract).CheckResponse();
        Responses.serviceContract.push(await contract.json());
    });

    await createProfileData(fixtures, podIndex);
}

async function getProductContractPodIdentifiers(
    Request: PrepFixtures['Request'],
    Responses: PrepFixtures['Responses'],
    contractIndex: number,
    podIndexes: number[]
): Promise<string[]> {
    const contractResponse = Responses.productContract[contractIndex];
    const contractId = getEntityId(contractResponse, `product contract ${contractIndex}`);
    const contract = await Request.get(`product-contract/${contractId}?version=1`);
    await expect(contract).CheckResponse();
    const pods =
        ((await contract.json()) as {
            contractPodsResponses?: Array<{ podId?: number; identifier?: string | number }>;
        }).contractPodsResponses ?? [];

    return podIndexes.map((podIndex) =>
        String(
            pods.find((pod) => pod.podId === Responses.pod[podIndex].id)?.identifier ??
                Responses.pod[podIndex].identifier ??
                Responses.pod[podIndex].id
        )
    );
}

async function getPodIdentifiers(
    Request: PrepFixtures['Request'],
    Responses: PrepFixtures['Responses']
): Promise<{ pod0: string; pod1: string }> {
    const [pod0, pod1] = await getProductContractPodIdentifiers(Request, Responses, 0, [0, 1]);
    return { pod0, pod1 };
}

function customerNumberCondition(customer: {
    customerNumber?: string | number;
    identifier?: string | number;
}): string {
    return `$CUSTOMER_NUMBER$=$${customer.customerNumber ?? customer.identifier}$`;
}

function customerGuard(Responses: PrepFixtures['Responses']): string {
    const customerNumberConditions = Responses.customer.map((customer) => customerNumberCondition(customer));
    if (customerNumberConditions.length === 1) {
        return `AND${customerNumberConditions[0]}`;
    }

    // Parenthesize OR so AND with the attribute condition cannot be bypassed by a second customer number
    return `AND(${customerNumberConditions.join('OR')})`;
}

async function runConditionBilling(
    fixtures: PrepFixtures,
    applicationLevel: 'CUSTOMER' | 'CONTRACT' | 'POD',
    conditionFragment: string,
    applicationModelTypes?: BillingApplicationModel[]
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const payload = await GeneratePayload.billing.billingRun();
    if (applicationModelTypes !== undefined) {
        payload.basicParameters.applicationModelType = applicationModelTypes;
    }
    payload.basicParameters.listOfCustomersContractsOrPOD = null;
    payload.basicParameters.billingCriteria = 'CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS';
    payload.basicParameters.billingApplicationLevel = applicationLevel;
    payload.basicParameters.customersContractOrPODConditions = `${conditionFragment}${customerGuard(Responses)}`;

    const billingRun = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(billingRun).CheckResponse();
    Responses.billingRun.push(await billingRun.json());
}

async function getCustomerIdentifier(
    Request: PrepFixtures['Request'],
    Responses: PrepFixtures['Responses'],
    customerIndex = 0
): Promise<string> {
    const customerId = getEntityId(Responses.customer[customerIndex], `customer ${customerIndex}`);
    const customerRes = await Request.get(`customer/${customerId}?version=1`);
    await expect(customerRes).CheckResponse();
    const customerJson = (await customerRes.json()) as { identifier?: string | number };
    const identifier = customerJson.identifier ?? Responses.customer[customerIndex].identifier;
    if (identifier == null || String(identifier).trim() === '') {
        throw new Error(`customer ${customerIndex} identifier is missing`);
    }
    return String(identifier);
}

async function getProductContractNumber(
    Request: PrepFixtures['Request'],
    Responses: PrepFixtures['Responses'],
    contractIndex = 0
): Promise<string> {
    const contractId = getEntityId(Responses.productContract[contractIndex], `product contract ${contractIndex}`);
    const contractRes = await Request.get(`product-contract/${contractId}?version=1`);
    await expect(contractRes).CheckResponse();
    const contractJson = (await contractRes.json()) as {
        basicParameters?: { contractNumber?: string | number | null };
    };
    const contractNumber = contractJson.basicParameters?.contractNumber;
    if (contractNumber == null || String(contractNumber).trim() === '') {
        throw new Error(`product contract ${contractIndex} contractNumber is missing`);
    }
    return String(contractNumber);
}

async function getPodApiIdentifier(
    Request: PrepFixtures['Request'],
    Responses: PrepFixtures['Responses'],
    podIndex: number
): Promise<string> {
    const podId = getEntityId(Responses.pod[podIndex], `pod ${podIndex}`);
    const podRes = await Request.get(`pod/${podId}?version=1`);
    await expect(podRes).CheckResponse();
    const podJson = (await podRes.json()) as { identifier?: string | number };
    const identifier = podJson.identifier ?? Responses.pod[podIndex].identifier;
    if (identifier == null || String(identifier).trim() === '') {
        throw new Error(`pod ${podIndex} identifier is missing`);
    }
    return String(identifier);
}

async function runListBilling(
    fixtures: PrepFixtures,
    applicationLevel: 'CUSTOMER' | 'CONTRACT' | 'POD',
    listValue: string
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const payload = await GeneratePayload.billing.billingRun(applicationLevel);
    payload.basicParameters.billingCriteria = 'LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS';
    payload.basicParameters.billingApplicationLevel = applicationLevel;
    payload.basicParameters.listOfCustomersContractsOrPOD = listValue;
    payload.basicParameters.customersContractOrPODConditions = null;

    const billingRun = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(billingRun).CheckResponse();
    Responses.billingRun.push(await billingRun.json());
}

async function assertListCriteriaInvoices(
    Request: PrepFixtures['Request'],
    Responses: PrepFixtures['Responses'],
    expectation: {
        applicationLevel: 'CUSTOMER' | 'CONTRACT' | 'POD';
        listValue: string;
        expectedInvoiceCount?: number;
        includedPodIdentifiers?: string[];
        excludedPodIdentifiers?: string[];
        includedContractIds?: Array<string | number>;
        excludedContractIds?: Array<string | number>;
        includedCustomerIds?: Array<string | number>;
        excludedCustomerIds?: Array<string | number>;
    }
): Promise<void> {
    const rawBillingRun = Responses.billingRun?.[0];
    const billingRunId =
        typeof rawBillingRun === 'object' && rawBillingRun !== null && 'id' in rawBillingRun
            ? (rawBillingRun as { id: number }).id
            : rawBillingRun;
    expect(billingRunId, 'billingRun id missing').toBeTruthy();

    const billingRunRes = await Request.get(`billing-run/${billingRunId}`);
    await expect(billingRunRes).CheckResponse();
    const billingRunJson = await billingRunRes.json();
    const params = billingRunJson.standardBillingRunParameters;

    expect(params.billingCriteria).toBe('LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS');
    expect(params.applicationLevel).toBe(expectation.applicationLevel);
    expect(String(params.customerContractOrPodList ?? '')).toBe(expectation.listValue);
    expect(
        params.customerContractOrPodConditions == null ||
            String(params.customerContractOrPodConditions).trim() === ''
    ).toBe(true);

    const expectedCount = expectation.expectedInvoiceCount ?? 1;
    expect(Responses.invoice.length).toBe(expectedCount);

    const foundPodIdentifiers = new Set<string>();
    const foundContractIds = new Set<string>();
    const foundCustomerIds = new Set<string>();

    for (const invoiceId of Responses.invoice) {
        const invoiceRes = await Request.get(`invoice?id=${invoiceId}`);
        await expect(invoiceRes).CheckResponse();
        const invoiceJson = await invoiceRes.json();

        if (invoiceJson.customer?.id != null) {
            foundCustomerIds.add(String(invoiceJson.customer.id));
        }
        if (invoiceJson.productContract?.id != null) {
            foundContractIds.add(String(invoiceJson.productContract.id));
        }

        const detailedRes = await Request.get(`invoice/detailed-data?id=${invoiceId}&page=0&size=100`);
        await expect(detailedRes).CheckResponse();
        const detailedJson = await detailedRes.json();
        const rows: Array<{ pointOfDelivery?: string | null }> = detailedJson.content ?? [];
        for (const row of rows) {
            const podId = String(row.pointOfDelivery ?? '').trim();
            if (podId) foundPodIdentifiers.add(podId);
        }
    }

    for (const id of expectation.includedCustomerIds ?? []) {
        expect(foundCustomerIds.has(String(id)), `expected customer ${id} on invoice(s)`).toBe(true);
    }
    for (const id of expectation.excludedCustomerIds ?? []) {
        expect(foundCustomerIds.has(String(id)), `customer ${id} should NOT be on invoice(s)`).toBe(false);
    }
    for (const id of expectation.includedContractIds ?? []) {
        expect(foundContractIds.has(String(id)), `expected contract ${id} on invoice(s)`).toBe(true);
    }
    for (const id of expectation.excludedContractIds ?? []) {
        expect(foundContractIds.has(String(id)), `contract ${id} should NOT be on invoice(s)`).toBe(false);
    }
    for (const pod of expectation.includedPodIdentifiers ?? []) {
        expect(
            foundPodIdentifiers.has(pod),
            `expected POD ${pod} on invoice detailed-data; found [${[...foundPodIdentifiers].join(', ')}]`
        ).toBe(true);
    }
    for (const pod of expectation.excludedPodIdentifiers ?? []) {
        expect(
            foundPodIdentifiers.has(pod),
            `POD ${pod} should NOT be on invoice detailed-data; found [${[...foundPodIdentifiers].join(', ')}]`
        ).toBe(false);
    }
}

test.describe('[REG-993]: Billing run conditions', { tag: ['@billing', '@billing-conditions'] }, () => {
    // Pipeline runs REG-1245, REG-1246, and REG-1247. The other cases stay skipped.

    // ─── POD-level attribute conditions ───────────────────────────────────────

    test.skip('[REG-1241]: COND-01 [POD][POD_WITH_RPS=YES]', { tag: ['@pod-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { rpsNumberId: 1047 },
            pod1: { rpsNumberId: null },
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'POD', '$POD_WITH_RPS$=$YES$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'POD',
                expectedInvoiceCount: 1,
                includedPodIdentifiers: [pod0],
                excludedPodIdentifiers: [pod1],
            });
        });

        test.info().attach('[COND-01] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1242]: COND-02 [POD][POD_WITH_RPS=NO]', { tag: ['@pod-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { rpsNumberId: 1047 },
            pod1: { rpsNumberId: null },
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'POD', '$POD_WITH_RPS$=$NO$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'POD',
                expectedInvoiceCount: 1,
                includedPodIdentifiers: [pod1],
                excludedPodIdentifiers: [pod0],
            });
        });

        test.info().attach('[COND-02] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1243]: COND-03 [CONTRACT][POD_WITH_RPS=YES]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { rpsNumberId: 1047 },
            pod1: { rpsNumberId: null },
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CONTRACT', '$POD_WITH_RPS$=$YES$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COND-03] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1244]: COND-04 [CUSTOMER][POD_WITH_RPS=YES]', { tag: ['@customer-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { rpsNumberId: 1047 },
            pod1: { rpsNumberId: null },
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CUSTOMER', '$POD_WITH_RPS$=$YES$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CUSTOMER',
                expectedInvoiceCount: 1,
                includedCustomerIds: [Responses.customer[0].id],
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COND-04] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-1245]: COND-05 [POD][VOLTAGE_LEVEL=LOW]', { tag: ['@billing', '@billing-conditions', '@pod-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { voltageLevel: 'LOW' },
            pod1: { voltageLevel: 'MEDIUM' },
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'POD', '$VOLTAGE_LEVEL$=$LOW$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'POD',
                expectedInvoiceCount: 1,
                includedPodIdentifiers: [pod0],
                excludedPodIdentifiers: [pod1],
            });
        });

        test.info().attach('[COND-05] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-1246]: COND-06 [CONTRACT][VOLTAGE_LEVEL=LOW]', { tag: ['@billing', '@billing-conditions', '@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { voltageLevel: 'LOW' },
            pod1: { voltageLevel: 'MEDIUM' },
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CONTRACT', '$VOLTAGE_LEVEL$=$LOW$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COND-06] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test('[REG-1247]: COND-07 [CUSTOMER][VOLTAGE_LEVEL=LOW]', { tag: ['@billing', '@billing-conditions', '@customer-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { voltageLevel: 'LOW' },
            pod1: { voltageLevel: 'MEDIUM' },
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CUSTOMER', '$VOLTAGE_LEVEL$=$LOW$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CUSTOMER',
                expectedInvoiceCount: 1,
                includedCustomerIds: [Responses.customer[0].id],
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COND-07] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1248]: COND-08 [POD][PURPOSE_OF_CONSUMPTION=NON_HOUSEHOLD]', { tag: ['@pod-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { consumptionPurpose: 'NON_HOUSEHOLD' },
            podCount: 1,
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'POD', '$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const [pod0] = await getProductContractPodIdentifiers(Request, Responses, 0, [0]);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'POD',
                expectedInvoiceCount: 1,
                includedPodIdentifiers: [pod0],
            });
        });

        test.info().attach('[COND-08] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1303]: COND-08 [POD][PURPOSE_OF_CONSUMPTION=NON_HOUSEHOLD] | no match', {
        tag: ['@pod-level'],
    }, async ({ Request, GeneratePayload, Responses, Endpoints, validateInvoice }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { consumptionPurpose: 'HOUSEHOLD' },
            podCount: 1,
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'POD', '$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, false, 0);
        });

        await test.step('validate conditions', async () => {
            const podRes = await Request.get(`pod/${Responses.pod[0].id}?version=1`);
            await expect(podRes).CheckResponse();
            expect((await podRes.json()).consumptionPurpose).toBe('HOUSEHOLD');

            await validateInvoice.conditionsValidator({
                applicationLevel: 'POD',
                expectNoInvoices: true,
            });
        });

        test.info().attach('[COND-08 no match] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1249]: COND-09 [CONTRACT][PURPOSE_OF_CONSUMPTION=NON_HOUSEHOLD]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { consumptionPurpose: 'NON_HOUSEHOLD' },
            podCount: 1,
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CONTRACT', '$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const [pod0] = await getProductContractPodIdentifiers(Request, Responses, 0, [0]);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0],
            });
        });

        test.info().attach('[COND-09] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1304]: COND-09 [CONTRACT][PURPOSE_OF_CONSUMPTION=NON_HOUSEHOLD] | no match', {
        tag: ['@contract-level'],
    }, async ({ Request, GeneratePayload, Responses, Endpoints, validateInvoice }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { consumptionPurpose: 'HOUSEHOLD' },
            podCount: 1,
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CONTRACT', '$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, false, 0);
        });

        await test.step('validate conditions', async () => {
            const podRes = await Request.get(`pod/${Responses.pod[0].id}?version=1`);
            await expect(podRes).CheckResponse();
            expect((await podRes.json()).consumptionPurpose).toBe('HOUSEHOLD');

            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectNoInvoices: true,
            });
        });

        test.info().attach('[COND-09 no match] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1250]: COND-10 [POD][POD_TYPE=CONSUMER]', { tag: ['@pod-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { type: 'CONSUMER' },
            pod1: { type: 'GENERATOR' },
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'POD', '$POD_TYPE$=$CONSUMER$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'POD',
                expectedInvoiceCount: 1,
                includedPodIdentifiers: [pod0],
                excludedPodIdentifiers: [pod1],
            });
        });

        test.info().attach('[COND-10] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1251]: COND-11 [CONTRACT][POD_TYPE=CONSUMER]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { type: 'CONSUMER' },
            pod1: { type: 'GENERATOR' },
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CONTRACT', '$POD_TYPE$=$CONSUMER$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COND-11] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1252]: COND-12 [POD][POD_MEASUREMENT_TYPE=SETTLEMENT_PERIOD]', { tag: ['@pod-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'POD', '$POD_MEASUREMENT_TYPE$=$SETTLEMENT_PERIOD$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'POD',
                expectedInvoiceCount: 1,
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COND-12] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1253]: COND-13 [POD][POD_GRID_OP]', { tag: ['@pod-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);

        await test.step('Billing run', async () => {
            const podRes = await Request.get(`pod/${Responses.pod[0].id}?version=1`);
            await expect(podRes).CheckResponse();
            const gridOpId = (await podRes.json()).gridOperatorId;
            await runConditionBilling(fixtures, 'POD', `$POD_GRID_OP$=$${gridOpId}$`);
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'POD',
                expectedInvoiceCount: 1,
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COND-13] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ─── Contract-level attribute conditions ──────────────────────────────────

    test.skip('[REG-1255]: COND-15 [CONTRACT][CONTRACT=PRODUCT_CONTRACT]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);
        await createAdditionalServiceContract(fixtures);

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CONTRACT', '$CONTRACT$=$PRODUCT_CONTRACT$', [
                'FOR_VOLUMES',
                'OVER_TIME_PERIODICAL',
            ]);
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
            });
        });

        test.info().attach('[COND-15] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1256]: COND-16 [CONTRACT][PRODUCT]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);
        const excludedProductIndex = await createAdditionalProduct(fixtures);
        await createAdditionalProductContract(fixtures, {
            productIndex: excludedProductIndex,
        });

        await test.step('Billing run', async () => {
            const productId = Responses.product[0];
            await runConditionBilling(fixtures, 'CONTRACT', `$PRODUCT$=$${productId}$`);
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
            });
        });

        test.info().attach('[COND-16] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1257]: COND-17 [CONTRACT][PRODUCT_TYPE]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);
        const includedProductTypeId = await test.step('prepare product type condition controls', async () => {
            const productRes = await Request.get(`products/${Responses.product[0]}`);
            await expect(productRes).CheckResponse();
            const productTypeId = Number((await productRes.json()).productType.id);
            const excludedProductTypeId = Number(
                await Nomenclatures.product_type(`billing-condition-product-type-${Date.now()}`)
            );
            expect(excludedProductTypeId).not.toBe(productTypeId);

            const excludedProductIndex = await createAdditionalProduct(fixtures, excludedProductTypeId);
            await createAdditionalProductContract(fixtures, {
                productIndex: excludedProductIndex,
            });
            return productTypeId;
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(
                fixtures,
                'CONTRACT',
                `$PRODUCT_TYPE$=$${includedProductTypeId}$`
            );
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
            });
        });

        test.info().attach('[COND-17] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1258]: COND-18 [CONTRACT][CONTRACT_TYPE]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);
        const includedContractType = await test.step('prepare contract type condition controls', async () => {
            const contractRes = await Request.get(`product-contract/${Responses.productContract[0].id}?version=1`);
            await expect(contractRes).CheckResponse();
            const contractType = (await contractRes.json()).productParameters.contractType;
            const excludedContractType = contractType === 'COMBINED' ? 'WITHOUT_SUPPLY' : 'COMBINED';

            await createAdditionalProductContract(fixtures, {
                contractType: excludedContractType,
            });
            return contractType;
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(
                fixtures,
                'CONTRACT',
                `$CONTRACT_TYPE$=$${includedContractType}$`
            );
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
            });
        });

        test.info().attach('[COND-18] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1259]: COND-19 [CONTRACT][INTERIM_ADVANCE_PAYMENT=NO]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);
        await test.step('prepare interim condition control', async () => {
            const interim = await Request.post(Endpoints.interim, {
                data: GeneratePayload.productAndServices.interim(),
            });
            await expect(interim).CheckResponse();
            Responses.interim.push(await interim.json());

            const productIndex = await createAdditionalProduct(fixtures);
            await createAdditionalProductContract(fixtures, { productIndex });
        });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CONTRACT', '$INTERIM_ADVANCE_PAYMENT$=$NO$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
            });
        });

        test.info().attach('[COND-19] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1260]: COND-20 [CUSTOMER][CONTRACT=PRODUCT_CONTRACT]', { tag: ['@customer-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CUSTOMER', '$CONTRACT$=$PRODUCT_CONTRACT$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CUSTOMER',
                expectedInvoiceCount: 1,
                includedCustomerIds: [Responses.customer[0].id],
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COND-20] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1261]: COND-21 [CUSTOMER][PRODUCT]', { tag: ['@customer-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);

        await test.step('Billing run', async () => {
            const productId = Responses.product[0];
            await runConditionBilling(fixtures, 'CUSTOMER', `$PRODUCT$=$${productId}$`);
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CUSTOMER',
                expectedInvoiceCount: 1,
                includedCustomerIds: [Responses.customer[0].id],
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COND-21] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ─── Customer-level attribute conditions ──────────────────────────────────

    test.skip('[REG-1262]: COND-22 [CUSTOMER][CUSTOMER_TYPE=LEGAL_ENTITY]', { tag: ['@customer-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);
        const excludedCustomerIndex = await createAdditionalCustomer(fixtures, { type: 'PRIVATE_CUSTOMER' });
        await createAdditionalProductContract(fixtures, { customerIndex: excludedCustomerIndex });

        await test.step('Billing run', async () => {
            await runConditionBilling(fixtures, 'CUSTOMER', '$CUSTOMER_TYPE$=$LEGAL_ENTITY$');
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            const excludedPodIndex = Responses.pod.length - 1;
            const excludedContractIndex = Responses.productContract.length - 1;
            const [excludedPod] = await getProductContractPodIdentifiers(
                Request,
                Responses,
                excludedContractIndex,
                [excludedPodIndex]
            );
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CUSTOMER',
                expectedInvoiceCount: 1,
                includedCustomerIds: [Responses.customer[0].id],
                includedContractIds: [Responses.productContract[0].id],
                excludedContractIds: [Responses.productContract[excludedContractIndex].id],
                includedPodIdentifiers: [pod0, pod1],
                excludedPodIdentifiers: [excludedPod],
            });
        });

        test.info().attach('[COND-22] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1263]: COND-23 [CUSTOMER][CUSTOMER_SEGMENT]', { tag: ['@customer-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);
        const excludedSegmentId = Number(
            await Nomenclatures.segment(`billing-condition-segment-${Date.now()}`)
        );
        const excludedCustomerIndex = await createAdditionalCustomer(fixtures, {
            type: 'LEGAL_ENTITY',
            segmentId: excludedSegmentId,
        });
        await createAdditionalProductContract(fixtures, { customerIndex: excludedCustomerIndex });

        await test.step('Billing run', async () => {
            const custRes = await Request.get(`customer/${Responses.customer[0].id}?version=1`);
            await expect(custRes).CheckResponse();
            const segmentId = (await custRes.json()).customerSegments[0].segment.id;
            expect(segmentId).not.toBe(excludedSegmentId);
            await runConditionBilling(fixtures, 'CUSTOMER', `$CUSTOMER_SEGMENT$=$${segmentId}$`);
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            const excludedPodIndex = Responses.pod.length - 1;
            const excludedContractIndex = Responses.productContract.length - 1;
            const [excludedPod] = await getProductContractPodIdentifiers(
                Request,
                Responses,
                excludedContractIndex,
                [excludedPodIndex]
            );
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CUSTOMER',
                expectedInvoiceCount: 1,
                includedCustomerIds: [Responses.customer[0].id],
                includedContractIds: [Responses.productContract[0].id],
                excludedContractIds: [Responses.productContract[excludedContractIndex].id],
                includedPodIdentifiers: [pod0, pod1],
                excludedPodIdentifiers: [excludedPod],
            });
        });

        test.info().attach('[COND-23] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ─── Combination cases ────────────────────────────────────────────────────
    // Portable operators (no RPS): IN / AND / OR / NOT.
    // Same product contract cannot mix HOUSEHOLD + NON_HOUSEHOLD
    // (ProductContractPodService: "multiple consumption types").
    // Split PODs by voltage only; purpose and POD type stay NON_HOUSEHOLD + CONSUMER.

    test.skip('[REG-1264]: COMB-01 [CONTRACT][IN AND OR NOT match]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { voltageLevel: 'LOW', consumptionPurpose: 'NON_HOUSEHOLD', type: 'CONSUMER' },
            pod1: { voltageLevel: 'MEDIUM', consumptionPurpose: 'NON_HOUSEHOLD', type: 'CONSUMER' },
        });

        await test.step('Billing run', async () => {
            // (voltage IN (LOW, HIGH) AND purpose=NON_HOUSEHOLD AND NOT voltage=HIGH)
            // OR (voltage=MEDIUM AND measurement=SETTLEMENT)
            // AND contract=PRODUCT
            // Customer type is not applicable at CONTRACT (Confluence Phase 2 billing conditions).
            await runConditionBilling(
                fixtures,
                'CONTRACT',
                '(($VOLTAGE_LEVEL$IN($LOW$,$HIGH$)AND$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$AND(NOT($VOLTAGE_LEVEL$=$HIGH$)))OR($VOLTAGE_LEVEL$=$MEDIUM$AND$POD_MEASUREMENT_TYPE$=$SETTLEMENT_PERIOD$))AND$CONTRACT$=$PRODUCT_CONTRACT$'
            );
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0, pod1],
            });
        });

        test.info().attach('[COMB-01] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1299]: COMB-02 [CONTRACT][IN AND OR NOT no match]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures, {
            pod0: { voltageLevel: 'LOW', consumptionPurpose: 'NON_HOUSEHOLD', type: 'CONSUMER' },
            pod1: { voltageLevel: 'MEDIUM', consumptionPurpose: 'NON_HOUSEHOLD', type: 'CONSUMER' },
        });

        await test.step('Billing run', async () => {
            // Billing-run condition values (VariableTypes), not POD OpenAPI enums:
            // MEDIUM_DIRECTLY_CONNECTED (not MEDIUM_DIRECT_CONNECTED), GENERATR (not GENERATOR).
            // Neither POD satisfies:
            // (voltage IN (HIGH, MEDIUM_DIRECTLY_CONNECTED) AND purpose=NON_HOUSEHOLD AND NOT type=CONSUMER)
            // OR (voltage=LOW AND type=GENERATR)
            await runConditionBilling(
                fixtures,
                'CONTRACT',
                '(($VOLTAGE_LEVEL$IN($HIGH$,$MEDIUM_DIRECTLY_CONNECTED$)AND$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$AND(NOT($POD_TYPE$=$CONSUMER$)))OR($VOLTAGE_LEVEL$=$LOW$AND$POD_TYPE$=$GENERATR$))AND$CONTRACT$=$PRODUCT_CONTRACT$'
            );
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, false, 0);
        });

        await test.step('validate conditions', async () => {
            await validateInvoice.conditionsValidator({
                applicationLevel: 'CONTRACT',
                expectNoInvoices: true,
            });
        });

        test.info().attach('[COMB-02] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    // ─── List of Customers, Contracts or Points of Delivery ───────────────────
    // billingCriteria LIST_OF_CUSTOMERS_CONTRACTS_OR_PODS (not the condition builder).
    // Values: customer identifier / contract number / POD identifier (comma-separated, no spaces).
    // Application level scopes invoices the same way as the matching CONDITIONS tests.

    test.skip('[REG-1300]: LIST-01 [CUSTOMER][list identifier]', { tag: ['@customer-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);
        const excludedCustomerIndex = await createAdditionalCustomer(fixtures, { type: 'LEGAL_ENTITY' });
        await createAdditionalProductContract(fixtures, { customerIndex: excludedCustomerIndex });

        let customerIdentifier = '';
        await test.step('Billing run', async () => {
            customerIdentifier = await getCustomerIdentifier(Request, Responses, 0);
            await runListBilling(fixtures, 'CUSTOMER', customerIdentifier);
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate list criteria', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            const excludedPodIndex = Responses.pod.length - 1;
            const excludedContractIndex = Responses.productContract.length - 1;
            const [excludedPod] = await getProductContractPodIdentifiers(
                Request,
                Responses,
                excludedContractIndex,
                [excludedPodIndex]
            );
            await assertListCriteriaInvoices(Request, Responses, {
                applicationLevel: 'CUSTOMER',
                listValue: customerIdentifier,
                expectedInvoiceCount: 1,
                includedCustomerIds: [Responses.customer[0].id],
                excludedCustomerIds: [Responses.customer[excludedCustomerIndex].id],
                includedContractIds: [Responses.productContract[0].id],
                excludedContractIds: [Responses.productContract[excludedContractIndex].id],
                includedPodIdentifiers: [pod0, pod1],
                excludedPodIdentifiers: [excludedPod],
            });
        });

        test.info().attach('[LIST-01] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1301]: LIST-02 [CONTRACT][list contract number]', { tag: ['@contract-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);
        await createAdditionalProductContract(fixtures);

        let contractNumber = '';
        await test.step('Billing run', async () => {
            contractNumber = await getProductContractNumber(Request, Responses, 0);
            await runListBilling(fixtures, 'CONTRACT', contractNumber);
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate list criteria', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            const excludedPodIndex = Responses.pod.length - 1;
            const excludedContractIndex = Responses.productContract.length - 1;
            const [excludedPod] = await getProductContractPodIdentifiers(
                Request,
                Responses,
                excludedContractIndex,
                [excludedPodIndex]
            );
            await assertListCriteriaInvoices(Request, Responses, {
                applicationLevel: 'CONTRACT',
                listValue: contractNumber,
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
                excludedContractIds: [Responses.productContract[excludedContractIndex].id],
                includedPodIdentifiers: [pod0, pod1],
                excludedPodIdentifiers: [excludedPod],
            });
        });

        test.info().attach('[LIST-02] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1302]: LIST-03 [POD][list identifier]', { tag: ['@pod-level'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingData(fixtures);

        let pod0Identifier = '';
        await test.step('Billing run', async () => {
            pod0Identifier = await getPodApiIdentifier(Request, Responses, 0);
            await runListBilling(fixtures, 'POD', pod0Identifier);
        });

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate list criteria', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await assertListCriteriaInvoices(Request, Responses, {
                applicationLevel: 'POD',
                listValue: pod0Identifier,
                expectedInvoiceCount: 1,
                includedContractIds: [Responses.productContract[0].id],
                includedPodIdentifiers: [pod0],
                excludedPodIdentifiers: [pod1],
            });
        });

        test.info().attach('[LIST-03] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });
});
