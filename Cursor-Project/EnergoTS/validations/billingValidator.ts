import { APIRequestContext } from "playwright";
import { baseFixture } from "../fixtures/baseFixture";

import {expect} from '../fixtures/baseFixture';


export class billingValidator {
    private Request: APIRequestContext;
    private responses: baseFixture['Responses'];
    private currentInvoiceId: string | null;

    constructor(apiRequestContext: APIRequestContext, responses: baseFixture['Responses']) {
        this.Request = apiRequestContext;
        this.responses = responses;
        this.currentInvoiceId = null;
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
            expect(firstTabJson.contractBillingGroup.id).not.toBeNull();
            expect(firstTabJson.incomeAccountNumber).not.toBeNull();
            expect(firstTabJson.costCenterControllingOrder).not.toBeNull();

        }else if(firstTabJson.serviceContract){
            const serviceContractIds = this.responses.serviceContract.map(sc => sc.id);
            const serviceIds = this.responses.service.map(s => s.id);

            expect(serviceIds).toContain(firstTabJson.serviceContract.service);
            expect(serviceContractIds).toContain(firstTabJson.serviceContract.id);
            expect(firstTabJson.contractBillingGroup.id).toBeNull();
            expect(firstTabJson.incomeAccountNumber).not.toBeNull();
            expect(firstTabJson.costCenterControllingOrder).not.toBeNull();
        }

        expect(firstTabJson.receiptOfInvoice).not.toBeNull();
        expect(firstTabJson.customerCommunications).not.toBeNull();

        if(this.responses.dataByProfiles || this.responses.dataByScales) {
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

}