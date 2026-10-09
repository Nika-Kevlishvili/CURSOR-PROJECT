import { envVariables } from "../../../fixtures/envCashed";
import { randomGens } from "../../../utils/randomGens";

type NumberType = 'POSITIVE' | 'NEGATIVE' | 'POSITIVE_NEGATIVE';
type InvoiceSeparation = 'INVOICE_ONE' | 'INVOICE_TWO'| 'INVOICE_THREE' | 'INVOICE_FOUR';
type PeriodType = 'DAY_OF_MONTH' | 'DAY_OF_WEEK' | 'HOUR_OF_DAY';
type MinuteRange = 'ZERO_FIFTEEN' | 'SIXTEEN_THIRTY' | 'THIRTYONE_FORTYFIVE' | 'FORTYSIX_SIXTY';
type Hours = 'ALL_HOURS' | string;
type Month = 'JANUARY' | 'FEBRUARY' | 'MARCH' | 'APRIL' | 'MAY' | 'JUNE' | 'JULY' | 'AUGUST' | 'SEPTEMBER' | 'OCTOBER' | 'NOVEMBER' | 'DECEMBER';
type MonthNumbers = 'ALL_DAYS' | string;
type TimeZone = 'CET' | 'UTC' | string;
type ApplicationType = 'BY_SETTLEMENT_PERIODS';

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

type SettlementPeriod = {
    minuteRange: MinuteRange;
    hours: Hours[];
}

type Profile = {
    profileId: number;
    percentage: number;
}

type DateOfMonth = {
    month: Month;
    monthNumbers: MonthNumbers[];
}

type KwhRestriction = {
    valueFrom: number | null;
    valueTo: number | null;
}

type CcyRestriction = {
    valueFrom: number | null;
    valueTo: number | null;
    currency: number | null;
}

type SettlementPeriodsRequest = {
    periodType: PeriodType;
    settlementPeriods: SettlementPeriod[];
    timeZone: TimeZone;
    profiles: Profile[];
    dateOfMonths: DateOfMonth[];
    hasVolumeRestriction: boolean;
    hasValueRestriction: boolean;
    kwhRestrictionPercent: number | null;
    kwhRestriction: KwhRestriction[];
    ccyRestriction: CcyRestriction[];
}

type ApplicationModelRequest = {
    applicationModelType: string;
    applicationType: ApplicationType;
    settlementPeriodsRequest: SettlementPeriodsRequest;
}

type PriceSettlementPayload = {
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
    customerDetailId: number | null;
}

export function PriceSettlement(): PriceSettlementPayload {
    return {
        "discount": false,
        "name": 'BY_SETTLEMENT_PERIODS',
        "displayName": `${randomGens.generateRandomString(true, false, 10)}`,
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
            "applicationModelType": "PRICE_AM_FOR_VOLUMES",
            "applicationType": "BY_SETTLEMENT_PERIODS",
            "settlementPeriodsRequest": {
                "periodType": "DAY_OF_MONTH",
                "settlementPeriods": [
                    {
                        "minuteRange": "ZERO_FIFTEEN",
                        "hours": [
                            "ALL_HOURS"
                        ]
                    },
                    {
                        "minuteRange": "SIXTEEN_THIRTY",
                        "hours": [
                            "ALL_HOURS"
                        ]
                    },
                    {
                        "minuteRange": "THIRTYONE_FORTYFIVE",
                        "hours": [
                            "ALL_HOURS"
                        ]
                    },
                    {
                        "minuteRange": "FORTYSIX_SIXTY",
                        "hours": [
                            "ALL_HOURS"
                        ]
                    }
                ],
                "timeZone": "CET",
                "profiles": [
                    {
                        "profileId": envVariables.profiles,
                        "percentage": randomGens.generatePercentage()
                    }
                ],
                "dateOfMonths": [
                    {
                        "month": "JANUARY",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "FEBRUARY",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "MARCH",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "APRIL",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "MAY",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "JUNE",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "JULY",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "AUGUST",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "SEPTEMBER",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "OCTOBER",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "NOVEMBER",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    },
                    {
                        "month": "DECEMBER",
                        "monthNumbers": [
                            "ALL_DAYS"
                        ]
                    }
                ],
                "hasVolumeRestriction": false,
                "hasValueRestriction": false,
                "ccyRestriction": [
                {
                    "valueFrom": null,
                    "valueTo": null,
                    "currency": envVariables.currency
                }],
                "kwhRestriction": [
                    {
                        "valueFrom": null,
                        "valueTo": null
                    }
                ],
                "ccyRestriction": [
                    {
                        "valueFrom": null,
                        "valueTo": null,
                        "currency": envVariables.currency
                    }
                ],
                "kwhRestrictionPercent": null,
            }
        },
        "consumer": false,
        "generator": false,
        "doNotIncludeVatBase": false,
        "customerDetailId": null
    }
}
