import { APIRequestContext } from "playwright";
import { baseFixture } from "../fixtures/baseFixture";

import {expect} from '../fixtures/baseFixture';
import { Endpoints } from "../fixtures/constants/endpoints";


export class billingValidator {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];
    private currentInvoiceId: string | null;
    private GeneratePayload: baseFixture['GeneratePayload'];

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses'], generatePayload: baseFixture['GeneratePayload']) {
        this.Request = apiRequestContext;
        this.responses = responses;
        this.currentInvoiceId = null;
        this.GeneratePayload = generatePayload;
    }

    public async invoiceFirstTabValidator() {
        const invoiceTypes = ['MANUAL', 'STANDARD', 'INTERIM', 'CORRECTION', 'REVERSAL', 'RECONNECTION'];
        const invoiceDocumentTypes = ['INVOICE', 'DEBIT', 'CREDIT']
        const basisForIssuing = ['Електрическа енергия за периода', 'Междинно/авансово плащане за периода', 'Продажба на стоки']

        const firstTab = await this.Request.get(`invoice?id=${this.currentInvoiceId}`);
        const firstTabJson = await firstTab.json();

        const billingRun = await this.Request.get(`billing-run/${firstTabJson.billingRun.id}`)
        const billingRunJson = await billingRun.json();

        expect(firstTabJson.invoiceNumber).toHaveLength(10);
        expect(firstTabJson.invoiceDate).toBe(billingRunJson.commonParameters.invoiceDate);
        expect(firstTabJson.invoiceStatus).toBe('REAL');
        expect(invoiceTypes).toContain(firstTabJson.invoiceType);
        expect(firstTabJson.customer.id).toBe(this.responses.customer[0].id);
        expect(firstTabJson.customer.name).not.toBeNull();
        expect(firstTabJson.taxEventDate).toBe(billingRunJson.commonParameters.invoiceDate);
        expect(invoiceDocumentTypes).toContain(firstTabJson.invoiceDocumentType);

        if(firstTabJson.productContract){
            const productContractIds = this.responses.productContract.map(pc => pc.id);
            const productIds = this.responses.product.map(p => p.id);

            expect(productIds).toContain(firstTabJson.productContract.product);
            expect(productContractIds).toContain(firstTabJson.productContract.id);
            if (billingRunJson.applicationModel === 'FOR_VOLUMES') {
                expect(firstTabJson.contractBillingGroup.id).not.toBeNull();
            }

            expect(firstTabJson.incomeAccountNumber).not.toBeNull();
            expect(firstTabJson.costCenterControllingOrder).not.toBeNull();

        }else if(firstTabJson.serviceContract){
            const serviceContractIds = this.responses.serviceContract;
            const serviceIds = this.responses.service.map(s => s.id);

            expect(serviceIds).toContain(firstTabJson.serviceContract.service);
            expect(serviceContractIds).toContain(firstTabJson.serviceContract.id);
            expect(firstTabJson.contractBillingGroup).toBeNull();
            expect(firstTabJson.incomeAccountNumber).not.toBeNull();
            expect(firstTabJson.costCenterControllingOrder).not.toBeNull();
        }

        expect(firstTabJson.receiptOfInvoice).not.toBeNull();
        expect(firstTabJson.customerCommunications).not.toBeNull();
        
        if(this.responses.dataByProfiles?.length || this.responses.dataByScales?.length) {
            expect(firstTabJson.meterReadingFrom).not.toBeNull();
            expect(firstTabJson.meterReadingTo).not.toBeNull();
        }

        expect(basisForIssuing).toContain(firstTabJson.basisForIssuing);
        expect(firstTabJson.template).not.toBeNull();
        expect(firstTabJson.issuer).not.toBeNull();
        expect(firstTabJson.file).not.toBeNull();

        expect(firstTabJson.totalAmountExcludingVat).not.toBeNull();
        expect(firstTabJson.invoiceVatRateResponses).not.toBeNull();
        expect(firstTabJson.totalAmountIncludingVat).not.toBeNull();
        expect(firstTabJson.totalAmountIncludingVatInOtherCurrency).not.toBeNull();
        expect(firstTabJson.currency).not.toBeNull();

        if(firstTabJson.totalAmountExcludingVat !== 0) {
            expect(firstTabJson.liabilitiesAndReceivables).not.toBeNull();
        }
    }

    public async invoiceSecondTabValidator() {
        const getSymmaryTab = this.Request.get(`invoice/summary-data?page=0&size=25&id=${this.currentInvoiceId}`);
        const summaryTabJson = await (await getSymmaryTab).json();

        //group price components with same validations by application types
        const group1 = ['BY_SCALES', 'BY_SETTLEMENT_PERIODS'];
        const group2 = ['PERIODICALLY', 'WITH_ELECTRICITY_INVOICE', 'ONE_TIME'];
        const group3 = ['PRICE_AM_PER_PIECE', 'SERVICE_ORDER', 'GOODS_ORDER'];

        expect(summaryTabJson.content).not.toBeNull();
        for(const priceComp of summaryTabJson.content) {
            expect(priceComp.value).not.toBeNull();
            expect(priceComp.unitOfMeasureForValue).not.toBeNull();
            expect(priceComp.incomeAccount).not.toBeNull();
            expect(priceComp.costCenter).not.toBeNull();
            expect(priceComp.vatRatePercent).not.toBeNull();

            if(priceComp.priceComponent in group1) {
                expect(priceComp.totalVolumes).not.toBeNull();
                expect(priceComp.unitOfMeasure).toBe('kWh/kVArh')
                expect(priceComp.unitPrice).not.toBeNull();
                expect(priceComp.unitOfMeasureForUnitPrice).not.toBeNull();
            }else if(priceComp.priceComponent in group2) {
                expect(priceComp.totalVolumes).not.toBeLessThan(1)
                expect(priceComp.unitOfMeasure).toBeNull();
                expect(priceComp.unitPrice).toBeNull();
                expect(priceComp.unitOfMeasureForUnitPrice).toBeNull();
            }
            else if(priceComp.priceComponent in group3) {
                expect(priceComp.totalVolumes).not.toBeNull();
                expect(priceComp.unitOfMeasure).not.toBeNull();
                expect(priceComp.unitPrice).not.toBeNull();
                if(priceComp.priceComponent === 'PRICE_AM_PER_PIECE') {
                    expect(priceComp.unitOfMeasureForUnitPrice).not.toBeNull();
                }
            }
            else if(priceComp.priceComponent.startsWith('INTERIM')) {
                expect(priceComp.totalVolumes).toBeNull();
                expect(priceComp.unitOfMeasure).toBeNull();
                expect(priceComp.unitPrice).toBeNull();
                expect(priceComp.unitOfMeasureForUnitPrice).toBeNull();
            }

        }
    }

    public async invoiceThirdTabValidator() {}

    public async checkInvoiceTabs() {
        for(const invoiceId of this.responses.invoice) {
            this.currentInvoiceId = invoiceId;
            await this.invoiceFirstTabValidator();
            await this.invoiceSecondTabValidator();
            // await this.invoiceThirdTabValidator();
        }
    }

    public async interimInvoiceValidator() {
        let allInterimJsons: any[] = [];
        let sumAmount = 0;

        if (this.responses.interim) {
            for (const i of this.responses.interim) {
                const response = await this.Request.get(`iap/${i}`);
                const interimjson = await response.json();
                allInterimJsons.push(interimjson);
            }
        }

        for (const interimJson of allInterimJsons) {
            const type = interimJson.valueType

            switch (type) {
                case 'EXACT_AMOUNT': {

                    let allPriceJsons: any[] = [];
                    let vatRate = 0;
                    let sum = 0;
                    if(this.responses.priceComponent) {
                        for (const i of this.responses.priceComponent) {
                            const response = await this.Request.get(`price-components/${i}`);
                            const pricejson = await response.json();
                            allPriceJsons.push(pricejson);
                        }
                        
                    }
                    for (const priceJson of allPriceJsons) {
                        if (priceJson.globalVatRate === true) {
                        let vatRateResponse = await this.Request.get('vat-rates?page=0&size=1&statuses=ACTIVE');
                        const vatRateBody = await vatRateResponse.json();
                        vatRate = vatRateBody.content[0].valueInPercent;
                    } else {
                        vatRate = priceJson.vatRate?.valueInPercent || 0;
                    }
                    }
                    
                    const amount = parseFloat(interimJson.value || '0');
                    console.log('Interim Amount:', amount);
                    console.log('VAT Rate:', vatRate);
                    const amountWithVat = amount + (amount * vatRate / 100);
                    sum = Math.round(amountWithVat * 100) / 100;
                    sumAmount += sum;
                    console.log(sumAmount);
                    break;
                }

                case 'PRICE_COMPONENT': {
                    let allPriceJsons: any[] = [];
                    let vatRate = 0;
                    let sum = 0;

                    if(this.responses.priceComponent[0]) {
                        const response = await this.Request.get(`price-components/${this.responses.priceComponent[0]}`);
                        const pricejson = await response.json();
                        allPriceJsons.push(pricejson);
                    }
                    
                    for (const priceJson of allPriceJsons) {
                        if (priceJson.globalVatRate === true) {
                        let vatRateResponse = await this.Request.get('vat-rates?page=0&size=1&statuses=ACTIVE');
                        const vatRateBody = await vatRateResponse.json();
                        vatRate = vatRateBody.content[0].valueInPercent;
                    } else {
                        vatRate = priceJson.vatRate?.valueInPercent || 0;
                    }
                    }
                    
                    const priceJson = allPriceJsons[0];
                    const priceAmount = parseFloat(priceJson.priceFormula || '0');
                    const amountWithVat = priceAmount + (priceAmount * vatRate / 100);

                    sum = Math.round(amountWithVat * 100) / 100;
                    sumAmount += sum;

                    break;
                
                }
            }
        }
        return sumAmount;
    }
    
    public async amountCalculation() {
        let allPriceJsons: any[] = [];
        let sumAmount = 0;

        if (this.responses.priceComponent) {
            for (const i of this.responses.priceComponent) {
                const response = await this.Request.get(`price-components/${i}`);
                const pricejson = await response.json();
                allPriceJsons.push(pricejson);
            }
        }

        for (const priceJson of allPriceJsons) {
            const type = priceJson.applicationModelResponse?.applicationType;
            
            switch (type) {
                case 'WITH_ELECTRICITY_INVOICE': {
                    let vatRateElectricity = 0;
                    let sumElectricity = 0;
                    let sumElectricityWithVat = 0;

                    if (priceJson.globalVatRate === true) {
                        const vatRate = await this.Request.get('vat-rates?page=0&size=1&statuses=ACTIVE');
                        const vatRateBody = await vatRate.json();
                        vatRateElectricity = vatRateBody.content[0].valueInPercent;
                    } else {
                        vatRateElectricity = priceJson.vatRate?.valueInPercent || 0;
                    }
                    
                    const priceAmount = parseFloat(priceJson.priceFormula || '0');

                    const periodFrom = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString();
                    const periodTo = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).toISOString();

                    const Profile = await this.Request.get(`billing-by-profile/${this.responses.dataByProfiles?.[0]?.id}?periodFrom=${encodeURIComponent(periodFrom)}&periodTo=${encodeURIComponent(periodTo)}`);
                    const profileJson = await Profile.json();
                    const profileAmount = parseFloat(profileJson.entries?.[0]?.value || '0');               
                    
                    sumElectricity = priceAmount;
                    sumElectricityWithVat = sumElectricity + (sumElectricity * vatRateElectricity / 100);
                    sumElectricityWithVat = Math.round(sumElectricityWithVat * 100) / 100;
                    sumAmount += sumElectricityWithVat;
                    break;
                };

                case 'PERIODICALLY':{

                    let vatRatePeriodic = 0;
                    let sumPeriodic = 0;
                    let sumPeriodicWithVat = 0;

                    if (priceJson.globalVatRate === true) {
                        const vatRate = await this.Request.get('vat-rates?page=0&size=1&statuses=ACTIVE');
                        const vatRateBody = await vatRate.json();
                        vatRatePeriodic = vatRateBody.content[0].valueInPercent;
                    } else {
                        vatRatePeriodic = priceJson.vatRate?.valueInPercent || 0;
                    }
                      const priceAmount = parseFloat(priceJson.priceFormula || '0');
                    sumPeriodic = priceAmount;
                    sumPeriodicWithVat = sumPeriodic + (sumPeriodic * vatRatePeriodic / 100);
                    sumPeriodicWithVat = Math.round(sumPeriodicWithVat * 100) / 100;
                    sumAmount += sumPeriodicWithVat;
                    break;
                    
                }

                case 'BY_SETTLEMENT_PERIODS': {
                    let vatRateVolume = 0;
                    let sumWithoutVat = 0;
                    let sum = 0;

                    if (priceJson.globalVatRate === true) {
                        const vatRate = await this.Request.get('vat-rates?page=0&size=1&statuses=ACTIVE');
                        const vatRateBody = await vatRate.json();
                        vatRateVolume = vatRateBody.content[0].valueInPercent;
                    } else {
                        vatRateVolume = priceJson.vatRate?.valueInPercent || 0;
                    }

                    if(priceJson.priceFormula === '$PRICE_PROFILE$') {
                        const periodFrom2 = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString();
                        const periodTo2 = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).toISOString();

                        const Profile2 = await this.Request.get(`billing-by-profile/${this.responses.dataByProfiles?.[0].id}?periodFrom=${encodeURIComponent(periodFrom2)}&periodTo=${encodeURIComponent(periodTo2)}`);
                        const profileJson2 = await Profile2.json();

                        const profilePercentage = parseFloat(profileJson2.entries?.[0]?.value || '0');
                        
                        const pricePercent = parseFloat(priceJson.applicationModelResponse?.volumesBySettlementPeriodResponse?.profileResponses?.[0]?.percentage || '0');
                        
                        const ProfilePrice = await this.Request.get(`billing-by-profile/${this.responses.dataByProfiles?.[1].id}?periodFrom=${encodeURIComponent(periodFrom2)}&periodTo=${encodeURIComponent(periodTo2)}`);
                        const profileJsonPrice = await ProfilePrice.json();

                        const profilePercentagePrice = parseFloat(profileJsonPrice.entries?.[0]?.value || '0');
                        const priceAmount = profilePercentagePrice;

                        sumWithoutVat = (profilePercentage * pricePercent / 100) * priceAmount;
                        sum = sumWithoutVat + (sumWithoutVat * vatRateVolume / 100);
                        sum = Math.round(sum * 100) / 100;
                        sumAmount += sum;
                    }else{
                    const priceAmount = parseFloat(priceJson.priceFormula || '0');
                    const pricePercent = parseFloat(priceJson.applicationModelResponse?.volumesBySettlementPeriodResponse?.profileResponses?.[0]?.percentage || '0');

                    const periodFrom2 = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString();
                    const periodTo2 = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).toISOString();

                    const Profile2 = await this.Request.get(`billing-by-profile/${this.responses.dataByProfiles?.[0].id}?periodFrom=${encodeURIComponent(periodFrom2)}&periodTo=${encodeURIComponent(periodTo2)}`);
                    const profileJson2 = await Profile2.json();
                    const profilePercentage = parseFloat(profileJson2.entries?.[0]?.value || '0');

                    sumWithoutVat = (profilePercentage * pricePercent / 100) * priceAmount;
                    sum = sumWithoutVat + (sumWithoutVat * vatRateVolume / 100);
                    sum = Math.round(sum * 100) / 100;
                    sumAmount += sum;}
                    break;
                }

                case 'BY_SCALES': {
                    let vatRateScale = 0;
                    let sumScale = 0;
                    let sumScaleWithVat = 0;

                     if (priceJson.globalVatRate === true) {
                        const vatRate = await this.Request.get('vat-rates?page=0&size=1&statuses=ACTIVE');
                        const vatRateBody = await vatRate.json();
                        vatRateScale = vatRateBody.content[0].valueInPercent;
                    } else {
                        vatRateScale = priceJson.vatRate?.valueInPercent || 0;
                    }
                   
                    const priceAmount = parseFloat(priceJson.priceFormula || '0');
                    const scaleGet = await this.Request.get(`billing-by-scales/${this.responses.dataByScales[0]}`);
                    const scaleJson = await scaleGet.json();
                    const totalVolume = await scaleJson.billingByScalesTableCreateRequests[0].totalVolumes;

                    sumScale = totalVolume * priceAmount;
                    sumScaleWithVat = sumScale + (sumScale * vatRateScale / 100)
                    sumScaleWithVat = Math.round(sumScaleWithVat * 100) / 100;
                    sumAmount += sumScaleWithVat;
                } break;

                case 'ONE_TIME': {
                    
                    let vatRatePeriodic = 0;
                    let sumPeriodic = 0;
                    let sumPeriodicWithVat = 0;

                    if (priceJson.globalVatRate === true) {
                        const vatRate = await this.Request.get('vat-rates?page=0&size=1&statuses=ACTIVE');
                        const vatRateBody = await vatRate.json();
                        vatRatePeriodic = vatRateBody.content[0].valueInPercent;
                    } else {
                        vatRatePeriodic = priceJson.vatRate?.valueInPercent || 0;
                    }
                      const priceAmount = parseFloat(priceJson.priceFormula || '0');
                    sumPeriodic = priceAmount;
                    sumPeriodicWithVat = sumPeriodic + (sumPeriodic * vatRatePeriodic / 100);
                    sumPeriodicWithVat = Math.round(sumPeriodicWithVat * 100) / 100;
                    sumAmount += sumPeriodicWithVat;
                    
                    
                }break;

                case null: {
                    
                    let vatRatePerPiece = 0;
                    let sumPerPiece = 0;
                    let sumPerPieceWithVat = 0;

                    if (priceJson.globalVatRate === true) {
                        const vatRate = await this.Request.get('vat-rates?page=0&size=1&statuses=ACTIVE');
                        const vatRateBody = await vatRate.json();
                        vatRatePerPiece = vatRateBody.content[0].valueInPercent;
                    } else {
                        vatRatePerPiece = priceJson.vatRate?.valueInPercent || 0;
                    }
                      
                    const priceAmount = parseFloat(priceJson.priceFormula || '0');
                    const quantity = parseFloat(priceJson.applicationModelResponse?.perPieceResponse?.ranges?.[0]?.to || '0');

                    sumPerPiece = priceAmount * quantity;
                    sumPerPieceWithVat = sumPerPiece + (sumPerPiece * vatRatePerPiece / 100);
                    sumPerPieceWithVat = Math.round(sumPerPieceWithVat * 100) / 100;
                    sumAmount += sumPerPieceWithVat;
                    
                }
            }
        }
        return sumAmount;
    }

    public async restrictionValidator(){
        const priceComponentResponse = await this.Request.get(`price-components/${this.responses.priceComponent[0]}`);
        const priceComponentJson = await priceComponentResponse.json();
        const vbsp = priceComponentJson.applicationModelResponse.volumesBySettlementPeriodResponse;

        const periodFrom = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString();
        const periodTo = new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).toISOString();
        const profile = await this.Request.get(`billing-by-profile/${this.responses.dataByProfiles?.[0]?.id}?periodFrom=${encodeURIComponent(periodFrom)}&periodTo=${encodeURIComponent(periodTo)}`);
        const profileJson = await profile.json();
        const profileAmount = Number(profileJson.entries?.[0]?.value ?? 0);
        const profilePercent = Number(vbsp.profileResponses?.[0]?.percentage ?? 0);

        const to8 = (v: number) => Number(v.toFixed(8));
        const to12 = (v: string | number) => Number(Number(v).toFixed(12));

        // Helper: fetch summary tab for the first invoice
        const getSummary = async () => {
            const invoiceId = this.responses.invoice[0];
            const res = await this.Request.get(`invoice/summary-data?page=0&size=25&id=${invoiceId}`);
            return await res.json();
        };

        // Helper: assert billing run produced no real invoices (restriction blocked billing)
        const assertNoInvoiceProduced = async () => {
            const billingRunRes = await this.Request.get(`${Endpoints.billingRun}/${this.responses.billingRun[0]}`);
            const billingRunJson = await billingRunRes.json();
            const draftRes = await this.Request.get(`${Endpoints.billingRun}/draft-invoice?id=${this.responses.billingRun[0]}`);
            const draftJson = await draftRes.json();

            expect(this.responses.invoice.length,
                `Expected no invoices for blocked billing run ${this.responses.billingRun[0]}, but found ${this.responses.invoice.length}`
            ).toBe(0);
            expect(draftJson.content?.length ?? 0,
                `Expected 0 draft invoices for blocked billing run ${this.responses.billingRun[0]}, but found ${draftJson.content?.length ?? 0}`
            ).toBe(0);
        };

        // --- Volume restriction (kWh) ---
        if (vbsp.hasVolumeRestriction) {
            if (vbsp.volumeRestrictionPercent) {
                const restrictionPercent = Number(vbsp.volumeRestrictionPercent ?? 0);
                const expectedVolume = to8(profileAmount * (profilePercent / 100) * (restrictionPercent / 100));
                const summaryData = await getSummary();
                const totalVolumes = to8(Number(summaryData.content?.[0]?.totalVolumes ?? 0));
                expect(expectedVolume).toBe(totalVolumes);
            } else if (vbsp.kwhRestriction?.[0]?.valueFrom && vbsp.kwhRestriction?.[0]?.valueTo) {
                const volume = profileAmount * (profilePercent / 100);
                const restrictionFrom = Number(vbsp.kwhRestriction[0].valueFrom ?? 0);
                const restrictionTo = Number(vbsp.kwhRestriction[0].valueTo ?? 0);

                if (volume < restrictionFrom) {
                    await assertNoInvoiceProduced();
                } else if (volume > restrictionTo) {
                    const summaryData = await getSummary();
                    const expectedVolume = restrictionTo - restrictionFrom + 1;
                    expect(summaryData.content?.[0]?.totalVolumes).toBe(expectedVolume);
                } else {
                    const summaryData = await getSummary();
                    const expectedVolume = volume - restrictionFrom + 1;
                    expect(summaryData.content?.[0]?.totalVolumes).toBe(expectedVolume);
                }
            }
        }
        // --- Value restriction (currency) ---
        else if (vbsp.hasValueRestriction) {
            const formula = Number(priceComponentJson.priceFormula);
            const amount = profileAmount * (profilePercent / 100) * formula;
            const restrictionFrom = Number(vbsp.ccyRestriction?.[0]?.valueFrom ?? 0);
            const restrictionTo = Number(vbsp.ccyRestriction?.[0]?.valueTo ?? 0);

            if (amount < restrictionFrom) {
                await assertNoInvoiceProduced();
            } else if (amount > restrictionTo) {
                const summaryData = await getSummary();
                const totalVolume = restrictionTo / formula;
                const value = to12(summaryData.content?.[0]?.value);
                expect(summaryData.content?.[0]?.totalVolumes).toBe(totalVolume);
                expect(value).toBe(restrictionTo);
            } else {
                const summaryData = await getSummary();
                const totalVolume = profileAmount;
                const value = amount;
                expect(summaryData.content?.[0]?.totalVolumes).toBe(totalVolume);
                expect(to12(summaryData.content?.[0]?.value ?? 0)).toBe(to12(value));
            }
        }
    }

    public async conditionsValidator(expectation: {
        applicationLevel: 'CUSTOMER' | 'CONTRACT' | 'POD';
        billingRunIndex?: number;
        expectedInvoiceCount?: number;
        expectNoInvoices?: boolean;
        includedPodIdentifiers?: string[];
        excludedPodIdentifiers?: string[];
        includedContractIds?: Array<string | number>;
        excludedContractIds?: Array<string | number>;
        includedCustomerIds?: Array<string | number>;
    }): Promise<void> {
        const billingRunIndex = expectation.billingRunIndex ?? 0;
        const rawBillingRun = this.responses.billingRun?.[billingRunIndex];
        const billingRunId =
            typeof rawBillingRun === 'object' && rawBillingRun !== null && 'id' in rawBillingRun
                ? (rawBillingRun as { id: number }).id
                : rawBillingRun;

        expect(billingRunId, `billingRun id missing at index ${billingRunIndex}`).toBeTruthy();

        const billingRunRes = await this.Request.get(`billing-run/${billingRunId}`);
        const billingRunJson = await billingRunRes.json();
        const params = billingRunJson.standardBillingRunParameters;

        expect(params.billingCriteria,
            `Expected CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS, got ${params.billingCriteria}`
        ).toBe('CUSTOMERS_CONTRACTS_OR_POD_CONDITIONS');
        expect(params.applicationLevel,
            `Expected applicationLevel ${expectation.applicationLevel}, got ${params.applicationLevel}`
        ).toBe(expectation.applicationLevel);

        const expectedCount = expectation.expectNoInvoices ? 0 : (expectation.expectedInvoiceCount ?? 1);

        if (expectedCount === 0) {
            const draftRes = await this.Request.get(`billing-run/draft-invoices?id=${billingRunId}&page=0&size=25`);
            const draftJson = await draftRes.json();
            expect(this.responses.invoice.length,
                `Expected no invoices for billing run ${billingRunId} at ${expectation.applicationLevel}, but found ${this.responses.invoice.length}`
            ).toBe(0);
            expect(draftJson.content?.length ?? 0,
                `Expected 0 draft invoices for billing run ${billingRunId}, but found ${draftJson.content?.length ?? 0}`
            ).toBe(0);
            return;
        }

        expect(this.responses.invoice.length,
            `Expected ${expectedCount} invoice(s) for ${expectation.applicationLevel}, got ${this.responses.invoice.length}`
        ).toBe(expectedCount);

        const foundPodIdentifiers = new Set<string>();
        const foundContractIds = new Set<string>();
        const foundCustomerIds = new Set<string>();

        for (const invoiceId of this.responses.invoice) {
            const invoiceRes = await this.Request.get(`invoice?id=${invoiceId}`);
            const invoiceJson = await invoiceRes.json();

            if (invoiceJson.customer?.id != null) {
                foundCustomerIds.add(String(invoiceJson.customer.id));
            }
            if (invoiceJson.productContract?.id != null) {
                foundContractIds.add(String(invoiceJson.productContract.id));
            }
            if (invoiceJson.serviceContract?.id != null) {
                foundContractIds.add(String(invoiceJson.serviceContract.id));
            }

            const detailedRes = await this.Request.get(`invoice/detailed-data?id=${invoiceId}&page=0&size=100`);
            const detailedJson = await detailedRes.json();
            const rows: Array<{ pointOfDelivery?: string | null }> = detailedJson.content ?? [];
            for (const row of rows) {
                const podId = String(row.pointOfDelivery ?? '').trim();
                if (podId) foundPodIdentifiers.add(podId);
            }
        }

        if (expectation.includedCustomerIds?.length) {
            for (const id of expectation.includedCustomerIds) {
                expect(foundCustomerIds.has(String(id)),
                    `[${expectation.applicationLevel}] expected customer ${id} on invoice(s)`
                ).toBe(true);
            }
        }

        if (expectation.includedContractIds?.length) {
            for (const id of expectation.includedContractIds) {
                expect(foundContractIds.has(String(id)),
                    `[${expectation.applicationLevel}] expected contract ${id} on invoice(s)`
                ).toBe(true);
            }
        }

        if (expectation.excludedContractIds?.length) {
            for (const id of expectation.excludedContractIds) {
                expect(foundContractIds.has(String(id)),
                    `[${expectation.applicationLevel}] contract ${id} should NOT be on invoice(s)`
                ).toBe(false);
            }
        }

        if (expectation.includedPodIdentifiers?.length) {
            for (const pod of expectation.includedPodIdentifiers) {
                expect(foundPodIdentifiers.has(pod),
                    `[${expectation.applicationLevel}] expected POD ${pod} on invoice detailed-data; found [${[...foundPodIdentifiers].join(', ')}]`
                ).toBe(true);
            }
        }

        if (expectation.excludedPodIdentifiers?.length) {
            for (const pod of expectation.excludedPodIdentifiers) {
                expect(foundPodIdentifiers.has(pod),
                    `[${expectation.applicationLevel}] POD ${pod} should NOT be on invoice detailed-data; found [${[...foundPodIdentifiers].join(', ')}]`
                ).toBe(false);
            }
        }
    }

    public async priceComponentConditionsValidator(expectation: {
        expectedInvoiceCount?: number;
        expectNoInvoices?: boolean;
        included?: Array<{ priceComponentName: string; podIdentifiers: string[] }>;
        excluded?: Array<{ priceComponentName: string; podIdentifiers: string[] }>;
        expectNoRowsForPriceComponent?: string[];
    }): Promise<void> {
        const expectedCount = expectation.expectNoInvoices ? 0 : (expectation.expectedInvoiceCount ?? 1);

        if (expectedCount === 0) {
            expect(this.responses.invoice.length, `Expected no invoices, found ${this.responses.invoice.length}`).toBe(0);
            return;
        }

        expect(
            this.responses.invoice.length,
            `Expected ${expectedCount} invoice(s), got ${this.responses.invoice.length}`
        ).toBe(expectedCount);

        // detailed-data.priceComponent returns application name (e.g. BY_SETTLEMENT_PERIODS),
        // not invoiceAndTemplateText/displayName — resolve via unitPrice ↔ price formula.
        const formulaToDisplayName = new Map<number, string>();
        for (const pcRef of this.responses.priceComponent ?? []) {
            const pcId =
                typeof pcRef === 'object' && pcRef !== null && 'id' in pcRef
                    ? (pcRef as { id: string | number }).id
                    : pcRef;
            const pcRes = await this.Request.get(`price-components/${pcId}`);
            const pcJson = await pcRes.json();
            const label = String(
                pcJson.invoiceAndTemplateText ?? pcJson.displayName ?? pcJson.name ?? ''
            ).trim();
            const formula = Number(pcJson.priceFormula ?? pcJson.formulaRequest?.expression);
            if (label && !Number.isNaN(formula)) {
                formulaToDisplayName.set(formula, label);
            }
        }

        type PairKey = string;
        const foundPairs = new Set<PairKey>();
        const foundPcNames = new Set<string>();

        const pairKey = (pcName: string, pod: string) => `${pcName}||${pod}`;

        for (const invoiceId of this.responses.invoice) {
            const detailedRes = await this.Request.get(`invoice/detailed-data?id=${invoiceId}&page=0&size=100`);
            const detailedJson = await detailedRes.json();
            const rows: Array<{
                priceComponent?: string | null;
                pointOfDelivery?: string | null;
                unitPrice?: string | number | null;
            }> = detailedJson.content ?? [];

            for (const row of rows) {
                const unitPrice = Number(row.unitPrice);
                const pcName = (
                    (!Number.isNaN(unitPrice) ? formulaToDisplayName.get(unitPrice) : undefined) ??
                    String(row.priceComponent ?? '').trim()
                ).trim();
                const podId = String(row.pointOfDelivery ?? '').trim();
                if (pcName) foundPcNames.add(pcName);
                if (pcName && podId) foundPairs.add(pairKey(pcName, podId));
            }
        }

        for (const item of expectation.included ?? []) {
            for (const pod of item.podIdentifiers) {
                expect(
                    foundPairs.has(pairKey(item.priceComponentName, pod)),
                    `Expected priceComponent "${item.priceComponentName}" on POD ${pod}; found pairs [${[...foundPairs].join(', ')}]`
                ).toBe(true);
            }
        }

        for (const item of expectation.excluded ?? []) {
            for (const pod of item.podIdentifiers) {
                expect(
                    foundPairs.has(pairKey(item.priceComponentName, pod)),
                    `PriceComponent "${item.priceComponentName}" should NOT appear on POD ${pod}; found pairs [${[...foundPairs].join(', ')}]`
                ).toBe(false);
            }
        }

        for (const pcName of expectation.expectNoRowsForPriceComponent ?? []) {
            expect(
                foundPcNames.has(pcName),
                `PriceComponent "${pcName}" should have no detailed-data rows; found [${[...foundPcNames].join(', ')}]`
            ).toBe(false);
        }
    }
}