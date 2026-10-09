import { test, expect } from '../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../backend/fixtures/baseFixture';
import reportGenerator from '../../backend/utils/generateReport';
import { envVariables } from '../../backend/fixtures/envCashed';
import { randomGens } from '../../backend/utils/randomGens';

type AddressOverride = Partial<{
    countryId: number;
    regionId: number;
    municipalityId: number;
    populatedPlaceId: number;
    zipCodeId: number;
}>;

type PodOverride = Partial<{
    voltageLevel: string;
    consumptionPurpose: string;
    gridOperatorId: number | null;
    settlementPeriod: boolean;
    slp: boolean;
    measurementTypeId: number | null;
    providedPower: number | null;
    multiplier: number | null;
    podAdditionalParameters: number[] | null;
    address: AddressOverride;
}>;

type PrepFixtures = Pick<baseFixture, 'Request' | 'GeneratePayload' | 'Responses' | 'Endpoints'>;

const PC_BASE = 'PC_BASE';
const ALT_GRID_OPERATOR_ID = 1001;
const PROVIDED_POWER_MATCH = 10;
const PROVIDED_POWER_CONTRAST = 20;
const MULTIPLIER_MATCH = 2;
const MULTIPLIER_CONTRAST = 4;
const DIRECT_DEBIT_IBAN = 'BG11AAAA111111AAAAAAAA';

function applyCustomerDirectDebit(
    payload: { bankingDetails: { directDebit: boolean; iban: string | null } },
    directDebit: boolean
): void {
    payload.bankingDetails.directDebit = directDebit;
    if (directDebit) {
        payload.bankingDetails.iban = DIRECT_DEBIT_IBAN;
    }
}

function applyPodOverride(payload: Record<string, unknown>, podOverride?: PodOverride): void {
    if (!podOverride) {
        return;
    }

    const { address, ...rest } = podOverride;
    Object.assign(payload, rest);
    if (address) {
        const addressRequest = (payload.addressRequest ?? {}) as {
            localAddressData?: Record<string, unknown>;
        };
        payload.addressRequest = addressRequest;
        addressRequest.localAddressData = {
            ...(addressRequest.localAddressData ?? {}),
            ...address,
        };
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

async function createPriceComponent(
    fixtures: PrepFixtures,
    options: { name: string; condition: string | null; discount?: boolean }
): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const payload = GeneratePayload.productAndServices.priceSettlement();
    // Invoice detailed-data.priceComponent is PriceComponent.name (not id / displayName).
    // Default generator name is BY_SETTLEMENT_PERIODS for every PC — set an explicit unique name.
    payload.name = options.name;
    payload.displayName = options.name;
    payload.discount = options.discount ?? true;
    payload.formulaRequest.condition = options.condition;
    // Unique formula so invoice detailed-data unitPrice can still resolve the PC if name mapping fails
    payload.formulaRequest.expression = String(2000 + Responses.priceComponent.length);
    const res = await Request.post(Endpoints.priceComponent, { data: payload });
    await expect(res).CheckResponse();
    Responses.priceComponent.push(await res.json());
    return Responses.priceComponent.length - 1;
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

async function createAdditionalPod(
    fixtures: PrepFixtures,
    podOverride?: PodOverride
): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const podIndex = Responses.pod.length;

    await test.step(`generate POD ${podIndex}`, async () => {
        const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
        applyPodOverride(payload as unknown as Record<string, unknown>, podOverride);
        const pod = await Request.post(Endpoints.pod, { data: payload });
        await expect(pod).CheckResponse();
        Responses.pod.push(await pod.json());
    });

    return podIndex;
}

async function createAdditionalProductContract(
    fixtures: PrepFixtures,
    options: {
        productIndex?: number;
        contractType?: string;
        customerIndex?: number;
        podOverride?: PodOverride;
        campaignId?: number | string | null;
        directDebit?: boolean;
        activationDate?: string;
        perpetuity?: boolean;
        riskAssessmentAdditionalConditions?: string[];
        podActivationDate?: string;
    } = {}
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const podIndex = await createAdditionalPod(fixtures, options.podOverride);
    const contractIndex = Responses.productContract.length;
    const customerIndex = options.customerIndex ?? 0;

    await test.step(`generate product contract ${contractIndex}`, async () => {
        const payload = await GeneratePayload.contractsAndOrders.product_contract(
            customerIndex,
            options.productIndex ?? 0,
            podIndex
        );
        payload.basicParameters.customerId = Responses.customer[customerIndex].id;
        if (options.contractType !== undefined) {
            payload.productParameters.contractType = options.contractType;
        }
        if (options.campaignId !== undefined) {
            payload.additionalParameters.campaignId = options.campaignId;
        }
        if (options.directDebit !== undefined) {
            payload.additionalParameters.bankingDetails.directDebit = options.directDebit;
        }
        if (options.activationDate !== undefined) {
            payload.basicParameters.activationDate = options.activationDate;
        }
        if (options.perpetuity) {
            payload.basicParameters.status = 'ACTIVE_IN_PERPETUITY';
            payload.basicParameters.perpetuityDate = randomGens.generateTodaysDate('yyyy-mm-dd');
        }
        if (options.riskAssessmentAdditionalConditions !== undefined) {
            payload.additionalParameters.riskAssessmentAdditionalConditions =
                options.riskAssessmentAdditionalConditions;
        }

        const contract = await Request.post(Endpoints.productContract, { data: payload });
        await expect(contract).CheckResponse();
        Responses.productContract.push(await contract.json());
    });

    await test.step(`Activate POD ${podIndex}`, async () => {
        const podActivation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(
                podIndex,
                options.podActivationDate ?? options.activationDate,
                undefined,
                contractIndex
            ),
        });
        await expect(podActivation).CheckResponse();
    });

    await createProfileData(fixtures, podIndex);
}

async function prepareBillingDataWithPriceComponents(
    fixtures: PrepFixtures,
    options: {
        pcs: Array<{ name: string; condition: string | null }>;
        pod0?: PodOverride;
        pod1?: PodOverride;
        contractType?: string;
        podCount?: 1 | 2;
        customerType?: 'LEGAL_ENTITY' | 'PRIVATE_CUSTOMER';
        customerSegmentId?: number;
        customerPreferenceId?: number;
        customerDirectDebit?: boolean;
        campaignId?: number | string | null;
        activationDate?: string;
        podActivationDate?: string;
        riskAssessmentAdditionalConditions?: string[];
        productTypeId?: number;
    }
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const podCount = options.podCount ?? 2;

    await test.step('generate customer', async () => {
        const customerPayload =
            options.customerType === 'PRIVATE_CUSTOMER'
                ? GeneratePayload.customers.customer_private()
                : GeneratePayload.customers.customer_legal();
        if (options.customerSegmentId !== undefined) {
            customerPayload.segmentIds = [options.customerSegmentId];
        }
        if (options.customerPreferenceId !== undefined) {
            customerPayload.bankingDetails.preferenceIds = [options.customerPreferenceId];
        }
        if (options.customerDirectDebit !== undefined) {
            applyCustomerDirectDebit(customerPayload, options.customerDirectDebit);
        }
        const customer = await Request.post(Endpoints.customer, { data: customerPayload });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    await test.step('generate price components', async () => {
        for (const pc of options.pcs) {
            await createPriceComponent(fixtures, pc);
        }
    });

    await test.step('generate POD 0', async () => {
        const useSlp = options.pod0?.slp === true || options.pod0?.settlementPeriod === false;
        const payload = useSlp
            ? GeneratePayload.pointsOfDelivery.pod_slp()
            : GeneratePayload.pointsOfDelivery.pod_settlement();
        applyPodOverride(payload as unknown as Record<string, unknown>, options.pod0);
        const podSettlement = await Request.post(Endpoints.pod, { data: payload });
        await expect(podSettlement).CheckResponse();
        Responses.pod.push(await podSettlement.json());
    });

    if (podCount > 1) {
        await test.step('generate POD 1', async () => {
            const useSlp = options.pod1?.slp === true || options.pod1?.settlementPeriod === false;
            const payload = useSlp
                ? GeneratePayload.pointsOfDelivery.pod_slp()
                : GeneratePayload.pointsOfDelivery.pod_settlement();
            applyPodOverride(payload as unknown as Record<string, unknown>, options.pod1);
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
        const payload = GeneratePayload.productAndServices.product();
        if (options.productTypeId !== undefined) {
            payload.productTypeId = options.productTypeId;
        }
        const product = await Request.post(Endpoints.product, { data: payload });
        await expect(product).CheckResponse();
        Responses.product.push(await product.json());
    });

    await test.step('generate contract', async () => {
        const payload = await GeneratePayload.contractsAndOrders.product_contract(
            0,
            0,
            podCount === 1 ? 0 : undefined
        );
        if (options.contractType !== undefined) {
            payload.productParameters.contractType = options.contractType;
        }
        if (options.campaignId !== undefined) {
            payload.additionalParameters.campaignId = options.campaignId;
        }
        if (options.activationDate !== undefined) {
            payload.basicParameters.activationDate = options.activationDate;
        }
        if (options.riskAssessmentAdditionalConditions !== undefined) {
            payload.additionalParameters.riskAssessmentAdditionalConditions =
                options.riskAssessmentAdditionalConditions;
        }
        const contract = await Request.post(Endpoints.productContract, { data: payload });
        await expect(contract).CheckResponse();
        Responses.productContract.push(await contract.json());
    });

    await test.step('Activate POD 0', async () => {
        const podActivation = await Request.post('/contract-pods/manual', {
            data: await GeneratePayload.pointsOfDelivery.pod_activation(0, options.podActivationDate),
        });
        await expect(podActivation).CheckResponse();
    });

    if (podCount > 1) {
        await test.step('Activate POD 1', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {
                data: await GeneratePayload.pointsOfDelivery.pod_activation(1, options.podActivationDate),
            });
            await expect(podActivation).CheckResponse();
        });
    }

    await createProfileData(fixtures, 0);

    if (podCount > 1) {
        await createProfileData(fixtures, 1);
    }
}

async function createPrivateCustomerTopology(fixtures: PrepFixtures): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;

    await test.step('generate private customer', async () => {
        const customer = await Request.post(Endpoints.customer, {
            data: GeneratePayload.customers.customer_private(),
        });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    const customerIndex = Responses.customer.length - 1;
    const podIndex = await createAdditionalPod(fixtures);
    const contractIndex = Responses.productContract.length;

    await test.step(`generate product contract for private customer`, async () => {
        const payload = await GeneratePayload.contractsAndOrders.product_contract(
            customerIndex,
            0,
            podIndex
        );
        payload.basicParameters.customerId = Responses.customer[customerIndex].id;
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

async function getTwoContractPodIdentifiers(
    Request: PrepFixtures['Request'],
    Responses: PrepFixtures['Responses']
): Promise<{ pod0: string; pod1: string }> {
    const [pod0] = await getProductContractPodIdentifiers(Request, Responses, 0, [0]);
    const [pod1] = await getProductContractPodIdentifiers(Request, Responses, 1, [1]);
    return { pod0, pod1 };
}

async function runCustomerScopedBilling(
    fixtures: PrepFixtures,
    customerIndexes: number[] = [0],
    options?: { maxEndDate?: string }
): Promise<void> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const payload = await GeneratePayload.billing.billingRun('CUSTOMER');
    payload.basicParameters.billingCriteria = 'CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS';
    payload.basicParameters.billingApplicationLevel = 'CUSTOMER';
    payload.basicParameters.listOfCustomersContractsOrPOD = null;

    const customerNumberConditions = customerIndexes
        .map((index) => {
            const customerNumber =
                Responses.customer[index].customerNumber ?? Responses.customer[index].identifier;
            return `$CUSTOMER_NUMBER$=$${customerNumber}$`;
        })
        .join('OR');
    payload.basicParameters.customersContractOrPODConditions =
        customerIndexes.length > 1 ? `(${customerNumberConditions})` : customerNumberConditions;

    if (options?.maxEndDate) {
        payload.basicParameters.maxEndDate = options.maxEndDate;
    }

    const billingRun = await Request.post(Endpoints.billingRun, { data: payload });
    await expect(billingRun).CheckResponse();
    Responses.billingRun.push(await billingRun.json());
}

async function createAdditionalCustomer(
    fixtures: PrepFixtures,
    options: {
        type?: 'LEGAL_ENTITY' | 'PRIVATE_CUSTOMER';
        segmentId?: number;
        preferenceId?: number;
        directDebit?: boolean;
    } = {}
): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const type = options.type ?? 'LEGAL_ENTITY';

    await test.step(`generate ${type} customer`, async () => {
        const payload =
            type === 'PRIVATE_CUSTOMER'
                ? GeneratePayload.customers.customer_private()
                : GeneratePayload.customers.customer_legal();
        if (options.segmentId !== undefined) {
            payload.segmentIds = [options.segmentId];
        }
        if (options.preferenceId !== undefined) {
            payload.bankingDetails.preferenceIds = [options.preferenceId];
        }
        if (options.directDebit !== undefined) {
            applyCustomerDirectDebit(payload, options.directDebit);
        }

        const customer = await Request.post(Endpoints.customer, { data: payload });
        await expect(customer).CheckResponse();
        Responses.customer.push(await customer.json());
    });

    return Responses.customer.length - 1;
}

async function createProductSharingPriceComponents(
    fixtures: PrepFixtures,
    productTypeId?: number
): Promise<number> {
    const { Request, GeneratePayload, Responses, Endpoints } = fixtures;
    const productIndex = Responses.product.length;

    await test.step(`generate product ${productIndex} sharing price components`, async () => {
        const payload = GeneratePayload.productAndServices.product();
        payload.priceComponentIds = Responses.priceComponent.map((pc) =>
            typeof pc === 'object' && pc !== null && 'id' in pc
                ? (pc as { id: number }).id
                : (pc as number)
        );
        if (productTypeId !== undefined) {
            payload.productTypeId = productTypeId;
        }
        const product = await Request.post(Endpoints.product, { data: payload });
        await expect(product).CheckResponse();
        Responses.product.push(await product.json());
    });

    return productIndex;
}

type AddressIds = {
    countryId: number;
    regionId: number;
    municipalityId: number;
    populatedPlaceId: number;
    zipCodeId: number;
};

async function createAddressChain(
    Nomenclatures: baseFixture['Nomenclatures'],
    suffix: string,
    reuse: { country?: boolean; municipality?: boolean } = {}
): Promise<AddressIds> {
    const countryId = Number(
        reuse.country || reuse.municipality
            ? await Nomenclatures.countries('STANDARD')
            : await Nomenclatures.countries(`pcc-country-${suffix}`)
    );
    const regionId = Number(
        reuse.municipality
            ? await Nomenclatures.regions('STANDARD')
            : await Nomenclatures.regions(`pcc-region-${suffix}`)
    );
    const municipalityId = Number(
        reuse.municipality
            ? await Nomenclatures.municipalities('STANDARD')
            : await Nomenclatures.municipalities(`pcc-mun-${suffix}`)
    );
    const populatedPlaceId = Number(await Nomenclatures.populated_places(`pcc-place-${suffix}`));
    const zipCodeId = Number(await Nomenclatures.zip_codes(`pcc-zip-${suffix}`));
    return { countryId, regionId, municipalityId, populatedPlaceId, zipCodeId };
}

async function billAndValidatePcConditions(
    fixtures: PrepFixtures,
    validateInvoice: baseFixture['validateInvoice'],
    jiraKey: string,
    options: {
        customerIndexes?: number[];
        maxEndDate?: string;
        expectedInvoiceCount: number;
        included: Array<{ priceComponentName: string; podIdentifiers: string[] }>;
        excluded: Array<{ priceComponentName: string; podIdentifiers: string[] }>;
    }
): Promise<void> {
    await test.step('Billing run', async () => {
        await runCustomerScopedBilling(fixtures, options.customerIndexes ?? [0], {
            maxEndDate: options.maxEndDate,
        });
    });

    console.log(reportGenerator.setLinksToResponses(fixtures.Responses));

    await test.step('invoice generation', async () => {
        await fixtures.GeneratePayload.billing.waitForInvoiceGeneration(
            true,
            true,
            options.expectedInvoiceCount
        );
    });

    await test.step('validate PC conditions', async () => {
        await validateInvoice.priceComponentConditionsValidator({
            expectedInvoiceCount: options.expectedInvoiceCount,
            included: options.included,
            excluded: options.excluded,
        });
    });

    test.info().attach(`[${jiraKey}] response`, {
        body: JSON.stringify(reportGenerator.setLinksToResponses(fixtures.Responses), null, 2),
        contentType: 'application/json',
    });
}

test.describe('[REG-1064]: Price Conditions', { tag: ['@billing', '@price-component-conditions'] }, () => {
    // Pipeline runs REG-1293, REG-1294, and REG-1295. The other cases stay skipped.

    test.skip('[REG-1269]: PCC-01 [POD_VOLTAGE_LEVEL=LOW]', { tag: ['@pc-voltage'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                { name: 'PC_COND_VOLTAGE_LOW', condition: '$POD_VOLTAGE_LEVEL$=$LOW$' },
            ],
            pod0: { voltageLevel: 'LOW' },
            pod1: { voltageLevel: 'MEDIUM' },
        });

        await test.step('Billing run', async () => {
            await runCustomerScopedBilling(fixtures);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate PC conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.priceComponentConditionsValidator({
                expectedInvoiceCount: 1,
                included: [
                    { priceComponentName: 'PC_COND_VOLTAGE_LOW', podIdentifiers: [pod0] },
                    { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
                ],
                excluded: [
                    { priceComponentName: 'PC_COND_VOLTAGE_LOW', podIdentifiers: [pod1] },
                ],
            });
        });

        test.info().attach('[REG-1269] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1270]: PCC-02 [POD_VOLTAGE_LEVEL<>HIGH]', { tag: ['@pc-voltage'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                { name: 'PC_COND_VOLTAGE_NE_HIGH', condition: '$POD_VOLTAGE_LEVEL$<>$HIGH$' },
            ],
            pod0: { voltageLevel: 'LOW' },
            pod1: { voltageLevel: 'HIGH' },
        });

        await test.step('Billing run', async () => {
            await runCustomerScopedBilling(fixtures);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate PC conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.priceComponentConditionsValidator({
                expectedInvoiceCount: 1,
                included: [
                    { priceComponentName: 'PC_COND_VOLTAGE_NE_HIGH', podIdentifiers: [pod0] },
                    { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
                ],
                excluded: [
                    { priceComponentName: 'PC_COND_VOLTAGE_NE_HIGH', podIdentifiers: [pod1] },
                ],
            });
        });

        test.info().attach('[REG-1270] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1271]: PCC-03 [PURPOSE_OF_CONSUMPTION=NON_HOUSEHOLD]', { tag: ['@pc-purpose'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        // HOUSEHOLD + NON_HOUSEHOLD cannot share one product contract ("multiple consumption types")
        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_PURPOSE_NON_HH',
                    condition: '$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$',
                },
            ],
            podCount: 1,
            pod0: { consumptionPurpose: 'NON_HOUSEHOLD' },
        });

        await createAdditionalProductContract(fixtures, {
            podOverride: { consumptionPurpose: 'HOUSEHOLD' },
        });

        await test.step('Billing run', async () => {
            await runCustomerScopedBilling(fixtures);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 2);
        });

        await test.step('validate PC conditions', async () => {
            const [pod0] = await getProductContractPodIdentifiers(Request, Responses, 0, [0]);
            const [pod1] = await getProductContractPodIdentifiers(Request, Responses, 1, [1]);
            await validateInvoice.priceComponentConditionsValidator({
                expectedInvoiceCount: 2,
                included: [
                    { priceComponentName: 'PC_COND_PURPOSE_NON_HH', podIdentifiers: [pod0] },
                    { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
                ],
                excluded: [
                    { priceComponentName: 'PC_COND_PURPOSE_NON_HH', podIdentifiers: [pod1] },
                ],
            });
        });

        test.info().attach('[REG-1271] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1272]: PCC-04 [POD_MEASUREMENT_TYPE=SETTLEMENT_PERIOD]', { tag: ['@pc-measurement'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        // SETTLEMENT_PERIOD + SLP typically cannot share one product contract
        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_MEASUREMENT_SP',
                    condition: '$POD_MEASUREMENT_TYPE$=$SETTLEMENT_PERIOD$',
                },
            ],
            podCount: 1,
            pod0: { settlementPeriod: true, slp: false },
        });

        await createAdditionalProductContract(fixtures, {
            podOverride: {
                settlementPeriod: false,
                slp: true,
                measurementTypeId: envVariables.measurement_type,
            },
        });

        await test.step('Billing run', async () => {
            await runCustomerScopedBilling(fixtures);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 2);
        });

        await test.step('validate PC conditions', async () => {
            const [pod0] = await getProductContractPodIdentifiers(Request, Responses, 0, [0]);
            const [pod1] = await getProductContractPodIdentifiers(Request, Responses, 1, [1]);
            await validateInvoice.priceComponentConditionsValidator({
                expectedInvoiceCount: 2,
                included: [
                    { priceComponentName: 'PC_COND_MEASUREMENT_SP', podIdentifiers: [pod0] },
                    { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
                ],
                excluded: [
                    { priceComponentName: 'PC_COND_MEASUREMENT_SP', podIdentifiers: [pod1] },
                ],
            });
        });

        test.info().attach('[REG-1272] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1273]: PCC-05 [POD_GRID_OP]', { tag: ['@pc-grid-op'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const matchGridOpId = envVariables.grid_operator;
        const contrastGridOpId =
            matchGridOpId === ALT_GRID_OPERATOR_ID ? 1002 : ALT_GRID_OPERATOR_ID;

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_GRID_OP',
                    condition: `$POD_GRID_OP$=$${matchGridOpId}$`,
                },
            ],
            pod0: { gridOperatorId: matchGridOpId },
            pod1: { gridOperatorId: contrastGridOpId },
        });

        await test.step('Billing run', async () => {
            await runCustomerScopedBilling(fixtures);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate PC conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.priceComponentConditionsValidator({
                expectedInvoiceCount: 1,
                included: [
                    { priceComponentName: 'PC_COND_GRID_OP', podIdentifiers: [pod0] },
                    { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
                ],
                excluded: [
                    { priceComponentName: 'PC_COND_GRID_OP', podIdentifiers: [pod1] },
                ],
            });
        });

        test.info().attach('[REG-1273] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1274]: PCC-06 [CONTRACT_TYPE=COMBINED]', { tag: ['@pc-contract-type'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                { name: 'PC_COND_CONTRACT_COMBINED', condition: '$CONTRACT_TYPE$=$COMBINED$' },
            ],
            podCount: 1,
            contractType: 'COMBINED',
        });

        await createAdditionalProductContract(fixtures, { contractType: 'SUPPLY_ONLY' });

        await test.step('Billing run', async () => {
            await runCustomerScopedBilling(fixtures);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 2);
        });

        await test.step('validate PC conditions', async () => {
            const [pod0] = await getProductContractPodIdentifiers(Request, Responses, 0, [0]);
            const [pod1] = await getProductContractPodIdentifiers(Request, Responses, 1, [1]);
            await validateInvoice.priceComponentConditionsValidator({
                expectedInvoiceCount: 2,
                included: [
                    { priceComponentName: 'PC_COND_CONTRACT_COMBINED', podIdentifiers: [pod0] },
                    { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
                ],
                excluded: [
                    { priceComponentName: 'PC_COND_CONTRACT_COMBINED', podIdentifiers: [pod1] },
                ],
            });
        });

        test.info().attach('[REG-1274] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1275]: PCC-07 [CUSTOMER_TYPE=LEGAL_ENTITY]', { tag: ['@pc-customer-type'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                { name: 'PC_COND_CUSTOMER_LEGAL', condition: '$CUSTOMER_TYPE$=$LEGAL_ENTITY$' },
            ],
            podCount: 1,
            customerType: 'LEGAL_ENTITY',
        });

        await createPrivateCustomerTopology(fixtures);

        await test.step('Billing run', async () => {
            await runCustomerScopedBilling(fixtures, [0, 1]);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 2);
        });

        await test.step('validate PC conditions', async () => {
            const [pod0] = await getProductContractPodIdentifiers(Request, Responses, 0, [0]);
            const [pod1] = await getProductContractPodIdentifiers(Request, Responses, 1, [1]);
            await validateInvoice.priceComponentConditionsValidator({
                expectedInvoiceCount: 2,
                included: [
                    { priceComponentName: 'PC_COND_CUSTOMER_LEGAL', podIdentifiers: [pod0] },
                    { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
                ],
                excluded: [
                    { priceComponentName: 'PC_COND_CUSTOMER_LEGAL', podIdentifiers: [pod1] },
                ],
            });
        });

        test.info().attach('[REG-1275] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1276]: PCC-08 [CONTRACT_TYPE=COMBINED AND POD_VOLTAGE_LEVEL<>HIGH]', { tag: ['@pc-and'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_COMBINED_NE_HIGH',
                    condition: '$CONTRACT_TYPE$=$COMBINED$AND$POD_VOLTAGE_LEVEL$<>$HIGH$',
                },
            ],
            pod0: { voltageLevel: 'LOW' },
            pod1: { voltageLevel: 'HIGH' },
            contractType: 'COMBINED',
        });

        await test.step('Billing run', async () => {
            await runCustomerScopedBilling(fixtures);
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        await test.step('invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('validate PC conditions', async () => {
            const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);
            await validateInvoice.priceComponentConditionsValidator({
                expectedInvoiceCount: 1,
                included: [
                    { priceComponentName: 'PC_COND_COMBINED_NE_HIGH', podIdentifiers: [pod0] },
                    { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
                ],
                excluded: [
                    { priceComponentName: 'PC_COND_COMBINED_NE_HIGH', podIdentifiers: [pod1] },
                ],
            });
        });

        test.info().attach('[REG-1276] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json',
        });
    });

    test.skip('[REG-1277]: PCC-09 [CUSTOMER_SEGMENT]', { tag: ['@pc-customer-segment'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const stamp = Date.now();
        const matchSegmentId = Number(await Nomenclatures.segment(`pcc-seg-match-${stamp}`));
        const excludedSegmentId = Number(await Nomenclatures.segment(`pcc-seg-excl-${stamp}`));

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_CUSTOMER_SEGMENT',
                    condition: `$CUSTOMER_SEGMENT$=$${matchSegmentId}$`,
                },
            ],
            podCount: 1,
            customerSegmentId: matchSegmentId,
        });

        await createAdditionalCustomer(fixtures, { segmentId: excludedSegmentId });
        await createAdditionalProductContract(fixtures, { customerIndex: 1 });

        const { pod0, pod1 } = await getTwoContractPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1277', {
            customerIndexes: [0, 1],
            expectedInvoiceCount: 2,
            included: [
                { priceComponentName: 'PC_COND_CUSTOMER_SEGMENT', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_CUSTOMER_SEGMENT', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1278]: PCC-10 [CUSTOMER_PREFERENCES]', { tag: ['@pc-customer-preferences'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const stamp = Date.now();
        const matchPreferenceId = Number(await Nomenclatures.preferences(`pcc-pref-match-${stamp}`));
        const excludedPreferenceId = Number(await Nomenclatures.preferences(`pcc-pref-excl-${stamp}`));

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_CUSTOMER_PREFERENCES',
                    condition: `$CUSTOMER_PREFERENCES$=$${matchPreferenceId}$`,
                },
            ],
            podCount: 1,
            customerPreferenceId: matchPreferenceId,
        });

        await createAdditionalCustomer(fixtures, { preferenceId: excludedPreferenceId });
        await createAdditionalProductContract(fixtures, { customerIndex: 1 });

        const { pod0, pod1 } = await getTwoContractPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1278', {
            customerIndexes: [0, 1],
            expectedInvoiceCount: 2,
            included: [
                { priceComponentName: 'PC_COND_CUSTOMER_PREFERENCES', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_CUSTOMER_PREFERENCES', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1279]: PCC-11 [MONTHS_DIFFERENCE_BETWEEN_CURRENT_DATE_CONTRACT_ACTIVATION_DATE=0]', {
        tag: ['@pc-months-current-date'],
    }, async ({ Request, GeneratePayload, Responses, Endpoints, validateInvoice }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const currentMonthStart = randomGens.generateMonthStartDate('yyyy-mm-dd', 0);
        const olderMonthStart = randomGens.generateMonthStartDate('yyyy-mm-dd', -3);

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_MONTHS_CURRENT_DATE',
                    // Billing SQL returns 1 for the same calendar month (AGE months + 1), not 0.
                    condition: '$MONTHS_DIFFERENCE_BETWEEN_CURRENT_DATE_CONTRACT_ACTIVATION_DATE$=1',
                },
            ],
            podCount: 1,
            activationDate: currentMonthStart,
            podActivationDate: currentMonthStart,
        });

        await createAdditionalProductContract(fixtures, {
            activationDate: olderMonthStart,
            podActivationDate: olderMonthStart,
        });

        const { pod0, pod1 } = await getTwoContractPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1279', {
            expectedInvoiceCount: 2,
            included: [
                { priceComponentName: 'PC_COND_MONTHS_CURRENT_DATE', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_MONTHS_CURRENT_DATE', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1280]: PCC-12 [MONTHS_DIFFERENCE_BETWEEN_MAX_DATE_OF_BILLING_RUN_CONTRACT_ACTIVATION_DATE=0]', {
        tag: ['@pc-months-billing-run'],
    }, async ({ Request, GeneratePayload, Responses, Endpoints, validateInvoice }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const currentMonthStart = randomGens.generateMonthStartDate('yyyy-mm-dd', 0);
        const currentMonthEnd = randomGens.generateMonthEndDate('yyyy-mm-dd', 0);
        const olderMonthStart = randomGens.generateMonthStartDate('yyyy-mm-dd', -3);

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_MONTHS_BILLING_RUN',
                    condition: '$MONTHS_DIFFERENCE_BETWEEN_MAX_DATE_OF_BILLING_RUN_CONTRACT_ACTIVATION_DATE$=0',
                },
            ],
            podCount: 1,
            activationDate: currentMonthStart,
            podActivationDate: currentMonthStart,
        });

        await createAdditionalProductContract(fixtures, {
            activationDate: olderMonthStart,
            podActivationDate: olderMonthStart,
        });

        const { pod0, pod1 } = await getTwoContractPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1280', {
            maxEndDate: currentMonthEnd,
            expectedInvoiceCount: 2,
            included: [
                { priceComponentName: 'PC_COND_MONTHS_BILLING_RUN', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_MONTHS_BILLING_RUN', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1281]: PCC-13 [POD_ADDITIONAL_PARAMETER]', { tag: ['@pc-pod-additional-parameter'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const stamp = Date.now();
        const matchParamId = Number(await Nomenclatures.pod_additional_parameters(`pcc-pap-match-${stamp}`));
        const excludedParamId = Number(await Nomenclatures.pod_additional_parameters(`pcc-pap-excl-${stamp}`));

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_POD_ADDITIONAL_PARAMETER',
                    condition: `$POD_ADDITIONAL_PARAMETER$=$${matchParamId}$`,
                },
            ],
            pod0: { podAdditionalParameters: [matchParamId] },
            pod1: { podAdditionalParameters: [excludedParamId] },
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1281', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_POD_ADDITIONAL_PARAMETER', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_POD_ADDITIONAL_PARAMETER', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1282]: PCC-14 [POD_COUNTRY]', { tag: ['@pc-pod-country'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const stamp = String(Date.now());
        const matchAddress = await createAddressChain(Nomenclatures, `match-${stamp}`);
        const excludedAddress = await createAddressChain(Nomenclatures, `excl-${stamp}`);

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_POD_COUNTRY',
                    condition: `$POD_COUNTRY$=$${matchAddress.countryId}$`,
                },
            ],
            pod0: { address: matchAddress },
            pod1: { address: excludedAddress },
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1282', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_POD_COUNTRY', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_POD_COUNTRY', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1283]: PCC-15 [POD_REGION]', { tag: ['@pc-pod-region'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const stamp = String(Date.now());
        const matchAddress = await createAddressChain(Nomenclatures, `match-${stamp}`, { country: true });
        const excludedAddress = await createAddressChain(Nomenclatures, `excl-${stamp}`, { country: true });

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_POD_REGION',
                    condition: `$POD_REGION$=$${matchAddress.regionId}$`,
                },
            ],
            pod0: { address: matchAddress },
            pod1: { address: excludedAddress },
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1283', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_POD_REGION', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_POD_REGION', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1284]: PCC-16 [POD_POPULATED_PLACE]', { tag: ['@pc-pod-populated-place'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const stamp = String(Date.now());
        const matchAddress = await createAddressChain(Nomenclatures, `match-${stamp}`, { municipality: true });
        const excludedAddress = await createAddressChain(Nomenclatures, `excl-${stamp}`, { municipality: true });

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_POD_POPULATED_PLACE',
                    condition: `$POD_POPULATED_PLACE$=$${matchAddress.populatedPlaceId}$`,
                },
            ],
            pod0: { address: matchAddress },
            pod1: { address: excludedAddress },
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1284', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_POD_POPULATED_PLACE', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_POD_POPULATED_PLACE', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1285]: PCC-17 [POD_PROVIDED_POWER]', { tag: ['@pc-pod-provided-power'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_POD_PROVIDED_POWER',
                    condition: `$POD_PROVIDED_POWER$=${PROVIDED_POWER_MATCH}`,
                },
            ],
            pod0: { providedPower: PROVIDED_POWER_MATCH },
            pod1: { providedPower: PROVIDED_POWER_CONTRAST },
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1285', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_POD_PROVIDED_POWER', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_POD_PROVIDED_POWER', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1286]: PCC-18 [POD_MULTIPLIER]', { tag: ['@pc-pod-multiplier'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_POD_MULTIPLIER',
                    condition: `$POD_MULTIPLIER$=${MULTIPLIER_MATCH}`,
                },
            ],
            pod0: { multiplier: MULTIPLIER_MATCH },
            pod1: { multiplier: MULTIPLIER_CONTRAST },
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1286', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_POD_MULTIPLIER', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_POD_MULTIPLIER', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1287]: PCC-19 [DIRECT_DEBIT=$YES$]', { tag: ['@pc-direct-debit'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                { name: 'PC_COND_DIRECT_DEBIT_YES', condition: '$DIRECT_DEBIT$=$YES$' },
            ],
            podCount: 1,
            customerDirectDebit: true,
        });

        await createAdditionalCustomer(fixtures, { directDebit: false });
        await createAdditionalProductContract(fixtures, { customerIndex: 1 });

        const { pod0, pod1 } = await getTwoContractPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1287', {
            customerIndexes: [0, 1],
            expectedInvoiceCount: 2,
            included: [
                { priceComponentName: 'PC_COND_DIRECT_DEBIT_YES', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_DIRECT_DEBIT_YES', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1288]: PCC-20 [CONTRACT_SUB_STATUS_IN_PERPETUITY=$NO$]', { tag: ['@pc-contract-perpetuity'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_PERPETUITY_YES',
                    condition: '$CONTRACT_SUB_STATUS_IN_PERPETUITY$=$YES$',
                },
                {
                    name: 'PC_COND_PERPETUITY_NO',
                    condition: '$CONTRACT_SUB_STATUS_IN_PERPETUITY$=$NO$',
                },
            ],
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1288', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_PERPETUITY_NO', podIdentifiers: [pod0, pod1] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_PERPETUITY_YES', podIdentifiers: [pod0, pod1] }],
        });
    });

    test.skip('[REG-1289]: PCC-21 [ACTIVE_POWER_SUPPLY_TERMINATION=$NO$]', {
        tag: ['@pc-active-power-supply-termination'],
    }, async ({ Request, GeneratePayload, Responses, Endpoints, validateInvoice }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_APS_TERMINATION_YES',
                    condition: '$ACTIVE_POWER_SUPPLY_TERMINATION$=$YES$',
                },
                {
                    name: 'PC_COND_APS_TERMINATION_NO',
                    condition: '$ACTIVE_POWER_SUPPLY_TERMINATION$=$NO$',
                },
            ],
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1289', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_APS_TERMINATION_NO', podIdentifiers: [pod0, pod1] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [
                { priceComponentName: 'PC_COND_APS_TERMINATION_YES', podIdentifiers: [pod0, pod1] },
            ],
        });
    });

    test.skip('[REG-1290]: PCC-22 [PRODUCT_TYPE]', { tag: ['@pc-product-type'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const stamp = Date.now();
        const matchProductTypeId = Number(await Nomenclatures.product_type(`pcc-ptype-match-${stamp}`));
        const excludedProductTypeId = Number(await Nomenclatures.product_type(`pcc-ptype-excl-${stamp}`));

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_PRODUCT_TYPE',
                    condition: `$PRODUCT_TYPE$=$${matchProductTypeId}$`,
                },
            ],
            podCount: 1,
            productTypeId: matchProductTypeId,
        });

        await createProductSharingPriceComponents(fixtures, excludedProductTypeId);
        await createAdditionalProductContract(fixtures, { productIndex: 1 });

        const { pod0, pod1 } = await getTwoContractPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1290', {
            expectedInvoiceCount: 2,
            included: [
                { priceComponentName: 'PC_COND_PRODUCT_TYPE', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_PRODUCT_TYPE', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1291]: PCC-23 [RISK_ASSESSMENT_ADDITIONAL_CONDITIONS]', {
        tag: ['@pc-risk-assessment'],
    }, async ({ Request, GeneratePayload, Responses, Endpoints, Nomenclatures, validateInvoice }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const stamp = Date.now();
        const matchRiskId = Number(await Nomenclatures.risk_assesment(`pcc-risk-match-${stamp}`));
        const excludedRiskId = Number(await Nomenclatures.risk_assesment(`pcc-risk-excl-${stamp}`));

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_RISK_ASSESSMENT',
                    condition: `$RISK_ASSESSMENT_ADDITIONAL_CONDITIONS$=$${matchRiskId}$`,
                },
            ],
            podCount: 1,
            riskAssessmentAdditionalConditions: [String(matchRiskId)],
        });

        await createAdditionalProductContract(fixtures, {
            riskAssessmentAdditionalConditions: [String(excludedRiskId)],
        });

        const { pod0, pod1 } = await getTwoContractPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1291', {
            expectedInvoiceCount: 2,
            included: [
                { priceComponentName: 'PC_COND_RISK_ASSESSMENT', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_RISK_ASSESSMENT', podIdentifiers: [pod1] }],
        });
    });

    test.skip('[REG-1292]: PCC-24 [CONTRACT_CAMPAIGN]', { tag: ['@pc-contract-campaign'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        Nomenclatures,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };
        const stamp = Date.now();
        const matchCampaignId = Number(await Nomenclatures.campaign(`pcc-camp-match-${stamp}`));
        const excludedCampaignId = Number(await Nomenclatures.campaign(`pcc-camp-excl-${stamp}`));

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_CONTRACT_CAMPAIGN',
                    condition: `$CONTRACT_CAMPAIGN$=$${matchCampaignId}$`,
                },
            ],
            podCount: 1,
            campaignId: matchCampaignId,
        });

        await createAdditionalProductContract(fixtures, { campaignId: excludedCampaignId });

        const { pod0, pod1 } = await getTwoContractPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1292', {
            expectedInvoiceCount: 2,
            included: [
                { priceComponentName: 'PC_COND_CONTRACT_CAMPAIGN', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_CONTRACT_CAMPAIGN', podIdentifiers: [pod1] }],
        });
    });

    // ─── Combination cases ────────────────────────────────────────────────────
    // Portable operators (Confluence Phase 2 - Price component conditions): IN / AND / OR / NOT.
    // Same product contract cannot mix HOUSEHOLD + NON_HOUSEHOLD or SETTLEMENT + SLP.
    // Split PODs by voltage only; purpose stays NON_HOUSEHOLD; measurement stays SETTLEMENT_PERIOD.
    // Runtime token form matches existing PCC-08 / billingConditions COMB (no spaces around AND/OR/IN).

    test('[REG-1293]: PCC-25 [IN AND OR NOT match one POD]', { tag: ['@billing', '@price-component-conditions', '@pc-combination'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_COMB_MATCH_ONE',
                    // (voltage IN (LOW, HIGH) AND purpose=NON_HOUSEHOLD AND NOT voltage=HIGH)
                    // OR (voltage=MEDIUM AND measurement=SETTLEMENT)
                    // AND contract=COMBINED
                    condition:
                        '(($POD_VOLTAGE_LEVEL$IN($LOW$,$HIGH$)AND$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$AND(NOT($POD_VOLTAGE_LEVEL$=$HIGH$)))OR($POD_VOLTAGE_LEVEL$=$MEDIUM$AND$POD_MEASUREMENT_TYPE$=$SETTLEMENT_PERIOD$))AND$CONTRACT_TYPE$=$COMBINED$',
                },
            ],
            pod0: { voltageLevel: 'LOW', consumptionPurpose: 'NON_HOUSEHOLD' },
            pod1: { voltageLevel: 'HIGH', consumptionPurpose: 'NON_HOUSEHOLD' },
            contractType: 'COMBINED',
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1293', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_COMB_MATCH_ONE', podIdentifiers: [pod0] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [{ priceComponentName: 'PC_COND_COMB_MATCH_ONE', podIdentifiers: [pod1] }],
        });
    });

    test('[REG-1294]: PCC-26 [IN AND OR NOT match both PODs via OR]', { tag: ['@billing', '@price-component-conditions', '@pc-combination'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_COMB_MATCH_BOTH',
                    // Same operator tree as PCC-25. LOW matches the IN/AND/NOT branch;
                    // MEDIUM matches the OR (voltage=MEDIUM AND SETTLEMENT) branch.
                    condition:
                        '(($POD_VOLTAGE_LEVEL$IN($LOW$,$HIGH$)AND$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$AND(NOT($POD_VOLTAGE_LEVEL$=$HIGH$)))OR($POD_VOLTAGE_LEVEL$=$MEDIUM$AND$POD_MEASUREMENT_TYPE$=$SETTLEMENT_PERIOD$))AND$CONTRACT_TYPE$=$COMBINED$',
                },
            ],
            pod0: { voltageLevel: 'LOW', consumptionPurpose: 'NON_HOUSEHOLD' },
            pod1: { voltageLevel: 'MEDIUM', consumptionPurpose: 'NON_HOUSEHOLD' },
            contractType: 'COMBINED',
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1294', {
            expectedInvoiceCount: 1,
            included: [
                { priceComponentName: 'PC_COND_COMB_MATCH_BOTH', podIdentifiers: [pod0, pod1] },
                { priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] },
            ],
            excluded: [],
        });
    });

    test('[REG-1295]: PCC-27 [IN AND OR NOT no match]', { tag: ['@billing', '@price-component-conditions', '@pc-combination'] }, async ({
        Request,
        GeneratePayload,
        Responses,
        Endpoints,
        validateInvoice,
    }) => {
        test.setTimeout(10 * 60 * 1000);
        const fixtures = { Request, GeneratePayload, Responses, Endpoints };

        await prepareBillingDataWithPriceComponents(fixtures, {
            pcs: [
                { name: PC_BASE, condition: null },
                {
                    name: 'PC_COND_COMB_NO_MATCH',
                    // Neither POD satisfies:
                    // (voltage IN (HIGH, MEDIUM_DIRECT_CONNECTED) AND purpose=NON_HOUSEHOLD AND NOT voltage=LOW)
                    // OR (voltage=LOW AND measurement=SLP)
                    // AND contract=COMBINED
                    condition:
                        '(($POD_VOLTAGE_LEVEL$IN($HIGH$,$MEDIUM_DIRECT_CONNECTED$)AND$PURPOSE_OF_CONSUMPTION$=$NON_HOUSEHOLD$AND(NOT($POD_VOLTAGE_LEVEL$=$LOW$)))OR($POD_VOLTAGE_LEVEL$=$LOW$AND$POD_MEASUREMENT_TYPE$=$SLP$))AND$CONTRACT_TYPE$=$COMBINED$',
                },
            ],
            pod0: { voltageLevel: 'LOW', consumptionPurpose: 'NON_HOUSEHOLD' },
            pod1: { voltageLevel: 'MEDIUM', consumptionPurpose: 'NON_HOUSEHOLD' },
            contractType: 'COMBINED',
        });

        const { pod0, pod1 } = await getPodIdentifiers(Request, Responses);

        await billAndValidatePcConditions(fixtures, validateInvoice, 'REG-1295', {
            expectedInvoiceCount: 1,
            included: [{ priceComponentName: PC_BASE, podIdentifiers: [pod0, pod1] }],
            excluded: [{ priceComponentName: 'PC_COND_COMB_NO_MATCH', podIdentifiers: [pod0, pod1] }],
        });
    });
});
