
import { test, expect } from '../../../backend/fixtures/baseFixture';
import type { baseFixture } from '../../../backend/fixtures/baseFixture';
import { createResponsesContainer } from '../../../backend/fixtures/types/responses';
import { GeneratePayload as GeneratePayloadClass } from '../../../backend/jsons/payloadGenerators/PayloadGenerator';
import { envVariables } from '../../../backend/fixtures/envCashed';
import { randomGens } from '../../../backend/utils/randomGens';
import reportGenerator from '../../../backend/utils/generateReport';
import priceParameterMP from '../../../backend/mass-imports/generators/domains/priceParameterMP';
import {
    COMBINED_MATCH_STASH_ID,
    COMBINED_MATCH_VOLUME,
    COMBINED_MATCH_PERCENT,
    COMBINED_MATCH_PRICE,
    COMBINED_MATCH_SCALE_DIFFERENCE,
    COMBINED_MATCH_SCALE_MULTIPLIER,
    COMBINED_MISMATCH_STASH_ID,
    COMBINED_MISMATCH_SCALE_VOLUME,
    COMBINED_MISMATCH_PROFILE_VOLUME,
    COMBINED_MISMATCH_PERCENT,
    COMBINED_MISMATCH_PRICE,
    COMBINED_MISMATCH_SCALE_DIFFERENCE,
    COMBINED_MISMATCH_SCALE_MULTIPLIER,
} from '../forVolumes/combined.fixtures';

type DataPrepFixtures = Pick<baseFixture, 'Request' | 'Endpoints' | 'Nomenclatures' | 'saveResponsesToFile' | 'FileUploadRequest'>;

export class volumesDataPrep {
    private fixtures: DataPrepFixtures;
    private stashKey: string;

    constructor(fixtures: DataPrepFixtures, stashKey: string) {
        this.fixtures = fixtures;
        this.stashKey = stashKey;
    }

    // REG-713: For volumes - simple settlement | happy pass
    async REG_713(): Promise<void> {
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

        saveResponsesToFile(Responses, this.stashKey, 'REG-713');

        test.info().attach('[REG-713] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-873: For volumes - Scale with scale code | happy pass
    async REG_873(): Promise<void> {
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
            const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            const podJsonBody = await podGet.json();
            const payloadMeter = GeneratePayload.pointsOfDelivery.meters();
            const scaleCode = await Nomenclatures.scales_code(grid);
            payloadMeter.gridOperatorId = podJsonBody.gridOperatorId;
            payloadMeter.podId = podJsonBody.id;
            payloadMeter.meterScales = [scaleCode];
            const meter = await Request.post(Endpoints.meters, {data: payloadMeter});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate penalty', async () => {
            const penalty = await Request.post(Endpoints.penalty, {data: GeneratePayload.productAndServices.penalty()});
            await expect(penalty).CheckResponse();
            Responses.penalty.push(await penalty.json());
        });

        await test.step('generate termination', async () => {
            const termination = await Request.post(Endpoints.termination, {data: GeneratePayload.productAndServices.termination('EXPIRATION_OF_THE_CONTRACT_TERM')});
            await expect(termination).CheckResponse();
            Responses.termination.push(await termination.json());
        });

        await test.step('generate price component with scale code', async () => {
            const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
            const scaleCode = await Nomenclatures.scales_code(grid);
            const payload = GeneratePayload.productAndServices.scaleComponent();
            payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleCode;
            const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(priceComp).CheckResponse();
            Responses.priceComponent.push(await priceComp.json());
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

        await test.step('Data by scales', async () => {
            const payload = await GeneratePayload.energyData.scaleCode();
            const scales = await Request.post(Endpoints.dataByScales, {data: payload});
            await expect(scales).CheckResponse();
            Responses.dataByScales.push(await scales.json());
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-873');

        test.info().attach('[REG-873] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-963: Profile flow with VAT base
    async REG_963(): Promise<void> {
        const { Request, Endpoints, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate customer for vat base', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price component', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 100;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate vat base price component', async () => {
            const vatBasePrice = GeneratePayload.productAndServices.priceSettlement();
            vatBasePrice.doNotIncludeVatBase = true;
            vatBasePrice.customerDetailId = Responses.customer[1].lastCustomerDetailId;
            vatBasePrice.formulaRequest.expression = 10;
            const vatBase = await Request.post(Endpoints.priceComponent, {data: vatBasePrice});
            await expect(vatBase).CheckResponse();
            Responses.priceComponent.push(await vatBase.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate POD 1', async () => {
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
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Data by profiles', async () => {
            const payload = await GeneratePayload.energyData.profile1Month();
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

        saveResponsesToFile(Responses, this.stashKey, 'REG-963');

        test.info().attach('[REG-963] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-962: Data by profile - all profile types (15min, 1h, 1day, 1month)
    async REG_962(): Promise<void> {
        const { Request, Endpoints, Nomenclatures, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        const profile1 = await Nomenclatures.profiles('15 MINUTES');
        const profile2 = await Nomenclatures.profiles('1 HOUR');
        const profile3 = await Nomenclatures.profiles('1 DAY');
        const profile4 = await Nomenclatures.profiles('1 MONTH');

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate price components', async () => {
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = 100;

            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = profile1;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();

            payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_TWO';
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = profile2;
            const price2 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price2).CheckResponse();

            payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_THREE';
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = profile3;
            const price3 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price3).CheckResponse();

            payload.formulaRequest.issuedSeparateInvoice = 'INVOICE_FOUR';
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = profile4;
            const price4 = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price4).CheckResponse();

            Responses.priceComponent.push(await price.json());
            Responses.priceComponent.push(await price2.json());
            Responses.priceComponent.push(await price3.json());
            Responses.priceComponent.push(await price4.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate 4 settlement pods', async () => {
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();

            payload.name = '15 min pod';
            const podSettlement1 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});

            payload.name = '1 hour pod';
            const podSettlement2 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});

            payload.name = '1 day pod';
            const podSettlement3 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});

            payload.name = '1 month pod';
            const podSettlement4 = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});

            await expect(podSettlement1).CheckResponse();
            await expect(podSettlement2).CheckResponse();
            await expect(podSettlement3).CheckResponse();
            await expect(podSettlement4).CheckResponse();

            Responses.pod.push(await podSettlement1.json());
            Responses.pod.push(await podSettlement2.json());
            Responses.pod.push(await podSettlement3.json());
            Responses.pod.push(await podSettlement4.json());
        });

        await test.step('generate product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            for (let i = 0; i < Responses.pod.length; i++) {
                const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(i)});
                await expect(podActivation).CheckResponse();
            }
        });

        await test.step('Data by profiles 1 month', async () => {
            const payload = await GeneratePayload.energyData.profile1Month(0);
            payload.profileId = profile4;
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

        await test.step('Data by profiles 1 hour', async () => {
            const payload = await GeneratePayload.energyData.profile1Hour(1);
            payload.profileId = profile2;
            const profiles = await Request.post('billing-by-profile', {data: payload});
            await expect(profiles).CheckResponse();
            const profileData = await profiles.json();
            Responses.dataByProfiles.push({
                id: profileData,
                periodFrom: payload.periodFrom,
                periodTo: payload.periodTo,
                periodType: payload.periodType
            });
            await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'HOUR');
        });

        await test.step('Data by profiles 1 day', async () => {
            const payload = await GeneratePayload.energyData.profile1Day(2);
            payload.profileId = profile3;
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

        await test.step('Data by profiles 15 min', async () => {
            const payload = await GeneratePayload.energyData.profile15minute(3);
            payload.profileId = profile1;
            const profiles = await Request.post('billing-by-profile', {data: payload});
            await expect(profiles).CheckResponse();
            const profileData = await profiles.json();
            Responses.dataByProfiles.push({
                id: profileData,
                periodFrom: payload.periodFrom,
                periodTo: payload.periodTo,
                periodType: payload.periodType
            });
            await GeneratePayload.energyData.uploadDataByProfileFile(profileData, 'MIN');
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            const billingRunResponse = await billingRun.json();
            Responses.billingRun.push({
                id: billingRunResponse,
                invoiceNumbers: 4
            });
        });

        console.log(reportGenerator.setLinksToResponses(Responses));

        saveResponsesToFile(Responses, this.stashKey, 'REG-962');

        test.info().attach('[REG-962] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-992: Scale with tariff - splitting | happy pass
    async REG_992(): Promise<void> {
        const { Request, Endpoints, Nomenclatures, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('POD create', async () => {
            const grid = await Nomenclatures.grid_operator('GIO');
            const measurementType = await Nomenclatures.measurement_type('GIO', grid);
            const payload = GeneratePayload.pointsOfDelivery.pod_slp();
            payload.gridOperatorId = grid;
            payload.measurementTypeId = measurementType;
            const podSlp = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSlp).CheckResponse();
            Responses.pod.push(await podSlp.json());
        });

        await test.step('Meters create', async () => {
            const grid = await Nomenclatures.grid_operator('GIO');
            const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            const podJsonBody = await podGet.json();
            const payloadMeter = GeneratePayload.pointsOfDelivery.meters();
            const scaleCode = await Nomenclatures.scales_code('scaleCode');
            const scalesTariff = await Nomenclatures.scales_tariff('GIO');
            payloadMeter.gridOperatorId = podJsonBody.gridOperatorId;
            payloadMeter.podId = podJsonBody.id;
            payloadMeter.meterScales = [scaleCode, scalesTariff];
            const meter = await Request.post(Endpoints.meters, {data: payloadMeter});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate price component with scale code', async () => {
            const scaleTariff = await Nomenclatures.scales_tariff('GIO');
            const scaleCode = await Nomenclatures.scales_code('scaleCode');
            const payload = GeneratePayload.productAndServices.scaleComponent();
            payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleTariff;
            payload.applicationModelRequest.volumesByScaleRequest.scaleIds[1] = scaleCode;
            const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(priceComp).CheckResponse();
            Responses.priceComponent.push(await priceComp.json());
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

        await test.step('Data by scales', async () => {
            const payload = await GeneratePayload.energyData.scaleTariff();
            const scales = await Request.post(Endpoints.dataByScales, {data: payload});
            await expect(scales).CheckResponse();
            Responses.dataByScales.push(await scales.json());
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-992');

        test.info().attach('[REG-992] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1013: Scale with tariff - splitting | calculation for number of days
    async REG_1013(): Promise<void> {
        const { Request, Endpoints, Nomenclatures, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('POD create', async () => {
            const grid = await Nomenclatures.grid_operator('me');
            const measurementType = await Nomenclatures.measurement_type('me', grid);
            const payload = GeneratePayload.pointsOfDelivery.pod_slp();
            payload.gridOperatorId = grid;
            payload.measurementTypeId = measurementType;
            const podSlp = await Request.post(Endpoints.pod, {data: payload});
            await expect(podSlp).CheckResponse();
            Responses.pod.push(await podSlp.json());
        });

        await test.step('Meters create', async () => {
            const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            const podJsonBody = await podGet.json();
            const payloadMeter = GeneratePayload.pointsOfDelivery.meters();
            const scaleCode = await Nomenclatures.scales_code_numberOfDays('scaleCodenumberOfDays');
            const scalesTariff = await Nomenclatures.scales_tariff_numberOfDays('grid');
            payloadMeter.gridOperatorId = podJsonBody.gridOperatorId;
            payloadMeter.podId = podJsonBody.id;
            payloadMeter.meterScales = [scaleCode, scalesTariff];
            const meter = await Request.post(Endpoints.meters, {data: payloadMeter});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate price component with scale code', async () => {
            const scaleTariff = await Nomenclatures.scales_tariff_numberOfDays('grid');
            const scaleCode = await Nomenclatures.scales_code_numberOfDays('scaleCodenumberOfDays');
            const payload = GeneratePayload.productAndServices.scaleComponent();
            payload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = scaleTariff;
            payload.applicationModelRequest.volumesByScaleRequest.scaleIds[1] = scaleCode;
            const priceComp = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(priceComp).CheckResponse();
            Responses.priceComponent.push(await priceComp.json());
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

        await test.step('Data by scales', async () => {
            const payload = await GeneratePayload.energyData.scaleTariff();
            const scales = await Request.post(Endpoints.dataByScales, {data: payload});
            await expect(scales).CheckResponse();
            Responses.dataByScales.push(await scales.json());
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1013');

        test.info().attach('[REG-1013] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1029: Price profile flow
    async REG_1029(): Promise<void> {
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
            payload.formulaRequest.expression = '$PRICE_PROFILE$';
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

        await test.step('Data by profiles', async () => {
            const payload = await GeneratePayload.energyData.profile1Month();
            payload.timeZone = 'CET';
            payload.profileId = 3;
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

        saveResponsesToFile(Responses, this.stashKey, 'REG-1029');

        test.info().attach('[REG-1029] response', {
            body: JSON.stringify(reportGenerator.setLinksToResponses(Responses), null, 2),
            contentType: 'application/json'
        });
    }

    // REG-1037: Splitting when total amount of scale codes is zero and "Number of days" checkbox not selected
    async REG_1037(): Promise<void> {
        const { Request, Endpoints, Nomenclatures, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('create term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('create POD', async () => {
            const grid = await Nomenclatures.grid_operator('splitting');
            const payload = GeneratePayload.pointsOfDelivery.pod_settlement();
            payload.gridOperatorId = grid;
            const PodSettlement = await Request.post(Endpoints.pod, {data: payload});
            await expect(PodSettlement).CheckResponse();
            Responses.pod.push(await PodSettlement.json());
        });

        await test.step('create meter', async () => {
            const scaleCode = await Nomenclatures.scales_code_withoutCheckbox('Scale splitting');
            const tariff = await Nomenclatures.scales_tariff_withoutCheckbox('tariff splitting');
            const getpod = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            const podJson = await getpod.json();
            const Meters = GeneratePayload.pointsOfDelivery.meters();
            Meters.podId = podJson.id;
            Meters.gridOperatorId = podJson.gridOperatorId;
            Meters.meterScales = [scaleCode, tariff];
            const meter = await Request.post(Endpoints.meters, {data: Meters});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('create scale', async () => {
            const scalePayload = await GeneratePayload.energyData.scaleZero();
            const scale = await Request.post(Endpoints.dataByScales, {data: scalePayload});
            await expect(scale).CheckResponse();
            Responses.dataByScales.push(await scale.json());
        });

        await test.step('create scale price component', async () => {
            const tariff = await Nomenclatures.scales_tariff_withoutCheckbox('tariff splitting');
            const priceCompPayload = GeneratePayload.productAndServices.scaleComponent();
            priceCompPayload.applicationModelRequest.volumesByScaleRequest.scaleIds[0] = tariff;
            const createScale = await Request.post(Endpoints.priceComponent, {data: priceCompPayload});
            await expect(createScale).CheckResponse();
            Responses.priceComponent.push(await createScale.json());
        });

        await test.step('create product', async () => {
            const productPayload = GeneratePayload.productAndServices.product();
            const createProduct = await Request.post(Endpoints.product, {data: productPayload});
            await expect(createProduct).CheckResponse();
            Responses.product.push(await createProduct.json());
        });

        await test.step('create contract', async () => {
            const contractPayload = await GeneratePayload.contractsAndOrders.product_contract();
            const createContract = await Request.post(Endpoints.productContract, {data: contractPayload});
            await expect(createContract).CheckResponse();
            Responses.productContract.push(await createContract.json());
        });

        await test.step('pod activation', async () => {
            await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
        });

        await test.step('create billing', async () => {
            const createBilling = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(createBilling).CheckResponse();
            Responses.billingRun.push(await createBilling.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1037');
    }

    // REG-718: Interim deduction (from same period)
    async REG_718(): Promise<void> {
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

        await test.step('Billing run - interim and advance payment', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        await test.step('Wait for invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('Billing run - for volumes', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        await test.step('Wait for invoice generation (for volumes)', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 1);
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-718');
    }

    // REG-718 longer payment: Interim deduction from first invoice with longer payment term.
    // Same chain as REG-718 except IAP deductionFrom + own 1 WD term (strictly earlier due date).
    // Date of issue stays PERIODICAL (default) so a standalone INTERIM billing run issues the IAP.
    // MATCH_THE_INVOICE_DATE is rejected with a calendar block, and without PERIODICAL days
    // the INTERIM run reaches DRAFT with 0 invoices (Phoenix Dev 2026-08-17).
    async REG_718_LONGER_PAYMENT(): Promise<void> {
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

        await test.step('generate interim (longer payment term deduction, issuing month current)', async () => {
            const payload = GeneratePayload.productAndServices.interim();
            payload.value = 100;
            payload.paymentType = 'OBLIGATORY';
            payload.issuingForTheMonthToCurrent = 'ZERO';
            payload.deductionFrom = 'FIRST_INVOICE_WITH_LONGER_PAYMENT_TERM';
            payload.matchesWithTermOfStandardInvoice = false;
            payload.interimAdvancePaymentTerm.value = 1;
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

        await test.step('Billing run - interim and advance payment', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['INTERIM_AND_ADVANCE_PAYMENT'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        await test.step('Wait for invoice generation', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration();
        });

        await test.step('Billing run - for volumes', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun('CONTRACT', ['FOR_VOLUMES'])});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        await test.step('Wait for invoice generation (for volumes)', async () => {
            await GeneratePayload.billing.waitForInvoiceGeneration(true, true, 1, 1);
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-718-LONGER');
    }

    // REG-974: For volumes - Combined flow (scale and profile do not match)
    async REG_974(): Promise<void> {
        const { Request, Endpoints, Nomenclatures, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate POD', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('Meters create', async () => {
            const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
            const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            await expect(podGet).CheckResponse();
            const podJsonBody = await podGet.json();
            const payloadMeter = GeneratePayload.pointsOfDelivery.meters();
            const scaleCode = await Nomenclatures.scales_code(grid);
            payloadMeter.gridOperatorId = podJsonBody.gridOperatorId;
            payloadMeter.podId = podJsonBody.id;
            payloadMeter.meterScales = [scaleCode];
            const meter = await Request.post(Endpoints.meters, {data: payloadMeter});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('generate price component', async () => {
            const izmereno = await Nomenclatures.profiles('Измерено количество');
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = String(COMBINED_MISMATCH_PRICE);
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = izmereno;
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage = COMBINED_MISMATCH_PERCENT;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.productParameters.contractType = 'COMBINED';
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        let alignedProfilePayload: any;

        await test.step('Data by scales (total does not equal profile)', async () => {
            alignedProfilePayload = await GeneratePayload.energyData.profile1Month();
            const rangeFrom = String(alignedProfilePayload.periodFrom).slice(0, 10);
            const rangeTo = String(alignedProfilePayload.periodTo).slice(0, 10);
            const payload = await GeneratePayload.energyData.scaleCode(0, rangeFrom, rangeTo);
            const row = payload.billingByScalesTableCreateRequests[0];
            row.oldMeterReading = '0';
            row.newMeterReading = String(COMBINED_MISMATCH_SCALE_DIFFERENCE);
            row.difference = String(COMBINED_MISMATCH_SCALE_DIFFERENCE);
            row.multiplier = String(COMBINED_MISMATCH_SCALE_MULTIPLIER);
            row.totalVolumes = String(COMBINED_MISMATCH_SCALE_VOLUME);
            const scales = await Request.post(Endpoints.dataByScales, {data: payload});
            await expect(scales).CheckResponse();
            Responses.dataByScales.push(await scales.json());
        });

        await test.step('data by profiles (total does not equal scale)', async () => {
            const izmereno = await Nomenclatures.profiles('Измерено количество');
            const payload = alignedProfilePayload;
            payload.profileId = izmereno;
            payload.entries[0].value = COMBINED_MISMATCH_PROFILE_VOLUME;
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

        saveResponsesToFile(Responses, this.stashKey, COMBINED_MISMATCH_STASH_ID);
    }

    // REG-1296: For volumes - combined flow (profile and scale matches)
    async REG_1296(): Promise<void> {
        const { Request, Endpoints, Nomenclatures, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('generate POD', async () => {
            const podSettlement = await Request.post(Endpoints.pod, {data: GeneratePayload.pointsOfDelivery.pod_settlement()});
            await expect(podSettlement).CheckResponse();
            Responses.pod.push(await podSettlement.json());
        });

        await test.step('Meters create', async () => {
            const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');
            const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            await expect(podGet).CheckResponse();
            const podJsonBody = await podGet.json();
            const payloadMeter = GeneratePayload.pointsOfDelivery.meters();
            const scaleCode = await Nomenclatures.scales_code(grid);
            payloadMeter.gridOperatorId = podJsonBody.gridOperatorId;
            payloadMeter.podId = podJsonBody.id;
            payloadMeter.meterScales = [scaleCode];
            const meter = await Request.post(Endpoints.meters, {data: payloadMeter});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('generate price component', async () => {
            const izmereno = await Nomenclatures.profiles('Измерено количество');
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.formulaRequest.expression = String(COMBINED_MATCH_PRICE);
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = izmereno;
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].percentage = COMBINED_MATCH_PERCENT;
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.productParameters.contractType = 'COMBINED';
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        let alignedProfilePayload: any;

        await test.step('Data by scales (total equals profile)', async () => {
            alignedProfilePayload = await GeneratePayload.energyData.profile1Month();
            const rangeFrom = String(alignedProfilePayload.periodFrom).slice(0, 10);
            const rangeTo = String(alignedProfilePayload.periodTo).slice(0, 10);
            const payload = await GeneratePayload.energyData.scaleCode(0, rangeFrom, rangeTo);
            const row = payload.billingByScalesTableCreateRequests[0];
            row.oldMeterReading = '0';
            row.newMeterReading = String(COMBINED_MATCH_SCALE_DIFFERENCE);
            row.difference = String(COMBINED_MATCH_SCALE_DIFFERENCE);
            row.multiplier = String(COMBINED_MATCH_SCALE_MULTIPLIER);
            row.totalVolumes = String(COMBINED_MATCH_VOLUME);
            const scales = await Request.post(Endpoints.dataByScales, {data: payload});
            await expect(scales).CheckResponse();
            Responses.dataByScales.push(await scales.json());
        });

        await test.step('data by profiles (total equals scale)', async () => {
            const izmereno = await Nomenclatures.profiles('Измерено количество');
            const payload = alignedProfilePayload;
            payload.profileId = izmereno;
            payload.entries[0].value = COMBINED_MATCH_VOLUME;
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

        saveResponsesToFile(Responses, this.stashKey, COMBINED_MATCH_STASH_ID);
    }

    // REG-1008: SLP - Scale with scale code | happy pass
    async REG_1008(): Promise<void> {
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
            const res = await Request.post(Endpoints.pod, {data: payload});
            await expect(res).CheckResponse();
            Responses.pod.push(await res.json());
        });

        await test.step('Meters create', async () => {
            const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            const podJsonBody = await podGet.json();
            const scaleCodes = await Request.get('nomenclature/scales/filter?statuses=ACTIVE&page=0&size=25&prompt=codePlaywright');
            let scaleCode = null;
            for (const scale of (await scaleCodes.json()).content) {
                if (String(scale.name).includes('codePlaywright')) {
                    scaleCode = scale.id;
                    break;
                }
            }
            const payloadMeter = GeneratePayload.pointsOfDelivery.meters();
            payloadMeter.gridOperatorId = podJsonBody.gridOperatorId;
            payloadMeter.podId = podJsonBody.id;
            payloadMeter.meterScales = [scaleCode];
            const meter = await Request.post(Endpoints.meters, {data: payloadMeter});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('price parameter 15 minute', async () => {
            const measurementName = randomGens.generateCurrentTimeStamp();
            const payload = await GeneratePayload.productAndServices.priceParameter15minute();
            payload.name = `${measurementName}`;
            const priceParameter = await Request.post(Endpoints.priceParameters, {data: payload});
            expect(priceParameter).CheckResponse();
            const priceParameterJson = await priceParameter.json();
            Responses.priceParameters.push(priceParameterJson);
            const measurementPayload = {
                name: payload.name,
                gridOperatorId: envVariables.grid_operator,
                status: 'ACTIVE',
                defaultSelection: false
            };
            await Request.put(`/measurement-type/${envVariables.measurement_type}`, {data: measurementPayload});
            await GeneratePayload.productAndServices.uploadPriceParameterFile(priceParameterJson, 'MIN');
        });

        await test.step('generate price component', async () => {
            const izmereno = await Nomenclatures.profiles('Измерено количество');
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = izmereno;
            payload.applicationModelRequest.settlementPeriodsRequest.timeZone = 'EET';
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.productParameters.contractType = 'COMBINED';
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Data by scales', async () => {
            const payload = await GeneratePayload.energyData.scaleCode();
            const scales = await Request.post(Endpoints.dataByScales, {data: payload});
            await expect(scales).CheckResponse();
            Responses.dataByScales.push(await scales.json());
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1008');
    }

    // REG-1030: SLP with price profile
    async REG_1030(): Promise<void> {
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
            const res = await Request.post(Endpoints.pod, {data: payload});
            await expect(res).CheckResponse();
            Responses.pod.push(await res.json());
        });

        await test.step('Meters create', async () => {
            const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            const podJsonBody = await podGet.json();
            const scaleCodes = await Request.get('nomenclature/scales/filter?statuses=ACTIVE&page=0&size=25&prompt=codePlaywright');
            let scaleCode = null;
            for (const scale of (await scaleCodes.json()).content) {
                if (String(scale.name).includes('codePlaywright')) {
                    scaleCode = scale.id;
                    break;
                }
            }
            const payloadMeter = GeneratePayload.pointsOfDelivery.meters();
            payloadMeter.gridOperatorId = podJsonBody.gridOperatorId;
            payloadMeter.podId = podJsonBody.id;
            payloadMeter.meterScales = [scaleCode];
            const meter = await Request.post(Endpoints.meters, {data: payloadMeter});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('price parameter 15 minute', async () => {
            const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            const podJson = await podGet.json();
            const payload = await GeneratePayload.productAndServices.priceParameter15minute();
            payload.name = `${podJson.podViewMeasurementType.measurementTypeName}${randomGens.generateCurrentTimeStamp()}`;
            const priceParameter = await Request.post(Endpoints.priceParameters, {data: payload});
            expect(priceParameter).CheckResponse();
            const priceParameterJson = await priceParameter.json();
            Responses.priceParameters.push(priceParameterJson);
            const measurementPayload = {
                name: payload.name,
                gridOperatorId: envVariables.grid_operator,
                status: 'ACTIVE',
                defaultSelection: false
            };
            await Request.put(`/measurement-type/${envVariables.measurement_type}`, {data: measurementPayload});
            await GeneratePayload.productAndServices.uploadPriceParameterFile(priceParameterJson, 'MIN');
        });

        await test.step('generate price component', async () => {
            const izmereno = await Nomenclatures.profiles('Измерено количество');
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = izmereno;
            payload.applicationModelRequest.settlementPeriodsRequest.timeZone = 'EET';
            payload.formulaRequest.expression = '$PRICE_PROFILE$';
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.productParameters.contractType = 'COMBINED';
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation()});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Data by profiles', async () => {
            const payload = await GeneratePayload.energyData.profile1Month(0);
            payload.timeZone = 'CET';
            payload.profileId = 3;
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

        await test.step('Data by scales', async () => {
            const payload = await GeneratePayload.energyData.scaleCode();
            const scales = await Request.post(Endpoints.dataByScales, {data: payload});
            await expect(scales).CheckResponse();
            Responses.dataByScales.push(await scales.json());
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1030');
    }
        
    // REG-1221: slp case with shifted hour price parameter
    async REG_1221(): Promise<void> {
        const { Request, Endpoints, Nomenclatures, saveResponsesToFile, FileUploadRequest } = this.fixtures;
        const Responses = createResponsesContainer();
        const GeneratePayload = new GeneratePayloadClass(Request.raw, Responses, FileUploadRequest);

        let measurementType: any;

        await test.step('check measurement type', async () => {
            measurementType = await Nomenclatures.measurement_type('SHIFTED', envVariables.grid_operator);
        });

        await test.step('generate customer', async () => {
            const customer = await Request.post(Endpoints.customer, {data: GeneratePayload.customers.customer_legal()});
            await expect(customer).CheckResponse();
            Responses.customer.push(await customer.json());
        });

        await test.step('POD create', async () => {
            const grid = await Nomenclatures.grid_operator('ЕСО ЕАД');


            const payload = GeneratePayload.pointsOfDelivery.pod_slp();

            payload.gridOperatorId = grid;
            payload.measurementTypeId = measurementType;

            const res = await Request.post(Endpoints.pod, {data: payload});
            await expect(res).CheckResponse();
            Responses.pod.push(await res.json());
        });

        await test.step('Meters create', async () => {
            const podGet = await Request.get(`pod/${Responses.pod[0].id}?versionId=1`);
            const podJsonBody = await podGet.json();
            const scaleCodes = await Request.get('nomenclature/scales/filter?statuses=ACTIVE&page=0&size=25&prompt=codePlaywright');
            let scaleCode = null;
            for (const scale of (await scaleCodes.json()).content) {
                if (String(scale.name).includes('codePlaywright')) {
                    scaleCode = scale.id;
                    break;
                }
            }
            const payloadMeter = GeneratePayload.pointsOfDelivery.meters();
            payloadMeter.gridOperatorId = podJsonBody.gridOperatorId;
            payloadMeter.podId = podJsonBody.id;
            payloadMeter.meterScales = [scaleCode];
            const meter = await Request.post(Endpoints.meters, {data: payloadMeter});
            await expect(meter).CheckResponse();
            Responses.meters.push(await meter.json());
        });

        await test.step('generate term', async () => {
            const term = await Request.post(Endpoints.terms, {data: GeneratePayload.productAndServices.term()});
            await expect(term).CheckResponse();
            Responses.terms.push(await term.json());
        });

        await test.step('generate price parameter excel file with shifted hour', async () => {
            const startDate = '2025-10-01 00:00:00';
            const endDate = '2025-10-31 23:45:00';

            const generator = new priceParameterMP([
                {
                    startDate,
                    endDate,
                    amount: 100,
                    shiftedHours: [
                        { period: '26.10.2025 03:00' },
                        { period: '26.10.2025 03:15' },
                        { period: '26.10.2025 03:30' },
                        { period: '26.10.2025 03:45' },
                    ],
                },
            ]);

            await generator.generateFifteenMinutes();
        })

        await test.step('price parameter 15 minute', async () => {
            const payload = await GeneratePayload.productAndServices.priceParameter()

            payload.periodType = 'FIFTEEN_MINUTES'
            payload.name = 'SHIFTED';

            const getPriceParameters = await Request.get('/prices/list?filterField=ALL&page=0&size=25&priceListColumns=DATE_OF_CREATION&prompt=SHIFTED&exactMatch=false');
            await expect(getPriceParameters).CheckResponse();
            const getPriceParametersJson = await getPriceParameters.json();

            if(getPriceParametersJson.content.length > 0 && getPriceParametersJson.content[0].name === 'SHIFTED' && getPriceParametersJson.content[0].status === 'ACTIVE') {
                Responses.priceParameters.push(getPriceParametersJson.content[0].id)
            }else{
                const priceParameter = await Request.post(Endpoints.priceParameters, {data: payload});
                expect(priceParameter).CheckResponse();

                const priceParameterJson = await priceParameter.json();
                Responses.priceParameters.push(priceParameterJson);
            }
            
            await GeneratePayload.productAndServices.uploadPriceParameterFile(Responses.priceParameters[0], 'MIN');
        });

        await test.step('generate price component', async () => {
            const izmereno = await Nomenclatures.profiles('Измерено количество');
            const payload = GeneratePayload.productAndServices.priceSettlement();
            payload.applicationModelRequest.settlementPeriodsRequest.profiles[0].profileId = izmereno;
            payload.applicationModelRequest.settlementPeriodsRequest.timeZone = 'EET';
            const price = await Request.post(Endpoints.priceComponent, {data: payload});
            await expect(price).CheckResponse();
            Responses.priceComponent.push(await price.json());
        });

        await test.step('generate product', async () => {
            const product = await Request.post(Endpoints.product, {data: GeneratePayload.productAndServices.product()});
            await expect(product).CheckResponse();
            Responses.product.push(await product.json());
        });

        await test.step('generate contract', async () => {
            const payload = await GeneratePayload.contractsAndOrders.product_contract();
            payload.productParameters.contractType = 'COMBINED';
            const contract = await Request.post(Endpoints.productContract, {data: payload});
            await expect(contract).CheckResponse();
            Responses.productContract.push(await contract.json());
        });

        await test.step('Activate POD', async () => {
            const podActivation = await Request.post('/contract-pods/manual', {data: await GeneratePayload.pointsOfDelivery.pod_activation(0, '2025-10-01', '2025-10-31')});
            await expect(podActivation).CheckResponse();
        });

        await test.step('Data by scales', async () => {
            const payload = await GeneratePayload.energyData.scaleCode(0, '2025-10-01', '2025-10-31');
            const scales = await Request.post(Endpoints.dataByScales, {data: payload});
            await expect(scales).CheckResponse();
            Responses.dataByScales.push(await scales.json());
        });

        await test.step('Billing run', async () => {
            const billingRun = await Request.post(Endpoints.billingRun, {data: await GeneratePayload.billing.billingRun()});
            await expect(billingRun).CheckResponse();
            Responses.billingRun.push(await billingRun.json());
        });

        saveResponsesToFile(Responses, this.stashKey, 'REG-1221');
    }
}