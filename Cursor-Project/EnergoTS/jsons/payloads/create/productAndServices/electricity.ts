import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

type NumberType = 'POSITIVE' | 'NEGATIVE' | 'POSITIVE_NEGATIVE';
type InvoiceSeparation = 'INVOICE_ONE' | 'INVOICE_SEPARATE';
type ApplicationType = 'WITH_ELECTRICITY_INVOICE';
type PeriodType = 'DAY' | 'WEEK' | 'MONTH';

type FormulaVariable = {
    name: string;
    value: string | number;
}

type FormulaRequest = {
    expression: string | number;
    variables: FormulaVariable[];
    condition: string | null;
    priceText: string | null;
    issuedSeparateInvoice: InvoiceSeparation;
    segmentIds: number[];
    preferenceIds: number[];
    podCountryIds: number[];
    podRegionIds: number[];
    podPopulatedPlaceIds: number[];
    podGridOperatorIds: number[];
    podMeasurementIds: number[];
    riskAssessmentIds: number[];
    contractCampaignIds: number[];
    priceParameterIds: number[];
}

type OverTimeWithElectricityInvoiceRequest = {
    withEveryInvoice: boolean;
    atMostOncePer: boolean;
    overTimeWithElectricityPeriodType: PeriodType;
}

type ApplicationModelRequest = {
    applicationModelType: string;
    applicationType: ApplicationType;
    applicationLevel: string;
    overTimeWithElectricityInvoiceRequest: OverTimeWithElectricityInvoiceRequest;
}

type ElectricityPayload = {
    discount: boolean;
    name: string;
    displayName: string;
    priceComponentPriceTypeId: number;
    priceComponentValueTypeId: number;
    currencyId: number;
    numberType: NumberType;
    vatRateId: number | null;
    globalVatRate: boolean;
    numberOfIncomeAccount: string | null;
    controllingOrder: string | null;
    tagForContractTemplate: string | null;
    formulaRequest: FormulaRequest;
    applicationModelRequest: ApplicationModelRequest;
    consumer: boolean;
    generator: boolean;
    doNotIncludeVatBase: boolean;
}

export function electricity(): ElectricityPayload {
    return {
        "discount": false,
        "name": 'WITH_ELECTRICITY_INVOICE',
        "displayName": randomGens.generateRandomString(true, false, 10),
        "priceComponentPriceTypeId": envVariables.price_component_price_type,
        "priceComponentValueTypeId": envVariables.price_component_value_type,
        "currencyId": envVariables.currency,
        "numberType": "POSITIVE",
        "vatRateId": null,
        "globalVatRate": true,
        "numberOfIncomeAccount": null,
        "controllingOrder": null,
        "tagForContractTemplate": null,
        "formulaRequest": {
            "expression": `${randomGens.getRandomEven()}`,
            "variables": [],
            "condition": null,
            "priceText": null,
            "issuedSeparateInvoice": "INVOICE_ONE",
            "segmentIds": [],
            "preferenceIds": [],
            "podCountryIds": [],
            "podRegionIds": [],
            "podPopulatedPlaceIds": [],
            "podGridOperatorIds": [],
            "podMeasurementIds": [],
            "riskAssessmentIds": [],
            "contractCampaignIds": [],
            "priceParameterIds": []
        },
        "applicationModelRequest": {
            "applicationModelType": "PRICE_AM_OVERTIME",
            "applicationType": "WITH_ELECTRICITY_INVOICE",
            "applicationLevel": "POD",
            "overTimeWithElectricityInvoiceRequest": {
                "withEveryInvoice": false,
                "atMostOncePer": true,
                "overTimeWithElectricityPeriodType": "DAY"
            }
        },
        "consumer": false,
        "generator": false,
        "doNotIncludeVatBase": false
    };
}