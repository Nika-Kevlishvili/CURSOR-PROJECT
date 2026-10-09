import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type NumberType = 'POSITIVE' | 'NEGATIVE' | 'POSITIVE_NEGATIVE';
type InvoiceSeparation = 'INVOICE_ONE' | 'INVOICE_SEPARATE';
type ApplicationType = 'PERIODICALLY';
type ApplicationLevel = 'CONTRACT' | 'POD';
type PeriodType = 'DAY_OF_WEEK_AND_PERIOD_OF_YEAR' | 'MONTHLY' | 'YEARLY';
type Week = 'FIRST_WEEK' | 'SECOND_WEEK' | 'THIRD_WEEK' | 'FOURTH_WEEK' | 'FIFTH_WEEK' | 'LAST_WEEK';
type Days = 'ALL_DAYS' | 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

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

type DayOfWeek = {
    week: Week;
    days: Days[];
}

type DayOfWeekAndPeriodOfYear = {
    daysOfWeek: DayOfWeek[];
    yearRound: boolean;
}

type OverTimePeriodicallyRequest = {
    periodType: PeriodType;
    dayOfWeekAndPeriodOfYear: DayOfWeekAndPeriodOfYear;
}

type ApplicationModelRequest = {
    applicationModelType: string;
    applicationType: ApplicationType;
    applicationLevel: ApplicationLevel;
    overTimePeriodicallyRequest: OverTimePeriodicallyRequest;
}

type PeriodicalComponentPayload = {
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

export const periodicalComponent: PeriodicalComponentPayload = {
    "discount": false,
    "name": 'PERIODICALLY',
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
        "applicationType": "PERIODICALLY",
        "applicationLevel": "CONTRACT",
        "overTimePeriodicallyRequest": {
            "periodType": "DAY_OF_WEEK_AND_PERIOD_OF_YEAR",
            "dayOfWeekAndPeriodOfYear": {
                "daysOfWeek": [
                    {
                        "week": "FIRST_WEEK",
                        "days": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "week": "SECOND_WEEK",
                        "days": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "week": "THIRD_WEEK",
                        "days": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "week": "FOURTH_WEEK",
                        "days": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "week": "FIFTH_WEEK",
                        "days": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "week": "LAST_WEEK",
                        "days": [
                            "ALL_DAYS"
                        ]
                    }
                ],
                "yearRound": true
            }
        }
    },
    "consumer": false,
    "generator": false,
    "doNotIncludeVatBase": false
}